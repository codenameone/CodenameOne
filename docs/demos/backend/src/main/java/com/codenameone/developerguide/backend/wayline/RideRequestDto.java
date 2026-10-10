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

import com.codename1.annotations.Mapped;

// tag::wayline-contract-dto[]
/// Where a ride starts and ends, and how the rider wants it.
@Mapped
public class RideRequestDto {
    public double pickupLat;
    public double pickupLng;
    public String pickupAddress;
    public double dropoffLat;
    public double dropoffLng;
    public String dropoffAddress;
    /// A saved method's id, or `cash`.
    public String paymentMethodId;
    /// `standard`, `comfort` or `xl`; empty reads as standard.
    public String product;
    // tag::wayline-column-dto[]
    /// Whether a pet is coming: only drivers who take pets are offered the ride.
    public boolean petFriendly;
    // end::wayline-column-dto[]

    public RideRequestDto() {
    }
}
// end::wayline-contract-dto[]
