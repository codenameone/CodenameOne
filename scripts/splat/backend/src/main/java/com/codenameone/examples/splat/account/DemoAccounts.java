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
package com.codenameone.examples.splat.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.splat.admin.Flags;
import com.codenameone.examples.splat.api.ConditionDto;
import com.codenameone.examples.splat.api.PipelineDto;
import com.codenameone.examples.splat.api.SpotlightDto;
import com.codenameone.examples.splat.progress.Progression;
import com.codenameone.examples.splat.social.Pipelines;
import com.codenameone.examples.splat.social.Promotion;
import java.io.IOException;
import java.util.Arrays;

/// The accounts a development server starts with: two players to challenge
/// each other, one flagged player for the console to find, and an admin.
/// Anywhere else only the admin is made, and only when
/// `splat.admin.email` and `splat.admin.password` say who.
@Component
public class DemoAccounts {
    public static final String PLAYER = "player@splat.example";
    public static final String RIVAL = "rival@splat.example";
    public static final String TROUBLE = "trouble@splat.example";
    public static final String ADMIN = "admin@splat.example";
    public static final String PASSWORD = "splat-demo";

    private final Accounts accounts;
    private final Flags flags;
    private final Progression progression;
    private final Config config;
    private final Promotion promotion;
    private final Pipelines pipelines;
    private final DemoHistory history;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Flags flags, Progression progression, Config config,
            Promotion promotion, Pipelines pipelines, DemoHistory history,
            @Value("${splat.admin.email:}") String adminEmail,
            @Value("${splat.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.flags = flags;
        this.progression = progression;
        this.config = config;
        this.promotion = promotion;
        this.pipelines = pipelines;
        this.history = history;
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
        field();
        if (!accounts.exists(TROUBLE)) {
            add(TROUBLE, "Sore Loser", false);
            flags.auto(TROUBLE, "Wiped paint off three times in one match", null);
        }
        // A banner on the home screen from the start, so the spotlight the
        // console's Promote tab sets is there to be seen and changed.
        String banner = promotion.spotlight().text();
        if (banner == null || banner.length() == 0) {
            promotion.spotlight(ADMIN, new SpotlightDto("New field: Cul-de-Sac Siege. The HOA has approved paint.",
                    "", "play", 0L));
        }
        // Two pipelines to start from: one brings lapsed players back, the
        // other (off until someone turns it on) rewards a keen player who has
        // never bought anything.
        if (pipelines.list().isEmpty()) {
            pipelines.save(ADMIN, new PipelineDto(null, "Win back", true,
                    Arrays.asList(new ConditionDto("daysAway", ">=", 7), new ConditionDto("races", ">=", 1)),
                    Pipelines.PUSH, "Your marker misses you", "The other team says you are scared. Prove them wrong.",
                    "play", 0, 14, 0, 0L));
            pipelines.save(ADMIN, new PipelineDto(null, "Keen, never paid", false,
                    Arrays.asList(new ConditionDto("races", ">=", 10), new ConditionDto("purchases", "=", 0)),
                    Pipelines.GEMS, "A gift for you", "20 gems for fresh camo. Thanks for playing!",
                    "shop", 20, 30, 0, 0L));
        }
    }

    /// The field a development server's leaderboard starts with: named bots
    /// of fixed skill whose month of matches [DemoHistory] plays for real.
    static final String[][] FIELD = {
        {"nova@splat.example", "Nova", "95"},
        {"kai@splat.example", "Kai", "80"},
        {"juno@splat.example", "Juno", "70"},
        {"ezra@splat.example", "Ezra", "60"},
        {"sasha@splat.example", "Sasha", "50"},
        {"dev@splat.example", "Dev", "40"},
        {"ivy@splat.example", "Ivy", "30"},
        {"theo@splat.example", "Theo", "20"},
    };

    private void field() throws IOException {
        for (int i = 0; i < FIELD.length; i++) {
            add(FIELD[i][0], FIELD[i][1], false);
        }
        history.seed(FIELD);
    }

    private void add(String username, String name, boolean admin) throws IOException {
        if (!accounts.exists(username)) {
            accounts.create(username, PASSWORD, name, admin);
            progression.seedDemo(username);
        }
    }
}
