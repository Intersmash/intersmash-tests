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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import org.apache.commons.io.IOUtils;

/**
 * Utility class for Infinispan secret-related operations.
 */
public class InfinispanSecretUtils {

	/**
	 * Encodes the contents of an InputStream to a Base64-encoded string.
	 *
	 * @param is the InputStream to encode
	 * @return Base64-encoded string of the input stream contents
	 * @throws IllegalArgumentException if the input stream cannot be processed
	 */
	public static String getEncodedIdentitiesSecretContents(InputStream is) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			IOUtils.copy(is, os);
			return Base64.getEncoder().encodeToString(os.toByteArray());
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not process data stream", e);
		}
	}
}
