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
package com.codenameone.examples.splat.play;

import com.codename1.gaming.AnimatedModel;
import com.codename1.util.MathUtil;

/// Puts a marker in an operator's right hand.
///
/// The marker is bolted to the hand bone, position and turn, the way a game
/// parents a weapon to a hand: arm and marker move as one through every clip,
/// and when a player is painted the marker goes down with the hand instead of
/// vanishing. A marker kept at a fixed carry point and aimed along the look
/// had the animated arm running up and down through a gun glued in the air.
///
/// The constants are measured from the `Aim` clip of the operators
/// `tools/make-operators.py` builds, where the marker points straight ahead.
/// Re-measure them after changing the rig.
final class Grip {
    /// From the grip to the end of a held marker's barrel, in metres.
    static final float MUZZLE_REACH = 0.36f;
    /// The hand bone's turn in the aim pose, column major.
    private static final float[] REST_HAND = {
        -0.9112f, 0.408f, -0.0569f,
        0.0217f, 0.1856f, 0.9824f,
        0.4114f, 0.8939f, -0.178f};
    /// The hand bone's position in the aim pose, model space, in metres.
    private static final float[] REST_AT = {-0.158f, 1.357f, 0.418f};
    /// The marker's turn in the hand's frame: in the aim pose it points
    /// straight ahead. Column major, the transpose of `REST_HAND`.
    private static final float[] GUN_IN_HAND = {
        0.9112f, -0.0217f, -0.4114f,
        0.408f, 0.1856f, 0.8939f,
        0.0569f, -0.9824f, 0.178f};
    /// The grip, in the hand's frame: in the palm, by the knuckles.
    private static final float[] GRIP_LOCAL = {-0.0025f, 0.0543f, -0.0057f};

    private final float[] hand = new float[16];
    private final float[] handTurn = new float[9];
    private final float[] gunTurn = new float[16];

    /// The marker's turn as the last `place` left it, column major 4x4; its
    /// third column is the barrel's back direction.
    float[] turn() {
        return gunTurn;
    }

    /// Places `gun` in the hand of `op`, drawn at `scale` with body turn
    /// `turn` (column major 4x4) at `ox, oy, oz`, and writes the end of its
    /// barrel into `muzzle` when that is not null.
    void place(AnimatedModel op, float s, float[] turn, float ox, float oy, float oz,
            AnimatedModel gun, float[] muzzle) {
        float[] w = hand;
        float wx;
        float wy;
        float wz;
        if (op.getNodeMatrix(Models.HAND, w)) {
            // the bone's rotation, without whatever scale the rig carries
            for (int c = 0; c < 3; c++) {
                float n = (float) Math.sqrt(w[c * 4] * w[c * 4] + w[c * 4 + 1] * w[c * 4 + 1]
                        + w[c * 4 + 2] * w[c * 4 + 2]);
                if (n > 1e-6f) {
                    w[c * 4] /= n;
                    w[c * 4 + 1] /= n;
                    w[c * 4 + 2] /= n;
                }
            }
            wx = w[12];
            wy = w[13];
            wz = w[14];
        } else {
            // no hand bone: hold it where the aim pose holds the hand
            for (int k = 0; k < 9; k++) {
                w[(k / 3) * 4 + k % 3] = REST_HAND[k];
            }
            wx = REST_AT[0];
            wy = REST_AT[1];
            wz = REST_AT[2];
        }
        // grip in model space: the hand plus the palm offset, in the hand's frame
        float lx = w[0] * GRIP_LOCAL[0] + w[4] * GRIP_LOCAL[1] + w[8] * GRIP_LOCAL[2];
        float ly = w[1] * GRIP_LOCAL[0] + w[5] * GRIP_LOCAL[1] + w[9] * GRIP_LOCAL[2];
        float lz = w[2] * GRIP_LOCAL[0] + w[6] * GRIP_LOCAL[1] + w[10] * GRIP_LOCAL[2];
        float hx = (wx + lx) * s;
        float hy = (wy + ly) * s;
        float hz = (wz + lz) * s;
        float gx = ox + turn[0] * hx + turn[4] * hy + turn[8] * hz;
        float gy = oy + turn[1] * hx + turn[5] * hy + turn[9] * hz;
        float gz = oz + turn[2] * hx + turn[6] * hy + turn[10] * hz;
        // marker turn = body * hand * (the hand's aim-pose turn undone)
        for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 3; r++) {
                float v = 0f;
                for (int k = 0; k < 3; k++) {
                    v += w[k * 4 + r] * GUN_IN_HAND[c * 3 + k];
                }
                handTurn[c * 3 + r] = v;
            }
        }
        float[] g = gunTurn;
        for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 3; r++) {
                g[c * 4 + r] = turn[r] * handTurn[c * 3] + turn[4 + r] * handTurn[c * 3 + 1]
                        + turn[8 + r] * handTurn[c * 3 + 2];
            }
            g[c * 4 + 3] = 0f;
        }
        g[12] = 0f;
        g[13] = 0f;
        g[14] = 0f;
        g[15] = 1f;
        setEuler(gun, g);
        gun.setPosition(gx, gy, gz);
        if (muzzle != null) {
            // the marker's barrel runs along its own -z, from the grip
            muzzle[0] = gx - g[8] * MUZZLE_REACH;
            muzzle[1] = gy - g[9] * MUZZLE_REACH;
            muzzle[2] = gz - g[10] * MUZZLE_REACH;
        }
    }

    private static void setEuler(AnimatedModel m, float[] r) {
        float r20 = r[2];
        float r21 = r[6];
        float r22 = r[10];
        float r10 = r[1];
        float r00 = r[0];
        if (r20 > 1f) {
            r20 = 1f;
        } else if (r20 < -1f) {
            r20 = -1f;
        }
        double ry = MathUtil.asin(-r20);
        double rx;
        double rz;
        if (Math.abs(r20) < 0.9999f) {
            rx = MathUtil.atan2(r21, r22);
            rz = MathUtil.atan2(r10, r00);
        } else {
            // gimbal lock: fold the whole turn into x
            rx = MathUtil.atan2(-r[9], r[5]);
            rz = 0;
        }
        m.setRotation((float) Math.toDegrees(rx), (float) Math.toDegrees(ry), (float) Math.toDegrees(rz));
    }
}
