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
package com.codenameone.examples.brickyard.rbx.place;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The small XML subset place files use: elements, attributes, text, CDATA,
/// comments and the predefined and numeric entities. No DTDs, no
/// namespaces. Written here rather than taken from a platform parser because
/// the engine runs on targets that have none, and a place's script source
/// must come back byte for byte.
final class Xml {
    final String name;
    final Map<String, String> attrs = new LinkedHashMap<String, String>();
    final List<Xml> children = new ArrayList<Xml>();
    final StringBuilder text = new StringBuilder(); // NOPMD AvoidStringBufferField - element text accumulates across CDATA and entity runs

    Xml(String name) {
        this.name = name;
    }

    Xml child(String n) {
        for (Xml c : children) {
            if (c.name.equals(n)) {
                return c;
            }
        }
        return null;
    }

    String childText(String n) {
        Xml c = child(n);
        return c == null ? null : c.text.toString();
    }

    Xml add(String n) {
        Xml c = new Xml(n);
        children.add(c);
        return c;
    }

    Xml add(String n, String t) {
        Xml c = add(n);
        c.text.append(t);
        return c;
    }

    /// Parses a whole document and answers its root element.
    static Xml parse(String s) {
        int[] p = {0};
        Xml doc = new Xml("#document");
        List<Xml> stack = new ArrayList<Xml>();
        stack.add(doc);
        int n = s.length();
        while (p[0] < n) {
            Xml top = stack.get(stack.size() - 1);
            int lt = s.indexOf('<', p[0]);
            if (lt < 0) {
                appendText(top, s, p[0], n);
                break;
            }
            appendText(top, s, p[0], lt);
            if (s.startsWith("<!--", lt)) {
                p[0] = end(s, "-->", lt) + 3;
            } else if (s.startsWith("<![CDATA[", lt)) {
                int e = end(s, "]]>", lt);
                top.text.append(s, lt + 9, e);
                p[0] = e + 3;
            } else if (s.startsWith("<?", lt) || s.startsWith("<!", lt)) {
                p[0] = end(s, ">", lt) + 1;
            } else if (s.startsWith("</", lt)) {
                int gt = end(s, ">", lt);
                String nm = s.substring(lt + 2, gt).trim();
                if (stack.size() < 2 || !top.name.equals(nm)) {
                    throw new IllegalArgumentException("Mismatched </" + nm + "> at " + lt);
                }
                stack.remove(stack.size() - 1);
                p[0] = gt + 1;
            } else {
                p[0] = lt + 1;
                Xml el = new Xml(name(s, p));
                top.children.add(el);
                boolean selfClose = attributes(s, p, el);
                if (!selfClose) {
                    stack.add(el);
                }
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Unclosed <" + stack.get(stack.size() - 1).name + ">");
        }
        if (doc.children.isEmpty()) {
            throw new IllegalArgumentException("Empty document");
        }
        return doc.children.get(0);
    }

    private static int end(String s, String token, int from) {
        int e = s.indexOf(token, from);
        if (e < 0) {
            throw new IllegalArgumentException("Unterminated markup at " + from);
        }
        return e;
    }

    private static boolean isNameChar(char c) {
        return c > ' ' && c != '>' && c != '/' && c != '=' && c != '"' && c != '\'';
    }

    private static String name(String s, int[] p) {
        int st = p[0];
        while (p[0] < s.length() && isNameChar(s.charAt(p[0]))) {
            p[0]++;
        }
        return s.substring(st, p[0]);
    }

    private static void skipSpace(String s, int[] p) {
        while (p[0] < s.length() && s.charAt(p[0]) <= ' ') {
            p[0]++;
        }
    }

    /// Reads attributes up to the end of a start tag; true for `/>`.
    private static boolean attributes(String s, int[] p, Xml el) {
        while (true) {
            skipSpace(s, p);
            if (p[0] >= s.length()) {
                throw new IllegalArgumentException("Unterminated <" + el.name);
            }
            char c = s.charAt(p[0]);
            if (c == '>') {
                p[0]++;
                return false;
            }
            if (c == '/') {
                p[0] = end(s, ">", p[0]) + 1;
                return true;
            }
            String an = name(s, p);
            skipSpace(s, p);
            String val = "";
            if (p[0] < s.length() && s.charAt(p[0]) == '=') {
                p[0]++;
                skipSpace(s, p);
                char q = s.charAt(p[0]);
                int e = end(s, String.valueOf(q), p[0] + 1);
                StringBuilder b = new StringBuilder();
                decode(b, s, p[0] + 1, e);
                val = b.toString();
                p[0] = e + 1;
            }
            if (an.length() == 0) {
                throw new IllegalArgumentException("Bad attribute in <" + el.name);
            }
            el.attrs.put(an, val);
        }
    }

    private static void appendText(Xml top, String s, int from, int to) {
        if (from < to) {
            decode(top.text, s, from, to);
        }
    }

    private static void decode(StringBuilder out, String s, int from, int to) {
        int i = from;
        while (i < to) {
            char c = s.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            int semi = s.indexOf(';', i);
            if (semi < 0 || semi > to) {
                out.append(c);
                i++;
                continue;
            }
            String ent = s.substring(i + 1, semi);
            if ("lt".equals(ent)) {
                out.append('<');
            } else if ("gt".equals(ent)) {
                out.append('>');
            } else if ("amp".equals(ent)) {
                out.append('&');
            } else if ("quot".equals(ent)) {
                out.append('"');
            } else if ("apos".equals(ent)) {
                out.append('\'');
            } else if (ent.startsWith("#x") || ent.startsWith("#X")) {
                appendCodePoint(out, Integer.parseInt(ent.substring(2), 16));
            } else if (ent.startsWith("#")) {
                appendCodePoint(out, Integer.parseInt(ent.substring(1)));
            } else {
                out.append(s, i, semi + 1);
            }
            i = semi + 1;
        }
    }

    private static void appendCodePoint(StringBuilder out, int cp) {
        if (cp < 0x10000) {
            out.append((char) cp);
        } else {
            int v = cp - 0x10000;
            out.append((char) (0xD800 + (v >> 10)));
            out.append((char) (0xDC00 + (v & 0x3FF)));
        }
    }

    /// Escapes text for an element body or a double-quoted attribute.
    static void escape(StringBuilder out, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                default:
                    if (c < ' ' && c != '\n' && c != '\t') {
                        out.append("&#").append((int) c).append(';');
                    } else {
                        out.append(c);
                    }
            }
        }
    }
}
