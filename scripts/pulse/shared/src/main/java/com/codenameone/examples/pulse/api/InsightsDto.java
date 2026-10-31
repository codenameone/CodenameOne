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
package com.codenameone.examples.pulse.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// How the studio is doing over the last twelve weeks, for the owner's
/// insights screen. Every figure is computed by the server from bookings,
/// check-ins, memberships and invoices; the app only draws it.
///
/// @param busyHours check-ins of the last eight weeks by weekday and hour,
///     Monday first: `busyHours.get(day * (lastHour - firstHour) + hour - firstHour)`,
///     in the studio's own time zone.
/// @param weeks one point a week, oldest first, the last the current week.
/// @param retention90 of the members who joined 90 to 180 days ago, the
///     percent still running a membership.
/// @param visitsPerMemberTenths average visits a week per active member, in
///     tenths (23 is 2.3).
/// @param revenuePerMemberCents monthly recurring revenue over active members.
@Mapped
public record InsightsDto(
        int firstHour,
        int lastHour,
        List<Integer> busyHours,
        List<WeekPointDto> weeks,
        List<TypeFillDto> classes,
        List<PlanMixDto> plans,
        List<CoachStatDto> coaches,
        int retention90,
        int visitsPerMemberTenths,
        int noShowPercent,
        long revenuePerMemberCents) {
}
