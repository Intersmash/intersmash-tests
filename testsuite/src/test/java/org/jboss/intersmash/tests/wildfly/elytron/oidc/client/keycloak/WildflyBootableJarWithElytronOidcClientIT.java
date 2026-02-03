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
package org.jboss.intersmash.tests.wildfly.elytron.oidc.client.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import cz.xtf.core.http.Https;
import cz.xtf.junit5.listeners.ProjectCreator;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.KeycloakTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.jboss.intersmash.tests.wildfly.elytron.oidc.client.keycloak.util.KeycloakLoginPageUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly/JBoss EAP 8.z + Keycloak/RHBK interoperability tests.
 *
 * WildFly/JBoss EAP 8.z application secured by Keycloak/RHBK.
 * The Keycloak client is provided to the WildFly/JBoss EAP 8.z application by the elytron-oidc-client layer.
 *
 * This test configures the elytron-oidc-client subsystem using the oidc.json file to secure the application with OIDC.
 */
@KeycloakTest
@WildflyTest
@EapTest
@EapXpTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(KeycloakPostgresqlApplication.class),
		@Service(BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.class),
		@Service(WildflyBootableJarWithElytronOidcClientApplication.class)
})
public class WildflyBootableJarWithElytronOidcClientIT {
	/** Flag indicating whether the WildFly application is running and ready for tests. */
	private static boolean wildflyApplicationIsRunning = false;

	/** URL of the WildFly application route. */
	@ServiceUrl(WildflyBootableJarWithElytronOidcClientApplication.class)
	private String wildflyApplicationRouteUrl;

	/** Expected message when authentication is successful. */
	private static final String SUCCESS_EXPECTED_MESSAGE = "The user is authenticated";
	/** Expected message when access is forbidden. */
	private static final String FORBIDDEN_EXPECTED_MESSAGE = "Forbidden";

	/** The URL of the secured page that requires OIDC authentication. */
	private String wildFlyBootableJarSecuredPage;

	/** The URL of the unsecured page that doesn't require authentication. */
	private String wildFlyBootableJarUnSecuredPage;

	@BeforeEach
	public void beforeEach() {
		wildFlyBootableJarSecuredPage = wildflyApplicationRouteUrl.replace("http://", "https://") + "/secured";
		wildFlyBootableJarUnSecuredPage = wildflyApplicationRouteUrl.replace("http://", "https://") + "/unsecured";
		log.debug("Eap secured url: {}", wildFlyBootableJarSecuredPage);
		log.debug("Eap unsecured url: {}", wildFlyBootableJarUnSecuredPage);
	}

	/**
	 * Checks that the bootable jar application is up and running
	 */
	@Test
	@Order(1)
	public void testWildflyApplicationIsRunning() {
		log.info("https base url: {}", wildFlyBootableJarUnSecuredPage);
		Https.doesUrlReturnOK(wildFlyBootableJarUnSecuredPage).timeout(5000).waitFor();
		wildflyApplicationIsRunning = true;
	}

	/**
	 * Test sending HTTP requests to SecuredServlet using correct credentials.
	 * User is redirected to authentication form and after successful authentication back to protected content.
	 * Request to the protected resource is successful and the user is authenticated and authorized.
	 */
	@Test
	@Order(2)
	public void testSuccess() throws IOException {
		assumeTrue(wildflyApplicationIsRunning);
		TextPage securedPage = (TextPage) requestSecuredPageAndLogin(
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.USER_NAME_WITH_CORRECT_ROLE,
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.USER_PASSWORD_WITH_CORRECT_ROLE);
		assertThat(KeycloakLoginPageUtilities.statusCodeOf(securedPage)).isEqualTo(HttpStatus.SC_OK);
		assertThat(contentOf(securedPage)).contains(SUCCESS_EXPECTED_MESSAGE);
	}

	/**
	 * Test sending HTTP requests to SecuredServlet using credentials for a user without the "user" role
	 * to verify that the access is protected with Keycloak/RHBK.
	 * Request to the protected resource is not successful and the user is not authorized.
	 */
	@Test
	@Order(3)
	public void testForbidden() throws IOException {
		assumeTrue(wildflyApplicationIsRunning);
		HtmlPage securedPage = (HtmlPage) requestSecuredPageAndLogin(
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.USER_NAME_WITH_WRONG_ROLE,
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.USER_PASSWORD_WITH_WRONG_ROLE);
		assertThat(KeycloakLoginPageUtilities.statusCodeOf(securedPage)).isEqualTo(HttpStatus.SC_FORBIDDEN);
		assertThat(contentOf(securedPage)).contains(FORBIDDEN_EXPECTED_MESSAGE);
	}

	/**
	 * Test sending HTTP requests to SecuredServlet using incorrect password to verify
	 * that the access is protected with Keycloak/RHBK and users can try to authenticate themselves again.
	 * Request to the protected resource is not successful and the user is not authenticated.
	 */
	@Test
	@Order(4)
	public void testUnauthorized() throws IOException {
		assumeTrue(wildflyApplicationIsRunning);
		HtmlPage loginPage = (HtmlPage) requestSecuredPageAndLogin(
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.USER_NAME_WITH_CORRECT_ROLE,
				"wrong_password");
		KeycloakLoginPageUtilities.assertIsLoginPage(loginPage);
	}

	/**
	 * Requests the secured page and performs login with the provided credentials.
	 * <p>
	 * This method creates a WebClient, requests the secured page (which redirects to Keycloak),
	 * and submits the login form with the given credentials.
	 * </p>
	 *
	 * @param login the username to use for authentication
	 * @param password the password to use for authentication
	 * @return the resulting page after login attempt (either the secured page or login page on failure)
	 * @throws IOException if an I/O error occurs during the request
	 */
	private Page requestSecuredPageAndLogin(String login, String password) throws IOException {
		try (final WebClient webClient = new WebClient()) {
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			HtmlPage loginPage = requestSecuredPage(webClient);
			KeycloakLoginPageUtilities.assertIsLoginPage(loginPage);
			return KeycloakLoginPageUtilities.makeLogin(loginPage, login, password);
		}
	}

	/**
	 * Requests the secured page using the provided WebClient.
	 *
	 * @param webClient the WebClient to use for the request
	 * @return the HTML page representing the secured resource (or login page if redirected)
	 * @throws IOException if an I/O error occurs during the request
	 */
	private HtmlPage requestSecuredPage(WebClient webClient) throws IOException {
		return webClient.getPage(wildFlyBootableJarSecuredPage);
	}

	/**
	 * Extracts the text content from a TextPage.
	 *
	 * @param securedPage the text page to extract content from
	 * @return the text content of the page
	 */
	private static String contentOf(TextPage securedPage) {
		return securedPage.getContent();
	}

	/**
	 * Extracts the text content from an HtmlPage.
	 *
	 * @param securedPage the HTML page to extract content from
	 * @return the text content of the page body
	 */
	private static String contentOf(HtmlPage securedPage) {
		return securedPage.getBody().getTextContent();
	}
}
