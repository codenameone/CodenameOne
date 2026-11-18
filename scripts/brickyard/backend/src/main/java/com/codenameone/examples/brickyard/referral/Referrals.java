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
import com.codenameone.examples.brickyard.ClientAddress;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.InviteDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import com.codenameone.examples.brickyard.domain.Invite;
import com.codenameone.examples.brickyard.repository.FraudFlagRepository;
import com.codenameone.examples.brickyard.repository.InviteRepository;
import java.io.IOException;

/// Invite codes and their redemption.
///
/// Redeeming is one transaction ([ReferralLedger#redeem]): the referral row
/// and both credits commit together or not at all. A refusal for fraud rolls
/// that transaction back, and its flag is then written here, outside it, in
/// a step of its own that commits -- staff see every refused attempt.
@Component
public class Referrals {
    private final ReferralLedger ledger;
    private final InviteRepository invites;
    private final FraudFlagRepository fraud;

    public Referrals(ReferralLedger ledger, InviteRepository invites, FraudFlagRepository fraud) {
        this.ledger = ledger;
        this.invites = invites;
        this.fraud = fraud;
    }

    /// A new code, remembering a hash of the device and of the network
    /// address it was made from.
    @Transactional(rollbackFor = IOException.class)
    public InviteDto createInvite(String username, InviteDto request) throws IOException {
        Invite i = new Invite();
        i.setId(Ids.next().substring(0, 12));
        i.setInviter(username);
        i.setDeviceHash(deviceHash(request));
        i.setIpHash(ClientAddress.hash(ClientAddress.current()));
        i.setCreatedAt(Clock.now());
        invites.insert(i);
        return new InviteDto(i.getId(), "");
    }

    static String deviceHash(InviteDto request) throws IOException {
        if (request == null || request.deviceId() == null || request.deviceId().trim().length() < 8) {
            throw new ResponseStatusException(400, "An invite needs the device id");
        }
        return Ids.sha256(request.deviceId().trim()).substring(0, 64);
    }

    /// See [ReferralLedger#redeem] for the rules.
    public WalletDto redeem(String username, InviteDto request) throws IOException {
        String ip = ClientAddress.hash(ClientAddress.current());
        try {
            return ledger.redeem(username, request, ip);
        } catch (ReferralLedger.Refused refused) {
            flag(refused.username(), refused.kind(), refused.detail());
            throw new ResponseStatusException(403, "That invite cannot be redeemed");
        }
    }

    /// Writes a fraud flag in its own step; called outside any transaction.
    void flag(String username, String kind, String detail) throws IOException {
        fraud.insert(ReferralLedger.fraudFlag(username, kind, detail));
    }
}
