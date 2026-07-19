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
        Object value = Storage.getInstance().readObject(prefix + sourceId);
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
        if (!Storage.getInstance().writeObject(prefix + sourceId, state)) {
            throw new CalendarException(CalendarError.STORAGE, "Unable to write calendar cache");
        }
    }

    @Override
    public void clear(String sourceId) {
        Storage.getInstance().deleteStorageFile(prefix + sourceId);
    }
}
