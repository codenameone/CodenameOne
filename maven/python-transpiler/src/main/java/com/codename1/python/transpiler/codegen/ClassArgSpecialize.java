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
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Folds an instance field that names an attribute to the one literal every
/// construction gives it.
///
/// openpyxl's `BoundDictionary` keeps a constructor argument and uses it as
/// an attribute name:
///
/// ```python
/// def __init__(self, reference=None, *args, **kw):
///     self.reference = reference
/// def __getitem__(self, key):
///     ...
///     setattr(value, self.reference, key)
/// ```
///
/// Its one construction is `DimensionHolder`'s `super().__init__(reference,
/// ...)`, whose own `reference` parameter defaults to `"index"` and which no
/// caller passes. When every construction of the class -- direct calls, and
/// calls of subclasses that forward one of their own parameters or a literal
/// through `super().__init__` -- gives the field the same string literal, and
/// nothing else in the closed program can write the field, the field's
/// value is that literal for every instance, and `setattr(value,
/// self.reference, key)` is written `setattr(value, "index", key)`.
///
/// It refuses anything it cannot prove: a class name used other than to
/// call it, subclass it or test `isinstance` against it (a class object that
/// escapes can be constructed where no call site shows); a class or
/// subclass that reaches `type(...)`, `__class__`, `cls(...)`, `__dict__` or
/// defines `__setattr__` or the field's name in its body; a store of the
/// field anywhere but the one in `__init__` (by attribute, `setattr` with
/// the literal or `del`); a call with `*`/`**` arguments; a forwarded
/// parameter the subclass rebinds; and a field whose constructions disagree.
/// Two literals would mean cloning the class per literal, which this pass
/// does not do: identity, `isinstance` and subclassing would all have to be
/// rewritten with it. A computed `setattr` elsewhere is not considered: the
/// strict build refuses those.
public final class ClassArgSpecialize {

    private static final String STORE_PREFIX = "_cn1_set_";

    private ClassArgSpecialize() {
    }

    /// Folds every qualifying field among `trees`; `libTrees` are the bundled
    /// modules, read only so their uses count against the rewrite.
    public static void rewrite(Map<String, Node.Module> trees, Collection<Node.Module> libTrees) {
        List<Object> all = new ArrayList<Object>(trees.values());
        all.addAll(libTrees);
        final Map<String, List<ClassDef>> classes = new HashMap<String, List<ClassDef>>();
        final IdentityHashMap<Object, Boolean> allowed = new IdentityHashMap<Object, Boolean>();
        final Set<String> escaped = new LinkedHashSet<String>();
        final Map<String, List<Call>> callSites = new HashMap<String, List<Call>>();
        final Map<String, Integer> fieldStores = new HashMap<String, Integer>();
        for (Object m : all) {
            scan(m, classes, allowed, escaped, callSites, fieldStores);
        }
        for (Object m : trees.values()) {
            candidates(m, classes, escaped, callSites, fieldStores);
        }
    }

    private static void scan(Object o, Map<String, List<ClassDef>> classes, IdentityHashMap<Object, Boolean> allowed,
            Set<String> escaped, Map<String, List<Call>> callSites, Map<String, Integer> fieldStores) {
        if (o instanceof FunctionDef && ((FunctionDef) o).name.startsWith(STORE_PREFIX)
                && storeMethod((FunctionDef) o) != null) {
            // LiteralAttrStore's `def _cn1_set_x(self, target, value):
            // target.<lit> = value`: counted per caller in storeOwners.
            count(fieldStores, STORE_PREFIX + storeMethod((FunctionDef) o));
            return;
        }
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            List<ClassDef> l = classes.get(c.name);
            if (l == null) {
                l = new ArrayList<ClassDef>();
                classes.put(c.name, l);
            }
            l.add(c);
            for (Expr b : c.bases) {
                allowed.put(b, Boolean.TRUE);
            }
        } else if (o instanceof Call) {
            Call c = (Call) o;
            String n = null;
            if (c.func instanceof Name) {
                n = ((Name) c.func).id;
            } else if (c.func instanceof Attribute) {
                n = ((Attribute) c.func).attr;
            }
            if (n != null) {
                allowed.put(c.func, Boolean.TRUE);
                List<Call> l = callSites.get(n);
                if (l == null) {
                    l = new ArrayList<Call>();
                    callSites.put(n, l);
                }
                l.add(c);
            }
            if (("isinstance".equals(n) || "issubclass".equals(n)) && c.args.size() == 2) {
                Expr t = c.args.get(1);
                allowed.put(t, Boolean.TRUE);
                if (t instanceof Node.TupleExpr) {
                    for (Expr x : ((Node.TupleExpr) t).elts) {
                        allowed.put(x, Boolean.TRUE);
                    }
                }
            }
            if (("setattr".equals(n) || "delattr".equals(n)) && c.args.size() >= 2
                    && c.args.get(1) instanceof Constant) {
                count(fieldStores, ((Constant) c.args.get(1)).text);
            }
        } else if (o instanceof Node.Assign) {
            Node.Assign a = (Node.Assign) o;
            if (isAll(a)) {
                return;
            }
            for (Expr t : a.targets) {
                stores(t, fieldStores);
            }
        } else if (o instanceof Node.AugAssign) {
            stores(((Node.AugAssign) o).target, fieldStores);
        } else if (o instanceof Node.AnnAssign) {
            stores(((Node.AnnAssign) o).target, fieldStores);
        } else if (o instanceof Node.Delete) {
            for (Expr t : ((Node.Delete) o).targets) {
                stores(t, fieldStores);
            }
        } else if (o instanceof Node.For) {
            stores(((Node.For) o).target, fieldStores);
        } else if (o instanceof Node.WithItem) {
            stores(((Node.WithItem) o).vars, fieldStores);
        } else if (o instanceof Name) {
            if (!allowed.containsKey(o)) {
                escaped.add(((Name) o).id);
            }
        } else if (o instanceof Attribute) {
            if (!allowed.containsKey(o)) {
                escaped.add(((Attribute) o).attr);
            }
        } else if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            escaped.add(((Constant) o).text);
        }
        final Map<String, List<ClassDef>> cl = classes;
        Infer.children(o, x -> scan(x, cl, allowed, escaped, callSites, fieldStores));
    }

    /// The attribute a LiteralAttrStore store method writes, or null when
    /// `f` is not exactly `target.<attr> = value`.
    private static String storeMethod(FunctionDef f) {
        if (f.args.positional.size() != 3 || f.body.size() != 1 || !(f.body.get(0) instanceof Node.Assign)) {
            return null;
        }
        Node.Assign a = (Node.Assign) f.body.get(0);
        if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Attribute)) {
            return null;
        }
        Attribute t = (Attribute) a.targets.get(0);
        if (!(t.value instanceof Name) || !f.args.positional.get(1).name.equals(((Name) t.value).id)) {
            return null;
        }
        return t.attr;
    }

    /// True when every store method writing `field` is called only with the
    /// caller's own `self` as target, from a class outside `family`.
    private static boolean storeMethodsSafe(String field, Map<String, List<ClassDef>> classes,
            Map<String, List<Call>> callSites, List<ClassDef> family) {
        Set<String> fam = new LinkedHashSet<String>();
        for (ClassDef k : family) {
            fam.add(k.name);
        }
        Set<String> methods = new LinkedHashSet<String>();
        for (List<ClassDef> l : classes.values()) {
            for (ClassDef k : l) {
                for (Stmt s : k.body) {
                    if (s instanceof FunctionDef && ((FunctionDef) s).name.startsWith(STORE_PREFIX)
                            && field.equals(storeMethod((FunctionDef) s))) {
                        methods.add(((FunctionDef) s).name);
                    }
                }
            }
        }
        final IdentityHashMap<Object, Boolean> ok = new IdentityHashMap<Object, Boolean>();
        for (List<ClassDef> l : classes.values()) {
            for (ClassDef k : l) {
                if (fam.contains(k.name)) {
                    continue;
                }
                for (Stmt s : k.body) {
                    if (!(s instanceof FunctionDef) || ((FunctionDef) s).args.positional.isEmpty()
                            || rebinds(((FunctionDef) s).body, ((FunctionDef) s).args.positional.get(0).name)) {
                        continue;
                    }
                    final String self = ((FunctionDef) s).args.positional.get(0).name;
                    walk(s, x -> {
                        if (x instanceof Call && !((Call) x).args.isEmpty() && ((Call) x).args.get(0) instanceof Name
                                && self.equals(((Name) ((Call) x).args.get(0)).id)) {
                            ok.put(x, Boolean.TRUE);
                        }
                    });
                }
            }
        }
        for (String m : methods) {
            List<Call> cs = callSites.get(m);
            if (cs == null) {
                continue;
            }
            for (Call c : cs) {
                if (!ok.containsKey(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isAll(Node.Assign a) {
        return a.targets.size() == 1 && a.targets.get(0) instanceof Name
                && "__all__".equals(((Name) a.targets.get(0)).id);
    }

    private static void stores(Object t, Map<String, Integer> fieldStores) {
        if (t instanceof Attribute) {
            count(fieldStores, ((Attribute) t).attr);
        } else if (t instanceof Node.TupleExpr) {
            for (Expr x : ((Node.TupleExpr) t).elts) {
                stores(x, fieldStores);
            }
        } else if (t instanceof Node.ListExpr) {
            for (Expr x : ((Node.ListExpr) t).elts) {
                stores(x, fieldStores);
            }
        } else if (t instanceof Node.Starred) {
            stores(((Node.Starred) t).value, fieldStores);
        }
    }

    private static void count(Map<String, Integer> m, String k) {
        Integer n = m.get(k);
        m.put(k, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
    }

    private static void candidates(Object o, Map<String, List<ClassDef>> classes, Set<String> escaped,
            Map<String, List<Call>> callSites, Map<String, Integer> fieldStores) {
        if (o instanceof ClassDef) {
            fold((ClassDef) o, classes, escaped, callSites, fieldStores);
        }
        Infer.children(o, x -> candidates(x, classes, escaped, callSites, fieldStores));
    }

    private static void fold(ClassDef c, Map<String, List<ClassDef>> classes, Set<String> escaped,
            Map<String, List<Call>> callSites, Map<String, Integer> fieldStores) {
        if (escaped.contains(c.name) || classes.get(c.name).size() != 1 || !c.decorators.isEmpty()) {
            return;
        }
        FunctionDef init = method(c, "__init__");
        if (init == null || !init.decorators.isEmpty() || init.args.positional.isEmpty()) {
            return;
        }
        String self = init.args.positional.get(0).name;
        for (Stmt s : init.body) {
            if (!(s instanceof Node.Assign)) {
                continue;
            }
            Node.Assign a = (Node.Assign) s;
            if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Attribute) || !(a.value instanceof Name)) {
                continue;
            }
            Attribute t = (Attribute) a.targets.get(0);
            String p = ((Name) a.value).id;
            if (!(t.value instanceof Name) || !self.equals(((Name) t.value).id) || param(init, p) < 1
                    || !Integer.valueOf(1).equals(fieldStores.get(t.attr)) || rebinds(init.body, p)
                    || rebinds(init.body, self)) {
                continue;
            }
            String field = t.attr;
            List<ClassDef> family = new ArrayList<ClassDef>();
            if (!family(c, classes, escaped, family) || !usesAsName(c, field)
                    || fieldStores.containsKey(STORE_PREFIX + field)
                    && !storeMethodsSafe(field, classes, callSites, family)) {
                continue;
            }
            boolean ok = true;
            for (ClassDef k : family) {
                ok &= closed(k, field);
            }
            Set<String> values = new LinkedHashSet<String>();
            if (!ok || !values(c, init, p, classes, callSites, values) || values.size() != 1) {
                continue;
            }
            String lit = values.iterator().next();
            if (lit == null) {
                continue;
            }
            for (ClassDef k : family) {
                for (Stmt m : k.body) {
                    if (m instanceof FunctionDef) {
                        FunctionDef f = (FunctionDef) m;
                        if (!f.args.positional.isEmpty() && f.decorators.isEmpty()
                                && !rebinds(f.body, f.args.positional.get(0).name)) {
                            substitute(f.body, f.args.positional.get(0).name, field, lit);
                        }
                    }
                }
            }
        }
    }

    private static FunctionDef method(ClassDef c, String name) {
        FunctionDef r = null;
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                if (r != null) {
                    return null;
                }
                r = (FunctionDef) s;
            }
        }
        return r;
    }

    private static int param(FunctionDef f, String p) {
        List<Node.Arg> ps = f.args.positional;
        for (int i = 0; i < ps.size(); i++) {
            if (p.equals(ps.get(i).name)) {
                return i;
            }
        }
        return -1;
    }

    /// The class and all its subclasses, or false when one of them escapes
    /// or names the class other than by a plain base.
    private static boolean family(ClassDef c, Map<String, List<ClassDef>> classes, Set<String> escaped,
            List<ClassDef> out) {
        out.add(c);
        for (List<ClassDef> l : classes.values()) {
            for (ClassDef k : l) {
                for (Expr b : k.bases) {
                    boolean named = b instanceof Name && c.name.equals(((Name) b).id);
                    if (b instanceof Attribute && c.name.equals(((Attribute) b).attr)) {
                        return false;
                    }
                    if (named) {
                        if (k.bases.size() != 1 || !k.keywords.isEmpty() || escaped.contains(k.name)
                                || l.size() != 1 || !family(k, classes, escaped, out)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /// True when a method of `c` names an attribute by `self.field`.
    private static boolean usesAsName(ClassDef c, final String field) {
        final boolean[] found = {false};
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && !((FunctionDef) s).args.positional.isEmpty()) {
                final String self = ((FunctionDef) s).args.positional.get(0).name;
                walk(s, x -> {
                    if (nameArg(x, self, field) != null) {
                        found[0] = true;
                    }
                });
            }
        }
        return found[0];
    }

    private static Call nameArg(Object x, String self, String field) {
        if (!(x instanceof Call)) {
            return null;
        }
        Call c = (Call) x;
        if (!(c.func instanceof Name) || c.args.size() < 2 || !(c.args.get(1) instanceof Attribute)) {
            return null;
        }
        String n = ((Name) c.func).id;
        if (!"getattr".equals(n) && !"setattr".equals(n) && !"hasattr".equals(n) && !"delattr".equals(n)) {
            return null;
        }
        Attribute a = (Attribute) c.args.get(1);
        if (field.equals(a.attr) && a.value instanceof Name && self.equals(((Name) a.value).id)) {
            return c;
        }
        return null;
    }

    private static void substitute(Object o, final String self, final String field, final String lit) {
        walk(o, x -> {
            Call c = nameArg(x, self, field);
            if (c != null) {
                c.args.set(1, new Constant(Constant.Kind.STR, lit));
            }
        });
    }

    /// True when nothing in `k` can construct it behind a call site's back,
    /// or write `field` but the one store.
    private static boolean closed(ClassDef k, final String field) {
        for (Stmt s : k.body) {
            if (s instanceof FunctionDef) {
                String n = ((FunctionDef) s).name;
                if (field.equals(n) || "__setattr__".equals(n) || "__getattribute__".equals(n)
                        || "__new__".equals(n) || "__init_subclass__".equals(n)) {
                    return false;
                }
            } else if (!(s instanceof Node.Assign) && !(s instanceof Node.ExprStmt) && !(s instanceof Node.Pass)) {
                return false;
            } else if (s instanceof Node.Assign) {
                for (Expr t : ((Node.Assign) s).targets) {
                    if (!(t instanceof Name) || field.equals(((Name) t).id) || "__slots__".equals(((Name) t).id)) {
                        return false;
                    }
                }
            }
        }
        final boolean[] bad = {false};
        walk(k.body, x -> {
            if (x instanceof Attribute) {
                String a = ((Attribute) x).attr;
                bad[0] |= "__class__".equals(a) || "__dict__".equals(a);
            } else if (x instanceof Call && ((Call) x).func instanceof Name) {
                String n = ((Name) ((Call) x).func).id;
                bad[0] |= "type".equals(n) || "cls".equals(n) || "vars".equals(n);
            }
        });
        return !bad[0];
    }

    /// Adds to `out` every value parameter `p` of `init` takes when `k` (or
    /// a subclass relying on it) is constructed; null stands for None.
    private static boolean values(ClassDef k, FunctionDef init, String p, Map<String, List<ClassDef>> classes,
            Map<String, List<Call>> callSites, Set<String> out) {
        List<Call> cs = callSites.get(k.name);
        if (cs != null) {
            for (Call c : cs) {
                if (!bind(c, init, p, 1, out)) {
                    return false;
                }
            }
        }
        for (List<ClassDef> l : classes.values()) {
            for (ClassDef s : l) {
                if (s.bases.size() != 1 || !(s.bases.get(0) instanceof Name)
                        || !k.name.equals(((Name) s.bases.get(0)).id)) {
                    continue;
                }
                FunctionDef own = method(s, "__init__");
                if (own == null) {
                    if (hasMethod(s, "__init__") || !values(s, init, p, classes, callSites, out)) {
                        return false;
                    }
                    continue;
                }
                List<Call> sup = superInits(s, k.name);
                if (sup.size() != 1 || !contains(own.body, sup.get(0))) {
                    return false;
                }
                Set<String> fwd = new LinkedHashSet<String>();
                Expr arg = argFor(sup.get(0), init, p, sup.get(0).func instanceof Attribute
                        && ((Attribute) sup.get(0).func).value instanceof Name ? 0 : 1);
                if (arg == null) {
                    return false;
                }
                if (arg instanceof Name) {
                    String q = ((Name) arg).id;
                    if (param(own, q) < 1 || rebinds(own.body, q)
                            || !values(s, own, q, classes, callSites, fwd)) {
                        return false;
                    }
                    out.addAll(fwd);
                } else if (!literal(arg, out)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasMethod(ClassDef c, String n) {
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && n.equals(((FunctionDef) s).name)) {
                return true;
            }
        }
        return false;
    }

    /// The `super().__init__(...)` and `Base.__init__(self, ...)` calls in `s`.
    private static List<Call> superInits(ClassDef s, final String base) {
        final List<Call> r = new ArrayList<Call>();
        walk(s.body, x -> {
            if (x instanceof Call && ((Call) x).func instanceof Attribute
                    && "__init__".equals(((Attribute) ((Call) x).func).attr)) {
                Expr v = ((Attribute) ((Call) x).func).value;
                if (v instanceof Call && ((Call) v).func instanceof Name && "super".equals(((Name) ((Call) v).func).id)
                        && ((Call) v).args.isEmpty()) {
                    r.add((Call) x);
                } else if (v instanceof Name && base.equals(((Name) v).id)) {
                    r.add((Call) x);
                } else {
                    // Some other __init__ call: unknown, so make it ambiguous.
                    r.add((Call) x);
                    r.add((Call) x);
                }
            }
        });
        return r;
    }

    private static boolean contains(Object o, final Object target) {
        final boolean[] f = {false};
        walk(o, x -> f[0] |= x == target); //NOPMD CompareObjectsWithEquals - identity is meant
        return f[0];
    }

    /// Binds call `c` of a constructor whose `__init__` is `init`; adds the
    /// value parameter `p` takes.
    private static boolean bind(Call c, FunctionDef init, String p, int skip, Set<String> out) {
        Expr a = argFor(c, init, p, skip);
        return a != null && literal(a, out);
    }

    /// The expression parameter `p` takes in call `c` (its default when the
    /// call leaves it out), or null when that cannot be told. `skip` is how
    /// many leading parameters the call does not pass (1 for self).
    private static Expr argFor(Call c, FunctionDef init, String p, int skip) {
        for (Expr x : c.args) {
            if (x instanceof Node.Starred) {
                return null;
            }
        }
        Expr found = null;
        for (Node.Keyword k : c.keywords) {
            if (k.arg == null) {
                return null;
            }
            if (p.equals(k.arg)) {
                found = k.value;
            }
        }
        int idx = param(init, p);
        int pos = idx - skip;
        if (pos < c.args.size()) {
            return found == null ? c.args.get(pos) : null;
        }
        if (found != null) {
            return found;
        }
        List<Expr> d = init.args.defaults;
        int di = idx - (init.args.positional.size() - d.size());
        return di >= 0 ? d.get(di) : null;
    }

    private static boolean literal(Expr a, Set<String> out) {
        if (a instanceof Constant) {
            Constant k = (Constant) a;
            if (k.kind == Constant.Kind.STR && ConstArgSpecialize.ident(k.text)) {
                out.add(k.text);
                return true;
            }
            if (k.kind == Constant.Kind.NONE) {
                out.add(null);
                return true;
            }
        }
        return false;
    }

    /// True when `o` might rebind `p` or hide it in a nested scope.
    private static boolean rebinds(Object o, final String p) {
        final boolean[] r = {false};
        walk(o, x -> {
            List<Object> ts = new ArrayList<Object>();
            if (x instanceof Node.Lambda || x instanceof FunctionDef || x instanceof ClassDef
                    || x instanceof Node.Global) {
                r[0] = true;
            } else if (x instanceof Node.Assign) {
                ts.addAll(((Node.Assign) x).targets);
            } else if (x instanceof Node.AugAssign) {
                ts.add(((Node.AugAssign) x).target);
            } else if (x instanceof Node.AnnAssign) {
                ts.add(((Node.AnnAssign) x).target);
            } else if (x instanceof Node.For) {
                ts.add(((Node.For) x).target);
            } else if (x instanceof Node.WithItem) {
                ts.add(((Node.WithItem) x).vars);
            } else if (x instanceof Node.NamedExpr) {
                ts.add(((Node.NamedExpr) x).target);
            } else if (x instanceof Node.Delete) {
                ts.addAll(((Node.Delete) x).targets);
            } else if (x instanceof Node.ExceptHandler && p.equals(((Node.ExceptHandler) x).name)) {
                r[0] = true;
            }
            for (Object t : ts) {
                walk(t, y -> r[0] |= y instanceof Name && p.equals(((Name) y).id)
                        && !(t instanceof Attribute) && !(t instanceof Node.Subscript));
            }
        });
        return r[0];
    }

    private static void walk(Object o, Infer.Visit v) {
        v.on(o);
        Infer.children(o, x -> walk(x, v));
    }
}
