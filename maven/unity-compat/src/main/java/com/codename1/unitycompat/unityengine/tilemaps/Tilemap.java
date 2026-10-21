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

import UnityEngine.Color;
import UnityEngine.Vector3Int;
import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.TilemapBase;

/// `UnityEngine.Tilemaps.Tilemap`: tiles in the cells of a grid.
///
/// The storage, the drawing and the cell arithmetic are [TilemapBase]'s;
/// this adds the members that name a tile asset.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Tilemap extends TilemapBase {
    private final Color scratch = new Color();

    @Override
    public Component $new() {
        return new Tilemap();
    }

    public TileBase GetTile(Vector3Int position) {
        Object t = $tileAt(position.x, position.y);
        return t instanceof TileBase ? (TileBase) t : null;
    }

    /// `GetTile<T>`: the tile, when it is of the class asked for.
    public TileBase GetTile(Vector3Int position, Class type) {
        TileBase t = GetTile(position);
        return t != null && type.isInstance(t) ? t : null;
    }

    /// Puts a tile in a cell, or with null empties it. The cell shows the
    /// tile as the asset describes it: its sprite, its tint and its
    /// collider, not turned or moved.
    public void SetTile(Vector3Int position, TileBase tile) {
        if (tile == null) {
            $set(position.x, position.y, -1);
            return;
        }
        int v = $variantOf(tile);
        if (v < 0) {
            if (tile instanceof Tile) {
                Tile t = (Tile) tile;
                t.get_color(scratch);
                v = $variant(tile, t.get_sprite(), t.$colliderType(), 1f, 1f, 0f, 0f, 0f, scratch.r, scratch.g,
                        scratch.b, scratch.a);
            } else {
                v = $variant(tile, null, 0, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f);
            }
        }
        $set(position.x, position.y, v);
    }
}
