# Intersmash Tests - WildFly Elytron OIDC client + Keycloak

This tests validates an interoperability use case based on a WildFly/JBoss EAP/JBoss EAP XP application that 
uses the Elytron subsystem to configure an OIDC client for a remote Keycloak/Red Hat Build of Keycloak service,
which is configured to allow OIDC Single-sign-on in order to secure the application resources.

The deployed application descriptor sets the `SSO_APP_SERVICE` environment variable to the URL of the Keycloak service.
