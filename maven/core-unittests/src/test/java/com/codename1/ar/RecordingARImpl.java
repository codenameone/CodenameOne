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
package com.codename1.ar;

import com.codename1.impl.ARImpl;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written recording {@link ARImpl} for the AR unit tests. Counts
 * lifecycle calls, captures the last arguments and exposes the installed
 * {@link ARImpl.EventSink} through fire helpers so tests can push platform
 * events - optionally from a background thread to prove EDT marshaling.
 */
class RecordingARImpl extends ARImpl {

    ARCapabilities capabilities = new ARCapabilities(true, true, true, true, true);
    boolean capabilitiesReturnsNull;

    IOException openFailure;
    ARSessionOptions openedOptions;
    int openCount;
    int closeCount;
    int pauseCount;
    int resumeCount;
    int viewPeerCount;

    EventSink sink;

    ARHitResult[] hitResults = new ARHitResult[0];
    float lastHitX = Float.NaN;
    float lastHitY = Float.NaN;

    int anchorSeq;
    final List<String> createdAnchorIds = new ArrayList<String>();
    final List<String> removedAnchorIds = new ArrayList<String>();
    Object lastHitHandle;

    String lastNodeAnchorId;
    ARNode lastNode;
    int setNodeCount;
    String lastChangedAnchorId;
    ARNode lastChangedNode;
    int nodeChangedCount;

    Boolean permissionResult = Boolean.TRUE;
    Throwable permissionFailure;

    @Override
    public ARCapabilities getCapabilities() {
        return capabilitiesReturnsNull ? null : capabilities;
    }

    @Override
    public void setEventSink(EventSink sink) {
        this.sink = sink;
    }

    @Override
    public void open(ARSessionOptions opts) throws IOException {
        openCount++;
        openedOptions = opts;
        if (openFailure != null) {
            throw openFailure;
        }
    }

    @Override
    public PeerComponent createViewPeer() {
        viewPeerCount++;
        return null;
    }

    @Override
    public void hitTest(float xNorm, float yNorm, AsyncResource<ARHitResult[]> result) {
        lastHitX = xNorm;
        lastHitY = yNorm;
        result.complete(hitResults);
    }

    @Override
    public String createAnchor(ARPose pose) {
        String id = "anchor-" + (++anchorSeq);
        createdAnchorIds.add(id);
        return id;
    }

    @Override
    public String createAnchorFromHit(Object nativeHandle, ARPose pose) {
        lastHitHandle = nativeHandle;
        String id = "hit-anchor-" + (++anchorSeq);
        createdAnchorIds.add(id);
        return id;
    }

    @Override
    public void removeAnchor(String anchorId) {
        removedAnchorIds.add(anchorId);
    }

    @Override
    public void setAnchorNode(String anchorId, ARNode node) {
        setNodeCount++;
        lastNodeAnchorId = anchorId;
        lastNode = node;
    }

    @Override
    public void nodeChanged(String anchorId, ARNode node) {
        nodeChangedCount++;
        lastChangedAnchorId = anchorId;
        lastChangedNode = node;
    }

    @Override
    public void requestPermissions(AsyncResource<Boolean> result) {
        if (permissionFailure != null) {
            result.error(permissionFailure);
        } else {
            result.complete(permissionResult);
        }
    }

    @Override
    public void pause() {
        pauseCount++;
    }

    @Override
    public void resume() {
        resumeCount++;
    }

    @Override
    public void close() {
        closeCount++;
    }

    /**
     * Runs {@code r} on a freshly started background thread and joins it, so
     * tests can prove sink events arriving off the EDT are marshaled.
     */
    void onBackgroundThread(Runnable r) {
        Thread t = new Thread(r, "ar-test-events");
        t.start();
        try {
            t.join(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
