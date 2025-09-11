# Intersmash Tests - WildFly Web cache offload + Infinispan

This tests validates an interoperability use case based on a WildFly/JBoss EAP/JBoss EAP XP application which 
interacts with a remote Infinispan/Red Hat Data Grid service.

The application is configured to use a invalidation cache backed by the remote Infinispan/Red Hat Data Grid service,
and environment variables are set conveniently in [the relevant application descriptor](src/test/java/org/jboss/intersmash/tests/wildfly/web/cache/offload/infinispan/WildflyOffloadingSessionsToInfinispanApplication.java). 
