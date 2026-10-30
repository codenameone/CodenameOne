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
package com.codenameone.examples.pulse.trainer;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.Roles;
import com.codenameone.examples.pulse.api.AttendanceDto;
import com.codenameone.examples.pulse.api.RosterEntryDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.ShiftDto;
import com.codenameone.examples.pulse.api.TrainerApiServer;

import java.util.List;

/// The server's half of `TrainerApi`. An owner may read any class's roster
/// and take its attendance; a trainer only their own.
@Component
@PreAuthorize("hasAnyRole('TRAINER', 'OWNER')")
public class TrainerEndpoint implements TrainerApiServer {
    private final Trainers trainers;

    public TrainerEndpoint(Trainers trainers) {
        this.trainers = trainers;
    }

    @Override
    public List<SessionDto> classes() throws Exception {
        return trainers.classes(Caller.name());
    }

    @Override
    public List<RosterEntryDto> roster(String id) throws Exception {
        return trainers.roster(Caller.name(), Caller.hasRole(Roles.OWNER), id);
    }

    @Override
    public RosterEntryDto attendance(String id, AttendanceDto mark) throws Exception {
        return trainers.attendance(Caller.name(), Caller.hasRole(Roles.OWNER), id, mark);
    }

    @Override
    public List<ShiftDto> shifts() throws Exception {
        return trainers.shifts(Caller.name());
    }

    @Override
    public ShiftDto offer(String id) throws Exception {
        return trainers.offer(Caller.name(), id);
    }

    @Override
    public ShiftDto take(String id) throws Exception {
        return trainers.take(Caller.name(), id);
    }
}
