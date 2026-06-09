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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// A z-ordered collection of sprites with an optional camera offset.
///
/// A `Scene` is the model a `SpriteRenderer` draws: it holds the sprites, keeps
/// them sorted by `Sprite#getZOrder()` (higher draws on top, re-sorted only when
/// the contents change) and applies a camera offset that scrolls the whole scene.
/// `#update(double)` advances every sprite (driving `AnimatedSprite` playback).
public class Scene {
    private final List sprites = new ArrayList();
    private boolean sortDirty;
    private int cameraX;
    private int cameraY;

    private static final Comparator Z_ORDER = new ZComparator();

    private static final class ZComparator implements Comparator {
        @Override
        public int compare(Object a, Object b) {
            int za = ((Sprite) a).getZOrder();
            int zb = ((Sprite) b).getZOrder();
            return za < zb ? -1 : (za > zb ? 1 : 0);
        }
    }

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

    /// Advances every sprite in the scene by calling `Sprite#onUpdate(double)`. Do
    /// not add or remove sprites from within `onUpdate`; defer that to the next frame.
    public void update(double deltaSeconds) {
        for (Object sprite : sprites) {
            ((Sprite) sprite).onUpdate(deltaSeconds);
        }
    }

    /// Sorts the sprites by z-order if the contents changed. Called by the renderer
    /// before drawing.
    void ensureSorted() {
        if (sortDirty) {
            Collections.sort(sprites, Z_ORDER);
            sortDirty = false;
        }
    }

    /// Forces a re-sort on the next frame. Call this after changing a sprite's
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
