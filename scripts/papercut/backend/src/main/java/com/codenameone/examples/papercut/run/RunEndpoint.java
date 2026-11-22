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
