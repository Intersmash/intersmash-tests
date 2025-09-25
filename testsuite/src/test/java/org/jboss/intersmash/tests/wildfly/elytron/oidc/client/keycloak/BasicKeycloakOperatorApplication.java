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

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.openshift.PostgreSQLImageOpenShiftApplication;
import org.jboss.intersmash.application.operator.KeycloakOperatorApplication;
import org.jboss.intersmash.util.CommandLineBasedKeystoreGenerator;
import org.keycloak.k8s.v2alpha1.Keycloak;
import org.keycloak.k8s.v2alpha1.KeycloakBuilder;
import org.keycloak.k8s.v2alpha1.KeycloakRealmImport;
import org.keycloak.k8s.v2alpha1.KeycloakRealmImportBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.RealmBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.ClientsBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.RequiredActionsBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.UsersBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.users.CredentialsBuilder;
import org.keycloak.k8s.v2alpha1.keycloakspec.DbBuilder;
import org.keycloak.k8s.v2alpha1.keycloakspec.HostnameBuilder;
import org.keycloak.k8s.v2alpha1.keycloakspec.HttpBuilder;
import org.keycloak.k8s.v2alpha1.keycloakspec.IngressBuilder;
import org.keycloak.k8s.v2alpha1.keycloakspec.db.PasswordSecretBuilder;
import org.keycloak.k8s.v2alpha1.keycloakspec.db.UsernameSecretBuilder;

/**
 * Deploys one basic Keycloak instance with a realm with users and a client.
 * This can be re-used and extended with other realms and/or clients for different applications.
 */
public class BasicKeycloakOperatorApplication implements KeycloakOperatorApplication, OpenShiftApplication {

	public static final String APP_NAME = "sso-basic";
	// operator creates route which is prefixed by "keycloak" while APP_NAME is not used for route.
	public static final String KEYCLOAK_ROUTE = APP_NAME;

	protected static final String REALM_NAME = "basic-auth";
	// TODO - more cases supported by the same Keycloak service here
	protected static final String WILDFLY_CLIENT_ELYTRON_NAME = "wildfly-basic-elytron-auth-service";
	protected static final long KEYCLOAK_INSTANCES = 1;

	private final Keycloak keycloak;
	private final List<KeycloakRealmImport> keycloakRealmImports = new ArrayList<>();
	private final List<Secret> secrets = new ArrayList<>();
	public static final String TLS_SECRET_NAME = "tls-secret";

	public BasicKeycloakOperatorApplication() throws IOException {
		final String hostName = OpenShifts.master().generateHostname(APP_NAME);
		final CommandLineBasedKeystoreGenerator.GeneratedPaths certPaths = CommandLineBasedKeystoreGenerator
				.generateCerts(hostName);

		Secret tlsSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(TLS_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", APP_NAME))
				.endMetadata()
				.addToData(Map.of("tls.crt",
						Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(certPaths.certPem.toFile()))))
				.addToData(Map.of("tls.key",
						Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(certPaths.keyPem.toFile()))))
				.build();
		secrets.add(tlsSecret);

		keycloak = new KeycloakBuilder()
				.withNewMetadata()
				.withName(APP_NAME)
				.withLabels(Collections.singletonMap("app", APP_NAME))
				.endMetadata()
				.withNewSpec()
				.withInstances(KEYCLOAK_INSTANCES)
				.withDb(new DbBuilder()
						.withVendor("postgres")
						.withHost(KeycloakPostgresqlApplication.getServiceName())
						.withPort(KeycloakPostgresqlApplication.getServicePort())
						.withUsernameSecret(new UsernameSecretBuilder()
								.withName(KeycloakPostgresqlApplication.getServiceSecretName())
								.withKey(PostgreSQLImageOpenShiftApplication.POSTGRESQL_USER_KEY)
								.build())
						.withPasswordSecret(new PasswordSecretBuilder()
								.withName(KeycloakPostgresqlApplication.getServiceSecretName())
								.withKey(PostgreSQLImageOpenShiftApplication.POSTGRESQL_PASSWORD_KEY)
								.build())
						.withDatabase(KeycloakPostgresqlApplication.getServiceDbName())
						.build())
				.withHostname(new HostnameBuilder()
						.withHostname(hostName)
						.build())
				.withHttp(
						new HttpBuilder()
								.withTlsSecret(TLS_SECRET_NAME)
								.build())
				// On OCP 4.12+ .spec.ingress.className must be set
				.withIngress(new IngressBuilder().withClassName("openshift-default").build())
				// The Intersmash Keycloak provisioner sets the keycloak image for the Keycloak CRs, when it is defined
				// via configuration properties. In such a case, as the Keycloak documentation recommends,
				// .spec.startOptimized must be set to false.
				.withStartOptimized(false)
				.endSpec()
				.build();

		// TODO - more cases supported by the same Keycloak service here
		final String wildflyWithElytronOidcClientRoute = WildflyWithElytronOidcClientApplication.getRoute();

		keycloakRealmImports.add(
				new KeycloakRealmImportBuilder()
						.withNewMetadata()
						.withName(REALM_NAME)
						.withLabels(Collections.singletonMap("app", APP_NAME))
						.endMetadata()
						.withNewSpec()
						.withKeycloakCRName(keycloak.getMetadata().getName())
						.withRealm(new RealmBuilder()
								.withRequiredActions(
										new RequiredActionsBuilder().withAlias("CONFIGURE_TOTP").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("TERMS_AND_CONDITIONS").withEnabled(false)
												.build(),
										new RequiredActionsBuilder().withAlias("UPDATE_PASSWORD").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("UPDATE_PROFILE").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("VERIFY_EMAIL").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("delete_account").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("webauthn-register").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("webauthn-register-passwordless")
												.withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("VERIFY_PROFILE").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("delete_credential").withEnabled(false).build(),
										new RequiredActionsBuilder().withAlias("update_user_locale").withEnabled(false).build())
								.withId(REALM_NAME)
								.withRealm(REALM_NAME)
								.withEnabled(true)
								.withDisplayName(REALM_NAME)
								.withUsers(new UsersBuilder()
										.withUsername("admin")
										.withEnabled(true)
										.withCredentials(new CredentialsBuilder()
												.withType("password")
												.withValue("password")
												.build())
										.withRealmRoles("user", "admin")
										.withClientRoles(Map.of("realm-management", Arrays.asList("create-client")))
										.build(),
										new UsersBuilder()
												.withUsername("admin2")
												.withEnabled(true)
												.withCredentials(new CredentialsBuilder()
														.withType("password")
														.withValue("password2")
														.build())
												.withRealmRoles("admin")
												.build())
								.withClients(
										// TODO - more cases supported by the same Keycloak service here
										new ClientsBuilder()
												.withClientId(WILDFLY_CLIENT_ELYTRON_NAME)
												.withPublicClient(true)
												.withStandardFlowEnabled(true)
												.withEnabled(true)
												.withRootUrl(String.format("http://%s/", wildflyWithElytronOidcClientRoute))
												.withRedirectUris(
														String.format("http://%s/*", wildflyWithElytronOidcClientRoute))
												.withAdminUrl(String.format("http://%s/", wildflyWithElytronOidcClientRoute))
												.withWebOrigins(String.format("http://%s/", wildflyWithElytronOidcClientRoute))
												.withSecret("password")
												.withFullScopeAllowed(true)
												.build())
								.build())
						.endSpec()
						.build());
	}

	@Override
	public List<KeycloakRealmImport> getKeycloakRealmImports() {
		return keycloakRealmImports;
	}

	@Override
	public Keycloak getKeycloak() {
		return this.keycloak;
	}

	@Override
	public String getName() {
		return APP_NAME;
	}

	/**
	 * Get a route to Keycloak.
	 *
	 * @return route to Keycloak
	 */
	public static String getRoute() {
		return OpenShifts.master().generateHostname(KEYCLOAK_ROUTE);
	}

	@Override
	public List<Secret> getSecrets() {
		return Collections.unmodifiableList(secrets);
	}
}
