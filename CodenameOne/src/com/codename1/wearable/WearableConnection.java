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
package com.codename1.wearable;

import com.codename1.ui.Display;
import com.codename1.wearable.spi.WearableBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The link between a phone app and its watch app. The same API on both ends, and the same API on
/// Apple Watch and Wear OS.
///
/// ```java
/// // On the phone: publish state the watch should show whenever it next wakes.
/// WearableConnection.putData(new WearableMessage("/steps").put("count", steps));
///
/// // On the watch: react to it, and ask for a fresh value on demand.
/// WearableConnection.addDataListener(new WearableDataListener() {
///     public void dataChanged(WearableMessage data) { label.setText("" + data.getInt("count", 0)); }
///     public void dataRemoved(String path) { label.setText("--"); }
/// });
/// ```
///
/// Register listeners from your app's `init()`. A payload that arrives before the first listener is
/// registered -- including the one that made the platform launch your app -- is queued and replayed,
/// but only to a listener that exists by the time the EDT gets to it.
///
/// Where the platform provides no wearable link at all, [#isSupported()] returns false and every
/// call here is an inert no-op, so this API needs no platform conditionals around it. Note what
/// that method does NOT tell you: an iPhone with no watch paired to it still reports true, because
/// the question is whether the API exists. Gate wearable UI on [#isPaired()] or [#isReachable()].
/// See the package documentation for how to choose between a message, replicated data and a file
/// transfer.
public final class WearableConnection {
    private static final List<WearableMessageListener> messageListeners =
            new ArrayList<WearableMessageListener>();
    private static final List<WearableDataListener> dataListeners =
            new ArrayList<WearableDataListener>();
    private static final List<WearableStateListener> stateListeners =
            new ArrayList<WearableStateListener>();

    /// Payloads that arrived before anyone was listening. The platform can start an app purely to
    /// hand it a message, so dropping these would lose exactly the payload that mattered most.
    ///
    /// Queued separately per listener type: an app that registers its data listener first would
    /// otherwise drain a queued *message* while messageListeners was still empty, losing it for
    /// good.
    /// How many deliveries may be parked while no listener exists.
    ///
    /// Bounded on purpose. A peer that keeps sending to an app version which never registers the
    /// matching listener -- a retired message path, a build that dropped the feature -- would
    /// otherwise grow these without limit, and each queued runnable captures its whole payload, so
    /// the cost tracks traffic rather than count.
    ///
    /// At the cap the oldest REPLACEABLE delivery is dropped: for a replicated value the newest is
    /// the one that matters, and for a live message a listener that has never appeared was not
    /// going to read the old ones either. A one-shot file transfer is not replaceable and is
    /// evicted only when nothing else is parked -- see [#evictOne].
    private static final int MAX_PENDING = 256;
    private static final List<Runnable> pendingMessages = new ArrayList<Runnable>();
    private static final List<Runnable> pendingData = new ArrayList<Runnable>();

    /// Outstanding requests, keyed by the token handed to the bridge. The request path is kept
    /// alongside the handler so the reply decodes onto a real path -- a payload has to have one.
    private static final Map<Integer, PendingReply> pendingReplies =
            new HashMap<Integer, PendingReply>();
    private static int nextReplyToken = 1;

    /// A request waiting for its answer.
    private static final class PendingReply {
        final WearableReplyHandler handler;
        final String path;

        PendingReply(WearableReplyHandler handler, String path) {
            this.handler = handler;
            this.path = path;
        }
    }

    private WearableConnection() {
    }

    private static WearableBridge bridge() {
        return Display.getInstance().getWearableBridge();
    }

    /// Brings the platform bridge into existence.
    ///
    /// An app that only listens never calls anything that would otherwise create it, and on Apple
    /// the native session is not activated until the bridge is first touched -- so a pure listener
    /// would sit waiting for traffic that the platform was never told to deliver.
    private static void activate() {
        WearableBridge b = bridge();
        if (b != null) {
            b.isSupported();
        }
    }

    // --- state --------------------------------------------------------------

    /// Returns true when this PLATFORM provides a wearable link, not when a counterpart exists.
    ///
    /// False on a desktop build and on any platform with no wearable API at all, and when false
    /// every other call here does nothing. But an iPhone with no watch paired to it still reports
    /// true: the question this answers is whether the API is present, and Apple's is. The same
    /// holds on Android whenever the app was built with the wearable glue.
    ///
    /// Ask [#isPaired()] whether a counterpart device is actually paired, and [#isReachable()]
    /// whether its app can receive something right now. Treating this method as either of those
    /// will offer wearable features on a phone that has no watch.
    ///
    /// #### Returns
    ///
    /// true if the platform provides the wearable link
    public static boolean isSupported() {
        WearableBridge b = bridge();
        return b != null && b.isSupported();
    }

    /// Returns true when a counterpart device is paired, whether or not it is switched on or in
    /// range. Distinct from [#isReachable()], which asks whether its app can receive something now.
    ///
    /// On Android there is one case this cannot see: a paired watch that has never run your watch
    /// app. The Data Layer exposes pairing only through the nodes it knows about, and a watch that
    /// never ran the app appears in no such list -- so a phone that is genuinely paired reports
    /// false until the watch app has run once. Treat false as "no counterpart known", not as proof
    /// that none exists, and prefer showing setup guidance over hiding it. Apple's API answers the
    /// pairing question directly and has no such gap.
    ///
    /// #### Returns
    ///
    /// true if a counterpart device is known to be paired
    public static boolean isPaired() {
        WearableBridge b = bridge();
        return b != null && b.isPaired();
    }

    /// Returns true when the peer app can receive a live message right now. This is the condition
    /// [#sendMessage(WearableMessage)] needs; [#putData(WearableMessage)] does not.
    ///
    /// #### Returns
    ///
    /// true if the peer app is reachable
    public static boolean isReachable() {
        WearableBridge b = bridge();
        return b != null && b.isReachable();
    }

    /// Returns true when the counterpart app is installed on the paired device. A watch that is
    /// paired but has no watch app installed is worth prompting the user about, and is the usual
    /// reason a correct-looking `sendMessage` never arrives.
    ///
    /// #### Returns
    ///
    /// true if the peer app is installed
    public static boolean isCompanionAppInstalled() {
        WearableBridge b = bridge();
        return b != null && b.isCompanionAppInstalled();
    }

    /// Returns the counterpart devices currently connected. Apple pairs one watch at a time, so
    /// expect at most one; Wear OS allows several.
    ///
    /// #### Returns
    ///
    /// the connected nodes, never null
    public static List<WearableNode> getConnectedNodes() {
        List<WearableNode> out = new ArrayList<WearableNode>();
        WearableBridge b = bridge();
        if (b == null) {
            return out;
        }
        String[] raw = b.getConnectedNodes();
        if (raw == null) {
            return out;
        }
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            // id \t displayName \t nearby -- see WearableBridge#getConnectedNodes.
            String[] parts = com.codename1.util.StringUtil.tokenize(entry, '\t')
                    .toArray(new String[0]);
            if (parts.length == 0) {
                continue;
            }
            String id = parts[0];
            String name = parts.length > 1 ? parts[1] : id;
            boolean nearby = parts.length > 2 && "1".equals(parts[2]);
            out.add(new WearableNode(id, name, nearby));
        }
        return out;
    }

    // --- sending ------------------------------------------------------------

    /// Sends a live message to the peer app, with no reply expected.
    ///
    /// The message is delivered only if the peer is reachable; if it is not, the message is dropped.
    /// Use [#putData(WearableMessage)] when the peer needs to see it eventually rather than now.
    ///
    /// #### Parameters
    ///
    /// - `message`: the payload to send
    public static void sendMessage(WearableMessage message) {
        sendMessage(message, null);
    }

    /// Sends a live message to the peer app and waits for its answer.
    ///
    /// Exactly one method on the handler is called, on the EDT. A reply is not guaranteed: the peer
    /// may be asleep, out of range, or running a version of your app that does not know this path.
    ///
    /// #### Parameters
    ///
    /// - `message`: the payload to send
    /// - `reply`: notified with the answer, or null when no answer is wanted
    public static void sendMessage(WearableMessage message, WearableReplyHandler reply) {
        if (message == null) {
            return;
        }
        WearableBridge b = bridge();
        if (b == null || !b.isSupported()) {
            if (reply != null) {
                failReply(reply, "No wearable link on this device");
            }
            return;
        }
        int token = 0;
        if (reply != null) {
            synchronized (pendingReplies) {
                token = nextReplyToken++;
                pendingReplies.put(Integer.valueOf(token),
                        new PendingReply(reply, message.getPath()));
            }
        }
        b.sendMessage(message.getPath(), message.toByteArray(), token);
    }

    /// Publishes the current value at a path, replacing whatever was there.
    ///
    /// This is the transport to reach for by default. The value survives both apps being killed and
    /// reaches the peer whenever it next runs, so the peer always converges on the latest value.
    /// Because each path holds one value, this is state replication and not a message queue -- two
    /// rapid updates to the same path may be collapsed into one delivery.
    ///
    /// #### Parameters
    ///
    /// - `data`: the payload to publish, addressed to the path to publish under
    public static void putData(WearableMessage data) {
        if (data == null) {
            return;
        }
        WearableBridge b = bridge();
        if (b != null && b.isSupported()) {
            b.putData(data.getPath(), data.toByteArray());
        }
    }

    /// Reads the replicated value at a path, as published by either side.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to read
    ///
    /// #### Returns
    ///
    /// the value, or null when nothing is published at that path
    public static WearableMessage getData(String path) {
        WearableBridge b = bridge();
        if (b == null || !b.isSupported() || path == null) {
            return null;
        }
        byte[] raw = b.getData(path);
        return raw == null ? null : WearableMessage.fromByteArray(path, raw);
    }

    /// Removes the replicated value at a path. The peer is notified through
    /// [WearableDataListener#dataRemoved(String)].
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to clear
    public static void removeData(String path) {
        WearableBridge b = bridge();
        if (b != null && b.isSupported() && path != null) {
            b.removeData(path);
        }
    }

    /// Returns every path that currently holds a replicated value.
    ///
    /// #### Returns
    ///
    /// the published paths, never null
    public static List<String> getDataPaths() {
        List<String> out = new ArrayList<String>();
        WearableBridge b = bridge();
        if (b == null || !b.isSupported()) {
            return out;
        }
        String[] paths = b.getDataPaths();
        if (paths != null) {
            for (String p : paths) {
                if (p != null) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    /// Sends a file to the peer in the background.
    ///
    /// Delivery is not immediate and may happen after this app has exited -- that is the point. Use
    /// it for anything too big for a message: a captured image, a synced document, a map tile.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the peer matches on
    /// - `name`: the file name to present to the peer
    /// - `contents`: the file bytes
    public static void transferFile(String path, String name, byte[] contents) {
        // Empty is rejected here, alongside null, because WearableMessage rejects it too and the
        // receiving side builds one. Letting it through made the failure platform-dependent and
        // put it in the worst possible place: JavaSE threw immediately in the caller's own frame,
        // while Android and iOS sent it happily and threw IllegalArgumentException later on the
        // EDT, where it aborts the delivery pass and takes unrelated deliveries with it.
        if (path == null || path.length() == 0 || contents == null) {
            return;
        }
        WearableBridge b = bridge();
        if (b != null && b.isSupported()) {
            b.transferFile(path, name, contents);
        }
    }

    // --- listeners ----------------------------------------------------------

    /// Registers a listener for live messages from the peer. Register from your app's `init()`: a
    /// message queued while the app was starting is replayed only to listeners that exist by the
    /// time the EDT drains the queue.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addMessageListener(WearableMessageListener l) {
        if (l != null && !messageListeners.contains(l)) {
            synchronized (pendingMessages) {
                messageListeners.add(l);
            }
            activate();
            drainPending(pendingMessages);
        }
    }

    /// Removes a previously registered message listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeMessageListener(WearableMessageListener l) {
        messageListeners.remove(l);
    }

    /// Registers a listener for replicated data changes. Register from your app's `init()` for the
    /// same reason as [#addMessageListener(WearableMessageListener)].
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addDataListener(WearableDataListener l) {
        if (l != null && !dataListeners.contains(l)) {
            synchronized (pendingData) {
                dataListeners.add(l);
            }
            activate();
            drainPending(pendingData);
        }
    }

    /// Removes a previously registered data listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeDataListener(WearableDataListener l) {
        dataListeners.remove(l);
    }

    /// Registers a listener for changes to the link itself -- reachability, pairing, whether the
    /// peer app is installed.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addStateListener(WearableStateListener l) {
        if (l != null && !stateListeners.contains(l)) {
            stateListeners.add(l);
            activate();
        }
    }

    /// Removes a previously registered state listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeStateListener(WearableStateListener l) {
        stateListeners.remove(l);
    }

    // --- platform port entry points -----------------------------------------

    /// Framework/port entry point: hands a message received from the peer to the app. Called by the
    /// platform port on whatever thread the native transport uses; delivery is marshalled to the
    /// EDT, and queued if no listener has been registered yet.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the message arrived on
    /// - `payload`: the encoded payload
    /// - `replyToken`: a positive token when the peer is waiting for an answer, otherwise 0
    public static void deliverMessage(final String path, final byte[] payload, final int replyToken) {
        deliver(new Runnable() {
            @Override
            public void run() {
                WearableMessage m = WearableMessage.fromByteArray(path, payload);
                WearableMessage reply = null;
                WearableMessageListener[] copy =
                        messageListeners.toArray(new WearableMessageListener[messageListeners.size()]);
                for (WearableMessageListener l : copy) {
                    WearableMessage r = l.messageReceived(m, replyToken != 0);
                    if (r != null && reply == null) {
                        reply = r;
                    }
                }
                // Nothing answers for an app that has no listener left. The snapshot can empty
                // between the dispatch and this runnable -- an app shutting down, or one that
                // deregisters on pause -- and replying anyway handed the sender an empty SUCCESS,
                // so replyReceived fired for a request no application code ever saw. Staying
                // silent lets the sender's own timeout report the failure it actually had.
                if (replyToken != 0 && copy.length > 0) {
                    WearableBridge b = bridge();
                    if (b != null) {
                        b.sendReply(replyToken,
                                reply == null ? new byte[0] : reply.toByteArray());
                    }
                }
            }
        }, messageListeners, pendingMessages);
    }

    /// Framework/port entry point: hands the peer's answer to the waiting reply handler. Called by
    /// the platform port; a token with no waiting handler is ignored.
    ///
    /// #### Parameters
    ///
    /// - `replyToken`: the token returned with the original request
    /// - `payload`: the encoded reply payload, or null when the request failed
    /// - `error`: a description of the failure, or null on success
    public static void deliverReply(int replyToken, final byte[] payload, final String error) {
        final PendingReply pending;
        synchronized (pendingReplies) {
            pending = pendingReplies.remove(Integer.valueOf(replyToken));
        }
        if (pending == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    pending.handler.replyFailed(error);
                } else {
                    // On the request's own path: a message always has one, and answering on the
                    // path you asked about is what a handler wants to see.
                    pending.handler.replyReceived(
                            WearableMessage.fromByteArray(pending.path, payload));
                }
            }
        });
    }

    /// Framework/port entry point: reports that the peer published or updated a replicated value.
    /// Called by the platform port; queued across a cold start like a message.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value changed
    /// - `payload`: the encoded new value
    public static void deliverDataChanged(final String path, final byte[] payload) {
        deliver(new Runnable() {
            @Override
            public void run() {
                WearableMessage m = WearableMessage.fromByteArray(path, payload);
                WearableDataListener[] copy =
                        dataListeners.toArray(new WearableDataListener[dataListeners.size()]);
                for (WearableDataListener l : copy) {
                    l.dataChanged(m);
                }
            }
        }, dataListeners, pendingData);
    }

    /// Framework/port entry point: reports that the peer removed a replicated value. Called by the
    /// platform port.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value is gone
    public static void deliverDataRemoved(final String path) {
        deliver(new Runnable() {
            @Override
            public void run() {
                WearableDataListener[] copy =
                        dataListeners.toArray(new WearableDataListener[dataListeners.size()]);
                for (WearableDataListener l : copy) {
                    l.dataRemoved(path);
                }
            }
        }, dataListeners, pendingData);
    }

    /// Framework/port entry point: reports that reachability, pairing or peer-app installation
    /// changed. Called by the platform port. Unlike payload delivery this is not queued -- state is
    /// re-queried by the listener, so a stale notification is worthless.
    public static void notifyStateChanged() {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                WearableStateListener[] copy =
                        stateListeners.toArray(new WearableStateListener[stateListeners.size()]);
                for (WearableStateListener l : copy) {
                    l.connectionStateChanged();
                }
            }
        });
    }

    /// Runs a delivery on the EDT, or parks it until a listener exists.
    ///
    /// The platform starts an app to hand it a payload, so the payload routinely arrives before the
    /// app has finished wiring itself up. Parking rather than dropping is what makes it safe to
    /// register listeners in `init()`.
    private static boolean deliver(Runnable delivery, List<?> listeners, List<Runnable> queue) {
        return deliver(delivery, listeners, queue, null, false);
    }

    /// Runs a delivery on the EDT, or parks it until a listener exists.
    ///
    /// `oneShot` marks a delivery whose payload has no other copy in this process -- a file
    /// transfer. It changes only what the cap evicts.
    private static boolean deliver(Runnable delivery, List<?> listeners, List<Runnable> queue,
            Runnable onDropped, boolean oneShot) {
        List<Runnable> release = null;
        boolean parked;
        // The listener check and the enqueue share the queue's monitor with drainPending, so a
        // delivery can never be parked after the drain that would have replayed it.
        synchronized (queue) {
            parked = listeners.isEmpty();
            if (parked) {
                while (queue.size() >= MAX_PENDING) {
                    Runnable evicted = evictOne(queue);
                    if (evicted != null) {
                        if (release == null) {
                            release = new ArrayList<Runnable>();
                        }
                        release.add(evicted);
                    }
                }
                queue.add(oneShot ? new OneShot(delivery, onDropped) : delivery);
            }
        }
        // Run outside the monitor: this calls back into the port, and holding the queue's lock
        // across foreign code invites a deadlock with whatever the port synchronises on.
        if (release != null) {
            for (int i = 0; i < release.size(); i++) {
                release.get(i).run();
            }
        }
        if (parked) {
            return false;
        }
        Display.getInstance().callSerially(delivery);
        return true;
    }

    /// Makes room for one delivery, taking a replaceable one first, and returns anything whose
    /// port-side claim has to be released. Run OUTSIDE the queue's monitor.
    ///
    /// The cap used to take the oldest entry outright, and replicated updates share this queue with
    /// file transfers. A replicated update is safely replaceable -- a later publication of the same
    /// path supersedes it, and the value is still readable with `getData`. A transfer is not: it is
    /// one-shot, the payload exists nowhere else in this process, and dropping it is the whole
    /// delivery.
    ///
    /// So transfers are evicted only when every parked delivery is one, and even then the eviction
    /// merely drops the runnable: the confirmation callback is NOT invoked, so the port's durable
    /// copy -- the iOS inbox entry, the JavaSE file, the Android transfer claim -- is left
    /// unretired and the payload is redelivered on the next activation instead of being lost.
    private static Runnable evictOne(List<Runnable> queue) {
        for (int i = 0; i < queue.size(); i++) {
            if (!(queue.get(i) instanceof OneShot)) {
                queue.remove(i);
                return null;
            }
        }
        OneShot dropped = (OneShot) queue.remove(0);
        return dropped.onDropped;
    }

    /// Marks a parked delivery as the only copy of its payload. See [#evictOne].
    private static final class OneShot implements Runnable {
        private final Runnable delivery;
        /// Releases the port's in-process claim on this payload when the delivery is evicted, or
        /// null when the port has nothing to release.
        final Runnable onDropped;

        OneShot(Runnable delivery, Runnable onDropped) {
            this.delivery = delivery;
            this.onDropped = onDropped;
        }

        @Override
        public void run() {
            delivery.run();
        }
    }

    /// Framework/port entry point: as [#deliverDataChanged], reporting whether the delivery reached
    /// a registered listener rather than being parked for a cold start.
    ///
    /// Ports use this where the answer changes what they record. A file transfer is the case: its
    /// one-shot claim must not be made durable while the payload exists only in this process's
    /// pending queue, because a process death then loses the payload AND suppresses the redelivery
    /// that would have replaced it.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value changed
    /// - `payload`: the encoded new value
    ///
    /// #### Returns
    ///
    /// `true` when a listener was registered and the delivery was dispatched; `false` when it was
    /// queued for a listener that does not exist yet.
    public static boolean deliverDataChangedTracked(final String path, final byte[] payload) {
        return deliverDataChangedTracked(path, payload, null);
    }

    /// As above, invoking `onDelivered` once application listeners have actually RUN.
    ///
    /// The distinction matters for anything that records a delivery durably. `deliver` returning
    /// true means the runnable was handed to the EDT, not that it executed -- a process death in
    /// between loses the payload while the record says it arrived. A one-shot file transfer
    /// suppresses its own redelivery on the strength of that record, so the difference between
    /// "dispatched" and "delivered" is the difference between a duplicate and a permanent loss.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value changed
    /// - `payload`: the encoded new value
    /// - `onDelivered`: run on the EDT after the listeners, or null
    ///
    /// #### Returns
    ///
    /// `true` when a listener was registered and the delivery was dispatched.
    public static boolean deliverDataChangedTracked(final String path, final byte[] payload,
            final Runnable onDelivered) {
        return deliverDataChangedTracked(path, payload, onDelivered, null);
    }

    /// As above, additionally releasing the port's in-process claim if the parked delivery is
    /// evicted to make room under the queue cap.
    ///
    /// Dropping the runnable alone is not enough for a one-shot transfer. The ports suppress a
    /// second callback for a payload they have already handed over -- Android holds an in-memory
    /// transfer claim, JavaSE has recorded the file in its seen set -- so nothing would redeliver
    /// it while this process stays alive, and Android's sender-side retention can expire in the
    /// meantime. `onRelinquished` undoes exactly that bookkeeping, so the payload is offered again
    /// on the next scan instead of waiting for a restart.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value changed
    /// - `payload`: the encoded new value
    /// - `onDelivered`: run on the EDT after the listeners, or null
    /// - `onRelinquished`: run when the parked delivery is evicted undelivered, or null
    ///
    /// #### Returns
    ///
    /// `true` when a listener was registered and the delivery was dispatched.
    public static boolean deliverDataChangedTracked(final String path, final byte[] payload,
            final Runnable onDelivered, final Runnable onRelinquished) {
        return deliver(new Runnable() {
            @Override
            public void run() {
                WearableMessage m = WearableMessage.fromByteArray(path, payload);
                WearableDataListener[] copy =
                        dataListeners.toArray(new WearableDataListener[dataListeners.size()]);
                if (copy.length == 0) {
                    // Every listener went away between the dispatch and this runnable -- an app
                    // shutting down, or one that deregisters on pause. Confirming here would be a
                    // lie with consequences: for a one-shot transfer the confirmation DELETES the
                    // port's durable copy, so the payload would be destroyed having reached nobody.
                    // Hand it back instead, exactly as an eviction does.
                    if (onRelinquished != null) {
                        onRelinquished.run();
                    }
                    return;
                }
                for (WearableDataListener l : copy) {
                    l.dataChanged(m);
                }
                if (onDelivered != null) {
                    onDelivered.run();
                }
            }
        }, dataListeners, pendingData, onRelinquished, onDelivered != null);
    }

    /// Actions a port asked to run once deliveries can actually reach a listener, keyed so repeated
    /// requests for the same operation collapse into one.
    private static final java.util.LinkedHashMap<String, Runnable> replayRequests =
            new java.util.LinkedHashMap<String, Runnable>();

    /// Framework/port entry point: asks for `replay` to run once a listener exists.
    ///
    /// A port whose payload was evicted from the pending queue cannot simply re-offer it: nothing
    /// has changed yet, so the delivery would be parked, immediately evict another one-shot to make
    /// room, and that one would re-offer in turn. Deferring to the moment the queue drains breaks
    /// that cycle -- by then a listener exists and deliveries dispatch instead of parking.
    ///
    /// Requests are keyed, and a repeat replaces the pending one. A port whose replay re-offers
    /// EVERYTHING it is holding -- as a rescan does -- should pass a constant key: one such action
    /// covers any number of evictions, and queueing one per evicted payload would both grow this
    /// map past the delivery cap and rescan the whole backlog once per eviction.
    ///
    /// Runs immediately when a data listener is already registered.
    ///
    /// #### Parameters
    ///
    /// - `key`: identifies the operation; a later request with the same key supersedes this one
    /// - `replay`: the port's re-offer action
    public static void requestReplayAfterDrain(String key, Runnable replay) {
        if (replay == null) {
            return;
        }
        synchronized (pendingData) {
            if (dataListeners.isEmpty()) {
                replayRequests.put(key == null ? replay.toString() : key, replay);
                return;
            }
        }
        replay.run();
    }

    /// Replays what was queued for one listener type, once a listener of that type exists.
    private static void drainPending(List<Runnable> queue) {
        List<Runnable> drained;
        List<Runnable> replays = null;
        synchronized (queue) {
            if (queue == pendingData && !replayRequests.isEmpty()) {
                replays = new ArrayList<Runnable>(replayRequests.values());
                replayRequests.clear();
            }
            if (!queue.isEmpty()) {
                drained = new ArrayList<Runnable>(queue);
                queue.clear();
            } else {
                drained = null;
            }
        }
        if (drained != null) {
            for (Runnable r : drained) {
                Display.getInstance().callSerially(r);
            }
        }
        // After the drain, so a re-offered payload finds room and a registered listener rather than
        // landing straight back in a full queue.
        if (replays != null) {
            for (int i = 0; i < replays.size(); i++) {
                replays.get(i).run();
            }
        }
    }

    private static void failReply(final WearableReplyHandler reply, final String message) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                reply.replyFailed(message);
            }
        });
    }
}
