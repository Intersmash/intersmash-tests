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

import java.io.IOException;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.KeycloakOperatorApplication;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.Clients;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.ClientsBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.Users;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.UsersBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.users.CredentialsBuilder;

/**
 * Deploys one basic Keycloak instance with a realm with users and a client.
 * This can be re-used and extended with other realms and/or clients for different applications.
 */
public class BasicKeycloakOperatorOidcApplication extends BasicKeycloakOperatorDynamicClientOidcApplication
		implements KeycloakOperatorApplication, OpenShiftApplication {

	/**
	 * Creates a new Keycloak instance which is pre-configured with an OIDC Client; that is for cases when we DON'T use
	 * dynamic client registration.
	 *
	 * @throws IOException if an I/O error occurs during certificate generation
	 */
	public BasicKeycloakOperatorOidcApplication() throws IOException {
		super();
	}

	/**
	 * Defines users for the Keycloak realm.
	 *
	 * @return users for the Keycloak realm
	 */
	protected Users[] getUsers() {
		return new Users[] {
				new UsersBuilder()
						.withUsername(USER_NAME_WITH_CORRECT_ROLE)
						.withEnabled(true)
						.withCredentials(new CredentialsBuilder()
								.withType("password")
								.withValue(USER_PASSWORD_WITH_CORRECT_ROLE)
								.build())
						.withRealmRoles("user")
						.build(),
				new UsersBuilder()
						.withUsername(USER_NAME_WITH_WRONG_ROLE)
						.withEnabled(true)
						.withCredentials(new CredentialsBuilder()
								.withType("password")
								.withValue(USER_PASSWORD_WITH_WRONG_ROLE)
								.build())
						.withRealmRoles("admin")
						.build() };
	}

	/**
	 * Return the list of pre-configured OIDC Clients
	 * @return the list of pre-configured OIDC Clients
	 */
	@Override
	protected Clients getClients() {
		String wildflyWithElytronOidcClientRoute = WildflyWithElytronOidcClientApplication.getRoute();
		return new ClientsBuilder()
				.withClientId(WILDFLY_CLIENT_ELYTRON_NAME)
				.withPublicClient(true)
				.withStandardFlowEnabled(true)
				.withEnabled(true)
				.withRootUrl(
						String.format("http://%s/", wildflyWithElytronOidcClientRoute))
				.withRedirectUris(
						String.format("http://%s/*", wildflyWithElytronOidcClientRoute))
				.withAdminUrl(
						String.format("http://%s/", wildflyWithElytronOidcClientRoute))
				.withWebOrigins(
						String.format("http://%s/", wildflyWithElytronOidcClientRoute))
				.withSecret(OIDC_SECURE_DEPLOYMENT_SECRET)
				.withFullScopeAllowed(true)
				.build();
	}
}
