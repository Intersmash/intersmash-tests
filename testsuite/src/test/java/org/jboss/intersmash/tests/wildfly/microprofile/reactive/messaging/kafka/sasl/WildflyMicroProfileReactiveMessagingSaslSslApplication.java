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
import org.jboss.intersmash.tools.client.OpenShifts;

/**
 * WildFly/JBoss EAP application descriptor configured to use SASL_SSL for authenticating to a Kafka/Streams
 * for Apache Kafka instance with SCRAM-SHA-512 authentication over a TLS secured connection.
 * <p>
 * Unlike the plaintext variant ({@link WildflyMicroProfileReactiveMessagingSaslPlaintextApplication}),
 * this application connects to the Kafka SSL listener (encrypted) and authenticates via SASL over TLS.
 * </p>
 * <p>
 * The following MicroProfile Reactive Messaging properties are configured via environment variables:
 * <ul>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS} — Kafka bootstrap service (SSL port)</li>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SECURITY_PROTOCOL} — set to {@code SASL_SSL}</li>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SASL_MECHANISM} — set to {@code SCRAM-SHA-512}</li>
 *     <li>{@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SASL_JAAS_CONFIG} — JAAS configuration from KafkaUser secret</li>
 * </ul>
 * </p>
 * <p>
 *     Additionally the class creates a {@link Secret} containing the truststore to be used for the mutual authentication
 *     required bty the secured connection, and sets the {@code KEYSTORE_PATH} and {@code KEYSTORE_PASSWORD}
 *     environment variables to let the application use it.
 *     <br>
 *     Finally, the {@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_WILDFLY_ELYTRON_SSL_CONTEXT} MicroProfile Reactive
 *     Messaging configuration property is set with the name of the client SSL context that defines the Elytron
 *     subsystem truststore configuration.
 * </p>
 */
public class WildflyMicroProfileReactiveMessagingSaslSslApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	private final BuildInput buildInput;
	public static final String APP_NAME = "mp-rm-sasl-ssl";
	private final List<EnvVar> environmentVariables = new ArrayList<>();
	// Client SSL context name
	static final String CLIENT_SSL_CONTEXT_NAME = "kafka-ssl-test";
	private final Secret clientSecret;
	private final String CERTIFICATE_SECRET_PATH = "/etc/secrets/ca.p12";
	private final String password;

	public WildflyMicroProfileReactiveMessagingSaslSslApplication() {
		String applicationDir = "wildfly/kafka-sasl-application";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		clientSecret = OpenShifts.master().getSecret("amq-streams-cluster-ca-cert");
		clientSecret.getMetadata().setName("client-amq-streams-cluster-ca-cert-secret");
		clientSecret.getMetadata().setResourceVersion(null);
		password = new String(Base64.getDecoder().decode(clientSecret.getData().get("ca.password")));

		// Bootstrap servers: connect to the secured (encrypted) Kafka listener
		// Overrides 'mp.messaging.connector.smallrye-kafka.bootstrap.servers=localhost:9092' in microprofile-config.properties
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS")
						.withValue(SecuredKafkaMicroProfileReactiveMessagingApplication.APP_NAME
								+ "-kafka-bootstrap:"
								+ SecuredKafkaMicroProfileReactiveMessagingApplication.KAFKA_SSL_PORT)
						.build());

		environmentVariables.add(
				new EnvVarBuilder().withName("KEYSTORE_PATH")
						.withValue(CERTIFICATE_SECRET_PATH)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("KEYSTORE_PASSWORD")
						.withValue(password)
						.build());

		// Security protocol: SASL_SSL (SASL authentication over an encrypted connection)
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SECURITY_PROTOCOL")
						.withValue("SASL_SSL")
						.build());

		// SASL mechanism: SCRAM-SHA-512
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_SASL_MECHANISM")
						.withValue("SCRAM-SHA-512")
						.build());
		// Testing application don't have client-ssl-context configured in the microprofile-config.properties file so
		// that it can be deployed on the server without it (server is configured with relevant client-ssl-context after
		// the application is deployed). As such, let's configure its value _globally_ here via environment properties.
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_WILDFLY_ELYTRON_SSL_CONTEXT")
						.withValue(
								CLIENT_SSL_CONTEXT_NAME)
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

	@Override
	public List<Secret> getSecrets() {
		return List.of(clientSecret);
	}
}
