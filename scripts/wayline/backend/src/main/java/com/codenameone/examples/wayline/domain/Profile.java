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
package com.codenameone.examples.wayline.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// An account's profile: who it is to the people it rides with.
///
/// The sign-in -- password hash, roles, whether it is enabled -- is the backend's
/// own user store, under the same name.
@Entity(table = "wl_profile")
public class Profile {
    @Id(autoIncrement = false)
    @Column(name = "username", nullable = false)
    public String username = "";

    @Column(name = "display_name", nullable = false)
    public String displayName = "";

    @Column(name = "phone", nullable = false)
    public String phone = "";

    @Column(name = "phone_verified", nullable = false)
    public boolean phoneVerified;

    @Column(name = "vehicle", nullable = false)
    public String vehicle = "";

    @Column(name = "plate", nullable = false)
    public String plate = "";

    @Column(name = "created_at", nullable = false)
    public long createdAt;

    @Column(name = "gender", nullable = false)
    public String gender = "unspecified";

    @Column(name = "lang", nullable = false)
    public String language = "en";

    @Column(name = "flagged", nullable = false)
    public boolean flagged;

    @Column(name = "flag_reason", nullable = false)
    public String flagReason = "";

    @Column(name = "emergency_name", nullable = false)
    public String emergencyName = "";

    @Column(name = "emergency_phone", nullable = false)
    public String emergencyPhone = "";

    @Column(name = "home_address", nullable = false)
    public String homeAddress = "";

    @Column(name = "work_address", nullable = false)
    public String workAddress = "";

    public Profile() {
    }
}
