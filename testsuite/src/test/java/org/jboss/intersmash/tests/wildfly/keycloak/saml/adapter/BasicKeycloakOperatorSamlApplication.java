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

import java.io.IOException;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.KeycloakOperatorApplication;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.Clients;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.ClientsBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.Users;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.UsersBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.users.CredentialsBuilder;

/**
 * Keycloak Operator Application with pre-configured SAML client for adapter testing.
 * <p>
 * This application extends {@link BasicKeycloakOperatorDynamicClientSamlApplication} and
 * pre-configures a SAML client instead of relying on dynamic client registration.
 * </p>
 * <p>
 * This configuration is suitable for scenarios where:
 * <ul>
 *   <li>Static SAML client configuration is preferred over dynamic registration</li>
 *   <li>Client configuration needs to be predefined before application deployment</li>
 *   <li>More control over SAML client settings is required</li>
 * </ul>
 * </p>
 * <p>
 * The pre-configured SAML client includes:
 * <ul>
 *   <li>Client ID matching the WildFly application name</li>
 *   <li>SAML protocol configuration</li>
 *   <li>Base URL and admin URL for SAML endpoints</li>
 *   <li>Client authentication secret</li>
 *   <li>Full scope allowed for realm roles</li>
 * </ul>
 * </p>
 */
public class BasicKeycloakOperatorSamlApplication extends BasicKeycloakOperatorDynamicClientSamlApplication
		implements KeycloakOperatorApplication, OpenShiftApplication {

	/**
	 * Constructs a new Keycloak instance with a pre-configured SAML client.
	 * <p>
	 * This constructor initializes the Keycloak application by calling the parent
	 * constructor, which sets up the database, HTTPS certificates, realm configuration,
	 * and test users. The SAML client is then configured through the {@link #getClients()}
	 * method override.
	 * </p>
	 *
	 * @throws IOException if an I/O error occurs during certificate generation
	 */
	public BasicKeycloakOperatorSamlApplication() throws IOException {
		super();
	}

	/**
	 * Defines users for the Keycloak realm.
	 *
	 * @return users for the Keycloak realm
	 */
	protected Users[] getUsers() {
		return new Users[] { new UsersBuilder()
				.withUsername(USER_NAME)
				.withEnabled(true)
				.withCredentials(new CredentialsBuilder()
						.withType("password")
						.withValue(USER_PASSWORD)
						.build())
				// this must match with the role defined in "keycloak-saml-adapter"'s web.xml
				.withRealmRoles("user-role")
				.build(),
				new UsersBuilder()
						.withUsername(ANOTHER_USER_NAME)
						.withEnabled(true)
						.withCredentials(new CredentialsBuilder()
								.withType("password")
								.withValue(ANOTHER_USER_PASSWORD)
								.build())
						.withRealmRoles("another-role")
						.build() };
	}

	/**
	 * Returns a pre-configured SAML client for the WildFly/JBoss EAP application.
	 * <p>
	 * The client configuration includes:
	 * <ul>
	 *   <li>Client ID: matches the WildFly application name</li>
	 *   <li>Protocol: SAML 2.0</li>
	 *   <li>Base URL: HTTPS route to the WildFly application</li>
	 *   <li>Admin URL: SAML endpoint for processing assertions and logout</li>
	 *   <li>Authentication: client-secret authenticator with predefined secret</li>
	 *   <li>Standard flow: enabled for browser-based authentication</li>
	 *   <li>Full scope: allowed for accessing all realm roles</li>
	 * </ul>
	 * </p>
	 *
	 * @return pre-configured SAML client configuration
	 */
	@Override
	protected Clients getClients() {
		// General settings -> Home URL -> Default URL to use when the auth server needs to redirect or link back to the client.
		//		http://keycloak-saml-adapter-ejb-tom1.apps.mash-420-ulfk.eapqe.psi.redhat.com:80/
		// General settings -> Master SAML Processing URL -> If configured, this URL will be used for every binding to both the SP's Assertion Consumer and Single Logout Services. This can be individually overridden for each binding and service in the Fine Grain SAML Endpoint Configuration.
		//		http://keycloak-saml-adapter-ejb-tom1.apps.mash-420-ulfk.eapqe.psi.redhat.com:80/saml
		// General settings -> Signature and Encryption -> Should SAML documents be signed by the realm?
		//		On
		// General settings -> Signature algorithm -> The signature algorithm to use to sign documents. Note that 'SHA1' based algorithms are deprecated and can be removed in the future. It is recommended to stick to some more secure algorithm instead of '*_SHA1'.
		// 		RSA_SHA256
		// Keys -> Signing keys config -> Client Certificate for validate JWT issued by client and signed by Client private key from your keystore.
		return new ClientsBuilder()
				.withClientId(WildflyBootableJarWithKeycloakSamlAdapterEjbHelmApplication.APP_NAME)
				.withEnabled(true)
				.withStandardFlowEnabled(true)
				.withProtocol("saml")
				.withBaseUrl(
						String.format("https://%s/", WildflyBootableJarWithKeycloakSamlAdapterEjbHelmApplication.getRoute()))
				.withAdminUrl(String.format("https://%s/secured/saml",
						WildflyBootableJarWithKeycloakSamlAdapterEjbHelmApplication.getRoute()))
				.withFullScopeAllowed(true)
				.build();
	}
}
