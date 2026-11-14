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
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shaders.Fx;
import com.codename1.gpu.shaders.Liquid;
import com.codename1.gpu.shaders.ScenePresets;
import com.codename1.gpu.shaders.Surface;
import com.codename1.gpu.shaders.Toon;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Twelve library shaders side by side on spheres under the noon preset:
/// the surfaces in the top rows, the stylized and animated effects below.
/// The shader clock is pinned so the animated ones hold still.
public class GpuShaderGalleryScreenshotTest extends BaseTest {
    private RenderView view;
    // frames the renderer has finished: the first one compiles twelve
    // programs, which on an emulator outlasts any fixed wait
    private final java.util.concurrent.atomic.AtomicInteger frames =
            new java.util.concurrent.atomic.AtomicInteger();

    @Override
    public boolean runTest() {
        Form form = createForm("Shader gallery", new BorderLayout(), "GpuShaderGallery");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private final ScenePresets.Preset look = ScenePresets.clearNoon();
            private Mesh ball;
            private Material[] mats;

            public void onInit(GraphicsDevice device) {
                ball = Primitives.sphere(device, 0.45f, 24, 32, false);
                ShaderMaterial[] shaders = {
                    Surface.paintedMetal(), Surface.rust(), Surface.wood(), Surface.concrete(),
                    Surface.glass(), Surface.ice(), Surface.fabric(), Surface.carPaint(),
                    Toon.cel(), Fx.hologram(), Liquid.lava(), Fx.dissolve()
                };
                mats = new Material[shaders.length];
                for (int i = 0; i < shaders.length; i++) {
                    Material.Type type = i == 8 ? Material.Type.UNLIT : Material.Type.PBR;
                    mats[i] = new Material(type).setShader(shaders[i]);
                }
                camera.setPerspective(40f, 0.1f, 100f).setPosition(0f, 0f, 6.2f).setTarget(0f, 0f, 0f);
                look.apply(device);
            }

            public void onResize(GraphicsDevice device, int w, int h) {
                camera.setAspect((float) Math.max(1, w) / Math.max(1, h));
                device.setViewport(0, 0, w, h);
            }

            public void onFrame(GraphicsDevice device) {
                device.setTime(1.5f);
                device.setCamera(camera);
                device.clear(look.getClearColor(), true, true);
                for (int i = 0; i < mats.length; i++) {
                    float x = (i % 4 - 1.5f) * 1.1f;
                    float y = (1 - i / 4) * 1.1f;
                    device.draw(ball, mats[i], Matrix4.translation(x, y, 0f));
                }
                frames.incrementAndGet();
            }

            public void onDispose(GraphicsDevice device) {
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
                // capture only once a frame has been drawn after the slow
                // first one, or after 20s so a broken device still reports
                if (frames.get() >= 2 || polls++ > 90) {
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
