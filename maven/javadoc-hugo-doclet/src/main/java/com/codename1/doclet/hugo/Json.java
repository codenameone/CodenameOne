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
package com.codename1.doclet.hugo;

import java.util.List;
import java.util.Map;

/**
 * A minimal JSON writer, used to emit Hugo front matter.
 *
 * <p>Hugo accepts front matter as TOML, YAML or JSON, and this generator emits
 * JSON deliberately. Almost every value written here is prose lifted verbatim
 * out of a documentation comment: it contains quotes, colons, backslashes,
 * leading dashes, blank lines and occasionally something that reads as a YAML
 * anchor. Escaping all of that correctly for YAML is a source of silent
 * corruption, whereas JSON has exactly one escaping rule and no significant
 * whitespace at all.
 */
final class Json {

    private Json() {
    }

    static String write(Map<String, Object> value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value, 0);
        out.append('\n');
        return out.toString();
    }

    /**
     * The same JSON with no indentation, for the search index.
     *
     * <p>Front matter is indented because a person reads it while debugging a
     * page. The search index is only ever read by a browser, and pretty printing
     * tens of thousands of nested entries costs more bytes than the entries.
     */
    static String writeCompact(Map<String, Object> value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value, COMPACT);
        return out.toString();
    }

    /** Depth sentinel meaning "emit no whitespace at all". */
    private static final int COMPACT = Integer.MIN_VALUE;

    private static void writeValue(StringBuilder out, Object value, int depth) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Map<?, ?> map) {
            writeObject(out, map, depth);
        } else if (value instanceof List<?> list) {
            writeArray(out, list, depth);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            out.append(value);
        } else {
            writeString(out, value.toString());
        }
    }

    private static void writeObject(StringBuilder out, Map<?, ?> map, int depth) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        boolean compact = depth == COMPACT;
        out.append(compact ? "{" : "{\n");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(compact ? "," : ",\n");
            }
            first = false;
            indent(out, depth + 1);
            writeString(out, entry.getKey().toString());
            out.append(compact ? ":" : ": ");
            writeValue(out, entry.getValue(), compact ? COMPACT : depth + 1);
        }
        if (!compact) {
            out.append('\n');
            indent(out, depth);
        }
        out.append('}');
    }

    private static void writeArray(StringBuilder out, List<?> list, int depth) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        boolean compact = depth == COMPACT;
        out.append(compact ? "[" : "[\n");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(compact ? "," : ",\n");
            }
            indent(out, depth + 1);
            writeValue(out, list.get(i), compact ? COMPACT : depth + 1);
        }
        if (!compact) {
            out.append('\n');
            indent(out, depth);
        }
        out.append(']');
    }

    private static void indent(StringBuilder out, int depth) {
        if (depth > 0) {
            out.append("  ".repeat(depth));
        }
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    // Control characters have to be escaped for the JSON to parse at
                    // all, and a stray one in a comment must never reach a generated
                    // file: scripts/check-control-characters.py rejects the whole
                    // tree over a single raw control byte.
                    //
                    // A lone surrogate is escaped for a harder reason: it has no
                    // UTF-8 encoding at all, so writing one out throws rather than
                    // producing a bad file, and one emoji cut in half by a summary
                    // would abort the whole generation.
                    if (c < 0x20 || c == 0x7f || isLoneSurrogate(value, i)) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    /** Whether the character at the index is a surrogate without its partner. */
    private static boolean isLoneSurrogate(String value, int index) {
        char c = value.charAt(index);
        if (Character.isHighSurrogate(c)) {
            return index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1));
        }
        if (Character.isLowSurrogate(c)) {
            return index == 0 || !Character.isHighSurrogate(value.charAt(index - 1));
        }
        return false;
    }
}
