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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

/// Redirects the few members of JDK classes that the Codename One class
/// library has no equivalent for, or that mean something else where there is
/// no class path, to the static methods that stand in for them.
///
/// The `java.*` classes a device has stay `java.*`. The ones it has not --
/// files, `java.nio.file`, `URL`, bundles, logging and preferences -- are
/// renamed onto the desktop runtime by [CompatLayers#DESKTOP_JDK_SHIMS]. What
/// is left here are single members:
///
/// - the resource lookups of `Class`, which read the flattened resources
///   through the desktop runtime's `Resources`
///   ([CompatLayers#DESKTOP_RUNTIME]);
/// - the transcendental functions of `Math` and `StrictMath`, to the
///   framework's `MathUtil`, because the API an application builds against
///   has none;
/// - `Runtime.exec`, to the desktop runtime's `RuntimeExec` over the
///   framework's `ProcessBuilder`;
/// - `Thread.setDefaultUncaughtExceptionHandler`, to the desktop runtime's
///   `UncaughtExceptions`, which also hands the handler what event dispatch
///   thread tasks throw.
///
/// An instance method becomes a static one taking the receiver first:
/// `cls.getResource(name)` is `Resources.getResource(cls, name)`. A static
/// method moves to the member of the same name. The same is done to a method
/// reference, which a class file holds as a method handle among the
/// arguments of an `invokedynamic`.
///
/// #### Where it runs
///
/// Inside [ClassRelocator#remap], BEFORE the names are relocated, so every
/// rule is written in the names an application is compiled against. It runs
/// only when a desktop layer is active: an application of the Android layer
/// alone asks for resources by their flat names already. The Android runtime
/// and the desktop runtime -- whose methods ARE the stand-ins, and would call
/// themselves -- are not rewritten.
///
/// A rewritten class holds nothing a rule matches, so a second run over it
/// changes nothing.
///
/// #### What is deliberately absent
///
/// `ClassLoader`, `Thread.getContextClassLoader`, `Class.getClassLoader` and
/// `ServiceLoader` have no rule and so reach the compliance check as the
/// build error they are: a device has no class loader and loads no classes
/// at run time.
final class CompatRewrites {

    private static final String RESOURCES = CompatLayers.DESKTOP_RUNTIME_PACKAGE + "Resources";
    private static final String UNCAUGHT = CompatLayers.DESKTOP_RUNTIME_PACKAGE + "UncaughtExceptions";
    private static final String RUNTIME_EXEC = CompatLayers.DESKTOP_RUNTIME_PACKAGE + "RuntimeExec";
    /// The framework's own math class: the API an application builds against
    /// has no transcendental functions in `Math`, and this one has them under
    /// the JDK's names.
    private static final String MATH_UTIL = "com/codename1/util/MathUtil";
    private static final String STRING = "Ljava/lang/String;";

    /// Instance methods, as `owner.name descriptor`, to the class holding
    /// the static method that takes the receiver as its first parameter. An
    /// instance method is looked up here whether it is called on a class or
    /// on an interface.
    private static final Map<String, String> VIRTUAL = new HashMap<String, String>();
    /// Static methods, as `owner.name descriptor`, to the class holding the
    /// static method of the same name and descriptor.
    private static final Map<String, String> STATIC = new HashMap<String, String>();

    static {
        String url = "(Ljava/lang/String;)Ljava/net/URL;";
        String stream = "(Ljava/lang/String;)Ljava/io/InputStream;";
        VIRTUAL.put("java/lang/Class.getResource" + url, RESOURCES);
        VIRTUAL.put("java/lang/Class.getResourceAsStream" + stream, RESOURCES);

        STATIC.put("java/lang/Thread.setDefaultUncaughtExceptionHandler("
                + "Ljava/lang/Thread$UncaughtExceptionHandler;)V", UNCAUGHT);

        // NOT HERE: String's regex members. They are rewritten in every
        // application, whatever layers are active, by BytecodeCompliance's
        // INVOCATION_REWRITE_RULES -- see the comment there. A rule here would
        // only run with a desktop layer, which is exactly the bug that was.

        String strings = "[" + STRING;
        String file = "Ljava/io/File;";
        // RuntimeExec answers the framework's Process. The rewritten call
        // still names java/lang/Process here; the row of Relocation.JDK_SHIMS
        // that moves the class runs after this and moves the descriptor too.
        for (String args : new String[] {STRING, STRING + strings, STRING + strings + file, strings,
            strings + strings, strings + strings + file}) {
            VIRTUAL.put("java/lang/Runtime.exec(" + args + ")Ljava/lang/Process;", RUNTIME_EXEC);
        }

        String math = "java/lang/Math.";
        for (String fn : new String[] {"exp", "log", "log10", "asin", "acos", "atan"}) {
            STATIC.put(math + fn + "(D)D", MATH_UTIL);
        }
        STATIC.put(math + "pow(DD)D", MATH_UTIL);
        STATIC.put(math + "atan2(DD)D", MATH_UTIL);
        // StrictMath is not on a device at all; its results are Math's here.
        String[] keys = STATIC.keySet().toArray(new String[0]);
        for (String key : keys) {
            if (key.startsWith(math)) {
                STATIC.put("java/lang/StrictMath." + key.substring(math.length()), STATIC.get(key));
            }
        }
    }

    private CompatRewrites() {
    }

    /// Whether the class named `internalName` is one whose calls are left as
    /// they are: the Android runtime, which asks for resources by their flat
    /// names already, and the desktop runtime, which the rules point at.
    private static boolean exempt(String internalName) {
        return internalName.startsWith(AndroidRemapper.RELOCATION.target())
                || internalName.startsWith(CompatLayers.DESKTOP_RUNTIME_PACKAGE);
    }

    /// A visitor that applies the rules to the class it is shown and passes
    /// the result to `next`.
    static ClassVisitor visitor(final ClassVisitor next) {
        return new ClassVisitor(Opcodes.ASM9, next) {
            private boolean exempt;

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                exempt = exempt(name);
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return mv == null || exempt ? mv : new Rewriter(mv);
            }
        };
    }

    /// The descriptor of the static method standing in for the instance
    /// method `descriptor` of `owner`.
    private static String receiverFirst(String owner, String descriptor) {
        return "(L" + owner + ";" + descriptor.substring(1);
    }

    private static final class Rewriter extends MethodVisitor {
        Rewriter(MethodVisitor next) {
            super(Opcodes.ASM9, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (opcode == Opcodes.INVOKESTATIC) {
                String target = STATIC.get(owner + "." + name + descriptor);
                if (target != null) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, target, name, descriptor, false);
                    return;
                }
            } else if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
                String target = VIRTUAL.get(owner + "." + name + descriptor);
                if (target != null) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, target, name, receiverFirst(owner, descriptor),
                            false);
                    return;
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... arguments) {
            Object[] rewritten = arguments;
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] instanceof Handle) {
                    Handle moved = rewrite((Handle) arguments[i]);
                    if (moved != arguments[i]) {
                        if (rewritten == arguments) {
                            rewritten = arguments.clone();
                        }
                        rewritten[i] = moved;
                    }
                }
            }
            super.visitInvokeDynamicInsn(name, descriptor, bootstrap, rewritten);
        }

        /// The handle a method reference should hold instead of `h`, or `h`.
        /// A reference to an instance method takes its receiver as the
        /// first argument either way, so the stand-in takes it the same way.
        private static Handle rewrite(Handle h) {
            if (h.getTag() == Opcodes.H_INVOKESTATIC) {
                String target = STATIC.get(h.getOwner() + "." + h.getName() + h.getDesc());
                if (target != null) {
                    return new Handle(Opcodes.H_INVOKESTATIC, target, h.getName(), h.getDesc(), false);
                }
            } else if (h.getTag() == Opcodes.H_INVOKEVIRTUAL || h.getTag() == Opcodes.H_INVOKEINTERFACE) {
                String target = VIRTUAL.get(h.getOwner() + "." + h.getName() + h.getDesc());
                if (target != null) {
                    return new Handle(Opcodes.H_INVOKESTATIC, target, h.getName(),
                            receiverFirst(h.getOwner(), h.getDesc()), false);
                }
            }
            return h;
        }
    }
}
