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

import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This transpiler erases every generic type parameter: an enclosing generic method's
 * {@code <T>} has no runtime {@code Class} the emitter can hand to an API that needs one.
 * {@code whereType<T>()} used to pass a {@code null} witness for exactly that case, and
 * {@code DartIterable.whereType} reads a null witness the way Dart reads {@code whereType
 * <dynamic>()}: "keep every non-null element" -- so {@code only<T>([1, 'a']).toList()}
 * silently kept the {@code 'a'} too, for ANY {@code T}. {@code is T} has the same missing
 * witness but surfaced as a javac error in the generated sources (a bare type-variable
 * {@code instanceof}). Both now report E0142 at transpile time instead of lowering.
 * {@code as T} is the exception: it lowers to an unchecked cast, right whenever the value
 * is a T, so rejecting it would break working code.
 *
 * <p>{@code whereType<int>()}/{@code whereType<MyClass>()} -- a CONCRETE type argument --
 * must keep working exactly as before; that is covered by the {@code dart_review_round28}
 * behavior fixture, not duplicated here.</p>
 */
public class TypeVariableWitnessTest {

    private static long count(TestSupport.Result r, String code) {
        long n = 0;
        for (Diagnostic d : r.diags.asList()) {
            if (d.code.equals(code)) {
                n++;
            }
        }
        return n;
    }

    /** {@code xs.whereType<T>()} where T is the enclosing generic method's type parameter. */
    @Test
    public void whereTypeOfATypeVariableIsDiagnosed() {
        String src =
                "Iterable<Object> only<T>(List<Object> xs) => xs.whereType<T>();\n"
              + "void main() {}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertTrue(r.diags.hasErrors(), "whereType<T>() on a type variable must be diagnosed");
        assertTrue(count(r, "E0142") >= 1, "expected E0142: " + r.diags.asList());
        boolean namesConstruct = false;
        for (Diagnostic d : r.diags.asList()) {
            if (d.code.equals("E0142") && d.message.contains("whereType<T>") && d.message.contains("T")) {
                namesConstruct = true;
            }
        }
        assertTrue(namesConstruct, "diagnostic should name whereType<T> and T: " + r.diags.asList());
    }

    /** {@code x is T} where T is the enclosing generic method's type parameter. */
    @Test
    public void isTestOfATypeVariableIsDiagnosed() {
        String src =
                "bool isa<T>(Object x) => x is T;\n"
              + "void main() {}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertTrue(r.diags.hasErrors(), "x is T on a type variable must be diagnosed");
        assertTrue(count(r, "E0142") >= 1, "expected E0142: " + r.diags.asList());
    }

    /**
     * {@code x as T} where T is the enclosing generic method's type parameter is NOT
     * diagnosed: it lowers to an unchecked cast, which is right whenever the value is a
     * T (the common {@code json['x'] as T}); only the mismatch fails late. See
     * JavaEmitter.emitAsCast.
     */
    @Test
    public void asCastOfATypeVariableLowersToAnUncheckedCast() {
        String src =
                "T cast<T>(Object x) => x as T;\n"
              + "void main() {}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertEquals(0, count(r, "E0142"), "as T must not be rejected: " + r.diags.asList());
    }

    /** whereType<int>() -- a CONCRETE type argument -- must not be diagnosed. */
    @Test
    public void whereTypeOfAConcreteTypeIsNotDiagnosed() {
        String src =
                "List<int> onlyInts(List<Object?> xs) => xs.whereType<int>().toList();\n"
              + "void main() {\n"
              + "  print(onlyInts(<Object?>[1, 'a', 2]).length);\n"
              + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        assertTrue(count(r, "E0142") == 0, "concrete whereType<int>() must not report E0142");
    }
}
