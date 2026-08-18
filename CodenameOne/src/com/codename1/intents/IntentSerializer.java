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
package com.codename1.intents;

import com.codename1.io.JSONParser;
import com.codename1.io.JSONWriter;
import com.codename1.io.Log;
import com.codename1.surfaces.SurfaceSerializer;
import com.codename1.ui.EncodedImage;
import com.codename1.util.Base64;

import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Serializes intent declarations, entities and results to the wire format the
/// platform bridges consume: a compact JSON document plus PNG blobs named by
/// content hash. This class is an internal seam between the core API and the
/// platform bridges -- it is public only because ports live in separate
/// artifacts; applications never call it.
///
/// The format is versioned (`"v": 1`). Everything crosses as data because an
/// invocation can be answered while the app has no UI, and because the peer is
/// Swift or Kotlin rather than Java.
public final class IntentSerializer {

    private static final int VERSION = 1;

    private IntentSerializer() {
    }

    /// Serializes the application's intent catalogue.
    ///
    /// #### Parameters
    ///
    /// - `declarations`: the declarations to serialize; null becomes empty
    public static String serializeDeclarations(List<IntentDeclaration> declarations) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(VERSION));
        List<Object> out = new ArrayList<Object>();
        if (declarations != null) {
            for (IntentDeclaration d : declarations) {
                out.add(declarationToMap(d));
            }
        }
        doc.put("intents", out);
        return JSONWriter.toJson(doc);
    }

    private static Map<String, Object> declarationToMap(IntentDeclaration d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", d.getId());
        m.put("title", d.getTitle());
        m.put("description", d.getDescription());
        m.put("headless", Boolean.valueOf(d.isHeadless()));
        m.put("discoverable", Boolean.valueOf(d.isDiscoverable()));
        m.put("destructive", Boolean.valueOf(d.isDestructive()));
        m.put("opensRoute", d.getOpensRoute());
        m.put("timeoutSeconds", Integer.valueOf(d.getTimeoutSeconds()));
        m.put("phrases", new ArrayList<Object>(d.getPhrases()));

        List<Object> params = new ArrayList<Object>();
        for (IntentParameterInfo p : d.getParameters()) {
            Map<String, Object> pm = new LinkedHashMap<String, Object>();
            pm.put("name", p.getName());
            pm.put("title", p.getTitle());
            pm.put("type", p.getType().name().toLowerCase());
            pm.put("required", Boolean.valueOf(p.isRequired()));
            if (p.getEntityType() != null) {
                pm.put("entityType", p.getEntityType());
            }
            if (p.getDefaultValue() != null) {
                pm.put("default", p.getDefaultValue());
            }
            if (!p.getOptions().isEmpty()) {
                pm.put("options", new ArrayList<Object>(p.getOptions()));
            }
            params.add(pm);
        }
        m.put("params", params);
        return m;
    }

    /// Serializes a parameter map for donation or invocation.
    ///
    /// Values are reduced to the wire types -- text, numbers, booleans and epoch
    /// millis for dates -- because the receiving side is not Java. An [AppEntity]
    /// value reduces to its id, which is the only part of it the platform needs
    /// in order to hand the same entity back later.
    ///
    /// #### Parameters
    ///
    /// - `params`: the values; null becomes an empty document
    public static String serializeParams(Map<String, Object> params) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                Object wire = toWire(e.getValue());
                if (wire != null) {
                    out.put(e.getKey(), wire);
                }
            }
        }
        return JSONWriter.toJson(out);
    }

    /// Merges bound values under a serialized parameter document, returning a fresh document.
    ///
    /// Used when a parameterization is donated: the shortcut has to carry the values the
    /// parameterization bound, with anything supplied at donation time still winning, because a
    /// binding is a default rather than a lock.
    ///
    /// #### Parameters
    ///
    /// - `bound`: the parameterization's values
    /// - `paramsJson`: values supplied at donation time, may be null
    public static String mergeParams(Map<String, Object> bound, String paramsJson) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (bound != null) {
            for (Map.Entry<String, Object> e : bound.entrySet()) {
                Object wire = toWire(e.getValue());
                if (wire != null) {
                    out.put(e.getKey(), wire);
                }
            }
        }
        if (paramsJson != null && paramsJson.length() > 0) {
            try {
                Map<String, Object> supplied = parsePayload(paramsJson);
                if (supplied != null) {
                    out.putAll(supplied);
                }
            } catch (Throwable t) {
                Log.e(t);
            }
        }
        return JSONWriter.toJson(out);
    }

    /// Serializes entities for the search index, collecting their thumbnails.
    ///
    /// #### Parameters
    ///
    /// - `entities`: the entities to serialize
    /// - `images`: receives PNG blobs keyed by the name used in the JSON
    public static String serializeEntities(List<AppEntity> entities, Map<String, byte[]> images) {
        return serializeEntities(entities, images, false);
    }

    /// Serializes entities, optionally carrying their thumbnails inside the document.
    ///
    /// Indexing wants the second form: the blobs go out separately, keyed by the name embedded
    /// in the JSON, because a search index takes them one at a time and a base64 copy of every
    /// thumbnail in one string would be wasteful.
    ///
    /// A query answering a platform picker wants the first. The reply is synchronous and the
    /// caller is a native picker being built right now, so there is nowhere to put a side
    /// channel of bytes that the reply is guaranteed to be matched with. Inlining a handful of
    /// small thumbnails is the whole transaction.
    ///
    /// #### Parameters
    ///
    /// - `entities`: the entities to serialize
    /// - `images`: receives PNG blobs keyed by the name used in the JSON; may be null when
    ///   inlining
    /// - `inlineImages`: true to also write each thumbnail as base64 under `imageData`
    public static String serializeEntities(List<AppEntity> entities, Map<String, byte[]> images,
                                           boolean inlineImages) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(VERSION));
        List<Object> out = new ArrayList<Object>();
        if (entities != null) {
            for (AppEntity e : entities) {
                out.add(entityToMap(e, images, inlineImages));
            }
        }
        doc.put("entities", out);
        return JSONWriter.toJson(doc);
    }

    /// Serializes a single `{type, id}` reference for index removal.
    ///
    /// #### Parameters
    ///
    /// - `entityType`: the entity type id
    /// - `id`: the entity id
    public static String serializeEntityRef(String entityType, String id) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(VERSION));
        List<Object> refs = new ArrayList<Object>();
        Map<String, Object> ref = new LinkedHashMap<String, Object>();
        ref.put("type", entityType);
        ref.put("id", id);
        ref.put("uid", entityType + ":" + id);
        refs.add(ref);
        doc.put("refs", refs);
        return JSONWriter.toJson(doc);
    }

    /// Serializes an intent result for the platform, collecting any snippet
    /// imagery.
    ///
    /// #### Parameters
    ///
    /// - `result`: the result to serialize
    /// - `images`: receives PNG blobs referenced by the snippet
    public static String serializeResult(IntentResult result, Map<String, byte[]> images) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(VERSION));
        if (result == null) {
            doc.put("ok", Boolean.TRUE);
            return JSONWriter.toJson(doc);
        }
        doc.put("ok", Boolean.valueOf(!result.isFailed()));
        if (result.isFailed()) {
            doc.put("error", result.getErrorMessage() == null ? "" : result.getErrorMessage());
            return JSONWriter.toJson(doc);
        }
        Object value = toWire(result.getValue());
        if (value != null) {
            doc.put("value", value);
        }
        if (result.getDialog() != null) {
            doc.put("dialog", result.getDialog());
        }
        if (result.getOpenUrl() != null) {
            doc.put("openUrl", result.getOpenUrl());
        }
        if (result.getEntity() != null) {
            doc.put("entity", entityToMap(result.getEntity(), images));
        }
        if (result.getSnippet() != null) {
            // Reuse the surfaces wire format verbatim. A snippet is rendered by
            // the platform while this app may be off screen, which is the same
            // problem a widget solves, so it gets the same descriptor and the
            // same native renderer rather than a parallel one.
            doc.put("snippet", SurfaceSerializer.serializeNodeToMap(
                    result.getSnippet(), images));
        }
        return JSONWriter.toJson(doc);
    }

    private static Map<String, Object> entityToMap(AppEntity e, Map<String, byte[]> images) {
        return entityToMap(e, images, false);
    }

    /// A thumbnail large enough to matter here is a mistake rather than a picture: a picker row
    /// renders it at a few dozen points. Inlining is capped so one oversized image cannot turn
    /// a synchronous query reply into a payload the picker waits on.
    private static final int MAX_INLINE_IMAGE_BYTES = 128 * 1024;

    private static Map<String, Object> entityToMap(AppEntity e, Map<String, byte[]> images,
                                                   boolean inlineImages) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", e.getType());
        m.put("id", e.getId());
        // The platform search index stores one opaque identifier per entry and
        // hands that same string back on a tap, so the type has to travel inside
        // it. Composing it here keeps the native side from having to know the
        // convention.
        m.put("uid", e.getType() + ":" + e.getId());
        if (e.getTitle() != null) {
            m.put("title", e.getTitle());
        }
        if (e.getSubtitle() != null) {
            m.put("subtitle", e.getSubtitle());
        }
        if (!e.getKeywords().isEmpty()) {
            m.put("keywords", new ArrayList<Object>(e.getKeywords()));
        }
        EncodedImage img = e.getImage();
        if (img != null && (images != null || inlineImages)) {
            byte[] bytes = img.getImageData();
            if (bytes != null && bytes.length > 0) {
                String name = imageName(bytes, images);
                if (images != null) {
                    images.put(name, bytes);
                }
                m.put("image", name);
                if (inlineImages && bytes.length <= MAX_INLINE_IMAGE_BYTES) {
                    m.put("imageData", Base64.encodeNoNewline(bytes));
                }
            }
        }
        return m;
    }

    /// Reduces a Java value to something the wire format can carry. Returns null
    /// for anything unsupported, which the callers drop rather than guess at.
    ///
    /// Note the deliberate absence of a cast-and-catch: every branch is an
    /// `instanceof` test. On iOS a failed cast does not throw, so a
    /// `catch (ClassCastException)` here would be dead code guarding a native
    /// crash.
    /// Parses an intent payload, keeping whole numbers as `Long`.
    ///
    /// The convenience `JSONParser.parseJSON` defaults to materialising every number as a
    /// `Double`, which silently rounds anything past 2^53: an id of 9007199254740993 arrives as
    /// ...992, still integral, so every downstream check accepts it and the handler acts on a
    /// number the caller never sent. Ids of that size are ordinary -- snowflake ids, database
    /// keys -- and the corruption is invisible at every layer.
    ///
    /// Every path that reads an intent payload goes through this, in core and in the ports, so
    /// the guarantee does not depend on remembering it at six call sites.
    ///
    /// #### Parameters
    ///
    /// - `json`: the document; null or empty yields null
    public static Map<String, Object> parsePayload(String json) throws IOException {
        if (json == null || json.length() == 0) {
            return null;
        }
        JSONParser parser = new JSONParser();
        parser.setUseLongsInstance(true);
        return parser.parseJSON(new StringReader(json));
    }

    private static Object toWire(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                // JSONWriter would emit a bare NaN or Infinity token, which is not JSON. The
                // iOS decoder rejects the whole document and reports an empty success, so the
                // dialog and the snippet go with it -- one unrepresentable number costing the
                // entire result. Dropped instead: everything else in the document survives.
                return null;
            }
            return value;
        }
        if (value instanceof Date) {
            return Long.valueOf(((Date) value).getTime());
        }
        if (value instanceof AppEntity) {
            return ((AppEntity) value).getId();
        }
        if (value instanceof Character) {
            return value.toString();
        }
        return null;
    }

    /// A name for this blob that no other blob in the same request will take.
    ///
    /// The name was a 32-bit content hash, which is fine as a cache key and wrong as an
    /// identity: two different thumbnails that collide got the same name and the second
    /// overwrote the first, so one entity displayed another entity's picture on every platform.
    /// The length narrows it further, and an actual collision is then resolved by comparing the
    /// bytes -- exact, and cheaper than pulling in a digest the ParparVM runtime would have to
    /// carry.
    private static String imageName(byte[] bytes, Map<String, byte[]> images) {
        String base = "e" + Integer.toHexString(contentHash(bytes)) + "_" + bytes.length;
        if (images == null) {
            return base;
        }
        String name = base;
        for (int i = 1; ; i++) {
            byte[] existing = images.get(name);
            if (existing == null || sameBytes(existing, bytes)) {
                return name;
            }
            name = base + "_" + i;
        }
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static int contentHash(byte[] data) {
        int h = 17;
        for (byte b : data) {
            h = h * 31 + b;
        }
        return h;
    }
}
