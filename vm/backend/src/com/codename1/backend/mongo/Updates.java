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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/// Builds update documents; [#combine] merges several into one.
///
/// ```java
/// Updates.combine(Updates.set("name", "Ada"), Updates.inc("visits", 1))
/// ```
public final class Updates {
    private Updates() {
    }

    private static Map op(String op, String field, Object value) {
        Map inner = new LinkedHashMap();
        inner.put(field, value);
        Map m = new LinkedHashMap();
        m.put(op, inner);
        return m;
    }

    /// Sets `field` to `value`.
    public static Map set(String field, Object value) {
        return op("$set", field, value);
    }

    /// Removes `field`.
    public static Map unset(String field) {
        return op("$unset", field, "");
    }

    /// Adds `amount` to `field`.
    public static Map inc(String field, Number amount) {
        return op("$inc", field, amount);
    }

    /// Appends `value` to the array in `field`.
    public static Map push(String field, Object value) {
        return op("$push", field, value);
    }

    /// Adds `value` to the array in `field` unless it is already there.
    public static Map addToSet(String field, Object value) {
        return op("$addToSet", field, value);
    }

    /// Removes every element equal to `value` from the array in `field`.
    public static Map pull(String field, Object value) {
        return op("$pull", field, value);
    }

    /// One update with the operators of all of these.
    public static Map combine(Map... updates) {
        Map out = new LinkedHashMap();
        for (int i = 0; i < updates.length; i++) {
            Iterator it = updates[i].entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                Map existing = (Map) out.get(e.getKey());
                if (existing == null) {
                    existing = new LinkedHashMap();
                    out.put(e.getKey(), existing);
                }
                existing.putAll((Map) e.getValue());
            }
        }
        return out;
    }
}
