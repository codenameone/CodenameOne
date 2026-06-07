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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.ArrayList;

/// Build-time `@ProtoMessage` / `@ProtoEnum` processor. Emits one
/// `<SimpleName>ProtoCodec` per `@ProtoMessage` class and aggregates
/// them in `cn1app.ProtoBootstrap`. Generated codecs implement
/// `com.codename1.io.grpc.ProtoCodec<T>` and register themselves at
/// startup via `com.codename1.io.grpc.ProtoCodecs.register(...)` so
/// gRPC clients can resolve nested-message and enum codecs at
/// runtime without reflection.
public final class ProtoMessageAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String PROTO_MESSAGE_DESC = "Lcom/codename1/annotations/grpc/ProtoMessage;";
    public static final String PROTO_ENUM_DESC = "Lcom/codename1/annotations/grpc/ProtoEnum;";
    public static final String PROTO_FIELD_DESC = "Lcom/codename1/annotations/grpc/ProtoField;";

    static final String BOOTSTRAP_BINARY = "cn1app.ProtoBootstrap";
    static final String BOOTSTRAP_SIMPLE = "ProtoBootstrap";
    static final String BOOTSTRAP_PACKAGE = "cn1app";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(PROTO_MESSAGE_DESC);
        s.add(PROTO_ENUM_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    private final TreeMap<String, ProtoClass> messages = new TreeMap<String, ProtoClass>();
    /// Internal-name set of enums declared in the project so message-field
    /// resolution can tell `Lcom/example/Status;` (enum) apart from
    /// `Lcom/example/Pet;` (message).
    private final Set<String> enums = new LinkedHashSet<String>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        messages.clear();
        enums.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) return;
        boolean isMessage = cls.getClassAnnotation(PROTO_MESSAGE_DESC) != null;
        boolean isEnum = cls.getClassAnnotation(PROTO_ENUM_DESC) != null;
        if (!isMessage && !isEnum) return;

        if (isEnum) {
            enums.add(cls.getInternalName());
            return;
        }

        if (cls.isInterface() || cls.isAbstract()) {
            ctx.error(cls, "@ProtoMessage requires a concrete class or record; "
                    + cls.getBinaryName() + " is abstract or an interface");
            return;
        }

        ProtoClass pc = new ProtoClass();
        pc.binaryName = cls.getBinaryName();
        pc.simpleName = simpleName(pc.binaryName);
        pc.packageName = packageOf(pc.binaryName);
        pc.codecSimpleName = pc.simpleName + "ProtoCodec";
        pc.codecBinaryName = pc.packageName.length() == 0
                ? pc.codecSimpleName
                : pc.packageName + "." + pc.codecSimpleName;
        pc.isRecord = cls.isRecord();

        Set<Integer> tagsSeen = new LinkedHashSet<Integer>();
        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic()) continue;
            if (f.getName().startsWith("this$")) continue;
            AnnotationValues pf = f.getAnnotation(PROTO_FIELD_DESC);
            if (pf == null) continue;
            int tag = pf.getIntOrDefault("tag", 0);
            if (tag <= 0) {
                ctx.error(cls, "@ProtoField on " + pc.binaryName + "." + f.getName()
                        + " must declare a positive tag");
                continue;
            }
            if (!tagsSeen.add(tag)) {
                ctx.error(cls, "@ProtoField on " + pc.binaryName + "." + f.getName()
                        + " reuses tag " + tag + " from another field");
                continue;
            }
            ProtoFieldInfo pfi = new ProtoFieldInfo();
            pfi.name = f.getName();
            pfi.descriptor = f.getDescriptor();
            pfi.signature = f.getSignature();
            pfi.tag = tag;
            pfi.wireKind = wireKindFromAnnotation(pf);
            classifyField(pfi, ctx, cls);
            pc.fields.add(pfi);
        }
        messages.put(pc.binaryName, pc);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) return;
        if (messages.isEmpty() && enums.isEmpty()) return;

        // Second resolution pass: any field whose static type is a project
        // class but is not annotated @ProtoMessage / @ProtoEnum needs to be
        // resolved against the index now that we've seen every class.
        for (ProtoClass pc : messages.values()) {
            for (ProtoFieldInfo f : pc.fields) {
                resolveReferenceKind(f, ctx);
            }
        }
        if (ctx.hasErrors()) return;

        if (messages.isEmpty()) return; // enums alone don't need a bootstrap

        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (ProtoClass pc : messages.values()) {
            sources.put(pc.codecBinaryName, generateCodecSource(pc));
        }
        sources.put(BOOTSTRAP_BINARY, generateBootstrapSource(messages.values()));

        try {
            List<java.io.File> cp = new ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated @ProtoMessage sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + messages.size()
                + " @ProtoMessage codec(s) + " + BOOTSTRAP_BINARY);
    }

    // ----------------------------------------------------------------
    // Classification
    // ----------------------------------------------------------------

    private static String wireKindFromAnnotation(AnnotationValues pf) {
        Object v = pf.get("wireType");
        if (v instanceof String[]) {
            String[] enumRef = (String[]) v;
            if (enumRef.length == 2) return enumRef[1];
        }
        return "DEFAULT";
    }

    /// Classifies the field based on its JVM descriptor and (for
    /// `List<T>`) the generic signature. Reference types are left
    /// pending until `resolveReferenceKind` runs in `finish`, by
    /// which point the index of `@ProtoEnum` classes is complete.
    private void classifyField(ProtoFieldInfo pfi, ProcessorContext ctx, AnnotatedClass cls) {
        String d = pfi.descriptor;
        if ("I".equals(d)) {
            pfi.kind = intKind(pfi.wireKind);
            return;
        }
        if ("J".equals(d)) {
            pfi.kind = longKind(pfi.wireKind);
            return;
        }
        if ("F".equals(d)) { pfi.kind = ProtoKind.FLOAT; return; }
        if ("D".equals(d)) { pfi.kind = ProtoKind.DOUBLE; return; }
        if ("Z".equals(d)) { pfi.kind = ProtoKind.BOOL; return; }
        if ("[B".equals(d)) { pfi.kind = ProtoKind.BYTES; return; }
        if ("Ljava/lang/String;".equals(d)) { pfi.kind = ProtoKind.STRING; return; }
        if ("Ljava/lang/Integer;".equals(d)) { pfi.kind = intKind(pfi.wireKind); pfi.boxed = true; return; }
        if ("Ljava/lang/Long;".equals(d)) { pfi.kind = longKind(pfi.wireKind); pfi.boxed = true; return; }
        if ("Ljava/lang/Float;".equals(d)) { pfi.kind = ProtoKind.FLOAT; pfi.boxed = true; return; }
        if ("Ljava/lang/Double;".equals(d)) { pfi.kind = ProtoKind.DOUBLE; pfi.boxed = true; return; }
        if ("Ljava/lang/Boolean;".equals(d)) { pfi.kind = ProtoKind.BOOL; pfi.boxed = true; return; }
        if ("Ljava/util/List;".equals(d)) {
            pfi.repeated = true;
            // Element type comes from the generic signature.
            String element = parseListElement(pfi.signature);
            if (element == null) {
                ctx.error(cls, "@ProtoField on " + cls.getBinaryName() + "." + pfi.name
                        + " is List but lacks a generic element type");
                return;
            }
            pfi.elementDescriptor = element;
            classifyListElement(pfi, element);
            return;
        }
        if (d.startsWith("L") && d.endsWith(";")) {
            // Reference type -- defer to resolveReferenceKind once the
            // @ProtoEnum index is complete.
            pfi.pendingReference = true;
            pfi.referenceInternalName = d.substring(1, d.length() - 1);
            return;
        }
        ctx.error(cls, "@ProtoField on " + cls.getBinaryName() + "." + pfi.name
                + " has unsupported type descriptor " + d);
    }

    private static ProtoKind intKind(String wire) {
        if ("SINT".equals(wire)) return ProtoKind.SINT32;
        if ("FIXED".equals(wire)) return ProtoKind.FIXED32;
        return ProtoKind.INT32;
    }

    private static ProtoKind longKind(String wire) {
        if ("SINT".equals(wire)) return ProtoKind.SINT64;
        if ("FIXED".equals(wire)) return ProtoKind.FIXED64;
        return ProtoKind.INT64;
    }

    /// Parses the element type out of a `Ljava/util/List<...>;` generic
    /// signature. Returns the inner descriptor (e.g. `Ljava/lang/String;`
    /// or `Lcom/example/Pet;`) or `null` if the signature is missing.
    static String parseListElement(String signature) {
        if (signature == null) return null;
        int lt = signature.indexOf('<');
        int gt = signature.lastIndexOf('>');
        if (lt < 0 || gt < 0 || gt <= lt + 1) return null;
        return signature.substring(lt + 1, gt);
    }

    private void classifyListElement(ProtoFieldInfo pfi, String element) {
        if (element.length() == 0) return;
        char c = element.charAt(0);
        switch (c) {
            case 'L':
                pfi.referenceInternalName = element.substring(1, element.length() - 1);
                if ("java/lang/String".equals(pfi.referenceInternalName)) {
                    pfi.kind = ProtoKind.STRING;
                } else if ("java/lang/Integer".equals(pfi.referenceInternalName)) {
                    pfi.kind = intKind(pfi.wireKind);
                    pfi.boxed = true;
                } else if ("java/lang/Long".equals(pfi.referenceInternalName)) {
                    pfi.kind = longKind(pfi.wireKind);
                    pfi.boxed = true;
                } else if ("java/lang/Float".equals(pfi.referenceInternalName)) {
                    pfi.kind = ProtoKind.FLOAT;
                    pfi.boxed = true;
                } else if ("java/lang/Double".equals(pfi.referenceInternalName)) {
                    pfi.kind = ProtoKind.DOUBLE;
                    pfi.boxed = true;
                } else if ("java/lang/Boolean".equals(pfi.referenceInternalName)) {
                    pfi.kind = ProtoKind.BOOL;
                    pfi.boxed = true;
                } else {
                    pfi.pendingReference = true;
                }
                return;
            default:
                // Lists of primitives aren't expressible in JVM
                // signatures, so anything else is invalid.
                pfi.kind = ProtoKind.UNSUPPORTED;
        }
    }

    /// Resolves a deferred-reference field by checking whether the
    /// referenced type is a `@ProtoEnum`. Anything else is treated as
    /// a `@ProtoMessage` -- the generated codec lookup at runtime
    /// will throw if no codec is registered, which gives a
    /// less-useful error message than this validation but lets a
    /// project ship cycles between project-local message types.
    private void resolveReferenceKind(ProtoFieldInfo pfi, ProcessorContext ctx) {
        if (!pfi.pendingReference) return;
        pfi.pendingReference = false;
        if (enums.contains(pfi.referenceInternalName)) {
            pfi.kind = ProtoKind.ENUM;
            return;
        }
        // Assume message. (We can't validate the referenced type is
        // really @ProtoMessage without walking the index again; in
        // practice typos surface at runtime via ProtoCodecs.lookup.)
        pfi.kind = ProtoKind.MESSAGE;
    }

    // ----------------------------------------------------------------
    // Source generation
    // ----------------------------------------------------------------

    private static String generateCodecSource(ProtoClass pc) {
        StringBuilder sb = new StringBuilder(2048);
        if (pc.packageName.length() > 0) {
            sb.append("package ").append(pc.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(pc.codecSimpleName)
                .append(" implements com.codename1.io.grpc.ProtoCodec<")
                .append(pc.binaryName).append("> {\n\n");

        sb.append("    public static final ").append(pc.codecSimpleName)
                .append(" INSTANCE = new ").append(pc.codecSimpleName).append("();\n\n");

        sb.append("    public static void register() {\n");
        sb.append("        com.codename1.io.grpc.ProtoCodecs.register(")
                .append(pc.binaryName).append(".class, INSTANCE);\n");
        sb.append("    }\n\n");

        sb.append("    public ").append(pc.codecSimpleName).append("() {\n    }\n\n");

        emitWrite(sb, pc);
        emitRead(sb, pc);

        sb.append("}\n");
        return sb.toString();
    }

    private static void emitWrite(StringBuilder sb, ProtoClass pc) {
        sb.append("    public void write(com.codename1.io.grpc.ProtoWriter out, ")
                .append(pc.binaryName).append(" value) throws java.io.IOException {\n");
        sb.append("        if (value == null) return;\n");
        for (ProtoFieldInfo f : pc.fields) {
            emitWriteField(sb, pc, f);
        }
        sb.append("    }\n\n");
    }

    private static String readExpr(ProtoClass pc, ProtoFieldInfo f) {
        return pc.isRecord ? "value." + f.name + "()" : "value." + f.name;
    }

    private static void emitWriteField(StringBuilder sb, ProtoClass pc, ProtoFieldInfo f) {
        String e = readExpr(pc, f);
        int tag = f.tag;
        if (f.repeated) {
            switch (f.kind) {
                case STRING:
                    sb.append("        out.writeStringList(").append(tag).append(", ").append(e).append(");\n");
                    return;
                case MESSAGE: {
                    String codec = codecFor(f.referenceInternalName);
                    sb.append("        out.writeMessageList(").append(tag).append(", ").append(e)
                            .append(", ").append(codec).append(");\n");
                    return;
                }
                case INT32: case SINT32: case FIXED32:
                case INT64: case SINT64: case FIXED64:
                case FLOAT: case DOUBLE: case BOOL: case ENUM:
                    emitRepeatedScalarWrite(sb, f, e, tag);
                    return;
                case BYTES:
                    sb.append("        if (").append(e).append(" != null) {\n");
                    sb.append("            for (int _i = 0, _n = ").append(e).append(".size(); _i < _n; _i++) {\n");
                    sb.append("                out.writeBytes(").append(tag).append(", ").append(e).append(".get(_i));\n");
                    sb.append("            }\n");
                    sb.append("        }\n");
                    return;
                default:
                    sb.append("        // TODO unsupported repeated kind ").append(f.kind).append("\n");
                    return;
            }
        }
        switch (f.kind) {
            case INT32: sb.append("        out.writeInt32(").append(tag).append(", ").append(e).append(");\n"); return;
            case SINT32: sb.append("        out.writeSInt32(").append(tag).append(", ").append(e).append(");\n"); return;
            case FIXED32: sb.append("        out.writeFixed32Field(").append(tag).append(", ").append(e).append(");\n"); return;
            case INT64: sb.append("        out.writeInt64(").append(tag).append(", ").append(e).append(");\n"); return;
            case SINT64: sb.append("        out.writeSInt64(").append(tag).append(", ").append(e).append(");\n"); return;
            case FIXED64: sb.append("        out.writeFixed64Field(").append(tag).append(", ").append(e).append(");\n"); return;
            case FLOAT: sb.append("        out.writeFloat(").append(tag).append(", ").append(e).append(");\n"); return;
            case DOUBLE: sb.append("        out.writeDouble(").append(tag).append(", ").append(e).append(");\n"); return;
            case BOOL: sb.append("        out.writeBool(").append(tag).append(", ").append(e).append(");\n"); return;
            case STRING: sb.append("        out.writeString(").append(tag).append(", ").append(e).append(");\n"); return;
            case BYTES: sb.append("        out.writeBytes(").append(tag).append(", ").append(e).append(");\n"); return;
            case ENUM:
                sb.append("        if (").append(e).append(" != null) {\n");
                sb.append("            out.writeInt32(").append(tag).append(", ").append(e).append(".number);\n");
                sb.append("        }\n");
                return;
            case MESSAGE: {
                String codec = codecFor(f.referenceInternalName);
                sb.append("        out.writeMessage(").append(tag).append(", ").append(e)
                        .append(", ").append(codec).append(");\n");
                return;
            }
            default:
                sb.append("        // TODO unsupported kind ").append(f.kind).append("\n");
        }
    }

    /// Packed repeated scalar emit: opens a `ByteArrayOutputStream`,
    /// writes every element using its varint / fixed-width encoder,
    /// then attaches the buffer as a length-delimited field. proto3
    /// servers accept either packed or unpacked, so the writer
    /// chooses packed for compactness; the reader handles both.
    private static void emitRepeatedScalarWrite(StringBuilder sb, ProtoFieldInfo f, String e, int tag) {
        sb.append("        if (").append(e).append(" != null && !").append(e).append(".isEmpty()) {\n");
        sb.append("            java.io.ByteArrayOutputStream _buf = new java.io.ByteArrayOutputStream();\n");
        sb.append("            com.codename1.io.grpc.ProtoWriter _sub = new com.codename1.io.grpc.ProtoWriter(_buf);\n");
        sb.append("            for (int _i = 0, _n = ").append(e).append(".size(); _i < _n; _i++) {\n");
        switch (f.kind) {
            case INT32: sb.append("                _sub.writeVarint32(").append(e).append(".get(_i));\n"); break;
            case SINT32: sb.append("                _sub.writeVarint64(com.codename1.io.grpc.ProtoWriter.zigZag32(").append(e).append(".get(_i)) & 0xFFFFFFFFL);\n"); break;
            case FIXED32: sb.append("                _sub.writeFixed32(").append(e).append(".get(_i));\n"); break;
            case INT64: sb.append("                _sub.writeVarint64(").append(e).append(".get(_i));\n"); break;
            case SINT64: sb.append("                _sub.writeVarint64(com.codename1.io.grpc.ProtoWriter.zigZag64(").append(e).append(".get(_i)));\n"); break;
            case FIXED64: sb.append("                _sub.writeFixed64(").append(e).append(".get(_i));\n"); break;
            case FLOAT: sb.append("                _sub.writeFixed32(Float.floatToIntBits(").append(e).append(".get(_i)));\n"); break;
            case DOUBLE: sb.append("                _sub.writeFixed64(Double.doubleToLongBits(").append(e).append(".get(_i)));\n"); break;
            case BOOL: sb.append("                _sub.writeVarint32(").append(e).append(".get(_i) ? 1 : 0);\n"); break;
            case ENUM: sb.append("                _sub.writeVarint32(").append(e).append(".get(_i).number);\n"); break;
            default: break;
        }
        sb.append("            }\n");
        sb.append("            byte[] _body = _buf.toByteArray();\n");
        sb.append("            out.writeTag(").append(tag).append(", com.codename1.io.grpc.ProtoWriter.WIRE_LEN);\n");
        sb.append("            out.writeVarint32(_body.length);\n");
        sb.append("            out.stream().write(_body);\n");
        sb.append("        }\n");
    }

    private static String codecFor(String referenceInternalName) {
        String binary = referenceInternalName.replace('/', '.');
        return binary + "ProtoCodec.INSTANCE";
    }

    // -- Read --------------------------------------------------------

    private static void emitRead(StringBuilder sb, ProtoClass pc) {
        sb.append("    public ").append(pc.binaryName)
                .append(" read(com.codename1.io.grpc.ProtoReader in) throws java.io.IOException {\n");
        if (pc.isRecord) {
            for (ProtoFieldInfo f : pc.fields) {
                sb.append("        ").append(localType(f)).append(" _")
                        .append(f.name).append(" = ").append(localInit(f)).append(";\n");
            }
        } else {
            sb.append("        ").append(pc.binaryName).append(" value = new ")
                    .append(pc.binaryName).append("();\n");
        }
        // For repeated fields on POJO we need to lazily new the list.
        sb.append("        int _tag;\n");
        sb.append("        while ((_tag = in.readTag()) != 0) {\n");
        sb.append("            int _field = _tag >>> 3;\n");
        sb.append("            int _wire = _tag & 7;\n");
        sb.append("            switch (_field) {\n");
        for (ProtoFieldInfo f : pc.fields) {
            emitReadCase(sb, pc, f);
        }
        sb.append("                default: in.skipField(_tag); break;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        if (pc.isRecord) {
            sb.append("        return new ").append(pc.binaryName).append("(");
            for (int i = 0; i < pc.fields.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("_").append(pc.fields.get(i).name);
            }
            sb.append(");\n");
        } else {
            sb.append("        return value;\n");
        }
        sb.append("    }\n");
    }

    private static String localType(ProtoFieldInfo f) {
        // For records we need a Java type literal for the local
        // that ultimately feeds the canonical constructor.
        if (f.repeated) {
            return "java.util.List<" + elementJavaType(f) + ">";
        }
        switch (f.kind) {
            case INT32: case SINT32: case FIXED32: return f.boxed ? "java.lang.Integer" : "int";
            case INT64: case SINT64: case FIXED64: return f.boxed ? "java.lang.Long" : "long";
            case FLOAT: return f.boxed ? "java.lang.Float" : "float";
            case DOUBLE: return f.boxed ? "java.lang.Double" : "double";
            case BOOL: return f.boxed ? "java.lang.Boolean" : "boolean";
            case STRING: return "java.lang.String";
            case BYTES: return "byte[]";
            case MESSAGE: case ENUM: return f.referenceInternalName.replace('/', '.');
            default: return "java.lang.Object";
        }
    }

    private static String elementJavaType(ProtoFieldInfo f) {
        switch (f.kind) {
            case INT32: case SINT32: case FIXED32: return "java.lang.Integer";
            case INT64: case SINT64: case FIXED64: return "java.lang.Long";
            case FLOAT: return "java.lang.Float";
            case DOUBLE: return "java.lang.Double";
            case BOOL: return "java.lang.Boolean";
            case STRING: return "java.lang.String";
            case BYTES: return "byte[]";
            case MESSAGE: case ENUM: return f.referenceInternalName.replace('/', '.');
            default: return "java.lang.Object";
        }
    }

    private static String localInit(ProtoFieldInfo f) {
        if (f.repeated) return "new java.util.ArrayList<" + elementJavaType(f) + ">()";
        if (f.boxed) return "null";
        switch (f.kind) {
            case INT32: case SINT32: case FIXED32: return "0";
            case INT64: case SINT64: case FIXED64: return "0L";
            case FLOAT: return "0.0f";
            case DOUBLE: return "0.0d";
            case BOOL: return "false";
            default: return "null";
        }
    }

    private static void emitReadCase(StringBuilder sb, ProtoClass pc, ProtoFieldInfo f) {
        sb.append("                case ").append(f.tag).append(": {\n");
        if (f.repeated) {
            emitReadRepeated(sb, pc, f);
        } else {
            emitReadScalar(sb, pc, f);
        }
        sb.append("                    break;\n");
        sb.append("                }\n");
    }

    private static String writeTarget(ProtoClass pc, ProtoFieldInfo f) {
        return pc.isRecord ? "_" + f.name : "value." + f.name;
    }

    private static void emitReadScalar(StringBuilder sb, ProtoClass pc, ProtoFieldInfo f) {
        String target = writeTarget(pc, f);
        switch (f.kind) {
            case INT32: sb.append("                    ").append(target).append(" = in.readVarint32();\n"); break;
            case SINT32: sb.append("                    ").append(target).append(" = in.readSInt32();\n"); break;
            case FIXED32: sb.append("                    ").append(target).append(" = in.readFixed32();\n"); break;
            case INT64: sb.append("                    ").append(target).append(" = in.readVarint64();\n"); break;
            case SINT64: sb.append("                    ").append(target).append(" = in.readSInt64();\n"); break;
            case FIXED64: sb.append("                    ").append(target).append(" = in.readFixed64();\n"); break;
            case FLOAT: sb.append("                    ").append(target).append(" = in.readFloat();\n"); break;
            case DOUBLE: sb.append("                    ").append(target).append(" = in.readDouble();\n"); break;
            case BOOL: sb.append("                    ").append(target).append(" = in.readBool();\n"); break;
            case STRING: sb.append("                    ").append(target).append(" = in.readString();\n"); break;
            case BYTES: sb.append("                    ").append(target).append(" = in.readBytes();\n"); break;
            case ENUM:
                sb.append("                    ").append(target).append(" = ")
                        .append(f.referenceInternalName.replace('/', '.'))
                        .append(".forNumber(in.readVarint32());\n");
                break;
            case MESSAGE:
                sb.append("                    ").append(target).append(" = in.readMessage(")
                        .append(codecFor(f.referenceInternalName)).append(");\n");
                break;
            default:
                sb.append("                    in.skipField(_tag);\n");
        }
    }

    private static void emitReadRepeated(StringBuilder sb, ProtoClass pc, ProtoFieldInfo f) {
        String target = pc.isRecord ? "_" + f.name : "value." + f.name;
        if (!pc.isRecord) {
            // POJO: lazily new the list if null (declared field can be
            // null when the message is decoded with no occurrences).
            sb.append("                    if (").append(target).append(" == null) ")
                    .append(target).append(" = new java.util.ArrayList<").append(elementJavaType(f)).append(">();\n");
        }
        switch (f.kind) {
            case STRING:
                sb.append("                    ").append(target).append(".add(in.readString());\n");
                return;
            case MESSAGE:
                sb.append("                    ").append(target).append(".add(in.readMessage(")
                        .append(codecFor(f.referenceInternalName)).append("));\n");
                return;
            case BYTES:
                sb.append("                    ").append(target).append(".add(in.readBytes());\n");
                return;
            case ENUM: {
                final String enumType = f.referenceInternalName.replace('/', '.');
                sb.append("                    if (_wire == com.codename1.io.grpc.ProtoWriter.WIRE_LEN) {\n");
                sb.append("                        in.readPacked(").append(target).append(", new com.codename1.io.grpc.ProtoReader.PackedReader<")
                        .append(enumType).append(">() {\n");
                sb.append("                            public ").append(enumType).append(" read(com.codename1.io.grpc.ProtoReader _r) throws java.io.IOException {\n");
                sb.append("                                return ").append(enumType).append(".forNumber(_r.readVarint32());\n");
                sb.append("                            }\n");
                sb.append("                        });\n");
                sb.append("                    } else {\n");
                sb.append("                        ").append(target).append(".add(").append(enumType).append(".forNumber(in.readVarint32()));\n");
                sb.append("                    }\n");
                return;
            }
            default:
                // Scalar repeated: support both packed and unpacked.
                sb.append("                    if (_wire == com.codename1.io.grpc.ProtoWriter.WIRE_LEN) {\n");
                sb.append("                        in.readPacked(").append(target).append(", new com.codename1.io.grpc.ProtoReader.PackedReader<")
                        .append(elementJavaType(f)).append(">() {\n");
                sb.append("                            public ").append(elementJavaType(f)).append(" read(com.codename1.io.grpc.ProtoReader _r) throws java.io.IOException {\n");
                sb.append("                                ").append(elementReadExpr(f)).append("\n");
                sb.append("                            }\n");
                sb.append("                        });\n");
                sb.append("                    } else {\n");
                sb.append("                        ").append(target).append(".add(")
                        .append(elementReadInline(f)).append(");\n");
                sb.append("                    }\n");
        }
    }

    private static String elementReadExpr(ProtoFieldInfo f) {
        switch (f.kind) {
            case INT32: return "return _r.readVarint32();";
            case SINT32: return "return _r.readSInt32();";
            case FIXED32: return "return _r.readFixed32();";
            case INT64: return "return _r.readVarint64();";
            case SINT64: return "return _r.readSInt64();";
            case FIXED64: return "return _r.readFixed64();";
            case FLOAT: return "return _r.readFloat();";
            case DOUBLE: return "return _r.readDouble();";
            case BOOL: return "return _r.readBool();";
            default: return "return null;";
        }
    }

    private static String elementReadInline(ProtoFieldInfo f) {
        switch (f.kind) {
            case INT32: return "in.readVarint32()";
            case SINT32: return "in.readSInt32()";
            case FIXED32: return "in.readFixed32()";
            case INT64: return "in.readVarint64()";
            case SINT64: return "in.readSInt64()";
            case FIXED64: return "in.readFixed64()";
            case FLOAT: return "in.readFloat()";
            case DOUBLE: return "in.readDouble()";
            case BOOL: return "in.readBool()";
            default: return "null";
        }
    }

    private static String generateBootstrapSource(Iterable<ProtoClass> classes) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        sb.append("/// gRPC protobuf codec bootstrap. The iOS / Android per-build\n");
        sb.append("/// application stub instantiates this class before Display.init\n");
        sb.append("/// (the build server probes the project zip for it and emits the\n");
        sb.append("/// install line conditionally); JavaSEPort picks it up via\n");
        sb.append("/// Class.forName for the simulator and desktop runs.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        for (ProtoClass pc : classes) {
            sb.append("        ").append(pc.codecBinaryName).append(".register();\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String simpleName(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot < 0 ? binaryName : binaryName.substring(dot + 1);
    }

    private static String packageOf(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot < 0 ? "" : binaryName.substring(0, dot);
    }

    // ----------------------------------------------------------------
    // Accumulators
    // ----------------------------------------------------------------

    enum ProtoKind {
        INT32, SINT32, FIXED32,
        INT64, SINT64, FIXED64,
        FLOAT, DOUBLE, BOOL,
        STRING, BYTES,
        MESSAGE, ENUM,
        UNSUPPORTED
    }

    static final class ProtoClass {
        String binaryName;
        String simpleName;
        String packageName;
        String codecSimpleName;
        String codecBinaryName;
        boolean isRecord;
        final List<ProtoFieldInfo> fields = new ArrayList<ProtoFieldInfo>();
    }

    static final class ProtoFieldInfo {
        String name;
        String descriptor;
        String signature;
        int tag;
        String wireKind = "DEFAULT";
        boolean repeated;
        boolean boxed;
        ProtoKind kind;
        boolean pendingReference;
        String referenceInternalName;
        String elementDescriptor;
    }
}
