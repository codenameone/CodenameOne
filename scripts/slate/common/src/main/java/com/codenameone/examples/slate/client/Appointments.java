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

import com.codenameone.examples.slate.api.AppointmentDto;
import com.codenameone.examples.slate.api.PartyDto;
import com.codenameone.examples.slate.ui.Lang;

/// What the booking screens say about an appointment.
public final class Appointments {
    public static final String HELD = "HELD";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String COMPLETED = "COMPLETED";
    public static final String NO_SHOW = "NO_SHOW";
    public static final String CANCELLED = "CANCELLED";

    private Appointments() {
    }

    /// `with Maya Brooks`, or empty when the appointment names nobody.
    public static String with(AppointmentDto a) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; a.parties() != null && iter < a.parties().size(); iter++) {
            PartyDto p = a.parties().get(iter);
            if (!"PROVIDER".equals(p.kind())) {
                continue;
            }
            out.append(out.length() == 0 ? "" : ", ").append(p.name());
        }
        return out.length() == 0 ? "" : Lang.tr("with {0}", out.toString());
    }

    /// The provider the appointment is with, to keep them when it moves.
    public static String providerRef(AppointmentDto a) {
        for (int iter = 0; a.parties() != null && iter < a.parties().size(); iter++) {
            if ("PROVIDER".equals(a.parties().get(iter).kind())) {
                return a.parties().get(iter).ref();
            }
        }
        return "";
    }

    public static boolean upcoming(AppointmentDto a) {
        return (CONFIRMED.equals(a.state()) || HELD.equals(a.state()))
                && a.end() > System.currentTimeMillis();
    }

    /// The state in words.
    public static String state(AppointmentDto a) {
        if (HELD.equals(a.state())) {
            return Lang.tr("Held for you");
        }
        if (CONFIRMED.equals(a.state())) {
            return Lang.tr("Booked");
        }
        if (COMPLETED.equals(a.state())) {
            return Lang.tr("Done");
        }
        if (NO_SHOW.equals(a.state())) {
            return Lang.tr("Missed");
        }
        return Lang.tr("Cancelled");
    }

    /// The deposit in words.
    public static String payment(AppointmentDto a) {
        String p = a.paymentState();
        if ("PENDING".equals(p)) {
            return Lang.tr("Deposit held a few days before");
        }
        if ("AUTHORIZED".equals(p)) {
            return Lang.tr("Deposit held on your card");
        }
        if ("CAPTURED".equals(p)) {
            return Lang.tr("Deposit charged");
        }
        if ("RELEASED".equals(p)) {
            return Lang.tr("Deposit released");
        }
        if ("FAILED".equals(p)) {
            return Lang.tr("The deposit could not be held");
        }
        return Lang.tr("Pay on the day");
    }
}
