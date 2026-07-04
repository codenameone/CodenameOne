/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/// Active augmented reality session. Obtained from
/// `AR#open(ARSessionOptions)`.
///
/// Only one `ARSession` may be open at a time; opening a second throws
/// `IllegalStateException`. Closing the session releases the camera and
/// tracking hardware and invalidates the `ARView` returned by
/// `#createView()`.
///
/// All events are delivered on the EDT and every getter reflects the state as
/// of the most recently delivered events. High-frequency refinements (anchor
/// and plane updates, camera pose, light estimate) are coalesced so the EDT
/// sees the latest value rather than a backlog.
public final class ARSession implements AutoCloseable {
    private final ARImpl impl;
    private final ARSessionOptions options;
    private final Bridge bridge = new Bridge();
    private ARView view;
    private volatile boolean closed;

    private final LinkedHashMap<String, ARPlane> planes = new LinkedHashMap<String, ARPlane>();
    private final LinkedHashMap<String, ARAnchor> anchors = new LinkedHashMap<String, ARAnchor>();
    private ARTrackingState trackingState = ARTrackingState.NOT_TRACKING;
    private ARTrackingFailureReason failureReason = ARTrackingFailureReason.INITIALIZING;
    private ARPose cameraPose = ARPose.IDENTITY;
    private ARLightEstimate lightEstimate = ARLightEstimate.INVALID;

    private final ArrayList<ARTrackingListener> trackingListeners = new ArrayList<ARTrackingListener>();
    private final ArrayList<ARPlaneListener> planeListeners = new ArrayList<ARPlaneListener>();
    private final ArrayList<ARAnchorListener> anchorListeners = new ArrayList<ARAnchorListener>();

    ARSession(ARImpl impl, ARSessionOptions options) {
        this.impl = impl;
        this.options = options;
        impl.setEventSink(bridge);
    }

    /// Creates the AR view that renders the camera image composited with the
    /// anchored content. Each session owns one view; subsequent calls return
    /// the same instance.
    public ARView createView() {
        if (view == null) {
            view = new ARView(this, impl.createViewPeer());
        }
        return view;
    }

    /// The options the session was opened with. Read-only snapshot; mutating
    /// it after `AR#open(ARSessionOptions)` has no effect.
    public ARSessionOptions getOptions() {
        return options;
    }

    /// The session's overall tracking quality as of the latest update.
    public ARTrackingState getTrackingState() {
        return trackingState;
    }

    /// Why tracking is degraded, or `ARTrackingFailureReason#NONE` when it is
    /// not.
    public ARTrackingFailureReason getTrackingFailureReason() {
        return failureReason;
    }

    /// The latest device pose in world space. Poll-style: the value refreshes
    /// every frame without firing events.
    public ARPose getCameraPose() {
        return cameraPose;
    }

    /// The latest estimate of real-world lighting. Poll-style: the value
    /// refreshes every frame without firing events. Before the first platform
    /// estimate this returns `ARLightEstimate#INVALID`.
    public ARLightEstimate getLightEstimate() {
        return lightEstimate;
    }

    /// Performs an asynchronous hit test from a normalized view coordinate
    /// (`0.0` top left to `1.0` bottom right) into the world. The returned
    /// `AsyncResource` resolves on the EDT with the intersections ordered
    /// nearest first; an empty array means nothing was hit.
    ///
    /// #### Parameters
    ///
    /// - `xNorm`: the horizontal view coordinate, `0.0` to `1.0`
    ///
    /// - `yNorm`: the vertical view coordinate, `0.0` to `1.0`
    ///
    /// #### Returns
    ///
    /// resolves with the hits; call `ARHitResult#createAnchor()` on one to
    /// place content there
    public AsyncResource<ARHitResult[]> hitTest(float xNorm, float yNorm) {
        checkClosed();
        final AsyncResource<ARHitResult[]> out = new AsyncResource<ARHitResult[]>();
        AsyncResource<ARHitResult[]> in = new AsyncResource<ARHitResult[]>();
        in.ready(new SuccessCallback<ARHitResult[]>() {
            @Override public void onSucess(ARHitResult[] hits) {
                if (hits == null) {
                    hits = new ARHitResult[0];
                }
                for (int i = 0; i < hits.length; i++) {
                    hits[i].session = ARSession.this;
                }
                out.complete(hits);
            }
        });
        in.except(new SuccessCallback<Throwable>() {
            @Override public void onSucess(Throwable t) {
                out.error(t);
            }
        });
        impl.hitTest(xNorm, yNorm, in);
        return out;
    }

    /// Creates an anchor at an arbitrary world pose and registers it with the
    /// session. Prefer `ARHitResult#createAnchor()` when placing content on
    /// detected geometry.
    ///
    /// #### Parameters
    ///
    /// - `pose`: the world pose to anchor
    ///
    /// #### Returns
    ///
    /// the new anchor
    public ARAnchor createAnchor(ARPose pose) {
        checkClosed();
        if (pose == null) {
            throw new IllegalArgumentException("pose is required");
        }
        return registerAnchor(impl.createAnchor(pose), pose);
    }

    ARAnchor createAnchorFromHitInternal(ARHitResult hit) {
        checkClosed();
        return registerAnchor(impl.createAnchorFromHit(hit.getNativeHandle(), hit.getPose()),
                hit.getPose());
    }

    private ARAnchor registerAnchor(String id, ARPose pose) {
        ARAnchor anchor = new ARAnchor(id, pose);
        anchor.session = this;
        anchors.put(id, anchor);
        fireAnchorEvent(new ARAnchorEvent(ARAnchorEvent.Kind.ADDED, anchor, this));
        return anchor;
    }

    /// The anchors currently registered with the session, in registration
    /// order.
    public ARAnchor[] getAnchors() {
        return anchors.values().toArray(new ARAnchor[anchors.size()]);
    }

    /// The planes the session currently tracks, in detection order.
    public ARPlane[] getPlanes() {
        return planes.values().toArray(new ARPlane[planes.size()]);
    }

    /// Registers a tracking state listener. Events fire on the EDT.
    public void addTrackingListener(ARTrackingListener l) {
        if (l != null && !trackingListeners.contains(l)) {
            trackingListeners.add(l);
        }
    }

    /// Removes a tracking state listener.
    public void removeTrackingListener(ARTrackingListener l) {
        trackingListeners.remove(l);
    }

    /// Registers a plane listener. Events fire on the EDT.
    public void addPlaneListener(ARPlaneListener l) {
        if (l != null && !planeListeners.contains(l)) {
            planeListeners.add(l);
        }
    }

    /// Removes a plane listener.
    public void removePlaneListener(ARPlaneListener l) {
        planeListeners.remove(l);
    }

    /// Registers an anchor listener. Events fire on the EDT.
    public void addAnchorListener(ARAnchorListener l) {
        if (l != null && !anchorListeners.contains(l)) {
            anchorListeners.add(l);
        }
    }

    /// Removes an anchor listener.
    public void removeAnchorListener(ARAnchorListener l) {
        anchorListeners.remove(l);
    }

    /// Suspends tracking and the camera while keeping this session object
    /// alive. Pair with `#resume()`.
    public void pause() {
        if (!closed) {
            impl.pause();
        }
    }

    /// Re-acquires the camera and resumes tracking after `#pause()`.
    public void resume() {
        if (!closed) {
            impl.resume();
        }
    }

    /// Releases the session. Idempotent.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            impl.close();
        } finally {
            AR.clearActive(this);
        }
    }

    /// True once `#close()` has been called on this session.
    public boolean isClosed() {
        return closed;
    }

    void anchorDetachedInternal(ARAnchor anchor) {
        if (!closed) {
            impl.removeAnchor(anchor.getId());
        }
        if (anchors.remove(anchor.getId()) != null) {
            fireAnchorEvent(new ARAnchorEvent(ARAnchorEvent.Kind.REMOVED, anchor, this));
        }
    }

    void anchorNodeChangedInternal(ARAnchor anchor, ARNode node) {
        if (!closed) {
            impl.setAnchorNode(anchor.getId(), node);
        }
    }

    void nodeChangedInternal(String anchorId, ARNode root) {
        if (!closed && anchorId != null) {
            impl.nodeChanged(anchorId, root);
        }
    }

    ARImpl getImpl() {
        return impl;
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("the AR session is closed");
        }
    }

    private void fireTrackingEvent() {
        ARTrackingListener[] ls = trackingListeners.toArray(
                new ARTrackingListener[trackingListeners.size()]);
        for (int i = 0; i < ls.length; i++) {
            ls[i].trackingStateChanged(this, trackingState, failureReason);
        }
    }

    private void firePlaneEvent(ARPlaneEvent ev) {
        ARPlaneListener[] ls = planeListeners.toArray(
                new ARPlaneListener[planeListeners.size()]);
        for (int i = 0; i < ls.length; i++) {
            ls[i].planeChanged(ev);
        }
    }

    private void fireAnchorEvent(ARAnchorEvent ev) {
        ARAnchorListener[] ls = anchorListeners.toArray(
                new ARAnchorListener[anchorListeners.size()]);
        for (int i = 0; i < ls.length; i++) {
            ls[i].anchorChanged(ev);
        }
    }

    private static final int OP_TRACKING = 0;
    private static final int OP_PLANE_ADDED = 1;
    private static final int OP_PLANE_REMOVED = 2;
    private static final int OP_ANCHOR_ADDED = 3;
    private static final int OP_ANCHOR_REMOVED = 4;

    /// Marshals implementation events - which may arrive on any thread - to
    /// the EDT. Discrete events (add/remove/tracking) keep their order;
    /// per-frame refinements coalesce to the latest value per id so a slow
    /// EDT never accumulates a backlog.
    private final class Bridge implements ARImpl.EventSink {
        private final Object lock = new Object();
        private final ArrayList<Object[]> ops = new ArrayList<Object[]>();
        private final LinkedHashMap<String, ARPlane> planeUpdates =
                new LinkedHashMap<String, ARPlane>();
        private final LinkedHashMap<String, Object[]> anchorUpdates =
                new LinkedHashMap<String, Object[]>();
        private ARPose pendingCameraPose;
        private ARLightEstimate pendingLight;
        private boolean drainScheduled;

        @Override
        public void onTrackingStateChanged(ARTrackingState state, ARTrackingFailureReason reason) {
            enqueueOp(new Object[]{Integer.valueOf(OP_TRACKING), state, reason});
        }

        @Override
        public void onPlaneAdded(ARPlane plane) {
            if (plane != null) {
                enqueueOp(new Object[]{Integer.valueOf(OP_PLANE_ADDED), plane});
            }
        }

        @Override
        public void onPlaneUpdated(ARPlane plane) {
            if (plane == null) {
                return;
            }
            synchronized (lock) {
                if (closed) {
                    return;
                }
                planeUpdates.put(plane.getId(), plane);
                scheduleDrain();
            }
        }

        @Override
        public void onPlaneRemoved(String planeId) {
            if (planeId != null) {
                enqueueOp(new Object[]{Integer.valueOf(OP_PLANE_REMOVED), planeId});
            }
        }

        @Override
        public void onAnchorAdded(ARAnchor anchor) {
            if (anchor != null) {
                enqueueOp(new Object[]{Integer.valueOf(OP_ANCHOR_ADDED), anchor});
            }
        }

        @Override
        public void onAnchorUpdated(String anchorId, ARPose pose, ARTrackingState state) {
            onFaceAnchorUpdated(anchorId, pose, state, null, null, null);
        }

        @Override
        public void onFaceAnchorUpdated(String anchorId, ARPose pose, ARTrackingState state,
                                        ARPose[] regionPoses, float[] meshVertices,
                                        int[] meshTriangles) {
            if (anchorId == null) {
                return;
            }
            synchronized (lock) {
                if (closed) {
                    return;
                }
                Object[] prev = anchorUpdates.get(anchorId);
                if (prev != null) {
                    // Merge with the pending update so face payloads survive a
                    // later pose-only refinement in the same batch.
                    if (pose == null) {
                        pose = (ARPose) prev[0];
                    }
                    if (state == null) {
                        state = (ARTrackingState) prev[1];
                    }
                    if (regionPoses == null) {
                        regionPoses = (ARPose[]) prev[2];
                    }
                    if (meshVertices == null) {
                        meshVertices = (float[]) prev[3];
                    }
                    if (meshTriangles == null) {
                        meshTriangles = (int[]) prev[4];
                    }
                }
                anchorUpdates.put(anchorId,
                        new Object[]{pose, state, regionPoses, meshVertices, meshTriangles});
                scheduleDrain();
            }
        }

        @Override
        public void onAnchorRemoved(String anchorId) {
            if (anchorId != null) {
                enqueueOp(new Object[]{Integer.valueOf(OP_ANCHOR_REMOVED), anchorId});
            }
        }

        @Override
        public void onLightEstimate(ARLightEstimate estimate) {
            if (estimate == null) {
                return;
            }
            synchronized (lock) {
                if (closed) {
                    return;
                }
                pendingLight = estimate;
                scheduleDrain();
            }
        }

        @Override
        public void onCameraPose(ARPose pose) {
            if (pose == null) {
                return;
            }
            synchronized (lock) {
                if (closed) {
                    return;
                }
                pendingCameraPose = pose;
                scheduleDrain();
            }
        }

        private void enqueueOp(Object[] op) {
            synchronized (lock) {
                if (closed) {
                    return;
                }
                ops.add(op);
                scheduleDrain();
            }
        }

        private void scheduleDrain() {
            if (drainScheduled) {
                return;
            }
            drainScheduled = true;
            Display.getInstance().callSerially(new Runnable() {
                @Override public void run() {
                    drain();
                }
            });
        }

        private void drain() {
            Object[][] opsSnapshot;
            ARPlane[] planeSnapshot;
            Map.Entry[] anchorSnapshot;
            ARPose newCameraPose;
            ARLightEstimate newLight;
            synchronized (lock) {
                drainScheduled = false;
                if (closed) {
                    ops.clear();
                    planeUpdates.clear();
                    anchorUpdates.clear();
                    pendingCameraPose = null;
                    pendingLight = null;
                    return;
                }
                opsSnapshot = ops.toArray(new Object[ops.size()][]);
                ops.clear();
                planeSnapshot = planeUpdates.values().toArray(new ARPlane[planeUpdates.size()]);
                planeUpdates.clear();
                anchorSnapshot = anchorUpdates.entrySet().toArray(
                        new Map.Entry[anchorUpdates.size()]);
                anchorUpdates.clear();
                newCameraPose = pendingCameraPose;
                pendingCameraPose = null;
                newLight = pendingLight;
                pendingLight = null;
            }
            if (newCameraPose != null) {
                cameraPose = newCameraPose;
            }
            if (newLight != null) {
                lightEstimate = newLight;
            }
            for (int i = 0; i < opsSnapshot.length; i++) {
                applyOp(opsSnapshot[i]);
            }
            for (int i = 0; i < planeSnapshot.length; i++) {
                ARPlane p = planeSnapshot[i];
                if (planes.containsKey(p.getId())) {
                    planes.put(p.getId(), p);
                    firePlaneEvent(new ARPlaneEvent(ARPlaneEvent.Kind.UPDATED, p, ARSession.this));
                }
            }
            for (int i = 0; i < anchorSnapshot.length; i++) {
                String id = (String) anchorSnapshot[i].getKey();
                Object[] u = (Object[]) anchorSnapshot[i].getValue();
                ARAnchor anchor = anchors.get(id);
                if (anchor == null) {
                    continue;
                }
                anchor.update((ARPose) u[0], (ARTrackingState) u[1]);
                if (anchor instanceof ARFaceAnchor) {
                    ((ARFaceAnchor) anchor).updateFace(
                            (ARPose[]) u[2], (float[]) u[3], (int[]) u[4]);
                }
                fireAnchorEvent(new ARAnchorEvent(ARAnchorEvent.Kind.UPDATED, anchor,
                        ARSession.this));
            }
        }

        private void applyOp(Object[] op) {
            switch (((Integer) op[0]).intValue()) {
                case OP_TRACKING: {
                    ARTrackingState state = (ARTrackingState) op[1];
                    ARTrackingFailureReason reason = (ARTrackingFailureReason) op[2];
                    trackingState = state == null ? ARTrackingState.NOT_TRACKING : state;
                    failureReason = reason == null ? ARTrackingFailureReason.NONE : reason;
                    fireTrackingEvent();
                    break;
                }
                case OP_PLANE_ADDED: {
                    ARPlane plane = (ARPlane) op[1];
                    boolean known = planes.containsKey(plane.getId());
                    planes.put(plane.getId(), plane);
                    firePlaneEvent(new ARPlaneEvent(
                            known ? ARPlaneEvent.Kind.UPDATED : ARPlaneEvent.Kind.ADDED,
                            plane, ARSession.this));
                    break;
                }
                case OP_PLANE_REMOVED: {
                    ARPlane plane = planes.remove((String) op[1]);
                    if (plane != null) {
                        firePlaneEvent(new ARPlaneEvent(ARPlaneEvent.Kind.REMOVED, plane,
                                ARSession.this));
                    }
                    break;
                }
                case OP_ANCHOR_ADDED: {
                    ARAnchor anchor = (ARAnchor) op[1];
                    if (!anchors.containsKey(anchor.getId())) {
                        anchor.session = ARSession.this;
                        anchors.put(anchor.getId(), anchor);
                        fireAnchorEvent(new ARAnchorEvent(ARAnchorEvent.Kind.ADDED, anchor,
                                ARSession.this));
                    }
                    break;
                }
                case OP_ANCHOR_REMOVED: {
                    ARAnchor anchor = anchors.remove((String) op[1]);
                    if (anchor != null) {
                        anchor.markDetached();
                        fireAnchorEvent(new ARAnchorEvent(ARAnchorEvent.Kind.REMOVED, anchor,
                                ARSession.this));
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}
