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
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.util.Base64;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A Model Context Protocol server. Speaks JSON-RPC 2.0 over a pluggable
/// {@link MCPTransport}, dispatching the MCP methods `initialize`, `tools/list`,
/// `tools/call`, `resources/list`, `resources/read`, `ping` and the
/// `notifications/*` family.
///
/// Tools are {@link com.codename1.ai.Tool} instances. The built in tools from
/// {@link McpUiTools} expose the accessibility semantics tree so any application is
/// drivable without code; applications add domain tools with {@link #addTool(Tool)}.
///
/// The message dispatch is thread free and reentrant: {@link #handleMessage(String)}
/// takes one request line and returns one response line (or null for a notification),
/// which makes it directly unit testable. {@link #start(MCPTransport)} wraps that in a
/// reader thread over the transport.
public class MCPServer {
    /// Standard JSON-RPC and MCP error codes.
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int RESOURCE_NOT_FOUND = -32002;

    /// MCP protocol revision advertised when the client does not request one.
    public static final String DEFAULT_PROTOCOL_VERSION = "2024-11-05";

    private static final String SCREEN_RESOURCE_URI = "cn1://screen.png";

    private final Map<String, Tool> tools = new LinkedHashMap<String, Tool>();
    private String serverName = "Codename One MCP";
    private String serverVersion = "1.0";
    private boolean screenshotEnabled = true;
    private volatile boolean running;
    private MCPTransport transport;
    private Thread readerThread;

    public MCPServer() {
        List<Tool> builtIn = McpUiTools.builtInTools();
        for (Tool t : builtIn) {
            tools.put(t.getName(), t);
        }
    }

    /// Registers a developer defined tool, replacing any existing tool with the same
    /// name. This is how an application publishes domain specific data and actions.
    public synchronized void addTool(Tool tool) {
        if (tool != null) {
            tools.put(tool.getName(), tool);
        }
    }

    /// Removes a previously registered tool by name.
    public synchronized void removeTool(String name) {
        tools.remove(name);
    }

    /// Sets the server identity reported to the host during `initialize`.
    public void setServerInfo(String name, String version) {
        if (name != null) {
            this.serverName = name;
        }
        if (version != null) {
            this.serverVersion = version;
        }
    }

    /// Enables or disables the built in screenshot resource. Enabled by default.
    public void setScreenshotEnabled(boolean screenshotEnabled) {
        this.screenshotEnabled = screenshotEnabled;
    }

    public boolean isRunning() {
        return running;
    }

    /// Starts serving over the given transport on a dedicated daemon reader thread.
    public synchronized void start(MCPTransport transport) {
        if (running) {
            return;
        }
        this.transport = transport;
        running = true;
        readerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runLoop();
            }
        }, "cn1-mcp-server");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /// Stops serving and closes the transport.
    public synchronized void stop() {
        running = false;
        if (transport != null) {
            transport.close();
        }
    }

    private void runLoop() {
        try {
            transport.open();
        } catch (IOException ex) {
            Log.e(ex);
            running = false;
            return;
        }
        while (running) {
            String line;
            try {
                line = transport.readMessage();
            } catch (IOException ex) {
                break;
            }
            if (line == null) {
                break;
            }
            if (line.trim().length() == 0) {
                continue;
            }
            String response = handleMessage(line);
            if (response != null) {
                try {
                    transport.writeMessage(response);
                } catch (IOException ex) {
                    break;
                }
            }
        }
        running = false;
        transport.close();
    }

    /// Handles one inbound JSON-RPC message and returns the response line, or null
    /// when the message is a notification that warrants no reply. Never throws; every
    /// failure is turned into a JSON-RPC error response.
    public String handleMessage(String line) {
        Map<String, Object> request;
        try {
            request = JSONParser.parseJSON(line);
        } catch (Exception ex) {
            return errorEnvelope(null, PARSE_ERROR, "Parse error");
        }
        if (request == null) {
            return errorEnvelope(null, PARSE_ERROR, "Parse error");
        }
        boolean hasId = request.containsKey("id");
        Object id = request.get("id");
        try {
            String method = JSONParser.getString(request, "method");
            Map<String, Object> params = JSONParser.asMap(request.get("params"));
            if (method == null) {
                return hasId ? errorEnvelope(id, INVALID_REQUEST, "Invalid Request") : null;
            }
            if (!hasId) {
                handleNotification(method, params);
                return null;
            }
            return dispatch(id, method, params);
        } catch (Exception ex) {
            return errorEnvelope(hasId ? id : null, INTERNAL_ERROR,
                    "Internal error: " + messageOf(ex));
        }
    }

    private String dispatch(Object id, String method, Map<String, Object> params) throws Exception {
        if ("initialize".equals(method)) {
            return resultEnvelope(id, initializeResult(params));
        }
        if ("ping".equals(method)) {
            return resultEnvelope(id, new LinkedHashMap<String, Object>());
        }
        if ("tools/list".equals(method)) {
            return resultEnvelope(id, toolsList());
        }
        if ("tools/call".equals(method)) {
            return toolsCall(id, params);
        }
        if ("resources/list".equals(method)) {
            return resultEnvelope(id, resourcesList());
        }
        if ("resources/read".equals(method)) {
            return resourcesRead(id, params);
        }
        return errorEnvelope(id, METHOD_NOT_FOUND, "Method not found: " + method);
    }

    private void handleNotification(String method, Map<String, Object> params) {
        // notifications/initialized and cancellation notices need no action here.
    }

    private Map<String, Object> initializeResult(Map<String, Object> params) {
        String protocol = params == null ? null : JSONParser.getString(params, "protocolVersion");
        if (protocol == null || protocol.length() == 0) {
            protocol = DEFAULT_PROTOCOL_VERSION;
        }
        Map<String, Object> capabilities = new LinkedHashMap<String, Object>();
        capabilities.put("tools", new LinkedHashMap<String, Object>());
        capabilities.put("resources", new LinkedHashMap<String, Object>());
        Map<String, Object> serverInfo = new LinkedHashMap<String, Object>();
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("protocolVersion", protocol);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return result;
    }

    private synchronized Map<String, Object> toolsList() {
        List<Object> list = new ArrayList<Object>();
        for (Tool tool : tools.values()) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("name", tool.getName());
            entry.put("description", tool.getDescription());
            entry.put("inputSchema", JSONParser.rawJson(tool.getParametersJsonSchema()));
            list.add(entry);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tools", list);
        return result;
    }

    private String toolsCall(Object id, Map<String, Object> params) {
        String name = params == null ? null : JSONParser.getString(params, "name");
        if (name == null || name.length() == 0) {
            return errorEnvelope(id, INVALID_PARAMS, "Missing tool name");
        }
        Tool tool;
        synchronized (this) {
            tool = tools.get(name);
        }
        if (tool == null) {
            return errorEnvelope(id, METHOD_NOT_FOUND, "Unknown tool: " + name);
        }
        Object argObj = params.get("arguments");
        String argumentsJson = argObj == null ? "{}" : JSONParser.toJson(argObj);
        String text;
        boolean isError;
        try {
            text = tool.invoke(argumentsJson);
            isError = false;
        } catch (Exception ex) {
            text = messageOf(ex);
            isError = true;
        }
        if (text == null) {
            text = "";
        }
        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("type", "text");
        content.put("text", text);
        List<Object> contentList = new ArrayList<Object>();
        contentList.add(content);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("content", contentList);
        result.put("isError", Boolean.valueOf(isError));
        return resultEnvelope(id, result);
    }

    private Map<String, Object> resourcesList() {
        List<Object> list = new ArrayList<Object>();
        if (screenshotEnabled) {
            Map<String, Object> screen = new LinkedHashMap<String, Object>();
            screen.put("uri", SCREEN_RESOURCE_URI);
            screen.put("name", "Current screen");
            screen.put("description", "A PNG screenshot of the current Codename One form.");
            screen.put("mimeType", "image/png");
            list.add(screen);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("resources", list);
        return result;
    }

    private String resourcesRead(Object id, Map<String, Object> params) {
        String uri = params == null ? null : JSONParser.getString(params, "uri");
        if (uri == null || uri.length() == 0) {
            return errorEnvelope(id, INVALID_PARAMS, "Missing resource uri");
        }
        if (screenshotEnabled && SCREEN_RESOURCE_URI.equals(uri)) {
            byte[] png = McpUiTools.screenshotPng();
            if (png == null) {
                return errorEnvelope(id, INTERNAL_ERROR, "No screen available to capture");
            }
            Map<String, Object> content = new LinkedHashMap<String, Object>();
            content.put("uri", uri);
            content.put("mimeType", "image/png");
            content.put("blob", Base64.encodeNoNewline(png));
            List<Object> contents = new ArrayList<Object>();
            contents.add(content);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("contents", contents);
            return resultEnvelope(id, result);
        }
        return errorEnvelope(id, RESOURCE_NOT_FOUND, "Unknown resource: " + uri);
    }

    private String resultEnvelope(Object id, Map<String, Object> result) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + idJson(id)
                + ",\"result\":" + JSONParser.toJson(result) + "}";
    }

    private String errorEnvelope(Object id, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("code", Integer.valueOf(code));
        error.put("message", message);
        return "{\"jsonrpc\":\"2.0\",\"id\":" + idJson(id)
                + ",\"error\":" + JSONParser.toJson(error) + "}";
    }

    /// Serializes the JSON-RPC id, preserving integer form. The lenient CN1 parser reads
    /// `1` as a double, but the id must be echoed exactly so strict hosts can correlate
    /// the response, so an integral double is emitted without a fractional part.
    private static String idJson(Object id) {
        if (id instanceof Double || id instanceof Float) {
            double d = ((Number) id).doubleValue();
            if (!Double.isInfinite(d) && !Double.isNaN(d) && d == Math.floor(d)) {
                return Long.toString((long) d);
            }
        }
        return JSONParser.toJson(id);
    }

    private static String messageOf(Throwable ex) {
        String m = ex.getMessage();
        return m == null ? ex.getClass().getName() : m;
    }
}
