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
package com.codenameone.examples.slate.owner;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.slate.api.BusinessDto;
import com.codenameone.examples.slate.api.DashboardDto;
import com.codenameone.examples.slate.api.LocationDto;
import com.codenameone.examples.slate.api.OwnerApiServer;
import com.codenameone.examples.slate.api.PolicyDto;
import com.codenameone.examples.slate.api.PreviewDto;
import com.codenameone.examples.slate.api.PreviewRequestDto;
import com.codenameone.examples.slate.api.ProviderDto;
import com.codenameone.examples.slate.api.ResourceDto;
import com.codenameone.examples.slate.api.ScheduleDto;
import com.codenameone.examples.slate.api.ServiceDto;
import com.codenameone.examples.slate.api.WeekDto;
import com.codenameone.examples.slate.booking.Availability;
import com.codenameone.examples.slate.catalog.Catalog;
import com.codenameone.examples.slate.live.LiveHub;
import com.codenameone.examples.slate.provider.Schedules;

import java.util.List;

/// The server's half of `OwnerApi`: setting the business up and watching it
/// run. Guarded by path in the filter chain and by role here.
///
/// A change to the rules, the services or anyone's hours changes what is
/// offered, so each tells every open app to read its slots again.
@Component
@PreAuthorize("hasRole('OWNER')")
public class OwnerEndpoint implements OwnerApiServer {
    private final Catalog catalog;
    private final Schedules schedules;
    private final Availability availability;
    private final Dashboard dashboard;
    private final LiveHub live;

    public OwnerEndpoint(Catalog catalog, Schedules schedules, Availability availability,
            Dashboard dashboard, LiveHub live) {
        this.catalog = catalog;
        this.schedules = schedules;
        this.availability = availability;
        this.dashboard = dashboard;
        this.live = live;
    }

    @Override
    public BusinessDto saveBusiness(BusinessDto body) throws Exception {
        return catalog.saveBusiness(body);
    }

    @Override
    public PolicyDto savePolicy(PolicyDto body) throws Exception {
        PolicyDto saved = catalog.savePolicy(body);
        live.slotsChanged("", "");
        return saved;
    }

    @Override
    public List<ServiceDto> services() throws Exception {
        return catalog.services(false);
    }

    @Override
    public ServiceDto saveService(ServiceDto body) throws Exception {
        ServiceDto saved = catalog.saveService(body);
        live.slotsChanged(saved.id(), "");
        return saved;
    }

    @Override
    public List<ProviderDto> providers() throws Exception {
        return catalog.providers(false);
    }

    @Override
    public ProviderDto saveProvider(ProviderDto body) throws Exception {
        ProviderDto saved = catalog.saveProvider(body, true);
        live.slotsChanged("", "");
        live.scheduleChanged(saved.username());
        return saved;
    }

    @Override
    public WeekDto hours(String username) throws Exception {
        return schedules.week(username);
    }

    @Override
    public WeekDto saveHours(String username, WeekDto body) throws Exception {
        WeekDto saved = schedules.saveWeek(username, body);
        live.slotsChanged("", "");
        return saved;
    }

    @Override
    public List<LocationDto> locations() throws Exception {
        return catalog.locations(false);
    }

    @Override
    public LocationDto saveLocation(LocationDto body) throws Exception {
        LocationDto saved = catalog.saveLocation(body);
        live.slotsChanged("", "");
        return saved;
    }

    @Override
    public List<ResourceDto> resources() throws Exception {
        return catalog.resources();
    }

    @Override
    public ResourceDto saveResource(ResourceDto body) throws Exception {
        ResourceDto saved = catalog.saveResource(body);
        live.slotsChanged("", "");
        return saved;
    }

    @Override
    public List<ScheduleDto> schedule(String day) throws Exception {
        return schedules.day(day);
    }

    @Override
    public PreviewDto preview(PreviewRequestDto body) throws Exception {
        return availability.preview(body);
    }

    @Override
    public DashboardDto dashboard() throws Exception {
        return dashboard.read();
    }
}
