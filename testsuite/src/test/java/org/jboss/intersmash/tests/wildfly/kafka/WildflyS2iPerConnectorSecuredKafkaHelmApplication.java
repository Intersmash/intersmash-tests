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
package org.jboss.intersmash.tests.wildfly.kafka;

import cz.xtf.core.openshift.OpenShifts;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jboss.intersmash.provision.helm.wildfly.WildflyHelmChartRelease;

/**
 * WildFly/JBoss EAP application configured for per-connector SSL with Kafka/Streams for Apache Kafka,
 * deployed via S2I (server provisioning) using Helm Charts.
 * <p>
 * This is the S2I variant of the per-connector secured Kafka application. Unlike
 * {@link WildflyBootableJarPerConnectorSecuredKafkaHelmApplication} which deploys a bootable JAR,
 * this application uses {@link WildflyHelmChartRelease.BuildMode#S2I} to deploy the WAR via the
 * standard S2I builder process.
 * <p>
 * The {@code wildfly/kafka-application} module's {@code s2i} Maven profile is activated to disable
 * bootable JAR packaging.
 * <p>
 * SSL is configured per-connector via
 * {@code MP_MESSAGING_OUTGOING_SSLTO_WILDFLY_ELYTRON_SSL_CONTEXT} and
 * {@code MP_MESSAGING_INCOMING_SSLFROM_WILDFLY_ELYTRON_SSL_CONTEXT}.
 *
 * @see WildflyBootableJarPerConnectorSecuredKafkaHelmApplication
 * @see WildflyBootableJarGloballySecuredKafkaHelmApplication
 */
@Slf4j
public class WildflyS2iPerConnectorSecuredKafkaHelmApplication
		extends WildflyBootableJarGloballySecuredKafkaHelmApplication {
	/** Application name used for labeling and resource identification. */
	public static final String APP_NAME = "mp-reactive-messaging-pc-s2i";

	/**
	 * Constructs a new WildFly/JBoss EAP application configured with per-connector SSL for Kafka,
	 * deployed via S2I.
	 *
	 * @throws IOException if Helm chart configuration loading fails
	 */
	public WildflyS2iPerConnectorSecuredKafkaHelmApplication() throws IOException {
		super();
	}

	@Override
	protected WildflyHelmChartRelease.BuildMode getBuildMode() {
		return WildflyHelmChartRelease.BuildMode.S2I;
	}

	@Override
	protected String getBuildSpecificMavenArgs() {
		return " -Ps2i";
	}

	@Override
	protected void addSslContextEnvironmentVariables(Map<String, String> deploymentEnvironmentVariables) {
		// Configure the Elytron SSL context per-connector (outgoing and incoming)
		deploymentEnvironmentVariables.put("MP_MESSAGING_OUTGOING_SSLTO_WILDFLY_ELYTRON_SSL_CONTEXT",
				CLIENT_SSL_CONTEXT_NAME);
		deploymentEnvironmentVariables.put("MP_MESSAGING_INCOMING_SSLFROM_WILDFLY_ELYTRON_SSL_CONTEXT",
				CLIENT_SSL_CONTEXT_NAME);
	}

	@Override
	public String getName() {
		return APP_NAME;
	}

	public static String getRoute() {
		return OpenShifts.master().generateHostname(APP_NAME);
	}
}
