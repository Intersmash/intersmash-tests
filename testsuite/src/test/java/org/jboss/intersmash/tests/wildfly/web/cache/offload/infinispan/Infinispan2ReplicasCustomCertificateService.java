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
import org.jboss.intersmash.tests.wildfly.util.SimpleCommandLineBasedKeystoreGenerator;
import org.jboss.intersmash.tests.wildfly.web.cache.offload.infinispan.util.InfinispanSecretUtils;

/**
 * Application descriptor that represents an Infinispan/Red Hat Data Grid service deployed by the related Operator, and
 * having two replicas; this version, with respect to {@link Infinispan2ReplicasService}, is using a custom certificate
 * to encrypt the communication with the endpoint;
 */
public class Infinispan2ReplicasCustomCertificateService implements InfinispanOperatorApplication, OpenShiftApplication {
	public static final String INFINISPAN_APP_NAME = "infinispan";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_USERNAME = "foo";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD = "bar";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME = "infinispan-custom-credentials-secret";
	private static final Secret INFINISPAN_CUSTOM_CREDENTIALS_SECRET = buildInfinispanCustomCredentialsSecret();
	public static final String TLS_KEYSTORE_SECRET_NAME = String.format("%s-custom-tls-secret", INFINISPAN_APP_NAME);
	public static final String TLS_CERTIFICATE_SECRET_NAME = String.format("%s-custom-cert-secret", INFINISPAN_APP_NAME);
	public static final String TLS_CERTIFICATE_FILE_NAME = String.format("%s.crt", INFINISPAN_APP_NAME);
	public static final String STOREPASS = "s3cr3t!passwd";
	public static final String KEYALIAS = "server";

	protected Infinispan infinispan;
	protected final List<Secret> secrets = new ArrayList<>();

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

	public Infinispan2ReplicasCustomCertificateService() throws IOException {
		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo infinispanCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(INFINISPAN_APP_NAME),
						KEYALIAS,
						STOREPASS,
						Collections.emptyList());

		Secret tlsKeystoreSecret = new SecretBuilder()
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
		secrets.add(tlsKeystoreSecret);
		secrets.add(INFINISPAN_CUSTOM_CREDENTIALS_SECRET);

		infinispan = new InfinispanBuilder()
				.withNewMetadata().withName(getName()).withLabels(Map.of("app", "datagrid")).endMetadata()
				.withNewSpec()
				.withReplicas(2)
				.withNewSecurity()
				// TLS Certificate used to secure the Infinispan Service
				.withEndpointEncryption(
						new EndpointEncryptionBuilder()
								.withType(EndpointEncryption.Type.Secret)
								.withCertSecretName(TLS_KEYSTORE_SECRET_NAME)
								.build())
				// Credentials to access the Infinispan Service (username + password)
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
