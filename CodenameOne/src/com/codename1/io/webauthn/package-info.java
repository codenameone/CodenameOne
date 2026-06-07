/// WebAuthn / passkey client.
///
/// `WebAuthnClient` wraps the OS public-key credential APIs
/// (`ASAuthorizationPlatformPublicKeyCredentialProvider` on iOS 16+,
/// `androidx.credentials.CredentialManager` on Android API 28+) behind a
/// portable, JSON-friendly Java surface, letting an app register and
/// authenticate passkeys against any standards-compliant relying party --
/// your own backend, Auth0, Firebase, etc.
///
/// `PublicKeyCredentialCreationOptions` /
/// `PublicKeyCredentialRequestOptions` describe the ceremony parameters,
/// `PublicKeyCredential` carries the result, and `WebAuthnException`
/// reports cancellation / platform errors. `WebAuthnNative` is the SPI the
/// active port implements.
package com.codename1.io.webauthn;
