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
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.Mapped;

/// Who is signed in, as the app needs to know it: enough to greet them, to
/// decide which modes to offer, and to tell whether they still owe a phone
/// verification.
@Mapped
public class UserDto {
    /// The sign-in name, which is the e-mail address the account was opened with.
    public String username;
    public String displayName;
    public String phone;
    public boolean phoneVerified;
    /// Every account can ride; the other two modes are granted.
    public boolean rider;
    public boolean driver;
    public boolean admin;
    /// Blocked by an admin: the account cannot sign in or call the API.
    public boolean suspended;
    /// A driver's car, as riders see it. Empty for anyone else.
    public String vehicle;
    public String plate;
    /// `unspecified`, `female`, `male` or `nonbinary`.
    public String gender;
    /// Where the account stands on driving: `none`, `draft`, `pending`, `approved`
    /// or `rejected`. The DRIVER role follows `approved`.
    public String driverStatus;
    /// Marked by an admin for a closer look. The account still works; `suspended`
    /// is what stops one.
    public boolean flagged;
    public String flagReason;
    /// The language the app is shown in: `en`, `es`, `fr`, `de` or `he`.
    public String language;

    public UserDto() {
    }
}
