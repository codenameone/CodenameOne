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
package com.codenameone.examples.slate.booking;

/// The words the booking tables store. They live here rather than on the
/// entities so that nothing on an entity is anything but a column.
public final class States {
    // Appointment.state
    public static final String HELD = "HELD";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String COMPLETED = "COMPLETED";
    public static final String NO_SHOW = "NO_SHOW";
    public static final String CANCELLED = "CANCELLED";

    // Appointment.paymentState
    public static final String PAY_NONE = "NONE";
    public static final String PAY_PENDING = "PENDING";
    public static final String PAY_AUTHORIZED = "AUTHORIZED";
    public static final String PAY_CAPTURED = "CAPTURED";
    public static final String PAY_RELEASED = "RELEASED";
    public static final String PAY_FAILED = "FAILED";

    // AppointmentParty.kind
    public static final String PROVIDER = "PROVIDER";
    public static final String RESOURCE = "RESOURCE";
    public static final String GUEST = "GUEST";

    // Waitlist.state
    public static final String WAITING = "WAITING";
    public static final String OFFERED = "OFFERED";
    public static final String BOOKED = "BOOKED";
    public static final String EXPIRED = "EXPIRED";

    private States() {
    }
}
