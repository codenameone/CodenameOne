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
    private final Long start;
    private final Long end;
    /// Null for an error built from a finished text (the runtime's own guards).
    private final String kind;

    public RangeError(String message) {
        super(message);
        this.start = null;
        this.end = null;
        this.kind = null;
    }

    private RangeError(String message, String kind) {
        super(null, false, null, message);
        this.start = null;
        this.end = null;
        this.kind = kind;
    }

    /// Dart's `RangeError(message)`: no value, name or bounds, so it prints as
    /// `RangeError: message`. The String constructor above is the runtime's own
    /// guards, whose message is already the whole text.
    public static RangeError ofMessage(String message) {
        return new RangeError(message, "message");
    }

    private RangeError(Object value, String name, String message, Long start, Long end, String kind) {
        super(value, true, name, message);
        this.start = start;
        this.end = end;
        this.kind = kind;
    }

    /// A program subclass's `super(message)`, `super.range(..)` or `super.value(..)`:
    /// Java cannot run the static factories as a super constructor, so `ctor` names the
    /// Dart constructor ("message", "range" or "value") and the defaults are applied here.
    protected RangeError(String ctor, Long value, Long min, Long max, String name, String message) {
        super(value, !"message".equals(ctor), name, "message".equals(ctor) || message != null ? message
                : "range".equals(ctor) ? "Invalid value" : "Value not in range");
        this.start = min;
        this.end = max;
        this.kind = ctor;
    }

    public Long start() {
        return start;
    }

    public Long end() {
        return end;
    }

    @Override
    protected String errorName() {
        return "RangeError";
    }

    /// Dart's RangeError._errorExplanation, and IndexError's.
    @Override
    protected String errorExplanation() {
        if ("index".equals(kind)) {
            long len = end.longValue() + 1;
            Object v = invalidValue();
            if (v instanceof Number && ((Number) v).longValue() < 0) {
                return ": index must not be negative";
            }
            return len == 0 ? ": no indices are valid" : ": index should be less than " + len;
        }
        if (start == null) {
            return end == null ? "" : ": Not less than or equal to " + end;
        }
        if (end == null) {
            return ": Not greater than or equal to " + start;
        }
        if (end.longValue() > start.longValue()) {
            return ": Not in inclusive range " + start + ".." + end;
        }
        if (end.longValue() < start.longValue()) {
            return ": Valid value range is empty";
        }
        return ": Only valid value is " + start;
    }

    @Override
    public String toString() {
        return kind == null ? getMessage() : super.toString();
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
            throw range(value, Long.valueOf(0), null, name == null ? "index" : name, null);
        }
        return value;
    }

    public static void indexError(long index, long length) {
        throw new RangeError("RangeError (index): Invalid value: Not in inclusive range 0.." + (length - 1) + ": " + index);
    }

    public static long checkValueInInterval(long value, long minValue, long maxValue, String name) {
        if (value < minValue || value > maxValue) {
            throw range(value, Long.valueOf(minValue), Long.valueOf(maxValue), name, null);
        }
        return value;
    }

    /// Dart's `RangeError.checkValidRange(start, end, length)`: `0 <= start <= length`
    /// and, when given, `start <= end <= length`. Answers the effective end.
    public static long checkValidRange(long start, Long end, long length) {
        if (start < 0 || start > length) {
            throw new RangeError("RangeError (start): Invalid value: Not in inclusive range 0.."
                    + length + ": " + start);
        }
        if (end == null) {
            return length;
        }
        long e = end.longValue();
        if (e < start || e > length) {
            throw new RangeError("RangeError (end): Invalid value: Not in inclusive range "
                    + start + ".." + length + ": " + e);
        }
        return e;
    }

    public static long checkValidRange(long start, long end, long length) {
        return checkValidRange(start, Long.valueOf(end), length);
    }

    public static long checkNotNegative(long value) {
        return checkNotNegative(value, null);
    }

    public static long checkValidIndex(long index, Object indexable, String name, Long length) {
        long len = length != null ? length.longValue()
                : indexable instanceof java.util.Collection ? ((java.util.Collection<?>) indexable).size()
                : indexable instanceof String ? ((String) indexable).length() : 0;
        return checkValidIndex(index, len);
    }

    /// Dart's `RangeError.range(value, min, max, [name, message])`.
    public static RangeError range(long value, Long min, Long max, String name, String message) {
        return new RangeError(Long.valueOf(value), name, message == null ? "Invalid value" : message, min, max,
                "range");
    }

    public static RangeError range(long value, Long min, Long max) {
        return range(value, min, max, null, null);
    }

    /// Dart's `RangeError.value(value, [name, message])`.
    public static RangeError value(long value, String name, String message) {
        return new RangeError(Long.valueOf(value), name, message == null ? "Value not in range" : message, null,
                null, "value");
    }

    public static RangeError index(long index, Object indexable, String name, String message, Long length) {
        return new IndexError(index, indexable, name == null ? "index" : name, message, length);
    }

}
