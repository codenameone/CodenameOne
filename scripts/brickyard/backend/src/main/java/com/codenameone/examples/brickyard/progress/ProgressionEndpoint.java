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
package com.codenameone.examples.brickyard.progress;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.ProgressDto;
import com.codenameone.examples.brickyard.api.ProgressEventDto;
import com.codenameone.examples.brickyard.api.ProgressionApiServer;
import com.codenameone.examples.brickyard.api.QuestDto;
import com.codenameone.examples.brickyard.api.SeasonTierDto;
import java.util.List;

/// The progression routes; the work is in [Progression].
@Component
public class ProgressionEndpoint implements ProgressionApiServer {
    private final Progression progression;

    public ProgressionEndpoint(Progression progression) {
        this.progression = progression;
    }

    @Override
    public ProgressDto progress() throws Exception {
        return progression.describe(Caller.name());
    }

    @Override
    public ProgressDto dailyLogin() throws Exception {
        return progression.dailyLogin(Caller.name());
    }

    @Override
    public List<QuestDto> quests() throws Exception {
        return progression.quests(Caller.name());
    }

    @Override
    public QuestDto claimQuest(String questId) throws Exception {
        return progression.claimQuest(Caller.name(), questId);
    }

    @Override
    public ProgressDto reportProgress(ProgressEventDto event) throws Exception {
        return progression.report(event);
    }

    @Override
    public List<SeasonTierDto> season() throws Exception {
        return progression.season(Caller.name());
    }

    @Override
    public ProgressDto buyPremium() throws Exception {
        return progression.buyPremium(Caller.name());
    }

    @Override
    public SeasonTierDto claimTier(int tier, String track) throws Exception {
        return progression.claimTier(Caller.name(), tier, track);
    }
}
