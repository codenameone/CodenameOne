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

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.reel.api.AdCampaignDto;
import com.codenameone.examples.reel.api.CouponDto;
import com.codenameone.examples.reel.api.FeatureFlagDto;
import com.codenameone.examples.reel.api.JobDto;
import com.codenameone.examples.reel.api.PlanDto;
import com.codenameone.examples.reel.api.PresetDto;
import com.codenameone.examples.reel.api.SeriesPointDto;
import com.codenameone.examples.reel.api.SessionDto;
import com.codenameone.examples.reel.api.SharingAlertDto;
import com.codenameone.examples.reel.api.SharingDecisionDto;
import com.codenameone.examples.reel.api.UserDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.ui.Chart;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Nav;
import com.codenameone.examples.reel.ui.SettingsForm;
import com.codenameone.examples.reel.ui.Shell;
import com.codenameone.examples.reel.ui.Ui;
import java.util.List;

/// The operator's console: how the service is doing, who is on it, what it
/// offers (features, plans, coupons, ad campaigns) and what needs a decision
/// (account sharing, failed processing).
public final class AdminForm implements Nav.Home {
    private final Shell shell = new Shell("Admin", Nav.ADMIN);

    @Override
    public void start() {
        final Form form = shell.form();
        shell.menu().item(FontImage.MATERIAL_DASHBOARD, "Dashboard", "dashboard", this::dashboard)
                .item(FontImage.MATERIAL_PEOPLE, "Users", "users", this::users)
                .item(FontImage.MATERIAL_CAST, "Watching now", "sessions", this::sessions)
                .item(FontImage.MATERIAL_TUNE, "Features", "features", this::features)
                .item(FontImage.MATERIAL_PAYMENTS, "Plans and coupons", "plans", this::plans)
                .item(FontImage.MATERIAL_CAMPAIGN, "Ad campaigns", "campaigns", this::campaigns)
                .item(FontImage.MATERIAL_GROUP_OFF, "Account sharing", "sharing", this::sharing)
                .item(FontImage.MATERIAL_MEMORY, "Processing", "jobs", this::jobs)
                .item(FontImage.MATERIAL_SETTINGS, "Settings", "settings", () -> SettingsForm.show(form));
        shell.done();
        dashboard();
    }

    @Override
    public Form form() {
        return shell.form();
    }

    private void dashboard() {
        final Container page = Ui.list("dashboardPage");
        shell.show("Dashboard", page);
        Ui.pull(page, this::dashboard);
        Api.admin().stats(Net.to(s -> {
            page.removeAll();
            Container facts = Ui.page();
            facts.add(Ui.fact("Accounts", String.valueOf(s.accounts())))
                    .add(Ui.fact("Subscriptions", s.activeSubscriptions() + Ui.DOT + Lang.tr("{0} trialing",
                            String.valueOf(s.trialing()))))
                    .add(Ui.fact("Monthly revenue", Ui.money(s.mrrCents(), s.currency())))
                    .add(Ui.fact("Streams now", String.valueOf(s.streamsNow())))
                    .add(Ui.fact("Minutes today", String.valueOf(s.minutesToday())))
                    .add(Ui.fact("Titles", String.valueOf(s.titles())))
                    .add(Ui.fact("Ad impressions today", String.valueOf(s.adImpressionsToday())))
                    .add(Ui.fact("Ad revenue today", Ui.money(s.adRevenueCentsToday(), s.currency())));
            page.add(facts);
            if (s.pendingModeration() > 0) {
                page.add(Ui.callout(FontImage.MATERIAL_GAVEL, "Moderation",
                        Lang.tr("{0} items wait for a moderator", String.valueOf(s.pendingModeration()))));
            }
            if (s.jobsFailed() > 0) {
                page.add(Ui.tap(Ui.callout(FontImage.MATERIAL_ERROR, "Processing",
                        Lang.tr("{0} jobs failed", String.valueOf(s.jobsFailed()))), "failedJobs", e -> jobs()));
            }
            if (s.sharingAlerts() > 0) {
                page.add(Ui.tap(Ui.callout(FontImage.MATERIAL_GROUP_OFF, "Account sharing",
                        Lang.tr("{0} open alerts", String.valueOf(s.sharingAlerts()))), "openAlerts", e -> sharing()));
            }
            final String currency = s.currency();
            Api.admin().statsSeries(30, Net.to(series -> {
                chart(page, "Sign ups", "signups", series.signups());
                chart(page, "Minutes watched", "minutes", series.minutes());
                chart(page, "Revenue (cents)", "revenue", series.revenue());
                chart(page, "Cancellations", "churn", series.churn());
                if (series.topTitles() != null && !series.topTitles().isEmpty()) {
                    Ui.section(page, "Most watched");
                    for (int iter = 0; iter < series.topTitles().size(); iter++) {
                        page.add(Ui.row((char) 0, series.topTitles().get(iter).name(), null,
                                Ui.label(String.valueOf(series.topTitles().get(iter).count()), "SvMuted")));
                    }
                }
                page.revalidate();
            }, Ui::fail));
            page.revalidate();
        }, Ui.failed(page, this::dashboard)));
    }

    private static void chart(Container page, String title, String name, List<SeriesPointDto> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        double[] v = new double[points.size()];
        for (int iter = 0; iter < v.length; iter++) {
            v[iter] = points.get(iter).value();
        }
        Ui.section(page, title);
        page.add(Chart.of(name, v, points.get(0).day(), points.get(v.length - 1).day()));
    }

    private void users() {
        final Container list = Ui.list("userList");
        Container whole = new Container(new BorderLayout());
        Container top = Ui.page();
        final TextField q = Ui.field(top, "Search", "userSearch", "Name or email");
        whole.add(BorderLayout.NORTH, top);
        whole.add(BorderLayout.CENTER, list);
        shell.show("Users", whole);
        final Runnable search = () -> Api.admin().users(q.getText().trim(), Net.to(users -> {
            list.removeAll();
            for (int iter = 0; users != null && iter < users.size(); iter++) {
                final UserDto u = users.get(iter);
                String state = u.suspended() ? Lang.tr("Suspended") : u.flagged() ? Lang.tr("Flagged")
                        : u.subscriptionStatus() == null ? "" : Lang.tr(u.subscriptionStatus());
                list.add(Ui.tap(Ui.row(FontImage.MATERIAL_PERSON, u.name(), u.email() + Ui.DOT
                        + Lang.tr("{0} devices", String.valueOf(u.devices())) + (u.streamsNow() > 0 ? Ui.DOT
                        + Lang.tr("watching") : ""), state.length() == 0 ? null : Ui.badge(state,
                        u.suspended() ? "SvBadgeBad" : "SvBadge")), "user-" + iter,
                        e -> UserForm.show(shell.form(), u.username())));
            }
            list.revalidate();
        }, Ui::fail));
        q.addActionListener(e -> search.run());
        q.addDataChangedListener((type, index) -> search.run());
        search.run();
    }

    private void sessions() {
        final Container list = Ui.list("sessionList");
        shell.show("Watching now", list);
        Api.admin().sessions(Net.to(sessions -> {
            list.removeAll();
            if (sessions == null || sessions.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_TV_OFF, "Nobody is watching", null));
            }
            for (int iter = 0; sessions != null && iter < sessions.size(); iter++) {
                final SessionDto s = sessions.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_CAST, s.titleName(), s.username() + Ui.DOT + s.deviceName()
                        + Ui.DOT + s.country() + Ui.DOT + Ui.when(s.startedAt()),
                        Ui.flat(FontImage.MATERIAL_STOP, "end-" + iter, e -> Api.admin().endSession(s.id(),
                                Net.to(x -> sessions(), Ui::fail)))));
            }
            list.revalidate();
        }, Ui.failed(list, this::sessions)));
    }

    private void features() {
        final Container list = Ui.list("featureList");
        shell.show("Features", list);
        Api.admin().presets(Net.to(presets -> Api.admin().flags(Net.to(flags -> {
            list.removeAll();
            Ui.section(list, "Presets");
            for (int iter = 0; presets != null && iter < presets.size(); iter++) {
                final PresetDto p = presets.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_AUTO_AWESOME, p.name(), p.description(), p.active()
                        ? Ui.badge(Lang.tr("Active"), "SvBadgeGood") : Ui.link("Apply", "preset-" + p.id(),
                        e -> Api.admin().applyPreset(p.id(), Net.to(x -> features(), Ui::fail)))));
            }
            String group = null;
            for (int iter = 0; flags != null && iter < flags.size(); iter++) {
                final FeatureFlagDto f = flags.get(iter);
                if (!f.group().equals(group)) {
                    group = f.group();
                    Ui.section(list, Lang.tr(group));
                }
                list.add(Ui.row((char) 0, f.label(), f.value() == null || f.value().length() == 0 ? null : f.value(),
                        Ui.toggle("flag-" + f.key(), f.enabled(), on -> Api.admin().setFlag(f.key(),
                                new FeatureFlagDto(f.key(), f.label(), f.group(), on, f.value()),
                                Net.to(x -> com.codenameone.examples.reel.Telemetry.flagChanged(f.key()), Ui::fail)))));
            }
            list.add(Ui.secondary("Branding", "branding", e -> BrandingForm.show(shell.form())));
            list.revalidate();
        }, Ui.failed(list, this::features))), Ui.failed(list, this::features)));
    }

    private void plans() {
        final Container list = Ui.list("planList");
        shell.show("Plans and coupons", list);
        Api.admin().plans(Net.to(plans -> Api.admin().coupons(Net.to(coupons -> {
            list.removeAll();
            Ui.section(list, "Plans");
            for (int iter = 0; plans != null && iter < plans.size(); iter++) {
                final PlanDto p = plans.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_WORKSPACE_PREMIUM, p.name(), Ui.money(p.priceCents(), p.currency())
                        + " / " + Lang.tr(p.interval()) + Ui.DOT + p.maxQuality() + Ui.DOT
                        + Lang.tr("{0} streams", String.valueOf(p.maxStreams())) + (p.adSupported() ? Ui.DOT
                        + Lang.tr("with ads") : ""), Ui.toggle("plan-" + p.code(), p.active(),
                        on -> Api.admin().savePlan(p.code(), new PlanDto(p.code(), p.name(), p.description(),
                                p.priceCents(), p.currency(), p.interval(), p.maxStreams(), p.maxQuality(),
                                p.adSupported(), p.downloads(), p.trialDays(), on), Net.to(x -> { }, Ui::fail)))));
            }
            Ui.section(list, "Coupons");
            for (int iter = 0; coupons != null && iter < coupons.size(); iter++) {
                final CouponDto c = coupons.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_LOCAL_OFFER, c.code(), c.percentOff() + "%" + Ui.DOT
                        + Lang.tr("{0} of {1} used", String.valueOf(c.redemptions()), String.valueOf(c.maxRedemptions()))
                        + (c.expiresAt() > 0 ? Ui.DOT + Lang.tr("until {0}", Ui.day(c.expiresAt())) : ""),
                        Ui.toggle("coupon-" + c.code(), c.active(), on -> Api.admin().saveCoupon(c.code(),
                                new CouponDto(c.code(), c.percentOff(), c.periods(), c.maxRedemptions(),
                                        c.redemptions(), c.expiresAt(), on), Net.to(x -> { }, Ui::fail)))));
            }
            Container add = Ui.page();
            final TextField code = Ui.field(add, "New coupon code", "couponCode", "SPRING25");
            final TextField pct = Ui.field(add, "Percent off", "couponPercent", "25",
                    com.codename1.ui.TextArea.NUMERIC);
            final TextField max = Ui.field(add, "Uses", "couponUses", "100", com.codename1.ui.TextArea.NUMERIC);
            add.add(Ui.secondary("Add coupon", "addCoupon", e -> {
                int percent = number(pct.getText());
                String c = code.getText().trim();
                if (c.length() == 0 || percent < 1 || percent > 100) {
                    Ui.say(Lang.tr("A code and a percentage from 1 to 100"));
                    return;
                }
                Api.admin().saveCoupon(c, new CouponDto(c, percent, 1, Math.max(1, number(max.getText())), 0, 0, true),
                        Net.to(x -> plans(), Ui::fail));
            }));
            list.add(add);
            list.revalidate();
        }, Ui.failed(list, this::plans))), Ui.failed(list, this::plans)));
    }

    private void campaigns() {
        final Container list = Ui.list("campaignList");
        shell.show("Ad campaigns", list);
        Api.admin().campaigns(Net.to(campaigns -> {
            list.removeAll();
            if (campaigns == null || campaigns.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_CAMPAIGN, "No campaigns", null));
            }
            for (int iter = 0; campaigns != null && iter < campaigns.size(); iter++) {
                final AdCampaignDto c = campaigns.get(iter);
                String ctr = c.impressions() == 0 ? "-" : Ui.tenths(c.clicks() * 100.0 / c.impressions()) + "%";
                list.add(Ui.row(FontImage.MATERIAL_CAMPAIGN, c.name(), c.advertiser() + Ui.DOT
                        + Lang.tr("{0} impressions", String.valueOf(c.impressions())) + Ui.DOT + Lang.tr("CTR {0}", ctr)
                        + Ui.DOT + Ui.money(c.spentCents(), null) + " / " + Ui.money(c.budgetCents(), null),
                        Ui.toggle("campaign-" + c.id(), c.active(), on -> Api.admin().saveCampaign(new AdCampaignDto(
                                c.id(), c.advertiser(), c.name(), c.creativeUrl(), c.durationSeconds(),
                                c.skippableAfterSeconds(), c.clickUrl(), c.cpmCents(), c.budgetCents(), c.spentCents(),
                                c.impressions(), c.clicks(), c.genres(), on, c.startsAt(), c.endsAt()),
                                Net.to(x -> { }, Ui::fail)))));
            }
            list.revalidate();
        }, Ui.failed(list, this::campaigns)));
    }

    private void sharing() {
        final Container list = Ui.list("alertList");
        shell.show("Account sharing", list);
        Api.admin().sharingAlerts("OPEN", Net.to(alerts -> {
            list.removeAll();
            if (alerts == null || alerts.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_VERIFIED_USER, "No open alerts", null));
            }
            for (int iter = 0; alerts != null && iter < alerts.size(); iter++) {
                final SharingAlertDto a = alerts.get(iter);
                Container row = Ui.row(FontImage.MATERIAL_GROUP_OFF, a.username(), a.detail() + Ui.DOT
                        + Lang.tr("{0} addresses in {1} countries", String.valueOf(a.distinctIps()),
                        String.valueOf(a.distinctCountries())) + Ui.DOT + Ui.when(a.createdAt()), null);
                list.add(row);
                final int at = iter;
                list.add(com.codename1.ui.layouts.GridLayout.encloseIn(2,
                        Ui.secondary("Dismiss", "dismiss-" + at, e -> decide(a, "DISMISS")),
                        Ui.danger("Sign out everywhere", "signOutAll-" + at, e -> decide(a, "SIGN_OUT_ALL"))));
            }
            list.revalidate();
        }, Ui.failed(list, this::sharing)));
    }

    private void decide(final SharingAlertDto a, final String action) {
        Ui.reason(shell.form(), Lang.tr(action), Lang.tr("Kept on {0}'s record.", a.username()), Lang.tr(action),
                why -> Api.admin().decideSharing(a.id(), new SharingDecisionDto(action, why), Net.to(x -> {
                    shell.form().showBack();
                    sharing();
                }, Ui::fail)));
    }

    private void jobs() {
        final Container list = Ui.list("jobList");
        shell.show("Processing", list);
        Api.admin().jobs("", Net.to(jobs -> {
            list.removeAll();
            for (int iter = 0; jobs != null && iter < jobs.size(); iter++) {
                final JobDto j = jobs.get(iter);
                list.add(Ui.row(FontImage.MATERIAL_MEMORY, j.kind() + Ui.DOT + j.videoId(), Lang.tr(j.status())
                        + Ui.DOT + j.progressPercent() + "%" + (j.worker() == null ? "" : Ui.DOT + j.worker())
                        + (j.error() == null || j.error().length() == 0 ? "" : Ui.DOT + j.error()),
                        "FAILED".equals(j.status()) ? Ui.link("Retry", "retry-" + iter,
                                e -> Api.admin().retryJob(j.id(), Net.to(x -> jobs(), Ui::fail))) : null));
            }
            list.revalidate();
        }, Ui.failed(list, this::jobs)));
    }

    static int number(String text) {
        String s = text.trim();
        if (s.length() == 0 || s.length() > 9) {
            return 0;
        }
        for (int iter = 0; iter < s.length(); iter++) {
            if (s.charAt(iter) < '0' || s.charAt(iter) > '9') {
                return 0;
            }
        }
        return Integer.parseInt(s);
    }
}
