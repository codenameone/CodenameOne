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
package com.codenameone.examples.brickyard.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.brickyard.api.AccountApi;
import com.codenameone.examples.brickyard.api.BadgeApi;
import com.codenameone.examples.brickyard.api.DataStoreApi;
import com.codenameone.examples.brickyard.api.ExperienceApi;
import com.codenameone.examples.brickyard.api.LeaderboardApi;
import com.codenameone.examples.brickyard.api.EconomyApi;
import com.codenameone.examples.brickyard.api.ProgressionApi;
import com.codenameone.examples.brickyard.api.SocialApi;
import com.codenameone.examples.brickyard.api.ModerationApi;
import com.codenameone.examples.brickyard.api.AnalyticsApi;

/// The generated REST clients, one per contract in the shared module, all
/// pointed at the server [Session] is using.
public final class Api {
    private static AccountApi account;
    private static ExperienceApi experiences;
    private static BadgeApi badges;
    private static LeaderboardApi leaderboards;
    private static DataStoreApi data;
    private static EconomyApi economy;
    private static ProgressionApi progression;
    private static SocialApi social;
    private static ModerationApi moderation;
    private static AnalyticsApi analytics;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        experiences = RestClients.create(ExperienceApi.class, baseUrl);
        badges = RestClients.create(BadgeApi.class, baseUrl);
        leaderboards = RestClients.create(LeaderboardApi.class, baseUrl);
        data = RestClients.create(DataStoreApi.class, baseUrl);
        economy = RestClients.create(EconomyApi.class, baseUrl);
        progression = RestClients.create(ProgressionApi.class, baseUrl);
        social = RestClients.create(SocialApi.class, baseUrl);
        moderation = RestClients.create(ModerationApi.class, baseUrl);
        analytics = RestClients.create(AnalyticsApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static ExperienceApi experiences() {
        return experiences;
    }

    public static BadgeApi badges() {
        return badges;
    }

    public static LeaderboardApi leaderboards() {
        return leaderboards;
    }

    public static DataStoreApi data() {
        return data;
    }

    public static EconomyApi economy() {
        return economy;
    }

    public static ProgressionApi progression() {
        return progression;
    }

    public static SocialApi social() {
        return social;
    }

    public static ModerationApi moderation() {
        return moderation;
    }

    public static AnalyticsApi analytics() {
        return analytics;
    }
}
