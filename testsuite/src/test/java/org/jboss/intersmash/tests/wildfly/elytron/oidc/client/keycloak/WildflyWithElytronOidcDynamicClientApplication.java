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
 * WildFly image based OpenShift application descriptor that uses the wildfly-with-elytron-oidc-client
 * deployment.
 *
 *  You can configure the elytron-oidc-client subsystem in three different ways:
 * <ul>
 *     <li>Adding an oidc.json into your deployment.</li>
 *     <li>Running a CLI script to configure the elytron-oidc-client subsystem.</li>
 *     <li>Defining environment variables to configure an elytron-oidc-client subsystem on start of JBoss EAP server on OpenShift.</li>
 * </ul>
 *
 * This class is used to verify the third option: this is generally referred to as "OIDC Dynamic client registration";
 *
 * The details can be checked in the `oidc_configure_remote_client` function in
 * [oidc.sh](https://github.com/wildfly/wildfly-cekit-modules/blob/main/jboss/container/wildfly/launch/oidc/added/oidc.sh);<br>
 * The general "OIDC Dynamic Client registration" process is described in the following:<br>
 * <ul>
 *   <li>OIDC_USER_NAME and OIDC_USER_PASSWORD credentials are used to obtain a Token from the "/protocol/openid-connect/token" Keycloak endpoint</li>
 *   <li>The Token is used to register an OIDC client through the "/clients-registrations/default" Keycloak endpoint</li>
 *   <li>NOTE: the certificate presented by the "/protocol/openid-connect/token" and "/clients-registrations/default" Keycloak endpoints,
 *     is verified using the truststore specified by the OIDC_PROVIDER_TRUSTSTORE variables (Keycloak certificate
 *     verification can be skipped by setting OIDC_DISABLE_SSL_CERTIFICATE_VALIDATION="true")</li>
 *   <li>the OIDC client is registered with the password specified in the OIDC_SECURE_DEPLOYMENT_SECRET variable</li>
 * </ul>
 */
@Slf4j
public class WildflyWithElytronOidcDynamicClientApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	public static final String APP_NAME = "elytron-oidc-client";

	private final BuildInput buildInput;
	private final List<EnvVar> environmentVariables = new ArrayList<>();

	/** Directory path for TLS certificates and keystores within the container. */
	private static final String TLS_CERTIFICATE_DIR_NAME = "/etc/secrets";

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
		// The app URL is needed for automatic client registration
		OpenShiftBinaryClient instance = OpenShiftBinaryClient.getInstance();
		String namespaceName = OpenShifts.master().getNamespace();
		String serviceAccount = String.format("system:serviceaccount:%s:default", namespaceName);
		String roleNameSpace = String.format("--role-namespace=%s", namespaceName);

		instance.executeCommand(error, "project", namespaceName);
		instance.executeCommand(error, "create", "role", "routeview", "--verb=list", "--resource=route");
		instance.executeCommand(error, "policy", "add-role-to-user", "routeview",
				serviceAccount, roleNameSpace, "-n", namespaceName);
	}

	/**
	 * Creates a new WildFly application with Elytron OIDC client configured using environment variables
	 * for dynamic client registration.
	 *
	 * @throws IOException if an I/O error occurs during certificate generation
	 */
	public WildflyWithElytronOidcDynamicClientApplication() throws IOException {
		setupNamespace();

		String applicationDir = "wildfly/elytron-oidc-client-keycloak";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();
		environmentVariables.add(new EnvVarBuilder()
				.withName("SSO_APP_SERVICE")
				.withValue(String.format("https://%s", BasicKeycloakOperatorDynamicClientApplication.getRoute()))
				.build());

		// still set to "rh-sso" here, see
		// https://docs.redhat.com/it/documentation/red_hat_jboss_enterprise_application_platform/8.1/html-single/using_jboss_eap_on_openshift_container_platform/index#ref_environment-variable-based-configuration_assembly_using-openid-connect-to-secure-jboss-eap-applications-on-openshift
		// should be "keycloak" or "rh-sso"
		environmentVariables.add(new EnvVarBuilder()
				.withName("OIDC_PROVIDER_NAME")
				.withValue("rh-sso")
				.build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("OIDC_PROVIDER_URL")
				.withValue(String.format("https://%s/realms/%s", BasicKeycloakOperatorDynamicClientApplication.getRoute(),
						"basic-auth"))
				.build());

		// We need to trust Keycloak certificate when communicating over HTTPS (alternatively we can set
		// OIDC_DISABLE_SSL_CERTIFICATE_VALIDATION=true and remove all the OIDC_PROVIDER_* variables)
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo keycloakCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(BasicKeycloakOperatorDynamicClientApplication.APP_NAME),
						BasicKeycloakOperatorDynamicClientApplication.HTTPS_CERTIFICATE_NAME,
						BasicKeycloakOperatorDynamicClientApplication.HTTPS_KEYSTORE_PASSWORD,
						BasicKeycloakOperatorDynamicClientApplication.HTTPS_KEYSTORE_PASSWORD,
						Collections.emptyList());

		Secret keystoreAndTruststoreSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(APP_NAME + "-truststore")
				.withLabels(Collections.singletonMap("app", APP_NAME))
				.endMetadata()
				.addToData(Map.of("truststore.pkcs12",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(keycloakCertificate.truststore.toFile()))))
				.build();
		secrets.add(keystoreAndTruststoreSecret);

		// Set truststore for the SP (the WildFly/JBoss EAP application)
		environmentVariables.add(
				new EnvVarBuilder().withName("OIDC_PROVIDER_TRUSTSTORE")
						.withValue("truststore.pkcs12")
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("OIDC_PROVIDER_TRUSTSTORE_DIR")
						.withValue(TLS_CERTIFICATE_DIR_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("OIDC_PROVIDER_TRUSTSTORE_CERTIFICATE_ALIAS")
						.withValue(BasicKeycloakOperatorDynamicClientApplication.HTTPS_CERTIFICATE_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("OIDC_PROVIDER_TRUSTSTORE_PASSWORD")
						.withValue(BasicKeycloakOperatorDynamicClientApplication.HTTPS_KEYSTORE_PASSWORD)
						.build());

		/** in case the env variable APPLICATION_NAME is set the client-id is prefixed by the value */
		environmentVariables.add(new EnvVarBuilder().withName("APPLICATION_NAME")
				.withValue(APP_NAME).build());

		/** The two following variables are required by the dynamic registration of clients feature: they are used to
		 * authenticate to the registration service and actually perform client registration */
		environmentVariables.add(new EnvVarBuilder().withName("OIDC_USER_NAME")
				.withValue(BasicKeycloakOperatorDynamicClientApplication.OIDC_USER_NAME).build());
		environmentVariables.add(new EnvVarBuilder().withName("OIDC_USER_PASSWORD")
				.withValue(BasicKeycloakOperatorDynamicClientApplication.OIDC_USER_PASSWORD).build());

		/** This env variable is required in case of dynamic client registration: it's the password that is set for the
		 * OIDC client during dynamic client registration */
		environmentVariables.add(new EnvVarBuilder()
				.withName("OIDC_SECURE_DEPLOYMENT_SECRET")
				.withValue(BasicKeycloakOperatorDynamicClientApplication.OIDC_SECURE_DEPLOYMENT_SECRET)
				.build());

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
						.withValue(mavenAdditionalArgs + " -Pno-oidc-json")
						.build());

		environmentVariables.add(
				new EnvVarBuilder().withName("SCRIPT_DEBUG")
						.withValue(IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false")
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("LOGGING_SCRIPT_DEBUG")
						.withValue(IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false")
						.build());
	}

	/**
	 * Get the build input configuration for this application.
	 *
	 * @return the build input configuration
	 */
	@Override
	public BuildInput getBuildInput() {
		return buildInput;
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
	 * Get the environment variables for this application.
	 *
	 * @return unmodifiable list of environment variables
	 */
	@Override
	public List<EnvVar> getEnvVars() {
		return Collections.unmodifiableList(environmentVariables);
	}

	/**
	 * Get a route to the application.
	 *
	 * @return route to the application
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
