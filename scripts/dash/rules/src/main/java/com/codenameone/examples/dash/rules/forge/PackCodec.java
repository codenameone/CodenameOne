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

import com.codenameone.examples.dash.rules.Json;
import com.codenameone.examples.dash.rules.LevelCodec;
import com.codenameone.examples.dash.rules.LevelDef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A [ContentPack] as JSON. Each level is written by [LevelCodec], so a single
/// level exported from the editor and the same level inside a pack are one
/// format; the pack's own definitions and art are written once, at the top.
///
/// The output is canonical -- keys in a fixed order, no whitespace -- because
/// the pack's hash is taken over it.
public final class PackCodec {
    private PackCodec() {
    }

    public static String write(ContentPack p) {
        return Json.write(toMap(p));
    }

    public static Map<String, Object> toMap(ContentPack p) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("format", Long.valueOf(ContentPack.FORMAT));
        m.put("id", p.id);
        m.put("name", p.name);
        m.put("author", p.author);
        m.put("description", p.description);
        m.put("version", Long.valueOf(p.version));
        // The definitions travel inside a carrier level so LevelCodec writes them;
        // only its "defs" member is kept.
        LevelDef carrier = new LevelDef("defs", "defs");
        Map<?, ?> defs = (Map<?, ?>) Json.parse(LevelCodec.encode(carrier, p.defs, p.art, p.art.get(ContentPack.RUNNER)));
        m.put("defs", defs.get("defs"));
        if (p.art.containsKey(ContentPack.RUNNER)) {
            m.put("runnerArt", p.art.get(ContentPack.RUNNER));
        }
        List<Object> levels = new ArrayList<Object>();
        for (int i = 0; i < p.levels.size(); i++) {
            levels.add(Json.parse(LevelCodec.encode(p.levels.get(i))));
        }
        m.put("levels", levels);
        List<Object> assets = new ArrayList<Object>();
        for (int i = 0; i < p.assets.size(); i++) {
            AssetRef a = p.assets.get(i);
            Map<String, Object> am = new LinkedHashMap<String, Object>();
            am.put("sha256", a.sha256);
            am.put("kind", a.kind);
            am.put("name", a.name);
            am.put("bytes", Long.valueOf(a.bytes));
            assets.add(am);
        }
        m.put("assets", assets);
        return m;
    }

    /// Parses a pack, refusing anything malformed. Whether it is any good is
    /// [Validator]'s question.
    public static ContentPack read(String json) {
        Object root = Json.parse(json);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("a pack is an object");
        }
        Map<?, ?> m = (Map<?, ?>) root;
        ContentPack p = new ContentPack();
        p.id = str(m, "id");
        p.name = str(m, "name");
        p.author = str(m, "author");
        p.description = str(m, "description");
        Object v = m.get("version");
        p.version = v instanceof Long ? (int) ((Long) v).longValue() : 0;
        Map<String, Object> carrier = new LinkedHashMap<String, Object>();
        carrier.put("id", "defs");
        carrier.put("defs", m.get("defs") == null ? new ArrayList<Object>() : m.get("defs"));
        if (m.get("runnerArt") != null) {
            carrier.put("runnerArt", m.get("runnerArt"));
        }
        LevelCodec.Decoded d = LevelCodec.decode(Json.write(carrier));
        p.art.putAll(d.art);
        if (d.runnerArt != null) {
            p.art.put(ContentPack.RUNNER, d.runnerArt);
        }
        Object defs = m.get("defs");
        if (defs instanceof List) {
            for (int i = 0; i < ((List<?>) defs).size(); i++) {
                Object id = ((Map<?, ?>) ((List<?>) defs).get(i)).get("id");
                Object def = d.custom.obstacle((String) id);
                p.defs.add(def != null ? def : d.custom.pickup((String) id));
            }
        }
        Object levels = m.get("levels");
        if (levels instanceof List) {
            for (int i = 0; i < ((List<?>) levels).size(); i++) {
                p.levels.add(LevelCodec.decode(Json.write(((List<?>) levels).get(i))).level);
            }
        }
        Object assets = m.get("assets");
        if (assets instanceof List) {
            for (int i = 0; i < ((List<?>) assets).size(); i++) {
                Object o = ((List<?>) assets).get(i);
                if (!(o instanceof Map)) {
                    throw new IllegalArgumentException("an asset is an object");
                }
                Map<?, ?> am = (Map<?, ?>) o;
                AssetRef a = new AssetRef();
                a.sha256 = str(am, "sha256");
                a.kind = str(am, "kind");
                a.name = str(am, "name");
                Object b = am.get("bytes");
                a.bytes = b instanceof Long ? (int) ((Long) b).longValue() : 0;
                p.assets.add(a);
            }
        }
        return p;
    }

    private static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v instanceof String ? (String) v : "";
    }
}
