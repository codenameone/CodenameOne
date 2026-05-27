/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */
package com.codename1.maven;

import com.codename1.io.JSONParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Build-time OpenAPI 3.x -> Codename One Java client generator.
 *
 * <h2>Input</h2>
 *
 * An OpenAPI 3.x JSON document. YAML is not supported (avoid pulling SnakeYAML
 * into the plugin's classpath); convert with {@code yq} or an online tool
 * upstream. The spec may live at an {@code http://} / {@code https://} URL or
 * on the local filesystem.
 *
 * <h2>Output</h2>
 *
 * Generated under the configured output directory:
 *
 * <ul>
 *   <li>{@code <basePackage>/model/<Schema>.java} -- one POJO per
 *       {@code components.schemas} entry, annotated with {@code @Mapped} and
 *       {@code @JsonProperty} on each field. The generated POJOs work directly
 *       with {@code Rest.fetchAsMapped(...)} on the runtime side.</li>
 *   <li>{@code <basePackage>/<Tag>Api.java} -- one Api class per OpenAPI tag,
 *       with one public method per operation tagged with that tag.</li>
 * </ul>
 *
 * <h2>Method shape</h2>
 *
 * Each generated method takes the path parameters first, then required query
 * parameters, then the optional request body, then a {@code String bearerToken}
 * (or {@code null} to skip the {@code Authorization} header), then an
 * {@code OnComplete<Response<T>>} callback. The body is a single chained
 * call to {@code Rest.<verb>(url)...fetchAsMapped(Type.class, callback)}.
 *
 * <pre>{@code
 * // From the Petstore spec:
 * public void getPetById(long petId, String bearerToken, OnComplete<Response<Pet>> cb) {
 *     Rest.get(baseUrl + "/pet/" + petId)
 *         .acceptJson()
 *         .header("Authorization", bearerToken == null ? null : "Bearer " + bearerToken)
 *         .fetchAsMapped(Pet.class, cb);
 * }
 * }</pre>
 *
 * <h2>Scope (MVP)</h2>
 *
 * <ul>
 *   <li>HTTP verbs: GET, POST, PUT, DELETE, PATCH.</li>
 *   <li>Parameter locations: path, query.</li>
 *   <li>Request bodies: {@code application/json} -> serialised via
 *       {@code Mappers.toJson(body)} before being passed to {@code Rest#body}.</li>
 *   <li>Schemas: plain object types with {@code properties}; {@code $ref}
 *       resolution; {@code array}/{@code integer}/{@code number}/{@code string}/
 *       {@code boolean} primitives. {@code oneOf}/{@code anyOf}/{@code allOf}
 *       are flattened to {@code Object} (caller-cast) -- a richer follow-up
 *       can specialise these.</li>
 *   <li>Response types: the first JSON success response (2xx) wins; everything
 *       else gets a {@code Map} fallback.</li>
 * </ul>
 *
 * Header parameters, multipart upload, cookie params, file upload, security
 * schemes beyond bearer-token, oneOf/anyOf are out of scope for this MVP and
 * documented as deferred in {@code IMPROVEMENT_PLAN.md}.
 */
final class OpenApiCodegen {

    private final File outputDir;
    private final String basePackage;
    private final org.apache.maven.plugin.logging.Log log;
    private final Map<String, Object> spec;
    private final String modelPackage;
    private final Map<String, Object> schemas;

    /** Tag -> list of operations associated with that tag. */
    private final Map<String, List<OperationInfo>> opsByTag = new TreeMap<String, List<OperationInfo>>();

    OpenApiCodegen(File outputDir, String basePackage,
                   org.apache.maven.plugin.logging.Log log, Map<String, Object> spec) {
        this.outputDir = outputDir;
        this.basePackage = basePackage;
        this.modelPackage = basePackage + ".model";
        this.log = log;
        this.spec = spec;
        Object components = spec.get("components");
        Object schemasObj = components instanceof Map ? ((Map<?, ?>) components).get("schemas") : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> s = schemasObj instanceof Map ? (Map<String, Object>) schemasObj : Collections.<String, Object>emptyMap();
        this.schemas = s;
    }

    /** Public entry point: parses spec into operations, then emits files. */
    void generate() throws IOException {
        // Index every operation by tag.
        Object pathsObj = spec.get("paths");
        if (!(pathsObj instanceof Map)) {
            log.warn("OpenAPI spec has no `paths` -- nothing to generate.");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) pathsObj;
        for (Map.Entry<String, Object> e : paths.entrySet()) {
            String path = e.getKey();
            if (!(e.getValue() instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) e.getValue();
            for (String verb : new String[]{"get", "post", "put", "delete", "patch"}) {
                Object opObj = pathItem.get(verb);
                if (!(opObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> op = (Map<String, Object>) opObj;
                OperationInfo info = buildOperation(verb, path, op, pathItem);
                String tag = primaryTag(op);
                List<OperationInfo> list = opsByTag.get(tag);
                if (list == null) {
                    list = new ArrayList<OperationInfo>();
                    opsByTag.put(tag, list);
                }
                list.add(info);
            }
        }

        // Emit models for every schema (whether referenced by an op or not).
        File modelDir = new File(outputDir, modelPackage.replace('.', '/'));
        modelDir.mkdirs();
        for (Map.Entry<String, Object> e : schemas.entrySet()) {
            if (!(e.getValue() instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = (Map<String, Object>) e.getValue();
            emitModel(modelDir, e.getKey(), schema);
        }

        // Emit one Api class per tag.
        File apiDir = new File(outputDir, basePackage.replace('.', '/'));
        apiDir.mkdirs();
        for (Map.Entry<String, List<OperationInfo>> e : opsByTag.entrySet()) {
            emitApi(apiDir, e.getKey(), e.getValue());
        }

        log.info("Generated " + schemas.size() + " models and "
                + opsByTag.size() + " Api classes under " + outputDir);
    }

    // ---- parser / model ----

    static final class OperationInfo {
        final String verb;
        final String path;
        final String operationId;
        final String summary;
        final List<ParamInfo> pathParams = new ArrayList<ParamInfo>();
        final List<ParamInfo> queryParams = new ArrayList<ParamInfo>();
        boolean hasRequestBody;
        String requestBodyType;       // Java type, or "Map" for unrefed JSON
        String responseType;          // Java type, or "Map" for unrefed JSON
        boolean responseIsList;
        OperationInfo(String verb, String path) { this.verb = verb; this.path = path; this.operationId = null; this.summary = null; }
        OperationInfo(String verb, String path, String operationId, String summary) {
            this.verb = verb; this.path = path; this.operationId = operationId; this.summary = summary;
        }
    }

    static final class ParamInfo {
        final String name;
        final String javaType;
        final boolean required;
        ParamInfo(String name, String javaType, boolean required) {
            this.name = name; this.javaType = javaType; this.required = required;
        }
    }

    private OperationInfo buildOperation(String verb, String path,
                                         Map<String, Object> op, Map<String, Object> pathItem) {
        String operationId = (String) op.get("operationId");
        if (operationId == null) {
            operationId = synthesizeOperationId(verb, path);
        }
        String summary = (String) op.get("summary");
        OperationInfo info = new OperationInfo(verb, path, sanitizeIdentifier(operationId), summary);

        // Parameters: union of path-level params and operation-level params.
        List<Object> combined = new ArrayList<Object>();
        Object pp = pathItem.get("parameters");
        if (pp instanceof List) combined.addAll((List<?>) pp);
        Object op2 = op.get("parameters");
        if (op2 instanceof List) combined.addAll((List<?>) op2);
        for (Object pObj : combined) {
            if (!(pObj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> p = (Map<String, Object>) pObj;
            Object resolved = resolveRef(p);
            @SuppressWarnings("unchecked")
            Map<String, Object> pr = (Map<String, Object>) resolved;
            String in = (String) pr.get("in");
            String pname = (String) pr.get("name");
            boolean required = Boolean.TRUE.equals(pr.get("required"));
            String javaType = paramTypeJava(pr);
            ParamInfo pi = new ParamInfo(sanitizeIdentifier(pname), javaType, required);
            if ("path".equals(in)) {
                info.pathParams.add(pi);
            } else if ("query".equals(in)) {
                info.queryParams.add(pi);
            }
            // header / cookie params silently ignored in MVP
        }

        // Request body: only application/json. If present, set the type.
        Object rb = op.get("requestBody");
        if (rb instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) rb;
            Object content = body.get("content");
            if (content instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cmap = (Map<String, Object>) content;
                Object jsonEntry = cmap.get("application/json");
                if (jsonEntry instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> je = (Map<String, Object>) jsonEntry;
                    Object schema = je.get("schema");
                    info.hasRequestBody = true;
                    info.requestBodyType = schemaToJavaType(schema, /*listIsArrayList*/ true);
                }
            }
        }

        // Response: pick the first 2xx with application/json.
        Object responses = op.get("responses");
        info.responseType = "java.util.Map<String, Object>";
        if (responses instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> r = (Map<String, Object>) responses;
            for (Map.Entry<String, Object> re : r.entrySet()) {
                String code = re.getKey();
                if (code != null && code.startsWith("2") && re.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resp = (Map<String, Object>) re.getValue();
                    Object content = resp.get("content");
                    if (content instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cmap = (Map<String, Object>) content;
                        Object jsonEntry = cmap.get("application/json");
                        if (jsonEntry instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> je = (Map<String, Object>) jsonEntry;
                            Object schema = je.get("schema");
                            if (schema instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> sm = (Map<String, Object>) schema;
                                if ("array".equals(sm.get("type"))) {
                                    info.responseIsList = true;
                                    info.responseType = schemaToJavaType(sm.get("items"), false);
                                } else {
                                    info.responseType = schemaToJavaType(schema, false);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return info;
    }

    /// Resolves {@code $ref} pointers within the spec to their target object.
    /// Only {@code #/components/...} refs are supported; external refs are
    /// returned as-is and produce a {@code Map} typing fallback.
    private Object resolveRef(Map<String, Object> maybeRef) {
        Object ref = maybeRef.get("$ref");
        if (!(ref instanceof String)) return maybeRef;
        String r = (String) ref;
        if (!r.startsWith("#/")) return maybeRef;
        String[] parts = r.substring(2).split("/");
        Object cur = spec;
        for (String p : parts) {
            if (cur instanceof Map) {
                cur = ((Map<?, ?>) cur).get(p);
            } else {
                return maybeRef;
            }
        }
        return cur instanceof Map ? cur : maybeRef;
    }

    private String paramTypeJava(Map<String, Object> param) {
        Object schema = param.get("schema");
        if (schema instanceof Map) return schemaToJavaType(schema, false);
        return "String";
    }

    /// Maps an OpenAPI schema node to a Java type name suitable for use in
    /// generated source. Composition keywords (allOf/oneOf/anyOf) collapse to
    /// {@code Object} -- callers can cast. Unknown types become {@code String}.
    @SuppressWarnings("unchecked")
    String schemaToJavaType(Object schemaObj, boolean listIsArrayList) {
        if (schemaObj == null) return "Object";
        if (!(schemaObj instanceof Map)) return "Object";
        Map<String, Object> schema = (Map<String, Object>) schemaObj;
        // $ref short-circuit: resolve to a class name under modelPackage.
        Object ref = schema.get("$ref");
        if (ref instanceof String) {
            String r = (String) ref;
            int slash = r.lastIndexOf('/');
            if (slash >= 0 && r.startsWith("#/components/schemas/")) {
                String name = r.substring(slash + 1);
                return modelPackage + "." + sanitizeClassName(name);
            }
            return "Object";
        }
        Object type = schema.get("type");
        if (type instanceof String) {
            String t = (String) type;
            if ("integer".equals(t)) {
                Object fmt = schema.get("format");
                if ("int64".equals(fmt)) return "long";
                return "int";
            }
            if ("number".equals(t)) {
                Object fmt = schema.get("format");
                if ("float".equals(fmt)) return "float";
                return "double";
            }
            if ("boolean".equals(t)) return "boolean";
            if ("string".equals(t)) return "String";
            if ("array".equals(t)) {
                String element = schemaToJavaType(schema.get("items"), false);
                return "java.util.List<" + boxIfPrimitive(element) + ">";
            }
            // "object" fallthrough to Map
            return "java.util.Map<String, Object>";
        }
        // Composition keywords: collapse to Object.
        if (schema.containsKey("allOf") || schema.containsKey("oneOf") || schema.containsKey("anyOf")) {
            return "Object";
        }
        return "java.util.Map<String, Object>";
    }

    private String primaryTag(Map<String, Object> op) {
        Object tags = op.get("tags");
        if (tags instanceof List && !((List<?>) tags).isEmpty()) {
            Object first = ((List<?>) tags).get(0);
            if (first instanceof String) return sanitizeClassName((String) first);
        }
        return "Default";
    }

    // ---- emit ----

    private void emitModel(File dir, String name, Map<String, Object> schema) throws IOException {
        String className = sanitizeClassName(name);
        StringBuilder sb = new StringBuilder();
        sb.append("/* Generated by cn1:generate-openapi-client. DO NOT EDIT. */\n");
        sb.append("package ").append(modelPackage).append(";\n\n");
        sb.append("import com.codename1.annotations.JsonProperty;\n");
        sb.append("import com.codename1.annotations.Mapped;\n\n");
        sb.append("@Mapped\n");
        sb.append("public class ").append(className).append(" {\n");

        Object propsObj = schema.get("properties");
        if (propsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) propsObj;
            for (Map.Entry<String, Object> e : props.entrySet()) {
                String propName = e.getKey();
                String javaType = schemaToJavaType(e.getValue(), false);
                String javaName = sanitizeIdentifier(propName);
                sb.append("    @JsonProperty(\"").append(escapeJavaString(propName)).append("\")\n");
                sb.append("    public ").append(javaType).append(" ").append(javaName).append(";\n");
            }
        }

        sb.append("}\n");
        writeFile(new File(dir, className + ".java"), sb.toString());
    }

    private void emitApi(File dir, String tag, List<OperationInfo> ops) throws IOException {
        String className = sanitizeClassName(tag) + "Api";
        StringBuilder sb = new StringBuilder();
        sb.append("/* Generated by cn1:generate-openapi-client. DO NOT EDIT. */\n");
        sb.append("package ").append(basePackage).append(";\n\n");
        sb.append("import com.codename1.io.JSONWriter;\n");
        sb.append("import com.codename1.io.rest.Rest;\n");
        sb.append("import com.codename1.io.rest.RequestBuilder;\n");
        sb.append("import com.codename1.io.rest.Response;\n");
        sb.append("import com.codename1.util.OnComplete;\n\n");
        sb.append("public class ").append(className).append(" {\n");
        sb.append("    private final String baseUrl;\n\n");
        sb.append("    public ").append(className).append("(String baseUrl) {\n");
        sb.append("        this.baseUrl = stripTrailing(baseUrl);\n");
        sb.append("    }\n\n");

        for (OperationInfo op : ops) {
            emitOperationMethod(sb, op);
        }

        // Footer helper.
        sb.append("    private static String stripTrailing(String s) {\n");
        sb.append("        if (s == null) return \"\";\n");
        sb.append("        while (s.endsWith(\"/\")) { s = s.substring(0, s.length() - 1); }\n");
        sb.append("        return s;\n");
        sb.append("    }\n");
        sb.append("}\n");
        writeFile(new File(dir, className + ".java"), sb.toString());
    }

    private void emitOperationMethod(StringBuilder sb, OperationInfo op) {
        String javaResponseType = op.responseIsList
                ? "java.util.List<" + boxIfPrimitive(op.responseType) + ">"
                : op.responseType;

        sb.append("    /// ").append(op.summary == null ? op.operationId : op.summary).append("\n");
        sb.append("    /// HTTP ").append(op.verb.toUpperCase(Locale.ROOT)).append(" ").append(op.path).append("\n");
        sb.append("    public void ").append(op.operationId).append("(");
        boolean first = true;
        for (ParamInfo p : op.pathParams) {
            if (!first) sb.append(", ");
            sb.append(p.javaType).append(" ").append(p.name);
            first = false;
        }
        for (ParamInfo p : op.queryParams) {
            if (!first) sb.append(", ");
            sb.append(p.javaType).append(" ").append(p.name);
            first = false;
        }
        if (op.hasRequestBody) {
            if (!first) sb.append(", ");
            sb.append(op.requestBodyType).append(" body");
            first = false;
        }
        if (!first) sb.append(", ");
        sb.append("String bearerToken, OnComplete<Response<").append(boxIfPrimitive(javaResponseType)).append(">> callback) {\n");

        // Build URL: substitute path params, then append query string.
        sb.append("        String url = baseUrl + \"").append(interpolatePath(op.path, op.pathParams)).append("\";\n");
        sb.append("        RequestBuilder rb = Rest.").append(op.verb.toLowerCase(Locale.ROOT)).append("(url).acceptJson();\n");
        for (ParamInfo p : op.queryParams) {
            sb.append("        if (").append(p.name).append(" != null) rb = rb.queryParam(\"")
              .append(escapeJavaString(p.name)).append("\", String.valueOf(").append(p.name).append("));\n");
        }
        sb.append("        if (bearerToken != null) rb = rb.header(\"Authorization\", \"Bearer \" + bearerToken);\n");
        if (op.hasRequestBody) {
            sb.append("        rb = rb.header(\"Content-Type\", \"application/json\").jsonContent().body(\n");
            sb.append("                com.codename1.mapping.Mappers.toJson(body));\n");
        }

        // Fetch by shape. A response type is "mapped" (typed POJO) only
        // when it resolves to a generated model class under modelPackage;
        // primitive / Map / List<primitive> responses go through the
        // untyped fetchAsJsonMap / fetchAsJsonList path and the caller
        // does its own un-marshaling.
        boolean responseIsModel = op.responseType.startsWith(modelPackage + ".");
        if (op.responseIsList) {
            if (responseIsModel) {
                sb.append("        rb.fetchAsMappedList(").append(op.responseType).append(".class, callback);\n");
            } else {
                sb.append("        rb.fetchAsJsonList((OnComplete) callback);\n");
            }
        } else if (responseIsModel) {
            sb.append("        rb.fetchAsMapped(").append(op.responseType).append(".class, callback);\n");
        } else {
            sb.append("        rb.fetchAsJsonMap((OnComplete) callback);\n");
        }
        sb.append("    }\n\n");
    }

    private String interpolatePath(String path, List<ParamInfo> pathParams) {
        // Replace {name} with a Java string-concat segment.
        String out = path;
        for (ParamInfo p : pathParams) {
            String placeholder = "{" + originalNameFor(p.name, path) + "}";
            int idx = out.indexOf(placeholder);
            if (idx < 0) continue;
            String before = out.substring(0, idx);
            String after = out.substring(idx + placeholder.length());
            out = before + "\" + " + p.name + " + \"" + after;
        }
        return out;
    }

    /// Looks for the original (un-sanitized) parameter name in the path
    /// placeholders. Path placeholders use the OpenAPI declared name which
    /// is preserved verbatim; sanitizeIdentifier may have transformed the
    /// Java identifier.
    private String originalNameFor(String sanitizedName, String path) {
        // Quick path: most OpenAPI parameter names are already Java-identifier-safe.
        if (path.contains("{" + sanitizedName + "}")) return sanitizedName;
        // Otherwise scan placeholders and pick the one whose sanitized form matches.
        int i = 0;
        while ((i = path.indexOf('{', i)) >= 0) {
            int end = path.indexOf('}', i);
            if (end < 0) break;
            String inner = path.substring(i + 1, end);
            if (sanitizeIdentifier(inner).equals(sanitizedName)) return inner;
            i = end + 1;
        }
        return sanitizedName;
    }

    // ---- name sanitization ----

    /// Strips characters that would make a name invalid as a Java identifier,
    /// upper-cases the first letter to match Java class-name convention.
    static String sanitizeClassName(String s) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || c == ' ' || c == '.') { upper = true; continue; }
            if (!Character.isJavaIdentifierPart(c)) continue;
            if (sb.length() == 0 && !Character.isJavaIdentifierStart(c)) {
                sb.append('_');
            }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        if (sb.length() == 0) return "Anonymous";
        return sb.toString();
    }

    static String sanitizeIdentifier(String s) {
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || c == ' ' || c == '.') { upperNext = true; continue; }
            if (!Character.isJavaIdentifierPart(c)) continue;
            if (sb.length() == 0 && !Character.isJavaIdentifierStart(c)) {
                sb.append('_');
            }
            sb.append(upperNext ? Character.toUpperCase(c) : c);
            upperNext = false;
        }
        if (sb.length() == 0) return "anonymous";
        // Lowercase the first char (camelCase for methods/fields).
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        // Reserved-word collision: append underscore.
        String word = sb.toString();
        if (isJavaReservedWord(word)) return word + "_";
        return word;
    }

    private static boolean isJavaReservedWord(String s) {
        return s.equals("abstract") || s.equals("assert") || s.equals("boolean") || s.equals("break")
            || s.equals("byte") || s.equals("case") || s.equals("catch") || s.equals("char")
            || s.equals("class") || s.equals("const") || s.equals("continue") || s.equals("default")
            || s.equals("do") || s.equals("double") || s.equals("else") || s.equals("enum")
            || s.equals("extends") || s.equals("final") || s.equals("finally") || s.equals("float")
            || s.equals("for") || s.equals("goto") || s.equals("if") || s.equals("implements")
            || s.equals("import") || s.equals("instanceof") || s.equals("int") || s.equals("interface")
            || s.equals("long") || s.equals("native") || s.equals("new") || s.equals("null")
            || s.equals("package") || s.equals("private") || s.equals("protected") || s.equals("public")
            || s.equals("return") || s.equals("short") || s.equals("static") || s.equals("strictfp")
            || s.equals("super") || s.equals("switch") || s.equals("synchronized") || s.equals("this")
            || s.equals("throw") || s.equals("throws") || s.equals("transient") || s.equals("true")
            || s.equals("false") || s.equals("try") || s.equals("void") || s.equals("volatile")
            || s.equals("while") || s.equals("record");
    }

    private static String synthesizeOperationId(String verb, String path) {
        StringBuilder sb = new StringBuilder(verb);
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' || c == '{' || c == '}' || c == '-' || c == '_') { upper = true; continue; }
            if (!Character.isJavaIdentifierPart(c)) continue;
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    /// Java requires reference types for type parameters (`List<int>` is
    /// illegal). Box primitive type names.
    static String boxIfPrimitive(String type) {
        if (type == null) return "Object";
        if (type.equals("int")) return "Integer";
        if (type.equals("long")) return "Long";
        if (type.equals("double")) return "Double";
        if (type.equals("float")) return "Float";
        if (type.equals("boolean")) return "Boolean";
        if (type.equals("byte")) return "Byte";
        if (type.equals("short")) return "Short";
        if (type.equals("char")) return "Character";
        return type;
    }

    private static String escapeJavaString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    private void writeFile(File f, String content) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
    }

    // ---- spec loading helpers (also used by the mojo) ----

    /// Loads an OpenAPI spec from `specLocation`, which is either a URL
    /// (http / https) or a local file path. Returns the parsed JSON
    /// document as a Map.
    static Map<String, Object> loadSpec(String specLocation) throws IOException {
        Reader reader;
        if (specLocation.startsWith("http://") || specLocation.startsWith("https://")) {
            URL url = new URL(specLocation);
            InputStream is = url.openStream();
            reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        } else {
            File f = new File(specLocation);
            if (!f.exists()) {
                throw new IOException("OpenAPI spec not found: " + specLocation);
            }
            reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8);
        }
        try {
            return new JSONParser().parseJSON(reader);
        } finally {
            reader.close();
        }
    }
}
