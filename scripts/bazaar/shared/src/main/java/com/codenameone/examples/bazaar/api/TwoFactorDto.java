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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// The caller's authenticator app.
///
/// @param secret While enrolling, the Base32 secret to type into the app; otherwise empty.
/// @param otpauthUri While enrolling, the `otpauth://` address the QR code carries.
/// @param recoveryCodes Answered once, when the codes are made.
/// @param recoveryLeft How many unused recovery codes remain.
/// @param requiredBy Stores the caller works at that require an authenticator app.
@Mapped
public record TwoFactorDto(
        boolean enabled,
        String secret,
        String otpauthUri,
        List<String> recoveryCodes,
        int recoveryLeft,
        List<String> requiredBy) {
}
