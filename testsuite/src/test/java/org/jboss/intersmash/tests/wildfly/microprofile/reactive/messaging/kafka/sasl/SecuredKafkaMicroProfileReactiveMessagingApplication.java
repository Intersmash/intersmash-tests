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

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.strimzi.api.kafka.model.common.CertificateAuthority;
import io.strimzi.api.kafka.model.common.CertificateAuthorityBuilder;
import io.strimzi.api.kafka.model.common.PasswordSourceBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512Builder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthenticationBuilder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.KafkaOperatorApplication;

/**
 * Provides a Kafka/Streams for Apache Kafka service with SASL SCRAM-SHA-512 authentication on all listeners.
 * <p>
 * Two listeners are configured: a plaintext listener (port 9092) and an SSL listener (port 9093), both requiring
 * SCRAM-SHA-512 client authentication. A {@link KafkaUser} is created with credentials stored in a Kubernetes
 * {@link Secret}, which can be used by client applications to authenticate.
 * </p>
 */
public class SecuredKafkaMicroProfileReactiveMessagingApplication implements KafkaOperatorApplication, OpenShiftApplication {
	public static final String APP_NAME = "amq-streams";

	private static final String KAFKA_VERSION = KafkaOperatorApplication.KAFKA_VERSION;
	private static final String KAFKA_METADATA_VERSION = KafkaOperatorApplication.METADATA_VERSION;
	private static final int KAFKA_INSTANCE_NUM = KafkaOperatorApplication.KAFKA_INSTANCE_NUM;
	private static final int TOPIC_RECONCILIATION_INTERVAL_SECONDS = KafkaOperatorApplication.TOPIC_RECONCILIATION_INTERVAL_SECONDS;
	private static final long USER_RECONCILIATION_INTERVAL_SECONDS = KafkaOperatorApplication.USER_RECONCILIATION_INTERVAL_SECONDS;

	public static final int KAFKA_PLAINTEXT_PORT = 9092;
	public static final int KAFKA_SSL_PORT = 9093;

	public static final String KAFKA_USER_CREDENTIALS_SECRET = "kafka-user-secret";
	public static final String KAFKA_USER_SECRET_PASSWORD_DATA_KEY = "kafka-user.password";
	public static final String KAFKA_USER_SECRET_SASL_JAAS_CONFIG_DATA_KEY = "kafka-user.sasl.jaas.config";
	public static final String KAFKA_USER_USERNAME = "kafka-user";
	public static final String KAFKA_USER_PASSWORD = "s3cret-p4ssw0rd!";

	private final Kafka kafka;
	private final List<KafkaUser> kafkaUsers = new ArrayList<>();
	private final List<Secret> kafkaSecrets = new ArrayList<>();
	private final List<KafkaTopic> kafkaTopics = new ArrayList<>();

	public SecuredKafkaMicroProfileReactiveMessagingApplication() {
		Map<String, Object> config = new HashMap<>();
		config.put("inter.broker.protocol.version", KAFKA_METADATA_VERSION);
		config.put("offsets.topic.replication.factor", KAFKA_INSTANCE_NUM);
		config.put("transaction.state.log.min.isr", KAFKA_INSTANCE_NUM);
		config.put("transaction.state.log.replication.factor", KAFKA_INSTANCE_NUM);
		config.put("default.replication.factor", KAFKA_INSTANCE_NUM);
		config.put("min.insync.replicas", 2);
		config.put("num.partitions", 2);

		// Create a secret holding credentials for the Kafka user
		final String saslClientJaasConfigString = "org.apache.kafka.common.security.scram.ScramLoginModule required"
				+ String.format(" username=\"%s\"", KAFKA_USER_USERNAME)
				+ String.format(" password=\"%s\";", KAFKA_USER_PASSWORD);
		kafkaSecrets.add(new SecretBuilder()
				.withNewMetadata()
				.withName(KAFKA_USER_CREDENTIALS_SECRET)
				.endMetadata()
				.withData(Map.of(
						KAFKA_USER_SECRET_PASSWORD_DATA_KEY,
						Base64.getEncoder().encodeToString(KAFKA_USER_PASSWORD.getBytes(StandardCharsets.UTF_8)),
						KAFKA_USER_SECRET_SASL_JAAS_CONFIG_DATA_KEY,
						Base64.getEncoder().encodeToString(saslClientJaasConfigString.getBytes(StandardCharsets.UTF_8))))
				.build());

		// Create a KafkaUser resource with SCRAM-SHA-512 authentication
		kafkaUsers.add(new KafkaUserBuilder()
				.withNewMetadata()
				.withName(KAFKA_USER_USERNAME)
				.withLabels(Map.of("strimzi.io/cluster", APP_NAME))
				.endMetadata()
				.withNewSpec()
				.withAuthentication(new KafkaUserScramSha512ClientAuthenticationBuilder()
						.withNewPassword().withValueFrom(
								new PasswordSourceBuilder().withSecretKeyRef(
										new SecretKeySelectorBuilder()
												.withName(KAFKA_USER_CREDENTIALS_SECRET)
												.withKey(KAFKA_USER_SECRET_PASSWORD_DATA_KEY)
												.build())
										.build())
						.endPassword()
						.build())
				.endSpec()
				.build());

		// Create listeners with SCRAM-SHA-512 authentication
		List<GenericKafkaListener> listeners = new ArrayList<>();

		GenericKafkaListener listenerSsl = new GenericKafkaListener();
		listenerSsl.setName("ssl");
		listenerSsl.setPort(KAFKA_SSL_PORT);
		listenerSsl.setType(KafkaListenerType.INTERNAL);
		listenerSsl.setTls(true);
		listenerSsl.setAuth(new KafkaListenerAuthenticationScramSha512Builder().build());

		GenericKafkaListener listenerPlaintext = new GenericKafkaListener();
		listenerPlaintext.setName("plaintext");
		listenerPlaintext.setPort(KAFKA_PLAINTEXT_PORT);
		listenerPlaintext.setType(KafkaListenerType.INTERNAL);
		listenerPlaintext.setTls(false);
		listenerPlaintext.setAuth(new KafkaListenerAuthenticationScramSha512Builder().build());

		listeners.add(listenerSsl);
		listeners.add(listenerPlaintext);

		CertificateAuthority ca = new CertificateAuthorityBuilder().build();

		kafka = new KafkaBuilder()
				.withNewMetadata().withName(APP_NAME).endMetadata()
				.withNewSpec()
				.withNewEntityOperator()
				.withNewTopicOperator().withReconciliationIntervalSeconds(TOPIC_RECONCILIATION_INTERVAL_SECONDS)
				.endTopicOperator()
				.withNewUserOperator().withReconciliationIntervalSeconds(USER_RECONCILIATION_INTERVAL_SECONDS)
				.endUserOperator()
				.endEntityOperator()
				.withNewKafka()
				.withConfig(config)
				.withListeners(listeners)
				.withReplicas(KAFKA_INSTANCE_NUM)
				.withNewEphemeralStorage().endEphemeralStorage()
				.withVersion(KAFKA_VERSION)
				.endKafka()
				.withNewZookeeper()
				.withReplicas(KAFKA_INSTANCE_NUM)
				.withNewEphemeralStorage().endEphemeralStorage()
				.endZookeeper()
				.withClusterCa(ca)
				.endSpec()
				.build();

		// Create topic explicitly since auto-creation may not work with authentication enabled
		kafkaTopics.add(new KafkaTopicBuilder()
				.withNewMetadata()
				.withLabels(Map.of("strimzi.io/cluster", APP_NAME))
				.withName("testing")
				.endMetadata()
				.withNewSpec()
				.withReplicas(1)
				.withPartitions(3)
				.endSpec()
				.build());
	}

	@Override
	public Kafka getKafka() {
		return kafka;
	}

	@Override
	public List<KafkaTopic> getTopics() {
		return kafkaTopics;
	}

	@Override
	public List<KafkaUser> getUsers() {
		return kafkaUsers;
	}

	@Override
	public String getName() {
		return APP_NAME;
	}

	@Override
	public List<Secret> getSecrets() {
		return kafkaSecrets;
	}
}
