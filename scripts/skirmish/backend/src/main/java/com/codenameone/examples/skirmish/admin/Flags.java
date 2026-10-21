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
package com.codenameone.examples.skirmish.admin;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.Text;
import com.codenameone.examples.skirmish.api.FlagDto;
import com.codenameone.examples.skirmish.domain.Flag;
import com.codenameone.examples.skirmish.domain.MatchRecord;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.push.GamePushes;
import com.codenameone.examples.skirmish.repository.FlagRepository;
import com.codenameone.examples.skirmish.repository.MatchRecordRepository;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Flags: the queue the admin console works through.
///
/// Two sources fill it. Players report an opponent after a match (one report
/// per player per match), and the server raises its own AUTO flags when it sees
/// what an honest client never does: plays it had to refuse, a flood of plays,
/// a state that diverged from the server's, or leaving match after match.
// tag::skirmish-flags[]
@Component
public class Flags {
    public static final String REPORT = "REPORT";
    public static final String AUTO = "AUTO";

    private final FlagRepository flags;
    private final ProfileRepository profiles;
    private final MatchRecordRepository matches;
    private final GamePushes pushes;
    private final AdminChannel channel;
    private final List<String> adminNames;

    public Flags(FlagRepository flags, ProfileRepository profiles, MatchRecordRepository matches,
            GamePushes pushes, AdminChannel channel, @Value("${skirmish.admin.notify:}") String adminNames) {
        this.flags = flags;
        this.profiles = profiles;
        this.matches = matches;
        this.pushes = pushes;
        this.channel = channel;
        this.adminNames = com.codenameone.examples.skirmish.account.Lists.split(adminNames);
    }

    /// A player reporting the opponent of a match they played.
    @Transactional(rollbackFor = IOException.class)
    public FlagDto report(String reporter, String matchId, String reason) throws IOException {
        MatchRecord m = matchId == null ? null : matches.findById(matchId);
        if (m == null || !(reporter.equals(m.getPlayer0()) || reporter.equals(m.getPlayer1()))) {
            throw new ResponseStatusException(404, "No such match");
        }
        String accused = reporter.equals(m.getPlayer0()) ? m.getPlayer1() : m.getPlayer0();
        if (accused.length() == 0) {
            throw new ResponseStatusException(400, "The training bot cannot be reported");
        }
        if (flags.countByReporterAndMatchId(reporter, matchId) > 0) {
            throw new ResponseStatusException(409, "You already reported this match");
        }
        String why = Text.required(reason, 255, "what happened");
        return raise(accused, REPORT, why, matchId, reporter);
    }

    /// Something the server noticed itself.
    public void auto(String username, String reason, String matchId) {
        try {
            raise(username, AUTO, reason, matchId == null ? "" : matchId, "");
        } catch (IOException | RuntimeException failed) {
            System.err.println("[flags] could not flag " + username + ": " + failed);
        }
    }

    FlagDto raise(String username, String kind, String reason, String matchId, String reporter)
            throws IOException {
        Flag f = store(username, kind, reason, matchId, reporter);
        channel.flagged(username, reason);
        pushes.flagRaised(adminNames, username, reason);
        return toDto(f, profiles.findById(username));
    }

    @Transactional(rollbackFor = IOException.class)
    Flag store(String username, String kind, String reason, String matchId, String reporter)
            throws IOException {
        Flag f = new Flag();
        f.setId(Ids.next());
        f.setUsername(username);
        f.setKind(kind);
        f.setReason(reason);
        f.setMatchId(matchId);
        f.setReporter(reporter);
        f.setCreatedAt(Clock.now());
        f.setPending(true);
        flags.insert(f);
        Profile p = profiles.findById(username);
        if (p != null && !p.isFlagged()) {
            p.setFlagged(true);
            profiles.save(p);
        }
        return f;
    }

    @Transactional(rollbackFor = IOException.class)
    public FlagDto resolve(String admin, String id) {
        Flag f = flags.findById(id);
        if (f == null) {
            throw new ResponseStatusException(404, "No such flag");
        }
        f.setPending(false);
        f.setResolvedBy(admin);
        flags.save(f);
        Profile p = profiles.findById(f.getUsername());
        if (p != null && flags.countByUsernameAndPendingTrue(f.getUsername()) == 0) {
            p.setFlagged(false);
            profiles.save(p);
        }
        return toDto(f, p);
    }

    @Transactional(readOnly = true)
    public List<FlagDto> list(boolean pending) {
        List<FlagDto> out = new ArrayList<FlagDto>();
        for (Flag f : flags.inState(pending, new PageRequest(0, 200))) {
            out.add(toDto(f, profiles.findById(f.getUsername())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<FlagDto> of(String username) {
        List<FlagDto> out = new ArrayList<FlagDto>();
        Profile p = profiles.findById(username);
        for (Flag f : flags.of(username, new PageRequest(0, 50))) {
            out.add(toDto(f, p));
        }
        return out;
    }

    public long pendingCount() {
        return flags.countByPendingTrue();
    }

    static FlagDto toDto(Flag f, Profile p) {
        return new FlagDto(f.getId(), f.getUsername(), p == null ? f.getUsername() : p.getDisplayName(),
                f.getKind(), f.getReason(), f.getMatchId(), f.getReporter(), f.getCreatedAt(), f.isPending());
    }
}
// end::skirmish-flags[]
