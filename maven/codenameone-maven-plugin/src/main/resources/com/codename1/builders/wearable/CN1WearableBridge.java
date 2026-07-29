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
    }

    // --- state --------------------------------------------------------------

    public boolean isSupported() {
        return true;
    }

    public boolean isPaired() {
        // Pairing, not reachability: a paired watch that is switched off or out of range reports no
        // connected node, and the API promises these are different questions.
        return !connectedNodes().isEmpty() || !bondedNodeIds().isEmpty();
    }

    /// Ids of the nodes the Data Layer currently reports, for the listener service's caller check.
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

    /// Nodes the Data Layer knows about whether or not they are currently connected.
    private List<String> bondedNodeIds() {
        if (com.codename1.ui.CN.isEdt()) {
            // Never block the EDT -- but the cache has to be filled by someone, or an installed
            // companion is reported absent forever. Kick off a refresh and answer with what is
            // known so far; listeners are notified when it lands.
            refreshBondedAsync();
            return cachedBonded;
        }
        if (bondedStamp != 0 && System.currentTimeMillis() - bondedStamp <= NODE_CACHE_MILLIS) {
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
            // No capability info: fall back to "nothing known".
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
            return;
        }
        List<String> out = new ArrayList<String>();
        for (Node n : info.getNodes()) {
            out.add(n.getId());
        }
        b.cachedBonded = out;
        b.bondedStamp = System.currentTimeMillis();
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
                        List<String> out = new ArrayList<String>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (Node n : task.getResult().getNodes()) {
                                out.add(n.getId());
                            }
                        }
                        cachedBonded = out;
                        bondedStamp = System.currentTimeMillis();
                        refreshingBonded = false;
                        WearableConnection.notifyStateChanged();
                    }
                });
    }

    public boolean isReachable() {
        for (Node n : connectedNodes()) {
            if (n.isNearby()) {
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
        try {
            cachedNodes = Tasks.await(nodeClient.getConnectedNodes(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Throwable unavailable) {
            cachedNodes = new ArrayList<Node>();
        }
        cachedNodesStamp = System.currentTimeMillis();
    }

    private void refreshNodesAsync() {
        if (refreshingNodes) {
            return;
        }
        refreshingNodes = true;
        nodeClient.getConnectedNodes().addOnCompleteListener(
                new com.google.android.gms.tasks.OnCompleteListener<List<Node>>() {
                    public void onComplete(com.google.android.gms.tasks.Task<List<Node>> task) {
                        cachedNodes = task.isSuccessful() && task.getResult() != null
                                ? task.getResult() : new ArrayList<Node>();
                        cachedNodesStamp = System.currentTimeMillis();
                        refreshingNodes = false;
                        // Reachability may have changed; let listeners re-query.
                        WearableConnection.notifyStateChanged();
                    }
                });
    }

    // --- messages -----------------------------------------------------------

    public void sendMessage(final String path, final byte[] payload, final int replyToken) {
        if (cachedNodesStamp == 0) {
            // Nothing has been discovered yet. Sending now would fan out to an empty list and
            // report "no nearby device" while a watch is sitting right there, so wait for the
            // first refresh instead of trusting a cache that has never been filled.
            nodeClient.getConnectedNodes().addOnCompleteListener(
                    new com.google.android.gms.tasks.OnCompleteListener<List<Node>>() {
                        public void onComplete(com.google.android.gms.tasks.Task<List<Node>> task) {
                            cachedNodes = task.isSuccessful() && task.getResult() != null
                                    ? task.getResult() : new ArrayList<Node>();
                            cachedNodesStamp = System.currentTimeMillis();
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
            // The '/' after the token is the delimiter the listener splits on; a CN1 path is only
            // conventionally slash-prefixed, so add one rather than assuming it.
            String wire = (replyToken == 0 ? MESSAGE_PATH : REQUEST_PATH + replyToken)
                    + slashPrefixed(encode(path));
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
            int local = nextLocalToken++;
            inboundNodes.put(Integer.valueOf(local), new InboundRequest(nodeId, peerToken));
            return local;
        }
    }

    /// Who asked, and what token they used. Their token is theirs alone; ours identifies the
    /// request locally so two nodes cannot collide.
    private static final class InboundRequest {
        final String nodeId;
        final int peerToken;

        InboundRequest(String nodeId, int peerToken) {
            this.nodeId = nodeId;
            this.peerToken = peerToken;
        }
    }

    private static final Map<Integer, InboundRequest> inboundNodes =
            new HashMap<Integer, InboundRequest>();
    private static int nextLocalToken = 1;

    /** How long an accepted request may go unanswered before the handler is failed. */
    private static final int REPLY_TIMEOUT_MILLIS = 30000;

    /**
     * Fails a pending request that is never answered. {@code deliverReply} removes the token on the
     * first call, so a real answer arriving first makes this a no-op.
     */
    private void scheduleReplyTimeout(final int replyToken) {
        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            public void run() {
                WearableConnection.deliverReply(replyToken, null,
                        "The peer did not answer within " + (REPLY_TIMEOUT_MILLIS / 1000)
                                + " seconds");
            }
        }, REPLY_TIMEOUT_MILLIS);
    }

    private static String slashPrefixed(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    // --- replicated data ----------------------------------------------------

    public void putData(String path, byte[] payload) {
        PutDataRequest req = PutDataRequest.create(dataPath(path));
        req.setData(payload == null ? new byte[0] : payload);
        // Urgent: without it the system may sit on the change for minutes, which reads as "my watch
        // never updated" even though the API did its job.
        dataClient.putDataItem(req.setUrgent());
    }

    public byte[] getData(String path) {
        try {
            // The authority is required: a wear:// Uri without one matches nothing. "*" means
            // "any node", which is what a reader wants -- the value may have come from either side.
            Uri uri = new Uri.Builder().scheme("wear").authority("*").path(dataPath(path)).build();
            DataItemBuffer items = Tasks.await(dataClient.getDataItems(uri),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                if (items.getCount() == 0) {
                    return null;
                }
                return items.get(0).getData();
            } finally {
                items.release();
            }
        } catch (Throwable unavailable) {
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
                    if (p != null && p.startsWith(PATH_PREFIX)) {
                        out.add(decode(p.substring(PATH_PREFIX.length())));
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
        PutDataMapRequest req = PutDataMapRequest.create(dataPath(path + "/" + fileName));
        req.getDataMap().putString("name", fileName);
        req.getDataMap().putAsset("asset", Asset.createFromBytes(body));
        dataClient.putDataItem(req.asPutDataRequest().setUrgent());
    }

    /**
     * Rebuilds the {@code WearableMessage} form of a file transfer, or null when the item is an
     * ordinary published value rather than a transfer.
     *
     * @param context any context
     * @param item the received data item
     * @return the encoded payload, or null
     */
    static byte[] decodeTransfer(Context context, DataItem item) {
        try {
            DataMap map = DataMapItem.fromDataItem(item).getDataMap();
            Asset asset = map.getAsset("asset");
            if (asset == null) {
                return null;
            }
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
            return new WearableMessage(item.getUri().getPath())
                    .put("name", map.getString("name", "file"))
                    .put("contents", out.toByteArray())
                    .toByteArray();
        } catch (Throwable notATransfer) {
            return null;
        }
    }

    // --- paths --------------------------------------------------------------

    static String dataPath(String path) {
        return PATH_PREFIX + encode(path);
    }

    /**
     * Data Layer paths allow a restricted character set and are matched by prefix, so a Codename One
     * path is percent-escaped into it and unescaped on the way back.
     */
    static String encode(String path) {
        if (path == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '/' || c == '-' || c == '_') {
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
