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
package com.codenameone.examples.splat.mission;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.splat.api.MissionApiServer;
import com.codenameone.examples.splat.api.MissionDto;
import java.util.List;

/// `/api/missions`: signed-in players only, like the rest of `/api/**`.
@Component
public class MissionEndpoint implements MissionApiServer {
    private final Missions missions;

    public MissionEndpoint(Missions missions) {
        this.missions = missions;
    }

    @Override
    public MissionDto publishMission(MissionDto mission) throws Exception {
        return missions.publish(Caller.name(), mission == null ? null : mission.json());
    }

    @Override
    public List<MissionDto> missions() throws Exception {
        return missions.list(Caller.name());
    }

    @Override
    public MissionDto mission(String code) throws Exception {
        return missions.get(code);
    }
}
