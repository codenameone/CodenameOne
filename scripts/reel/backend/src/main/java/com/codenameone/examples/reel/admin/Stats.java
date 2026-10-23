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
package com.codenameone.examples.reel.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.ads.Ads;
import com.codenameone.examples.reel.api.AdminStatsDto;
import com.codenameone.examples.reel.api.NameCountDto;
import com.codenameone.examples.reel.api.SeriesPointDto;
import com.codenameone.examples.reel.api.StatsSeriesDto;
import com.codenameone.examples.reel.billing.Billing;
import com.codenameone.examples.reel.billing.Entitlements;
import com.codenameone.examples.reel.domain.AdCampaign;
import com.codenameone.examples.reel.domain.AdEvent;
import com.codenameone.examples.reel.domain.Invoice;
import com.codenameone.examples.reel.domain.Plan;
import com.codenameone.examples.reel.domain.Playback;
import com.codenameone.examples.reel.domain.Profile;
import com.codenameone.examples.reel.domain.Subscription;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.moderation.Reviews;
import com.codenameone.examples.reel.repository.AdCampaignRepository;
import com.codenameone.examples.reel.repository.AdEventRepository;
import com.codenameone.examples.reel.repository.InvoiceRepository;
import com.codenameone.examples.reel.repository.PlanRepository;
import com.codenameone.examples.reel.repository.PlaybackRepository;
import com.codenameone.examples.reel.repository.ProfileRepository;
import com.codenameone.examples.reel.repository.SharingAlertRepository;
import com.codenameone.examples.reel.repository.SubscriptionRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.settings.Settings;
import com.codenameone.examples.reel.studio.Jobs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/// The numbers on the admin dashboard, computed from the tables on request.
/// Days are UTC days.
@Component
public class Stats {
    private static final long DAY = 86400000L;

    private final ProfileRepository profiles;
    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final PlaybackRepository playbacks;
    private final TitleRepository titles;
    private final InvoiceRepository invoices;
    private final AdEventRepository adEvents;
    private final AdCampaignRepository campaigns;
    private final SharingAlertRepository alerts;
    private final Reviews reviews;
    private final Jobs jobs;
    private final Settings settings;

    public Stats(ProfileRepository profiles, SubscriptionRepository subscriptions, PlanRepository plans,
            PlaybackRepository playbacks, TitleRepository titles, InvoiceRepository invoices,
            AdEventRepository adEvents, AdCampaignRepository campaigns, SharingAlertRepository alerts,
            Reviews reviews, Jobs jobs, Settings settings) {
        this.profiles = profiles;
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.playbacks = playbacks;
        this.titles = titles;
        this.invoices = invoices;
        this.adEvents = adEvents;
        this.campaigns = campaigns;
        this.alerts = alerts;
        this.reviews = reviews;
        this.jobs = jobs;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public AdminStatsDto now() {
        long now = System.currentTimeMillis();
        long today = now - now % DAY;
        List<Subscription> active = subscriptions.findByStatus(Entitlements.ACTIVE);
        long mrr = 0;
        for (int iter = 0; iter < active.size(); iter++) {
            Plan p = plans.findById(active.get(iter).getPlanCode());
            if (p != null) {
                mrr += Billing.YEAR.equals(p.getBillingInterval()) ? p.getPriceCents() / 12 : p.getPriceCents();
            }
        }
        List<Playback> sinceMidnight = playbacks.findByStartedAtGreaterThan(today);
        long seconds = 0;
        int streams = 0;
        for (int iter = 0; iter < sinceMidnight.size(); iter++) {
            Playback p = sinceMidnight.get(iter);
            seconds += Math.max(0L, ((p.getEndedAt() > 0 ? p.getEndedAt() : p.getLastHeartbeatAt())
                    - p.getStartedAt()) / 1000L);
        }
        streams = (int) playbacks.countByEndedAt(0L);
        long impressions = Rows.number(adEvents.countSince(Ads.IMPRESSION, today));
        return new AdminStatsDto((int) profiles.count(), active.size(),
                (int) subscriptions.countByStatus(Entitlements.TRIALING), mrr, settings.currency(), streams,
                seconds / 60L, (int) titles.countByStatus(Kinds.PUBLISHED), reviews.pending(),
                jobs.count(Kinds.QUEUED), jobs.count(Kinds.FAILED), alerts.findByStatusOrderByCreatedAtDesc(Kinds.OPEN).size(),
                impressions, adRevenueSince(today));
    }

    @Transactional(readOnly = true)
    public StatsSeriesDto series(int days) {
        int n = Math.max(1, Math.min(90, days));
        long now = System.currentTimeMillis();
        long first = now - now % DAY - (n - 1) * DAY;
        Map<String, Long> signups = buckets(first, n);
        Map<String, Long> minutes = buckets(first, n);
        Map<String, Long> revenue = buckets(first, n);
        Map<String, Long> churn = buckets(first, n);
        List<Profile> people = profiles.findAll();
        for (int iter = 0; iter < people.size(); iter++) {
            add(signups, people.get(iter).getCreatedAt(), 1);
        }
        Map<String, Integer> plays = new HashMap<String, Integer>();
        List<Playback> sessions = playbacks.findByStartedAtGreaterThan(first);
        for (int iter = 0; iter < sessions.size(); iter++) {
            Playback p = sessions.get(iter);
            long end = p.getEndedAt() > 0 ? p.getEndedAt() : p.getLastHeartbeatAt();
            add(minutes, p.getStartedAt(), Math.max(0L, (end - p.getStartedAt()) / 60000L));
            Integer c = plays.get(p.getTitleId());
            plays.put(p.getTitleId(), Integer.valueOf(c == null ? 1 : c.intValue() + 1));
        }
        List<Invoice> paid = invoices.findByCreatedAtGreaterThan(first);
        for (int iter = 0; iter < paid.size(); iter++) {
            if (Billing.PAID.equals(paid.get(iter).getStatus())) {
                add(revenue, paid.get(iter).getCreatedAt(), paid.get(iter).getAmountCents());
            }
        }
        List<Subscription> ended = subscriptions.findByStatusAndUpdatedAtGreaterThan(Entitlements.CANCELED, first);
        for (int iter = 0; iter < ended.size(); iter++) {
            add(churn, ended.get(iter).getUpdatedAt(), 1);
        }
        List<NameCountDto> top = new ArrayList<NameCountDto>();
        for (Map.Entry<String, Integer> e : plays.entrySet()) {
            Title t = titles.findById(e.getKey());
            top.add(new NameCountDto(t == null ? e.getKey() : t.getName(), e.getValue().intValue()));
        }
        for (int i = 1; i < top.size(); i++) {
            NameCountDto d = top.get(i);
            int j = i - 1;
            while (j >= 0 && top.get(j).count() < d.count()) {
                top.set(j + 1, top.get(j));
                j--;
            }
            top.set(j + 1, d);
        }
        return new StatsSeriesDto(points(signups), points(minutes), points(revenue), points(churn),
                top.size() > 10 ? new ArrayList<NameCountDto>(top.subList(0, 10)) : top);
    }

    /// What the day's counted impressions earned: each campaign's CPM over a
    /// thousand, summed.
    private long adRevenueSince(long since) {
        Map<String, Long> cpm = new HashMap<String, Long>();
        List<AdCampaign> all = campaigns.findAll();
        for (int iter = 0; iter < all.size(); iter++) {
            cpm.put(all.get(iter).getId(), Long.valueOf(all.get(iter).getCpmCents()));
        }
        long millicents = 0;
        List<AdEvent> events = adEvents.findByEventAndCreatedAtGreaterThan(Ads.IMPRESSION, since);
        for (int iter = 0; iter < events.size(); iter++) {
            Long c = cpm.get(events.get(iter).getCampaignId());
            millicents += c == null ? 0 : c.longValue();
        }
        return millicents / 1000L;
    }

    private static Map<String, Long> buckets(long first, int n) {
        Map<String, Long> out = new LinkedHashMap<String, Long>();
        for (int iter = 0; iter < n; iter++) {
            out.put(day(first + iter * DAY), Long.valueOf(0));
        }
        return out;
    }

    private static void add(Map<String, Long> buckets, long at, long by) {
        String key = day(at);
        Long v = buckets.get(key);
        if (v != null) {
            buckets.put(key, Long.valueOf(v.longValue() + by));
        }
    }

    private static List<SeriesPointDto> points(Map<String, Long> buckets) {
        List<SeriesPointDto> out = new ArrayList<SeriesPointDto>();
        for (Map.Entry<String, Long> e : buckets.entrySet()) {
            out.add(new SeriesPointDto(e.getKey(), e.getValue().longValue()));
        }
        return out;
    }

    static String day(long at) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(at));
    }
}
