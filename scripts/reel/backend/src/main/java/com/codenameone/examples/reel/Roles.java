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
package com.codenameone.examples.reel;

/// The roles, as the backend's user store names them (without `ROLE_`). The
/// shared [com.codenameone.examples.reel.api.Roles] holds the same names for the app.
public final class Roles {
    public static final String VIEWER = com.codenameone.examples.reel.api.Roles.VIEWER;
    public static final String CREATOR = com.codenameone.examples.reel.api.Roles.CREATOR;
    public static final String MODERATOR = com.codenameone.examples.reel.api.Roles.MODERATOR;
    public static final String ADMIN = com.codenameone.examples.reel.api.Roles.ADMIN;

    /// Whether the caller moderates or administers: staff can play any title,
    /// published or not, without a plan, to review it.
    public static boolean isStaff() {
        return Caller.hasRole(ADMIN) || Caller.hasRole(MODERATOR);
    }

    private Roles() {
    }
}
