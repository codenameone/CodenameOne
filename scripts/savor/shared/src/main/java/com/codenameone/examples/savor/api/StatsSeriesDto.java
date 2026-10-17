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
package com.codenameone.examples.savor.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// The admin's charts: how the service did, day by day.
///
/// @param days Oldest first, every day present.
/// @param byHour Deliveries asked for in each hour of the day (UTC), 24 numbers.
/// @param topCouriers The couriers who completed the most deliveries in the period; `deliveries` is that count.
/// @param averageFare Cents.
/// @param averageWaitSeconds From a delivery being asked for to a courier taking it.
/// @param completionRate Completed deliveries over deliveries asked for, 0 to 1.
@Mapped
public record StatsSeriesDto(
        List<DayStatsDto> days,
        List<Integer> byHour,
        List<NameCountDto> byState,
        List<NameCountDto> byProduct,
        List<CourierDto> topCouriers,
        long averageFare,
        double averageWaitSeconds,
        double completionRate,
        int pendingApplications,
        int flaggedUsers,
        String currency) {
}
