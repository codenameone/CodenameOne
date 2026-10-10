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

/// What a ride cost and how it was paid. Every amount is in cents.
@Mapped
public class ReceiptDto {
    public String rideId;
    public String currency;
    /// The three parts of the fare.
    public long baseFare;
    public long distanceFare;
    public long timeFare;
    public long serviceFee;
    public long tip;
    public long discount;
    /// The fare, the service fee and the tip, less the discount.
    public long total;
    public String methodLabel;
    /// `pending`, `paid`, `failed`, `refunded` or `cash`.
    public String status;
    /// Milliseconds since the epoch, or 0 while unpaid.
    public long paidAt;
    public String pickupName;
    public String dropoffName;
    public double distanceMeters;
    public double durationSeconds;
    public String driverName;
    public String vehicle;
    /// Who paid. Filled for an admin; a rider's own receipts leave it empty.
    public String riderUsername;

    public ReceiptDto() {
    }
}
