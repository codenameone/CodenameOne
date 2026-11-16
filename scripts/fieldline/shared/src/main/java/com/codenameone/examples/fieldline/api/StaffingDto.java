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
import java.util.List;

/// The staffing report: capacity against demand by day and by skill, every
/// technician's load, how many jobs meet their SLA, and why the rest of the
/// work is unplaced. `lowPct` and `highPct` bound a healthy load: a day under
/// the first is overstaffed, a day over the second understaffed.
/// `plannedCostCents` is what the planned hours cost at each technician's rate.
@Mapped
public record StaffingDto(
        long from,
        int lowPct,
        int highPct,
        List<DayLoadDto> days,
        List<SkillLoadDto> skills,
        List<TechLoadDto> technicians,
        int jobsDue,
        int onTime,
        int late,
        int unassigned,
        List<ReasonCountDto> reasons,
        long plannedCostCents) {
}
