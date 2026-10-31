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
package com.codenameone.examples.pulse.studio;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.pulse.Caller;
import com.codenameone.examples.pulse.api.HouseholdApiServer;
import com.codenameone.examples.pulse.api.MemberDto;
import com.codenameone.examples.pulse.api.NewMemberDto;
import com.codenameone.examples.pulse.api.PassDto;

import java.util.List;

/// The server's half of `HouseholdApi`: always the caller's own household.
@Component
public class HouseholdEndpoint implements HouseholdApiServer {
    private final Household household;

    public HouseholdEndpoint(Household household) {
        this.household = household;
    }

    @Override
    public List<MemberDto> members() throws Exception {
        return household.list(Caller.name());
    }

    @Override
    public MemberDto addChild(NewMemberDto child) throws Exception {
        return household.addChild(Caller.name(), child);
    }

    @Override
    public MemberDto signWaiver(String id) throws Exception {
        return household.signWaiver(Caller.name(), id);
    }

    @Override
    public PassDto pass(String id) throws Exception {
        return household.pass(Caller.name(), id);
    }

    @Override
    public PassDto rotatePass(String id) throws Exception {
        return household.rotate(Caller.name(), id);
    }
}
