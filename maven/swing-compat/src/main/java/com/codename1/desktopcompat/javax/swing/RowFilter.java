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
package com.codename1.desktopcompat.javax.swing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/// Decides which rows of a model a sorter shows.
///
/// The filters by number, by date and the combinations are here; the
/// JDK's regular expression filter is not, because a device has no
/// regular expressions.
public abstract class RowFilter<M, I> {

    /// How a value is compared with the filter's.
    public enum ComparisonType {
        BEFORE,
        AFTER,
        EQUAL,
        NOT_EQUAL
    }

    public RowFilter() {
    }

    private static void check(int[] columns) {
        for (int i = columns.length - 1; i >= 0; i--) {
            if (columns[i] < 0) {
                throw new IllegalArgumentException("Index must be >= 0");
            }
        }
    }

    /// Shows the rows with a date value that compares to `date` as asked,
    /// in one of the given columns or, without any, in any column.
    public static <M, I> RowFilter<M, I> dateFilter(ComparisonType type, Date date, int... indices) {
        if (date == null || type == null) {
            throw new IllegalArgumentException("type and date must be non-null");
        }
        check(indices);
        return new ValueFilter<M, I>(type, null, date.getTime(), indices);
    }

    /// Shows the rows with a number that compares to `number` as asked,
    /// in one of the given columns or, without any, in any column.
    public static <M, I> RowFilter<M, I> numberFilter(ComparisonType type, Number number, int... indices) {
        if (number == null || type == null) {
            throw new IllegalArgumentException("type and number must be non-null");
        }
        check(indices);
        return new ValueFilter<M, I>(type, number, 0L, indices);
    }

    public static <M, I> RowFilter<M, I> orFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new Combined<M, I>(filters, false);
    }

    public static <M, I> RowFilter<M, I> andFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new Combined<M, I>(filters, true);
    }

    public static <M, I> RowFilter<M, I> notFilter(RowFilter<M, I> filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter must be non-null");
        }
        return new Not<M, I>(filter);
    }

    public abstract boolean include(Entry<? extends M, ? extends I> entry);

    /// One row as a filter sees it.
    public abstract static class Entry<M, I> {

        public Entry() {
        }

        public abstract M getModel();

        public abstract int getValueCount();

        public abstract Object getValue(int index);

        public String getStringValue(int index) {
            Object value = getValue(index);
            return value == null ? "" : value.toString();
        }

        public abstract I getIdentifier();
    }

    /// Compares numbers by magnitude, or dates by their time.
    private static final class ValueFilter<M, I> extends RowFilter<M, I> {

        private final ComparisonType type;
        private final Number number;
        private final long time;
        private final int[] columns;

        ValueFilter(ComparisonType type, Number number, long time, int[] columns) {
            this.type = type;
            this.number = number;
            this.time = time;
            this.columns = new int[columns.length];
            System.arraycopy(columns, 0, this.columns, 0, columns.length);
        }

        private boolean matches(Object v) {
            int c;
            if (number != null) {
                if (!(v instanceof Number)) {
                    return false;
                }
                Number n = (Number) v;
                if (isWhole(n) && isWhole(number)) {
                    long a = n.longValue();
                    long b = number.longValue();
                    c = a < b ? -1 : a > b ? 1 : 0;
                } else {
                    c = Double.compare(n.doubleValue(), number.doubleValue());
                }
            } else {
                if (!(v instanceof Date)) {
                    return false;
                }
                long t = ((Date) v).getTime();
                c = t < time ? -1 : t > time ? 1 : 0;
            }
            switch (type) {
                case BEFORE:
                    return c < 0;
                case AFTER:
                    return c > 0;
                case EQUAL:
                    return c == 0;
                default:
                    return c != 0;
            }
        }

        private static boolean isWhole(Number n) {
            return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
        }

        @Override
        public boolean include(Entry<? extends M, ? extends I> entry) {
            int count = entry.getValueCount();
            if (columns.length == 0) {
                for (int i = 0; i < count; i++) {
                    if (matches(entry.getValue(i))) {
                        return true;
                    }
                }
                return false;
            }
            for (int i = 0; i < columns.length; i++) {
                if (columns[i] < count && matches(entry.getValue(columns[i]))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Combined<M, I> extends RowFilter<M, I> {

        private final List<RowFilter<? super M, ? super I>> filters =
                new ArrayList<RowFilter<? super M, ? super I>>();
        private final boolean all;

        Combined(Iterable<? extends RowFilter<? super M, ? super I>> source, boolean all) {
            this.all = all;
            for (RowFilter<? super M, ? super I> f : source) {
                if (f == null) {
                    throw new IllegalArgumentException("Filter must be non-null");
                }
                filters.add(f);
            }
        }

        @Override
        public boolean include(Entry<? extends M, ? extends I> entry) {
            for (int i = 0; i < filters.size(); i++) {
                if (filters.get(i).include(entry) != all) {
                    return !all;
                }
            }
            return all;
        }
    }

    private static final class Not<M, I> extends RowFilter<M, I> {

        private final RowFilter<M, I> filter;

        Not(RowFilter<M, I> filter) {
            this.filter = filter;
        }

        @Override
        public boolean include(Entry<? extends M, ? extends I> entry) {
            return !filter.include(entry);
        }
    }
}
