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
package com.codenameone.examples.fieldline.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.fieldline.field.DemoField;

import java.io.IOException;

/// The accounts a server starts with.
///
/// On a development profile: a dispatcher, two technicians who can sign in,
/// an admin, and -- through [DemoField] -- the rest of a twelve-person crew,
/// their customers' sites and a working week of jobs, planned.
///
/// Anywhere else, at most one: the admin named by `fieldline.admin.email` and
/// `fieldline.admin.password` (or the same two names in the environment, in
/// upper case with `_` for each `.`), because a new deployment needs somebody
/// able to grant the first roles. It is created once; changing the setting
/// later does not change the password of an account that exists.
@Component
public class DemoAccounts {
    public static final String DISPATCHER = "dispatch@fieldline.example";
    public static final String TECHNICIAN = "tech@fieldline.example";
    public static final String SECOND_TECHNICIAN = "tech2@fieldline.example";
    public static final String ADMIN = "admin@fieldline.example";
    /// The password of every demo account. Not a secret: these accounts exist
    /// only where the profile says the server is not a real one.
    public static final String PASSWORD = "fieldline-demo";

    private final Accounts accounts;
    private final DemoField field;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, DemoField field, Config config,
            @Value("${fieldline.admin.email:}") String adminEmail,
            @Value("${fieldline.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.field = field;
        this.config = config;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /// Runs once, when the server starts. Each account is made by a
    /// transactional method, so a server stopped half way through start-up
    /// is left with whole accounts and the next start adds the missing ones.
    @PostConstruct
    public void seed() throws IOException {
        field.manuals();
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8
                    && !accounts.exists(Accounts.email(adminEmail))) {
                accounts.create(Accounts.email(adminEmail), adminPassword, "Administrator", "",
                        false, true, true, System.currentTimeMillis());
            }
            return;
        }
        boolean first = !accounts.exists(DISPATCHER);
        long now = System.currentTimeMillis();
        if (first) {
            accounts.create(DISPATCHER, PASSWORD, "Morgan Reyes", "+15550200", false, true, false,
                    now);
        }
        if (!accounts.exists(ADMIN)) {
            accounts.create(ADMIN, PASSWORD, "Alex Admin", "+15550203", false, true, true, now);
        }
        if (!accounts.exists(TECHNICIAN)) {
            accounts.create(TECHNICIAN, PASSWORD, "Dana Okafor", "+15550201", true, false, false,
                    now);
        }
        if (!accounts.exists(SECOND_TECHNICIAN)) {
            accounts.create(SECOND_TECHNICIAN, PASSWORD, "Sam Whitaker", "+15550202", true, false,
                    false, now);
        }
        if (first) {
            field.seed(TECHNICIAN, SECOND_TECHNICIAN);
        }
    }
}
