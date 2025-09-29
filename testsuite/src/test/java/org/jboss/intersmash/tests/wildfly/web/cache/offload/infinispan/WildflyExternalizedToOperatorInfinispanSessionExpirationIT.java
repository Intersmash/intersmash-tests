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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.openshift.PodShell;
import cz.xtf.core.openshift.PodShellOutput;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.Endpoints;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.assertj.core.util.Strings;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration test for WildFly session externalization and expiration using Infinispan/Red Hat Data Grid.
 * <p>
 * This test verifies that WildFly can externalize HTTP sessions to a remote Infinispan cluster deployed via the
 * Infinispan Operator, and that session expiration scheduling works correctly when sessions are stored remotely.
 * The test uses a custom certificate to secure the communication with the Infinispan endpoint.
 * </p>
 * <p>
 * The test suite covers:
 * <ul>
 *     <li>WildFly cluster formation using KUBE_PING</li>
 *     <li>Session data persistence in the remote Infinispan cluster</li>
 *     <li>Session expiration scheduling and execution for externalized sessions</li>
 * </ul>
 * </p>
 */
@WildflyTest
@EapTest
@EapXpTest
@InfinispanTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(Infinispan2ReplicasCustomCertificateService.class),
		@Service(WildflyExternalizeSessionsToInfinispanApplication.class)
})
public class WildflyExternalizedToOperatorInfinispanSessionExpirationIT {
	public final static String SESSION_CONVENTIONAL_CREATION_MESSAGE_TEMPLATE = "Session %s created, will expire in %d seconds";
	public final static String SESSION_CONVENTIONAL_EXPIRATION_MESSAGE_TEMPLATE = "Session %s destroyed";
	public final static Integer SESSION_CONVENTIONAL_EXPIRATION_TIMEOUT_SECONDS = 120;

	private static RequestConfig globalConfig;
	private static CookieStore cookieStore;
	private static HttpClientContext httpClientContext;
	private static CloseableHttpClient httpClient;

	@ServiceUrl(WildflyExternalizeSessionsToInfinispanApplication.class)
	private String wildflyApplicationRouteUrl;

	@ServiceProvisioner(WildflyExternalizeSessionsToInfinispanApplication.class)
	private OpenShiftProvisioner<WildflyExternalizeSessionsToInfinispanApplication> wildflyOpenShiftProvisioner;

	@ServiceProvisioner(Infinispan2ReplicasCustomCertificateService.class)
	private OpenShiftProvisioner<Infinispan2ReplicasCustomCertificateService> infinispanOpenShiftProvisioner;

	/**
	 * Sets up the OpenShift namespace and HTTP client configuration.
	 * <p>
	 * This method grants the 'view' role to the 'default' service account to enable KUBE_PING clustering,
	 * and configures an HTTP client with cookie preservation for session tracking across requests.
	 * </p>
	 */
	@BeforeAll
	public static void setupNamespace() {
		// KUBE_PING requires this permission, otherwise warning is logged and clustering doesn't work,
		// an invalidation-cache requires a functioning jgroups cluster.
		OpenShifts.master().addRoleToServiceAccount("view", "default");
		// to have cookies preserved across subsequent invocations
		globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
		cookieStore = new BasicCookieStore();
		httpClientContext = HttpClientContext.create();
		httpClientContext.setCookieStore(cookieStore);
		httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
	}

	/**
	 * Test that WildFly cluster has been formed.<br>
	 * 1) access the deployed application to read what the "members" servlet returns<br>
	 * 2) check that the number of members is actually 2
	 * Test passes if the number of cluster members is 2.
	 */
	@Test
	public void testClusterIsFormed() throws IOException, InterruptedException {
		wildflyOpenShiftProvisioner.scale(2, true);
		String responseBody = accessApplicationRouteAndGetResponseBody(
				String.format("%s/%s", wildflyApplicationRouteUrl, "members"));
		Assertions.assertTrue(responseBody != null, "Response from \"members\" servlet is empty!");
		Assertions.assertTrue(responseBody.split("Members:").length > 1,
				"Response from \"members\" servlet doesn't contain cluster members!");
		Assertions.assertTrue(responseBody.split("Members:")[1].split(",").length > 1,
				String.format("Response from \"members\" servlet should contain 2 cluster members: %s!",
						responseBody.split("Members:")[1]));
	}

	/**
	 * Test that session replication works as expected and that the Infinispan cluster actually stores session data.<br>
	 * 1) access the application to create a session and gets the serial number<br>
	 * 2) check that serial number is 0<br>
	 * 3) scale down to 0 and back up to 1 replicas<br>
	 * 4) access the application to create a session and gets the serial number<br>
	 * 5) check that serial number is 1<br>
	 * Test passes if serial number is incremented and stored inside Infinispan cluster across re-start.
	 */
	@Test
	public void testClusterIsConnectedToInfinispan() throws IOException, InterruptedException {
		wildflyOpenShiftProvisioner.scale(2, true);

		String responseBody = accessApplicationRouteAndGetResponseBody(
				String.format("%s/%s", wildflyApplicationRouteUrl, "members"));
		Assertions.assertTrue(responseBody != null, "Response from \"members\" servlet is empty!");
		Assertions.assertTrue(responseBody.split("Members:").length > 1,
				"Response from \"members\" servlet doesn't contain cluster members!");
		Assertions.assertTrue(responseBody.split("Members:")[1].split(",").length > 1,
				String.format("Response from \"members\" servlet should contain 2 cluster members: %s!",
						responseBody.split("Members:")[1]));

		responseBody = accessApplicationRouteAndGetResponseBody(String.format("%s/%s", wildflyApplicationRouteUrl, "serial"));
		Assertions.assertTrue(responseBody != null, "Response from \"serial\" servlet doesn't contain serial number!");
		Assertions.assertTrue(responseBody.trim().matches("^[0-9]+$"),
				"Response from \"serial\" servlet doesn't contain serial number!");

		// Should be 0
		Integer serial = Integer.parseInt(responseBody);
		Assertions.assertEquals(0, serial, "Response from \"serial\" servlet returned a serial number other than 0!");

		wildflyOpenShiftProvisioner.scale(0, true);
		wildflyOpenShiftProvisioner.scale(1, true);

		responseBody = accessApplicationRouteAndGetResponseBody(String.format("%s/%s", wildflyApplicationRouteUrl, "serial"));
		Assertions.assertTrue(responseBody != null, "Response from \"serial\" servlet is empty!");
		Assertions.assertTrue(responseBody.trim().matches("^[0-9]+$"),
				"Response from \"serial\" servlet doesn't contain serial number!");

		// Should be 1
		serial = Integer.parseInt(responseBody);
		Assertions.assertEquals(1, serial, "Response from \"serial\" servlet returned a serial number other than 1!");
	}

	/**
	 * Test that session expiration scheduling works as expected.<br>
	 * 1) access the application twice to create a session on each node<br>
	 * 2) check that session expiration was scheduled locally on each node<br>
	 * 3) scale down to 0 and back up to a single replica<br>
	 * 4) check that all the sessions get scheduled for expiration after the scale-up
	 * Test passes if sessions are scheduled and correctly expire as expected.
	 */
	@Test
	public void testExpirations() throws InterruptedException {
		infinispanOpenShiftProvisioner.scale(1, true);
		wildflyOpenShiftProvisioner.scale(2, true);

		// In the ideal world we would only execute
		//
		//String sessionId1 = RestAssured.get(wildflyRouteUrl + "/session")
		//		.then()
		//		.statusCode(HttpStatus.SC_OK)
		//		.log()
		//		.ifValidationFails(LogDetail.ALL, true)
		//		.assertThat()
		//		.body(Matchers.containsString("Session"))
		//		.extract().asString().substring(8);
		//
		//String sessionId2 = RestAssured.get(wildflyRouteUrl + "/session")
		//		.then()
		//		.statusCode(HttpStatus.SC_OK)
		//		.log()
		//		.ifValidationFails(LogDetail.ALL, true)
		//		.assertThat()
		//		.body(Matchers.containsString("Session"))
		//		.extract().asString().substring(8);
		//
		// but although endpoint is up to date, sometimes http requests end on the same WildFly/JBoss EAP node,
		//	if accessed right after scale up, test expects them to end on different nodes.
		// I don't know what else we could check, scale() waits until
		//   - pods are ready
		//   - endpoint contains addresses of all pods
		//   - route is up
		//
		// Default strategy is round robin (https://kubernetes.io/docs/concepts/services-networking/service/#proxy-mode-userspace) (verified also manually)
		//
		// We could wait a little bit
		//
		// Thread.sleep(3000); // 2 seconds is not enough
		//
		// or expose and call each node directly, or call each using internal address from within OpenShift

		List<String> ips = getWildFlyClusterInternalIps();

		// run curl inside cluster on RHDG pod
		final String sessionId1 = accessApplicationOnPodAndGetSessionId(ips.get(0));
		final String sessionId2 = accessApplicationOnPodAndGetSessionId(ips.get(1));
		// session expiration timeout as per LoggingHttpSessionListener
		final Integer sessionExpirationTimeoutInSeconds = SESSION_CONVENTIONAL_EXPIRATION_TIMEOUT_SECONDS;
		// session creation message as per LoggingHttpSessionListener
		final String sessionCreationMessageTemplate = SESSION_CONVENTIONAL_CREATION_MESSAGE_TEMPLATE;
		final String session1CreationMsg = String.format(sessionCreationMessageTemplate, sessionId1,
				sessionExpirationTimeoutInSeconds);
		final String session2CreationMsg = String.format(sessionCreationMessageTemplate, sessionId2,
				sessionExpirationTimeoutInSeconds);
		// session expiration message as per LoggingHttpSessionListener
		final String sessionExpirationMessageTemplate = SESSION_CONVENTIONAL_EXPIRATION_MESSAGE_TEMPLATE;
		final String session1ExpirationMsg = String.format(sessionExpirationMessageTemplate, sessionId1);
		final String session2ExpirationMsg = String.format(sessionExpirationMessageTemplate, sessionId2);

		Matcher<String> sessionIdMatcher = allOf(
				is(either(containsString(session1CreationMsg)).or(containsString(session2CreationMsg))),
				is(not(allOf(containsString(sessionId1), containsString(sessionId2)))));

		wildflyOpenShiftProvisioner.getPods().stream().forEach(pod -> {
			MatcherAssert.assertThat("Session expirations should be scheduled locally for each node.",
					wildflyOpenShiftProvisioner.getOpenShift().getPodLog(pod), sessionIdMatcher);
		});

		wildflyOpenShiftProvisioner.scale(0, true);
		wildflyOpenShiftProvisioner.scale(1, true);

		Matcher<String> expirationMatcher = allOf(
				containsString(session1ExpirationMsg),
				containsString(session2ExpirationMsg));

		// let's wait for the sessions to expire
		Thread.sleep(sessionExpirationTimeoutInSeconds * 1_000);

		// and eventually look for the session expiration message
		MatcherAssert.assertThat("Sessions in remote cache are not expiring after restart.",
				wildflyOpenShiftProvisioner.getOpenShift()
						.getPodLog(WildflyExternalizeSessionsToInfinispanApplication.WILDFLY_APP_NAME),
				expirationMatcher);
	}

	/**
	 * Retrieves the internal IP addresses of all WildFly cluster members.
	 *
	 * @return a list of internal IP addresses for the WildFly pods
	 */
	private List<String> getWildFlyClusterInternalIps() {
		Endpoints endpoint = OpenShifts.master()
				.getEndpoint(WildflyExternalizeSessionsToInfinispanApplication.WILDFLY_APP_NAME);
		return endpoint.getSubsets().get(0).getAddresses().stream().map(EndpointAddress::getIp)
				.collect(Collectors.toList());
	}

	/**
	 * Accesses the WildFly application from within the cluster and retrieves the session ID.
	 * <p>
	 * This method executes a curl command from within a WildFly pod to access another pod directly
	 * using its internal IP address, ensuring the request is routed to a specific cluster member.
	 * </p>
	 *
	 * @param ip the internal IP address of the WildFly pod to access
	 * @return the session ID returned by the application
	 * @throws IllegalStateException if the HTTP call fails
	 */
	private String accessApplicationOnPodAndGetSessionId(String ip) {
		PodShell podShell = OpenShifts.master()
				.podShell(WildflyExternalizeSessionsToInfinispanApplication.WILDFLY_APP_NAME);
		PodShellOutput podShellOutput = podShell.executeWithBash("curl " + ip + ":8080/session -s");
		if (!Strings.isNullOrEmpty(podShellOutput.getError())) {
			throw new IllegalStateException("HTTP call failed with error: " + podShellOutput.getError());

		}
		return podShellOutput.getOutputAsMap(":").get("Session").trim();
	}

	/**
	 * Accesses the WildFly application via the OpenShift route and retrieves the response body.
	 * <p>
	 * This method uses an HTTP client with cookie preservation to maintain session state across requests.
	 * </p>
	 *
	 * @param route the full URL of the application route to access
	 * @return the response body as a string
	 * @throws IOException if an I/O error occurs during the HTTP request
	 */
	private String accessApplicationRouteAndGetResponseBody(String route) throws IOException {
		HttpGet httpGet = new HttpGet(route);
		CloseableHttpResponse response = httpClient.execute(httpGet, httpClientContext);
		String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
		log.info("Response: {}", responseString);
		return responseString;
	}
}
