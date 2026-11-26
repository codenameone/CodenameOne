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
package com.codenameone.examples.bazaar.platform;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BulkApiServer;
import com.codenameone.examples.bazaar.api.BulkJobDto;

import java.util.List;

/// The server's half of `BulkApi`; [BulkJobs] checks the caller.
@Component
public class BulkEndpoint implements BulkApiServer {
    private final BulkJobs jobs;

    public BulkEndpoint(BulkJobs jobs) {
        this.jobs = jobs;
    }

    @Override
    public List<BulkJobDto> list(String storeId) throws Exception {
        return jobs.list(Caller.name(), storeId);
    }

    @Override
    public BulkJobDto start(String storeId, BulkJobDto job) throws Exception {
        return jobs.startAsStaff(Caller.name(), storeId, job);
    }

    @Override
    public BulkJobDto job(String storeId, String jobId) throws Exception {
        return jobs.job(Caller.name(), storeId, jobId);
    }
}
