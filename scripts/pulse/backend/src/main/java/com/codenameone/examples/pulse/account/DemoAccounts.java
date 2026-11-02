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
package com.codenameone.examples.pulse.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.pulse.Days;
import com.codenameone.examples.pulse.api.CardDto;
import com.codenameone.examples.pulse.api.PaymentSetupDto;
import com.codenameone.examples.pulse.domain.Profile;
import com.codenameone.examples.pulse.pay.Payments;
import com.codenameone.examples.pulse.repository.ProfileRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

/// The accounts a server starts with.
///
/// On a development profile, one of each kind, so that every screen of the
/// app has someone to show it: members (one with children, one the owners
/// flagged), two trainers, the front desk and the owner. The studio itself --
/// locations, classes, plans and a few weeks of history -- is
/// `studio/DemoStudio`'s, which runs after this.
///
/// Anywhere else, at most one: the owner named by `pulse.admin.email` and
/// `pulse.admin.password` (or the same two names in the environment, in upper
/// case with `_` for each `.`), because a new deployment needs somebody able
/// to give the first roles. It is created once; changing the setting later
/// does not change the password of an account that exists.
@Component
public class DemoAccounts {
    /// A member with two children in their household and a card on file.
    public static final String MEMBER = "member@pulse.example";
    /// A member with no household and no card, as a new sign-up is.
    public static final String SECOND_MEMBER = "member2@pulse.example";
    /// A member the owners have flagged.
    public static final String FLAGGED_MEMBER = "member3@pulse.example";
    /// A member who has not been in for weeks, for the we-miss-you nudges.
    public static final String LAPSED_MEMBER = "member4@pulse.example";
    public static final String TRAINER = "trainer@pulse.example";
    public static final String SECOND_TRAINER = "trainer2@pulse.example";
    /// Pilates and barre at Downtown.
    public static final String PILATES_TRAINER = "trainer3@pulse.example";
    /// The ride and HIIT coach at Downtown.
    public static final String RIDE_TRAINER = "trainer4@pulse.example";
    /// Yoga and climbing at Riverside.
    public static final String RIVERSIDE_TRAINER = "trainer5@pulse.example";
    public static final String DESK = "desk@pulse.example";
    public static final String OWNER = "owner@pulse.example";
    /// The password of every demo account. Not a secret: these accounts exist
    /// only where the profile says the server is not a real one.
    public static final String PASSWORD = "pulse-demo";

    private final Accounts accounts;
    private final Moderation moderation;
    private final Payments payments;
    private final ProfileRepository profiles;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Moderation moderation, Payments payments,
            ProfileRepository profiles, Config config,
            @Value("${pulse.admin.email:}") String adminEmail,
            @Value("${pulse.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.moderation = moderation;
        this.payments = payments;
        this.profiles = profiles;
        this.config = config;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /// Runs when the server starts, called by `studio/DemoStudio` before it
    /// seeds the studio. Not a transaction itself: each
    /// account is made by a transactional method, so a server stopped half
    /// way through start-up is left with whole accounts, and the next start
    /// adds the ones that are missing.
    public void seed() throws IOException {
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8) {
                add(Accounts.email(adminEmail), adminPassword, "Owner", "+10000000",
                        LocalDate.of(1980, 1, 1), false, false, true, 0);
            }
            return;
        }
        // The tests sign in as the first member and expect to find them as a
        // new account is: no card saved. Under the `test` profile only the
        // others get one.
        boolean card = !"test".equals(config.getProfile());
        if (add(MEMBER, PASSWORD, "Riley Park", "+15550100", LocalDate.of(1987, 5, 14),
                false, false, false, 120) && card) {
            card(MEMBER, "Riley Park");
        }
        add(SECOND_MEMBER, PASSWORD, "Robin Chen", "+15550101", LocalDate.of(1995, 9, 2),
                false, false, false, 3);
        if (add(FLAGGED_MEMBER, PASSWORD, "Rae Okafor", "+15550102", LocalDate.of(1990, 2, 20),
                false, false, false, 60)) {
            flag(FLAGGED_MEMBER, "Three no-shows to full classes in one week", 4);
        }
        if (add(LAPSED_MEMBER, PASSWORD, "Lee Moreno", "+15550103", LocalDate.of(1979, 11, 8),
                false, false, false, 200)) {
            card(LAPSED_MEMBER, "Lee Moreno");
        }
        add(TRAINER, PASSWORD, "Tess Alvarez", "+15550110", LocalDate.of(1991, 3, 30), true,
                false, false, 400);
        add(SECOND_TRAINER, PASSWORD, "Theo Brooks", "+15550111", LocalDate.of(1985, 7, 19),
                true, false, false, 300);
        add(PILATES_TRAINER, PASSWORD, "Maya Lindqvist", "+15550114", LocalDate.of(1989, 6, 11),
                true, false, false, 380);
        add(RIDE_TRAINER, PASSWORD, "Andre Okoye", "+15550115", LocalDate.of(1993, 1, 25), true,
                false, false, 340);
        add(RIVERSIDE_TRAINER, PASSWORD, "Sofia Marin", "+15550116", LocalDate.of(1988, 10, 3),
                true, false, false, 270);
        add(DESK, PASSWORD, "Dee Patel", "+15550112", LocalDate.of(1998, 12, 1), false, true,
                false, 250);
        add(OWNER, PASSWORD, "Olive Hart", "+15550113", LocalDate.of(1976, 4, 4), false,
                false, true, 500);
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
            LocalDate born, boolean trainer, boolean desk, boolean owner, int daysAgo)
            throws IOException {
        if (accounts.exists(username)) {
            return false;
        }
        try {
            accounts.create(username, password, name, phone, true, born, trainer, desk, owner,
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
    /// processor configured the demo members pay at the desk until someone
    /// adds a card.
    private void card(String username, String name) throws IOException {
        PaymentSetupDto setup = payments.startSetup(username, name);
        if (!setup.hosted()) {
            int year = Integer.parseInt(Days.iso(System.currentTimeMillis()).substring(0, 4));
            payments.completeSetup(username, setup.id(),
                    new CardDto("4242424242424242", 12, year + 3, "123", name));
        }
    }
}
