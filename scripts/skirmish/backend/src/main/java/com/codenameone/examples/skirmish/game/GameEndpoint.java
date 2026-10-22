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
package com.codenameone.examples.skirmish.game;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.skirmish.Caller;
import com.codenameone.examples.skirmish.SecurityConfig;
import com.codenameone.examples.skirmish.account.Lists;
import com.codenameone.examples.skirmish.admin.Flags;
import com.codenameone.examples.skirmish.api.ChallengeDto;
import com.codenameone.examples.skirmish.api.DeckDto;
import com.codenameone.examples.skirmish.api.FlagDto;
import com.codenameone.examples.skirmish.api.GameApiServer;
import com.codenameone.examples.skirmish.api.LeaderDto;
import com.codenameone.examples.skirmish.api.MatchSummaryDto;
import com.codenameone.examples.skirmish.api.ReplayDto;
import com.codenameone.examples.skirmish.api.ReportDto;
import com.codenameone.examples.skirmish.domain.MatchRecord;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.repository.MatchRecordRepository;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.rules.Cards;
import java.util.ArrayList;
import java.util.List;

@Component
public class GameEndpoint implements GameApiServer {
    private final ProfileRepository profiles;
    private final MatchRecordRepository matches;
    private final Challenges challenges;
    private final Flags flags;

    public GameEndpoint(ProfileRepository profiles, MatchRecordRepository matches, Challenges challenges,
            Flags flags) {
        this.profiles = profiles;
        this.matches = matches;
        this.challenges = challenges;
        this.flags = flags;
    }

    @Override
    @Transactional(readOnly = true)
    public DeckDto deck() throws Exception {
        return new DeckDto(Lists.split(profile(Caller.name()).getDeck()));
    }

    /// The deck is checked with the same rule the match itself uses.
    @Override
    @Transactional
    public DeckDto saveDeck(DeckDto deck) throws Exception {
        List<String> cards = deck == null || deck.cards() == null ? new ArrayList<String>() : deck.cards();
        if (!Cards.isValidDeck(cards.toArray(new String[cards.size()]))) {
            throw new ResponseStatusException(400, "A deck is eight different cards");
        }
        Profile p = profile(Caller.name());
        p.setDeck(Lists.join(cards));
        profiles.save(p);
        return new DeckDto(cards);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchSummaryDto> history() throws Exception {
        String me = Caller.name();
        return summaries(me, matches.of(me, new PageRequest(0, 30)));
    }

    public static List<MatchSummaryDto> summaries(String me, List<MatchRecord> rows) {
        List<MatchSummaryDto> out = new ArrayList<MatchSummaryDto>();
        for (MatchRecord m : rows) {
            int seat = me.equals(m.getPlayer0()) ? 0 : 1;
            int result = m.getResult() == -1 ? 0 : (m.getResult() == seat ? 1 : -1);
            out.add(new MatchSummaryDto(m.getId(), seat == 0 ? m.getPlayer1() : m.getPlayer0(),
                    seat == 0 ? m.getName1() : m.getName0(), result,
                    seat == 0 ? m.getCrowns0() : m.getCrowns1(), seat == 0 ? m.getCrowns1() : m.getCrowns0(),
                    seat == 0 ? m.getTrophies0() : m.getTrophies1(), m.getEndedAt(), m.getTicks()));
        }
        return out;
    }

    /// Anyone signed in may watch any finished match: replays are how players
    /// learn, and they contain nothing private.
    @Override
    @Transactional(readOnly = true)
    public ReplayDto replay(String id) throws Exception {
        MatchRecord m = matches.findById(id);
        if (m == null) {
            throw new ResponseStatusException(404, "No such match");
        }
        return new ReplayDto(m.getId(), m.getPlayer0(), m.getPlayer1(), m.getName0(), m.getName1(),
                m.getResult(), m.getCrowns0(), m.getCrowns1(), m.getReplay(), m.getRulesVersion(), m.getEndedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderDto> leaderboard(int limit) throws Exception {
        int n = limit < 1 || limit > 100 ? 50 : limit;
        List<LeaderDto> out = new ArrayList<LeaderDto>();
        int rank = 1;
        for (Profile p : profiles.leaders(new PageRequest(0, n))) {
            out.add(new LeaderDto(rank++, p.getDisplayName(), p.getTrophies(), p.getWins()));
        }
        return out;
    }

    @Override
    public ChallengeDto challenge() throws Exception {
        return challenges.create(Caller.name());
    }

    @Override
    public FlagDto report(ReportDto report) throws Exception {
        if (report == null) {
            throw new ResponseStatusException(400, "Nothing to report");
        }
        return flags.report(Caller.name(), report.matchId(), report.reason());
    }

    private Profile profile(String username) {
        Profile p = profiles.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return p;
    }
}
