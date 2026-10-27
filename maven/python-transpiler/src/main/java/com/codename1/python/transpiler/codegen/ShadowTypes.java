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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Picks, for each local of a function body, the compiled class its field
/// accesses most often name: the class a typed copy of the local is kept as
/// (see `Emitter.shadows`). Nested functions, classes, lambdas and
/// comprehensions are their own scopes and are not walked.
final class ShadowTypes {
    private final Layouts layouts;
    private final Map<String, Map<Layouts.Cls, Integer>> counts = new HashMap<String, Map<Layouts.Cls, Integer>>();

    private ShadowTypes(Layouts layouts) {
        this.layouts = layouts;
    }

    /// Local name -> class, for the names read for a compiled field at
    /// least twice.
    static Map<String, Layouts.Cls> of(List<Stmt> body, Layouts layouts) {
        ShadowTypes t = new ShadowTypes(layouts);
        for (Stmt s : body) {
            t.walk(s);
        }
        Map<String, Layouts.Cls> r = new LinkedHashMap<String, Layouts.Cls>();
        for (Map.Entry<String, Map<Layouts.Cls, Integer>> e : t.counts.entrySet()) {
            Layouts.Cls best = null;
            int n = 1;
            for (Map.Entry<Layouts.Cls, Integer> c : e.getValue().entrySet()) {
                if (c.getValue().intValue() > n) {
                    best = c.getKey();
                    n = c.getValue().intValue();
                }
            }
            if (best != null) {
                r.put(e.getKey(), best);
            }
        }
        return r;
    }

    private void walk(Object o) {
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                walk(x);
            }
            return;
        }
        if (!(o instanceof Node) || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda
                || o instanceof Comp) {
            return;
        }
        if (o instanceof Attribute && ((Attribute) o).value instanceof Name) {
            Attribute a = (Attribute) o;
            List<Layouts.Cls> owners = layouts.fieldOwners.get(a.attr);
            if (owners != null) {
                String n = ((Name) a.value).id;
                Map<Layouts.Cls, Integer> m = counts.get(n);
                if (m == null) {
                    m = new LinkedHashMap<Layouts.Cls, Integer>();
                    counts.put(n, m);
                }
                for (Layouts.Cls c : owners) {
                    // A subclass reached through this access counts for the
                    // declaring class too: its instances pass that test.
                    Integer k = m.get(c);
                    m.put(c, Integer.valueOf(k == null ? 1 : k.intValue() + 1));
                }
            }
        }
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                Class<?> t = f.getType();
                if (!Node.class.isAssignableFrom(t) && !List.class.isAssignableFrom(t)) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    walk(f.get(o));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}
