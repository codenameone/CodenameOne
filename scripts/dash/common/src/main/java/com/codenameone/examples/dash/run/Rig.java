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
import com.codename1.gpu.Primitives;

/// A runner's body, built from capsules, rounded boxes and spheres and posed
/// by code: no model files, no animation clips. Each joint is an angle, the
/// pose is the angle set for what the runner is doing -- running, in the
/// air, sliding, down, or celebrating -- and the body eases towards it every
/// frame, so one move flows into the next.
///
/// The proportions are an athlete's: long legs, a chest that tapers to a
/// narrow waist, a small streamlined helmet with a visor that wraps round it,
/// trainers and a slim pack with a light strip. The suit is matte and the
/// trim glows -- a stripe down each leg and arm -- so the body reads as one
/// sleek shape at speed rather than a stack of glossy balls.
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
    private static final float UPPER_ARM = 0.31f;
    private static final float FOREARM = 0.29f;
    private static final float STAND = 1.02f;
    private static final float HEAD = 0.17f;

    /// The meshes every runner shares, built once per run.
    static final class Kit {
        final Mesh thigh = Meshes.capsule(0.075f, THIGH - 0.075f, 4, 12);
        final Mesh shin = Meshes.capsule(0.058f, SHIN - 0.058f, 4, 12);
        final Mesh arm = Meshes.capsule(0.05f, UPPER_ARM - 0.05f, 4, 10);
        final Mesh fore = Meshes.capsule(0.045f, FOREARM - 0.045f, 4, 10);
        final Mesh chest = Meshes.roundedBox(0.42f, 0.3f, 0.24f, 0.1f, 4);
        final Mesh torso = Meshes.capsule(0.13f, 0.14f, 6, 16);
        final Mesh pelvis = Meshes.roundedBox(0.3f, 0.16f, 0.2f, 0.07f, 3);
        final Mesh band = Meshes.torus(0.135f, 0.018f, 8, 20);
        final Mesh ball = Primitives.sphere(0.5f, 14, 20, false);
        final Mesh visor = Meshes.band(HEAD + 0.012f, 0.07f, 170f, 16);
        final Mesh pack = Meshes.roundedBox(0.24f, 0.28f, 0.08f, 0.035f, 3);
        final Mesh strip = Meshes.capsule(0.016f, 0.2f, 3, 8);
        final Mesh stripe = Meshes.capsule(0.012f, 0.3f, 3, 6);
        final Mesh shoe = Meshes.roundedBox(0.12f, 0.085f, 0.3f, 0.04f, 3);
        final Mesh sole = Meshes.roundedBox(0.13f, 0.03f, 0.31f, 0.014f, 2);
        final Mesh quad;
        final Material shadow;

        Kit(Mesh quad, Glow glow) {
            this.quad = quad;
            shadow = Glow.blended(glow.shadow, 0x99000000);
        }
    }

    private final Model pelvis;
    private final Model torso;
    private final Model chest;
    private final Model band;
    private final Model head;
    private final Model visor;
    private final Model pack;
    private final Model strip;
    private final Model[] thigh = new Model[2];
    private final Model[] shin = new Model[2];
    private final Model[] shoe = new Model[2];
    private final Model[] sole = new Model[2];
    private final Model[] arm = new Model[2];
    private final Model[] fore = new Model[2];
    private final Model[] hand = new Model[2];
    private final Model[] legStripe = new Model[2];
    private final Model[] armStripe = new Model[2];
    private final Model shadow;
    private final Model[] all;
    private final float[] pose = new float[JOINTS];
    private final float[] want = new float[JOINTS];
    private final float[] end = new float[2];
    private float phase;

    Rig(Kit k, Outfit o) {
        // A matte suit, so the light does not pool into highlights that make
        // every rounded part read as a balloon; the gloss is kept for the
        // helmet alone.
        Material suit = new Material(Material.Type.PHONG).setColor(o.suit).setShininess(8f);
        Material shell = new Material(Material.Type.PHONG).setColor(o.shell).setShininess(20f);
        Material helmet = new Material(Material.Type.PHONG).setColor(o.shell).setShininess(120f);
        Material kicks = new Material(Material.Type.PHONG).setColor(Outfit.mix(o.shell, 0xffffffff, 0.78f))
                .setShininess(70f);
        Material glow = new Material(Material.Type.UNLIT).setColor(o.trim);
        pelvis = part(k.pelvis, shell, 1f, 1f, 1f);
        torso = part(k.torso, suit, 1f, 1f, 0.8f);
        chest = part(k.chest, suit, 1f, 1f, 1f);
        band = part(k.band, glow, 1f, 1f, 0.8f);
        head = part(k.ball, helmet, HEAD * 2f, HEAD * 2f * 1.08f, HEAD * 2f * 1.12f);
        visor = part(k.visor, glow, 1f, 1f, 1f);
        pack = part(k.pack, shell, 1f, 1f, 1f);
        strip = part(k.strip, glow, 1f, 1f, 1f);
        for (int s = 0; s < 2; s++) {
            thigh[s] = part(k.thigh, suit, 1f, 1f, 1f);
            shin[s] = part(k.shin, suit, 1f, 1f, 1f);
            shoe[s] = part(k.shoe, kicks, 1f, 1f, 1f);
            sole[s] = part(k.sole, glow, 1f, 1f, 1f);
            arm[s] = part(k.arm, suit, 1f, 1f, 1f);
            fore[s] = part(k.fore, shell, 1f, 1f, 1f);
            hand[s] = part(k.ball, shell, 0.1f, 0.11f, 0.1f);
            legStripe[s] = part(k.stripe, glow, 1f, 1f, 1f);
            armStripe[s] = part(k.stripe, glow, 1f, 0.8f, 1f);
        }
        shadow = new Model(k.quad, k.shadow);
        shadow.setRotation(-90f, 0f, 0f);
        all = new Model[] {shadow, pelvis, torso, chest, band, pack, strip, head, visor, thigh[0], thigh[1],
            shin[0], shin[1], shoe[0], shoe[1], sole[0], sole[1], arm[0], arm[1], fore[0], fore[1],
            hand[0], hand[1], legStripe[0], legStripe[1], armStripe[0], armStripe[1]};
        running(0f);
        System.arraycopy(want, 0, pose, 0, JOINTS);
    }

    private static Model part(Mesh mesh, Material mat, float sx, float sy, float sz) {
        Model m = new Model(mesh, mat);
        m.setScale(sx, sy, sz);
        return m;
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
    private void place(float x, float y, float z) {
        float hipY = y + pose[HIP];
        float lean = pose[LEAN];
        float sinL = sin(lean);
        float cosL = cos(lean);
        put(pelvis, x, hipY, z, -lean);
        // Torso: from the hip, up and leaning forward.
        float midY = hipY + cosL * TORSO / 2f;
        float midF = sinL * TORSO / 2f;
        put(torso, x, midY - cosL * 0.06f, z - (midF - sinL * 0.06f), -lean);
        // The chest, broader than the waist under it.
        float chestUp = TORSO * 0.72f;
        put(chest, x, hipY + cosL * chestUp, z - sinL * chestUp, -lean);
        put(band, x, hipY + cosL * TORSO * 0.3f, z - sinL * TORSO * 0.3f, -lean);
        // The pack sits on the back, square to the torso.
        float back = 0.15f;
        midY = hipY + cosL * chestUp;
        midF = sinL * chestUp;
        put(pack, x, midY + sinL * back, z - (midF - cosL * back), -lean);
        float glowBack = back + 0.045f;
        put(strip, x, midY + sinL * glowBack, z - (midF - cosL * glowBack), -lean);
        float topY = hipY + cosL * TORSO;
        float topF = sinL * TORSO;
        float headY = topY + cosL * (HEAD - 0.02f);
        float headF = topF + sinL * (HEAD - 0.02f);
        put(head, x, headY, z - headF, -lean);
        put(visor, x, headY + cosL * 0.02f, z - (headF + sinL * 0.02f), -lean);
        float shY = topY - cosL * 0.06f;
        float shF = topF - sinL * 0.06f;
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? -1f : 1f;
            float lx = x + side * 0.1f;
            float ta = pose[s == 0 ? THIGH_L : THIGH_R];
            limb(thigh[s], lx, hipY, z, 0f, ta, THIGH);
            // The stripe down the outside of the thigh.
            put(legStripe[s], lx + side * 0.07f, (hipY + end[0]) / 2f, z - end[1] / 2f, ta);
            float kneeY = end[0];
            float kneeF = end[1];
            limb(shin[s], lx, kneeY, z, kneeF, pose[s == 0 ? SHIN_L : SHIN_R], SHIN);
            float sa = pose[s == 0 ? SHIN_L : SHIN_R];
            put(shoe[s], lx, end[0] - 0.02f, z - (end[1] + 0.06f), sa * 0.3f);
            put(sole[s], lx, end[0] - 0.07f, z - (end[1] + 0.06f), sa * 0.3f);
            float ax = x + side * 0.25f;
            float aa = pose[s == 0 ? ARM_L : ARM_R] + lean * 0.3f;
            limb(arm[s], ax, shY, z, shF, aa, UPPER_ARM);
            put(armStripe[s], ax + side * 0.048f, (shY + end[0]) / 2f, z - (shF + end[1]) / 2f, aa);
            limb(fore[s], ax, end[0], z, end[1], pose[s == 0 ? FORE_L : FORE_R] + lean * 0.3f, FOREARM);
            hand[s].setPosition(ax, end[0], z - end[1]);
        }
    }

    /// A limb hanging from `(y, f)` at angle `a`; places its capsule and
    /// leaves where it ends in `end`.
    private void limb(Model m, float x, float y, float z, float f, float a, float len) {
        float c = cos(a);
        float s = sin(a);
        put(m, x, y - c * len / 2f, z - (f + s * len / 2f), a);
        end[0] = y - c * len;
        end[1] = f + s * len;
    }

    private static void put(Model m, float x, float y, float z, float rotX) {
        m.setPosition(x, y, z);
        m.setRotation(rotX, 0f, 0f);
    }

    private static float sin(float deg) {
        return (float) Math.sin(Math.toRadians(deg));
    }

    private static float cos(float deg) {
        return (float) Math.cos(Math.toRadians(deg));
    }
}
