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

public class WildFlyHelmChartsConfiguration {

	public static String getWildflyTargetDistribution() {
		return System.getProperty("wildfly-target-distribution", "wildfly");
	}

	@TargetReleaseSensitive
	public static WildflyHelmChartRelease getHelmChartRelease() {
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
