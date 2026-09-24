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
package dart.core;

/**
 * Dart's RangeError — a numeric argument was outside its valid range,
 * including list index errors.
 */
public class RangeError extends ArgumentError {

    public RangeError(String message) {
        super(message);
    }

    /**
     * Guard used by DartList index access; mirrors RangeError.checkValidIndex.
     */
    public static long checkValidIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new RangeError("RangeError (index): Invalid value: Not in inclusive range 0.." + (length - 1) + ": " + index);
        }
        return index;
    }

    /**
     * Cold throw helper for the primitive-list hot paths. The bounds comparison
     * is done inline by the caller (a frameless method); only on failure is this
     * called. Keeping the throw (which allocates a message + RangeError) out of
     * the caller lets the caller stay a lightweight frameless method instead of
     * paying a full method-stack frame on every in-range index access — the
     * dominant cost of tight index loops (e.g. quicksort) on ParparVM.
     */
    /**
     * A list length no array can hold. Narrowed to an int, 2^32 wrapped to 0 and the
     * list came back empty; Dart runs out of memory trying to allocate it, and so does
     * this, rather than handing back a plausible wrong answer.
     */
    public static void checkAllocatable(long length) {
        if (length > Integer.MAX_VALUE - 8) {
            throw new OutOfMemoryError("Cannot allocate a list of length " + length);
        }
    }

    /** Dart's {@code RangeError.checkNotNegative}. */
    public static long checkNotNegative(long value, String name) {
        if (value < 0) {
            throw new RangeError("Invalid value: Not greater than or equal to 0: " + value
                    + (name == null ? "" : " (" + name + ")"));
        }
        return value;
    }

    public static void indexError(long index, long length) {
        throw new RangeError("RangeError (index): Invalid value: Not in inclusive range 0.." + (length - 1) + ": " + index);
    }

    public static long checkValueInInterval(long value, long minValue, long maxValue, String name) {
        if (value < minValue || value > maxValue) {
            throw new RangeError("RangeError (" + name + "): Invalid value: Not in inclusive range "
                    + minValue + ".." + maxValue + ": " + value);
        }
        return value;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
