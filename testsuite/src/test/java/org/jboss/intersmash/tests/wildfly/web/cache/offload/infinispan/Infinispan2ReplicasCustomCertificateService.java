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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.infinispan.v1.Infinispan;
import org.infinispan.v1.InfinispanBuilder;
import org.infinispan.v1.infinispanspec.security.EndpointEncryption;
import org.infinispan.v1.infinispanspec.security.EndpointEncryptionBuilder;
import org.infinispan.v2alpha1.Cache;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.InfinispanOperatorApplication;
import org.jboss.intersmash.tests.infinispan.Infinispan2ReplicasService;
import org.jboss.intersmash.tests.infinispan.InfinispanSecretUtils;
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;

/**
 * Application descriptor that represents an Infinispan/Red Hat Data Grid service deployed by the related Operator,
 * configured with two replicas and mutual TLS (mTLS) encryption using custom certificates.
 *
 * <p>This class differs from {@link Infinispan2ReplicasService} in that it provides custom TLS certificates
 * for securing the Infinispan endpoint, rather than relying on operator-generated certificates.</p>
 *
 * <h3>TLS Configuration</h3>
 * <p>The endpoint encryption is configured with two Kubernetes secrets
 * (see <a href="https://infinispan.org/docs/infinispan-operator/main/operator.html">Infinispan Operator Guide</a>):</p>
 * <ul>
 *   <li><b>Keystore secret</b> ({@code infinispan-custom-tls-secret}) — contains the Infinispan server's PKCS12 keystore
 *       ({@code keystore.p12}), along with the key {@code alias} and {@code password}; referenced by
 *       {@code spec.security.endpointEncryption.certSecretName} in the Infinispan CR.</li>
 *   <li><b>Client certificate trust store secret</b> ({@code infinispan-custom-client-cert-secret}) — contains a PKCS12
 *       trust store ({@code truststore.p12}) holding the WildFly client's certificate and the {@code truststore-password};
 *       referenced by {@code spec.security.endpointEncryption.clientCertSecretName} in the Infinispan CR and used by the
 *       Infinispan server to validate client certificates.</li>
 * </ul>
 *
 * <h3>Client Certificate Validation</h3>
 * <p>The {@code clientCert} field is set to {@code Validate}, meaning the Infinispan server requires connecting
 * clients (e.g. the WildFly Hot Rod client) to present a valid certificate that can be verified against the
 * trust store.</p>
 *
 * <h3>Custom Credentials</h3>
 * <p>In addition to TLS, custom Infinispan user credentials (username/password) are configured via a dedicated
 * secret, loaded from the {@code identities.yaml} classpath resource.</p>
 *
 * @see WildflyExternalizeSessionsToInfinispanApplication
 * @see SimpleCommandLineBasedKeystoreGenerator
 */
public class Infinispan2ReplicasCustomCertificateService implements InfinispanOperatorApplication, OpenShiftApplication {
	public static final String INFINISPAN_APP_NAME = "infinispan";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_USERNAME = "foo";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD = "bar";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME = "infinispan-custom-credentials-secret";
	private static final Secret INFINISPAN_CUSTOM_CREDENTIALS_SECRET = buildInfinispanCustomCredentialsSecret();
	public static final String TLS_KEYSTORE_SECRET_NAME = String.format("%s-custom-tls-secret", INFINISPAN_APP_NAME);
	public static final String TLS_TRUSTSTORE_SECRET_NAME = String.format("%s-custom-client-cert-secret", INFINISPAN_APP_NAME);
	public static final String STOREPASS = "s3cr3t!passwd";
	public static final String KEYALIAS = "server";

	protected Infinispan infinispan;
	protected final List<Secret> secrets = new ArrayList<>();

	/**
	 * Builds the Kubernetes secret containing Infinispan user credentials from the {@code identities.yaml}
	 * classpath resource.
	 *
	 * @return a {@link Secret} with the encoded identities data
	 * @throws UnsupportedOperationException if the {@code identities.yaml} resource cannot be read
	 */
	private static Secret buildInfinispanCustomCredentialsSecret() {
		try (InputStream is = Infinispan2ReplicasCustomCertificateService.class.getResourceAsStream("identities.yaml")) {
			return new SecretBuilder()
					.withNewMetadata().withName(INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME).endMetadata()
					.withData(Map.of("identities.yaml", InfinispanSecretUtils.getEncodedIdentitiesSecretContents(is)))
					.build();
		} catch (IOException e) {
			throw new UnsupportedOperationException("Cannot create a custom credentials secret from identities.yaml");
		}
	}

	/**
	 * Creates the Infinispan service descriptor with mTLS encryption and custom credentials.
	 *
	 * <p>Generates self-signed certificates for both the WildFly client and the Infinispan server using
	 * {@link SimpleCommandLineBasedKeystoreGenerator}. The generated certificates are cached on disk and reused
	 * across invocations (see {@link SimpleCommandLineBasedKeystoreGenerator#generateCertificate}).</p>
	 *
	 * <p>The same certificate generation calls must be made in
	 * {@link WildflyExternalizeSessionsToInfinispanApplication} so that both sides share the same cached
	 * certificates and can establish mutual trust.</p>
	 *
	 * @throws IOException if the keystore or trust store files cannot be read
	 */
	public Infinispan2ReplicasCustomCertificateService() throws IOException {
		// Generate (or reuse cached) WildFly client certificate — the trust store from this certificate
		// will be loaded into the Infinispan server so it can validate the WildFly Hot Rod client certificate
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo wildflyCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master()
								.generateHostname(WildflyExternalizeSessionsToInfinispanApplication.WILDFLY_APP_NAME),
						WildflyExternalizeSessionsToInfinispanApplication.WILDFLY_CERTIFICATE_NAME,
						WildflyExternalizeSessionsToInfinispanApplication.WILDFLY_KEYSTORE_PASSWORD, null,
						Collections.emptyList());

		// Generate (or reuse cached) Infinispan server certificate — the keystore from this certificate
		// will be used as the Infinispan server's TLS identity
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo infinispanCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(INFINISPAN_APP_NAME),
						KEYALIAS,
						STOREPASS, null,
						Collections.emptyList());

		// Infinispan server keystore secret: contains the server's private key and certificate.
		// The Infinispan operator expects: "keystore.p12" (PKCS12 data), "alias", and "password".
		// See https://infinispan.org/docs/infinispan-operator/main/operator.html
		Secret tlsKeyStoreSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(TLS_KEYSTORE_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", INFINISPAN_APP_NAME))
				.endMetadata()
				.addToStringData("alias", KEYALIAS)
				.addToStringData("password", STOREPASS)
				.addToData(Map.of("keystore.p12",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(infinispanCertificate.keystore.toFile()))))
				.build();
		secrets.add(tlsKeyStoreSecret);

		// Client certificate trust store secret: contains the WildFly client's certificate so the Infinispan
		// server can validate it during the mTLS handshake.
		// The Infinispan operator expects: "truststore.p12" (PKCS12 data) and "truststore-password".
		// See https://infinispan.org/docs/infinispan-operator/main/operator.html
		Secret tlsTrustStoreSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(TLS_TRUSTSTORE_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", INFINISPAN_APP_NAME))
				.endMetadata()
				.addToStringData("truststore-password",
						WildflyExternalizeSessionsToInfinispanApplication.WILDFLY_KEYSTORE_PASSWORD)
				.addToData(Map.of("truststore.p12",
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(wildflyCertificate.truststore.toFile()))))
				.build();
		secrets.add(tlsTrustStoreSecret);

		secrets.add(INFINISPAN_CUSTOM_CREDENTIALS_SECRET);

		// Build the Infinispan CR with 2 replicas, mTLS endpoint encryption, and custom credentials
		infinispan = new InfinispanBuilder()
				.withNewMetadata().withName(getName()).withLabels(Map.of("app", "datagrid")).endMetadata()
				.withNewSpec()
				.withReplicas(2)
				.withNewSecurity()
				.withEndpointEncryption(
						new EndpointEncryptionBuilder()
								// Use a custom secret (not operator-managed certificates)
								.withType(EndpointEncryption.Type.Secret)
								// Secret containing the Infinispan server's keystore
								.withCertSecretName(TLS_KEYSTORE_SECRET_NAME)
								// Require and validate client certificates (mTLS)
								.withClientCert(EndpointEncryption.ClientCert.valueOf("Validate"))
								// Secret containing the trust store for validating client certificates
								.withClientCertSecretName(TLS_TRUSTSTORE_SECRET_NAME)
								.build())
				.withEndpointSecretName(INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME)
				.endSecurity()
				.endSpec()
				.build();
	}

	@Override
	public String getName() {
		return INFINISPAN_APP_NAME;
	}

	@Override
	public Infinispan getInfinispan() {
		return infinispan;
	}

	@Override
	public List<Cache> getCaches() {
		return Collections.emptyList();
	}

	@Override
	public List<Secret> getSecrets() {
		return secrets;
	}
}
