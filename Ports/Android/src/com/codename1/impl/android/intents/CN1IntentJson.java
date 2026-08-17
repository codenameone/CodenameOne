/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android.intents;

import android.util.Log;

import com.codename1.io.JSONParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Reads the index documents the framework's serializer produces.
///
/// This used to scan the raw text for `"uid"` rather than parse it, on the reasoning that
/// adding a JSON dependency to the Android port would be paid for by every application. That
/// reasoning was wrong: `com.codename1.io.JSONParser` is part of core and CN1IntentService in
/// this same package already uses it, so the dependency was always present and the scanner
/// bought nothing.
///
/// It also cost something. Searching for a key by its text cannot tell a key from a string
/// value that happens to equal it, so an entity legitimately typed `uid` -- serialized as
/// `"type":"uid"` -- was read as the start of another entity, and Android published an extra
/// malformed shortcut that counted against the ten-entry cap.
final class CN1IntentJson {

    private static final String TAG = "CN1Intents";

    private CN1IntentJson() {
    }

    /// Returns `{uid, title, subtitle, image}` for each entity in an index document.
    @SuppressWarnings("unchecked")
    static List<String[]> entities(String json) {
        List<String[]> out = new ArrayList<String[]>();
        Object list = field(parse(json), "entities");
        if (!(list instanceof List)) {
            return out;
        }
        for (Object o : (List<Object>) list) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> entity = (Map<String, Object>) o;
            String uid = string(entity, "uid");
            if (uid == null) {
                continue;
            }
            out.add(new String[]{uid, string(entity, "title"), string(entity, "subtitle"),
                    string(entity, "image")});
        }
        return out;
    }

    /// Returns the uids named in a removal document.
    @SuppressWarnings("unchecked")
    static List<String> refs(String json) {
        List<String> out = new ArrayList<String>();
        Object list = field(parse(json), "refs");
        if (!(list instanceof List)) {
            return out;
        }
        for (Object o : (List<Object>) list) {
            if (o instanceof Map) {
                String uid = string((Map<String, Object>) o, "uid");
                if (uid != null) {
                    out.add(uid);
                }
            }
        }
        return out;
    }

    private static Object field(Map<String, Object> doc, String name) {
        return doc == null ? null : doc.get(name);
    }

    /// A string field, or null when absent or of another type. Never a coerced value: a
    /// non-string where a string belongs means the document is not what this expects.
    private static String string(Map<String, Object> m, String name) {
        Object v = m.get(name);
        return v instanceof String ? (String) v : null;
    }

    private static Map<String, Object> parse(String json) {
        if (json == null || json.length() == 0) {
            return null;
        }
        try {
            return JSONParser.parseJSON(json);
        } catch (Throwable t) {
            Log.w(TAG, "Could not read an index document", t);
            return null;
        }
    }
}
