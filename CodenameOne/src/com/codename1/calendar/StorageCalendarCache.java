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

import com.codename1.io.Storage;
import java.util.HashMap;
import java.util.Map;

/// Opt-in persistent cache backed by application `Storage`.
public class StorageCalendarCache implements CalendarCache {

    private final String prefix;

    public StorageCalendarCache(String namespace) {
        if (namespace == null || namespace.length() == 0) {
            throw new IllegalArgumentException("namespace required");
        }
        prefix = "cn1-calendar-" + namespace + "-";
    }

    @Override
    public Map<String, Object> load(String sourceId) throws CalendarException {
        Object first = Storage.getInstance().readObject(key(sourceId, 0));
        Object second = Storage.getInstance().readObject(key(sourceId, 1));
        Map wrapper = newer(first, second);
        Object value = wrapper == null ? Storage.getInstance().readObject(prefix + sourceId) : wrapper.get("state");
        if (value == null) {
            return new HashMap<String, Object>();
        }
        if (!(value instanceof Map)) {
            throw new CalendarException(CalendarError.STORAGE, "Invalid calendar cache data");
        }
        return new HashMap<String, Object>((Map) value);
    }

    @Override
    public void store(String sourceId, Map<String, Object> state) throws CalendarException {
        Storage storage = Storage.getInstance();
        Object first = storage.readObject(key(sourceId, 0));
        Object second = storage.readObject(key(sourceId, 1));
        Map newest = newer(first, second);
        long generation = newest == null ? 1L : number(newest.get("generation")) + 1L;
        int slot = newest == first ? 1 : 0; //NOPMD CompareObjectsWithEquals
        Map<String, Object> wrapper = new HashMap<String, Object>();
        wrapper.put("generation", Long.valueOf(generation));
        wrapper.put("state", new HashMap<String, Object>(state));
        if (!storage.writeObject(key(sourceId, slot), wrapper)) {
            storage.clearCache();
            throw new CalendarException(CalendarError.STORAGE, "Unable to write calendar cache");
        }
    }

    @Override
    public void clear(String sourceId) {
        Storage.getInstance().deleteStorageFile(prefix + sourceId);
        Storage.getInstance().deleteStorageFile(key(sourceId, 0));
        Storage.getInstance().deleteStorageFile(key(sourceId, 1));
    }

    private String key(String sourceId, int slot) {
        return prefix + sourceId + "-" + slot;
    }

    private static Map newer(Object first, Object second) {
        Map a = wrapper(first);
        Map b = wrapper(second);
        return a == null ? b : b == null ? a
                : number(a.get("generation")) >= number(b.get("generation")) ? a : b;
    }

    private static Map wrapper(Object value) {
        if (!(value instanceof Map)) {
            return null;
        }
        Map map = (Map) value;
        return map.get("state") instanceof Map && map.get("generation") instanceof Number ? map : null;
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
