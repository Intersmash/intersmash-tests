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
package org.jboss.intersmash.tests.wildfly.microprofile.reactive.messaging.kafka.sasl;

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;

/**
 * WildFly/JBoss EAP application descriptor configured to use SASL_PLAINTEXT for authenticating to a Kafka/Streams
 * for Apache Kafka instance with SCRAM-SHA-512 authentication.
 * <p>
 * Unlike the SSL-based variant ({@link WildflyMicroProfileReactiveMessagingSaslSslApplication}),
 * this application connects to the Kafka plaintext listener (unencrypted) and authenticates via SASL.
 * No keystore or Elytron SSL context configuration is required.
 * </p>
 * <p>
 * The following MicroProfile Reactive Messaging properties are configured via environment variables:
 * <ul>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS} — Kafka bootstrap service (plaintext port)</li>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SECURITY_PROTOCOL} — set to {@code SASL_PLAINTEXT}</li>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SASL_MECHANISM} — set to {@code SCRAM-SHA-512}</li>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SASL_JAAS_CONFIG} — JAAS configuration from KafkaUser secret</li>
 * </ul>
 * </p>
 */
public class WildflyMicroProfileReactiveMessagingSaslPlaintextApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	private final BuildInput buildInput;
	public static final String APP_NAME = "mp-rm-sasl-plaintext";
	private final List<EnvVar> environmentVariables = new ArrayList<>();

	public WildflyMicroProfileReactiveMessagingSaslPlaintextApplication() {
		String applicationDir = "wildfly/kafka-sasl-application";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		// Bootstrap servers: connect to the plaintext (unencrypted) Kafka listener
		// Overrides 'mp.messaging.connector.smallrye-kafka.bootstrap.servers=localhost:9092' in microprofile-config.properties
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS")
						.withValue(SecuredKafkaMicroProfileReactiveMessagingApplication.APP_NAME
								+ "-kafka-bootstrap:"
								+ SecuredKafkaMicroProfileReactiveMessagingApplication.KAFKA_PLAINTEXT_PORT)
						.build());

		// Security protocol: SASL_PLAINTEXT (SASL authentication over an unencrypted connection)
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SECURITY_PROTOCOL")
						.withValue("SASL_PLAINTEXT")
						.build());

		// SASL mechanism: SCRAM-SHA-512
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SASL_MECHANISM")
						.withValue("SCRAM-SHA-512")
						.build());

		// JAAS configuration: read from the KafkaUser credentials secret
		final Secret kafkaUserSecret = OpenShifts.master().getSecret(
				SecuredKafkaMicroProfileReactiveMessagingApplication.KAFKA_USER_CREDENTIALS_SECRET);
		if (kafkaUserSecret == null) {
			throw new IllegalStateException(String.format("Couldn't retrieve the %s secret.",
					SecuredKafkaMicroProfileReactiveMessagingApplication.KAFKA_USER_CREDENTIALS_SECRET));
		}
		final String jaasConfigurationString = new String(
				Base64.getDecoder().decode(kafkaUserSecret.getData().get(
						SecuredKafkaMicroProfileReactiveMessagingApplication.KAFKA_USER_SECRET_SASL_JAAS_CONFIG_DATA_KEY)));
		if (Strings.isNullOrEmpty(jaasConfigurationString)) {
			throw new IllegalStateException(
					String.format("Couldn't retrieve a valid JAAS configuration string from %s secret.",
							SecuredKafkaMicroProfileReactiveMessagingApplication.KAFKA_USER_CREDENTIALS_SECRET));
		}
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SASL_JAAS_CONFIG")
						.withValue(jaasConfigurationString)
						.build());

		// S2I build configuration
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
}
