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
package com.codenameone.examples.pulse;

/// The roles an account can hold. Every account is a member; teaching, working
/// the front desk and running the studio are granted on top of that, and the
/// app offers a mode for each role the account holds.
public final class Roles {
    public static final String MEMBER = "MEMBER";
    /// Teaches classes: sees their rosters, marks attendance, swaps shifts.
    public static final String TRAINER = "TRAINER";
    /// Works the front desk and the door kiosk: checks people in, takes walk-ins.
    public static final String DESK = "DESK";
    /// Runs the studio: plans, timetable, staff, campaigns, accounts, numbers.
    public static final String OWNER = "OWNER";

    private Roles() {
    }
}
