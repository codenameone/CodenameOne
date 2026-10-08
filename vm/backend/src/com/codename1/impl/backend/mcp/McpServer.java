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
package com.codename1.impl.backend.mcp;

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

/// The server's Model Context Protocol endpoint: JSON-RPC over MCP's Streamable
/// HTTP transport, at `cn1.mcp.path` (`/mcp` by default).
///
/// It serves two kinds of tools. The application's own -- every
/// `@McpTool` method, registered by the generated entry point -- and, on a
/// development profile of a development build, the [DevTools]: the routes,
/// the beans, the database, the jobs and the metrics of the running server, so an
/// agent building the backend can inspect and exercise it.
///
/// ```java
///   claude mcp add --transport http backend http://127.0.0.1:8080/mcp
/// ```
///
/// ## Security
///
/// Outside a development profile the endpoint needs
/// `Authorization: Bearer ` and the server refuses to start
/// without a token, because a tool anyone can call is a vulnerability. On a
/// development profile the token is optional. Either way a request whose
/// `Origin` is not a loopback address or one listed in
/// `cn1.mcp.allowedOrigins` is refused -- including a page the server
/// serves itself, which has to be listed -- which is what the MCP
/// specification asks for against DNS rebinding: a web page in the developer's
/// browser must not be able to drive the server.
///
/// The endpoint answers POSTed JSON-RPC with a JSON body. It keeps no session
/// and opens no event stream, so a GET is answered 405, as the transport allows.
///
/// `cn1.mcp.enabled` is a build-time setting as well as a switch: a packaged
/// server with no tools has the endpoint only when its build found the key true,
/// and one built without it that finds the key true at run time does not start.
public final class McpServer implements HttpServer.Handler {
    public static final String ENABLED = "cn1.mcp.enabled";
    public static final String PATH = "cn1.mcp.path";
    public static final String TOKEN = "cn1.mcp.token";
    public static final String ALLOWED_ORIGINS = "cn1.mcp.allowedOrigins";
    public static final String DEV_TOOLS = "cn1.mcp.devTools";

    /// The newest protocol revision this server speaks, and the ones it accepts.
    static final String[] PROTOCOL_VERSIONS = {"2025-06-18", "2025-03-26", "2024-11-05"};

    /// This endpoint's tools. Per server, never per process: a server started
    /// again in the same process -- with its development tools off, or a
    /// conditional @McpTool bean inactive -- must not keep serving the tools of
    /// one that stopped, bound to beans that have been destroyed.
    private final List tools = new ArrayList();

    private final String path;
    private final byte[] token;
    private final String[] allowedOrigins;
    private final String serverName;

    /// Extra tools installed when the server starts; the development tools are one.
    public interface Extension {
        /// Registers tools, given the running server.
        void install(McpServer server, Backend backend);
    }

    private final Extension devTools;

    private McpServer(String path, String token, String[] allowedOrigins, String serverName,
                      Extension devTools, List tools) {
        if (tools != null) {
            for (Object element : tools) {
                register((McpTool) element);
            }
        }
        this.path = path;
        this.token = token == null || token.length() == 0 ? null : utf8(token);
        this.allowedOrigins = allowedOrigins;
        this.serverName = serverName;
        this.devTools = devTools;
    }

    /// The endpoint this configuration asks for, or null when it is off or there
    /// would be no tool on it.
    ///
    /// #### Parameters
    ///
    /// - `devTools`: the development tools, which the build passes only in a
    /// development build; they are installed only on a development profile
    /// or with `cn1.mcp.devTools=true`
    ///
    /// - `tools`: the application's tools, the ones its server registered
    public static McpServer fromConfig(Config config, Extension devTools, String serverName,
                                       List tools) throws IOException {
        boolean development = config.isDevelopmentProfile();
        Extension extension = devTools != null
                && config.getBoolean(DEV_TOOLS, development) ? devTools : null;
        boolean anyTools = tools != null && !tools.isEmpty();
        if (!config.getBoolean(ENABLED, anyTools || extension != null)) {
            return null;
        }
        String token = config.getHeaderSecret(TOKEN);
        if (!development && (token == null || token.length() == 0)) {
            throw new IOException("The MCP endpoint is on outside a development profile and "
                    + TOKEN + " is not set, so anyone who can reach the port could call its "
                    + "tools. Set a token, or " + ENABLED + "=false.");
        }
        String path = config.getRoutePath(PATH, "/mcp");
        if (!path.startsWith("/")) {
            throw new IOException(PATH + " must start with /");
        }
        String origins = config.get(ALLOWED_ORIGINS, "");
        List list = new ArrayList();
        int start = 0;
        while (start <= origins.length()) {
            int comma = origins.indexOf(',', start);
            if (comma < 0) {
                comma = origins.length();
            }
            String o = origins.substring(start, comma).trim();
            if (o.length() > 0) {
                list.add(o);
            }
            start = comma + 1;
        }
        String[] allowed = new String[list.size()];
        for (int iter = 0 ; iter < allowed.length ; iter++) {
            allowed[iter] = (String) list.get(iter);
        }
        return new McpServer(path, token, allowed,
                serverName == null || serverName.length() == 0 ? "codenameone-backend"
                        : serverName, extension, tools);
    }

    /// Adds a tool to this endpoint, replacing one of the same name.
    public synchronized void register(McpTool tool) {
        for (int iter = 0 ; iter < tools.size() ; iter++) {
            if (((McpTool) tools.get(iter)).name().equals(tool.name())) {
                tools.set(iter, tool);
                return;
            }
        }
        tools.add(tool);
    }

    /// Every tool on this endpoint.
    public synchronized List tools() {
        return new ArrayList(tools);
    }

    /// Whether the development tools are installed on this endpoint.
    public boolean hasDevTools() {
        return devTools != null;
    }

    /// The path the endpoint answers on.
    public String getPath() {
        return path;
    }

    /// Called once the server is listening.
    public void attach(Backend running) {
        if (devTools != null) {
            devTools.install(this, running);
        }
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        // The CANONICAL path, as every generated route and the static files
        // compare it: /%6dcp is the same URI as /mcp (RFC 3986 6.2.2), and
        // matching the raw spelling let it fall through to a later handler.
        if (request.getTarget() == null || !request.pathFrom(0).equals(path)) {
            return null;
        }
        // CORS for an origin the configuration allows. A browser sends a JSON
        // POST with a bearer token only after an OPTIONS preflight, which carries
        // no token -- so it is answered here, before authentication, and every
        // answer to that origin names it, or the browser withholds the response
        // from the page. An origin that is not allowed gets neither, and the 403
        // below.
        String origin = request.getHeader("origin");
        boolean cors = origin != null && origin.length() > 0 && originAllowed(request);
        if (cors && "OPTIONS".equals(request.getMethod())) {
            return request.respond(204, "text/plain", new byte[0])
                    .header("Access-Control-Allow-Origin", origin)
                    .header("Vary", "Origin")
                    .header("Access-Control-Allow-Methods", "POST")
                    .header("Access-Control-Allow-Headers",
                            "authorization, content-type, mcp-protocol-version, mcp-session-id")
                    .header("Access-Control-Max-Age", "600");
        }
        HttpServer.Response response = serve(request);
        if (cors && response != null) {
            response.header("Access-Control-Allow-Origin", origin).header("Vary", "Origin");
        }
        return response;
    }

    private HttpServer.Response serve(HttpServer.Request request) throws Exception {
        if (!originAllowed(request)) {
            return request.respondJson(403, rpcError(null, -32600,
                    "Origin not allowed; add it to " + ALLOWED_ORIGINS));
        }
        if (!authorized(request)) {
            return request.respondJson(401, rpcError(null, -32600,
                    "A bearer token is required"));
        }
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method)) {
            // No server-initiated stream and no session to end: the transport
            // lets a server answer both with 405.
            return request.respond(405, "text/plain; charset=utf-8",
                    utf8("POST JSON-RPC to this endpoint"));
        }
        if (!"POST".equals(method)) {
            return request.respond(405, "text/plain; charset=utf-8", utf8("POST only"));
        }
        Object parsed;
        try {
            parsed = Json.parse(request.getBody());
        } catch (IOException err) {
            return request.respondJson(400, rpcError(null, -32700, "Parse error"));
        }
        if (parsed instanceof List) {
            List batch = (List) parsed;
            if (batch.isEmpty()) {
                // JSON-RPC: an empty batch is an invalid request, answered with a
                // single error -- not the silent 202 an all-notification batch gets.
                return request.respondJson(200, rpcError(null, -32600, "Invalid request"));
            }
            List answers = new ArrayList();
            for (Object element : batch) {
                Object answer = dispatch(element);
                if (answer != null) {
                    answers.add(answer);
                }
            }
            return answers.isEmpty() ? request.respond(202, "application/json", new byte[0])
                    : request.respondJson(200, answers);
        }
        Object answer = dispatch(parsed);
        if (answer == null) {
            return request.respond(202, "application/json", new byte[0]);
        }
        return request.respondJson(200, answer);
    }

    /// One JSON-RPC message; the response, or null for a notification.
    Object dispatch(Object message) {
        if (!(message instanceof Map)) {
            return rpcError(null, -32600, "Invalid request");
        }
        Map m = (Map) message;
        Object id = m.get("id");
        // By the key, not the value: an absent id is a notification, while an
        // explicit null is a request JSON-RPC answers -- with null as its id.
        // Reading the value alone left such a client waiting forever on a 202.
        boolean notification = !m.containsKey("id");
        if (id != null && !(id instanceof String) && !(id instanceof Number)) {
            // JSON-RPC ids are strings, numbers or null. An object, array or
            // boolean id is refused before the method runs -- a tool call must not
            // take effect for a request whose answer the client cannot match --
            // and the error carries null, the id of a request it could not read.
            return rpcError(null, -32600, "Invalid request: id must be a string, a number "
                    + "or null");
        }
        Object methodValue = m.get("method");
        if (!(methodValue instanceof String)) {
            if (m.containsKey("result") || m.containsKey("error")) {
                // A response to something the server sent; it sends nothing, so
                // there is nothing to match it to and nothing to answer.
                return null;
            }
            // No method is not a notification -- a notification is a VALID
            // request without an id -- so it is answered, with a null id when it
            // carried none.
            return rpcError(notification ? null : id, -32600, "Invalid request");
        }
        if (!"2.0".equals(m.get("jsonrpc"))) {
            // A JSON-RPC 2.0 request says so exactly; one that does not -- no
            // version, or a 1.0 envelope -- is refused before anything runs, a
            // tool call included.
            return rpcError(notification ? null : id, -32600, "Invalid request: jsonrpc "
                    + "must be \"2.0\"");
        }
        String method = (String) methodValue;
        Object rawParams = m.get("params");
        if (rawParams != null && !(rawParams instanceof Map)) {
            // Every method here takes named params. An array or a scalar is not
            // read as "none": initialize with params 1 must not succeed with the
            // defaults, nor a positional tool call run with no arguments.
            return notification ? null : rpcError(id, -32602, "Invalid params: params must "
                    + "be an object");
        }
        Map params = rawParams == null ? new LinkedHashMap() : (Map) rawParams;
        // A notification is still an invocation -- an id-less tools/call runs its
        // tool -- and only the answer, result or error, is withheld.
        Object answer = invoke(method, params, id);
        return notification ? null : answer;
    }

    private Object invoke(String method, Map params, Object id) {
        try {
            if ("initialize".equals(method)) {
                return result(id, initialize(params));
            }
            if ("ping".equals(method)) {
                return result(id, new LinkedHashMap());
            }
            if ("tools/list".equals(method)) {
                return result(id, listTools());
            }
            if ("tools/call".equals(method)) {
                return result(id, callTool(params));
            }
            if ("resources/list".equals(method)) {
                return result(id, single("resources", new ArrayList()));
            }
            if ("resources/templates/list".equals(method)) {
                return result(id, single("resourceTemplates", new ArrayList()));
            }
            if ("prompts/list".equals(method)) {
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
        for (String element : PROTOCOL_VERSIONS) {
            if (element.equals(requested)) {
                version = element;
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
        if (devTools != null) {
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
        for (Object element : all) {
            McpTool tool = (McpTool) element;
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
        if (!(name instanceof String)) {
            throw new IllegalArgumentException("tools/call needs a tool name");
        }
        Object rawArguments = params.get("arguments");
        if (rawArguments != null && !(rawArguments instanceof Map)) {
            // Refused, not read as "no arguments": a tool that needs none, or has
            // defaults, would otherwise run -- side effects and all -- for a call
            // the client got wrong.
            throw new IllegalArgumentException("tools/call arguments must be an object");
        }
        Map arguments = rawArguments == null ? new LinkedHashMap() : (Map) rawArguments;
        List all = tools();
        for (Object element : all) {
            McpTool tool = (McpTool) element;
            if (tool.name().equals(name)) {
                Map out = new LinkedHashMap();
                List content = new ArrayList();
                Map text = new LinkedHashMap();
                text.put("type", "text");
                try {
                    Object value = tool.call(arguments);
                    text.put("text", value instanceof String ? (String) value
                            : Json.write(value));
                    out.put("isError", Boolean.FALSE);
                } catch (IllegalArgumentException err) {
                    // A tool that fails answers a RESULT marked as an error, not a
                    // protocol error: the agent is meant to read it and try again.
                    // A bad argument's message is written for the agent already.
                    text.put("text", err.getMessage());
                    out.put("isError", Boolean.TRUE);
                } catch (Exception err) {
                    text.put("text", String.valueOf(err));
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
        if (origin == null || origin.length() == 0) {
            // Not a browser: an agent's HTTP client sends none.
            return true;
        }
        for (String element : allowedOrigins) {
            if (element.equals(origin) || "*".equals(element)) {
                return true;
            }
        }
        // Loopback origins only, never "the Host this request names": under DNS
        // rebinding a hostile page's origin and the Host header are BOTH the
        // attacker's name (evil.example:8080), resolved to 127.0.0.1, so
        // comparing them lets exactly the page this check exists to stop drive
        // the server. A page the server itself serves must be listed.
        String host = hostOf(origin);
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host) //NOPMD AvoidUsingHardCodedIP - recognises a loopback origin
                || "[::1]".equals(host);
    }

    private static String hostOf(String origin) {
        int scheme = origin.indexOf("://");
        String rest = scheme < 0 ? origin : origin.substring(scheme + 3);
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }
        if (rest.startsWith("[")) {
            int close = rest.indexOf(']');
            return close > 0 ? rest.substring(0, close + 1) : rest;
        }
        int colon = rest.lastIndexOf(':');
        return colon > 0 ? rest.substring(0, colon) : rest;
    }

    /// Whether requests must present a bearer token. Without one -- a
    /// development profile's default -- the server binds its listener to
    /// loopback, since this endpoint reaches the database and every handler.
    public boolean hasToken() {
        return token != null;
    }

    private boolean authorized(HttpServer.Request request) {
        if (token == null) {
            return true;
        }
        String header = request.getHeader("authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
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
        error.put("code", Integer.valueOf(code));
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
            throw new IllegalStateException("UTF-8 is required", err);
        }
    }
}
