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

/// What an app charges a store, which the store's owner approves or declines.
///
/// @param kind `ONE_TIME` or `RECURRING` (every 30 days).
/// @param amount In the currency's minor units.
/// @param trialDays Days before a recurring charge's first bill; 0 for none.
/// @param status `PENDING`, `ACTIVE` (approved), `DECLINED` or `CANCELLED`.
/// @param returnUrl Where the app wants the owner sent after deciding.
/// @param confirmUrl The web page where the owner decides, on this server.
/// @param billingStartsAt When the first bill falls due, or 0 until approved.
@Mapped
public record AppChargeDto(
        String id,
        String appId,
        String appName,
        String name,
        String kind,
        long amount,
        String currency,
        int trialDays,
        String status,
        String returnUrl,
        String confirmUrl,
        long createdAt,
        long decidedAt,
        long billingStartsAt) {
}
