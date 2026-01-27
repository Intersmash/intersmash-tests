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
package org.jboss.intersmash.tests.wildfly.keycloak.saml.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import cz.xtf.core.http.Https;
import cz.xtf.junit5.annotations.OpenShiftRecorder;
import cz.xtf.junit5.listeners.ProjectCreator;
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
import org.jboss.intersmash.tests.wildfly.util.LoginUtil;
import org.junit.jupiter.api.BeforeEach;
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
		WildflyBootableJarWithKeycloakSamlAdapterEjbHelmApplication.APP_NAME })
@KeycloakTest
@WildflyTest
// EAP 8.1: Bootable jar is not supported on OpenShift Container Platform.
// See https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/8.1/html-single/provisioning_jboss_eap/index
@EapTest
@EapXpTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(KeycloakPostgresqlApplication.class),
		@Service(BasicKeycloakOperatorApplication.class),
		@Service(WildflyBootableJarWithKeycloakSamlAdapterEjbHelmApplication.class)
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WildFlyBootableJarKeycloakSamlAdapterEjbHelmIT {

	/** Flag indicating whether the WildFly application is running and ready for tests. */
	private static boolean wildflyApplicationIsRunning = false;

	/** The base URL for accessing the WildFly application route. */
	@ServiceUrl(WildflyBootableJarWithKeycloakSamlAdapterEjbHelmApplication.class)
	private String wildflyApplicationRouteUrl;

	/** The URL of the secured page that requires SAML authentication. */
	private String eapSecuredPage;

	/** The URL of the unsecured page that doesn't require authentication. */
	private String eapUnSecuredPage;

	/**
	 * Sets up the test environment before each test method.
	 * <p>
	 * This method initializes the secured and unsecured page URLs by:
	 * <ul>
	 *   <li>Converting HTTP URLs to HTTPS</li>
	 *   <li>Appending the appropriate paths (/secured and /unsecured)</li>
	 *   <li>Logging the URLs for debugging purposes</li>
	 * </ul>
	 * </p>
	 */
	@BeforeEach
	public void beforeEach() {
		eapSecuredPage = wildflyApplicationRouteUrl.replace("http://", "https://") + "/secured";
		eapUnSecuredPage = wildflyApplicationRouteUrl.replace("http://", "https://") + "/unsecured";
		log.debug("Eap secured url: {}", eapSecuredPage);
		log.debug("Eap unsecured url: {}", eapUnSecuredPage);
	}

	/**
	 * Verifies that the WildFly/JBoss EAP application is running and accessible.
	 * <p>
	 * This test performs a health check by verifying that the application
	 * route returns an HTTP OK (200) status code.
	 * </p>
	 */
	@Test
	@Order(1)
	public void testWildflyApplicationIsRunning() {
		log.info("https base url: {}", eapUnSecuredPage);
		Https.doesUrlReturnOK(eapUnSecuredPage).timeout(5000).waitFor();
		wildflyApplicationIsRunning = true;
	}

	/**
	 * Verifies successful SAML-based authentication and access to protected resources.
	 * <p>
	 * This test validates that:
	 * <ul>
	 *   <li>Accessing a protected resource redirects to the Keycloak login page</li>
	 *   <li>The login page displays the correct realm name</li>
	 *   <li>User can successfully authenticate with valid credentials</li>
	 *   <li>After authentication, user is redirected to the requested secured resource</li>
	 *   <li>The authenticated user has the expected 'user-role' assigned</li>
	 *   <li>The user can call secured EJB methods based on their role</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException if the HTTP request fails
	 */
	@Test
	@Order(2)
	public void testAccessOk() throws IOException {
		assumeTrue(wildflyApplicationIsRunning);

		try (final WebClient webClient = new WebClient()) {
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

			HtmlPage loginPage = webClient.getPage(eapSecuredPage);
			LoginUtil.assertIsLoginPage(loginPage);
			LoginUtil.assertIsExpectedRealm(loginPage, BasicKeycloakOperatorApplication.REALM_NAME);

			TextPage securedPage = (TextPage) LoginUtil.makeLogin(loginPage, BasicKeycloakOperatorApplication.USER_NAME,
					BasicKeycloakOperatorApplication.USER_PASSWORD);
			// first we check we landed on the expected secured page
			assertIsSecuredPage(securedPage);
			// check the user have expected role assigned
			assertUserRole(securedPage);
			// then we check the server could call secured EJB method
			assertEJBMethodCall(securedPage);
		}
	}

	/**
	 * Verifies that role-based access control properly denies access to unauthorized users.
	 * <p>
	 * This test validates that:
	 * <ul>
	 *   <li>Users can authenticate successfully with valid credentials</li>
	 *   <li>Users without the required role ('user-role') are denied access to protected resources</li>
	 *   <li>A Forbidden (403) response is returned when access is denied</li>
	 *   <li>SAML authentication works correctly even when authorization fails</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException if the HTTP request fails
	 */
	@Test
	@Order(3)
	public void testForbidden() throws IOException {
		assumeTrue(wildflyApplicationIsRunning);

		try (final WebClient webClient = new WebClient()) {
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

			HtmlPage loginPage = webClient.getPage(eapSecuredPage);
			LoginUtil.assertIsLoginPage(loginPage);
			LoginUtil.assertIsExpectedRealm(loginPage, BasicKeycloakOperatorApplication.REALM_NAME);

			HtmlPage securedPage = (HtmlPage) LoginUtil.makeLogin(loginPage, BasicKeycloakOperatorApplication.ANOTHER_USER_NAME,
					BasicKeycloakOperatorApplication.ANOTHER_USER_PASSWORD);
			assertIsForbidden(securedPage);
		}
	}

	/**
	 * Asserts that the given page is a Forbidden (403) error page.
	 *
	 * @param securedPage the HTML page to verify
	 */
	private void assertIsForbidden(HtmlPage securedPage) {
		assertThat("The HTML page is not the expected Forbidden page!",
				securedPage.getByXPath("//body//text()").get(0).toString().equalsIgnoreCase("Forbidden"));
	}

	/**
	 * Asserts that the given page is the expected secured page.
	 *
	 * @param securedPage the text page to verify
	 */
	private void assertIsSecuredPage(TextPage securedPage) {
		String content = securedPage.getContent();
		assertThat("The secured page isn't the expected one", content.startsWith("Servlet - secured page"));
	}

	/**
	 * Asserts that the authenticated user has the 'user-role' assigned.
	 *
	 * @param securedPage the text page containing role information
	 */
	private void assertUserRole(TextPage securedPage) {
		String content = securedPage.getContent();
		assertThat("The user don't have a 'user' role", content.contains("Servlet.isCallerInRole(\"user-role\")=true"));
	}

	/**
	 * Asserts that EJB method access control is working correctly.
	 * <p>
	 * This verifies that:
	 * <ul>
	 *   <li>Methods requiring 'user-role' are accessible (allowed)</li>
	 *   <li>Methods requiring 'manager-role' are denied</li>
	 * </ul>
	 * </p>
	 *
	 * @param securedPage the text page containing EJB method call results
	 */
	private void assertEJBMethodCall(TextPage securedPage) {
		String content = securedPage.getContent();
		assertThat("The user method is forbiden", content.contains("@EJB.allowUserMethod() ALLOWED"));
		assertThat("The manager method is allowd", content.contains("@EJB.allowManagerMethod() DENIED"));
	}
}
