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
package com.codenameone.examples.brickyard.referral;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.InviteDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import com.codenameone.examples.brickyard.domain.Account;
import com.codenameone.examples.brickyard.domain.FraudFlag;
import com.codenameone.examples.brickyard.domain.Invite;
import com.codenameone.examples.brickyard.domain.Referral;
import com.codenameone.examples.brickyard.economy.Wallet;
import com.codenameone.examples.brickyard.repository.FraudFlagRepository;
import com.codenameone.examples.brickyard.repository.InviteRepository;
import com.codenameone.examples.brickyard.repository.ReferralRepository;
import com.codenameone.examples.brickyard.social.Social;
import java.io.IOException;

/// The transactional half of redeeming an invite; [Referrals] is the door.
@Component
public class ReferralLedger {
    /// A refusal for fraud. Thrown out of the transaction, so it rolls back;
    /// [Referrals] writes the flag it describes afterwards.
    public static final class Refused extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String username;
        private final String kind;
        private final String detail;

        Refused(String username, String kind, String detail) {
            super(kind);
            this.username = username;
            this.kind = kind;
            this.detail = detail;
        }

        public String username() {
            return username;
        }

        public String kind() {
            return kind;
        }

        public String detail() {
            return detail;
        }
    }

    private final Accounts accounts;
    private final Wallet wallet;
    private final InviteRepository invites;
    private final ReferralRepository referrals;
    private final FraudFlagRepository fraud;

    public ReferralLedger(Accounts accounts, Wallet wallet, InviteRepository invites,
            ReferralRepository referrals, FraudFlagRepository fraud) {
        this.accounts = accounts;
        this.wallet = wallet;
        this.invites = invites;
        this.referrals = referrals;
        this.fraud = fraud;
    }

    /// Redeems an invite: both sides get the referral Bricks. Refused, and
    /// flagged for staff, when the code is the caller's own, when the
    /// redeeming device is the one that made the code, or when the inviter
    /// passed the daily cap. An account redeems once, and only within the
    /// referral window of opening. Redeeming from the network address the
    /// code was made on is allowed -- a household shares one -- but is
    /// flagged, in this transaction, for staff to look at.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto redeem(String username, InviteDto request, String ipHash) throws IOException {
        String device = Referrals.deviceHash(request);
        Invite i = request.code() == null ? null : invites.findById(request.code().trim());
        if (i == null) {
            throw new ResponseStatusException(404, "No such invite");
        }
        if (referrals.findById(username) != null) {
            throw new ResponseStatusException(409, "You already redeemed an invite");
        }
        Account me = accounts.account(username);
        if (me.getCreatedAt() < Clock.now() - Social.REFERRAL_WINDOW) {
            throw new ResponseStatusException(409, "Invites are for new accounts");
        }
        if (i.getInviter().equals(username)) {
            throw new Refused(username, "SELF_REFERRAL", "own code " + i.getId());
        }
        if (i.getDeviceHash().equals(device)) {
            throw new Refused(username, "SAME_DEVICE", "code " + i.getId() + " from " + i.getInviter());
        }
        if (referrals.byInviterSince(i.getInviter(), Clock.now() - Clock.DAY).size() >= Social.REFERRALS_PER_DAY) {
            throw new Refused(i.getInviter(), "REFERRAL_VELOCITY", "redeemed by " + username);
        }
        Referral r = new Referral();
        r.setId(username);
        r.setInviter(i.getInviter());
        r.setCode(i.getId());
        r.setDeviceHash(device);
        r.setIpHash(ipHash == null ? "" : ipHash);
        r.setCreatedAt(Clock.now());
        referrals.insert(r);
        if (r.getIpHash().length() > 0 && r.getIpHash().equals(i.getIpHash())) {
            fraud.insert(fraudFlag(username, "SAME_NETWORK", "code " + i.getId() + " from " + i.getInviter()));
        }
        wallet.post(username, Social.REFERRAL_BRICKS, "REFERRAL", i.getInviter());
        wallet.post(i.getInviter(), Social.REFERRAL_BRICKS, "REFERRAL", username);
        return wallet.describe(username);
    }

    static FraudFlag fraudFlag(String username, String kind, String detail) throws IOException {
        FraudFlag f = new FraudFlag();
        f.setId(Ids.next());
        f.setUsername(username);
        f.setKind(kind);
        f.setDetail(detail.length() > 400 ? detail.substring(0, 400) : detail);
        f.setCreatedAt(Clock.now());
        return f;
    }
}
