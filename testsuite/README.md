# Intersmash Tests

## WildFly MicroProfile Reactive Messaging + Kafka

This tests validates an interoperability use case based on a WildFly/JBoss EAP XP MicroProfile Reactive
Messaging interacting with a remote Kafka/Streams for Apache Kafka service.

## WildFly Elytron OIDC client + Keycloak

This tests validates an interoperability use case based on a WildFly/JBoss EAP/JBoss EAP XP application that 
uses the Elytron subsystem to configure an OIDC client for a remote Keycloak/Red Hat Build of Keycloak service,
which is configured to allow OIDC Single-sign-on in order to secure the application resources.

The deployed application descriptor sets the `SSO_APP_SERVICE` environment variable to the URL of the Keycloak service.

## WildFly Web cache offload + Infinispan

This tests validates an interoperability use case based on a WildFly/JBoss EAP/JBoss EAP XP application which
interacts with a remote Infinispan/Red Hat Data Grid service.

The application is configured to use an invalidation cache backed by the remote Infinispan/Red Hat Data Grid service,
and environment variables are set conveniently in [the relevant application descriptor](src/test/java/org/jboss/intersmash/tests/wildfly/web/cache/offload/infinispan/WildflyOffloadingSessionsToInfinispanApplication.java). 