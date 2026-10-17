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
package com.codenameone.examples.morsel.api;

import com.codename1.annotations.Mapped;

/// Who is signed in, as the app needs to know it: enough to greet them, to
/// decide which modes to offer, and to tell whether they still owe a phone
/// verification.
///
/// @param username The sign-in name, which is the e-mail address the account was opened with.
/// @param customer Every account can delivery.
/// @param courier Granted: the account may deliver.
/// @param admin Granted: the account may run the service.
/// @param merchant On a restaurant's staff: the account takes its orders and keeps its menu.
/// @param suspended Blocked by an admin: the account cannot sign in or call the API.
/// @param vehicle A courier's car, as customers see it. Empty for anyone else.
/// @param plate The car's licence plate. Empty for anyone else.
/// @param gender `unspecified`, `female`, `male` or `nonbinary`.
/// @param courierStatus Where the account stands on onboarding: `none`, `draft`, `pending`, `approved`
///     or `rejected`. The COURIER role follows `approved`.
/// @param flagged Marked by an admin for a closer look. The account still works; `suspended` is
///     what stops one.
/// @param flagReason Why it was flagged, for the other admins.
/// @param language The language the app is shown in: `en`, `es`, `fr`, `de` or `he`.
@Mapped
public record UserDto(
        String username,
        String displayName,
        String phone,
        boolean phoneVerified,
        boolean customer,
        boolean courier,
        boolean admin,
        boolean merchant,
        boolean suspended,
        String vehicle,
        String plate,
        String gender,
        String courierStatus,
        boolean flagged,
        String flagReason,
        String language) {
}
