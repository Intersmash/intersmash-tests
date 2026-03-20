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
package org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.connector;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
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
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.openshift.helm.HelmChartRelease;
import org.jboss.intersmash.application.openshift.helm.WildflyHelmChartOpenShiftApplication;
import org.jboss.intersmash.provision.helm.wildfly.WildflyHelmChartRelease;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;
import org.jboss.intersmash.tests.wildfly.util.WildFlyHelmChartsConfiguration;

/**
 * WildFly Bootable JAR application configured with the MicroProfile Reactive Messaging AMQP connector
 * to connect to an external ActiveMQ Artemis broker with SSL.
 * <p>
 * This application is deployed as a Bootable JAR using WildFly Helm charts.
 * </p>
 */
@Slf4j
public class WildflyAmqpConnectorApplication
		implements WildflyHelmChartOpenShiftApplication, WildflyApplicationConfiguration, WildFlyHelmChartsConfiguration {

	static final String NAME = "wildfly-amqp-connector";
	private static final String APP_MODULE_DIRECTORY = "wildfly/activemq-artemis-connector";
	private static final String WILDFLY_CLIENT_SECRET_NAME = String.format("%s-ssl-secret", NAME);
	private static final String SECRETS_VOLUME_MOUNT_PATH = "/etc/secrets";
	private static final String KEYSTORE_FILE_NAME = "broker.ks";
	private static final String TRUSTSTORE_FILE_NAME = "client.ts";

	private final List<Secret> secrets = new ArrayList<>();
	private final HelmChartRelease release;

	public WildflyAmqpConnectorApplication() throws IOException {
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo certificateInfo = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						ActiveMQArtemisAmqpApplication.getAcceptorServiceName(),
						ActiveMQArtemisAmqpApplication.KEYALIAS,
						ActiveMQArtemisAmqpApplication.STOREPASS, null,
						Arrays.asList(ActiveMQArtemisAmqpApplication.getWildcardSAN()));

		Secret clientSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(WILDFLY_CLIENT_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", NAME))
				.endMetadata()
				.addToData(Map.of(KEYSTORE_FILE_NAME,
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(certificateInfo.keystore.toFile()))))
				.addToData(Map.of(TRUSTSTORE_FILE_NAME,
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(certificateInfo.truststore.toFile()))))
				.build();
		secrets.add(clientSecret);

		release = loadRelease(getHelmChartRelease());
	}

	private HelmChartRelease loadRelease(final WildflyHelmChartRelease release) throws IOException {
		// let's compute some additional maven args for our s2i build to happen on a Pod
		String mavenAdditionalArgs = "-Denforcer.skip=true";
		// let's add configurable deployment additional args:
		mavenAdditionalArgs = mavenAdditionalArgs.concat(generateAdditionalMavenArgs());
		// to speed up the build process we target a specific module; we also skip generating the "*-sources.jar" to disambiguate the S2I artifact to deploy
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

		// AMQP broker connection
		deploymentEnvironmentVariables.put("REMOTE_AMQP_BROKER", ActiveMQArtemisAmqpApplication.getRoute());
		deploymentEnvironmentVariables.put("AMQP_USER", ActiveMQArtemisAmqpApplication.ADMIN_USER);
		deploymentEnvironmentVariables.put("AMQP_PASSWORD", ActiveMQArtemisAmqpApplication.ADMIN_PASSWORD);

		// Keystore and TrustStore
		deploymentEnvironmentVariables.put("KEY_STORE_PATH",
				String.format("%s/%s", SECRETS_VOLUME_MOUNT_PATH, KEYSTORE_FILE_NAME));
		deploymentEnvironmentVariables.put("KEY_STORE_PASSWORD", ActiveMQArtemisAmqpApplication.STOREPASS);
		deploymentEnvironmentVariables.put("KEY_STORE_TYPE", SimpleCommandLineBasedKeystoreGenerator.STORE_TYPE);
		deploymentEnvironmentVariables.put("TRUST_STORE_PATH",
				String.format("%s/%s", SECRETS_VOLUME_MOUNT_PATH, TRUSTSTORE_FILE_NAME));
		deploymentEnvironmentVariables.put("TRUST_STORE_PASSWORD", ActiveMQArtemisAmqpApplication.STOREPASS);
		deploymentEnvironmentVariables.put("TRUST_STORE_TYPE", SimpleCommandLineBasedKeystoreGenerator.STORE_TYPE);

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
								.withName(WILDFLY_CLIENT_SECRET_NAME)
								.withSecret(
										new SecretVolumeSourceBuilder()
												.withSecretName(WILDFLY_CLIENT_SECRET_NAME)
												.build())
								.build())
				.withVolumeMount(
						new VolumeMountBuilder()
								.withName(WILDFLY_CLIENT_SECRET_NAME)
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
		return NAME;
	}
}
