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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;
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
 * This class is used to verify the third option
 */
@Slf4j
public class WildflyWithElytronOidcClientWithS2IVariablesApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	public static final String APP_NAME = "elytron-oidc-client";

	private final BuildInput buildInput;
	private final List<EnvVar> environmentVariables = new ArrayList<>();

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

	public WildflyWithElytronOidcClientWithS2IVariablesApplication() {
		setupNamespace();

		String applicationDir = "wildfly/elytron-oidc-client-keycloak";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();
		environmentVariables.add(new EnvVarBuilder()
				.withName("SSO_APP_SERVICE")
				.withValue(String.format("https://%s", BasicKeycloakOperatorApplication.getRoute()))
				.build());

		// START OIDC S2I VARIABLES

		// still set to "rh-sso" here, see
		// https://docs.redhat.com/it/documentation/red_hat_jboss_enterprise_application_platform/8.1/html-single/using_jboss_eap_on_openshift_container_platform/index#ref_environment-variable-based-configuration_assembly_using-openid-connect-to-secure-jboss-eap-applications-on-openshift
		environmentVariables.add(new EnvVarBuilder()
				.withName("OIDC_PROVIDER_NAME")
				.withValue("rh-sso")
				.build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("OIDC_PROVIDER_URL")
				.withValue(String.format("https://%s/realms/%s", BasicKeycloakOperatorApplication.getRoute(), "basic-auth"))
				.build());

		/** to allow self signet certificate */
		environmentVariables.add(new EnvVarBuilder().withName("OIDC_DISABLE_SSL_CERTIFICATE_VALIDATION")
				.withValue("true").build());

		/** This env variable is required in case of dynamic client registration */
		environmentVariables.add(new EnvVarBuilder()
				.withName("OIDC_SECURE_DEPLOYMENT_SECRET")
				.withValue("foo-secret")
				.build());

		/** in case the env variable APPLICATION_NAME is set the client-id is prefixed by the value */
		environmentVariables.add(new EnvVarBuilder().withName("APPLICATION_NAME")
				.withValue(APP_NAME).build());

		/** The two following variables are required by the Dynamic registration of clients feature. */
		environmentVariables.add(new EnvVarBuilder().withName("OIDC_USER_NAME")
				.withValue(BasicKeycloakOperatorApplication.OIDC_USER_NAME).build());
		environmentVariables.add(new EnvVarBuilder().withName("OIDC_USER_PASSWORD")
				.withValue(BasicKeycloakOperatorApplication.OIDC_USER_PASSWORD).build());

		// END OIDC S2I VARIABLES

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

	@Override
	public BuildInput getBuildInput() {
		return buildInput;
	}

	@Override
	public String getName() {
		return APP_NAME;
	}

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
}
