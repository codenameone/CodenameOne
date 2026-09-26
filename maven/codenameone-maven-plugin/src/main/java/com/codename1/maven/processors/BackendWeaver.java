/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

/// Rewrites the compiled classes of a backend module so the generated wiring can
/// reach what it needs without reflection, and so annotated methods carry their
/// aspects.
///
/// Three rewrites, each decided by the class alone -- so a class that was not
/// recompiled is rewritten the same way every build, and a class that carries the
/// marker field has already been:
///
/// 1. **Injection setters.** An `@Autowired` or `@Value` field gets a public
///    `cn1$inject$<field>(value)`, which the entry point calls. The field keeps
///    its visibility; nothing but the setter can write it from outside.
/// 2. **Bridges.** A non-public method or constructor the entry point must call
///    -- `@PostConstruct`, a `@Bean` factory, a scheduled job, a package-private
///    constructor -- gets a public `cn1$<name>` twin (a static `cn1$new` for a
///    constructor) that calls it.
/// 3. **Aspects.** A `@Transactional`, `@Async`, `@Timed` or `@Counted` method
///    keeps its name, descriptor, access and annotations, but its body moves to
///    a package-private `<name>$cn1body`; the method itself becomes one static call
///    into `<Class>Cn1Aspects`, a class generated as Java source that holds the
///    begin/commit, the task submission or the clock reads. Straight-line
///    bytecode here and javac-compiled code there, so no stack map frame is ever
///    written by hand.
///
/// Because the method itself carries the aspect rather than a proxy around the
/// object, a self-call, a private method and an instance built with `new` get it
/// too.
final class BackendWeaver {
    /// A static field whose presence says a class has been rewritten.
    static final String MARKER = "cn1$woven";
    static final String BODY_SUFFIX = "$cn1body";
    static final String INJECT_PREFIX = "cn1$inject$";
    static final String BRIDGE_PREFIX = "cn1$";
    static final String NEW_BRIDGE = "cn1$new";

    /// What to do to one class.
    static final class Plan {
        final String internalName;
        final String helperInternalName;
        /// field name -> descriptor
        final Map<String, String> injectFields = new LinkedHashMap<String, String>();
        /// name + descriptor of non-public methods to bridge
        final Set<String> bridges = new LinkedHashSet<String>();
        /// the subset of [#bridges] that are static
        final Set<String> staticBridges = new LinkedHashSet<String>();
        /// the subset of [#bridges] that are private, called with INVOKESPECIAL
        final Set<String> privateBridges = new LinkedHashSet<String>();
        /// descriptors of non-public constructors to bridge
        final Set<String> constructors = new LinkedHashSet<String>();
        /// name + descriptor of methods to give aspects
        final Set<String> aspects = new LinkedHashSet<String>();

        Plan(String internalName, String helperInternalName) {
            this.internalName = internalName;
            this.helperInternalName = helperInternalName;
        }

        boolean isEmpty() {
            return injectFields.isEmpty() && bridges.isEmpty() && constructors.isEmpty()
                    && aspects.isEmpty();
        }
    }

    private BackendWeaver() {
    }

    /// The name the entry point calls to inject `field`.
    static String injectSetter(String field) {
        return INJECT_PREFIX + field;
    }

    /// The name of a non-public method's public bridge.
    static String bridge(String method) {
        return BRIDGE_PREFIX + method;
    }

    /// Rewrites one class file in place. Answers false when it was already
    /// rewritten, or has nothing to rewrite.
    static boolean weave(File outputDir, Plan plan) throws IOException {
        if (plan.isEmpty()) {
            return false;
        }
        File file = new File(outputDir, plan.internalName + ".class");
        if (!file.isFile()) {
            throw new IOException("Cannot weave " + plan.internalName + ": no class file at "
                    + file);
        }
        byte[] original = Files.readAllBytes(file.toPath());
        byte[] result = transform(original, plan);
        if (result == null) {
            return false;
        }
        Files.write(file.toPath(), result);
        return true;
    }

    /// The rewritten class, or null when it already carries the marker.
    static byte[] transform(byte[] original, final Plan plan) {
        ClassReader reader = new ClassReader(original);
        final boolean[] marked = {false};
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                if (MARKER.equals(name)) {
                    marked[0] = true;
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (marked[0]) {
            return null;
        }
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        final String owner = plan.internalName;
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (!plan.aspects.contains(name + descriptor)) {
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
                // The body, renamed and package-private, without annotations: it is
                // an implementation detail, and a second copy of @GetMapping or
                // @Scheduled would be read as a second declaration.
                //
                // `synchronized` moves WITH the body, off the stub. The monitor
                // guards the code its author wrote, so it has to be held where
                // that code runs: an @Async body runs on an executor thread, and a
                // lock the stub took only while enqueueing let two workers run
                // the body at once. On the body it also matches Spring for
                // @Transactional, whose transaction wraps the synchronized call.
                int bodyAccess = access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED
                        | Opcodes.ACC_PRIVATE | Opcodes.ACC_VARARGS | Opcodes.ACC_FINAL);
                MethodVisitor body = writer.visitMethod(bodyAccess, name + BODY_SUFFIX,
                        descriptor, signature, exceptions);
                MethodVisitor stub = writer.visitMethod(access & ~Opcodes.ACC_SYNCHRONIZED,
                        name, descriptor, signature, exceptions);
                return new AspectSplitter(body, stub, owner, plan.helperInternalName, access,
                        name, descriptor);
            }

            @Override
            public void visitEnd() {
                FieldVisitor marker = writer.visitField(Opcodes.ACC_PRIVATE
                        | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                        MARKER, "I", null, Integer.valueOf(1));
                marker.visitEnd();
                for (Map.Entry<String, String> field : plan.injectFields.entrySet()) {
                    emitSetter(writer, owner, field.getKey(), field.getValue());
                }
                for (String key : plan.bridges) {
                    int paren = key.indexOf('(');
                    emitBridge(writer, owner, key.substring(0, paren), key.substring(paren),
                            plan.staticBridges.contains(key), plan.privateBridges.contains(key));
                }
                for (String descriptor : plan.constructors) {
                    emitConstructorBridge(writer, owner, descriptor);
                }
                super.visitEnd();
            }
        }, 0);
        return writer.toByteArray();
    }

    private static void emitSetter(ClassWriter writer, String owner, String field,
                                   String descriptor) {
        MethodVisitor m = writer.visitMethod(Opcodes.ACC_PUBLIC, injectSetter(field),
                "(" + descriptor + ")V", null, null);
        m.visitCode();
        m.visitVarInsn(Opcodes.ALOAD, 0);
        m.visitVarInsn(Type.getType(descriptor).getOpcode(Opcodes.ILOAD), 1);
        m.visitFieldInsn(Opcodes.PUTFIELD, owner, field, descriptor);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    private static void emitBridge(ClassWriter writer, String owner, String name,
                                   String descriptor, boolean isStatic, boolean isPrivate) {
        MethodVisitor m = writer.visitMethod(Opcodes.ACC_PUBLIC
                | (isStatic ? Opcodes.ACC_STATIC : 0), bridge(name), descriptor, null, null);
        m.visitCode();
        int slot = 0;
        if (!isStatic) {
            m.visitVarInsn(Opcodes.ALOAD, 0);
            slot = 1;
        }
        for (Type arg : Type.getArgumentTypes(descriptor)) {
            m.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), slot);
            slot += arg.getSize();
        }
        // INVOKESPECIAL for a private instance method, as javac emits for class
        // files of this age; INVOKEVIRTUAL otherwise, so an override in a
        // subclass is the one called, as it would be by a direct call.
        m.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC
                : isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, owner, name,
                descriptor, false);
        m.visitInsn(Type.getReturnType(descriptor).getOpcode(Opcodes.IRETURN));
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    private static void emitConstructorBridge(ClassWriter writer, String owner,
                                              String descriptor) {
        Type[] args = Type.getArgumentTypes(descriptor);
        MethodVisitor m = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                NEW_BRIDGE, Type.getMethodDescriptor(Type.getObjectType(owner), args), null,
                null);
        m.visitCode();
        m.visitTypeInsn(Opcodes.NEW, owner);
        m.visitInsn(Opcodes.DUP);
        int slot = 0;
        for (Type arg : args) {
            m.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), slot);
            slot += arg.getSize();
        }
        m.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", descriptor, false);
        m.visitInsn(Opcodes.ARETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    /// Sends the method's code to the renamed body, its annotations to the stub,
    /// and writes the stub's one call when the method ends.
    private static final class AspectSplitter extends MethodVisitor {
        private final MethodVisitor stub;
        private final String owner;
        private final String helper;
        private final int access;
        private final String name;
        private final String descriptor;

        AspectSplitter(MethodVisitor body, MethodVisitor stub, String owner, String helper,
                       int access, String name, String descriptor) {
            super(Opcodes.ASM9, body);
            this.stub = stub;
            this.owner = owner;
            this.helper = helper;
            this.access = access;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public void visitParameter(String parameterName, int parameterAccess) {
            stub.visitParameter(parameterName, parameterAccess);
            super.visitParameter(parameterName, parameterAccess);
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return stub.visitAnnotationDefault();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return stub.visitAnnotation(desc, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                     String desc, boolean visible) {
            return stub.visitTypeAnnotation(typeRef, typePath, desc, visible);
        }

        @Override
        public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
            stub.visitAnnotableParameterCount(parameterCount, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc,
                                                          boolean visible) {
            return stub.visitParameterAnnotation(parameter, desc, visible);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            stub.visitCode();
            int slot = 0;
            StringBuilder helperDescriptor = new StringBuilder("(");
            if (!isStatic) {
                stub.visitVarInsn(Opcodes.ALOAD, 0);
                helperDescriptor.append('L').append(owner).append(';');
                slot = 1;
            }
            for (Type arg : Type.getArgumentTypes(descriptor)) {
                stub.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), slot);
                slot += arg.getSize();
                helperDescriptor.append(arg.getDescriptor());
            }
            Type ret = Type.getReturnType(descriptor);
            helperDescriptor.append(')').append(ret.getDescriptor());
            stub.visitMethodInsn(Opcodes.INVOKESTATIC, helper, name,
                    helperDescriptor.toString(), false);
            stub.visitInsn(ret.getOpcode(Opcodes.IRETURN));
            stub.visitMaxs(0, 0);
            stub.visitEnd();
        }
    }
}
