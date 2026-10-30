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

/// One class on the timetable, with how full it is and, for the caller, which of their
/// household hold a place in it (`booked`) or are queued for one (`waitlisted`): member
/// ids.
@Mapped
public record SessionDto(
        String id,
        String classTypeId,
        String name,
        String category,
        int intensity,
        int minAge,
        int maxAge,
        String color,
        String locationId,
        String locationName,
        String roomName,
        String trainer,
        String trainerName,
        long startsAt,
        long endsAt,
        int capacity,
        int booked,
        int waitlisted,
        String state,
        String note,
        List<String> mine,
        List<String> queued,
        int friendsGoing) {
}
