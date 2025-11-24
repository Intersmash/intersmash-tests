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

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.core.openshift.PodShellOutput;
import io.amq.broker.v1beta1.ActiveMQArtemis;
import io.amq.broker.v1beta1.ActiveMQArtemisAddress;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.ActiveMQOperatorApplication;
import org.jboss.intersmash.provision.operator.model.activemq.broker.ActiveMQArtemisBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.AcceptorBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.ConsoleBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.DeploymentPlanBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.UpgradesBuilder;
import org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.ssl.util.ArtemisCliOutputParser;
import org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.ssl.util.JmsTestConstants;
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ActiveMQ Artemis broker application configured with SSL/TLS security.
 * <p>
 * This application class represents an ActiveMQ Artemis broker deployment on OpenShift with SSL enabled.
 * It configures the broker with:
 * <ul>
 *   <li>SSL-enabled acceptor for secure client connections</li>
 *   <li>Keystore and truststore secrets for SSL certificates</li>
 *   <li>Admin user credentials for broker management</li>
 *   <li>Exposed console and acceptor services</li>
 * </ul>
 * </p>
 * <p>
 * The broker is configured with persistence disabled and NIO journal type for testing purposes.
 * It exposes an SSL acceptor on port {@value #ARTEMIS_ACCEPTOR_PORT} for secure JMS client connections.
 * </p>
 */
public class ActiveMQArtemisApplication implements ActiveMQOperatorApplication, OpenShiftApplication {
	private static final Logger log = LoggerFactory.getLogger(ActiveMQArtemisApplication.class);

	/**
	 * Admin username for broker management and client authentication.
	 */
	public static final String ADMIN_USER = "admin";

	/**
	 * Admin password for broker management and client authentication.
	 */
	public static final String ADMIN_PASSWORD = "3up3r3cr3t!passwd";

	/**
	 * The ActiveMQArtemis custom resource representing the broker deployment.
	 */
	private static ActiveMQArtemis activeMQArtemis;

	/**
	 * The name of the broker deployment.
	 */
	static final String NAME = "activemq-artemis";

	/**
	 * The name of the SSL acceptor configuration.
	 */
	private static final String ACCEPTOR_NAME = "sslacceptor";

	/**
	 * List of Kubernetes secrets containing SSL certificates and credentials.
	 */
	protected final List<Secret> secrets = new ArrayList<>();

	/**
	 * Password for the keystore and truststore.
	 */
	public static final String STOREPASS = "s3cr3t!passwd";

	/**
	 * Alias for the server certificate in the keystore.
	 */
	public static final String KEYALIAS = "server";

	/**
	 * Name of the Kubernetes secret containing the SSL acceptor certificates.
	 */
	public static final String AMQ_ACCEPTOR_SECRET_NAME = String.format("%s-secret", ACCEPTOR_NAME);

	/**
	 * Port number on which the Artemis SSL acceptor listens for client connections.
	 */
	public static final Integer ARTEMIS_ACCEPTOR_PORT = 61617;

	/**
	 * Constructs a new ActiveMQ Artemis broker application with SSL configuration.
	 * <p>
	 * This constructor initializes the broker by:
	 * <ul>
	 *   <li>Generating SSL certificates (keystore and truststore) using the broker service hostname</li>
	 *   <li>Creating a Kubernetes secret containing the SSL certificates and passwords</li>
	 *   <li>Configuring the ActiveMQArtemis custom resource with SSL acceptor, deployment plan, and admin credentials</li>
	 * </ul>
	 * </p>
	 * <p>
	 * The broker is configured with 2 replicas, persistence disabled, and an SSL acceptor
	 * that exposes secure messaging services on port {@value #ARTEMIS_ACCEPTOR_PORT}.
	 * </p>
	 *
	 * @throws IOException if an error occurs while generating SSL certificates or reading certificate files
	 */
	public ActiveMQArtemisApplication() throws IOException {
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo certificateInfo = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						getAcceptorServiceName(),
						KEYALIAS,
						STOREPASS, null,
						Collections.emptyList());

		/*
		    apiVersion: v1
		    kind: Secret
		    metadata:
		      name: ex-aao-sslacceptor-secret
		    type: Opaque
		    stringData:
		      alias: server
		      keyStorePassword: password
		      trustStorePassword: password
		    data:
		      broker.ks: $(cat privatekey.pkcs12 | base64 -w 0)
		      client.ts: $(cat truststore.pkcs12 | base64 -w 0)
		 */
		Secret sslAcceptorSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(AMQ_ACCEPTOR_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", NAME))
				.endMetadata()
				.addToStringData("alias", KEYALIAS)
				.addToStringData("keyStorePassword", STOREPASS)
				.addToStringData("trustStorePassword", STOREPASS)
				.addToData(Map.of("broker.ks",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(certificateInfo.keystore.toFile()))))
				.addToData(Map.of("client.ts",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(certificateInfo.truststore.toFile()))))
				.build();
		secrets.add(sslAcceptorSecret);

		/*
		    apiVersion: broker.amq.io/v1beta1
		    kind: ActiveMQArtemis
		    metadata:
		      name: ex-aao
		      namespace: amq
		    spec:
		      properties:
		        adminPassword: admin
		        adminUser: password
		      deploymentPlan:
		        image: placeholder
		        jolokiaAgentEnabled: false
		        journalType: nio
		        managementRBACEnabled: true
		        messageMigration: false
		        persistenceEnabled: false
		        requireLogin: false
		        size: 1
		      acceptors:
		      - name: sslacceptor
		        protocols: all
		        port: 61617
		        sslEnabled: true
		        sslSecret: ex-aao-sslacceptor-secret
		        verifyHost: false
		        expose: true
		 */

		// Initialize activemq-artemis ActiveMQArtemis resource
		activeMQArtemis = new ActiveMQArtemisBuilder(NAME)
				.deploymentPlan(new DeploymentPlanBuilder()
						// these size & image are set by DeploymentPlanBuilder by default, set here as an API demonstration
						.size(2)
						.image(IntersmashConfig.activeMQImageUrl())
						.initImage(IntersmashConfig.activeMQInitImageUrl())
						.requireLogin(true)
						.persistenceEnabled(false)
						.journalType("nio")
						.messageMigration(false)
						.build())
				.console(new ConsoleBuilder()
						.expose(true)
						.build())
				/**
				  * Produce a final acceptor configured with SSL, e.g.:
				  * <code>
				  *     <acceptor name="sslacceptor">
				  *         tcp://activemq-artemis-ss-0.activemq-artemis-hdls-svc.appsint-mgbr.svc.cluster.local:61617?
				  *             protocols=AMQP,CORE,HORNETQ,MQTT,OPENWIRE,STOMP;
				  *             sslEnabled=true;
				  *             keyStorePath=/etc/sslacceptor-acceptor-secret-volume/broker.ks;
				  *             keyStorePassword=s3cr3t!passwd;
				  *             trustStorePath=/etc/sslacceptor-acceptor-secret-volume/client.ts;
				  *             trustStorePassword=s3cr3t!passwd;
				  *             sslProvider=JDK;
				  *             anycastPrefix=jms.queue.;
				  *             multicastPrefix=jms.topic.;
				  *             connectionsAllowed=10;
				  *             tcpSendBufferSize=1048576;
				  *             tcpReceiveBufferSize=1048576;
				  *             useEpoll=true;amqpCredits=1000;
				  *             amqpMinCredits=300
				  *     </acceptor>
				  * </code>
				 */
				.acceptors(new AcceptorBuilder(ACCEPTOR_NAME)
						.protocols("all")
						.port(ARTEMIS_ACCEPTOR_PORT)
						.sslEnabled(true)
						.sslSecret(sslAcceptorSecret.getMetadata().getName())
						.verifyHost(false)
						.sslProvider("JDK")
						.expose(true)
						.connectionsAllowed(10L)
						.anycastPrefix("jms.queue.")
						.multicastPrefix("jms.topic.")
						.build())
				.upgrades(new UpgradesBuilder()
						.enabled(false)
						.minor(false)
						.build())
				.adminUser(ADMIN_USER)
				.adminPassword(ADMIN_PASSWORD)
				.build();
	}

	/**
	 * Returns the ActiveMQArtemis custom resource that defines the broker deployment.
	 *
	 * @return the ActiveMQArtemis custom resource
	 */
	@Override
	public ActiveMQArtemis getActiveMQArtemis() {
		return activeMQArtemis;
	}

	/**
	 * Returns the list of ActiveMQArtemisAddress resources for pre-configured addresses.
	 * <p>
	 * This application does not pre-configure any addresses; they are created dynamically by the broker for
	 * the mappings in the application, e.g.:
	 * <pre>
	 * {@code
	 * @JMSDestinationDefinition(name = "java:/queue/testQueue", interfaceName = "jakarta.jms.Queue", destinationName = "test-queue")
	 * }
	 * </pre>
	 * </p>
	 *
	 * @return an empty list
	 */
	@Override
	public List<ActiveMQArtemisAddress> getActiveMQArtemisAddresses() {
		return Collections.emptyList();
	}

	/**
	 * Returns the name of the broker application.
	 *
	 * @return the application name
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Returns the list of Kubernetes secrets required by this application.
	 * <p>
	 * The secrets contain SSL certificates (keystore and truststore) and their passwords
	 * required for the SSL acceptor configuration.
	 * </p>
	 *
	 * @return the list of secrets
	 */
	@Override
	public List<Secret> getSecrets() {
		return secrets;
	}

	/**
	 * Returns the Kubernetes service name for the ActiveMQ Artemis Broker acceptor.
	 * <p>
	 * The service name follows the naming convention for automatically-created services in ActiveMQ Artemis Broker on OpenShift.
	 * The format is {@code <custom-resource-name>-<acceptor-name>-<broker-pod-ordinal>-svc}.
	 * For example, {@code activemq-artemis-sslacceptor-0-svc}.
	 * </p>
	 *
	 * @return the Kubernetes service name for the ActiveMQ Artemis Broker acceptor
	 */
	public static String getAcceptorServiceName() {
		return String.format("%s-%s-0-svc", NAME, ACCEPTOR_NAME);
	}

	/**
	 * Retrieves the total count of messages in a specific queue across all broker pods.
	 * <p>
	 * This method connects to each broker pod via the Artemis CLI and queries the message count
	 * for the specified queue. The counts from all pods are summed to provide the total.
	 * </p>
	 *
	 * @param queueName the name of the queue to query
	 * @return the total number of messages in the specified queue across all broker pods
	 */
	public static int getMessagesCount(String queueName) {
		int messagesCount = 0;
		Map<String, Map<String, Integer>> podInfo = new HashMap<>();
		// get broker pods: they are labeled with the CRD kind they were created from
		List<Pod> pods = OpenShifts.master().getLabeledPods(ActiveMQArtemis.class.getSimpleName(), NAME);
		for (Pod pod : pods) {
			Map<String, Integer> queueInfo = new HashMap<>();
			podInfo.put(pod.getMetadata().getName(), queueInfo);
			// get queues on the broker pod
			String cmdArtemis = String.format(
					"/home/jboss/amq-broker/bin/artemis queue stat --user=%s --password='%s' --json --url=tcp://%s:61616",
					ADMIN_USER,
					ADMIN_PASSWORD,
					pod.getStatus().getPodIP());
			PodShellOutput queues = OpenShifts.master().podShell(pod).executeWithBash(cmdArtemis);
			String cmdError = queues.getError();
			if (!Strings.isNullOrEmpty(sanitizeShellOutput(cmdError))) {
				log.warn(
						"[getMessagesCount] Error getting queues stats from artemis with command \"{}\" on Pod {}: \nOutput: {}\nError: {}",
						cmdArtemis, pod.getMetadata().getName(), queues.getOutput(), cmdError);
			}
			try {
				int cnt = ArtemisCliOutputParser.sumMessageCounts(queues.getOutput(), queueName);
				messagesCount += cnt;
				log.info("[getMessagesCount] Messages for queue {} on POD {}: {}", queueName, pod.getMetadata().getName(), cnt);
			} catch (NumberFormatException | JsonSyntaxException err) {
				log.error("[getMessagesCount] Error parsing queues stats from artemis: \n{}\n", queues.getOutput(), err);
			}
		}
		return messagesCount;
	}

	/**
	 * Counts the number of messages on every queue of every brokers' PODs
	 * @return string containing above info formatted as a plain text message
	 */
	public static String getMessagesStatus() {
		Map<String, Map<String, Integer>> podInfo = new HashMap<>();
		// get broker pods: they are labeled with the CRD kind they were created from
		List<Pod> pods = OpenShifts.master().getLabeledPods(ActiveMQArtemis.class.getSimpleName(), NAME);
		for (Pod pod : pods) {
			Map<String, Integer> queueInfo = new HashMap<>();
			podInfo.put(pod.getMetadata().getName(), queueInfo);
			// get queues on the broker pod
			log.info("[getMessagesStatus] pod: {}", pod.getMetadata().getName());
			String cmdArtemis = String.format(
					"/home/jboss/amq-broker/bin/artemis address show --user=%s --password='%s' --url=tcp://%s:61616",
					ADMIN_USER,
					ADMIN_PASSWORD,
					pod.getStatus().getPodIP());
			PodShellOutput queues = OpenShifts.master().podShell(pod).executeWithBash(cmdArtemis);
			String cmdError = queues.getError();
			if (!Strings.isNullOrEmpty(sanitizeShellOutput(cmdError))) {
				log.warn(
						"[getMessagesStatus] Error getting queues from artemis with command \"{}\" on Pod {}: \nOutput: {}\nError: {}",
						cmdArtemis, pod.getMetadata().getName(), queues.getOutput(), cmdError);
			} else {
				for (String queue : queues.getOutputAsList()) {
					log.info("[getMessagesStatus] pod {}, address show line: {}", pod.getMetadata().getName(), queue);
					Integer count = 0;
					if (Arrays.asList(JmsTestConstants.TEST_QUEUE, JmsTestConstants.IN_QUEUE, JmsTestConstants.OUT_QUEUE)
							.contains(queue)
							|| Arrays.asList(ArtemisCliOutputParser.convertCamelCaseToKebab(JmsTestConstants.TEST_QUEUE),
									ArtemisCliOutputParser.convertCamelCaseToKebab(JmsTestConstants.IN_QUEUE),
									ArtemisCliOutputParser.convertCamelCaseToKebab(JmsTestConstants.OUT_QUEUE))
									.contains(queue)) {
						// get messages on the queue
						cmdArtemis = String.format(
								"/home/jboss/amq-broker/bin/artemis browser --user=%s --password='%s' --destination=queue://%s --url=tcp://%s:61616",
								ADMIN_USER,
								ADMIN_PASSWORD,
								queue,
								pod.getStatus().getPodIP());
						PodShellOutput messages = OpenShifts.master().podShell(pod).executeWithBash(cmdArtemis);
						cmdError = messages.getError();
						if (!Strings.isNullOrEmpty(sanitizeShellOutput(cmdError))) {
							log.warn(
									"[getMessagesStatus] Error getting messages from artemis's queue {} with command \"{}\" on Pod {}: \nOutput: {}\nError: {}",
									queue, cmdArtemis, pod.getMetadata().getName(), queues.getOutput(), cmdError);
						} else {
							for (String message : messages.getOutputAsList()) {
								if (message.contains(JmsTestConstants.QUEUE_MDB_TEXT_REPLY_MESSAGE)
										|| message.contains(JmsTestConstants.QUEUE_MDB_TEXT_MESSAGE)) {
									count++;
								}
							}
							queueInfo.put(queue, count);
						}
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Map<String, Integer>> podEntry : podInfo.entrySet()) {
			sb.append("\tBroker pod \"");
			sb.append(podEntry.getKey());
			sb.append("\":");
			for (Map.Entry<String, Integer> queueEntry : podEntry.getValue().entrySet()) {
				sb.append("\n\t\tQueue \"");
				sb.append(queueEntry.getKey());
				sb.append("\" num.messages: ");
				sb.append(queueEntry.getValue());
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Sanitizes shell output by removing informational NOTE: lines.
	 * <p>
	 * This method strips out lines beginning with "NOTE:" that are often added
	 * by shell commands as informational messages, leaving only the actual command output.
	 * </p>
	 *
	 * @param output the raw shell output to sanitize
	 * @return the sanitized output with NOTE: lines removed
	 */
	private static String sanitizeShellOutput(String output) {
		return output.replaceAll("^NOTE:[^\\n]+[\\r\\n]*", "");
	}
}
