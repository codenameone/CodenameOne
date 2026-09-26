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

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.MethodInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// The Java source of the classes generated beside a backend's own: the aspect
/// helpers the woven methods call, the task class of each `@Async` method, the
/// stand-ins for request, session and lazy beans, and the adapters that publish
/// `@McpTool` and `@ManagedResource` beans.
///
/// Source rather than bytecode because javac then writes every frame, every
/// boxing conversion and every exception table, and the result reads like code a
/// person could have written -- which is what someone debugging into it sees.
final class BackendSources {
    private static final String GENERATED = "@com.codename1.backend.annotations.Generated\n";

    private final BackendBeans beans;

    BackendSources(BackendBeans beans) {
        this.beans = beans;
    }

    // ---------------------------------------------------------------- aspects

    /// The helper class of one class's aspects, and the task class of each of its
    /// `@Async` methods.
    void aspects(BackendBeans.Aspects a, Map<String, String> out) {
        AnnotatedClass cls = a.cls;
        String owner = cls.getSourceName();
        StringBuilder sb = header(a.helperBinary, "the aspects of " + cls.getBinaryName());
        sb.append("final class ").append(simple(a.helperBinary)).append(" {\n");
        sb.append("    private ").append(simple(a.helperBinary)).append("() {\n    }\n\n");
        for (int index = 0; index < a.methods.size(); index++) {
            BackendBeans.Aspect aspect = a.methods.get(index);
            MethodInfo m = aspect.method;
            boolean isStatic = m.isStatic();
            Type[] args = Type.getArgumentTypes(m.getDescriptor());
            Type ret = Type.getReturnType(m.getDescriptor());
            boolean isVoid = ret.getSort() == Type.VOID;
            String retType = isVoid ? "void" : typeName(ret);
            String params = parameters(owner, isStatic, args);
            String callArgs = arguments(isStatic, args.length);
            String inner = (isStatic ? owner : "self") + "." + m.getName()
                    + BackendWeaver.BODY_SUFFIX + "(" + plainArguments(args.length) + ")";
            List<String> layers = new ArrayList<String>();
            if (aspect.transactional != null) {
                layers.add("tx");
            }
            if (aspect.timed != null || aspect.counted != null) {
                layers.add("metrics");
            }
            for (int i = 0; i < layers.size(); i++) {
                boolean outermost = i == layers.size() - 1 && aspect.async == null;
                String name = outermost ? m.getName() : m.getName() + "$cn1" + layers.get(i);
                sb.append("    static ").append(retType).append(' ').append(name).append('(')
                  .append(params).append(") throws Throwable {\n");
                if ("tx".equals(layers.get(i))) {
                    transaction(sb, aspect, inner, isVoid, retType);
                } else {
                    metrics(sb, cls, aspect, index, inner, isVoid, retType);
                }
                sb.append("    }\n\n");
                inner = simple(a.helperBinary) + "." + name + "(" + callArgs + ")";
            }
            if (aspect.async != null) {
                async(sb, cls, aspect, index, params, callArgs, isVoid, retType, m);
                out.put(aspect.asyncTaskBinary, task(cls, aspect, args, inner, isVoid));
            }
        }
        sb.append("}\n");
        out.put(a.helperBinary, flushMembers(sb.toString()));
    }

    private void transaction(StringBuilder sb, BackendBeans.Aspect aspect, String inner,
                             boolean isVoid, String retType) {
        AnnotationValues tx = aspect.transactional;
        String propagation = BackendBeans.enumName(tx.get("propagation"), "REQUIRED");
        sb.append("        com.codename1.backend.Transactions.Transaction cn1Tx =\n")
          .append("                com.codename1.backend.Transactions.begin(")
          .append("com.codename1.backend.Transactions.").append(propagation).append(", ")
          .append(tx.getBoolOrDefault("readOnly", false)).append(", ")
          .append(tx.getIntOrDefault("timeout", -1)).append(");\n");
        if (!isVoid) {
            sb.append("        ").append(retType).append(" cn1Result;\n");
        }
        sb.append("        try {\n");
        sb.append("            ").append(isVoid ? "" : "cn1Result = ").append(inner)
          .append(";\n");
        sb.append("        } catch (Throwable cn1Error) {\n");
        sb.append("            com.codename1.backend.Transactions.afterThrow(cn1Tx, ")
          .append(rollbackDecision(tx)).append(");\n");
        sb.append("            throw cn1Error;\n");
        sb.append("        }\n");
        sb.append("        com.codename1.backend.Transactions.commit(cn1Tx);\n");
        if (!isVoid) {
            sb.append("        return cn1Result;\n");
        }
    }

    /// Spring's rollback rule as one expression over `cn1Error`: the listed types,
    /// most specific first, then unchecked-rolls-back.
    String rollbackDecision(AnnotationValues tx) {
        List<Object[]> rules = new ArrayList<Object[]>();
        addRules(rules, tx.get("rollbackFor"), Boolean.TRUE);
        addRules(rules, tx.get("noRollbackFor"), Boolean.FALSE);
        final Map<String, Integer> depth = new java.util.HashMap<String, Integer>();
        for (Object[] rule : rules) {
            depth.put((String) rule[0], Integer.valueOf(depthOf((String) rule[0])));
        }
        Collections.sort(rules, new Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                return depth.get((String) b[0]).intValue() - depth.get((String) a[0]).intValue();
            }
        });
        StringBuilder sb = new StringBuilder();
        int open = 0;
        for (Object[] rule : rules) {
            sb.append("cn1Error instanceof ").append(sourceOf((String) rule[0])).append(" ? ")
              .append(rule[1]).append(" : (");
            open++;
        }
        sb.append("cn1Error instanceof java.lang.RuntimeException || cn1Error instanceof "
                + "java.lang.Error");
        for (int i = 0; i < open; i++) {
            sb.append(')');
        }
        return sb.toString();
    }

    private static void addRules(List<Object[]> rules, Object value, Boolean rollback) {
        if (!(value instanceof List)) {
            return;
        }
        for (Object o : (List<?>) value) {
            if (o instanceof Type) {
                rules.add(new Object[] {((Type) o).getInternalName(), rollback});
            }
        }
    }

    /// How many superclasses an exception type has: the more, the more specific.
    private int depthOf(String internal) {
        int depth = 0;
        String current = internal;
        while (current != null && !"java/lang/Object".equals(current) && depth < 64) {
            AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(beans.ctx, current);
            if (c != null) {
                current = c.getSuperInternalName();
                depth++;
                continue;
            }
            try {
                Class<?> jdk = Class.forName(current.replace('/', '.'), false,
                        BackendSources.class.getClassLoader());
                while (jdk != null) {
                    depth++;
                    jdk = jdk.getSuperclass();
                }
            } catch (ClassNotFoundException | LinkageError err) {
                // Unknown: counted as shallow, which is what an unresolvable type
                // most likely is, being an exception from a library.
            }
            break;
        }
        return depth;
    }

    private void metrics(StringBuilder sb, AnnotatedClass cls, BackendBeans.Aspect aspect,
                         int index, String inner, boolean isVoid, String retType) {
        String base = cls.getBinaryName() + "." + aspect.method.getName();
        if (aspect.timed != null) {
            String name = aspect.timed.getStringOrDefault("value", "");
            fieldAccessor(sb, "com.codename1.backend.metrics.Histogram", "T" + index,
                    "com.codename1.backend.metrics.Metrics.histogram("
                    + quote(name.length() > 0 ? name : base + ".duration") + ", "
                    + quote(aspect.timed.getStringOrDefault("description", "")) + ", \"ms\")");
        }
        if (aspect.counted != null) {
            String name = aspect.counted.getStringOrDefault("value", "");
            String calls = name.length() > 0 ? name : base + ".calls";
            fieldAccessor(sb, "com.codename1.backend.metrics.Counter", "C" + index,
                    "com.codename1.backend.metrics.Metrics.counter(" + quote(calls) + ", "
                    + quote(aspect.counted.getStringOrDefault("description", "")) + ", \"{call}\")");
            fieldAccessor(sb, "com.codename1.backend.metrics.Counter", "F" + index,
                    "com.codename1.backend.metrics.Metrics.counter(" + quote(calls + ".failures")
                    + ", \"Calls that threw\", \"{call}\")");
        }
        sb.append("        long cn1Start = System.nanoTime();\n");
        sb.append("        boolean cn1Ok = false;\n");
        sb.append("        try {\n");
        if (isVoid) {
            sb.append("            ").append(inner).append(";\n");
            sb.append("            cn1Ok = true;\n");
        } else {
            sb.append("            ").append(retType).append(" cn1Result = ").append(inner)
              .append(";\n");
            sb.append("            cn1Ok = true;\n");
            sb.append("            return cn1Result;\n");
        }
        sb.append("        } finally {\n");
        if (aspect.timed != null) {
            sb.append("            t").append(index).append("().record((System.nanoTime() - ")
              .append("cn1Start) / 1000000.0);\n");
        }
        if (aspect.counted != null) {
            sb.append("            c").append(index).append("().increment();\n");
            sb.append("            if (!cn1Ok) {\n                f").append(index)
              .append("().increment();\n            }\n");
        }
        sb.append("        }\n");
    }

    /// A lazily created instrument: a static field and the accessor that fills it
    /// the first time. Emitted inside the method body's class, before it.
    private void fieldAccessor(StringBuilder sb, String type, String field, String create) {
        fieldAccessor(sb, type, field, create, null);
    }

    /**
     * @param stale a condition on {@code value} under which the cached object is
     *        thrown away and fetched again, or null when it never goes stale
     */
    private void fieldAccessor(StringBuilder sb, String type, String field, String create,
                               String stale) {
        // Written as a local class-level member by splicing: the caller is in the
        // middle of a method, so the member goes into a separate buffer that the
        // class writer appends. Kept simple by emitting them as nested holders.
        pendingMembers.append("    private static ").append(type).append(' ').append(field)
          .append(";\n\n");
        pendingMembers.append("    static ").append(type).append(' ')
          .append(Character.toLowerCase(field.charAt(0))).append(field.substring(1))
          .append("() {\n");
        pendingMembers.append("        ").append(type).append(" value = ").append(field)
          .append(";\n");
        pendingMembers.append("        if (value == null").append(stale == null ? "" : " || " + stale)
          .append(") {\n            value = ").append(create)
          .append(";\n            ").append(field).append(" = value;\n        }\n");
        pendingMembers.append("        return value;\n    }\n\n");
    }

    private final StringBuilder pendingMembers = new StringBuilder();

    private void async(StringBuilder sb, AnnotatedClass cls, BackendBeans.Aspect aspect,
                       int index, String params, String callArgs, boolean isVoid,
                       String retType, MethodInfo m) {
        String executor = aspect.async.getStringOrDefault("value", "");
        String thread = BackendBeans.enumName(aspect.async.get("thread"), "PLATFORM");
        fieldAccessor(sb, "com.codename1.backend.TaskExecutor", "E" + index,
                "com.codename1.backend.Tasks.executor(" + quote(executor)
                + ", com.codename1.backend.Tasks." + thread + ")",
                // Tasks.shutdown() stops and forgets every executor when the
                // server stops, so a server started again in the same process
                // must not keep submitting to the stopped one it cached.
                "value.isShutdown()");
        sb.append("    static ").append(retType).append(' ').append(m.getName()).append('(')
          .append(params).append(") throws Throwable {\n");
        sb.append("        ").append(simple(aspect.asyncTaskBinary)).append(" cn1Task = new ")
          .append(simple(aspect.asyncTaskBinary)).append('(').append(callArgs).append(");\n");
        sb.append("        e").append(index).append("().execute(cn1Task);\n");
        if (!isVoid) {
            sb.append("        return cn1Task;\n");
        }
        sb.append("    }\n\n");
    }

    private String task(AnnotatedClass cls, BackendBeans.Aspect aspect, Type[] args,
                        String inner, boolean isVoid) {
        MethodInfo m = aspect.method;
        boolean isStatic = m.isStatic();
        String owner = cls.getSourceName();
        String name = simple(aspect.asyncTaskBinary);
        StringBuilder sb = header(aspect.asyncTaskBinary, "@Async " + cls.getBinaryName() + "."
                + m.getName());
        sb.append("final class ").append(name)
          .append(" extends com.codename1.backend.AsyncTask {\n");
        if (!isStatic) {
            sb.append("    private final ").append(owner).append(" self;\n");
        }
        for (int i = 0; i < args.length; i++) {
            sb.append("    private final ").append(typeName(args[i])).append(" a").append(i)
              .append(";\n");
        }
        sb.append("\n    ").append(name).append('(').append(parameters(owner, isStatic, args))
          .append(") {\n");
        sb.append("        super(").append(quote(cls.getBinaryName() + "." + m.getName()))
          .append(", ").append(isVoid).append(");\n");
        if (!isStatic) {
            sb.append("        this.self = self;\n");
        }
        for (int i = 0; i < args.length; i++) {
            sb.append("        this.a").append(i).append(" = a").append(i).append(";\n");
        }
        sb.append("    }\n\n");
        sb.append("    protected Object call() throws Exception {\n");
        sb.append("        try {\n");
        if (isVoid) {
            sb.append("            ").append(inner).append(";\n");
            sb.append("            return null;\n");
        } else {
            sb.append("            return ").append(inner).append(";\n");
        }
        sb.append("        } catch (Exception cn1Error) {\n            throw cn1Error;\n");
        sb.append("        } catch (Error cn1Error) {\n            throw cn1Error;\n");
        sb.append("        } catch (Throwable cn1Error) {\n");
        sb.append("            throw new RuntimeException(cn1Error);\n        }\n");
        sb.append("    }\n}\n");
        return sb.toString();
    }

    // ----------------------------------------------------------------- proxies

    /// The stand-in for a request, session or lazy bean: a subclass that sends
    /// every call to the instance its scope holds for the current request,
    /// session, or -- for a lazy bean -- the whole server.
    String proxy(BackendBeans.Bean b) {
        String name = simple(b.proxyBinary);
        String type = b.cls.getSourceName();
        StringBuilder sb = header(b.proxyBinary, "the " + (b.lazy ? "lazy" : b.scope)
                + " bean " + b.name);
        sb.append("public final class ").append(name).append(" extends ").append(type)
          .append(" {\n");
        sb.append("    private final com.codename1.backend.Wiring.Scope cn1Scope;\n\n");
        sb.append("    public ").append(name)
          .append("(com.codename1.backend.Wiring.Scope scope) {\n");
        sb.append("        super();\n        this.cn1Scope = scope;\n    }\n\n");
        sb.append("    private ").append(type).append(" cn1Target() {\n");
        sb.append("        return (").append(type).append(") cn1Scope.get(").append(b.slot)
          .append(");\n    }\n\n");
        String pkg = RestClientAnnotationProcessor.packageOf(b.cls.getBinaryName());
        for (MethodInfo m : beans.proxiedMethods(b.cls, false)) {
            boolean isProtected = (m.getAccess() & Opcodes.ACC_PROTECTED) != 0;
            if (isProtected && !declaredIn(b.cls, m, pkg)) {
                continue;
            }
            Type[] args = Type.getArgumentTypes(m.getDescriptor());
            Type ret = Type.getReturnType(m.getDescriptor());
            String access = m.isPublic() ? "public " : isProtected ? "protected " : "";
            sb.append("    ").append(access)
              .append(ret.getSort() == Type.VOID ? "void" : typeName(ret)).append(' ')
              .append(m.getName()).append('(');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(typeName(args[i])).append(" a").append(i);
            }
            sb.append(')').append(throwsClause(m)).append(" {\n        ");
            if (ret.getSort() != Type.VOID) {
                sb.append("return ");
            }
            sb.append("cn1Target().").append(m.getName()).append('(')
              .append(plainArguments(args.length)).append(");\n    }\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /// Whether the method is declared in a class of `pkg` -- a protected method
    /// inherited from another package cannot be called on another instance.
    private boolean declaredIn(AnnotatedClass cls, MethodInfo m, String pkg) {
        AnnotatedClass c = cls;
        while (c != null) {
            if (c.getMethods().contains(m)) {
                return RestClientAnnotationProcessor.packageOf(c.getBinaryName()).equals(pkg);
            }
            String parent = c.getSuperInternalName();
            c = parent == null ? null : beans.ctx.lookup(parent);
        }
        return false;
    }

    // ------------------------------------------------------------------- tools

    /// The adapter publishing one `@McpTool` method.
    String tool(BackendBeans.Bean b, BackendBeans.Tool t) {
        String name = simple(t.adapterBinary);
        String type = b.cls.getSourceName();
        StringBuilder sb = header(t.adapterBinary, "@McpTool " + b.cls.getBinaryName() + "."
                + t.method.getName());
        sb.append("public final class ").append(name)
          .append(" implements com.codename1.backend.mcp.McpTool {\n");
        sb.append("    private final ").append(type).append(" target;\n\n");
        sb.append("    public ").append(name).append('(').append(type)
          .append(" target) {\n        this.target = target;\n    }\n\n");
        sb.append("    public String name() {\n        return ").append(quote(t.name))
          .append(";\n    }\n\n");
        sb.append("    public String description() {\n        return ")
          .append(quote(t.description)).append(";\n    }\n\n");
        Type[] args = Type.getArgumentTypes(t.method.getDescriptor());
        sb.append("    public java.util.Map inputSchema() {\n");
        sb.append("        java.util.Map properties = new java.util.LinkedHashMap();\n");
        StringBuilder required = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append("        properties.put(").append(quote(t.paramNames.get(i)))
              .append(", com.codename1.backend.mcp.McpArgs.property(")
              .append(quote(jsonType(args[i]))).append(", ")
              .append(quote(t.paramDescriptions.get(i))).append(", ")
              .append(enumConstants(args[i])).append("));\n");
            if (t.paramRequired.get(i).booleanValue()) {
                if (required.length() > 0) {
                    required.append(", ");
                }
                required.append(quote(t.paramNames.get(i)));
            }
        }
        sb.append("        return com.codename1.backend.mcp.McpArgs.object(properties, ")
          .append("new String[] {").append(required).append("});\n    }\n\n");
        sb.append("    public Object call(java.util.Map arguments) throws Exception {\n");
        StringBuilder call = new StringBuilder("target.")
                .append(BackendBeans.bridged(t.method) ? BackendWeaver.bridge(t.method.getName())
                        : t.method.getName()).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                call.append(", ");
            }
            call.append(convert(args[i], "arguments", t.paramNames.get(i),
                    t.paramRequired.get(i).booleanValue()));
        }
        call.append(')');
        if (Type.getReturnType(t.method.getDescriptor()).getSort() == Type.VOID) {
            sb.append("        ").append(call).append(";\n        return \"done\";\n");
        } else {
            sb.append("        return ").append(call).append(";\n");
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    /// The expression reading one argument out of a map of arguments, converted.
    String convert(Type t, String map, String name, boolean required) {
        String args = map + ", " + quote(name) + ", " + required;
        String helper = "com.codename1.backend.mcp.McpArgs.";
        switch (t.getSort()) {
            case Type.BOOLEAN: return helper + "booleanValue(" + args + ")";
            case Type.CHAR: return helper + "charValue(" + args + ")";
            // Range-checked, never cast: a narrowing cast would run the tool
            // with a different number than the caller sent.
            case Type.BYTE: return helper + "byteValue(" + args + ")";
            case Type.SHORT: return helper + "shortValue(" + args + ")";
            case Type.INT: return helper + "intValue(" + args + ")";
            case Type.LONG: return helper + "longValue(" + args + ")";
            case Type.FLOAT: return "(float) " + helper + "doubleValue(" + args + ")";
            case Type.DOUBLE: return helper + "doubleValue(" + args + ")";
            default:
                break;
        }
        String n = t.getInternalName();
        if ("java/lang/String".equals(n)) {
            return helper + "string(" + args + ")";
        }
        if ("java/lang/Integer".equals(n)) {
            return helper + "integerObject(" + args + ")";
        }
        if ("java/lang/Long".equals(n)) {
            return helper + "longObject(" + args + ")";
        }
        if ("java/lang/Double".equals(n)) {
            return helper + "doubleObject(" + args + ")";
        }
        if ("java/lang/Float".equals(n)) {
            return helper + "floatObject(" + args + ")";
        }
        if ("java/lang/Short".equals(n)) {
            return helper + "shortObject(" + args + ")";
        }
        if ("java/lang/Byte".equals(n)) {
            return helper + "byteObject(" + args + ")";
        }
        if ("java/lang/Boolean".equals(n)) {
            return helper + "booleanObject(" + args + ")";
        }
        if ("java/lang/Character".equals(n)) {
            return helper + "characterObject(" + args + ")";
        }
        if ("java/util/Map".equals(n)) {
            return helper + "map(" + args + ")";
        }
        if ("java/util/List".equals(n) || "java/util/Collection".equals(n)) {
            return helper + "list(" + args + ")";
        }
        if ("java/lang/Object".equals(n)) {
            return helper + "any(" + args + ")";
        }
        String type = sourceOf(n);
        return "(" + type + ") " + helper + "enumValue(" + type + ".values(), " + args + ")";
    }

    private static String jsonType(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return "boolean";
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG: return "integer";
            case Type.FLOAT:
            case Type.DOUBLE: return "number";
            case Type.CHAR: return "string";
            default:
                break;
        }
        String n = t.getInternalName();
        if ("java/lang/Integer".equals(n) || "java/lang/Long".equals(n)
                || "java/lang/Short".equals(n) || "java/lang/Byte".equals(n)) {
            return "integer";
        }
        if ("java/lang/Double".equals(n) || "java/lang/Float".equals(n)) {
            return "number";
        }
        if ("java/lang/Boolean".equals(n)) {
            return "boolean";
        }
        if ("java/util/Map".equals(n)) {
            return "object";
        }
        if ("java/util/List".equals(n) || "java/util/Collection".equals(n)) {
            return "array";
        }
        if ("java/lang/Object".equals(n)) {
            return "";
        }
        return "string";
    }

    /// `new String[] {...}` of an enum's constants, or null.
    private String enumConstants(Type t) {
        if (t.getSort() != Type.OBJECT) {
            return "null";
        }
        AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(beans.ctx,
                t.getInternalName());
        if (c == null || !c.isEnum()) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("new String[] {");
        boolean first = true;
        for (FieldInfo f : c.getFields()) {
            if ((f.getAccess() & Opcodes.ACC_ENUM) != 0) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(quote(f.getName()));
            }
        }
        return sb.append('}').toString();
    }

    // ----------------------------------------------------------------- managed

    /// The adapter publishing one `@ManagedResource` bean.
    String managed(BackendBeans.Bean b) {
        BackendBeans.Managed mg = b.managed;
        String name = simple(mg.adapterBinary);
        String type = b.cls.getSourceName();
        StringBuilder sb = header(mg.adapterBinary, "@ManagedResource " + b.cls.getBinaryName());
        sb.append("public final class ").append(name)
          .append(" implements com.codename1.backend.ManagedBean {\n");
        sb.append("    private final ").append(type).append(" target;\n\n");
        sb.append("    public ").append(name).append('(').append(type)
          .append(" target) {\n        this.target = target;\n    }\n\n");
        sb.append("    public String getObjectName() {\n        return ")
          .append(quote(mg.objectName)).append(";\n    }\n\n");
        sb.append("    public String getDescription() {\n        return ")
          .append(quote(mg.description)).append(";\n    }\n\n");
        sb.append("    public String[] attributeNames() {\n        return ")
          .append(array(mg.attributeNames)).append(";\n    }\n\n");
        sb.append("    public String[] attributeDescriptions() {\n        return ")
          .append(array(mg.attributeDescriptions)).append(";\n    }\n\n");
        sb.append("    public Object readAttribute(int index) throws Exception {\n");
        sb.append("        switch (index) {\n");
        for (int i = 0; i < mg.attributes.size(); i++) {
            MethodInfo m = mg.attributes.get(i);
            sb.append("            case ").append(i).append(": return target.")
              .append(callName(m)).append("();\n");
        }
        sb.append("            default: throw new IllegalArgumentException(\"No attribute \" "
                + "+ index);\n        }\n    }\n\n");
        List<String> opNames = new ArrayList<String>();
        for (MethodInfo m : mg.operations) {
            opNames.add(m.getName());
        }
        sb.append("    public String[] operationNames() {\n        return ").append(array(opNames))
          .append(";\n    }\n\n");
        sb.append("    public String[] operationDescriptions() {\n        return ")
          .append(array(mg.operationDescriptions)).append(";\n    }\n\n");
        sb.append("    public String[][] operationParameters() {\n        return new String[][] {");
        for (int i = 0; i < mg.operationParams.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(array(mg.operationParams.get(i)));
        }
        sb.append("};\n    }\n\n");
        sb.append("    public Object invoke(int index, java.util.Map arguments) throws Exception {\n");
        sb.append("        switch (index) {\n");
        for (int i = 0; i < mg.operations.size(); i++) {
            MethodInfo m = mg.operations.get(i);
            Type[] args = Type.getArgumentTypes(m.getDescriptor());
            StringBuilder call = new StringBuilder("target.").append(callName(m)).append('(');
            for (int a = 0; a < args.length; a++) {
                if (a > 0) {
                    call.append(", ");
                }
                call.append(convert(args[a], "arguments", mg.operationParams.get(i).get(a), true));
            }
            call.append(')');
            sb.append("            case ").append(i).append(":\n");
            if (Type.getReturnType(m.getDescriptor()).getSort() == Type.VOID) {
                sb.append("                ").append(call).append(";\n");
                sb.append("                return null;\n");
            } else {
                sb.append("                return ").append(call).append(";\n");
            }
        }
        sb.append("            default: throw new IllegalArgumentException(\"No operation \" "
                + "+ index);\n        }\n    }\n\n");
        sb.append("    /** Publishes the numeric attributes as gauges. */\n");
        sb.append("    public void registerGauges() {\n");
        sb.append("        final ").append(type).append(" bean = target;\n");
        for (int i = 0; i < mg.attributes.size(); i++) {
            MethodInfo m = mg.attributes.get(i);
            String read = gaugeRead(Type.getReturnType(m.getDescriptor()),
                    "bean." + callName(m) + "()");
            if (read == null) {
                continue;
            }
            sb.append("        com.codename1.backend.metrics.Metrics.gauge(")
              .append(quote(mg.objectName + "." + mg.attributeNames.get(i))).append(", ")
              .append(quote(mg.attributeDescriptions.get(i))).append(", ")
              .append(quote(mg.attributeUnits.get(i))).append(",\n")
              .append("                new com.codename1.backend.metrics.Gauge.Source() {\n")
              .append("            public double read() {\n                return ").append(read)
              .append(";\n            }\n        });\n");
        }
        sb.append("    }\n}\n");
        return sb.toString();
    }

    /// A double from a getter's value, or null when it has none.
    private static String gaugeRead(Type t, String call) {
        switch (t.getSort()) {
            case Type.BOOLEAN: return call + " ? 1 : 0";
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG:
            case Type.FLOAT:
            case Type.DOUBLE:
            case Type.CHAR: return call;
            case Type.OBJECT:
                String n = t.getInternalName();
                if ("java/lang/Boolean".equals(n)) {
                    return "Boolean.TRUE.equals(" + call + ") ? 1 : 0";
                }
                if (n.startsWith("java/lang/") && (n.endsWith("Integer") || n.endsWith("Long")
                        || n.endsWith("Double") || n.endsWith("Float") || n.endsWith("Short")
                        || n.endsWith("Byte"))) {
                    return "com.codename1.backend.mcp.McpArgs.toDouble(" + call + ")";
                }
                return null;
            default:
                return null;
        }
    }

    private static String callName(MethodInfo m) {
        return BackendBeans.bridged(m) ? BackendWeaver.bridge(m.getName()) : m.getName();
    }

    // ----------------------------------------------------------------- helpers

    private StringBuilder header(String binary, String what) {
        StringBuilder sb = new StringBuilder();
        String pkg = RestClientAnnotationProcessor.packageOf(binary);
        if (pkg.length() > 0) {
            sb.append("package ").append(pkg).append(";\n\n");
        }
        sb.append("// Generated from ").append(what).append(". Do not edit.\n");
        sb.append(GENERATED);
        return sb;
    }

    /// Appends the lazily created fields a class's methods asked for, before its
    /// closing brace. Called once per class, after its methods.
    String flushMembers(String source) {
        if (pendingMembers.length() == 0) {
            return source;
        }
        int close = source.lastIndexOf('}');
        String out = source.substring(0, close) + pendingMembers + source.substring(close);
        pendingMembers.setLength(0);
        return out;
    }

    private String parameters(String owner, boolean isStatic, Type[] args) {
        StringBuilder sb = new StringBuilder();
        if (!isStatic) {
            sb.append(owner).append(" self");
        }
        for (int i = 0; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(typeName(args[i])).append(" a").append(i);
        }
        return sb.toString();
    }

    private static String arguments(boolean isStatic, int count) {
        StringBuilder sb = new StringBuilder();
        if (!isStatic) {
            sb.append("self");
        }
        for (int i = 0; i < count; i++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append('a').append(i);
        }
        return sb.toString();
    }

    private static String plainArguments(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('a').append(i);
        }
        return sb.toString();
    }

    private String throwsClause(MethodInfo m) {
        if (m.getExceptions().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" throws ");
        for (int i = 0; i < m.getExceptions().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(sourceOf(m.getExceptions().get(i)));
        }
        return sb.toString();
    }

    /// The erased type as Java source writes it.
    String typeName(Type t) {
        switch (t.getSort()) {
            case Type.ARRAY:
                StringBuilder sb = new StringBuilder(typeName(t.getElementType()));
                for (int i = 0; i < t.getDimensions(); i++) {
                    sb.append("[]");
                }
                return sb.toString();
            case Type.OBJECT:
                return sourceOf(t.getInternalName());
            default:
                return t.getClassName();
        }
    }

    /// A class's name as Java source writes it: the scanner's answer for a class
    /// it has seen, which knows nesting from `$` in a name, and dots otherwise.
    String sourceOf(String internal) {
        AnnotatedClass c = RestControllerAnnotationProcessor.resolveClass(beans.ctx, internal);
        if (c != null) {
            return c.getSourceName();
        }
        return internal.replace('/', '.').replace('$', '.');
    }

    private static String simple(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String array(List<String> values) {
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        return sb.append('}').toString();
    }

    /// A Java string literal, ASCII only: anything else is written as an escape.
    static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20 || c > 0x7e) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
