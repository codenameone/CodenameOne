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

    private static final String KEY_PENDING_SINCE = "cn1.appattest.pendingSince";
    /**
     * Set immediately before {@code attestKey}, cleared once the result is recorded.
     *
     * <p>Attestation is one-time and rate limited, so a key that has already been
     * submitted must not be submitted again. Finding this marker on a later launch means
     * the app died between Apple accepting the attestation and us persisting that fact
     * -- and the key may well be spent, so the only safe move is a fresh one.</p>
     */
    private static final String KEY_ATTEST_STARTED = "cn1.appattest.attestStarted";
    /**
     * Marks the one-shot invalid-key recovery as already spent.
     *
     * <p>Persisted rather than carried on the in-flight request: {@code pending.retried}
     * dies with that request, so the next caller was reconstructed as a first attempt
     * and allowed to reset and burn another rate-limited key -- repeatedly, once the OS
     * has decided it dislikes this device's keys.</p>
     */
    private static final String KEY_RECOVERY_SPENT = "cn1.appattest.recoverySpent";

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
    /**
     * The throttle deadline, held in memory as well as in the keychain.
     *
     * <p>{@code requestToken} reads the deadline from storage, so a refused write left
     * the backoff existing only as a duration nobody consulted -- and the next caller
     * went straight back to a throttled App Attest service, which is how a suspension
     * gets extended rather than waited out. This survives at least until the process
     * dies, which is the case the keychain was covering.</p>
     */
    private long retryAfterFallback;
    /// Mirrors [#KEY_RECOVERY_SPENT] in memory, so a refused keychain write still bounds the
    /// one-shot recovery for the life of the process rather than letting it repeat per request.
    private boolean recoverySpentInMemory;

    /**
     * The key whose attestation Apple has already answered, when the keychain refused to
     * drop its start marker.
     *
     * <p>The marker means "submitted, outcome unknown", and clearing it is what stops the
     * next request discarding a perfectly good key. A refused removal therefore puts the
     * device straight back into burning one rate-limited hardware key per request -- the
     * failure the clearing was added to prevent, reached through the storage layer
     * instead. Held in memory so at least the life of this process is covered; a restart
     * still reads the marker and discards the key once, which is the pre-existing
     * behaviour and bounded.</p>
     */
    private String attestAnsweredForKey;
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
        // The lock is taken BEFORE the removals, so deleting the state and invalidating
        // the generation are one step. Removing first left a window in which a callback
        // could take the lock, pass its staleness check -- the generation had not been
        // bumped yet -- and write the discarded key straight back. The reset would then
        // mark only that callback stale and leave the resurrected key behind for the
        // next bootstrap to find.
        synchronized (flowLock) {
            try {
                resetLocked();
            } catch (IllegalStateException e) {
                // Public API entry point, so the failure is logged rather than thrown at
                // an app that asked us to forget a key. The state is untouched, so the
                // next attempt can retry it.
                Log.e(e);
            }
        }
    }

    /// The reset itself, for callers that must not release the lock between discarding
    /// the old identity and starting the replacement. Caller holds `flowLock`.
    private void resetLocked() {
        SecureStorage store = SecureStorage.getInstance();
        // The two that carry the identity are checked. If the keychain refuses to
        // delete them the reset has not happened, and advancing the generation anyway
        // would report success while the next request reloads the very key the backend
        // asked us to discard -- and keeps asserting with it. Better to leave the state
        // visibly unchanged so the caller fails and can retry.
        boolean idGone = store.remove(KEY_ID);
        boolean stateGone = store.remove(KEY_STATE);
        store.remove(KEY_RETRY_AFTER);
        store.remove(KEY_PENDING_SINCE);
        store.remove(KEY_ATTEST_STARTED);
        store.remove(KEY_RECOVERY_SPENT);
        recoverySpentInMemory = false;
        attestAnsweredForKey = null;
        if (!idGone || !stateGone) {
            // The two deletions are not atomic, so a partial failure leaves half the
            // identity gone. Treating that as "untouched" would let a callback from the
            // rejected identity still pass its staleness check and act on inconsistent
            // state, so the generation is advanced first: whatever the keychain did, no
            // outstanding callback belongs to the current flow any more.
            generation++;
            bootstrapInFlight = false;
            failBootstrapWaiters("App Attest could not fully discard its stored key");
            throw new IllegalStateException("App Attest could not discard its stored key; "
                    + "the keychain refused the deletion");
        }
        currentBackoff = MIN_BACKOFF_MILLIS;
        retryAfterFallback = 0L;
        bootstrapInFlight = false;
        // Any callback still outstanding now belongs to an abandoned flow.
        generation++;
        failBootstrapWaiters("App Attest state was reset while a bootstrap was in flight");
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
    void confirmAttestation(String keyId) {
        if (keyId == null || keyId.length() == 0) {
            return;
        }
        synchronized (flowLock) {
            // Re-read inside the lock. Checking first and transitioning afterwards let a
            // reset and a replacement key land in between, so this would stamp
            // STATE_ATTESTED over a STATE_NEW key Apple has not attested yet -- and the
            // next request would assert against it.
            SecureStorage store = SecureStorage.getInstance();
            if (!STATE_PENDING.equals(store.get(KEY_STATE))) {
                return;
            }
            // And it must acknowledge THIS key. A response for an earlier attestation
            // can arrive after a reset has already replaced the identity, and promoting
            // on that would mark a key attested that the backend has never seen -- so
            // the next assertion is rejected and costs another reset, which is the loop
            // this state exists to break.
            if (!keyId.equals(store.get(KEY_ID))) {
                return;
            }
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
            long retryAfter = Math.max(readRetryAfter(), retryAfterFallback);
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
                if (keyId != null && keyId.length() > 0
                        && (store.get(KEY_ATTEST_STARTED) == null
                            || keyId.equals(attestAnsweredForKey))) {
                    // A key exists but attestation was never started for it -- the
                    // previous attempt died between generating and submitting. Attest
                    // that key rather than burning another.
                    attestKey(r, nonce, keyId);
                } else if (keyId != null && keyId.length() > 0
                        && (recoverySpentInMemory
                            || store.get(KEY_RECOVERY_SPENT) != null)) {
                    // The interrupted key belongs to a recovery that has already been
                    // used once. Discarding it here would generate yet another
                    // rate-limited key, and would do so on every subsequent request --
                    // the exact loop the spent marker exists to stop. Report instead.
                    bootstrapInFlight = false;
                    failResource(r, "App Attest could not establish a usable key on this "
                            + "device; call DeviceIntegrity.resetAttestation() to try again");
                    return r;
                } else if (keyId != null && keyId.length() > 0) {
                    // Attestation WAS started for this key and we never recorded the
                    // outcome, so Apple may already have consumed it. Re-submitting a
                    // spent key is rejected and the failure repeats on every request
                    // until something resets, so discard it and start clean. One
                    // rate-limited key is spent either way; this way the device recovers.
                    store.remove(KEY_ID);
                    store.remove(KEY_STATE);
                    store.remove(KEY_ATTEST_STARTED);
                    // The discarded key is the one the in-memory marker named.
                    attestAnsweredForKey = null;
                    PendingRequest fresh = new PendingRequest(r, nonce,
                            PendingRequest.OP_GENERATE_KEY, null);
                    int rid = register(fresh);
                    nativeInstance.appAttestGenerateKey(rid);
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
        // Recorded BEFORE the call, so a crash between here and the result is
        // distinguishable from never having tried -- and the write is checked, because
        // proceeding without the marker recreates exactly the case it exists to catch:
        // the app dies after Apple consumes the one-time attestation, the next launch
        // sees a key with no marker, and submits the spent key again.
        if (!SecureStorage.getInstance().set(KEY_ATTEST_STARTED, "1")) {
            bootstrapInFlight = false;
            failBootstrapWaiters("App Attest could not record that attestation had started");
            failResource(r, "App Attest could not record that attestation had started, so it "
                    + "was not attempted rather than risk spending the key untracked");
            return;
        }
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

    /**
     * Fails a request and the bootstrap it was driving.
     *
     * <p>For the branches that give up before touching storage. Failing only the
     * initiating request there left {@code bootstrapInFlight} set, so every caller
     * already queued behind it -- and every caller that arrived afterwards and was queued
     * because a bootstrap looked live -- waited on a bootstrap that had already stopped,
     * with nothing left to release them. The mid-flow storage failures got this right;
     * these two did not.</p>
     *
     * <p>A stale request only fails itself: a reset has already cleared the flag and
     * failed the waiters, and clearing it again would clear the flag belonging to the
     * replacement bootstrap that reset started.</p>
     */
    private static void failBootstrapAttempt(PendingRequest pending, String msg) {
        if (instance == null) {
            fail(pending, msg);
            return;
        }
        synchronized (instance.flowLock) {
            if (isStale(pending)) {
                fail(pending, "App Attest state was reset while this request was in flight");
                return;
            }
            instance.bootstrapInFlight = false;
            fail(pending, msg);
            instance.failBootstrapWaiters(msg);
        }
    }

    /** Called from native once a fresh hardware key exists. */
    public static void nativeKeyGenerated(final int requestId, final String keyId) {
        PendingRequest pending = take(requestId);
        if (pending == null || instance == null) {
            return;
        }
        if (keyId == null || keyId.length() == 0) {
            failBootstrapAttempt(pending,
                    "App Attest key generation returned no identifier");
            return;
        }
        // The staleness check and the writes it guards happen under the same lock a
        // reset takes. Split apart, a reset landing between them would delete the key
        // and bump the generation, and these writes would then put the deleted key
        // back -- registered under the new generation, so every later staleness check
        // would accept the bootstrap that was supposed to have been abandoned.
        // fail() only schedules onto the EDT, so it is safe to call while holding this.
        synchronized (instance.flowLock) {
            if (isStale(pending)) {
                fail(pending, "App Attest state was reset while this request was in flight");
                return;
            }
            SecureStorage store = SecureStorage.getInstance();
            if (!store.set(KEY_ID, keyId) || !store.set(KEY_STATE, STATE_NEW)) {
                // The keychain refused the write. Attesting anyway would burn a
                // rate-limited attestation on a key the next request cannot find, so it
                // would generate another, and another. Give up on this attempt instead
                // and leave nothing half-written behind.
                store.remove(KEY_ID);
                store.remove(KEY_STATE);
                store.remove(KEY_ATTEST_STARTED);
                instance.attestAnsweredForKey = null;
                instance.bootstrapInFlight = false;
                fail(pending, "App Attest could not store its key identifier");
                instance.failBootstrapWaiters(
                        "App Attest could not store its key identifier");
                return;
            }
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
        if (attestationB64 == null) {
            // Same cleanup as key generation returning nothing: this bootstrap is over,
            // and the queue behind it has to be told.
            failBootstrapAttempt(pending, "App Attest attestation returned no data");
            return;
        }
        if (instance == null) {
            return;
        }
        // Apple accepted the attestation, but only the backend can confirm it
        // recorded the key, so the key becomes pending rather than attested.
        // Every caller -- queued or newly arriving -- is told to retry until
        // confirmAttestation() lands or the grace window expires; otherwise a
        // caller arriving right after this callback would sail past the queue and
        // assert against a key the server has never seen. If the backend later
        // rejects it, the app calls DeviceIntegrity.resetAttestation() instead.
        // Same lock as the reset, for the same reason as nativeKeyGenerated: the
        // staleness check and the state it writes have to move together.
        synchronized (instance.flowLock) {
            if (isStale(pending)) {
                fail(pending, "App Attest state was reset while this request was in flight");
                return;
            }
            SecureStorage store = SecureStorage.getInstance();
            if (!store.set(KEY_STATE, STATE_PENDING)) {
                // The key is attested with Apple but we cannot record that. Reporting
                // success would leave the next request attesting the same key again,
                // against Apple's rate limit, forever.
                instance.bootstrapInFlight = false;
                fail(pending, "App Attest could not record its attestation state");
                instance.failBootstrapWaiters(
                        "App Attest could not record its attestation state");
                return;
            }
            if (!store.set(KEY_PENDING_SINCE, Long.toString(System.currentTimeMillis()))) {
                // Part of the same state transition, not a nicety: with no timestamp,
                // registrationGraceRemaining() reads the window as already expired, so
                // the very next request promotes the key to attested and asserts against
                // a key no backend has acknowledged -- the first-use rejection and
                // pointless key reset this state exists to prevent. Roll back to new so
                // the key is attested again rather than used prematurely.
                if (!store.set(KEY_STATE, STATE_NEW)) {
                    // The rollback failed too, so the key would sit pending with no
                    // deadline and be promoted on the next request. Discard the identity
                    // outright rather than leave a state that reads as ready.
                    try {
                        instance.resetLocked();
                    } catch (IllegalStateException ignored) {
                        // resetLocked already advanced the generation and failed the
                        // waiters; nothing further to do but report to this caller.
                    }
                    fail(pending, "App Attest could not record its registration deadline");
                    return;
                }
                instance.bootstrapInFlight = false;
                fail(pending, "App Attest could not record its registration deadline");
                instance.failBootstrapWaiters(
                        "App Attest could not record its registration deadline");
                return;
            }
            // The recovery this key came from, if any, is now complete. Leaving the
            // marker would refuse a future replacement when iOS legitimately invalidates
            // THIS key, and every later assertion would fail until the app reset by hand.
            store.remove(KEY_ATTEST_STARTED);
            // Attested, so there is no interrupted attestation left to remember.
            instance.attestAnsweredForKey = null;
            // The in-memory copy is cleared unconditionally, but the persisted marker is
            // only forgotten once the keychain confirms the deletion. A stale marker
            // would refuse the one-shot replacement the next time iOS legitimately
            // invalidates this key, leaving every assertion failing until the app reset
            // by hand -- so if the delete fails, the marker stays true here too and the
            // process keeps behaving as though the recovery were spent, which is the
            // conservative direction.
            instance.recoverySpentInMemory = !store.remove(KEY_RECOVERY_SPENT);
            instance.currentBackoff = MIN_BACKOFF_MILLIS;
            instance.bootstrapInFlight = false;
            // Deliberately NOT asserted here, for the reason above. Queued callers
            // are told to retry; by then the key is registered and their request
            // costs one cheap assertion.
            instance.failBootstrapWaiters("App Attest is completing first-run "
                    + "registration for this device; retry shortly");
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
        if (instance != null) {
            // ONE acquisition covering the staleness check and everything it guards, for
            // the reason spelled out on nativeAttestError. Split in two, a reset landing
            // between them let this callback clear the throttle state belonging to the
            // replacement generation: if that replacement had already recorded a
            // serverUnavailable, its deadline was erased here while succeed() went on to
            // reject this token as stale anyway -- so the next request went straight back
            // at a service Apple had just told us to stay away from, which is how an app
            // gets its whole attestation budget suspended.
            synchronized (instance.flowLock) {
                if (isStale(pending)) {
                    // A reset landed while this assertion was in flight -- typically
                    // because a concurrent request learned the backend does not
                    // recognise the key. Handing back an assertion for the key that was
                    // just discarded would send the server what it already rejected.
                    fail(pending,
                            "App Attest state was reset while this request was in flight");
                    return;
                }
                // A working assertion means Apple is answering again, so the throttle
                // sequence starts over. Without this the doubling only ever accumulated:
                // outages separated by months of successful requests would compound
                // until one transient failure imposed the full hour.
                instance.currentBackoff = MIN_BACKOFF_MILLIS;
                instance.retryAfterFallback = 0L;
                SecureStorage.getInstance().remove(KEY_RETRY_AFTER);
            }
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
        if (instance != null) {
            // The staleness check and every mutation this callback performs sit inside
            // ONE acquisition. Checking under the lock and then releasing it let a reset
            // bump the generation and start a replacement in between, and this callback
            // would then discard that replacement -- or, for a non-invalid-key error,
            // clear the in-flight flag and waiters belonging to it.
            synchronized (instance.flowLock) {
                if (isStale(pending)) {
                    fail(pending,
                            "App Attest state was reset while this request was in flight");
                    return;
                }
                boolean recoverySpent = pending.retried || instance.recoverySpentInMemory
                        || SecureStorage.getInstance().get(KEY_RECOVERY_SPENT) != null;
                if (errorCode == DC_ERROR_INVALID_KEY && !recoverySpent) {
                    // The key is gone or was never valid. Wipe it and try once from
                    // scratch; a second failure is reported rather than looped.
                    try {
                        instance.resetLocked();
                    } catch (IllegalStateException e) {
                        // Nothing was discarded, so starting a replacement would leave
                        // two identities and the old one still usable. Fail the caller.
                        instance.bootstrapInFlight = false;
                        fail(pending, "App Attest could not discard the rejected key");
                        instance.failBootstrapWaiters(
                                "App Attest could not discard the rejected key");
                        return;
                    }
                    // Recorded before the replacement starts, so a request arriving after
                    // this process dies still sees the recovery as used. The in-memory
                    // copy is set regardless: if the keychain refuses the write, the
                    // one-shot limit still holds for the life of the process rather than
                    // letting every later request burn another key.
                    instance.recoverySpentInMemory = true;
                    SecureStorage.getInstance().set(KEY_RECOVERY_SPENT, "1");
                    PendingRequest retry = new PendingRequest(pending.result, pending.nonce,
                            PendingRequest.OP_GENERATE_KEY, null);
                    retry.retried = true;
                    // resetLocked() clears the flag, so set it again before starting the
                    // replacement -- still under the same lock, so nothing observes the
                    // cleared state.
                    instance.bootstrapInFlight = true;
                    int rid = register(retry);
                    instance.nativeInstance.appAttestGenerateKey(rid);
                    return;
                }
                if (errorCode == DC_ERROR_SERVER_UNAVAILABLE) {
                    // Never retry a throttle in a loop -- that is what gets an app's
                    // whole attestation budget suspended. Recorded in memory as well as
                    // in the keychain, because a refused write must not mean no backoff.
                    long backoff = instance.currentBackoff;
                    long deadline = System.currentTimeMillis() + backoff;
                    instance.retryAfterFallback = Math.max(instance.retryAfterFallback,
                            deadline);
                    SecureStorage.getInstance().set(KEY_RETRY_AFTER,
                            Long.toString(deadline));
                    instance.currentBackoff = Math.min(backoff * 2, MAX_BACKOFF_MILLIS);
                }
                if (pending.op == PendingRequest.OP_ATTEST) {
                    // The marker means "submitted, outcome unknown", and the
                    // interrupted-attestation branch answers an unknown outcome by
                    // discarding the key, because a spent one-time attestation cannot be
                    // resubmitted. But reaching this callback at all means Apple
                    // answered: the outcome is known, and for anything other than
                    // invalidKey -- which is handled above by resetting -- the key was
                    // not consumed. Leaving the marker set therefore burned a
                    // rate-limited hardware key on every request for as long as the
                    // condition lasted, whether that was an outage or a persistently
                    // invalid input.
                    //
                    // unknownSystemFailure is the one code that could in principle have
                    // landed after Apple consumed the attestation. It is cleared anyway:
                    // resubmitting a consumed key is answered with invalidKey, which
                    // routes into the one-shot recovery above and self-corrects, whereas
                    // keeping the marker costs a fresh hardware key per request with
                    // nothing bounding it.
                    //
                    // The removal is checked. If the keychain refuses it, the marker
                    // stays and the next request reads it as an unknown outcome -- back
                    // to discarding a reusable key and generating another, which is the
                    // whole failure this clearing exists to prevent. Remembering the
                    // answered key in memory covers the life of this process; a restart
                    // still reads the marker and discards once, which is where this
                    // branch started and is bounded.
                    if (!SecureStorage.getInstance().remove(KEY_ATTEST_STARTED)) {
                        instance.attestAnsweredForKey = pending.keyId;
                    }
                }
                if (pending.op != PendingRequest.OP_ASSERT) {
                    // Still the same acquisition: releasing here and reacquiring would
                    // reopen the window on this mutation alone, clearing the in-flight
                    // flag of whatever bootstrap had started meanwhile.
                    instance.bootstrapInFlight = false;
                    instance.failBootstrapWaiters(
                            msg == null ? "App Attest failed (code " + errorCode + ")" : msg);
                }
            }
        }
        String message = msg == null ? "App Attest failed (code " + errorCode + ")" : msg;
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

    /**
     * Completes a caller with a token, unless a reset overtook it.
     *
     * <p>Completion is scheduled onto the EDT, so between the callback deciding the
     * token is good and the caller receiving it, another request can reset attestation
     * -- and the caller would then be handed an assertion for the key that was just
     * discarded, which is exactly what the staleness checks exist to suppress. The
     * generation is therefore checked again at the moment of publication.</p>
     */
    private static void succeed(final PendingRequest pending, final String token) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (pending.result.isDone()) {
                    return;
                }
                if (instance == null) {
                    pending.result.complete(token);
                    return;
                }
                // The staleness check happens under the lock; the completion itself does
                // not, and that is deliberate. complete() runs the app's own listeners
                // synchronously, and holding the attestation lock across arbitrary
                // application code is how the deadlock earlier in this class happened.
                //
                // The residual window is a reset landing between the check and the
                // handover. What the caller then holds is an assertion for a key the
                // backend no longer knows, which it rejects; the app resets and retries,
                // which is the recovery path that already exists. A deadlocked EDT has
                // no such recovery, so the trade runs this way round.
                synchronized (instance.flowLock) {
                    if (isStale(pending)) {
                        pending.result.error(new RuntimeException("App Attest state was "
                                + "reset while this request was in flight"));
                        return;
                    }
                }
                pending.result.complete(token);
            }
        });
    }

    /**
     * Fails a caller that has no PendingRequest yet, on the EDT.
     *
     * <p>Static, and not merely for tidiness: the callers are instance methods, so an
     * anonymous Runnable written inline there captures the enclosing IOSDeviceIntegrity.
     * That reference outlives the call for as long as the EDT queue holds the Runnable,
     * and it is a reference nothing here needs.</p>
     */
    private static void failResource(final AsyncResource<String> r, final String msg) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (!r.isDone()) {
                    r.error(new RuntimeException(msg));
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
