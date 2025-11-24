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
 * Utility class for generating self-signed certificates, keystores, and truststores using command-line tools.
 * <p>
 * This class provides functionality to create complete certificate infrastructure for test environments,
 * including:
 * <ul>
 *   <li>PKCS12 keystores with self-signed certificates and private keys</li>
 *   <li>Certificate files in PEM format for HTTPS/TLS configuration</li>
 *   <li>PKCS12 truststores containing the self-signed certificates</li>
 *   <li>Private keys in PEM format for various security configurations</li>
 * </ul>
 * </p>
 * <p>
 * The class uses standard Java keytool and OpenSSL commands to generate the security artifacts.
 * Generated files are cached in a temporary directory and reused if they already exist, making
 * it efficient for test scenarios where the same certificates are needed across multiple test runs.
 * </p>
 * <p>
 * <b>Note:</b> This class generates self-signed certificates suitable for testing purposes only.
 * Do not use in production environments.
 * </p>
 *
 * @see CertificateInfo
 */
public class SimpleCommandLineBasedKeystoreGenerator {
	private static final Logger log = LoggerFactory.getLogger(SimpleCommandLineBasedKeystoreGenerator.class);
	private static final Path TMP_DIRECTORY = Paths.get("tmp").toAbsolutePath()
			.resolve(SimpleCommandLineBasedKeystoreGenerator.class.getSimpleName());
	private static final String KEYSTORE_FILE_NAME = "keystore.pkcs12";
	private static final String CERTIFICATE_FILE_NAME = "certificate.crt";
	private static final String TRUSTSTORE_FILE_NAME = "truststore.pkcs12";
	private static final String PRIVATEKEY_FILE_NAME = "privatekey.pem";

	/**
	 * Generates a complete certificate infrastructure for the specified hostname.
	 * <p>
	 * This method creates the following security artifacts using Java keytool and OpenSSL:
	 * <ul>
	 *   <li><b>PKCS12 Keystore</b> - Contains the private key and self-signed certificate (RSA 2048-bit, SHA256withRSA signature)
	 *   e.g.:<br>
	 *   <code>
	 *       keytool -genkeypair -noprompt -alias keycloak-saml-adapter -keyalg RSA -keysize 2048 -sigalg SHA256withRSA
	 *               -dname CN=keycloak-saml-adapter-....com -validity 365 -keystore keystore.pkcs12 -storepass 1234password
	 *               -storetype PKCS12 -keypass 1234password -ext bc=ca:true
	 *               -ext san=dns:*.keycloak-saml-adapter-....com
	 *   </code></li>
	 *   <li><b>Certificate File</b> - Self-signed certificate in PEM/RFC format exported from the keystore
	 *   e.g.:<br>
	 *   <code>
	 *       keytool -exportcert -noprompt -rfc -alias keycloak-saml-adapter -file certificate.crt -keystore keystore.pkcs12
	 *               -storepass 1234password -storetype PKCS12
	 *   </code></li>
	 *   <li><b>PKCS12 Truststore</b> - Contains the certificate for establishing trust
	 *   e.g.:<br>
	 *   <code>
	 *       keytool -import -v -trustcacerts -noprompt -alias keycloak-saml-adapter -file certificate.crt -keystore truststore.pkcs12
	 *               -storepass 1234password -storetype PKCS12
	 *   </code></li>
	 *   <li><b>Private Key File</b> - Private key in PEM format for configurations requiring separate key files
	 *   e.g.:<br>
	 *   <code>
	 *       openssl pkcs12 -in keystore.pkcs12 -nocerts -nodes -out privatekey.pem -passin pass:1234password
	 *   </code></li>
	 * </ul>
	 * </p>
	 * <p>
	 * <b>Certificate Details:</b>
	 * <ul>
	 *   <li>The certificate is valid for 365 days from generation</li>
	 *   <li>The certificate DN (Distinguished Name) uses the provided hostname as CN (Common Name)</li>
	 *   <li>The certificate includes the Basic Constraints extension with CA:TRUE, allowing tools like curl
	 *       to accept it as a valid Certificate Authority</li>
	 *   <li>Subject Alternative Names (SANs) are included for flexible hostname matching</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <b>File Caching:</b> Generated files are stored in a temporary directory under {@code tmp/SimpleCommandLineBasedKeystoreGenerator/hostname/}.
	 * If the directory already exists, the method reuses the existing files rather than regenerating them,
	 * improving performance in test scenarios.
	 * </p>
	 *
	 * @param hostName the hostname for which the certificate is generated (used as CN in the certificate)
	 * @param keyAlias the alias for the key in the keystore and truststore
	 * @param storepass the password for the keystore and truststore
	 * @param keypass the password for the private key (can be null or empty if not required)
	 * @param subjectAlternativeNames list of Subject Alternative Names (SANs) to include; if null or empty,
	 *                                a wildcard DNS entry is generated using the hostname
	 * @return a {@link CertificateInfo} object containing paths to all generated files and metadata
	 * @throws RuntimeException if any of the required files (keystore, certificate, truststore, privatekey) are not created successfully
	 * @throws IllegalStateException if keytool or openssl commands fail to execute
	 */
	public static CertificateInfo generateCertificate(String hostName, String keyAlias, String storepass, String keypass,
			List<String> subjectAlternativeNames) {
		assert hostName != null;
		Path finalDir = Paths.get(TMP_DIRECTORY.toFile().getAbsolutePath(), hostName);
		log.info("Creating keystore directory {} for hostname {}", finalDir.toFile().getAbsolutePath(), hostName);
		CertificateInfo certificateInfo = new CertificateInfo(hostName, keyAlias, storepass,
				finalDir.resolve(KEYSTORE_FILE_NAME),
				finalDir.resolve(CERTIFICATE_FILE_NAME),
				finalDir.resolve(TRUSTSTORE_FILE_NAME),
				finalDir.resolve(PRIVATEKEY_FILE_NAME));
		if (!finalDir.toFile().exists()) {
			finalDir.toFile().mkdirs();
			// Generate a keystore containing the private key + self-signed certificate
			if (keypass != null && !keypass.isEmpty()) {
				processCall(finalDir, "keytool", "-genkeypair", "-noprompt", "-alias", keyAlias, "-keyalg", "RSA",
						"-keysize", "2048", "-sigalg", "SHA256withRSA", "-dname", "CN=" + hostName,
						"-validity", "365", "-keystore", KEYSTORE_FILE_NAME, "-storepass", storepass, "-storetype",
						"PKCS12", "-keypass", keypass, "-ext", "bc=ca:true", "-ext",
						formatSubjectAlternativeNames(hostName, subjectAlternativeNames));
			} else {
				processCall(finalDir, "keytool", "-genkeypair", "-noprompt", "-alias", keyAlias, "-keyalg", "RSA",
						"-keysize", "2048", "-sigalg", "SHA256withRSA", "-dname", "CN=" + hostName,
						"-validity", "365", "-keystore", KEYSTORE_FILE_NAME, "-storepass", storepass, "-storetype",
						"PKCS12", "-ext", "bc=ca:true", "-ext",
						formatSubjectAlternativeNames(hostName, subjectAlternativeNames));
			}
			// Extracts the self-signed certificate from the keystore
			processCall(finalDir, "keytool", "-exportcert", "-noprompt", "-rfc", "-alias", keyAlias, "-file",
					CERTIFICATE_FILE_NAME, "-keystore", KEYSTORE_FILE_NAME, "-storepass", storepass, "-storetype", "PKCS12");
			// Generate a truststore containing the self-signed certificate
			processCall(finalDir, "keytool", "-import", "-v", "-trustcacerts", "-noprompt", "-alias", keyAlias, "-file",
					CERTIFICATE_FILE_NAME, "-keystore", TRUSTSTORE_FILE_NAME, "-storepass", storepass, "-storetype", "PKCS12");
			// Extract the private key in PEM format
			processCall(finalDir, "openssl", "pkcs12", "-in", KEYSTORE_FILE_NAME, "-nocerts", "-nodes", "-out",
					PRIVATEKEY_FILE_NAME, "-passin", String.format("pass:%s", storepass));
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
		if (!certificateInfo.privatekey.toFile().exists()) {
			throw new RuntimeException("Privatekey file does not exist!");
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
		log.info(String.join(" ", args));
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
	 * Immutable container class holding certificate metadata and file paths.
	 * <p>
	 * This class encapsulates all information related to a generated certificate infrastructure,
	 * including:
	 * <ul>
	 *   <li>Metadata: hostname, key alias, and store password</li>
	 *   <li>File paths: keystore, certificate (PEM), truststore, and private key (PEM)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Instances of this class are returned by {@link #generateCertificate(String, String, String, String, List)}
	 * and provide convenient access to all generated security artifacts.
	 * </p>
	 */
	public static final class CertificateInfo {
		public final String hostName, keyAlias, storepass;
		public final Path keystore, certificate, truststore, privatekey;

		/**
		 * Creates a new CertificateInfo instance.
		 *
		 * @param hostName the hostname for which the certificate was generated
		 * @param keyAlias the alias for the key in the keystore
		 * @param storepass the password for the keystore
		 * @param keystore the path to the keystore file
		 * @param certificate the path to the certificate file (PEM format)
		 * @param truststore the path to the truststore file
		 * @param privatekey the path to the key file (PEM format)
		 */
		public CertificateInfo(String hostName, String keyAlias, String storepass, Path keystore, Path certificate,
				Path truststore, Path privatekey) {
			this.hostName = hostName;
			this.keyAlias = keyAlias;
			this.storepass = storepass;
			this.keystore = keystore;
			this.certificate = certificate;
			this.truststore = truststore;
			this.privatekey = privatekey;
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
			return String.format("san=dns:%s", hostName.replaceFirst("^[^\\.]+\\.", "*."));
		}
	}
}
