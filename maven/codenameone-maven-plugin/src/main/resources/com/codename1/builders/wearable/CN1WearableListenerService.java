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

    @Override
    public void onMessageReceived(MessageEvent event) {
        String path = event.getPath();
        if (path == null) {
            return;
        }
        if (path.startsWith(CN1WearableBridge.replyPath())) {
            // An answer to a request we sent. The token rides in the path.
            String token = path.substring(CN1WearableBridge.replyPath().length());
            try {
                WearableConnection.deliverReply(Integer.parseInt(token), event.getData(), null);
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
                int token = Integer.parseInt(rest.substring(0, slash));
                WearableConnection.deliverMessage(
                        CN1WearableBridge.decode(rest.substring(slash)), event.getData(), token);
            } catch (NumberFormatException malformed) {
                // Not ours.
            }
            return;
        }
        if (path.startsWith(CN1WearableBridge.messagePath())) {
            WearableConnection.deliverMessage(
                    CN1WearableBridge.decode(path.substring(CN1WearableBridge.messagePath().length())),
                    event.getData(), 0);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) {
            String path = event.getDataItem().getUri().getPath();
            if (path == null || !path.startsWith(CN1WearableBridge.pathPrefix())) {
                continue;
            }
            String appPath = CN1WearableBridge.decode(
                    path.substring(CN1WearableBridge.pathPrefix().length()));
            if (event.getType() == DataEvent.TYPE_DELETED) {
                WearableConnection.deliverDataRemoved(appPath);
            } else {
                WearableConnection.deliverDataChanged(appPath, event.getDataItem().getData());
            }
        }
    }

    @Override
    public void onPeerConnected(com.google.android.gms.wearable.Node peer) {
        WearableConnection.notifyStateChanged();
    }

    @Override
    public void onPeerDisconnected(com.google.android.gms.wearable.Node peer) {
        WearableConnection.notifyStateChanged();
    }
}
