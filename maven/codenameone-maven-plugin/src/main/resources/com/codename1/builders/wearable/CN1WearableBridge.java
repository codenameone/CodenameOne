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

import android.content.Context;
import android.net.Uri;

import com.codename1.wearable.WearableConnection;
import com.codename1.wearable.WearableMessage;
import com.codename1.wearable.spi.WearableBridge;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Wearable Data Layer implementation of the Codename One {@code WearableBridge}, injected into the
 * generated project only when the app references {@code com.codename1.wearable}. The Android port
 * itself carries no dependency on play-services-wearable, which is why this class lives in the
 * builder's resources rather than in the port -- see {@link AndroidWearableSupport}.
 *
 * <p>The three Codename One transports map onto the Data Layer as follows:
 * <ul>
 *   <li>a live message is {@code MessageClient.sendMessage}, delivered only to nearby nodes;</li>
 *   <li>replicated data is a {@code DataItem} at the given path, which the system syncs to every
 *       paired node whenever it next connects, surviving both apps being killed;</li>
 *   <li>a file transfer is a DataItem carrying an {@code Asset}, which the system streams in the
 *       background.</li>
 * </ul>
 *
 * <p>Unlike Apple, Wear allows several watches paired to one phone, so sends fan out to every
 * connected node. Payloads are the opaque bytes produced by {@code WearableMessage}, so nothing here
 * has to understand the value model.
 */
public class CN1WearableBridge implements WearableBridge {
    /** Data Layer paths must start with a slash, and so do Codename One paths by convention. */
    private static final String PATH_PREFIX = "/cn1";
    /** The key the payload bytes live under inside a DataItem. */
    private static final String PAYLOAD_KEY = "cn1.payload";
    /** The publication order of a value or transfer, so the newer of two items wins. */
    private static final String SEQUENCE_KEY = "cn1.seq";
    /**
     * Transfers live under their own prefix, not under {@link #PATH_PREFIX}. Sharing the prefix made
     * the two APIs collide: {@code transferFile("/inbox", "photo.png", ...)} built the same DataItem
     * URI as {@code putData("/inbox/photo.png")}, so each could silently overwrite the other.
     *
     * <p>The trailing slash is what makes the namespace unambiguous rather than merely different.
     * {@link #encode} escapes {@code '/'}, so a replicated value's path is {@code /cn1} followed by
     * characters that never include a slash -- meaning no value can ever match {@code /cn1x/}. Without
     * the delimiter, {@code putData("xstatus")} would produce {@code /cn1xstatus} and be misread as a
     * transfer, its value dropped by the listener and hidden from {@code getDataPaths()}.
     */
    private static final String TRANSFER_PREFIX = "/cn1x/";
    /** How long a blocking Data Layer call may take before we give up and answer "not available". */
    private static final long TIMEOUT_SECONDS = 5;
    /**
     * The Codename One EDT must never wait five seconds on Play services -- isPaired/isReachable are
     * exactly the sort of thing an app calls from init() or a button handler. The node list is
     * therefore cached and refreshed off the EDT; callers get the last known answer immediately.
     */
    private static final long NODE_CACHE_MILLIS = 3000;
    private volatile List<Node> cachedNodes = new ArrayList<Node>();
    private volatile long cachedNodesStamp;
    private volatile boolean refreshingNodes;

    private final Context context;
    private final MessageClient messageClient;
    private final DataClient dataClient;
    private final NodeClient nodeClient;
    private final CapabilityClient capabilityClient;

    /**
     * Reply blocks are not a Data Layer concept: MessageClient is one-way. A request carries its
     * token in the path and the answer comes back on a reply path carrying the same token, which is
     * what lets the Codename One reply handler work identically on both platforms.
     */
    private static final String REPLY_PATH = PATH_PREFIX + "/reply/";
    private static final String REQUEST_PATH = PATH_PREFIX + "/request/";
    private static final String MESSAGE_PATH = PATH_PREFIX + "/message";

    public CN1WearableBridge(Context context) {
        this.context = context.getApplicationContext();
        this.messageClient = Wearable.getMessageClient(this.context);
        this.dataClient = Wearable.getDataClient(this.context);
        this.nodeClient = Wearable.getNodeClient(this.context);
        this.capabilityClient = Wearable.getCapabilityClient(this.context);
        current = this;
        // Sweep at startup as well as after each publish. An app that sends a few files and then
        // stops would otherwise never run the sweep again, leaving its last transfers published
        // indefinitely -- the post-publish sweep only helps an app that keeps transferring.
        expireOwnTransfers();
    }

    // --- state --------------------------------------------------------------

    public boolean isSupported() {
        return true;
    }

    public boolean isPaired() {
        // Pairing, not reachability: a paired watch that is switched off or out of range reports no
        // connected node, and the API promises these are different questions. The capability query
        // behind bondedNodeIds() uses FILTER_ALL, so it still lists a paired peer that is currently
        // disconnected, which is as close to "paired" as the Data Layer gets. A paired watch that
        // has never run this app is invisible to both queries because Android exposes no such list.
        return !connectedNodes().isEmpty() || !bondedNodeIds().isEmpty();
    }

    /// Whether an event's source node may be trusted, for the listener service's caller check.
    ///
    /// A fresh blocking query would be the strictest answer and is also the wrong one: a peer that
    /// disconnects between Play services queueing the callback and this check completing -- or a
    /// query that transiently fails -- would make us discard a message the Data Layer already
    /// validated and delivered. So the test is membership of a *recent* snapshot: nodes seen
    /// connected in the last few minutes, plus this device itself (our own published data is echoed
    /// back to us with the local node as its host). A forged intent from another app on the device
    /// still carries a node id that was never in that snapshot.
    ///
    /// @param context any context; the Data Layer clients are cheap to obtain
    /// @param sourceNodeId the node the event claims to come from
    /// @return true when the id belongs to a node we have seen
    static boolean isKnownNode(Context context, String sourceNodeId) {
        if (sourceNodeId == null || sourceNodeId.length() == 0) {
            return false;
        }
        if (recentlySeen(sourceNodeId) || sourceNodeId.equals(localNodeId(context))) {
            return true;
        }
        // Nothing remembered yet -- this is the cold-start case, where the service process was
        // created to deliver the very first event. Now a blocking query is both safe (we are on a
        // Play services callback thread, never the EDT) and necessary.
        List<String> connected = connectedNodeIds(context);
        for (String id : connected) {
            rememberNode(id);
        }
        if (recentlySeen(sourceNodeId)) {
            return true;
        }
        if (!connected.isEmpty()) {
            // A populated snapshot that does not contain the sender is real evidence against it.
            return false;
        }
        // The query established nothing at all: the sender may have disconnected while we were
        // starting, or Play services may not have been ready. That is not licence to trust an
        // arbitrary id -- this service is exported, so an empty snapshot is exactly the state a
        // forged intent would like to find. Retry a couple of times instead, which covers the
        // transient case without ever admitting an unverified node.
        for (int attempt = 0; attempt < NODE_QUERY_RETRIES; attempt++) {
            try {
                Thread.sleep(NODE_QUERY_RETRY_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
            for (String id : connectedNodeIds(context)) {
                rememberNode(id);
            }
            if (recentlySeen(sourceNodeId)) {
                return true;
            }
        }
        return false;
    }

    /// Ids of the nodes the Data Layer currently reports. Blocking; never call on the EDT.
    ///
    /// @param context any context; the Data Layer clients are cheap to obtain
    /// @return the connected node ids, never null
    static List<String> connectedNodeIds(Context context) {
        List<String> out = new ArrayList<String>();
        try {
            List<Node> nodes = Tasks.await(
                    Wearable.getNodeClient(context.getApplicationContext()).getConnectedNodes(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (Node n : nodes) {
                out.add(n.getId());
            }
        } catch (Throwable unavailable) {
            // Nothing reachable: nothing is trusted.
        }
        return out;
    }

    /**
     * Node ids seen connected recently, and when. A message may legitimately arrive from a node that
     * has just dropped off the connected list, so trust outlives the connection by a wide margin.
     */
    private static final Map<String, Long> recentNodes = new HashMap<String, Long>();
    /** How long a node stays trusted after it was last seen. */
    private static final long RECENT_NODE_MILLIS = 10 * 60 * 1000L;
    /** Retries for a cold-start node query that came back empty; see {@link #isKnownNode}. */
    private static final int NODE_QUERY_RETRIES = 2;
    private static final long NODE_QUERY_RETRY_MILLIS = 750;
    private static volatile String localNode;

    private static void rememberNode(String id) {
        if (id == null || id.length() == 0) {
            return;
        }
        synchronized (recentNodes) {
            recentNodes.put(id, Long.valueOf(System.currentTimeMillis()));
        }
    }

    private static boolean recentlySeen(String id) {
        synchronized (recentNodes) {
            Long seen = recentNodes.get(id);
            if (seen == null) {
                return false;
            }
            if (System.currentTimeMillis() - seen.longValue() > RECENT_NODE_MILLIS) {
                recentNodes.remove(id);
                return false;
            }
            return true;
        }
    }

    /// This device's own node id, cached: the Data Layer echoes our own published values back to us
    /// with the local node as the DataItem host, and dropping those would break putData locally.
    private static String localNodeId(Context context) {
        String known = localNode;
        if (known != null) {
            return known;
        }
        try {
            known = Tasks.await(
                    Wearable.getNodeClient(context.getApplicationContext()).getLocalNode(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS).getId();
            localNode = known;
        } catch (Throwable unavailable) {
            return null;
        }
        return known;
    }

    /// Nodes the Data Layer knows about whether or not they are currently connected.
    private List<String> bondedNodeIds() {
        if (bondedStamp != 0 && System.currentTimeMillis() - bondedStamp <= NODE_CACHE_MILLIS) {
            // Honour the cache lifetime on every thread. Refreshing on each EDT call would make a
            // state listener that calls isPaired() or isReachable() start another refresh, whose
            // completion notifies listeners again -- a self-sustaining loop.
            return cachedBonded;
        }
        if (com.codename1.ui.CN.isEdt()) {
            // Never block the EDT -- but the cache has to be filled by someone, or an installed
            // companion is reported absent forever. Kick off a refresh and answer with what is
            // known so far; listeners are notified only when the answer actually changed.
            refreshBondedAsync();
            return cachedBonded;
        }
        List<String> out = new ArrayList<String>();
        try {
            CapabilityInfo info = Tasks.await(
                    capabilityClient.getCapability("cn1_wearable", CapabilityClient.FILTER_ALL),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (Node n : info.getNodes()) {
                out.add(n.getId());
            }
        } catch (Throwable unavailable) {
            // Keep the previous snapshot, as every other refresh path does. Falling through to the
            // assignments below would replace a valid companion set with an empty one and stamp it
            // fresh, so isCompanionAppInstalled(), isPaired() and isReachable() would all report no
            // companion for a full cache lifetime because one query timed out.
            return cachedBonded;
        }
        cachedBonded = out;
        bondedStamp = System.currentTimeMillis();
        return out;
    }

    private volatile List<String> cachedBonded = new ArrayList<String>();
    private volatile boolean refreshingBonded;
    private volatile long bondedStamp;

    /// Accepts a capability set pushed by Play services, so the cache tracks an install or
    /// uninstall that happens while the device stays connected.
    static void capabilityChanged(CapabilityInfo info) {
        CN1WearableBridge b = current;
        if (b == null || info == null) {
            // No bridge to update the cache on, but listeners are held by WearableConnection rather
            // than by the bridge, so the state change still has to reach them -- this is the only
            // notification for it, the caller does not send a second one.
            if (info != null) {
                WearableConnection.notifyStateChanged();
            }
            return;
        }
        List<String> out = new ArrayList<String>();
        for (Node n : info.getNodes()) {
            out.add(n.getId());
        }
        boolean changed = !sameIds(b.cachedBonded, out);
        b.cachedBonded = out;
        b.bondedStamp = System.currentTimeMillis();
        if (changed) {
            WearableConnection.notifyStateChanged();
        }
    }

    /// Order-insensitive comparison of two node-id lists, so a refresh that returns the same set does
    /// not fire a state change (and cannot become a feedback loop through a listener).
    private static boolean sameIds(List<String> a, List<String> b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.size() == b.size() && a.containsAll(b);
    }

    /// A peer connected or disconnected. The caches have to be corrected *before* listeners run,
    /// otherwise a listener that responds by calling isReachable() sees the node it was just told
    /// about as still present (or still absent) for the rest of the cache lifetime.
    static void peerChanged(Node peer, boolean connected) {
        CN1WearableBridge b = current;
        if (peer != null && connected) {
            rememberNode(peer.getId());
        }
        if (b == null) {
            WearableConnection.notifyStateChanged();
            return;
        }
        List<Node> updated = new ArrayList<Node>(b.cachedNodes);
        if (peer != null) {
            for (int i = updated.size() - 1; i >= 0; i--) {
                if (peer.getId().equals(updated.get(i).getId())) {
                    updated.remove(i);
                }
            }
            if (connected) {
                updated.add(peer);
            }
        }
        b.cachedNodes = updated;
        // Keep the stamp: this is a push from Play services, which is more current than any query
        // we could make, so there is nothing to re-ask. A zero stamp would also make the next
        // sendMessage() defer needlessly.
        b.cachedNodesStamp = System.currentTimeMillis();
        // A disconnect can also mean the capability set shrank; let that refresh on its own clock.
        WearableConnection.notifyStateChanged();
    }

    /// The live bridge, so the listener service can push state into it. The service and the bridge
    /// are created independently by Android, which is why this is not a constructor argument.
    private static volatile CN1WearableBridge current;

    private void refreshBondedAsync() {
        if (refreshingBonded) {
            return;
        }
        refreshingBonded = true;
        capabilityClient.getCapability("cn1_wearable", CapabilityClient.FILTER_ALL)
                .addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<CapabilityInfo>() {
                    public void onComplete(com.google.android.gms.tasks.Task<CapabilityInfo> task) {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            // A transient failure is not evidence the companion was uninstalled.
                            // Overwriting a good cache with an empty result -- and stamping it fresh
                            // -- would make isCompanionAppInstalled(), isPaired() and isReachable()
                            // all report "no companion" for a full cache lifetime.
                            refreshingBonded = false;
                            return;
                        }
                        List<String> out = new ArrayList<String>();
                        for (Node n : task.getResult().getNodes()) {
                            out.add(n.getId());
                        }
                        boolean changed = !sameIds(cachedBonded, out);
                        cachedBonded = out;
                        bondedStamp = System.currentTimeMillis();
                        refreshingBonded = false;
                        if (changed) {
                            WearableConnection.notifyStateChanged();
                        }
                    }
                });
    }

    public boolean isReachable() {
        // "Reachable" promises the peer app can receive a message, so a connected watch that does
        // not run this app must not qualify: getConnectedNodes() lists physical devices, and only
        // the capability set says which of them installed the counterpart.
        List<String> withApp = bondedNodeIds();
        for (Node n : connectedNodes()) {
            if (n.isNearby() && withApp.contains(n.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isCompanionAppInstalled() {
        // A connected node is a connected *device*, not a device running this app -- so the node
        // list alone would report a bare watch as having the companion installed. The peer half
        // advertises the "cn1_wearable" capability (declared in res/values/cn1_wearable.xml by the
        // build), so asking who advertises it is the actual question.
        return !bondedNodeIds().isEmpty();
    }

    public String[] getConnectedNodes() {
        List<Node> nodes = connectedNodes();
        String[] out = new String[nodes.size()];
        for (int i = 0; i < out.length; i++) {
            Node n = nodes.get(i);
            // id \t displayName \t nearby -- the flat form the SPI documents.
            out[i] = n.getId() + "\t" + n.getDisplayName() + "\t" + (n.isNearby() ? "1" : "0");
        }
        return out;
    }

    /**
     * The nodes last seen, refreshed in the background. Blocking is only acceptable off the EDT --
     * on it, a stale answer now beats a correct answer after a five-second freeze.
     */
    private List<Node> connectedNodes() {
        long age = System.currentTimeMillis() - cachedNodesStamp;
        if (age > NODE_CACHE_MILLIS) {
            if (com.codename1.ui.CN.isEdt()) {
                refreshNodesAsync();
            } else {
                refreshNodesNow();
            }
        }
        return cachedNodes;
    }

    private void refreshNodesNow() {
        List<Node> fresh;
        try {
            fresh = Tasks.await(nodeClient.getConnectedNodes(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Throwable unavailable) {
            // Keep the previous snapshot, exactly as the asynchronous and send-time refreshes do.
            // Clearing it here would report every peer gone -- and stamp that fresh -- because one
            // blocking query happened to time out.
            return;
        }
        cachedNodes = fresh;
        cachedNodesStamp = System.currentTimeMillis();
        rememberAll(cachedNodes);
    }

    private void refreshNodesAsync() {
        if (refreshingNodes) {
            return;
        }
        refreshingNodes = true;
        nodeClient.getConnectedNodes().addOnCompleteListener(
                new com.google.android.gms.tasks.OnCompleteListener<List<Node>>() {
                    public void onComplete(com.google.android.gms.tasks.Task<List<Node>> task) {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            // A transient Play services failure is not evidence that every peer
                            // vanished. Replacing a good snapshot with an empty list would make
                            // isReachable() and getConnectedNodes() report a disconnected pair for a
                            // full cache lifetime, and fire a spurious state change with it. Keep
                            // what we had; the next call retries.
                            refreshingNodes = false;
                            return;
                        }
                        List<Node> fresh = task.getResult();
                        boolean changed = !sameIds(idsOf(cachedNodes), idsOf(fresh));
                        cachedNodes = fresh;
                        cachedNodesStamp = System.currentTimeMillis();
                        refreshingNodes = false;
                        rememberAll(fresh);
                        // Reachability may have changed; let listeners re-query. Only on an actual
                        // change, or a listener that re-queries here would refresh forever.
                        if (changed) {
                            WearableConnection.notifyStateChanged();
                        }
                    }
                });
    }

    private static void rememberAll(List<Node> nodes) {
        for (Node n : nodes) {
            rememberNode(n.getId());
        }
    }

    private static List<String> idsOf(List<Node> nodes) {
        List<String> out = new ArrayList<String>();
        for (Node n : nodes) {
            out.add(n.getId());
        }
        return out;
    }

    // --- messages -----------------------------------------------------------

    public void sendMessage(final String path, final byte[] payload, final int replyToken) {
        if (System.currentTimeMillis() - cachedNodesStamp > NODE_CACHE_MILLIS) {
            // The cache is empty or stale. Sending now would fan out to a list that predates the
            // current connection state and report "no nearby device" while a watch is sitting right
            // there, so resolve the node list first -- a send is not a state query, and it is worth
            // one round trip to address it correctly. (connectedNodes() would only *start* an async
            // refresh on the EDT and then fan out to the stale list anyway.)
            nodeClient.getConnectedNodes().addOnCompleteListener(
                    new com.google.android.gms.tasks.OnCompleteListener<List<Node>>() {
                        public void onComplete(com.google.android.gms.tasks.Task<List<Node>> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                cachedNodes = task.getResult();
                                cachedNodesStamp = System.currentTimeMillis();
                                rememberAll(cachedNodes);
                            }
                            // On failure keep the previous snapshot rather than clearing it: a
                            // stale-but-real node list still addresses the peer, whereas an empty
                            // one silently drops this message (or fails its reply handler) purely
                            // because a refresh happened to time out.
                            fanOut(path, payload, replyToken);
                        }
                    });
            return;
        }
        fanOut(path, payload, replyToken);
    }

    private void fanOut(String path, byte[] payload, final int replyToken) {
        List<Node> nodes = connectedNodes();
        boolean sentToAnyone = false;
        List<com.google.android.gms.tasks.Task<Integer>> tasks =
                new ArrayList<com.google.android.gms.tasks.Task<Integer>>();
        for (Node n : nodes) {
            if (!n.isNearby()) {
                continue;
            }
            // The peer needs both the CN1 path and, when an answer is wanted, the token to answer
            // with. Both ride in the Data Layer path so the payload stays exactly the app's bytes.
            // encode() escapes '/' as well, so the encoded app path is a single segment containing no
            // delimiter: the '/' inserted here is unambiguously the separator, and a relative app
            // path like "steps" survives instead of arriving as "/steps".
            String wire = (replyToken == 0 ? MESSAGE_PATH : REQUEST_PATH + replyToken)
                    + "/" + encode(path);
            tasks.add(messageClient.sendMessage(n.getId(), wire, payload));
            sentToAnyone = true;
        }
        if (!sentToAnyone) {
            if (replyToken != 0) {
                WearableConnection.deliverReply(replyToken, null,
                        "No nearby device is running the app");
            }
            return;
        }
        if (replyToken != 0) {
            // A send can succeed and still never be answered -- an older peer that does not know
            // the path, or a cold start Android refused to allow. Without this the pending entry
            // lives forever and neither handler method is ever called.
            scheduleReplyTimeout(replyToken);
            // Fail only when NO node accepted the request: one watch failing while another
            // succeeds must not cancel the handler that the successful one is about to answer.
            com.google.android.gms.tasks.Tasks.whenAllComplete(tasks).addOnCompleteListener(
                    new com.google.android.gms.tasks.OnCompleteListener<List<com.google.android.gms.tasks.Task<?>>>() {
                        public void onComplete(
                                com.google.android.gms.tasks.Task<List<com.google.android.gms.tasks.Task<?>>> all) {
                            if (all.getResult() == null) {
                                return;
                            }
                            for (com.google.android.gms.tasks.Task<?> t : all.getResult()) {
                                if (t.isSuccessful()) {
                                    return;
                                }
                            }
                            WearableConnection.deliverReply(replyToken, null,
                                    "The message could not be delivered to any paired device");
                        }
                    });
        }
    }

    public void sendReply(int replyToken, byte[] payload) {
        // Back to the node that asked, not to every watch on the wrist rack: tokens are allocated
        // per node and routinely collide, so broadcasting would answer the wrong request.
        // Two watches can allocate the same token before either is answered, so the origin is
        // keyed by node AND token; the local token handed to Java is unique on its own.
        InboundRequest req;
        synchronized (inboundNodes) {
            req = inboundNodes.remove(Integer.valueOf(replyToken));
        }
        if (req == null) {
            return;
        }
        messageClient.sendMessage(req.nodeId, REPLY_PATH + req.peerToken, payload);
    }

    /// Records which node sent a request, so its answer can be routed back to it. Called by
    /// {@link CN1WearableListenerService} as the request arrives.
    static int rememberRequestOrigin(int peerToken, String nodeId) {
        synchronized (inboundNodes) {
            // An app that never answers a path would otherwise grow this map for its whole life.
            // The sender gives up after its own timeout, so an entry older than that can go.
            long cutoff = System.currentTimeMillis() - INBOUND_TTL_MILLIS;
            java.util.Iterator<Map.Entry<Integer, InboundRequest>> it =
                    inboundNodes.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().created < cutoff) {
                    it.remove();
                }
            }
            int local = nextLocalToken++;
            inboundNodes.put(Integer.valueOf(local), new InboundRequest(nodeId, peerToken));
            return local;
        }
    }

    /** How long an unanswered inbound request is remembered; outlives the sender's own timeout. */
    private static final long INBOUND_TTL_MILLIS = 60000;

    /// Who asked, and what token they used. Their token is theirs alone; ours identifies the
    /// request locally so two nodes cannot collide.
    private static final class InboundRequest {
        final String nodeId;
        final int peerToken;
        final long created;

        InboundRequest(String nodeId, int peerToken) {
            this.nodeId = nodeId;
            this.peerToken = peerToken;
            this.created = System.currentTimeMillis();
        }
    }

    private static final Map<Integer, InboundRequest> inboundNodes =
            new HashMap<Integer, InboundRequest>();
    private static int nextLocalToken = 1;

    /** How long an accepted request may go unanswered before the handler is failed. */
    private static final int REPLY_TIMEOUT_MILLIS = 30000;

    /**
     * One daemon timer for every reply deadline in the process. A Timer per request would start a
     * thread per request, and a burst of sends would hold all of them for the full timeout.
     */
    private static final java.util.Timer replyTimer = new java.util.Timer("cn1-wearable-replies", true);
    private static final Map<Integer, java.util.TimerTask> replyTimeouts =
            new HashMap<Integer, java.util.TimerTask>();

    /**
     * Fails a pending request that is never answered. {@code deliverReply} removes the token on the
     * first call, so a real answer arriving first makes this a no-op even if the task still runs;
     * {@link #cancelReplyTimeout} additionally stops it being scheduled at all.
     */
    private void scheduleReplyTimeout(final int replyToken) {
        java.util.TimerTask task = new java.util.TimerTask() {
            public void run() {
                synchronized (replyTimeouts) {
                    replyTimeouts.remove(Integer.valueOf(replyToken));
                }
                WearableConnection.deliverReply(replyToken, null,
                        "The peer did not answer within " + (REPLY_TIMEOUT_MILLIS / 1000)
                                + " seconds");
            }
        };
        synchronized (replyTimeouts) {
            replyTimeouts.put(Integer.valueOf(replyToken), task);
        }
        replyTimer.schedule(task, REPLY_TIMEOUT_MILLIS);
    }

    /// Cancels the timeout for a request that has just been answered for real, so a burst of
    /// requests does not keep one scheduled task per request alive for the full timeout.
    static void cancelReplyTimeout(int replyToken) {
        java.util.TimerTask task;
        synchronized (replyTimeouts) {
            task = replyTimeouts.remove(Integer.valueOf(replyToken));
        }
        if (task != null) {
            task.cancel();
        }
    }

    // --- replicated data ----------------------------------------------------

    public void putData(String path, byte[] payload) {
        // The payload travels inside a DataMap rather than as the item's raw data so it can be
        // stamped with a publication sequence. Both halves of a pair may publish the same logical
        // path, which the Data Layer stores as two items under two node authorities; without an
        // ordering stamp a reader has no way to tell which of them is the newer value.
        PutDataMapRequest req = PutDataMapRequest.create(dataPath(path));
        req.getDataMap().putByteArray(PAYLOAD_KEY, payload == null ? new byte[0] : payload);
        req.getDataMap().putLong(SEQUENCE_KEY, nextSequence());
        // Urgent: without it the system may sit on the change for minutes, which reads as "my watch
        // never updated" even though the API did its job.
        dataClient.putDataItem(req.asPutDataRequest().setUrgent());
    }

    /**
     * A monotonic publication stamp. Wall-clock millis order correctly against the peer's stamps
     * (both devices' clocks are network-synced within far less than a replication round trip), and
     * the counter breaks ties between two puts inside the same millisecond on this device.
     */
    private static synchronized long nextSequence() {
        long now = System.currentTimeMillis();
        lastSequence = now > lastSequence ? now : lastSequence + 1;
        return lastSequence;
    }

    private static long lastSequence;

    public byte[] getData(String path) {
        try {
            // The authority is required: a wear:// Uri without one matches nothing. "*" means
            // "any node", which is what a reader wants -- the value may have come from either side.
            Uri uri = new Uri.Builder().scheme("wear").authority("*").path(dataPath(path)).build();
            DataItemBuffer items = Tasks.await(dataClient.getDataItems(uri),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                byte[] best = null;
                long bestSeq = Long.MIN_VALUE;
                for (DataItem item : items) {
                    DataMap map = valueMap(item);
                    if (map == null) {
                        continue;
                    }
                    long seq = map.getLong(SEQUENCE_KEY, Long.MIN_VALUE);
                    if (best == null || seq > bestSeq) {
                        best = map.getByteArray(PAYLOAD_KEY);
                        bestSeq = seq;
                    }
                }
                return best;
            } finally {
                items.release();
            }
        } catch (Throwable unavailable) {
            return null;
        }
    }

    /**
     * The DataMap of an ordinary published value, or null when the item is not one -- a file transfer
     * (which carries an Asset instead of a payload) or something not written by this API at all.
     * This is what keeps transfers out of {@link #getDataPaths()} and out of {@link #getData}.
     *
     * @param item a received or queried data item
     * @return the value's DataMap, or null
     */
    /// The payload bytes out of a value's DataMap, never null.
    ///
    /// @param value a map obtained from {@link #valueMap}
    /// @return the published bytes
    static byte[] payloadOf(DataMap value) {
        byte[] payload = value.getByteArray(PAYLOAD_KEY);
        return payload == null ? new byte[0] : payload;
    }

    static DataMap valueMap(DataItem item) {
        try {
            DataMap map = DataMapItem.fromDataItem(item).getDataMap();
            return map.containsKey(PAYLOAD_KEY) ? map : null;
        } catch (Throwable notADataMap) {
            return null;
        }
    }

    public void removeData(String path) {
        Uri uri = new Uri.Builder().scheme("wear").authority("*").path(dataPath(path)).build();
        dataClient.deleteDataItems(uri);
    }

    public String[] getDataPaths() {
        try {
            DataItemBuffer items = Tasks.await(dataClient.getDataItems(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                List<String> out = new ArrayList<String>();
                for (DataItem item : items) {
                    String p = item.getUri().getPath();
                    if (p == null || isTransferPath(p) || !p.startsWith(PATH_PREFIX)
                            || valueMap(item) == null) {
                        // Not ours, or a file transfer. A transfer lives in its own namespace and
                        // getData() on its storage path would answer with DataMap metadata rather
                        // than a payload, so it is not a readable replicated path. (The prefix test
                        // is explicit because the transfer prefix extends the value prefix.)
                        continue;
                    }
                    // Both halves may publish the same logical path, giving two items under two node
                    // authorities; the API contract is one path per value.
                    String logical = decode(p.substring(PATH_PREFIX.length()));
                    if (!out.contains(logical)) {
                        out.add(logical);
                    }
                }
                return out.toArray(new String[out.size()]);
            } finally {
                items.release();
            }
        } catch (Throwable unavailable) {
            return new String[0];
        }
    }

    public void transferFile(String path, String name, byte[] contents) {
        // A DataItem's inline payload is capped at about 100KB, which a real file routinely
        // exceeds; an Asset is the Data Layer's own answer for bulk and is streamed in the
        // background. The DataItem carries the name and the Asset, so the receiver still gets a
        // WearableMessage rather than raw bytes.
        String fileName = name == null ? "file" : name;
        byte[] body = contents == null ? new byte[0] : contents;
        long sequence = nextSequence();
        PutDataMapRequest req = PutDataMapRequest.create(transferPath(path, fileName, sequence));
        req.getDataMap().putString("name", fileName);
        // The DataItem path is namespaced and filename-suffixed, so the caller's own path has to
        // travel with the payload -- a listener routes on the path it was given, not on ours.
        req.getDataMap().putString("cn1.path", path);
        // A file transfer is a one-shot operation, but a DataItem is a *value*: sending the same
        // bytes to the same name twice would produce an identical item, which the Data Layer treats
        // as unchanged and never reports, silently dropping the second transfer. The sequence stamp
        // makes every invocation a real change.
        req.getDataMap().putLong(SEQUENCE_KEY, sequence);
        req.getDataMap().putAsset("asset", Asset.createFromBytes(body));
        dataClient.putDataItem(req.asPutDataRequest().setUrgent());
        expireOwnTransfers();
    }

    /** How long one of our own published transfer items is kept before it is swept. */
    private static final long TRANSFER_RETENTION_MILLIS = 24 * 60 * 60 * 1000L;

    /**
     * Deletes transfer items this device published long enough ago that the peer has had every
     * reasonable chance to take them.
     *
     * <p>Putting the sequence in the item path is what stops a second transfer replacing a first
     * that has not synced yet -- but it also means nothing ever reuses a URI, so without a sweep an
     * app that transfers regularly would grow its Data Layer storage without bound. Only our own
     * items are touched, and only old ones: a receiver still never deletes, because that would
     * propagate and rob a second watch of the file.
     */
    private void expireOwnTransfers() {
        final long cutoff = System.currentTimeMillis() - TRANSFER_RETENTION_MILLIS;
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                try {
                    DataItemBuffer items = Tasks.await(dataClient.getDataItems(),
                            TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    try {
                        String localNode = localNodeId(context);
                        for (DataItem item : items) {
                            String p = item.getUri().getPath();
                            if (p == null || !isTransferPath(p)) {
                                continue;
                            }
                            // Only our OWN items. getDataItems() also returns transfers replicated
                            // from other nodes, and deleting one of those propagates -- which is
                            // precisely how a second watch loses a file it has not collected yet.
                            // A publisher is responsible for its own items and nobody else's.
                            if (localNode == null || !localNode.equals(item.getUri().getHost())) {
                                continue;
                            }
                            DataMap map = valueOrTransferMap(item);
                            long seq = sequenceOf(map);
                            // The stamp is wall-clock millis, so it doubles as the publication time.
                            if (seq != Long.MIN_VALUE && seq < cutoff) {
                                dataClient.deleteDataItems(item.getUri());
                            }
                        }
                    } finally {
                        items.release();
                    }
                } catch (Throwable unavailable) {
                    // Best effort: the next transfer sweeps again.
                }
            }
        }, 0);
    }

    /**
     * Rebuilds the {@code WearableMessage} form of a file transfer, or null when the item is an
     * ordinary published value rather than a transfer.
     *
     * @param context any context
     * @param item the received data item
     * @return the encoded payload, or null
     */
    static Transfer decodeTransfer(Context context, DataItem item) {
        Transfer t = decodeTransferOnce(context, item);
        if (t == Transfer.UNREADABLE) {
            scheduleTransferRetry(context, item.getUri());
        }
        return t;
    }

    /// One attempt, with no retry scheduling -- the form the retry itself uses.
    private static Transfer decodeTransferOnce(Context context, DataItem item) {
        DataMap map;
        Asset asset;
        try {
            map = DataMapItem.fromDataItem(item).getDataMap();
            asset = map.getAsset("asset");
        } catch (Throwable notADataMap) {
            return Transfer.NOT_A_TRANSFER;
        }
        if (asset == null) {
            return Transfer.NOT_A_TRANSFER;
        }
        try {
            java.io.InputStream in = Tasks.await(
                    Wearable.getDataClient(context.getApplicationContext()).getFdForAsset(asset),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS).getInputStream();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            // The caller's own path, not the namespaced DataItem one; returned alongside the payload
            // because the delivery path routes on the path it is given, which would otherwise be the
            // filename-suffixed storage path.
            String logical = map.getString("cn1.path", item.getUri().getPath());
            return Transfer.of(logical, new WearableMessage(logical)
                    .put("name", map.getString("name", "file"))
                    .put("contents", out.toByteArray())
                    .toByteArray());
        } catch (Throwable assetUnreadable) {
            // A transient download failure -- typically getFdForAsset timing out while the system
            // is still streaming the bytes. Forwarding DataItem.getData() here would hand the
            // listener DataMap metadata dressed up as a payload, so retry instead: keeping the item
            // published is not by itself enough, because an unchanged item produces no further
            // callback and the transfer would be lost for good.
            return Transfer.UNREADABLE;
        }
    }

    /** How many times, and how far apart, an unreadable asset is re-fetched before giving up. */
    private static final int TRANSFER_RETRIES = 4;
    private static final long TRANSFER_RETRY_MILLIS = 3000;

    /**
     * Re-reads a transfer whose asset could not be resolved, on the shared timer. Each attempt goes
     * back to the Data Layer for the item, so a transfer that was still streaming lands as soon as
     * it is complete; after the last attempt the transfer is genuinely dropped.
     */
    private static void scheduleTransferRetry(final Context context, final Uri uri) {
        scheduleTransferRetry(context, uri, 1);
    }

    /**
     * Retries run on their own timer, not on {@link #replyTimer}. A retry blocks on
     * {@code Tasks.await} and then reads the whole asset stream, and the reply timer is a single
     * thread that also owns every pending 30-second reply deadline -- a slow or large asset would
     * delay those deadlines, so a request that timed out would be reported late or not at all.
     */
    private static final java.util.Timer transferTimer =
            new java.util.Timer("cn1-wearable-transfers", true);

    private static void scheduleTransferRetry(final Context context, final Uri uri, final int attempt) {
        if (uri == null || attempt > TRANSFER_RETRIES) {
            return;
        }
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                try {
                    DataItemBuffer items = Tasks.await(
                            Wearable.getDataClient(context.getApplicationContext()).getDataItems(uri),
                            TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    try {
                        for (DataItem item : items) {
                            Transfer t = decodeTransferOnce(context, item);
                            if (t.payload != null) {
                                if (claimTransfer(uri, sequenceOf(valueOrTransferMap(item)))) {
                                    WearableConnection.deliverDataChanged(t.logicalPath, t.payload);
                                }
                                return;
                            }
                        }
                    } finally {
                        items.release();
                    }
                    scheduleTransferRetry(context, uri, attempt + 1);
                } catch (Throwable stillUnavailable) {
                    scheduleTransferRetry(context, uri, attempt + 1);
                }
            }
        }, TRANSFER_RETRY_MILLIS * attempt);
    }

    /**
     * The outcome of inspecting a received DataItem: an ordinary published value, a decoded file
     * transfer, or a transfer whose asset could not be read this time.
     */
    static final class Transfer {
        static final Transfer NOT_A_TRANSFER = new Transfer(null, null, false);
        static final Transfer UNREADABLE = new Transfer(null, null, true);

        final byte[] payload;
        /** The path the sender passed to {@code transferFile}, which is what listeners route on. */
        final String logicalPath;
        final boolean isTransfer;

        private Transfer(String logicalPath, byte[] payload, boolean isTransfer) {
            this.logicalPath = logicalPath;
            this.payload = payload;
            this.isTransfer = isTransfer;
        }

        static Transfer of(String logicalPath, byte[] payload) {
            return new Transfer(logicalPath, payload, true);
        }
    }

    // --- paths --------------------------------------------------------------

    static String dataPath(String path) {
        return PATH_PREFIX + encode(path);
    }

    /// The DataItem path a file transfer is stored at: its own namespace, and suffixed with the file
    /// name so two files sent to one logical path do not overwrite each other. The logical path
    /// travels in the DataMap, because this is not it.
    ///
    /// @param path the path the sender passed to transferFile
    /// @param fileName the file's name
    /// @return the DataItem path
    static String transferPath(String path, String fileName, long sequence) {
        // The sequence is part of the URI, not just the payload. A transfer is one-shot, so two
        // sends to the same path and name while the peer is offline have to queue as two items --
        // sharing a URI meant the second Asset replaced the first before it could ever sync.
        return TRANSFER_PREFIX + encode(path + "/" + fileName) + "/" + Long.toHexString(sequence);
    }

    /// True when a DataItem path belongs to the transfer namespace.
    ///
    /// @param path a DataItem path
    /// @return true for a transfer item
    static boolean isTransferPath(String path) {
        return path != null && path.startsWith(TRANSFER_PREFIX);
    }

    static String transferPrefix() {
        return TRANSFER_PREFIX;
    }

    /**
     * Whether a received item is newer than the last thing delivered for its logical path.
     *
     * <p>Both nodes may publish the same path, which the Data Layer stores as two items under two
     * authorities. A reconnect can then hand us the older one after the newer, and forwarding it
     * would walk a listener-driven UI back to stale state while an immediate {@code getData()} still
     * returned the newer value -- the listener and the getter disagreeing about the same path.
     *
     * @param path the logical path
     * @param sequence the item's publication stamp
     * @return true when the event should be delivered
     */
    static boolean isNewerThanDelivered(String path, long sequence) {
        synchronized (deliveredSequences) {
            Long previous = deliveredSequences.get(path);
            if (previous != null && sequence <= previous.longValue()) {
                return false;
            }
            deliveredSequences.put(path, Long.valueOf(sequence));
            return true;
        }
    }

    /**
     * Forgets a path's delivery stamp, so a value republished after a removal is delivered even if
     * the publisher's clock produced a lower stamp than the removed value carried.
     *
     * @param path the logical path
     */
    static void forgetDeliveredSequence(String path) {
        synchronized (deliveredSequences) {
            deliveredSequences.remove(path);
        }
    }

    /**
     * Delivery stamps, bounded.
     *
     * <p>Replicated paths are few, but every transfer contributes a key -- transfers are addressed
     * by a sequence-suffixed URI so that repeated sends queue instead of replacing each other, which
     * means their keys are all distinct and none is ever superseded. Left unbounded this map grows
     * for the life of the process on a phone that receives files regularly.
     *
     * <p>Access-ordered with an eviction cap: the only cost of evicting a transfer claim is that a
     * re-synced copy of a very old transfer could be delivered twice, and the sender's own sweep
     * removes those items long before that many newer ones accumulate.
     */
    private static final Map<String, Long> deliveredSequences =
            new java.util.LinkedHashMap<String, Long>(64, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                    return size() > MAX_DELIVERY_KEYS;
                }
            };

    private static final int MAX_DELIVERY_KEYS = 2048;

    /**
     * The value a path still resolves to, or null when nothing is left under it.
     *
     * <p>Used when one authority's DataItem is deleted: both nodes may have published the path, so a
     * deletion event is not on its own evidence that the value is gone. Answering from a fresh query
     * keeps the listener and {@code getData} telling the same story.
     *
     * @param context any context
     * @param path the application path
     * @return the winning payload, or null when the path is genuinely empty
     */
    static byte[] currentValue(Context context, String path) throws java.io.IOException {
        ResolvedValue v = resolveValue(context, path);
        return v == null ? null : v.payload;
    }

    /** A path's winning value together with the sequence it was published at. */
    static final class ResolvedValue {
        final byte[] payload;
        final long sequence;

        ResolvedValue(byte[] payload, long sequence) {
            this.payload = payload;
            this.sequence = sequence;
        }
    }

    /**
     * The winning value for a path and the sequence it carries, or null when the path is empty.
     *
     * <p>The sequence matters to the caller: after a deletion the surviving item has to be recorded
     * as delivered, or an older item still queued under another authority would later pass the
     * newer-than-delivered test and overwrite it.
     *
     * @param context any context
     * @param path the application path
     * @return the winner, or null when nothing is published there
     * @throws java.io.IOException when the query failed, which is NOT the same as an empty path
     */
    static ResolvedValue resolveValue(Context context, String path) throws java.io.IOException {
        try {
            Uri uri = new Uri.Builder().scheme("wear").authority("*").path(dataPath(path)).build();
            DataItemBuffer items = Tasks.await(
                    Wearable.getDataClient(context.getApplicationContext()).getDataItems(uri),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                byte[] best = null;
                long bestSeq = Long.MIN_VALUE;
                for (DataItem item : items) {
                    DataMap map = valueMap(item);
                    if (map == null) {
                        continue;
                    }
                    long seq = sequenceOf(map);
                    if (best == null || seq > bestSeq) {
                        best = payloadOf(map);
                        bestSeq = seq;
                    }
                }
                return best == null ? null : new ResolvedValue(best, bestSeq);
            } finally {
                items.release();
            }
        } catch (Throwable unavailable) {
            // Not the same as "the path is empty": saying so would let a timed-out query be reported
            // to the app as a removal of a path another node may still be publishing.
            throw new java.io.IOException("could not resolve " + path, unavailable);
        }
    }

    /// The stamp an item was published at, or {@code Long.MIN_VALUE} for an item that predates
    /// stamping (which then always counts as older than anything stamped).
    ///
    /// @param map a value or transfer DataMap
    /// @return the publication stamp
    /// The DataMap of a value or a transfer, whichever this item is, or null when it is neither.
    ///
    /// @param item a received or queried data item
    /// @return the item's DataMap, or null
    static DataMap valueOrTransferMap(DataItem item) {
        try {
            return DataMapItem.fromDataItem(item).getDataMap();
        } catch (Throwable notADataMap) {
            return null;
        }
    }

    static long sequenceOf(DataMap map) {
        return map == null ? Long.MIN_VALUE : map.getLong(SEQUENCE_KEY, Long.MIN_VALUE);
    }

    /**
     * Records that a transfer has been handed to the app, so a re-sync of the same item does not
     * deliver the same one-shot file twice.
     *
     * <p>Deliberately NOT a delete. A DataItem belongs to the node that published it, and deleting it
     * from a receiver propagates the deletion to every other node: with two watches paired to one
     * phone, the first to connect would consume the item and the second would receive the tombstone
     * instead of the file. Suppressing the duplicate locally keeps the Data Layer's own multi-peer
     * replication intact, which is the property that makes a transfer reach every watch at all.
     *
     * <p>The sender bounds the storage instead -- see {@link #transferFile}, where republishing the
     * same path and name replaces the item rather than adding one.
     *
     * @param uri the delivered transfer's item Uri
     * @param sequence the transfer's publication stamp
     * @return true when this is the first delivery of that transfer
     */
    static boolean claimTransfer(Uri uri, long sequence) {
        if (uri == null) {
            return true;
        }
        // Keyed by the publishing node as well as the path. Two devices may transfer the same
        // logical path and file name; their items differ only in the Uri authority, so dropping it
        // would treat the two as one stream and discard the second sender's file whenever its
        // sequence did not happen to exceed the first's.
        return isNewerThanDelivered("\u0000xfer:" + uri.getHost() + ":" + uri.getPath(), sequence);
    }

    /**
     * Data Layer paths allow a restricted character set and are matched by prefix, so a Codename One
     * path is percent-escaped into it and unescaped on the way back.
     *
     * <p>{@code '/'} is escaped along with everything else, which is what makes an encoded path a
     * single segment carrying no delimiter of its own. A request's wire form can then separate its
     * reply token from the application path with a literal slash, and an application path is
     * reproduced exactly -- whether or not the app gave it a leading slash.
     */
    static String encode(String path) {
        if (path == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('%').append(Integer.toHexString(0x10000 | c).substring(1));
            }
        }
        return sb.toString();
    }

    static String decode(String path) {
        if (path == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '%' && i + 4 < path.length()) {
                sb.append((char) Integer.parseInt(path.substring(i + 1, i + 5), 16));
                i += 4;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** The wire path prefixes, shared with the listener service. */
    static String messagePath() {
        return MESSAGE_PATH;
    }

    static String requestPath() {
        return REQUEST_PATH;
    }

    static String replyPath() {
        return REPLY_PATH;
    }

    static String pathPrefix() {
        return PATH_PREFIX;
    }
}
