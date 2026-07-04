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
package com.codename1.vr;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Quaternion;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;

/// Component that renders an application scene in virtual reality: stereo
/// side-by-side per-eye views with sensor-fusion head tracking, built
/// entirely on the portable `com.codename1.gpu` pipeline so it works on every
/// platform where `Display#isGpuSupported()` is true - including the
/// simulator.
///
/// Supply a `VRRenderer` that draws the scene; the view clears the frame,
/// splits the viewport per eye, positions a `VRCameraRig` from the
/// `HeadTracker` orientation and invokes the renderer once per eye.
///
/// IMPORTANT: `VRRenderer` callbacks run on the platform render thread, never
/// on the EDT. Do not touch Codename One UI components from them.
///
/// ```java
/// VRView vr = new VRView(new VRRenderer() {
///     Mesh cube;
///     Material material;
///     public void onInit(GraphicsDevice device) {
///         cube = Primitives.cube(device, 0.5f);
///         material = new Material(Material.Type.PHONG).setColor(0xff3366ff);
///     }
///     public void onEyeFrame(GraphicsDevice device, VREye eye, Camera camera) {
///         device.draw(cube, material, Matrix4.translation(0, 0, -2));
///     }
///     public void onDispose(GraphicsDevice device) { }
/// });
/// vr.setContinuous(true);
/// form.add(BorderLayout.CENTER, vr);
/// ```
public class VRView extends Container {
    private final VRRenderer vrRenderer;
    private final VRSettings settings;
    private final HeadTracker tracker;
    private final VRCameraRig rig;
    private final RenderView renderView;
    private final Camera eyeCamera = new Camera();
    private final float[] quat = Quaternion.identity();
    // Guards the settings the EDT writes and the render thread reads each
    // frame (the codebase convention is locking over volatile fields).
    private final Object stateLock = new Object();
    private boolean stereo;
    private boolean headTrackingEnabled = true;
    private int clearColor = 0xff000000;
    private float posX;
    private float posY;
    private float posZ;
    // Written and read on the render thread only.
    private int surfaceWidth = 1;
    private int surfaceHeight = 1;

    /// Creates a VR view with default settings.
    ///
    /// #### Parameters
    ///
    /// - `renderer`: draws the scene, once per eye per frame
    public VRView(VRRenderer renderer) {
        this(renderer, new VRSettings());
    }

    /// Creates a VR view.
    ///
    /// #### Parameters
    ///
    /// - `renderer`: draws the scene, once per eye per frame
    ///
    /// - `settings`: eye separation and lens parameters; null uses defaults
    public VRView(VRRenderer renderer, VRSettings settings) {
        super(new BorderLayout());
        if (renderer == null) {
            throw new IllegalArgumentException("renderer is required");
        }
        this.vrRenderer = renderer;
        this.settings = settings == null ? new VRSettings() : settings;
        this.stereo = this.settings.isStereo();
        this.tracker = new HeadTracker();
        this.rig = new VRCameraRig(this.settings);
        this.renderView = new RenderView(new EyeLoop());
        add(BorderLayout.CENTER, renderView);
    }

    /// True when the current platform provides a 3D backend. Equivalent to
    /// `Display#isGpuSupported()`.
    public boolean isSupported() {
        return Display.getInstance().isGpuSupported();
    }

    /// The settings this view was created with.
    public VRSettings getSettings() {
        return settings;
    }

    /// The head tracker driving this view. Exposed to tune the fusion filter
    /// or observe the raw orientation.
    public HeadTracker getHeadTracker() {
        return tracker;
    }

    /// Enables or disables head tracking. When disabled the last orientation
    /// freezes; combine with `VRCameraRig` manually for custom control.
    public void setHeadTrackingEnabled(boolean enabled) {
        synchronized (stateLock) {
            this.headTrackingEnabled = enabled;
        }
        if (isInitialized()) {
            if (enabled) {
                tracker.start();
            } else {
                tracker.stop();
            }
        }
        renderView.requestRender();
    }

    /// True when head tracking drives the view orientation.
    public boolean isHeadTrackingEnabled() {
        synchronized (stateLock) {
            return headTrackingEnabled;
        }
    }

    /// Rotates the view so the current direction becomes "straight ahead".
    public void recenter() {
        tracker.recenter();
        renderView.requestRender();
    }

    /// Switches between side-by-side stereo and a single centered viewpoint.
    public void setStereo(boolean stereo) {
        synchronized (stateLock) {
            this.stereo = stereo;
        }
        renderView.requestRender();
    }

    /// True when rendering side-by-side stereo.
    public boolean isStereo() {
        synchronized (stateLock) {
            return stereo;
        }
    }

    /// Sets the head center position in world space, for example to move the
    /// viewer through the scene.
    public void setPosition(float x, float y, float z) {
        synchronized (stateLock) {
            posX = x;
            posY = y;
            posZ = z;
        }
        renderView.requestRender();
    }

    /// The background clear color as `0xAARRGGBB`. Default opaque black.
    public void setClearColor(int argb) {
        synchronized (stateLock) {
            this.clearColor = argb;
        }
        renderView.requestRender();
    }

    /// Controls whether the view renders continuously or only when
    /// `#requestRender()` is called. Head tracked scenes normally want
    /// continuous rendering.
    public VRView setContinuous(boolean continuous) {
        renderView.setContinuous(continuous);
        return this;
    }

    /// Requests that a single frame be rendered.
    public void requestRender() {
        renderView.requestRender();
    }

    /// The underlying render view, exposed for advanced integration.
    public RenderView getRenderView() {
        return renderView;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (isHeadTrackingEnabled()) {
            tracker.start();
        }
    }

    @Override
    protected void deinitialize() {
        tracker.stop();
        super.deinitialize();
    }

    /// The internal `Renderer` that splits each frame into per-eye passes.
    private final class EyeLoop implements Renderer {
        @Override
        public void onInit(GraphicsDevice device) {
            vrRenderer.onInit(device);
        }

        @Override
        public void onResize(GraphicsDevice device, int width, int height) {
            surfaceWidth = Math.max(1, width);
            surfaceHeight = Math.max(1, height);
        }

        @Override
        public void onFrame(GraphicsDevice device) {
            boolean trackingNow;
            boolean stereoNow;
            int clearNow;
            float px;
            float py;
            float pz;
            synchronized (stateLock) {
                trackingNow = headTrackingEnabled;
                stereoNow = stereo;
                clearNow = clearColor;
                px = posX;
                py = posY;
                pz = posZ;
            }
            if (trackingNow) {
                tracker.getOrientation(quat);
                rig.setOrientation(quat);
            }
            rig.setPosition(px, py, pz);
            int w = surfaceWidth;
            int h = surfaceHeight;
            device.clear(clearNow, true, true);
            if (stereoNow) {
                int half = w / 2;
                renderEye(device, VREye.LEFT, 0, 0, half, h);
                renderEye(device, VREye.RIGHT, half, 0, w - half, h);
                device.setViewport(0, 0, w, h);
            } else {
                renderEye(device, VREye.CENTER, 0, 0, w, h);
            }
        }

        private void renderEye(GraphicsDevice device, VREye eye,
                               int x, int y, int w, int h) {
            device.setViewport(x, y, w, h);
            rig.apply(eyeCamera, eye, (float) w / (float) Math.max(1, h));
            device.setCamera(eyeCamera);
            vrRenderer.onEyeFrame(device, eye, eyeCamera);
        }

        @Override
        public void onDispose(GraphicsDevice device) {
            vrRenderer.onDispose(device);
        }
    }
}
