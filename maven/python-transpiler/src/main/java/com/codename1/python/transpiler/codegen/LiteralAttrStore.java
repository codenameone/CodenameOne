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
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// `setattr(obj, axis.tagname, value)` where every class of the program
/// binds `tagname` in its body to a string literal, and nothing anywhere
/// stores, deletes or otherwise binds it: the name stored is a constant of
/// `axis`'s class. Each class binding the attribute gets a method
/// `_cn1_set_tagname(self, target, value)` storing `target.<its literal>`,
/// and the call becomes `axis._cn1_set_tagname(obj, value)`: a method call
/// picks the class's own store, as reading the attribute picked its own
/// name, with no name looked up at run time. openpyxl's
/// `PlotArea.to_tree` stores each chart axis under its axis's tag name.
///
/// Only an attribute read whose every binding is such a literal qualifies:
/// a class binding it any other way, an instance store of it (`x.tagname =
/// ...`, `del x.tagname`, `setattr(x, "tagname", ...)`) or a literal that
/// is not an identifier leaves every call as it is.
public final class LiteralAttrStore {
    private static final String PREFIX = "_cn1_set_";
    /// The classes whose binding of the attribute is a property that only
    /// raises, with that property; one rewrite at a time.
    private static final ThreadLocal<Map<ClassDef, FunctionDef>> RAISING = new ThreadLocal<Map<ClassDef, FunctionDef>>();

    private LiteralAttrStore() {
    }

    public static void rewrite(Map<String, Node.Module> trees) {
        rewrite(trees, java.util.Collections.<Node.Module>emptyList());
    }

    /// @param library the bundled standard library and site modules the
    ///        program will import, which are added after this pass runs:
    ///        they are only scanned, so a store of the attribute there
    ///        (or a class binding it otherwise) disqualifies it as one in
    ///        the program would
    public static void rewrite(Map<String, Node.Module> trees, java.util.Collection<Node.Module> library) {
        // The attribute names a setattr reads its name from.
        final Set<String> wanted = new HashSet<String>();
        for (Node.Module m : trees.values()) {
            walk(m, new Infer.Visit() {
                @Override
                public void on(Object o) {
                    String a = target(o);
                    if (a != null) {
                        wanted.add(a);
                    }
                    walk(o, this);
                }
            });
        }
        if (wanted.isEmpty()) {
            return;
        }
        // Each wanted attribute's class-body literals; a bad binding
        // anywhere disqualifies it.
        final Set<String> bad = new HashSet<String>();
        final Map<ClassDef, FunctionDef> raising = new HashMap<ClassDef, FunctionDef>();
        RAISING.set(raising);
        final Map<ClassDef, Map<String, String>> literals = new HashMap<ClassDef, Map<String, String>>();
        for (Node.Module m : library) {
            walk(m, new Infer.Visit() {
                @Override
                public void on(Object o) {
                    if (o instanceof ClassDef) {
                        // No store method is added to a library class, so
                        // any binding of the attribute there is a bad one.
                        Map<ClassDef, Map<String, String>> none = new HashMap<ClassDef, Map<String, String>>();
                        classBody((ClassDef) o, wanted, bad, none);
                        FunctionDef r = raising.remove((ClassDef) o);
                        if (r != null) {
                            bad.add(r.name);
                        }
                        for (Map<String, String> l : none.values()) {
                            bad.addAll(l.keySet());
                        }
                    } else {
                        stores(o, wanted, bad);
                    }
                    walk(o, this);
                }
            });
        }
        for (Node.Module m : trees.values()) {
            walk(m, new Infer.Visit() {
                @Override
                public void on(Object o) {
                    if (o instanceof ClassDef) {
                        classBody((ClassDef) o, wanted, bad, literals);
                    } else {
                        stores(o, wanted, bad);
                    }
                    walk(o, this);
                }
            });
        }
        wanted.removeAll(bad);
        if (wanted.isEmpty()) {
            return;
        }
        // A class named as a later base (openpyxl's `_3DBase` beside
        // `_BarChartBase`) takes no store: a method a mixin binds and the
        // first base's chain does not costs every chart class its layout.
        // Only where each class naming it as a base binds the attribute
        // itself, so every subclass instance has its own store; an
        // instance of the mixin itself, an abstract base, has none.
        Map<String, List<ClassDef>> byBase = new HashMap<String, List<ClassDef>>();
        final Set<String> later = new HashSet<String>();
        for (ClassDef c : allClasses(trees)) {
            for (int i = 0; i < c.bases.size(); i++) {
                Expr b = c.bases.get(i);
                String n = b instanceof Name ? ((Name) b).id : b instanceof Attribute ? ((Attribute) b).attr : null;
                if (n == null) {
                    continue;
                }
                if (i > 0) {
                    later.add(n);
                }
                List<ClassDef> l = byBase.get(n);
                if (l == null) {
                    l = new ArrayList<ClassDef>();
                    byBase.put(n, l);
                }
                l.add(c);
            }
        }
        for (java.util.Iterator<Map.Entry<ClassDef, Map<String, String>>> it = literals.entrySet().iterator(); it.hasNext();) {
            Map.Entry<ClassDef, Map<String, String>> e = it.next();
            if (!later.contains(e.getKey().name)) {
                continue;
            }
            it.remove();
            List<ClassDef> subs = byBase.get(e.getKey().name);
            for (String a : e.getValue().keySet()) {
                for (ClassDef sc : subs == null ? new ArrayList<ClassDef>() : subs) {
                    Map<String, String> own = literals.get(sc);
                    if (own == null || !own.containsKey(a)) {
                        wanted.remove(a);
                    }
                }
            }
        }
        if (wanted.isEmpty()) {
            return;
        }
        for (Map.Entry<ClassDef, Map<String, String>> e : literals.entrySet()) {
            for (Map.Entry<String, String> l : e.getValue().entrySet()) {
                if (wanted.contains(l.getKey())) {
                    ClassDef c = e.getKey();
                    FunctionDef f = ConstArgSpecialize.parseMethod("def " + PREFIX + l.getKey()
                            + "(self, target, value):\n    target." + l.getValue() + " = value\n",
                            Math.max(c.line, 1), "<" + PREFIX + l.getKey() + ">");
                    c.body.add(f);
                }
            }
        }
        for (Map.Entry<ClassDef, FunctionDef> e : raising.entrySet()) {
            FunctionDef p = e.getValue();
            if (wanted.contains(p.name)) {
                FunctionDef f = ConstArgSpecialize.parseMethod("def " + PREFIX + p.name
                        + "(self, target, value):\n    pass\n", Math.max(p.line, 1), "<" + PREFIX + p.name + ">");
                f.body.clear();
                f.body.addAll(p.body);
                e.getKey().body.add(f);
            }
        }
        RAISING.remove();
        for (Node.Module m : trees.values()) {
            replace(m.body, wanted);
        }
    }

    /// Walks the children of `o`, entering nested statement lists.
    private static void walk(Object o, Infer.Visit v) {
        Infer.children(o, v);
    }

    private static List<ClassDef> allClasses(Map<String, Node.Module> trees) {
        final List<ClassDef> out = new ArrayList<ClassDef>();
        for (Node.Module m : trees.values()) {
            walk(m, new Infer.Visit() {
                @Override
                public void on(Object o) {
                    if (o instanceof ClassDef) {
                        out.add((ClassDef) o);
                    }
                    walk(o, this);
                }
            });
        }
        return out;
    }

    /// The attribute `setattr(x, y.<attr>, v)` reads its name from.
    private static String target(Object o) {
        if (!(o instanceof Call)) {
            return null;
        }
        Call c = (Call) o;
        if (!(c.func instanceof Name) || !"setattr".equals(((Name) c.func).id) || c.args.size() != 3
                || !c.keywords.isEmpty() || !(c.args.get(1) instanceof Attribute)) {
            return null;
        }
        return ((Attribute) c.args.get(1)).attr;
    }

    private static void classBody(ClassDef c, Set<String> wanted, Set<String> bad,
            Map<ClassDef, Map<String, String>> literals) {
        for (Stmt s : c.body) {
            for (String a : wanted) {
                if (s instanceof Assign && ((Assign) s).targets.size() == 1
                        && ((Assign) s).targets.get(0) instanceof Name && a.equals(((Name) ((Assign) s).targets.get(0)).id)) {
                    Expr v = ((Assign) s).value;
                    Map<String, String> mine = literals.get(c);
                    if (v instanceof Constant && ((Constant) v).kind == Constant.Kind.STR && identifier(((Constant) v).text)
                            && (mine == null || !mine.containsKey(a))) {
                        if (mine == null) {
                            mine = new HashMap<String, String>();
                            literals.put(c, mine);
                        }
                        mine.put(a, ((Constant) v).text);
                    } else {
                        bad.add(a);
                    }
                } else if (raisingProperty(s, a)) {
                    // openpyxl's abstract `tagname` property: the store
                    // raises what reading the name raised.
                    Map<ClassDef, FunctionDef> mine = RAISING.get();
                    mine.put(c, (FunctionDef) s);
                } else if (binds(s, a)) {
                    bad.add(a);
                }
            }
        }
    }

    /// Whether a class-body statement binds `a` other than by a plain
    /// literal assignment (a def, an annotated or augmented assignment, an
    /// unpacking, an import).
    private static boolean binds(Stmt s, final String a) {
        if (s instanceof FunctionDef) {
            return a.equals(((FunctionDef) s).name);
        }
        if (s instanceof ClassDef) {
            return a.equals(((ClassDef) s).name);
        }
        if (s instanceof Node.Import || s instanceof Node.ImportFrom) {
            return true;
        }
        if (s instanceof Node.If || s instanceof Node.For || s instanceof Node.While || s instanceof Node.Try
                || s instanceof Node.With) {
            // A conditional or looped binding: judged by any Name of `a`.
            final boolean[] hit = {false};
            Infer.children(s, new Infer.Visit() {
                @Override
                public void on(Object o) {
                    if (o instanceof Name && a.equals(((Name) o).id)) {
                        hit[0] = true;
                    } else if (!(o instanceof FunctionDef) && !(o instanceof ClassDef)) {
                        Infer.children(o, this);
                    }
                }
            });
            return hit[0];
        }
        final boolean[] hit = {false};
        Expr[] targets = s instanceof Assign ? ((Assign) s).targets.toArray(new Expr[0])
                : s instanceof Node.AnnAssign ? new Expr[] {((Node.AnnAssign) s).target}
                : s instanceof Node.AugAssign ? new Expr[] {((Node.AugAssign) s).target}
                : s instanceof Node.Delete ? ((Node.Delete) s).targets.toArray(new Expr[0]) : new Expr[0];
        for (Expr t : targets) {
            if (t instanceof Name) {
                hit[0] |= a.equals(((Name) t).id);
            } else {
                Infer.children(t, new Infer.Visit() {
                    @Override
                    public void on(Object o) {
                        if (o instanceof Name && a.equals(((Name) o).id)) {
                            hit[0] = true;
                        }
                        Infer.children(o, this);
                    }
                });
            }
        }
        return hit[0];
    }

    /// Marks a wanted attribute stored, deleted or set by a literal name.
    private static void stores(Object o, Set<String> wanted, Set<String> bad) {
        List<Expr> targets = new ArrayList<Expr>();
        if (o instanceof Assign) {
            targets.addAll(((Assign) o).targets);
        } else if (o instanceof Node.AnnAssign) {
            targets.add(((Node.AnnAssign) o).target);
        } else if (o instanceof Node.AugAssign) {
            targets.add(((Node.AugAssign) o).target);
        } else if (o instanceof Node.Delete) {
            targets.addAll(((Node.Delete) o).targets);
        } else if (o instanceof Node.For) {
            targets.add(((Node.For) o).target);
        } else if (o instanceof Call && ((Call) o).func instanceof Name && ((Call) o).args.size() >= 2
                && ("setattr".equals(((Name) ((Call) o).func).id) || "delattr".equals(((Name) ((Call) o).func).id))
                && ((Call) o).args.get(1) instanceof Constant && wanted.contains(((Constant) ((Call) o).args.get(1)).text)) {
            bad.add(((Constant) ((Call) o).args.get(1)).text);
        }
        for (Expr t : targets) {
            final Set<String> found = new HashSet<String>();
            Infer.Visit v = new Infer.Visit() {
                @Override
                public void on(Object x) {
                    if (x instanceof Attribute) {
                        found.add(((Attribute) x).attr);
                    }
                }
            };
            v.on(t);
            if (t instanceof Node.TupleExpr || t instanceof Node.ListExpr || t instanceof Node.Starred) {
                Infer.children(t, v);
            }
            for (String f : found) {
                if (wanted.contains(f)) {
                    bad.add(f);
                }
            }
        }
    }

    private static void replace(Object o, final Set<String> wanted) {
        if (o instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> l = (List<Object>) o;
            for (int i = 0; i < l.size(); i++) {
                Object x = l.get(i);
                if (x instanceof ExprStmt) {
                    String a = target(((ExprStmt) x).value);
                    if (a != null && wanted.contains(a)) {
                        Call old = (Call) ((ExprStmt) x).value;
                        Call c = new Call(new Attribute(((Attribute) old.args.get(1)).value, PREFIX + a));
                        c.args.add(old.args.get(0));
                        c.args.add(old.args.get(2));
                        c.line = old.line;
                        c.col = old.col;
                        c.func.line = old.line;
                        c.func.col = old.col;
                        ExprStmt n = new ExprStmt(c);
                        n.line = ((ExprStmt) x).line;
                        n.col = ((ExprStmt) x).col;
                        l.set(i, n);
                        continue;
                    }
                }
                replace(x, wanted);
            }
            return;
        }
        if (o instanceof Node) {
            final List<Object> lists = new ArrayList<Object>();
            for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !List.class.isAssignableFrom(f.getType())) {
                        continue;
                    }
                    try {
                        lists.add(f.get(o));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            for (Object l : lists) {
                replace(l, wanted);
            }
        }
    }

    /// `@property def a(self): raise X`, a docstring allowed before it.
    private static boolean raisingProperty(Stmt s, String a) {
        if (!(s instanceof FunctionDef) || !a.equals(((FunctionDef) s).name)) {
            return false;
        }
        FunctionDef f = (FunctionDef) s;
        if (f.decorators.size() != 1 || !(f.decorators.get(0) instanceof Name)
                || !"property".equals(((Name) f.decorators.get(0)).id) || f.body.isEmpty()) {
            return false;
        }
        for (int i = 0; i < f.body.size() - 1; i++) {
            Stmt b = f.body.get(i);
            if (!(b instanceof ExprStmt) || !(((ExprStmt) b).value instanceof Constant)) {
                return false;
            }
        }
        return f.body.get(f.body.size() - 1) instanceof Node.Raise;
    }

    private static boolean identifier(String s) {
        if (s.isEmpty() || !(Character.isLetter(s.charAt(0)) || s.charAt(0) == '_') || s.charAt(0) > 0x7e) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 0x7e || !(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return !"None".equals(s) && !"True".equals(s) && !"False".equals(s);
    }
}
