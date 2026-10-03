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
package com.codename1.androidcompat.runtime;

/// Conversions used by the generated [JsInterfaceDispatch]: JavaScript
/// arguments arrive as strings and are converted to each parameter's Java
/// type the way Android's bridge converts them (numbers truncate toward
/// zero), and results go back as JavaScript source.
public final class JsBridge {

    private JsBridge() {
    }

    private static double number(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        if (s.equals("true")) {
            return 1;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int toInt(String s) {
        double d = number(s);
        return d != d ? 0 : (int) d;
    }

    public static long toLong(String s) {
        double d = number(s);
        return d != d ? 0 : (long) d;
    }

    public static short toShort(String s) {
        return (short) toInt(s);
    }

    public static byte toByte(String s) {
        return (byte) toInt(s);
    }

    public static char toChar(String s) {
        return (char) toInt(s);
    }

    public static double toDouble(String s) {
        return number(s);
    }

    public static float toFloat(String s) {
        return (float) number(s);
    }

    public static boolean toBoolean(String s) {
        return "true".equals(s);
    }

    public static String undefined() {
        return "undefined";
    }

    public static String literal(int v) {
        return String.valueOf(v);
    }

    public static String literal(long v) {
        return String.valueOf(v);
    }

    public static String literal(double v) {
        if (v != v) {
            return "NaN";
        }
        if (v == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (v == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        return String.valueOf(v);
    }

    public static String literal(float v) {
        return literal((double) v);
    }

    public static String literal(boolean v) {
        return v ? "true" : "false";
    }

    public static String literal(char v) {
        return quote(String.valueOf(v));
    }

    /// Strings are returned as strings; any other object as its `toString`.
    public static String literal(Object v) {
        if (v == null) {
            return "null";
        }
        return quote(v.toString());
    }

    /// A JavaScript string literal for `s`, safe to embed in a script.
    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2);
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
                    if (c < 0x20 || c > 0x7e || c == '<' || c == '>') {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int k = hex.length(); k < 4; k++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
