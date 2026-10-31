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
package com.codenameone.examples.pulse.api;

import com.codename1.annotations.Mapped;

/// How one person wants the app and its reminders. Kept on the server, so it
/// follows the account to another phone.
///
/// @param language `en`, `es`, `fr`, `de` or `he`.
/// @param theme `system`, `light` or `dark`.
/// @param units `km` or `mi`, for distances run.
/// @param notifyClasses Reminders before a booked class, and a place opening up from a waitlist.
/// @param notifyPromotions Nudges: a class with room in it, a friend's invitation, a campaign.
/// @param defaultPaymentMethodId A saved card's id; memberships renew on it.
/// @param quietFrom Hour of the day, 0 to 23, from which no nudge is sent.
/// @param quietTo Hour at which nudges may start again. Equal to `quietFrom` means no quiet hours.
@Mapped
public record PreferencesDto(
        String language,
        String theme,
        String units,
        boolean notifyClasses,
        boolean notifyReceipts,
        boolean notifyPromotions,
        String defaultPaymentMethodId,
        int quietFrom,
        int quietTo) {
}
