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
package com.codename1.health.sensors;

import java.util.Calendar;
import java.util.TimeZone;

/// Reader for the Bluetooth SIG `Date Time` structure (7 bytes: `uint16`
/// year, then `uint8` month, day, hour, minute, second).
///
/// The structure carries no time zone. Devices set their clock from the
/// phone during pairing and then report **local** wall-clock time, so the
/// fields are interpreted in the device's default zone -- the same one the
/// user saw when they took the measurement.
final class GattDateTime {

    /// The year field is 0 when the device has no idea what the date is,
    /// which happens after a battery change.
    private static final int YEAR_UNKNOWN = 0;

    private GattDateTime() {
    }

    /// Reads seven bytes and returns epoch millis, or `-1` when the device
    /// reported an unknown or nonsensical date.
    static long read(GattReader r) {
        int year = r.uint16();
        int month = r.uint8();
        int day = r.uint8();
        int hour = r.uint8();
        int minute = r.uint8();
        int second = r.uint8();
        if (!r.isValid() || year == YEAR_UNKNOWN || month < 1 || month > 12
                || day < 1 || day > 31 || hour > 23 || minute > 59
                || second > 59) {
            return -1;
        }
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.clear();
        c.set(year, month - 1, day, hour, minute, second);
        return c.getTimeInMillis();
    }
}
