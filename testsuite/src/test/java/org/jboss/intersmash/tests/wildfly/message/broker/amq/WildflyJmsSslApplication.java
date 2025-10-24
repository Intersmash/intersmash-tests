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
package org.jboss.intersmash.tests.wildfly.message.broker.amq;

import cz.xtf.builder.builders.pod.PersistentVolumeClaim;
import cz.xtf.builder.builders.pod.VolumeMount;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;

public class WildflyJmsSslApplication implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {
	static final String NAME = "wildfly-jms";
	protected final List<Secret> secrets = new ArrayList<>();
	public static final String WILDFLY_TRUSTSTORE_SECRET_NAME = String.format("%s-truststore-secret", NAME);
	private final BuildInput buildInput;
	private final Map<PersistentVolumeClaim, Set<VolumeMount>> persistentVolumeClaimMounts;
	private final List<EnvVar> environmentVariables;
	private static final String WILDFLY_TRUSTSTORE_DIR_NAME = "/etc/secrets";
	private static final String WILDFLY_TRUSTSTORE_FILE_NAME = "client.ts";

	public WildflyJmsSslApplication() throws IOException {
		final String applicationDir = "wildfly/activemq-artemis-ssl";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo infinispanCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						AmqBrokerSslApplication.getAcceptorServiceName(),
						AmqBrokerSslApplication.KEYALIAS,
						AmqBrokerSslApplication.STOREPASS,
						Collections.emptyList());
		/*
		    apiVersion: v1
		    kind: Secret
		    metadata:
		      name: ex-aao-truststore-secret
		    type: Opaque
		    stringData:
		      alias: server
		      trustStorePassword: 1234PIPPOBAUDO
		    data:
		      client.ts: $(cat truststore.pkcs12 | base64 -w 0)
		 */
		Secret sslTrustStoreSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(WILDFLY_TRUSTSTORE_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", NAME))
				.endMetadata()
				.addToStringData("alias", AmqBrokerSslApplication.KEYALIAS)
				.addToStringData("trustStorePassword", AmqBrokerSslApplication.STOREPASS)
				.addToData(Map.of(WILDFLY_TRUSTSTORE_FILE_NAME,
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(infinispanCertificate.truststore.toFile()))))
				.build();
		secrets.add(sslTrustStoreSecret);

		// Setup PVC which will be used as mount point "/opt/eap/standalone/data" where EAP stores transaction tx object and store and messaging journal.
		persistentVolumeClaimMounts = new HashMap<>();
		Set<VolumeMount> volumeMounts = new HashSet<>();
		String pvcName = "data-dir";
		volumeMounts.add(
				new VolumeMount(pvcName, "/opt/server/standalone/data", false));
		persistentVolumeClaimMounts.put(new PersistentVolumeClaim(pvcName, pvcName), volumeMounts);

		// Setup environment
		environmentVariables = new ArrayList<>();
		environmentVariables.add(new EnvVarBuilder().withName("SCRIPT_DEBUG").withValue("true").build());
		environmentVariables.add(new EnvVarBuilder().withName("DISABLE_EMBEDDED_JMS_BROKER").withValue("true").build());

		environmentVariables.add(
				new EnvVarBuilder().withName("MAVEN_S2I_ARTIFACT_DIRS")
						.withValue(applicationDir + "/target")
						.build());
		String mavenAdditionalArgs = generateAdditionalMavenArgs()
				.concat(" -pl " + applicationDir + " -am");
		final String mavenMirrorUrl = this.getMavenMirrorUrl();
		if (!Strings.isNullOrEmpty(mavenMirrorUrl)) {
			environmentVariables.add(
					new EnvVarBuilder().withName("MAVEN_MIRROR_URL")
							.withValue(mavenMirrorUrl)
							.build());
			mavenAdditionalArgs = mavenAdditionalArgs.concat(" -Dinsecure.repositories=WARN");
		}
		environmentVariables.add(
				new EnvVarBuilder().withName("MAVEN_ARGS_APPEND")
						.withValue(mavenAdditionalArgs)
						.build());

		environmentVariables
				.add(new EnvVarBuilder().withName("ARTEMIS_USER").withValue(AmqBrokerSslApplication.ADMIN_USER).build());
		environmentVariables.add(
				new EnvVarBuilder().withName("ARTEMIS_PASSWORD").withValue(AmqBrokerSslApplication.ADMIN_PASSWORD).build());
		environmentVariables.add(new EnvVarBuilder().withName("TRUST_STORE_FILENAME").withValue(
				String.format("%s/%s", WILDFLY_TRUSTSTORE_DIR_NAME, WILDFLY_TRUSTSTORE_FILE_NAME)).build());
		environmentVariables
				.add(new EnvVarBuilder().withName("TRUSTSTORE_PASSWORD").withValue(AmqBrokerSslApplication.STOREPASS).build());
		/**
		 * Environment variables <b>JBOSS_MESSAGING_CONNECTOR_HOST</b> and <b>JBOSS_MESSAGING_CONNECTOR_PORT</b>
		 * correspond to auto-generated configuration in WildFly for the default connection to a remote ActiveMQ broker:
		 * <code>
		 *   <outbound-socket-binding name="messaging-activemq">
		 *     <remote-destination host="${jboss.messaging.connector.host:localhost}" port="${jboss.messaging.connector.port:61616}"/>
		 *   </outbound-socket-binding>
		 * </code>
		 */
		environmentVariables.add(new EnvVarBuilder().withName("JBOSS_MESSAGING_CONNECTOR_HOST")
				.withValue(AmqBrokerSslApplication.getAcceptorServiceName()).build());
		environmentVariables.add(new EnvVarBuilder().withName("JBOSS_MESSAGING_CONNECTOR_PORT")
				.withValue(AmqBrokerSslApplication.ARTEMIS_ACCEPTOR_PORT.toString()).build());
	}

	@Override
	public BuildInput getBuildInput() {
		return buildInput;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public List<Secret> getSecrets() {
		return secrets;
	}

	@Override
	public Map<PersistentVolumeClaim, Set<VolumeMount>> getPersistentVolumeClaimMounts() {
		return Collections.unmodifiableMap(persistentVolumeClaimMounts);
	}

	@Override
	public List<EnvVar> getEnvVars() {
		return Collections.unmodifiableList(environmentVariables);
	}
}
