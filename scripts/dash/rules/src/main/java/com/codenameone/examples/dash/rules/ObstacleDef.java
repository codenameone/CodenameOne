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
package com.codenameone.examples.dash.rules;

/// A kind of obstacle. Content, not code: the editor creates these from data and
/// a fork re-skins one by giving its id a different model or sprite.
///
/// A non-lethal obstacle makes the runner stumble -- lose a third of their speed
/// -- and two stumbles within [Runner#STUMBLE_WINDOW] ticks end the run.
public final class ObstacleDef {
    public final String id;
    public final Clearance clearance;
    /// Length along the track, in millimetres.
    public final int depth;
    /// For [Clearance#JUMP]: how high the runner's feet must be, in millimetres.
    public final int height;
    public final boolean lethal;

    public ObstacleDef(String id, Clearance clearance, int depth, int height, boolean lethal) {
        this.id = id;
        this.clearance = clearance;
        this.depth = depth < 1 ? 1 : depth;
        this.height = height < 0 ? 0 : height;
        this.lethal = lethal;
    }
}
