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
package org.jboss.intersmash.tests.wildfly.util;

import org.jboss.intersmash.model.helm.charts.values.eap81.HelmEap81Release;
import org.jboss.intersmash.model.helm.charts.values.wildfly.HelmWildflyRelease;
import org.jboss.intersmash.model.helm.charts.values.xp6.HelmXp6Release;
import org.jboss.intersmash.provision.helm.wildfly.WildFlyHelmChartReleaseAdapterPatched;
import org.jboss.intersmash.provision.helm.wildfly.WildflyHelmChartRelease;
import org.jboss.intersmash.provision.helm.wildfly.eap81.Eap81HelmChartReleaseAdapter;
import org.jboss.intersmash.provision.helm.wildfly.xp6.EapXp6HelmChartReleaseAdapter;
import org.jboss.intersmash.tests.junit.annotations.TargetReleaseSensitive;

/**
 * Configuration interface for WildFly Helm chart releases.
 * <p>
 * Provides utility methods to determine the target WildFly distribution and create
 * the appropriate Helm chart release adapter based on the distribution type.
 */
public interface WildFlyHelmChartsConfiguration {

	/**
	 * Retrieves the WildFly target distribution from system properties.
	 * <p>
	 * The distribution type is determined by the {@code wildfly-target-distribution}
	 * system property. If not set, defaults to {@code "wildfly"}.
	 *
	 * @return the WildFly target distribution name (e.g., "wildfly", "jboss-eap", "jboss-eap-xp")
	 */
	default String getWildflyTargetDistribution() {
		return System.getProperty("wildfly-target-distribution", "wildfly");
	}

	/**
	 * Creates and returns the appropriate Helm chart release adapter based on the target distribution.
	 * <p>
	 * The method selects the release adapter based on the distribution type:
	 * <ul>
	 *   <li>For distributions starting with "jboss-eap-xp": returns {@link EapXp6HelmChartReleaseAdapter}</li>
	 *   <li>For distributions starting with "jboss-eap": returns {@link Eap81HelmChartReleaseAdapter}</li>
	 *   <li>For all other distributions (default "wildfly"): returns {@link WildFlyHelmChartReleaseAdapterPatched}</li>
	 * </ul>
	 *
	 * @return the Helm chart release adapter for the configured WildFly distribution
	 */
	@TargetReleaseSensitive
	default WildflyHelmChartRelease getHelmChartRelease() {
		String targetDistribution = getWildflyTargetDistribution();
		if (targetDistribution.startsWith("jboss-eap-xp")) {
			return new EapXp6HelmChartReleaseAdapter(new HelmXp6Release());
		} else if (targetDistribution.startsWith("jboss-eap")) {
			return new Eap81HelmChartReleaseAdapter(new HelmEap81Release());
		} else {
			return new WildFlyHelmChartReleaseAdapterPatched(new HelmWildflyRelease());
		}
	}
}
