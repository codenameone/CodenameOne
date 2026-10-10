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

/// What opening an account takes. The password travels once, in the body of a
/// request over TLS, and the server keeps only its hash.
@Mapped
public class RegisterDto {
    public String email;
    public String password;
    public String displayName;
    public String phone;
    /// The older spelling of [#wantsToDrive], kept for apps built before it. It no
    /// longer grants anything: it opens a driving application like the newer flag.
    public boolean driver;
    public String vehicle;
    public String plate;
    /// Opens a driving application with the account. The account is a rider until
    /// an admin approves the application.
    public boolean wantsToDrive;
    /// `unspecified`, `female`, `male` or `nonbinary`; empty reads as unspecified.
    public String gender;

    public RegisterDto() {
    }
}
