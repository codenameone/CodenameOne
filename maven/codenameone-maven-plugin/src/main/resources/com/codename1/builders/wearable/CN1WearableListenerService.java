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
 */
public class CN1WearableListenerService extends WearableListenerService {

    /**
     * The service has to be exported for Play services to bind it, and there is no binding
     * permission that would narrow that to Play services alone. So rather than trust the caller,
     * every event is checked against the nodes the Data Layer has actually reported: a crafted intent
     * from another app on the device carries a source node that was never among them and is dropped.
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
     * Brings the app process up so its {@code init()} runs and its listeners exist.
     *
     * <p>Android starts this service in a dead process to deliver traffic. Queueing the delivery is
     * only half the answer: without the app itself starting, nothing ever registers a listener and
     * the queue is never drained. Launching is a no-op when the app is already running.
     */
    private void ensureAppRunning() {
        try {
            if (com.codename1.ui.Display.isInitialized()) {
                return;
            }
            android.content.Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(getApplicationInfo().packageName);
            if (launch != null) {
                launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            }
        } catch (Throwable notPermitted) {
            // Background activity starts are restricted on newer Android. The delivery stays in the
            // in-memory queue and is replayed if the app opens while this process is still alive.
            //
            // That is a convenience, not a durability guarantee, and the two transports differ on
            // purpose: replicated data and file transfers are durable in the Data Layer itself -- the
            // item stays published and the next connection re-delivers it -- whereas a live message
            // is best-effort by contract and needs both apps awake, which is what isReachable() and
            // the sender's reply timeout exist to tell it.
        }
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        String path = event.getPath();
        if (path == null || !isFromAKnownNode(event.getSourceNodeId())) {
            return;
        }
        ensureAppRunning();
        if (path.startsWith(CN1WearableBridge.replyPath())) {
            // An answer to a request we sent. The token rides in the path.
            String token = path.substring(CN1WearableBridge.replyPath().length());
            try {
                int replyToken = Integer.parseInt(token);
                // A real answer arrived, so the deadline that would have failed this request is no
                // longer needed; leaving it scheduled holds a task per request for the full timeout.
                CN1WearableBridge.cancelReplyTimeout(replyToken);
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
                // The peer's token is unique only on the peer, so trade it for a locally unique one
                // keyed to the node that asked; two watches can otherwise pick the same number.
                int localToken = CN1WearableBridge.rememberRequestOrigin(
                        peerToken, event.getSourceNodeId());
                // Past the delimiter, not onto it: the encoded application path escapes its own
                // slashes, so this one belongs to the wire format and is not part of the app's path.
                WearableConnection.deliverMessage(
                        CN1WearableBridge.decode(rest.substring(slash + 1)),
                        event.getData(), localToken);
            } catch (NumberFormatException malformed) {
                // Not ours.
            }
            return;
        }
        if (path.startsWith(CN1WearableBridge.messagePath() + "/")) {
            WearableConnection.deliverMessage(
                    CN1WearableBridge.decode(
                            path.substring(CN1WearableBridge.messagePath().length() + 1)),
                    event.getData(), 0);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer events) {
        ensureAppRunning();
        for (DataEvent event : events) {
            android.net.Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            boolean transferItem = CN1WearableBridge.isTransferPath(path);
            if (path == null || !isFromAKnownHost(uri)
                    || (!transferItem && !path.startsWith(CN1WearableBridge.pathPrefix()))) {
                continue;
            }
            if (transferItem) {
                // A file transfer arrives as a DataMap carrying an Asset rather than an inline
                // payload. Turn it back into the WearableMessage the receiver expects; this callback
                // already runs off the main thread, so resolving the asset here is fine.
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    // Our own consumeTransfer, or the sender clearing up. Not an app-visible removal:
                    // the logical path may well still hold a replicated value.
                    continue;
                }
                CN1WearableBridge.Transfer transfer =
                        CN1WearableBridge.decodeTransfer(this, event.getDataItem());
                if (transfer.payload != null
                        && CN1WearableBridge.claimTransfer(uri, CN1WearableBridge.sequenceOf(
                                CN1WearableBridge.valueOrTransferMap(event.getDataItem())))) {
                    // On the path the sender passed to transferFile, not the filename-suffixed
                    // storage path this item happens to live at: a listener routes on what it asked
                    // for. The decoded payload carries the same path internally.
                    //
                    // A transfer is one-shot, so a re-sync of the same item must not deliver twice --
                    // but the duplicate is suppressed locally rather than by deleting the item. The
                    // item belongs to the sender, and deleting it here would propagate: with two
                    // watches paired to one phone, the first to connect would consume the file and
                    // the second would get the tombstone.
                    WearableConnection.deliverDataChanged(transfer.logicalPath, transfer.payload);
                }
                // An unreadable asset delivers nothing now; decodeTransfer has scheduled a re-read,
                // which beats handing the listener DataMap bytes dressed up as a payload.
                continue;
            }
            String appPath = CN1WearableBridge.decode(
                    path.substring(CN1WearableBridge.pathPrefix().length()));
            if (event.getType() == DataEvent.TYPE_DELETED) {
                // The ordering stamp is dropped only once we know the path is genuinely empty --
                // see the branches below. Dropping it here, before the query, meant a query that
                // then FAILED left the path with no stamp at all, so an older item from another
                // authority arriving next would pass the newer-than-delivered test and win.
                // One authority's item going away does not mean the path is gone: both nodes may
                // have published it, and the other item can still be there. Reporting a removal on
                // the strength of this event alone would tell the listener the value disappeared
                // while getData(path) still returned it. Ask what is left and report that instead.
                try {
                    CN1WearableBridge.ResolvedValue remaining =
                            CN1WearableBridge.resolveValue(this, appPath);
                    if (remaining != null) {
                        // Record the survivor's stamp outright -- the state of the path IS this
                        // item now. Using the newer-than test here would decline whenever the
                        // survivor carries a lower sequence than the winner just deleted (which is
                        // ordinary: the winner is gone precisely because it was removed), leaving
                        // the dead item's higher stamp recorded and filtering out any later item
                        // that falls between the two.
                        // Only when the winner actually changed. Deleting a lower-ranked SHADOW
                        // replica leaves the same item winning, and re-announcing a value the app
                        // already holds is a spurious change -- listeners re-render, and anything
                        // that treats a change as an event would act on it twice.
                        if (CN1WearableBridge.setDeliveredSequence(
                                appPath, remaining.sequence, remaining.node)) {
                            WearableConnection.deliverDataChanged(appPath, remaining.payload);
                        }
                    } else {
                        // Genuinely empty, so the stamp can go: a value republished here later with
                        // a lower stamp than the removed one carried must not be filtered as older.
                        CN1WearableBridge.forgetDeliveredSequence(appPath);
                        WearableConnection.deliverDataRemoved(appPath);
                    }
                } catch (java.io.IOException couldNotResolve) {
                    // The follow-up query failed rather than answering "nothing here". Reporting a
                    // removal on that would tell the app a path had gone when another node may still
                    // be publishing it -- and a removal is not recoverable from the app's side. Say
                    // nothing; the next sync re-reports whatever is actually there.
                }
                continue;
            }
            com.google.android.gms.wearable.DataMap value =
                    CN1WearableBridge.valueMap(event.getDataItem());
            if (value == null) {
                // Under our prefix but not written by this API -- nothing to deliver.
                continue;
            }
            if (!CN1WearableBridge.hasDeliveredStamp(appPath)) {
                // First sight of this path in this process -- after a restart there is no baseline,
                // so accepting the event on the strength of "nothing recorded" would hand the app a
                // lower-ranked replica while a higher-ranked item exists on another node, and
                // getData() would immediately disagree. Resolve the actual winner instead.
                try {
                    CN1WearableBridge.ResolvedValue winner =
                            CN1WearableBridge.resolveValueWithRetry(this, appPath);
                    if (winner != null && CN1WearableBridge.setDeliveredSequence(
                            appPath, winner.sequence, winner.node)) {
                        WearableConnection.deliverDataChanged(appPath, winner.payload);
                    }
                    continue;
                } catch (java.io.IOException couldNotResolve) {
                    // Every attempt failed. Deliver the event we were handed so the app is not left
                    // with nothing -- but do NOT record it as the baseline: it may be a lower-ranked
                    // replica, and the winning item, being unchanged, may never produce another
                    // callback. Leaving the path unstamped means the next event for it resolves
                    // properly instead of being judged against a value we are not sure of.
                    com.google.android.gms.wearable.DataMap fallback =
                            CN1WearableBridge.valueMap(event.getDataItem());
                    if (fallback != null) {
                        WearableConnection.deliverDataChanged(
                                appPath, CN1WearableBridge.payloadOf(fallback));
                    }
                    continue;
                }
            }
            if (!CN1WearableBridge.isNewerThanDelivered(
                    appPath, CN1WearableBridge.sequenceOf(value), uri.getHost())) {
                // An older item arriving after a newer one, which a reconnect can do when both nodes
                // publish this path. getData() would return the newer value, so delivering this would
                // make the listener and the getter disagree.
                continue;
            }
            WearableConnection.deliverDataChanged(appPath, CN1WearableBridge.payloadOf(value));
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
