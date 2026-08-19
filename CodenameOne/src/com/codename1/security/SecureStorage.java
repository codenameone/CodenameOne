/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.security;

import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/// Biometric-gated secure storage backed by the platform keychain. Reading
/// an entry prompts the user for biometric authentication; writing or
/// deleting may or may not, depending on the platform.
///
/// Entries are bound to the current set of enrolled biometrics. If the user
/// adds a fingerprint, enrols a new face, or disables device security, every
/// stored entry is automatically invalidated and subsequent
/// [#get(String, String)] calls fail with [BiometricError#KEY_REVOKED]. The
/// application must then re-prompt the user for the original value and
/// [#set(String, String, String)] it again.
///
/// Use this for short, secret strings (auth tokens, refresh tokens,
/// encryption keys). For larger data, encrypt with a key stored here.
///
/// #### Platform support
///
/// - **iOS** -- backed by Security.framework (`SecItemAdd` /
///   `SecItemCopyMatching` / `SecItemDelete`) with
///   `kSecAccessControlTouchIDCurrentSet`. Sharing entries with App
///   Extensions requires both the `ios.keychainAccessGroup` build hint AND
///   a call to [#setKeychainAccessGroup(String)] passing the same
///   Team-ID-prefixed group identifier.
/// - **Android** -- AES/CBC/PKCS7 ciphertext stored in `SharedPreferences`
///   with the key in the `AndroidKeyStore`, locked via
///   `setUserAuthenticationRequired(true)`. The `BiometricPrompt` (API 29+)
///   or `FingerprintManager` (API 23-28) unlocks the cipher for one
///   operation per prompt.
/// - **JavaSE simulator** -- backed by `java.util.prefs.Preferences`, gated
///   on the same Biometric Simulation menu used by [Biometrics]. Useful for
///   testing the round-trip and `KEY_REVOKED` paths without a device.
/// - **All other platforms** -- this base class is returned as-is and acts
///   as a non-supporting fallback: every method completes with
///   [BiometricError#NOT_AVAILABLE]. Application code does not need
///   platform `if` statements.
public class SecureStorage {

    /// Subclasses are constructed by the port. Application code obtains the
    /// active instance via [#getInstance()].
    protected SecureStorage() {
    }

    /// Returns the platform-specific singleton owned by the current port.
    /// On ports that do not implement secure storage this returns a base
    /// [SecureStorage] instance whose methods report
    /// [BiometricError#NOT_AVAILABLE].
    public static SecureStorage getInstance() {
        SecureStorage s = Display.getInstance().getSecureStorage();
        return s != null ? s : DEFAULT;
    }

    private static final SecureStorage DEFAULT = new SecureStorage();

    /// Retrieves a previously-stored entry, prompting for biometric
    /// authentication. The returned `AsyncResource` completes with the
    /// value, or with a [BiometricException] on failure (including
    /// [BiometricError#KEY_REVOKED] when biometrics have been re-enrolled
    /// since the entry was written). On the fallback base class this
    /// completes immediately with [BiometricError#NOT_AVAILABLE].
    public AsyncResource<String> get(String reason, String account) {
        AsyncResource<String> r = new AsyncResource<String>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Secure storage is not available on this platform"));
        return r;
    }

    /// Stores or overwrites a value for the given account. On iOS the user
    /// is typically not prompted (Apple's keychain accepts writes without
    /// re-authenticating); on Android the user is prompted because the
    /// underlying cipher requires biometric authentication. On the fallback
    /// base class this completes immediately with
    /// [BiometricError#NOT_AVAILABLE].
    public AsyncResource<Boolean> set(String reason, String account, String value) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Secure storage is not available on this platform"));
        return r;
    }

    /// Removes a previously-stored entry. No authentication is required
    /// since deletion does not reveal the value. On the fallback base class
    /// this completes immediately with [BiometricError#NOT_AVAILABLE].
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> r = new AsyncResource<Boolean>();
        r.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                "Secure storage is not available on this platform"));
        return r;
    }

    /// Configures the iOS keychain access group for sharing entries between
    /// the main app and its extensions. The argument must include the Team
    /// ID prefix (e.g. `"ABCDE12345.group.com.example.app"`). Pass `null`
    /// or empty to clear. Ignored on non-iOS platforms and on the fallback
    /// base class.
    ///
    /// The `ios.keychainAccessGroup` build hint must declare the same group
    /// in the app's entitlements for this to work.
    public void setKeychainAccessGroup(String group) {
        // No-op fallback.
    }

    // -----------------------------------------------------------------
    // Non-prompting (no-biometric) storage
    // -----------------------------------------------------------------
    //
    // The methods above all gate reads on biometric authentication.
    // That is the right contract for refresh tokens and other things
    // the user actively unlocks, but it is the wrong contract for
    // secrets that the app needs to read on every network call --
    // notably LLM API keys, where prompting at chat-call time would
    // be unusable.
    //
    // The single-argument overloads below provide a quieter store
    // that the platform persists with the OS's strong-but-non-
    // interactive secrets backend:
    //
    // - iOS: keychain with `kSecAttrAccessibleAfterFirstUnlock`, no
    //   `SecAccessControl`. Entries survive app updates and OS
    //   reboots; they are extracted only after the user unlocks the
    //   device at least once after each reboot.
    // - Android: AES-GCM under a dedicated AndroidKeyStore key created
    //   without `setUserAuthenticationRequired(true)`, persisted to a
    //   private preferences file. No biometric prompt. Devices below
    //   API 23 fall back to obfuscated (not encrypted) storage.
    // - JavaSE simulator: `java.util.prefs.Preferences` encrypted
    //   with an AES key derived from the OS user account. Useful
    //   for round-tripping `LlmClient.openai(SecureStorage.getInstance().get("openai_key"))`
    //   during simulator runs without storing the key in plaintext
    //   in the project tree.
    // - All other platforms: the base class returns null / false so
    //   the call is observable but harmless. Treat `null` from
    //   `get(account)` the same way you'd treat "not configured".

    /// Quietly stores or overwrites an entry under `account`. The
    /// user is not prompted. Returns `false` on the fallback base
    /// class.
    public boolean set(String account, String value) {
        return false;
    }

    /// Quietly retrieves a previously-stored entry. Returns `null`
    /// when the entry does not exist or when the platform does not
    /// provide non-prompting storage.
    public String get(String account) {
        return null;
    }

    /// Quietly removes an entry. Returns `false` on the fallback
    /// base class.
    public boolean remove(String account) {
        return false;
    }

    /// The entry is there.
    /// Stores a value only if this account has none, and reports what the store ended up holding.
    ///
    /// The operation a first-time key needs. Reading, generating and storing as three steps is
    /// safe within one process and not between two: `synchronized` covers threads in one VM, while
    /// an application can be opened from more than one -- Android components declared with their
    /// own `android:process`, or simply two runs of a desktop build -- and both can see nothing
    /// stored, generate different keys, and each overwrite the other. The database is then
    /// encrypted with whichever key did not survive, and nothing can open it again.
    ///
    /// The return value is what makes racing callers agree: a caller that lost stores nothing and
    /// is handed the value that won, so both go on to open the database with the same key.
    ///
    /// This default is the best a store with no create-if-absent of its own can do -- the check
    /// and the write are still two operations, so a second process can land between them, and what
    /// it prevents is the divergence rather than the race. A port whose store can do this in one
    /// step overrides it; iOS does, because the keychain's own add fails when the item exists.
    ///
    /// #### Parameters
    ///
    /// - `account`: the account to create
    ///
    /// - `value`: the value to store if there is none
    ///
    /// #### Returns
    ///
    /// the value now stored, which may be another caller's, or null if the store cannot say
    public String setIfAbsent(String account, String value) {
        if (account == null || value == null) {
            return null;
        }
        if (entryState(account) == ENTRY_ABSENT && set(account, value)) {
            // Read back rather than assuming: another process may have written between the check
            // and the write, and its value is the one to agree on.
            String stored = get(account);
            return stored != null ? stored : value;
        }
        return get(account);
    }

    /// A name unique to this application, for a store the platform shares between applications.
    ///
    /// The mobile ports do not need this: an OS sandbox already separates one application's
    /// keychain or keystore from another's. A native desktop build has no sandbox -- its storage
    /// is a plain directory under the user account -- so two applications that ask for the same
    /// account name reach the same entry. For a managed database key that means one application
    /// reading another's key, and forgetting it in either one removing the other's only copy.
    ///
    /// The package is what the installer, the store and the build all treat as the application's
    /// identity. A display name is the fallback because a build without a package still has one,
    /// though it is weaker: two vendors can both ship "Notes".
    ///
    /// #### Returns
    ///
    /// an identifier safe to embed in a storage name, never null and never empty
    protected static String applicationNamespace() {
        return applicationNamespace(null);
    }

    /// The same identifier, for a port that knows the application before `Display` can say.
    ///
    /// The simulator is that case: it builds its store while the port is still coming up, so
    /// `Display` cannot answer yet -- and it has the launcher's main class in hand, which is where
    /// its `package_name` comes from in the first place.
    ///
    /// #### Parameters
    ///
    /// - `preferred`: an identity the port already knows, or null to ask `Display`
    ///
    /// #### Returns
    ///
    /// an identifier safe to embed in a storage name, never null and never empty
    protected static String applicationNamespace(String preferred) {
        String id = preferred;
        if (id == null || id.length() == 0) {
            try {
                id = Display.getInstance().getProperty("package_name", null);
                if (id == null || id.length() == 0) {
                    id = Display.getInstance().getProperty("AppName", null);
                }
            } catch (RuntimeException tooEarly) {
                // Asked before Display is ready, which is not a reason to fail an entry lookup.
                id = null;
            }
        }
        if (id == null || id.length() == 0) {
            // A build that stamped neither. A constant rather than something that looks unique
            // and is not: sharing one namespace is the thing this exists to fix, and a name that
            // pretended otherwise would hide it.
            return "cn1app";
        }
        return sanitizeNamespace(id);
    }

    /// Reduces an identity to what a storage name, a preferences node or a keychain service holds.
    ///
    /// Reversibly, which is the whole point of it: folding every character it cannot carry onto
    /// one replacement made `com.acme.foo$bar` and `com.acme.foo_bar` the same namespace, and
    /// "My App" and "My_App" as well -- so two applications that collide there share a store this
    /// exists to keep apart, and forgetting a key in one takes the other's. The escape is the one
    /// `ManagedKeys#accountName(String)` already uses on the account half of the same name, so
    /// the two halves are encoded the same way.
    private static String sanitizeNamespace(String id) {
        StringBuilder b = new StringBuilder(id.length());
        for (int iter = 0; iter < id.length(); iter++) {
            char c = id.charAt(iter);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '-';
            if (safe) {
                b.append(c);
            } else {
                // Including the escape character itself, or "a%0020b" and "a b" would encode alike.
                b.append('%');
                b.append(HEX.charAt((c >> 12) & 0x0f));
                b.append(HEX.charAt((c >> 8) & 0x0f));
                b.append(HEX.charAt((c >> 4) & 0x0f));
                b.append(HEX.charAt(c & 0x0f));
            }
        }
        return b.toString();
    }

    /// The digits `#sanitizeNamespace(String)` escapes with.
    private static final String HEX = "0123456789abcdef";

    public static final int ENTRY_PRESENT = 1;

    /// The store answered, and there is nothing under that account.
    public static final int ENTRY_ABSENT = 0;

    /// The store could not be asked, so nothing is known about the entry.
    public static final int ENTRY_UNKNOWN = -1;

    /// Whether an entry exists, as distinct from whether it can be read.
    ///
    /// `#get(String)` cannot answer this: it returns null for an entry that is not there and for
    /// one it could not read, and a caller that treats those alike will eventually treat a store
    /// that is briefly unavailable as a store that is empty. Where that caller then writes -- a
    /// managed database key is the case this was added for -- it overwrites a key that was there
    /// all along, and the database encrypted under the old one can never be opened again.
    ///
    /// A port answers `#ENTRY_PRESENT` for an entry it can see even if it cannot decrypt it: the
    /// question is existence, not readability. The default is `#ENTRY_UNKNOWN`, which is the
    /// honest answer for a platform with no non-prompting store, and callers must treat it as
    /// "do not write".
    ///
    /// #### Parameters
    ///
    /// - `account`: the entry to ask about
    ///
    /// #### Returns
    ///
    /// one of `#ENTRY_PRESENT`, `#ENTRY_ABSENT` or `#ENTRY_UNKNOWN`
    public int entryState(String account) {
        return ENTRY_UNKNOWN;
    }
}
