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
package com.codename1.impl.android.ar;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Quaternion;
import com.codename1.impl.ARImpl;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Android implementation of {@link ARImpl} backed by ARCore (Google Play
 * Services for AR). This package compiles against {@code com.google.ar.core}
 * which only exists when the build detects {@code com.codename1.ar} usage and
 * adds the ARCore Gradle dependency; for non-AR apps the whole
 * {@code com.codename1.impl.android.ar} package is deleted from the generated
 * project, so {@code AndroidImplementation} reaches this class through
 * reflection only.
 *
 * ARCore supplies tracking but no renderer: {@link ARCoreView} owns the
 * GLSurfaceView that draws the camera background and the anchored content,
 * and drives {@link #processFrame(Frame, Camera)} once per rendered frame.
 */
public class AndroidARImpl extends ARImpl {

    private final Activity activity;
    private EventSink sink;
    private Session session;
    private ARSessionOptions options;
    private ARCoreView view;
    private volatile boolean closed;

    private final Object sceneLock = new Object();
    private final Map<String, Anchor> anchors = new HashMap<String, Anchor>();
    private final Map<Plane, String> planeIds = new HashMap<Plane, String>();
    private final Map<String, ARPlane> planeSnapshots = new HashMap<String, ARPlane>();
    private final Map<AugmentedImage, String> imageIds = new HashMap<AugmentedImage, String>();
    private final Map<AugmentedFace, String> faceIds = new HashMap<AugmentedFace, String>();
    private final Map<String, AugmentedImage> imagesById = new HashMap<String, AugmentedImage>();
    private final Map<String, AugmentedFace> facesById = new HashMap<String, AugmentedFace>();
    private final List<PendingHit> pendingHits = new ArrayList<PendingHit>();
    private final List<HitResult> recentHits = new ArrayList<HitResult>();
    private final List<MeshEntry> meshes = new ArrayList<MeshEntry>();
    private final Map<ARModel, Bitmap> textureCache = new HashMap<ARModel, Bitmap>();
    private int idSeq;
    private int frameCounter;
    private int lastTrackingCode = -1;
    private int lastReasonCode = -1;

    private static final class PendingHit {
        final float xNorm;
        final float yNorm;
        final AsyncResource<ARHitResult[]> result;

        PendingHit(float xNorm, float yNorm, AsyncResource<ARHitResult[]> result) {
            this.xNorm = xNorm;
            this.yNorm = yNorm;
            this.result = result;
        }
    }

    /**
     * One flattened renderable: an anchored mesh with its transform relative
     * to the anchor. {@link ARCoreView} uploads the GL buffers lazily on the
     * render thread and stores the handles here.
     */
    static final class MeshEntry {
        final String anchorId;
        final float[] interleaved;
        final int vertexCount;
        final int[] indices;
        final int argbColor;
        final Bitmap texture;
        final float[] anchorLocal16;
        // GL handles owned by the render thread.
        int vbo;
        int ibo;
        int textureId;

        MeshEntry(String anchorId, float[] interleaved, int vertexCount, int[] indices,
                  int argbColor, Bitmap texture, float[] anchorLocal16) {
            this.anchorId = anchorId;
            this.interleaved = interleaved;
            this.vertexCount = vertexCount;
            this.indices = indices;
            this.argbColor = argbColor;
            this.texture = texture;
            this.anchorLocal16 = anchorLocal16;
        }
    }

    public AndroidARImpl(Activity activity) {
        this.activity = activity;
    }

    @Override
    public ARCapabilities getCapabilities() {
        boolean supported;
        try {
            ArCoreApk.Availability availability =
                    ArCoreApk.getInstance().checkAvailability(activity);
            supported = availability.isSupported()
                    || availability == ArCoreApk.Availability.UNKNOWN_CHECKING;
        } catch (Throwable t) {
            supported = false;
        }
        return new ARCapabilities(supported, supported, supported, supported, supported);
    }

    @Override
    public void setEventSink(EventSink sink) {
        this.sink = sink;
    }

    @Override
    public void open(ARSessionOptions opts) throws IOException {
        this.options = opts;
        if (!AndroidImplementation.checkForPermission(Manifest.permission.CAMERA,
                "AR needs the camera")) {
            throw new IOException("The camera permission was denied");
        }
        try {
            ArCoreApk.InstallStatus install =
                    ArCoreApk.getInstance().requestInstall(activity, true);
            if (install == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                // Play Services for AR installation was launched; the app
                // resumes afterwards and should retry AR.open then.
                throw new IOException(
                        "ARCore installation was requested; retry once it completes");
            }
            if (opts.getTrackingMode() == ARTrackingMode.FACE) {
                session = new Session(activity, EnumSet.of(Session.Feature.FRONT_CAMERA));
            } else {
                session = new Session(activity);
            }
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setFocusMode(Config.FocusMode.AUTO);
            config.setLightEstimationMode(opts.isLightEstimation()
                    ? Config.LightEstimationMode.AMBIENT_INTENSITY
                    : Config.LightEstimationMode.DISABLED);
            if (opts.getTrackingMode() == ARTrackingMode.FACE) {
                config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
                config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
            } else {
                boolean h = opts.getPlaneDetection().includesHorizontal();
                boolean v = opts.getPlaneDetection().includesVertical();
                config.setPlaneFindingMode(h && v
                        ? Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                        : v ? Config.PlaneFindingMode.VERTICAL
                        : h ? Config.PlaneFindingMode.HORIZONTAL
                        : Config.PlaneFindingMode.DISABLED);
                ARReferenceImage[] images = opts.getReferenceImages();
                if (images.length > 0) {
                    AugmentedImageDatabase db = new AugmentedImageDatabase(session);
                    for (ARReferenceImage img : images) {
                        byte[] bytes = img.getEncodedImage();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            db.addImage(img.getName(), bitmap, img.getPhysicalWidthMeters());
                        }
                    }
                    config.setAugmentedImageDatabase(db);
                }
            }
            session.configure(config);
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            closeSessionQuietly();
            throw new IOException("Could not start ARCore: " + t, t);
        }
    }

    private void closeSessionQuietly() {
        Session s = session;
        session = null;
        if (s != null) {
            try {
                s.close();
            } catch (Throwable ignore) {
            }
        }
    }

    Session getSession() {
        return session;
    }

    Activity getActivity() {
        return activity;
    }

    @Override
    public PeerComponent createViewPeer() {
        if (session == null) {
            return null;
        }
        // The GLSurfaceView must be constructed on the Android UI thread.
        final ARCoreView[] holder = new ARCoreView[1];
        final CountDownLatch latch = new CountDownLatch(1);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    holder[0] = new ARCoreView(AndroidARImpl.this);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        view = holder[0];
        if (view == null) {
            return null;
        }
        return PeerComponent.create(view);
    }

    @Override
    public void hitTest(float xNorm, float yNorm, AsyncResource<ARHitResult[]> result) {
        synchronized (sceneLock) {
            if (closed || session == null) {
                completeHits(result, new ARHitResult[0]);
                return;
            }
            // ARCore hit tests need a Frame; queue for the render loop.
            pendingHits.add(new PendingHit(xNorm, yNorm, result));
        }
    }

    private void completeHits(final AsyncResource<ARHitResult[]> result, final ARHitResult[] hits) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                result.complete(hits);
            }
        });
    }

    @Override
    public String createAnchor(ARPose pose) {
        return registerAnchor(session.createAnchor(toPose(pose)));
    }

    @Override
    public String createAnchorFromHit(Object nativeHandle, ARPose pose) {
        if (nativeHandle instanceof HitResult) {
            synchronized (sceneLock) {
                if (recentHits.contains(nativeHandle)) {
                    try {
                        return registerAnchor(((HitResult) nativeHandle).createAnchor());
                    } catch (Throwable t) {
                        // Fall back to a plain pose anchor below.
                    }
                }
            }
        }
        return createAnchor(pose);
    }

    private String registerAnchor(Anchor anchor) {
        String id = "arcore-anchor-" + (++idSeq);
        synchronized (sceneLock) {
            anchors.put(id, anchor);
        }
        return id;
    }

    private static Pose toPose(ARPose pose) {
        return new Pose(
                new float[]{pose.getTx(), pose.getTy(), pose.getTz()},
                new float[]{pose.getQx(), pose.getQy(), pose.getQz(), pose.getQw()});
    }

    private static ARPose fromPose(Pose pose) {
        float[] t = new float[3];
        float[] q = new float[4];
        pose.getTranslation(t, 0);
        pose.getRotationQuaternion(q, 0);
        return new ARPose(t[0], t[1], t[2], q[0], q[1], q[2], q[3]);
    }

    @Override
    public void removeAnchor(String anchorId) {
        Anchor anchor;
        synchronized (sceneLock) {
            anchor = anchors.remove(anchorId);
            removeMeshesFor(anchorId);
        }
        if (anchor != null) {
            anchor.detach();
        }
    }

    private void removeMeshesFor(String anchorId) {
        for (int i = meshes.size() - 1; i >= 0; i--) {
            if (meshes.get(i).anchorId.equals(anchorId)) {
                MeshEntry dead = meshes.remove(i);
                if (view != null) {
                    view.recycleEntry(dead);
                }
            }
        }
    }

    @Override
    public void setAnchorNode(String anchorId, ARNode node) {
        synchronized (sceneLock) {
            removeMeshesFor(anchorId);
            if (node != null) {
                flatten(anchorId, node, Matrix4.identity());
            }
        }
    }

    @Override
    public void nodeChanged(String anchorId, ARNode node) {
        setAnchorNode(anchorId, node);
    }

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
                meshes.add(new MeshEntry(anchorId, mesh.getVertices().getData(), vertexCount,
                        indices, model.getColor(), textureBitmap(model), world));
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            flatten(anchorId, node.getChildAt(i), world);
        }
    }

    private Bitmap textureBitmap(ARModel model) {
        if (textureCache.containsKey(model)) {
            return textureCache.get(model);
        }
        Bitmap bitmap = null;
        Image img = model.getBaseColorImage();
        if (img != null) {
            byte[] bytes;
            if (img instanceof EncodedImage) {
                bytes = ((EncodedImage) img).getImageData();
            } else {
                bytes = EncodedImage.createFromImage(img, false).getImageData();
            }
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        textureCache.put(model, bitmap);
        return bitmap;
    }

    @Override
    public void requestPermissions(final AsyncResource<Boolean> result) {
        final boolean granted = AndroidImplementation.checkForPermission(
                Manifest.permission.CAMERA, "AR needs the camera");
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                result.complete(Boolean.valueOf(granted));
            }
        });
    }

    @Override
    public void pause() {
        if (view != null) {
            view.pauseView();
        }
        Session s = session;
        if (s != null) {
            s.pause();
        }
    }

    @Override
    public void resume() {
        Session s = session;
        if (s != null) {
            try {
                s.resume();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        if (view != null) {
            view.resumeView();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        List<PendingHit> orphans;
        synchronized (sceneLock) {
            orphans = new ArrayList<PendingHit>(pendingHits);
            pendingHits.clear();
            anchors.clear();
            meshes.clear();
            recentHits.clear();
        }
        for (PendingHit hit : orphans) {
            completeHits(hit.result, new ARHitResult[0]);
        }
        if (view != null) {
            view.pauseView();
        }
        Session s = session;
        session = null;
        if (s != null) {
            try {
                s.pause();
            } catch (Throwable ignore) {
            }
            try {
                s.close();
            } catch (Throwable ignore) {
            }
        }
    }

    boolean isClosed() {
        return closed;
    }

    /**
     * Snapshot of the anchored meshes plus their current anchor poses, taken
     * by the render thread each frame.
     */
    List<MeshEntry> meshSnapshot(Map<String, float[]> anchorPoseMatrices) {
        synchronized (sceneLock) {
            for (Map.Entry<String, Anchor> e : anchors.entrySet()) {
                if (e.getValue().getTrackingState() == TrackingState.TRACKING) {
                    float[] m = new float[16];
                    e.getValue().getPose().toMatrix(m, 0);
                    anchorPoseMatrices.put(e.getKey(), m);
                }
            }
            for (Map.Entry<String, AugmentedImage> e : imagesById.entrySet()) {
                float[] m = new float[16];
                e.getValue().getCenterPose().toMatrix(m, 0);
                anchorPoseMatrices.put(e.getKey(), m);
            }
            for (Map.Entry<String, AugmentedFace> e : facesById.entrySet()) {
                float[] m = new float[16];
                e.getValue().getCenterPose().toMatrix(m, 0);
                anchorPoseMatrices.put(e.getKey(), m);
            }
            return new ArrayList<MeshEntry>(meshes);
        }
    }

    /**
     * Called by {@link ARCoreView} on the GL thread after every
     * {@code session.update()}: publishes tracking / plane / anchor / image /
     * face / light events through the sink (which marshals to the EDT) and
     * services queued hit tests.
     */
    void processFrame(Frame frame, Camera camera) {
        if (closed || sink == null) {
            return;
        }
        publishTracking(camera);
        servicePendingHits(frame, camera);
        if (options.getTrackingMode() == ARTrackingMode.FACE) {
            processFaces(frame);
        } else {
            processPlanes(frame);
            processImages(frame);
        }
        publishAnchors();
        frameCounter++;
        if (frameCounter % 6 == 0) {
            sink.onCameraPose(fromPose(camera.getDisplayOrientedPose()));
            publishLight(frame);
        }
    }

    private void publishTracking(Camera camera) {
        int state;
        int reason = 0;
        TrackingState ts = camera.getTrackingState();
        if (ts == TrackingState.TRACKING) {
            state = 2;
        } else if (ts == TrackingState.PAUSED) {
            state = 1;
            TrackingFailureReason r = camera.getTrackingFailureReason();
            if (r == TrackingFailureReason.EXCESSIVE_MOTION) {
                reason = 2;
            } else if (r == TrackingFailureReason.INSUFFICIENT_LIGHT) {
                reason = 3;
            } else if (r == TrackingFailureReason.INSUFFICIENT_FEATURES) {
                reason = 4;
            } else {
                reason = 1;
            }
        } else {
            state = 0;
        }
        if (state != lastTrackingCode || reason != lastReasonCode) {
            lastTrackingCode = state;
            lastReasonCode = reason;
            ARTrackingState s = state == 2 ? ARTrackingState.TRACKING
                    : state == 1 ? ARTrackingState.LIMITED : ARTrackingState.NOT_TRACKING;
            ARTrackingFailureReason fr = reason == 2 ? ARTrackingFailureReason.EXCESSIVE_MOTION
                    : reason == 3 ? ARTrackingFailureReason.INSUFFICIENT_LIGHT
                    : reason == 4 ? ARTrackingFailureReason.INSUFFICIENT_FEATURES
                    : reason == 1 ? ARTrackingFailureReason.INITIALIZING
                    : ARTrackingFailureReason.NONE;
            sink.onTrackingStateChanged(s, fr);
        }
    }

    private void servicePendingHits(Frame frame, Camera camera) {
        List<PendingHit> hits = null;
        synchronized (sceneLock) {
            if (!pendingHits.isEmpty()) {
                hits = new ArrayList<PendingHit>(pendingHits);
                pendingHits.clear();
            }
        }
        if (hits == null) {
            return;
        }
        int w = view == null ? 1 : Math.max(1, view.getWidth());
        int h = view == null ? 1 : Math.max(1, view.getHeight());
        for (PendingHit pending : hits) {
            List<ARHitResult> out = new ArrayList<ARHitResult>();
            if (camera.getTrackingState() == TrackingState.TRACKING) {
                try {
                    List<HitResult> results = frame.hitTest(pending.xNorm * w, pending.yNorm * h);
                    for (HitResult hit : results) {
                        if (hit.getTrackable() instanceof Plane) {
                            Plane plane = (Plane) hit.getTrackable();
                            if (!plane.isPoseInPolygon(hit.getHitPose())) {
                                continue;
                            }
                            synchronized (sceneLock) {
                                recentHits.add(hit);
                            }
                            String planeId = planeIds.get(plane);
                            out.add(new ARHitResult(fromPose(hit.getHitPose()),
                                    hit.getDistance(), ARHitResult.Type.PLANE,
                                    planeId == null ? null : planeSnapshots.get(planeId),
                                    hit));
                        } else {
                            synchronized (sceneLock) {
                                recentHits.add(hit);
                            }
                            out.add(new ARHitResult(fromPose(hit.getHitPose()),
                                    hit.getDistance(), ARHitResult.Type.FEATURE_POINT,
                                    null, hit));
                        }
                    }
                } catch (Throwable t) {
                    // Deliver what was collected; a torn-down session mid-test
                    // simply yields no hits.
                }
            }
            // Cap the retained native hit handles.
            synchronized (sceneLock) {
                while (recentHits.size() > 32) {
                    recentHits.remove(0);
                }
            }
            completeHits(pending.result, out.toArray(new ARHitResult[out.size()]));
        }
    }

    private void processPlanes(Frame frame) {
        for (Plane plane : frame.getUpdatedTrackables(Plane.class)) {
            if (plane.getSubsumedBy() != null) {
                String id = planeIds.remove(plane);
                if (id != null) {
                    planeSnapshots.remove(id);
                    sink.onPlaneRemoved(id);
                }
                continue;
            }
            if (plane.getTrackingState() == TrackingState.STOPPED) {
                String id = planeIds.remove(plane);
                if (id != null) {
                    planeSnapshots.remove(id);
                    sink.onPlaneRemoved(id);
                }
                continue;
            }
            boolean added = false;
            String id = planeIds.get(plane);
            if (id == null) {
                id = "arcore-plane-" + (++idSeq);
                planeIds.put(plane, id);
                added = true;
            }
            ARPlane.Type type = plane.getType() == Plane.Type.VERTICAL
                    ? ARPlane.Type.VERTICAL
                    : plane.getType() == Plane.Type.HORIZONTAL_DOWNWARD_FACING
                    ? ARPlane.Type.HORIZONTAL_DOWN : ARPlane.Type.HORIZONTAL_UP;
            float[] polygon = null;
            FloatBuffer poly = plane.getPolygon();
            if (poly != null) {
                polygon = new float[poly.limit()];
                poly.rewind();
                poly.get(polygon);
            }
            ARPlane snapshot = new ARPlane(id, type, fromPose(plane.getCenterPose()),
                    plane.getExtentX(), plane.getExtentZ(), polygon,
                    plane.getTrackingState() == TrackingState.TRACKING
                            ? ARTrackingState.TRACKING : ARTrackingState.LIMITED);
            planeSnapshots.put(id, snapshot);
            if (added) {
                sink.onPlaneAdded(snapshot);
            } else {
                sink.onPlaneUpdated(snapshot);
            }
        }
    }

    private void processImages(Frame frame) {
        for (AugmentedImage image : frame.getUpdatedTrackables(AugmentedImage.class)) {
            String id = imageIds.get(image);
            if (image.getTrackingState() == TrackingState.STOPPED) {
                if (id != null) {
                    imageIds.remove(image);
                    synchronized (sceneLock) {
                        imagesById.remove(id);
                    }
                    sink.onAnchorRemoved(id);
                }
                continue;
            }
            if (image.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            if (id == null) {
                id = "arcore-image-" + (++idSeq);
                imageIds.put(image, id);
                synchronized (sceneLock) {
                    imagesById.put(id, image);
                }
                sink.onAnchorAdded(new ARImageAnchor(id, fromPose(image.getCenterPose()),
                        image.getName(), image.getExtentX()));
            } else {
                sink.onAnchorUpdated(id, fromPose(image.getCenterPose()),
                        ARTrackingState.TRACKING);
            }
        }
    }

    private void processFaces(Frame frame) {
        for (AugmentedFace face : frame.getUpdatedTrackables(AugmentedFace.class)) {
            String id = faceIds.get(face);
            if (face.getTrackingState() == TrackingState.STOPPED) {
                if (id != null) {
                    faceIds.remove(face);
                    synchronized (sceneLock) {
                        facesById.remove(id);
                    }
                    sink.onAnchorRemoved(id);
                }
                continue;
            }
            if (face.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            boolean added = false;
            if (id == null) {
                id = "arcore-face-" + (++idSeq);
                faceIds.put(face, id);
                synchronized (sceneLock) {
                    facesById.put(id, face);
                }
                added = true;
                sink.onAnchorAdded(new ARFaceAnchor(id, fromPose(face.getCenterPose())));
            }
            // Region poses: ARCore natively supplies nose + forehead regions.
            ARPose[] regions = new ARPose[ARFaceRegion.values().length];
            regions[ARFaceRegion.NOSE_TIP.ordinal()] =
                    fromPose(face.getRegionPose(AugmentedFace.RegionType.NOSE_TIP));
            regions[ARFaceRegion.FOREHEAD_LEFT.ordinal()] =
                    fromPose(face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT));
            regions[ARFaceRegion.FOREHEAD_RIGHT.ordinal()] =
                    fromPose(face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_RIGHT));
            float[] meshVertices = null;
            int[] meshTriangles = null;
            if (added || frameCounter % 6 == 0) {
                FloatBuffer v = face.getMeshVertices();
                if (v != null) {
                    meshVertices = new float[v.limit()];
                    v.rewind();
                    v.get(meshVertices);
                }
                ShortBuffer tri = face.getMeshTriangleIndices();
                if (tri != null) {
                    meshTriangles = new int[tri.limit()];
                    tri.rewind();
                    for (int i = 0; i < meshTriangles.length; i++) {
                        meshTriangles[i] = tri.get(i);
                    }
                }
            }
            sink.onFaceAnchorUpdated(id, fromPose(face.getCenterPose()),
                    ARTrackingState.TRACKING, regions, meshVertices, meshTriangles);
        }
    }

    private void publishAnchors() {
        if (frameCounter % 6 != 0) {
            return;
        }
        List<Object[]> updates = new ArrayList<Object[]>();
        synchronized (sceneLock) {
            for (Map.Entry<String, Anchor> e : anchors.entrySet()) {
                Anchor anchor = e.getValue();
                ARTrackingState state = anchor.getTrackingState() == TrackingState.TRACKING
                        ? ARTrackingState.TRACKING
                        : anchor.getTrackingState() == TrackingState.PAUSED
                        ? ARTrackingState.LIMITED : ARTrackingState.NOT_TRACKING;
                updates.add(new Object[]{e.getKey(), fromPose(anchor.getPose()), state});
            }
        }
        for (Object[] u : updates) {
            sink.onAnchorUpdated((String) u[0], (ARPose) u[1], (ARTrackingState) u[2]);
        }
    }

    private void publishLight(Frame frame) {
        LightEstimate estimate = frame.getLightEstimate();
        if (estimate != null && estimate.getState() == LightEstimate.State.VALID) {
            // ARCore's pixel intensity averages about 0.18 in neutral indoor
            // lighting; normalize so 1.0 is neutral (the ARLightEstimate
            // convention).
            float intensity = estimate.getPixelIntensity() / 0.18f;
            sink.onLightEstimate(new ARLightEstimate(true, intensity, 1f, 1f, 1f));
            if (view != null) {
                view.setLightIntensity(intensity);
            }
        }
    }
}
