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

/// One product a ride can be taken as, priced for the ride being quoted.
///
/// @param product `standard`, `comfort` or `xl`.
/// @param fare Cents, like every amount here.
/// @param total What the rider would pay: [#fare()] plus [#serviceFee()].
/// @param driversNearby Drivers of this product in reach; 0 when there is none.
/// @param etaMinutes Minutes for the nearest of them to arrive; 0 when there is none.
@Mapped
public record FareOptionDto(
        String product,
        String name,
        String description,
        int seats,
        long fare,
        long serviceFee,
        long total,
        int driversNearby,
        int etaMinutes) {
}
