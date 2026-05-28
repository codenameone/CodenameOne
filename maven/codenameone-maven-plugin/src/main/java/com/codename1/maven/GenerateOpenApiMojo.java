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
package com.codename1.maven;

import com.codename1.io.JSONParser;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Generates user-edited Codename One REST client sources from an
/// OpenAPI 3.x JSON specification.
///
/// Invocation:
///
/// ```
/// mvn cn1:generate-openapi -Dcn1.openapi.spec=petstore.json \
///                          -Dcn1.openapi.basePackage=com.example.petstore
/// ```
///
/// (You can also pass the two as positional arguments after the goal name --
/// when present they are read off `pluginContext` / system properties --
/// see the developer-guide appendix for the user-facing form.)
///
/// Outputs:
///
/// - `<basePackage>.model.<Schema>` -- one `@Mapped` record (Java 17+ target)
///   or class (Java 8 target) per `components.schemas` entry, with
///   `@JsonProperty` carrying the original spec name when sanitization
///   renames a Java identifier.
/// - `<basePackage>.<Tag>Api` -- one `@RestClient`-annotated interface per
///   OpenAPI tag, with one method per operation. Each method's parameters
///   come in the order: path params, query params, header params, body,
///   bearer-token header, OnComplete callback. A `static <Self> of(String
///   baseUrl)` factory wires the interface to the runtime registry.
///
/// Identical schemas (same property name + Java type list, same order)
/// collapse onto a single record/class to avoid an explosion of duplicates.
@Mojo(name = "generate-openapi",
      defaultPhase = LifecyclePhase.NONE,
      requiresProject = true,
      threadSafe = true)
public class GenerateOpenApiMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /// Path or URL to the OpenAPI JSON spec. YAML is not supported -- use
    /// `yq` upstream. Override via `-Dcn1.openapi.spec=...`.
    @Parameter(property = "cn1.openapi.spec")
    private String spec;

    /// Java base package the generated sources are emitted under. Override
    /// via `-Dcn1.openapi.basePackage=...`.
    @Parameter(property = "cn1.openapi.basePackage")
    private String basePackage;

    /// Output directory for the generated sources. Defaults to
    /// `${project.basedir}/src/main/java` because the emitted code is
    /// user-edited (records / classes / @RestClient interfaces live in the
    /// project's source tree, not under `target/generated-sources`).
    @Parameter(property = "cn1.openapi.outputDirectory",
            defaultValue = "${project.basedir}/src/main/java")
    private File outputDirectory;

    /// When `true` (default) existing files at the destination are
    /// overwritten. Pass `-Dcn1.openapi.overwrite=false` to keep your
    /// hand-edits and only emit missing files.
    @Parameter(property = "cn1.openapi.overwrite", defaultValue = "true")
    private boolean overwrite;

    /// First positional argument fallback. Maven CLI doesn't expose
    /// positional goal arguments directly so we accept the spec / package
    /// via system properties (`-Dcn1.openapi.spec=...`,
    /// `-Dcn1.openapi.basePackage=...`) or via inline `<spec>` configuration
    /// in the POM. Documented alternatively in the developer guide.
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String effectiveSpec = effectiveSpec();
        String effectivePackage = effectiveBasePackage();
        if (effectiveSpec == null || effectiveSpec.length() == 0) {
            throw new MojoFailureException(
                    "No OpenAPI spec supplied. Pass -Dcn1.openapi.spec=<path-or-url> "
                            + "or configure <spec> in the plugin block.");
        }
        if (effectivePackage == null || effectivePackage.length() == 0) {
            throw new MojoFailureException(
                    "No base package supplied. Pass -Dcn1.openapi.basePackage=<pkg> "
                            + "or configure <basePackage> in the plugin block.");
        }

        if (effectiveSpec.endsWith(".yaml") || effectiveSpec.endsWith(".yml")) {
            throw new MojoFailureException(
                    "OpenAPI YAML is not supported. Convert with `yq -o json " + effectiveSpec
                            + " > spec.json` and re-run the goal against the JSON output.");
        }

        Map<String, Object> document;
        try {
            document = loadSpec(effectiveSpec);
        } catch (IOException ioe) {
            throw new MojoFailureException("Could not load OpenAPI spec at "
                    + effectiveSpec + ": " + ioe.getMessage(), ioe);
        }

        int target = detectJavaTarget();
        boolean emitRecords = target >= 17;
        getLog().info("cn1:generate-openapi target=" + target + " emitRecords=" + emitRecords
                + " basePackage=" + effectivePackage);

        Generator gen = new Generator(document, effectivePackage, outputDirectory, overwrite,
                emitRecords, getLog());
        try {
            gen.run();
        } catch (IOException ioe) {
            throw new MojoExecutionException("Failed to write generated sources: "
                    + ioe.getMessage(), ioe);
        }
    }

    private String effectiveSpec() {
        if (spec != null && spec.length() > 0) return spec;
        return System.getProperty("cn1.openapi.spec");
    }

    private String effectiveBasePackage() {
        if (basePackage != null && basePackage.length() > 0) return basePackage;
        return System.getProperty("cn1.openapi.basePackage");
    }

    private int detectJavaTarget() {
        // Prefer maven.compiler.release if present, otherwise maven.compiler.target.
        String release = null, targetProp = null;
        if (project != null && project.getProperties() != null) {
            release = project.getProperties().getProperty("maven.compiler.release");
            targetProp = project.getProperties().getProperty("maven.compiler.target");
        }
        if (release == null) release = System.getProperty("maven.compiler.release");
        if (targetProp == null) targetProp = System.getProperty("maven.compiler.target");
        return parseJavaVersion(release != null ? release : targetProp);
    }

    /// Parses a `maven.compiler.target` style version. Returns `8` for
    /// `1.8` or null inputs so callers default to the classic POJO path.
    static int parseJavaVersion(String s) {
        if (s == null) return 8;
        s = s.trim();
        if (s.length() == 0) return 8;
        if (s.startsWith("1.")) s = s.substring(2);
        // Strip qualifiers like "17-LTS" / "21-ea".
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        if (i == 0) return 8;
        try {
            return Integer.parseInt(s.substring(0, i));
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    /// Loads the spec document. Supports `http://`, `https://`, and local
    /// file paths. JSON only -- YAML is rejected by `execute`.
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

    // ----------------------------------------------------------------
    // Generator
    // ----------------------------------------------------------------

    /// Stateful generator -- parses the spec, accumulates schema + API
    /// models, then writes the output files. Package-private for direct
    /// unit testing without spinning up a Maven session.
    static final class Generator {
        private final Map<String, Object> spec;
        private final String basePackage;
        private final String modelPackage;
        private final File outputDir;
        private final boolean overwrite;
        private final boolean emitRecords;
        private final org.apache.maven.plugin.logging.Log log;
        private final Map<String, Object> schemas;
        /// Tag -> list of operations.
        private final TreeMap<String, List<OperationInfo>> opsByTag = new TreeMap<String, List<OperationInfo>>();
        /// Schema name -> SchemaInfo accumulator.
        private final LinkedHashMap<String, SchemaInfo> schemaByName = new LinkedHashMap<String, SchemaInfo>();
        /// Shape-hash -> canonical SchemaInfo so identical shapes collapse.
        private final LinkedHashMap<String, SchemaInfo> shapeIndex = new LinkedHashMap<String, SchemaInfo>();
        /// Schema name -> canonical schema name (post-unification).
        private final Map<String, String> nameAliases = new LinkedHashMap<String, String>();

        Generator(Map<String, Object> spec, String basePackage, File outputDir, boolean overwrite,
                  boolean emitRecords, org.apache.maven.plugin.logging.Log log) {
            this.spec = spec;
            this.basePackage = basePackage;
            this.modelPackage = basePackage + ".model";
            this.outputDir = outputDir;
            this.overwrite = overwrite;
            this.emitRecords = emitRecords;
            this.log = log;
            Object components = spec.get("components");
            Object schemasObj = components instanceof Map ? ((Map<?, ?>) components).get("schemas") : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> s = schemasObj instanceof Map ? (Map<String, Object>) schemasObj
                    : Collections.<String, Object>emptyMap();
            this.schemas = s;
        }

        void run() throws IOException {
            // Build per-schema info up front so the unification map is ready
            // before any operation references a schema by name.
            for (Map.Entry<String, Object> e : schemas.entrySet()) {
                if (!(e.getValue() instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) e.getValue();
                SchemaInfo info = buildSchemaInfo(e.getKey(), schema);
                if (info == null) continue;
                schemaByName.put(e.getKey(), info);
            }
            unifyShapes();

            // Build operations now that schema names are stable.
            Object pathsObj = spec.get("paths");
            if (pathsObj instanceof Map) {
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
            } else {
                log.warn("OpenAPI spec has no `paths` -- no @RestClient interfaces will be generated.");
            }

            // Emit models.
            File modelDir = new File(outputDir, modelPackage.replace('.', '/'));
            ensureDir(modelDir);
            // Iterate canonical schemas only (unification dropped duplicates).
            Set<String> emittedCanonical = new HashSet<String>();
            for (SchemaInfo info : schemaByName.values()) {
                if (!info.isCanonical) continue;
                if (!emittedCanonical.add(info.javaName)) continue;
                emitModel(modelDir, info);
            }

            // Emit @RestClient interfaces -- one per tag.
            File apiDir = new File(outputDir, basePackage.replace('.', '/'));
            ensureDir(apiDir);
            for (Map.Entry<String, List<OperationInfo>> e : opsByTag.entrySet()) {
                emitApi(apiDir, e.getKey(), e.getValue());
            }

            log.info("Generated " + emittedCanonical.size() + " model(s) and "
                    + opsByTag.size() + " @RestClient interface(s) under " + outputDir);
        }

        // ----------------------------------------------------------------
        // Schema -> SchemaInfo
        // ----------------------------------------------------------------

        private SchemaInfo buildSchemaInfo(String name, Map<String, Object> schema) {
            SchemaInfo s = new SchemaInfo();
            s.specName = name;
            s.javaName = sanitizeClassName(name);
            s.isCanonical = true;
            Object propsObj = schema.get("properties");
            if (propsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) propsObj;
                for (Map.Entry<String, Object> e : props.entrySet()) {
                    PropInfo p = new PropInfo();
                    p.specName = e.getKey();
                    p.javaName = sanitizeIdentifier(e.getKey());
                    p.javaType = schemaToJavaType(e.getValue());
                    s.props.add(p);
                }
            }
            return s;
        }

        /// Compute a shape hash per schema -- two schemas with the same
        /// `(propName, javaType)` list in the same order collapse to a single
        /// emitted record/class. We keep the first-encountered name and alias
        /// the duplicates to it.
        private void unifyShapes() {
            for (Map.Entry<String, SchemaInfo> e : schemaByName.entrySet()) {
                SchemaInfo info = e.getValue();
                String shape = shapeOf(info);
                SchemaInfo prior = shapeIndex.get(shape);
                if (prior == null) {
                    shapeIndex.put(shape, info);
                    nameAliases.put(info.specName, info.javaName);
                } else {
                    info.isCanonical = false;
                    info.javaName = prior.javaName;
                    nameAliases.put(info.specName, prior.javaName);
                }
            }
        }

        private static String shapeOf(SchemaInfo s) {
            StringBuilder sb = new StringBuilder();
            for (PropInfo p : s.props) {
                sb.append(p.specName).append(':').append(p.javaType).append(';');
            }
            return sb.toString();
        }

        // ----------------------------------------------------------------
        // Operation parsing
        // ----------------------------------------------------------------

        private OperationInfo buildOperation(String verb, String path,
                                             Map<String, Object> op, Map<String, Object> pathItem) {
            String operationId = (String) op.get("operationId");
            if (operationId == null) operationId = synthesizeOperationId(verb, path);
            OperationInfo info = new OperationInfo();
            info.verb = verb;
            info.path = path;
            info.methodName = sanitizeIdentifier(operationId);
            info.summary = (String) op.get("summary");

            // Parameters: combine path-level + operation-level.
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
                if (pname == null) continue;
                ParamInfo pi = new ParamInfo();
                pi.specName = pname;
                pi.javaName = sanitizeIdentifier(pname);
                pi.javaType = paramTypeJava(pr);
                if ("path".equals(in)) {
                    info.pathParams.add(pi);
                } else if ("query".equals(in)) {
                    info.queryParams.add(pi);
                } else if ("header".equals(in)) {
                    if (!"Authorization".equalsIgnoreCase(pname)) {
                        info.headerParams.add(pi);
                    }
                    // The Authorization header is exposed uniformly via the
                    // trailing `bearerToken` argument -- skip the duplicate.
                } else if ("cookie".equals(in)) {
                    info.cookieParams.add(pi);
                }
            }

            // Request body (application/json only).
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
                        info.hasBody = true;
                        info.bodyJavaType = boxIfPrimitive(schemaToJavaType(schema));
                    }
                }
            }

            // Response: pick the first 2xx response with application/json.
            Object responses = op.get("responses");
            info.responseJavaType = "String";   // default -- treat as raw String body.
            info.responseIsString = true;
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
                                        String elem = schemaToJavaType(sm.get("items"));
                                        info.responseJavaType = "java.util.List<" + boxIfPrimitive(elem) + ">";
                                    } else {
                                        info.responseJavaType = schemaToJavaType(schema);
                                    }
                                    info.responseIsString = "String".equals(info.responseJavaType);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return info;
        }

        private Object resolveRef(Map<String, Object> maybeRef) {
            Object ref = maybeRef.get("$ref");
            if (!(ref instanceof String)) return maybeRef;
            String r = (String) ref;
            if (!r.startsWith("#/")) return maybeRef;
            String[] parts = r.substring(2).split("/");
            Object cur = spec;
            for (String p : parts) {
                if (cur instanceof Map) cur = ((Map<?, ?>) cur).get(p);
                else return maybeRef;
            }
            return cur instanceof Map ? cur : maybeRef;
        }

        private String paramTypeJava(Map<String, Object> param) {
            Object schema = param.get("schema");
            if (schema instanceof Map) return schemaToJavaType(schema);
            return "String";
        }

        /// Maps an OpenAPI schema node to a Java type string. Object schemas
        /// without a `$ref` collapse to `java.util.Map<String, Object>`.
        @SuppressWarnings("unchecked")
        String schemaToJavaType(Object schemaObj) {
            if (!(schemaObj instanceof Map)) return "Object";
            Map<String, Object> schema = (Map<String, Object>) schemaObj;
            Object ref = schema.get("$ref");
            if (ref instanceof String) {
                String r = (String) ref;
                int slash = r.lastIndexOf('/');
                if (slash >= 0 && r.startsWith("#/components/schemas/")) {
                    String specName = r.substring(slash + 1);
                    String alias = nameAliases.get(specName);
                    String name = alias != null ? alias : sanitizeClassName(specName);
                    return modelPackage + "." + name;
                }
                return "Object";
            }
            Object type = schema.get("type");
            if (type instanceof String) {
                String t = (String) type;
                if ("integer".equals(t)) {
                    Object fmt = schema.get("format");
                    if ("int64".equals(fmt)) return "Long";
                    return "Integer";
                }
                if ("number".equals(t)) {
                    Object fmt = schema.get("format");
                    if ("float".equals(fmt)) return "Float";
                    return "Double";
                }
                if ("boolean".equals(t)) return "Boolean";
                if ("string".equals(t)) return "String";
                if ("array".equals(t)) {
                    String element = schemaToJavaType(schema.get("items"));
                    return "java.util.List<" + boxIfPrimitive(element) + ">";
                }
            }
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

        // ----------------------------------------------------------------
        // Source emit
        // ----------------------------------------------------------------

        private void emitModel(File dir, SchemaInfo info) throws IOException {
            File f = new File(dir, info.javaName + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            StringBuilder sb = new StringBuilder(1024);
            sb.append("// Generated by cn1:generate-openapi.\n");
            sb.append("package ").append(modelPackage).append(";\n\n");
            sb.append("import com.codename1.annotations.JsonProperty;\n");
            sb.append("import com.codename1.annotations.Mapped;\n\n");
            if (emitRecords) {
                sb.append("@Mapped\n");
                sb.append("public record ").append(info.javaName).append("(");
                for (int i = 0; i < info.props.size(); i++) {
                    PropInfo p = info.props.get(i);
                    if (i > 0) sb.append(", ");
                    sb.append("@JsonProperty(\"").append(escapeJava(p.specName)).append("\") ")
                            .append(boxIfPrimitive(p.javaType)).append(' ').append(p.javaName);
                }
                sb.append(") {}\n");
            } else {
                sb.append("@Mapped\n");
                sb.append("public class ").append(info.javaName).append(" {\n");
                for (PropInfo p : info.props) {
                    sb.append("    @JsonProperty(\"").append(escapeJava(p.specName)).append("\")\n");
                    sb.append("    public ").append(p.javaType).append(' ')
                            .append(p.javaName).append(";\n");
                }
                sb.append("    public ").append(info.javaName).append("() {}\n");
                sb.append("}\n");
            }
            writeFile(f, sb.toString());
        }

        private void emitApi(File dir, String tag, List<OperationInfo> ops) throws IOException {
            String className = tag.endsWith("Api") ? tag : tag + "Api";
            File f = new File(dir, className + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            StringBuilder sb = new StringBuilder(2048);
            sb.append("// Generated by cn1:generate-openapi.\n");
            sb.append("package ").append(basePackage).append(";\n\n");
            sb.append("import com.codename1.annotations.rest.Body;\n");
            sb.append("import com.codename1.annotations.rest.Cookie;\n");
            sb.append("import com.codename1.annotations.rest.DELETE;\n");
            sb.append("import com.codename1.annotations.rest.GET;\n");
            sb.append("import com.codename1.annotations.rest.Header;\n");
            sb.append("import com.codename1.annotations.rest.PATCH;\n");
            sb.append("import com.codename1.annotations.rest.POST;\n");
            sb.append("import com.codename1.annotations.rest.PUT;\n");
            sb.append("import com.codename1.annotations.rest.Path;\n");
            sb.append("import com.codename1.annotations.rest.Query;\n");
            sb.append("import com.codename1.annotations.rest.RestClient;\n");
            sb.append("import com.codename1.io.rest.Response;\n");
            sb.append("import com.codename1.io.rest.RestClients;\n");
            sb.append("import com.codename1.util.OnComplete;\n\n");
            sb.append("@RestClient\n");
            sb.append("public interface ").append(className).append(" {\n\n");
            Set<String> usedNames = new LinkedHashSet<String>();
            for (OperationInfo op : ops) {
                emitOperationMethod(sb, op, usedNames);
            }
            sb.append("    static ").append(className).append(" of(String baseUrl) {\n");
            sb.append("        return RestClients.create(").append(className).append(".class, baseUrl);\n");
            sb.append("    }\n");
            sb.append("}\n");
            writeFile(f, sb.toString());
        }

        private void emitOperationMethod(StringBuilder sb, OperationInfo op, Set<String> usedNames) {
            if (op.summary != null && op.summary.length() > 0) {
                sb.append("    /// ").append(escapeAdoc(op.summary)).append("\n");
            }
            // Verb annotation.
            sb.append("    @").append(verbAnnotation(op.verb)).append("(\"")
                    .append(escapeJava(op.path)).append("\")\n");
            String methodName = uniqueName(op.methodName, usedNames);
            sb.append("    void ").append(methodName).append("(");
            boolean first = true;
            for (ParamInfo p : op.pathParams) {
                if (!first) sb.append(", ");
                sb.append("@Path(\"").append(escapeJava(p.specName)).append("\") ")
                        .append(boxIfPrimitive(p.javaType)).append(' ').append(p.javaName);
                first = false;
            }
            for (ParamInfo p : op.queryParams) {
                if (!first) sb.append(", ");
                sb.append("@Query(\"").append(escapeJava(p.specName)).append("\") ")
                        .append(boxIfPrimitive(p.javaType)).append(' ').append(p.javaName);
                first = false;
            }
            for (ParamInfo p : op.headerParams) {
                if (!first) sb.append(", ");
                sb.append("@Header(\"").append(escapeJava(p.specName)).append("\") ")
                        .append(boxIfPrimitive(p.javaType)).append(' ').append(p.javaName);
                first = false;
            }
            for (ParamInfo p : op.cookieParams) {
                if (!first) sb.append(", ");
                sb.append("@Cookie(\"").append(escapeJava(p.specName)).append("\") ")
                        .append(boxIfPrimitive(p.javaType)).append(' ').append(p.javaName);
                first = false;
            }
            if (op.hasBody) {
                if (!first) sb.append(", ");
                sb.append("@Body ").append(op.bodyJavaType).append(" body");
                first = false;
            }
            // Bearer token last before callback.
            if (!first) sb.append(", ");
            sb.append("@Header(\"Authorization\") String bearerToken");
            first = false;
            sb.append(", OnComplete<Response<").append(boxIfPrimitive(op.responseJavaType))
                    .append(">> callback);\n\n");
        }

        private static String uniqueName(String base, Set<String> used) {
            if (used.add(base)) return base;
            int n = 2;
            while (!used.add(base + n)) n++;
            return base + n;
        }

        private static String verbAnnotation(String verb) {
            return verb.toUpperCase(Locale.ROOT);
        }

        // ----------------------------------------------------------------
        // Helpers shared with the old codegen
        // ----------------------------------------------------------------

        private static String boxIfPrimitive(String type) {
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

        private static String escapeJava(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length() + 4);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"' || c == '\\') sb.append('\\');
                sb.append(c);
            }
            return sb.toString();
        }

        private static String escapeAdoc(String s) {
            return s.replace("\n", " ").replace("\r", " ");
        }

        private static void ensureDir(File f) throws IOException {
            if (!f.exists() && !f.mkdirs()) {
                throw new IOException("Could not create " + f);
            }
        }

        private static void writeFile(File f, String content) throws IOException {
            FileOutputStream out = new FileOutputStream(f);
            try {
                out.write(content.getBytes(StandardCharsets.UTF_8));
            } finally {
                out.close();
            }
        }
    }

    // ----------------------------------------------------------------
    // Static helpers
    // ----------------------------------------------------------------

    /// Strips characters that would make a name invalid as a Java identifier,
    /// upper-cases the first letter to match Java class-name convention.
    static String sanitizeClassName(String s) {
        if (s == null) return "Anonymous";
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || c == ' ' || c == '.') { upper = true; continue; }
            if (!Character.isJavaIdentifierPart(c)) continue;
            if (sb.length() == 0 && !Character.isJavaIdentifierStart(c)) sb.append('_');
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        if (sb.length() == 0) return "Anonymous";
        return sb.toString();
    }

    static String sanitizeIdentifier(String s) {
        if (s == null) return "anonymous";
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_' || c == '-' || c == ' ' || c == '.') { upperNext = true; continue; }
            if (!Character.isJavaIdentifierPart(c)) continue;
            if (sb.length() == 0 && !Character.isJavaIdentifierStart(c)) sb.append('_');
            sb.append(upperNext ? Character.toUpperCase(c) : c);
            upperNext = false;
        }
        if (sb.length() == 0) return "anonymous";
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
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

    // ----------------------------------------------------------------
    // Accumulators
    // ----------------------------------------------------------------

    static final class SchemaInfo {
        String specName;       // original OpenAPI name
        String javaName;       // sanitized Java identifier (post-unification)
        boolean isCanonical;   // false when this entry is aliased to another
        final List<PropInfo> props = new ArrayList<PropInfo>();
    }

    static final class PropInfo {
        String specName;
        String javaName;
        String javaType;
    }

    static final class OperationInfo {
        String verb;
        String path;
        String methodName;
        String summary;
        final List<ParamInfo> pathParams = new ArrayList<ParamInfo>();
        final List<ParamInfo> queryParams = new ArrayList<ParamInfo>();
        final List<ParamInfo> headerParams = new ArrayList<ParamInfo>();
        final List<ParamInfo> cookieParams = new ArrayList<ParamInfo>();
        boolean hasBody;
        String bodyJavaType;
        String responseJavaType;
        boolean responseIsString;
    }

    static final class ParamInfo {
        String specName;
        String javaName;
        String javaType;
    }
}
