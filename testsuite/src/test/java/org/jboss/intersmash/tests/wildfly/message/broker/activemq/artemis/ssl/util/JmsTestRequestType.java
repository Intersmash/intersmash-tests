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
package org.jboss.intersmash.tests.wildfly.message.broker.activemq.artemis.ssl.util;

/**
 * Enumeration of JMS test request types for testing messaging functionality.
 * <p>
 * This enum defines the different types of JMS operations that can be requested via the
 * JMS test servlet. Each request type corresponds to a specific messaging scenario such as
 * sending messages, consuming messages, or testing message-driven beans (MDB).
 * </p>
 * <p>
 * The enum values are used as request parameters when invoking the JMS test servlet endpoints.
 * </p>
 */
public enum JmsTestRequestType {
	/**
	 * Request to send a message to a JMS queue.
	 */
	REQUEST_SEND("send-message"),

	/**
	 * Request to send a message that will be consumed by a Message-Driven Bean (MDB).
	 * The MDB processes the message and sends a reply to an output queue.
	 */
	REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB("send-request-message-for-mdb"),

	/**
	 * Request to send messages for MDB consumption and trigger a server kill operation.
	 * This is used to test XA transaction recovery and message redelivery after server failure.
	 * The server is killed while the MDB is processing messages.
	 */
	REQUEST_SEND_REQUEST_MESSAGE_FOR_MDB_AND_KILL_SERVER("send-request-message-for-mdb-and-kill-server"),

	/**
	 * Request to consume a single message from a JMS queue.
	 */
	REQUEST_CONSUME_MESSAGE("consume-message"),

	/**
	 * Request to consume a single reply message from the output queue after MDB processing.
	 */
	REQUEST_CONSUME_REPLY_MESSAGE_FOR_MDB("consume-reply-message-for-mdb"),

	/**
	 * Request to consume all available reply messages from the output queue after MDB processing.
	 * This is typically used to verify that all expected messages have been processed.
	 */
	REQUEST_CONSUME_ALL_REPLY_MESSAGES_FOR_MDB("consume-all-reply-messages-for-mdb");

	/**
	 * The string value of the request type, used in HTTP request parameters.
	 */
	private final String value;

	/**
	 * Returns the string value of this request type.
	 * <p>
	 * This value is used as the request parameter when invoking the JMS test servlet.
	 * </p>
	 *
	 * @return the string value of this request type
	 */
	public String value() {
		return value;
	}

	/**
	 * Constructs a JMS test request type with the specified string value.
	 *
	 * @param value the string value for this request type
	 */
	JmsTestRequestType(String value) {
		this.value = value;
	}

	/**
	 * Converts a string value to its corresponding {@code JmsTestRequestType} enum constant.
	 * <p>
	 * This method performs a case-sensitive search for the enum constant with the matching value.
	 * </p>
	 *
	 * @param value the string value to convert
	 * @return the matching {@code JmsTestRequestType} enum constant
	 * @throws IllegalArgumentException if no enum constant with the given value exists
	 */
	public static JmsTestRequestType fromValue(String value) {
		for (JmsTestRequestType e : values()) {
			if (e.value.equals(value)) {
				return e;
			}
		}
		throw new IllegalArgumentException(
				String.format("Unsupported value for %s: %s", JmsTestRequestType.class.getSimpleName(), value));
	}
}
