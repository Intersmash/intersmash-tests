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

import static io.restassured.RestAssured.get;

import cz.xtf.junit5.annotations.OpenShiftRecorder;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.tests.junit.annotations.ActiveMQArtemisTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration test for WildFly Bootable JAR with MicroProfile Reactive Messaging AMQP connector
 * connecting to an external ActiveMQ Artemis broker over SSL.
 * <p>
 * The application sends messages to an AMQ topic and consumes from another topic,
 * verifying that the last consumed value reaches {@code 64}.
 * </p>
 */
@WildflyTest
@EapXpTest
@ActiveMQArtemisTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(ActiveMQArtemisAmqpApplication.class),
		@Service(WildflyAmqpConnectorApplication.class)
})
@OpenShiftRecorder(resourceNames = { ActiveMQArtemisAmqpApplication.NAME, WildflyAmqpConnectorApplication.NAME })
public class WildflyAmqpConnectorIT {

	@ServiceUrl(WildflyAmqpConnectorApplication.class)
	private String wildflyRouteUrl;

	@Test
	public void testAmqpReactiveMessaging() {
		RestAssured.useRelaxedHTTPSValidation();
		Awaitility.await("MP Reactive Messaging with AMQP connector did not process messages")
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> get(wildflyRouteUrl.replace("http:/", "https:/") + "/last")
						.then()
						.log()
						.ifValidationFails(LogDetail.ALL, true)
						.assertThat()
						.body(Matchers.containsString("64")));
	}
}
