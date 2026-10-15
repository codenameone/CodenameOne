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

/// How one person wants the app and their rides. Kept on the server, so it
/// follows the account to another phone.
///
/// @param language `en`, `es`, `fr`, `de` or `he`.
/// @param theme `system`, `light` or `dark`.
/// @param units `km` or `mi`.
/// @param driverGender `any` or `women`: whom a ride may be offered to.
/// @param defaultPaymentMethodId A saved method's id, or `cash`.
/// @param defaultTipPercent 0, 10, 15 or 20.
/// @param shareTripsWith The phone number trips are shared with, or empty.
@Mapped
public record PreferencesDto(
        String language,
        String theme,
        String units,
        boolean notifyRideUpdates,
        boolean notifyReceipts,
        boolean notifyPromotions,
        String driverGender,
        boolean quietRide,
        boolean accessibleVehicle,
        boolean petFriendly,
        String defaultPaymentMethodId,
        int defaultTipPercent,
        String shareTripsWith) {
}
