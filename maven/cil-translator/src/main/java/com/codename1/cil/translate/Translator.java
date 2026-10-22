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
package com.codename1.cil.translate;

import com.codename1.cil.metadata.CilAssembly;
import com.codename1.cil.metadata.CilAssembly.FieldDef;
import com.codename1.cil.metadata.CilAssembly.MethodDef;
import com.codename1.cil.metadata.CilAssembly.MethodImpl;
import com.codename1.cil.metadata.CilAssembly.MethodRef;
import com.codename1.cil.metadata.CilAssembly.MethodSig;
import com.codename1.cil.metadata.CilAssembly.TypeDef;
import com.codename1.cil.metadata.CilType;
import com.codename1.cil.metadata.MethodBody.Instr;
import com.codename1.cil.metadata.MetadataReader;
import com.codename1.cil.metadata.Universe;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// Translates .NET assemblies to JVM classes.
///
/// One class is written per C# class, struct and interface, plus one small
/// class per method a delegate is made from. Enums become the integer under
/// them and have no class. Generics are erased, except for the static fields
/// of a generic type, which [GenericStatics] keeps apart for each constructed
/// type in classes of their own. Classes are Java 5 class
/// files: no stack maps, no `invokedynamic`, nothing a Codename One target
/// has to lower again.
///
///     Translator --out <dir> [--runtime <classes dir>] [--ref <assembly>]... <assembly>...
///
/// `--runtime` names the compiled Java runtime library; with it, every
/// reference into that library is checked to exist.
public final class Translator implements Opcodes {
    final Universe universe = new Universe();
    final RuntimeIndex index = new RuntimeIndex();
    Names names;
    /// Where the static state of generic types lives: see [GenericStatics].
    final GenericStatics generics = new GenericStatics(this);
    private final Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>();
    private final Map<String, String> delegateClasses = new HashMap<String, String>();
    private String entryClass;
    private final List<String> warnings = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        File out = null;
        List<File> translate = new ArrayList<File>();
        List<File> references = new ArrayList<File>();
        Translator t = new Translator();
        for (int i = 0; i < args.length; i++) {
            if ("--out".equals(args[i])) {
                out = new File(args[++i]);
            } else if ("--ref".equals(args[i])) {
                references.add(new File(args[++i]));
            } else if ("--runtime".equals(args[i])) {
                t.index.addDirectory(new File(args[++i]));
            } else {
                translate.add(new File(args[i]));
            }
        }
        if (out == null || translate.isEmpty()) {
            System.err.println("usage: Translator --out <dir> [--runtime <classes dir>] [--ref <assembly>]..."
                    + " <assembly>...");
            System.exit(2);
        }
        try {
            t.run(translate, references);
        } catch (TranslationException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
        for (Map.Entry<String, byte[]> e : t.classes.entrySet()) {
            File f = new File(out, e.getKey() + ".class");
            Files.createDirectories(f.getParentFile().toPath());
            Files.write(f.toPath(), e.getValue());
        }
        System.out.println(t.classes.size() + " classes" + (t.entryClass == null ? ""
                : ", entry point " + t.entryClass.replace('/', '.')));
        for (String w : t.warnings) {
            System.err.println("warning: " + w);
        }
        if (t.index.missingCount() > 0) {
            System.err.println("error: the runtime library lacks " + t.index.missingCount()
                    + " members the code uses:");
            for (String m : t.index.missing()) {
                System.err.println("  " + m);
            }
            System.exit(1);
        }
    }

    /// What translated and will not behave as it does in Unity, a line
    /// each.
    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    /// The members of the runtime library the code uses and the library
    /// lacks: a line for each, then an indented line for each use.
    public List<String> missingMembers() {
        return index.missing();
    }

    public Map<String, byte[]> classes() {
        return Collections.unmodifiableMap(classes);
    }

    void putClass(String name, byte[] bytes) {
        classes.put(name, bytes);
    }

    /// Stack: nothing. Leaves the `System.Type` of a type: the enum's own,
    /// or one made from the class.
    void emitTypeOf(MethodVisitor mv, CilType type, String neededFor) {
        if (hasEnumType(type)) {
            mv.visitFieldInsn(GETSTATIC, names.className(type.name), "$TYPE", TYPE_DESCRIPTOR);
            return;
        }
        CilType n = names.norm(type);
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(classToken(n)));
        String constructed = n.kind == CilType.Kind.GENERICINST ? dotnetName(n) : null;
        mv.visitLdcInsn(constructed != null ? constructed
                : String.valueOf(n.typeName() == null ? n.toString() : n.typeName()).replace('/', '+'));
        String descriptor = "(Ljava/lang/Class;Ljava/lang/String;)" + TYPE_DESCRIPTOR;
        index.requireMethod(TYPE, "$of", descriptor, neededFor);
        mv.visitMethodInsn(INVOKESTATIC, TYPE, "$of", descriptor, false);
    }

    /// The name .NET's `Type.ToString()` gives a type, which for a
    /// constructed type lists its arguments: ``System.Collections.Generic.List`1[System.Int32]``,
    /// ``G`1+N[System.Int32]`` for a type nested in a generic one. It is
    /// the identity of the type at run time too (`Type.$of` keeps one
    /// object per name), so `typeof(List<int>) != typeof(List<string>)`
    /// although erasure makes both one class.
    ///
    /// Null when the type names a type variable: `typeof(List<T>)` in code
    /// with no context to ask (see GenericStatics) is known only as its
    /// definition, as it was before arguments were part of the name.
    static String dotnetName(CilType type) {
        switch (type.kind) {
            case VAR:
            case MVAR:
                return null;
            case SZARRAY: {
                String e = dotnetName(type.element);
                return e == null ? null : e + "[]";
            }
            case ARRAY: {
                String e = dotnetName(type.element);
                if (e == null) {
                    return null;
                }
                StringBuilder sb = new StringBuilder(e).append('[');
                for (int i = 1; i < type.index; i++) {
                    sb.append(',');
                }
                return sb.append(']').toString();
            }
            case GENERICINST: {
                StringBuilder sb = new StringBuilder(type.element.name.replace('/', '+')).append('[');
                for (int i = 0; i < type.args.length; i++) {
                    String a = dotnetName(type.args[i]);
                    if (a == null) {
                        return null;
                    }
                    sb.append(i > 0 ? "," : "").append(a);
                }
                return sb.append(']').toString();
            }
            default: {
                String name = type.typeName();
                return name == null ? null : name.replace('/', '+');
            }
        }
    }

    /// The class a type is known by at run time, to `typeof` and to a
    /// generic method of the runtime: a primitive or an enum is the class
    /// it is boxed as, and a type variable nobody can name is `Object`.
    String classToken(CilType type) {
        CilType n = names.norm(type);
        if (Names.isTypeVariable(n) || n.kind == CilType.Kind.OBJECT) {
            return Names.OBJECT;
        }
        return n.isPrimitive() ? Names.boxClass(n) : names.classRef(n);
    }

    public void run(List<File> translate, List<File> references) throws IOException {
        List<File> all = new ArrayList<File>(translate);
        all.addAll(references);
        List<CilAssembly> loaded = universe.load(all);
        Set<CilAssembly> own = new HashSet<CilAssembly>(loaded.subList(0, translate.size()));
        names = new Names(universe, own);
        for (CilAssembly assembly : loaded.subList(0, translate.size())) {
            MethodDef entry = assembly.entryPoint();
            for (TypeDef type : assembly.types()) {
                emitType(type, entry);
            }
        }
    }

    // ------------------------------------------------------------------ types

    private void emitType(TypeDef type, MethodDef entry) {
        String full = type.fullName();
        if ("<Module>".equals(full) || type.name.startsWith("__StaticArrayInitTypeSize")) {
            return;
        }
        if (type.isEnum()) {
            emitEnum(type);
            return;
        }
        if (type.isDelegate()) {
            emitDelegateType(type);
            return;
        }
        String name = names.className(full);
        boolean struct = type.isValueType();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String superName = type.isInterface() || struct || type.baseType == null ? Names.OBJECT
                : names.classRef(type.baseType);
        List<String> interfaces = new ArrayList<String>();
        for (CilType i : type.interfaces) {
            String c = names.classRef(i);
            if (!interfaces.contains(c)) {
                interfaces.add(c);
                index.requireClass(c, full);
            }
        }
        index.requireClass(superName, full);
        if (struct && !interfaces.contains(Names.STRUCT)) {
            interfaces.add(Names.STRUCT);
            index.requireClass(Names.STRUCT, full);
        }
        int access = ACC_PUBLIC | (type.isInterface() ? ACC_INTERFACE | ACC_ABSTRACT : ACC_SUPER)
                | (type.isAbstract() ? ACC_ABSTRACT : 0) | (struct ? ACC_FINAL : 0);
        cw.visit(V1_5, access, name, null, superName, interfaces.toArray(new String[0]));

        boolean keyed = generics.hasContext(type);
        if (!keyed && type.isInterface() && type.genericParamCount > 0) {
            for (FieldDef f : type.fields) {
                if (f.isStatic() && !f.isLiteral()) {
                    throw new TranslationException(full + "." + f.name + ": a static field of a generic interface"
                            + " is not supported; every constructed type has its own in C#");
                }
            }
        }
        Set<String> members = new HashSet<String>();
        for (FieldDef f : type.fields) {
            if (f.isLiteral() || f.rva != 0) {
                continue;
            }
            String fieldName = Names.sanitize(f.name);
            if (!members.add(fieldName)) {
                throw new TranslationException(full + ": two fields are both named " + fieldName
                        + " once their names are made legal");
            }
            if (f.isStatic() && keyed) {
                // Not a Java static: one per constructed type, in a context.
                continue;
            }
            cw.visitField(ACC_PUBLIC | (f.isStatic() ? ACC_STATIC : 0), fieldName, names.descriptor(f.type), null,
                    null).visitEnd();
        }
        if (keyed && !struct) {
            // What a constructor is handed and the instance methods read.
            cw.visitField(ACC_PUBLIC, generics.contextField(type), "L" + generics.contextClass(type) + ";", null,
                    null).visitEnd();
        }

        boolean hasStaticConstructor = false;
        boolean[] objectVirtuals = new boolean[3];
        for (MethodDef m : type.methods) {
            String methodName = names.methodName(m.name, m.sig, struct) + names.hidingSuffix(m);
            String descriptor = GenericStatics.withHidden(names.methodDescriptor(m.sig),
                    generics.hiddenParameters(m));
            if (generics.hasContext(m)) {
                generics.emitContextClass(m);
            }
            if (keyed && "<clinit>".equals(methodName)) {
                // Run once for each constructed type, by the class that
                // holds that type's context.
                methodName = GenericStatics.STATIC_CONSTRUCTOR;
            }
            if (!members.add(methodName + descriptor)) {
                throw new TranslationException(m + ": another method of the type has the same name and"
                        + " parameters once generics are erased (" + methodName + descriptor + ")");
            }
            String objectVirtual = Names.objectVirtual(m.name, m.sig);
            if (objectVirtual != null) {
                objectVirtuals["toString".equals(objectVirtual) ? 0 : "equals".equals(objectVirtual) ? 1 : 2] = true;
            }
            if ("<clinit>".equals(methodName)) {
                hasStaticConstructor = true;
                MethodVisitor mv = cw.visitMethod(ACC_STATIC, methodName, descriptor, null, null);
                translateBody(type, m, mv, false);
                continue;
            }
            int methodAccess = ACC_PUBLIC | (m.isStatic() ? ACC_STATIC : 0) | (m.isAbstract() ? ACC_ABSTRACT : 0);
            MethodVisitor mv = cw.visitMethod(methodAccess, methodName, descriptor, null, null);
            if (m.isAbstract()) {
                mv.visitEnd();
            } else if (!m.hasBody() || type.isInterface()) {
                throw new TranslationException(m + ": a method with no IL body, or a method with a body in an"
                        + " interface, is not supported");
            } else if (isAutoEventAccessor(m)) {
                emitEventAccessor(type, m, mv);
            } else {
                translateBody(type, m, mv, false);
                if (struct && "$ctor".equals(methodName)) {
                    // The same constructor with the object last: see
                    // MethodTranslator.factory.
                    String self = "L" + name + ";";
                    MethodVisitor factory = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "$new",
                            descriptor.substring(0, descriptor.length() - 2) + self + ")" + self, null, null);
                    translateBody(type, m, factory, true);
                }
            }
        }

        emitMessageHooks(cw, type, name);
        emitScriptSupport(cw, type, name, superName);
        emitSerializableSupport(cw, type, name);
        if (struct) {
            emitStructMembers(cw, type, name, objectVirtuals);
        }
        if (keyed) {
            generics.emitContextClass(type);
        } else if (!hasStaticConstructor && hasStructField(type, true)) {
            MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            emitStaticStructFields(type, mv);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if (!type.isInterface() && !objectVirtuals[0] && inheritsDefaultToString(type)) {
            // What Object.ToString() answers on .NET: the name of the type.
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitLdcInsn(full.replace('/', '+'));
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if (!type.isInterface()) {
            emitBridges(cw, type, name, members);
        }
        if (entry != null && entry.owner == type) { // NOPMD CompareObjectsWithEquals
            emitMain(cw, name, entry);
            entryClass = name;
        }
        cw.visitEnd();
        classes.put(name, cw.toByteArray());
    }

    private boolean inheritsDefaultToString(TypeDef type) {
        CilType base = type.baseType;
        while (base != null) {
            String baseName = base.typeName();
            if ("System.Object".equals(baseName) || "System.ValueType".equals(baseName)) {
                return true;
            }
            TypeDef def = universe.find(baseName);
            if (def == null || !names.isTranslated(baseName)) {
                return false;
            }
            for (MethodDef m : def.methods) {
                if ("toString".equals(Names.objectVirtual(m.name, m.sig))) {
                    return false;
                }
            }
            base = def.baseType;
        }
        return true;
    }

    private boolean hasStructField(TypeDef type, boolean isStatic) {
        for (FieldDef f : type.fields) {
            if (f.isStatic() == isStatic && !f.isLiteral() && f.rva == 0 && names.isStruct(f.type)) {
                return true;
            }
        }
        return false;
    }

    private void emitStructFields(TypeDef type, MethodVisitor mv, boolean isStatic) {
        String owner = names.className(type.fullName());
        for (FieldDef f : type.fields) {
            if (f.isStatic() != isStatic || f.isLiteral() || f.rva != 0 || !names.isStruct(f.type)) {
                continue;
            }
            String c = names.classRef(f.type);
            if (!isStatic) {
                mv.visitVarInsn(ALOAD, 0);
            }
            index.requireMethod(c, "<init>", "()V", type.fullName());
            mv.visitTypeInsn(NEW, c);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, c, "<init>", "()V", false);
            if (tupleArity(f.type) > 0) {
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEVIRTUAL, c, "$defaults", pushTupleDefaults(mv, f.type) + "V", false);
            }
            mv.visitFieldInsn(isStatic ? PUTSTATIC : PUTFIELD, owner, Names.sanitize(f.name),
                    names.descriptor(f.type));
        }
    }

    /// A field of struct type always holds an object, the way it always holds
    /// a value in C#: every constructor allocates them before anything else.
    void emitInstanceStructFields(TypeDef type, MethodVisitor mv) {
        emitStructFields(type, mv, false);
    }

    void emitStaticStructFields(TypeDef type, MethodVisitor mv) {
        emitStructFields(type, mv, true);
    }

    // ---------------------------------------------------------------- structs

    private List<FieldDef> instanceFields(TypeDef type) {
        List<FieldDef> out = new ArrayList<FieldDef>();
        for (FieldDef f : type.fields) {
            if (!f.isStatic()) {
                out.add(f);
            }
        }
        return out;
    }

    /// What makes a class behave as a value: a no-argument constructor that
    /// yields the default value, a field-wise copy, a field-wise assignment
    /// into an existing instance, a reset to the default, an array factory
    /// that fills every element, and value equality.
    ///
    /// The copy is written out because `Object.clone()` answers null on
    /// ParparVM.
    private void emitStructMembers(ClassWriter cw, TypeDef type, String name, boolean[] objectVirtuals) {
        List<FieldDef> fields = instanceFields(type);
        String self = "L" + name + ";";

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, Names.OBJECT, "<init>", "()V", false);
        emitInstanceStructFields(type, mv);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "$copy", "()" + self, null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, name);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, name, "$assign", "(" + self + ")V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // The copy again, for runtime code that knows only that this is a
        // struct: `Array.Clone` of an array of them.
        mv = cw.visitMethod(ACC_PUBLIC, "$copyValue", "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, name, "$copy", "()" + self, false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "$assign", "(" + self + ")V", null, null);
        mv.visitCode();
        for (FieldDef f : fields) {
            String fieldName = Names.sanitize(f.name);
            String d = names.descriptor(f.type);
            mv.visitVarInsn(ALOAD, 0);
            if (names.isStruct(f.type)) {
                String c = names.classRef(f.type);
                mv.visitFieldInsn(GETFIELD, name, fieldName, d);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(GETFIELD, name, fieldName, d);
                mv.visitMethodInsn(INVOKEVIRTUAL, c, "$assign", "(L" + c + ";)V", false);
            } else {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(GETFIELD, name, fieldName, d);
                mv.visitFieldInsn(PUTFIELD, name, fieldName, d);
            }
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "$clear", "()V", null, null);
        mv.visitCode();
        for (FieldDef f : fields) {
            String fieldName = Names.sanitize(f.name);
            String d = names.descriptor(f.type);
            mv.visitVarInsn(ALOAD, 0);
            if (names.isStruct(f.type)) {
                String c = names.classRef(f.type);
                mv.visitFieldInsn(GETFIELD, name, fieldName, d);
                mv.visitMethodInsn(INVOKEVIRTUAL, c, "$clear", "()V", false);
            } else {
                Jvm.pushDefault(mv, names.kind(f.type));
                mv.visitFieldInsn(PUTFIELD, name, fieldName, d);
            }
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // array[index] = value, as one call: the element is copied into.
        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "$store", "([" + self + "I" + self + ")V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, name, "$assign", "(" + self + ")V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "$newArray", "(I)[" + self, null, null);
        mv.visitCode();
        Label loop = new Label();
        Label done = new Label();
        mv.visitVarInsn(ILOAD, 0);
        mv.visitTypeInsn(ANEWARRAY, name);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitLabel(loop);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitJumpInsn(IF_ICMPGE, done);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitTypeInsn(NEW, name);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
        mv.visitInsn(AASTORE);
        mv.visitIincInsn(2, 1);
        mv.visitJumpInsn(GOTO, loop);
        mv.visitLabel(done);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        if (!objectVirtuals[1]) {
            mv = cw.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
            mv.visitCode();
            Label no = new Label();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(INSTANCEOF, name);
            mv.visitJumpInsn(IFEQ, no);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, name);
            mv.visitVarInsn(ASTORE, 2);
            for (FieldDef f : fields) {
                String fieldName = Names.sanitize(f.name);
                String d = names.descriptor(f.type);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, name, fieldName, d);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitFieldInsn(GETFIELD, name, fieldName, d);
                switch (names.kind(f.type)) {
                    case Val.I4:
                        mv.visitJumpInsn(IF_ICMPNE, no);
                        break;
                    case Val.I8:
                        mv.visitInsn(LCMP);
                        mv.visitJumpInsn(IFNE, no);
                        break;
                    case Val.R4:
                    case Val.R8: {
                        // `ValueType.Equals` compares a field with the
                        // field's own `Equals`, and `float.Equals` is not
                        // `==`: NaN equals NaN, or a struct holding one
                        // would not equal itself and could never be found
                        // again as a dictionary key. Zero and negative zero
                        // are equal either way. The comparison says "equal"
                        // for every pair but one with a NaN in it, so the
                        // two extra tests run only for those: the fields
                        // are equal after all when neither equals itself.
                        boolean wide = names.kind(f.type) == Val.R8;
                        Label same = new Label();
                        mv.visitInsn(wide ? DCMPL : FCMPL);
                        mv.visitJumpInsn(IFEQ, same);
                        for (int side = 0; side < 3; side += 2) {
                            mv.visitVarInsn(ALOAD, side);
                            mv.visitFieldInsn(GETFIELD, name, fieldName, d);
                            mv.visitInsn(wide ? DUP2 : DUP);
                            mv.visitInsn(wide ? DCMPL : FCMPL);
                            mv.visitJumpInsn(IFEQ, no);
                        }
                        mv.visitLabel(same);
                        break;
                    }
                    default:
                        mv.visitMethodInsn(INVOKESTATIC, Names.INTEROP, "areEqual",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                        mv.visitJumpInsn(IFEQ, no);
                        break;
                }
            }
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
            mv.visitLabel(no);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if (!objectVirtuals[2]) {
            mv = cw.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
            mv.visitCode();
            // Equal values have to hash alike, and the conversion to an
            // integer below gives the two zeros one hash and every NaN
            // another (it answers 0 for a NaN), which is what `equals`
            // above calls equal.
            Jvm.pushInt(mv, 17);
            for (FieldDef f : fields) {
                Jvm.pushInt(mv, 31);
                mv.visitInsn(IMUL);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, name, Names.sanitize(f.name), names.descriptor(f.type));
                switch (names.kind(f.type)) {
                    case Val.I4:
                        break;
                    case Val.I8:
                        mv.visitInsn(L2I);
                        break;
                    case Val.R4:
                        mv.visitInsn(F2I);
                        break;
                    case Val.R8:
                        mv.visitInsn(D2I);
                        break;
                    default:
                        mv.visitMethodInsn(INVOKESTATIC, Names.INTEROP, "hash", "(Ljava/lang/Object;)I", false);
                        break;
                }
                mv.visitInsn(IADD);
            }
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    /// Translates a method body twice. The first pass writes nowhere and only
    /// reports the scratch structs the body needs; the second emits, and can
    /// therefore allocate them all before the first instruction.
    private void translateBody(TypeDef type, MethodDef m, MethodVisitor mv, boolean factory) {
        MethodTranslator probe = new MethodTranslator(this, type, m, new DiscardingVisitor(), null, factory);
        probe.translate();
        new MethodTranslator(this, type, m, mv, probe.tempsFound(), factory).translate();
    }

    /// Where the first pass over a method body writes: nowhere.
    private static final class DiscardingVisitor extends MethodVisitor {
        DiscardingVisitor() {
            super(ASM9);
        }
    }

    // ---------------------------------------------------------- Unity messages

    /// The methods Unity calls on a script by name.
    private static final String[] MESSAGES = {
        "Awake", "Start", "Update", "FixedUpdate", "LateUpdate", "OnEnable", "OnDisable", "OnDestroy",
        "OnCollisionEnter2D", "OnCollisionStay2D", "OnCollisionExit2D", "OnTriggerEnter2D", "OnTriggerStay2D",
        "OnTriggerExit2D", "OnMouseDown", "OnMouseUp", "OnApplicationPause", "OnApplicationFocus",
    };

    /// The rest of what Unity's documentation of `MonoBehaviour` lists
    /// under "Messages": methods Unity would call and this runtime never
    /// does. A script that declares one compiles and translates, and then
    /// waits for a call that does not come, so each is reported.
    ///
    /// Left out are the four only the editor sends -- `OnDrawGizmos`,
    /// `OnDrawGizmosSelected`, `OnValidate` and `Reset` -- which a built
    /// Unity player never calls either.
    private static final String[] UNSENT_MESSAGES = {
        "OnAnimatorIK", "OnAnimatorMove", "OnApplicationQuit", "OnAudioFilterRead", "OnBecameInvisible",
        "OnBecameVisible", "OnCollisionEnter", "OnCollisionExit", "OnCollisionStay", "OnConnectedToServer",
        "OnControllerColliderHit", "OnDisconnectedFromServer", "OnFailedToConnect",
        "OnFailedToConnectToMasterServer", "OnGUI", "OnJointBreak", "OnJointBreak2D", "OnMasterServerEvent",
        "OnMouseDrag", "OnMouseEnter", "OnMouseExit", "OnMouseOver", "OnMouseUpAsButton", "OnNetworkInstantiate",
        "OnParticleCollision", "OnParticleSystemStopped", "OnParticleTrigger", "OnParticleUpdateJobScheduled",
        "OnPlayerConnected", "OnPlayerDisconnected", "OnPostRender", "OnPreCull", "OnPreRender", "OnRenderImage",
        "OnRenderObject", "OnSerializeNetworkView", "OnServerInitialized", "OnTransformChildrenChanged",
        "OnTransformParentChanged", "OnTriggerEnter", "OnTriggerExit", "OnTriggerStay", "OnWillRenderObject",
    };

    /// Says that a script declares a message nobody sends: the class, the
    /// method and, when the assembly came with its PDB, the line.
    private void warnUnsent(TypeDef type, MethodDef m) {
        String source = m.hasBody() ? type.assembly.sourceLocation(m, 0) : null;
        warnings.add(type.fullName() + "." + m.name + (source == null ? "" : " (" + source + ")")
                + ": Unity calls this method by its name and the compatibility runtime does not send the "
                + m.name + " message; the method is never called");
    }

    /// Unity has no `Update` to override: it looks a script's methods up by
    /// name, private ones included. The runtime's `MonoBehaviour` declares a
    /// do-nothing `$update` instead, and a script that has an `Update` gets
    /// an override of it here that calls the real one. The player loop then
    /// makes a virtual call and nothing is searched for at run time.
    ///
    /// A `Start` that is an iterator is a coroutine, and is started as one.
    private void emitMessageHooks(ClassWriter cw, TypeDef type, String name) {
        String full = type.fullName();
        if ("UnityEngine.MonoBehaviour".equals(full) || !universe.derivesFrom(full, "UnityEngine.MonoBehaviour")) {
            return;
        }
        String base = Names.RUNTIME + "unityengine/MonoBehaviour";
        for (MethodDef m : type.methods) {
            if (m.isStatic() || m.sig.genericParamCount > 0) {
                continue;
            }
            String message = null;
            for (String candidate : MESSAGES) {
                if (candidate.equals(m.name)) {
                    message = candidate;
                }
            }
            if (message == null) {
                for (String unsent : UNSENT_MESSAGES) {
                    if (unsent.equals(m.name)) {
                        warnUnsent(type, m);
                    }
                }
                continue;
            }
            MethodSig sig = m.sig;
            String hook = "$" + Character.toLowerCase(message.charAt(0)) + message.substring(1);
            String descriptor = names.methodDescriptor(sig);
            String hookDescriptor = descriptor.substring(0, descriptor.lastIndexOf(')') + 1) + "V";
            index.requireMethod(base, hook, hookDescriptor, "the Unity message " + m);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, hook, hookDescriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            if (sig.returnType.kind != CilType.Kind.VOID) {
                // Its result is the coroutine to run.
                mv.visitInsn(DUP);
            }
            int slot = 1;
            for (CilType p : sig.params) {
                int k = names.kind(p);
                mv.visitVarInsn(Jvm.loadOp(k), slot);
                slot += k == Val.I8 || k == Val.R8 ? 2 : 1;
            }
            mv.visitMethodInsn(INVOKEVIRTUAL, name, Names.sanitize(m.name), descriptor, false);
            if (sig.returnType.kind != CilType.Kind.VOID) {
                String enumerator = Names.RUNTIME + "system/collections/IEnumerator";
                if (!("L" + enumerator + ";").equals(names.descriptor(sig.returnType))) {
                    throw new TranslationException(m + ": a Unity message must return void or IEnumerator");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, base, "StartCoroutine",
                        "(L" + enumerator + ";)L" + Names.RUNTIME + "unityengine/Coroutine;", false);
                mv.visitInsn(POP);
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    // ------------------------------------------------- copying and invoking

    private static final int FIELD_ACCESS_MASK = 0x7;
    private static final int FIELD_PUBLIC = 0x6;
    private static final int FIELD_INIT_ONLY = 0x20;
    private static final int FIELD_NOT_SERIALIZED = 0x80;
    private static final int TYPE_SERIALIZABLE = 0x2000;

    /// Whether Unity serializes a field, which decides what `Instantiate`
    /// copies: an instance field that is public, or marked
    /// `[SerializeField]`, and neither `readonly` nor `[NonSerialized]`, nor
    /// a container of containers (`int[][]`, `List<int[]>`), which Unity
    /// does not serialize.
    public static boolean isSerialized(TypeDef type, FieldDef f) {
        if (f.isStatic() || f.isLiteral() || f.rva != 0 || (f.flags & (FIELD_INIT_ONLY | FIELD_NOT_SERIALIZED)) != 0) {
            return false;
        }
        CilType inner = containerElement(f.type);
        if (inner != null && containerElement(inner) != null) {
            return false;
        }
        return (f.flags & FIELD_ACCESS_MASK) == FIELD_PUBLIC
                || type.assembly.attributes(f.token()).contains("UnityEngine.SerializeField");
    }

    /// The element type of an array or a `List<T>`, or null for anything else.
    private static CilType containerElement(CilType t) {
        if (t.kind == CilType.Kind.SZARRAY) {
            return t.element;
        }
        if (t.kind == CilType.Kind.GENERICINST && t.args != null && t.args.length == 1 && t.element != null
                && "System.Collections.Generic.List`1".equals(t.element.name)) {
            return t.args[0];
        }
        return null;
    }

    /// What the runtime needs of a script that reflection would give it
    /// elsewhere, and that a Codename One device does not have.
    ///
    /// - `$new()` makes another script of the same class, for `Instantiate`
    ///   of an object already in the scene and for `AddComponent`.
    /// - `$copyFrom(source)` copies the serialized fields from another
    ///   instance, which is what `Instantiate` means by a copy. A struct is
    ///   copied, and what Unity serializes inside it copied as a class's
    ///   is (see [#copiesSerializedStruct]), an array is duplicated, a serializable class is copied
    ///   field by field (see [#emitSerializableSupport]), and a reference
    ///   to a Unity object is passed through the runtime, which redirects
    ///   one that points into the tree being copied to the copy. A field
    ///   of a type Unity
    ///   does not serialize -- a dictionary, a delegate -- is left as the
    ///   constructor set it, as Unity leaves it.
    /// - `$invoke(name)` calls a method without parameters by its name, for
    ///   `Invoke` and `InvokeRepeating`.
    ///
    /// Each handles the members its own class declares and defers to its
    /// base for the rest.
    private void emitScriptSupport(ClassWriter cw, TypeDef type, String name, String superName) {
        String full = type.fullName();
        if (type.isInterface() || type.genericParamCount > 0 || "UnityEngine.MonoBehaviour".equals(full)
                || !universe.derivesFrom(full, "UnityEngine.MonoBehaviour")) {
            return;
        }
        String component = Names.RUNTIME + "unityengine/Component";
        String why = "copying the script " + full;
        if (!type.isAbstract() && hasDefaultConstructor(type)) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "$new", "()L" + component + ";", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, name);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        String copyDescriptor = "(L" + component + ";)V";
        index.requireMethod(component, "$copyFrom", copyDescriptor, why);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "$copyFrom", copyDescriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, superName, "$copyFrom", copyDescriptor, false);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, name);
        mv.visitVarInsn(ASTORE, 2);
        emitSerializedCopies(mv, type, name, 2, -1, why);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        String invokeDescriptor = "(Ljava/lang/String;)Z";
        mv = cw.visitMethod(ACC_PUBLIC, "$invoke", invokeDescriptor, null, null);
        mv.visitCode();
        for (MethodDef m : type.methods) {
            if (m.isStatic() || m.isAbstract() || m.sig.params.length != 0 || m.sig.genericParamCount > 0
                    || m.name.startsWith(".") || m.sig.returnType.kind != CilType.Kind.VOID) {
                continue;
            }
            Label next = new Label();
            mv.visitLdcInsn(m.name);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, Names.STRING, "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFEQ, next);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, names.methodName(m.name, m.sig, false) + names.hidingSuffix(m),
                    names.methodDescriptor(m.sig), false);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
            mv.visitLabel(next);
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, superName, "$invoke", invokeDescriptor, false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /// How many serializable classes inside one another a copy follows
    /// before it leaves a field null. Unity's serializer stops at ten
    /// levels too, which is what ends a class that holds one of its own
    /// kind and, here, a cycle of them: without a limit that is a stack
    /// overflow, and on a device no exception.
    private static final int SERIALIZED_DEPTH = 10;

    private static final String LIST = Names.RUNTIME + "system/collections/generic/List_1";

    /// Whether a class gets a `$copySerialized`: a `[Serializable]` class
    /// of the application's own that is not a Unity object. The classes the
    /// C# compiler writes for lambdas are marked serializable as well and
    /// are named with a `<`; nothing can declare a field of one.
    private boolean copiesSerialized(TypeDef def) {
        return def != null && (def.flags & TYPE_SERIALIZABLE) != 0 && !def.isInterface() && !def.isValueType()
                && !def.isEnum() && !def.isDelegate() && def.genericParamCount == 0 && def.name.indexOf('<') < 0
                && names.isTranslated(def.fullName())
                && !universe.derivesFrom(def.fullName(), "UnityEngine.Object");
    }

    private static boolean hasDefaultConstructor(TypeDef def) {
        for (MethodDef m : def.methods) {
            if (".ctor".equals(m.name) && m.sig.params.length == 0) {
                return true;
            }
        }
        return false;
    }

    /// The class of a field Unity serializes inline, as part of the object
    /// that holds it, and that `Instantiate` therefore gives each copy an
    /// instance of its own; or null. An abstract class cannot be made. One
    /// with only constructors that take arguments is made through the blank
    /// constructor (see #hasBlankConstructor); a class that cannot have one
    /// either is shared by the copy, as every such field once was.
    private TypeDef inlineClass(CilType t) {
        if (t == null || t.kind != CilType.Kind.CLASS) {
            return null;
        }
        TypeDef def = universe.find(t.name);
        return copiesSerialized(def) && !def.isAbstract() && (hasDefaultConstructor(def) || hasBlankConstructor(def))
                ? def : null;
    }

    /// The marker type of the blank constructor's one parameter, which no
    /// C# constructor can take: it is a runtime class of no C# name.
    static final String BLANK = Names.RUNTIME + "system/Blank";

    /// Whether a serializable class gets `<init>(Blank)`: a constructor that
    /// runs none of the class's own code and only allocates its struct
    /// fields, for `$instantiate` to make a copy with when the class
    /// declares no constructor that takes nothing. Unity's serializer
    /// writes such a class inline all the same, and every serialized field
    /// is copied into the instance right after. It chains to the base
    /// class's blank constructor, or to the constructor that takes nothing
    /// of a base that is not serializable; a base with neither has none.
    private boolean hasBlankConstructor(TypeDef def) {
        if (!copiesSerialized(def)) {
            return false;
        }
        if (def.baseType == null || "System.Object".equals(def.baseType.name)) {
            return true;
        }
        TypeDef base = universe.definitionOf(def.baseType);
        if (base == null || base.genericParamCount > 0) {
            return false;
        }
        return copiesSerialized(base) ? hasBlankConstructor(base) : hasDefaultConstructor(base);
    }

    private void emitBlankConstructor(ClassWriter cw, TypeDef type) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(L" + BLANK + ";)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        TypeDef base = type.baseType == null ? null : universe.definitionOf(type.baseType);
        String b = type.baseType == null ? Names.OBJECT : names.classRef(type.baseType);
        if (copiesSerialized(base)) {
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESPECIAL, b, "<init>", "(L" + BLANK + ";)V", false);
        } else {
            mv.visitMethodInsn(INVOKESPECIAL, b, "<init>", "()V", false);
        }
        emitInstanceStructFields(type, mv);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /// Whether a struct gets a `$copySerialized` and an `$instantiate`: a
    /// `[Serializable]` struct of the application's own that holds, among
    /// the fields Unity serializes, something `$copy` alone would get wrong
    /// in a copy made by `Instantiate`. Unity serializes such a struct into
    /// the object that holds it, like a serializable class, so a reference
    /// it holds to a Unity object inside the tree being copied has to lead
    /// to the copy, and a serializable class or an array in it has to be
    /// the copy's own. `$copy` is a plain field-by-field copy and did
    /// neither: a `Link { Transform target; }` field of a prefab's script
    /// went on pointing into the prefab after `Instantiate`. A struct of
    /// numbers alone -- a `Vector3`, most structs -- is copied as before.
    private boolean copiesSerializedStruct(TypeDef def) {
        return def != null && (def.flags & TYPE_SERIALIZABLE) != 0 && def.isValueType() && !def.isEnum()
                && def.genericParamCount == 0 && def.name.indexOf('<') < 0 && names.isTranslated(def.fullName())
                && holdsDeepSerialized(def, 0);
    }

    /// Whether a serialized field of `def` needs more than a copy of its
    /// bits. A struct cannot hold itself, so the walk ends; the level is a
    /// guard all the same.
    private boolean holdsDeepSerialized(TypeDef def, int level) {
        if (level > SERIALIZED_DEPTH) {
            return false;
        }
        for (FieldDef f : def.fields) {
            if (!isSerialized(def, f)) {
                continue;
            }
            CilType t = f.type;
            if (t.kind == CilType.Kind.VALUETYPE && names.isStruct(t)) {
                TypeDef inner = universe.find(t.name);
                if (inner != null && (inner.flags & TYPE_SERIALIZABLE) != 0 && inner.genericParamCount == 0
                        && names.isTranslated(inner.fullName()) && holdsDeepSerialized(inner, level + 1)) {
                    return true;
                }
            } else if (names.kind(t) == Val.REF && t.kind != CilType.Kind.STRING && !names.isStruct(t)) {
                if (t.kind == CilType.Kind.SZARRAY || t.kind == CilType.Kind.GENERICINST) {
                    return true;
                }
                if (t.kind == CilType.Kind.CLASS) {
                    TypeDef c = universe.find(t.name);
                    if (universe.derivesFrom(t.name, "UnityEngine.Object")
                            || (c != null && (c.flags & TYPE_SERIALIZABLE) != 0)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// The struct of a field, or of the elements of an array or a list,
    /// whose copy goes through its own `$instantiate`; or null.
    private TypeDef inlineStruct(CilType t) {
        if (t == null || t.kind != CilType.Kind.VALUETYPE) {
            return null;
        }
        TypeDef def = universe.find(t.name);
        return copiesSerializedStruct(def) ? def : null;
    }

    /// What `Instantiate` needs of a `[Serializable]` class that is not a
    /// Unity object. Unity writes such a class into the object whose field
    /// holds it, by value, so a copy of that object has an instance of its
    /// own and changing one does not change the other. With no reflection
    /// to walk the fields, the copy is written out here:
    ///
    /// - `$copySerialized(source, depth)` copies the serialized fields its
    ///   class declares, after its base class has copied its own if that is
    ///   serializable too. The descriptor names the class, so one in a
    ///   derived class is another method and not an override.
    /// - `$instantiate(source, depth)` makes the copy: null for null, and
    ///   an instance of the class the *field* was declared as, whatever
    ///   the source was, which is as much of it as Unity writes.
    private void emitSerializableSupport(ClassWriter cw, TypeDef type, String name) {
        String self = "L" + name + ";";
        if (copiesSerializedStruct(type)) {
            // A struct (see copiesSerializedStruct): its bits by `$copy`,
            // then what Unity serializes in it copied again, as a class's
            // are. Never null, and never cut off by the depth: a struct is a
            // value, and only what it holds by reference can run in a cycle.
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "$copySerialized", "(" + self + "I)V", null, null);
            mv.visitCode();
            emitSerializedCopies(mv, type, name, 1, 2, "copying the serializable struct " + type.fullName());
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "$instantiate", "(" + self + "I)" + self, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, "$copy", "()" + self, false);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, name, "$copySerialized", "(" + self + "I)V", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return;
        }
        if (!copiesSerialized(type)) {
            return;
        }
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "$copySerialized", "(" + self + "I)V", null, null);
        mv.visitCode();
        TypeDef base = type.baseType == null ? null : universe.definitionOf(type.baseType);
        if (copiesSerialized(base)) {
            String b = names.classRef(type.baseType);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, b, "$copySerialized", "(L" + b + ";I)V", false);
        }
        emitSerializedCopies(mv, type, name, 1, 2, "copying the serializable class " + type.fullName());
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        boolean blank = !hasDefaultConstructor(type) && hasBlankConstructor(type);
        if (blank) {
            emitBlankConstructor(cw, type);
        }
        if (type.isAbstract() || !(hasDefaultConstructor(type) || blank)) {
            return;
        }
        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "$instantiate", "(" + self + "I)" + self, null, null);
        mv.visitCode();
        Label none = new Label();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitJumpInsn(IFNULL, none);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFLE, none);
        mv.visitTypeInsn(NEW, name);
        mv.visitInsn(DUP);
        if (blank) {
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "(L" + BLANK + ";)V", false);
        } else {
            mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
        }
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitMethodInsn(INVOKEVIRTUAL, name, "$copySerialized", "(" + self + "I)V", false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(none);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /// Stack: a value of the class `def`, or null. Leaves its copy.
    private void instantiateSerialized(MethodVisitor mv, TypeDef def, int depthSlot) {
        String c = names.className(def.fullName());
        if (depthSlot < 0) {
            Jvm.pushInt(mv, SERIALIZED_DEPTH);
        } else {
            mv.visitVarInsn(ILOAD, depthSlot);
        }
        mv.visitMethodInsn(INVOKESTATIC, c, "$instantiate", "(L" + c + ";I)L" + c + ";", false);
    }

    /// Copies each serialized field `type` declares from the object in
    /// local `source` into `this`, for a script's `$copyFrom` and for a
    /// serializable class's `$copySerialized`. `depthSlot` is the local
    /// that counts the levels left, or negative in a script, where the
    /// count starts. Locals 3 and 4 are scratch.
    private void emitSerializedCopies(MethodVisitor mv, TypeDef type, String name, int source, int depthSlot,
            String why) {
        String runtime = Names.RUNTIME + "unityengine/UnityRuntime";
        for (FieldDef f : type.fields) {
            if (!isSerialized(type, f)) {
                continue;
            }
            String fieldName = Names.sanitize(f.name);
            String descriptor = names.descriptor(f.type);
            CilType t = f.type;
            String helper = null;
            boolean structArray = false;
            // The class of the elements, when each needs a copy of its own.
            TypeDef elements = null;
            if (names.isStruct(t)) {
                String c = names.classRef(t);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, source);
                mv.visitFieldInsn(GETFIELD, name, fieldName, descriptor);
                TypeDef inline = inlineStruct(t);
                if (inline != null) {
                    // One that holds references: see copiesSerializedStruct.
                    instantiateSerialized(mv, inline, depthSlot);
                } else {
                    mv.visitMethodInsn(INVOKEVIRTUAL, c, "$copy", "()L" + c + ";", false);
                }
                mv.visitFieldInsn(PUTFIELD, name, fieldName, descriptor);
                continue;
            } else if (names.kind(t) != Val.REF || t.kind == CilType.Kind.STRING) {
                helper = "";
            } else if (t.kind == CilType.Kind.SZARRAY) {
                helper = "$copyArray";
                elements = inlineStruct(t.element);
                structArray = elements == null && names.isStruct(t.element);
                if (elements == null) {
                    elements = inlineClass(t.element);
                }
            } else if (t.kind == CilType.Kind.GENERICINST) {
                String definition = t.element.typeName();
                if ("System.Collections.Generic.List`1".equals(definition)) {
                    helper = "$copyList";
                    elements = t.args.length != 1 ? null
                            : inlineStruct(t.args[0]) != null ? inlineStruct(t.args[0]) : inlineClass(t.args[0]);
                }
            } else if (t.kind == CilType.Kind.CLASS) {
                if (universe.derivesFrom(t.name, "UnityEngine.Object")) {
                    helper = "$remap";
                } else {
                    TypeDef def = universe.find(t.name);
                    TypeDef inline = inlineClass(t);
                    if (inline != null) {
                        // By value, as Unity serializes it: the copy gets
                        // an object of its own. Sharing the source's meant
                        // that damage dealt to one enemy's `Stats` was
                        // dealt to the enemy it was instantiated from and
                        // to every other copy of it.
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitVarInsn(ALOAD, source);
                        mv.visitFieldInsn(GETFIELD, name, fieldName, descriptor);
                        instantiateSerialized(mv, inline, depthSlot);
                        mv.visitFieldInsn(PUTFIELD, name, fieldName, descriptor);
                        continue;
                    }
                    // A serializable class that cannot be made here: see
                    // inlineClass. The two objects share the one instance.
                    helper = def != null && (def.flags & TYPE_SERIALIZABLE) != 0 ? "" : null;
                }
            }
            if (helper == null) {
                continue;
            }
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, source);
            mv.visitFieldInsn(GETFIELD, name, fieldName, descriptor);
            if (helper.length() > 0) {
                String helperDescriptor = "(Ljava/lang/Object;)Ljava/lang/Object;";
                index.requireMethod(runtime, helper, helperDescriptor, why);
                mv.visitMethodInsn(INVOKESTATIC, runtime, helper, helperDescriptor, false);
                mv.visitTypeInsn(CHECKCAST, descriptor.startsWith("[") ? descriptor
                        : descriptor.substring(1, descriptor.length() - 1));
            }
            mv.visitFieldInsn(PUTFIELD, name, fieldName, descriptor);
            if (structArray || (elements != null && t.kind == CilType.Kind.SZARRAY)) {
                // The array is new but still holds the source's elements.
                String c = names.classRef(t.element);
                Label loop = new Label();
                Label end = new Label();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, name, fieldName, descriptor);
                mv.visitVarInsn(ASTORE, 3);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitJumpInsn(IFNULL, end);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, 4);
                mv.visitLabel(loop);
                mv.visitVarInsn(ILOAD, 4);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitInsn(ARRAYLENGTH);
                mv.visitJumpInsn(IF_ICMPGE, end);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitVarInsn(ILOAD, 4);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitVarInsn(ILOAD, 4);
                mv.visitInsn(AALOAD);
                if (structArray) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, c, "$copy", "()L" + c + ";", false);
                } else {
                    instantiateSerialized(mv, elements, depthSlot);
                }
                mv.visitInsn(AASTORE);
                mv.visitIincInsn(4, 1);
                mv.visitJumpInsn(GOTO, loop);
                mv.visitLabel(end);
            } else if (elements != null) {
                // And so does the new list.
                String c = names.className(elements.fullName());
                index.requireMethod(LIST, "get_Count", "()I", why);
                index.requireMethod(LIST, "get_Item", "(I)Ljava/lang/Object;", why);
                index.requireMethod(LIST, "set_Item", "(ILjava/lang/Object;)V", why);
                Label loop = new Label();
                Label end = new Label();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, name, fieldName, descriptor);
                mv.visitVarInsn(ASTORE, 3);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitJumpInsn(IFNULL, end);
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, 4);
                mv.visitLabel(loop);
                mv.visitVarInsn(ILOAD, 4);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, LIST, "get_Count", "()I", false);
                mv.visitJumpInsn(IF_ICMPGE, end);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitVarInsn(ILOAD, 4);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitVarInsn(ILOAD, 4);
                mv.visitMethodInsn(INVOKEVIRTUAL, LIST, "get_Item", "(I)Ljava/lang/Object;", false);
                mv.visitTypeInsn(CHECKCAST, c);
                instantiateSerialized(mv, elements, depthSlot);
                mv.visitMethodInsn(INVOKEVIRTUAL, LIST, "set_Item", "(ILjava/lang/Object;)V", false);
                mv.visitIincInsn(4, 1);
                mv.visitJumpInsn(GOTO, loop);
                mv.visitLabel(end);
            }
        }
    }

    // ----------------------------------------------------- copy elimination

    private final Map<MethodDef, Boolean> nonInterfering = new HashMap<MethodDef, Boolean>();

    /// True when calling this method cannot change a struct the caller can
    /// already see. A struct argument read out of a field or an array element
    /// is normally copied before the call, because the callee might write to
    /// that field while it still reads the argument; for a method that writes
    /// to nothing but its own locals and the objects it creates, the copy
    /// protects against nothing, and it is the dominant cost of operator-heavy
    /// vector code (`pos[i] + vel[i] * dt` is two calls and would be two
    /// copies).
    ///
    /// The test is deliberately crude, and only ever errs towards copying: no
    /// stores except into the fields of a struct the method owns, no address
    /// of anything on the heap, no by-reference parameters, and no calls but
    /// to methods that pass this same test.
    boolean isNonInterfering(MethodRef ref) {
        MethodDef def = universe.resolve(ref);
        if (def == null || !names.isTranslated(def.owner.fullName()) || !def.hasBody()) {
            return false;
        }
        Boolean known = nonInterfering.get(def);
        if (known != null) {
            return known.booleanValue();
        }
        // Assume the worst of a method that calls itself, directly or not.
        nonInterfering.put(def, Boolean.FALSE);
        boolean result = computeNonInterfering(def);
        nonInterfering.put(def, Boolean.valueOf(result));
        return result;
    }

    private boolean computeNonInterfering(MethodDef def) {
        boolean struct = def.owner.isValueType();
        if (!def.isStatic() && !struct) {
            // A virtual call could land on an override this has not looked at.
            return false;
        }
        for (CilType p : def.sig.params) {
            if (p.kind == CilType.Kind.BYREF) {
                return false;
            }
        }
        // `this` of a struct method is a reference to wherever the struct
        // lives, so only a constructor -- whose `this` is always new -- may
        // store through it.
        boolean mayStoreFields = def.isStatic() || ".ctor".equals(def.name);
        CilAssembly assembly = def.owner.assembly;
        for (Instr ins : def.body().instructions) {
            String op = ins.op;
            if ("stfld".equals(op)) {
                if (!mayStoreFields || !names.isStruct(assembly.fieldRef(ins.operand).declaringType)) {
                    return false;
                }
            } else if ("stsfld".equals(op) || op.startsWith("stelem") || op.startsWith("stind")
                    || "stobj".equals(op) || "cpobj".equals(op) || "initobj".equals(op) || "ldflda".equals(op)
                    || "ldsflda".equals(op) || "ldelema".equals(op) || "unbox".equals(op) || "calli".equals(op)
                    || "ldftn".equals(op) || "ldvirtftn".equals(op)) {
                return false;
            } else if ("call".equals(op) || "callvirt".equals(op)) {
                if (!isNonInterfering(assembly.methodRef(ins.operand))) {
                    return false;
                }
            } else if ("newobj".equals(op)) {
                MethodRef ctor = assembly.methodRef(ins.operand);
                if (!names.isStruct(ctor.declaringType) || !isNonInterfering(ctor)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ----------------------------------------------------------------- events

    /// The accessors C# writes for a field-like event swap the delegate in
    /// with `Interlocked.CompareExchange` through the field's address, to be
    /// safe against another thread subscribing at the same moment. Codename
    /// One runs application code on one thread, so they become a plain
    /// combine-and-store.
    private boolean isAutoEventAccessor(MethodDef m) {
        if ((m.semantics & (CilAssembly.SEMANTICS_ADD_ON | CilAssembly.SEMANTICS_REMOVE_ON)) == 0) {
            return false;
        }
        for (Instr ins : m.body().instructions) {
            if ("call".equals(ins.op)) {
                MethodRef ref = m.owner.assembly.methodRef(ins.operand);
                if ("CompareExchange".equals(ref.name)) {
                    return m.owner.field(m.associationName) != null;
                }
            }
        }
        return false;
    }

    private void emitEventAccessor(TypeDef type, MethodDef m, MethodVisitor mv) {
        FieldDef field = type.field(m.associationName);
        String owner = names.className(type.fullName());
        String fieldName = Names.sanitize(field.name);
        String d = names.descriptor(field.type);
        String delegate = "L" + Names.DELEGATE + ";";
        boolean add = (m.semantics & CilAssembly.SEMANTICS_ADD_ON) != 0;
        mv.visitCode();
        boolean inContext = m.isStatic() && generics.hasContext(type);
        if (inContext) {
            // The event of one constructed type: a field of the context
            // the accessor is handed after the delegate.
            owner = generics.contextClass(type);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, owner, fieldName, d);
            mv.visitVarInsn(ALOAD, 0);
        } else if (m.isStatic()) {
            mv.visitFieldInsn(GETSTATIC, owner, fieldName, d);
            mv.visitVarInsn(ALOAD, 0);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, owner, fieldName, d);
            mv.visitVarInsn(ALOAD, 1);
        }
        index.requireMethod(Names.DELEGATE, add ? "Combine" : "Remove", "(" + delegate + delegate + ")" + delegate,
                m.toString());
        mv.visitMethodInsn(INVOKESTATIC, Names.DELEGATE, add ? "Combine" : "Remove",
                "(" + delegate + delegate + ")" + delegate, false);
        mv.visitTypeInsn(CHECKCAST, names.classRef(field.type));
        mv.visitFieldInsn(m.isStatic() && !inContext ? PUTSTATIC : PUTFIELD, owner, fieldName, d);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // -------------------------------------------------------------- adapters

    /// Calls `to` with the parameters of a method shaped like `from`, and
    /// returns its result as `from` would. The two differ only in erasure:
    /// one side says `T` where the other says `int`, so a value is boxed,
    /// unboxed or cast on the way through. Used for interface bridges and
    /// for delegates.
    private void adapt(MethodVisitor mv, MethodSig from, int firstSlot, MethodSig to, int opcode, String owner,
            String name, String descriptor, boolean isInterface) {
        adapt(mv, from, firstSlot, to, opcode, owner, name, descriptor, isInterface, null);
    }

    /// `context`, when given, is the class of the context the target takes
    /// after everything else; the delegate keeps it as its target.
    private void adapt(MethodVisitor mv, MethodSig from, int firstSlot, MethodSig to, int opcode, String owner,
            String name, String descriptor, boolean isInterface, String context) {
        int slot = firstSlot;
        for (int i = 0; i < from.params.length; i++) {
            CilType fp = from.params[i];
            CilType tp = to.params[i];
            if (fp.kind == CilType.Kind.BYREF) {
                mv.visitVarInsn(ALOAD, slot++);
                if (!names.isStruct(fp.element)) {
                    mv.visitVarInsn(ILOAD, slot++);
                }
                continue;
            }
            int k = names.kind(fp);
            mv.visitVarInsn(Jvm.loadOp(k), slot);
            slot += k == Val.I8 || k == Val.R8 ? 2 : 1;
            if (Names.isTypeVariable(fp)) {
                if (!Names.isTypeVariable(tp) && tp.kind != CilType.Kind.OBJECT) {
                    if (names.norm(tp).isPrimitive()) {
                        Jvm.unbox(mv, names, tp);
                    } else {
                        // A struct goes through as the object it is, with
                        // no copy, and that is the whole of the protocol
                        // rather than a gap in it. The target makes its own
                        // copy on entry of any struct parameter it writes
                        // or takes the address of (MethodTranslator's
                        // emitEntry), and copies one it keeps, so it cannot
                        // change the caller's. And nobody can change the
                        // argument under the target: translated code copies
                        // a struct it passes as a `T` unless it just made
                        // it, a list or a dictionary replaces an element
                        // and never writes into one, and an array of
                        // structs, whose elements are written in place,
                        // reaches erased code only through an ArrayView
                        // that copies. A copy here would be a second one on
                        // each of those paths, for every element of every
                        // query, every frame.
                        mv.visitTypeInsn(CHECKCAST, names.classRef(tp));
                    }
                }
            } else if (Names.isTypeVariable(tp) && names.norm(fp).isPrimitive()) {
                Jvm.box(mv, names, fp);
            }
        }
        CilType fr = from.returnType;
        CilType tr = to.returnType;
        // `slot` is now where the caller's result object is, if `from` is
        // handed one.
        boolean fromStruct = names.returnsStruct(from);
        if (names.returnsStruct(to)) {
            if (fromStruct) {
                mv.visitVarInsn(ALOAD, slot);
            } else {
                // The result leaves as an object of its own: a box.
                String c = names.classRef(tr);
                mv.visitTypeInsn(NEW, c);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, c, "<init>", "()V", false);
            }
            loadDelegateContext(mv, context);
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (fr.kind == CilType.Kind.VOID) {
                mv.visitInsn(POP);
                mv.visitInsn(RETURN);
            } else {
                mv.visitInsn(ARETURN);
            }
            return;
        }
        loadDelegateContext(mv, context);
        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (fromStruct) {
            // An erased method answering for one declared to return a struct.
            String c = names.classRef(fr);
            mv.visitTypeInsn(CHECKCAST, c);
            mv.visitVarInsn(ALOAD, slot);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(INVOKEVIRTUAL, c, "$assign", "(L" + c + ";)V", false);
            mv.visitVarInsn(ALOAD, slot);
            mv.visitInsn(ARETURN);
            return;
        }
        if (fr.kind == CilType.Kind.VOID) {
            if (tr.kind != CilType.Kind.VOID) {
                int k = names.kind(tr);
                mv.visitInsn(k == Val.I8 || k == Val.R8 ? POP2 : POP);
            }
            mv.visitInsn(RETURN);
            return;
        }
        if (Names.isTypeVariable(fr) || fr.kind == CilType.Kind.OBJECT) {
            if (!Names.isTypeVariable(tr) && names.norm(tr).isPrimitive()) {
                Jvm.box(mv, names, tr);
            }
            mv.visitInsn(ARETURN);
            return;
        }
        if (Names.isTypeVariable(tr)) {
            if (names.norm(fr).isPrimitive()) {
                Jvm.unbox(mv, names, fr);
            } else {
                mv.visitTypeInsn(CHECKCAST, names.classRef(fr));
            }
        }
        int k = names.kind(fr);
        mv.visitInsn(k == Val.REF ? ARETURN : IRETURN + k);
    }

    private static void loadDelegateContext(MethodVisitor mv, String context) {
        if (context != null) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, Names.DELEGATE, "$target", "Ljava/lang/Object;");
            mv.visitTypeInsn(CHECKCAST, context);
        }
    }

    private void collectInterfaces(CilType iface, List<CilType> out) {
        if (out.contains(iface)) {
            return;
        }
        out.add(iface);
        TypeDef def = universe.definitionOf(iface);
        if (def == null) {
            return;
        }
        CilType[] typeArgs = iface.kind == CilType.Kind.GENERICINST ? iface.args : null;
        for (CilType base : def.interfaces) {
            collectInterfaces(base.substitute(typeArgs, null), out);
        }
    }

    private static boolean sameParameters(MethodSig a, MethodSig b, CilType[] typeArgs) {
        if (a.params.length != b.params.length
                || !a.returnType.equals(b.returnType.substitute(typeArgs, null))) {
            return false;
        }
        for (int i = 0; i < a.params.length; i++) {
            if (!a.params[i].equals(b.params[i].substitute(typeArgs, null))) {
                return false;
            }
        }
        return true;
    }

    /// An interface method is found on the JVM by its name and erased
    /// descriptor. C# is looser: an explicit implementation has another name
    /// altogether, and an implementation of `IComparable<Rect>.CompareTo`
    /// takes a `Rect` where the interface, erased, takes an `Object`. Each
    /// such method gets a bridge under the name and descriptor the interface
    /// call will look for.
    private void emitBridges(ClassWriter cw, TypeDef type, String name, Set<String> members) {
        List<CilType> interfaces = new ArrayList<CilType>();
        for (CilType i : type.interfaces) {
            collectInterfaces(i, interfaces);
        }
        boolean struct = type.isValueType();
        for (CilType iface : interfaces) {
            TypeDef idef = universe.definitionOf(iface);
            if (idef == null) {
                continue;
            }
            CilType[] typeArgs = iface.kind == CilType.Kind.GENERICINST ? iface.args : null;
            for (MethodDef im : idef.methods) {
                if (im.isStatic()) {
                    continue;
                }
                MethodDef impl = null;
                for (MethodImpl mi : type.methodImpls) {
                    if (mi.declaration.declaringType.equals(iface) && mi.declaration.name.equals(im.name)
                            && mi.declaration.sig.sameShape(im.sig)) {
                        impl = universe.resolve(mi.body);
                    }
                }
                TypeDef at = type;
                while (impl == null && at != null && names.isTranslated(at.fullName())) {
                    for (MethodDef m : at.methods) {
                        if (!m.isStatic() && m.name.equals(im.name) && sameParameters(m.sig, im.sig, typeArgs)) {
                            impl = m;
                            break;
                        }
                    }
                    at = at.baseType == null ? null : universe.definitionOf(at.baseType);
                }
                if (impl == null) {
                    continue;
                }
                String slotName = names.methodName(im.name, im.sig, false);
                String slotDescriptor = names.methodDescriptor(im.sig);
                String implName = names.methodName(impl.name, impl.sig, struct) + names.hidingSuffix(impl);
                String implDescriptor = names.methodDescriptor(impl.sig);
                if ((slotName.equals(implName) && slotDescriptor.equals(implDescriptor))
                        || !members.add(slotName + slotDescriptor)) {
                    continue;
                }
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE, slotName, slotDescriptor,
                        null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                adapt(mv, im.sig, 1, impl.sig, INVOKEVIRTUAL, name, implName, implDescriptor, false);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
    }

    // -------------------------------------------------------------- delegates

    /// The class that makes a delegate out of one method: a subclass of the
    /// delegate type whose `Invoke` calls it. One per method and delegate
    /// type, however many places create it, because every class costs one of
    /// ParparVM's 65,536 class ids.
    static final String TYPE = Names.RUNTIME + "system/Type";
    static final String TYPE_DESCRIPTOR = "L" + TYPE + ";";

    /// An enum is the integer under it wherever it is stored or passed, and
    /// that is all most code needs. What it cannot give is the name of a
    /// value -- `side.ToString()`, an enum in an interpolated string,
    /// `Enum.GetValues` -- so each enum also gets a class holding one static
    /// field, `$TYPE`: the `System.Type` of the enum, with its members.
    /// `typeof` reads that field, and boxing an enum pairs the value with it.
    private void emitEnum(TypeDef type) {
        String name = names.className(type.fullName());
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_5, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, name, null, Names.OBJECT, null);
        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "$TYPE", TYPE_DESCRIPTOR, null, null).visitEnd();
        List<FieldDef> members = new ArrayList<FieldDef>();
        for (FieldDef f : type.fields) {
            if (f.isStatic() && f.isLiteral() && f.constant != null) {
                members.add(f);
            }
        }
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(type.fullName().replace('/', '+'));
        Jvm.pushInt(mv, members.size());
        mv.visitTypeInsn(ANEWARRAY, Names.STRING);
        for (int i = 0; i < members.size(); i++) {
            mv.visitInsn(DUP);
            Jvm.pushInt(mv, i);
            mv.visitLdcInsn(members.get(i).name);
            mv.visitInsn(AASTORE);
        }
        Jvm.pushInt(mv, members.size());
        mv.visitIntInsn(NEWARRAY, T_LONG);
        for (int i = 0; i < members.size(); i++) {
            mv.visitInsn(DUP);
            Jvm.pushInt(mv, i);
            mv.visitLdcInsn(members.get(i).constant);
            mv.visitInsn(LASTORE);
        }
        mv.visitInsn(type.assembly.attributes((MetadataReader.TYPE_DEF << 24) | type.row).contains("System.FlagsAttribute") ? ICONST_1 : ICONST_0);
        // The integer type the enum is stored as, by its .NET name: the
        // runtime needs it to hand `Enum.GetValues` back as an array of that
        // type, to order and print the values of an unsigned enum, and to
        // tell a number the enum can hold from one it cannot.
        mv.visitLdcInsn(type.enumUnderlyingType().typeName());
        String descriptor = "(Ljava/lang/String;[Ljava/lang/String;[JZLjava/lang/String;)" + TYPE_DESCRIPTOR;
        index.requireMethod(TYPE, "$enum", descriptor, "the enum " + type.fullName());
        mv.visitMethodInsn(INVOKESTATIC, TYPE, "$enum", descriptor, false);
        mv.visitFieldInsn(PUTSTATIC, name, "$TYPE", TYPE_DESCRIPTOR);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        classes.put(name, cw.toByteArray());
    }

    /// A delegate type declared in C#: an abstract class with the `Invoke`
    /// the declaration gives it, in the shape of the runtime's own `Action`
    /// and `Func`, and beside it the class of a delegate over several
    /// methods, which calls each in turn and returns what the last returned.
    private void emitDelegateType(TypeDef type) {
        String full = type.fullName();
        String name = names.className(full);
        String multi = name + "$$Multi";
        String base = Names.RUNTIME + "system/MulticastDelegate";
        String list = "[L" + Names.DELEGATE + ";";
        MethodDef invoke = null;
        for (MethodDef m : type.methods) {
            if ("Invoke".equals(m.name)) {
                invoke = m;
            }
        }
        if (invoke == null) {
            throw new TranslationException(full + " has no Invoke method");
        }
        String descriptor = names.methodDescriptor(invoke.sig);
        index.requireMethod(base, "<init>", "()V", "the delegate type " + full);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_5, ACC_PUBLIC | ACC_ABSTRACT | ACC_SUPER, name, null, base, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, base, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "Invoke", descriptor, null, null).visitEnd();
        mv = cw.visitMethod(ACC_PROTECTED, "$multi", "(" + list + ")L" + Names.DELEGATE + ";", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, multi);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, multi, "<init>", "(" + list + ")V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        // What `Delegate.Combine` asks before it joins two: only this
        // class can say what delegate type it is.
        mv = cw.visitMethod(ACC_PROTECTED, "$sameType", "(L" + Names.DELEGATE + ";)Z", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(INSTANCEOF, name);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        classes.put(name, cw.toByteArray());

        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_5, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, multi, null, name, null);
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + list + ")V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, Names.DELEGATE, "$list", list);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC, "Invoke", descriptor, null, null);
        mv.visitCode();
        org.objectweb.asm.Type[] arguments = org.objectweb.asm.Type.getArgumentTypes(descriptor);
        org.objectweb.asm.Type result = org.objectweb.asm.Type.getReturnType(descriptor);
        int slots = 1;
        for (org.objectweb.asm.Type a : arguments) {
            slots += a.getSize();
        }
        int counter = slots;
        int kept = slots + 1;
        boolean returns = result.getSort() != org.objectweb.asm.Type.VOID;
        if (returns) {
            // What the last call returned; the zero of its type before any.
            switch (result.getSort()) {
                case org.objectweb.asm.Type.LONG:
                    mv.visitInsn(LCONST_0);
                    break;
                case org.objectweb.asm.Type.FLOAT:
                    mv.visitInsn(FCONST_0);
                    break;
                case org.objectweb.asm.Type.DOUBLE:
                    mv.visitInsn(DCONST_0);
                    break;
                case org.objectweb.asm.Type.OBJECT:
                case org.objectweb.asm.Type.ARRAY:
                    mv.visitInsn(ACONST_NULL);
                    break;
                default:
                    mv.visitInsn(ICONST_0);
                    break;
            }
            mv.visitVarInsn(result.getOpcode(ISTORE), kept);
        }
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, counter);
        Label top = new Label();
        Label done = new Label();
        mv.visitLabel(top);
        mv.visitVarInsn(ILOAD, counter);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, Names.DELEGATE, "$list", list);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPGE, done);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, Names.DELEGATE, "$list", list);
        mv.visitVarInsn(ILOAD, counter);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, name);
        int at = 1;
        for (org.objectweb.asm.Type a : arguments) {
            mv.visitVarInsn(a.getOpcode(ILOAD), at);
            at += a.getSize();
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, name, "Invoke", descriptor, false);
        if (returns) {
            mv.visitVarInsn(result.getOpcode(ISTORE), kept);
        }
        mv.visitIincInsn(counter, 1);
        mv.visitJumpInsn(GOTO, top);
        mv.visitLabel(done);
        if (returns) {
            mv.visitVarInsn(result.getOpcode(ILOAD), kept);
            mv.visitInsn(result.getOpcode(IRETURN));
        } else {
            mv.visitInsn(RETURN);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        classes.put(multi, cw.toByteArray());
    }

    /// True for an enum this run translates: one that has a `$TYPE`.
    /// For a constructed `ValueTuple` of two or three items, pushes the
    /// default of each item and answers the descriptor of what takes them;
    /// null for any other type, and nothing pushed. The runtime's tuple
    /// erases its items to `Object`, so `default((int, int))` would hold two
    /// nulls, and `Equals` and `ToString`, which read the items without a
    /// type to read them as, answered for nulls: `(, )`, and unequal to
    /// `(0, 0)`. Where the type arguments are known they are boxed zeros; a
    /// type variable, a reference type and anything unknown stay null.
    String pushTupleDefaults(MethodVisitor mv, CilType type) {
        int arity = tupleArity(type);
        if (arity == 0) {
            return null;
        }
        StringBuilder d = new StringBuilder("(");
        for (int i = 0; i < arity; i++) {
            CilType a = type.args[i];
            CilType p = names.norm(a);
            if (Names.isTypeVariable(a)) {
                mv.visitInsn(ACONST_NULL);
            } else if (hasEnumType(a)) {
                if (names.kind(a) == Val.I8) {
                    mv.visitInsn(LCONST_0);
                } else {
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(I2L);
                }
                mv.visitFieldInsn(GETSTATIC, names.className(a.name), "$TYPE", TYPE_DESCRIPTOR);
                mv.visitMethodInsn(INVOKESTATIC, TYPE, "$box", "(J" + TYPE_DESCRIPTOR + ")Ljava/lang/Object;", false);
            } else if (p.isPrimitive()) {
                Jvm.pushDefault(mv, names.kind(p));
                Jvm.box(mv, names, p);
            } else if (names.isStruct(p)) {
                String c = names.classRef(p);
                mv.visitTypeInsn(NEW, c);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, c, "<init>", "()V", false);
                // A tuple nested in a tuple gets its own defaults.
                if (tupleArity(p) > 0) {
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(INVOKEVIRTUAL, c, "$defaults", pushTupleDefaults(mv, p) + "V", false);
                }
            } else {
                mv.visitInsn(ACONST_NULL);
            }
            d.append("Ljava/lang/Object;");
        }
        return d.append(')').toString();
    }

    /// 2 or 3 for a constructed `ValueTuple` the runtime has, else 0.
    static int tupleArity(CilType type) {
        if (type == null || type.kind != CilType.Kind.GENERICINST || type.element == null
                || type.element.name == null) {
            return 0;
        }
        String n = type.element.name;
        int arity = "System.ValueTuple`2".equals(n) ? 2 : "System.ValueTuple`3".equals(n) ? 3 : 0;
        return type.args.length == arity ? arity : 0;
    }

    boolean hasEnumType(CilType type) {
        return names.isEnum(type) && names.isTranslated(type.name);
    }

    String delegateClass(TypeDef user, CilType delegateType, MethodRef target, boolean virtual) {
        String key = delegateType + " <- " + target + (target.methodArgs == null ? "" : "/" + target.methodArgs.length)
                + (virtual ? " virtual" : "");
        String existing = delegateClasses.get(key);
        if (existing != null) {
            return existing;
        }
        TypeDef ddef = universe.definitionOf(delegateType);
        MethodDef invoke = null;
        for (MethodDef m : ddef.methods) {
            if ("Invoke".equals(m.name)) {
                invoke = m;
            }
        }
        if (invoke == null) {
            throw new TranslationException(delegateType + " has no Invoke method");
        }
        String base = names.classRef(delegateType);
        String name = names.className(user.assembly.entryPoint() != null
                ? user.assembly.entryPoint().owner.fullName() : user.fullName()) + "$$Delegate" + delegateClasses.size();
        index.requireMethod(base, "<init>", "()V", "a delegate of type " + delegateType);
        index.requireMethod(base, "Invoke", names.methodDescriptor(invoke.sig), "a delegate of type " + delegateType);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_5, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, name, null, base, null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, base, "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, Names.DELEGATE, "$target", "Ljava/lang/Object;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        MethodSig sig = target.sig;
        String declaring = target.declaringType.typeName();
        int opcode;
        String owner;
        String targetName;
        String descriptor;
        boolean isInterface = false;
        String context = null;
        if (Names.hasHelperClass(declaring)) {
            opcode = INVOKESTATIC;
            owner = Names.helperClass(declaring);
            targetName = Names.sanitize(target.name);
            descriptor = names.methodDescriptor(sig, sig.hasThis ? names.descriptor(target.declaringType) : null);
        } else {
            TypeDef tdef = universe.definitionOf(target.declaringType);
            owner = names.classRef(target.declaringType);
            targetName = names.methodName(target.name, sig, names.isStruct(target.declaringType));
            descriptor = names.methodDescriptor(sig);
            MethodDef resolved = universe.resolve(target);
            if (resolved != null) {
                targetName += names.hidingSuffix(resolved);
            }
            String hidden = resolved == null ? "" : generics.hiddenParameters(resolved);
            if (hidden.length() > 0) {
                // The one context such a method takes is the delegate's
                // target (MethodTranslator.newObject puts it there, and
                // refuses what would need two).
                context = hidden.substring(1, hidden.length() - 1);
                descriptor = GenericStatics.withHidden(descriptor, hidden);
            }
            isInterface = tdef != null && tdef.isInterface();
            opcode = !sig.hasThis ? INVOKESTATIC : isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL;
        }
        index.requireMethod(owner, targetName, descriptor, "a delegate of type " + delegateType);
        mv = cw.visitMethod(ACC_PUBLIC, "Invoke", names.methodDescriptor(invoke.sig), null, null);
        mv.visitCode();
        if (sig.hasThis) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, Names.DELEGATE, "$target", "Ljava/lang/Object;");
            String receiver = names.classRef(target.declaringType);
            if (!receiver.equals(Names.OBJECT)) {
                mv.visitTypeInsn(CHECKCAST, receiver);
            }
        }
        adapt(mv, invoke.sig, 1, sig, opcode, owner, targetName, descriptor, isInterface, context);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        classes.put(name, cw.toByteArray());
        delegateClasses.put(key, name);
        return name;
    }

    // ------------------------------------------------------------------- main

    private void emitMain(ClassWriter cw, String name, MethodDef entry) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        if (entry.sig.params.length == 1) {
            mv.visitVarInsn(ALOAD, 0);
        }
        mv.visitMethodInsn(INVOKESTATIC, name, names.methodName(entry.name, entry.sig, false),
                names.methodDescriptor(entry.sig), false);
        if (entry.sig.returnType.kind != CilType.Kind.VOID) {
            mv.visitInsn(POP);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
