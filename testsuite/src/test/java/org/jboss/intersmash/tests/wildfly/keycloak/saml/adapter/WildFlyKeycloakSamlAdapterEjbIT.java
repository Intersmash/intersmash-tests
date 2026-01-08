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

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
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
@OpenShiftRecorder(resourceNames = { BasicKeycloakOperatorApplication.APP_NAME,
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
		@Service(BasicKeycloakOperatorApplication.class),
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
			KeycloakLoginPageUtilities.assertIsExpectedRealm(keycloakLoginPage, BasicKeycloakOperatorApplication.REALM_NAME);

			HtmlPage securedPage = (HtmlPage) KeycloakLoginPageUtilities.makeLogin(keycloakLoginPage,
					BasicKeycloakOperatorApplication.USER_NAME,
					BasicKeycloakOperatorApplication.USER_PASSWORD);
			// first we check we landed on the expected JSP page
			assertIsSecuredPage(securedPage);
			// then we check the JSP page received the expected security info from the servlet layer
			assertIsUserPrincipal(securedPage, HTML_ID_WITH_USERNAME);
			// then we check the JSP page received the expected security info from the EJB layer
			assertIsUserPrincipal(securedPage, HTML_ID_WITH_USERNAME_EJB);
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
			KeycloakLoginPageUtilities.assertIsExpectedRealm(keycloakLoginPage, BasicKeycloakOperatorApplication.REALM_NAME);

			HtmlPage securedPage = (HtmlPage) KeycloakLoginPageUtilities.makeLogin(keycloakLoginPage,
					BasicKeycloakOperatorApplication.ANOTHER_USER_NAME,
					BasicKeycloakOperatorApplication.ANOTHER_USER_PASSWORD);
			assertIsForbidden(securedPage);
		}
	}

	/**
	 * Asserts that the page displays a "Forbidden" message.
	 * <p>
	 * This method verifies that access was denied to a resource,
	 * typically due to insufficient permissions or missing required roles.
	 * </p>
	 *
	 * @param securedPage the page to check for forbidden access
	 * @throws AssertionError if the page does not contain the "Forbidden" message
	 */
	protected void assertIsForbidden(HtmlPage securedPage) {
		assertThat("The HTML page is not the expected Forbidden page!",
				securedPage.getByXPath("//body//text()").get(0).toString().equalsIgnoreCase("Forbidden"));
	}

	/**
	 * Asserts that the page is the expected secured page (profile.jsp).
	 * <p>
	 * This method verifies that after successful authentication, the user
	 * was redirected to the correct protected resource.
	 * </p>
	 *
	 * @param securedPage the page to verify
	 * @throws AssertionError if the page URL does not contain "profile.jsp"
	 */
	protected void assertIsSecuredPage(HtmlPage securedPage) {
		assertThat(
				"The page the client was redirected to, isn't the one expected",
				securedPage.getUrl().toString(), containsStringIgnoringCase("profile.jsp"));
	}

	/**
	 * Asserts that the secured page contains the authenticated user principal.
	 * <p>
	 * This method verifies that the HTML element with the specified ID contains
	 * a valid user principal value matching the expected GUID pattern (G-[UUID]).
	 * This confirms that the SAML authentication was successful and the user
	 * identity was properly propagated to the application.
	 * </p>
	 *
	 * @param securedPage the secured page containing user information
	 * @param htmlId the HTML element ID that should contain the username
	 * @throws AssertionError if the element is not found or doesn't match the expected pattern
	 */
	protected void assertIsUserPrincipal(HtmlPage securedPage, String htmlId) {
		try {
			HtmlElement username = securedPage.getHtmlElementById(htmlId);
			assertThat(
					String.format("The HTML element with ID (%s) does not contain expected %s value", htmlId,
							BasicKeycloakOperatorApplication.USER_NAME),
					username.getTextContent(), matchesPattern("G-[a-zA-Z0-9\\-]{36}"));
		} catch (ElementNotFoundException exception) {
			fail("The element with id " + exception.getAttributeValue() + " was not found");
		}
	}
}
