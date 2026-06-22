/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.appreview;

import com.codename1.io.Preferences;
import com.codename1.ui.CN;
import com.codename1.util.SuccessCallback;

/// Entry point for requesting an app store review and collecting feedback.
///
/// `AppReview` prefers the platform's native review prompt
/// (`SKStoreReviewController` on iOS, the Play In-App Review API on Android)
/// and transparently falls back to a Codename One drawn rating widget on the
/// simulator, desktop, the web target and platforms/OS versions without a
/// native prompt.
///
/// There are two ways to use it:
///
/// 1. **Manual** -- call [#requestReview] at a moment that makes sense in your
///    app (e.g. right after the user completed a meaningful task). You decide
///    the timing entirely.
///
/// 2. **Scheduled** -- configure the engagement heuristics once and call
///    [#registerSession] on every app start. `AppReview` keeps a small amount
///    of state in [com.codename1.io.Preferences] (launch count, install date,
///    last prompt time) and only prompts once the thresholds are met and the
///    user has not already rated or opted out.
///
/// The fallback widget routes high ratings to the store and low ratings to a
/// private feedback channel so unhappy users are heard before they post a one
/// star public review (see [#setHighRatingThreshold] and
/// [#setFeedbackListener]).
///
/// ```java
/// AppReview.getInstance()
///         .setStoreUrl("https://apps.apple.com/app/id0000000000")
///         .setSupportEmail("support@example.com")
///         .registerSession();
/// ```
public class AppReview {
    private static final String PREF_LAUNCHES = "cn1$appReview$launches";
    private static final String PREF_FIRST_INSTALL = "cn1$appReview$firstInstall";
    private static final String PREF_LAST_PROMPT = "cn1$appReview$lastPrompt";
    private static final String PREF_COMPLETED = "cn1$appReview$completed";

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private static final AppReview instance = new AppReview();

    private int minimumLaunches = 5;
    private int minimumDaysInstalled = 3;
    private int daysBetweenPrompts = 30;
    private int highRatingThreshold = 4;
    private String storeUrl;
    private String supportEmail;
    private FeedbackListener feedbackListener;

    AppReview() {
    }

    /// The shared `AppReview` instance.
    ///
    /// #### Returns
    ///
    /// the singleton used by the whole application.
    public static AppReview getInstance() {
        return instance;
    }

    /// The number of app launches that must accumulate before the scheduler in
    /// [#registerSession] will prompt for a review. Defaults to 5.
    ///
    /// #### Returns
    ///
    /// this instance for chaining.
    public AppReview setMinimumLaunches(int minimumLaunches) {
        this.minimumLaunches = minimumLaunches;
        return this;
    }

    /// The number of days that must elapse after the first recorded launch
    /// before the scheduler will prompt for a review. Defaults to 3.
    ///
    /// #### Returns
    ///
    /// this instance for chaining.
    public AppReview setMinimumDaysInstalled(int minimumDaysInstalled) {
        this.minimumDaysInstalled = minimumDaysInstalled;
        return this;
    }

    /// The minimum number of days between two consecutive review prompts shown
    /// by the scheduler. Defaults to 30.
    ///
    /// #### Returns
    ///
    /// this instance for chaining.
    public AppReview setDaysBetweenPrompts(int daysBetweenPrompts) {
        this.daysBetweenPrompts = daysBetweenPrompts;
        return this;
    }

    /// The lowest star value (1-5) that is still considered a positive rating
    /// in the fallback widget. Ratings at or above this value send the user to
    /// the store, lower ratings open the private feedback flow. Defaults to 4.
    ///
    /// #### Returns
    ///
    /// this instance for chaining.
    public AppReview setHighRatingThreshold(int highRatingThreshold) {
        this.highRatingThreshold = highRatingThreshold;
        return this;
    }

    /// The store URL opened by the fallback widget for a positive rating (and
    /// used when no native prompt is available). On iOS/Android with a native
    /// prompt this is not needed. Typically your App Store or Google Play
    /// listing URL.
    ///
    /// #### Returns
    ///
    /// this instance for chaining.
    public AppReview setStoreUrl(String storeUrl) {
        this.storeUrl = storeUrl;
        return this;
    }

    /// The support e-mail address used by the fallback widget to collect
    /// feedback for low ratings when no [FeedbackListener] handled it. When
    /// null and no listener is set, the feedback step is skipped.
    ///
    /// #### Returns
    ///
    /// this instance for chaining.
    public AppReview setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
        return this;
    }

    /// Registers a listener that intercepts the outcome of the fallback rating
    /// widget so feedback can be delivered through your own channel.
    ///
    /// #### Returns
    ///
    /// this instance for chaining.
    public AppReview setFeedbackListener(FeedbackListener feedbackListener) {
        this.feedbackListener = feedbackListener;
        return this;
    }

    int getHighRatingThreshold() {
        return highRatingThreshold;
    }

    String getStoreUrl() {
        return storeUrl;
    }

    String getSupportEmail() {
        return supportEmail;
    }

    FeedbackListener getFeedbackListener() {
        return feedbackListener;
    }

    /// Records the current app session and, when the configured engagement
    /// thresholds are satisfied and the user has not already rated or opted
    /// out, prompts for a review. Call this once per app start (e.g. from the
    /// `start` lifecycle method). It is cheap and safe to call every launch.
    public void registerSession() {
        int launches = Preferences.get(PREF_LAUNCHES, 0) + 1;
        Preferences.set(PREF_LAUNCHES, launches);
        if (Preferences.get(PREF_FIRST_INSTALL, 0L) == 0L) {
            Preferences.set(PREF_FIRST_INSTALL, System.currentTimeMillis());
        }
        if (shouldPrompt()) {
            requestReview();
        }
    }

    /// Whether [#registerSession] would prompt for a review given the current
    /// persisted state and configuration. Exposed mainly for testing and for
    /// apps that want to drive the prompt from their own trigger.
    ///
    /// #### Returns
    ///
    /// true if a prompt is currently due.
    public boolean shouldPrompt() {
        if (Preferences.get(PREF_COMPLETED, false)) {
            return false;
        }
        if (Preferences.get(PREF_LAUNCHES, 0) < minimumLaunches) {
            return false;
        }
        long now = System.currentTimeMillis();
        long firstInstall = Preferences.get(PREF_FIRST_INSTALL, now);
        if (now - firstInstall < ((long) minimumDaysInstalled) * DAY_MILLIS) {
            return false;
        }
        long lastPrompt = Preferences.get(PREF_LAST_PROMPT, 0L);
        return lastPrompt <= 0L || now - lastPrompt >= ((long) daysBetweenPrompts) * DAY_MILLIS;
    }

    /// Immediately asks the user for a review. Uses the native store review
    /// prompt when available, otherwise shows the Codename One rating widget.
    /// Unlike [#registerSession] this ignores the scheduling thresholds, but it
    /// still respects the "already completed" opt out and records the prompt
    /// time so the scheduler will not pile on.
    public void requestReview() {
        if (Preferences.get(PREF_COMPLETED, false)) {
            // The user already rated or opted out -- honour that even for a
            // manual request, as documented above.
            return;
        }
        Preferences.set(PREF_LAST_PROMPT, System.currentTimeMillis());
        if (CN.isEdt()) {
            requestReviewImpl();
        } else {
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    requestReviewImpl();
                }
            });
        }
    }

    private void requestReviewImpl() {
        if (CN.isNativeInAppReviewSupported()) {
            CN.requestNativeInAppReview(new SuccessCallback<Boolean>() {
                @Override
                public void onSucess(Boolean handled) {
                    if (handled != null && handled.booleanValue()) {
                        // The OS now owns the rate-limiting / cadence of review
                        // prompts, so we stop driving our own scheduler.
                        markCompleted();
                    } else {
                        RatingDialog.show(AppReview.this);
                    }
                }
            });
        } else {
            RatingDialog.show(this);
        }
    }

    /// Permanently stops the scheduler from prompting again (the user rated the
    /// app or chose "don't ask again"). The fallback widget calls this for you;
    /// it is exposed for apps that gather a rating through their own UI.
    public void markCompleted() {
        Preferences.set(PREF_COMPLETED, true);
    }

    /// Clears all persisted engagement state (launch count, install date, last
    /// prompt time and the completed flag) so the engagement cycle starts over.
    /// Mostly useful for testing.
    public void reset() {
        Preferences.delete(PREF_LAUNCHES);
        Preferences.delete(PREF_FIRST_INSTALL);
        Preferences.delete(PREF_LAST_PROMPT);
        Preferences.delete(PREF_COMPLETED);
    }
}
