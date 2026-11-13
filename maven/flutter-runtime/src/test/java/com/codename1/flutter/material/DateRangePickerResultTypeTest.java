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
package com.codename1.flutter.material;

import com.codename1.flutter.navigation.Navigator;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.io.Util;

import dart.async.Future;
import dart.core.DateTime;
import dart.core.DateTimeRange;
import dart.runtime.Funcs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// `showDateRangePicker` answers Flutter's `Future<DateTimeRange?>`, so a transpiled
/// `DateTimeRange r = await showDateRangePicker(...)` compiles and receives the range.
class DateRangePickerResultTypeTest {

    // Opening the dialog mounts its calendar, whose components need fonts and styles:
    // RasterDisplay.install() puts a Display implementation in place, and Util gets the
    // same one (install() bypasses Display.initImpl, which would wire it). Both are
    // removed afterwards so the module's headless tests see no implementation.
    @BeforeEach
    void reset() {
        Util.setImplementation(RasterDisplay.install());
        Navigator.reset();
        Dialogs.reset();
    }

    @AfterEach
    void uninstall() {
        Navigator.reset();
        Dialogs.reset();
        Util.setImplementation(null);
        RasterDisplay.uninstall();
    }

    private static DateTime day(long y, long m, long d) {
        return new DateTime(y, m, d, 0L, 0L, 0L, 0L, 0L);
    }

    private static Future<DateTimeRange> open(DateTimeRange initial) {
        return DateRangePickerDialog.show(null, day(2026, 1, 1), day(2027, 1, 1), initial, null,
                "Pick", null, "Book");
    }

    @Test
    void dateRangePickerFutureCompletesWithTheChosenRangeTyped() {
        final DateTimeRange[] got = {null};
        final boolean[] completed = {false};
        DateTimeRange initial = new DateTimeRange(day(2026, 3, 1), day(2026, 3, 4));
        open(initial).then(new Funcs.VoidFunc1<DateTimeRange>() {
            @Override
            public void call(DateTimeRange v) {
                got[0] = v;
                completed[0] = true;
            }
        });
        assertEquals(false, completed[0], "still open");
        DateTimeRange chosen = new DateTimeRange(day(2026, 5, 2), day(2026, 5, 9));
        Navigator.pop(null, chosen);
        assertEquals(true, completed[0]);
        assertSame(chosen, got[0], "the typed future carries the popped range");
    }

    @Test
    void cancelledDateRangePickerCompletesWithNull() {
        final Object[] got = {"pending"};
        open(null).then(new Funcs.VoidFunc1<DateTimeRange>() {
            @Override
            public void call(DateTimeRange v) {
                got[0] = v;
            }
        });
        Navigator.pop(null, null);
        assertNull(got[0], "a dismissed picker answers null, as Flutter's DateTimeRange? does");
    }
}
