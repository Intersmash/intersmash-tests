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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.Page;
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
 * Utility class for interacting with Keycloak/RHBK login pages.
 * <p>
 * This class provides helper methods to:
 * <ul>
 *   <li>Assert that a page is a valid Keycloak login page with required fields</li>
 *   <li>Verify HTTP status codes</li>
 *   <li>Perform login operations</li>
 *   <li>Validate the displayed realm name</li>
 * </ul>
 * </p>
 */
public class KeycloakLoginPageUtilities {

	/** HTML ID of login form */
	private static final String FORM_LOGIN = "kc-form-login";
	/** HTML name of input for username */
	private static final String FIELD_USERNAME = "username";
	/** HTML name of input for password */
	private static final String FIELD_PASSWORD = "password";
	/** HTML name of button for login */
	private static final String BTN_LOGIN_LOGIN_PAGE = "login";

	/**
	 * Asserts that the provided page is a valid RHBK/Keycloak login page.
	 * <p>
	 * This method verifies that:
	 * <ul>
	 *   <li>The HTTP status code is 200 (OK)</li>
	 *   <li>The login form exists with the expected ID</li>
	 *   <li>The username and password input fields are present</li>
	 *   <li>The login button is present</li>
	 * </ul>
	 * </p>
	 *
	 * @param page the HTML page to validate
	 * @throws AssertionError if any of the validation checks fail
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
	 * Performs a login operation on the RHBK/Keycloak login page.
	 * <p>
	 * This method fills in the username and password fields and submits the login form.
	 * </p>
	 *
	 * @param loginPage the RHBK/Keycloak login page
	 * @param user the username to use for login
	 * @param password the password to use for login
	 * @return the resulting page after login submission
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
	 * Retrieves the HTTP status code from the page response.
	 *
	 * @param response the page to extract the status code from
	 * @return the HTTP status code
	 */
	public static int statusCodeOf(Page response) {
		return response.getWebResponse().getStatusCode();
	}

	/**
	 * Asserts that the login page displays the expected realm name.
	 * <p>
	 * This method searches for a DIV element containing the realm name to verify
	 * that the user is being authenticated against the correct Keycloak realm.
	 * </p>
	 *
	 * @param page the HTML page to check
	 * @param realmName the expected realm name
	 * @throws AssertionError if the realm name is not found on the page
	 */
	public static void assertIsExpectedRealm(HtmlPage page, String realmName) {
		List<Object> foundObjects = page.getByXPath(
				String.format("/html/body//div[contains(text(),'%s')]", realmName));
		Optional<Object> first = foundObjects.stream().findFirst();
		MatcherAssert.assertThat(
				String.format("The HTML 'DIV' element with text '%s' was not found in: %n----%n%s%n----%n", realmName,
						page.getBody().asXml()),
				!first.isEmpty());
	}
}
