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
import com.codename1.python.transpiler.parser.Lexer;
import com.codename1.python.transpiler.parser.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Unrolls a loop over attribute names the program spells out.
///
/// openpyxl walks its fields by name:
///
/// ```python
/// def __iter__(self):
///     for attr in self.__attrs__:
///         value = getattr(self, attr)
///         ...
/// ```
///
/// When the loop variable is the name argument of a
/// `getattr`/`setattr`/`hasattr`/`delattr` and the iterable is a
/// statically known sequence of identifier strings -- a literal tuple or
/// list, a module-level tuple bound once, or `self.X`/`cls.X` where `X` is
/// a class-level tuple (as `DeclarativeMap` writes `__attrs__`,
/// `__elements__` and `__nested__`, or as the source spells `__fields__`)
/// -- the loop is replaced by one copy of its body per name with the name
/// written in as a literal. Every attribute access is then literal, which
/// the strict build accepts and the inference can type.
///
/// `self.X` is resolved per concrete class: a subclass whose tuple
/// differs gets its own copy of the unrolled method, in the same module;
/// a method reached through `super()` from an override with a different
/// tuple, a copy that would cross modules, or one whose body names
/// `super` or a private (mangled) name is refused. A loop with `else`,
/// `break` or `continue`, a body that rebinds the variable or closes
/// over it, a variable read after the loop, a tuple attribute stored
/// anywhere in the program, and a method whose tree no longer matches its
/// source text are refused as well. No name-dispatch code is generated.
public final class LoopUnroll {

    private static final int MAX_PASSES = 64;
    private static final int MAX_DEPTH = 32;

    private LoopUnroll() {
    }

    /// A class, the module it is in and the program classes it derives from.
    private static final class Cls {
        final String path;
        final ClassDef def;
        final List<Cls> bases = new ArrayList<Cls>();
        final List<Cls> subs = new ArrayList<Cls>();
        boolean opaque;
        /// A base names no single program class: what derives from what is
        /// then unknown, not only what the class binds.
        boolean unlinked;

        Cls(String path, ClassDef def) {
            this.path = path;
            this.def = def;
        }
    }

    /// The program as this pass reads it.
    private static final class World {
        final Map<String, Node.Module> trees;
        final Map<String, List<Cls>> byName = new HashMap<String, List<Cls>>();
        final Map<ClassDef, Cls> byDef = new IdentityHashMap<ClassDef, Cls>();
        final Set<String> storedAttrs = new HashSet<String>();
        /// Per attribute, the classes whose instances (or the classes
        /// themselves) a store with a known receiver can reach; closed under
        /// [#family]. A class outside it keeps its class-level tuple.
        final Map<String, Set<Cls>> blocked = new HashMap<String, Set<Cls>>();
        /// Attributes whose receivers were only inferred from a typed
        /// sequence descriptor: an unrolled read of one on the receiver is
        /// guarded at run time ([#guardText]).
        final Set<String> guarded = new HashSet<String>();
        /// Set while [#names] relies on a [#guarded] attribute of the receiver.
        String needGuard;
        /// Per `path:name`, whether that function is an options-dict helper.
        final Map<String, Boolean> helpers = new HashMap<String, Boolean>();
        /// Per attribute, whether [#frozen] proved every list a class body
        /// binds to it is never mutated or handed on.
        final Map<String, Boolean> frozen = new HashMap<String, Boolean>();
        /// The text of each method ConstArgSpecialize made, which no module source holds.
        final Map<FunctionDef, ConstArgSpecialize.Text> made = new IdentityHashMap<FunctionDef, ConstArgSpecialize.Text>();
        /// Calls whose arguments [#frozen] may ignore: ConstArgSpecialize is
        /// replacing every one of them.
        final Set<Object> exempt = java.util.Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

        World(Map<String, Node.Module> trees) {
            this.trees = trees;
        }
    }

    /// Unrolls every qualifying loop among `trees`; `sources` holds the
    /// text each tree was parsed from, under the same keys.
    public static void rewrite(Map<String, Node.Module> trees, Map<String, String> sources) {
        rewrite(trees, sources, new IdentityHashMap<FunctionDef, ConstArgSpecialize.Text>());
    }

    /// As [#rewrite(Map, Map)], reading the text of a method ConstArgSpecialize
    /// cloned from `made`.
    public static void rewrite(Map<String, Node.Module> trees, Map<String, String> sources,
            Map<FunctionDef, ConstArgSpecialize.Text> made) {
        World w = build(trees);
        w.made.putAll(made);
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            String path = e.getKey();
            for (Stmt s : new ArrayList<Stmt>(e.getValue().body)) {
                if (s instanceof FunctionDef) {
                    unrollFunction(w, path, null, (FunctionDef) s, e.getValue().body, sources);
                }
            }
            unrollModule(w, path, e.getValue(), sources.get(path));
        }
        for (List<Cls> l : new ArrayList<List<Cls>>(w.byName.values())) {
            for (Cls c : l) {
                for (Stmt s : new ArrayList<Stmt>(c.def.body)) {
                    if (s instanceof FunctionDef) {
                        unrollFunction(w, c.path, c, (FunctionDef) s, c.def.body, sources);
                    }
                }
            }
        }
        dropHelpers(w);
    }

    /// The identifier strings `e`, an argument of a call in module `path`,
    /// always evaluates to (a literal, a module constant, or a class-level
    /// sequence such as `Container.cached_properties`), or null. The calls
    /// in `exempt` are being specialized away, so handing the sequence to
    /// them does not count as letting it escape.
    static List<String> staticSeq(Map<String, Node.Module> trees, String path, Expr e,
            java.util.Collection<? extends Object> exempt) {
        World w = build(trees);
        w.exempt.addAll(exempt);
        w.needGuard = null;
        List<String> r = names(w, path, null, null, e, false, 0);
        return w.needGuard == null ? r : null;
    }

    private static World build(Map<String, Node.Module> trees) {
        World w = new World(trees);
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            collect(w, e.getKey(), e.getValue());
        }
        for (List<Cls> l : w.byName.values()) {
            for (Cls c : l) {
                for (Expr b : c.def.bases) {
                    String n = b instanceof Name ? ((Name) b).id : b instanceof Attribute ? ((Attribute) b).attr : null;
                    if (n == null) {
                        c.opaque = true;
                        c.unlinked = true;
                        continue;
                    }
                    Cls base = find(w, n, c.path);
                    if (base == AMBIGUOUS) {
                        c.opaque = true;
                        c.unlinked = true;
                    } else if (base != null) {
                        c.bases.add(base);
                        base.subs.add(c);
                    }
                }
                if (!c.def.keywords.isEmpty()) {
                    c.opaque = true;
                }
            }
        }
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            boolean meta = DeclarativeMap.META_PATH.equals(e.getKey());
            for (Stmt s : e.getValue().body) {
                typeStores(w, e.getKey(), meta, null, null, s);
            }
        }
        return w;
    }

    /// Removes an options-dict helper every call of which was lowered: no
    /// name, attribute, string or import anywhere spells it any more.
    private static void dropHelpers(World w) {
        for (Map.Entry<String, Boolean> h : w.helpers.entrySet()) {
            if (!h.getValue().booleanValue()) {
                continue;
            }
            int cut = h.getKey().lastIndexOf(':');
            String path = h.getKey().substring(0, cut);
            String name = h.getKey().substring(cut + 1);
            int refs = 0;
            for (Map.Entry<String, Node.Module> e : w.trees.entrySet()) {
                refs += refsOther(e.getValue(), name, false);
            }
            Node.Module m = w.trees.get(path);
            int defs = 0;
            for (Stmt s : m.body) {
                if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                    defs++;
                }
            }
            // With own == false the definition and every call count, so the
            // one definition is all that is left.
            if (defs == 1 && refs == 1) {
                for (int i = 0; i < m.body.size(); i++) {
                    if (m.body.get(i) instanceof FunctionDef && name.equals(((FunctionDef) m.body.get(i)).name)) {
                        m.body.remove(i);
                        break;
                    }
                }
            }
        }
    }

    private static final Cls AMBIGUOUS = new Cls("", null);

    private static Cls find(World w, String name, String path) {
        List<Cls> l = w.byName.get(name);
        if (l == null || l.isEmpty()) {
            return null;
        }
        if (l.size() == 1) {
            return l.get(0);
        }
        Cls hit = null;
        for (Cls c : l) {
            if (c.path.equals(path)) {
                if (hit != null) {
                    return AMBIGUOUS;
                }
                hit = c;
            }
        }
        return hit == null ? AMBIGUOUS : hit;
    }

    private static void collect(final World w, final String path, Object o) {
        if (o instanceof ClassDef) {
            ClassDef d = (ClassDef) o;
            Cls c = new Cls(path, d);
            List<Cls> l = w.byName.get(d.name);
            if (l == null) {
                l = new ArrayList<Cls>();
                w.byName.put(d.name, l);
            }
            l.add(c);
            w.byDef.put(d, c);
        }
        List<Expr> targets = new ArrayList<Expr>();
        if (o instanceof Node.Assign) {
            targets.addAll(((Node.Assign) o).targets);
        } else if (o instanceof Node.AugAssign) {
            targets.add(((Node.AugAssign) o).target);
        } else if (o instanceof Node.AnnAssign) {
            targets.add(((Node.AnnAssign) o).target);
        } else if (o instanceof Node.Delete) {
            targets.addAll(((Node.Delete) o).targets);
        } else if (o instanceof Node.For) {
            targets.add(((Node.For) o).target);
        }
        if (!DeclarativeMap.META_PATH.equals(path)) {
            // The model metaclass only fills in what a class body left
            // out, and a class under a metaclass is resolved from its own
            // body alone (classAttr).
            for (Expr t : targets) {
                storedAttrs(w, t);
            }
        }
        Infer.children(o, x -> collect(w, path, x));
    }

    private static void storedAttrs(final World w, Object t) {
        if (t instanceof Attribute) {
            // Typed by typeStores once every class is known.
            return;
        }
        if (t instanceof Node.TupleExpr || t instanceof Node.ListExpr || t instanceof Node.Starred) {
            Infer.children(t, x -> storedAttrs(w, x));
        }
    }

    /// Unrolls the qualifying loops at the top level of module `m`
    /// (`for name in ("sqrt", "exp"): getattr(cmath, name)(z)`). The loop
    /// variable is a module global, so it must be read nowhere else at
    /// module level, and it is bound to the last name afterwards, as the
    /// loop would have left it.
    private static void unrollModule(World w, String path, Node.Module m, String src) {
        if (src == null) {
            return;
        }
        String[] lines = src.split("\n", -1);
        for (int i = 0; i < m.body.size(); i++) {
            if (!(m.body.get(i) instanceof Node.For)) {
                continue;
            }
            Node.For loop = (Node.For) m.body.get(i);
            if (loop.isAsync || !(loop.target instanceof Name) || !hasCandidate(loop)) {
                continue;
            }
            String v = ((Name) loop.target).id;
            // Other module-level reads and stores of v run before or after
            // the loop and see what they would have seen: the unrolled text
            // ends by binding v to the last name. Only code that can run
            // DURING the loop -- a function or lambda body the loop calls --
            // could observe the per-iteration value, so a deferred use of v
            // (or an import binding it) refuses the loop.
            if (declaresGlobal(m, v) || deferredUses(m, v, false) != 0) {
                continue;
            }
            int endLine = lines.length;
            if (i + 1 < m.body.size()) {
                endLine = startLine(m.body.get(i + 1)) - 1;
            }
            if (loop.line < 1 || endLine < loop.line || endLine > lines.length) {
                continue;
            }
            StringBuilder b = new StringBuilder("def _cn1_m():\n");
            for (int l = loop.line; l <= endLine; l++) {
                String s = lines[l - 1];
                if (s.startsWith(" ") || s.startsWith("\t") && !s.trim().isEmpty()) {
                    if (s.startsWith("\t")) {
                        b = null;
                        break;
                    }
                }
                b.append(s.isEmpty() ? "" : "    " + s).append('\n');
            }
            if (b == null) {
                continue;
            }
            ConstArgSpecialize.Text t = new ConstArgSpecialize.Text(b.toString(), loop.line - 1);
            FunctionDef f;
            try {
                f = ConstArgSpecialize.parseMethod(t.src, t.line, path);
            } catch (RuntimeException ex) {
                continue;
            }
            if (f.body.size() != 1 || !ConstArgSpecialize.dump(f.body.get(0)).equals(ConstArgSpecialize.dump(loop))) {
                continue;
            }
            List<String> names = qualifies(w, path, null, null, f, (Node.For) f.body.get(0));
            if (names == null) {
                continue;
            }
            String out = unrollAll(w, path, null, null, t);
            if (out == null) {
                continue;
            }
            if (!names.isEmpty()) {
                out = out + (out.endsWith("\n") ? "" : "\n") + "    " + v + " = '" + names.get(names.size() - 1) + "'\n";
            }
            FunctionDef g;
            try {
                g = ConstArgSpecialize.parseMethod(out, t.line, path);
            } catch (RuntimeException ex) {
                continue;
            }
            m.body.remove(i);
            m.body.addAll(i, g.body);
            i += g.body.size() - 1;
        }
    }

    /// The first line `s` occupies, decorators included.
    private static int startLine(Stmt s) {
        int l = s.line;
        List<Expr> decs = s instanceof FunctionDef ? ((FunctionDef) s).decorators
                : s instanceof ClassDef ? ((ClassDef) s).decorators : null;
        if (decs != null) {
            for (Expr d : decs) {
                l = Math.min(l, d.line - 1);
            }
        }
        return l;
    }

    /// The reads and stores of the global `v` inside function and lambda
    /// bodies (deferred code) under `o`, plus 1000 for an import binding it.
    private static int deferredUses(Object o, final String v, final boolean deferred) {
        if (o instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) o;
            if ((paramNamed(f, v) || binds(f.body, v)) && !declaresGlobal(f, v)) {
                return 0;
            }
        }
        if (o instanceof Node.Alias) {
            Node.Alias a = (Node.Alias) o;
            String b = a.asname != null ? a.asname : a.name;
            if (v.equals(b) || "*".equals(a.name)) {
                return 1000;
            }
        }
        final boolean inner = deferred || o instanceof FunctionDef || o instanceof Node.Lambda;
        final int[] n = {inner && o instanceof Name && v.equals(((Name) o).id) ? 1 : 0};
        Infer.children(o, x -> n[0] += deferredUses(x, v, inner));
        return n[0];
    }

    // ---- attribute stores, by the classes they can reach ----

    /// Types every attribute store under `o`. `cls` is the class whose body
    /// directly holds the method `fn` (null elsewhere). A store whose
    /// receiver is the method's own `self`/`cls`, or a program class by
    /// name, reaches that class's [#family] only; one whose receiver is the
    /// element variable of a loop over a typed sequence descriptor reaches
    /// that element type's family and leaves the attribute [World#guarded];
    /// any other store makes the attribute non-constant everywhere.
    private static void typeStores(final World w, final String path, final boolean meta, final Cls cls,
            final FunctionDef fn, Object o) {
        if (o instanceof ClassDef) {
            Cls c = w.byDef.get(o);
            for (Stmt s : ((ClassDef) o).body) {
                if (s instanceof FunctionDef) {
                    FunctionDef d = (FunctionDef) s;
                    typeStores(w, path, meta, c, d, d.body);
                    typeStores(w, path, meta, null, fn, d.decorators);
                    typeStores(w, path, meta, null, fn, d.args);
                } else {
                    typeStores(w, path, meta, null, fn, s);
                }
            }
            typeStores(w, path, meta, null, fn, ((ClassDef) o).bases);
            return;
        }
        if (o instanceof FunctionDef) {
            FunctionDef d = (FunctionDef) o;
            typeStores(w, path, meta, null, d, d.body);
            typeStores(w, path, meta, null, fn, d.decorators);
            typeStores(w, path, meta, null, fn, d.args);
            return;
        }
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                typeStores(w, path, meta, cls, fn, x);
            }
            return;
        }
        List<Expr> targets = new ArrayList<Expr>();
        if (o instanceof Node.Assign) {
            targets.addAll(((Node.Assign) o).targets);
        } else if (o instanceof Node.AugAssign) {
            targets.add(((Node.AugAssign) o).target);
        } else if (o instanceof Node.AnnAssign) {
            targets.add(((Node.AnnAssign) o).target);
        } else if (o instanceof Node.Delete) {
            targets.addAll(((Node.Delete) o).targets);
        } else if (o instanceof Node.For) {
            targets.add(((Node.For) o).target);
        }
        if (!meta) {
            for (Expr t : targets) {
                storeTarget(w, path, cls, fn, t);
            }
        }
        if (o instanceof Call && ((Call) o).func instanceof Name) {
            Call c = (Call) o;
            String f = ((Name) c.func).id;
            if (("setattr".equals(f) || "delattr".equals(f)) && c.args.size() >= 2
                    && c.args.get(1) instanceof Constant) {
                store(w, path, cls, fn, c.args.get(0), ((Constant) c.args.get(1)).text);
            }
        }
        Infer.children(o, x -> typeStores(w, path, meta, cls, fn, x));
    }

    private static void storeTarget(World w, String path, Cls cls, FunctionDef fn, Expr t) {
        if (t instanceof Attribute) {
            store(w, path, cls, fn, ((Attribute) t).value, ((Attribute) t).attr);
        } else if (t instanceof Node.TupleExpr) {
            for (Expr x : ((Node.TupleExpr) t).elts) {
                storeTarget(w, path, cls, fn, x);
            }
        } else if (t instanceof Node.ListExpr) {
            for (Expr x : ((Node.ListExpr) t).elts) {
                storeTarget(w, path, cls, fn, x);
            }
        } else if (t instanceof Node.Starred) {
            storeTarget(w, path, cls, fn, ((Node.Starred) t).value);
        }
    }

    private static void store(World w, String path, Cls cls, FunctionDef fn, Expr recv, String attr) {
        if (w.storedAttrs.contains(attr)) {
            return;
        }
        Set<Cls> reach = null;
        boolean guard = false;
        if (recv instanceof Name) {
            String n = ((Name) recv).id;
            String self = selfName(cls, fn);
            if (n.equals(self)) {
                reach = family(w, cls);
            } else if (fn != null && (paramNamed(fn, n) || binds(fn.body, n))) {
                Set<Cls> elems = sequenceElements(w, cls, fn, self, n);
                if (elems != null) {
                    reach = new HashSet<Cls>();
                    for (Cls x : elems) {
                        Set<Cls> f = family(w, x);
                        if (f == null) {
                            reach = null;
                            break;
                        }
                        reach.addAll(f);
                    }
                    guard = true;
                }
            } else if (fn == null || !declaresGlobal(fn, n)) {
                Cls c = find(w, n, path);
                reach = c == null || c == AMBIGUOUS ? null : family(w, c); //NOPMD CompareObjectsWithEquals - sentinel
            }
        }
        if (reach == null) {
            w.storedAttrs.add(attr);
            w.blocked.remove(attr);
            w.guarded.remove(attr);
            return;
        }
        Set<Cls> b = w.blocked.get(attr);
        if (b == null) {
            b = new HashSet<Cls>();
            w.blocked.put(attr, b);
        }
        b.addAll(reach);
        if (guard) {
            w.guarded.add(attr);
        }
    }

    /// The name the method `fn` of `cls` receives its instance or class as,
    /// or null.
    private static String selfName(Cls cls, FunctionDef fn) {
        if (cls == null || fn == null || fn.args.positional.isEmpty()) {
            return null;
        }
        boolean plain = fn.decorators.isEmpty();
        boolean cm = fn.decorators.size() == 1 && fn.decorators.get(0) instanceof Name
                && "classmethod".equals(((Name) fn.decorators.get(0)).id);
        String n = fn.args.positional.get(0).name;
        return (plain || cm) && !binds(fn.body, n) ? n : null;
    }

    private static boolean paramNamed(FunctionDef fn, String n) {
        final boolean[] hit = {false};
        Infer.children(fn.args, x -> hit[0] |= x instanceof Node.Arg && n.equals(((Node.Arg) x).name));
        return hit[0];
    }

    /// `c`, every class deriving from it and every ancestor of those: the
    /// classes an instance of `c` (or `c` itself) can be looked up through.
    /// Null when an opaque class could hide part of it.
    private static Set<Cls> family(World w, Cls c) {
        if (c == null) {
            return null;
        }
        Set<Cls> down = new HashSet<Cls>();
        List<Cls> work = new ArrayList<Cls>();
        work.add(c);
        while (!work.isEmpty()) {
            Cls k = work.remove(work.size() - 1);
            if (down.add(k)) {
                work.addAll(k.subs);
            }
        }
        Set<Cls> out = new HashSet<Cls>();
        for (Cls d : down) {
            if (!ancestors(d, out)) {
                return null;
            }
        }
        Set<String> names = new HashSet<String>();
        for (Cls x : out) {
            names.add(x.def.name);
        }
        for (List<Cls> l : w.byName.values()) {
            for (Cls x : l) {
                if (x.unlinked && !out.contains(x)) {
                    for (Expr b : x.def.bases) {
                        // A plain name base was resolved by [#build] in x's
                        // own module: x is in the family through its
                        // subclass links when it names one of these, and
                        // a same-named class of another module is not this
                        // family -- unless the name is ambiguous there.
                        if (b instanceof Name ? names.contains(((Name) b).id)
                                && find(w, ((Name) b).id, x.path) == AMBIGUOUS //NOPMD CompareObjectsWithEquals - sentinel
                                : mentions(b, names)) {
                            return null;
                        }
                    }
                }
            }
        }
        return out;
    }

    /// True when `e` spells one of `names`, as a name or an attribute: a
    /// base computed by a call can only reach a family class through one.
    private static boolean mentions(Object e, final Set<String> names) {
        if (e instanceof Name && names.contains(((Name) e).id)
                || e instanceof Attribute && names.contains(((Attribute) e).attr)) {
            return true;
        }
        final boolean[] hit = {false};
        Infer.children(e, x -> hit[0] |= mentions(x, names));
        return hit[0];
    }

    private static boolean ancestors(Cls c, Set<Cls> out) {
        if (!out.add(c)) {
            return true;
        }
        if (c.unlinked) {
            return false;
        }
        for (Cls b : c.bases) {
            if (!ancestors(b, out)) {
                return false;
            }
        }
        return true;
    }

    /// The element classes of `n` when the method's only binding of it is
    /// `for n in self.F:` and every class `self` can be binds `F` to a
    /// sequence descriptor naming its element type (openpyxl's `Sequence`,
    /// which converts each element it is given), an empty tuple, or another
    /// class's `F`. A list such a descriptor keeps can still be mutated, so
    /// the claim is only trusted behind a run-time guard ([World#guarded]).
    private static Set<Cls> sequenceElements(World w, Cls cls, FunctionDef fn, String self, String n) {
        if (self == null || paramNamed(fn, n) || count(fn, n) < 1) {
            return null;
        }
        final Node.For[] only = {null};
        final int[] binds = {0};
        countBinds(fn.body, n, only, binds);
        if (binds[0] != 1 || only[0] == null || !(only[0].iter instanceof Attribute)
                || !(((Attribute) only[0].iter).value instanceof Name)
                || !self.equals(((Name) ((Attribute) only[0].iter).value).id)) {
            return null;
        }
        String f = ((Attribute) only[0].iter).attr;
        Set<Cls> fam = family(w, cls);
        if (fam == null) {
            return null;
        }
        Set<Cls> out = new HashSet<Cls>();
        for (Cls k : fam) {
            if (!sequenceBinding(w, k, f, out, 0)) {
                return null;
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static void countBinds(Object o, String n, Node.For[] only, int[] binds) {
        if (bindsHere(o, n)) {
            binds[0]++;
            if (o instanceof Node.For && ((Node.For) o).target instanceof Name) {
                only[0] = (Node.For) o;
            } else {
                binds[0] += 2;
            }
        }
        if (o instanceof Node.Global && ((Node.Global) o).names.contains(n)) {
            binds[0] += 2;
        }
        if (o instanceof Node.Comp) {
            return;
        }
        Infer.children(o, x -> countBinds(x, n, only, binds));
    }

    private static boolean sequenceBinding(World w, Cls k, String f, Set<Cls> out, int depth) {
        if (depth > MAX_DEPTH) {
            return false;
        }
        for (Stmt s : k.def.body) {
            if (!bindsHere(s, f)) {
                continue;
            }
            if (!(s instanceof Node.Assign) || ((Node.Assign) s).targets.size() != 1
                    || !(((Node.Assign) s).targets.get(0) instanceof Name)) {
                return false;
            }
            Expr v = ((Node.Assign) s).value;
            if (v instanceof Node.TupleExpr && ((Node.TupleExpr) v).elts.isEmpty()) {
                continue;
            }
            if (v instanceof Attribute && ((Attribute) v).value instanceof Name && f.equals(((Attribute) v).attr)) {
                Cls b = find(w, ((Name) ((Attribute) v).value).id, k.path);
                if (b == null || b == AMBIGUOUS || !sequenceBinding(w, b, f, out, depth + 1)) { //NOPMD CompareObjectsWithEquals - sentinel
                    return false;
                }
                continue;
            }
            if (!(v instanceof Call) || !(((Call) v).func instanceof Name)
                    || !"Sequence".equals(((Name) ((Call) v).func).id)
                    || !importsFrom(w, k.path, "Sequence", "openpyxl.descriptors")) {
                return false;
            }
            Cls e = null;
            for (Node.Keyword kw : ((Call) v).keywords) {
                if ("expected_type".equals(kw.arg) && kw.value instanceof Name) {
                    e = find(w, ((Name) kw.value).id, k.path);
                }
            }
            if (e == null || e == AMBIGUOUS) { //NOPMD CompareObjectsWithEquals - sentinel
                return false;
            }
            out.add(e);
        }
        return true;
    }

    /// True when module `path` binds `name` at top level only through
    /// `from <a module under pkg> import name`.
    private static boolean importsFrom(World w, String path, String name, String pkg) {
        Node.Module m = w.trees.get(path);
        if (m == null) {
            return false;
        }
        boolean hit = false;
        for (Stmt s : m.body) {
            if (s instanceof Node.ImportFrom) {
                Node.ImportFrom f = (Node.ImportFrom) s;
                for (Node.Alias a : f.names) {
                    if (name.equals(a.asname != null ? a.asname : a.name)) {
                        if (f.level != 0 || f.module == null || !(f.module.equals(pkg) || f.module.startsWith(pkg + "."))) {
                            return false;
                        }
                        hit = true;
                    }
                }
            } else if (binds(s, name)) {
                return false;
            }
        }
        return hit;
    }

    /// The line an unrolled loop over the receiver's `attr` starts with:
    /// the names were read from the class, and an instance may hold its
    /// own tuple, so a different one stops the method loudly rather than
    /// running the class's copies.
    private static String guardText(String recv, String attr, List<String> names) {
        StringBuilder b = new StringBuilder("if ").append(recv).append('.').append(attr).append(" != (");
        for (String n : names) {
            b.append('\'').append(n).append("', ");
        }
        return b.append("): raise TypeError('").append(attr)
                .append(" differs from the class tuple the loop was unrolled for')").toString();
    }

    // ---- the method ----

    private static void unrollFunction(World w, String path, Cls owner, FunctionDef f, List<Stmt> body,
            Map<String, String> sources) {
        if (f.isAsync || !hasCandidate(f)) {
            return;
        }
        String recv = null;
        if (owner != null && !f.args.positional.isEmpty()) {
            boolean plain = f.decorators.isEmpty();
            boolean cm = f.decorators.size() == 1 && f.decorators.get(0) instanceof Name
                    && "classmethod".equals(((Name) f.decorators.get(0)).id);
            if ((plain || cm) && !binds(f.body, f.args.positional.get(0).name)) {
                recv = f.args.positional.get(0).name;
            }
        }
        ConstArgSpecialize.Text t = w.made.get(f);
        if (t == null) {
            t = ConstArgSpecialize.extract(sources.get(path), f);
        }
        if (t == null || !ConstArgSpecialize.dump(f).equals(
                ConstArgSpecialize.dump(ConstArgSpecialize.parseMethod(t.src, t.line, path)))) {
            return;
        }
        String own = unrollAll(w, path, owner, recv, t);
        if (own == null) {
            return;
        }
        Map<Cls, String> copies = new LinkedHashMap<Cls, String>();
        if (owner != null && recv != null) {
            Set<Cls> needs = new HashSet<Cls>();
            List<Cls> order = descendants(owner);
            if (order == null) {
                return;
            }
            for (Cls k : order) {
                String tk = unrollAll(w, path, k, recv, t);
                if (tk == null) {
                    return;
                }
                boolean overridden = overridesBetween(k, owner, f.name);
                boolean inheritsCopy = false;
                for (Cls b : k.bases) {
                    if (needs.contains(b)) {
                        inheritsCopy = true;
                    }
                }
                if (overridden) {
                    // Reached only through super(): the text must not depend on the class.
                    if (!tk.equals(own)) {
                        return;
                    }
                    continue;
                }
                if (!tk.equals(own) || inheritsCopy) {
                    if (!copyable(tk) || !k.path.equals(path) && !portable(w, path, k.path, tk)) {
                        return;
                    }
                    needs.add(k);
                    copies.put(k, tk);
                }
            }
        }
        int at = indexOf(body, f);
        body.set(at, ConstArgSpecialize.parseMethod(own, t.line, path));
        for (Map.Entry<Cls, String> e : copies.entrySet()) {
            e.getKey().def.body.add(ConstArgSpecialize.parseMethod(e.getValue(), t.line, path));
        }
    }

    /// Every program subclass of `c`, each after all of its bases that are
    /// themselves subclasses of `c`; null when one cannot be classified.
    private static List<Cls> descendants(Cls c) {
        Set<Cls> all = new HashSet<Cls>();
        List<Cls> work = new ArrayList<Cls>(c.subs);
        while (!work.isEmpty()) {
            Cls k = work.remove(work.size() - 1);
            if (k.opaque) {
                return null;
            }
            if (all.add(k)) {
                work.addAll(k.subs);
            }
        }
        List<Cls> order = new ArrayList<Cls>();
        Set<Cls> done = new HashSet<Cls>();
        done.add(c);
        while (order.size() < all.size()) {
            boolean moved = false;
            for (Cls k : all) {
                if (done.contains(k)) {
                    continue;
                }
                boolean ready = true;
                for (Cls b : k.bases) {
                    if (all.contains(b) && !done.contains(b)) {
                        ready = false;
                    }
                }
                if (ready) {
                    order.add(k);
                    done.add(k);
                    moved = true;
                }
            }
            if (!moved) {
                return null;
            }
        }
        return order;
    }

    /// True when `k` or a class between it and `top` defines `name`.
    private static boolean overridesBetween(Cls k, Cls top, String name) {
        if (k == top) { //NOPMD CompareObjectsWithEquals - identity is meant
            return false;
        }
        if (defines(k.def, name)) {
            return true;
        }
        for (Cls b : k.bases) {
            if (derives(b, top) && overridesBetween(b, top, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean derives(Cls k, Cls top) {
        if (k == top) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        for (Cls b : k.bases) {
            if (derives(b, top)) {
                return true;
            }
        }
        return false;
    }

    private static boolean defines(ClassDef d, String name) {
        for (Stmt s : d.body) {
            if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                return true;
            }
            if (s instanceof Node.Assign) {
                for (Expr t : ((Node.Assign) s).targets) {
                    if (t instanceof Name && name.equals(((Name) t).id)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// A copy placed in another class must mean the same there.
    private static boolean copyable(String src) {
        for (Token k : new Lexer(src, "<copy>").tokenize()) {
            if (k.kind == Token.Kind.NAME && ("super".equals(k.text) || "__class__".equals(k.text)
                    || k.text.startsWith("__") && !k.text.endsWith("__"))) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(List<Stmt> body, Stmt s) {
        for (int i = 0; i < body.size(); i++) {
            if (body.get(i) == s) { //NOPMD CompareObjectsWithEquals - identity is meant
                return i;
            }
        }
        throw new IllegalStateException("function not in its body");
    }

    private static boolean hasCandidate(Object o) {
        if (o instanceof Node.For && ((Node.For) o).target instanceof Name
                && namesAttribute(((Node.For) o).body, ((Name) ((Node.For) o).target).id)) {
            return true;
        }
        if (compCandidate(o) || o instanceof Call && ((Call) o).func instanceof Name
                && ((Call) o).args.size() == 1 && ((Call) o).keywords.isEmpty()) {
            // A one-argument call might be an options-dict helper (findOptionsDict).
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && hasCandidate(x)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// `t` with every qualifying loop unrolled for receiver class `k`, or
    /// null when nothing qualifies.
    private static String unrollAll(World w, String path, Cls k, String recv, ConstArgSpecialize.Text t) {
        String src = t.src;
        boolean any = false;
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            FunctionDef f = ConstArgSpecialize.parseMethod(src, t.line, path);
            Object[] hit = new Object[3];
            findLoop(w, path, k, recv, f, f, hit);
            if (hit[0] == null) {
                findComp(w, path, k, recv, f, f, hit);
                if (hit[0] == null) {
                    findOptionsDict(w, path, k, recv, f, f, hit);
                    if (hit[0] == null) {
                        break;
                    }
                    @SuppressWarnings("unchecked")
                    List<String> keys = (List<String>) hit[1];
                    String dnext = optionsDictText(src, t.line, (Call) hit[0], keys);
                    if (dnext == null) {
                        break;
                    }
                    src = dnext;
                    any = true;
                    continue;
                }
                @SuppressWarnings("unchecked")
                List<String> cn = (List<String>) hit[1];
                String cnext = compText(src, t.line, (Node.Comp) hit[0], cn, (String) hit[2]);
                if (cnext == null) {
                    break;
                }
                src = cnext;
                any = true;
                continue;
            }
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) hit[1];
            String next = unrollText(src, t.line, (Node.For) hit[0], names, (String) hit[2]);
            if (next == null) {
                break;
            }
            src = next;
            any = true;
        }
        return any ? src : null;
    }

    /// Whether `o` is a list comprehension over one plain name whose element
    /// names an attribute by it: `[getattr(self, a) for a in self.__attrs__]`.
    private static boolean compCandidate(Object o) {
        if (!(o instanceof Node.Comp)) {
            return false;
        }
        Node.Comp c = (Node.Comp) o;
        if (c.kind != Node.Comp.Kind.LIST || c.generators.size() != 1) {
            return false;
        }
        Node.Comprehension g = c.generators.get(0);
        return !g.isAsync && g.ifs.isEmpty() && g.target instanceof Name
                && namesAttribute(c.elt, ((Name) g.target).id);
    }

    /// Finds a list comprehension [#compCandidate] whose iterable is a known
    /// sequence of names. It becomes a list display with one element per
    /// name: the iterable is evaluated first in both forms and is free of
    /// effects ([#names] accepts only literals and constant tuples), the
    /// elements are then evaluated in the same order, and the comprehension
    /// variable is local to the comprehension, so nothing outside can see
    /// that it is gone. A guarded receiver, a closure or comprehension over
    /// the variable, or a walrus binding it is refused.
    private static void findComp(final World w, final String path, final Cls k, final String recv,
            final FunctionDef f, Object o, final Object[] hit) {
        if (hit[0] != null) {
            return;
        }
        if (compCandidate(o)) {
            Node.Comp c = (Node.Comp) o;
            String v = ((Name) c.generators.get(0).target).id;
            if (!badUse(c.elt, v)) {
                w.needGuard = null;
                List<String> names = names(w, path, k, recv, c.generators.get(0).iter, true, 0);
                String guard = null;
                if (names != null && w.needGuard != null) {
                    guard = compGuard(w, path, k, recv, c.generators.get(0).iter, w.needGuard);
                }
                if (names != null && (w.needGuard == null || guard != null)) {
                    hit[0] = c;
                    hit[1] = names;
                    hit[2] = guard;
                    w.needGuard = null;
                    return;
                }
                w.needGuard = null;
            }
        }
        if (o instanceof Node.Lambda || o instanceof ClassDef
                || o instanceof FunctionDef && o != f) { //NOPMD CompareObjectsWithEquals - identity is meant
            return;
        }
        Infer.children(o, x -> findComp(w, path, k, recv, f, x, hit));
    }

    /// The run-time guard for the operand `self.<attr>` of a `+` chain
    /// `e` whose names were inferred from a typed descriptor, or null.
    private static String compGuard(World w, String path, Cls k, String recv, Expr e, String attr) {
        List<Expr> ops = new ArrayList<Expr>();
        Expr x = e;
        while (x instanceof Node.BinOp && "+".equals(((Node.BinOp) x).op)) {
            ops.add(0, ((Node.BinOp) x).right);
            x = ((Node.BinOp) x).left;
        }
        ops.add(0, x);
        String out = null;
        for (Expr op : ops) {
            if (op instanceof Attribute && ((Attribute) op).value instanceof Name
                    && recv != null && recv.equals(((Name) ((Attribute) op).value).id)
                    && attr.equals(((Attribute) op).attr)) {
                if (out != null) {
                    return null;
                }
                w.needGuard = null;
                List<String> part = names(w, path, k, recv, op, true, 0);
                if (part == null || !attr.equals(w.needGuard)) {
                    return null;
                }
                out = guardText(recv, attr, part);
            }
        }
        w.needGuard = attr;
        return out;
    }

    /// `src` with the one-line comprehension `comp` written as a list
    /// display of its element once per name, or null.
    private static String compText(String src, int line0, Node.Comp comp, List<String> names, String guard) {
        String v = ((Name) comp.generators.get(0).target).id;
        List<Token> toks = new Lexer(src, "<unroll>").tokenize();
        int rel = comp.line - line0 + 1;
        int open = -1;
        int forAt = -1;
        int close = -1;
        for (int i = 0; i < toks.size(); i++) {
            Token k = toks.get(i);
            if (k.line != rel || !k.is("[")) {
                continue;
            }
            int depth = 0;
            int fa = -1;
            int cl = -1;
            for (int j = i; j < toks.size(); j++) {
                Token q = toks.get(j);
                if (q.is("[") || q.is("(") || q.is("{")) {
                    depth++;
                } else if (q.is("]") || q.is(")") || q.is("}")) {
                    depth--;
                    if (depth == 0) {
                        cl = j;
                        break;
                    }
                } else if (depth == 1 && q.is("for")) {
                    if (fa >= 0) {
                        fa = -2;
                    } else {
                        fa = j;
                    }
                }
            }
            if (fa >= 0 && cl > 0 && toks.get(fa + 1).kind == Token.Kind.NAME && toks.get(fa + 1).text.equals(v)
                    && toks.get(fa + 2).is("in")) {
                if (open >= 0) {
                    // Two candidates on one line: which one the tree means is unknown.
                    return null;
                }
                open = i;
                forAt = fa;
                close = cl;
            }
        }
        if (open < 0 || toks.get(close).line != rel || forAt == open + 1) {
            return null;
        }
        int[] starts = ConstArgSpecialize.lineStarts(src);
        int eltStart = ConstArgSpecialize.off(starts, toks.get(open + 1));
        int eltEnd = ConstArgSpecialize.off(starts, toks.get(forAt));
        StringBuilder disp = new StringBuilder("[");
        for (int n = 0; n < names.size(); n++) {
            StringBuilder b = new StringBuilder(src.substring(eltStart, eltEnd));
            int base = eltStart;
            for (int i = forAt - 1; i > open; i--) {
                Token k = toks.get(i);
                if (k.kind == Token.Kind.STRING && (k.prefix != null && k.prefix.indexOf('f') >= 0
                        && k.text.contains(v) || ConstArgSpecialize.count(k.text, '\n') > 0)) {
                    return null;
                }
                if (k.kind != Token.Kind.NAME || !k.text.equals(v) || toks.get(i - 1).is(".")) {
                    continue;
                }
                if (toks.get(i + 1).is("=")) {
                    return null;
                }
                int o = ConstArgSpecialize.off(starts, k) - base;
                b.replace(o, o + v.length(), "\"" + names.get(n) + "\"");
            }
            if (n > 0) {
                disp.append(", ");
            }
            disp.append(b);
        }
        disp.append(']');
        int s0 = ConstArgSpecialize.off(starts, toks.get(open));
        int s1 = ConstArgSpecialize.off(starts, toks.get(close)) + 1;
        if (guard != null) {
            // Only `name = [...]` as a whole statement: nothing of the
            // statement runs between the guard and the comprehension.
            if (open < 3 || !toks.get(open - 1).is("=") || toks.get(open - 2).kind != Token.Kind.NAME
                    || toks.get(open - 2).line != rel
                    || toks.get(open - 3).kind != Token.Kind.NEWLINE && toks.get(open - 3).kind != Token.Kind.INDENT
                    && toks.get(open - 3).kind != Token.Kind.DEDENT
                    || close + 1 >= toks.size() || toks.get(close + 1).kind != Token.Kind.NEWLINE) {
                return null;
            }
            int ls = ConstArgSpecialize.off(starts, toks.get(open - 2));
            int lineStart = starts[rel - 1];
            String ind = src.substring(lineStart, ls);
            if (!ind.trim().isEmpty()) {
                return null;
            }
            return src.substring(0, ls) + guard + "\n" + ind + src.substring(ls, s0) + disp + src.substring(s1);
        }
        return src.substring(0, s0) + disp + src.substring(s1);
    }

    // ---- options classes read as a dict ----

    /// markdownify collects its options from nested classes:
    ///
    /// ```python
    /// def _todict(obj):
    ///     return dict((k, getattr(obj, k)) for k in dir(obj) if not k.startswith('_'))
    /// ...
    /// self.options = _todict(self.DefaultOptions)
    /// ```
    ///
    /// When the helper has exactly that shape, nothing references it but
    /// one-argument calls in its own module, and the argument is `self.N` or
    /// a module class `N` where `N` resolves, for the receiver class this
    /// copy is made for, to a nested class whose body (and its nested base
    /// chain) binds plain names only, the call becomes the dict display
    /// `{"a": X.a, "b": X.b}` with the public names in `dir()` order
    /// (sorted). The values are still read at run time from the class, so
    /// only the key set is fixed at build time; it is refused when any of
    /// the names, or `N` itself, is stored anywhere a store can reach these
    /// classes, or when the classes involve a metaclass or decorator.
    private static void findOptionsDict(final World w, final String path, final Cls k, final String recv,
            final FunctionDef f, Object o, final Object[] hit) {
        if (hit[0] != null) {
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && ((Call) o).args.size() == 1
                && ((Call) o).keywords.isEmpty() && isOptionsHelper(w, path, ((Name) ((Call) o).func).id)) {
            List<String> keys = optionKeys(w, k, recv, ((Call) o).args.get(0));
            if (keys != null) {
                hit[0] = o;
                hit[1] = keys;
                return;
            }
        }
        if (o instanceof Node.Lambda || o instanceof ClassDef
                || o instanceof FunctionDef && o != f) { //NOPMD CompareObjectsWithEquals - identity is meant
            return;
        }
        Infer.children(o, x -> findOptionsDict(w, path, k, recv, f, x, hit));
    }

    /// Whether `name` is a module-level function of `path` with the exact
    /// shape [#findOptionsDict] reads, referenced only by one-argument calls
    /// in that module.
    private static boolean isOptionsHelper(World w, String path, String name) {
        String key = path + ":" + name;
        Boolean b = w.helpers.get(key);
        if (b == null) {
            b = Boolean.valueOf(helperShape(w, path, name));
            w.helpers.put(key, b);
        }
        return b.booleanValue();
    }

    private static boolean helperShape(World w, String path, String name) {
        Node.Module m = w.trees.get(path);
        if (m == null || declaresGlobal(m, name)) {
            return false;
        }
        FunctionDef def = null;
        for (Stmt s : m.body) {
            if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                if (def != null) {
                    return false;
                }
                def = (FunctionDef) s;
            } else if (binds(s, name)) {
                return false;
            }
        }
        if (def == null || def.isAsync || !def.decorators.isEmpty() || def.args.positional.size() != 1
                || !def.args.defaults.isEmpty() || def.args.vararg != null || !def.args.kwonly.isEmpty()
                || def.args.kwarg != null || def.body.size() != 1 || !(def.body.get(0) instanceof Node.Return)) {
            return false;
        }
        Expr r = ((Node.Return) def.body.get(0)).value;
        if (!(r instanceof Call) || ((Call) r).args.size() != 1 || !(((Call) r).args.get(0) instanceof Node.Comp)) {
            return false;
        }
        Node.Comp c = (Node.Comp) ((Call) r).args.get(0);
        if (c.generators.size() != 1 || !(c.generators.get(0).target instanceof Name)) {
            return false;
        }
        String p = def.args.positional.get(0).name;
        String v = ((Name) c.generators.get(0).target).id;
        if (p.equals(v) || "dict".equals(p) || "dir".equals(p) || "getattr".equals(p)
                || "dict".equals(v) || "dir".equals(v) || "getattr".equals(v)) {
            return false;
        }
        String tmpl = "def " + name + "(" + p + "):\n    return dict((" + v + ", getattr(" + p + ", " + v
                + ")) for " + v + " in dir(" + p + ") if not " + v + ".startswith('_'))\n";
        FunctionDef want;
        try {
            want = ConstArgSpecialize.parseMethod(tmpl, def.line, path);
        } catch (RuntimeException e) { //NOPMD AvoidCatchingGenericException - an unparsable template is no match
            return false;
        }
        if (want == null || !ConstArgSpecialize.dump(want).equals(ConstArgSpecialize.dump(def))) {
            return false;
        }
        // The builtins it calls must be the builtins.
        for (Stmt s : m.body) {
            if (binds(s, "dict") || binds(s, "dir") || binds(s, "getattr")) {
                return false;
            }
        }
        // Every reference is a direct one-argument call in this module.
        for (Map.Entry<String, Node.Module> e : w.trees.entrySet()) {
            int refs = refsOther(e.getValue(), name, e.getKey().equals(path));
            if (refs != 0) {
                return false;
            }
        }
        return true;
    }

    /// References of `name` other than a one-argument call (in its own
    /// module) or its own definition.
    private static int refsOther(Object o, final String name, final boolean own) {
        if (o instanceof Call && ((Call) o).func instanceof Name && name.equals(((Name) ((Call) o).func).id)) {
            Call c = (Call) o;
            int n = own && c.args.size() == 1 && c.keywords.isEmpty() ? 0 : 1;
            for (Expr a : c.args) {
                n += refsOther(a, name, own);
            }
            for (Node.Keyword kw : c.keywords) {
                n += refsOther(kw, name, own);
            }
            return n;
        }
        if (o instanceof Name && name.equals(((Name) o).id)
                || o instanceof Attribute && name.equals(((Attribute) o).attr)
                || o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR && name.equals(((Constant) o).text)
                || o instanceof Node.Alias && (name.equals(((Node.Alias) o).name)
                        || name.equals(((Node.Alias) o).asname))
                || o instanceof FunctionDef && name.equals(((FunctionDef) o).name) && !own) {
            return 1;
        }
        final int[] n = {0};
        Infer.children(o, x -> n[0] += refsOther(x, name, own));
        return n[0];
    }

    /// The sorted public names the options class `arg` evaluates to binds,
    /// or null.
    private static List<String> optionKeys(World w, Cls k, String recv, Expr arg) {
        Cls holder;
        String n;
        if (arg instanceof Attribute && ((Attribute) arg).value instanceof Name && recv != null
                && recv.equals(((Name) ((Attribute) arg).value).id) && k != null) {
            holder = k;
            n = ((Attribute) arg).attr;
        } else {
            return null;
        }
        ClassDef[] found = new ClassDef[2];
        if (!nestedClass(holder, n, found, 0)) {
            return null;
        }
        List<String> keys = new ArrayList<String>();
        Set<Cls> chain = new HashSet<Cls>();
        if (!classKeys(w, found[1], found[0], keys, chain, 0)) {
            return null;
        }
        Set<String> uniq = new java.util.TreeSet<String>(keys);
        if (uniq.size() != keys.size()) {
            // A name rebound down the chain: dir() lists it once, as here,
            // but keep to the plain case.
            return null;
        }
        // The key set is fixed only if nothing can store into or delete from
        // these class objects: each is reachable by name only as a helper
        // argument `X.N` or as a nested base, never stored, passed or spelled.
        for (Cls c : chain) {
            for (Map.Entry<String, Node.Module> e : w.trees.entrySet()) {
                if (classEscapes(w, e.getKey(), e.getValue(), c.def.name)) {
                    return null;
                }
            }
        }
        return new ArrayList<String>(uniq);
    }

    /// Whether the name `n` occurs in module `o` other than as the class
    /// definition itself, a base of a class definition, or the attribute of
    /// the sole argument of an options-dict helper call.
    private static boolean classEscapes(final World w, final String path, Object o, final String n) {
        if (o instanceof Call && ((Call) o).func instanceof Name && ((Call) o).args.size() == 1
                && ((Call) o).keywords.isEmpty() && ((Call) o).args.get(0) instanceof Attribute
                && ((Attribute) ((Call) o).args.get(0)).value instanceof Name
                && n.equals(((Attribute) ((Call) o).args.get(0)).attr)
                && isOptionsHelper(w, path, ((Name) ((Call) o).func).id)) {
            return false;
        }
        if (o instanceof ClassDef) {
            ClassDef d = (ClassDef) o;
            for (Expr b : d.bases) {
                if (!(b instanceof Name) && classEscapes(w, path, b, n)) {
                    return true;
                }
            }
            for (Expr x : d.decorators) {
                if (classEscapes(w, path, x, n)) {
                    return true;
                }
            }
            for (Node.Keyword kw : d.keywords) {
                if (classEscapes(w, path, kw, n)) {
                    return true;
                }
            }
            for (Stmt s : d.body) {
                if (classEscapes(w, path, s, n)) {
                    return true;
                }
            }
            return false;
        }
        if (o instanceof Name && n.equals(((Name) o).id)
                || o instanceof Attribute && n.equals(((Attribute) o).attr)
                || o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR && n.equals(((Constant) o).text)
                || o instanceof Node.Alias && (n.equals(((Node.Alias) o).name) || n.equals(((Node.Alias) o).asname))
                || o instanceof FunctionDef && n.equals(((FunctionDef) o).name)) {
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && classEscapes(w, path, x, n)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// Finds the nested class `n` that class `k` (or a base, through single
    /// inheritance) defines: found[0] the nested class, found[1] the class
    /// whose body holds it.
    private static boolean nestedClass(Cls k, String n, ClassDef[] found, int depth) {
        if (depth > MAX_DEPTH || k.opaque || hasMeta(k, 0) || !k.def.decorators.isEmpty()) {
            return false;
        }
        for (Stmt s : k.def.body) {
            if (s instanceof ClassDef && n.equals(((ClassDef) s).name)) {
                if (found[0] != null) {
                    return false;
                }
                found[0] = (ClassDef) s;
                found[1] = k.def;
            } else if (s instanceof FunctionDef && n.equals(((FunctionDef) s).name)
                    || !(s instanceof FunctionDef) && !(s instanceof ClassDef) && binds(s, n)) {
                return false;
            }
        }
        if (found[0] != null) {
            return true;
        }
        int declared = 0;
        for (Expr b : k.def.bases) {
            if (!(b instanceof Name && "object".equals(((Name) b).id))) {
                declared++;
            }
        }
        if (declared != k.bases.size() || k.bases.size() != 1) {
            return false;
        }
        return nestedClass(k.bases.get(0), n, found, depth + 1);
    }

    /// Adds the names nested class `d` (held in `outer`) and its nested base
    /// chain bind; false when anything else is in a body.
    private static boolean classKeys(World w, ClassDef outer, ClassDef d, List<String> keys, Set<Cls> chain,
            int depth) {
        if (depth > MAX_DEPTH || !d.decorators.isEmpty() || !d.keywords.isEmpty() || d.bases.size() > 1) {
            return false;
        }
        Cls dc = w.byDef.get(d);
        if (dc == null || dc.unlinked) {
            return false;
        }
        chain.add(dc);
        for (Stmt s : d.body) {
            if (s instanceof Node.Pass) {
                continue;
            }
            if (s instanceof Node.ExprStmt && ((Node.ExprStmt) s).value instanceof Constant) {
                continue;
            }
            if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                    && ((Node.Assign) s).targets.get(0) instanceof Name) {
                String key = ((Name) ((Node.Assign) s).targets.get(0)).id;
                if (!key.startsWith("_")) {
                    // The helper drops the private names.
                    keys.add(key);
                }
            } else {
                return false;
            }
        }
        if (d.bases.isEmpty() || d.bases.get(0) instanceof Name && "object".equals(((Name) d.bases.get(0)).id)) {
            return true;
        }
        if (!(d.bases.get(0) instanceof Name)) {
            return false;
        }
        String bn = ((Name) d.bases.get(0)).id;
        ClassDef base = null;
        for (Stmt s : outer.body) {
            if (s instanceof ClassDef && bn.equals(((ClassDef) s).name)) {
                if (base != null) {
                    return false;
                }
                base = (ClassDef) s;
            } else if (!(s instanceof FunctionDef) && binds(s, bn)) {
                return false;
            }
        }
        if (base == null || base == d || dc.bases.size() != 1 //NOPMD CompareObjectsWithEquals - identity is meant
                || dc.bases.get(0).def != base) { //NOPMD CompareObjectsWithEquals - identity is meant
            return false;
        }
        return classKeys(w, outer, base, keys, chain, depth + 1);
    }

    /// `src` with the one-line call `call` written as a dict display.
    private static String optionsDictText(String src, int line0, Call call, List<String> keys) {
        String fn = ((Name) call.func).id;
        List<Token> toks = new Lexer(src, "<unroll>").tokenize();
        int rel = call.line - line0 + 1;
        int at = -1;
        int close = -1;
        for (int i = 0; i + 1 < toks.size(); i++) {
            Token k = toks.get(i);
            if (k.line != rel || k.kind != Token.Kind.NAME || !k.text.equals(fn) || !toks.get(i + 1).is("(")
                    || i > 0 && toks.get(i - 1).is(".")) {
                continue;
            }
            if (at >= 0) {
                return null;
            }
            int depth = 0;
            for (int j = i + 1; j < toks.size(); j++) {
                Token q = toks.get(j);
                if (q.is("[") || q.is("(") || q.is("{")) {
                    depth++;
                } else if (q.is("]") || q.is(")") || q.is("}")) {
                    depth--;
                    if (depth == 0) {
                        close = j;
                        break;
                    }
                }
            }
            at = i;
        }
        if (at < 0 || close < 0 || toks.get(close).line != rel) {
            return null;
        }
        int[] starts = ConstArgSpecialize.lineStarts(src);
        int s0 = ConstArgSpecialize.off(starts, toks.get(at));
        int a0 = ConstArgSpecialize.off(starts, toks.get(at + 2));
        int s1 = ConstArgSpecialize.off(starts, toks.get(close));
        String arg = src.substring(a0, s1).trim();
        StringBuilder d = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                d.append(", ");
            }
            d.append('"').append(keys.get(i)).append("\": ").append(arg).append('.').append(keys.get(i));
        }
        d.append('}');
        return src.substring(0, s0) + d + src.substring(s1 + 1);
    }

    private static void findLoop(final World w, final String path, final Cls k, final String recv,
            final FunctionDef f, Object o, final Object[] hit) {
        if (hit[0] != null) {
            return;
        }
        if (o instanceof Node.For) {
            Node.For loop = (Node.For) o;
            List<String> names = qualifies(w, path, k, recv, f, loop);
            if (names != null) {
                hit[0] = loop;
                hit[1] = names;
                hit[2] = w.needGuard == null ? null
                        : guardText(((Name) ((Attribute) loop.iter).value).id, w.needGuard, names);
                return;
            }
        }
        if (o instanceof Node.Lambda || o instanceof ClassDef
                || o instanceof FunctionDef && o != f) { //NOPMD CompareObjectsWithEquals - identity is meant
            return;
        }
        Infer.children(o, x -> findLoop(w, path, k, recv, f, x, hit));
    }

    private static List<String> qualifies(World w, String path, Cls k, String recv, FunctionDef f, Node.For loop) {
        w.needGuard = null;
        if (loop.isAsync || !loop.orelse.isEmpty() || !(loop.target instanceof Name)) {
            return null;
        }
        String v = ((Name) loop.target).id;
        if (!namesAttribute(loop.body, v) || ownJump(loop.body, true) || badUse(loop.body, v)) {
            return null;
        }
        if (count(f, v) != count(loop.body, v) + 1) {
            return null;
        }
        if (loop.iter instanceof Name) {
            List<String> local = localConst(w, path, k, recv, f, ((Name) loop.iter).id);
            if (local != null) {
                return local;
            }
        }
        w.needGuard = null;
        List<String> out = names(w, path, k, recv, loop.iter, true, 0);
        if (out != null && w.needGuard != null && !(loop.iter instanceof Attribute
                && ((Attribute) loop.iter).value instanceof Name && ((Attribute) loop.iter).attr.equals(w.needGuard))) {
            return null;
        }
        return out;
    }

    /// The names a function local evaluates to when the function binds it
    /// exactly once, at the top of its body, to a literal list or tuple of
    /// identifiers, and reads it nowhere but as the loop's iterable
    /// (`names = ['top', 'left']` then `for name in names:`). Nothing else
    /// can reach the local, so the literal is the only value it holds.
    private static List<String> localConst(World w, String path, Cls k, String recv, FunctionDef f, String n) {
        if (count(f, n) != 2 || declaresGlobal(f, n)) {
            return null;
        }
        for (Stmt s : f.body) {
            if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                    && ((Node.Assign) s).targets.get(0) instanceof Name
                    && n.equals(((Name) ((Node.Assign) s).targets.get(0)).id)) {
                Expr v = fold(((Node.Assign) s).value);
                if (v instanceof Node.Attribute && w != null) {
                    // `props = self.cached_properties`: a class-level
                    // sequence, resolved per class as the loop's own would be.
                    w.needGuard = null;
                    List<String> r = names(w, path, k, recv, v, false, 0);
                    return w.needGuard == null ? r : null;
                }
                if (!(v instanceof Node.TupleExpr || v instanceof Node.ListExpr)) {
                    return null;
                }
                return names(null, null, null, null, v, true, 0);
            }
        }
        return null;
    }

    /// The names `e` evaluates to, every one an identifier, or null.
    private static List<String> names(World w, String path, Cls k, String recv, Expr e, boolean inline, int depth) {
        if (depth > MAX_DEPTH) {
            return null;
        }
        if (e instanceof Node.TupleExpr || inline && e instanceof Node.ListExpr) {
            List<Expr> elts = e instanceof Node.TupleExpr ? ((Node.TupleExpr) e).elts : ((Node.ListExpr) e).elts;
            List<String> out = new ArrayList<String>();
            for (Expr x : elts) {
                if (!(x instanceof Constant) || ((Constant) x).kind != Constant.Kind.STR
                        || !ConstArgSpecialize.ident(((Constant) x).text)) {
                    return null;
                }
                out.add(((Constant) x).text);
            }
            return out;
        }
        if (e instanceof Node.BinOp && "+".equals(((Node.BinOp) e).op)) {
            // `+` makes a new sequence: an operand literal is as private as
            // the result is.
            List<String> a = names(w, path, k, recv, ((Node.BinOp) e).left, inline, depth + 1);
            List<String> b = a == null ? null : names(w, path, k, recv, ((Node.BinOp) e).right, inline, depth + 1);
            if (b == null) {
                return null;
            }
            List<String> out = new ArrayList<String>(a);
            out.addAll(b);
            return out;
        }
        if (e instanceof Attribute && ((Attribute) e).value instanceof Name) {
            String base = ((Name) ((Attribute) e).value).id;
            String attr = ((Attribute) e).attr;
            if (w.storedAttrs.contains(attr)) {
                return null;
            }
            Set<Cls> blocked = w.blocked.get(attr);
            if (recv != null && recv.equals(base)) {
                if (k == null || blocked != null && blocked.contains(k)) {
                    return null;
                }
                if (w.guarded.contains(attr)) {
                    w.needGuard = attr;
                }
                return classAttr(w, k, attr, depth + 1);
            }
            Cls c = find(w, base, path);
            if (c == null || c == AMBIGUOUS || blocked != null && blocked.contains(c)) {
                return null;
            }
            return classAttr(w, c, attr, depth + 1);
        }
        if (e instanceof Name) {
            return moduleConst(w, path, ((Name) e).id, depth + 1);
        }
        return null;
    }

    private static List<String> moduleConst(World w, String path, String n, int depth) {
        Node.Module m = w.trees.get(path);
        if (m == null || declaresGlobal(m, n)) {
            return null;
        }
        Expr value = null;
        int stores = 0;
        for (Stmt s : m.body) {
            if (binds(s, n)) {
                stores++;
                if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                        && ((Node.Assign) s).targets.get(0) instanceof Name) {
                    value = ((Node.Assign) s).value;
                } else {
                    return null;
                }
            }
        }
        if (stores != 1 || value == null) {
            return null;
        }
        return names(w, path, null, null, value, false, depth);
    }

    private static boolean declaresGlobal(Object o, final String n) {
        if (o instanceof Node.Global && ((Node.Global) o).names.contains(n)) {
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && declaresGlobal(x, n)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// `attr` looked up on class `c` as a class-level tuple, or null.
    private static List<String> classAttr(World w, Cls c, String attr, int depth) {
        if (depth > MAX_DEPTH || c.opaque) {
            return null;
        }
        Expr own = null;
        int stores = 0;
        for (Stmt s : c.def.body) {
            if (s instanceof Node.Assign) {
                for (Expr t : ((Node.Assign) s).targets) {
                    if (mentionsName(t, attr)) {
                        stores++;
                        own = ((Node.Assign) s).targets.size() == 1 && t instanceof Name ? ((Node.Assign) s).value
                                : null;
                    }
                }
            } else if (s instanceof Node.AnnAssign && ((Node.AnnAssign) s).target instanceof Name
                    && attr.equals(((Name) ((Node.AnnAssign) s).target).id)) {
                stores++;
                own = ((Node.AnnAssign) s).value;
            } else if (s instanceof FunctionDef && attr.equals(((FunctionDef) s).name)
                    || !(s instanceof FunctionDef) && !(s instanceof Node.Assign) && !(s instanceof Node.AnnAssign)
                    && binds(s, attr)) {
                return null;
            }
        }
        if (stores > 1) {
            return null;
        }
        if (stores == 1) {
            return own == null ? null : classBodyNames(w, c, own, depth + 1, frozen(w, attr));
        }
        List<String> found = null;
        if (c.bases.isEmpty() || hasMeta(c, 0)) {
            // A metaclass computes what a class body leaves out, per class.
            return null;
        }
        if (c.bases.size() != c.def.bases.size()) {
            // A base outside the program might define it.
            return null;
        }
        for (Cls b : c.bases) {
            List<String> r = classAttr(w, b, attr, depth + 1);
            if (r == null || found != null && !found.equals(r)) {
                return null;
            }
            found = r;
        }
        return found;
    }

    /// An expression of a class body: names bound earlier in it resolve
    /// there before the module.
    private static List<String> classBodyNames(World w, Cls c, Expr e, int depth, boolean lists) {
        if (e instanceof Name && definesAttr(c.def, ((Name) e).id)) {
            return classAttr(w, c, ((Name) e).id, depth);
        }
        if (e instanceof Node.BinOp && "+".equals(((Node.BinOp) e).op)) {
            List<String> a = classBodyNames(w, c, ((Node.BinOp) e).left, depth + 1, lists);
            List<String> b = a == null ? null : classBodyNames(w, c, ((Node.BinOp) e).right, depth + 1, lists);
            if (b == null) {
                return null;
            }
            List<String> out = new ArrayList<String>(a);
            out.addAll(b);
            return out;
        }
        return names(w, c.path, null, null, e, lists, depth);
    }

    /// `v` with a conditional whose test compares literals to `None` by
    /// identity decided (`A if None is None else None`, as
    /// ConstArgSpecialize writes a defaulted parameter in); `v` itself
    /// otherwise, or null when such a test cannot be decided.
    private static Expr fold(Expr v) {
        if (!(v instanceof Node.IfExp)) {
            return v;
        }
        Node.IfExp x = (Node.IfExp) v;
        if (!(x.test instanceof Node.Compare) || ((Node.Compare) x.test).ops.size() != 1) {
            return null;
        }
        Node.Compare c = (Node.Compare) x.test;
        String op = c.ops.get(0);
        if (!"is".equals(op) && !"is not".equals(op)) {
            return null;
        }
        Expr l = c.left;
        Expr r = c.comparators.get(0);
        boolean ln = isNone(l);
        boolean rn = isNone(r);
        if (!ln && !literalSeq(l) || !rn && !literalSeq(r) || !ln && !rn) {
            return null;
        }
        boolean same = ln && rn;
        return fold("is".equals(op) == same ? x.body : x.orelse);
    }

    private static boolean isNone(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.NONE;
    }

    private static boolean literalSeq(Expr e) {
        return e instanceof Node.TupleExpr || e instanceof Node.ListExpr;
    }

    /// Whether every list a class body binds to `attr` is provably never
    /// mutated and never handed to code that could: whole program, every
    /// read of `.attr` is a loop's iterable, an operand of `+` or of a
    /// comparison, the argument of a call in [World#exempt], or (bare or
    /// as a branch of a conditional) the value of a function local that is
    /// read only as a loop's iterable. The attribute is never stored or
    /// deleted outside its class bodies, never spelled as a string (a
    /// literal `getattr`), and a class body binding it reads it nowhere
    /// else. A list so used holds what its literal says, as a tuple would.
    private static boolean frozen(World w, String attr) {
        Boolean b = w.frozen.get(attr);
        if (b == null) {
            boolean[] ok = {!w.storedAttrs.contains(attr)};
            for (Node.Module m : w.trees.values()) {
                for (Stmt s : m.body) {
                    frozenScan(w, attr, s, null, ok);
                }
            }
            b = Boolean.valueOf(ok[0]);
            w.frozen.put(attr, b);
        }
        return b.booleanValue();
    }

    private static boolean isRead(Object o, String attr) {
        return o instanceof Attribute && attr.equals(((Attribute) o).attr);
    }

    /// Scans `e`, a position where a read of `attr` is harmless: such a
    /// read has only its receiver scanned.
    private static void frozenSafe(World w, String attr, Expr e, FunctionDef fn, boolean[] ok) {
        frozenScan(w, attr, isRead(e, attr) ? ((Attribute) e).value : e, fn, ok);
    }

    private static void frozenScan(final World w, final String attr, Object o, FunctionDef fn, final boolean[] ok) {
        if (!ok[0] || o == null) {
            return;
        }
        if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR && attr.equals(((Constant) o).text)
                || isRead(o, attr)) {
            ok[0] = false;
            return;
        }
        if (o instanceof Node.For) {
            Node.For f = (Node.For) o;
            frozenSafe(w, attr, f.iter, fn, ok);
            frozenScan(w, attr, f.target, fn, ok);
            for (Stmt s : f.body) {
                frozenScan(w, attr, s, fn, ok);
            }
            for (Stmt s : f.orelse) {
                frozenScan(w, attr, s, fn, ok);
            }
            return;
        }
        if (o instanceof Node.BinOp && "+".equals(((Node.BinOp) o).op)) {
            frozenSafe(w, attr, ((Node.BinOp) o).left, fn, ok);
            frozenSafe(w, attr, ((Node.BinOp) o).right, fn, ok);
            return;
        }
        if (o instanceof Node.Compare) {
            frozenSafe(w, attr, ((Node.Compare) o).left, fn, ok);
            for (Expr x : ((Node.Compare) o).comparators) {
                frozenSafe(w, attr, x, fn, ok);
            }
            return;
        }
        if (o instanceof Call && w.exempt.contains(o)) {
            frozenScan(w, attr, ((Call) o).func, fn, ok);
            for (Expr x : ((Call) o).args) {
                frozenSafe(w, attr, x, fn, ok);
            }
            for (Node.Keyword k : ((Call) o).keywords) {
                frozenScan(w, attr, k, fn, ok);
            }
            return;
        }
        if (o instanceof Node.Assign && fn != null && ((Node.Assign) o).targets.size() == 1
                && ((Node.Assign) o).targets.get(0) instanceof Name
                && onlyIterated(fn, ((Name) ((Node.Assign) o).targets.get(0)).id)) {
            Expr v = ((Node.Assign) o).value;
            if (v instanceof Node.IfExp) {
                frozenScan(w, attr, ((Node.IfExp) v).test, fn, ok);
                frozenSafe(w, attr, ((Node.IfExp) v).body, fn, ok);
                frozenSafe(w, attr, ((Node.IfExp) v).orelse, fn, ok);
            } else {
                frozenSafe(w, attr, v, fn, ok);
            }
            return;
        }
        if (o instanceof ClassDef) {
            for (Stmt s : ((ClassDef) o).body) {
                if (s instanceof FunctionDef) {
                    continue;
                }
                Expr own = null;
                if (s instanceof Node.Assign && ((Node.Assign) s).targets.size() == 1
                        && ((Node.Assign) s).targets.get(0) instanceof Name
                        && attr.equals(((Name) ((Node.Assign) s).targets.get(0)).id)) {
                    own = ((Node.Assign) s).value;
                } else if (s instanceof Node.AnnAssign && ((Node.AnnAssign) s).target instanceof Name
                        && attr.equals(((Name) ((Node.AnnAssign) s).target).id)) {
                    own = ((Node.AnnAssign) s).value;
                }
                if (own != null ? count(own, attr) > 0 : count(s, attr) > 0) {
                    ok[0] = false;
                    return;
                }
            }
        }
        final FunctionDef in = o instanceof FunctionDef ? (FunctionDef) o : fn;
        Infer.children(o, x -> frozenScan(w, attr, x, in, ok));
    }

    /// Whether local `n` of `fn` is bound once and read, if at all, only
    /// as the iterable of a loop.
    private static boolean onlyIterated(FunctionDef fn, final String n) {
        if (declaresGlobal(fn, n)) {
            return false;
        }
        final int[] iters = {0};
        iterUses(fn, n, iters);
        return count(fn, n) == 1 + iters[0] && iters[0] <= 1;
    }

    private static void iterUses(Object o, final String n, final int[] iters) {
        if (o instanceof Node.For && ((Node.For) o).iter instanceof Name && n.equals(((Name) ((Node.For) o).iter).id)) {
            iters[0]++;
        }
        Infer.children(o, x -> iterUses(x, n, iters));
    }

    private static final Set<String> PORTABLE_BUILTINS = new HashSet<String>(java.util.Arrays.asList(
            "hasattr", "delattr", "getattr", "setattr", "isinstance", "len", "None", "True", "False"));

    /// Whether `src`, a method of module `from`, means the same in module
    /// `to`: it reads no name but its parameters, its own plain locals and
    /// builtins neither module rebinds, and opens no nested scope.
    private static boolean portable(World w, String from, String to, String src) {
        FunctionDef f = ConstArgSpecialize.parseMethod(src, 1, to);
        final Set<String> local = new HashSet<String>();
        for (Node.Arg a : f.args.positional) {
            local.add(a.name);
        }
        if (f.args.vararg != null || f.args.kwarg != null || !f.args.kwonly.isEmpty() || !f.decorators.isEmpty()) {
            return false;
        }
        final Set<String> used = new HashSet<String>();
        final boolean[] ok = {true};
        for (Stmt s : f.body) {
            portableScan(s, local, used, ok);
        }
        if (!ok[0]) {
            return false;
        }
        for (String n : used) {
            if (local.contains(n)) {
                continue;
            }
            if (!PORTABLE_BUILTINS.contains(n)) {
                return false;
            }
            for (String p : new String[] {from, to}) {
                Node.Module m = w.trees.get(p);
                if (m == null) {
                    return false;
                }
                for (Stmt s : m.body) {
                    if (binds(s, n)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void portableScan(Object o, final Set<String> local, final Set<String> used, final boolean[] ok) {
        if (o instanceof Node.Lambda || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Comp
                || o instanceof Node.Global) {
            ok[0] = false;
            return;
        }
        if (o instanceof Node.Assign) {
            for (Expr t : ((Node.Assign) o).targets) {
                if (t instanceof Name) {
                    local.add(((Name) t).id);
                }
            }
        } else if (o instanceof Node.For && ((Node.For) o).target instanceof Name) {
            local.add(((Name) ((Node.For) o).target).id);
        }
        if (o instanceof Name) {
            used.add(((Name) o).id);
        }
        Infer.children(o, x -> portableScan(x, local, used, ok));
    }

    private static boolean definesAttr(ClassDef d, String n) {
        for (Stmt s : d.body) {
            if (!(s instanceof FunctionDef) && binds(s, n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMeta(Cls c, int depth) {
        if (depth > MAX_DEPTH || !c.def.keywords.isEmpty()) {
            return true;
        }
        for (Cls b : c.bases) {
            if (hasMeta(b, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentionsName(Object o, String n) {
        return count(o, n) > 0;
    }

    /// True when `o` binds `n` in the scope it is in.
    private static boolean binds(Object o, final String n) {
        if (bindsHere(o, n)) {
            return true;
        }
        if (o instanceof Node.Lambda || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Comp) {
            return false;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && binds(x, n)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// True when the node `o` itself binds `n`.
    private static boolean bindsHere(Object o, String n) {
        List<Object> targets = new ArrayList<Object>();
        if (o instanceof Node.Assign) {
            targets.addAll(((Node.Assign) o).targets);
        } else if (o instanceof Node.AugAssign) {
            targets.add(((Node.AugAssign) o).target);
        } else if (o instanceof Node.AnnAssign) {
            targets.add(((Node.AnnAssign) o).target);
        } else if (o instanceof Node.For) {
            targets.add(((Node.For) o).target);
        } else if (o instanceof Node.WithItem) {
            targets.add(((Node.WithItem) o).vars);
        } else if (o instanceof Node.NamedExpr) {
            targets.add(((Node.NamedExpr) o).target);
        } else if (o instanceof Node.Delete) {
            targets.addAll(((Node.Delete) o).targets);
        } else if (o instanceof Node.ExceptHandler) {
            return n.equals(((Node.ExceptHandler) o).name);
        } else if (o instanceof FunctionDef) {
            return n.equals(((FunctionDef) o).name);
        } else if (o instanceof ClassDef) {
            return n.equals(((ClassDef) o).name);
        } else if (o instanceof Node.Alias) {
            Node.Alias a = (Node.Alias) o;
            String b = a.asname != null ? a.asname : a.name;
            return b != null && (n.equals(b) || b.startsWith(n + "."));
        }
        for (Object t : targets) {
            if (t != null && storesName(t, n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean storesName(Object t, final String n) {
        if (t instanceof Name) {
            return n.equals(((Name) t).id);
        }
        if (t instanceof Node.TupleExpr || t instanceof Node.ListExpr || t instanceof Node.Starred) {
            final boolean[] found = {false};
            Infer.children(t, x -> {
                if (!found[0] && storesName(x, n)) {
                    found[0] = true;
                }
            });
            return found[0];
        }
        return false;
    }

    // ---- the loop body ----

    /// True when `p` is the name argument of a getattr/setattr/hasattr/delattr in `o`.
    private static boolean namesAttribute(Object o, final String p) {
        if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Name && c.args.size() >= 2 && foldsTo(c.args.get(1), p)) {
                String n = ((Name) c.func).id;
                if ("getattr".equals(n) || "setattr".equals(n) || "hasattr".equals(n) || "delattr".equals(n)) {
                    return true;
                }
            }
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && namesAttribute(x, p)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// Whether `e` is the loop variable `p`, or a pure string transform of
    /// it that [#unrollText] folds to a literal once the name is written
    /// in: `p.lower()`, `p.upper()`, `p + "s"` or `"s" + p`, with a plain
    /// literal operand.
    private static boolean foldsTo(Expr e, String p) {
        if (e instanceof Name) {
            return p.equals(((Name) e).id);
        }
        if (e instanceof Call) {
            Call c = (Call) e;
            return c.args.isEmpty() && c.keywords.isEmpty() && c.func instanceof Attribute
                    && ("lower".equals(((Attribute) c.func).attr) || "upper".equals(((Attribute) c.func).attr))
                    && ((Attribute) c.func).value instanceof Name && p.equals(((Name) ((Attribute) c.func).value).id);
        }
        if (e instanceof Node.BinOp && "+".equals(((Node.BinOp) e).op)) {
            Expr l = ((Node.BinOp) e).left;
            Expr r = ((Node.BinOp) e).right;
            return l instanceof Name && p.equals(((Name) l).id) && identSuffix(r)
                    || r instanceof Name && p.equals(((Name) r).id) && identSuffix(l);
        }
        return false;
    }

    private static boolean identSuffix(Expr e) {
        if (!(e instanceof Constant) || ((Constant) e).kind != Constant.Kind.STR) {
            return false;
        }
        String t = ((Constant) e).text;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (!(ch == '_' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9')) {
                return false;
            }
        }
        return true;
    }

    /// Whether `o` holds a `break` (with `breaks`) or a `continue` (without)
    /// of the loop whose body it is: a jump inside a nested loop or scope
    /// belongs to that one. A `continue` is lowered by [#unrollText]; a
    /// `break` refuses the loop.
    private static boolean ownJump(Object o, boolean breaks) {
        if (breaks ? o instanceof Node.Break : o instanceof Node.Continue) {
            return true;
        }
        if (o instanceof Node.For || o instanceof Node.While || o instanceof FunctionDef || o instanceof ClassDef
                || o instanceof Node.Lambda) {
            return false;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && ownJump(x, breaks)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// True when `v` is rebound in `o`, or used inside a nested scope.
    private static boolean badUse(Object o, final String v) {
        if (eagerComp(o, v)) {
            // A list, set or dict comprehension runs to completion where it
            // stands, inside the iteration, so reading the loop variable in
            // it sees exactly the name written in (a generator would run
            // later and is refused below).
            final boolean[] bad = {false};
            Infer.children(o, x -> bad[0] |= !bad[0] && badUse(x, v));
            return bad[0];
        }
        if (o instanceof Node.Lambda || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Comp
                || o instanceof Node.Global || o instanceof Node.Match) {
            return count(o, v) > 0 || o instanceof Node.Global && ((Node.Global) o).names.contains(v)
                    || o instanceof FunctionDef && v.equals(((FunctionDef) o).name)
                    || o instanceof ClassDef && v.equals(((ClassDef) o).name);
        }
        if (bindsHere(o, v)) {
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && badUse(x, v)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// Whether `o` is a non-generator, non-async comprehension none of
    /// whose targets binds `v`.
    private static boolean eagerComp(Object o, String v) {
        if (!(o instanceof Node.Comp) || ((Node.Comp) o).kind == Node.Comp.Kind.GENERATOR) {
            return false;
        }
        for (Node.Comprehension g : ((Node.Comp) o).generators) {
            if (g.isAsync || count(g.target, v) > 0) {
                return false;
            }
        }
        return true;
    }

    private static int count(Object o, final String v) {
        final int[] n = {o instanceof Name && v.equals(((Name) o).id) ? 1 : 0};
        Infer.children(o, x -> n[0] += count(x, v));
        return n[0];
    }

    // ---- the text ----

    /// `src` with `loop` replaced by one copy of its body per name, or null.
    private static String unrollText(String src, int line0, Node.For loop, List<String> names, String guard) {
        String v = ((Name) loop.target).id;
        List<Token> toks = new Lexer(src, "<unroll>").tokenize();
        int rel = loop.line - line0 + 1;
        int start = -1;
        for (int i = 1; i < toks.size(); i++) {
            Token k = toks.get(i);
            Token prev = toks.get(i - 1);
            if (k.line == rel && k.is("for") && (prev.kind == Token.Kind.NEWLINE || prev.kind == Token.Kind.INDENT
                    || prev.kind == Token.Kind.DEDENT)) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return null;
        }
        // The header ends at the first NEWLINE; the body must be an indented block.
        int nl = -1;
        for (int i = start; i < toks.size(); i++) {
            if (toks.get(i).kind == Token.Kind.NEWLINE) {
                nl = i;
                break;
            }
        }
        if (nl < 0 || nl + 1 >= toks.size() || toks.get(nl + 1).kind != Token.Kind.INDENT
                || !toks.get(nl - 1).is(":")) {
            return null;
        }
        int depth = 0;
        int end = -1;
        int last = -1;
        for (int i = nl + 1; i < toks.size(); i++) {
            Token k = toks.get(i);
            if (k.kind == Token.Kind.INDENT) {
                depth++;
            } else if (k.kind == Token.Kind.DEDENT) {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            } else if (k.kind == Token.Kind.EOF) {
                end = i;
                break;
            } else if (k.kind != Token.Kind.NEWLINE) {
                if (k.kind == Token.Kind.STRING && ConstArgSpecialize.count(k.text, '\n') > 0) {
                    return null;
                }
                if (k.kind == Token.Kind.STRING && k.prefix != null && k.prefix.indexOf('f') >= 0
                        && k.text.contains(v)) {
                    return null;
                }
                last = Math.max(last, k.line);
            }
        }
        if (end < 0 || last < 0) {
            return null;
        }
        Token first = toks.get(nl + 2);
        int forInd = toks.get(start).col - 1;
        int bodyInd = first.col - 1;
        int bodyFirst = first.line;
        int headerLast = toks.get(nl).line;
        int[] starts = ConstArgSpecialize.lineStarts(src);
        String[] lines = src.split("\n", -1);
        if (last > lines.length || bodyFirst <= headerLast) {
            return null;
        }
        // Only blank and comment lines may sit between the header and the
        // first statement; they are dropped from the copies.
        for (int l = headerLast + 1; l < bodyFirst; l++) {
            String s = lines[l - 1].trim();
            if (!s.isEmpty() && s.charAt(0) != '#') {
                return null;
            }
        }
        StringBuilder out = new StringBuilder();
        for (int l = 1; l < rel; l++) {
            out.append(lines[l - 1]).append('\n');
        }
        String pad = spaces(forInd);
        // A `continue` skips the rest of one name's copy: each copy then runs
        // as the body of a loop over one element, where `continue` means
        // exactly that and nothing else changes (a `break` of the unrolled
        // loop was refused, [#qualifies]).
        boolean once = false;
        for (Stmt s : loop.body) {
            once |= ownJump(s, false);
        }
        String copyPad = once ? pad + "    " : pad;
        if (guard != null) {
            out.append(pad).append(guard).append('\n');
        }
        for (String name : names) {
            if (once) {
                out.append(pad).append("for _cn1_once in (0,):\n");
            }
            StringBuilder b = new StringBuilder(src);
            for (int i = end - 1; i > nl; i--) {
                Token k = toks.get(i);
                if (k.kind != Token.Kind.NAME || !k.text.equals(v)) {
                    continue;
                }
                if (toks.get(i - 1).is(".")) {
                    continue;
                }
                if (toks.get(i + 1).is("=")) {
                    // A keyword argument named like the variable.
                    return null;
                }
                int o = ConstArgSpecialize.off(starts, k);
                int e = o + v.length();
                String lit = name;
                // Fold the pure string transforms [#foldsTo] accepts, so the
                // attribute name reaches the strict build as one literal.
                if (i + 4 < end && toks.get(i + 1).is(".") && toks.get(i + 3).is("(")
                        && toks.get(i + 4).is(")")
                        && (toks.get(i + 2).is("lower") || toks.get(i + 2).is("upper"))) {
                    lit = asciiCase(name, toks.get(i + 2).is("upper"));
                    e = ConstArgSpecialize.off(starts, toks.get(i + 4)) + 1;
                } else if (i + 3 < end && toks.get(i + 1).is("+") && plainString(src, starts, toks.get(i + 2))
                        && concatEdge(toks.get(i - 1)) && concatEdge(toks.get(i + 3))) {
                    Token r = toks.get(i + 2);
                    int ro = ConstArgSpecialize.off(starts, r);
                    int re = src.indexOf(src.charAt(ro), ro + 1) + 1;
                    lit = name + src.substring(ro + 1, re - 1);
                    e = re;
                } else if (i - 3 > nl && toks.get(i - 1).is("+") && plainString(src, starts, toks.get(i - 2))
                        && concatEdge(toks.get(i - 3)) && concatEdge(toks.get(i + 1))) {
                    Token l = toks.get(i - 2);
                    int lo = ConstArgSpecialize.off(starts, l);
                    lit = src.substring(lo + 1, src.indexOf(src.charAt(lo), lo + 1)) + name;
                    o = lo;
                    i -= 2;
                }
                b.replace(o, e, "\"" + lit + "\"");
            }
            String[] sub = b.toString().split("\n", -1);
            for (int l = bodyFirst; l <= last; l++) {
                String s = sub[l - 1];
                if (s.trim().isEmpty()) {
                    out.append('\n');
                    continue;
                }
                int lead = 0;
                while (lead < s.length() && s.charAt(lead) == ' ') {
                    lead++;
                }
                if (lead < bodyInd || lead < s.length() && s.charAt(lead) == '\t') {
                    return null;
                }
                out.append(copyPad).append(s.substring(bodyInd)).append('\n');
            }
        }
        if (names.isEmpty()) {
            out.append(pad).append("pass\n");
        }
        for (int l = last + 1; l <= lines.length; l++) {
            out.append(lines[l - 1]);
            if (l < lines.length) {
                out.append('\n');
            }
        }
        return out.toString();
    }

    /// Whether `t` is a one-line string literal with no prefix and no
    /// escape whose text is identifier characters, as [#identSuffix] reads it.
    private static boolean plainString(String src, int[] starts, Token t) {
        if (t.kind != Token.Kind.STRING || t.prefix != null && !t.prefix.isEmpty()) {
            return false;
        }
        int o = ConstArgSpecialize.off(starts, t);
        char q = src.charAt(o);
        if (q != '"' && q != '\'') {
            return false;
        }
        int c = src.indexOf(q, o + 1);
        if (c < 0 || c + 1 < src.length() && src.charAt(c + 1) == q && c == o + 1) {
            return false;
        }
        for (int j = o + 1; j < c; j++) {
            char ch = src.charAt(j);
            if (!(ch == '_' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9')) {
                return false;
            }
        }
        return true;
    }

    /// Whether a token beside `a + "s"` binds no tighter than `+`, so the
    /// concatenation is one operand that a literal can replace.
    private static boolean concatEdge(Token t) {
        return t.kind == Token.Kind.NEWLINE || t.is("(") || t.is(")") || t.is(",") || t.is("[") || t.is("]")
                || t.is("=") || t.is(":") || t.is("+");
    }

    /// ASCII case mapping; names are identifiers, so no locale applies.
    private static String asciiCase(String s, boolean upper) {
        StringBuilder b = new StringBuilder(s.length());
        for (int j = 0; j < s.length(); j++) {
            char ch = s.charAt(j);
            if (upper && ch >= 'a' && ch <= 'z') {
                ch = (char) (ch - 32);
            } else if (!upper && ch >= 'A' && ch <= 'Z') {
                ch = (char) (ch + 32);
            }
            b.append(ch);
        }
        return b.toString();
    }

    private static String spaces(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(' ');
        }
        return b.toString();
    }
}
