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
package org.jboss.intersmash.tests.wildfly.elytron.oidc.client.keycloak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;

/**
 * This class help interact with login page of Keycloak. It can assert the page is login page and have required fields.
 * Verify status code of page. And make login.
 */
public class KeycloakLoginPageUtilities {

	/** HTML ID of login form. */
	private static final String FORM_LOGIN = "kc-form-login";
	/** HTML name of input for username. */
	private static final String FIELD_USERNAME = "username";
	/** HTML name of input for password. */
	private static final String FIELD_PASSWORD = "password";
	/** HTML name of button for login. */
	private static final String BTN_LOGIN_LOGIN_PAGE = "login";

	/**
	 * Asserts that the given page is a Keycloak login page with the expected form fields.
	 *
	 * @param page the HTML page to verify
	 */
	public static void assertIsLoginPage(HtmlPage page) {
		try {
			assertThat(statusCodeOf(page)).isEqualTo(HttpStatus.SC_OK);
			HtmlForm loginForm = page.getHtmlElementById(FORM_LOGIN);
			assertThat(loginForm.getInputByName(FIELD_USERNAME) != null);
			assertThat(loginForm.getInputByName(FIELD_PASSWORD) != null);
			assertThat(loginForm.getButtonByName(BTN_LOGIN_LOGIN_PAGE) != null);
		} catch (ElementNotFoundException exception) {
			fail("The input element with name " + exception.getAttributeValue() + " was not found");
		}
	}

	/**
	 * Performs login on a Keycloak login page with the provided credentials.
	 *
	 * @param loginPage the login page
	 * @param user the username
	 * @param password the password
	 * @return the page returned after clicking the login button
	 * @throws IOException if an I/O error occurs during the login process
	 */
	public static Page makeLogin(HtmlPage loginPage, String user, String password) throws IOException {
		HtmlForm loginForm = loginPage.getHtmlElementById(FORM_LOGIN);
		HtmlInput userNameInput = loginForm.getInputByName(FIELD_USERNAME);
		HtmlInput passwordInput = loginForm.getInputByName(FIELD_PASSWORD);
		HtmlButton loginButton = loginForm.getButtonByName(BTN_LOGIN_LOGIN_PAGE);
		userNameInput.type(user);
		passwordInput.type(password);
		return loginButton.click();
	}

	/**
	 * Extracts the HTTP status code from a page response.
	 *
	 * @param response the page response
	 * @return the HTTP status code
	 */
	public static int statusCodeOf(Page response) {
		return response.getWebResponse().getStatusCode();
	}

	/**
	 * Asserts that the given page displays the expected Keycloak realm name.
	 *
	 * @param page the HTML page to verify
	 * @param realmName the expected realm name
	 */
	public static void assertIsExpectedRealm(HtmlPage page, String realmName) {
		List<Object> foundObjects = page.getByXPath(
				String.format("/html/body//div[contains(text(),'%s')]", realmName));
		Optional<Object> first = foundObjects.stream().findFirst();
		MatcherAssert.assertThat(String.format("The HTML 'DIV' element with text '%s' was not found", realmName),
				!first.isEmpty());
	}

	/**
	 * Requests a secured page which redirects to the Keycloak login page, then performs login.
	 *
	 * @param securedURL the URL of the secured resource
	 * @param login the username for login
	 * @param password the password for login
	 * @return the page returned after successful or unsuccessful login
	 * @throws IOException if an I/O error occurs during the request or login process
	 */
	public static Page requestSecuredPageAndLogin(String securedURL, String login, String password) throws IOException {
		try (final WebClient webClient = new WebClient()) {
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			HtmlPage loginPage = requestSecuredPage(securedURL, webClient);
			KeycloakLoginPageUtilities.assertIsLoginPage(loginPage);
			return KeycloakLoginPageUtilities.makeLogin(loginPage, login, password);
		}
	}

	private static HtmlPage requestSecuredPage(String securedURL, WebClient webClient) throws IOException {
		return webClient.getPage(securedURL);
	}

	/**
	 * Extracts the content from a text page.
	 *
	 * @param securedPage the text page
	 * @return the page content as a string
	 */
	public static String contentOf(TextPage securedPage) {
		return securedPage.getContent();
	}

	/**
	 * Extracts the text content from an HTML page body.
	 *
	 * @param securedPage the HTML page
	 * @return the body text content as a string
	 */
	public static String contentOf(HtmlPage securedPage) {
		return securedPage.getBody().getTextContent();
	}
}
