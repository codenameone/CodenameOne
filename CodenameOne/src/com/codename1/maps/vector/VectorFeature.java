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
import java.util.Map;

/// A single decoded feature of a Mapbox Vector Tile: its geometry type, its
/// attribute map, and its geometry as one or more parts in tile-local
/// coordinates (0..[VectorLayer#getExtent], origin top-left).
///
/// For [#GEOM_POINT] each part is a flat run of `x,y` points; for
/// [#GEOM_LINESTRING] each part is a polyline; for [#GEOM_POLYGON] each part
/// is a closed ring (exterior rings wind one way, holes the other, per the
/// MVT spec).
public final class VectorFeature {

    /// Unknown / empty geometry.
    public static final int GEOM_UNKNOWN = 0;
    /// One or more points.
    public static final int GEOM_POINT = 1;
    /// One or more polylines.
    public static final int GEOM_LINESTRING = 2;
    /// One or more polygon rings.
    public static final int GEOM_POLYGON = 3;

    private final long id;
    private final int geometryType;
    private final Map attributes;
    private final List parts;

    VectorFeature(long id, int geometryType, Map attributes, List parts) {
        this.id = id;
        this.geometryType = geometryType;
        this.attributes = attributes;
        this.parts = parts;
    }

    /// The feature id (0 when absent).
    public long getId() {
        return id;
    }

    /// One of the `GEOM_*` constants.
    public int getGeometryType() {
        return geometryType;
    }

    /// The feature attributes as a `Map<String,Object>` (string/number/bool).
    public Map getAttributes() {
        return attributes;
    }

    /// Convenience accessor for a single attribute, or `null`.
    public Object getAttribute(String key) {
        return attributes == null ? null : attributes.get(key);
    }

    /// The geometry parts, each an `int[]` of interleaved `x,y` tile-local
    /// coordinates.
    public List getParts() {
        return parts;
    }
}
