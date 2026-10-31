/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.rules.forge;

import com.codenameone.examples.dash.rules.Catalog;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.ObstacleDef;
import com.codenameone.examples.dash.rules.PickupDef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What a player builds in the Forge and publishes: one or more levels, the
/// obstacles and pickups they invent, and the art those use. Official content is
/// a pack too ([Samples#official]), so the game's own levels go through the same
/// path as anyone's.
public final class ContentPack {
    /// The version of the pack format [PackCodec] writes.
    public static final int FORMAT = 1;

    public String id = "";
    public String name = "";
    public String author = "";
    public String description = "";
    /// Set by the server on publishing; 0 for a draft.
    public int version;
    /// [ObstacleDef] and [PickupDef] entries, ids namespaced `u:`.
    public List<Object> defs = new ArrayList<Object>();
    /// Definition id (or [#RUNNER]) to asset reference ([AssetRef#ref]).
    public Map<String, String> art = new LinkedHashMap<String, String>();
    public List<LevelDef> levels = new ArrayList<LevelDef>();
    public List<AssetRef> assets = new ArrayList<AssetRef>();

    /// The key under [#art] for a runner model the pack replaces the player's with.
    public static final String RUNNER = "runner";

    /// The built-in catalog plus this pack's definitions.
    public Catalog catalog() {
        Catalog own = new Catalog();
        for (int i = 0; i < defs.size(); i++) {
            Object d = defs.get(i);
            if (d instanceof ObstacleDef) {
                own.add((ObstacleDef) d);
            } else if (d instanceof PickupDef) {
                own.add((PickupDef) d);
            }
        }
        return Content.catalog().with(own);
    }

    public LevelDef level(String levelId) {
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).id.equals(levelId)) {
                return levels.get(i);
            }
        }
        return null;
    }

    public AssetRef asset(String sha256) {
        for (int i = 0; i < assets.size(); i++) {
            if (assets.get(i).sha256.equals(sha256)) {
                return assets.get(i);
            }
        }
        return null;
    }

    /// A 64-bit FNV-1a hash of the pack's canonical text, in 16 hex digits.
    public String hash() {
        return hashOf(PackCodec.write(this));
    }

    public static String hashOf(String canonical) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < canonical.length(); i++) {
            char c = canonical.charAt(i);
            h ^= c & 0xff;
            h *= 0x100000001b3L;
            h ^= (c >>> 8) & 0xff;
            h *= 0x100000001b3L;
        }
        String hex = Long.toHexString(h);
        StringBuilder sb = new StringBuilder();
        for (int i = hex.length(); i < 16; i++) {
            sb.append('0');
        }
        return sb.append(hex).toString();
    }

    /// A copy that shares nothing mutable with this one.
    public ContentPack copy() {
        return PackCodec.read(PackCodec.write(this));
    }
}
