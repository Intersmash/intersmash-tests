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

import io.amq.broker.v1beta1.ActiveMQArtemis;
import io.amq.broker.v1beta1.ActiveMQArtemisAddress;
import io.amq.broker.v1beta1.activemqartemisspec.ConsoleBuilder;
import io.amq.broker.v1beta1.activemqartemisspec.DeploymentPlanBuilder;
import io.amq.broker.v1beta1.activemqartemisspec.UpgradesBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.ActiveMQOperatorApplication;
import org.jboss.intersmash.provision.operator.model.activemq.address.ActiveMQArtemisAddressBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.ActiveMQArtemisBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.AcceptorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines an ActiveMQ Artemis broker deployment on OpenShift, managed by the AMQ Operator.
 * <p>
 * This application configures a single-instance ActiveMQ Artemis broker with an acceptor
 * supporting all protocols on port {@value #ARTEMIS_ACCEPTOR_PORT}, and provisions an anycast
 * queue named {@value #QUEUE_NAME} for use by the JMS bridge test.
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
	private static final String ARTEMIS_ACCEPTOR_NAME = "all";

	public static final int ARTEMIS_ACCEPTOR_PORT = 61616;
	private final List<ActiveMQArtemisAddress> activeMQArtemisAddresses = new ArrayList<>();
	public static final String QUEUE_NAME = "testQueue";

	public ActiveMQArtemisApplication() throws IOException {
		// Initialize amq-broker ActiveMQArtemis resource
		activeMQArtemis = new ActiveMQArtemisBuilder(NAME)
				.deploymentPlan(new DeploymentPlanBuilder()
						// these size & image are set by DeploymentPlanBuilder by default, set here as an API demonstration
						.withSize(1)
						.withImage(IntersmashConfig.activeMQImageUrl())
						.withRequireLogin(true)
						.withPersistenceEnabled(false)
						.withJournalType("nio")
						.withMessageMigration(false)
						.build())
				.console(new ConsoleBuilder()
						.withExpose(true)
						.build())
				.adminUser(ADMIN_USER)
				.adminPassword(ADMIN_PASSWORD)
				.acceptors(new AcceptorBuilder(ARTEMIS_ACCEPTOR_NAME)
						.protocols("all")
						.port(ARTEMIS_ACCEPTOR_PORT)
						.expose(false)
						.connectionsAllowed(10L)
						.anycastPrefix("jms.queue.")
						.multicastPrefix("jms.topic.")
						.build())
				.upgrades(new UpgradesBuilder()
						.withEnabled(false)
						.withMinor(false)
						.build())
				.build();
		// Initialize test-queue-address ActiveMQArtemisAddress resource
		activeMQArtemisAddresses.add(new ActiveMQArtemisAddressBuilder("test-queue-address")
				.addressName(QUEUE_NAME)
				.queueName(QUEUE_NAME)
				.routingType(
						ActiveMQArtemisAddressBuilder.RoutingType.ANYCAST)
				.build());
	}

	@Override
	public ActiveMQArtemis getActiveMQArtemis() {
		return activeMQArtemis;
	}

	@Override
	public List<ActiveMQArtemisAddress> getActiveMQArtemisAddresses() {
		return activeMQArtemisAddresses;
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * The headless Service provides access to ports 8161 and 61616 on each broker Pod.
	 * Port 8161 is used by the broker management console, and port 61616 is used for broker clustering.
	 * You can also use the headless Service to connect to a broker Pod from an internal client (that is, a client
	 * inside the same OpenShift cluster as the broker deployment).
	 *
	 * @return headless service name to be used to connect the broker from inside OpenShift
	 */
	public static String getHeadlessService() {
		return String.format("%s-hdls-svc", NAME);
	}

	/**
	 * @return name of the first Pod belonging to the statefulset created by the AMQ Operator
	 */
	public static String getFirstPodName() {
		return String.format("%s-ss-0", NAME);
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
		return String.format("%s-%s-0-svc", NAME, ARTEMIS_ACCEPTOR_NAME);
	}
}
