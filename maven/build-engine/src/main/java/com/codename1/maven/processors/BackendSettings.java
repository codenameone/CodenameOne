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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.ProcessorContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The settings annotations -- `@ServerConfig`, `@SessionConfig`,
/// `@DataSourceConfig`, `@StaticFilesConfig`, `@EnableManagement`,
/// `@EnableMcpServer` and the exporter settings on `@OpenTelemetry` -- turned
/// into what the generated entry point passes the runtime: key and value pairs
/// for the bottom layer of its configuration, and whether to link the
/// management and MCP endpoints at all.
///
/// Every attribute is one `cn1.*` key, so an annotation is a compiled-in
/// `application.properties` line and nothing more: the files and the environment
/// still override it, and a key the runtime would refuse is refused here first,
/// where the message can name the class.
final class BackendSettings {
    private static final String PKG = "Lcom/codename1/backend/annotations/";
    static final String SERVER = PKG + "ServerConfig;";
    static final String SESSION = PKG + "SessionConfig;";
    static final String DATA_SOURCE = PKG + "DataSourceConfig;";
    static final String STATIC_FILES = PKG + "StaticFilesConfig;";
    static final String ENABLE_MANAGEMENT = PKG + "EnableManagement;";
    static final String ENABLE_MCP = PKG + "EnableMcpServer;";

    /// Key and value pairs, in the order they were found.
    final Map<String, String> values = new LinkedHashMap<String, String>();
    /// Which class set each key, for a conflict's message.
    private final Map<String, String> owners = new LinkedHashMap<String, String>();
    /// Whether the build asked for the management endpoints.
    boolean management;
    /// Whether the build asked for the MCP endpoint, tools aside.
    boolean mcp;

    private final ProcessorContext ctx;

    private BackendSettings(ProcessorContext ctx) {
        this.ctx = ctx;
    }

    /// Reads every settings annotation in the module, reporting conflicts and
    /// values the runtime would refuse through `ctx`.
    static BackendSettings resolve(ProcessorContext ctx) {
        BackendSettings out = new BackendSettings(ctx);
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            // The orphan rule every other annotation gets: a class whose source
            // was deleted left its .class behind and must not keep a setting.
            if (!BuildHintAnnotationProcessor.hasBackingSource(cls, ctx.getCompileSourceRoots(),
                    ctx.getSourceEncoding())) {
                continue;
            }
            out.read(cls);
        }
        out.management |= RestControllerAnnotationProcessor.applicationPropertyTrue(ctx,
                "cn1.management.enabled");
        out.mcp |= RestControllerAnnotationProcessor.applicationPropertyTrue(ctx,
                "cn1.mcp.enabled");
        return out;
    }

    /// The pairs as the flat array the runtime takes.
    List<String> flat() {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            out.add(e.getKey());
            out.add(e.getValue());
        }
        return out;
    }

    private void read(AnnotatedClass cls) {
        AnnotationValues a = cls.getClassAnnotation(SERVER);
        if (a != null) {
            port(cls, a);
            positive(cls, a, "workers", "cn1.server.workers");
            positive(cls, a, "backlog", "cn1.server.backlog");
            nonNegative(cls, a, "shutdownTimeoutMillis", "cn1.server.shutdownTimeoutMillis");
        }
        a = cls.getClassAnnotation(SESSION);
        if (a != null) {
            oneOf(cls, a, "store", "cn1.session.store", "memory", "db");
            nonNegative(cls, a, "timeoutSeconds", "cn1.session.timeout");
            string(cls, a, "cookie", "cn1.session.cookie");
            oneOf(cls, a, "sameSite", "cn1.session.same-site", "Lax", "Strict", "None");
            oneOf(cls, a, "secure", "cn1.session.secure", "auto", "true", "false");
            string(cls, a, "namespace", "cn1.session.namespace");
        }
        a = cls.getClassAnnotation(DATA_SOURCE);
        if (a != null) {
            string(cls, a, "url", "cn1.datasource.url");
            positive(cls, a, "poolSize", "cn1.datasource.pool.size");
            nonNegative(cls, a, "borrowTimeoutMillis", "cn1.datasource.pool.borrowTimeoutMillis");
            nonNegative(cls, a, "busyTimeoutMillis", "cn1.datasource.busyTimeoutMillis");
        }
        a = cls.getClassAnnotation(STATIC_FILES);
        if (a != null) {
            string(cls, a, "root", "cn1.static.root");
            path(cls, a, "prefix", "cn1.static.prefix");
            string(cls, a, "index", "cn1.static.index");
            string(cls, a, "cacheControl", "cn1.static.cacheControl");
        }
        a = cls.getClassAnnotation(ENABLE_MANAGEMENT);
        if (a != null) {
            management = true;
            set(cls, "cn1.management.enabled", "true");
            path(cls, a, "path", "cn1.management.path");
        }
        a = cls.getClassAnnotation(ENABLE_MCP);
        if (a != null) {
            // Linked, not forced on: the endpoint's own default -- on when the
            // server has a tool to serve -- still decides, as without the
            // annotation.
            mcp = true;
            path(cls, a, "path", "cn1.mcp.path");
            List<String> origins = strings(a.get("allowedOrigins"));
            if (!origins.isEmpty()) {
                StringBuilder joined = new StringBuilder();
                for (String o : origins) {
                    if (o.indexOf(',') >= 0) {
                        ctx.error(cls, "@EnableMcpServer allowedOrigins names \"" + o
                                + "\"; an origin has no comma in it. List each one separately.");
                        return;
                    }
                    if (joined.length() > 0) {
                        joined.append(',');
                    }
                    joined.append(o);
                }
                set(cls, "cn1.mcp.allowedOrigins", joined.toString());
            }
        }
        a = cls.getClassAnnotation(RestControllerAnnotationProcessor.OPEN_TELEMETRY);
        if (a != null) {
            string(cls, a, "endpoint", "cn1.otel.endpoint");
            oneOf(cls, a, "protocol", "cn1.otel.protocol", "http/protobuf", "http/json");
            string(cls, a, "sampler", "cn1.otel.sampler");
            string(cls, a, "samplerArg", "cn1.otel.sampler.arg");
        }
    }

    private void string(AnnotatedClass cls, AnnotationValues a, String attribute, String key) {
        String v = a.getStringOrDefault(attribute, "").trim();
        if (v.length() > 0) {
            set(cls, key, v);
        }
    }

    private void path(AnnotatedClass cls, AnnotationValues a, String attribute, String key) {
        String v = a.getStringOrDefault(attribute, "").trim();
        if (v.length() == 0) {
            return;
        }
        if (!v.startsWith("/")) {
            ctx.error(cls, "@" + simpleName(a) + " " + attribute + " is \"" + v
                    + "\"; a path starts with /.");
            return;
        }
        set(cls, key, v);
    }

    private void oneOf(AnnotatedClass cls, AnnotationValues a, String attribute, String key,
                       String... allowed) {
        String v = a.getStringOrDefault(attribute, "").trim();
        if (v.length() == 0) {
            return;
        }
        for (String candidate : allowed) {
            if (candidate.equalsIgnoreCase(v)) {
                set(cls, key, candidate);
                return;
            }
        }
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < allowed.length; i++) {
            list.append(i == 0 ? "" : i == allowed.length - 1 ? " or " : ", ").append(allowed[i]);
        }
        ctx.error(cls, "@" + simpleName(a) + " " + attribute + " is \"" + v + "\"; it must be "
                + list + ".");
    }

    private void port(AnnotatedClass cls, AnnotationValues a) {
        int v = a.getIntOrDefault("port", -1);
        if (v == -1) {
            return;
        }
        if (v < 0 || v > 65535) {
            ctx.error(cls, "@ServerConfig port is " + v + "; a port is 0 to 65535.");
            return;
        }
        set(cls, "cn1.server.port", String.valueOf(v));
    }

    private void positive(AnnotatedClass cls, AnnotationValues a, String attribute, String key) {
        int v = a.getIntOrDefault(attribute, -1);
        if (v == -1) {
            return;
        }
        if (v < 1) {
            ctx.error(cls, "@" + simpleName(a) + " " + attribute + " is " + v
                    + "; it must be at least 1.");
            return;
        }
        set(cls, key, String.valueOf(v));
    }

    private void nonNegative(AnnotatedClass cls, AnnotationValues a, String attribute, String key) {
        int v = a.getIntOrDefault(attribute, -1);
        if (v == -1) {
            return;
        }
        if (v < 0) {
            ctx.error(cls, "@" + simpleName(a) + " " + attribute + " is " + v
                    + "; it can't be negative.");
            return;
        }
        set(cls, key, String.valueOf(v));
    }

    /// One key, from one class. Two classes giving the same key different values
    /// is refused rather than resolved by scan order, which nobody can see.
    private void set(AnnotatedClass cls, String key, String value) {
        String existing = values.get(key);
        if (existing != null && !existing.equals(value)) {
            ctx.error(cls, key + " is set to \"" + value + "\" here and to \"" + existing
                    + "\" on " + owners.get(key) + ". Keep one of them.");
            return;
        }
        values.put(key, value);
        owners.put(key, cls.getBinaryName());
    }

    private static String simpleName(AnnotationValues a) {
        String d = a.getDescriptor();
        int slash = d.lastIndexOf('/');
        return d.substring(slash + 1, d.length() - 1);
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                if (o instanceof String && ((String) o).trim().length() > 0) {
                    out.add(((String) o).trim());
                }
            }
        }
        return out;
    }
}
