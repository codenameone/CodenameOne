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
package com.codenameone.examples.pulse.studio;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.domain.PassKey;
import com.codenameone.examples.pulse.domain.UsedPassCode;
import com.codenameone.examples.pulse.repository.PassKeyRepository;
import com.codenameone.examples.pulse.repository.UsedPassCodeRepository;

import java.io.IOException;
import java.time.Instant;

/// Reading a door pass.
///
/// A pass on a phone is a QR code that changes every
/// [Household#PERIOD_SECONDS]: `pulse:<member id>:<code>`, where the code is
/// an HOTP (RFC 4226, SHA-256, [Household#DIGITS] digits) of the pass's
/// secret over the number of periods since the epoch -- the same
/// `com.codename1.security.Otp` call on the phone and here, so the phone
/// needs no signal to show a pass that works. A screenshot of one is good
/// for at most a period either side of now, and then only once: every code
/// accepted is kept until it could no longer be valid, and a second
/// presentation is refused as a replay.
///
/// An NFC id is the other kind of pass: a fob or wristband, for a child
/// without a phone. It does not change, so it is as good as the thing it is
/// written on, and replacing the pass's secret replaces it too.
@Component
public class Passes {
    /// What a QR pass starts with.
    public static final String PREFIX = "pulse:";
    /// How many periods either side of now a code is still taken, for a
    /// phone whose clock is a little off.
    static final int TOLERANCE = 1;

    private final PassKeyRepository keys;
    private final UsedPassCodeRepository used;

    public Passes(PassKeyRepository keys, UsedPassCodeRepository used) {
        this.keys = keys;
        this.used = used;
    }

    /// What [#redeem] answers for a code that was right but has been shown
    /// before. No member id is empty, so it cannot be mistaken for one.
    public static final String REPLAYED = "";

    /// The member a QR pass is for; [#REPLAYED] when it was shown before; or
    /// null when it is not a pass or not current. Must run inside the caller's
    /// transaction: the code is spent by inserting it, and a second insert of
    /// the same code fails the transaction it is in.
    public String redeem(String scanned, long nowMillis) throws IOException {
        if (scanned == null || !scanned.startsWith(PREFIX)) {
            return null;
        }
        int split = scanned.indexOf(':', PREFIX.length());
        if (split < 0) {
            return null;
        }
        String memberId = scanned.substring(PREFIX.length(), split);
        String code = scanned.substring(split + 1).trim();
        if (memberId.length() == 0 || memberId.length() > 32
                || code.length() != Household.DIGITS) {
            return null;
        }
        PassKey key = keys.findById(memberId);
        if (key == null) {
            return null;
        }
        long counter = counter(nowMillis);
        byte[] secret = bytes(key.getSecret());
        for (long step = counter - TOLERANCE; step <= counter + TOLERANCE; step++) {
            if (same(code(secret, step), code)) {
                String spent = Ids.sha256(memberId + ":" + key.getSecret() + ":" + step);
                if (used.existsById(spent)) {
                    return REPLAYED;
                }
                UsedPassCode row = new UsedPassCode();
                row.setCodeHash(spent);
                row.setExpiresAt(Instant.ofEpochMilli((step + TOLERANCE + 1)
                        * Household.PERIOD_SECONDS * 1000L));
                used.insert(row);
                return memberId;
            }
        }
        return null;
    }

    /// The member a fob is for, or null.
    public String byNfc(String nfcId) {
        if (nfcId == null || nfcId.length() == 0 || nfcId.length() > 64) {
            return null;
        }
        PassKey key = keys.findFirstByNfcId(nfcId);
        return key == null ? null : key.getMemberId();
    }

    /// The code a pass shows at a moment, for the tests and the demo kiosk.
    public static String code(String secretHex, long nowMillis) {
        return code(bytes(secretHex), counter(nowMillis));
    }

    static String code(byte[] secret, long counter) {
        return Otp.hotp(secret, counter, Household.DIGITS, Hash.SHA256);
    }

    static long counter(long nowMillis) {
        return nowMillis / 1000L / Household.PERIOD_SECONDS;
    }

    /// Forgets the codes that could no longer be accepted.
    @Scheduled(fixedDelay = 300000, initialDelay = 60000, lock = "pulse-pass-codes")
    @Transactional
    public void sweep() {
        used.deleteByExpiresAtLessThan(Instant.now());
    }

    private static boolean same(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int iter = 0; iter < a.length(); iter++) {
            diff |= a.charAt(iter) ^ b.charAt(iter);
        }
        return diff == 0;
    }

    private static byte[] bytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) ((Character.digit(hex.charAt(iter * 2), 16) << 4)
                    | Character.digit(hex.charAt(iter * 2 + 1), 16));
        }
        return out;
    }
}
