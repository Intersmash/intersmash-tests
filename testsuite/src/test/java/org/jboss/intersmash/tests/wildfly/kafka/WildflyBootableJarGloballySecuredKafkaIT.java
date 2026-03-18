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
package org.jboss.intersmash.tests.wildfly.kafka;

import cz.xtf.junit5.annotations.OpenShiftRecorder;
import cz.xtf.junit5.extensions.ServiceLogsStreamingRunner;
import cz.xtf.junit5.listeners.ProjectCreator;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.helm.wildfly.WildflyHelmChartOpenShiftProvisioner;
import org.jboss.intersmash.provision.openshift.OpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.KafkaTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.jboss.intersmash.tests.wildfly.microprofile.reactive.messaging.kafka.KafkaMicroProfileReactiveMessagingApplication;
import org.jboss.intersmash.tests.wildfly.microprofile.reactive.messaging.kafka.WildflyMicroProfileReactiveMessagingTestsCommon;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly/JBoss EAP XP + Kafka/Streams for Apache Kafka interoperability tests using a bootable JAR.
 *
 * Verify the interoperability between JBoss EAP XP and Kafka/Streams for Apache Kafka on OpenShift.
 * <br>
 * The Strimzi/Streams for Apache Kafka operator is used to provide a Kafka/Streams for Apache Kafka instance.
 * The WildFly/JBoss EAP XP application is built as a bootable JAR with the MicroProfile Reactive Messaging
 * Galleon feature pack.
 * <br>
 * This application sends messages to a Kafka/Streams for Apache Kafka service and, at the same time, listens to
 * different topic in order to read data.
 * Connections are performed both as not secured (plaintext) and secured via SSL with SSLContext too, leveraging
 * Elytron based SSLContext configuration.
 * <br>
 * In this use case, the Elytron SSL context name is configured <i>globally</i> via the
 * {@code MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_WILDFLY_ELYTRON_SSL_CONTEXT} environment variable, as opposed to
 * the per-connector configuration used by
 * {@link org.jboss.intersmash.tests.wildfly.microprofile.reactive.messaging.kafka.WildflyMicroProfileReactiveMessagingPerConnectorSecuredIT}.
 * <br>
 * Actual test implementations are placed in {@link WildflyMicroProfileReactiveMessagingTestsCommon}
 */
@KafkaTest
@WildflyTest
@EapXpTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Intersmash({
		@Service(KafkaMicroProfileReactiveMessagingApplication.class),
		@Service(WildflyBootableJarGloballySecuredKafkaHelmApplication.class)
})
@OpenShiftRecorder(resourceNames = { KafkaMicroProfileReactiveMessagingApplication.APP_NAME,
		WildflyBootableJarGloballySecuredKafkaHelmApplication.APP_NAME })
@ExtendWith(ServiceLogsStreamingRunner.class)
public class WildflyBootableJarGloballySecuredKafkaIT extends WildflyMicroProfileReactiveMessagingTestsCommon {
	@ServiceUrl(WildflyBootableJarGloballySecuredKafkaHelmApplication.class)
	private String applicationRouteUrl;

	@ServiceProvisioner(KafkaMicroProfileReactiveMessagingApplication.class)
	private OpenShiftProvisioner<KafkaMicroProfileReactiveMessagingApplication> amqStreamsOpenShiftProvisioner;

	@ServiceProvisioner(WildflyBootableJarGloballySecuredKafkaHelmApplication.class)
	private WildflyHelmChartOpenShiftProvisioner wildflyHelmChartOpenShiftProvisioner;

	@Override
	protected String getApplicationRouteUrl() {
		return applicationRouteUrl;
	}
}
