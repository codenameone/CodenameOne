# Universal Sign-In Demo

A single-screen Codename One app demonstrating the modernized sign-in stack from Codename One 8.0:

- `com.codename1.io.oidc.OidcClient` -- generic OpenID Connect / OAuth 2.0 client (PKCE, discovery, system browser, refresh)
- `com.codename1.social.AppleSignIn` -- Sign in with Apple (native on iOS 13+, web fallback elsewhere)
- `com.codename1.social.GoogleConnect#signIn` -- Google Identity Services
- `com.codename1.social.FacebookConnect#signIn` -- Facebook Login via system browser
- `com.codename1.social.MicrosoftConnect` -- Microsoft / Entra ID
- `com.codename1.social.Auth0Connect` -- Auth0 tenant
- `com.codename1.social.FirebaseAuth` -- Firebase Authentication (email/password + IdP exchange)

The app puts one button per provider on the screen, runs the chosen flow, and writes the result (or error) into a `TextArea`. It is intentionally tiny so it serves as a copy-paste reference for your own integration.

## What you need before running

Replace the credential constants at the top of `UniversalSignInDemo.java` with your own:

| Provider   | Required values                                                                 |
|------------|---------------------------------------------------------------------------------|
| Google     | OAuth 2.0 Web Client ID (Google Cloud Console → Credentials)                    |
| Microsoft  | Entra ID app registration Client ID (Azure portal → App registrations)          |
| Facebook   | Facebook App ID + a Valid OAuth Redirect URI configured in the app dashboard    |
| Auth0      | Tenant domain (`tenant.region.auth0.com`) + Application Client ID               |
| Apple      | *Services ID* + a redirect URI registered on it + a server-minted client secret |
| Firebase   | Web API key (Firebase Console → Project Settings → General)                     |

Custom-scheme redirect URIs (`com.codename1.samples.signin:/oauth2redirect`) must be registered with the OS:

- **iOS** -- add build hint `ios.urlScheme=com.codename1.samples.signin:`
- **Android** -- the build cloud auto-registers the scheme based on your `cn1.useCustomScheme` build hint; or add it manually to `AndroidManifest.xml` if you ship a custom one.

## How it works

Every provider flows through `SystemBrowser.authenticate(authUrl, redirectUri)`, which dispatches to:

1. The native sign-in sheet (iOS `ASWebAuthenticationSession` / Android Custom Tabs) if a `com.codename1.io.oidc.OidcBrowserNative` is available on the platform.
2. The Codename One `BrowserWindow` otherwise.

The bottom of the screen tells you which path you are on.

## Why this replaces `Oauth2`

The legacy `com.codename1.io.Oauth2` class drives sign-in through an embedded `WebBrowser` component. Google, Apple, Microsoft and Facebook all now refuse to render their sign-in pages inside embedded views (`disallowed_useragent` and similar errors). `OidcClient` solves that by using the OS-provided system browser, which the providers do accept.

## Further reading

- Developer Guide → *Authentication and Identity* chapter
- `com.codename1.io.oidc` package Javadoc
