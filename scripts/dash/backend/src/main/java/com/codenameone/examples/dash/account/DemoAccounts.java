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
package com.codenameone.examples.dash.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.dash.admin.Flags;
import com.codenameone.examples.dash.api.ConditionDto;
import com.codenameone.examples.dash.api.PipelineDto;
import com.codenameone.examples.dash.api.SpotlightDto;
import com.codenameone.examples.dash.progress.Progression;
import com.codenameone.examples.dash.social.Pipelines;
import com.codenameone.examples.dash.social.Promotion;
import java.io.IOException;
import java.util.Arrays;

/// The accounts a development server starts with: two players to challenge
/// each other, one flagged player for the console to find, and an admin.
/// Anywhere else only the admin is made, and only when
/// `dash.admin.email` and `dash.admin.password` say who.
@Component
public class DemoAccounts {
    public static final String PLAYER = "player@dash.example";
    public static final String RIVAL = "rival@dash.example";
    public static final String TROUBLE = "trouble@dash.example";
    public static final String ADMIN = "admin@dash.example";
    public static final String PASSWORD = "dash-demo";

    private final Accounts accounts;
    private final Flags flags;
    private final Progression progression;
    private final Config config;
    private final Promotion promotion;
    private final Pipelines pipelines;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Flags flags, Progression progression, Config config,
            Promotion promotion, Pipelines pipelines, @Value("${dash.admin.email:}") String adminEmail,
            @Value("${dash.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.flags = flags;
        this.progression = progression;
        this.config = config;
        this.promotion = promotion;
        this.pipelines = pipelines;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @PostConstruct
    public void seed() throws IOException {
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8
                    && !accounts.exists(Accounts.email(adminEmail))) {
                accounts.create(Accounts.email(adminEmail), adminPassword, "Administrator", true);
            }
            return;
        }
        add(PLAYER, "Riley", false);
        add(RIVAL, "Morgan", false);
        add(ADMIN, "Game Master", true);
        if (!accounts.exists(TROUBLE)) {
            add(TROUBLE, "Sore Loser", false);
            flags.auto(TROUBLE, "Left three races in a row", null);
        }
        // A banner on the home screen from the start, so the spotlight the
        // console's Promote tab sets is there to be seen and changed.
        String banner = promotion.spotlight().text();
        if (banner == null || banner.length() == 0) {
            promotion.spotlight(ADMIN, new SpotlightDto("New: build your own levels in the Forge and share them",
                    "", "forge", 0L));
        }
        // Two pipelines to start from: one brings lapsed players back, the
        // other (off until someone turns it on) rewards a keen player who has
        // never bought anything.
        if (pipelines.list().isEmpty()) {
            pipelines.save(ADMIN, new PipelineDto(null, "Win back", true,
                    Arrays.asList(new ConditionDto("daysAway", ">=", 7), new ConditionDto("races", ">=", 1)),
                    Pipelines.PUSH, "Your runner misses you", "A new track is live. Come back for a race!",
                    "race", 0, 14, 0, 0L));
            pipelines.save(ADMIN, new PipelineDto(null, "Keen, never paid", false,
                    Arrays.asList(new ConditionDto("races", ">=", 10), new ConditionDto("purchases", "=", 0)),
                    Pipelines.GEMS, "A gift for you", "20 gems for your next crate. Thanks for playing!",
                    "shop", 20, 30, 0, 0L));
        }
    }

    private void add(String username, String name, boolean admin) throws IOException {
        if (!accounts.exists(username)) {
            accounts.create(username, PASSWORD, name, admin);
            progression.seedDemo(username);
        }
    }
}
