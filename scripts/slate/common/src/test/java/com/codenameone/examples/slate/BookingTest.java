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
package com.codenameone.examples.slate;

import com.codenameone.examples.slate.client.Times;

import java.util.Map;

/// A client books from the screens: a service, a day, a free start, the
/// hold, the confirmation. The server is then asked whether the appointment
/// is there, and the same start is no longer offered to anyone.
public class BookingTest extends E2e {
    @Override
    @SuppressWarnings("unchecked")
    protected boolean run() throws Exception {
        Actor client = Actor.signIn(CLIENT);
        String serviceId = "";
        for (Object s : client.list("/api/services")) {
            Object capacity = s instanceof Map ? ((Map<String, Object>) s).get("capacity") : null;
            if (serviceId.length() == 0 && capacity instanceof Number
                    && ((Number) capacity).intValue() == 1) {
                serviceId = Actor.text((Map<String, Object>) s, "id");
            }
        }
        assertTrue(serviceId.length() > 0, "the demo business offers a one-to-one service");

        signIn(CLIENT, "Client");
        click("service-" + serviceId);
        waitForForm("Book");
        // Two days out: far enough that no notice rule refuses it.
        String day = Times.iso(Times.addDays(System.currentTimeMillis(), 2));
        click("day-" + day);
        until(() -> firstNamed("slot-").length() > 0, 20000, "a free start");
        shot("book-slots");
        click(firstNamed("slot-"));
        waitForForm("Confirm");
        until(() -> has("confirm"), 20000, "the confirm button");
        shot("book-confirm");
        click("confirm");
        waitForForm("Booked");
        shot("book-booked");

        String booked = "";
        for (Object a : client.list("/api/appointments")) {
            if (a instanceof Map && "CONFIRMED".equals(Actor.text((Map<String, Object>) a, "state"))) {
                booked = Actor.text((Map<String, Object>) a, "id");
            }
        }
        assertTrue(booked.length() > 0, "the appointment is confirmed on the server");

        click("details");
        waitForForm("Appointment");
        shot("appointment");
        return true;
    }
}
