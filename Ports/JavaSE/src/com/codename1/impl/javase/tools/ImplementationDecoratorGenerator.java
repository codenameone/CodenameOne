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
package com.codename1.impl.javase.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// Generates `com.codename1.impl.CodenameOneImplementationDecorator`, a delegating
/// wrapper around `CodenameOneImplementation` used by the simulator to layer tool
/// proxies (network monitor, performance monitor, location simulation) over any
/// backend implementation (Swing or native).
///
/// The generated source is checked in. Rerun this generator whenever
/// `CodenameOneImplementation` gains, loses or changes a method:
///
/// ```
/// bash scripts/javase/generate-impl-decorator.sh
/// ```
///
/// `DecoratorCoverageTest` in maven/javase fails the build when the generated
/// file is out of sync with the core class.
///
/// Generation rules:
///
/// - Every public/protected, non-static, non-final method declared by
///   `CodenameOneImplementation` gets a forwarding override (`@Override` ensures
///   removed core methods fail compilation here rather than silently lingering).
/// - `init(Object)` forwards to `delegate.initImpl(Object)` so the delegate's
///   private initialization state (package name, initialized flag) is set; the
///   decorator's own copy of that state is maintained by the final `initImpl`
///   that `Display.init` invokes on the decorator itself.
/// - `editString(...)` forwards to `delegate.editStringImpl(...)` for the same
///   reason (the delegate's editing state must track the edit).
/// - Final methods are skipped: they run on the decorator and either only touch
///   decorator-local state or dispatch through virtual methods that forward.
public class ImplementationDecoratorGenerator {
    private static final String TARGET_CLASS = "com.codename1.impl.CodenameOneImplementation";
    private static final String PACKAGE = "com.codename1.impl";
    private static final String CLASS_NAME = "CodenameOneImplementationDecorator";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: ImplementationDecoratorGenerator <output-file>");
            System.exit(1);
        }
        Class cls = Class.forName(TARGET_CLASS);
        String source = generate(cls);
        File out = new File(args[0]);
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        Writer w = new OutputStreamWriter(new FileOutputStream(out), "UTF-8");
        try {
            w.write(source);
        } finally {
            w.close();
        }
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    /// Methods deliberately NOT forwarded to the delegate: the decorator
    /// inherits the base-class implementations, which decompose these
    /// composite rendering operations (and ParparVM instanceof workarounds)
    /// into primitive calls that ARE forwarded. This lets backends whose
    /// versions are device-only native fast paths (the iOS port's
    /// drawLabelComponent etc.) run under the simulator without those
    /// natives, and is behavior-neutral for backends that never override
    /// them (the Swing port).
    private static final String[] RENDER_DECOMPOSED = {
        "drawLabelComponent",
        "paintComponentBackground",
        "instanceofObjArray",
        "instanceofByteArray",
        "instanceofShortArray",
        "instanceofLongArray",
        "instanceofIntArray",
        "instanceofFloatArray",
        "instanceofDoubleArray",
    };

    /// True for methods the decorator should inherit from the base class
    /// rather than forward (see RENDER_DECOMPOSED; also the byte-alpha
    /// fillRect composite).
    public static boolean isRenderDecomposed(Method m) {
        for (String name : RENDER_DECOMPOSED) {
            if (m.getName().equals(name)) {
                return true;
            }
        }
        if (m.getName().equals("fillRect") && m.getParameterTypes().length == 6
                && m.getParameterTypes()[5] == Byte.TYPE) {
            return true;
        }
        return false;
    }

    /// Returns the methods of the given class that require a forwarding override,
    /// sorted deterministically. Shared with `DecoratorCoverageTest`.
    public static List<Method> forwardedMethods(Class cls) {
        List<Method> methods = new ArrayList<Method>();
        for (Method m : cls.getDeclaredMethods()) {
            int mod = m.getModifiers();
            if (m.isSynthetic() || Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                continue;
            }
            if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) {
                continue;
            }
            if (isRenderDecomposed(m)) {
                continue;
            }
            methods.add(m);
        }
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method a, Method b) {
                int n = a.getName().compareTo(b.getName());
                if (n != 0) {
                    return n;
                }
                return paramList(a).compareTo(paramList(b));
            }
        });
        return methods;
    }

    static String paramList(Method m) {
        StringBuilder b = new StringBuilder();
        Class[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(params[i].getCanonicalName()).append(" arg").append(i);
        }
        return b.toString();
    }

    private static String argNames(Method m) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < m.getParameterTypes().length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append("arg").append(i);
        }
        return b.toString();
    }

    private static String throwsClause(Method m) {
        Class[] ex = m.getExceptionTypes();
        if (ex.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(" throws ");
        for (int i = 0; i < ex.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(ex[i].getCanonicalName());
        }
        return b.toString();
    }

    /// True for the `init(Object)` method which must forward to `initImpl`.
    public static boolean isInitSpecialCase(Method m) {
        return m.getName().equals("init") && m.getParameterTypes().length == 1
                && m.getParameterTypes()[0] == Object.class;
    }

    /// True for the `editString(Component, int, int, String, int)` method which
    /// must forward to `editStringImpl`.
    public static boolean isEditStringSpecialCase(Method m) {
        Class[] p = m.getParameterTypes();
        return m.getName().equals("editString") && p.length == 5
                && p[0].getName().equals("com.codename1.ui.Component")
                && p[1] == Integer.TYPE && p[2] == Integer.TYPE
                && p[3] == String.class && p[4] == Integer.TYPE;
    }

    static String generate(Class cls) {
        StringBuilder b = new StringBuilder();
        b.append("// GENERATED FILE - DO NOT EDIT\n");
        b.append("// Generated by com.codename1.impl.javase.tools.ImplementationDecoratorGenerator\n");
        b.append("// Regenerate with: bash scripts/javase/generate-impl-decorator.sh\n");
        b.append("package ").append(PACKAGE).append(";\n\n");
        b.append("/// Delegating wrapper around `CodenameOneImplementation`. The simulator layers\n");
        b.append("/// tool proxies (network monitor, performance monitor, location simulation) on\n");
        b.append("/// top of the backend implementation by subclassing this class and overriding\n");
        b.append("/// just the methods a tool needs to observe.\n");
        b.append("///\n");
        b.append("/// All public/protected non-final methods forward to the delegate. See the\n");
        b.append("/// generator class for the special cases (`init`, `editString`) and why final\n");
        b.append("/// methods are safe to inherit.\n");
        b.append("@SuppressWarnings({\"unchecked\", \"rawtypes\", \"deprecation\"})\n");
        b.append("public class ").append(CLASS_NAME).append(" extends CodenameOneImplementation {\n");
        b.append("    protected final CodenameOneImplementation delegate;\n\n");
        b.append("    protected ").append(CLASS_NAME).append("(CodenameOneImplementation delegate) {\n");
        b.append("        this.delegate = delegate;\n");
        b.append("    }\n\n");
        b.append("    /// Returns the implementation this decorator wraps (which may itself be a decorator).\n");
        b.append("    public CodenameOneImplementation getDelegate() {\n");
        b.append("        return delegate;\n");
        b.append("    }\n\n");
        b.append("    /// Walks the decorator chain and returns the innermost (real) implementation.\n");
        b.append("    public static CodenameOneImplementation unwrap(CodenameOneImplementation impl) {\n");
        b.append("        while (impl instanceof ").append(CLASS_NAME).append(") {\n");
        b.append("            impl = ((").append(CLASS_NAME).append(") impl).delegate;\n");
        b.append("        }\n");
        b.append("        return impl;\n");
        b.append("    }\n");

        for (Method m : forwardedMethods(cls)) {
            String visibility = Modifier.isPublic(m.getModifiers()) ? "public" : "protected";
            String ret = m.getReturnType().getCanonicalName();
            b.append("\n    @Override\n");
            b.append("    ").append(visibility).append(" ").append(ret).append(" ")
                    .append(m.getName()).append("(").append(paramList(m)).append(")")
                    .append(throwsClause(m)).append(" {\n");
            String call;
            if (isInitSpecialCase(m)) {
                // Forward through the final initImpl so the delegate's private
                // initialization state (packageName, initialized) is set too.
                call = "delegate.initImpl(arg0)";
            } else if (isEditStringSpecialCase(m)) {
                // Forward through the final editStringImpl so the delegate's
                // private editing state tracks the edit.
                call = "delegate.editStringImpl(arg0, arg1, arg2, arg3, arg4)";
            } else {
                call = "delegate." + m.getName() + "(" + argNames(m) + ")";
            }
            if (m.getReturnType() == Void.TYPE) {
                b.append("        ").append(call).append(";\n");
            } else {
                b.append("        return ").append(call).append(";\n");
            }
            b.append("    }\n");
        }
        b.append("}\n");
        return b.toString();
    }
}
