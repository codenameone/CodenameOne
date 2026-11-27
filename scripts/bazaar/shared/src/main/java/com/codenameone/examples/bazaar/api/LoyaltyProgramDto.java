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

/// A store's loyalty programme. `rounding` DOWN, NEAREST or UP; `tierBasis` SPEND (cents) or POINTS, over the last 365 days; `expiryDays` 0 for points that never lapse, else days without activity.
@Mapped
public record LoyaltyProgramDto(
        boolean active,
        int pointsPerUnit,
        long unitCents,
        String rounding,
        int pointsPerReward,
        long rewardCents,
        int minRedeemPoints,
        int expiryDays,
        boolean earnOnPos,
        String tierBasis,
        int referrerPoints,
        int refereePoints,
        List<LoyaltyTierDto> tiers) {
}
