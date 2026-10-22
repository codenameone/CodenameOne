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
package com.codenameone.examples.parley.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// The admin dashboard's charts.
///
/// @param days One entry per day, oldest first.
/// @param byHour Envelopes per hour of the day, UTC, 24 entries.
/// @param byKind Conversations per kind.
/// @param byPlatform Devices per platform.
/// @param flaggedUsers Accounts flagged for attention.
/// @param openReports Reports nobody has handled.
@Mapped
public record StatsSeriesDto(
        List<DayStatsDto> days,
        List<Integer> byHour,
        List<NameCountDto> byKind,
        List<NameCountDto> byPlatform,
        int flaggedUsers,
        int openReports) {
}
