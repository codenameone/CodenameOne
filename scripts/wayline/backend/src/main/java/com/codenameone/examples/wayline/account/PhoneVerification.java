/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.account;

import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.api.PhoneChallengeDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.domain.PhoneCode;
import com.codenameone.examples.wayline.sms.SmsSender;

import java.io.IOException;

/// Proves an account's phone number by texting it a code.
///
/// The server makes the code, keeps only its hash, and is the one that decides
/// whether what the user typed matches. The app never sees the code it is asking
/// the user for, so it cannot be talked into accepting one.
@Component
public class PhoneVerification {
    /// How long a code stays good.
    static final int CODE_SECONDS = 300;
    /// Wrong guesses allowed before the code is void. Six digits and five
    /// guesses is one chance in 200,000 per code sent.
    static final int MAX_ATTEMPTS = 5;
    /// The least time between two codes for one account. Each one is a text
    /// message somebody pays for and somebody receives.
    static final int RESEND_SECONDS = 30;

    private final PhoneCodeRepository codes;
    private final Accounts accounts;
    private final SmsSender sms;
    private final Config config;

    public PhoneVerification(PhoneCodeRepository codes, Accounts accounts, SmsSender sms,
            Config config) {
        this.codes = codes;
        this.accounts = accounts;
        this.sms = sms;
        this.config = config;
    }

    /// Sends a new code, replacing the one before it.
    ///
    /// Not one transaction, because a text message is sent in the middle and a
    /// transaction must not be held open across a call that leaves the server.
    /// The code is stored and committed, then sent, and taken back in a second
    /// transaction when it could not be.
    public PhoneChallengeDto start(String username) throws IOException {
        UserDto user = accounts.describe(username);
        String code = newCode();
        store(username, hash(username, code), System.currentTimeMillis());
        try {
            sms.send(user.phone, "Your Wayline code is " + code);
        } catch (IOException failed) {
            System.err.println("could not send a verification code: " + failed.getMessage());
            discard(username);
            throw new ResponseStatusException(503, "We could not send the code. Try again shortly");
        }
        PhoneChallengeDto challenge = new PhoneChallengeDto();
        challenge.expiresInSeconds = CODE_SECONDS;
        // Only a server that cannot send a text message, and only on a
        // development profile, tells the app what the code was. Both halves
        // matter: a production server whose SMS settings were forgotten must fail
        // to verify anyone, not verify everyone.
        if (!sms.delivers() && config.isDevelopmentProfile()) {
            challenge.demoCode = code;
        }
        return challenge;
    }

    /// Keeps the hash of a new code in place of the one before it. Answers 429
    /// when the one before it was sent a moment ago.
    @Transactional
    void store(String username, String codeHash, long now) throws IOException {
        PhoneCode pending = codes.find(username);
        if (pending != null && now - pending.sentAt < RESEND_SECONDS * 1000L) {
            throw new ResponseStatusException(429, "Wait a moment before asking for another code");
        }
        boolean first = pending == null;
        if (first) {
            pending = new PhoneCode();
            pending.username = username;
        }
        pending.codeHash = codeHash;
        pending.expiresAt = now + CODE_SECONDS * 1000L;
        pending.attempts = 0;
        pending.sentAt = now;
        if (first) {
            codes.add(pending);
        }
    }

    /// Takes back a code that could not be sent.
    @Transactional
    void discard(String username) throws IOException {
        PhoneCode pending = codes.find(username);
        if (pending != null) {
            codes.remove(pending);
        }
    }

    /// Checks the code the user typed. Answers 400 for a wrong or expired one
    /// and 429 once the guesses are used up.
    ///
    /// The refusal of a wrong code is thrown here, outside the transaction that
    /// counted the guess. Thrown inside it, the error would roll the count back
    /// and every code could be guessed at for ever.
    public UserDto verify(String username, String code) throws IOException {
        if (!guess(username, code)) {
            throw new ResponseStatusException(400, "That is not the code we sent");
        }
        return accounts.describe(username);
    }

    /// Takes one guess at an account's code and answers whether it was right.
    /// A right one uses the code up and marks the phone verified; a wrong one
    /// leaves the guess counted, which is why this returns false where it
    /// could have thrown.
    @Transactional
    boolean guess(String username, String code) throws IOException {
        PhoneCode pending = codes.find(username);
        if (pending == null || pending.expiresAt < System.currentTimeMillis()) {
            throw new ResponseStatusException(400, "That code has expired. Ask for a new one");
        }
        if (pending.attempts >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(429, "Too many wrong codes. Ask for a new one");
        }
        String expected = pending.codeHash;
        // Counted before the comparison, and by the database: two guesses sent
        // at once each take a guess, whichever is answered first.
        codes.countGuess(username);
        String typed = code == null ? "" : code.trim();
        if (!Crypto.equalsConstantTime(hash(username, typed).getBytes("UTF-8"),
                expected.getBytes("UTF-8"))) {
            return false;
        }
        pending = codes.find(username);
        if (pending != null) {
            codes.remove(pending);
        }
        accounts.markPhoneVerified(username);
        return true;
    }

    /// Six digits, each value as likely as the next: a draw that would favour
    /// the low numbers is thrown away and drawn again.
    private static String newCode() throws IOException {
        while (true) {
            byte[] random = Crypto.randomBytes(4);
            int value = ((random[0] & 0x7f) << 24) | ((random[1] & 0xff) << 16)
                    | ((random[2] & 0xff) << 8) | (random[3] & 0xff);
            if (value < 2147000000) {
                String digits = String.valueOf(1000000 + value % 1000000);
                return digits.substring(1);
            }
        }
    }

    private static String hash(String username, String code) throws IOException {
        return Ids.sha256(username + ":" + code);
    }
}
