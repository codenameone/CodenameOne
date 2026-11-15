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
import com.codename1.gpu.GltfScene;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shaders.Fx;
import com.codename1.gpu.shaders.Foliage;
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
    /// Floats a live particle keeps: position, velocity, age, colour bits,
    /// life, upward acceleration, size, and whether it grows (smoke) or
    /// shrinks (sparks and flames) as it ages.
    private static final int STRIDE = 12;
    private int frameNo;

    private final AtomicReference<SceneSnapshot> next = new AtomicReference<SceneSnapshot>();
    private final AtomicReference<float[]> bursts = new AtomicReference<float[]>(new float[0]);
    private SceneSnapshot scene = new SceneSnapshot();
    private final Camera camera = new Camera();
    private final Light light = new Light();
    private final Environment env = new Environment();
    private final PostProcess post = new PostProcess();
    private final Avatars avatars = new Avatars();

    /// The post effects the last [#lighting] call set; for tests.
    PostProcess postProcess() {
        return post;
    }
    private final Mesh[] meshes = new Mesh[6];
    /// Models drawn in place of primitives, by [SceneSnapshot.Item#kind]
    /// and then variant; null where a kind keeps its primitive, which is
    /// every kind when the bundled models failed to load.
    private final Model[][] models = new Model[SceneSnapshot.ROCK + 1][];
    private final float[] treeMatrix = new float[16];
    private Mesh sky;
    private Material skyMaterial;
    private Mesh faceMesh;
    private Material faceMaterial;
    private final float[] skyMatrix = new float[16]; // NOPMD SingularField - reused every frame, never allocated per frame
    private final Map<String, Material> materials = new HashMap<String, Material>();
    private Mesh particleMesh;
    private Material sparks;
    private final float[] particles = new float[MAX_PARTICLES * STRIDE]; // NOPMD SingularField - particles live across frames
    private int particleCount;
    private int width = 1;
    private int height = 1;
    private float time;

    /// Hands the next frame over. Called on the EDT after each engine step.
    public void submit(SceneSnapshot s) {
        next.set(s);
    }

    /// One painted HUD frame: the place's GUI and the touch controls, as
    /// ARGB pixels covering the whole view.
    static final class Hud {
        final int[] argb;
        final int width;
        final int height;

        Hud(int[] argb, int width, int height) {
            this.argb = argb;
            this.width = width;
            this.height = height;
        }
    }

    private final AtomicReference<Hud> nextHud = new AtomicReference<Hud>();
    private Texture hudTexture;
    private Mesh hudQuad;
    private Material hudMaterial;
    private final Camera hudCamera = new Camera();
    // no fog, no grade: the HUD shows its own colours
    private final Environment hudEnv = new Environment();
    private final float[] hudMatrix = new float[16];

    private final AtomicReference<float[]> nextGuides = new AtomicReference<float[]>(new float[0]);
    private Mesh guideMesh;
    private Material guideMaterial;
    private float[] guideMatrices = new float[0];
    private int[] guideColors = new int[0];

    /// The studio's selection outline and handles (see [Gizmo#guides]),
    /// drawn over the scene without depth so they are never hidden by
    /// what they select. Called on the EDT.
    public void submitGuides(float[] g) {
        nextGuides.set(g == null ? new float[0] : g);
    }

    private void drawGuides(GraphicsDevice device) {
        float[] g = nextGuides.get();
        int n = g.length / 17;
        if (n == 0) {
            return;
        }
        if (guideMesh == null) {
            guideMesh = Primitives.cube(device, 1f);
            guideMaterial = new Material(Material.Type.UNLIT).setRenderState(RenderState.transparent()
                    .setDepthTest(false).setDepthWrite(false));
            guideMaterial.setCastShadows(false);
        }
        if (guideColors.length < n) {
            guideMatrices = new float[n * 16];
            guideColors = new int[n];
        }
        for (int i = 0; i < n; i++) {
            System.arraycopy(g, i * 17, guideMatrices, i * 16, 16);
            guideColors[i] = Float.floatToRawIntBits(g[i * 17 + 16]);
        }
        device.drawInstanced(guideMesh, guideMaterial, guideMatrices, guideColors, n);
    }

    /// Hands over the HUD painted on the EDT. Lightweight components are
    /// not drawn over a GPU view on every port, so the HUD is drawn here,
    /// as a texture over the finished frame. Called on the EDT.
    public void submitHud(int[] argb, int w, int h) {
        nextHud.set(new Hud(argb, w, h));
    }

    private void drawHud(GraphicsDevice device) {
        Hud h = nextHud.getAndSet(null);
        if (h != null) {
            if (hudTexture != null) {
                device.dispose(hudTexture);
            }
            hudTexture = device.createTexture(h.width, h.height, h.argb);
        }
        if (hudTexture == null) {
            return;
        }
        if (hudQuad == null) {
            hudQuad = Primitives.quad(device, 1f);
            hudMaterial = new Material(Material.Type.SPRITE).setRenderState(RenderState.transparent()
                    .setDepthTest(false).setCullMode(RenderState.CullMode.NONE));
        }
        hudCamera.setOrthographic(height, -10f, 10f).setAspect((float) width / height)
                .setPosition(0f, 0f, 1f).setTarget(0f, 0f, 0f).setUp(0f, 1f, 0f);
        device.setCamera(hudCamera);
        device.setEnvironment(hudEnv);
        hudMaterial.setTexture(hudTexture).setColor(0xffffffff);
        Matrix4.multiply(Matrix4.identity(), Matrix4.scaling(width, height, 1f), hudMatrix);
        device.draw(hudQuad, hudMaterial, hudMatrix);
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
        meshes[SceneSnapshot.BALL] = Primitives.sphere(device, 0.5f, 32, 48, false);
        meshes[SceneSnapshot.CYLINDER] = PartMeshes.build(device, PartMeshes.cylinderTriangles(24));
        meshes[SceneSnapshot.WEDGE] = PartMeshes.build(device, PartMeshes.wedgeTriangles());
        meshes[SceneSnapshot.CORNER_WEDGE] = PartMeshes.build(device, PartMeshes.cornerWedgeTriangles());
        meshes[SceneSnapshot.HEAD] = PartMeshes.build(device, PartMeshes.headTriangles(32));
        loadModels(device);
        particleMesh = Primitives.cube(device, 1f);
        sky = Primitives.sphere(device, 900f, 16, 24, true);
        skyMaterial = new Material(Material.Type.SKYBOX);
        skyMaterial.setCastShadows(false);
        faceMesh = PartMeshes.face(device);
        faceMaterial = new Material(Material.Type.PBR).setRoughness(0.6f)
                .setTexture(device.createTexture(64, 64, PartMeshes.facePixels(64)));
        faceMaterial.setRenderState(RenderState.transparent());
        faceMaterial.setCastShadows(false);
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
        if ("Glass".equals(name) && s != null) {
            // Place glass reads as tinted, glossy glass: a part's own
            // Transparency fades it further. The shader's default suits a
            // clear pane, and drew a coloured gem as a ghost.
            s.setUniform(Surface.OPACITY, 0.55f);
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
            case "Grass":
                return Terrain.turf();
            case "LeafyGrass":
                return Foliage.canopy();
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
        avatars.update(device, scene, 1 / 60f);
        Map<String, Batch> batches = batch(scene);
        if (device.beginShadowPass(light)) {
            avatars.draw(device);
            for (Batch b : batches.values()) {
                if (!b.transparent) {
                    device.drawInstanced(b.mesh, b.material, b.matrices, b.colors, b.count);
                }
            }
            device.endShadowPass();
        }
        boolean postOn = device.beginPostProcess(post, width, height);
        device.clear(0xff000000 | sky(scene.clockTime), true, true);
        // The sky dome follows the eye, so it is never reached or left.
        for (int i = 0; i < 16; i++) {
            skyMatrix[i] = i % 5 == 0 ? 1 : 0;
        }
        skyMatrix[12] = scene.eye[0];
        skyMatrix[13] = scene.eye[1];
        skyMatrix[14] = scene.eye[2];
        device.draw(sky, skyMaterial, skyMatrix);
        avatars.draw(device);
        for (Batch b : batches.values()) {
            if (!b.transparent) {
                device.drawInstanced(b.mesh, b.material, b.matrices, b.colors, b.count);
            }
        }
        for (Batch b : batches.values()) {
            if (b.transparent) {
                device.drawInstanced(b.mesh, b.material, b.matrices, b.colors, b.count);
            }
        }
        for (int i = 0; i < scene.items.size(); i++) { // NOPMD ForLoopCanBeForeach - per frame; no iterator
            SceneSnapshot.Item it = scene.items.get(i);
            if (it.face && !onBody(it)) {
                device.draw(faceMesh, faceMaterial, it.model);
            }
        }
        particles(device);
        if (postOn) {
            device.endPostProcess(time);
        }
        // after the grade and the bloom, so a handle keeps its true colour
        drawGuides(device);
        drawHud(device);
    }

    /// A model fitted to the unit cube: its pieces, and whether they take
    /// the part's colour (limbs, rocks) or keep their own (trees).
    static final class Model {
        final Mesh[] meshes;
        final int[] colors;
        final boolean tinted;

        Model(Mesh[] meshes, int[] colors, boolean tinted) {
            this.meshes = meshes;
            this.colors = colors;
            this.tinted = tinted;
        }
    }

    private void loadModels(GraphicsDevice device) {
        try {
            GltfScene c = ModelMeshes.scene(ModelMeshes.CHARACTER);
            for (int k = SceneSnapshot.TORSO; c != null && k <= SceneSnapshot.RIGHT_LEG; k++) {
                Model m = model(device, ModelMeshes.fitted(c, ModelMeshes.characterNode(k), true), true);
                models[k] = m == null ? null : new Model[] {m};
            }
            models[SceneSnapshot.TRUNK] = variants(device, ModelMeshes.TREES, false);
            models[SceneSnapshot.ROCK] = variants(device, ModelMeshes.ROCKS, true);
        } catch (RuntimeException ex) {
            // A model that will not upload leaves its parts on the primitives.
            for (int i = 0; i < models.length; i++) {
                models[i] = null;
            }
        }
    }

    private static Model[] variants(GraphicsDevice device, String[] names, boolean tinted) {
        List<Model> out = new ArrayList<Model>();
        for (String n : names) {
            GltfScene s = ModelMeshes.scene(n);
            ModelMeshes.Piece[] pieces = s == null ? null : ModelMeshes.fitted(s, null, false);
            for (int i = 0; pieces != null && !tinted && i < pieces.length; i++) {
                if (isCrown(pieces[i].rgb)) {
                    pieces[i] = ModelMeshes.smoothed(pieces[i]);
                }
            }
            Model m = pieces == null ? null : model(device, pieces, tinted);
            if (m != null && !tinted) {
                for (int i = 0; i < m.colors.length; i++) {
                    m.colors[i] = foliage(m.colors[i]);
                }
            }
            if (m != null) {
                out.add(m);
            }
        }
        return out.isEmpty() ? null : out.toArray(new Model[out.size()]);
    }

    /// The Nature Kit paints its crowns a stylised teal, which reads as
    /// cyan beside the templates' grass; a crown (green over red) becomes a
    /// leaf green of the same brightness, and bark keeps its colour.
    static int foliage(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        if (g <= r || g < 80) {
            return rgb;
        }
        int l = (r + g + b) / 3;
        int nr = Math.min(255, l * 55 / 100);
        int ng = Math.min(255, l * 135 / 100);
        int nb = Math.min(255, l * 45 / 100);
        return (rgb & 0xff000000) | (nr << 16) | (ng << 8) | nb;
    }

    /// True for a crown colour of a tree model (green over red).
    static boolean isCrown(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        return g > r && g >= 60;
    }

    private static Model model(GraphicsDevice device, ModelMeshes.Piece[] pieces, boolean tinted) {
        if (pieces == null) {
            return null;
        }
        Mesh[] m = new Mesh[pieces.length];
        int[] c = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            m[i] = ModelMeshes.mesh(device, pieces[i]);
            c[i] = pieces[i].rgb;
        }
        return new Model(m, c, tinted);
    }

    /// The box a tree grows into from a trunk's model matrix: from the
    /// trunk's bottom, 1.75 trunk lengths tall and 0.9 wide, which covers the
    /// crown of `Leaves` the templates put on it.
    static void treeBox(float[] trunk, float[] out) {
        float h = 0;
        for (int c = 0; c < 3; c++) {
            float x = trunk[c * 4];
            float y = trunk[c * 4 + 1];
            float z = trunk[c * 4 + 2];
            h = Math.max(h, (float) Math.sqrt(x * x + y * y + z * z));
        }
        float tall = h * 1.75f;
        float wide = h * 0.9f;
        for (int i = 0; i < 16; i++) {
            out[i] = 0;
        }
        out[0] = wide;
        out[5] = tall;
        out[10] = wide;
        out[12] = trunk[12];
        out[13] = trunk[13] - h / 2 + tall / 2;
        out[14] = trunk[14];
        out[15] = 1;
    }

    /// Instances of one mesh and material.
    static final class Batch {
        final Mesh mesh;
        final Material material;
        final boolean transparent;
        float[] matrices = new float[16 * 8];
        int[] colors = new int[8];
        int count;

        Batch(Mesh mesh, Material material, boolean transparent) {
            this.mesh = mesh;
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
            if (onBody(it)) {
                continue; // the animated body stands in for the rig
            }
            boolean tr = it.transparency > 0.01f;
            int a = Math.round((1 - it.transparency) * 255);
            Model[] variants = models[it.kind];
            if (it.kind == SceneSnapshot.LEAVES && models[SceneSnapshot.TRUNK] != null) {
                continue; // the tree model drawn for the trunk carries its own crown
            }
            if (variants == null) {
                String key = it.shape + ":" + it.material + (tr ? "/t" : "");
                Batch b = out.get(key);
                if (b == null) {
                    b = new Batch(meshes[it.shape], material(it.material, tr), tr);
                    out.put(key, b);
                }
                b.add(it.model, (a << 24) | it.rgb);
                continue;
            }
            int v = (int) (Math.abs(Math.floor(it.model[12]) * 31 + Math.floor(it.model[14])) % variants.length);
            Model m = variants[v];
            float[] at = it.model;
            if (it.kind == SceneSnapshot.TRUNK) {
                treeBox(it.model, treeMatrix);
                at = treeMatrix;
            }
            for (int p = 0; p < m.meshes.length; p++) {
                // Trees keep their own bark and leaf colours; the crown is
                // drawn as foliage and the bark as wood rather than as the
                // kit's flat facets.
                String mat = m.tinted ? it.material : isCrown(m.colors[p]) ? "LeafyGrass" : "Wood";
                String key = "m" + it.kind + "." + v + "." + p + ":" + mat + (tr ? "/t" : "");
                Batch b = out.get(key);
                if (b == null) {
                    b = new Batch(m.meshes[p], material(mat, tr), tr);
                    out.put(key, b);
                }
                b.add(at, (a << 24) | (m.tinted ? it.rgb : m.colors[p]));
            }
        }
        return out;
    }

    /// A limb, head or accessory of a character drawn as an animated body.
    private boolean onBody(SceneSnapshot.Item it) {
        Instance p = it.part.getParent();
        return avatars.draws(p) || p != null && avatars.draws(p.getParent());
    }

    /// Whether a kind is drawn with a bundled model; for tests.
    boolean drawsModel(int kind) {
        return models[kind] != null;
    }

    /// Sun, sky, fog and the post effects from a snapshot.
    void lighting(SceneSnapshot s) {
        float[] d = SceneSnapshot.sunDirection(s.clockTime);
        boolean day = s.clockTime > 6 && s.clockTime < 18;
        float k = Math.min(1, s.brightness / 3f);
        float g = golden(s.clockTime);
        light.setDirection(d[0], d[1], d[2]).setColor(day ? scale(lerp(0xfff4e0, 0xffb46a, g), k) : 0xff384a70)
                .setAmbientColor(0xff000000 | mix(s.ambient, s.outdoorAmbient));
        int horizon = sky(s.clockTime);
        env.setSky(day ? 0xff000000 | lerp(0x3f7fd8, 0x4a5f9a, g) : 0xff070b1c, 0xff000000 | horizon,
                day ? 0xff000000 | lerp(0xfff2c0, 0xffb060, g) : 0xffb0c0ff, 0.3f + s.atmosphereDensity * 0.5f);
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
        // ColorCorrection's TintColor multiplies the image (white leaves it);
        // it is the highlight tint. The shadow tint is a lift added to the
        // darkest tones, so it stays black: lifting by the default white tint
        // turned every frame solid white.
        post.setGrade(1 + s.contrast, 1 + s.saturation, 0xff000000, 0xff000000 | s.tint);
        post.setGodRays(day ? Math.min(1f, s.sunRays * 2) : 0);
        if (!Float.isNaN(s.focusDistance) && s.blur > 0) {
            post.setDepthOfField(s.focusDistance, s.blur);
        } else {
            post.setDepthOfField(0, 0);
        }
        post.setFxaa(true);
        // Contact shadows where parts meet and a soft vignette: without them
        // blocks float on each other and the frame reads as flat.
        post.setAmbientOcclusion(0.55f, 14f);
        post.setVignette(0.22f);
    }

    /// The horizon colour at a clock time: night, a warm band at dawn and
    /// dusk that the day colour blends into, and the day.
    static int sky(float clock) {
        if (clock < 5 || clock > 19.5f) {
            return 0x101828;
        }
        if (clock < 6 || clock > 18.5f) {
            float t = clock < 12 ? clock - 5 : 19.5f - clock;
            return lerp(0x101828, 0xe08850, t);
        }
        return lerp(0xb8d4f0, 0xf0a066, golden(clock));
    }

    /// 0 through the middle of the day, rising to 1 as the sun nears the
    /// horizon (from 15:30 and before 8:30): how golden the light is.
    static float golden(float clock) {
        float h = Math.abs(clock - 12);
        if (h < 3.5f) {
            return 0;
        }
        float t = Math.min(1, (h - 3.5f) / 2.5f);
        return t * t * (3 - 2 * t);
    }

    /// Mixes two 0xRRGGBB colours; `t` 0 gives `a`.
    static int lerp(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int gr = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (gr << 8) | bl;
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
        frameNo++;
        float dt = 1 / 60f;
        float[] b = bursts.getAndSet(new float[0]);
        for (int i = 0; i + 4 < b.length; i += 5) {
            int n = (int) b[i + 3];
            for (int k = 0; k < n; k++) {
                double th = hash(k * 7 + i) * Math.PI * 2;
                double ph = hash(k * 13 + i + 1) * Math.PI;
                double sp = 6 + hash(k * 3 + i + 2) * 10;
                spawn(b[i], b[i + 1], b[i + 2], (float) (Math.cos(th) * Math.sin(ph) * sp), (float) (Math.cos(ph) * sp),
                        (float) (Math.sin(th) * Math.sin(ph) * sp), Float.floatToRawIntBits(b[i + 4]), 1.2f, -9.8f,
                        0.48f, false);
            }
        }
        List<SceneSnapshot.Emitter> es = scene.emitters;
        for (int i = 0; i < es.size(); i++) { // NOPMD ForLoopCanBeForeach - per frame; no iterator
            emit(es.get(i), i, dt);
        }
        if (particleCount == 0) {
            return;
        }
        float[] m = new float[particleCount * 16];
        int[] c = new int[particleCount];
        int live = 0;
        for (int p = 0; p < particleCount; p++) {
            int o = p * STRIDE;
            float age = particles[o + 6] + dt;
            float life = particles[o + 8];
            if (age > life) {
                continue;
            }
            int d = live * STRIDE;
            particles[d] = particles[o] + particles[o + 3] * dt;
            particles[d + 1] = particles[o + 1] + particles[o + 4] * dt;
            particles[d + 2] = particles[o + 2] + particles[o + 5] * dt;
            particles[d + 3] = particles[o + 3] * 0.96f;
            particles[d + 4] = particles[o + 4] * 0.96f + particles[o + 9] * dt;
            particles[d + 5] = particles[o + 5] * 0.96f;
            particles[d + 6] = age;
            particles[d + 7] = particles[o + 7];
            particles[d + 8] = life;
            particles[d + 9] = particles[o + 9];
            particles[d + 10] = particles[o + 10];
            particles[d + 11] = particles[o + 11];
            float f = age / life;
            float sz = particles[d + 10] * (particles[d + 11] > 0 ? 0.5f + f : 1 - f);
            float[] t = Matrix4.translation(particles[d], particles[d + 1], particles[d + 2]);
            t[0] = sz;
            t[5] = sz;
            t[10] = sz;
            System.arraycopy(t, 0, m, live * 16, 16);
            int a = Math.round(255 * (1 - f));
            c[live] = (a << 24) | (Float.floatToRawIntBits(particles[d + 7]) & 0xffffff);
            live++;
        }
        particleCount = live;
        if (live > 0) {
            device.drawInstanced(particleMesh, sparks, m, c, live);
        }
    }

    /// This frame's share of a continuous effect, with the fraction carried
    /// by a hash of the frame so a rate under one a frame still emits.
    private void emit(SceneSnapshot.Emitter e, int index, float dt) {
        float want = e.rate * dt;
        int n = (int) want;
        if (hash(frameNo * 31 + index * 977) < want - n) {
            n++;
        }
        for (int k = 0; k < n; k++) {
            int seed = frameNo * 131 + index * 7919 + k * 17;
            float r1 = (float) hash(seed);
            float r2 = (float) hash(seed + 1);
            float r3 = (float) hash(seed + 2);
            float r4 = (float) hash(seed + 3);
            float speed = e.speedMin + (e.speedMax - e.speedMin) * r1;
            float life = e.lifeMin + (e.lifeMax - e.lifeMin) * r2;
            switch (e.kind) {
                case SceneSnapshot.FIRE:
                    spawn(e.x + (r3 - 0.5f) * e.size * 2, e.y, e.z + (r4 - 0.5f) * e.size * 2, (r4 - 0.5f) * 0.8f,
                            speed, (r3 - 0.5f) * 0.8f, r1 < 0.65f ? e.rgb : e.rgb2, life, e.rise, e.size, false);
                    break;
                case SceneSnapshot.SMOKE:
                    spawn(e.x + (r3 - 0.5f), e.y, e.z + (r4 - 0.5f), (r4 - 0.5f) * 0.6f, speed, (r3 - 0.5f) * 0.6f,
                            e.rgb, life, 0, e.size, true);
                    break;
                case SceneSnapshot.SPARKLES: {
                    double th = r3 * Math.PI * 2;
                    double ph = r4 * Math.PI;
                    spawn(e.x, e.y, e.z, (float) (Math.cos(th) * Math.sin(ph) * speed), (float) (Math.cos(ph) * speed),
                            (float) (Math.sin(th) * Math.sin(ph) * speed), r1 < 0.7f ? e.rgb : e.rgb2, life, 0, e.size,
                            false);
                    break;
                }
                default:
                    // A ParticleEmitter: from anywhere on the part's top,
                    // upward with a little spread.
                    spawn(e.x + (r3 * 2 - 1) * e.hx, e.y, e.z + (r4 * 2 - 1) * e.hz, (r4 - 0.5f) * speed * 0.3f, speed,
                            (r3 - 0.5f) * speed * 0.3f, r1 < 0.5f ? e.rgb : e.rgb2, life, e.rise, e.size, false);
                    break;
            }
        }
    }

    private void spawn(float x, float y, float z, float vx, float vy, float vz, int rgb, float life, float rise,
            float size, boolean grows) {
        if (particleCount >= MAX_PARTICLES) {
            return;
        }
        int o = particleCount * STRIDE;
        particles[o] = x;
        particles[o + 1] = y;
        particles[o + 2] = z;
        particles[o + 3] = vx;
        particles[o + 4] = vy;
        particles[o + 5] = vz;
        particles[o + 6] = 0;
        particles[o + 7] = Float.intBitsToFloat(rgb);
        particles[o + 8] = life;
        particles[o + 9] = rise;
        particles[o + 10] = size;
        particles[o + 11] = grows ? 1 : 0;
        particleCount++;
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
