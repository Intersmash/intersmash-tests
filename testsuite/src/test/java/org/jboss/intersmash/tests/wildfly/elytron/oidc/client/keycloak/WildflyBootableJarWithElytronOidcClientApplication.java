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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.openshift.helm.HelmChartRelease;
import org.jboss.intersmash.application.openshift.helm.WildflyHelmChartOpenShiftApplication;
import org.jboss.intersmash.provision.helm.wildfly.WildflyHelmChartRelease;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;
import org.jboss.intersmash.tests.wildfly.util.WildFlyHelmChartsConfiguration;

/**
 * WildFly image based OpenShift application descriptor that uses the <a href="https://github.com/Intersmash/intersmash-applications/blob/main/wildfly/elytron-oidc-client-keycloak">elytron-oidc-client-keycloak</a>
 * deployment.
 *
 *  You can configure the elytron-oidc-client subsystem in three different ways:
 * <ul>
 *     <li>Adding an oidc.json into your deployment.</li>
 *     <li>Running a CLI script to configure the elytron-oidc-client subsystem.</li>
 *     <li>Defining environment variables to configure an elytron-oidc-client subsystem on start of JBoss EAP server on OpenShift.</li>
 * </ul>
 *
 * This class is used to verify the first option
 */
@Slf4j
public class WildflyBootableJarWithElytronOidcClientApplication
		implements WildflyHelmChartOpenShiftApplication, WildflyApplicationConfiguration {
	/** Application name used for labeling and resource identification. */
	public static final String APP_NAME = "elytron-oidc-client";

	/** List of Kubernetes secrets for keystores and truststores. */
	private final List<Secret> secrets = new ArrayList<>();

	/** Helm chart release configuration for deploying the WildFly/JBoss EAP application. */
	private final HelmChartRelease release;

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
	}

	/**
	 * Constructs a new WildFly/JBoss EAP application configured with Keycloak SAML adapter.
	 * <p>
	 * This constructor initializes the application by:
	 * <ul>
	 *   <li>Setting up the OpenShift namespace with required permissions</li>
	 *   <li>Loading and configuring the Helm chart release with build and deployment settings</li>
	 *   <li>Configuring environment variables for Keycloak integration</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException if Helm chart configuration loading fails
	 */
	public WildflyBootableJarWithElytronOidcClientApplication() throws IOException {
		setupNamespace();
		release = loadRelease(WildFlyHelmChartsConfiguration.getHelmChartRelease());
	}

	/**
	 * Loads and configures the Helm chart release for the WildFly/JBoss EAP application.
	 * <p>
	 * This method configures:
	 * <ul>
	 *   <li>Build mode (Bootable JAR)</li>
	 *   <li>Source repository URL and reference</li>
	 *   <li>Maven build arguments and environment variables</li>
	 *   <li>Builder and runtime images</li>
	 *   <li>Build and deployment environment variables including Keycloak integration settings</li>
	 *   <li>Optional EE channel configuration for EAP builds</li>
	 * </ul>
	 * </p>
	 *
	 * @param release the WildFly Helm chart release to configure
	 * @return the configured Helm chart release
	 * @throws IOException if configuration loading fails
	 */
	private HelmChartRelease loadRelease(final WildflyHelmChartRelease release) throws IOException {
		// =======================================
		//  WILDFLY HELM CHARTS
		// =======================================

		//let's compute some additional maven args for our s2i build to happen on a Pod
		String mavenAdditionalArgs = "-Denforcer.skip=true";
		// let's add configurable deployment additional args:
		mavenAdditionalArgs = mavenAdditionalArgs.concat(generateAdditionalMavenArgs());
		// let's pass the profile for building the deployment too...
		mavenAdditionalArgs = mavenAdditionalArgs
				.concat(" " + WildflyApplicationConfiguration.getWildflyApplicationTargetDistributionProfile());

		// =======================================
		//  BUILD
		// =======================================

		Map<String, String> buildEnvironmentVariables = new HashMap<>();
		final String mavenMirrorUrl = this.getMavenMirrorUrl();
		if (!Strings.isNullOrEmpty(mavenMirrorUrl)) {
			buildEnvironmentVariables.put("MAVEN_MIRROR_URL", mavenMirrorUrl);
			mavenAdditionalArgs = mavenAdditionalArgs.concat(" -Dinsecure.repositories=WARN");
		}

		buildEnvironmentVariables.put("SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");
		buildEnvironmentVariables.put("LOGGING_SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");
		buildEnvironmentVariables.put("MAVEN_ARGS_APPEND", mavenAdditionalArgs);
		// realm-name in web.xml is set a compile time
		buildEnvironmentVariables.put("SSO_OIDC_KEYCLOAK_REALM",
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.REALM_NAME);

		// =======================================
		// DEPLOY
		// =======================================

		Map<String, String> deploymentEnvironmentVariables = new HashMap<>();

		deploymentEnvironmentVariables.put("SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");
		deploymentEnvironmentVariables.put("LOGGING_SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");

		deploymentEnvironmentVariables.put("SSO_OIDC_KEYCLOAK_URL",
				String.format("https://%s", BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.getRoute()));
		deploymentEnvironmentVariables.put("SSO_OIDC_CLIENT_ID",
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.SSO_OIDC_CLIENT_ID);
		deploymentEnvironmentVariables.put("SSO_OIDC_CLIENT_SECRET",
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.SSO_OIDC_CLIENT_SECRET);
		deploymentEnvironmentVariables.put("SSO_OIDC_KEYCLOAK_REALM",
				BasicKeycloakOperatorDynamicClientOidcBootableJarApplication.REALM_NAME);

		// =======================================
		// APPLICATION
		// =======================================
		release
				// we explicitly set we need an s2i build, ot Bootable JAR, otherwise the OpenJDK image would be
				// used since the Bootable JAR mode is the default.
				.withBuildMode(WildflyHelmChartRelease.BuildMode.BOOTABLE_JAR)
				.withSourceRepositoryUrl(IntersmashConfig.deploymentsRepositoryUrl())
				.withSourceRepositoryRef(IntersmashConfig.deploymentsRepositoryRef())
				.withContextDir("wildfly/elytron-oidc-client-keycloak-bootable-jar")
				// TODO: check why jdk17 / jdk21 in `intersmash.bootable.jar.image=registry.access.redhat.com/ubi8/openjdk-17` and `intersmash.wildfly.image=quay.io/wildfly/wildfly-s2i:2.1.0-jdk21`
				.withJdk17BuilderImage(IntersmashConfig.wildflyImageURL())
				.withJdk17RuntimeImage(IntersmashConfig.wildflyRuntimeImageURL())
				.withBuildEnvironmentVariables(buildEnvironmentVariables)
				.withDeploymentEnvironmentVariables(deploymentEnvironmentVariables);
		List<String> channelDefinition = Arrays.asList(this.eeChannelGroupId(), this.eeChannelArtifactId(),
				this.eeChannelVersion());
		if (!channelDefinition.isEmpty()) {
			// an example of EAP channel usage, not working with EAP 7.4.x or WildFly
			release.withS2iChannel(channelDefinition.stream().collect(Collectors.joining(":")));
		}

		return release;
	}

	/**
	 * Returns the builder image URL for Source-to-Image builds.
	 *
	 * @return the WildFly/JBoss EAP builder image URL
	 */
	@Override
	public String getBuilderImage() {
		return IntersmashConfig.wildflyImageURL();
	}

	/**
	 * Returns the runtime image URL for the Bootable JAR deployment.
	 *
	 * @return the WildFly/JBoss EAP runtime image URL
	 */
	@Override
	public String getRuntimeImage() {
		return IntersmashConfig.wildflyRuntimeImageURL();
	}

	/**
	 * Returns the configured Helm chart release.
	 *
	 * @return the Helm chart release configuration
	 */
	@Override
	public HelmChartRelease getRelease() {
		return release;
	}

	/**
	 * Returns the Helm charts repository URL.
	 *
	 * @return the WildFly Helm charts repository URL
	 */
	@Override
	public String getHelmChartsRepositoryUrl() {
		return IntersmashConfig.getWildflyHelmChartsRepo();
	}

	/**
	 * Returns the Helm charts repository reference (branch or tag).
	 *
	 * @return the WildFly Helm charts repository reference
	 */
	@Override
	public String getHelmChartsRepositoryRef() {
		return IntersmashConfig.getWildflyHelmChartsBranch();
	}

	/**
	 * Returns the Helm charts repository name.
	 *
	 * @return the WildFly Helm charts repository name
	 */
	@Override
	public String getHelmChartsRepositoryName() {
		return IntersmashConfig.getWildflyHelmChartsName();
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
	 * Returns the route hostname to access the WildFly/JBoss EAP application.
	 * <p>
	 * This method generates the OpenShift route hostname based on the application name.
	 * The route is used by the Keycloak client configuration for callback URLs and
	 * redirect URIs.
	 * </p>
	 *
	 * @return the WildFly/JBoss EAP application route hostname
	 */
	public static String getRoute() {
		return OpenShifts.master().generateHostname(APP_NAME);
	}
}
