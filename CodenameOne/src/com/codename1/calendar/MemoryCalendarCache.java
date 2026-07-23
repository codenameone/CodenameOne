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
package com.codename1.calendar;

import java.util.HashMap;
import java.util.Map;

/// Process-local cache, useful for tests or sessions that must leave no data on disk.
public class MemoryCalendarCache implements CalendarCache {

    private final Map<String, Map<String, Object>> states = new HashMap<String, Map<String, Object>>();

    @Override
    public synchronized Map<String, Object> load(String sourceId) {
        Map<String, Object> state = states.get(sourceId);
        return state == null ? new HashMap<String, Object>() : deepCopyMap(state);
    }

    @Override
    public synchronized void store(String sourceId, Map<String, Object> state) {
        states.put(sourceId, deepCopyMap(state));
    }

    @Override
    public synchronized void clear(String sourceId) {
        states.remove(sourceId);
    }

    // A shallow copy would share the nested mutation lists/maps by reference,
    // so a caller's later in-memory edits would silently alter the "persisted"
    // snapshot and two engines over one cache would share live mutable state.
    private static Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> out = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            out.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopyValue(Object value) {
        if (value instanceof Map) {
            return deepCopyMap((Map<String, Object>) value);
        }
        if (value instanceof java.util.List) {
            java.util.List<Object> out = new java.util.ArrayList<Object>();
            for (Object item : (java.util.List<Object>) value) {
                out.add(deepCopyValue(item));
            }
            return out;
        }
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            byte[] copy = new byte[bytes.length];
            System.arraycopy(bytes, 0, copy, 0, bytes.length);
            return copy;
        }
        return value;
    }
}
