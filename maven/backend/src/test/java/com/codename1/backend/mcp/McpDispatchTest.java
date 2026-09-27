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
package com.codename1.backend.mcp;

import com.codename1.backend.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpDispatchTest {
    @Test
    @DisplayName("an explicit null JSON-RPC id is a request, answered; an absent one is not")
    void aNullIdIsNotANotification() throws Exception {
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null, null);
        Map ping = new LinkedHashMap();
        ping.put("jsonrpc", "2.0");
        ping.put("id", null);
        ping.put("method", "ping");
        Object answer = server.dispatch(ping);
        assertNotNull(answer, "a request with id null got no answer");
        assertTrue(((Map) answer).containsKey("id") && ((Map) answer).get("id") == null);
        ping.remove("id");
        assertNull(server.dispatch(ping), "a notification was answered");
    }

    @Test
    @DisplayName("a notification runs its method; only the answer is withheld")
    void aNotificationStillRuns() throws Exception {
        final int[] calls = {0};
        McpTool tool = new McpTool() {
            public String name() {
                return "touch";
            }

            public String description() {
                return "counts calls";
            }

            public Map inputSchema() {
                Map schema = new LinkedHashMap();
                schema.put("type", "object");
                return schema;
            }

            public Object call(Map arguments) {
                calls[0]++;
                return "ok";
            }
        };
        Properties settings = new Properties();
        settings.setProperty(McpServer.ENABLED, "true");
        McpServer server = McpServer.fromConfig(Config.of(settings, "dev"), null, null,
                java.util.Collections.singletonList(tool));
        Map call = new LinkedHashMap();
        call.put("jsonrpc", "2.0");
        call.put("method", "tools/call");
        Map params = new LinkedHashMap();
        params.put("name", "touch");
        params.put("arguments", new LinkedHashMap());
        call.put("params", params);
        assertNull(server.dispatch(call), "a notification was answered");
        org.junit.jupiter.api.Assertions.assertEquals(1, calls[0],
                "an id-less tools/call never ran its tool");
    }
}
