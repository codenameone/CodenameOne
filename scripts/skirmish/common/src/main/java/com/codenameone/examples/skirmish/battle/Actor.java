/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.skirmish.battle;

import com.codename1.gaming.AnimatedModel;
import com.codename1.gaming.Model;
import com.codename1.gaming.Sprite;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.util.MathUtil;
import com.codenameone.examples.skirmish.rules.CardDef;
import com.codenameone.examples.skirmish.rules.Unit;

/// One unit on the 3D field: its animated model, the team ring under its
/// feet and its health bar. It is told where the rules put the unit each tick
/// and what happened to it (an attack, a hit, a death), and chooses the clip
/// to play from that -- it never decides anything about the match.
final class Actor {
    private static final String CHEER = "Cheer";

    final int id;
    final AnimatedModel model;
    final Model ring;
    final Sprite barBack;
    final Sprite bar;
    final Models.Look look;
    final boolean structure;
    final boolean mine;
    final float height;
    final float scale;
    float fromX;
    float fromZ;
    float toX;
    float toZ;
    float x;
    float z;
    float yaw;
    float attackLeft;
    float flash;
    float dying = -1f;
    float bob;
    int seen;
    boolean placed;

    Actor(Unit u, boolean mine, AnimatedModel model, Models.Look look, Model ring, float footprint) {
        this.id = u.id();
        this.model = model;
        this.look = look;
        this.ring = ring;
        this.mine = mine;
        CardDef card = u.card();
        structure = card.isStructure();
        float[] b = bounds(model);
        float w = Math.max(b[3] - b[0], b[5] - b[2]);
        float h = b[4] - b[1];
        if (structure) {
            scale = footprint / Math.max(0.01f, w);
        } else {
            scale = (look == null ? 1.2f : look.size) / Math.max(0.01f, h);
        }
        height = h * scale;
        model.setScale(scale);
        if (look != null && look.tint != 0xffffffff) {
            model.setTint(look.tint);
        }
        String first = u.isDeploying() && clip("Skeletons_Awaken_Floor") != null
                ? "Skeletons_Awaken_Floor" : clip(idle());
        if (!structure && first != null) {
            model.play(first, !u.isDeploying());
        }
        barBack = new Sprite(Art.pixel());
        barBack.setColor(0xe0101418);
        barBack.setAnchor(0, 0.5);
        bar = new Sprite(Art.pixel());
        bar.setColor(mine ? 0xff4fa3ff : 0xffff5a4f);
        bar.setAnchor(0, 0.5);
        yaw = mine ? 180f : 0f;
    }

    /// The axis-aligned bounds of the model's rest pose:
    /// `minX, minY, minZ, maxX, maxY, maxZ`.
    static float[] bounds(AnimatedModel m) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
            -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        Model[] parts = m.getParts();
        for (int p = 0; p < parts.length; p++) {
            if (!parts[p].isVisible()) {
                continue;
            }
            float[] d = parts[p].getMesh().getVertices().getData();
            for (int i = 0; i + 2 < d.length; i += 8) {
                for (int k = 0; k < 3; k++) {
                    b[k] = Math.min(b[k], d[i + k]);
                    b[k + 3] = Math.max(b[k + 3], d[i + k]);
                }
            }
        }
        return b;
    }

    /// A new tick: the unit now stands at `(wx, wz)` in world units.
    void moveTo(float wx, float wz) {
        if (!placed) {
            fromX = wx;
            fromZ = wz;
            toX = wx;
            toZ = wz;
            x = wx;
            z = wz;
            placed = true;
            return;
        }
        fromX = toX;
        fromZ = toZ;
        toX = wx;
        toZ = wz;
    }

    /// The rules say this unit just struck toward `(tx, tz)`.
    void attacked(float tx, float tz) {
        face(tx - toX, tz - toZ);
        if (look != null && clip(look.attack) != null && !structure) {
            model.crossFade(look.attack, 0.06f, false);
            attackLeft = model.getScene().findAnimation(look.attack).getDuration() * 0.9f;
        }
    }

    void hit() {
        flash = 0.12f;
        model.setTint(0xffff8080);
    }

    void die() {
        dying = 0f;
        barBack.setVisible(false);
        bar.setVisible(false);
        ring.setVisible(false);
        if (!structure && clip(look == null ? "Death_A" : look.death) != null) {
            model.crossFade(clip(look == null ? "Death_A" : look.death), 0.08f, false);
        }
    }

    void cheer() {
        if (!structure && dying < 0f && clip(CHEER) != null) {
            model.crossFade(CHEER, 0.2f, true);
            attackLeft = 1e6f;
        }
    }

    private String idle() {
        return look == null ? "Idle" : look.idle;
    }

    /// `name` when the model has that clip, else null: an uploaded model has
    /// only the clips its author mapped, and may lack a cheer or a death.
    private String clip(String name) {
        return name != null && model.getScene().findAnimation(name) != null ? name : null;
    }

    private void face(float dx, float dz) {
        if (dx * dx + dz * dz > 1e-6f) {
            yaw = (float) Math.toDegrees(MathUtil.atan2(dx, dz));
        }
    }

    /// Places everything for a frame `alpha` of the way through the tick.
    void frame(Unit u, float alpha, float dt) {
        if (dying < 0f) {
            float nx = fromX + (toX - fromX) * alpha;
            float nz = fromZ + (toZ - fromZ) * alpha;
            boolean moving = Math.abs(toX - fromX) + Math.abs(toZ - fromZ) > 0.002f;
            if (moving && attackLeft <= 0f) {
                face(toX - fromX, toZ - fromZ);
            }
            x = nx;
            z = nz;
            if (!structure && u != null && !u.isDeploying()) {
                if (attackLeft > 0f) {
                    attackLeft -= dt;
                } else {
                    String c = clip(moving ? (look == null ? "Running_A" : look.run) : idle());
                    if (c != null) {
                        model.crossFade(c, 0.15f, true);
                    }
                    model.setSpeed(moving ? 0.6f + u.card().speed() / 60f : 1f);
                }
            }
        } else {
            dying += dt;
        }
        if (flash > 0f) {
            flash -= dt;
            if (flash <= 0f) {
                model.setTint(look != null ? look.tint : 0xffffffff);
            }
        }
        float y = 0f;
        if (look != null && look.hover > 0f) {
            bob += dt;
            y = look.hover + 0.15f * (float) Math.sin(bob * 3.0 + id);
        }
        if (dying > 0.9f) {
            y -= (dying - 0.9f) * 1.2f; // sink into the ground
        }
        model.setPosition(x, y, z);
        model.setRotation(0f, yaw, 0f);
        model.update(dt);
        ring.setPosition(x, 0.02f, z);
        if (u != null && dying < 0f) {
            boolean hurt = u.hp() < u.maxHp() || structure;
            barBack.setVisible(hurt);
            bar.setVisible(hurt);
            if (hurt) {
                float bw = structure ? 2.2f : 1.0f;
                float bh = structure ? 0.22f : 0.14f;
                float top = y + height + 0.35f;
                barBack.setSize(bw + 0.08f, bh + 0.08f);
                barBack.setPosition(x - bw / 2f - 0.04f, top, z);
                bar.setSize(Math.max(0.02f, bw * u.hp() / Math.max(1, u.maxHp())), bh);
                bar.setPosition(x - bw / 2f, top, z + 0.01f);
            }
        }
    }

    boolean gone() {
        return dying > 2.2f;
    }

    static Model ringModel(GraphicsDevice device, com.codename1.gpu.Mesh quad, com.codename1.gpu.Texture disc,
                           boolean mine, float radius) {
        com.codename1.gpu.Material m = new com.codename1.gpu.Material(com.codename1.gpu.Material.Type.UNLIT)
                .setTexture(disc).setColor(mine ? 0xc04fa3ff : 0xc0ff5a4f)
                .setRenderState(com.codename1.gpu.RenderState.transparent());
        Model ring = new Model(quad, m);
        ring.setRotation(-90f, 0f, 0f);
        ring.setScale(radius * 2f);
        return ring;
    }
}
