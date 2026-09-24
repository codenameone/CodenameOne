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
// Guide samples for server stacks that are not Codename One. This directory is
// NOT a Maven module and nothing builds it: the guide's snippet validator skips
// includes under src/main/java, so these carry no dependencies and are read
// rather than compiled.

package com.codenameone.developerguide.serverframeworks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** MicroProfile / Jakarta REST receiver for the delivery-feedback digest. */
// tag::push-notifications-java-microprofile[]
@Path("/push")
@ApplicationScoped
public class PushFeedbackMicroProfile {

    // Injected, not a method on this bean: CDI interceptors are proxy-based
    // exactly like Spring's, so this.apply(...) would never reach the
    // @Transactional interceptor and the guarantee would be silently absent.
    @Inject
    PushFeedbackCdiApplier applier;

    // MicroProfile Config, so the secret arrives the same way as every other
    // deployment value rather than as a constant in the source.
    @Inject
    @ConfigProperty(name = "cn1.push.callback.secret")
    String secret;

    // byte[] entity, for the reason it is byte[] everywhere else: a provider
    // that binds this to a POJO reads the bytes, and the re-serialised form no
    // longer matches the signature.
    @POST
    @Path("/feedback")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response feedback(@HeaderParam("X-CN1-Signature") String signature, byte[] raw)
            throws Exception {
        String body = new String(raw, StandardCharsets.UTF_8);
        // The verification is protocol, not framework: HMAC-SHA256 over
        // "<t>.<raw body>", constant-time compare, timestamp inside the window.
        if (!PushFeedbackSpring.verified(signature, body,
                secret.getBytes(StandardCharsets.UTF_8))) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("bad signature").build();
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject digest = reader.readObject();
            JsonValue events = digest.get("events");
            if (events != null) {
                for (JsonValue value : events.asJsonArray()) {
                    applier.apply(value.asJsonObject());
                }
            }
        }
        return Response.ok("ok").build();
    }
}

@ApplicationScoped
class PushFeedbackCdiApplier {

    @Inject
    DeviceStore store;

    /** Marker and deletion in one transaction, as in the Spring receiver. */
    @Transactional
    public void apply(JsonObject event) {
        String deliveryId = event.getString("deliveryId", null);
        if (store.alreadyApplied(deliveryId)) {
            return;
        }
        if ("INVALID_TARGET".equals(event.getString("reason", null))) {
            String key = event.containsKey("token")
                    ? event.getString("token")
                    : event.getString("endpoint", null);
            store.removeKey(key);
        }
        store.markApplied(deliveryId);
    }
}
// end::push-notifications-java-microprofile[]
