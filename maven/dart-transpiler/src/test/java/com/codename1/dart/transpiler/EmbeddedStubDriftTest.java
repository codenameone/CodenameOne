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
package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.analyze.StubRegistry;
import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code com/codename1/dart/stubs/flutter_material.dart} (loaded by
 * {@link StubRegistry#loadEmbedded}, the fallback the mojo uses when an app has no
 * {@code codenameone-flutter-runtime} on its stub classpath) is a hand-maintained SUBSET
 * of the real runtime's own stub, {@code codenameone-flutter-runtime}'s
 * {@code META-INF/dart/flutter_material.dart}. It is not meant to mirror every
 * declaration the runtime file has grown since -- but every declaration the embedded copy
 * DOES carry must still describe the real Java runtime correctly, because the emitter
 * compiles a Dart call onto a Java call using exactly what the stub says: the return type,
 * and -- for a named parameter -- the canonical order it is declared in (positional
 * parameters map onto Java arguments by position directly).
 *
 * <p>A return type that silently drifted (the embedded {@code showDialog} declared
 * {@code void} while the runtime one declares {@code Future<dynamic>}, matching
 * {@code Dialogs.showDialog}'s real Java signature) does not fail to parse -- it emits a
 * call that is wrong in a way nothing downstream of the stub can see; {@code final r =
 * await showDialog(...)} against the {@code void} version emits invalid Java. The same
 * class of bug showed up twice more once this test went looking: {@code Navigator.push}
 * was declared {@code void} here while the runtime stub (matching
 * {@code Navigator.push}'s real {@code dart.async.Future<Object>}) returns a value, and
 * {@code GridView.count} declared {@code crossAxisCount} as its first named parameter
 * while the real {@code GridView.count(Key, String, Object, Boolean, long, ...)} takes
 * {@code restorationId}, {@code physics} and {@code primary} before it -- so the canonical
 * order this stub declared would have passed {@code crossAxisCount} into the
 * {@code restorationId} slot.
 *
 * <p>This test catches that class of drift generically rather than by hand: for every
 * function and method the two stub sets declare under the same name (and, for a class
 * method, the same static/getter/setter kind), their return type must agree, their
 * positional parameters must agree pairwise, and any named parameter present in both must
 * sit at the same position among the named parameters in both declarations. It is
 * deliberately not a parity check -- the runtime stub may freely declare members (or extra
 * trailing parameters) the embedded copy does not have; only declarations -- and
 * parameters -- present in BOTH are compared. Constructors are excluded: the header
 * comment both stub files carry says named constructor parameters compile to Java setter
 * calls dispatched by name, so their declared order carries no meaning to miscompile.</p>
 */
public class EmbeddedStubDriftTest {

    /** The runtime module's own copy of the stub this test compares the embedded one against. */
    private static final File RUNTIME_STUB = new File(
            "../flutter-runtime/src/main/resources/META-INF/dart/flutter_material.dart");

    @Test
    public void embeddedStubMatchesRuntimeStubWhereTheyOverlap() throws Exception {
        TestSupport.prerequisite(RUNTIME_STUB.isFile(),
                "runtime stub not found at " + RUNTIME_STUB.getAbsolutePath());

        Diagnostics embeddedDiags = new Diagnostics();
        StubRegistry embedded = StubRegistry.loadEmbedded(embeddedDiags);
        assertFalse(embeddedDiags.hasErrors(), "embedded stub failed to parse: " + embeddedDiags.asList());

        Diagnostics runtimeDiags = new Diagnostics();
        StubRegistry runtime = new StubRegistry();
        runtime.load("flutter_material.dart (runtime)",
                new String(Files.readAllBytes(RUNTIME_STUB.toPath()), StandardCharsets.UTF_8), runtimeDiags);
        assertFalse(runtimeDiags.hasErrors(), "runtime stub failed to parse: " + runtimeDiags.asList());

        List<String> drift = new ArrayList<String>();

        for (Ast.FunctionDecl fn : embedded.functions.values()) {
            Ast.FunctionDecl real = runtime.functions.get(fn.name);
            if (real != null) {
                compareCallable("function " + fn.name, fn.returnType, fn.params, real.returnType, real.params, drift);
            }
        }

        for (Ast.ClassDecl cls : embedded.classes.values()) {
            Ast.ClassDecl real = runtime.classes.get(cls.name);
            if (real == null) {
                continue;  // the whole class is only in the embedded copy -- not this test's concern
            }
            for (Ast.MethodDecl m : cls.methods) {
                Ast.MethodDecl realM = findMethod(real, m.name, m.isStatic, m.isGetter, m.isSetter);
                if (realM == null) {
                    continue;  // missing on the runtime side (or a different kind) -- not drift, just absent
                }
                String label = cls.name + "." + m.name;
                if (m.isGetter || m.isSetter) {
                    // A getter/setter's single implicit parameter carries no named-order
                    // meaning; only its type (the return type for a getter) matters.
                    if (!typeStr(m.returnType).equals(typeStr(realM.returnType))) {
                        drift.add(label + ": return type " + typeStr(m.returnType)
                                + " (embedded) != " + typeStr(realM.returnType) + " (runtime)");
                    }
                    continue;
                }
                compareCallable(label, m.returnType, m.params, realM.returnType, realM.params, drift);
            }
        }

        assertTrue(drift.isEmpty(), "The embedded flutter_material.dart fallback stub has drifted from "
                + "the runtime module's own stub for declarations present in both -- fix the embedded "
                + "copy (maven/dart-transpiler/src/main/resources/com/codename1/dart/stubs/flutter_material.dart) "
                + "to match the runtime one (maven/flutter-runtime/src/main/resources/META-INF/dart/"
                + "flutter_material.dart), which in turn must match the real Java runtime:\n  "
                + String.join("\n  ", drift));
    }

    private static Ast.MethodDecl findMethod(Ast.ClassDecl c, String name, boolean isStatic,
            boolean isGetter, boolean isSetter) {
        for (Ast.MethodDecl m : c.methods) {
            if (m.name.equals(name) && m.isStatic == isStatic && m.isGetter == isGetter && m.isSetter == isSetter) {
                return m;
            }
        }
        return null;
    }

    /**
     * Compares one callable's return type and parameters as declared in the embedded stub
     * ({@code A}) against its namesake in the runtime stub ({@code B}), appending every
     * mismatch found to {@code drift}. Positional parameters are compared pairwise by
     * index (that index IS their Java call position). Named parameters are compared by
     * name: a name present on both sides must sit at the same index among the named
     * parameters on both sides -- that index is what the emitter uses as the Java call
     * position for a named argument (see the stub files' own header comment) -- and must
     * share the same type. A parameter only one side declares is not drift: the embedded
     * stub is an intentional subset.
     */
    private static void compareCallable(String label, Ast.TypeRef retA, List<Ast.Param> paramsA,
            Ast.TypeRef retB, List<Ast.Param> paramsB, List<String> drift) {
        if (!typeStr(retA).equals(typeStr(retB))) {
            drift.add(label + ": return type " + typeStr(retA) + " (embedded) != " + typeStr(retB) + " (runtime)");
        }

        List<Ast.Param> posA = positional(paramsA);
        List<Ast.Param> posB = positional(paramsB);
        for (int i = 0; i < Math.min(posA.size(), posB.size()); i++) {
            Ast.Param a = posA.get(i);
            Ast.Param b = posB.get(i);
            if (!a.name.equals(b.name) || !typeStr(a.type).equals(typeStr(b.type))) {
                drift.add(label + ": positional parameter #" + i + " is " + typeStr(a.type) + " " + a.name
                        + " (embedded) but " + typeStr(b.type) + " " + b.name + " (runtime)");
            }
        }

        List<Ast.Param> namedA = named(paramsA);
        List<Ast.Param> namedB = named(paramsB);
        for (int i = 0; i < namedA.size(); i++) {
            Ast.Param a = namedA.get(i);
            int j = indexOfNamed(namedB, a.name);
            if (j < 0) {
                continue;  // the runtime stub dropped a named parameter -- not this test's concern
            }
            if (j != i) {
                drift.add(label + ": named parameter '" + a.name + "' is at canonical position " + i
                        + " in the embedded stub but " + j + " in the runtime stub -- the emitter maps a named "
                        + "argument onto the Java call by this canonical position, so this would pass the "
                        + "wrong value into the wrong Java parameter");
                continue;
            }
            Ast.Param b = namedB.get(j);
            if (!typeStr(a.type).equals(typeStr(b.type))) {
                drift.add(label + ": named parameter '" + a.name + "' is " + typeStr(a.type)
                        + " (embedded) but " + typeStr(b.type) + " (runtime)");
            }
        }
    }

    private static List<Ast.Param> positional(List<Ast.Param> params) {
        List<Ast.Param> r = new ArrayList<Ast.Param>();
        for (Ast.Param p : params) {
            if (!p.named) {
                r.add(p);
            }
        }
        return r;
    }

    private static List<Ast.Param> named(List<Ast.Param> params) {
        List<Ast.Param> r = new ArrayList<Ast.Param>();
        for (Ast.Param p : params) {
            if (p.named) {
                r.add(p);
            }
        }
        return r;
    }

    private static int indexOfNamed(List<Ast.Param> namedParams, String name) {
        for (int i = 0; i < namedParams.size(); i++) {
            if (namedParams.get(i).name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static String typeStr(Ast.TypeRef t) {
        return t == null ? "void" : t.toString();
    }
}
