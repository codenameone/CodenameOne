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
package com.demo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HashMap against LinkedHashMap on the translated target.
 *
 * HashMap's get/put/remove are NATIVE in ParparVM (open addressing over parallel
 * arrays, see nativeMethods.m). LinkedHashMap extends it but overrides exactly
 * those methods in Java to maintain its ordering links, so it cannot reach the C
 * fast path -- and its put allocates a CompactEntry per call to hand to
 * removeEldestEntry. This measures what that costs.
 */
public class MapBench {
    static final int N = 4;          // the shape a JSON codec builds
    static final int ITERS = 2000000;

    static long fill(boolean linked) {
        long t0 = System.nanoTime();
        for (int i = 0; i < ITERS; i++) {
            Map m = linked ? new LinkedHashMap() : new HashMap();
            m.put("name", "value");
            m.put("email", "x@example.com");
            m.put("id", new Long(42));
            m.put("active", Boolean.TRUE);
            if (m.size() != N) {
                throw new IllegalStateException("bad size");
            }
        }
        return System.nanoTime() - t0;
    }

    static long lookup(boolean linked) {
        Map m = linked ? new LinkedHashMap() : new HashMap();
        m.put("name", "value");
        m.put("email", "x@example.com");
        m.put("id", new Long(42));
        m.put("active", Boolean.TRUE);
        long t0 = System.nanoTime();
        long sink = 0;
        for (int i = 0; i < ITERS; i++) {
            if (m.get("email") != null) {
                sink++;
            }
        }
        if (sink != ITERS) {
            throw new IllegalStateException("bad sink");
        }
        return System.nanoTime() - t0;
    }

    public static void main(String[] args) {
        fill(false); fill(true); lookup(false); lookup(true);   // warm
        for (int rep = 1; rep <= 3; rep++) {
            long hf = fill(false), lf = fill(true);
            long hg = lookup(false), lg = lookup(true);
            System.out.println("rep" + rep
                    + "  build: hash=" + (hf / ITERS) + "ns linked=" + (lf / ITERS)
                    + "ns (" + (lf * 100 / hf) + "% of hash)"
                    + "   get: hash=" + (hg / ITERS) + "ns linked=" + (lg / ITERS)
                    + "ns (" + (lg * 100 / hg) + "%)");
        }
    }
}
