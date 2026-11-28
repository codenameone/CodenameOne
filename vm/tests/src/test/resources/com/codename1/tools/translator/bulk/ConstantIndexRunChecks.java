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

/// Runs of constant-index loads of one array (`b[0]`, `b[1]`, ... in straight-line
/// code): the translator checks the run's highest index once, at its first load,
/// and emits the rest unchecked. These hold the values and the exceptions to what
/// a load-by-load check gives. Each run is in a helper with no handler, which is
/// the only kind of method the translator forms a run in.
public final class ConstantIndexRunChecks {
    private ConstantIndexRunChecks() {
    }

    // Two little-endian words, as BLAKE2b reads its message: one run over
    // b[0]..b[15] spanning two statements.
    private static long[] le64x2(byte[] b) {
        long m0 = b[0] & 0xffL | (b[1] & 0xffL) << 8 | (b[2] & 0xffL) << 16 | (b[3] & 0xffL) << 24
                | (b[4] & 0xffL) << 32 | (b[5] & 0xffL) << 40 | (b[6] & 0xffL) << 48 | (long) b[7] << 56;
        long m1 = b[8] & 0xffL | (b[9] & 0xffL) << 8 | (b[10] & 0xffL) << 16 | (b[11] & 0xffL) << 24
                | (b[12] & 0xffL) << 32 | (b[13] & 0xffL) << 40 | (b[14] & 0xffL) << 48 | (long) b[15] << 56;
        return new long[] {m0, m1};
    }

    private static int ints(int[] a) {
        int x = a[0] + a[1] * 3;
        int y = a[2] * 5 - a[3];
        return x ^ y;
    }

    private static long longs(long[] a) {
        return a[0] + (a[1] << 1) + (a[2] << 2);
    }

    // A run that starts past index 0: b[2]..b[9].
    private static int fromTwo(byte[] b) {
        return b[2] + b[3] + b[4] + b[5] + b[6] + b[7] + b[8] + b[9];
    }

    private static byte[] bytes(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) (i * 37 - 100);
        }
        return b;
    }

    private static int reportedIndex(Bulk.Body body) {
        try {
            body.run();
        } catch (ArrayIndexOutOfBoundsException e) {
            String m = String.valueOf(e.getMessage());
            // The index is the first number in the message, in either JDK's wording.
            int i = 0;
            while (i < m.length() && (m.charAt(i) < '0' || m.charAt(i) > '9')) {
                i++;
            }
            int v = 0;
            boolean any = false;
            while (i < m.length() && m.charAt(i) >= '0' && m.charAt(i) <= '9') {
                v = v * 10 + (m.charAt(i) - '0');
                i++;
                any = true;
            }
            return any ? v : -2;
        } catch (Throwable t) {
            return -3;
        }
        return -1;
    }

    public static void run() {
        Bulk.check("ConstantIndexRun.values", () -> {
            long[] m = le64x2(bytes(16));
            Bulk.p("m0", m[0]);
            Bulk.p("m1", m[1]);
            Bulk.p("longer", le64x2(bytes(40))[1]);
            Bulk.p("ints", ints(new int[] {7, -3, 11, 1 << 30}));
            Bulk.p("longs", longs(new long[] {1L << 40, -5, 9}));
            Bulk.p("fromTwo", fromTwo(bytes(10)));
        });
        Bulk.check("ConstantIndexRun.exceptions", () -> {
            Bulk.pThrown("short", ArrayIndexOutOfBoundsException.class, () -> le64x2(bytes(5)));
            Bulk.pThrown("oneShort", ArrayIndexOutOfBoundsException.class, () -> le64x2(bytes(15)));
            Bulk.pThrown("null", NullPointerException.class, () -> le64x2(null));
            Bulk.pThrown("empty", ArrayIndexOutOfBoundsException.class, () -> ints(new int[0]));
            Bulk.pThrown("fromTwoShort", ArrayIndexOutOfBoundsException.class, () -> fromTwo(bytes(6)));
            Bulk.pThrown("fromTwoNone", ArrayIndexOutOfBoundsException.class, () -> fromTwo(bytes(2)));
        });
        // The index the exception names is the first load that would fail, as it
        // is checked load by load: length when the run starts inside the array,
        // the run's first index when it starts past the end. Message wording
        // differs between JDKs, so the index is compared, not the text.
        Bulk.check("ConstantIndexRun.failingIndex", () -> {
            Bulk.p("short", reportedIndex(() -> le64x2(bytes(5))));
            Bulk.p("oneShort", reportedIndex(() -> le64x2(bytes(15))));
            Bulk.p("secondWord", reportedIndex(() -> le64x2(bytes(9))));
            Bulk.p("fromTwoShort", reportedIndex(() -> fromTwo(bytes(6))));
            Bulk.p("fromTwoNone", reportedIndex(() -> fromTwo(bytes(1))));
        });
    }
}
