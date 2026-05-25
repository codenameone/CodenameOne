/// OpenID Connect (OIDC) client with PKCE.
///
/// `OidcClient` drives the Authorization Code + PKCE flow against any
/// standards-compliant identity provider (Auth0, Okta, Google, Microsoft
/// Entra, Keycloak, ...). It launches a system browser via
/// `SystemBrowser` / `OidcBrowserNative`, exchanges the authorization code
/// for tokens, and persists them through a pluggable `TokenStore`.
///
/// Companion classes (`OidcConfiguration`, `OidcTokens`, `PkceChallenge`,
/// `OidcException`) carry configuration, results and errors.
package com.codename1.io.oidc;
