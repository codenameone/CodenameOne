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
package com.codenameone.examples.parley.api;

import com.codename1.annotations.Mapped;

/// Who is signed in, as the app needs to know it: enough to greet them, to
/// decide whether to offer the admin console, and to tell whether they still
/// owe a phone verification.
///
/// @param username The sign-in name, the e-mail address the account was opened with.
/// @param displayName The name other people see.
/// @param phone The phone number, as entered.
/// @param phoneVerified The phone number was confirmed with a code.
/// @param admin Granted: the account may run the service.
/// @param suspended An admin blocked the account.
/// @param flagged An admin marked the account for attention.
/// @param flagReason Why it was flagged, or empty.
/// @param language The language the account asked for, or empty for the device's.
/// @param about The one-line status shown under the name.
/// @param hasPhoto A profile photo was uploaded.
@Mapped
public record UserDto(
        String username,
        String displayName,
        String phone,
        boolean phoneVerified,
        boolean admin,
        boolean suspended,
        boolean flagged,
        String flagReason,
        String language,
        String about,
        boolean hasPhoto) {
}
