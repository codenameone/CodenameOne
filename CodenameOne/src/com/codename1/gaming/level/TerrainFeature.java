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

/// A placed, variable-size terrain element such as a wall, ramp or platform. Unlike a per-cell
/// height/wall value, a feature has a free world position (in tile units) and an arbitrary
/// `width`x`height`x`depth` plus rotation, so dungeon walls, sloped roads and raised platforms
/// of any size are first-class, dynamic objects. The `#getType()` names the shape family
/// (`#TYPE_WALL`, `#TYPE_RAMP`, `#TYPE_PLATFORM`) and `#getMaterial()` its surface.
public class TerrainFeature {
    public static final String TYPE_WALL = "wall";
    public static final String TYPE_RAMP = "ramp";
    public static final String TYPE_PLATFORM = "platform";

    private String id;
    private String type = TYPE_WALL;
    private double x;
    private double y;
    private double z;
    private double width = 1;
    private double height = 1;
    private double depth = 1;
    private double rotation;
    private String material = MaterialRegistry.STONE;
    private final Map<String, Object> props = new HashMap<String, Object>();

    public TerrainFeature() {
    }

    public TerrainFeature(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public TerrainFeature setId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public TerrainFeature setType(String type) {
        this.type = type;
        return this;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    /// Sets the feature's world position in tile units (y = elevation).
    public TerrainFeature setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getDepth() {
        return depth;
    }

    /// Sets the feature size in tile units.
    public TerrainFeature setSize(double width, double height, double depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        return this;
    }

    public double getRotation() {
        return rotation;
    }

    public TerrainFeature setRotation(double rotation) {
        this.rotation = rotation;
        return this;
    }

    public String getMaterial() {
        return material;
    }

    public TerrainFeature setMaterial(String material) {
        this.material = material;
        return this;
    }

    public Map<String, Object> props() {
        return props;
    }

    void write(StringBuilder sb) {
        sb.append("{\"id\":");
        Json.writeString(sb, id);
        sb.append(",\"type\":");
        Json.writeString(sb, type);
        sb.append(",\"x\":");
        Json.writeNumber(sb, x);
        sb.append(",\"y\":");
        Json.writeNumber(sb, y);
        sb.append(",\"z\":");
        Json.writeNumber(sb, z);
        sb.append(",\"w\":");
        Json.writeNumber(sb, width);
        sb.append(",\"h\":");
        Json.writeNumber(sb, height);
        sb.append(",\"d\":");
        Json.writeNumber(sb, depth);
        sb.append(",\"rot\":");
        Json.writeNumber(sb, rotation);
        sb.append(",\"mat\":");
        Json.writeString(sb, material);
        if (!props.isEmpty()) {
            sb.append(",\"props\":");
            Json.writeValue(sb, props);
        }
        sb.append('}');
    }

    static TerrainFeature read(Map<String, Object> m) {
        TerrainFeature f = new TerrainFeature(Json.str(m.get("id"), null), Json.str(m.get("type"), TYPE_WALL));
        f.setPosition(Json.num(m.get("x"), 0), Json.num(m.get("y"), 0), Json.num(m.get("z"), 0));
        f.setSize(Json.num(m.get("w"), 1), Json.num(m.get("h"), 1), Json.num(m.get("d"), 1));
        f.rotation = Json.num(m.get("rot"), 0);
        f.material = Json.str(m.get("mat"), MaterialRegistry.STONE);
        Map<String, Object> p = Json.asMap(m.get("props"));
        if (p != null) {
            f.props.putAll(p);
        }
        return f;
    }
}
