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
import com.codename1.unitycompat.unityengine.Sprite;

/// `UnityEngine.Tilemaps.Tile`: a tile that shows one sprite.
///
/// A scene's tilemap is filled from its own tables rather than from these
/// objects, so changing one after the scene is built changes only the
/// cells a script sets with it afterwards.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Tile extends TileBase {
    private Sprite sprite;
    private final Color color = new Color();
    private int colliderType = 1;

    public Tile() {
        color.r = 1f;
        color.g = 1f;
        color.b = 1f;
        color.a = 1f;
    }

    /// What a tile asset's file sets: its sprite, its tint, and how it
    /// collides -- 0 not at all, 1 as its sprite, 2 as its whole cell.
    public Tile $setup(String name, Sprite shown, float r, float g, float b, float a, int collides) {
        set_name(name);
        sprite = shown;
        color.r = r;
        color.g = g;
        color.b = b;
        color.a = a;
        colliderType = collides;
        return this;
    }

    public Sprite get_sprite() {
        return sprite;
    }

    public void set_sprite(Sprite value) {
        sprite = value;
    }

    public Color get_color(Color ret) {
        ret.$assign(color);
        return ret;
    }

    public void set_color(Color value) {
        color.$assign(value);
    }

    /// How the tile collides, by the numbers Unity's
    /// `Tile.ColliderType` has.
    public int $colliderType() {
        return colliderType;
    }
}
