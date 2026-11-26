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

/// An e-mail or SMS campaign. `abPercent` of the audience is split between subjects A and B; after `winnerAfterMinutes` the subject with more opens goes to the rest. `status` DRAFT, SCHEDULED, TESTING, SENDING, SENT or CANCELLED.
@Mapped
public record CampaignDto(
        String id,
        String name,
        String channel,
        String audienceId,
        String subjectA,
        String subjectB,
        String body,
        int abPercent,
        int winnerAfterMinutes,
        String status,
        long scheduledAt,
        String winner,
        String colour,
        List<String> links) {
}
