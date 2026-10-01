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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Build-time `@Bindable` processor. Two passes:
///
/// 1. **Source generation.** For every `@Bindable` class the processor
///    emits a `<SimpleName>Cn1Binder` Java class in the source class's
///    package, plus a single `cn1app.BinderBootstrap` whose constructor
///    references every generated binder. The build server probes the
///    project zip for `cn1app.BinderBootstrap` and splices
///    `new cn1app.BinderBootstrap();` into the iOS / Android per-build
///    application stub before `Display.init`; `JavaSEPort#postInit`
///    loads it via `Class.forName`. The bootstrap's constructor calls
///    `XxxCn1Binder.register()` (a public static hook) on each generated
///    binder, which installs an instance in `Binders`.
///
/// 2. **Setter instrumentation.** For every two-way `@Bind` field that
///    resolves through a setter method (whether the user wrote
///    `@Bind(setter="setName")` or the processor detected
///    `setName(String)` via JavaBeans convention), the processor reads
///    the original `.class` file, walks its bytecode with ASM, and
///    inserts `ALOAD 0; INVOKESTATIC com/codename1/binding/Binders.
///    notifyChanged (Ljava/lang/Object;)V` before every `XRETURN`
///    opcode. The instrumented setter is emitted back through
///    `ProcessorContext#emitClass`, overwriting the original. At
///    runtime, mutations to the model through the setter automatically
///    refresh every active binding for that model -- unless the calling
///    thread is already inside `Binders#enterUpdate` / `exitUpdate` (the
///    binder uses this to break the model -> component -> model loop).
///
/// #### Accessor resolution
///
/// Each `@Bind` field resolves a (read, write) accessor pair in this
/// order:
///
/// - `@Bind(getter="...", setter="...")` -- explicit override. Each
///   string is the method name on the model class. The setter is
///   instrumented if `twoWay` is true.
/// - JavaBeans convention -- `getFoo()` / `isFoo()` for read,
///   `setFoo(T)` for write. The processor scans the class's bytecode
///   for matching public instance methods. The setter is instrumented
///   when found.
/// - Direct public-field access -- the legacy path for `public String
///   foo;`-style models. No setter to instrument; two-way bindings on
///   such fields still flow component -> model through the listener
///   the binder installs, but the model -> component direction has to
///   be triggered explicitly via `Binding#refresh()`.
///
/// The processor fails the build when none of the three resolves.
///
/// #### Validation annotations
///
/// Alongside `@Bind`, a field may carry any of `@Required`, `@Length`,
/// `@Regex`, `@Email`, `@Url`, `@Numeric`, `@ExistIn`, `@Validate`. The
/// generator builds a `com.codename1.ui.validation.Validator` in the
/// `bind()` method, calls `addConstraint(component, constraints...)` once
/// per annotated field, and exposes the validator through
/// `Binding#getValidator()`. Multiple validation annotations on the same
/// field are combined under a `GroupConstraint` (first failure wins), so
/// `@Required @Email` on a single field reads naturally.
public final class BindingAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String BINDABLE_DESC = "Lcom/codename1/annotations/Bindable;";
    public static final String BIND_DESC = "Lcom/codename1/annotations/Bind;";

    /// Field-level validation annotations consumed by the binder generator.
    /// Each one maps to a single `com.codename1.ui.validation.Constraint`
    /// installed on the matching component via `Validator.addConstraint`.
    static final String REQUIRED_DESC = "Lcom/codename1/annotations/Required;";
    static final String LENGTH_DESC = "Lcom/codename1/annotations/Length;";
    static final String REGEX_DESC = "Lcom/codename1/annotations/Regex;";
    static final String EMAIL_DESC = "Lcom/codename1/annotations/Email;";
    static final String URL_DESC = "Lcom/codename1/annotations/Url;";
    static final String NUMERIC_DESC = "Lcom/codename1/annotations/Numeric;";
    static final String EXIST_IN_DESC = "Lcom/codename1/annotations/ExistIn;";
    static final String VALIDATE_DESC = "Lcom/codename1/annotations/Validate;";

    static final String BOOTSTRAP_BINARY = "cn1app.BinderBootstrap";
    static final String BOOTSTRAP_SIMPLE = "BinderBootstrap";
    static final String BOOTSTRAP_PACKAGE = "cn1app";

    private static final int ASM_API = Opcodes.ASM9;

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(BINDABLE_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    private final TreeMap<String, BindableClass> accepted = new TreeMap<String, BindableClass>();

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
        if (cls.isSynthetic()) {
            return;
        }
        if (cls.getClassAnnotation(BINDABLE_DESC) == null) {
            return;
        }
        if (cls.isAbstract() || cls.isInterface()) {
            ctx.error(cls, "@Bindable requires a concrete class; " + cls.getBinaryName()
                    + " is abstract or an interface");
            return;
        }

        BindableClass bc = new BindableClass();
        bc.binaryName = cls.getBinaryName();
        bc.internalName = cls.getInternalName();
        bc.classFile = cls.getClassFile();
        bc.simpleName = simpleName(cls.getBinaryName());
        bc.packageName = packageOf(cls.getBinaryName());
        bc.binderSimpleName = bc.simpleName + "Cn1Binder";
        bc.binderBinaryName = (bc.packageName.length() == 0)
                ? bc.binderSimpleName
                : bc.packageName + "." + bc.binderSimpleName;

        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic()) {
                continue;
            }
            AnnotationValues bind = f.getAnnotation(BIND_DESC);
            if (bind == null) {
                continue;
            }
            BoundField bf = new BoundField();
            bf.fieldName = f.getName();
            bf.componentName = bind.getString("name");
            bf.attr = readAttr(bind);
            bf.twoWay = bind.getBoolOrDefault("twoWay", true);
            bf.kind = PropertyTypeKind.of(f);

            if (bf.componentName == null || bf.componentName.length() == 0) {
                ctx.error(cls, "@Bind on " + bc.binaryName + "." + f.getName()
                        + " requires name() to identify the target component");
                continue;
            }
            if (bf.kind.kind == PropertyTypeKind.Kind.UNSUPPORTED) {
                ctx.error(cls, "@Bind field " + bc.binaryName + "." + f.getName()
                        + " has an unsupported type (descriptor " + f.getDescriptor() + ")");
                continue;
            }

            String explicitGetter = bind.getStringOrDefault("getter", "");
            String explicitSetter = bind.getStringOrDefault("setter", "");
            if (!resolveAccessors(bf, f, cls, explicitGetter, explicitSetter, ctx)) {
                continue;
            }
            collectValidationAnnotations(bf, f, cls, ctx);
            bc.fields.add(bf);
        }
        // @Bindable with no @Bind fields is accepted -- the generated
        // binder is a no-op, the registration still happens.
        accepted.put(bc.binaryName, bc);
    }

    private static BindAttrName readAttr(AnnotationValues bind) {
        Object v = bind.get("attr");
        if (v instanceof String[]) {
            String name = ((String[]) v)[1];
            for (BindAttrName candidate : BindAttrName.values()) {
                if (candidate.name().equals(name)) {
                    return candidate;
                }
            }
        }
        return BindAttrName.TEXT;
    }

    /// Walks the class's methods looking for a matching getter / setter
    /// for `field`. Falls back to direct field access only when the field
    /// is public and no accessor is required by the explicit annotation
    /// members. Returns false (and reports an error) when nothing usable
    /// is found.
    private static boolean resolveAccessors(BoundField bf, FieldInfo field, AnnotatedClass cls,
                                            String explicitGetter, String explicitSetter,
                                            ProcessorContext ctx) {
        String desc = field.getDescriptor();
        String simpleField = field.getName();
        // For Property<T,?> fields the read/write descriptors are
        // Property; the field-level accessor goes through .get()/.set().
        // Honour explicit overrides verbatim in that case too.

        // Getter.
        if (explicitGetter.length() > 0) {
            MethodInfo m = findMethod(cls, explicitGetter, null);
            if (m == null || !m.isPublic() || m.isStatic()) {
                ctx.error(cls, "@Bind getter='" + explicitGetter + "' on "
                        + cls.getBinaryName() + "." + simpleField
                        + " must name a public instance method");
                return false;
            }
            bf.getter = m.getName();
            bf.getterDescriptor = m.getDescriptor();
        } else {
            MethodInfo m = findJavaBeansGetter(cls, simpleField, desc);
            if (m != null) {
                bf.getter = m.getName();
                bf.getterDescriptor = m.getDescriptor();
            } else if (field.isPublic()) {
                bf.getter = null; // direct field access
                bf.getterDescriptor = null;
            } else {
                ctx.error(cls, "@Bind on " + cls.getBinaryName() + "." + simpleField
                        + ": no readable accessor. Make the field public, add a JavaBeans "
                        + "get/is accessor, or use @Bind(getter=\"...\").");
                return false;
            }
        }

        // Setter.
        if (explicitSetter.length() > 0) {
            MethodInfo m = findMethod(cls, explicitSetter, null);
            if (m == null || !m.isPublic() || m.isStatic()) {
                ctx.error(cls, "@Bind setter='" + explicitSetter + "' on "
                        + cls.getBinaryName() + "." + simpleField
                        + " must name a public instance method");
                return false;
            }
            bf.setter = m.getName();
            bf.setterDescriptor = m.getDescriptor();
        } else {
            MethodInfo m = findJavaBeansSetter(cls, simpleField, desc);
            if (m != null) {
                bf.setter = m.getName();
                bf.setterDescriptor = m.getDescriptor();
            } else if (field.isPublic()) {
                bf.setter = null; // direct field assignment
                bf.setterDescriptor = null;
            } else {
                // Allowed to be missing for one-way bindings.
                if (bf.twoWay && bf.attr.isTwoWayCapable()) {
                    ctx.error(cls, "@Bind two-way on " + cls.getBinaryName() + "." + simpleField
                            + ": no writable accessor. Make the field public, add a JavaBeans "
                            + "set accessor, or use @Bind(setter=\"...\", twoWay=false).");
                    return false;
                }
                bf.setter = null;
                bf.setterDescriptor = null;
            }
        }
        return true;
    }

    /// Reads each validation annotation (`@Required`, `@Length`, `@Regex`,
    /// `@Email`, `@Url`, `@Numeric`, `@ExistIn`, `@Validate`) off `field` and
    /// appends a `Validation` spec to `bf` for every one found. The order is
    /// preserved -- the generated binder hands them to
    /// `Validator.addConstraint(Component, Constraint...)` in declaration
    /// order, and a `GroupConstraint` makes the first failing constraint
    /// win.
    private static void collectValidationAnnotations(BoundField bf, FieldInfo field,
                                                      AnnotatedClass cls, ProcessorContext ctx) {
        AnnotationValues a;

        a = field.getAnnotation(REQUIRED_DESC);
        if (a != null) {
            Validation v = new Validation(ValidationKind.REQUIRED);
            v.message = a.getStringOrDefault("message", "");
            bf.validations.add(v);
        }

        a = field.getAnnotation(LENGTH_DESC);
        if (a != null) {
            Validation v = new Validation(ValidationKind.LENGTH);
            v.intArg = a.getIntOrDefault("min", 1);
            v.message = a.getStringOrDefault("message", "");
            bf.validations.add(v);
        }

        a = field.getAnnotation(REGEX_DESC);
        if (a != null) {
            String pattern = a.getStringOrDefault("pattern", "");
            if (pattern.length() == 0) {
                ctx.error(cls, "@Regex on " + cls.getBinaryName() + "." + field.getName()
                        + " requires a non-empty pattern");
            } else {
                Validation v = new Validation(ValidationKind.REGEX);
                v.stringArg = pattern;
                v.message = a.getStringOrDefault("message", "Invalid value");
                bf.validations.add(v);
            }
        }

        a = field.getAnnotation(EMAIL_DESC);
        if (a != null) {
            Validation v = new Validation(ValidationKind.EMAIL);
            v.message = a.getStringOrDefault("message", "");
            bf.validations.add(v);
        }

        a = field.getAnnotation(URL_DESC);
        if (a != null) {
            Validation v = new Validation(ValidationKind.URL);
            v.message = a.getStringOrDefault("message", "");
            bf.validations.add(v);
        }

        a = field.getAnnotation(NUMERIC_DESC);
        if (a != null) {
            Validation v = new Validation(ValidationKind.NUMERIC);
            v.boolArg = a.getBoolOrDefault("decimal", false);
            v.doubleMin = doubleOrDefault(a, "min", Double.NEGATIVE_INFINITY);
            v.doubleMax = doubleOrDefault(a, "max", Double.POSITIVE_INFINITY);
            v.message = a.getStringOrDefault("message", "");
            bf.validations.add(v);
        }

        a = field.getAnnotation(EXIST_IN_DESC);
        if (a != null) {
            List<String> values = new ArrayList<String>();
            Object raw = a.get("value");
            if (raw instanceof List) {
                for (Object item : (List<?>) raw) {
                    if (item instanceof String) {
                        values.add((String) item);
                    }
                }
            }
            if (values.isEmpty()) {
                ctx.error(cls, "@ExistIn on " + cls.getBinaryName() + "." + field.getName()
                        + " requires at least one allowed value");
            } else {
                Validation v = new Validation(ValidationKind.EXIST_IN);
                v.stringArrayArg = values.toArray(new String[0]);
                v.boolArg = a.getBoolOrDefault("caseSensitive", false);
                v.message = a.getStringOrDefault("message", "");
                bf.validations.add(v);
            }
        }

        a = field.getAnnotation(VALIDATE_DESC);
        if (a != null) {
            Object raw = a.get("value");
            String binaryName = null;
            if (raw instanceof Type) {
                binaryName = ((Type) raw).getClassName();
            }
            if (binaryName == null || binaryName.length() == 0) {
                ctx.error(cls, "@Validate on " + cls.getBinaryName() + "." + field.getName()
                        + " must name a Constraint class");
            } else {
                Validation v = new Validation(ValidationKind.CUSTOM);
                v.stringArg = binaryName;
                bf.validations.add(v);
            }
        }
    }

    private static double doubleOrDefault(AnnotationValues a, String key, double defaultValue) {
        Object v = a.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return defaultValue;
    }

    private static MethodInfo findMethod(AnnotatedClass cls, String name, String descriptor) {
        for (MethodInfo m : cls.getMethods()) {
            if (!m.getName().equals(name)) {
                continue;
            }
            if (descriptor != null && !m.getDescriptor().equals(descriptor)) {
                continue;
            }
            return m;
        }
        return null;
    }

    private static MethodInfo findJavaBeansGetter(AnnotatedClass cls, String field, String fieldDesc) {
        String cap = capitalize(field);
        String getDesc = "()" + fieldDesc;
        String isDesc = "()Z";
        MethodInfo m = findMethod(cls, "get" + cap, getDesc);
        if (m != null && m.isPublic() && !m.isStatic()) {
            return m;
        }
        if ("Z".equals(fieldDesc)) {
            m = findMethod(cls, "is" + cap, isDesc);
            if (m != null && m.isPublic() && !m.isStatic()) {
                return m;
            }
        }
        return null;
    }

    private static MethodInfo findJavaBeansSetter(AnnotatedClass cls, String field, String fieldDesc) {
        String cap = capitalize(field);
        String setName = "set" + cap;
        // Prefer void(field) first; fall back to any setX(field) shape.
        for (MethodInfo m : cls.getMethods()) {
            if (!m.getName().equals(setName) || !m.isPublic() || m.isStatic()) {
                continue;
            }
            String d = m.getDescriptor();
            // Single-arg method whose param descriptor matches the field.
            if (d != null && d.startsWith("(") && d.contains(")")) {
                int close = d.indexOf(')');
                String params = d.substring(1, close);
                if (params.equals(fieldDesc)) {
                    return m;
                }
            }
        }
        return null;
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---------------------------------------------------------------
    // finish: emit binder sources + instrument setters
    // ---------------------------------------------------------------

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) {
            return;
        }
        if (accepted.isEmpty()) {
            return;
        }

        // 1. Source generation.
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (BindableClass bc : accepted.values()) {
            sources.put(bc.binderBinaryName, generateBinderSource(bc));
        }
        sources.put(BOOTSTRAP_BINARY, generateBootstrapSource(accepted.values()));
        try {
            List<File> cp = new ArrayList<File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated binder sources: "
                    + ioe.getMessage(), ioe);
        }

        // 2. Setter instrumentation.
        for (BindableClass bc : accepted.values()) {
            instrumentSetters(bc, ctx);
        }

        ctx.getLog().info("cn1: generated " + accepted.size()
                + " @Bindable binder(s) + " + BOOTSTRAP_BINARY);
    }

    /// Re-reads the source class's bytecode, walks its methods, and for
    /// every method that matches a resolved setter for a two-way bound
    /// field, inserts a `Binders.notifyChanged(this)` call before every
    /// `XRETURN` opcode. The modified bytes go through
    /// `ProcessorContext#emitClass`, overwriting the original.
    private void instrumentSetters(BindableClass bc, ProcessorContext ctx)
            throws ProcessingException {
        // Collect the (name, descriptor) pairs to instrument.
        final Set<String> targets = new LinkedHashSet<String>();
        for (BoundField bf : bc.fields) {
            if (!bf.twoWay || !bf.attr.isTwoWayCapable() || bf.setter == null) {
                continue;
            }
            targets.add(bf.setter + bf.setterDescriptor);
        }
        if (targets.isEmpty()) {
            return;
        }
        if (bc.classFile == null || !bc.classFile.isFile()) {
            ctx.error(null, "Cannot instrument setters on " + bc.binaryName
                    + ": class file is missing");
            return;
        }

        byte[] original;
        try {
            original = Files.readAllBytes(bc.classFile.toPath());
        } catch (IOException ioe) {
            throw new ProcessingException("Could not read " + bc.classFile + ": "
                    + ioe.getMessage(), ioe);
        }

        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(ASM_API, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                              String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (mv == null) {
                    return null;
                }
                if (!targets.contains(name + descriptor)) {
                    return mv;
                }
                return new MethodVisitor(ASM_API, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "com/codename1/binding/Binders",
                                    "notifyChanged",
                                    "(Ljava/lang/Object;)V",
                                    false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);

        ctx.emitClass(bc.internalName, writer.toByteArray());
    }

    // ---------------------------------------------------------------
    // Source generation
    // ---------------------------------------------------------------

    private static String generateBinderSource(BindableClass bc) {
        StringBuilder sb = new StringBuilder(3072);
        if (bc.packageName.length() > 0) {
            sb.append("package ").append(bc.packageName).append(";\n\n");
        }
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(bc.binderSimpleName)
                .append(" implements com.codename1.binding.Binder<").append(bc.binaryName).append("> {\n\n");

        sb.append("    public static void register() {\n");
        sb.append("        com.codename1.binding.Binders.register(new ").append(bc.binderSimpleName).append("());\n");
        sb.append("    }\n\n");

        sb.append("    public ").append(bc.binderSimpleName).append("() {\n");
        sb.append("    }\n\n");

        sb.append("    public Class<").append(bc.binaryName).append("> type() {\n");
        sb.append("        return ").append(bc.binaryName).append(".class;\n");
        sb.append("    }\n\n");

        sb.append("    public com.codename1.binding.Binding bind(final ").append(bc.binaryName)
                .append(" model, final com.codename1.ui.Container container) {\n");
        sb.append("        final java.util.ArrayList<com.codename1.ui.events.ActionListener> _disposers = new java.util.ArrayList<com.codename1.ui.events.ActionListener>();\n");
        sb.append("        final com.codename1.ui.validation.Validator _validator = new com.codename1.ui.validation.Validator();\n");

        // Resolve each component once.
        for (int i = 0; i < bc.fields.size(); i++) {
            BoundField f = bc.fields.get(i);
            sb.append("        final com.codename1.ui.Component _c").append(i)
                    .append(" = _findByName(container, \"").append(escape(f.componentName)).append("\");\n");
        }

        // Wire validation annotations into the Validator. Constraints are
        // added in declaration order; multiple constraints on a single
        // component compose into a GroupConstraint via the varargs overload
        // (first failure wins).
        for (int i = 0; i < bc.fields.size(); i++) {
            BoundField f = bc.fields.get(i);
            if (f.validations.isEmpty()) {
                continue;
            }
            emitValidationWireUp(sb, f, i);
        }

        // refresh() pushes model -> components inside an update region.
        sb.append("        final Runnable _refresh = new Runnable() { public void run() {\n");
        sb.append("            com.codename1.binding.Binders.enterUpdate();\n");
        sb.append("            try {\n");
        for (int i = 0; i < bc.fields.size(); i++) {
            emitRefreshOne(sb, bc.fields.get(i), i);
        }
        sb.append("            } finally { com.codename1.binding.Binders.exitUpdate(); }\n");
        sb.append("        }};\n");
        sb.append("        _refresh.run();\n");

        // commit() pulls components -> model via the setters, again
        // inside an update region.
        sb.append("        final Runnable _commit = new Runnable() { public void run() {\n");
        sb.append("            com.codename1.binding.Binders.enterUpdate();\n");
        sb.append("            try {\n");
        for (int i = 0; i < bc.fields.size(); i++) {
            BoundField f = bc.fields.get(i);
            if (!f.twoWay || !f.attr.isTwoWayCapable()) {
                continue;
            }
            emitCommitOne(sb, f, i);
        }
        sb.append("            } finally { com.codename1.binding.Binders.exitUpdate(); }\n");
        sb.append("        }};\n");

        // Per-field live listeners.
        for (int i = 0; i < bc.fields.size(); i++) {
            BoundField f = bc.fields.get(i);
            if (!f.twoWay || !f.attr.isTwoWayCapable()) {
                continue;
            }
            emitListenerInstall(sb, f, i);
        }

        // Register the binding for notifyChanged dispatch.
        sb.append("        final String _typeName = ").append(bc.binaryName).append(".class.getName();\n");
        sb.append("        final ").append(bc.binaryName).append(" _modelRef = model;\n");
        sb.append("        com.codename1.binding.NotifiableBinding _binding = new com.codename1.binding.NotifiableBinding() {\n");
        sb.append("            public void refresh() { _refresh.run(); }\n");
        sb.append("            public void commit() { _commit.run(); }\n");
        sb.append("            public void disconnect() {\n");
        sb.append("                com.codename1.binding.Binders.unregisterBinding(this);\n");
        sb.append("                for (com.codename1.ui.events.ActionListener _d : _disposers) _d.actionPerformed(null);\n");
        sb.append("                _disposers.clear();\n");
        sb.append("            }\n");
        sb.append("            public com.codename1.ui.validation.Validator getValidator() { return _validator; }\n");
        sb.append("            public String modelTypeName() { return _typeName; }\n");
        sb.append("            public boolean matches(Object o) { return o == _modelRef; }\n");
        sb.append("        };\n");
        sb.append("        com.codename1.binding.Binders.registerBinding(_binding);\n");
        sb.append("        return _binding;\n");
        sb.append("    }\n\n");

        // Recursive name lookup.
        sb.append("    private static com.codename1.ui.Component _findByName(com.codename1.ui.Container c, String name) {\n");
        sb.append("        if (c == null || name == null) return null;\n");
        sb.append("        if (name.equals(c.getName())) return c;\n");
        sb.append("        int n = c.getComponentCount();\n");
        sb.append("        for (int i = 0; i < n; i++) {\n");
        sb.append("            com.codename1.ui.Component child = c.getComponentAt(i);\n");
        sb.append("            if (name.equals(child.getName())) return child;\n");
        sb.append("            if (child instanceof com.codename1.ui.Container) {\n");
        sb.append("                com.codename1.ui.Component found = _findByName((com.codename1.ui.Container) child, name);\n");
        sb.append("                if (found != null) return found;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String generateBootstrapSource(Iterable<BindableClass> classes) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(BOOTSTRAP_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("///\n");
        sb.append("/// Component binding bootstrap. The iOS / Android per-build\n");
        sb.append("/// application stub instantiates this class before Display.init\n");
        sb.append("/// (the build server probes the project zip for it and emits the\n");
        sb.append("/// install line conditionally); JavaSEPort.postInit picks it up\n");
        sb.append("/// via Class.forName for the simulator and desktop runs.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        for (BindableClass bc : classes) {
            sb.append("        ").append(bc.binderBinaryName).append(".register();\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Per-attribute code generation
    // ---------------------------------------------------------------

    /// Generates the `model.fieldOrAccessor.get()` expression to read the
    /// field, honouring Property unwrapping and accessor resolution.
    private static String readExpr(BoundField f) {
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
            // Property fields: always go through .get() regardless of
            // whether an accessor was resolved (the accessor returns the
            // Property wrapper, then we still call .get()).
            if (f.getter != null) {
                return "model." + f.getter + "().get()";
            }
            return "model." + f.fieldName + ".get()";
        }
        if (f.getter != null) {
            return "model." + f.getter + "()";
        }
        return "model." + f.fieldName;
    }

    private static void emitRefreshOne(StringBuilder sb, BoundField f, int i) {
        sb.append("            if (_c").append(i).append(" != null) {\n");
        String modelExpr = readExpr(f);
        switch (f.attr) {
            case TEXT:
                sb.append("                String _v = ").append(modelExpr).append(" == null ? \"\" : String.valueOf(").append(modelExpr).append(");\n");
                sb.append("                if (_c").append(i).append(" instanceof com.codename1.ui.TextArea) ((com.codename1.ui.TextArea) _c").append(i).append(").setText(_v);\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.Label) ((com.codename1.ui.Label) _c").append(i).append(").setText(_v);\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.Button) ((com.codename1.ui.Button) _c").append(i).append(").setText(_v);\n");
                break;
            case UIID:
                sb.append("                String _v = ").append(modelExpr).append(" == null ? \"\" : String.valueOf(").append(modelExpr).append(");\n");
                sb.append("                _c").append(i).append(".setUIID(_v);\n");
                break;
            case HIDDEN:
                sb.append("                _c").append(i).append(".setHidden(").append(boolExpr(f, modelExpr)).append(");\n");
                break;
            case VISIBLE:
                sb.append("                _c").append(i).append(".setVisible(").append(boolExpr(f, modelExpr)).append(");\n");
                break;
            case ENABLED:
                sb.append("                _c").append(i).append(".setEnabled(").append(boolExpr(f, modelExpr)).append(");\n");
                break;
            case SELECTED:
                sb.append("                if (_c").append(i).append(" instanceof com.codename1.ui.RadioButton) ((com.codename1.ui.RadioButton) _c").append(i).append(").setSelected(").append(boolExpr(f, modelExpr)).append(");\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.CheckBox) ((com.codename1.ui.CheckBox) _c").append(i).append(").setSelected(").append(boolExpr(f, modelExpr)).append(");\n");
                break;
            case ICON_NAME:
                sb.append("                String _v = ").append(modelExpr).append(" == null ? null : String.valueOf(").append(modelExpr).append(");\n");
                sb.append("                if (_v != null && _c").append(i).append(" instanceof com.codename1.ui.Label) {\n");
                sb.append("                    com.codename1.ui.util.Resources _r = com.codename1.ui.util.Resources.getGlobalResources();\n");
                sb.append("                    if (_r != null) ((com.codename1.ui.Label) _c").append(i).append(").setIcon(_r.getImage(_v));\n");
                sb.append("                }\n");
                break;
            case NAME:
                sb.append("                _c").append(i).append(".setName(String.valueOf(").append(modelExpr).append("));\n");
                break;
        }
        sb.append("            }\n");
    }

    private static void emitCommitOne(StringBuilder sb, BoundField f, int i) {
        sb.append("                if (_c").append(i).append(" != null) {\n");
        switch (f.attr) {
            case TEXT:
                sb.append("                    String _v = null;\n");
                sb.append("                    if (_c").append(i).append(" instanceof com.codename1.ui.TextArea) _v = ((com.codename1.ui.TextArea) _c").append(i).append(").getText();\n");
                sb.append("                    else if (_c").append(i).append(" instanceof com.codename1.ui.Label) _v = ((com.codename1.ui.Label) _c").append(i).append(").getText();\n");
                sb.append("                    if (_v != null) {\n");
                emitWriteFromString(sb, f, "_v");
                sb.append("                    }\n");
                break;
            case SELECTED:
                sb.append("                    boolean _v = false;\n");
                sb.append("                    if (_c").append(i).append(" instanceof com.codename1.ui.RadioButton) _v = ((com.codename1.ui.RadioButton) _c").append(i).append(").isSelected();\n");
                sb.append("                    else if (_c").append(i).append(" instanceof com.codename1.ui.CheckBox) _v = ((com.codename1.ui.CheckBox) _c").append(i).append(").isSelected();\n");
                emitWriteFromBoolean(sb, f, "_v");
                break;
            default:
                break;
        }
        sb.append("                }\n");
    }

    private static void emitListenerInstall(StringBuilder sb, BoundField f, int i) {
        if (f.attr == BindAttrName.TEXT) {
            sb.append("        if (_c").append(i).append(" instanceof com.codename1.ui.TextArea) {\n");
            sb.append("            final com.codename1.ui.TextArea _ta = (com.codename1.ui.TextArea) _c").append(i).append(";\n");
            sb.append("            final com.codename1.ui.events.DataChangedListener _l = new com.codename1.ui.events.DataChangedListener() {\n");
            sb.append("                public void dataChanged(int type, int index) {\n");
            sb.append("                    if (com.codename1.binding.Binders.isInUpdate()) return;\n");
            sb.append("                    com.codename1.binding.Binders.enterUpdate();\n");
            sb.append("                    try {\n");
            sb.append("                        String _v = _ta.getText();\n");
            emitWriteFromString(sb, f, "_v");
            sb.append("                    } finally { com.codename1.binding.Binders.exitUpdate(); }\n");
            sb.append("                }\n");
            sb.append("            };\n");
            sb.append("            _ta.addDataChangedListener(_l);\n");
            sb.append("            _disposers.add(new com.codename1.ui.events.ActionListener() {\n");
            sb.append("                public void actionPerformed(com.codename1.ui.events.ActionEvent _e) { _ta.removeDataChangedListener(_l); }\n");
            sb.append("            });\n");
            sb.append("        }\n");
        } else if (f.attr == BindAttrName.SELECTED) {
            sb.append("        if (_c").append(i).append(" instanceof com.codename1.ui.Button) {\n");
            sb.append("            final com.codename1.ui.Button _b = (com.codename1.ui.Button) _c").append(i).append(";\n");
            sb.append("            final com.codename1.ui.events.ActionListener _l = new com.codename1.ui.events.ActionListener() {\n");
            sb.append("                public void actionPerformed(com.codename1.ui.events.ActionEvent _e) {\n");
            sb.append("                    if (com.codename1.binding.Binders.isInUpdate()) return;\n");
            sb.append("                    com.codename1.binding.Binders.enterUpdate();\n");
            sb.append("                    try {\n");
            sb.append("                        boolean _v = false;\n");
            sb.append("                        if (_b instanceof com.codename1.ui.RadioButton) _v = ((com.codename1.ui.RadioButton) _b).isSelected();\n");
            sb.append("                        else if (_b instanceof com.codename1.ui.CheckBox) _v = ((com.codename1.ui.CheckBox) _b).isSelected();\n");
            emitWriteFromBoolean(sb, f, "_v");
            sb.append("                    } finally { com.codename1.binding.Binders.exitUpdate(); }\n");
            sb.append("                }\n");
            sb.append("            };\n");
            sb.append("            _b.addActionListener(_l);\n");
            sb.append("            _disposers.add(new com.codename1.ui.events.ActionListener() {\n");
            sb.append("                public void actionPerformed(com.codename1.ui.events.ActionEvent _e) { _b.removeActionListener(_l); }\n");
            sb.append("            });\n");
            sb.append("        }\n");
        }
    }

    /// Emits validator wire-up for the component at index `i`. Generates a
    /// guarded block that constructs each `Constraint` and hands them to
    /// `Validator.addConstraint(Component, Constraint...)`.
    private static void emitValidationWireUp(StringBuilder sb, BoundField f, int i) {
        sb.append("        if (_c").append(i).append(" != null) {\n");
        sb.append("            _validator.addConstraint(_c").append(i);
        for (Validation v : f.validations) {
            sb.append(", ");
            emitConstraintExpr(sb, v);
        }
        sb.append(");\n");
        sb.append("        }\n");
    }

    private static void emitConstraintExpr(StringBuilder sb, Validation v) {
        switch (v.kind) {
            case REQUIRED:
                // LengthConstraint(1, message) -- value required (non-empty).
                if (v.message.length() == 0) {
                    sb.append("new com.codename1.ui.validation.LengthConstraint(1)");
                } else {
                    sb.append("new com.codename1.ui.validation.LengthConstraint(1, \"")
                            .append(escape(v.message)).append("\")");
                }
                break;
            case LENGTH:
                if (v.message.length() == 0) {
                    sb.append("new com.codename1.ui.validation.LengthConstraint(")
                            .append(v.intArg).append(")");
                } else {
                    sb.append("new com.codename1.ui.validation.LengthConstraint(")
                            .append(v.intArg).append(", \"").append(escape(v.message)).append("\")");
                }
                break;
            case REGEX:
                sb.append("new com.codename1.ui.validation.RegexConstraint(\"")
                        .append(escape(v.stringArg)).append("\", \"")
                        .append(escape(v.message)).append("\")");
                break;
            case EMAIL:
                if (v.message.length() == 0) {
                    sb.append("com.codename1.ui.validation.RegexConstraint.validEmail()");
                } else {
                    sb.append("com.codename1.ui.validation.RegexConstraint.validEmail(\"")
                            .append(escape(v.message)).append("\")");
                }
                break;
            case URL:
                if (v.message.length() == 0) {
                    sb.append("com.codename1.ui.validation.RegexConstraint.validURL()");
                } else {
                    sb.append("com.codename1.ui.validation.RegexConstraint.validURL(\"")
                            .append(escape(v.message)).append("\")");
                }
                break;
            case NUMERIC:
                String msg = v.message.length() == 0 ? "null" : "\"" + escape(v.message) + "\"";
                sb.append("new com.codename1.ui.validation.NumericConstraint(")
                        .append(v.boolArg).append(", ")
                        .append(doubleLiteral(v.doubleMin)).append(", ")
                        .append(doubleLiteral(v.doubleMax)).append(", ")
                        .append(msg).append(")");
                break;
            case EXIST_IN:
                String msgX = v.message.length() == 0 ? "null" : "\"" + escape(v.message) + "\"";
                sb.append("new com.codename1.ui.validation.ExistInConstraint(new String[]{");
                for (int k = 0; k < v.stringArrayArg.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append('"').append(escape(v.stringArrayArg[k])).append('"');
                }
                sb.append("}, ").append(v.boolArg).append(", ").append(msgX).append(")");
                break;
            case CUSTOM:
                // v.stringArg is the binary name of the Constraint implementation.
                sb.append("new ").append(v.stringArg).append("()");
                break;
        }
    }

    private static String doubleLiteral(double d) {
        if (Double.isNaN(d)) {
            return "Double.NaN";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
        }
        return Double.toString(d);
    }

    private static String boolExpr(BoundField f, String modelExpr) {
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY
                && "java.lang.Boolean".equals(f.kind.elementBinaryName)) {
            return "Boolean.TRUE.equals(" + modelExpr + ")";
        }
        if (f.kind.kind == PropertyTypeKind.Kind.BOOLEAN) {
            return modelExpr;
        }
        return modelExpr + " != null && Boolean.parseBoolean(String.valueOf(" + modelExpr + "))";
    }

    private static void emitWriteFromString(StringBuilder sb, BoundField f, String src) {
        // Property fields: always through .set() on the Property wrapper.
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
            String elem = f.kind.elementBinaryName;
            String propRead = f.getter != null
                    ? "model." + f.getter + "()"
                    : "model." + f.fieldName;
            if ("java.lang.String".equals(elem)) {
                sb.append("                        ").append(propRead).append(".set(").append(src).append(");\n");
            } else if ("java.lang.Integer".equals(elem)) {
                sb.append("                        try { ").append(propRead).append(".set(Integer.valueOf(").append(src).append(")); } catch (NumberFormatException _nfe) {}\n");
            } else if ("java.lang.Long".equals(elem)) {
                sb.append("                        try { ").append(propRead).append(".set(Long.valueOf(").append(src).append(")); } catch (NumberFormatException _nfe) {}\n");
            } else if ("java.lang.Double".equals(elem)) {
                sb.append("                        try { ").append(propRead).append(".set(Double.valueOf(").append(src).append(")); } catch (NumberFormatException _nfe) {}\n");
            } else {
                sb.append("                        ").append(propRead).append(".set((").append(elem).append(") ").append(src).append(");\n");
            }
            return;
        }
        // Scalars: via setter if resolved, else direct field assignment.
        String assignLhs;
        if (f.setter != null) {
            assignLhs = "model." + f.setter + "(";
        } else {
            assignLhs = "model." + f.fieldName + " = ";
        }
        String suffix = f.setter != null ? ")" : "";

        switch (f.kind.kind) {
            case STRING:
                sb.append("                        ").append(assignLhs).append(src).append(suffix).append(";\n");
                break;
            case INT:
                sb.append("                        try { ").append(assignLhs).append("Integer.parseInt(").append(src).append(")").append(suffix).append("; } catch (NumberFormatException _nfe) {}\n");
                break;
            case LONG:
                sb.append("                        try { ").append(assignLhs).append("Long.parseLong(").append(src).append(")").append(suffix).append("; } catch (NumberFormatException _nfe) {}\n");
                break;
            case DOUBLE:
                sb.append("                        try { ").append(assignLhs).append("Double.parseDouble(").append(src).append(")").append(suffix).append("; } catch (NumberFormatException _nfe) {}\n");
                break;
            case FLOAT:
                sb.append("                        try { ").append(assignLhs).append("Float.parseFloat(").append(src).append(")").append(suffix).append("; } catch (NumberFormatException _nfe) {}\n");
                break;
            default:
                break;
        }
    }

    private static void emitWriteFromBoolean(StringBuilder sb, BoundField f, String src) {
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY
                && "java.lang.Boolean".equals(f.kind.elementBinaryName)) {
            String propRead = f.getter != null
                    ? "model." + f.getter + "()"
                    : "model." + f.fieldName;
            sb.append("                        ").append(propRead).append(".set(Boolean.valueOf(").append(src).append("));\n");
            return;
        }
        if (f.kind.kind != PropertyTypeKind.Kind.BOOLEAN) {
            return;
        }
        if (f.setter != null) {
            sb.append("                        model.").append(f.setter).append("(").append(src).append(");\n");
        } else {
            sb.append("                        model.").append(f.fieldName).append(" = ").append(src).append(";\n");
        }
    }

    private static String simpleName(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String packageOf(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? "" : binary.substring(0, dot);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.toString();
    }

    // ---------------------------------------------------------------
    // Accumulator types
    // ---------------------------------------------------------------

    enum BindAttrName {
        TEXT(true), UIID(false), HIDDEN(false), VISIBLE(false), ENABLED(false),
        SELECTED(true), ICON_NAME(false), NAME(false);

        private final boolean twoWayCapable;

        BindAttrName(boolean twoWayCapable) {
            this.twoWayCapable = twoWayCapable;
        }

        boolean isTwoWayCapable() {
            return twoWayCapable;
        }
    }

    static final class BindableClass {
        String binaryName;
        String internalName;
        File classFile;
        String packageName;
        String simpleName;
        String binderBinaryName;
        String binderSimpleName;
        final List<BoundField> fields = new ArrayList<BoundField>();
    }

    static final class BoundField {
        String fieldName;
        String componentName;
        BindAttrName attr;
        boolean twoWay;
        PropertyTypeKind kind;
        String getter;          // null means direct field access
        String getterDescriptor;
        String setter;          // null means direct field assignment
        String setterDescriptor;
        final List<Validation> validations = new ArrayList<Validation>();
    }

    /// The kind of constraint a validation annotation maps to. Each kind
    /// drives one `case` of the generator switch in `emitConstraintExpr`.
    enum ValidationKind {
        REQUIRED, LENGTH, REGEX, EMAIL, URL, NUMERIC, EXIST_IN, CUSTOM
    }

    /// Parsed view of a single validation annotation occurrence on a
    /// `@Bind` field. Only the fields relevant to the chosen `kind` are
    /// populated; the others stay at their defaults.
    static final class Validation {
        final ValidationKind kind;
        String message = "";
        int intArg;
        boolean boolArg;
        double doubleMin;
        double doubleMax;
        String stringArg;
        String[] stringArrayArg;

        Validation(ValidationKind kind) {
            this.kind = kind;
        }
    }
}
