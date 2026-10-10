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
package com.codename1.desktopcompat.rt;

import java.util.Comparator;

/// The two comparators the row sorters fall back on.
///
/// A failed cast does not throw on a device, so a comparator that is
/// handed two cell values may never cast one to the type of the other,
/// and may never rely on `compareTo` rejecting a stranger. These check
/// first.
public final class Comparators {

    /// Compares strings, and anything else by its string, with
    /// `String.compareTo`: there is no locale collation on a device.
    public static final Comparator<Object> STRINGS = new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            return a.toString().compareTo(b.toString());
        }
    };

    /// Compares numbers as numbers, two values of one comparable class by
    /// `compareTo`, and anything else by its string.
    public static final Comparator<Object> VALUES = new Comparator<Object>() {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public int compare(Object a, Object b) {
            if (a instanceof Number && b instanceof Number) {
                if (whole(a) && whole(b)) {
                    long x = ((Number) a).longValue();
                    long y = ((Number) b).longValue();
                    return x < y ? -1 : x > y ? 1 : 0;
                }
                return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            }
            if (a instanceof Comparable && a.getClass() == b.getClass()) {
                return ((Comparable) a).compareTo(b);
            }
            return a.toString().compareTo(b.toString());
        }
    };

    private Comparators() {
    }

    static boolean whole(Object n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
    }
}
