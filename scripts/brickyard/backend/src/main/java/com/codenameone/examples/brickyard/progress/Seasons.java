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
package com.codenameone.examples.brickyard.progress;

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.Staff;
import com.codenameone.examples.brickyard.api.SeasonDto;
import com.codenameone.examples.brickyard.domain.Season;
import com.codenameone.examples.brickyard.moderation.Audit;
import com.codenameone.examples.brickyard.repository.SeasonRepository;
import java.io.IOException;
import java.util.List;

/// The seasons of the season pass. One is current at a time: the latest to
/// have started. Before an admin starts the first, the current season is the
/// preseason, id "". Season progress -- season XP, the premium pass and the
/// tier rewards claimed -- belongs to one season and starts over in the next
/// (see [Progression]); levels, XP and streaks carry on.
@Component
public class Seasons {
    public static final long DEFAULT_LENGTH = 30 * Clock.DAY;
    public static final String PRESEASON = "";

    private final SeasonRepository seasons;
    private final Audit audit;

    public Seasons(SeasonRepository seasons, Audit audit) {
        this.seasons = seasons;
        this.audit = audit;
    }

    /// The season in progress, or a preseason that never ends.
    public Season current() {
        List<Season> started = seasons.startedBy(Clock.now(), new PageRequest(0, 1));
        if (!started.isEmpty()) {
            return started.get(0);
        }
        Season pre = new Season();
        pre.setId(PRESEASON);
        pre.setName("Preseason");
        pre.setEndsAt(Long.MAX_VALUE);
        return pre;
    }

    public SeasonDto describe() {
        return toDto(current());
    }

    /// An admin ends the current season now and starts the next, named
    /// `name`, running until `endsAt` (thirty days when 0).
    @Transactional(rollbackFor = IOException.class)
    public SeasonDto start(SeasonDto request) throws IOException {
        Staff.require(Staff.ADMINISTER);
        if (request == null) {
            throw new ResponseStatusException(400, "Name the season");
        }
        long now = Clock.now();
        long ends = request.endsAt() == 0 ? now + DEFAULT_LENGTH : request.endsAt();
        if (ends <= now || ends > now + 366 * Clock.DAY) {
            throw new ResponseStatusException(400, "A season ends within a year from now");
        }
        Season s = open(Text.required(request.name(), 80, "a name").trim(), ends);
        audit.record("SEASON_START", s.getId(), s.getName());
        return toDto(s);
    }

    /// The scheduled rollover: a season that has run out is followed by the
    /// next, of the same length, so season progress resets on time without
    /// an admin. Answers the new season, or null when none was due.
    @Transactional(rollbackFor = IOException.class)
    public Season rollOver() throws IOException {
        Season c = current();
        long now = Clock.now();
        if (PRESEASON.equals(c.getId()) || c.getEndsAt() > now) {
            return null;
        }
        long length = Math.max(Clock.DAY, c.getEndsAt() - c.getStartsAt());
        Season s = open("Season after " + c.getName(), now + length);
        audit.record("SEASON_ROLLOVER", s.getId(), s.getName());
        return s;
    }

    private Season open(String name, long ends) throws IOException {
        long now = Clock.now();
        Season c = current();
        // Strictly after the season it replaces, so "the latest to have
        // started" is never a tie, even within one millisecond.
        long start = PRESEASON.equals(c.getId()) ? now : Math.max(now, c.getStartsAt() + 1);
        if (!PRESEASON.equals(c.getId()) && c.getEndsAt() > start) {
            c.setEndsAt(start);
            seasons.save(c);
        }
        Season s = new Season();
        s.setId(Ids.next());
        s.setName(name);
        s.setStartsAt(start);
        s.setEndsAt(Math.max(ends, start + 1));
        s.setCreatedAt(now);
        seasons.insert(s);
        return s;
    }

    static SeasonDto toDto(Season s) {
        return new SeasonDto(s.getId(), s.getName(), s.getStartsAt(), s.getEndsAt());
    }
}
