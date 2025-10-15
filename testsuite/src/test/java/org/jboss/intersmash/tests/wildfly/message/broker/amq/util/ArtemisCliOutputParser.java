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
package org.jboss.intersmash.tests.wildfly.message.broker.amq.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for parsing output from Artemis CLI commands.
 * <p>
 * This class provides helper methods to parse and process JSON-formatted output
 * returned by ActiveMQ Artemis CLI commands, particularly for extracting metrics
 * and statistics from broker operations.
 * </p>
 */
public class ArtemisCliOutputParser {
	private static final Logger log = LoggerFactory.getLogger(ArtemisCliOutputParser.class);

	/**
	 * Calculates the total message count from Artemis CLI JSON output with optional name filtering.
	 * <p>
	 * This method parses JSON output from Artemis CLI commands and sums the message counts
	 * from queues that match the specified name filter. The expected JSON format contains a
	 * "data" array where each element has both "name" and "messageCount" fields.
	 * </p>
	 * <p>
	 * The name matching supports both exact matches and automatic conversion from camelCase
	 * to kebab-case format. For example, a filter of "myQueue" will match both "myQueue"
	 * and "my-queue" in the data.
	 * </p>
	 *
	 * @param jsonString the JSON-formatted string returned by Artemis CLI commands
	 * @param queueName the name to filter by; if {@code null}, sums message counts from all queues
	 * @return the sum of all message counts from queues matching the filter (or all queues if filter is {@code null})
	 * @throws JsonSyntaxException if the input string is not valid JSON or doesn't match the expected structure
	 */
	public static int sumMessageCounts(String jsonString, String queueName) {
		log.info("[ArtemisCliOutputParser#sumMessageCounts] Counting messages in queue {} from json string: \n{}\n", queueName,
				jsonString);
		JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
		JsonArray dataArray = root.getAsJsonArray("data");

		int sum = 0;
		for (JsonElement element : dataArray) {
			JsonObject obj = element.getAsJsonObject();
			String name = obj.get("name").getAsString();

			// If queueName is null or matches the current element's name, add to sum
			if (queueName == null || name.equals(queueName) || name.equals(convertCamelCaseToKebab(queueName))) {
				String messageCountStr = obj.get("messageCount").getAsString();
				sum += Integer.parseInt(messageCountStr);
			}
		}

		return sum;
	}

	/**
	 * Converts a camelCase string to kebab-case format.
	 * <p>
	 * This method transforms naming conventions by inserting hyphens before uppercase letters
	 * and converting all characters to lowercase. This is useful for matching queue names that
	 * may be represented differently in various contexts (e.g., "myQueue" becomes "my-queue").
	 * </p>
	 * <p>
	 * Examples:
	 * <ul>
	 *   <li>"myQueue" → "my-queue"</li>
	 *   <li>"HTTPSConnection" → "https-connection"</li>
	 *   <li>"simpleTest" → "simple-test"</li>
	 * </ul>
	 * </p>
	 *
	 * @param input the camelCase string to convert; may be {@code null}
	 * @return the kebab-case formatted string, or {@code null} if the input is {@code null}
	 */
	public static String convertCamelCaseToKebab(String input) {
		return input == null ? null
				: input
						.replaceAll("([A-Z])(?=[A-Z])", "$1-")
						.replaceAll("([a-z])([A-Z])", "$1-$2")
						.toLowerCase();
	}
}
