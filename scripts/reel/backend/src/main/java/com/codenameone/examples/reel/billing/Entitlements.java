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
package com.codenameone.examples.reel.billing;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.domain.Plan;
import com.codenameone.examples.reel.domain.Rental;
import com.codenameone.examples.reel.domain.Subscription;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.repository.PlanRepository;
import com.codenameone.examples.reel.repository.RentalRepository;
import com.codenameone.examples.reel.repository.SubscriptionRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.util.List;

/// What an account may watch, and how: the one place that turns a plan, a
/// rental and the feature switches into a yes or a no.
///
/// The rules, in order:
///
/// - With paid plans switched off every title is free, and ads play where ads
///   are switched on.
/// - A FREE title plays for anyone, with ads unless the plan is ad free.
/// - A SUBSCRIPTION title needs a live subscription: trialing or active, its
///   period not yet over. A cancelled subscription stays live to the end of
///   the period that was paid for.
/// - A RENTAL title needs a rental that has not expired, whatever the plan.
///
/// The creator of a title, moderators and admins play it whatever it costs:
/// they have to see what they publish and what they judge.
@Component
public class Entitlements {
    public static final String TRIALING = "TRIALING";
    public static final String ACTIVE = "ACTIVE";
    public static final String PAST_DUE = "PAST_DUE";
    public static final String CANCELED = "CANCELED";
    public static final String NONE = "NONE";

    public static final String FREE = "FREE";
    public static final String SUBSCRIPTION = "SUBSCRIPTION";
    public static final String RENTAL = "RENTAL";

    /// How long a renewal that failed keeps the subscription playing while
    /// the card is retried.
    public static final long GRACE_MILLIS = 3L * 86400000L;

    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final RentalRepository rentals;
    private final Settings settings;

    public Entitlements(SubscriptionRepository subscriptions, PlanRepository plans,
            RentalRepository rentals, Settings settings) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.rentals = rentals;
        this.settings = settings;
    }

    /// What an account's plan gives it, right now.
    public static final class Allowance {
        public final String planCode;
        public final String planName;
        public final String status;
        public final long periodEndsAt;
        public final boolean live;
        public final boolean ads;
        public final int maxStreams;
        public final String maxQuality;
        public final boolean downloads;

        Allowance(String planCode, String planName, String status, long periodEndsAt, boolean live,
                boolean ads, int maxStreams, String maxQuality, boolean downloads) {
            this.planCode = planCode;
            this.planName = planName;
            this.status = status;
            this.periodEndsAt = periodEndsAt;
            this.live = live;
            this.ads = ads;
            this.maxStreams = maxStreams;
            this.maxQuality = maxQuality;
            this.downloads = downloads;
        }
    }

    /// The decision for one title.
    public static final class Access {
        public final boolean allowed;
        public final boolean ads;
        /// Why not, in words the app shows; empty when allowed.
        public final String reason;
        /// 402 for something money fixes, 403 for something it does not.
        public final int status;

        Access(boolean allowed, boolean ads, String reason, int status) {
            this.allowed = allowed;
            this.ads = ads;
            this.reason = reason;
            this.status = status;
        }
    }

    @Transactional(readOnly = true)
    public Allowance allowance(String username) {
        boolean adsOn = settings.enabled(Features.ADS);
        boolean downloadsOn = settings.enabled(Features.DOWNLOADS);
        if (!settings.enabled(Features.SUBSCRIPTIONS)) {
            // No plans: one generous allowance for everybody, paid for by ads
            // when ads are on.
            return new Allowance("", "Free", NONE, 0L, true, adsOn, 4, "1080", downloadsOn);
        }
        Subscription sub = username == null ? null : subscriptions.findById(username);
        long now = System.currentTimeMillis();
        if (sub != null && isLive(sub, now)) {
            Plan plan = plans.findById(sub.getPlanCode());
            if (plan != null) {
                return new Allowance(plan.getCode(), plan.getName(), sub.getStatus(),
                        sub.getPeriodEndsAt(), true, adsOn && plan.isAdSupported(),
                        Math.max(1, plan.getMaxStreams()), plan.getMaxQuality(),
                        downloadsOn && plan.isDownloads());
            }
        }
        String status = sub == null ? NONE : sub.getStatus();
        return new Allowance("", "No plan", status, sub == null ? 0L : sub.getPeriodEndsAt(), false,
                adsOn, 1, "480", false);
    }

    /// Whether a subscription row is playing now.
    public static boolean isLive(Subscription sub, long now) {
        String status = sub.getStatus();
        if (TRIALING.equals(status) || ACTIVE.equals(status) || CANCELED.equals(status)) {
            return sub.getPeriodEndsAt() > now;
        }
        if (PAST_DUE.equals(status)) {
            return sub.getPeriodEndsAt() + GRACE_MILLIS > now;
        }
        return false;
    }

    /// May `username` play `title`, and with ads? `staff` is true for the
    /// title's creator and for moderators and admins.
    @Transactional(readOnly = true)
    public Access access(String username, Title title, boolean staff) {
        if (staff) {
            return new Access(true, false, "", 200);
        }
        Allowance allowance = allowance(username);
        String kind = title.getAccess();
        if (RENTAL.equals(kind) && settings.enabled(Features.RENTALS)) {
            if (hasRental(username, title.getId())) {
                return new Access(true, false, "", 200);
            }
            return new Access(false, false, "Rent this title to watch it", 402);
        }
        if (FREE.equals(kind) || !settings.enabled(Features.SUBSCRIPTIONS)) {
            return new Access(true, allowance.ads, "", 200);
        }
        if (allowance.live) {
            return new Access(true, allowance.ads, "", 200);
        }
        return new Access(false, false, "Choose a plan to watch this", 402);
    }

    @Transactional(readOnly = true)
    public boolean hasRental(String username, String titleId) {
        if (username == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        List<Rental> held = rentals.findByUsernameAndTitleId(username, titleId);
        for (int iter = 0; iter < held.size(); iter++) {
            if (held.get(iter).getExpiresAt() > now) {
                return true;
            }
        }
        return false;
    }

    /// The tallest rendition a quality cap allows.
    public static int maxHeight(String quality) {
        if ("2160".equals(quality)) {
            return 2160;
        }
        if ("1080".equals(quality)) {
            return 1080;
        }
        if ("720".equals(quality)) {
            return 720;
        }
        return 480;
    }
}
