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

import java.util.Hashtable;

/**
 * Models the issue-5425 looping Dtest: repeatedly build a large dictionary
 * (Hashtable of String -> small byte[]), drop the previous one, and report
 * per-round wall time. Steady-state per-round time should be flat; growth
 * means the collector degrades as loads repeat.
 */
public class LoadLoop {
    static Hashtable dict;

    public static void main(String[] args) {
        for (int round = 0; round < 12; round++) {
            long start = System.currentTimeMillis();
            Hashtable h = new Hashtable();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 120000; i++) {
                sb.setLength(0);
                sb.append("word");
                sb.append(i);
                String key = sb.toString();
                byte[] def = new byte[16 + (i & 31)];
                def[0] = (byte) i;
                h.put(key, def);
            }
            dict = h;
            long ms = System.currentTimeMillis() - start;
            System.out.println("round " + round + " ms=" + ms + " size=" + h.size());
        }
        System.out.println("LOAD_LOOP_DONE");
    }
}
