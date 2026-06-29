/**
* Copyright (C) 2026 Red Hat, Inc.
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
package org.jboss.intersmash.tests.wildfly.postgresql;

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.Secret;
import java.io.IOException;
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
 * WildFly application descriptor for the PostgreSQL EJB Timer application.
 * <p>
 * Configures a WildFly application that uses EJB timers backed by a PostgreSQL database.
 * </p>
 */
@Slf4j
public class WildflyPostgresqlTimerHelmApplication
		implements WildflyHelmChartOpenShiftApplication, WildflyApplicationConfiguration, WildFlyHelmChartsConfiguration {
	/** Application name used for labeling and resource identification. */
	public static final String APP_NAME = "wildfly-postgresql-timer-application";
	private static final String APP_MODULE_DIRECTORY = "wildfly/postgresql-timer-application";

	/** Helm chart release configuration for deploying the WildFly/JBoss EAP application. */
	private final HelmChartRelease release;

	/**
	 * Sets up the OpenShift namespace with required permissions for the WildFly/JBoss EAP application.
	 * <p>
	 * Grants the 'view' role to the default service account, which is required for KUBE_PING clustering.
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
	 * Constructs a new WildFly/JBoss EAP application configured for the PostgreSQL EJB Timer test.
	 * <p>
	 * This constructor sets up the OpenShift namespace, then loads and configures the
	 * Helm chart release with build and deployment settings.
	 *
	 * @throws IOException if Helm chart configuration loading fails
	 */
	public WildflyPostgresqlTimerHelmApplication() throws IOException {
		setupNamespace();
		release = loadRelease(getHelmChartRelease());
	}

	/**
	 * Loads and configures the Helm chart release for the WildFly/JBoss EAP application.
	 * <p>
	 * This method configures:
	 * <ul>
	 *   <li>Build mode (S2I)</li>
	 *   <li>Source repository URL and reference</li>
	 *   <li>Maven build arguments and environment variables</li>
	 *   <li>Builder and runtime images</li>
	 *   <li>Build and deployment environment variables including PostgreSQL connection settings</li>
	 *   <li>Optional EE channel configuration for EAP builds</li>
	 * </ul>
	 * </p>
	 *
	 * @param release the Helm chart release to configure
	 * @return the configured Helm chart release
	 * @throws IOException if configuration loading fails
	 */
	private HelmChartRelease loadRelease(final WildflyHelmChartRelease release) throws IOException {
		// =======================================
		//  WILDFLY HELM CHARTS
		// =======================================

		// let's compute some additional maven args for our s2i build to happen on a Pod
		String mavenAdditionalArgs = "-Denforcer.skip=true";
		// let's add configurable deployment additional args:
		mavenAdditionalArgs = mavenAdditionalArgs.concat(generateAdditionalMavenArgs());
		// to speed up the build process we target a specific module; we also skip generating the "*-sources.jar" to disambiguate the artifact to deploy when using S2I (having both "*-sources.jar" and "*-bootable.jar" would fail the deployment)
		mavenAdditionalArgs = mavenAdditionalArgs
				.concat(getBuildSpecificMavenArgs() + " -Dmaven.source.skip -pl " + APP_MODULE_DIRECTORY + " -am ");

		// =======================================
		//  BUILD
		// =======================================

		Map<String, String> buildEnvironmentVariables = new HashMap<>();
		final String mavenMirrorUrl = this.getMavenMirrorUrl();
		if (!org.assertj.core.util.Strings.isNullOrEmpty(mavenMirrorUrl)) {
			buildEnvironmentVariables.put("MAVEN_MIRROR_URL", mavenMirrorUrl);
			mavenAdditionalArgs = mavenAdditionalArgs.concat(" -Dinsecure.repositories=WARN");
		}

		buildEnvironmentVariables.put("SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");
		buildEnvironmentVariables.put("LOGGING_SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");
		buildEnvironmentVariables.put("MAVEN_ARGS_APPEND", mavenAdditionalArgs);

		// MAVEN_S2I_ARTIFACT_DIRS: tells S2I where to find artifacts to deploy (the "*-bootable.jar" file)
		buildEnvironmentVariables.put("MAVEN_S2I_ARTIFACT_DIRS", APP_MODULE_DIRECTORY + "/target");
		buildEnvironmentVariables.put("POSTGRESQL_DRIVER_VERSION",
				System.getProperty("org.jboss.eap.datasources.postgresql.driver.version"));

		// =======================================
		// DEPLOY
		// =======================================

		Map<String, String> deploymentEnvironmentVariables = new HashMap<>();

		deploymentEnvironmentVariables.put("SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");
		deploymentEnvironmentVariables.put("LOGGING_SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");

		deploymentEnvironmentVariables.put("POSTGRESQL_SERVICE_HOST", PostgresqlService.POSTGRESQL_NAME);
		deploymentEnvironmentVariables.put("POSTGRESQL_SERVICE_PORT", "5432");
		deploymentEnvironmentVariables.put("POSTGRESQL_DATABASE", PostgresqlService.POSTGRESQL_DATABASE);
		deploymentEnvironmentVariables.put("POSTGRESQL_USER", PostgresqlService.POSTGRESQL_USER);
		deploymentEnvironmentVariables.put("POSTGRESQL_PASSWORD", PostgresqlService.POSTGRESQL_PASSWORD);

		// =======================================
		// APPLICATION
		// =======================================
		WildflyHelmChartRelease.JdkImage.Version jdkVersion = getJdkImageVersion();
		release
				// we explicitly set we need an s2i build, ot Bootable JAR, otherwise the OpenJDK image would be
				// used since the Bootable JAR mode is the default.
				.withBuildMode(getBuildMode())
				.withSourceRepositoryUrl(IntersmashConfig.deploymentsRepositoryUrl())
				.withSourceRepositoryRef(IntersmashConfig.deploymentsRepositoryRef())
				.withJdkBuilderImage(
						new WildflyHelmChartRelease.JdkImage(IntersmashConfig.wildflyImageURL(), jdkVersion))
				.withJdkRuntimeImage(
						new WildflyHelmChartRelease.JdkImage(IntersmashConfig.wildflyRuntimeImageURL(), jdkVersion))
				.withBuildEnvironmentVariables(buildEnvironmentVariables)
				.withDeploymentEnvironmentVariables(deploymentEnvironmentVariables);
		// Bootable Jar image can optionally be overridden with e.g. -Dintersmash.bootable.jar.image=registry.access.redhat.com/ubi10/openjdk-25:latest
		if (!Strings.isNullOrEmpty(IntersmashConfig.bootableJarImageURL())) {
			release.setBootableJarBuilderImage(IntersmashConfig.bootableJarImageURL());
		}
		List<String> channelDefinition = Arrays.asList(this.eeChannelGroupId(), this.eeChannelArtifactId(),
				this.eeChannelVersion());
		if (!channelDefinition.isEmpty()) {
			// an example of EAP channel usage, not working with EAP 7.4.x or WildFly
			release.withS2iChannel(channelDefinition.stream().collect(Collectors.joining(":")));
		}

		return release;
	}

	/**
	 * Returns the build mode for the Helm chart release.
	 * <p>
	 * This base implementation returns {@link WildflyHelmChartRelease.BuildMode#S2I}.
	 * Subclasses may override this to use a different build mode (e.g. Bootable JAR).
	 *
	 * @return the build mode
	 */
	protected WildflyHelmChartRelease.BuildMode getBuildMode() {
		return WildflyHelmChartRelease.BuildMode.S2I;
	}

	/**
	 * Returns additional Maven arguments specific to the build mode.
	 * <p>
	 * This base implementation returns an empty string (no additional build-specific args for S2I).
	 * Subclasses may override this to activate specific Maven profiles (e.g. {@code bootable-jar}).
	 *
	 * @return build-specific Maven arguments, or empty string
	 */
	protected String getBuildSpecificMavenArgs() {
		return "";
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
	 * Returns an empty list of Kubernetes secrets.
	 *
	 * @return empty list of Kubernetes secrets
	 */
	@Override
	public List<Secret> getSecrets() {
		return Collections.EMPTY_LIST;
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
	 * Returns the route to access the WildFly/JBoss EAP application.
	 *
	 * @return the WildFly/JBoss EAP application route hostname
	 */
	public static String getRoute() {
		return OpenShifts.master().generateHostname(APP_NAME);
	}
}
