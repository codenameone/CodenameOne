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

/// Casts and `instanceof` tests on a reference array type the program never
/// allocates. The translator emits an array class's id only for a type
/// something names, and a `CHECKCAST` or `INSTANCEOF` names one as much as
/// an allocation does: the generated C once compared against an id it never
/// declared and failed to compile.
public final class ArrayTypeChecks {
    private ArrayTypeChecks() {
    }

    /// Never instantiated, and no array of it is ever made.
    static final class NeverAllocated {
    }

    private static Object pick(int i) {
        return i == 0 ? (Object) "text" : (Object) new Object[] {"a"};
    }

    private static Object nothing() {
        return null;
    }

    public static void run() {
        Bulk.check("ArrayType.instanceofNeverAllocated", () -> {
            Bulk.p("string", pick(0) instanceof NeverAllocated[]);
            Bulk.p("objects", pick(1) instanceof NeverAllocated[]);
            Bulk.p("objects2d", pick(1) instanceof NeverAllocated[][]);
        });
        Bulk.check("ArrayType.castNull", () -> {
            Object none = nothing();
            NeverAllocated[] a = (NeverAllocated[]) none;
            NeverAllocated[][] b = (NeverAllocated[][]) none;
            Bulk.p("a", a == null);
            Bulk.p("b", b == null);
        });
    }
}
