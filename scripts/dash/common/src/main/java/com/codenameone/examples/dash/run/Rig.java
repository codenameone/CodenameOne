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
package com.codenameone.examples.dash.run;

import com.codename1.gaming.GameView;
import com.codename1.gaming.Model;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;

/// A runner's body -- a sleek low-poly android athlete -- built in code
/// and posed by code: no model files, no animation clips. Each joint is an
/// angle, the pose is the angle set for what the runner is doing -- running,
/// in the air, sliding, down, or celebrating -- and the body eases towards
/// it every frame, so one move flows into the next.
///
/// Every segment is lofted from octagonal cross sections ([Meshes]'s `loft`)
/// with one normal per face, so the light breaks across it in machined
/// panels instead of sliding round a balloon. The torso is a real torso:
/// hips, a narrow waist, a chest that widens to the shoulders and a neck,
/// with a chest plate and a back pack over it. Arms and legs are tapered
/// upper and lower segments meeting at armoured elbows and knees, ending in
/// fists and trainers with soles; the head is a faceted helmet with a face
/// plate. Each segment is up to three meshes sharing one origin at its joint
/// -- the suit, the dark armour shell and the glowing trim -- so an [Outfit]
/// recolours the whole body.
///
/// Angles are in degrees in the runner's side plane: 0 hangs straight down
/// (a limb) or stands straight up (the torso), and a positive angle swings
/// the far end forward, towards the way the runner faces.
final class Rig {
    private static final int LEAN = 0;
    private static final int HIP = 1;
    private static final int THIGH_L = 2;
    private static final int SHIN_L = 3;
    private static final int THIGH_R = 4;
    private static final int SHIN_R = 5;
    private static final int ARM_L = 6;
    private static final int FORE_L = 7;
    private static final int ARM_R = 8;
    private static final int FORE_R = 9;
    private static final int JOINTS = 10;

    private static final float THIGH = 0.5f;
    private static final float SHIN = 0.48f;
    private static final float TORSO = 0.54f;
    private static final float UPPER_ARM = 0.3f;
    private static final float STAND = 1.02f;

    /// The meshes every runner shares, built once per run. Each has its
    /// origin at the joint it hangs from -- the torso at the hip, the head at
    /// the neck, a limb at its upper joint -- and runs along -Y (+Y for the
    /// torso and head), with forward at -z.
    static final class Kit {
        final Mesh torsoSuit;
        final Mesh torsoShell;
        final Mesh torsoGlow;
        final Mesh headShell;
        final Mesh headSuit;
        final Mesh headGlow;
        final Mesh armSuit;
        final Mesh armShell;
        final Mesh foreSuit;
        final Mesh foreShell;
        final Mesh foreGlow;
        final Mesh thighSuit;
        final Mesh thighShell;
        final Mesh thighGlow;
        final Mesh shinSuit;
        final Mesh shinShell;
        final Mesh shoe;
        final Mesh sole;
        final Mesh quad;
        final Material shadow;

        Kit(Mesh quad, Glow glow) {
            this.quad = quad;
            shadow = Glow.blended(glow.shadow, 0x99000000);
            // Hips, a waist narrower than both, the rib cage widening to
            // the shoulders, then the trapezius sloping into the neck.
            torsoSuit = Meshes.builder()
                    .loft(0f, 0f, f(-0.07f, 0.04f, 0.15f, 0.27f, 0.38f, 0.46f, 0.51f, 0.55f),
                            f(0.125f, 0.14f, 0.112f, 0.15f, 0.205f, 0.22f, 0.16f, 0.085f),
                            f(0.085f, 0.09f, 0.078f, 0.095f, 0.11f, 0.105f, 0.085f, 0.065f),
                            f(0f, 0f, 0f, -0.008f, -0.016f, -0.008f, 0.005f, 0.01f))
                    .loft(0f, 0.012f, f(0.53f, 0.64f), f(0.045f, 0.04f), f(0.045f, 0.04f))
                    .done();
            torsoShell = Meshes.builder()
                    // Hip guard, chest plate, back pack and shoulder caps.
                    .loft(0f, 0f, f(-0.09f, -0.06f, 0.07f, 0.1f), f(0.12f, 0.145f, 0.152f, 0.125f),
                            f(0.08f, 0.1f, 0.102f, 0.085f))
                    .loft(0f, -0.118f, f(0.29f, 0.33f, 0.44f, 0.47f), f(0.1f, 0.14f, 0.165f, 0.12f),
                            f(0.018f, 0.025f, 0.025f, 0.015f))
                    .loft(0f, 0.15f, f(0.2f, 0.23f, 0.46f, 0.49f), f(0.09f, 0.12f, 0.12f, 0.09f),
                            f(0.03f, 0.045f, 0.045f, 0.03f))
                    .loft(-0.225f, 0f, f(0.4f, 0.45f, 0.51f, 0.535f), f(0.055f, 0.074f, 0.072f, 0.045f),
                            f(0.065f, 0.082f, 0.077f, 0.05f))
                    .loft(0.225f, 0f, f(0.4f, 0.45f, 0.51f, 0.535f), f(0.055f, 0.074f, 0.072f, 0.045f),
                            f(0.065f, 0.082f, 0.077f, 0.05f))
                    .done();
            torsoGlow = Meshes.builder()
                    // The belt line, the seam under the rib cage, the light
                    // down the chest plate and the strip on the pack.
                    .loft(0f, 0f, f(0.0f, 0.025f), f(0.155f, 0.155f), f(0.104f, 0.104f))
                    .loft(0f, 0f, f(0.235f, 0.25f), f(0.142f, 0.144f), f(0.094f, 0.095f), f(-0.006f, -0.007f))
                    .loft(0f, -0.145f, f(0.31f, 0.45f), f(0.016f, 0.016f), f(0.006f, 0.006f))
                    .loft(0f, 0.198f, f(0.25f, 0.44f), f(0.022f, 0.022f), f(0.005f, 0.005f))
                    .done();
            // A faceted helmet: a narrow jaw, wide temples, a crown.
            headShell = Meshes.builder()
                    .loft(0f, -0.005f, f(0.02f, 0.06f, 0.15f, 0.22f, 0.27f, 0.295f),
                            f(0.06f, 0.088f, 0.1f, 0.098f, 0.08f, 0.04f),
                            f(0.07f, 0.1f, 0.115f, 0.112f, 0.09f, 0.045f),
                            f(-0.012f, -0.008f, 0f, 0.004f, 0.008f, 0.01f))
                    .done();
            headSuit = Meshes.builder()
                    // Ear pods and the chin guard.
                    .loft(-0.1f, 0.01f, f(0.09f, 0.11f, 0.17f, 0.19f), f(0.012f, 0.02f, 0.02f, 0.012f),
                            f(0.03f, 0.042f, 0.042f, 0.03f))
                    .loft(0.1f, 0.01f, f(0.09f, 0.11f, 0.17f, 0.19f), f(0.012f, 0.02f, 0.02f, 0.012f),
                            f(0.03f, 0.042f, 0.042f, 0.03f))
                    .loft(0f, -0.085f, f(0.025f, 0.05f, 0.085f), f(0.045f, 0.06f, 0.07f), f(0.018f, 0.025f, 0.02f))
                    .done();
            headGlow = Meshes.builder()
                    // The face plate, wrapping a little round the sides, and
                    // a crest along the crown.
                    .loft(0f, -0.105f, f(0.1f, 0.115f, 0.175f, 0.19f), f(0.07f, 0.092f, 0.09f, 0.072f),
                            f(0.015f, 0.026f, 0.026f, 0.015f), f(0.006f, 0f, 0f, 0.006f))
                    .loft(0f, 0f, f(0.25f, 0.305f), f(0.014f, 0.01f), f(0.1f, 0.06f), f(-0.005f, 0.01f))
                    .done();
            armSuit = Meshes.builder()
                    .loft(0f, 0f, f(0.02f, -0.05f, -0.2f, -0.29f), f(0.055f, 0.062f, 0.05f, 0.04f),
                            f(0.055f, 0.065f, 0.052f, 0.042f))
                    .done();
            armShell = Meshes.builder()
                    // The elbow guard.
                    .loft(0f, 0f, f(-0.27f, -0.295f, -0.33f), f(0.045f, 0.052f, 0.04f), f(0.045f, 0.055f, 0.045f))
                    .done();
            foreSuit = Meshes.builder()
                    .loft(0f, 0f, f(0f, -0.08f, -0.27f), f(0.042f, 0.046f, 0.032f), f(0.044f, 0.05f, 0.034f))
                    .done();
            foreShell = Meshes.builder()
                    // The bracer and the fist.
                    .loft(0f, 0f, f(-0.05f, -0.08f, -0.2f, -0.23f), f(0.044f, 0.052f, 0.045f, 0.036f),
                            f(0.048f, 0.056f, 0.048f, 0.038f))
                    .loft(0f, -0.005f, f(-0.255f, -0.28f, -0.33f, -0.36f), f(0.028f, 0.034f, 0.034f, 0.026f),
                            f(0.032f, 0.045f, 0.046f, 0.034f))
                    .done();
            foreGlow = Meshes.builder()
                    .loft(0f, 0f, f(-0.12f, -0.14f), f(0.053f, 0.052f), f(0.058f, 0.057f))
                    .done();
            thighSuit = Meshes.builder()
                    .loft(0f, 0f, f(0.04f, -0.08f, -0.3f, -0.45f, -0.52f), f(0.08f, 0.09f, 0.075f, 0.058f, 0.05f),
                            f(0.085f, 0.098f, 0.085f, 0.062f, 0.055f), f(0f, -0.006f, -0.004f, 0f, 0f))
                    .done();
            thighShell = Meshes.builder()
                    // The knee guard.
                    .loft(0f, -0.045f, f(-0.43f, -0.46f, -0.53f, -0.56f), f(0.045f, 0.06f, 0.058f, 0.04f),
                            f(0.02f, 0.032f, 0.032f, 0.018f))
                    .done();
            thighGlow = Meshes.builder()
                    .loft(0f, 0f, f(-0.06f, -0.34f), f(0.014f, 0.012f), f(0.008f, 0.008f), f(-0.1f, -0.088f))
                    .done();
            shinSuit = Meshes.builder()
                    .loft(0f, 0f, f(0f, -0.13f, -0.3f, -0.42f, -0.48f), f(0.052f, 0.06f, 0.048f, 0.036f, 0.034f),
                            f(0.056f, 0.07f, 0.055f, 0.04f, 0.038f), f(0f, 0.01f, 0.006f, 0f, 0f))
                    .done();
            shinShell = Meshes.builder()
                    // The shin guard down the front.
                    .loft(0f, 0f, f(-0.05f, -0.09f, -0.33f, -0.38f), f(0.035f, 0.048f, 0.04f, 0.03f),
                            f(0.012f, 0.018f, 0.016f, 0.01f), f(-0.058f, -0.066f, -0.05f, -0.042f))
                    .done();
            // A trainer from the ankle forwards: a low toe, a raised heel
            // collar, and a sole that runs a little proud of the upper.
            shoe = Meshes.builder()
                    .loft(0f, 0f, f(-0.075f, -0.035f, 0.005f, 0.045f), f(0.056f, 0.058f, 0.05f, 0.04f),
                            f(0.15f, 0.148f, 0.105f, 0.055f), f(-0.065f, -0.065f, -0.035f, 0f))
                    .done();
            sole = Meshes.builder()
                    .loft(0f, 0f, f(-0.1f, -0.075f), f(0.062f, 0.062f), f(0.16f, 0.16f), f(-0.066f, -0.066f))
                    .done();
        }

        private static float[] f(float... v) {
            return v;
        }
    }

    private final Model[] torso = new Model[3];
    private final Model[] head = new Model[3];
    private final Model[][] arm = new Model[2][2];
    private final Model[][] fore = new Model[2][3];
    private final Model[][] thigh = new Model[2][3];
    private final Model[][] shin = new Model[2][2];
    private final Model[][] foot = new Model[2][2];
    private final Model shadow;
    private final Model[] all;
    private final float[] pose = new float[JOINTS];
    private final float[] want = new float[JOINTS];
    private final float[] end = new float[2];
    private int count;
    private float phase;

    Rig(Kit k, Outfit o) {
        // A matte suit and satin armour, flat shaded, so each panel takes
        // the light as one tone; the gloss is kept for the helmet.
        Material suit = new Material(Material.Type.PHONG).setColor(o.suit).setShininess(10f);
        Material shell = new Material(Material.Type.PHONG).setColor(o.shell).setShininess(40f);
        Material helmet = new Material(Material.Type.PHONG).setColor(Outfit.mix(o.shell, o.suit, 0.3f))
                .setShininess(90f);
        Material kicks = new Material(Material.Type.PHONG).setColor(Outfit.mix(o.shell, 0xffffffff, 0.8f))
                .setShininess(50f);
        Material glow = new Material(Material.Type.UNLIT).setColor(o.trim);
        torso[0] = new Model(k.torsoSuit, suit);
        torso[1] = new Model(k.torsoShell, shell);
        torso[2] = new Model(k.torsoGlow, glow);
        head[0] = new Model(k.headShell, helmet);
        head[1] = new Model(k.headSuit, suit);
        head[2] = new Model(k.headGlow, glow);
        for (int s = 0; s < 2; s++) {
            arm[s][0] = new Model(k.armSuit, suit);
            arm[s][1] = new Model(k.armShell, shell);
            fore[s][0] = new Model(k.foreSuit, suit);
            fore[s][1] = new Model(k.foreShell, shell);
            fore[s][2] = new Model(k.foreGlow, glow);
            thigh[s][0] = new Model(k.thighSuit, suit);
            thigh[s][1] = new Model(k.thighShell, shell);
            thigh[s][2] = new Model(k.thighGlow, glow);
            shin[s][0] = new Model(k.shinSuit, suit);
            shin[s][1] = new Model(k.shinShell, shell);
            foot[s][0] = new Model(k.shoe, kicks);
            foot[s][1] = new Model(k.sole, glow);
        }
        shadow = new Model(k.quad, k.shadow);
        shadow.setRotation(-90f, 0f, 0f);
        all = new Model[1 + 6 + 2 * 12];
        all[count++] = shadow;
        add(torso);
        add(head);
        for (int s = 0; s < 2; s++) {
            add(arm[s]);
            add(fore[s]);
            add(thigh[s]);
            add(shin[s]);
            add(foot[s]);
        }
        running(0f);
        System.arraycopy(want, 0, pose, 0, JOINTS);
    }

    private void add(Model[] parts) {
        for (int i = 0; i < parts.length; i++) {
            all[count++] = parts[i];
        }
    }

    void addTo(GameView view) {
        for (int i = 0; i < all.length; i++) {
            view.addModel(all[i]);
        }
    }

    void setVisible(boolean v) {
        for (int i = 0; i < all.length; i++) {
            all[i].setVisible(v);
        }
    }


    /// Poses the body standing at `x, y, z` (feet on the ground plane at
    /// `y`), facing down the track, for one frame of `dt` seconds.
    void pose(float x, float y, float z, boolean alive, boolean finished, boolean air, boolean slide,
            float speed, float dt) {
        phase += dt * (5f + 6f * Math.min(1.6f, speed));
        if (!alive) {
            down();
        } else if (finished) {
            cheer();
        } else if (slide) {
            sliding();
        } else if (air) {
            airborne();
        } else {
            running(phase);
        }
        float k = Math.min(1f, dt * (alive ? 14f : 6f));
        for (int i = 0; i < JOINTS; i++) {
            pose[i] += (want[i] - pose[i]) * k;
        }
        place(x, y, z);
        float lift = Math.max(0f, y);
        float s = Math.max(0.35f, 0.9f - lift * 0.18f);
        shadow.setScale(s * 1.05f, s * 1.3f, 1f);
        shadow.setPosition(x, 0.03f, z);
    }

    private void running(float ph) {
        float sin = (float) Math.sin(ph);
        float cos = (float) Math.cos(ph);
        set(14f, STAND - 0.05f + Math.abs(cos) * 0.06f);
        legs(40f * sin, 22f + 58f * (0.5f - 0.5f * sin), -40f * sin, 22f + 58f * (0.5f + 0.5f * sin));
        arms(-48f * sin, 80f, 48f * sin, 80f);
    }

    private void airborne() {
        set(18f, STAND - 0.12f);
        legs(75f, 95f, 25f, 70f);
        arms(130f, 20f, -40f, 60f);
    }

    private void sliding() {
        set(-62f, 0.32f);
        legs(82f, 0f, 70f, -10f);
        arms(-50f, -20f, -60f, -30f);
    }

    private void down() {
        set(84f, 0.16f);
        legs(-80f, 0f, -96f, -10f);
        arms(165f, 0f, 150f, -10f);
    }

    private void cheer() {
        set(-4f, STAND);
        legs(6f, 6f, -6f, -6f);
        arms(170f, 0f, 160f, -10f);
    }

    private void set(float lean, float hip) {
        want[LEAN] = lean;
        want[HIP] = hip;
    }

    /// Thigh angle and knee bend for each leg.
    private void legs(float tl, float bl, float tr, float br) {
        want[THIGH_L] = tl;
        want[SHIN_L] = tl - bl;
        want[THIGH_R] = tr;
        want[SHIN_R] = tr - br;
    }

    /// Upper arm angle and elbow bend for each arm.
    private void arms(float al, float el, float ar, float er) {
        want[ARM_L] = al;
        want[FORE_L] = al + el;
        want[ARM_R] = ar;
        want[FORE_R] = ar + er;
    }

    // The body is solved in the runner's side plane -- height and distance
    // forward -- and turned into world space at the end: forward is -z.
    // Every part hangs from its joint, so placing one is a position and a
    // pitch.
    private void place(float x, float y, float z) {
        float hipY = y + pose[HIP];
        float lean = pose[LEAN];
        float sinL = sin(lean);
        float cosL = cos(lean);
        put(torso, x, hipY, z, -lean);
        // The head rides on the neck and lifts against the lean, so the
        // face plate looks down the track.
        float neck = TORSO + 0.05f;
        put(head, x, hipY + cosL * neck, z - sinL * neck, -lean * 0.45f);
        float sh = TORSO - 0.065f;
        float shY = hipY + cosL * sh;
        float shF = sinL * sh;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1f : 1f;
            float lx = x + side * 0.095f;
            float ta = pose[s == 0 ? THIGH_L : THIGH_R];
            put(thigh[s], lx, hipY, z, ta);
            reach(hipY, 0f, ta, THIGH);
            float sa = pose[s == 0 ? SHIN_L : SHIN_R];
            float kneeY = end[0];
            float kneeF = end[1];
            put(shin[s], lx, kneeY, z - kneeF, sa);
            reach(kneeY, kneeF, sa, SHIN);
            put(foot[s], lx, end[0], z - end[1], sa * 0.3f);
            float ax = x + side * 0.235f;
            float aa = pose[s == 0 ? ARM_L : ARM_R] + lean * 0.3f;
            put(arm[s], ax, shY, z - shF, aa);
            reach(shY, shF, aa, UPPER_ARM);
            put(fore[s], ax, end[0], z - end[1], pose[s == 0 ? FORE_L : FORE_R] + lean * 0.3f);
        }
    }

    /// Where a limb hanging from `(y, f)` at angle `a` ends, left in `end`.
    private void reach(float y, float f, float a, float len) {
        end[0] = y - cos(a) * len;
        end[1] = f + sin(a) * len;
    }

    private static void put(Model[] parts, float x, float y, float z, float rotX) {
        for (int i = 0; i < parts.length; i++) {
            parts[i].setPosition(x, y, z);
            parts[i].setRotation(rotX, 0f, 0f);
        }
    }

    private static float sin(float deg) {
        return (float) Math.sin(Math.toRadians(deg));
    }

    private static float cos(float deg) {
        return (float) Math.cos(Math.toRadians(deg));
    }
}
