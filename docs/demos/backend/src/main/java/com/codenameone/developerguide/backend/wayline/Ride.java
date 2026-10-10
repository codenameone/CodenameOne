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
package com.codenameone.developerguide.backend.wayline;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

// tag::wayline-entity[]
/// A ride, from the request to its end.
@Entity(table = "wl_ride")
public class Ride {
    @Id(autoIncrement = false)
    @Column(name = "id", nullable = false)
    public String id = "";

    @Column(name = "state", nullable = false)
    public String state = "";

    @Column(name = "rider", nullable = false)
    public String rider = "";

    @Column(name = "driver", nullable = false)
    public String driver = "";

    @Column(name = "fare_cents", nullable = false)
    public long fareCents;

    @Column(name = "pets", nullable = false)
    public boolean pets;
    // end::wayline-entity[]

    @Column(name = "offer_expires_at", nullable = false)
    public long offerExpiresAt;

    @Column(name = "updated_at", nullable = false)
    public long updatedAt;

    // tag::wayline-column-entity[]
    /// Whether the rider asked for a child seat.
    @Column(name = "child_seat", nullable = false)
    public boolean childSeat;
    // end::wayline-column-entity[]
}
