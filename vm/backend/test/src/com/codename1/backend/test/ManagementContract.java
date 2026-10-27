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
package com.codename1.backend.test;

import java.io.IOException;

/// The operational contract of a server with management on, as tests a test
/// class inherits: implement it on a [BackendTest] with
/// [BackendTest.Feature#MANAGEMENT] and a port, and its default `@Test` methods
/// check the probes, the endpoint authentication, the metric schema and the
/// Prometheus scrape against that application.
///
/// ```java
/// @BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT,
///         features = BackendTest.Feature.MANAGEMENT,
///         properties = "cn1.management.token=s3cret")
/// class OperationsTest implements ManagementContract {
///     @Autowired TestRestTemplate rest;
///     public TestRestTemplate managementClient() { return rest; }
///     public String managementToken() { return "s3cret"; }
/// }
/// ```
///
/// The same class runs on the JVM and compiled, so a packaged server answers
/// a dashboard and a load balancer the way the JVM one does.
public interface ManagementContract {
    /// A client rooted at the application, a [TestRestTemplate] the test
    /// injected.
    TestRestTemplate managementClient();

    /// The `cn1.management.token` the test configured.
    String managementToken();

    /// The `cn1.management.path` the test configured.
    default String managementPath() {
        return "/manage";
    }

    /// The status `path` under the management path answers, with `token` as a
    /// bearer token, or with none when it is null.
    default int managementStatus(String path, String token) throws IOException {
        return managementGet(path, token).getStatusCodeValue();
    }

    /// The answer to `GET` of `path` under the management path.
    default ResponseEntity<String> managementGet(String path, String token) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return managementClient().exchange(managementPath() + path, HttpMethod.GET,
                new HttpEntity<Object>(headers), String.class);
    }

    /// Liveness and readiness answer a probe that carries no token, as an
    /// orchestrator's does.
    @org.junit.jupiter.api.Test
    default void probesNeedNoToken() throws IOException {
        ContractChecks.equal("liveness", 200, managementStatus("/health/liveness", null));
        ContractChecks.equal("readiness", 200, managementStatus("/health/readiness", null));
        ContractChecks.equal("health", 200, managementStatus("/health", null));
    }

    /// Everything else refuses a missing or a wrong token and serves the right one.
    @org.junit.jupiter.api.Test
    default void protectedEndpointsNeedTheToken() throws IOException {
        String[] paths = {"/schema", "/metrics", "/prometheus"};
        for (String path : paths) {
            ContractChecks.equal(path + " without a token", 401, managementStatus(path, null));
            ContractChecks.equal(path + " with a wrong token", 401,
                    managementStatus(path, managementToken() + "-wrong"));
            ContractChecks.equal(path + " with the token", 200,
                    managementStatus(path, managementToken()));
        }
    }

    /// The schema names the runtime and describes each metric with its unit and
    /// whether this server measures it.
    @org.junit.jupiter.api.Test
    default void theSchemaDescribesItsMetrics() throws IOException {
        String body = managementGet("/schema", managementToken()).getBody();
        ContractChecks.check("no runtime in " + body, body.contains("\"runtime\""));
        String[] always = {"http.server.request.duration", "http.server.active_requests",
            "process.runtime.memory.used", "process.uptime"};
        for (String name : always) {
            ContractChecks.check(name + " missing from the schema", body.contains("\"" + name + "\""));
        }
        ContractChecks.check("no units in the schema", body.contains("\"unit\""));
        ContractChecks.check("no availability in the schema", body.contains("\"available\""));
    }

    /// The scrape types its families and carries the metrics every runtime has.
    @org.junit.jupiter.api.Test
    default void prometheusScrapeIsTyped() throws IOException {
        String body = managementGet("/prometheus", managementToken()).getBody();
        ContractChecks.check("untyped scrape", body.contains("# TYPE "));
        ContractChecks.check("no uptime in the scrape", body.contains("process_uptime"));
        ContractChecks.check("no active requests in the scrape",
                body.contains("http_server_active_requests"));
    }
}
