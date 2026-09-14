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
import com.codename1.security.vault.SecureEnvelope;
import com.codename1.security.vault.VaultError;
import com.codename1.security.vault.VaultException;
import com.codename1.security.vault.spi.DeviceProtection;
import com.codename1.util.AsyncResource;

/// The browser's user-verifying device protection: a passkey, through the WebAuthn PRF extension.
///
/// #### Why a passkey can hold a key at all
///
/// A passkey signature is not an encryption key, and treating one as such is the mistake this
/// class exists to not make: signatures are randomised, so signing a fixed challenge gives
/// different bytes every time. The PRF extension is the part of WebAuthn that genuinely derives
/// one -- the authenticator evaluates its own HMAC secret over a salt the page supplies, so the
/// same credential and salt give the same 32 bytes forever, and nothing else can produce them.
///
/// #### What it buys over [HTML5DeviceProtection]
///
/// One thing, and it is the thing that policy asks for: **the key material does not exist until
/// the user verifies**. A copied browser profile carries the credential id, which is not a secret,
/// and cannot derive anything without the authenticator and a user in front of it. That is the
/// gap the non-extractable `CryptoKey` cannot close -- it sits in IndexedDB and works in the copy.
///
/// What it does not buy is anything after unlocking. Once the vault is open the derived key is in
/// the page's memory exactly as the other path's is, and script in the origin reaches it the same
/// way.
///
/// #### Availability
///
/// Runtime-detected, never assumed. The PRF extension needs an authenticator that implements
/// `hmac-secret`, and a browser that registers a passkey without it is common -- so enrolment
/// checks `prf.enabled` from the creation ceremony and refuses to keep a credential that cannot
/// derive. A prompt with nothing behind it is worse than no option.
///
/// Note also that **WebAuthn does not work on an IP-address origin at all**: a relying-party id
/// must be a domain. Serve from a hostname; for local development that means `localhost` rather
/// than `127.0.0.1`.
public final class HTML5PasskeyProtection extends DeviceProtection {

    private static HTML5PasskeyProtection instance;

    private HTML5PasskeyProtection() {
    }

    /// The port's singleton.
    public static synchronized HTML5PasskeyProtection getInstance() {
        if (instance == null) {
            instance = new HTML5PasskeyProtection();
        }
        return instance;
    }

    @Override
    public boolean requiresUserVerification() {
        return true;
    }

    @Override
    public ProtectionReport protection() {
        ProtectionReport.Builder b = ProtectionReport.builder();
        int caps = HTML5DeviceProtection.getInstance().capabilities();
        boolean usable = (caps & HTML5DeviceProtection.CAP_SECURE_CONTEXT) != 0
                && (caps & HTML5DeviceProtection.CAP_SUBTLE) != 0
                && (caps & HTML5DeviceProtection.CAP_INDEXEDDB) != 0
                && (caps & HTML5DeviceProtection.CAP_WEBAUTHN_PRF) != 0;
        b.set(Protection.PERSISTENT, usable);
        b.set(Protection.ENCRYPTED_AT_REST, usable);
        // The authenticator's HMAC secret never leaves it -- there is no API that exports one, on
        // purpose. This is the one protection a browser can report as genuinely yes.
        b.set(Protection.NON_EXTRACTABLE_KEY, usable);
        // A platform authenticator is held by the operating system and a roaming one is not, and
        // the page is not told which it got. Unknown is the answer, not a guess from the fact that
        // a prompt appeared.
        b.set(Protection.OS_PROTECTED, ProtectionReport.UNKNOWN);
        b.set(Protection.HARDWARE_BACKED, ProtectionReport.UNKNOWN);
        b.set(Protection.USER_VERIFICATION, usable);
        b.set(Protection.ISOLATED_FROM_APPLICATION_CODE, false);
        return b.build();
    }

    @Override
    public int keyState(String keyId) {
        byte[] answer = nativePrfState(keyId);
        if (status(answer) != HTML5DeviceProtection.STATUS_OK) {
            return KEY_UNKNOWN;
        }
        return answer.length > 1 && answer[1] != 0 ? KEY_PRESENT : KEY_ABSENT;
    }

    @Override
    public AsyncResource<Boolean> ensureKey(String keyId) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        byte[] answer = nativePrfEnroll(keyId, "Codename One vault");
        VaultException failure = failureOf(answer, "a passkey could not be enrolled for this vault");
        if (failure != null) {
            out.error(failure);
        } else {
            out.complete(Boolean.TRUE);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> wrap(String keyId, byte[] plaintext, byte[] aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] derived = null;
        try {
            derived = derive(keyId);
            out.complete(SecureEnvelope.seal(derived, keyId, 1, aad, plaintext));
        } catch (VaultException failed) {
            out.error(failed);
        } finally {
            zero(derived);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> unwrap(String keyId, byte[] wrapped, byte[] aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] derived = null;
        try {
            derived = derive(keyId);
            out.complete(SecureEnvelope.parse(wrapped).open(derived, aad));
        } catch (VaultException failed) {
            out.error(failed);
        } finally {
            zero(derived);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> deleteKey(String keyId) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        out.complete(Boolean.valueOf(
                status(nativePrfForget(keyId)) == HTML5DeviceProtection.STATUS_OK));
        return out;
    }

    /// One PRF evaluation, which is where the user is prompted.
    ///
    /// The bytes are the caller's to clear, and every caller here does so in a `finally`. That is
    /// best effort in a browser -- the heap is not ours to scrub -- and it still removes the copy
    /// this code controls.
    private byte[] derive(String keyId) {
        byte[] answer = nativePrfDerive(keyId);
        VaultException failure = failureOf(answer,
                "the passkey could not derive this vault's key");
        if (failure != null) {
            throw failure;
        }
        byte[] derived = new byte[answer.length - 1];
        System.arraycopy(answer, 1, derived, 0, derived.length);
        if (derived.length != 32) {
            zero(derived);
            throw new VaultException(VaultError.CRYPTO_UNAVAILABLE,
                    "the authenticator returned an unexpected amount of key material");
        }
        return derived;
    }

    private static void zero(byte[] data) {
        if (data != null) {
            for (int iter = 0; iter < data.length; iter++) {
                data[iter] = 0;
            }
        }
    }

    private static int status(byte[] answer) {
        return answer == null || answer.length == 0
                ? HTML5DeviceProtection.STATUS_UNKNOWN : (answer[0] & 0xff);
    }

    private static VaultException failureOf(byte[] answer, String message) {
        int code = status(answer);
        if (code == HTML5DeviceProtection.STATUS_OK) {
            return null;
        }
        return new VaultException(HTML5DeviceProtection.errorFor(code), message);
    }

    // -----------------------------------------------------------------
    // Natives. Bound in Ports/JavaScriptPort/src/main/webapp/port.js, forwarding to the
    // __cn1_vault__ host bridge. Status byte first, payload after -- see HTML5DeviceProtection.
    // -----------------------------------------------------------------

    /// One byte: non-zero when a passkey is enrolled for this vault. Does not prompt.
    static native byte[] nativePrfState(String keyId);

    /// Creates a passkey and confirms the authenticator will evaluate a PRF. Prompts.
    static native byte[] nativePrfEnroll(String keyId, String userName);

    /// Evaluates the PRF for this vault's stored salt. Prompts. Payload is 32 bytes.
    static native byte[] nativePrfDerive(String keyId);

    /// Forgets the credential id and salt. The passkey itself stays on the authenticator, where
    /// only the user can remove it -- this makes it unusable for this vault, not non-existent.
    static native byte[] nativePrfForget(String keyId);
}
