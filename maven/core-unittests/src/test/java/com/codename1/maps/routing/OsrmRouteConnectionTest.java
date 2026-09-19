/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.io.ConnectionRequest;
import com.codename1.junit.UITestBase;
import com.codename1.maps.LatLng;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the request {@link OsrmRouteService} sends, as opposed to the URL it builds. Needs a
 * Display because constructing a {@link ConnectionRequest} asks the implementation whether the
 * access point API is supported.
 */
class OsrmRouteConnectionTest extends UITestBase {

    @BeforeEach
    void setUp() throws Exception {
        super.setUpDisplay();
    }

    @AfterEach
    void tearDown() throws Exception {
        ConnectionRequest.setDefaultUserAgent(null);
        super.tearDownDisplay();
    }

    @Test
    void routingRequestsCarryTheirOwnUserAgent() {
        // Issue #5854: the Android port answers Display.getProperty("User-Agent") with
        // System.getProperty("http.agent"), which is "Dalvik/2.1.0 (Linux; U; Android ...)", and
        // initDefaultUserAgent() makes that the default for every ConnectionRequest. The OSRM demo
        // server's nginx blocklists the "Dalvik/2.1.0" token and answers 403 to it, so keyless
        // routing was dead on every Android device while the simulator, which sends a Mozilla
        // string, was fine. The agent has to be the service's own rather than the platform default.
        ConnectionRequest.setDefaultUserAgent("Dalvik/2.1.0 (Linux; U; Android 10; Pixel Build/QQ)");

        ConnectionRequest con = newConnection();

        assertEquals(OsrmRouteService.USER_AGENT, con.getUserAgent());
        assertTrue(OsrmRouteService.USER_AGENT.indexOf("Dalvik") < 0, OsrmRouteService.USER_AGENT);
    }

    @Test
    void routingDoesNotParseAnErrorBody() {
        // handleErrorResponseCode already has the status and builds the whole message from it.
        // Reading the body as well runs JSONParser over nginx's HTML 403 page, which logs a
        // complaint per unexpected character -- that is what buried the real one-line reason under
        // a screenful of parser noise in the logcat of #5854.
        assertFalse(newConnection().isReadResponseForErrors());
    }

    @Test
    void routingRequestsAreGetRequests() {
        assertFalse(newConnection().isPost());
    }

    private ConnectionRequest newConnection() {
        OsrmRouteService service = new OsrmRouteService();
        RouteRequest request = new RouteRequest(new LatLng(38.8977, -77.0365),
                new LatLng(38.8894, -77.0352));
        ConnectionRequest con = service.newConnection(request, new RouteCallback() {
            @Override
            public void routesFound(List routes) {
            }

            @Override
            public void routeFailed(String message, Throwable error) {
            }
        });
        assertEquals(service.buildUrl(request), con.getUrl());
        return con;
    }
}
