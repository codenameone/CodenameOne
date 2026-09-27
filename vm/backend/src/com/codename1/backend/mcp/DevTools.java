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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.DevConsole;
import com.codename1.backend.Management;
import com.codename1.backend.Scheduler;
import com.codename1.backend.Web;
import com.codename1.backend.metrics.Metrics;
import com.codename1.backend.orm.ColumnDefinition;
import com.codename1.backend.orm.EntityDefinition;
import com.codename1.backend.orm.EntityManager;

/// The tools an agent developing a backend uses on the running server.
///
/// | Tool | What it does |
/// |---|---|
/// | `backend_routes` | every route the build generated |
/// | `backend_beans` | every bean, its scope and what it was given |
/// | `backend_config` | the profile and the configured keys, secrets masked |
/// | `backend_call` | sends the server an HTTP request and returns the response |
/// | `backend_requests` | the last requests served, and what failed and why |
/// | `backend_logs` | the last console lines |
/// | `backend_sql` | runs SQL against the server's database |
/// | `backend_schema` | the entities and their tables and columns |
/// | `backend_jobs` / `backend_run_job` | the scheduled jobs; run one now |
/// | `backend_metrics` | every metric's current value |
/// | `backend_managed` / `backend_invoke` | the managed beans; call an operation |
///
/// Linked only into a development build -- the entry point `cn1:backend`
/// runs -- and installed only on a development profile, so a production binary
/// has none of it.
public final class DevTools implements McpServer.Extension {
    private Backend backend;

    /// The server the tools were installed on; they are only reachable after that.
    private Backend backend() {
        Backend running = backend;
        if (running == null) {
            throw new IllegalStateException("The development tools are not installed");
        }
        return running;
    }

    @Override
    public void install(McpServer server, Backend running) {
        this.backend = running;
        running.getRequestLog().enable(200);
        DevConsole.install();
        server.register(new Tool("backend_routes",
                "Lists every HTTP route of the running backend: method, path and the "
                + "controller method that serves it. Use it to learn the API before "
                + "calling it with backend_call.", schema()) {
            @Override
            Object run(Map a) {
                return application() == null ? new ArrayList()
                        : application().describeRoutes();
            }
        });
        server.register(new Tool("backend_beans",
                "Lists every bean the build wired: its name, type, scope and the beans "
                + "injected into it. Use it to check dependency injection did what the "
                + "code intends.", schema()) {
            @Override
            Object run(Map a) {
                return application() == null ? new ArrayList()
                        : application().describeBeans();
            }
        });
        server.register(new Tool("backend_config",
                "Shows the active profile and every configured key, with values that "
                + "look secret masked.", schema()) {
            @Override
            Object run(Map a) throws Exception {
                return config();
            }
        });
        server.register(new Tool("backend_call",
                "Sends an HTTP request to the running backend and returns the status, "
                + "headers and body. Use it to exercise an endpoint after changing it.",
                schema(new String[] {"method", "string", "GET, POST, PUT, PATCH or DELETE",
                        "path", "string", "The path and query, starting with /",
                        "body", "string", "The request body, usually JSON",
                        "headers", "object", "Extra request headers, name to value"},
                        new String[] {"method", "path"})) {
            @Override
            Object run(Map a) throws Exception {
                // Named apart from McpTool.call: inside this class, call(a) bound
                // to the inherited method, which runs this one again -- a
                // backend_call that recursed until the stack ran out.
                return sendRequest(a);
            }
        });
        server.register(new Tool("backend_requests",
                "The last requests the backend served, newest first, with status, time "
                + "and any exception a handler threw. Set failuresOnly to see what broke.",
                schema(new String[] {"limit", "integer", "How many, default 20",
                        "failuresOnly", "boolean", "Only 5xx answers and exceptions"}, null)) {
            @Override
            Object run(Map a) {
                return backend().getRequestLog().recent(intArg(a, "limit", 20),
                        boolArg(a, "failuresOnly"));
            }
        });
        server.register(new Tool("backend_logs",
                "The last lines the backend printed to its console, oldest first.",
                schema(new String[] {"limit", "integer", "How many lines, default 100"},
                        null)) {
            @Override
            Object run(Map a) {
                if (!DevConsole.supported()) {
                    return "This runtime cannot capture the console; run the backend with "
                            + "cn1:backend to have logs here.";
                }
                return DevConsole.recent(intArg(a, "limit", 100));
            }
        });
        server.register(new Tool("backend_sql",
                "Runs SQL against the backend's database and returns the rows. Only "
                + "SELECT-like statements run unless write is true. Use ? placeholders "
                + "with params; the same SQL works on SQLite, PostgreSQL and MySQL.",
                schema(new String[] {"sql", "string", "The statement",
                        "params", "array", "Values for the ? placeholders",
                        "write", "boolean", "Allow INSERT, UPDATE, DELETE and DDL"},
                        new String[] {"sql"})) {
            @Override
            Object run(Map a) throws Exception {
                return sql(a);
            }
        });
        server.register(new Tool("backend_schema",
                "Lists the entities the build generated persistence for, with their "
                + "tables and columns.", schema()) {
            @Override
            Object run(Map a) {
                return schemaOf();
            }
        });
        server.register(new Tool("backend_jobs",
                "Lists the scheduled jobs: schedule, runs, failures, last and next run.",
                schema()) {
            @Override
            Object run(Map a) {
                Scheduler s = application() == null ? null : application().getScheduler();
                return s == null ? new ArrayList() : s.describe();
            }
        });
        server.register(new Tool("backend_run_job",
                "Runs a scheduled job now, whatever its schedule says.",
                schema(new String[] {"name", "string", "The job's name from backend_jobs"},
                        new String[] {"name"})) {
            @Override
            Object run(Map a) {
                Scheduler s = application() == null ? null : application().getScheduler();
                if (s == null || !s.trigger(stringArg(a, "name"))) {
                    throw new IllegalArgumentException("No idle job named "
                            + stringArg(a, "name"));
                }
                return "started";
            }
        });
        server.register(new Tool("backend_metrics",
                "Every metric's current value: request durations by route, pool and "
                + "executor gauges, and the application's own.",
                schema(new String[] {"prefix", "string", "Only metrics whose name starts "
                        + "with this"}, null)) {
            @Override
            Object run(Map a) {
                return metrics(stringArg(a, "prefix"));
            }
        });
        server.register(new Tool("backend_managed",
                "Lists the @ManagedResource beans with their attributes' current values "
                + "and their operations.", schema()) {
            @Override
            Object run(Map a) {
                return Management.describeBeans(backend().getManagedBeans());
            }
        });
        server.register(new Tool("backend_invoke",
                "Calls an operation of a @ManagedResource bean.",
                schema(new String[] {"bean", "string", "The bean's objectName",
                        "operation", "string", "The operation's name",
                        "arguments", "object", "Arguments by parameter name"},
                        new String[] {"bean", "operation"})) {
            @Override
            Object run(Map a) throws Exception {
                Object args = a.get("arguments");
                return Management.invoke(backend().getManagedBeans(), stringArg(a, "bean"), stringArg(a, "operation"),
                        args instanceof Map ? (Map) args : new LinkedHashMap());
            }
        });
    }

    private Backend.Application application() {
        return backend == null ? null : backend().getApplication();
    }

    private Map config() throws Exception {
        Config config = backend().getConfig();
        Map out = new LinkedHashMap();
        out.put("profile", config.getProfile());
        out.put("source", config.describe());
        Map values = new LinkedHashMap();
        List keys = config.keys();
        for (Object element : keys) {
            String key = String.valueOf(element);
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
        for (String element : marks) {
            if (containsIgnoreCase(k, element)) {
                return true;
            }
        }
        // A URL with a password in it: scheme://user:password@host
        if (value != null) {
            int scheme = value.indexOf("://");
            int at = value.indexOf('@');
            if (scheme > 0 && at > scheme && value.indexOf(':', scheme + 3) < at
                    && value.indexOf(':', scheme + 3) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String text, String part) {
        for (int iter = 0 ; iter + part.length() <= text.length() ; iter++) {
            if (text.regionMatches(true, iter, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }

    private Map sendRequest(Map a) throws Exception {
        String method = stringArg(a, "method");
        String path = stringArg(a, "path");
        if (method == null || path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("method and a path starting with / are "
                    + "required");
        }
        List headers = new ArrayList();
        Object extra = a.get("headers");
        boolean contentType = false;
        if (extra instanceof Map) {
            Iterator it = ((Map) extra).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                String name = String.valueOf(e.getKey());
                if ("content-type".equalsIgnoreCase(name)) {
                    contentType = true;
                }
                headers.add(name + ": " + e.getValue());
            }
        }
        String body = stringArg(a, "body");
        if (body != null && !contentType) {
            headers.add("Content-Type: application/json");
        }
        int port = backend().getServer().getPort();
        // The server's own scheme: plain HTTP to a TLS listener is answered
        // with a handshake failure, never with the route's response.
        String scheme = backend().getServer().isSecure() ? "https" : "http";
        Web.Result result = Web.request(asciiUpper(method), scheme + "://127.0.0.1:" + port
                + path, headers, body == null ? null : McpServer.utf8(body));
        Map out = new LinkedHashMap();
        out.put("status", Integer.valueOf(result.getStatus()));
        out.put("headers", result.getHeaders());
        String text = result.getBodyAsString();
        if (text != null && text.length() > 65536) {
            text = text.substring(0, 65536) + "... (" + text.length() + " characters)";
        }
        out.put("body", text);
        return out;
    }

    /// Runs a statement the caller did not confirm as a write, where the ENGINE
    /// refuses writes -- the first keyword is only a courtesy check. A
    /// PostgreSQL data-modifying WITH, an EXPLAIN ANALYZE DELETE and a writable
    /// SQLite pragma all begin like reads, and each would otherwise change the
    /// database. PostgreSQL and MySQL enforce a read-only transaction; SQLite
    /// ignores that flag, so query_only is set on the connection as well. The
    /// transaction is always rolled back.
    private static List readOnly(DataSource pool, String statement, Object[] params)
            throws IOException {
        String body = statement.trim();
        while (body.endsWith(";")) {
            body = body.substring(0, body.length() - 1).trim();
        }
        if (body.indexOf(';') >= 0) {
            // A second statement could end the read-only transaction and run
            // outside it.
            throw new IllegalArgumentException("Send one statement at a time, or pass "
                    + "write=true");
        }
        if (body.regionMatches(true, 0, "PRAGMA", 0, 6) && !readOnlyPragma(body)) {
            // Setting a pragma changes the pooled connection for whoever borrows
            // it next -- or the file -- whatever the transaction says, and SQLite
            // takes the value as `name = v` or as `name(v)`. So only pragmas known
            // to read run unconfirmed.
            throw new IllegalArgumentException("That pragma may change a setting; pass "
                    + "write=true to run it");
        }
        Database db = pool.borrow();
        boolean sqlite = "sqlite".equals(db.dialect().getName());
        boolean healthy = true;
        try {
            if (sqlite) {
                db.execute("PRAGMA query_only = ON", null);
            }
            db.beginTransaction(true);
            List rows;
            try {
                rows = db.query(body, params);
            } finally {
                try {
                    db.rollbackTransaction();
                } catch (IOException err) {
                    // A connection whose rollback failed still believes it is in
                    // a transaction, and the next borrower would wait on an owner
                    // that has left; it is closed, not pooled.
                    healthy = false;
                    throw err;
                }
            }
            return rows;
        } finally {
            if (sqlite) {
                try {
                    db.execute("PRAGMA query_only = OFF", null);
                } catch (IOException err) {
                    // A connection stuck read-only must not go back to the pool.
                    healthy = false;
                }
            }
            if (!healthy) {
                db.close();
            }
            pool.release(db);
        }
    }

    /// The pragmas that only read -- listing tables, columns, indexes and settings
    /// -- in either spelling: bare, or with a table name in parentheses. Anything
    /// else, and any `=`, needs write=true.
    private static final String[] READ_PRAGMAS = {"table_info", "table_xinfo",
            "table_list", "index_list", "index_info", "index_xinfo", "foreign_key_list",
            "foreign_key_check", "integrity_check", "quick_check", "database_list",
            "collation_list", "function_list", "module_list", "pragma_list",
            "compile_options", "page_count", "page_size", "freelist_count", "encoding",
            "user_version", "schema_version", "application_id", "foreign_keys",
            "journal_mode", "busy_timeout", "cache_size", "synchronous", "query_only"};

    /// The pragmas that may take a table or index name in parentheses.
    private static final String[] NAMED_PRAGMAS = {"table_info", "table_xinfo",
            "table_list", "index_list", "index_info", "index_xinfo", "foreign_key_list",
            "foreign_key_check", "integrity_check", "quick_check"};

    static boolean readOnlyPragma(String statement) {
        if (statement.indexOf('=') >= 0) {
            return false;
        }
        String rest = statement.substring(6).trim();
        int paren = rest.indexOf('(');
        String name = (paren < 0 ? rest : rest.substring(0, paren)).trim();
        int dot = name.indexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);              // schema.name
        }
        String[] allowed = paren < 0 ? READ_PRAGMAS : NAMED_PRAGMAS;
        for (String element : allowed) {
            if (element.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private Object sql(Map a) throws Exception {
        DataSource pool = backend().getDataSource();
        if (pool == null) {
            throw new IllegalArgumentException("This backend has no database");
        }
        String statement = stringArg(a, "sql");
        if (statement == null || statement.trim().length() == 0) {
            throw new IllegalArgumentException("sql is required");
        }
        Object[] params = new Object[0];
        Object p = a.get("params");
        if (p instanceof List) {
            params = ((List) p).toArray();
        }
        if (boolArg(a, "write")) {
            Map out = new LinkedHashMap();
            out.put("updated", Integer.valueOf(pool.execute(statement, params)));
            return out;
        }
        String head = statement.trim();
        String[] reads = {"SELECT", "WITH", "EXPLAIN", "PRAGMA", "SHOW", "DESCRIBE", "VALUES"};
        boolean read = false;
        for (int iter = 0 ; iter < reads.length && !read; iter++) {
            read = head.regionMatches(true, 0, reads[iter], 0, reads[iter].length());
        }
        if (!read) {
            throw new IllegalArgumentException("That statement writes; pass write=true to run "
                    + "it");
        }
        List rows = readOnly(pool, statement, params);
        if (rows.size() > 500) {
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
        for (EntityDefinition d : all) {
            Map m = new LinkedHashMap();
            m.put("entity", d.type().getName());
            m.put("table", d.table());
            List columns = new ArrayList();
            ColumnDefinition[] cols = d.columns();
            for (ColumnDefinition element : cols) {
                Map col = new LinkedHashMap();
                col.put("field", element.getField());
                col.put("column", element.getColumn());
                col.put("type", element.getDeclaredType());
                if (element.isId()) {
                    col.put("id", Boolean.TRUE);
                }
                if (element.isNullable()) {
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
        if (prefix == null || prefix.length() == 0) {
            return all;
        }
        Map out = new LinkedHashMap();
        Iterator it = all.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (String.valueOf(e.getKey()).startsWith(prefix)) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /// Upper case by hand: toUpperCase follows the locale, and an HTTP method is ASCII.
    static String asciiUpper(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            sb.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return sb.toString();
    }

    static String stringArg(Map a, String name) {
        Object v = a.get(name);
        return v == null ? null : String.valueOf(v);
    }

    static int intArg(Map a, String name, int fallback) {
        Object v = a.get(name);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt((String) v);
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

    /// An object schema with no properties.
    static Map schema() {
        return schema(new String[0], null);
    }

    /// An object schema from triples of name, JSON type and description, and the
    /// names that are required.
    static Map schema(String[] triples, String[] required) {
        Map properties = new LinkedHashMap();
        for (int iter = 0 ; iter + 2 < triples.length ; iter += 3) {
            Map p = new LinkedHashMap();
            p.put("type", triples[iter + 1]);
            p.put("description", triples[iter + 2]);
            properties.put(triples[iter], p);
        }
        Map out = new LinkedHashMap();
        out.put("type", "object");
        out.put("properties", properties);
        if (required != null && required.length > 0) {
            List r = new ArrayList();
            for (String element : required) {
                r.add(element);
            }
            out.put("required", r);
        }
        return out;
    }

    /// A tool whose behaviour is one method.
    abstract static class Tool implements McpTool {
        private final String name;
        private final String description;
        private final Map schema;

        Tool(String name, String description, Map schema) {
            this.name = name;
            this.description = description;
            this.schema = schema;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public Map inputSchema() {
            return schema;
        }

        @Override
        public Object call(Map arguments) throws Exception {
            return run(arguments);
        }

        abstract Object run(Map arguments) throws Exception;
    }
}
