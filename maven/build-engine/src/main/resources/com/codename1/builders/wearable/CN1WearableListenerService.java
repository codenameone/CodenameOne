/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import com.codename1.wearable.WearableConnection;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Receives Wearable Data Layer traffic and hands it to {@code com.codename1.wearable}. Injected into
 * the generated project alongside {@link CN1WearableBridge} only when the app references the
 * wearable API.
 *
 * <p>Android starts this service to deliver a message even when the app is not running, which is
 * exactly the case the Codename One API's cold-start queue exists for: everything here forwards
 * straight to {@code WearableConnection}, which parks the delivery until the app registers a
 * listener and then replays it on the EDT.
 *
 * <h2>Why an exported service is not an open door</h2>
 *
 * <p>The manifest has to export this -- a {@code WearableListenerService} that Play services
 * cannot bind is useless -- and there is no manifest permission that narrows who may bind. That
 * reads like an invitation for any installed app to bind it and hand us a forged message, and it
 * has been reported as one. It is not, and the reason is in the library rather than in this file,
 * which is why it is recorded here.</p>
 *
 * <p>Every event reaches a subclass through the binder {@code WearableListenerService.onBind}
 * returns, and that method is {@code final} -- there is no override point, and no need for one.
 * The binder ({@code com.google.android.gms.wearable.zzag}) begins each dispatch with
 * {@code Binder.getCallingUid()} and, on any uid it has not already accepted, calls
 * {@code UidVerifier.isGooglePlayServicesUid}, which requires the calling uid to own the
 * {@code com.google.android.gms} package AND that package to satisfy
 * {@code GoogleSignatureVerifier.isGooglePublicSignedPackage}. A uid that fails logs
 * "Caller is not GooglePlayServices" and the dispatch returns without invoking anything here.</p>
 *
 * <p>The started-intent route is closed too: {@code WearableListenerService} declares no
 * {@code onStartCommand}, so an intent from another app starts the service and delivers nothing.
 * A local app can therefore bind or start this service and still never reach
 * {@code onMessageReceived}, {@code onDataChanged} or {@code ensureAppRunning}.</p>
 *
 * <p>So the node checks below are NOT the barrier against a local attacker; the uid check already
 * is. They bound what an authentic Play services delivery may claim about which PEER sent it,
 * which is a different and weaker question -- see {@code dataItemExists}.</p>
 */
public class CN1WearableListenerService extends WearableListenerService {

    /**
     * The service has to be exported for Play services to bind it, and there is no manifest
     * permission that narrows who may bind. The CALLER is nevertheless authenticated, by the
     * library rather than by us -- see the class javadoc -- so this check is about which NODE an
     * authentic Play services delivery names, not about who delivered it.
     *
     * <p>A node id is not a secret and does not authenticate the node: a compromised or
     * misbehaving peer can name another. What cannot be faked is the item itself, so a data CHANGE
     * is additionally confirmed to exist in our namespace; see {@code dataItemExists}.
     *
     * <p>The check is against a recent snapshot rather than a fresh query, so a peer that drops off
     * between Play services queueing the callback and the check running does not cost us a message
     * the Data Layer already accepted -- see {@code CN1WearableBridge.isKnownNode}.
     */
    private boolean isFromAKnownNode(String sourceNodeId) {
        return CN1WearableBridge.isKnownNode(this, sourceNodeId);
    }

    /**
     * The node that published a data item. The Data Layer puts it in the item's Uri authority
     * ({@code wear://<nodeId>/<path>}), which is the same provenance {@code onMessageReceived} gets
     * from the message event -- and this service is exported, so it is checked the same way.
     */
    private boolean isFromAKnownHost(android.net.Uri uri) {
        return uri != null && isFromAKnownNode(uri.getHost());
    }

    /**
     * Whether this data item is really in our Data Layer, rather than merely claimed to be.
     *
     * <p>A node id does not authenticate the node it names -- it is not a secret, and a peer that
     * is compromised or simply wrong can present another one. It bounds who can be impersonated,
     * it does not establish who spoke.</p>
     *
     * <p>The item itself can be checked, though, and that is a real answer. Data Layer items live
     * in the writing package's namespace, so nothing outside this app -- on this device or a paired
     * one -- can put one at this uri. Reading it back through OUR client and finding it there is
     * proof the event describes something that actually happened, whoever delivered the news.</p>
     *
     * <p>A deletion has nothing left to read, so it cannot be confirmed this way and falls back to
     * the membership check. So does a message: {@code MessageClient} is transient by design and
     * leaves no record to verify against. What that leaves unverified is the NODE, not the caller;
     * the class javadoc records why a local app cannot reach any of these callbacks at all.</p>
     */
    private boolean dataItemExists(android.net.Uri uri) {
        if (uri == null) {
            return false;
        }
        try {
            com.google.android.gms.wearable.DataItemBuffer items =
                    com.google.android.gms.tasks.Tasks.await(
                            com.google.android.gms.wearable.Wearable.getDataClient(this)
                                    .getDataItems(uri),
                            10, java.util.concurrent.TimeUnit.SECONDS);
            try {
                return items.getCount() > 0;
            } finally {
                items.release();
            }
        } catch (Throwable unavailable) {
            // The query failed rather than answered. Falling back to the membership check keeps a
            // genuine event from being dropped because Play services was briefly unavailable --
            // this is defence in depth over that check, not a replacement for it.
            return true;
        }
    }

    /**
     * Brings the app process up so its {@code init()} runs and its listeners exist.
     *
     * <p>Android starts this service in a dead process to deliver traffic. Queueing the delivery is
     * only half the answer: without the app itself starting, nothing ever registers a listener and
     * the queue is never drained. Launching is a no-op when the app is already running.
     */
    private void ensureAppRunning() {
        // One implementation, in the bridge: dispatch happens there too -- a resolved removal and a
        // retried asset are both announced from CN1WearableBridge, outside every launch site this
        // service owns -- and two copies of this would drift apart the moment one of them learned
        // something the other did not.
        CN1WearableBridge.ensureAppRunning();
    }

    @Override
    public void onMessageReceived(final MessageEvent event) {
        // Same reasoning as onDataChanged: isFromAKnownNode can block on a cold start.
        // Copied field by field rather than frozen: DataEvent is Freezable and MessageEvent is
        // not -- it is a plain four-method interface -- so there is no freeze() to call here. The
        // copy is what makes the hand-off safe, because Play services may recycle the event once
        // onMessageReceived returns and the worker below reads it after that.
        final MessageEvent frozen = new FrozenMessageEvent(event);
        MESSAGE_WORKER.execute(new Runnable() {
            public void run() {
                handleMessageReceived(frozen);
            }
        });
    }

    private void handleMessageReceived(MessageEvent event) {
        CN1WearableBridge.noteServiceContext(this);
        String path = event.getPath();
        if (path == null || !isFromAKnownNode(event.getSourceNodeId())) {
            return;
        }
        // Classified BEFORE the app is started, for the same reason the data path is. A recognised
        // node can still send something this process cannot act on -- a reply whose requester died,
        // a malformed request path, a path from a different build -- and launching first brought
        // the UI forward for a message that is discarded a few lines later.
        if (path.startsWith(CN1WearableBridge.surfaceReloadPath())) {
            // A watch asking the phone to publish a mirrored kind again. Framework traffic, so it
            // is answered here and never delivered to the app's own listeners -- the same
            // position and reasoning the /cn1surface descriptors get on the way down.
            //
            // The watch cannot refresh a mirrored surface itself: the content is produced here,
            // and it has no background-fetch listener recorded because that preference is written
            // by the publish path a watch never runs.
            surfaceMirror("reloadRequested",
                    path.substring(CN1WearableBridge.surfaceReloadPath().length()), null);
            return;
        }
        if (path.startsWith(CN1WearableBridge.replyUnavailablePath())) {
            // The peer says it cannot answer -- it is installed and reachable, but has no listener
            // and no permitted way to get one (see CN1WearableBridge.declineRequest). Failing the
            // request now is the whole point, so the deadline goes with it.
            //
            // NOT ensureAppRunning(): this is a failure notice for a request THIS process made, so
            // if a handler is still waiting the app is already up, and if it is not there is
            // nothing to show the user.
            String token = path.substring(CN1WearableBridge.replyUnavailablePath().length());
            try {
                int replyToken = Integer.parseInt(token);
                CN1WearableBridge.cancelReplyTimeout(replyToken);
                byte[] reason = event.getData();
                WearableConnection.deliverReply(replyToken, null,
                        reason == null || reason.length == 0
                                ? "The peer could not answer the request"
                                : new String(reason, "UTF-8"));
            } catch (NumberFormatException malformed) {
                // Not ours, or a peer running a different build.
            } catch (java.io.UnsupportedEncodingException never) {
                // UTF-8 is required of every JVM.
            }
            return;
        }
        if (path.startsWith(CN1WearableBridge.replyPath())) {
            // An answer to a request we sent. The token rides in the path.
            String token = path.substring(CN1WearableBridge.replyPath().length());
            try {
                int replyToken = Integer.parseInt(token);
                // A real answer arrived, so the deadline that would have failed this request is no
                // longer needed; leaving it scheduled holds a task per request for the full timeout.
                CN1WearableBridge.cancelReplyTimeout(replyToken);
                // Only when the request is still outstanding. A reply that arrives after the
                // requesting process was killed has no handler to reach -- deliverReply drops it --
                // so waking the app for one shows the user a window they did not ask for and
                // nothing else. A live request means a handler is waiting in THIS process, which
                // also means the app is already up, so this is a no-op in the normal case.
                if (WearableConnection.hasPendingReply(replyToken)) {
                    ensureAppRunning();
                }
                WearableConnection.deliverReply(replyToken, event.getData(), null);
            } catch (NumberFormatException malformed) {
                // Not ours, or a peer running a different build.
            }
            return;
        }
        if (path.startsWith(CN1WearableBridge.requestPath())) {
            // A message that wants an answer. The token and the CN1 path are both in the wire path:
            // /cn1/request/<token>/<escaped app path>
            String rest = path.substring(CN1WearableBridge.requestPath().length());
            int slash = rest.indexOf('/');
            if (slash < 0) {
                return;
            }
            try {
                int peerToken = Integer.parseInt(rest.substring(0, slash));
                // The path parsed and a peer is waiting for an answer, so this one is worth a
                // launch: without the app up nothing will ever answer it.
                ensureAppRunning();
                // Past the delimiter, not onto it: the encoded application path escapes its own
                // slashes, so this one belongs to the wire format and is not part of the app's path.
                CN1WearableBridge.spoolOrDeliverRequest(getApplicationContext(),
                        CN1WearableBridge.decode(rest.substring(slash + 1)),
                        event.getData(), peerToken, event.getSourceNodeId());
            } catch (NumberFormatException malformed) {
                // Not ours.
            }
            return;
        }
        if (path.startsWith(CN1WearableBridge.messagePath() + "/")) {
            ensureAppRunning();
            // Through the spool, not straight into the in-memory queue. A one-shot message is not
            // retained by the Data Layer, so if this service process is reclaimed before the user
            // opens the app there is nothing left to replay -- and the activity launch above cannot
            // be relied on to prevent that, because Android 10+ refuses a background start.
            CN1WearableBridge.spoolOrDeliverMessage(getApplicationContext(),
                    CN1WearableBridge.decode(
                            path.substring(CN1WearableBridge.messagePath().length() + 1)),
                    event.getData());
        }
    }

    /**
     * Where the Data Layer callbacks do their work.
     *
     * <p>Provenance checking blocks: on a cold start it runs {@code Tasks.await} for the local node,
     * the connected nodes and the capability set. Play services REFUSES a main-thread await, so if
     * these callbacks arrive on the main thread every one of those throws, each failure becomes an
     * empty snapshot, and the first legitimate event of a cold start is discarded as unverified --
     * while the retry loop sleeps the main thread on the way.
     *
     * <p>The documentation is not unambiguous about which thread {@code WearableListenerService}
     * uses, so this does not rely on the answer: the work is moved to a background thread either
     * way, which is correct under both readings and removes the ANR risk outright. Events are
     * frozen first because the buffer is recycled as soon as the callback returns.</p>
     */
    private static final java.util.concurrent.ExecutorService WORKER =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    /// Live messages get their OWN thread.
    ///
    /// A data event can hold the data worker for a long time and legitimately so: a cold-start
    /// resolution runs several five-second identity and capability queries and then
    /// resolveValueWithRetry, which is three more Data Layer attempts. Behind that, an unrelated
    /// request could sit long enough for the SENDER's thirty-second reply deadline to expire on a
    /// message that had already arrived -- the one kind of delivery where being late is the same as
    /// being lost. The two kinds of traffic have no ordering relationship with each other, so there
    /// is nothing to serialise between them.
    private static final java.util.concurrent.ExecutorService MESSAGE_WORKER =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override
    public void onDataChanged(DataEventBuffer events) {
        final java.util.List<DataEvent> frozen = new java.util.ArrayList<DataEvent>();
        for (DataEvent e : events) {
            frozen.add(e.freeze());
        }
        // Captured HERE, at arrival, not in the handler. The handler may run after a removeData
        // that this event predates, and clearing whatever marker it finds by then would wipe a
        // newer one. The generation pins the handler to the state it actually observed.
        final long removalGeneration = CN1WearableBridge.currentRemovalGeneration();
        // Which removals were in progress AT ARRIVAL. The 30-second window has to be judged from
        // here: a worker delayed past it -- two first-sight resolutions timing out is enough --
        // would otherwise find the marker expired and announce this device's own wildcard tombstone
        // back to the app as a peer removal.
        final java.util.Set<String> openRemovals = CN1WearableBridge.openRemovals();
        WORKER.execute(new Runnable() {
            public void run() {
                handleDataChanged(frozen, removalGeneration, openRemovals);
            }
        });
    }

    /**
     * An immutable copy of a delivered {@link MessageEvent}.
     *
     * <p>{@link com.google.android.gms.wearable.DataEvent} extends {@code Freezable} and hands out
     * a detached copy through {@code freeze()}; {@code MessageEvent} does not, so a hand-off to a
     * worker thread has to copy the four accessors by hand. Play services documents the delivered
     * event as valid only for the duration of the callback, and every read below happens after it
     * has returned.</p>
     *
     * <p>Implementing the interface, rather than passing the fields separately, keeps
     * {@code handleMessageReceived} typed against {@code MessageEvent}. If Play services ever adds
     * a fifth method this stops compiling, which is the intended way to find out.</p>
     */
    private static final class FrozenMessageEvent implements MessageEvent {
        private final int requestId;
        private final String path;
        private final String sourceNodeId;
        private final byte[] data;

        FrozenMessageEvent(MessageEvent event) {
            requestId = event.getRequestId();
            path = event.getPath();
            sourceNodeId = event.getSourceNodeId();
            byte[] payload = event.getData();
            data = payload == null ? null : (byte[]) payload.clone();
        }

        @Override
        public int getRequestId() {
            return requestId;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getSourceNodeId() {
            return sourceNodeId;
        }

        @Override
        public byte[] getData() {
            return data;
        }
    }

    /** The port's surface mirror, or null on a port that predates it. See {@link #surfaceMirror}. */
    private static Class<?> mirrorClass;
    private static boolean mirrorLookedUp;

    /**
     * The mirror class, looked up once, or null when this port has no surfaces implementation.
     *
     * <p>Reflective on purpose. This service is injected into EVERY build that references
     * {@code com.codename1.wearable}, including a versioned build pinned to a Codename One
     * release older than external surfaces -- and a hard reference to a class that port does not
     * contain fails javac in a file the developer never wrote, for a feature they never enabled.
     * The same reason {@code CN1WatchSurfaceNotifier} reaches for the androidx complication
     * classes this way.</p>
     *
     * <p>Safe against R8 because the two conditions coincide: the builder emits
     * {@code -keep class com.codename1.impl.android.surfaces.**} exactly when the app uses
     * surfaces, which is exactly when this class is present to be found. A rename would otherwise
     * turn this into the failure the reflection ban exists for -- working in the simulator and
     * silently dead in a release build.</p>
     *
     * @return the mirror class, or null
     */
    private static synchronized Class<?> mirrorClass() {
        if (!mirrorLookedUp) {
            mirrorLookedUp = true;
            try {
                mirrorClass = Class.forName(
                        "com.codename1.impl.android.surfaces.CN1SurfaceMirror");
            } catch (Throwable t) {
                // A port without surfaces. Mirror traffic cannot arrive for it either, because
                // nothing on the phone half would have sent any.
                mirrorClass = null;
            }
        }
        return mirrorClass;
    }

    /** Whether a path belongs to the surface mirror rather than to the application. */
    private static boolean surfaceMirrorHandles(String path) {
        Class<?> mirror = mirrorClass();
        if (mirror == null || path == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(mirror.getMethod("isMirrorPath", String.class)
                    .invoke(null, path));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Hands one piece of mirror traffic to the port.
     *
     * @param method {@code receive}, {@code receiveFile} or {@code remove}
     * @param path the reserved application path
     * @param payload the payload, or null for {@code remove}
     * @return true when the mirror took it; false when there is no mirror or it threw. A caller
     *         that acknowledges delivery has to know the difference, because an acknowledgement
     *         is durable and stops the sender retrying
     */
    /// How many times a failed mirrored descriptor write is re-attempted, and the delay before
    /// the first. The delays double, so the last attempt is a little over twenty minutes out --
    /// long enough to outlast the transient conditions this is for (storage momentarily full, a
    /// directory briefly unwritable) without holding the payload for ever.
    private static final int MIRROR_WRITE_RETRIES = 6;
    private static final long MIRROR_WRITE_RETRY_MILLIS = 20000L;

    /// How many mirror events each reserved path has seen, so a delayed retry can tell whether it
    /// has been overtaken. Small and bounded by the number of declared kinds.
    private static final java.util.HashMap<String, Long> MIRROR_GENERATIONS =
            new java.util.HashMap<String, Long>();

    private static synchronized long bumpMirrorGeneration(String path) {
        Long current = MIRROR_GENERATIONS.get(path);
        long next = (current == null ? 0L : current.longValue()) + 1L;
        MIRROR_GENERATIONS.put(path, Long.valueOf(next));
        return next;
    }

    private static synchronized long mirrorGeneration(String path) {
        Long current = MIRROR_GENERATIONS.get(path);
        return current == null ? 0L : current.longValue();
    }

    /**
     * Re-attempts a mirrored descriptor the watch could not store.
     *
     * <p>Bounded, and in memory. What it cannot cover is the process dying mid-outage: the
     * payload goes with it and the unchanged Data Layer item produces no fresh callback, so that
     * descriptor waits for the phone's next publish. Persisting it to survive that would mean
     * writing to the storage that just refused a write, which is the condition being retried.</p>
     *
     * @param path the reserved application path
     * @param payload the descriptor payload, held for the retry
     * @param attempt 1 for the first re-attempt
     */
    /**
     * Re-attempts a mirrored withdrawal the watch could not carry out.
     *
     * <p>The mirror image of {@link #retryMirrorDescriptor}, and needed for a sharper reason: a
     * descriptor that fails to apply is at least offered again by the next publish, while a
     * deletion is offered once and never again.</p>
     *
     * @param path the reserved application path
     * @param attempt 1 for the first re-attempt
     * @param generation the mirror generation this withdrawal belongs to
     */
    private void retryMirrorRemoval(final String path, final int attempt, final long generation) {
        if (mirrorGeneration(path) != generation) {
            // Overtaken by a republish, which supersedes the withdrawal outright.
            return;
        }
        if (attempt > MIRROR_WRITE_RETRIES) {
            android.util.Log.w("CN1Surfaces", "gave up withdrawing the mirrored surface on "
                    + path + " after " + MIRROR_WRITE_RETRIES + " attempts; the watch keeps "
                    + "showing it until the phone publishes that kind again");
            return;
        }
        final CN1WearableListenerService self = this;
        CN1WearableBridge.scheduleFrameworkRetry(new Runnable() {
            public void run() {
                if (mirrorGeneration(path) != generation) {
                    return;
                }
                if (!self.surfaceMirror("remove", path, null)) {
                    self.retryMirrorRemoval(path, attempt + 1, generation);
                }
            }
        }, MIRROR_WRITE_RETRY_MILLIS << (attempt - 1));
    }

    private void retryMirrorDescriptor(final String path, final byte[] payload,
            final int attempt, final long generation) {
        if (mirrorGeneration(path) != generation) {
            // Overtaken. A newer descriptor for this path -- or its tombstone -- has been handled
            // since this retry was scheduled, so applying the payload now would either overwrite
            // content that is newer than it or resurrect a surface the phone has withdrawn. The
            // newer event has its own retry if it needs one.
            return;
        }
        if (payload == null || attempt > MIRROR_WRITE_RETRIES) {
            android.util.Log.w("CN1Surfaces", "gave up applying the mirrored surface on " + path
                    + " after " + MIRROR_WRITE_RETRIES + " attempts; the watch keeps what it had "
                    + "until the phone publishes again");
            return;
        }
        final CN1WearableListenerService self = this;
        CN1WearableBridge.scheduleFrameworkRetry(new Runnable() {
            public void run() {
                if (mirrorGeneration(path) != generation) {
                    // Checked again here, not only on entry: the overtaking event usually lands
                    // while this task is sitting on the timer, which is the whole window.
                    return;
                }
                if (!self.surfaceMirror("receive", path, payload)) {
                    self.retryMirrorDescriptor(path, payload, attempt + 1, generation);
                }
            }
        }, MIRROR_WRITE_RETRY_MILLIS << (attempt - 1));
    }

    private boolean surfaceMirror(String method, String path, byte[] payload) {
        Class<?> mirror = mirrorClass();
        if (mirror == null) {
            return false;
        }
        try {
            // A null payload means the two-argument form, whichever method it is: remove and
            // reloadRequested both take (Context, String) and neither carries bytes.
            if (payload == null) {
                Object removed = mirror
                        .getMethod(method, android.content.Context.class, String.class)
                        .invoke(null, this, path);
                // Its own answer. remove used to be void and this returned true regardless, so a
                // withdrawal the watch could not carry out looked like one that had -- and a
                // deletion is offered exactly once, so nothing would have tried again. A port
                // still declaring the void form answers null here, which is treated as success
                // exactly as it was before: it is the same old behaviour for the same old port.
                return !(removed instanceof Boolean) || ((Boolean) removed).booleanValue();
            }
            Object answer = mirror
                    .getMethod(method, android.content.Context.class, String.class, byte[].class)
                    .invoke(null, this, path, payload);
            // The mirror's own answer where it has one. receiveFile catches its write failures
            // internally and reports them by returning false, so a call that merely did not throw
            // proves nothing -- and an acknowledgement made on that basis is durable, which loses
            // the artwork rather than having it redelivered. A void method (receive) answers null
            // and is taken at its word.
            return !(answer instanceof Boolean) || ((Boolean) answer).booleanValue();
        } catch (Throwable t) {
            // Caught rather than propagated: a listener that throws takes the Data Layer
            // callback down with it, and mirror traffic is a refresh rather than something the
            // app is waiting on. Logged under the mirror's own tag so it reads beside the
            // failures the mirror reports itself.
            android.util.Log.w("CN1Surfaces",
                    "Could not hand " + path + " to the surface mirror", t);
            return false;
        }
    }

    /**
     * The payload of a mirrored surface item.
     *
     * <p>Read straight from the DataMap rather than through the bridge's ordering machinery: a
     * mirror is a replacement, not a replicated value with a logical clock, and the newest write
     * always wins.</p>
     */
    private static byte[] readMirrorPayload(DataEvent event) {
        try {
            return DataMapItem.fromDataItem(event.getDataItem()).getDataMap()
                    .getByteArray(CN1WearableBridge.payloadKey());
        } catch (Throwable t) {
            return null;
        }
    }

    private void handleDataChanged(java.lang.Iterable<DataEvent> events, long removalGeneration,
            java.util.Set<String> openRemovals) {
        // Before anything is read: in a cold service process this is the only context there is, and
        // the logical clock has to be restored from it before observations are compared, and
        // persisted through it afterwards.
        CN1WearableBridge.noteServiceContext(this);
        // NOT before the loop. The caller is authentic Play services (see the class javadoc), but
        // an event it delivers can still name a node we do not know, and starting the app first
        // let such an event bring the UI forward on a payload the check below then discards -- a
        // launch cannot be undone. The app is started on the first event that proves it came from
        // a known node, which is also the first event that could
        // give the app anything to do.
        boolean started = false;
        for (DataEvent event : events) {
            android.net.Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            // An echo of this device's own publish. It stays fully visible to getData() and to the
            // ordering bookkeeping below -- it IS the path's current value -- but it is not a peer
            // event, and WearableDataListener documents its callbacks as peer changes. iOS and the
            // simulator already suppress self-authored changes; forwarding them here made the same
            // app code fire an extra callback on Android only, so an app that acts on a change
            // processed its own write twice.
            boolean transferItem = CN1WearableBridge.isTransferPath(path);
            // A receiver's acknowledgement of one of OUR transfers. It is the event that says the
            // file has been taken and our copy can go, and the sweep it arms is the only thing that
            // removes that copy before the hard cap -- the bridge registers no DataClient listener
            // of its own, so without this the sender sat on a delivered file for seven days unless
            // it happened to send another or restart.
            //
            // Never an app-visible callback: it is bookkeeping between the two ports, on a path no
            // application ever names.
            if (path != null && CN1WearableBridge.ackedTransferKey(path) != null) {
                if (isFromAKnownHost(uri) && event.getType() != DataEvent.TYPE_DELETED
                        && dataItemExists(uri)) {
                    CN1WearableBridge.sweepAfterAcknowledgement(getApplicationContext());
                }
                continue;
            }
            if (path == null || !isFromAKnownHost(uri)
                    || (!transferItem && !path.startsWith(CN1WearableBridge.pathPrefix()))) {
                continue;
            }
            // And, for a change, that the item is really there. See dataItemExists: the node id in
            // the event is not a secret and does not authenticate anyone, but nothing outside this
            // app can put an item at this uri, so reading it back is proof the change happened. A
            // deletion has nothing left to read and keeps the membership check alone.
            if (event.getType() != DataEvent.TYPE_DELETED && !dataItemExists(uri)) {
                continue;
            }
            // Computed AFTER the provenance check, not before it. isFromAKnownHost retries the
            // identity queries, so a getLocalNode() that failed on the first attempt can succeed
            // inside it -- and an ownEcho decided beforehand was still false, so this device's own
            // putData came back through the peer-change path and an app acting on changes
            // processed its own write twice.
            boolean ownEcho = CN1WearableBridge.isLocallyAuthored(this, uri.getHost());
            // The events that deliver NOTHING are filtered before the app is started.
            //
            // ensureAppRunning used to come first, so on a device that permits the background
            // activity start, a sender tidying up an acknowledged or expired transfer -- which can
            // be a day later -- brought the receiver's UI to the front to process an event that is
            // discarded two lines on. The same went for this device's own removal echoing back.
            boolean deleted = event.getType() == DataEvent.TYPE_DELETED;
            if (transferItem && deleted) {
                // Our own consumeTransfer, or the sender clearing up. Not an app-visible removal:
                // the logical path may well still hold a replicated value.
                continue;
            }
            String appPath = transferItem
                    ? null
                    : CN1WearableBridge.decode(
                            path.substring(CN1WearableBridge.pathPrefix().length()));
            // Framework bookkeeping, routed BEFORE anything app-visible and before
            // ensureAppRunning -- the same position and the same reasoning the acknowledgement
            // traffic above uses. A mirrored complication is applied by writing a file and asking
            // the watch face to re-read; there is nothing for the application to do, and starting
            // it would bring a UI forward that the user did not ask for. Delivering it to the
            // app's own listeners would also show it a message it never sent itself.
            if (appPath != null
                    && surfaceMirrorHandles(appPath)) {
                // Deletions too, and NOT through the ordinary value-removal path below. A mirror
                // is a replacement rather than a replicated value, so it never entered that
                // path's cache or its logical clock, and letting a tombstone go there left the
                // descriptor CN1SurfaceMirror.receive wrote sitting on disk -- the complication
                // kept showing content the phone had already withdrawn.
                // Every mirror event for this path moves its generation on, which is what lets a
                // scheduled retry tell that it has been overtaken. Bumped BEFORE the work, so a
                // retry scheduled by this very event carries the current number.
                long generation = bumpMirrorGeneration(appPath);
                if (deleted) {
                    // The tombstone is consumed either way, and deliberately. A Data Layer
                    // deletion is not redelivered -- unlike a changed item there is nothing left
                    // to ask for -- so there is no later attempt to preserve it for.
                    //
                    // Which is exactly why a FAILED removal has to be retried here rather than
                    // dropped: nothing else will ever offer this deletion again, so a directory
                    // that is momentarily unwritable would leave the complication showing content
                    // the phone withdrew, permanently. Same generation guard as a descriptor
                    // retry, so a republish landing meanwhile cancels the withdrawal instead of
                    // racing it.
                    if (!surfaceMirror("remove", appPath, null)) {
                        retryMirrorRemoval(appPath, 1, generation);
                    }
                } else {
                    byte[] descriptor = readMirrorPayload(event);
                    if (!surfaceMirror("receive", appPath, descriptor)) {
                        // The write failed -- storage momentarily full is the case this is for.
                        // Nothing else will offer this descriptor again: a Data Layer item that
                        // has not changed produces no further callback, so the watch would keep
                        // showing content the phone has already replaced, indefinitely. The
                        // payload is in hand, so the retry needs no round trip.
                        retryMirrorDescriptor(appPath, descriptor, 1, generation);
                    }
                }
                continue;
            }
            // Read before anything is cleared, so the reset below can tell this device's own
            // removal from a republish that landed while it was being processed.
            String beforeTombstone = !transferItem && deleted
                    ? CN1WearableBridge.deliveredStamp(appPath) : null;
            if (!transferItem && deleted && openRemovals.contains(path)) {
                // This device's own removeData coming back, for an ordinary value. The app made the
                // call, so announcing it would break the peer-only contract in the other direction.
                //
                // The arrival snapshot is the ONLY test here, deliberately. ownEcho asks who published
                // the item, which for a tombstone is the wrong question twice over: a wildcard
                // removal produces a tombstone per replica and the peer-authority ones are not
                // locally authored (so ownEcho misses them), while a peer deleting a path THIS
                // device published leaves our authority on the tombstone (so ownEcho claims it as
                // ours and swallowed a genuine peer removal, with no later event guaranteed to
                // correct it). What matters is what this device ASKED to remove, which is what the
                // removal markers record -- read at arrival, not at handler time.
                //
                // The cached value goes, or a latency-sensitive getData() would keep answering with
                // a value this device has just deleted -- and so does the ordering baseline.
                // Keeping the baseline was wrong for a peer whose logical clock never caught up
                // with ours: an offline peer republishing the path draws a sequence LOWER than the
                // winner we removed, deliverIfOutranks rejects it against a stamp describing an
                // item that no longer exists anywhere, and both the listener and getData() stay
                // empty with no later event to correct them. After a successful removal the path
                // holds nothing, so anything that arrives next is by definition the new winner.
                //
                // Both are cleared as ONE step, anchored on a stamp read BEFORE either moves. A
                // deferred resolution can deliver a peer republish concurrently with this
                // tombstone, and clearing first and reading after would remove the republish's own
                // stamp and snapshot -- leaving the app holding a value getData() no longer
                // returns, with no baseline against a later stale replica.
                CN1WearableBridge.forgetAfterLocalRemoval(appPath, beforeTombstone);
                continue;
            }
            // The app is started at the DISPATCH sites below, not here.
            //
            // Every pre-filter added ahead of this line only narrowed the problem: an unreadable
            // asset still downloading, a transfer another pass already claimed, and this device's
            // own echoed publication all reach this point and then deliver nothing. On a device
            // that permits the background activity start, each of those brought the receiver's UI
            // forward for an event no listener ever sees. Starting where the callback is actually
            // handed over is the version of this that cannot be outflanked by the next case.
            if (transferItem) {
                // A file transfer arrives as a DataMap carrying an Asset rather than an inline
                // payload. Turn it back into the WearableMessage the receiver expects; this callback
                // already runs off the main thread, so resolving the asset here is fine.
                CN1WearableBridge.Transfer transfer =
                        CN1WearableBridge.decodeTransfer(this, event.getDataItem());
                long transferSeq = CN1WearableBridge.sequenceOf(
                        CN1WearableBridge.valueOrTransferMap(event.getDataItem()));
                if (transfer.payload != null && !ownEcho
                        && CN1WearableBridge.claimTransfer(this, uri, transferSeq)) {
                    // On the path the sender passed to transferFile, not the filename-suffixed
                    // storage path this item happens to live at: a listener routes on what it asked
                    // for. The decoded payload carries the same path internally.
                    //
                    // A transfer is one-shot, so a re-sync of the same item must not deliver twice --
                    // but the duplicate is suppressed locally rather than by deleting the item. The
                    // item belongs to the sender, and deleting it here would propagate: with two
                    // watches paired to one phone, the first to connect would consume the file and
                    // the second would get the tombstone.
                    //
                    // The claim is made DURABLE only if this reached a live listener. Parked in the
                    // cold-start queue it is not safe yet: a process death would lose the payload
                    // while a persisted claim suppressed the redelivery that would replace it.
                    // Confirmed from inside the delivery: dispatched is not delivered, and a
                    // claim persisted for a file the app never received suppresses the redelivery
                    // that would have replaced it.
                    if (surfaceMirrorHandles(transfer.logicalPath)) {
                        // Mirrored complication artwork. Stored beside the descriptor that names
                        // it, without waking the app: see the data-item branch above.
                        // Confirmed only if it was actually stored. The helper catches whatever
                        // the mirror throws -- a directory that is momentarily unwritable, say --
                        // and a claim made anyway is durable: the sender stops retrying and the
                        // artwork is gone for good.
                        if (surfaceMirror("receiveFile", transfer.logicalPath, transfer.payload)) {
                            CN1WearableBridge.confirmTransferDelivered(this, uri, transferSeq,
                                    true);
                        } else {
                            // RELINQUISHED, not merely left unconfirmed. Passing false to
                            // confirmTransferDelivered returns without touching the in-memory
                            // claim claimTransfer already made, and that claim then suppresses
                            // every retry -- while the DataItem is unchanged, so nothing
                            // generates a fresh callback either. The artwork would be missing
                            // until the process restarted. relinquishTransfer drops the claim
                            // AND goes back to read the item, which is the same thing the
                            // tracked-delivery path below does when the listener never got the
                            // payload.
                            CN1WearableBridge.relinquishTransfer(this, uri);
                        }
                        continue;
                    }
                    if (!started) {
                        ensureAppRunning();
                        started = true;
                    }
                    final android.net.Uri claimed = uri;
                    final long claimedSeq = transferSeq;
                    final android.content.Context svc = this;
                    WearableConnection.deliverDataChangedTracked(
                            transfer.logicalPath, transfer.payload, new Runnable() {
                                public void run() {
                                    CN1WearableBridge.confirmTransferDelivered(
                                            svc, claimed, claimedSeq, true);
                                }
                            }, new Runnable() {
                                public void run() {
                                    // Evicted from the pending queue before any listener existed.
                                    // The in-memory claim would otherwise keep suppressing this
                                    // payload for the life of the process, and an unchanged
                                    // DataItem raises no new callback -- so this both releases the
                                    // claim and schedules a fresh read of the item.
                                    CN1WearableBridge.relinquishTransfer(svc, claimed);
                                }
                            });
                }
                // An unreadable asset delivers nothing now; decodeTransfer has scheduled a re-read,
                // which beats handing the listener DataMap bytes dressed up as a payload.
                continue;
            }
            if (deleted) {
                // The ordering stamp is dropped only once we know the path is genuinely empty --
                // see the branches below. Dropping it here, before the query, meant a query that
                // then FAILED left the path with no stamp at all, so an older item from another
                // authority arriving next would pass the newer-than-delivered test and win.
                // One authority's item going away does not mean the path is gone: both nodes may
                // have published it, and the other item can still be there. Reporting a removal on
                // the strength of this event alone would tell the listener the value disappeared
                // while getData(path) still returned it. Ask what is left and report that instead.
                try {
                    // Snapshot BEFORE the query: resolveValueWithRetry blocks and retries, so a
                    // newer publication can be delivered and stamped while it runs.
                    String beforeQuery = CN1WearableBridge.deliveredStamp(appPath);
                    // Retry, because a deletion is the ONLY callback for this state: if the path is
                    // now empty, or an unchanged lower-ranked replica is the survivor, nothing else
                    // will fire and staying silent leaves the listener permanently wrong while
                    // getData() reports otherwise. Same reasoning as the first-sight path.
                    CN1WearableBridge.ResolvedValue remaining =
                            CN1WearableBridge.resolveValueWithRetry(this, appPath);
                    // Recorded for EVERY deletion that resolves, survivor or not, and before the
                    // stamp tests below rather than inside either of them.
                    //
                    // Both restrictions were wrong. An older first-sight resolution may be in
                    // flight holding the item this deletion removed: if it captured the now-deleted
                    // HIGHER-ranked replica it can still pass deliverIfOutranks afterwards and
                    // restore a deleted value, which the survivor branch did nothing about. And
                    // when such a resolver is pending the path has no delivery stamp yet, so
                    // anything placed inside a stamp test never ran at all.
                    CN1WearableBridge.notePendingDeletion(appPath);
                    if (remaining != null) {
                        // Record the survivor's stamp outright -- the state of the path IS this
                        // item now. Using the newer-than test here would decline whenever the
                        // survivor carries a lower sequence than the winner just deleted (which is
                        // ordinary: the winner is gone precisely because it was removed), leaving
                        // the dead item's higher stamp recorded and filtering out any later item
                        // that falls between the two. So the outranks-guarded form is wrong here.
                        //
                        // But an UNconditional replace is wrong too: the query above blocks, and a
                        // newer publication delivered while it ran would be overwritten by this
                        // older survivor -- the newer callback has already fired and may not
                        // repeat, so the listener would sit regressed while getData() answered with
                        // the newer item. Anchoring on the pre-query stamp gets both: the dead
                        // winner's stamp is replaced whatever its number, and anything that landed
                        // meanwhile wins instead.
                        //
                        // Only when the winner actually changed. Deleting a lower-ranked SHADOW
                        // replica leaves the same item winning, and re-announcing a value the app
                        // already holds is a spurious change -- listeners re-render, and anything
                        // that treats a change as an event would act on it twice.
                        // AFTER the delivery, and only if it happened. This helper declines when
                        // another publication advanced the stamp while the query above was
                        // blocked -- the race the comments here already describe -- and starting
                        // the app for a callback that was never emitted foregrounds the UI for
                        // nothing. The queue is drained whenever a listener registers, so starting
                        // second costs the delivery nothing.
                        if (CN1WearableBridge.deliverIfStampUnchanged(appPath, beforeQuery,
                                remaining.sequence, remaining.node, remaining.payload)
                                && !started) {
                            ensureAppRunning();
                            started = true;
                        }
                    } else {
                        // Genuinely empty, so the stamp can go: a value republished here later
                        // with a lower stamp than the removed one carried must not be filtered as
                        // older.
                        //
                        // Announced only when that drop actually took a stamp. Deleting a
                        // replicated path deletes every authority's copy, and one buffer can carry
                        // a TYPE_DELETED event per item -- each would otherwise resolve empty and
                        // announce the same logical removal again, so a listener that treats
                        // removal as an event would act on it once per replica. The first event
                        // takes the stamp and reports; the rest find nothing and stay quiet.
                        //
                        // Anchored on beforeQuery, not merely "was something there": the query
                        // above blocks, so a newer publication delivered while it ran would
                        // otherwise have its stamp removed and a removal announced for a path
                        // that has already been refilled -- the newer callback has already fired
                        // and may not repeat.
                        //
                        // Inside this branch, never outside it: hoisting the call meant deleting a
                        // lower-ranked SHADOW replica -- where the winner survives and
                        // deliverIfStampUnchanged leaves the stamp as it found it -- dropped the
                        // LIVE winner's stamp, cleared its cached value and told the app the value
                        // had gone.
                        //
                        // Both rules live in the bridge, because the deferred resolver reaches the
                        // same point and knew only one of them.
                        CN1WearableBridge.announceResolvedRemoval(appPath, beforeQuery);
                    }
                } catch (java.io.IOException couldNotResolve) {
                    // The follow-up query failed rather than answering "nothing here". Still do not
                    // report a removal on that: it would tell the app a path had gone while another
                    // node may be publishing it, and a removal is not recoverable from the app's
                    // side.
                    //
                    // But staying silent is not safe either, which is what this used to do. A
                    // deletion is the ONLY callback for this state -- if the path is now empty, or
                    // an unchanged lower-ranked replica is the survivor, nothing else is guaranteed
                    // to fire, and the listener stays wrong for the life of the process while
                    // getData() reports otherwise. Resolve it later instead, and pass
                    // afterDeletion so an empty result is delivered as the removal it is rather
                    // than being read as "nothing to announce".
                    CN1WearableBridge.scheduleWinnerResolution(this, appPath, true);
                }
                continue;
            }
            com.google.android.gms.wearable.DataMap value =
                    CN1WearableBridge.valueMap(event.getDataItem());
            if (value == null) {
                // Under our prefix but not written by this API -- nothing to deliver.
                continue;
            }
            // A publication for this path ends any local-removal window: the wildcard delete it
            // covered is demonstrably over, so a peer's later removal must not be mistaken for it.
            //
            // Hoisted ABOVE the first-sight branch, which exits by `continue` -- leaving it below
            // meant a publication arriving with no delivery stamp recorded took that exit and never
            // cleared the marker, so a peer removing its brand-new value inside the window still
            // had that removal swallowed.
            CN1WearableBridge.clearLocalRemoval(path, removalGeneration);
            if (!CN1WearableBridge.hasDeliveredStamp(appPath)) {
                // First sight of this path in this process -- after a restart there is no baseline,
                // so accepting the event on the strength of "nothing recorded" would hand the app a
                // lower-ranked replica while a higher-ranked item exists on another node, and
                // getData() would immediately disagree. Resolve the actual winner instead.
                try {
                    CN1WearableBridge.ResolvedValue winner =
                            CN1WearableBridge.resolveValueWithRetry(this, appPath);
                    // Compare-and-replace AND dispatch as one step. resolveValueWithRetry blocks
                    // (and retries), so another callback can stamp this path with a newer
                    // publication while it runs -- and committing the stamp separately from the
                    // delivery leaves the same race one line further down: the newer payload goes
                    // out first, this older one after it, and the cache keeps the newer stamp so
                    // nothing is left that can correct the listener.
                    if (winner != null) {
                        // The resolved winner may be OUR OWN item -- a clean start whose first
                        // callback is the echo of this device's putData resolves to exactly that.
                        // Checking ownEcho on the event is not enough here: what matters is who
                        // published the winner, which resolution may have taken from a different
                        // authority than the event that triggered it.
                        if (CN1WearableBridge.isLocallyAuthored(this, winner.node)) {
                            CN1WearableBridge.recordLocalEcho(appPath, winner.sequence,
                                    winner.node, winner.payload);
                        } else if (CN1WearableBridge.deliverIfOutranks(appPath, winner.sequence,
                                winner.node, winner.payload) && !started) {
                            // Same ordering as above: deliverIfOutranks declines a replica an
                            // earlier publication already beat, and that is not worth a launch.
                            ensureAppRunning();
                            started = true;
                        }
                    }
                    continue;
                } catch (java.io.IOException couldNotResolve) {
                    // Every inline attempt failed. Do NOT fall back to the event we were handed: it
                    // may be a lower-ranked replica, and the winning item, being unchanged, may
                    // never produce another callback -- so the listener would disagree with
                    // getData() for the life of the process, with nothing left to correct it. That
                    // is the one outcome worse than a late delivery.
                    //
                    // Resolve it later instead, on a backoff, and leave the path unstamped so the
                    // next event for it still resolves from scratch.
                    CN1WearableBridge.scheduleWinnerResolution(this, appPath);
                    continue;
                }
            }
            // Ordering test and dispatch as ONE step, the same as the resolution paths. Testing
            // first and dispatching after leaves the window between them: a deferred resolution or
            // another callback can advance this path in between, emit the newer payload, and then
            // this callback emits the older one after it -- with the cache holding the newer stamp,
            // so that publication is rejected if seen again and nothing corrects the listener.
            //
            // An older item arriving after a newer one is ordinary, not exotic: a reconnect does it
            // whenever both nodes publish the same path. deliverIfOutranks declines those silently.
            if (ownEcho) {
                // The ONE deliberate use of the raw setter. Bookkeeping only: the stamp still has
                // to move so a later peer item is judged against what this device actually
                // published, but no listener is told, so there is no dispatch to order it with.
                // Every other stamp mutation goes through the deliver* helpers, which commit the
                // stamp and the delivery together.
                // Stamp and snapshot together, or neither. The stamp update declines when a newer
                // publication was recorded meanwhile, and an unconditional snapshot write then put
                // this echo's older bytes in front of it -- so a latency-sensitive getData() kept
                // answering with them while the delivery stamp already tracked the newer value.
                CN1WearableBridge.recordLocalEcho(appPath, CN1WearableBridge.sequenceOf(value),
                        uri.getHost(), CN1WearableBridge.payloadOf(value));
            } else if (CN1WearableBridge.deliverIfOutranks(appPath,
                    CN1WearableBridge.sequenceOf(value), uri.getHost(),
                    CN1WearableBridge.payloadOf(value)) && !started) {
                // Same ordering again: a stale replica is declined and does not deserve a launch.
                ensureAppRunning();
                started = true;
            }
        }
    }

    @Override
    public void onCapabilityChanged(com.google.android.gms.wearable.CapabilityInfo info) {
        // The companion was installed or removed while the device stayed connected. Nothing else
        // would notice: the capability cache would keep answering with the previous result.
        //
        // capabilityChanged() notifies listeners itself, and only when the set actually changed, so
        // there is deliberately no second notifyStateChanged() here -- it would deliver the same
        // state change twice, and would fire even when nothing changed.
        CN1WearableBridge.capabilityChanged(info);
    }

    @Override
    public void onPeerConnected(com.google.android.gms.wearable.Node peer) {
        // Correct the bridge's node cache before listeners run: one that responds by calling
        // isReachable() must not be told about a peer the cache has not heard of yet.
        CN1WearableBridge.peerChanged(peer, true);
    }

    @Override
    public void onPeerDisconnected(com.google.android.gms.wearable.Node peer) {
        CN1WearableBridge.peerChanged(peer, false);
    }
}
