/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gaming;

import com.codename1.ui.geom.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// A flipped sprite still has to collide. `setScale(-1, 1)` is how the guide
/// turns a character around, and `getBounds()` used the signed scale directly,
/// so the box came out with a negative width -- which every `Rectangle`
/// intersection test rejects. Pure (no Display, no rasterizer).
class SpriteBoundsTest {

    private static Sprite sized(float w, float h, double x, double y) {
        Sprite s = new Sprite();
        s.setSize(w, h);
        s.setPosition(x, y);
        return s;
    }

    @Test
    void flippedSpriteKeepsAPositiveBox() {
        Sprite s = sized(40, 60, 100, 200);
        s.setScale(-1, 1);
        Rectangle b = s.getBounds();
        assertTrue(b.getWidth() > 0, "width must stay positive when flipped");
        assertTrue(b.getHeight() > 0, "height must stay positive when flipped");
        assertEquals(40, b.getWidth());
        assertEquals(60, b.getHeight());
    }

    @Test
    void flippingDoesNotMoveTheBoxForACentredAnchor() {
        // The default anchor is centred, so mirroring about it leaves the box put.
        Sprite facing = sized(40, 60, 100, 200);
        Sprite flipped = sized(40, 60, 100, 200);
        flipped.setScale(-1, 1);
        assertEquals(facing.getBounds().getX(), flipped.getBounds().getX());
        assertEquals(facing.getBounds().getY(), flipped.getBounds().getY());
    }

    @Test
    void flippedSpritesStillIntersect() {
        Sprite hero = sized(40, 60, 100, 200);
        hero.setScale(-1, 1);
        Sprite coin = sized(20, 20, 105, 205);
        assertTrue(hero.intersects(coin), "a left-facing hero must still collect");
        assertTrue(coin.intersects(hero));
    }

    @Test
    void anchorReflectsSoTheBoxTracksTheDrawnQuad() {
        // Anchored at its left edge: unflipped the box starts at x, flipped it
        // ends there, because the renderer mirrors the quad about the anchor.
        Sprite s = sized(40, 60, 100, 200);
        s.setAnchor(0, 0.5);
        assertEquals(100, s.getBounds().getX());
        s.setScale(-1, 1);
        assertEquals(60, s.getBounds().getX());
    }
}
