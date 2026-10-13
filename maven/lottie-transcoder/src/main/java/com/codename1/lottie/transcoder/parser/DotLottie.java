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
package com.codename1.lottie.transcoder.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Reads a dotLottie archive: a zip around one or more Lottie animations and
/// a manifest that says which of them is shown.
public final class DotLottie {
    private DotLottie() {
    }

    /// The animation a dotLottie archive shows, as Lottie JSON text.
    ///
    /// That is the one its `manifest.json` picks: `initial.animation` in the
    /// newer layout, `activeAnimationId` in the older one, and the first of
    /// the manifest's `animations` when neither is given. The animation is
    /// `a/<id>.json` or `animations/<id>.json`. An archive whose manifest
    /// is missing, unreadable or names nothing that is there gives the
    /// first entry of either directory, and failing that its first JSON
    /// entry that is not the manifest.
    ///
    /// #### Returns
    ///
    /// the JSON, or null when the archive holds no animation
    public static String animation(InputStream in) throws IOException {
        Map<String, String> entries = new LinkedHashMap<String, String>();
        ZipInputStream zip = new ZipInputStream(in);
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            String name = entry.getName();
            if (entry.isDirectory() || !name.endsWith(".json")) {
                continue;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            for (int n = zip.read(chunk); n >= 0; n = zip.read(chunk)) {
                bytes.write(chunk, 0, n);
            }
            entries.put(name, new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        }
        String id = chosen(entries.get("manifest.json"));
        if (id != null) {
            String picked = entries.get("a/" + id + ".json");
            if (picked == null) {
                picked = entries.get("animations/" + id + ".json");
            }
            if (picked != null) {
                return picked;
            }
        }
        String fallback = null;
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String name = e.getKey();
            if (name.endsWith("manifest.json")) {
                continue;
            }
            if (name.startsWith("animations/") || name.startsWith("a/")) {
                return e.getValue();
            }
            if (fallback == null) {
                fallback = e.getValue();
            }
        }
        return fallback;
    }

    /// The id of the animation a manifest picks, or null.
    private static String chosen(String manifest) {
        if (manifest == null) {
            return null;
        }
        Object root;
        try {
            root = JsonParser.parse(manifest);
        } catch (RuntimeException ex) {
            return null;
        }
        if (!(root instanceof Map)) {
            return null;
        }
        Map<?, ?> m = (Map<?, ?>) root;
        Object initial = m.get("initial");
        if (initial instanceof Map && ((Map<?, ?>) initial).get("animation") instanceof String) {
            return (String) ((Map<?, ?>) initial).get("animation");
        }
        if (m.get("activeAnimationId") instanceof String) {
            return (String) m.get("activeAnimationId");
        }
        Object animations = m.get("animations");
        if (animations instanceof List && !((List<?>) animations).isEmpty()) {
            Object first = ((List<?>) animations).get(0);
            if (first instanceof Map && ((Map<?, ?>) first).get("id") instanceof String) {
                return (String) ((Map<?, ?>) first).get("id");
            }
        }
        return null;
    }
}
