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
package com.codenameone.examples.parley.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.parley.Days;
import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.ConversationEditDto;
import com.codenameone.examples.parley.api.NewConversationDto;
import com.codenameone.examples.parley.api.ReportDto;
import com.codenameone.examples.parley.chat.Conversations;
import com.codenameone.examples.parley.contact.Contacts;
import com.codenameone.examples.parley.domain.Profile;
import com.codenameone.examples.parley.repository.ProfileRepository;

import java.io.IOException;
import java.util.Arrays;

/// The accounts a development server starts with, so that a fresh checkout
/// has people to talk to: a handful of users who share two groups and a
/// channel, an admin, an account the admins have flagged with a report
/// waiting about it, and the demo bot, which answers whoever writes to it.
///
/// There is no message history. Every message is encrypted by the devices
/// that take part in it, and these accounts have no devices until someone
/// signs in as them; a history written by the server would be one the
/// server could read, which is the thing this app exists not to be. The
/// groups start with the notices the server writes itself, and the bot
/// greets each new account, in plain text, saying that it is not encrypted.
///
/// On any profile but a development one only the configured admin is made,
/// from `parley.admin.email` and `parley.admin.password`.
@Component
public class DemoAccounts {
    public static final String ALICE = "alice@parley.example";
    public static final String BOB = "bob@parley.example";
    public static final String CAROL = "carol@parley.example";
    public static final String DAN = "dan@parley.example";
    public static final String ADMIN = "admin@parley.example";
    /// An account the admins flagged, with a report about it still open.
    public static final String FLAGGED = "mallory@parley.example";
    /// The bot, which is the server: see [Conversations#BOT].
    public static final String BOT = Conversations.BOT;
    /// The password of every demo account. Not a secret: these accounts exist
    /// only where the profile says the server is not a real one.
    public static final String PASSWORD = "parley-demo";

    private final Accounts accounts;
    private final Moderation moderation;
    private final Conversations conversations;
    private final Contacts contacts;
    private final ProfileRepository profiles;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Moderation moderation, Conversations conversations,
            Contacts contacts, ProfileRepository profiles, Config config,
            @Value("${parley.admin.email:}") String adminEmail,
            @Value("${parley.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.moderation = moderation;
        this.conversations = conversations;
        this.contacts = contacts;
        this.profiles = profiles;
        this.config = config;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /// Runs once, when the server starts. Not a transaction itself: each
    /// account is made by a transactional method, so a server stopped half
    /// way through start-up is left with whole accounts, and the next start
    /// adds the ones that are missing.
    @PostConstruct
    public void seed() throws IOException {
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8) {
                add(Accounts.email(adminEmail), adminPassword, "Administrator", "+10000000",
                        "", true);
            }
            return;
        }
        // The conversations are made once, with the accounts, and not again
        // when a server with a database that persists restarts. The marker is
        // the last account made before them.
        boolean first = !accounts.exists(FLAGGED);
        add(ALICE, PASSWORD, "Alice Avery", "+15550100", "Hiking whenever it is not raining",
                false);
        add(BOB, PASSWORD, "Bob Brennan", "+15550101", "At the climbing gym", false);
        add(CAROL, PASSWORD, "Carol Chen", "+15550102", "Available", false);
        add(DAN, PASSWORD, "Dan Dorsey", "+15550103", "Do not disturb", false);
        add(ADMIN, PASSWORD, "Alex Admin", "+15550104", "Running Parley", true);
        add(BOT, PASSWORD + "-bot-" + System.nanoTime(), "Parley Bot", "+15550199",
                "A demo bot. Not end-to-end encrypted.", false);
        add(FLAGGED, PASSWORD, "Mallory Mason", "+15550105", "", false);
        if (first) {
            conversations();
            flag(FLAGGED, "Sent the same link to every group", 2);
            contacts.report(CAROL, new ReportDto(FLAGGED, "", "Spam in the hiking group",
                    "\"Win a free phone: tap here\" -- sent three times in an hour"));
        }
    }

    private void conversations() throws IOException {
        // The groups say nothing of their own: what members write there is
        // theirs to write, and encrypted.
        ConversationDto hike = conversations.create(ALICE, new NewConversationDto(
                Conversations.GROUP, "Weekend hike", Arrays.asList(BOB, CAROL, DAN)));
        conversations.edit(ALICE, hike.id(), new ConversationEditDto("Weekend hike",
                "Saturday, 8am at the trailhead", 0, false));
        ConversationDto family = conversations.create(BOB, new NewConversationDto(
                Conversations.GROUP, "Brennan family", Arrays.asList(ALICE, CAROL)));
        conversations.edit(BOB, family.id(), new ConversationEditDto("Brennan family", "",
                86400, false));
        conversations.create(ADMIN, new NewConversationDto(Conversations.CHANNEL,
                "Parley news", Arrays.asList(ALICE, BOB, CAROL, DAN)));
        conversations.create(ALICE, new NewConversationDto(Conversations.DIRECT, "",
                Arrays.asList(BOT)));
    }

    /// Marks an account as one the admins flagged some days ago, with the
    /// event that says who did and why.
    @Transactional(rollbackFor = IOException.class)
    void flag(String username, String why, int daysAgo) throws IOException {
        Profile profile = profiles.findById(username);
        profile.setFlagged(true);
        profile.setFlagReason(why);
        moderation.record(username, ADMIN, Moderation.FLAG, why,
                System.currentTimeMillis() - daysAgo * Days.MILLIS);
    }

    private void add(String username, String password, String name, String phone, String about,
            boolean admin) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        try {
            accounts.create(username, password, name, phone, true, admin);
            if (about.length() > 0) {
                about(username, about);
            }
        } catch (IOException | RuntimeException failed) {
            // Two servers started together against a new database both find
            // no admin and both make one; the second insert fails on the name.
            // create() is one transaction, so the loser left nothing behind,
            // and the account the other server made is the one wanted.
            if (!accounts.exists(username)) {
                throw failed;
            }
        }
    }

    @Transactional(rollbackFor = IOException.class)
    void about(String username, String about) {
        Profile profile = profiles.findById(username);
        if (profile != null) {
            profile.setAbout(about);
        }
    }
}
