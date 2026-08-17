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

import com.codename1.io.JSONWriter;
import com.codename1.surfaces.SurfaceSerializer;
import com.codename1.ui.EncodedImage;

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

    /// Serializes entities for the search index, collecting their thumbnails.
    ///
    /// #### Parameters
    ///
    /// - `entities`: the entities to serialize
    /// - `images`: receives PNG blobs keyed by the name used in the JSON
    public static String serializeEntities(List<AppEntity> entities, Map<String, byte[]> images) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(VERSION));
        List<Object> out = new ArrayList<Object>();
        if (entities != null) {
            for (AppEntity e : entities) {
                out.add(entityToMap(e, images));
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
        if (img != null && images != null) {
            byte[] bytes = img.getImageData();
            if (bytes != null && bytes.length > 0) {
                String name = "e" + Integer.toHexString(contentHash(bytes));
                images.put(name, bytes);
                m.put("image", name);
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
    private static Object toWire(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number) {
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

    private static int contentHash(byte[] data) {
        int h = 17;
        for (byte b : data) {
            h = h * 31 + b;
        }
        return h;
    }
}
