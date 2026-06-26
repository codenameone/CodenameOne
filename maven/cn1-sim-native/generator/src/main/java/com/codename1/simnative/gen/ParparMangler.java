/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.simnative.gen;

import java.lang.reflect.Method;

/**
 * Computes the ParparVM C function names for Java methods, mirroring the
 * mangling implemented by the bytecode translator
 * (vm/ByteCodeTranslator/.../BytecodeMethod.appendCMethodPrefix and
 * ByteCodeMethodArg.appendCMethodExt):
 *
 * <pre>
 *   &lt;cls&gt;_&lt;method&gt;__&lt;argExt...&gt;[_R&lt;retExt&gt;]
 * </pre>
 *
 * where class and object-type names replace '/', '.' and '$' with '_',
 * primitive extensions are _int/_long/_byte/_short/_char/_boolean/_float/_double,
 * object extensions are _&lt;mangled class&gt; and arrays append _&lt;dim&gt;ARRAY
 * after the element extension.
 *
 * <p>The handwritten native sources define both the plain and the _R-suffixed
 * symbol for non-void methods; translator-generated call sites link against
 * the _R form, so the JNI shims call the _R symbol for non-void methods and
 * the plain (no-_R) symbol for void methods.</p>
 */
public final class ParparMangler {
    private ParparMangler() {
    }

    /**
     * @param cls a Java class
     * @return the mangled class prefix, e.g. com_codename1_impl_ios_IOSNative
     */
    public static String mangleClassName(Class cls) {
        return mangleClassName(cls.getName());
    }

    /**
     * @param name a class name in dot or slash form
     * @return the mangled form with separators replaced by underscores
     */
    public static String mangleClassName(String name) {
        return name.replace('/', '_').replace('.', '_').replace('$', '_');
    }

    /**
     * @param type a parameter or return type
     * @return the mangled extension appended to a function name for this type
     * (including the leading underscore)
     */
    public static String typeExtension(Class type) {
        int dim = 0;
        while (type.isArray()) {
            dim++;
            type = type.getComponentType();
        }
        StringBuilder b = new StringBuilder("_");
        if (type.isPrimitive()) {
            b.append(type.getName());
        } else {
            b.append(mangleClassName(type));
        }
        if (dim > 0) {
            b.append("_").append(dim).append("ARRAY");
        }
        return b.toString();
    }

    /**
     * The C function name the translator emits calls against: for non-void
     * methods this includes the _R&lt;returnType&gt; suffix.
     *
     * @param m the Java method
     * @return the linked C symbol name
     */
    public static String functionName(Method m) {
        StringBuilder b = new StringBuilder();
        b.append(mangleClassName(m.getDeclaringClass()));
        b.append("_");
        b.append(m.getName().replace('-', '_'));
        b.append("__");
        for (Class p : m.getParameterTypes()) {
            b.append(typeExtension(p));
        }
        if (m.getReturnType() != Void.TYPE) {
            b.append("_R").append(typeExtension(m.getReturnType()));
        }
        return b.toString();
    }

    /**
     * The ParparVM C type for a Java type, as used in function signatures.
     *
     * @param type a Java type
     * @return JAVA_INT family, JAVA_LONG, JAVA_FLOAT, JAVA_DOUBLE, JAVA_OBJECT
     * or JAVA_VOID
     */
    public static String parparType(Class type) {
        if (type == Void.TYPE) {
            return "JAVA_VOID";
        }
        if (type == Long.TYPE) {
            return "JAVA_LONG";
        }
        if (type == Float.TYPE) {
            return "JAVA_FLOAT";
        }
        if (type == Double.TYPE) {
            return "JAVA_DOUBLE";
        }
        if (type == Boolean.TYPE) {
            return "JAVA_BOOLEAN";
        }
        if (type == Character.TYPE) {
            return "JAVA_CHAR";
        }
        if (type == Byte.TYPE) {
            return "JAVA_BYTE";
        }
        if (type == Short.TYPE) {
            return "JAVA_SHORT";
        }
        if (type == Integer.TYPE) {
            return "JAVA_INT";
        }
        return "JAVA_OBJECT";
    }

    /**
     * The JNI C type for a Java type.
     *
     * @param type a Java type
     * @return jint, jlong, jstring, jintArray, jobject etc.
     */
    public static String jniType(Class type) {
        if (type == Void.TYPE) {
            return "void";
        }
        if (type.isPrimitive()) {
            return "j" + type.getName();
        }
        if (type == String.class) {
            return "jstring";
        }
        if (type.isArray()) {
            Class c = type.getComponentType();
            if (c.isPrimitive()) {
                return "j" + c.getName() + "Array";
            }
            return "jobjectArray";
        }
        return "jobject";
    }

    /**
     * The JNI descriptor for a Java type (used in RegisterNatives signatures).
     *
     * @param type a Java type
     * @return the descriptor, e.g. I, J, Ljava/lang/String;, [B
     */
    public static String jniDescriptor(Class type) {
        if (type == Void.TYPE) {
            return "V";
        }
        if (type == Integer.TYPE) {
            return "I";
        }
        if (type == Long.TYPE) {
            return "J";
        }
        if (type == Boolean.TYPE) {
            return "Z";
        }
        if (type == Byte.TYPE) {
            return "B";
        }
        if (type == Short.TYPE) {
            return "S";
        }
        if (type == Character.TYPE) {
            return "C";
        }
        if (type == Float.TYPE) {
            return "F";
        }
        if (type == Double.TYPE) {
            return "D";
        }
        if (type.isArray()) {
            return "[" + jniDescriptor(type.getComponentType());
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    /**
     * The JNI method descriptor for a method, e.g. (ILjava/lang/String;)J
     *
     * @param m the method
     * @return the full descriptor
     */
    public static String jniMethodDescriptor(Method m) {
        StringBuilder b = new StringBuilder("(");
        for (Class p : m.getParameterTypes()) {
            b.append(jniDescriptor(p));
        }
        b.append(")").append(jniDescriptor(m.getReturnType()));
        return b.toString();
    }
}
