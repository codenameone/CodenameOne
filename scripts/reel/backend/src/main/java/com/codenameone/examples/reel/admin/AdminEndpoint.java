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
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.account.Accounts;
import com.codenameone.examples.reel.account.Devices;
import com.codenameone.examples.reel.account.Moderation;
import com.codenameone.examples.reel.ads.Ads;
import com.codenameone.examples.reel.api.AdCampaignDto;
import com.codenameone.examples.reel.api.AdminApiServer;
import com.codenameone.examples.reel.api.AdminStatsDto;
import com.codenameone.examples.reel.api.AppConfigDto;
import com.codenameone.examples.reel.api.BrandingDto;
import com.codenameone.examples.reel.api.CouponDto;
import com.codenameone.examples.reel.api.FeatureFlagDto;
import com.codenameone.examples.reel.api.FlagDto;
import com.codenameone.examples.reel.api.JobDto;
import com.codenameone.examples.reel.api.PlanDto;
import com.codenameone.examples.reel.api.PresetDto;
import com.codenameone.examples.reel.api.RoleChangeDto;
import com.codenameone.examples.reel.api.SessionDto;
import com.codenameone.examples.reel.api.SharingAlertDto;
import com.codenameone.examples.reel.api.SharingDecisionDto;
import com.codenameone.examples.reel.api.StatsSeriesDto;
import com.codenameone.examples.reel.api.SuspendDto;
import com.codenameone.examples.reel.api.UserDetailDto;
import com.codenameone.examples.reel.api.UserDto;
import com.codenameone.examples.reel.billing.Billing;
import com.codenameone.examples.reel.catalog.Catalog;
import com.codenameone.examples.reel.pay.Payments;
import com.codenameone.examples.reel.playback.Playbacks;
import com.codenameone.examples.reel.settings.Settings;
import com.codenameone.examples.reel.studio.Jobs;

import java.util.ArrayList;
import java.util.List;

/// The admin console. Everything under `/api/admin/` is limited to admins in
/// [com.codenameone.examples.reel.SecurityConfig]; `/api/config` is the one
/// public call, what every app reads at start to theme itself and to know
/// which features to show.
@Component
public class AdminEndpoint implements AdminApiServer {
    private final Accounts accounts;
    private final Devices devices;
    private final Playbacks playbacks;
    private final Settings settings;
    private final Billing billing;
    private final Ads ads;
    private final Jobs jobs;
    private final Stats stats;
    private final Sharing sharing;
    private final Moderation history;
    private final Payments payments;

    public AdminEndpoint(Accounts accounts, Devices devices, Playbacks playbacks, Settings settings,
            Billing billing, Ads ads, Jobs jobs, Stats stats, Sharing sharing, Moderation history,
            Payments payments) {
        this.accounts = accounts;
        this.devices = devices;
        this.playbacks = playbacks;
        this.settings = settings;
        this.billing = billing;
        this.ads = ads;
        this.jobs = jobs;
        this.stats = stats;
        this.sharing = sharing;
        this.history = history;
        this.payments = payments;
    }

    @Override
    public AppConfigDto config() throws Exception {
        return new AppConfigDto(settings.branding(), settings.preset(), settings.flags(),
                payments.config(settings.currency()));
    }

    @Override
    public AdminStatsDto stats() throws Exception {
        return stats.now();
    }

    @Override
    public StatsSeriesDto statsSeries(int days) throws Exception {
        return stats.series(days == 0 ? 30 : days);
    }

    /// Accounts newest first, filtered by a fragment of the address or name.
    @Override
    public List<UserDto> users(String q) throws Exception {
        String wanted = q == null ? "" : Catalog.fold(q.trim());
        List<String> all = accounts.newestFirst();
        List<UserDto> out = new ArrayList<UserDto>();
        for (int iter = 0; iter < all.size() && out.size() < 100; iter++) {
            String username = all.get(iter);
            if (wanted.length() > 0 && Catalog.fold(username).indexOf(wanted) < 0
                    && Catalog.fold(accounts.displayName(username)).indexOf(wanted) < 0) {
                continue;
            }
            out.add(summary(username));
        }
        return out;
    }

    @Override
    public UserDetailDto user(String username) throws Exception {
        UserDto user = summary(username);
        return new UserDetailDto(user, devices.list(username), playbacks.liveFor(username),
                billing.invoices(username), history.events(username), sharing.forUser(username));
    }

    @Override
    public UserDto setRoles(String username, RoleChangeDto roles) throws Exception {
        accounts.setRoles(Caller.name(), username, roles == null ? null : roles.roles());
        return summary(username);
    }

    /// Suspending also ends whatever the account is streaming now.
    @Override
    public UserDto suspend(String username, SuspendDto suspend) throws Exception {
        boolean on = suspend != null && suspend.suspended();
        accounts.suspend(Caller.name(), username, on, suspend == null ? "" : suspend.reason());
        if (on) {
            playbacks.endAll(username, "SUSPENDED");
        }
        return summary(username);
    }

    @Override
    public UserDto flag(String username, FlagDto flag) throws Exception {
        accounts.flag(Caller.name(), username, flag != null && flag.flagged(), flag == null ? "" : flag.reason());
        return summary(username);
    }

    @Override
    public List<SessionDto> sessions() throws Exception {
        return playbacks.allLive();
    }

    @Override
    public String endSession(String id) throws Exception {
        playbacks.endSession(id);
        return "ok";
    }

    @Override
    public List<FeatureFlagDto> flags() throws Exception {
        return settings.flags();
    }

    @Override
    public FeatureFlagDto setFlag(String key, FeatureFlagDto flag) throws Exception {
        return settings.setFlag(Caller.name(), key, flag != null && flag.enabled());
    }

    @Override
    public List<PresetDto> presets() throws Exception {
        return settings.presets();
    }

    @Override
    public List<FeatureFlagDto> applyPreset(String id) throws Exception {
        return settings.applyPreset(Caller.name(), id);
    }

    @Override
    public BrandingDto setBranding(BrandingDto branding) throws Exception {
        return settings.setBranding(Caller.name(), branding);
    }

    @Override
    public List<PlanDto> plans() throws Exception {
        return billing.plans(true);
    }

    @Override
    public PlanDto savePlan(String code, PlanDto plan) throws Exception {
        return billing.savePlan(code, plan);
    }

    @Override
    public List<CouponDto> coupons() throws Exception {
        return billing.coupons();
    }

    @Override
    public CouponDto saveCoupon(String code, CouponDto coupon) throws Exception {
        return billing.saveCoupon(code, coupon);
    }

    @Override
    public List<AdCampaignDto> campaigns() throws Exception {
        return ads.list();
    }

    @Override
    public AdCampaignDto saveCampaign(AdCampaignDto campaign) throws Exception {
        return ads.save(campaign);
    }

    @Override
    public List<SharingAlertDto> sharingAlerts(String status) throws Exception {
        return sharing.list(status);
    }

    @Override
    public SharingAlertDto decideSharing(String id, SharingDecisionDto decision) throws Exception {
        return sharing.decide(Caller.name(), id, decision);
    }

    @Override
    public List<JobDto> jobs(String status) throws Exception {
        return jobs.list(status);
    }

    @Override
    public JobDto retryJob(String id) throws Exception {
        return jobs.retry(id);
    }

    private UserDto summary(String username) throws Exception {
        return accounts.summary(username, devices.count(username), playbacks.liveFor(username).size());
    }
}
