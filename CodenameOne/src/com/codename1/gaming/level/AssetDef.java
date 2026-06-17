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

/// The definition of one placeable asset in an `AssetPack`: the template a placed
/// `GameElement` references through its `GameElement#getAssetId()`.
///
/// It mirrors an entry in the editor's asset catalog -- an id, a display name, a
/// `#getKind()` (a `#KIND_TILE` painted into a grid cell vs a freely placed
/// `#KIND_ACTOR`), a natural pixel size, a base `#getColor()` (used to render a
/// placeholder until real art is transcoded in), whether the level may hold more than
/// one (`#isUnique()`), the `#getSource()` art reference, and the default authoring
/// properties copied onto a freshly placed element.
public class AssetDef {
    /// Painted into a grid cell of a `Layer#KIND_TILE` layer.
    public static final int KIND_TILE = 0;
    /// A freely placed entity / model.
    public static final int KIND_ACTOR = 1;

    private String id;
    private String name;
    private int kind = KIND_ACTOR;
    private int width = 32;
    private int height = 32;
    private int color = 0xff888888;
    private boolean unique;
    /// reference to the source art (e.g. a resource path or an svg/lottie key); the
    /// build-time pipeline turns this into a real image, runtime resolves it.
    private String source;

    private final Map<String, Object> defaultProperties = new HashMap<String, Object>();

    public AssetDef() {
    }

    public AssetDef(String id, int kind, int color, int width, int height) {
        this.id = id;
        this.name = id;
        this.kind = kind;
        this.color = color;
        this.width = width;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    public AssetDef setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public AssetDef setName(String name) {
        this.name = name;
        return this;
    }

    public int getKind() {
        return kind;
    }

    public AssetDef setKind(int kind) {
        this.kind = kind;
        return this;
    }

    public boolean isTile() {
        return kind == KIND_TILE;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public AssetDef setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public int getColor() {
        return color;
    }

    public AssetDef setColor(int color) {
        this.color = color;
        return this;
    }

    public boolean isUnique() {
        return unique;
    }

    public AssetDef setUnique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public String getSource() {
        return source;
    }

    public AssetDef setSource(String source) {
        this.source = source;
        return this;
    }

    /// The default authoring properties copied onto a newly placed element of this
    /// asset (a coin's `value`, a player's `lives`, ...).
    public Map<String, Object> defaultProperties() {
        return defaultProperties;
    }

    public AssetDef putDefault(String key, Object value) {
        defaultProperties.put(key, value);
        return this;
    }

    /// Parses an asset definition from a parsed-JSON map (see `AssetCatalog#load(String)`).
    static AssetDef fromMap(Map<String, Object> m) {
        AssetDef d = new AssetDef();
        d.id = Json.str(m.get("id"), null);
        d.name = Json.str(m.get("name"), d.id);
        d.kind = "tile".equalsIgnoreCase(Json.str(m.get("kind"), "actor")) ? KIND_TILE : KIND_ACTOR;
        d.width = Json.intval(m.get("w"), 32);
        d.height = Json.intval(m.get("h"), 32);
        d.color = Json.color(m.get("color"), 0xff888888);
        d.unique = Json.bool(m.get("unique"), false);
        d.source = Json.str(m.get("source"), null);
        Map<String, Object> defs = Json.asMap(m.get("defaults"));
        if (defs != null) {
            d.defaultProperties.putAll(defs);
        }
        return d;
    }
}
