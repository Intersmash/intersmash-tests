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

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;

/**
 * Set up an WildFly/JBoss EAP XP s2i application that starts a configured (during the app build) server which
 * uses MicroProfile Reactive Messaging with a connection to a remote AMQ Streams (Kafka) instance.
 * <p>
 * Connections are performed both as not secured (plaintext) and secured via SSL with
 * SSLContext configured via Elytron too, {@link WildflyMicroProfileReactiveMessagingPerConnectorSecuredIT}.
 * <br>
 * Regarding the SSL connections, the Kafka connector on the WildFly/JBoss EAP XP side (client with respect to Kafka) is
 * secured via MicroProfile Config properties that automatically set the Elytron SSL client context.
 * In this use case, the Elytron SSL context name is configured <i>per-connection</i>, see the
 * <a href="https://github.com/wildfly/wildfly/blob/main/testsuite/integration/microprofile/src/test/java/org/wildfly/test/integration/microprofile/reactive/messaging/kafka/ssl/microprofile-config-ssl-connection.properties">WildFLy testsuite version</a>
 */
public class WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	private final BuildInput buildInput;
	public static final String APP_NAME = "mp-reactive-messaging";
	private final List<EnvVar> environmentVariables = new ArrayList<>();
	private final Secret clientSecret;
	private static final String CLIENT_SSL_CONTEXT_NAME = "kafka-ssl-test";

	String password;

	public WildflyMicroProfileReactiveMessagingPerConnectorSecuredApplication() {
		// Set up the Reactive-messaging deployment.
		String applicationDir = "wildfly/microprofile-reactive-messaging-kafka";
		buildInput = new BuildInputBuilder()
				.uri("https://github.com/Intersmash/intersmash-applications.git")
				.ref("main")
				.build();

		clientSecret = OpenShifts.master().getSecret("amq-streams-cluster-ca-cert");
		clientSecret.getMetadata().setName(APP_NAME + "-amq-streams-cluster-ca-cert-secret");
		clientSecret.getMetadata().setResourceVersion(null);
		password = new String(Base64.getDecoder().decode(clientSecret.getData().get("ca.password")));

		// Set up environment
		// SSL
		final String certificateSecretPath = "/etc/secrets/ca.p12";
		environmentVariables.add(
				new EnvVarBuilder().withName("KEYSTORE_PATH")
						.withValue(certificateSecretPath)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("KEYSTORE_PASSWORD")
						.withValue(password)
						.build());
		// The tested application doesn't configure the client-ssl-context via the microprofile-config.properties file,
		// so that it can be deployed on the server without it (server is configured with relevant client-ssl-context
		// after the application is deployed). As such, let's configure values here via environment properties.
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_OUTGOING_SSLTO_WILDFLY_ELYTRON_SSL_CONTEXT")
						.withValue(
								CLIENT_SSL_CONTEXT_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("MP_MESSAGING_INCOMING_SSLFROM_WILDFLY_ELYTRON_SSL_CONTEXT")
						.withValue(
								CLIENT_SSL_CONTEXT_NAME)
						.build());

		environmentVariables.add(
				new EnvVarBuilder().withName("MAVEN_S2I_ARTIFACT_DIRS")
						.withValue(applicationDir + "/target")
						.build());

		final String mavenAdditionalArgs = generateAdditionalMavenArgs()
				.concat(" -pl " + applicationDir + " -am");
		environmentVariables.add(
				new EnvVarBuilder().withName("MAVEN_ARGS_APPEND")
						.withValue(mavenAdditionalArgs)
						.build());
	}

	@Override
	public BuildInput getBuildInput() {
		return buildInput;
	}

	@Override
	public String getName() {
		return APP_NAME;
	}

	@Override
	public List<EnvVar> getEnvVars() {
		return Collections.unmodifiableList(environmentVariables);
	}

	@Override
	public List<Secret> getSecrets() {
		return Stream.of(clientSecret).collect(Collectors.toList());
	}
}
