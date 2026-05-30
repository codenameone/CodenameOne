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
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Build-time `@Mapped` processor. Scans the project's compiled classes for
/// `@Mapped` types, validates each one (concrete class, public no-arg
/// constructor, supported field types), then generates:
///
/// 1. One `<SimpleName>Cn1Mapper` Java class per `@Mapped` type, in the
///    **same package as the source class** (so the generated artifact
///    lives alongside the model it describes), implementing
///    `com.codename1.mapping.Mapper<XXX>`. Each has a public
///    `static register()` method that installs an instance in
///    `Mappers`.
/// 2. A single `cn1app.MapperBootstrap` whose no-arg constructor calls
///    `UserCn1Mapper.register()`, `ItemCn1Mapper.register()`, ... for every
///    accepted `@Mapped` class. The build server probes the project zip
///    for this class and splices `new cn1app.MapperBootstrap();` into the
///    iOS / Android per-build application stub before `Display.init`;
///    `JavaSEPort#postInit` loads it via `Class.forName`. Direct symbol
///    references survive ParparVM rename and R8 obfuscation; the JavaSE
///    classloading path is the legitimate exception (unobfuscated run).
public final class MappingAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String MAPPED_DESC = "Lcom/codename1/annotations/Mapped;";
    public static final String JSON_PROPERTY_DESC = "Lcom/codename1/annotations/JsonProperty;";
    public static final String JSON_IGNORE_DESC = "Lcom/codename1/annotations/JsonIgnore;";
    public static final String XML_ROOT_DESC = "Lcom/codename1/annotations/XmlRoot;";
    public static final String XML_ELEMENT_DESC = "Lcom/codename1/annotations/XmlElement;";
    public static final String XML_ATTRIBUTE_DESC = "Lcom/codename1/annotations/XmlAttribute;";
    public static final String XML_TRANSIENT_DESC = "Lcom/codename1/annotations/XmlTransient;";

    static final String BOOTSTRAP_BINARY = "cn1app.MapperBootstrap";
    static final String BOOTSTRAP_SIMPLE = "MapperBootstrap";
    static final String BOOTSTRAP_PACKAGE = "cn1app";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(MAPPED_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    /// Accepted classes keyed by binary name. TreeMap so the emitted index is
    /// deterministic regardless of scan order.
    private final TreeMap<String, MappedClass> accepted = new TreeMap<String, MappedClass>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) return;
        if (cls.getClassAnnotation(MAPPED_DESC) == null) return;
        boolean isRecord = cls.isRecord();
        if (!isRecord && (cls.isAbstract() || cls.isInterface())) {
            ctx.error(cls, "@Mapped requires a concrete class; "
                    + cls.getBinaryName() + " is abstract or an interface");
            return;
        }
        if (!isRecord && !hasPublicNoArgConstructor(cls)) {
            ctx.error(cls, "@Mapped class " + cls.getBinaryName()
                    + " must declare a public no-arg constructor for fromJson / fromXml");
            return;
        }

        MappedClass mc = new MappedClass();
        mc.binaryName = cls.getBinaryName();
        mc.simpleName = simpleName(cls.getBinaryName());
        mc.packageName = packageOf(cls.getBinaryName());
        mc.mapperSimpleName = mc.simpleName + "Cn1Mapper";
        mc.mapperBinaryName = (mc.packageName.length() == 0)
                ? mc.mapperSimpleName
                : mc.packageName + "." + mc.mapperSimpleName;
        mc.isRecord = isRecord;

        AnnotationValues xmlRoot = cls.getClassAnnotation(XML_ROOT_DESC);
        if (xmlRoot != null) {
            String v = xmlRoot.getString("value");
            mc.xmlRootName = (v == null || v.length() == 0) ? deriveXmlRoot(mc.simpleName) : v;
        } else {
            mc.xmlRootName = deriveXmlRoot(mc.simpleName);
        }

        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic()) continue;
            if (f.getName().startsWith("this$")) continue; // inner-class outer ref
            boolean useAccessor = false;
            String getterName = null;
            String setterName = null;
            if (!isRecord && !f.isPublic()) {
                // POJO with non-public field: try the JavaBeans path. A
                // matching `getX()` (or `isX()` for booleans) plus a
                // `setX(FieldType)` on the same class promotes the field
                // to a first-class @Mapped target. Property / ListProperty
                // fields only need the getter -- the field itself is not
                // replaced, only its inner value mutated through .set(...) /
                // .add(...).
                PropertyTypeKind probeKind = PropertyTypeKind.of(f);
                String getter = findGetter(cls, f, probeKind);
                if (getter == null) {
                    ctx.getLog().debug("cn1: @Mapped skipping private field "
                            + f.getName() + " on " + cls.getBinaryName()
                            + " (no matching getter/setter)");
                    continue;
                }
                boolean needsSetter = !(probeKind.kind == PropertyTypeKind.Kind.PROPERTY
                        || probeKind.kind == PropertyTypeKind.Kind.LIST_PROPERTY);
                String setter = needsSetter ? findSetter(cls, f) : null;
                if (needsSetter && setter == null) {
                    ctx.getLog().debug("cn1: @Mapped skipping private field "
                            + f.getName() + " on " + cls.getBinaryName()
                            + " (no matching getter/setter)");
                    continue;
                }
                useAccessor = true;
                getterName = getter;
                setterName = setter;
            }
            MappedField mf = new MappedField();
            mf.name = f.getName();
            mf.kind = PropertyTypeKind.of(f);
            mf.useAccessor = useAccessor;
            mf.getterName = getterName;
            mf.setterName = setterName;
            if (isRecord) {
                if (mf.kind.kind == PropertyTypeKind.Kind.PROPERTY
                        || mf.kind.kind == PropertyTypeKind.Kind.LIST_PROPERTY) {
                    ctx.error(cls, "@Mapped record " + mc.binaryName
                            + " cannot use Property / ListProperty for component "
                            + mf.name + " -- records are immutable");
                    continue;
                }
            }
            mf.jsonName = mf.name;
            mf.xmlName = mf.name;
            mf.xmlAttribute = false;
            mf.includeInJson = true;
            mf.includeInXml = true;

            AnnotationValues jp = f.getAnnotation(JSON_PROPERTY_DESC);
            if (jp != null) {
                String v = jp.getString("value");
                if (v != null && v.length() > 0) mf.jsonName = v;
            }
            if (f.getAnnotation(JSON_IGNORE_DESC) != null) {
                mf.includeInJson = false;
            }
            AnnotationValues xe = f.getAnnotation(XML_ELEMENT_DESC);
            if (xe != null) {
                String v = xe.getString("value");
                if (v != null && v.length() > 0) mf.xmlName = v;
            }
            AnnotationValues xa = f.getAnnotation(XML_ATTRIBUTE_DESC);
            if (xa != null) {
                mf.xmlAttribute = true;
                String v = xa.getString("value");
                if (v != null && v.length() > 0) mf.xmlName = v;
            }
            if (f.getAnnotation(XML_TRANSIENT_DESC) != null) {
                mf.includeInXml = false;
            }
            if (mf.kind.kind == PropertyTypeKind.Kind.UNSUPPORTED) {
                ctx.error(cls, "@Mapped field " + mf.name + " on " + mc.binaryName
                        + " has an unsupported type (descriptor " + f.getDescriptor() + ")");
                continue;
            }
            if (mf.xmlAttribute && !canBeAttribute(mf.kind)) {
                ctx.error(cls, "@XmlAttribute on " + mc.binaryName + "." + mf.name
                        + " requires a scalar / Property<scalar> field");
                continue;
            }
            mc.fields.add(mf);
        }

        accepted.put(mc.binaryName, mc);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) return;
        if (accepted.isEmpty()) return;

        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (MappedClass mc : accepted.values()) {
            sources.put(mc.mapperBinaryName, generateMapperSource(mc));
        }
        sources.put(BOOTSTRAP_BINARY, generateBootstrapSource(accepted.values()));

        try {
            // The output directory holds the application's @Mapped types --
            // we reference them by direct symbol in the generated mappers, so
            // it has to be on the compile classpath.
            java.util.List<java.io.File> cp = new java.util.ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated mapper sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size()
                + " @Mapped mapper(s) + " + BOOTSTRAP_BINARY);
    }

    // ---------------------------------------------------------------
    // Source generation
    // ---------------------------------------------------------------

    /// Returns the Java expression that reads the field's current value
    /// from the runtime instance `o`. Records use the synthesised accessor
    /// (`o.name()`); JavaBeans-style POJOs use their public getter
    /// (`o.getName()` / `o.isFlag()`); plain POJOs use direct field access
    /// (`o.name`).
    private static String readExpr(MappedField f, boolean isRecord) {
        if (isRecord) return "o." + f.name + "()";
        if (f.useAccessor) return "o." + f.getterName + "()";
        return "o." + f.name;
    }

    /// Returns a full Java statement (terminated with `;`) that writes
    /// `rhsExpr` into the field for the runtime instance `o`. Records
    /// can't mutate after construction, so we accumulate in a local of
    /// the form `_name` for later canonical-constructor invocation;
    /// JavaBeans POJOs route through `o.setName(rhsExpr)`; plain POJOs
    /// assign the field directly.
    private static String writeStmt(MappedField f, boolean isRecord, String rhsExpr) {
        if (isRecord) return "_" + f.name + " = " + rhsExpr + ";";
        if (f.useAccessor) return "o." + f.setterName + "(" + rhsExpr + ");";
        return "o." + f.name + " = " + rhsExpr + ";";
    }

    /// The literal Java initializer for a freshly-declared local of the
    /// given kind -- used when seeding the per-component locals that
    /// feed a record's canonical constructor.
    private static String defaultLiteral(PropertyTypeKind k) {
        switch (k.kind) {
            case INT: case SHORT: case BYTE: case CHAR: return "0";
            case LONG: return "0L";
            case DOUBLE: return "0.0";
            case FLOAT: return "0.0f";
            case BOOLEAN: return "false";
            default: return "null";
        }
    }

    /// The Java type literal used to declare a local variable carrying
    /// the field's value (records-only: the locals are later fed to the
    /// canonical constructor in declaration order).
    private static String fieldType(MappedField f) {
        switch (f.kind.kind) {
            case STRING:     return "String";
            case INT:        return "int";
            case LONG:       return "long";
            case SHORT:      return "short";
            case BYTE:       return "byte";
            case CHAR:       return "char";
            case DOUBLE:     return "double";
            case FLOAT:      return "float";
            case BOOLEAN:    return "boolean";
            case DATE:       return "java.util.Date";
            case BYTE_ARRAY: return "byte[]";
            // PROPERTY / LIST_PROPERTY are rejected upstream for records;
            // they share the same fallback shape as a plain REFERENCE so the
            // helper can't NPE if it's ever reached from a non-record path.
            case REFERENCE:
            case PROPERTY:
            case LIST_PROPERTY:
                return f.kind.binaryName;
            case LIST:       return "java.util.List<" + f.kind.elementBinaryName + ">";
            default:
                return "Object";
        }
    }

    private static String generateMapperSource(MappedClass mc) {
        StringBuilder sb = new StringBuilder(2048);
        if (mc.packageName.length() > 0) {
            sb.append("package ").append(mc.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(mc.mapperSimpleName)
                .append(" implements com.codename1.mapping.Mapper<").append(mc.binaryName).append("> {\n\n");

        // Public static register() hook. The bootstrap class invokes
        // this once per generated mapper at app start; the call
        // triggers this class's <clinit> and installs the mapper in
        // the Mappers registry.
        sb.append("    public static void register() {\n");
        sb.append("        com.codename1.mapping.Mappers.register(new ").append(mc.mapperSimpleName).append("());\n");
        sb.append("    }\n\n");

        sb.append("    public ").append(mc.mapperSimpleName).append("() {\n");
        sb.append("    }\n\n");

        // type()
        sb.append("    public Class<").append(mc.binaryName).append("> type() {\n");
        sb.append("        return ").append(mc.binaryName).append(".class;\n");
        sb.append("    }\n\n");

        // xmlRootName()
        sb.append("    public String xmlRootName() {\n");
        sb.append("        return \"").append(escape(mc.xmlRootName)).append("\";\n");
        sb.append("    }\n\n");

        boolean isRecord = mc.isRecord;

        // toMap() -- reads are identical in shape regardless of record-ness;
        // `readExpr` picks between `o.name` (POJO) and `o.name()` (record).
        sb.append("    public java.util.Map<String, Object> toMap(").append(mc.binaryName).append(" o) {\n");
        sb.append("        java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<String, Object>();\n");
        sb.append("        if (o == null) return m;\n");
        for (MappedField f : mc.fields) {
            if (!f.includeInJson) continue;
            emitFieldToMap(sb, f, isRecord);
        }
        sb.append("        return m;\n");
        sb.append("    }\n\n");

        // fromMap() -- POJO mutates an instance in-place; record accumulates
        // per-component locals and feeds them to the canonical constructor.
        sb.append("    public ").append(mc.binaryName)
                .append(" fromMap(java.util.Map<String, Object> m) {\n");
        if (isRecord) {
            for (MappedField f : mc.fields) {
                sb.append("        ").append(fieldType(f)).append(" _")
                        .append(f.name).append(" = ").append(defaultLiteral(f.kind)).append(";\n");
            }
            sb.append("        if (m == null) return ");
            appendRecordCtorCall(sb, mc);
            sb.append(";\n");
            for (MappedField f : mc.fields) {
                if (!f.includeInJson) continue;
                emitFieldFromMap(sb, f, true);
            }
            sb.append("        return ");
            appendRecordCtorCall(sb, mc);
            sb.append(";\n");
        } else {
            sb.append("        ").append(mc.binaryName).append(" o = new ").append(mc.binaryName).append("();\n");
            sb.append("        if (m == null) return o;\n");
            for (MappedField f : mc.fields) {
                if (!f.includeInJson) continue;
                emitFieldFromMap(sb, f, false);
            }
            sb.append("        return o;\n");
        }
        sb.append("    }\n\n");

        // writeXml()
        sb.append("    public void writeXml(").append(mc.binaryName)
                .append(" o, com.codename1.xml.Element root) {\n");
        sb.append("        if (o == null) return;\n");
        for (MappedField f : mc.fields) {
            if (!f.includeInXml) continue;
            emitFieldToXml(sb, f, isRecord);
        }
        sb.append("    }\n\n");

        // readXml() -- same fork as fromMap.
        sb.append("    public ").append(mc.binaryName)
                .append(" readXml(com.codename1.xml.Element root) {\n");
        if (isRecord) {
            for (MappedField f : mc.fields) {
                sb.append("        ").append(fieldType(f)).append(" _")
                        .append(f.name).append(" = ").append(defaultLiteral(f.kind)).append(";\n");
            }
            sb.append("        if (root == null) return ");
            appendRecordCtorCall(sb, mc);
            sb.append(";\n");
            for (MappedField f : mc.fields) {
                if (!f.includeInXml) continue;
                emitFieldFromXml(sb, f, true);
            }
            sb.append("        return ");
            appendRecordCtorCall(sb, mc);
            sb.append(";\n");
        } else {
            sb.append("        ").append(mc.binaryName).append(" o = new ").append(mc.binaryName).append("();\n");
            sb.append("        if (root == null) return o;\n");
            for (MappedField f : mc.fields) {
                if (!f.includeInXml) continue;
                emitFieldFromXml(sb, f, false);
            }
            sb.append("        return o;\n");
        }
        sb.append("    }\n\n");

        // textOf helper -- inlined per-mapper to keep each generated class
        // self-contained (no shared runtime dependency beyond Mappers).
        sb.append("    private static String textOf(com.codename1.xml.Element e) {\n");
        sb.append("        if (e == null) return null;\n");
        sb.append("        if (e.isTextElement()) return e.getText();\n");
        sb.append("        int n = e.getNumChildren();\n");
        sb.append("        for (int i = 0; i < n; i++) {\n");
        sb.append("            com.codename1.xml.Element c = e.getChildAt(i);\n");
        sb.append("            if (c.isTextElement()) return c.getText();\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n");

        sb.append("}\n");
        return sb.toString();
    }

    /// Appends `new pkg.Type(_a, _b, ...)` to `sb`, where the args are the
    /// per-component locals declared at the top of a record `fromMap` /
    /// `readXml`. Order tracks `mc.fields` which is the class-file
    /// declaration order, identical to the canonical constructor's
    /// parameter order.
    private static void appendRecordCtorCall(StringBuilder sb, MappedClass mc) {
        sb.append("new ").append(mc.binaryName).append("(");
        boolean first = true;
        for (MappedField f : mc.fields) {
            if (!first) sb.append(", ");
            sb.append("_").append(f.name);
            first = false;
        }
        sb.append(")");
    }

    private static String generateBootstrapSource(Iterable<MappedClass> classes) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        sb.append("/// JSON / XML mapper bootstrap. The iOS / Android per-build\n");
        sb.append("/// application stub instantiates this class before Display.init\n");
        sb.append("/// (the build server probes the project zip for it and emits the\n");
        sb.append("/// install line conditionally); JavaSEPort.postInit picks it up\n");
        sb.append("/// via Class.forName for the simulator and desktop runs.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        for (MappedClass mc : classes) {
            sb.append("        ").append(mc.mapperBinaryName).append(".register();\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String packageOf(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? "" : binary.substring(0, dot);
    }

    // ---------------------------------------------------------------
    // toMap field-emit helpers
    // ---------------------------------------------------------------

    private static void emitFieldToMap(StringBuilder sb, MappedField f, boolean isRecord) {
        String key = "\"" + escape(f.jsonName) + "\"";
        String read = readExpr(f, isRecord);
        switch (f.kind.kind) {
            case STRING: case INT: case LONG: case SHORT: case BYTE: case CHAR:
            case DOUBLE: case FLOAT: case BOOLEAN:
                sb.append("        m.put(").append(key).append(", ").append(read).append(");\n");
                return;
            case DATE:
                sb.append("        m.put(").append(key).append(", ").append(read)
                        .append(" == null ? null : Long.valueOf(").append(read).append(".getTime()));\n");
                return;
            case BYTE_ARRAY:
                sb.append("        m.put(").append(key).append(", ").append(read)
                        .append(" == null ? null : com.codename1.util.Base64.encode(").append(read).append("));\n");
                return;
            case PROPERTY:
                sb.append("        m.put(").append(key).append(", ").append(read).append(".get());\n");
                return;
            case LIST: case LIST_PROPERTY:
                sb.append("        {\n");
                sb.append("            java.util.ArrayList<Object> _l = new java.util.ArrayList<Object>();\n");
                if (f.kind.kind == PropertyTypeKind.Kind.LIST) {
                    sb.append("            java.util.List _src = ").append(read).append(";\n");
                } else {
                    sb.append("            java.util.List _src = ").append(read).append(".asList();\n");
                }
                sb.append("            if (_src != null) {\n");
                sb.append("                for (Object _e : _src) {\n");
                if (isScalarBinary(f.kind.elementBinaryName)) {
                    sb.append("                    _l.add(_e);\n");
                } else if ("java.util.Date".equals(f.kind.elementBinaryName)) {
                    sb.append("                    _l.add(_e == null ? null : Long.valueOf(((java.util.Date) _e).getTime()));\n");
                } else {
                    sb.append("                    com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.elementBinaryName).append(".class);\n");
                    sb.append("                    _l.add(_nm == null || _e == null ? _e : _nm.toMap(_e));\n");
                }
                sb.append("                }\n");
                sb.append("            }\n");
                sb.append("            m.put(").append(key).append(", _l);\n");
                sb.append("        }\n");
                return;
            case REFERENCE:
                sb.append("        {\n");
                sb.append("            Object _v = ").append(read).append(";\n");
                sb.append("            if (_v == null) { m.put(").append(key).append(", null); }\n");
                sb.append("            else {\n");
                sb.append("                com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.binaryName).append(".class);\n");
                sb.append("                m.put(").append(key).append(", _nm == null ? _v.toString() : _nm.toMap(_v));\n");
                sb.append("            }\n");
                sb.append("        }\n");
                return;
            default:
                return;
        }
    }

    // ---------------------------------------------------------------
    // fromMap field-emit helpers
    // ---------------------------------------------------------------

    private static void emitFieldFromMap(StringBuilder sb, MappedField f, boolean isRecord) {
        String key = "\"" + escape(f.jsonName) + "\"";
        sb.append("        {\n");
        sb.append("            Object _v = m.get(").append(key).append(");\n");
        sb.append("            if (_v != null) {\n");
        switch (f.kind.kind) {
            case STRING:
                sb.append("                ").append(writeStmt(f, isRecord, "_v.toString()")).append("\n");
                break;
            case INT:
                sb.append("                ").append(writeStmt(f, isRecord, "((Number) _v).intValue()")).append("\n");
                break;
            case LONG:
                sb.append("                ").append(writeStmt(f, isRecord, "((Number) _v).longValue()")).append("\n");
                break;
            case SHORT:
                sb.append("                ").append(writeStmt(f, isRecord, "((Number) _v).shortValue()")).append("\n");
                break;
            case BYTE:
                sb.append("                ").append(writeStmt(f, isRecord, "((Number) _v).byteValue()")).append("\n");
                break;
            case CHAR:
                sb.append("                ").append(writeStmt(f, isRecord, "_v.toString().length() == 0 ? '\\0' : _v.toString().charAt(0)")).append("\n");
                break;
            case DOUBLE:
                sb.append("                ").append(writeStmt(f, isRecord, "((Number) _v).doubleValue()")).append("\n");
                break;
            case FLOAT:
                sb.append("                ").append(writeStmt(f, isRecord, "((Number) _v).floatValue()")).append("\n");
                break;
            case BOOLEAN:
                sb.append("                ").append(writeStmt(f, isRecord, "(_v instanceof Boolean) ? ((Boolean) _v).booleanValue() : Boolean.parseBoolean(_v.toString())")).append("\n");
                break;
            case DATE:
                sb.append("                ").append(writeStmt(f, isRecord, "new java.util.Date(((Number) _v).longValue())")).append("\n");
                break;
            case BYTE_ARRAY:
                sb.append("                ").append(writeStmt(f, isRecord, "com.codename1.util.Base64.decode(_v.toString().getBytes())")).append("\n");
                break;
            case PROPERTY:
                emitPropertySetFromJsonValue(sb, f, isRecord);
                break;
            case REFERENCE:
                sb.append("                com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.binaryName).append(".class);\n");
                sb.append("                if (_nm != null && _v instanceof java.util.Map) {\n");
                sb.append("                    ").append(writeStmt(f, isRecord, "(" + f.kind.binaryName + ") _nm.fromMap((java.util.Map) _v)")).append("\n");
                sb.append("                }\n");
                break;
            case LIST: case LIST_PROPERTY:
                // LIST_PROPERTY mutates o.field.clear()/add(...) -- safe for
                // POJO path only (records reject LIST_PROPERTY upstream). For
                // a record's LIST we materialize a local _l and assign it to
                // the per-component local that later feeds the canonical ctor.
                sb.append("                if (_v instanceof java.util.List) {\n");
                if (isScalarBinary(f.kind.elementBinaryName)) {
                    if (f.kind.kind == PropertyTypeKind.Kind.LIST) {
                        sb.append("                    java.util.ArrayList<").append(f.kind.elementBinaryName).append("> _l = new java.util.ArrayList<").append(f.kind.elementBinaryName).append(">();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) { _l.add((").append(f.kind.elementBinaryName).append(") _e); }\n");
                        sb.append("                    ").append(writeStmt(f, isRecord, "_l")).append("\n");
                    } else {
                        sb.append("                    ").append(readExpr(f, isRecord)).append(".clear();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) { ").append(readExpr(f, isRecord)).append(".add((").append(f.kind.elementBinaryName).append(") _e); }\n");
                    }
                } else if ("java.util.Date".equals(f.kind.elementBinaryName)) {
                    if (f.kind.kind == PropertyTypeKind.Kind.LIST) {
                        sb.append("                    java.util.ArrayList<java.util.Date> _l = new java.util.ArrayList<java.util.Date>();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) { _l.add(_e == null ? null : new java.util.Date(((Number) _e).longValue())); }\n");
                        sb.append("                    ").append(writeStmt(f, isRecord, "_l")).append("\n");
                    } else {
                        sb.append("                    ").append(readExpr(f, isRecord)).append(".clear();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) { ").append(readExpr(f, isRecord)).append(".add(_e == null ? null : new java.util.Date(((Number) _e).longValue())); }\n");
                    }
                } else {
                    sb.append("                    com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.elementBinaryName).append(".class);\n");
                    if (f.kind.kind == PropertyTypeKind.Kind.LIST) {
                        sb.append("                    java.util.ArrayList<").append(f.kind.elementBinaryName).append("> _l = new java.util.ArrayList<").append(f.kind.elementBinaryName).append(">();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) {\n");
                        sb.append("                        if (_nm != null && _e instanceof java.util.Map) { _l.add((").append(f.kind.elementBinaryName).append(") _nm.fromMap((java.util.Map) _e)); }\n");
                        sb.append("                    }\n");
                        sb.append("                    ").append(writeStmt(f, isRecord, "_l")).append("\n");
                    } else {
                        sb.append("                    ").append(readExpr(f, isRecord)).append(".clear();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) {\n");
                        sb.append("                        if (_nm != null && _e instanceof java.util.Map) { ").append(readExpr(f, isRecord)).append(".add((").append(f.kind.elementBinaryName).append(") _nm.fromMap((java.util.Map) _e)); }\n");
                        sb.append("                    }\n");
                    }
                }
                sb.append("                }\n");
                break;
            default:
                break;
        }
        sb.append("            }\n");
        sb.append("        }\n");
    }

    private static void emitPropertySetFromJsonValue(StringBuilder sb, MappedField f, boolean isRecord) {
        // PROPERTY fields are rejected upstream for records; read through
        // `readExpr` anyway so the helper is safe if it ever runs in the
        // record path.
        String prop = readExpr(f, isRecord);
        String elem = f.kind.elementBinaryName;
        if ("java.lang.String".equals(elem)) {
            sb.append("                ").append(prop).append(".set(_v.toString());\n");
        } else if ("java.lang.Integer".equals(elem)) {
            sb.append("                ").append(prop).append(".set(Integer.valueOf(((Number) _v).intValue()));\n");
        } else if ("java.lang.Long".equals(elem)) {
            sb.append("                ").append(prop).append(".set(Long.valueOf(((Number) _v).longValue()));\n");
        } else if ("java.lang.Double".equals(elem)) {
            sb.append("                ").append(prop).append(".set(Double.valueOf(((Number) _v).doubleValue()));\n");
        } else if ("java.lang.Float".equals(elem)) {
            sb.append("                ").append(prop).append(".set(Float.valueOf(((Number) _v).floatValue()));\n");
        } else if ("java.lang.Boolean".equals(elem)) {
            sb.append("                ").append(prop).append(".set((_v instanceof Boolean) ? (Boolean) _v : Boolean.valueOf(_v.toString()));\n");
        } else if ("java.util.Date".equals(elem)) {
            sb.append("                ").append(prop).append(".set(new java.util.Date(((Number) _v).longValue()));\n");
        } else {
            // Nested Property<T> where T is another mapped type.
            sb.append("                com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(elem).append(".class);\n");
            sb.append("                if (_nm != null && _v instanceof java.util.Map) {\n");
            sb.append("                    ").append(prop).append(".set((").append(elem).append(") _nm.fromMap((java.util.Map) _v));\n");
            sb.append("                }\n");
        }
    }

    // ---------------------------------------------------------------
    // XML helpers
    // ---------------------------------------------------------------

    private static void emitFieldToXml(StringBuilder sb, MappedField f, boolean isRecord) {
        String read = readExpr(f, isRecord);
        if (f.xmlAttribute) {
            sb.append("        {\n");
            sb.append("            String _s = ");
            emitScalarToString(sb, f, isRecord);
            sb.append(";\n");
            sb.append("            if (_s != null) root.setAttribute(\"").append(escape(f.xmlName)).append("\", _s);\n");
            sb.append("        }\n");
            return;
        }
        switch (f.kind.kind) {
            case STRING: case INT: case LONG: case SHORT: case BYTE: case CHAR:
            case DOUBLE: case FLOAT: case BOOLEAN: case DATE: case BYTE_ARRAY:
            case PROPERTY:
                sb.append("        {\n");
                sb.append("            String _s = ");
                emitScalarToString(sb, f, isRecord);
                sb.append(";\n");
                sb.append("            if (_s != null) {\n");
                sb.append("                com.codename1.xml.Element _e = new com.codename1.xml.Element(\"").append(escape(f.xmlName)).append("\");\n");
                sb.append("                com.codename1.xml.Element _txt = new com.codename1.xml.Element(_s, true);\n");
                sb.append("                _e.addChild(_txt);\n");
                sb.append("                root.addChild(_e);\n");
                sb.append("            }\n");
                sb.append("        }\n");
                return;
            case REFERENCE:
                sb.append("        if (").append(read).append(" != null) {\n");
                sb.append("            com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.binaryName).append(".class);\n");
                sb.append("            if (_nm != null) {\n");
                sb.append("                com.codename1.xml.Element _e = new com.codename1.xml.Element(\"").append(escape(f.xmlName)).append("\");\n");
                sb.append("                _nm.writeXml(").append(read).append(", _e);\n");
                sb.append("                root.addChild(_e);\n");
                sb.append("            }\n");
                sb.append("        }\n");
                return;
            case LIST: case LIST_PROPERTY:
                sb.append("        {\n");
                if (f.kind.kind == PropertyTypeKind.Kind.LIST) {
                    sb.append("            java.util.List _src = ").append(read).append(";\n");
                } else {
                    sb.append("            java.util.List _src = ").append(read).append(".asList();\n");
                }
                sb.append("            if (_src != null) {\n");
                sb.append("                for (Object _e : _src) {\n");
                sb.append("                    com.codename1.xml.Element _el = new com.codename1.xml.Element(\"").append(escape(f.xmlName)).append("\");\n");
                if (isScalarBinary(f.kind.elementBinaryName) || "java.util.Date".equals(f.kind.elementBinaryName)) {
                    sb.append("                    if (_e != null) {\n");
                    if ("java.util.Date".equals(f.kind.elementBinaryName)) {
                        sb.append("                        _el.addChild(new com.codename1.xml.Element(String.valueOf(((java.util.Date) _e).getTime()), true));\n");
                    } else {
                        sb.append("                        _el.addChild(new com.codename1.xml.Element(String.valueOf(_e), true));\n");
                    }
                    sb.append("                    }\n");
                } else {
                    sb.append("                    com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.elementBinaryName).append(".class);\n");
                    sb.append("                    if (_nm != null && _e != null) _nm.writeXml(_e, _el);\n");
                }
                sb.append("                    root.addChild(_el);\n");
                sb.append("                }\n");
                sb.append("            }\n");
                sb.append("        }\n");
                return;
            default:
                return;
        }
    }

    private static void emitFieldFromXml(StringBuilder sb, MappedField f, boolean isRecord) {
        if (f.xmlAttribute) {
            sb.append("        {\n");
            sb.append("            String _s = root.getAttribute(\"").append(escape(f.xmlName)).append("\");\n");
            sb.append("            if (_s != null) {\n");
            emitScalarFromString(sb, f, "_s", isRecord);
            sb.append("            }\n");
            sb.append("        }\n");
            return;
        }
        sb.append("        {\n");
        sb.append("            java.util.Vector _kids = root.getChildrenByTagName(\"").append(escape(f.xmlName)).append("\");\n");
        switch (f.kind.kind) {
            case LIST:
                sb.append("            java.util.ArrayList<").append(f.kind.elementBinaryName).append("> _l = new java.util.ArrayList<").append(f.kind.elementBinaryName).append(">();\n");
                sb.append("            for (int _i = 0; _i < _kids.size(); _i++) {\n");
                sb.append("                com.codename1.xml.Element _ch = (com.codename1.xml.Element) _kids.elementAt(_i);\n");
                emitListElementFromXml(sb, f, "_ch", "_l");
                sb.append("            }\n");
                sb.append("            ").append(writeStmt(f, isRecord, "_l")).append("\n");
                break;
            case LIST_PROPERTY:
                // Rejected upstream for records; reads through `readExpr`
                // which yields `o.name` for POJO public fields or
                // `o.getName()` for JavaBeans accessor POJOs.
                sb.append("            ").append(readExpr(f, isRecord)).append(".clear();\n");
                sb.append("            for (int _i = 0; _i < _kids.size(); _i++) {\n");
                sb.append("                com.codename1.xml.Element _ch = (com.codename1.xml.Element) _kids.elementAt(_i);\n");
                emitListElementFromXml(sb, f, "_ch", readExpr(f, isRecord));
                sb.append("            }\n");
                break;
            default:
                sb.append("            if (_kids.size() > 0) {\n");
                sb.append("                com.codename1.xml.Element _e = (com.codename1.xml.Element) _kids.elementAt(0);\n");
                if (f.kind.kind == PropertyTypeKind.Kind.REFERENCE) {
                    sb.append("                com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.binaryName).append(".class);\n");
                    sb.append("                if (_nm != null) ").append(writeStmt(f, isRecord, "(" + f.kind.binaryName + ") _nm.readXml(_e)")).append("\n");
                } else {
                    sb.append("                String _s = textOf(_e);\n");
                    sb.append("                if (_s != null) {\n");
                    emitScalarFromString(sb, f, "_s", isRecord);
                    sb.append("                }\n");
                }
                sb.append("            }\n");
                break;
        }
        sb.append("        }\n");
    }

    private static void emitListElementFromXml(StringBuilder sb, MappedField f, String elemVar, String sink) {
        String elem = f.kind.elementBinaryName;
        if (isScalarBinary(elem)) {
            sb.append("                String _s = textOf(").append(elemVar).append(");\n");
            sb.append("                if (_s != null) {\n");
            if ("java.lang.String".equals(elem)) {
                sb.append("                    ").append(sink).append(".add(_s);\n");
            } else if ("java.lang.Integer".equals(elem)) {
                sb.append("                    ").append(sink).append(".add(Integer.valueOf(_s));\n");
            } else if ("java.lang.Long".equals(elem)) {
                sb.append("                    ").append(sink).append(".add(Long.valueOf(_s));\n");
            } else if ("java.lang.Double".equals(elem)) {
                sb.append("                    ").append(sink).append(".add(Double.valueOf(_s));\n");
            } else if ("java.lang.Float".equals(elem)) {
                sb.append("                    ").append(sink).append(".add(Float.valueOf(_s));\n");
            } else if ("java.lang.Boolean".equals(elem)) {
                sb.append("                    ").append(sink).append(".add(Boolean.valueOf(_s));\n");
            } else {
                sb.append("                    ").append(sink).append(".add(_s);\n");
            }
            sb.append("                }\n");
        } else if ("java.util.Date".equals(elem)) {
            sb.append("                String _s = textOf(").append(elemVar).append(");\n");
            sb.append("                if (_s != null) ").append(sink).append(".add(new java.util.Date(Long.parseLong(_s)));\n");
        } else {
            sb.append("                com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(elem).append(".class);\n");
            sb.append("                if (_nm != null) ").append(sink).append(".add((").append(elem).append(") _nm.readXml(").append(elemVar).append("));\n");
        }
    }

    private static void emitScalarToString(StringBuilder sb, MappedField f, boolean isRecord) {
        String read = readExpr(f, isRecord);
        switch (f.kind.kind) {
            case STRING:
                sb.append(read); break;
            case INT: case LONG: case SHORT: case BYTE: case CHAR:
            case DOUBLE: case FLOAT: case BOOLEAN:
                sb.append("String.valueOf(").append(read).append(")"); break;
            case DATE:
                sb.append(read).append(" == null ? null : String.valueOf(").append(read).append(".getTime())"); break;
            case BYTE_ARRAY:
                sb.append(read).append(" == null ? null : com.codename1.util.Base64.encode(").append(read).append(")"); break;
            case PROPERTY:
                sb.append(read).append(".get() == null ? null : ");
                String elem = f.kind.elementBinaryName;
                if ("java.util.Date".equals(elem)) {
                    sb.append("String.valueOf(((java.util.Date) ").append(read).append(".get()).getTime())");
                } else {
                    sb.append("String.valueOf(").append(read).append(".get())");
                }
                break;
            default:
                sb.append("null");
        }
    }

    private static void emitScalarFromString(StringBuilder sb, MappedField f, String src, boolean isRecord) {
        // PROPERTY is rejected for records, but `read` keeps the helper safe
        // if it ever reaches the record path.
        String read = readExpr(f, isRecord);
        switch (f.kind.kind) {
            case STRING:
                sb.append("                ").append(writeStmt(f, isRecord, src)).append("\n"); break;
            case INT:
                sb.append("                ").append(writeStmt(f, isRecord, "Integer.parseInt(" + src + ")")).append("\n"); break;
            case LONG:
                sb.append("                ").append(writeStmt(f, isRecord, "Long.parseLong(" + src + ")")).append("\n"); break;
            case SHORT:
                sb.append("                ").append(writeStmt(f, isRecord, "Short.parseShort(" + src + ")")).append("\n"); break;
            case BYTE:
                sb.append("                ").append(writeStmt(f, isRecord, "Byte.parseByte(" + src + ")")).append("\n"); break;
            case CHAR:
                sb.append("                ").append(writeStmt(f, isRecord, src + ".length() == 0 ? '\\0' : " + src + ".charAt(0)")).append("\n"); break;
            case DOUBLE:
                sb.append("                ").append(writeStmt(f, isRecord, "Double.parseDouble(" + src + ")")).append("\n"); break;
            case FLOAT:
                sb.append("                ").append(writeStmt(f, isRecord, "Float.parseFloat(" + src + ")")).append("\n"); break;
            case BOOLEAN:
                sb.append("                ").append(writeStmt(f, isRecord, "Boolean.parseBoolean(" + src + ")")).append("\n"); break;
            case DATE:
                sb.append("                ").append(writeStmt(f, isRecord, "new java.util.Date(Long.parseLong(" + src + "))")).append("\n"); break;
            case BYTE_ARRAY:
                sb.append("                ").append(writeStmt(f, isRecord, "com.codename1.util.Base64.decode(" + src + ".getBytes())")).append("\n"); break;
            case PROPERTY: {
                String elem = f.kind.elementBinaryName;
                if ("java.lang.String".equals(elem)) {
                    sb.append("                ").append(read).append(".set(").append(src).append(");\n");
                } else if ("java.lang.Integer".equals(elem)) {
                    sb.append("                ").append(read).append(".set(Integer.valueOf(").append(src).append("));\n");
                } else if ("java.lang.Long".equals(elem)) {
                    sb.append("                ").append(read).append(".set(Long.valueOf(").append(src).append("));\n");
                } else if ("java.lang.Double".equals(elem)) {
                    sb.append("                ").append(read).append(".set(Double.valueOf(").append(src).append("));\n");
                } else if ("java.lang.Float".equals(elem)) {
                    sb.append("                ").append(read).append(".set(Float.valueOf(").append(src).append("));\n");
                } else if ("java.lang.Boolean".equals(elem)) {
                    sb.append("                ").append(read).append(".set(Boolean.valueOf(").append(src).append("));\n");
                } else if ("java.util.Date".equals(elem)) {
                    sb.append("                ").append(read).append(".set(new java.util.Date(Long.parseLong(").append(src).append(")));\n");
                } else {
                    sb.append("                ").append(read).append(".set((").append(elem).append(") ").append(src).append(");\n");
                }
                break;
            }
            default:
                break;
        }
    }

    // ---------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------

    private static boolean isScalarBinary(String binary) {
        return "java.lang.String".equals(binary)
                || "java.lang.Integer".equals(binary)
                || "java.lang.Long".equals(binary)
                || "java.lang.Short".equals(binary)
                || "java.lang.Byte".equals(binary)
                || "java.lang.Character".equals(binary)
                || "java.lang.Double".equals(binary)
                || "java.lang.Float".equals(binary)
                || "java.lang.Boolean".equals(binary);
    }

    private static boolean canBeAttribute(PropertyTypeKind k) {
        if (k.isScalar()) return true;
        if (k.kind == PropertyTypeKind.Kind.PROPERTY) {
            return k.elementBinaryName != null
                    && (isScalarBinary(k.elementBinaryName)
                            || "java.util.Date".equals(k.elementBinaryName));
        }
        return false;
    }

    private static boolean hasPublicNoArgConstructor(AnnotatedClass cls) {
        for (MethodInfo m : cls.getMethods()) {
            if (m.isConstructor() && m.isPublic() && "()V".equals(m.getDescriptor())) {
                return true;
            }
        }
        return false;
    }

    /// Finds a public, non-static, no-arg instance accessor returning the
    /// field's declared type. Tries the JavaBeans variant first
    /// (`getFirstName` for field `firstName`, `isActive` for boolean field
    /// `active`), then the literal-prefix variant (`getURL` when the
    /// field is `URL`). Returns the method name or null when no accessor
    /// matches.
    private static String findGetter(AnnotatedClass cls, FieldInfo f, PropertyTypeKind kind) {
        String fieldName = f.getName();
        String fieldDesc = f.getDescriptor();
        boolean isBool = "Z".equals(fieldDesc) || "Ljava/lang/Boolean;".equals(fieldDesc);
        String beanCap = capitalizeForBean(fieldName);
        // Literal-prefix variant -- if `firstName` ⇒ already same as
        // beanCap; for `URL` the literal is `URL` itself (no extra cap).
        String[] capCandidates;
        if (fieldName.equals(beanCap)) {
            capCandidates = new String[] { beanCap };
        } else {
            capCandidates = new String[] { beanCap, fieldName };
        }
        for (String cap : capCandidates) {
            if (isBool) {
                String name = "is" + cap;
                if (hasInstanceMethod(cls, name, "()" + fieldDesc)) return name;
            }
            String getter = "get" + cap;
            if (hasInstanceMethod(cls, getter, "()" + fieldDesc)) return getter;
        }
        return null;
    }

    /// Finds a public, non-static setter `setX(FieldType)` on `cls`. The
    /// return type is intentionally ignored (be lenient: builders that
    /// return `this` count). Returns the method name or null when no
    /// setter matches.
    private static String findSetter(AnnotatedClass cls, FieldInfo f) {
        String fieldName = f.getName();
        String fieldDesc = f.getDescriptor();
        String beanCap = capitalizeForBean(fieldName);
        String[] capCandidates;
        if (fieldName.equals(beanCap)) {
            capCandidates = new String[] { beanCap };
        } else {
            capCandidates = new String[] { beanCap, fieldName };
        }
        String paramPrefix = "(" + fieldDesc + ")";
        for (String cap : capCandidates) {
            String name = "set" + cap;
            for (MethodInfo m : cls.getMethods()) {
                if (!name.equals(m.getName())) continue;
                if (!m.isPublic() || m.isStatic() || m.isAbstract()) continue;
                String d = m.getDescriptor();
                if (d != null && d.startsWith(paramPrefix)) return name;
            }
        }
        return null;
    }

    private static boolean hasInstanceMethod(AnnotatedClass cls, String name, String descriptor) {
        for (MethodInfo m : cls.getMethods()) {
            if (!name.equals(m.getName())) continue;
            if (!m.isPublic() || m.isStatic() || m.isAbstract()) continue;
            if (descriptor.equals(m.getDescriptor())) return true;
        }
        return false;
    }

    /// JavaBeans capitalisation: lower-case first letter goes upper
    /// (`firstName` ⇒ `FirstName`). When the first two letters are both
    /// upper-case (e.g. `URL`) the name is left as-is so the literal
    /// `getURL` is the primary candidate; the caller probes both.
    private static String capitalizeForBean(String name) {
        if (name == null || name.length() == 0) return name;
        if (name.length() >= 2
                && Character.isUpperCase(name.charAt(0))
                && Character.isUpperCase(name.charAt(1))) {
            // Already all-caps prefix -- leave alone (Beans Introspector
            // does the same for `URL` → `URL`).
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String simpleName(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String deriveXmlRoot(String simpleName) {
        if (simpleName.length() == 0) return simpleName;
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    // ---------------------------------------------------------------
    // Accumulator types
    // ---------------------------------------------------------------

    static final class MappedClass {
        String binaryName;
        String packageName;
        String simpleName;
        String mapperBinaryName;
        String mapperSimpleName;
        String xmlRootName;
        /// `true` for Java 17+ records. Drives accessor-style reads
        /// (`o.name()` vs `o.name`) and canonical-constructor writes
        /// (`new T(_a, _b, ...)` vs `o.a = ...; o.b = ...`).
        boolean isRecord;
        final List<MappedField> fields = new ArrayList<MappedField>();
    }

    static final class MappedField {
        String name;
        PropertyTypeKind kind;
        String jsonName;
        String xmlName;
        boolean xmlAttribute;
        boolean includeInJson;
        boolean includeInXml;
        /// `true` when this POJO field is private but a public
        /// `getX()`/`setX(...)` pair (or `isX()` for booleans) was found
        /// on the same class. Reads route through the getter, writes
        /// route through the setter. Records never set this flag --
        /// records always use the synthesised component accessor and
        /// the canonical constructor for writes.
        boolean useAccessor;
        /// `getFirstName` / `isActive` -- only populated when `useAccessor`
        /// is `true`.
        String getterName;
        /// `setFirstName` -- only populated when `useAccessor` is `true`
        /// AND the field is not a `Property` / `ListProperty` (those
        /// mutate through their own `.set(...)` / `.add(...)` API; the
        /// field reference itself is not replaced).
        String setterName;
    }
}
