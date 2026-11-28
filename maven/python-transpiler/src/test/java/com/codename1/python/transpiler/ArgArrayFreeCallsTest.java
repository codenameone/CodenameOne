/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.transpiler;

import com.codename1.python.runtime.Builtins;
import com.codename1.python.runtime.Py;
import com.codename1.python.runtime.PyFunction;
import com.codename1.python.runtime.PyInt;
import com.codename1.python.runtime.PyBuiltin;
import com.codename1.python.runtime.PyModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The calls a measured application makes hundreds of thousands of times must
/// not allocate an argument array. A builtin whose fixed-arity slot is empty
/// falls back to `call(Object[])`, which builds one per call: nothing fails, the
/// program is simply slower and the garbage collector busier. This holds every
/// call shape a measured application was seen making to a builtin to a fixed
/// slot, so a refactor that drops one fails here instead of in a benchmark.
class ArgArrayFreeCallsTest {
    private static final String[] FIELDS = {"one", "two", "three", "four", "five", "six", "seven"};

    /// module, name, arity: the shapes counted on that benchmark.
    private static final Object[][] HOT = {
        {"_cn1sql", "plain", 1}, {"_cn1sql", "run", 4}, {"_cn1sql", "plan", 7},
        {"_cn1sql", "begin", 1}, {"_cn1sql", "commit", 1},
        {"_cn1hash", "new", 5},
        {"_cn1json", "encode", 2},
        {"builtins", "zip", 2}, {"builtins", "iter", 1}, {"builtins", "any", 1},
        {"builtins", "all", 1}, {"builtins", "sorted", 1}, {"builtins", "enumerate", 1},
        {"builtins", "enumerate", 2},
    };

    @Test
    void hotBuiltinsHaveFixedArityPaths() throws ReflectiveOperationException {
        List<String> missing = new ArrayList<>();
        for (Object[] h : HOT) {
            String module = (String) h[0];
            String name = (String) h[1];
            int arity = (Integer) h[2];
            Object f = "builtins".equals(module) ? Builtins.lookup(name) : PyModule.load(module).getAttrOrNull(name);
            if (!(f instanceof PyBuiltin)) {
                missing.add(module + "." + name + " is not a builtin");
                continue;
            }
            Field slot = PyBuiltin.class.getDeclaredField(FIELDS[arity - 1]);
            slot.setAccessible(true);
            if (slot.get(f) == null) {
                missing.add(module + "." + name + "/" + arity);
            }
        }
        assertTrue(missing.isEmpty(), "calls that would allocate an argument array: " + missing);
    }

    /// A Python function called positionally binds into its reused slot array
    /// for every shape that needs no keyword matching. Each case is checked by
    /// what its body received, and the whole set by the runtime's count of
    /// calls that had to build an argument array instead.
    @Test
    void positionalCallsOfEveryParameterShapeBuildNoArray() {
        Object one = PyInt.of(1);
        Object two = PyInt.of(2);
        Object dflt = PyInt.of(9);
        long before = Py.argArrayFallbacks();
        // def f(a, b, c=9, d=9, e=9, f=9, g=9, h=9): eight parameters, past the
        // fixed-arity bodies.
        Object[] eight = {dflt, dflt, dflt, dflt, dflt, dflt};
        PyFunction many = fn(new String[] {"a", "b", "c", "d", "e", "f", "g", "h"}, 8, false, 0, false, eight, null);
        assertEquals("[1, 2, 9, 9, 9, 9, 9, 9]", Py.toRepr(many.call2(one, two)));
        // def f(self, x, *, strict=9)
        PyFunction kwOnly = fn(new String[] {"self", "x", "strict"}, 2, false, 1, false, new Object[0],
                new Object[] {dflt});
        assertEquals("[1, 2, 9]", Py.toRepr(kwOnly.call2(one, two)));
        // def f(a, *args, strict=9): no extra positional
        PyFunction star = fn(new String[] {"a", "args", "strict"}, 1, true, 1, false, new Object[0],
                new Object[] {dflt});
        assertEquals("[1, (), 9]", Py.toRepr(star.call1(one)));
        // def f(self, name, **kwargs)
        PyFunction kw = fn(new String[] {"self", "name", "kwargs"}, 2, false, 0, true, new Object[0], null);
        assertEquals("[1, 2, {}]", Py.toRepr(kw.call2(one, two)));
        // def f(a, b=9, *args, c=9, **kw)
        PyFunction all = fn(new String[] {"a", "b", "args", "c", "kw"}, 2, true, 1, true, new Object[] {dflt},
                new Object[] {dflt});
        assertEquals("[1, 9, (), 9, {}]", Py.toRepr(all.call1(one)));
        assertEquals(before, Py.argArrayFallbacks(), "a positional call built an argument array");
    }

    /// A function whose body answers the slots it was given, as a list.
    private static PyFunction fn(String[] params, int positional, boolean varArgs, int kwOnly, boolean varKw,
            Object[] defaults, Object[] kwDefaults) {
        return new PyFunction("f", params, positional, varArgs, kwOnly, varKw, defaults, kwDefaults,
                (Object[] a) -> Py.list(a.clone()));
    }
}
