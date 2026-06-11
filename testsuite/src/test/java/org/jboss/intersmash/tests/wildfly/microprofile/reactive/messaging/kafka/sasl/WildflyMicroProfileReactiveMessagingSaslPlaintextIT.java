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
package org.jboss.intersmash.tests.wildfly.microprofile.reactive.messaging.kafka.sasl;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.jboss.intersmash.annotations.Intersmash;
import org.jboss.intersmash.annotations.Service;
import org.jboss.intersmash.annotations.ServiceProvisioner;
import org.jboss.intersmash.annotations.ServiceUrl;
import org.jboss.intersmash.k8s.junit5.ProjectCreator;
import org.jboss.intersmash.provision.openshift.OpenShiftProvisioner;
import org.jboss.intersmash.provision.openshift.WildflyImageOpenShiftProvisioner;
import org.jboss.intersmash.tests.junit.annotations.EapXpTest;
import org.jboss.intersmash.tests.junit.annotations.KafkaTest;
import org.jboss.intersmash.tests.junit.annotations.OpenShiftTest;
import org.jboss.intersmash.tests.junit.annotations.WildflyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies the interoperability between
 * WildFly/JBoss EAP XP and Strimzi/Streams for Apache Kafka on OpenShift, specifically with respect to the <b>SASL_PLAINTEXT</b>
 * use case.
 * <p>
 * The Strimzi/Streams for Apache Kafka service - provisioned via the related Operator, see
 * {@link SecuredKafkaMicroProfileReactiveMessagingApplication} -
 * provides two Kafka listeners, one which allows unencrypted communication, and another that allows only encrypted
 * communication. Both listeners are configured to let only authenticated clients connect.
 * <br>
 * The used authentication mechanism is SASL (<i>Simple Authentication and Security Layer</i>), and this test verifies
 * that a WildFly/JBoss EAP XP application configured to use the <i>SASL_PLAINTEXT</i> mechanism over an unencrypted connection
 * can successfully authenticate to a Kafka listener and send/receive messages from a {@link io.strimzi.api.kafka.model.KafkaTopic}.
 * <br>
 * Both the Strimzi/Streams for Apache Kafka and WildFly/JBoss EAP XP instances are configured to use SCRAM-SHA-512
 * based authentication.
 * <br>
 * WildFly/JBoss EAP XP client applications are configured through MicroProfile Config, both via a properties file and
 * environment variables.
 * </p>
 */
@KafkaTest
@WildflyTest
@EapXpTest
@OpenShiftTest
@ExtendWith(ProjectCreator.class)
@Intersmash({
		@Service(SecuredKafkaMicroProfileReactiveMessagingApplication.class),
		@Service(WildflyMicroProfileReactiveMessagingSaslPlaintextApplication.class)
})

public class WildflyMicroProfileReactiveMessagingSaslPlaintextIT {
	@ServiceUrl(WildflyMicroProfileReactiveMessagingSaslPlaintextApplication.class)
	private String applicationRouteUrl;

	@ServiceProvisioner(SecuredKafkaMicroProfileReactiveMessagingApplication.class)
	private OpenShiftProvisioner<SecuredKafkaMicroProfileReactiveMessagingApplication> kafkaOpenShiftProvisioner;

	@ServiceProvisioner(WildflyMicroProfileReactiveMessagingSaslPlaintextApplication.class)
	private WildflyImageOpenShiftProvisioner eapOpenShiftProvisioner;

	@Test
	public void test() throws Exception {
		int status = RestAssured.given().header("Content-Type", MediaType.TEXT_PLAIN)
				.post(applicationRouteUrl + "/one").getStatusCode();
		Assertions.assertEquals(200, status);
		status = RestAssured.given().header("Content-Type", MediaType.TEXT_PLAIN)
				.post(applicationRouteUrl + "/two").getStatusCode();
		Assertions.assertEquals(200, status);

		List<String> list = new ArrayList<>();
		long end = System.currentTimeMillis() + 20000;
		while (list.size() != 2 && System.currentTimeMillis() < end) {
			Response r = RestAssured.get(applicationRouteUrl);
			Assertions.assertEquals(200, r.getStatusCode());
			list = r.as(List.class);
			Thread.sleep(1000);
		}
		Assertions.assertArrayEquals(new String[] { "one", "two" }, list.toArray(new String[list.size()]));
	}
}
