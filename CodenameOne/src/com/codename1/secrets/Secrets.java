/*
 * Codename One - secure secrets storage.
 */
package com.codename1.secrets;

import java.util.List;

/// A simple, secure-by-default key/value store for app secrets (auth tokens,
/// API keys, refresh tokens, anything you must not keep in
/// [com.codename1.io.Preferences] or [com.codename1.io.Storage] as
/// plaintext).
///
/// On a device with a platform keychain wired in (iOS Keychain Services,
/// Android Keystore) the values are protected by the OS secure enclave; if no
/// hardware store is registered, [Secrets] falls back to AES-encrypted
/// storage so a value is never written in the clear. Either way the developer
/// uses the same four calls:
///
/// ```java
/// Secrets.set("auth.token", token);
/// String token = Secrets.get("auth.token");
/// if (Secrets.contains("auth.token")) { ... }
/// Secrets.delete("auth.token");
/// ```
///
/// [#isHardwareBacked()] reports whether the active store is the OS
/// keychain (vs the software fallback). A platform port or cn1lib installs a
/// hardware store with [#setStore(SecretsStore)] during initialisation;
/// see [SecretsStore].
public final class Secrets {

    private static SecretsStore store;

    private Secrets() {
    }

    /// Install the backing store (called by a platform port / keychain cn1lib at init).
    public static synchronized void setStore(SecretsStore s) {
        store = s;
    }

    private static synchronized SecretsStore store() {
        if (store == null) {
            store = new DefaultSecretsStore();
        }
        return store;
    }

    /// Store (or replace) `value` under `key`.
    public static void set(String key, String value) {
        requireKey(key);
        store().set(key, value);
    }

    /// The secret under `key`, or `null` if absent.
    public static String get(String key) {
        requireKey(key);
        return store().get(key);
    }

    /// The secret under `key`, or `defaultValue` if absent.
    public static String get(String key, String defaultValue) {
        String v = get(key);
        return v == null ? defaultValue : v;
    }

    /// Whether a secret is stored under `key`.
    public static boolean contains(String key) {
        requireKey(key);
        return store().contains(key);
    }

    /// Remove the secret stored under `key` (no-op if absent).
    public static void delete(String key) {
        requireKey(key);
        store().delete(key);
    }

    /// Keys of all stored secrets (never the values).
    public static List<String> keys() {
        return store().keys();
    }

    /// Whether the active store is the OS keychain (vs the AES software fallback).
    public static boolean isHardwareBacked() {
        return store().isHardwareBacked();
    }

    private static void requireKey(String key) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("secret key must be non-empty");
        }
    }
}
