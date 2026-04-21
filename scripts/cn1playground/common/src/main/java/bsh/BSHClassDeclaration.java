/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/


package bsh;

import static bsh.ClassGenerator.Type;

/**
*/
class BSHClassDeclaration extends SimpleNode
{
    /**
        The class instance initializer method name.
        A BshMethod by this name is installed by the class delcaration into
        the static class body namespace.
        It is called once to initialize the static members of the class space
        and each time an instances is created to initialize the instance
        members.
    */
    static final String CLASSINITNAME = "_bshClassInit";

    String name;
    Modifiers modifiers = new Modifiers(Modifiers.CLASS);
    int numInterfaces;
    boolean extend;
    Type type;
    private ScriptedClass scriptedClass;

    BSHClassDeclaration(int id) { super(id); }

    /**
     * Evaluates the class declaration by building a {@link ScriptedClass}
     * descriptor and binding it into the enclosing namespace under the
     * declared name. Subsequent {@code new ClassName(...)} expressions
     * resolve to a {@link ScriptedInstance} via {@link
     * BSHAllocationExpression}.
     *
     * <p>Interfaces and enums are still rejected — they require richer
     * runtime support (SAM binding, singleton instances) that is layered
     * on top of the basic scripted-class core.
     */
    public synchronized Object eval(final CallStack callstack, final Interpreter interpreter) throws EvalError {
        if (scriptedClass == null) {
            BSHBlock body = findBody();
            ScriptedClass parent = extend ? resolveParentScriptedClass(callstack) : null;
            Class<?> javaParent = (extend && parent == null)
                    ? resolveParentJavaClass(callstack, interpreter) : null;
            java.util.List<ScriptedClass> interfaces = resolveImplementedInterfaces(callstack);
            // Interfaces share the ScriptedClass machinery — they can declare
            // static methods (callable as Iface.foo()) and default methods
            // (inherited by implementing classes). They cannot be instantiated
            // directly with `new Iface()`. Enums use the same machinery with
            // a marker plus per-constant ScriptedInstances populated at build.
            scriptedClass = ScriptedClass.build(name, callstack.top(), body, parent,
                    interfaces, type == Type.INTERFACE, callstack, interpreter);
            scriptedClass.markInterface(type == Type.INTERFACE);
            scriptedClass.markEnum(type == Type.ENUM);
            scriptedClass.setJavaParent(javaParent);
            if (type == Type.ENUM) {
                scriptedClass.populateEnumConstants(body, callstack, interpreter);
            }
            if (type == Type.CLASS && !isAbstract()) {
                enforceInterfaceMethodImplementations(callstack, interpreter);
            }
            try {
                callstack.top().setVariable(name, scriptedClass, false);
            } catch (UtilEvalError ex) {
                throw ex.toEvalError(this, callstack);
            }
        }
        return scriptedClass;
    }

    private boolean isAbstract() {
        return modifiers != null && modifiers.hasModifier("abstract");
    }

    /** After a concrete class is built, verify that every abstract
     * method declared on each implemented interface is actually
     * provided by the class with a body (declared here, inherited
     * from an extends-parent, or pulled in as a default from another
     * scripted interface). Fails with a clear diagnostic when
     * anything is missing — catches the case where a user writes
     * `class X implements ActionListener {}` and forgets
     * {@code actionPerformed}. Works for both Java interfaces
     * (names drawn from the CN1 registry) and scripted interfaces
     * (names drawn from the interface's own abstract-method list). */
    private void enforceInterfaceMethodImplementations(CallStack callstack, Interpreter interpreter)
            throws EvalError {
        java.util.Set<String> concrete = scriptedClass.getConcreteInstanceMethodNames();
        int seen = 0;
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node child = jjtGetChild(i);
            if (!(child instanceof BSHAmbiguousName)) continue;
            if (extend && seen == 0) { seen++; continue; }
            seen++;
            BSHAmbiguousName node = (BSHAmbiguousName) child;
            String rawName = node.text;
            if (rawName == null || rawName.isEmpty()) continue;

            String ifaceLabel;
            java.util.List<String> required;
            // Scripted interfaces: pull abstract method names directly
            // from the interface's own ScriptedClass. Java interfaces:
            // pull names from the CN1 registry's method index.
            try {
                Object v = callstack.top().getVariable(simpleIfaceName(rawName));
                if (v instanceof ScriptedClass) {
                    ScriptedClass iface = (ScriptedClass) v;
                    ifaceLabel = "scripted interface '" + iface.getName() + "'";
                    required = iface.getAbstractInstanceMethodNames();
                } else {
                    ifaceLabel = null;
                    required = null;
                }
            } catch (UtilEvalError ignore) {
                ifaceLabel = null;
                required = null;
            }
            if (required == null) {
                Class<?> ifaceClass;
                try {
                    ifaceClass = node.toClass(callstack, interpreter);
                } catch (EvalError ex) {
                    continue;
                }
                if (ifaceClass == null || !ifaceClass.isInterface()) continue;
                ifaceLabel = ifaceClass.getName();
                String[] javaMethods = methodNamesOf(ifaceClass);
                if (javaMethods == null) continue;
                required = new java.util.ArrayList<String>();
                for (String m : javaMethods) if (m != null && !m.isEmpty()) required.add(m);
            }

            java.util.List<String> missing = new java.util.ArrayList<String>();
            for (String m : required) {
                if (m == null || m.isEmpty()) continue;
                if (!concrete.contains(m)) missing.add(m);
            }
            if (!missing.isEmpty()) {
                StringBuilder msg = new StringBuilder("class '").append(name)
                        .append("' is not abstract and does not implement all methods from ")
                        .append(ifaceLabel).append(". Missing: ");
                for (int k = 0; k < missing.size(); k++) {
                    if (k > 0) msg.append(", ");
                    msg.append(missing.get(k));
                }
                msg.append('.');
                throw new EvalError(msg.toString(), this, callstack);
            }
        }
    }

    private String simpleIfaceName(String raw) {
        int lt = raw.indexOf('<');
        String simple = lt >= 0 ? raw.substring(0, lt).trim() : raw.trim();
        int dot = simple.lastIndexOf('.');
        return dot >= 0 ? simple.substring(dot + 1) : simple;
    }

    private String[] methodNamesOf(Class<?> ifaceClass) {
        bsh.cn1.CN1Access registry = bsh.cn1.CN1AccessRegistry.getInstance();
        if (!(registry instanceof bsh.cn1.GeneratedCN1Access)) return null;
        String[] entries = ((bsh.cn1.GeneratedCN1Access) registry).getMethodSignatures(ifaceClass.getName());
        if (entries == null) return null;
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (String entry : entries) {
            if (entry == null) continue;
            int paren = entry.indexOf('(');
            String n = paren > 0 ? entry.substring(0, paren) : entry;
            n = n.trim();
            if (!n.isEmpty()) names.add(n);
        }
        return names.toArray(new String[names.size()]);
    }

    /** Resolve the extends-target as a Java class when it isn't a
     * ScriptedClass. Powers the super(args) forwarding for classes that
     * extend e.g. RuntimeException. */
    private Class<?> resolveParentJavaClass(CallStack callstack, Interpreter interpreter) {
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node child = jjtGetChild(i);
            if (child instanceof BSHAmbiguousName) {
                try {
                    return ((BSHAmbiguousName) child).toClass(callstack, interpreter);
                } catch (EvalError ignore) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Collect the scripted interfaces this class/interface declares (via
     * `implements` on a class or `extends` on an interface — in the parser's
     * AST they appear as BSHAmbiguousName children after the optional
     * extends-name). Default methods from these interfaces are merged into
     * the class's instance-method table so `c.defaultMethod()` resolves. */
    private java.util.List<ScriptedClass> resolveImplementedInterfaces(CallStack callstack) {
        java.util.List<ScriptedClass> result = new java.util.ArrayList<ScriptedClass>();
        // The first BSHAmbiguousName child is the extends-target when
        // `extend` is true; skip it.
        int seen = 0;
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node child = jjtGetChild(i);
            if (!(child instanceof BSHAmbiguousName)) continue;
            if (extend && seen == 0) { seen++; continue; }
            seen++;
            String rawName = ((BSHAmbiguousName) child).text;
            if (rawName == null || rawName.isEmpty()) continue;
            int lt = rawName.indexOf('<');
            String simple = lt >= 0 ? rawName.substring(0, lt).trim() : rawName.trim();
            if (simple.indexOf('.') >= 0) continue;
            try {
                Object v = callstack.top().getVariable(simple);
                if (v instanceof ScriptedClass) result.add((ScriptedClass) v);
            } catch (UtilEvalError ex) {
                // skip unresolved interfaces
            }
        }
        return result;
    }

    private BSHBlock findBody() {
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node child = jjtGetChild(i);
            if (child instanceof BSHBlock) {
                return (BSHBlock) child;
            }
        }
        return null;
    }

    /** Look up the extends-named class in the namespace. Only ScriptedClass
     * parents are inherited at runtime — extending a Java class is treated as
     * "no inheritance" since we cannot reflect into Java class internals
     * without breaking the CN1 no-reflection invariant. */
    private ScriptedClass resolveParentScriptedClass(CallStack callstack) {
        for (int i = 0; i < jjtGetNumChildren(); i++) {
            Node child = jjtGetChild(i);
            if (child instanceof BSHAmbiguousName) {
                String parentName = ((BSHAmbiguousName) child).text;
                if (parentName == null || parentName.length() == 0) return null;
                int lt = parentName.indexOf('<');
                if (lt >= 0) parentName = parentName.substring(0, lt);
                if (parentName.indexOf('.') >= 0) return null;
                try {
                    Object v = callstack.top().getVariable(parentName);
                    if (v instanceof ScriptedClass) return (ScriptedClass) v;
                } catch (UtilEvalError ex) {
                    // not bound — caller falls back to no-inheritance
                }
                return null;
            }
        }
        return null;
    }

    public String toString() {
        return super.toString() + ": " + name;
    }
}
