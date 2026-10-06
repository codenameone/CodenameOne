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
import java.util.Properties;
import java.util.TreeSet;

/// What the generated entry point passes the runtime about its settings: the
/// module's `application.properties`, compiled in as the bottom layer of its
/// configuration, the exporter settings on `@OpenTelemetry`, and whether to link
/// the management and MCP endpoints at all.
///
/// Compiling the file in is what lets a native binary run with nothing beside
/// it -- a container built FROM SCRATCH has no file next to the binary -- while a
/// file or the environment present at run time still overrides every key, since
/// the compiled layer sits under both. Only the base file is compiled: a profile
/// file describes a different deployment, and baking `application-dev.properties`
/// into a production binary would make the dev settings its defaults.
final class BackendSettings {
    /// The owner recorded for a key the properties file set.
    private static final String FILE = "application.properties";
    /// Key and value pairs, in the order they were found.
    final Map<String, String> values = new LinkedHashMap<String, String>();
    /// Which class set each key, for a conflict's message.
    private final Map<String, String> owners = new LinkedHashMap<String, String>();
    /// Whether the build asked for the management endpoints.
    boolean management;
    /// Whether the build asked for the MCP endpoint, tools aside.
    boolean mcp;
    /// Whether the build asked for the security layer's own tables:
    /// `cn1.security.schema.enabled=true`.
    boolean securitySchema;

    private final ProcessorContext ctx;

    private BackendSettings(ProcessorContext ctx) {
        this.ctx = ctx;
    }

    /// Reads the module's `application.properties` and the settings on
    /// `@OpenTelemetry`, reporting conflicts through `ctx`.
    static BackendSettings resolve(ProcessorContext ctx) {
        BackendSettings out = new BackendSettings(ctx);
        Properties file = RestControllerAnnotationProcessor.baseApplicationProperties(ctx);
        if (file != null) {
            // Sorted, so the generated entry point is the same on every build.
            for (String key : new TreeSet<String>(file.stringPropertyNames())) {
                out.values.put(key, file.getProperty(key));
                out.owners.put(key, FILE);
            }
        }
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
        out.securitySchema |= RestControllerAnnotationProcessor.applicationPropertyTrue(ctx,
                "cn1.security.schema.enabled");
        // Moving the endpoint or naming its origins is asking for it; the
        // endpoint's own default -- on when the server has a tool to serve --
        // still decides whether it answers.
        out.mcp |= RestControllerAnnotationProcessor.applicationPropertyTrue(ctx, "cn1.mcp.enabled")
                || RestControllerAnnotationProcessor.applicationPropertyKnown(ctx, "cn1.mcp.path")
                || RestControllerAnnotationProcessor.applicationPropertyKnown(ctx,
                        "cn1.mcp.allowedOrigins");
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
        AnnotationValues a = cls.getClassAnnotation(RestControllerAnnotationProcessor.OPEN_TELEMETRY);
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

    /// One key, from one class. Two classes giving the same key different values
    /// is refused rather than resolved by scan order, which nobody can see. A key
    /// `application.properties` already sets keeps the file's value: the file
    /// overrides an annotation at run time too, so the build agrees with it.
    private void set(AnnotatedClass cls, String key, String value) {
        if (FILE.equals(owners.get(key))) {
            return;
        }
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
}
