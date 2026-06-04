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

import com.codename1.ui.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// A z-ordered collection of sprites with an optional camera offset.
///
/// A typical `GameView` keeps a `Scene`, calls `#update(double)` from its update
/// loop and `#render(com.codename1.ui.Graphics)` from its render method. Sprites
/// are drawn from lowest to highest `Sprite#getZOrder()`, so higher z-order sprites
/// appear on top. The list is only re-sorted when it changes, not every frame.
///
/// The camera offset (`#setCamera(int, int)`) is subtracted from every sprite's
/// position while rendering, which scrolls the whole scene.
public class Scene {
    private final List sprites = new ArrayList();
    private boolean sortDirty;
    private int cameraX;
    private int cameraY;

    private static final Comparator Z_ORDER = new Comparator() {
        public int compare(Object a, Object b) {
            int za = ((Sprite) a).getZOrder();
            int zb = ((Sprite) b).getZOrder();
            return za < zb ? -1 : (za > zb ? 1 : 0);
        }
    };

    /// Adds a sprite to the scene.
    public void add(Sprite s) {
        sprites.add(s);
        sortDirty = true;
    }

    /// Removes a sprite from the scene.
    public void remove(Sprite s) {
        sprites.remove(s);
    }

    /// Removes all sprites.
    public void clear() {
        sprites.clear();
    }

    public int size() {
        return sprites.size();
    }

    public Sprite get(int index) {
        return (Sprite) sprites.get(index);
    }

    /// Advances every sprite in the scene by calling `Sprite#onUpdate(double)`.
    /// Iterating by index tolerates a sprite removing itself during update.
    public void update(double deltaSeconds) {
        for (int i = 0; i < sprites.size(); i++) {
            ((Sprite) sprites.get(i)).onUpdate(deltaSeconds);
        }
    }

    /// Renders every visible sprite in z-order, applying the camera offset.
    public void render(Graphics g) {
        if (sortDirty) {
            Collections.sort(sprites, Z_ORDER);
            sortDirty = false;
        }
        boolean cam = cameraX != 0 || cameraY != 0;
        if (cam) {
            g.translate(-cameraX, -cameraY);
        }
        for (int i = 0; i < sprites.size(); i++) {
            ((Sprite) sprites.get(i)).draw(g);
        }
        if (cam) {
            g.translate(cameraX, cameraY);
        }
    }

    /// Forces a re-sort on the next render. Call this after changing a sprite's
    /// z-order so the new ordering takes effect.
    public void markSortDirty() {
        sortDirty = true;
    }

    public int getCameraX() {
        return cameraX;
    }

    public int getCameraY() {
        return cameraY;
    }

    /// Sets the camera offset subtracted from sprite positions while rendering.
    public void setCamera(int x, int y) {
        this.cameraX = x;
        this.cameraY = y;
    }
}
