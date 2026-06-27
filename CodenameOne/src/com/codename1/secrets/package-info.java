/// Secure, secure-by-default storage for app secrets (auth tokens, API keys,
/// refresh tokens) that must never be written to
/// [com.codename1.io.Preferences] or [com.codename1.io.Storage] as plaintext.
///
/// [com.codename1.secrets.Secrets] is the entry point: a small
/// `set`/`get`/`contains`/`delete`/`keys` API. On a device with a platform
/// keychain wired in (iOS Keychain Services, Android Keystore) values are
/// protected by the OS secure enclave; otherwise a software fallback
/// AES-256-CBC encrypts and HMAC-SHA256 authenticates each value at rest.
/// The backend is pluggable through the [com.codename1.secrets.SecretsStore]
/// SPI.
package com.codename1.secrets;
