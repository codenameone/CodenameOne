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

/// One technician's week: the paid minutes their roster holds
/// (`plannedMinutes`), what they clocked on shift, driving, on
/// site and on a break, the overtime past their regular week, and what it
/// cost at their rate.
@Mapped
public record TimesheetRowDto(
        String techId,
        String name,
        String rateName,
        int plannedMinutes,
        int shiftMinutes,
        int travelMinutes,
        int jobMinutes,
        int breakMinutes,
        int overtimeMinutes,
        int weekendMinutes,
        int callouts,
        long costCents,
        List<TimeEntryDto> entries) {
}
