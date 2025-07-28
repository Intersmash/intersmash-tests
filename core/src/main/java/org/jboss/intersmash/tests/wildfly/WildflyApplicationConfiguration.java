/**
* Copyright (C) 2023 Red Hat, Inc.
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
package org.jboss.intersmash.tests.wildfly;

import org.apache.commons.lang3.StringUtils;

/**
 * Defines the contract for WILDFLY application services that expose
 * configuration which should be taken into account by s2i builds
 */
public interface WildflyApplicationConfiguration {

	String WILDFLY_EE_FEATURE_PACK_LOCATION = "wildfly.ee-feature-pack.location";
	String WILDFLY_FEATURE_PACK_LOCATION = "wildfly.feature-pack.location";
	String WILDFLY_CLOUD_FEATURE_PACK_LOCATION = "wildfly.cloud-feature-pack.location";
	String WILDFLY_DATASOURCES_FEATURE_PACK_LOCATION = "wildfly.datasources-feature-pack.location";
	String WILDFLY_EE_CHANNEL_GROUPID = "wildfly.ee-channel.groupId";
	String WILDFLY_EE_CHANNEL_ARTIFACTID = "wildfly.ee-channel.artifactId";
	String WILDFLY_EE_CHANNEL_VERSION = "wildfly.ee-channel.version";
	String WILDFLY_BOMS_EE_SERVER_VERSION = "bom.wildfly-ee.version";
	String WILDFLY_MAVEN_PLUGIN_GROUPID = "wildfly-maven-plugin.groupId";
	String WILDFLY_MAVEN_PLUGIN_ARTIFACTID = "wildfly-maven-plugin.artifactId";
	String WILDFLY_MAVEN_PLUGIN_VERSION = "wildfly-maven-plugin.version";
	String WILDFLY_KEYCLOAK_SAML_ADAPTER_FEATURE_PACK_VERSION = "wildfly.keycloak-saml-adapter-feature-pack.version";
	String MAVEN_MIRROR_URL = "maven-mirror.url";
	String MAVEN_RESOLVER_TRANSPORT = "maven.resolver.transport";
	String MAVEN_WAGON_HTTP_SSL_INSECURE = "maven.wagon.http.ssl.insecure";

	default String wildflyMavenPluginGroupId() {
		return System.getProperty(WILDFLY_MAVEN_PLUGIN_GROUPID);
	}

	default String wildflyMavenPluginArtifactId() {
		return System.getProperty(WILDFLY_MAVEN_PLUGIN_ARTIFACTID);
	}

	default String wildflyMavenPluginVersion() {
		return System.getProperty(WILDFLY_MAVEN_PLUGIN_VERSION);
	}

	default String eeFeaturePackLocation() {
		return System.getProperty(WILDFLY_EE_FEATURE_PACK_LOCATION);
	}

	default String featurePackLocation() {
		return System.getProperty(WILDFLY_FEATURE_PACK_LOCATION);
	}

	default String cloudFeaturePackLocation() {
		return System.getProperty(WILDFLY_CLOUD_FEATURE_PACK_LOCATION);
	}

	default String datasourcesFeaturePackLocation() {
		return System.getProperty(WILDFLY_DATASOURCES_FEATURE_PACK_LOCATION);
	}

	default String eeChannelGroupId() {
		return System.getProperty(WILDFLY_EE_CHANNEL_GROUPID);
	}

	default String eeChannelArtifactId() {
		return System.getProperty(WILDFLY_EE_CHANNEL_ARTIFACTID);
	}

	default String eeChannelVersion() {
		return System.getProperty(WILDFLY_EE_CHANNEL_VERSION);
	}

	default String bomsEeServerVersion() {
		return System.getProperty(WILDFLY_BOMS_EE_SERVER_VERSION);
	}

	default String getMavenMirrorUrl() {
		return System.getProperty(MAVEN_MIRROR_URL);
	}

	default String getMavenResolverTransport() {
		return System.getProperty(MAVEN_RESOLVER_TRANSPORT);
	}

	default String getMavenWagonHttpSslInsecure() {
		return System.getProperty(MAVEN_WAGON_HTTP_SSL_INSECURE);
	}

	default String keycloakSamlAdapterFeaturePackVersion() {
		return System.getProperty(WILDFLY_KEYCLOAK_SAML_ADAPTER_FEATURE_PACK_VERSION);
	}

	/**
	 * Server version property name for BOMs.
	 *
	 * @return A string that represents the name of a property which could hold a
	 *         given WILDFLY 8 application testable BOMs version
	 */
	default String bomsEeServerVersionPropertyName() {
		return WILDFLY_BOMS_EE_SERVER_VERSION;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly-galleon-pack" feature pack GAV.
	 *
	 * @return property name in the pom.xml file used to set the feature pack GAV
	 */
	default String featurePackLocationPropertyName() {
		return WILDFLY_FEATURE_PACK_LOCATION;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly-ee-galleon-pack" feature pack GAV.
	 *
	 * @return property name in the pom.xml file used to set the feature pack GAV
	 */
	default String eeFeaturePackLocationPropertyName() {
		return WILDFLY_EE_FEATURE_PACK_LOCATION;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly-cloud-galleon-pack" feature pack GAV.
	 *
	 * @return property name in the pom.xml file used to set the cloud feature pack
	 *         GAV
	 */
	default String cloudFeaturePackLocationPropertyName() {
		return WILDFLY_CLOUD_FEATURE_PACK_LOCATION;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly-datasources-galleon-pack" feature pack GAV.
	 *
	 * @return property name in the pom.xml file used to set the datasources feature
	 *         pack GAV
	 */
	default String datasourcesFeaturePackLocationPropertyName() {
		return WILDFLY_DATASOURCES_FEATURE_PACK_LOCATION;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly.keycloak-saml-adapter-feature-pack.version" feature pack version.
	 *
	 * @return property name in the pom.xml file used to set the Keycloack SAML
	 *         Adapter feature pack version
	 */
	default String keycloakSamlAdapterFeaturePackVersionPropertyName() {
		return WILDFLY_KEYCLOAK_SAML_ADAPTER_FEATURE_PACK_VERSION;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the channel
	 * GroupId.
	 *
	 * @return property name in the pom.xml file used to set the channel artifact
	 *         groupId
	 */
	default String eeChannelGroupIdPropertyName() {
		return WILDFLY_EE_CHANNEL_GROUPID;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the channel
	 * ArtifactId.
	 *
	 * @return property name in the pom.xml file used to set the channel artifact
	 *         artifactId
	 */
	default String eeChannelArtifactIdPropertyName() {
		return WILDFLY_EE_CHANNEL_ARTIFACTID;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the channel
	 * Version.
	 *
	 * @return property name in the pom.xml file used to set the channel artifact
	 *         version
	 */
	default String eeChannelVersionPropertyName() {
		return WILDFLY_EE_CHANNEL_VERSION;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the channel
	 * GAV.
	 *
	 * @return property name in the pom.xml file used to set the channel GAV
	 */
	default String postgresFeaturePackDriverVersion() {
		return "42.2.8";
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly-maven-plugin" groupId.
	 *
	 * @return property name in the pom.xml file used to set the
	 *         "wildfly-maven-plugin" groupId
	 */
	default String wildflyMavenPluginGroupIdPropertyName() {
		return WILDFLY_MAVEN_PLUGIN_GROUPID;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly-maven-plugin" artifactId.
	 *
	 * @return property name in the pom.xml file used to set the
	 *         "wildfly-maven-plugin" artifactId
	 */
	default String wildflyMavenPluginArtifactIdPropertyName() {
		return WILDFLY_MAVEN_PLUGIN_ARTIFACTID;
	}

	/**
	 * This must match the property name in the pom.xml file used to set the
	 * "wildfly-maven-plugin" version.
	 *
	 * @return property name in the pom.xml file used to set the
	 *         "wildfly-maven-plugin" version
	 */
	default String wildflyMavenPluginVersionPropertyName() {
		return WILDFLY_MAVEN_PLUGIN_VERSION;
	}

	/**
	 * Build a string that will be used to set the {@code MAVEN_ARGS_APPEND}
	 * environment variable of a s2i build configuration. This way all relevant
	 * system properties and profiles which should be used to build a WildFly/JBoss
	 * EAP or JBoss EAP XP application conveniently, i.e. with the expected bits
	 * and configuration, can be forwarded to a remote s2i build.
	 * 
	 * @return A string that will be set as the value for the
	 *         {@code MAVEN_ARGS_APPEND} environment variable of a s2i build
	 *         configuration.
	 */
	default String generateAdditionalMavenArgs() {
		String result = "".concat(((StringUtils.isBlank(this.eeFeaturePackLocation())
				? ""
				: (" -D" + this.eeFeaturePackLocationPropertyName() + "=" + this.eeFeaturePackLocation())))
				+ ((StringUtils.isBlank(this.featurePackLocation())
						? ""
						: (" -D" + this.featurePackLocationPropertyName() + "=" + this.featurePackLocation())))
				+ ((StringUtils.isBlank(this.cloudFeaturePackLocation())
						? ""
						: (" -D" + this.cloudFeaturePackLocationPropertyName() + "="
								+ this.cloudFeaturePackLocation())))
				+ ((StringUtils.isBlank(this.datasourcesFeaturePackLocation())
						? ""
						: (" -D" + this.datasourcesFeaturePackLocationPropertyName() + "="
								+ this.datasourcesFeaturePackLocation())))
				+ ((StringUtils.isBlank(this.keycloakSamlAdapterFeaturePackVersion())
						? ""
						: (" -D" + this.keycloakSamlAdapterFeaturePackVersionPropertyName() + "="
								+ this.keycloakSamlAdapterFeaturePackVersion())))
				+ ((StringUtils.isBlank(this.eeChannelGroupId())
						? ""
						: (" -D" + this.eeChannelGroupIdPropertyName() + "=" + this.eeChannelGroupId())))
				+ ((StringUtils.isBlank(this.eeChannelArtifactId())
						? ""
						: (" -D" + this.eeChannelArtifactIdPropertyName() + "=" + this.eeChannelArtifactId())))
				+ ((StringUtils.isBlank(this.eeChannelVersion())
						? ""
						: (" -D" + this.eeChannelVersionPropertyName() + "=" + this.eeChannelVersion())))
				+ ((StringUtils.isBlank(this.wildflyMavenPluginGroupId())
						? ""
						: (" -D" + this.wildflyMavenPluginGroupIdPropertyName() + "="
								+ this.wildflyMavenPluginGroupId())))
				+ ((StringUtils.isBlank(this.wildflyMavenPluginArtifactId())
						? ""
						: (" -D" + this.wildflyMavenPluginArtifactIdPropertyName() + "="
								+ this.wildflyMavenPluginArtifactId())))
				+ ((StringUtils.isBlank(this.wildflyMavenPluginVersion())
						? ""
						: (" -D" + this.wildflyMavenPluginVersionPropertyName() + "="
								+ this.wildflyMavenPluginVersion())))
				+ ((StringUtils.isBlank(this.bomsEeServerVersion())
						? ""
						: (" -D" + this.bomsEeServerVersionPropertyName() + "=" + this.bomsEeServerVersion()))));
		// let's forward the distribution for building the application.
		result = result.concat(" " + getWildflyApplicationTargetDistributionProfile());
		// wagon related properties
		final String mavenResolverTransport = this.getMavenResolverTransport();
		result = result.concat(StringUtils.isBlank(mavenResolverTransport)
				? ""
				: String.format(" -D%s=%s", MAVEN_RESOLVER_TRANSPORT, mavenResolverTransport));
		final String mavenWagonHttpSslInsecure = this.getMavenWagonHttpSslInsecure();
		result = result.concat(StringUtils.isBlank(mavenWagonHttpSslInsecure)
				? ""
				: String.format(" -D%s=%s", MAVEN_WAGON_HTTP_SSL_INSECURE, mavenWagonHttpSslInsecure));
		return result;
	}

	static String getWildflyApplicationTargetDistributionProfile() {
		return String.format("-Pwildfly-target-distribution.%s",
				System.getProperty("wildfly-target-distribution", "wildfly"));
	}
}