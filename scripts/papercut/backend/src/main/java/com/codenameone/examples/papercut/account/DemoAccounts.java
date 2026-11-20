/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.papercut.Clock;
import com.codenameone.examples.papercut.api.RunDto;
import com.codenameone.examples.papercut.friends.Friends;
import com.codenameone.examples.papercut.run.Modes;
import com.codenameone.examples.papercut.run.Runs;
import java.io.IOException;

/// The accounts a development server starts with: a player and a rival to
/// compare scores, a player with a flagged run for the console to find, an
/// admin, and a field of rivals with runs spread over all three modes, this
/// week and today, so every board and the daily sheet have something on them.
/// Anywhere else only the admin is made, and only when `papercut.admin.email`
/// and `papercut.admin.password` say who.
@Component
public class DemoAccounts {
    public static final String PLAYER = "player@papercut.example";
    public static final String RIVAL = "rival@papercut.example";
    public static final String TROUBLE = "trouble@papercut.example";
    public static final String ADMIN = "admin@papercut.example";
    public static final String PASSWORD = "papercut-demo";

    private static final String[] FIELD = {
        "Snips", "Dog Ear", "Crease", "Paper Tiger", "Staple", "Quire", "Foolscap", "Vellum",
        "Deckle", "Ream Queen", "Tissue", "Cardstock", "Onion Skin", "Letterhead", "Watermark"
    };

    private final Accounts accounts;
    private final Runs runs;
    private final Friends friends;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Runs runs, Friends friends, Config config,
            @Value("${papercut.admin.email:}") String adminEmail,
            @Value("${papercut.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.runs = runs;
        this.friends = friends;
        this.config = config;
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
        if (accounts.exists(PLAYER)) {
            // Seeded on an earlier start against a database that kept it.
            return;
        }
        long now = Clock.now();
        accounts.create(PLAYER, PASSWORD, "Riley", false);
        accounts.create(RIVAL, PASSWORD, "Morgan", false);
        accounts.create(ADMIN, PASSWORD, "Editor in Chief", true);
        accounts.create(TROUBLE, PASSWORD, "Too Sharp", false);
        run(PLAYER, Modes.CLASSIC, 182000, 240, now - 3 * Clock.DAY);
        run(PLAYER, Modes.FOLD, 96000, 150, now - Clock.DAY);
        run(RIVAL, Modes.CLASSIC, 214000, 260, now - 2 * 60 * 60 * 1000L);
        run(RIVAL, Modes.DAILY, 121000, 190, now - 60 * 60 * 1000L);
        // Ten thousand points a second: possible, so kept, but past the
        // suspicious line, so it waits in the console's flag queue.
        runs.accept(TROUBLE, Modes.CLASSIC, new RunDto(null, Modes.CLASSIC, 600000, 200, 0, 60, 20),
                now - 30 * 60 * 1000L);
        long state = 20261120L;
        for (int i = 0; i < FIELD.length; i++) {
            String who = "rival" + (i + 1) + "@papercut.example";
            accounts.create(who, PASSWORD, FIELD[i], false);
            String[] modes = {Modes.CLASSIC, Modes.FOLD, Modes.DAILY};
            for (String mode : modes) {
                // An old run, one this week and, for most, one today.
                long[] ages = {20 * Clock.DAY, 2 * Clock.DAY, 60 * 60 * 1000L};
                for (int a = 0; a < ages.length; a++) {
                    state = state * 6364136223846793005L + 1442695040888963407L;
                    if (a == 2 && ((state >>> 33) % 4) == 0) {
                        continue;
                    }
                    int seconds = 90 + (int) ((state >>> 20) % 420);
                    long score = seconds * (400L + (state >>> 40) % 1800);
                    run(who, mode, score, seconds, now - ages[a] - (state >>> 45) % 3600000L);
                }
            }
        }
        friends.add(PLAYER, RIVAL);
        friends.add(PLAYER, "rival1@papercut.example");
        friends.add(PLAYER, "rival2@papercut.example");
    }

    /// A believable run: enough cuts for the score, well inside `ScoreBounds`.
    private void run(String who, String mode, long score, int seconds, long when) throws IOException {
        int kills = (int) (score / 2000) + 10;
        int gates = Modes.FOLD.equals(mode) ? Math.max(1, seconds / 12) : 0;
        int multiplier = (int) Math.min(20, 2 + score / 25000);
        runs.accept(who, mode, new RunDto(null, mode, score, kills, gates, seconds, multiplier), when);
    }
}
