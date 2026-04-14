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
import cz.xtf.junit5.annotations.OpenShiftRecorder;
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
 * <p>
 * The tests will employ the following workflow:<br>
 *         1. start a minimal (0-0) WildFly/Infinispan cluster<br>
 *         2. scale up to a basic (2-2) WildFly/Infinispan cluster<br>
 *         ... do something<br>
 *         3. eventually scale back to the minimal cluster form again to let Infinispan have the exact number of
 *         replicas (1) that will be expected at the following restart.<br>
 */
@Intersmash({
		@Service(PostgresqlTimerExpirationStoreApplication.class),
		@Service(WildFlyTimerExpirationStoreApplication.class),
		@Service(InfinispanOperatorWithExternalRouteApplication.class),
		@Service(WildFlyDistributedTimersApplication.class)
})
@OpenShiftRecorder(resourceNames = {
		WildFlyTimerExpirationStoreApplication.NAME,
		WildFlyDistributedTimersApplication.NAME
})
@WildflyTest
@EapTest
@EapXpTest
@InfinispanTest
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(ProjectCreator.class)
public class WildFlyInfinispanDistributedTimersIT {

	private static final OpenShift MASTER_SHIFT = OpenShifts.master();
	private static final Long RECURRING_TIMER_EXPIRATION_TIMEOUT = 10_000L;
	private static final Long RECURRING_TIMER_INITIAL_DELAY = 0L;
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
	 * Checks that the WildFly/JBoss EAP application service cache has been successfully created, by
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
	 * No timer expirations have been recorded in the monitored time period.
	 *
	 * @throws InterruptedException Thrown when the main thread wait interval is interrupted unexpectedly
	 * @throws ExecutionException Thrown when an error occurs during the asynchronous monitoring method execution
	 */
	@Test
	@Order(2)
	public void testTimerCanBeSuccessfullyCancelledClusterWide() throws InterruptedException, ExecutionException {
		final Long expirationTimeout = RECURRING_TIMER_EXPIRATION_TIMEOUT;
		// set a time window
		final Long timeWindowDuration = SHORT_TIME_WINDOW_DURATION_SECONDS * 1_000L;
		// now, let's forge the timer info (the server will use such id to delete it)
		final String applicationInfo = String.format("%s:%s", RECURRING_TIMER_INFO,
				"testTimerCanBeSuccessfullyCancelledClusterWide");
		// scale up to the basic form so that we have a non executor
		basicClusters();
		try {
			Pod nonExecutor = null;
			createTimer(RECURRING_TIMER_INITIAL_DELAY, expirationTimeout, applicationInfo);
			try {
				// let's give the timer some time to execute
				final Long waitInterval = 2 * expirationTimeout;
				giveTheTimerEnoughTimeToExecuteOnce(waitInterval, applicationInfo);
				nonExecutor = getNonExecutor(applicationInfo);
				Assertions.assertNotNull(nonExecutor, "Expected non-executing pod was not found after timer creation");
				log.debug("Current non-executing pod is {}", nonExecutor.getMetadata().getName());
			} finally {
				if (nonExecutor == null) {
					// this can happen when an exception is thrown and the non executor is null: we need to cancel the
					// timer anyway, the flow will jump out of the test method itself
					cancelTimer(applicationInfo);
				} else {
					cancelTimer(nonExecutor, applicationInfo);
				}
			}
			// let's check the timer doesn't execute anymore
			// define the failure we want to inject (none, BTW)
			final Runnable injectedFailure = () -> log
					.info("No failure is injected when checking that a timer has been cancelled");
			// timer expirations are expected to be exactly 0
			final BiFunction<List<TimerExpiration>, Long, Boolean> expirationsValidator = (actualExpirations,
					pause) -> actualExpirations.isEmpty();
			// timer expirations are expected to be from exactly 0 executors, since the timer should have been cancelled
			final Function<Long, Boolean> executorsValidator = (actualExecutors) -> actualExecutors == 0;
			testFailOverWithinTimeWindow(timeWindowDuration, applicationInfo, injectedFailure, expirationsValidator,
					executorsValidator);
		} finally {
			minimalClusters();
		}
	}

	/**
	 * Verify a (distributed) timers persistence service fail-over scenario. One minimal (0-0)
	 * WildFly/Infinispan cluster is started, the initial Infinispan pod is marked for deletion. Then a distributed persistent interval
	 * timer is started, after which the cluster is scaled up to its basic (2-2) form. Now timer expirations will be
	 * counted asynchronously for a given time period, during which a failure will be injected, i.e. the initial Infinispan
	 * will be stopped. At the end of the given monitoring period, the list of expirations is checked to perform
	 * assertions about the persistence service fail-over capabilities.
	 * <br>
	 * The expected timer expirations by <i>exactly</i> one executor have been recorded in the monitored
	 * time period.
	 */
	@Test
	@Order(3)
	public void testPersistenceServiceBasicFailOver() throws InterruptedException, ExecutionException {
		final Long expirationTimeout = RECURRING_TIMER_EXPIRATION_TIMEOUT;
		// set a time window (duration + 2 more expirations in order to be sure...)
		final Long timeWindowDuration = LONG_TIME_WINDOW_DURATION_SECONDS * 1_000L + (expirationTimeout * 2);
		// and expectations:
		// timer expirations are expected to be from the original executor, since only Infinispan will be restarted
		final Function<Long, Boolean> executorsValidator = (actualExecutors) -> actualExecutors == 1;
		// calculate expected timer expirations
		final Long computedExpirations = timeWindowDuration / expirationTimeout;
		final BiFunction<List<TimerExpiration>, Long, Boolean> expirationsValidator = (actualExpirations,
				pause) -> validateExpectedExpirations(computedExpirations, pause, expirationTimeout, actualExpirations);
		// start clusters
		basicClusters();
		try {
			// now, let's forge the timer info (the server will use such id to delete it)
			final String applicationInfo = String.format("%s:%s", RECURRING_TIMER_INFO, "testPersistenceServiceBasicFailOver");
			// trigger the recurring timer creation on distributed timers WildFly/JBoss EAP app
			createTimer(RECURRING_TIMER_INITIAL_DELAY, expirationTimeout, applicationInfo);
			try {
				// let's give the timer some time to execute
				final Long waitInterval = 2 * expirationTimeout;
				giveTheTimerEnoughTimeToExecuteOnce(waitInterval, applicationInfo);
				// let's log the executed timers, just for debugging purpose
				final Pod timerExecutor = getExecutor(applicationInfo);
				log.debug("Current timer executor is {}", timerExecutor.getMetadata().getName());
				// get a reference to the pod that's going to be stopped
				Pod podToBeDeleted = infinispanProvisioner.getPods().get(0);
				Assertions.assertNotNull(podToBeDeleted, "Infinispan Pod to be deleted was not found");
				log.debug("Current Infinispan Pod designated for deletion is {}", podToBeDeleted.getMetadata().getName());
				// define the failure we want to inject
				final Long intervalBeforeStoppingThePod = expirationTimeout * 2;
				final Runnable injectedFailure = () -> stopOriginalServicePod(podToBeDeleted, intervalBeforeStoppingThePod);
				// inspect the time window for expected results
				testFailOverWithinTimeWindow(timeWindowDuration, applicationInfo, injectedFailure, expirationsValidator,
						executorsValidator);
			} finally {
				// always cancel a previously created timer
				cancelTimer(applicationInfo);
			}
		} finally {
			// otherwise Infinispan won't restart because of spec.replicasWantedAtRestart
			minimalClusters();
		}
	}

	/**
	 * Verifies a EJB (distributed) timer service fail-over scenario. One minimal (0-0)
	 * EPA/Infinispan cluster is started, then the cluster is scaled up to its basic (2-2) form. Now timer expirations will be
	 * counted asynchronously for a given time period, during which a failure will be injected, i.e. the actual executor
	 * will be stopped. At the end of the given monitoring period, the list of expirations is checked to perform
	 * assertions about the timer service fail-over capabilities.
	 * <br>
	 * The expected timer expirations by <i>more than one</i> executor have been recorded in the monitored
	 * time period.
	 */
	@Test
	@Order(4)
	public void testTimerServiceBasicFailOver() throws InterruptedException, ExecutionException {
		final Long initialDelay = RECURRING_TIMER_INITIAL_DELAY,
				expirationTimeout = RECURRING_TIMER_EXPIRATION_TIMEOUT;
		// set a time window (duration + 2 more expirations in order to be sure...)
		final Long timeWindowDuration = LONG_TIME_WINDOW_DURATION_SECONDS * 1_000L + (expirationTimeout * 2);
		// and expectations...
		// timer expirations are expected to be from >= 1 different executors. Initially it will be the one which
		// scheduled the timer expiration after the scale up (topology change) to the basic cluster form.
		// At such point the actual executor is stopped, hence the timer expiration could be scheduled by:
		// - yet the original one
		// - the remaining one or by the one created by OpenShift to replace the stopped one, bringing the total number
		// of executors to 2.
		final Function<Long, Boolean> executorsValidator = (actualExecutors) -> actualExecutors >= 1;
		// calculate expected timer expirations
		final Long computedExpirations = timeWindowDuration / expirationTimeout;
		final BiFunction<List<TimerExpiration>, Long, Boolean> expirationsValidator = (actualExpirations,
				pause) -> validateExpectedExpirations(computedExpirations, pause, expirationTimeout, actualExpirations);
		// now, let's forge the timer info (the server will use such id to delete it)
		final String applicationInfo = String.format("%s:%s", RECURRING_TIMER_INFO, "testTimerServiceBasicFailOver");
		// scale up from minimal to basic configuration
		basicClusters();
		try {
			// trigger the recurring timer creation on distributed timers WildFly/JBoss EAP app
			createTimer(initialDelay, expirationTimeout, applicationInfo);
			try {
				// let's give the timer some time to execute
				final Long waitInterval = 2 * expirationTimeout;
				giveTheTimerEnoughTimeToExecuteOnce(waitInterval, applicationInfo);
				// now one of the two instances is executing the timer, let's discover which one is actually
				final Pod podToBeDeleted = getExecutor(applicationInfo);
				Assertions.assertNotNull(podToBeDeleted, "Expected timer executor was not found after timer creation");
				log.debug("Current timer executor is {}", podToBeDeleted.getMetadata().getName());
				// define the failure we want to inject
				final Long intervalBeforeStoppingThePod = expirationTimeout + (expirationTimeout / 2);
				final Runnable injectedFailure = () -> stopOriginalServicePod(podToBeDeleted, intervalBeforeStoppingThePod);
				testFailOverWithinTimeWindow(timeWindowDuration, applicationInfo, injectedFailure, expirationsValidator,
						executorsValidator);
			} finally {
				// always cancel a previously created timer
				cancelTimer(applicationInfo);
			}
		} finally {
			// otherwise Infinispan won't restart because of spec.replicasWantedAtRestart
			minimalClusters();
		}
	}

	/**
	 * Verifies a (distributed) timers persistence service fail-over scenario. One minimal (0-0)
	 * WildFly/Infinispan cluster is started and then scaled to its basic (2-2) form.
	 * Then a distributed persistent interval timer is created, which expirations will be counted asynchronously for a
	 * given time period, during which a failure will be injected, i.e. the WildFly/JBoss EAP cluster will be gracefully shut down,
	 * and eventually restarted.
	 * At the end of the given monitoring period, the list of expirations is checked to perform assertions about the
	 * persistence service fail-over capabilities.
	 * <br>
	 * The expected timer expirations by <i>more than</i> one executor have been recorded in the monitored
	 * time period.
	 */
	@Test
	@Order(5)
	public void testPersistentTimersSurviveTimerServiceTemporaryShutdown() throws InterruptedException, ExecutionException {
		final Long initialDelay = RECURRING_TIMER_INITIAL_DELAY,
				expirationTimeout = RECURRING_TIMER_EXPIRATION_TIMEOUT;
		// set a time window (duration + 2 more expirations in order to be sure...)
		final Long timeWindowDuration = LONG_TIME_WINDOW_DURATION_SECONDS * 1_000L + (expirationTimeout * 2);
		// and expectations...
		// timer expirations are expected to be from > 1 executor, since the original one will stop because of the
		// shutdown, then two more topology related re-balances occur when restoring the timer service
		final Function<Long, Boolean> executorsValidator = (actualExecutors) -> actualExecutors > 1;
		// calculate expected timer expirations
		final Long computedExpirations = timeWindowDuration / expirationTimeout;
		final BiFunction<List<TimerExpiration>, Long, Boolean> expirationsValidator = (actualExpirations,
				pause) -> validateExpectedExpirations(computedExpirations, pause, expirationTimeout, actualExpirations);
		// define the failure we'll inject within the observed time window
		final Runnable injectedFailure = this::shutTimerServiceDown;
		// now, let's forge the timer info (the server will use such id to delete it)
		final String applicationInfo = String.format("%s:%s", RECURRING_TIMER_INFO,
				"testPersistentTimersSurviveTimerServiceTemporaryShutdown");
		// scale up from minimal to basic configuration
		basicClusters();
		try {
			createTimer(initialDelay, expirationTimeout, applicationInfo);
			try {
				// let's give the timer some time to execute
				final Long waitInterval = 2 * expirationTimeout;
				giveTheTimerEnoughTimeToExecuteOnce(waitInterval, applicationInfo);
				testFailOverWithinTimeWindow(timeWindowDuration, applicationInfo, injectedFailure, expirationsValidator,
						executorsValidator);
			} finally {
				// always cancel a previously created timer
				cancelTimer(applicationInfo);
			}
		} finally {
			// always back to minimal, otherwise Infinispan won't restart because of spec.replicasWantedAtRestart
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

	private void createTimer(final Long initialDelay, final Long expirationTimeout, final String applicationInfo) {
		log.info("About to CREATE a timer by calling: " + wildflyDistributedTimersRouteUrl);
		RestAssured
				.given()
				.queryParam("initialDelay", initialDelay)
				.queryParam("expirationInterval", expirationTimeout)
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
				"Checking a deleted timer is actually removed after the REST call").interval(TimeUnit.SECONDS, 5);
		// We wait here since we've seen cases in which this is the timeout after an HTTP 504 is returned,
		// and we've filed https://issues.redhat.com/browse/JBEAP-25790 + 30 secs again to support cases in which
		// execution will be resumed by the newly created pod, rather than the surviving one
		waiter.timeout(TimeUnit.SECONDS, 60).waitFor();
	}

	private void giveTheTimerEnoughTimeToExecuteOnce(final Long waitInterval, final String applicationInfo) {
		SimpleWaiter waiter = new SimpleWaiter(
				() -> !retrieveExpirations(applicationInfo).isEmpty(),
				"Waiting for at least one timer expiration to be recorded");
		waiter.timeout(TimeUnit.MILLISECONDS, waitInterval).waitFor();
	}

	private Pod getExecutor(String applicationInfo) {
		final List<TimerExpiration> latestExpirations = retrieveExpirations(applicationInfo);
		final String currentExecutorName = retrieveExecutorName(latestExpirations);
		final Pod podToBeDeleted = retrieveExecutor(currentExecutorName);
		return podToBeDeleted;
	}

	private Pod getNonExecutor(String applicationInfo) {
		final List<TimerExpiration> latestExpirations = retrieveExpirations(applicationInfo);
		final String currentExecutorName = retrieveExecutorName(latestExpirations);
		final Pod nonExecutor = retrieveNonExecutor(currentExecutorName);
		return nonExecutor;
	}

	private String retrieveExecutorName(final List<TimerExpiration> expirations) {
		if (expirations.isEmpty()) {
			throw new IllegalStateException("There are no recorded timer expirations");
		}
		// sort DESC
		final List<TimerExpiration> sortedExpirations = expirations.stream()
				.sorted((b, a) -> a.getTimestamp().compareTo(b.getTimestamp()))
				.collect(Collectors.toList());
		log.debug("Retrieving current timer executor pod name, the following {} expirations were collected so far:\n---\n{}",
				expirations.size(),
				sortedExpirations.stream()
						.map(Objects::toString).collect(Collectors.joining("\n---\n")));
		// MUST work, since not empty
		return sortedExpirations.stream().findFirst().get().getExecutor().split("/")[0];
	}

	private Pod retrieveExecutor(final String currentExecutorName) {
		final Pod actualExecutor = wildflyDistributedTimersProvisioner.getPods().stream()
				.filter(p -> currentExecutorName.equals(p.getMetadata().getName()))
				.findFirst().get();
		return actualExecutor;
	}

	private Pod retrieveNonExecutor(final String currentExecutorName) {
		final Pod actualExecutor = wildflyDistributedTimersProvisioner.getPods().stream()
				.filter(p -> !currentExecutorName.equals(p.getMetadata().getName()))
				.findFirst().get();
		return actualExecutor;
	}

	private List<TimerExpiration> retrieveExpirations(final String applicationInfo) {
		return Arrays.stream(
				RestAssured
						.when()
						.get(timerExpirationStoreRouteUrl + "/timer")
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

	private boolean validateExpectedExpirations(Long expected, Long pause, Long interval, List<TimerExpiration> actual) {
		// expected without injected failure - (missed (i.e. pause/interval) + 1 coalesced expiration)
		long missed = pause / interval;
		final long actualExpectation = expected - missed + 1;
		log.info(String.format("Expected expirations: %d, missed during pause: %d, pause: %d, interval: %d, actual: %d",
				expected, missed, pause, interval, actual.size()));
		return actualExpectation == (long) actual.size();
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
		final List<TimerExpiration> actualExpirations = executeMonitoredWorkflow(timeWindowDuration, applicationInfo, failure);
		long pause = 0;
		// ok, let's assert...
		log.debug("Finished observing timer expirations for {} milliseconds.", timeWindowDuration);
		if (!actualExpirations.isEmpty()) {
			log.debug(
					"The following {} expirations were collected in the monitored time interval having \"applicationInfo\" set to {}:\n{}",
					actualExpirations.size(),
					applicationInfo,
					actualExpirations.stream()
							.sorted((b, a) -> a.getTimestamp().compareTo(b.getTimestamp()))
							.map(Objects::toString).collect(Collectors.joining("\n---\n")));
		}
		if (actualExpirations.size() == 1) {
			throw new IllegalStateException("Only one expiration was collected, which doesn't allow to perform assertions");
		}
		// let's assert the number of timer expirations
		List<TimerExpiration> sorted = actualExpirations.stream()
				.sorted(Comparator.comparing(TimerExpiration::getTimestamp).reversed())
				.collect(Collectors.toList());
		// store the maximum interval between ticks, as the supposed "black-out" window - just 1 (coalesced tick)
		// should be executed in such time
		for (int i = 0; i < actualExpirations.size() - 1; i++) {
			final long interval = Duration.between(Instant.from(sorted.get(i + 1).getTimestamp()),
					Instant.from(sorted.get(i).getTimestamp())).toMillis();
			if (interval > pause)
				pause = interval;
		}
		Assertions.assertTrue(expectedExpirationsValidator.apply(actualExpirations, pause));
		// also, let's assert that timer expirations are by a given number of executors
		Assertions.assertTrue(expectedExecutorsValidator
				.apply(actualExpirations.stream().map(TimerExpiration::getExecutor).distinct().count()));
	}

	/**
	 *
	 * @param timeWindowDuration Duration of the monitored time window in milliseconds
	 * @param applicationInfo The timer info that will be used to identify a given timer
	 * @param failure A {@link Runnable} instance that represents the failure which is being injected within the
	 *                monitored time window
	 * @return A list of {@link TimerExpiration} instances, representing a given timer expirations occurred within
	 * the monitored time window
	 *
	 * @throws InterruptedException Thrown when the main thread wait interval is interrupted unexpectedly
	 * @throws ExecutionException Thrown when an error occurs during the asynchronous monitoring method execution
	 */
	private List<TimerExpiration> executeMonitoredWorkflow(
			Long timeWindowDuration, String applicationInfo, Runnable failure) throws InterruptedException, ExecutionException {
		// start an asynchronous time window that will return the expirations
		CompletableFuture<List<TimerExpiration>> completableFuture = CompletableFuture.supplyAsync(
				() -> monitorTimerExpirationsInTimeWindow(timeWindowDuration, applicationInfo));
		log.debug("Started observing timer expirations for {} milliseconds:", timeWindowDuration);
		// execute failure workflow
		failure.run();
		// wait until the time window will pass, meantime timers should be expiring again and filling the time window
		// results
		while (!completableFuture.isDone()) {
			// it is ok to sleep for 1 sec given the granularity of this test (several seconds time windows)
			Thread.sleep(1_000L);
			log.debug("Still observing timer expirations...");
		}
		return completableFuture.get();
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
		// set the start and end of a time window
		final Instant start = Instant.now(), end = start.plusMillis(timeWindowDuration);
		final String requestUrl = timerExpirationStoreRouteUrl
				+ String.format("/timer/range?from=%s&to=%s", start, end);
		log.debug("---> Observing time window from {} to {} ({})", start, end, requestUrl);
		do {
			// wait until enough time (i.e. greater than the above window) is passed, so the window is reliable
			try {
				expirations = Arrays.stream(
						RestAssured
								.when()
								.get(requestUrl)
								.then()
								.assertThat()
								.statusCode(HttpStatus.SC_OK)
								.contentType(ContentType.JSON)
								.extract()
								.as(TimerExpiration[].class))
						.filter(e -> applicationInfo.equals(e.getInfo())).collect(Collectors.toList());
				log.debug(
						"---> There are currently {} expirations in the monitored time window",
						expirations.size());
				// it is ok to sleep for 1 sec given the granularity of this test (several seconds time windows)
				Thread.sleep(1_000L);
			} catch (InterruptedException e) {
				throw new IllegalStateException("Test error. Observing timer expirations failed: " + e.getMessage());
			}
		} while (Instant.now().isBefore(end));
		// now fetch executions for the last time in the conventional time window from the EJB Timer Expiration Store app
		expirations = Arrays.stream(
				RestAssured
						.when()
						.get(requestUrl)
						.then()
						.assertThat()
						.statusCode(HttpStatus.SC_OK)
						.contentType(ContentType.JSON)
						.extract()
						.as(TimerExpiration[].class))
				.filter(e -> applicationInfo.equals(e.getInfo())).collect(Collectors.toList());
		log.debug(
				"---> There are finally {} expirations in the monitored time window",
				expirations.size());
		return expirations;
	}
}
