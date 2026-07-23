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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Registry and entry point for device and online calendar sources.
public final class CalendarManager {

    private static final CalendarManager INSTANCE = new CalendarManager();

    private final Map<String, CalendarSource> sources = new LinkedHashMap<String, CalendarSource>();

    private CalendarManager() {
    }

    public static CalendarManager getInstance() {
        return INSTANCE;
    }

    public synchronized CalendarManager registerSource(CalendarSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source required");
        }
        CalendarSource existing = sources.get(source.getId());
        if (existing != null && existing != source) {
            throw new IllegalArgumentException("A calendar source with id '" + source.getId() + "' is already registered");
        }
        sources.put(source.getId(), source);
        return this;
    }

    public synchronized CalendarSource removeSource(String id) {
        return sources.remove(id);
    }

    public synchronized CalendarSource getSource(String id) {
        return sources.get(id);
    }

    public synchronized List<CalendarSource> getSources() {
        return Collections.unmodifiableList(new ArrayList<CalendarSource>(sources.values()));
    }

    public synchronized LocalCalendarSource getLocalSource() {
        LocalCalendarSource local = LocalCalendarSource.getInstance();
        CalendarSource existing = sources.get(local.getId());
        if (existing != null && existing != local) {
            throw new IllegalStateException("A calendar source with id '" + local.getId() + "' is already registered");
        }
        sources.put(local.getId(), local);
        return local;
    }
}
