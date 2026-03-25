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

import cz.xtf.junit5.listeners.ProjectCreator;
import io.restassured.filter.log.LogDetail;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.tests.junit.annotations.EapTest;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.PostgreSqlTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * WildFly (JBoss EAP) + PostgreSQL EJB Timer interoperability test.
 *
 * <p>This test verifies that an EJB timer is correctly stored into a PostgreSQL database
 * and that the timestamp value has a correct time format (zero milliseconds).</p>
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
public class WildflyPostgresqlTimerIT {

	private static final int TIME_PART_INDEX = 21;

	@ServiceUrl(WildflyPostgresqlTimerApplication.class)
	private String appUrl;

	/**
	 * Creates an EJB timer via the servlet and verifies the timestamp stored in the PostgreSQL database
	 * has zero milliseconds, confirming correct time format handling.
	 */
	@Test
	public void testTimestamp() {
		String time = get(appUrl + "/EjbTimerServlet" + "?request=CREATE_TIMER")
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(containsString("next_date"))
				.extract().asString().substring(TIME_PART_INDEX);

		int milliseconds = LocalTime.parse(time).getNano();
		Assertions.assertEquals(0, milliseconds);
	}
}
