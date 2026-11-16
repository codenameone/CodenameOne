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
import com.codenameone.examples.fieldline.account.Accounts;
import com.codenameone.examples.fieldline.ai.Assistant;
import com.codenameone.examples.fieldline.api.AssistantReplyDto;
import com.codenameone.examples.fieldline.api.AssistantRequestDto;
import com.codenameone.examples.fieldline.api.AvailabilityDto;
import com.codenameone.examples.fieldline.api.ChecklistDto;
import com.codenameone.examples.fieldline.api.ClockDto;
import com.codenameone.examples.fieldline.api.FieldApiServer;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.api.LocationDto;
import com.codenameone.examples.fieldline.api.PhotoDto;
import com.codenameone.examples.fieldline.api.PhotoUploadDto;
import com.codenameone.examples.fieldline.api.ReportDto;
import com.codenameone.examples.fieldline.api.SignatureDto;
import com.codenameone.examples.fieldline.api.StatusDto;
import com.codenameone.examples.fieldline.api.TimeOffDto;
import com.codenameone.examples.fieldline.api.TodayDto;
import com.codenameone.examples.fieldline.api.UserDto;

import java.util.List;

/// The server's half of `FieldApi`: what a technician does in the field.
@Component
public class FieldEndpoint implements FieldApiServer {
    private final Accounts accounts;
    private final Jobs jobs;
    private final Crew crew;
    private final Planner planner;
    private final Assistant assistant;
    private final Timekeeping clock;

    public FieldEndpoint(Accounts accounts, Jobs jobs, Crew crew, Planner planner,
            Assistant assistant, Timekeeping clock) {
        this.clock = clock;
        this.accounts = accounts;
        this.jobs = jobs;
        this.crew = crew;
        this.planner = planner;
        this.assistant = assistant;
    }

    private UserDto me() throws Exception {
        return accounts.describe(Caller.name());
    }

    @Override
    public TodayDto today() throws Exception {
        return jobs.today(Caller.name());
    }

    @Override
    public TodayDto clock(ClockDto clock) throws Exception {
        this.clock.clock(crew.of(Caller.name()).getId(), clock != null && clock.in());
        return jobs.today(Caller.name());
    }

    @Override
    public JobDto job(String id) throws Exception {
        return jobs.job(me(), id);
    }

    @Override
    public JobDto setStatus(String id, StatusDto status) throws Exception {
        if (status == null || status.status() == null) {
            throw new ResponseStatusException(400, "Which status?");
        }
        return jobs.setStatus(me(), id, status.status(), status.lat(), status.lng());
    }

    @Override
    public JobDto saveChecklist(String id, ChecklistDto checklist) throws Exception {
        return jobs.saveChecklist(me(), id, checklist);
    }

    @Override
    public PhotoDto addPhoto(String id, PhotoUploadDto photo) throws Exception {
        return jobs.addPhoto(me(), id, photo);
    }

    @Override
    public List<PhotoDto> photos(String id) throws Exception {
        return jobs.photos(me(), id);
    }

    @Override
    public SignatureDto sign(String id, SignatureDto signature) throws Exception {
        return jobs.sign(me(), id, signature);
    }

    @Override
    public SignatureDto signature(String id) throws Exception {
        return jobs.signature(me(), id);
    }

    @Override
    public ReportDto draftReport(String id) throws Exception {
        return jobs.draftReport(me(), id);
    }

    @Override
    public AvailabilityDto availability() throws Exception {
        return crew.availability(Caller.name());
    }

    /// New hours change what fits, so the plan is repaired around them.
    @Override
    public AvailabilityDto saveAvailability(AvailabilityDto availability) throws Exception {
        AvailabilityDto saved = crew.saveAvailability(Caller.name(), availability);
        planner.repair(Planner.AVAILABILITY, me().displayName() + " changed their hours",
                Caller.name());
        return saved;
    }

    @Override
    public List<TimeOffDto> myTimeOff() throws Exception {
        return crew.myTimeOff(Caller.name());
    }

    @Override
    public TimeOffDto requestTimeOff(TimeOffDto request) throws Exception {
        return crew.requestTimeOff(Caller.name(), request);
    }

    @Override
    public LocationDto reportLocation(LocationDto where) throws Exception {
        if (where == null) {
            throw new ResponseStatusException(400, "Where?");
        }
        crew.reportLocation(Caller.name(), where.lat(), where.lng());
        return where;
    }

    @Override
    public AssistantReplyDto ask(AssistantRequestDto request) throws Exception {
        return assistant.ask(me(), request);
    }
}
