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

import com.codename1.python.transpiler.ast.Node.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Compiled layouts for the classes a module defines at its top level.
///
/// Such a class gets a Java subclass of `PyInstance` whose attributes are
/// fields, and whose plain methods are static Java methods reached through
/// a virtual `m_<name>` per method name and arity. Generated code then reads
/// `self.x` as an `instanceof` and a field load, and calls `t.run(p)` as an
/// `instanceof` and a virtual call, instead of going through an inline cache.
///
/// What is planned here is only ever a fast path: every access still falls
/// back to the generic one when the receiver is not of the expected Java
/// class, a field is unset, or the class was changed after it was built
/// (`PyType.patched`). A field is a name the class's own methods store on
/// their first parameter, and never a name any compiled class binds in its
/// body, since a class attribute -- a property in particular -- must keep
/// deciding what a store to that name does.
final class Layouts {
    static final class Cls {
        final ClassDef def;
        final String javaName;
        final String typeVar;
        final Cls parent;
        final List<String> fields = new ArrayList<String>();
        /// Every field, the inherited ones first: the index order the
        /// generated `fieldIndex` uses.
        final List<String> allFields = new ArrayList<String>();
        final Set<String> bodyNames;
        final Map<String, FunctionDef> methods = new LinkedHashMap<String, FunctionDef>();
        /// `methods`, plus the plain methods with too many parameters for a
        /// generic dispatch: those `Infer` and the typed code still see.
        final Map<String, FunctionDef> typable = new LinkedHashMap<String, FunctionDef>();

        Cls(ClassDef def, String javaName, String typeVar, Cls parent) {
            this.def = def;
            this.javaName = javaName;
            this.typeVar = typeVar;
            this.parent = parent;
            this.bodyNames = def.scope.assigned;
        }
    }

    final Map<ClassDef, Cls> classes = new LinkedHashMap<ClassDef, Cls>();
    /// Field name -> the classes that declare it (not inherit it).
    final Map<String, List<Cls>> fieldOwners = new LinkedHashMap<String, List<Cls>>();
    /// `name/arity` of every compiled method, arity without the receiver.
    final Set<String> dispatch = new LinkedHashSet<String>();
    final Map<FunctionDef, String> staticNames = new LinkedHashMap<FunctionDef, String>();
    /// The class each compiled method belongs to.
    final Map<FunctionDef, Cls> methodClass = new LinkedHashMap<FunctionDef, Cls>();

    private static final Set<String> FORBIDDEN = new HashSet<String>();

    static {
        // A class that customises attribute access itself is left alone.
        // `__slots__` is not on the list: it fixes the attribute set, which
        // is what a layout is, and the generic instance does not enforce it
        // either, so the two paths agree.
        FORBIDDEN.add("__setattr__");
        FORBIDDEN.add("__getattribute__");
        FORBIDDEN.add("__delattr__");
        FORBIDDEN.add("__dict__");
        FORBIDDEN.add("__class__");
        FORBIDDEN.add("__new__");
    }

    /// `attrStores` is the module scope's (`Scope.attrStores`): a field the
    /// module itself assigns on a class is left to the generic path, since
    /// the class attribute -- a property, say -- must then decide its reads.
    static Layouts plan(List<Stmt> body, String prefix, Set<String> attrStores) {
        Layouts l = new Layouts();
        Map<String, Cls> byName = new LinkedHashMap<String, Cls>();
        int n = 0;
        for (Stmt s : body) {
            if (!(s instanceof ClassDef)) {
                continue;
            }
            ClassDef c = (ClassDef) s;
            Cls parent = null;
            boolean ok = c.keywords.isEmpty() && c.decorators.isEmpty() && c.bases.size() <= 1;
            if (ok && c.bases.size() == 1) {
                Expr b = c.bases.get(0);
                parent = b instanceof Name ? byName.get(((Name) b).id) : null;
                ok = parent != null;
            }
            for (String f : FORBIDDEN) {
                ok &= !c.scope.assigned.contains(f);
            }
            if (!ok) {
                byName.remove(c.name);
                continue;
            }
            n++;
            Cls k = new Cls(c, prefix + "L" + n + "_" + Emitter.mangle(c.name), "T" + n + "_" + Emitter.mangle(c.name), parent);
            l.classes.put(c, k);
            byName.put(c.name, k);
        }
        Set<String> bodyNames = new HashSet<String>();
        for (Cls k : l.classes.values()) {
            bodyNames.addAll(k.bodyNames);
        }
        boolean noFields = false;
        for (String st : attrStores) {
            int dot = st.indexOf('.');
            if (byName.containsKey(st.substring(0, dot))) {
                String a = st.substring(dot + 1);
                bodyNames.add(a);
                // An attribute hook added from outside sees every access.
                noFields |= "*".equals(a) || FORBIDDEN.contains(a);
            }
        }
        for (Cls k : l.classes.values()) {
            Map<String, Integer> defined = new LinkedHashMap<String, Integer>();
            for (Stmt s : k.def.body) {
                if (s instanceof FunctionDef) {
                    String nm = ((FunctionDef) s).name;
                    defined.put(nm, defined.containsKey(nm) ? defined.get(nm) + 1 : 1);
                }
            }
            Set<String> rebound = new HashSet<String>();
            for (Stmt s : k.def.body) {
                if (s instanceof Assign) {
                    for (Expr t : ((Assign) s).targets) {
                        names(t, rebound);
                    }
                } else if (s instanceof AugAssign) {
                    names(((AugAssign) s).target, rebound);
                } else if (s instanceof AnnAssign) {
                    names(((AnnAssign) s).target, rebound);
                } else if (s instanceof For) {
                    names(((For) s).target, rebound);
                } else if (s instanceof ClassDef) {
                    rebound.add(((ClassDef) s).name);
                } else if (!(s instanceof FunctionDef) && !(s instanceof ExprStmt) && !(s instanceof Pass)) {
                    // Anything else might bind a method's name too.
                    rebound.addAll(k.bodyNames);
                }
            }
            Set<String> stored = new LinkedHashSet<String>();
            for (Stmt s : k.def.body) {
                if (!(s instanceof FunctionDef)) {
                    continue;
                }
                FunctionDef f = (FunctionDef) s;
                List<String> names = f.args.names();
                if (names.isEmpty()) {
                    continue;
                }
                selfStores(f.body, names.get(0), stored);
                boolean plain = f.decorators.isEmpty() && !f.isAsync && !f.scope.hasYield && f.args.vararg == null
                        && f.args.kwarg == null && f.args.kwonly.isEmpty() && defined.get(f.name) == 1
                        && !rebound.contains(f.name) && !hasFree(f);
                if (plain && names.size() <= 8) {
                    k.typable.put(f.name, f);
                }
                if (plain && names.size() <= 4) {
                    k.methods.put(f.name, f);
                    l.dispatch.add(f.name + "/" + (names.size() - 1));
                    l.staticNames.put(f, "M_" + k.javaName.substring(prefix.length()) + "_" + Emitter.mangle(f.name));
                    l.methodClass.put(f, k);
                }
            }
            if (k.parent != null) {
                k.allFields.addAll(k.parent.allFields);
            }
            for (String f : stored) {
                if (noFields || f.startsWith("__") && f.endsWith("__") || bodyNames.contains(f) || k.allFields.contains(f)) {
                    continue;
                }
                k.fields.add(f);
                k.allFields.add(f);
                List<Cls> o = l.fieldOwners.get(f);
                if (o == null) {
                    o = new ArrayList<Cls>();
                    l.fieldOwners.put(f, o);
                }
                o.add(k);
            }
        }
        return l;
    }

    /// Whether the method uses `super()` or `__class__`, the one way a method
    /// at the top of a module captures anything: its body becomes a static
    /// Java method, which can capture nothing.
    private static boolean hasFree(FunctionDef f) {
        return f.scope.used.contains("super") || f.scope.used.contains("__class__");
    }

    /// The attribute names `self.<name> = ...` stores in `body`, outside
    /// nested functions and classes.
    private static void selfStores(List<Stmt> body, String self, Set<String> out) {
        for (Stmt s : body) {
            if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    target(t, self, out);
                }
            } else if (s instanceof AugAssign) {
                target(((AugAssign) s).target, self, out);
            } else if (s instanceof AnnAssign) {
                target(((AnnAssign) s).target, self, out);
            } else if (s instanceof For) {
                target(((For) s).target, self, out);
                selfStores(((For) s).body, self, out);
                selfStores(((For) s).orelse, self, out);
            } else if (s instanceof While) {
                selfStores(((While) s).body, self, out);
                selfStores(((While) s).orelse, self, out);
            } else if (s instanceof If) {
                selfStores(((If) s).body, self, out);
                selfStores(((If) s).orelse, self, out);
            } else if (s instanceof With) {
                for (WithItem w : ((With) s).items) {
                    if (w.vars != null) {
                        target(w.vars, self, out);
                    }
                }
                selfStores(((With) s).body, self, out);
            } else if (s instanceof Try) {
                Try t = (Try) s;
                selfStores(t.body, self, out);
                for (ExceptHandler h : t.handlers) {
                    selfStores(h.body, self, out);
                }
                selfStores(t.orelse, self, out);
                selfStores(t.finalbody, self, out);
            }
        }
    }

    private static void names(Expr t, Set<String> out) {
        if (t instanceof Name) {
            out.add(((Name) t).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                names(e, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                names(e, out);
            }
        } else if (t instanceof Starred) {
            names(((Starred) t).value, out);
        }
    }

    private static void target(Expr t, String self, Set<String> out) {
        if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            if (a.value instanceof Name && ((Name) a.value).id.equals(self)) {
                out.add(a.attr);
            }
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                target(e, self, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                target(e, self, out);
            }
        } else if (t instanceof Starred) {
            target(((Starred) t).value, self, out);
        }
    }

    /// What `name` resolves to for class `k`: the class whose body binds it
    /// first along the compiled chain, or null when none does.
    static Cls resolver(Cls k, String name) {
        for (Cls c = k; c != null; c = c.parent) {
            if (c.bodyNames.contains(name)) {
                return c;
            }
        }
        return null;
    }
}
