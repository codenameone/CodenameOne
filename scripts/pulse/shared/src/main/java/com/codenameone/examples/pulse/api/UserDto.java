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

/// Who is signed in, as the app needs to know it: enough to greet them, to
/// decide which modes to offer, and to tell whether they still owe a phone
/// verification.
///
/// @param username The sign-in name, which is the e-mail address the account was opened with.
/// @param member Every account is a member, and books for its household.
/// @param trainer Granted: teaches classes and sees their rosters.
/// @param desk Granted: works the front desk -- checks people in and takes walk-ins.
/// @param owner Granted: runs the studio -- plans, timetable, staff, campaigns, numbers.
/// @param suspended Blocked by an owner: the account cannot sign in or call the API.
/// @param flagged Marked by an owner for a closer look. The account still works; `suspended` is
///     what stops one.
/// @param flagReason Why it was flagged, for the other owners.
/// @param language The language the app is shown in: `en`, `es`, `fr`, `de` or `he`.
@Mapped
public record UserDto(
        String username,
        String displayName,
        String phone,
        boolean phoneVerified,
        boolean member,
        boolean trainer,
        boolean desk,
        boolean owner,
        boolean suspended,
        boolean flagged,
        String flagReason,
        String language) {
}
