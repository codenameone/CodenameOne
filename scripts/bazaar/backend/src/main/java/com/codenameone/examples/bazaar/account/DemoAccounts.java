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
package com.codenameone.examples.bazaar.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bazaar.Days;
import com.codenameone.examples.bazaar.api.CardDto;
import com.codenameone.examples.bazaar.api.PaymentSetupDto;
import com.codenameone.examples.bazaar.domain.Profile;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.repository.ProfileRepository;
import com.codenameone.examples.bazaar.store.DemoStores;

import java.io.IOException;
import java.time.Instant;

/// The accounts a server starts with.
///
/// On a development profile, one of each kind so that every mode of the app
/// can be signed in to straight after a checkout: shoppers, a store owner, a
/// cashier, an admin, and an account the admins have flagged. Which store the
/// owner and the cashier work at is the store's staff list, not theirs.
///
/// Anywhere else, at most one: the admin named by `bazaar.admin.email` and
/// `bazaar.admin.password` (or the same two names in the environment, in upper
/// case with `_` for each `.`), because a new deployment needs somebody able to
/// grant the first roles. It is created once; changing the setting later does
/// not change the password of an account that exists.
@Component
public class DemoAccounts {
    public static final String CUSTOMER = "shopper@bazaar.example";
    public static final String ADMIN = "admin@bazaar.example";
    /// Owns the first demo store.
    public static final String MERCHANT = "owner@bazaar.example";
    /// Works the first demo store's register.
    public static final String CASHIER = "cashier@bazaar.example";
    public static final String SECOND_CUSTOMER = "shopper2@bazaar.example";
    /// A shopper the admins have flagged.
    public static final String FLAGGED_CUSTOMER = "shopper3@bazaar.example";
    /// The password of every demo account. Not a secret: these accounts exist
    /// only where the profile says the server is not a real one.
    public static final String PASSWORD = "bazaar-demo";

    private final Accounts accounts;
    private final Moderation moderation;
    private final Payments payments;
    private final ProfileRepository profiles;
    private final Config config;
    private final DemoStores demoStores;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Moderation moderation, Payments payments,
            ProfileRepository profiles, Config config, DemoStores demoStores,
            @Value("${bazaar.admin.email:}") String adminEmail,
            @Value("${bazaar.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.moderation = moderation;
        this.payments = payments;
        this.profiles = profiles;
        this.config = config;
        this.demoStores = demoStores;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /// Runs once, when the server starts. Not a transaction itself: each
    /// account is made by a transactional method -- [Accounts]' or one of this
    /// class's own -- so a server stopped half way through start-up is left
    /// with whole accounts, and the next start adds the ones that are missing.
    @PostConstruct
    public void seed() throws IOException {
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8) {
                add(Accounts.email(adminEmail), adminPassword, "Administrator", "+10000000",
                        false, true, 0);
            }
            return;
        }
        // The tests sign in as the first accounts and expect to find them as
        // new accounts are: no card saved, cash by default.
        boolean everyone = !"test".equals(config.getProfile());

        customer(CUSTOMER, "Riley Shopper", "+15550100", 0, everyone);
        add(ADMIN, PASSWORD, "Alex Admin", "+15550103", false, true, 0);
        add(MERCHANT, PASSWORD, "Morgan Owner", "+15550109", true, false, 20);
        add(CASHIER, PASSWORD, "Casey Cashier", "+15550104", true, false, 12);
        customer(SECOND_CUSTOMER, "Robin Shopper", "+15550106", 11, true);
        boolean flag = !accounts.exists(FLAGGED_CUSTOMER);
        customer(FLAGGED_CUSTOMER, "Rae Shopper", "+15550107", 6, false);
        if (flag) {
            flag(FLAGGED_CUSTOMER, "Three chargebacks in one week", 3);
        }
        // The stores the owner and the cashier work at, with their catalogs.
        demoStores.seed(MERCHANT, CASHIER);
    }

    /// Marks an account as one the admins flagged some days ago, with the
    /// event that says who did and why.
    @Transactional(rollbackFor = IOException.class)
    void flag(String username, String why, int daysAgo) throws IOException {
        Profile profile = profiles.findById(username);
        profile.setFlagged(true);
        profile.setFlagReason(why);
        moderation.record(username, ADMIN, Moderation.FLAG, why,
                Instant.now().minusMillis(daysAgo * Days.MILLIS));
    }

    /// An admin, a member of a store's staff, or the plainest kind of account.
    private void add(String username, String password, String name, String phone,
            boolean merchant, boolean admin, int daysAgo) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        try {
            accounts.create(username, password, name, phone, true, merchant, admin, opened(daysAgo));
        } catch (IOException | RuntimeException failed) {
            // Two servers started together against a new database both find
            // no admin and both make one; the second insert fails on the name.
            // create() is one transaction, so the loser left nothing behind,
            // and the account the other server made is the one wanted. Any
            // other failure leaves no account, and is the start-up's to report.
            if (!accounts.exists(username)) {
                throw failed;
            }
        }
    }

    private void customer(String username, String name, String phone, int daysAgo,
            boolean card) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        accounts.create(username, PASSWORD, name, phone, true, false, false, opened(daysAgo));
        if (card) {
            PaymentSetupDto setup = payments.startSetup(username, name);
            // Only a simulated card can be made up. With a real processor
            // configured the demo customers pay cash until someone adds a card.
            if (!setup.hosted()) {
                CardDto visa = new CardDto("4242424242424242", 12, year() + 3, "123", name);
                payments.completeSetup(username, setup.id(), visa);
            }
        }
    }

    /// When an account opened that many days ago was opened, so that the chart
    /// of new accounts is not one bar.
    private static long opened(int daysAgo) {
        return System.currentTimeMillis() - daysAgo * Days.MILLIS;
    }

    private static int year() {
        return Integer.parseInt(Days.iso(System.currentTimeMillis()).substring(0, 4));
    }
}
