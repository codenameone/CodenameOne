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
package com.codename1.backend.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// The JSONPath subset the matchers read: `$` then any of `.name`,
/// `['name']`, `[3]`, `[-1]`, `[*]`, `.*`, and a final `.length()`. A wildcard
/// turns the rest of the path into a list of every match.
final class JsonPath {
    /// What a path that leads nowhere reads as; distinct from a JSON null.
    static final Object MISSING = new Object() {
        @Override
        public String toString() {
            return "<missing>";
        }
    };

    /// True for [#MISSING], which is one object compared by identity.
    static boolean isMissing(Object value) {
        return value == MISSING; //NOPMD CompareObjectsWithEquals - a sentinel, by identity
    }

    private static final int NAME = 0;
    private static final int INDEX = 1;
    private static final int WILDCARD = 2;
    private static final int LENGTH = 3;

    private final List steps;

    private JsonPath(List steps) {
        this.steps = steps;
    }

    static JsonPath compile(String expression) {
        String e = expression.trim();
        if (!e.startsWith("$")) {
            throw new IllegalArgumentException("A JSON path starts with $: " + expression);
        }
        List steps = new ArrayList();
        int pos = 1;
        while (pos < e.length()) {
            char c = e.charAt(pos);
            if (c == '.') {
                pos++;
                if (pos < e.length() && e.charAt(pos) == '*') {
                    steps.add(new Object[] {Integer.valueOf(WILDCARD), null});
                    pos++;
                    continue;
                }
                int end = pos;
                while (end < e.length() && e.charAt(end) != '.' && e.charAt(end) != '[') {
                    end++;
                }
                String name = e.substring(pos, end);
                if ("length()".equals(name) && end == e.length()) {
                    steps.add(new Object[] {Integer.valueOf(LENGTH), null});
                } else if (name.length() == 0) {
                    throw new IllegalArgumentException("An empty name in JSON path " + expression);
                } else {
                    steps.add(new Object[] {Integer.valueOf(NAME), name});
                }
                pos = end;
            } else if (c == '[') {
                int close = e.indexOf(']', pos);
                if (close < 0) {
                    throw new IllegalArgumentException("An unclosed [ in JSON path " + expression);
                }
                String inner = e.substring(pos + 1, close).trim();
                if ("*".equals(inner)) {
                    steps.add(new Object[] {Integer.valueOf(WILDCARD), null});
                } else if (inner.length() >= 2 && (inner.charAt(0) == '\'' || inner.charAt(0) == '"')) {
                    steps.add(new Object[] {Integer.valueOf(NAME), inner.substring(1, inner.length() - 1)});
                } else {
                    try {
                        steps.add(new Object[] {Integer.valueOf(INDEX), Integer.valueOf(Integer.parseInt(inner))});
                    } catch (NumberFormatException err) {
                        throw new IllegalArgumentException("Not an index or a quoted name: [" + inner
                                + "] in JSON path " + expression, err);
                    }
                }
                pos = close + 1;
            } else {
                throw new IllegalArgumentException("Unexpected '" + c + "' in JSON path " + expression);
            }
        }
        return new JsonPath(steps);
    }

    Object read(Object root) {
        return walk(root, 0);
    }

    private Object walk(Object current, int step) {
        if (step == steps.size()) {
            return current;
        }
        if (current == MISSING) {
            return MISSING;
        }
        Object[] s = (Object[]) steps.get(step);
        int kind = ((Integer) s[0]).intValue();
        if (kind == NAME) {
            if (!(current instanceof Map) || !((Map) current).containsKey(s[1])) {
                return MISSING;
            }
            return walk(((Map) current).get(s[1]), step + 1);
        }
        if (kind == INDEX) {
            if (!(current instanceof List)) {
                return MISSING;
            }
            List list = (List) current;
            int index = ((Integer) s[1]).intValue();
            if (index < 0) {
                index += list.size();
            }
            if (index < 0 || index >= list.size()) {
                return MISSING;
            }
            return walk(list.get(index), step + 1);
        }
        if (kind == LENGTH) {
            if (current instanceof List) {
                return Integer.valueOf(((List) current).size());
            }
            if (current instanceof Map) {
                return Integer.valueOf(((Map) current).size());
            }
            if (current instanceof String) {
                return Integer.valueOf(((String) current).length());
            }
            return MISSING;
        }
        List out = new ArrayList();
        Iterator it;
        if (current instanceof List) {
            it = ((List) current).iterator();
        } else if (current instanceof Map) {
            it = ((Map) current).values().iterator();
        } else {
            return MISSING;
        }
        while (it.hasNext()) {
            Object value = walk(it.next(), step + 1);
            if (value != MISSING) {
                out.add(value);
            }
        }
        return out;
    }
}
