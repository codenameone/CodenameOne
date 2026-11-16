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
package com.codenameone.examples.fieldline.api;

import com.codename1.annotations.Mapped;

/// The company's planning rules as the Rules page edits them. Weights are
/// relative: what matters is how one compares with another. Rush hours are
/// minutes of the day: a drive that sets off inside one takes `rushExtraPct`
/// percent longer. Clocked time rounds to `timeRoundingMinutes`, and a shift
/// nobody clocked out of closes itself after `autoClockOutHours`.
@Mapped
public record PolicyDto(
        int breakMinutes,
        int breakAfterMinutes,
        int breakBeforeMinutes,
        boolean strictTerritories,
        int travelWeight,
        int overtimeWeight,
        int latenessWeight,
        int continuityWeight,
        int balanceWeight,
        int territoryWeight,
        int urgentSlaHours,
        int routineSlaHours,
        int rushAmFrom,
        int rushAmTo,
        int rushPmFrom,
        int rushPmTo,
        int rushExtraPct,
        int timeRoundingMinutes,
        int autoClockOutHours) {
}
