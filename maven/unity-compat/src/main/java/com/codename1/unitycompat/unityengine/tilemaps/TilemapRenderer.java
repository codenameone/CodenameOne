/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.unitycompat.unityengine.tilemaps;

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.DrawView;
import com.codename1.unitycompat.unityengine.Renderer;
import com.codename1.unitycompat.unityengine.TilemapBase;

/// `UnityEngine.Tilemaps.TilemapRenderer`: draws the tilemap beside it.
///
/// Every tile on the screen is one sprite in the frame's draw list, all
/// with the renderer's sorting layer and order; the tiles off the screen
/// cost nothing. The renderer's `mode` and `sortOrder`, which in Unity
/// choose between one batch and a sprite a tile and the order of tiles
/// among themselves, are not read: tiles are drawn bottom row first.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class TilemapRenderer extends Renderer {
    private TilemapBase map;

    @Override
    public Component $new() {
        return new TilemapRenderer();
    }

    @Override
    public int $roles() {
        return DRAWS;
    }

    @Override
    public void $draw(DrawView view) {
        if (!get_enabled()) {
            return;
        }
        if (map == null) {
            Object m = get_gameObject().GetComponent(TilemapBase.class);
            if (!(m instanceof TilemapBase)) {
                return;
            }
            map = (TilemapBase) m;
        }
        map.$draw(view, get_sortingOrder(), get_sortingLayerID());
    }
}
