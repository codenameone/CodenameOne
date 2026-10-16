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
