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
/// `#getKind()` (a `Kind#TILE` painted into a grid cell vs a freely placed
/// `Kind#ACTOR`), a natural pixel size, a base `#getColor()` (used to render a
/// placeholder until the real art loads), whether the level may hold more than one
/// (`#isUnique()`), the default authoring properties copied onto a freshly placed
/// element, and -- the part that makes it a real asset -- a `#getType()` and a
/// `#getSource()` art file:
///
/// - `Type#IMAGE` -- a single static image (`source` is a PNG/JPG path), realized as a
///   `com.codename1.gaming.Sprite`.
/// - `Type#SHEET` -- a sprite sheet: an image of equal frames in a grid
///   (`#getFrameWidth()` x `#getFrameHeight()`, `#getFps()`), realized as an
///   animated `com.codename1.gaming.AnimatedSprite`.
/// - `Type#MESH` -- a 3D mesh (`source` is a glTF/glb path), realized as a
///   `com.codename1.gaming.Model` in a 3D level.
public class AssetDef {
    /// How an asset is placed: painted into a grid cell, or freely positioned.
    public enum Kind {
        /// Painted into a grid cell of a `Layer.Kind#TILE` layer.
        TILE("tile"),
        /// A freely placed entity / model.
        ACTOR("actor");

        private final String wire;

        Kind(String wire) {
            this.wire = wire;
        }

        /// The lowercase JSON token for this kind (`"tile"` / `"actor"`).
        public String wireName() {
            return wire;
        }

        /// The kind for a JSON token (case-insensitive); defaults to `#ACTOR`.
        public static Kind fromWire(String name) {
            return "tile".equalsIgnoreCase(name) ? TILE : ACTOR;
        }
    }

    /// The art format of an asset, which decides what it is realized into.
    public enum Type {
        /// A single static image (`source` is a PNG/JPG), realized as a `com.codename1.gaming.Sprite`.
        IMAGE("image"),
        /// A sprite sheet (a grid of equal frames) played as an animated `com.codename1.gaming.AnimatedSprite`.
        SHEET("sheet"),
        /// A 3D mesh (glTF/glb), realized as a `com.codename1.gaming.Model`.
        MESH("mesh");

        private final String wire;

        Type(String wire) {
            this.wire = wire;
        }

        /// The lowercase JSON token for this format (`"image"` / `"sheet"` / `"mesh"`).
        public String wireName() {
            return wire;
        }

        /// The format for an explicit JSON token (case-insensitive); `null`/unknown is `#IMAGE`.
        public static Type fromWire(String name) {
            if ("sheet".equalsIgnoreCase(name)) {
                return SHEET;
            }
            if ("mesh".equalsIgnoreCase(name)) {
                return MESH;
            }
            return IMAGE;
        }
    }

    private String id;
    private String name;
    private Kind kind = Kind.ACTOR;
    private int width = 32;
    private int height = 32;
    private int color = 0xff888888;
    private boolean unique;
    /// The art format -- `Type#IMAGE`, `Type#SHEET` or `Type#MESH`.
    private Type type = Type.IMAGE;
    /// The art file: a PNG/JPG (image, sheet) or a glTF/glb (mesh), resolved from
    /// bundled resources or the project's `games/assets/` folder at load time.
    private String source;
    /// sprite-sheet frame size in pixels (0 = a single frame the size of the image).
    private int frameWidth;
    private int frameHeight;
    /// sprite-sheet frame count (0 = every frame in the sheet) and playback rate.
    private int frameCount;
    private double fps = 12;

    private final Map<String, Object> defaultProperties = new HashMap<String, Object>();

    public AssetDef() {
    }

    public AssetDef(String id, Kind kind, int color, int width, int height) {
        this.id = id;
        this.name = id;
        this.kind = kind == null ? Kind.ACTOR : kind;
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

    /// How this asset is placed (`Kind#TILE` vs `Kind#ACTOR`).
    public Kind getKind() {
        return kind;
    }

    public AssetDef setKind(Kind kind) {
        this.kind = kind == null ? Kind.ACTOR : kind;
        return this;
    }

    public boolean isTile() {
        return kind == Kind.TILE;
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

    /// The art format: `Type#IMAGE`, `Type#SHEET` or `Type#MESH`.
    public Type getType() {
        return type;
    }

    public AssetDef setType(Type type) {
        this.type = type == null ? Type.IMAGE : type;
        return this;
    }

    public boolean isSheet() {
        return type == Type.SHEET;
    }

    public boolean isMesh() {
        return type == Type.MESH;
    }

    /// Sprite-sheet frame width in pixels (0 = the whole image is one frame).
    public int getFrameWidth() {
        return frameWidth;
    }

    /// Sprite-sheet frame height in pixels (0 = the whole image is one frame).
    public int getFrameHeight() {
        return frameHeight;
    }

    /// Number of frames to play (0 = every frame in the sheet).
    public int getFrameCount() {
        return frameCount;
    }

    /// Sprite-sheet playback rate in frames per second.
    public double getFps() {
        return fps;
    }

    /// Marks this asset as a sprite sheet with the given frame grid and rate.
    public AssetDef setSheet(int frameWidth, int frameHeight, int frameCount, double fps) {
        this.type = Type.SHEET;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;
        this.fps = fps;
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
        d.kind = Kind.fromWire(Json.str(m.get("kind"), "actor"));
        d.width = Json.intval(m.get("w"), 32);
        d.height = Json.intval(m.get("h"), 32);
        d.color = Json.color(m.get("color"), 0xff888888);
        d.unique = Json.bool(m.get("unique"), false);
        d.source = Json.str(m.get("source"), null);
        d.frameWidth = Json.intval(m.get("frameW"), 0);
        d.frameHeight = Json.intval(m.get("frameH"), 0);
        d.frameCount = Json.intval(m.get("frames"), 0);
        d.fps = Json.num(m.get("fps"), 12);
        d.type = typeFromName(Json.str(m.get("type"), null), d.source, d.frameWidth);
        Map<String, Object> defs = Json.asMap(m.get("defaults"));
        if (defs != null) {
            d.defaultProperties.putAll(defs);
        }
        return d;
    }

    /// Resolves the art format from an explicit {@code "type"} key, else infers it: a
    /// {@code .glb}/{@code .gltf} source is a mesh, a frame width means a sheet, else an
    /// image.
    static Type typeFromName(String type, String source, int frameWidth) {
        if (type != null) {
            return Type.fromWire(type);
        }
        if (source != null) {
            String s = source.toLowerCase();
            if (s.endsWith(".glb") || s.endsWith(".gltf")) {
                return Type.MESH;
            }
        }
        return frameWidth > 0 ? Type.SHEET : Type.IMAGE;
    }
}
