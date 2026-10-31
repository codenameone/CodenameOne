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

/// A trainer's own numbers over the last four weeks, and the game made of them.
///
/// @param regulars members who came to three or more of their classes.
/// @param attendeesByWeek people who came, a week at a time for eight weeks,
///     oldest first.
/// @param topClasses their kinds of class by attendance, most first.
/// @param points what the last eight weeks earned: 10 a check-in, 5 a kudos and
///     25 a full class.
/// @param level the level those points reach; `levelFloor` and `nextLevel` are
///     the points it starts at and the next one starts at.
/// @param weekStreak weeks running, back from this one, with a class taught.
/// @param fullHouses classes in the four weeks with every place booked.
/// @param rank where the trainer stands among the coaches by check-ins in the
///     four weeks, 1 the most; 0 when they taught nothing.
/// @param badges the coach badges earned, by id.
/// @param leaderboard every coach who taught in the four weeks with their
///     check-ins, most first.
@Mapped
public record TrainerStatsDto(int classes, int attendees, int fillPercent, int showPercent,
        int regulars, int kudos, List<Integer> attendeesByWeek, List<NameCountDto> topClasses,
        int points, int level, int levelFloor, int nextLevel, int weekStreak, int fullHouses,
        int rank, List<String> badges, List<NameCountDto> leaderboard) {
}
