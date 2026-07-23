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
 * A/B driver for the issue-5425 pacing question: a single thread that
 * sustains a small-object allocation storm (the Dtest dictionary-load shape)
 * with a modest retained set. Compare wall time and peak RSS across VM
 * revisions.
 */
public class StormAB {
    static class Filler {
        long a, b, c, d, e, f, g, h, i2, j, k, l;
    }

    static Object[] tmp = new Object[16];
    static long checksum;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int round = 0; round < 40; round++) {
            for (int i = 0; i < 1000000; i++) {
                Filler f = new Filler();
                f.a = i;
                tmp[i & 15] = f;
            }
        }
        for (int i = 0; i < 16; i++) {
            if (tmp[i] != null) {
                checksum++;
            }
        }
        System.out.println("STORM_AB_DONE checksum=" + checksum
                + " ms=" + (System.currentTimeMillis() - start));
    }
}
