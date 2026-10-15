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
package com.codenameone.examples.wayline;

import com.codenameone.examples.wayline.api.RideState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Reading the rows a repository answers for a select of several values,
/// `select r.driver, count(r) ...`, which come back as `Object[]`.
///
/// What a column holds is checked and never cast blindly: a count is a `Long`
/// on one database and an `Integer` on another, and a state is the enum or
/// its name depending on how the column was read.
public final class Rows {
    private Rows() {
    }

    /// A column as a whole number; 0 for null or anything that is not a
    /// number.
    public static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /// A column as text; empty for null.
    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /// A column holding a ride's state, or null for one that names none.
    public static RideState state(Object value) {
        if (value instanceof RideState) {
            return (RideState) value;
        }
        String name = text(value);
        RideState[] all = RideState.values();
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter].name().equals(name)) {
                return all[iter];
            }
        }
        return null;
    }

    /// Rows of `{name, count}` as a map from the name to the count.
    public static Map<String, Long> counts(List<Object[]> rows) {
        Map<String, Long> out = new HashMap<String, Long>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Object[] row = rows.get(iter);
            out.put(text(row[0]), Long.valueOf(number(row[1])));
        }
        return out;
    }

    /// A single number a repository answers, which is null for a sum or a
    /// maximum of no rows.
    public static long number(Long value) {
        return value == null ? 0L : value.longValue();
    }
}
