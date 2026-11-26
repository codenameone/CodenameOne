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

/// A store's custom domain and the proof that the store owns it.
///
/// @param domain The hostname, lower case; empty for none.
/// @param token What the store publishes to prove it owns the domain.
/// @param verified Whether the proof was found; only then is the domain served.
/// @param verifiedAt When it was found, epoch milliseconds; 0 before.
/// @param fileUrl Where the token may be published as a file whose whole body is the token.
/// @param txtName The DNS name a TXT record may be added to instead.
/// @param txtValue The TXT record's value.
/// @param message What the last check found, or what to do next.
@Mapped
public record CustomDomainDto(
        String domain,
        String token,
        boolean verified,
        long verifiedAt,
        String fileUrl,
        String txtName,
        String txtValue,
        String message) {
}
