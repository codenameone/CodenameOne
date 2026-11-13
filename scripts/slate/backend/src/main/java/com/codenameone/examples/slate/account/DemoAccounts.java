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
package com.codenameone.examples.slate.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.slate.Days;
import com.codenameone.examples.slate.api.CardDto;
import com.codenameone.examples.slate.api.PaymentSetupDto;
import com.codenameone.examples.slate.domain.Profile;
import com.codenameone.examples.slate.pay.Payments;
import com.codenameone.examples.slate.repository.ProfileRepository;

import java.io.IOException;
import java.time.Instant;

/// The accounts a server starts with.
///
/// On a development profile, one of each kind, so that every screen of the
/// app has someone to show it: clients (one with a card, one new, one the
/// owners flagged), three providers and the owner. The business itself --
/// its services, hours and a few weeks of bookings -- is
/// `booking/DemoBusiness`'s, which calls this once the business row exists,
/// since a provider's row takes its title from the business's vertical.
///
/// Anywhere else, at most one: the owner named by `slate.admin.email` and
/// `slate.admin.password` (or the same two names in the environment, in upper
/// case with `_` for each `.`), because a new deployment needs somebody able
/// to give the first roles. It is created once; changing the setting later
/// does not change the password of an account that exists.
@Component
public class DemoAccounts {
    /// A client with a card on file and a history.
    public static final String CLIENT = "client@slate.example";
    /// A client with no card and no bookings, as a new sign-up is.
    public static final String SECOND_CLIENT = "client2@slate.example";
    /// A client the owners have flagged.
    public static final String FLAGGED_CLIENT = "client3@slate.example";
    /// The provider most of the demo's bookings are with.
    public static final String PROVIDER = "provider@slate.example";
    public static final String SECOND_PROVIDER = "provider2@slate.example";
    /// Works at the second location too, for the travel-time rule.
    public static final String THIRD_PROVIDER = "provider3@slate.example";
    public static final String OWNER = "owner@slate.example";
    /// The password of every demo account. Not a secret: these accounts exist
    /// only where the profile says the server is not a real one.
    public static final String PASSWORD = "slate-demo";

    private final Accounts accounts;
    private final Moderation moderation;
    private final Payments payments;
    private final ProfileRepository profiles;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Moderation moderation, Payments payments,
            ProfileRepository profiles, Config config,
            @Value("${slate.admin.email:}") String adminEmail,
            @Value("${slate.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.moderation = moderation;
        this.payments = payments;
        this.profiles = profiles;
        this.config = config;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /// Runs when the server starts, called by `booking/DemoBusiness`. Not a
    /// transaction itself: each account is made by a transactional method, so
    /// a server stopped half way through start-up is left with whole
    /// accounts, and the next start adds the ones that are missing.
    public void seed() throws IOException {
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8) {
                add(Accounts.email(adminEmail), adminPassword, "Owner", "+10000000", false,
                        true, 0);
            }
            return;
        }
        // The tests sign in as the first client and expect to find them as a
        // new account is: no card saved. Under the `test` profile only the
        // others get one.
        boolean card = !"test".equals(config.getProfile());
        if (add(CLIENT, PASSWORD, "Riley Park", "+15550100", false, false, 120) && card) {
            card(CLIENT, "Riley Park");
        }
        add(SECOND_CLIENT, PASSWORD, "Robin Chen", "+15550101", false, false, 3);
        if (add(FLAGGED_CLIENT, PASSWORD, "Rae Okafor", "+15550102", false, false, 60)) {
            card(FLAGGED_CLIENT, "Rae Okafor");
            flag(FLAGGED_CLIENT, "Three late cancellations in two weeks", 4);
        }
        add(PROVIDER, PASSWORD, "Tess Alvarez", "+15550110", true, false, 400);
        add(SECOND_PROVIDER, PASSWORD, "Theo Brooks", "+15550111", true, false, 300);
        add(THIRD_PROVIDER, PASSWORD, "Maya Lindqvist", "+15550114", true, false, 380);
        add(OWNER, PASSWORD, "Olive Hart", "+15550113", false, true, 500);
    }

    /// Marks an account as one the owners flagged some days ago, with the
    /// event that says who did and why.
    @Transactional(rollbackFor = IOException.class)
    void flag(String username, String why, int daysAgo) throws IOException {
        Profile profile = profiles.findById(username);
        profile.setFlagged(true);
        profile.setFlagReason(why);
        moderation.record(username, OWNER, Moderation.FLAG, why,
                Instant.now().minusMillis(daysAgo * Days.MILLIS));
    }

    /// Makes an account unless it exists. True when this call made it.
    private boolean add(String username, String password, String name, String phone,
            boolean provider, boolean owner, int daysAgo) throws IOException {
        if (accounts.exists(username)) {
            return false;
        }
        try {
            accounts.create(username, password, name, phone, true, provider, owner,
                    System.currentTimeMillis() - daysAgo * Days.MILLIS);
            return true;
        } catch (IOException | RuntimeException failed) {
            // Two servers started together against a new database both find
            // no owner and both make one; the second insert fails on the name.
            // create() is one transaction, so the loser left nothing behind,
            // and the account the other server made is the one wanted. Any
            // other failure leaves no account, and is the start-up's to report.
            if (!accounts.exists(username)) {
                throw failed;
            }
            return false;
        }
    }

    /// Saves a test card. Only a simulated card can be made up: with a real
    /// processor configured the demo clients pay on the day until they add a
    /// card.
    private void card(String username, String name) throws IOException {
        PaymentSetupDto setup = payments.startSetup(username, name);
        if (!setup.hosted()) {
            int year = Integer.parseInt(Days.iso(System.currentTimeMillis()).substring(0, 4));
            payments.completeSetup(username, setup.id(),
                    new CardDto("4242424242424242", 12, year + 3, "123", name));
        }
    }
}
