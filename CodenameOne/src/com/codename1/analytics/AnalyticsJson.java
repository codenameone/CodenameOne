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
package com.codename1.analytics;

/// Minimal hand-rolled JSON writing helpers shared by the analytics providers.
/// Mirrors the approach used by {@code com.codename1.crash} so the device does
/// not pull in a JSON parser dependency, and produces RFC 8259 compliant
/// strings.
final class AnalyticsJson {
    private AnalyticsJson() {
    }

    static void appendString(StringBuilder b, String key, String value, boolean first) {
        if (!first) {
            b.append(',');
        }
        b.append('"').append(key).append("\":");
        if (value == null) {
            b.append("null");
            return;
        }
        b.append('"');
        escape(b, value);
        b.append('"');
    }

    static void appendLong(StringBuilder b, String key, long value, boolean first) {
        if (!first) {
            b.append(',');
        }
        b.append('"').append(key).append("\":").append(value);
    }

    static void appendBoolean(StringBuilder b, String key, boolean value, boolean first) {
        if (!first) {
            b.append(',');
        }
        b.append('"').append(key).append("\":").append(value);
    }

    static void appendValue(StringBuilder b, Object value) {
        if (value == null) {
            b.append("null");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            b.append(value.toString());
            return;
        }
        b.append('"');
        escape(b, value.toString());
        b.append('"');
    }

    static void escape(StringBuilder b, String value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            b.append('0');
                        }
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
            }
        }
    }
}
