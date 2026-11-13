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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Holds every `@JavaName` stub class's declared supertypes (`extends`, `implements`,
/// `with`, a mixin's `on`) to the Java class it maps to. A Java supertype that is itself
/// a stub type and that the stub does not reach (directly or through another stub) means
/// the analyzer refuses an assignment the generated Java would accept -- the
/// `StreamController` stub once omitted `StreamSink`. The reverse, a stub supertype whose
/// Java class the mapped class is not assignable to, is a cast the generated Java rejects.
public class StubJavaSupertypeTest {

    /// Java supertypes a stub deliberately does not declare, each because declaring it
    /// would let Dart code make an assignment real Dart rejects. Keyed `Stub:JavaSuper`.
    private static final Set<String> IMPLEMENTATION_ONLY = new HashSet<String>(java.util.Arrays.asList(
            // dart:convert's Codec is no Converter in Dart; the Java ByteCodec reuses one.
            "Encoding:dart.convert.Converter",
            // EventSink maps onto the Java StreamSink, which is also a StreamConsumer;
            // a Dart EventSink is not.
            "EventSink:dart.async.StreamConsumer",
            // The Java ListMixin/SetMixin inherit their Iterable members from IterableMixin;
            // Dart's ListMixin and SetMixin only implement List and Set.
            "ListMixin:dart.collection.IterableMixin",
            "ListBase:dart.collection.IterableMixin",
            "SetMixin:dart.collection.IterableMixin",
            "SetBase:dart.collection.IterableMixin"));

    private static final File RUNTIME_RESOURCES = new File("../flutter-runtime/src/main/resources");

    @Test
    public void runtimeStubSupertypesMatchTheJavaClasses() {
        TestSupport.prerequisite(new File(RUNTIME_RESOURCES, "META-INF/dart").isDirectory(),
                "runtime stubs not found under " + RUNTIME_RESOURCES.getAbsolutePath());
        Diagnostics diags = new Diagnostics();
        StubRegistry reg = StubRegistry.loadFromClasspath(Collections.singletonList(RUNTIME_RESOURCES), diags);
        assertFalse(diags.hasErrors(), "runtime stubs failed to parse: " + diags.asList());
        checkAll(reg, "runtime", 50);
    }

    @Test
    public void builtInStubSupertypesMatchTheJavaClasses() {
        Diagnostics diags = new Diagnostics();
        StubRegistry reg = StubRegistry.loadEmbedded(diags);
        assertFalse(diags.hasErrors(), "built-in stubs failed to parse: " + diags.asList());
        checkAll(reg, "built-in", 20);
    }

    private void checkAll(StubRegistry reg, String which, int minimum) {
        ClassLoader loader = getClass().getClassLoader();
        Map<String, Class<?>> javaOf = new HashMap<String, Class<?>>();
        Map<Class<?>, Set<String>> stubsOf = new HashMap<Class<?>, Set<String>>();
        for (Ast.ClassDecl cls : reg.classes.values()) {
            if (cls.javaName == null) {
                continue;
            }
            try {
                Class<?> c = Class.forName(cls.javaName, false, loader);
                javaOf.put(cls.name, c);
                Set<String> names = stubsOf.get(c);
                if (names == null) {
                    names = new HashSet<String>();
                    stubsOf.put(c, names);
                }
                names.add(cls.name);
            } catch (ClassNotFoundException | LinkageError e) {
                // a class the runtime does not ship is another test's concern
            }
        }
        List<String> bad = new ArrayList<String>();
        int checked = 0;
        for (Ast.ClassDecl cls : reg.classes.values()) {
            Class<?> java = javaOf.get(cls.name);
            if (java == null || cls.extensionOn != null) {
                continue;
            }
            checked++;
            Set<String> reach = stubClosure(cls.name, reg, new HashSet<String>());
            for (Class<?> sup : directSupers(java)) {
                Set<String> names = stubsOf.get(sup);
                if (names == null || sup == java) {
                    continue;
                }
                boolean found = false;
                for (String n : names) {
                    found |= reach.contains(n);
                }
                if (!found && !sup.isInterface() && reachesAncestor(sup, reach, stubsOf)) {
                    // An implementation base: the Java class extends StatelessWidget where
                    // Flutter's is a StatefulWidget, and the stub names the common ancestor
                    // (`Widget`) that Dart code may rely on. Interfaces are API, never skipped.
                    found = true;
                }
                if (!found && !IMPLEMENTATION_ONLY.contains(cls.name + ":" + sup.getName())) {
                    bad.add(cls.name + ": Java " + java.getName() + " extends/implements " + sup.getName()
                            + " (stub " + names + ") but the stub does not declare it");
                }
            }
            for (Ast.TypeRef t : declaredSupers(cls)) {
                Class<?> sup = javaOf.get(t.name);
                // A Java interface cannot extend a class, so an interface stub naming a
                // class supertype (`PreferredSizeWidget implements Widget`) is not a drift.
                boolean unrepresentable = sup != null && java.isInterface() && !sup.isInterface();
                if (sup != null && !unrepresentable && !sup.isAssignableFrom(java)) {
                    bad.add(cls.name + ": stub declares " + t.name + " (" + sup.getName() + ") but "
                            + java.getName() + " is not one");
                }
            }
        }
        System.out.println("StubJavaSupertypeTest (" + which + "): compared " + checked + " classes");
        assertTrue(checked > minimum, "only " + checked + " stub classes were compared -- the mapping broke");
        assertTrue(bad.isEmpty(), bad.size() + " stub class(es) disagree with the Java class they map to:\n  "
                + String.join("\n  ", bad));
    }

    private static boolean reachesAncestor(Class<?> sup, Set<String> reach, Map<Class<?>, Set<String>> stubsOf) {
        for (Class<?> c = sup.getSuperclass(); c != null && c != Object.class; c = c.getSuperclass()) {
            Set<String> names = stubsOf.get(c);
            if (names != null) {
                for (String n : names) {
                    if (reach.contains(n)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<Class<?>> directSupers(Class<?> c) {
        List<Class<?>> out = new ArrayList<Class<?>>();
        if (c.getSuperclass() != null && c.getSuperclass() != Object.class) {
            out.add(c.getSuperclass());
        }
        Collections.addAll(out, c.getInterfaces());
        return out;
    }

    private static List<Ast.TypeRef> declaredSupers(Ast.ClassDecl cls) {
        List<Ast.TypeRef> out = new ArrayList<Ast.TypeRef>();
        if (cls.superclass != null) {
            out.add(cls.superclass);
        }
        out.addAll(cls.interfaces);
        out.addAll(cls.mixins);
        out.addAll(cls.mixinOn);
        return out;
    }

    private static Set<String> stubClosure(String name, StubRegistry reg, Set<String> acc) {
        if (!acc.add(name)) {
            return acc;
        }
        Ast.ClassDecl cls = reg.classes.get(name);
        if (cls != null) {
            for (Ast.TypeRef t : declaredSupers(cls)) {
                if (t != null && t.name != null) {
                    stubClosure(t.name, reg, acc);
                }
            }
        }
        return acc;
    }
}
