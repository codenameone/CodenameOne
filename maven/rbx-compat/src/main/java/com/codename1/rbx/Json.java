/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.rbx;

import com.codename1.luau.Interp;
import com.codename1.luau.LuaError;
import com.codename1.luau.LuaTable;
import com.codename1.luau.Utf8;

/// JSON for `HttpService:JSONEncode/JSONDecode` and the DataStore wire
/// format. Luau strings are byte strings, so text is UTF-8 decoded on the
/// way out and encoded on the way in; the JSON text itself is Java text.
public final class Json {
    private Json() {
    }

    public static String encode(Object v) {
        StringBuilder b = new StringBuilder();
        write(b, v, 0);
        return b.toString();
    }

    private static void write(StringBuilder b, Object v, int depth) {
        if (depth > 64) {
            throw LuaError.rt("Cannot convert a table with cycles to JSON");
        }
        if (v == null) {
            b.append("null");
        } else if (v instanceof Boolean) {
            b.append(((Boolean) v).booleanValue() ? "true" : "false");
        } else if (v instanceof Double) {
            double d = ((Double) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                b.append("null");
            } else {
                b.append(Interp.numberToString(d));
            }
        } else if (v instanceof String) {
            quote(b, Utf8.toJava((String) v));
        } else if (v instanceof LuaTable) {
            LuaTable t = (LuaTable) v;
            int n = t.length();
            Object[] kv = new Object[2];
            boolean array = n > 0 || !t.next(null, kv);
            if (array && n > 0) {
                Object k = null;
                int count = 0;
                while (t.next(k, kv)) {
                    count++;
                    k = kv[0];
                }
                array = count == n;
            }
            if (array) {
                b.append('[');
                for (int i = 1; i <= n; i++) {
                    if (i > 1) {
                        b.append(',');
                    }
                    write(b, t.rawgeti(i), depth + 1);
                }
                b.append(']');
            } else {
                b.append('{');
                Object k = null;
                boolean first = true;
                while (t.next(k, kv)) {
                    k = kv[0];
                    if (!first) {
                        b.append(',');
                    }
                    first = false;
                    quote(b, k instanceof String ? Utf8.toJava((String) k) : Interp.rawToString(k));
                    b.append(':');
                    write(b, kv[1], depth + 1);
                }
                b.append('}');
            }
        } else {
            throw LuaError.rt("Cannot convert " + Interp.typeName(v) + " to JSON");
        }
    }

    private static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        String h = Integer.toHexString(c);
                        b.append("\\u");
                        for (int p = h.length(); p < 4; p++) {
                            b.append('0');
                        }
                        b.append(h);
                    } else {
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }

    public static Object decode(String s) {
        int[] pos = {0};
        Object v = value(s, pos);
        skip(s, pos);
        if (pos[0] != s.length()) {
            throw LuaError.rt("Can't parse JSON");
        }
        return v;
    }

    private static void skip(String s, int[] p) {
        while (p[0] < s.length() && s.charAt(p[0]) <= ' ') {
            p[0]++;
        }
    }

    private static Object value(String s, int[] p) {
        skip(s, p);
        if (p[0] >= s.length()) {
            throw LuaError.rt("Can't parse JSON");
        }
        char c = s.charAt(p[0]);
        if (c == '{') {
            p[0]++;
            LuaTable t = new LuaTable();
            skip(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == '}') {
                p[0]++;
                return t;
            }
            while (true) {
                skip(s, p);
                Object k = value(s, p);
                skip(s, p);
                expect(s, p, ':');
                t.rawset(k, value(s, p));
                skip(s, p);
                if (p[0] < s.length() && s.charAt(p[0]) == ',') {
                    p[0]++;
                    continue;
                }
                expect(s, p, '}');
                return t; // NOPMD AvoidBranchingStatementAsLastInLoop - the loop ends at the closing brace
            }
        }
        if (c == '[') {
            p[0]++;
            LuaTable t = new LuaTable();
            skip(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == ']') {
                p[0]++;
                return t;
            }
            int i = 1;
            while (true) {
                t.rawseti(i++, value(s, p));
                skip(s, p);
                if (p[0] < s.length() && s.charAt(p[0]) == ',') {
                    p[0]++;
                    continue;
                }
                expect(s, p, ']');
                return t; // NOPMD AvoidBranchingStatementAsLastInLoop - the loop ends at the closing bracket
            }
        }
        if (c == '"') {
            return Utf8.fromJava(string(s, p));
        }
        if (s.startsWith("true", p[0])) {
            p[0] += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", p[0])) {
            p[0] += 5;
            return Boolean.FALSE;
        }
        if (s.startsWith("null", p[0])) {
            p[0] += 4;
            return null;
        }
        int start = p[0];
        while (p[0] < s.length() && "+-0123456789.eE".indexOf(s.charAt(p[0])) >= 0) {
            p[0]++;
        }
        if (start == p[0]) {
            throw LuaError.rt("Can't parse JSON");
        }
        try {
            return Args.n(Double.parseDouble(s.substring(start, p[0])));
        } catch (NumberFormatException e) {
            throw LuaError.rt("Can't parse JSON"); // NOPMD PreserveStackTrace - a script sees the message only; LuaError carries no cause
        }
    }

    private static void expect(String s, int[] p, char c) {
        if (p[0] >= s.length() || s.charAt(p[0]) != c) {
            throw LuaError.rt("Can't parse JSON");
        }
        p[0]++;
    }

    private static String string(String s, int[] p) {
        p[0]++;
        StringBuilder b = new StringBuilder();
        while (p[0] < s.length()) {
            char c = s.charAt(p[0]++);
            if (c == '"') {
                return b.toString();
            }
            if (c == '\\' && p[0] < s.length()) {
                char e = s.charAt(p[0]++);
                switch (e) {
                    case 'n':
                        b.append('\n');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 'b':
                        b.append('\b');
                        break;
                    case 'f':
                        b.append('\f');
                        break;
                    case 'u':
                        if (p[0] + 4 > s.length()) {
                            throw LuaError.rt("Can't parse JSON");
                        }
                        b.append((char) Integer.parseInt(s.substring(p[0], p[0] + 4), 16));
                        p[0] += 4;
                        break;
                    default:
                        b.append(e);
                }
            } else {
                b.append(c);
            }
        }
        throw LuaError.rt("Can't parse JSON");
    }
}
