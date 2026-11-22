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
package com.codenameone.examples.skirmish.rules;

/// Something that happened during the last tick, for the renderer and for sound.
///
/// Events are output only: nothing in the simulation reads them, so the client is
/// free to draw a projectile, a flash or a crown however it likes.
public final class Event {
    public static final int SPAWN = 1;
    public static final int ATTACK = 2;
    public static final int DEATH = 3;
    public static final int SPELL = 4;
    public static final int TOWER_DOWN = 5;
    public static final int REJECTED = 6;
    public static final int PLAYED = 7;

    public final int kind;
    /// The acting unit's id (attacker, spawned or dead unit), or the player.
    public final int source;
    /// The target unit's id, or -1.
    public final int target;
    public final int x;
    public final int y;
    public final int value;

    Event(int kind, int source, int target, int x, int y, int value) {
        this.kind = kind;
        this.source = source;
        this.target = target;
        this.x = x;
        this.y = y;
        this.value = value;
    }
}
