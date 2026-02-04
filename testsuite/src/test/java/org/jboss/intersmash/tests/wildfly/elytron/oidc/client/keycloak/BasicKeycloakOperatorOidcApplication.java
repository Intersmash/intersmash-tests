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
