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

import com.google.common.base.Strings;
import cz.xtf.builder.builders.SecretBuilder;
import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;
import org.jboss.intersmash.util.CommandLineBasedKeystoreGenerator;

/**
 * Set up a WildFly/JBoss EAP 8 application that starts a configured server, which configures Infinispan distributed
 * timers on a remote Infinispan cluster.
 */
public class WildFlyDistributedTimersApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	public static final String NAME = "distributed-ejb-timers";
	private final BuildInput buildInput;
	final String applicationDir = "wildfly/distributed-timers-infinispan";
	private final List<EnvVar> environmentVariables = new ArrayList<>();
	private final List<Secret> secrets = new ArrayList<>();

	public WildFlyDistributedTimersApplication() throws IOException {

		final String truststorePassword = CommandLineBasedKeystoreGenerator.getPassword();
		// Set up client OpenShift secret for SSL
		secrets.add(new SecretBuilder("infinispan-client-secret")
				.addData("truststore.jks", CommandLineBasedKeystoreGenerator.getTruststore())
				.addRawData("trustStorePassword", truststorePassword)
				.build());

		// Set the build input
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		// configure KUBE_PING, an invalidation-cache requires a functioning JGroups cluster.
		environmentVariables.add(new EnvVarBuilder()
				.withName("KUBERNETES_NAMESPACE")
				.withValue(OpenShifts.master().getNamespace())
				.build());

		// set up environment variables
		environmentVariables.add(new EnvVarBuilder()
				.withName("JDG_HOST")
				.withValue("$(RHDG_SERVICE_HOST)")
				.build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("JDG_PORT")
				.withValue("$(RHDG_SERVICE_PORT)")
				.build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("TRUSTSTORE_PASSWORD")
				.withValue(truststorePassword)
				.build());

		// credentials from Infinispan/Red Hat Data Grid custom secret
		environmentVariables.add(new EnvVarBuilder()
				.withName("CACHE_USERNAME")
				.withValue(InfinispanOperatorWithExternalRouteApplication.INFINISPAN_CUSTOM_CREDENTIALS_USERNAME)
				.build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("CACHE_PASSWORD")
				.withValue(InfinispanOperatorWithExternalRouteApplication.INFINISPAN_CUSTOM_CREDENTIALS_PASSWORD)
				.build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("TIMER_EXPIRATION_API_BASE_URL")
				.withValue("http://timer-expiration-store:8080").build());

		// More env vars
		// TODO: this appears in many application descriptors that implement WildflyImageOpenShiftApplication and
		//  WildflyApplicationConfiguration, therefore it might be refactored into a unique interface method
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

		environmentVariables.add(new EnvVarBuilder()
				.withName("SCRIPT_DEBUG").withValue(IntersmashConfig.scriptDebug()).build());
	}

	@Override
	public String getName() {
		return NAME;
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
		return secrets;
	}
}
