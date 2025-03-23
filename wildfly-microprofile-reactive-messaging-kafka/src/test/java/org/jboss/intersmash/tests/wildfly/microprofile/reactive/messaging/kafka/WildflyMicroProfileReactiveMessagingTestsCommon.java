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
package org.jboss.intersmash.tests.wildfly.microprofile.reactive.messaging.kafka;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * @tpChapter AMQ Streams interoperability tests.
 *
 * @tpTestCaseDetails This class serves only as a common point for extension for the following test cases:
 * <br>
 * {@link WildflyMicroProfileReactiveMessagingPerConnectorSecuredIT}
 */
public abstract class WildflyMicroProfileReactiveMessagingTestsCommon {

	protected abstract String getEapRouteUrl();

	/**
	 *V ery simple test to check that custom data serializers (<i>PersonSerializer</i>) and
	 *                deserializers (<i>PersonDeserializer</i>) can be successfully used with AMQ Streams (Apache
	 *                Kafka) cluster instance. In this test we also check that order of send and received elements
	 *                in particular partitions is preserved.
	 * Correct data are printed by the testing servlet application
	 * Since EAP XP2, AMQ Streams 2021Q2
	 */
	@Test
	public void testSerializer() {
		RestAssured.get(getEapRouteUrl() + "/serializer")
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(Matchers.containsString("Starting serializer servlet; waiting for data..."))
				.body(Matchers.containsString("Name: Kabir; Age: 101"))
				.body(Matchers.containsString("Name: Bob; Age: 18"))
				.body(Matchers.containsString("Name: Roger; Age: 21"))
				.body(Matchers.containsString("Name: Franta; Age: 11"))
				.body(Matchers.containsString("Name: Pepa; Age: 12"))
				.body(Matchers.containsString("Name: Karel; Age: 13"))
				.body(Matchers.containsString("Name: Jaromir; Age: 14"))
				.body(Matchers.containsString("Name: Vita; Age: 15"))
				.body(Matchers.containsString("Name: Suzie; Age: 16"))
				.body(Matchers.containsString("Name: Paja; Age: 17"))
				.body(Matchers.not(Matchers.containsString("10. Name:")))
				.body(Matchers.containsString("Data order checked and is as expected."));
	}

	/**
	 * Very simple test to check that we can send data to AMQ Streams (Apache Kafka) cluster instance,
	 *                read them back and push them into the database successfully.
	 * Correct data are printed by the testing servlet application
	 * Since EAP XP2, AMQ Streams 2021Q2
	 */
	@Test
	public void testTx() {
		RestAssured.get(getEapRouteUrl() + "/tx")
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(Matchers.containsString("Starting tx servlet; waiting for data..."))
				// Check items received to the final method
				.body(Matchers.containsString("items: 3"))
				.body(Matchers.containsString("item - hello"))
				.body(Matchers.containsString("item - reactive"))
				.body(Matchers.containsString("item - messaging"))
				// Check items that got into the database - only 'reactive' is expected to be stored
				.body(Matchers.containsString("database records: 1"))
				.body(Matchers.containsString("db record - reactive"));
	}

	/**
	 * Very simple test to check that we can configure some message metadata to adjust the behavior of
	 * sent/received messages.
	 * Correct data are printed by the testing servlet application
	 * EAP XP2, AMQ Streams 2021Q2
	 */
	@Test
	public void testMetadata() {
		RestAssured.get(getEapRouteUrl() + "/metadata")
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(Matchers.containsString("Starting BasicMetadataBean metadata servlet; waiting for data..."))
				// Check items received to the final method
				.body(Matchers.containsString("Map 2 contains '2' items"))
				.body(Matchers.containsString("Map 3 contains '2' items"))
				.body(Matchers.containsString(
						"Map 2, data '1', topic 'testing2', key 'null', header 'header-1=[0, 1, 2]', timestamp"))
				.body(Matchers.containsString("Map 2, data '2', topic 'testing2', key 'KEY-2', header '', timestamp"))
				.body(Matchers.containsString(
						"Map 3, data '3', topic 'testing3', key 'null', header 'header-3=[0, 1, 2]', timestamp"))
				.body(Matchers.containsString("Map 3, data '4', topic 'testing3', key 'KEY-4', header '', timestamp"));
	}

	/**
	 * Very simple test to check that we can configure some message metadata to adjust the behavior of
	 * sent/received messages.
	 * Correct data are printed by the testing servlet application
	 * EAP XP2, AMQ Streams 2021Q2
	 */
	@Test
	public void testPartitionsMetadata() {
		RestAssured.get(getEapRouteUrl() + "/partitionsMetadata")
				.then()
				.log()
				.ifValidationFails(LogDetail.ALL, true)
				.assertThat()
				.body(Matchers.containsString("Starting SpecifyPartitionBean metadata servlet; waiting for data..."))
				// Check items received to the final method
				.body(Matchers.containsString("Metadata4 unspecified partition: 10"))
				.body(Matchers.containsString("Metadata4 specified partition: 10"))
				.body(Matchers.containsString("Metadata4, item 1 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 2 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 3 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 4 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 5 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 6 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 7 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 8 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 9 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 10 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata4, item 11 partition 1"))
				.body(Matchers.containsString("Metadata4, item 12 partition 1"))
				.body(Matchers.containsString("Metadata4, item 13 partition 1"))
				.body(Matchers.containsString("Metadata4, item 14 partition 1"))
				.body(Matchers.containsString("Metadata4, item 15 partition 1"))
				.body(Matchers.containsString("Metadata4, item 16 partition 1"))
				.body(Matchers.containsString("Metadata4, item 17 partition 1"))
				.body(Matchers.containsString("Metadata4, item 18 partition 1"))
				.body(Matchers.containsString("Metadata4, item 19 partition 1"))
				.body(Matchers.containsString("Metadata4, item 20 partition 1"))
				.body(Matchers.containsString("Metadata5 unspecified partition: 10"))
				.body(Matchers.containsString("Metadata5 specified partition: 10"))
				.body(Matchers.containsString("Metadata5, item 1 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 2 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 3 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 4 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 5 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 6 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 7 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 8 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 9 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 10 partition ")) // we don't care about partition here
				.body(Matchers.containsString("Metadata5, item 11 partition 0"))
				.body(Matchers.containsString("Metadata5, item 12 partition 0"))
				.body(Matchers.containsString("Metadata5, item 13 partition 0"))
				.body(Matchers.containsString("Metadata5, item 14 partition 0"))
				.body(Matchers.containsString("Metadata5, item 15 partition 0"))
				.body(Matchers.containsString("Metadata5, item 16 partition 0"))
				.body(Matchers.containsString("Metadata5, item 17 partition 0"))
				.body(Matchers.containsString("Metadata5, item 18 partition 0"))
				.body(Matchers.containsString("Metadata5, item 19 partition 0"))
				.body(Matchers.containsString("Metadata5, item 20 partition 0"));
	}
}
