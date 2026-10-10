/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
