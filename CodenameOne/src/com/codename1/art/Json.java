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
package com.codename1.art;

import java.util.List;
import java.util.Map;

/// Typed reads of parsed JSON values, checked with `instanceof`.
final class Json {
    private Json() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object m, String k) {
        Object o = m instanceof Map ? ((Map<String, Object>) m).get(k) : null;
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObj(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object m, String k) {
        Object o = m instanceof Map ? ((Map<String, Object>) m).get(k) : null;
        return o instanceof List ? (List<Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    static double num(Object m, String k, double def) {
        Object o = m instanceof Map ? ((Map<String, Object>) m).get(k) : null;
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue() ? 1 : 0;
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    static boolean bool(Object m, String k, boolean def) {
        Object o = m instanceof Map ? ((Map<String, Object>) m).get(k) : null;
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue() != 0;
        }
        if (o instanceof String) {
            return "true".equals(o);
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    static String str(Object m, String k) {
        Object o = m instanceof Map ? ((Map<String, Object>) m).get(k) : null;
        return o instanceof String ? (String) o : null;
    }
}
