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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly/JBoss EAP 8.z + Keycloak/RHBK interoperability tests.
 *
 * WildFly/JBoss EAP 8.z application secured by Keycloak/RHBK.
 * The Keycloak client is provided to the WildFly/JBoss EAP 8.z application by the elytron-oidc-client layer.
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
		@Service(BasicKeycloakOperatorApplication.class),
		@Service(WildflyWithElytronOidcClientApplication.class)
})
public class WildflyWithElytronOidcClientIT {

	@ServiceUrl(WildflyWithElytronOidcClientApplication.class)
	private String wildflyApplicationRouteUrl;

	private static final String USER_NAME_WITH_CORRECT_ROLE = "admin";
	private static final String USER_PASSWORD_WITH_CORRECT_ROLE = "password";
	private static final String WRONG_USER_PASSWORD = "wrong_password";
	private static final String USER_NAME_WITH_WRONG_ROLE = "admin2";
	private static final String USER_PASSWORD_WITH_WRONG_ROLE = "password2";

	private static final String SUCCESS_EXPECTED_MESSAGE = "The user is authenticated";
	private static final String FORBIDDEN_EXPECTED_MESSAGE = "Forbidden";
	private static final String SECURED_CONTENT = "/secured";

	@BeforeEach
	public void beforeEach() {
		// make sure application is up
		Https.doesUrlReturnOK(wildflyApplicationRouteUrl).waitFor();
	}

	/**
	 * Sending HTTP requests to <i>SecuredServlet</i> using correct admin:password.
	 * The User is redirected to authentication form and after successful authentication back to protected content.
	 * The test passes if the request to the protected resource is successful and the user is authenticated and authorized.
	 */
	@Test
	public void testSuccess() throws IOException {
		TextPage securedPage = (TextPage) requestSecuredPageAndLogin(USER_NAME_WITH_CORRECT_ROLE,
				USER_PASSWORD_WITH_CORRECT_ROLE);
		assertThat(KeycloakLoginPageUtilities.statusCodeOf(securedPage)).isEqualTo(HttpStatus.SC_OK);
		assertThat(contentOf(securedPage)).contains(SUCCESS_EXPECTED_MESSAGE);
	}

	/**
	 * Sending HTTP requests to <i>SecuredServlet</i> using correct admin2:password2.
	 * User without the "user" role to verify that the access is protected with Keycloak/RHBK.
	 * The test passes if the request to the protected resource is not successful and the user is not authorized
	 */
	@Test
	public void testForbidden() throws IOException {
		HtmlPage securedPage = (HtmlPage) requestSecuredPageAndLogin(USER_NAME_WITH_WRONG_ROLE, USER_PASSWORD_WITH_WRONG_ROLE);
		assertThat(KeycloakLoginPageUtilities.statusCodeOf(securedPage)).isEqualTo(HttpStatus.SC_FORBIDDEN);
		assertThat(contentOf(securedPage)).contains(FORBIDDEN_EXPECTED_MESSAGE);
	}

	/**
	 * Sending HTTP requests to <i>SecuredServlet</i> using incorrect admin:wrong_password to verify
	 * that the access is protected with Keycloak/RHBK and users can try to authenticate themselves again.
	 * The test passes if the request to the protected resource is not successful and the user is not authenticated.
	 */
	@Test
	public void testUnauthorized() throws IOException {
		HtmlPage loginPage = (HtmlPage) requestSecuredPageAndLogin(USER_NAME_WITH_CORRECT_ROLE, WRONG_USER_PASSWORD);
		KeycloakLoginPageUtilities.assertIsLoginPage(loginPage);
	}

	private Page requestSecuredPageAndLogin(String login, String password) throws IOException {
		try (final WebClient webClient = new WebClient()) {
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			HtmlPage loginPage = requestSecuredPage(webClient);
			KeycloakLoginPageUtilities.assertIsLoginPage(loginPage);
			return KeycloakLoginPageUtilities.makeLogin(loginPage, login, password);
		}
	}

	private HtmlPage requestSecuredPage(WebClient webClient) throws IOException {
		String securedURL = wildflyApplicationRouteUrl + SECURED_CONTENT;
		return webClient.getPage(securedURL);
	}

	private static String contentOf(TextPage securedPage) {
		return securedPage.getContent();
	}

	private static String contentOf(HtmlPage securedPage) {
		return securedPage.getBody().getTextContent();
	}

}
