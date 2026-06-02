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

import com.codename1.maven.GraphQLOperationModel.Document;
import com.codename1.maven.GraphQLOperationModel.Field;
import com.codename1.maven.GraphQLOperationModel.FragmentDef;
import com.codename1.maven.GraphQLOperationModel.FragmentSpread;
import com.codename1.maven.GraphQLOperationModel.InlineFragment;
import com.codename1.maven.GraphQLOperationModel.OperationDef;
import com.codename1.maven.GraphQLOperationModel.Selection;
import com.codename1.maven.GraphQLOperationModel.VarDef;
import com.codename1.maven.GraphQLSchemaModel.ArgDef;
import com.codename1.maven.GraphQLSchemaModel.EnumDef;
import com.codename1.maven.GraphQLSchemaModel.FieldDef;
import com.codename1.maven.GraphQLSchemaModel.ObjectTypeDef;
import com.codename1.maven.GraphQLSchemaModel.Schema;
import com.codename1.maven.GraphQLSchemaModel.TypeRef;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Generates user-edited Codename One GraphQL client sources from a
/// GraphQL SDL schema (and, optionally, a set of operation documents).
///
/// Invocation:
///
/// ```
/// mvn cn1:generate-graphql -Dcn1.graphql.schema=schema.graphqls \
///                          -Dcn1.graphql.operations=ops.graphql \
///                          -Dcn1.graphql.basePackage=com.example.api
/// ```
///
/// Two modes:
///
/// - **Operations mode** (when `cn1.graphql.operations` is supplied):
///   for each named `query` / `mutation` / `subscription` in the
///   operation document(s), emits one client-interface method plus the
///   precise `@Mapped` response classes matching that operation's
///   selection set. This is the recommended, precise path.
/// - **Schema-only quick-start mode** (no operations file): for each
///   field of the root `Query` / `Mutation` / `Subscription` type emits
///   one client method whose selection set is auto-expanded to a bounded
///   depth (`cn1.graphql.maxDepth`, default 2), stopping at cycles. A
///   convenience that may over- or under-fetch; prefer operations mode
///   for production.
///
/// GraphQL enums map to a generated Java enum in response classes, input
/// classes, and variable parameters alike (the JSON mapper binds enums via
/// their `name()`); custom scalars fall back to `String`.
@Mojo(name = "generate-graphql",
      defaultPhase = LifecyclePhase.NONE,
      requiresProject = true,
      threadSafe = true)
public class GenerateGraphQLMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /// Path or URL of the GraphQL SDL schema file
    /// (`.graphqls` / `.graphql`). Override via
    /// `-Dcn1.graphql.schema=...`.
    @Parameter(property = "cn1.graphql.schema")
    private String schema;

    /// Optional path to a GraphQL operation-document file (or a
    /// directory of `.graphql` files). When supplied the generator runs
    /// in the precise operations mode; when omitted it falls back to the
    /// schema-only quick-start mode. Override via
    /// `-Dcn1.graphql.operations=...`.
    @Parameter(property = "cn1.graphql.operations")
    private String operations;

    /// Java base package the generated sources are emitted under.
    /// Override via `-Dcn1.graphql.basePackage=...`.
    @Parameter(property = "cn1.graphql.basePackage")
    private String basePackage;

    /// Simple name of the generated `@GraphQLClient` interface.
    @Parameter(property = "cn1.graphql.clientName", defaultValue = "GraphQLApi")
    private String clientName;

    /// Optional default endpoint URL recorded on the generated
    /// `@GraphQLClient` annotation (informational only; the effective
    /// endpoint is the argument to `of(String endpoint)`).
    @Parameter(property = "cn1.graphql.endpoint", defaultValue = "")
    private String endpoint;

    /// Maximum selection-set depth used by schema-only quick-start mode.
    @Parameter(property = "cn1.graphql.maxDepth", defaultValue = "2")
    private int maxDepth;

    /// Output directory for the generated sources. Defaults to
    /// `${project.basedir}/src/main/java` because the emitted code is
    /// user-edited.
    @Parameter(property = "cn1.graphql.outputDirectory",
            defaultValue = "${project.basedir}/src/main/java")
    private File outputDirectory;

    /// When `true` (default) existing files at the destination are
    /// overwritten. Pass `-Dcn1.graphql.overwrite=false` to keep your
    /// hand-edits and only emit missing files.
    @Parameter(property = "cn1.graphql.overwrite", defaultValue = "true")
    private boolean overwrite;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String effSchema = effective(schema, "cn1.graphql.schema");
        String effPackage = effective(basePackage, "cn1.graphql.basePackage");
        if (effSchema == null || effSchema.length() == 0) {
            throw new MojoFailureException("No schema supplied. Pass "
                    + "-Dcn1.graphql.schema=<path> or configure <schema>.");
        }
        if (effPackage == null || effPackage.length() == 0) {
            throw new MojoFailureException("No base package supplied. Pass "
                    + "-Dcn1.graphql.basePackage=<pkg> or configure <basePackage>.");
        }
        File schemaFile = new File(effSchema);
        if (!schemaFile.exists()) {
            throw new MojoFailureException("Schema file not found: " + schemaFile);
        }

        Schema parsedSchema;
        try {
            String src = new String(Files.readAllBytes(schemaFile.toPath()), StandardCharsets.UTF_8);
            parsedSchema = new GraphQLSchemaModel.Parser(src, schemaFile.getName()).parse();
        } catch (IOException ioe) {
            throw new MojoFailureException("Could not read " + schemaFile + ": " + ioe.getMessage(), ioe);
        } catch (GraphQLSchemaModel.ParseException ppe) {
            throw new MojoFailureException(ppe.getMessage(), ppe);
        }

        Document doc = null;
        String effOps = effective(operations, "cn1.graphql.operations");
        if (effOps != null && effOps.length() > 0) {
            try {
                doc = parseOperations(new File(effOps));
            } catch (IOException ioe) {
                throw new MojoFailureException("Could not read operations " + effOps
                        + ": " + ioe.getMessage(), ioe);
            } catch (GraphQLOperationModel.ParseException ppe) {
                throw new MojoFailureException(ppe.getMessage(), ppe);
            }
        }

        int target = GenerateOpenApiMojo.parseJavaVersion(detectJavaTargetString());
        boolean emitRecords = target >= 17;
        getLog().info("cn1:generate-graphql target=" + target + " emitRecords=" + emitRecords
                + " basePackage=" + effPackage
                + " mode=" + (doc != null ? "operations" : "schema-only"));

        Generator gen = new Generator(parsedSchema, doc, effPackage, outputDirectory,
                overwrite, emitRecords, endpoint, clientName, maxDepth, getLog());
        try {
            gen.run();
        } catch (IOException ioe) {
            throw new MojoExecutionException("Failed to write generated sources: "
                    + ioe.getMessage(), ioe);
        }
    }

    private Document parseOperations(File path) throws IOException {
        StringBuilder all = new StringBuilder();
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && (f.getName().endsWith(".graphql") || f.getName().endsWith(".gql"))) {
                        all.append(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                        all.append('\n');
                    }
                }
            }
        } else {
            if (!path.exists()) {
                throw new IOException("operations path not found: " + path);
            }
            all.append(new String(Files.readAllBytes(path.toPath()), StandardCharsets.UTF_8));
        }
        return new GraphQLOperationModel.Parser(all.toString(), path.getName()).parse();
    }

    private String effective(String configured, String prop) {
        if (configured != null && configured.length() > 0) return configured;
        return System.getProperty(prop);
    }

    private String detectJavaTargetString() {
        String release = null, targetProp = null;
        if (project != null && project.getProperties() != null) {
            release = project.getProperties().getProperty("maven.compiler.release");
            targetProp = project.getProperties().getProperty("maven.compiler.target");
        }
        if (release == null) release = System.getProperty("maven.compiler.release");
        if (targetProp == null) targetProp = System.getProperty("maven.compiler.target");
        return release != null ? release : targetProp;
    }

    // ----------------------------------------------------------------
    // Generator
    // ----------------------------------------------------------------

    static final class Generator {
        private final Schema schema;
        private final Document operations;
        private final String basePackage;
        private final File outputDir;
        private final boolean overwrite;
        private final boolean emitRecords;
        private final String endpoint;
        private final String clientName;
        private final int maxDepth;
        private final org.apache.maven.plugin.logging.Log log;

        private final Set<String> emitted = new LinkedHashSet<String>();
        private final List<String> methods = new ArrayList<String>();
        private final Set<String> methodNames = new LinkedHashSet<String>();
        private File pkgDir;

        Generator(Schema schema, Document operations, String basePackage, File outputDir,
                  boolean overwrite, boolean emitRecords, String endpoint, String clientName,
                  int maxDepth, org.apache.maven.plugin.logging.Log log) {
            this.schema = schema;
            this.operations = operations;
            this.basePackage = basePackage;
            this.outputDir = outputDir;
            this.overwrite = overwrite;
            this.emitRecords = emitRecords;
            this.endpoint = endpoint;
            this.clientName = clientName;
            this.maxDepth = maxDepth;
            this.log = log;
        }

        void run() throws IOException {
            pkgDir = new File(outputDir, basePackage.replace('.', '/'));
            ensureDir(pkgDir);
            if (operations != null && !operations.operations.isEmpty()) {
                generateFromOperations();
            } else {
                generateFromSchema();
            }
            emitClientInterface();
            log.info("Generated " + methods.size() + " GraphQL operation method(s) and "
                    + emitted.size() + " model class(es) under " + outputDir);
        }

        // -- operations mode -----------------------------------------

        private void generateFromOperations() throws IOException {
            for (OperationDef op : operations.operations) {
                String rootType = rootTypeFor(op.kind);
                String opName = op.name != null ? op.name : deriveName(op);
                String methodName = unique(lowerFirst(javaName(opName)));
                String dataClass = cap(opName) + "Data";
                emitResponseClass(dataClass, rootType, op.selections);

                List<Param> params = new ArrayList<Param>();
                for (VarDef v : op.vars) {
                    Param p = new Param();
                    p.varName = v.name;
                    p.name = javaName(v.name);
                    p.javaType = javaTypeForVar(v.type);
                    params.add(p);
                }

                String document = minify(op.rawText + appendedFragments(op.directSpreads));
                methods.add(buildMethod(op.kind, methodName, document, op.name, params, dataClass));
            }
        }

        private String deriveName(OperationDef op) {
            for (Selection s : op.selections) {
                if (s instanceof Field) {
                    return ((Field) s).name;
                }
            }
            return "operation";
        }

        /// Transitively gathers the fragment definitions referenced by
        /// `directSpreads` and returns their raw text appended (so the
        /// request document is self-contained).
        private String appendedFragments(Set<String> directSpreads) {
            Set<String> resolved = new LinkedHashSet<String>();
            collectFragments(directSpreads, resolved);
            StringBuilder sb = new StringBuilder();
            for (String name : resolved) {
                FragmentDef fd = operations.fragments.get(name);
                if (fd != null) {
                    sb.append('\n').append(fd.rawText);
                }
            }
            return sb.toString();
        }

        private void collectFragments(Set<String> spreads, Set<String> into) {
            for (String name : spreads) {
                if (into.add(name)) {
                    FragmentDef fd = operations.fragments.get(name);
                    if (fd != null) {
                        collectFragments(fd.directSpreads, into);
                    }
                }
            }
        }

        /// Emits the `@Mapped` response class for `selections` resolved
        /// against `typeName`, recursing into nested object selections.
        private void emitResponseClass(String className, String typeName, List<Selection> selections)
                throws IOException {
            if (!emitted.add(className)) {
                return;
            }
            Map<String, Resolved> fields = new LinkedHashMap<String, Resolved>();
            collectSelections(typeName, selections, fields);

            List<ModelField> modelFields = new ArrayList<ModelField>();
            for (Resolved r : fields.values()) {
                ModelField mf = new ModelField();
                mf.jsonName = r.responseKey;
                mf.javaName = javaName(r.responseKey);
                if ("__typename".equals(r.name)) {
                    mf.javaType = "String";
                    modelFields.add(mf);
                    continue;
                }
                FieldDef def = findField(r.contextType, r.name);
                if (def == null) {
                    log.warn("cn1:generate-graphql: field '" + r.name + "' not found on type '"
                            + r.contextType + "'; emitting it as String");
                    mf.javaType = "String";
                    modelFields.add(mf);
                    continue;
                }
                String baseName = def.type.baseName();
                if (!r.subselections.isEmpty()) {
                    String nested = className + "_" + cap(r.responseKey);
                    emitResponseClass(nested, baseName, r.subselections);
                    mf.javaType = wrapList(def.type, nested);
                } else {
                    mf.javaType = wrapList(def.type, leafMappedJava(baseName));
                }
                modelFields.add(mf);
            }
            emitMapped(className, modelFields);
        }

        private void collectSelections(String typeName, List<Selection> sels,
                                       Map<String, Resolved> out) {
            for (Selection s : sels) {
                if (s instanceof Field) {
                    Field f = (Field) s;
                    String key = f.responseKey();
                    Resolved r = out.get(key);
                    if (r == null) {
                        r = new Resolved();
                        r.responseKey = key;
                        r.name = f.name;
                        r.contextType = typeName;
                        out.put(key, r);
                    }
                    r.subselections.addAll(f.selections);
                } else if (s instanceof FragmentSpread) {
                    FragmentDef fd = operations.fragments.get(((FragmentSpread) s).fragmentName);
                    if (fd != null) {
                        collectSelections(fd.typeCondition, fd.selections, out);
                    }
                } else if (s instanceof InlineFragment) {
                    InlineFragment inf = (InlineFragment) s;
                    String t = inf.typeCondition != null ? inf.typeCondition : typeName;
                    collectSelections(t, inf.selections, out);
                }
            }
        }

        // -- schema-only mode ----------------------------------------

        private void generateFromSchema() throws IOException {
            emitRootMethods(schema.queryType, GraphQLOperationModel.OP_QUERY, "Query");
            emitRootMethods(schema.mutationType, GraphQLOperationModel.OP_MUTATION, "Mutation");
            emitRootMethods(schema.subscriptionType, GraphQLOperationModel.OP_SUBSCRIPTION, "Subscription");
        }

        private void emitRootMethods(String rootTypeName, String kind, String roleWord)
                throws IOException {
            ObjectTypeDef root = schema.object(rootTypeName);
            if (root == null) {
                return;
            }
            for (FieldDef f : root.fields) {
                String methodName = unique(lowerFirst(javaName(f.name)));
                String dataClass = cap(f.name) + roleWord + "Data";
                String baseName = f.type.baseName();

                String fieldSelection;
                String fieldJavaType;
                if (schema.isObject(baseName) && schema.object(baseName) != null) {
                    String fieldClass = dataClass + "_" + cap(f.name);
                    Set<String> visited = new LinkedHashSet<String>();
                    visited.add(baseName);
                    fieldSelection = autoSelect(baseName, fieldClass, maxDepth, visited);
                    fieldJavaType = wrapList(f.type, fieldClass);
                } else {
                    fieldSelection = "";
                    fieldJavaType = wrapList(f.type, leafMappedJava(baseName));
                }

                ModelField wrapperField = new ModelField();
                wrapperField.jsonName = f.name;
                wrapperField.javaName = javaName(f.name);
                wrapperField.javaType = fieldJavaType;
                List<ModelField> one = new ArrayList<ModelField>();
                one.add(wrapperField);
                if (emitted.add(dataClass)) {
                    emitMapped(dataClass, one);
                }

                List<Param> params = new ArrayList<Param>();
                StringBuilder varDefs = new StringBuilder();
                StringBuilder argList = new StringBuilder();
                for (ArgDef a : f.args) {
                    Param p = new Param();
                    p.varName = a.name;
                    p.name = javaName(a.name);
                    p.javaType = javaTypeForVar(a.type);
                    params.add(p);
                    if (varDefs.length() > 0) varDefs.append(", ");
                    varDefs.append('$').append(a.name).append(": ").append(typeRefToSdl(a.type));
                    if (argList.length() > 0) argList.append(", ");
                    argList.append(a.name).append(": $").append(a.name);
                }

                String opName = cap(methodName);
                StringBuilder docB = new StringBuilder();
                docB.append(kind).append(' ').append(opName);
                if (varDefs.length() > 0) docB.append('(').append(varDefs).append(')');
                docB.append(" { ").append(f.name);
                if (argList.length() > 0) docB.append('(').append(argList).append(')');
                if (fieldSelection.length() > 0) docB.append(' ').append(fieldSelection);
                docB.append(" }");

                methods.add(buildMethod(kind, methodName, minify(docB.toString()), opName, params, dataClass));
            }
        }

        /// Auto-expands an object type's selection set to `depth` levels,
        /// emitting the matching `@Mapped` class and returning the
        /// selection-set string `{ ... }`. Object fields are expanded
        /// only while depth remains and their type is not already on the
        /// current path (cycle guard); skipped object fields are logged.
        private String autoSelect(String typeName, String className, int depth, Set<String> visited)
                throws IOException {
            ObjectTypeDef o = schema.object(typeName);
            List<ModelField> modelFields = new ArrayList<ModelField>();
            List<String> tokens = new ArrayList<String>();
            if (o != null) {
                for (FieldDef f : o.fields) {
                    String baseName = f.type.baseName();
                    boolean leaf = !(schema.isObject(baseName) && schema.object(baseName) != null);
                    if (leaf) {
                        if (schema.unions.containsKey(baseName)) {
                            continue; // unions need explicit fragments; skip in quick-start
                        }
                        tokens.add(f.name);
                        ModelField mf = new ModelField();
                        mf.jsonName = f.name;
                        mf.javaName = javaName(f.name);
                        mf.javaType = wrapList(f.type, leafMappedJava(baseName));
                        modelFields.add(mf);
                    } else {
                        if (depth <= 1 || visited.contains(baseName)) {
                            log.debug("cn1:generate-graphql: omitting field '" + f.name
                                    + "' of type '" + baseName + "' (depth/cycle limit)");
                            continue;
                        }
                        String nested = className + "_" + cap(f.name);
                        Set<String> v2 = new LinkedHashSet<String>(visited);
                        v2.add(baseName);
                        String childSel = autoSelect(baseName, nested, depth - 1, v2);
                        tokens.add(f.name + " " + childSel);
                        ModelField mf = new ModelField();
                        mf.jsonName = f.name;
                        mf.javaName = javaName(f.name);
                        mf.javaType = wrapList(f.type, nested);
                        modelFields.add(mf);
                    }
                }
            }
            if (tokens.isEmpty()) {
                tokens.add("__typename");
                ModelField mf = new ModelField();
                mf.jsonName = "__typename";
                mf.javaName = "__typename".equals(javaName("__typename")) ? "typename" : javaName("__typename");
                mf.javaType = "String";
                modelFields.add(mf);
            }
            if (emitted.add(className)) {
                emitMapped(className, modelFields);
            }
            StringBuilder sel = new StringBuilder("{ ");
            for (int i = 0; i < tokens.size(); i++) {
                if (i > 0) sel.append(' ');
                sel.append(tokens.get(i));
            }
            sel.append(" }");
            return sel.toString();
        }

        // -- variable / input typing ---------------------------------

        private String javaTypeForVar(TypeRef t) throws IOException {
            if (t.list) {
                return "java.util.List<" + javaTypeForVar(t.element) + ">";
            }
            String base = t.name;
            String sc = scalarJava(base);
            if (sc != null) return sc;
            if (schema.isEnum(base)) {
                ensureEnumEmitted(base);
                return base;
            }
            if (schema.isInput(base)) {
                ensureInputEmitted(base);
                return base;
            }
            // Object type used as a variable is invalid GraphQL; custom
            // scalars and anything unknown fall back to String.
            return "String";
        }

        private void ensureEnumEmitted(String name) throws IOException {
            if (!emitted.add(name)) return;
            EnumDef e = schema.enums.get(name);
            if (e == null) return;
            StringBuilder sb = new StringBuilder(512);
            sb.append("// Generated by cn1:generate-graphql.\n");
            sb.append("package ").append(basePackage).append(";\n\n");
            sb.append("public enum ").append(name).append(" {\n");
            for (int i = 0; i < e.values.size(); i++) {
                sb.append("    ").append(e.values.get(i));
                sb.append(i == e.values.size() - 1 ? "\n" : ",\n");
            }
            sb.append("}\n");
            writeClass(name, sb.toString());
        }

        private void ensureInputEmitted(String name) throws IOException {
            if (!emitted.add(name)) return;
            ObjectTypeDef in = schema.inputs.get(name);
            if (in == null) return;
            List<ModelField> fields = new ArrayList<ModelField>();
            for (FieldDef f : in.fields) {
                ModelField mf = new ModelField();
                mf.jsonName = f.name;
                mf.javaName = javaName(f.name);
                mf.javaType = javaTypeForInputField(f.type);
                fields.add(mf);
            }
            emitMapped(name, fields);
        }

        private String javaTypeForInputField(TypeRef t) throws IOException {
            if (t.list) {
                return "java.util.List<" + javaTypeForInputField(t.element) + ">";
            }
            String base = t.name;
            String sc = scalarJava(base);
            if (sc != null) return sc;
            if (schema.isEnum(base)) {
                ensureEnumEmitted(base);
                return base;
            }
            if (schema.isInput(base)) {
                ensureInputEmitted(base);
                return base;
            }
            // Custom scalars map to String in @Mapped fields.
            return "String";
        }

        // -- method + interface emission -----------------------------

        private String buildMethod(String kind, String methodName, String document,
                                   String operationName, List<Param> params, String dataClass) {
            boolean subscription = GraphQLOperationModel.OP_SUBSCRIPTION.equals(kind);
            StringBuilder sb = new StringBuilder(512);
            String anno = subscription ? "Subscription"
                    : (GraphQLOperationModel.OP_MUTATION.equals(kind) ? "Mutation" : "Query");
            // When operationName is also present the document must be named
            // (`value = "..."`) -- a positional value is only legal when it is
            // the sole annotation element.
            boolean hasOpName = operationName != null && operationName.length() > 0;
            sb.append("    @").append(anno).append('(');
            if (hasOpName) {
                sb.append("value = \"").append(escapeJava(document))
                        .append("\", operationName = \"").append(escapeJava(operationName)).append('"');
            } else {
                sb.append('"').append(escapeJava(document)).append('"');
            }
            sb.append(")\n");
            String ret = subscription ? "GraphQLSubscription" : "void";
            sb.append("    ").append(ret).append(' ').append(methodName).append('(');
            for (Param p : params) {
                sb.append("@Var(\"").append(escapeJava(p.varName)).append("\") ")
                        .append(p.javaType).append(' ').append(p.name).append(", ");
            }
            sb.append("@Header(\"Authorization\") String bearerToken, ");
            if (subscription) {
                sb.append("GraphQLSubscription.Handler<").append(dataClass).append("> handler);\n\n");
            } else {
                sb.append("OnComplete<GraphQLResponse<").append(dataClass).append(">> callback);\n\n");
            }
            return sb.toString();
        }

        private void emitClientInterface() throws IOException {
            File f = new File(pkgDir, clientName + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            StringBuilder sb = new StringBuilder(2048);
            sb.append("// Generated by cn1:generate-graphql.\n");
            sb.append("package ").append(basePackage).append(";\n\n");
            sb.append("import com.codename1.annotations.graphql.GraphQLClient;\n");
            sb.append("import com.codename1.annotations.graphql.Query;\n");
            sb.append("import com.codename1.annotations.graphql.Mutation;\n");
            sb.append("import com.codename1.annotations.graphql.Subscription;\n");
            sb.append("import com.codename1.annotations.graphql.Var;\n");
            sb.append("import com.codename1.annotations.rest.Header;\n");
            sb.append("import com.codename1.io.graphql.GraphQLClients;\n");
            sb.append("import com.codename1.io.graphql.GraphQLResponse;\n");
            sb.append("import com.codename1.io.graphql.GraphQLSubscription;\n");
            sb.append("import com.codename1.util.OnComplete;\n\n");
            sb.append("@GraphQLClient(\"").append(escapeJava(endpoint == null ? "" : endpoint)).append("\")\n");
            sb.append("public interface ").append(clientName).append(" {\n\n");
            for (String m : methods) {
                sb.append(m);
            }
            sb.append("    static ").append(clientName).append(" of(String endpoint) {\n");
            sb.append("        return GraphQLClients.create(").append(clientName).append(".class, endpoint);\n");
            sb.append("    }\n");
            sb.append("}\n");
            writeFile(f, sb.toString());
        }

        // -- @Mapped model emission ----------------------------------

        private void emitMapped(String className, List<ModelField> fields) throws IOException {
            File f = new File(pkgDir, className + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            boolean usesList = false;
            for (ModelField mf : fields) {
                if (mf.javaType.startsWith("java.util.List")) { usesList = true; break; }
            }
            StringBuilder sb = new StringBuilder(1024);
            sb.append("// Generated by cn1:generate-graphql.\n");
            sb.append("package ").append(basePackage).append(";\n\n");
            sb.append("import com.codename1.annotations.JsonProperty;\n");
            sb.append("import com.codename1.annotations.Mapped;\n");
            if (usesList) sb.append("import java.util.List;\n");
            sb.append("\n@Mapped\n");
            if (emitRecords) {
                sb.append("public record ").append(className).append("(\n");
                for (int i = 0; i < fields.size(); i++) {
                    ModelField mf = fields.get(i);
                    if (i > 0) sb.append(",\n");
                    sb.append("    ");
                    if (!mf.javaName.equals(mf.jsonName)) {
                        sb.append("@JsonProperty(\"").append(escapeJava(mf.jsonName)).append("\") ");
                    }
                    sb.append(shortList(mf.javaType)).append(' ').append(mf.javaName);
                }
                sb.append("\n) {}\n");
            } else {
                sb.append("public class ").append(className).append(" {\n");
                for (ModelField mf : fields) {
                    if (!mf.javaName.equals(mf.jsonName)) {
                        sb.append("    @JsonProperty(\"").append(escapeJava(mf.jsonName)).append("\")\n");
                    }
                    sb.append("    public ").append(shortList(mf.javaType)).append(' ')
                            .append(mf.javaName).append(";\n");
                }
                sb.append("    public ").append(className).append("() {}\n");
                sb.append("}\n");
            }
            writeFile(f, sb.toString());
        }

        private void writeClass(String className, String content) throws IOException {
            File f = new File(pkgDir, className + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            writeFile(f, content);
        }

        // -- type helpers --------------------------------------------

        private String rootTypeFor(String kind) {
            if (GraphQLOperationModel.OP_MUTATION.equals(kind)) return schema.mutationType;
            if (GraphQLOperationModel.OP_SUBSCRIPTION.equals(kind)) return schema.subscriptionType;
            return schema.queryType;
        }

        private FieldDef findField(String typeName, String fieldName) {
            ObjectTypeDef o = schema.object(typeName);
            if (o == null) o = schema.inputs.get(typeName);
            if (o == null) return null;
            for (FieldDef f : o.fields) {
                if (f.name.equals(fieldName)) return f;
            }
            return null;
        }

        /// Boxed Java scalar for a GraphQL leaf scalar, or null when
        /// `name` is not a built-in scalar.
        private static String scalarJava(String name) {
            if ("Int".equals(name)) return "Integer";
            if ("Float".equals(name)) return "Double";
            if ("Boolean".equals(name)) return "Boolean";
            if ("String".equals(name)) return "String";
            if ("ID".equals(name)) return "String";
            return null;
        }

        /// Java type for a leaf field inside an `@Mapped` class. Built-in
        /// scalars map to their boxed Java type; GraphQL enums map to a
        /// generated Java enum (the JSON mapper binds enums via their
        /// `name()`); custom scalars fall back to `String`.
        private String leafMappedJava(String baseName) throws IOException {
            String sc = scalarJava(baseName);
            if (sc != null) return sc;
            if (schema.isEnum(baseName)) {
                ensureEnumEmitted(baseName);
                return baseName;
            }
            return "String";
        }

        private String wrapList(TypeRef t, String elem) {
            if (t.list) {
                return "java.util.List<" + wrapList(t.element, elem) + ">";
            }
            return elem;
        }

        private static String typeRefToSdl(TypeRef t) {
            if (t.list) {
                return "[" + typeRefToSdl(t.element) + "]" + (t.nonNull ? "!" : "");
            }
            return t.name + (t.nonNull ? "!" : "");
        }

        /// Rewrites `java.util.List<...>` to the short `List<...>` once
        /// the file imports `java.util.List`.
        private static String shortList(String javaType) {
            return javaType.replace("java.util.List<", "List<");
        }

        // -- GraphQL document minifier -------------------------------

        /// Collapses whitespace and strips `#` comments outside string
        /// literals, producing a compact single-line operation document.
        static String minify(String s) {
            StringBuilder out = new StringBuilder(s.length());
            boolean lastSpace = true; // suppress leading space
            int i = 0;
            int n = s.length();
            while (i < n) {
                char c = s.charAt(i);
                if (c == '"') {
                    if (i + 3 <= n && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"') {
                        int end = s.indexOf("\"\"\"", i + 3);
                        int stop = end < 0 ? n : end + 3;
                        out.append(s, i, stop);
                        i = stop;
                    } else {
                        int j = i + 1;
                        while (j < n && s.charAt(j) != '"') {
                            if (s.charAt(j) == '\\') j++;
                            j++;
                        }
                        if (j < n) j++;
                        out.append(s, i, j);
                        i = j;
                    }
                    lastSpace = false;
                    continue;
                }
                if (c == '#') {
                    while (i < n && s.charAt(i) != '\n') i++;
                    continue;
                }
                if (Character.isWhitespace(c)) {
                    if (!lastSpace) {
                        out.append(' ');
                        lastSpace = true;
                    }
                    i++;
                    continue;
                }
                out.append(c);
                lastSpace = false;
                i++;
            }
            return out.toString().trim();
        }

        // -- name helpers (mirrors GenerateGrpcMojo) -----------------

        private String unique(String base) {
            if (methodNames.add(base)) return base;
            int n = 2;
            while (!methodNames.add(base + n)) n++;
            return base + n;
        }

        private static String lowerFirst(String s) {
            if (s == null || s.length() == 0) return s;
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }

        private static String cap(String s) {
            String j = javaName(s);
            if (j.length() == 0) return j;
            return Character.toUpperCase(j.charAt(0)) + j.substring(1);
        }

        static String javaName(String name) {
            if (name == null || name.length() == 0) return "field";
            StringBuilder sb = new StringBuilder(name.length());
            boolean upper = false;
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == '_') {
                    if (sb.length() > 0) upper = true;
                    continue;
                }
                if (!(Character.isLetterOrDigit(c))) {
                    continue;
                }
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
            String candidate = sb.length() == 0 ? "field" : sb.toString();
            if (candidate.length() > 0 && Character.isDigit(candidate.charAt(0))) {
                candidate = "_" + candidate;
            } else if (candidate.length() > 0) {
                candidate = Character.toLowerCase(candidate.charAt(0)) + candidate.substring(1);
            }
            if (isReservedWord(candidate)) candidate = candidate + "_";
            return candidate;
        }

        private static String escapeJava(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length() + 4);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }

        private static boolean isReservedWord(String s) {
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
    // Small structs
    // ----------------------------------------------------------------

    static final class Param {
        String varName;
        String name;
        String javaType;
    }

    static final class ModelField {
        String jsonName;
        String javaName;
        String javaType;
    }

    static final class Resolved {
        String responseKey;
        String name;
        String contextType;
        final List<Selection> subselections = new ArrayList<Selection>();
    }
}
