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

/// Something that happened during a tick, for the renderer, the kill feed and the
/// sound. Events are output only: nothing in the rules reads them back, so a
/// client that drops one cannot desync.
public final class Event {
    /// A ball burst on a box or the fence. `x y z` is where; `a` is the shooter.
    public static final int SPLAT = 1;
    /// `a` painted `b`. The big red stamp.
    public static final int PAINTED = 2;
    /// `a` wiped the paint off and the referee did not notice.
    public static final int WIPE_OK = 3;
    /// `a` tried to wipe and the referee very much noticed.
    public static final int WIPE_CAUGHT = 4;
    /// `a` respawned.
    public static final int RESPAWN = 5;
    /// `a` fired a ball.
    public static final int SHOT = 6;
    /// `a` threw a grenade; it burst at `x y z`.
    public static final int GRENADE = 7;
    /// `a` picked up team `b`'s flag.
    public static final int FLAG_TAKEN = 8;
    /// Team `b`'s flag was dropped at `x y z`.
    public static final int FLAG_DROPPED = 9;
    /// Team `b`'s flag went home.
    public static final int FLAG_RETURNED = 10;
    /// `a` captured; team `b` scored.
    public static final int CAPTURE = 11;
    /// Team `b` scored a snack bar point.
    public static final int HILL_POINT = 12;
    /// `a` started reloading.
    public static final int RELOAD = 13;
    /// The match ended; `b` is the winning team or -1 for a draw.
    public static final int END = 14;
    /// `a` was hit by a teammate and loudly insists it does not count (it does not).
    public static final int FRIENDLY = 15;

    public final int kind;
    public final int tick;
    public final int a;
    public final int b;
    public final int x;
    public final int y;
    public final int z;

    public Event(int kind, int tick, int a, int b, int x, int y, int z) {
        this.kind = kind;
        this.tick = tick;
        this.a = a;
        this.b = b;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return kind + "@" + tick + "(" + a + "," + b + ")";
    }
}
