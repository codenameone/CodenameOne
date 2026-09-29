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
package com.bench;

import java.util.ArrayList;
import java.util.HashMap;

/** Finalizer exceptions must not skip native ownership cleanup or kill the collector. */
public class ThrowingFinalizer {
    private static volatile int finalized;
    private static volatile int unrequestedSuperCalls;
    private static class ListBase extends ArrayList<Object> {
        protected void finalize() { unrequestedSuperCalls++; }
    }
    private static final class ListOwner extends ListBase {
        protected void finalize() { finalized++; throw new IllegalStateException("list finalizer"); }
    }
    private static final class MapOwner extends HashMap<Object, Object> {
        protected void finalize() { finalized++; throw new IllegalStateException("map finalizer"); }
    }
    private static void discard() {
        for (int i = 0; i < 32; i++) {
            ListOwner list = new ListOwner();
            MapOwner map = new MapOwner();
            for (int j = 0; j < 64; j++) { list.add(new Object()); map.put(new Object(), new Object()); }
        }
    }
    public static void main(String[] args) throws Exception {
        // Conservative stack scanning may retain the last few dead references.
        discard();
        for (int round = 0; round < 200 && finalized < 60; round++) {
            // Retire the active allocation pages too; sweeping only closed pages
            // cannot finalize a tiny batch while its allocation page stays active.
            discard();
            System.gc();
            Thread.sleep(50);
        }
        if (finalized < 60) { System.out.println("finalized=" + finalized); throw new AssertionError("Finalizers stopped"); }
        int firstGeneration = finalized;
        // A second generation proves the collector survived the first exceptions.
        discard();
        for (int round = 0; round < 200 && finalized < firstGeneration + 60; round++) {
            // Retire the active allocation pages too; sweeping only closed pages
            // cannot finalize a tiny batch while its allocation page stays active.
            discard();
            System.gc();
            Thread.sleep(50);
        }
        if (finalized < firstGeneration + 60) { System.out.println("finalized=" + finalized); throw new AssertionError("Collector stopped"); }
        if (unrequestedSuperCalls != 0) throw new AssertionError("Implicit super.finalize call");
        System.out.println("THROWING_FINALIZERS_OK");
    }
}
