/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codenameone.examples.brickyard.rbx;

import com.codename1.gaming.AnimatedModel;
import com.codename1.gpu.Camera;
import com.codename1.gpu.Environment;
import com.codename1.gpu.GltfScene;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;

/// A character body on a turntable, lit as the game lights it: what an
/// avatar editor shows, so the player picks the figure they will play as
/// rather than a drawing of it.
public final class AvatarPreview extends RenderView {
    private final Stage stage;

    /// A preview of `body`, one of [#bodies()]; an unknown name shows the first.
    public AvatarPreview(String body) {
        this(new Stage(body));
    }

    private AvatarPreview(Stage stage) {
        super(stage);
        this.stage = stage;
        setContinuous(true);
    }

    /// The body names a character can wear, as its `AvatarBody` attribute.
    public static String[] bodies() {
        String[] out = new String[Avatars.BODIES.length];
        System.arraycopy(Avatars.BODIES, 0, out, 0, out.length);
        return out;
    }

    /// Shows `body` instead, with a cheer so the change reads as a choice.
    public void setBody(String body) {
        stage.next = body;
    }

    /// The renderer: one animated model, a key light, a sky and a floor shadow.
    private static final class Stage implements Renderer {
        private final Camera camera = new Camera();
        private final Light light = new Light();
        private final Environment env = new Environment();
        private final float[] scratchA = new float[16];
        private final float[] scratchB = new float[16];
        private final float[] matrix = new float[16];
        private String shown = "";
        private String next;
        private final float[] plinth = new float[16];
        private AnimatedModel model;
        private Mesh disc;
        private Mesh dome;
        private Material skyMaterial;
        private Material stone;
        private float scale = 1;
        private float lift;
        private float yaw = -25;
        private long last;

        Stage(String body) {
            next = body;
        }

        @Override
        public void onInit(GraphicsDevice device) {
            // A warm key from the upper left over a cool fill, and a sky that
            // falls from deep blue to violet: the figure reads as a solid
            // standing in a lit place, not a cut-out on a flat fill.
            light.setDirection(-0.5f, -0.75f, -0.45f);
            light.setColor(0xffffe6c8);
            light.setAmbientColor(0xff252a44);
            env.setHemisphere(0xff7f9cff, 0xff2a2238);
            env.setSky(0xff141a3a, 0xff5b4a9a, 0xffffd9a0, 0);
            env.setToneMapping(true);
            env.setExposure(1.15f);
            dome = Primitives.sphere(device, 60f, 16, 24, true);
            skyMaterial = new Material(Material.Type.SKYBOX);
            skyMaterial.setCastShadows(false);
            disc = Primitives.sphere(device, 1f, 12, 40, false);
            stone = new Material(Material.Type.PBR).setColor(0xff3b4266).setRoughness(0.55f).setMetallic(0.1f);
            Matrix4.multiply(Matrix4.translation(0, -0.18f, 0), Matrix4.scaling(2.6f, 0.2f, 2.6f), plinth);
            camera.setPerspective(30, 0.5f, 100);
            camera.setPosition(0, 3.4f, 12.5f);
            camera.setTarget(0, 2.5f, 0);
        }

        @Override
        public void onResize(GraphicsDevice device, int w, int h) {
            camera.setAspect(h <= 0 ? 1 : (float) w / h);
            device.setViewport(0, 0, w, h);
        }

        @Override
        public void onFrame(GraphicsDevice device) {
            long now = System.currentTimeMillis();
            float dt = last == 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
            last = now;
            String want = next;
            if (want != null && !want.equals(shown)) {
                load(device, want);
            }
            device.clear(0xff1b2140, true, true);
            device.setCamera(camera);
            device.setLight(light);
            device.setEnvironment(env);
            if (dome != null) {
                device.draw(dome, skyMaterial, null);
                device.draw(disc, stone, plinth);
            }
            if (model == null) {
                return;
            }
            if (model.isFinished()) {
                model.crossFade("Idle", 0.25f, true);
            }
            model.update(dt);
            yaw += dt * 18;
            Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(yaw), 0f, 1f, 0f),
                    Matrix4.scaling(scale, scale, scale), scratchA);
            Matrix4.multiply(Matrix4.translation(0, lift, 0), scratchA, scratchB);
            System.arraycopy(scratchB, 0, matrix, 0, 16);
            com.codename1.gaming.Model[] parts = model.getParts();
            for (com.codename1.gaming.Model part : parts) {
                if (part.isVisible()) {
                    device.draw(part.getMesh(), part.getMaterial(), matrix);
                }
            }
        }

        private void load(GraphicsDevice device, String body) {
            boolean first = shown.length() == 0;
            shown = body;
            GltfScene s = ModelMeshes.scene("avatar_" + body + ".glb");
            if (s == null) {
                s = ModelMeshes.scene("avatar_" + Avatars.BODIES[0] + ".glb");
            }
            if (s == null) {
                model = null;
                return;
            }
            AnimatedModel m = new AnimatedModel(device, s);
            m.setMaterialType(Material.Type.PBR);
            float[] b = m.getBounds();
            float h = b == null ? 0 : b[4] - b[1];
            scale = h <= 0 ? 1 : Avatars.HEIGHT / h;
            lift = b == null ? 0 : -b[1] * scale;
            if (first) {
                m.play("Idle", true);
            } else {
                m.play("Cheer", false);
            }
            model = m;
        }

        @Override
        public void onDispose(GraphicsDevice device) {
            model = null;
            disc = null;
            dome = null;
            shown = "";
        }
    }
}
