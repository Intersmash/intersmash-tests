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
package org.jboss.intersmash.tests.wildfly.web.cache.offload.infinispan;

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;

/**
 * Application descriptor for a WildFly/JBoss EAP application that externalizes distributed HTTP sessions to a remote
 * Infinispan/Red Hat Data Grid cluster using the WildFly Infinispan subsystem and the Hot Rod protocol.
 *
 * <p>The application is provisioned via the WildFly/JBoss EAP S2I image and connects to the Infinispan service
 * deployed by {@link Infinispan2ReplicasCustomCertificateService} using mutual TLS (mTLS).</p>
 *
 * <h3>TLS Configuration</h3>
 * <p>A single Kubernetes secret ({@code wildfly-custom-tls-secret}) is created containing both:</p>
 * <ul>
 *   <li>{@code keystore.pkcs12} — the WildFly client's PKCS12 keystore (private key + certificate), presented to the
 *       Infinispan server during the mTLS handshake</li>
 *   <li>{@code truststore.pkcs12} — a PKCS12 trust store containing the Infinispan server's certificate, used by the
 *       Hot Rod client to verify the server's identity</li>
 * </ul>
 * <p>The secret is mounted at {@code /etc/secrets} and referenced via environment variables that configure the
 * Hot Rod client's TLS settings
 * (see <a href="https://infinispan.org/docs/stable/apidocs/org/infinispan/client/hotrod/configuration/package-summary.html">
 * Hot Rod client configuration</a>).</p>
 *
 * <h3>Environment Variables</h3>
 * <p>Key environment variables passed to the WildFly container:</p>
 * <ul>
 *   <li>{@code INFINISPAN_HOST} / {@code INFINISPAN_PORT} — connection coordinates for the Infinispan service</li>
 *   <li>{@code CACHE_USERNAME} / {@code CACHE_PASSWORD} — Infinispan authentication credentials</li>
 *   <li>{@code KEY_STORE_FILE_NAME}, {@code KEY_STORE_PASSWORD}, {@code KEY_STORE_TYPE}, {@code KEY_ALIAS} — Hot Rod
 *       client keystore (for mTLS client certificate)</li>
 *   <li>{@code TRUST_STORE_FILE_NAME}, {@code TRUST_STORE_PASSWORD}, {@code TRUST_STORE_TYPE} — Hot Rod client
 *       trust store (for verifying the Infinispan server certificate)</li>
 *   <li>{@code KUBERNETES_NAMESPACE} — required for KUBE_PING-based JGroups clustering</li>
 * </ul>
 *
 * @see Infinispan2ReplicasCustomCertificateService
 * @see SimpleCommandLineBasedKeystoreGenerator
 */
public class WildflyExternalizeSessionsToInfinispanApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {
	public static final String WILDFLY_APP_NAME = "wildfly";
	private static final String WILDFLY_SECRETS_DIR_NAME = "/etc/secrets";
	public static final String WILDFLY_CERTIFICATE_NAME = WILDFLY_APP_NAME;
	public static final String WILDFLY_KEYSTORE_PASSWORD = "s3cr3t!passwd";
	private static final String CUSTOM_TLS_SECRET_NAME = String.format("%s-custom-tls-secret", WILDFLY_APP_NAME);

	private final BuildInput buildInput;
	private final List<EnvVar> environmentVariables;
	private final List<Secret> secrets = new ArrayList<>();

	/**
	 * Creates the WildFly application descriptor with mTLS-enabled Hot Rod client configuration.
	 *
	 * <p>Generates self-signed certificates for both the WildFly client and the Infinispan server using
	 * {@link SimpleCommandLineBasedKeystoreGenerator}. The generated certificates are cached on disk and reused
	 * across invocations (see {@link SimpleCommandLineBasedKeystoreGenerator#generateCertificate}).</p>
	 *
	 * <p>The same certificate generation calls must be made in
	 * {@link Infinispan2ReplicasCustomCertificateService} so that both sides share the same cached
	 * certificates and can establish mutual trust.</p>
	 *
	 * @throws IOException if the keystore or trust store files cannot be read
	 */
	public WildflyExternalizeSessionsToInfinispanApplication() throws IOException {
		final String applicationDir = "wildfly/distributed-sessions-infinispan";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		environmentVariables = new ArrayList<>();

		// Infinispan connection coordinates: the Hot Rod client connects to the Kubernetes service
		// by name (INFINISPAN_HOST) on the port exposed by the service (INFINISPAN_PORT)
		environmentVariables.add(new EnvVarBuilder().withName("APP_NAME")
				.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_APP_NAME).build());
		environmentVariables
				.add(new EnvVarBuilder().withName("INFINISPAN_HOST")
						.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_APP_NAME).build());
		environmentVariables
				.add(new EnvVarBuilder().withName("INFINISPAN_PORT").withValue("$(INFINISPAN_SERVICE_PORT)").build());

		// KUBE_PING requires the namespace to discover cluster members via the Kubernetes API;
		// an invalidation-cache requires a functioning JGroups cluster
		environmentVariables.add(new EnvVarBuilder().withName("KUBERNETES_NAMESPACE")
				.withValue(OpenShifts.master().getNamespace()).build());

		// Infinispan authentication credentials (must match the identities.yaml in the Infinispan service)
		environmentVariables.add(new EnvVarBuilder().withName("CACHE_USERNAME")
				.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_CUSTOM_CREDENTIALS_USERNAME).build());
		environmentVariables.add(new EnvVarBuilder().withName("CACHE_PASSWORD")
				.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD).build());

		// Generate (or reuse cached) WildFly client certificate — the keystore from this certificate
		// will be used by the Hot Rod client to present its identity during the mTLS handshake
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo wildflyCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(WILDFLY_APP_NAME),
						WILDFLY_CERTIFICATE_NAME,
						WILDFLY_KEYSTORE_PASSWORD, null,
						Collections.emptyList());

		// Generate (or reuse cached) Infinispan server certificate — the trust store from this certificate
		// will be used by the Hot Rod client to verify the Infinispan server's identity
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo infinispanCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(Infinispan2ReplicasCustomCertificateService.INFINISPAN_APP_NAME),
						Infinispan2ReplicasCustomCertificateService.KEYALIAS,
						Infinispan2ReplicasCustomCertificateService.STOREPASS, null,
						Collections.emptyList());

		// Kubernetes secret containing both the WildFly client keystore and the Infinispan trust store,
		// mounted at WILDFLY_SECRETS_DIR_NAME (/etc/secrets) in the WildFly container
		Secret customTlsSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(CUSTOM_TLS_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", WILDFLY_APP_NAME))
				.endMetadata()
				.addToData(Map.of("keystore.pkcs12",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(wildflyCertificate.keystore.toFile()))))
				.addToData(Map.of("truststore.pkcs12",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(infinispanCertificate.truststore.toFile()))))
				.build();
		secrets.add(customTlsSecret);

		// Hot Rod client keystore configuration — used for the mTLS client certificate
		environmentVariables.add(new EnvVarBuilder().withName("KEY_STORE_FILE_NAME")
				.withValue(String.format("%s/%s", WILDFLY_SECRETS_DIR_NAME, "keystore.pkcs12"))
				.build());
		environmentVariables.add(new EnvVarBuilder().withName("KEY_STORE_PASSWORD")
				.withValue(WILDFLY_KEYSTORE_PASSWORD).build());
		environmentVariables.add(new EnvVarBuilder().withName("KEY_STORE_TYPE")
				.withValue("PKCS12").build());
		environmentVariables.add(new EnvVarBuilder().withName("KEY_ALIAS")
				.withValue(WILDFLY_CERTIFICATE_NAME).build());

		// Hot Rod client trust store configuration — used to verify the Infinispan server certificate
		environmentVariables.add(new EnvVarBuilder().withName("TRUST_STORE_FILE_NAME")
				.withValue(String.format("%s/%s", WILDFLY_SECRETS_DIR_NAME, "truststore.pkcs12"))
				.build());
		environmentVariables.add(new EnvVarBuilder().withName("TRUST_STORE_PASSWORD")
				.withValue(Infinispan2ReplicasCustomCertificateService.STOREPASS).build());
		environmentVariables.add(new EnvVarBuilder().withName("TRUST_STORE_TYPE")
				.withValue("PKCS12").build());

		// S2I build configuration
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
	}

	@Override
	public String getName() {
		return WILDFLY_APP_NAME;
	}

	@Override
	public BuildInput getBuildInput() {
		return buildInput;
	}

	@Override
	public List<EnvVar> getEnvVars() {
		return Collections.unmodifiableList(environmentVariables);
	}

	@Override
	public List<Secret> getSecrets() {
		return Collections.unmodifiableList(secrets);
	}
}
