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
/// them and have no class. Generics are erased. Classes are Java 5 class
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
            cw.visitField(ACC_PUBLIC | (f.isStatic() ? ACC_STATIC : 0), fieldName, names.descriptor(f.type), null,
                    null).visitEnd();
        }

        boolean hasStaticConstructor = false;
        boolean[] objectVirtuals = new boolean[3];
        for (MethodDef m : type.methods) {
            String methodName = names.methodName(m.name, m.sig, struct);
            String descriptor = names.methodDescriptor(m.sig);
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
        if (struct) {
            emitStructMembers(cw, type, name, objectVirtuals);
        }
        if (!hasStaticConstructor && hasStructField(type, true)) {
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
                        mv.visitInsn(FCMPL);
                        mv.visitJumpInsn(IFNE, no);
                        break;
                    case Val.R8:
                        mv.visitInsn(DCMPL);
                        mv.visitJumpInsn(IFNE, no);
                        break;
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
    /// `[SerializeField]`, and neither `readonly` nor `[NonSerialized]`.
    private boolean isSerialized(TypeDef type, FieldDef f) {
        if (f.isStatic() || f.isLiteral() || f.rva != 0 || (f.flags & (FIELD_INIT_ONLY | FIELD_NOT_SERIALIZED)) != 0) {
            return false;
        }
        return (f.flags & FIELD_ACCESS_MASK) == FIELD_PUBLIC
                || type.assembly.attributes(f.token()).contains("UnityEngine.SerializeField");
    }

    /// What the runtime needs of a script that reflection would give it
    /// elsewhere, and that a Codename One device does not have.
    ///
    /// - `$new()` makes another script of the same class, for `Instantiate`
    ///   of an object already in the scene and for `AddComponent`.
    /// - `$copyFrom(source)` copies the serialized fields from another
    ///   instance, which is what `Instantiate` means by a copy. A struct is
    ///   copied, an array is duplicated, and a reference to a Unity object
    ///   is passed through the runtime, which redirects one that points
    ///   into the tree being copied to the copy. A field of a type Unity
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
        String runtime = Names.RUNTIME + "unityengine/UnityRuntime";
        String why = "copying the script " + full;
        boolean hasConstructor = false;
        for (MethodDef m : type.methods) {
            hasConstructor |= ".ctor".equals(m.name) && m.sig.params.length == 0;
        }
        if (!type.isAbstract() && hasConstructor) {
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
        for (FieldDef f : type.fields) {
            if (!isSerialized(type, f)) {
                continue;
            }
            String fieldName = Names.sanitize(f.name);
            String descriptor = names.descriptor(f.type);
            CilType t = f.type;
            String helper = null;
            boolean structArray = false;
            if (names.isStruct(t)) {
                String c = names.classRef(t);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitFieldInsn(GETFIELD, name, fieldName, descriptor);
                mv.visitMethodInsn(INVOKEVIRTUAL, c, "$copy", "()L" + c + ";", false);
                mv.visitFieldInsn(PUTFIELD, name, fieldName, descriptor);
                continue;
            } else if (names.kind(t) != Val.REF || t.kind == CilType.Kind.STRING) {
                helper = "";
            } else if (t.kind == CilType.Kind.SZARRAY) {
                helper = "$copyArray";
                structArray = names.isStruct(t.element);
            } else if (t.kind == CilType.Kind.GENERICINST) {
                String definition = t.element.typeName();
                if ("System.Collections.Generic.List`1".equals(definition)) {
                    helper = "$copyList";
                }
            } else if (t.kind == CilType.Kind.CLASS) {
                if (universe.derivesFrom(t.name, "UnityEngine.Object")) {
                    helper = "$remap";
                } else {
                    TypeDef def = universe.find(t.name);
                    // Unity copies a serializable class field by field;
                    // here the two scripts share the one object.
                    helper = def != null && (def.flags & TYPE_SERIALIZABLE) != 0 ? "" : null;
                }
            }
            if (helper == null) {
                continue;
            }
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(GETFIELD, name, fieldName, descriptor);
            if (helper.length() > 0) {
                String helperDescriptor = "(Ljava/lang/Object;)Ljava/lang/Object;";
                index.requireMethod(runtime, helper, helperDescriptor, why);
                mv.visitMethodInsn(INVOKESTATIC, runtime, helper, helperDescriptor, false);
                mv.visitTypeInsn(CHECKCAST, descriptor.startsWith("[") ? descriptor
                        : descriptor.substring(1, descriptor.length() - 1));
            }
            mv.visitFieldInsn(PUTFIELD, name, fieldName, descriptor);
            if (structArray) {
                // The array is new but still holds the source's structs.
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
                mv.visitMethodInsn(INVOKEVIRTUAL, c, "$copy", "()L" + c + ";", false);
                mv.visitInsn(AASTORE);
                mv.visitIincInsn(4, 1);
                mv.visitJumpInsn(GOTO, loop);
                mv.visitLabel(end);
            }
        }
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
            mv.visitMethodInsn(INVOKEVIRTUAL, name, names.methodName(m.name, m.sig, false),
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
        if (m.isStatic()) {
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
        mv.visitFieldInsn(m.isStatic() ? PUTSTATIC : PUTFIELD, owner, fieldName, d);
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
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (fr.kind == CilType.Kind.VOID) {
                mv.visitInsn(POP);
                mv.visitInsn(RETURN);
            } else {
                mv.visitInsn(ARETURN);
            }
            return;
        }
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
                String implName = names.methodName(impl.name, impl.sig, struct);
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
        String descriptor = "(Ljava/lang/String;[Ljava/lang/String;[JZ)" + TYPE_DESCRIPTOR;
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
        adapt(mv, invoke.sig, 1, sig, opcode, owner, targetName, descriptor, isInterface);
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
