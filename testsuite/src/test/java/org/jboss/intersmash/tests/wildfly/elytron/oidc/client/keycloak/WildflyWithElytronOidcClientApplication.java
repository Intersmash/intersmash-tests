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
package org.jboss.intersmash.tests.wildfly.elytron.oidc.client.keycloak;

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
 * WildFly image based OpenShift application descriptor that uses the wildfly-with-elytron-oidc-client
 * deployment.
 *
 *  You can configure the elytron-oidc-client subsystem in three different ways:
 * <ul>
 *     <li>Adding an oidc.json into your deployment.</li>
 *     <li>Running a CLI script to configure the elytron-oidc-client subsystem.</li>
 *     <li>Defining environment variables to configure an elytron-oidc-client subsystem on start of JBoss EAP server on OpenShift.</li>
 * </ul>
 *
 * This class is used to verify the first option
 */
public class WildflyWithElytronOidcClientApplication
		implements WildflyImageOpenShiftApplication, WildflyApplicationConfiguration {

	public static final String APP_NAME = "elytron-oidc-client";

	private final BuildInput buildInput;
	private final List<EnvVar> environmentVariables = new ArrayList<>();

	/**
	 * Creates a new WildFly application with Elytron OIDC client configured using an oidc.json file.
	 */
	public WildflyWithElytronOidcClientApplication() {

		String applicationDir = "wildfly/elytron-oidc-client-keycloak";
		buildInput = new BuildInputBuilder()
				.uri(IntersmashConfig.deploymentsRepositoryUrl())
				.ref(IntersmashConfig.deploymentsRepositoryRef())
				.build();
		environmentVariables.add(new EnvVarBuilder()
				.withName("SSO_APP_SERVICE")
				.withValue(String.format("https://%s", BasicKeycloakOperatorApplication.getRoute()))
				.build());

		// On Keycloak we added a pre-configured OIDC client that can be accessed using this password (see file oidc.json)
		environmentVariables.add(
				new EnvVarBuilder().withName("OIDC_SECURE_DEPLOYMENT_SECRET")
						.withValue(BasicKeycloakOperatorDynamicClientApplication.OIDC_SECURE_DEPLOYMENT_SECRET)
						.build());

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

		environmentVariables.add(
				new EnvVarBuilder().withName("SCRIPT_DEBUG")
						.withValue(IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false")
						.build());
		environmentVariables.add(
				new EnvVarBuilder().withName("LOGGING_SCRIPT_DEBUG")
						.withValue(IntersmashConfig.scriptDebug() != null ? IntersmashConfig.scriptDebug() : "false")
						.build());
	}

	/**
	 * Get the build input configuration for this application.
	 *
	 * @return the build input configuration
	 */
	@Override
	public BuildInput getBuildInput() {
		return buildInput;
	}

	/**
	 * Get the application name.
	 *
	 * @return the application name
	 */
	@Override
	public String getName() {
		return APP_NAME;
	}

	/**
	 * Get the environment variables for this application.
	 *
	 * @return unmodifiable list of environment variables
	 */
	@Override
	public List<EnvVar> getEnvVars() {
		return Collections.unmodifiableList(environmentVariables);
	}

	/**
	 * Get a route to the application.
	 *
	 * @return route to the application
	 */
	public static String getRoute() {
		return OpenShifts.master().generateHostname(APP_NAME);
	}
}
