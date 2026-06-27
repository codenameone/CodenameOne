/*
 * Codename One - secure secrets storage SPI.
 */
package com.codename1.secrets;

import java.util.List;

/// The pluggable backend behind [Secrets]. A platform port or cn1lib
/// registers a hardware-backed implementation (iOS Keychain Services, Android
/// Keystore / EncryptedSharedPreferences, Windows DPAPI, macOS Keychain) via
/// [Secrets#setStore(SecretsStore)] so secrets are protected by the
/// operating system's secure enclave. When none is registered, [Secrets]
/// falls back to `DefaultSecretsStore` (AES-encrypted at rest in
/// `Storage`), which is secure-by-default but software-only.
///
/// Implementations must treat keys and values as opaque UTF-8 strings and
/// must never log or transmit plaintext values.
public interface SecretsStore {

    /// Store (or replace) the secret `value` under `key`.
    void set(String key, String value);

    /// Return the secret stored under `key`, or `null` if absent.
    String get(String key);

    /// Whether a secret is stored under `key`.
    boolean contains(String key);

    /// Remove the secret stored under `key` (no-op if absent).
    void delete(String key);

    /// The keys of every stored secret (never the values).
    List<String> keys();

    /// Whether this store is backed by the device's hardware/OS keychain
    /// (true) rather than the software AES-at-rest fallback (false). Lets an
    /// app decide whether to store especially sensitive material.
    boolean isHardwareBacked();
}
