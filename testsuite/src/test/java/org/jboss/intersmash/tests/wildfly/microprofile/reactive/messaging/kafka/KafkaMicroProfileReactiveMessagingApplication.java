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
package org.jboss.intersmash.tests.wildfly.microprofile.reactive.messaging.kafka;

import io.strimzi.api.kafka.model.common.CertificateAuthority;
import io.strimzi.api.kafka.model.common.CertificateAuthorityBuilder;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.user.KafkaUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.KafkaOperatorApplication;

/**
 * Provides a Kafka/Streams for Apache Kafka service which is required by
 * {@link WildflyBootableJarGloballySecuredKafkaIT},
 * {@link WildflyS2iPerConnectorSecuredKafkaIT},
 * and deployed via the Strimzi/Streams for Apache Kafka operator.
 */
public class KafkaMicroProfileReactiveMessagingApplication implements KafkaOperatorApplication, OpenShiftApplication {
	public static final String APP_NAME = "amq-streams";

	private static final String KAFKA_VERSION = "4.2.0";
	private static final int KAFKA_INSTANCE_NUM = 3;
	private static final int TOPIC_RECONCILIATION_INTERVAL_SECONDS = 90;
	private static final long USER_RECONCILIATION_INTERVAL_SECONDS = 120L;

	public static final int KAFKA_PLAINTEXT_PORT = 9092;
	public static final int KAFKA_SSL_PORT = 9093;

	private final Kafka kafka;
	private final List<KafkaNodePool> kafkaNodePools = new ArrayList<>();

	public KafkaMicroProfileReactiveMessagingApplication() {
		// Create a KafkaNodePool for KRaft mode (required for Kafka 4.x)
		// This node pool combines broker and controller roles
		kafkaNodePools.add(new KafkaNodePoolBuilder()
				.withNewMetadata()
				.withName("kafka-pool")
				.withLabels(Map.of("strimzi.io/cluster", APP_NAME))
				.endMetadata()
				.withNewSpec()
				.withReplicas(KAFKA_INSTANCE_NUM)
				.withRoles(ProcessRoles.BROKER, ProcessRoles.CONTROLLER)
				.withNewEphemeralStorage()
				.endEphemeralStorage()
				.endSpec()
				.build());

		Map<String, Object> config = new HashMap<>();
		config.put("offsets.topic.replication.factor", KAFKA_INSTANCE_NUM);
		config.put("transaction.state.log.min.isr", KAFKA_INSTANCE_NUM);
		config.put("transaction.state.log.replication.factor", KAFKA_INSTANCE_NUM);

		config.put("default.replication.factor", KAFKA_INSTANCE_NUM);
		config.put("min.insync.replicas", 2);

		// We need this configuration due to the tests that sets custom partition number for the message in topic.
		config.put("num.partitions", 2);

		config.put("advertised.listeners",
				"SSL://localhost:" + KAFKA_SSL_PORT + ",PLAINTEXT://localhost:" + KAFKA_PLAINTEXT_PORT);
		config.put("security.inter.broker.protocol", "PLAINTEXT");

		List<GenericKafkaListener> listeners = new ArrayList<>();

		GenericKafkaListener listenerSsl = new GenericKafkaListener();
		listenerSsl.setName("ssl");
		listenerSsl.setPort(KAFKA_SSL_PORT);
		listenerSsl.setType(KafkaListenerType.INTERNAL);
		listenerSsl.setTls(true);

		GenericKafkaListener listenerPlaintext = new GenericKafkaListener();
		listenerPlaintext.setName("plaintext");
		listenerPlaintext.setPort(KAFKA_PLAINTEXT_PORT);
		listenerPlaintext.setType(KafkaListenerType.INTERNAL);
		listenerPlaintext.setTls(false);

		listeners.add(listenerSsl);
		listeners.add(listenerPlaintext);

		CertificateAuthority ca = new CertificateAuthorityBuilder().build();

		// Initialize AMQ Streams Kafka resource (KRaft mode - ZooKeeper removed in Kafka 4.x)
		kafka = new KafkaBuilder()
				.withNewMetadata().withName(APP_NAME).endMetadata()
				.withNewSpec()
				.withNewEntityOperator()
				.withNewTopicOperator().withReconciliationIntervalSeconds(TOPIC_RECONCILIATION_INTERVAL_SECONDS)
				.endTopicOperator()
				.withNewUserOperator().withReconciliationIntervalSeconds(USER_RECONCILIATION_INTERVAL_SECONDS).endUserOperator()
				.endEntityOperator()
				.withNewKafka()
				.withConfig(config)
				.withListeners(listeners)
				.withReplicas(KAFKA_INSTANCE_NUM)
				.withNewEphemeralStorage().endEphemeralStorage()
				.withVersion(KAFKA_VERSION)
				.endKafka()
				.withClusterCa(ca)
				.endSpec()
				.build();
	}

	@Override
	public Kafka getKafka() {
		return kafka;
	}

	@Override
	public List<KafkaTopic> getTopics() {
		// We don't need any special topics for our tests.
		// Topic is created during the test internally on the Kafka cluster automatically.
		return null;
	}

	@Override
	public List<KafkaUser> getUsers() {
		// We don't need any user for our tests.
		return null;
	}

	@Override
	public String getName() {
		return APP_NAME;
	}

	@Override
	public List<KafkaNodePool> getNodePools() {
		return kafkaNodePools;
	}
}
