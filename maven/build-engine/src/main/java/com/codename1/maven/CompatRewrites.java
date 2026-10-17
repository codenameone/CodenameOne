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
/// Everything else of `java.*` stays `java.*`: an application compiled
/// against a JDK runs against the JDK on JavaSE and Android and against the
/// class library (`vm/JavaAPI`) on the other targets, which carries the same
/// API. What is left here:
///
/// - the resource lookups of `Class`, `ClassLoader` and `Thread`, which read
///   the flattened resources through `com.codename1.impl.compat.Resources`,
///   and `ResourceBundle.getBundle` and `ServiceLoader.load`, which have to
///   reach them through the loader a port on a real JDK installs there;
/// - the `String` members that take a regular expression, to
///   `com.codename1.util.regex.StringRegex` (the device has no
///   `java.util.regex`; a class that names one is relocated there by
///   [Relocation#JDK_SHIMS]);
/// - the transcendental functions of `Math` and `StrictMath`, to the
///   framework's `MathUtil`, because the API an application builds against
///   has none;
/// - `Runtime.exec`, to `com.codename1.impl.process.RuntimeExec` over the
///   framework's `ProcessBuilder`;
/// - `Thread.setDefaultUncaughtExceptionHandler`, to
///   `com.codename1.impl.compat.UncaughtExceptions`, which also hands the
///   handler what event dispatch thread tasks throw.
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
/// itself is not rewritten.
///
/// A rewritten class holds nothing a rule matches, so a second run over it
/// changes nothing.
///
/// #### What is deliberately absent
///
/// Anything else of `ClassLoader` -- `defineClass`, a `URLClassLoader` -- has
/// no rule and so reaches the compliance check as the build error it is: a
/// device loads no classes at run time. `loadClass(String)` is the one
/// exception, because all it does with the one loader there is, is what
/// `Class.forName` does: answer a class the application already has.
final class CompatRewrites {

    private static final String RESOURCES = "com/codename1/impl/compat/Resources";
    private static final String UNCAUGHT = "com/codename1/impl/compat/UncaughtExceptions";
    private static final String REGEX = "com/codename1/util/regex/StringRegex";
    private static final String RUNTIME_EXEC = "com/codename1/impl/process/RuntimeExec";
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
        String urls = "(Ljava/lang/String;)Ljava/util/Enumeration;";
        VIRTUAL.put("java/lang/Class.getResource" + url, RESOURCES);
        VIRTUAL.put("java/lang/Class.getResourceAsStream" + stream, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResource" + url, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResourceAsStream" + stream, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResources" + urls, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.loadClass(Ljava/lang/String;)Ljava/lang/Class;", RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResource" + url, RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResourceAsStream" + stream, RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResources" + urls, RESOURCES);
        VIRTUAL.put("java/lang/Thread.getContextClassLoader()Ljava/lang/ClassLoader;", RESOURCES);
        VIRTUAL.put("java/lang/Thread.setContextClassLoader(Ljava/lang/ClassLoader;)V", RESOURCES);

        String bundle = "Ljava/util/ResourceBundle;";
        STATIC.put("java/util/ResourceBundle.getBundle(" + STRING + ")" + bundle, RESOURCES);
        STATIC.put("java/util/ResourceBundle.getBundle(" + STRING + "Ljava/util/Locale;)" + bundle, RESOURCES);
        STATIC.put("java/util/ServiceLoader.load(Ljava/lang/Class;)Ljava/util/ServiceLoader;", RESOURCES);

        STATIC.put("java/lang/Thread.setDefaultUncaughtExceptionHandler("
                + "Ljava/lang/Thread$UncaughtExceptionHandler;)V", UNCAUGHT);

        String str = "java/lang/String.";
        VIRTUAL.put(str + "matches(" + STRING + ")Z", REGEX);
        VIRTUAL.put(str + "split(" + STRING + ")[" + STRING, REGEX);
        VIRTUAL.put(str + "split(" + STRING + "I)[" + STRING, REGEX);
        VIRTUAL.put(str + "replaceAll(" + STRING + STRING + ")" + STRING, REGEX);
        VIRTUAL.put(str + "replaceFirst(" + STRING + STRING + ")" + STRING, REGEX);

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
    /// names already.
    private static boolean exempt(String internalName) {
        return internalName.startsWith(AndroidRemapper.RELOCATION.target());
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
