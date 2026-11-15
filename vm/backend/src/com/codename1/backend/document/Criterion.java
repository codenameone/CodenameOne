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
package com.codename1.backend.document;

/// One predicate of a document query: a field, an operator and its operand.
public final class Criterion {
    /// Equal; a null operand matches a null or missing field.
    public static final int EQ = 0;
    /// Not equal; a null operand matches any present, non-null field.
    public static final int NE = 1;
    /// Greater than.
    public static final int GT = 2;
    /// Greater than or equal.
    public static final int GE = 3;
    /// Less than.
    public static final int LT = 4;
    /// Less than or equal.
    public static final int LE = 5;
    /// SQL `LIKE`: `%` is any run of characters and `_` any one character.
    public static final int LIKE = 6;
    /// One of an array of operands; an empty array matches nothing.
    public static final int IN = 7;

    private final String field;
    private final int op;
    private final Object value;

    /// A predicate; `value` is an `Object[]` for [#IN].
    public Criterion(String field, int op, Object value) {
        this.field = field;
        this.op = op;
        this.value = value;
    }

    /// The document field.
    public String getField() {
        return field;
    }

    /// One of the operator constants.
    public int getOp() {
        return op;
    }

    /// The operand.
    public Object getValue() {
        return value;
    }

    /// Whether `text` matches a SQL `LIKE` pattern, case sensitive, without
    /// an escape character.
    public static boolean like(String text, String pattern) {
        return like(text, 0, pattern, 0);
    }

    private static boolean like(String t, int ti, String p, int pi) {
        while (pi < p.length()) {
            char c = p.charAt(pi);
            if (c == '%') {
                while (pi < p.length() && p.charAt(pi) == '%') {
                    pi++;
                }
                if (pi == p.length()) {
                    return true;
                }
                for (int i = ti; i <= t.length(); i++) {
                    if (like(t, i, p, pi)) {
                        return true;
                    }
                }
                return false;
            }
            if (ti >= t.length() || (c != '_' && c != t.charAt(ti))) {
                return false;
            }
            ti++;
            pi++;
        }
        return ti == t.length();
    }

    /// Whether `doc` satisfies this predicate, compared the way the stores do:
    /// numbers numerically, everything else by `equals` or natural order.
    public boolean matches(java.util.Map doc) {
        Object actual = doc.get(field);
        switch (op) {
            case EQ:
                return same(actual, value);
            case NE:
                return value == null ? actual != null : actual != null && !same(actual, value);
            case LIKE:
                return actual instanceof String && value != null
                        && like((String) actual, String.valueOf(value));
            case IN: {
                Object[] values = (Object[]) value;
                for (Object eachValue : values) {
                    if (actual != null && same(actual, eachValue)) {
                        return true;
                    }
                }
                return false;
            }
            default: {
                if (actual == null || value == null || !comparable(actual, value)) {
                    return false;
                }
                int c = compare(actual, value);
                return op == GT ? c > 0 : op == GE ? c >= 0 : op == LT ? c < 0 : c <= 0;
            }
        }
    }

    static boolean same(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            if (integral(a) && integral(b)) {
                return ((Number) a).longValue() == ((Number) b).longValue();
            }
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a == null ? b == null : a.equals(b);
    }

    private static boolean integral(Object n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte;
    }

    /// Orders two field values; nulls first.
    static int compare(Object a, Object b) {
        if (a == null || b == null) {
            return a == null ? (b == null ? 0 : -1) : 1;
        }
        if (a instanceof Number && b instanceof Number && integral(a) && integral(b)) {
            // Exact: a double holds no more than 53 bits, and ids and versions
            // past 2^53 would otherwise compare equal to their neighbours.
            long x = ((Number) a).longValue();
            long y = ((Number) b).longValue();
            return x < y ? -1 : (x > y ? 1 : 0);
        }
        if (a instanceof Number && b instanceof Number) {
            double x = ((Number) a).doubleValue();
            double y = ((Number) b).doubleValue();
            return x < y ? -1 : (x > y ? 1 : 0);
        }
        if (a instanceof java.util.Date && b instanceof java.util.Date) {
            long x = ((java.util.Date) a).getTime();
            long y = ((java.util.Date) b).getTime();
            return x < y ? -1 : (x > y ? 1 : 0);
        }
        if (a instanceof Boolean && b instanceof Boolean) {
            boolean x = ((Boolean) a).booleanValue();
            return x == ((Boolean) b).booleanValue() ? 0 : (x ? 1 : -1);
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        // Values of different types: a stable order for sorting, by type.
        // A range test never gets here (see comparable).
        int x = rank(a);
        int y = rank(b);
        if (x != y) {
            return x < y ? -1 : 1;
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    /// Whether a range test between the two can hold. Like the stores, a
    /// number is never greater or less than a string.
    static boolean comparable(Object a, Object b) {
        return rank(a) == rank(b);
    }

    private static int rank(Object v) {
        if (v instanceof Number) {
            return 0;
        }
        if (v instanceof String) {
            return 1;
        }
        if (v instanceof java.util.Date) {
            return 2;
        }
        return v instanceof Boolean ? 3 : 4;
    }
}
