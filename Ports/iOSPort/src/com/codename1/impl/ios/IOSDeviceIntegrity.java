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
package com.codename1.impl.ios;

import com.codename1.io.Log;
import com.codename1.security.Hash;
import com.codename1.security.SecureStorage;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;

import java.util.HashMap;
import java.util.Map;

/**
 * iOS backing for App Attest (DeviceCheck.framework), surfaced through
 * {@link com.codename1.security.DeviceIntegrity#requestIntegrityToken(String)}.
 *
 * <h2>Attest once, assert many</h2>
 *
 * <p>Apple's model is: generate a Secure Enclave key <em>once</em>, attest it
 * <em>once</em> so the server can record its public key, then produce cheap
 * assertions against that key for every subsequent request. Key generation and
 * attestation are both rate limited; assertions are not.</p>
 *
 * <p>This class implements that state machine over three keychain entries in
 * {@link IOSSecureStorage}'s non-prompting tier, so the identity survives app
 * restarts and updates:</p>
 *
 * <ul>
 *   <li>{@code cn1.appattest.keyId} -- the key identifier
 *   <li>{@code cn1.appattest.state} -- {@code new} or {@code attested}
 *   <li>{@code cn1.appattest.retryAfter} -- backoff deadline after a throttle
 * </ul>
 *
 * <h2>Token format</h2>
 *
 * <p>Tokens are prefixed so a backend can tell the two forms apart, which the
 * bare {@code base64:base64} form could not:</p>
 *
 * <pre>
 * cn1aa1:attest:&lt;b64 keyId&gt;:&lt;b64 attestationObject&gt;
 * cn1aa1:assert:&lt;b64 keyId&gt;:&lt;b64 assertion&gt;:&lt;b64 clientData&gt;
 * </pre>
 *
 * <p>The client data for an assertion is a small JSON object carrying the
 * server nonce, so the server can recompute the hash the assertion signed.</p>
 *
 * <h2>ParparVM note</h2>
 *
 * <p>The native side dispatches results back through the static callbacks
 * below. As with {@link IOSBiometrics}, the static initializer invokes each with
 * no-op values so the dead-code eliminator does not strip them -- no Java caller
 * exists, and without a reachable reference they become empty stubs and the
 * native call silently does nothing.</p>
 */
final class IOSDeviceIntegrity {

    static {
        // Prevents the iOS VM optimizer from eliding these native callbacks.
        nativeKeyGenerated(-1, null);
        nativeAttestationReady(-1, null);
        nativeAssertionReady(-1, null);
        nativeAttestError(-1, -1, null);
    }

    static final String TOKEN_PREFIX = "cn1aa1";

    private static final String KEY_ID = "cn1.appattest.keyId";
    private static final String KEY_STATE = "cn1.appattest.state";
    private static final String KEY_RETRY_AFTER = "cn1.appattest.retryAfter";

    private static final String STATE_NEW = "new";
    private static final String STATE_ATTESTED = "attested";

    /** DCError.invalidKey -- the server or the OS no longer knows this key. */
    private static final int DC_ERROR_INVALID_KEY = 2;
    /** DCError.serverUnavailable -- Apple is throttling or unreachable. */
    private static final int DC_ERROR_SERVER_UNAVAILABLE = 4;

    private static final long MIN_BACKOFF_MILLIS = 30L * 1000L;
    private static final long MAX_BACKOFF_MILLIS = 60L * 60L * 1000L;

    private static final Map<Integer, PendingRequest> REQUESTS =
            new HashMap<Integer, PendingRequest>();
    private static int nextRequestId = 1;

    private static IOSDeviceIntegrity instance;

    private final IOSNative nativeInstance;
    /** Guards the whole attest flow so concurrent callers cannot each burn a key. */
    private final Object flowLock = new Object();
    private long currentBackoff = MIN_BACKOFF_MILLIS;

    IOSDeviceIntegrity(IOSNative nativeInstance) {
        this.nativeInstance = nativeInstance;
        instance = this;
    }

    boolean isSupported() {
        return nativeInstance.isAppAttestSupported();
    }

    /**
     * Discards the stored key so the next request attests afresh. Called when the
     * backend reports it does not recognise the key -- after a reinstall, a
     * restore to a new device, or an OS-side invalidation.
     */
    void resetAttestation() {
        SecureStorage store = SecureStorage.getInstance();
        store.remove(KEY_ID);
        store.remove(KEY_STATE);
        store.remove(KEY_RETRY_AFTER);
        synchronized (flowLock) {
            currentBackoff = MIN_BACKOFF_MILLIS;
        }
    }

    String[] jailbreakSignals() {
        try {
            String signals = nativeInstance.iosJailbreakSignals();
            if (signals == null || signals.length() == 0) {
                return new String[0];
            }
            return com.codename1.io.Util.split(signals, ",");
        } catch (Throwable t) {
            return new String[0];
        }
    }

    AsyncResource<String> requestToken(String nonce) {
        AsyncResource<String> r = new AsyncResource<String>();
        if (!nativeInstance.isAppAttestSupported()) {
            r.error(new UnsupportedOperationException(
                    "App Attest is not supported on this device"));
            return r;
        }
        if (nonce == null || nonce.length() == 0) {
            r.error(new IllegalArgumentException(
                    "App Attest requires a server-issued nonce; a client-generated "
                    + "one is not replay-safe"));
            return r;
        }
        synchronized (flowLock) {
            long retryAfter = readRetryAfter();
            if (retryAfter > System.currentTimeMillis()) {
                r.error(new RuntimeException("App Attest is backing off after a throttle; "
                        + "retry in " + ((retryAfter - System.currentTimeMillis()) / 1000)
                        + "s"));
                return r;
            }
            SecureStorage store = SecureStorage.getInstance();
            String keyId = store.get(KEY_ID);
            if (keyId == null || keyId.length() == 0) {
                // No key yet: generate one, then continue into attestation.
                int rid = register(new PendingRequest(r, nonce, PendingRequest.OP_GENERATE_KEY, null));
                nativeInstance.appAttestGenerateKey(rid);
                return r;
            }
            if (STATE_ATTESTED.equals(store.get(KEY_STATE))) {
                assertWithKey(r, nonce, keyId);
            } else {
                attestKey(r, nonce, keyId);
            }
        }
        return r;
    }

    // --- flow steps ------------------------------------------------------

    private void attestKey(AsyncResource<String> r, String nonce, String keyId) {
        // The attestation binds to SHA-256 of the raw nonce. Hashing here rather
        // than natively keeps a single hash implementation across both paths.
        String hash = base64(Hash.sha256(bytes(nonce)));
        int rid = register(new PendingRequest(r, nonce, PendingRequest.OP_ATTEST, keyId));
        nativeInstance.appAttestAttestKey(rid, keyId, hash);
    }

    private void assertWithKey(AsyncResource<String> r, String nonce, String keyId) {
        String clientData = clientDataJson(nonce, keyId);
        String hash = base64(Hash.sha256(bytes(clientData)));
        PendingRequest pending =
                new PendingRequest(r, nonce, PendingRequest.OP_ASSERT, keyId);
        pending.clientData = clientData;
        int rid = register(pending);
        nativeInstance.appAttestGenerateAssertion(rid, keyId, hash);
    }

    /**
     * Minimal JSON, hand-built rather than pulled from a parser: it has to be
     * byte-identical to what the server recomputes the hash over, so its exact
     * serialization is part of the wire contract.
     */
    private static String clientDataJson(String nonce, String keyId) {
        return "{\"n\":\"" + escapeJson(nonce) + "\",\"k\":\"" + escapeJson(keyId) + "\"}";
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- Callbacks invoked from native code (do not rename) ----------------

    /** Called from native once a fresh hardware key exists. */
    public static void nativeKeyGenerated(final int requestId, final String keyId) {
        PendingRequest pending = take(requestId);
        if (pending == null || instance == null) {
            return;
        }
        if (keyId == null || keyId.length() == 0) {
            fail(pending, "App Attest key generation returned no identifier");
            return;
        }
        SecureStorage store = SecureStorage.getInstance();
        store.set(KEY_ID, keyId);
        store.set(KEY_STATE, STATE_NEW);
        synchronized (instance.flowLock) {
            instance.attestKey(pending.result, pending.nonce, keyId);
        }
    }

    /** Called from native with the attestation object for a newly attested key. */
    public static void nativeAttestationReady(final int requestId, final String attestationB64) {
        PendingRequest pending = take(requestId);
        if (pending == null) {
            return;
        }
        if (attestationB64 == null) {
            fail(pending, "App Attest attestation returned no data");
            return;
        }
        // Optimistic: Apple accepted the attestation, but only the backend can
        // confirm it recorded the key. If it later rejects, the app calls
        // DeviceIntegrity.resetAttestation() and we start over.
        SecureStorage store = SecureStorage.getInstance();
        store.set(KEY_STATE, STATE_ATTESTED);
        if (instance != null) {
            synchronized (instance.flowLock) {
                instance.currentBackoff = MIN_BACKOFF_MILLIS;
            }
        }
        succeed(pending, TOKEN_PREFIX + ":attest:" + base64(bytes(pending.keyId))
                + ":" + attestationB64);
    }

    /** Called from native with an assertion over an already attested key. */
    public static void nativeAssertionReady(final int requestId, final String assertionB64) {
        PendingRequest pending = take(requestId);
        if (pending == null) {
            return;
        }
        if (assertionB64 == null) {
            fail(pending, "App Attest assertion returned no data");
            return;
        }
        succeed(pending, TOKEN_PREFIX + ":assert:" + base64(bytes(pending.keyId))
                + ":" + assertionB64
                + ":" + base64(bytes(pending.clientData)));
    }

    /** Called from native when any step fails. errorCode is the raw DCError code. */
    public static void nativeAttestError(final int requestId, final int errorCode,
            final String msg) {
        PendingRequest pending = take(requestId);
        if (pending == null) {
            return;
        }
        if (errorCode == DC_ERROR_INVALID_KEY && instance != null && !pending.retried) {
            // The key is gone or was never valid. Wipe it and try once from
            // scratch; a second failure is reported rather than looped.
            instance.resetAttestation();
            synchronized (instance.flowLock) {
                PendingRequest retry = new PendingRequest(pending.result, pending.nonce,
                        PendingRequest.OP_GENERATE_KEY, null);
                retry.retried = true;
                int rid = register(retry);
                instance.nativeInstance.appAttestGenerateKey(rid);
            }
            return;
        }
        if (errorCode == DC_ERROR_SERVER_UNAVAILABLE && instance != null) {
            // Never retry a throttle in a loop -- that is what gets an app's
            // whole attestation budget suspended.
            synchronized (instance.flowLock) {
                long backoff = instance.currentBackoff;
                SecureStorage.getInstance().set(KEY_RETRY_AFTER,
                        Long.toString(System.currentTimeMillis() + backoff));
                instance.currentBackoff = Math.min(backoff * 2, MAX_BACKOFF_MILLIS);
            }
        }
        fail(pending, msg == null ? "App Attest failed (code " + errorCode + ")" : msg);
    }

    // --- helpers ---------------------------------------------------------

    private static long readRetryAfter() {
        try {
            String v = SecureStorage.getInstance().get(KEY_RETRY_AFTER);
            return v == null ? 0 : Long.parseLong(v);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int register(PendingRequest pending) {
        synchronized (REQUESTS) {
            int rid = nextRequestId++;
            REQUESTS.put(Integer.valueOf(rid), pending);
            return rid;
        }
    }

    private static PendingRequest take(int requestId) {
        synchronized (REQUESTS) {
            return REQUESTS.remove(Integer.valueOf(requestId));
        }
    }

    private static void succeed(final PendingRequest pending, final String token) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (!pending.result.isDone()) {
                    pending.result.complete(token);
                }
            }
        });
    }

    private static void fail(final PendingRequest pending, final String msg) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (!pending.result.isDone()) {
                    pending.result.error(new RuntimeException(msg));
                }
            }
        });
    }

    private static byte[] bytes(String s) {
        if (s == null) {
            return new byte[0];
        }
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e(e);
            return s.getBytes();
        }
    }

    private static String base64(byte[] data) {
        return Base64.encodeNoNewline(data);
    }

    /** One in-flight native operation and the state needed to finish or resume it. */
    private static final class PendingRequest {
        static final int OP_GENERATE_KEY = 0;
        static final int OP_ATTEST = 1;
        static final int OP_ASSERT = 2;

        final AsyncResource<String> result;
        final String nonce;
        final int op;
        final String keyId;
        String clientData;
        boolean retried;

        PendingRequest(AsyncResource<String> result, String nonce, int op, String keyId) {
            this.result = result;
            this.nonce = nonce;
            this.op = op;
            this.keyId = keyId;
        }
    }
}
