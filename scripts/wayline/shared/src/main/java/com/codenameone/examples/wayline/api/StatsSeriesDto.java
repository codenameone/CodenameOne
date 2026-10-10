/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// The admin's charts: how the service did, day by day.
@Mapped
public class StatsSeriesDto {
    /// Oldest first, every day present.
    public List<DayStatsDto> days;
    /// Rides asked for in each hour of the day (UTC), 24 numbers.
    public List<Integer> byHour;
    public List<NameCountDto> byState;
    public List<NameCountDto> byProduct;
    /// The drivers who completed the most rides in the period; `rides` is that count.
    public List<DriverDto> topDrivers;
    /// Cents.
    public long averageFare;
    /// From a ride being asked for to a driver taking it.
    public double averageWaitSeconds;
    /// Completed rides over rides asked for, 0 to 1.
    public double completionRate;
    public int pendingApplications;
    public int flaggedUsers;
    public String currency;

    public StatsSeriesDto() {
    }
}
