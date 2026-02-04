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
package org.jboss.intersmash.tests.wildfly.keycloak.saml.adapter;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import cz.xtf.core.http.Https;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.junit5.annotations.OpenShiftRecorder;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.fabric8.kubernetes.api.model.Pod;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.KeycloakTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.jboss.intersmash.tests.wildfly.elytron.oidc.client.keycloak.KeycloakPostgresqlApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for WildFly/JBoss EAP with Keycloak SAML Adapter.
 * <p>
 * This test class validates the integration between WildFly/JBoss EAP and Keycloak/RHBK
 * using SAML-based authentication. It tests:
 * <ul>
 *   <li>Automatic SAML client registration with Keycloak</li>
 *   <li>Successful authentication and access to protected resources</li>
 *   <li>Role-based access control and forbidden access scenarios</li>
 * </ul>
 * </p>
 * <p>
 * The test deploys a complete environment with PostgreSQL, Keycloak, and a
 * SAML-secured WildFly/JBoss EAP application, verifying the entire authentication flow.
 * </p>
 */
@OpenShiftRecorder(resourceNames = { BasicKeycloakOperatorDynamicClientSamlApplication.APP_NAME,
		WildflyWithKeycloakSamlAdapterEjbApplication.APP_NAME })
@KeycloakTest
@WildflyTest
@EapTest
@EapXpTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(KeycloakPostgresqlApplication.class),
		@Service(BasicKeycloakOperatorDynamicClientSamlApplication.class),
		@Service(WildflyWithKeycloakSamlAdapterEjbApplication.class)
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WildFlyKeycloakSamlAdapterEjbIT {
	/** HTML element ID containing the authenticated username from the Servlet layer . */
	private static final String HTML_ID_WITH_USERNAME = "username";
	/** HTML element ID containing the authenticated username from the EJB layer. */
	private static final String HTML_ID_WITH_USERNAME_EJB = "usernameEjb";

	/** Route URL for the WildFly/JBoss EAP application. */
	@ServiceUrl(WildflyWithKeycloakSamlAdapterEjbApplication.class)
	private String wildflyApplicationRouteUrl;

	private static boolean samlClientCreated = false;
	private static boolean wildflyApplicationIsRunning = false;

	/**
	 * Verifies that the SAML client has been automatically created in Keycloak.
	 * <p>
	 * This test checks the WildFly/JBoss EAP pod logs to confirm that the SAML client
	 * was successfully registered with the Keycloak realm after the "routeview"
	 * role was added to the service account.
	 * </p>
	 */
	@Test
	@Order(1)
	public void testSamlClientCreation() {
		Pod wildflyPod = OpenShifts.master().getLabeledPods("name", WildflyWithKeycloakSamlAdapterEjbApplication.APP_NAME)
				.get(0);
		Assertions.assertNotNull(wildflyPod, "An WildFly/JBoss EAP instance should exist");
		String podLog = OpenShifts.master().getPodLog(wildflyPod);
		Assertions.assertTrue(podLog.contains("INFO Registered saml client for module"),
				"The WildFly/JBoss EAP pod logs should report that the SAML client has been created");
		samlClientCreated = true;
	}

	/**
	 * Verifies that the WildFly/JBoss EAP application is running and accessible.
	 * <p>
	 * This test performs a health check by verifying that the application
	 * route returns an HTTP OK (200) status code.
	 * </p>
	 */
	@Test
	@Order(2)
	public void testWildflyApplicationIsRunning() {
		log.info("https base url: {}", wildflyApplicationRouteUrl);
		Https.doesUrlReturnOK(wildflyApplicationRouteUrl);
		wildflyApplicationIsRunning = true;
	}

	/**
	 * Tests successful SAML-based authentication and access to protected resources.
	 * <p>
	 * This test verifies the complete SAML authentication flow:
	 * <ol>
	 *   <li>User attempts to access a protected resource (profile.jsp)</li>
	 *   <li>User is redirected to the Keycloak/RHBK login page</li>
	 *   <li>User successfully logs in with valid credentials</li>
	 *   <li>User is redirected back to the protected resource</li>
	 *   <li>The page displays the authenticated user principal</li>
	 * </ol>
	 * </p>
	 *
	 * @throws IOException if an I/O error occurs during the test
	 */
	@Test
	@Order(3)
	public void testAccessOk() throws IOException {
		assumeTrue(samlClientCreated && wildflyApplicationIsRunning);

		String wildflySecuredPage = wildflyApplicationRouteUrl + "/profile.jsp";
		log.info("https url: {}", wildflySecuredPage);

		try (final WebClient webClient = new WebClient()) {
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

			HtmlPage keycloakLoginPage = webClient.getPage(wildflySecuredPage);
			KeycloakLoginPageUtilities.assertIsExpectedRealm(keycloakLoginPage,
					BasicKeycloakOperatorDynamicClientSamlApplication.REALM_NAME);

			HtmlPage securedPage = (HtmlPage) KeycloakLoginPageUtilities.makeLogin(keycloakLoginPage,
					BasicKeycloakOperatorDynamicClientSamlApplication.USER_NAME,
					BasicKeycloakOperatorDynamicClientSamlApplication.USER_PASSWORD);
			// first we check we landed on the expected JSP page
			KeycloakLoginPageUtilities.assertIsSecuredPage(securedPage);
			// then we check the JSP page received the expected security info from the servlet layer
			KeycloakLoginPageUtilities.assertIsUserPrincipal(securedPage, HTML_ID_WITH_USERNAME);
			// then we check the JSP page received the expected security info from the EJB layer
			KeycloakLoginPageUtilities.assertIsUserPrincipal(securedPage, HTML_ID_WITH_USERNAME_EJB);
		}
	}

	/**
	 * Tests that access is denied for users without required roles.
	 * <p>
	 * This test verifies the SAML authentication flow with role-based access control:
	 * <ol>
	 *   <li>User attempts to access a protected resource (profile.jsp)</li>
	 *   <li>User is redirected to the Keycloak/RHBK login page</li>
	 *   <li>User successfully logs in with valid credentials</li>
	 *   <li>User is redirected back to the application</li>
	 *   <li>Access is denied with a Forbidden page due to missing required role</li>
	 * </ol>
	 * </p>
	 *
	 * @throws IOException if an I/O error occurs during the test
	 */
	@Test
	@Order(4)
	public void testForbidden() throws IOException {
		assumeTrue(samlClientCreated && wildflyApplicationIsRunning);

		String wildflySecuredPage = wildflyApplicationRouteUrl + "/profile.jsp";
		log.info("https url: {}", wildflySecuredPage);

		try (final WebClient webClient = new WebClient()) {
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

			HtmlPage keycloakLoginPage = webClient.getPage(wildflySecuredPage);
			KeycloakLoginPageUtilities.assertIsExpectedRealm(keycloakLoginPage,
					BasicKeycloakOperatorDynamicClientSamlApplication.REALM_NAME);

			HtmlPage securedPage = (HtmlPage) KeycloakLoginPageUtilities.makeLogin(keycloakLoginPage,
					BasicKeycloakOperatorDynamicClientSamlApplication.ANOTHER_USER_NAME,
					BasicKeycloakOperatorDynamicClientSamlApplication.ANOTHER_USER_PASSWORD);
			KeycloakLoginPageUtilities.assertIsForbidden(securedPage);
		}
	}
}
