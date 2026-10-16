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

/// What opening an account takes. The password travels once, in the body of a
/// request over TLS, and the server keeps only its hash.
///
/// @param driver The older spelling of [#wantsToDrive()], kept for apps built before it. It no
///     longer grants anything: it opens a driving application like the newer flag.
/// @param wantsToDrive Opens a driving application with the account. The account is a rider until
///     an admin approves the application.
/// @param gender `unspecified`, `female`, `male` or `nonbinary`; empty reads as unspecified.
@Mapped
public record RegisterDto(
        String email,
        String password,
        String displayName,
        String phone,
        boolean driver,
        String vehicle,
        String plate,
        boolean wantsToDrive,
        String gender) {
}
