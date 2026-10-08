---
title: "Easy Full Stack Authentication and Login in the Style of Spring"
slug: sign-in-client-server-contract
url: /blog/sign-in-client-server-contract/
date: '2026-10-10'
author: Shai Almog
description: "Connect client token renewal to server access rules, persistence and end-to-end tests. Understand the supported security mechanisms and the different browser testing boundary."
feed_html: '<img src="https://www.codenameone.com/blog/sign-in-client-server-contract.jpg" alt="A client session connects to server access rules and verification" /> Connect client token renewal to server access rules, persistence and end-to-end tests. Understand the supported security mechanisms and the different browser testing boundary.'
series: ["release-2026-10-09"]
---

![A client session connects to server access rules and verification](/blog/sign-in-client-server-contract.jpg)

Three requests leave your app just as its access token expires. If each refreshes independently, the first exchange can invalidate the refresh token the other two are about to use. A login feature becomes a race between requests.

This week's security work joins the Codename One client and backend around those transitions. The client can coordinate renewal, the server can rotate tokens, and tests can verify what happens after a real request is refused.

## Put the access rule beside the route

The backend has Spring-style security chains under `com.codename1.backend.security`. A chain chooses which requests it handles, then authenticates and authorizes them. This excerpt from the [security guide example](https://github.com/codenameone/CodenameOne/blob/ebe0e64be3/docs/demos/backend/src/main/java/com/codenameone/developerguide/backend/security/SecurityConfig.java) protects an API using JWT scopes:

```java
@Bean
@Order(1)
SecurityFilterChain api(HttpSecurity http) {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/orders/**").hasAuthority("SCOPE_orders:read")
            .anyRequest().authenticated())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
}
```

Configure the trusted issuer and the audience your API expects. The guide's configuration keys include `cn1.security.oauth2.resourceserver.jwt.issuer-uri` and `cn1.security.oauth2.resourceserver.jwt.audiences`. Audience checking is optional unless configured, so set it deliberately when several services share an issuer.

**A backend with no `SecurityFilterChain` bean has no security layer.** Adding a dependency does not secure the routes. A stateless chain also has no CSRF filter unless you request one. Session-based browser applications have a different threat model and need the corresponding session and CSRF configuration.

Method rules such as `@PreAuthorize` are compiled into checks. They can apply to self-calls and private methods because the build weaves them into the method, rather than relying on calls passing through a runtime proxy. The expression language is a supported subset; ACLs, `@PostAuthorize` and arbitrary expression evaluation are outside it.

## Give the client one owner for renewal

Register an installed app as a public OAuth client with an exact redirect URI and PKCE. A client secret embedded in an app would not remain secret. After configuring `OidcClient`, install an authorizer for the API base URL:

```java
OidcRequestAuthorizer authorizer =
        new OidcRequestAuthorizer(client).install("https://api.example.com/v1");
authorizer.addSignInRequiredListener((source, reason) -> showSignIn());
authorizer.load().ready(saved -> {
    if (saved == null) {
        showSignIn();
    } else {
        showNotes();
    }
});
```

This is an application excerpt: `client` is configured with your provider and token store; `showSignIn()` and `showNotes()` are your UI methods. Use `SecureStorageTokenStore` for platform-protected persistence. Plain storage fallback is an explicit option, not something the app should silently enable to make an error disappear.

The authorizer matches the scheme, host and port, then whole path segments. `/v1` includes `/v1/notes`, but not `/v10`. It covers ordinary `ConnectionRequest` calls, the REST builder and generated clients through the network layer.

{{< mermaid >}}
sequenceDiagram
    participant App as App requests
    participant Auth as Request authorizer
    participant Server as Backend
    App->>Auth: Queue several requests
    Auth->>Server: One refresh exchange
    Server-->>Auth: New access and refresh tokens
    Auth->>Auth: Persist tokens
    Auth->>Server: Send waiting requests
    Server-->>App: Deliver responses
{{< /mermaid >}}

Renewal starts before expiry by default. Requests queued during the exchange wait for its result. If a token-bearing request receives `401`, the authorizer can refresh and retry it once; the caller receives the eventual response. A refused refresh ends the session, clears stored tokens and calls the sign-in listener. If your request writes a body from a stream, verify that your implementation can supply it again before depending on a retry.

For redirects followed by Codename One, the token is removed outside the registered origin or path. The current iOS port lets the system follow redirects before application code sees them. Keep protected API redirects within hosts you control; do not infer that every transport has the same redirect interception point.

## Choose the state your deployment can share

The backend includes form login, HTTP Basic, API keys, JWT resource servers, OAuth login and authorization-server support. MFA includes TOTP and recovery codes; passkey endpoints use WebAuthn. These are separate mechanisms with separate configuration, not one switch that makes every deployment secure.

Several instances cannot share an in-memory token repository or rate limiter. Declare the database-backed stores when that is your deployment shape. `cn1.security.schema.enabled=true` belongs in the module's build-time `application.properties`; it registers the security migrations. It creates tables, but **does not select JDBC stores for you**.

Refresh tokens and one-use codes must be consumed atomically. Otherwise two instances can both accept the same secret. The JDBC implementations use conditional database operations and their affected-row counts for those decisions. TOTP secrets are sealed under the configured encryption key; password hashes and one-use recovery codes have their own storage rules.

The packaged server links the mechanisms its chains reach. A token-verifying API need not carry login pages and a password store. That is a useful consequence of explicit configuration and dead-code removal, although it does not reduce the need to review the rules you enabled.

## Test the failure after the successful login

The [end-to-end client tests](https://github.com/codenameone/CodenameOne/tree/ebe0e64be3/scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/backend) connect to the HelloCodenameOne backend. `BackendTokenRefreshTest` deliberately replaces an access token with one the server did not sign. It checks that the generated client receives one successful response after renewal, that the refresh token rotates, and that reusing a spent refresh token ends the session.

That proves more than a mock that always supplies an authenticated user. It exercises the client's networking and token state together with the server's token endpoint and protected route.

| Test layer | What it can establish | What it does not establish |
| --- | --- | --- |
| Backend `MockMvc` and mock identities | Route and method authorization decisions | A provider login or signature verification when identity is injected |
| Native client integration tests | Requests, token exchange, renewal and denial against the backend | Every external identity provider or device sign-in surface |
| Browser-specific tests | Browser networking and credential APIs, including the virtual-authenticator vault test | Passing the native credential/redirect harness unchanged |
| Port test matrix | The declared cases on each supported target | Every security mechanism on every port |

The credential/redirect tests explicitly report a browser skip because the browser owns those cookies and redirects. A green port suite must be read with those skips. WebAuthn PRF bridge coverage uses Chromium's virtual authenticator; it is not a claim that every hardware authenticator supports that extension.

SAML, LDAP, opaque-token introspection and several authorization-server extensions are outside this release. The [backend security guide](/developer-guide/backend-security/) and [client identity guide](/developer-guide/authentication-and-identity/) specify the supported surface. [PR #5961](https://github.com/codenameone/CodenameOne/pull/5961) connects those pieces; tomorrow we will look at the migrations underneath their persistent stores.

## Discussion

_Which failure is hardest to reproduce in your sign-in flow: renewal, revocation, or returning from the provider?_

{{< giscus >}}
