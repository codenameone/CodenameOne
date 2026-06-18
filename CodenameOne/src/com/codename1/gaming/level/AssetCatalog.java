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

import com.codename1.gaming.SpriteSheet;
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

import java.io.ByteArrayOutputStream;
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
/// It indexes one or more `AssetPack`s and resolves each asset's art by its
/// `AssetDef#getType()`: `#image(String)` for a static image or a sprite-sheet still,
/// `#sheet(String)` for the `com.codename1.gaming.SpriteSheet` of an animated asset, and
/// `#meshData(String)` for the glTF/glb bytes of a 3D asset. `#resolveArt()` loads those
/// from each def's `AssetDef#getSource()` resource; until art is supplied `#image(String)`
/// returns a cached solid-color placeholder sized from the def, so a level always realizes
/// to something visible.
public class AssetCatalog {
    private final Map<String, AssetPack> packs = new LinkedHashMap<String, AssetPack>();
    /// flat assetId -> def index across all packs (first pack wins on a clash).
    private final Map<String, AssetDef> byId = new HashMap<String, AssetDef>();
    private final Map<String, Image> images = new HashMap<String, Image>();
    private final Map<String, Image> placeholders = new HashMap<String, Image>();
    private final Map<String, SpriteSheet> sheets = new HashMap<String, SpriteSheet>();
    private final Map<String, byte[]> meshes = new HashMap<String, byte[]>();

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

    /// Supplies the sprite sheet for an animated (`AssetDef#TYPE_SHEET`) asset.
    public AssetCatalog setSheet(String assetId, SpriteSheet sheet) {
        sheets.put(assetId, sheet);
        return this;
    }

    /// The sprite sheet for an animated asset, or null if it has none.
    public SpriteSheet sheet(String assetId) {
        return sheets.get(assetId);
    }

    /// Supplies the glTF/glb bytes for a 3D (`AssetDef#TYPE_MESH`) asset.
    public AssetCatalog setMeshData(String assetId, byte[] data) {
        meshes.put(assetId, data);
        return this;
    }

    /// The glTF/glb bytes for a 3D asset (load with `com.codename1.gpu.GltfLoader`), or
    /// null if it has none.
    public byte[] meshData(String assetId) {
        return meshes.get(assetId);
    }

    /// Resolves the image for an asset id: the explicit art if one was supplied, else the
    /// first frame of a sprite sheet, else a cached solid-color placeholder sized from the
    /// def. Returns null only when the id is unknown to every pack and no art was set.
    public Image image(String assetId) {
        Image img = images.get(assetId);
        if (img != null) {
            return img;
        }
        SpriteSheet sh = sheets.get(assetId);
        if (sh != null) {
            return sh.getFrame(0);
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

    /// Loads each asset's art from its `AssetDef#getSource()` resource: a static image,
    /// a sprite sheet (`AssetDef#TYPE_SHEET`), or glTF/glb mesh bytes (`AssetDef#TYPE_MESH`).
    /// Best-effort -- an asset whose source is missing keeps its placeholder. Tries the
    /// source path as-is and, for the flat device bundle, by file name. Returns `this`.
    public AssetCatalog resolveArt() {
        for (AssetDef d : byId.values()) {
            String src = d.getSource();
            if (src == null || images.containsKey(d.getId())
                    || sheets.containsKey(d.getId()) || meshes.containsKey(d.getId())) {
                continue;
            }
            InputStream in = openResource(src);
            if (in == null) {
                continue;
            }
            try {
                if (d.isMesh()) {
                    meshes.put(d.getId(), readBytes(in));
                } else {
                    Image img = Image.createImage(in);
                    if (img == null) {
                        continue;
                    }
                    if (d.isSheet() && d.getFrameWidth() > 0) {
                        int fh = d.getFrameHeight() > 0 ? d.getFrameHeight() : img.getHeight();
                        sheets.put(d.getId(), new SpriteSheet(img, d.getFrameWidth(), fh));
                    } else {
                        images.put(d.getId(), img);
                    }
                }
            } catch (IOException ignore) { //NOPMD - a missing/bad art file just keeps the placeholder
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) { //NOPMD - closing is best-effort
                }
            }
        }
        return this;
    }

    /// Opens an art resource, trying the full path then the bare file name (the CN1
    /// device bundle is a flat namespace).
    private static InputStream openResource(String src) {
        String path = src.startsWith("/") ? src : "/" + src;
        InputStream in = Display.getInstance().getResourceAsStream(AssetCatalog.class, path);
        if (in == null) {
            int slash = src.lastIndexOf('/');
            String base = slash >= 0 ? src.substring(slash + 1) : src;
            in = Display.getInstance().getResourceAsStream(AssetCatalog.class, "/" + base);
        }
        return in;
    }

    private static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
