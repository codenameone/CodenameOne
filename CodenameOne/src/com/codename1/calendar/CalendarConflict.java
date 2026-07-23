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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/// A queued local mutation that conflicts with the provider version.
public final class CalendarConflict {

    public enum Resolution {

        KEEP_LOCAL, KEEP_REMOTE, MERGED
    }

    private final String mutationId;

    private final Map<String, Object> local;

    private final Map<String, Object> remote;

    CalendarConflict(String mutationId, Map<String, Object> local, Map<String, Object> remote) {
        this.mutationId = mutationId;
        this.local = immutableMap(local);
        this.remote = immutableMap(remote);
    }

    public String getMutationId() {
        return mutationId;
    }

    public Map<String, Object> getLocal() {
        return local;
    }

    public Map<String, Object> getRemote() {
        return remote;
    }

    private static Map<String, Object> immutableMap(Map source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> copy = new HashMap<String, Object>();
        for (Object entryObject : source.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            copy.put(String.valueOf(entry.getKey()), immutableValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map) {
            return immutableMap((Map) value);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<Object>();
            for (Object item : (List) value) {
                copy.add(immutableValue(item));
            }
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }
        return value;
    }
}
