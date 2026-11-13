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
package com.codename1.backend.mongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Builds query filter documents.
///
/// ```java
/// Filters.and(Filters.eq("status", "open"), Filters.gte("total", 100))
/// ```
public final class Filters {
    private Filters() {
    }

    private static Map op(String field, String op, Object value) {
        Map inner = new LinkedHashMap();
        inner.put(op, value);
        Map m = new LinkedHashMap();
        m.put(field, inner);
        return m;
    }

    /// Matches every document.
    public static Map all() {
        return new LinkedHashMap();
    }

    /// `field` equals `value`.
    public static Map eq(String field, Object value) {
        return op(field, "$eq", value);
    }

    /// `field` does not equal `value`.
    public static Map ne(String field, Object value) {
        return op(field, "$ne", value);
    }

    /// `field` is greater than `value`.
    public static Map gt(String field, Object value) {
        return op(field, "$gt", value);
    }

    /// `field` is greater than or equal to `value`.
    public static Map gte(String field, Object value) {
        return op(field, "$gte", value);
    }

    /// `field` is less than `value`.
    public static Map lt(String field, Object value) {
        return op(field, "$lt", value);
    }

    /// `field` is less than or equal to `value`.
    public static Map lte(String field, Object value) {
        return op(field, "$lte", value);
    }

    /// `field` equals one of `values`.
    public static Map in(String field, Collection values) {
        return op(field, "$in", new ArrayList(values));
    }

    /// `field` equals none of `values`.
    public static Map nin(String field, Collection values) {
        return op(field, "$nin", new ArrayList(values));
    }

    /// `field` is present (or absent, when `exists` is false).
    public static Map exists(String field, boolean exists) {
        return op(field, "$exists", Boolean.valueOf(exists));
    }

    /// `field` matches a regular expression, with options such as `"i"`.
    public static Map regex(String field, String pattern, String options) {
        Map inner = new LinkedHashMap();
        inner.put("$regex", pattern);
        if (options != null) {
            inner.put("$options", options);
        }
        Map m = new LinkedHashMap();
        m.put(field, inner);
        return m;
    }

    /// Every filter holds.
    public static Map and(Map... filters) {
        return combine("$and", filters);
    }

    /// At least one filter holds.
    public static Map or(Map... filters) {
        return combine("$or", filters);
    }

    /// The filter does not hold.
    public static Map not(Map filter) {
        return combine("$nor", new Map[] {filter});
    }

    /// The collection's text index matches `search`.
    public static Map text(String search) {
        Map inner = new LinkedHashMap();
        inner.put("$search", search);
        Map m = new LinkedHashMap();
        m.put("$text", inner);
        return m;
    }

    private static Map combine(String op, Map[] filters) {
        List l = new ArrayList(filters.length);
        for (int i = 0; i < filters.length; i++) {
            l.add(filters[i]);
        }
        Map m = new LinkedHashMap();
        m.put(op, l);
        return m;
    }
}
