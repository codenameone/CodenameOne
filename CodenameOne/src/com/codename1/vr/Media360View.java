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
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.Quaternion;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;

/// A 360 degree panorama viewer: renders an equirectangular image onto the
/// inside of a sphere with drag-to-look navigation and optional gyroscope
/// look. Works everywhere `Display#isGpuSupported()` is true; on platforms
/// without a GPU backend it degrades to the standard "3D not supported"
/// placeholder without crashing.
///
/// Mono mode fills the component; `#setStereo(boolean)` renders the same
/// viewpoint side by side for cardboard-style viewers. Photo spheres are
/// captured from a single point, so stereo intentionally uses a zero eye
/// separation - there is no parallax information in the image to reproduce.
///
/// 360 video is not supported directly because the platform has no path from
/// media frames to GPU textures yet; `#setTextureSource(TextureSource)` is
/// the extension point for dynamic content.
///
/// ```java
/// Media360View pano = new Media360View();
/// pano.setImage(EncodedImage.create("/panorama.jpg"));
/// form.add(BorderLayout.CENTER, pano);
/// ```
public class Media360View extends Container {
    private static final float MAX_PITCH_DEGREES = 89f;

    private final RenderView renderView;
    // Guards the state the EDT writes and the render thread reads each frame
    // (the codebase convention is locking over volatile fields).
    private final Object stateLock = new Object();
    private Image pendingImage;
    private boolean imageDirty;
    private TextureSource textureSource;
    private boolean sourceDirty;
    private float yaw;
    private float pitch;
    private boolean stereo;
    private boolean headTrackingEnabled;
    private HeadTracker tracker;

    private int lastDragX = -1;
    private int lastDragY = -1;

    /// Creates an empty viewer; supply content with `#setImage(Image)` or
    /// `#setTextureSource(TextureSource)`.
    public Media360View() {
        super(new BorderLayout());
        renderView = new RenderView(new SphereLoop());
        add(BorderLayout.CENTER, renderView);
        setFocusable(true);
    }

    /// True when the current platform provides a 3D backend.
    public boolean isSupported() {
        return Display.getInstance().isGpuSupported();
    }

    /// Shows an equirectangular panorama image (the format produced by 360
    /// cameras and phone panorama modes: longitude maps to X, latitude to Y).
    /// Replaces any previous image or texture source.
    ///
    /// #### Parameters
    ///
    /// - `image`: the panorama to display
    public void setImage(Image image) {
        synchronized (stateLock) {
            pendingImage = image;
            imageDirty = true;
        }
        renderView.requestRender();
    }

    /// Installs a dynamic texture source, replacing any static image. The
    /// extension point for procedural or (future) video content.
    ///
    /// #### Parameters
    ///
    /// - `source`: the source, or null to remove it
    public void setTextureSource(TextureSource source) {
        synchronized (stateLock) {
            textureSource = source;
            sourceDirty = true;
        }
        renderView.requestRender();
    }

    /// Switches between a full-viewport mono view (the default) and
    /// side-by-side stereo for cardboard-style viewers.
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

    /// Enables gyroscope look-around, composed with drag navigation. When the
    /// device has no motion sensors this quietly stays drag-only.
    public void setHeadTrackingEnabled(boolean enabled) {
        synchronized (stateLock) {
            headTrackingEnabled = enabled;
        }
        if (enabled) {
            if (tracker == null) {
                tracker = new HeadTracker();
            }
            if (isInitialized()) {
                tracker.start();
            }
            renderView.setContinuous(true);
        } else {
            if (tracker != null) {
                tracker.stop();
            }
            renderView.setContinuous(false);
        }
        renderView.requestRender();
    }

    /// True when gyroscope look-around is enabled.
    public boolean isHeadTrackingEnabled() {
        synchronized (stateLock) {
            return headTrackingEnabled;
        }
    }

    /// The horizontal look angle in degrees; positive turns right.
    public float getYaw() {
        synchronized (stateLock) {
            return yaw;
        }
    }

    /// Sets the horizontal look angle in degrees.
    public void setYaw(float yawDegrees) {
        synchronized (stateLock) {
            this.yaw = yawDegrees;
        }
        renderView.requestRender();
    }

    /// The vertical look angle in degrees, clamped so the view cannot flip
    /// over the poles; positive looks up.
    public float getPitch() {
        synchronized (stateLock) {
            return pitch;
        }
    }

    /// Sets the vertical look angle in degrees. Clamped to stay off the
    /// poles.
    public void setPitch(float pitchDegrees) {
        synchronized (stateLock) {
            this.pitch = clampPitch(pitchDegrees);
        }
        renderView.requestRender();
    }

    /// Resets the view to look straight ahead and recenters the gyroscope.
    public void reset() {
        synchronized (stateLock) {
            yaw = 0f;
            pitch = 0f;
        }
        if (tracker != null) {
            tracker.recenter();
        }
        renderView.requestRender();
    }

    /// The underlying render view, exposed for advanced integration.
    public RenderView getRenderView() {
        return renderView;
    }

    private static float clampPitch(float p) {
        if (p > MAX_PITCH_DEGREES) {
            return MAX_PITCH_DEGREES;
        }
        if (p < -MAX_PITCH_DEGREES) {
            return -MAX_PITCH_DEGREES;
        }
        return p;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (isHeadTrackingEnabled() && tracker != null) {
            tracker.start();
        }
    }

    @Override
    protected void deinitialize() {
        if (tracker != null) {
            tracker.stop();
        }
        super.deinitialize();
    }

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        lastDragX = x;
        lastDragY = y;
    }

    @Override
    public void pointerDragged(int x, int y) {
        super.pointerDragged(x, y);
        if (lastDragX >= 0) {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            // Dragging the image right turns the view left, matching the
            // grab-the-world gesture users expect from panorama viewers.
            synchronized (stateLock) {
                yaw -= (x - lastDragX) * 180f / w;
                pitch = clampPitch(pitch + (y - lastDragY) * 180f / h);
            }
        }
        lastDragX = x;
        lastDragY = y;
        renderView.requestRender();
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        lastDragX = -1;
        lastDragY = -1;
    }

    /// The internal renderer: an inside-out UV sphere carrying the panorama
    /// texture, looked at from the origin.
    private final class SphereLoop implements Renderer {
        private Mesh sphere;
        private Material material;
        private Texture texture;
        private final Camera camera = new Camera();
        private final float[] quat = Quaternion.identity();
        private final float[] dir = new float[3];
        private final float[] up = new float[3];
        private int surfaceWidth = 1;
        private int surfaceHeight = 1;

        @Override
        public void onInit(GraphicsDevice device) {
            sphere = Primitives.sphere(device, 50f, 48, 96, true);
            material = new Material(Material.Type.UNLIT).setColor(0xffffffff);
        }

        @Override
        public void onResize(GraphicsDevice device, int width, int height) {
            surfaceWidth = Math.max(1, width);
            surfaceHeight = Math.max(1, height);
        }

        @Override
        public void onFrame(GraphicsDevice device) {
            // Snapshot the EDT-owned state once per frame.
            TextureSource sourceNow;
            boolean sourceDirtyNow;
            Image imageNow;
            boolean imageDirtyNow;
            float yawNow;
            float pitchNow;
            boolean stereoNow;
            boolean trackingNow;
            synchronized (stateLock) {
                sourceNow = textureSource;
                sourceDirtyNow = sourceDirty;
                sourceDirty = false;
                imageNow = pendingImage;
                imageDirtyNow = imageDirty;
                imageDirty = false;
                yawNow = yaw;
                pitchNow = pitch;
                stereoNow = stereo;
                trackingNow = headTrackingEnabled;
            }
            refreshTexture(device, sourceNow, sourceDirtyNow, imageNow, imageDirtyNow);
            computeLook(yawNow, pitchNow, trackingNow);
            int w = surfaceWidth;
            int h = surfaceHeight;
            device.clear(0xff000000, true, true);
            if (stereoNow) {
                int half = w / 2;
                drawSphere(device, 0, 0, half, h);
                drawSphere(device, half, 0, w - half, h);
                device.setViewport(0, 0, w, h);
            } else {
                drawSphere(device, 0, 0, w, h);
            }
        }

        private void refreshTexture(GraphicsDevice device, TextureSource source,
                                    boolean sourceChanged, Image img, boolean imageChanged) {
            if (sourceChanged) {
                if (texture != null) {
                    device.dispose(texture);
                    texture = null;
                }
            }
            if (source != null) {
                if (texture == null) {
                    texture = source.createTexture(device);
                    if (texture != null) {
                        texture.setFilter(Texture.Filter.LINEAR);
                    }
                }
                if (texture != null) {
                    source.updateTexture(device, texture);
                }
            } else if (imageChanged) {
                if (texture != null) {
                    device.dispose(texture);
                    texture = null;
                }
                if (img != null) {
                    texture = device.createTexture(img);
                    texture.setFilter(Texture.Filter.LINEAR);
                }
            }
            material.setTexture(texture);
        }

        private void computeLook(float yawDegrees, float pitchDegrees, boolean tracking) {
            float yawRad = (float) Math.toRadians(yawDegrees);
            float pitchRad = (float) Math.toRadians(pitchDegrees);
            if (tracking && tracker != null) {
                // The drag yaw acts as a manual offset on top of the gyro
                // orientation; drag pitch is ignored while the gyro owns it.
                tracker.getOrientation(quat);
                float[] yawQ = Quaternion.fromAxisAngle(-yawRad, 0f, 1f, 0f);
                Quaternion.multiply(yawQ, quat, quat);
                dir[0] = 0f;
                dir[1] = 0f;
                dir[2] = -1f;
                Quaternion.rotateVector(quat, dir);
                up[0] = 0f;
                up[1] = 1f;
                up[2] = 0f;
                Quaternion.rotateVector(quat, up);
            } else {
                float cp = (float) Math.cos(pitchRad);
                dir[0] = cp * (float) Math.sin(yawRad);
                dir[1] = (float) Math.sin(pitchRad);
                dir[2] = -cp * (float) Math.cos(yawRad);
                up[0] = 0f;
                up[1] = 1f;
                up[2] = 0f;
            }
            camera.setPosition(0f, 0f, 0f);
            camera.setTarget(dir[0], dir[1], dir[2]);
            camera.setUp(up[0], up[1], up[2]);
            camera.setPerspective(75f, 0.1f, 100f);
        }

        private void drawSphere(GraphicsDevice device, int x, int y, int w, int h) {
            device.setViewport(x, y, w, h);
            camera.setAspect((float) w / (float) Math.max(1, h));
            device.setCamera(camera);
            device.draw(sphere, material, null);
        }

        @Override
        public void onDispose(GraphicsDevice device) {
            TextureSource source = textureSource;
            if (source != null && device != null) {
                source.dispose(device);
            }
            if (texture != null && device != null) {
                device.dispose(texture);
            }
            texture = null;
            sphere = null;
        }
    }
}
