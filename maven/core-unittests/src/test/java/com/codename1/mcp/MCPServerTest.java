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
package com.codename1.mcp;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.io.JSONParser;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Verifies MCP protocol conformance (JSON-RPC 2.0 framing, capabilities handshake,
/// error codes) and end to end user interface driving through the accessibility
/// semantics tree. Runs on the Codename One EDT via {@link FormTest}.
class MCPServerTest extends UITestBase {

    @FormTest
    void initializeReportsProtocolCapabilitiesAndServerInfo() throws Exception {
        MCPServer server = new MCPServer();
        Map<String, Object> response = parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                        + "\"params\":{\"protocolVersion\":\"2024-11-05\"}}"));

        assertEquals("2.0", response.get("jsonrpc"));
        assertEquals(1, asInt(response.get("id")));
        Map<String, Object> result = asMap(response.get("result"));
        assertEquals("2024-11-05", result.get("protocolVersion"));
        Map<String, Object> capabilities = asMap(result.get("capabilities"));
        assertTrue(capabilities.containsKey("tools"));
        assertTrue(capabilities.containsKey("resources"));
        Map<String, Object> serverInfo = asMap(result.get("serverInfo"));
        assertNotNull(serverInfo.get("name"));
        assertNotNull(serverInfo.get("version"));
    }

    @FormTest
    void initializeDefaultsProtocolWhenClientOmitsIt() throws Exception {
        MCPServer server = new MCPServer();
        Map<String, Object> result = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")).get("result"));
        assertEquals(MCPServer.DEFAULT_PROTOCOL_VERSION, result.get("protocolVersion"));
    }

    @FormTest
    void initializeRejectsUnsupportedProtocolVersionAndOffersOurOwn() throws Exception {
        MCPServer server = new MCPServer();
        Map<String, Object> result = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                        + "\"params\":{\"protocolVersion\":\"2099-01-01\"}}")).get("result"));
        // MCP requires an unsupported request to be answered with a version the server
        // implements, not the version the client asked for.
        assertEquals(MCPServer.DEFAULT_PROTOCOL_VERSION, result.get("protocolVersion"));
    }

    @FormTest
    void toolArgumentsPreserveJsonTypes() throws Exception {
        MCPServer server = new MCPServer();
        server.addTool(new Tool("echo", "echoes arguments",
                "{\"type\":\"object\",\"properties\":{}}", echoHandler()));

        Map<String, Object> result = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"echo\","
                        + "\"arguments\":{\"flag\":true,\"count\":1,\"nothing\":null}}}")).get("result"));
        String echoed = (String) asMap(asList(result.get("content")).get(0)).get("text");
        String compact = echoed.replaceAll("\\s+", "");
        // The developer tool must receive booleans, integers and nulls unchanged, not the
        // string "true", the float 1.0, or a dropped null that the default parser produces.
        assertTrue(compact.indexOf("\"flag\":true") >= 0, echoed);
        assertTrue(compact.indexOf("\"count\":1") >= 0 && compact.indexOf("\"count\":1.0") < 0, echoed);
        assertTrue(compact.indexOf("\"nothing\":null") >= 0, echoed);
    }

    @FormTest
    void toolsListIncludesBuiltInAndDeveloperTools() throws Exception {
        MCPServer server = new MCPServer();
        server.addTool(new Tool("current_user", "Returns the user",
                "{\"type\":\"object\",\"properties\":{}}", echoHandler()));

        Map<String, Object> result = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}")).get("result"));
        List<Object> tools = asList(result.get("tools"));
        assertTrue(toolNames(tools).contains("ui_snapshot"));
        assertTrue(toolNames(tools).contains("ui_perform_action"));
        assertTrue(toolNames(tools).contains("current_user"));

        Map<String, Object> uiSnapshot = toolByName(tools, "ui_snapshot");
        assertNotNull(uiSnapshot.get("description"));
        assertNotNull(uiSnapshot.get("inputSchema"));
        assertTrue(asMap(uiSnapshot.get("inputSchema")).containsKey("type"));
    }

    @FormTest
    void toolsCallDispatchesToDeveloperHandler() throws Exception {
        MCPServer server = new MCPServer();
        server.addTool(new Tool("echo", "echoes arguments",
                "{\"type\":\"object\",\"properties\":{}}", echoHandler()));

        Map<String, Object> result = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"echo\",\"arguments\":{\"hello\":\"world\"}}}")).get("result"));

        assertFalse(asBool(result.get("isError")));
        List<Object> content = asList(result.get("content"));
        Map<String, Object> first = asMap(content.get(0));
        assertEquals("text", first.get("type"));
        assertTrue(((String) first.get("text")).indexOf("world") >= 0);
    }

    @FormTest
    void toolFailureIsReportedAsIsErrorResult() throws Exception {
        MCPServer server = new MCPServer();
        server.addTool(new Tool("boom", "always fails",
                "{\"type\":\"object\",\"properties\":{}}", new ToolHandler() {
                    @Override
                    public String invoke(String argumentsJson) throws Exception {
                        throw new IllegalStateException("kaboom");
                    }
                }));

        Map<String, Object> result = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"boom\",\"arguments\":{}}}")).get("result"));
        assertTrue(asBool(result.get("isError")));
        assertTrue(((String) asMap(asList(result.get("content")).get(0)).get("text")).indexOf("kaboom") >= 0);
    }

    @FormTest
    void unknownMethodReturnsMethodNotFound() throws Exception {
        MCPServer server = new MCPServer();
        Map<String, Object> error = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"does/notExist\"}")).get("error"));
        assertEquals(MCPServer.METHOD_NOT_FOUND, asInt(error.get("code")));
    }

    @FormTest
    void unknownToolReturnsMethodNotFound() throws Exception {
        MCPServer server = new MCPServer();
        Map<String, Object> error = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"nope\"}}")).get("error"));
        assertEquals(MCPServer.METHOD_NOT_FOUND, asInt(error.get("code")));
    }

    @FormTest
    void requestWithoutMethodReturnsInvalidRequest() throws Exception {
        MCPServer server = new MCPServer();
        Map<String, Object> error = asMap(parse(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":7}")).get("error"));
        assertEquals(MCPServer.INVALID_REQUEST, asInt(error.get("code")));
    }

    @FormTest
    void notificationsProduceNoResponse() {
        MCPServer server = new MCPServer();
        assertNull(server.handleMessage(
                "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
    }

    @FormTest
    void uiSnapshotExposesTheLiveForm() throws Exception {
        Form form = new Form("Login");
        Button save = new Button("Save");
        form.add(save);
        form.add(new TextField("hello"));
        form.show();

        MCPServer server = new MCPServer();
        String snapshotJson = callTool(server, "ui_snapshot", "{}");
        assertTrue(snapshotJson.indexOf("\"BUTTON\"") >= 0);
        assertTrue(snapshotJson.indexOf("Save") >= 0);
        assertTrue(snapshotJson.indexOf("\"activate\"") >= 0);
    }

    @FormTest
    void uiSetTextDrivesAnEditableField() throws Exception {
        Form form = new Form("Editor");
        TextField field = new TextField("old");
        form.add(field);
        form.show();

        MCPServer server = new MCPServer();
        long nodeId = firstNodeWithAction(callTool(server, "ui_snapshot", "{}"), "setText");
        assertTrue(nodeId >= 0, "expected an editable node exposing setText");

        Map<String, Object> result = parse(callTool(server, "ui_set_text",
                "{\"nodeId\":" + nodeId + ",\"text\":\"driven\"}"));
        assertTrue(asBool(result.get("success")));
        assertEquals("driven", field.getText());
    }

    @FormTest
    void uiActivateFiresTheComponentAction() throws Exception {
        Form form = new Form("Actions");
        Button button = new Button("Go");
        final boolean[] fired = new boolean[1];
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0] = true;
            }
        });
        form.add(button);
        form.show();

        MCPServer server = new MCPServer();
        long nodeId = firstNodeWithAction(callTool(server, "ui_snapshot", "{}"), "activate");
        assertTrue(nodeId >= 0, "expected a node exposing activate");
        Map<String, Object> result = parse(callTool(server, "ui_activate",
                "{\"nodeId\":" + nodeId + "}"));
        assertTrue(asBool(result.get("success")));
        assertTrue(fired[0], "activating the node should fire the button action");
    }

    @FormTest
    void uiFindLocatesNodesByLabel() throws Exception {
        Form form = new Form("Find");
        form.add(new Button("Unique Label"));
        form.show();

        MCPServer server = new MCPServer();
        String json = callTool(server, "ui_find", "{\"label\":\"unique\"}");
        // ui_find returns a JSON array; wrap it so the object-oriented parser can read it.
        List<Object> matches = asList(parse("{\"m\":" + json + "}").get("m"));
        assertNotNull(matches);
        assertTrue(matches.size() >= 1);
        assertEquals("Unique Label", asMap(matches.get(0)).get("label"));
    }

    // ---- helpers ----------------------------------------------------------

    private String callTool(MCPServer server, String name, String argumentsJson) throws Exception {
        String line = "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"" + name + "\",\"arguments\":" + argumentsJson + "}}";
        Map<String, Object> result = asMap(parse(server.handleMessage(line)).get("result"));
        assertFalse(asBool(result.get("isError")), "tool " + name + " reported an error");
        return (String) asMap(asList(result.get("content")).get(0)).get("text");
    }

    private long firstNodeWithAction(String snapshotJson, String actionId) throws Exception {
        Map<String, Object> tree = parse(snapshotJson);
        List<Object> nodes = asList(tree.get("nodes"));
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> node = asMap(nodes.get(i));
            List<Object> actions = asList(node.get("actions"));
            if (actions != null) {
                for (int a = 0; a < actions.size(); a++) {
                    if (actionId.equals(asMap(actions.get(a)).get("id"))) {
                        return asLong(node.get("id"));
                    }
                }
            }
        }
        return -1;
    }

    private ToolHandler echoHandler() {
        return new ToolHandler() {
            @Override
            public String invoke(String argumentsJson) {
                return argumentsJson;
            }
        };
    }

    private java.util.List<String> toolNames(List<Object> tools) {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (int i = 0; i < tools.size(); i++) {
            names.add((String) asMap(tools.get(i)).get("name"));
        }
        return names;
    }

    private Map<String, Object> toolByName(List<Object> tools, String name) {
        for (int i = 0; i < tools.size(); i++) {
            Map<String, Object> tool = asMap(tools.get(i));
            if (name.equals(tool.get("name"))) {
                return tool;
            }
        }
        return null;
    }

    private Map<String, Object> parse(String json) throws Exception {
        return JSONParser.parseJSON(json);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object o) {
        return (List<Object>) o;
    }

    private int asInt(Object o) {
        return ((Number) o).intValue();
    }

    private long asLong(Object o) {
        return ((Number) o).longValue();
    }

    /// The CN1 JSON parser returns booleans as strings by default, so accept either.
    private boolean asBool(Object o) {
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        return "true".equals(String.valueOf(o));
    }
}
