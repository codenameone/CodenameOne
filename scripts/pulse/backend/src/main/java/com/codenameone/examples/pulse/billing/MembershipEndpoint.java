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
package com.codenameone.examples.pulse.billing;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.api.FreezeDto;
import com.codenameone.examples.pulse.api.InvoiceDto;
import com.codenameone.examples.pulse.api.JoinDto;
import com.codenameone.examples.pulse.api.MembershipApiServer;
import com.codenameone.examples.pulse.api.MembershipDto;
import com.codenameone.examples.pulse.api.PayInvoiceDto;
import com.codenameone.examples.pulse.api.PlanDto;

import java.util.List;

/// The server's half of `MembershipApi`: a household's plans and bills.
@Component
public class MembershipEndpoint implements MembershipApiServer {
    private final Billing billing;

    public MembershipEndpoint(Billing billing) {
        this.billing = billing;
    }

    @Override
    public List<PlanDto> plans() throws Exception {
        return billing.plans(false);
    }

    @Override
    public List<MembershipDto> memberships() throws Exception {
        return billing.memberships(Caller.name());
    }

    @Override
    public MembershipDto join(JoinDto body) throws Exception {
        return billing.join(Caller.name(), body);
    }

    @Override
    public MembershipDto freeze(String id, FreezeDto body) throws Exception {
        return billing.freeze(Caller.name(), id, body);
    }

    @Override
    public MembershipDto unfreeze(String id) throws Exception {
        return billing.unfreeze(Caller.name(), id);
    }

    @Override
    public MembershipDto cancel(String id) throws Exception {
        return billing.cancel(Caller.name(), id);
    }

    @Override
    public List<InvoiceDto> invoices() throws Exception {
        return billing.invoices(Caller.name());
    }

    @Override
    public InvoiceDto pay(String id, PayInvoiceDto body) throws Exception {
        return billing.pay(Caller.name(), id, body == null ? null : body.methodId());
    }
}
