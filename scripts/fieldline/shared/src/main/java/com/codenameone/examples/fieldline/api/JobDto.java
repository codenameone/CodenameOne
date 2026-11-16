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

/// A visit, as both the board and the technician see it. Times are epoch milliseconds, 0 when unset.
@Mapped
public record JobDto(
        String id,
        String title,
        String kind,
        String status,
        int priority,
        String siteId,
        String siteName,
        String address,
        double lat,
        double lng,
        String customer,
        String contactPhone,
        String assetType,
        String assetModel,
        String description,
        int durationMinutes,
        List<String> skills,
        long windowStart,
        long windowEnd,
        String techId,
        String techName,
        long plannedStart,
        long plannedEnd,
        int travelBefore,
        int sequence,
        long actualStart,
        long actualEnd,
        List<CheckDto> checklist,
        int photos,
        boolean signed,
        String notes,
        long dueAt,
        List<String> certifications,
        String afterJobId,
        List<String> excludedTechs,
        String territory,
        String unassignedReason) {
}
