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
package com.codename1.backend;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Synthetic descriptors for the Java SE runtime.
 *
 * The shared code above -- HttpServer, StaticFiles -- deals in int descriptors,
 * because on the translated target that is what they are. The JVM will not hand
 * out a real fd portably, so this maps a synthetic int onto the channel it stands
 * for. Nothing above needs to know.
 *
 * Ids start above the numbers a real process would use for stdin/stdout/stderr so
 * a stray 0, 1 or 2 cannot be mistaken for a live descriptor.
 */
final class Descriptors {
    private static final AtomicInteger NEXT = new AtomicInteger(64);
    private static final Map<Integer, Object> ENTRIES = new ConcurrentHashMap<Integer, Object>();

    private Descriptors() {
    }

    static int add(Object entry) {
        int id = NEXT.getAndIncrement();
        ENTRIES.put(Integer.valueOf(id), entry);
        return id;
    }

    static Object get(int id) {
        return ENTRIES.get(Integer.valueOf(id));
    }

    static Object remove(int id) {
        return ENTRIES.remove(Integer.valueOf(id));
    }

    static void closeQuietly(Object entry) {
        if(entry instanceof Channel) {
            try {
                ((Channel)entry).close();
            } catch (IOException ignored) {
                // already gone
            }
        }
    }
}
