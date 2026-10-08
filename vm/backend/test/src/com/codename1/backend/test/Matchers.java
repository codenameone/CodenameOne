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
package com.codename1.backend.test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// What the result matchers share: comparisons that report the way JUnit does.
final class Matchers {
    private Matchers() {
    }

    static void equal(String what, Object expected, Object actual) {
        if (!same(expected, actual)) {
            throw new AssertionError(what + " expected:<" + expected + "> but was:<" + actual
                    + ">");
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /// Equality that treats JSON numbers by value -- an expected Integer 5 matches
    /// the Long 5 the parser produces, and 2.5f matches 2.5 -- and compares lists
    /// and maps element by element under the same rule.
    static boolean same(Object expected, Object actual) {
        if (expected == actual) { //NOPMD CompareObjectsWithEquals - the same object, a shortcut
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        if (expected instanceof Number && actual instanceof Number) {
            Number e = (Number) expected;
            Number a = (Number) actual;
            if (isIntegral(e) && isIntegral(a)) {
                return e.longValue() == a.longValue();
            }
            return e.doubleValue() == a.doubleValue();
        }
        if (expected instanceof Character && actual instanceof String) {
            return String.valueOf(expected).equals(actual);
        }
        if (expected instanceof List && actual instanceof List) {
            List e = (List) expected;
            List a = (List) actual;
            if (e.size() != a.size()) {
                return false;
            }
            for (int iter = 0 ; iter < e.size() ; iter++) {
                if (!same(e.get(iter), a.get(iter))) {
                    return false;
                }
            }
            return true;
        }
        if (expected instanceof Object[] && actual instanceof List) {
            return same(java.util.Arrays.asList((Object[]) expected), actual);
        }
        if (expected instanceof Map && actual instanceof Map) {
            Map e = (Map) expected;
            Map a = (Map) actual;
            if (e.size() != a.size()) {
                return false;
            }
            Iterator it = e.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                if (!a.containsKey(entry.getKey()) || !same(entry.getValue(), a.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return expected.equals(actual);
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte;
    }

    /// `actual` holds everything `expected` does: objects may carry extra fields,
    /// arrays must match in length and order. Strict asks for exact equality.
    static boolean jsonMatches(Object expected, Object actual, boolean strict) {
        if (strict) {
            return same(expected, actual);
        }
        if (expected instanceof Map && actual instanceof Map) {
            Map e = (Map) expected;
            Map a = (Map) actual;
            Iterator it = e.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                if (!a.containsKey(entry.getKey())
                        || !jsonMatches(entry.getValue(), a.get(entry.getKey()), false)) {
                    return false;
                }
            }
            return true;
        }
        if (expected instanceof List && actual instanceof List) {
            List e = (List) expected;
            List a = (List) actual;
            if (e.size() != a.size()) {
                return false;
            }
            for (int iter = 0 ; iter < e.size() ; iter++) {
                if (!jsonMatches(e.get(iter), a.get(iter), false)) {
                    return false;
                }
            }
            return true;
        }
        return same(expected, actual);
    }
}
