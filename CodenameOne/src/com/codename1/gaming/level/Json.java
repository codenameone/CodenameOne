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
package com.codename1.gaming.level;

import java.util.List;
import java.util.Map;

/// Small JSON coercion + emit helpers for the level format. `com.codename1.io.JSONParser`
/// parses (numbers come back as `Double`, flags as `String` unless configured), but the
/// framework has no canonical writer, so this emits one. Package-private on purpose.
final class Json {
    private Json() {
    }

    // ---- reading / coercion --------------------------------------------------

    static double num(Object v, double def) {
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ignore) {
                return def;
            }
        }
        return def;
    }

    static int intval(Object v, int def) {
        return (int) Math.round(num(v, def));
    }

    static boolean bool(Object v, boolean def) {
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                return true;
            }
            if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                return false;
            }
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue() != 0;
        }
        return def;
    }

    static String str(Object v, String def) {
        return v == null ? def : v.toString();
    }

    /// Coerces a color: an ARGB `Number`, or a CSS-style hex string (`#rgb`, `#rrggbb`
    /// or `#aarrggbb`). A 6-digit hex is made opaque. Falls back to `def`.
    static int color(Object v, int def) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            try {
                if (s.length() == 3) {
                    int r = Integer.parseInt(s.substring(0, 1), 16);
                    int g = Integer.parseInt(s.substring(1, 2), 16);
                    int b = Integer.parseInt(s.substring(2, 3), 16);
                    return 0xff000000 | (r * 17 << 16) | (g * 17 << 8) | (b * 17);
                }
                if (s.length() == 6) {
                    return 0xff000000 | (int) (Long.parseLong(s, 16) & 0xffffff);
                }
                if (s.length() == 8) {
                    return (int) (Long.parseLong(s, 16) & 0xffffffffL);
                }
            } catch (NumberFormatException ignore) {
                return def;
            }
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object v) {
        if (v instanceof Map) {
            return (Map<String, Object>) v;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object v) {
        if (v instanceof List) {
            return (List<Object>) v;
        }
        return null;
    }

    // ---- writing -------------------------------------------------------------

    static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    /// Writes a number, collapsing whole doubles to integers so files stay clean
    /// (`160` rather than `160.0`).
    static void writeNumber(StringBuilder sb, double value) {
        if (!Double.isInfinite(value) && !Double.isNaN(value)
                && value == Math.floor(value) && Math.abs(value) < 9.007199254740992E15) {
            sb.append(Long.toString((long) value));
        } else {
            sb.append(Double.toString(value));
        }
    }

    /// Writes an arbitrary property-bag value (Number / Boolean / String / Map / List /
    /// null) as JSON.
    @SuppressWarnings("unchecked")
    static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Number) {
            writeNumber(sb, ((Number) v).doubleValue());
        } else if (v instanceof Boolean) {
            sb.append(((Boolean) v).booleanValue() ? "true" : "false");
        } else if (v instanceof Map) {
            sb.append('{');
            boolean first = true;
            Map<String, Object> m = (Map<String, Object>) v;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, e.getKey());
                sb.append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof List) {
            sb.append('[');
            List<Object> l = (List<Object>) v;
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                writeValue(sb, l.get(i));
            }
            sb.append(']');
        } else {
            writeString(sb, v.toString());
        }
    }
}
