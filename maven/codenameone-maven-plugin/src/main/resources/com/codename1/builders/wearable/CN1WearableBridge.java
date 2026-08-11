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

import com.google.android.gms.tasks.OnFailureListener;
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
import java.util.LinkedHashMap;
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
    /**
     * The capability this app advertises to say the counterpart is installed, declared in
     * res/values/cn1_wearable.xml by the build. Named, rather than repeated as a literal, because
     * the manifest filters CAPABILITY_CHANGED on the "/cn1" path prefix and Play services matches a
     * capability name as the path -- so an app that declares any other capability starting with
     * "cn1" is delivered here too, and the name is what tells the two apart.
     */
    static final String CAPABILITY_NAME = "cn1_wearable";
    /** The key the payload bytes live under inside a DataItem. */
    private static final String PAYLOAD_KEY = "cn1.payload";
    /** The publication order of a value or transfer, so the newer of two items wins. */
    private static final String SEQUENCE_KEY = "cn1.seq";
    /**
     * When an item was published, in wall-clock millis.
     *
     * <p>Separate from {@link #SEQUENCE_KEY} because the sequence is a logical clock: once this
     * device has observed a peer running ahead, a sequence no longer corresponds to a time at all,
     * and the transfer sweep -- which is genuinely about age -- would keep items until local time
     * happened to reach the borrowed value.
     */
    private static final String PUBLISHED_AT_KEY = "cn1.at";
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
    /**
     * Bumped on every write to the node cache, for the same reason as {@link #bondedGeneration}: an
     * in-flight refresh must not overwrite a pushed onPeerConnected/Disconnected update with an
     * older snapshot and stamp it fresh, which would leave isReachable() wrong until the cache
     * expired.
     */
    private final Object nodesLock = new Object();
    private long nodesGeneration;

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
        restoreClock(this.context);
        // Anything a service process wrote down while the app was away, replayed before any live
        // event of this run -- a one-shot message and a data removal have no other way back. First,
        // because these are older than whatever arrives next and the app should see them in order.
        drainSpool(this.context);
        // And re-derive the logical clock from what is actually published.
        //
        // The stored floor can be missing: a refused commit leaves nothing on disk, and the retry
        // only happens on the next observation. Restored from an older floor, the first putData of
        // this run could stamp a sequence below a peer item that is still there, and
        // deliverIfOutranks then ranks our own update stale and drops it with no error. The
        // published items are themselves durable, so reading them says what the disk could not.
        reconcileClockFromPublishedItems();
        // Sweep at startup as well as after each publish. An app that sends a few files and then
        // stops would otherwise never run the sweep again, leaving its last transfers published
        // indefinitely -- the post-publish sweep only helps an app that keeps transferring.
        expireOwnTransfers();
        // The receiver's durable claims are pruned by the replay itself, once it has succeeded --
        // NOT here. Pruning first deleted an aged claim before the replay could see that its item
        // is still published, and the replay then delivered that one-shot file a second time.
        // Ordering matters more than promptness: the claim store is bounded by the periodic prune
        // as well, so deferring it costs nothing.
        replayOutstandingTransfers();
        // What to do when the pending-delivery cap has to discard one of our callbacks: forget that
        // the path was delivered and resolve it again. Without forgetting, every replay skips it --
        // the stamp is recorded before the delivery is queued, so the path looks delivered even
        // though nothing ran. Runs after the drain, so the re-offer meets a listener.
        WearableConnection.setDroppedDeliveryHandler(
                new WearableConnection.DroppedDeliveryHandler() {
                    public void deliveryDropped(String path) {
                        if (path == null) {
                            // More was discarded than could be named. Forget every delivery this
                            // process has recorded and re-enumerate: the replay then treats each
                            // still-published path as first sight and offers it again.
                            forgetAllDeliveredSequences();
                            replayOutstandingTransfers();
                            return;
                        }
                        // Only value changes arrive here; core re-announces a discarded removal
                        // itself, which is the only thing that can -- a deleted item is absent from
                        // every enumeration by definition.
                        forgetDeliveredSequence(path);
                        scheduleWinnerResolution(context, path);
                    }
                });
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
        // has never run this app is invisible to both queries because Android exposes no such list;
        // that limit is stated in the public contract rather than papered over here.
        // Cold start: both caches are empty until the first query completes, and a latency-sensitive
        // caller cannot be made to wait for it, so the honest answer here is "not known yet" and
        // the only value this signature can carry for that is false.
        //
        // An earlier attempt to paper over it -- remembering that a peer had once been seen -- was
        // no help at all, because on a cold start nothing has been seen yet; that is the whole
        // situation. What actually closes the window is the refresh completing: it sets
        // bondedQueryCompleted, and when the answer CHANGES it fires the state listeners. So the
        // contract tells callers to decide from a WearableStateListener rather than from one call
        // at startup, and this stays a plain report of what is currently known.
        return !connectedNodes().isEmpty() || !bondedNodeIds().isEmpty();
    }

    /// True once the capability query has actually answered, so an empty cache can be told apart
    /// from one that has not been filled yet. Guarded by the bonded cache's monitor.
    private volatile boolean bondedQueryCompleted;

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
        // The local node is checked again first: a peer snapshot can never contain this device, so
        // rejecting on "populated but no match" would discard our own echoed putData() whenever any
        // peer happens to be connected and the earlier getLocalNode() failed transiently.
        if (sourceNodeId.equals(localNodeId(context))) {
            return true;
        }
        // The capability set is consulted BEFORE this rejection, not after it. With several paired
        // watches, a durable item from a disconnected watch A can arrive while watch B is online:
        // the snapshot is then populated and simply does not contain A, so treating "populated but
        // absent" as evidence dropped an event from a genuinely paired device. Connectivity says
        // who is reachable now; the capability set says who is paired and running this app, and for
        // a stored item that is the question.
        boolean capabilityQueried = false;
        List<String> capable = capabilityNodeIds(context);
        if (capable != null) {
            capabilityQueried = true;
            for (String id : capable) {
                rememberNode(id);
            }
        }
        if (recentlySeen(sourceNodeId)) {
            return true;
        }
        if (!connected.isEmpty() && capabilityQueried) {
            // Reachable peers exist, the sender is not among them, and it advertises no capability
            // either. That is real evidence against it.
            //
            // Gated on the capability query having actually ANSWERED. With several paired watches,
            // a durable item from disconnected watch A arriving while watch B is online leaves the
            // connected snapshot populated -- so a transient failure of the one query that can see
            // A would have been read as evidence against A and rejected it outright, and an
            // unchanged item may raise no later callback to put that right.
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
            // Re-attempt the CAPABILITY set as well, not just the connected nodes. It is the only
            // query that can see a paired-but-disconnected sender, so a single transient failure of
            // it on a cold start left the item permanently unverifiable: retrying connected nodes
            // can never rediscover a node that is not connected, and the unchanged item is then
            // discarded with no later callback guaranteed.
            List<String> retryCapable = capabilityNodeIds(context);
            if (retryCapable != null) {
                for (String id : retryCapable) {
                    rememberNode(id);
                }
            }
            // Re-attempt the local identity too. A peer query can never return this device, so if
            // getLocalNode() failed transiently on the first pass, an event from our OWN putData()
            // -- which the Data Layer echoes back with the local node as host -- would be rejected
            // no matter how many times we asked about peers.
            if (recentlySeen(sourceNodeId) || sourceNodeId.equals(localNodeId(context))) {
                return true;
            }
        }
        // The capability set was already consulted above, before the populated-snapshot rejection.
        return recentlySeen(sourceNodeId);
    }

    /// Ids of the nodes advertising this app's capability, reachable or not. Blocking.
    /// Null when the query FAILED, which is not the same as a peer set that is genuinely empty --
    /// the caller uses the difference to decide whether a populated connected snapshot is evidence.
    private static List<String> capabilityNodeIds(Context context) {
        List<String> out = new ArrayList<String>();
        try {
            CapabilityInfo info = Tasks.await(
                    Wearable.getCapabilityClient(context.getApplicationContext())
                            .getCapability(CAPABILITY_NAME, CapabilityClient.FILTER_ALL),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (info != null) {
                for (Node n : info.getNodes()) {
                    out.add(n.getId());
                }
            }
        } catch (Throwable unavailable) {
            // Nothing established. Reported as null so the caller can tell "could not ask" from
            // "asked, and this node is not paired".
            return null;
        }
        return out;
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

    /**
     * Whether a data item was published by THIS device.
     *
     * <p>Play services echoes an app's own {@code putData}/{@code removeData} back to it, with the
     * local node as the item's authority. Those echoes have to stay visible to reads and to the
     * ordering bookkeeping -- they are genuinely the current value of the path -- but they are not
     * peer events, and {@code WearableDataListener} documents its callbacks as peer changes. iOS and
     * the simulator already suppress self-authored changes, so forwarding them made identical app
     * code fire an extra callback on Android only, and an app that acts on a change would process
     * its own write twice.
     *
     * @param context any context
     * @param host the item Uri's authority
     * @return true when the item came from this device
     */
    /**
     * Last known value per path, and the last successful path enumeration.
     *
     * <p>Exists so a read from a latency-sensitive thread has something truthful to answer with.
     * getData/getDataPaths reach Play services through a blocking await, and on the EDT that is up
     * to five seconds of frozen painting and input -- the same stall the state queries in this
     * class already refuse to take. Bounded like the other caches; a value falling out only costs
     * an EDT caller a null it would otherwise have blocked five seconds for.</p>
     */
    private static final int VALUE_CACHE_MAX = 256;
    private static final Map<String, byte[]> valueCache =
            new LinkedHashMap<String, byte[]>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > VALUE_CACHE_MAX;
                }
            };
    private static volatile String[] pathsCache;
    /// Bumped whenever the path snapshot changes, so a blocking enumeration can tell whether a
    /// delivery maintained it while the query was in flight. Guarded by {@link #valueCache}.
    private static int pathsGeneration;

    /**
     * Records what a delivery or a successful read saw, so a later EDT caller can be answered
     * without blocking.
     *
     * <p>Maintains the path snapshot too. An enumeration-only {@code pathsCache} went stale the
     * moment a peer published or removed anything and stayed that way until some background caller
     * happened to enumerate again -- while the listener had already been told enough to keep it
     * right. A path appearing or disappearing is exactly what these calls report.</p>
     */
    static void rememberValue(String path, byte[] payload) {
        if (path == null) {
            return;
        }
        synchronized (valueCache) {
            // Any authoritative answer supersedes a remembered absence, in both directions: a
            // publication makes the path exist, and a removal is itself the absence.
            if (payload == null) {
                knownAbsent.add(path);
                valueCache.remove(path);
            } else {
                knownAbsent.remove(path);
                // A copy, because the same array is handed to the application. A listener that
                // mutates the payload it receives -- decrypting or unpacking in place, say -- would
                // otherwise rewrite the bridge's own snapshot, and getData() would answer with
                // bytes nobody ever published until the next event refreshed the path.
                valueCache.put(path, payload.clone());
            }
            String[] known = pathsCache;
            if (known == null) {
                // No enumeration has succeeded yet, so there is no snapshot to keep consistent;
                // inventing a one-element one would claim this is the only path that exists.
                //
                // The GENERATION still moves. An enumeration already in flight may have captured
                // the Data Layer before this change, and the generation is the only thing that
                // tells it so -- leaving it untouched let that query install a snapshot missing a
                // path just published, or still holding one just removed, and a latency-sensitive
                // getDataPaths() would answer from it indefinitely because the callback that would
                // have corrected it has already fired.
                pathsGeneration++;
                return;
            }
            boolean present = false;
            for (String p : known) {
                if (path.equals(p)) {
                    present = true;
                    break;
                }
            }
            if (payload == null && present) {
                List<String> out = new ArrayList<String>(known.length);
                for (String p : known) {
                    if (!path.equals(p)) {
                        out.add(p);
                    }
                }
                pathsCache = out.toArray(new String[out.size()]);
                pathsGeneration++;
            } else if (payload != null && !present) {
                String[] out = new String[known.length + 1];
                System.arraycopy(known, 0, out, 0, known.length);
                out[known.length] = path;
                pathsCache = out;
                pathsGeneration++;
            }
        }
    }

    private static byte[] cachedValue(String path) {
        synchronized (valueCache) {
            byte[] cached = valueCache.get(path);
            // Also a copy on the way out: the caller owns what getData() returns and may do as it
            // likes with it, which must not reach back into the snapshot.
            return cached == null ? null : cached.clone();
        }
    }

    static boolean isLocallyAuthored(Context context, String host) {
        if (host == null) {
            return false;
        }
        String local = localNodeId(context);
        return local != null && local.equals(host);
    }

    /**
     * Whether the calling thread must not be blocked.
     *
     * <p>The Codename One EDT is the obvious one. Android's main thread matters just as much and was
     * missed: Play services completion listeners run there unless given an executor, so a blocking
     * Data Layer call reached from one is an ANR rather than a dropped frame.
     *
     * @return true when the caller needs an immediate answer
     */
    private static boolean isCallerLatencySensitive() {
        if (com.codename1.ui.CN.isEdt()) {
            return true;
        }
        try {
            return android.os.Looper.myLooper() == android.os.Looper.getMainLooper();
        } catch (Throwable notOnAndroidThread) {
            return false;
        }
    }

    /// Nodes the Data Layer knows about whether or not they are currently connected.
    private List<String> bondedNodeIds() {
        if (bondedStamp != 0 && System.currentTimeMillis() - bondedStamp <= NODE_CACHE_MILLIS) {
            // Honour the cache lifetime on every thread. Refreshing on each EDT call would make a
            // state listener that calls isPaired() or isReachable() start another refresh, whose
            // completion notifies listeners again -- a self-sustaining loop.
            return cachedBonded;
        }
        if (isCallerLatencySensitive()) {
            // Never block the EDT -- or Android's main thread, which is where a Play services
            // completion listener runs by default: fanOut() reaches here from the send-time refresh
            // callback, and a five-second Tasks.await() there is an ANR, not a slow frame.
            //
            // The cache still has to be filled by someone, or an installed companion is reported
            // absent forever. Kick off a refresh and answer with what is known so far; listeners
            // are notified only when the answer actually changed.
            refreshBondedAsync();
            return cachedBonded;
        }
        final long startedAt;
        synchronized (bondedLock) {
            startedAt = bondedGeneration;
        }
        List<String> out = new ArrayList<String>();
        try {
            CapabilityInfo info = Tasks.await(
                    capabilityClient.getCapability(CAPABILITY_NAME, CapabilityClient.FILTER_ALL),
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
        synchronized (bondedLock) {
            if (bondedGeneration != startedAt) {
                // A pushed onCapabilityChanged landed while this blocking query was out. It is more
                // current than anything we asked for, so keep it rather than restoring the older
                // answer and stamping it fresh.
                return cachedBonded;
            }
            cachedBonded = out;
            bondedStamp = System.currentTimeMillis();
            bondedQueryCompleted = true;
            bondedKnown = true;
            bondedGeneration++;
        }
        return out;
    }

    private volatile List<String> cachedBonded = new ArrayList<String>();
    private volatile boolean refreshingBonded;
    private volatile long bondedStamp;
    /**
     * Whether a capability query has ever completed. An empty {@link #cachedBonded} is ambiguous
     * without it -- "not asked yet" and "asked, nobody runs the app" are opposite answers for
     * {@link #fanOut}, and treating the second as the first sends to a watch that cannot receive.
     */
    private volatile boolean bondedKnown;
    /** Guards the {@link #cachedBonded} / {@link #bondedKnown} pair so they can be read together. */
    private final Object bondedLock = new Object();
    /**
     * Bumped on every write to the capability cache.
     *
     * <p>An in-flight refresh and a pushed {@code onCapabilityChanged} can complete in either order.
     * Without a version the older query result lands last, overwrites the newer pushed set AND gets
     * a fresh timestamp -- so an install or removal that Play services told us about directly is
     * discarded and the wrong answer is held for a full cache lifetime.
     */
    private long bondedGeneration;

    /**
     * The capability cache read as ONE value.
     *
     * <p>Two independent volatile reads cannot express "these belong together": reading the flag
     * first let a completed query leave a true flag beside a stale empty list, and reading the list
     * first let a query that completes during the read leave a populated list beside a false flag --
     * so the filter was skipped even though the answer was known. Both fields are written and read
     * under one lock instead, so a caller always sees a consistent pair.
     *
     * @return the snapshot; {@code known} false means no query has completed yet
     */
    private BondedSnapshot bondedSnapshot() {
        // Take the list outside the lock: bondedNodeIds() may block on Play services, and holding
        // the lock across that would stall every other reader.
        List<String> ids = bondedNodeIds();
        synchronized (bondedLock) {
            return new BondedSnapshot(bondedKnown, bondedKnown ? cachedBonded : ids);
        }
    }

    /** A consistent view of the capability cache. */
    private static final class BondedSnapshot {
        final boolean known;
        final List<String> ids;

        BondedSnapshot(boolean known, List<String> ids) {
            this.known = known;
            this.ids = ids;
        }
    }

    /// Accepts a capability set pushed by Play services, so the cache tracks an install or
    /// uninstall that happens while the device stays connected.
    static void capabilityChanged(CapabilityInfo info) {
        if (info != null && !CAPABILITY_NAME.equals(info.getName())) {
            // Another of the app's capabilities whose name also begins with "cn1" (the manifest
            // filters on that prefix and the capability name IS the path). Its node set says
            // nothing about whether the counterpart app is installed, so adopting it would corrupt
            // the cache behind isCompanionAppInstalled(); and no state of ours changed, so this
            // must not notify either.
            return;
        }
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
        boolean changed;
        synchronized (b.bondedLock) {
            changed = !sameIds(b.cachedBonded, out);
            b.cachedBonded = out;
            b.bondedStamp = System.currentTimeMillis();
            b.bondedQueryCompleted = true;
            b.bondedKnown = true;
            b.bondedGeneration++;
        }
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
        synchronized (b.nodesLock) {
            b.applyPeerChange(peer, connected);
        }
        // A disconnect can also mean the capability set shrank; let that refresh on its own clock.
        WearableConnection.notifyStateChanged();
    }

    /// Applies a pushed peer change. Must hold {@link #nodesLock}: copying the cache outside it let
    /// a refresh complete in between, after which this rebuilt the list from the OLD snapshot and
    /// stamped it fresh -- dropping whatever peers that refresh had just discovered.
    private void applyPeerChange(Node peer, boolean connected) {
        List<Node> updated = new ArrayList<Node>(cachedNodes);
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
        cachedNodes = updated;
        // Keep the stamp: this is a push from Play services, which is more current than any query
        // we could make, so there is nothing to re-ask. A zero stamp would also make the next
        // sendMessage() defer needlessly. Bumping the generation is what stops an in-flight refresh
        // from undoing this.
        cachedNodesStamp = System.currentTimeMillis();
        nodesGeneration++;
    }

    /// The live bridge, so the listener service can push state into it. The service and the bridge
    /// are created independently by Android, which is why this is not a constructor argument.
    private static volatile CN1WearableBridge current;

    private void refreshBondedAsync() {
        if (refreshingBonded) {
            return;
        }
        refreshingBonded = true;
        final long startedAt;
        synchronized (bondedLock) {
            startedAt = bondedGeneration;
        }
        capabilityClient.getCapability(CAPABILITY_NAME, CapabilityClient.FILTER_ALL)
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
                        boolean changed;
                        synchronized (bondedLock) {
                            if (bondedGeneration != startedAt) {
                                // Something newer landed while this query was in flight -- typically
                                // a pushed onCapabilityChanged, which is more current than anything
                                // we could have asked for. Discard this result rather than reviving
                                // a pre-install/pre-removal answer and stamping it fresh.
                                refreshingBonded = false;
                                return;
                            }
                            changed = !sameIds(cachedBonded, out);
                            cachedBonded = out;
                            bondedStamp = System.currentTimeMillis();
                            bondedQueryCompleted = true;
                            bondedKnown = true;
                            bondedGeneration++;
                        }
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
            if (isCallerLatencySensitive()) {
                refreshNodesAsync();
            } else {
                refreshNodesNow();
            }
        }
        return cachedNodes;
    }

    private void refreshNodesNow() {
        final long startedAt;
        synchronized (nodesLock) {
            startedAt = nodesGeneration;
        }
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
        synchronized (nodesLock) {
            if (nodesGeneration != startedAt) {
                // A pushed peer connect/disconnect landed while this blocking query was out. Keep
                // it: a push is more current than anything we could have asked for.
                return;
            }
            cachedNodes = fresh;
            cachedNodesStamp = System.currentTimeMillis();
            nodesGeneration++;
        }
        rememberAll(cachedNodes);
    }

    private void refreshNodesAsync() {
        if (refreshingNodes) {
            return;
        }
        refreshingNodes = true;
        final long nodesStartedAt;
        synchronized (nodesLock) {
            nodesStartedAt = nodesGeneration;
        }
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
                        boolean changed;
                        synchronized (nodesLock) {
                            if (nodesGeneration != nodesStartedAt) {
                                // A pushed peer connect/disconnect landed while this was in flight.
                                refreshingNodes = false;
                                return;
                            }
                            changed = !sameIds(idsOf(cachedNodes), idsOf(fresh));
                            cachedNodes = fresh;
                            cachedNodesStamp = System.currentTimeMillis();
                            nodesGeneration++;
                        }
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
            final long sendStartedAt;
            synchronized (nodesLock) {
                sendStartedAt = nodesGeneration;
            }
            nodeClient.getConnectedNodes().addOnCompleteListener(
                    new com.google.android.gms.tasks.OnCompleteListener<List<Node>>() {
                        public void onComplete(com.google.android.gms.tasks.Task<List<Node>> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                synchronized (nodesLock) {
                                    // A pushed peer connect/disconnect that landed while this query
                                    // was out is more current than the query; keep it and fan out
                                    // against it rather than reviving the older snapshot.
                                    if (nodesGeneration == sendStartedAt) {
                                        cachedNodes = task.getResult();
                                        cachedNodesStamp = System.currentTimeMillis();
                                        nodesGeneration++;
                                    }
                                }
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
        // Prefer nodes that advertise the app capability, so a connected watch WITHOUT this app is
        // not counted as a recipient -- otherwise a reply-bearing request "succeeds" against a watch
        // that cannot answer and the caller waits out the full timeout instead of being told there
        // is nobody to ask. This also stops fanOut and isReachable() disagreeing.
        //
        // Only once a capability query has actually completed. Before that an empty set means "not
        // asked yet", not "nobody runs the app", and refusing to send on it would break the first
        // send after a cold start -- so bondedKnown, not emptiness, is what gates the filter.
        // One consistent (known, ids) pair -- see bondedSnapshot(). Sampling the two fields
        // independently is wrong in both orders: flag-then-list can pair a true flag with a stale
        // empty list (filtering out every node, so the send reaches nobody), and list-then-flag can
        // pair a populated list with a false flag (skipping the filter although the answer is
        // known, so a send goes to a watch without the app).
        BondedSnapshot bonded = bondedSnapshot();
        for (Node n : nodes) {
            if (!n.isNearby()) {
                continue;
            }
            if (bonded.known && !bonded.ids.contains(n.getId())) {
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
            pruneInboundOrigins();
            int local = nextLocalToken++;
            inboundNodes.put(Integer.valueOf(local), new InboundRequest(nodeId, peerToken));
            // Also swept on a timer. Pruning only here meant a FINAL request -- or a final burst --
            // to an app that never answers kept its origin records for the rest of the process,
            // long after every sender had timed out. A TTL that needs future traffic to take effect
            // is not a TTL.
            scheduleInboundPrune();
            return local;
        }
    }

    /// Drops origins older than the TTL. Caller holds {@link #inboundNodes}.
    private static void pruneInboundOrigins() {
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
    }

    private static boolean inboundPruneScheduled;

    /// Arms one prune, and only one: the task re-arms itself while anything is still remembered, so
    /// a burst of requests does not queue a task each. Caller holds {@link #inboundNodes}.
    private static void scheduleInboundPrune() {
        if (inboundPruneScheduled || inboundNodes.isEmpty()) {
            return;
        }
        inboundPruneScheduled = true;
        replyTimer.schedule(new java.util.TimerTask() {
            public void run() {
                synchronized (inboundNodes) {
                    inboundPruneScheduled = false;
                    pruneInboundOrigins();
                    scheduleInboundPrune();
                }
            }
        }, INBOUND_TTL_MILLIS + 1000L);
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
        final String p = path;
        final byte[] body = payload == null ? new byte[0] : payload;
        // OFF the caller's thread, because the caller is usually the EDT.
        //
        // Allocating the sequence advances the logical-clock floor, and the floor is written with a
        // synchronous commit() -- so an ordinary UI-triggered putData blocked rendering and input on
        // storage I/O for as long as that took. The whole publication moves to the transfer worker,
        // which keeps the two things that matter: the commit still completes before the item is
        // published, and the worker is single-threaded, so two publications keep the order their
        // callers made them in -- which is what their sequence stamps are supposed to record.
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                // The payload travels inside a DataMap rather than as the item's raw data so it can
                // be stamped with a publication sequence. Both halves of a pair may publish the
                // same logical path, which the Data Layer stores as two items under two node
                // authorities; without an ordering stamp a reader has no way to tell which of them
                // is the newer value.
                PutDataMapRequest req = PutDataMapRequest.create(dataPath(p));
                req.getDataMap().putByteArray(PAYLOAD_KEY, body);
                req.getDataMap().putLong(SEQUENCE_KEY, nextSequence());
                // Urgent: without it the system may sit on the change for minutes, which reads as
                // "my watch never updated" even though the API did its job.
                dataClient.putDataItem(req.asPutDataRequest().setUrgent());
            }
        }, 0);
    }

    /**
     * A monotonic publication stamp. Wall-clock millis order correctly against the peer's stamps
     * (both devices' clocks are network-synced within far less than a replication round trip), and
     * the counter breaks ties between two puts inside the same millisecond on this device.
     */
    private static synchronized long nextSequence() {
        long now = System.currentTimeMillis();
        lastSequence = now > lastSequence ? now : lastSequence + 1;
        persistClock(lastSequence);
        return lastSequence;
    }

    /** Preference store for the logical clock; see {@link #persistClock}. */
    private static final String CLOCK_PREFS = "cn1.wearable";
    private static final String CLOCK_KEY = "clock";
    private static volatile long persistedClock;

    /**
     * Remembers the clock floor across process restarts.
     *
     * <p>Once this device has observed a peer sequence ahead of its own wall clock, that floor is
     * the only thing keeping its next publish above the peer's existing item. Holding it in a static
     * field alone means a restart drops back to local time and publishes something the peer will
     * correctly judge older -- silently losing the write.
     *
     * @param value the clock value to remember
     */
    private static void persistClock(long value) {
        CN1WearableBridge b = current;
        // The listener's context stands in when no bridge exists. A peer item can wake the service
        // alone, and returning early there left the observation in static memory only: if Android
        // then refused the background activity launch and killed the process, the next launch
        // restored the OLDER floor, and an immediate local publication could draw a sequence below
        // the peer item that is still published -- silently losing as the older replica.
        Context c = b != null ? b.context : serviceContext;
        if (c == null || value <= persistedClock) {
            return;
        }
        // persistedClock is NOT advanced yet. It records what is on DISK, and moving it before the
        // write meant a refused commit was remembered as a success: every later call saw
        // value <= persistedClock, returned early, and the floor never reached disk at all. The
        // next process then restored the older floor and published a sequence below a peer item
        // that is still there, which deliverIfOutranks ranks stale and drops without a callback.
        try {
            // commit(), not apply(). This floor is the promise that no future publication of ours
            // will draw a sequence at or below one we have already SEEN from a peer. apply() writes
            // in the background, so a process death between observing an ahead-of-wall-clock peer
            // sequence and the flush restored the older floor on restart -- the next publication
            // then drew a sequence below the peer item that is still published, deliverIfOutranks
            // ranked it stale, and the value was dropped with no callback and no error.
            //
            // Runs off the main thread (the Data Layer workers), and only when the floor actually
            // moves, which is rare: the blocking write is bounded and not on any UI path.
            if (c.getSharedPreferences(CLOCK_PREFS, Context.MODE_PRIVATE)
                    .edit().putLong(CLOCK_KEY, value).commit()) {
                persistedClock = value;
                return;
            }
        } catch (Throwable unavailable) {
            // Falls through to the retry below, which is the same situation: nothing on disk.
        }
        // A store that is full or momentarily unwritable is the case this exists for, so a single
        // refused write must not be the end of it. persistedClock stays where it was, so the next
        // observation or publication tries again -- and there is always a next one before the floor
        // matters, because the floor only matters when this device publishes.
        android.util.Log.w("CN1Wearable", "the wearable logical-clock floor " + value
                + " was not written; it will be retried on the next observation");
    }

    /// Raises the logical clock to match the highest sequence currently published, in the
    /// background.
    ///
    /// Off the calling thread because it is a Data Layer round trip, and at startup because that is
    /// the only moment the in-memory clock can be BEHIND what this device itself published -- every
    /// later read raises it through sequenceOf as a matter of course.
    private void reconcileClockFromPublishedItems() {
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                try {
                    DataItemBuffer items = Tasks.await(dataClient.getDataItems(),
                            TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    try {
                        for (DataItem item : items) {
                            // sequenceOf observes as it reads, which is the whole point: one pass
                            // over what exists puts the clock above all of it.
                            sequenceOf(valueMap(item));
                        }
                    } finally {
                        items.release();
                    }
                } catch (Throwable unavailable) {
                    // Best effort. The wall-clock seed still orders the common case, and the next
                    // item this device reads raises the clock anyway.
                }
            }
        }, 0);
    }

    /// A context for a cold service process, where the clock still has to be durable.
    private static volatile Context serviceContext;

    /// Called by the listener service before it handles anything, so a process that never starts an
    /// activity can still restore and persist the logical clock.
    /// Brings the app process up because a callback is about to be handed to WearableConnection.
    ///
    /// Lives here rather than only in the listener service because dispatch does not: a resolved
    /// removal and a retried asset are both announced from this class, outside every launch site
    /// the service owns. Queueing a callback into a process with no listener is only half a
    /// delivery -- nothing drains the queue, and a service process that dies before the user opens
    /// the app loses it, with a deleted DataItem absent from startup replay and a transfer bounded
    /// by the sender's retention.
    ///
    /// A no-op once the app is up, which is the common case: the checks below are a field read and
    /// a package-manager lookup.
    static void ensureAppRunning() {
        Context c = serviceContext;
        if (c == null) {
            CN1WearableBridge b = current;
            c = b == null ? null : b.context;
        }
        if (c == null) {
            return;
        }
        try {
            if (com.codename1.ui.Display.isInitialized()) {
                return;
            }
            android.content.Intent launch = c.getPackageManager()
                    .getLaunchIntentForPackage(c.getApplicationInfo().packageName);
            if (launch != null) {
                launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(launch);
            }
        } catch (Throwable notPermitted) {
            // Background activity starts are restricted on newer Android. Nothing is lost by that
            // any more: the two deliveries that cannot be reconstructed later are spooled to disk
            // before this is even attempted -- see spoolOrDeliverMessage / spoolOrDeliverRemoval.
        }
    }

    // ==================================================================
    // the durable spool
    // ==================================================================

    /// Deliveries that survive this process, because nothing else can reconstruct them.
    ///
    /// The in-memory queue in WearableConnection is the right home for a delivery that arrives
    /// before a listener registers -- while the process lives. It is the wrong home for one that
    /// arrives in a service process the OS then reclaims, and two kinds cannot be recovered
    /// afterwards:
    ///
    /// - a one-shot message, which the Data Layer does not retain at all, and
    /// - a data REMOVAL, whose item is by definition gone, so startup replay sees nothing to report.
    ///
    /// A replicated value needs none of this: the item is still published and startup replay finds
    /// it. Nor does a reply-bearing request -- an answer produced after the peer's call has timed
    /// out is not an answer -- so those keep going straight to the in-memory queue.
    ///
    /// Depending on the activity launch to bridge the gap was the bug: Android 10+ refuses a
    /// background start, and the catch above then left the delivery in memory with nothing to
    /// drain it.
    private static final String SPOOL_PREFS = "cn1_wearable_spool";

    private static final String SPOOL_SEQ_KEY = "seq";

    /// Enough to absorb a burst while the app is away; past this the OLDEST go, because a spool
    /// that grows without bound is its own failure and the newest state is the more useful.
    private static final int SPOOL_MAX_ENTRIES = 256;

    /// Guards sequence allocation, trimming, commit and drain together.
    ///
    /// Not decoration: the message worker and the data worker are different threads, and two of
    /// them reading the same seq before either commits produced the same key -- the later write
    /// then overwrote the earlier record and lost a delivery that has no other copy anywhere. A
    /// SharedPreferences editor is not a transaction, so the read-modify-write has to be one.
    private static final Object SPOOL_LOCK = new Object();

    private static final String SPOOL_MESSAGE = "m";

    private static final String SPOOL_REMOVAL = "r";

    /// Hands a one-shot message to a live listener, or writes it down for the next process.
    static void spoolOrDeliverMessage(Context context, String path, byte[] payload) {
        if (deliverableNow(context, true)) {
            WearableConnection.deliverMessage(path, payload, 0);
            return;
        }
        if (!spool(context, SPOOL_MESSAGE, path, payload)) {
            // The spool is the durable half; if it could not be written, the in-memory queue is
            // still better than dropping the message outright.
            WearableConnection.deliverMessage(path, payload, 0);
            return;
        }
        // Whoever writes tries to drain. If an owner already holds the walk this returns at once,
        // and the dirty flag raised above is what makes that owner come back for this record.
        drainSpool(context);
    }

    /// Hands a reply-bearing request to a live listener, or writes the PAYLOAD down for later.
    ///
    /// The answer cannot be saved for later -- the peer waits out a timeout measured in seconds and
    /// is long gone by the next launch -- but the request itself is still information the app asked
    /// to receive, and dropping it was the outcome before this. On Android 10+ the background
    /// activity start is refused, so a cold-start request went into a process-local queue that
    /// nothing would ever drain, and the app never learned it had been asked.
    ///
    /// So: delivered as a request while the app can answer, and spooled as an ordinary one-shot
    /// message when it cannot. The listener then sees it on the next launch with `expectsReply`
    /// false, which is exactly what it means -- nobody is waiting any more.
    ///
    /// The local token is allocated only on the live path. Trading the peer's token for one of ours
    /// records an origin that would never be answered, and the reply-timeout bookkeeping that goes
    /// with it has nothing to cancel.
    static void spoolOrDeliverRequest(Context context, String path, byte[] payload,
            int peerToken, String sourceNodeId) {
        if (deliverableNow(context, true)) {
            // The peer's token is unique only on the peer, so trade it for a locally unique one
            // keyed to the node that asked; two watches can otherwise pick the same number.
            int localToken = rememberRequestOrigin(peerToken, sourceNodeId);
            WearableConnection.deliverMessage(path, payload, localToken);
            return;
        }
        android.util.Log.w("CN1Wearable", "no listener can answer the request on " + path
                + " right now; spooling it as a plain message, and the peer will time out");
        if (!spool(context, SPOOL_MESSAGE, path, payload)) {
            int localToken = rememberRequestOrigin(peerToken, sourceNodeId);
            WearableConnection.deliverMessage(path, payload, localToken);
            return;
        }
        drainSpool(context);
    }

    /// The same for a removal, whose item no longer exists to be replayed from.
    static void spoolOrDeliverRemoval(Context context, String path) {
        if (deliverableNow(context, false)) {
            WearableConnection.deliverDataRemoved(path);
            return;
        }
        if (!spool(context, SPOOL_REMOVAL, path, null)) {
            WearableConnection.deliverDataRemoved(path);
            return;
        }
        drainSpool(context);
    }

    /// Whether a LISTENER can take this right now, and nothing older is waiting to be replayed.
    ///
    /// `Display.isInitialized()` was the whole test, and it answers a different question. Between
    /// initialization and the app's addMessageListener call the in-memory queue holds payloads
    /// nothing has received -- and a process killed in that window loses a one-shot message the
    /// Data Layer does not retain and a removal whose item is already gone. The spool is the thing
    /// that survives it, so the bar for skipping the spool is someone actually being there.
    ///
    /// @param wantsMessageListener true for a message or a request, false for a data removal
    private static boolean deliverableNow(Context context, boolean wantsMessageListener) {
        try {
            if (!com.codename1.ui.Display.isInitialized()) {
                return false;
            }
            if (wantsMessageListener) {
                if (!WearableConnection.hasMessageListener()) {
                    return false;
                }
            } else if (!WearableConnection.hasDataListener()) {
                return false;
            }
        } catch (Throwable notInitialized) {
            return false;
        }
        // Initialized, so anything already spooled has to go FIRST -- otherwise a delivery written
        // moments ago would arrive after one that happened later. Returns at once if another worker
        // already owns the drain.
        drainSpool(context);
        // And a live event does NOT overtake that drain. Answering true while an owner was midway
        // through the store let the message worker hand the app a NEW event before the data worker
        // had queued an older spooled one -- the reversal the single owner was supposed to end,
        // arriving through the other door. Saying no here spools this event instead, so it takes a
        // later key than everything outstanding and the owner replays it in order.
        return !spoolBusy();
    }

    /// Whether a drain is running or any claimed record has yet to be confirmed.
    ///
    /// In-flight records count: one parked for want of a listener has not reached the app, and a
    /// live event delivered past it would arrive first.
    private static boolean spoolBusy() {
        synchronized (SPOOL_LOCK) {
            return spoolDraining || !SPOOL_IN_FLIGHT.isEmpty();
        }
    }

    private static boolean spool(Context context, String kind, String path, byte[] payload) {
        Context c = spoolContext(context);
        if (c == null) {
            return false;
        }
        synchronized (SPOOL_LOCK) {
            try {
                android.content.SharedPreferences prefs =
                        c.getSharedPreferences(SPOOL_PREFS, Context.MODE_PRIVATE);
                long seq = prefs.getLong(SPOOL_SEQ_KEY, 0) + 1;
                android.content.SharedPreferences.Editor edit = prefs.edit();
                edit.putLong(SPOOL_SEQ_KEY, seq);
                // Zero-padded, because the drain replays in key order and the whole point is that
                // a message arrives in the order it was sent.
                //
                // The leading 0 is the attempt count. Written explicitly rather than left to be
                // inferred, so the record has one shape everywhere and no reader has to guess
                // whether the first field is a count or a kind.
                edit.putString(spoolKey(seq), "0|" + kind + "|" + encode(path) + "|"
                        + (payload == null ? "" : android.util.Base64.encodeToString(
                                payload, android.util.Base64.NO_WRAP)));
                trimSpool(prefs, edit);
                // Under the lock, and before the commit: an owner finishing its last pass has to
                // see this rather than stop with the record unread.
                spoolDirty = true;
                // commit(), and its result: the caller falls back to the in-memory queue when the
                // write did not land, so an ignored false would silently lose exactly what this
                // exists for.
                return edit.commit();
            } catch (Throwable unavailable) {
                return false;
            }
        }
    }

    /// Whether a drain is already walking the store.
    ///
    /// ONE drain at a time, or the order the app sees is not the order things happened. Two Data
    /// Layer workers can both reach here: the first claims the oldest key and is descheduled, the
    /// second skips that in-flight key and queues the NEXT record, and the oldest then arrives
    /// after it. Sorted keys buy nothing if two threads walk the list at once.
    ///
    /// A second thread returns rather than blocking. It is a Data Layer worker with a live event to
    /// deliver, and the drain it would have run is the one already in progress -- which loops until
    /// the store is empty, so anything written meanwhile is picked up before it finishes.
    private static boolean spoolDraining;

    /// Set by every writer, cleared by the owner only when it is about to stop.
    ///
    /// Ownership alone leaves a gap at the very end: a worker that sees a drain in progress spools
    /// its event and does not start one, and if the owner clears the flag before that write lands,
    /// nobody is left looking. The record then waits for unrelated traffic or a restart. Raising
    /// this under the same lock the owner checks closes it -- the owner either sees the flag and
    /// loops, or has not finished yet and the writer's record is in a pass still to come.
    private static boolean spoolDirty;

    /// Replays everything written down, oldest first, forgetting each only once it is delivered.
    static void drainSpool(Context context) {
        synchronized (SPOOL_LOCK) {
            if (spoolDraining) {
                return;
            }
            spoolDraining = true;
        }
        try {
            while (true) {
                drainSpoolPass(context);
                synchronized (SPOOL_LOCK) {
                    if (!spoolDirty) {
                        // Nothing arrived while that pass ran, and no writer can slip in behind
                        // this: a writer raises the flag under this same lock, so it either did so
                        // before this check -- and the loop continues -- or it is still waiting to
                        // take the lock, and will find spoolDraining false and drain for itself.
                        spoolDraining = false;
                        return;
                    }
                    spoolDirty = false;
                }
            }
        } catch (RuntimeException failed) {
            synchronized (SPOOL_LOCK) {
                spoolDraining = false;
            }
            throw failed;
        }
    }

    /// One walk of the store. Returns whether anything was replayed, so the owner knows to look
    /// again.
    private static boolean drainSpoolPass(Context context) {
        boolean replayed = false;
        for (String key : spooledKeys(context)) {
            // ONE record at a time. Charging an attempt to the whole batch up front meant the
            // oldest record crashing the process three times threw away every later message and
            // removal with it -- none of which had been tried even once.
            String record = claimSpooled(context, key);
            if (record == null) {
                continue;
            }
            final Context c = context;
            final String claimed = key;
            // Released from the delivery callback, not here. replaySpooled only QUEUES onto
            // WearableConnection: the listeners run later on the EDT, and may not run at all yet if
            // none is registered. Forgetting the record at queue time lost exactly what this spool
            // exists to survive -- a process that dies between the queueing and the callback.
            replaySpooled(record, new Runnable() {
                public void run() {
                    releaseSpooled(c, claimed);
                }
            });
            replayed = true;
        }
        return replayed;
    }

    /// How many launches a single record may be replayed on before it is abandoned.
    ///
    /// The cost of keeping a record until delivery is that a listener which throws, or a process
    /// that dies mid-callback, sees it again next time. That is the right trade for a one-shot --
    /// delivered twice is recoverable, lost is not -- but it cannot be unbounded, or one poison
    /// payload replays on every launch for ever.
    private static final int SPOOL_MAX_ATTEMPTS = 3;

    /// Keys that have been claimed and whose delivery has not been confirmed yet.
    ///
    /// A drain runs from deliverableNow, which every incoming message and removal calls, so a
    /// second event can re-enter it while the first record is still queued on the EDT. With nothing
    /// marking the record as in flight it was claimed again: the same callback queued twice, and
    /// three such events inside one process burned the whole attempt budget and deleted the durable
    /// copy before any listener had run.
    ///
    /// Guarded by SPOOL_LOCK. A record parked for want of a listener stays here for the life of the
    /// process, which is exactly right -- the parked runnable still holds it, and re-claiming it
    /// would duplicate the delivery rather than rescue it.
    private static final java.util.Set<String> SPOOL_IN_FLIGHT = new java.util.HashSet<String>();

    /// The spooled keys in delivery order, without touching their attempt counts.
    private static java.util.List<String> spooledKeys(Context context) {
        java.util.List<String> keys = new ArrayList<String>();
        Context c = spoolContext(context);
        if (c == null) {
            return keys;
        }
        synchronized (SPOOL_LOCK) {
            try {
                for (String k : c.getSharedPreferences(SPOOL_PREFS, Context.MODE_PRIVATE)
                        .getAll().keySet()) {
                    if (!SPOOL_SEQ_KEY.equals(k)) {
                        keys.add(k);
                    }
                }
            } catch (Throwable unavailable) {
                return new ArrayList<String>();
            }
        }
        // Zero-padded keys, so lexical order IS the order they were written in.
        java.util.Collections.sort(keys);
        return keys;
    }

    /// Charges one attempt to a single record and returns what to replay, or null to skip it.
    ///
    /// The attempt is written BEFORE the callback runs, so a record that kills the process is
    /// counted against its budget rather than retried for ever.
    private static String claimSpooled(Context context, String key) {
        Context c = spoolContext(context);
        if (c == null) {
            return null;
        }
        synchronized (SPOOL_LOCK) {
            // Already being delivered, by an earlier drain whose callback has not fired. Claiming
            // it again would queue the same payload a second time.
            if (!SPOOL_IN_FLIGHT.add(key)) {
                return null;
            }
            try {
                android.content.SharedPreferences prefs =
                        c.getSharedPreferences(SPOOL_PREFS, Context.MODE_PRIVATE);
                String record = prefs.getString(key, null);
                if (record == null) {
                    SPOOL_IN_FLIGHT.remove(key);
                    return null;
                }
                // Before the attempt is charged. A record replayed with no listener registered is
                // only parked in memory, so its confirmation callback never runs -- and a service
                // process reclaimed over and over each charged another attempt for a delivery that
                // never reached application code. Four of those lifetimes deleted the record on the
                // attempt budget alone, which is precisely the loss the budget was meant to bound.
                //
                // A record nobody can take is left exactly as it was, for the launch where someone
                // can.
                if (!listenerExistsFor(bodyOf(record))) {
                    SPOOL_IN_FLIGHT.remove(key);
                    return null;
                }
                int attempts = attemptsOf(record) + 1;
                android.content.SharedPreferences.Editor edit = prefs.edit();
                if (attempts > SPOOL_MAX_ATTEMPTS) {
                    android.util.Log.w("CN1Wearable", "giving up on a spooled wearable delivery"
                            + " after " + SPOOL_MAX_ATTEMPTS + " attempts: " + bodyOf(record));
                    edit.remove(key);
                    edit.commit();
                    SPOOL_IN_FLIGHT.remove(key);
                    return null;
                }
                edit.putString(key, attempts + "|" + bodyOf(record));
                if (!edit.commit()) {
                    // The attempt did not land, so replaying now would be unbounded on a record
                    // that keeps killing the process. It waits for the next launch.
                    SPOOL_IN_FLIGHT.remove(key);
                    return null;
                }
                return bodyOf(record);
            } catch (Throwable unavailable) {
                SPOOL_IN_FLIGHT.remove(key);
                return null;
            }
        }
    }

    /// Forgets one record, once its listeners have actually seen it.
    private static void releaseSpooled(Context context, String key) {
        Context c = spoolContext(context);
        if (c == null) {
            return;
        }
        synchronized (SPOOL_LOCK) {
            try {
                if (c.getSharedPreferences(SPOOL_PREFS, Context.MODE_PRIVATE)
                        .edit().remove(key).commit()) {
                    SPOOL_IN_FLIGHT.remove(key);
                }
                // Still on disk if that failed, and deliberately still in flight: the listeners
                // have already had it, so re-claiming it in THIS process would only duplicate the
                // delivery. The next launch retries it, and the attempt count bounds that.
            } catch (Throwable unavailable) {
                // Same reasoning. It keeps its attempt count and is retried on a later launch.
            }
        }
    }

    /// Whether a listener of the kind this record needs is registered right now.
    private static boolean listenerExistsFor(String body) {
        try {
            int bar = body.indexOf('|');
            String kind = bar < 0 ? body : body.substring(0, bar);
            return SPOOL_REMOVAL.equals(kind)
                    ? WearableConnection.hasDataListener()
                    : WearableConnection.hasMessageListener();
        } catch (Throwable unavailable) {
            return false;
        }
    }

    /// The leading attempt count of a stored record, or 0 for one written before it had one.
    private static int attemptsOf(String record) {
        int bar = record.indexOf('|');
        if (bar <= 0) {
            return 0;
        }
        try {
            return Integer.parseInt(record.substring(0, bar));
        } catch (NumberFormatException notCounted) {
            return 0;
        }
    }

    /// Everything after the attempt count: the record replaySpooled understands.
    private static String bodyOf(String record) {
        int bar = record.indexOf('|');
        if (bar <= 0) {
            return record;
        }
        try {
            Integer.parseInt(record.substring(0, bar));
        } catch (NumberFormatException notCounted) {
            return record;
        }
        return record.substring(bar + 1);
    }

    private static void replaySpooled(String record, Runnable delivered) {
        int first = record.indexOf('|');
        int second = first < 0 ? -1 : record.indexOf('|', first + 1);
        if (first < 0 || second < 0) {
            return;
        }
        String kind = record.substring(0, first);
        String path = decode(record.substring(first + 1, second));
        String encoded = record.substring(second + 1);
        try {
            if (SPOOL_REMOVAL.equals(kind)) {
                WearableConnection.deliverDataRemoved(path, delivered);
                return;
            }
            byte[] payload = encoded.length() == 0 ? new byte[0]
                    : android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP);
            WearableConnection.deliverMessage(path, payload, 0, delivered);
        } catch (Throwable unreadable) {
            // A record this build cannot parse is dropped rather than retried forever.
        }
    }

    private static void trimSpool(android.content.SharedPreferences prefs,
            android.content.SharedPreferences.Editor edit) {
        java.util.List<String> keys = new ArrayList<String>();
        for (String k : prefs.getAll().keySet()) {
            if (!SPOOL_SEQ_KEY.equals(k)) {
                keys.add(k);
            }
        }
        if (keys.size() < SPOOL_MAX_ENTRIES) {
            return;
        }
        java.util.Collections.sort(keys);
        int drop = keys.size() - SPOOL_MAX_ENTRIES + 1;
        for (int i = 0; i < drop; i++) {
            edit.remove(keys.get(i));
            android.util.Log.w("CN1Wearable", "wearable spool is full at " + SPOOL_MAX_ENTRIES
                    + " entries; dropping the oldest undelivered entry " + keys.get(i));
        }
    }

    private static String spoolKey(long seq) {
        String digits = Long.toString(seq);
        StringBuilder sb = new StringBuilder("e");
        for (int i = digits.length(); i < 18; i++) {
            sb.append('0');
        }
        return sb.append(digits).toString();
    }

    private static Context spoolContext(Context context) {
        Context c = context;
        if (c == null) {
            c = serviceContext;
        }
        if (c == null) {
            CN1WearableBridge b = current;
            c = b == null ? null : b.context;
        }
        return c;
    }

    static void noteServiceContext(Context context) {
        if (context == null || serviceContext != null) {
            return;
        }
        serviceContext = context.getApplicationContext();
        // Restore FIRST: an observation compared against an unrestored floor would look new and
        // overwrite a higher stored value with a lower one.
        restoreClock(serviceContext);
    }

    /** Restores the persisted floor, so the first publish after a restart cannot regress. */
    private static synchronized void restoreClock(Context context) {
        try {
            long stored = context.getSharedPreferences(CLOCK_PREFS, Context.MODE_PRIVATE)
                    .getLong(CLOCK_KEY, 0);
            persistedClock = stored;
            if (stored > lastSequence) {
                lastSequence = stored;
            }
        } catch (Throwable unavailable) {
            // No stored floor: wall-clock millis seed the counter as before.
        }
    }

    /**
     * Raises this device's clock past a stamp it has just seen from a peer.
     *
     * <p>Wall-clock millis alone are not a sound cross-device order: if one device's clock runs
     * ahead -- automatic time switched off, or either clock corrected -- its stamps would beat every
     * later write from the other device until real time caught up, which can be hours.
     *
     * <p>Observing fixes that without needing synchronised clocks. Every sequence we read from an
     * item pushes our own counter past it, so the moment a behind device sees an ahead device's
     * stamp it can publish a higher one. Millis remain the seed, which keeps stamps monotonic across
     * a process restart and roughly meaningful as a time; the observation is what makes the ORDER
     * correct. This is a Lamport clock with a wall-clock floor.
     *
     * @param seen a sequence read from a published item
     */
    static synchronized void observeSequence(long seen) {
        if (seen != Long.MIN_VALUE && seen > lastSequence) {
            lastSequence = seen;
            persistClock(lastSequence);
        }
    }

    private static long lastSequence;

    public byte[] getData(String path) {
        // The EDT gets the AUTHORITATIVE answer, obtained without freezing.
        //
        // Answering an EDT caller from the cache was wrong in a way the contract cannot absorb:
        // this getter documents null as "nothing is published here", and after a cold launch the
        // cache is empty for durable state that still exists -- the Data Layer has no reason to
        // re-announce an item it delivered before the restart. A UI action reading it then
        // concluded there was no state and discarded valid data. Priming in the background does not
        // fix that; it only makes the NEXT call right, and there may not be a next call.
        //
        // invokeAndBlock is exactly the tool for this: the query runs off the EDT while the EDT
        // keeps pumping paint and input, so the caller gets the real answer and the UI does not
        // freeze for TIMEOUT_SECONDS. As with any invokeAndBlock, do not call this from paint().
        if (com.codename1.ui.CN.isEdt()) {
            final String requested = path;
            final byte[][] resolved = new byte[1][];
            com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    resolved[0] = resolveDataBlocking(requested);
                }
            });
            return resolved[0];
        }
        // Android's main thread is not the EDT and has no invokeAndBlock: this is where a Play
        // services completion listener runs, and blocking it is an ANR rather than a slow frame.
        // Internal callers only -- app code reaching this getter is on the EDT or a thread of its
        // own -- so the cached answer plus a background prime is the best available here.
        if (isCallerLatencySensitive()) {
            byte[] cached = cachedValue(path);
            boolean absent;
            synchronized (valueCache) {
                absent = knownAbsent.contains(path);
            }
            if (cached == null && !absent) {
                // An empty cache is not an authoritative absence. After a process restart the Data
                // Layer has no reason to re-announce an item it already delivered, so nothing
                // refills the cache on its own and the getter would report "nothing published" for
                // durable state that still exists. Answer null now -- the EDT cannot wait -- but
                // populate in the background so the next call is right.
                primeValue(path);
            }
            return cached;
        }
        return resolveDataBlocking(path);
    }

    /// The blocking resolution, on a thread that can afford it.
    private byte[] resolveDataBlocking(String path) {
        // Deliberately the same resolution the listener uses. This used to have its own loop, which
        // kept whichever item the buffer yielded first -- so once resolveValue() gained the
        // publisher tie-break, getData() could return a different value than the listener had just
        // delivered for the same path. One implementation, one answer.
        String before = deliveredStamp(path);
        try {
            ResolvedValue v = resolveValue(context, path);
            byte[] out = v == null ? null : v.payload;
            // Only if no delivery moved this path while the query was blocked -- otherwise this
            // older snapshot would outlive the newer one a delivery has already recorded.
            rememberValueIfStampUnchanged(path, before, out);
            return out;
        } catch (java.io.IOException unavailable) {
            // A failed query is NOT an empty path, and the two must not collapse into the same
            // answer: the public getter documents null as "no value here", so returning it after a
            // timeout invites a caller to clear state for a path that is still published.
            // resolveValue throws precisely to keep them apart. The last known snapshot is the
            // honest answer -- it is what this device last saw -- and null only when there is not
            // even that.
            return cachedValue(path);
        }
    }

    /// Populates the cache for one path in the background, at most once per path per process.
    ///
    /// Only for the latency-sensitive path, which cannot block. The query itself is the ordinary
    /// resolution, and its result is recorded exactly as a delivery would record it, so a
    /// publication that lands meanwhile still wins.
    private void primeValue(final String path) {
        synchronized (primedValues) {
            if (!primedValues.add(path)) {
                return;
            }
        }
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                String before = deliveredStamp(path);
                try {
                    ResolvedValue v = resolveValue(context, path);
                    // Both outcomes go through the SAME stamp-guarded recorder. An AUTHORITATIVE
                    // absence -- the query answered and the path is empty -- is worth recording,
                    // because it stops a polling UI re-asking forever: every repaint reading a path
                    // that genuinely does not exist otherwise scheduled another blocking query on
                    // the timer that transfer replay, retries and cleanup all share. resolveValue
                    // THROWS when it cannot ask, so a failure never reaches here and cannot
                    // masquerade as an absence.
                    //
                    // But it has to be anchored like any other stale answer. Writing it
                    // unconditionally let a query that started before a publication mark the path
                    // absent AFTER that publication had recorded its value -- masked while the
                    // cached payload survived, then permanent once the LRU evicted it, because the
                    // stale absence stopped anything from priming the path again.
                    rememberValueIfStampUnchanged(path, before, v == null ? null : v.payload);
                } catch (Throwable unavailable) {
                    // Nothing to record; the marker is released below either way.
                } finally {
                    // IN-FLIGHT only, released whatever happened. Holding it for the life of the
                    // process meant a path whose cached value was later evicted by the LRU could
                    // never be fetched again -- getData returned null and declined to ask, so a
                    // durable item stayed unreadable unless a new Data Layer event happened to
                    // arrive. Releasing it still bounds the work, because a path already being
                    // queried is not queried again and each query holds the marker for its whole
                    // duration, so a repainting caller cannot stack them up.
                    synchronized (primedValues) {
                        primedValues.remove(path);
                    }
                }
            }
        }, 0);
    }

    /// Enumerates in the background so a cold {@code getDataPaths} is only wrong once.
    ///
    /// Runs the off-EDT branch of {@code getDataPaths} itself, which already records the snapshot
    /// and its generation, rather than duplicating the enumeration.
    private void primePaths() {
        synchronized (primedValues) {
            if (!primedValues.add(PATHS_PRIMED_KEY)) {
                return;
            }
        }
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                try {
                    getDataPaths();
                } catch (Throwable unavailable) {
                    // Not the usual case: getDataPaths swallows a Data Layer failure itself.
                }
                // So the OUTCOME is what decides, not an exception. A transient failure leaves
                // pathsCache null, and marking the attempt done on that basis meant every later
                // latency-sensitive call answered empty forever without ever asking again.
                if (pathsCache == null) {
                    synchronized (primedValues) {
                        primedValues.remove(PATHS_PRIMED_KEY);
                    }
                }
            }
        }, 0);
    }

    /// Not a path: paths are prefixed, so this cannot collide with one.
    private static final String PATHS_PRIMED_KEY = "\u0000paths";

    /// Paths whose cold-start population has been attempted, so a repainting caller polling
    /// {@code getData} does not queue a query per frame.
    private static final java.util.Set<String> primedValues = new java.util.HashSet<String>();

    /// The payload bytes out of a value's DataMap, never null.
    ///
    /// @param value a map obtained from {@link #valueMap}
    /// @return the published bytes
    static byte[] payloadOf(DataMap value) {
        byte[] payload = value.getByteArray(PAYLOAD_KEY);
        return payload == null ? new byte[0] : payload;
    }

    /**
     * The DataMap of an ordinary published value, or null when the item is not one -- a file transfer
     * (which carries an Asset instead of a payload) or something not written by this API at all.
     * This is what keeps transfers out of {@link #getDataPaths()} and out of {@link #getData}.
     *
     * @param item a received or queried data item
     * @return the value's DataMap, or null
     */
    static DataMap valueMap(DataItem item) {
        try {
            DataMap map = DataMapItem.fromDataItem(item).getDataMap();
            return map.containsKey(PAYLOAD_KEY) ? map : null;
        } catch (Throwable notADataMap) {
            return null;
        }
    }

    public void removeData(String path) {
        // Remembered before the delete is issued. removeData targets wear://*/... -- a WILDCARD --
        // so Play services deletes every authority's replica and the resulting buffer can carry
        // tombstones whose authority is a PEER node even though this app initiated the removal.
        // Tombstone authorship therefore does not identify who asked, and suppressing only the
        // local-authority one still reported the app's own removal back to it.
        final String storagePath = dataPath(path);
        final long generation = noteLocalRemoval(storagePath);
        Uri uri = new Uri.Builder().scheme("wear").authority("*").path(storagePath).build();
        // The marker is dropped again if the delete FAILS. Left standing for its full window, it
        // would swallow a genuine peer removal of the same path arriving inside it -- the listener
        // reads the path as this device's own pending delete and stays silent, for a removal that
        // never happened here. Nothing else would correct that.
        //
        // Only this operation's own marker: the generation makes a later removeData for the same
        // path a different marker, so a failure reported after it cannot clear the newer one.
        dataClient.deleteDataItems(uri).addOnFailureListener(new OnFailureListener() {
            public void onFailure(Exception e) {
                forgetLocalRemovalIfGeneration(storagePath, generation);
            }
        });
    }

    /// Drops a local-removal marker, but only while it is still the one the caller recorded.
    private static void forgetLocalRemovalIfGeneration(String storagePath, long generation) {
        synchronized (localRemovals) {
            Removal r = localRemovals.get(storagePath);
            if (r != null && r.generation == generation) {
                localRemovals.remove(storagePath);
            }
        }
    }

    /**
     * Paths this device has just asked to remove, with the time it asked.
     *
     * <p>Time-bounded rather than cleared on first use: one wildcard delete produces one tombstone
     * per replica, so the entry has to outlive all of them, while a peer's genuine removal of the
     * same path later must still reach the app.</p>
     */
    /// Paths a completed query reported as empty. Guarded by {@link #valueCache}, and cleared for a
    /// path the moment anything authoritative says it exists again.
    private static final java.util.Set<String> knownAbsent =
            java.util.Collections.newSetFromMap(new java.util.LinkedHashMap<String, Boolean>() {
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    // Bounded like valueCache, and for the same reason. Only a later publication of
                    // the SAME path clears an entry, so an app using record-specific paths would
                    // otherwise retain every deleted path string for the life of the process. An
                    // evicted entry costs one extra background query the next time that path is
                    // read, which is exactly the state before this cache existed.
                    return size() > MAX_KNOWN_ABSENT;
                }
            });

    private static final int MAX_KNOWN_ABSENT = 256;

    private static final Map<String, Removal> localRemovals = new HashMap<String, Removal>();
    private static final long LOCAL_REMOVAL_WINDOW_MILLIS = 30 * 1000L;

    /**
     * A local removal, tagged with the order in which it was recorded.
     *
     * <p>The generation is what lets a queued event tell "the marker I saw" from "a marker recorded
     * after me". Callbacks are handled on a worker, so handler time and event time are no longer
     * the same instant, and a publication received BEFORE a {@code removeData} can run after it.
     * Time alone cannot express that ordering safely -- {@code currentTimeMillis} ties and can go
     * backwards -- so removals are numbered.</p>
     */
    private static final class Removal {
        final long at;
        final long generation;

        Removal(long at, long generation) {
            this.at = at;
            this.generation = generation;
        }
    }

    private static long removalGeneration;

    /// The current removal generation, captured by a listener when an event ARRIVES so it can later
    /// tell whether the marker it is about to clear is the one it actually saw.
    static long currentRemovalGeneration() {
        synchronized (localRemovals) {
            return removalGeneration;
        }
    }

    /// @return the generation recorded, so the caller can withdraw exactly this marker
    private static long noteLocalRemoval(String storagePath) {
        long now = System.currentTimeMillis();
        long generation;
        synchronized (localRemovals) {
            generation = ++removalGeneration;
            localRemovals.put(storagePath, new Removal(now, generation));
            purgeExpiredRemovals(now);
        }
        return generation;
    }

    /// Drops markers whose window has passed. Caller holds the localRemovals monitor.
    ///
    /// Called from the READ paths as well as from noteLocalRemoval, because expiry that happens
    /// only when another removal arrives never happens at all for an app that deletes a burst of
    /// record-specific paths and then stops: the markers were retained for the life of the process
    /// and every later Data Layer event walked past them.
    private static void purgeExpiredRemovals(long now) {
        java.util.Iterator<Map.Entry<String, Removal>> it = localRemovals.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().at > LOCAL_REMOVAL_WINDOW_MILLIS) {
                it.remove();
            }
        }
    }

    /**
     * Ends the local-removal window for a path.
     *
     * <p>The window exists to cover the tombstones of ONE wildcard delete, and a publication for
     * the same path is proof that delete is over. Without this the marker stood for its full
     * duration regardless of what happened next, so a peer that republished the path and then
     * removed its new value inside the window had that genuine deletion classified as part of this
     * device's earlier delete -- the removal was swallowed and the listener kept a value that no
     * longer existed, with no later callback to correct it.</p>
     *
     * <p>Only a marker the caller actually saw is cleared. Callbacks are handled on a worker, so a
     * publication received before a {@code removeData} can be handled after it; clearing
     * unconditionally then wiped a NEWER marker, and the wildcard tombstones that followed were no
     * longer recognised as this device's own delete -- with a replica on each device the
     * peer-authority tombstone bypasses the echo check and the app was handed a peer removal
     * callback for its own operation.</p>
     *
     * @param observedGeneration the value {@link #currentRemovalGeneration()} returned when the
     *                           event being handled arrived
     */
    static void clearLocalRemoval(String storagePath, long observedGeneration) {
        synchronized (localRemovals) {
            Removal r = localRemovals.get(storagePath);
            if (r != null && r.generation <= observedGeneration) {
                localRemovals.remove(storagePath);
            }
        }
    }

    /**
     * The set of paths whose local-removal window is open right now.
     *
     * <p>Captured when a callback ARRIVES, because the worker can run it much later -- two
     * first-sight resolutions timing out and retrying is enough to push a handler past the 30-second
     * window. Classifying with {@link #isLocallyRemoved} at handler time then found the marker
     * expired and announced the app's own wildcard tombstone back to it as a peer removal. The
     * window is measured from when the event arrived, which is when the removal was actually still
     * in progress.</p>
     */
    static java.util.Set<String> openRemovals() {
        long now = System.currentTimeMillis();
        java.util.Set<String> open = new java.util.HashSet<String>();
        synchronized (localRemovals) {
            // Purged rather than merely skipped: this walk is already O(size), so dropping the
            // expired entries as it goes costs nothing and is what keeps the map from growing.
            purgeExpiredRemovals(now);
            for (Map.Entry<String, Removal> e : localRemovals.entrySet()) {
                open.add(e.getKey());
            }
        }
        return open;
    }

    /** Whether a tombstone for this storage path came from a removal this device asked for. */
    static boolean isLocallyRemoved(String storagePath) {
        long now = System.currentTimeMillis();
        synchronized (localRemovals) {
            purgeExpiredRemovals(now);
            Removal r = localRemovals.get(storagePath);
            return r != null;
        }
    }

    public String[] getDataPaths() {
        // Same reasoning as getData, and the same answer. An empty array read as "there is no
        // persisted wearable state" is how one-shot initialisation code decides never to read it,
        // and after a cold launch that is exactly what an unenumerated cache produced. The EDT gets
        // the real enumeration through invokeAndBlock, which keeps painting and input alive while
        // the await runs.
        if (com.codename1.ui.CN.isEdt()) {
            final String[][] enumerated = new String[1][];
            com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    enumerated[0] = enumerateDataPathsBlocking();
                }
            });
            return enumerated[0];
        }
        // Android's main thread: no invokeAndBlock, and blocking it is an ANR. Internal callers
        // only; app code is on the EDT or a thread of its own.
        if (isCallerLatencySensitive()) {
            String[] known = pathsCache;
            if (known == null) {
                // Never enumerated in this process. Same cold-start problem as getData: the Data
                // Layer will not re-announce items it delivered before the restart, so this would
                // keep answering "no paths" for state that exists. Enumerate in the background.
                primePaths();
                return new String[0];
            }
            return known.clone();
        }
        return enumerateDataPathsBlocking();
    }

    /// The blocking enumeration, on a thread that can afford it.
    private String[] enumerateDataPathsBlocking() {
        int startedGeneration;
        synchronized (valueCache) {
            startedGeneration = pathsGeneration;
        }
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
                String[] enumerated = out.toArray(new String[out.size()]);
                // Only if no delivery maintained the snapshot while this enumeration was blocked.
                // rememberValue adds and removes paths as callbacks arrive, and those callbacks
                // have already fired and may not repeat -- so an older enumeration landing on top
                // would leave every EDT getDataPaths() answering from it indefinitely.
                synchronized (valueCache) {
                    if (pathsGeneration == startedGeneration) {
                        pathsCache = enumerated;
                        pathsGeneration++;
                    }
                }
                return enumerated.clone();
            } finally {
                items.release();
            }
        } catch (Throwable unavailable) {
            // A transport failure is not an authoritative "no paths". Returning an empty array let
            // a caller clear valid replicated state on a timeout, the same conflation getData had.
            // The last successful enumeration is the honest answer.
            String[] known = pathsCache;
            return known == null ? new String[0] : known.clone();
        }
    }

    public void transferFile(String path, String name, byte[] contents) {
        final String p = path;
        final String fileName = name == null ? "file" : name;
        final byte[] body = contents == null ? new byte[0] : contents;
        // Off the caller's thread for the same reason putData is: allocating the sequence advances
        // the durable clock floor with a synchronous commit(), and this is called from application
        // code that is usually on the EDT. The transfer worker is single-threaded, so transfers
        // keep the order they were requested in.
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                publishTransfer(p, fileName, body);
            }
        }, 0);
    }

    /// Builds and publishes one transfer item, on the transfer worker.
    private void publishTransfer(String path, String fileName, byte[] body) {
        // A DataItem's inline payload is capped at about 100KB, which a real file routinely
        // exceeds; an Asset is the Data Layer's own answer for bulk and is streamed in the
        // background. The DataItem carries the name and the Asset, so the receiver still gets a
        // WearableMessage rather than raw bytes.
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
        req.getDataMap().putLong(PUBLISHED_AT_KEY, System.currentTimeMillis());
        req.getDataMap().putAsset("asset", Asset.createFromBytes(body));
        dataClient.putDataItem(req.asPutDataRequest().setUrgent());
        expireOwnTransfers();
    }

    /** How long one of our own published transfer items is kept before it is swept. */
    private static final long TRANSFER_RETENTION_MILLIS = 24 * 60 * 60 * 1000L;

    /// The outer bound on a transfer nobody has acknowledged.
    ///
    /// Retention alone was age-only, and age is not evidence of delivery: a watch offline for more
    /// than a day, or one of several watches that had not synced yet, came back to nothing. The
    /// receiver never deletes a transfer and the sender kept no record of who had taken it, so the
    /// file was simply gone.
    ///
    /// Retirement now needs an acknowledgement from every peer this device knows about. This cap is
    /// the backstop for the peer that never returns -- an uninstalled watch app, a watch unpaired
    /// and forgotten -- because without one such a device would pin every file the phone has ever
    /// sent, forever. A week is far past any sync window and still bounded.
    private static final long TRANSFER_HARD_CAP_MILLIS = 7 * 24 * 60 * 60 * 1000L;

    /// Namespace for delivery acknowledgements. A receiver publishes one per transfer it has
    /// actually handed to a listener; the sender reads them to decide what it may retire.
    ///
    /// Deliberately OUTSIDE the "/cn1" namespace rather than a suffix of it. PATH_PREFIX has no
    /// trailing slash, so anything beginning "/cn1" -- "/cn1xk/..." included -- passes the value
    /// filter and would have been handed to the app as a replicated change at a path it never
    /// published. Transfers escape that only because they are excluded by name a line earlier.
    private static final String TRANSFER_ACK_PREFIX = "/cnxk";

    /// The acknowledgement path for a transfer, scoped to the node that PUBLISHED it.
    ///
    /// The transfer path alone is not an identity. Two senders -- two watches reacting to the same
    /// event -- can produce the same logical path, file name and per-device sequence, and their
    /// item URIs then differ only in authority. An acknowledgement keyed on the path alone would
    /// answer for both, and the sender that had not been delivered to would delete a transfer
    /// nobody had taken.
    static String transferAckPath(String publisherNode, String transferPath) {
        return TRANSFER_ACK_PREFIX + "/" + publisherNode + transferPath;
    }

    /// The publisher-scoped transfer key an acknowledgement refers to, or null when this path is
    /// not an acknowledgement. Compared against {@link #transferKey}.
    static String ackedTransferKey(String ackPath) {
        return ackPath != null && ackPath.startsWith(TRANSFER_ACK_PREFIX + "/")
                ? ackPath.substring(TRANSFER_ACK_PREFIX.length() + 1) : null;
    }

    /// The identity of a transfer item: who published it, and at what path.
    static String transferKey(String publisherNode, String transferPath) {
        return publisherNode + transferPath;
    }
    /** Floor between sweeps; retention is a day, so sweeping more often than this buys nothing. */
    private static final long SWEEP_MIN_INTERVAL_MILLIS = 5 * 60 * 1000L;
    private final Object sweepLock = new Object();
    private boolean sweepScheduled;
    private long lastSweepAt;

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
        // The deadline is armed UNCONDITIONALLY, before the coalescing check, because it belongs to
        // the item that was just published rather than to this call. Arming it after the check
        // meant a transfer published inside the five-minute coalescing window never got one: the
        // earlier task woke at the FIRST item's deadline, found the later item still too young, and
        // put its next sweep a full day out -- so that item could stay published for nearly 48
        // hours while receiver claims are pruned at 24, and a reconnect could redeliver a supposedly
        // one-shot file. Retention is a promise about the item.
        //
        // ONE task for the earliest outstanding deadline, not one per item. Every task did the same
        // global sweep, so an app sending continuously retained tens of thousands of them for a day
        // and then woke the shared timer in a burst. When the task fires, the sweep rearms it for
        // the oldest item that is still published -- which is exact, needs no queue of deadlines,
        // and stops on its own when nothing is left.
        armSweepDeadline(System.currentTimeMillis() + TRANSFER_RETENTION_MILLIS + 1000L);
        sweepOwnTransfers();
    }

    /// The absolute time the pending deadline task will fire, or 0 when none is pending.
    private long sweepDeadlineAt;

    private java.util.TimerTask sweepDeadlineTask;

    /// Ensures a deadline sweep happens no later than {@code at}.
    ///
    /// An existing task that already fires by then is left alone -- that is what collapses a burst
    /// of transfers into a single timer entry, since their deadlines only ever move later. A task
    /// is replaced only when something genuinely needs an EARLIER sweep, which is why the cancelled
    /// one is purged rather than left to expire in the queue.
    private void armSweepDeadline(long at) {
        synchronized (sweepLock) {
            if (sweepDeadlineTask != null && sweepDeadlineAt <= at) {
                return;
            }
            if (sweepDeadlineTask != null) {
                sweepDeadlineTask.cancel();
                transferTimer.purge();
            }
            sweepDeadlineAt = at;
            sweepDeadlineTask = new java.util.TimerTask() {
                public void run() {
                    synchronized (sweepLock) {
                        sweepDeadlineTask = null;
                        sweepDeadlineAt = 0;
                    }
                    sweepOwnTransfers();
                }
            };
            long delay = at - System.currentTimeMillis();
            transferTimer.schedule(sweepDeadlineTask, delay < 0 ? 0 : delay);
        }
    }

    private boolean deferredSweepScheduled;

    /// Re-runs a sweep once the coalescing interval has elapsed. At most one is pending; it is not
    /// a per-call chain. Caller holds {@link #sweepLock}.
    private void scheduleDeferredSweep(long delay) {
        if (deferredSweepScheduled) {
            return;
        }
        deferredSweepScheduled = true;
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                synchronized (sweepLock) {
                    deferredSweepScheduled = false;
                }
                sweepOwnTransfers();
            }
        }, delay < 0 ? 0 : delay + 1000L);
    }

    /**
     * Offers, once per app start, anything still published that this process has not delivered:
     * inbound transfers that were never claimed, and replicated values with no delivery stamp.
     *
     * <p>Nothing else covers this. A transfer can wake the listener service in a cold process, and
     * if Android refuses the background activity launch the payload exists only in
     * WearableConnection's in-memory queue -- so killing that process before the user opens the app
     * loses it. The DataItem itself is untouched, but it is also UNCHANGED, and an unchanged item
     * raises no callback for a process that starts later, so the app would never see it and the
     * sender would eventually sweep the only durable copy.
     *
     * <p>The claim is what makes this safe to run unconditionally: an item already handed over has
     * a durable claim, so {@code claimTransfer} refuses it here and nothing is delivered twice.</p>
     */
    private void replayOutstandingTransfers() {
        replayOutstandingTransfers(1, System.currentTimeMillis());
    }

    /// Linear backoff, capped, so a long outage is cheap but a short one recovers quickly.
    private static long replayDelay(int attempt) {
        long linear = REPLAY_RETRY_MILLIS * (long) attempt;
        return linear > REPLAY_RETRY_CAP_MILLIS ? REPLAY_RETRY_CAP_MILLIS : linear;
    }

    /// Replay retries are bounded by the sender's RETENTION WINDOW, not by an attempt count.
    ///
    /// Five tries spanned about twenty seconds, and an outage longer than that ended the only
    /// recovery this transfer has -- the item stays published for a day, raises no callback because
    /// it never changes, and is then swept. So the chain runs until an enumeration actually
    /// succeeds or the window has passed, backing off to a minute so a long outage costs one
    /// failing query an hour rather than a busy loop.
    private static final long REPLAY_RETRY_MILLIS = 5000;
    private static final long REPLAY_RETRY_CAP_MILLIS = 60 * 1000L;

    private void replayOutstandingTransfers(final int attempt, final long startedAt) {
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                boolean replayFailed = false;
                try {
                    // The local identity FIRST, and no replay at all without it. A transient
                    // getLocalNode() failure returning null used to read as "not local", so the
                    // replay claimed this device's OWN outbound transfers and handed the sender its
                    // own one-shot file through its own data listener.
                    // Named apart from the `localNode` FIELD, which is a cache: this must be the
                    // value just resolved.
                    String replayNode = localNodeId(context);
                    if (replayNode == null) {
                        throw new java.io.IOException("local node identity unavailable");
                    }
                    DataItemBuffer items = Tasks.await(dataClient.getDataItems(),
                            TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    try {
                        for (DataItem item : items) {
                            final Uri uri = item.getUri();
                            String p = uri == null ? null : uri.getPath();
                            if (p == null) {
                                continue;
                            }
                            if (!isTransferPath(p)) {
                                // An ordinary replicated VALUE. The same loss applies to it: the
                                // event that woke the service can die with that process when
                                // Android refuses the activity launch, and the DataItem is then
                                // unchanged, so nothing re-announces it and a listener registered
                                // from init() stays stale until the peer publishes again -- which
                                // contradicts the cold-start replay this API promises.
                                //
                                // Routed through the ordinary winner resolution rather than
                                // delivered from here: that is the code that decides between two
                                // publishers and records the delivery stamp, and a path this
                                // process HAS already delivered has a stamp, so nothing is
                                // delivered twice.
                                if (p.startsWith(PATH_PREFIX) && valueMap(item) != null
                                        && !replayNode.equals(uri.getHost())) {
                                    // Items THIS device published are not worth a resolution: the
                                    // resolver now suppresses a local winner anyway, so asking
                                    // would cost a blocking query to reach the same silence. A path
                                    // a peer also published is scheduled by that peer's item, which
                                    // is in this same enumeration.
                                    String appPath = decode(p.substring(PATH_PREFIX.length()));
                                    if (!hasDeliveredStamp(appPath)) {
                                        scheduleReplayResolution(context, appPath);
                                    }
                                }
                                continue;
                            }
                            // Our own outbound transfer: handing it back would deliver the sender
                            // its own file.
                            if (replayNode.equals(uri.getHost())) {
                                continue;
                            }
                            // The claim is refreshed BEFORE the payload is decoded, because a
                            // decode can fail: an unreadable asset returns early, and the prune at
                            // the end of this pass would then drop an aged claim for an item nobody
                            // had examined -- so the retry that decodeTransfer schedules would find
                            // no claim and deliver the one-shot file a second time. Refreshing
                            // first also skips reading a potentially large asset we have already
                            // handed over.
                            if (hasAnyClaim(context, uri)) {
                                refreshClaim(context, uri);
                                continue;
                            }
                            Transfer t = decodeTransfer(context, item);
                            if (t == null || t.payload == null) {
                                continue;
                            }
                            final long seq = sequenceOf(valueOrTransferMap(item));
                            // claimTransfer applies the age-independent rule for both paths.
                            if (!claimTransfer(context, uri, seq)) {
                                // Already delivered, in this process or a previous one.
                                continue;
                            }
                            ensureAppRunning();
                            WearableConnection.deliverDataChangedTracked(
                                    t.logicalPath, t.payload, new Runnable() {
                                        public void run() {
                                            confirmTransferDelivered(context, uri, seq, true);
                                        }
                                    }, new Runnable() {
                                        public void run() {
                                            relinquishTransfer(context, uri);
                                        }
                                    });
                        }
                    } finally {
                        items.release();
                    }
                } catch (Throwable unavailable) {
                    replayFailed = true;
                }
                if (!replayFailed) {
                    // Only now, with every still-published item examined and its claim refreshed,
                    // is it safe to drop the aged ones: whatever is left describes an item that is
                    // genuinely gone. This is the startup prune the constructor used to do first.
                    pruneClaims(context);
                }
                if (replayFailed
                        && System.currentTimeMillis() - startedAt < TRANSFER_HARD_CAP_MILLIS) {
                    // Retried for as long as the sender may still be holding the item. This pass is
                    // the ONLY cover for a transfer whose cold-start delivery died with the service
                    // process: the DataItem is unchanged, so no normal callback is guaranteed, and
                    // an app that stays open through an outage would otherwise lose the file when
                    // the sender eventually sweeps it.
                    //
                    // Against the HARD CAP, like the unreadable-asset retry. An unclaimed transfer
                    // is by definition unacknowledged, and the sender now keeps those for the cap
                    // rather than the retention window -- so giving up at 24 hours abandoned an
                    // item that will still be there on day six.
                    replayOutstandingTransfers(attempt + 1, startedAt);
                }
            }
        }, attempt == 1 ? 0 : replayDelay(attempt));
    }

    /// The sweep itself, coalesced. Schedules no follow-up beyond a deferred retry when coalescing
    /// suppressed it: see [#expireOwnTransfers].
    private void sweepOwnTransfers() {
        // One sweep at a time, and not more often than the interval. A burst of transfers used to
        // schedule one immediate task per call, each blocking on a full DataItem query and scan --
        // on the same single timer the unreadable-asset retries use, so transfer traffic starved
        // the retries it was most likely to need.
        synchronized (sweepLock) {
            long now = System.currentTimeMillis();
            if (sweepScheduled || now - lastSweepAt < SWEEP_MIN_INTERVAL_MILLIS) {
                // Deferred, not dropped. An item's own deadline task can land inside the coalescing
                // window opened by a neighbouring transfer -- the earlier sweep ran just before
                // this deadline and found this item too young -- and simply returning left the next
                // guaranteed sweep at some other item's deadline up to a day later. The item then
                // outlived its retention window while receiver claims expired on time, which is
                // exactly the stale-redelivery case retention exists to prevent.
                if (!sweepScheduled) {
                    scheduleDeferredSweep(SWEEP_MIN_INTERVAL_MILLIS - (now - lastSweepAt));
                }
                return;
            }
            sweepScheduled = true;
            lastSweepAt = now;
        }
        final long cutoff = System.currentTimeMillis() - TRANSFER_RETENTION_MILLIS;
        final long hardCutoff = System.currentTimeMillis() - TRANSFER_HARD_CAP_MILLIS;
        // The earliest ABSOLUTE time a decision could change for something this sweep keeps.
        // Tracking the oldest publish time instead assumed every kept item was waiting on the
        // retention window, but an unacknowledged one waits on the hard cap -- so the deadline
        // fired at once, found nothing to do and rearmed on the same instant.
        final long[] nextDue = {Long.MAX_VALUE};
        // Every peer this device knows of, connected or merely paired. A transfer is retired once
        // ALL of them have taken it: one watch of several having synced is not enough, which is
        // exactly the case a single acknowledgement would get wrong. Never ourselves -- our own
        // items are echoed back, and waiting for an acknowledgement this device will never write
        // would pin every transfer to the cap.
        final java.util.Set<String> expected = new java.util.HashSet<String>(bondedNodeIds());
        for (Node connected : connectedNodes()) {
            expected.add(connected.getId());
        }
        expected.remove(localNodeId(context));
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                boolean failed = false;
                try {
                    // Resolved BEFORE the enumeration, and an unavailable identity is a FAILED
                    // sweep. Treating null as "every item is remote" skipped them all while
                    // reporting success, so the retry never armed and an outbound transfer could
                    // stay published past the point where receiver claims expire.
                    //
                    // Named apart from the `localNode` FIELD on purpose: this must be the value
                    // just resolved, not whatever the cache happens to hold.
                    String sweepNode = localNodeId(context);
                    if (sweepNode == null) {
                        throw new java.io.IOException("local node identity unavailable");
                    }
                    DataItemBuffer items = Tasks.await(dataClient.getDataItems(),
                            TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    try {
                        // Acknowledgements first, in a pass of their own: the enumeration has no
                        // useful order, so a transfer can be seen before the acknowledgement that
                        // retires it.
                        java.util.Map<String, java.util.Set<String>> acked =
                                new java.util.HashMap<String, java.util.Set<String>>();
                        java.util.List<Uri> ownAcks = new java.util.ArrayList<Uri>();
                        java.util.Set<String> livePaths = new java.util.HashSet<String>();
                        for (DataItem item : items) {
                            String ackPath = item.getUri().getPath();
                            if (ackPath == null) {
                                continue;
                            }
                            if (isTransferPath(ackPath)) {
                                livePaths.add(transferKey(item.getUri().getHost(), ackPath));
                                continue;
                            }
                            String forPath = ackedTransferKey(ackPath);
                            if (forPath == null) {
                                continue;
                            }
                            String acker = item.getUri().getHost();
                            if (sweepNode.equals(acker)) {
                                // OUR acknowledgement, for something a peer sent us. Only its
                                // author may delete it -- deleting another node's item propagates
                                // and would rob a second watch -- so ours are cleaned up here,
                                // once the transfer they vouch for is gone.
                                ownAcks.add(item.getUri());
                                continue;
                            }
                            java.util.Set<String> ackers = acked.get(forPath);
                            if (ackers == null) {
                                ackers = new java.util.HashSet<String>();
                                acked.put(forPath, ackers);
                            }
                            ackers.add(acker);
                        }
                        boolean retainedAck = false;
                        for (Uri ownAck : ownAcks) {
                            if (!livePaths.contains(ackedTransferKey(ownAck.getPath()))) {
                                // The item's OWN uri, not one rebuilt from parts: it already
                                // carries the authority the Data Layer expects.
                                Tasks.await(dataClient.deleteDataItems(ownAck),
                                        TIMEOUT_SECONDS, TimeUnit.SECONDS);
                            } else {
                                retainedAck = true;
                            }
                        }
                        if (retainedAck) {
                            // An acknowledgement we are KEEPING, because the transfer it vouches
                            // for is still published. Only its author can delete it, and nothing
                            // else will schedule that: the sender's eventual deletion of the
                            // transfer raises a callback the listener's deletion branch does not
                            // sweep on, so a receive-only process that stays alive would keep it
                            // for good.
                            //
                            // A slow poll rather than a precise deadline, because the receiver
                            // cannot know when the sender will retire its item -- the sender may be
                            // offline for days. One sweep a day over a handful of small items.
                            long recheck = System.currentTimeMillis() + TRANSFER_RETENTION_MILLIS;
                            if (recheck < nextDue[0]) {
                                nextDue[0] = recheck;
                            }
                        }
                        for (DataItem item : items) {
                            String p = item.getUri().getPath();
                            if (p == null || !isTransferPath(p)) {
                                continue;
                            }
                            // Only our OWN items. getDataItems() also returns transfers replicated
                            // from other nodes, and deleting one of those propagates -- which is
                            // precisely how a second watch loses a file it has not collected yet.
                            // A publisher is responsible for its own items and nobody else's.
                            if (!sweepNode.equals(item.getUri().getHost())) {
                                continue;
                            }
                            DataMap map = valueOrTransferMap(item);
                            // Age, not order: the sequence is a logical clock and may have been
                            // raised far past local time by a peer, so it says nothing about when
                            // this item was published.
                            long publishedAt = map == null
                                    ? Long.MIN_VALUE : map.getLong(PUBLISHED_AT_KEY, Long.MIN_VALUE);
                            // Age is not delivery. Retired once every known peer has said it
                            // took the file, or -- for the peer that never comes back -- once the
                            // hard cap has passed. A transfer with no known peers waits for the cap
                            // too: nobody can acknowledge it, and deleting on age alone is what
                            // lost files to an offline watch.
                            java.util.Set<String> ackers = acked.get(transferKey(sweepNode, p));
                            boolean allTook = !expected.isEmpty() && ackers != null
                                    && ackers.containsAll(expected);
                            boolean retire = publishedAt != Long.MIN_VALUE
                                    && ((allTook && publishedAt < cutoff)
                                            || publishedAt < hardCutoff);
                            if (!retire && publishedAt != Long.MIN_VALUE) {
                                // The deadline that actually applies to THIS item.
                                long due = publishedAt + (allTook
                                        ? TRANSFER_RETENTION_MILLIS : TRANSFER_HARD_CAP_MILLIS);
                                if (due < nextDue[0]) {
                                    nextDue[0] = due;
                                }
                            }
                            if (retire) {
                                // Awaited, so a deletion that fails is not counted as a sweep that
                                // succeeded. Firing and forgetting left `failed` false while the
                                // item stayed published, and for the LAST transfer there may be no
                                // later sweep -- so it outlived the receiver claims that expire at
                                // the same age, and a reconnect could redeliver a one-shot file.
                                Tasks.await(dataClient.deleteDataItems(item.getUri()),
                                        TIMEOUT_SECONDS, TimeUnit.SECONDS);
                            }
                        }
                    } finally {
                        items.release();
                    }
                } catch (Throwable unavailable) {
                    failed = true;
                } finally {
                    synchronized (sweepLock) {
                        sweepScheduled = false;
                        if (!failed && nextDue[0] != Long.MAX_VALUE) {
                            // Rearmed for what is still published. Outside the failure branch: a
                            // sweep that threw enumerated nothing reliable, and its retry below
                            // rearms this when it succeeds.
                            armSweepDeadline(nextDue[0] + 1000L);
                        }
                        if (failed) {
                            // Retried, not abandoned. "The next transfer sweeps again" assumes
                            // there IS a next transfer: an app that sends its last file and then
                            // hits a transient getDataItems() failure at that item's deadline left
                            // it published for good, and once the receiver's claim is pruned at 24
                            // hours a reconnect can redeliver a one-shot file. Retrying costs one
                            // task on a timer that is otherwise idle.
                            //
                            // lastSweepAt was set when this attempt started, so the deferred sweep
                            // is not suppressed by its own coalescing window.
                            scheduleDeferredSweep(SWEEP_MIN_INTERVAL_MILLIS);
                        }
                    }
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
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } finally {
                // The stream is backed by a ParcelFileDescriptor. A read that throws part way --
                // an interrupted or corrupt transfer -- used to skip the close and go straight to
                // the retry, so a file that kept failing leaked a descriptor per attempt.
                try {
                    in.close();
                } catch (java.io.IOException alreadyBroken) {
                    // Nothing useful to do; the descriptor is released either way.
                }
            }
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
    /// Starts a retry chain for a transfer whose Asset would not read, unless one is already
    /// running for it.
    ///
    /// ONE chain per URI. The startup replay and onDataChanged both inspect the same item on a cold
    /// start -- that race is expected, not exceptional -- and each inspection used to start its own
    /// chain. Every chain then reopens the same asset on the shared transfer timer for up to the
    /// hard cap, and each further callback added another, so the cost grew with the number of
    /// times the item was looked at while it happened to be unreadable. It also delays the replay
    /// and cleanup work queued behind it on that timer.
    ///
    /// The claim is the retryStarts entry, which every exit from the chain already clears -- decode
    /// success, our own echo, the item having disappeared, and the hard-cap give-up -- so a chain
    /// that ends releases the URI for a later one without any new bookkeeping.
    private static void scheduleTransferRetry(final Context context, final Uri uri) {
        if (uri == null) {
            return;
        }
        if (!claimRetryChain(uri)) {
            return;
        }
        scheduleTransferRetry(context, uri, 1);
    }

    /// Records that a retry chain now owns this URI, or reports that one already did.
    private static boolean claimRetryChain(Uri uri) {
        String key = uri.toString();
        synchronized (retryStarts) {
            if (retryStarts.containsKey(key)) {
                return false;
            }
            // Stamped from the FIRST attempt rather than from the first hard-cap check, so the
            // window the chain is bounded by is the one the caller actually experienced.
            retryStarts.put(key, Long.valueOf(System.currentTimeMillis()));
            return true;
        }
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
        if (uri == null) {
            return;
        }
        // Bounded by the longest the sender can still be holding the item, not by an attempt
        // count. A fixed four tries abandoned a transfer whose Asset was merely slow -- a large
        // file on a poor connection -- and the item never changes, so nothing provides a later
        // callback to pick it up again and the one-shot file was simply lost.
        //
        // Against the HARD CAP rather than the retention window. Retention is now the acknowledged
        // case only: an unacknowledged transfer -- which this is, since we have never managed to
        // read it -- is kept for the hard cap, so stopping at 24 hours abandoned an item the
        // sender would go on holding for another six days. The bound has to match the longer
        // promise, or the retry gives up while the file is still there.
        if (attempt > TRANSFER_RETRIES && retryElapsed(uri) > TRANSFER_HARD_CAP_MILLIS) {
            forgetRetryStart(uri);
            return;
        }
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                try {
                    DataItemBuffer items = Tasks.await(
                            Wearable.getDataClient(context.getApplicationContext()).getDataItems(uri),
                            TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    try {
                        if (items.getCount() == 0) {
                            // An authoritative EMPTY answer: the item is gone. Typically the
                            // sender's retention sweep reached it while its asset was still
                            // unreadable here. Rescheduling would then poll a deleted URI every few
                            // minutes for the rest of the retention window, on the single timer
                            // that startup replay and cleanup also use, and no attempt could ever
                            // succeed. Stop and forget the chain.
                            forgetRetryStart(uri);
                            return;
                        }
                        for (DataItem item : items) {
                            Transfer t = decodeTransferOnce(context, item);
                            // Recheck authorship. The listener suppressed this as our own echo and
                            // only an unreadable Asset sent it down the retry path -- arriving here
                            // does not make our own transfer someone else's.
                            if (t.payload != null && isLocallyAuthored(context, uri.getHost())) {
                                // Decoded, and it is ours. Nothing to deliver -- but the chain must
                                // STOP here rather than fall through and reschedule: it would
                                // otherwise reopen and re-read our own file every few minutes for
                                // the whole 24-hour retention window, occupying the shared transfer
                                // timer and re-reading a potentially large asset each time.
                                forgetRetryStart(uri);
                                return;
                            }
                            if (t.payload != null) {
                                long tseq = sequenceOf(valueOrTransferMap(item));
                                if (claimTransfer(context, uri, tseq)) {
                                    // Confirmed from INSIDE the delivery, not from its dispatch.
                                    // deliverDataChangedTracked returning true only means the
                                    // runnable reached the EDT; a process death before it ran would
                                    // persist a claim for a file the app never saw and suppress the
                                    // redelivery that would have replaced it.
                                    final Uri claimed = uri;
                                    final long claimedSeq = tseq;
                                    // The first sighting deliberately did NOT start the app -- the
                                    // asset was unreadable and nothing was deliverable yet. This
                                    // retry is the point at which it becomes deliverable, so it is
                                    // the point that owes the launch.
                                    ensureAppRunning();
                                    WearableConnection.deliverDataChangedTracked(
                                            t.logicalPath, t.payload, new Runnable() {
                                                public void run() {
                                                    confirmTransferDelivered(context, claimed, claimedSeq, true);
                                                }
                                            }, new Runnable() {
                                                public void run() {
                                                    // The direct callback path releases an evicted
                                                    // claim; this one has to as well. It is in fact
                                                    // the worse case: the retry chain stops right
                                                    // below, so without this nothing would look at
                                                    // this item again for the life of the process.
                                                    relinquishTransfer(context, claimed);
                                                }
                                            });
                                }
                                // Released on SUCCESS as well as on give-up. Every transfer has a
                                // sequence-suffixed URI, so a receiver that handles many slow
                                // transfers would otherwise keep one entry per transfer forever.
                                forgetRetryStart(uri);
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
        }, retryDelay(attempt));
    }

    /**
     * Backoff for transfer retries: linear while the failure may be momentary, then capped.
     *
     * <p>Capped rather than exponential because the wait has to stay short enough to catch an
     * Asset that finishes downloading hours in -- an unbounded doubling would be sleeping for
     * hours by then and the retention window would expire mid-sleep.</p>
     */
    private static long retryDelay(int attempt) {
        long linear = TRANSFER_RETRY_MILLIS * attempt;
        return linear > TRANSFER_RETRY_CAP_MILLIS ? TRANSFER_RETRY_CAP_MILLIS : linear;
    }

    private static final long TRANSFER_RETRY_CAP_MILLIS = 5 * 60 * 1000L;
    private static final Map<String, Long> retryStarts = new HashMap<String, Long>();

    private static long retryElapsed(Uri uri) {
        String key = uri.toString();
        long now = System.currentTimeMillis();
        synchronized (retryStarts) {
            Long started = retryStarts.get(key);
            if (started == null) {
                retryStarts.put(key, Long.valueOf(now));
                return 0L;
            }
            return now - started.longValue();
        }
    }

    private static void forgetRetryStart(Uri uri) {
        synchronized (retryStarts) {
            retryStarts.remove(uri.toString());
        }
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
     * Forgets a path's delivery stamp, so a value republished after a removal is delivered even if
     * the publisher's clock produced a lower stamp than the removed value carried.
     *
     * @param path the logical path
     */
    /**
     * Records a delivery stamp outright, replacing whatever was there.
     *
     * <p>Distinct from the outranks-guarded delivery, which refuses to go backwards. After a
     * deletion the surviving item can legitimately carry a LOWER sequence than the winner that was
     * just removed, so the newer-than test would decline to record it and leave the dead winner's
     * stamp in place -- filtering out a later item that sits between the two.
     *
     * @param path the application path
     * @param sequence the surviving item's sequence
     * @param node the surviving item's publishing node
     * @return true when this differs from what was last delivered, and so is worth delivering
     */
    /**
     * Records a delivery stamp unconditionally, reporting whether it changed.
     *
     * <p>Unconditional on purpose, and only correct where the stamp being replaced describes an
     * item that is now GONE -- the deletion-survivor path, where the survivor routinely carries a
     * lower sequence than the winner just removed. Anywhere the recorded stamp may still describe
     * a live newer value, use {@link #setDeliveredSequenceIfOutranks} instead: this method will
     * happily overwrite newer state with older.</p>
     */
    static boolean setDeliveredSequence(String path, long sequence, String node) {
        String stamp = sequence + "|" + (node == null ? "" : node);
        synchronized (deliveredSequences) {
            String previous = deliveredSequences.put(path, stamp);
            return !stamp.equals(previous);
        }
    }

    /**
     * Records a delivery stamp only when it outranks the one recorded now, atomically.
     *
     * <p>This is what a resolution that BLOCKED needs. Between {@code resolveValue()} returning and
     * the caller acting on it, an ordinary Data Layer callback can deliver a newer publication for
     * the same path; replacing the stamp then hands the app an older payload and leaves the older
     * stamp recorded, so the newer value stays hidden behind it. The compare and the replace have
     * to happen under one lock, or the check is just a smaller window.</p>
     *
     * @return true when the stamp was taken and the payload should be delivered
     */
    static boolean setDeliveredSequenceIfOutranks(String path, long sequence, String node) {
        String stamp = sequence + "|" + (node == null ? "" : node);
        synchronized (deliveredSequences) {
            String previous = deliveredSequences.get(path);
            if (previous != null && !outranks(sequence, node,
                    stampSequence(previous), stampNode(previous))) {
                return false;
            }
            String old = deliveredSequences.put(path, stamp);
            return !stamp.equals(old);
        }
    }

    /**
     * Replaces a path's stamp only while it still matches {@code expected}, atomically.
     *
     * <p>The rule the deletion paths need, and neither of the other two primitives expresses it.
     * {@link #setDeliveredSequence} would clobber a newer publication that landed mid-query;
     * {@link #setDeliveredSequenceIfOutranks} would refuse the survivor, because after a deletion
     * the recorded stamp belongs to the item that was just removed and a survivor is very often
     * older than it. Anchoring on the pre-query snapshot gets both: the dead item's stamp is
     * replaced whatever its number, and anything that arrived while we were asking wins instead.</p>
     *
     * @param expected the stamp read before the query, or null if the path had none
     * @return true when the stamp was taken and the payload should be delivered
     */
    static boolean setDeliveredSequenceIfStampUnchanged(String path, String expected,
            long sequence, String node) {
        String stamp = sequence + "|" + (node == null ? "" : node);
        synchronized (deliveredSequences) {
            String current = deliveredSequences.get(path);
            boolean unchanged = current == null ? expected == null : current.equals(expected);
            if (!unchanged) {
                return false;
            }
            String old = deliveredSequences.put(path, stamp);
            return !stamp.equals(old);
        }
    }

    /**
     * Commits an ordering decision AND the delivery it authorises as one step.
     *
     * <p>Doing the compare-and-replace atomically is not enough on its own: between committing the
     * stamp and calling deliverDataChanged, an ordinary callback for a NEWER publication can run,
     * advance the stamp and emit its payload -- and then this caller emits its older payload after
     * it. The cache is left holding the newer stamp, so the newer value is rejected if it is ever
     * seen again, and nothing is left to correct the listener. Ordering the stamps without ordering
     * the deliveries just moves the race one line down.</p>
     *
     * <p>Safe to hold the monitor across the dispatch because
     * {@link WearableConnection#deliverDataChanged} does not run listener code on this thread -- it
     * either parks the delivery on the pending queue or hands it to the EDT. No application code
     * runs under this lock.</p>
     */
    static boolean deliverIfOutranks(String path, long sequence, String node, byte[] payload) {
        synchronized (deliveredSequences) {
            if (!setDeliveredSequenceIfOutranks(path, sequence, node)) {
                return false;
            }
            rememberValue(path, payload);
            WearableConnection.deliverDataChanged(path, payload);
            return true;
        }
    }

    /**
     * Records a suppressed local echo: stamp and snapshot together, or neither.
     *
     * <p>Doing the two separately let them disagree. The stamp update is conditional -- a newer
     * publication recorded meanwhile makes it decline -- while the snapshot write was
     * unconditional, so a rejected echo still replaced the cached payload with its older bytes and
     * a latency-sensitive getData() answered from it indefinitely, even though the delivery stamp
     * already tracked the newer peer value. A newer update landing between the two calls did the
     * same.</p>
     */
    /**
     * Records a locally authored winner after a deletion: the anchored replacement, and the
     * snapshot, as ONE step.
     *
     * <p>Two separate anchored calls would leave a window between them. A peer publication landing
     * there advances the stamp and caches its own payload, and the second call then finds the
     * anchor stale and does nothing -- or, worse, an unanchored one would overwrite the value
     * alone, so {@code getData} answered with local bytes while the stamp and the listener tracked
     * the peer's. Nothing is dispatched: this is the suppressed local-winner path.</p>
     */
    static boolean recordLocalWinnerIfStampUnchanged(String path, String expected, long sequence,
            String node, byte[] payload) {
        synchronized (deliveredSequences) {
            if (!setDeliveredSequenceIfStampUnchanged(path, expected, sequence, node)) {
                return false;
            }
            rememberValue(path, payload);
            return true;
        }
    }

    static boolean recordLocalEcho(String path, long sequence, String node, byte[] payload) {
        synchronized (deliveredSequences) {
            if (!setDeliveredSequenceIfOutranks(path, sequence, node)) {
                return false;
            }
            rememberValue(path, payload);
            return true;
        }
    }

    /**
     * Stores a read result only while the path's delivery stamp is still the one the read started
     * from.
     *
     * <p>An off-EDT getData() blocks, and a delivery landing while it does has already advanced the
     * stamp and may not fire again. Writing the query's older snapshot afterwards would leave every
     * later latency-sensitive read answering with it.</p>
     */
    static void rememberValueIfStampUnchanged(String path, String expected, byte[] payload) {
        synchronized (deliveredSequences) {
            String current = deliveredStamp(path);
            boolean unchanged = current == null ? expected == null : current.equals(expected);
            if (unchanged) {
                rememberValue(path, payload);
            }
        }
    }

    /** As {@link #deliverIfOutranks}, for the deletion paths that anchor on a pre-query stamp. */
    static boolean deliverIfStampUnchanged(String path, String expected, long sequence, String node,
            byte[] payload) {
        synchronized (deliveredSequences) {
            if (!setDeliveredSequenceIfStampUnchanged(path, expected, sequence, node)) {
                return false;
            }
            rememberValue(path, payload);
            WearableConnection.deliverDataChanged(path, payload);
            return true;
        }
    }

    /**
     * Drops the stamp and announces the removal as one step, and only while the stamp is still the
     * one read before the query.
     *
     * <p>{@code expected == null} returns false rather than removing: it means either that the app
     * was never told this path had a value (so a removal would be an event that never happened) or
     * that another deletion event in the same buffer already announced it -- which is what keeps
     * one logical removal from being reported once per replica.</p>
     */
    static boolean deliverRemovalIfStampUnchanged(String path, String expected) {
        synchronized (deliveredSequences) {
            if (expected == null || !forgetDeliveredSequenceIfUnchanged(path, expected)) {
                return false;
            }
            // Drop the snapshot with the stamp, or an EDT getData would keep answering with a value
            // the app has just been told was removed.
            rememberValue(path, null);
            // A sentinel in its place, so the SIBLINGS of this delete find one. Consuming the
            // stamp and leaving the path absent meant the next tombstone of the same wildcard
            // delete -- a replica published by another node -- read as first sight and announced
            // the same logical removal a second time; only the third onwards were suppressed. The
            // stamped branch and the first-sight branch now leave the path in the same state.
            //
            // Harmless to ordering: a sentinel carries no sequence, so stampSequence reads it as
            // the weakest possible value and any real republication outranks it -- which is the
            // same reason dropping the stamp was safe.
            markRemovalAnnounced(path);
            // A removal is dispatched from here, not from the service, so the launch belongs here
            // too: the deleted DataItem is gone from startup replay, so a queued removal that no
            // listener ever drains leaves the app's persisted state holding a value the peer
            // deleted, with nothing left to correct it.
            ensureAppRunning();
            // Spooled rather than queued when nothing can run it: the deleted item is gone from
            // startup replay, so a removal lost with the service process leaves the app holding a
            // value the peer deleted, with nothing left to correct it.
            spoolOrDeliverRemoval(null, path);
            return true;
        }
    }

    /**
     * Drops the ordering baseline and the cached snapshot together after this device's own removal,
     * but only while the baseline is still the one the caller observed.
     *
     * <p>The two have to move as one, anchored on a stamp captured BEFORE anything is cleared. A
     * deferred resolution can deliver a peer republish concurrently with a local tombstone: reading
     * the stamp after clearing the snapshot would read the REPUBLISH's stamp, find it unchanged,
     * and remove it -- so the app holds a value that {@code getData} no longer returns and a later
     * stale replica faces no baseline at all.</p>
     *
     * @param expected the stamp observed before the tombstone was processed, or null if there was
     *                 none
     * @return true when nothing had changed and both were cleared
     */
    static boolean forgetAfterLocalRemoval(String path, String expected) {
        synchronized (deliveredSequences) {
            if (expected == null) {
                // Nothing to remove -- unless a delivery installed a baseline in the meantime, in
                // which case that delivery owns the path now and its snapshot must survive.
                if (hasDeliveredStamp(path)) {
                    return false;
                }
            } else if (!forgetDeliveredSequenceIfUnchanged(path, expected)) {
                return false;
            }
            rememberValue(path, null);
            return true;
        }
    }

    /**
     * Records a deletion against a resolution that is ALREADY pending, without scheduling one.
     *
     * <p>Used when an inline deletion query succeeds while an older first-sight resolution is still
     * in flight for the same path. That older query may have captured the item before it was
     * deleted; without this it finishes as a non-deletion, reports no missed upgrade, and delivers
     * a deleted item against the now-empty baseline, with no later callback guaranteed to correct
     * it. Bumping the generation makes the finishing task see that it did not act on the latest
     * deletion and resolve again.</p>
     */
    static void notePendingDeletion(String path) {
        if (path == null) {
            return;
        }
        synchronized (pendingWinnerPaths) {
            if (pendingWinnerPaths.contains(path)) {
                deletionPaths.put(path, Long.valueOf(++deletionGeneration));
            }
        }
    }

    /**
     * Records that a removal has been announced for a path with no stamp of its own, returning
     * false when one already was.
     *
     * <p>For the cold-process case: the first event a fresh process handles is a deletion, so there
     * is no delivery stamp to drop and the ordinary atomic path declines. Announcing needs a way to
     * happen exactly once all the same, because one wildcard delete produces a tombstone per
     * replica. The sentinel is a stamp like any other, so those later tombstones take the ordinary
     * route and dedupe against it.</p>
     */
    static boolean markRemovalAnnounced(String path) {
        synchronized (deliveredSequences) {
            if (deliveredSequences.containsKey(path)) {
                return false;
            }
            deliveredSequences.put(path, REMOVAL_ANNOUNCED);
            // Removed before it is put back, so the entry moves to the END of the eviction order.
            // An insertion-ordered LinkedHashMap does not reposition an existing key on put, so a
            // path removed, republished and removed again kept the FIRST removal's place in the
            // queue -- and could be evicted by the next unrelated deletion, leaving a delayed
            // sibling of the second delete to read as first sight and announce a duplicate.
            removalSentinels.remove(path);
            // Tracked so it can be retired. Unlike a real stamp, a sentinel describes a path that
            // is GONE, so nothing will ever publish over it -- on an app that deletes
            // record-specific paths, every one of them would sit in the map for the life of the
            // process. The stamps themselves stay uncapped for the reason above; it is only these
            // that need a bound, and they are the only entries safe to have one.
            removalSentinels.put(path, Boolean.TRUE);
            return true;
        }
    }

    /// Announces a removal that a resolution has just confirmed, by whichever of the two rules
    /// applies.
    ///
    /// Shared because the inline handler and the deferred resolver both end here and only one of
    /// them knew the first-sight rule. The deferred path called
    /// {@link #deliverRemovalIfStampUnchanged} alone, which refuses a null expected stamp -- so a
    /// fresh process whose first callback for a path was a deletion, and whose inline attempts had
    /// failed, retired the resolution silently. A deleted item is absent from the startup
    /// enumeration and produces no further callback, so an app that persisted the value across the
    /// restart kept showing it indefinitely.
    static void announceResolvedRemoval(String path, String before) {
        // Under the delivery-stamp monitor for the whole first-sight branch, as the stamped path
        // already was. Taking the lock only for markRemovalAnnounced let a publication commit
        // between the sentinel and the dispatch: it replaced the sentinel, cached its value and
        // queued the change, and then this branch cleared that fresh cache entry and queued a
        // removal behind it -- leaving the listener removed while the durable value existed, with
        // the publication's own callback already spent.
        //
        // Safe to hold across the dispatch for the same reason the other paths are:
        // deliverDataRemoved runs no listener code on this thread, it queues or hands to the EDT.
        synchronized (deliveredSequences) {
            if (before == null) {
                if (markRemovalAnnounced(path)) {
                    rememberValue(path, null);
                    // The stamped branch below reaches deliverRemovalIfStampUnchanged, which starts
                    // the app; this one dispatches directly and did not. A cold process whose FIRST
                    // event for a path is its deletion took exactly this branch, so the removal was
                    // queued with no lifecycle to drain it -- and the deleted item is absent from
                    // startup replay, so the app's persisted state kept a value the peer removed.
                    ensureAppRunning();
                    spoolOrDeliverRemoval(null, path);
                }
            } else if (!isRemovalAnnounced(before)) {
                // A sentinel means this logical removal has already been reported, by an earlier
                // tombstone of the same wildcard delete. The ordinary path CONSUMES the stamp it
                // finds and announces, so passing one in would report the removal again, once per
                // replica.
                deliverRemovalIfStampUnchanged(path, before);
            }
        }
    }

    /// Stands in for "this path's removal has been reported" where no real stamp exists. A real
    /// stamp is "sequence|node", so a value with neither cannot collide with one.
    private static final String REMOVAL_ANNOUNCED = "removed";

    /// Whether a stamp is the removal sentinel rather than a real delivery.
    ///
    /// The remaining tombstones of one wildcard delete must not be passed to the ordinary removal
    /// path: that path CONSUMES the stamp it finds and announces, so it would report the same
    /// logical removal again -- and re-announce once per replica, alternating between recreating
    /// and consuming the sentinel.
    static boolean isRemovalAnnounced(String stamp) {
        return REMOVAL_ANNOUNCED.equals(stamp);
    }

    /** The recorded stamp for a path, or null -- an opaque snapshot for {@link #forgetDeliveredSequenceIfUnchanged}. */
    static String deliveredStamp(String path) {
        synchronized (deliveredSequences) {
            return deliveredSequences.get(path);
        }
    }

    /**
     * Drops a path's stamp only if it still matches {@code expected}.
     *
     * <p>Guards the other half of the same race: a resolution that came back empty may be reporting
     * a path that a concurrent publication has since refilled, and announcing a removal for it
     * would be wrong in the one direction the app cannot recover from.</p>
     *
     * @return true when the stamp was unchanged and has now been dropped
     */
    static boolean forgetDeliveredSequenceIfUnchanged(String path, String expected) {
        synchronized (deliveredSequences) {
            String current = deliveredSequences.get(path);
            boolean unchanged = current == null ? expected == null : current.equals(expected);
            if (unchanged) {
                deliveredSequences.remove(path);
            }
            return unchanged;
        }
    }

    private static long stampSequence(String stamp) {
        int bar = stamp.indexOf('|');
        try {
            return Long.parseLong(bar < 0 ? stamp : stamp.substring(0, bar));
        } catch (RuntimeException unparsable) {
            // Treat an unreadable stamp as the weakest possible, so a real value outranks it rather
            // than being refused by a record nobody can interpret.
            return Long.MIN_VALUE;
        }
    }

    private static String stampNode(String stamp) {
        int bar = stamp.indexOf('|');
        if (bar < 0 || bar + 1 >= stamp.length()) {
            return null;
        }
        return stamp.substring(bar + 1);
    }

    /**
     * Whether this process has delivered anything for a path yet.
     *
     * <p>An empty baseline is not the same as "this event is newer". After a restart the map is
     * empty, so the first event for a path would be accepted whatever it is -- including a
     * lower-ranked replica while a higher-ranked one exists on another node.
     *
     * @param path the application path
     * @return true when a delivery stamp is already recorded
     */
    static boolean hasDeliveredStamp(String path) {
        synchronized (deliveredSequences) {
            return deliveredSequences.containsKey(path);
        }
    }

    /// Forgets every recorded delivery, so a full replay treats each path as first sight.
    ///
    /// Only for the overflow case: the pending-delivery record lost track of which paths were
    /// discarded, and re-offering everything still published is the honest recovery. A path the app
    /// already has arrives again -- a duplicate an app can recognise, against a missing update it
    /// cannot.
    static void forgetAllDeliveredSequences() {
        synchronized (deliveredSequences) {
            deliveredSequences.clear();
            removalSentinels.clear();
        }
    }

    static void forgetDeliveredSequence(String path) {
        synchronized (deliveredSequences) {
            deliveredSequences.remove(path);
        }
    }

    /**
     * Drops a path's stamp and reports whether there was one, so a removal is announced exactly
     * once.
     *
     * <p>Deleting a replicated path deletes every authority's copy, and the Data Layer can hand
     * back one TYPE_DELETED event per item in a single buffer. Announcing per event repeats one
     * logical removal N times, and a listener that treats removal as an event -- clearing a cache,
     * cancelling something -- would act on it N times. Making the announcement conditional on this
     * drop coalesces them: the first event takes the stamp, the rest find nothing and stay quiet.
     *
     * <p>It also refuses to invent removals. No recorded stamp means the app was never told this
     * path had a value, and telling it the value went away would be an event that never happened.</p>
     *
     * @return true when a stamp was present and has now been dropped
     */
    static boolean forgetDeliveredSequenceIfPresent(String path) {
        synchronized (deliveredSequences) {
            return deliveredSequences.remove(path) != null;
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
    /**
     * Delivery stamps per path. A plain HashMap, deliberately SEPARATE from {@link #transferClaims}
     * and deliberately NOT size-capped.
     *
     * <p>Recorded because it has been read the other way: transfer claims live in their own bounded
     * LRU, so a burst of file transfers cannot evict a replicated path's ordering stamp. They are
     * different lifetimes -- a claim is one-shot and expendable once the sender's retention window
     * passes, an ordering stamp must outlive anything that could arrive for that path -- which is
     * exactly why they are not one map. Evicting a stamp would let a stale item pass the ordering
     * test, so this one grows with the number of distinct paths an app uses, which is bounded by
     * the app rather than by traffic.</p>
     */
    private static final Map<String, String> deliveredSequences = new HashMap<String, String>();

    /// How many removal sentinels are kept. Generous: the sentinel exists to dedupe the tombstones
    /// of ONE wildcard delete, which is one per replica -- a handful, arriving in one buffer -- so
    /// nothing real is riding on the two hundred and fifty-sixth oldest.
    private static final int MAX_REMOVAL_SENTINELS = 256;

    /// The paths currently holding a removal sentinel, oldest first, so the oldest can be retired.
    ///
    /// Sentinels are the one kind of entry in {@link #deliveredSequences} that a later publication
    /// does not replace -- the path is deleted -- so on an app that deletes record-specific paths
    /// they accumulate for the life of the process. Retiring the oldest costs at worst a second
    /// announcement of a removal that happened long enough ago for 256 other paths to have been
    /// deleted since, which is not the same logical delete and so not the duplicate the sentinel
    /// exists to prevent.
    ///
    /// Guarded by the deliveredSequences monitor like every other access to that map.
    private static final Map<String, Boolean> removalSentinels =
            new LinkedHashMap<String, Boolean>(16, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    if (size() <= MAX_REMOVAL_SENTINELS) {
                        return false;
                    }
                    // Only while it is still a sentinel. A real stamp can have replaced it -- a
                    // path deleted and later republished -- and evicting THAT would drop an
                    // ordering baseline, which is what lets a stale item through.
                    if (REMOVAL_ANNOUNCED.equals(deliveredSequences.get(eldest.getKey()))) {
                        deliveredSequences.remove(eldest.getKey());
                    }
                    return true;
                }
            };

    /**
     * Transfer claims, bounded separately from the replicated ordering stamps.
     *
     * <p>They shared one map, which meant a burst of transfers could evict a replicated path's
     * ordering stamp -- and losing that is a correctness bug, because a reconnect supplying an older
     * item for the path would then pass the newer-than test and overwrite the current value.
     * Replicated paths are few and application-defined, so they are held unbounded; transfer keys
     * are unbounded by nature (every transfer has its own URI) and are what needs the cap. Evicting
     * a transfer claim only risks delivering a very old re-synced transfer twice.
     */
    private static final Map<String, String> transferClaims =
            new java.util.LinkedHashMap<String, String>(64, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_TRANSFER_CLAIMS;
                }
            };

    private static final int MAX_TRANSFER_CLAIMS = 2048;

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
        /** The node that published it, which is also how a sequence tie is broken. */
        final String node;

        ResolvedValue(byte[] payload, long sequence, String node) {
            this.payload = payload;
            this.sequence = sequence;
            this.node = node;
        }
    }

    /**
     * Whether one publication beats another, ties included.
     *
     * <p>Two devices that publish the same path in the same millisecond before observing each other
     * produce identical sequences -- the logical clock only orders them once one has seen the other.
     * Without a tiebreak, {@code getData()} keeps whichever item the buffer happened to yield first
     * while the delivery path keeps whichever arrived first, so the getter and the listener can
     * disagree and two watches can settle on different values for the same path.
     *
     * <p>The publishing node id is the tiebreak: it is stable, it is visible to every device, and
     * comparing it lexicographically makes every device pick the same winner.
     *
     * @param seq the candidate's sequence
     * @param node the candidate's publishing node
     * @param bestSeq the incumbent's sequence
     * @param bestNode the incumbent's publishing node
     * @return true when the candidate should win
     */
    static boolean outranks(long seq, String node, long bestSeq, String bestNode) {
        if (seq != bestSeq) {
            return seq > bestSeq;
        }
        if (node == null) {
            return false;
        }
        return bestNode == null || node.compareTo(bestNode) > 0;
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
    /**
     * {@link #resolveValue} with a couple of retries.
     *
     * <p>For the first event after a restart the answer matters more than the latency: falling back
     * to the delivered item can hand the app a lower-ranked replica, and the winning item -- being
     * unchanged -- may never produce another callback, so the listener would stay wrong while
     * {@code getData()} said otherwise. Callers are Play services callback threads, never the EDT.
     *
     * @param context any context
     * @param path the application path
     * @return the winner, or null when the path is genuinely empty
     * @throws java.io.IOException when every attempt failed
     */
    static ResolvedValue resolveValueWithRetry(Context context, String path)
            throws java.io.IOException {
        java.io.IOException last = null;
        for (int attempt = 0; attempt <= RESOLVE_RETRIES; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(RESOLVE_RETRY_MILLIS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                return resolveValue(context, path);
            } catch (java.io.IOException failed) {
                last = failed;
            }
        }
        throw last == null ? new java.io.IOException("could not resolve " + path) : last;
    }

    private static final int RESOLVE_RETRIES = 2;
    private static final long RESOLVE_RETRY_MILLIS = 500;

    /** Deferred winner resolution: attempts after the inline ones, and the gap between them. */
    private static final int WINNER_RETRIES = 4;
    private static final long WINNER_RETRY_MILLIS = 3000;

    /**
     * Paths with a deferred resolution already in flight. Several events can arrive for one path
     * while the Data Layer is unreachable -- each would otherwise start its own retry chain against
     * the same path.
     */
    private static final java.util.Set<String> pendingWinnerPaths = new java.util.HashSet<String>();

    /**
     * Resolves the winning item for a path later, after the inline attempts have all failed.
     *
     * <p>The alternative -- handing the app the event we happened to receive -- is not safe here.
     * That event may be a lower-ranked replica, and the winning item, being unchanged, may never
     * produce another callback: the listener would then disagree with {@link #getData} for the life
     * of the process, with nothing to correct it. Waiting delivers late; guessing delivers wrong and
     * stays wrong.
     *
     * <p>Runs on the transfer timer rather than {@link #replyTimer} for the reason given there: each
     * attempt blocks on a Data Layer query, and the reply timer also owns every pending reply
     * deadline.
     */
    static void scheduleWinnerResolution(Context context, String path) {
        scheduleWinnerResolution(context, path, false);
    }

    /**
     * @param afterDeletion the pending event was a deletion, so "nothing there" is itself the
     *      answer and has to be delivered as a removal. On the first-sight path an empty result
     *      means only that there is nothing to announce, and announcing a removal for a path the
     *      app was never told about would invent an event.
     */
    static void scheduleWinnerResolution(Context context, String path, boolean afterDeletion) {
        if (context == null || path == null) {
            return;
        }
        synchronized (pendingWinnerPaths) {
            boolean fresh = pendingWinnerPaths.add(path);
            // Upgrade to deletion state even when a resolution is already pending, and do it
            // BEFORE the early return. Coalescing on the path alone loses the reason: a first-sight
            // change can schedule a non-deletion resolution, a deletion for the same path can
            // arrive before that timer runs, and the flag would be dropped -- so an empty result
            // would be read as "nothing to announce" while the deletion was the LAST event the
            // Data Layer will send, leaving the listener holding a value that no longer exists.
            //
            // Only ever set, never cleared: once a deletion is folded into a pending resolution,
            // a later non-deletion caller must not downgrade it back.
            if (afterDeletion) {
                // Numbered, not flagged. A second deletion arriving while a resolution is in flight
                // used to re-add an entry that was already there, so it left no trace: the running
                // task had itself acted on a deletion, reported no missed upgrade, and cleared the
                // only pending marker -- while its query predated the republication and could not
                // have seen the newer deletion at all. The generation lets the finishing task
                // notice that the deletion it handled is no longer the latest one.
                deletionPaths.put(path, Long.valueOf(++deletionGeneration));
            }
            if (!fresh) {
                return;
            }
        }
        scheduleWinnerResolution(context, path, 1);
    }

    private static void scheduleWinnerResolution(final Context context, final String path,
            final int attempt) {
        // Its OWN timer, not the transfer timer. Deletion chains now run until they resolve, and
        // each attempt can block for the full Tasks.await timeout -- so a batch of deletions during
        // an outage would hold the shared timer thread for seconds per round and stall the file
        // transfer retries that share it. Two independent failure domains should not queue behind
        // each other.
        winnerTimer.schedule(new java.util.TimerTask() {
            public void run() {
                try {
                    // Snapshot BEFORE the query. resolveValue() blocks, and an ordinary callback can
                    // deliver a newer publication for this path while it does; both branches below
                    // have to notice that rather than act on a view of the world that has expired.
                    String before = deliveredStamp(path);
                    // Read ONCE, and remember what we acted on. Calling wasAfterDeletion() again
                    // below would let an upgrade landing mid-task change the rule half way through.
                    // The generation is captured with it, so a SECOND deletion arriving while this
                    // query is in flight is distinguishable from the one being handled.
                    long actedGeneration;
                    synchronized (pendingWinnerPaths) {
                        actedGeneration = deletionGenerationFor(path);
                    }
                    boolean afterDeletion = actedGeneration != 0L;
                    ResolvedValue winner = resolveValue(context, path);
                    if (winner != null) {
                        // Two different rules, because the recorded stamp means two different things.
                        //
                        // After a DELETION the stamp describes the item that was just removed, and
                        // the survivor routinely carries a LOWER sequence than it -- so an outranks
                        // test rejects the survivor, nothing is delivered, and the listener keeps
                        // showing the deleted value while later publications stay suppressed until
                        // they climb past a dead item's number. It has to replace instead. But it
                        // still must not clobber a NEWER live delivery that landed while this query
                        // was in flight, so it replaces only while the stamp is still the one we
                        // saw before asking.
                        //
                        // Otherwise the stamp describes a live value, and the ordinary monotonic
                        // rule is right.
                        // A winner published by THIS device is recorded, never dispatched. The
                        // inline callback path applies that rule and this shared resolver did not,
                        // so any route into it -- a retry after an inline failure, a lower-ranked
                        // peer replica scheduling a resolution that our own item then wins, the
                        // startup replay -- could hand the app its own write as a peer change.
                        // Recording the stamp still matters: it is what stops the same item being
                        // resolved again and again.
                        if (isLocallyAuthored(context, winner.node)) {
                            // Recorded by the SAME rule the dispatching branches use -- only the
                            // dispatch is suppressed, not the bookkeeping.
                            //
                            // After a deletion that means the anchored REPLACEMENT, not the
                            // monotonic one: the stamp describes the item that was just removed and
                            // a survivor routinely carries a lower sequence, so an outranks test
                            // would leave the baseline sitting on a dead item and filter out every
                            // later peer publication beneath it. And both updates go through one
                            // atomic call, because splitting the stamp from the snapshot let a peer
                            // publication land in between -- advancing the stamp and caching its
                            // payload -- after which this older resolver overwrote the value alone
                            // and getData answered with stale local bytes the listener never saw.
                            if (afterDeletion) {
                                recordLocalWinnerIfStampUnchanged(path, before, winner.sequence,
                                        winner.node, winner.payload);
                            } else {
                                recordLocalEcho(path, winner.sequence, winner.node, winner.payload);
                            }
                        } else if (afterDeletion) {
                            deliverIfStampUnchanged(path, before, winner.sequence, winner.node,
                                    winner.payload);
                        } else {
                            deliverIfOutranks(path, winner.sequence, winner.node, winner.payload);
                        }
                    } else if (afterDeletion) {
                        // Empty, and the announcement is anchored on the stamp read before the
                        // query. Announcing a removal for a path a concurrent publication has since
                        // refilled is the one error the app cannot recover from, so the drop, the
                        // check and the announcement are one atomic step -- and dropping the stamp
                        // stays coupled to the removal, since a value republished later with a
                        // lower sequence must not be filtered as older.
                        //
                        // Through the shared rule, so first sight is handled here too: this branch
                        // called deliverRemovalIfStampUnchanged directly, which refuses a null
                        // expected stamp, and retired the resolution without telling anyone.
                        announceResolvedRemoval(path, before);
                    }
                    if (finishPendingWinner(path, actedGeneration)) {
                        // A deletion upgrade arrived while this task was running and we applied the
                        // non-deletion rule, so the deleted winner may still be recorded as
                        // delivered. The scheduler that set the flag saw the path already pending
                        // and scheduled nothing, so if we simply cleared the state here that
                        // deletion would have no task left to resolve it, and no later callback
                        // either -- a deleted value shown indefinitely. Run again for it.
                        scheduleWinnerResolution(context, path, true);
                    }
                } catch (Throwable stillUnavailable) {
                    if (attempt >= WINNER_RETRIES) {
                        // A DELETION is retried until it RESOLVES, with no deadline.
                        //
                        // Borrowing TRANSFER_RETENTION_MILLIS here was wrong twice over: it is the
                        // lifetime of a transfer DataItem, which has nothing to do with a deletion,
                        // and any deadline at all reintroduces the same permanent staleness. The
                        // deleted item produces no further callback once connectivity returns, so
                        // whatever bound is chosen, an outage that outlasts it leaves the dead
                        // value's stamp and cached payload in place with nothing left to correct
                        // them.
                        //
                        // The cost of retrying is one task per affected path on a shared daemon
                        // timer, waking at the capped backoff and doing nothing but a failing query
                        // while the Data Layer is down. It stops the moment the query answers --
                        // survivor or empty, both are resolutions -- so a reachable Data Layer ends
                        // it immediately. That is a cheap price for not showing deleted data.
                        if (retireUnlessDeletion(path)) {
                            scheduleWinnerResolution(context, path, attempt + 1);
                            return;
                        }
                        // A non-deletion resolution can stop: the path keeps whatever it already
                        // had, stays unstamped where it was, and the next event resolves afresh.
                        return;
                    }
                    scheduleWinnerResolution(context, path, attempt + 1);
                }
            }
        }, winnerDelay(attempt));
    }

    /// One daemon timer for deferred winner resolution, separate from the transfer retries.
    private static final java.util.Timer winnerTimer =
            new java.util.Timer("cn1-wearable-resolve", true);

    /**
     * Backoff for a deferred resolution, capped.
     *
     * <p>Capped because a deletion chain has no attempt limit any more: multiplying an unbounded
     * counter gave an unbounded delay, so after a long outage the next query would be an hour or
     * more away and restored connectivity would NOT clear the stale deletion promptly -- which is
     * the whole point of keeping the chain alive.</p>
     */
    private static long winnerDelay(int attempt) {
        long linear = WINNER_RETRY_MILLIS * (long) attempt;
        return linear > WINNER_RETRY_CAP_MILLIS ? WINNER_RETRY_CAP_MILLIS : linear;
    }

    private static final long WINNER_RETRY_CAP_MILLIS = 60 * 1000L;

    /**
     * Retires an exhausted resolution unless a deletion is owed one, in a single step.
     *
     * <p>The terminal-failure branch used to test {@link #wasAfterDeletion} and then call
     * clear the markers in a separate step. A deletion arriving between the two saw the path
     * still pending and therefore deliberately scheduled nothing, and the clear that followed
     * dropped both markers -- so the only resolution that deletion would ever get was thrown away
     * and the deleted value stayed on screen indefinitely. This is the same hazard the success
     * path already handles through {@link #finishPendingWinner}.</p>
     *
     * <p>When a deletion is owed the markers are deliberately LEFT in place, so the path never
     * stops looking pending and a concurrent deletion cannot slip a duplicate task in behind the
     * caller's rescheduled one.</p>
     *
     * @return true when a deletion is owed and the caller must schedule the next attempt
     */
    private static boolean retireUnlessDeletion(String path) {
        synchronized (pendingWinnerPaths) {
            if (deletionPaths.containsKey(path) || replayPaths.contains(path)) {
                return true;
            }
            pendingWinnerPaths.remove(path);
            deletionPaths.remove(path);
            return false;
        }
    }

    /**
     * Schedules the winner resolution for a value recovered by the startup replay.
     *
     * <p>Identical to the ordinary first-sight resolution except that it is not allowed to give up.
     * The ordinary policy retires after {@code WINNER_RETRIES} because another event will come
     * along; that is exactly what is NOT true here -- the item is unchanged, so nothing else will
     * announce it, and the enclosing replay pass has already counted its enumeration as successful
     * and will not run again. Retiring on a transient failure would lose the very value this path
     * exists to recover.</p>
     */
    private static void scheduleReplayResolution(Context context, String path) {
        if (path == null) {
            return;
        }
        synchronized (pendingWinnerPaths) {
            replayPaths.add(path);
        }
        scheduleWinnerResolution(context, path);
    }

    /// Paths whose resolution came from the startup replay and must therefore keep retrying.
    /// Guarded by {@link #pendingWinnerPaths}; cleared when the resolution finally completes.
    private static final java.util.Set<String> replayPaths = new java.util.HashSet<String>();

    /**
     * Retires a finished resolution, reporting whether a deletion upgrade arrived too late to be
     * honoured by it.
     *
     * <p>The read of the flag and the release of the pending marker have to be one step. Between a
     * task reading "not a deletion" and clearing its pending state, a deletion whose inline
     * attempts failed can set the flag -- and because the path still looks pending, that scheduler
     * deliberately schedules nothing. Clearing the flag on the way out would then discard a
     * deletion that has no task left to resolve it and no callback coming, leaving a deleted value
     * on screen indefinitely.</p>
     *
     * @param actedOnDeletion whether this task applied the deletion rule
     * @return true when a deletion upgrade was missed and a fresh resolution is owed
     */
    private static boolean finishPendingWinner(String path, long actedOnGeneration) {
        synchronized (pendingWinnerPaths) {
            // A resolution that COMPLETED discharges the replay's obligation, whatever it found.
            replayPaths.remove(path);
            long current = deletionGenerationFor(path);
            // Missed when a deletion is recorded that this task did not act on -- either it acted
            // on none (generation 0) or it acted on an OLDER one, which is the republish-then-delete
            // case: the task's query predates the second deletion, so its result cannot speak for
            // it, and treating "I handled a deletion" as "I handled every deletion" discarded the
            // newer one's only resolution.
            boolean missed = current != 0L && current != actedOnGeneration;
            pendingWinnerPaths.remove(path);
            deletionPaths.remove(path);
            return missed;
        }
    }

    /**
     * Paths whose pending resolution came from a deletion. Kept beside
     * {@link #pendingWinnerPaths} and under the same monitor so the flag cannot outlive the
     * resolution that owns it.
     */
    private static final Map<String, Long> deletionPaths = new HashMap<String, Long>();

    /// Ever-increasing, so two deletions of the same path are distinguishable. Guarded by
    /// {@link #pendingWinnerPaths}.
    private static long deletionGeneration;

    /// The deletion generation a task is acting on, or 0 when it is not a deletion resolution.
    /// Guarded by {@link #pendingWinnerPaths}.
    private static long deletionGenerationFor(String path) {
        Long g = deletionPaths.get(path);
        return g == null ? 0L : g.longValue();
    }

    private static boolean wasAfterDeletion(String path) {
        synchronized (pendingWinnerPaths) {
            return deletionPaths.containsKey(path);
        }
    }

    static ResolvedValue resolveValue(Context context, String path) throws java.io.IOException {
        try {
            Uri uri = new Uri.Builder().scheme("wear").authority("*").path(dataPath(path)).build();
            DataItemBuffer items = Tasks.await(
                    Wearable.getDataClient(context.getApplicationContext()).getDataItems(uri),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                byte[] best = null;
                long bestSeq = Long.MIN_VALUE;
                String bestNode = null;
                for (DataItem item : items) {
                    DataMap map = valueMap(item);
                    if (map == null) {
                        continue;
                    }
                    long seq = sequenceOf(map);
                    String node = item.getUri().getHost();
                    if (best == null || outranks(seq, node, bestSeq, bestNode)) {
                        best = payloadOf(map);
                        bestSeq = seq;
                        bestNode = node;
                    }
                }
                return best == null ? null : new ResolvedValue(best, bestSeq, bestNode);
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
        long seq = map == null ? Long.MIN_VALUE : map.getLong(SEQUENCE_KEY, Long.MIN_VALUE);
        // Every stamp we read raises our own clock, so a peer whose clock is ahead cannot keep
        // winning; reading one of our own items is a no-op because it can never exceed our counter.
        observeSequence(seq);
        return seq;
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
    static boolean claimTransfer(Context context, Uri uri, long sequence) {
        if (uri == null) {
            return true;
        }
        // Any record at all counts, whatever its age -- the same rule the startup replay uses, and
        // for the same reason. Reaching here means the item is in front of us, so it is still
        // published; the sender retries a failed retention deletion indefinitely, so a claim that
        // merely aged out says nothing about whether the app already received the payload. Age
        // alone would have let a normal callback redeliver a one-shot file after a cold start.
        // Restamped so pruning cannot drop it while the item it describes is demonstrably alive.
        if (hasAnyClaim(context, uri)) {
            refreshClaim(context, uri);
            return false;
        }
        // Keyed by the publishing node as well as the path. Two devices may transfer the same
        // logical path and file name; their items differ only in the Uri authority, so dropping it
        // would treat the two as one stream and discard the second sender's file whenever its
        // sequence did not happen to exceed the first's.
        String key = uri.getHost() + ":" + uri.getPath();
        synchronized (transferClaims) {
            String previous = transferClaims.get(key);
            if (previous == null) {
                // Not in memory does not mean not delivered. A transfer stays published as a
                // durable DataItem for the sender's whole retention window, and the Data Layer
                // re-delivers it on the next connection -- so a receiver that restarted in between
                // would hand the app the same one-shot file again and repeat whatever the app does
                // with it. The claim has to outlive the process for the contract to mean anything.
                previous = persistedClaim(context, key);
            }
            if (previous != null) {
                // seq|node in memory, seq|node|receivedAt on disk -- so the node is delimited on
                // BOTH sides, not "everything after the first bar". Taking the rest of the string
                // would fold the receipt time into the node id and make every persisted claim
                // compare unequal to the live one.
                int split = previous.indexOf('|');
                int nodeEnd = previous.indexOf('|', split + 1);
                if (nodeEnd < 0) {
                    nodeEnd = previous.length();
                }
                long prevSeq = Long.parseLong(previous.substring(0, split));
                String prevNode = previous.substring(split + 1, nodeEnd);
                if (!outranks(sequence, uri.getHost(), prevSeq,
                        prevNode.length() == 0 ? null : prevNode)) {
                    return false;
                }
            }
            transferClaims.put(key, sequence + "|" + (uri.getHost() == null ? "" : uri.getHost()));
            // Claimed in memory only. The DURABLE claim waits until the payload has actually
            // reached a listener -- see confirmTransferDelivered.
            return true;
        }
    }

    /**
     * Makes a transfer's claim durable, once the payload is no longer only in this process.
     *
     * <p>Persisting at claim time was wrong in the one direction that cannot be recovered from. A
     * transfer can wake this service in a cold process, and if Android refuses the background
     * activity launch the payload sits in WearableConnection's in-memory pending queue. Kill the
     * process before the user opens the app and the payload is gone -- while the persisted claim
     * suppresses the Data Layer redelivery that would have replaced it, losing a transfer that the
     * durable-item design exists to guarantee.
     *
     * <p>So the claim is only written once delivery reached a registered listener. If it was
     * parked, the in-memory claim still prevents a duplicate within this process, and a redelivery
     * after a restart is allowed through. That can hand the app a file twice if it did drain the
     * queue before dying -- deliberately the direction to err in, because a duplicate is something
     * an app can recognise and a lost one-shot file is not.</p>
     */
    static void confirmTransferDelivered(Context context, Uri uri, long sequence,
            boolean reachedListener) {
        if (uri == null || !reachedListener) {
            return;
        }
        final String key = uri.getHost() + ":" + uri.getPath();
        // The persisted form carries a RECEIPT TIME that the in-memory form does not need. The
        // stamp is a Lamport sequence, and observeSequence deliberately drags that ahead of wall
        // time whenever a peer's clock is ahead -- so comparing it against a wall-clock cutoff
        // would keep such a claim until real time caught up with a fabricated future, and since
        // every transfer gets a unique sequence-suffixed URI the store would grow without bound.
        // Pruning needs a clock that measures elapsed time; the sequence is not one.
        final String stamp = sequence + "|"
                + (uri.getHost() == null ? "" : uri.getHost())
                + "|" + System.currentTimeMillis();
        final Context c = context;
        final Uri item = uri;
        // OFF the calling thread. This runs from the delivery callback, which the tracked-data path
        // invokes on the EDT once the listener has had the payload -- and commit() is a synchronous
        // disk write, so a slow or contended store froze rendering and input for the duration of
        // every received transfer. The timer is the same single-threaded worker the rest of the
        // transfer bookkeeping uses, so claims still land in the order they were confirmed.
        //
        // The acknowledgement moves with it and stays behind the write: it is what lets the SENDER
        // retire the item, and publishing it from here while the claim was still queued would put
        // it back in the window this ordering exists to close.
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                boolean durable;
                synchronized (transferClaims) {
                    durable = persistClaim(c, key, stamp);
                }
                if (!durable) {
                    // No acknowledgement without a durable claim. The acknowledgement is what lets
                    // the SENDER retire the item, and retiring it while this device has no claim on
                    // disk means a restart here finds neither the claim nor -- eventually -- the
                    // item, so the one-shot transfer is simply lost. Staying silent costs the
                    // sender an item kept until the hard cap and buys a redelivery this device will
                    // accept, which is the recoverable side of the trade. commit() returning false
                    // is rare (a full or unwritable store) and is exactly when this matters.
                    android.util.Log.w("CN1Wearable", "transfer claim for " + key
                            + " was not written; withholding the acknowledgement so the sender"
                            + " keeps the item and can redeliver it");
                    return;
                }
                publishTransferAck(c, item);
            }
        }, 0);
    }

    /// Tells the SENDER that this device has handed the transfer to a listener.
    ///
    /// The claim above is local, so it answers "have I already taken this?" and nothing else. The
    /// sender needs the same fact, and the Data Layer only replicates items, so the acknowledgement
    /// has to be an item of our own. Published from the same place the claim is persisted -- after
    /// the payload actually reached the listener -- so it never vouches for a delivery that did not
    /// happen.
    ///
    /// Fire and forget: a lost acknowledgement costs the sender an item kept until the hard cap,
    /// while blocking here would stall the delivery path on a network round trip.
    private static void publishTransferAck(Context context, Uri uri) {
        String path = uri.getPath();
        if (path == null || !isTransferPath(path)) {
            return;
        }
        try {
            String publisher = uri.getHost();
            if (publisher == null) {
                return;
            }
            PutDataMapRequest ack =
                    PutDataMapRequest.create(transferAckPath(publisher, path));
            ack.getDataMap().putLong(PUBLISHED_AT_KEY, System.currentTimeMillis());
            Wearable.getDataClient(context.getApplicationContext())
                    .putDataItem(ack.asPutDataRequest());
            // A sweep is what deletes these again, once the transfer they vouch for is gone -- and
            // on a RECEIVE-ONLY device nothing else ever arms one. The constructor sweeps at
            // startup and publishing a transfer sweeps after; a device that only takes files does
            // neither, so its acknowledgements accumulated for the life of the process. The
            // sender's deletion of the transfer does not help: the listener's deletion branch does
            // not run a sweep.
            //
            // Coalesced like every other caller, so a burst of deliveries adds no timer entries.
            CN1WearableBridge live = current;
            if (live != null) {
                live.expireOwnTransfers();
            }
        } catch (Throwable unavailable) {
            // The transfer has been delivered either way. Losing the acknowledgement only means the
            // sender holds its copy until the hard cap.
        }
    }

    /**
     * Gives up the in-memory claim on a transfer that was never delivered.
     *
     * <p>Called when the pending-delivery queue evicts a parked transfer to stay under its cap.
     * Dropping the runnable alone left the claim standing, and the claim is precisely what stops
     * the next scan offering the payload again -- so within a live process the file was gone for
     * good, and the sender's retention window could expire before a restart cleared it.</p>
     *
     * <p>Only the in-memory claim is released. The durable one is written on delivery, so an
     * undelivered transfer never had one. A fresh read of the item is then scheduled, because
     * releasing the claim alone does not make the Data Layer say anything new.</p>
     */
    static void relinquishTransfer(Context context, Uri uri) {
        if (uri == null) {
            return;
        }
        String key = uri.getHost() + ":" + uri.getPath();
        synchronized (transferClaims) {
            transferClaims.remove(key);
        }
        // Releasing the claim is necessary but not sufficient. The DataItem has not changed, and an
        // unchanged item produces no further callback -- the unreadable-asset path above exists for
        // exactly that reason -- so on a connection that simply stays up, nothing would ever offer
        // this payload again and it could expire at the sender. Go back and read it.
        scheduleTransferRetry(context, uri);
    }

    private static boolean claimPruneScheduled;

    /// Arms one prune of the durable claim store.
    ///
    /// The store was pruned only from {@code persistClaim}, so a receiver that handled a finite
    /// burst of transfers and then saw no more kept every one of those unique-URI claims forever --
    /// an unbounded permanent SharedPreferences store, despite a documented 24-hour bound. The task
    /// re-arms itself while anything is left, so it stops on its own once the store is empty.
    private static void scheduleClaimPrune(final Context context) {
        if (context == null) {
            return;
        }
        final Context app = context.getApplicationContext();
        synchronized (transferClaims) {
            if (claimPruneScheduled) {
                return;
            }
            claimPruneScheduled = true;
        }
        transferTimer.schedule(new java.util.TimerTask() {
            public void run() {
                synchronized (transferClaims) {
                    claimPruneScheduled = false;
                }
                // A REPLAY PASS, not a bare prune. Pruning by age alone here bypassed the
                // live-item check the startup path was given: a sender retrying a failed deletion
                // can hold an item well past the claim's grace, and dropping the claim while the
                // item is still published lets the next restart deliver that one-shot file again.
                // The replay refreshes the claim of everything still there and prunes only
                // afterwards, so age retires a record exactly when its item is genuinely gone.
                CN1WearableBridge b = current;
                if (b != null) {
                    b.replayOutstandingTransfers();
                } else {
                    // No bridge in this process (a cold service): nothing can enumerate, so leave
                    // the claims alone rather than expiring records that may still be needed.
                    // Storage stays bounded because the next process with a bridge sweeps them.
                    scheduleClaimPrune(app);
                    return;
                }
                if (hasAnyStoredClaim(app)) {
                    scheduleClaimPrune(app);
                }
            }
        }, TRANSFER_RETENTION_MILLIS + 1000L);
    }

    /** Preference store for durable transfer claims; see {@link #claimTransfer}. */
    private static final String CLAIM_PREFS = "cn1.wearable.claims";

    /**
     * The claim recorded for a transfer key in a previous process, or null.
     *
     * <p>Read only on an in-memory miss, so the common path stays a map lookup and the disk read
     * happens once per key per process.</p>
     */
    private static String persistedClaim(Context context, String key) {
        // Takes a Context rather than reading `current`. A redelivered transfer arrives in a COLD
        // service process where no bridge exists yet -- ensureAppRunning only starts the activity,
        // asynchronously -- so keying off `current` returned null and the durable claim was invisible
        // exactly when it matters, handing the app a one-shot file it already received.
        Context c = context;
        if (c == null) {
            CN1WearableBridge b = current;
            c = b == null ? null : b.context;
        }
        if (c == null) {
            return null;
        }
        try {
            String recorded =
                    c.getSharedPreferences(CLAIM_PREFS, Context.MODE_PRIVATE).getString(key, null);
            // Expiry is checked on the way OUT, not only by the sweep. The prune runs on a daemon
            // timer, which dies with the process while the claims outlive it -- so a receiver that
            // took a burst of transfers and then restarted would honour records long past the
            // retention window they were bounded by. An entry older than the window is treated as
            // absent, which is what the sweep would have made it.
            if (recorded == null || !claimExpired(recorded)) {
                return recorded;
            }
            // Also drop it, so the store shrinks on read even if the sweep never runs.
            c.getSharedPreferences(CLAIM_PREFS, Context.MODE_PRIVATE).edit().remove(key).apply();
            return null;
        } catch (Throwable unavailable) {
            return null;
        }
    }

    /// True when a stored {@code seq|node|receivedAt} record is past the retention window, or is
    /// malformed -- an entry with no receipt time cannot be bounded and is not worth trusting.
    /// How long a durable claim is kept BEYOND the sender's retention window.
    ///
    /// The sender does not delete at exactly the window: its deadline sweep fires at retention +
    /// 1s, and coalescing can defer that by another SWEEP_MIN_INTERVAL. Expiring a claim at exactly
    /// the window therefore left a gap in which the item is still published and no longer claimed,
    /// so a receiver restarting inside it replayed a one-shot file the app already had. The grace
    /// covers the whole worst case with room to spare.
    private static final long CLAIM_GRACE_MILLIS = 2 * SWEEP_MIN_INTERVAL_MILLIS + 60 * 1000L;

    private static boolean claimExpired(String recorded) {
        int lastBar = recorded.lastIndexOf('|');
        int firstBar = recorded.indexOf('|');
        if (lastBar <= 0 || lastBar == firstBar) {
            return true;
        }
        try {
            return Long.parseLong(recorded.substring(lastBar + 1))
                    < System.currentTimeMillis() - TRANSFER_RETENTION_MILLIS - CLAIM_GRACE_MILLIS;
        } catch (NumberFormatException unparsable) {
            return true;
        }
    }

    /**
     * Records a claim durably, and prunes claims older than the sender's retention window.
     *
     * <p>Bounded by the same window the sender sweeps its transfers on: once the item itself is
     * gone there is nothing left to re-deliver, so the claim has no one to stop and keeping it
     * would grow this store without limit.</p>
     *
     * @return whether the claim actually reached disk. The caller publishes the acknowledgement
     *         only on true: a false says the sender must keep the item, because this device has
     *         nothing to stop a redelivery with and losing the transfer is the unrecoverable side.
     */
    private static boolean persistClaim(Context context, String key, String stamp) {
        Context c = context;
        if (c == null) {
            CN1WearableBridge b = current;
            c = b == null ? null : b.context;
        }
        if (c == null) {
            return false;
        }
        try {
            android.content.SharedPreferences prefs =
                    c.getSharedPreferences(CLAIM_PREFS, Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor edit = prefs.edit();
            edit.putString(key, stamp);
            // commit(), not apply(). This record is what stops a one-shot transfer being delivered
            // twice, and it has to be on disk BEFORE anything observable says the payload was
            // taken: the acknowledgement published to the sender, and the deletion that follows it.
            // With apply() a process death in that window left no claim while the sender's DataItem
            // was still published, so the next startup replay handed the app the same file again.
            //
            // Called from the confirmation path on a Data Layer worker, never the main thread.
            //
            // The RESULT matters, and ignoring it undid half the point of using commit(): a full or
            // unwritable store returns false, and the caller then acknowledged a transfer this
            // device has no durable claim on.
            if (!edit.commit()) {
                return false;
            }
            // NO inline prune. Writing one claim says nothing about whether OTHER items are still
            // published, and the sender retries a failed deletion indefinitely -- so age-pruning
            // here could drop a live transfer's claim while recording an unrelated one, and the
            // next replay or restart would hand the app that one-shot file again. Every prune now
            // goes through a replay pass that has just refreshed what still exists; the
            // maintenance timer below is what keeps the store bounded.
            scheduleClaimPrune(c);
            return true;
        } catch (Throwable unavailable) {
            // The in-memory claim still holds for this process, but nothing is on disk -- so this
            // is a failure by the only measure the caller cares about.
            return false;
        }
    }

    /// Whether ANY durable claim is recorded for this item, expired or not.
    ///
    /// Used only by the startup replay, which has the item in front of it: if the DataItem is still
    /// published then the sender has not finished retiring it, and a claim that merely aged out
    /// says nothing about whether the app already has the payload.
    private static boolean hasAnyClaim(Context context, Uri uri) {
        if (uri == null || context == null) {
            return false;
        }
        try {
            String key = uri.getHost() + ":" + uri.getPath();
            return context.getSharedPreferences(CLAIM_PREFS, Context.MODE_PRIVATE)
                    .getString(key, null) != null;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    /// Restamps a claim's receipt time, so a claim cannot be pruned out from under an item that is
    /// demonstrably still published.
    private static void refreshClaim(Context context, Uri uri) {
        if (uri == null || context == null) {
            return;
        }
        try {
            String key = uri.getHost() + ":" + uri.getPath();
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(CLAIM_PREFS, Context.MODE_PRIVATE);
            String recorded = prefs.getString(key, null);
            if (recorded == null) {
                return;
            }
            int lastBar = recorded.lastIndexOf('|');
            int firstBar = recorded.indexOf('|');
            if (lastBar <= 0 || lastBar == firstBar) {
                return;
            }
            prefs.edit().putString(key, recorded.substring(0, lastBar + 1)
                    + System.currentTimeMillis()).apply();
        } catch (Throwable unavailable) {
            // Best effort: the claim simply keeps its old receipt time.
        }
    }

    /// Whether the durable store holds anything at all, so the maintenance timer knows to re-arm.
    private static boolean hasAnyStoredClaim(Context context) {
        try {
            return !context.getSharedPreferences(CLAIM_PREFS, Context.MODE_PRIVATE)
                    .getAll().isEmpty();
        } catch (Throwable unavailable) {
            return false;
        }
    }

    /// Prunes expired claims.
    ///
    /// Called ONLY from a successful replay pass, which has just refreshed the claim of every item
    /// still published -- so anything left aged out describes an item that is genuinely gone. There
    /// is deliberately no age-only caller: expiring a claim while its item still exists is what
    /// lets a restart redeliver a one-shot transfer.
    private static void pruneClaims(Context context) {
        if (context == null) {
            return;
        }
        try {
            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(CLAIM_PREFS, Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor edit = prefs.edit();
            pruneInto(prefs, edit);
            edit.apply();
        } catch (Throwable unavailable) {
            // Best effort: the next replay prunes again.
        }
    }

    /// Marks every expired entry for removal on the editor, returning how many survive.
    ///
    /// Reachable ONLY from a replay pass that has already refreshed the claims of items still
    /// published. There is no age-only caller by design: expiring a claim whose item still exists
    /// is what lets a restart redeliver a one-shot transfer.
    private static int pruneInto(android.content.SharedPreferences prefs,
                                 android.content.SharedPreferences.Editor edit) {
        int kept = 0;
        // Same grace as claimExpired: the two must agree, or the sweep would drop a record the
        // lookup still considers live.
        long cutoff = System.currentTimeMillis() - TRANSFER_RETENTION_MILLIS - CLAIM_GRACE_MILLIS;
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            Object v = e.getValue();
            if (!(v instanceof String)) {
                continue;
            }
            // seq|node|receivedAt. Pruned on receivedAt, never on seq -- see
            // confirmTransferDelivered for why the sequence cannot serve as a timestamp. An
            // entry written before this field existed has no receipt time and is dropped:
            // it is at most one retention window old, and keeping an unprunable record
            // forever is the failure being fixed.
            String recorded = (String) v;
            int lastBar = recorded.lastIndexOf('|');
            int firstBar = recorded.indexOf('|');
            if (lastBar <= 0 || lastBar == firstBar) {
                edit.remove(e.getKey());
                continue;
            }
            try {
                if (Long.parseLong(recorded.substring(lastBar + 1)) < cutoff) {
                    edit.remove(e.getKey());
                } else {
                    kept++;
                }
            } catch (NumberFormatException unparsable) {
                edit.remove(e.getKey());
            }
        }
        return kept;
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
