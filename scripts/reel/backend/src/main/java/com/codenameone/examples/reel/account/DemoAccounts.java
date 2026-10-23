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
package com.codenameone.examples.reel.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.reel.Days;
import com.codenameone.examples.reel.Roles;
import com.codenameone.examples.reel.domain.Profile;
import com.codenameone.examples.reel.repository.ProfileRepository;

import java.io.IOException;

/// The accounts a development server starts with, one per role, all with the
/// password [#PASSWORD]. A server on any other profile makes only the first
/// admin, from `reel.admin.email` and `reel.admin.password`, and nobody else.
///
/// [com.codenameone.examples.reel.DemoData] calls [#seed()] before it writes
/// the catalog, because the uploaded titles belong to [#CREATOR].
@Component
public class DemoAccounts {
    public static final String VIEWER = "viewer@reel.example";
    public static final String SECOND_VIEWER = "viewer2@reel.example";
    /// A viewer with an open sharing alert and a flag, for the admin console.
    public static final String FLAGGED_VIEWER = "viewer3@reel.example";
    public static final String CREATOR = "creator@reel.example";
    public static final String MODERATOR = "moderator@reel.example";
    public static final String ADMIN = "admin@reel.example";
    public static final String PASSWORD = "reel-demo";

    private final Accounts accounts;
    private final Moderation moderation;
    private final ProfileRepository profiles;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Moderation moderation, ProfileRepository profiles, Config config,
            @Value("${reel.admin.email:}") String adminEmail,
            @Value("${reel.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.moderation = moderation;
        this.profiles = profiles;
        this.config = config;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /// Makes the accounts that do not exist yet. Returns whether this start
    /// made the development set, so the caller seeds what goes with it once.
    public boolean seed() throws IOException {
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8) {
                add(Accounts.email(adminEmail), "Administrator", 0, Roles.VIEWER, Roles.ADMIN);
            }
            return false;
        }
        boolean first = !accounts.exists(ADMIN);
        add(VIEWER, "Riley Viewer", 0, Roles.VIEWER);
        add(SECOND_VIEWER, "Robin Viewer", 40, Roles.VIEWER);
        add(CREATOR, "Casey Creator", 120, Roles.VIEWER, Roles.CREATOR);
        add(MODERATOR, "Morgan Moderator", 200, Roles.VIEWER, Roles.MODERATOR);
        add(ADMIN, "Alex Admin", 365, Roles.VIEWER, Roles.ADMIN);
        boolean flag = !accounts.exists(FLAGGED_VIEWER);
        add(FLAGGED_VIEWER, "Rae Viewer", 9, Roles.VIEWER);
        if (flag) {
            flag(FLAGGED_VIEWER, "Streams from five cities in one evening", 1);
        }
        return first;
    }

    @Transactional(rollbackFor = IOException.class)
    void flag(String username, String why, int daysAgo) throws IOException {
        Profile profile = profiles.findById(username);
        profile.setFlagged(true);
        profile.setFlagReason(why);
        profiles.save(profile);
        moderation.record(username, ADMIN, Moderation.FLAG, why,
                System.currentTimeMillis() - daysAgo * Days.MILLIS);
    }

    private void add(String username, String name, int daysAgo, String... roles) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        String password = config.isDevelopmentProfile() ? PASSWORD : adminPassword;
        try {
            accounts.create(username, password, name, roles, System.currentTimeMillis() - daysAgo * Days.MILLIS);
        } catch (IOException | RuntimeException failed) {
            // Two servers started together against a new database both find
            // no admin and both make one; the second insert fails on the name.
            // create() is one transaction, so the loser left nothing behind.
            if (!accounts.exists(username)) {
                throw failed;
            }
        }
    }
}
