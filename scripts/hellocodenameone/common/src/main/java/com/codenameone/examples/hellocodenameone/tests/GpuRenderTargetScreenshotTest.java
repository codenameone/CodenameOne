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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Render to texture: a small scene is drawn from a second camera into an
/// offscreen target, which then shows on two screens in the main view, a
/// monitor facing the camera and a tilted panel on the floor. The scene in
/// the target is lopsided on purpose (a tall red block top left, a small blue
/// one bottom right), so a backend that hands the texture back upside down
/// or mirrored shows a different picture.
public class GpuRenderTargetScreenshotTest extends BaseTest {
    // frames the renderer has started: the first compiles every program and
    // can outlast a fixed timer on a slow emulator, leaving a blank capture
    private final java.util.concurrent.atomic.AtomicInteger frames =
            new java.util.concurrent.atomic.AtomicInteger();
    private RenderView view;

    private static float[] mul(float[] a, float[] b) {
        float[] out = new float[16];
        Matrix4.multiply(a, b, out);
        return out;
    }

    @Override
    public boolean runTest() {
        Form form = createForm("Render target", new BorderLayout(), "GpuRenderTarget");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private final Camera monitorCamera = new Camera();
            private Mesh cube;
            private Mesh screen;
            private Mesh floor;
            private Material red;
            private Material blue;
            private Material ground;
            private Material display;
            private Texture target;
            private boolean unsupported;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1f);
                screen = Primitives.quad(device, 1f);
                floor = Primitives.quad(device, 1f);
                red = new Material(Material.Type.PHONG).setColor(0xffd2402e);
                blue = new Material(Material.Type.PHONG).setColor(0xff2e64d2);
                ground = new Material(Material.Type.LAMBERT).setColor(0xff6c7480);
                target = device.createRenderTarget(256, 256);
                unsupported = target == null;
                display = new Material(Material.Type.UNLIT).setColor(0xffffffff).setTexture(target);
                device.setLight(new Light().setDirection(-0.4f, -1f, -0.6f));
                monitorCamera.setPerspective(45f, 0.1f, 50f).setAspect(1f)
                        .setPosition(0f, 0f, 6f).setTarget(0f, 0f, 0f);
                camera.setPerspective(50f, 0.1f, 100f).setPosition(0f, 1.2f, 5.5f).setTarget(0f, 0.4f, 0f);
            }

            public void onResize(GraphicsDevice device, int w, int h) {
                camera.setAspect((float) Math.max(1, w) / Math.max(1, h));
                device.setViewport(0, 0, w, h);
            }

            public void onFrame(GraphicsDevice device) {
                frames.incrementAndGet();
                if (target != null && device.beginRenderTarget(target, 0xff1d2733)) {
                    device.setCamera(monitorCamera);
                    device.draw(cube, red, mul(Matrix4.translation(-1.1f, 0.6f, 0f),
                            Matrix4.scaling(0.8f, 2.2f, 0.8f)));
                    device.draw(cube, blue, mul(Matrix4.translation(1.2f, -1.2f, 0f),
                            Matrix4.scaling(0.6f, 0.6f, 0.6f)));
                    device.endRenderTarget();
                }
                device.setCamera(camera);
                device.clear(0xff10141a, true, true);
                device.draw(floor, ground, mul(Matrix4.translation(0f, -0.6f, 0f),
                        mul(Matrix4.rotation((float) Math.toRadians(-90), 1f, 0f, 0f),
                                Matrix4.scaling(12f, 12f, 1f))));
                if (unsupported) {
                    device.draw(cube, red, null);
                    return;
                }
                device.draw(screen, display, mul(Matrix4.translation(-0.9f, 0.9f, 0f),
                        Matrix4.scaling(2.2f, 2.2f, 1f)));
                device.draw(screen, display, mul(Matrix4.translation(1.5f, -0.2f, 1.2f),
                        mul(Matrix4.rotation((float) Math.toRadians(-55), 1f, 0f, 0f),
                                Matrix4.scaling(1.4f, 1.4f, 1f))));
            }

            public void onDispose(GraphicsDevice device) {
                if (target != null) {
                    device.dispose(target);
                    target = null;
                }
            }
        });
        if (!view.isSupported()) {
            done();
            return true;
        }
        form.add(BorderLayout.CENTER, view);
        form.show();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return com.codename1.ui.CN.isGpuSupported();
    }

    @Override
    protected void registerReadyCallback(final Form parent, final Runnable run) {
        UITimer.timer(1200, false, parent, new Runnable() {
            int polls;

            public void run() {
                // a third frame has started once two are complete; give up
                // after ~20s so a broken device still reports
                if (frames.get() >= 3 || polls++ > 90) {
                    UITimer.timer(400, false, parent, run);
                    return;
                }
                if (view != null) {
                    view.requestRender();
                }
                UITimer.timer(200, false, parent, this);
            }
        });
    }
}
