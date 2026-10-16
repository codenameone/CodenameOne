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

/**
 * Driver for the stop-the-world hold (cn1GcTryResumeActive in cn1_globals.h).
 *
 * <p>The main thread crosses a native resume -- Thread.sleep -- on every round, and right
 * after it stores freshly allocated objects into an OLD array, which is the shape a minor
 * cycle reaches only through the remembered set. A thread that slips out of the hold at
 * that resume runs exactly this code while the collector believes it is parked: it
 * allocates into pages the cycle retired as pre-cycle and stores young objects into
 * {@code sink} after the cycle took its remembered set, and the sweep frees them under the
 * live array. The test runs it with every resume window widened, once with the handshake
 * and once without it, and only the second may report a dangling reference.</p>
 *
 * <p>Every field it relies on is read at the end, so dead-field elimination cannot strip
 * the stores the hazard depends on.</p>
 */
public class GcResumeWindowApp {
    static class Filler {
        long a, b, c, d;
    }

    static Object[] sink = new Object[16];
    static long checksum;

    public static void main(String[] args) throws Exception {
        for (int r = 0; r < 6000; r++) {
            Thread.sleep(0);
            for (int i = 0; i < 4000; i++) {
                Filler f = new Filler();
                f.a = i;
                f.b = r;
                sink[i & 15] = f;
            }
            if ((r & 3) == 0) {
                System.gc();
            }
        }
        for (int i = 0; i < 16; i++) {
            Filler f = (Filler) sink[i];
            checksum += f.a + f.b + f.c + f.d;
        }
        System.out.println("RESULT=" + checksum);
        System.out.println("GC_RESUME_WINDOW_APP_DONE");
    }
}
