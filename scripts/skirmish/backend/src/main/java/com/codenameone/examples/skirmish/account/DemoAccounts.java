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
package com.codenameone.examples.skirmish.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.skirmish.admin.Flags;
import com.codenameone.examples.skirmish.progress.Progression;
import java.io.IOException;

/// The accounts a development server starts with: two players to challenge
/// each other, one flagged player for the console to find, and an admin.
/// Anywhere else only the admin is made, and only when
/// `skirmish.admin.email` and `skirmish.admin.password` say who.
@Component
public class DemoAccounts {
    public static final String PLAYER = "player@skirmish.example";
    public static final String RIVAL = "rival@skirmish.example";
    public static final String TROUBLE = "trouble@skirmish.example";
    public static final String ADMIN = "admin@skirmish.example";
    public static final String PASSWORD = "skirmish-demo";

    private final Accounts accounts;
    private final Flags flags;
    private final Progression progression;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Flags flags, Progression progression, Config config,
            @Value("${skirmish.admin.email:}") String adminEmail,
            @Value("${skirmish.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.flags = flags;
        this.progression = progression;
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
        add(PLAYER, "Riley", false);
        add(RIVAL, "Morgan", false);
        add(ADMIN, "Game Master", true);
        if (!accounts.exists(TROUBLE)) {
            add(TROUBLE, "Sore Loser", false);
            flags.auto(TROUBLE, "Left three matches in a row", null);
        }
    }

    private void add(String username, String name, boolean admin) throws IOException {
        if (!accounts.exists(username)) {
            accounts.create(username, PASSWORD, name, admin);
            progression.seedDemo(username);
        }
    }
}
