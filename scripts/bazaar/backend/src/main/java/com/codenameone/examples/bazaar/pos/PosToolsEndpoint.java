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
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.ApprovalDto;
import com.codenameone.examples.bazaar.api.ApprovalRequestDto;
import com.codenameone.examples.bazaar.api.CommissionDto;
import com.codenameone.examples.bazaar.api.ParkedCartDto;
import com.codenameone.examples.bazaar.api.PosGridDto;
import com.codenameone.examples.bazaar.api.PosPolicyDto;
import com.codenameone.examples.bazaar.api.PosToolsApiServer;
import com.codenameone.examples.bazaar.api.StaffRateDto;
import java.util.List;

/// The server's half of `PosToolsApi`.
@Component
public class PosToolsEndpoint implements PosToolsApiServer {
    private final PosTools tools;

    public PosToolsEndpoint(PosTools tools) {
        this.tools = tools;
    }

    @Override
    public PosPolicyDto policy(String storeId) throws Exception {
        return tools.policy(Caller.name(), storeId);
    }

    @Override
    public PosPolicyDto savePolicy(String storeId, PosPolicyDto body) throws Exception {
        return tools.savePolicy(Caller.name(), storeId, body);
    }

    @Override
    public List<StaffRateDto> staffRates(String storeId) throws Exception {
        return tools.staffRates(Caller.name(), storeId);
    }

    @Override
    public StaffRateDto saveStaffRate(String storeId, StaffRateDto body) throws Exception {
        return tools.saveStaffRate(Caller.name(), storeId, body);
    }

    @Override
    public List<CommissionDto> commissions(String storeId, String from, String to)
            throws Exception {
        return tools.commissions(Caller.name(), storeId, from, to);
    }

    @Override
    public ApprovalDto approve(String storeId, String registerId, ApprovalRequestDto body)
            throws Exception {
        return tools.approve(Caller.name(), storeId, registerId, body);
    }

    @Override
    public List<ParkedCartDto> parked(String storeId, String registerId) throws Exception {
        return tools.parked(Caller.name(), storeId, registerId);
    }

    @Override
    public ParkedCartDto park(String storeId, String registerId, ParkedCartDto body)
            throws Exception {
        return tools.park(Caller.name(), storeId, registerId, body);
    }

    @Override
    public ParkedCartDto resume(String storeId, String registerId, String parkedId)
            throws Exception {
        return tools.resume(Caller.name(), storeId, registerId, parkedId);
    }

    @Override
    public ParkedCartDto discard(String storeId, String registerId, String parkedId)
            throws Exception {
        return tools.discard(Caller.name(), storeId, registerId, parkedId);
    }

    @Override
    public PosGridDto grid(String storeId, String locationId) throws Exception {
        return tools.grid(Caller.name(), storeId, locationId);
    }

    @Override
    public PosGridDto saveGrid(String storeId, String locationId, PosGridDto body)
            throws Exception {
        return tools.saveGrid(Caller.name(), storeId, locationId, body);
    }
}
