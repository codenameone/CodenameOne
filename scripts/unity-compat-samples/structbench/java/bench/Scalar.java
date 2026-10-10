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
package bench;

/// The floor: no vector objects at all, four float arrays.
public final class Scalar {
    private Scalar() {
    }

    public static float run(int bodies, int frames) {
        float[] px = new float[bodies];
        float[] py = new float[bodies];
        float[] vx = new float[bodies];
        float[] vy = new float[bodies];
        for (int i = 0; i < bodies; i++) {
            px[i] = i * 0.5f;
            py[i] = 10f + (i % 7);
            vx[i] = (i % 5) - 2f;
            vy[i] = 0f;
        }
        float gx = 0f;
        float gy = -9.81f;
        float dt = 1f / 60f;
        for (int f = 0; f < frames; f++) {
            for (int i = 0; i < bodies; i++) {
                vx[i] = vx[i] + gx * dt;
                vy[i] = vy[i] + gy * dt;
                px[i] = px[i] + vx[i] * dt;
                py[i] = py[i] + vy[i] * dt;
                if (py[i] < 0f) {
                    py[i] = -py[i];
                    vy[i] = -vy[i] * 0.9f;
                }
            }
        }
        float sum = 0f;
        for (int i = 0; i < bodies; i++) {
            sum += px[i] + py[i] + (vx[i] * vx[i] + vy[i] * vy[i]) * 0.001f;
        }
        return sum;
    }
}
