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
package org.jboss.intersmash.tests.infinispan;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.infinispan.v1.Infinispan;
import org.infinispan.v1.InfinispanBuilder;
import org.infinispan.v2alpha1.Cache;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.application.operator.InfinispanOperatorApplication;

/**
 * Application descriptor that represents an Infinispan/Red Hat Data Grid service deployed by the related Operator, and
 * having two replicas.
 */
public class Infinispan2ReplicasService implements InfinispanOperatorApplication, OpenShiftApplication {
	public static final String INFINISPAN_APP_NAME = "infinispan";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_USERNAME = "foo";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD = "bar";
	public static final String INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME = "infinispan-custom-credentials-secret";
	private static final Secret INFINISPAN_CUSTOM_CREDENTIALS_SECRET = buildInfinispanCustomCredentialsSecret();
	public static final String TLS_SECRET_NAME = "tls-secret";

	protected Infinispan infinispan;
	protected final List<Secret> secrets = new ArrayList<>();

	private static Secret buildInfinispanCustomCredentialsSecret() {
		try (InputStream is = Infinispan2ReplicasService.class.getResourceAsStream("identities.yaml")) {
			return new SecretBuilder()
					.withNewMetadata().withName(INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME).endMetadata()
					.withData(Map.of("identities.yaml", InfinispanSecretUtils.getEncodedIdentitiesSecretContents(is)))
					.build();
		} catch (IOException e) {
			throw new UnsupportedOperationException("Cannot create a custom credentials secret from identities.yaml");
		}
	}

	public Infinispan2ReplicasService() {
		secrets.add(INFINISPAN_CUSTOM_CREDENTIALS_SECRET);
		infinispan = new InfinispanBuilder()
				.withNewMetadata().withName(getName()).withLabels(Map.of("app", "datagrid")).endMetadata()
				.withNewSpec()
				.withReplicas(2)
				.withNewSecurity().withEndpointSecretName(INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME).endSecurity()
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
