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
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;
import org.jboss.intersmash.tests.wildfly.web.cache.offload.infinispan.util.OpenShiftBinaryClient;

/**
 * WildFly/JBoss EAP Application configured with Keycloak SAML Adapter for authentication.
 * <p>
 * This application deploys a WildFly/JBoss EAP instance that:
 * <ul>
 *   <li>Uses the Keycloak SAML adapter for securing web resources</li>
 *   <li>Configures keystores and truststores for SAML encryption and signing</li>
 *   <li>Connects to an external Keycloak/RHBK service for authentication</li>
 *   <li>Automatically registers as a SAML client with the Keycloak realm</li>
 *   <li>Supports HTTPS communication with the Keycloak service</li>
 * </ul>
 * </p>
 * <p>
 * The application sets up the necessary permissions for KUBE_PING clustering and route discovery,
 * configures Maven build parameters, and creates the required Kubernetes secrets for keystores.
 * </p>
 */
@Slf4j
public class WildflyWithKeycloakSamlAdapterApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {
	/** Application name used for labeling and resource identification. */
	public static final String APP_NAME = "keycloak-saml-adapter";

	/** Certificate name used for SAML encryption and signing. */
	public static final String SSO_SAML_CERTIFICATE_NAME = APP_NAME;

	/** Keystore password for SAML certificates. */
	public static final String SSO_SAML_KEYSTORE_PASSWORD = "1234password";

	/** Directory path for TLS certificates and keystores within the container. */
	private static final String TLS_CERTIFICATE_DIR_NAME = "/etc/secrets";

	/** Build input configuration for the WildFly/JBoss EAP S2I build. */
	private final BuildInput buildInput;

	/** List of environment variables for the WildFly/JBoss EAP container. */
	private final List<EnvVar> environmentVariables = new ArrayList<>();

	/** List of Kubernetes secrets for keystores and truststores. */
	private final List<Secret> secrets = new ArrayList<>();

	/**
	 * Sets up the OpenShift namespace with required permissions for the WildFly/JBoss EAP application.
	 * <p>
	 * This method configures:
	 * <ul>
	 *   <li>The 'view' role for the default service account (required for KUBE_PING clustering)</li>
	 *   <li>A 'routeview' role with permissions to list routes (required for route discovery)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Without these permissions, JGroups clustering and the SAML adapter's route discovery
	 * feature will not function properly.
	 * </p>
	 *
	 * @throws IllegalStateException if the OpenShift commands fail to execute
	 */
	public static void setupNamespace() {
		// KUBE_PING requires this permission, otherwise warning is logged and clustering doesn't work,
		// an invalidation-cache requires a functioning jgroups cluster.
		log.debug("Adding role view to default service account...");
		OpenShifts.master().addRoleToServiceAccount("view", "default");

		// Needed for feature "The Routes discovery" to work;
		// Instead of using HOSTNAME_HTTPS/S the service account require permission to list routes of project.
		// Execute the two following commands to permit list routes to service account for current namespace:
		//   oc create role routeview --verb=list --resource=route
		//   oc policy add-role-to-user routeview system:serviceaccount:<namespace-name>:default --role-namespace=<namespace-name> -n <namespace-name>
		// Without this, the following error would be produced:
		//   "routes.route.openshift.io is forbidden: User system:serviceaccount:appsint-5i7y:default
		//   cannot list resource routes in API group route.openshift.io in the namespace"
		log.debug("Adding permission to list routes to default service account...");
		String error = "ERROR: cannot create and assign the role to the service account";
		try {
			// KUBE_PING needs view permissions
			OpenShifts.master().addRoleToServiceAccount("view", "default");
			// The app URL is needed for automatic client registration
			OpenShiftBinaryClient instance = OpenShiftBinaryClient.getInstance();
			String namespaceName = OpenShifts.master().getNamespace();
			String serviceAccount = String.format("system:serviceaccount:%s:default", namespaceName);
			String roleNameSpace = String.format("--role-namespace=%s", namespaceName);

			instance.executeCommand(error, "project", namespaceName);
			instance.executeCommand(error, "create", "role", "routeview", "--verb=list", "--resource=route");
			instance.executeCommand(error, "policy", "add-role-to-user", "routeview",
					serviceAccount, roleNameSpace, "-n", namespaceName);
		} catch (IllegalStateException e) {
			log.error("Error during execution OC command: {}", e.getMessage());
			throw e;
		}
	}

	/**
	 * Constructs a new WildflyWithKeycloakSamlAdapterApplication.
	 * <p>
	 * This constructor initializes:
	 * <ul>
	 *   <li>OpenShift namespace with required permissions</li>
	 *   <li>Build input configuration for Maven S2I build</li>
	 *   <li>Environment variables for SAML adapter configuration</li>
	 *   <li>Keystores and truststores for SAML encryption and HTTPS communication</li>
	 *   <li>Kubernetes secrets containing the certificates</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException if certificate generation or file operations fail
	 */
	public WildflyWithKeycloakSamlAdapterApplication() throws IOException {
		setupNamespace();

		String applicationDir = "wildfly/keycloak-saml-adapter";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		environmentVariables.add(
				new EnvVarBuilder().withName("MAVEN_S2I_ARTIFACT_DIRS")
						.withValue(applicationDir + "/target")
						.build());
		String mavenAdditionalArgs = generateAdditionalMavenArgs()
				.concat(" -pl " + applicationDir + " -am");
		final String mavenMirrorUrl = this.getMavenMirrorUrl();
		if (!Strings.isNullOrEmpty(mavenMirrorUrl)) {
			environmentVariables.add(
					new EnvVarBuilder().withName("MAVEN_MIRROR_URL")
							.withValue(mavenMirrorUrl)
							.build());
			mavenAdditionalArgs = mavenAdditionalArgs.concat(" -Dinsecure.repositories=WARN");
		}
		environmentVariables.add(
				new EnvVarBuilder().withName("MAVEN_ARGS_APPEND")
						.withValue(mavenAdditionalArgs)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_URL")
						.withValue("https://" + BasicKeycloakOperatorDynamicClientSamlApplication.getRoute())
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_REALM")
						.withValue(BasicKeycloakOperatorDynamicClientSamlApplication.REALM_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_USERNAME")
						.withValue(BasicKeycloakOperatorDynamicClientSamlApplication.SSO_USERNAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_PASSWORD")
						.withValue(BasicKeycloakOperatorDynamicClientSamlApplication.SSO_PASSWORD)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_SAML_LOGOUT_PAGE")
						.withValue("/index.jsp")
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SCRIPT_DEBUG")
						.withValue(IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false")
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("LOGGING_SCRIPT_DEBUG")
						.withValue(IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false")
						.build());

		// Private Key + Self-signed certificate for SAML requests encrypt and SAML assertions decrypt
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo wildflyCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(APP_NAME),
						SSO_SAML_CERTIFICATE_NAME,
						SSO_SAML_KEYSTORE_PASSWORD,
						SSO_SAML_KEYSTORE_PASSWORD,
						Collections.emptyList());

		/*
		    Cryptographic Keys can be used for both:
		     - HTTP Traffic encryption
		     - and/or SAML messages signing
		    See https://www.keycloak.org/securing-apps/saml-galleon-layers#_saml-general-config
		 */

		// We need to trust Keycloak certificate when communicating over HTTPS
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo keycloakCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(BasicKeycloakOperatorDynamicClientSamlApplication.APP_NAME),
						BasicKeycloakOperatorDynamicClientSamlApplication.HTTPS_CERTIFICATE_NAME,
						BasicKeycloakOperatorDynamicClientSamlApplication.HTTPS_KEYSTORE_PASSWORD,
						BasicKeycloakOperatorDynamicClientSamlApplication.HTTPS_KEYSTORE_PASSWORD,
						Collections.emptyList());

		Secret keystoreAndTruststoreSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(APP_NAME + "-keystore-and-truststore")
				.withLabels(Collections.singletonMap("app", APP_NAME))
				.endMetadata()
				.addToData(Map.of("keystore.jks",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(wildflyCertificate.keystore.toFile()))))
				.addToData(Map.of("truststore.pkcs12",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(keycloakCertificate.truststore.toFile()))))
				.build();
		secrets.add(keystoreAndTruststoreSecret);

		// SSO_SAML_KEYSTORE_DIR/SSO_SAML_KEYSTORE : together form the complete path to the KeyStore
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_SAML_KEYSTORE_DIR")
						.withValue(TLS_CERTIFICATE_DIR_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_SAML_KEYSTORE")
						.withValue("keystore.jks")
						.build());
		// SSO_SAML_KEYSTORE_PASSWORD : password for both KeyStore and PrivateKey
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_SAML_KEYSTORE_PASSWORD")
						.withValue(SSO_SAML_KEYSTORE_PASSWORD)
						.build());
		// SSO_SAML_CERTIFICATE_NAME : alias for both PrivateKey and Certificate
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_SAML_CERTIFICATE_NAME")
						.withValue(SSO_SAML_CERTIFICATE_NAME)
						.build());

		// Set truststore for the SP (the WildFly/JBoss EAP application)
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_TRUSTSTORE")
						.withValue("truststore.pkcs12")
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_TRUSTSTORE_DIR")
						.withValue(TLS_CERTIFICATE_DIR_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_TRUSTSTORE_CERTIFICATE_ALIAS")
						.withValue(BasicKeycloakOperatorDynamicClientSamlApplication.HTTPS_CERTIFICATE_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("SSO_TRUSTSTORE_PASSWORD")
						.withValue(BasicKeycloakOperatorDynamicClientSamlApplication.HTTPS_KEYSTORE_PASSWORD)
						.build());
	}

	/**
	 * Returns the build input configuration for the WildFly/JBoss EAP S2I build.
	 *
	 * @return the build input configuration
	 */
	@Override
	public BuildInput getBuildInput() {
		return buildInput;
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
	 * Returns an unmodifiable list of environment variables for the WildFly/JBoss EAP container.
	 * <p>
	 * The environment variables include configuration for:
	 * <ul>
	 *   <li>Maven build parameters</li>
	 *   <li>SAML adapter settings (SSO_URL, SSO_REALM, etc.)</li>
	 *   <li>Keystore and truststore paths and passwords</li>
	 *   <li>Debug logging settings</li>
	 * </ul>
	 * </p>
	 *
	 * @return unmodifiable list of environment variables
	 */
	@Override
	public List<EnvVar> getEnvVars() {
		return Collections.unmodifiableList(environmentVariables);
	}

	/**
	 * Returns the route to access the WildFly/JBoss EAP application.
	 *
	 * @return the WildFly/JBoss EAP application route hostname
	 */
	public static String getRoute() {
		return OpenShifts.master().generateHostname(APP_NAME);
	}

	/**
	 * Returns an unmodifiable list of Kubernetes secrets.
	 * <p>
	 * The secrets include:
	 * <ul>
	 *   <li>SAML keystore for encryption and signing</li>
	 *   <li>HTTPS truststore for secure communication with Keycloak</li>
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
