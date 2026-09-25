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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Traces this server with OpenTelemetry.
///
/// Put it on any class in the backend module -- a controller is the natural
/// place. The build then wires a tracer into the entry point it generates, and
/// from then on every request, every outbound `Web` call and every `Database`
/// statement is a span, exported over OTLP/HTTP to the collector the deployment
/// names:
///
/// ```java
/// @OpenTelemetry(serviceName = "orders")
/// @RestController
/// public class OrderController { ... }
/// ```
///
/// ```
/// OTEL_EXPORTER_OTLP_ENDPOINT=https://collector.internal:4318 ./server
/// ```
///
/// Nothing else changes. An incoming `traceparent` makes the request part of the
/// caller's trace -- which is how a Codename One app's own spans connect to the
/// server's -- and the W3C trace context is sent on every outbound request.
///
/// A project that would rather not touch its source can set
/// `cn1.otel.enabled=true` in `application.properties` instead; the effect is
/// the same.
///
/// THIS IS A BUILD-TIME SWITCH, and deliberately so. Without it the tracer is
/// never referenced, so the translator leaves it out of the binary entirely. With
/// it, the deployment still has the last word: `OTEL_SDK_DISABLED=true` turns
/// tracing off at start-up without a rebuild.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface OpenTelemetry {
    /// The `service.name` spans are reported under, unless `OTEL_SERVICE_NAME`
    /// or `cn1.otel.service.name` names another. Empty means `unknown_service`,
    /// which is what every OpenTelemetry SDK reports when nobody said.
    String serviceName() default "";
}
