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
package com.codenameone.examples.pulse.owner;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PreAuthorize;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.api.CampaignDto;
import com.codenameone.examples.pulse.api.ClassTypeDto;
import com.codenameone.examples.pulse.api.InsightsDto;
import com.codenameone.examples.pulse.api.InvoiceDto;
import com.codenameone.examples.pulse.api.MemberDetailDto;
import com.codenameone.examples.pulse.api.NewSessionDto;
import com.codenameone.examples.pulse.api.OwnerApiServer;
import com.codenameone.examples.pulse.api.OwnerDashboardDto;
import com.codenameone.examples.pulse.api.OwnerMemberDto;
import com.codenameone.examples.pulse.api.PlanDto;
import com.codenameone.examples.pulse.api.ReasonDto;
import com.codenameone.examples.pulse.api.RoomDto;
import com.codenameone.examples.pulse.api.SessionDto;
import com.codenameone.examples.pulse.api.ShiftDto;
import com.codenameone.examples.pulse.api.StaffDto;
import com.codenameone.examples.pulse.billing.Billing;

import java.util.List;

/// The server's half of `OwnerApi`.
@Component
@PreAuthorize("hasRole('OWNER')")
public class OwnerEndpoint implements OwnerApiServer {
    private final Owner owner;
    private final Billing billing;
    private final Insights insights;

    public OwnerEndpoint(Owner owner, Billing billing, Insights insights) {
        this.owner = owner;
        this.billing = billing;
        this.insights = insights;
    }

    @Override
    public OwnerDashboardDto dashboard() throws Exception {
        return owner.dashboard();
    }

    @Override
    public InsightsDto insights() throws Exception {
        return insights.insights();
    }

    @Override
    public List<OwnerMemberDto> members(String filter) throws Exception {
        return insights.members(filter);
    }

    @Override
    public MemberDetailDto member(String id) throws Exception {
        return insights.member(id);
    }

    @Override
    public List<PlanDto> plans() throws Exception {
        return billing.plans(true);
    }

    @Override
    public PlanDto savePlan(PlanDto body) throws Exception {
        return owner.savePlan(body);
    }

    @Override
    public ClassTypeDto saveClassType(ClassTypeDto body) throws Exception {
        return owner.saveClassType(body);
    }

    @Override
    public List<RoomDto> rooms() throws Exception {
        return owner.rooms();
    }

    @Override
    public List<SessionDto> schedule(NewSessionDto body) throws Exception {
        return owner.schedule(body);
    }

    @Override
    public SessionDto cancelClass(String id, ReasonDto body) throws Exception {
        return owner.cancelClass(id, body == null ? "" : body.reason());
    }

    @Override
    public SessionDto substitute(String id, String trainer) throws Exception {
        return owner.substitute(id, trainer);
    }

    @Override
    public List<StaffDto> staff() throws Exception {
        return owner.staff();
    }

    @Override
    public StaffDto saveStaff(StaffDto body) throws Exception {
        return owner.saveStaff(Caller.name(), body);
    }

    @Override
    public List<ShiftDto> shifts(String location) throws Exception {
        return owner.shifts(location);
    }

    @Override
    public ShiftDto addShift(ShiftDto body) throws Exception {
        return owner.addShift(body);
    }

    @Override
    public List<CampaignDto> campaigns() throws Exception {
        return owner.campaigns();
    }

    @Override
    public CampaignDto send(CampaignDto body) throws Exception {
        return owner.send(Caller.name(), body);
    }

    @Override
    public List<InvoiceDto> invoices(String status) throws Exception {
        return billing.all(status);
    }

    @Override
    public InvoiceDto voidInvoice(String id, ReasonDto body) throws Exception {
        return billing.voidInvoice(id, body == null ? "" : body.reason());
    }
}
