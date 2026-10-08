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
/// Anything else of `ClassLoader` -- `defineClass`, a `URLClassLoader` -- has
/// no rule and so reaches the compliance check as the build error it is: a
/// device loads no classes at run time. `loadClass(String)` is the one
/// exception, because all it does with the one loader there is, is what
/// `Class.forName` does: answer a class the application already has.
final class CompatRewrites {

    private static final String RESOURCES = Relocation.JDK_PACKAGE + "Resources";
    private static final String OBJECTS = Relocation.JDK_PACKAGE + "JdkObjects";
    private static final String LOCALE = Relocation.JDK_PACKAGE + "JdkLocale";

    private static final String JAVA_LOCALE = "java/util/Locale";
    private static final String STRING = "Ljava/lang/String;";

    private static final String SYSTEM = Relocation.JDK_PACKAGE + "JdkSystem";
    private static final String STRINGS = Relocation.JDK_PACKAGE + "JdkStrings";
    private static final String NUMBERS = Relocation.JDK_PACKAGE + "JdkNumbers";
    /// The framework's own math class: the device's `Math` has no
    /// transcendental functions, and this one has them under the JDK's names.
    private static final String MATH_UTIL = "com/codename1/util/MathUtil";

    /// Instance methods, as `owner.name descriptor`, to the class holding
    /// the static method that takes the receiver as its first parameter.
    ///
    /// In all three tables a target of the form `class#name` names a member
    /// called something else than the one it stands in for, which is how two
    /// methods that differ only in their return type -- `Long.decode` and
    /// `Integer.decode` -- share one class.
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
        VIRTUAL.put("java/lang/ClassLoader.loadClass(Ljava/lang/String;)Ljava/lang/Class;", RESOURCES);
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

    static {
        String sys = "java/lang/System.";
        // getProperty(String) exists on the device and answers null for
        // every key; the rule is what gives user.home a value.
        STATIC.put(sys + "getProperty(" + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "getProperty(" + STRING + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "setProperty(" + STRING + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "clearProperty(" + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "getenv(" + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "getenv()Ljava/util/Map;", SYSTEM);
        STATIC.put(sys + "lineSeparator()" + STRING, SYSTEM);
        // The device's exit ends the process under the framework; this one
        // runs the shutdown hooks and leaves through Display.
        STATIC.put(sys + "exit(I)V", SYSTEM);

        String runtime = "java/lang/Runtime.";
        String thread = "Ljava/lang/Thread;";
        VIRTUAL.put(runtime + "exit(I)V", SYSTEM);
        VIRTUAL.put(runtime + "halt(I)V", SYSTEM);
        VIRTUAL.put(runtime + "addShutdownHook(" + thread + ")V", SYSTEM);
        VIRTUAL.put(runtime + "removeShutdownHook(" + thread + ")Z", SYSTEM);
        VIRTUAL.put(runtime + "availableProcessors()I", SYSTEM);
        VIRTUAL.put(runtime + "maxMemory()J", SYSTEM);

        String thr = "java/lang/Thread.";
        String handler = "Ljava/lang/Thread$UncaughtExceptionHandler;";
        VIRTUAL.put(thr + "setDaemon(Z)V", SYSTEM);
        VIRTUAL.put(thr + "isDaemon()Z", SYSTEM);
        VIRTUAL.put(thr + "setName(" + STRING + ")V", SYSTEM);
        VIRTUAL.put(thr + "isInterrupted()Z", SYSTEM);
        VIRTUAL.put(thr + "getId()J", SYSTEM);
        VIRTUAL.put(thr + "join(J)V", SYSTEM);
        VIRTUAL.put(thr + "join(JI)V", SYSTEM);
        VIRTUAL.put(thr + "setUncaughtExceptionHandler(" + handler + ")V", SYSTEM);
        VIRTUAL.put(thr + "getUncaughtExceptionHandler()" + handler, SYSTEM);
        STATIC.put(thr + "interrupted()Z", SYSTEM);
        STATIC.put(thr + "sleep(JI)V", SYSTEM);
        STATIC.put(thr + "setDefaultUncaughtExceptionHandler(" + handler + ")V", SYSTEM);
        STATIC.put(thr + "getDefaultUncaughtExceptionHandler()" + handler, SYSTEM);
        VIRTUAL.put("java/lang/Throwable.printStackTrace(Ljava/io/PrintWriter;)V", SYSTEM);

        String str = "java/lang/String.";
        String seq = "Ljava/lang/CharSequence;";
        STATIC.put(str + "join(" + seq + "[" + seq + ")" + STRING, STRINGS);
        STATIC.put(str + "join(" + seq + "Ljava/lang/Iterable;)" + STRING, STRINGS);
        STATIC.put(str + "valueOf([C)" + STRING, STRINGS);
        STATIC.put(str + "format(Ljava/util/Locale;" + STRING + "[Ljava/lang/Object;)" + STRING, STRINGS);
        VIRTUAL.put(str + "matches(" + STRING + ")Z", STRINGS);
        VIRTUAL.put(str + "toLowerCase(Ljava/util/Locale;)" + STRING, STRINGS);
        for (String builder : new String[] {"java/lang/StringBuilder", "java/lang/StringBuffer"}) {
            VIRTUAL.put(builder + ".indexOf(" + STRING + ")I", STRINGS);
            VIRTUAL.put(builder + ".indexOf(" + STRING + "I)I", STRINGS);
            VIRTUAL.put(builder + ".lastIndexOf(" + STRING + ")I", STRINGS);
            VIRTUAL.put(builder + ".lastIndexOf(" + STRING + "I)I", STRINGS);
            VIRTUAL.put(builder + ".substring(I)" + STRING, STRINGS);
            VIRTUAL.put(builder + ".substring(II)" + STRING, STRINGS);
            VIRTUAL.put(builder + ".replace(II" + STRING + ")L" + builder + ";", STRINGS);
        }
        VIRTUAL.put("java/lang/StringBuilder.insert(I[C)Ljava/lang/StringBuilder;", STRINGS);

        String chr = "java/lang/Character.";
        STATIC.put(chr + "toString(C)" + STRING, STRINGS);
        for (String test : new String[] {"isLetter", "isLetterOrDigit", "isISOControl"}) {
            STATIC.put(chr + test + "(C)Z", STRINGS);
            STATIC.put(chr + test + "(I)Z", STRINGS);
        }
        for (String test : new String[] {"isAlphabetic", "isDigit", "isLowerCase", "isUpperCase", "isSpaceChar"}) {
            STATIC.put(chr + test + "(I)Z", STRINGS);
        }
        STATIC.put(chr + "toLowerCase(I)I", STRINGS);
        STATIC.put(chr + "toUpperCase(I)I", STRINGS);
        STATIC.put(chr + "forDigit(II)C", STRINGS);
        STATIC.put(chr + "digit(II)I", STRINGS);
        STATIC.put(chr + "getNumericValue(C)I", STRINGS);
        STATIC.put(chr + "getNumericValue(I)I", STRINGS);

        String integer = "java/lang/Integer.";
        for (String op : new String[] {"max", "min", "sum", "rotateLeft", "rotateRight"}) {
            STATIC.put(integer + op + "(II)I", NUMBERS);
        }
        for (String op : new String[] {"bitCount", "highestOneBit", "lowestOneBit", "numberOfTrailingZeros",
            "reverse"}) {
            STATIC.put(integer + op + "(I)I", NUMBERS);
        }
        STATIC.put(integer + "decode(" + STRING + ")Ljava/lang/Integer;", NUMBERS + "#decodeInteger");

        String lng = "java/lang/Long.";
        for (String op : new String[] {"max", "min", "sum"}) {
            STATIC.put(lng + op + "(JJ)J", NUMBERS);
        }
        STATIC.put(lng + "rotateLeft(JI)J", NUMBERS);
        STATIC.put(lng + "rotateRight(JI)J", NUMBERS);
        for (String op : new String[] {"signum", "bitCount", "numberOfLeadingZeros", "numberOfTrailingZeros"}) {
            STATIC.put(lng + op + "(J)I", NUMBERS);
        }
        for (String op : new String[] {"highestOneBit", "lowestOneBit", "reverse"}) {
            STATIC.put(lng + op + "(J)J", NUMBERS);
        }
        for (String op : new String[] {"toHexString", "toOctalString", "toBinaryString"}) {
            STATIC.put(lng + op + "(J)" + STRING, NUMBERS);
        }
        STATIC.put(lng + "decode(" + STRING + ")Ljava/lang/Long;", NUMBERS + "#decodeLong");
        STATIC.put(lng + "valueOf(" + STRING + ")Ljava/lang/Long;", NUMBERS + "#longValueOf");
        STATIC.put(lng + "valueOf(" + STRING + "I)Ljava/lang/Long;", NUMBERS + "#longValueOf");
        VIRTUAL.put(lng + "shortValue()S", NUMBERS);

        String shrt = "java/lang/Short.";
        STATIC.put(shrt + "decode(" + STRING + ")Ljava/lang/Short;", NUMBERS + "#decodeShort");
        STATIC.put(shrt + "valueOf(" + STRING + ")Ljava/lang/Short;", NUMBERS + "#shortValueOf");
        STATIC.put(shrt + "valueOf(" + STRING + "I)Ljava/lang/Short;", NUMBERS + "#shortValueOf");
        STATIC.put(shrt + "toString(S)" + STRING, NUMBERS);
        VIRTUAL.put(shrt + "byteValue()B", NUMBERS);
        String bte = "java/lang/Byte.";
        STATIC.put(bte + "decode(" + STRING + ")Ljava/lang/Byte;", NUMBERS + "#decodeByte");
        STATIC.put(bte + "valueOf(" + STRING + ")Ljava/lang/Byte;", NUMBERS + "#byteValueOf");
        STATIC.put(bte + "valueOf(" + STRING + "I)Ljava/lang/Byte;", NUMBERS + "#byteValueOf");
        STATIC.put(bte + "toString(B)" + STRING, NUMBERS);
        VIRTUAL.put(bte + "shortValue()S", NUMBERS);

        STATIC.put("java/lang/Double.isFinite(D)Z", NUMBERS);
        STATIC.put("java/lang/Float.isFinite(F)Z", NUMBERS);
        for (String op : new String[] {"max", "min", "sum"}) {
            STATIC.put("java/lang/Double." + op + "(DD)D", NUMBERS);
            STATIC.put("java/lang/Float." + op + "(FF)F", NUMBERS);
        }

        String bool = "java/lang/Boolean.";
        STATIC.put(bool + "toString(Z)" + STRING, NUMBERS);
        STATIC.put(bool + "getBoolean(" + STRING + ")Z", NUMBERS);
        for (String op : new String[] {"logicalAnd", "logicalOr", "logicalXor"}) {
            STATIC.put(bool + op + "(ZZ)Z", NUMBERS);
        }

        String math = "java/lang/Math.";
        for (String fn : new String[] {"exp", "log", "log10", "asin", "acos", "atan", "ulp"}) {
            STATIC.put(math + fn + "(D)D", MATH_UTIL);
        }
        STATIC.put(math + "pow(DD)D", MATH_UTIL);
        STATIC.put(math + "atan2(DD)D", MATH_UTIL);
        STATIC.put(math + "copySign(DD)D", MATH_UTIL);
        STATIC.put(math + "scalb(DI)D", MATH_UTIL);
        STATIC.put(math + "copySign(FF)F", NUMBERS);
        STATIC.put(math + "random()D", NUMBERS);
        STATIC.put(math + "signum(D)D", NUMBERS);
        STATIC.put(math + "signum(F)F", NUMBERS);
        STATIC.put(math + "hypot(DD)D", NUMBERS);
        for (String fn : new String[] {"cbrt", "rint", "sinh", "cosh", "tanh"}) {
            STATIC.put(math + fn + "(D)D", NUMBERS);
        }
        for (String op : new String[] {"floorDiv", "floorMod", "addExact", "subtractExact", "multiplyExact"}) {
            STATIC.put(math + op + "(II)I", NUMBERS);
            STATIC.put(math + op + "(JJ)J", NUMBERS);
        }
        for (String op : new String[] {"incrementExact", "decrementExact", "negateExact"}) {
            STATIC.put(math + op + "(I)I", NUMBERS);
            STATIC.put(math + op + "(J)J", NUMBERS);
        }
        STATIC.put(math + "toIntExact(J)I", NUMBERS);
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

    /// The class of a rule's target.
    private static String targetOwner(String target) {
        int hash = target.indexOf('#');
        return hash < 0 ? target : target.substring(0, hash);
    }

    /// The member a rule's target names, `name` unless it says otherwise.
    private static String targetName(String target, String name) {
        int hash = target.indexOf('#');
        return hash < 0 ? name : target.substring(hash + 1);
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
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner(target), targetName(target, name),
                            descriptor, false);
                    return;
                }
            } else if (opcode == Opcodes.INVOKEVIRTUAL) {
                String target = VIRTUAL.get(owner + "." + name + descriptor);
                if (target != null) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner(target), targetName(target, name),
                            receiverFirst(owner, descriptor), false);
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
                    super.visitFieldInsn(opcode, targetOwner(target), targetName(target, name), descriptor);
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
                    return new Handle(Opcodes.H_INVOKESTATIC, targetOwner(target), targetName(target, h.getName()),
                            h.getDesc(), false);
                }
            } else if (h.getTag() == Opcodes.H_INVOKEVIRTUAL) {
                String target = VIRTUAL.get(key);
                if (target != null) {
                    return new Handle(Opcodes.H_INVOKESTATIC, targetOwner(target), targetName(target, h.getName()),
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
