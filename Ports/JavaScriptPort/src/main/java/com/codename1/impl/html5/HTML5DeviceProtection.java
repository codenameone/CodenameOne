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
package com.codename1.impl.html5;

import com.codename1.security.vault.Protection;
import com.codename1.security.vault.ProtectionReport;
import com.codename1.security.vault.VaultError;
import com.codename1.security.vault.VaultException;
import com.codename1.security.vault.spi.DeviceProtection;
import com.codename1.util.AsyncResource;

/// The browser's device protection: an AES-GCM key the page can use and cannot read.
///
/// #### What the browser actually offers
///
/// There is no key store in a page. What there is, is `crypto.subtle.generateKey` with
/// `extractable: false`, which returns a `CryptoKey` the page can encrypt and decrypt with and
/// that `exportKey` rejects, and IndexedDB, which stores a `CryptoKey` as a structured clone
/// without ever serialising its material into anything JavaScript can see. Put together, that is
/// a wrapping key which never exists as bytes in the page: what lands on disk in the origin's
/// storage is ciphertext, and the key beside it is a handle the browser will not expand.
///
/// That is a real improvement on a hex string in local storage, and it is smaller than it sounds.
/// Three things it is not:
///
/// - **Not hardware backing.** The browser does not say where the key lives and there is no way
///   to ask, so [Protection#HARDWARE_BACKED] is reported [ProtectionReport#UNKNOWN] here and will
///   stay that way until a browser offers an answer.
/// - **Not protection from a copied profile.** The `CryptoKey` is in the origin's IndexedDB.
///   Someone who copies the whole profile directory copies it too, and it works in the copy. The
///   database-file-alone threat is covered; the whole-profile threat is not. Saying "copying the
///   profile is protected because copying the database is not enough" would be exactly wrong.
/// - **Not protection from the page.** Script running in this origin calls `decrypt` on the same
///   handle the application calls it on. Non-extractability stops a key being carried away, not
///   a key being used while the attacker is here.
///
/// #### Why every call crosses to the main thread
///
/// The translated application runs in a Worker, and Web Crypto and IndexedDB both live on the
/// browser side of the host bridge. The natives below are the bridge; each one suspends the
/// worker's coroutine until the host promise settles, which is why they can look synchronous in
/// Java and still be asynchronous in the browser.
///
/// #### Failures are codes, not exceptions
///
/// A rejected host promise arrives in Java as an untyped `RuntimeException`, which loses exactly
/// the distinction that matters -- a key that is absent against a key that could not be read.
/// Every native here therefore returns a byte array whose first byte is a status and whose
/// remainder is the payload, and the mapping back to [VaultError] happens in Java where it can be
/// tested. See [#STATUS_OK].
public final class HTML5DeviceProtection extends DeviceProtection {

    /// The operation succeeded; the rest of the array is the payload.
    static final int STATUS_OK = 0;

    /// There is no key under that id, definitely. Safe to create one.
    static final int STATUS_KEY_MISSING = 1;

    /// The ciphertext did not authenticate.
    static final int STATUS_AUTHENTICATION_FAILED = 2;

    /// Web Crypto is missing or failed. In a browser this is usually an insecure context.
    static final int STATUS_CRYPTO_UNAVAILABLE = 3;

    /// IndexedDB could not be opened. Private browsing modes do this.
    static final int STATUS_STORAGE_UNAVAILABLE = 4;

    /// The write was refused for space, or the origin has been evicted.
    static final int STATUS_QUOTA_EXCEEDED = 5;

    /// Not an HTTPS origin (or localhost), so none of this is available by specification.
    static final int STATUS_INSECURE_CONTEXT = 6;

    /// The store could not be asked, so nothing is known about the key.
    static final int STATUS_TEMPORARILY_UNREADABLE = 7;

    /// Anything else.
    static final int STATUS_UNKNOWN = 8;

    /// The operation could have succeeded and would not have met what was required of it -- a
    /// passkey that may sync where one bound to this device was asked for.
    static final int STATUS_POLICY_NOT_MET = 10;

    /// The user dismissed a prompt, or it timed out. Not a failure: nothing went wrong, the
    /// check simply did not happen, and an application that shows an error here is showing one
    /// for a button the user chose not to press.
    static final int STATUS_CANCELLED = 9;

    private static HTML5DeviceProtection instance;

    private HTML5DeviceProtection() {
    }

    /// The port's singleton.
    public static synchronized HTML5DeviceProtection getInstance() {
        if (instance == null) {
            instance = new HTML5DeviceProtection();
        }
        return instance;
    }

    /// The passkey-backed variant, when this browser can offer one.
    ///
    /// Reported from a capability bit rather than assumed: the PRF extension needs an
    /// authenticator that implements `hmac-secret`, and plenty register a passkey without one.
    /// Null here makes [com.codename1.security.vault.UnlockPolicy#REQUIRE_USER_VERIFICATION]
    /// refuse rather than enrol under the weaker unattended key.
    @Override
    public DeviceProtection userVerifying() {
        return (capabilities() & CAP_WEBAUTHN_PRF) != 0
                ? HTML5PasskeyProtection.getInstance() : null;
    }

    public ProtectionReport protection() {
        ProtectionReport.Builder b = ProtectionReport.builder();
        int caps = capabilities();
        boolean crypto = (caps & CAP_SUBTLE) != 0 && (caps & CAP_SECURE_CONTEXT) != 0;
        boolean storage = (caps & CAP_INDEXEDDB) != 0;
        // PERSISTENT is reported from storage actually working, NOT from
        // `navigator.storage.persisted()`. That looks like the stronger check and is the wrong
        // one, so this is worth stating rather than leaving to be re-derived.
        //
        // [Protection#PERSISTENT] is defined as "survives the process" -- a page reload, a browser
        // restart -- which working IndexedDB gives. `persisted()` answers a different question:
        // whether the browser has promised never to evict the origin under storage pressure. It
        // grants that on engagement heuristics, so a freshly loaded application is told no.
        //
        // Measured, on a real profile with a real user, all three returning false:
        // Chrome 152, Safari 26.6, Firefox 155. Gating on it would make
        // `VaultCapabilities.supports(REMEMBER_DEVICE)` answer false in every browser on first
        // run and take the remembered-device feature away from everyone, to describe a risk that
        // is eviction rather than non-persistence.
        //
        // The eviction risk is real and is reported where it belongs: [#isStoragePersisted()]
        // answers it directly, [VaultError#QUOTA_EXCEEDED] is what an evicted origin produces,
        // and the class documentation says a browser makes no permanent guarantee.
        b.set(Protection.PERSISTENT, storage);
        b.set(Protection.ENCRYPTED_AT_REST, crypto && storage);
        b.set(Protection.NON_EXTRACTABLE_KEY, crypto && storage);
        // No browser has an OS key store reachable from a page. This is a definite no, not an
        // unknown.
        b.set(Protection.OS_PROTECTED, false);
        // And this is a definite unknown. The browser does not say, and inferring it from the
        // platform name or from a passkey prompt is how a claim gets made up.
        b.set(Protection.HARDWARE_BACKED, ProtectionReport.UNKNOWN);
        b.set(Protection.USER_VERIFICATION, false);
        b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        return b.build();
    }

    public int keyState(String keyId) {
        byte[] answer = nativeKeyState(keyId);
        if (status(answer) != STATUS_OK) {
            return KEY_UNKNOWN;
        }
        return answer.length > 1 && answer[1] != 0 ? KEY_PRESENT : KEY_ABSENT;
    }

    public AsyncResource<Boolean> ensureKey(String keyId) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        byte[] answer = nativeEnsureKey(keyId);
        VaultException failure = failureOf(answer, "the browser could not establish a device key");
        if (failure != null) {
            out.error(failure);
        } else {
            out.complete(Boolean.TRUE);
        }
        return out;
    }

    public AsyncResource<byte[]> wrap(String keyId, byte[] plaintext, byte[] aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] answer = nativeWrap(keyId, plaintext, aad);
        VaultException failure = failureOf(answer, "the browser could not wrap under the device key");
        if (failure != null) {
            out.error(failure);
        } else {
            out.complete(payload(answer));
        }
        return out;
    }

    public AsyncResource<byte[]> unwrap(String keyId, byte[] wrapped, byte[] aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] answer = nativeUnwrap(keyId, wrapped, aad);
        VaultException failure = failureOf(answer, "the browser could not unwrap the device key");
        if (failure != null) {
            out.error(failure);
        } else {
            out.complete(payload(answer));
        }
        return out;
    }

    public AsyncResource<Boolean> deleteKey(String keyId) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        byte[] answer = nativeDeleteKey(keyId);
        out.complete(Boolean.valueOf(status(answer) == STATUS_OK));
        return out;
    }

    /// Whether the browser has granted this origin persistent storage.
    ///
    /// Without it the origin is evictable: the browser may clear it under storage pressure, and
    /// everything the vault holds goes with it. An application that has enrolled a vault should
    /// ask for persistence and tell the user if it was refused, because the failure mode is
    /// "your notes are gone" and it arrives with no warning.
    public boolean isStoragePersisted() {
        return (capabilities() & CAP_PERSISTED) != 0;
    }

    /// Whether this page is a secure context. Web Crypto is unavailable outside one by
    /// specification, so a vault in an insecure context can encrypt nothing.
    public boolean isSecureContext() {
        return (capabilities() & CAP_SECURE_CONTEXT) != 0;
    }

    /// `window.isSecureContext`.
    static final int CAP_SECURE_CONTEXT = 1;

    /// `crypto.subtle` is present.
    static final int CAP_SUBTLE = 2;

    /// IndexedDB opened successfully.
    static final int CAP_INDEXEDDB = 4;

    /// `navigator.storage.persisted()` answered true.
    static final int CAP_PERSISTED = 8;

    /// A `PublicKeyCredential` API that reports the PRF extension is available.
    static final int CAP_WEBAUTHN_PRF = 16;

    /// The capability bits, re-asked every call rather than cached: a browser can lose IndexedDB
    /// mid-session -- the user clears site data, the origin is evicted, a private window applies
    /// a quota -- and a cached "yes" would have the vault reporting protection it no longer has.
    int capabilities() {
        byte[] answer = nativeCapabilities();
        if (status(answer) != STATUS_OK || answer.length < 2) {
            return 0;
        }
        return answer[1] & 0xff;
    }

    private static int status(byte[] answer) {
        return answer == null || answer.length == 0 ? STATUS_UNKNOWN : (answer[0] & 0xff);
    }

    private static byte[] payload(byte[] answer) {
        byte[] out = new byte[answer.length - 1];
        System.arraycopy(answer, 1, out, 0, out.length);
        return out;
    }

    private static VaultException failureOf(byte[] answer, String message) {
        int code = status(answer);
        if (code == STATUS_OK) {
            return null;
        }
        return new VaultException(errorFor(code), message);
    }

    static VaultError errorFor(int code) {
        switch (code) {
            case STATUS_KEY_MISSING:
                return VaultError.KEY_MISSING;
            case STATUS_AUTHENTICATION_FAILED:
                return VaultError.AUTHENTICATION_FAILED;
            case STATUS_CRYPTO_UNAVAILABLE:
                return VaultError.CRYPTO_UNAVAILABLE;
            case STATUS_STORAGE_UNAVAILABLE:
                return VaultError.STORAGE_UNAVAILABLE;
            case STATUS_QUOTA_EXCEEDED:
                return VaultError.QUOTA_EXCEEDED;
            case STATUS_INSECURE_CONTEXT:
                return VaultError.INSECURE_CONTEXT;
            case STATUS_TEMPORARILY_UNREADABLE:
                return VaultError.TEMPORARILY_UNREADABLE;
            case STATUS_CANCELLED:
                return VaultError.CANCELLED;
            case STATUS_POLICY_NOT_MET:
                return VaultError.POLICY_NOT_MET;
            default:
                return VaultError.UNKNOWN;
        }
    }

    // -----------------------------------------------------------------
    // Natives. Bound in Ports/JavaScriptPort/src/main/webapp/port.js, which forwards to the
    // __cn1_vault__ host bridge in vm/ByteCodeTranslator/src/javascript/browser_bridge.js.
    //
    // Every one returns a status byte followed by its payload -- see STATUS_OK. A native whose
    // binding is missing throws "Missing javascript native method", which is loud and is the
    // behaviour we want: the alternative is a vault that silently reports no capabilities and
    // stores plaintext.
    // -----------------------------------------------------------------

    /// One byte of capability bits. See CAP_SECURE_CONTEXT and friends.
    static native byte[] nativeCapabilities();

    /// One byte: non-zero when a key exists under this id.
    static native byte[] nativeKeyState(String keyId);

    /// Creates the key if absent. Converges when two tabs race -- see the note in port.js.
    static native byte[] nativeEnsureKey(String keyId);

    /// AES-GCM encrypt under the stored key. Payload is a 12 byte nonce followed by the
    /// ciphertext and its tag.
    static native byte[] nativeWrap(String keyId, byte[] plaintext, byte[] aad);

    /// AES-GCM decrypt of what nativeWrap produced.
    static native byte[] nativeUnwrap(String keyId, byte[] wrapped, byte[] aad);

    /// Deletes the key.
    static native byte[] nativeDeleteKey(String keyId);
}
