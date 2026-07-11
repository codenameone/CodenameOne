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
package com.codename1.surfaces;

import com.codename1.io.JSONWriter;
import com.codename1.io.Log;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Serializes surface descriptors to the canonical wire format shared by every port: a compact
/// JSON document plus PNG blobs named by content hash. This class is an internal seam between the
/// core API and the platform bridges -- it is public only because ports live in separate
/// artifacts; apps never call it.
///
/// The wire format is versioned (`"v": 1`) and deterministic: node maps preserve build order,
/// state maps are emitted with sorted keys, timeline entries are sorted ascending by date and
/// image names derive from a hash of the PNG bytes (identical art always gets the same name).
public final class SurfaceSerializer {
    private static final int PAYLOAD_WARN_BYTES = 200 * 1024;

    private SurfaceSerializer() {
    }

    /// Serializes a widget timeline.
    ///
    /// #### Parameters
    ///
    /// - `kindId`: the widget kind id the timeline belongs to
    /// - `timeline`: the timeline to serialize
    /// - `imagesOut`: receives PNG blobs keyed by registered name
    ///
    /// #### Returns
    ///
    /// the timeline JSON
    public static String serializeTimeline(String kindId, WidgetTimeline timeline,
            Map<String, byte[]> imagesOut) {
        if (timeline.getDefaultContent() == null
                && timeline.getContent(WidgetSize.SMALL) == null
                && timeline.getContent(WidgetSize.MEDIUM) == null
                && timeline.getContent(WidgetSize.LARGE) == null
                && timeline.getContent(WidgetSize.LOCKSCREEN) == null) {
            throw new IllegalArgumentException("A widget timeline needs content: call "
                    + "setContent(...) before publishing");
        }
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(1));
        doc.put("kind", kindId);
        doc.put("reload", timeline.getReloadPolicy() == WidgetTimeline.RELOAD_NEVER ? "never" : "atEnd");

        Map<String, Object> layouts = new LinkedHashMap<String, Object>();
        SurfaceNode def = timeline.getDefaultContent();
        if (def != null) {
            layouts.put("default", def.toMap(imagesOut, 0));
        }
        for (WidgetSize size : WidgetSize.values()) {
            SurfaceNode content = timeline.getContent(size);
            if (content != null && content != def) {
                layouts.put(size.getJsonName(), content.toMap(imagesOut, 0));
            }
        }
        doc.put("layouts", layouts);

        List<WidgetTimeline.Entry> entries =
                new ArrayList<WidgetTimeline.Entry>(timeline.getEntries());
        if (entries.isEmpty()) {
            entries.add(new WidgetTimeline.Entry(new Date(),
                    new LinkedHashMap<String, Object>()));
        }
        sortEntries(entries);
        List<Object> entryList = new ArrayList<Object>(entries.size());
        for (WidgetTimeline.Entry e : entries) {
            Map<String, Object> em = new LinkedHashMap<String, Object>();
            em.put("date", Long.valueOf(e.getDate().getTime()));
            em.put("state", sortedCopy(e.getState()));
            entryList.add(em);
        }
        doc.put("entries", entryList);
        doc.put("images", new ArrayList<Object>(imagesOut.keySet()));
        return emit(doc, imagesOut);
    }

    /// Serializes a live activity descriptor together with its initial state.
    ///
    /// #### Parameters
    ///
    /// - `descriptor`: the descriptor to serialize
    /// - `state`: the initial state map, may be null
    /// - `imagesOut`: receives PNG blobs keyed by registered name
    ///
    /// #### Returns
    ///
    /// the descriptor JSON
    public static String serializeLiveActivity(LiveActivityDescriptor descriptor,
            Map<String, Object> state, Map<String, byte[]> imagesOut) {
        if (descriptor.getContent() == null) {
            throw new IllegalArgumentException("A live activity needs content: call "
                    + "setContent(...) before starting it");
        }
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("v", Integer.valueOf(1));
        doc.put("type", descriptor.getActivityType());
        if (descriptor.getTint() != null) {
            doc.put("tint", colorMap(descriptor.getTint()));
        }
        if (descriptor.getAndroidChannelId() != null) {
            Map<String, Object> android = new LinkedHashMap<String, Object>();
            android.put("channel", descriptor.getAndroidChannelId());
            doc.put("android", android);
        }
        doc.put("content", descriptor.getContent().toMap(imagesOut, 0));
        Map<String, Object> island = new LinkedHashMap<String, Object>();
        putRegion(island, "compactLeading", descriptor.getCompactLeading(), imagesOut);
        putRegion(island, "compactTrailing", descriptor.getCompactTrailing(), imagesOut);
        putRegion(island, "minimal", descriptor.getMinimal(), imagesOut);
        putRegion(island, "expandedLeading", descriptor.getExpandedLeading(), imagesOut);
        putRegion(island, "expandedTrailing", descriptor.getExpandedTrailing(), imagesOut);
        putRegion(island, "expandedCenter", descriptor.getExpandedCenter(), imagesOut);
        putRegion(island, "expandedBottom", descriptor.getExpandedBottom(), imagesOut);
        if (!island.isEmpty()) {
            doc.put("island", island);
        }
        doc.put("state", sortedCopy(state == null ? new LinkedHashMap<String, Object>() : state));
        doc.put("images", new ArrayList<Object>(imagesOut.keySet()));
        return emit(doc, imagesOut);
    }

    /// Serializes a state map with sorted keys.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state map, may be null
    ///
    /// #### Returns
    ///
    /// the state JSON
    public static String serializeState(Map<String, Object> state) {
        return JSONWriter.toJson(sortedCopy(state == null
                ? new LinkedHashMap<String, Object>() : state));
    }

    /// Serializes a widget kind declaration.
    ///
    /// #### Parameters
    ///
    /// - `kind`: the kind to serialize
    ///
    /// #### Returns
    ///
    /// the kind JSON
    public static String serializeKind(WidgetKind kind) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("id", kind.getId());
        if (kind.getDisplayName() != null) {
            doc.put("name", kind.getDisplayName());
        }
        if (kind.getDescription() != null) {
            doc.put("description", kind.getDescription());
        }
        List<Object> sizes = new ArrayList<Object>();
        for (WidgetSize s : kind.getSupportedSizes()) {
            sizes.add(s.getJsonName());
        }
        doc.put("sizes", sizes);
        return JSONWriter.toJson(doc);
    }

    // --- package-private helpers used by the node model ----------------------

    /// Serializes a color to its wire form.
    static Map<String, Object> colorMap(SurfaceColor color) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        if (color.getRole() != null) {
            m.put("role", color.getRole());
        } else {
            m.put("l", Integer.valueOf(color.getLight()));
            m.put("d", Integer.valueOf(color.getDark()));
        }
        return m;
    }

    /// Copies a state or parameter map into a key-sorted map so serialization is deterministic
    /// regardless of the map implementation the app supplied.
    static Map<String, Object> sortedCopy(Map<String, Object> map) {
        Map<String, Object> sorted = new TreeMap<String, Object>();
        if (map != null) {
            sorted.putAll(map);
        }
        return sorted;
    }

    /// Encodes an image to PNG, names the blob by content hash and registers it in `images`.
    /// Returns the registered name, or null when encoding failed.
    static String registerImage(Image image, Map<String, byte[]> images) {
        return registerImageBytes(encode(image), images);
    }

    /// Names a PNG blob by content hash and registers it in `images`. Returns the registered
    /// name, or null for a null blob.
    static String registerImageBytes(byte[] data, Map<String, byte[]> images) {
        if (data == null) {
            return null;
        }
        String name = "img" + fnv1a(data);
        if (!images.containsKey(name)) {
            images.put(name, data);
        }
        return name;
    }

    // --- internals ------------------------------------------------------------

    private static void putRegion(Map<String, Object> island, String key, SurfaceNode node,
            Map<String, byte[]> images) {
        if (node != null) {
            island.put(key, node.toMap(images, 0));
        }
    }

    private static String emit(Map<String, Object> doc, Map<String, byte[]> images) {
        String json = JSONWriter.toJson(doc);
        int total = json.length();
        for (byte[] blob : images.values()) {
            total += blob.length;
        }
        if (total > PAYLOAD_WARN_BYTES) {
            Log.p("Surfaces: the published payload is " + (total / 1024) + "kb; widget renderers "
                    + "run under tight memory and transaction budgets, consider smaller images");
        }
        return json;
    }

    private static byte[] encode(Image img) {
        if (img == null) {
            return null;
        }
        try {
            if (img instanceof EncodedImage) {
                return ((EncodedImage) img).getImageData();
            }
            ImageIO io = ImageIO.getImageIO();
            if (io != null) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                io.save(img, bo, ImageIO.FORMAT_PNG, 1f);
                return bo.toByteArray();
            }
        } catch (Throwable t) {
            Log.e(t);
        }
        return null;
    }

    private static String fnv1a(byte[] data) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : data) {
            hash ^= b & 0xff;
            hash *= 0x100000001b3L;
        }
        String hex = Long.toHexString(hash);
        StringBuilder sb = new StringBuilder(16);
        for (int p = hex.length(); p < 16; p++) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }

    private static void sortEntries(List<WidgetTimeline.Entry> entries) {
        // insertion sort keeps this dependency-free and stable; timelines are short
        for (int i = 1; i < entries.size(); i++) {
            WidgetTimeline.Entry e = entries.get(i);
            int j = i - 1;
            while (j >= 0 && entries.get(j).getDate().getTime() > e.getDate().getTime()) {
                entries.set(j + 1, entries.get(j));
                j--;
            }
            entries.set(j + 1, e);
        }
    }
}
