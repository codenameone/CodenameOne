/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

import com.codename1.ar.ARAnchor;
import com.codename1.ar.ARCapabilities;
import com.codename1.ar.ARFaceAnchor;
import com.codename1.ar.ARFaceRegion;
import com.codename1.ar.ARHitResult;
import com.codename1.ar.ARImageAnchor;
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
import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.Quaternion;
import com.codename1.gpu.Texture;
import com.codename1.impl.ARImpl;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimerTask;

/// Simulated AR backend for the Codename One simulator, modeled on the
/// Android emulator's ARCore "virtual scene": the session runs against a
/// virtual room whose floor and wall planes are "detected" shortly after the
/// session opens, mimicking real AR startup. The AR view renders the room
/// plus every anchored `ARNode` through the port's software 3D device, with
/// the virtual device camera driven by the mouse (drag to look) and keyboard
/// (WASD / arrow keys to move, Q / E for up / down).
///
/// The full core event pipeline - plane events, anchors, hit tests, image
/// and face anchors, light estimates - flows through the same
/// `ARImpl.EventSink` contract the native backends use, so application AR
/// logic is debuggable in the simulator with breakpoints. The
/// Simulate - AR Simulation window (`ARSimulation`) drives error states,
/// lighting, image detection and face toggling.
public class JavaSEARImpl extends ARImpl {

    /// The floor of the virtual room sits 1.4 meters below the session
    /// origin, roughly a phone held at chest height.
    static final float FLOOR_Y = -1.4f;
    private static final float FLOOR_EXTENT = 6f;
    private static final float WALL_Z = -3f;
    private static final float WALL_HEIGHT = 3f;
    private static final float FOV_Y_DEGREES = 60f;

    /// The impl behind the currently open session, reachable by the
    /// Simulate menu. Null when no AR session is open.
    static volatile JavaSEARImpl activeInstance;

    private EventSink sink;
    private ARSessionOptions options;
    private volatile boolean closed;
    private volatile boolean paused;
    private java.util.Timer timer;

    private final Object sceneLock = new Object();
    private float camX;
    private float camY;
    private float camZ;
    private float camYaw;
    private float camPitch;

    private final List<ARPlane> planes = new ArrayList<ARPlane>();
    private final LinkedHashMap<String, SimAnchor> anchors = new LinkedHashMap<String, SimAnchor>();
    private int anchorSeq;
    private int planeSeq;
    private String faceAnchorId;

    private ARSurface surface;

    private static final class SimAnchor {
        final String id;
        ARPose pose;
        ARNode node;

        SimAnchor(String id, ARPose pose) {
            this.id = id;
            this.pose = pose;
        }
    }

    @Override
    public ARCapabilities getCapabilities() {
        return new ARCapabilities(true, true, true, true, true);
    }

    @Override
    public void setEventSink(EventSink sink) {
        this.sink = sink;
    }

    @Override
    public void open(ARSessionOptions opts) {
        this.options = opts == null ? new ARSessionOptions() : opts;
        activeInstance = this;
        timer = new java.util.Timer("ar-simulation", true);
        schedule(200, new Runnable() {
            public void run() {
                sink.onTrackingStateChanged(ARTrackingState.LIMITED,
                        ARTrackingFailureReason.INITIALIZING);
            }
        });
        schedule(1000, new Runnable() {
            public void run() {
                sink.onTrackingStateChanged(ARTrackingState.TRACKING,
                        ARTrackingFailureReason.NONE);
                sink.onLightEstimate(new ARLightEstimate(true, 1f, 1f, 1f, 1f));
                publishCameraPose();
            }
        });
        if (options.getTrackingMode() == ARTrackingMode.FACE) {
            schedule(1200, new Runnable() {
                public void run() {
                    simSetFaceVisible(true);
                }
            });
        } else {
            if (options.getPlaneDetection().includesHorizontal()) {
                schedule(1500, new Runnable() {
                    public void run() {
                        detectFloor();
                    }
                });
            }
            if (options.getPlaneDetection().includesVertical()) {
                schedule(2200, new Runnable() {
                    public void run() {
                        detectWall();
                    }
                });
            }
        }
    }

    private void schedule(long delayMillis, final Runnable r) {
        java.util.Timer t = timer;
        if (t == null) {
            return;
        }
        t.schedule(new TimerTask() {
            @Override public void run() {
                if (!closed && !paused) {
                    try {
                        r.run();
                    } catch (Throwable err) {
                        err.printStackTrace();
                    }
                }
            }
        }, delayMillis);
    }

    private void detectFloor() {
        ARPlane floor = new ARPlane("sim-plane-floor-" + (++planeSeq),
                ARPlane.Type.HORIZONTAL_UP,
                new ARPose(0f, FLOOR_Y, 0f, 0f, 0f, 0f, 1f),
                FLOOR_EXTENT, FLOOR_EXTENT, null, ARTrackingState.TRACKING);
        synchronized (sceneLock) {
            planes.add(floor);
        }
        sink.onPlaneAdded(floor);
    }

    private void detectWall() {
        // The wall faces the viewer: its normal is world +Z, so the plane's
        // local Y axis (the normal) is rotated onto +Z by a 90 degree
        // rotation around X.
        float s = (float) Math.sin(Math.PI / 4);
        float c = (float) Math.cos(Math.PI / 4);
        ARPlane wall = new ARPlane("sim-plane-wall-" + (++planeSeq),
                ARPlane.Type.VERTICAL,
                new ARPose(0f, FLOOR_Y + WALL_HEIGHT / 2f, WALL_Z, s, 0f, 0f, c),
                FLOOR_EXTENT, WALL_HEIGHT, null, ARTrackingState.TRACKING);
        synchronized (sceneLock) {
            planes.add(wall);
        }
        sink.onPlaneAdded(wall);
    }

    @Override
    public PeerComponent createViewPeer() {
        surface = new ARSurface();
        return PeerComponent.create(surface);
    }

    @Override
    public void hitTest(float xNorm, float yNorm, final AsyncResource<ARHitResult[]> result) {
        final ARHitResult[] hits = computeHits(xNorm, yNorm);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                result.complete(hits);
            }
        });
    }

    private ARHitResult[] computeHits(float xNorm, float yNorm) {
        float ox;
        float oy;
        float oz;
        float[] dir = new float[3];
        ARPlane[] planeSnapshot;
        synchronized (sceneLock) {
            ox = camX;
            oy = camY;
            oz = camZ;
            // Camera-space ray through the normalized view point.
            float aspect = surface == null || surface.getHeight() == 0 ? 1.5f
                    : (float) surface.getWidth() / (float) surface.getHeight();
            float tanHalf = (float) Math.tan(Math.toRadians(FOV_Y_DEGREES / 2));
            dir[0] = (xNorm * 2f - 1f) * tanHalf * aspect;
            dir[1] = (1f - yNorm * 2f) * tanHalf;
            dir[2] = -1f;
            float[] q = orientationQuat();
            Quaternion.rotateVector(q, dir);
            planeSnapshot = planes.toArray(new ARPlane[planes.size()]);
        }
        float dlen = (float) Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        dir[0] /= dlen;
        dir[1] /= dlen;
        dir[2] /= dlen;

        List<ARHitResult> hits = new ArrayList<ARHitResult>();
        for (ARPlane plane : planeSnapshot) {
            ARPose center = plane.getCenterPose();
            if (plane.getType() == ARPlane.Type.VERTICAL) {
                // Ray vs the z = WALL_Z plane.
                if (Math.abs(dir[2]) < 1e-6f) {
                    continue;
                }
                float t = (center.getTz() - oz) / dir[2];
                if (t <= 0) {
                    continue;
                }
                float px = ox + dir[0] * t;
                float py = oy + dir[1] * t;
                if (Math.abs(px - center.getTx()) > plane.getExtentX() / 2
                        || Math.abs(py - center.getTy()) > plane.getExtentZ() / 2) {
                    continue;
                }
                hits.add(new ARHitResult(new ARPose(px, py, center.getTz(),
                        center.getQx(), center.getQy(), center.getQz(), center.getQw()),
                        t, ARHitResult.Type.PLANE, plane, null));
            } else {
                // Ray vs the y = FLOOR_Y plane.
                if (Math.abs(dir[1]) < 1e-6f) {
                    continue;
                }
                float t = (center.getTy() - oy) / dir[1];
                if (t <= 0) {
                    continue;
                }
                float px = ox + dir[0] * t;
                float pz = oz + dir[2] * t;
                if (Math.abs(px - center.getTx()) > plane.getExtentX() / 2
                        || Math.abs(pz - center.getTz()) > plane.getExtentZ() / 2) {
                    continue;
                }
                hits.add(new ARHitResult(new ARPose(px, center.getTy(), pz, 0f, 0f, 0f, 1f),
                        t, ARHitResult.Type.PLANE, plane, null));
            }
        }
        // Nearest first.
        for (int i = 0; i < hits.size(); i++) {
            for (int j = i + 1; j < hits.size(); j++) {
                if (hits.get(j).getDistance() < hits.get(i).getDistance()) {
                    ARHitResult tmp = hits.get(i);
                    hits.set(i, hits.get(j));
                    hits.set(j, tmp);
                }
            }
        }
        return hits.toArray(new ARHitResult[hits.size()]);
    }

    @Override
    public String createAnchor(ARPose pose) {
        String id = "sim-anchor-" + (++anchorSeq);
        synchronized (sceneLock) {
            anchors.put(id, new SimAnchor(id, pose));
        }
        repaintSurface();
        return id;
    }

    @Override
    public String createAnchorFromHit(Object nativeHandle, ARPose pose) {
        return createAnchor(pose);
    }

    @Override
    public void removeAnchor(String anchorId) {
        synchronized (sceneLock) {
            anchors.remove(anchorId);
        }
        repaintSurface();
    }

    @Override
    public void setAnchorNode(String anchorId, ARNode node) {
        synchronized (sceneLock) {
            SimAnchor a = anchors.get(anchorId);
            if (a != null) {
                a.node = node;
            }
        }
        repaintSurface();
    }

    @Override
    public void nodeChanged(String anchorId, ARNode node) {
        repaintSurface();
    }

    @Override
    public void requestPermissions(final AsyncResource<Boolean> result) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                result.complete(Boolean.TRUE);
            }
        });
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
        publishCameraPose();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (surface != null) {
            surface.stopTimer();
        }
        if (activeInstance == this) {
            activeInstance = null;
        }
    }

    // ------------------------------------------------------------------
    // Hooks for the Simulate > AR Simulation window
    // ------------------------------------------------------------------

    /// The session configuration, for the simulation window's status display.
    ARSessionOptions getOpenOptions() {
        return options;
    }

    void simSetTrackingState(ARTrackingState state, ARTrackingFailureReason reason) {
        if (!closed) {
            sink.onTrackingStateChanged(state, reason);
        }
    }

    void simSetLight(float intensity, float r, float g, float b) {
        if (!closed) {
            sink.onLightEstimate(new ARLightEstimate(true, intensity, r, g, b));
        }
    }

    String[] simGetReferenceImageNames() {
        ARReferenceImage[] imgs = options == null
                ? new ARReferenceImage[0] : options.getReferenceImages();
        String[] names = new String[imgs.length];
        for (int i = 0; i < imgs.length; i++) {
            names[i] = imgs[i].getName();
        }
        return names;
    }

    /// Simulates the camera recognizing a registered reference image one
    /// meter in front of the current virtual camera.
    void simDetectImage(String name) {
        if (closed) {
            return;
        }
        float physicalWidth = 0.3f;
        ARReferenceImage[] imgs = options.getReferenceImages();
        for (ARReferenceImage img : imgs) {
            if (img.getName().equals(name)) {
                physicalWidth = img.getPhysicalWidthMeters();
            }
        }
        ARPose pose = poseInFrontOfCamera(1f);
        String id = "sim-image-" + (++anchorSeq);
        synchronized (sceneLock) {
            anchors.put(id, new SimAnchor(id, pose));
        }
        sink.onAnchorAdded(new ARImageAnchor(id, pose, name, physicalWidth));
        repaintSurface();
    }

    /// Adds or removes the simulated face anchor (FACE tracking mode).
    void simSetFaceVisible(boolean visible) {
        if (closed) {
            return;
        }
        if (visible) {
            if (faceAnchorId != null) {
                return;
            }
            ARPose pose = poseInFrontOfCamera(0.4f);
            faceAnchorId = "sim-face-" + (++anchorSeq);
            synchronized (sceneLock) {
                anchors.put(faceAnchorId, new SimAnchor(faceAnchorId, pose));
            }
            ARFaceAnchor face = new ARFaceAnchor(faceAnchorId, pose);
            sink.onAnchorAdded(face);
            // Ship a basic geometry payload so getRegionPose()/mesh code paths
            // are exercised in the simulator.
            ARPose[] regions = new ARPose[ARFaceRegion.values().length];
            regions[ARFaceRegion.NOSE_TIP.ordinal()] = pose.transform(
                    new ARPose(0f, 0f, 0.06f, 0f, 0f, 0f, 1f));
            regions[ARFaceRegion.FOREHEAD_LEFT.ordinal()] = pose.transform(
                    new ARPose(-0.05f, 0.08f, 0.02f, 0f, 0f, 0f, 1f));
            regions[ARFaceRegion.FOREHEAD_RIGHT.ordinal()] = pose.transform(
                    new ARPose(0.05f, 0.08f, 0.02f, 0f, 0f, 0f, 1f));
            float[] meshVertices = {
                    -0.08f, -0.1f, 0f,
                    0.08f, -0.1f, 0f,
                    0.08f, 0.1f, 0f,
                    -0.08f, 0.1f, 0f
            };
            int[] meshTriangles = {0, 1, 2, 0, 2, 3};
            sink.onFaceAnchorUpdated(faceAnchorId, pose, ARTrackingState.TRACKING,
                    regions, meshVertices, meshTriangles);
        } else {
            if (faceAnchorId == null) {
                return;
            }
            String id = faceAnchorId;
            faceAnchorId = null;
            synchronized (sceneLock) {
                anchors.remove(id);
            }
            sink.onAnchorRemoved(id);
        }
        repaintSurface();
    }

    boolean simIsFaceVisible() {
        return faceAnchorId != null;
    }

    /// Clears and re-runs the plane detection sequence.
    void simRedetectPlanes() {
        if (closed || options.getTrackingMode() == ARTrackingMode.FACE) {
            return;
        }
        ARPlane[] old;
        synchronized (sceneLock) {
            old = planes.toArray(new ARPlane[planes.size()]);
            planes.clear();
        }
        for (ARPlane p : old) {
            sink.onPlaneRemoved(p.getId());
        }
        if (options.getPlaneDetection().includesHorizontal()) {
            schedule(600, new Runnable() {
                public void run() {
                    detectFloor();
                }
            });
        }
        if (options.getPlaneDetection().includesVertical()) {
            schedule(1200, new Runnable() {
                public void run() {
                    detectWall();
                }
            });
        }
        repaintSurface();
    }

    // ------------------------------------------------------------------
    // Virtual camera
    // ------------------------------------------------------------------

    private float[] orientationQuat() {
        float[] yawQ = Quaternion.fromAxisAngle(-camYaw, 0f, 1f, 0f);
        float[] pitchQ = Quaternion.fromAxisAngle(camPitch, 1f, 0f, 0f);
        float[] q = new float[4];
        Quaternion.multiply(yawQ, pitchQ, q);
        return q;
    }

    private ARPose cameraPose() {
        float[] q;
        float x;
        float y;
        float z;
        synchronized (sceneLock) {
            q = orientationQuat();
            x = camX;
            y = camY;
            z = camZ;
        }
        return new ARPose(x, y, z, q[0], q[1], q[2], q[3]);
    }

    private ARPose poseInFrontOfCamera(float distanceMeters) {
        return cameraPose().transform(
                new ARPose(0f, 0f, -distanceMeters, 0f, 0f, 0f, 1f));
    }

    private void publishCameraPose() {
        if (!closed && !paused && sink != null) {
            sink.onCameraPose(cameraPose());
        }
    }

    private void repaintSurface() {
        ARSurface s = surface;
        if (s != null) {
            s.repaint();
        }
    }

    // ------------------------------------------------------------------
    // The Swing rendering surface
    // ------------------------------------------------------------------

    /// Renders the virtual room and anchored content through the port's
    /// software 3D device, and owns the mouse / keyboard camera controls.
    private final class ARSurface extends JComponent {
        private final JavaSESoftwareDevice device = new JavaSESoftwareDevice();
        private final Camera camera = new Camera();
        private final Light light = new Light();
        private final IdentityHashMap<ARModel, Material> materials =
                new IdentityHashMap<ARModel, Material>();
        private Mesh floorMesh;
        private Mesh wallMesh;
        private Mesh anchorMarker;
        private Material floorMaterial;
        private Material wallMaterial;
        private Material markerMaterial;
        private boolean initialized;
        private int lastW = -1;
        private int lastH = -1;
        private final javax.swing.Timer repaintTimer;
        private int lastMouseX;
        private int lastMouseY;

        ARSurface() {
            setOpaque(true);
            setFocusable(true);
            // A gentle continuous repaint keeps the camera-pose publishing
            // and view fresh, mirroring a live camera feed.
            repaintTimer = new javax.swing.Timer(50, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    repaint();
                }
            });
            repaintTimer.start();
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    requestFocusInWindow();
                }

                @Override public void mouseDragged(MouseEvent e) {
                    synchronized (sceneLock) {
                        camYaw += (e.getX() - lastMouseX) * 0.006f;
                        camPitch -= (e.getY() - lastMouseY) * 0.006f;
                        float limit = (float) Math.toRadians(89);
                        if (camPitch > limit) {
                            camPitch = limit;
                        }
                        if (camPitch < -limit) {
                            camPitch = -limit;
                        }
                    }
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    publishCameraPose();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    float step = 0.08f;
                    float fx = 0;
                    float fz = 0;
                    float dy = 0;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W:
                        case KeyEvent.VK_UP:
                            fz = -step;
                            break;
                        case KeyEvent.VK_S:
                        case KeyEvent.VK_DOWN:
                            fz = step;
                            break;
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_LEFT:
                            fx = -step;
                            break;
                        case KeyEvent.VK_D:
                        case KeyEvent.VK_RIGHT:
                            fx = step;
                            break;
                        case KeyEvent.VK_Q:
                            dy = step;
                            break;
                        case KeyEvent.VK_E:
                            dy = -step;
                            break;
                        default:
                            return;
                    }
                    synchronized (sceneLock) {
                        float sin = (float) Math.sin(camYaw);
                        float cos = (float) Math.cos(camYaw);
                        // Move in the yaw-rotated horizontal frame.
                        camX += fx * cos - fz * sin;
                        camZ += fx * sin + fz * cos;
                        camY += dy;
                    }
                    publishCameraPose();
                }
            });
        }

        void stopTimer() {
            repaintTimer.stop();
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            repaintTimer.stop();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (!closed) {
                repaintTimer.start();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            device.resize(w, h);
            if (!initialized) {
                initScene();
                initialized = true;
            }
            if (w != lastW || h != lastH) {
                lastW = w;
                lastH = h;
                device.setViewport(0, 0, w, h);
                camera.setAspect((float) w / (float) h);
            }
            renderScene();
            BufferedImage img = device.getImage();
            if (img != null) {
                g.drawImage(img, 0, 0, null);
            }
        }

        private void initScene() {
            // The software rasterizer rejects triangles that cross the near
            // plane instead of clipping them, so the room surfaces must be
            // tessellated: only the cells at the viewer's feet drop out
            // rather than the whole plane.
            floorMesh = gridMesh(FLOOR_EXTENT, FLOOR_EXTENT, 16, 16);
            wallMesh = gridMesh(FLOOR_EXTENT, WALL_HEIGHT, 16, 8);
            anchorMarker = Primitives.cube(device, 0.06f);
            floorMaterial = new Material(Material.Type.UNLIT)
                    .setTexture(createGridTexture(0xff3c78b4, 0xff2d5a87));
            wallMaterial = new Material(Material.Type.UNLIT)
                    .setTexture(createGridTexture(0xff6a6a72, 0xff55555c));
            markerMaterial = new Material(Material.Type.UNLIT).setColor(0xffffc020);
            light.setDirection(-0.4f, -1f, -0.3f);
            light.setColor(0xffffffff);
            camera.setPerspective(FOV_Y_DEGREES, 0.05f, 100f);
        }

        /// Builds a subdivided quad in the XY plane facing +Z, centered at the
        /// origin, with 0..1 texture coordinates - Primitives.quad with
        /// tessellation.
        private Mesh gridMesh(float width, float height, int cols, int rows) {
            int vertexCount = (cols + 1) * (rows + 1);
            float[] v = new float[vertexCount * 8];
            int o = 0;
            for (int r = 0; r <= rows; r++) {
                float fy = (float) r / rows;
                for (int c = 0; c <= cols; c++) {
                    float fx = (float) c / cols;
                    v[o] = (fx - 0.5f) * width;
                    v[o + 1] = (fy - 0.5f) * height;
                    v[o + 2] = 0f;
                    v[o + 3] = 0f;
                    v[o + 4] = 0f;
                    v[o + 5] = 1f;
                    v[o + 6] = fx;
                    v[o + 7] = 1f - fy;
                    o += 8;
                }
            }
            int[] idx = new int[cols * rows * 6];
            int i = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int first = r * (cols + 1) + c;
                    int second = first + cols + 1;
                    idx[i] = first;
                    idx[i + 1] = first + 1;
                    idx[i + 2] = second + 1;
                    idx[i + 3] = first;
                    idx[i + 4] = second + 1;
                    idx[i + 5] = second;
                    i += 6;
                }
            }
            com.codename1.gpu.VertexBuffer vb = device.createVertexBuffer(
                    com.codename1.gpu.VertexFormat.POSITION_NORMAL_TEXCOORD, vertexCount);
            vb.setData(v);
            com.codename1.gpu.IndexBuffer ib = device.createIndexBuffer(idx.length);
            ib.setData(idx);
            return new Mesh(vb, ib, com.codename1.gpu.PrimitiveType.TRIANGLES);
        }

        private Texture createGridTexture(int lineColor, int fillColor) {
            int size = 256;
            int cell = 32;
            int[] argb = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean line = x % cell == 0 || y % cell == 0;
                    argb[y * size + x] = line ? lineColor : fillColor;
                }
            }
            Texture t = device.createTexture(size, size, argb);
            t.setFilter(Texture.Filter.LINEAR);
            return t;
        }

        private void renderScene() {
            float[] q;
            float ex;
            float ey;
            float ez;
            ARPlane[] planeSnapshot;
            Object[][] anchorSnapshot;
            synchronized (sceneLock) {
                q = orientationQuat();
                ex = camX;
                ey = camY;
                ez = camZ;
                planeSnapshot = planes.toArray(new ARPlane[planes.size()]);
                anchorSnapshot = new Object[anchors.size()][];
                int i = 0;
                for (SimAnchor a : anchors.values()) {
                    anchorSnapshot[i++] = new Object[]{a.pose, a.node};
                }
            }
            float[] fwd = {0f, 0f, -1f};
            Quaternion.rotateVector(q, fwd);
            float[] up = {0f, 1f, 0f};
            Quaternion.rotateVector(q, up);
            camera.setPosition(ex, ey, ez);
            camera.setTarget(ex + fwd[0], ey + fwd[1], ez + fwd[2]);
            camera.setUp(up[0], up[1], up[2]);

            device.clear(0xff26303a, true, true);
            device.setCamera(camera);
            device.setLight(light);

            for (ARPlane plane : planeSnapshot) {
                if (plane.getType() == ARPlane.Type.VERTICAL) {
                    // The tessellated wall quad already faces +Z which is the
                    // wall normal.
                    float[] m = Matrix4.translation(plane.getCenterPose().getTx(),
                            plane.getCenterPose().getTy(), plane.getCenterPose().getTz());
                    device.draw(wallMesh, wallMaterial, m);
                } else {
                    // Lay the +Z-facing quad flat (normal up) on the floor.
                    float[] m = new float[16];
                    Matrix4.multiply(Matrix4.translation(plane.getCenterPose().getTx(),
                                    plane.getCenterPose().getTy(), plane.getCenterPose().getTz()),
                            Matrix4.rotation((float) (-Math.PI / 2), 1f, 0f, 0f), m);
                    device.draw(floorMesh, floorMaterial, m);
                }
            }

            for (Object[] entry : anchorSnapshot) {
                ARPose pose = (ARPose) entry[0];
                ARNode node = (ARNode) entry[1];
                float[] anchorMatrix = pose.toMatrix();
                device.draw(anchorMarker, markerMaterial, anchorMatrix);
                if (node != null) {
                    drawNode(anchorMatrix, node);
                }
            }
        }

        private void drawNode(float[] parentMatrix, ARNode node) {
            if (!node.isVisible()) {
                return;
            }
            float[] local = Matrix4.translation(node.getLocalX(), node.getLocalY(),
                    node.getLocalZ());
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
                if (mesh != null) {
                    device.draw(mesh, materialFor(model), world);
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                drawNode(world, node.getChildAt(i));
            }
        }

        private Material materialFor(ARModel model) {
            Material m = materials.get(model);
            if (m == null) {
                com.codename1.ui.Image img = model.getBaseColorImage();
                if (img != null) {
                    Texture t = device.createTexture(img);
                    t.setFilter(Texture.Filter.LINEAR);
                    m = new Material(Material.Type.PHONG).setTexture(t);
                } else {
                    m = new Material(Material.Type.PHONG).setColor(model.getColor());
                }
                materials.put(model, m);
            }
            return m;
        }
    }
}
