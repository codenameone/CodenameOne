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
package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.fieldline.api.AbsenceDto;
import com.codenameone.examples.fieldline.api.BoardDto;
import com.codenameone.examples.fieldline.api.DecisionDto;
import com.codenameone.examples.fieldline.api.DispatchApiServer;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.MoveDto;
import com.codenameone.examples.fieldline.api.IntakeDto;
import com.codenameone.examples.fieldline.api.NewJobDto;
import com.codenameone.examples.fieldline.api.ScheduleRunDto;
import com.codenameone.examples.fieldline.api.SiteDto;
import com.codenameone.examples.fieldline.api.TechnicianDto;
import com.codenameone.examples.fieldline.api.TimeOffDto;
import com.codenameone.examples.fieldline.api.ValidationDto;

import com.codenameone.examples.fieldline.api.StaffingDto;
import java.util.ArrayList;
import java.util.List;

/// The server's half of `DispatchApi`. Every path is under `/api/dispatch`,
/// which the security configuration opens to dispatchers alone.
@Component
public class DispatchEndpoint implements DispatchApiServer {
    private final Jobs jobs;
    private final Planner planner;

    public DispatchEndpoint(Jobs jobs, Planner planner) {
        this.jobs = jobs;
        this.planner = planner;
    }

    /// The board always covers the planning horizon; the window the app asks
    /// for is a hint it may scroll within.
    @Override
    public BoardDto board(long day, int days) throws Exception {
        return jobs.board();
    }

    @Override
    public List<TechnicianDto> technicians() throws Exception {
        return jobs.board().technicians();
    }

    @Override
    public List<SiteDto> sites() throws Exception {
        return jobs.sites();
    }

    @Override
    public JobDto createJob(NewJobDto job) throws Exception {
        JobDto made = jobs.create(job);
        planner.repair(Planner.NEW_JOB, "New job: " + made.title(), Caller.name());
        return jobs.job(Jobs.DISPATCHER, made.id());
    }

    @Override
    public JobDto intake(String id, IntakeDto intake) throws Exception {
        if (intake == null || intake.status() == null) {
            throw new ResponseStatusException(400, "Which state?");
        }
        return jobs.intake(id, intake.status());
    }

    @Override
    public ScheduleRunDto optimize() throws Exception {
        return planner.optimize(Caller.name());
    }

    @Override
    public ScheduleRunDto absence(AbsenceDto absence) throws Exception {
        return planner.absence(absence, Caller.name());
    }

    @Override
    public ValidationDto move(MoveDto move) throws Exception {
        Planner.Validation v = planner.move(move, Caller.name());
        JobDto job = v.ok ? jobs.job(Jobs.DISPATCHER, move.jobId()) : null;
        return new ValidationDto(v.ok, v.message, new ArrayList<String>(v.warnings), job);
    }

    @Override
    public List<TimeOffDto> timeOff() throws Exception {
        return jobs.pendingTimeOff();
    }

    @Override
    public ScheduleRunDto decide(String id, DecisionDto decision) throws Exception {
        if (decision == null) {
            throw new ResponseStatusException(400, "Approve or decline?");
        }
        return planner.decide(id, decision.approve(), Caller.name());
    }

    @Override
    public List<ScheduleRunDto> runs() throws Exception {
        return planner.history(30);
    }

    @Override
    public StaffingDto staffing(int days) throws Exception {
        return planner.staffing(days);
    }
}
