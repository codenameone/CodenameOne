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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.DevConsole;
import com.codename1.backend.Management;
import com.codename1.backend.RequestLog;
import com.codename1.backend.Scheduler;
import com.codename1.backend.Web;
import com.codename1.backend.metrics.Metrics;
import com.codename1.backend.orm.ColumnDefinition;
import com.codename1.backend.orm.EntityDefinition;
import com.codename1.backend.orm.EntityManager;

/**
 * The tools an agent developing a backend uses on the running server.
 *
 * <table>
 *   <tr><td>backend_routes</td><td>every route the build generated</td></tr>
 *   <tr><td>backend_beans</td><td>every bean, its scope and what it was given</td></tr>
 *   <tr><td>backend_config</td><td>the profile and the configured keys, secrets masked</td></tr>
 *   <tr><td>backend_call</td><td>sends the server an HTTP request and returns the response</td></tr>
 *   <tr><td>backend_requests</td><td>the last requests served, and what failed and why</td></tr>
 *   <tr><td>backend_logs</td><td>the last console lines</td></tr>
 *   <tr><td>backend_sql</td><td>runs SQL against the server's database</td></tr>
 *   <tr><td>backend_schema</td><td>the entities and their tables and columns</td></tr>
 *   <tr><td>backend_jobs / backend_run_job</td><td>the scheduled jobs; run one now</td></tr>
 *   <tr><td>backend_metrics</td><td>every metric's current value</td></tr>
 *   <tr><td>backend_managed / backend_invoke</td><td>the managed beans; call an operation</td></tr>
 * </table>
 *
 * <p>Linked only into a development build -- the entry point {@code cn1:backend}
 * runs -- and installed only on a development profile, so a production binary
 * has none of it.
 */
public final class DevTools implements McpServer.Extension {
    private Backend backend;

    public void install(McpServer server, Backend running) {
        this.backend = running;
        RequestLog.enable(200);
        DevConsole.install();
        McpServer.register(new Tool("backend_routes",
                "Lists every HTTP route of the running backend: method, path and the "
                + "controller method that serves it. Use it to learn the API before "
                + "calling it with backend_call.", schema()) {
            Object run(Map a) {
                return application() == null ? new ArrayList()
                        : application().describeRoutes();
            }
        });
        McpServer.register(new Tool("backend_beans",
                "Lists every bean the build wired: its name, type, scope and the beans "
                + "injected into it. Use it to check dependency injection did what the "
                + "code intends.", schema()) {
            Object run(Map a) {
                return application() == null ? new ArrayList()
                        : application().describeBeans();
            }
        });
        McpServer.register(new Tool("backend_config",
                "Shows the active profile and every configured key, with values that "
                + "look secret masked.", schema()) {
            Object run(Map a) throws Exception {
                return config();
            }
        });
        McpServer.register(new Tool("backend_call",
                "Sends an HTTP request to the running backend and returns the status, "
                + "headers and body. Use it to exercise an endpoint after changing it.",
                schema(new String[] {"method", "string", "GET, POST, PUT, PATCH or DELETE",
                        "path", "string", "The path and query, starting with /",
                        "body", "string", "The request body, usually JSON",
                        "headers", "object", "Extra request headers, name to value"},
                        new String[] {"method", "path"})) {
            Object run(Map a) throws Exception {
                return call(a);
            }
        });
        McpServer.register(new Tool("backend_requests",
                "The last requests the backend served, newest first, with status, time "
                + "and any exception a handler threw. Set failuresOnly to see what broke.",
                schema(new String[] {"limit", "integer", "How many, default 20",
                        "failuresOnly", "boolean", "Only 5xx answers and exceptions"}, null)) {
            Object run(Map a) {
                return RequestLog.recent(intArg(a, "limit", 20), boolArg(a, "failuresOnly"));
            }
        });
        McpServer.register(new Tool("backend_logs",
                "The last lines the backend printed to its console, oldest first.",
                schema(new String[] {"limit", "integer", "How many lines, default 100"},
                        null)) {
            Object run(Map a) {
                if(!DevConsole.supported()) {
                    return "This runtime cannot capture the console; run the backend with "
                            + "cn1:backend to have logs here.";
                }
                return DevConsole.recent(intArg(a, "limit", 100));
            }
        });
        McpServer.register(new Tool("backend_sql",
                "Runs SQL against the backend's database and returns the rows. Only "
                + "SELECT-like statements run unless write is true. Use ? placeholders "
                + "with params; the same SQL works on SQLite, PostgreSQL and MySQL.",
                schema(new String[] {"sql", "string", "The statement",
                        "params", "array", "Values for the ? placeholders",
                        "write", "boolean", "Allow INSERT, UPDATE, DELETE and DDL"},
                        new String[] {"sql"})) {
            Object run(Map a) throws Exception {
                return sql(a);
            }
        });
        McpServer.register(new Tool("backend_schema",
                "Lists the entities the build generated persistence for, with their "
                + "tables and columns.", schema()) {
            Object run(Map a) {
                return schemaOf();
            }
        });
        McpServer.register(new Tool("backend_jobs",
                "Lists the scheduled jobs: schedule, runs, failures, last and next run.",
                schema()) {
            Object run(Map a) {
                Scheduler s = application() == null ? null : application().getScheduler();
                return s == null ? new ArrayList() : s.describe();
            }
        });
        McpServer.register(new Tool("backend_run_job",
                "Runs a scheduled job now, whatever its schedule says.",
                schema(new String[] {"name", "string", "The job's name from backend_jobs"},
                        new String[] {"name"})) {
            Object run(Map a) {
                Scheduler s = application() == null ? null : application().getScheduler();
                if(s == null || !s.trigger(stringArg(a, "name"))) {
                    throw new IllegalArgumentException("No idle job named "
                            + stringArg(a, "name"));
                }
                return "started";
            }
        });
        McpServer.register(new Tool("backend_metrics",
                "Every metric's current value: request durations by route, pool and "
                + "executor gauges, and the application's own.",
                schema(new String[] {"prefix", "string", "Only metrics whose name starts "
                        + "with this"}, null)) {
            Object run(Map a) {
                return metrics(stringArg(a, "prefix"));
            }
        });
        McpServer.register(new Tool("backend_managed",
                "Lists the @ManagedResource beans with their attributes' current values "
                + "and their operations.", schema()) {
            Object run(Map a) {
                return Management.describeBeans();
            }
        });
        McpServer.register(new Tool("backend_invoke",
                "Calls an operation of a @ManagedResource bean.",
                schema(new String[] {"bean", "string", "The bean's objectName",
                        "operation", "string", "The operation's name",
                        "arguments", "object", "Arguments by parameter name"},
                        new String[] {"bean", "operation"})) {
            Object run(Map a) throws Exception {
                Object args = a.get("arguments");
                return Management.invoke(stringArg(a, "bean"), stringArg(a, "operation"),
                        args instanceof Map ? (Map)args : new LinkedHashMap());
            }
        });
    }

    private Backend.Application application() {
        return backend == null ? null : backend.getApplication();
    }

    private Map config() throws Exception {
        Config config = backend.getConfig();
        Map out = new LinkedHashMap();
        out.put("profile", config.getProfile());
        out.put("source", config.describe());
        Map values = new LinkedHashMap();
        List keys = config.keys();
        for(int iter = 0 ; iter < keys.size() ; iter++) {
            String key = String.valueOf(keys.get(iter));
            String value;
            try {
                value = config.get(key);
            } catch (Exception err) {
                value = "<" + err.getMessage() + ">";
            }
            values.put(key, secret(key, value) ? "***" : value);
        }
        out.put("values", values);
        return out;
    }

    static boolean secret(String key, String value) {
        String k = key;
        String[] marks = {"password", "secret", "token", "key", "credential"};
        for(int iter = 0 ; iter < marks.length ; iter++) {
            if(containsIgnoreCase(k, marks[iter])) {
                return true;
            }
        }
        // A URL with a password in it: scheme://user:password@host
        if(value != null) {
            int scheme = value.indexOf("://");
            int at = value.indexOf('@');
            if(scheme > 0 && at > scheme && value.indexOf(':', scheme + 3) < at
                    && value.indexOf(':', scheme + 3) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String text, String part) {
        for(int iter = 0 ; iter + part.length() <= text.length() ; iter++) {
            if(text.regionMatches(true, iter, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }

    private Map call(Map a) throws Exception {
        String method = stringArg(a, "method");
        String path = stringArg(a, "path");
        if(method == null || path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("method and a path starting with / are "
                    + "required");
        }
        List headers = new ArrayList();
        Object extra = a.get("headers");
        boolean contentType = false;
        if(extra instanceof Map) {
            Iterator it = ((Map)extra).entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry e = (Map.Entry)it.next();
                String name = String.valueOf(e.getKey());
                if("content-type".equalsIgnoreCase(name)) {
                    contentType = true;
                }
                headers.add(name + ": " + e.getValue());
            }
        }
        String body = stringArg(a, "body");
        if(body != null && !contentType) {
            headers.add("Content-Type: application/json");
        }
        int port = backend.getServer().getPort();
        String scheme = "http";
        Web.Result result = Web.request(asciiUpper(method), scheme + "://127.0.0.1:" + port
                + path, headers, body == null ? null : McpServer.utf8(body));
        Map out = new LinkedHashMap();
        out.put("status", new Integer(result.getStatus()));
        out.put("headers", result.getHeaders());
        String text = result.getBodyAsString();
        if(text != null && text.length() > 65536) {
            text = text.substring(0, 65536) + "... (" + text.length() + " characters)";
        }
        out.put("body", text);
        return out;
    }

    private Object sql(Map a) throws Exception {
        DataSource pool = backend.getDataSource();
        if(pool == null) {
            throw new IllegalArgumentException("This backend has no database");
        }
        String statement = stringArg(a, "sql");
        if(statement == null || statement.trim().length() == 0) {
            throw new IllegalArgumentException("sql is required");
        }
        Object[] params = new Object[0];
        Object p = a.get("params");
        if(p instanceof List) {
            params = ((List)p).toArray();
        }
        if(boolArg(a, "write")) {
            Map out = new LinkedHashMap();
            out.put("updated", new Integer(pool.execute(statement, params)));
            return out;
        }
        String head = statement.trim();
        String[] reads = {"SELECT", "WITH", "EXPLAIN", "PRAGMA", "SHOW", "DESCRIBE", "VALUES"};
        boolean read = false;
        for(int iter = 0 ; iter < reads.length && !read; iter++) {
            read = head.regionMatches(true, 0, reads[iter], 0, reads[iter].length());
        }
        if(!read) {
            throw new IllegalArgumentException("That statement writes; pass write=true to run "
                    + "it");
        }
        List rows = pool.query(statement, params);
        if(rows.size() > 500) {
            List cut = new ArrayList(rows.subList(0, 500));
            Map out = new LinkedHashMap();
            out.put("rows", cut);
            out.put("truncated", "first 500 of " + rows.size() + " rows");
            return out;
        }
        return rows;
    }

    private List schemaOf() {
        List out = new ArrayList();
        EntityDefinition[] all = EntityManager.registered();
        for(int iter = 0 ; iter < all.length ; iter++) {
            EntityDefinition d = all[iter];
            Map m = new LinkedHashMap();
            m.put("entity", d.type().getName());
            m.put("table", d.table());
            List columns = new ArrayList();
            ColumnDefinition[] cols = d.columns();
            for(int c = 0 ; c < cols.length ; c++) {
                Map col = new LinkedHashMap();
                col.put("field", cols[c].getField());
                col.put("column", cols[c].getColumn());
                col.put("type", cols[c].getDeclaredType());
                if(cols[c].isId()) {
                    col.put("id", Boolean.TRUE);
                }
                if(cols[c].isNullable()) {
                    col.put("nullable", Boolean.TRUE);
                }
                columns.add(col);
            }
            m.put("columns", columns);
            out.add(m);
        }
        return out;
    }

    private static Map metrics(String prefix) {
        Map all = Metrics.snapshot();
        if(prefix == null || prefix.length() == 0) {
            return all;
        }
        Map out = new LinkedHashMap();
        Iterator it = all.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry e = (Map.Entry)it.next();
            if(String.valueOf(e.getKey()).startsWith(prefix)) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /** Upper case by hand: toUpperCase follows the locale, and an HTTP method is ASCII. */
    static String asciiUpper(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            sb.append(c >= 'a' && c <= 'z' ? (char)(c - 32) : c);
        }
        return sb.toString();
    }

    static String stringArg(Map a, String name) {
        Object v = a.get(name);
        return v == null ? null : String.valueOf(v);
    }

    static int intArg(Map a, String name, int fallback) {
        Object v = a.get(name);
        if(v instanceof Number) {
            return ((Number)v).intValue();
        }
        if(v instanceof String) {
            try {
                return Integer.parseInt((String)v);
            } catch (NumberFormatException err) {
                return fallback;
            }
        }
        return fallback;
    }

    static boolean boolArg(Map a, String name) {
        Object v = a.get(name);
        return Boolean.TRUE.equals(v) || "true".equals(v);
    }

    /** An object schema with no properties. */
    static Map schema() {
        return schema(new String[0], null);
    }

    /**
     * An object schema from triples of name, JSON type and description, and the
     * names that are required.
     */
    static Map schema(String[] triples, String[] required) {
        Map properties = new LinkedHashMap();
        for(int iter = 0 ; iter + 2 < triples.length ; iter += 3) {
            Map p = new LinkedHashMap();
            p.put("type", triples[iter + 1]);
            p.put("description", triples[iter + 2]);
            properties.put(triples[iter], p);
        }
        Map out = new LinkedHashMap();
        out.put("type", "object");
        out.put("properties", properties);
        if(required != null && required.length > 0) {
            List r = new ArrayList();
            for(int iter = 0 ; iter < required.length ; iter++) {
                r.add(required[iter]);
            }
            out.put("required", r);
        }
        return out;
    }

    /** A tool whose behaviour is one method. */
    abstract static class Tool implements McpTool {
        private final String name;
        private final String description;
        private final Map schema;

        Tool(String name, String description, Map schema) {
            this.name = name;
            this.description = description;
            this.schema = schema;
        }

        public String name() {
            return name;
        }

        public String description() {
            return description;
        }

        public Map inputSchema() {
            return schema;
        }

        public Object call(Map arguments) throws Exception {
            return run(arguments);
        }

        abstract Object run(Map arguments) throws Exception;
    }
}
