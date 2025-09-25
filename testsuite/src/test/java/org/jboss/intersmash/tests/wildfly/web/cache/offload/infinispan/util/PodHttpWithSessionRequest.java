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
package org.jboss.intersmash.tests.wildfly.web.cache.offload.infinispan.util;

import cz.xtf.core.openshift.OpenShifts;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.MediaType;

/**
 * Represents an HTTP request originating by a specific {@link Pod} and with a given session {@code JSESSIONID}.
 */
public class PodHttpWithSessionRequest {

	private final OpenShiftBinaryClient oc;
	private String jSessionID;

	public PodHttpWithSessionRequest(final String jSessionID) {
		this.jSessionID = jSessionID;
		oc = OpenShiftBinaryClient.getInstance();
		oc.project(OpenShifts.master().getNamespace());
	}

	/**
	 * Executes an HTTP GET request
	 * @param pod The {@link Pod} instance that sends the HTTP request
	 * @param url The request URL
	 * @param accept The {@link MediaType} value for the request {@code Accept} header.
	 * @return The response body.
	 */
	public String get(Pod pod, String url, String accept) {
		return request(pod, url, "GET", accept);
	}

	/**
	 * Executes an HTTP GET request
	 * @param pod The {@link Pod} instance that sends the HTTP request
	 * @param url The request URL
	 * @return The response body.
	 */
	public String get(Pod pod, String url) {
		return request(pod, url, "GET", MediaType.TEXT_PLAIN);
	}

	/**
	 * Executes an HTTP PUT request
	 * @param pod The {@link Pod} instance that sends the HTTP request
	 * @param url The request URL
	 * @return The response body.
	 */
	public String put(Pod pod, String url) {
		return request(pod, url, "PUT", null);
	}

	private String request(Pod pod, String url, String method, String accept) {
		final List<String> args = new ArrayList<>(9);
		args.addAll(Arrays.asList("curl", "--silent", url, "-X", method, "--cookie", "JSESSIONID=" + jSessionID));
		if (accept != null) {
			args.add("-H");
			args.add("Accept:" + accept);
		}
		return exec(pod, args.toArray(new String[0]));
	}

	private String exec(Pod pod, String... command) {
		final List<String> args = new ArrayList<>();
		args.add("exec");
		args.add(pod.getMetadata().getName());
		args.add("--");
		args.addAll(Arrays.asList(command));

		return oc.executeCommandWithReturn("remote execution has failed", args.toArray(new String[0]));
	}
}
