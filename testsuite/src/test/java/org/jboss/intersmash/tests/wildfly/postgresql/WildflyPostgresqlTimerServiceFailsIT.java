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
package org.jboss.intersmash.tests.wildfly.postgresql;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import cz.xtf.core.openshift.OpenShiftWaiters;
import cz.xtf.core.openshift.OpenShifts;
import cz.xtf.junit5.listeners.ProjectCreator;
import io.restassured.filter.log.LogDetail;
import lombok.extern.slf4j.Slf4j;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.provision.openshift.OpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.PostgreSqlTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly (JBoss EAP) + PostgreSQL EJB Timer service failure test.
 *
 * <p>This test terminates the PostgreSQL pod, then calls the EJB TimerService and checks
 * that no {@code NullPointerException} is thrown when the timer service fails.</p>
 *
 */
@PostgreSqlTest
@Slf4j
@WildflyTest
@EapTest
@EapXpTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Intersmash({
		@Service(PostgresqlService.class),
		@Service(WildflyPostgresqlTimerApplication.class)
})
public class WildflyPostgresqlTimerServiceFailsIT {

	@ServiceUrl(WildflyPostgresqlTimerApplication.class)
	private String appUrl;

	@ServiceProvisioner(PostgresqlService.class)
	private OpenShiftProvisioner postgresqlProvisioner;

	/**
	 * Terminates the PostgreSQL pod, calls the EJB TimerService via the servlet's
	 * {@code CREATE_TIMER_FAILS} request, and verifies that no NPE is thrown.
	 */
	@Test
	public void testEjbTimerServiceFails() {
		// kill the Postgresql pod
		postgresqlProvisioner.scale(0, false);

		// wait till the Postgresql pod is completely terminated
		OpenShiftWaiters.get(OpenShifts.master(), () -> false).areNoPodsPresent("postgresql").waitFor();

		String exceptions = get(appUrl + "/EjbTimerServlet" + "?request=CREATE_TIMER_FAILS")
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(containsString("An error occurred"))
				.extract().asString();
		Assertions.assertFalse(exceptions.contains("java.lang.NullPointerException"));
	}
}
