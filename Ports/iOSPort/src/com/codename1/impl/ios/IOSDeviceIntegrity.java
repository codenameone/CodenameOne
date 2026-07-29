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

    private static final String KEY_PENDING_SINCE = "cn1.appattest.pendingSince";

    private static final String STATE_NEW = "new";
    private static final String STATE_ATTESTED = "attested";
    /**
     * Apple returned the attestation object, but no backend has acknowledged
     * recording the key yet. See {@link #confirmAttestation()}.
     */
    private static final String STATE_PENDING = "pending";

    /**
     * How long a key may sit unacknowledged before it is treated as registered
     * anyway.
     *
     * <p>Needed because acknowledgement is optional: a caller using
     * {@code DeviceIntegrity.requestIntegrityToken} directly, without the shield
     * engine, never calls {@link #confirmAttestation()}. Without an expiry such
     * an app would sit in the pending state forever and re-attest on every
     * request, which is exactly the rate-limit burn this class exists to avoid.
     * A minute comfortably covers a registration round trip on a slow link.</p>
     */
    private static final long REGISTRATION_GRACE_MILLIS = 60L * 1000L;

    // Ordinals from DeviceCheck's DCError enum, which declares no explicit
    // values: unknownSystemFailure, featureUnsupported, invalidInput,
    // invalidKey, serverUnavailable. Getting invalidKey wrong is quiet and
    // expensive -- an invalidated key would never enter the reset-and-reattest
    // branch, so the device would fail forever while malformed input would
    // pointlessly burn a fresh key.
    /** DCError.invalidKey -- the OS no longer recognises this key. */
    private static final int DC_ERROR_INVALID_KEY = 3;
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
    /** True while a generate-then-attest bootstrap is running. */
    private boolean bootstrapInFlight;
    /**
     * Bumped by every reset. A callback carrying an older generation belongs to a
     * bootstrap that was abandoned, and acting on it would repopulate the key a
     * reset just deleted -- racing whatever flow started afterwards to persist a
     * different key.
     */
    private int generation;
    /** Callers that arrived mid-bootstrap and will assert once it completes. */
    private final java.util.Vector waitingForBootstrap = new java.util.Vector();

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
        store.remove(KEY_PENDING_SINCE);
        synchronized (flowLock) {
            currentBackoff = MIN_BACKOFF_MILLIS;
            bootstrapInFlight = false;
            // Any callback still outstanding now belongs to an abandoned flow.
            generation++;
            failBootstrapWaiters("App Attest state was reset while a bootstrap was in flight");
        }
    }

    /**
     * Acknowledges that a backend has recorded this device's attested public key,
     * moving it from {@code pending} to {@code attested} so subsequent requests
     * take the cheap assertion path.
     *
     * <p>Until this is called -- or the grace window expires -- requests are
     * refused with a retry hint rather than asserting. An assertion references a
     * key by identifier only, so one sent before the server has the public key is
     * simply unknown to it: the server rejects it, the app reads that as an
     * invalid key, and resets a key that was in fact perfectly good. The whole
     * point of attest-once is not to burn keys that way.</p>
     */
    void confirmAttestation() {
        SecureStorage store = SecureStorage.getInstance();
        if (!STATE_PENDING.equals(store.get(KEY_STATE))) {
            return;
        }
        synchronized (flowLock) {
            store.set(KEY_STATE, STATE_ATTESTED);
            store.remove(KEY_PENDING_SINCE);
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
            String state = store.get(KEY_STATE);
            if (STATE_PENDING.equals(state) && keyId != null && keyId.length() > 0) {
                if (registrationGraceRemaining() > 0) {
                    // The key is attested with Apple but no backend has confirmed
                    // recording it. Asserting now would present a key identifier
                    // the server cannot resolve, which reads as an invalid key and
                    // costs a needless reset -- so refuse briefly instead.
                    r.error(new RuntimeException("App Attest is completing first-run "
                            + "registration for this device; retry shortly"));
                    return r;
                }
                // Nobody acknowledged within the window. Either the consumer does
                // not participate in acknowledgement at all, or its registration
                // call was lost. Assume registered rather than re-attesting on
                // every request forever.
                store.set(KEY_STATE, STATE_ATTESTED);
                store.remove(KEY_PENDING_SINCE);
                state = STATE_ATTESTED;
            }
            boolean attested = STATE_ATTESTED.equals(state);
            if (keyId == null || keyId.length() == 0 || !attested) {
                // Checked before branching on the key state, not after: between
                // key generation persisting the id and its attestation
                // completing, the key exists but is not yet attested, and a
                // caller arriving in that window would otherwise attest the same
                // key a second time. Attestation is rate limited, so that costs
                // real budget and races its own result.
                PendingRequest pending =
                        new PendingRequest(r, nonce, PendingRequest.OP_GENERATE_KEY, null);
                if (bootstrapInFlight) {
                    // Key generation is asynchronous, so holding the lock only
                    // until the native call is issued is not enough: a second
                    // caller would still see no key and generate its own,
                    // burning a second hardware key against Apple's per-device
                    // budget. Queue instead, and assert once the first bootstrap
                    // lands -- assertions are unlimited.
                    waitingForBootstrap.addElement(pending);
                    return r;
                }
                bootstrapInFlight = true;
                if (keyId != null && keyId.length() > 0) {
                    // A key exists but was never attested -- a previous attempt
                    // died between the two steps. Attest that key rather than
                    // generating another.
                    attestKey(r, nonce, keyId);
                } else {
                    int rid = register(pending);
                    nativeInstance.appAttestGenerateKey(rid);
                }
                return r;
            }
            assertWithKey(r, nonce, keyId);
        }
        return r;
    }

    /**
     * Milliseconds left in the registration grace window, or 0 once it has run
     * out. A pending timestamp from the future -- a clock the user moved back --
     * is treated as expired rather than as an unbounded wait.
     */
    private static long registrationGraceRemaining() {
        String since = SecureStorage.getInstance().get(KEY_PENDING_SINCE);
        if (since == null || since.length() == 0) {
            return 0;
        }
        long started;
        try {
            started = Long.parseLong(since);
        } catch (NumberFormatException e) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - started;
        if (elapsed < 0 || elapsed >= REGISTRATION_GRACE_MILLIS) {
            return 0;
        }
        return REGISTRATION_GRACE_MILLIS - elapsed;
    }

    // --- flow steps ------------------------------------------------------

    private void attestKey(AsyncResource<String> r, String nonce, String keyId) {
        attestKey(r, nonce, keyId, false);
    }

    private void attestKey(AsyncResource<String> r, String nonce, String keyId, boolean retried) {
        // The attestation binds to SHA-256 of the raw nonce. Hashing here rather
        // than natively keeps a single hash implementation across both paths.
        String hash = base64(Hash.sha256(bytes(nonce)));
        PendingRequest pending = new PendingRequest(r, nonce, PendingRequest.OP_ATTEST, keyId);
        pending.retried = retried;
        int rid = register(pending);
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
        if (isStale(pending)) {
            fail(pending, "App Attest state was reset while this request was in flight");
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
            // Carry the marker forward: without it a recovery attempt whose
            // replacement key also reports invalidKey would recover again, and
            // again, instead of surfacing the failure.
            instance.attestKey(pending.result, pending.nonce, keyId, pending.retried);
        }
    }

    /** Called from native with the attestation object for a newly attested key. */
    public static void nativeAttestationReady(final int requestId, final String attestationB64) {
        PendingRequest pending = take(requestId);
        if (pending == null) {
            return;
        }
        if (isStale(pending)) {
            fail(pending, "App Attest state was reset while this request was in flight");
            return;
        }
        if (attestationB64 == null) {
            fail(pending, "App Attest attestation returned no data");
            return;
        }
        // Apple accepted the attestation, but only the backend can confirm it
        // recorded the key, so the key becomes pending rather than attested.
        // Every caller -- queued or newly arriving -- is told to retry until
        // confirmAttestation() lands or the grace window expires; otherwise a
        // caller arriving right after this callback would sail past the queue and
        // assert against a key the server has never seen. If the backend later
        // rejects it, the app calls DeviceIntegrity.resetAttestation() instead.
        SecureStorage store = SecureStorage.getInstance();
        store.set(KEY_STATE, STATE_PENDING);
        store.set(KEY_PENDING_SINCE, Long.toString(System.currentTimeMillis()));
        if (instance != null) {
            synchronized (instance.flowLock) {
                instance.currentBackoff = MIN_BACKOFF_MILLIS;
                instance.bootstrapInFlight = false;
                // Deliberately NOT asserted here, for the reason above. Queued
                // callers are told to retry; by then the key is registered and
                // their request costs one cheap assertion.
                instance.failBootstrapWaiters("App Attest is completing first-run "
                        + "registration for this device; retry shortly");
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
        if (isStale(pending)) {
            // A reset landed while this assertion was in flight -- typically because a
            // concurrent request learned the backend does not recognise the key. Handing
            // back an assertion for the key that was just discarded would send the
            // server the very thing it already rejected.
            fail(pending, "App Attest state was reset while this request was in flight");
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
                // resetAttestation() clears the flag, so set it again before
                // starting the replacement: otherwise a request arriving before
                // the recovery callback sees no key and no bootstrap running,
                // generates a second rate-limited key and races this one.
                instance.bootstrapInFlight = true;
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
        String message = msg == null ? "App Attest failed (code " + errorCode + ")" : msg;
        if (instance != null && pending.op != PendingRequest.OP_ASSERT) {
            synchronized (instance.flowLock) {
                instance.bootstrapInFlight = false;
                instance.failBootstrapWaiters(message);
            }
        }
        fail(pending, message);
    }

    /** Caller holds flowLock. Leaving these unresolved would hang the callers. */
    private void failBootstrapWaiters(String message) {
        while (!waitingForBootstrap.isEmpty()) {
            PendingRequest waiting = (PendingRequest) waitingForBootstrap.elementAt(0);
            waitingForBootstrap.removeElementAt(0);
            fail(waiting, message);
        }
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
        if (instance != null) {
            pending.generation = instance.generation;
        }
        synchronized (REQUESTS) {
            int rid = nextRequestId++;
            REQUESTS.put(Integer.valueOf(rid), pending);
            return rid;
        }
    }

    /**
     * True when this callback belongs to a flow a reset has since abandoned. Its
     * results must be discarded rather than written back, or it would resurrect
     * the key the reset deleted.
     */
    private static boolean isStale(PendingRequest pending) {
        return instance != null && pending.generation != instance.generation;
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
            // Every JVM is required to support UTF-8, so this cannot happen. Falling
            // back to the platform default would be worse than failing: these bytes are
            // hashed and the server recomputes the same hash over UTF-8, so a silent
            // re-encode would produce an attestation that never verifies and no
            // indication of why.
            throw new IllegalStateException("UTF-8 is unavailable on this VM", e);
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
        /// Which reset generation this request was registered under. A callback
        /// carrying an older one belongs to an abandoned flow.
        int generation;

        PendingRequest(AsyncResource<String> result, String nonce, int op, String keyId) {
            this.result = result;
            this.nonce = nonce;
            this.op = op;
            this.keyId = keyId;
        }
    }
}
