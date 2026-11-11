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
package com.codenameone.examples.splat.rules;

/// A paintball (or a grenade) in flight. Balls are real projectiles that drop and
/// take time to arrive, which is what makes dodging behind a hay bale work and
/// lets the server judge a hit without trusting anybody's aim.
public final class Ball {
    int owner;
    int team;
    int x;
    int y;
    int z;
    int vx;
    int vy;
    int vz;
    int expires;
    boolean grenade;

    public int owner() {
        return owner;
    }

    public int team() {
        return team;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public int vx() {
        return vx;
    }

    public int vy() {
        return vy;
    }

    public int vz() {
        return vz;
    }

    public boolean grenade() {
        return grenade;
    }

    void write(IntBuf b) {
        b.add(owner).add(team).add(x).add(y).add(z).add(vx).add(vy).add(vz).add(expires).add(grenade);
    }

    static Ball read(IntBuf.Reader r) {
        Ball b = new Ball();
        b.owner = r.next();
        b.team = r.next();
        b.x = r.next();
        b.y = r.next();
        b.z = r.next();
        b.vx = r.next();
        b.vy = r.next();
        b.vz = r.next();
        b.expires = r.next();
        b.grenade = r.bool();
        return b;
    }
}
