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
package org.jboss.intersmash.tests.wildfly.web.cache.offload.infinispan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for generating Infinispan certificates and keystores using the command line keytool.
 * This class provides methods to create PKCS12 keystores and export certificates for securing Infinispan endpoints.
 */
public class InfinispanCommandLineBasedKeystoreGenerator {
	private static final Logger log = LoggerFactory.getLogger(InfinispanCommandLineBasedKeystoreGenerator.class);
	private static final Path TMP_DIRECTORY = Paths.get("tmp").toAbsolutePath()
			.resolve(InfinispanCommandLineBasedKeystoreGenerator.class.getSimpleName());
	private static final String KEYSTORE_FILE_NAME = "keystore.pkcs12";
	private static final String CERTIFICATE_FILE_NAME = "certificate.crt";

	/**
	 * <p>Generates an Infinispan certificate and keystore for the specified hostname.
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
	 *     </ul>
	 * </p>
	 *
	 * @param hostName the hostname for which the certificate is generated (used as CN in the certificate)
	 * @param keyAlias the alias for the key in the keystore
	 * @param storepass the password for the keystore
	 * @return an InfinispanCertificate object containing paths to the keystore and certificate files
	 * @throws IOException if an I/O error occurs during certificate generation
	 * @throws RuntimeException if the keystore or certificate files are not created successfully
	 */
	public static InfinispanCertificate generateInfinispanCertificate(String hostName, String keyAlias, String storepass)
			throws IOException {
		Path finalDir = Paths.get(TMP_DIRECTORY.toFile().getAbsolutePath(), hostName);
		InfinispanCertificate infinispanCertificate = new InfinispanCertificate(hostName, keyAlias, storepass,
				finalDir.resolve(KEYSTORE_FILE_NAME),
				finalDir.resolve(CERTIFICATE_FILE_NAME));
		if (!finalDir.toFile().exists()) {
			finalDir.toFile().mkdirs();
			// Generate the private key + public key pair
			processCall(finalDir, "keytool", "-genkeypair", "-noprompt", "-alias", keyAlias, "-keyalg", "RSA",
					"-keysize", "2048", "-sigalg", "SHA256withRSA", "-dname", "CN=" + hostName,
					"-validity", "365", "-keystore", KEYSTORE_FILE_NAME, "-storepass", storepass, "-storetype",
					"PKCS12", "-ext", String.format("san=dns:*.%s", hostName));
			// Generating a self-signed certificate
			processCall(finalDir, "keytool", "-exportcert", "-noprompt", "-rfc", "-alias", "server", "-file",
					CERTIFICATE_FILE_NAME, "-keystore", KEYSTORE_FILE_NAME, "-storepass", storepass, "-storetype", "PKCS12");
		}
		if (!infinispanCertificate.keystore.toFile().exists()) {
			throw new RuntimeException("Keystore file does not exist!");
		}
		if (!infinispanCertificate.certificate.toFile().exists()) {
			throw new RuntimeException("Certificate file does not exist!");
		}
		return infinispanCertificate;
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
	 * Container class for Infinispan certificate information.
	 * This class holds the metadata and file paths for a generated Infinispan certificate and keystore.
	 */
	public static final class InfinispanCertificate {
		public final String hostName, keyAlias, storepass;
		public final Path keystore, certificate;

		/**
		 * Creates a new InfinispanCertificate instance.
		 *
		 * @param hostName the hostname for which the certificate was generated
		 * @param keyAlias the alias for the key in the keystore
		 * @param storepass the password for the keystore
		 * @param keystore the path to the keystore file
		 * @param certificate the path to the certificate file
		 */
		public InfinispanCertificate(String hostName, String keyAlias, String storepass, Path keystore, Path certificate) {
			this.hostName = hostName;
			this.keyAlias = keyAlias;
			this.storepass = storepass;
			this.keystore = keystore;
			this.certificate = certificate;
		}
	}
}
