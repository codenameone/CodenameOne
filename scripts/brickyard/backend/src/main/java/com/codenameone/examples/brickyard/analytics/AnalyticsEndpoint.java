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
package com.codenameone.examples.brickyard.analytics;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.AnalyticsApiServer;
import com.codenameone.examples.brickyard.api.DayCountDto;
import com.codenameone.examples.brickyard.api.ErrorGroupDto;
import com.codenameone.examples.brickyard.api.EventDto;
import com.codenameone.examples.brickyard.api.RetentionDto;
import com.codenameone.examples.brickyard.api.RevenueDto;
import java.util.List;

/// The analytics routes; the work is in [Analytics].
@Component
public class AnalyticsEndpoint implements AnalyticsApiServer {
    private final Analytics analytics;

    public AnalyticsEndpoint(Analytics analytics) {
        this.analytics = analytics;
    }

    @Override
    public EventDto ingest(EventDto event) throws Exception {
        return analytics.ingest(event);
    }

    @Override
    public List<DayCountDto> visits(String experienceId, int days) throws Exception {
        return analytics.visits(experienceId, days);
    }

    @Override
    public RetentionDto retention(String experienceId) throws Exception {
        return analytics.retention(experienceId);
    }

    @Override
    public List<RevenueDto> revenue(String experienceId) throws Exception {
        return analytics.revenue(experienceId);
    }

    @Override
    public List<ErrorGroupDto> errors(String experienceId) throws Exception {
        return analytics.errors(experienceId);
    }
}
