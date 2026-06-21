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

/// A single placed thing in a `GameLevel` -- pure authoring data, deliberately *not*
/// a `com.codename1.gaming.Sprite`.
///
/// An element only describes *what* (an `#getAssetId()` resolved through an
/// `AssetCatalog`), *where* (a position / rotation / scale transform), which `Layer`
/// it belongs to, and a bag of typed authoring `#properties()` (a coin's `value`, a
/// patrolling enemy's `speed`, a player's `lives`). What it *becomes* at runtime
/// depends on the level's `GameLevel#getMode()`: a `LevelRealizer` turns a 2D / board
/// element into a `com.codename1.gaming.Sprite` and a 3D element into a
/// `com.codename1.gaming.Model`. The realized object keeps a reference back to its
/// element through `setUserData`, so an editor can map a picked sprite/model to the
/// data it came from.
///
/// The property bag stores values as they survive JSON: `Double` for numbers, `String`
/// for text, `Boolean` for flags. The typed getters coerce defensively so callers do
/// not care whether a number arrived as a `Double`, a `Long` or a numeric `String`.
public class GameElement {
    private String id;
    private String name;
    private String assetId;
    private String layer;

    private double x;
    private double y;
    private double z;
    private float rotation;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private float scaleZ = 1f;

    private final Map<String, Object> properties = new HashMap<String, Object>();

    /// Lazily-cached resolution of `#getAssetId()` through an `AssetCatalog` (see
    /// `#resolveDef(AssetCatalog)`). Transient: it never serializes, and `#setAssetId` clears
    /// it. The stored data stays the catalog-independent id; this is only a per-element cache so
    /// per-frame code does not re-hit the catalog's map.
    private transient AssetDef resolvedDef;

    public GameElement() {
    }

    public GameElement(String id, String assetId) {
        this.id = id;
        this.assetId = assetId;
    }

    public String getId() {
        return id;
    }

    /// Sets this element's stable id (unique within a level; the editor assigns one).
    public GameElement setId(String id) {
        this.id = id;
        return this;
    }

    /// The optional display name typed in the editor's Inspector. A *named* element becomes a
    /// generated field in the companion class; may be `null`.
    public String getName() {
        return name;
    }

    /// Sets the display name (see `#getName()`).
    public GameElement setName(String name) {
        this.name = name;
        return this;
    }

    /// The catalog id of the asset this element draws (e.g. `"player"`, `"coin"`).
    public String getAssetId() {
        return assetId;
    }

    /// Points this element at a different catalog asset (and clears the cached `#resolveDef`).
    public GameElement setAssetId(String assetId) {
        this.assetId = assetId;
        this.resolvedDef = null;   // invalidate the cached resolution
        return this;
    }

    /// The `AssetDef` this element's `#getAssetId()` resolves to in the given catalog, cached
    /// after the first lookup. The element stays pure, catalog-independent data (only the id is
    /// stored and serialized); this just spares per-frame callers a repeated catalog-map lookup.
    /// Returns `null` for a null catalog or an unknown id. The cache is cleared by `#setAssetId`;
    /// if you re-register an asset under the same id, build a fresh element or set the id again.
    public AssetDef resolveDef(AssetCatalog catalog) {
        if (resolvedDef == null && catalog != null) {
            resolvedDef = catalog.def(assetId);
        }
        return resolvedDef;
    }

    /// The name of the `Layer` this element belongs to.
    public String getLayer() {
        return layer;
    }

    /// Moves this element to the named `Layer`.
    public GameElement setLayer(String layer) {
        this.layer = layer;
        return this;
    }

    /// The x position -- pixels in a 2D / board level, a world-space coordinate in 3D.
    public double getX() {
        return x;
    }

    /// The y position -- pixels in a 2D / board level, a world-space coordinate in 3D.
    public double getY() {
        return y;
    }

    /// The world-space depth, meaningful only in `GameLevel.Mode#THREE_D`.
    public double getZ() {
        return z;
    }

    /// Sets the x and y position (see `#getX()`).
    public GameElement setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /// Sets the x, y and z position (z is the 3D depth).
    public GameElement setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /// Sets the x position.
    public GameElement setX(double x) {
        this.x = x;
        return this;
    }

    /// Sets the y position.
    public GameElement setY(double y) {
        this.y = y;
        return this;
    }

    /// Sets the z (3D) depth.
    public GameElement setZ(double z) {
        this.z = z;
        return this;
    }

    /// The rotation in degrees (clockwise in 2D, applied about the model's axes in 3D).
    public float getRotation() {
        return rotation;
    }

    /// Sets the rotation in degrees (see `#getRotation()`).
    public GameElement setRotation(float rotation) {
        this.rotation = rotation;
        return this;
    }

    /// The per-instance x scale (`1` = the asset's natural size).
    public float getScaleX() {
        return scaleX;
    }

    /// The per-instance y scale (`1` = the asset's natural size).
    public float getScaleY() {
        return scaleY;
    }

    /// The per-instance z scale, used in 3D (`1` = the asset's natural size).
    public float getScaleZ() {
        return scaleZ;
    }

    /// Sets a uniform scale on all three axes.
    public GameElement setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
        this.scaleZ = scale;
        return this;
    }

    /// Sets the per-axis scale (z applies in 3D).
    public GameElement setScale(float scaleX, float scaleY, float scaleZ) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        return this;
    }

    /// The mutable authoring property bag. Values are `Double` / `String` / `Boolean`
    /// (or nested `Map` / `List` if you store structured data). Prefer the typed
    /// getters for reading.
    public Map<String, Object> properties() {
        return properties;
    }

    /// Writes this element as a JSON object (same shape `GameLevel` uses) into `sb`. Package
    /// visible so `Region`/`GameLevel` share one element format.
    void write(StringBuilder sb) {
        sb.append("{\"id\":");
        Json.writeString(sb, getId() == null ? "" : getId());
        sb.append(",\"assetId\":");
        Json.writeString(sb, getAssetId() == null ? "" : getAssetId());
        if (getName() != null) {
            sb.append(",\"name\":");
            Json.writeString(sb, getName());
        }
        if (getLayer() != null) {
            sb.append(",\"layer\":");
            Json.writeString(sb, getLayer());
        }
        sb.append(",\"x\":");
        Json.writeNumber(sb, getX());
        sb.append(",\"y\":");
        Json.writeNumber(sb, getY());
        if (getZ() != 0) {
            sb.append(",\"z\":");
            Json.writeNumber(sb, getZ());
        }
        if (getRotation() != 0) {
            sb.append(",\"rotation\":");
            Json.writeNumber(sb, getRotation());
        }
        if (getScaleX() != 1 || getScaleY() != 1 || getScaleZ() != 1) {
            sb.append(",\"scaleX\":");
            Json.writeNumber(sb, getScaleX());
            sb.append(",\"scaleY\":");
            Json.writeNumber(sb, getScaleY());
            sb.append(",\"scaleZ\":");
            Json.writeNumber(sb, getScaleZ());
        }
        if (!properties().isEmpty()) {
            sb.append(",\"props\":");
            Json.writeValue(sb, properties());
        }
        sb.append('}');
    }

    /// Reconstructs an element from a parsed JSON object (the shape `#write(StringBuilder)`
    /// emits).
    static GameElement read(Map<String, Object> em) {
        GameElement el = new GameElement(Json.str(em.get("id"), null), Json.str(em.get("assetId"), null));
        el.setName(Json.str(em.get("name"), null));
        el.setLayer(Json.str(em.get("layer"), null));
        el.setPosition(Json.num(em.get("x"), 0), Json.num(em.get("y"), 0), Json.num(em.get("z"), 0));
        el.setRotation((float) Json.num(em.get("rotation"), 0));
        el.setScale((float) Json.num(em.get("scaleX"), 1),
                (float) Json.num(em.get("scaleY"), 1),
                (float) Json.num(em.get("scaleZ"), 1));
        Map<String, Object> ep = Json.asMap(em.get("props"));
        if (ep != null) {
            el.properties().putAll(ep);
        }
        return el;
    }

    /// Whether an authoring property with the given key is set.
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /// Sets an authoring property (a coin's `value`, an enemy's `speed`, ...). Store
    /// `Double` / `String` / `Boolean` so the value survives a JSON round-trip.
    public GameElement setProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    /// The raw value of an authoring property, or `null` if unset. Prefer the typed getters.
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /// Reads a property as an int, coercing `Number` and numeric `String` values and
    /// falling back to `defaultValue` when the key is missing or not numeric.
    public int getInt(String key, int defaultValue) {
        return (int) Math.round(getDouble(key, defaultValue));
    }

    /// Reads a property as a double, coercing `Number` and numeric `String` values.
    public double getDouble(String key, double defaultValue) {
        Object v = properties.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ignore) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /// Reads a property as a String (any value's `toString()`), or `defaultValue` if absent.
    public String getString(String key, String defaultValue) {
        Object v = properties.get(key);
        return v == null ? defaultValue : v.toString();
    }

    /// Reads a property as a boolean. Accepts a `Boolean` or the strings `"true"`/`"1"`.
    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = properties.get(key);
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                return true;
            }
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                return false;
            }
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue() != 0;
        }
        return defaultValue;
    }
}
