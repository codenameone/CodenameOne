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
package com.codename1.gamebuilder.editor;

/// What the Terrain tool does to the cell under the pointer. Stored on the editor model so
/// the rail/inspector pick it and the canvas applies it; see EditorController#paintTerrain.
public enum TerrainBrush {
    /// Raise the cell's ground elevation by one step (and fill a hole if there was one).
    RAISE("Raise"),
    /// Lower the cell's ground elevation by one step.
    LOWER("Lower"),
    /// Toggle whether the cell has a floor at all (carve / fill an open hole).
    GROUND("Ground on/off"),
    /// Toggle a full-height wall on the cell (dungeon rooms / mazes).
    WALL("Wall on/off"),
    /// Paint the selected surface material (grass / road / stone / …) onto the cell.
    PAINT("Paint surface");

    private final String label;

    TerrainBrush(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
