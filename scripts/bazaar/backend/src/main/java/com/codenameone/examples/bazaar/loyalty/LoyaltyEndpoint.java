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
package com.codenameone.examples.bazaar.loyalty;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.LedgerEntryDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.api.LoyaltyApiServer;
import com.codenameone.examples.bazaar.api.LoyaltyMemberDto;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyRewardDto;
import com.codenameone.examples.bazaar.api.PointsAdjustDto;
import com.codenameone.examples.bazaar.api.RedeemDto;
import com.codenameone.examples.bazaar.api.ReferralDto;

import java.util.List;

/// The server's half of `LoyaltyApi`. Merchant calls are checked against the
/// store's staff by [Loyalty]; shopper calls act on the caller's own card.
@Component
public class LoyaltyEndpoint implements LoyaltyApiServer {
    private final Loyalty loyalty;

    public LoyaltyEndpoint(Loyalty loyalty) {
        this.loyalty = loyalty;
    }

    @Override
    public LoyaltyProgramDto program(String storeId) throws Exception {
        return loyalty.program(Caller.name(), storeId);
    }

    @Override
    public LoyaltyProgramDto saveProgram(String storeId, LoyaltyProgramDto body) throws Exception {
        return loyalty.saveProgram(Caller.name(), storeId, body);
    }

    @Override
    public List<LoyaltyMemberDto> members(String storeId, String q) throws Exception {
        return loyalty.members(Caller.name(), storeId, q);
    }

    @Override
    public LoyaltyMemberDto card(String storeId, String token) throws Exception {
        return loyalty.card(Caller.name(), storeId, token);
    }

    @Override
    public List<LedgerEntryDto> ledger(String storeId, String memberId) throws Exception {
        return loyalty.ledger(Caller.name(), storeId, memberId);
    }

    @Override
    public LoyaltyMemberDto adjust(String storeId, String memberId, PointsAdjustDto body)
            throws Exception {
        if (body == null) {
            throw new ResponseStatusException(400, "Say how many points");
        }
        return loyalty.adjust(Caller.name(), storeId, memberId, body.points(), body.note());
    }

    @Override
    public LoyaltyRewardDto redeemFor(String storeId, String memberId, RedeemDto body)
            throws Exception {
        return loyalty.redeemFor(Caller.name(), storeId, memberId, body == null ? 0 : body.points());
    }

    @Override
    public LoyaltyMemberDto me(String storeId) throws Exception {
        return loyalty.me(Codes.callerOrNull(), storeId);
    }

    @Override
    public List<LedgerEntryDto> myLedger(String storeId) throws Exception {
        return loyalty.myLedger(Codes.callerOrNull(), storeId);
    }

    @Override
    public List<LoyaltyRewardDto> myRewards(String storeId) throws Exception {
        return loyalty.myRewards(Codes.callerOrNull(), storeId);
    }

    @Override
    public LoyaltyRewardDto redeem(String storeId, RedeemDto body) throws Exception {
        return loyalty.redeem(Codes.callerOrNull(), storeId, body == null ? 0 : body.points());
    }

    @Override
    public LoyaltyMemberDto referral(String storeId, ReferralDto body) throws Exception {
        return loyalty.applyReferral(Codes.callerOrNull(), storeId, body == null ? null : body.code());
    }
}
