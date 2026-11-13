/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.slate.client;

import com.codenameone.examples.slate.ui.Lang;

/// Why a day has nothing free, from the slot engine's reason code. The
/// server sends the code rather than the words so that the words can be
/// the app's, in the user's language.
public final class Reasons {
    private Reasons() {
    }

    public static String say(String code) {
        if ("TOO_SOON".equals(code)) {
            return Lang.tr("Bookings need more notice than that.");
        }
        if ("TOO_FAR".equals(code)) {
            return Lang.tr("Bookings are not open that far ahead yet.");
        }
        if ("OUTSIDE_HOURS".equals(code)) {
            return Lang.tr("Nobody is working that day.");
        }
        if ("BOOKED".equals(code) || "TIME_OFF".equals(code) || "CALENDAR".equals(code)) {
            return Lang.tr("Everyone is booked that day.");
        }
        if ("DAY_FULL".equals(code)) {
            return Lang.tr("That day is fully booked.");
        }
        if ("CLIENT_BUSY".equals(code) || "CLIENT_GAP".equals(code)) {
            return Lang.tr("You already have an appointment close to every free time.");
        }
        if ("CLIENT_DAY_FULL".equals(code)) {
            return Lang.tr("You have booked as many appointments that day as the business allows.");
        }
        if ("NO_RESOURCE".equals(code)) {
            return Lang.tr("Every room is in use that day.");
        }
        if ("NOT_ENOUGH_PROVIDERS".equals(code)) {
            return Lang.tr("Not enough people are free together that day.");
        }
        if ("GROUP_FULL".equals(code)) {
            return Lang.tr("Every group that day is full.");
        }
        return Lang.tr("Try another day.");
    }
}
