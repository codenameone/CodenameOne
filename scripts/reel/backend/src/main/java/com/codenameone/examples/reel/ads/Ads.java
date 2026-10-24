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
package com.codenameone.examples.reel.ads;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.api.AdBreakDto;
import com.codenameone.examples.reel.api.AdCampaignDto;
import com.codenameone.examples.reel.api.AdDto;
import com.codenameone.examples.reel.api.AdEventDto;
import com.codenameone.examples.reel.domain.AdCampaign;
import com.codenameone.examples.reel.domain.AdEvent;
import com.codenameone.examples.reel.repository.AdCampaignRepository;
import com.codenameone.examples.reel.repository.AdEventRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Ads: where the breaks go in a stream, which campaign fills each, and what
/// the player reports back.
///
/// A campaign pays a CPM, so every counted impression adds a thousandth of it
/// to what the campaign has spent (kept in millicents, so one impression of a
/// $4 CPM is a whole number). A campaign stops being chosen when its budget is
/// spent, before its start or after its end, and prefers titles in its genres.
///
/// The app plays the ads from the JSON this class answers and reports
/// impressions, clicks, skips and completions back through `/api/ads/event`,
/// signed in, so an event is tied to the session that showed the ad.
@Component
public class Ads {
    public static final String IMPRESSION = "impression";
    public static final String CLICK = "click";
    public static final String SKIP = "skip";
    public static final String COMPLETE = "complete";
    /// Written by the server when it hands a campaign to a session, never
    /// accepted from the player: the other events count only against it.
    public static final String SERVED = "served";
    /// A mid-roll every this many seconds of a long title.
    public static final int MIDROLL_EVERY = 600;
    /// Prefix of a creative URL that names a packaged Reel video instead of an
    /// external file: `video:ad-coffee`.
    public static final String VIDEO = "video:";

    private final AdCampaignRepository campaigns;
    private final AdEventRepository events;

    public Ads(AdCampaignRepository campaigns, AdEventRepository events) {
        this.campaigns = campaigns;
        this.events = events;
    }

    /// The breaks of a stream: a pre-roll, and a mid-roll every ten minutes of
    /// a title long enough to have one, never in its last minute.
    public List<AdBreakDto> breaks(String sessionId, int durationSeconds, int resumeSeconds) {
        List<AdBreakDto> out = new ArrayList<AdBreakDto>();
        out.add(new AdBreakDto(sessionId + "~0", 0, null, 5));
        int index = 1;
        for (int at = MIDROLL_EVERY; at < durationSeconds - 60; at += MIDROLL_EVERY) {
            if (at > resumeSeconds) {
                out.add(new AdBreakDto(sessionId + "~" + index, at, null, 5));
            }
            index++;
        }
        return out;
    }

    /// The campaign that fills a break: the eligible one furthest from its
    /// budget, preferring those that target one of the title's genres. One
    /// creative per break keeps breaks short; null when nothing is eligible.
    @Transactional(readOnly = true)
    public AdCampaign choose(List<String> genres, String breakId) {
        long now = System.currentTimeMillis();
        List<AdCampaign> all = campaigns.findByActiveTrue();
        AdCampaign best = null;
        double bestScore = -1;
        for (int iter = 0; iter < all.size(); iter++) {
            AdCampaign c = all.get(iter);
            if ((c.getStartsAt() > 0 && c.getStartsAt() > now) || (c.getEndsAt() > 0 && c.getEndsAt() < now)) {
                continue;
            }
            if (c.getBudgetCents() > 0 && c.getSpentMillicents() >= c.getBudgetCents() * 1000L) {
                continue;
            }
            double left = c.getBudgetCents() <= 0 ? 1.0
                    : 1.0 - (double) c.getSpentMillicents() / (c.getBudgetCents() * 1000.0);
            List<String> targets = Rows.split(c.getGenres());
            boolean matches = targets.isEmpty();
            for (int g = 0; !matches && genres != null && g < genres.size(); g++) {
                matches = targets.contains(genres.get(g));
            }
            // Spread breaks across equally good campaigns rather than always
            // the same one: the break id varies the tie break.
            double score = (matches ? 2.0 : 1.0) + left + ((breakId.hashCode() ^ c.getId().hashCode()) & 7) / 100.0;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    /// Counts an ad event. An impression or a click is counted once per
    /// campaign per session, whatever the player sends: a replayed request
    /// must not spend an advertiser's budget twice.
    @Transactional(rollbackFor = IOException.class)
    public void record(String username, AdEventDto event) throws IOException {
        if (event == null || event.campaignId() == null || event.sessionId() == null) {
            throw new ResponseStatusException(400, "Not an ad event");
        }
        String kind = event.event();
        if (!IMPRESSION.equals(kind) && !CLICK.equals(kind) && !SKIP.equals(kind) && !COMPLETE.equals(kind)) {
            throw new ResponseStatusException(400, "Not an ad event");
        }
        if (campaigns.findById(event.campaignId()) == null) {
            throw new ResponseStatusException(404, "No such campaign");
        }
        // Only a campaign this session was actually served can be reported,
        // or a viewer could post impressions for every campaign and spend any
        // advertiser's budget.
        if (events.countBySessionIdAndCampaignIdAndEvent(event.sessionId(), event.campaignId(), SERVED) == 0
                || events.countBySessionIdAndCampaignIdAndEvent(event.sessionId(), event.campaignId(), kind) > 0) {
            return;
        }
        insert(username, event.sessionId(), event.campaignId(), kind);
        if (IMPRESSION.equals(kind)) {
            campaigns.impression(event.campaignId());
        } else if (CLICK.equals(kind)) {
            campaigns.click(event.campaignId());
        }
    }

    /// Notes that a session was handed a campaign, once.
    @Transactional(rollbackFor = IOException.class)
    public void served(String username, String sessionId, String campaignId) throws IOException {
        if (events.countBySessionIdAndCampaignIdAndEvent(sessionId, campaignId, SERVED) == 0) {
            insert(username, sessionId, campaignId, SERVED);
        }
    }

    private void insert(String username, String sessionId, String campaignId, String kind) throws IOException {
        AdEvent row = new AdEvent();
        row.setId(Ids.next());
        row.setCampaignId(campaignId);
        row.setSessionId(sessionId);
        row.setUsername(username);
        row.setEvent(kind);
        row.setCreatedAt(System.currentTimeMillis());
        events.insert(row);
    }

    public static AdDto dto(AdCampaign c, String mediaUrl, String breakId) {
        return new AdDto(breakId + ":" + c.getId(), c.getId(), mediaUrl, c.getDurationSeconds(),
                c.getSkippableAfterSeconds(), c.getClickUrl(), c.getAdvertiser(), new ArrayList<String>());
    }

    // -------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<AdCampaignDto> list() {
        List<AdCampaign> all = campaigns.findAll();
        List<AdCampaignDto> out = new ArrayList<AdCampaignDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(campaign(all.get(iter)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public AdCampaignDto save(AdCampaignDto wanted) throws IOException {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        AdCampaign row = wanted.id() == null || wanted.id().length() == 0 ? null : campaigns.findById(wanted.id());
        boolean fresh = row == null;
        if (fresh) {
            row = new AdCampaign();
            row.setId(Ids.next());
            row.setCreatedAt(System.currentTimeMillis());
        }
        row.setAdvertiser(text(wanted.advertiser(), 120, "the advertiser"));
        row.setName(text(wanted.name(), 120, "the campaign name"));
        String creative = wanted.creativeUrl() == null ? "" : wanted.creativeUrl().trim();
        if (!creative.startsWith(VIDEO) && !creative.startsWith("https://")) {
            throw new ResponseStatusException(400, "The creative is video:<id> or an https address");
        }
        row.setCreativeUrl(creative.length() > 255 ? creative.substring(0, 255) : creative);
        row.setDurationSeconds(clamp(wanted.durationSeconds(), 1, 120));
        row.setSkippableAfterSeconds(clamp(wanted.skippableAfterSeconds(), 0, 120));
        String click = wanted.clickUrl() == null ? "" : wanted.clickUrl().trim();
        if (click.length() > 0 && !click.startsWith("https://")) {
            throw new ResponseStatusException(400, "The click address must be https");
        }
        row.setClickUrl(click.length() > 255 ? click.substring(0, 255) : click);
        row.setCpmCents(Math.max(0L, wanted.cpmCents()));
        row.setBudgetCents(Math.max(0L, wanted.budgetCents()));
        row.setGenres(Rows.join(wanted.genres()));
        row.setActive(wanted.active());
        row.setStartsAt(Math.max(0L, wanted.startsAt()));
        row.setEndsAt(Math.max(0L, wanted.endsAt()));
        if (fresh) {
            campaigns.insert(row);
        } else {
            campaigns.save(row);
        }
        return campaign(row);
    }

    public static AdCampaignDto campaign(AdCampaign c) {
        return new AdCampaignDto(c.getId(), c.getAdvertiser(), c.getName(), c.getCreativeUrl(),
                c.getDurationSeconds(), c.getSkippableAfterSeconds(), c.getClickUrl(), c.getCpmCents(),
                c.getBudgetCents(), c.getSpentMillicents() / 1000L, (int) c.getImpressions(), (int) c.getClicks(),
                Rows.split(c.getGenres()), c.isActive(), c.getStartsAt(), c.getEndsAt());
    }

    private static String text(String value, int max, String what) {
        String t = value == null ? "" : value.trim();
        if (t.length() == 0 || t.length() > max) {
            throw new ResponseStatusException(400, "Fill in " + what + " (at most " + max + " characters)");
        }
        return t;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
