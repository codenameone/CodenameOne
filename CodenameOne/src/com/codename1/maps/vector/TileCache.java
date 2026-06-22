/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A bounded least-recently-used cache of rendered tiles keyed by their
/// `z/x/y` address. Bounding the in-memory tile set is the eviction the
/// legacy `WeakHashMap`-based tile cache lacked, keeping memory predictable
/// while panning across a large area.
final class TileCache {

    private final int maxEntries;
    private final Map map = new HashMap();
    private final List order = new ArrayList();

    TileCache(int maxEntries) {
        this.maxEntries = maxEntries < 1 ? 1 : maxEntries;
    }

    synchronized Object get(String key) {
        Object v = map.get(key);
        if (v != null) {
            order.remove(key);
            order.add(key);
        }
        return v;
    }

    synchronized boolean contains(String key) {
        return map.containsKey(key);
    }

    synchronized void put(String key, Object value) {
        if (map.containsKey(key)) {
            map.put(key, value);
            order.remove(key);
            order.add(key);
            return;
        }
        map.put(key, value);
        order.add(key);
        while (order.size() > maxEntries) {
            Object evicted = order.remove(0);
            map.remove(evicted);
        }
    }

    synchronized void clear() {
        map.clear();
        order.clear();
    }

    synchronized int size() {
        return map.size();
    }
}
