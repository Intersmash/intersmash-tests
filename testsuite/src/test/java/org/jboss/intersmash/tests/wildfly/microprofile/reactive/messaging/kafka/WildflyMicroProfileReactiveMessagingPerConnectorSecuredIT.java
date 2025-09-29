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

import cz.xtf.junit5.extensions.ServiceLogsStreamingRunner;
import cz.xtf.junit5.listeners.ProjectCreator;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.openshift.OpenShiftProvisioner;
import org.jboss.intersmash.provision.openshift.WildflyImageOpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.KafkaTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly/JBoss EAP XP + Kafka/Streams for Apache Kafka interoperability tests.
 *
 * Verify the interoperability between JBoss EAP XP and Kafka/Streams for Apache Kafka on OpenShift.
 * <br>
 * This test application is built via the WildFly/JBoss EAP s2i features, but it <i>cannot</i> be executed against
 * JBoss EAP 8.z, since it does not contain MicroProfile specs, including Reactive Messaging.
 * <br>
 * The Strimzi/Streams for Apache Kafka operator is used to provide a Kafka/Streams for Apache Kafka instance.
 * The WildFly/JBoss EAP XP application includes the MicroProfile Reactive Messaging Galleon feature pack.
 * <br>
 * This application sends messages to a Kafka/Streams for Apache Kafka service and, at the same time, listens to
 * different topic in order to read data.
 * Connections are performed both as not secured (plaintext) and secured via SSL with SSLContext too, leveraging
 * Elytron based SSLContext configuration.
 * Actual test implementations are placed in {@link WildflyMicroProfileReactiveMessagingTestsCommon}
 */
@KafkaTest
@WildflyTest
@EapXpTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Intersmash({
		@Service(KafkaMicroProfileReactiveMessagingApplication.class),
		@Service(WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication.class)
})
@ExtendWith(ServiceLogsStreamingRunner.class)
public class WildflyMicroProfileReactiveMessagingPerConnectorSecuredIT
		extends WildflyMicroProfileReactiveMessagingTestsCommon {
	@ServiceUrl(WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication.class)
	private String applicationRouteUrl;

	@ServiceProvisioner(KafkaMicroProfileReactiveMessagingApplication.class)
	private OpenShiftProvisioner<KafkaMicroProfileReactiveMessagingApplication> amqStreamsOpenShiftProvisioner;

	@ServiceProvisioner(WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication.class)
	private WildflyImageOpenShiftProvisioner eapOpenShiftProvisioner;

	@Override
	protected String getApplicationRouteUrl() {
		return applicationRouteUrl;
	}
}
