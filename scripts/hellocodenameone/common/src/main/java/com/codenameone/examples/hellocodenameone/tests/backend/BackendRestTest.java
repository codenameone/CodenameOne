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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.io.rest.Rest;

import java.util.Map;

/// `com.codename1.io.rest.Rest` against the backend: every verb, query parameters
/// and headers, JSON in and out, HTTP Basic and bearer authentication, and an
/// error status delivered to its handler.
public class BackendRestTest extends BackendClientTest {
    @Override
    protected void defineSteps() {
        step(() -> {
            final int id = currentStep();
            Rest.get(url("/api/hello/Ada")).fetchAsString(r -> {
                if (expect(r.getResponseCode() == 200, "hello answered " + r.getResponseCode())
                        && expect("Hello, Ada".equals(r.getResponseData()),
                        "hello said " + r.getResponseData())) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            Rest.get(url("/api/echo")).queryParam("a", "1").queryParam("b", "two words")
                    .header("X-Test", "yes").acceptJson().fetchAsJsonMap(r -> {
                        Map echo = r.getResponseData();
                        Object params = field(echo, "params");
                        Object headers = field(echo, "headers");
                        if (expect("GET".equals(field(echo, "method")), "echo saw " + echo)
                                && expect("1".equals(field(params, "a")), "a was " + params)
                                && expect("two words".equals(field(params, "b")), "b was " + params)
                                && expect("yes".equals(field(headers, "X-Test")),
                                "X-Test was " + headers)) {
                            proceed(id);
                        }
                    });
        });
        step(() -> {
            final int id = currentStep();
            Rest.post(url("/api/echo")).jsonContent().body("{\"hello\":\"world\"}")
                    .fetchAsJsonMap(r -> {
                        Map echo = r.getResponseData();
                        Object type = field(field(echo, "headers"), "Content-Type");
                        if (expect("POST".equals(field(echo, "method")), "post echo " + echo)
                                && expect("{\"hello\":\"world\"}".equals(field(echo, "body")),
                                "post body " + field(echo, "body"))
                                && expect(String.valueOf(type).indexOf("application/json") >= 0,
                                "post content type " + type)) {
                            proceed(id);
                        }
                    });
        });
        verb("PUT");
        verb("PATCH");
        verb("DELETE");
        step(() -> {
            final int id = currentStep();
            Rest.get(url("/api/json/map")).acceptJson().fetchAsJsonMap(r -> {
                Map m = r.getResponseData();
                Object nested = field(m, "nested");
                if (expect("probe".equals(field(m, "title")), "title " + m)
                        && expect(number(field(m, "count")) == 3, "count " + field(m, "count"))
                        && expect(number(field(nested, "depth")) == 2, "nested " + nested)) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            Rest.get(url("/api/auth/basic")).basicAuth("user", "pass").fetchAsString(r -> {
                if (expect("user".equals(r.getResponseData()), "basic auth answered "
                        + r.getResponseCode() + " " + r.getResponseData())) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            Rest.get(url("/api/auth/bearer")).bearer("token-123").fetchAsString(r -> {
                if (expect(r.getResponseCode() == 200, "bearer answered " + r.getResponseCode())) {
                    proceed(id);
                }
            });
        });
        step(() -> {
            final int id = currentStep();
            Rest.get(url("/api/status/418")).onErrorCodeString(err -> {
                if (expect(err.getResponseCode() == 418, "error handler got "
                        + err.getResponseCode())) {
                    proceed(id);
                }
            }).fetchAsString(r -> expect(false, "a 418 reached the success callback: "
                    + r.getResponseCode()));
        });
    }

    /// One verb against the echo route, checking the server saw it.
    private void verb(final String method) {
        step(() -> {
            final int id = currentStep();
            com.codename1.io.rest.RequestBuilder builder = "PUT".equals(method)
                    ? Rest.put(url("/api/echo")) : "PATCH".equals(method)
                    ? Rest.patch(url("/api/echo")) : Rest.delete(url("/api/echo"));
            if (!"DELETE".equals(method)) {
                builder.jsonContent().body("{\"verb\":\"" + method + "\"}");
            }
            builder.fetchAsJsonMap(r -> {
                Map echo = r.getResponseData();
                if (expect(method.equals(field(echo, "method")), method + " echo saw " + echo)) {
                    proceed(id);
                }
            });
        });
    }
}
