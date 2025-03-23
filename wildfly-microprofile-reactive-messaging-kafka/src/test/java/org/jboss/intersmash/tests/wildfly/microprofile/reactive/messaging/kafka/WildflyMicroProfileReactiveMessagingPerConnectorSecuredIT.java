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

import cz.xtf.junit5.listeners.ProjectCreator;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.openshift.OpenShiftProvisioner;
import org.jboss.intersmash.provision.openshift.WildflyImageOpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.EapXp6Test;
import org.jboss.intersmash.tests.junit.annotations.KafkaTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * AMQ Streams interoperability tests.
 *
 * Verify the interoperability between EAP XP 5 and AMQ Streams on OpenShift cluster.
 * <br>
 * This test application is built via EAP s2i features, but it <i>cannot</i> be executed against EAP 8, since it does not contain
 * MicroProfile specs, including Reactive Messaging.
 * <br>
 * AMQ Streams operator is utilized to provide AMQ Streams (Kafka) instance. EAP s2i application is created with
 * MicroProfile Reactive Messaging implementation deployed, based on the EAP feature pack.
 * Some connections are performed via SSL protocol and Elytron based SSLContext configuration.
 * <br>
 * This application sends messages to an AMQ Streams topic and at the same time listens to another topic from AMQ Streams
 * instance to read data from there. Connections are performed both as not secured (plaintext) and secured via SSL with SSLContext too.
 * Actual test implementations are placed in {@link WildflyMicroProfileReactiveMessagingTestsCommon}
 */
@KafkaTest
@WildflyTest
@EapXp6Test
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Intersmash({
		@Service(KafkaMicroProfileReactiveMessagingApplication.class),
		@Service(WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication.class)
})
public class WildflyMicroProfileReactiveMessagingPerConnectorSecuredIT
		extends WildflyMicroProfileReactiveMessagingTestsCommon {
	@ServiceUrl(WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication.class)
	private String eapRouteUrl;

	@ServiceProvisioner(KafkaMicroProfileReactiveMessagingApplication.class)
	private OpenShiftProvisioner<KafkaMicroProfileReactiveMessagingApplication> amqStreamsOpenShiftProvisioner;

	@ServiceProvisioner(WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication.class)
	private WildflyImageOpenShiftProvisioner eapOpenShiftProvisioner;

	@Override
	protected String getEapRouteUrl() {
		return eapRouteUrl;
	}
}
