---
title: OIDC, Sign In With Apple, And Passkeys In The Core
slug: oidc-and-passkeys
url: /blog/oidc-and-passkeys/
date: '2026-06-01'
author: Shai Almog
description: A modern OpenID Connect client backed by ASWebAuthenticationSession on iOS and Custom Tabs on Android, Sign in with Apple in core, refreshed Google / Facebook / Microsoft / Auth0 / Firebase wrappers, and a portable WebAuthn / passkey client. The legacy in-app-WebView Oauth2 flow is deprecated.
feed_html: '<img src="https://www.codenameone.com/blog/oidc-and-passkeys.jpg" alt="OIDC, Sign In With Apple, And Passkeys In The Core" /> A modern OpenID Connect client backed by ASWebAuthenticationSession on iOS and Custom Tabs on Android, Sign in with Apple in core, refreshed provider wrappers, and a portable WebAuthn / passkey client. The legacy Oauth2 flow is deprecated.'
---

![OIDC, Sign In With Apple, And Passkeys In The Core](/blog/oidc-and-passkeys.jpg)

This one has been overdue. The in-app-WebView based `Oauth2` flow that Codename One has shipped since approximately forever was the way every cross-platform mobile framework solved the "sign in with Google / Facebook / Microsoft" problem in the 2010s. It is also the way every one of those identity providers stopped wanting you to solve it. Google has been blocking embedded user agents for years. Apple does not want third-party apps wrapping the Apple ID flow in a `WKWebView`. Microsoft and Facebook joined the chorus. The right answer is the system browser: `ASWebAuthenticationSession` on iOS, Custom Tabs on Android, with PKCE on the wire. That is what [PR #5018](https://github.com/codenameone/CodenameOne/pull/5018) does.

The companion [PR #5039](https://github.com/codenameone/CodenameOne/pull/5039) layers a portable passkey / WebAuthn client on top, for the cases where you want passwordless sign-in against your own backend or against an identity provider that exposes WebAuthn directly.

Both ship in the core; no cn1lib. Documentation is at [Authentication-And-Identity.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Authentication-And-Identity.asciidoc).

## OidcClient: the system-browser flow

`com.codename1.io.oidc.OidcClient` is the new entry point. Point it at an issuer that publishes a `.well-known/openid-configuration`, hand it the client id and the redirect URI you registered with the provider, ask for tokens:

```java
OidcConfiguration cfg = OidcConfiguration.discover("https://accounts.google.com");

OidcClient client = OidcClient.builder()
        .configuration(cfg)
        .clientId("123-abc.apps.googleusercontent.com")
        .redirectUri("com.example.myapp:/oauthredirect")
        .scopes("openid", "email", "profile")
        .build();

client.signIn().onResult((tokens, err) -> {
    if (err != null) {
        OidcException oe = (OidcException) err;
        if (oe.getCode() == OidcException.USER_CANCELLED) return;
        showError(oe);
        return;
    }
    String idToken = tokens.getIdToken();
    String email   = tokens.getIdToken().getClaim("email").asString();
    proceed();
});
```

The underlying flow is the modern one. Discovery JSON parsed and cached. PKCE S256 challenge generated and verified. State and nonce checked on the callback. ID token claims decoded for you (we deliberately do not verify the signature client-side; the developer guide is explicit about why and points at the "re-validate on your backend" remedy). Refresh and revoke are first-class. The token store is pluggable via `TokenStore`; the default is the `Storage`-backed implementation, but Keychain-backed and in-memory variants are reasonable to plug in.

The system-browser piece is hidden behind a `SystemBrowser` facade. On iOS it routes through `ASWebAuthenticationSession`, which is the API Apple wants you to use; the system handles the cookie jar, the Safari sandbox, and the dismiss-on-redirect; it ASKs for sign-in-with-the-identity-provider rather than wraps it. On Android it routes through `androidx.browser.customtabs`, with a plain `ACTION_VIEW` fallback for the rare device that has no Custom Tabs provider. The Maven plugin auto-links `AuthenticationServices.framework` on iOS and `androidx.browser:browser:1.8.0` on Android when the classpath scanner sees `OidcClient` in use. Apps that do not use OIDC pay nothing.

The provider wrappers are the same flow with the issuer pre-wired:

```java
GoogleConnect.signIn(googleClientId, "com.example.myapp:/oauthredirect",
                    "openid", "email", "profile")
    .onResult((tokens, err) -> { /* ... */ });

MicrosoftConnect.signIn(entraClientId, "msauth.com.example.myapp://auth",
                       "User.Read")
    .onResult((tokens, err) -> { /* ... */ });

Auth0Connect.signIn("tenant.auth0.com", clientId, redirectUri,
                   "openid profile email")
    .onResult((tokens, err) -> { /* ... */ });
```

`FacebookConnect.signIn(...)` follows the same shape against the Facebook OIDC endpoint. `FirebaseAuth` covers the REST-based Firebase auth surface (email and password, IdP token exchange, refresh) which sits underneath identity-provider hand-offs you might want to drive from app code.

## Sign in with Apple

Sign in with Apple moves into the core too, as `com.codename1.social.AppleSignIn`. On iOS 13 and later this is the native flow (`ASAuthorizationAppleIDProvider`), which shows the Apple sheet rather than a browser tab and is the only experience Apple permits in production. On non-iOS platforms it falls through to the same OIDC web flow as everything else, so a single line of app code works on every port:

```java
AppleSignIn.signIn()
    .onResult((result, err) -> {
        if (err != null) return;
        String idToken = result.getIdToken();
        String code    = result.getAuthorizationCode();
        proceedToBackend(idToken, code);
    });
```

The Maven plugin injects the `com.apple.developer.applesignin` entitlement on iOS when it sees `AppleSignIn` in use; Android Studio and the rest of your build do not see it because it is not there. The same scanner gating we apply to NFC, biometrics, and now WiFi.

## The legacy Oauth2 is deprecated

`com.codename1.io.Oauth2` is now deprecated. It still works (we deliberately did not break it; apps that were relying on it on a platform that has not started rejecting WebView-based auth yet keep working). The deprecation message points at the migration recipe in the dev guide. The recipe is short: replace the issuer URL and the credentials with their `OidcConfiguration` equivalents, swap the `accessToken()` call for `signIn()`, delete the `setBrowserComponent(...)` wiring. The result is shorter, the result works against providers that reject the legacy flow, and the cookie state is owned by the operating system instead of by a `WKWebView` you have to manage.

## Passkeys / WebAuthn

The second PR ([#5039](https://github.com/codenameone/CodenameOne/pull/5039)) lands `com.codename1.io.webauthn`. The model is the W3C WebAuthn JSON wire format, the same way every relying party expects it:

```java
WebAuthnClient client = WebAuthnClient.getInstance();

if (!client.isAvailable()) {
    fallbackToPassword();
    return;
}

PublicKeyCredentialCreationOptions opts =
        PublicKeyCredentialCreationOptions.fromServerJson(serverJson);

client.create(opts).onResult((cred, err) -> {
    if (err != null) {
        WebAuthnException w = (WebAuthnException) err;
        if (w.getCode() == WebAuthnException.USER_CANCELLED) return;
        showError(w);
        return;
    }
    postToRelyingParty(cred.toJson());
});
```

The native bindings:

- iOS 16 and later: `ASAuthorizationPlatformPublicKeyCredentialProvider`. Same modal that Safari uses for Face ID / Touch ID passkeys, attested by the Secure Enclave.
- Android API 28 and later: `androidx.credentials.CredentialManager`, which is Google's umbrella for the Credential Manager APIs that talk to the Google Password Manager / FIDO2 platform credentials.

Sign-in mirrors the same shape:

```java
PublicKeyCredentialRequestOptions opts =
        PublicKeyCredentialRequestOptions.fromServerJson(serverJson);

client.get(opts).onResult((cred, err) -> { /* ... */ });
```

The data interchange is W3C JSON in both directions, so the response can be POSTed verbatim to any standard server-side WebAuthn library; `webauthn4j` on Java, `@simplewebauthn/server` on Node, `webauthn-rs` on Rust, whichever your backend stack reaches for.

Two provider-specific helpers ride on top:

- `Auth0Connect.signInWithPasskey(...)` / `.registerPasskey(...)` against Auth0's WebAuthn grant.
- `FirebaseAuth.signInWithPasskey(...)` / `.registerPasskey(...)` against the Firebase Identity Platform v2 passkey endpoints.

Both of those are the cases where the WebAuthn ceremony is genuinely client-side rather than wrapped behind an OIDC redirect.

## Why a separate PR for WebAuthn

This is worth a paragraph because the question keeps coming up: if you sign in via OIDC against Google, Apple, Microsoft, Auth0, or Firebase, you usually already get passkeys for free. The identity provider runs the WebAuthn ceremony inside the system browser; OIDC just hands you the resulting tokens. So why have a separate WebAuthn surface at all?

Two reasons. First, apps that run their own relying-party backend and want passwordless sign-in against it. Those do not have a federated IdP doing the ceremony; the ceremony has to happen in the app. Second, Auth0 and Firebase both offer client-side WebAuthn grants in addition to their hosted UI; those want a JSON ceremony, not a redirect flow. The new `WebAuthnClient` is the right shape for both of those cases.

If you are using OIDC against Google / Apple / Microsoft / a hosted Auth0 / a hosted Firebase, you already have passkeys for the platforms that have them set up. You do not need this PR for that. The dev guide section calls this out explicitly so nobody adds a WebAuthn dependency they do not need.

## A note on the entitlements story

Every piece of native gating in both PRs follows the same pattern we have been applying for a few weeks now: the classpath scanner in `IPhoneBuilder` / `AndroidGradleBuilder` flips a single per-feature flag (`usesOidc`, `usesAppleSignIn`, `usesWebAuthn`), and that flag is the thing that drives framework linking, entitlement injection, plist injection, and Objective-C conditional compilation. Apps that do not use a feature do not pay for it at App Store review time, and the binary does not even contain the platform calls.

The companion cloud build server changes ship together so local builds and cloud builds match.

## Wrapping up

The new stack is the one to reach for. The migration from `Oauth2` is a thirty-line edit for most apps and shorter on the platforms with first-class wrappers. The full chapter is at [Authentication-And-Identity.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Authentication-And-Identity.asciidoc), and there is a copy-pasteable demo at [Samples/samples/UniversalSignInDemo/](https://github.com/codenameone/CodenameOne/tree/master/Samples/samples/UniversalSignInDemo) that has one button per provider plus a generic OIDC issuer.

Tomorrow's post covers the Share result callback API.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
