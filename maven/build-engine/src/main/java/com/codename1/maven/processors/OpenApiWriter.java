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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Writes the OpenAPI 3.1 document of a backend module from its route table.
///
/// The router processor hands over one [Operation] per route it generated code for,
/// after it has resolved and checked every binding, so the document describes what
/// the server answers and nothing else. Schemas come from [BackendJsonCodecs], the
/// model the generated codecs are written from: a field the codec writes is a
/// property in the schema, under the same name, and nothing else is.
///
/// What the build cannot see is left out rather than guessed: an exception a
/// handler throws, an authentication scheme the `SecurityFilterChain` configures at
/// run time (see `cn1.openapi.security`), a `HttpServer.Response` a handler builds
/// by hand.
final class OpenApiWriter {
    /// One parameter of an operation.
    static final class Param {
        /// `path`, `query`, `header`, `body` or `part`.
        String in;
        String name;
        String type;
        boolean required;
        String defaultValue;
        /// The parameter's constraint annotations, or null.
        BackendJsonCodecs.ConstraintSource constraints;
    }

    /// One route.
    static final class Operation {
        String method;
        String path;
        String operationId;
        String tag;
        int status;
        /// Comma separated, or null for the defaults.
        String consumes;
        String produces;
        /// The method's return type, `void`, or a type the handler answers by hand.
        String returnType;
        /// The handler answers with a `HttpServer.Response` it built itself.
        boolean handBuilt;
        /// An MVC view: HTML.
        boolean view;
        /// Method security applies.
        boolean secured;
        final List<Param> params = new ArrayList<Param>();
    }

    private final BackendJsonCodecs codecs;
    private final Map<String, Object> components = new LinkedHashMap<String, Object>();
    private Map<String, Object> schemes = new LinkedHashMap<String, Object>();

    OpenApiWriter(BackendJsonCodecs codecs) {
        this.codecs = codecs;
    }

    /// The document, as JSON.
    ///
    /// @param title `info.title`
    /// @param version `info.version`
    /// @param security comma separated scheme names from `cn1.openapi.security`
    ///                 (`bearer`, `basic`, `cookie`, `apikey`), or null
    String write(String title, String version, List<Operation> operations, String security) {
        schemes = securitySchemes(security);
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("openapi", "3.1.0");
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("title", title);
        info.put("version", version);
        doc.put("info", info);
        Map<String, Object> paths = new LinkedHashMap<String, Object>();
        List<String> tags = new ArrayList<String>();
        for (Operation op : operations) {
            String path = templatePath(op.path);
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) paths.get(path);
            if (item == null) {
                item = new LinkedHashMap<String, Object>();
                paths.put(path, item);
            }
            String method = op.method.toLowerCase(java.util.Locale.ROOT);
            if (item.containsKey(method)) {
                // The router refuses two routes one request could match, so this
                // is two patterns that differ only in a variable's name or regex
                // ({id:[0-9]+} and {slug}) collapsing to one template. OpenAPI
                // cannot describe both under one key; dropping one silently
                // would publish a document missing an operation.
                throw new IllegalArgumentException(op.method + " " + path + " is described "
                        + "twice (" + ((Map<?, ?>) item.get(method)).get("operationId") + " and "
                        + op.operationId + "): OpenAPI paths ignore a variable's pattern, "
                        + "so these routes cannot both be documented");
            }
            item.put(method, operation(op));
            if (!tags.contains(op.tag)) {
                tags.add(op.tag);
            }
        }
        List<Object> tagList = new ArrayList<Object>();
        for (String t : tags) {
            Map<String, Object> tag = new LinkedHashMap<String, Object>();
            tag.put("name", t);
            tagList.add(tag);
        }
        if (!tagList.isEmpty()) {
            doc.put("tags", tagList);
        }
        doc.put("paths", paths);
        Map<String, Object> comps = new LinkedHashMap<String, Object>();
        if (!components.isEmpty()) {
            comps.put("schemas", components);
        }
        if (!schemes.isEmpty()) {
            comps.put("securitySchemes", schemes);
        }
        if (!comps.isEmpty()) {
            doc.put("components", comps);
        }
        StringBuilder sb = new StringBuilder();
        json(sb, doc, "");
        return sb.append('\n').toString();
    }

    /// `{id:[0-9]+}`, `{path:.+}` and `{*path}` as OpenAPI spells a path
    /// variable: `{id}`, `{path}`.
    static String templatePath(String pattern) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c != '{') {
                out.append(c);
                i++;
                continue;
            }
            int depth = 0;
            int end = i;
            for (; end < pattern.length(); end++) {
                char e = pattern.charAt(end);
                if (e == '{') {
                    depth++;
                } else if (e == '}' && --depth == 0) {
                    break;
                }
            }
            String inner = pattern.substring(i + 1, Math.min(end, pattern.length()));
            int colon = inner.indexOf(':');
            String name = colon < 0 ? inner : inner.substring(0, colon);
            if (name.startsWith("*")) {
                name = name.substring(1);
            }
            out.append('{').append(name.trim()).append('}');
            i = end + 1;
        }
        String s = out.toString();
        return s.length() == 0 ? "/" : s;
    }

    private Map<String, Object> operation(Operation op) {
        Map<String, Object> o = new LinkedHashMap<String, Object>();
        List<Object> tags = new ArrayList<Object>();
        tags.add(op.tag);
        o.put("tags", tags);
        o.put("operationId", op.operationId);
        if (op.secured && !schemes.isEmpty()) {
            // Per operation, not document-wide: a public route must not be shown
            // (and called by Swagger UI) as needing credentials. One entry per
            // scheme is an OR -- any configured scheme authenticates, which is
            // how the security layer tries them. Only method security is known
            // at build time; a route the filter chain guards by URL is not marked.
            List<Object> req = new ArrayList<Object>();
            for (String name : schemes.keySet()) {
                Map<String, Object> one = new LinkedHashMap<String, Object>();
                one.put(name, new ArrayList<Object>());
                req.add(one);
            }
            o.put("security", req);
        }
        List<Object> params = new ArrayList<Object>();
        Map<String, Object> bodySchema = null;
        boolean bodyRequired = false;
        Map<String, Object> parts = null;
        List<Object> partsRequired = new ArrayList<Object>();
        for (Param p : op.params) {
            Map<String, Object> schema = paramSchema(p);
            if ("body".equals(p.in)) {
                bodySchema = schema;
                bodyRequired = p.required;
                continue;
            }
            if ("part".equals(p.in)) {
                if (parts == null) {
                    parts = new LinkedHashMap<String, Object>();
                }
                if (!"java.lang.String".equals(p.type)) {
                    schema = new LinkedHashMap<String, Object>();
                    schema.put("type", "string");
                    schema.put("format", "binary");
                }
                parts.put(p.name, schema);
                if (p.required) {
                    partsRequired.add(p.name);
                }
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<String, Object>();
            param.put("name", p.name);
            param.put("in", p.in);
            boolean hasDefault = p.defaultValue != null && p.defaultValue.length() > 0;
            boolean required = "path".equals(p.in) || p.required && !hasDefault;
            if (p.constraints != null
                    && BackendJsonCodecs.applyConstraints(p.constraints, schema)) {
                required = true;
            }
            if (hasDefault) {
                schema.put("default", typedDefault(p.defaultValue, schema.get("type")));
            }
            param.put("required", Boolean.valueOf(required));
            param.put("schema", schema);
            params.add(param);
        }
        // OpenAPI requires every template variable to be declared. A method need
        // not bind one ("/orgs/{org}/items" without a @PathVariable org), so the
        // rest are declared as the plain strings the router matches. A catch-all
        // ({*rest}) matches across '/', which no OpenAPI path parameter can say;
        // it is documented as a single string segment.
        for (String var : templateVariables(op.path)) {
            boolean declared = false;
            for (Param p : op.params) {
                if ("path".equals(p.in) && var.equals(p.name)) {
                    declared = true;
                    break;
                }
            }
            if (!declared) {
                Map<String, Object> param = new LinkedHashMap<String, Object>();
                param.put("name", var);
                param.put("in", "path");
                param.put("required", Boolean.TRUE);
                Map<String, Object> str = new LinkedHashMap<String, Object>();
                str.put("type", "string");
                param.put("schema", str);
                params.add(param);
            }
        }
        if (!params.isEmpty()) {
            o.put("parameters", params);
        }
        if (bodySchema != null || parts != null) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            Map<String, Object> content = new LinkedHashMap<String, Object>();
            if (parts != null) {
                Map<String, Object> form = new LinkedHashMap<String, Object>();
                form.put("type", "object");
                form.put("properties", parts);
                if (!partsRequired.isEmpty()) {
                    form.put("required", partsRequired);
                }
                content.put("multipart/form-data", media(form));
                body.put("required", Boolean.valueOf(!partsRequired.isEmpty()));
            } else {
                for (String type : mediaTypes(op.consumes, "application/json")) {
                    content.put(type, media(bodySchema));
                }
                body.put("required", Boolean.valueOf(bodyRequired));
            }
            body.put("content", content);
            o.put("requestBody", body);
        }
        Map<String, Object> responses = new LinkedHashMap<String, Object>();
        Map<String, Object> ok = new LinkedHashMap<String, Object>();
        ok.put("description", reason(op.status));
        if (op.view) {
            Map<String, Object> content = new LinkedHashMap<String, Object>();
            Map<String, Object> html = new LinkedHashMap<String, Object>();
            html.put("type", "string");
            content.put("text/html", media(html));
            ok.put("content", content);
        } else if (!"void".equals(op.returnType) && op.status != 204) {
            Map<String, Object> schema = op.handBuilt ? new LinkedHashMap<String, Object>()
                    : codecs.jsonSchema(op.returnType, components);
            Map<String, Object> content = new LinkedHashMap<String, Object>();
            List<String> produced = mediaTypes(op.produces, op.handBuilt ? "*/*"
                    : "java.lang.String".equals(op.returnType) ? "text/plain"
                    : "application/json");
            for (String type : produced) {
                content.put(type, media(schema));
            }
            ok.put("content", content);
        }
        responses.put(String.valueOf(op.status), ok);
        if (op.status != 200 && op.handBuilt) {
            responses.put("default", describe("Whatever the handler answers"));
        }
        if (hasValidatedInput(op)) {
            responses.put("400", describe("Bad Request: a parameter or the body is "
                    + "missing, malformed or violates its constraints"));
        }
        if (op.secured) {
            responses.put("401", describe("Unauthorized"));
            responses.put("403", describe("Forbidden"));
        }
        o.put("responses", responses);
        return o;
    }

    /// Any bound input can be refused: a value that does not convert, a missing
    /// required one, a body that does not parse or violates a constraint.
    private static boolean hasValidatedInput(Operation op) {
        return !op.params.isEmpty();
    }

    /// The variable names of a path pattern, in order.
    static List<String> templateVariables(String pattern) {
        List<String> out = new ArrayList<String>();
        String t = templatePath(pattern);
        int i = t.indexOf('{');
        while (i >= 0) {
            int end = t.indexOf('}', i);
            if (end < 0) {
                break;
            }
            out.add(t.substring(i + 1, end));
            i = t.indexOf('{', end);
        }
        return out;
    }

    /// A default value as the schema's type: OpenAPI validators reject a string
    /// `"20"` as the default of an integer, and typed clients fail to compile it.
    static Object typedDefault(String value, Object type) {
        String v = value.trim();
        try {
            if ("integer".equals(type)) {
                return Long.valueOf(v);
            }
            if ("number".equals(type)) {
                Double d = Double.valueOf(v);
                // NaN and Infinity have no JSON spelling.
                return d.isNaN() || d.isInfinite() ? value : d;
            }
        } catch (NumberFormatException e) {
            return value;                // the router refuses it at run time anyway
        }
        if ("boolean".equals(type) && ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v))) {
            return Boolean.valueOf(v);
        }
        return value;
    }

    private Map<String, Object> paramSchema(Param p) {
        if ("java.util.List<java.lang.String>".equals(p.type) || "java.util.List".equals(p.type)
                || "java.lang.String[]".equals(p.type)) {
            Map<String, Object> s = new LinkedHashMap<String, Object>();
            s.put("type", "array");
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("type", "string");
            s.put("items", item);
            return s;
        }
        return codecs.jsonSchema(p.type, components);
    }

    private static Map<String, Object> describe(String text) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("description", text);
        return m;
    }

    private static Map<String, Object> media(Map<String, Object> schema) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("schema", schema);
        return m;
    }

    private static List<String> mediaTypes(String declared, String fallback) {
        List<String> out = new ArrayList<String>();
        if (declared != null) {
            for (String t : declared.split(",")) {
                if (t.trim().length() > 0) {
                    out.add(t.trim());
                }
            }
        }
        if (out.isEmpty()) {
            out.add(fallback);
        }
        return out;
    }

    private static Map<String, Object> securitySchemes(String names) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (names == null) {
            return out;
        }
        for (String raw : names.split(",")) {
            String n = raw.trim();
            Map<String, Object> s = new LinkedHashMap<String, Object>();
            if ("bearer".equalsIgnoreCase(n)) {
                s.put("type", "http");
                s.put("scheme", "bearer");
                s.put("bearerFormat", "JWT");
                out.put("bearerAuth", s);
            } else if ("basic".equalsIgnoreCase(n)) {
                s.put("type", "http");
                s.put("scheme", "basic");
                out.put("basicAuth", s);
            } else if ("cookie".equalsIgnoreCase(n)) {
                s.put("type", "apiKey");
                s.put("in", "cookie");
                s.put("name", "JSESSIONID");
                out.put("sessionCookie", s);
            } else if (n.regionMatches(true, 0, "apikey", 0, 6)) {
                int colon = n.indexOf(':');
                s.put("type", "apiKey");
                s.put("in", "header");
                s.put("name", colon < 0 ? "X-API-Key" : n.substring(colon + 1).trim());
                out.put("apiKey", s);
            }
        }
        return out;
    }

    private static String reason(int status) {
        switch (status) {
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 204: return "No Content";
            default: return "Status " + status;
        }
    }

    // ------------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------------

    static void json(StringBuilder sb, Object v, String indent) {
        if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            String inner = indent + "  ";
            sb.append("{\n");
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;
                sb.append(inner);
                string(sb, String.valueOf(e.getKey()));
                sb.append(": ");
                json(sb, e.getValue(), inner);
            }
            sb.append('\n').append(indent).append('}');
        } else if (v instanceof List) {
            List<?> l = (List<?>) v;
            if (l.isEmpty()) {
                sb.append("[]");
                return;
            }
            String inner = indent + "  ";
            sb.append("[\n");
            for (int i = 0; i < l.size(); i++) {
                sb.append(i == 0 ? "" : ",\n").append(inner);
                json(sb, l.get(i), inner);
            }
            sb.append('\n').append(indent).append(']');
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v == null) {
            sb.append("null");
        } else {
            string(sb, v.toString());
        }
    }

    private static void string(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = hex.length(); k < 4; k++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
