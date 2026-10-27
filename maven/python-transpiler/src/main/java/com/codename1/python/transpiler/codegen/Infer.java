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
import com.codename1.python.transpiler.ast.Scope;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Whole-module type inference over a closed program.
///
/// Runs only when `Closed` found nothing in the application that could
/// change a class, an instance's attributes or a module global behind the
/// compiler's back. Then every value that reaches a parameter, a field, a
/// list element or a global is one this pass sees, and the types it gives
/// are sound: typed code never needs to check them again, and a function
/// is entered typed after one check of its arguments at the door.
///
/// It is a flow-insensitive fixpoint. Each round walks the whole module --
/// nested functions, lambdas and class bodies included, where names are
/// simply `DYN` -- joining the type of every stored value into where it is
/// stored: a local of a compiled function, a parameter at each call site
/// that names its callee statically, a field, the elements of the lists
/// created at an allocation site, a global. A value that reaches somewhere
/// the pass cannot follow escapes: a function or class used as a value gets
/// `DYN` parameters, and a list that escapes gets `DYN` elements.
final class Infer {
    /// A function the pass types: a compiled method, or a plain function at
    /// the top of the module with a stable name.
    static final class Fn {
        final FunctionDef def;
        /// The class, for a method.
        final Layouts.Cls cls;
        final List<String> names;
        final Ty[] params;
        final Map<String, Ty> locals = new LinkedHashMap<String, Ty>();
        Ty ret = Ty.BOT;
        boolean escapes;

        Fn(FunctionDef def, Layouts.Cls cls) {
            this.def = def;
            this.cls = cls;
            this.names = def.args.names();
            this.params = new Ty[names.size()];
            for (int i = 0; i < params.length; i++) {
                params[i] = Ty.BOT;
            }
        }

        Ty param(String n) {
            int i = names.indexOf(n);
            return i < 0 ? null : params[i];
        }
    }

    final Layouts layouts;
    final Scope moduleScope;
    final Map<FunctionDef, Fn> fns = new LinkedHashMap<FunctionDef, Fn>();
    /// Stable global name -> the function or class it is bound to, once.
    final Map<String, Fn> globalFns = new HashMap<String, Fn>();
    final Map<String, Layouts.Cls> globalClasses = new HashMap<String, Layouts.Cls>();
    /// The type a call of an expression function (see `exprBody`) has at
    /// that call site, from the argument types there; where it is narrower
    /// than the function's own return type, typed code relabels the result.
    final Map<Call, Ty> siteRet = new HashMap<Call, Ty>();
    /// Globals bound once, by `import math`: calls of its functions on
    /// numbers are typed (see `MATH_FNS`) and run as `PyMath` directly.
    final Set<String> mathNames = new HashSet<String>();
    /// `math` functions typed code calls, with their arity.
    static final Map<String, Integer> MATH_FNS = new HashMap<String, Integer>();
    /// `math` constants, all floats.
    static final Set<String> MATH_CONSTS = new HashSet<String>();

    static {
        String[] unary = {"sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "exp", "fabs", "log", "log10"};
        for (String f : unary) {
            MATH_FNS.put(f, Integer.valueOf(1));
        }
        MATH_FNS.put("fmod", Integer.valueOf(2));
        MATH_FNS.put("atan2", Integer.valueOf(2));
        for (String c : new String[] {"pi", "e", "tau", "inf", "nan"}) {
            MATH_CONSTS.add(c);
        }
    }
    final Map<String, Ty> globals = new HashMap<String, Ty>();
    /// Field name -> type, per declaring class.
    final Map<Layouts.Cls, Map<String, Ty>> fields = new LinkedHashMap<Layouts.Cls, Map<String, Ty>>();
    /// Fields every instance has set once its `__init__` returned.
    final Map<Layouts.Cls, Set<String>> definite = new HashMap<Layouts.Cls, Set<String>>();
    /// Classes something the pass cannot see may subclass or patch.
    final Set<Layouts.Cls> open = new HashSet<Layouts.Cls>();
    final Map<Expr, Integer> siteOf = new IdentityHashMap<Expr, Integer>();
    final List<Ty> siteElem = new ArrayList<Ty>();
    final Set<Integer> escapedSites = new HashSet<Integer>();
    boolean allListsEscape;

    private boolean changed;
    private final Map<String, Integer> moduleBindings = new HashMap<String, Integer>();

    private Infer(Layouts layouts, Scope moduleScope) {
        this.layouts = layouts;
        this.moduleScope = moduleScope;
    }

    /// Infers the module, or returns null when it gives up (no fixpoint).
    static Infer run(List<Stmt> body, Layouts layouts, Scope moduleScope) {
        Infer in = new Infer(layouts, moduleScope);
        in.collect(body);
        for (int round = 0; round < 60; round++) {
            in.changed = false;
            in.walkBlock(body, null, moduleScope);
            in.unify();
            if (!in.changed) {
                return in;
            }
        }
        return null;
    }

    // ---- setup -----------------------------------------------------------

    private void collect(List<Stmt> body) {
        countBindings(body);
        for (Layouts.Cls k : layouts.classes.values()) {
            fields.put(k, new LinkedHashMap<String, Ty>());
            for (String f : k.fields) {
                fields.get(k).put(f, Ty.BOT);
            }
            for (FunctionDef f : k.typable.values()) {
                Fn fn = new Fn(f, k);
                fn.params[0] = Ty.inst(k, false);
                fns.put(f, fn);
            }
        }
        for (Stmt s : body) {
            if (s instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) s;
                if (stable(f.name) && f.decorators.isEmpty() && !f.isAsync && !f.scope.hasYield
                        && f.args.vararg == null && f.args.kwarg == null && f.args.kwonly.isEmpty()
                        && f.scope.cells.isEmpty() && f.args.names().size() <= 8) {
                    Fn fn = new Fn(f, null);
                    fns.put(f, fn);
                    globalFns.put(f.name, fn);
                }
            } else if (s instanceof ClassDef) {
                Layouts.Cls k = layouts.classes.get(s);
                if (k != null && stable(((ClassDef) s).name)) {
                    globalClasses.put(((ClassDef) s).name, k);
                }
            } else if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    if ("math".equals(a.name) && stable(n)) {
                        mathNames.add(n);
                    }
                }
            }
        }
        markOpen(body);
        for (Layouts.Cls k : layouts.classes.values()) {
            definite.put(k, initDefinite(k, new HashSet<Layouts.Cls>()));
        }
    }

    /// Whether `e` is the `math` module, through a global only `import math`
    /// binds.
    boolean isMath(Expr e, Scope sc) {
        return e instanceof Name && mathNames.contains(((Name) e).id)
                && sc.resolve(((Name) e).id) == Scope.Binding.GLOBAL;
    }

    /// Whether `c` is a `math` function typed code runs, on the right number
    /// of plain positional arguments.
    boolean isMathCall(Call c, Scope sc) {
        if (!(c.func instanceof Attribute) || !c.keywords.isEmpty() || anyStarred(c.args)) {
            return false;
        }
        Attribute a = (Attribute) c.func;
        Integer n = MATH_FNS.get(a.attr);
        return n != null && n.intValue() == c.args.size() && isMath(a.value, sc);
    }

    /// The return type of expression function `f` called with `args`, or
    /// null. Helpers such as `def i32(v): return ((v + B) & M) - B` are
    /// shared by every caller, and one caller passing an untyped value
    /// makes the parameter, and so the result, untyped for all of them. The
    /// body is plain arithmetic on its parameters and global constants, so
    /// its type at one call follows from that call's arguments alone.
    private Ty siteType(Fn f, List<Ty> args) {
        Expr body = exprBody(f.def);
        if (body == null) {
            return null;
        }
        for (Ty t : args) {
            if (t.opt || t.k != Ty.K.INT && t.k != Ty.K.FLOAT && t.k != Ty.K.BOOL) {
                return null;
            }
        }
        Fn site = new Fn(f.def, null);
        for (int i = 0; i < args.size(); i++) {
            site.params[i] = args.get(i);
            site.locals.put(site.names.get(i), args.get(i));
        }
        Ty t = type(body, site, f.def.scope);
        return !t.opt && (t.k == Ty.K.INT || t.k == Ty.K.FLOAT || t.k == Ty.K.BOOL) ? t : null;
    }

    /// The returned expression of a function whose body is only `return
    /// <arithmetic>` (after an optional docstring), the arithmetic made of
    /// operators, constants, its parameters and global names; else null.
    static Expr exprBody(FunctionDef d) {
        List<Stmt> b = d.body;
        int i = 0;
        if (b.size() == 2 && b.get(0) instanceof ExprStmt && ((ExprStmt) b.get(0)).value instanceof Constant) {
            i = 1;
        }
        if (b.size() != i + 1 || !(b.get(i) instanceof Return) || ((Return) b.get(i)).value == null) {
            return null;
        }
        Expr e = ((Return) b.get(i)).value;
        return arithmetic(e, d.scope) ? e : null;
    }

    private static boolean arithmetic(Expr e, Scope sc) {
        if (e instanceof Constant) {
            return true;
        }
        if (e instanceof Name) {
            Scope.Binding r = sc.resolve(((Name) e).id);
            return r == Scope.Binding.GLOBAL || sc.params.contains(((Name) e).id);
        }
        if (e instanceof BinOp) {
            return arithmetic(((BinOp) e).left, sc) && arithmetic(((BinOp) e).right, sc);
        }
        if (e instanceof UnaryOp) {
            return arithmetic(((UnaryOp) e).operand, sc);
        }
        return false;
    }

    private boolean stable(String n) {
        Integer c = moduleBindings.get(n);
        return c != null && c.intValue() == 1;
    }

    /// Counts the bindings of each global at the top level, and every
    /// `global` declaration in a function as one more (such a store is
    /// invisible to a module-level count).
    private void countBindings(List<Stmt> body) {
        for (Stmt s : body) {
            List<String> names = new ArrayList<String>();
            if (s instanceof FunctionDef) {
                names.add(((FunctionDef) s).name);
            } else if (s instanceof ClassDef) {
                names.add(((ClassDef) s).name);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    targetNames(t, names);
                }
            } else if (s instanceof AugAssign) {
                targetNames(((AugAssign) s).target, names);
                targetNames(((AugAssign) s).target, names);
            } else if (s instanceof AnnAssign) {
                targetNames(((AnnAssign) s).target, names);
            } else if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    int dot = n.indexOf('.');
                    names.add(dot < 0 ? n : n.substring(0, dot));
                }
            } else if (s instanceof ImportFrom) {
                for (Alias a : ((ImportFrom) s).names) {
                    names.add(a.asname != null ? a.asname : a.name);
                }
            } else {
                // A compound statement: everything it binds counts twice,
                // since it may run any number of times.
                for (String n : moduleScope.assigned) {
                    if (binds(s, n)) {
                        names.add(n);
                        names.add(n);
                    }
                }
            }
            for (String n : names) {
                Integer c = moduleBindings.get(n);
                moduleBindings.put(n, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            }
        }
        globalDecls(body);
    }

    private void globalDecls(Object o) {
        if (o instanceof Global && !((Global) o).nonlocal) {
            for (String n : ((Global) o).names) {
                moduleBindings.put(n, Integer.valueOf(99));
            }
        }
        children(o, x -> globalDecls(x));
    }

    private static boolean binds(Stmt s, String n) {
        List<String> out = new ArrayList<String>();
        boundIn(s, out);
        return out.contains(n);
    }

    /// The names a statement stores into, outside nested scopes.
    private static void boundIn(Object o, List<String> out) {
        if (o instanceof Lambda || o instanceof Comp) {
            return;
        }
        if (o instanceof FunctionDef) {
            out.add(((FunctionDef) o).name);
            return;
        }
        if (o instanceof ClassDef) {
            out.add(((ClassDef) o).name);
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                targetNames(t, out);
            }
        } else if (o instanceof AugAssign) {
            targetNames(((AugAssign) o).target, out);
        } else if (o instanceof AnnAssign) {
            targetNames(((AnnAssign) o).target, out);
        } else if (o instanceof For) {
            targetNames(((For) o).target, out);
        } else if (o instanceof WithItem && ((WithItem) o).vars != null) {
            targetNames(((WithItem) o).vars, out);
        } else if (o instanceof NamedExpr) {
            out.add(((NamedExpr) o).target.id);
        } else if (o instanceof ExceptHandler && ((ExceptHandler) o).name != null) {
            out.add(((ExceptHandler) o).name);
        } else if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                String n = a.asname != null ? a.asname : a.name;
                int dot = n.indexOf('.');
                out.add(dot < 0 ? n : n.substring(0, dot));
            }
        } else if (o instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) o).names) {
                out.add(a.asname != null ? a.asname : a.name);
            }
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                targetNames(t, out);
            }
        }
        children(o, x -> boundIn(x, out));
    }

    private static void targetNames(Expr t, List<String> out) {
        if (t instanceof Name) {
            out.add(((Name) t).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                targetNames(e, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                targetNames(e, out);
            }
        } else if (t instanceof Starred) {
            targetNames(((Starred) t).value, out);
        }
    }

    /// A class some class the layout planner rejected derives from: its
    /// instances may run methods this pass does not see, so neither it nor
    /// its ancestors are typed.
    private void markOpen(Object o) {
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            if (!layouts.classes.containsKey(c)) {
                for (Expr b : c.bases) {
                    if (b instanceof Name) {
                        Layouts.Cls k = byName(((Name) b).id);
                        for (; k != null; k = k.parent) {
                            open.add(k);
                        }
                    }
                }
            }
        }
        children(o, x -> markOpen(x));
    }

    private Layouts.Cls byName(String n) {
        for (Layouts.Cls k : layouts.classes.values()) {
            if (k.def.name.equals(n)) {
                return k;
            }
        }
        return null;
    }

    // ---- definite assignment of fields ------------------------------------

    private Set<String> initDefinite(Layouts.Cls k, Set<Layouts.Cls> seen) {
        Set<String> r = new HashSet<String>();
        Layouts.Cls owner = Layouts.resolver(k, "__init__");
        if (owner == null || !seen.add(owner)) {
            return r;
        }
        FunctionDef f = owner.typable.get("__init__");
        if (f == null || f.args.names().isEmpty()) {
            return r;
        }
        String self = f.args.names().get(0);
        if (f.scope.assigned.contains(self)) {
            return r;
        }
        for (Stmt s : f.body) {
            if (s instanceof Pass || s instanceof ExprStmt && ((ExprStmt) s).value instanceof Constant) {
                continue;
            }
            if (s instanceof Assign && ((Assign) s).targets.size() == 1) {
                Assign a = (Assign) s;
                Expr t = a.targets.get(0);
                if (t instanceof Attribute && ((Attribute) t).value instanceof Name
                        && ((Name) ((Attribute) t).value).id.equals(self) && !mentions(a.value, self)) {
                    r.add(((Attribute) t).attr);
                    continue;
                }
            }
            if (s instanceof ExprStmt && ((ExprStmt) s).value instanceof Call) {
                Call c = (Call) ((ExprStmt) s).value;
                if (c.func instanceof Attribute && "__init__".equals(((Attribute) c.func).attr)
                        && ((Attribute) c.func).value instanceof Name && c.keywords.isEmpty() && !c.args.isEmpty()
                        && c.args.get(0) instanceof Name && ((Name) c.args.get(0)).id.equals(self)) {
                    Layouts.Cls base = globalClasses.get(((Name) ((Attribute) c.func).value).id);
                    boolean clean = base != null && isAncestor(base, owner);
                    for (int i = 1; i < c.args.size() && clean; i++) {
                        clean = !mentions(c.args.get(i), self);
                    }
                    if (clean) {
                        r.addAll(initDefinite(base, seen));
                        continue;
                    }
                }
            }
            if (mentions(s, self)) {
                break;
            }
        }
        return r;
    }

    private static boolean isAncestor(Layouts.Cls a, Layouts.Cls k) {
        for (Layouts.Cls x = k.parent; x != null; x = x.parent) {
            if (x == a) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    static boolean mentions(Object o, String n) {
        boolean[] r = new boolean[1];
        mentionsIn(o, n, r);
        return r[0];
    }

    private static void mentionsIn(Object o, String n, boolean[] r) {
        if (r[0]) {
            return;
        }
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            r[0] = true;
            return;
        }
        children(o, x -> mentionsIn(x, n, r));
    }

    /// Whether field `f` of class `k` (declared there or inherited) is set
    /// on every instance of `k` and of all its subclasses.
    boolean definitelySet(Layouts.Cls owner, String f) {
        for (Layouts.Cls k : layouts.classes.values()) {
            if (isSelfOrAncestor(owner, k) && !definite.get(k).contains(f)) {
                return false;
            }
        }
        return true;
    }

    static boolean isSelfOrAncestor(Layouts.Cls a, Layouts.Cls k) {
        for (Layouts.Cls x = k; x != null; x = x.parent) {
            if (x == a) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    // ---- joins -----------------------------------------------------------

    private Ty joinInto(Ty cur, Ty v) {
        Ty r = Ty.join(cur, v);
        if (!r.equals(cur)) {
            changed = true;
        }
        if (r.k != Ty.K.LIST) {
            escape(v);
        }
        return r;
    }

    /// A value went somewhere the pass does not follow.
    void escape(Ty v) {
        if (v != null && v.k == Ty.K.LIST) {
            if (v.sites.length == 0) {
                if (!allListsEscape) {
                    allListsEscape = true;
                    changed = true;
                }
            } else {
                for (int s : v.sites) {
                    if (escapedSites.add(Integer.valueOf(s))) {
                        changed = true;
                    }
                }
            }
        }
    }

    private void escapeFn(Fn f) {
        if (f != null && !f.escapes) {
            f.escapes = true;
            changed = true;
        }
        if (f != null) {
            // Callers through a value are unknown, but the typed version
            // is entered from the generic one only through a guard that
            // checks every argument (TypedEmit.entry): a parameter keeps
            // what its direct callers pass, and one nobody calls directly
            // takes the type its uses suggest. A wrong guess costs the
            // guard, never correctness.
            for (int i = f.cls != null ? 1 : 0; i < f.params.length; i++) {
                if (f.params[i].k == Ty.K.BOT) {
                    f.params[i] = joinInto(f.params[i], guess(f.def, f.names.get(i)));
                }
            }
        }
    }

    /// The type the uses of parameter `p` in `def` suggest: INT when it is
    /// a `range` bound, a list repeat count or an operand next to an int
    /// constant; FLOAT next to a float constant; DYN when the uses say
    /// nothing or disagree.
    static Ty guess(FunctionDef def, String p) {
        Ty[] g = {null};
        boolean[] bad = {false};
        for (Stmt s : def.body) {
            guessIn(s, p, g, bad);
        }
        return bad[0] || g[0] == null ? Ty.DYN : g[0];
    }

    private static void guessIn(Object o, String p, Ty[] g, boolean[] bad) {
        if (o instanceof Call && ((Call) o).func instanceof Name && "range".equals(((Name) ((Call) o).func).id)) {
            for (Expr a : ((Call) o).args) {
                if (isName(a, p)) {
                    vote(Ty.INT, g, bad);
                }
            }
        } else if (o instanceof BinOp) {
            BinOp b = (BinOp) o;
            if ("*".equals(b.op) && (b.left instanceof ListExpr && isName(b.right, p))) {
                vote(Ty.INT, g, bad);
            } else if (isName(b.left, p) || isName(b.right, p)) {
                Ty c = constType(isName(b.left, p) ? b.right : b.left);
                if (c != null) {
                    vote(c, g, bad);
                }
            }
        } else if (o instanceof Compare && ((Compare) o).ops.size() == 1) {
            Compare c = (Compare) o;
            Expr r = c.comparators.get(0);
            if (isName(c.left, p) || isName(r, p)) {
                Ty t = constType(isName(c.left, p) ? r : c.left);
                if (t != null) {
                    vote(t, g, bad);
                }
            }
        } else if (o instanceof Assign) {
            // Rebinding the parameter: its later uses say nothing of the
            // argument.
            for (Expr t : ((Assign) o).targets) {
                if (isName(t, p)) {
                    bad[0] = true;
                }
            }
        }
        Infer.children(o, x -> guessIn(x, p, g, bad));
    }

    private static boolean isName(Expr e, String p) {
        return e instanceof Name && ((Name) e).id.equals(p);
    }

    private static Ty constType(Expr e) {
        if (e instanceof Constant) {
            Constant c = (Constant) e;
            if (c.kind == Constant.Kind.INT) {
                return Ty.INT;
            }
            if (c.kind == Constant.Kind.FLOAT && !c.text.matches(".*[jJ]")) {
                return Ty.FLOAT;
            }
        }
        return null;
    }

    private static void vote(Ty t, Ty[] g, boolean[] bad) {
        if (g[0] != null && !g[0].equals(t)) {
            bad[0] = true;
        }
        g[0] = t;
    }

    private void escapeClass(Layouts.Cls k) {
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        if (o != null && o.typable.get("__init__") != null) {
            escapeFn(fns.get(o.typable.get("__init__")));
        }
    }

    /// Every compiled method `name` may be reached with arguments the pass
    /// cannot type.
    private void escapeMethods(String name) {
        for (Fn f : fns.values()) {
            if (f.cls != null && f.def.name.equals(name)) {
                escapeFn(f);
            }
        }
    }

    Ty elem(Ty list) {
        if (list.k != Ty.K.LIST || allListsEscape || list.sites.length == 0) {
            return Ty.DYN;
        }
        Ty r = Ty.BOT;
        for (int s : list.sites) {
            if (escapedSites.contains(Integer.valueOf(s))) {
                return Ty.DYN;
            }
            r = Ty.join(r, siteElem.get(s));
        }
        return r;
    }

    private void storeElem(Ty list, Ty v) {
        if (list.k != Ty.K.LIST || list.sites.length == 0) {
            escape(v);
            return;
        }
        for (int s : list.sites) {
            siteElem.set(s, joinInto(siteElem.get(s), v));
        }
    }

    private int site(Expr e) {
        Integer s = siteOf.get(e);
        if (s == null) {
            s = Integer.valueOf(siteElem.size());
            siteElem.add(Ty.BOT);
            siteOf.put(e, s);
        }
        return s.intValue();
    }

    private void unify() {
        // Methods of one name and arity are called through one virtual
        // signature (TypedEmit), so they share their parameter types.
        Map<String, List<Fn>> groups = new LinkedHashMap<String, List<Fn>>();
        for (Fn f : fns.values()) {
            if (f.cls != null && !f.def.name.equals("__init__")) {
                String key = f.def.name + "/" + f.params.length;
                List<Fn> g = groups.get(key);
                if (g == null) {
                    g = new ArrayList<Fn>();
                    groups.put(key, g);
                }
                g.add(f);
            }
        }
        for (List<Fn> g : groups.values()) {
            if (g.size() < 2) {
                continue;
            }
            int n = g.get(0).params.length;
            for (int i = 1; i < n; i++) {
                Ty j = Ty.BOT;
                for (Fn f : g) {
                    j = Ty.join(j, f.params[i]);
                }
                for (Fn f : g) {
                    f.params[i] = joinInto(f.params[i], j);
                }
            }
            Ty r = Ty.BOT;
            for (Fn f : g) {
                r = Ty.join(r, f.ret);
            }
            for (Fn f : g) {
                f.ret = joinInto(f.ret, r);
            }
        }
    }

    // ---- walking ---------------------------------------------------------

    /// `fn` is the compiled function being walked, or null where names are
    /// not tracked (the module body tracks globals instead).
    private void walkBlock(List<Stmt> body, Fn fn, Scope sc) {
        for (Stmt s : body) {
            walk(s, fn, sc);
        }
    }

    private void walk(Stmt s, Fn fn, Scope sc) {
        if (s instanceof ExprStmt) {
            type(((ExprStmt) s).value, fn, sc);
        } else if (s instanceof Assign) {
            Assign a = (Assign) s;
            Ty v = type(a.value, fn, sc);
            for (Expr t : a.targets) {
                store(t, v, a.value, fn, sc);
            }
        } else if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            if (a.value != null) {
                store(a.target, type(a.value, fn, sc), a.value, fn, sc);
            } else if (!(a.target instanceof Name)) {
                type(a.target, fn, sc);
            }
        } else if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            Ty cur = type(a.target, fn, sc);
            Ty v = type(a.value, fn, sc);
            Ty r = binop(a.op, cur, v, a.value);
            if (cur.k == Ty.K.LIST && "+".equals(a.op)) {
                storeElem(cur, elem(v));
                r = cur;
            }
            store(a.target, r, null, fn, sc);
        } else if (s instanceof Return) {
            Return r = (Return) s;
            Ty v = r.value == null ? Ty.NONE : type(r.value, fn, sc);
            if (fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                fn.ret = joinInto(fn.ret, v);
            } else {
                escape(v);
            }
        } else if (s instanceof If) {
            If i = (If) s;
            type(i.test, fn, sc);
            walkBlock(i.body, fn, sc);
            walkBlock(i.orelse, fn, sc);
        } else if (s instanceof While) {
            While w = (While) s;
            type(w.test, fn, sc);
            walkBlock(w.body, fn, sc);
            walkBlock(w.orelse, fn, sc);
        } else if (s instanceof For) {
            For f = (For) s;
            Ty it = iterElem(f.iter, fn, sc);
            store(f.target, it, null, fn, sc);
            walkBlock(f.body, fn, sc);
            walkBlock(f.orelse, fn, sc);
        } else if (s instanceof FunctionDef) {
            FunctionDef d = (FunctionDef) s;
            for (Expr e : d.decorators) {
                type(e, fn, sc);
            }
            for (Expr e : d.args.defaults) {
                type(e, fn, sc);
            }
            for (Expr e : d.args.kwDefaults) {
                if (e != null) {
                    type(e, fn, sc);
                }
            }
            Fn inner = fns.get(d);
            if (inner != null) {
                // Defaults fill parameters the call leaves out.
                int first = inner.params.length - d.args.defaults.size();
                for (int i = 0; i < d.args.defaults.size(); i++) {
                    inner.params[first + i] = joinInto(inner.params[first + i], type(d.args.defaults.get(i), fn, sc));
                }
                if (!d.decorators.isEmpty() || inner.cls == null && !globalFns.containsValue(inner)) {
                    escapeFn(inner);
                }
                for (int i = 0; i < inner.params.length; i++) {
                    inner.locals.put(inner.names.get(i), Ty.join(inner.locals.get(inner.names.get(i)), inner.params[i]));
                }
                walkBlock(d.body, inner, d.scope);
                if (!endsAbruptly(d.body)) {
                    inner.ret = joinInto(inner.ret, Ty.NONE);
                }
                if (inner.def.name.startsWith("__") && !"__init__".equals(inner.def.name)) {
                    // The runtime calls these itself, with anything.
                    escapeFn(inner);
                    inner.ret = joinInto(inner.ret, Ty.DYN);
                }
            } else {
                walkBlock(d.body, null, d.scope);
            }
            if (sc.kind == Scope.Kind.MODULE && fns.get(d) == null) {
                storeName(d.name, Ty.DYN, fn, sc);
            }
        } else if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            for (Expr b : c.bases) {
                if (!(b instanceof Name && globalClasses.containsKey(((Name) b).id))) {
                    type(b, fn, sc);
                }
            }
            for (Expr d : c.decorators) {
                type(d, fn, sc);
            }
            walkBlock(c.body, null, c.scope);
        } else if (s instanceof Try) {
            Try t = (Try) s;
            walkBlock(t.body, fn, sc);
            for (ExceptHandler h : t.handlers) {
                if (h.type != null) {
                    type(h.type, fn, sc);
                }
                if (h.name != null) {
                    storeName(h.name, Ty.DYN, fn, sc);
                }
                walkBlock(h.body, fn, sc);
            }
            walkBlock(t.orelse, fn, sc);
            walkBlock(t.finalbody, fn, sc);
        } else if (s instanceof With) {
            With w = (With) s;
            for (WithItem i : w.items) {
                type(i.context, fn, sc);
                if (i.vars != null) {
                    store(i.vars, Ty.DYN, null, fn, sc);
                }
            }
            walkBlock(w.body, fn, sc);
        } else if (s instanceof Import || s instanceof ImportFrom) {
            List<String> names = new ArrayList<String>();
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    int dot = n.indexOf('.');
                    names.add(dot < 0 ? n : n.substring(0, dot));
                }
            } else {
                for (Alias a : ((ImportFrom) s).names) {
                    names.add(a.asname != null ? a.asname : a.name);
                }
            }
            for (String n : names) {
                storeName(n, Ty.DYN, fn, sc);
            }
        } else {
            // Raise, Assert, Delete, Match, Global, Pass, Break, Continue:
            // whatever they evaluate escapes.
            anyChildren(s, fn, sc);
        }
    }

    private static boolean endsAbruptly(List<Stmt> body) {
        if (body.isEmpty()) {
            return false;
        }
        Stmt s = body.get(body.size() - 1);
        if (s instanceof Return || s instanceof Raise) {
            return true;
        }
        if (s instanceof If) {
            If i = (If) s;
            return endsAbruptly(i.body) && endsAbruptly(i.orelse);
        }
        if (s instanceof While) {
            While w = (While) s;
            return w.test instanceof Constant && ((Constant) w.test).kind == Constant.Kind.TRUE && !hasBreak(w.body);
        }
        return false;
    }

    static boolean hasBreak(List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof Break) {
                return true;
            }
            if (s instanceof If && (hasBreak(((If) s).body) || hasBreak(((If) s).orelse))) {
                return true;
            }
            if (s instanceof Try) {
                Try t = (Try) s;
                if (hasBreak(t.body) || hasBreak(t.orelse) || hasBreak(t.finalbody)) {
                    return true;
                }
                for (ExceptHandler h : t.handlers) {
                    if (hasBreak(h.body)) {
                        return true;
                    }
                }
            }
            if (s instanceof With && hasBreak(((With) s).body)) {
                return true;
            }
        }
        return false;
    }

    /// The type of the items `for x in it` binds.
    Ty iterElem(Expr it, Fn fn, Scope sc) {
        if (rangeCall(it, sc)) {
            for (Expr a : ((Call) it).args) {
                type(a, fn, sc);
            }
            return Ty.INT;
        }
        Ty t = type(it, fn, sc);
        if (t.k == Ty.K.LIST) {
            return elem(t);
        }
        escape(t);
        return Ty.DYN;
    }

    boolean rangeCall(Expr e, Scope sc) {
        if (!(e instanceof Call)) {
            return false;
        }
        Call c = (Call) e;
        return c.func instanceof Name && "range".equals(((Name) c.func).id) && c.keywords.isEmpty()
                && c.args.size() >= 1 && c.args.size() <= 3 && sc.resolve("range") == Scope.Binding.BUILTIN
                && !moduleScope.hasStarImport && !anyStarred(c.args);
    }

    private static boolean anyStarred(List<Expr> l) {
        for (Expr e : l) {
            if (e instanceof Starred) {
                return true;
            }
        }
        return false;
    }

    private void store(Expr t, Ty v, Expr value, Fn fn, Scope sc) {
        if (t instanceof Name) {
            storeName(((Name) t).id, v, fn, sc);
        } else if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            Ty r = type(a.value, fn, sc);
            storeField(r, a.attr, v);
        } else if (t instanceof Subscript) {
            Subscript s = (Subscript) t;
            Ty r = type(s.value, fn, sc);
            if (s.index instanceof Slice) {
                anyChildren(s.index, fn, sc);
                if (r.k == Ty.K.LIST) {
                    storeElem(r, elem(v));
                } else {
                    escape(v);
                }
            } else {
                type(s.index, fn, sc);
                if (r.k == Ty.K.LIST) {
                    storeElem(r, v);
                } else if (r.k != Ty.K.BOT) {
                    escape(v);
                }
            }
        } else if (t instanceof TupleExpr || t instanceof ListExpr) {
            List<Expr> elts = t instanceof TupleExpr ? ((TupleExpr) t).elts : ((ListExpr) t).elts;
            List<Expr> src = value instanceof TupleExpr ? ((TupleExpr) value).elts
                    : value instanceof ListExpr ? ((ListExpr) value).elts : null;
            boolean starred = anyStarred(elts);
            if (src != null && src.size() == elts.size() && !starred && !anyStarred(src)) {
                // `a, b = x, y` pairs each target with its own value.
                for (int i = 0; i < elts.size(); i++) {
                    store(elts.get(i), type(src.get(i), fn, sc), null, fn, sc);
                }
                return;
            }
            Ty each = Ty.DYN;
            if (value != null && rangeCall(value, sc)) {
                each = Ty.INT;
            } else if (v.k == Ty.K.LIST) {
                each = elem(v);
            } else {
                escape(v);
            }
            for (Expr e : elts) {
                store(e instanceof Starred ? ((Starred) e).value : e, e instanceof Starred ? Ty.DYN : each, null, fn,
                        sc);
            }
        } else if (t instanceof Starred) {
            store(((Starred) t).value, Ty.DYN, null, fn, sc);
        } else {
            anyChildren(t, fn, sc);
            escape(v);
        }
    }

    private void storeName(String n, Ty v, Fn fn, Scope sc) {
        switch (sc.resolve(n)) {
            case LOCAL:
                if (fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                    fn.locals.put(n, joinInto(fn.locals.get(n), v));
                } else {
                    escape(v);
                }
                return;
            case GLOBAL:
                globals.put(n, joinInto(globals.get(n), v));
                return;
            default:
                escape(v);
        }
    }

    void storeField(Ty recv, String attr, Ty v) {
        if (recv.k == Ty.K.BOT || recv.k == Ty.K.NONE) {
            return;
        }
        List<Layouts.Cls> owners = layouts.fieldOwners.get(attr);
        if (owners == null) {
            escape(v);
            return;
        }
        boolean any = false;
        for (Layouts.Cls o : owners) {
            boolean reach = recv.k != Ty.K.INST || isSelfOrAncestor(o, recv.cls) || isSelfOrAncestor(recv.cls, o);
            if (reach) {
                Map<String, Ty> m = fields.get(o);
                m.put(attr, joinInto(m.get(attr), v));
                any = true;
            }
        }
        if (!any) {
            escape(v);
        }
    }

    /// The declaring class of field `attr` for an instance of `k`.
    static Layouts.Cls fieldOwner(Layouts.Cls k, String attr) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (c.fields.contains(attr)) {
                return c;
            }
        }
        return null;
    }

    Ty fieldType(Layouts.Cls owner, String attr) {
        Ty t = fields.get(owner).get(attr);
        return t == null ? Ty.DYN : t;
    }

    /// Compiled methods a call `recv.m(...)` on an instance of `k` may run:
    /// the one `k` resolves, and every override in a subclass of `k`. Null
    /// when the name may resolve to anything else (a field, a class
    /// attribute that is not a compiled method).
    List<FunctionDef> candidates(Layouts.Cls k, String m) {
        if (layouts.fieldOwners.containsKey(m)) {
            return null;
        }
        List<FunctionDef> r = new ArrayList<FunctionDef>();
        Layouts.Cls res = Layouts.resolver(k, m);
        if (res != null) {
            FunctionDef f = res.typable.get(m);
            if (f == null) {
                return null;
            }
            r.add(f);
        }
        for (Layouts.Cls c : layouts.classes.values()) {
            if (c != k && isSelfOrAncestor(k, c) && c.bodyNames.contains(m)) { //NOPMD CompareObjectsWithEquals - identity is meant
                FunctionDef f = c.typable.get(m);
                if (f == null) {
                    return null;
                }
                if (!r.contains(f)) {
                    r.add(f);
                }
            }
        }
        return r.isEmpty() ? null : r;
    }

    private void anyChildren(Object o, Fn fn, Scope sc) {
        children(o, x -> {
            if (x instanceof Expr) {
                escape(type((Expr) x, fn, sc));
            } else if (x instanceof Stmt) {
                walk((Stmt) x, fn, sc);
            } else {
                anyChildren(x, fn, sc);
            }
        });
    }

    // ---- expressions -----------------------------------------------------

    Ty nameType(String n, Fn fn, Scope sc) {
        switch (sc.resolve(n)) {
            case LOCAL:
                if (fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                    Ty t = fn.locals.get(n);
                    return t == null ? Ty.BOT : t;
                }
                return Ty.DYN;
            case GLOBAL: {
                Fn f = globalFns.get(n);
                if (f != null) {
                    escapeFn(f);
                    return Ty.DYN;
                }
                Layouts.Cls k = globalClasses.get(n);
                if (k != null) {
                    escapeClass(k);
                    return Ty.DYN;
                }
                Ty t = globals.get(n);
                return t == null ? Ty.BOT : t;
            }
            default:
                return Ty.DYN;
        }
    }

    Ty type(Expr e, Fn fn, Scope sc) {
        if (e instanceof Constant) {
            switch (((Constant) e).kind) {
                case NONE:
                    return Ty.NONE;
                case TRUE:
                case FALSE:
                    return Ty.BOOL;
                case INT:
                    return Ty.INT;
                case FLOAT:
                    return ((Constant) e).text.endsWith("j") || ((Constant) e).text.endsWith("J") ? Ty.DYN
                            : Ty.FLOAT;
                case STR:
                    return Ty.STR;
                default:
                    return Ty.DYN;
            }
        }
        if (e instanceof Name) {
            return nameType(((Name) e).id, fn, sc);
        }
        if (e instanceof Attribute) {
            Attribute a = (Attribute) e;
            if (MATH_CONSTS.contains(a.attr) && isMath(a.value, sc)) {
                return Ty.FLOAT;
            }
            if (a.value instanceof Name && globalClasses.containsKey(((Name) a.value).id)
                    && sc.resolve(((Name) a.value).id) == Scope.Binding.GLOBAL) {
                // `Cls.attr` as a value: a method taken off the class runs
                // with arguments nobody tracks.
                escapeMethods(a.attr);
                return Ty.DYN;
            }
            Ty r = type(a.value, fn, sc);
            return attrType(r, a.attr);
        }
        if (e instanceof Call) {
            return call((Call) e, fn, sc);
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            Ty l = type(b.left, fn, sc);
            Ty r = type(b.right, fn, sc);
            if ("*".equals(b.op) && b.left instanceof ListExpr && l.k == Ty.K.LIST) {
                return Ty.list(l.sites, false);
            }
            if ("+".equals(b.op) && l.k == Ty.K.LIST && r.k == Ty.K.LIST) {
                int s = site(b);
                siteElem.set(s, joinInto(siteElem.get(s), Ty.join(elem(l), elem(r))));
                return Ty.list(new int[] {s}, false);
            }
            if (l.k == Ty.K.BOT || r.k == Ty.K.BOT) {
                return Ty.BOT;
            }
            Ty t = binop(b.op, l, r, b.right);
            if (t.k == Ty.K.DYN) {
                escape(l);
                escape(r);
            }
            return t;
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            Ty t = type(u.operand, fn, sc);
            if ("not".equals(u.op)) {
                return Ty.BOOL;
            }
            if (t.k == Ty.K.BOT) {
                return Ty.BOT;
            }
            if (t.k == Ty.K.FLOAT && !t.opt) {
                return Ty.FLOAT;
            }
            if ((t.k == Ty.K.INT || t.k == Ty.K.BOOL) && !"+".equals(u.op)) {
                return Ty.INT;
            }
            if (t.k == Ty.K.INT && "+".equals(u.op)) {
                return Ty.INT;
            }
            escape(t);
            return Ty.DYN;
        }
        if (e instanceof BoolOp) {
            Ty r = Ty.BOT;
            for (Expr v : ((BoolOp) e).values) {
                r = Ty.join(r, type(v, fn, sc));
            }
            return r;
        }
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            Ty l = type(c.left, fn, sc);
            boolean plain = true;
            for (int i = 0; i < c.ops.size(); i++) {
                Ty r = type(c.comparators.get(i), fn, sc);
                String op = c.ops.get(i);
                if (!("is".equals(op) || "is not".equals(op) || "in".equals(op) || "not in".equals(op)
                        || plainCompare(l) && plainCompare(r))) {
                    plain = false;
                }
                if ("in".equals(op) || "not in".equals(op)) {
                    escape(r);
                    escape(l);
                }
                l = r;
            }
            return plain ? Ty.BOOL : Ty.DYN;
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            type(i.test, fn, sc);
            return Ty.join(type(i.body, fn, sc), type(i.orelse, fn, sc));
        }
        if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            Ty r = type(s.value, fn, sc);
            if (s.index instanceof Slice) {
                anyChildren(s.index, fn, sc);
                if (r.k == Ty.K.LIST && !r.opt) {
                    return Ty.list(r.sites, false);
                }
                escape(r);
                return Ty.DYN;
            }
            Ty i = type(s.index, fn, sc);
            if (r.k == Ty.K.BOT || r.k == Ty.K.NONE || i.k == Ty.K.BOT) {
                return Ty.BOT;
            }
            if (r.k == Ty.K.LIST && (i.k == Ty.K.INT || i.k == Ty.K.BOOL)) {
                return elem(r);
            }
            escape(r);
            escape(i);
            return Ty.DYN;
        }
        if (e instanceof ListExpr) {
            ListExpr l = (ListExpr) e;
            int s = site(l);
            for (Expr x : l.elts) {
                if (x instanceof Starred) {
                    Ty t = type(((Starred) x).value, fn, sc);
                    siteElem.set(s, joinInto(siteElem.get(s), t.k == Ty.K.LIST ? elem(t) : Ty.DYN));
                    escape(t);
                } else {
                    siteElem.set(s, joinInto(siteElem.get(s), type(x, fn, sc)));
                }
            }
            return Ty.list(new int[] {s}, false);
        }
        if (e instanceof NamedExpr) {
            NamedExpr n = (NamedExpr) e;
            Ty v = type(n.value, fn, sc);
            storeName(n.target.id, v, fn, sc);
            return v;
        }
        if (e instanceof Lambda) {
            Lambda l = (Lambda) e;
            for (Expr d : l.args.defaults) {
                escape(type(d, fn, sc));
            }
            escape(type(l.body, null, l.scope));
            return Ty.DYN;
        }
        if (e instanceof Comp) {
            Comp c = (Comp) e;
            boolean first = true;
            for (Comprehension g : c.generators) {
                // The first iterable is evaluated in the enclosing scope.
                escape(type(g.iter, first ? fn : null, first ? sc : c.scope));
                first = false;
                store(g.target, Ty.DYN, null, null, c.scope);
                for (Expr x : g.ifs) {
                    type(x, null, c.scope);
                }
            }
            if (c.elt != null) {
                escape(type(c.elt, null, c.scope));
            }
            if (c.value != null) {
                escape(type(c.value, null, c.scope));
            }
            return Ty.DYN;
        }
        anyChildren(e, fn, sc);
        return Ty.DYN;
    }

    /// Kinds whose comparisons always answer a bool: no user `__eq__`.
    private boolean plainCompare(Ty t) {
        switch (t.k) {
            case BOT:
            case NONE:
            case BOOL:
            case INT:
            case FLOAT:
            case STR:
                return true;
            case INST:
                for (Layouts.Cls c = t.cls; c != null; c = c.parent) {
                    for (String d : new String[] {"__eq__", "__ne__", "__lt__", "__le__", "__gt__", "__ge__"}) {
                        if (c.bodyNames.contains(d)) {
                            return false;
                        }
                    }
                }
                return !anySubclassDefinesCompare(t.cls);
            default:
                return false;
        }
    }

    private boolean anySubclassDefinesCompare(Layouts.Cls k) {
        for (Layouts.Cls c : layouts.classes.values()) {
            if (isSelfOrAncestor(k, c)) {
                for (String d : new String[] {"__eq__", "__ne__", "__lt__", "__le__", "__gt__", "__ge__"}) {
                    if (c.bodyNames.contains(d)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    Ty attrType(Ty r, String attr) {
        if (r.k == Ty.K.BOT || r.k == Ty.K.NONE) {
            // Nothing reached the receiver yet, or None (which raises): no value.
            // Nothing reached the receiver yet: a later round decides.
            return Ty.BOT;
        }
        if (r.k == Ty.K.INST) {
            Layouts.Cls o = fieldOwner(r.cls, attr);
            if (o != null) {
                return fieldType(o, attr);
            }
            // A bound method, or something only a subclass has.
            escapeMethods(attr);
            return Ty.DYN;
        }
        escape(r);
        if (r.k == Ty.K.DYN) {
            escapeMethods(attr);
        }
        return Ty.DYN;
    }

    static Ty binop(String op, Ty l, Ty r, Expr right) {
        if (l.k == Ty.K.BOT || r.k == Ty.K.BOT) {
            return Ty.BOT;
        }
        if (l.opt || r.opt) {
            return Ty.DYN;
        }
        boolean li = l.k == Ty.K.INT || l.k == Ty.K.BOOL;
        boolean ri = r.k == Ty.K.INT || r.k == Ty.K.BOOL;
        boolean lf = l.k == Ty.K.FLOAT;
        boolean rf = r.k == Ty.K.FLOAT;
        if (li && ri) {
            switch (op) {
                case "+":
                case "-":
                case "*":
                case "//":
                case "%":
                case "&":
                case "|":
                case "^":
                case "<<":
                case ">>":
                    return Ty.INT;
                case "/":
                    return Ty.FLOAT;
                case "**":
                    // A negative exponent answers a float.
                    return right instanceof Constant && ((Constant) right).kind == Constant.Kind.INT ? Ty.INT
                            : Ty.DYN;
                default:
                    return Ty.DYN;
            }
        }
        if ((lf || li) && (rf || ri)) {
            switch (op) {
                case "+":
                case "-":
                case "*":
                case "/":
                case "//":
                case "%":
                    return Ty.FLOAT;
                default:
                    // `**` of a negative float answers a complex.
                    return Ty.DYN;
            }
        }
        if (l.k == Ty.K.STR && r.k == Ty.K.STR && "+".equals(op)) {
            return Ty.STR;
        }
        return Ty.DYN;
    }

    private Ty call(Call c, Fn fn, Scope sc) {
        boolean simple = c.keywords.isEmpty() && !anyStarred(c.args);
        if (c.func instanceof Name && sc.resolve(((Name) c.func).id) == Scope.Binding.GLOBAL) {
            String n = ((Name) c.func).id;
            Fn f = globalFns.get(n);
            Layouts.Cls k = globalClasses.get(n);
            if (f != null || k != null) {
                List<Ty> args = new ArrayList<Ty>();
                for (Expr a : c.args) {
                    args.add(a instanceof Starred ? Ty.DYN : type(a, fn, sc));
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                if (f != null) {
                    callFn(f, args, 0, simple && args.size() <= f.params.length
                            && args.size() >= f.params.length - f.def.args.defaults.size());
                    Ty site = simple && args.size() == f.params.length ? siteType(f, args) : null;
                    if (site != null) {
                        siteRet.put(c, site);
                        return site;
                    }
                    siteRet.remove(c);
                    return f.ret;
                }
                if (open.contains(k)) {
                    for (Ty a : args) {
                        escape(a);
                    }
                    escapeClass(k);
                    return Ty.DYN;
                }
                Layouts.Cls o = Layouts.resolver(k, "__init__");
                FunctionDef init = o == null ? null : o.typable.get("__init__");
                if (o != null && init == null) {
                    for (Ty a : args) {
                        escape(a);
                    }
                    return Ty.inst(k, false);
                }
                if (init != null) {
                    Fn fi = fns.get(init);
                    fi.params[0] = joinInto(fi.params[0], Ty.inst(k, false));
                    callFn(fi, args, 1, simple && args.size() + 1 <= fi.params.length
                            && args.size() + 1 >= fi.params.length - init.args.defaults.size());
                } else {
                    for (Ty a : args) {
                        escape(a);
                    }
                }
                return Ty.inst(k, false);
            }
        }
        if (isMathCall(c, sc)) {
            boolean numbers = true;
            for (Expr x : c.args) {
                Ty t = type(x, fn, sc);
                numbers &= (t.k == Ty.K.INT || t.k == Ty.K.FLOAT) && !t.opt;
            }
            return numbers ? Ty.FLOAT : Ty.DYN;
        }
        if (c.func instanceof Attribute) {
            Attribute a = (Attribute) c.func;
            if (a.value instanceof Name && globalClasses.containsKey(((Name) a.value).id)
                    && sc.resolve(((Name) a.value).id) == Scope.Binding.GLOBAL) {
                // `Cls.method(obj, ...)`: an explicit call of a method.
                Layouts.Cls k = globalClasses.get(((Name) a.value).id);
                Layouts.Cls o = Layouts.resolver(k, a.attr);
                FunctionDef m = o == null ? null : o.typable.get(a.attr);
                List<Ty> args = new ArrayList<Ty>();
                for (Expr x : c.args) {
                    args.add(x instanceof Starred ? Ty.DYN : type(x, fn, sc));
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                if (m != null && !open.contains(k)) {
                    Fn f = fns.get(m);
                    callFn(f, args, 0, simple && args.size() <= f.params.length
                            && args.size() >= f.params.length - m.args.defaults.size());
                    return f.ret;
                }
                for (Ty t : args) {
                    escape(t);
                }
                if (m != null) {
                    escapeFn(fns.get(m));
                }
                return Ty.DYN;
            }
            Ty r = type(a.value, fn, sc);
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : c.args) {
                args.add(x instanceof Starred ? Ty.DYN : type(x, fn, sc));
            }
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
            if (r.k == Ty.K.BOT || r.k == Ty.K.NONE) {
                return Ty.BOT;
            }
            if (r.k == Ty.K.INST && !open.contains(r.cls)) {
                List<FunctionDef> cand = candidates(r.cls, a.attr);
                if (cand != null) {
                    Ty ret = Ty.BOT;
                    for (FunctionDef m : cand) {
                        Fn f = fns.get(m);
                        callFn(f, args, 1, simple && args.size() + 1 == f.params.length);
                        ret = Ty.join(ret, f.ret);
                    }
                    if (Layouts.resolver(r.cls, a.attr) == null) {
                        // Not every instance has it: the rest raise.
                        ret = Ty.join(ret, Ty.BOT);
                    }
                    return ret;
                }
            }
            if (r.k == Ty.K.LIST && !r.opt) {
                return listMethod(r, a.attr, args);
            }
            escape(r);
            for (Ty t : args) {
                escape(t);
            }
            if (r.k == Ty.K.DYN || r.k == Ty.K.INST) {
                escapeMethods(a.attr);
            }
            return Ty.DYN;
        }
        if (c.func instanceof Name && sc.resolve(((Name) c.func).id) == Scope.Binding.BUILTIN
                && !moduleScope.hasStarImport) {
            String n = ((Name) c.func).id;
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : c.args) {
                args.add(x instanceof Starred ? Ty.DYN : type(x, fn, sc));
            }
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
            if (simple && args.size() == 1 && ("len".equals(n) || "isinstance".equals(n) && false)) {
                // len reads its argument and keeps nothing.
                if (args.get(0).k != Ty.K.LIST) {
                    escape(args.get(0));
                }
                return Ty.INT;
            }
            if (simple && "float".equals(n) && args.size() == 1
                    && (args.get(0).k == Ty.K.INT || args.get(0).k == Ty.K.FLOAT) && !args.get(0).opt) {
                return Ty.FLOAT;
            }
            if (simple && "str".equals(n) && args.size() == 1 && !args.get(0).opt
                    && (args.get(0).k == Ty.K.INT || args.get(0).k == Ty.K.FLOAT
                    || args.get(0).k == Ty.K.STR || args.get(0).k == Ty.K.BOOL)) {
                return Ty.STR;
            }
            if (simple && "ord".equals(n) && args.size() == 1 && args.get(0).k == Ty.K.STR && !args.get(0).opt) {
                return Ty.INT;
            }
            if (simple && "abs".equals(n) && args.size() == 1 && !args.get(0).opt
                    && (args.get(0).k == Ty.K.INT || args.get(0).k == Ty.K.FLOAT)) {
                return args.get(0);
            }
            if (simple && "list".equals(n) && args.size() == 1) {
                int s = site(c);
                Ty t = args.get(0);
                boolean range = rangeCall(c.args.get(0), sc);
                siteElem.set(s, joinInto(siteElem.get(s), t.k == Ty.K.LIST ? elem(t) : range ? Ty.INT : Ty.DYN));
                if (t.k != Ty.K.LIST && !rangeCall(c.args.get(0), sc)) {
                    escape(t);
                }
                return Ty.list(new int[] {s}, false);
            }
            for (Ty t : args) {
                escape(t);
            }
            return Ty.DYN;
        }
        escape(type(c.func, fn, sc));
        for (Expr x : c.args) {
            escape(type(x instanceof Starred ? ((Starred) x).value : x, fn, sc));
        }
        for (Keyword kw : c.keywords) {
            escape(type(kw.value, fn, sc));
        }
        return Ty.DYN;
    }

    private Ty listMethod(Ty r, String m, List<Ty> args) {
        switch (m) {
            case "append":
                if (args.size() == 1) {
                    storeElem(r, args.get(0));
                    return Ty.NONE;
                }
                break;
            case "pop":
                if (args.size() <= 1) {
                    return elem(r);
                }
                break;
            case "extend":
                if (args.size() == 1) {
                    storeElem(r, args.get(0).k == Ty.K.LIST ? elem(args.get(0)) : Ty.DYN);
                    if (args.get(0).k != Ty.K.LIST) {
                        escape(args.get(0));
                    }
                    return Ty.NONE;
                }
                break;
            case "insert":
                if (args.size() == 2) {
                    storeElem(r, args.get(1));
                    return Ty.NONE;
                }
                break;
            case "reverse":
            case "clear":
                return Ty.NONE;
            case "copy":
                return Ty.list(r.sites, false);
            case "index":
            case "count":
                return Ty.INT;
            default:
                break;
        }
        escape(r);
        for (Ty t : args) {
            escape(t);
        }
        return Ty.DYN;
    }

    /// Joins a call's arguments into `f`'s parameters, from `from` on.
    private void callFn(Fn f, List<Ty> args, int from, boolean exact) {
        if (!exact) {
            escapeFn(f);
            for (Ty t : args) {
                escape(t);
            }
            return;
        }
        for (int i = 0; i < args.size(); i++) {
            f.params[from + i] = joinInto(f.params[from + i], args.get(i));
        }
    }

    // ---- reflection-free traversal ---------------------------------------

    interface Visit {
        void on(Object child);
    }

    /// Calls `v` for every AST child (node or list element) of `o`.
    static void children(Object o, Visit v) {
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                v.on(x);
            }
            return;
        }
        if (!(o instanceof Node)) {
            return;
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
                    Object x = f.get(o);
                    if (x instanceof List) {
                        for (Object y : (List<?>) x) {
                            v.on(y);
                        }
                    } else if (x != null) {
                        v.on(x);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    String dump() {
        StringBuilder b = new StringBuilder();
        for (Fn f : fns.values()) {
            b.append(f.cls == null ? "" : f.cls.def.name + ".").append(f.def.name).append('(');
            for (int i = 0; i < f.params.length; i++) {
                b.append(i == 0 ? "" : ", ").append(f.names.get(i)).append(": ").append(f.params[i]);
            }
            b.append(") -> ").append(f.ret).append(f.escapes ? " [escapes]" : "").append("\n    ").append(f.locals)
                    .append('\n');
        }
        for (Map.Entry<Layouts.Cls, Map<String, Ty>> e : fields.entrySet()) {
            b.append(e.getKey().def.name).append(' ').append(e.getValue()).append(" definite=")
                    .append(definite.get(e.getKey())).append('\n');
        }
        b.append("globals ").append(globals).append("\nsites ").append(siteElem).append(" escaped ")
                .append(escapedSites).append(allListsEscape ? " ALL" : "").append('\n');
        return b.toString();
    }
}
