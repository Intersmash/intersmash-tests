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

import org.jboss.intersmash.application.openshift.PostgreSQLImageOpenShiftApplication;
import org.jboss.intersmash.application.openshift.template.PostgreSQLTemplate;

/**
 * Deploy the postgresql database using the {@link PostgreSQLTemplate#POSTGRESQL_EPHEMERAL} template. Use the template
 * default parameters.
 */
public class KeycloakPostgresqlApplication implements PostgreSQLImageOpenShiftApplication {

	public static final String POSTGRESQL_NAME = "postgresql";
	public static final String POSTGRESQL_DATABASE = "keycloak-db";
	public static final String POSTGRESQL_PASSWORD = "keycloak-1234";
	public static final String POSTGRESQL_USER = "user-keycloak";

	/**
	 * Get the application name.
	 *
	 * @return the application name
	 */
	@Override
	public String getName() {
		return POSTGRESQL_NAME;
	}

	/**
	 * Get the database user.
	 *
	 * @return the database user
	 */
	@Override
	public String getUser() {
		return POSTGRESQL_USER;
	}

	/**
	 * Get the database password.
	 *
	 * @return the database password
	 */
	@Override
	public String getPassword() {
		return POSTGRESQL_PASSWORD;
	}

	/**
	 * Get the database name.
	 *
	 * @return the database name
	 */
	@Override
	public String getDbName() {
		return POSTGRESQL_DATABASE;
	}

	/**
	 * Get the service name for the PostgreSQL database.
	 *
	 * @return the service name
	 */
	public static String getServiceName() {
		return POSTGRESQL_NAME + "-service";
	}

	/**
	 * Get the service port for the PostgreSQL database.
	 *
	 * @return the service port
	 */
	public static Long getServicePort() {
		return 5432L;
	}

	/**
	 * Get the service secret name for the PostgreSQL database credentials.
	 *
	 * @return the service secret name
	 */
	public static String getServiceSecretName() {
		return POSTGRESQL_NAME + "-credentials";
	}

	/**
	 * Get the service database name for the PostgreSQL database.
	 *
	 * @return the service database name
	 */
	public static String getServiceDbName() {
		return POSTGRESQL_DATABASE;
	}
}
