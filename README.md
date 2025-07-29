# Intersmash Tests

![Simple build workflow](https://github.com/Intersmash/intersmash-tests/actions/workflows/simple-build.yml/badge.svg)

Intersmash test cases.

## Overview

The goal is to have a common repository for Intersmash test cases, which are executed to verify complex interoperability 
scenarios between Middleware runtimes in Cloud platform environments, most notably OpenShift.

Intersmash Tests leverage the [Intersmash framework](https://github.com/Intersmash/intersmash) to provision the tested 
scenarios, including both service runtimes - like Kafka, Infinispan or Keycloak - and 
[Intersmash Applications](https://github.com/Intersmash/intersmash-applications), for instance WildFly deployments.

The tests in the repository can be executed by enabling specific Maven profiles in order to use either community or 
product deliverables, like images or Helm Charts, see the [Profiles section](#profiles).

## Tests

Tests are executed by default on Kubernetes, and with community bits for applications, with the Maven Failsafe Plugin 
using the [global-test.properties](global-test.properties) file to configure Intersmash framework in order to employ Kubernetes specifics, 
like for example the default OLM namespace and catalog source.

### Running the tests

The simplest test execution can be performed via a `mvn clean verify` command.

### Implemented tests

#### [WildFly (JBoss EAP XP) MiroProfile Reactive Messaging + Kafka (Streams for ApacheKafka)](testsuite/wildfly-microprofile-reactive-messaging-kafka/src/test/java/org/jboss/intersmash/tests/wildfly/microprofile/reactive/messaging/kafka/WildflyMicroProfileReactiveMessagingPerConnectorSecuredIT.java)

This test validates an interoperability use case based on a WildFly (JBoss EAP XP) MicroProfile Reactive
Messaging application, which interacts with a remote Kafka (Streams for Apache Kafka) service.
See the [WildflyMicroProfileReactiveMessagingPerConnectorSecuredTests](testsuite/wildfly-microprofile-reactive-messaging-kafka/src/test/java/org/jboss/intersmash/tests/wildfly/microprofile/reactive/messaging/kafka/WildflyMicroProfileReactiveMessagingPerConnectorSecuredIT.java) class Javadoc for more details.

## Profiles

### Executing tests based on target platform

The default test execution will exclude tests that are expected to run on OpenShift only, as for 
instance those that involve an s2i build. 

By default, i.e. when no profiles are enabled, the Maven Failsafe Plugin is configured to use the
[global-test.openshift.properties](global-test.openshift.properties)
file, so that the Intersmash framework will run tests on OpenShift, and leverage OpenShift cluster specifics - like the
default OLM namespace and catalog source.

### WildFly, JBoss EAP and JBoss EAP XP related profiles

Tests involving WildFly and the related products (i.e. JBoss EAP and JBoss EAP XP) are executed by using 
the community version of the involved applications (WildFly) and cloud related deliverables, e.g.: images, Helm Charts
etc. by default. 

Such values can be overridden via system properties.

#### Executing tests based on the target distribution

- `wildfly-target-distribution.jboss-eap`

When this profile is enabled, _application descriptors_ that implement the
[WildflyApplicationConfiguration](./core/src/main/java/org/jboss/intersmash/tests/wildfly/WildflyApplicationConfiguration.java)
interface will generate additional Maven args that will be forwarded to a remote s2i build, so that the tested
application will be built accordingly.
Additionally, the Maven Failsafe Plugin will use the 
[global-test.jboss-eap.openshift.properties](global-test.jboss-eap.openshift.properties) defaults
file in order to configure the Intersmash framework, so that JBoss EAP cloud deliverables - e.g.: images and Helm 
Charts -  will be used during the test execution.

Such values can be overridden via system properties.

- `wildfly-target-distribution.jboss-eap-xp`

When this profile is enabled, _application descriptors_ that implement the
[WildflyApplicationConfiguration](./core/src/main/java/org/jboss/intersmash/tests/wildfly/WildflyApplicationConfiguration.java)
interface will generate additional Maven args that will be forwarded to a remote s2i build, so that the tested
application will be built accordingly.
Additionally, the Maven Failsafe Plugin will use the
[global-test.jboss-eap-xp.openshift.properties](global-test.jboss-eap-xp.openshift.properties)
file in order to configure the Intersmash framework, so that JBoss EAP XP cloud deliverables - e.g.: images and Helm
Charts -  will be used during the test execution.

- `wildfly-target-distribution.jboss-eap-xp.merged-channel`

Similar to `wildfly-target-distribution.jboss-eap-xp` but when forwarded to the remote s2i build, it will enable 
a dedicated profile to provision the tested JBoss EAP XP application by using a single - i.e. merged - channel 
manifest, as when testing JBoss EAP candidate releases. 

Such values can be overridden via system properties.

**IMPORTANT**:
- When using `-Pwildfly-target-distribution.jboss-eap` the JBoss EAP 8.1 **Beta** GA deliverables will be used by default,
  since JBoss EAP 8.1.0 is still not available.
- When using `-Pwildfly-target-distribution.jboss-eap-xp` the JBoss EAP XP **5.x** GA deliverables will be used by default,
  since JBoss EAP XP 6 is still not available.

## Modules

### [intersmash-tests-core](./core)

This module contains annotations used to decorate test classes, specifically JUnit 5 `@Tag` Java interfaces which can be 
used to selectively execute groups of tests.

### [intersmash-tests-testsuite](./testsuite)

This module contains the actual Intersmash tests, i.e. integration tests that verify runtimes interoperability scenarios 
on OpenShift.

### [style-config](./style-config)

Utility module that holds the resources needed to perform code style validation and formatting. 