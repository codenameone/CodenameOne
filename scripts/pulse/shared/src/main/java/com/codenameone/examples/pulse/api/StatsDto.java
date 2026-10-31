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

/// How a member is doing: visits, the current weekly streak, minutes trained,
/// and the points, level and badges they earned for it.
///
/// `levelFloor` and `nextLevel` are the points the current level starts at and
/// the next one does, for drawing progress between them; `nextLevel` equals
/// `levelFloor` at the top level. `badges` are the ids of the badges earned,
/// in the order they are listed in `Points.BADGES` on the server.
@Mapped
public record StatsDto(
        int visitsThisMonth,
        int visitsTotal,
        int weekStreak,
        int minutesThisMonth,
        int caloriesThisMonth,
        long lastVisit,
        List<Integer> weeklyVisits,
        int points,
        int level,
        int levelFloor,
        int nextLevel,
        List<String> badges) {
}
