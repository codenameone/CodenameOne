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
package com.codenameone.examples.bazaar.automation;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AuditDto;
import com.codenameone.examples.bazaar.api.AutomationApiServer;
import com.codenameone.examples.bazaar.api.AutomationDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;
import com.codenameone.examples.bazaar.api.ValidationDto;

import java.util.List;

/// The server's half of `AutomationApi`; [Automations] checks the caller.
@Component
public class AutomationEndpoint implements AutomationApiServer {
    private final Automations automations;

    public AutomationEndpoint(Automations automations) {
        this.automations = automations;
    }

    @Override
    public List<NodeTypeDto> catalog(String storeId) throws Exception {
        return automations.catalog(Caller.name(), storeId);
    }

    @Override
    public List<AutomationDto> list(String storeId) throws Exception {
        return automations.list(Caller.name(), storeId);
    }

    @Override
    public AutomationDto get(String storeId, String automationId) throws Exception {
        return automations.get(Caller.name(), storeId, automationId);
    }

    @Override
    public AutomationDto create(String storeId, AutomationDto automation) throws Exception {
        return automations.create(Caller.name(), storeId, automation);
    }

    @Override
    public AutomationDto save(String storeId, String automationId, AutomationDto automation)
            throws Exception {
        return automations.save(Caller.name(), storeId, automationId, automation);
    }

    @Override
    public List<AutomationDto> remove(String storeId, String automationId) throws Exception {
        return automations.remove(Caller.name(), storeId, automationId);
    }

    @Override
    public ValidationDto validate(String storeId, AutomationDto automation) throws Exception {
        return automations.validate(Caller.name(), storeId, automation);
    }

    @Override
    public List<AuditDto> activity(String storeId, String automationId) throws Exception {
        return automations.activity(Caller.name(), storeId, automationId);
    }
}
