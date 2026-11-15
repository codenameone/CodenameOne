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

import com.codename1.gaming.AnimatedModel;
import com.codename1.gpu.GltfScene;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Draws every character as a skinned, animated body instead of its R6 limb
/// parts. The limbs stay in the engine -- physics, Touched and scripts still
/// see a Roblox rig -- and only the picture changes: one KayKit adventurer
/// per character, posed from the humanoid's state and speed and placed on
/// its HumanoidRootPart.
final class Avatars {
    /// The bodies a character can wear, by its `AvatarBody` attribute.
    static final String[] BODIES = {"knight", "barbarian", "mage", "rogue"};
    /// An R6 character stands five studs tall: legs 2, torso 2, head 1.
    static final float HEIGHT = 5.2f;
    private static final float FADE = 0.15f;

    private final Map<String, GltfScene> scenes = new HashMap<String, GltfScene>();
    private final Map<Instance, Body> live = new HashMap<Instance, Body>();
    private final Set<Instance> drawn = new HashSet<Instance>();
    private final float[] scratchA = new float[16];
    private final float[] scratchB = new float[16];
    private boolean loaded;

    /// One character's animated body and what it is playing.
    static final class Body {
        final AnimatedModel model;
        final float scale;
        final float lift;
        final float[] matrix = new float[16];
        String clip = "";
        boolean airborne;
        float landing;
        boolean seen;

        Body(AnimatedModel model, float scale, float lift) {
            this.model = model;
            this.scale = scale;
            this.lift = lift;
        }
    }

    /// Loads the bundled bodies; false when none loads, and the renderer
    /// then keeps drawing the limb parts.
    boolean load() {
        if (!loaded) {
            loaded = true;
            for (String b : BODIES) {
                GltfScene s = ModelMeshes.scene("avatar_" + b + ".glb");
                if (s != null) {
                    scenes.put(b, s);
                }
            }
        }
        return !scenes.isEmpty();
    }

    /// True when `m` is a character drawn as a body this frame, so its limb
    /// parts and face decal are not drawn as well.
    boolean draws(Instance m) {
        return m != null && drawn.contains(m);
    }

    /// The clip a character's state and speed call for.
    static String clipFor(String state, float speed, boolean landing) {
        if ("Dead".equals(state)) {
            return "Death_A";
        }
        if ("Jumping".equals(state)) {
            return "Jump_Start";
        }
        if ("Freefall".equals(state) || "FallingDown".equals(state)) {
            return "Jump_Idle";
        }
        if (landing && speed < 4) {
            return "Jump_Land";
        }
        if (speed > 9) {
            return "Running_A";
        }
        if (speed > 0.5f) {
            return "Walking_A";
        }
        return "Idle";
    }

    /// Picks up this frame's characters, starts or blends their clips and
    /// advances them by `dt` seconds.
    void update(GraphicsDevice device, SceneSnapshot s, float dt) {
        drawn.clear();
        if (!load()) {
            return;
        }
        for (Body b : live.values()) {
            b.seen = false;
        }
        List<SceneSnapshot.Figure> fs = s.figures;
        for (int i = 0; i < fs.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator
            SceneSnapshot.Figure f = fs.get(i);
            Body b = live.get(f.model);
            if (b == null) {
                b = create(device, f);
                if (b == null) {
                    continue;
                }
                live.put(f.model, b);
            }
            b.seen = true;
            drawn.add(f.model);
            boolean air = "Jumping".equals(f.state) || "Freefall".equals(f.state);
            if (b.airborne && !air) {
                b.landing = 0.35f;
            }
            b.airborne = air;
            b.landing = Math.max(0, b.landing - dt);
            String clip = clipFor(f.state, f.speed, b.landing > 0);
            if ("Jump_Start".equals(b.clip) && "Jump_Start".equals(clip) && b.model.isFinished()) {
                clip = "Jump_Idle";
            }
            if (!clip.equals(b.clip)) {
                boolean loop = !"Death_A".equals(clip) && !"Jump_Start".equals(clip) && !"Jump_Land".equals(clip);
                b.model.crossFade(clip, b.clip.length() == 0 ? 0 : FADE, loop);
                b.clip = clip;
            }
            // Feet keep pace with the ground: the run clip is authored for
            // about 7 studs a second, the walk for about 3.
            if ("Running_A".equals(clip)) {
                b.model.setSpeed(Math.max(0.8f, Math.min(2.2f, f.speed / 12f)));
            } else if ("Walking_A".equals(clip)) {
                b.model.setSpeed(Math.max(0.6f, Math.min(1.6f, f.speed / 5f)));
            } else {
                b.model.setSpeed(1);
            }
            b.model.update(dt);
            Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(f.yaw), 0f, 1f, 0f),
                    Matrix4.scaling(b.scale, b.scale, b.scale), scratchA);
            Matrix4.multiply(Matrix4.translation(f.x, f.y + b.lift, f.z), scratchA, scratchB);
            System.arraycopy(scratchB, 0, b.matrix, 0, 16);
        }
        // A character that left the place (a player gone, a model destroyed)
        Iterator<Map.Entry<Instance, Body>> it = live.entrySet().iterator();
        while (it.hasNext()) {
            if (!it.next().getValue().seen) {
                it.remove();
            }
        }
    }

    /// Draws every live body; in the shadow pass too, so they cast shadows.
    void draw(GraphicsDevice device) {
        for (Body b : live.values()) {
            if (!b.seen) {
                continue;
            }
            com.codename1.gaming.Model[] parts = b.model.getParts();
            for (com.codename1.gaming.Model part : parts) {
                if (part.isVisible()) {
                    device.draw(part.getMesh(), part.getMaterial(), b.matrix);
                }
            }
        }
    }

    private Body create(GraphicsDevice device, SceneSnapshot.Figure f) {
        String name = f.body;
        if (name == null || !scenes.containsKey(name)) {
            // No choice made: a stable body per character, so a room of
            // players is not a room of twins.
            name = BODIES[Math.abs(f.model.getName().hashCode() % BODIES.length)];
        }
        GltfScene s = scenes.get(name);
        if (s == null) {
            s = scenes.values().iterator().next();
        }
        AnimatedModel m = new AnimatedModel(device, s);
        m.setMaterialType(Material.Type.PBR);
        m.play("Idle", true);
        float[] bounds = m.getBounds();
        if (bounds == null) {
            return null;
        }
        float h = bounds[4] - bounds[1];
        float scale = h <= 0 ? 1 : HEIGHT / h;
        return new Body(m, scale, -bounds[1] * scale);
    }
}
