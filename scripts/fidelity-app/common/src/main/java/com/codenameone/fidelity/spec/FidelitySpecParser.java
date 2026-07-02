/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.fidelity.spec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free parser for the flat YAML subset used by
 * fidelity-tests.yaml. Deliberately tiny so it translates on every CN1 backend
 * (no SnakeYAML, no regex-heavy logic). Understands:
 *   - two top-level sections: "defaults:" and "components:"
 *   - 2-space-indented "key: value" maps
 *   - comma-separated scalar lists (appearances, states, platforms)
 *   - list items under components introduced by "- "
 *   - "#" line comments and optional single/double quotes around values
 * It does NOT support anchors, flow style, nested maps, or tabs.
 */
public class FidelitySpecParser {
    private FidelitySpecParser() {
    }

    /** Reads the whole stream as UTF-8 and parses it. Closes the stream. */
    public static FidelitySpec parse(InputStream in) throws IOException {
        if (in == null) {
            throw new IOException("fidelity spec stream is null");
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                bos.write(buffer, 0, read);
            }
            return parse(new String(bos.toByteArray(), "UTF-8"));
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static FidelitySpec parse(String text) {
        FidelitySpec spec = new FidelitySpec();
        if (text == null) {
            return spec;
        }
        String section = "";
        ComponentSpec current = null;
        String[] lines = splitLines(text);
        for (int i = 0; i < lines.length; i++) {
            String raw = stripComment(lines[i]);
            if (raw.trim().length() == 0) {
                continue;
            }
            int indent = leadingSpaces(raw);
            String trimmed = raw.trim();
            if (indent == 0 && trimmed.endsWith(":")) {
                section = trimmed.substring(0, trimmed.length() - 1).trim();
                current = null;
                continue;
            }
            if ("defaults".equals(section)) {
                applyDefault(spec, trimmed);
            } else if ("components".equals(section)) {
                if (trimmed.startsWith("- ")) {
                    current = new ComponentSpec();
                    spec.getComponents().add(current);
                    applyComponentField(current, trimmed.substring(2).trim());
                } else if (current != null) {
                    applyComponentField(current, trimmed);
                }
            }
        }
        // Default any component without an explicit states list to a single
        // "normal" state so the suite always renders something for it.
        for (int i = 0; i < spec.getComponents().size(); i++) {
            ComponentSpec c = (ComponentSpec) spec.getComponents().get(i);
            if (c.getStates() == null || c.getStates().isEmpty()) {
                List def = new ArrayList();
                def.add("normal");
                c.setStates(def);
            }
        }
        if (spec.getAppearances() == null || spec.getAppearances().isEmpty()) {
            List def = new ArrayList();
            def.add("light");
            spec.getAppearances().add("light");
        }
        return spec;
    }

    private static void applyDefault(FidelitySpec spec, String keyValue) {
        int idx = keyValue.indexOf(':');
        if (idx < 0) {
            return;
        }
        String key = keyValue.substring(0, idx).trim();
        String value = unquote(keyValue.substring(idx + 1).trim());
        if ("tile_width_mm".equals(key)) {
            spec.setDefaultTileWidthMm(parseInt(value, spec.getDefaultTileWidthMm()));
        } else if ("tile_height_mm".equals(key)) {
            spec.setDefaultTileHeightMm(parseInt(value, spec.getDefaultTileHeightMm()));
        } else if ("bg".equals(key)) {
            spec.setBackgroundHex(value);
        } else if ("appearances".equals(key)) {
            spec.setAppearances(splitList(value));
        }
    }

    private static void applyComponentField(ComponentSpec component, String keyValue) {
        int idx = keyValue.indexOf(':');
        if (idx < 0) {
            return;
        }
        String key = keyValue.substring(0, idx).trim();
        String value = unquote(keyValue.substring(idx + 1).trim());
        if ("id".equals(key)) {
            component.setId(value);
        } else if ("cn1_uiid".equals(key)) {
            component.setCn1Uiid(value);
        } else if ("native".equals(key)) {
            component.setNativeKindIos(value);
        } else if ("native_android".equals(key)) {
            component.setNativeKindAndroid(value);
        } else if ("text".equals(key)) {
            component.setText(value);
        } else if ("backdrop".equals(key)) {
            component.setBackdrop(value);
        } else if ("material".equals(key)) {
            component.setMaterial(value);
        } else if ("tile_width_mm".equals(key)) {
            component.setTileWidthMm(parseInt(value, -1));
        } else if ("tile_height_mm".equals(key)) {
            component.setTileHeightMm(parseInt(value, -1));
        } else if ("states".equals(key)) {
            component.setStates(splitList(value));
        } else if ("platforms".equals(key)) {
            component.setPlatforms(splitList(value));
        }
    }

    private static String[] splitLines(String text) {
        String normalized = replaceAll(text, "\r\n", "\n");
        normalized = replaceAll(normalized, "\r", "\n");
        List parts = new ArrayList();
        int start = 0;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == '\n') {
                parts.add(normalized.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(normalized.substring(start));
        String[] out = new String[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            out[i] = (String) parts.get(i);
        }
        return out;
    }

    private static String replaceAll(String s, String from, String to) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (s.regionMatches(i, from, 0, from.length())) {
                sb.append(to);
                i += from.length();
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash < 0) {
            return line;
        }
        return line.substring(0, hash);
    }

    private static int leadingSpaces(String line) {
        int n = 0;
        while (n < line.length() && line.charAt(n) == ' ') {
            n++;
        }
        return n;
    }

    private static List splitList(String value) {
        List out = new ArrayList();
        if (value == null || value.length() == 0) {
            return out;
        }
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == ',') {
                String item = unquote(value.substring(start, i).trim());
                if (item.length() > 0) {
                    out.add(item);
                }
                start = i + 1;
            }
        }
        return out;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
