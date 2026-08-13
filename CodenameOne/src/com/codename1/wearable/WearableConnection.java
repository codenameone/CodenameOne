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
    /// Where this process's block of reply tokens ends.
    ///
    /// Tokens are handed out from a RESERVED BLOCK, and the next block is recorded before any of it
    /// is used. Each run therefore issues numbers no other run can, which is what the wire needs:
    /// the Android path carries nothing but the integer, so a sender killed with a request
    /// outstanding and restarted could otherwise have the stale reply complete a NEW request's
    /// handler with the wrong payload.
    ///
    /// A clock seed was the first attempt and is not enough on its own -- twenty requests and a
    /// restart ten milliseconds later reissues half of them. The reservation is exact instead of
    /// probabilistic, and it costs one small preference write per process.
    private static final int REPLY_TOKEN_BLOCK = 4096;

    private static final String REPLY_TOKEN_BASE_KEY = "cn1$wearableReplyTokenBase";

    private static int nextReplyToken;

    private static int replyTokenLimit;

    /// Reserves the next block, recording where the one after it starts before handing any out.
    ///
    /// Called with the pendingReplies monitor held. Recording FIRST is the whole point: a process
    /// that dies mid-run has still moved the stored base past everything it could have issued.
    ///
    /// Wraps back to 1 rather than through 0: 0 is the "no answer wanted" marker every dispatch
    /// site tests for, and a negative token would put a minus sign in a wire path.
    private static void reserveReplyTokenBlock() {
        int base;
        try {
            base = com.codename1.io.Preferences.get(REPLY_TOKEN_BASE_KEY, 0);
        } catch (RuntimeException storageUnavailable) {
            // No storage is not a reason to hand out a colliding token. The clock is a weaker
            // discriminator than the counter and a better one than starting from 1 again.
            base = (int) (System.currentTimeMillis() & 0x3FFFFFFFL);
        }
        if (base <= 0 || base > Integer.MAX_VALUE - REPLY_TOKEN_BLOCK * 2) {
            base = 0;
        }
        nextReplyToken = base + 1;
        replyTokenLimit = base + REPLY_TOKEN_BLOCK;
        try {
            com.codename1.io.Preferences.set(REPLY_TOKEN_BASE_KEY, replyTokenLimit);
        } catch (RuntimeException storageUnavailable) {
            // The block is still unique within this process; only the cross-restart guarantee is
            // lost, and the next run falls back to the clock above.
        }
    }

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
    /// Do not decide your UI from a single call at startup. Both platforms answer from state that
    /// is queried asynchronously, so the first calls in a cold process can report false for a
    /// device that is paired -- there is nothing to report until the first query lands. Register a
    /// [WearableStateListener] and react when the answer changes; that is what it is for.
    ///
    /// On Android there is one case this cannot see at all: a paired watch that has never run your
    /// watch app. The Data Layer exposes pairing only through the nodes it knows about, and a watch that
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
        // Serialized BEFORE the handler is registered. toByteArray refuses a message with more
        // entries than the wire format can express, and throwing after the registration left a
        // handler waiting on a request that was never sent -- no reply, and no port timeout to
        // release it either, so it was retained for the life of the process.
        byte[] wire = message.toByteArray();
        int token = 0;
        if (reply != null) {
            synchronized (pendingReplies) {
                if (nextReplyToken <= 0 || nextReplyToken > replyTokenLimit) {
                    // First request of the run, or this block is spent. Either way the next block
                    // is reserved and recorded before a number out of it is used.
                    reserveReplyTokenBlock();
                }
                token = nextReplyToken++;
                pendingReplies.put(Integer.valueOf(token),
                        new PendingReply(reply, message.getPath()));
            }
        }
        b.sendMessage(message.getPath(), wire, token);
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
    /// Null means the path holds nothing, and it is safe to act on that: where the platform has to
    /// ask its own replication layer -- Android does -- the query is authoritative rather than
    /// answered from a cache that a cold launch leaves empty. Called on the EDT that query runs
    /// through `invokeAndBlock`, so the UI keeps painting while it waits; as with any
    /// `invokeAndBlock`, do not call this from `paint()`.
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
    /// An empty list means there is nothing published, not "not enumerated yet" -- see
    /// [#getData(String)] for how that is arranged and what it costs on the EDT.
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
        if (l == null) {
            return;
        }
        boolean added;
        // One step, for the reason given on addDataListener.
        synchronized (pendingMessages) {
            added = !messageListeners.contains(l);
            if (added) {
                messageListeners.add(l);
            }
        }
        if (added) {
            activate();
            drainPending(pendingMessages, false);
            // A port waiting for SOMEONE to listen is waiting for this too. The Android spool
            // holds one-shot messages, and a message listener is the only thing that makes them
            // deliverable -- the data queue's own replay hand-off never sees them.
            runListenerWaiters();
        }
    }

    /// Removes a previously registered message listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeMessageListener(WearableMessageListener l) {
        // Same reasoning as removeDataListener.
        synchronized (pendingMessages) {
            messageListeners.remove(l);
        }
    }

    /// Registers a listener for replicated data changes. Register from your app's `init()` for the
    /// same reason as [#addMessageListener(WearableMessageListener)].
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addDataListener(WearableDataListener l) {
        if (l == null) {
            return;
        }
        boolean added;
        // The membership test and the add are ONE step. Split, two threads registering the same
        // instance both passed the test before either appended, and every later change was then
        // reported to that listener twice. Concurrent registration is not hypothetical here -- the
        // drain guard is a count precisely because two of them can run at once.
        synchronized (pendingData) {
            added = !dataListeners.contains(l);
            if (added) {
                dataListeners.add(l);
            }
        }
        if (added) {
            activate();
            drainPending(pendingData, true);
            runListenerWaiters();
        }
    }

    /// Removes a previously registered data listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeDataListener(WearableDataListener l) {
        // The SAME monitor the registration holds. Unsynchronized, a removal could land between a
        // concurrent registration's membership check and its append: the remove found nothing,
        // returned successfully, and the append then put the listener back -- so a listener the
        // app had removed went on receiving callbacks, and stayed reachable.
        synchronized (pendingData) {
            dataListeners.remove(l);
        }
    }

    /// Registers a listener for changes to the link itself -- reachability, pairing, whether the
    /// peer app is installed.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to add
    public static void addStateListener(WearableStateListener l) {
        if (l == null) {
            return;
        }
        boolean added;
        // The state list has no pending queue of its own, so it guards itself. Test and add in one
        // step, as the other two registrations do -- and it is the same monitor the snapshot takes,
        // so locking the snapshot alone would have guarded nothing.
        synchronized (stateListeners) {
            added = !stateListeners.contains(l);
            if (added) {
                stateListeners.add(l);
            }
        }
        if (added) {
            activate();
        }
    }

    /// Removes a previously registered state listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener to remove
    public static void removeStateListener(WearableStateListener l) {
        synchronized (stateListeners) {
            stateListeners.remove(l);
        }
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
        deliverMessage(path, payload, replyToken, null);
    }

    /// The same, with a callback for a port that has the message written down somewhere durable.
    ///
    /// `delivered` runs on the EDT once every registered listener has been offered the message --
    /// not when it is queued. A port whose spool survives process death needs exactly that
    /// distinction: releasing its record when the delivery was merely queued loses the message if
    /// the process dies before the EDT gets to it, which is the failure the spool exists for.
    ///
    /// Not called at all while the delivery is parked for want of a listener. That is the point: the
    /// record stays durable until something actually receives it.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the message arrived on
    /// - `payload`: the encoded payload
    /// - `replyToken`: a positive token when the peer is waiting for an answer, otherwise 0
    /// - `delivered`: run after the listeners have seen it, or null
    public static void deliverMessage(final String path, final byte[] payload,
            final int replyToken, final Runnable delivered) {
        deliverMessage(path, payload, replyToken, delivered, null);
    }

    /// The same, telling the port when the cap discarded the delivery instead of running it.
    ///
    /// A durable message needs BOTH callbacks or neither is safe. `delivered` says the listeners
    /// saw it, so the record can go; `dropped` says the queue cap evicted it, so the record must
    /// stay AND the port's in-process claim has to be released, or nothing will ever claim that
    /// record again in this process.
    ///
    /// Without `dropped` the message was parked as an ordinary runnable, which is what
    /// [#evictOne] discards first and silently. A spool drain that queued more than the cap while
    /// a listener existed -- and then lost that listener before the EDT ran the batch, an app
    /// deregistering on pause -- had every re-parked message reach that path: evicted with no
    /// callback of any kind, so the record stayed on disk marked in-flight, unclaimable until the
    /// process restarted, having burned an attempt from its budget for a delivery no application
    /// code ever saw.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path the message arrived on
    /// - `payload`: the encoded payload
    /// - `replyToken`: a positive token when the peer is waiting for an answer, otherwise 0
    /// - `delivered`: run after the listeners have seen it, or null
    /// - `dropped`: run when the cap evicted this delivery undelivered, or null
    public static void deliverMessage(final String path, final byte[] payload,
            final int replyToken, final Runnable delivered, final Runnable dropped) {
        // A message the port has written down is a one-shot by definition: this process holds the
        // only in-memory copy, and the durable record behind it is claimed. Marking it so is what
        // moves it to the BACK of the eviction order, behind everything replaceable.
        final boolean durable = delivered != null || dropped != null;
        deliver(new Runnable() {
            @Override
            public void run() {
                WearableMessage m = WearableMessage.fromByteArray(path, payload);
                WearableMessage reply = null;
                WearableMessageListener[] copy =
                        messageListenerSnapshot();
                if (copy.length == 0) {
                    // The snapshot emptied between the dispatch and this runnable -- an app
                    // shutting down, or one that deregisters on pause. Parked again rather than
                    // run through: nobody received the message, so the port must NOT be told it
                    // was delivered. It would release its durable record on that word and the
                    // one-shot would be gone, which is the failure the record exists to prevent.
                    // Re-parked with its one-shot marking and its dropped callback intact.
                    // Handing it back as a plain runnable stripped both, so the cap discarded it
                    // first and told nobody -- see the note on the dropped parameter.
                    deliver(this, messageListeners, pendingMessages, dropped, durable);
                    return;
                }
                // Isolated per listener, as every other dispatch here is. A live message cannot be
                // replayed, so a listener skipped because an EARLIER one threw simply never sees
                // it -- and for a request the sender waits out its whole timeout even when a later
                // listener would have answered.
                RuntimeException failure = null;
                for (WearableMessageListener l : copy) {
                    try {
                        WearableMessage r = l.messageReceived(m, replyToken != 0);
                        if (r != null && reply == null) {
                            reply = r;
                        }
                    } catch (RuntimeException listenerFailed) {
                        if (failure == null) {
                            failure = listenerFailed;
                        }
                    }
                }
                // An app with no listener left answers nothing, which the re-park above now
                // handles: replying anyway handed the sender an empty SUCCESS, so replyReceived
                // fired for a request no application code ever saw, and the sender's own timeout
                // is the honest report of what happened.
                if (replyToken != 0) {
                    WearableBridge b = bridge();
                    if (b != null) {
                        b.sendReply(replyToken,
                                reply == null ? new byte[0] : reply.toByteArray());
                    }
                }
                // Before the rethrow, and deliberately: every listener HAS been offered the
                // message by here, so as far as the port's durable record is concerned this one is
                // delivered. Holding it back because a listener threw would replay it on the next
                // launch into the same listener, for ever.
                runDeliveredCallback(delivered);
                // Reported, not swallowed -- but only once every listener has been offered the
                // message and the sender has its answer.
                if (failure != null) {
                    throw failure;
                }
            }
        }, messageListeners, pendingMessages, dropped, durable);
    }

    /// Framework/port entry point: hands the peer's answer to the waiting reply handler. Called by
    /// the platform port; a token with no waiting handler is ignored.
    ///
    /// #### Parameters
    ///
    /// - `replyToken`: the token returned with the original request
    /// - `payload`: the encoded reply payload, or null when the request failed
    /// - `error`: a description of the failure, or null on success
    /// Framework/port entry point: whether a request is still waiting on this token.
    ///
    /// A port uses this to decide whether an inbound reply is worth waking the application for. A
    /// reply whose requester is gone -- the process was killed while the peer was answering -- has
    /// nowhere to be delivered, and on Android starting the app for it brings the UI forward only
    /// for [#deliverReply] to drop the payload on the next line.
    ///
    /// #### Parameters
    ///
    /// - `replyToken`: the token the reply arrived under
    ///
    /// #### Returns
    ///
    /// `true` when a request registered under that token is still outstanding.
    /// Framework/port entry point: whether any application code is listening for messages yet.
    ///
    /// A port with a durable spool asks this before handing a one-shot message to the in-memory
    /// queue instead. `Display` being initialized is not the same question: between initialization
    /// and the app's `addMessageListener` call the queue holds payloads nothing has received, and a
    /// process killed in that window loses a message the Data Layer does not retain either. A port
    /// that can write the message down should keep doing so until someone is there to take it.
    ///
    /// #### Returns
    ///
    /// true when at least one message listener is registered
    public static boolean hasMessageListener() {
        synchronized (pendingMessages) {
            return !messageListeners.isEmpty();
        }
    }

    /// The same question for replicated-data listeners, which receive removals.
    ///
    /// #### Returns
    ///
    /// true when at least one data listener is registered
    public static boolean hasDataListener() {
        synchronized (pendingData) {
            return !dataListeners.isEmpty();
        }
    }

    public static boolean hasPendingReply(int replyToken) {
        synchronized (pendingReplies) {
            return pendingReplies.containsKey(Integer.valueOf(replyToken));
        }
    }

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

    /// A snapshot of the data listeners, taken under the monitor registration and removal use.
    ///
    /// size() and toArray() as two steps is not a snapshot. Codename One's ArrayList.toArray(T[])
    /// fills a caller-sized array, so a listener removed between the two leaves the last slot null
    /// -- and a null-terminated array is not empty, so the "no listeners" recovery does not run and
    /// the loop dereferences null. For a tracked transfer that throws before either callback, so
    /// the port keeps its claim and the payload cannot be recovered in this process.
    private static WearableDataListener[] dataListenerSnapshot() {
        synchronized (pendingData) {
            return dataListeners.toArray(new WearableDataListener[dataListeners.size()]);
        }
    }

    /// A snapshot of the state listeners. Same reasoning as dataListenerSnapshot -- state
    /// notifications carry no payload, so a null here is a crash rather than a lost file, but the
    /// two-step read is wrong for the same reason.
    private static WearableStateListener[] stateListenerSnapshot() {
        synchronized (stateListeners) {
            return stateListeners.toArray(new WearableStateListener[stateListeners.size()]);
        }
    }

    /// A snapshot of the message listeners. Same reasoning as dataListenerSnapshot.
    private static WearableMessageListener[] messageListenerSnapshot() {
        synchronized (pendingMessages) {
            return messageListeners.toArray(new WearableMessageListener[messageListeners.size()]);
        }
    }

    /// Framework/port entry point: reports that the peer published or updated a replicated value.
    /// Called by the platform port; queued across a cold start like a message.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value changed
    /// - `payload`: the encoded new value
    public static void deliverDataChanged(final String path, final byte[] payload) {
        deliverTagged(path, new Runnable() {
            @Override
            public void run() {
                WearableDataListener[] copy = dataListenerSnapshot();
                if (copy.length == 0) {
                    // The last listener went away between the park check and this EDT turn -- an
                    // app pausing while a native callback was in flight. Dispatching to nobody
                    // would DISCARD the change: the port has already recorded the path as
                    // delivered, so registering a listener again replays nothing, and a replicated
                    // value raises no second callback while it stays unchanged. Park it instead,
                    // which is what would have happened had the list been empty a moment earlier.
                    deliverTagged(path, this);
                    return;
                }
                WearableMessage m = WearableMessage.fromByteArray(path, payload);
                // Isolated per listener, as the tracked and removal paths already are. The port
                // recorded this path as delivered before the runnable ran, and an unchanged
                // replicated value raises no further callback, so a listener skipped because an
                // earlier one threw stays stale until the peer happens to publish again.
                RuntimeException failure = null;
                for (WearableDataListener l : copy) {
                    try {
                        l.dataChanged(m);
                    } catch (RuntimeException listenerFailed) {
                        if (failure == null) {
                            failure = listenerFailed;
                        }
                    }
                }
                // Reported, not swallowed -- but only once every listener has been offered it.
                if (failure != null) {
                    throw failure;
                }
            }
        });
    }

    /// Framework/port entry point: reports that the peer removed a replicated value. Called by the
    /// platform port.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value is gone
    public static void deliverDataRemoved(final String path) {
        deliverDataRemoved(path, null);
    }

    /// The same, with a callback for a port holding the removal in a durable spool.
    ///
    /// See [#deliverMessage(String,byte[],int,Runnable)]: `delivered` runs once the listeners have
    /// been offered the removal, and never while it is parked for want of one.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value was removed
    /// - `delivered`: run after the listeners have seen it, or null
    public static void deliverDataRemoved(final String path, final Runnable delivered) {
        deliverDataRemoved(path, delivered, null);
    }

    /// The same, telling the port when the cap discarded the removal instead of running it.
    ///
    /// See [#deliverMessage(String,byte[],int,Runnable,Runnable)]. The listener side of an evicted
    /// removal is already covered -- the drain re-announces it by path -- but a port holding the
    /// removal in a durable spool is not: that re-announcement carries no callback, so its record
    /// stays claimed and undeliverable for the life of the process.
    ///
    /// #### Parameters
    ///
    /// - `path`: the path whose value was removed
    /// - `delivered`: run after the listeners have seen it, or null
    /// - `dropped`: run when the cap evicted this delivery undelivered, or null
    public static void deliverDataRemoved(final String path, final Runnable delivered,
            final Runnable dropped) {
        deliverTagged(path, new Runnable() {
            @Override
            public void run() {
                WearableDataListener[] copy = dataListenerSnapshot();
                if (copy.length == 0) {
                    // Same gap as deliverDataChanged, and worse for a removal: the item is gone, so
                    // there is nothing left for any later enumeration to find. Park it.
                    deliverTagged(path, this, true, dropped);
                    return;
                }
                // Isolated per listener, as the tracked delivery is, and for a sharper reason: a
                // removal has nothing to fall back on. The item is gone, so no later enumeration
                // finds it and no re-read recovers it -- a listener skipped because an EARLIER one
                // threw stays permanently wrong about a path nothing will mention again.
                RuntimeException failure = null;
                for (WearableDataListener l : copy) {
                    try {
                        l.dataRemoved(path);
                    } catch (RuntimeException listenerFailed) {
                        if (failure == null) {
                            failure = listenerFailed;
                        }
                    }
                }
                // Every listener has been offered the removal, so the port may forget its durable
                // record. Before the rethrow, for the same reason as the message path.
                runDeliveredCallback(delivered);
                // Reported, not swallowed -- but only once every listener has been offered it.
                if (failure != null) {
                    throw failure;
                }
            }
        }, true, dropped);
    }

    /// Framework/port entry point: reports that reachability, pairing or peer-app installation
    /// changed. Called by the platform port. Unlike payload delivery this is not queued -- state is
    /// re-queried by the listener, so a stale notification is worthless.
    public static void notifyStateChanged() {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                WearableStateListener[] copy =
                        stateListenerSnapshot();
                // Isolated per listener, as the three payload paths are. The platform raises this
                // only on a TRANSITION -- reachability, pairing, installation -- so a listener
                // passed over because an earlier one threw shows stale connection state until the
                // next transition, which on a stable pairing may never come.
                RuntimeException failure = null;
                for (WearableStateListener l : copy) {
                    try {
                        l.connectionStateChanged();
                    } catch (RuntimeException listenerFailed) {
                        if (failure == null) {
                            failure = listenerFailed;
                        }
                    }
                }
                // Reported, not swallowed -- but only once every listener has been told.
                if (failure != null) {
                    throw failure;
                }
            }
        });
    }

    /// Parks a replicated delivery TAGGED with its path, so the cap can prefer an entry the
    /// incoming one actually supersedes.
    /// Runs a port's delivery-confirmation callback without letting it break the dispatch.
    ///
    /// A port releasing a durable record does I/O, and an exception from that must not be mistaken
    /// for a listener failure -- the listeners have already run by the time this is called.
    private static void runDeliveredCallback(Runnable delivered) {
        if (delivered == null) {
            return;
        }
        try {
            delivered.run();
        } catch (RuntimeException portFailed) {
            com.codename1.io.Log.e(portFailed);
        }
    }

    private static void deliverTagged(String path, Runnable delivery) {
        deliverTagged(path, delivery, false);
    }

    private static void deliverTagged(String path, Runnable delivery, boolean removal) {
        deliverTagged(path, delivery, removal, null);
    }

    private static void deliverTagged(String path, Runnable delivery, boolean removal,
            Runnable onDropped) {
        deliver(delivery, dataListeners, pendingData, onDropped, false, path, removal, true);
    }

    /// Runs a delivery on the EDT, or parks it until a listener exists.
    ///
    /// The platform starts an app to hand it a payload, so the payload routinely arrives before the
    /// app has finished wiring itself up. Parking rather than dropping is what makes it safe to
    /// register listeners in `init()`.
    ///
    /// `oneShot` marks a delivery whose payload has no other copy in this process -- a file
    /// transfer, or a message a port is holding in a durable spool. It changes only what the cap
    /// evicts, and it is what makes `onDropped` reachable.
    private static boolean deliver(Runnable delivery, List<?> listeners, List<Runnable> queue,
            Runnable onDropped, boolean oneShot) {
        return deliver(delivery, listeners, queue, onDropped, oneShot, null);
    }

    private static boolean deliver(Runnable delivery, List<?> listeners, List<Runnable> queue,
            Runnable onDropped, boolean oneShot, String path) {
        return deliver(delivery, listeners, queue, onDropped, oneShot, path, false);
    }

    private static boolean deliver(Runnable delivery, List<?> listeners, List<Runnable> queue,
            Runnable onDropped, boolean oneShot, String path, boolean removal) {
        return deliver(delivery, listeners, queue, onDropped, oneShot, path, removal, false);
    }

    /// @param dataQueue whether this is the replicated-data queue, whose drain has a tail of
    ///                  recovery work that must not be overtaken
    private static boolean deliver(Runnable delivery, List<?> listeners, List<Runnable> queue,
            Runnable onDropped, boolean oneShot, String path, boolean removal, boolean dataQueue) {
        List<Runnable> release = null;
        boolean parked;
        // The listener check and the enqueue share the queue's monitor with drainPending, so a
        // delivery can never be parked after the drain that would have replayed it.
        synchronized (queue) {
            // Parked when there is no listener OR when anything is already parked for this queue.
            //
            // The second half is what keeps ORDER. A listener is registered and the backlog drained
            // as two steps, and an update arriving between them saw a listener, dispatched straight
            // to the EDT, and landed AHEAD of older state that was still parked -- a republished
            // value followed by the stale removal it replaced, leaving the listener removed while
            // getData returned the value. Queueing behind an existing backlog makes that ordering
            // structural instead of a matter of timing, and it also routes the update through the
            // park path, which is where a superseded recovery record is cancelled.
            parked = listeners.isEmpty() || !queue.isEmpty() || (dataQueue && drainingData > 0);
            if (parked) {
                while (queue.size() >= MAX_PENDING) {
                    Runnable evicted = evictOne(queue, path);
                    if (evicted != null) {
                        if (release == null) {
                            release = new ArrayList<Runnable>();
                        }
                        release.add(evicted);
                    }
                }
                queue.add(oneShot ? new OneShot(delivery, onDropped)
                        : (path != null ? new Replicated(delivery, path, removal, onDropped)
                                : delivery));
                if (path != null) {
                    // Any recovery record for THIS path is now obsolete: the delivery just parked
                    // is a newer statement about it than the one that was discarded.
                    //
                    // Leaving them cost more than a redundant re-offer. droppedRemovals is
                    // re-announced by the drain itself, AFTER the parked deliveries are handed to
                    // the EDT -- so a path whose removal was evicted and which was then
                    // republished ended with the listener told "removed" on top of the newer
                    // value, while getData returned the value. Nothing later corrects that: the
                    // republication has already been consumed.
                    droppedRemovals.remove(path);
                    droppedPaths.remove(path);
                }
            }
        }
        // Run outside the monitor: this calls back into the port, and holding the queue's lock
        // across foreign code invites a deadlock with whatever the port synchronises on.
        if (release != null) {
            for (Runnable r : release) {
                r.run();
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
    private static Runnable evictOne(List<Runnable> queue, String incomingPath) {
        // SUPERSEDED first: an older delivery for the same path as the one arriving, which the
        // newcomer genuinely replaces. That is the case the "safely replaceable" reasoning above
        // actually describes, and it was applied to any replicated entry regardless of path -- so a
        // burst across many paths discarded callbacks nothing would replace, for paths the ports
        // have already marked as seen.
        if (incomingPath != null) {
            for (int i = 0; i < queue.size(); i++) {
                Runnable parked = queue.get(i);
                if (parked instanceof Replicated
                        && incomingPath.equals(((Replicated) parked).path)) {
                    queue.remove(i);
                    // Superseded is still not delivered. A port with a durable copy has to hear
                    // so, or its claim outlives the delivery it was taken for.
                    return ((Replicated) parked).onDropped;
                }
            }
        }
        // Nothing superseded. Then the oldest replicated entry -- but its path is REMEMBERED, and
        // handed to the port after the drain. The ports record a delivery as made before queueing
        // it, so their own replay would skip this path as already delivered; and a removal cannot
        // be reconstructed from getData at all. Without that hand-back the listener stays
        // permanently wrong about a path nothing will mention again.
        for (int i = 0; i < queue.size(); i++) {
            Runnable parked = queue.get(i);
            if (!(parked instanceof OneShot)) {
                queue.remove(i);
                if (parked instanceof Replicated) {
                    Replicated r = (Replicated) parked;
                    if (r.removal) {
                        droppedRemovals.add(r.path);
                    } else {
                        droppedPaths.add(r.path);
                    }
                    return r.onDropped;
                }
                return null;
            }
        }
        return ((OneShot) queue.remove(0)).onDropped;
    }

    /// A parked replicated delivery, tagged with its path so the cap can drop one the incoming
    /// delivery actually supersedes. See [#evictOne].
    private static final class Replicated implements Runnable {
        private final Runnable delivery;
        final String path;
        /// Whether this parked delivery was a removal rather than a value change.
        final boolean removal;
        /// Releases the port's in-process claim when the cap discards this one, or null.
        ///
        /// A removal is re-announced after an eviction, so the LISTENER recovers -- but a port
        /// holding the removal in a durable spool does not. Its confirmation runs only when
        /// listeners actually see the delivery, and the re-announcement goes through the plain
        /// entry point carrying no callback, so the record stayed on disk still marked in flight:
        /// unclaimable for the life of the process.
        final Runnable onDropped;

        Replicated(Runnable delivery, String path, boolean removal, Runnable onDropped) {
            this.delivery = delivery;
            this.path = path;
            this.removal = removal;
            this.onDropped = onDropped;
        }

        @Override
        public void run() {
            delivery.run();
        }
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
                WearableDataListener[] copy = dataListenerSnapshot();
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
                // One listener's failure must not decide the payload's fate. A throw used to skip
                // BOTH terminal callbacks, which for a transfer is the worst of the two outcomes:
                // the port has already marked the payload handed over -- JavaSE recorded the file
                // as seen, Android holds the in-memory claim -- so nothing offered it again for the
                // rest of the process, and the confirmation that would have retired the durable
                // copy never ran either.
                //
                // So each listener is isolated, and exactly one terminal callback always runs:
                // confirmed when at least one listener took the message, handed back when every
                // one of them threw -- the same reasoning as the no-listener case above, since a
                // payload nobody consumed has reached nobody.
                boolean reached = false;
                RuntimeException failure = null;
                for (WearableDataListener l : copy) {
                    try {
                        l.dataChanged(m);
                        reached = true;
                    } catch (RuntimeException listenerFailed) {
                        if (failure == null) {
                            failure = listenerFailed;
                        }
                    }
                }
                if (!reached) {
                    if (onRelinquished != null) {
                        onRelinquished.run();
                    }
                } else if (onDelivered != null) {
                    onDelivered.run();
                }
                // Reported, not swallowed -- but only once the payload is accounted for.
                if (failure != null) {
                    throw failure;
                }
            }
        }, dataListeners, pendingData, onRelinquished, onDelivered != null, path, false, true);
    }

    /// What a port does about a replicated delivery the cap had to discard, or null when it has
    /// registered nothing. See [#setDroppedDeliveryHandler].
    /// Guarded by the pendingData monitor rather than declared volatile.
    ///
    /// Every read already happens under that lock -- the drain holds it while deciding whether to
    /// hand paths back -- so volatile bought nothing beyond the write, and a lock that covers the
    /// decision is stronger than a field that only covers the load.
    private static DroppedDeliveryHandler droppedDeliveries;

    /// The registered handler, read under the monitor that guards it.
    ///
    /// Callers already hold that monitor in the drain, so this is reentrant there; it exists so no
    /// read of the field is left depending on the caller remembering to take the lock.
    private static DroppedDeliveryHandler droppedHandler() {
        synchronized (pendingData) {
            return droppedDeliveries;
        }
    }

    /// How a port recovers a parked delivery the cap discarded.
    ///
    /// Dropping the runnable is not enough on its own: the port recorded the delivery as made
    /// before queueing it, so its own replay will skip that path as already delivered. The port has
    /// to forget that record and offer the path again.
    public interface DroppedDeliveryHandler {
        /// Called once per discarded path, on the EDT, after the queue has drained and listeners
        /// exist -- so a re-offer can actually be delivered rather than parked and dropped again.
        ///
        /// A null path means the record itself overflowed and more was discarded than can be
        /// named: re-offer everything available rather than one path.
        ///
        /// Only value changes reach here. A discarded REMOVAL is re-announced by this class
        /// directly, because the path is the whole of it and no port could rediscover one -- the
        /// evidence of a removal is an item that is not there.
        void deliveryDropped(String path);
    }

    /// Framework entry point: forgets every listener and everything parked for them.
    ///
    /// The simulator's hot reload builds a NEW app instance while this class -- static, and loaded
    /// by a class loader the reload does not replace -- keeps the old one's listeners. Every later
    /// wearable event was then delivered to both instances, so side effects ran twice and callbacks
    /// reached UI objects belonging to a screen that no longer exists. The old instance also stayed
    /// strongly reachable through these lists, so it could never be collected.
    ///
    /// The parked deliveries and recovery records go with them. They describe work owed to the
    /// listeners being dropped; handing them to the replacement would deliver, as brand new, state
    /// the previous instance had already been told about.
    ///
    /// Only the simulator calls this. On a device the process dies instead, which is why nothing
    /// needed it before.
    public static void resetForReload() {
        List<Runnable> release = new ArrayList<Runnable>();
        synchronized (pendingData) {
            // A reload discards every PARKED delivery, and a parked delivery is by definition one
            // no listener has seen. Dropping the runnables alone stranded them: the ports record a
            // delivery as made before it is queued, so their own replay skips those paths, and the
            // replacement app instance was never told about state the previous one had never been
            // told about either.
            //
            // So a reload retires the queue exactly as an eviction does. A transfer's claim is
            // released -- outside this monitor, below -- so the payload is offered again on the
            // next scan; a replicated update is remembered as a hand-back request, which the next
            // listener's drain turns into a re-offer through the port's retained handler.
            List<String> unseenPaths = new ArrayList<String>();
            List<String> unseenRemovals = new ArrayList<String>();
            for (Runnable parked : pendingData) {
                if (parked instanceof OneShot) {
                    Runnable onDropped = ((OneShot) parked).onDropped;
                    if (onDropped != null) {
                        release.add(onDropped);
                    }
                } else if (parked instanceof Replicated) {
                    Replicated rep = (Replicated) parked;
                    if (rep.removal) {
                        unseenRemovals.add(rep.path);
                    } else {
                        unseenPaths.add(rep.path);
                    }
                }
            }
            dataListeners.clear();
            pendingData.clear();
            droppedPaths.clear();
            droppedRemovals.clear();
            replayRequests.clear();
            rescanRequested = false;
            drainingData = 0;
            // Re-recorded after the clears, deliberately, and after rescanRequested is reset: what
            // the clears drop is the hand-back owed to the listeners being replaced, what goes back
            // in is work that never reached anybody -- and if enough of it overflows the bound, the
            // set raises the rescan flag, which resetting it afterwards would have erased.
            droppedPaths.addAll(unseenPaths);
            droppedRemovals.addAll(unseenRemovals);
            // droppedDeliveries is deliberately KEPT. It belongs to the port, not to the app
            // instance being replaced: the bridge registers it once from its constructor, and the
            // simulator caches that bridge across reloads -- so clearing it here removed the
            // recovery path for good. After a reload with more than MAX_PENDING updates and no
            // listener, evicted paths would stay marked delivered by the port and could never be
            // re-offered.
        }
        synchronized (pendingMessages) {
            messageListeners.clear();
            // The QUEUE survives the reload, unlike the listeners.
            //
            // A parked message is one nothing has received yet, and there is nothing to receive it
            // from a second time: the port consumed the socket frame to park it, and no later
            // enumeration reconstructs a live message the way a replicated value is re-read above.
            // Clearing it therefore lost the payload outright, and a reply-bearing request left the
            // peer waiting out its whole timeout for an answer no code would ever be asked for.
            //
            // Safe to keep, because a parked delivery captures the payload and NOT the listeners:
            // it resolves them through messageListenerSnapshot() when it finally runs, which is the
            // reloaded app's set. That is the same reason the replicated paths are handed back
            // rather than dropped.
            //
            // Nothing else needs resetting here: the pending list is its own drain state.
        }
        synchronized (stateListeners) {
            stateListeners.clear();
        }
        synchronized (pendingReplies) {
            pendingReplies.clear();
        }
        // Outside every monitor: these call back into the port, and holding a queue lock across
        // foreign code is what the drain path avoids for the same reason.
        for (Runnable r : release) {
            r.run();
        }
    }

    /// Framework/port entry point: registers what to do about a discarded delivery.
    ///
    /// #### Parameters
    ///
    /// - `handler`: the port's recovery action, or null to remove it
    public static void setDroppedDeliveryHandler(DroppedDeliveryHandler handler) {
        synchronized (pendingData) {
            droppedDeliveries = handler;
        }
    }

    /// Paths whose parked delivery was discarded and not superseded, awaiting the drain.
    ///
    /// A SET, and bounded like every other cache here. Recovery is per path, so a path repeated
    /// adds nothing, and an app that never registers a listener while the peer churns through
    /// paths would otherwise grow this list without limit -- defeating the cap it exists to serve.
    /// Past the bound the oldest entry goes: the port's own startup replay remains the backstop for
    /// anything that falls off, which is the same guarantee as before this list existed.
    private static final java.util.Set<String> droppedPaths =
            java.util.Collections.newSetFromMap(new java.util.LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    if (size() > MAX_PENDING) {
                        // Overflowing loses a specific path, so instead of forgetting quietly, ask
                        // the port to re-offer everything it can. One flag, however many paths
                        // overflow: the whole point of the bound is that per-path bookkeeping has
                        // stopped being affordable.
                        rescanRequested = true;
                        return true;
                    }
                    return false;
                }
            });

    /// Paths whose discarded delivery was a REMOVAL, kept apart from the changes.
    ///
    /// Two reasons. A removal is re-announced from here directly rather than handed to the port:
    /// its entire content is the path, which this class already has, and no port can rediscover it
    /// -- the evidence of a removal is the absence of an item, so there is nothing to enumerate,
    /// nothing in a received context, and no file on disk. Asking a port to "re-offer" one is
    /// asking it to invent something.
    ///
    /// And keeping them in their own set means a burst of ordinary changes cannot evict them.
    private static final java.util.Set<String> droppedRemovals =
            java.util.Collections.newSetFromMap(new java.util.LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    if (size() <= MAX_REMEMBERED_REMOVALS) {
                        return false;
                    }
                    // The one loss in this class that nothing downstream can repair. A discarded
                    // CHANGE can be re-offered by the port, and overflowing that record asks for a
                    // rescan; a removal has no such fallback, because the item is gone and there is
                    // nothing anywhere to rediscover. So this is said out loud rather than dropped
                    // quietly -- an app whose listener arrives after this many distinct removals
                    // has one it will never hear about, and the log is the only evidence.
                    com.codename1.io.Log.p("com.codename1.wearable: no listener has registered and "
                            + MAX_REMEMBERED_REMOVALS + " removals are already waiting; the "
                            + "removal of " + eldest.getKey() + " can no longer be delivered. "
                            + "Register a WearableDataListener from init().");
                    return true;
                }
            });

    /// How many discarded removals are remembered. Larger than the delivery cap on purpose: a
    /// removal is a path and nothing else, so remembering one is cheap, and it is the only kind
    /// whose loss cannot be repaired by re-offering something that still exists.
    private static final int MAX_REMEMBERED_REMOVALS = 4096;

    /// Set when the dropped-path set overflowed, so the port is asked for a full rescan instead of
    /// a list of paths it can no longer be given. Read and cleared with [#droppedPaths]'s monitor.
    /// Claims a pending dropped-removal announcement, or reports that it has been superseded.
    ///
    /// False when a publication for the path parked while the drain was announcing, which removed
    /// the record: that newer value is the truth about the path, and the removal must not be queued
    /// behind it.
    private static boolean takeDroppedRemoval(String path) {
        synchronized (pendingData) {
            return droppedRemovals.remove(path);
        }
    }

    /// Takes the overflow flag and clears it.
    ///
    /// Synchronized on {@link #pendingData} by NAME even though the caller already holds that
    /// monitor -- this runs only while draining pendingData itself, so the acquisition is reentrant
    /// and nothing changes at runtime. The caller locks a queue it received as a parameter, which
    /// merely happens to alias the static field, and a guard that depends on an alias is one a
    /// reader cannot check and a static analyzer must assume is broken.
    private static boolean takeRescanRequest() {
        synchronized (pendingData) {
            boolean requested = rescanRequested;
            rescanRequested = false;
            return requested;
        }
    }

    /// True while a data drain is between taking its recovery records and announcing them.
    ///
    /// Those records are copied out and cleared under the monitor, then announced with it released
    /// -- so a publication arriving in that gap saw an empty queue, dispatched straight to the EDT,
    /// and could not cancel a record already copied into the drain's own list. The stale removal
    /// was then announced after the newer value, which is the failure the cancellation exists to
    /// prevent, reached by a different route. While this is set, deliveries park and are drained in
    /// order behind the recovery work.
    ///
    /// Guarded by the pendingData monitor.
    ///
    /// A COUNT, not a flag. addDataListener does not serialise, so two threads registering
    /// listeners can drain at once -- and with a boolean the one that finished first cleared it
    /// while the other was still announcing, reopening the window for whatever arrived next.
    private static int drainingData;

    /// Sets the draining flag under the monitor that guards it, named rather than aliased.
    ///
    /// The callers hold that monitor already -- both run while draining pendingData itself, so the
    /// acquisition is reentrant -- but they reach it through a parameter, and a guard that holds
    /// only through an alias is one neither a reader nor an analyzer can check. Same reasoning as
    /// takeRescanRequest.
    private static void setDrainingData(boolean draining) {
        synchronized (pendingData) {
            drainingData += draining ? 1 : -1;
            if (drainingData < 0) {
                // Cannot happen from the paired calls in drainPendingOnce, but a count that goes
                // negative would silently disable the guard for every later drain, so it is pinned
                // rather than trusted.
                drainingData = 0;
            }
        }
    }

    private static boolean rescanRequested;

    /// Actions a port asked to run the next time ANY listener registers, keyed so repeated
    /// requests for the same operation collapse into one.
    private static final java.util.LinkedHashMap<String, Runnable> listenerWaiters =
            new java.util.LinkedHashMap<String, Runnable>();

    /// Framework/port entry point: runs `action` the next time a listener of any kind registers.
    ///
    /// Distinct from [#requestReplayAfterDrain(String,Runnable)], which asks "run this as soon as
    /// a delivery can reach a listener" and therefore runs immediately when one already can. A
    /// port holding a DURABLE record cannot use that: the Android bridge spools a one-shot message
    /// and a data removal to disk, and its startup drain leaves behind whatever kind of listener
    /// is not registered yet. Asked through the other method, a spooled MESSAGE record with a data
    /// listener already registered would run the drain immediately, find the message listener
    /// still absent, ask again, and run again -- unbounded recursion. And nothing else re-triggers
    /// that drain: only inbound traffic does, so after a cold launch with no new traffic the
    /// records sat on disk indefinitely.
    ///
    /// This one never runs the action inline. It fires on the registration itself, which is the
    /// event the port is actually waiting for, and a request re-armed from inside a waiter is
    /// simply held for the next registration.
    ///
    /// #### Parameters
    ///
    /// - `key`: identifies the operation; a later request with the same key supersedes this one
    /// - `action`: what to run once someone is listening
    public static void runWhenListenerRegisters(String key, Runnable action) {
        if (action == null) {
            return;
        }
        synchronized (listenerWaiters) {
            listenerWaiters.put(key == null ? action.toString() : key, action);
        }
    }

    /// Runs and clears the waiters. Called after a registration has drained its own queue, so a
    /// port re-offering what it holds finds the backlog already dispatched.
    private static void runListenerWaiters() {
        List<Runnable> waiters;
        synchronized (listenerWaiters) {
            if (listenerWaiters.isEmpty()) {
                return;
            }
            // Copied and cleared together, so a waiter that re-arms itself -- the spool drain does,
            // when a record still needs the other kind of listener -- is held for the NEXT
            // registration rather than being wiped by this pass.
            waiters = new ArrayList<Runnable>(listenerWaiters.values());
            listenerWaiters.clear();
        }
        for (Runnable waiter : waiters) {
            // Isolated, for the same reason every other port callback in this file is: one
            // failing must not cost the others their run.
            try {
                waiter.run();
            } catch (Throwable portFailed) {
                com.codename1.io.Log.p("Wearable: a listener-registration action failed");
                com.codename1.io.Log.e(portFailed);
            }
        }
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
    /// Runs immediately only when a listener exists AND nothing is parked or draining.
    ///
    /// A registered listener is not on its own enough for a delivery to reach one. While a drain
    /// is in flight everything parks -- that is what keeps a re-offer behind the batch already on
    /// its way -- so running the replay here put it straight back into the queue it was evicted
    /// from. With more than MAX_PENDING transfers tracked, filling the queue evicted another
    /// one-shot, which asked for a replay, which this method ran immediately because the listener
    /// was still registered: the same durable backlog rescanned and re-evicted itself one stack
    /// frame deeper each time, until the process overflowed or stalled.
    ///
    /// So the question is not "is there a listener" but "can a delivery reach one right now". A
    /// nonempty queue answers no for the same reason a drain does, and both are cases the keyed
    /// request already handles -- the next drain pass takes it, and drainPending will not finish
    /// while one is outstanding.
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
            if (dataListeners.isEmpty() || drainingData > 0 || !pendingData.isEmpty()) {
                replayRequests.put(key == null ? replay.toString() : key, replay);
                return;
            }
        }
        replay.run();
    }

    /// Replays what was queued for one listener type, once a listener of that type exists.
    /// @param dataQueue whether this is the replicated-data queue, which carries the recovery
    ///                  bookkeeping the message queue has none of
    private static void drainPending(List<Runnable> queue, boolean dataQueue) {
        // Until the queue is genuinely empty, not once.
        //
        // Deliveries now park behind an existing backlog, and the tail of a drain calls out to the
        // port -- recovery hand-backs and replays -- with the monitor released. Anything arriving
        // in that window parks, and without this loop it would sit there until the next listener
        // registration, which for a single-listener app never comes.
        for (;;) {
            drainPendingOnce(queue, dataQueue);
            synchronized (queue) {
                // An outstanding replay request counts as work, not just a nonempty queue.
                //
                // A replay deferred DURING this drain -- which is now what an eviction inside a
                // replay produces, rather than an immediate recursive re-offer -- lands in
                // replayRequests after the pass that would have taken it. If that replay parked
                // nothing, the queue is empty and returning here would leave the request sitting
                // until some later listener registration, which for a single-listener app never
                // comes. This is the same stranding the loop itself exists to prevent, reached
                // through the other collection.
                if (queue.isEmpty() && (!dataQueue || replayRequests.isEmpty())) {
                    return;
                }
            }
        }
    }

    private static void drainPendingOnce(List<Runnable> queue, boolean dataQueue) {
        List<Runnable> replays = null;
        List<String> dropped = null;
        List<String> removals = null;
        synchronized (queue) {
            if (dataQueue) {
                // Held across the recovery announcements below, which run with the monitor
                // released. Anything arriving meanwhile parks rather than overtaking them.
                setDrainingData(true);
            }
            // Only taken when a port can actually act on them. Clearing the set with no handler
            // registered would discard the one record that a path needs re-offering -- and iOS,
            // which registers late in its bridge's construction, would lose whatever arrived first.
            if (dataQueue && !droppedRemovals.isEmpty()) {
                // Taken whether or not a port registered a handler: these are re-announced here.
                // COPIED, not taken. The entries stay in the set until each is actually
                // announced, so a publication parking in the meantime can still cancel one --
                // deliver() already removes an incoming path from this set, and clearing here put
                // the record out of its reach. The announcement re-checks membership.
                removals = new ArrayList<String>(droppedRemovals);
            }
            if (dataQueue && droppedHandler() != null && !droppedPaths.isEmpty()) {
                dropped = new ArrayList<String>(droppedPaths);
                droppedPaths.clear();
                if (takeRescanRequest()) {
                    // A null path is the rescan request: more was lost than can be named.
                    dropped.add(null);
                }
            }
            if (dataQueue && !replayRequests.isEmpty()) {
                replays = new ArrayList<Runnable>(replayRequests.values());
                replayRequests.clear();
            }
            if (!queue.isEmpty()) {
                List<Runnable> drained = new ArrayList<Runnable>(queue);
                queue.clear();
                // Handed to the EDT while the monitor is STILL held. Clearing the queue and then
                // dispatching outside it reopened the same gap from the other side: a delivery
                // arriving in between saw an empty queue, dispatched itself, and overtook the batch
                // that had already been taken out. callSerially only enqueues -- it runs no
                // listener code on this thread -- so holding the lock across it is safe, and the
                // same reasoning is already documented on deliverIfOutranks.
                for (Runnable r : drained) {
                    Display.getInstance().callSerially(r);
                }
            }
        }
        try {
            // Discarded REMOVALS are simply re-announced. The path is the whole of a removal, so this
            // is a complete recovery rather than a request for one -- and it is the only recovery
            // available, since the item is gone and no port can enumerate an absence.
            if (removals != null) {
                for (String removed : removals) {
                    // Isolated too: deliverDataRemoved only queues, but it runs the same
                    // park/evict path as any delivery, and an eviction hands a payload back to the
                    // port -- which is foreign code that can throw.
                    // Only if it is still pending. A publication for this path parking during the
                    // recovery supersedes the removal, and announcing anyway put a stale removal
                    // BEHIND the newer value in the same queue -- the listener ended up removed
                    // while getData returned the replacement, which is the failure this recovery
                    // exists to prevent, produced by the recovery itself.
                    try {
                        if (takeDroppedRemoval(removed)) {
                            deliverDataRemoved(removed);
                        }
                    } catch (Throwable portFailed) {
                        com.codename1.io.Log.p("Wearable: re-announcing the removal of " + removed
                                + " failed; continuing the drain");
                        com.codename1.io.Log.e(portFailed);
                    }
                }
            }
            // Paths the cap discarded, handed back now that a listener exists and there is room.
            if (dropped != null) {
                DroppedDeliveryHandler handler = droppedHandler();
                if (handler != null) {
                    for (String path : dropped) {
                        // Each hand-back isolated. This is PORT code reaching into a native
                        // bridge, and one of them throwing used to escape the drain entirely:
                        // the finally released the guard, but the parked deliveries stayed
                        // parked, and because the queue was then non-empty every later
                        // publication parked behind them too. An app with a registered listener
                        // stopped receiving data until some other listener happened to register.
                        // One failing path must cost that path, not the queue.
                        try {
                            handler.deliveryDropped(path);
                        } catch (Throwable portFailed) {
                            com.codename1.io.Log.p("Wearable: recovery for " + path
                                    + " failed; continuing the drain");
                            com.codename1.io.Log.e(portFailed);
                        }
                    }
                }
            }
            // After the drain, so a re-offered payload finds room and a registered listener rather than
            // landing straight back in a full queue.
            if (replays != null) {
                for (Runnable replay : replays) {
                    // Isolated for the same reason: a replay is the port re-offering what it
                    // holds, and one that fails must not strand the queue.
                    try {
                        replay.run();
                    } catch (Throwable portFailed) {
                        com.codename1.io.Log.p("Wearable: a port replay failed; continuing the drain");
                        com.codename1.io.Log.e(portFailed);
                    }
                }
            }
        } finally {
            if (dataQueue) {
                // Cleared only now that every recovery announcement has been made, and cleared
                // even if one of them THREW. A port callback that fails -- deliveryDropped
                // reaching a native replay, say -- used to skip this, leaving the guard
                // permanently positive: every later delivery parked behind a drain that had
                // already aborted, and a process with a live listener silently stopped receiving
                // data until it restarted.
                setDrainingData(false);
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
