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

/// A gift card. `code` is shown in full only when it is issued; afterwards only `lastFour`. With a `recipientEmail` the card is mailed there, with `message`, on `deliverAt` (epoch milliseconds; 0 sends it at once); `deliveredAt` is when it went, 0 while it waits.
@Mapped
public record GiftCardDto(
        String id,
        String code,
        String lastFour,
        long initialCents,
        long balanceCents,
        boolean active,
        String note,
        long expiresAt,
        String customerId,
        long createdAt,
        String recipientEmail,
        String recipientName,
        String message,
        long deliverAt,
        long deliveredAt) {
}
