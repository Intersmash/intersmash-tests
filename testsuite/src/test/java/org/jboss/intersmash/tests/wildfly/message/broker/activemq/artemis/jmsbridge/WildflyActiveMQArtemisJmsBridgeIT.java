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
package org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.jmsbridge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.openshift.PodShell;
import cz.xtf.core.waiting.SimpleWaiter;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.fabric8.kubernetes.api.model.Pod;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.operator.ActiveMQOperatorProvisioner;
import org.jboss.intersmash.tests.junit.annotations.ActiveMQArtemisTest;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@WildflyTest
@EapTest
@EapXpTest
@ActiveMQArtemisTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Slf4j
@Intersmash({
		@Service(ActiveMQArtemisApplication.class),
		@Service(WildflyJmsBridgeApplication.class)
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WildflyActiveMQArtemisJmsBridgeIT {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final int MAX_SECONDS_WAIT_FOR_JMS_BRIDGE_RECONCILIATION = 70;

	static final String QUEUE_SEND_RESPONSE = "Sent a text message to ";
	static final String QUEUE_TEXT_MESSAGE = "Hello Servlet!";
	static final String QUEUE_COUNT_TEMPLATE = "browsed: %d messages";
	static final String REQUEST_PRODUCE = "produce";
	static final String REQUEST_COUNT = "count";

	@ServiceUrl(WildflyJmsBridgeApplication.class)
	private String eapUrl;

	@ServiceProvisioner(ActiveMQArtemisApplication.class)
	private ActiveMQOperatorProvisioner amqBrokerOperatorProvisioner;

	private static final String FIRST_POD_NAME = ActiveMQArtemisApplication.getFirstPodName();

	/**
	 * Produces messages and sends them via a JMS bridge to an AMQ broker.
	 * <p>
	 * Verifies that the destination queue is empty before sending, then produces multiple messages
	 * and confirms they all arrive at the remote AMQ broker through the JMS bridge.
	 * </p>
	 *
	 * @see <a href="https://issues.redhat.com/browse/JBQA-14186">JBQA-14186</a>
	 */
	@Test
	@Order(1)
	public void testSendMessageToAMQThroughJmsBridge() throws Exception {

		assertThat(getTestQueueInfo().replace("\n", " "), containsString("browsed: 0 messages"));

		// Produce a message to be sent to the JMS Bridge on WildFly/JBoss EAP
		final int totMessages = 10;
		for (int messages = 0; messages < totMessages; messages++) {
			HttpResponse<String> response = httpGet(
					eapUrl + "/jms-test?request=" + REQUEST_PRODUCE + "&test=testSendMessageToAMQThroughJmsBridge");
			assertEquals(200, response.statusCode());
			assertThat(response.body(), containsString(QUEUE_SEND_RESPONSE));
		}

		assertThat(getTestQueueInfo().replace("\n", " "), containsString("browsed: " + totMessages + " messages"));
	}

	/**
	 * Tests JMS bridge resilience by scaling the AMQ cluster to 0 and back.
	 * <p>
	 * This test verifies that the JMS bridge correctly handles broker unavailability:
	 * </p>
	 * <ul>
	 *   <li>Scales the AMQ broker to 0 replicas, making it unavailable</li>
	 *   <li>Produces a message on WildFly/JBoss EAP, which should be held locally until the bridge reconnects</li>
	 *   <li>Scales the AMQ broker back to 1 replica and waits for the JMS bridge to re-establish
	 *       the connection</li>
	 *   <li>Verifies that the parked message is delivered to the AMQ broker once connectivity
	 *       is restored, and that subsequent messages are also delivered successfully</li>
	 * </ul>
	 */
	@Test
	@Order(2)
	public void testScaleAMQ() throws Exception {
		// being the AMQ Broker stateless in the current configuration, scaling to zero causes loss of all messages
		amqBrokerOperatorProvisioner.scale(0, true);

		// Produce a message to be sent to the JMS Bridge on WildFly/JBoss EAP: the message should wait on WildFly/JBoss EAP until AQM is resumed
		HttpResponse<String> response = httpGet(eapUrl + "/jms-test?request=" + REQUEST_PRODUCE + "&test=testScaleAMQ");
		assertEquals(200, response.statusCode());
		assertThat(response.body(), containsString(QUEUE_SEND_RESPONSE));

		// Log how may messages are queued on
		HttpResponse<String> cnt = httpGet(eapUrl + "/jms-test?request=" + REQUEST_COUNT + "&test=testScaleAMQ");
		log.info("BEFORE RESTARTING AMQ:\n" + cnt.body());

		// scaling back to 1: now the message parked on WildFly/JBoss EAP is sent to AMQ
		amqBrokerOperatorProvisioner.scale(1, true);

		// We have to wait _at least. failure-retry-interval (.5 seconds in our case) before checking, but
		// it will take more time since the ActiveMQ Broker instance is ready again, and the JMS Bridge reconnected.
		// We've seen this to change a bit between 55/65 seconds, depending on the environment, so we set the waiter to
		// last for 70 seconds, just to be sure.
		SimpleWaiter waiter = new SimpleWaiter(
				() -> {
					try {
						HttpResponse<String> countResponse = httpGet(
								eapUrl + "/jms-test?request=" + REQUEST_COUNT + "&test=testScaleAMQ");
						return countResponse.body().contains(String.format(QUEUE_COUNT_TEMPLATE, 0));
					} catch (IOException | InterruptedException e) {
						throw new RuntimeException(e);
					}
				},
				"Waiting for the JMS Bridge to be reconnected, and messages count to be 0").interval(TimeUnit.SECONDS, 3);
		waiter.timeout(TimeUnit.SECONDS, MAX_SECONDS_WAIT_FOR_JMS_BRIDGE_RECONCILIATION).waitFor();

		// just the one message parked on WildFly/JBoss EAP that is sent after AMQ Broker is resumed
		new SimpleWaiter(
				() -> getTestQueueInfo().replace("\n", " ").contains("browsed: 1 messages"),
				"Waiting for 1 message to appear on the AMQ broker queue")
				.interval(TimeUnit.SECONDS, 5)
				.timeout(TimeUnit.SECONDS, MAX_SECONDS_WAIT_FOR_JMS_BRIDGE_RECONCILIATION)
				.waitFor();

		// Produce a message to be sent to the JMS Bridge on WildFly/JBoss EAP
		HttpResponse<String> produceResponse = httpGet(eapUrl + "/jms-test?request=" + REQUEST_PRODUCE + "&test=testScaleAMQ");
		assertEquals(200, produceResponse.statusCode());
		assertThat(produceResponse.body(), containsString(QUEUE_SEND_RESPONSE));

		assertThat(getTestQueueInfo().replace("\n", " "), containsString("browsed: 2 messages"));
	}

	private HttpResponse<String> httpGet(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();
		return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	}

	/**
	 * Browses the test queue on the AMQ broker pod via the Artemis CLI and returns the command output.
	 *
	 * @return the output of the {@code artemis browser} command for the configured test queue
	 */
	private static String getTestQueueInfo() {
		Pod brokerPod = OpenShifts.master().getPod(FIRST_POD_NAME);
		PodShell podShell = OpenShifts.master().podShell(brokerPod);
		String output = podShell.executeWithBash(
				String.format(
						"amq-broker/bin/artemis browser --url=tcp://%s:%d --user=%s --password='%s' --destination=queue://%s",
						ActiveMQArtemisApplication.getHeadlessService(), ActiveMQArtemisApplication.ARTEMIS_ACCEPTOR_PORT,
						ActiveMQArtemisApplication.ADMIN_USER, ActiveMQArtemisApplication.ADMIN_PASSWORD,
						ActiveMQArtemisApplication.QUEUE_NAME))
				.getOutput();
		log.info(output);
		return output;
	}
}
