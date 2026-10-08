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

/// Redirects single members of JDK classes the device HAS, but with fewer
/// members than the JDK's, to the static methods that stand in for them.
///
/// A class the device lacks altogether is moved whole by a row of
/// [Relocation#JDK_SHIMS]. That cannot work for `java.lang.Class`,
/// `ClassLoader`, `Thread`, `java.util.Objects` or `java.util.Locale`: the
/// device has each of them, every other class is compiled against the
/// device's, and only some of their members are missing or -- for the
/// resource lookups -- mean something else where there is no classpath. So
/// the call is rewritten instead and the class is left alone:
///
/// - an instance method becomes a static one taking the receiver first:
///   `cls.getResource(name)` is `Resources.getResource(cls, name)`;
/// - a static method or a static field read moves to the member of the same
///   name: `Locale.FRANCE` is `JdkLocale.FRANCE`;
/// - a constructor the device lacks becomes the one it has, with the
///   arguments adjusted in place.
///
/// The same is done to a method reference (`Objects::isNull`), which a class
/// file holds as a method handle among the arguments of an `invokedynamic`.
///
/// #### Where it runs
///
/// Inside [ClassRelocator#remap], BEFORE the names are relocated, so every
/// rule is written in the names an application is compiled against and the
/// relocation that follows moves the `java/net/URL` in a rewritten
/// descriptor like any other. It runs only when a desktop layer is active:
/// the targets live in the shared JDK classes, and an application of the
/// Android layer alone was never compiled against a JDK that has these
/// members. The shared JDK classes themselves are not rewritten --
/// `Resources` reads the bundle through the very `Class.getResourceAsStream`
/// an application's call is redirected away from -- and neither is the
/// Android runtime, which already asks for resources by their flat names.
///
/// A rewritten class holds nothing a rule matches, so a second run over it
/// changes nothing.
///
/// #### What is deliberately absent
///
/// Anything else of `ClassLoader` -- `loadClass`, `defineClass`, a
/// `URLClassLoader` -- has no rule and so reaches the compliance check as the
/// build error it is: a device loads no classes at run time.
final class CompatRewrites {

    private static final String RESOURCES = Relocation.JDK_PACKAGE + "Resources";
    private static final String OBJECTS = Relocation.JDK_PACKAGE + "JdkObjects";
    private static final String LOCALE = Relocation.JDK_PACKAGE + "JdkLocale";

    private static final String JAVA_LOCALE = "java/util/Locale";
    private static final String STRING = "Ljava/lang/String;";

    /// Instance methods, as `owner.name descriptor`, to the class holding
    /// the static method that takes the receiver as its first parameter.
    private static final Map<String, String> VIRTUAL = new HashMap<String, String>();
    /// Static methods, as `owner.name descriptor`, to the class holding the
    /// static method of the same name and descriptor.
    private static final Map<String, String> STATIC = new HashMap<String, String>();
    /// Static fields, as `owner.name`, to the class holding the field of the
    /// same name and type.
    private static final Map<String, String> FIELDS = new HashMap<String, String>();

    static {
        String url = "(Ljava/lang/String;)Ljava/net/URL;";
        String stream = "(Ljava/lang/String;)Ljava/io/InputStream;";
        String urls = "(Ljava/lang/String;)Ljava/util/Enumeration;";
        VIRTUAL.put("java/lang/Class.getResource" + url, RESOURCES);
        VIRTUAL.put("java/lang/Class.getResourceAsStream" + stream, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResource" + url, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResourceAsStream" + stream, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResources" + urls, RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResource" + url, RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResourceAsStream" + stream, RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResources" + urls, RESOURCES);
        VIRTUAL.put("java/lang/Thread.getContextClassLoader()Ljava/lang/ClassLoader;", RESOURCES);
        VIRTUAL.put("java/lang/Thread.setContextClassLoader(Ljava/lang/ClassLoader;)V", RESOURCES);

        String supplier = "Ljava/util/function/Supplier;";
        String object = "Ljava/lang/Object;";
        STATIC.put("java/util/Objects.isNull(" + object + ")Z", OBJECTS);
        STATIC.put("java/util/Objects.requireNonNull(" + object + supplier + ")" + object, OBJECTS);
        STATIC.put("java/util/Objects.requireNonNullElse(" + object + object + ")" + object, OBJECTS);
        STATIC.put("java/util/Objects.requireNonNullElseGet(" + object + supplier + ")" + object, OBJECTS);
        STATIC.put("java/util/Objects.checkIndex(II)I", OBJECTS);
        STATIC.put("java/util/Objects.checkFromToIndex(III)I", OBJECTS);
        STATIC.put("java/util/Objects.checkFromIndexSize(III)I", OBJECTS);

        STATIC.put(JAVA_LOCALE + ".forLanguageTag(" + STRING + ")Ljava/util/Locale;", LOCALE);
        STATIC.put(JAVA_LOCALE + ".getAvailableLocales()[Ljava/util/Locale;", LOCALE);
        for (String getter : new String[] {"toLanguageTag", "getVariant", "getScript", "getDisplayName",
            "getDisplayLanguage", "getDisplayCountry", "toString"}) {
            VIRTUAL.put(JAVA_LOCALE + "." + getter + "()" + STRING, LOCALE);
        }
        VIRTUAL.put(JAVA_LOCALE + ".equals(" + object + ")Z", LOCALE);
        VIRTUAL.put(JAVA_LOCALE + ".hashCode()I", LOCALE);
        for (String constant : new String[] {"ENGLISH", "FRENCH", "GERMAN", "ITALIAN", "JAPANESE", "KOREAN",
            "CHINESE", "SIMPLIFIED_CHINESE", "TRADITIONAL_CHINESE", "FRANCE", "GERMANY", "ITALY", "JAPAN", "KOREA",
            "CHINA", "PRC", "TAIWAN", "UK", "US", "CANADA", "CANADA_FRENCH", "ROOT"}) {
            FIELDS.put(JAVA_LOCALE + "." + constant, LOCALE);
        }
    }

    private CompatRewrites() {
    }

    /// Whether the class named `internalName` is one whose calls are left as
    /// they are; see the class description.
    private static boolean exempt(String internalName) {
        return internalName.startsWith(Relocation.JDK_PACKAGE)
                || internalName.startsWith(AndroidRemapper.RELOCATION.target());
    }

    /// A visitor that applies the rules to the class it is shown and passes
    /// the result to `next`.
    static ClassVisitor visitor(ClassVisitor next) {
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
        /// How much deeper than the compiler counted the operand stack gets:
        /// one slot, once a one-argument `Locale` constructor was widened.
        private int extraStack;

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
            } else if (opcode == Opcodes.INVOKEVIRTUAL) {
                String target = VIRTUAL.get(owner + "." + name + descriptor);
                if (target != null) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, target, name, receiverFirst(owner, descriptor), false);
                    return;
                }
            } else if (opcode == Opcodes.INVOKESPECIAL && JAVA_LOCALE.equals(owner) && "<init>".equals(name)) {
                // The device's Locale is a language and a country. A locale
                // of a language alone has the empty country; a variant has
                // nowhere to go and is dropped.
                if (("(" + STRING + ")V").equals(descriptor)) {
                    super.visitLdcInsn("");
                    extraStack = 1;
                    super.visitMethodInsn(opcode, owner, name, "(" + STRING + STRING + ")V", false);
                    return;
                }
                if (("(" + STRING + STRING + STRING + ")V").equals(descriptor)) {
                    super.visitInsn(Opcodes.POP);
                    super.visitMethodInsn(opcode, owner, name, "(" + STRING + STRING + ")V", false);
                    return;
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == Opcodes.GETSTATIC) {
                String target = FIELDS.get(owner + "." + name);
                if (target != null) {
                    super.visitFieldInsn(opcode, target, name, descriptor);
                    return;
                }
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
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
        private static Handle rewrite(Handle h) {
            String key = h.getOwner() + "." + h.getName() + h.getDesc();
            if (h.getTag() == Opcodes.H_INVOKESTATIC) {
                String target = STATIC.get(key);
                if (target != null) {
                    return new Handle(Opcodes.H_INVOKESTATIC, target, h.getName(), h.getDesc(), false);
                }
            } else if (h.getTag() == Opcodes.H_INVOKEVIRTUAL) {
                String target = VIRTUAL.get(key);
                if (target != null) {
                    return new Handle(Opcodes.H_INVOKESTATIC, target, h.getName(),
                            receiverFirst(h.getOwner(), h.getDesc()), false);
                }
            }
            return h;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + extraStack, maxLocals);
        }
    }
}
