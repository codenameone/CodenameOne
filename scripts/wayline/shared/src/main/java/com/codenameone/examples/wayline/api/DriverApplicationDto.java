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
///
/// @param status `none`, `draft`, `pending`, `approved` or `rejected`.
/// @param dateOfBirth `yyyy-MM-dd`.
/// @param licenceExpiry `yyyy-MM-dd`.
/// @param product `standard`, `comfort` or `xl`.
/// @param accessible Whether the car takes a wheelchair.
/// @param petFriendly Whether a rider's pet is welcome in the car.
/// @param documents One entry for each kind of document asked for, uploaded or not.
/// @param submittedAt Milliseconds since the epoch; 0 until it is handed in.
/// @param reviewedAt Milliseconds since the epoch; 0 until an admin decides.
/// @param reviewedBy The admin who decided; empty until one has.
@Mapped
public record DriverApplicationDto(
        String username,
        String name,
        String status,
        String legalName,
        String dateOfBirth,
        String licenceNumber,
        String licenceExpiry,
        String vehicleMake,
        String vehicleModel,
        int vehicleYear,
        String vehicleColor,
        String vehiclePlate,
        int vehicleSeats,
        String product,
        boolean accessible,
        boolean petFriendly,
        List<DocumentDto> documents,
        String rejectionReason,
        long submittedAt,
        long reviewedAt,
        String reviewedBy) {
}
