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
package com.codenameone.examples.dash.admin;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.dash.Caller;
import com.codenameone.examples.dash.Clock;
import com.codenameone.examples.dash.account.Accounts;
import com.codenameone.examples.dash.api.AdminActionDto;
import com.codenameone.examples.dash.api.AdminApiServer;
import com.codenameone.examples.dash.api.AdminStatsDto;
import com.codenameone.examples.dash.api.AssetDto;
import com.codenameone.examples.dash.api.AuditDto;
import com.codenameone.examples.dash.api.BroadcastDto;
import com.codenameone.examples.dash.api.FlagDto;
import com.codenameone.examples.dash.api.ForgeActionDto;
import com.codenameone.examples.dash.api.LiveRaceDto;
import com.codenameone.examples.dash.api.PackSummaryDto;
import com.codenameone.examples.dash.api.PlayerDetailDto;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.api.PipelineDto;
import com.codenameone.examples.dash.api.PromoteDto;
import com.codenameone.examples.dash.api.QueryDto;
import com.codenameone.examples.dash.api.QueryResultDto;
import com.codenameone.examples.dash.api.InsightsDto;
import com.codenameone.examples.dash.api.SpotlightDto;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.domain.StorePurchase;
import com.codenameone.examples.dash.forge.Forge;
import com.codenameone.examples.dash.game.GameEndpoint;
import com.codenameone.examples.dash.game.Rooms;
import com.codenameone.examples.dash.repository.RaceEntryRepository;
import com.codenameone.examples.dash.repository.RaceRecordRepository;
import com.codenameone.examples.dash.repository.ProfileRepository;
import com.codenameone.examples.dash.repository.StorePurchaseRepository;
import com.codenameone.examples.dash.shop.Shop;
import com.codenameone.examples.dash.social.Insights;
import com.codenameone.examples.dash.social.Pipelines;
import com.codenameone.examples.dash.social.Promotion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// God mode. The security chain already requires ADMIN for `/api/admin/**`.
// tag::dash-admin[]
@Component
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final ProfileRepository profiles;
    private final RaceRecordRepository races;
    private final RaceEntryRepository entries;
    private final StorePurchaseRepository purchases;
    private final Flags flags;
    private final Moderation moderation;
    private final Rooms rooms;
    private final Shop shop;

    private final Forge forge;
    private final Promotion promotion;
    private final Insights insights;
    private final Pipelines pipelines;

    public AdminEndpoint(Accounts accounts, ProfileRepository profiles, RaceRecordRepository races, RaceEntryRepository entries,
            StorePurchaseRepository purchases, Flags flags, Moderation moderation, Rooms rooms, Shop shop,
            Forge forge, Promotion promotion, Insights insights, Pipelines pipelines) {
        this.insights = insights;
        this.pipelines = pipelines;
        this.forge = forge;
        this.promotion = promotion;
        this.accounts = accounts;
        this.profiles = profiles;
        this.races = races;
        this.entries = entries;
        this.purchases = purchases;
        this.flags = flags;
        this.moderation = moderation;
        this.rooms = rooms;
        this.shop = shop;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto stats() throws Exception {
        Instant today = Instant.ofEpochMilli(Clock.dayOf(Clock.now()));
        int gems = 0;
        List<StorePurchase> sold = purchases.since(today);
        for (StorePurchase s : sold) {
            gems += s.getGems();
        }
        return new AdminStatsDto((int) profiles.count(), rooms.online(), rooms.live().size(),
                (int) races.countByEndedAtGreaterThan(today), (int) flags.pendingCount(),
                (int) count(profiles.suspended(Instant.now())), sold.size(), gems);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerDto> players(String query) throws Exception {
        String q = query == null ? "" : query.trim();
        List<PlayerDto> out = new ArrayList<PlayerDto>();
        for (Profile p : profiles.search("%" + q + "%", new PageRequest(0, 100))) {
            out.add(accounts.toDto(p));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerDetailDto player(String username) throws Exception {
        Profile p = profiles.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return new PlayerDetailDto(accounts.toDto(p),
                GameEndpoint.summaries(entries.history(username, new PageRequest(0, 30))),
                flags.of(username), moderation.of(username), shop.purchasesOf(username));
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
    public FlagDto resolve(String id) throws Exception {
        return flags.resolve(Caller.name(), id);
    }

    @Override
    public List<LiveRaceDto> liveRaces() throws Exception {
        return rooms.live();
    }

    private static long count(Long n) {
        return n == null ? 0 : n.longValue();
    }

    @Override
    public List<AuditDto> audit() throws Exception {
        return moderation.newest();
    }

    @Override
    public List<PackSummaryDto> forgeQueue() throws Exception {
        return forge.queue();
    }

    @Override
    public List<AssetDto> forgeAssets() throws Exception {
        return forge.pendingAssets();
    }

    @Override
    public PackSummaryDto forgePackAction(ForgeActionDto body) throws Exception {
        if (body == null) {
            throw new ResponseStatusException(400, "Nothing to do");
        }
        return forge.packAction(Caller.name(), body.target(), body.action(), body.reason());
    }

    @Override
    public AssetDto forgeAssetAction(ForgeActionDto body) throws Exception {
        if (body == null) {
            throw new ResponseStatusException(400, "Nothing to do");
        }
        return forge.assetAction(Caller.name(), body.target(), body.action(), body.reason());
    }

    @Override
    public PromoteDto promote() throws Exception {
        return promotion.overview();
    }

    @Override
    public BroadcastDto audience(BroadcastDto body) throws Exception {
        return promotion.audience(body);
    }

    @Override
    public BroadcastDto broadcast(BroadcastDto body) throws Exception {
        return promotion.broadcast(Caller.name(), body);
    }

    @Override
    public SpotlightDto spotlight(SpotlightDto body) throws Exception {
        return promotion.spotlight(Caller.name(), body);
    }

    @Override
    public InsightsDto reports(int days) throws Exception {
        return insights.report(days);
    }

    @Override
    public QueryResultDto query(QueryDto body) throws Exception {
        return insights.query(body);
    }

    @Override
    public List<PipelineDto> pipelines() throws Exception {
        return pipelines.list();
    }

    @Override
    public PipelineDto savePipeline(PipelineDto body) throws Exception {
        return pipelines.save(Caller.name(), body);
    }

    @Override
    public PipelineDto deletePipeline(String id) throws Exception {
        return pipelines.delete(Caller.name(), id);
    }

    @Override
    public PipelineDto runPipeline(String id) throws Exception {
        return pipelines.run(id);
    }
}
