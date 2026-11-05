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

/// A paintball marker (never "gun" -- the people at the field are very particular
/// about that). Pure data: a fork rebalances the game by editing [Catalog#MARKERS].
public final class MarkerDef {
    public final String id;
    public final String name;
    /// What the loadout screen says about it, with a perfectly straight face.
    public final String blurb;
    /// Ticks between shots.
    public final int cooldown;
    /// Muzzle speed in millimetres per tick.
    public final int speed;
    /// Largest random deviation of a shot, in binary angle units ([Trig#FULL] per turn).
    public final int spread;
    /// Balls per hopper.
    public final int hopper;
    /// Ticks a reload takes.
    public final int reload;
    /// Movement speed in percent while carrying it.
    public final int mobility;

    public MarkerDef(String id, String name, String blurb, int cooldown, int speed, int spread,
            int hopper, int reload, int mobility) {
        this.id = id;
        this.name = name;
        this.blurb = blurb;
        this.cooldown = cooldown;
        this.speed = speed;
        this.spread = spread;
        this.hopper = hopper;
        this.reload = reload;
        this.mobility = mobility;
    }
}
