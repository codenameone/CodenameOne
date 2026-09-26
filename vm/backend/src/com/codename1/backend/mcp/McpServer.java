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
package com.codename1.backend.mcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;

/**
 * The server's Model Context Protocol endpoint: JSON-RPC over MCP's Streamable
 * HTTP transport, at {@code cn1.mcp.path} ({@code /mcp} by default).
 *
 * <p>It serves two kinds of tools. The application's own -- every
 * {@code @McpTool} method, registered by the generated entry point -- and, on a
 * development profile of a development build, the {@link DevTools}: the routes,
 * the beans, the database, the jobs and the metrics of the running server, so an
 * agent building the backend can inspect and exercise it.
 *
 * <pre>
 *   claude mcp add --transport http backend http://127.0.0.1:8080/mcp
 * </pre>
 *
 * <h2>Security</h2>
 *
 * <p>Outside a development profile the endpoint needs
 * {@code Authorization: Bearer <cn1.mcp.token>} and the server refuses to start
 * without a token, because a tool anyone can call is a vulnerability. On a
 * development profile the token is optional. Either way a request whose
 * {@code Origin} is not a loopback address or one listed in
 * {@code cn1.mcp.allowedOrigins} is refused -- including a page the server
 * serves itself, which has to be listed -- which is what the MCP
 * specification asks for against DNS rebinding: a web page in the developer's
 * browser must not be able to drive the server.
 *
 * <p>The endpoint answers POSTed JSON-RPC with a JSON body. It keeps no session
 * and opens no event stream, so a GET is answered 405, as the transport allows.
 */
public final class McpServer implements HttpServer.Handler {
    public static final String ENABLED = "cn1.mcp.enabled";
    public static final String PATH = "cn1.mcp.path";
    public static final String TOKEN = "cn1.mcp.token";
    public static final String ALLOWED_ORIGINS = "cn1.mcp.allowedOrigins";
    public static final String DEV_TOOLS = "cn1.mcp.devTools";

    /** The newest protocol revision this server speaks, and the ones it accepts. */
    static final String[] PROTOCOL_VERSIONS = {"2025-06-18", "2025-03-26", "2024-11-05"};

    /**
     * This endpoint's tools. Per server, never per process: a server started
     * again in the same process -- with its development tools off, or a
     * conditional @McpTool bean inactive -- must not keep serving the tools of
     * one that stopped, bound to beans that have been destroyed.
     */
    private final List tools = new ArrayList();

    private final String path;
    private final byte[] token;
    private final String[] allowedOrigins;
    private final String serverName;
    private Backend backend;

    /** Extra tools installed when the server starts; the development tools are one. */
    public interface Extension {
        /** Registers tools, given the running server. */
        void install(McpServer server, Backend backend);
    }

    private final Extension devTools;

    private McpServer(String path, String token, String[] allowedOrigins, String serverName,
                      Extension devTools, List tools) {
        if(tools != null) {
            for(int iter = 0 ; iter < tools.size() ; iter++) {
                register((McpTool)tools.get(iter));
            }
        }
        this.path = path;
        this.token = token == null || token.length() == 0 ? null : utf8(token);
        this.allowedOrigins = allowedOrigins;
        this.serverName = serverName;
        this.devTools = devTools;
    }

    /**
     * The endpoint this configuration asks for, or null when it is off or there
     * would be no tool on it.
     *
     * @param devTools the development tools, which the build passes only in a
     *        development build; they are installed only on a development profile
     *        or with {@code cn1.mcp.devTools=true}
     * @param tools the application's tools, the ones its server registered
     */
    public static McpServer fromConfig(Config config, Extension devTools, String serverName,
                                       List tools) throws IOException {
        boolean development = config.isDevelopmentProfile();
        Extension extension = devTools != null
                && config.getBoolean(DEV_TOOLS, development) ? devTools : null;
        boolean anyTools = tools != null && !tools.isEmpty();
        if(!config.getBoolean(ENABLED, anyTools || extension != null)) {
            return null;
        }
        String token = config.get(TOKEN);
        if(!development && (token == null || token.length() == 0)) {
            throw new IOException("The MCP endpoint is on outside a development profile and "
                    + TOKEN + " is not set, so anyone who can reach the port could call its "
                    + "tools. Set a token, or " + ENABLED + "=false.");
        }
        String path = config.get(PATH, "/mcp");
        if(!path.startsWith("/")) {
            throw new IOException(PATH + " must start with /");
        }
        String origins = config.get(ALLOWED_ORIGINS, "");
        List list = new ArrayList();
        int start = 0;
        while(start <= origins.length()) {
            int comma = origins.indexOf(',', start);
            if(comma < 0) {
                comma = origins.length();
            }
            String o = origins.substring(start, comma).trim();
            if(o.length() > 0) {
                list.add(o);
            }
            start = comma + 1;
        }
        String[] allowed = new String[list.size()];
        for(int iter = 0 ; iter < allowed.length ; iter++) {
            allowed[iter] = (String)list.get(iter);
        }
        return new McpServer(path, token, allowed,
                serverName == null || serverName.length() == 0 ? "codenameone-backend"
                        : serverName, extension, tools);
    }

    /** Adds a tool to this endpoint, replacing one of the same name. */
    public synchronized void register(McpTool tool) {
        for(int iter = 0 ; iter < tools.size() ; iter++) {
            if(((McpTool)tools.get(iter)).name().equals(tool.name())) {
                tools.set(iter, tool);
                return;
            }
        }
        tools.add(tool);
    }

    /** Every tool on this endpoint. */
    public synchronized List tools() {
        return new ArrayList(tools);
    }

    /** Whether the development tools are installed on this endpoint. */
    public boolean hasDevTools() {
        return devTools != null;
    }

    /** The path the endpoint answers on. */
    public String getPath() {
        return path;
    }

    /** Called once the server is listening. */
    public void attach(Backend running) {
        this.backend = running;
        if(devTools != null) {
            devTools.install(this, running);
        }
    }

    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        String target = request.getTarget();
        if(target == null) {
            return null;
        }
        int query = target.indexOf('?');
        String p = query < 0 ? target : target.substring(0, query);
        if(!p.equals(path)) {
            return null;
        }
        if(!originAllowed(request)) {
            return request.respondJson(403, rpcError(null, -32600,
                    "Origin not allowed; add it to " + ALLOWED_ORIGINS));
        }
        if(!authorized(request)) {
            return request.respondJson(401, rpcError(null, -32600,
                    "A bearer token is required"));
        }
        String method = request.getMethod();
        if("GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method)) {
            // No server-initiated stream and no session to end: the transport
            // lets a server answer both with 405.
            return request.respond(405, "text/plain; charset=utf-8",
                    utf8("POST JSON-RPC to this endpoint"));
        }
        if(!"POST".equals(method)) {
            return request.respond(405, "text/plain; charset=utf-8", utf8("POST only"));
        }
        Object parsed;
        try {
            parsed = Json.parse(request.getBody());
        } catch (IOException err) {
            return request.respondJson(400, rpcError(null, -32700, "Parse error"));
        }
        if(parsed instanceof List) {
            List batch = (List)parsed;
            List answers = new ArrayList();
            for(int iter = 0 ; iter < batch.size() ; iter++) {
                Object answer = dispatch(batch.get(iter));
                if(answer != null) {
                    answers.add(answer);
                }
            }
            return answers.isEmpty() ? request.respond(202, "application/json", new byte[0])
                    : request.respondJson(200, answers);
        }
        Object answer = dispatch(parsed);
        if(answer == null) {
            return request.respond(202, "application/json", new byte[0]);
        }
        return request.respondJson(200, answer);
    }

    /** One JSON-RPC message; the response, or null for a notification. */
    Object dispatch(Object message) {
        if(!(message instanceof Map)) {
            return rpcError(null, -32600, "Invalid request");
        }
        Map m = (Map)message;
        Object id = m.get("id");
        Object methodValue = m.get("method");
        if(!(methodValue instanceof String)) {
            // A response to something the server sent, or garbage; the server
            // sends nothing, so either way there is nothing to answer.
            return id == null ? null : rpcError(id, -32600, "Invalid request");
        }
        String method = (String)methodValue;
        Map params = m.get("params") instanceof Map ? (Map)m.get("params") : new LinkedHashMap();
        if(id == null) {
            return null;
        }
        try {
            if("initialize".equals(method)) {
                return result(id, initialize(params));
            }
            if("ping".equals(method)) {
                return result(id, new LinkedHashMap());
            }
            if("tools/list".equals(method)) {
                return result(id, listTools());
            }
            if("tools/call".equals(method)) {
                return result(id, callTool(params));
            }
            if("resources/list".equals(method)) {
                return result(id, single("resources", new ArrayList()));
            }
            if("resources/templates/list".equals(method)) {
                return result(id, single("resourceTemplates", new ArrayList()));
            }
            if("prompts/list".equals(method)) {
                return result(id, single("prompts", new ArrayList()));
            }
            return rpcError(id, -32601, "Method not found: " + method);
        } catch (IllegalArgumentException err) {
            return rpcError(id, -32602, err.getMessage());
        } catch (Exception err) {
            return rpcError(id, -32603, String.valueOf(err));
        }
    }

    private Map initialize(Map params) {
        Object requested = params.get("protocolVersion");
        String version = PROTOCOL_VERSIONS[0];
        for(int iter = 0 ; iter < PROTOCOL_VERSIONS.length ; iter++) {
            if(PROTOCOL_VERSIONS[iter].equals(requested)) {
                version = PROTOCOL_VERSIONS[iter];
            }
        }
        Map out = new LinkedHashMap();
        out.put("protocolVersion", version);
        Map capabilities = new LinkedHashMap();
        Map tools = new LinkedHashMap();
        tools.put("listChanged", Boolean.FALSE);
        capabilities.put("tools", tools);
        capabilities.put("resources", new LinkedHashMap());
        capabilities.put("prompts", new LinkedHashMap());
        out.put("capabilities", capabilities);
        Map info = new LinkedHashMap();
        info.put("name", serverName);
        info.put("version", "1");
        out.put("serverInfo", info);
        if(devTools != null) {
            out.put("instructions", "A Codename One backend running in development. The "
                    + "backend_* tools inspect and exercise it: backend_routes and "
                    + "backend_beans show what the build wired, backend_call sends it a "
                    + "request, backend_sql reads its database, backend_requests shows what "
                    + "it served and what failed.");
        }
        return out;
    }

    private Map listTools() {
        List out = new ArrayList();
        List all = tools();
        for(int iter = 0 ; iter < all.size() ; iter++) {
            McpTool tool = (McpTool)all.get(iter);
            Map t = new LinkedHashMap();
            t.put("name", tool.name());
            t.put("description", tool.description());
            t.put("inputSchema", tool.inputSchema());
            out.add(t);
        }
        return single("tools", out);
    }

    private Map callTool(Map params) throws Exception {
        Object name = params.get("name");
        if(!(name instanceof String)) {
            throw new IllegalArgumentException("tools/call needs a tool name");
        }
        Map arguments = params.get("arguments") instanceof Map ? (Map)params.get("arguments")
                : new LinkedHashMap();
        List all = tools();
        for(int iter = 0 ; iter < all.size() ; iter++) {
            McpTool tool = (McpTool)all.get(iter);
            if(tool.name().equals(name)) {
                Map out = new LinkedHashMap();
                List content = new ArrayList();
                Map text = new LinkedHashMap();
                text.put("type", "text");
                try {
                    Object value = tool.call(arguments);
                    text.put("text", value instanceof String ? (String)value
                            : Json.write(value));
                    out.put("isError", Boolean.FALSE);
                } catch (Exception err) {
                    // A tool that fails answers a RESULT marked as an error, not a
                    // protocol error: the agent is meant to read it and try again.
                    text.put("text", err instanceof IllegalArgumentException
                            ? err.getMessage() : String.valueOf(err));
                    out.put("isError", Boolean.TRUE);
                }
                content.add(text);
                out.put("content", content);
                return out;
            }
        }
        throw new IllegalArgumentException("No tool named " + name);
    }

    private boolean originAllowed(HttpServer.Request request) {
        String origin = request.getHeader("origin");
        if(origin == null || origin.length() == 0) {
            // Not a browser: an agent's HTTP client sends none.
            return true;
        }
        for(int iter = 0 ; iter < allowedOrigins.length ; iter++) {
            if(allowedOrigins[iter].equals(origin) || "*".equals(allowedOrigins[iter])) {
                return true;
            }
        }
        // Loopback origins only, never "the Host this request names": under DNS
        // rebinding a hostile page's origin and the Host header are BOTH the
        // attacker's name (evil.example:8080), resolved to 127.0.0.1, so
        // comparing them lets exactly the page this check exists to stop drive
        // the server. A page the server itself serves must be listed.
        String host = hostOf(origin);
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "[::1]".equals(host);
    }

    private static String hostOf(String origin) {
        int scheme = origin.indexOf("://");
        String rest = scheme < 0 ? origin : origin.substring(scheme + 3);
        int slash = rest.indexOf('/');
        if(slash >= 0) {
            rest = rest.substring(0, slash);
        }
        if(rest.startsWith("[")) {
            int close = rest.indexOf(']');
            return close > 0 ? rest.substring(0, close + 1) : rest;
        }
        int colon = rest.lastIndexOf(':');
        return colon > 0 ? rest.substring(0, colon) : rest;
    }

    private boolean authorized(HttpServer.Request request) {
        if(token == null) {
            return true;
        }
        String header = request.getHeader("authorization");
        if(header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return false;
        }
        return Crypto.equalsConstantTime(token, utf8(header.substring(7).trim()));
    }

    static Map result(Object id, Object value) {
        Map out = new LinkedHashMap();
        out.put("jsonrpc", "2.0");
        out.put("id", id);
        out.put("result", value);
        return out;
    }

    static Map rpcError(Object id, int code, String message) {
        Map error = new LinkedHashMap();
        error.put("code", new Integer(code));
        error.put("message", message == null ? "error" : message);
        Map out = new LinkedHashMap();
        out.put("jsonrpc", "2.0");
        out.put("id", id);
        out.put("error", error);
        return out;
    }

    private static Map single(String key, Object value) {
        Map out = new LinkedHashMap();
        out.put(key, value);
        return out;
    }

    static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is required");
        }
    }
}
