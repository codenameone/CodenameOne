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

/// One piece of cover: an axis-aligned box standing on the ground.
///
/// Every solid thing on a field is one of these -- an inflatable bunker, a hay
/// bale, the porta-potty, the minivan somebody's dad parked on the field. The
/// `kind` is only a name for the renderer to pick a model by; the rules treat all
/// boxes alike. Coordinates are millimetres, `y` is up.
public final class Box {
    public final String kind;
    public final int x0;
    public final int z0;
    public final int x1;
    public final int z1;
    public final int height;

    public Box(String kind, int x0, int z0, int x1, int z1, int height) {
        this.kind = kind;
        this.x0 = Math.min(x0, x1);
        this.z0 = Math.min(z0, z1);
        this.x1 = Math.max(x0, x1);
        this.z1 = Math.max(z0, z1);
        this.height = height;
    }

    public boolean contains(int x, int y, int z) {
        return x >= x0 && x <= x1 && z >= z0 && z <= z1 && y >= 0 && y <= height;
    }

    /// True when a circle of `radius` at `(x, z)` overlaps the footprint.
    public boolean overlaps(int x, int z, int radius) {
        return x + radius > x0 && x - radius < x1 && z + radius > z0 && z - radius < z1;
    }

    void hash(Hash h) {
        h.add(kind).add(x0).add(z0).add(x1).add(z1).add(height);
    }
}
