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
import java.lang.ref.WeakReference;

public class WeakLegacyArrayApp {
    static WeakReference[] cache = new WeakReference[128];
    static volatile int sink;

    static void populate() {
        for (int i = 0; i < cache.length; i++) {
            int[] pixels = new int[32768]; // larger than a BiBOP slot: legacy heap
            pixels[0] = i;
            pixels[pixels.length - 1] = ~i;
            cache[i] = new WeakReference(pixels);
        }
    }

    public static void main(String[] args) throws Exception {
        int cleared = 0;
        for (int round = 0; round < 8; round++) {
            populate();
            // Leave only weak edges before the first collection of these arrays.
            System.gc();
            Thread.sleep(300);
            for (int j = 0; j < 256; j++) {
                int[] replacement = new int[32768];
                for (int k = 0; k < replacement.length; k++) replacement[k] = 0x900f765d;
                sink = replacement[123];
            }
            for (int i = 0; i < cache.length; i++) {
                int[] pixels = (int[]) cache[i].get();
                if (pixels == null) {
                    cleared++;
                } else if (pixels.length != 32768 || pixels[0] != i || pixels[32767] != ~i) {
                    throw new RuntimeException("Reclaimed array returned by weak cache");
                }
            }
        }
        if (cleared == 0) throw new RuntimeException("No weak arrays collected");
        System.out.println("WEAK_LEGACY_ARRAY_OK");
    }
}
