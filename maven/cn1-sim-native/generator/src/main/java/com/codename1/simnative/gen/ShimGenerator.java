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

/**
 * Generates the JNI shim layer binding a port's native-method class (e.g.
 * com.codename1.impl.ios.IOSNative) to the ParparVM-convention C functions
 * implemented by the port's handwritten native sources.
 *
 * <p>Emits three artifacts into the output directory:</p>
 * <ul>
 *   <li><b>cn1sim_decls.h</b> - extern prototypes of the ParparVM symbols the
 *   shims call (the port sources define them; this header lets the shim file
 *   compile without including the full translated-code headers)</li>
 *   <li><b>cn1sim_jni_&lt;suffix&gt;.m</b> - one static JNI function per
 *   native method plus a registration function called from JNI_OnLoad;
 *   RegisterNatives is used so overloaded methods bind correctly</li>
 *   <li><b>cn1sim_symbols_&lt;suffix&gt;.txt</b> - the list of ParparVM
 *   symbols the shims reference, one per line, for cross-checking against the
 *   symbols actually defined by the native sources</li>
 * </ul>
 *
 * <p>Marshalling is delegated to the cn1jni compat runtime
 * (cn1jni_runtime.h): cn1jni_enter/cn1jni_exit manage the per-call reference
 * arena and thread state, cn1jni_wrap_* convert JNI references to JAVA_OBJECT
 * wrappers (pinning arrays), and cn1jni_unwrap_* convert returned
 * JAVA_OBJECTs back to JNI references.</p>
 *
 * <p>Usage: ShimGenerator &lt;nativeClassName&gt; &lt;outputDir&gt; &lt;suffix&gt;
 * (the native class must be on the classpath).</p>
 */
public class ShimGenerator {
    private final Class nativeClass;
    private final String suffix;
    private final List<Method> methods;
    /**
     * Symbols actually defined by the port's native sources. When provided,
     * the generator resolves each method to the _R_-suffixed symbol when it
     * exists, falling back to the plain symbol (a handful of newer native
     * files define only the plain form), and records methods with neither as
     * unresolved - those are satisfied by the generated weak stub file.
     */
    private final java.util.Set<String> definedSymbols;

    public ShimGenerator(Class nativeClass, String suffix, java.util.Set<String> definedSymbols) {
        this.nativeClass = nativeClass;
        this.suffix = suffix;
        this.definedSymbols = definedSymbols;
        methods = new ArrayList<Method>();
        for (Method m : nativeClass.getDeclaredMethods()) {
            if (Modifier.isNative(m.getModifiers())) {
                methods.add(m);
            }
        }
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method a, Method b) {
                int n = a.getName().compareTo(b.getName());
                if (n != 0) {
                    return n;
                }
                return ParparMangler.jniMethodDescriptor(a).compareTo(ParparMangler.jniMethodDescriptor(b));
            }
        });
    }

    /**
     * The plain (no _R suffix) ParparVM symbol for a method.
     */
    private static String plainFunctionName(Method m) {
        StringBuilder b = new StringBuilder();
        b.append(ParparMangler.mangleClassName(m.getDeclaringClass()));
        b.append("_");
        b.append(m.getName().replace('-', '_'));
        b.append("__");
        for (Class p : m.getParameterTypes()) {
            b.append(ParparMangler.typeExtension(p));
        }
        return b.toString();
    }

    /**
     * The symbol the shim for this method calls: the _R form when defined (or
     * when no defined-symbol list was provided), otherwise the plain form.
     */
    private String resolvedSymbol(Method m) {
        String full = ParparMangler.functionName(m);
        if (definedSymbols == null || definedSymbols.contains(full)) {
            return full;
        }
        String plain = plainFunctionName(m);
        if (definedSymbols.contains(plain)) {
            return plain;
        }
        return full;
    }

    /**
     * @return true when neither symbol form is defined by the native sources
     */
    private boolean isUnresolved(Method m) {
        return definedSymbols != null
                && !definedSymbols.contains(ParparMangler.functionName(m))
                && !definedSymbols.contains(plainFunctionName(m));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Usage: ShimGenerator <nativeClassName> <outputDir> <suffix> [definedSymbolsFile]");
            System.exit(1);
        }
        Class cls = Class.forName(args[0]);
        File outDir = new File(args[1]);
        outDir.mkdirs();
        java.util.Set<String> defined = null;
        if (args.length == 4) {
            defined = new java.util.HashSet<String>();
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(args[3]), "UTF-8"));
            try {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        defined.add(line);
                    }
                }
            } finally {
                r.close();
            }
        }
        ShimGenerator gen = new ShimGenerator(cls, args[2], defined);
        write(new File(outDir, "cn1sim_decls.h"), gen.generateDecls());
        write(new File(outDir, "cn1sim_jni_" + args[2] + ".m"), gen.generateShims());
        write(new File(outDir, "cn1sim_symbols_" + args[2] + ".txt"), gen.generateSymbolList());
        write(new File(outDir, "cn1sim_unresolved_" + args[2] + ".txt"), gen.generateUnresolvedList());
        int unresolved = 0;
        for (Method m : gen.methods) {
            if (gen.isUnresolved(m)) {
                unresolved++;
            }
        }
        System.out.println("Generated shims for " + gen.methods.size() + " native methods of "
                + cls.getName() + " (" + unresolved + " unresolved, satisfied by weak stubs)");
    }

    /**
     * @return list of methods whose symbols the native sources do not define
     * (one per line); these link against the generated weak stub file
     */
    public String generateUnresolvedList() {
        StringBuilder b = new StringBuilder();
        for (Method m : methods) {
            if (isUnresolved(m)) {
                b.append(resolvedSymbol(m)).append('\n');
            }
        }
        return b.toString();
    }

    private static void write(File f, String content) throws Exception {
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            w.write(content);
        } finally {
            w.close();
        }
    }

    /**
     * @return the list of ParparVM symbols referenced by the shims
     */
    public String generateSymbolList() {
        StringBuilder b = new StringBuilder();
        for (Method m : methods) {
            b.append(resolvedSymbol(m)).append('\n');
        }
        return b.toString();
    }

    /**
     * @return the contents of cn1sim_decls.h
     */
    public String generateDecls() {
        StringBuilder b = new StringBuilder();
        b.append("// GENERATED FILE - DO NOT EDIT\n");
        b.append("// Generated by com.codename1.simnative.gen.ShimGenerator for ")
                .append(nativeClass.getName()).append('\n');
        b.append("#ifndef CN1SIM_DECLS_H\n#define CN1SIM_DECLS_H\n\n");
        b.append("#include \"cn1_globals.h\"\n\n");
        for (Method m : methods) {
            b.append("extern ").append(retType(m)).append(' ')
                    .append(resolvedSymbol(m)).append('(')
                    .append(parparParams(m)).append(");\n");
        }
        b.append("\n#endif // CN1SIM_DECLS_H\n");
        return b.toString();
    }

    private String retType(Method m) {
        String t = ParparMangler.parparType(m.getReturnType());
        return "JAVA_VOID".equals(t) ? "void" : t;
    }

    private String parparParams(Method m) {
        StringBuilder b = new StringBuilder("CODENAME_ONE_THREAD_STATE");
        if (!Modifier.isStatic(m.getModifiers())) {
            b.append(", JAVA_OBJECT instanceObject");
        }
        int i = 0;
        for (Class p : m.getParameterTypes()) {
            b.append(", ").append(ParparMangler.parparType(p)).append(" arg").append(i++);
        }
        return b.toString();
    }

    /**
     * @return the contents of the JNI shim .m file
     */
    public String generateShims() {
        StringBuilder b = new StringBuilder();
        b.append("// GENERATED FILE - DO NOT EDIT\n");
        b.append("// Generated by com.codename1.simnative.gen.ShimGenerator for ")
                .append(nativeClass.getName()).append('\n');
        b.append("#include <jni.h>\n");
        b.append("#include \"cn1jni_runtime.h\"\n");
        b.append("#include \"cn1sim_decls.h\"\n\n");

        int idx = 0;
        for (Method m : methods) {
            emitShim(b, m, idx++);
        }

        // registration table
        b.append("static const JNINativeMethod cn1sim_methods_").append(suffix).append("[] = {\n");
        idx = 0;
        for (Method m : methods) {
            b.append("    {\"").append(m.getName()).append("\", \"")
                    .append(ParparMangler.jniMethodDescriptor(m)).append("\", (void*)shim_")
                    .append(suffix).append('_').append(idx++).append("},\n");
        }
        b.append("};\n\n");

        String jniClassName = nativeClass.getName().replace('.', '/');
        b.append("// Called from JNI_OnLoad in the compat runtime\n");
        b.append("jint cn1sim_register_").append(suffix).append("(JNIEnv *env) {\n");
        b.append("    jclass cls = (*env)->FindClass(env, \"").append(jniClassName).append("\");\n");
        b.append("    if (cls == NULL) {\n        return JNI_ERR;\n    }\n");
        b.append("    return (*env)->RegisterNatives(env, cls, cn1sim_methods_").append(suffix)
                .append(", sizeof(cn1sim_methods_").append(suffix)
                .append(") / sizeof(JNINativeMethod));\n");
        b.append("}\n");
        return b.toString();
    }

    private void emitShim(StringBuilder b, Method m, int idx) {
        Class ret = m.getReturnType();
        boolean isStatic = Modifier.isStatic(m.getModifiers());
        b.append("// ").append(m.toString()).append('\n');
        b.append("static ").append(ParparMangler.jniType(ret)).append(" shim_")
                .append(suffix).append('_').append(idx).append("(JNIEnv *env, ")
                .append(isStatic ? "jclass cls" : "jobject self");
        Class[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            b.append(", ").append(ParparMangler.jniType(params[i])).append(" a").append(i);
        }
        b.append(") {\n");
        b.append("    struct ThreadLocalData *ts = cn1jni_enter(env);\n");

        StringBuilder call = new StringBuilder();
        call.append(resolvedSymbol(m)).append("(ts");
        if (!isStatic) {
            call.append(", cn1jni_wrap_object(ts, self)");
        }
        for (int i = 0; i < params.length; i++) {
            call.append(", ").append(wrapArg(params[i], "a" + i));
        }
        call.append(")");

        if (ret == Void.TYPE) {
            b.append("    ").append(call).append(";\n");
            b.append("    cn1jni_exit(ts);\n");
        } else {
            b.append("    ").append(ParparMangler.parparType(ret)).append(" r = ").append(call).append(";\n");
            b.append("    ").append(ParparMangler.jniType(ret)).append(" jr = ")
                    .append(unwrapReturn(ret, "r")).append(";\n");
            b.append("    cn1jni_exit(ts);\n");
            b.append("    return jr;\n");
        }
        b.append("}\n\n");
    }

    private String wrapArg(Class type, String name) {
        if (type.isPrimitive()) {
            return "(" + ParparMangler.parparType(type) + ")" + name;
        }
        if (type == String.class) {
            return "cn1jni_wrap_string(ts, " + name + ")";
        }
        if (type.isArray()) {
            Class c = type.getComponentType();
            if (c.isPrimitive()) {
                return "cn1jni_wrap_array_" + c.getName() + "(ts, " + name + ")";
            }
            return "cn1jni_wrap_array_object(ts, " + name + ")";
        }
        return "cn1jni_wrap_object(ts, " + name + ")";
    }

    private String unwrapReturn(Class type, String name) {
        if (type.isPrimitive()) {
            return "(" + ParparMangler.jniType(type) + ")" + name;
        }
        if (type == String.class) {
            return "cn1jni_unwrap_string(ts, " + name + ")";
        }
        if (type.isArray()) {
            Class c = type.getComponentType();
            if (c.isPrimitive()) {
                return "cn1jni_unwrap_array_" + c.getName() + "(ts, " + name + ")";
            }
            return "cn1jni_unwrap_array_object(ts, " + name + ")";
        }
        return "cn1jni_unwrap_object(ts, " + name + ")";
    }
}
