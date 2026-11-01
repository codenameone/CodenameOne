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
package com.codename1.backend.log;

import java.util.Map;
import java.util.TreeMap;

/// A per-thread context map appended to every line logged on that thread, as
/// slf4j's MDC is: put a request id once and every line of that request carries
/// it.
///
/// ```java
/// MDC.put("request", requestId);
/// try {
///     handle(request);
/// } finally {
///     MDC.remove("request");
/// }
/// ```
///
/// The map belongs to the thread, a virtual thread included, so a value put by
/// one request is never seen by another. Remove what you put: a pooled thread
/// otherwise carries it into the next request it serves.
public final class MDC {
    private static final ThreadLocal<TreeMap<String, String>> CONTEXT = new ThreadLocal<TreeMap<String, String>>();

    private MDC() {
    }

    /// Sets `key` for every line this thread logs from now on; a null value
    /// removes it.
    public static void put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (value == null) {
            remove(key);
            return;
        }
        TreeMap<String, String> map = CONTEXT.get();
        if (map == null) {
            map = new TreeMap<String, String>();
            CONTEXT.set(map);
        }
        map.put(key, value);
    }

    /// The value this thread set for `key`, or null.
    public static String get(String key) {
        TreeMap<String, String> map = CONTEXT.get();
        return map == null ? null : map.get(key);
    }

    /// Removes `key` from this thread's context.
    public static void remove(String key) {
        TreeMap<String, String> map = CONTEXT.get();
        if (map != null) {
            map.remove(key);
            if (map.isEmpty()) {
                CONTEXT.set(null);
            }
        }
    }

    /// Empties this thread's context.
    public static void clear() {
        CONTEXT.set(null);
    }

    /// Appends this thread's context as ` {key=value, ...}`, or nothing when empty.
    static void appendTo(StringBuilder out) {
        TreeMap<String, String> map = CONTEXT.get();
        if (map == null || map.isEmpty()) {
            return;
        }
        out.append(" {");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        out.append('}');
    }
}
