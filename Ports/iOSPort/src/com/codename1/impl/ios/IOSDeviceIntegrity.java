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
 * below. As with {@link IOSBiometrics}, the static initializer invokes each once
 * so the dead-code eliminator does not strip them -- no Java caller exists, and
 * without a reachable reference they become empty stubs and the native call
 * silently does nothing. Each returns immediately while that is happening; see
 * {@code dceGuard}.</p>
 */
final class IOSDeviceIntegrity {

    /**
     * True only while the retention calls below are running, so each callback returns
     * before it touches anything.
     *
     * <p>The same idiom as {@code IOSCarPlayCallbacks} and {@code IOSSurfaceCallbacks},
     * and it is deliberately not the more obvious {@code if (neverTrue) { call(); }}:
     * the call has to be unconditional for the eliminator to be certain to keep it,
     * whereas a branch on a field nothing ever assigns is something an optimizer is
     * entitled to fold away -- taking the reference with it, silently, and leaving the
     * native dispatch calling an empty stub.</p>
     */
    private static boolean dceGuard;

    static {
        // Prevents the iOS VM optimizer from eliding these native callbacks: no Java
        // caller exists, and without a reference they translate to empty stubs and the
        // native dispatch silently does nothing.
        //
        // The guard is what makes the calls harmless. Without it they ran their real
        // bodies during class initialization -- before the static fields below this
        // block were assigned, since static initializers run in textual order -- so
        // take() synchronized on a null REQUESTS map and threw. This class initializes
        // on the EDT, so that NPE reached Display's EDT handler, which shows a modal
        // error dialog: the app hung at launch on a dialog nobody could dismiss, the
        // first time anything asked about device integrity.
        dceGuard = true;
        nativeKeyGenerated(-1, null);
        nativeAttestationReady(-1, null);
        nativeAssertionReady(-1, null);
        nativeAttestError(-1, -1, null);
        dceGuard = false;
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
     * When this key's attestation was accepted, held here as well as in the keychain.
     *
     * <p>The keychain copy is what survives a restart; this one is what survives the
     * keychain refusing the write. Without it an accepted key looked unregistered, and
     * every route out of that state cost a rate-limited hardware key.</p>
     */
    private long pendingSinceInMemory;
    /**
     * The key whose state this process holds in memory, when the keychain refused to
     * hold it.
     *
     * <p>Apple accepts an attestation once. If the write recording that fact fails, the
     * persisted state still describes a key that has never been submitted -- and acting
     * on it re-submits an already-consumed one-time key, which Apple answers with
     * invalidKey, which spends the one-shot recovery and mints a replacement. Every
     * route out of a failed state write costs a rate-limited hardware key unless
     * something remembers what actually happened.</p>
     */
    private String inMemoryStateKeyId;
    /** The state {@link #inMemoryStateKeyId} is really in. */
    private String inMemoryState;
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

    /**
     * Set when the keychain refused to delete part of a discarded identity.
     *
     * <p>Attestation refuses outright while it is set. The alternative is worse than a
     * failing app: half an identity with no markers reads as a fresh key whose
     * attestation never began, so every request would spend a rate-limited attempt on a
     * key that is already attested and already rejected. Cleared by a reset that
     * succeeds, and not persisted -- there is nowhere to persist it, since the failure
     * being recorded is that persistence is not working.</p>
     */
    private boolean discardFailed;
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
        resetLocked(false);
    }

    /// @param keepSpentMarker true when this reset is the discard step of a recovery
    ///        that has already recorded itself as spent. Clearing the marker there would
    ///        undo the one-shot limit at the exact moment it starts applying -- the
    ///        replacement key would look like a first recovery all over again.
    private void resetLocked(boolean keepSpentMarker) {
        SecureStorage store = SecureStorage.getInstance();
        // The two that carry the identity are checked. If the keychain refuses to
        // delete them the reset has not happened, and advancing the generation anyway
        // would report success while the next request reloads the very key the backend
        // asked us to discard -- and keeps asserting with it. Better to leave the state
        // visibly unchanged so the caller fails and can retry.
        boolean idGone = store.remove(KEY_ID);
        // The state goes only once the identifier is confirmed gone, and the ordering is
        // the whole point. Deleted unconditionally, a keychain that refused KEY_ID and
        // accepted KEY_STATE left an already-attested key behind with no state at all --
        // and the in-memory terminal flag that covers this does not survive a restart.
        // On the next launch requestToken() reads a key with no state as freshly
        // generated and submits it to Apple, which is a rate-limited attestation of a
        // key Apple has already attested, once per launch, forever. Leaving the state in
        // place keeps the surviving key readable as what it actually is.
        boolean stateGone = idGone && store.remove(KEY_STATE);
        store.remove(KEY_RETRY_AFTER);
        store.remove(KEY_PENDING_SINCE);
        // The markers that make a surviving key terminal are cleared only once the key
        // itself is confirmed gone. Clearing them first meant a keychain that deleted
        // KEY_STATE and refused KEY_ID left a known-invalid key behind with no state and
        // no start marker -- which the next request reads as a freshly generated key
        // whose attestation never began, and submits to Apple. Every request after it
        // does the same, against a rate limit, with the one-shot recovery marker also
        // gone so nothing stops the loop.
        boolean markerGone = true;
        if (idGone && stateGone) {
            store.remove(KEY_ATTEST_STARTED);
            attestAnsweredForKey = null;
            // The identity is gone, so anything this process remembered about it is too.
            inMemoryStateKeyId = null;
            inMemoryState = null;
            pendingSinceInMemory = 0L;
            discardFailed = false;
            if (!keepSpentMarker) {
                // Checked, and the in-memory flag follows what the keychain actually
                // did. Clearing it regardless reported a successful reset while the
                // persisted marker survived -- so the next invalidKey found the recovery
                // already spent and refused the one replacement it is allowed, which is
                // precisely what resetAttestation() was called to restore. A reset that
                // does not restore the thing it promises has to say so.
                markerGone = store.remove(KEY_RECOVERY_SPENT);
                recoverySpentInMemory = !markerGone;
            }
        }
        if (idGone && stateGone && !markerGone) {
            // The identity is gone, so requests can proceed and a fresh key will be
            // generated -- deliberately NOT the terminal discardFailed state, which
            // would refuse every request over a marker. What is wrong is narrower: the
            // one-shot recovery still reads as spent, which is the documented behaviour
            // of that marker rather than a new failure. The caller is told the reset
            // did not do everything it was asked to, and can retry it.
            generation++;
            bootstrapInFlight = false;
            failBootstrapWaiters("App Attest could not clear its recovery marker");
            throw new IllegalStateException("App Attest discarded its stored key but "
                    + "could not clear the spent-recovery marker; the keychain refused "
                    + "the deletion, so a replacement key would still be refused its "
                    + "one recovery attempt");
        }
        if (!idGone || !stateGone) {
            // The identity survives in part, and there may be nothing left to make it
            // terminal: a successfully attested key has no start marker and no spent
            // marker, by design, so the next request would read the retained key as
            // never submitted and attest it again -- a rate-limited attempt on a key
            // Apple has already attested, repeated per request. Nothing persisted can
            // express "this key is finished" once the keychain is refusing writes, so
            // the state is held in memory and requestToken refuses until a reset
            // succeeds or the process restarts.
            discardFailed = true;
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
            // It must acknowledge THIS key. A response for an earlier attestation can
            // arrive after a reset has already replaced the identity, and promoting on
            // that would mark a key attested that the backend has never seen -- so the
            // next assertion is rejected and costs another reset, which is the loop this
            // state exists to break. The in-memory identity counts as much as the
            // persisted one: both are only ever set for the key this process is actually
            // holding, and a reset clears both together.
            boolean pendingInMemory = keyId.equals(inMemoryStateKeyId)
                    && STATE_PENDING.equals(inMemoryState);
            if (!keyId.equals(store.get(KEY_ID)) && !pendingInMemory) {
                return;
            }
            // What this process knows wins over what the keychain managed to store, the
            // same rule the request path follows.
            //
            // When the write recording the attestation was refused, storage still says
            // "new" -- so reading it alone dropped the backend's acknowledgement of a key
            // Apple had already attested, and dropped it permanently: the acceptance is
            // never re-sent. The key then sat until the grace window promoted it, or, if
            // the app restarted first, was read as an interrupted attestation (the start
            // marker is still there, because the path that clears it never ran) and
            // discarded for another rate-limited key. Honouring the in-memory state costs
            // nothing when the keychain is healthy and is the whole point of holding it.
            if (!pendingInMemory && !STATE_PENDING.equals(store.get(KEY_STATE))) {
                return;
            }
            if (!store.set(KEY_STATE, STATE_ATTESTED)) {
                // Still a promotion, just one only this process knows about -- exactly
                // what the grace-window promotion does when the keychain refuses it.
                inMemoryStateKeyId = keyId;
                inMemoryState = STATE_ATTESTED;
            }
            store.remove(KEY_PENDING_SINCE);
            // Then the rest of the bookkeeping the attestation callback does on its way
            // out, because the branch that recorded a refused state write returned before
            // reaching any of it. The start marker left behind makes an attested key look
            // like an interrupted attempt on the next launch and costs a replacement key
            // -- the precise outcome the in-memory state exists to avoid, arrived at one
            // step later -- and a recovery marker left behind refuses the one-shot
            // replacement the next time iOS legitimately invalidates this key. Doing it
            // here rather than only there is harmless on the healthy path: it removes
            // entries that are already gone.
            store.remove(KEY_ATTEST_STARTED);
            attestAnsweredForKey = null;
            recoverySpentInMemory = !store.remove(KEY_RECOVERY_SPENT);
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
            if (discardFailed) {
                // A previous discard left half an identity behind because the keychain
                // refused a deletion. Attesting from here spends a rate-limited attempt
                // on a key that is already attested and already rejected, once per
                // request, so the app is told plainly instead. resetAttestation() clears
                // it if the keychain recovers.
                r.error(new RuntimeException("App Attest could not discard a rejected key "
                        + "and cannot safely generate another; call "
                        + "DeviceIntegrity.resetAttestation() once the device's keychain "
                        + "is writable again"));
                return r;
            }
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
            // What this process knows wins over what the keychain managed to store. The
            // keychain copy is what survives a restart; this one is what survives the
            // keychain refusing the write, and without it an accepted key reads as one
            // that was never submitted.
            if (keyId != null && keyId.equals(inMemoryStateKeyId) && inMemoryState != null) {
                state = inMemoryState;
            }
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
                if (!store.set(KEY_STATE, STATE_ATTESTED)) {
                    // Same reasoning one step later: a promotion the keychain refused
                    // must not send an accepted key back to attestKey on the next
                    // request. Held in memory for this process; a restart re-reads
                    // whatever the keychain does hold.
                    inMemoryStateKeyId = keyId;
                    inMemoryState = STATE_ATTESTED;
                }
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
                    // Ordered like resetLocked, and for the same reason: the markers
                    // that make a surviving key terminal go only once the key itself is
                    // confirmed gone. Removing KEY_ATTEST_STARTED while the keychain
                    // refused KEY_ID left an outcome-unknown key looking like one that
                    // was never submitted, so the next request handed it to attestKey
                    // again -- and if Apple had already consumed the original submission
                    // that burns another rate-limited attempt and drops the device into
                    // invalid-key recovery for a reason that was never true.
                    if (!store.remove(KEY_ID)) {
                        bootstrapInFlight = false;
                        failResource(r, "App Attest could not discard the key whose "
                                + "attestation was interrupted; the keychain refused the "
                                + "deletion");
                        return r;
                    }
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
            // The keychain refused the timestamp, and this process remembers when the
            // key was accepted. Falling through to 0 here is what made the accepted key
            // look unregistered and sent it back through attestation.
            IOSDeviceIntegrity live = instance;
            if (live != null && live.pendingSinceInMemory > 0) {
                long left = REGISTRATION_GRACE_MILLIS
                        - (System.currentTimeMillis() - live.pendingSinceInMemory);
                return left > 0 ? left : 0;
            }
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

    /**
     * JSON string escaping that is reversible.
     *
     * <p>Control characters are escaped, not replaced. Substituting a space for them
     * lost information: a nonce containing a newline and the same nonce containing a
     * space produced identical clientData, so the assertion no longer bound the exact
     * challenge the server issued -- and the server, recomputing the hash over what it
     * sent, would not match it either. The one-to-one binding between a challenge and
     * the bytes signed over it is the whole point of the nonce.</p>
     */
    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c < 0x20) {
                // Everything else below 0x20 has no short form and must be \\u-escaped.
                // Hand-rolled rather than String.format, which the CLDC-era core does
                // not have -- and the exact serialization is part of the wire contract,
                // so it has to be predictable rather than locale-dependent.
                sb.append("\\u00");
                sb.append(HEX[(c >> 4) & 0xf]);
                sb.append(HEX[c & 0xf]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

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
        if (dceGuard) {
            return;
        }
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
        if (dceGuard) {
            return;
        }
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
        // Labelled so the storage-failure branches can leave the critical section and
        // still hand the caller its attestation, which is the one thing in here that
        // cannot be produced a second time.
        shieldAttestState:
        synchronized (instance.flowLock) {
            if (isStale(pending)) {
                fail(pending, "App Attest state was reset while this request was in flight");
                return;
            }
            SecureStorage store = SecureStorage.getInstance();
            if (!store.set(KEY_STATE, STATE_PENDING)) {
                // The key is attested with Apple and the keychain will not record it.
                //
                // Reporting success would leave the next request attesting the same key
                // again -- but so did simply failing, because the persisted state still
                // says "new" with a start marker, which is read as an interrupted
                // attestation and discards the key for another rate-limited one. Every
                // route out of here spent a key until something remembered that Apple
                // had already answered. So the state is held in memory for this process,
                // exactly as the deadline below is, and this request is failed so the
                // caller retries into a state that now describes the key correctly.
                instance.inMemoryStateKeyId = pending.keyId;
                instance.inMemoryState = STATE_PENDING;
                instance.pendingSinceInMemory = System.currentTimeMillis();
                instance.bootstrapInFlight = false;
                instance.failBootstrapWaiters("App Attest is completing first-run "
                        + "registration for this device; retry shortly");
                // The caller still gets the attestation, because it is the ONE thing
                // here that cannot be produced again. Apple attests a key once; failing
                // this request threw that object away, so the backend never received the
                // key to register -- and after the grace window the client promotes it
                // locally and starts asserting against a key the backend has never seen,
                // which is rejected, which resets, which spends another rate-limited
                // key. Discarding an irreplaceable result to report a storage problem
                // costs strictly more than reporting nothing.
                //
                // Falls through to the same succeed() the normal path uses. The state is
                // pending in memory, so later callers take the registration-in-progress
                // path exactly as they would have.
                break shieldAttestState;
            }
            long pendingSince = System.currentTimeMillis();
            // Held in memory whatever the keychain does, and set BEFORE the write is
            // attempted so the fallback is already in place if it fails.
            instance.pendingSinceInMemory = pendingSince;
            if (!store.set(KEY_PENDING_SINCE, Long.toString(pendingSince))) {
                // The key is KEPT, in the pending state it is already in. Nothing is
                // rolled back and nothing is discarded.
                //
                // Two earlier attempts here were both wrong in the same direction --
                // they sent an accepted key back through attestation. Rolling the state
                // to new while KEY_ATTEST_STARTED was present made it read as an
                // interrupted attestation, which discards the key and mints another;
                // clearing that marker first only moved the failure, because a key in
                // STATE_NEW is submitted to attestKey again and Apple answers invalidKey
                // for a one-time key it has already consumed -- which triggers recovery
                // and burns a replacement anyway. Apple accepted this attestation. The
                // key is good. The only thing missing is a timestamp.
                //
                // So the timestamp lives in memory for this process (set above, before
                // the write was attempted) and registrationGraceRemaining() falls back
                // to it. Across a restart the key is found PENDING with no deadline,
                // which the request path already handles: it promotes to attested and
                // uses it, the same fallback applied when a consumer never acknowledges.
                // That is right here too -- the key IS attested with Apple; only the
                // backend's confirmation is unknown -- and it costs no rate-limited key.
                store.remove(KEY_ATTEST_STARTED);
                instance.bootstrapInFlight = false;
                instance.failBootstrapWaiters("App Attest is completing first-run "
                        + "registration for this device; retry shortly");
                // Same reasoning as the state write above: the attestation object is
                // one-time, so it goes to the caller rather than being discarded to
                // report that a timestamp could not be stored.
                break shieldAttestState;
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
        if (dceGuard) {
            return;
        }
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
        if (dceGuard) {
            return;
        }
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
                    //
                    // The spent marker is written BEFORE the identity is discarded, which
                    // is the ordering the whole one-shot limit rests on. Discarding first
                    // and then failing to record left no key at all AND no record that a
                    // recovery had happened -- so the next request took the plain
                    // generate-key path, which consults neither marker, and every request
                    // (and every launch, since the in-memory copy does not survive one)
                    // burned another rate-limited hardware key. Writing first means a
                    // refusal costs one request and changes nothing else: the rejected
                    // key is still there, and the terminal flag below stops it being
                    // attested again in this process.
                    instance.recoverySpentInMemory = true;
                    if (!SecureStorage.getInstance().set(KEY_RECOVERY_SPENT, "1")) {
                        instance.discardFailed = true;
                        instance.bootstrapInFlight = false;
                        fail(pending, "App Attest could not record that its one-time "
                                + "recovery had been used, so it was not attempted");
                        instance.failBootstrapWaiters("App Attest could not record that "
                                + "its one-time recovery had been used");
                        return;
                    }
                    try {
                        // Keeps the marker written a moment ago: this reset IS the
                        // recovery, so clearing it here would let the replacement look
                        // like a first recovery all over again.
                        instance.resetLocked(true);
                    } catch (IllegalStateException e) {
                        // Nothing was discarded, so starting a replacement would leave
                        // two identities and the old one still usable. Fail the caller.
                        instance.bootstrapInFlight = false;
                        fail(pending, "App Attest could not discard the rejected key");
                        instance.failBootstrapWaiters(
                                "App Attest could not discard the rejected key");
                        return;
                    }
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
                if (pending.op == PendingRequest.OP_ATTEST
                        && errorCode != DC_ERROR_INVALID_KEY) {
                    // invalidKey is excluded. Reaching here with it means the reset
                    // branch above declined -- the one-shot recovery is already spent --
                    // so this key is known bad and known unreplaceable. Clearing the
                    // marker would make the next request read it as a reusable key with
                    // an unstarted attestation and submit it to Apple again, once per
                    // request, instead of reporting the exhausted recovery the caller
                    // needs to see. Terminal has to stay terminal.
                    //
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
