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

/// What a Java programmer writes when told that vectors are objects and
/// allocation costs: one object per body, updated in place.
public final class MutableVec2 {
    public float x;
    public float y;

    public MutableVec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static float run(int bodies, int frames) {
        MutableVec2[] pos = new MutableVec2[bodies];
        MutableVec2[] vel = new MutableVec2[bodies];
        for (int i = 0; i < bodies; i++) {
            pos[i] = new MutableVec2(i * 0.5f, 10f + (i % 7));
            vel[i] = new MutableVec2((i % 5) - 2f, 0f);
        }
        float gx = 0f;
        float gy = -9.81f;
        float dt = 1f / 60f;
        for (int f = 0; f < frames; f++) {
            for (int i = 0; i < bodies; i++) {
                MutableVec2 v = vel[i];
                MutableVec2 p = pos[i];
                v.x = v.x + gx * dt;
                v.y = v.y + gy * dt;
                p.x = p.x + v.x * dt;
                p.y = p.y + v.y * dt;
                if (p.y < 0f) {
                    p.y = -p.y;
                    v.y = -v.y * 0.9f;
                }
            }
        }
        float sum = 0f;
        for (int i = 0; i < bodies; i++) {
            sum += pos[i].x + pos[i].y + (vel[i].x * vel[i].x + vel[i].y * vel[i].y) * 0.001f;
        }
        return sum;
    }
}
