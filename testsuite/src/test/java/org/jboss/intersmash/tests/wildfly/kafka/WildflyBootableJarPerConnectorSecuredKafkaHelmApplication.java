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
package org.jboss.intersmash.tests.wildfly.kafka;

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
 * WildFly/JBoss EAP application configured for per-connector SSL with Kafka/Streams for Apache Kafka,
 * deployed as a bootable JAR via Helm Charts.
 * <p>
 * Unlike {@link WildflyBootableJarGloballySecuredKafkaHelmApplication} which uses a single global
 * {@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_WILDFLY_ELYTRON_SSL_CONTEXT} environment variable,
 * this application configures SSL per-connector via
 * {@code MP_MESSAGING_OUTGOING_SSLTO_WILDFLY_ELYTRON_SSL_CONTEXT} and
 * {@code MP_MESSAGING_INCOMING_SSLFROM_WILDFLY_ELYTRON_SSL_CONTEXT}.
 */
@Slf4j
public class WildflyBootableJarPerConnectorSecuredKafkaHelmApplication
		implements WildflyHelmChartOpenShiftApplication, WildflyApplicationConfiguration, WildFlyHelmChartsConfiguration {
	/** Application name used for labeling and resource identification. */
	public static final String APP_NAME = "mp-reactive-messaging-pc-bjar";
	private static final String APP_MODULE_DIRECTORY = "wildfly/kafka-application";

	private static final String CLIENT_SSL_CONTEXT_NAME = "kafka-ssl-test";
	private static final String SECRETS_VOLUME_MOUNT_PATH = "/etc/secrets";
	private static final String CERTIFICATE_SECRET_PATH = SECRETS_VOLUME_MOUNT_PATH + "/ca.p12";

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
	 * </ul>
	 * </p>
	 *
	 * @throws IllegalStateException if the OpenShift commands fail to execute
	 */
	public static void setupNamespace() {
		log.debug("Adding role view to default service account...");
		OpenShifts.master().addRoleToServiceAccount("view", "default");
	}

	/**
	 * Constructs a new WildFly/JBoss EAP application configured with per-connector SSL for Kafka.
	 *
	 * @throws IOException if Helm chart configuration loading fails
	 */
	public WildflyBootableJarPerConnectorSecuredKafkaHelmApplication() throws IOException {
		setupNamespace();
		release = loadRelease(getHelmChartRelease());
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
	 *   <li>Build and deployment environment variables including per-connector SSL settings</li>
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

		// let's compute some additional maven args for our s2i build to happen on a Pod
		String mavenAdditionalArgs = "-Denforcer.skip=true";
		// let's add configurable deployment additional args:
		mavenAdditionalArgs = mavenAdditionalArgs.concat(generateAdditionalMavenArgs());
		// to speed up the build process we target a specific module; we also skip generating the "*-sources.jar" to disambiguate the S2I artifact to deploy (having both "*-sources.jar" and "*-bootable.jar" would fail the deployment)
		mavenAdditionalArgs = mavenAdditionalArgs.concat(" -Dmaven.source.skip -pl " + APP_MODULE_DIRECTORY + " -am ");

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

		// MAVEN_S2I_ARTIFACT_DIRS: tells S2I where to find artifacts to deploy (the "*-bootable.jar" file)
		buildEnvironmentVariables.put("MAVEN_S2I_ARTIFACT_DIRS", APP_MODULE_DIRECTORY + "/target");

		// =======================================
		// DEPLOY
		// =======================================

		Map<String, String> deploymentEnvironmentVariables = new HashMap<>();

		deploymentEnvironmentVariables.put("SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");
		deploymentEnvironmentVariables.put("LOGGING_SCRIPT_DEBUG",
				IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false");

		// =======================================
		// SECRET
		// =======================================

		Secret clientSecret = OpenShifts.master().getSecret("amq-streams-cluster-ca-cert");
		clientSecret.getMetadata().setName("client-amq-streams-cluster-ca-cert-secret");
		clientSecret.getMetadata().setResourceVersion(null);
		String password = new String(Base64.getDecoder().decode(clientSecret.getData().get("ca.password")));
		secrets.add(clientSecret);

		// SSL
		deploymentEnvironmentVariables.put("KEYSTORE_PATH", CERTIFICATE_SECRET_PATH);
		deploymentEnvironmentVariables.put("KEYSTORE_PASSWORD", password);

		// Configure the Elytron SSL context per-connector (outgoing and incoming)
		deploymentEnvironmentVariables.put("MP_MESSAGING_OUTGOING_SSLTO_WILDFLY_ELYTRON_SSL_CONTEXT",
				CLIENT_SSL_CONTEXT_NAME);
		deploymentEnvironmentVariables.put("MP_MESSAGING_INCOMING_SSLFROM_WILDFLY_ELYTRON_SSL_CONTEXT",
				CLIENT_SSL_CONTEXT_NAME);

		// =======================================
		// APPLICATION
		// =======================================
		release
				.withBuildMode(WildflyHelmChartRelease.BuildMode.BOOTABLE_JAR)
				.withSourceRepositoryUrl(IntersmashConfig.deploymentsRepositoryUrl())
				.withSourceRepositoryRef(IntersmashConfig.deploymentsRepositoryRef())
				.withJdk17BuilderImage(IntersmashConfig.wildflyImageURL())
				.withJdk17RuntimeImage(IntersmashConfig.wildflyRuntimeImageURL())
				.withBuildEnvironmentVariables(buildEnvironmentVariables)
				.withDeploymentEnvironmentVariables(deploymentEnvironmentVariables)
				.withVolume(
						new VolumeBuilder()
								.withName("client-amq-streams-cluster-ca-cert-secret")
								.withSecret(
										new SecretVolumeSourceBuilder()
												.withSecretName("client-amq-streams-cluster-ca-cert-secret")
												.build())
								.build())
				.withVolumeMount(
						new VolumeMountBuilder()
								.withName("client-amq-streams-cluster-ca-cert-secret")
								.withMountPath(SECRETS_VOLUME_MOUNT_PATH)
								.withReadOnly(true)
								.build());
		List<String> channelDefinition = Arrays.asList(this.eeChannelGroupId(), this.eeChannelArtifactId(),
				this.eeChannelVersion());
		if (!channelDefinition.isEmpty()) {
			release.withS2iChannel(channelDefinition.stream().collect(Collectors.joining(":")));
		}

		return release;
	}

	@Override
	public String getBuilderImage() {
		return IntersmashConfig.wildflyImageURL();
	}

	@Override
	public String getRuntimeImage() {
		return IntersmashConfig.wildflyRuntimeImageURL();
	}

	@Override
	public HelmChartRelease getRelease() {
		return release;
	}

	@Override
	public String getHelmChartsRepositoryUrl() {
		return IntersmashConfig.getWildflyHelmChartsRepo();
	}

	@Override
	public String getHelmChartsRepositoryRef() {
		return IntersmashConfig.getWildflyHelmChartsBranch();
	}

	@Override
	public String getHelmChartsRepositoryName() {
		return IntersmashConfig.getWildflyHelmChartsName();
	}

	@Override
	public List<Secret> getSecrets() {
		return Collections.unmodifiableList(secrets);
	}

	@Override
	public String getName() {
		return APP_NAME;
	}

	public static String getRoute() {
		return OpenShifts.master().generateHostname(APP_NAME);
	}
}
