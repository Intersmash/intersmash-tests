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

import cz.xtf.core.openshift.OpenShifts;
import io.amq.broker.v1beta1.ActiveMQArtemis;
import io.amq.broker.v1beta1.ActiveMQArtemisAddress;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.ActiveMQOperatorApplication;
import org.jboss.intersmash.provision.operator.model.activemq.broker.ActiveMQArtemisBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.AcceptorBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.ConnectorBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.ConsoleBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.DeploymentPlanBuilder;
import org.jboss.intersmash.provision.operator.model.activemq.broker.spec.UpgradesBuilder;
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;

/**
 * ActiveMQ Artemis broker application configured with SSL/TLS for AMQP connector testing.
 * <p>
 * This application deploys an ActiveMQ Artemis broker on OpenShift with SSL enabled,
 * configured for use with the MicroProfile Reactive Messaging AMQP connector.
 * It requires mutual TLS (client certificate authentication).
 * </p>
 */
public class ActiveMQArtemisAmqpApplication implements ActiveMQOperatorApplication, OpenShiftApplication {

	static final String NAME = "amq-broker";
	private static final String ACCEPTOR_NAME = "all";
	public static final String ADMIN_USER = "admin";
	public static final String ADMIN_PASSWORD = "admin";
	protected static final String STOREPASS = "password";
	protected static final String KEYALIAS = "server";
	public static final String AMQ_ACCEPTOR_SECRET_NAME = String.format("%s-secret", ACCEPTOR_NAME);

	private static ActiveMQArtemis activeMQArtemis;
	protected final List<Secret> secrets = new ArrayList<>();

	public ActiveMQArtemisAmqpApplication() throws IOException {
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo certificateInfo = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						getAcceptorServiceName(),
						KEYALIAS,
						STOREPASS, null,
						Arrays.asList(getWildcardSAN()));

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

		activeMQArtemis = new ActiveMQArtemisBuilder(NAME)
				.deploymentPlan(new DeploymentPlanBuilder()
						.size(2)
						.image(IntersmashConfig.activeMQImageUrl())
						.initImage(IntersmashConfig.activeMQInitImageUrl())
						.requireLogin(false)
						.persistenceEnabled(false)
						.journalType("nio")
						.messageMigration(false)
						.build())
				.console(new ConsoleBuilder()
						.expose(true)
						.build())
				.acceptors(new AcceptorBuilder(ACCEPTOR_NAME)
						.protocols("all")
						.port(61617)
						.sslEnabled(true)
						.sslSecret(sslAcceptorSecret.getMetadata().getName())
						.needClientAuth(true)
						.wantClientAuth(true)
						.verifyHost(false)
						.sslProvider("JDK")
						.expose(true)
						.connectionsAllowed(10L)
						.anycastPrefix("jms.queue.")
						.multicastPrefix("jms.topic.")
						.build())
				.connectors(new ConnectorBuilder("connector0")
						.host("localhost")
						.port(22222)
						.sslEnabled(false)
						.enabledCipherSuites("SSL_RSA_WITH_RC4_128_SHA,SSL_DH_anon_WITH_3DES_EDE_CBC_SHA")
						.enabledProtocols("TLSv1,TLSv1.1,TLSv1.2")
						.needClientAuth(true)
						.wantClientAuth(true)
						.verifyHost(true)
						.sslProvider("JDK")
						.expose(true)
						.build())
				.upgrades(new UpgradesBuilder()
						.enabled(false)
						.minor(false)
						.build())
				.build();
	}

	@Override
	public ActiveMQArtemis getActiveMQArtemis() {
		return activeMQArtemis;
	}

	@Override
	public List<ActiveMQArtemisAddress> getActiveMQArtemisAddresses() {
		return Collections.emptyList();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public List<Secret> getSecrets() {
		return secrets;
	}

	public static String getAcceptorServiceName() {
		return String.format("%s-%s-0-svc", NAME, ACCEPTOR_NAME);
	}

	/**
	 * Returns the route to the AMQ Broker acceptor service.
	 *
	 * @return route to AMQ Broker acceptor service
	 */
	public static String getRoute() {
		return OpenShifts.master().generateHostname(String.format("%s-%s-0-svc-rte", NAME, ACCEPTOR_NAME));
	}

	public static String getWildcardSAN() {
		return String.format("*.apps.%s", OpenShifts.master().getOpenshiftUrl().getHost().replaceFirst("^api\\.", ""));
	}
}
