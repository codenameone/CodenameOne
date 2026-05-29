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

/// Generates user-edited Codename One gRPC client sources from a
/// proto3 `.proto` specification.
///
/// Invocation:
///
/// ```
/// mvn cn1:generate-grpc -Dcn1.grpc.proto=helloworld.proto \
///                       -Dcn1.grpc.basePackage=com.example.hello
/// ```
///
/// Outputs (under `<basePackage>` in `src/main/java`):
///
/// - one `@ProtoMessage` record (Java 17+) or class (Java 8 target)
///   per `message` declared in the `.proto` file, with one
///   `@ProtoField(tag = N)` per field;
/// - one `@ProtoEnum` enum per `enum` declared in the file;
/// - one `@GrpcClient` interface per `service` declared in the file,
///   with one `@Rpc("Method")`-annotated method per `rpc` line. Each
///   method takes the request message, an `@Header("Authorization")
///   String bearerToken`, and an `OnComplete<GrpcResponse<Response>>`
///   callback. A `static <Self> of(String baseUrl)` factory wires the
///   interface to the runtime registry.
///
/// Streaming RPCs (`stream` keyword on request or response) are not
/// supported in this release and produce a build error pointing at
/// the offending line.
@Mojo(name = "generate-grpc",
      defaultPhase = LifecyclePhase.NONE,
      requiresProject = true,
      threadSafe = true)
public class GenerateGrpcMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /// Path or URL of the `.proto` file. Override via
    /// `-Dcn1.grpc.proto=...`.
    @Parameter(property = "cn1.grpc.proto")
    private String proto;

    /// Java base package the generated sources are emitted under.
    /// Override via `-Dcn1.grpc.basePackage=...`. The proto `package`
    /// declaration in the source `.proto` file does *not* override
    /// this -- it is preserved separately as the gRPC service path.
    @Parameter(property = "cn1.grpc.basePackage")
    private String basePackage;

    /// Output directory for the generated sources. Defaults to
    /// `${project.basedir}/src/main/java` because the emitted code is
    /// user-edited.
    @Parameter(property = "cn1.grpc.outputDirectory",
            defaultValue = "${project.basedir}/src/main/java")
    private File outputDirectory;

    /// When `true` (default) existing files at the destination are
    /// overwritten. Pass `-Dcn1.grpc.overwrite=false` to keep your
    /// hand-edits and only emit missing files.
    @Parameter(property = "cn1.grpc.overwrite", defaultValue = "true")
    private boolean overwrite;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String effectiveProto = effective(proto, "cn1.grpc.proto");
        String effectivePackage = effective(basePackage, "cn1.grpc.basePackage");
        if (effectiveProto == null || effectiveProto.length() == 0) {
            throw new MojoFailureException(
                    "No .proto file supplied. Pass -Dcn1.grpc.proto=<path> "
                            + "or configure <proto> in the plugin block.");
        }
        if (effectivePackage == null || effectivePackage.length() == 0) {
            throw new MojoFailureException(
                    "No base package supplied. Pass -Dcn1.grpc.basePackage=<pkg> "
                            + "or configure <basePackage> in the plugin block.");
        }

        File protoFile = new File(effectiveProto);
        if (!protoFile.exists()) {
            throw new MojoFailureException("Proto file not found: " + protoFile);
        }
        String source;
        try {
            source = new String(Files.readAllBytes(protoFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            throw new MojoFailureException("Could not read " + protoFile + ": " + ioe.getMessage(), ioe);
        }

        ProtoFile parsed;
        try {
            parsed = new ProtoParser(source, protoFile.getName()).parseFile();
        } catch (ProtoParseException ppe) {
            throw new MojoFailureException(ppe.getMessage(), ppe);
        }

        int target = GenerateOpenApiMojo.parseJavaVersion(detectJavaTargetString());
        boolean emitRecords = target >= 17;
        getLog().info("cn1:generate-grpc target=" + target + " emitRecords=" + emitRecords
                + " basePackage=" + effectivePackage
                + " protoPackage=" + (parsed.protoPackage == null ? "(none)" : parsed.protoPackage));

        Generator gen = new Generator(parsed, effectivePackage, outputDirectory, overwrite,
                emitRecords, getLog());
        try {
            gen.run();
        } catch (IOException ioe) {
            throw new MojoExecutionException("Failed to write generated sources: "
                    + ioe.getMessage(), ioe);
        }
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
    // AST
    // ----------------------------------------------------------------

    static final class ProtoFile {
        String protoPackage;          // e.g. "helloworld"
        final List<ProtoMessage> messages = new ArrayList<ProtoMessage>();
        final List<ProtoEnum> enums = new ArrayList<ProtoEnum>();
        final List<ProtoService> services = new ArrayList<ProtoService>();
    }

    static final class ProtoMessage {
        String name;                  // proto simple name
        final List<ProtoMessage> nestedMessages = new ArrayList<ProtoMessage>();
        final List<ProtoEnum> nestedEnums = new ArrayList<ProtoEnum>();
        final List<ProtoField> fields = new ArrayList<ProtoField>();
    }

    static final class ProtoField {
        boolean repeated;
        String typeName;              // proto type token (scalar or message/enum simple name)
        String name;                  // proto field name (snake_case typical)
        int tag;
    }

    static final class ProtoEnum {
        String name;
        final List<ProtoEnumValue> values = new ArrayList<ProtoEnumValue>();
    }

    static final class ProtoEnumValue {
        String name;
        int number;
    }

    static final class ProtoService {
        String name;
        final List<ProtoRpc> rpcs = new ArrayList<ProtoRpc>();
    }

    static final class ProtoRpc {
        String name;
        String requestType;
        String responseType;
    }

    static final class ProtoParseException extends RuntimeException {
        ProtoParseException(String msg) { super(msg); }
    }

    // ----------------------------------------------------------------
    // Tokenizer + parser
    // ----------------------------------------------------------------

    static final class ProtoParser {
        private final String src;
        private final String file;
        private int pos;
        private int line;

        ProtoParser(String src, String file) {
            this.src = src;
            this.file = file;
            this.pos = 0;
            this.line = 1;
        }

        ProtoFile parseFile() {
            ProtoFile pf = new ProtoFile();
            while (true) {
                skipWhitespaceAndComments();
                if (pos >= src.length()) break;
                if (peek("syntax")) {
                    consumeToSemicolon(); // syntax = "proto3";
                } else if (peek("package")) {
                    consume("package");
                    pf.protoPackage = readDottedIdent();
                    expect(';');
                } else if (peek("import")) {
                    consumeToSemicolon(); // imports are ignored
                } else if (peek("option")) {
                    consumeToSemicolon(); // file-level options ignored
                } else if (peek("message")) {
                    pf.messages.add(parseMessage());
                } else if (peek("enum")) {
                    pf.enums.add(parseEnum());
                } else if (peek("service")) {
                    pf.services.add(parseService());
                } else if (peek(";")) {
                    pos++;
                } else {
                    throw err("Unexpected token at top level: '" + previewToken() + "'");
                }
            }
            return pf;
        }

        private ProtoMessage parseMessage() {
            consume("message");
            ProtoMessage m = new ProtoMessage();
            m.name = readIdent();
            expect('{');
            while (true) {
                skipWhitespaceAndComments();
                if (peek("}")) { pos++; break; }
                if (peek("message")) {
                    m.nestedMessages.add(parseMessage());
                } else if (peek("enum")) {
                    m.nestedEnums.add(parseEnum());
                } else if (peek("option") || peek("reserved") || peek("extensions")) {
                    consumeToSemicolon();
                } else if (peek("oneof")) {
                    parseOneof(m);
                } else if (peek("map")) {
                    throw err("`map<K,V>` fields are not supported in this release "
                            + "(message " + m.name + ")");
                } else if (peek(";")) {
                    pos++;
                } else {
                    m.fields.add(parseField());
                }
            }
            return m;
        }

        /// Treats every member of a `oneof { ... }` as a regular
        /// optional field on the parent message. This loses the
        /// mutual-exclusion guarantee but lets the codec round-trip
        /// any single value the server emits. The user can enforce
        /// the oneof shape in application code.
        private void parseOneof(ProtoMessage m) {
            consume("oneof");
            readIdent(); // discard oneof name
            expect('{');
            while (true) {
                skipWhitespaceAndComments();
                if (peek("}")) { pos++; break; }
                if (peek("option")) { consumeToSemicolon(); continue; }
                if (peek(";")) { pos++; continue; }
                m.fields.add(parseField());
            }
        }

        private ProtoField parseField() {
            ProtoField f = new ProtoField();
            if (peek("repeated")) {
                consume("repeated");
                f.repeated = true;
            } else if (peek("optional")) {
                consume("optional"); // proto3 optional -- ignored, fields are nullable by default in our mapping
            } else if (peek("required")) {
                throw err("proto2 `required` fields are not supported (use proto3)");
            }
            f.typeName = readDottedIdent();
            f.name = readIdent();
            expect('=');
            f.tag = readInt();
            // Skip field options `[deprecated = true, ...]`.
            skipWhitespaceAndComments();
            if (pos < src.length() && src.charAt(pos) == '[') {
                int depth = 1;
                pos++;
                while (pos < src.length() && depth > 0) {
                    char c = src.charAt(pos);
                    if (c == '[') depth++;
                    else if (c == ']') depth--;
                    if (c == '\n') line++;
                    pos++;
                }
            }
            expect(';');
            return f;
        }

        private ProtoEnum parseEnum() {
            consume("enum");
            ProtoEnum e = new ProtoEnum();
            e.name = readIdent();
            expect('{');
            while (true) {
                skipWhitespaceAndComments();
                if (peek("}")) { pos++; break; }
                if (peek("option") || peek("reserved")) { consumeToSemicolon(); continue; }
                if (peek(";")) { pos++; continue; }
                ProtoEnumValue v = new ProtoEnumValue();
                v.name = readIdent();
                expect('=');
                v.number = readInt();
                // Skip value options [...].
                skipWhitespaceAndComments();
                if (pos < src.length() && src.charAt(pos) == '[') {
                    int depth = 1;
                    pos++;
                    while (pos < src.length() && depth > 0) {
                        char c = src.charAt(pos);
                        if (c == '[') depth++;
                        else if (c == ']') depth--;
                        if (c == '\n') line++;
                        pos++;
                    }
                }
                expect(';');
                e.values.add(v);
            }
            return e;
        }

        private ProtoService parseService() {
            consume("service");
            ProtoService s = new ProtoService();
            s.name = readIdent();
            expect('{');
            while (true) {
                skipWhitespaceAndComments();
                if (peek("}")) { pos++; break; }
                if (peek("option")) { consumeToSemicolon(); continue; }
                if (peek(";")) { pos++; continue; }
                if (peek("rpc")) {
                    s.rpcs.add(parseRpc(s.name));
                } else {
                    throw err("Unexpected token in service body: '" + previewToken() + "'");
                }
            }
            return s;
        }

        private ProtoRpc parseRpc(String serviceName) {
            consume("rpc");
            ProtoRpc r = new ProtoRpc();
            r.name = readIdent();
            expect('(');
            skipWhitespaceAndComments();
            if (peek("stream")) {
                throw err("Streaming RPCs are not supported in this release "
                        + "(service " + serviceName + ".rpc " + r.name + ")");
            }
            r.requestType = readDottedIdent();
            expect(')');
            skipWhitespaceAndComments();
            consume("returns");
            expect('(');
            skipWhitespaceAndComments();
            if (peek("stream")) {
                throw err("Streaming RPCs are not supported in this release "
                        + "(service " + serviceName + ".rpc " + r.name + ")");
            }
            r.responseType = readDottedIdent();
            expect(')');
            skipWhitespaceAndComments();
            // Optional `{ option ... }` body.
            if (pos < src.length() && src.charAt(pos) == '{') {
                int depth = 1;
                pos++;
                while (pos < src.length() && depth > 0) {
                    char c = src.charAt(pos);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    if (c == '\n') line++;
                    pos++;
                }
            } else if (pos < src.length() && src.charAt(pos) == ';') {
                pos++;
            }
            return r;
        }

        // -- Token primitives ----------------------------------------

        private boolean peek(String keyword) {
            skipWhitespaceAndComments();
            int n = keyword.length();
            if (pos + n > src.length()) return false;
            if (!src.regionMatches(pos, keyword, 0, n)) return false;
            // Single-char punctuation matches without identifier boundary.
            if (n == 1 && !Character.isJavaIdentifierPart(keyword.charAt(0))) return true;
            if (pos + n < src.length() && Character.isJavaIdentifierPart(src.charAt(pos + n))) return false;
            return true;
        }

        private void consume(String keyword) {
            if (!peek(keyword)) {
                throw err("Expected '" + keyword + "' but got '" + previewToken() + "'");
            }
            pos += keyword.length();
        }

        private void expect(char c) {
            skipWhitespaceAndComments();
            if (pos >= src.length() || src.charAt(pos) != c) {
                throw err("Expected '" + c + "' but got '" + previewToken() + "'");
            }
            pos++;
        }

        private void consumeToSemicolon() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '\n') line++;
                pos++;
                if (c == ';') return;
            }
        }

        private String readIdent() {
            skipWhitespaceAndComments();
            int start = pos;
            if (pos >= src.length() || !Character.isJavaIdentifierStart(src.charAt(pos))) {
                throw err("Expected identifier, got '" + previewToken() + "'");
            }
            while (pos < src.length() && Character.isJavaIdentifierPart(src.charAt(pos))) pos++;
            return src.substring(start, pos);
        }

        /// Reads a dotted identifier like `foo.bar.Baz`. Leading dot
        /// allowed (proto3 allows leading-dot fully-qualified refs);
        /// we strip it.
        private String readDottedIdent() {
            skipWhitespaceAndComments();
            if (pos < src.length() && src.charAt(pos) == '.') pos++;
            StringBuilder sb = new StringBuilder();
            sb.append(readIdent());
            while (true) {
                skipWhitespaceAndComments();
                if (pos < src.length() && src.charAt(pos) == '.') {
                    pos++;
                    sb.append('.').append(readIdent());
                } else {
                    break;
                }
            }
            return sb.toString();
        }

        private int readInt() {
            skipWhitespaceAndComments();
            int start = pos;
            if (pos < src.length() && src.charAt(pos) == '-') pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            if (pos == start) throw err("Expected integer literal");
            try {
                return Integer.parseInt(src.substring(start, pos));
            } catch (NumberFormatException nfe) {
                throw err("Bad integer literal '" + src.substring(start, pos) + "'");
            }
        }

        private String previewToken() {
            if (pos >= src.length()) return "<EOF>";
            int end = Math.min(pos + 16, src.length());
            return src.substring(pos, end);
        }

        private void skipWhitespaceAndComments() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '\n') { line++; pos++; continue; }
                if (Character.isWhitespace(c)) { pos++; continue; }
                if (c == '/' && pos + 1 < src.length()) {
                    char d = src.charAt(pos + 1);
                    if (d == '/') {
                        while (pos < src.length() && src.charAt(pos) != '\n') pos++;
                        continue;
                    }
                    if (d == '*') {
                        pos += 2;
                        while (pos + 1 < src.length()
                                && !(src.charAt(pos) == '*' && src.charAt(pos + 1) == '/')) {
                            if (src.charAt(pos) == '\n') line++;
                            pos++;
                        }
                        if (pos + 1 < src.length()) pos += 2;
                        continue;
                    }
                }
                return;
            }
        }

        private ProtoParseException err(String msg) {
            return new ProtoParseException(file + ":" + line + ": " + msg);
        }
    }

    // ----------------------------------------------------------------
    // Generator -- proto AST to Java source.
    // ----------------------------------------------------------------

    static final class Generator {
        private final ProtoFile file;
        private final String basePackage;
        private final File outputDir;
        private final boolean overwrite;
        private final boolean emitRecords;
        private final org.apache.maven.plugin.logging.Log log;

        /// Simple-name -> kind ("message" | "enum"). Lets a field type
        /// like `Status` (an enum) emit a different ProtoField shape
        /// from `Pet` (a message).
        private final Map<String, String> typeKinds = new LinkedHashMap<String, String>();
        /// Simple-name -> generated Java FQN.
        private final Map<String, String> typeJavaFqn = new LinkedHashMap<String, String>();

        Generator(ProtoFile file, String basePackage, File outputDir, boolean overwrite,
                  boolean emitRecords, org.apache.maven.plugin.logging.Log log) {
            this.file = file;
            this.basePackage = basePackage;
            this.outputDir = outputDir;
            this.overwrite = overwrite;
            this.emitRecords = emitRecords;
            this.log = log;
        }

        void run() throws IOException {
            // Pass 1: collect known type names (messages + enums, including nested) so
            // field-type resolution can determine whether `Status` is a scalar (no), an
            // enum (so emit `int number` accessor) or a message (so emit nested codec).
            for (ProtoMessage m : file.messages) collectTypes(m);
            for (ProtoEnum e : file.enums) {
                typeKinds.put(e.name, "enum");
                typeJavaFqn.put(e.name, basePackage + "." + e.name);
            }

            File pkgDir = new File(outputDir, basePackage.replace('.', '/'));
            ensureDir(pkgDir);

            for (ProtoMessage m : file.messages) {
                emitMessage(pkgDir, m);
            }
            for (ProtoEnum e : file.enums) {
                emitEnum(pkgDir, e, basePackage);
            }
            for (ProtoService s : file.services) {
                emitService(pkgDir, s);
            }

            log.info("Generated " + file.messages.size() + " @ProtoMessage class(es), "
                    + file.enums.size() + " @ProtoEnum(s), and "
                    + file.services.size() + " @GrpcClient interface(s) under " + outputDir);
        }

        private void collectTypes(ProtoMessage m) {
            typeKinds.put(m.name, "message");
            typeJavaFqn.put(m.name, basePackage + "." + m.name);
            for (ProtoMessage n : m.nestedMessages) collectTypes(n);
            for (ProtoEnum e : m.nestedEnums) {
                typeKinds.put(e.name, "enum");
                typeJavaFqn.put(e.name, basePackage + "." + e.name);
            }
        }

        // -- Message emission -----------------------------------------

        private void emitMessage(File dir, ProtoMessage m) throws IOException {
            // Emit nested types as top-level siblings to keep the
            // generated Java layout flat -- nested-class emission
            // would force inner-class codec generation in the
            // annotation processor and isn't worth the complexity for
            // the first cut.
            for (ProtoMessage n : m.nestedMessages) emitMessage(dir, n);
            for (ProtoEnum e : m.nestedEnums) emitEnum(dir, e, basePackage);

            File f = new File(dir, m.name + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            StringBuilder sb = new StringBuilder(2048);
            sb.append("// Generated by cn1:generate-grpc.\n");
            sb.append("package ").append(basePackage).append(";\n\n");
            sb.append("import com.codename1.annotations.grpc.ProtoField;\n");
            sb.append("import com.codename1.annotations.grpc.ProtoMessage;\n");
            if (hasRepeated(m)) sb.append("import java.util.List;\n");
            sb.append("\n");
            sb.append("@ProtoMessage\n");
            if (emitRecords) {
                sb.append("public record ").append(m.name).append("(\n");
                for (int i = 0; i < m.fields.size(); i++) {
                    ProtoField pf = m.fields.get(i);
                    if (i > 0) sb.append(",\n");
                    sb.append("    ").append(protoFieldAnnotation(pf)).append(' ')
                            .append(javaTypeFor(pf)).append(' ').append(javaName(pf.name));
                }
                sb.append("\n) {}\n");
            } else {
                sb.append("public class ").append(m.name).append(" {\n");
                for (ProtoField pf : m.fields) {
                    sb.append("    ").append(protoFieldAnnotation(pf)).append('\n');
                    sb.append("    public ").append(javaTypeFor(pf)).append(' ')
                            .append(javaName(pf.name)).append(";\n");
                }
                sb.append("    public ").append(m.name).append("() {}\n");
                sb.append("}\n");
            }
            writeFile(f, sb.toString());
        }

        private boolean hasRepeated(ProtoMessage m) {
            for (ProtoField pf : m.fields) {
                if (pf.repeated) return true;
            }
            return false;
        }

        private String protoFieldAnnotation(ProtoField pf) {
            String wire = wireKindFor(pf.typeName);
            StringBuilder sb = new StringBuilder();
            sb.append("@ProtoField(tag = ").append(pf.tag);
            if (!"DEFAULT".equals(wire)) {
                sb.append(", wireType = ProtoField.WireKind.").append(wire);
            }
            if (!javaName(pf.name).equals(pf.name)) {
                sb.append(", name = \"").append(escapeJava(pf.name)).append("\"");
            }
            sb.append(')');
            return sb.toString();
        }

        private static String wireKindFor(String protoType) {
            if ("sint32".equals(protoType) || "sint64".equals(protoType)) return "SINT";
            if ("fixed32".equals(protoType) || "fixed64".equals(protoType)
                    || "sfixed32".equals(protoType) || "sfixed64".equals(protoType)) return "FIXED";
            return "DEFAULT";
        }

        private String javaTypeFor(ProtoField pf) {
            String element = javaTypeForScalarOrRef(pf.typeName);
            if (pf.repeated) {
                return "List<" + boxIfPrimitive(element) + ">";
            }
            return element;
        }

        private String javaTypeForScalarOrRef(String protoType) {
            if ("int32".equals(protoType) || "sint32".equals(protoType)
                    || "uint32".equals(protoType) || "fixed32".equals(protoType)
                    || "sfixed32".equals(protoType)) return "int";
            if ("int64".equals(protoType) || "sint64".equals(protoType)
                    || "uint64".equals(protoType) || "fixed64".equals(protoType)
                    || "sfixed64".equals(protoType)) return "long";
            if ("float".equals(protoType)) return "float";
            if ("double".equals(protoType)) return "double";
            if ("bool".equals(protoType)) return "boolean";
            if ("string".equals(protoType)) return "String";
            if ("bytes".equals(protoType)) return "byte[]";
            // Message / enum reference. Strip leading proto-package
            // segments and resolve by simple name.
            String simple = protoType;
            int dot = simple.lastIndexOf('.');
            if (dot >= 0) simple = simple.substring(dot + 1);
            String fqn = typeJavaFqn.get(simple);
            return fqn != null ? fqn : ("java.lang.Object /* unknown proto type: " + protoType + " */");
        }

        // -- Enum emission --------------------------------------------

        private void emitEnum(File dir, ProtoEnum e, String pkg) throws IOException {
            File f = new File(dir, e.name + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            StringBuilder sb = new StringBuilder(1024);
            sb.append("// Generated by cn1:generate-grpc.\n");
            sb.append("package ").append(pkg).append(";\n\n");
            sb.append("import com.codename1.annotations.grpc.ProtoEnum;\n\n");
            sb.append("@ProtoEnum\n");
            sb.append("public enum ").append(e.name).append(" {\n");
            for (int i = 0; i < e.values.size(); i++) {
                ProtoEnumValue v = e.values.get(i);
                sb.append("    ").append(v.name).append("(").append(v.number).append(")");
                sb.append(i == e.values.size() - 1 ? ";\n" : ",\n");
            }
            sb.append("\n    public final int number;\n");
            sb.append("    ").append(e.name).append("(int n) { this.number = n; }\n\n");
            sb.append("    public static ").append(e.name).append(" forNumber(int n) {\n");
            sb.append("        for (").append(e.name).append(" v : values()) {\n");
            sb.append("            if (v.number == n) return v;\n");
            sb.append("        }\n");
            sb.append("        return null;\n");
            sb.append("    }\n");
            sb.append("}\n");
            writeFile(f, sb.toString());
        }

        // -- Service / @GrpcClient interface emission -----------------

        private void emitService(File dir, ProtoService s) throws IOException {
            String className = s.name.endsWith("Grpc") ? s.name : s.name + "Grpc";
            File f = new File(dir, className + ".java");
            if (f.exists() && !overwrite) {
                log.debug("skip existing " + f);
                return;
            }
            String serviceFqn = file.protoPackage == null || file.protoPackage.length() == 0
                    ? s.name
                    : file.protoPackage + "." + s.name;
            StringBuilder sb = new StringBuilder(2048);
            sb.append("// Generated by cn1:generate-grpc.\n");
            sb.append("package ").append(basePackage).append(";\n\n");
            sb.append("import com.codename1.annotations.grpc.GrpcClient;\n");
            sb.append("import com.codename1.annotations.grpc.Rpc;\n");
            sb.append("import com.codename1.annotations.rest.Header;\n");
            sb.append("import com.codename1.io.grpc.GrpcClients;\n");
            sb.append("import com.codename1.io.grpc.GrpcResponse;\n");
            sb.append("import com.codename1.util.OnComplete;\n\n");
            sb.append("@GrpcClient(\"").append(escapeJava(serviceFqn)).append("\")\n");
            sb.append("public interface ").append(className).append(" {\n\n");
            Set<String> usedNames = new LinkedHashSet<String>();
            for (ProtoRpc rpc : s.rpcs) emitRpcMethod(sb, rpc, usedNames);
            sb.append("    static ").append(className).append(" of(String baseUrl) {\n");
            sb.append("        return GrpcClients.create(").append(className).append(".class, baseUrl);\n");
            sb.append("    }\n");
            sb.append("}\n");
            writeFile(f, sb.toString());
        }

        private void emitRpcMethod(StringBuilder sb, ProtoRpc rpc, Set<String> usedNames) {
            String methodName = uniqueName(lowerFirst(rpc.name), usedNames);
            String reqType = resolveMessageType(rpc.requestType);
            String respType = resolveMessageType(rpc.responseType);
            sb.append("    @Rpc(\"").append(escapeJava(rpc.name)).append("\")\n");
            sb.append("    void ").append(methodName).append('(')
                    .append(reqType).append(" request, ")
                    .append("@Header(\"Authorization\") String bearerToken, ")
                    .append("OnComplete<GrpcResponse<").append(respType).append(">> callback);\n\n");
        }

        private String resolveMessageType(String protoType) {
            String simple = protoType;
            int dot = simple.lastIndexOf('.');
            if (dot >= 0) simple = simple.substring(dot + 1);
            String fqn = typeJavaFqn.get(simple);
            if (fqn == null) {
                throw new ProtoParseException("Unknown message type referenced in rpc: "
                        + protoType + " (only messages declared in the same .proto are recognised)");
            }
            return fqn;
        }

        // -- Helpers --------------------------------------------------

        private static String uniqueName(String base, Set<String> used) {
            if (used.add(base)) return base;
            int n = 2;
            while (!used.add(base + n)) n++;
            return base + n;
        }

        private static String lowerFirst(String s) {
            if (s == null || s.length() == 0) return s;
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }

        private static String boxIfPrimitive(String type) {
            if ("int".equals(type)) return "Integer";
            if ("long".equals(type)) return "Long";
            if ("float".equals(type)) return "Float";
            if ("double".equals(type)) return "Double";
            if ("boolean".equals(type)) return "Boolean";
            if ("byte".equals(type)) return "Byte";
            if ("short".equals(type)) return "Short";
            if ("char".equals(type)) return "Character";
            return type;
        }

        /// Maps a proto field name (typical `snake_case`) to a Java
        /// identifier. Leaves the name alone when it is already a
        /// valid Java identifier; otherwise camel-cases it.
        static String javaName(String protoName) {
            if (protoName == null || protoName.length() == 0) return "field";
            boolean hasUnderscore = protoName.indexOf('_') >= 0;
            String candidate;
            if (hasUnderscore) {
                StringBuilder sb = new StringBuilder(protoName.length());
                boolean upper = false;
                for (int i = 0; i < protoName.length(); i++) {
                    char c = protoName.charAt(i);
                    if (c == '_') { upper = true; continue; }
                    sb.append(upper ? Character.toUpperCase(c) : c);
                    upper = false;
                }
                candidate = sb.toString();
            } else {
                candidate = protoName;
            }
            if (candidate.length() > 0) {
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
                if (c == '"' || c == '\\') sb.append('\\');
                sb.append(c);
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
}
