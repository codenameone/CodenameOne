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
package com.codename1.dart.transpiler.codegen;

import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Holds every runtime stub's parameter types to the Java members they map to.
///
/// A stub class names its Java class with `@JavaName`; a named constructor
/// parameter becomes a one-argument method of the same name, a positional one a
/// constructor argument, and a method parameter the Java method's argument. The
/// transpiler types a lambda passed to a parameter from the STUB's declaration,
/// so a slot declared `Object` where Java takes a `Funcs.Func2<...>` gets an
/// untyped `Func<Object...>` lambda that javac then rejects at the call. The
/// reverse -- a function type in the stub where Java takes `Object` -- emits a
/// typed lambda into a slot that later calls it through the wrong interface.
///
/// The check is on the KIND of the type (loose, function, specific), not its
/// exact spelling: Dart and Java name types differently, and the kind is what
/// decides how an argument is emitted. Overloads pass when any candidate agrees.
public class StubJavaParameterTypeTest {

    private static final File RUNTIME_RESOURCES = new File("../flutter-runtime/src/main/resources");

    /// The callback typedefs the emitter knows by name (`VoidCallback`, `WidgetBuilder`),
    /// read from the emitter itself so this test cannot drift from it.
    private static final java.util.Set<String> EMITTER_TYPEDEFS = emitterTypedefs();

    private static java.util.Set<String> emitterTypedefs() {
        return JavaEmitter.typedefNames();
    }

    /// `ANY_FUNCTION` is Dart's bare `Function`: it names no signature, so a Java
    /// `Object` slot that inspects the arity at run time (`onError`) agrees with it.
    private enum Kind { LOOSE, FUNCTION, ANY_FUNCTION, SPECIFIC, UNKNOWN }

    @Test
    public void stubParameterKindsMatchTheJavaMembersTheyMapTo() throws Exception {
        TestSupport.prerequisite(new File(RUNTIME_RESOURCES, "META-INF/dart").isDirectory(),
                "runtime stubs not found under " + RUNTIME_RESOURCES.getAbsolutePath());
        Diagnostics diags = new Diagnostics();
        StubRegistry reg = StubRegistry.loadFromClasspath(Collections.singletonList(RUNTIME_RESOURCES), diags);
        assertFalse(diags.hasErrors(), "runtime stubs failed to parse: " + diags.asList());
        checkAll(reg, "runtime", 100);
    }

    /// The transpiler's own built-in stubs (`com/codename1/dart/stubs`: the
    /// dart:collection mixins and the embedded Flutter fallback set) map onto the same
    /// runtime classes and are held to them the same way.
    @Test
    public void builtInStubParameterKindsMatchTheJavaMembersTheyMapTo() throws Exception {
        Diagnostics diags = new Diagnostics();
        StubRegistry reg = StubRegistry.loadEmbedded(diags);
        assertFalse(diags.hasErrors(), "built-in stubs failed to parse: " + diags.asList());
        checkAll(reg, "built-in", 50);
    }

    private void checkAll(StubRegistry reg, String which, int minimum) {
        List<String> bad = new ArrayList<String>();
        int checked = 0;
        List<String> skipped = new ArrayList<String>();
        ClassLoader loader = getClass().getClassLoader();
        for (Ast.ClassDecl cls : reg.classes.values()) {
            if (cls.javaName == null) {
                continue;
            }
            Class<?> java;
            try {
                java = Class.forName(cls.javaName, false, loader);
            } catch (ClassNotFoundException | LinkageError e) {
                continue;  // a class the runtime does not ship is another test's concern
            }
            try {
                java.getMethods();
                java.getConstructors();
            } catch (LinkageError e) {
                skipped.add(cls.javaName + " (" + e + ")");
                continue;  // its signatures name a class absent from this test class path
            }
            for (Ast.CtorDecl ctor : cls.ctors) {
                if (ctor.name != null) {
                    continue;  // named constructors map to factory methods of their own
                }
                List<Ast.Param> positional = new ArrayList<Ast.Param>();
                for (Ast.Param p : ctor.params) {
                    if (p.named) {
                        List<Type> cands = new ArrayList<Type>();
                        for (Method m : java.getMethods()) {
                            if (m.getName().equals(p.name) && m.getParameterCount() == 1) {
                                cands.add(m.getGenericParameterTypes()[0]);
                            }
                        }
                        checked += check(cls, cls.name + "({" + p.name + "})", p.type, cands, reg, bad);
                    } else {
                        positional.add(p);
                    }
                }
                for (int i = 0; i < positional.size(); i++) {
                    List<Type> cands = new ArrayList<Type>();
                    for (Constructor<?> c : java.getConstructors()) {
                        if (c.getParameterCount() == positional.size()) {
                            cands.add(c.getGenericParameterTypes()[i]);
                        }
                    }
                    checked += check(cls, cls.name + "(" + positional.get(i).name + ")",
                            positional.get(i).type, cands, reg, bad);
                }
            }
            for (Ast.MethodDecl m : cls.methods) {
                if (m.isGetter || m.isSetter) {
                    continue;
                }
                boolean anyNamed = false;
                boolean allNamed = !m.params.isEmpty();
                for (Ast.Param p : m.params) {
                    anyNamed |= p.named;
                    allNamed &= p.named;
                }
                // A static whose parameters are all named -- how a stub spells a named
                // constructor, `StreamController.broadcast({onListen, ...})` -- is called
                // with every one of them positionally, in declaration order, nulls filling
                // the ones not given. Any other mix is lowered differently.
                if (anyNamed && !(m.isStatic && allNamed)) {
                    continue;
                }
                for (int i = 0; i < m.params.size(); i++) {
                    List<Type> cands = new ArrayList<Type>();
                    for (Method jm : java.getMethods()) {
                        if (jm.getName().equals(m.name) && jm.getParameterCount() == m.params.size()) {
                            cands.add(jm.getGenericParameterTypes()[i]);
                        }
                    }
                    checked += check(cls, cls.name + "." + m.name + "(" + m.params.get(i).name + ")",
                            m.params.get(i).type, cands, reg, bad);
                }
            }
        }
        System.out.println("StubJavaParameterTypeTest (" + which + "): compared " + checked + " parameters; skipped "
                + skipped.size() + " classes with unresolvable signatures: " + skipped);
        assertTrue(checked > minimum, "only " + checked + " stub parameters were compared -- the mapping broke");
        assertTrue(bad.isEmpty(), bad.size() + " stub parameter(s) disagree with the Java member they map to "
                + "(give the stub the Dart equivalent of the Java type):\n  " + String.join("\n  ", bad));
    }

    private static int check(Ast.ClassDecl cls, String label, Ast.TypeRef dartType, List<Type> cands,
            StubRegistry reg, List<String> bad) {
        Kind d = dartKind(dartType, cls, reg);
        if (d == Kind.UNKNOWN || cands.isEmpty()) {
            return 0;
        }
        StringBuilder seen = new StringBuilder();
        for (Type t : cands) {
            Kind j = javaKind(t);
            if (j == Kind.UNKNOWN || agrees(d, j)) {
                return 1;
            }
            seen.append(seen.length() == 0 ? "" : " | ").append(t.getTypeName());
        }
        bad.add(label + ": stub " + typeStr(dartType) + " (" + d + "), Java " + seen);
        return 1;
    }

    private static boolean agrees(Kind dart, Kind java) {
        if (dart == Kind.ANY_FUNCTION) {
            return java == Kind.FUNCTION || java == Kind.LOOSE;
        }
        if ((dart == Kind.FUNCTION) != (java == Kind.FUNCTION)) {
            return false;
        }
        return !(dart == Kind.LOOSE && java == Kind.SPECIFIC);
    }

    private static Kind dartKind(Ast.TypeRef t, Ast.ClassDecl cls, StubRegistry reg) {
        if (t == null || t.name == null || "var".equals(t.name)) {
            return Kind.UNKNOWN;
        }
        if ("Object".equals(t.name) || "dynamic".equals(t.name)) {
            return Kind.LOOSE;
        }
        if ("Function".equals(t.name)) {
            return t.funcParams == null ? Kind.ANY_FUNCTION : Kind.FUNCTION;
        }
        if (cls.typeParams.contains(t.name)) {
            return Kind.UNKNOWN;
        }
        if (EMITTER_TYPEDEFS.contains(t.name)) {
            return Kind.FUNCTION;
        }
        Ast.TypedefDecl td = reg.typedefs.get(t.name);
        if (td != null) {
            return td.returnType != null ? Kind.FUNCTION : Kind.UNKNOWN;
        }
        return Kind.SPECIFIC;
    }

    private static Kind javaKind(Type t) {
        if (t instanceof TypeVariable) {
            return Kind.UNKNOWN;
        }
        Type raw = t instanceof ParameterizedType ? ((ParameterizedType) t).getRawType() : t;
        if (!(raw instanceof Class)) {
            return Kind.UNKNOWN;
        }
        Class<?> c = (Class<?>) raw;
        if (c == Object.class) {
            return Kind.LOOSE;
        }
        Class<?> outer = c.getEnclosingClass();
        if ((outer != null && "dart.runtime.Funcs".equals(outer.getName()))
                || (c.isInterface() && c.getName().startsWith("java.util.function."))) {
            return Kind.FUNCTION;
        }
        return Kind.SPECIFIC;
    }

    private static String typeStr(Ast.TypeRef t) {
        return t.name + (t.args.isEmpty() ? "" : "<...>") + (t.nullable ? "?" : "");
    }
}
