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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A themed collection of `AssetDef`s -- the editor's "Platformer", "Top-Down RPG" or
/// "Board & Card" pack. A `GameLevel` names the pack it draws from
/// (`GameLevel#getAssetPack()`), and an `AssetCatalog` indexes every pack so a placed
/// element's `GameElement#getAssetId()` resolves to its `AssetDef`.
public class AssetPack {
    private String id;
    private String name;
    private final Map<String, AssetDef> assets = new LinkedHashMap<String, AssetDef>();

    public AssetPack() {
    }

    public AssetPack(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public AssetPack setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public AssetPack setName(String name) {
        this.name = name;
        return this;
    }

    /// Adds an asset definition to the pack (replacing any with the same id).
    public AssetPack add(AssetDef def) {
        assets.put(def.getId(), def);
        return this;
    }

    public AssetDef get(String assetId) {
        return assets.get(assetId);
    }

    public boolean contains(String assetId) {
        return assets.containsKey(assetId);
    }

    /// The asset definitions in insertion order (the order they appear in a palette).
    public List<AssetDef> assets() {
        return new ArrayList<AssetDef>(assets.values());
    }

    public int size() {
        return assets.size();
    }

    /// Parses a pack from a parsed-JSON map (see `AssetCatalog#load(String)`).
    static AssetPack fromMap(Map<String, Object> m) {
        AssetPack pack = new AssetPack(Json.str(m.get("id"), null), Json.str(m.get("name"), null));
        List<Object> assetList = Json.asList(m.get("assets"));
        if (assetList != null) {
            for (Object assetEntry : assetList) {
                Map<String, Object> am = Json.asMap(assetEntry);
                if (am != null) {
                    pack.add(AssetDef.fromMap(am));
                }
            }
        }
        return pack;
    }
}
