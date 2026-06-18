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

import com.codename1.io.JSONParser;
import com.codename1.ui.Image;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The registry that turns an `GameElement#getAssetId()` into something a
/// `LevelRealizer` can draw.
///
/// It indexes one or more `AssetPack`s and answers two questions: `#def(String)`
/// (the `AssetDef` template for an id, used for size / default props / kind) and
/// `#image(String)` (the artwork). Real art is supplied with `#setImage(String, Image)`
/// -- the game builder draws procedural art for the starter packs and decodes any images
/// you import, then wires them in here. Until an image is supplied `#image(String)`
/// returns a cached solid-color placeholder sized from the def (the same flat-color look
/// the editor uses while authoring), so a level always realizes to something visible.
public class AssetCatalog {
    private final Map<String, AssetPack> packs = new LinkedHashMap<String, AssetPack>();
    /// flat assetId -> def index across all packs (first pack wins on a clash).
    private final Map<String, AssetDef> byId = new HashMap<String, AssetDef>();
    private final Map<String, Image> images = new HashMap<String, Image>();
    private final Map<String, Image> placeholders = new HashMap<String, Image>();

    /// Parses one or more `AssetPack`s from a JSON document of the form
    /// `{"packs":[{"id":..,"name":..,"assets":[{"id":..,"kind":"tile|actor","w":..,
    /// "h":..,"color":"#rrggbb","unique":false,"source":..,"defaults":{..}}]}]}` and
    /// adds them to a new catalog. The same format the editor ships its starter packs in.
    public static AssetCatalog load(String json) throws IOException {
        return fromMap(JSONParser.parseJSON(json));
    }

    /// Parses packs from a UTF-8 JSON stream and closes it.
    public static AssetCatalog load(InputStream in) throws IOException {
        Reader r = new InputStreamReader(in, "UTF-8");
        try {
            return fromMap(new JSONParser().parseJSON(r));
        } finally {
            try {
                r.close();
            } catch (IOException ignore) { //NOPMD - closing the reader is best-effort
            }
        }
    }

    static AssetCatalog fromMap(Map<String, Object> root) {
        AssetCatalog catalog = new AssetCatalog();
        if (root == null) {
            return catalog;
        }
        List<Object> packList = Json.asList(root.get("packs"));
        if (packList != null) {
            for (Object packEntry : packList) {
                Map<String, Object> pm = Json.asMap(packEntry);
                if (pm != null) {
                    catalog.addPack(AssetPack.fromMap(pm));
                }
            }
        }
        return catalog;
    }

    public AssetCatalog addPack(AssetPack pack) {
        packs.put(pack.getId(), pack);
        List<AssetDef> defs = pack.assets();
        for (AssetDef d : defs) {
            if (!byId.containsKey(d.getId())) {
                byId.put(d.getId(), d);
            }
        }
        return this;
    }

    public AssetPack getPack(String id) {
        return packs.get(id);
    }

    public List<AssetPack> packs() {
        return new ArrayList<AssetPack>(packs.values());
    }

    /// The definition for an asset id, or null if no pack defines it.
    public AssetDef def(String assetId) {
        return byId.get(assetId);
    }

    /// Supplies real artwork for an asset id, overriding the placeholder.
    public AssetCatalog setImage(String assetId, Image image) {
        images.put(assetId, image);
        return this;
    }

    public boolean hasImage(String assetId) {
        return images.containsKey(assetId);
    }

    /// Resolves the image for an asset id: the explicit art if one was supplied,
    /// otherwise a cached solid-color placeholder sized from the def. Returns null
    /// only when the id is unknown to every pack and no image was set.
    public Image image(String assetId) {
        Image img = images.get(assetId);
        if (img != null) {
            return img;
        }
        Image ph = placeholders.get(assetId);
        if (ph != null) {
            return ph;
        }
        AssetDef d = byId.get(assetId);
        if (d == null) {
            return null;
        }
        ph = Image.createImage(Math.max(1, d.getWidth()), Math.max(1, d.getHeight()), d.getColor());
        placeholders.put(assetId, ph);
        return ph;
    }
}
