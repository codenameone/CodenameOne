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

/// A hand-written model of a lowering the translator does not emit yet: a
/// method that returns a struct takes the object to write its result into as
/// a hidden last argument, and the caller owns one scratch object per pending
/// result, allocated once on entry. It is here to be measured before anybody
/// builds it.
///
/// It is written the way the translator would have to write it without
/// knowing more: every result goes through a scratch object and is then
/// copied into its destination, so `v[i] = v[i] + g * dt` cannot be caught
/// reading an element it has half written.
public final class OutParam {
    public float x;
    public float y;

    public OutParam() {
    }

    public void $ctor(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void $assign(OutParam other) {
        x = other.x;
        y = other.y;
    }

    public static OutParam add(OutParam a, OutParam b, OutParam ret) {
        ret.$ctor(a.x + b.x, a.y + b.y);
        return ret;
    }

    public static OutParam mul(OutParam a, float s, OutParam ret) {
        ret.$ctor(a.x * s, a.y * s);
        return ret;
    }

    private static void step(OutParam[] p, OutParam[] v, OutParam gravity, float dt) {
        OutParam t0 = new OutParam();
        OutParam t1 = new OutParam();
        for (int i = 0; i < p.length; i++) {
            v[i].$assign(add(v[i], mul(gravity, dt, t0), t1));
            p[i].$assign(add(p[i], mul(v[i], dt, t0), t1));
            if (p[i].y < 0f) {
                p[i].y = -p[i].y;
                v[i].y = -v[i].y * 0.9f;
            }
        }
    }

    public static float run(int bodies, int frames) {
        OutParam[] pos = new OutParam[bodies];
        OutParam[] vel = new OutParam[bodies];
        for (int i = 0; i < bodies; i++) {
            pos[i] = new OutParam();
            pos[i].$ctor(i * 0.5f, 10f + (i % 7));
            vel[i] = new OutParam();
            vel[i].$ctor((i % 5) - 2f, 0f);
        }
        OutParam gravity = new OutParam();
        gravity.$ctor(0f, -9.81f);
        float dt = 1f / 60f;
        for (int f = 0; f < frames; f++) {
            step(pos, vel, gravity, dt);
        }
        float sum = 0f;
        for (int i = 0; i < bodies; i++) {
            sum += pos[i].x + pos[i].y + (vel[i].x * vel[i].x + vel[i].y * vel[i].y) * 0.001f;
        }
        return sum;
    }
}
