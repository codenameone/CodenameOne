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
package com.codename1.impl.ios;

import com.codename1.ar.ARCapabilities;
import com.codename1.ar.ARFaceRegion;
import com.codename1.ar.ARHitResult;
import com.codename1.ar.ARImageAnchor;
import com.codename1.ar.ARFaceAnchor;
import com.codename1.ar.ARLightEstimate;
import com.codename1.ar.ARModel;
import com.codename1.ar.ARNode;
import com.codename1.ar.ARPlane;
import com.codename1.ar.ARPose;
import com.codename1.ar.ARReferenceImage;
import com.codename1.ar.ARSessionOptions;
import com.codename1.ar.ARTrackingFailureReason;
import com.codename1.ar.ARTrackingMode;
import com.codename1.ar.ARTrackingState;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Quaternion;
import com.codename1.impl.ARImpl;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// iOS implementation of `ARImpl`. Bridges to `CN1AR.{h,m}` which wraps an
/// ARKit `ARSession` composited through an `ARSCNView`; anchored content is
/// flattened from the portable `ARNode` tree into raw mesh buffers that the
/// native side turns into SceneKit geometry.
///
/// @hidden
public class IOSARImpl extends ARImpl {

    // One Java instance is the active AR session at any moment (the core
    // enforces a single open session); native callbacks arrive from the
    // ARKit session/renderer queues without context, so we route them
    // through this static.
    private static volatile IOSARImpl ACTIVE;

    private long sessionPeer;
    private EventSink sink;
    private final Map<String, ARPlane> planes = new HashMap<String, ARPlane>();
    private final Map<ARModel, byte[]> textureCache = new HashMap<ARModel, byte[]>();

    @Override
    public ARCapabilities getCapabilities() {
        boolean world = IOSImplementation.nativeInstance.cn1ArIsSupported(0);
        boolean face = IOSImplementation.nativeInstance.cn1ArIsSupported(1);
        return new ARCapabilities(world, world, world, face, world || face);
    }

    @Override
    public void setEventSink(EventSink sink) {
        this.sink = sink;
    }

    @Override
    public void open(ARSessionOptions opts) throws IOException {
        long peer = IOSImplementation.nativeInstance.cn1ArCreate();
        if (peer == 0) {
            throw new IOException("AR is not supported on this device");
        }
        ARReferenceImage[] images = opts.getReferenceImages();
        for (int i = 0; i < images.length; i++) {
            IOSImplementation.nativeInstance.cn1ArAddReferenceImage(peer,
                    images[i].getEncodedImage(), images[i].getName(),
                    images[i].getPhysicalWidthMeters());
        }
        int configType = opts.getTrackingMode() == ARTrackingMode.FACE ? 1 : 0;
        int planeMask = 0;
        if (opts.getPlaneDetection().includesHorizontal()) {
            planeMask |= 1;
        }
        if (opts.getPlaneDetection().includesVertical()) {
            planeMask |= 2;
        }
        if (!IOSImplementation.nativeInstance.cn1ArStart(peer, configType, planeMask,
                opts.isLightEstimation())) {
            IOSImplementation.nativeInstance.cn1ArClose(peer);
            throw new IOException("The requested AR configuration is not supported on this device");
        }
        this.sessionPeer = peer;
        ACTIVE = this;
    }

    @Override
    public PeerComponent createViewPeer() {
        if (sessionPeer == 0) {
            return null;
        }
        long viewPeer = IOSImplementation.nativeInstance.cn1ArCreateView(sessionPeer);
        if (viewPeer == 0) {
            return null;
        }
        return IOSImplementation.instance.createNativePeer(new long[]{viewPeer});
    }

    @Override
    public void hitTest(float xNorm, float yNorm, final AsyncResource<ARHitResult[]> result) {
        String packed = sessionPeer == 0 ? null
                : IOSImplementation.nativeInstance.cn1ArHitTest(sessionPeer, xNorm, yNorm);
        final ARHitResult[] hits = parseHits(packed);
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() {
                result.complete(hits);
            }
        });
    }

    // packed: "hitId|type|tx|ty|tz|qx|qy|qz|qw|distance|planeId;..."
    private ARHitResult[] parseHits(String packed) {
        if (packed == null || packed.length() == 0) {
            return new ARHitResult[0];
        }
        List<String> entries = splitChar(packed, ';');
        List<ARHitResult> out = new ArrayList<ARHitResult>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            List<String> f = splitChar(entries.get(i), '|');
            if (f.size() < 11) {
                continue;
            }
            try {
                int hitId = Integer.parseInt(f.get(0));
                int type = Integer.parseInt(f.get(1));
                ARPose pose = new ARPose(
                        Float.parseFloat(f.get(2)), Float.parseFloat(f.get(3)),
                        Float.parseFloat(f.get(4)), Float.parseFloat(f.get(5)),
                        Float.parseFloat(f.get(6)), Float.parseFloat(f.get(7)),
                        Float.parseFloat(f.get(8)));
                float distance = Float.parseFloat(f.get(9));
                ARPlane plane = f.get(10).length() == 0 ? null : planes.get(f.get(10));
                ARHitResult.Type t = type == 0 ? ARHitResult.Type.PLANE
                        : type == 1 ? ARHitResult.Type.ESTIMATED_PLANE
                        : ARHitResult.Type.FEATURE_POINT;
                out.add(new ARHitResult(pose, distance, t, plane, Integer.valueOf(hitId)));
            } catch (NumberFormatException err) {
                // Skip a malformed entry rather than failing the whole test.
            }
        }
        return out.toArray(new ARHitResult[out.size()]);
    }

    private static List<String> splitChar(String s, char sep) {
        List<String> out = new ArrayList<String>();
        int start = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == sep) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    @Override
    public String createAnchor(ARPose pose) {
        return IOSImplementation.nativeInstance.cn1ArCreateAnchor(sessionPeer,
                pose.getTx(), pose.getTy(), pose.getTz(),
                pose.getQx(), pose.getQy(), pose.getQz(), pose.getQw());
    }

    @Override
    public String createAnchorFromHit(Object nativeHandle, ARPose pose) {
        if (nativeHandle instanceof Integer) {
            String id = IOSImplementation.nativeInstance.cn1ArCreateAnchorFromHit(
                    sessionPeer, ((Integer) nativeHandle).intValue());
            if (id != null) {
                return id;
            }
        }
        return createAnchor(pose);
    }

    @Override
    public void removeAnchor(String anchorId) {
        if (sessionPeer != 0) {
            IOSImplementation.nativeInstance.cn1ArRemoveAnchor(sessionPeer, anchorId);
        }
    }

    @Override
    public void setAnchorNode(String anchorId, ARNode node) {
        if (sessionPeer == 0) {
            return;
        }
        IOSImplementation.nativeInstance.cn1ArClearAnchorContent(sessionPeer, anchorId);
        if (node != null) {
            flatten(anchorId, node, Matrix4.identity());
        }
    }

    @Override
    public void nodeChanged(String anchorId, ARNode node) {
        // Rebuild the whole subtree; placement-style content is small so the
        // rebuild is cheap and keeps the bridge surface minimal.
        setAnchorNode(anchorId, node);
    }

    /// Walks the node tree accumulating local transforms and hands every
    /// model mesh to the native renderer in anchor-local space.
    private void flatten(String anchorId, ARNode node, float[] parentMatrix) {
        if (!node.isVisible()) {
            return;
        }
        float[] local = Matrix4.translation(node.getLocalX(), node.getLocalY(), node.getLocalZ());
        float[] rot = new float[16];
        Quaternion.toMatrix(new float[]{node.getLocalQx(), node.getLocalQy(),
                node.getLocalQz(), node.getLocalQw()}, rot);
        float[] tmp = new float[16];
        Matrix4.multiply(local, rot, tmp);
        float s = node.getLocalScale();
        float[] m = new float[16];
        Matrix4.multiply(tmp, Matrix4.scaling(s, s, s), m);
        float[] world = new float[16];
        Matrix4.multiply(parentMatrix, m, world);

        ARModel model = node.getModel();
        if (model != null) {
            Mesh mesh = model.getMesh();
            if (mesh != null && mesh.getVertices().getFormat().getFloatsPerVertex() == 8) {
                int vertexCount = mesh.getVertices().getVertexCount();
                float[] interleaved = mesh.getVertices().getData();
                int[] indices;
                if (mesh.getIndices() != null) {
                    short[] shortIdx = mesh.getIndices().getData();
                    indices = new int[mesh.getIndices().getIndexCount()];
                    for (int i = 0; i < indices.length; i++) {
                        indices[i] = shortIdx[i] & 0xffff;
                    }
                } else {
                    indices = new int[vertexCount];
                    for (int i = 0; i < vertexCount; i++) {
                        indices[i] = i;
                    }
                }
                IOSImplementation.nativeInstance.cn1ArAddAnchorMesh(sessionPeer, anchorId,
                        interleaved, vertexCount, indices, indices.length,
                        model.getColor(), textureBytes(model), world);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            flatten(anchorId, node.getChildAt(i), world);
        }
    }

    private byte[] textureBytes(ARModel model) {
        if (textureCache.containsKey(model)) {
            return textureCache.get(model);
        }
        byte[] bytes = null;
        Image img = model.getBaseColorImage();
        if (img != null) {
            if (img instanceof EncodedImage) {
                bytes = ((EncodedImage) img).getImageData();
            } else {
                bytes = EncodedImage.createFromImage(img, false).getImageData();
            }
        }
        textureCache.put(model, bytes);
        return bytes;
    }

    @Override
    public void requestPermissions(final AsyncResource<Boolean> result) {
        // iOS prompts for the camera permission when the AR session first
        // runs; there is no separate pre-flight, so report available.
        Display.getInstance().callSerially(new Runnable() {
            @Override public void run() {
                result.complete(Boolean.TRUE);
            }
        });
    }

    @Override
    public void pause() {
        if (sessionPeer != 0) {
            IOSImplementation.nativeInstance.cn1ArPause(sessionPeer);
        }
    }

    @Override
    public void resume() {
        if (sessionPeer != 0) {
            IOSImplementation.nativeInstance.cn1ArResume(sessionPeer);
        }
    }

    @Override
    public void close() {
        long s = sessionPeer;
        sessionPeer = 0;
        if (ACTIVE == this) {
            ACTIVE = null;
        }
        planes.clear();
        textureCache.clear();
        if (s != 0) {
            IOSImplementation.nativeInstance.cn1ArClose(s);
        }
    }

    // ---------------------------------------------------------------------
    // Native -> Java callbacks. Called from CN1AR.m on the ARKit session /
    // SceneKit renderer queues; the core session bridge marshals to the EDT.
    // ---------------------------------------------------------------------

    private static ARTrackingState stateFor(int code) {
        switch (code) {
            case 0:
                return ARTrackingState.NOT_TRACKING;
            case 1:
                return ARTrackingState.LIMITED;
            default:
                return ARTrackingState.TRACKING;
        }
    }

    private static ARTrackingFailureReason reasonFor(int code) {
        switch (code) {
            case 1:
                return ARTrackingFailureReason.INITIALIZING;
            case 2:
                return ARTrackingFailureReason.EXCESSIVE_MOTION;
            case 3:
                return ARTrackingFailureReason.INSUFFICIENT_LIGHT;
            case 4:
                return ARTrackingFailureReason.INSUFFICIENT_FEATURES;
            default:
                return ARTrackingFailureReason.NONE;
        }
    }

    /// Called from native code when the camera tracking quality changes.
    public static void onTrackingStateChanged(int state, int reason) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null) {
            return;
        }
        self.sink.onTrackingStateChanged(stateFor(state), reasonFor(reason));
    }

    /// Called from native code on plane add (kind 0), update (1), remove (2).
    public static void onPlaneEvent(int kind, String planeId, int planeType,
                                    float tx, float ty, float tz,
                                    float qx, float qy, float qz, float qw,
                                    float extentX, float extentZ) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null) {
            return;
        }
        if (kind == 2) {
            self.planes.remove(planeId);
            self.sink.onPlaneRemoved(planeId);
            return;
        }
        ARPlane.Type type = planeType == 2 ? ARPlane.Type.VERTICAL
                : planeType == 1 ? ARPlane.Type.HORIZONTAL_DOWN
                : ARPlane.Type.HORIZONTAL_UP;
        ARPlane plane = new ARPlane(planeId, type,
                new ARPose(tx, ty, tz, qx, qy, qz, qw),
                extentX, extentZ, null, ARTrackingState.TRACKING);
        self.planes.put(planeId, plane);
        if (kind == 0) {
            self.sink.onPlaneAdded(plane);
        } else {
            self.sink.onPlaneUpdated(plane);
        }
    }

    /// Called from native code when an anchor's pose or tracking refines.
    public static void onAnchorUpdated(String anchorId,
                                       float tx, float ty, float tz,
                                       float qx, float qy, float qz, float qw,
                                       int state) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null) {
            return;
        }
        self.sink.onAnchorUpdated(anchorId,
                new ARPose(tx, ty, tz, qx, qy, qz, qw), stateFor(state));
    }

    /// Called from native code when the platform removes an anchor.
    public static void onAnchorRemoved(String anchorId) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null) {
            return;
        }
        self.sink.onAnchorRemoved(anchorId);
    }

    /// Called from native code when a registered reference image is detected.
    public static void onImageAnchorAdded(String anchorId, String imageName,
                                          float tx, float ty, float tz,
                                          float qx, float qy, float qz, float qw,
                                          float physicalWidth) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null) {
            return;
        }
        self.sink.onAnchorAdded(new ARImageAnchor(anchorId,
                new ARPose(tx, ty, tz, qx, qy, qz, qw), imageName, physicalWidth));
    }

    /// Called from native code when a face is detected (FACE tracking mode).
    public static void onFaceAnchorAdded(String anchorId,
                                         float tx, float ty, float tz,
                                         float qx, float qy, float qz, float qw) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null) {
            return;
        }
        self.sink.onAnchorAdded(new ARFaceAnchor(anchorId,
                new ARPose(tx, ty, tz, qx, qy, qz, qw)));
    }

    /// Called from native code with world poses for the eye regions, packed as
    /// "lx,ly,lz,lqx,lqy,lqz,lqw;rx,...". ARKit does not supply nose/forehead
    /// region transforms so those stay null on iOS.
    public static void onFaceRegionsUpdated(String anchorId, String packed) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null || packed == null) {
            return;
        }
        List<String> parts = splitChar(packed, ';');
        if (parts.size() < 2) {
            return;
        }
        ARPose[] regions = new ARPose[ARFaceRegion.values().length];
        regions[ARFaceRegion.LEFT_EYE.ordinal()] = parsePose(parts.get(0));
        regions[ARFaceRegion.RIGHT_EYE.ordinal()] = parsePose(parts.get(1));
        self.sink.onFaceAnchorUpdated(anchorId, null, null, regions, null, null);
    }

    private static ARPose parsePose(String csv) {
        List<String> f = splitChar(csv, ',');
        if (f.size() < 7) {
            return null;
        }
        try {
            return new ARPose(Float.parseFloat(f.get(0)), Float.parseFloat(f.get(1)),
                    Float.parseFloat(f.get(2)), Float.parseFloat(f.get(3)),
                    Float.parseFloat(f.get(4)), Float.parseFloat(f.get(5)),
                    Float.parseFloat(f.get(6)));
        } catch (NumberFormatException err) {
            return null;
        }
    }

    /// Called from native code with the throttled per-frame camera pose and
    /// light estimate.
    public static void onCameraFrame(float tx, float ty, float tz,
                                     float qx, float qy, float qz, float qw,
                                     float lightIntensity, float lightR,
                                     float lightG, float lightB, boolean lightValid) {
        IOSARImpl self = ACTIVE;
        if (self == null || self.sink == null) {
            return;
        }
        self.sink.onCameraPose(new ARPose(tx, ty, tz, qx, qy, qz, qw));
        if (lightValid) {
            self.sink.onLightEstimate(new ARLightEstimate(true, lightIntensity,
                    lightR, lightG, lightB));
        }
    }
}
