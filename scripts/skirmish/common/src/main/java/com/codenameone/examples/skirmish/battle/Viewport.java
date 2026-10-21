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
package com.codenameone.examples.skirmish.battle;

import com.codenameone.examples.skirmish.rules.Arena;

/// Maps the rules' milli-tiles to screen pixels and back.
///
/// Each player sees their own side at the bottom, so for seat 1 the arena is
/// turned half way round. The rules never know: only this class does.
public final class Viewport {
    private final int me;
    private final int width;
    private final int height;
    /// Pixels per 1000 milli-tiles, as a fraction to stay in integers.
    private final int tilePx;
    private final int left;
    private final int top;

    public Viewport(int me, int width, int height) {
        this.me = me;
        this.width = width;
        this.height = height;
        int byW = width * Arena.TILE / Arena.WIDTH;
        int byH = height * Arena.TILE / Arena.HEIGHT;
        this.tilePx = Math.max(1, Math.min(byW, byH));
        this.left = (width - tilePx * Arena.WIDTH / Arena.TILE) / 2;
        this.top = (height - tilePx * Arena.HEIGHT / Arena.TILE) / 2;
    }

    public int me() {
        return me;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int tilePx() {
        return tilePx;
    }

    /// Screen x of arena point (x, y).
    public int sx(int x, int y) {
        int ax = me == 0 ? x : Arena.WIDTH - x;
        return left + ax * tilePx / Arena.TILE;
    }

    /// Screen y of arena point (x, y).
    public int sy(int x, int y) {
        int ay = me == 0 ? y : Arena.HEIGHT - y;
        return top + ay * tilePx / Arena.TILE;
    }

    /// Arena x under screen point (px, py).
    public int ax(int px, int py) {
        int x = (px - left) * Arena.TILE / tilePx;
        return me == 0 ? x : Arena.WIDTH - x;
    }

    /// Arena y under screen point (px, py).
    public int ay(int px, int py) {
        int y = (py - top) * Arena.TILE / tilePx;
        return me == 0 ? y : Arena.HEIGHT - y;
    }

    /// Pixels for a length in milli-tiles.
    public int px(int milli) {
        return milli * tilePx / Arena.TILE;
    }
}
