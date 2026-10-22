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
package com.codenameone.examples.skirmish.api;

import com.codename1.annotations.Mapped;
import java.util.List;

/// The console's reports, read from the game's own records: accounts,
/// battles and purchases. The daily series have one entry per day, oldest
/// first, starting at `firstDay`.
///
/// @param firstDay the start of the first day, in milliseconds
/// @param active players who fought at least one battle that day
/// @param joined accounts created that day
/// @param battles battles finished that day
/// @param gems gems sold that day
/// @param funnel joined, played, won, paid and invited a friend
/// @param retention the share of players, in percent, seen again at least 1, 7 and 30 days after joining
/// @param trophies how many players are in each trophy band
@Mapped
public record InsightsDto(long firstDay, List<Integer> active, List<Integer> joined, List<Integer> battles,
        List<Integer> gems, List<NameCountDto> funnel, List<NameCountDto> retention, List<NameCountDto> trophies) {
}
