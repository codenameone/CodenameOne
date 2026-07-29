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

    /// Days in a month, so an impossible date is rejected rather than
    /// rolled forward.
    ///
    /// The CLDC Calendar this framework targets is always lenient and has
    /// no setLenient, so 31 February would otherwise be normalised into
    /// March and stored as a plausible but wrong measurement date.
    private static int daysInMonth(int year, int month) {
        switch (month) {
            case 2:
                boolean leap = (year % 4 == 0 && year % 100 != 0)
                        || year % 400 == 0;
                return leap ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    /// The first and last years the Date Time characteristic defines.
    /// Everything else is reserved, including the 0 a device reports
    /// when it has no idea what the date is -- after a battery change,
    /// say -- so that case falls out of the same range test.
    private static final int YEAR_MIN = 1582;
    private static final int YEAR_MAX = 9999;

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
        // The profile defines the year as 1582-9999 and reserves
        // everything else, 0 included. A malformed 10000 used to become a
        // perfectly ordinary future timestamp: the session published the
        // reading under a date the device never claimed, and getLatest()
        // treated it as permanently fresh because its age came out
        // negative.
        if (!r.isValid() || year < YEAR_MIN || year > YEAR_MAX
                || month < 1 || month > 12
                || day < 1 || day > daysInMonth(year, month)
                || hour > 23 || minute > 59 || second > 59) {
            return -1;
        }
        // Field-at-a-time rather than clear() plus the six-argument set():
        // the CLDC Calendar this framework targets has neither, and reads
        // its instant through getTime().
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, 0);
        long stamped = c.getTime().getTime();
        if (stamped - System.currentTimeMillis() > MAX_SKEW_MILLIS) {
            // A structurally valid date the reading cannot have been
            // taken at. A cuff or scale with its clock left at 2099 --
            // out of the box, or after a battery change -- passes every
            // field check above, and the session then prefers that stamp
            // over the moment the notification arrived, stores the
            // reading under it, and getLatest() reports it as the
            // freshest there is because its age comes out negative.
            //
            // Refused rather than clamped, so the caller falls back to
            // receipt time instead of being handed a fabricated one. Same
            // bound and same reasoning as the glucose time offset.
            return -1;
        }
        return stamped;
    }

    /// How far ahead of now a device may stamp a reading before the
    /// timestamp is refused: a day, which is slack for a clock that
    /// disagrees with the phone's rather than a licence to record in the
    /// future.
    private static final long MAX_SKEW_MILLIS = 24L * 60 * 60 * 1000;
}
