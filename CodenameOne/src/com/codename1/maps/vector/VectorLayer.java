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

/// A named layer of a decoded vector tile (for example `water`, `road`,
/// `building`, `place`) holding its features and the integer extent of its
/// local coordinate grid (4096 by default).
public final class VectorLayer {

    private final String name;
    private final int extent;
    private final List features;

    VectorLayer(String name, int extent, List features) {
        this.name = name;
        this.extent = extent;
        this.features = features;
    }

    /// The layer name as authored in the tileset (used to match style rules).
    public String getName() {
        return name;
    }

    /// The tile-local coordinate extent; geometry ranges over `0..extent`.
    public int getExtent() {
        return extent;
    }

    /// The features in this layer ([VectorFeature]).
    public List getFeatures() {
        return features;
    }
}
