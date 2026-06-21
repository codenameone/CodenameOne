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
package com.codename1.gaming.level;

import java.util.HashMap;
import java.util.Map;

/// A pluggable surface material used by terrain cells and `TerrainFeature`s. A material is
/// referenced everywhere by its string `#getId()` and resolved through the `MaterialRegistry`,
/// so applications can register their own materials (sand, lava, ice, ...) without changing the
/// level format. Carries authoring + light-runtime hints (base colour, whether it blocks
/// movement, friction) plus an optional `#getArtId()` for textured rendering.
public class Material {
    private String id;
    private String name;
    private int color = 0x808080;
    private boolean solid;
    private double friction = 1.0;
    private String artId;
    private final Map<String, Object> props = new HashMap<String, Object>();

    public Material() {
    }

    public Material(String id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public Material setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Material setName(String name) {
        this.name = name;
        return this;
    }

    /// Base RGB colour (0xRRGGBB) used to render the material before art is applied.
    public int getColor() {
        return color;
    }

    public Material setColor(int color) {
        this.color = color;
        return this;
    }

    /// Whether walkers are blocked by this material (e.g. water, lava).
    public boolean isSolid() {
        return solid;
    }

    public Material setSolid(boolean solid) {
        this.solid = solid;
        return this;
    }

    /// Movement friction multiplier (1 = normal, <1 = slippery like ice).
    public double getFriction() {
        return friction;
    }

    public Material setFriction(double friction) {
        this.friction = friction;
        return this;
    }

    /// Optional asset id supplying the material's texture art (resolved via `AssetCatalog`).
    public String getArtId() {
        return artId;
    }

    public Material setArtId(String artId) {
        this.artId = artId;
        return this;
    }

    /// Free-form authoring properties for app-specific material data.
    public Map<String, Object> props() {
        return props;
    }

    void write(StringBuilder sb) {
        sb.append('{');
        sb.append("\"id\":");
        Json.writeString(sb, id);
        sb.append(",\"name\":");
        Json.writeString(sb, name);
        sb.append(",\"color\":");
        Json.writeNumber(sb, color);
        sb.append(",\"solid\":").append(solid);
        sb.append(",\"friction\":");
        Json.writeNumber(sb, friction);
        if (artId != null) {
            sb.append(",\"artId\":");
            Json.writeString(sb, artId);
        }
        if (!props.isEmpty()) {
            sb.append(",\"props\":");
            Json.writeValue(sb, props);
        }
        sb.append('}');
    }

    static Material read(Map<String, Object> m) {
        Material mat = new Material(Json.str(m.get("id"), null), Json.str(m.get("name"), null),
                Json.intval(m.get("color"), 0x808080));
        mat.solid = Json.bool(m.get("solid"), false);
        mat.friction = Json.num(m.get("friction"), 1.0);
        mat.artId = Json.str(m.get("artId"), null);
        Map<String, Object> p = Json.asMap(m.get("props"));
        if (p != null) {
            mat.props.putAll(p);
        }
        return mat;
    }
}
