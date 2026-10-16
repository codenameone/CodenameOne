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
