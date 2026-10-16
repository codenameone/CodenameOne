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
import com.codenameone.examples.wayline.repository.PhoneCodeRepository;
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
    /// guesses, each against at most two live codes (see [#store]), is one
    /// chance in 100,000 per code sent.
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
        String codeHash = hash(username, code);
        store(username, codeHash, System.currentTimeMillis());
        try {
            sms.send(user.phone(), "Your Wayline code is " + code);
        } catch (IOException failed) {
            System.err.println("could not send a verification code: " + failed.getMessage());
            discard(username, codeHash);
            throw new ResponseStatusException(503, "We could not send the code. Try again shortly");
        }
        // Only a server that cannot send a text message, and only on a
        // development profile, tells the app what the code was. Both halves
        // matter: a production server whose SMS settings were forgotten must fail
        // to verify anyone, not verify everyone.
        String demoCode = !sms.delivers() && config.isDevelopmentProfile() ? code : null;
        return new PhoneChallengeDto(CODE_SECONDS, demoCode);
    }

    /// Keeps the hash of a new code in place of the one before it. Answers 429
    /// when the one before it was sent a moment ago.
    ///
    /// The code before it stays good until it would have expired anyway. A
    /// second code is asked for when the first is slow to arrive, and text
    /// messages are not delivered in order: the first may arrive last, and the
    /// newest message on the phone must not be a code the server refuses. Only
    /// one is kept, so at most two codes are live, and only one whose guesses
    /// were not used up.
    @Transactional
    void store(String username, String codeHash, long now) throws IOException {
        // Before the code is read: two requests for one account take turns,
        // so the second finds the code the first just stored and is told to
        // wait. Otherwise both pass the check below and both send a text.
        accounts.hold(username);
        PhoneCode pending = codes.findById(username);
        if (pending != null && now - pending.getSentAt() < RESEND_SECONDS * 1000L) {
            throw new ResponseStatusException(429, "Wait a moment before asking for another code");
        }
        boolean first = pending == null;
        if (first) {
            pending = new PhoneCode();
            pending.setUsername(username);
        } else if (pending.getExpiresAt() >= now && pending.getAttempts() < MAX_ATTEMPTS) {
            // With the guesses it has used: asking for another code must not
            // buy the old one a fresh set.
            pending.setPreviousHash(pending.getCodeHash());
            pending.setPreviousExpiresAt(pending.getExpiresAt());
            pending.setPreviousAttempts(pending.getAttempts());
        } else {
            pending.setPreviousHash("");
            pending.setPreviousExpiresAt(0L);
            pending.setPreviousAttempts(0);
        }
        pending.setCodeHash(codeHash);
        pending.setExpiresAt(now + CODE_SECONDS * 1000L);
        pending.setAttempts(0);
        pending.setSentAt(now);
        if (first) {
            codes.insert(pending);
        }
    }

    /// Takes back a code that could not be sent, if it is still the one the
    /// account is waiting on. A send can take longer to fail than the wait
    /// between two codes, and by then a later request may have stored a code
    /// of its own and had it delivered: that one is the user's, and stays.
    /// The code before it, when it is still good, is the one waited on again,
    /// and another may be asked for at once.
    @Transactional
    void discard(String username, String codeHash) throws IOException {
        // The hold store() takes, before the row is read: a resend storing its
        // code meanwhile commits first, and this then finds that code and
        // leaves it, where read before the wait it would overwrite or delete it.
        accounts.hold(username);
        PhoneCode pending = codes.findById(username);
        if (pending == null || !codeHash.equals(pending.getCodeHash())) {
            return;
        }
        if (pending.getPreviousHash().length() == 0
                || pending.getPreviousExpiresAt() < System.currentTimeMillis()) {
            codes.deleteById(username);
            return;
        }
        pending.setCodeHash(pending.getPreviousHash());
        pending.setExpiresAt(pending.getPreviousExpiresAt());
        pending.setAttempts(pending.getPreviousAttempts());
        pending.setPreviousHash("");
        pending.setPreviousExpiresAt(0L);
        pending.setPreviousAttempts(0);
        pending.setSentAt(0L);
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
        PhoneCode pending = codes.findById(username);
        if (pending == null || pending.getExpiresAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(400, "That code has expired. Ask for a new one");
        }
        if (pending.getAttempts() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(429, "Too many wrong codes. Ask for a new one");
        }
        String expected = pending.getCodeHash();
        // Taken before the comparison, and by the database, which also holds
        // the limit: the count read above is what it was when this request
        // arrived, and any number of requests arriving together all read the
        // same one. Only a request that is given a guess has its code looked at.
        // And only of the code read above: one sent meanwhile is not this one.
        if (codes.takeGuess(username, expected, MAX_ATTEMPTS) != 1) {
            throw new ResponseStatusException(429, "Too many wrong codes. Ask for a new one");
        }
        // Read again after the guess was taken, which the update did under
        // the row's lock: the count of the code before is then this guess's
        // own, and within its limit only when this guess was one it had left.
        PhoneCode counted = codes.findById(username);
        String typed = code == null ? "" : code.trim();
        byte[] given = hash(username, typed).getBytes("UTF-8");
        // Both compared every time, so the answer takes as long either way. The
        // code before is good only until it would have expired; it was sent
        // first, so it never outlives the current one checked above.
        //
        // `counted.getPreviousAttempts()` already includes every guess made
        // since the resend: takeGuess adds one to `previousAttempts` in the
        // same statement that adds one to `attempts`, so it is the old code's
        // whole count, not the snapshot store() copied. A code that used four
        // guesses before the resend reads 5 on its first guess after it (still
        // allowed: its fifth) and 6 on the next (refused), so its budget stays
        // five. AccountTest.theCodeBeforeKeepsTheGuessesItHadLeft holds that.
        boolean current = Crypto.equalsConstantTime(given, expected.getBytes("UTF-8"));
        boolean previous = Crypto.equalsConstantTime(given,
                pending.getPreviousHash().getBytes("UTF-8"))
                && pending.getPreviousHash().length() > 0
                && pending.getPreviousExpiresAt() >= System.currentTimeMillis()
                && counted != null && counted.getPreviousAttempts() <= MAX_ATTEMPTS;
        if (!current && !previous) {
            return false;
        }
        pending = codes.findById(username);
        if (pending != null) {
            codes.deleteById(username);
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

    /// An unkeyed digest, kept so on purpose. Somebody who can read the
    /// `phone_code` table could try every code within the five minutes one
    /// lasts, but somebody who can read the database already owns what a
    /// phone check guards: it proves a number, and grants nothing on its own.
    /// A keyed digest under a server secret is a hardening step for a
    /// production deployment, listed in the implementor's guide among what the
    /// sample leaves out.
    private static String hash(String username, String code) throws IOException {
        return Ids.sha256(username + ":" + code);
    }
}
