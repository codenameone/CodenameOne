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
public final class MappingAnnotationProcessor extends AbstractAnnotationProcessor
        implements com.codename1.maven.annotations.ProcessesDependencyClasses {

    /// A `@Mapped` type in a library the application shares with its server is
    /// mapped here, in the application, where the mapper runtime is. Not in a
    /// module without that runtime: the generated mapper could not link there.
    @Override
    public boolean acceptsDependencyClasses(ProcessorContext ctx) {
        return com.codename1.maven.annotations.DependencyClasses.onClasspath(
                ctx.getCompileClasspath(), "com/codename1/mapping/Mappers.class");
    }


    public static final String MAPPED_DESC = "Lcom/codename1/annotations/Mapped;";
    public static final String JSON_PROPERTY_DESC = "Lcom/codename1/annotations/JsonProperty;";
    public static final String JSON_IGNORE_DESC = "Lcom/codename1/annotations/JsonIgnore;";
    public static final String XML_ROOT_DESC = "Lcom/codename1/annotations/XmlRoot;";
    public static final String XML_ELEMENT_DESC = "Lcom/codename1/annotations/XmlElement;";
    public static final String XML_ATTRIBUTE_DESC = "Lcom/codename1/annotations/XmlAttribute;";
    public static final String XML_TRANSIENT_DESC = "Lcom/codename1/annotations/XmlTransient;";
    public static final String JSON_CREATOR_DESC = "Lcom/codename1/annotations/JsonCreator;";
    public static final String JSON_STRICT_DESC = "Lcom/codename1/annotations/JsonStrict;";
    public static final String JSON_OMIT_NULL_DESC = "Lcom/codename1/annotations/JsonOmitNull;";
    public static final String JSON_FORMAT_DESC = "Lcom/codename1/annotations/JsonFormat;";
    public static final String JSON_ADAPTER_DESC = "Lcom/codename1/annotations/JsonAdapter;";

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
        if (ctx.isDependencyClass(cls)
                && com.codename1.maven.annotations.DependencyClasses.onClasspath(
                        ctx.getCompileClasspath(), ctx.getOutputClassDir(),
                        cls.getInternalName() + "Cn1Mapper.class")) {
            // Already mapped by the module that owns it, or by a module between
            // that one and this: a platform module sees the application's types
            // as dependencies too, and generating a second mapper there would
            // put two definitions of one class into the build.
            return;
        }
        boolean isRecord = cls.isRecord();
        if (!isRecord && (cls.isAbstract() || cls.isInterface())) {
            ctx.error(cls, "@Mapped requires a concrete class; "
                    + cls.getBinaryName() + " is abstract or an interface");
            return;
        }
        MethodInfo creator = null;
        for (MethodInfo m : cls.getMethods()) {
            if (m.getAnnotation(JSON_CREATOR_DESC) == null) continue;
            if (creator != null) {
                ctx.error(cls, "@Mapped class " + cls.getBinaryName()
                        + " declares more than one @JsonCreator (" + creator.getName()
                        + " and " + m.getName() + "); keep exactly one");
                return;
            }
            creator = m;
        }
        if (creator != null && isRecord) {
            ctx.error(cls, "@JsonCreator on record " + cls.getBinaryName()
                    + " is not supported; a record is read through its canonical constructor");
            return;
        }
        if (!isRecord && creator == null && !hasPublicNoArgConstructor(cls)) {
            ctx.error(cls, "@Mapped class " + cls.getBinaryName()
                    + " must declare a public no-arg constructor (or a @JsonCreator) for fromJson / fromXml");
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
        mc.strict = cls.getClassAnnotation(JSON_STRICT_DESC) != null;
        boolean classOmitNull = cls.getClassAnnotation(JSON_OMIT_NULL_DESC) != null;
        mc.typeParams = classTypeParams(cls.getSignature());
        String selfAdapter = adapterOf(cls.getClassAnnotation(JSON_ADAPTER_DESC));
        if (selfAdapter != null) {
            if (!validAdapter(cls, mc.binaryName, null, selfAdapter, ctx)) return;
            mc.classAdapter = selfAdapter;
        }

        AnnotationValues xmlRoot = cls.getClassAnnotation(XML_ROOT_DESC);
        if (xmlRoot != null) {
            String v = xmlRoot.getString("value");
            mc.xmlRootName = (v == null || v.length() == 0) ? deriveXmlRoot(mc.simpleName) : v;
        } else {
            mc.xmlRootName = deriveXmlRoot(mc.simpleName);
        }

        // Properties are the class's own fields plus those it inherits. A
        // @Mapped class commonly extends a plain (non-@Mapped, often abstract)
        // model base holding id/name/...; reading only the class's own fields
        // silently dropped every inherited property in both directions. The
        // chain is walked up to java.lang.Object (or the first class not on
        // the classpath), base fields first so the JSON order reads naturally,
        // and a field hidden by a same-named field lower down is skipped.
        // Accessors are looked up from the subclass upward, so an override wins.
        List<AnnotatedClass> chain = new ArrayList<AnnotatedClass>();
        chain.add(cls);
        if (!isRecord) {
            String sup = cls.getSuperInternalName();
            while (sup != null && !"java/lang/Object".equals(sup) && chain.size() < 64) {
                AnnotatedClass sc = ctx.lookupWithDependencies(sup);
                if (sc == null) break;
                chain.add(sc);
                sup = sc.getSuperInternalName();
            }
        }
        List<FieldInfo> allFields = new ArrayList<FieldInfo>();
        java.util.Set<String> seenNames = new java.util.HashSet<String>();
        for (AnnotatedClass c : chain) {
            List<FieldInfo> own = new ArrayList<FieldInfo>();
            for (FieldInfo f : c.getFields()) {
                if (f.isStatic() || !seenNames.add(f.getName())) continue;
                own.add(f);
            }
            allFields.addAll(0, own);
        }
        for (FieldInfo f : allFields) {
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
                String getter = findGetter(chain, f, probeKind);
                if (getter == null) {
                    ctx.getLog().debug("cn1: @Mapped skipping private field "
                            + f.getName() + " on " + cls.getBinaryName()
                            + " (no matching getter/setter)");
                    continue;
                }
                boolean needsSetter = !(probeKind.kind == PropertyTypeKind.Kind.PROPERTY
                        || probeKind.kind == PropertyTypeKind.Kind.LIST_PROPERTY);
                String setter = needsSetter ? findSetter(chain, f) : null;
                // A @JsonCreator class sets what it needs through the creator, so a
                // getter alone is enough: the field is written, and read only if the
                // creator takes it.
                if (needsSetter && setter == null && creator == null) {
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
            mf.helper = mf.name;
            mf.kind = PropertyTypeKind.of(f);
            // The base classifier never reflects, so an enum-typed field
            // arrives here as REFERENCE (and a List<Enum> as a LIST whose
            // element is unrecognised). Upgrade them using the compiled
            // class index so enums serialise as their name().
            if (mf.kind.kind == PropertyTypeKind.Kind.REFERENCE
                    && isEnumType(mf.kind.binaryName, ctx)) {
                mf.kind = PropertyTypeKind.enumType(mf.kind.binaryName);
            } else if (mf.kind.kind == PropertyTypeKind.Kind.LIST
                    && isEnumType(mf.kind.elementBinaryName, ctx)) {
                mf.elementIsEnum = true;
            }
            mf.useAccessor = useAccessor;
            mf.getterName = getterName;
            mf.setterName = setterName;
            mf.descriptor = f.getDescriptor();
            if (creator != null && ((useAccessor && setterName == null
                    && mf.kind.kind != PropertyTypeKind.Kind.PROPERTY
                    && mf.kind.kind != PropertyTypeKind.Kind.LIST_PROPERTY)
                    || (!useAccessor && f.isFinal()))) {
                mf.readOnly = true;
            }
            String desc = f.getDescriptor();
            if (desc.startsWith("[") && !"[B".equals(desc)) {
                if (desc.startsWith("[[")) {
                    ctx.error(cls, "@Mapped field " + mf.name + " on " + mc.binaryName
                            + " is a multi-dimensional array (" + desc
                            + "); only one-dimensional arrays map to JSON");
                    continue;
                }
                PropertyTypeKind ek = kindForDescriptor(desc.substring(1), ctx);
                if (ek.kind == PropertyTypeKind.Kind.UNSUPPORTED
                        || ek.kind == PropertyTypeKind.Kind.BYTE_ARRAY) {
                    ctx.error(cls, "@Mapped field " + mf.name + " on " + mc.binaryName
                            + " is an array of an unsupported element type (" + desc + ")");
                    continue;
                }
                mf.kind = ek;
                mf.isArray = true;
            }
            if (f.getSignature() != null) {
                mf.typeRef = parseTypeSignature(f.getSignature());
            }
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
                mf.aliases = stringList(jp.get("aliases"));
                mf.required = jp.getBoolOrDefault("required", false);
            }
            if (f.getAnnotation(JSON_IGNORE_DESC) != null) {
                mf.includeInJson = false;
            }
            mf.omitNull = classOmitNull || f.getAnnotation(JSON_OMIT_NULL_DESC) != null;
            if (mf.isArray && mf.includeInXml) {
                // The XML form has no representation of an array yet; the field is
                // JSON only rather than written as something unreadable.
                ctx.getLog().warn("cn1: @Mapped array field " + mf.name + " on "
                        + mc.binaryName + " is mapped to JSON only, not XML");
                mf.includeInXml = false;
            }
            if (!checkJsonOptions(cls, mc, mf, f, ctx)) continue;
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

        if (creator != null && !bindCreator(cls, mc, creator, ctx)) return;
        accepted.put(mc.binaryName, mc);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        for (MappedClass mc : accepted.values()) {
            mc.specs.clear();
        }
        for (MappedClass mc : accepted.values()) {
            assignSpecs(mc, mc, ctx, 0);
        }
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
    /// the form `__name` for later canonical-constructor invocation;
    /// JavaBeans POJOs route through `o.setName(rhsExpr)`; plain POJOs
    /// assign the field directly.
    /// The local a record component is gathered in before the constructor
    /// call. Two underscores: the generated code's own locals (`_kids`, `_i`,
    /// `_e`, ...) take one, so a component named `kids` cannot redeclare one.
    private static String recordLocal(MappedField f) {
        return "__" + f.name;
    }

    private static String writeStmt(MappedField f, boolean isRecord, String rhsExpr) {
        if (isRecord) return recordLocal(f) + " = " + rhsExpr + ";";
        // `_p_`, not `_`: a creator parameter called `v` would otherwise be the
        // `_v` every read already declares.
        if (f.viaLocal) return "_p_" + f.name + " = " + rhsExpr + ";";
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
        if (f.isArray) return descriptorToJava(f.descriptor);
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
            case ENUM:
                return f.kind.binaryName;
            case LIST:       return "java.util.List<" + f.kind.elementBinaryName + ">";
            default:
                return "Object";
        }
    }

    /// Generates the mapper source, then rewrites every nested type it names
    /// from its binary form (`pkg.Outer$Inner`) to the canonical form
    /// (`pkg.Outer.Inner`). The model keeps binary names because class-file
    /// lookups need them, and the emitters append them as-is in dozens of
    /// places (class literals, casts, generics, `valueOf`, `instanceof`); but
    /// javac does not resolve `Outer$Inner` in source, so a field whose type
    /// is a nested class or enum produced a mapper that did not compile.
    /// Only the exact names this mapper references are rewritten, and only as
    /// whole tokens, so the mapper's own `Outer$InnerCn1Mapper` name (a legal
    /// identifier) is left alone.
    private static String generateMapperSource(MappedClass mc) {
        String src = generateMapperSourceRaw(mc);
        java.util.Set<String> nested = new java.util.TreeSet<String>();
        collectNestedNames(mc, nested);
        for (String bin : nested) {
            src = src.replaceAll("(?<![A-Za-z0-9_$.])" + java.util.regex.Pattern.quote(bin)
                    + "(?![A-Za-z0-9_$])",
                    java.util.regex.Matcher.quoteReplacement(bin.replace('$', '.')));
        }
        return src;
    }

    private static void collectNestedNames(MappedClass mc, java.util.Set<String> out) {
        addNested(mc.binaryName, out);
        for (MappedField f : mc.fields) {
            if (f.kind != null) {
                addNested(f.kind.binaryName, out);
                addNested(f.kind.elementBinaryName, out);
            }
            addNested(f.adapter, out);
            addNested(f.adapterCast, out);
            addNested(f.elemAdapter, out);
        }
        for (MappedClass sp : mc.specs) {
            collectNestedNames(sp, out);
        }
    }

    private static void addNested(String bin, java.util.Set<String> out) {
        if (bin != null && bin.indexOf('$') > 0) {
            out.add(bin);
        }
    }

    private static String generateMapperSourceRaw(MappedClass mc) {
        StringBuilder sb = new StringBuilder(2048);
        if (mc.packageName.length() > 0) {
            sb.append("package ").append(mc.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(mc.mapperSimpleName)
                .append(" implements com.codename1.mapping.Mapper<").append(mc.binaryName).append(">");
        boolean direct = mc.classAdapter != null || canWriteDirectly(mc);
        if (direct) {
            sb.append(", com.codename1.mapping.Mapper.Direct<").append(mc.binaryName).append(">");
        }
        sb.append(" {\n\n");

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

        // One instance per adapter class, called directly: no reflection, and
        // a direct reference keeps the adapter alive through ParparVM and R8.
        java.util.TreeSet<String> adapters = new java.util.TreeSet<String>();
        collectAdapters(mc, adapters);
        for (MappedClass sp : mc.specs) collectAdapters(sp, adapters);
        for (String a : adapters) {
            sb.append("    private static final ").append(a).append(' ').append(adapterConst(a))
              .append(" = new ").append(a).append("();\n\n");
        }

        // toMap() -- reads are identical in shape regardless of record-ness;
        // `readExpr` picks between `o.name` (POJO) and `o.name()` (record).
        sb.append("    public java.util.Map<String, Object> toMap(").append(mc.binaryName).append(" o) {\n");
        if (mc.classAdapter != null) {
            sb.append("        if (o == null) return new java.util.LinkedHashMap<String, Object>();\n");
            sb.append("        Object _t = ").append(adapterConst(mc.classAdapter)).append(".toJson(o);\n");
            sb.append("        if (_t instanceof java.util.Map) return (java.util.Map<String, Object>) _t;\n");
            sb.append("        throw new IllegalStateException(\"@JsonAdapter ").append(escape(mc.classAdapter))
              .append(" writes ").append(escape(mc.binaryName))
              .append(" as a JSON value that is not an object; serialise it with Mappers.toJson\");\n");
        } else {
            emitToMapBody(sb, mc);
        }
        sb.append("    }\n\n");

        // toJson() -- the same properties, appended in order instead of going
        // through a LinkedHashMap that the writer then walks back. Emitted ONLY
        // for classes every field of which has a shape this can render exactly;
        // Mappers#toJson tests for the interface, so anything not emitted here
        // keeps the map path with no special casing.
        if (direct) {
            sb.append("    public void toJson(").append(mc.binaryName)
              .append(" o, StringBuilder out) {\n");
            sb.append("        if (o == null) { out.append(\"null\"); return; }\n");
            if (mc.classAdapter != null) {
                sb.append("        com.codename1.mapping.Mappers.appendJsonRaw(out, ")
                  .append(adapterConst(mc.classAdapter)).append(".toJson(o));\n");
            } else {
                // A property that may be left out (@JsonOmitNull) makes the comma a
                // run-time decision; without one, the separators stay literals and
                // the output is exactly what it always was.
                boolean runtimeComma = false;
                for (MappedField f : mc.fields) {
                    if (f.includeInJson && f.omitNull
                            && (isSpecial(f) || omitCond(f, isRecord) != null)) {
                        runtimeComma = true;
                    }
                }
                if (runtimeComma) sb.append("        boolean _first = true;\n");
                sb.append("        out.append('{');\n");
                boolean firstProp = true;
                for (MappedField f : mc.fields) {
                    if (!f.includeInJson) continue;
                    emitDirectProperty(sb, f, isRecord, firstProp, runtimeComma);
                    firstProp = false;
                }
                sb.append("        out.append('}');\n");
            }
            sb.append("    }\n\n");
        }

        // fromMap() -- POJO mutates an instance in-place; record accumulates
        // per-component locals and feeds them to the canonical constructor;
        // a @JsonCreator class collects its parameters the same way.
        sb.append("    public ").append(mc.binaryName)
                .append(" fromMap(java.util.Map<String, Object> m) {\n");
        if (mc.classAdapter != null) {
            sb.append("        if (m == null) return null;\n");
            sb.append("        try { return (").append(mc.binaryName).append(") ")
              .append(adapterConst(mc.classAdapter)).append(".fromJson(m); }\n");
            sb.append("        catch (IllegalArgumentException _x) { throw com.codename1.mapping.JsonValues.at(\"$\", _x); }\n");
        } else {
            emitReadBody(sb, mc, true);
        }
        sb.append("    }\n\n");

        // Specialisations of generic classes used with concrete type arguments.
        for (int i = 0; i < mc.specs.size(); i++) {
            MappedClass sp = mc.specs.get(i);
            sb.append("    private static java.util.Map<String, Object> _sToMap_").append(i)
              .append('(').append(sp.binaryName).append(" o) {\n");
            emitToMapBody(sb, sp);
            sb.append("    }\n\n");
            sb.append("    private static ").append(sp.binaryName).append(" _sFromMap_").append(i)
              .append("(java.util.Map<String, Object> m) {\n");
            emitReadBody(sb, sp, true);
            sb.append("    }\n\n");
            emitClassHelpers(sb, sp);
        }
        emitClassHelpers(sb, mc);

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
        emitReadBody(sb, mc, false);
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
            sb.append(recordLocal(f));
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

    /// Whether every JSON property of `mc` has a shape the direct writer can
    /// render EXACTLY as the map path would.
    ///
    /// Deliberately conservative: lists, nested mapped objects, byte arrays and
    /// Property wrappers all reach for the registry or another mapper at run
    /// time, and getting one of them subtly wrong produces valid-looking JSON
    /// with the wrong contents. They keep the map path until each is done and
    /// tested on its own.
    /// Whether every JSON property of `mc` has a shape [#emitFieldToJson] can
    /// render. Now every kind [#emitFieldToMap] handles, so the map path is used
    /// only for classes with a property neither of them renders.
    private static boolean canWriteDirectly(MappedClass mc) {
        for (MappedField f : mc.fields) {
            if (!f.includeInJson) continue;
            if (isSpecial(f)) continue;
            switch (f.kind.kind) {
                case STRING: case INT: case LONG: case SHORT: case BYTE: case CHAR:
                case DOUBLE: case FLOAT: case BOOLEAN: case ENUM: case DATE:
                case BYTE_ARRAY: case PROPERTY: case REFERENCE:
                case LIST: case LIST_PROPERTY:
                    break;
                default:
                    // emitFieldToMap ignores anything else, so there is nothing to
                    // render and no reason to claim the fast path.
                    return false;
            }
        }
        return true;
    }

    /// One property, appended directly. Mirrors [#emitFieldToMap] case for case
    /// and MUST keep mirroring it: the two produce the same JSON by construction
    /// rather than by test, so a case that drifts changes the document silently.
    /// The conversions that matter are a date to its millisecond value, an enum
    /// to `name()`, a byte array to Base64 and a char to a one-character STRING
    /// -- the map path applies those before the writer sees the value.
    private static void emitFieldToJson(StringBuilder sb, MappedField f, boolean isRecord) {
        String read = readExpr(f, isRecord);
        switch (f.kind.kind) {
            case STRING:
                sb.append("        com.codename1.mapping.Mappers.appendJsonString(out, ")
                  .append(read).append(");\n");
                return;
            case INT: case LONG: case SHORT: case BYTE: case DOUBLE: case FLOAT:
                sb.append("        out.append(").append(read).append(");\n");
                return;
            case BOOLEAN:
                // Boolean and boolean share this kind, and only the BOXED one can be
                // null. Unboxing it in a ternary threw NullPointerException where the
                // map path -- which just puts the value in and lets JSONWriter see a
                // null -- emits JSON null. Told apart by binaryName; the temporary
                // keeps a getter from being evaluated twice.
                if ("java.lang.Boolean".equals(f.kind.binaryName)) {
                    sb.append("        {\n");
                    sb.append("            Boolean _b = ").append(read).append(";\n");
                    sb.append("            out.append(_b == null ? \"null\" : (_b.booleanValue() ? \"true\" : \"false\"));\n");
                    sb.append("        }\n");
                } else {
                    sb.append("        out.append(").append(read).append(" ? \"true\" : \"false\");\n");
                }
                return;
            case CHAR:
                // Same split. A null Character went through String.valueOf(Object),
                // which returns the four characters "null", and then got QUOTED --
                // so an unset field serialised as the string "null" instead of JSON
                // null. charValue() below also picks String.valueOf(char) rather than
                // the Object overload.
                if ("java.lang.Character".equals(f.kind.binaryName)) {
                    sb.append("        {\n");
                    sb.append("            Character _c = ").append(read).append(";\n");
                    sb.append("            if (_c == null) { out.append(\"null\"); }\n");
                    sb.append("            else { com.codename1.mapping.Mappers.appendJsonString(out, String.valueOf(_c.charValue())); }\n");
                    sb.append("        }\n");
                } else {
                    sb.append("        com.codename1.mapping.Mappers.appendJsonString(out, String.valueOf(")
                      .append(read).append("));\n");
                }
                return;
            case ENUM:
                sb.append("        com.codename1.mapping.Mappers.appendJsonString(out, ")
                  .append(read).append(" == null ? null : ").append(read).append(".name());\n");
                return;
            case DATE:
                sb.append("        if (").append(read).append(" == null) { out.append(\"null\"); }")
                  .append(" else { out.append(").append(read).append(".getTime()); }\n");
                return;
            case BYTE_ARRAY:
                sb.append("        com.codename1.mapping.Mappers.appendJsonString(out, ")
                  .append(read).append(" == null ? null : com.codename1.util.Base64.encode(")
                  .append(read).append("));\n");
                return;
            case PROPERTY:
                // appendJsonRaw, not appendJsonValue. emitFieldToMap puts the value
                // in the map UNCHANGED, so the old writer renders a Date or a mapped
                // object through JSONWriter's String.valueOf fallback. appendJsonValue
                // would render epoch millis and nested JSON instead -- better, and
                // therefore a silent wire change for every mapper the day it gains a
                // direct writer.
                sb.append("        com.codename1.mapping.Mappers.appendJsonRaw(out, ")
                  .append(read).append(".get());\n");
                return;
            case REFERENCE:
                // Looked up by the DECLARED type, exactly as emitFieldToMap does.
                // appendJson would look up by the instance's RUNTIME class, so a
                // field declared as a mapped base holding an unmapped subclass found
                // no mapper and fell back to a quoted toString, where the map path
                // serialises it as an object. appendJsonUsing still takes the nested
                // mapper's direct route when it has one, so nesting builds no map.
                sb.append("        com.codename1.mapping.Mappers.appendJsonUsing(")
                  .append("com.codename1.mapping.Mappers.get(").append(f.kind.binaryName)
                  .append(".class), ").append(read).append(", out);\n");
                return;
            case LIST: case LIST_PROPERTY: {
                String src = f.kind.kind == PropertyTypeKind.Kind.LIST
                        ? read : read + ".asList()";
                sb.append("        {\n");
                sb.append("            java.util.List _src = ").append(src).append(";\n");
                // EMPTY ARRAY, not null. emitFieldToMap unconditionally builds an
                // ArrayList and fills it only when the source is non-null, so the map
                // path serialises a null list as []. The direct path has to agree:
                // Mapper.Direct's contract is to produce exactly what
                // JSONWriter.toJson(toMap(instance)) would, and a mapper silently
                // changing a field's wire representation the day it gains a direct
                // writer is the one thing that contract exists to prevent.
                sb.append("            if (_src == null) { out.append(\"[]\"); }\n");
                sb.append("            else {\n");
                sb.append("                out.append('[');\n");
                sb.append("                boolean _first = true;\n");
                sb.append("                for (java.util.Iterator _it = _src.iterator(); _it.hasNext(); ) {\n");
                sb.append("                    if (!_first) { out.append(','); }\n");
                sb.append("                    _first = false;\n");
                // One branch per branch emitFieldToMap has for an element, in the
                // same order and with the same answer. Anything less specific
                // diverges: the enum and the mapped-object cases both did.
                sb.append("                    Object _e = _it.next();\n");
                if (f.elementIsEnum) {
                    // name(), not toString(). Deserialisation matches against the
                    // declared constants, so an enum overriding toString() would
                    // serialise to something that cannot be read back.
                    sb.append("                    com.codename1.mapping.Mappers.appendJsonRaw(out, _e == null ? null : ((")
                      .append(f.kind.elementBinaryName).append(") _e).name());\n");
                } else if (isScalarBinary(f.kind.elementBinaryName)) {
                    sb.append("                    com.codename1.mapping.Mappers.appendJsonRaw(out, _e);\n");
                } else if ("java.util.Date".equals(f.kind.elementBinaryName)) {
                    sb.append("                    com.codename1.mapping.Mappers.appendJsonRaw(out, _e == null ? null : Long.valueOf(((java.util.Date) _e).getTime()));\n");
                } else {
                    // By the DECLARED element type, as the map path does -- a
                    // List<Base> holding an unmapped subclass otherwise found no
                    // mapper by runtime class and fell back to a quoted toString.
                    //
                    // The NO-MAPPER case differs between the two paths and must be
                    // split here. For a reference field emitFieldToMap stores
                    // _v.toString(), so appendJsonUsing quotes it; for a list element
                    // it stores _e UNCHANGED, so the writer keeps its JSON type and a
                    // List<Object> holding 5 must stay [5] rather than becoming ["5"].
                    // Routing both through appendJsonUsing regressed the list case.
                    sb.append("                    {\n");
                    sb.append("                        com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(")
                      .append(f.kind.elementBinaryName).append(".class);\n");
                    sb.append("                        if (_nm == null) { com.codename1.mapping.Mappers.appendJsonRaw(out, _e); }\n");
                    sb.append("                        else { com.codename1.mapping.Mappers.appendJsonUsing(_nm, _e, out); }\n");
                    sb.append("                    }\n");
                }
                sb.append("                }\n");
                sb.append("                out.append(']');\n");
                sb.append("            }\n");
                sb.append("        }\n");
                return;
            }
            default:
                throw new IllegalStateException(
                        "no direct JSON writer for " + f.kind.kind + " on " + f.jsonName);
        }
    }

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
            case ENUM:
                sb.append("        m.put(").append(key).append(", ").append(read)
                        .append(" == null ? null : ").append(read).append(".name());\n");
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
                if (f.elementIsEnum) {
                    sb.append("                    _l.add(_e == null ? null : ((")
                            .append(f.kind.elementBinaryName).append(") _e).name());\n");
                } else if (isScalarBinary(f.kind.elementBinaryName)) {
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
        sb.append("        {\n");
        sb.append("            Object _v = ").append(lookupExpr(f)).append(";\n");
        if (f.required) {
            // Present under any of its names, even as an explicit null, is
            // enough: only an absent member is refused.
            sb.append("            if (!(").append(presentExpr(f))
              .append(")) throw new IllegalArgumentException(\"$.").append(escape(f.jsonName))
              .append(": required member is missing\");\n");
        }
        sb.append("            if (_v != null) {\n");
        if (isSpecial(f)) {
            sb.append("                try { ").append(writeStmt(f, isRecord, "_r_" + f.helper + "(_v)")).append(" }\n");
            sb.append("                catch (IllegalArgumentException _x) { throw com.codename1.mapping.JsonValues.at(")
              .append(pathLiteral(f)).append(", _x); }\n");
            sb.append("            }\n");
            emitExplicitNull(sb, f, isRecord);
            sb.append("        }\n");
            return;
        }
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
                // Either shape: epoch millis, or an ISO-8601 string.
                sb.append("                try { ").append(writeStmt(f, isRecord, "com.codename1.mapping.JsonValues.toDate(_v)")).append(" }\n");
                sb.append("                catch (IllegalArgumentException _x) { throw com.codename1.mapping.JsonValues.at(")
                  .append(pathLiteral(f)).append(", _x); }\n");
                break;
            case BYTE_ARRAY:
                sb.append("                ").append(writeStmt(f, isRecord, "com.codename1.util.Base64.decode(_v.toString().getBytes())")).append("\n");
                break;
            case PROPERTY:
                emitPropertySetFromJsonValue(sb, f, isRecord);
                break;
            case ENUM: {
                String bin = f.kind.binaryName;
                sb.append("                if (_v instanceof ").append(bin).append(") {\n");
                sb.append("                    ").append(writeStmt(f, isRecord, "(" + bin + ") _v")).append("\n");
                sb.append("                } else {\n");
                sb.append("                    try { ").append(writeStmt(f, isRecord, bin + ".valueOf(_v.toString())")).append(" }\n");
                sb.append("                    catch (IllegalArgumentException _ex) { ").append(writeStmt(f, isRecord, "null")).append(" }\n");
                sb.append("                }\n");
                break;
            }
            case REFERENCE:
                sb.append("                com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(").append(f.kind.binaryName).append(".class);\n");
                sb.append("                if (_nm != null && _v instanceof java.util.Map) {\n");
                sb.append("                    try { ").append(writeStmt(f, isRecord, "(" + f.kind.binaryName + ") _nm.fromMap((java.util.Map) _v)")).append(" }\n");
                sb.append("                    catch (IllegalArgumentException _x) { throw com.codename1.mapping.JsonValues.at(")
                  .append(pathLiteral(f)).append(", _x); }\n");
                sb.append("                }\n");
                break;
            case LIST: case LIST_PROPERTY:
                // LIST_PROPERTY mutates o.field.clear()/add(...) -- safe for
                // POJO path only (records reject LIST_PROPERTY upstream). For
                // a record's LIST we materialize a local _l and assign it to
                // the per-component local that later feeds the canonical ctor.
                sb.append("                if (_v instanceof java.util.List) {\n");
                if (f.elementIsEnum) {
                    String elem = f.kind.elementBinaryName;
                    if (f.kind.kind == PropertyTypeKind.Kind.LIST) {
                        sb.append("                    java.util.ArrayList<").append(elem).append("> _l = new java.util.ArrayList<").append(elem).append(">();\n");
                        emitEnumListLoop(sb, elem, "_l");
                        sb.append("                    ").append(writeStmt(f, isRecord, "_l")).append("\n");
                    } else {
                        sb.append("                    ").append(readExpr(f, isRecord)).append(".clear();\n");
                        emitEnumListLoop(sb, elem, readExpr(f, isRecord));
                    }
                } else if (isScalarBinary(f.kind.elementBinaryName)) {
                    if (f.kind.kind == PropertyTypeKind.Kind.LIST) {
                        sb.append("                    java.util.ArrayList<").append(f.kind.elementBinaryName).append("> _l = new java.util.ArrayList<").append(f.kind.elementBinaryName).append(">();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) { _l.add(").append(scalarElement(f.kind.elementBinaryName, "_e")).append("); }\n");
                        sb.append("                    ").append(writeStmt(f, isRecord, "_l")).append("\n");
                    } else {
                        sb.append("                    ").append(readExpr(f, isRecord)).append(".clear();\n");
                        sb.append("                    for (Object _e : (java.util.List) _v) { ").append(readExpr(f, isRecord)).append(".add(").append(scalarElement(f.kind.elementBinaryName, "_e")).append("); }\n");
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
                        sb.append("                    java.util.List _src = (java.util.List) _v;\n");
                        sb.append("                    for (int _i = 0; _i < _src.size(); _i++) {\n");
                        sb.append("                        Object _e = _src.get(_i);\n");
                        sb.append("                        if (_nm != null && _e instanceof java.util.Map) {\n");
                        sb.append("                            try { _l.add((").append(f.kind.elementBinaryName).append(") _nm.fromMap((java.util.Map) _e)); }\n");
                        sb.append("                            catch (IllegalArgumentException _x) { throw com.codename1.mapping.JsonValues.at(\"$.")
                          .append(escape(f.jsonName)).append("[\" + _i + \"]\", _x); }\n");
                        sb.append("                        }\n");
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
        emitExplicitNull(sb, f, isRecord);
        sb.append("        }\n");
    }

    /// A member present as an explicit null, matching the backend codecs: a
    /// reference field becomes null (an absent member keeps what the
    /// constructor set), a primitive keeps its value unless it is required,
    /// in which case the body is refused.
    private static void emitExplicitNull(StringBuilder sb, MappedField f, boolean isRecord) {
        if (!f.nullClears) return;
        String body;
        if (isPrimitiveDesc(f.descriptor) && !f.isArray) {
            if (!f.required) return;
            body = "throw new IllegalArgumentException(\"$." + escape(f.jsonName)
                    + ": expected a value, got null\");";
        } else if (f.adapter != null) {
            // The adapter is handed the null, as the server's codec does.
            body = "try { " + writeStmt(f, isRecord, "_r_" + f.helper + "(null)")
                    + " } catch (IllegalArgumentException _x) { throw com.codename1.mapping.JsonValues.at("
                    + pathLiteral(f) + ", _x); }";
        } else if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY && !isSpecial(f)) {
            body = readExpr(f, isRecord) + ".set(null);";
        } else if (f.kind.kind == PropertyTypeKind.Kind.LIST_PROPERTY && !isSpecial(f)) {
            body = readExpr(f, isRecord) + ".clear();";
        } else {
            body = writeStmt(f, isRecord, "null");
        }
        sb.append("            else if (").append(presentExpr(f)).append(") { ").append(body).append(" }\n");
    }

    /// Emits a loop that converts a `List` of JSON values into enum
    /// constants (`name()` strings, or pass-through enum instances),
    /// adding each to `sink`. Unknown names decode to null.
    private static void emitEnumListLoop(StringBuilder sb, String elem, String sink) {
        sb.append("                    for (Object _e : (java.util.List) _v) {\n");
        sb.append("                        ").append(elem).append(" _ev = null;\n");
        sb.append("                        if (_e instanceof ").append(elem).append(") { _ev = (").append(elem).append(") _e; }\n");
        sb.append("                        else if (_e != null) { try { _ev = ").append(elem).append(".valueOf(_e.toString()); } catch (IllegalArgumentException _ex) { _ev = null; } }\n");
        sb.append("                        ").append(sink).append(".add(_ev);\n");
        sb.append("                    }\n");
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
            case ENUM:
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
                if (f.elementIsEnum) {
                    sb.append("                    if (_e != null) {\n");
                    sb.append("                        _el.addChild(new com.codename1.xml.Element(((")
                            .append(f.kind.elementBinaryName).append(") _e).name(), true));\n");
                    sb.append("                    }\n");
                } else if (isScalarBinary(f.kind.elementBinaryName) || "java.util.Date".equals(f.kind.elementBinaryName)) {
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
        if (f.elementIsEnum) {
            sb.append("                String _s = textOf(").append(elemVar).append(");\n");
            sb.append("                if (_s != null) { try { ").append(sink).append(".add(")
                    .append(elem).append(".valueOf(_s)); } catch (IllegalArgumentException _ex) { ")
                    .append(sink).append(".add(null); } }\n");
        } else if (isScalarBinary(elem)) {
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
            case ENUM:
                sb.append(read).append(" == null ? null : ").append(read).append(".name()"); break;
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
            case ENUM: {
                String bin = f.kind.binaryName;
                sb.append("                try { ").append(writeStmt(f, isRecord, bin + ".valueOf(" + src + ")")).append(" }\n");
                sb.append("                catch (IllegalArgumentException _ex) { ").append(writeStmt(f, isRecord, "null")).append(" }\n");
                break;
            }
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

    /// The expression that turns `var`, one element of a parsed JSON array, into
    /// the boxed scalar `binary` names.
    ///
    /// Never a bare cast. A JSON parser hands every number over as a `Double`
    /// (or a `Long`), whatever the field is declared as, so casting an element
    /// of a `List<Integer>` throws ClassCastException on a JVM -- and on
    /// ParparVM, whose casts are unchecked, puts a `Double` in the list for the
    /// first `intValue()` to read as something it is not. An element of the
    /// wrong kind altogether becomes null.
    static String scalarElement(String binary, String var) {
        String number = "((Number) " + var + ")";
        String wrapped;
        if ("java.lang.Integer".equals(binary)) {
            wrapped = "Integer.valueOf(" + number + ".intValue())";
        } else if ("java.lang.Long".equals(binary)) {
            wrapped = "Long.valueOf(" + number + ".longValue())";
        } else if ("java.lang.Short".equals(binary)) {
            wrapped = "Short.valueOf(" + number + ".shortValue())";
        } else if ("java.lang.Byte".equals(binary)) {
            wrapped = "Byte.valueOf(" + number + ".byteValue())";
        } else if ("java.lang.Double".equals(binary)) {
            wrapped = "Double.valueOf(" + number + ".doubleValue())";
        } else if ("java.lang.Float".equals(binary)) {
            wrapped = "Float.valueOf(" + number + ".floatValue())";
        } else if ("java.lang.Boolean".equals(binary)) {
            return "(" + var + " instanceof Boolean ? (Boolean) " + var + " : null)";
        } else if ("java.lang.Character".equals(binary)) {
            return "(" + var + " instanceof Character ? (Character) " + var
                    + " : " + var + " instanceof String && ((String) " + var + ").length() > 0"
                    + " ? Character.valueOf(((String) " + var + ").charAt(0)) : null)";
        } else {
            return "(" + var + " instanceof String ? (String) " + var + " : null)";
        }
        return "(" + var + " instanceof Number ? " + wrapped + " : null)";
    }

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

    /// True when `binaryName` names a Java enum present in the compiled
    /// class index. Returns false for enums outside the annotation scan
    /// (e.g. library enums) -- those stay REFERENCE and behave as before.
    private static boolean isEnumType(String binaryName, ProcessorContext ctx) {
        if (binaryName == null || binaryName.length() == 0) return false;
        com.codename1.maven.annotations.AnnotatedClass c =
                ctx.lookupWithDependencies(binaryName.replace('.', '/'));
        return c != null && c.isEnum();
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
    /// [#findGetter] over a class and its superclasses, nearest first.
    private static String findGetter(List<AnnotatedClass> chain, FieldInfo f, PropertyTypeKind kind) {
        for (AnnotatedClass c : chain) {
            String g = findGetter(c, f, kind);
            if (g != null) return g;
        }
        return null;
    }

    /// [#findSetter] over a class and its superclasses, nearest first.
    private static String findSetter(List<AnnotatedClass> chain, FieldInfo f) {
        for (AnnotatedClass c : chain) {
            String s = findSetter(c, f);
            if (s != null) return s;
        }
        return null;
    }

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

    /**
     * JSON-escapes a property name, matching JSONWriter.writeString character for
     * character (minus the surrounding quotes, which the caller emits).
     *
     * Applied at GENERATION time because a jsonName is a compile-time constant --
     * the direct writer stays a plain literal append with no per-call escaping. It
     * must be composed with {@link #escape} afterwards, which is the Java-literal
     * escaper: one makes the JSON valid, the other makes the source compile.
     */
    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        b.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            b.append('0');
                        }
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.toString();
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
    // JSON options: generation
    // ---------------------------------------------------------------

    /// A field whose JSON form is produced and read by private helpers
    /// (`_j_<field>` / `_r_<field>`) rather than inline: an adapter, a format,
    /// an array, an adapted list element or a resolved generic class. Both
    /// writers call the same `_j_` helper and render its tree, so the direct
    /// and the map path agree by construction.
    static boolean isSpecial(MappedField f) {
        return f.isArray || f.adapter != null || f.elemAdapter != null
                || f.format != null || f.specIndex >= 0;
    }

    private static void collectAdapters(MappedClass mc, java.util.Set<String> out) {
        if (mc.classAdapter != null) out.add(mc.classAdapter);
        for (MappedField f : mc.fields) {
            if (!f.includeInJson) continue;
            if (f.adapter != null) out.add(f.adapter);
            if (f.elemAdapter != null) out.add(f.elemAdapter);
        }
    }

    private static String adapterConst(String adapter) {
        return "_A_" + adapter.replace('.', '_').replace('$', '_');
    }

    /// The null test a `@JsonOmitNull` property is written under, or null when
    /// its value can never be null (a primitive) or omission does not apply.
    private static String omitCond(MappedField f, boolean isRecord) {
        if (!f.omitNull) return null;
        String read = readExpr(f, isRecord);
        switch (f.kind.kind) {
            case PROPERTY:
                return read + ".get() != null";
            case LIST_PROPERTY:
                return null;
            default:
                if (f.descriptor != null && f.descriptor.length() == 1) return null;
                return read + " != null";
        }
    }

    private static void emitToMapBody(StringBuilder sb, MappedClass mc) {
        sb.append("        java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<String, Object>();\n");
        sb.append("        if (o == null) return m;\n");
        for (MappedField f : mc.fields) {
            if (!f.includeInJson) continue;
            if (isSpecial(f)) {
                sb.append("        { Object _t = _j_").append(f.helper).append("(o); ")
                  .append(f.omitNull ? "if (_t != null) " : "")
                  .append("m.put(\"").append(escape(f.jsonName)).append("\", _t); }\n");
                continue;
            }
            String cond = omitCond(f, mc.isRecord);
            if (cond != null) sb.append("        if (").append(cond).append(") {\n");
            emitFieldToMap(sb, f, mc.isRecord);
            if (cond != null) sb.append("        }\n");
        }
        sb.append("        return m;\n");
    }

    private static void emitDirectProperty(StringBuilder sb, MappedField f, boolean isRecord,
                                           boolean firstProp, boolean runtimeComma) {
        String keyLit = escape(jsonEscape(f.jsonName));
        if (isSpecial(f)) {
            sb.append("        {\n");
            sb.append("            Object _t = _j_").append(f.helper).append("(o);\n");
            if (f.omitNull) sb.append("            if (_t != null) {\n");
            emitKey(sb, keyLit, firstProp, runtimeComma, "            ");
            sb.append("            com.codename1.mapping.Mappers.appendJsonRaw(out, _t);\n");
            if (f.omitNull) sb.append("            }\n");
            sb.append("        }\n");
            return;
        }
        String cond = omitCond(f, isRecord);
        if (cond != null) sb.append("        if (").append(cond).append(") {\n");
        emitKey(sb, keyLit, firstProp, runtimeComma, "        ");
        emitFieldToJson(sb, f, isRecord);
        if (cond != null) sb.append("        }\n");
    }

    private static void emitKey(StringBuilder sb, String keyLit, boolean firstProp,
                                boolean runtimeComma, String ind) {
        if (runtimeComma) {
            sb.append(ind).append("out.append(_first ? \"\\\"").append(keyLit)
              .append("\\\":\" : \",\\\"").append(keyLit).append("\\\":\");\n");
            sb.append(ind).append("_first = false;\n");
            return;
        }
        // jsonEscape THEN escape: the inner one makes the key valid JSON, the
        // outer one makes it a valid Java literal. escape() alone only did the
        // second, so a @JsonProperty containing a quote compiled fine and then
        // emitted "a"b" -- unparseable, where the map path escapes it properly
        // because JSONWriter writes the key through writeString.
        sb.append(ind).append("out.append(\"").append(firstProp ? "" : ",")
          .append("\\\"").append(keyLit).append("\\\":\");\n");
    }

    /// The body of `fromMap` (`json`) or `readXml`: a POJO is filled in place,
    /// a record and a `@JsonCreator` class collect locals and construct once.
    private static void emitReadBody(StringBuilder sb, MappedClass mc, boolean json) {
        String src = json ? "m" : "root";
        // Before these options a JSON null was dropped by the parser, so a
        // field kept its value. Telling null from absent is opt-in, so a
        // class that uses none of the new options reads exactly as before.
        for (MappedField f : mc.fields) {
            f.nullClears = mc.strict || f.required || f.adapter != null;
        }
        if (mc.isRecord) {
            for (MappedField f : mc.fields) {
                sb.append("        ").append(fieldType(f)).append(" ")
                        .append(recordLocal(f)).append(" = ")
                        .append(f.isArray ? "null" : defaultLiteral(f.kind)).append(";\n");
            }
            sb.append("        if (").append(src).append(" == null) return ");
            appendRecordCtorCall(sb, mc);
            sb.append(";\n");
            if (json) emitStrictCheck(sb, mc);
            for (MappedField f : mc.fields) {
                emitRead(sb, f, true, json);
            }
            sb.append("        return ");
            appendRecordCtorCall(sb, mc);
            sb.append(";\n");
            return;
        }
        if (mc.creatorCall != null) {
            for (String name : mc.creatorParams) {
                MappedField f = mc.field(name);
                sb.append("        ").append(descriptorToJava(f.descriptor)).append(" _p_").append(f.name)
                  .append(" = ").append(defaultForDescriptor(f.descriptor)).append(";\n");
            }
            sb.append("        if (").append(src).append(" == null) return null;\n");
            if (json) emitStrictCheck(sb, mc);
            for (String name : mc.creatorParams) {
                emitRead(sb, mc.field(name), false, json);
            }
            sb.append("        ").append(mc.binaryName).append(" o = ").append(mc.creatorCall).append('(');
            for (int i = 0; i < mc.creatorParams.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("_p_").append(mc.creatorParams.get(i));
            }
            sb.append(");\n");
            for (MappedField f : mc.fields) {
                if (f.viaLocal || f.readOnly) continue;
                emitRead(sb, f, false, json);
            }
            sb.append("        return o;\n");
            return;
        }
        sb.append("        ").append(mc.binaryName).append(" o = new ").append(mc.binaryName).append("();\n");
        sb.append("        if (").append(src).append(" == null) return o;\n");
        if (json) emitStrictCheck(sb, mc);
        for (MappedField f : mc.fields) {
            emitRead(sb, f, false, json);
        }
        sb.append("        return o;\n");
    }

    private static void emitRead(StringBuilder sb, MappedField f, boolean isRecord, boolean json) {
        if (json) {
            if (f.includeInJson) emitFieldFromMap(sb, f, isRecord);
        } else {
            if (f.includeInXml) emitFieldFromXml(sb, f, isRecord);
        }
    }

    private static void emitStrictCheck(StringBuilder sb, MappedClass mc) {
        if (!mc.strict) return;
        sb.append("        for (java.util.Iterator _ki = m.keySet().iterator(); _ki.hasNext(); ) {\n");
        sb.append("            Object _k = _ki.next();\n");
        sb.append("            if (!_known").append(mc.prefix)
          .append("(_k)) throw new IllegalArgumentException(\"$.\" + _k + \": unknown member\");\n");
        sb.append("        }\n");
    }

    /// The lookup of a member under its primary name, else the first alias
    /// present.
    private static String lookupExpr(MappedField f) {
        String key = "\"" + escape(f.jsonName) + "\"";
        if (f.aliases.isEmpty()) return "m.get(" + key + ")";
        StringBuilder b = new StringBuilder();
        b.append("m.containsKey(").append(key).append(") ? m.get(").append(key).append(") : ");
        for (String a : f.aliases) {
            String ak = "\"" + escape(a) + "\"";
            b.append("m.containsKey(").append(ak).append(") ? m.get(").append(ak).append(") : ");
        }
        b.append("null");
        return b.toString();
    }

    private static String presentExpr(MappedField f) {
        StringBuilder b = new StringBuilder();
        b.append("m.containsKey(\"").append(escape(f.jsonName)).append("\")");
        for (String a : f.aliases) {
            b.append(" || m.containsKey(\"").append(escape(a)).append("\")");
        }
        return b.toString();
    }

    /// The path of the member read, under the name the body actually used:
    /// a value taken from an alias is reported under that alias.
    private static String pathLiteral(MappedField f) {
        String primary = "\"$." + escape(f.jsonName) + "\"";
        if (f.aliases.isEmpty()) return primary;
        StringBuilder b = new StringBuilder("(m.containsKey(\"").append(escape(f.jsonName))
                .append("\") ? ").append(primary);
        for (String a : f.aliases) {
            b.append(" : m.containsKey(\"").append(escape(a)).append("\") ? \"$.")
             .append(escape(a)).append('"');
        }
        return b.append(" : ").append(primary).append(')').toString();
    }

    private static void emitClassHelpers(StringBuilder sb, MappedClass mc) {
        if (mc.strict) {
            sb.append("    private static boolean _known").append(mc.prefix).append("(Object k) {\n");
            sb.append("        return false");
            for (MappedField f : mc.fields) {
                if (!f.includeInJson) continue;
                sb.append("\n            || \"").append(escape(f.jsonName)).append("\".equals(k)");
                for (String a : f.aliases) {
                    sb.append("\n            || \"").append(escape(a)).append("\".equals(k)");
                }
            }
            sb.append(";\n    }\n\n");
        }
        for (MappedField f : mc.fields) {
            if (!f.includeInJson || !isSpecial(f)) continue;
            emitWriteHelper(sb, mc, f);
            emitReadHelper(sb, f);
        }
    }

    private static boolean isPrimitiveDesc(String desc) {
        return desc != null && desc.length() == 1;
    }

    private static boolean isNumberKind(PropertyTypeKind k) {
        switch (k.kind) {
            case INT: case LONG: case SHORT: case BYTE: case DOUBLE: case FLOAT:
                return true;
            default:
                return false;
        }
    }

    private static void emitWriteHelper(StringBuilder sb, MappedClass mc, MappedField f) {
        String jt = descriptorToJava(f.descriptor);
        sb.append("    private static Object _j_").append(f.helper).append('(')
          .append(mc.binaryName).append(" o) {\n");
        sb.append("        ").append(jt).append(" _x = ").append(readExpr(f, mc.isRecord)).append(";\n");
        boolean listLike = !f.isArray && f.adapter == null && f.elemAdapter != null;
        // An adapter decides what null is, as on the server: it is called.
        if (!isPrimitiveDesc(f.descriptor) && f.adapter == null) {
            // A null list is [] on every other path; keep that unless the
            // property is to be left out instead.
            sb.append("        if (_x == null) return ")
              .append(listLike && !f.omitNull ? "new java.util.ArrayList<Object>()" : "null").append(";\n");
        }
        if (f.adapter != null) {
            sb.append("        return ").append(adapterConst(f.adapter)).append(".toJson((")
              .append(f.adapterCast).append(") _x);\n");
        } else if (f.specIndex >= 0) {
            sb.append("        return _sToMap_").append(f.specIndex).append("((")
              .append(f.kind.binaryName).append(") _x);\n");
        } else if (f.format != null && f.kind.kind == PropertyTypeKind.Kind.DATE) {
            if ("STRING".equals(f.format)) {
                sb.append("        return com.codename1.mapping.JsonValues.formatIso((java.util.Date) _x);\n");
            } else {
                sb.append("        return Long.valueOf(((java.util.Date) _x).getTime());\n");
            }
        } else if (f.format != null) {
            sb.append("        return String.valueOf(_x);\n");
        } else if (listLike) {
            String e = f.kind.elementBinaryName;
            sb.append("        java.util.ArrayList<Object> _l = new java.util.ArrayList<Object>();\n");
            sb.append("        for (java.util.Iterator _it = ((java.util.List) _x).iterator(); _it.hasNext(); ) {\n");
            sb.append("            Object _e = _it.next();\n");
            sb.append("            _l.add(").append(adapterConst(f.elemAdapter))
              .append(".toJson((").append(e).append(") _e));\n");
            sb.append("        }\n");
            sb.append("        return _l;\n");
        } else {
            String ej = f.kind.binaryName;
            boolean prim = isPrimitiveName(ej);
            boolean mapped = f.elemAdapter == null && f.kind.kind == PropertyTypeKind.Kind.REFERENCE;
            sb.append("        java.util.ArrayList<Object> _l = new java.util.ArrayList<Object>(_x.length);\n");
            if (mapped) {
                sb.append("        com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(")
                  .append(ej).append(".class);\n");
            }
            sb.append("        for (int _i = 0; _i < _x.length; _i++) {\n");
            sb.append("            ").append(ej).append(" _ex = ").append(prim ? "" : "(" + ej + ") ")
              .append("_x[_i];\n");
            sb.append("            _l.add(").append(elementToTree(f, ej, prim)).append(");\n");
            sb.append("        }\n");
            sb.append("        return _l;\n");
        }
        sb.append("    }\n\n");
    }

    private static String elementToTree(MappedField f, String ej, boolean prim) {
        if (f.elemAdapter != null) {
            return adapterConst(f.elemAdapter) + ".toJson(_ex)";
        }
        if (prim) {
            if ("char".equals(ej)) return "String.valueOf(_ex)";
            return boxOf(ej) + ".valueOf(_ex)";
        }
        switch (f.kind.kind) {
            case ENUM:
                return "_ex == null ? null : _ex.name()";
            case DATE:
                return "_ex == null ? null : Long.valueOf(_ex.getTime())";
            case REFERENCE:
                return "_ex == null ? null : (_nm == null ? (Object) _ex : _nm.toMap(_ex))";
            default:
                return "_ex";
        }
    }

    private static void emitReadHelper(StringBuilder sb, MappedField f) {
        String jt = descriptorToJava(f.descriptor);
        sb.append("    private static ").append(jt).append(" _r_").append(f.helper).append("(Object _v) {\n");
        if (f.adapter != null) {
            sb.append("        return (").append(jt).append(") ").append(adapterConst(f.adapter))
              .append(".fromJson(_v);\n");
        } else if (f.specIndex >= 0) {
            sb.append("        if (!(_v instanceof java.util.Map)) throw new IllegalArgumentException(\"expected a JSON object\");\n");
            sb.append("        return _sFromMap_").append(f.specIndex)
              .append("((java.util.Map<String, Object>) _v);\n");
        } else if (f.format != null && f.kind.kind == PropertyTypeKind.Kind.DATE) {
            sb.append("        return com.codename1.mapping.JsonValues.toDate(_v);\n");
        } else if (f.format != null) {
            String prim = primitiveOfNumberKind(f.kind);
            String expr = numberFromTree(prim, "_v");
            if (isPrimitiveDesc(f.descriptor)) {
                sb.append("        return ").append(expr).append(";\n");
            } else {
                sb.append("        return ").append(boxOf(prim)).append(".valueOf(").append(expr).append(");\n");
            }
        } else {
            boolean list = !f.isArray;
            String ej = list ? f.kind.elementBinaryName : f.kind.binaryName;
            boolean prim = isPrimitiveName(ej);
            boolean mapped = !list && f.elemAdapter == null && f.kind.kind == PropertyTypeKind.Kind.REFERENCE
                    && !"java.lang.Object".equals(ej);
            sb.append("        if (!(_v instanceof java.util.List)) throw new IllegalArgumentException(\"expected a JSON array\");\n");
            sb.append("        java.util.List _src = (java.util.List) _v;\n");
            if (list) {
                sb.append("        java.util.ArrayList<").append(ej).append("> _a = new java.util.ArrayList<")
                  .append(ej).append(">();\n");
            } else {
                sb.append("        ").append(ej).append("[] _a = new ").append(ej).append("[_src.size()];\n");
            }
            if (mapped) {
                sb.append("        com.codename1.mapping.Mapper _nm = com.codename1.mapping.Mappers.get(")
                  .append(ej).append(".class);\n");
            }
            sb.append("        for (int _i = 0; _i < _src.size(); _i++) {\n");
            sb.append("            Object _e = _src.get(_i);\n");
            sb.append("            try {\n");
            if (list) {
                sb.append("                _a.add((").append(ej).append(") ")
                  .append(adapterConst(f.elemAdapter)).append(".fromJson(_e));\n");
            } else if (f.elemAdapter == null && f.kind.kind == PropertyTypeKind.Kind.ENUM) {
                sb.append("                ").append(ej).append(" _ev = null;\n");
                sb.append("                if (_e instanceof String) { try { _ev = ").append(ej)
                  .append(".valueOf((String) _e); } catch (IllegalArgumentException _ex) { _ev = null; } }\n");
                sb.append("                _a[_i] = _ev;\n");
            } else {
                sb.append("                _a[_i] = ").append(elementFromTree(f, ej, prim)).append(";\n");
            }
            sb.append("            } catch (IllegalArgumentException _x) {\n");
            sb.append("                throw com.codename1.mapping.JsonValues.at(\"$[\" + _i + \"]\", _x);\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("        return _a;\n");
        }
        sb.append("    }\n\n");
    }

    private static String elementFromTree(MappedField f, String ej, boolean prim) {
        if (f.elemAdapter != null) {
            return "(" + ej + ") " + adapterConst(f.elemAdapter) + ".fromJson(_e)";
        }
        if (prim) {
            if ("boolean".equals(ej)) return "Boolean.TRUE.equals(_e) || \"true\".equals(_e)";
            if ("char".equals(ej)) {
                return "(_e == null || _e.toString().length() == 0) ? '\\0' : _e.toString().charAt(0)";
            }
            if ("int".equals(ej) || "short".equals(ej) || "byte".equals(ej)) {
                String box = "int".equals(ej) ? "Integer" : "short".equals(ej) ? "Short" : "Byte";
                return "(" + ej + ") com.codename1.mapping.JsonValues.toIntegral(_e, "
                        + box + ".MIN_VALUE, " + box + ".MAX_VALUE)";
            }
            if ("long".equals(ej)) {
                return "com.codename1.mapping.JsonValues.toIntegral(_e, Long.MIN_VALUE, Long.MAX_VALUE)";
            }
            return numberFromTree(ej, "_e");
        }
        switch (f.kind.kind) {
            case DATE:
                return "_e == null ? null : com.codename1.mapping.JsonValues.toDate(_e)";
            case REFERENCE:
                if ("java.lang.Object".equals(ej)) return "_e";
                // A null element stays null; anything else that is not an
                // object is refused, as the server's codec refuses it.
                return "_e == null ? null : (" + ej + ") com.codename1.mapping.JsonValues.object(_nm, _e)";
            default:
                return scalarElement(ej, "_e");
        }
    }

    private static String numberFromTree(String prim, String var) {
        String l = "com.codename1.mapping.JsonValues.toLong(" + var + ")";
        String d = "com.codename1.mapping.JsonValues.toDouble(" + var + ")";
        if ("long".equals(prim)) return l;
        if ("double".equals(prim)) return d;
        if ("float".equals(prim)) return "(float) " + d;
        return "(" + prim + ") " + l;
    }

    private static String primitiveOfNumberKind(PropertyTypeKind k) {
        switch (k.kind) {
            case INT: return "int";
            case LONG: return "long";
            case SHORT: return "short";
            case BYTE: return "byte";
            case DOUBLE: return "double";
            default: return "float";
        }
    }

    private static boolean isPrimitiveName(String n) {
        return "int".equals(n) || "long".equals(n) || "short".equals(n) || "byte".equals(n)
                || "char".equals(n) || "double".equals(n) || "float".equals(n) || "boolean".equals(n);
    }

    private static String boxOf(String prim) {
        if ("int".equals(prim)) return "Integer";
        if ("char".equals(prim)) return "Character";
        return Character.toUpperCase(prim.charAt(0)) + prim.substring(1);
    }

    /// The Java source spelling of an (erased) descriptor.
    static String descriptorToJava(String desc) {
        int dims = 0;
        while (desc.charAt(dims) == '[') dims++;
        String base = desc.substring(dims);
        String t;
        switch (base.charAt(0)) {
            case 'I': t = "int"; break;
            case 'J': t = "long"; break;
            case 'S': t = "short"; break;
            case 'B': t = "byte"; break;
            case 'C': t = "char"; break;
            case 'D': t = "double"; break;
            case 'F': t = "float"; break;
            case 'Z': t = "boolean"; break;
            default: t = base.substring(1, base.length() - 1).replace('/', '.');
        }
        StringBuilder b = new StringBuilder(t);
        for (int i = 0; i < dims; i++) b.append("[]");
        return b.toString();
    }

    /// The default a `@JsonCreator` parameter takes when its member is absent:
    /// the type's own default, so a boxed parameter gets null, not zero.
    private static String defaultForDescriptor(String desc) {
        if (desc.length() != 1) return "null";
        switch (desc.charAt(0)) {
            case 'Z': return "false";
            case 'J': return "0L";
            case 'F': return "0.0f";
            case 'D': return "0.0";
            case 'C': return "'\\0'";
            default: return "0";
        }
    }

    // ---------------------------------------------------------------
    // JSON options: analysis (aliases, required, strict, omit-null,
    // format, adapters, creators, generics, arrays)
    // ---------------------------------------------------------------

    /// Reads `@JsonFormat` and `@JsonAdapter` for one field and validates the
    /// combination against its type. Every shape this cannot generate is a
    /// build error naming the class and field; nothing is skipped silently.
    private boolean checkJsonOptions(AnnotatedClass cls, MappedClass mc, MappedField mf,
                                     FieldInfo f, ProcessorContext ctx) {
        String where = mc.binaryName + "." + mf.name;
        String fieldAdapter = adapterOf(f.getAnnotation(JSON_ADAPTER_DESC));
        if (fieldAdapter != null) {
            if (mf.descriptor.length() == 1) {
                ctx.error(cls, "@JsonAdapter on " + where + ": a primitive field cannot take an"
                        + " adapter; declare it with its boxed type");
                return false;
            }
            if (mf.kind.kind == PropertyTypeKind.Kind.PROPERTY
                    || mf.kind.kind == PropertyTypeKind.Kind.LIST_PROPERTY) {
                ctx.error(cls, "@JsonAdapter on " + where + ": a Property / ListProperty field"
                        + " cannot take an adapter");
                return false;
            }
            if (!validAdapter(cls, mc.binaryName, mf.name, fieldAdapter, ctx)) return false;
            mf.adapter = fieldAdapter;
            mf.adapterCast = descriptorToJava(mf.descriptor);
        } else if (mf.kind.kind == PropertyTypeKind.Kind.REFERENCE) {
            String a = classAdapterOf(mf.kind.binaryName, ctx);
            if (a != null) {
                if (!validAdapter(cls, mc.binaryName, mf.name, a, ctx)) return false;
                if (mf.isArray) {
                    mf.elemAdapter = a;
                } else {
                    mf.adapter = a;
                    mf.adapterCast = mf.kind.binaryName;
                }
            }
        } else if ((mf.kind.kind == PropertyTypeKind.Kind.LIST
                || mf.kind.kind == PropertyTypeKind.Kind.LIST_PROPERTY)
                && !mf.elementIsEnum && !isScalarBinary(mf.kind.elementBinaryName)) {
            String a = classAdapterOf(mf.kind.elementBinaryName, ctx);
            if (a != null) {
                if (mf.kind.kind == PropertyTypeKind.Kind.LIST_PROPERTY) {
                    ctx.error(cls, "@JsonAdapter on element type " + mf.kind.elementBinaryName
                            + " of " + where + ": a ListProperty cannot use an adapted element;"
                            + " declare a java.util.List");
                    return false;
                }
                if (!validAdapter(cls, mc.binaryName, mf.name, a, ctx)) return false;
                mf.elemAdapter = a;
            }
        }

        AnnotationValues fa = f.getAnnotation(JSON_FORMAT_DESC);
        String fmt = fa == null ? null : enumName(fa.get("value"));
        if (fmt == null || "DEFAULT".equals(fmt)) return true;
        if (mf.adapter != null) {
            ctx.error(cls, "@JsonFormat on " + where + " cannot be combined with @JsonAdapter;"
                    + " the adapter decides the JSON form");
            return false;
        }
        boolean date = !mf.isArray && mf.kind.kind == PropertyTypeKind.Kind.DATE;
        boolean number = false;
        if (!mf.isArray) {
            switch (mf.kind.kind) {
                case INT: case LONG: case SHORT: case BYTE: case DOUBLE: case FLOAT:
                    number = true;
                    break;
                default:
                    break;
            }
        }
        if ("STRING".equals(fmt) && !(date || number)) {
            ctx.error(cls, "@JsonFormat(STRING) on " + where + ": only a java.util.Date or a"
                    + " numeric field (primitive or boxed) has a string form; the field is "
                    + describe(mf));
            return false;
        }
        if ("NUMBER".equals(fmt) && !date) {
            ctx.error(cls, "@JsonFormat(NUMBER) on " + where + ": only a java.util.Date has a"
                    + " number form other than its default; the field is " + describe(mf));
            return false;
        }
        mf.format = fmt;
        return true;
    }

    private static String describe(MappedField mf) {
        return (mf.isArray ? "an array of " : "") + mf.kind.kind + " (" + mf.descriptor + ")";
    }

    /// Binds a `@JsonCreator` constructor or static factory: each parameter
    /// names, through its own `@JsonProperty`, the JSON member it receives, and
    /// the field of that name supplies its type and options.
    private boolean bindCreator(AnnotatedClass cls, MappedClass mc, MethodInfo creator,
                                ProcessorContext ctx) {
        String where = "@JsonCreator " + mc.binaryName + "."
                + (creator.isConstructor() ? mc.simpleName : creator.getName());
        if (!creator.isPublic()) {
            ctx.error(cls, where + " must be public");
            return false;
        }
        if (!creator.isConstructor()) {
            if (!creator.isStatic()) {
                ctx.error(cls, where + " must be a constructor or a static factory method");
                return false;
            }
            if (!creator.getDescriptor().endsWith(")L" + cls.getInternalName() + ";")) {
                ctx.error(cls, where + " must return " + mc.binaryName);
                return false;
            }
        }
        List<String> params = parameterDescriptors(creator.getDescriptor());
        List<Map<String, AnnotationValues>> pa = creator.getParameterAnnotations();
        for (int i = 0; i < params.size(); i++) {
            Map<String, AnnotationValues> anns = (pa != null && i < pa.size()) ? pa.get(i) : null;
            AnnotationValues jp = anns == null ? null : anns.get(JSON_PROPERTY_DESC);
            String name = jp == null ? null : jp.getString("value");
            if (name == null || name.length() == 0) {
                ctx.error(cls, where + ": parameter " + i + " must carry @JsonProperty"
                        + " naming the JSON member it receives");
                return false;
            }
            MappedField match = null;
            for (MappedField f : mc.fields) {
                if (f.includeInJson && name.equals(f.jsonName)) {
                    match = f;
                    break;
                }
            }
            if (match == null) {
                ctx.error(cls, where + ": parameter " + i + " names JSON member \"" + name
                        + "\", which no JSON field of " + mc.binaryName + " declares");
                return false;
            }
            if (match.viaLocal) {
                ctx.error(cls, where + ": JSON member \"" + name + "\" is taken by two parameters");
                return false;
            }
            if (!params.get(i).equals(match.descriptor)) {
                ctx.error(cls, where + ": parameter " + i + " (\"" + name + "\") is "
                        + params.get(i) + " but field " + match.name + " is " + match.descriptor);
                return false;
            }
            if (jp.getBoolOrDefault("required", false)) match.required = true;
            if (match.aliases.isEmpty()) match.aliases = stringList(jp.get("aliases"));
            match.viaLocal = true;
            mc.creatorParams.add(match.name);
        }
        mc.creatorCall = creator.isConstructor()
                ? "new " + mc.binaryName
                : mc.binaryName + "." + creator.getName();
        return true;
    }

    private static List<String> parameterDescriptors(String desc) {
        List<String> out = new ArrayList<String>();
        int i = 1;
        while (desc.charAt(i) != ')') {
            int start = i;
            while (desc.charAt(i) == '[') i++;
            if (desc.charAt(i) == 'L') {
                i = desc.indexOf(';', i) + 1;
            } else {
                i++;
            }
            out.add(desc.substring(start, i));
        }
        return out;
    }

    /// The adapter class a `@JsonAdapter` names, or null.
    private static String adapterOf(AnnotationValues av) {
        if (av == null) return null;
        Object v = av.get("value");
        if (v instanceof org.objectweb.asm.Type) {
            return ((org.objectweb.asm.Type) v).getClassName();
        }
        return v == null ? null : v.toString();
    }

    /// The class-level `@JsonAdapter` of `binaryName`, or null.
    private static String classAdapterOf(String binaryName, ProcessorContext ctx) {
        if (binaryName == null || isScalarBinary(binaryName)) return null;
        AnnotatedClass c = ctx.lookupWithDependencies(binaryName.replace('.', '/'));
        return c == null ? null : adapterOf(c.getClassAnnotation(JSON_ADAPTER_DESC));
    }

    /// The generated mapper holds one instance per adapter class and calls it
    /// directly, so the class must be instantiable from generated code.
    private static boolean validAdapter(AnnotatedClass cls, String owner, String field,
                                        String adapter, ProcessorContext ctx) {
        String where = field == null ? owner : owner + "." + field;
        AnnotatedClass a = ctx.lookupWithDependencies(adapter.replace('.', '/'));
        if (a == null) {
            ctx.error(cls, "@JsonAdapter(" + adapter + ") on " + where
                    + ": the adapter class cannot be found on the compile classpath");
            return false;
        }
        if (!a.isPublic() || a.isAbstract() || a.isInterface() || !hasPublicNoArgConstructor(a)) {
            ctx.error(cls, "@JsonAdapter(" + adapter + ") on " + where
                    + ": the adapter must be a public concrete class with a public no-arg constructor");
            return false;
        }
        return true;
    }

    private static String enumName(Object v) {
        if (v instanceof String[] && ((String[]) v).length == 2) return ((String[]) v)[1];
        return null;
    }

    private static List<String> stringList(Object v) {
        List<String> out = new ArrayList<String>();
        if (v instanceof List) {
            for (Object o : (List<?>) v) {
                if (o != null && o.toString().length() > 0) out.add(o.toString());
            }
        }
        return out;
    }

    /// Classifies an element descriptor (`I`, `Ljava/lang/String;`, ...),
    /// upgrading an enum the way field classification does. Anything that is
    /// not a scalar, an enum, a date or a class reference is unsupported.
    private static PropertyTypeKind kindForDescriptor(String desc, ProcessorContext ctx) {
        PropertyTypeKind k = PropertyTypeKind.of(desc, null);
        if (k.kind == PropertyTypeKind.Kind.REFERENCE && isEnumType(k.binaryName, ctx)) {
            return PropertyTypeKind.enumType(k.binaryName);
        }
        switch (k.kind) {
            case PROPERTY: case LIST_PROPERTY: case LIST:
                return PropertyTypeKind.scalar("?");
            default:
                return k;
        }
    }

    private static PropertyTypeKind kindForBinary(String binary, ProcessorContext ctx) {
        PropertyTypeKind k = PropertyTypeKind.scalar(binary);
        if (k.kind != PropertyTypeKind.Kind.UNSUPPORTED) return k;
        if (isEnumType(binary, ctx)) return PropertyTypeKind.enumType(binary);
        return PropertyTypeKind.reference(binary);
    }

    /// The names of a class's type parameters, read from its generic signature
    /// (`<T:Ljava/lang/Object;>Ljava/lang/Object;` gives `[T]`).
    static List<String> classTypeParams(String sig) {
        List<String> names = new ArrayList<String>();
        if (sig == null || !sig.startsWith("<")) return names;
        int i = 1;
        while (sig.charAt(i) != '>') {
            int colon = sig.indexOf(':', i);
            names.add(sig.substring(i, colon));
            i = colon;
            while (sig.charAt(i) == ':') {
                i++;
                if (sig.charAt(i) == ':') continue;
                int[] p = { i };
                parseType(sig, p);
                i = p[0];
            }
        }
        return names;
    }

    static TypeRef parseTypeSignature(String sig) {
        try {
            return parseType(sig, new int[] { 0 });
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static TypeRef parseType(String s, int[] p) {
        TypeRef t = new TypeRef();
        while (s.charAt(p[0]) == '[') {
            t.dims++;
            p[0]++;
        }
        char c = s.charAt(p[0]);
        if (c == 'T') {
            int e = s.indexOf(';', p[0]);
            t.var = s.substring(p[0] + 1, e);
            p[0] = e + 1;
            return t;
        }
        if (c == 'L') {
            p[0]++;
            StringBuilder name = new StringBuilder();
            while (true) {
                char ch = s.charAt(p[0]);
                if (ch == ';') {
                    p[0]++;
                    break;
                }
                if (ch == '<') {
                    p[0]++;
                    t.args.clear();
                    while (s.charAt(p[0]) != '>') {
                        char a = s.charAt(p[0]);
                        if (a == '*') {
                            p[0]++;
                            TypeRef w = new TypeRef();
                            w.binary = "java.lang.Object";
                            t.args.add(w);
                        } else {
                            if (a == '+' || a == '-') p[0]++;
                            t.args.add(parseType(s, p));
                        }
                    }
                    p[0]++;
                    continue;
                }
                if (ch == '.') {
                    name.append('$');
                } else {
                    name.append(ch == '/' ? '.' : ch);
                }
                p[0]++;
            }
            t.binary = name.toString();
            return t;
        }
        p[0]++;
        t.binary = String.valueOf(c);
        return t;
    }

    private static boolean hasVar(TypeRef t) {
        if (t.var != null) return true;
        for (TypeRef a : t.args) {
            if (hasVar(a)) return true;
        }
        return false;
    }

    private static TypeRef subst(TypeRef t, Map<String, TypeRef> sub) {
        if (t.var != null) {
            TypeRef r = sub.get(t.var);
            if (r == null) return t;
            TypeRef c = r.copy();
            c.dims += t.dims;
            return c;
        }
        TypeRef c = new TypeRef();
        c.binary = t.binary;
        c.dims = t.dims;
        for (TypeRef a : t.args) c.args.add(subst(a, sub));
        return c;
    }

    /// Resolves every field of a generic `@Mapped` class used with concrete
    /// type arguments (`Page<User> page`) into a specialised copy, which the
    /// owner's mapper reads and writes with private methods of its own. A use
    /// whose arguments are not all concrete keeps the raw behaviour.
    private void assignSpecs(MappedClass owner, MappedClass cls, ProcessorContext ctx, int depth) {
        for (MappedField f : cls.fields) {
            if (!f.includeInJson || f.isArray || f.adapter != null) continue;
            if (f.kind.kind != PropertyTypeKind.Kind.REFERENCE || f.typeRef == null
                    || f.typeRef.args.isEmpty() || f.typeRef.dims != 0 || hasVar(f.typeRef)) continue;
            MappedClass gen = accepted.get(f.kind.binaryName);
            if (gen == null || gen.classAdapter != null
                    || gen.typeParams.size() != f.typeRef.args.size()) continue;
            if (depth > 4) {
                ctx.error(cls.binaryName + "." + f.name + ": generic @Mapped types nest more than"
                        + " five levels deep");
                return;
            }
            Map<String, TypeRef> sub = new LinkedHashMap<String, TypeRef>();
            for (int i = 0; i < gen.typeParams.size(); i++) {
                sub.put(gen.typeParams.get(i), f.typeRef.args.get(i));
            }
            int idx = owner.specs.size();
            MappedClass sp = specialize(gen, sub, idx, cls.binaryName + "." + f.name, ctx);
            if (sp == null) return;
            f.specIndex = idx;
            owner.specs.add(sp);
            assignSpecs(owner, sp, ctx, depth + 1);
        }
    }

    private MappedClass specialize(MappedClass gen, Map<String, TypeRef> sub, int idx,
                                   String use, ProcessorContext ctx) {
        MappedClass sp = gen.copyHeader();
        sp.prefix = "s" + idx + "_";
        for (MappedField f : gen.fields) {
            MappedField c = f.copy();
            c.specIndex = -1;
            c.helper = sp.prefix + c.name;
            if (c.typeRef != null && hasVar(c.typeRef) && c.adapter == null && c.format == null) {
                TypeRef r = subst(c.typeRef, sub);
                if (r.var != null) {
                    sp.fields.add(c);
                    continue;
                }
                String where = gen.binaryName + "." + f.name + " (as used by " + use + ")";
                if (r.dims > 1) {
                    ctx.error(where + " resolves to a multi-dimensional array");
                    return null;
                }
                TypeRef base = r.copy();
                base.dims = 0;
                boolean list = r.dims == 0 && ("java.util.List".equals(base.binary)
                        || "java.util.ArrayList".equals(base.binary));
                if (r.dims == 1) {
                    c.isArray = true;
                    c.kind = kindForBinary(base.binary, ctx);
                    c.elemAdapter = classAdapterOf(base.binary, ctx);
                    c.typeRef = null;
                } else if (list) {
                    TypeRef e = base.args.isEmpty() ? null : base.args.get(0);
                    String eb = (e == null || e.var != null || e.dims != 0) ? "java.lang.String" : e.binary;
                    c.kind = PropertyTypeKind.list(eb);
                    c.elementIsEnum = isEnumType(eb, ctx);
                    c.elemAdapter = c.elementIsEnum ? null : classAdapterOf(eb, ctx);
                    c.typeRef = null;
                } else {
                    c.kind = kindForBinary(base.binary, ctx);
                    if (c.kind.kind == PropertyTypeKind.Kind.REFERENCE) {
                        c.adapter = classAdapterOf(base.binary, ctx);
                        c.adapterCast = base.binary;
                    }
                    c.typeRef = base;
                }
                if (c.kind.kind == PropertyTypeKind.Kind.UNSUPPORTED
                        || c.kind.kind == PropertyTypeKind.Kind.BYTE_ARRAY && c.isArray) {
                    ctx.error(where + " resolves to an unsupported type " + base.binary);
                    return null;
                }
            }
            sp.fields.add(c);
        }
        return sp;
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
        /// `@JsonStrict`: an undeclared member is refused on read.
        boolean strict;
        /// Class-level `@JsonAdapter` of this class itself, or null.
        String classAdapter;
        /// Names of the class's type parameters, in order.
        List<String> typeParams = new ArrayList<String>();
        /// Field names the `@JsonCreator` takes, in parameter order.
        final List<String> creatorParams = new ArrayList<String>();
        /// `new pkg.Type` or `pkg.Type.factory`, or null without a creator.
        String creatorCall;
        /// Prefix of the private helpers generated for this class: empty for
        /// the mapper's own class, `sN_` for a specialisation.
        String prefix = "";
        /// Specialisations of generic classes this mapper's fields use; the
        /// index is a field's `specIndex`.
        final List<MappedClass> specs = new ArrayList<MappedClass>();

        MappedClass copyHeader() {
            MappedClass c = new MappedClass();
            c.binaryName = binaryName;
            c.packageName = packageName;
            c.simpleName = simpleName;
            c.mapperBinaryName = mapperBinaryName;
            c.mapperSimpleName = mapperSimpleName;
            c.xmlRootName = xmlRootName;
            c.isRecord = isRecord;
            c.strict = strict;
            c.classAdapter = classAdapter;
            c.typeParams = typeParams;
            c.creatorParams.addAll(creatorParams);
            c.creatorCall = creatorCall;
            return c;
        }

        MappedField field(String name) {
            for (MappedField f : fields) {
                if (f.name.equals(name)) return f;
            }
            return null;
        }
    }

    /// A parsed generic type signature: a type variable, or a class with
    /// type arguments, either with array dimensions.
    static final class TypeRef {
        String var;
        String binary;
        int dims;
        final List<TypeRef> args = new ArrayList<TypeRef>();

        TypeRef copy() {
            TypeRef c = new TypeRef();
            c.var = var;
            c.binary = binary;
            c.dims = dims;
            for (TypeRef a : args) c.args.add(a.copy());
            return c;
        }
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
        /// `true` when this field is a `List<E>` whose element type `E`
        /// is a Java enum -- elements serialise as their `name()`.
        boolean elementIsEnum;
        /// The field's descriptor (its erased type).
        String descriptor;
        /// `@JsonProperty(aliases)`: other names accepted on read.
        List<String> aliases = new ArrayList<String>();
        /// `@JsonProperty(required = true)`.
        boolean required;
        /// An explicit JSON null clears the field rather than leaving it as
        /// constructed: `@JsonStrict` classes and required members only.
        boolean nullClears;
        /// `@JsonOmitNull` on the field or its class.
        boolean omitNull;
        /// `STRING` or `NUMBER` from `@JsonFormat`, or null.
        String format;
        /// The adapter converting the whole value, or null, and the type its
        /// value is cast to before `toJson`.
        String adapter;
        String adapterCast;
        /// The class-level adapter of a list or array element type, or null.
        String elemAdapter;
        /// A one-dimensional array; `kind` then describes the ELEMENT.
        boolean isArray;
        /// Parsed generic signature, or null.
        TypeRef typeRef;
        /// Index into the owner mapper's specialisations, or -1.
        int specIndex = -1;
        /// Received by the `@JsonCreator`: read into a local, not the instance.
        boolean viaLocal;
        /// Written, but never set on read (no setter, or a final field, in a
        /// class built by a `@JsonCreator` that does not take it).
        boolean readOnly;
        /// Suffix of this field's private helper methods.
        String helper;

        MappedField copy() {
            MappedField c = new MappedField();
            c.name = name;
            c.kind = kind;
            c.jsonName = jsonName;
            c.xmlName = xmlName;
            c.xmlAttribute = xmlAttribute;
            c.includeInJson = includeInJson;
            c.includeInXml = includeInXml;
            c.useAccessor = useAccessor;
            c.getterName = getterName;
            c.setterName = setterName;
            c.elementIsEnum = elementIsEnum;
            c.descriptor = descriptor;
            c.aliases = aliases;
            c.required = required;
            c.omitNull = omitNull;
            c.format = format;
            c.adapter = adapter;
            c.adapterCast = adapterCast;
            c.elemAdapter = elemAdapter;
            c.isArray = isArray;
            c.typeRef = typeRef;
            c.specIndex = specIndex;
            c.viaLocal = viaLocal;
            c.readOnly = readOnly;
            c.helper = helper;
            return c;
        }
    }
}
