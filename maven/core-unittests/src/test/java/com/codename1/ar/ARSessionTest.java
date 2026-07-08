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

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link ARSession}: view creation, lifecycle forwarding, hit
 * testing, anchor management, node forwarding, and the event bridge that
 * marshals implementation events to the EDT with per-id coalescing.
 */
class ARSessionTest extends UITestBase {

    private RecordingARImpl impl;
    private ARSession session;

    private final List<ARPlaneEvent> planeEvents = new ArrayList<ARPlaneEvent>();
    private final List<ARAnchorEvent> anchorEvents = new ArrayList<ARAnchorEvent>();
    private final List<ARTrackingState> trackingStates = new ArrayList<ARTrackingState>();

    @AfterEach
    void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
        planeEvents.clear();
        anchorEvents.clear();
        trackingStates.clear();
    }

    private ARSession open() {
        impl = new RecordingARImpl();
        implementation.setARImpl(impl);
        session = AR.open(new ARSessionOptions());
        return session;
    }

    private void listenAll() {
        session.addPlaneListener(new ARPlaneListener() {
            public void planeChanged(ARPlaneEvent ev) {
                planeEvents.add(ev);
            }
        });
        session.addAnchorListener(new ARAnchorListener() {
            public void anchorChanged(ARAnchorEvent ev) {
                anchorEvents.add(ev);
            }
        });
        session.addTrackingListener(new ARTrackingListener() {
            public void trackingStateChanged(ARSession s, ARTrackingState st,
                                             ARTrackingFailureReason r) {
                trackingStates.add(st);
            }
        });
    }

    private static ARPlane plane(String id) {
        return new ARPlane(id, ARPlane.Type.HORIZONTAL_UP, ARPose.IDENTITY, 1f, 1f, null,
                ARTrackingState.TRACKING);
    }

    // ---- view ----

    @Test
    void createViewIsCachedAndBacklinked() {
        open();
        ARView v1 = session.createView();
        ARView v2 = session.createView();
        assertSame(v1, v2);
        assertSame(session, v1.getSession());
        assertEquals(1, impl.viewPeerCount);
    }

    // ---- lifecycle ----

    @Test
    void pauseResumeForwardOnce() {
        open();
        session.pause();
        session.resume();
        assertEquals(1, impl.pauseCount);
        assertEquals(1, impl.resumeCount);
    }

    @Test
    void closeIsIdempotentAndForwardsOnce() {
        open();
        session.close();
        session.close();
        assertTrue(session.isClosed());
        assertEquals(1, impl.closeCount);
    }

    @Test
    void pauseResumeAfterCloseAreDropped() {
        open();
        session.close();
        session.pause();
        session.resume();
        assertEquals(0, impl.pauseCount);
        assertEquals(0, impl.resumeCount);
    }

    // ---- hit testing ----

    @Test
    void hitTestResolvesWithSessionAttachedResults() {
        open();
        final ARHitResult hit = new ARHitResult(ARPose.IDENTITY, 1.5f,
                ARHitResult.Type.PLANE, plane("p1"), "native-token");
        impl.hitResults = new ARHitResult[]{hit};
        final AtomicReference<ARHitResult[]> got = new AtomicReference<ARHitResult[]>();
        session.hitTest(0.25f, 0.75f).ready(new com.codename1.util.SuccessCallback<ARHitResult[]>() {
            public void onSucess(ARHitResult[] hits) {
                got.set(hits);
            }
        });
        flushSerialCalls();
        assertNotNull(got.get());
        assertEquals(1, got.get().length);
        assertEquals(0.25f, impl.lastHitX, 0f);
        assertEquals(0.75f, impl.lastHitY, 0f);

        // The session attached itself, so createAnchor() works and reaches
        // the impl with the native token.
        ARAnchor anchor = got.get()[0].createAnchor();
        assertNotNull(anchor);
        assertEquals("native-token", impl.lastHitHandle);
        assertEquals(1, session.getAnchors().length);
    }

    @Test
    void hitTestMapsNullResultsToEmptyArray() {
        open();
        impl.hitResults = null;
        final AtomicReference<ARHitResult[]> got = new AtomicReference<ARHitResult[]>();
        session.hitTest(0.5f, 0.5f).ready(new com.codename1.util.SuccessCallback<ARHitResult[]>() {
            public void onSucess(ARHitResult[] hits) {
                got.set(hits);
            }
        });
        flushSerialCalls();
        assertNotNull(got.get());
        assertEquals(0, got.get().length);
    }

    @Test
    void hitTestAfterCloseThrows() {
        open();
        session.close();
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                session.hitTest(0.5f, 0.5f);
            }
        });
    }

    @Test
    void unattachedHitResultCannotCreateAnchor() {
        final ARHitResult hit = new ARHitResult(ARPose.IDENTITY, 1f,
                ARHitResult.Type.FEATURE_POINT, null, null);
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                hit.createAnchor();
            }
        });
    }

    // ---- anchors ----

    @Test
    void createAnchorRegistersAndFiresAdded() {
        open();
        listenAll();
        ARAnchor a = session.createAnchor(new ARPose(1, 2, 3, 0, 0, 0, 1));
        assertEquals(1, session.getAnchors().length);
        assertSame(a, session.getAnchors()[0]);
        assertEquals(1, anchorEvents.size());
        assertEquals(ARAnchorEvent.Kind.ADDED, anchorEvents.get(0).getKind());
        assertSame(a, anchorEvents.get(0).getAnchor());
        assertEquals(1f, a.getPose().getTx(), 0f);
    }

    @Test
    void createAnchorRejectsNullPose() {
        open();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                session.createAnchor(null);
            }
        });
    }

    @Test
    void detachRemovesAnchorAndFiresRemoved() {
        open();
        listenAll();
        ARAnchor a = session.createAnchor(ARPose.IDENTITY);
        anchorEvents.clear();
        a.detach();
        assertTrue(a.isDetached());
        assertEquals(0, session.getAnchors().length);
        assertEquals(1, impl.removedAnchorIds.size());
        assertEquals(a.getId(), impl.removedAnchorIds.get(0));
        assertEquals(1, anchorEvents.size());
        assertEquals(ARAnchorEvent.Kind.REMOVED, anchorEvents.get(0).getKind());
        // Idempotent.
        a.detach();
        assertEquals(1, impl.removedAnchorIds.size());
    }

    @Test
    void setNodeForwardsToImplAndDetachedAnchorRejectsIt() {
        open();
        ARAnchor a = session.createAnchor(ARPose.IDENTITY);
        final ARNode node = new ARNode(ARModel.fromMesh(
                com.codename1.gpu.Primitives.sphere(0.1f, 4, 6, false), 0xff00ff00));
        a.setNode(node);
        assertEquals(1, impl.setNodeCount);
        assertEquals(a.getId(), impl.lastNodeAnchorId);
        assertSame(node, impl.lastNode);
        assertSame(node, a.getNode());

        a.setNode(null);
        assertEquals(2, impl.setNodeCount);
        assertNull(impl.lastNode);

        a.detach();
        assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                a.setNode(node);
            }
        });
    }

    @Test
    void nodeMutationsForwardTheRootSubtree() {
        open();
        ARAnchor a = session.createAnchor(ARPose.IDENTITY);
        ARNode root = new ARNode();
        a.setNode(root);
        impl.nodeChangedCount = 0;

        root.setLocalPosition(0, 0.5f, 0);
        assertEquals(1, impl.nodeChangedCount);
        assertEquals(a.getId(), impl.lastChangedAnchorId);
        assertSame(root, impl.lastChangedNode);

        // Child mutations bubble up to the root.
        ARNode child = new ARNode();
        root.addChild(child);
        assertEquals(2, impl.nodeChangedCount);
        child.setLocalScale(2f);
        assertEquals(3, impl.nodeChangedCount);
        assertSame(root, impl.lastChangedNode);

        // Detached subtrees stop notifying.
        a.setNode(null);
        impl.nodeChangedCount = 0;
        root.setVisible(false);
        assertEquals(0, impl.nodeChangedCount);
    }

    @Test
    void anchorContentRootMayNotHaveAParent() {
        open();
        final ARAnchor a = session.createAnchor(ARPose.IDENTITY);
        ARNode parent = new ARNode();
        final ARNode child = new ARNode();
        parent.addChild(child);
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                a.setNode(child);
            }
        });
    }

    // ---- event bridge ----

    @Test
    void planeEventsFromBackgroundThreadArriveAfterFlush() {
        open();
        listenAll();
        impl.onBackgroundThread(new Runnable() {
            public void run() {
                impl.sink.onPlaneAdded(plane("p1"));
            }
        });
        // Nothing delivered until the EDT drains.
        assertEquals(0, planeEvents.size());
        flushSerialCalls();
        assertEquals(1, planeEvents.size());
        assertEquals(ARPlaneEvent.Kind.ADDED, planeEvents.get(0).getKind());
        assertEquals("p1", planeEvents.get(0).getPlane().getId());
        assertEquals(1, session.getPlanes().length);
    }

    @Test
    void planeUpdatesCoalesceToTheLatestSnapshot() {
        open();
        listenAll();
        impl.sink.onPlaneAdded(plane("p1"));
        flushSerialCalls();
        planeEvents.clear();

        final ARPlane last = new ARPlane("p1", ARPlane.Type.HORIZONTAL_UP, ARPose.IDENTITY,
                5f, 5f, null, ARTrackingState.TRACKING);
        impl.onBackgroundThread(new Runnable() {
            public void run() {
                impl.sink.onPlaneUpdated(plane("p1"));
                impl.sink.onPlaneUpdated(last);
            }
        });
        flushSerialCalls();
        assertEquals(1, planeEvents.size(), "two updates for one id coalesce into one event");
        assertEquals(ARPlaneEvent.Kind.UPDATED, planeEvents.get(0).getKind());
        assertEquals(5f, session.getPlanes()[0].getExtentX(), 0f);
    }

    @Test
    void planeRemovalDropsThePlaneAndSkipsStaleUpdates() {
        open();
        listenAll();
        impl.sink.onPlaneAdded(plane("p1"));
        flushSerialCalls();
        planeEvents.clear();

        impl.sink.onPlaneRemoved("p1");
        impl.sink.onPlaneUpdated(plane("p1"));
        flushSerialCalls();
        assertEquals(1, planeEvents.size(), "the stale update after removal is dropped");
        assertEquals(ARPlaneEvent.Kind.REMOVED, planeEvents.get(0).getKind());
        assertEquals(0, session.getPlanes().length);
    }

    @Test
    void updateBeforeAddInTheSameBatchAppliesAfterTheAdd() {
        open();
        listenAll();
        final ARPlane refined = new ARPlane("p1", ARPlane.Type.HORIZONTAL_UP, ARPose.IDENTITY,
                3f, 3f, null, ARTrackingState.TRACKING);
        impl.sink.onPlaneUpdated(refined);
        impl.sink.onPlaneAdded(plane("p1"));
        flushSerialCalls();
        // Ordered add applies first, then the coalesced refinement.
        assertEquals(2, planeEvents.size());
        assertEquals(ARPlaneEvent.Kind.ADDED, planeEvents.get(0).getKind());
        assertEquals(ARPlaneEvent.Kind.UPDATED, planeEvents.get(1).getKind());
        assertEquals(3f, session.getPlanes()[0].getExtentX(), 0f);
    }

    @Test
    void platformAnchorsArriveAsSubtypes() {
        open();
        listenAll();
        impl.sink.onAnchorAdded(new ARImageAnchor("img-1", ARPose.IDENTITY, "poster", 0.4f));
        impl.sink.onAnchorAdded(new ARFaceAnchor("face-1", ARPose.IDENTITY));
        flushSerialCalls();
        assertEquals(2, anchorEvents.size());
        assertTrue(anchorEvents.get(0).getAnchor() instanceof ARImageAnchor);
        assertEquals("poster",
                ((ARImageAnchor) anchorEvents.get(0).getAnchor()).getReferenceImageName());
        assertTrue(anchorEvents.get(1).getAnchor() instanceof ARFaceAnchor);
        assertEquals(2, session.getAnchors().length);
    }

    @Test
    void anchorUpdatesRefreshPoseAndState() {
        open();
        listenAll();
        ARAnchor a = session.createAnchor(ARPose.IDENTITY);
        anchorEvents.clear();
        impl.sink.onAnchorUpdated(a.getId(), new ARPose(0, 1, 0, 0, 0, 0, 1),
                ARTrackingState.LIMITED);
        flushSerialCalls();
        assertEquals(1, anchorEvents.size());
        assertEquals(ARAnchorEvent.Kind.UPDATED, anchorEvents.get(0).getKind());
        assertEquals(1f, a.getPose().getTy(), 0f);
        assertEquals(ARTrackingState.LIMITED, a.getTrackingState());
    }

    @Test
    void faceUpdatesCarryGeometryAndMergeWithPoseUpdates() {
        open();
        listenAll();
        impl.sink.onAnchorAdded(new ARFaceAnchor("face-1", ARPose.IDENTITY));
        flushSerialCalls();
        ARFaceAnchor face = (ARFaceAnchor) session.getAnchors()[0];

        final ARPose[] regions = new ARPose[ARFaceRegion.values().length];
        regions[ARFaceRegion.NOSE_TIP.ordinal()] = new ARPose(0, 0, 0.1f, 0, 0, 0, 1);
        final float[] mesh = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        final int[] tris = {0, 1, 2};
        // A face payload followed by a pose-only refinement in the same batch
        // must not lose the geometry.
        impl.sink.onFaceAnchorUpdated("face-1", null, null, regions, mesh, tris);
        impl.sink.onAnchorUpdated("face-1", new ARPose(0, 2, 0, 0, 0, 0, 1),
                ARTrackingState.TRACKING);
        flushSerialCalls();

        assertEquals(2f, face.getPose().getTy(), 0f);
        assertNotNull(face.getRegionPose(ARFaceRegion.NOSE_TIP));
        assertNull(face.getRegionPose(ARFaceRegion.LEFT_EYE));
        assertArrayEquals(mesh, face.getMeshVertices());
        assertArrayEquals(tris, face.getMeshTriangles());
    }

    @Test
    void platformAnchorRemovalDetachesAndFires() {
        open();
        listenAll();
        impl.sink.onAnchorAdded(new ARImageAnchor("img-1", ARPose.IDENTITY, "poster", 0.4f));
        flushSerialCalls();
        ARAnchor a = session.getAnchors()[0];
        anchorEvents.clear();
        impl.sink.onAnchorRemoved("img-1");
        flushSerialCalls();
        assertTrue(a.isDetached());
        assertEquals(0, session.getAnchors().length);
        assertEquals(ARAnchorEvent.Kind.REMOVED, anchorEvents.get(0).getKind());
    }

    @Test
    void trackingStateChangesFireAndCache() {
        open();
        listenAll();
        assertEquals(ARTrackingState.NOT_TRACKING, session.getTrackingState());
        impl.sink.onTrackingStateChanged(ARTrackingState.TRACKING, ARTrackingFailureReason.NONE);
        flushSerialCalls();
        assertEquals(1, trackingStates.size());
        assertEquals(ARTrackingState.TRACKING, session.getTrackingState());
        assertEquals(ARTrackingFailureReason.NONE, session.getTrackingFailureReason());

        impl.sink.onTrackingStateChanged(ARTrackingState.LIMITED,
                ARTrackingFailureReason.EXCESSIVE_MOTION);
        flushSerialCalls();
        assertEquals(ARTrackingFailureReason.EXCESSIVE_MOTION,
                session.getTrackingFailureReason());
    }

    @Test
    void cameraPoseAndLightEstimateAreCachedForPolling() {
        open();
        assertEquals(ARPose.IDENTITY, session.getCameraPose());
        assertFalse(session.getLightEstimate().isValid());

        impl.sink.onCameraPose(new ARPose(1, 1, 1, 0, 0, 0, 1));
        impl.sink.onCameraPose(new ARPose(2, 2, 2, 0, 0, 0, 1));
        impl.sink.onLightEstimate(new ARLightEstimate(true, 0.5f, 1f, 0.9f, 0.8f));
        flushSerialCalls();
        // Coalesced: only the latest pose survives.
        assertEquals(2f, session.getCameraPose().getTx(), 0f);
        assertTrue(session.getLightEstimate().isValid());
        assertEquals(0.5f, session.getLightEstimate().getAmbientIntensity(), 0f);
        assertEquals(0.9f, session.getLightEstimate().getColorCorrection()[1], 0f);
    }

    @Test
    void eventsAfterCloseAreDropped() {
        open();
        listenAll();
        ARImpl_EventSinkHolder holder = new ARImpl_EventSinkHolder(impl.sink);
        session.close();
        holder.sink.onPlaneAdded(plane("p1"));
        holder.sink.onTrackingStateChanged(ARTrackingState.TRACKING,
                ARTrackingFailureReason.NONE);
        flushSerialCalls();
        assertEquals(0, planeEvents.size());
        assertEquals(0, trackingStates.size());
    }

    /** Tiny holder so the closed-session test reads clearly. */
    private static final class ARImpl_EventSinkHolder {
        final com.codename1.impl.ARImpl.EventSink sink;

        ARImpl_EventSinkHolder(com.codename1.impl.ARImpl.EventSink sink) {
            this.sink = sink;
        }
    }

    @Test
    void listenersCanBeRemoved() {
        open();
        final int[] count = {0};
        ARPlaneListener l = new ARPlaneListener() {
            public void planeChanged(ARPlaneEvent ev) {
                count[0]++;
            }
        };
        session.addPlaneListener(l);
        impl.sink.onPlaneAdded(plane("p1"));
        flushSerialCalls();
        assertEquals(1, count[0]);
        session.removePlaneListener(l);
        impl.sink.onPlaneAdded(plane("p2"));
        flushSerialCalls();
        assertEquals(1, count[0]);
    }
}
