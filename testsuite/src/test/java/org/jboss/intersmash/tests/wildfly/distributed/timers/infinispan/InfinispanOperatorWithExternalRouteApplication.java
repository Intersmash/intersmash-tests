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
package org.jboss.intersmash.tests.wildfly.distributed.timers.infinispan;

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.infinispan.v1.InfinispanBuilder;
import org.infinispan.v1.infinispanspec.Expose;
import org.infinispan.v1.infinispanspec.security.EndpointEncryption;
import org.infinispan.v1.infinispanspec.security.EndpointEncryptionBuilder;
import org.jboss.intersmash.application.openshift.OpenShiftApplication;
import org.jboss.intersmash.tests.infinispan.Infinispan2ReplicasService;
import org.jboss.intersmash.util.CommandLineBasedKeystoreGenerator;

/**
 * An Infinispan basic service, which is supposed to be provisioned by
 * {@link org.jboss.intersmash.provision.openshift.InfinispanOpenShiftOperatorProvisioner}
 *
 * This class extends {@link @Infinispan2ReplicasService} in order to configure the Infinispan CR with a
 * route that exposes the service and allows for consuming the REST APIs to perform test assertions.
 */
public class InfinispanOperatorWithExternalRouteApplication extends Infinispan2ReplicasService
		implements OpenShiftApplication {

	public InfinispanOperatorWithExternalRouteApplication() throws IOException {

		// Here we're crating a Secret that holds the certificates needed to secure the communication with
		// Infinispan/Red Hat Data Grid service
		final String hostName = OpenShifts.master().generateHostname(INFINISPAN_APP_NAME);
		final CommandLineBasedKeystoreGenerator.GeneratedPaths certPaths = CommandLineBasedKeystoreGenerator
				.generateCerts(hostName);
		Secret tlsSecret = new io.fabric8.kubernetes.api.model.SecretBuilder()
				.withNewMetadata()
				.withName(TLS_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", INFINISPAN_APP_NAME))
				.endMetadata()
				.addToData(Map.of("tls.crt",
						Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(certPaths.certPem.toFile()))))
				.addToData(Map.of("tls.key",
						Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(certPaths.keyPem.toFile()))))
				.build();
		secrets.add(tlsSecret);

		// https://access.redhat.com/documentation/en-us/red_hat_data_grid/8.1/html/running_data_grid_on_openshift/start_operator#minimal_crd-start
		// Override parent class Infinispan instance definition
		infinispan = new InfinispanBuilder()
				.withMetadata(new ObjectMetaBuilder()
						.withName(this.getName())
						.withLabels(Map.of("app", "datagrid"))
						.build())
				.withNewSpec()
				.withReplicas(2)
				.withNewSecurity()
				// The superclass sets the secret for Infinispan/Red Hat Data Grid identities credentials,
				// and we'll reuse it here
				.withEndpointSecretName(INFINISPAN_CUSTOM_CREDENTIALS_SECRET_NAME)
				.withEndpointEncryption(new EndpointEncryptionBuilder()
						.withCertSecretName(TLS_SECRET_NAME)
						.withType(EndpointEncryption.Type.Secret)
						.build())
				.endSecurity()
				.withNewExpose().withType(Expose.Type.Route).endExpose()
				.endSpec().build();
	}
}
