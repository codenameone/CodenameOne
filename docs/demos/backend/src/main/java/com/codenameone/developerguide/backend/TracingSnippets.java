/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.backend;

import com.codename1.backend.Config;
import com.codename1.backend.Tracing;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.OpenTelemetry;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.otel.OtlpTracer;

/// The Backend chapter's tracing examples, compiled so they cannot drift. This
/// module runs no annotation processing, which is what lets `@OpenTelemetry` appear
/// here as an example: in a server module it is a switch the build acts on.
public final class TracingSnippets {

    private TracingSnippets() {
    }

    public static final class Server {
// tag::backend-otel-annotation[]
@OpenTelemetry(serviceName = "notes")
@RestController
public static class NotesController {
    @GetMapping("/notes/{id}")
    public String note(@PathVariable("id") String id) {
        return id;
    }
}
// end::backend-otel-annotation[]
    }

    public static void installByHand() throws Exception {
// tag::backend-otel-install[]
Tracing.install(OtlpTracer.open(Config.load(), "notes"));
// end::backend-otel-install[]
    }

}
