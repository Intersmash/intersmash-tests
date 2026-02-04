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

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.openshift.PostgreSQLImageOpenShiftApplication;
import org.jboss.intersmash.application.operator.KeycloakOperatorApplication;
import org.jboss.intersmash.tests.wildfly.elytron.oidc.client.keycloak.KeycloakPostgresqlApplication;
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
 * Basic Keycloak Operator Application for SAML adapter testing.
 * <p>
 * This application configures a Keycloak instance with:
 * <ul>
 *   <li>PostgreSQL database backend</li>
 *   <li>HTTPS support with self-signed certificates</li>
 *   <li>A pre-configured realm for SAML authentication</li>
 *   <li>Test users with appropriate roles</li>
 *   <li>Truststore configuration for SAML client certificates</li>
 * </ul>
 * </p>
 */
public class BasicKeycloakOperatorDynamicClientSamlApplication implements KeycloakOperatorApplication, OpenShiftApplication {
	/** Application name used for labeling and resource identification. */
	public static final String APP_NAME = "sso-basic";

	/** Username for the SSO client creator user with realm management permissions. */
	public static final String SSO_USERNAME = "client";

	/** Password for the SSO client creator user. */
	protected static final String SSO_PASSWORD = "creator";

	/** Username for the first test user with user-role permissions. */
	protected static final String USER_NAME = "user";

	/** Password for the first test user. */
	protected static final String USER_PASSWORD = "password";

	/** Username for the second test user with another-role permissions. */
	protected static final String ANOTHER_USER_NAME = "another-user";

	/** Password for the second test user. */
	protected static final String ANOTHER_USER_PASSWORD = "another-password";

	/** Name of the Keycloak realm used for SAML authentication. */
	protected static final String REALM_NAME = "basic-auth-realm";

	/** Number of Keycloak instances to deploy. */
	protected static final long KEYCLOAK_INSTANCES = 1;

	/** Certificate name used for HTTPS encryption. */
	public static final String HTTPS_CERTIFICATE_NAME = APP_NAME;

	/** Keystore password for HTTPS certificates. */
	public static final String HTTPS_KEYSTORE_PASSWORD = "1234password";

	/** Environment variable name for the Keycloak service host. */
	public static final String SSO_BASIC_SERVICE_SERVICE_HOST = String.format("%s_SERVICE_SERVICE_HOST",
			APP_NAME.toUpperCase(Locale.ROOT).replaceAll("-", "_"));

	/** Environment variable name for the Keycloak service port. */
	public static final String SSO_BASIC_SERVICE_SERVICE_PORT = String.format("%s_SERVICE_SERVICE_PORT",
			APP_NAME.toUpperCase(Locale.ROOT).replaceAll("-", "_"));

	/** The Keycloak custom resource configuration. */
	private final Keycloak keycloak;

	/** List of Keycloak realm imports containing realm configurations and users. */
	private final List<KeycloakRealmImport> keycloakRealmImports = new ArrayList<>();

	/** List of Kubernetes secrets for certificates and keystores. */
	private final List<Secret> secrets = new ArrayList<>();

	/**
	 * Constructs a new BasicKeycloakOperatorDynamicClientSamlApplication.
	 * <p>
	 * This constructor initializes:
	 * <ul>
	 *   <li>Self-signed certificates for SAML client and HTTPS encryption</li>
	 *   <li>Kubernetes secrets for truststores and keystores</li>
	 *   <li>Keycloak custom resource with database, ingress, and TLS configuration</li>
	 *   <li>Keycloak realm import with users and required actions</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException if certificate generation or file operations fail
	 */
	public BasicKeycloakOperatorDynamicClientSamlApplication() throws IOException {
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

		final String hostName = OpenShifts.master().generateHostname(APP_NAME);
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
				// TLS passthrough is enabled when you associate a tlsSecret with the http configuration and leave
				// Ingress enabled without specifying a tlsSecret on it
				// (see https://www.keycloak.org/operator/basic-deployment#_deploying_keycloak)
				.withHttp(
						new HttpBuilder()
								.withTlsSecret(httpsSecret.getMetadata().getName())
								.build())
				// On OCP 4.12+ .spec.ingress.className must be set (see https://www.keycloak.org/operator/basic-deployment#_accessing_the_keycloak_deployment)
				.withIngress(
						new IngressBuilder()
								.withClassName("openshift-default")
								.build())
				// The Intersmash Keycloak provisioner sets the keycloak image for the Keycloak CRs, when it is defined
				// via configuration properties. In such a case, as the Keycloak documentation recommends,
				// .spec.startOptimized must be set to false.
				.withStartOptimized(false)
				.endSpec()
				.build();

		keycloakRealmImports.add(new KeycloakRealmImportBuilder()
				.withNewMetadata()
				.withName(REALM_NAME)
				.withLabels(Collections.singletonMap("app", APP_NAME))
				.endMetadata()
				.withNewSpec()
				.withKeycloakCRName(keycloak.getMetadata().getName())
				.withRealm(
						new RealmBuilder()
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
												.build(),
										// user `client` is required for WildFly/JBoss EAP being able to register a new SAML client into Keycloak/RHBK
										new UsersBuilder()
												.withUsername(SSO_USERNAME)
												.withEnabled(true)
												.withCredentials(new CredentialsBuilder()
														.withType("password")
														.withValue(SSO_PASSWORD)
														.build())
												.withRealmRoles("admin")
												.withClientRoles(
														Map.of(
																"realm-management",
																List.of(
																		"create-client",
																		"manage-realm",
																		"manage-clients")))
												.build())
								// When no client is preconfigured, it's assumed it will be created automatically by WildFly/JBoss EAP
								// through user "client" and making use of the "dynamic client registration" feature in the SAML feature pack
								.withClients(
										getClients())
								.build())
				.endSpec()
				.build());
	}

	/**
	 * Returns the list of pre-configured SAML clients.
	 * <p>
	 * This implementation returns null, indicating that no SAML clients are pre-configured.
	 * When using dynamic client registration, the WildFly/JBoss EAP application will
	 * automatically register itself as a SAML client with Keycloak at runtime using
	 * the 'client' user account with realm management permissions.
	 * </p>
	 *
	 * @return null to indicate no pre-configured SAML clients
	 */
	protected Clients getClients() {
		return null;
	}

	/**
	 * Returns the list of Keycloak realm imports.
	 * <p>
	 * The realm imports contain the realm configuration including users, roles, and required actions
	 * for SAML authentication testing.
	 * </p>
	 *
	 * @return list of Keycloak realm imports
	 */
	@Override
	public List<KeycloakRealmImport> getKeycloakRealmImports() {
		return keycloakRealmImports;
	}

	/**
	 * Returns the Keycloak custom resource.
	 *
	 * @return the Keycloak custom resource configuration
	 */
	@Override
	public Keycloak getKeycloak() {
		return this.keycloak;
	}

	/**
	 * Returns the application name.
	 *
	 * @return the application name
	 */
	@Override
	public String getName() {
		return APP_NAME;
	}

	/**
	 * Returns the route to access the Keycloak instance.
	 *
	 * @return the Keycloak route hostname
	 */
	public static String getRoute() {
		return OpenShifts.master().generateHostname(APP_NAME);
	}

	/**
	 * Returns an unmodifiable list of Kubernetes secrets.
	 * <p>
	 * The secrets include:
	 * <ul>
	 *   <li>Truststore secret containing SAML client certificates</li>
	 *   <li>Keystore secret containing HTTPS encryption certificates</li>
	 * </ul>
	 * </p>
	 *
	 * @return unmodifiable list of Kubernetes secrets
	 */
	@Override
	public List<Secret> getSecrets() {
		return Collections.unmodifiableList(secrets);
	}
}
