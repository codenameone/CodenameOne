/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.run;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.papercut.api.DailyDto;
import com.codenameone.examples.papercut.api.LeaderDto;
import com.codenameone.examples.papercut.api.RunApiServer;
import com.codenameone.examples.papercut.api.RunDto;
import com.codenameone.examples.papercut.api.RunResultDto;
import com.codenameone.examples.papercut.api.RunTicketDto;
import com.codenameone.examples.papercut.api.StartRunDto;
import java.util.List;

/// `RunApi` on the server.
@Component
public class RunEndpoint implements RunApiServer {
    private final Runs runs;
    private final Boards boards;

    public RunEndpoint(Runs runs, Boards boards) {
        this.runs = runs;
        this.boards = boards;
    }

    @Override
    public RunTicketDto start(StartRunDto request) throws Exception {
        return runs.start(Caller.name(), request == null ? null : request.mode());
    }

    @Override
    public RunResultDto submit(RunDto run) throws Exception {
        return runs.submit(Caller.name(), run);
    }

    @Override
    public List<LeaderDto> leaderboard(String mode, String period) throws Exception {
        return boards.leaderboard(Caller.name(), mode, period);
    }

    @Override
    public DailyDto daily() throws Exception {
        return boards.daily(Caller.name());
    }
}
