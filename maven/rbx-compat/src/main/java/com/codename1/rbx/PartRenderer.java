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
package com.codename1.rbx;

import com.codename1.gpu.Camera;
import com.codename1.gpu.Environment;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shaders.Fx;
import com.codename1.gpu.shaders.Liquid;
import com.codename1.gpu.shaders.Surface;
import com.codename1.gpu.shaders.Terrain;
import com.codename1.gpu.shaders.Water;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/// Draws a place on the GPU: every visible part, batched by shape and
/// material into one instanced draw each, under lighting read from the
/// `Lighting` service and its post effects, with particles for emitters,
/// explosions and `Emit` calls.
///
/// The engine runs on the EDT and the device may call back elsewhere, so the
/// game loop hands each frame over as a [SceneSnapshot] through [#submit];
/// the renderer never reads the instance tree itself.
public final class PartRenderer implements Renderer, Engine.EffectSink {
    private static final int MAX_PARTICLES = 2048;

    private final AtomicReference<SceneSnapshot> next = new AtomicReference<SceneSnapshot>();
    private final AtomicReference<float[]> bursts = new AtomicReference<float[]>(new float[0]);
    private SceneSnapshot scene = new SceneSnapshot();
    private final Camera camera = new Camera();
    private final Light light = new Light();
    private final Environment env = new Environment();
    private final PostProcess post = new PostProcess();
    private final Mesh[] meshes = new Mesh[5];
    private final Map<String, Material> materials = new HashMap<String, Material>();
    private Mesh particleMesh;
    private Material sparks;
    private final float[] particles = new float[MAX_PARTICLES * 8]; // NOPMD SingularField - particles live across frames
    private int particleCount;
    private int width = 1;
    private int height = 1;
    private float time;

    /// Hands the next frame over. Called on the EDT after each engine step.
    public void submit(SceneSnapshot s) {
        next.set(s);
    }

    @Override
    public void effect(Instance source, String kind, Object arg) {
        if (!"Emit".equals(kind) && !"Explode".equals(kind)) {
            return;
        }
        Vector3 at = Vector3.ZERO;
        int rgb = 0xffa040;
        int count = 32;
        if ("Explode".equals(kind)) {
            Object p = source.prop("Position");
            if (p instanceof Vector3) {
                at = (Vector3) p;
            }
            count = 96;
        } else {
            Instance part = source.getParent();
            if (part != null && part.isA("BasePart")) {
                CFrame c = Behaviors.cf(part);
                at = new Vector3(c.x, c.y, c.z);
            }
            if (arg instanceof Double) {
                count = (int) Math.min(256, ((Double) arg).doubleValue());
            }
            rgb = SceneSnapshot.rgb(source.prop("Color") instanceof Color3 ? source.prop("Color") : null);
        }
        float[] old;
        float[] b;
        do {
            // The render callback swaps the list out; retry if it did meanwhile.
            old = bursts.get();
            b = new float[old.length + 5];
            System.arraycopy(old, 0, b, 0, old.length);
            b[old.length] = (float) at.x;
            b[old.length + 1] = (float) at.y;
            b[old.length + 2] = (float) at.z;
            b[old.length + 3] = count;
            b[old.length + 4] = Float.intBitsToFloat(rgb);
        } while (!bursts.compareAndSet(old, b));
    }

    @Override
    public void onInit(GraphicsDevice device) {
        meshes[SceneSnapshot.BLOCK] = Primitives.cube(device, 1f);
        meshes[SceneSnapshot.BALL] = Primitives.sphere(device, 0.5f, 16, 24, false);
        meshes[SceneSnapshot.CYLINDER] = PartMeshes.build(device, PartMeshes.cylinderTriangles(24));
        meshes[SceneSnapshot.WEDGE] = PartMeshes.build(device, PartMeshes.wedgeTriangles());
        meshes[SceneSnapshot.CORNER_WEDGE] = PartMeshes.build(device, PartMeshes.cornerWedgeTriangles());
        particleMesh = Primitives.cube(device, 1f);
        sparks = new Material(Material.Type.UNLIT).setShader(Fx.fire());
        sparks.setRenderState(RenderState.transparent().setBlendMode(RenderState.BlendMode.ADDITIVE)
                .setDepthWrite(false));
        sparks.setCastShadows(false);
        light.setShadows(true).setShadowMap(2048, 80f).setShadowStyle(0.7f, 0.0015f).setShadowSoftness(1.5f);
    }

    @Override
    public void onResize(GraphicsDevice device, int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        camera.setAspect((float) width / height);
        device.setViewport(0, 0, w, h);
    }

    /// The material a place material and transparency map to. Cached by key;
    /// the part colour reaches it per instance.
    Material material(String name, boolean transparent) {
        String key = name + (transparent ? "/t" : "");
        Material m = materials.get(key);
        if (m != null) {
            return m;
        }
        m = new Material(Material.Type.PBR);
        ShaderMaterial s = shaderFor(name);
        if (s != null) {
            m.setShader(s);
        }
        if ("Neon".equals(name)) {
            m.setType(Material.Type.UNLIT);
            m.setCastShadows(false);
        } else if ("Metal".equals(name) || "DiamondPlate".equals(name) || "CorrodedMetal".equals(name)) {
            m.setMetallic(0.9f).setRoughness(0.35f);
        } else if ("SmoothPlastic".equals(name)) {
            m.setMetallic(0).setRoughness(0.3f);
        } else {
            m.setMetallic(0).setRoughness(0.7f);
        }
        if (transparent || "Glass".equals(name) || "ForceField".equals(name) || "Water".equals(name)) {
            m.setRenderState(RenderState.transparent());
            m.setCastShadows(false);
        }
        materials.put(key, m);
        return m;
    }

    /// The shader pack a place material is drawn with; null for plain PBR.
    static ShaderMaterial shaderFor(String name) {
        switch (name) {
            case "Wood":
            case "WoodPlanks":
                return Surface.wood();
            case "Concrete":
            case "Pavement":
            case "Brick":
            case "Cobblestone":
                return Surface.concrete();
            case "Slate":
            case "Granite":
            case "Rock":
            case "Basalt":
            case "Limestone":
            case "Sandstone":
                return Terrain.rock();
            case "Glass":
                return Surface.glass();
            case "Ice":
            case "Glacier":
                return Surface.ice();
            case "Fabric":
                return Surface.fabric();
            case "CorrodedMetal":
                return Surface.rust();
            case "Metal":
            case "DiamondPlate":
                return Surface.paintedMetal();
            case "ForceField":
                return Fx.forceField();
            case "CrackedLava":
                return Liquid.lava();
            case "Water":
                return Water.lake();
            default:
                return null;
        }
    }

    @Override
    public void onFrame(GraphicsDevice device) {
        SceneSnapshot s = next.getAndSet(null);
        if (s != null) {
            scene = s;
        }
        time += 1 / 60f;
        device.setTime(time);
        camera.setPerspective(scene.fov, 0.1f, 2000f).setPosition(scene.eye[0], scene.eye[1], scene.eye[2])
                .setTarget(scene.target[0], scene.target[1], scene.target[2]);
        device.setCamera(camera);
        lighting(scene);
        device.setLight(light);
        device.setEnvironment(env);
        Map<String, Batch> batches = batch(scene);
        if (device.beginShadowPass(light)) {
            for (Batch b : batches.values()) {
                if (!b.transparent) {
                    device.drawInstanced(meshes[b.shape], b.material, b.matrices, b.colors, b.count);
                }
            }
            device.endShadowPass();
        }
        boolean postOn = device.beginPostProcess(post, width, height);
        device.clear(0xff000000 | sky(scene.clockTime), true, true);
        for (Batch b : batches.values()) {
            if (!b.transparent) {
                device.drawInstanced(meshes[b.shape], b.material, b.matrices, b.colors, b.count);
            }
        }
        for (Batch b : batches.values()) {
            if (b.transparent) {
                device.drawInstanced(meshes[b.shape], b.material, b.matrices, b.colors, b.count);
            }
        }
        particles(device);
        if (postOn) {
            device.endPostProcess(time);
        }
    }

    /// Instances of one shape and material.
    static final class Batch {
        final int shape;
        final Material material;
        final boolean transparent;
        float[] matrices = new float[16 * 8];
        int[] colors = new int[8];
        int count;

        Batch(int shape, Material material, boolean transparent) {
            this.shape = shape;
            this.material = material;
            this.transparent = transparent;
        }

        void add(float[] m, int argb) {
            if (count == colors.length) {
                float[] nm = new float[matrices.length * 2];
                System.arraycopy(matrices, 0, nm, 0, matrices.length);
                matrices = nm;
                int[] nc = new int[colors.length * 2];
                System.arraycopy(colors, 0, nc, 0, colors.length);
                colors = nc;
            }
            System.arraycopy(m, 0, matrices, count * 16, 16);
            colors[count] = argb;
            count++;
        }
    }

    Map<String, Batch> batch(SceneSnapshot s) {
        Map<String, Batch> out = new HashMap<String, Batch>();
        for (int i = 0; i < s.items.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            SceneSnapshot.Item it = s.items.get(i);
            boolean tr = it.transparency > 0.01f;
            String key = it.shape + ":" + it.material + (tr ? "/t" : "");
            Batch b = out.get(key);
            if (b == null) {
                b = new Batch(it.shape, material(it.material, tr), tr);
                out.put(key, b);
            }
            int a = Math.round((1 - it.transparency) * 255);
            b.add(it.model, (a << 24) | it.rgb);
        }
        return out;
    }

    /// Sun, sky, fog and the post effects from a snapshot.
    void lighting(SceneSnapshot s) {
        float[] d = SceneSnapshot.sunDirection(s.clockTime);
        boolean day = s.clockTime > 6 && s.clockTime < 18;
        float k = Math.min(1, s.brightness / 3f);
        light.setDirection(d[0], d[1], d[2]).setColor(day ? scale(0xfff4e0, k) : 0xff384a70)
                .setAmbientColor(0xff000000 | mix(s.ambient, s.outdoorAmbient));
        int horizon = sky(s.clockTime);
        env.setSky(day ? 0xff3f7fd8 : 0xff070b1c, 0xff000000 | horizon, day ? 0xfffff2c0 : 0xffb0c0ff,
                s.atmosphereDensity * 0.5f);
        env.setHemisphere(0xff000000 | s.outdoorAmbient, 0xff000000 | s.ambient);
        env.setToneMapping(true).setExposure(0.6f + k * 0.6f);
        if (s.atmosphereDensity > 0) {
            env.setFog(0xff000000 | s.atmosphereColor, s.atmosphereDensity * 0.01f, 0.02f);
        } else if (s.fogEnd < 10000) {
            env.setFog(0xff000000 | s.fogColor, 2.5f / Math.max(1, s.fogEnd), 0);
        } else {
            env.setFog(0, 0, 0);
        }
        post.setBloom(s.bloomIntensity > 0 ? Math.max(0.2f, Math.min(1.5f, s.bloomThreshold)) : 1.5f,
                Math.min(2f, s.bloomIntensity));
        post.setGrade(1 + s.contrast, 1 + s.saturation, 0xff000000 | s.tint, 0xff000000 | s.tint);
        post.setGodRays(day ? Math.min(1f, s.sunRays * 2) : 0);
        if (!Float.isNaN(s.focusDistance) && s.blur > 0) {
            post.setDepthOfField(s.focusDistance, s.blur);
        } else {
            post.setDepthOfField(0, 0);
        }
        post.setFxaa(true);
    }

    /// The horizon colour at a clock time.
    static int sky(float clock) {
        if (clock < 5 || clock > 19.5f) {
            return 0x101828;
        }
        if (clock < 7 || clock > 17.5f) {
            return 0xe08850;
        }
        return 0xb8d4f0;
    }

    private static int scale(int rgb, float k) {
        int r = Math.round(((rgb >> 16) & 255) * k);
        int g = Math.round(((rgb >> 8) & 255) * k);
        int b = Math.round((rgb & 255) * k);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private static int mix(int a, int b) {
        int r = (((a >> 16) & 255) + ((b >> 16) & 255)) / 2;
        int g = (((a >> 8) & 255) + ((b >> 8) & 255)) / 2;
        int bl = ((a & 255) + (b & 255)) / 2;
        return (r << 16) | (g << 8) | bl;
    }

    /// Integrates and draws the live particles: x,y,z, vx,vy,vz, age, colour.
    private void particles(GraphicsDevice device) {
        float[] b = bursts.getAndSet(new float[0]);
        for (int i = 0; i + 4 < b.length; i += 5) {
            int n = (int) b[i + 3];
            for (int k = 0; k < n && particleCount < MAX_PARTICLES; k++) {
                int o = particleCount * 8;
                double th = hash(k * 7 + i) * Math.PI * 2;
                double ph = hash(k * 13 + i + 1) * Math.PI;
                double sp = 6 + hash(k * 3 + i + 2) * 10;
                particles[o] = b[i];
                particles[o + 1] = b[i + 1];
                particles[o + 2] = b[i + 2];
                particles[o + 3] = (float) (Math.cos(th) * Math.sin(ph) * sp);
                particles[o + 4] = (float) (Math.cos(ph) * sp);
                particles[o + 5] = (float) (Math.sin(th) * Math.sin(ph) * sp);
                particles[o + 6] = 0;
                particles[o + 7] = b[i + 4];
                particleCount++;
            }
        }
        if (particleCount == 0) {
            return;
        }
        float dt = 1 / 60f;
        float[] m = new float[particleCount * 16];
        int[] c = new int[particleCount];
        int live = 0;
        for (int p = 0; p < particleCount; p++) {
            int o = p * 8;
            float age = particles[o + 6] + dt;
            if (age > 1.2f) {
                continue;
            }
            int d = live * 8;
            particles[d] = particles[o] + particles[o + 3] * dt;
            particles[d + 1] = particles[o + 1] + particles[o + 4] * dt;
            particles[d + 2] = particles[o + 2] + particles[o + 5] * dt;
            particles[d + 3] = particles[o + 3] * 0.96f;
            particles[d + 4] = particles[o + 4] * 0.96f - 9.8f * dt;
            particles[d + 5] = particles[o + 5] * 0.96f;
            particles[d + 6] = age;
            particles[d + 7] = particles[o + 7];
            float sz = 0.4f * (1.2f - age);
            float[] t = Matrix4.translation(particles[d], particles[d + 1], particles[d + 2]);
            t[0] = sz;
            t[5] = sz;
            t[10] = sz;
            System.arraycopy(t, 0, m, live * 16, 16);
            int a = Math.round(255 * (1.2f - age) / 1.2f);
            c[live] = (a << 24) | (Float.floatToRawIntBits(particles[d + 7]) & 0xffffff);
            live++;
        }
        particleCount = live;
        if (live > 0) {
            device.drawInstanced(particleMesh, sparks, m, c, live);
        }
    }

    private static double hash(int n) {
        int x = n * 0x45d9f3b;
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        x = (x >>> 16) ^ x;
        return (x & 0xffff) / 65536.0;
    }

    @Override
    public void onDispose(GraphicsDevice device) {
        materials.clear();
    }

    /// The batches a snapshot draws as; for tests and the creator's stats panel.
    List<String> batchKeys(SceneSnapshot s) {
        return new ArrayList<String>(batch(s).keySet());
    }
}
