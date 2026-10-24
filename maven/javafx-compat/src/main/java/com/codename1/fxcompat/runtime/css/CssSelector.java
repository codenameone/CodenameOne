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
package com.codename1.fxcompat.runtime.css;

import java.util.ArrayList;

/// One selector: a chain of compound selectors joined by descendant and
/// child combinators, such as `.sidebar > Button.primary:hover`.
///
/// The compounds are held from the subject outwards: compound 0 is the
/// rightmost one, the node the rule styles, and each further compound is an
/// ancestor of the one before it. That is the order a matcher needs them
/// in, since it starts at the node and walks up.
///
/// A compound is a type name or `*`, at most one id, and any number of
/// style classes and pseudo-classes. Names are compared as written: JavaFX
/// type selectors and style classes are case sensitive.
public final class CssSelector {

    /// The compound is anywhere above the one before it.
    public static final byte DESCENDANT = 1;
    /// The compound is the parent of the one before it.
    public static final byte CHILD = 2;

    private static final String[] NONE = new String[0];

    private final String[] types;
    private final String[] ids;
    private final String[][] classes;
    private final String[][] pseudos;
    private final byte[] combinators;
    private final int specificity;

    /// Creates a selector from its compounds, subject first. `types[i]`
    /// and `ids[i]` are `null` for a compound without one;
    /// `combinators[i]` says how compound `i + 1` relates to compound `i`
    /// and has one entry fewer than there are compounds. The arrays are
    /// taken as they are.
    public CssSelector(String[] types, String[] ids, String[][] classes, String[][] pseudos, byte[] combinators) {
        this.types = types;
        this.ids = ids;
        this.classes = classes;
        this.pseudos = pseudos;
        this.combinators = combinators;
        int a = 0;
        int b = 0;
        int c = 0;
        for (int i = 0; i < types.length; i++) {
            a += ids[i] == null ? 0 : 1;
            b += classes[i].length + pseudos[i].length;
            c += types[i] == null ? 0 : 1;
        }
        this.specificity = (Math.min(a, 255) << 16) | (Math.min(b, 255) << 8) | Math.min(c, 255);
    }

    /// How many compounds the selector has; at least one.
    public int size() {
        return types.length;
    }

    /// The type name of compound `i`, or `null` when it matches any type.
    public String type(int i) {
        return types[i];
    }

    /// The id of compound `i`, or `null`.
    public String id(int i) {
        return ids[i];
    }

    /// How many style classes compound `i` requires.
    public int classCount(int i) {
        return classes[i].length;
    }

    /// Style class `j` of compound `i`.
    public String styleClass(int i, int j) {
        return classes[i][j];
    }

    /// How many pseudo-classes compound `i` requires.
    public int pseudoCount(int i) {
        return pseudos[i].length;
    }

    /// Pseudo-class `j` of compound `i`.
    public String pseudo(int i, int j) {
        return pseudos[i][j];
    }

    /// How compound `i + 1` relates to compound `i`: [#DESCENDANT] or
    /// [#CHILD].
    public byte combinator(int i) {
        return combinators[i];
    }

    /// The specificity as one number that orders selectors the way CSS
    /// does: ids, then classes and pseudo-classes, then types.
    public int specificity() {
        return specificity;
    }

    /// Parses one selector -- no commas; a selector list is split by the
    /// caller. Answers `null` for text that is not a selector this layer
    /// supports.
    public static CssSelector parse(String text) {
        ArrayList<String> types = new ArrayList<String>();
        ArrayList<String> ids = new ArrayList<String>();
        ArrayList<String[]> classes = new ArrayList<String[]>();
        ArrayList<String[]> pseudos = new ArrayList<String[]>();
        ArrayList<Byte> combinators = new ArrayList<Byte>();
        String s = text.trim();
        int n = s.length();
        int i = 0;
        boolean first = true;
        while (i < n) {
            // What separates this compound from the one before it.
            byte combinator = first ? 0 : DESCENDANT;
            boolean space = false;
            while (i < n && (CssValueParser.isSpace(s.charAt(i)) || s.charAt(i) == '>')) {
                if (s.charAt(i) == '>') {
                    if (first || combinator == CHILD) {
                        return null;
                    }
                    combinator = CHILD;
                } else {
                    space = true;
                }
                i++;
            }
            if (i >= n) {
                // A combinator with nothing after it.
                return combinator == CHILD ? null : build(types, ids, classes, pseudos, combinators);
            }
            if (!first && combinator == DESCENDANT && !space) {
                return null;
            }
            String type = null;
            String id = null;
            ArrayList<String> cls = new ArrayList<String>();
            ArrayList<String> pse = new ArrayList<String>();
            boolean any = false;
            int start = i;
            while (i < n && !CssValueParser.isSpace(s.charAt(i)) && s.charAt(i) != '>') {
                char kind = s.charAt(i);
                if (kind == '*') {
                    if (i != start) {
                        return null;
                    }
                    any = true;
                    i++;
                    continue;
                }
                int from = kind == '#' || kind == '.' || kind == ':' ? i + 1 : i;
                int end = from;
                while (end < n && isNameChar(s.charAt(end))) {
                    end++;
                }
                if (end == from) {
                    return null;
                }
                String name = s.substring(from, end);
                if (kind == '#') {
                    if (id != null) {
                        return null;
                    }
                    id = name;
                } else if (kind == '.') {
                    cls.add(name);
                } else if (kind == ':') {
                    pse.add(name);
                } else {
                    if (i != start || any) {
                        return null;
                    }
                    type = name;
                }
                i = end;
            }
            if (!first) {
                combinators.add(Byte.valueOf(combinator));
            }
            types.add(type);
            ids.add(id);
            classes.add(cls.toArray(NONE));
            pseudos.add(pse.toArray(NONE));
            first = false;
        }
        return build(types, ids, classes, pseudos, combinators);
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_'
                || c > 127;
    }

    /// Reverses what was read left to right into subject-first order.
    private static CssSelector build(ArrayList<String> types, ArrayList<String> ids, ArrayList<String[]> classes,
            ArrayList<String[]> pseudos, ArrayList<Byte> combinators) {
        int n = types.size();
        if (n == 0) {
            return null;
        }
        String[] t = new String[n];
        String[] d = new String[n];
        String[][] c = new String[n][];
        String[][] p = new String[n][];
        byte[] k = new byte[n - 1];
        for (int i = 0; i < n; i++) {
            int src = n - 1 - i;
            t[i] = types.get(src);
            d[i] = ids.get(src);
            c[i] = classes.get(src);
            p[i] = pseudos.get(src);
            if (i < n - 1) {
                // Written between compound src - 1 and src.
                k[i] = combinators.get(src - 1).byteValue();
            }
        }
        return new CssSelector(t, d, c, p, k);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int i = types.length - 1; i >= 0; i--) {
            s.append(types[i] == null ? "" : types[i]);
            if (ids[i] != null) {
                s.append('#').append(ids[i]);
            }
            for (int j = 0; j < classes[i].length; j++) {
                s.append('.').append(classes[i][j]);
            }
            for (int j = 0; j < pseudos[i].length; j++) {
                s.append(':').append(pseudos[i][j]);
            }
            if (types[i] == null && ids[i] == null && classes[i].length == 0 && pseudos[i].length == 0) {
                s.append('*');
            }
            if (i > 0) {
                s.append(combinators[i - 1] == CHILD ? " > " : " ");
            }
        }
        return s.toString();
    }
}
