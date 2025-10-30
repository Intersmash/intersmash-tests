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
package org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.ssl;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import cz.xtf.core.openshift.OpenShiftWaiters;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.restassured.filter.log.LogDetail;
import lombok.extern.slf4j.Slf4j;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.openshift.OpenShiftProvisioner;
import org.jboss.intersmash.provision.openshift.WildflyImageOpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.ActiveMQArtemisTest;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.ssl.util.JmsTestConstants;
import org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.ssl.util.JmsTestRequestType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration test for WildFly/EAP application communicating with an external ActiveMQ Artemis broker over SSL.
 * <p>
 * This test class verifies secure JMS messaging functionality between a WildFly/EAP application and an
 * external ActiveMQ Artemis message broker using SSL/TLS connections. It tests various messaging scenarios
 * including queue operations, message-driven beans (MDB), and XA transaction recovery after server failures.
 * </p>
 * <p>
 * The test deploys two services:
 * <ul>
 *   <li>{@link ActiveMQArtemisApplication} - ActiveMQ Artemis broker configured with SSL</li>
 *   <li>{@link WildflyJmsSslApplication} - WildFly/EAP application with JMS client configured for SSL</li>
 * </ul>
 * </p>
 */
@WildflyTest
@EapTest
@EapXpTest
@ActiveMQArtemisTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(ActiveMQArtemisApplication.class),
		@Service(WildflyJmsSslApplication.class)
})
public class WildflyActiveMQArtemisSslIT {

	/**
	 * The URL of the WildFly/EAP application route for accessing the JMS test servlet.
	 */
	@ServiceUrl(WildflyJmsSslApplication.class)
	private String wildFlyRouteUrl;

	/**
	 * Provisioner for the ActiveMQ Artemis broker application.
	 */
	@ServiceProvisioner(ActiveMQArtemisApplication.class)
	private OpenShiftProvisioner<ActiveMQArtemisApplication> amqOpenShiftProvisioner;

	/**
	 * Provisioner for the WildFly/EAP JMS client application.
	 */
	@ServiceProvisioner(WildflyJmsSslApplication.class)
	private WildflyImageOpenShiftProvisioner wildflyOpenShiftProvisioner;

	/**
	 * Send/receive a message to/from remote ActiveMQ Artemis broker via test servlet using ActiveMQQueue.<br>
	 * Verify that response reply message contains strings indicating that it has been processed by the ActiveMQQueue.
	 */
	@Test
	public void testSendReceiveMessageQueue() {
		// produce message - a name of queue would be 'null' in case that WildFly/EAP fails to create a connection
		get(wildFlyRouteUrl + "/jms-test?request=" + JmsTestRequestType.REQUEST_SEND.value())
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(containsString(JmsTestConstants.QUEUE_SEND_RESPONSE));
		get(wildFlyRouteUrl + "/jms-test?request=" + JmsTestRequestType.REQUEST_CONSUME_MESSAGE.value())
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(containsString(JmsTestConstants.QUEUE_TEXT_MESSAGE));
	}

	/**
	 * Send a message to remote ActiveMQ Artemis broker via test servlet using ActiveMQQueue. Consume the message by
	 * <i>JmsTestQueueMDB</i> message driven bean. <i>JmsTestQueueMDB</i> sends a new message for each message to outQueue.<br>
	 * Verify that response reply message can be consumed from outQueue.
	 */
	@Test
	public void testQueueMdb() {
		// produce message - a name of queue would be 'null' in case that WildFly/EAP fails to create a connection
		get(wildFlyRouteUrl + "/jms-test?request=" + JmsTestRequestType.REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB.value())
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(containsString(JmsTestConstants.QUEUE_MDB_SEND_RESPONSE));
		get(wildFlyRouteUrl + "/jms-test?request=" + JmsTestRequestType.REQUEST_CONSUME_REPLY_MESSAGE_FOR_MDB.value())
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(containsString(JmsTestConstants.QUEUE_MDB_TEXT_REPLY_MESSAGE));
	}

	/**
	 * Send messages to queue and let them be consumed by MDB. MDB creates new message which is sent
	 * to outQueue for each consumed message.<br>
	 * When MDB is processing messages, kill WildFly/EAP server.<br>
	 * Wait for WildFly/EAP server to be restarted by Openshift (statefulset) and wait for XA recovery and redelivery of the message.<br>
	 * Verify all messages can be consumed, note that we might not get all messages as recovery of XA transactions.
	 */
	@Test
	public void testKillEapXaRecoveryMdb() {
		wildflyOpenShiftProvisioner.scale(1, true);

		final int numberOfMessages = 180;
		// produce 180 messages for MDB to consume (they are moved from inQueue to outQueue),
		// once 100th message is consumed by MDB, WildFly/EAP is killed
		get(wildFlyRouteUrl + "/jms-test?request="
				+ JmsTestRequestType.REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB_AND_KILL_SERVER.value()
				+ "&messageCount=" + numberOfMessages)
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(containsString(numberOfMessages + " messages were sent into queue:"));
		String beforeRestart = ActiveMQArtemisApplication.getMessagesStatus();
		// check that pod was restarted
		OpenShiftWaiters.get(wildflyOpenShiftProvisioner.getOpenShift(), () -> false)
				.havePodsBeenRestarted("name", wildflyOpenShiftProvisioner.getApplication().getName())
				.waitFor();
		wildflyOpenShiftProvisioner.waitForReplicas(1);
		String afterRestart = ActiveMQArtemisApplication.getMessagesStatus();
		// check that the sum of messages in the queues is equal to the total number of messages sent to the server
		int numMessagesInQueueAfter = ActiveMQArtemisApplication.getMessagesCount(JmsTestConstants.IN_QUEUE);
		int numMessagesOutQueueAfter = ActiveMQArtemisApplication.getMessagesCount(JmsTestConstants.OUT_QUEUE);
		log.info("[testKillEapXaRecoveryMdb] numMessagesInQueueAfter {}", numMessagesInQueueAfter);
		log.info("[testKillEapXaRecoveryMdb] numMessagesOutQueueAfter {}", numMessagesOutQueueAfter);
		log.info("[testKillEapXaRecoveryMdb] numberOfMessages {}", numberOfMessages);
		Assertions.assertEquals(numberOfMessages, (numMessagesInQueueAfter + numMessagesOutQueueAfter));

		// make sure all messages can be consumed, note that we might not get all messages as recovery of XA transactions
		// can take up to 5 min, thus periodically consume messages until all are consumed
		long timeout = 300000; // 5 min
		long startTime = System.currentTimeMillis();
		int numberOfReceivedMessages = 0;
		while (numberOfReceivedMessages < numberOfMessages) {
			numberOfReceivedMessages = numberOfReceivedMessages + Integer
					.valueOf(get(wildFlyRouteUrl + "/jms-test?request="
							+ JmsTestRequestType.REQUEST_CONSUME_ALL_REPLY_MESSAGES_FOR_MDB.value())
							.getBody().asString().trim());
			if (System.currentTimeMillis() - startTime > timeout) {
				String afterConsume = ActiveMQArtemisApplication.getMessagesStatus();
				Assertions.fail("Number of replied messages was: " + numberOfReceivedMessages + " however " + numberOfMessages
						+ " was expected.\n" +
						"\nBefore restart:\n" + beforeRestart +
						"\nAfter restart:\n" + afterRestart + "\n" +
						"\nFinal status:\n" + afterConsume + "\n");
			}
		}
		String afterConsume = ActiveMQArtemisApplication.getMessagesStatus();
		log.info("Messages: \n\nBefore restart:\n{}\nAfter restart:\n{}\n\nFinal status:\n{}\n", beforeRestart, afterRestart,
				afterConsume);
	}
}
