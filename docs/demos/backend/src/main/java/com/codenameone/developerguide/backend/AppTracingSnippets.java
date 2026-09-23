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

import com.codename1.annotations.OpenTelemetry;
import com.codename1.system.Lifecycle;
import com.codename1.telemetry.Telemetry;
import com.codename1.telemetry.TelemetryConfig;

/// The app tracing chapter's examples, compiled so they cannot drift. They live in
/// this module, beside the server's, because it compiles against the core and runs
/// no annotation processing: in an app `@OpenTelemetry` is a switch the build acts
/// on, and two of them in one app are an error.
public final class AppTracingSnippets {

    private AppTracingSnippets() {
    }

    public static final class Relay {
// tag::app-otel-relay[]
@OpenTelemetry(relay = "https://api.example.com",
        serviceName = "shop-app")
public static class ShopApp extends Lifecycle {
}
// end::app-otel-relay[]
    }

    public static final class Direct {
// tag::app-otel-direct[]
@OpenTelemetry(
        endpoint = "https://abc12345.live.dynatrace.com/api/v2/otlp",
        headers = "Authorization: Api-Token dt0c01.XXXX")
public static class ShopApp extends Lifecycle {
}
// end::app-otel-direct[]
    }

    /// Stands in for whatever the app's checkout does.
    public static final class Cart {
        public void submit() {
        }
    }

    public static void timeAnAction(Cart cart) {
// tag::app-otel-run[]
Telemetry.run("checkout", () -> cart.submit());
// end::app-otel-run[]
    }

    public static void installInCode() {
// tag::app-otel-install[]
Telemetry.install(new TelemetryConfig()
        .relay("https://api.example.com")
        .serviceName("shop-app")
        .sampleRatio(0.2));
// end::app-otel-install[]
    }
}
