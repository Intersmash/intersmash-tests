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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jboss.intersmash.application.openshift.PostgreSQLTemplateOpenShiftApplication;
import org.jboss.intersmash.application.openshift.template.PostgreSQLTemplate;

/**
 * Deploy the PostgreSQL database using the {@link PostgreSQLTemplate#POSTGRESQL_EPHEMERAL} template to implement
 * the persistence store holding distributed timers expirations.
 * Uses the template default parameters.
 */
public class PostgresqlTimerExpirationStoreApplication implements PostgreSQLTemplateOpenShiftApplication {

	public static final String POSTGRESQL_NAME = "postgresql";
	public static final String POSTGRESQL_DATABASE = "theData";
	public static final String POSTGRESQL_PASSWORD = "thePassword";
	public static final String POSTGRESQL_USER = "theUser";

	private final Map<String, String> parameters = new HashMap<>();

	public PostgresqlTimerExpirationStoreApplication() {
		parameters.put("POSTGRESQL_DATABASE", POSTGRESQL_DATABASE);
		parameters.put("POSTGRESQL_PASSWORD", POSTGRESQL_PASSWORD);
		parameters.put("POSTGRESQL_USER", POSTGRESQL_USER);
	}

	@Override
	public PostgreSQLTemplate getTemplate() {
		return PostgreSQLTemplate.POSTGRESQL_EPHEMERAL;
	}

	@Override
	public String getName() {
		return POSTGRESQL_NAME;
	}

	@Override
	public Map<String, String> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}
}
