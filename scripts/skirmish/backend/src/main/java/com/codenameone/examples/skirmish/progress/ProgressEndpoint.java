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
package com.codenameone.examples.skirmish.progress;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.skirmish.Caller;
import com.codenameone.examples.skirmish.api.ClaimDto;
import com.codenameone.examples.skirmish.api.ProgressApiServer;
import com.codenameone.examples.skirmish.api.ProgressDto;
import com.codenameone.examples.skirmish.api.RewardDto;
import com.codenameone.examples.skirmish.social.Referrals;

@Component
public class ProgressEndpoint implements ProgressApiServer {
    private final Progression progression;
    private final Referrals referrals;

    public ProgressEndpoint(Progression progression, Referrals referrals) {
        this.progression = progression;
        this.referrals = referrals;
    }

    @Override
    public ProgressDto progress() throws Exception {
        return progression.view(Caller.name());
    }

    @Override
    public RewardDto claimQuest(ClaimDto quest) throws Exception {
        return progression.claim(Caller.name(), quest == null ? null : quest.questId());
    }

    @Override
    public RewardDto openChest() throws Exception {
        return progression.openChest(Caller.name());
    }

    @Override
    public RewardDto tutorialDone() throws Exception {
        RewardDto reward = progression.tutorialDone(Caller.name());
        // The training battle is a battle: an invited player who finished it
        // has played, and their inviter is paid.
        referrals.played(Caller.name());
        return reward;
    }
}
