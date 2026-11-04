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
package com.codenameone.examples.dash.social;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.dash.Caller;
import com.codenameone.examples.dash.api.FriendDto;
import com.codenameone.examples.dash.api.InviteClaimDto;
import com.codenameone.examples.dash.api.InviteDto;
import com.codenameone.examples.dash.api.RewardDto;
import com.codenameone.examples.dash.api.SocialApiServer;
import com.codenameone.examples.dash.api.SpotlightDto;
import java.util.List;

@Component
public class SocialEndpoint implements SocialApiServer {
    private final Referrals referrals;
    private final Promotion promotion;

    public SocialEndpoint(Referrals referrals, Promotion promotion) {
        this.referrals = referrals;
        this.promotion = promotion;
    }

    @Override
    public InviteDto invite() throws Exception {
        return referrals.invite(Caller.name());
    }

    @Override
    public RewardDto claim(InviteClaimDto claim) throws Exception {
        return referrals.claim(Caller.name(), claim);
    }

    @Override
    public List<FriendDto> friends() throws Exception {
        return referrals.friends(Caller.name());
    }

    @Override
    public SpotlightDto spotlight() throws Exception {
        return promotion.spotlight();
    }
}
