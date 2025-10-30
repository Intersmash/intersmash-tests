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
 * Constants used for JMS testing with ActiveMQ message broker.
 * <p>
 * This class defines message content and response templates used by the JMS test servlet
 * and message-driven beans during testing of message broker interactions.
 * </p>
 */
public class JmsTestConstants {

	/**
	 * Response message template when a message is successfully sent to a queue.
	 * The actual queue name will be appended to this string.
	 */
	public static final String QUEUE_SEND_RESPONSE = "Sent a text message to ";

	/**
	 * Default text content for test messages sent from the servlet to a queue.
	 */
	public static final String QUEUE_TEXT_MESSAGE = "Hello Servlet!";

	/**
	 * Response message template when a message is successfully sent to the MDB queue.
	 * The actual queue name will be appended to this string.
	 */
	public static final String QUEUE_MDB_SEND_RESPONSE = "Sent a text message for MDB to queue ";

	/**
	 * Default text content for test messages sent to be processed by the MDB.
	 */
	public static final String QUEUE_MDB_TEXT_MESSAGE = "Hello MDB!";

	/**
	 * Standard reply message text sent to the outQueue for each processed message.
	 */
	public static final String QUEUE_MDB_TEXT_REPLY_MESSAGE = "Hello MDB - reply message!";

	/**
	 * Name of the test queue used for basic JMS operations testing.
	 */
	public static final String TEST_QUEUE = "testQueue";

	/**
	 * Name of the input queue where messages are sent to be processed by the MDB.
	 */
	public static final String IN_QUEUE = "inQueue";

	/**
	 * Name of the output queue where the MDB sends reply messages after processing.
	 */
	public static final String OUT_QUEUE = "outQueue";
}
