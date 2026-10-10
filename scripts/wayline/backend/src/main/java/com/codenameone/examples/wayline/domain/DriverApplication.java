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

/// An account's application to drive, and what became of it.
@Entity(table = "wl_driver_application")
public class DriverApplication {
    @Id(autoIncrement = false)
    @Column(name = "username", nullable = false)
    public String username = "";

    @Column(name = "status", nullable = false)
    public String status = "";

    @Column(name = "legal_name", nullable = false)
    public String legalName = "";

    @Column(name = "date_of_birth", nullable = false)
    public String dateOfBirth = "";

    @Column(name = "licence_number", nullable = false)
    public String licenceNumber = "";

    @Column(name = "licence_expiry", nullable = false)
    public String licenceExpiry = "";

    @Column(name = "vehicle_make", nullable = false)
    public String vehicleMake = "";

    @Column(name = "vehicle_model", nullable = false)
    public String vehicleModel = "";

    @Column(name = "vehicle_year", nullable = false)
    public int vehicleYear;

    @Column(name = "vehicle_color", nullable = false)
    public String vehicleColor = "";

    @Column(name = "vehicle_plate", nullable = false)
    public String vehiclePlate = "";

    @Column(name = "vehicle_seats", nullable = false)
    public int vehicleSeats;

    @Column(name = "product", nullable = false)
    public String product = "";

    @Column(name = "wheelchair", nullable = false)
    public boolean wheelchair;

    @Column(name = "pets", nullable = false)
    public boolean pets;

    @Column(name = "rejection_reason", nullable = false)
    public String rejectionReason = "";

    @Column(name = "created_at", nullable = false)
    public long createdAt;

    @Column(name = "submitted_at", nullable = false)
    public long submittedAt;

    @Column(name = "reviewed_at", nullable = false)
    public long reviewedAt;

    @Column(name = "reviewed_by", nullable = false)
    public String reviewedBy = "";

    public DriverApplication() {
    }
}
