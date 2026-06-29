/**
* Copyright (C) 2026 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.jboss.intersmash.tests.wildfly.distributed.timers.infinispan;

import cz.xtf.core.openshift.OpenShift;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.openshift.PodShell;
import cz.xtf.core.waiting.SimpleWaiter;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.fabric8.kubernetes.api.model.Pod;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.openshift.InfinispanOpenShiftOperatorProvisioner;
import org.jboss.intersmash.provision.openshift.WildflyImageOpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.InfinispanTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.jboss.intersmash.tests.wildfly.distributed.timers.infinispan.model.TimerExpiration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly/JBoss EAP application based tests with (remote) <i>Infinispan</i> distributed timers.
 * <p>
 *     This test case focuses on validating regular behavior and different fail-over scenarios.
 *     The general idea is to let the test start a timer, asynchronously count its expirations for a given time, inject
 *     some kind of failure into the cluster, and eventually assert that the expected number of expirations were
 *     recorded.
 * </p>
 *
 * <h2>Deployed services</h2>
 *
 * Four services are deployed on OpenShift to form the test environment:
 *
 * <pre>{@code
 *  Client (test)        distributed-timers-infinispan (2 pods)       Infinispan / RHDG (2 pods)
 *       |                          |                                        |
 *       |--- REST (create/cancel)-->|--- HotRod (timer state) ------------->|
 *       |                          |                                        |
 *       |                          |--- Remote EJB (record expiration) ---->|
 *       |                          |                                        |
 *       |                    timer-expiration-store (1 pod)            PostgreSQL (1 pod)
 *       |                          |                                        |
 *       |--- REST (query) -------->|--- JPA (persist) --------------------->|
 * }</pre>
 *
 * In order to execute the individual tests with a deterministic status, a convenient workflow has been implemented:
 * <ul>
 *     <li>
 *     	A PostgreSql service is deployed to provide a persistence backend for saving timer expirations metadata, see
 *     	below.
 *     </li>
 *     <li>
 *     	The Timer Expiration Store app is deployed. This is a WildFly/JBoss EAP application that provides REST APIs
 *     	to save timer expirations'	meaningful data, see
 *     	{@link org.jboss.intersmash.tests.wildfly.distributed.timers.infinispan.WildFlyTimerExpirationStoreApplication}.
 *     	This application uses just 1 replica.
 *     </li>
 *     <li>
 *     	The Infinispan/Red Hat Data Grid service is deployed. This will provide the "remote" Infinispan {@code Cache}
 *     	instances that will store distributed timers' metadata. WildFly/JBoss EAP uses this as the EJB Timer Service
 *     	distributed management remote instance. See
 *     	{@link org.jboss.intersmash.tests.wildfly.distributed.timers.infinispan.InfinispanOperatorWithExternalRouteApplication}.
 *     	This service uses 2 replicas.
 *     </li>
 *     <li>
 *     	The WildFly/JBoss EAP Distributed Timers app is deployed.
 *     	This is a WildFly/JBoss EAP application that provides REST APIs to create interval timers, see
 *     	{@link org.jboss.intersmash.tests.wildfly.distributed.timers.infinispan.WildFlyDistributedTimersApplication}.
 *     	By hitting a given endpoint, an interval timer can be created with a custom configuration (e.g.: initial delay,
 *     	timeout interval etc.).
 *     	Such interval timer will consume the Timer Expiration Store app REST APIs to save each expiration
 *     	metadata (e.g.: timestamp, executor etc.).
 *     	The application is as well configured in order to connect to the remote Infinispan/Red Hat Data Grid service
 *     	to store persistent timers.
 *     </li>
 * </ul>
 *
 * <h2>How distributed-timers-infinispan uses timer-expiration-store</h2>
 *
 * <p>The {@code distributed-timers-infinispan} application connects to the {@code timer-expiration-store} application
 * via a <b>remote EJB call</b> over JNDI. The connection is configured by the {@code TIMER_EXPIRATION_API_BASE_URL}
 * environment variable (set to {@code http://timer-expiration-store:8080} in
 * {@link WildFlyDistributedTimersApplication}), with authentication credentials {@code user1/password123}.</p>
 *
 * <p>Each time a timer fires, {@code TransactionalRecurringTimerService.doExecute()} builds a
 * {@code TimerExpiration} record containing the executor pod hostname, the timer name, the application info, and the
 * current timestamp. It then invokes {@code TimerExpirationStore.createTimerExpiration()} on the remote EJB proxy,
 * which persists the record to PostgreSQL via JPA on the {@code timer-expiration-store} side.</p>
 *
 * <p>This remote EJB call records the <b>audit trail</b> of timer executions. It is separate from timer
 * <i>state persistence</i>, which is handled by the Infinispan HotRod cache
 * ({@code /subsystem=ejb3/service=timer-service} configured with
 * {@code default-persistent-timer-management=hotrod}).</p>
 *
 * <h2>What the tests verify</h2>
 *
 * <p>All tests follow the same pattern: create a timer, open a time window, inject a failure (kill a pod or scale
 * down), collect expirations from the {@code timer-expiration-store} REST API
 * ({@code GET /timer/range?from=...&to=...}), and assert the expected count and executor distribution.</p>
 *
 * <ol>
 *     <li><b>{@link #testCacheCreation()}</b> &mdash; Verifies that when WildFly connects to Infinispan via HotRod,
 *     the expected cache ({@code ROOT.war.TransactionalRecurringTimerService.PERSISTENT}) is automatically
 *     created.</li>
 *
 *     <li><b>{@link #testTimerCanBeSuccessfullyCancelledClusterWide()}</b> &mdash; Creates a timer, then cancels it
 *     from a <i>different</i> pod (not the one running the timer). Verifies that
 *     {@code TimerService.getTimers()} returns timers across the whole cluster, proving distributed timer
 *     visibility.</li>
 *
 *     <li><b>{@link #testPersistenceServiceBasicFailOver()}</b> &mdash; Creates a timer, then kills one
 *     <i>Infinispan</i> pod. Asserts that the expected number of expirations still occurred (from the same
 *     WildFly executor), proving the Infinispan cluster survived the loss of a node.</li>
 *
 *     <li><b>{@link #testTimerServiceBasicFailOver()}</b> &mdash; Creates a timer, then kills the <i>WildFly pod
 *     that is executing the timer</i>. Asserts that expirations were recorded by &ge; 1 executor (the surviving
 *     pod or a replacement picks up the timer), proving timer execution fails over between WildFly nodes.</li>
 *
 *     <li><b>{@link #testPersistentTimersSurviveTimerServiceTemporaryShutdown()}</b> &mdash; Creates a timer, then
 *     shuts down the <i>entire WildFly cluster</i> (scale to 0) and restarts it. Asserts that timer expirations
 *     resume from &gt; 1 executor, proving persistent timers survive a full application server restart because
 *     their state lives in the external Infinispan cluster.</li>
 * </ol>
 *
 * <p>
 * The tests will employ the following workflow:<br>
 *         1. start a minimal (0-0) WildFly/Infinispan cluster<br>
 *         2. scale up to a basic (2-2) WildFly/Infinispan cluster<br>
 *         ... do something<br>
 *         3. eventually scale back to the minimal cluster form again to let Infinispan have the exact number of
 *         replicas (1) that will be expected at the following restart.<br>
 */
@ExtendWith(ProjectCreator.class)
@Intersmash({
		@Service(PostgresqlTimerExpirationStoreApplication.class),
		@Service(WildFlyTimerExpirationStoreApplication.class),
		@Service(InfinispanOperatorWithExternalRouteApplication.class),
		@Service(WildFlyDistributedTimersApplication.class)
})
@WildflyTest
@EapTest
@EapXpTest
@InfinispanTest
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WildFlyInfinispanDistributedTimersIT {

	private static final OpenShift MASTER_SHIFT = OpenShifts.master();
	private static final Long RECURRING_TIMER_EXPIRATION_TIMEOUT = 10_000L;
	private static final String RECURRING_TIMER_INFO = WildFlyDistributedTimersApplication.class.getSimpleName();
	private static final int INFINISPAN_MINIMAL_CLUSTER_REPLICAS = 0;
	private static final int WILDFLY_MINIMAL_CLUSTER_REPLICAS = 0;
	private static final int INFINISPAN_BASIC_CLUSTER_REPLICAS = 2;
	private static final int WILDFLY_BASIC_CLUSTER_REPLICAS = 2;
	private static final int SHORT_TIME_WINDOW_DURATION_SECONDS = 30;
	private static final int LONG_TIME_WINDOW_DURATION_SECONDS = 60;

	@ServiceUrl(InfinispanOperatorWithExternalRouteApplication.class)
	private String infinispanRouteUrl;

	@ServiceUrl(WildFlyTimerExpirationStoreApplication.class)
	private String timerExpirationStoreRouteUrl;

	@ServiceUrl(WildFlyDistributedTimersApplication.class)
	private String wildflyDistributedTimersRouteUrl;

	@ServiceProvisioner(InfinispanOperatorWithExternalRouteApplication.class)
	private InfinispanOpenShiftOperatorProvisioner infinispanProvisioner;

	@ServiceProvisioner(WildFlyDistributedTimersApplication.class)
	private WildflyImageOpenShiftProvisioner wildflyDistributedTimersProvisioner;

	@ServiceProvisioner(WildFlyTimerExpirationStoreApplication.class)
	private WildflyImageOpenShiftProvisioner wildflyTimerExpirationStoreProvisioner;

	@BeforeAll
	public static void beforeAll() {
		// KUBE_PING requires this permission, otherwise warning is logged and clustering doesn't work,
		// an invalidation-cache requires a functioning jgroups cluster.
		MASTER_SHIFT.addRoleToServiceAccount("view", "default");
		// let's relax security constraints when calling the REST APIs for testing purposes
		RestAssured.useRelaxedHTTPSValidation();
	}

	/**
	 * Checks that the WildFly/JBoss EAP application service cache has been successfully created in the Infinispan Remote Cluster, by
	 * consuming the Infinispan REST APIs.
	 * <br>
	 * The list of {@code Cache} objects returned by the Infinispan REST APIs contains conventionally named
	 * instance, since it should be created automatically when WildFly/JBoss EAP connects to Infinispan via HotRod.
	 */
	@Test
	@Order(1)
	public void testCacheCreation() {
		minimalClusters();
		wildflyTimerExpirationStoreProvisioner.scale(0, true);
		wildflyTimerExpirationStoreProvisioner.scale(1, true);
		//
		basicClusters();
		try {
			/**
			 * Cache list can be a single item, e.g.:
			 * <pre>
			 * {@code
			 *   ["ROOT.war.TransactionalRecurringTimerService.PERSISTENT","___protobuf_metadata","___script_cache"]
			 * }
			 * </pre>
			 *
			 * or multiple items, e.g.:
			 * <pre>
			 * {@code
			 *   [ ROOT.war.TransactionalRecurringTimerService.PERSISTENT.0,
			 *     ROOT.war.TransactionalRecurringTimerService.PERSISTENT.1,
			 *     ROOT.war.TransactionalRecurringTimerService.PERSISTENT.10,
			 *     ROOT.war.TransactionalRecurringTimerService.PERSISTENT.100,
			 *     ... ]
			 * }
			 * </pre>
			 *
			 * depending on cache segmentation configuration.
			 */
			RestAssured
					.given()
					.auth()
					.basic(InfinispanOperatorWithExternalRouteApplication.INFINISPAN_CUSTOM_CREDENTIALS_USERNAME,
							InfinispanOperatorWithExternalRouteApplication.INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD)
					.when().get(infinispanRouteUrl + "/rest/v2/caches")
					.then()
					.assertThat()
					.statusCode(HttpStatus.SC_OK)
					.body("", Matchers.hasItem("ROOT.war.TransactionalRecurringTimerService.PERSISTENT"));
		} finally {
			minimalClusters();
		}
	}

	/**
	 * Verify that a timer can be accessed and cancelled from any cluster node by calling a REST API that
	 * will look for a given timer instance (discrimination happens based on the timer info) and cancel it.
	 * <br>
	 * A timer is first created, then cancelled by calling the related REST API from inside a different pod.
	 * A sample time window is then monitored to assert that no timer expiration occurred anymore after the timer
	 * cancellation.
	 * <br>
	 * The test should pass independently of the node that will receive the HTTP request, thus demonstrating
	 * that the timer service semantics do not change depending on the execution environment, i.e.
	 * the {@code javax.ejb.TimerService::getTimers()} method will take all the cluster instances into account, and
	 * timers owned by all nodes will be returned.
	 * <br>
	 * The gist of the test is no timer expirations is recorded in the monitored time period.
	 *
	 * @throws InterruptedException Thrown when the main thread wait interval is interrupted unexpectedly
	 * @throws ExecutionException Thrown when an error occurs during the asynchronous monitoring method execution
	 */
	@Test
	@Order(2)
	public void testTimerCanBeSuccessfullyCancelledClusterWide() throws InterruptedException, ExecutionException {
		final Long timeWindowDuration = SHORT_TIME_WINDOW_DURATION_SECONDS * 1_000L;
		final String applicationInfo = String.format("%s:%s", RECURRING_TIMER_INFO,
				"testTimerCanBeSuccessfullyCancelledClusterWide");
		basicClusters();
		try {
			Pod nonExecutor = null;
			createTimer(applicationInfo);
			try {
				giveTheTimerEnoughTimeToExecuteOnce(2 * RECURRING_TIMER_EXPIRATION_TIMEOUT, applicationInfo);
				nonExecutor = getNonExecutor(applicationInfo);
				Assertions.assertNotNull(nonExecutor, "Expected non-executing pod was not found after timer creation");
				log.debug("Current non-executing pod is {}", nonExecutor.getMetadata().getName());
			} finally {
				if (nonExecutor == null) {
					cancelTimer(applicationInfo);
				} else {
					cancelTimer(nonExecutor, applicationInfo);
				}
			}
			final Runnable injectedFailure = () -> log
					.info("No failure is injected when checking that a timer has been cancelled");
			final BiFunction<List<TimerExpiration>, Long, Boolean> expirationsValidator = (actualExpirations,
					pause) -> actualExpirations.isEmpty();
			final Function<Long, Boolean> executorsValidator = (actualExecutors) -> actualExecutors == 0;
			testFailOverWithinTimeWindow(timeWindowDuration, applicationInfo, injectedFailure, expirationsValidator,
					executorsValidator);
		} finally {
			minimalClusters();
		}
	}

	/**
	 * Verify a (distributed) timers persistence service fail-over scenario.
	 * Kills one Infinispan pod and asserts that the expected number of expirations still occurred from the
	 * same WildFly executor, proving the Infinispan cluster survived the loss of a node.
	 */
	@Test
	@Order(3)
	public void testPersistenceServiceBasicFailOver() throws InterruptedException, ExecutionException {
		runFailoverTest("testPersistenceServiceBasicFailOver",
				executors -> executors == 1,
				appInfo -> {
					Pod executor = getExecutor(appInfo);
					log.debug("Current timer executor is {}", executor.getMetadata().getName());
					Pod podToBeDeleted = infinispanProvisioner.getPods().get(0);
					log.debug("Infinispan pod designated for deletion: {}", podToBeDeleted.getMetadata().getName());
					long delay = RECURRING_TIMER_EXPIRATION_TIMEOUT * 2;
					return () -> stopOriginalServicePod(podToBeDeleted, delay);
				});
	}

	/**
	 * Verify a EJB (distributed) timer service fail-over scenario.
	 * Kills the WildFly pod that is executing the timer and asserts that expirations were recorded by
	 * &ge; 1 executor, proving timer execution fails over between WildFly nodes.
	 */
	@Test
	@Order(4)
	public void testTimerServiceBasicFailOver() throws InterruptedException, ExecutionException {
		runFailoverTest("testTimerServiceBasicFailOver",
				executors -> executors >= 1,
				appInfo -> {
					Pod podToBeDeleted = getExecutor(appInfo);
					log.debug("Current timer executor is {}", podToBeDeleted.getMetadata().getName());
					long delay = RECURRING_TIMER_EXPIRATION_TIMEOUT + (RECURRING_TIMER_EXPIRATION_TIMEOUT / 2);
					return () -> stopOriginalServicePod(podToBeDeleted, delay);
				});
	}

	/**
	 * Verify that persistent timers survive a full application server restart.
	 * Shuts down the entire WildFly cluster (scale to 0) and restarts it, then asserts that timer expirations
	 * resume from &gt; 1 executor, proving persistent timers survive because their state lives in the external
	 * Infinispan cluster.
	 */
	@Test
	@Order(5)
	public void testPersistentTimersSurviveTimerServiceTemporaryShutdown() throws InterruptedException, ExecutionException {
		runFailoverTest("testPersistentTimersSurviveTimerServiceTemporaryShutdown",
				executors -> executors > 1,
				appInfo -> () -> shutTimerServiceDown());
	}

	/**
	 * Common template for failover tests (tests 3, 4, 5). Scales to basic clusters, creates a timer,
	 * waits for at least one expiration, runs the test-specific failure scenario, then validates
	 * expirations and executor counts.
	 *
	 * @param testName Used to build the applicationInfo identifier
	 * @param executorsValidator Validates the expected number of distinct executors
	 * @param failureFactory Given the applicationInfo, sets up pre-failure state and returns the failure Runnable
	 */
	private void runFailoverTest(
			String testName,
			Function<Long, Boolean> executorsValidator,
			Function<String, Runnable> failureFactory) throws InterruptedException, ExecutionException {
		final Long timeWindowDuration = LONG_TIME_WINDOW_DURATION_SECONDS * 1_000L + (RECURRING_TIMER_EXPIRATION_TIMEOUT * 2);
		final BiFunction<List<TimerExpiration>, Long, Boolean> expirationsValidator = (actualExpirations,
				pause) -> validateExpectedExpirations(timeWindowDuration, pause, actualExpirations);
		final String applicationInfo = String.format("%s:%s", RECURRING_TIMER_INFO, testName);
		basicClusters();
		try {
			createTimer(applicationInfo);
			try {
				giveTheTimerEnoughTimeToExecuteOnce(2 * RECURRING_TIMER_EXPIRATION_TIMEOUT, applicationInfo);
				Runnable failure = failureFactory.apply(applicationInfo);
				testFailOverWithinTimeWindow(timeWindowDuration, applicationInfo, failure, expirationsValidator,
						executorsValidator);
			} finally {
				try {
					cancelTimer(applicationInfo);
				} catch (Exception e) {
					log.warn("Timer cancellation failed during cleanup: {}", e.getMessage());
				}
			}
		} finally {
			// otherwise Infinispan won't restart because of spec.replicasWantedAtRestart
			minimalClusters();
		}
	}

	private void minimalClusters() {
		// expected to be called when services are down (i.e. 0 replicas) this scales both the WildFly/JBoss EAP application and
		// the Infinispan cluster size up, in order to have a MINIMAL (0-0) distributed EJB timer service
		// (i.e: multiple WildFly/JBoss EAP instances that take care of timer expiration) and a distributed persistence service for
		// such timers (provided by the Infinispan instances)
		wildflyDistributedTimersProvisioner.scale(WILDFLY_MINIMAL_CLUSTER_REPLICAS, Boolean.TRUE);
		infinispanProvisioner.scale(INFINISPAN_MINIMAL_CLUSTER_REPLICAS, Boolean.TRUE);
	}

	private void basicClusters() {
		// scale both the WildFly/JBoss EAP application and the Infinispan cluster size up, in order to have a BASIC (2-2)
		// distributed EJB timer service (i.e: multiple WildFly/JBoss EAP instances that take care of timer expiration) and a
		// distributed persistence service for such timers (provided by the Infinispan instances)
		infinispanProvisioner.scale(INFINISPAN_BASIC_CLUSTER_REPLICAS, Boolean.TRUE);
		// WildFly/JBoss EAP clusters must be started with 1 replica
		wildflyDistributedTimersProvisioner.scale(1, Boolean.TRUE);
		wildflyDistributedTimersProvisioner.scale(WILDFLY_BASIC_CLUSTER_REPLICAS, Boolean.TRUE);
	}

	private void createTimer(final String applicationInfo) {
		log.info("About to CREATE a timer by calling: " + wildflyDistributedTimersRouteUrl);
		RestAssured
				.given()
				.queryParam("initialDelay", 0L)
				.queryParam("expirationInterval", RECURRING_TIMER_EXPIRATION_TIMEOUT)
				.queryParam("applicationInfo", applicationInfo)
				.get(wildflyDistributedTimersRouteUrl + "/timer/custom-interval")
				.then()
				.assertThat()
				.statusCode(HttpStatus.SC_OK);
	}

	private void cancelTimer(final String applicationInfo) {
		log.info("About to CANCEL a timer by calling DELETE on: " + wildflyDistributedTimersRouteUrl
				+ "/timer/custom-interval/{applicationInfo}");
		// let's try and delete
		SimpleWaiter waiter = new SimpleWaiter(
				() -> {
					ExtractableResponse<Response> deletionResponse = RestAssured
							.given()
							.pathParam("applicationInfo", applicationInfo)
							.delete(wildflyDistributedTimersRouteUrl + "/timer/custom-interval/{applicationInfo}")
							.then()
							.extract();
					log.info(deletionResponse.body().asString());
					return HttpStatus.SC_OK == deletionResponse.statusCode();
				},
				"Waiting for the timer deletion REST API call to return HTTP 200").interval(TimeUnit.SECONDS, 5);
		waitForCanceledTimer(applicationInfo, waiter);
	}

	private void cancelTimer(final Pod fromPod, final String applicationInfo) {
		final PodShell podShell = new PodShell(MASTER_SHIFT, fromPod);
		final String timerDeletionRequestUrl = String.format("http://localhost:8080/timer/custom-interval/%s",
				applicationInfo);
		// let's try and delete
		SimpleWaiter waiter = new SimpleWaiter(
				() -> {
					log.info("About to CANCEL a timer by calling {} from a pod", timerDeletionRequestUrl);
					final String result = podShell
							.executeWithBash("curl --request DELETE '" + timerDeletionRequestUrl + "' 2>/dev/null")
							.getOutputAsList().get(0).trim();
					log.info(result);
					return result.contains(String.valueOf(HttpStatus.SC_OK));
				},
				"Waiting for the timer deletion REST API call to return HTTP 200").interval(TimeUnit.SECONDS, 5);
		// We wait here since we've seen cases in which this is the timeout after an HTTP 504 is returned,
		// and we've filed https://issues.redhat.com/browse/JBEAP-25790 + 30 secs again to support cases in which
		// execution will be resumed by the newly created pod, rather than the surviving one
		waitForCanceledTimer(applicationInfo, waiter);
	}

	private void waitForCanceledTimer(String applicationInfo, SimpleWaiter waiter) {
		// We wait here since we've seen cases in which this is the timeout after an HTTP 504 is returned,
		// and we've filed https://issues.redhat.com/browse/JBEAP-25790 + 30 secs again to support cases in which
		// execution will be resumed by the newly created pod, rather than the surviving one
		waiter.timeout(TimeUnit.SECONDS, 60).waitFor();
		// let's check it is really gone
		waiter = new SimpleWaiter(
				() -> {
					ExtractableResponse<Response> checkResponse = RestAssured
							.given()
							.pathParam("applicationInfo", applicationInfo)
							.get(wildflyDistributedTimersRouteUrl + "/timer/custom-interval/{applicationInfo}")
							.then()
							.extract();
					final String checkResponseBody = checkResponse.body().asString();
					log.info(checkResponseBody);
					return HttpStatus.SC_NOT_FOUND == checkResponse.statusCode();
				},
				"Checking a deleted timer is actually removed after the REST call").interval(TimeUnit.SECONDS, 10);
		// We wait here since we've seen cases in which this is the timeout after an HTTP 504 is returned,
		// and we've filed https://issues.redhat.com/browse/JBEAP-25790 + 30 secs again to support cases in which
		// execution will be resumed by the newly created pod, rather than the surviving one
		waiter.timeout(TimeUnit.SECONDS, 120).waitFor();
	}

	private void giveTheTimerEnoughTimeToExecuteOnce(final Long waitInterval, final String applicationInfo) {
		SimpleWaiter waiter = new SimpleWaiter(
				() -> !fetchExpirations(timerExpirationStoreRouteUrl + "/timer", applicationInfo).isEmpty(),
				"Waiting for at least one timer expiration to be recorded");
		waiter.timeout(TimeUnit.MILLISECONDS, waitInterval).waitFor();
	}

	private Pod getExecutor(String applicationInfo) {
		return findPodByExecutorRole(applicationInfo, true);
	}

	private Pod getNonExecutor(String applicationInfo) {
		return findPodByExecutorRole(applicationInfo, false);
	}

	private Pod findPodByExecutorRole(String applicationInfo, boolean isExecutor) {
		List<TimerExpiration> expirations = fetchExpirations(timerExpirationStoreRouteUrl + "/timer", applicationInfo);
		if (expirations.isEmpty()) {
			throw new IllegalStateException("There are no recorded timer expirations");
		}
		String executorName = expirations.stream()
				.max(Comparator.comparing(TimerExpiration::getTimestamp))
				.get().getExecutor().split("/")[0];
		log.debug("Current timer executor pod name: {}, from {} expirations", executorName, expirations.size());
		return wildflyDistributedTimersProvisioner.getPods().stream()
				.filter(p -> isExecutor == executorName.equals(p.getMetadata().getName()))
				.findFirst().get();
	}

	private List<TimerExpiration> fetchExpirations(String url, String applicationInfo) {
		return Arrays.stream(
				RestAssured
						.when()
						.get(url)
						.then()
						.assertThat()
						.statusCode(HttpStatus.SC_OK)
						.contentType(ContentType.JSON)
						.extract()
						.as(TimerExpiration[].class))
				.filter(e -> applicationInfo.equals(e.getInfo())).collect(Collectors.toList());
	}

	private void stopOriginalServicePod(Pod podToBeDeleted, final Long waitInterval) {
		// sleep a bit and then delete the pod
		log.debug("Injecting a failure by stopping pod {} in {} milliseconds", podToBeDeleted.getMetadata().getName(),
				waitInterval);
		try {
			Thread.sleep(waitInterval);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Test error. Shutting timer service down failed: " + e.getMessage());
		}
		log.debug("Pod {} is being stopped!", podToBeDeleted.getMetadata().getName());
		MASTER_SHIFT.deletePod(podToBeDeleted);
	}

	private void shutTimerServiceDown() {
		// sleep a bit and then shutdown WildFly/JBoss EAP
		try {
			Thread.sleep(RECURRING_TIMER_EXPIRATION_TIMEOUT);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Test error. Shutting the timer service down failed: " + e.getMessage());
		}
		log.debug("Injecting a failure by (gracefully) shutting the timer service down!");
		wildflyDistributedTimersProvisioner.scale(0, Boolean.TRUE);
		// wait another bit and then start the WildFly/JBoss EAP service again
		try {
			Thread.sleep(RECURRING_TIMER_EXPIRATION_TIMEOUT);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Test error. Restarting the timer service failed: " + e.getMessage());
		}
		wildflyDistributedTimersProvisioner.scale(WILDFLY_BASIC_CLUSTER_REPLICAS, Boolean.TRUE);
	}

	private boolean validateExpectedExpirations(long timeWindowDuration, long pause, List<TimerExpiration> actual) {
		long expected = timeWindowDuration / RECURRING_TIMER_EXPIRATION_TIMEOUT;
		long missed = pause / RECURRING_TIMER_EXPIRATION_TIMEOUT;
		long minExpected = expected - missed;
		log.info("Expected expirations: {}, missed during pause: {}, pause: {}ms, actual: {}",
				expected, missed, pause, actual.size());
		return actual.size() >= minExpected;
	}

	/**
	 * Starts a time window that will monitor and collect a given timer expirations asynchronously.
	 * Within such time window a failure will be injected and eventually assertions will be made on the actual number of
	 * expirations and executors.
	 *
	 * @param timeWindowDuration Duration of the monitored time window in milliseconds
	 * @param applicationInfo The timer info that will be used to identify a given timer
	 * @param failure A {@link Runnable} instance that represents the failure which is being injected within the
	 *                monitored time window
	 * @param expectedExpirationsValidator A {@link Function} that will be executed to validate the expected number of
	 *                                     expirations
	 * @param expectedExecutorsValidator A {@link Function} that will be executed to validate the expected number of
	 *                                   executors
	 *
	 * @throws InterruptedException Thrown when the main thread wait interval is interrupted unexpectedly
	 * @throws ExecutionException Thrown when an error occurs during the asynchronous monitoring method execution
	 */
	private void testFailOverWithinTimeWindow(
			final Long timeWindowDuration,
			final String applicationInfo,
			final Runnable failure,
			final BiFunction<List<TimerExpiration>, Long, Boolean> expectedExpirationsValidator,
			final Function<Long, Boolean> expectedExecutorsValidator)
			throws InterruptedException, ExecutionException {
		CompletableFuture<List<TimerExpiration>> monitorFuture = CompletableFuture.supplyAsync(
				() -> monitorTimerExpirationsInTimeWindow(timeWindowDuration, applicationInfo));
		log.debug("Started observing timer expirations for {} milliseconds", timeWindowDuration);
		failure.run();
		final List<TimerExpiration> actualExpirations = monitorFuture.get();
		log.debug("Finished observing timer expirations for {} milliseconds.", timeWindowDuration);
		if (!actualExpirations.isEmpty()) {
			log.debug(
					"The following {} expirations were collected in the monitored time interval having \"applicationInfo\" set to {}:\n{}",
					actualExpirations.size(),
					applicationInfo,
					actualExpirations.stream()
							.sorted(Comparator.comparing(TimerExpiration::getTimestamp).reversed())
							.map(Objects::toString).collect(Collectors.joining("\n---\n")));
		}
		if (actualExpirations.size() == 1) {
			throw new IllegalStateException("Only one expiration was collected, which doesn't allow to perform assertions");
		}
		// find the largest gap between consecutive expirations (the "blackout" window)
		List<TimerExpiration> sorted = actualExpirations.stream()
				.sorted(Comparator.comparing(TimerExpiration::getTimestamp))
				.collect(Collectors.toList());
		long pause = 0;
		for (int i = 1; i < sorted.size(); i++) {
			long gap = Duration.between(sorted.get(i - 1).getTimestamp(), sorted.get(i).getTimestamp()).toMillis();
			if (gap > pause)
				pause = gap;
		}
		Assertions.assertTrue(expectedExpirationsValidator.apply(actualExpirations, pause));
		Assertions.assertTrue(expectedExecutorsValidator
				.apply(actualExpirations.stream().map(TimerExpiration::getExecutor).distinct().count()));
	}

	/**
	 * Waits until enough time (i.e. equal to the provided interval) is passed, i.e. while timer are executed
	 * within a given time window.
	 *
	 * @param timeWindowDuration Number of milliseconds that identify the time window duration.
	 * @return List of {@link TimerExpiration} instances representing the timer expiration which were triggered by
	 * this test in a given time period.
	 */
	private List<TimerExpiration> monitorTimerExpirationsInTimeWindow(final Long timeWindowDuration,
			final String applicationInfo) {
		List<TimerExpiration> expirations = new ArrayList<>();
		final Instant start = Instant.now(), end = start.plusMillis(timeWindowDuration);
		final String requestUrl = timerExpirationStoreRouteUrl
				+ String.format("/timer/range?from=%s&to=%s", start, end);
		log.debug("---> Observing time window from {} to {} ({})", start, end, requestUrl);
		do {
			try {
				expirations = fetchExpirations(requestUrl, applicationInfo);
				log.debug("---> {} expirations in the monitored time window", expirations.size());
				Thread.sleep(1_000L);
			} catch (InterruptedException e) {
				throw new IllegalStateException("Test error. Observing timer expirations failed: " + e.getMessage());
			}
		} while (Instant.now().isBefore(end));
		expirations = fetchExpirations(requestUrl, applicationInfo);
		log.debug("---> Finally {} expirations in the monitored time window", expirations.size());
		return expirations;
	}
}
