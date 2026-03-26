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
import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.application.input.BuildInput;
import org.jboss.intersmash.application.input.BuildInputBuilder;
import org.jboss.intersmash.application.openshift.WildflyImageOpenShiftApplication;
import org.jboss.intersmash.tests.wildfly.WildflyApplicationConfiguration;

/**
 * Set up a WildFly/JBoss EAP application that starts a configured server, which holds the timers execution metadata.
 */
public class WildFlyTimerExpirationStoreApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	public static final String NAME = "timer-expiration-store";
	private final BuildInput buildInput;
	final String applicationDir = "wildfly/timer-expiration-store";
	private final List<EnvVar> environmentVariables = new ArrayList<>();

	public WildFlyTimerExpirationStoreApplication() {

		// Set the build input
		buildInput = new BuildInputBuilder()
				// TODO: fix before merging!!! We have two aplications and 2 PRs for this test, but just one property
				.uri("https://github.com/fabiobrz/intersmash-applications.git")
				// TODO: fix before merging!!! We have two aplications and 2 PRs for this test, but just one property
				.ref("issues-88")
				.build();

		// setup environment variables
		environmentVariables.add(
				new EnvVarBuilder()
						.withName("POSTGRESQL_SERVICE_HOST")
						.withValue(PostgresqlTimerExpirationStoreApplication.POSTGRESQL_NAME)
						.build());
		environmentVariables.add(
				new EnvVarBuilder()
						.withName("POSTGRESQL_SERVICE_PORT")
						.withValue("5432")
						.build());
		environmentVariables.add(
				new EnvVarBuilder()
						.withName("POSTGRESQL_DATABASE")
						.withValue(PostgresqlTimerExpirationStoreApplication.POSTGRESQL_DATABASE)
						.build());
		environmentVariables.add(
				new EnvVarBuilder()
						.withName("POSTGRESQL_USER")
						.withValue(PostgresqlTimerExpirationStoreApplication.POSTGRESQL_USER)
						.build());
		environmentVariables.add(
				new EnvVarBuilder()
						.withName("POSTGRESQL_PASSWORD")
						.withValue(PostgresqlTimerExpirationStoreApplication.POSTGRESQL_PASSWORD)
						.build());
		// JGroups clustering needs this
		environmentVariables.add(
				new EnvVarBuilder()
						.withName("KUBERNETES_NAMESPACE")
						.withValue(OpenShifts.master().getNamespace())
						.build());

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
}
