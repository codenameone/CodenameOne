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

import com.codename1.ui.FontImage;

/// The editing tools in the game builder's left rail, mirroring the GameForge design.
public enum Tool {
    /// Click to select an element and show its handles / inspector.
    SELECT("Select", 'V', FontImage.MATERIAL_NEAR_ME),
    /// Drag the selected element to a new position.
    MOVE("Move", 'M', FontImage.MATERIAL_OPEN_WITH),
    /// Paint tiles into cells, or stamp an actor at the click.
    BRUSH("Brush", 'B', FontImage.MATERIAL_BRUSH),
    /// Flood-fill a contiguous region of identical tiles.
    FILL("Fill", 'G', FontImage.MATERIAL_FORMAT_COLOR_FILL),
    /// Erase a tile or delete an element.
    ERASE("Erase", 'E', FontImage.MATERIAL_CLEAR),
    /// Sculpt the 3D terrain: raise/lower ground, carve holes, or build walls (3D only).
    TERRAIN("Terrain", 'T', FontImage.MATERIAL_TERRAIN),
    /// Pan / scroll the canvas.
    PAN("Pan", 'H', FontImage.MATERIAL_PAN_TOOL);

    private final String label;
    private final char shortcut;
    private final char icon;

    Tool(String label, char shortcut, char icon) {
        this.label = label;
        this.shortcut = shortcut;
        this.icon = icon;
    }

    public String label() {
        return label;
    }

    public char shortcut() {
        return shortcut;
    }

    public char icon() {
        return icon;
    }
}
