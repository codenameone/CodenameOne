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

import java.util.List;

/// A driver's money: what came in, what is owed, and how the last two weeks
/// went. Amounts are cents, and are the driver's share -- the fare less the
/// service's commission, plus tips.
///
/// @param balance Earned and not yet paid out.
/// @param onlineMinutes Time on line today.
/// @param acceptanceRate 0 to 1.
/// @param daily The last 14 days, oldest first, every day present.
@Mapped
public record EarningsSummaryDto(
        String currency,
        long today,
        long week,
        long month,
        long balance,
        int trips,
        long tips,
        int onlineMinutes,
        double rating,
        double acceptanceRate,
        List<DayAmountDto> daily) {
}
