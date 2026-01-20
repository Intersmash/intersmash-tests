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
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;
import org.keycloak.k8s.v2alpha1.Keycloak;
import org.keycloak.k8s.v2alpha1.KeycloakBuilder;
import org.keycloak.k8s.v2alpha1.KeycloakRealmImport;
import org.keycloak.k8s.v2alpha1.KeycloakRealmImportBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.RealmBuilder;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.realm.Clients;
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
public class BasicKeycloakOperatorDynamicClientApplication implements KeycloakOperatorApplication, OpenShiftApplication {

	/** Application name for the Keycloak service. */
	public static final String APP_NAME = "sso-basic-dc";
	// operator creates route which is prefixed by "keycloak" while APP_NAME is not used for route.
	/** Route name for the Keycloak service. */
	public static final String KEYCLOAK_ROUTE = APP_NAME;

	/** Name of the Keycloak realm for authentication. */
	protected static final String REALM_NAME = "basic-auth";
	/** Client ID for the WildFly Elytron OIDC service. */
	protected static final String WILDFLY_CLIENT_ELYTRON_NAME = "wildfly-basic-elytron-auth-service";
	/** Number of Keycloak instances to deploy. */
	protected static final long KEYCLOAK_INSTANCES = 1;
	/** Shared secret for OIDC client authentication. */
	protected static final String OIDC_SECURE_DEPLOYMENT_SECRET = "3up7r37cr7doidccli7ntpa33word";

	/** The Keycloak instance custom resource. */
	protected final Keycloak keycloak;
	/** List of Keycloak realm import custom resources. */
	protected final List<KeycloakRealmImport> keycloakRealmImports = new ArrayList<>();
	/** List of Kubernetes secrets for certificates and credentials. */
	protected final List<Secret> secrets = new ArrayList<>();

	/** Username for the admin user with client registration permissions. */
	public static final String OIDC_USER_NAME = "admin";
	/** Password for the admin user with client registration permissions. */
	public static final String OIDC_USER_PASSWORD = "admin1234pa33word";

	/** Certificate name used for HTTPS encryption. */
	public static final String HTTPS_CERTIFICATE_NAME = APP_NAME;
	/** Keystore password for HTTPS certificates. */
	public static final String HTTPS_KEYSTORE_PASSWORD = "1234password";

	/** Username for test user with the correct role assignment. */
	protected static final String USER_NAME_WITH_CORRECT_ROLE = "user1";
	/** Password for test user with the correct role assignment. */
	protected static final String USER_PASSWORD_WITH_CORRECT_ROLE = "password1";
	/** Username for test user with incorrect role assignment. */
	protected static final String USER_NAME_WITH_WRONG_ROLE = "admin2";
	/** Password for test user with incorrect role assignment. */
	protected static final String USER_PASSWORD_WITH_WRONG_ROLE = "password2";

	/**
	 * Creates a new Keycloak instance
	 *
	 * @throws IOException if an I/O error occurs during certificate generation
	 */
	public BasicKeycloakOperatorDynamicClientApplication() throws IOException {
		final String hostName = OpenShifts.master().generateHostname(APP_NAME);

		// Private Key + Self-signed certificate to encrypt traffic to the Keycloak service
		// https://www.keycloak.org/docs/latest/server_admin/index.html#loading-keys-from-a-java-keystore
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo keycloakCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(APP_NAME),
						HTTPS_CERTIFICATE_NAME,
						HTTPS_KEYSTORE_PASSWORD,
						HTTPS_KEYSTORE_PASSWORD,
						Collections.emptyList());

		Secret httpsSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(APP_NAME + "-keystore")
				.withLabels(Collections.singletonMap("app", APP_NAME))
				.endMetadata()
				.addToData(Map.of("tls.crt",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(keycloakCertificate.certificate.toFile()))))
				.addToData(Map.of("tls.key",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(keycloakCertificate.privatekey.toFile()))))
				.build();
		secrets.add(httpsSecret);

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
								.withTlsSecret(httpsSecret.getMetadata().getName())
								.build())
				// On OCP 4.12+ .spec.ingress.className must be set
				.withIngress(new IngressBuilder().withClassName("openshift-default").build())
				// The Intersmash Keycloak provisioner sets the keycloak image for the Keycloak CRs, when it is defined
				// via configuration properties. In such a case, as the Keycloak documentation recommends,
				// .spec.startOptimized must be set to false.
				.withStartOptimized(false)
				.endSpec()
				.build();

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
								.withUsers(
										new UsersBuilder()
												.withUsername(OIDC_USER_NAME)
												.withEnabled(true)
												.withCredentials(new CredentialsBuilder()
														.withType("password")
														.withValue(OIDC_USER_PASSWORD)
														.build())
												.withRealmRoles("user", "admin")
												.withClientRoles(Map.of("realm-management", Arrays.asList("create-client")))
												.build(),
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
												.build())
								.withClients(
										getClients())
								.build())
						.endSpec()
						.build());
	}

	/**
	 * Return the list of pre-configured OIDC Clients: we don't have any in this class
	 * @return the list of pre-configured OIDC Clients
	 */
	protected Clients getClients() {
		return null;
	}

	/**
	 * Get the Keycloak realm imports for this application.
	 *
	 * @return the list of Keycloak realm imports
	 */
	@Override
	public List<KeycloakRealmImport> getKeycloakRealmImports() {
		return keycloakRealmImports;
	}

	/**
	 * Get the Keycloak instance for this application.
	 *
	 * @return the Keycloak instance
	 */
	@Override
	public Keycloak getKeycloak() {
		return this.keycloak;
	}

	/**
	 * Get the application name.
	 *
	 * @return the application name
	 */
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

	/**
	 * Get the Kubernetes secrets for this application.
	 *
	 * @return unmodifiable list of Kubernetes secrets
	 */
	@Override
	public List<Secret> getSecrets() {
		return Collections.unmodifiableList(secrets);
	}
}
