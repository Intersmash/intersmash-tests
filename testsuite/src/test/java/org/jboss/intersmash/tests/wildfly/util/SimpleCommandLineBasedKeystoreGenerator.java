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
package org.jboss.intersmash.tests.wildfly.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for generating certificates and keystores using the command line keytool.
 * This class provides methods to create PKCS12 keystores and export certificates for securing endpoints.
 */
public class SimpleCommandLineBasedKeystoreGenerator {
	private static final Logger log = LoggerFactory.getLogger(SimpleCommandLineBasedKeystoreGenerator.class);
	private static final Path TMP_DIRECTORY = Paths.get("tmp").toAbsolutePath()
			.resolve(SimpleCommandLineBasedKeystoreGenerator.class.getSimpleName());
	private static final String KEYSTORE_FILE_NAME = "keystore.pkcs12";
	private static final String CERTIFICATE_FILE_NAME = "certificate.crt";
	private static final String TRUSTSTORE_FILE_NAME = "truststore.pkcs12";

	/**
	 * <p>Generates a certificate and keystore for the specified hostname.
	 * This method creates a PKCS12 keystore with a self-signed certificate and exports the certificate to a file.
	 * The generated files are stored in a temporary directory and reused if they already exist.</p>
	 * <p>
	 *     The whole setup consists in just two operations:
	 *     <ul>
	 *         <li>Generating the private key + public key pair, e.g.:<br>
	 *         <code>
	 *             E.g. keytool -genkeypair -noprompt -alias server -keyalg RSA
	 * 			                -keysize 2048 -sigalg SHA256withRSA -dname "CN=example-infinispan.infinispan.svc"
	 * 			                -validity 365 -keystore privatekey.pkcs12 -storepass 1234$1$$0$@ud0 -storetype
	 * 			                PKCS12 -ext 'san=dns:*.example-infinispan.infinispan.svc'
	 *         </code><br>
	 *         This produce a PKCS12 keystore;</li>
	 *         <li>Generating a self-signed certificate, e.g.:<br>
	 *         <code>
	 *             E.g. keytool -exportcert -noprompt -rfc -alias server -file
	 * 			                 hostname.crt -keystore privatekey.pkcs12 -storepass 1234$1$$0$@ud0 -storetype PKCS12
	 *         </code><br>
	 *         This produce a certificate in PEM format;
	 *         </li>
	 *         <li>Generating a PKCS12 truststore containing the self-signed certificate, e.g.:<br>
	 *         <code>
	 *             keytool -import -v -trustcacerts -noprompt -alias server -file hostname.crt
	 *                     -keystore truststore.pkcs12 -storetype PKCS12 -storepass password
	 *         </code>
	 *         This produce a PKCS12 truststore;
	 *         </li>
	 *     </ul>
	 * </p>
	 *
	 * @param hostName the hostname for which the certificate is generated (used as CN in the certificate)
	 * @param keyAlias the alias for the key in the keystore and truststore
	 * @param storepass the password for the keystore and truststore
	 * @param subjectAlternativeNames Subject Alternative Names list
	 * @return an CertificateInfo object containing paths to the keystore, certificate file and truststore containing the certificate file
	 * @throws RuntimeException if the keystore or certificate or truststore files are not created successfully
	 */
	public static CertificateInfo generateCertificate(String hostName, String keyAlias, String storepass,
			List<String> subjectAlternativeNames) {
		Path finalDir = Paths.get(TMP_DIRECTORY.toFile().getAbsolutePath(), hostName);
		CertificateInfo certificateInfo = new CertificateInfo(hostName, keyAlias, storepass,
				finalDir.resolve(KEYSTORE_FILE_NAME),
				finalDir.resolve(CERTIFICATE_FILE_NAME),
				finalDir.resolve(TRUSTSTORE_FILE_NAME));
		if (!finalDir.toFile().exists()) {
			finalDir.toFile().mkdirs();
			// Generate a keystore containing the private key + self-signed certificate
			processCall(finalDir, "keytool", "-genkeypair", "-noprompt", "-alias", keyAlias, "-keyalg", "RSA",
					"-keysize", "2048", "-sigalg", "SHA256withRSA", "-dname", "CN=" + hostName,
					"-validity", "365", "-keystore", KEYSTORE_FILE_NAME, "-storepass", storepass, "-storetype",
					"PKCS12", "-ext", formatSubjectAlternativeNames(hostName, subjectAlternativeNames));
			// Extracts the self-signed certificate from the keystore
			processCall(finalDir, "keytool", "-exportcert", "-noprompt", "-rfc", "-alias", "server", "-file",
					CERTIFICATE_FILE_NAME, "-keystore", KEYSTORE_FILE_NAME, "-storepass", storepass, "-storetype", "PKCS12");
			// Generate a truststore containing the self-signed certificate
			processCall(finalDir, "keytool", "-import", "-v", "-trustcacerts", "-noprompt", "-alias", keyAlias, "-file",
					CERTIFICATE_FILE_NAME, "-keystore", TRUSTSTORE_FILE_NAME, "-storepass", storepass, "-storetype", "PKCS12");
		}
		if (!certificateInfo.keystore.toFile().exists()) {
			throw new RuntimeException("Keystore file does not exist!");
		}
		if (!certificateInfo.certificate.toFile().exists()) {
			throw new RuntimeException("Certificate file does not exist!");
		}
		if (!certificateInfo.truststore.toFile().exists()) {
			throw new RuntimeException("Truststore file does not exist!");
		}
		return certificateInfo;
	}

	/**
	 * Executes a command line process in the specified working directory.
	 * This method logs the output of the process and throws an exception if the process fails.
	 *
	 * @param cwd the working directory for the process
	 * @param args the command and its arguments to execute
	 * @throws IllegalStateException if the process fails to execute or returns a non-zero exit code
	 */
	private static void processCall(Path cwd, String... args) {
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(cwd.toFile());
		pb.redirectErrorStream(true);
		int result = -1;
		Process process = null;

		try {
			process = pb.start();
			result = process.waitFor();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				while (reader.ready()) {
					if (result == 0) {
						log.debug(reader.readLine());
					} else {
						log.error(reader.readLine());
					}
				}
			}
		} catch (InterruptedException | IOException var10) {
			throw new IllegalStateException("Failed executing " + String.join(" ", args));
		}

		if (result != 0) {
			throw new IllegalStateException("Failed executing " + String.join(" ", args));
		}
	}

	/**
	 * Container class for certificate information.
	 * This class holds the metadata and file paths for a generated certificate and keystore.
	 */
	public static final class CertificateInfo {
		public final String hostName, keyAlias, storepass;
		public final Path keystore, certificate, truststore;

		/**
		 * Creates a new CertificateInfo instance.
		 *
		 * @param hostName the hostname for which the certificate was generated
		 * @param keyAlias the alias for the key in the keystore
		 * @param storepass the password for the keystore
		 * @param keystore the path to the keystore file
		 * @param certificate the path to the certificate file
		 * @param truststore the path to the truststore file
		 */
		public CertificateInfo(String hostName, String keyAlias, String storepass, Path keystore, Path certificate,
				Path truststore) {
			this.hostName = hostName;
			this.keyAlias = keyAlias;
			this.storepass = storepass;
			this.keystore = keystore;
			this.certificate = certificate;
			this.truststore = truststore;
		}
	}

	/**
	 * Formats Subject Alternative Names (SANs) for SSL/TLS certificate configuration.
	 * <p>
	 * If subjectAlternativeNames is provided and non-empty, each SAN is formatted with a "dns:"
	 * prefix (unless it already contains a colon). If no SANs are provided, a wildcard DNS entry
	 * is generated using the hostName.
	 *
	 * @param hostName the hostname used to generate a wildcard SAN if no alternatives are provided
	 * @param subjectAlternativeNames the list of subject alternative names to format, or null
	 * @return a formatted string in the form "san=dns:value1,dns:value2,..." or "san=dns:*.hostname"
	 *         if no alternative names are provided
	 */
	private static String formatSubjectAlternativeNames(String hostName, List<String> subjectAlternativeNames) {
		final List<String> formattedSans = new ArrayList<>();
		if (subjectAlternativeNames != null && !subjectAlternativeNames.isEmpty()) {
			subjectAlternativeNames.forEach(
					san -> formattedSans.add(san.contains(":") ? san : String.format("dns:%s", san)));
			return String.format("san=%s", String.join(",", formattedSans));
		} else {
			return String.format("san=dns:*.%s", hostName);
		}
	}
}
