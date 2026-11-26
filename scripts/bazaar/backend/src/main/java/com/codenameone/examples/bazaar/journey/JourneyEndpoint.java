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
package com.codenameone.examples.bazaar.journey;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.JourneyApiServer;
import com.codenameone.examples.bazaar.api.JourneyDto;
import com.codenameone.examples.bazaar.api.JourneyNodeStatDto;
import com.codenameone.examples.bazaar.api.NodeTypeDto;

import java.util.List;

/// The server's half of `JourneyApi`.
@Component
public class JourneyEndpoint implements JourneyApiServer {
    private final Journeys journeys;

    public JourneyEndpoint(Journeys journeys) {
        this.journeys = journeys;
    }

    @Override
    public List<NodeTypeDto> catalog(String storeId) throws Exception {
        return journeys.catalog(Caller.name(), storeId);
    }

    @Override
    public List<JourneyDto> list(String storeId) throws Exception {
        return journeys.list(Caller.name(), storeId);
    }

    @Override
    public JourneyDto get(String storeId, String journeyId) throws Exception {
        return journeys.get(Caller.name(), storeId, journeyId);
    }

    @Override
    public JourneyDto create(String storeId, JourneyDto body) throws Exception {
        return journeys.save(Caller.name(), storeId, null, body);
    }

    @Override
    public JourneyDto save(String storeId, String journeyId, JourneyDto body) throws Exception {
        return journeys.save(Caller.name(), storeId, journeyId, body);
    }

    @Override
    public List<JourneyNodeStatDto> stats(String storeId, String journeyId) throws Exception {
        return journeys.stats(Caller.name(), storeId, journeyId);
    }
}
