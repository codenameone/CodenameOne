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

/// A sale a register made, possibly offline. `clientId` is made by the register and names the sale
/// for good: sending it again is answered as a duplicate. `staffUsername` is who signed in with
/// their PIN and made the sale (the device's account when empty); `tipCents` is a tip taken
/// with the card, on top of the total; `approvalId` is a manager's approval from
/// `PosToolsApi.approve` when the sale needs one.
@Mapped
public record PosSaleDto(
        String clientId,
        String shiftId,
        String customerId,
        String email,
        String discountCode,
        String note,
        long createdAt,
        java.util.List<PosLineDto> lines,
        java.util.List<PosTenderDto> tenders,
        String staffUsername,
        long tipCents,
        String approvalId) {
}
