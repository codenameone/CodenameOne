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
package com.codenameone.examples.wayline.driving;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.wayline.Caller;
import com.codenameone.examples.wayline.api.DocumentContentDto;
import com.codenameone.examples.wayline.api.DocumentUploadDto;
import com.codenameone.examples.wayline.api.DriverApplicationDto;
import com.codenameone.examples.wayline.api.DrivingApiServer;

/// The server's half of `DrivingApi`.
///
/// Open to anyone signed in, which is why its paths are not under
/// `/api/driver`: applying is what someone does before they hold the DRIVER
/// role. What keeps one applicant out of another's application -- and out of
/// the identity documents in it -- is that no method here takes a name. Each
/// acts on the application of whoever is signed in, and there is no parameter
/// to change.
@Component
public class DrivingEndpoint implements DrivingApiServer {
    private final Applications applications;

    public DrivingEndpoint(Applications applications) {
        this.applications = applications;
    }

    @Override
    public DriverApplicationDto application() throws Exception {
        return applications.get(Caller.name());
    }

    @Override
    public DriverApplicationDto save(DriverApplicationDto application) throws Exception {
        return applications.save(Caller.name(), application);
    }

    @Override
    public DriverApplicationDto upload(DocumentUploadDto document) throws Exception {
        return applications.upload(Caller.name(), document);
    }

    @Override
    public DocumentContentDto document(String kind) throws Exception {
        return applications.document(Caller.name(), kind);
    }

    @Override
    public DriverApplicationDto submit() throws Exception {
        return applications.submit(Caller.name());
    }
}
