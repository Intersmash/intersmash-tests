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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.intersmash.IntersmashConfig;
import org.jboss.intersmash.model.helm.charts.values.eap82.HelmEap82Release;
import org.jboss.intersmash.model.helm.charts.values.wildfly.HelmWildflyRelease;
import org.jboss.intersmash.model.helm.charts.values.xp6.HelmXp6Release;
import org.jboss.intersmash.provision.helm.wildfly.WildFlyHelmChartReleaseAdapterPatched;
import org.jboss.intersmash.provision.helm.wildfly.WildflyHelmChartRelease;
import org.jboss.intersmash.provision.helm.wildfly.eap82.Eap82HelmChartReleaseAdapter;
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
	 *   <li>For distributions starting with "jboss-eap": returns {@link Eap82HelmChartReleaseAdapter}</li>
	 *   <li>For all other distributions (default "wildfly"): returns {@link WildFlyHelmChartReleaseAdapterPatched}</li>
	 * </ul>
	 *
	 * @return the Helm chart release adapter for the configured WildFly distribution
	 */
	Pattern OPENJDK_PATTERN = Pattern.compile("jdk(\\d+)");

	/**
	 * Detects the JDK version from the builder image URL (e.g. "openjdk21" in the URL yields "21").
	 * Falls back to {@link IntersmashConfig#wildflyImageJdk()} if the pattern is not found.
	 */
	default WildflyHelmChartRelease.JdkImage.Version getJdkImageVersion() {
		String imageUrl = IntersmashConfig.wildflyImageURL();
		if (imageUrl != null) {
			Matcher matcher = OPENJDK_PATTERN.matcher(imageUrl);
			if (matcher.find()) {
				return WildflyHelmChartRelease.JdkImage.Version.fromValue(matcher.group(1));
			}
		}
		return WildflyHelmChartRelease.JdkImage.Version.fromValue(IntersmashConfig.wildflyImageJdk());
	}

	@TargetReleaseSensitive
	default WildflyHelmChartRelease getHelmChartRelease() {
		String targetDistribution = getWildflyTargetDistribution();
		if (targetDistribution.startsWith("jboss-eap-xp")) {
			return new EapXp6HelmChartReleaseAdapter(new HelmXp6Release());
		} else if (targetDistribution.startsWith("jboss-eap")) {
			String chartsName = IntersmashConfig.getWildflyHelmChartsName();
			return new Eap82HelmChartReleaseAdapter(new HelmEap82Release());
		} else {
			return new WildFlyHelmChartReleaseAdapterPatched(new HelmWildflyRelease());
		}
	}
}
