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
 * Application descriptor which represents a WildFly/JBoss EAP application which will be provisioned via WildFly/JBoss EAP image and that is
 * configured in order to externalize distributed sessions to Infinispan/Red Hat Data Grid using only Infinispan
 * subsystem.
 */
public class WildflyExternalizeSessionsToInfinispanApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {
	public static final String WILDFLY_APP_NAME = "wildfly";
	private static final String TLS_CERTIFICATE_DIR_NAME = "/etc/secrets";

	private final BuildInput buildInput;
	private final List<EnvVar> environmentVariables;
	private final List<Secret> secrets = new ArrayList<>();

	public WildflyExternalizeSessionsToInfinispanApplication() throws IOException {
		final String applicationDir = "wildfly/distributed-sessions-infinispan";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		// setup environment variables
		environmentVariables = new ArrayList<>();
		environmentVariables.add(new EnvVarBuilder().withName("APP_NAME")
				.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_APP_NAME).build());
		environmentVariables
				.add(new EnvVarBuilder().withName("INFINISPAN_HOST")
						.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_APP_NAME).build());
		environmentVariables
				.add(new EnvVarBuilder().withName("INFINISPAN_PORT").withValue("$(INFINISPAN_SERVICE_PORT)").build());
		// configure KUBE_PING, an invalidation-cache requires a functioning jgroups cluster.
		environmentVariables.add(new EnvVarBuilder().withName("KUBERNETES_NAMESPACE")
				.withValue(OpenShifts.master().getNamespace()).build());
		//	credentials from Infinispan/Red Hat Data Grid APP custom secret
		environmentVariables.add(new EnvVarBuilder().withName("CACHE_USERNAME")
				.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_CUSTOM_CREDENTIALS_USERNAME).build());
		environmentVariables.add(new EnvVarBuilder().withName("CACHE_PASSWORD")
				.withValue(Infinispan2ReplicasCustomCertificateService.INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD).build());
		environmentVariables.add(new EnvVarBuilder().withName("TRUST_STORE_PATH")
				.withValue(String.format("%s/%s", TLS_CERTIFICATE_DIR_NAME,
						Infinispan2ReplicasCustomCertificateService.TLS_CERTIFICATE_FILE_NAME))
				.build());

		final SimpleCommandLineBasedKeystoreGenerator.CertificateInfo infinispanCertificate = SimpleCommandLineBasedKeystoreGenerator
				.generateCertificate(
						OpenShifts.master().generateHostname(Infinispan2ReplicasCustomCertificateService.INFINISPAN_APP_NAME),
						Infinispan2ReplicasCustomCertificateService.KEYALIAS,
						Infinispan2ReplicasCustomCertificateService.STOREPASS,
						Collections.emptyList());
		Secret tlsCertificateSecret = new SecretBuilder()
				.withNewMetadata()
				.withName(Infinispan2ReplicasCustomCertificateService.TLS_CERTIFICATE_SECRET_NAME)
				.withLabels(Collections.singletonMap("app", Infinispan2ReplicasCustomCertificateService.INFINISPAN_APP_NAME))
				.endMetadata()
				.addToData(Map.of(Infinispan2ReplicasCustomCertificateService.TLS_CERTIFICATE_FILE_NAME,
						Base64.getEncoder()
								.encodeToString(FileUtils.readFileToByteArray(infinispanCertificate.certificate.toFile()))))
				.build();
		secrets.add(tlsCertificateSecret);

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
