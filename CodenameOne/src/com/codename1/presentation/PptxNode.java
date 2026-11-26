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
package com.codename1.presentation;

import com.codename1.impl.xml.XmlScanner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A small element tree of one part of a PowerPoint package. The scanner cannot list the
/// attributes of an element, so a fixed set of the names the reader asks for is copied into
/// each node; prefixes are cut from element names, and an attribute is kept under the name it
/// is asked for (`r:id`, `r:embed`, `r:link` keep their prefix).
final class PptxNode {
    /// The deepest element kept; anything deeper is skipped.
    static final int MAX_DEPTH = 256;

    /// The most elements one part may have.
    static final int MAX_NODES = 400000;

    private static final String[] ATTRS = {"id", "name", "descr", "type", "idx", "x", "y", "cx", "cy",
        "rot", "flipH", "flipV", "prst", "fmla", "val", "sz", "b", "i", "u", "strike", "algn",
        "lvl", "marL", "indent", "typeface", "r:id", "r:embed", "r:link", "l", "t", "r",
        "lIns", "tIns", "rIns", "bIns", "anchor", "wrap", "w", "h", "gridSpan", "rowSpan",
        "hMerge", "vMerge", "showMasterSp", "show", "spd", "dur", "advTm", "advClick", "dir",
        "presetClass", "presetID", "presetSubtype", "nodeType", "spid", "Id", "Type", "Target",
        "TargetMode", "lastClr", "ang", "pos", "startAt", "char", "baseline", "firstRow",
        "delay", "uri", "Requires", "len"};

    final String name;
    private final String[] values = new String[ATTRS.length];
    final List<PptxNode> children = new ArrayList<PptxNode>();
    private StringBuilder text; //NOPMD AvoidStringBufferField - one buffer per element while it is parsed

    private PptxNode(String name) {
        this.name = name;
    }

    /// Parses a part, null when it is not well formed enough to give a root.
    static PptxNode parse(byte[] data, long[] budget, List<String> warnings) throws IOException {
        if (data == null) {
            return null;
        }
        XmlScanner x = new XmlScanner(new ByteArrayInputStream(data), budget);
        List<PptxNode> stack = new ArrayList<PptxNode>();
        PptxNode root = null;
        int count = 0;
        while (true) {
            int e;
            try {
                e = x.next();
            } catch (IOException err) {
                if (root != null && stack.isEmpty()) {
                    // Junk after the root element ends: what was read is whole.
                    break;
                }
                throw err;
            }
            if (e == XmlScanner.EOF) {
                break;
            }
            if (e == XmlScanner.START) {
                if (stack.size() >= MAX_DEPTH || count >= MAX_NODES) {
                    warnings.add("Content nested or repeated too deeply was skipped");
                    x.skipElement();
                    continue;
                }
                count++;
                PptxNode n = new PptxNode(x.name);
                for (int i = 0; i < ATTRS.length; i++) {
                    n.values[i] = x.attr(ATTRS[i]);
                }
                if (stack.isEmpty()) {
                    if (root != null) {
                        break;
                    }
                    root = n;
                } else {
                    stack.get(stack.size() - 1).children.add(n);
                }
                stack.add(n);
            } else if (e == XmlScanner.END) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else if (!stack.isEmpty()) {
                PptxNode top = stack.get(stack.size() - 1);
                if (top.text == null) {
                    top.text = new StringBuilder();
                }
                if (top.text.length() < XmlScanner.MAX_TEXT) {
                    top.text.append(x.text);
                }
            }
        }
        return root;
    }

    /// An attribute, null when absent or not one of the names kept.
    String attr(String n) {
        for (int i = 0; i < ATTRS.length; i++) {
            if (ATTRS[i].equals(n)) {
                return values[i];
            }
        }
        return null;
    }

    /// A finite number attribute clamped to a sane range, or the fallback.
    double num(String n, double fallback) {
        return number(attr(n), fallback);
    }

    static double number(String v, double fallback) {
        if (v == null) {
            return fallback;
        }
        try {
            double d = Double.parseDouble(v.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return fallback;
            }
            return Math.max(-1e12, Math.min(1e12, d));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /// True for "1" or "true".
    boolean flag(String n, boolean fallback) {
        String v = attr(n);
        if (v == null) {
            return fallback;
        }
        return "1".equals(v) || "true".equals(v) || "on".equals(v);
    }

    /// The text directly inside this element.
    String text() {
        return text == null ? "" : text.toString();
    }

    /// The first direct child of that name, or null.
    PptxNode child(String n) {
        for (PptxNode c : children) {
            if (c.name.equals(n)) {
                return c;
            }
        }
        return null;
    }

    /// Follows a path of direct children.
    PptxNode path(String... names) {
        PptxNode n = this;
        for (int i = 0; i < names.length && n != null; i++) {
            n = n.child(names[i]);
        }
        return n;
    }

    /// Every direct child of that name.
    List<PptxNode> all(String n) {
        List<PptxNode> out = new ArrayList<PptxNode>();
        for (PptxNode c : children) {
            if (c.name.equals(n)) {
                out.add(c);
            }
        }
        return out;
    }

    /// The first descendant of that name, depth first, skipping extension lists.
    PptxNode find(String n) {
        return find(this, n, 0);
    }

    private static PptxNode find(PptxNode at, String n, int depth) {
        if (depth > MAX_DEPTH) {
            return null;
        }
        for (PptxNode c : at.children) {
            if (c.name.equals(n)) {
                return c;
            }
            if (!"extLst".equals(c.name)) {
                PptxNode f = find(c, n, depth + 1);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    /// The text of every descendant `t` element, concatenated.
    String allText() {
        StringBuilder sb = new StringBuilder();
        collect(this, sb, 0);
        return sb.toString();
    }

    private static void collect(PptxNode at, StringBuilder sb, int depth) {
        if (depth > MAX_DEPTH || sb.length() > XmlScanner.MAX_TEXT) {
            return;
        }
        if ("t".equals(at.name) || "v".equals(at.name)) {
            sb.append(at.text());
        }
        for (int i = 0; i < at.children.size(); i++) {
            collect(at.children.get(i), sb, depth + 1);
        }
    }
}
