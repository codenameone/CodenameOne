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
/// When there is nothing on the other end, [#isSupported()] returns false and every call here is an
/// inert no-op, so this API needs no platform conditionals around it. See the package documentation
/// for how to choose between a message, replicated data and a file transfer.
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

    /// Returns true when this device can talk to a counterpart app at all. False on a desktop build,
    /// on a phone whose platform has no wearable link, and in the simulator with no watch window
    /// open. When this is false every other call here does nothing.
    ///
    /// #### Returns
    ///
    /// true if the wearable link is available
    public static boolean isSupported() {
        WearableBridge b = bridge();
        return b != null && b.isSupported();
    }

    /// Returns true when a counterpart device is paired, whether or not it is switched on or in
    /// range.
    ///
    /// #### Returns
    ///
    /// true if a counterpart device is paired
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
        WearableBridge b = bridge();
        if (b != null && b.isSupported() && path != null && contents != null) {
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
                if (replyToken != 0) {
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
    private static void deliver(Runnable delivery, List<?> listeners, List<Runnable> queue) {
        // The listener check and the enqueue share the queue's monitor with drainPending, so a
        // delivery can never be parked after the drain that would have replayed it.
        synchronized (queue) {
            if (listeners.isEmpty()) {
                queue.add(delivery);
                return;
            }
        }
        Display.getInstance().callSerially(delivery);
    }

    /// Replays what was queued for one listener type, once a listener of that type exists.
    private static void drainPending(List<Runnable> queue) {
        List<Runnable> drained;
        synchronized (queue) {
            if (queue.isEmpty()) {
                return;
            }
            drained = new ArrayList<Runnable>(queue);
            queue.clear();
        }
        for (Runnable r : drained) {
            Display.getInstance().callSerially(r);
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
