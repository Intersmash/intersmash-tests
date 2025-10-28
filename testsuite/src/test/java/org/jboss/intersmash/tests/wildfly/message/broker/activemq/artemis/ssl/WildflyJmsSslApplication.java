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

/**
 * WildFly application configured to connect to an external ActiveMQ Artemis message broker using SSL/TLS.
 * <p>
 * This application sets up a WildFly server that disables the embedded JMS broker and instead connects
 * to an external ActiveMQ Artemis broker with SSL enabled. It configures the necessary truststore,
 * persistent volumes for transaction logs and messaging journals, and environment variables required
 * for secure messaging connectivity.
 * </p>
 */
public class WildflyJmsSslApplication implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {
	/**
	 * The application name used for identifying this WildFly JMS application instance.
	 */
	static final String NAME = "wildfly-jms";

	/**
	 * List of Kubernetes secrets containing SSL/TLS certificates and truststore data.
	 */
	protected final List<Secret> secrets = new ArrayList<>();

	/**
	 * Name of the Kubernetes secret containing the WildFly client truststore.
	 */
	public static final String WILDFLY_TRUSTSTORE_SECRET_NAME = String.format("%s-truststore-secret", NAME);

	/**
	 * Build input configuration specifying the source repository for the application.
	 */
	private final BuildInput buildInput;

	/**
	 * Map of persistent volume claims to their associated volume mounts for storing WildFly data.
	 */
	private final Map<PersistentVolumeClaim, Set<VolumeMount>> persistentVolumeClaimMounts;

	/**
	 * List of environment variables to configure the WildFly application container.
	 */
	private final List<EnvVar> environmentVariables;

	/**
	 * Directory path where the truststore will be mounted in the container.
	 */
	private static final String WILDFLY_TRUSTSTORE_DIR_NAME = "/etc/secrets";

	/**
	 * Filename of the client truststore file.
	 */
	private static final String WILDFLY_TRUSTSTORE_FILE_NAME = "client.ts";

	/**
	 * Constructs a new WildFly JMS SSL application with all necessary configurations.
	 * <p>
	 * This constructor performs the following setup:
	 * <ul>
	 *   <li>Configures the build input from the deployments repository</li>
	 *   <li>Generates SSL certificates and creates a truststore for secure communication</li>
	 *   <li>Creates Kubernetes secrets containing the truststore data</li>
	 *   <li>Sets up persistent volume claims for WildFly data directory</li>
	 *   <li>Configures environment variables for broker connectivity and SSL settings</li>
	 * </ul>
	 * </p>
	 *
	 * @throws IOException if there is an error reading certificate files or generating keystores
	 */
	public WildflyJmsSslApplication() throws IOException {
		final String applicationDir = "wildfly/activemq-artemis-ssl";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo certificateInfo = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						ActiveMQArtemisApplication.getAcceptorServiceName(),
						ActiveMQArtemisApplication.KEYALIAS,
						ActiveMQArtemisApplication.STOREPASS,
						Collections.emptyList());
		/*
		    apiVersion: v1
		    kind: Secret
		    metadata:
		      name: ex-aao-truststore-secret
		    type: Opaque
		    stringData:
		      alias: server
		      trustStorePassword: password
		    data:
		      client.ts: $(cat truststore.pkcs12 | base64 -w 0)
		 */
		Secret sslTrustStoreSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(WILDFLY_TRUSTSTORE_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", NAME))
				.endMetadata()
				.addToStringData("alias", ActiveMQArtemisApplication.KEYALIAS)
				.addToStringData("trustStorePassword", ActiveMQArtemisApplication.STOREPASS)
				.addToData(Map.of(WILDFLY_TRUSTSTORE_FILE_NAME,
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(certificateInfo.truststore.toFile()))))
				.build();
		secrets.add(sslTrustStoreSecret);

		// Setup PVC which will be used as mount point "/opt/eap/standalone/data" where WildFly/EAP stores transaction tx object and store and messaging journal.
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
				.add(new EnvVarBuilder().withName("ARTEMIS_USER").withValue(ActiveMQArtemisApplication.ADMIN_USER).build());
		environmentVariables.add(
				new EnvVarBuilder().withName("ARTEMIS_PASSWORD").withValue(ActiveMQArtemisApplication.ADMIN_PASSWORD).build());
		environmentVariables.add(new EnvVarBuilder().withName("TRUST_STORE_FILENAME").withValue(
				String.format("%s/%s", WILDFLY_TRUSTSTORE_DIR_NAME, WILDFLY_TRUSTSTORE_FILE_NAME)).build());
		environmentVariables
				.add(new EnvVarBuilder().withName("TRUSTSTORE_PASSWORD").withValue(ActiveMQArtemisApplication.STOREPASS)
						.build());
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
				.withValue(ActiveMQArtemisApplication.getAcceptorServiceName()).build());
		environmentVariables.add(new EnvVarBuilder().withName("JBOSS_MESSAGING_CONNECTOR_PORT")
				.withValue(ActiveMQArtemisApplication.ARTEMIS_ACCEPTOR_PORT.toString()).build());
	}

	/**
	 * Returns the build input configuration for this application.
	 *
	 * @return the build input specifying the source repository URL and reference
	 */
	@Override
	public BuildInput getBuildInput() {
		return buildInput;
	}

	/**
	 * Returns the name of this application.
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
	 * The secrets include the truststore data needed for SSL/TLS communication with the ActiveMQ broker.
	 * </p>
	 *
	 * @return the list of secrets
	 */
	@Override
	public List<Secret> getSecrets() {
		return secrets;
	}

	/**
	 * Returns the persistent volume claim mounts for this application.
	 * <p>
	 * The persistent volumes are used to store WildFly's transaction logs and messaging journals
	 * in the {@code /opt/server/standalone/data} directory.
	 * </p>
	 *
	 * @return an unmodifiable map of persistent volume claims to their volume mounts
	 */
	@Override
	public Map<PersistentVolumeClaim, Set<VolumeMount>> getPersistentVolumeClaimMounts() {
		return Collections.unmodifiableMap(persistentVolumeClaimMounts);
	}

	/**
	 * Returns the environment variables required to configure this application.
	 * <p>
	 * The environment variables include settings for:
	 * <ul>
	 *   <li>Maven build configuration</li>
	 *   <li>Artemis broker connection details (host, port, credentials)</li>
	 *   <li>SSL/TLS truststore location and password</li>
	 *   <li>JMS broker configuration (disabling embedded broker)</li>
	 * </ul>
	 * </p>
	 *
	 * @return an unmodifiable list of environment variables
	 */
	@Override
	public List<EnvVar> getEnvVars() {
		return Collections.unmodifiableList(environmentVariables);
	}
}
