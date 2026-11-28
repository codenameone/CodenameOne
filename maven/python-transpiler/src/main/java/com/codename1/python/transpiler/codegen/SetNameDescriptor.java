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
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/// Folds the attribute name a descriptor learns from `__set_name__`.
///
/// ```python
/// class Field:
///     def __set_name__(self, owner, name):
///         self.name = "_" + name
///     def __get__(self, obj, objtype=None):
///         return getattr(obj, self.name, "unset")
///
/// class Rec:
///     title = Field()
/// ```
///
/// The name `__set_name__` receives is the class-body target the descriptor
/// is bound to, a literal in the source. When the descriptor class is
/// constructed exactly once in the closed program, as the whole value of a
/// one-name assignment in a class body, its `__set_name__` field is that
/// literal (evaluated at build time from the target name) for its one
/// instance, and `getattr(obj, self.name, ...)` in its methods is written
/// with the literal. `class S(*bases)` over a module-level tuple of names
/// bound once is folded to `class S(A, B)`.
///
/// It refuses (leaves the code to the strict check) anything it cannot
/// prove: a class name defined twice, used other than by the one call (a
/// base, an argument, a second binding), a field stored anywhere but the
/// one assignment in `__set_name__`, or a value it cannot evaluate. Two
/// bindings would need a clone of the class per binding, with identity,
/// `isinstance` and `type(...).__name__` rewritten with it; that is not
/// done here.
public final class SetNameDescriptor {

    private SetNameDescriptor() {
    }

    /// Folds every qualifying descriptor and starred base list in `trees`.
    public static void rewrite(Map<String, Node.Module> trees) {
        // Judged per connected group of modules (ConstArgSpecialize.groups):
        // an unrelated module's own `self.name = name` is no second store of
        // this descriptor's field.
        IdentityHashMap<Node.Module, String> groups = ConstArgSpecialize.groups(trees);
        Map<String, Map<String, Node.Module>> parts = new LinkedHashMap<String, Map<String, Node.Module>>();
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            String g = groups.get(e.getValue());
            Map<String, Node.Module> part = parts.get(g);
            if (part == null) {
                part = new LinkedHashMap<String, Node.Module>();
                parts.put(g, part);
            }
            part.put(e.getKey(), e.getValue());
        }
        for (Map<String, Node.Module> part : parts.values()) {
            rewriteGroup(part);
        }
    }

    private static void rewriteGroup(Map<String, Node.Module> trees) {
        final Map<String, List<ClassDef>> classes = new HashMap<String, List<ClassDef>>();
        final Map<String, Integer> nameUses = new HashMap<String, Integer>();
        final Map<String, Integer> attrStores = new HashMap<String, Integer>();
        final Map<String, List<Object[]>> bindings = new HashMap<String, List<Object[]>>();
        for (Node.Module m : trees.values()) {
            scan(m, null, classes, nameUses, attrStores, bindings);
            foldStarBases(m);
        }
        for (Map.Entry<String, List<ClassDef>> e : classes.entrySet()) {
            if (e.getValue().size() != 1) {
                continue;
            }
            ClassDef d = e.getValue().get(0);
            FunctionDef setName = method(d, "__set_name__");
            if (setName == null || setName.args.positional.size() != 3 || !setName.decorators.isEmpty()) {
                continue;
            }
            List<Object[]> b = bindings.get(d.name);
            Integer uses = nameUses.get(d.name);
            if (b == null || b.size() != 1 || uses == null || uses.intValue() != 1
                    || nameUses.containsKey("__set_name__")) {
                continue;
            }
            String self = setName.args.positional.get(0).name;
            String param = setName.args.positional.get(2).name;
            String target = (String) b.get(0)[0];
            Map<String, String> fields = new HashMap<String, String>();
            for (Stmt s : setName.body) {
                if (!(s instanceof Node.Assign)) {
                    continue;
                }
                Node.Assign a = (Node.Assign) s;
                if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Attribute)) {
                    continue;
                }
                Attribute t = (Attribute) a.targets.get(0);
                if (!(t.value instanceof Name) || !self.equals(((Name) t.value).id)) {
                    continue;
                }
                String v = eval(a.value, param, target);
                Integer st = attrStores.get(t.attr);
                if (v == null || fields.containsKey(t.attr) || st == null || st.intValue() != 1) {
                    fields.put(t.attr, null);
                    continue;
                }
                fields.put(t.attr, v);
            }
            if (fields.isEmpty() || rebinds(setName, param)) {
                continue;
            }
            for (Stmt s : d.body) {
                if (s instanceof FunctionDef && s != setName) { //NOPMD CompareObjectsWithEquals - identity is meant
                    fold(s, fields);
                }
            }
        }
    }

    private static void scan(Object o, ClassDef owner, final Map<String, List<ClassDef>> classes,
            final Map<String, Integer> nameUses, final Map<String, Integer> attrStores,
            final Map<String, List<Object[]>> bindings) {
        if (o instanceof ClassDef) {
            final ClassDef c = (ClassDef) o;
            List<ClassDef> l = classes.get(c.name);
            if (l == null) {
                l = new ArrayList<ClassDef>();
                classes.put(c.name, l);
            }
            l.add(c);
            for (Stmt s : c.body) {
                if (s instanceof Node.Assign) {
                    Node.Assign a = (Node.Assign) s;
                    if (a.targets.size() == 1 && a.targets.get(0) instanceof Name && a.value instanceof Call) {
                        Call k = (Call) a.value;
                        if (k.func instanceof Name) {
                            List<Object[]> bl = bindings.get(((Name) k.func).id);
                            if (bl == null) {
                                bl = new ArrayList<Object[]>();
                                bindings.put(((Name) k.func).id, bl);
                            }
                            bl.add(new Object[] {((Name) a.targets.get(0)).id, k});
                        }
                    }
                }
            }
            Infer.children(o, x -> scan(x, c, classes, nameUses, attrStores, bindings));
            return;
        }
        if (o instanceof Name) {
            count(nameUses, ((Name) o).id);
        } else if (o instanceof Attribute) {
            // `mod.Field()` makes an instance too; an explicit
            // `d.__set_name__(...)` sets the field again.
            count(nameUses, ((Attribute) o).attr);
        } else if (o instanceof Node.Assign) {
            for (Expr t : ((Node.Assign) o).targets) {
                stores(t, attrStores);
            }
        } else if (o instanceof Node.AugAssign) {
            stores(((Node.AugAssign) o).target, attrStores);
        } else if (o instanceof Node.AnnAssign) {
            stores(((Node.AnnAssign) o).target, attrStores);
        } else if (o instanceof Node.Delete) {
            for (Expr t : ((Node.Delete) o).targets) {
                stores(t, attrStores);
            }
        } else if (o instanceof Node.For) {
            stores(((Node.For) o).target, attrStores);
        } else if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Name && ("setattr".equals(((Name) c.func).id) || "delattr".equals(((Name) c.func).id))
                    && c.args.size() >= 2 && c.args.get(1) instanceof Constant) {
                count(attrStores, ((Constant) c.args.get(1)).text);
            }
        }
        final ClassDef w = owner;
        Infer.children(o, x -> scan(x, w, classes, nameUses, attrStores, bindings));
    }

    private static void stores(Expr t, Map<String, Integer> attrStores) {
        if (t instanceof Attribute) {
            count(attrStores, ((Attribute) t).attr);
        } else if (t instanceof Node.TupleExpr) {
            for (Expr x : ((Node.TupleExpr) t).elts) {
                stores(x, attrStores);
            }
        } else if (t instanceof Node.ListExpr) {
            for (Expr x : ((Node.ListExpr) t).elts) {
                stores(x, attrStores);
            }
        } else if (t instanceof Node.Starred) {
            stores(((Node.Starred) t).value, attrStores);
        }
    }

    private static void count(Map<String, Integer> m, String k) {
        Integer n = m.get(k);
        m.put(k, n == null ? 1 : n + 1);
    }

    private static FunctionDef method(ClassDef d, String name) {
        FunctionDef f = null;
        for (Stmt s : d.body) {
            if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                if (f != null) {
                    return null;
                }
                f = (FunctionDef) s;
            }
        }
        return f;
    }

    /// The build-time value of `e` with `param` bound to `value`, or null.
    private static String eval(Expr e, String param, String value) {
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR) {
            return ((Constant) e).text;
        }
        if (e instanceof Name && param.equals(((Name) e).id)) {
            return value;
        }
        if (e instanceof Node.BinOp && "+".equals(((Node.BinOp) e).op)) {
            String l = eval(((Node.BinOp) e).left, param, value);
            String r = eval(((Node.BinOp) e).right, param, value);
            return l == null || r == null ? null : l + r;
        }
        return null;
    }

    private static boolean rebinds(FunctionDef f, final String p) {
        final boolean[] r = new boolean[1];
        walk(f.body, o -> {
            if (o instanceof Node.Assign) {
                for (Expr t : ((Node.Assign) o).targets) {
                    if (t instanceof Name && p.equals(((Name) t).id)) {
                        r[0] = true;
                    }
                }
            } else if (o instanceof Node.AugAssign && ((Node.AugAssign) o).target instanceof Name
                    && p.equals(((Name) ((Node.AugAssign) o).target).id)) {
                r[0] = true;
            }
        });
        return r[0];
    }

    private interface V {
        void see(Object o);
    }

    private static void walk(Object o, final V v) {
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                walk(x, v);
            }
            return;
        }
        v.see(o);
        Infer.children(o, x -> walk(x, v));
    }

    /// Writes the literal for `self.<field>` where it names an attribute.
    private static void fold(Object fn, final Map<String, String> fields) {
        final String self = ((FunctionDef) fn).args.positional.isEmpty() ? null
                : ((FunctionDef) fn).args.positional.get(0).name;
        if (self == null) {
            return;
        }
        walk(fn, o -> {
            if (!(o instanceof Call) || !(((Call) o).func instanceof Name)) {
                return;
            }
            Call c = (Call) o;
            String n = ((Name) c.func).id;
            if (!("getattr".equals(n) || "setattr".equals(n) || "hasattr".equals(n) || "delattr".equals(n))
                    || c.args.size() < 2 || !(c.args.get(1) instanceof Attribute)) {
                return;
            }
            Attribute a = (Attribute) c.args.get(1);
            if (a.value instanceof Name && self.equals(((Name) a.value).id) && fields.get(a.attr) != null) {
                c.args.set(1, new Constant(Constant.Kind.STR, fields.get(a.attr)));
            }
        });
    }

    /// `class S(*bases)` at the top level of a module, after `bases = (A,
    /// B)` there, where `bases` appears nowhere else in the module and each
    /// of `A`, `B` is bound once: the tuple's names are written in.
    /// Anything else is left to the strict check.
    private static void foldStarBases(Node.Module m) {
        final Map<String, Integer> uses = new HashMap<String, Integer>();
        final Map<String, Integer> binds = new HashMap<String, Integer>();
        final boolean[] dynamic = {false};
        // Module scope only: a parameter or local of the same name in a
        // function or class body is another variable, and only a `global`
        // (refused below) rebinds the module's from there.
        walkModuleScope(m, o -> {
            if (o instanceof Name) {
                count(uses, ((Name) o).id);
            } else if (o instanceof Node.Arg) {
                count(uses, ((Node.Arg) o).name);
                count(binds, ((Node.Arg) o).name);
            } else if (o instanceof Node.Alias) {
                Node.Alias al = (Node.Alias) o;
                String n = al.asname != null ? al.asname : al.name;
                count(uses, n);
                count(binds, n);
            } else if (o instanceof FunctionDef) {
                count(uses, ((FunctionDef) o).name);
                count(binds, ((FunctionDef) o).name);
            } else if (o instanceof ClassDef) {
                count(binds, ((ClassDef) o).name);
            } else if (o instanceof Node.Global || o instanceof Node.Delete) {
                dynamic[0] = true;
            }
            if (o instanceof Node.Assign) {
                for (Expr t : ((Node.Assign) o).targets) {
                    bindName(t, binds);
                }
            } else if (o instanceof Node.AugAssign) {
                bindName(((Node.AugAssign) o).target, binds);
            } else if (o instanceof Node.For) {
                bindName(((Node.For) o).target, binds);
            }
        });
        if (dynamic[0]) {
            return;
        }
        Map<String, Node.TupleExpr> tuples = new HashMap<String, Node.TupleExpr>();
        for (Stmt s : m.body) {
            if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                    && ((Node.Assign) s).targets.get(0) instanceof Name
                    && ((Node.Assign) s).value instanceof Node.TupleExpr) {
                Node.TupleExpr t = (Node.TupleExpr) ((Node.Assign) s).value;
                boolean names = true;
                for (Expr x : t.elts) {
                    names &= x instanceof Name && once(binds, ((Name) x).id);
                }
                if (names) {
                    tuples.put(((Name) ((Node.Assign) s).targets.get(0)).id, t);
                }
                continue;
            }
            if (!(s instanceof ClassDef)) {
                continue;
            }
            List<Expr> bases = ((ClassDef) s).bases;
            List<Expr> folded = new ArrayList<Expr>();
            boolean changed = false;
            for (Expr e : bases) {
                Node.TupleExpr t = null;
                if (e instanceof Node.Starred && ((Node.Starred) e).value instanceof Name) {
                    String n = ((Name) ((Node.Starred) e).value).id;
                    // The tuple's own binding and this one use.
                    if (once(binds, n) && Integer.valueOf(2).equals(uses.get(n))) {
                        t = tuples.get(n);
                    }
                }
                if (t == null) {
                    folded.add(e);
                } else {
                    changed = true;
                    for (Expr x : t.elts) {
                        folded.add(new Name(((Name) x).id));
                    }
                }
            }
            if (changed) {
                bases.clear();
                bases.addAll(folded);
            }
        }
    }

    private static void walkModuleScope(Object o, final V v) {
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                walkModuleScope(x, v);
            }
            return;
        }
        v.see(o);
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Lambda
                || o instanceof Node.Comp) {
            // The name a def or class binds is seen; its body is another
            // scope. A `global` inside it is still found.
            walk(o, x -> {
                if (x instanceof Node.Global) {
                    v.see(x);
                }
            });
            if (o instanceof ClassDef) {
                for (Expr b : ((ClassDef) o).bases) {
                    walkModuleScope(b, v);
                }
            }
            return;
        }
        Infer.children(o, x -> walkModuleScope(x, v));
    }

    private static boolean once(Map<String, Integer> binds, String n) {
        return Integer.valueOf(1).equals(binds.get(n));
    }

    private static void bindName(Expr t, Map<String, Integer> binds) {
        if (t instanceof Name) {
            count(binds, ((Name) t).id);
        } else if (t instanceof Node.TupleExpr) {
            for (Expr x : ((Node.TupleExpr) t).elts) {
                bindName(x, binds);
            }
        } else if (t instanceof Node.ListExpr) {
            for (Expr x : ((Node.ListExpr) t).elts) {
                bindName(x, binds);
            }
        } else if (t instanceof Node.Starred) {
            bindName(((Node.Starred) t).value, binds);
        }
    }

    /// Records what every class's subclasses bind over it, for the layout
    /// plan: a field a subclass of another module turns into a property is
    /// then no field of the base (Layouts).
    public static void subclassBindings(java.util.Collection<Node.Module> modules) {
        Layouts.subclassBindings(modules);
    }
}
