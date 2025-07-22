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
package org.jboss.intersmash.tests.wildfly;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

/**
 * Verifies {@link WildflyApplicationConfiguration} functionality when this is expected to be used to run a
 * JBoss EAP s2i build, based on documented WildFly application build profiles.
 */
@ExtendWith(SystemStubsExtension.class)
public class EapApplicationBuildProfilesTest {
	@SystemStub
	private SystemProperties systemProperties;

	/**
	 * Verifies that the correct properties are generated to be passed to an OpenShift s2i build which should produce
	 * a JBoss EAP application.
	 */
	@Test
	void generatedMavenArgsIncludeValidProfiles() {
		// Arrange
		systemProperties.set("wildfly-target-distribution", "jboss-eap");

		// Act
		WildflyApplicationConfiguration app = new WildflyApplicationConfiguration() {
		};
		final String mavenArgs = app.generateAdditionalMavenArgs();

		// Assert
		Assertions.assertTrue(mavenArgs.contains(" -Pwildfly-target-distribution.jboss-eap"));
	}
}
