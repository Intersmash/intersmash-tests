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
package org.jboss.intersmash.tests.wildfly.postgresql;

import com.google.common.base.Strings;
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
 * WildFly application descriptor for the PostgreSQL EJB Timer application.
 * <p>
 * Configures a WildFly application that uses EJB timers backed by a PostgreSQL database.
 * </p>
 */
public class WildflyPostgresqlTimerApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	static final String NAME = "wildfly-postgresql-timer-application";
	final String applicationDir = "wildfly/postgresql-timer-application";
	private final BuildInput buildInput;
	private final List<EnvVar> envVars;

	public WildflyPostgresqlTimerApplication() {
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();

		List<EnvVar> environmentVariables = new ArrayList<>();
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
				.withName("SCRIPT_DEBUG").withValue("true").build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("POSTGRESQL_SERVICE_HOST").withValue(PostgresqlService.POSTGRESQL_NAME).build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("POSTGRESQL_SERVICE_PORT").withValue("5432").build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("POSTGRESQL_DATABASE").withValue(PostgresqlService.POSTGRESQL_DATABASE).build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("POSTGRESQL_USER").withValue(PostgresqlService.POSTGRESQL_USER).build());
		environmentVariables.add(new EnvVarBuilder()
				.withName("POSTGRESQL_PASSWORD").withValue(PostgresqlService.POSTGRESQL_PASSWORD).build());

		this.envVars = Collections.unmodifiableList(environmentVariables);
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
		return envVars;
	}
}
