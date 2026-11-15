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
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// End-to-end screenshot test for the portable 3D API (com.codename1.gpu). It
/// hosts a {@link RenderView} in a normal form and renders a Phong-lit cube at a
/// fixed orientation so the capture is deterministic. On platforms without a 3D
/// backend the view shows its placeholder, which still screenshots cleanly.
public class Gpu3DCubeScreenshotTest extends BaseTest {
    // cn1ss-skip: macos, watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    private RenderView view;

    @Override
    public boolean runTest() {
        Form form = createForm("3D Cube", new BorderLayout(), "Gpu3DCube");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh cube;
            private Material material;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1.6f);
                material = new Material(Material.Type.PHONG)
                        .setColor(0xff3366ff)
                        .setShininess(24f);
                camera.setPerspective(45f, 0.1f, 100f)
                        .setPosition(2.6f, 2.1f, 3.4f)
                        .setTarget(0f, 0f, 0f);
                device.setLight(new Light().setDirection(-0.4f, -1f, -0.55f));
            }

            public void onResize(GraphicsDevice device, int width, int height) {
                camera.setAspect((float) width / Math.max(1, height));
                device.setViewport(0, 0, width, height);
            }

            public void onFrame(GraphicsDevice device) {
                device.clear(0xff101018, true, true);
                device.setCamera(camera);
                float[] model = Matrix4.rotation((float) Math.toRadians(25), 0.35f, 1f, 0.12f);
                device.draw(cube, material, model);
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        if (!view.isSupported()) {
            // No GPU 3D backend on this platform (e.g. the iOS GL build); skip the
            // screenshot (shouldTakeScreenshot() returns false) rather than gate a
            // "3D unsupported" placeholder that has no per-platform baseline.
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

    /// Force a fresh GPU frame to be rendered (and read back for capture) before
    /// the screenshot fires, so a cold GL surface that has not drawn yet cannot
    /// produce a blank capture.
    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        UITimer.timer(1000, false, parent, new Runnable() {
            public void run() {
                if (view != null) {
                    view.requestRender();
                }
                UITimer.timer(500, false, parent, run);
            }
        });
    }
}
