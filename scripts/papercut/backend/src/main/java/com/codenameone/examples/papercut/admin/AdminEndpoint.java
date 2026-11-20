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
package com.codenameone.examples.papercut.admin;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.papercut.Clock;
import com.codenameone.examples.papercut.account.Accounts;
import com.codenameone.examples.papercut.api.AdminActionDto;
import com.codenameone.examples.papercut.api.AdminApiServer;
import com.codenameone.examples.papercut.api.AdminStatsDto;
import com.codenameone.examples.papercut.api.AuditDto;
import com.codenameone.examples.papercut.api.BroadcastDto;
import com.codenameone.examples.papercut.api.DailyDto;
import com.codenameone.examples.papercut.api.DailySeedDto;
import com.codenameone.examples.papercut.api.FlagDto;
import com.codenameone.examples.papercut.api.PlayerDto;
import com.codenameone.examples.papercut.api.ResolveDto;
import com.codenameone.examples.papercut.domain.Flag;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.push.Broadcasts;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.repository.ScoreRunRepository;
import com.codenameone.examples.papercut.repository.StorePurchaseRepository;
import com.codenameone.examples.papercut.run.Boards;
import com.codenameone.examples.papercut.run.DailySeeds;
import com.codenameone.examples.papercut.run.Runs;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// `AdminApi` on the server. The ADMIN role is enforced for all of
/// `/api/admin` in [com.codenameone.examples.papercut.SecurityConfig], so no
/// method here can forget it; everything that changes something is audited.
@Component
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final ProfileRepository profiles;
    private final ScoreRunRepository runs;
    private final StorePurchaseRepository purchases;
    private final Flags flags;
    private final Moderation moderation;
    private final Runs runService;
    private final DailySeeds seeds;
    private final Boards boards;
    private final Broadcasts broadcasts;

    public AdminEndpoint(Accounts accounts, ProfileRepository profiles, ScoreRunRepository runs,
            StorePurchaseRepository purchases, Flags flags, Moderation moderation, Runs runService,
            DailySeeds seeds, Boards boards, Broadcasts broadcasts) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.runs = runs;
        this.purchases = purchases;
        this.flags = flags;
        this.moderation = moderation;
        this.runService = runService;
        this.seeds = seeds;
        this.boards = boards;
        this.broadcasts = broadcasts;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto stats() throws Exception {
        long now = Clock.now();
        Instant today = Instant.ofEpochMilli(Clock.dayOf(now));
        List<Profile> top = profiles.byBestClassic(new PageRequest(0, 1));
        return new AdminStatsDto((int) profiles.count(), profiles.seenSince(today).intValue(),
                runs.since(today).intValue(), (int) flags.open(),
                top.isEmpty() ? 0 : top.get(0).getBestClassic(), seeds.seedOf(now), (int) purchases.count());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerDto> players(String query) throws Exception {
        String q = query == null ? "" : query.trim();
        List<PlayerDto> out = new ArrayList<PlayerDto>();
        for (Profile p : profiles.search("%" + q + "%", new PageRequest(0, 50))) {
            out.add(accounts.toDto(p));
        }
        return out;
    }

    @Override
    public PlayerDto act(String username, AdminActionDto action) throws Exception {
        return moderation.act(Caller.name(), username, action);
    }

    @Override
    public List<FlagDto> flags(boolean open) throws Exception {
        return flags.list(open);
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public FlagDto resolve(String id, ResolveDto verdict) throws Exception {
        String v = verdict == null ? null : verdict.verdict();
        Flag f = flags.resolve(Caller.name(), id, v);
        if (Flags.REMOVE.equalsIgnoreCase(v)) {
            runService.recomputeBests(f.getUsername());
        }
        moderation.record(Caller.name(), f.getUsername(), "FLAG " + v, f.getReason());
        return flags.toDto(f);
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public DailyDto setDaily(DailySeedDto seed) throws Exception {
        long value = seed == null ? 0 : seed.seed();
        seeds.pick(Caller.name(), value);
        moderation.record(Caller.name(), "", "SET_DAILY", Long.toString(value));
        return boards.daily(Caller.name());
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public BroadcastDto broadcast(BroadcastDto broadcast) throws Exception {
        BroadcastDto sent = broadcasts.send(broadcast);
        moderation.record(Caller.name(), "", "BROADCAST", sent.title());
        return sent;
    }

    @Override
    public List<AuditDto> audit() throws Exception {
        return moderation.audit();
    }
}
