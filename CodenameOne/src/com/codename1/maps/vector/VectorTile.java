/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import java.util.List;

/// A decoded Mapbox Vector Tile: an ordered collection of named
/// [VectorLayer]s. Produced by [MvtDecoder] and consumed by [TileRenderer].
public final class VectorTile {

    private final List layers;

    VectorTile(List layers) {
        this.layers = layers;
    }

    /// All layers in declaration order.
    public List getLayers() {
        return layers;
    }

    /// The layer with the given name, or `null` if the tile has none.
    public VectorLayer getLayer(String name) {
        for (int i = 0; i < layers.size(); i++) {
            VectorLayer l = (VectorLayer) layers.get(i);
            if (l.getName().equals(name)) {
                return l;
            }
        }
        return null;
    }
}
