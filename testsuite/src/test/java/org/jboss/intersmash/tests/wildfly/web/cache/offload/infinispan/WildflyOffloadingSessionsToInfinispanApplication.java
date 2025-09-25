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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.assertj.core.util.Strings;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;

/**
 * Application descriptor which represents an EAP application which will be provisioned via EAP image and that is
 * configured in order to externalize distributed sessions to Infinispan/Red Hat Data Grid using only Infinispan
 * subsystem.
 */
public class WildflyOffloadingSessionsToInfinispanApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {
	public static final String WILDFLY_APP_NAME = "wildfly";

	private final BuildInput buildInput;
	private final List<EnvVar> environmentVariables;

	public WildflyOffloadingSessionsToInfinispanApplication() {

		final String applicationDir = "wildfly/web-cache-offload-infinispan";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		// setup environment variables
		environmentVariables = new ArrayList<>();
		environmentVariables.add(new EnvVarBuilder().withName("APP_NAME").withValue(WILDFLY_APP_NAME).build());
		environmentVariables
				.add(new EnvVarBuilder().withName("INFINISPAN_HOST").withValue("$(INFINISPAN_SERVICE_HOST)").build());
		environmentVariables
				.add(new EnvVarBuilder().withName("INFINISPAN_PORT").withValue("$(INFINISPAN_SERVICE_PORT)").build());
		// configure KUBE_PING, an invalidation-cache requires a functioning jgroups cluster.
		environmentVariables.add(new EnvVarBuilder().withName("KUBERNETES_NAMESPACE")
				.withValue(OpenShifts.master().getNamespace()).build());
		//	credentials from Infinispan/Red Hat Data Grid APP custom secret
		environmentVariables.add(new EnvVarBuilder().withName("CACHE_USERNAME")
				.withValue(Infinispan2ReplicasService.INFINISPAN_CUSTOM_CREDENTIALS_USERNAME).build());
		environmentVariables.add(new EnvVarBuilder().withName("CACHE_PASSWORD")
				.withValue(Infinispan2ReplicasService.INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD).build());

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
}
