/**
* Copyright (C) 2025 Red Hat, Inc.
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
package org.jboss.intersmash.tests.wildfly.web.cache.offload.infinispan;

import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.waiting.SimpleWaiter;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Pod;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matchers;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.openshift.OpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.InfinispanTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.jboss.intersmash.tests.wildfly.web.cache.offload.infinispan.util.PodHttpWithSessionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly/JBoss EAP 8.z/JBoss EAP XP 5.z and 6.x + Infinispan/Red Hat Data Grid interoperability tests.
 * <p>
 *     Validate several scenarios about a distributable application that provides REST endpoint for managing values
 *     stored in a web cache provided by a remote Infinispan/Red Hat Data Grid service.
 * </p>
 * This test is using the operator to deploy the Infinispan/Red Hat Data Grid service.
 */
@WildflyTest
@EapTest
@EapXpTest
@InfinispanTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(Infinispan2ReplicasService.class),
		@Service(WildflyOffloadingSessionsToInfinispanApplication.class)
})
public class WildflyWebCacheOffloadToInfinispanIT {

	public static final int PAUSE_TO_ALLOW_DATA_REPLICATION_IN_SECONDS = 30;

	@ServiceUrl(WildflyOffloadingSessionsToInfinispanApplication.class)
	private String wildflyApplicationRouteUrl;

	@ServiceProvisioner(WildflyOffloadingSessionsToInfinispanApplication.class)
	private OpenShiftProvisioner<WildflyOffloadingSessionsToInfinispanApplication> wildflyOpenShiftProvisioner;

	@ServiceProvisioner(Infinispan2ReplicasService.class)
	private OpenShiftProvisioner<Infinispan2ReplicasService> infinispanOpenShiftProvisioner;

	@BeforeAll
	public static void setupNamespace() {
		// KUBE_PING requires this permission, otherwise warning is logged and clustering doesn't work,
		// an invalidation-cache requires a functioning jgroups cluster.
		OpenShifts.master().addRoleToServiceAccount("view", "default");
	}

	@BeforeEach
	public void shutDownClusters() {
		wildflyOpenShiftProvisioner.scale(0, true);
		infinispanOpenShiftProvisioner.scale(0, true);
	}

	@AfterEach
	public void scaleDownClusters() {
		// Clusters of Data Grid service pods must restart with the same number of pods that existed before shutdown.
		// For example, if you shut down a cluster of 6 nodes, when you restart that cluster, you must specify 6 as
		// the value for spec.replicas.
		infinispanOpenShiftProvisioner.scale(2, true);
	}

	private void setInitialClustersReplicas() {
		infinispanOpenShiftProvisioner.scale(2, true);
		wildflyOpenShiftProvisioner.scale(2, true);
	}

	/**
	 * Verify that the web session is offloaded to the remote Infinispan/Red Hat Data Grid service.
	 * <p>
	 *     1) save value in web session cache and keep the session<br>
	 *     2) scale WidFly/JBoss EAP to 0 and back to 1<br>
	 *     3) get the value under same session ID<br>
	 *     The value stored in the distributed cache should be the same after the WidFly/JBoss EAP graceful shutdown.
	 * </p>
	 * @since JBoss EAP 7.4.x, Infinispan/Red Hat Data Grid 8.x
	 */
	@Test
	public void webCacheOffloaded() {
		setInitialClustersReplicas();
		RequestSpecification session = RestAssured.given().accept(ContentType.JSON)
				.filter(new SessionFilter());

		putValue(session, 5);
		testValue(session, 5);

		wildflyOpenShiftProvisioner.scale(0, true);
		wildflyOpenShiftProvisioner.scale(2, true);

		testValue(session, 5);
	}

	/**
	 * Verify that a web session contains the expected value when a WidFly/JBoss EAP pod is deleted.
	 * <p>
	 *     1) save value in web session cache and keep the session<br>
	 *     2) delete the WidFly/JBoss EAP pod<br>
	 *     3) get the value under same session ID, with retries to wait for pod missing heartbeat (JGroups FD_ALL3) to be detected
	 * </p>
	 * The value stored in the distributed cache should be the same after the WidFly/JBoss EAP failure.
	 * @since JBoss EAP 7.4.x, Infinispan/Red Hat Data Grid 8.x
	 */
	@Disabled("The test should be reworked in order to wait until the node removal is detected by the WildFly/JBoss EAP " +
			"cluster, and a new topology is delivered, before reading the value from the cache")
	@Test
	public void wildflyPodDeleteFailoverWithAwait() throws InterruptedException {
		setInitialClustersReplicas();
		Pod podToFail = wildflyOpenShiftProvisioner.getPods().get(0);
		log.debug("The \"{}\" pod will be deleted with no grace period to verify WildFly/JBoss EAP failover",
				podToFail.getMetadata().getName());
		RequestSpecification session = RestAssured.given().accept(ContentType.JSON).filter(new SessionFilter());

		putValue(session, 7);
		// Here we need to add a sleep period for the
		// pod deletion since it is not guaranteed that data was successfully replicated/persisted prior to abrupt pod
		// deletion, which would make the test fail intermittently.
		Thread.sleep(PAUSE_TO_ALLOW_DATA_REPLICATION_IN_SECONDS * 1000);
		testValue(session, 7);

		log.debug("Deleting \"{}\" pod", podToFail.getMetadata().getName());
		wildflyOpenShiftProvisioner.getOpenShift().deletePod(podToFail);
		log.debug("Reading value from cache with retries");
		testValue(session, 7, 60);
	}

	/**
	 * Verify that a web session contains the expected value when WidFly/JBoss EAP pod fails.
	 * <p>
	 *     1) save value in web session cache and keep the session<br>
	 *     2) make the WidFly/JBoss EAP pod crash<br>
	 *     3) get the value under same session ID
	 * 	 *
	 * </p>
	 * The value stored in the distributed cache should be the same after the WidFly/JBoss EAP failure.
	 * @since JBoss WidFly/JBoss EAP 7.4.x, Infinispan/Red Hat Data Grid 8.x
	 */
	@Test
	public void wildflyPodCrashFailover() throws InterruptedException {
		setInitialClustersReplicas();
		Pod podToFail = wildflyOpenShiftProvisioner.getPods().get(0);
		log.debug("The \"{}\" pod will be terminated ungracefully to verify WildFly/JBoss EAP failover",
				podToFail.getMetadata().getName());
		RequestSpecification session = RestAssured.given().accept(ContentType.JSON).filter(new SessionFilter());

		putValue(session, 7);
		// Here we need to add a sleep period for the
		// pod deletion since it is not guaranteed that data was successfully replicated/persisted prior to abrupt pod
		// deletion, which would make the test fail intermittently.
		Thread.sleep(PAUSE_TO_ALLOW_DATA_REPLICATION_IN_SECONDS * 1000);
		testValue(session, 7);

		log.debug("Making \"{}\" pod crash", podToFail.getMetadata().getName());
		makePodCrash(podToFail);
		log.debug("Reading value from cache with NO retries");
		testValue(session, 7);
	}

	/**
	 * Verify that a web session contains the expected value when Infinispan/Red Hat Data Grid pod is deleted.
	 * <p>
	 *     1) save value in web session cache and keep the session<br>
	 *     2) scale up Infinispan/Red Hat Data Grid to 2 nodes<br>
	 *     3) delete the first Infinispan/Red Hat Data Grid node that was originally the only one and scale down to 1<br>
	 *     4) get the value under same session ID
	 * </p>
	 * The value stored in the distributed cache should be the same after the Infinispan/Red Hat Data Grid failures.
	 * @since JBoss EAP 7.4.x, Infinispan/Red Hat Data Grid 8.x
	 */
	@Disabled("The test is using a pod deletion to simulate a service failure, which is not the correct approach.\n" +
			"It should verify that the cluster has detected it and updated the topology prior to reading the value from the cache again")
	@Test
	public void infinispanPodDeletionFailover() throws InterruptedException {
		setInitialClustersReplicas();
		List<Pod> pods = infinispanOpenShiftProvisioner.getPods();
		Pod podToFail = pods.get(0);
		log.debug("The \"{}\" pod will be terminated ungracefully to simulate Infinispan/Red Hat Data Grid failover",
				podToFail.getMetadata().getName());

		RequestSpecification session = RestAssured.given().accept(ContentType.JSON)
				.filter(new SessionFilter());

		putValue(session, 10);
		// Here we need to add a sleep period for the
		// pod deletion since it is not guaranteed that data was successfully replicated/persisted prior to abrupt pod
		// deletion, which would make the test fail intermittently.
		Thread.sleep(PAUSE_TO_ALLOW_DATA_REPLICATION_IN_SECONDS * 1000);
		testValue(session, 10);

		log.debug("Scaling Infinispan/Red Hat Data Grid cluster up to 3 replicas...");
		infinispanOpenShiftProvisioner.scale(3, true);
		//	deleting the first pod will cause the Infinispan/Red Hat Data Grid Operator to try and redeploy it
		infinispanOpenShiftProvisioner.getOpenShift().deletePod(podToFail);
		//	but here we scale down to 2, so the operator should:
		//	1. react to the #0-pod deletion by spinning it up again
		//	2. once it's ready, react to the sale down request by deleting the #1-pod
		log.debug("Scaling Infinispan/Red Hat Data Grid cluster down to 2 replicas...");
		infinispanOpenShiftProvisioner.scale(2, true);

		testValue(session, 10);
	}

	/**
	 * Verify that a web session contains the expected value when Infinispan/Red Hat Data Grid pod fails.
	 * <p>
	 *     1) save value in web session cache and keep the session<br>
	 *     2) scale up Infinispan/Red Hat Data Grid to 2 nodes<br>
	 *     3) make the first Infinispan/Red Hat Data Grid node crash, that was originally the only one and scale down to 1<br>
	 *     4) get the value under same session ID
	 * </p>
	 * The value stored in the distributed cache should be the same after the Infinispan/Red Hat Data Grid failures.
	 * @since JBoss EAP 7.4.x, Infinispan/Red Hat Data Grid 8.x
	 */
	@Test
	public void infinispanPodCrashFailover() throws InterruptedException {
		setInitialClustersReplicas();
		List<Pod> pods = infinispanOpenShiftProvisioner.getPods();
		Pod podToFail = pods.get(0);
		log.debug("The \"{}\" pod will be terminated ungracefully to simulate Infinispan/Red Hat Data Grid failover",
				podToFail.getMetadata().getName());

		RequestSpecification session = RestAssured.given().accept(ContentType.JSON)
				.filter(new SessionFilter());

		putValue(session, 10);
		// here we need to add a sleep period for the
		// pod deletion since it is not guaranteed that data was successfully replicated/persisted prior to abrupt pod
		// deletion, which would make the test fail intermittently.
		Thread.sleep(PAUSE_TO_ALLOW_DATA_REPLICATION_IN_SECONDS * 1000);
		testValue(session, 10);

		log.debug("Scaling Infinispan/Red Hat Data Grid cluster up to 3 replicas...");
		infinispanOpenShiftProvisioner.scale(3, true);
		//	killing the first pod will cause the Infinispan/Red Hat Data Grid Operator to try and redeploy it
		makePodCrash(podToFail);
		//	but here we scale down to 2, so the operator should:
		//	1. react to the #0-pod deletion by spinning it up again
		//	2. once it's ready, react to the sale down request by deleting the #1-pod
		log.debug("Scaling Infinispan/Red Hat Data Grid cluster down to 2 replicas...");
		infinispanOpenShiftProvisioner.scale(2, true);

		testValue(session, 10);
	}

	/**
	 * Verify that a value stored in the offloaded web session is consistent after a change.
	 * <p>
	 *     1) set & test value<br>
	 *     2) create cluster of 5 Infinispan/Red Hat Data Grid nodes and 3 WidFly/JBoss EAP nodes<br>
	 *     3) all WidFly/JBoss EAP pods must see the same value in websession<br>
	 *     4) change value on the first WidFly/JBoss EAP pod<br>
	 *     5) all WidFly/JBoss EAP pods must see the same value in websession<br>
	 *     6) change value on the last WidFly/JBoss EAP pod<br>
	 *     7) all WidFly/JBoss EAP pods must see the same value in websession
	 *
	 * </p>
	 * All the WidFly/JBoss EAP pods in a cluster must get the same value from the distributed cache after it is reset
	 * by a call to a specific Pod.
	 * @since JBoss EAP 7.3.z, Infinispan/Red Hat Data Grid 7.z
	 */
	@Test
	public void clusters() {
		setInitialClustersReplicas();
		Response putValueResponse = RestAssured.put(wildflyApplicationRouteUrl + "?value=2");
		if (HttpStatus.SC_OK != putValueResponse.statusCode()) {
			throw new IllegalStateException("PUT value call failed with status: " + putValueResponse.statusCode());
		}
		String sessionID = putValueResponse.getSessionId().replace("(.*)\\..*", "$1");
		PodHttpWithSessionRequest session = new PodHttpWithSessionRequest(sessionID);

		infinispanOpenShiftProvisioner.scale(5, true);
		wildflyOpenShiftProvisioner.scale(3, true);

		final Function<Pod, String> podInternalIpProducer = pod -> getPodInternalIp(pod,
				WildflyOffloadingSessionsToInfinispanApplication.WILDFLY_APP_NAME);

		final SoftAssertions assertions = new SoftAssertions();
		wildflyOpenShiftProvisioner.getPods().forEach(pod -> assertions
				.assertThat(session.get(pod, String.format("%s:8080", podInternalIpProducer.apply(pod))))
				.as("Get value from session on pod %s", pod.getMetadata().getName())
				.matches("2"));
		assertions.assertAll();

		List<Pod> pods = wildflyOpenShiftProvisioner.getPods();
		Pod pod1 = pods.get(0);
		Pod pod3 = pods.get(2);

		session.put(pod1, String.format("%s:8080?value=21", podInternalIpProducer.apply(pod1)));
		wildflyOpenShiftProvisioner.getPods().forEach(pod -> assertions
				.assertThat(session.get(pod, String.format("%s:8080", podInternalIpProducer.apply(pod))))
				.as("Get value from session on pod %s", pod.getMetadata().getName()).matches("21"));
		assertions.assertAll();

		session.put(pod3, String.format("%s:8080?value=23", podInternalIpProducer.apply(pod3)));
		wildflyOpenShiftProvisioner.getPods().forEach(pod -> assertions
				.assertThat(session.get(pod, String.format("%s:8080", podInternalIpProducer.apply(pod))))
				.as("Get value from session on pod %s", pod.getMetadata().getName()).matches("23"));
		assertions.assertAll();
	}

	private void putValue(RequestSpecification session, int value) {
		Response putValueResponse = session.put(wildflyApplicationRouteUrl + "?value=" + value);
		if (HttpStatus.SC_OK != putValueResponse.statusCode()) {
			throw new IllegalStateException("PUT value call failed with status: " + putValueResponse.statusCode());
		}
		putValueResponse.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.statusCode(200)
				.body("added", Matchers.is(String.valueOf(value)));
	}

	private void testValue(RequestSpecification session, int value) {
		session.get(wildflyApplicationRouteUrl)
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.statusCode(200)
				.body("value", Matchers.is(String.valueOf(value)));
	}

	private void testValue(RequestSpecification session, final int value, final int awaitSeconds) {
		SimpleWaiter waiter = new SimpleWaiter(
				() -> {
					final Response response = session.get(wildflyApplicationRouteUrl);
					log.info("Got response from testValue (with await): status code={}, body={}", response.getStatusCode(),
							response.getBody().asPrettyString());
					return response.statusCode() == HttpStatus.SC_OK &&
							response.getBody().asString().contains(String.valueOf(value));
				},
				"Waiting for the REST API (test value) call to return HTTP 200").interval(TimeUnit.SECONDS, 5);
		waiter.timeout(TimeUnit.SECONDS, awaitSeconds).waitFor();
	}

	private void makePodCrash(Pod pod) {
		final String podName = pod.getMetadata().getName();
		pod.getSpec().getContainers().stream().forEach(container -> {
			String outcome = OpenShifts.adminBinary().execute("exec", "-it", podName, "-c", container.getName(), "--",
					"/bin/sh", "-c", "kill -9 $(ls -l /proc/*/exe|grep java | cut -d '/' -f 3)");
			log.debug("Command for killing Java process in container {} (Pod {}) returned: {}", container.getName(), podName,
					outcome);
		});
		final String podDeletionCommandOutcome = OpenShifts.adminBinary().execute("delete", "pod", podName);
		log.debug("Command for deleting Pod {} returned: {}", podName, podDeletionCommandOutcome);
	}

	public static String getPodInternalIp(final Pod pod, final String endpointName) {
		Endpoints endpoint = OpenShifts.master().getEndpoint(endpointName);
		if (endpoint.getSubsets().isEmpty()) {
			throw new IllegalStateException(String.format("Couldn't find any '%s' Endpoints resource"));
		}
		return endpoint.getSubsets().get(0).getAddresses().stream()
				.filter(a -> "Pod".equals(a.getTargetRef().getKind())
						&& a.getTargetRef().getName().equals(pod.getMetadata().getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						String.format("Couldn't find any Pod related IP (pod name: %s)", pod.getMetadata().getName())))
				.getIp();
	}
}
