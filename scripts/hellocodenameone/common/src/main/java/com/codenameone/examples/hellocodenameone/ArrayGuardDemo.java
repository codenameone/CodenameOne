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
package com.codenameone.examples.hellocodenameone;

/**
 * Asserts that array access is memory-safe on whatever VM is running the app:
 * an out-of-range index must raise ArrayIndexOutOfBoundsException and a null
 * array must raise NullPointerException, rather than reading or writing past
 * the allocation.
 *
 * <p>This exists because the failure it guards is silent. ParparVM compiled the
 * bounds check out of release builds, so an out-of-range index became a raw C
 * pointer read: adjacent heap bytes most of the time, a hard crash when it
 * crossed an unmapped page (issue 5482). Nothing in the suite noticed, because
 * a corrupted read still returns a value and the app carries on with wrong data.
 *
 * <p>The out-of-bounds cases are the ones that need an explicit check. A null
 * dereference faults, so iOS/tvOS/watchOS convert it to NullPointerException via
 * the SIGSEGV handler in CodenameOne_GLAppDelegate.m even with no check emitted.
 * An out-of-range index usually does NOT fault -- it lands inside the process's
 * own heap -- so no signal arrives and no handler can help. That case is only
 * caught if the VM actually emits the check, which is exactly what this asserts.
 *
 * <p>Every value below is routed through a field or a method so neither javac
 * nor the translator's bounds-check-elimination pass can fold the access away
 * and turn the assertion into a tautology.
 */
final class ArrayGuardDemo {
    /**
     * Holder read through a second field. That indirection is what keeps the
     * translator from reducing the access into cn1_array_element_*, so it lands
     * on the stack-machine emission path (CHECK_ARRAY_ACCESS) -- the one that
     * compiled to an unchecked raw pointer read. A simpler `arr[i]` reduces and
     * would assert nothing, since the reduced paths always checked.
     */
    private static final class Holder {
        int[] a = new int[3];
        /** Never assigned, so the ARRAY is null while the holder is not. */
        int[] missing;
    }

    private static final Holder HOLDER = new Holder();

    private int[] table = new int[3];
    private int pointer;
    private int size = 6;

    /** Non-final so the indices are not compile-time constants. */
    private static int outOfRange = 7;
    private static int negative = -1;

    /** Sink for loaded values so a read cannot be discarded as unused. */
    static int sink;

    private ArrayGuardDemo() {
    }

    static void validate() {
        StringBuilder failures = new StringBuilder();

        // Reads only, deliberately. An out-of-range READ on a VM that omits the
        // check lands inside the process's own heap and returns garbage, so this
        // reports a clean failure. An out-of-range WRITE would instead corrupt
        // whatever is next to the array -- verified: the pre-fix VM takes SIGSEGV
        // partway through -- which is a legible CI failure but a terrible thing to
        // do during app startup, and it destroys the diagnostic. Reads prove the
        // same property without touching memory that is not ours.
        check(failures, "oob-read", tag(READ, HOLDER, outOfRange), "AIOOBE");
        check(failures, "negative-read", tag(READ, HOLDER, negative), "AIOOBE");
        check(failures, "oob-loop-read", tag(LOOP_READ, HOLDER, outOfRange), "AIOOBE");
        report(failures);

        // Only after the bounds result is safely reported. The null here is the
        // ARRAY, not the holder -- a null holder would be a null FIELD access,
        // which these guards do not cover and which would fault before reaching
        // any array code. A null array dereference still faults on a VM with
        // neither an emitted check nor a fault handler, which would lose the
        // diagnostic above if it ran first. On iOS/tvOS/watchOS the port's
        // SIGSEGV handler turns it into NullPointerException regardless, so this
        // case only really bites on targets without one.
        check(failures, "null-read", tag(READ_MISSING, HOLDER, 0), "NPE");
        report(failures);
    }

    private static void report(StringBuilder failures) {
        if (failures.length() > 0) {
            throw new IllegalStateException("Array guard regression:" + failures
                    + " -- array access is not memory safe on this VM");
        }
    }

    private static final int READ = 0;
    private static final int LOOP_READ = 1;
    private static final int READ_MISSING = 2;

    /**
     * @return the tag of the exception the access raised, or {@code "none"} if it
     * completed -- which for these inputs means it read or wrote out of bounds
     */
    private static String tag(int op, Holder h, int index) {
        try {
            if (op == READ) {
                sink = read(h, index);
            } else if (op == READ_MISSING) {
                sink = readMissing(h, index);
            } else {
                sink = new ArrayGuardDemo().loopRead(index);
            }
            return "none";
        } catch (ArrayIndexOutOfBoundsException e) {
            return "AIOOBE";
        } catch (NullPointerException e) {
            return "NPE";
        }
    }

    // Kept free of try/catch: wrapping the access in a handler here lets the
    // translator reduce it, which would move it off the path under test.
    private static int read(Holder h, int index) {
        return h.a[index];
    }

    private static int readMissing(Holder h, int index) {
        return h.missing[index];
    }

    private int loopRead(int index) {
        while (pointer < size) {
            pointer++;
            int v = table[index];
            if (v != 0) {
                return v;
            }
        }
        return 0;
    }

    private static void check(StringBuilder failures, String name, String actual, String expected) {
        if (!expected.equals(actual)) {
            failures.append(' ').append(name).append("(expected ").append(expected)
                    .append(", got ").append(actual).append(')');
        }
    }
}
