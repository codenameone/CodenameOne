/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

/**
 * Cost of the bulk SATB barrier's registration on the off-mark path.
 *
 * <p>Object-array copies only, no allocation, so no collection runs and every copy takes
 * the barrier's entry protocol and finds the mark inactive. This is what prices
 * {@code cn1SatbBulkEnter}'s registration against the flag precheck it replaced.</p>
 */
public class ObjCopyCost {
    private static final int LEN = 32;
    private static final int ITERS = 40000000;

    static Object[] src = new Object[LEN];
    static Object[] dst = new Object[LEN];
    static long checksum;

    public static void main(String[] args) {
        for (int i = 0; i < LEN; i++) { src[i] = new Object(); }
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < ITERS; i++) {
            System.arraycopy(src, 0, dst, 0, LEN);
            if (dst[i & (LEN - 1)] != null) { checksum++; }
        }
        long ms = System.currentTimeMillis() - t0;
        System.out.println("COPY_MS=" + ms);
        System.out.println("RESULT=" + checksum);
        System.out.println("OBJ_COPY_COST_DONE");
    }
}
