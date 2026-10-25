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
package com.codename1.tools.translator;

import com.codename1.tools.translator.classfile.ClassVisitor;
import com.codename1.tools.translator.classfile.Label;
import com.codename1.tools.translator.classfile.MethodVisitor;
import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// The two JDK facilities a device cannot provide at run time, provided at build time.
///
/// Both are reflection in disguise, and both are everywhere in Kotlin libraries:
/// kotlinx.coroutines, the Compose runtime, atomicfu and Ktor are built on them.
///
/// #### Field updaters
///
/// `AtomicReferenceFieldUpdater.newUpdater(Owner.class, V.class, "field")` (and the
/// `Integer` and `Long` forms) finds a field by name. Every compiler emits the
/// arguments as constants right before the call, so [#adapt] recognises the three (or
/// two) `ldc` instructions and the `invokestatic`, and replaces them with
/// `new Owner$cn1fu$field()`: a subclass of the updater that [#writeSynthetics]
/// generates for that one field. Its `get` and `set` are a plain `getfield` and
/// `putfield`, which on a volatile field are already atomic. Its `compareAndSet`
/// calls a static `cas0`, which is:
///
/// - on a native target, a native whose C body is generated here -- a C11 compare and
///   exchange on the struct member, behind the same SATB and generational barriers
///   the generated setter applies. A lock would not do: code that uses an updater
///   also writes the field directly (atomicfu lowers `value = x` to a volatile
///   store), and only a hardware CAS is atomic with respect to that store;
/// - on JavaScript, branch-free bytecode: every thread there runs on one host thread,
///   so nothing can run between the read and the write.
///
/// A call whose arguments are not constants is left alone, and reaches the JavaAPI
/// method, which throws.
///
/// #### Services
///
/// `java.util.ServiceLoader` cannot search a class path. [#recordResource] collects
/// every `META-INF/services` file among the translated inputs, and the generated
/// `java.util.ServiceLoader$Registry.providers(Class)` instantiates the providers each
/// names, in order. The JavaAPI stub of that class is skipped when parsing
/// ([#isReplacedStub]) and always regenerated, empty for an application that has no
/// service files.
final class RuntimeLowering {
    static final String REGISTRY = "java/util/ServiceLoader$Registry";
    private static final String REF_UPDATER = "java/util/concurrent/atomic/AtomicReferenceFieldUpdater";
    private static final String INT_UPDATER = "java/util/concurrent/atomic/AtomicIntegerFieldUpdater";
    private static final String LONG_UPDATER = "java/util/concurrent/atomic/AtomicLongFieldUpdater";
    private static final String GENERATED_C = "cn1_runtime_lowering";
    private static final int CLASS_VERSION_8 = 52;

    /// Every field an updater was resolved for, keyed by the generated class name.
    private static final Map<String, Updater> UPDATERS = new TreeMap<String, Updater>();
    /// Service interface (binary name) to provider class names, in file order.
    private static final Map<String, Set<String>> SERVICES = new TreeMap<String, Set<String>>();

    private RuntimeLowering() {
    }

    static void cleanup() {
        UPDATERS.clear();
        SERVICES.clear();
    }

    static boolean isReplacedStub(String internalName) {
        return REGISTRY.equals(internalName);
    }

    /// One resolved updater: the generated class, the class named as the owner, and
    /// the field.
    private static final class Updater {
        final String generated;
        final String owner;
        final String field;
        final String updaterClass;
        /// `I`, `J` or `L` -- the updater's kind, not the field's declared type.
        final char kind;

        Updater(String generated, String owner, String field, String updaterClass, char kind) {
            this.generated = generated;
            this.owner = owner;
            this.field = field;
            this.updaterClass = updaterClass;
            this.kind = kind;
        }
    }

    /// Wraps the parser's visitor so updater call sites are rewritten as they are read.
    static ClassVisitor adapt(ClassVisitor parser) {
        return new ClassVisitor(parser) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return mv == null ? null : new CallSites(mv);
            }
        };
    }

    /// Holds back trailing `ldc` instructions until it knows whether a `newUpdater`
    /// follows them.
    private static final class CallSites extends MethodVisitor {
        private final List<Object> pending = new ArrayList<Object>();

        CallSites(MethodVisitor mv) {
            super(mv);
        }

        private void flush() {
            for (Object o : pending) {
                mv.visitLdcInsn(o);
            }
            pending.clear();
        }

        @Override
        public void visitLdcInsn(Object value) {
            pending.add(value);
            if (pending.size() > 3) {
                mv.visitLdcInsn(pending.remove(0));
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC && "newUpdater".equals(name) && tryResolve(owner)) {
                return;
            }
            flush();
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        private boolean tryResolve(String updaterClass) {
            char kind;
            int args;
            if (REF_UPDATER.equals(updaterClass)) {
                kind = 'L';
                args = 3;
            } else if (INT_UPDATER.equals(updaterClass)) {
                kind = 'I';
                args = 2;
            } else if (LONG_UPDATER.equals(updaterClass)) {
                kind = 'J';
                args = 2;
            } else {
                return false;
            }
            if (pending.size() < args) {
                return false;
            }
            int base = pending.size() - args;
            Object ownerConst = pending.get(base);
            Object nameConst = pending.get(pending.size() - 1);
            if (!(ownerConst instanceof Type) || ((Type) ownerConst).getSort() != Type.OBJECT
                    || !(nameConst instanceof String)
                    || (args == 3 && !(pending.get(base + 1) instanceof Type))) {
                return false;
            }
            String owner = ((Type) ownerConst).getInternalName();
            String field = (String) nameConst;
            String generated = owner + "$cn1fu$" + field;
            synchronized (UPDATERS) {
                if (!UPDATERS.containsKey(generated)) {
                    UPDATERS.put(generated, new Updater(generated, owner, field, updaterClass, kind));
                }
            }
            for (int i = 0; i < base; i++) {
                mv.visitLdcInsn(pending.get(i));
            }
            pending.clear();
            mv.visitTypeInsn(Opcodes.NEW, generated);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, generated, "<init>", "()V", false);
            return true;
        }

        @Override public void visitInsn(int opcode) { flush(); mv.visitInsn(opcode); }
        @Override public void visitIntInsn(int opcode, int operand) { flush(); mv.visitIntInsn(opcode, operand); }
        @Override public void visitVarInsn(int opcode, int var) { flush(); mv.visitVarInsn(opcode, var); }
        @Override public void visitTypeInsn(int opcode, String type) { flush(); mv.visitTypeInsn(opcode, type); }
        @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            flush();
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
        @Override public void visitInvokeDynamicInsn(String name, String desc,
                com.codename1.tools.translator.classfile.Handle bsm, Object... bsmArgs) {
            flush();
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
        @Override public void visitJumpInsn(int opcode, Label label) { flush(); mv.visitJumpInsn(opcode, label); }
        @Override public void visitLabel(Label label) { flush(); mv.visitLabel(label); }
        @Override public void visitIincInsn(int var, int increment) { flush(); mv.visitIincInsn(var, increment); }
        @Override public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            flush();
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }
        @Override public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            flush();
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }
        @Override public void visitMultiANewArrayInsn(String desc, int dims) { flush(); mv.visitMultiANewArrayInsn(desc, dims); }
        @Override public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            flush();
            mv.visitTryCatchBlock(start, end, handler, type);
        }
        @Override public void visitLineNumber(int line, Label start) { flush(); mv.visitLineNumber(line, start); }
        @Override public void visitMaxs(int maxStack, int maxLocals) { flush(); mv.visitMaxs(maxStack, maxLocals); }
        @Override public void visitEnd() { flush(); mv.visitEnd(); }
    }

    /// Records a `META-INF/services` file met among the inputs. Returns true when `f` is
    /// one.
    static boolean recordResource(File f) throws IOException {
        File parent = f.getParentFile();
        if (parent == null || !"services".equals(parent.getName()) || parent.getParentFile() == null
                || !"META-INF".equals(parent.getParentFile().getName())) {
            return false;
        }
        String text;
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            text = new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        Set<String> providers;
        synchronized (SERVICES) {
            providers = SERVICES.get(f.getName());
            if (providers == null) {
                providers = new LinkedHashSet<String>();
                SERVICES.put(f.getName(), providers);
            }
        }
        int start = 0;
        while (start <= text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(start, end);
            start = end + 1;
            int hash = line.indexOf('#');
            String name = (hash >= 0 ? line.substring(0, hash) : line).trim();
            if (!name.isEmpty()) {
                providers.add(name);
            }
        }
        return true;
    }

    /// Parses the generated classes and writes the C bodies of their natives. Runs once
    /// every input is parsed and before anything is emitted.
    static void writeSynthetics(File outputDirectory) throws IOException {
        boolean javascript = ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT;
        StringBuilder c = new StringBuilder();
        Set<String> headers = new LinkedHashSet<String>();
        for (Updater u : UPDATERS.values()) {
            ByteCodeField field = findField(u.owner, u.field);
            if (field == null || field.isStaticField()) {
                System.out.println("WARNING: " + u.updaterClass.substring(u.updaterClass.lastIndexOf('/') + 1)
                        + " names " + u.owner.replace('/', '.') + "." + u.field
                        + ", which is not an instance field of a translated class");
                continue;
            }
            String desc = field.getDescriptor();
            emitUpdaterClass(u, desc, !javascript);
            if (!javascript) {
                headers.add(Util.mangle(u.owner));
                headers.add(Util.mangle(u.generated));
                appendCasBody(c, u, field);
            }
        }
        emitRegistry();
        File out = new File(outputDirectory, GENERATED_C + "." + ByteCodeTranslator.output.extension());
        if (javascript || c.length() == 0) {
            if (out.exists() && !out.delete()) {
                throw new IOException("Could not remove " + out);
            }
            return;
        }
        StringBuilder file = new StringBuilder();
        file.append("// Generated by the translator: compare-and-set for the field updaters.\n");
        file.append("#include \"cn1_globals.h\"\n#include <stdatomic.h>\n");
        for (String h : headers) {
            file.append("#include \"").append(h).append(".h\"\n");
        }
        file.append(c);
        try (OutputStream o = new FileOutputStream(out)) {
            o.write(file.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static ByteCodeField findField(String owner, String name) {
        // ByteCodeField keeps the mangled name ($ and - folded to _).
        String mangled = name.replace('$', '_').replace('-', '_');
        ByteCodeClass bc = Parser.getClassObject(Util.mangle(owner));
        while (bc != null) {
            for (ByteCodeField f : bc.getFields()) {
                if (f.getFieldName().equals(mangled)) {
                    return f;
                }
            }
            String base = bc.getBaseClass();
            bc = base == null ? null : Parser.getClassObject(Util.mangle(base));
        }
        return null;
    }

    private static String valueDesc(Updater u) {
        return u.kind == 'L' ? "Ljava/lang/Object;" : String.valueOf(u.kind);
    }

    private static void emitUpdaterClass(final Updater u, final String fieldDesc, final boolean nativeCas) {
        Parser.parseSynthetic(u.generated, new Parser.SyntheticEmitter() {
            @Override
            public void emit(ClassVisitor cv) {
                String v = valueDesc(u);
                cv.visit(CLASS_VERSION_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                        u.generated, null, u.updaterClass, new String[0]);

                MethodVisitor m = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                m.visitCode();
                m.visitVarInsn(Opcodes.ALOAD, 0);
                m.visitMethodInsn(Opcodes.INVOKESPECIAL, u.updaterClass, "<init>", "()V", false);
                m.visitInsn(Opcodes.RETURN);
                m.visitMaxs(1, 1);
                m.visitEnd();

                m = cv.visitMethod(Opcodes.ACC_PUBLIC, "get", "(Ljava/lang/Object;)" + v, null, null);
                m.visitCode();
                m.visitVarInsn(Opcodes.ALOAD, 1);
                m.visitTypeInsn(Opcodes.CHECKCAST, u.owner);
                m.visitFieldInsn(Opcodes.GETFIELD, u.owner, u.field, fieldDesc);
                m.visitInsn(returnOp(u.kind));
                m.visitMaxs(2, 2);
                m.visitEnd();

                m = cv.visitMethod(Opcodes.ACC_PUBLIC, "set", "(Ljava/lang/Object;" + v + ")V", null, null);
                m.visitCode();
                m.visitVarInsn(Opcodes.ALOAD, 1);
                m.visitTypeInsn(Opcodes.CHECKCAST, u.owner);
                m.visitVarInsn(loadOp(u.kind), 2);
                if (u.kind == 'L') {
                    m.visitTypeInsn(Opcodes.CHECKCAST, Type.getType(fieldDesc).getInternalName());
                }
                m.visitFieldInsn(Opcodes.PUTFIELD, u.owner, u.field, fieldDesc);
                m.visitInsn(Opcodes.RETURN);
                m.visitMaxs(4, 4);
                m.visitEnd();

                // compareAndSet(obj, expect, update): null-check, then cas0.
                int wide = u.kind == 'J' ? 2 : 1;
                String casDesc = "(Ljava/lang/Object;" + v + v + ")Z";
                m = cv.visitMethod(Opcodes.ACC_PUBLIC, "compareAndSet", casDesc, null, null);
                m.visitCode();
                m.visitVarInsn(Opcodes.ALOAD, 1);
                m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                m.visitVarInsn(loadOp(u.kind), 2);
                m.visitVarInsn(loadOp(u.kind), 2 + wide);
                m.visitMethodInsn(Opcodes.INVOKESTATIC, u.generated, "cas0", casDesc, false);
                m.visitInsn(Opcodes.IRETURN);
                m.visitMaxs(6, 2 + 2 * wide);
                m.visitEnd();

                if (nativeCas) {
                    m = cv.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE | Opcodes.ACC_PRIVATE, "cas0", casDesc, null, null);
                    m.visitEnd();
                } else {
                    // One host thread: read, compare, and write back either the update or
                    // what was there, with nothing able to run in between.
                    int same = 1 + 2 * wide;
                    String selDesc = "(Z" + v + v + ")" + v;
                    m = cv.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "cas0", casDesc, null, null);
                    m.visitCode();
                    m.visitVarInsn(Opcodes.ALOAD, 0);
                    m.visitTypeInsn(Opcodes.CHECKCAST, u.owner);
                    m.visitFieldInsn(Opcodes.GETFIELD, u.owner, u.field, fieldDesc);
                    m.visitVarInsn(loadOp(u.kind), 1);
                    m.visitMethodInsn(Opcodes.INVOKESTATIC, u.updaterClass, "cn1Same", "(" + v + v + ")Z", false);
                    m.visitVarInsn(Opcodes.ISTORE, same);
                    m.visitVarInsn(Opcodes.ALOAD, 0);
                    m.visitTypeInsn(Opcodes.CHECKCAST, u.owner);
                    m.visitVarInsn(Opcodes.ILOAD, same);
                    m.visitVarInsn(loadOp(u.kind), 1 + wide);
                    m.visitVarInsn(Opcodes.ALOAD, 0);
                    m.visitTypeInsn(Opcodes.CHECKCAST, u.owner);
                    m.visitFieldInsn(Opcodes.GETFIELD, u.owner, u.field, fieldDesc);
                    m.visitMethodInsn(Opcodes.INVOKESTATIC, u.updaterClass, "cn1Select", selDesc, false);
                    if (u.kind == 'L') {
                        m.visitTypeInsn(Opcodes.CHECKCAST, Type.getType(fieldDesc).getInternalName());
                    }
                    m.visitFieldInsn(Opcodes.PUTFIELD, u.owner, u.field, fieldDesc);
                    m.visitVarInsn(Opcodes.ILOAD, same);
                    m.visitInsn(Opcodes.IRETURN);
                    m.visitMaxs(8, same + 1);
                    m.visitEnd();
                }
                cv.visitEnd();
            }
        });
    }

    private static int loadOp(char kind) {
        return kind == 'L' ? Opcodes.ALOAD : kind == 'J' ? Opcodes.LLOAD : Opcodes.ILOAD;
    }

    private static int returnOp(char kind) {
        return kind == 'L' ? Opcodes.ARETURN : kind == 'J' ? Opcodes.LRETURN : Opcodes.IRETURN;
    }

    /// The C body of one updater's `cas0`.
    private static void appendCasBody(StringBuilder c, Updater u, ByteCodeField field) {
        ByteCodeClass gen = Parser.getClassObject(Util.mangle(u.generated));
        BytecodeMethod cas = null;
        for (BytecodeMethod m : gen.getMethods()) {
            if ("cas0".equals(m.getMethodName())) {
                cas = m;
            }
        }
        StringBuilder header = new StringBuilder();
        cas.appendMethodHeader(header, Util.mangle(u.generated));
        String proto = header.toString().trim();
        proto = proto.substring(0, proto.length() - 1);
        String ctype = u.kind == 'L' ? "JAVA_OBJECT" : u.kind == 'J' ? "JAVA_LONG" : "JAVA_INT";
        String member = "((struct obj__" + Util.mangle(u.owner) + "*)__cn1Arg1)->"
                + field.getClsName() + "_" + field.getFieldName();
        c.append('\n').append(proto).append(" {\n");
        c.append("    ").append(ctype).append(" __cn1Expect = __cn1Arg2;\n");
        if (u.kind == 'L') {
            c.append("    CN1_WRITE_BARRIER(__cn1Arg1, __cn1Arg3);\n");
            c.append("    CN1_SATB_DELETE(&").append(member).append(");\n");
        }
        if (field.isVolatile()) {
            c.append("    return atomic_compare_exchange_strong(&").append(member)
                    .append(", &__cn1Expect, __cn1Arg3) ? JAVA_TRUE : JAVA_FALSE;\n");
        } else {
            c.append("    return __atomic_compare_exchange_n(&").append(member)
                    .append(", &__cn1Expect, __cn1Arg3, 0, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST) ? JAVA_TRUE : JAVA_FALSE;\n");
        }
        c.append("}\n");
    }

    /// `ServiceLoader$Registry.providers(Class)`: compares the class against each
    /// service and returns a fresh array of its providers.
    private static void emitRegistry() {
        final Map<String, List<String>> table = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, Set<String>> e : SERVICES.entrySet()) {
            String service = e.getKey().replace('.', '/');
            if (Parser.getClassObject(Util.mangle(service)) == null) {
                continue;
            }
            List<String> impls = new ArrayList<String>();
            for (String p : e.getValue()) {
                String internal = p.replace('.', '/');
                ByteCodeClass bc = Parser.getClassObject(Util.mangle(internal));
                if (bc == null) {
                    // A provider whose class was not translated: the JDK would fail to
                    // load it too, and the loader skips it.
                    continue;
                }
                impls.add(internal);
            }
            if (!impls.isEmpty()) {
                table.put(service, impls);
            }
        }
        Parser.parseSynthetic(REGISTRY, new Parser.SyntheticEmitter() {
            @Override
            public void emit(ClassVisitor cv) {
                cv.visit(CLASS_VERSION_8, Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC, REGISTRY, null,
                        "java/lang/Object", new String[0]);
                MethodVisitor m = cv.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
                m.visitCode();
                m.visitVarInsn(Opcodes.ALOAD, 0);
                m.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                m.visitInsn(Opcodes.RETURN);
                m.visitMaxs(1, 1);
                m.visitEnd();

                m = cv.visitMethod(Opcodes.ACC_STATIC, "providers", "(Ljava/lang/Class;)[Ljava/lang/Object;", null, null);
                m.visitCode();
                for (Map.Entry<String, List<String>> e : table.entrySet()) {
                    Label next = new Label();
                    m.visitVarInsn(Opcodes.ALOAD, 0);
                    m.visitLdcInsn(Type.getObjectType(e.getKey()));
                    m.visitJumpInsn(Opcodes.IF_ACMPNE, next);
                    List<String> impls = e.getValue();
                    pushInt(m, impls.size());
                    m.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                    for (int i = 0; i < impls.size(); i++) {
                        m.visitInsn(Opcodes.DUP);
                        pushInt(m, i);
                        m.visitTypeInsn(Opcodes.NEW, impls.get(i));
                        m.visitInsn(Opcodes.DUP);
                        m.visitMethodInsn(Opcodes.INVOKESPECIAL, impls.get(i), "<init>", "()V", false);
                        m.visitInsn(Opcodes.AASTORE);
                    }
                    m.visitInsn(Opcodes.ARETURN);
                    m.visitLabel(next);
                }
                m.visitInsn(Opcodes.ICONST_0);
                m.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                m.visitInsn(Opcodes.ARETURN);
                m.visitMaxs(6, 1);
                m.visitEnd();
                cv.visitEnd();
            }
        });
    }

    private static void pushInt(MethodVisitor m, int v) {
        if (v <= 5) {
            m.visitInsn(Opcodes.ICONST_0 + v);
        } else {
            m.visitIntInsn(Opcodes.SIPUSH, v);
        }
    }
}
