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
package com.codename1.flutter;

import com.codename1.flutter.foundation.ValueNotifier;
import com.codename1.flutter.intl.NumberFormat;

import dart.runtime.Funcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// ValueNotifier uses Dart equality, stop(canceled: false) completes the run, and a
/// currency format honours an explicit zero digits.
class Round4RuntimeTest {

    @Test
    void aNotifierComparesWithDartEquality() {
        ValueNotifier<Object> n = new ValueNotifier<Object>(Long.valueOf(1));
        final int[] notified = {0};
        n.addListener(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                notified[0]++;
            }
        });
        n.value(Double.valueOf(1.0));
        assertEquals(0, notified[0], "1 == 1.0 in Dart: no change");
        n.value(Double.valueOf(Double.NaN));
        n.value(Double.valueOf(Double.NaN));
        assertEquals(2, notified[0], "NaN is never equal to itself, so both writes notify");
    }

    @Test
    void anExplicitZeroDigitsIsHonoured() {
        assertEquals("$12", NumberFormat.currency(null, "$", Long.valueOf(0), null).format(Long.valueOf(12)));
        assertEquals("$12.00", NumberFormat.currency(null, "$", null, null).format(Long.valueOf(12)),
                "omitted, it defaults to two");
    }

    @Test
    void aUtcValueFormatsInUtc() {
        java.util.TimeZone saved = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"));
            String hm = com.codename1.flutter.intl.DateFormat.Hm(null)
                    .format(dart.core.DateTime.utc(2024, 1, 15, 12, 0, 0, 0, 0));
            assertEquals("12:00", hm, "not shifted by the device's offset");
        } finally {
            java.util.TimeZone.setDefault(saved);
        }
    }
}
