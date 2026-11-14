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
import com.codename1.gpu.ColorLuts;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// The post effects that read the scene's depth, plus the colour lookup: a
/// row of blocks recedes from the camera over a floor. Ambient occlusion
/// darkens where each block meets the floor, the outline inks every
/// silhouette, depth of field keeps the third block sharp and blurs the
/// nearest and the farthest, and a film lookup warms the highlights. A
/// backend that reads the depth upside down puts the contact shadows and
/// lines in the wrong place; one that reads no depth shows none of them.
public class GpuPostDepthScreenshotTest extends BaseTest {
    private RenderView view;

    private static float[] mul(float[] a, float[] b) {
        float[] out = new float[16];
        Matrix4.multiply(a, b, out);
        return out;
    }

    @Override
    public boolean runTest() {
        Form form = createForm("Depth post effects", new BorderLayout(), "GpuPostDepth");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private final PostProcess post = new PostProcess()
                    .setAmbientOcclusion(0.9f, 14f)
                    .setDepthOfField(7.5f, 6f)
                    .setOutline(0.85f, 0xff1a1410);
            private Mesh cube;
            private Mesh floor;
            private final Material[] blocks = new Material[5];
            private Material ground;
            private Texture lut;
            private int width = 1;
            private int height = 1;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 1f);
                floor = Primitives.quad(device, 1f);
                int[] colors = {0xffd9643a, 0xff3a8fd9, 0xffe0c050, 0xff58b06a, 0xffb05ab0};
                for (int i = 0; i < blocks.length; i++) {
                    blocks[i] = new Material(Material.Type.PHONG).setColor(colors[i]);
                }
                ground = new Material(Material.Type.LAMBERT).setColor(0xffc8c0b4);
                lut = device.createTexture(ColorLuts.WIDTH, ColorLuts.HEIGHT, ColorLuts.filmic(0.8f, 0.08f));
                post.setColorLut(lut, 1f);
                device.setLight(new Light().setDirection(-0.5f, -1f, -0.35f));
                camera.setPerspective(45f, 0.3f, 60f).setPosition(2.2f, 1.6f, 5.5f).setTarget(-0.4f, 0f, -3f);
            }

            public void onResize(GraphicsDevice device, int w, int h) {
                width = Math.max(1, w);
                height = Math.max(1, h);
                camera.setAspect((float) width / height);
                device.setViewport(0, 0, w, h);
            }

            public void onFrame(GraphicsDevice device) {
                device.setCamera(camera);
                boolean postOn = device.beginPostProcess(post, width, height);
                device.clear(0xff8fa6b8, true, true);
                device.draw(floor, ground, mul(Matrix4.translation(0f, -0.5f, -4f),
                        mul(Matrix4.rotation((float) Math.toRadians(-90), 1f, 0f, 0f),
                                Matrix4.scaling(30f, 30f, 1f))));
                for (int i = 0; i < blocks.length; i++) {
                    float z = 2f - i * 3f;
                    float x = -0.4f * i;
                    float hgt = 1f + 0.35f * i;
                    device.draw(cube, blocks[i], mul(Matrix4.translation(x, -0.5f + hgt * 0.5f, z),
                            mul(Matrix4.rotation((float) Math.toRadians(20 * i), 0f, 1f, 0f),
                                    Matrix4.scaling(1f, hgt, 1f))));
                }
                if (postOn) {
                    device.endPostProcess(1f);
                }
            }

            public void onDispose(GraphicsDevice device) {
                if (lut != null) {
                    device.dispose(lut);
                    lut = null;
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
    protected void registerReadyCallback(Form parent, final Runnable run) {
        UITimer.timer(1200, false, parent, new Runnable() {
            public void run() {
                if (view != null) {
                    view.requestRender();
                }
                UITimer.timer(600, false, parent, run);
            }
        });
    }
}
