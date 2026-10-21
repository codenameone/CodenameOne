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
// The way a Unity script writes vector arithmetic: a small mutable struct,
// operators that return a new value, and arrays of them updated every frame.
namespace Bench
{
    public struct Vec2
    {
        public float x;
        public float y;

        public Vec2(float x, float y)
        {
            this.x = x;
            this.y = y;
        }

        public static Vec2 operator +(Vec2 a, Vec2 b)
        {
            return new Vec2(a.x + b.x, a.y + b.y);
        }

        public static Vec2 operator *(Vec2 a, float s)
        {
            return new Vec2(a.x * s, a.y * s);
        }

        public float SqrMagnitude
        {
            get { return x * x + y * y; }
        }
    }

    public sealed class Integrator
    {
        private readonly Vec2[] pos;
        private readonly Vec2[] vel;
        private readonly Vec2 gravity = new Vec2(0f, -9.81f);

        public Integrator(int bodies)
        {
            pos = new Vec2[bodies];
            vel = new Vec2[bodies];
            for (int i = 0; i < bodies; i++)
            {
                pos[i] = new Vec2(i * 0.5f, 10f + (i % 7));
                vel[i] = new Vec2((i % 5) - 2f, 0f);
            }
        }

        public void Step(float dt)
        {
            Vec2[] p = pos;
            Vec2[] v = vel;
            for (int i = 0; i < p.Length; i++)
            {
                v[i] = v[i] + gravity * dt;
                p[i] = p[i] + v[i] * dt;
                if (p[i].y < 0f)
                {
                    p[i].y = -p[i].y;
                    v[i].y = -v[i].y * 0.9f;
                }
            }
        }

        public float Checksum()
        {
            float sum = 0f;
            for (int i = 0; i < pos.Length; i++)
            {
                sum += pos[i].x + pos[i].y + vel[i].SqrMagnitude * 0.001f;
            }
            return sum;
        }

        public static float Run(int bodies, int frames)
        {
            Integrator it = new Integrator(bodies);
            float dt = 1f / 60f;
            for (int f = 0; f < frames; f++)
            {
                it.Step(dt);
            }
            return it.Checksum();
        }
    }
}
