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
package org.jboss.intersmash.provision.helm.wildfly;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jboss.intersmash.model.helm.charts.values.wildfly.Build;
import org.jboss.intersmash.model.helm.charts.values.wildfly.Deploy;
import org.jboss.intersmash.model.helm.charts.values.wildfly.Env;
import org.jboss.intersmash.model.helm.charts.values.wildfly.Env__1;
import org.jboss.intersmash.model.helm.charts.values.wildfly.HelmWildflyRelease;

/**
 * TODO: This class is just a temporary patch while waiting for https://github.com/Intersmash/intersmash/pull/341 to be merged and a new version of https://github.com/Intersmash/intersmash released: REMOVE AS SOON AS THE NEW LIBRARY VERSION IS AVAILABLE!!!
 *
 */
public class WildFlyHelmChartReleaseAdapterPatched extends WildFlyHelmChartReleaseAdapter {
	public WildFlyHelmChartReleaseAdapterPatched(@NonNull HelmWildflyRelease release) {
		super(release);
	}

	@Override
	public void setBuildEnvironmentVariables(Map<String, String> buildEnvironmentVariables) {
		if (adaptee.getBuild() == null) {
			adaptee.setBuild(new Build());
		}
		if ((adaptee.getBuild().getEnv() == null) || adaptee.getBuild().getEnv().isEmpty()) {
			adaptee.getBuild().setEnv(new ArrayList<>());
		} else {
			adaptee.getBuild().getEnv().clear();
		}
		// let's check just here, so there's a way to clear, i.e. by passing an empty or null list
		if ((buildEnvironmentVariables != null) && !buildEnvironmentVariables.isEmpty()) {
			buildEnvironmentVariables.entrySet().stream()
					.map(e -> adaptee.getBuild().getEnv().add(new Env().withName(e.getKey()).withValue(e.getValue())))
					.collect(Collectors.toList());
		}
	}

	@Override
	public void setDeploymentEnvironmentVariables(Map<String, String> deploymentEnvironmentVariables) {
		if (adaptee.getDeploy() == null) {
			adaptee.setDeploy(new Deploy());
		}
		if ((adaptee.getDeploy().getEnv() == null) || adaptee.getDeploy().getEnv().isEmpty()) {
			adaptee.getDeploy().setEnv(new ArrayList<>());
		} else {
			adaptee.getDeploy().getEnv().clear();
		}
		// let's check just here, so there's a way to clear, i.e. by passing an empty or null list
		if ((deploymentEnvironmentVariables != null) && !deploymentEnvironmentVariables.isEmpty()) {
			deploymentEnvironmentVariables.entrySet().stream()
					.map(e -> adaptee.getDeploy().getEnv().add(new Env__1().withName(e.getKey()).withValue(e.getValue())))
					.collect(Collectors.toList());
		}
	}
}
