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
package com.codenameone.examples.pulse.desk;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.api.CheckInRequestDto;
import com.codenameone.examples.pulse.api.CheckInResultDto;
import com.codenameone.examples.pulse.api.DeskApiServer;
import com.codenameone.examples.pulse.api.DeskDayDto;
import com.codenameone.examples.pulse.api.MemberSummaryDto;
import com.codenameone.examples.pulse.api.WalkInDto;

import java.util.List;

/// The server's half of `DeskApi`, for the front desk, the door kiosk and
/// the owner. Guarded by path in the filter chain and by role here.
@Component
@PreAuthorize("hasAnyRole('DESK', 'OWNER')")
public class DeskEndpoint implements DeskApiServer {
    private final Desk desk;

    public DeskEndpoint(Desk desk) {
        this.desk = desk;
    }

    @Override
    public CheckInResultDto checkIn(CheckInRequestDto request) throws Exception {
        return desk.checkIn(Caller.name(), request);
    }

    @Override
    public List<MemberSummaryDto> find(String q) throws Exception {
        Caller.name();
        return desk.find(q);
    }

    @Override
    public WalkInDto walkIn(WalkInDto walkIn) throws Exception {
        return desk.walkIn(Caller.name(), walkIn);
    }

    @Override
    public DeskDayDto today(String location) throws Exception {
        return desk.today(Caller.name(), location);
    }
}
