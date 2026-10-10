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

import java.util.List;

/// An application to drive: who, which car, and the papers that prove both.
@Mapped
public class DriverApplicationDto {
    public String username;
    public String name;
    /// `none`, `draft`, `pending`, `approved` or `rejected`.
    public String status;
    public String legalName;
    /// Dates are `yyyy-MM-dd`.
    public String dateOfBirth;
    public String licenceNumber;
    public String licenceExpiry;
    public String vehicleMake;
    public String vehicleModel;
    public int vehicleYear;
    public String vehicleColor;
    public String vehiclePlate;
    public int vehicleSeats;
    /// `standard`, `comfort` or `xl`.
    public String product;
    /// Whether the car takes a wheelchair.
    public boolean accessible;
    /// Whether a rider's pet is welcome in the car.
    public boolean petFriendly;
    /// One entry for each kind of document asked for, uploaded or not.
    public List<DocumentDto> documents;
    public String rejectionReason;
    /// Milliseconds since the epoch; 0 for a step not reached.
    public long submittedAt;
    public long reviewedAt;
    public String reviewedBy;

    public DriverApplicationDto() {
    }
}
