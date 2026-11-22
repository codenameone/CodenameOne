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
package com.codenameone.examples.bazaar.analytics;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AnalyticsApiServer;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.FunnelStepDto;
import com.codenameone.examples.bazaar.api.InsightsDto;

import java.util.List;

/// The server's half of `AnalyticsApi`; [Analytics] checks the caller.
@Component
public class AnalyticsEndpoint implements AnalyticsApiServer {
    private final Analytics analytics;

    public AnalyticsEndpoint(Analytics analytics) {
        this.analytics = analytics;
    }

    @Override
    public AnalyticsDto dashboard(String storeId, String from, String to) throws Exception {
        return analytics.dashboard(Caller.name(), storeId, millis(from), millis(to));
    }

    @Override
    public List<FunnelStepDto> funnel(String storeId, String from, String to) throws Exception {
        return analytics.funnel(Caller.name(), storeId, millis(from), millis(to));
    }

    @Override
    public InsightsDto insights(String storeId, String from, String to) throws Exception {
        return analytics.insights(Caller.name(), storeId, millis(from), millis(to));
    }

    static long millis(String text) {
        if (text == null || text.trim().length() == 0) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(text.trim()));
        } catch (NumberFormatException bad) {
            throw new ResponseStatusException(400, "A time is milliseconds since 1970");
        }
    }
}
