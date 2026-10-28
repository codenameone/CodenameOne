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
import com.codename1.python.transpiler.ast.Scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/// The typed half of a closed program: a second, statically typed Java
/// method for every function `Infer` typed whose body this class can lower.
///
/// Such a method keeps each value in the representation its type gives (see
/// `Ty`), reads and writes typed fields directly, and calls other typed
/// methods directly -- statically when one method can run, through a Java
/// virtual method (`tv_`) when several can. Nothing in it checks a type the
/// inference proved. The generic version of the function stays, and is
/// where every call from generic code lands: its first statement tests the
/// arguments against the typed signature and enters the typed method when
/// they fit, which in a closed program is always.
///
/// A function whose body uses anything this class does not lower keeps
/// only its generic version, and so does every typed function that would
/// call it: the set of typed functions is a fixpoint.
final class TypedEmit {
    /// Thrown for a construct the typed code does not lower.
    static final class Unsupported extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Unsupported(String why) {
            super(why, null, false, false);
        }
    }

    /// A lowered expression: Java source and the type it holds.
    static final class E {
        final String c;
        final Ty t;
        /// The same int as a Java `long` expression, cheap and free of side
        /// effects, when one is at hand (a loop counter, a constant); or null.
        final String lc;
        /// An int held as a pair (see `dual`): a Java `long` and the int
        /// object that replaces it when it is not null (a big int, or a
        /// bool). Both plain Java names; or both null.
        final String ll;
        final String bg;
        /// An int held as the bare `long` a typed function returned: a
        /// plain Java name whose `Long.MIN_VALUE` means the object is
        /// waiting in `Typed.take`, which `c` takes. Only the consumers that
        /// know to take it exactly once see one (see `pushed`); or null.
        String pk;

        E(String c, Ty t) {
            this(c, t, null);
        }

        E(String c, Ty t, String lc) {
            this(c, t, lc, null, null);
        }

        E(String c, Ty t, String lc, String ll, String bg) {
            this.c = c;
            this.t = t;
            this.lc = lc;
            this.ll = ll;
            this.bg = bg;
        }
    }

    /// Appends a copy of each entry of `m` to `out`: an entry set may hand
    /// out one reused entry object.
    private static <K, V> void copyEntries(Map<K, V> m, List<Map.Entry<K, V>> out) {
        for (Map.Entry<K, V> e : m.entrySet()) {
            out.add(new java.util.AbstractMap.SimpleImmutableEntry<K, V>(e.getKey(), e.getValue()));
        }
    }

    /// The int a typed function returned as `long` `r`, left as it is
    /// (see `E.pk`).
    static E pushed(String r) {
        E e = new E("Typed.unret(" + r + ")", Ty.INT);
        e.pk = r;
        return e;
    }

    /// An int pair `l`/`b` as an expression: the boxed value, built only
    /// when something needs the object.
    static E dual(String l, String b) {
        return new E("(" + b + " == null ? Long.valueOf(" + l + ") : " + b + ")", Ty.INT, null, l, b);
    }

    private final Emitter em;
    final Infer in;
    private final Layouts layouts;
    /// Fields held in a typed Java slot (`t_<name>`), per declaring class.
    final Map<Layouts.Cls, Map<String, Ty>> typedFields = new LinkedHashMap<Layouts.Cls, Map<String, Ty>>();
    /// The functions with a typed version, and its Java name.
    final Map<Infer.Fn, String> typed = new LinkedHashMap<Infer.Fn, String>();
    private final Map<Scope, Infer.Fn> byScope = new HashMap<Scope, Infer.Fn>();
    private final Set<Layouts.Cls> unboxers = new LinkedHashSet<Layouts.Cls>();
    private final Set<Layouts.Cls> constructors = new LinkedHashSet<Layouts.Cls>();
    /// `name/arity` -> the methods a `tv_` call may run.
    private final Map<String, List<Infer.Fn>> groups = new LinkedHashMap<String, List<Infer.Fn>>();
    private final Set<String> usedGroups = new LinkedHashSet<String>();
    private final Map<Infer.Fn, String> bodies = new LinkedHashMap<Infer.Fn, String>();

    // per function
    private Infer.Fn cur;
    private Scope sc;
    private DefAssign da;
    private Set<Infer.Fn> calls;
    private Set<String> callsGroups;
    private int tmp;
    /// Nullable locals known not to hold None here (flow-sensitive).
    private Set<String> nonNull = new HashSet<String>();
    /// Int locals whose value a Java long holds right now, and that long.
    private Map<String, String> shadow = new HashMap<String, String>();
    /// The int ranges of the function being emitted, and the locals they
    /// put in a Java `long` (see `IntRanges`).
    private IntRanges ranges;
    private Set<String> longLocals = new HashSet<String>();
    /// The module functions that also have a version taking their int
    /// parameters as Java longs (`TL_`), and the interval each such
    /// parameter is known to hold there; what that version returns, when
    /// it is bounded. See `planLongParams`.
    private Map<Infer.Fn, IntRanges.Iv[]> longParams = new HashMap<Infer.Fn, IntRanges.Iv[]>();
    private Map<Infer.Fn, IntRanges.Iv> longRets = new HashMap<Infer.Fn, IntRanges.Iv>();
    /// The expression a `return` of an int returns, which may lower to a
    /// pushed result (see `E.pk`); and the call a hoisted leaf is, likewise.
    private Expr pushTop;
    private Expr pushLeaf;
    /// See `top`; and whether the leaves being collected or evaluated are
    /// that expression's.
    private Expr hoistTop;
    private boolean topLeaves;
    private boolean leafImpureOk;
    private final Map<Infer.Fn, String> longBodies = new LinkedHashMap<Infer.Fn, String>();
    /// The argument intervals of the calls the function being emitted makes.
    private Map<Call, IntRanges.Iv[]> sites = new HashMap<Call, IntRanges.Iv[]>();
    /// The Java code of each parameter of the expression functions being
    /// inlined, innermost last, and whether it is the exact value (true) or
    /// only the value modulo 2^64.
    private final List<Map<String, String[]>> inlined = new ArrayList<Map<String, String[]>>();
    /// An expression `expr` must lower without trying `wrapped` first.
    private Expr plainExpr;
    /// Locals whose previous list a new one may be written into (see
    /// ListRecycling).
    private Set<String> recycled = new HashSet<String>();
    /// Already evaluated operands of hoisted arithmetic (see `hoistInt`):
    /// what `expr` answers for each instead.
    private Map<Expr, E> leafSubst = new IdentityHashMap<Expr, E>();
    /// The outlined slow paths of hoisted arithmetic.
    private final StringBuilder slow = new StringBuilder(); //NOPMD AvoidStringBufferField - the generator's output
    private int slowCount;
    TypedEmit(Emitter em, Infer in, Layouts layouts) {
        this.em = em;
        this.in = in;
        this.layouts = layouts;
    }

    // ---- planning --------------------------------------------------------

    /// Decides the typed fields and functions; call before the generic code
    /// is emitted, which must then leave the typed fields alone.
    /// Module functions that store into nothing but their own locals and
    /// call only such functions: a call of one cannot change a list or a
    /// field, so reads around it may move across it.
    private final Set<Infer.Fn> pure = new HashSet<Infer.Fn>();
    private static final Set<String> PURE_BUILTINS = new HashSet<String>(Arrays.asList("len", "float", "abs",
            "int", "bool", "range", "min", "max"));
    /// Every call in the statement being emitted is of a pure function.
    private boolean stmtPure;
    /// How many enclosing operands run only conditionally (`and`, `or`, an
    /// if-expression branch): nothing there may be hoisted to run always.
    private int condDepth;

    /// Methods and `__init__`s are pure on the same terms, but for
    /// `__init__` storing into the fields of the object it initializes:
    /// a new object nothing else can see yet.
    private void findPure() {
        for (Map.Entry<String, Infer.Fn> e : in.globalFns.entrySet()) {
            pure.add(e.getValue());
        }
        for (Infer.Fn f : in.fns.values()) {
            if (f.cls != null && !f.def.name.equals("__init__")) {
                pure.add(f);
            } else if (f.cls != null) {
                pureInits.add(f);
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Infer.Fn f : new ArrayList<Infer.Fn>(pure)) {
                if (!pureBody(f)) {
                    pure.remove(f);
                    changed = true;
                }
            }
            for (Infer.Fn f : new ArrayList<Infer.Fn>(pureInits)) {
                initSelf = f.names.isEmpty() ? null : f.names.get(0);
                try {
                    if (!pureBody(f)) {
                        pureInits.remove(f);
                        changed = true;
                    }
                } finally {
                    initSelf = null;
                }
            }
        }
    }

    private final Set<Infer.Fn> pureInits = new HashSet<Infer.Fn>();
    /// The `self` of the `__init__` `findPure` is checking, or null.
    private String initSelf;

    private boolean pureBody(Infer.Fn f) {
        if (f.def.args.vararg != null || f.def.args.kwarg != null || !f.def.decorators.isEmpty()
                || f.def.scope.hasYield || f.def.isAsync) {
            return false;
        }
        for (Stmt st : f.def.body) {
            if (!pureNode(st)) {
                return false;
            }
        }
        return true;
    }

    /// Methods of the built-in types that change their object: a call of
    /// one by name is never taken for a pure method of a class.
    private static final Set<String> MUTATORS = new HashSet<String>(Arrays.asList("append", "extend", "insert",
            "pop", "remove", "clear", "sort", "reverse", "update", "setdefault", "popitem", "add", "discard",
            "write", "send", "close", "put", "acquire", "release", "set", "throw", "__setitem__", "__delitem__",
            "__iadd__", "__init__"));

    /// Whether every method named `m` of every class is pure.
    private boolean pureMethod(String m) {
        if (MUTATORS.contains(m)) {
            return false;
        }
        boolean any = false;
        for (Infer.Fn f : in.fns.values()) {
            if (f.cls != null && f.def.name.equals(m)) {
                if (!pure.contains(f)) {
                    return false;
                }
                any = true;
            }
        }
        return any;
    }

    private boolean pureNode(Object o) {
        if (!(o instanceof com.codename1.python.transpiler.ast.Node)) {
            // An operator or a name in a node's list of strings.
            return true;
        }
        if (o instanceof Call) {
            if (!pureCall((Call) o, null)) {
                return false;
            }
        } else if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (!localTarget(t) && !(initSelf != null && t instanceof Attribute
                        && ((Attribute) t).value instanceof Name && ((Name) ((Attribute) t).value).id.equals(initSelf))) {
                    return false;
                }
            }
        } else if (o instanceof AugAssign) {
            if (!(((AugAssign) o).target instanceof Name)) {
                return false;
            }
        } else if (o instanceof For) {
            if (!localTarget(((For) o).target)) {
                return false;
            }
        } else if (!(o instanceof Name || o instanceof Constant || o instanceof BinOp || o instanceof UnaryOp
                || o instanceof BoolOp || o instanceof Compare || o instanceof IfExp || o instanceof Subscript
                || o instanceof Slice || o instanceof TupleExpr || o instanceof ListExpr || o instanceof Attribute
                || o instanceof ExprStmt || o instanceof Return || o instanceof Pass || o instanceof Break
                || o instanceof Continue || o instanceof If || o instanceof While || o instanceof Expr
                && !(o instanceof NamedExpr || o instanceof Lambda || o instanceof Comp || o instanceof Yield
                || o instanceof Await || o instanceof Starred))) {
            return false;
        }
        if (o instanceof Expr && !(o instanceof Name || o instanceof Constant || o instanceof BinOp
                || o instanceof UnaryOp || o instanceof BoolOp || o instanceof Compare || o instanceof IfExp
                || o instanceof Subscript || o instanceof Slice || o instanceof TupleExpr || o instanceof ListExpr
                || o instanceof Attribute || o instanceof Call)) {
            return false;
        }
        boolean[] ok = {true};
        Infer.children(o, x -> ok[0] &= pureNode(x));
        return ok[0];
    }

    private static boolean localTarget(Expr t) {
        if (t instanceof Name) {
            return true;
        }
        if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                if (!localTarget(e)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /// Whether `c` calls a pure function; `scope` resolves its name (null:
    /// the module's view, for `findPure`).
    private boolean pureCall(Call c, Scope scope) {
        if (!c.keywords.isEmpty()) {
            return false;
        }
        if (c.func instanceof Attribute) {
            Attribute a = (Attribute) c.func;
            if (in.isMath(a.value, scope == null ? em.moduleScope() : scope)) {
                return true;
            }
            return pureMethod(a.attr);
        }
        if (!(c.func instanceof Name)) {
            return false;
        }
        String n = ((Name) c.func).id;
        Scope.Binding b = scope == null ? null : scope.resolve(n);
        Infer.Fn f = in.globalFns.get(n);
        if (f != null && (b == null || b == Scope.Binding.GLOBAL)) {
            return pure.contains(f);
        }
        Layouts.Cls k = in.globalClasses.get(n);
        if (k != null && (b == null || b == Scope.Binding.GLOBAL)) {
            Layouts.Cls o = Layouts.resolver(k, "__init__");
            if (o == null) {
                return true;
            }
            FunctionDef init = o.typable.get("__init__");
            return init != null && pureInits.contains(in.fns.get(init));
        }
        return (b == null || b == Scope.Binding.BUILTIN) && PURE_BUILTINS.contains(n)
                && !em.moduleScopeHasStarImport();
    }

    private boolean callsArePure(Object o) {
        if (o instanceof Call && !pureCall((Call) o, sc)) {
            return false;
        }
        boolean[] ok = {true};
        Infer.children(o, x -> ok[0] &= callsArePure(x));
        return ok[0];
    }

    void plan() {
        findPure();
        if (!in.open.isEmpty()) {
            return;
        }
        for (Layouts.Cls k : layouts.classes.values()) {
            Map<String, Ty> m = new LinkedHashMap<String, Ty>();
            for (String f : k.fields) {
                Ty t = in.fieldType(k, f);
                if (representable(t) && in.definitelySet(k, f)) {
                    m.put(f, t);
                }
            }
            typedFields.put(k, m);
        }
        for (Infer.Fn f : in.fns.values()) {
            byScope.put(f.def.scope, f);
            if (f.cls != null && !f.def.name.equals("__init__")) {
                String key = f.def.name + "/" + (f.params.length - 1);
                List<Infer.Fn> g = groups.get(key);
                if (g == null) {
                    g = new ArrayList<Infer.Fn>();
                    groups.put(key, g);
                }
                g.add(f);
            }
        }
        Set<Infer.Fn> ok = new LinkedHashSet<Infer.Fn>(in.fns.values());
        Map<Infer.Fn, Set<Infer.Fn>> deps = new HashMap<Infer.Fn, Set<Infer.Fn>>();
        Map<Infer.Fn, Set<String>> gdeps = new HashMap<Infer.Fn, Set<String>>();
        for (Infer.Fn f : in.fns.values()) {
            typed.put(f, typedName(f));
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Infer.Fn f : new ArrayList<Infer.Fn>(ok)) {
                if (bodies.containsKey(f)) {
                    continue;
                }
                try {
                    bodies.put(f, function(f));
                    deps.put(f, calls);
                    gdeps.put(f, callsGroups);
                } catch (Unsupported u) {
                    if (System.getProperty("cn1.python.dumpTypes") != null) {
                        System.err.println("untyped " + f.def.name + ": " + u.getMessage());
                    }
                    ok.remove(f);
                    typed.remove(f);
                    changed = true;
                }
            }
            for (Infer.Fn f : new ArrayList<Infer.Fn>(ok)) {
                boolean good = ok.containsAll(deps.get(f));
                for (String g : gdeps.get(f)) {
                    good &= ok.containsAll(groups.get(g));
                }
                if (!good) {
                    ok.remove(f);
                    typed.remove(f);
                    bodies.remove(f);
                    changed = true;
                }
            }
        }
        planLongParams(ok);
        for (Set<String> g : gdeps.values()) {
            if (g != null) {
                usedGroups.addAll(g);
            }
        }
        usedGroups.retainAll(liveGroups(ok));
        // A typed field no generic code may reach directly: the generic
        // code goes through getField/setField, which box.
        for (Map.Entry<Layouts.Cls, Map<String, Ty>> e : typedFields.entrySet()) {
            for (String f : e.getValue().keySet()) {
                List<Layouts.Cls> o = layouts.fieldOwners.get(f);
                o.remove(e.getKey());
                if (o.isEmpty()) {
                    layouts.fieldOwners.remove(f);
                }
            }
        }
    }

    private Set<String> liveGroups(Set<Infer.Fn> ok) {
        Set<String> r = new HashSet<String>();
        for (Map.Entry<String, List<Infer.Fn>> e : groups.entrySet()) {
            if (ok.containsAll(e.getValue())) {
                r.add(e.getKey());
            }
        }
        return r;
    }

    /// Which typed module functions get a version taking their int
    /// parameters as longs, and the interval of each: the union of the
    /// arguments of every call whose int arguments all fit a long, found by
    /// running the range analysis over every typed body to a fixpoint (a
    /// recursive call's arguments depend on its own parameters). A call
    /// whose arguments are within those intervals calls that version
    /// directly, and the generic version starts with a test that sends
    /// arguments within them there; a call outside them is unchanged.
    /// `fib(n - 1)` under `if n < 2: return n` then runs on longs from the
    /// first call down.
    ///
    /// What the long version returns is bounded the same way, when the
    /// analysis bounds it, and that is only trusted once a round reproduces
    /// both the intervals and the returns it assumed.
    private void planLongParams(Set<Infer.Fn> ok) {
        Map<Infer.Fn, IntRanges.Iv[]> seeds = new HashMap<Infer.Fn, IntRanges.Iv[]>();
        Map<Infer.Fn, IntRanges.Iv> rets = new HashMap<Infer.Fn, IntRanges.Iv>();
        boolean settled = false;
        for (int round = 0; round < 12 && !settled; round++) {
            Map<Infer.Fn, IntRanges.Iv[]> ns = new HashMap<Infer.Fn, IntRanges.Iv[]>();
            Map<Infer.Fn, IntRanges.Iv> nr = new HashMap<Infer.Fn, IntRanges.Iv>();
            for (Infer.Fn f : ok) {
                // The generic and the long version's calls apart: one
                // site's arguments differ between the two bodies.
                Map<Call, IntRanges.Iv[]> st = new HashMap<Call, IntRanges.Iv[]>();
                new IntRanges(in, f, null, st, seeds, rets);
                List<Map.Entry<Call, IntRanges.Iv[]>> all = new ArrayList<Map.Entry<Call, IntRanges.Iv[]>>();
                copyEntries(st, all);
                if (seeds.containsKey(f)) {
                    Map<Call, IntRanges.Iv[]> sl = new HashMap<Call, IntRanges.Iv[]>();
                    IntRanges r = new IntRanges(in, f, seeds(f, seeds.get(f)), sl, seeds, rets);
                    copyEntries(sl, all);
                    IntRanges.Iv v = f.ret.k == Ty.K.INT && !f.ret.opt ? r.returns() : null;
                    if (v != null && v.fitsLong()) {
                        nr.put(f, v);
                    }
                }
                for (Map.Entry<Call, IntRanges.Iv[]> e : all) {
                    Infer.Fn g = in.globalFns.get(((Name) e.getKey().func).id);
                    IntRanges.Iv[] a = g == null || !ok.contains(g) || g.cls != null ? null
                            : IntRanges.longArgs(g, e.getValue());
                    if (a == null) {
                        continue;
                    }
                    IntRanges.Iv[] o = ns.get(g);
                    if (o != null) {
                        for (int i = 0; i < a.length; i++) {
                            a[i] = a[i] == null ? null : a[i].join(o[i]);
                        }
                    }
                    ns.put(g, a);
                }
            }
            if (round >= 3) {
                // Out to the next threshold, so the rounds end.
                for (Map.Entry<Infer.Fn, IntRanges.Iv[]> e : ns.entrySet()) {
                    IntRanges.Iv[] o = seeds.get(e.getKey());
                    IntRanges.Iv[] a = e.getValue();
                    for (int i = 0; o != null && i < a.length; i++) {
                        a[i] = a[i] == null ? null : IntRanges.widen(o[i], a[i]);
                    }
                }
                ns.values().removeIf(a -> !IntRanges.fitAll(a));
            }
            settled = sameSeeds(ns, seeds) && sameRets(nr, rets);
            seeds = ns;
            rets = nr;
        }
        if (!settled) {
            // The intervals are sound whatever they are (every call is
            // tested against them); the returns are not.
            rets = new HashMap<Infer.Fn, IntRanges.Iv>();
        }
        while (!seeds.isEmpty()) {
            longParams = seeds;
            longRets = rets;
            Infer.Fn failed;
            try {
                failed = emitLong(ok);
            } catch (Unsupported u) {
                // A typed body that only failed beside the long versions:
                // without them, it is what it was.
                seeds.clear();
                break;
            }
            if (failed == null) {
                return;
            }
            seeds.remove(failed);
            rets = new HashMap<Infer.Fn, IntRanges.Iv>();
        }
        longParams = seeds;
        longRets = rets;
        emitLong(ok);
    }

    /// Emits every typed body again against `longParams`, and the long
    /// versions; the function whose long version could not be emitted, or
    /// null.
    private Infer.Fn emitLong(Set<Infer.Fn> ok) {
        slow.setLength(0);
        slowCount = 0;
        longBodies.clear();
        for (Infer.Fn f : longParams.keySet()) {
            try {
                longBodies.put(f, function(f, true));
            } catch (Unsupported u) {
                return f;
            }
        }
        for (Infer.Fn f : ok) {
            bodies.put(f, function(f));
        }
        return null;
    }

    private static boolean sameSeeds(Map<Infer.Fn, IntRanges.Iv[]> a, Map<Infer.Fn, IntRanges.Iv[]> b) {
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        for (Map.Entry<Infer.Fn, IntRanges.Iv[]> e : a.entrySet()) {
            IntRanges.Iv[] x = e.getValue();
            IntRanges.Iv[] y = b.get(e.getKey());
            for (int i = 0; i < x.length; i++) {
                if (x[i] == null ? y[i] != null : !x[i].same(y[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean sameRets(Map<Infer.Fn, IntRanges.Iv> a, Map<Infer.Fn, IntRanges.Iv> b) {
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        for (Map.Entry<Infer.Fn, IntRanges.Iv> e : a.entrySet()) {
            if (!e.getValue().same(b.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static boolean representable(Ty t) {
        switch (t.k) {
            case INT:
            case BOOL:
            case FLOAT:
            case STR:
            case INST:
            case LIST:
                return true;
            default:
                return false;
        }
    }

    private static String longName(Infer.Fn f) {
        return "TL_" + Emitter.mangle(f.def.name);
    }

    private String typedName(Infer.Fn f) {
        if (f.cls != null) {
            return "TM_" + f.cls.javaName + "_" + Emitter.mangle(f.def.name);
        }
        return "TF_" + Emitter.mangle(f.def.name);
    }

    Ty typedField(Layouts.Cls owner, String f) {
        Map<String, Ty> m = typedFields.get(owner);
        return m == null ? null : m.get(f);
    }

    // ---- Java types ------------------------------------------------------

    static String jt(Ty t) {
        switch (t.k) {
            case BOOL:
                return "boolean";
            case FLOAT:
                return "double";
            case STR:
                return "String";
            case INST:
                return t.cls.javaName;
            case LIST:
                return "PyList";
            default:
                return "Object";
        }
    }

    static boolean objectRet(Ty t) {
        return "Object".equals(jt(t));
    }

    /// The Java return type of typed `f`. An int returns as a `long`; one
    /// that is not a long returns `Long.MIN_VALUE` and leaves the object
    /// in `Typed.retBig`, for the caller to `Typed.take`.
    static String retJ(Infer.Fn f) {
        return f.ret.k == Ty.K.INT ? "long" : jt(f.ret);
    }

    private static String zero(Ty t) {
        switch (t.k) {
            case BOOL:
                return "false";
            case FLOAT:
                return "0.0";
            default:
                return "null";
        }
    }

    // ---- conversions -----------------------------------------------------

    String box(E e) {
        switch (e.t.k) {
            case BOOL:
                return "Py.bool(" + e.c + ")";
            case FLOAT:
                return "Py.of(" + e.c + ")";
            case STR:
            case INST:
            case LIST:
                return e.t.opt ? "Typed.box(" + e.c + ")" : e.c;
            default:
                return e.c;
        }
    }

    /// `c`, an object the inference proved to be of type `to`, as `to`.
    String unbox(String c, Ty to) {
        switch (to.k) {
            case BOOL:
                return "Typed.asBool(" + c + ")";
            case FLOAT:
                return "Typed.asFloat(" + c + ")";
            case STR:
                return "Typed.asStr(" + c + ")";
            case LIST:
                return "Typed.asList(" + c + ")";
            case INST:
                unboxers.add(to.cls);
                return "u_" + to.cls.javaName + "(" + c + ")";
            default:
                return c;
        }
    }

    String conv(E e, Ty to) {
        String tj = jt(to);
        if ("Object".equals(tj)) {
            return box(e);
        }
        if (e.t.k == Ty.K.NONE) {
            if (to.nullable()) {
                return "Py.NONE".equals(e.c) ? "null" : "Typed.<" + tj + ">none(" + e.c + ")";
            }
            throw new Unsupported("None as " + to);
        }
        if ("Object".equals(jt(e.t))) {
            return unbox(e.c, to);
        }
        if (e.t.k == to.k) {
            if (e.t.k == Ty.K.INST && !Infer.isSelfOrAncestor(to.cls, e.t.cls)) {
                throw new Unsupported("downcast " + e.t + " to " + to);
            }
            return e.c;
        }
        throw new Unsupported("convert " + e.t + " to " + to);
    }

    String toD(E e) {
        switch (e.t.k) {
            case FLOAT:
                return e.c;
            case INT:
                return "h_toFloat(" + e.c + ")";
            case BOOL:
                return "(" + e.c + " ? 1.0 : 0.0)";
            default:
                throw new Unsupported("float of " + e.t);
        }
    }

    private boolean customTruth(Layouts.Cls k) {
        for (Layouts.Cls c : layouts.classes.values()) {
            if (Infer.isSelfOrAncestor(k, c) || Infer.isSelfOrAncestor(c, k)) {
                if (c.bodyNames.contains("__bool__") || c.bodyNames.contains("__len__")) {
                    return true;
                }
            }
        }
        return false;
    }

    String test(E e) {
        switch (e.t.k) {
            case BOOL:
                return e.c;
            case FLOAT:
                return "(" + e.c + " != 0.0)";
            case INST:
                if (customTruth(e.t.cls)) {
                    return "Py.isTrue(" + box(e) + ")";
                }
                return "(" + e.c + " != null)";
            case STR:
            case LIST:
                return "Typed.truthy(" + e.c + ")";
            default:
                return "Py.isTrue(" + e.c + ")";
        }
    }

    // ---- functions -------------------------------------------------------

    private String var(String n) {
        return "v_" + Emitter.mangle(n);
    }

    /// The object half of int local `n` (see `dual`).
    private static String big(String n) {
        return "w_" + Emitter.mangle(n);
    }

    /// A Java parameter of type `t`: an int is two, its long and its object.
    static String param(Ty t, String name, String bigName) {
        return t.k == Ty.K.INT ? "long " + name + ", Object " + bigName : jt(t) + " " + name;
    }

    /// The long and object halves of int (or bool) `e`, for a pair: the
    /// object may be a `Long` here when nothing better is at hand, which
    /// the receiving end normalizes.
    private String[] parts(E e) {
        if (e.lc != null) {
            return new String[] {e.lc, "null"};
        }
        if (e.ll != null) {
            return new String[] {e.ll, e.bg};
        }
        return new String[] {"0L", box(e)};
    }

    private Ty localType(String n) {
        Ty t = cur.locals.get(n);
        return t == null ? Ty.BOT : t;
    }

    /// Whether local `n` is read anywhere in the function but in the body
    /// of loop `f` (whose target it is).
    private boolean readOutside(String n, For f) {
        int[] c = new int[1];
        countReads(cur.def.body, n, f, c);
        return c[0] > 0;
    }

    private static void countReads(Object o, String n, For skip, int[] c) {
        if (o == skip) { //NOPMD CompareObjectsWithEquals - identity is meant
            countReads(skip.iter, n, null, c);
            return;
        }
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            c[0]++;
        }
        Infer.children(o, x -> countReads(x, n, skip, c));
    }

    private String function(Infer.Fn f) {
        return function(f, false);
    }

    /// The parameters of `f`'s long version, by name, at their intervals.
    private static Map<String, IntRanges.Iv> seeds(Infer.Fn f, IntRanges.Iv[] iv) {
        Map<String, IntRanges.Iv> m = new HashMap<String, IntRanges.Iv>();
        for (int i = 0; i < iv.length; i++) {
            if (iv[i] != null) {
                m.put(f.names.get(i), iv[i]);
            }
        }
        return m;
    }

    /// `f`'s typed body; with `asLong`, its long version (see `longParams`).
    private String function(Infer.Fn f, boolean asLong) {
        cur = f;
        sc = f.def.scope;
        calls = new LinkedHashSet<Infer.Fn>();
        callsGroups = new LinkedHashSet<String>();
        tmp = 0;
        nonNull = new HashSet<String>();
        shadow = new HashMap<String, String>();
        codePoints.clear();
        sites = new HashMap<Call, IntRanges.Iv[]>();
        IntRanges.Iv[] lp = asLong ? longParams.get(f) : null;
        ranges = new IntRanges(in, f, lp == null ? null : seeds(f, lp), sites, longParams, longRets);
        longLocals = ranges.longLocals();
        for (int i = 0; i < f.names.size(); i++) {
            if (lp == null || lp[i] == null) {
                // A parameter arrives as a pair, whatever its range.
                longLocals.remove(f.names.get(i));
            } else if (!longLocals.contains(f.names.get(i))) {
                throw new Unsupported("long parameter " + f.names.get(i) + " outgrows a long");
            }
        }
        if (!f.def.scope.cells.isEmpty() || f.def.scope.hasYield || f.def.isAsync || !f.def.decorators.isEmpty()
                || f.def.args.vararg != null || f.def.args.kwarg != null || !f.def.args.kwonly.isEmpty()) {
            throw new Unsupported("signature");
        }
        for (String n : f.locals.keySet()) {
            if (sc.resolve(n) != Scope.Binding.LOCAL) {
                throw new Unsupported("non-local " + n);
            }
        }
        da = new DefAssign();
        da.function(f.def.body, sc.params);
        recycled = ListRecycling.find(f.def.body, f.names, sc);
        scalars = findScalars(f);
        StringBuilder b = new StringBuilder();
        b.append("    static ").append(retJ(f)).append(' ').append(lp != null ? longName(f) : typed.get(f)).append('(');
        Set<String> declared = new HashSet<String>(f.names);
        StringBuilder norm = new StringBuilder();
        for (int i = 0; i < f.names.size(); i++) {
            String n = f.names.get(i);
            if (lp != null && lp[i] != null) {
                b.append(i == 0 ? "" : ", ").append("long ").append(var(n));
                continue;
            }
            b.append(i == 0 ? "" : ", ").append(param(f.params[i], var(n), big(n)));
            if (f.params[i].k == Ty.K.INT) {
                // A caller holding only the object passes it whole.
                norm.append("        if (").append(big(n)).append(" instanceof Long) {\n            ").append(var(n))
                        .append(" = ((Long) ").append(big(n)).append(").longValue();\n            ").append(big(n))
                        .append(" = null;\n        }\n");
            }
        }
        b.append(") {\n").append(norm);
        IntRanges.Iv[] dl = asLong ? null : longParams.get(f);
        if (dl != null) {
            // Arguments the long version takes go there.
            StringBuilder test = new StringBuilder();
            StringBuilder pass = new StringBuilder();
            for (int i = 0; i < dl.length; i++) {
                String n = f.names.get(i);
                pass.append(i == 0 ? "" : ", ").append(var(n));
                if (dl[i] == null) {
                    if (f.params[i].k == Ty.K.INT) {
                        pass.append(", ").append(big(n));
                    }
                    continue;
                }
                test.append(test.length() == 0 ? "" : " && ").append(big(n)).append(" == null");
                if (dl[i].lo.compareTo(IntRanges.LONG_MIN) > 0) {
                    test.append(" && ").append(var(n)).append(" >= ").append(lit(dl[i].lo));
                }
                if (dl[i].hi.compareTo(IntRanges.LONG_MAX) < 0) {
                    test.append(" && ").append(var(n)).append(" <= ").append(lit(dl[i].hi));
                }
            }
            b.append("        if (").append(test).append(") {\n            return ").append(longName(f)).append('(')
                    .append(pass).append(");\n        }\n");
        }
        for (Map.Entry<String, Ty> e : f.locals.entrySet()) {
            if (longLocals.contains(e.getKey())) {
                if (declared.contains(e.getKey())) {
                    continue;
                }
                declared.add(e.getKey());
                b.append("        long ").append(var(e.getKey())).append(" = 0L;\n");
            } else if (declared.add(e.getKey())) {
                if (e.getValue().k == Ty.K.INT) {
                    b.append("        long ").append(var(e.getKey())).append(" = 0L;\n        Object ")
                            .append(big(e.getKey())).append(" = null;\n");
                } else {
                    b.append("        ").append(jt(e.getValue())).append(' ').append(var(e.getKey())).append(" = ")
                            .append(zero(e.getValue())).append(";\n");
                }
            }
        }
        scalarDecls(b);
        boolean abrupt = block(f.def.body, b, 2);
        if (!abrupt) {
            if (f.ret.k == Ty.K.INT) {
                // Never reached (the function would not return an int if
                // it could fall off its end), but javac cannot tell.
                b.append("        return Typed.retBig(Py.NONE);\n");
            } else if (objectRet(f.ret)) {
                b.append("        return Py.NONE;\n");
            } else if (f.ret.nullable()) {
                b.append("        return null;\n");
            } else {
                throw new Unsupported("falls off the end returning " + f.ret);
            }
        }
        b.append("    }\n");
        return b.toString();
    }

    // ---- statements ------------------------------------------------------

    private static void indent(StringBuilder b, int ind) {
        for (int i = 0; i < ind; i++) {
            b.append("    ");
        }
    }

    /// Emits `body`; true when it cannot complete normally (javac agrees:
    /// nothing is emitted after such a statement).
    private boolean block(List<Stmt> body, StringBuilder b, int ind) {
        for (Stmt s : body) {
            if (stmt(s, b, ind)) {
                return true;
            }
        }
        return false;
    }

    private String fresh() {
        return "t" + (++tmp);
    }

    /// Statements that hoisted int arithmetic (`hoistInt`) runs before, or
    /// null where nothing may run first (a loop header).
    private StringBuilder pre; //NOPMD AvoidStringBufferField - the generator's output
    private int preInd;

    private boolean stmt(Stmt s, StringBuilder b, int ind) {
        StringBuilder savedPre = pre;
        int savedInd = preInd;
        boolean hoist = (s instanceof ExprStmt || s instanceof Assign || s instanceof AugAssign
                || s instanceof Return || s instanceof If) && !hasWalrus(s);
        pre = hoist ? new StringBuilder() : null;
        preInd = ind;
        boolean savedPure = stmtPure;
        stmtPure = hoist && callsArePure(s instanceof If ? ((If) s).test : s);
        StringBuilder sb = new StringBuilder();
        try {
            boolean r = stmt0(s, sb, ind);
            Emitter.lineMark(s.line, b, ind);
            if (pre != null) {
                b.append(pre);
            }
            b.append(sb);
            return r;
        } finally {
            pre = savedPre;
            preInd = savedInd;
            stmtPure = savedPure;
        }
    }

    private static boolean hasWalrus(Object o) {
        if (o instanceof NamedExpr) {
            return true;
        }
        boolean[] f = new boolean[1];
        Infer.children(o, x -> f[0] |= hasWalrus(x));
        return f[0];
    }

    private boolean stmt0(Stmt s, StringBuilder b, int ind) {
        if (s instanceof Pass) {
            return false;
        }
        if (s instanceof ExprStmt) {
            Expr v = ((ExprStmt) s).value;
            if (v instanceof Constant) {
                return false;
            }
            E e = expr(v);
            indent(b, ind);
            if (v instanceof Call && e.c.matches("[A-Za-z_][A-Za-z0-9_.<>]*\\(.*\\)")) {
                b.append(e.c).append(";\n");
            } else {
                b.append("Typed.drop(").append(box(e)).append(");\n");
            }
            return false;
        }
        if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                && scalars.containsKey(((Name) ((Assign) s).targets.get(0)).id)) {
            scalarStore(((Name) ((Assign) s).targets.get(0)).id, (Call) ((Assign) s).value, b, ind);
            return false;
        }
        if (s instanceof Assign && ((Assign) s).targets.size() == 1 && sliceOf(((Assign) s).targets.get(0))
                && sliceOf(((Assign) s).value)) {
            // `o[a:b] = v[d:e]`: copied without the intermediate list, the
            // source evaluated first as in Python.
            Subscript src = (Subscript) ((Assign) s).value;
            Subscript dst = (Subscript) ((Assign) s).targets.get(0);
            E v = expr(src.value);
            String vp = sliceParts((Slice) src.index);
            E o = expr(dst.value);
            String op = sliceParts((Slice) dst.index);
            if (v.t.k == Ty.K.LIST && o.t.k == Ty.K.LIST) {
                indent(b, ind);
                b.append("Py.setsliceFrom(").append(v.c).append(", ").append(vp).append(", ").append(o.c)
                        .append(", ").append(op).append(");\n");
                return false;
            }
            throw new Unsupported("slice copy between " + v.t + " and " + o.t);
        }
        if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                && recycled.contains(((Name) ((Assign) s).targets.get(0)).id)
                && localType(((Name) ((Assign) s).targets.get(0)).id).k == Ty.K.LIST
                && (ListRecycling.isSliceCopy(((Assign) s).value) || ListRecycling.isEmptyList(((Assign) s).value))) {
            // Nothing but this local can see its previous list, so the new
            // one reuses its array, as the generic code does.
            Name n = (Name) ((Assign) s).targets.get(0);
            Expr v = ((Assign) s).value;
            String old = var(n.id);
            String c;
            if (v instanceof ListExpr) {
                c = "Py.recycleList(" + old + ")";
            } else {
                E src = expr(((Subscript) v).value);
                if (src.t.k != Ty.K.LIST) {
                    throw new Unsupported("slice copy of " + src.t);
                }
                c = "Typed.asList(Py.recycleSlice(" + old + ", " + src.c + ", " + sliceParts((Slice) ((Subscript) v).index)
                        + "))";
            }
            store(n, new E(c, localType(n.id).nonNone()), b, ind);
            return false;
        }
        if (s instanceof Assign) {
            Assign a = (Assign) s;
            if (a.targets.size() == 1 && a.targets.get(0) instanceof TupleExpr && a.value instanceof TupleExpr) {
                List<Expr> ts = ((TupleExpr) a.targets.get(0)).elts;
                List<Expr> vs = ((TupleExpr) a.value).elts;
                if (ts.size() != vs.size()) {
                    throw new Unsupported("unpack length");
                }
                List<E> tv = new ArrayList<E>();
                for (Expr v : vs) {
                    if (v instanceof Starred) {
                        throw new Unsupported("starred");
                    }
                    tv.add(spillTo(expr(v), b, ind));
                }
                for (int i = 0; i < ts.size(); i++) {
                    store(ts.get(i), tv.get(i), b, ind);
                }
                return false;
            }
            E v = expr(a.value);
            if (a.targets.size() > 1) {
                String t = fresh();
                indent(b, ind);
                b.append(jt(v.t)).append(' ').append(t).append(" = ").append(v.c).append(";\n");
                v = new E(t, v.t);
            }
            for (Expr t : a.targets) {
                store(t, v, b, ind);
            }
            return false;
        }
        if (s instanceof AugAssign) {
            augAssign((AugAssign) s, b, ind);
            return false;
        }
        if (s instanceof Return) {
            Expr v = ((Return) s).value;
            indent(b, ind);
            if (v == null) {
                if (objectRet(cur.ret)) {
                    b.append("return Py.NONE;\n");
                } else if (cur.ret.nullable()) {
                    b.append("return null;\n");
                } else {
                    throw new Unsupported("return None as " + cur.ret);
                }
            } else if (cur.ret.k == Ty.K.INT) {
                Expr saved = pushTop;
                pushTop = v;
                E e;
                try {
                    e = top(v);
                } finally {
                    pushTop = saved;
                }
                if (e.lc != null && ranges.eval(v, null).fitsLong() && ranges.eval(v, null).lo.compareTo(IntRanges.LONG_MIN) > 0) {
                    // Never the marker.
                    b.append("return ").append(e.lc).append(";\n");
                } else {
                    returnInt(e, b, ind);
                }
            } else {
                b.append("return ").append(conv(expr(v), cur.ret)).append(";\n");
            }
            return true;
        }
        if (s instanceof If) {
            If i = (If) s;
            Set<String> before = new HashSet<String>(nonNull);
            indent(b, ind);
            b.append("if (").append(testExpr(i.test)).append(") {\n");
            nonNull = new HashSet<String>(before);
            nonNull.addAll(whenTrue(i.test));
            boolean a1 = block(i.body, b, ind + 1);
            Set<String> after1 = nonNull;
            nonNull = new HashSet<String>(before);
            nonNull.addAll(whenFalse(i.test));
            indent(b, ind);
            boolean a2 = false;
            if (i.orelse.isEmpty()) {
                b.append("}\n");
            } else {
                b.append("} else {\n");
                a2 = block(i.orelse, b, ind + 1);
                indent(b, ind);
                b.append("}\n");
            }
            // When the if branch always leaves, what holds is what the else
            // branch left, which nonNull already is.
            Set<String> after2 = nonNull;
            if (!a1) {
                if (!a2) {
                    after1.retainAll(after2);
                }
                nonNull = after1;
            }
            return a1 && a2;
        }
        if (s instanceof While) {
            While w = (While) s;
            if (!w.orelse.isEmpty()) {
                throw new Unsupported("while-else");
            }
            boolean forever = w.test instanceof Constant && ((Constant) w.test).kind == Constant.Kind.TRUE;
            nonNull.removeAll(assigned(w.body));
            shadow.keySet().removeAll(assigned(w.body));
            forgetPaths();
            Set<String> inv = new HashSet<String>(nonNull);
            String test = "true";
            StringBuilder head = null;
            if (!forever) {
                // The test may lower its int arithmetic and compares to
                // statements (see `hoistInt`); they then run at the top of
                // every iteration, before a test that leaves the loop.
                StringBuilder savedPre = pre;
                int savedInd = preInd;
                boolean savedPure = stmtPure;
                boolean lower = !hasWalrus(w.test);
                pre = lower ? new StringBuilder() : null;
                preInd = ind + 1;
                stmtPure = lower && callsArePure(w.test);
                try {
                    test = testExpr(w.test);
                    head = pre;
                } finally {
                    pre = savedPre;
                    preInd = savedInd;
                    stmtPure = savedPure;
                }
            }
            indent(b, ind);
            if (head != null && head.length() > 0) {
                b.append("while (true) {\n").append(head);
                indent(b, ind + 1);
                b.append("if (!").append(test).append(") {\n");
                indent(b, ind + 2);
                b.append("break;\n");
                indent(b, ind + 1);
                b.append("}\n");
            } else {
                b.append("while (").append(test).append(") {\n");
            }
            nonNull.addAll(whenTrue(w.test));
            block(w.body, b, ind + 1);
            nonNull = inv;
            if (!Infer.hasBreak(w.body)) {
                nonNull.addAll(whenFalse(w.test));
            }
            indent(b, ind);
            b.append("}\n");
            return forever && !Infer.hasBreak(w.body);
        }
        if (s instanceof For) {
            forLoop((For) s, b, ind);
            return false;
        }
        if (s instanceof Break) {
            indent(b, ind);
            b.append("break;\n");
            return true;
        }
        if (s instanceof Continue) {
            indent(b, ind);
            b.append("continue;\n");
            return true;
        }
        throw new Unsupported(s.getClass().getSimpleName());
    }

    /// `return e` from a function returning an int (see `retJ`); the
    /// statement is already indented.
    private void returnInt(E e, StringBuilder b, int ind) {
        if (e.pk != null) {
            // The marker and what it stands for, passed on as they are.
            b.append("return ").append(e.pk).append(";\n");
            return;
        }
        if (e.lc != null && literal(e.lc) && !e.lc.contains("-9223372036854775808")) {
            b.append("return ").append(e.lc).append(";\n");
            return;
        }
        if (e.lc != null) {
            String t = fresh();
            b.append("long ").append(t).append(" = ").append(e.lc).append(";\n");
            indent(b, ind);
            b.append("return ").append(t).append(" != Long.MIN_VALUE ? ").append(t)
                    .append(" : Typed.retBig(Long.valueOf(").append(t).append("));\n");
            return;
        }
        if (e.ll != null) {
            b.append("return ").append(e.bg).append(" == null && ").append(e.ll).append(" != Long.MIN_VALUE ? ")
                    .append(e.ll).append(" : Typed.retBig(").append(e.c).append(");\n");
            return;
        }
        b.append("return Typed.retObj(").append(box(e)).append(");\n");
    }

    /// A `range` bound as a Java long.
    private String bound(E e) {
        if (e.lc != null) {
            return e.lc;
        }
        if (e.ll != null) {
            return "(" + e.bg + " == null ? " + e.ll + " : Typed.bound(" + e.bg + "))";
        }
        return "Typed.bound(" + box(e) + ")";
    }

    /// Loop targets over a str whose only uses are `ord(c)`, to the Java
    /// variable holding the code point.
    private final Map<String, String> codePoints = new HashMap<String, String>();

    /// `for c in s`: the code points of `s`, one string each only when
    /// something reads `c` other than `ord(c)`.
    private void strLoop(For f, E it, Set<String> inBody, StringBuilder b, int ind) {
        String tn = ((Name) f.target).id;
        Ty vt = localType(tn);
        String s = fresh();
        String n = fresh();
        String i = fresh();
        String cp = fresh();
        indent(b, ind);
        b.append("{\n");
        indent(b, ind + 1);
        b.append("String ").append(s).append(" = ").append(it.c).append(";\n");
        indent(b, ind + 1);
        b.append("int ").append(n).append(" = ").append(s).append(".length();\n");
        indent(b, ind + 1);
        b.append("for (int ").append(i).append(" = 0; ").append(i).append(" < ").append(n).append(";) {\n");
        indent(b, ind + 2);
        b.append("int ").append(cp).append(" = ").append(s).append(".charAt(").append(i).append("++);\n");
        indent(b, ind + 2);
        // A surrogate pair is one character to Python.
        b.append("if (").append(cp).append(" >= 0xD800 && ").append(cp).append(" < 0xDC00 && ").append(i).append(" < ")
                .append(n).append(" && ").append(s).append(".charAt(").append(i).append(") >= 0xDC00 && ").append(s)
                .append(".charAt(").append(i).append(") < 0xE000) {\n");
        indent(b, ind + 3);
        b.append(cp).append(" = ((").append(cp).append(" - 0xD800) << 10) + (").append(s).append(".charAt(").append(i)
                .append("++) - 0xDC00) + 0x10000;\n");
        indent(b, ind + 2);
        b.append("}\n");
        boolean ordOnly = !inBody.contains(tn) && !readOutside(tn, f) && onlyOrd(f.body, tn);
        if (!ordOnly) {
            E c = new E("Typed.ch(" + cp + ")", Ty.STR);
            indent(b, ind + 2);
            if (vt.k == Ty.K.STR) {
                b.append(var(tn)).append(" = ").append(c.c).append(";\n");
            } else {
                b.append(var(tn)).append(" = ").append(conv(c, vt)).append(";\n");
            }
            if (vt.k == Ty.K.STR || vt.k == Ty.K.DYN) {
                nonNull.add(tn);
            }
        }
        if (!inBody.contains(tn)) {
            codePoints.put(tn, cp);
        }
        block(f.body, b, ind + 2);
        codePoints.remove(tn);
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
    }

    /// Whether every read of `n` in `o` is the argument of the builtin `ord`.
    private boolean onlyOrd(Object o, String n) {
        if (ordOf(o, n)) {
            return true;
        }
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            return false;
        }
        boolean[] ok = {true};
        Infer.children(o, x -> ok[0] &= onlyOrd(x, n));
        return ok[0];
    }

    /// Whether `o` is `ord(n)`, with `ord` the builtin.
    private boolean ordOf(Object o, String n) {
        if (!(o instanceof Call)) {
            return false;
        }
        Call c = (Call) o;
        return c.func instanceof Name && "ord".equals(((Name) c.func).id) && c.keywords.isEmpty()
                && c.args.size() == 1 && c.args.get(0) instanceof Name && ((Name) c.args.get(0)).id.equals(n)
                && sc.resolve("ord") == Scope.Binding.BUILTIN && !em.moduleScopeHasStarImport();
    }

    private void forLoop(For f, StringBuilder b, int ind) {
        if (!f.orelse.isEmpty()) {
            throw new Unsupported("for-else");
        }
        Set<String> inBody = assigned(f.body);
        nonNull.removeAll(inBody);
        shadow.keySet().removeAll(inBody);
        forgetPaths();
        if (f.target instanceof Name) {
            nonNull.remove(((Name) f.target).id);
        }
        if (!(f.target instanceof Name) || sc.resolve(((Name) f.target).id) != Scope.Binding.LOCAL) {
            throw new Unsupported("for target");
        }
        String v = var(((Name) f.target).id);
        Ty vt = localType(((Name) f.target).id);
        if (in.rangeCall(f.iter, sc)) {
            List<Expr> a = ((Call) f.iter).args;
            String start = "0L";
            String stop;
            long step = 1;
            if (a.size() == 1) {
                stop = bound(expr(a.get(0)));
            } else {
                start = bound(expr(a.get(0)));
                stop = bound(expr(a.get(1)));
                if (a.size() == 3) {
                    Long c = intConstant(a.get(2));
                    if (c == null || c.longValue() == 0) {
                        throw new Unsupported("range step");
                    }
                    step = c.longValue();
                }
            }
            String i = fresh();
            String e = fresh();
            indent(b, ind);
            b.append("{\n");
            indent(b, ind + 1);
            // Bounds are evaluated in order, once, before the loop.
            String s0 = fresh();
            b.append("long ").append(s0).append(" = ").append(start).append(";\n");
            indent(b, ind + 1);
            b.append("long ").append(e).append(" = ").append(stop).append(";\n");
            indent(b, ind + 1);
            b.append("for (long ").append(i).append(" = ").append(s0).append("; ").append(i)
                    .append(step > 0 ? " < " : " > ").append(e).append("; ").append(i).append(" += ").append(step)
                    .append("L) {\n");
            String tn = ((Name) f.target).id;
            boolean counter = vt.k == Ty.K.INT && !inBody.contains(tn);
            indent(b, ind + 2);
            if (longLocals.contains(tn)) {
                b.append(v).append(" = ").append(i).append(";\n");
            } else if (vt.k == Ty.K.INT) {
                b.append(v).append(" = ").append(i).append(";\n");
                indent(b, ind + 2);
                b.append(big(tn)).append(" = null;\n");
            } else {
                b.append(v).append(" = ").append(conv(new E("Long.valueOf(" + i + ")", Ty.INT), vt)).append(";\n");
            }
            if (counter) {
                shadow.put(tn, i);
            }
            block(f.body, b, ind + 2);
            shadow.remove(tn);
            indent(b, ind + 1);
            b.append("}\n");
            indent(b, ind);
            b.append("}\n");
            return;
        }
        E it = expr(f.iter);
        if (it.t.k == Ty.K.STR && !it.t.opt) {
            strLoop(f, it, inBody, b, ind);
            return;
        }
        if (it.t.k != Ty.K.LIST) {
            throw new Unsupported("for over " + it.t);
        }
        Ty el = in.elem(it.t);
        String l = fresh();
        String k = fresh();
        indent(b, ind);
        b.append("{\n");
        indent(b, ind + 1);
        b.append("PyList ").append(l).append(" = Typed.iterable(").append(it.c).append(");\n");
        indent(b, ind + 1);
        b.append("for (int ").append(k).append(" = 0; ").append(k).append(" < ").append(l).append(".size(); ")
                .append(k).append("++) {\n");
        indent(b, ind + 2);
        E item = el.k == Ty.K.FLOAT ? new E("Typed.atF(" + l + ", " + k + ")", Ty.FLOAT)
                : new E(unbox(l + ".get(" + k + ")", el), el);
        if (vt.k == Ty.K.INT && !longLocals.contains(((Name) f.target).id)) {
            storePair(v, big(((Name) f.target).id), item, b, ind + 2);
        } else if (vt.k == Ty.K.INT) {
            b.append(v).append(" = Typed.toLong(").append(box(item)).append(");\n");
        } else {
            b.append(v).append(" = ").append(conv(item, vt)).append(";\n");
        }
        block(f.body, b, ind + 2);
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
    }

    private static Long intConstant(Expr e) {
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.INT) {
            // Any spelling: hex, octal and binary constants are as common
            // as decimal ones in integer code.
            java.math.BigInteger v = IntRanges.parse(((Constant) e).text);
            return v == null || v.bitLength() > 63 ? null : Long.valueOf(v.longValue());
        }
        if (e instanceof UnaryOp && "-".equals(((UnaryOp) e).op)) {
            Long v = intConstant(((UnaryOp) e).operand);
            return v == null ? null : Long.valueOf(-v.longValue());
        }
        return null;
    }

    /// Whether `c` is a plain Java local: evaluating it twice, or after
    /// something else, is the same as once.
    private static boolean plain(String c) {
        return c.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    /// Whether `c` is a Java long literal.
    private static boolean literal(String c) {
        return c.matches("\\(?-?[0-9]+L\\)?");
    }

    private void store(Expr t, E v, StringBuilder b, int ind) {
        if (t instanceof Name) {
            String n = ((Name) t).id;
            switch (sc.resolve(n)) {
                case LOCAL:
                    indent(b, ind);
                    if (longLocals.contains(n)) {
                        // The analysis proved every value it holds fits.
                        b.append(var(n)).append(" = ").append(fitting(v))
                                .append(";\n");
                        return;
                    }
                    if (localType(n).k == Ty.K.INT) {
                        storePair(var(n), big(n), v, b, ind);
                    } else {
                        b.append(var(n)).append(" = ").append(conv(v, localType(n))).append(";\n");
                    }
                    forgetPathsOf(n);
                    shadow.remove(n);
                    if (v.t.k == localType(n).k && !v.t.opt && v.t.k != Ty.K.NONE) {
                        nonNull.add(n);
                    } else {
                        nonNull.remove(n);
                    }
                    return;
                case GLOBAL:
                    indent(b, ind);
                    b.append("G[").append(em.slot(n)).append("] = ").append(box(v)).append(";\n");
                    return;
                default:
                    throw new Unsupported("store " + n);
            }
        }
        if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            E r = expr(a.value);
            if (fieldTy(r, a.attr).k == Ty.K.INT && typedField(owner(r, a.attr), a.attr) != null) {
                if (!plain(r.c) && !(v.ll != null || v.lc != null && plain(v.lc) || plain(v.c))) {
                    // Python evaluates the value before the receiver.
                    String tv = fresh();
                    String tb = fresh();
                    indent(b, ind);
                    b.append("long ").append(tv).append(" = 0L;\n");
                    indent(b, ind);
                    b.append("Object ").append(tb).append(" = null;\n");
                    indent(b, ind);
                    storePair(tv, tb, v, b, ind);
                    v = dual(tv, tb);
                }
                indent(b, ind);
                storeIntField(r, a.attr, v, b, ind);
                forgetAttr(a.attr);
                return;
            }
            if (!plain(v.c) && !plain(r.c)) {
                // Python evaluates the value before the receiver.
                String tv = fresh();
                indent(b, ind);
                b.append(jt(v.t)).append(' ').append(tv).append(" = ").append(v.c).append(";\n");
                v = new E(tv, v.t);
            }
            indent(b, ind);
            b.append(fieldRef(r, a.attr)).append(" = ").append(conv(v, fieldTy(r, a.attr))).append(";\n");
            forgetAttr(a.attr);
            String p = path(a);
            if (p != null && !v.t.opt && v.t.k != Ty.K.NONE && v.t.k == fieldTy(r, a.attr).k) {
                nonNull.add(p);
            }
            return;
        }
        if (t instanceof Subscript) {
            Subscript s = (Subscript) t;
            if (s.index instanceof Slice) {
                E l = expr(s.value);
                if (l.t.k != Ty.K.LIST) {
                    throw new Unsupported("slice store on " + l.t);
                }
                String p = sliceParts((Slice) s.index);
                indent(b, ind);
                b.append("Py.setslice(").append(l.c).append(", ").append(p).append(", ").append(box(v)).append(");\n");
                return;
            }
            E l = expr(s.value);
            E i = expr(s.index);
            if (l.t.k != Ty.K.LIST || !(i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                if (!plain(v.c) && v.ll == null && !(v.lc != null && (plain(v.lc) || literal(v.lc)))) {
                    v = spillTo(v, b, ind);
                }
                indent(b, ind);
                String k = l.t.k == Ty.K.LIST ? null : intKey(i);
                if (k != null) {
                    b.append("Typed.dset(").append(box(l)).append(", ").append(k).append(", ").append(box(v)).append(");\n");
                    return;
                }
                b.append("Py.setitem(").append(box(l)).append(", ").append(box(i)).append(", ").append(box(v))
                        .append(");\n");
                return;
            }
            if (!plain(v.c) && v.ll == null && !(v.lc != null && (plain(v.lc) || literal(v.lc)))) {
                String tv = fresh();
                indent(b, ind);
                b.append(jt(v.t)).append(' ').append(tv).append(" = ").append(v.c).append(";\n");
                v = new E(tv, v.t);
            }
            Ty el = in.elem(l.t);
            indent(b, ind);
            String[] ix = index(i);
            if (el.k == Ty.K.INT && ix != null && plain(l.c)) {
                String[] vp = parts(v);
                b.append("if (").append(ix[1]).append(inRange(l, ix[0])).append(" && ").append(l.c)
                        .append(".ints != null").append("null".equals(vp[1]) ? "" : " && " + vp[1] + " == null")
                        .append(" && (int) ").append(vp[0]).append(" == ").append(vp[0]).append(") {\n");
                indent(b, ind + 1);
                b.append(l.c).append(".ints[(int) ").append(ix[0]).append("] = (int) ").append(vp[0]).append(";\n");
                indent(b, ind);
                b.append("} else {\n");
                indent(b, ind + 1);
                b.append("h_set(").append(l.c).append(", ").append(box(i)).append(", ").append(box(v)).append(");\n");
                indent(b, ind);
                b.append("}\n");
                return;
            }
            if (i.lc != null && plain(l.c)) {
                String in = inRange(l, i.lc);
                if (el.k == Ty.K.FLOAT) {
                    b.append("if (").append(in).append(" && ").append(l.c).append(".floats != null) {\n");
                    indent(b, ind + 1);
                    b.append(l.c).append(".floats[(int) ").append(i.lc).append("] = ").append(conv(v, el)).append(";\n");
                } else {
                    b.append("if (").append(in).append(" && ").append(l.c).append(".ints == null && ").append(l.c)
                            .append(".floats == null) {\n");
                    indent(b, ind + 1);
                    b.append(l.c).append(".items[(int) ").append(i.lc).append("] = ").append(box(v)).append(";\n");
                }
                indent(b, ind);
                b.append("} else {\n");
                indent(b, ind + 1);
                b.append(el.k == Ty.K.FLOAT ? "h_setF(" : "h_set(").append(l.c).append(", ").append(box(i))
                        .append(", ").append(el.k == Ty.K.FLOAT ? conv(v, el) : box(v)).append(");\n");
                indent(b, ind);
                b.append("}\n");
                return;
            }
            if (el.k == Ty.K.FLOAT) {
                b.append("h_setF(").append(l.c).append(", ").append(box(i)).append(", ").append(conv(v, el))
                        .append(");\n");
            } else {
                b.append("h_set(").append(l.c).append(", ").append(box(i)).append(", ").append(box(v))
                        .append(");\n");
            }
            return;
        }
        throw new Unsupported("store target " + t.getClass().getSimpleName());
    }

    /// `l`/`o = v` for an int pair, the line already indented: the object
    /// half holds no `Long`.
    private void storePair(String l, String o, E v, StringBuilder b, int ind) {
        if (v.lc != null) {
            b.append(l).append(" = ").append(v.lc).append(";\n");
            indent(b, ind);
            b.append(o).append(" = null;\n");
            return;
        }
        if (v.ll != null) {
            if (!v.ll.equals(l)) {
                b.append(l).append(" = ").append(v.ll).append(";\n");
                indent(b, ind);
            }
            b.append(o).append(" = ").append(v.bg).append(";\n");
            return;
        }
        String t = fresh();
        b.append("Object ").append(t).append(" = ").append(box(v)).append(";\n");
        indent(b, ind);
        b.append("if (").append(t).append(" instanceof Long) {\n");
        indent(b, ind + 1);
        b.append(l).append(" = ((Long) ").append(t).append(").longValue();\n");
        indent(b, ind + 1);
        b.append(o).append(" = null;\n");
        indent(b, ind);
        b.append("} else {\n");
        indent(b, ind + 1);
        b.append(o).append(" = ").append(t).append(";\n");
        indent(b, ind);
        b.append("}\n");
    }

    /// `e`, the whole value of a statement that does nothing else with an
    /// effect: an augmented assignment to a local, or a `return`. Its
    /// arithmetic may then be hoisted even over calls that are not pure
    /// (see `collectLeaves`): its leaves run in order, as Python runs them,
    /// and nothing else in the statement runs between them.
    private E top(Expr e) {
        Expr saved = hoistTop;
        hoistTop = e;
        try {
            return expr(e);
        } finally {
            hoistTop = saved;
        }
    }

    private void augAssign(AugAssign a, StringBuilder b, int ind) {
        Expr t = a.target;
        if (t instanceof Name && longLocals.contains(((Name) t).id)) {
            store(t, top(new BinOp(t, a.op, a.value)), b, ind);
            return;
        }
        if (t instanceof Name && (localType(((Name) t).id).k == Ty.K.INT || localType(((Name) t).id).k == Ty.K.FLOAT)
                && sc.resolve(((Name) t).id) == Scope.Binding.LOCAL) {
            store(t, top(new BinOp(t, a.op, a.value)), b, ind);
            return;
        }
        if (t instanceof Attribute && ((Attribute) t).value instanceof Name
                && sc.resolve(((Name) ((Attribute) t).value).id) == Scope.Binding.LOCAL) {
            // The receiver is a local: reading it again is reading it once.
            E r0 = expr(((Attribute) t).value);
            if (r0.t.k == Ty.K.INST && fieldTy(r0, ((Attribute) t).attr).k == Ty.K.INT
                    && typedField(owner(r0, ((Attribute) t).attr), ((Attribute) t).attr) != null) {
                store(t, expr(new BinOp(t, a.op, a.value)), b, ind);
                return;
            }
        }
        if (t instanceof Name) {
            E cur0 = expr(t);
            E r = binop(a.op, cur0, expr(a.value), a.value);
            store(t, r, b, ind);
            return;
        }
        if (t instanceof Attribute) {
            Attribute at = (Attribute) t;
            E r = expr(at.value);
            String rv = r.c;
            if (!plain(rv)) {
                rv = fresh();
                indent(b, ind);
                b.append(jt(r.t)).append(' ').append(rv).append(" = ").append(r.c).append(";\n");
            }
            E recv = new E(rv, r.t);
            if (fieldTy(recv, at.attr).k == Ty.K.INT && typedField(owner(recv, at.attr), at.attr) != null) {
                E v = binop(a.op, intField(recv, at.attr), expr(a.value), a.value);
                indent(b, ind);
                storeIntField(recv, at.attr, v, b, ind);
                return;
            }
            String ref = fieldRef(recv, at.attr);
            E old = new E(ref, fieldTy(recv, at.attr));
            E v = binop(a.op, old, expr(a.value), a.value);
            indent(b, ind);
            b.append(fieldRef(recv, at.attr)).append(" = ").append(conv(v, fieldTy(recv, at.attr)))
                    .append(";\n");
            return;
        }
        if (t instanceof Subscript && !(((Subscript) t).index instanceof Slice)) {
            Subscript s = (Subscript) t;
            E l = expr(s.value);
            E i = expr(s.index);
            if (l.t.k != Ty.K.LIST || !(i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                throw new Unsupported("subscript aug on " + l.t);
            }
            Ty el0 = in.elem(l.t);
            if (el0.k == Ty.K.FLOAT && i.lc != null && plain(l.c) && !hasCall(a.value)) {
                // No call in the value: nothing it evaluates can change the
                // list, so the element is read after it.
                E rv = expr(a.value);
                if (rv.t.k == Ty.K.FLOAT || rv.t.k == Ty.K.INT || rv.t.k == Ty.K.BOOL) {
                    String tv = fresh();
                    indent(b, ind);
                    b.append(jt(rv.t)).append(' ').append(tv).append(" = ").append(rv.c).append(";\n");
                    E rvE = new E(tv, rv.t, rv.lc);
                    String fa = l.c + ".floats[(int) " + i.lc + "]";
                    indent(b, ind);
                    b.append("if (").append(inRange(l, i.lc)).append(" && ").append(l.c).append(".floats != null) {\n");
                    E nv = binop(a.op, new E(fa, el0), rvE, a.value);
                    indent(b, ind + 1);
                    b.append(fa).append(" = ").append(conv(nv, el0)).append(";\n");
                    indent(b, ind);
                    b.append("} else {\n");
                    E ov = new E("h_getF(" + l.c + ", " + box(i) + ")", el0);
                    E sv = binop(a.op, ov, rvE, a.value);
                    indent(b, ind + 1);
                    b.append("h_setF(").append(l.c).append(", ").append(box(i)).append(", ").append(conv(sv, el0))
                            .append(");\n");
                    indent(b, ind);
                    b.append("}\n");
                    return;
                }
                throw new Unsupported("float aug with " + rv.t);
            }
            String lv = fresh();
            String iv = fresh();
            indent(b, ind);
            b.append("PyList ").append(lv).append(" = ").append(l.c).append(";\n");
            indent(b, ind);
            b.append("Object ").append(iv).append(" = ").append(box(i)).append(";\n");
            Ty el = in.elem(l.t);
            E old = el.k == Ty.K.FLOAT ? new E("h_getF(" + lv + ", " + iv + ")", el)
                    : new E(unbox("h_get(" + lv + ", " + iv + ")", el), el);
            E v = binop(a.op, old, expr(a.value), a.value);
            indent(b, ind);
            if (el.k == Ty.K.FLOAT) {
                b.append("h_setF(").append(lv).append(", ").append(iv).append(", ").append(conv(v, el))
                        .append(");\n");
            } else {
                b.append("h_set(").append(lv).append(", ").append(iv).append(", ").append(box(v)).append(");\n");
            }
            return;
        }
        throw new Unsupported("augmented target");
    }

    // ---- None narrowing --------------------------------------------------

    /// Locals a true `test` proves are not None.
    private Set<String> whenTrue(Expr test) {
        Set<String> r = new HashSet<String>();
        if (test instanceof BoolOp && ((BoolOp) test).isAnd) {
            for (Expr v : ((BoolOp) test).values) {
                r.addAll(whenTrue(v));
            }
        } else if (test instanceof UnaryOp && "not".equals(((UnaryOp) test).op)) {
            r.addAll(whenFalse(((UnaryOp) test).operand));
        } else if (test instanceof Compare) {
            String n = noneTest((Compare) test);
            if (n != null && ((Compare) test).ops.get(0).equals("is not")) {
                r.add(n);
            }
        } else if (test instanceof Name && narrowable(((Name) test).id)) {
            Ty t = localType(((Name) test).id);
            if (t.k == Ty.K.INST && !customTruth(t.cls)) {
                r.add(((Name) test).id);
            }
        }
        return r;
    }

    /// Locals a false `test` proves are not None.
    private Set<String> whenFalse(Expr test) {
        Set<String> r = new HashSet<String>();
        if (test instanceof BoolOp && !((BoolOp) test).isAnd) {
            for (Expr v : ((BoolOp) test).values) {
                r.addAll(whenFalse(v));
            }
        } else if (test instanceof UnaryOp && "not".equals(((UnaryOp) test).op)) {
            r.addAll(whenTrue(((UnaryOp) test).operand));
        } else if (test instanceof Compare) {
            String n = noneTest((Compare) test);
            if (n != null && ((Compare) test).ops.get(0).equals("is")) {
                r.add(n);
            }
        }
        return r;
    }

    /// `x.attr` of a local `x`, the key narrowing keeps for that field.
    private String path(Attribute a) {
        if (a.value instanceof Name && sc.resolve(((Name) a.value).id) == Scope.Binding.LOCAL) {
            return ((Name) a.value).id + "." + a.attr;
        }
        return null;
    }

    private void forgetAttr(String attr) {
        String suffix = "." + attr;
        nonNull.removeIf(k -> k.endsWith(suffix));
    }

    private void forgetPathsOf(String local) {
        String prefix = local + ".";
        nonNull.removeIf(k -> k.startsWith(prefix));
    }

    private void forgetPaths() {
        nonNull.removeIf(k -> k.indexOf('.') >= 0);
    }

    private static boolean sliceOf(Expr e) {
        return e instanceof Subscript && ((Subscript) e).index instanceof Slice;
    }

    /// The three parts of a slice, boxed, `Py.NONE` for a missing one.
    private String sliceParts(Slice s) {
        Expr[] parts = {s.lower, s.upper, s.step};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(i == 0 ? "" : ", ").append(parts[i] == null ? "Py.NONE" : box(expr(parts[i])));
        }
        return sb.toString();
    }

    private boolean narrowable(String n) {
        return sc.resolve(n) == Scope.Binding.LOCAL && localType(n).nullable();
    }

    /// The local `x` of `x is None` / `x is not None`, or null.
    private String noneTest(Compare c) {
        if (c.ops.size() != 1 || !c.ops.get(0).startsWith("is")) {
            return null;
        }
        Expr r = c.comparators.get(0);
        if (!(r instanceof Constant) || ((Constant) r).kind != Constant.Kind.NONE) {
            return null;
        }
        if (c.left instanceof Name && narrowable(((Name) c.left).id)) {
            return ((Name) c.left).id;
        }
        return c.left instanceof Attribute ? path((Attribute) c.left) : null;
    }

    /// Every local `body` may store into.
    static Set<String> assigned(List<Stmt> body) {
        List<String> out = new ArrayList<String>();
        for (Stmt s : body) {
            collectStores(s, out);
        }
        return new HashSet<String>(out);
    }

    private static void collectStores(Object o, List<String> out) {
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                names(t, out);
            }
        } else if (o instanceof AugAssign) {
            names(((AugAssign) o).target, out);
        } else if (o instanceof For) {
            names(((For) o).target, out);
        } else if (o instanceof NamedExpr) {
            out.add(((NamedExpr) o).target.id);
        }
        Infer.children(o, x -> collectStores(x, out));
    }

    private static void names(Expr t, List<String> out) {
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

    // ---- fields ----------------------------------------------------------

    private Layouts.Cls owner(E r, String attr) {
        if (r.t.k != Ty.K.INST) {
            throw new Unsupported("attribute of " + r.t);
        }
        Layouts.Cls o = Infer.fieldOwner(r.t.cls, attr);
        if (o == null) {
            throw new Unsupported("not a field: " + attr);
        }
        return o;
    }

    private Ty fieldTy(E r, String attr) {
        Layouts.Cls o = owner(r, attr);
        Ty t = typedField(o, attr);
        return t != null ? t : in.fieldType(o, attr);
    }

    /// `r` checked not to be None before reaching its `attr`.
    private String nz(E r, String attr) {
        if (!r.t.opt) {
            return r.c;
        }
        if (plain(r.c)) {
            // Inline: the throwing path is the only call.
            return "(" + r.c + " != null ? " + r.c + " : Typed.<" + jt(r.t) + ">nzf(" + Emitter.javaString(attr)
                    + "))";
        }
        return "Typed.nz(" + r.c + ", " + Emitter.javaString(attr) + ")";
    }

    /// The Java lvalue of field `attr` of `r`.
    private String fieldRef(E r, String attr) {
        Layouts.Cls o = owner(r, attr);
        String recv = nz(r, attr);
        if (typedField(o, attr) != null) {
            return recv + ".t_" + Emitter.mangle(attr);
        }
        if (!in.definitelySet(o, attr)) {
            throw new Unsupported("field may be unset: " + attr);
        }
        return recv + ".f_" + Emitter.mangle(attr);
    }

    /// Reads of typed int fields through a helper, where the receiver is
    /// an expression that may not be evaluated twice nor moved.
    private final Set<String> fieldBoxers = new TreeSet<String>();

    /// Typed int field `attr` of `r`, as a pair when the receiver is (or
    /// may be put in) a local.
    private E intField(E r, String attr) {
        Layouts.Cls o = owner(r, attr);
        String m = Emitter.mangle(attr);
        String recv = nz(r, attr);
        if (!plain(recv) && pre != null && stmtPure && condDepth == 0) {
            String t = fresh();
            indent(pre, preInd);
            pre.append(o.javaName).append(' ').append(t).append(" = ").append(recv).append(";\n");
            recv = t;
        }
        if (plain(recv)) {
            return dual(recv + ".t_" + m, recv + ".b_" + m);
        }
        fieldBoxers.add(o.javaName + " " + m);
        return new E("fb_" + o.javaName + "_" + m + "(" + recv + ")", Ty.INT);
    }

    /// Stores `v` into typed int field `attr` of `r`; Python evaluates the
    /// value first.
    private void storeIntField(E r, String attr, E v, StringBuilder b, int ind) {
        String m = Emitter.mangle(attr);
        Layouts.Cls o = owner(r, attr);
        String recv = nz(r, attr);
        if (!plain(recv)) {
            String t = fresh();
            b.append(o.javaName).append(' ').append(t).append(" = ").append(recv).append(";\n");
            indent(b, ind);
            recv = t;
        }
        storePair(recv + ".t_" + m, recv + ".b_" + m, v, b, ind);
    }

    // ---- expressions -----------------------------------------------------

    /// `e` as a Java boolean, for a test: `and`, `or` and `not` need no
    /// Python value there.
    String testExpr(Expr e) {
        if (e instanceof BoolOp) {
            BoolOp o = (BoolOp) e;
            StringBuilder sb = new StringBuilder("(");
            String j = o.isAnd ? " && " : " || ";
            Set<String> saved = new HashSet<String>(nonNull);
            for (int i = 0; i < o.values.size(); i++) {
                Expr v = o.values.get(i);
                condDepth += i == 0 ? 0 : 1;
                try {
                    sb.append(i == 0 ? "" : j).append(testExpr(v));
                } finally {
                    condDepth -= i == 0 ? 0 : 1;
                }
                // A later operand runs only when the earlier ones were true
                // (and) or false (or).
                nonNull.addAll(o.isAnd ? whenTrue(v) : whenFalse(v));
            }
            nonNull = saved;
            return sb.append(')').toString();
        }
        if (e instanceof UnaryOp && "not".equals(((UnaryOp) e).op)) {
            return "!" + testExpr(((UnaryOp) e).operand);
        }
        return test(expr(e));
    }

    E expr(Expr e) {
        E sub = leafSubst.get(e);
        if (sub != null) {
            return sub;
        }
        if (e != plainExpr && (e instanceof BinOp || e instanceof UnaryOp || e instanceof IfExp //NOPMD CompareObjectsWithEquals
                || e instanceof Call && ranges.inlinable((Call) e) != null)) {
            E w = wrapped(e);
            if (w != null) {
                return w;
            }
        }
        if (e instanceof Constant) {
            Constant c = (Constant) e;
            switch (c.kind) {
                case NONE:
                    return new E("Py.NONE", Ty.NONE);
                case TRUE:
                    return new E("true", Ty.BOOL);
                case FALSE:
                    return new E("false", Ty.BOOL);
                case INT: {
                    Long v = intConstant(c);
                    return new E(em.constant(c), Ty.INT,
                            v != null && Math.abs(v.longValue()) < (1L << 60) ? v + "L" : null);
                }
                case FLOAT: {
                    if (c.text.endsWith("j") || c.text.endsWith("J")) {
                        throw new Unsupported("complex");
                    }
                    double d = Double.parseDouble(c.text.replace("_", ""));
                    return new E(Double.isInfinite(d) ? "Double.POSITIVE_INFINITY" : Double.toString(d), Ty.FLOAT);
                }
                case STR:
                    return new E(em.constant(c), Ty.STR);
                default:
                    throw new Unsupported("constant");
            }
        }
        if (e instanceof Name) {
            return name((Name) e);
        }
        if (e instanceof Attribute) {
            Attribute a = (Attribute) e;
            if (Infer.MATH_CONSTS.contains(a.attr) && in.isMath(a.value, sc)) {
                return new E(mathConstant(a.attr), Ty.FLOAT);
            }
            if (a.value instanceof Name && scalars.containsKey(((Name) a.value).id)) {
                return scalarRead(((Name) a.value).id, a.attr);
            }
            E r = expr(a.value);
            if (r.t.k != Ty.K.INST) {
                return new E(em.attrSite(a.attr) + ".get(" + box(r) + ")", Ty.DYN);
            }
            Ty ft = fieldTy(r, a.attr);
            if (ft.k == Ty.K.INT && typedField(owner(r, a.attr), a.attr) != null) {
                return intField(r, a.attr);
            }
            String p = path(a);
            return new E(fieldRef(r, a.attr), p != null && nonNull.contains(p) ? ft.nonNone() : ft);
        }
        if (e instanceof BinOp) {
            if (pre != null && arith(e)) {
                E h = hoistInt(e);
                if (h != null) {
                    return h;
                }
            }
            BinOp b = (BinOp) e;
            if ("+".equals(b.op) && b.left instanceof BinOp && "+".equals(((BinOp) b.left).op)) {
                E c = strChain(b);
                if (c != null) {
                    return c;
                }
            }
            return binop(b.op, expr(b.left), expr(b.right), b.right);
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            if ("not".equals(u.op)) {
                return new E("!" + testExpr(u.operand), Ty.BOOL);
            }
            E o = expr(u.operand);
            if (o.t.k == Ty.K.FLOAT) {
                return new E("-".equals(u.op) ? "(-" + o.c + ")" : o.c, Ty.FLOAT);
            }
            if (o.t.k == Ty.K.INT || o.t.k == Ty.K.BOOL) {
                switch (u.op) {
                    case "-":
                        return new E("Py.neg(" + box(o) + ")", Ty.INT);
                    case "~":
                        return new E("Py.invert(" + box(o) + ")", Ty.INT);
                    default:
                        if (o.t.k == Ty.K.INT) {
                            return o;
                        }
                }
            }
            throw new Unsupported("unary " + u.op + " " + o.t);
        }
        if (e instanceof BoolOp) {
            BoolOp o = (BoolOp) e;
            StringBuilder sb = new StringBuilder("(");
            String j = o.isAnd ? " && " : " || ";
            for (int i = 0; i < o.values.size(); i++) {
                condDepth += i == 0 ? 0 : 1;
                E v;
                try {
                    v = expr(o.values.get(i));
                } finally {
                    condDepth -= i == 0 ? 0 : 1;
                }
                if (v.t.k != Ty.K.BOOL) {
                    throw new Unsupported("and/or of " + v.t);
                }
                sb.append(i == 0 ? "" : j).append(v.c);
            }
            return new E(sb.append(')').toString(), Ty.BOOL);
        }
        if (e instanceof Compare) {
            Compare cm = (Compare) e;
            if (pre != null && cm.ops.size() == 1 && ORDERS.contains(cm.ops.get(0))
                    && (arith(cm.left) || arith(cm.comparators.get(0)) || numeric(cm))) {
                E h = hoistInt(e);
                if (h != null) {
                    return h;
                }
            }
            return compare((Compare) e);
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            String tc = testExpr(i.test);
            Set<String> saved = new HashSet<String>(nonNull);
            nonNull.addAll(whenTrue(i.test));
            condDepth++;
            E a;
            E b;
            try {
                a = expr(i.body);
                nonNull = new HashSet<String>(saved);
                nonNull.addAll(whenFalse(i.test));
                b = expr(i.orelse);
            } finally {
                condDepth--;
            }
            nonNull = saved;
            Ty t = Ty.join(a.t, b.t);
            if (t.k == Ty.K.BOT) {
                throw new Unsupported("if-expression of nothing");
            }
            return new E("(" + tc + " ? " + conv(a, t) + " : " + conv(b, t) + ")", t);
        }
        if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            if (s.index instanceof Slice) {
                E l = expr(s.value);
                if (l.t.k != Ty.K.LIST) {
                    throw new Unsupported("slice of " + l.t);
                }
                return new E("Typed.asList(Py.getslice(" + l.c + ", " + sliceParts((Slice) s.index) + "))",
                        l.t.nonNone());
            }
            E bits = structCast(s);
            if (bits != null) {
                return bits;
            }
            E l = expr(s.value);
            E i = expr(s.index);
            if (l.t.k != Ty.K.LIST || !(i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                return new E("Py.getitem(" + box(l) + ", " + box(i) + ")", Ty.DYN);
            }
            Ty el = in.elem(l.t);
            if (el.k == Ty.K.BOT) {
                throw new Unsupported("element of nothing");
            }
            String slow = el.k == Ty.K.FLOAT ? "h_getF(" + l.c + ", " + box(i) + ")" : "h_get(" + l.c + ", " + box(i) + ")";
            String fast = slow;
            String[] ix = index(i);
            if (el.k == Ty.K.INT && ix != null && plain(l.c) && pre != null && stmtPure && condDepth == 0) {
                // From the list's longs into a pair, before the statement.
                String r = fresh();
                String rb = fresh();
                indent(pre, preInd);
                pre.append("long ").append(r).append(";\n");
                indent(pre, preInd);
                pre.append("Object ").append(rb).append(" = null;\n");
                indent(pre, preInd);
                pre.append("if (").append(ix[1]).append(inRange(l, ix[0])).append(" && ").append(l.c)
                        .append(".ints != null) {\n");
                indent(pre, preInd + 1);
                pre.append(r).append(" = ").append(l.c).append(".ints[(int) ").append(ix[0]).append("];\n");
                indent(pre, preInd);
                pre.append("} else {\n");
                indent(pre, preInd + 1);
                pre.append(r).append(" = 0L;\n");
                indent(pre, preInd + 1);
                storePair(r, rb, new E(slow, Ty.INT), pre, preInd + 1);
                indent(pre, preInd);
                pre.append("}\n");
                return dual(r, rb);
            }
            if (i.lc != null && plain(l.c) && pre != null && stmtPure && condDepth == 0) {
                // Read into a local before the statement: no call in it can
                // change the list, and a Java local, unlike an operand of a
                // conditional expression, stays out of ParparVM's stack.
                String r = fresh();
                String in = inRange(l, i.lc);
                indent(pre, preInd);
                pre.append(el.k == Ty.K.FLOAT ? "double " : "Object ").append(r).append(";\n");
                indent(pre, preInd);
                if (el.k == Ty.K.FLOAT) {
                    pre.append("if (").append(in).append(" && ").append(l.c).append(".floats != null) {\n");
                    indent(pre, preInd + 1);
                    pre.append(r).append(" = ").append(l.c).append(".floats[(int) ").append(i.lc).append("];\n");
                } else {
                    pre.append("if (").append(in).append(" && ").append(l.c).append(".ints == null && ")
                            .append(l.c).append(".floats == null) {\n");
                    indent(pre, preInd + 1);
                    pre.append(r).append(" = ").append(l.c).append(".items[(int) ").append(i.lc).append("];\n");
                }
                indent(pre, preInd);
                pre.append("} else {\n");
                indent(pre, preInd + 1);
                pre.append(r).append(" = ").append(slow).append(";\n");
                indent(pre, preInd);
                pre.append("}\n");
                return el.k == Ty.K.FLOAT ? new E(r, el) : new E(unbox(r, el), el);
            }
            if (i.lc != null && plain(l.c)) {
                // The common case inline, no call: ParparVM inlines none.
                String in = inRange(l, i.lc);
                fast = el.k == Ty.K.FLOAT
                        ? "(" + in + " && " + l.c + ".floats != null ? " + l.c + ".floats[(int) " + i.lc + "] : " + slow + ")"
                        : "(" + in + " ? (" + l.c + ".ints != null ? (Object) Long.valueOf(" + l.c + ".ints[(int) " + i.lc
                        + "]) : " + l.c + ".floats == null ? " + l.c + ".items[(int) " + i.lc + "] : " + slow + ") : "
                        + slow + ")";
            }
            return el.k == Ty.K.FLOAT ? new E(fast, el) : new E(unbox(fast, el), el);
        }
        if (e instanceof TupleExpr) {
            StringBuilder sb = new StringBuilder("Py.tuple(");
            List<Expr> el = ((TupleExpr) e).elts;
            for (int i = 0; i < el.size(); i++) {
                if (el.get(i) instanceof Starred) {
                    throw new Unsupported("starred");
                }
                sb.append(i == 0 ? "" : ", ").append(box(expr(el.get(i))));
            }
            return new E(sb.append(')').toString(), Ty.DYN);
        }
        if (e instanceof ListExpr) {
            ListExpr l = (ListExpr) e;
            Integer site = in.siteOf.get(l);
            if (site == null) {
                throw new Unsupported("list site");
            }
            StringBuilder sb = new StringBuilder("Typed.list(new Object[] {");
            for (int i = 0; i < l.elts.size(); i++) {
                if (l.elts.get(i) instanceof Starred) {
                    throw new Unsupported("starred");
                }
                sb.append(i == 0 ? "" : ", ").append(box(expr(l.elts.get(i))));
            }
            return new E(sb.append("})").toString(), Ty.list(new int[] {site.intValue()}, false));
        }
        if (e instanceof Call) {
            return call((Call) e);
        }
        if (e instanceof DictExpr) {
            DictExpr d = (DictExpr) e;
            StringBuilder sb = new StringBuilder("Py.dict(");
            for (int i = 0; i < d.keys.size(); i++) {
                if (d.keys.get(i) == null) {
                    throw new Unsupported("dict spread");
                }
                sb.append(i == 0 ? "" : ", ").append(box(expr(d.keys.get(i)))).append(", ")
                        .append(box(expr(d.values.get(i))));
            }
            return new E(sb.append(')').toString(), Ty.DYN);
        }
        throw new Unsupported(e.getClass().getSimpleName());
    }

    private E name(Name nm) {
        String n = nm.id;
        switch (sc.resolve(n)) {
            case LOCAL: {
                if (!sc.params.contains(n) && !da.safe.contains(nm)) {
                    throw new Unsupported("maybe unbound " + n);
                }
                Ty t = localType(n);
                if (sc.deleted.contains(n)) {
                    throw new Unsupported("deleted " + n);
                }
                if (longLocals.contains(n)) {
                    return new E("Long.valueOf(" + var(n) + ")", Ty.INT, var(n));
                }
                String l = shadow.get(n);
                if (l != null) {
                    return new E("Long.valueOf(" + l + ")", t, l);
                }
                if (t.k == Ty.K.INT) {
                    return dual(var(n), big(n));
                }
                return new E(var(n), nonNull.contains(n) ? t.nonNone() : t);
            }
            case GLOBAL: {
                if (in.globalFns.containsKey(n) || in.globalClasses.containsKey(n)) {
                    throw new Unsupported("function as a value");
                }
                Ty t = in.globals.get(n);
                if (t == null || t.k == Ty.K.BOT) {
                    throw new Unsupported("global " + n);
                }
                return new E(unbox("g(" + em.slot(n) + ")", t), t);
            }
            default:
                throw new Unsupported("name " + n);
        }
    }

    private static String pyOp(String op) {
        switch (op) {
            case "+":
                return "add";
            case "-":
                return "sub";
            case "*":
                return "mul";
            case "/":
                return "truediv";
            case "//":
                return "floordiv";
            case "%":
                return "mod";
            case "**":
                return "pow";
            case "<<":
                return "lshift";
            case ">>":
                return "rshift";
            case "&":
                return "and";
            case "|":
                return "or";
            case "^":
                return "xor";
            default:
                throw new Unsupported("operator " + op);
        }
    }

    /// The list and float operations typed code uses, emitted into every
    /// module that has typed code: ParparVM does not inline a call into
    /// another class, and these run once per element. Each falls back to
    /// `Typed` (or the list's own method) for every case but the common one.
    private static final String LOCAL_HELPERS = String.join("\n", new String[] {
            "    static Object h_get(PyList l, Object i) {",
            "        if (l != null && i instanceof Long) {",
            "            long k = ((Long) i).longValue();",
            "            if (k >= 0 && k < l.size) {",
            "                int[] n = l.ints;",
            "                if (n != null) {",
            "                    return Long.valueOf(n[(int) k]);",
            "                }",
            "                if (l.floats == null) {",
            "                    return l.items[(int) k];",
            "                }",
            "            }",
            "        }",
            "        return Typed.get(l, i);",
            "    }",
            "    static double h_getF(PyList l, Object i) {",
            "        if (l != null && i instanceof Long) {",
            "            long k = ((Long) i).longValue();",
            "            double[] f = l.floats;",
            "            if (f != null && k >= 0 && k < l.size) {",
            "                return f[(int) k];",
            "            }",
            "        }",
            "        return Typed.getF(l, i);",
            "    }",
            "    static void h_set(PyList l, Object i, Object v) {",
            "        if (l != null && i instanceof Long && l.ints == null && l.floats == null) {",
            "            long k = ((Long) i).longValue();",
            "            if (k >= 0 && k < l.size) {",
            "                l.items[(int) k] = v;",
            "                return;",
            "            }",
            "        }",
            "        Typed.set(l, i, v);",
            "    }",
            "    static void h_setF(PyList l, Object i, double v) {",
            "        if (l != null && i instanceof Long) {",
            "            long k = ((Long) i).longValue();",
            "            double[] f = l.floats;",
            "            if (f != null && k >= 0 && k < l.size) {",
            "                f[(int) k] = v;",
            "                return;",
            "            }",
            "        }",
            "        Typed.setF(l, i, v);",
            "    }",
            "    static Object h_len(PyList l) {",
            "        return l != null ? Long.valueOf(l.size) : Typed.len(l);",
            "    }",
            "    static double h_toFloat(Object i) {",
            "        return i instanceof Long ? (double) ((Long) i).longValue() : Typed.toFloat(i);",
            "    }",
            "    static double h_div(double a, double b) {",
            "        return b != 0 ? a / b : Typed.div(a, b);",
            "    }",
            ""
    });

    // ---- inline int arithmetic -------------------------------------------

    /// Whether `e` is an inner node of hoisted arithmetic: an operation
    /// on ints or floats that `longOf`/`doubleOf` spell out. Anything else
    /// under one is a leaf, evaluated once, in order, before it.
    private static boolean arith(Expr e) {
        if (e instanceof UnaryOp) {
            return "-".equals(((UnaryOp) e).op);
        }
        if (!(e instanceof BinOp)) {
            return false;
        }
        BinOp b = (BinOp) e;
        switch (b.op) {
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
            case "//":
            case "&":
            case "|":
            case "^":
                return true;
            case ">>": {
                Long c = intConstant(b.right);
                return c != null && c.longValue() >= 0 && c.longValue() < 64;
            }
            default:
                return false;
        }
    }

    /// The leaves of arithmetic `e` in evaluation order, or null when one
    /// may not run before the statement: only a local or a constant may,
    /// unless every call in the statement is pure and none of it runs
    /// conditionally.
    private List<Expr> leaves(Expr e) {
        List<Expr> out = new ArrayList<Expr>();
        topLeaves = e == hoistTop; //NOPMD CompareObjectsWithEquals - identity is meant
        try {
            return collectLeaves(e, out) ? out : null;
        } finally {
            topLeaves = false;
        }
    }

    private boolean collectLeaves(Expr e, List<Expr> out) {
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            return collectLeaves(c.left, out) && collectLeaves(c.comparators.get(0), out);
        }
        if (arith(e)) {
            if (e instanceof UnaryOp) {
                return collectLeaves(((UnaryOp) e).operand, out);
            }
            return collectLeaves(((BinOp) e).left, out) && collectLeaves(((BinOp) e).right, out);
        }
        boolean quiet = e instanceof Constant || e instanceof Name && sc.resolve(((Name) e).id) == Scope.Binding.LOCAL;
        if (!quiet && !((stmtPure || topLeaves) && condDepth == 0)) {
            return false;
        }
        out.add(e);
        return true;
    }

    /// 'i' (int), 'f' (float) or 0 (neither) for arithmetic `e` over the
    /// evaluated leaves.
    private char kindOf(Expr e) {
        E v = leafSubst.get(e);
        if (v != null) {
            switch (v.t.k) {
                case INT:
                case BOOL:
                    return 'i';
                case FLOAT:
                    return 'f';
                default:
                    return 0;
            }
        }
        if (e instanceof UnaryOp) {
            return kindOf(((UnaryOp) e).operand);
        }
        BinOp b = (BinOp) e;
        char l = kindOf(b.left);
        char r = kindOf(b.right);
        if (l == 0 || r == 0) {
            return 0;
        }
        switch (b.op) {
            case "/":
                return 'f';
            case "+":
            case "-":
            case "*":
                return l == 'f' || r == 'f' ? 'f' : 'i';
            default:
                return l == 'i' && r == 'i' ? 'i' : 0;
        }
    }

    /// `e`, int or float arithmetic (or a comparison of it), computed on
    /// Java longs and doubles in a block before the statement, falling back
    /// to the generic operations when an operand is a big int or a step
    /// overflows; or null when its leaves may not move there. ParparVM
    /// inlines no call, not even to a helper of the same class, so each
    /// operation is spelled out here.
    private E hoistInt(Expr e) {
        List<Expr> ls = leaves(e);
        if (ls == null) {
            return null;
        }
        boolean impureOk = e == hoistTop; //NOPMD CompareObjectsWithEquals - identity is meant
        Map<Expr, E> savedLeaves = leafSubst;
        leafSubst = new IdentityHashMap<Expr, E>(savedLeaves);
        try {
            List<Expr> pushedLeaves = new ArrayList<Expr>();
            for (Expr x : ls) {
                Expr savedLeaf = pushLeaf;
                boolean savedOk = leafImpureOk;
                pushLeaf = x;
                leafImpureOk = impureOk;
                E v;
                try {
                    v = expr(x);
                } finally {
                    pushLeaf = savedLeaf;
                    leafImpureOk = savedOk;
                }
                if (v.pk != null) {
                    pushedLeaves.add(x);
                    leafSubst.put(x, v);
                } else {
                    leafSubst.put(x, spill(v));
                }
            }
            char k;
            if (e instanceof Compare) {
                Compare c = (Compare) e;
                char a = kindOf(c.left);
                char b = kindOf(c.comparators.get(0));
                k = a == 0 || b == 0 ? 0 : a == 'f' || b == 'f' ? 'f' : 'i';
            } else {
                k = kindOf(e);
            }
            if (k == 0) {
                // Taken here, the last first: the generic path reads each
                // leaf as an object.
                for (int i = pushedLeaves.size() - 1; i >= 0; i--) {
                    String r = leafSubst.get(pushedLeaves.get(i)).pk;
                    String rb = fresh();
                    indent(pre, preInd);
                    pre.append("Object ").append(rb).append(" = ").append(r)
                            .append(" != Long.MIN_VALUE ? null : Typed.take();\n");
                    leafSubst.put(pushedLeaves.get(i), dual(r, rb));
                }
                return generic0(e);
            }
            return fastPath(e, ls, k);
        } finally {
            leafSubst = savedLeaves;
        }
    }

    /// `e` by the generic operations, over the leaves already evaluated.
    private E generic0(Expr e) {
        StringBuilder saved = pre;
        pre = null;
        try {
            return e instanceof Compare ? compare((Compare) e) : expr(e);
        } finally {
            pre = saved;
        }
    }

    /// `v` in plain Java locals, so it may be read twice: declared in `pre`.
    private E spill(E v) {
        if (v.ll != null || plain(v.c) && v.lc == null || v.lc != null && (plain(v.lc) || literal(v.lc))) {
            return v;
        }
        return spillTo(v, pre, preInd);
    }

    /// `v` copied into fresh Java locals declared in `into`.
    private E spillTo(E v, StringBuilder into, int ind) {
        String t = fresh();
        indent(into, ind);
        if (v.lc != null) {
            into.append("long ").append(t).append(" = ").append(v.lc).append(";\n");
            return new E("Long.valueOf(" + t + ")", v.t, t);
        }
        if (v.t.k == Ty.K.INT) {
            String tb = fresh();
            into.append("long ").append(t).append(" = 0L;\n");
            indent(into, ind);
            into.append("Object ").append(tb).append(" = null;\n");
            indent(into, ind);
            storePair(t, tb, v, into, ind);
            return dual(t, tb);
        }
        into.append(jt(v.t)).append(' ').append(t).append(" = ").append(v.c).append(";\n");
        return new E(t, v.t);
    }

    private static final Set<String> ORDERS = new HashSet<String>(Arrays.asList("<", "<=", ">", ">=", "==", "!="));

    /// Whether comparison `c` may be of numbers: no side is None.
    private static boolean numeric(Compare c) {
        return !(c.left instanceof Constant && ((Constant) c.left).kind == Constant.Kind.NONE)
                && !(c.comparators.get(0) instanceof Constant
                && ((Constant) c.comparators.get(0)).kind == Constant.Kind.NONE);
    }

    private E fastPath(Expr e, List<Expr> ls, char k) {
        Ty t = e instanceof Compare ? Ty.BOOL : k == 'f' ? Ty.FLOAT : Ty.INT;
        // The generic path goes into a method of its own: with it inline, a
        // function made only of such arithmetic would call out, and a
        // ParparVM function that calls out gets a stack guard and is no
        // longer inlined by the C compiler.
        Map<Expr, E> sub = new IdentityHashMap<Expr, E>(leafSubst);
        StringBuilder params = new StringBuilder();
        StringBuilder args = new StringBuilder();
        StringBuilder takes = new StringBuilder();
        for (int i = 0; i < ls.size(); i++) {
            E v = leafSubst.get(ls.get(i));
            String pn = "p" + i;
            String sep = i == 0 ? "" : ", ";
            if (v.pk != null) {
                // Passed bare and taken in the slow path, the last first,
                // so the caller holds no object (see Typed.take).
                params.append(sep).append("long ").append(pn);
                args.append(sep).append(v.pk);
                takes.insert(0, "        Object o" + i + " = " + pn + " != Long.MIN_VALUE ? Long.valueOf(" + pn
                        + ") : Typed.take();\n");
                sub.put(ls.get(i), new E("o" + i, Ty.INT));
                continue;
            }
            params.append(sep).append(jt(v.t)).append(' ').append(pn);
            args.append(sep).append(v.t.k == Ty.K.INT ? box(v) : v.c);
            sub.put(ls.get(i), new E(pn, v.t));
        }
        boolean push = t.k == Ty.K.INT && e == pushTop; //NOPMD CompareObjectsWithEquals - identity is meant
        String generic;
        Map<Expr, E> saved = leafSubst;
        leafSubst = sub;
        try {
            E g = generic0(e);
            String m = "S" + (slowCount++);
            String body = t.k == Ty.K.FLOAT ? toD(g) : conv(g, t);
            slow.append("    static ").append(push ? "long" : jt(t)).append(' ').append(m).append('(').append(params)
                    .append(") {\n").append(takes).append("        return ")
                    .append(push ? "Typed.retObj(" + box(new E(body, t)) + ")" : body).append(";\n    }\n");
            generic = m + "(" + args + ")";
        } finally {
            leafSubst = saved;
        }
        String done = fresh();
        String fast = fresh();
        StringBuilder f = new StringBuilder();
        Map<String, String> guards = new HashMap<String, String>();
        String v;
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            String op = c.ops.get(0);
            if (k == 'f') {
                v = doubleOf(c.left, f, guards, fast) + " " + op + " " + doubleOf(c.comparators.get(0), f, guards, fast);
            } else {
                v = longOf(c.left, f, guards, fast) + " " + op + " " + longOf(c.comparators.get(0), f, guards, fast);
            }
        } else if (k == 'f') {
            v = doubleOf(e, f, guards, fast);
        } else {
            v = longOf(e, f, guards, fast);
        }
        String r = fresh();
        String rb = t.k == Ty.K.INT && !push ? fresh() : null;
        int ind = preInd;
        if (push) {
            // The marker stands for a pushed result only: a true
            // Long.MIN_VALUE goes the slow way, and is pushed.
            String x = fresh();
            f.append("long ").append(x).append(" = ").append(v).append(";\nif (").append(x)
                    .append(" == Long.MIN_VALUE) {\n    break ").append(fast).append(";\n}\n");
            v = x;
        }
        boolean leaves = f.toString().contains("break " + fast + ";");
        indent(pre, ind);
        pre.append(t.k == Ty.K.INT ? "long" : jt(t)).append(' ').append(r).append(";\n");
        if (!leaves) {
            // Nothing in the fast path can leave it (no operand that may be
            // a big int, no operation that may overflow): the generic
            // fallback would be unreachable, which javac rejects.
            for (String line : f.toString().split("\n")) {
                if (!line.isEmpty()) {
                    indent(pre, ind);
                    pre.append(line).append('\n');
                }
            }
            indent(pre, ind);
            pre.append(r).append(" = ").append(v).append(";\n");
            return t.k == Ty.K.INT ? new E("Long.valueOf(" + r + ")", t, r) : new E(r, t);
        }
        if (push) {
            indent(pre, ind);
            pre.append(done).append(": {\n");
            indent(pre, ind + 1);
            pre.append(fast).append(": {\n");
            for (String line : f.toString().split("\n")) {
                indent(pre, ind + 2);
                pre.append(line).append('\n');
            }
            indent(pre, ind + 2);
            pre.append(r).append(" = ").append(v).append(";\n");
            indent(pre, ind + 2);
            pre.append("break ").append(done).append(";\n");
            indent(pre, ind + 1);
            pre.append("}\n");
            indent(pre, ind + 1);
            pre.append(r).append(" = ").append(generic).append(";\n");
            indent(pre, ind);
            pre.append("}\n");
            return pushed(r);
        }
        if (rb != null) {
            indent(pre, ind);
            pre.append("Object ").append(rb).append(" = null;\n");
        }
        indent(pre, ind);
        pre.append(done).append(": {\n");
        indent(pre, ind + 1);
        pre.append(fast).append(": {\n");
        for (String line : f.toString().split("\n")) {
            indent(pre, ind + 2);
            pre.append(line).append('\n');
        }
        indent(pre, ind + 2);
        pre.append(r).append(" = ").append(v).append(";\n");
        indent(pre, ind + 2);
        pre.append("break ").append(done).append(";\n");
        indent(pre, ind + 1);
        pre.append("}\n");
        indent(pre, ind + 1);
        if (rb == null) {
            pre.append(r).append(" = ").append(generic).append(";\n");
        } else {
            String g = fresh();
            pre.append("Object ").append(g).append(" = ").append(generic).append(";\n");
            indent(pre, ind + 1);
            pre.append("if (").append(g).append(" instanceof Long) {\n");
            indent(pre, ind + 2);
            pre.append(r).append(" = ((Long) ").append(g).append(").longValue();\n");
            indent(pre, ind + 1);
            pre.append("} else {\n");
            indent(pre, ind + 2);
            pre.append(r).append(" = 0L;\n");
            indent(pre, ind + 2);
            pre.append(rb).append(" = ").append(g).append(";\n");
            indent(pre, ind + 1);
            pre.append("}\n");
        }
        indent(pre, ind);
        pre.append("}\n");
        return rb == null ? new E(r, t) : dual(r, rb);
    }

    /// Statements into `f` computing `e` as a double; the expression
    /// holding it.
    private String doubleOf(Expr e, StringBuilder f, Map<String, String> guards, String fast) {
        if (kindOf(e) == 'i') {
            String x = longOf(e, f, guards, fast);
            String d = fresh();
            // Exact below 2^53; Python rounds a wider int the same way, but
            // the generic path is the one that knows.
            f.append("if (").append(x).append(" > 9007199254740992L || ").append(x)
                    .append(" < -9007199254740992L) {\n    break ").append(fast).append(";\n}\ndouble ").append(d)
                    .append(" = (double) ").append(x).append(";\n");
            return d;
        }
        E lv = leafSubst.get(e);
        if (lv != null) {
            return lv.c;
        }
        String d = fresh();
        if (e instanceof UnaryOp) {
            f.append("double ").append(d).append(" = -").append(doubleOf(((UnaryOp) e).operand, f, guards, fast))
                    .append(";\n");
            return d;
        }
        BinOp b = (BinOp) e;
        String x = doubleOf(b.left, f, guards, fast);
        String y = doubleOf(b.right, f, guards, fast);
        if ("/".equals(b.op)) {
            f.append("if (").append(y).append(" == 0) {\n    break ").append(fast).append(";\n}\n");
        }
        f.append("double ").append(d).append(" = ").append(x).append(' ').append(b.op).append(' ').append(y)
                .append(";\n");
        return d;
    }

    /// Statements into `f` computing int `e` as a long; the name holding it.
    private String longOf(Expr e, StringBuilder f, Map<String, String> guards, String fast) {
        E lv = leafSubst.get(e);
        if (lv != null) {
            if (lv.lc != null) {
                return lv.lc;
            }
            if (lv.t.k == Ty.K.BOOL) {
                return "(" + lv.c + " ? 1L : 0L)";
            }
            if (lv.pk != null) {
                if (guards.put(lv.pk, lv.pk) == null) {
                    f.append("if (").append(lv.pk).append(" == Long.MIN_VALUE) {\n    break ").append(fast)
                            .append(";\n}\n");
                }
                return lv.pk;
            }
            if (lv.ll != null) {
                if (guards.put(lv.bg, lv.ll) == null) {
                    f.append("if (").append(lv.bg).append(" != null) {\n    break ").append(fast).append(";\n}\n");
                }
                return lv.ll;
            }
            String d = guards.get(lv.c);
            if (d == null) {
                d = fresh();
                guards.put(lv.c, d);
                f.append("if (!(").append(lv.c).append(" instanceof Long)) {\n    break ").append(fast)
                        .append(";\n}\nlong ").append(d).append(" = ((Long) ").append(lv.c).append(").longValue();\n");
            }
            return d;
        }
        String d = fresh();
        if (e instanceof UnaryOp) {
            String x = longOf(((UnaryOp) e).operand, f, guards, fast);
            f.append("if (").append(x).append(" == Long.MIN_VALUE) {\n    break ").append(fast).append(";\n}\n")
                    .append("long ").append(d).append(" = -").append(x).append(";\n");
            return d;
        }
        BinOp b = (BinOp) e;
        String x = longOf(b.left, f, guards, fast);
        String y = longOf(b.right, f, guards, fast);
        switch (b.op) {
            case "+":
                f.append("long ").append(d).append(" = ").append(x).append(" + ").append(y).append(";\n")
                        .append("if (((").append(x).append(" ^ ").append(d).append(") & (").append(y).append(" ^ ")
                        .append(d).append(")) < 0) {\n    break ").append(fast).append(";\n}\n");
                break;
            case "-":
                f.append("long ").append(d).append(" = ").append(x).append(" - ").append(y).append(";\n")
                        .append("if (((").append(x).append(" ^ ").append(y).append(") & (").append(x).append(" ^ ")
                        .append(d).append(")) < 0) {\n    break ").append(fast).append(";\n}\n");
                break;
            case "*":
                f.append("long ").append(d).append(" = ").append(x).append(" * ").append(y).append(";\n");
                // Exact when the high half of the 128-bit product is the
                // sign of the low half (Math.multiplyHigh, spelled out).
                f.append("if (").append(mulOverflow(x, y, d)).append(") {\n    break ").append(fast).append(";\n}\n");
                break;
            case "%":
                // Python's remainder takes the divisor's sign.
                f.append("if (").append(y).append(" == 0L) {\n    break ").append(fast).append(";\n}\n")
                        .append("long ").append(d).append(" = ").append(x).append(" % ").append(y).append(";\n")
                        .append("if (").append(d).append(" != 0L && (").append(d).append(" ^ ").append(y)
                        .append(") < 0L) {\n    ").append(d).append(" += ").append(y).append(";\n}\n");
                break;
            case "//":
                // Floor division; MIN // -1 is the one quotient a long cannot hold.
                f.append("if (").append(y).append(" == 0L || ").append(x).append(" == Long.MIN_VALUE && ").append(y)
                        .append(" == -1L) {\n    break ").append(fast).append(";\n}\n")
                        .append("long ").append(d).append(" = ").append(x).append(" / ").append(y).append(";\n")
                        .append("if (").append(x).append(" % ").append(y).append(" != 0L && (").append(x).append(" ^ ")
                        .append(y).append(") < 0L) {\n    ").append(d).append("--;\n}\n");
                break;
            default:
                f.append("long ").append(d).append(" = ").append(x).append(' ').append(b.op).append(' ').append(y)
                        .append(";\n");
                break;
        }
        return d;
    }

    /// Java for "`x * y` (whose low 64 bits are in `d`) overflowed": the
    /// cheap test for operands of 31 bits, then the exact one.
    private static String mulOverflow(String x, String y, String d) {
        return "((" + x + " + 0x80000000L) | (" + y + " + 0x80000000L)) >>> 32 != 0 && (" + y + " != 0 && (" + d
                + " / " + y + " != " + x + " || " + x + " == Long.MIN_VALUE && " + y + " == -1L))";
    }

    /// Helpers emitted into this module: an int operation with the `Long`
    /// fast path inline, so the common case costs no call into the runtime.
    private final Set<String> intHelpers = new TreeSet<String>();

    private static final Set<String> INT_HELPED = new HashSet<String>(Arrays.asList("add", "sub", "mul", "and", "or",
            "xor", "rshift", "lt", "le", "gt", "ge", "eq"));

    /// `f` (a `Py` operation, or a comparison) of two boxed ints.
    private String intOp(String f, String a, String b, Expr right) {
        if (!INT_HELPED.contains(f)) {
            return "Py." + f + "(" + a + ", " + b + ")";
        }
        Long c = intConstant(right);
        if (c != null && (!f.equals("rshift") || c.longValue() >= 0 && c.longValue() < 64)) {
            intHelpers.add(f + "c");
            return "i_" + f + "c(" + a + ", " + c + "L)";
        }
        if ("rshift".equals(f)) {
            return "Py.rshift(" + a + ", " + b + ")";
        }
        intHelpers.add(f);
        return "i_" + f + "(" + a + ", " + b + ")";
    }

    private static String intHelper(String h) {
        boolean c = h.endsWith("c") && !"c".equals(h);
        String f = c ? h.substring(0, h.length() - 1) : h;
        boolean cmp = "lt".equals(f) || "le".equals(f) || "gt".equals(f) || "ge".equals(f) || "eq".equals(f);
        String y = c ? "y" : "((Long) b).longValue()";
        String guard = c ? "a instanceof Long" : "a instanceof Long && b instanceof Long";
        String bo = c ? "Long.valueOf(y)" : "b";
        StringBuilder o = new StringBuilder("    static ").append(cmp ? "boolean" : "Object").append(" i_").append(h)
                .append("(Object a, ").append(c ? "long y" : "Object b").append(") {\n        if (").append(guard)
                .append(") {\n            long x = ((Long) a).longValue();\n");
        if (!c) {
            o.append("            long y = ").append(y).append(";\n");
        }
        switch (f) {
            case "add":
                o.append("            long s = x + y;\n            if (((x ^ s) & (y ^ s)) >= 0) {\n")
                        .append("                return Long.valueOf(s);\n            }\n");
                break;
            case "sub":
                o.append("            long s = x - y;\n            if (((x ^ y) & (x ^ s)) >= 0) {\n")
                        .append("                return Long.valueOf(s);\n            }\n");
                break;
            case "mul":
                // Exact when both fit 31 bits; otherwise the runtime decides.
                o.append("            if (((x + 0x80000000L) | (y + 0x80000000L)) >>> 32 == 0) {\n")
                        .append("                return Long.valueOf(x * y);\n            }\n");
                break;
            case "and":
                o.append("            return Long.valueOf(x & y);\n");
                break;
            case "or":
                o.append("            return Long.valueOf(x | y);\n");
                break;
            case "xor":
                o.append("            return Long.valueOf(x ^ y);\n");
                break;
            case "rshift":
                o.append("            return Long.valueOf(x >> y);\n");
                break;
            default: {
                String jop = "lt".equals(f) ? "<" : "le".equals(f) ? "<=" : "gt".equals(f) ? ">" : "ge".equals(f)
                        ? ">=" : "==";
                o.append("            return x ").append(jop).append(" y;\n");
            }
        }
        o.append("        }\n        return ");
        if (cmp) {
            o.append("eq".equals(f) ? "Py.eqBool(a, " + bo + ")" : "Py.isTrue(Py." + f + "(a, " + bo + "))");
        } else {
            o.append("Py.").append(f).append("(a, ").append(bo).append(")");
        }
        return o.append(";\n    }\n").toString();
    }

    /// `a + b + c ...` of strs, three or more: one `StringBuilder` sized
    /// for the result, where nested concatenation made a string per `+`;
    /// a `str(i)` of an int is appended as the long itself. Null when the
    /// chain is not of strs.
    private E strChain(BinOp b) {
        List<Expr> ops = new ArrayList<Expr>();
        Expr x = b;
        while (x instanceof BinOp && "+".equals(((BinOp) x).op)) {
            ops.add(0, ((BinOp) x).right);
            x = ((BinOp) x).left;
        }
        ops.add(0, x);
        for (Expr o : ops) {
            Ty t = in.type(o, cur, sc);
            if (t.k != Ty.K.STR || t.opt) {
                return null;
            }
        }
        StringBuilder sb = new StringBuilder();
        int cap = 0;
        for (Expr o : ops) {
            if (o instanceof Call && ((Call) o).func instanceof Name && "str".equals(((Name) ((Call) o).func).id)
                    && ((Call) o).args.size() == 1 && ((Call) o).keywords.isEmpty()
                    && !(((Call) o).args.get(0) instanceof Starred)
                    && sc.resolve("str") == Scope.Binding.BUILTIN) {
                E v = expr(((Call) o).args.get(0));
                if (v.t.k == Ty.K.INT && v.lc != null && !v.t.opt) {
                    sb.append(".append(").append(v.lc).append(')');
                    cap += 20;
                    continue;
                }
                E sv = builtin("str", java.util.Collections.singletonList(v));
                if (sv == null || sv.t.k != Ty.K.STR || sv.t.opt) {
                    throw new Unsupported("str() of " + v.t);
                }
                sb.append(".append(").append(sv.c).append(')');
                cap += 16;
                continue;
            }
            E v = expr(o);
            if (v.t.k != Ty.K.STR || v.t.opt) {
                throw new Unsupported("str chain over " + v.t);
            }
            cap += o instanceof Constant ? v.c.length() - 2 : 16;
            sb.append(".append(").append(v.c).append(')');
        }
        return new E("new StringBuilder(" + cap + ")" + sb + ".toString()", Ty.STR);
    }

    E binop(String op, E l, E r, Expr right) {
        Ty t = Infer.binop(op, l.t, r.t, right);
        if (l.t.k == Ty.K.LIST && "*".equals(op) && (r.t.k == Ty.K.INT || r.t.k == Ty.K.BOOL)) {
            // `[x] * n`: Infer gave the product the sites of the literal.
            return new E("Typed.asList(Py.mul(" + l.c + ", " + box(r) + "))", Ty.list(l.t.sites, false));
        }
        switch (t.k) {
            case FLOAT: {
                if ((l.t.k == Ty.K.INT || l.t.k == Ty.K.BOOL) && (r.t.k == Ty.K.INT || r.t.k == Ty.K.BOOL)) {
                    return new E("Typed.truediv(" + box(l) + ", " + box(r) + ")", Ty.FLOAT);
                }
                String a = toD(l);
                String b = toD(r);
                switch (op) {
                    case "+":
                    case "-":
                    case "*":
                        return new E("(" + a + " " + op + " " + b + ")", Ty.FLOAT);
                    case "/":
                        return new E("h_div(" + a + ", " + b + ")", Ty.FLOAT);
                    case "//":
                        return new E("Typed.floordiv(" + a + ", " + b + ")", Ty.FLOAT);
                    case "%":
                        return new E("Typed.mod(" + a + ", " + b + ")", Ty.FLOAT);
                    default:
                        throw new Unsupported("float " + op);
                }
            }
            case INT:
                return new E(intOp(pyOp(op), box(l), box(r), right), Ty.INT);
            case STR:
                return new E("Typed.concat(" + l.c + ", " + r.c + ")", Ty.STR);
            case BOT:
                throw new Unsupported("operation on nothing");
            default:
                return new E("Py." + pyOp(op) + "(" + box(l) + ", " + box(r) + ")", Ty.DYN);
        }
    }

    private static boolean isNone(E e) {
        return e.t.k == Ty.K.NONE;
    }

    private String isNoneTest(E x) {
        if (x.t.nullable()) {
            return "(" + x.c + " == null)";
        }
        if ("Object".equals(jt(x.t))) {
            return "(" + x.c + " == Py.NONE)";
        }
        if (plain(x.c)) {
            return "false";
        }
        throw new Unsupported("is None of " + x.t);
    }

    private E compare(Compare c) {
        if (c.ops.size() != 1) {
            throw new Unsupported("chained comparison");
        }
        String op = c.ops.get(0);
        E l = expr(c.left);
        E r = expr(c.comparators.get(0));
        if ("is".equals(op) || "is not".equals(op) || isNone(l) || isNone(r)) {
            boolean neg = "is not".equals(op) || "!=".equals(op);
            if (!("is".equals(op) || "is not".equals(op) || "==".equals(op) || "!=".equals(op))) {
                throw new Unsupported("ordering against None");
            }
            String s;
            if (isNone(r)) {
                s = isNoneTest(l);
            } else if (isNone(l)) {
                s = isNoneTest(r);
            } else if (l.t.k == Ty.K.INST && r.t.k == Ty.K.INST || l.t.k == Ty.K.LIST && r.t.k == Ty.K.LIST) {
                s = "(" + l.c + " == " + r.c + ")";
            } else if (op.startsWith("is")) {
                s = "(" + box(l) + " == " + box(r) + ")";
            } else {
                throw new Unsupported("== on " + l.t + ", " + r.t);
            }
            return new E(neg ? "!" + s : s, Ty.BOOL);
        }
        boolean li = l.t.k == Ty.K.INT || l.t.k == Ty.K.BOOL;
        boolean ri = r.t.k == Ty.K.INT || r.t.k == Ty.K.BOOL;
        if ("in".equals(op) || "not in".equals(op)) {
            if (r.t.k == Ty.K.LIST) {
                String s = "Typed.nzl(" + r.c + ").contains(" + box(l) + ")";
                return new E("in".equals(op) ? s : "!" + s, Ty.BOOL);
            }
            throw new Unsupported("in " + r.t);
        }
        if (l.t.k == Ty.K.BOOL && r.t.k == Ty.K.BOOL && ("==".equals(op) || "!=".equals(op))) {
            return new E("(" + l.c + " " + op + " " + r.c + ")", Ty.BOOL);
        }
        if (li && ri) {
            String f;
            switch (op) {
                case "<":
                    f = "lt";
                    break;
                case "<=":
                    f = "le";
                    break;
                case ">":
                    f = "gt";
                    break;
                case ">=":
                    f = "ge";
                    break;
                case "==":
                    f = "eq";
                    break;
                case "!=":
                    return new E("!" + intOp("eq", box(l), box(r), c.comparators.get(0)), Ty.BOOL);
                default:
                    throw new Unsupported("compare " + op);
            }
            return new E(intOp(f, box(l), box(r), c.comparators.get(0)), Ty.BOOL);
        }
        if ((l.t.k == Ty.K.FLOAT || li) && (r.t.k == Ty.K.FLOAT || ri)) {
            return new E("(" + toD(l) + " " + op + " " + toD(r) + ")", Ty.BOOL);
        }
        if (l.t.k == Ty.K.STR && r.t.k == Ty.K.STR && !l.t.opt && !r.t.opt
                && ("==".equals(op) || "!=".equals(op))) {
            String s = l.c + ".equals(" + r.c + ")";
            return new E("==".equals(op) ? s : "!" + s, Ty.BOOL);
        }
        if (l.t.k == Ty.K.INST && r.t.k == Ty.K.INST && ("==".equals(op) || "!=".equals(op))
                && !customCompare(l.t.cls) && !customCompare(r.t.cls)) {
            return new E("(" + l.c + " " + op + " " + r.c + ")", Ty.BOOL);
        }
        String f;
        switch (op) {
            case "==":
                f = "eq";
                break;
            case "!=":
                f = "ne";
                break;
            case "<":
                f = "lt";
                break;
            case "<=":
                f = "le";
                break;
            case ">":
                f = "gt";
                break;
            case ">=":
                f = "ge";
                break;
            default:
                throw new Unsupported("compare " + op);
        }
        // Infer types this DYN too: a user comparison may answer anything.
        return new E("Py." + f + "(" + box(l) + ", " + box(r) + ")", Ty.DYN);
    }

    private boolean customCompare(Layouts.Cls k) {
        for (Layouts.Cls c : layouts.classes.values()) {
            if (Infer.isSelfOrAncestor(k, c) || Infer.isSelfOrAncestor(c, k)) {
                if (c.bodyNames.contains("__eq__") || c.bodyNames.contains("__ne__")) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- calls -----------------------------------------------------------

    private List<E> args(Call c) {
        if (!c.keywords.isEmpty()) {
            throw new Unsupported("keyword arguments");
        }
        List<E> r = new ArrayList<E>();
        for (Expr a : c.args) {
            if (a instanceof Starred) {
                throw new Unsupported("starred argument");
            }
            r.add(expr(a));
        }
        return r;
    }

    /// The Java argument list for `f`, from its parameter `from` on;
    /// missing trailing arguments take their constant defaults.
    private String argList(Infer.Fn f, int from, List<E> args) {
        int n = f.params.length - from;
        if (args.size() > n) {
            throw new Unsupported("too many arguments");
        }
        StringBuilder sb = new StringBuilder();
        List<Expr> defs = f.def.args.defaults;
        for (int i = 0; i < n; i++) {
            E a;
            if (i < args.size()) {
                a = args.get(i);
            } else {
                int d = defs.size() - (f.params.length - (from + i));
                if (d < 0 || !(defs.get(d) instanceof Constant)) {
                    throw new Unsupported("missing argument");
                }
                a = expr(defs.get(d));
            }
            sb.append(i == 0 ? "" : ", ").append(arg(a, f.params[from + i]));
        }
        return sb.toString();
    }

    /// The arguments of a call of `f`'s long version: its int parameters
    /// as longs (the call's ranges proved each fits).
    private String longArgList(Infer.Fn f, IntRanges.Iv[] lp, List<E> args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(lp[i] != null ? fitting(args.get(i)) : arg(args.get(i), f.params[i]));
        }
        return sb.toString();
    }

    /// `a` passed for a parameter of type `t`: an int as its two halves.
    private String arg(E a, Ty t) {
        if (t.k != Ty.K.INT) {
            return conv(a, t);
        }
        String[] p = parts(a);
        return p[0] + ", " + p[1];
    }

    /// `struct.unpack(f, struct.pack(g, x))[0]` where `g` packs one double
    /// and `f` reads the same eight bytes back as a signed long, or the
    /// other way round: a reinterpretation of the bits, which is what the
    /// idiom is for. Null for anything else.
    private E structCast(Subscript s) {
        Long ix = intConstant(s.index);
        if (ix == null || ix.longValue() != 0 || !(s.value instanceof Call)) {
            return null;
        }
        Call u = (Call) s.value;
        if (!isStructCall(u, "unpack") || !(u.args.get(1) instanceof Call) || !isStructCall((Call) u.args.get(1), "pack")
                || ((Call) u.args.get(1)).args.size() != 2) {
            return null;
        }
        Call p = (Call) u.args.get(1);
        String uf = strConstant(u.args.get(0));
        String pf = strConstant(p.args.get(0));
        if (uf == null || pf == null || uf.length() != pf.length() || uf.length() > 2
                || uf.length() == 2 && uf.charAt(0) != pf.charAt(0) || uf.length() == 2 && "<>!=".indexOf(uf.charAt(0)) < 0) {
            return null;
        }
        char ut = uf.charAt(uf.length() - 1);
        char pt = pf.charAt(pf.length() - 1);
        E x = expr(p.args.get(0));
        if (pt == 'd' && ut == 'q' && (x.t.k == Ty.K.FLOAT || x.t.k == Ty.K.INT) && !x.t.opt) {
            String v = "Double.doubleToRawLongBits(" + toD(x) + ")";
            if (pre != null) {
                String t = fresh();
                indent(pre, preInd);
                pre.append("long ").append(t).append(" = ").append(v).append(";\n");
                v = t;
            }
            return new E("Long.valueOf(" + v + ")", Ty.INT, v);
        }
        if (pt == 'q' && ut == 'd' && x.lc != null) {
            return new E("Double.longBitsToDouble(" + x.lc + ")", Ty.FLOAT);
        }
        return null;
    }

    private boolean isStructCall(Call c, String fn) {
        return c.func instanceof Attribute && ((Attribute) c.func).attr.equals(fn) && c.keywords.isEmpty()
                && c.args.size() == 2 && in.isStruct(((Attribute) c.func).value, sc);
    }

    private static String strConstant(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR ? ((Constant) e).text : null;
    }

    /// Int index `i` as {its long, a condition (ending in `&&`, or empty)
    /// that it holds}; null when only the object is at hand.
    private static String[] index(E i) {
        if (i.lc != null) {
            return new String[] {i.lc, ""};
        }
        if (i.ll != null) {
            return new String[] {i.ll, i.bg + " == null && "};
        }
        return null;
    }

    /// `0 <= lc < size` of list `l` (and `l` not None).
    private static String inRange(E l, String lc) {
        String r = lc + " >= 0 && " + lc + " < " + l.c + ".size";
        return l.t.opt ? l.c + " != null && " + r : r;
    }

    private static boolean hasCall(Object o) {
        if (o instanceof Call) {
            return true;
        }
        boolean[] f = new boolean[1];
        Infer.children(o, x -> f[0] |= hasCall(x));
        return f[0];
    }

    private E result(Infer.Fn f, String call) {
        if (f.ret.k == Ty.K.INT) {
            if (pre != null && stmtPure && condDepth == 0) {
                // Into a pair before the statement (see the subscript read).
                String r = fresh();
                String rb = fresh();
                indent(pre, preInd);
                pre.append("long ").append(r).append(" = ").append(call).append(";\n");
                indent(pre, preInd);
                pre.append("Object ").append(rb).append(" = ").append(r).append(" != Long.MIN_VALUE ? null : Typed.take();\n");
                return dual(r, rb);
            }
            return new E("Typed.unret(" + call + ")", Ty.INT);
        }
        return new E(call, f.ret.k == Ty.K.BOT ? Ty.DYN : f.ret);
    }

    private E call(Call c) {
        E r = call0(c);
        if (pre != null && stmtPure && condDepth == 0 && (r.t.k == Ty.K.FLOAT || r.t.k == Ty.K.BOOL)) {
            // A pure call's result into a local, in evaluation order (see
            // the subscript read).
            String t = fresh();
            indent(pre, preInd);
            pre.append(jt(r.t)).append(' ').append(t).append(" = ").append(r.c).append(";\n");
            r = new E(t, r.t);
        }
        // The callee may store into any field.
        forgetPaths();
        return r;
    }

    private E call0(Call c) {
        if (c.args.size() == 1 && c.args.get(0) instanceof Name && codePoints.containsKey(((Name) c.args.get(0)).id)
                && ordOf(c, ((Name) c.args.get(0)).id)) {
            String l = "((long) " + codePoints.get(((Name) c.args.get(0)).id) + ")";
            return new E("Long.valueOf(" + l + ")", Ty.INT, l);
        }
        if (c.func instanceof Attribute && ((Attribute) c.func).value instanceof Name
                && scalars.containsKey(((Name) ((Attribute) c.func).value).id)) {
            String n = ((Name) ((Attribute) c.func).value).id;
            Layouts.Cls o = Layouts.resolver(scalars.get(n).k, ((Attribute) c.func).attr);
            return scalarRead(n, getterField(in.fns.get(o.typable.get(((Attribute) c.func).attr))));
        }
        if (c.func instanceof Name) {
            String n = ((Name) c.func).id;
            Scope.Binding b = sc.resolve(n);
            if (b == Scope.Binding.GLOBAL) {
                Infer.Fn f = in.globalFns.get(n);
                if (f != null) {
                    List<E> a = args(c);
                    calls.add(f);
                    IntRanges.Iv[] lp = longParams.get(f);
                    String call = lp != null && IntRanges.within(IntRanges.longArgs(f, sites.get(c)), lp)
                            ? longName(f) + "(" + longArgList(f, lp, a) + ")"
                            : typed.get(f) + "(" + argList(f, 0, a) + ")";
                    Ty site = in.siteRet.get(c);
                    if (f.ret.k == Ty.K.INT && pre != null && condDepth == 0 && (site == null || site.k == Ty.K.INT)
                            && (c == pushTop || c == pushLeaf && (stmtPure || leafImpureOk))) { //NOPMD CompareObjectsWithEquals - identity is meant
                        String r = fresh();
                        indent(pre, preInd);
                        pre.append("long ").append(r).append(" = ").append(call).append(";\n");
                        return pushed(r);
                    }
                    E r = result(f, call);
                    return site == null || site.k == r.t.k ? r : relabel(r, site);
                }
                Layouts.Cls k = in.globalClasses.get(n);
                if (k != null) {
                    return construct(k, args(c));
                }
            }
            if (b == Scope.Binding.BUILTIN && !em.moduleScopeHasStarImport() && "list".equals(n)
                    && c.args.size() == 1 && c.keywords.isEmpty() && in.siteOf.get(c) != null) {
                Ty lt = Ty.list(new int[] {in.siteOf.get(c).intValue()}, false);
                Expr a0 = c.args.get(0);
                if (in.rangeCall(a0, sc)) {
                    StringBuilder sb = new StringBuilder("Typed.rangeList(");
                    List<Expr> ra = ((Call) a0).args;
                    for (int i = 0; i < ra.size(); i++) {
                        sb.append(i == 0 ? "" : ", ").append(box(expr(ra.get(i))));
                    }
                    return new E(sb.append(')').toString(), lt);
                }
                E x = expr(a0);
                if (x.t.k == Ty.K.LIST) {
                    return new E("Typed.copy(" + x.c + ")", lt);
                }
                throw new Unsupported("list of " + x.t);
            }
            if (b == Scope.Binding.BUILTIN && !em.moduleScopeHasStarImport()) {
                if (TYPED_BUILTINS.contains(n) && c.keywords.isEmpty()) {
                    E r = builtin(n, args(c));
                    if (r != null) {
                        return r;
                    }
                }
                return genericCall(em.builtinRef(n), c);
            }
            throw new Unsupported("call of " + n);
        }
        if (in.isMathCall(c, sc)) {
            String fn = ((Attribute) c.func).attr;
            StringBuilder sb = new StringBuilder("PyMath.").append(fn).append('(');
            List<String> ds = new ArrayList<String>();
            for (int i = 0; i < c.args.size(); i++) {
                E x = expr(c.args.get(i));
                if (x.t.opt || x.t.k != Ty.K.INT && x.t.k != Ty.K.FLOAT) {
                    throw new Unsupported("math of " + x.t);
                }
                ds.add(toD(x));
                sb.append(i == 0 ? "" : ", ").append(ds.get(i));
            }
            sb.append(')');
            String in0 = inlineMath(fn, ds);
            return new E(in0 != null ? "(" + in0 + " : " + sb + ")" : sb.toString(), Ty.FLOAT);
        }
        if (c.func instanceof Attribute) {
            Attribute a = (Attribute) c.func;
            if (a.value instanceof Name && in.globalClasses.containsKey(((Name) a.value).id)
                    && sc.resolve(((Name) a.value).id) == Scope.Binding.GLOBAL) {
                Layouts.Cls k = in.globalClasses.get(((Name) a.value).id);
                Layouts.Cls o = Layouts.resolver(k, a.attr);
                FunctionDef m = o == null ? null : o.typable.get(a.attr);
                if (m == null) {
                    throw new Unsupported("class attribute call");
                }
                Infer.Fn f = in.fns.get(m);
                calls.add(f);
                return result(f, typed.get(f) + "(" + argList(f, 0, args(c)) + ")");
            }
            E r = expr(a.value);
            if (r.t.k == Ty.K.LIST && !r.t.opt && "extend".equals(a.attr) && c.keywords.isEmpty()
                    && c.args.size() == 1 && repeatCount(c.args.get(0)) != null) {
                // `l.extend([v] * n)`: filled in place, no list of n made.
                BinOp m = (BinOp) c.args.get(0);
                boolean left = m.left instanceof ListExpr;
                E first = expr(left ? ((ListExpr) m.left).elts.get(0) : m.left);
                E second = expr(left ? m.right : ((ListExpr) m.right).elts.get(0));
                return new E("Typed.extendRepeat(" + r.c + ", " + box(left ? first : second) + ", "
                        + box(left ? second : first) + ")", Ty.NONE);
            }
            List<E> args = args(c);
            if (r.t.k == Ty.K.LIST && !r.t.opt) {
                return listMethod(r, a.attr, args);
            }
            if (r.t.k != Ty.K.INST) {
                String k = "get".equals(a.attr) && args.size() == 1 && c.keywords.isEmpty() ? intKey(args.get(0)) : null;
                if (k != null) {
                    // A plain dict looks an int key up without boxing it.
                    return new E("Typed.dget(" + em.attrSite(a.attr) + ", " + box(r) + ", " + k + ")", Ty.DYN);
                }
                return siteCall(r, a.attr, args);
            }
            List<FunctionDef> cand = in.candidates(r.t.cls, a.attr);
            if (cand == null) {
                throw new Unsupported("method " + a.attr);
            }
            String recv = nz(r, a.attr);
            Layouts.Cls res = Layouts.resolver(r.t.cls, a.attr);
            if (cand.size() == 1 && res != null && res.typable.get(a.attr) == cand.get(0)) { //NOPMD CompareObjectsWithEquals - identity is meant
                Infer.Fn f = in.fns.get(cand.get(0));
                String gf = getterField(f);
                if (gf != null && args.isEmpty()) {
                    // `return self.f`: the field, read in place of the call.
                    E rr = r.t.opt ? new E(recv, r.t.nonNone()) : r;
                    return fieldRead(rr, gf);
                }
                calls.add(f);
                String al = argList(f, 1, args);
                return result(f, typed.get(f) + "(" + recv + (al.isEmpty() ? "" : ", " + al) + ")");
            }
            String key = a.attr + "/" + args.size();
            List<Infer.Fn> g = groups.get(key);
            if (g == null) {
                throw new Unsupported("no group " + key);
            }
            for (FunctionDef m : cand) {
                if (!g.contains(in.fns.get(m))) {
                    throw new Unsupported("arity mismatch " + key);
                }
            }
            callsGroups.add(key);
            Infer.Fn f = g.get(0);
            String al = argList(f, 1, args);
            return result(f, recv + ".tv_" + Emitter.mangle(a.attr) + "_" + args.size() + "(" + al + ")");
        }
        throw new Unsupported("call");
    }

    /// The guard and the `Math` call of `math.fn(ds)` when its domain can be
    /// tested in line (`guard ? Math.fn(..)`, the `PyMath` call raising the
    /// error after the colon); or null. A `PyMath` call is two calls ParparVM
    /// does not inline, in what is usually an inner loop. `x - x == 0` holds
    /// for every finite `x`, not for an infinity or NaN.
    private static String inlineMath(String fn, List<String> ds) {
        for (String d : ds) {
            if (!plain(d) && !literal(d)) {
                return null;
            }
        }
        if (ds.size() == 1) {
            String x = ds.get(0);
            if ("sqrt".equals(fn)) {
                return x + " >= 0.0 ? Math.sqrt(" + x + ")";
            }
            if ("sin".equals(fn) || "cos".equals(fn)) {
                return x + " - " + x + " == 0.0 ? Math." + fn + "(" + x + ")";
            }
        } else if (ds.size() == 2 && "fmod".equals(fn)) {
            String x = ds.get(0);
            String y = ds.get(1);
            // Finite `x` and a nonzero `y`: C's fmod, which `%` is.
            return x + " - " + x + " == 0.0 && " + y + " != 0.0 ? " + x + " % " + y;
        }
        return null;
    }

    /// `x` as a Java `long` when it is an int at hand as one, cheap and free
    /// of effects (see `E.lc`); or null.
    private static String intKey(E x) {
        return x.t.k == Ty.K.INT && !x.t.opt && x.pk == null && x.lc != null && (plain(x.lc) || literal(x.lc)) ? x.lc : null;
    }

    /// `r.attr(args)` as the generic code calls a method: through an inline
    /// cache, with an untyped result.
    private E siteCall(E r, String attr, List<E> args) {
        String site = em.attrSite(attr);
        StringBuilder a = new StringBuilder(box(r));
        for (E x : args) {
            a.append(", ").append(box(x));
        }
        if (args.size() <= 3) {
            return new E(site + ".call" + args.size() + "(" + a + ")", Ty.DYN);
        }
        StringBuilder arr = new StringBuilder();
        for (E x : args) {
            arr.append(arr.length() == 0 ? "" : ", ").append(box(x));
        }
        return new E(site + ".callN(" + box(r) + ", new Object[] {" + arr + "})", Ty.DYN);
    }

    /// The field `f` returns when its body is only `return self.<field>`
    /// (a getter), or null.
    private String getterField(Infer.Fn f) {
        List<Stmt> body = f.def.body;
        if (body.size() != 1 || !(body.get(0) instanceof Return) || f.names.size() != 1) {
            return null;
        }
        Expr v = ((Return) body.get(0)).value;
        if (!(v instanceof Attribute) || !(((Attribute) v).value instanceof Name)
                || !((Name) ((Attribute) v).value).id.equals(f.names.get(0))) {
            return null;
        }
        String attr = ((Attribute) v).attr;
        Layouts.Cls o = Infer.fieldOwner(f.cls, attr);
        return o != null && in.definitelySet(o, attr) ? attr : null;
    }

    /// Field `attr` of `r`.
    private E fieldRead(E r, String attr) {
        Ty ft = fieldTy(r, attr);
        if (ft.k == Ty.K.INT && typedField(owner(r, attr), attr) != null) {
            return intField(r, attr);
        }
        return new E(fieldRef(r, attr), ft);
    }

    /// A call's result known, at this call site, to be `t` (`Infer.siteRet`)
    /// though the function returns something wider.
    private E relabel(E r, Ty t) {
        switch (t.k) {
            case INT:
                return new E(r.t.k == Ty.K.DYN ? r.c : box(r), Ty.INT);
            case FLOAT:
                return new E("Typed.asFloat(" + box(r) + ")", Ty.FLOAT);
            default:
                return new E("Typed.asBool(" + box(r) + ")", Ty.BOOL);
        }
    }

    // ---- int arithmetic in a Java long -----------------------------------

    /// `e` as a `long`, when `IntRanges` proves its value fits in one; or
    /// null. The result is then computed modulo 2^64 (`mod64`), which is
    /// the exact value however large the intermediate values are.
    private E wrapped(Expr e) {
        IntRanges.Iv r = ranges.eval(e, null);
        if (!r.fitsLong()) {
            return null;
        }
        int mark = pre == null ? 0 : pre.length();
        String c = mod64(e);
        if (c == null) {
            // Nothing evaluated on the way may run: the caller lowers `e`
            // another way, from the start.
            if (pre != null) {
                pre.setLength(mark);
            }
            return null;
        }
        if (pre != null && !plain(c)) {
            String t = fresh();
            indent(pre, preInd);
            pre.append("long ").append(t).append(" = ").append(c).append(";\n");
            c = t;
        }
        return new E("Long.valueOf(" + c + ")", Ty.INT, c);
    }

    /// Whether `e`, an int expression, provably fits in a `long`.
    private boolean fits(Expr e) {
        return ranges.eval(e, null).fitsLong();
    }

    /// Java `long` code for int expression `e` modulo 2^64, or null. Only
    /// `+ - * & | ^ ~ <<` commute with that reduction; anything else
    /// (`>>`, `//`, `%`) needs the exact value of its operands.
    private String mod64(Expr e) {
        E sub = leafSubst.get(e);
        if (sub != null) {
            return sub.t.k == Ty.K.INT || sub.t.k == Ty.K.BOOL ? fits(e) ? fitting(sub) : low64(sub) : null;
        }
        if (e instanceof Constant) {
            if (((Constant) e).kind != Constant.Kind.INT) {
                return null;
            }
            java.math.BigInteger v = IntRanges.parse(((Constant) e).text);
            return v == null ? null : lit(v);
        }
        if (e instanceof Name) {
            return nameLong((Name) e);
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            String o = mod64(u.operand);
            if (o == null) {
                return null;
            }
            switch (u.op) {
                case "+":
                    return o;
                case "-":
                    return "(-" + o + ")";
                case "~":
                    return "(~" + o + ")";
                default:
                    return null;
            }
        }
        if (e instanceof BinOp) {
            return binLong((BinOp) e);
        }
        if (e instanceof Call) {
            Infer.Fn f = ranges.inlinable((Call) e);
            if (f != null) {
                return inline(f, (Call) e);
            }
        }
        if (e instanceof IfExp && inlined.isEmpty()) {
            IfExp x = (IfExp) e;
            String a = mod64(x.body);
            String b = mod64(x.orelse);
            return a == null || b == null ? null : "(" + testExpr(x.test) + " ? " + a + " : " + b + ")";
        }
        return generic(e);
    }

    /// An int expression the long arithmetic cannot follow (a subscript, a
    /// call): computed as before, and its exact value when it is known to
    /// fit, else its value modulo 2^64.
    private String generic(Expr e) {
        if (!inlined.isEmpty()) {
            return null;
        }
        Expr saved = plainExpr;
        plainExpr = e;
        try {
            E v = expr(e);
            if (v.t.k != Ty.K.INT) {
                return null;
            }
            return fits(e) ? fitting(v) : low64(v);
        } finally {
            plainExpr = saved;
        }
    }

    /// Int `v`, known to fit, as a Java long.
    private String fitting(E v) {
        if (v.lc != null) {
            return v.lc;
        }
        if (v.ll != null) {
            // A pair's object is then a bool.
            return "(" + v.bg + " == null ? " + v.ll + " : Typed.toLong(" + v.bg + "))";
        }
        return "Typed.toLong(" + box(v) + ")";
    }

    /// Int `v` modulo 2^64.
    private String low64(E v) {
        if (v.lc != null) {
            return v.lc;
        }
        if (v.ll != null) {
            return "(" + v.bg + " == null ? " + v.ll + " : Typed.low64(" + v.bg + "))";
        }
        return "Typed.low64(" + box(v) + ")";
    }

    private static String lit(java.math.BigInteger v) {
        long l = v.longValue();
        return l < 0 ? "(" + l + "L)" : l + "L";
    }

    private String nameLong(Name nm) {
        String n = nm.id;
        if (!inlined.isEmpty()) {
            String[] p = inlined.get(inlined.size() - 1).get(n);
            if (p != null) {
                return p[0];
            }
            java.math.BigInteger c = in.intConsts.get(n);
            return c == null ? null : lit(c);
        }
        switch (sc.resolve(n)) {
            case LOCAL:
                if (longLocals.contains(n)) {
                    if (!sc.params.contains(n) && !da.safe.contains(nm)) {
                        return null;
                    }
                    return var(n);
                }
                return generic(nm);
            case GLOBAL: {
                java.math.BigInteger c = in.intConsts.get(n);
                return c != null ? lit(c) : generic(nm);
            }
            default:
                return null;
        }
    }

    /// Whether `e` is known exactly, not only modulo 2^64, by `mod64`.
    private boolean exact(Expr e) {
        if (!inlined.isEmpty() && e instanceof Name) {
            String[] p = inlined.get(inlined.size() - 1).get(((Name) e).id);
            if (p != null) {
                return "exact".equals(p[1]);
            }
        }
        return fits(e);
    }

    private String binLong(BinOp b) {
        switch (b.op) {
            case "+":
            case "-":
            case "*":
            case "&":
            case "|":
            case "^": {
                String l = mod64(b.left);
                String r = l == null ? null : mod64(b.right);
                return r == null ? null : "(" + l + " " + b.op + " " + r + ")";
            }
            case "<<": {
                java.math.BigInteger c = IntRanges.literal(b.right);
                if (c == null || c.signum() < 0 || c.bitLength() > 7) {
                    return null;
                }
                String l = mod64(b.left);
                if (l == null) {
                    return null;
                }
                return c.intValue() >= 64 ? "0L" : "(" + l + " << " + c.intValue() + ")";
            }
            case ">>": {
                java.math.BigInteger c = IntRanges.literal(b.right);
                if (c == null || c.signum() < 0 || c.bitLength() > 7) {
                    return null;
                }
                int k = c.intValue();
                if (exact(b.left)) {
                    String l = mod64(b.left);
                    return l == null ? null : "(" + l + " >> " + Math.min(k, 63) + ")";
                }
                if (k >= 1 && ranges.eval(b.left, null).fitsUnsigned64()) {
                    // In [0, 2^64): its 64 bits read unsigned are the value.
                    String l = mod64(b.left);
                    return l == null ? null : k >= 64 ? "0L" : "(" + l + " >>> " + k + ")";
                }
                return null;
            }
            case "%":
            case "//": {
                IntRanges.Iv d = ranges.eval(b.right, null);
                boolean nonZero = d.lo != null && d.lo.signum() > 0 || d.hi != null && d.hi.signum() < 0;
                if (!nonZero || !exact(b.left) || !exact(b.right)) {
                    return null;
                }
                String l = mod64(b.left);
                String r = l == null ? null : mod64(b.right);
                if (r == null) {
                    return null;
                }
                return ("%".equals(b.op) ? "Math.floorMod(" : "Math.floorDiv(") + l + ", " + r + ")";
            }
            default:
                return null;
        }
    }

    /// The body of expression function `f` in place of the call `c`.
    private String inline(Infer.Fn f, Call c) {
        Map<String, String[]> p = new HashMap<String, String[]>();
        Map<String, IntRanges.Iv> pr = new HashMap<String, IntRanges.Iv>();
        for (int i = 0; i < c.args.size(); i++) {
            Expr a = c.args.get(i);
            String code = mod64(a);
            if (code == null) {
                return null;
            }
            if (pre != null && !plain(code)) {
                String t = fresh();
                indent(pre, preInd);
                pre.append("long ").append(t).append(" = ").append(code).append(";\n");
                code = t;
            }
            p.put(f.names.get(i), new String[] {code, exact(a) ? "exact" : "mod"});
            pr.put(f.names.get(i), ranges.eval(a, null));
        }
        inlined.add(p);
        ranges.push(pr);
        try {
            return mod64(Infer.exprBody(f.def));
        } finally {
            ranges.pop();
            inlined.remove(inlined.size() - 1);
        }
    }

    private static String mathConstant(String n) {
        switch (n) {
            case "pi":
                return "Math.PI";
            case "e":
                return "Math.E";
            case "tau":
                return "(2 * Math.PI)";
            case "inf":
                return "Double.POSITIVE_INFINITY";
            default:
                return "Double.NaN";
        }
    }

    // ---- scalar replacement ----------------------------------------------

    /// A local whose objects never escape the function: every value it
    /// holds comes from a constructor that only copies its arguments into
    /// fields, and every use reads a field, directly or through a getter.
    /// The object is then never made; each field is a local of its own.
    private static final class Scalar {
        Layouts.Cls k;
        /// The field each constructor argument goes to, by position.
        String[] attrs;
        final Set<String> reads = new HashSet<String>();
        final Set<String> getters = new HashSet<String>();
    }

    private Map<String, Scalar> scalars = new HashMap<String, Scalar>();

    /// The local holding field `attr` of scalar local `n`; the dot keeps it
    /// apart from every Python name.
    private static String scalarField(String n, String attr) {
        return n + "." + attr;
    }

    private Map<String, Scalar> findScalars(Infer.Fn f) {
        Map<String, Scalar> cand = new HashMap<String, Scalar>();
        for (Map.Entry<String, Ty> e : f.locals.entrySet()) {
            String n = e.getKey();
            if (e.getValue().k == Ty.K.INST && !sc.params.contains(n) && !sc.deleted.contains(n)
                    && sc.resolve(n) == Scope.Binding.LOCAL) {
                cand.put(n, new Scalar());
            }
        }
        if (cand.isEmpty()) {
            return cand;
        }
        Set<String> bad = new HashSet<String>();
        scanScalars(f.def.body, cand, bad);
        Map<String, Scalar> out = new HashMap<String, Scalar>();
        for (Map.Entry<String, Scalar> e : cand.entrySet()) {
            Scalar x = e.getValue();
            if (bad.contains(e.getKey()) || x.k == null || !scalarUses(x)) {
                continue;
            }
            out.put(e.getKey(), x);
        }
        return out;
    }

    /// Whether every use of `x` reads a field its constructor set.
    private boolean scalarUses(Scalar x) {
        Set<String> fields = new HashSet<String>(Arrays.asList(x.attrs));
        for (String g : x.getters) {
            Layouts.Cls o = Layouts.resolver(x.k, g);
            FunctionDef d = o == null ? null : o.typable.get(g);
            Infer.Fn gf = d == null ? null : in.fns.get(d);
            String attr = gf == null ? null : getterField(gf);
            if (attr == null) {
                return false;
            }
            x.reads.add(attr);
        }
        for (String r : x.reads) {
            if (!fields.contains(r) || Layouts.resolver(x.k, r) != null) {
                return false;
            }
            Ty t = scalarTy(x, r);
            if (t == null || t.k == Ty.K.BOT) {
                return false;
            }
        }
        return true;
    }

    private Ty scalarTy(Scalar x, String attr) {
        Layouts.Cls o = Infer.fieldOwner(x.k, attr);
        if (o == null) {
            return null;
        }
        Ty t = typedField(o, attr);
        return t != null ? t : in.fieldType(o, attr);
    }

    private void scanScalars(Object o, Map<String, Scalar> cand, Set<String> bad) {
        if (o instanceof Assign) {
            Assign a = (Assign) o;
            for (Expr t : a.targets) {
                storeTargets(t, cand, bad);
            }
            if (a.targets.size() == 1 && a.targets.get(0) instanceof Name
                    && cand.containsKey(((Name) a.targets.get(0)).id)) {
                String n = ((Name) a.targets.get(0)).id;
                if (!(a.value instanceof Call) || !scalarCtor(cand.get(n), (Call) a.value)) {
                    bad.add(n);
                    scanScalars(a.value, cand, bad);
                } else {
                    for (Expr x : ((Call) a.value).args) {
                        scanScalars(x, cand, bad);
                    }
                }
                return;
            }
        } else if (o instanceof AugAssign) {
            storeTargets(((AugAssign) o).target, cand, bad);
        } else if (o instanceof AnnAssign) {
            storeTargets(((AnnAssign) o).target, cand, bad);
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                storeTargets(t, cand, bad);
            }
        } else if (o instanceof For) {
            storeTargets(((For) o).target, cand, bad);
        } else if (o instanceof Comprehension) {
            storeTargets(((Comprehension) o).target, cand, bad);
        } else if (o instanceof WithItem && ((WithItem) o).vars != null) {
            storeTargets(((WithItem) o).vars, cand, bad);
        } else if (o instanceof Call && ((Call) o).func instanceof Attribute) {
            Attribute f = (Attribute) ((Call) o).func;
            if (f.value instanceof Name && cand.containsKey(((Name) f.value).id)) {
                Call c = (Call) o;
                String n = ((Name) f.value).id;
                if (c.args.isEmpty() && c.keywords.isEmpty() && da.safe.contains((Name) f.value)) {
                    cand.get(n).getters.add(f.attr);
                } else {
                    bad.add(n);
                }
                for (Expr x : c.args) {
                    scanScalars(x, cand, bad);
                }
                for (Keyword k : c.keywords) {
                    scanScalars(k.value, cand, bad);
                }
                return;
            }
        } else if (o instanceof Attribute) {
            Attribute a = (Attribute) o;
            if (a.value instanceof Name && cand.containsKey(((Name) a.value).id)) {
                if (da.safe.contains((Name) a.value)) {
                    cand.get(((Name) a.value).id).reads.add(a.attr);
                } else {
                    bad.add(((Name) a.value).id);
                }
                return;
            }
        } else if (o instanceof Name) {
            bad.add(((Name) o).id);
            return;
        }
        Infer.children(o, x -> scanScalars(x, cand, bad));
    }

    /// Marks every candidate whose field a store target writes.
    private static void storeTargets(Expr t, Map<String, Scalar> cand, Set<String> bad) {
        if (t instanceof Attribute && ((Attribute) t).value instanceof Name) {
            bad.add(((Name) ((Attribute) t).value).id);
        } else if (t instanceof TupleExpr) {
            for (Expr x : ((TupleExpr) t).elts) {
                storeTargets(x, cand, bad);
            }
        } else if (t instanceof ListExpr) {
            for (Expr x : ((ListExpr) t).elts) {
                storeTargets(x, cand, bad);
            }
        } else if (t instanceof Starred) {
            storeTargets(((Starred) t).value, cand, bad);
        }
    }

    /// Whether `c` constructs, with positional arguments only, a class whose
    /// `__init__` only copies each argument into a field of its own; fills
    /// `x` on the first one, and requires the same class every time.
    private boolean scalarCtor(Scalar x, Call c) {
        if (!(c.func instanceof Name) || !c.keywords.isEmpty()
                || sc.resolve(((Name) c.func).id) != Scope.Binding.GLOBAL) {
            return false;
        }
        Layouts.Cls k = in.globalClasses.get(((Name) c.func).id);
        if (k == null || x.k != null && x.k != k) { //NOPMD CompareObjectsWithEquals - identity is meant
            return false;
        }
        for (String h : new String[] {"__new__", "__setattr__", "__getattribute__", "__getattr__", "__del__"}) {
            if (Layouts.resolver(k, h) != null) {
                return false;
            }
        }
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        FunctionDef init = o == null ? null : o.typable.get("__init__");
        Infer.Fn f = init == null ? null : in.fns.get(init);
        if (f == null || f.names.size() != c.args.size() + 1 || !init.decorators.isEmpty()) {
            return false;
        }
        for (Expr a : c.args) {
            if (a instanceof Starred) {
                return false;
            }
        }
        String[] attrs = new String[c.args.size()];
        String self = f.names.get(0);
        List<Expr> ts = new ArrayList<Expr>();
        List<Expr> vs = new ArrayList<Expr>();
        for (Stmt s : init.body) {
            if (!(s instanceof Assign) || ((Assign) s).targets.size() != 1) {
                return false;
            }
            Expr t = ((Assign) s).targets.get(0);
            Expr v = ((Assign) s).value;
            if (t instanceof TupleExpr && v instanceof TupleExpr
                    && ((TupleExpr) t).elts.size() == ((TupleExpr) v).elts.size()) {
                ts.addAll(((TupleExpr) t).elts);
                vs.addAll(((TupleExpr) v).elts);
            } else {
                ts.add(t);
                vs.add(v);
            }
        }
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < ts.size(); i++) {
            Expr t = ts.get(i);
            Expr v = vs.get(i);
            if (!(t instanceof Attribute) || !(((Attribute) t).value instanceof Name)
                    || !((Name) ((Attribute) t).value).id.equals(self) || !(v instanceof Name)) {
                return false;
            }
            int p = f.names.indexOf(((Name) v).id);
            String attr = ((Attribute) t).attr;
            if (p < 1 || attrs[p - 1] != null || !seen.add(attr)) {
                return false;
            }
            attrs[p - 1] = attr;
        }
        for (String a : attrs) {
            if (a == null) {
                return false;
            }
        }
        if (x.attrs != null && !Arrays.equals(x.attrs, attrs)) {
            return false;
        }
        x.k = k;
        x.attrs = attrs;
        return true;
    }

    /// Field `attr` of scalar local `n`.
    private E scalarRead(String n, String attr) {
        String l = scalarField(n, attr);
        Ty t = scalarTy(scalars.get(n), attr);
        if (t.k == Ty.K.INT) {
            return dual(var(l), big(l));
        }
        return new E(var(l), t);
    }

    /// `n = C(args)` for scalar local `n`: the arguments, in order, into
    /// the field locals.
    private void scalarStore(String n, Call c, StringBuilder b, int ind) {
        Scalar x = scalars.get(n);
        List<E> av = new ArrayList<E>();
        for (Expr a : c.args) {
            av.add(spillTo(expr(a), b, ind));
        }
        for (int i = 0; i < av.size(); i++) {
            String l = scalarField(n, x.attrs[i]);
            if (!x.reads.contains(x.attrs[i])) {
                continue;
            }
            Ty t = scalarTy(x, x.attrs[i]);
            indent(b, ind);
            if (t.k == Ty.K.INT) {
                storePair(var(l), big(l), av.get(i), b, ind);
            } else {
                b.append(var(l)).append(" = ").append(conv(av.get(i), t)).append(";\n");
            }
        }
    }

    /// Declarations of the field locals of every scalar local.
    private void scalarDecls(StringBuilder b) {
        for (Map.Entry<String, Scalar> e : scalars.entrySet()) {
            for (String attr : e.getValue().reads) {
                String l = scalarField(e.getKey(), attr);
                Ty t = scalarTy(e.getValue(), attr);
                if (t.k == Ty.K.INT) {
                    b.append("        long ").append(var(l)).append(" = 0L;\n        Object ").append(big(l))
                            .append(" = null;\n");
                } else {
                    b.append("        ").append(jt(t)).append(' ').append(var(l)).append(" = ").append(zero(t))
                            .append(";\n");
                }
            }
        }
    }

    private E construct(Layouts.Cls k, List<E> args) {
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        if (o == null) {
            if (!args.isEmpty()) {
                throw new Unsupported("arguments to object()");
            }
            return new E("new " + k.javaName + "(" + k.typeVar + ")", Ty.inst(k, false));
        }
        FunctionDef init = o.typable.get("__init__");
        if (init == null) {
            throw new Unsupported("__init__");
        }
        Infer.Fn f = in.fns.get(init);
        calls.add(f);
        String al = argList(f, 1, args);
        if (plainCtor(k) != null) {
            return new E("new " + k.javaName + "(" + k.typeVar + ", " + al + ")", Ty.inst(k, false));
        }
        constructors.add(k);
        return new E("n_" + k.javaName + "(" + al + ")", Ty.inst(k, false));
    }

    private E builtin(String n, List<E> a) {
        switch (n) {
            case "len":
                if (a.size() == 1 && a.get(0).t.k == Ty.K.LIST) {
                    return new E("h_len(" + a.get(0).c + ")", Ty.INT);
                }
                if (a.size() == 1 && a.get(0).t.k == Ty.K.STR && !a.get(0).t.opt) {
                    String l = "Typed.slen(" + a.get(0).c + ")";
                    return new E("Long.valueOf(" + l + ")", Ty.INT, l);
                }
                if (a.size() == 1 && a.get(0).t.k == Ty.K.DYN) {
                    // The builtin's work without a call, unless the name
                    // was rebound at run time.
                    String l = "Typed.len(" + em.builtinRef("len") + ", " + a.get(0).c + ")";
                    return new E("Long.valueOf(" + l + ")", Ty.INT, l);
                }
                break;
            case "float":
                if (a.size() == 1 && !a.get(0).t.opt
                        && (a.get(0).t.k == Ty.K.INT || a.get(0).t.k == Ty.K.FLOAT)) {
                    return new E(toD(a.get(0)), Ty.FLOAT);
                }
                break;
            case "str":
                if (a.size() == 1 && !a.get(0).t.opt) {
                    E x = a.get(0);
                    switch (x.t.k) {
                        case STR:
                            return x;
                        case BOOL:
                            return new E("((" + x.c + ") ? \"True\" : \"False\")", Ty.STR);
                        case INT:
                            if (x.lc != null) {
                                return new E("Long.toString(" + x.lc + ")", Ty.STR);
                            }
                            if (x.ll != null) {
                                return new E("(" + x.bg + " == null ? Long.toString(" + x.ll + ") : Py.toStr(" + x.bg
                                        + "))", Ty.STR);
                            }
                            return new E("Py.toStr(" + box(x) + ")", Ty.STR);
                        case FLOAT:
                            return new E("Py.toStr(" + box(x) + ")", Ty.STR);
                        default:
                            break;
                    }
                }
                break;
            case "ord":
                if (a.size() == 1 && a.get(0).t.k == Ty.K.STR && !a.get(0).t.opt) {
                    return new E("Typed.ord(" + a.get(0).c + ")", Ty.INT);
                }
                break;
            case "abs":
                if (a.size() == 1 && a.get(0).t.k == Ty.K.FLOAT) {
                    return new E("Math.abs(" + a.get(0).c + ")", Ty.FLOAT);
                }
                if (a.size() == 1 && a.get(0).t.k == Ty.K.INT) {
                    return new E("Typed.abs(" + a.get(0).c + ")", Ty.INT);
                }
                break;
            default:
                break;
        }
        return null;
    }

    /// The builtins `builtin` lowers; any other is called as Python calls it.
    private static final Set<String> TYPED_BUILTINS = new HashSet<String>(Arrays.asList("len", "float", "str", "ord",
            "abs"));

    /// A call of `f` with the arguments of `c` boxed, keywords included:
    /// what the generic code does, with an untyped result. Reached for
    /// builtins that have no typed lowering (`print`, `sorted`, ...), so one
    /// such call no longer leaves the whole function untyped.
    private E genericCall(String f, Call c) {
        StringBuilder a = new StringBuilder();
        for (Expr x : c.args) {
            if (x instanceof Starred) {
                throw new Unsupported("starred argument");
            }
            a.append(a.length() == 0 ? "" : ", ").append(box(expr(x)));
        }
        if (c.keywords.isEmpty()) {
            return new E("Py.call(" + f + (a.length() == 0 ? "" : ", " + a) + ")", Ty.DYN);
        }
        StringBuilder names = new StringBuilder();
        for (Keyword kw : c.keywords) {
            if (kw.arg == null) {
                throw new Unsupported("** argument");
            }
            a.append(a.length() == 0 ? "" : ", ").append(box(expr(kw.value)));
            names.append(names.length() == 0 ? "" : ", ").append(Emitter.javaString(kw.arg));
        }
        return new E("Py.callKw(" + f + ", new Object[] {" + a + "}, new String[] {" + names + "})", Ty.DYN);
    }

    /// The count of `[v] * n` or `n * [v]` when it is an int, or null.
    private Expr repeatCount(Expr e) {
        if (!(e instanceof BinOp) || !"*".equals(((BinOp) e).op)) {
            return null;
        }
        BinOp b = (BinOp) e;
        Expr l = b.left instanceof ListExpr ? b.left : b.right instanceof ListExpr ? b.right : null;
        if (l == null || ((ListExpr) l).elts.size() != 1 || ((ListExpr) l).elts.get(0) instanceof Starred) {
            return null;
        }
        Expr n = l == b.left ? b.right : b.left; //NOPMD CompareObjectsWithEquals - identity is meant
        Ty t = in.type(n, cur, sc);
        return t.k == Ty.K.INT && !t.opt ? n : null;
    }

    private E listMethod(E l, String m, List<E> a) {
        switch (m) {
            case "append":
                if (a.size() == 1) {
                    return new E("Typed.append(" + l.c + ", " + box(a.get(0)) + ")", Ty.NONE);
                }
                break;
            case "extend":
                if (a.size() == 1 && a.get(0).t.k == Ty.K.LIST && !a.get(0).t.opt) {
                    return new E("Typed.extend(" + l.c + ", " + a.get(0).c + ")", Ty.NONE);
                }
                break;
            case "pop":
                if (a.size() <= 1) {
                    Ty el = in.elem(l.t);
                    String c = l.c + ".pop(" + (a.isEmpty() ? "-1L" : "Py.asLong(" + box(a.get(0)) + ")") + ")";
                    return new E(unbox(c, el), el.k == Ty.K.BOT ? Ty.DYN : el);
                }
                break;
            default:
                break;
        }
        throw new Unsupported("list." + m);
    }

    // ---- output ----------------------------------------------------------

    /// The typed fn whose generic body is `fs`, or null.
    Infer.Fn fnFor(Scope fs) {
        Infer.Fn f = byScope.get(fs);
        return f != null && typed.containsKey(f) ? f : null;
    }

    /// The statement the generic body of `f` starts with: enter the typed
    /// version when the arguments, held in `locals`, fit it.
    String entry(Infer.Fn f, List<String> locals) {
        StringBuilder test = new StringBuilder();
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < f.params.length; i++) {
            Ty t = f.params[i];
            String v = locals.get(i);
            String check = null;
            String arg;
            switch (t.k) {
                case BOOL:
                    check = "(" + v + " == Py.TRUE || " + v + " == Py.FALSE)";
                    arg = v + " == Py.TRUE";
                    break;
                case FLOAT:
                    check = v + " instanceof Double";
                    arg = "((Double) " + v + ").doubleValue()";
                    break;
                case STR:
                    check = t.opt ? "(" + v + " instanceof String || " + v + " == Py.NONE)" : v + " instanceof String";
                    arg = "Typed.asStr(" + v + ")";
                    break;
                case LIST:
                    check = t.opt ? "(" + v + " instanceof PyList || " + v + " == Py.NONE)" : v + " instanceof PyList";
                    arg = "Typed.asList(" + v + ")";
                    break;
                case INST:
                    check = t.opt ? "(" + v + " instanceof " + t.cls.javaName + " || " + v + " == Py.NONE)"
                            : v + " instanceof " + t.cls.javaName;
                    arg = unbox(v, t);
                    break;
                case INT:
                    // A guessed parameter (Infer.guess) is checked like any other.
                    check = "(" + v + " instanceof Long || " + v + " instanceof PyInt)";
                    arg = "0L, " + v;
                    break;
                case NONE:
                    check = v + " == Py.NONE";
                    arg = v;
                    break;
                default:
                    arg = v;
                    break;
            }
            if (check != null) {
                test.append(test.length() == 0 ? "" : " && ").append(check);
            }
            args.append(i == 0 ? "" : ", ").append(arg);
        }
        String call = f.ret.k == Ty.K.INT ? "Typed.unret(" + typed.get(f) + "(" + args + "))"
                : box(new E(typed.get(f) + "(" + args + ")", f.ret));
        if (test.length() == 0) {
            // Always taken; javac must still see the generic body as reachable.
            test.append("Typed.yes");
        }
        return "if (" + test + ") {\n    return " + call + ";\n}\n";
    }

    /// The Java declaration of a typed field, or null for an object one.
    private final Map<Layouts.Cls, String> plainCtors = new HashMap<Layouts.Cls, String>();

    /// A Java constructor of `k`'s class doing its `__init__` in place,
    /// when that only stores parameters in fields (`self.x = x`); or null.
    /// Typed code then constructs with one `new`: ParparVM inlines no call,
    /// and the allocation, the generic constructor and a separate
    /// `__init__` were three of them on the path every object takes.
    String plainCtor(Layouts.Cls k) {
        if (!plainCtors.containsKey(k)) {
            plainCtors.put(k, plainCtor0(k));
        }
        return plainCtors.get(k);
    }

    private String plainCtor0(Layouts.Cls k) {
        for (String h : new String[] {"__new__", "__setattr__", "__getattribute__", "__getattr__", "__del__"}) {
            if (Layouts.resolver(k, h) != null) {
                return null;
            }
        }
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        FunctionDef init = o == null ? null : o.typable.get("__init__");
        Infer.Fn f = init == null ? null : in.fns.get(init);
        if (f == null || !typed.containsKey(f) || f.names.size() < 2 || !init.decorators.isEmpty()
                || init.args.vararg != null || init.args.kwarg != null || !init.args.kwonly.isEmpty()) {
            return null;
        }
        String self = f.names.get(0);
        StringBuilder sig = new StringBuilder();
        StringBuilder norm = new StringBuilder();
        for (int i = 1; i < f.names.size(); i++) {
            sig.append(", ").append(param(f.params[i], "a" + i, "b" + i));
        }
        StringBuilder body = new StringBuilder();
        Set<String> seen = new HashSet<String>();
        Set<Integer> normed = new HashSet<Integer>();
        for (Stmt st : init.body) {
            if (!(st instanceof Assign) || ((Assign) st).targets.size() != 1
                    || !(((Assign) st).targets.get(0) instanceof Attribute) || !(((Assign) st).value instanceof Name)) {
                return null;
            }
            Attribute t = (Attribute) ((Assign) st).targets.get(0);
            int p = f.names.indexOf(((Name) ((Assign) st).value).id);
            if (!(t.value instanceof Name) || !((Name) t.value).id.equals(self) || p < 1 || !seen.add(t.attr)) {
                return null;
            }
            Layouts.Cls ow = Infer.fieldOwner(k, t.attr);
            if (ow == null) {
                return null;
            }
            Ty ft = typedField(ow, t.attr);
            Ty pt = f.params[p];
            String m = Emitter.mangle(t.attr);
            if (ft != null && ft.k == Ty.K.INT) {
                if (pt.k != Ty.K.INT) {
                    return null;
                }
                if (normed.add(p)) {
                    // As the typed __init__ does: a caller holding only the
                    // object passes it whole.
                    norm.append("            if (b").append(p).append(" instanceof Long) {\n                a").append(p)
                            .append(" = ((Long) b").append(p).append(").longValue();\n                b").append(p)
                            .append(" = null;\n            }\n");
                }
                // The object half is almost always null, the field's
                // default: not stored, a barrier saved (see PyInstance).
                body.append("            t_").append(m).append(" = a").append(p).append(";\n            if (b").append(p)
                        .append(" != null) {\n                b_").append(m).append(" = b").append(p)
                        .append(";\n            }\n");
            } else if (ft != null) {
                if (pt.k == Ty.K.INT || !jt(ft).equals(jt(pt))) {
                    return null;
                }
                body.append("            t_").append(m).append(" = a").append(p).append(";\n");
            } else {
                if (!in.definitelySet(ow, t.attr)) {
                    return null;
                }
                String v = pt.k == Ty.K.INT ? dual("a" + p, "b" + p).c : box(new E("a" + p, pt));
                body.append("            f_").append(m).append(" = ").append(v).append(";\n");
            }
        }
        return "        " + k.javaName + "(PyType t" + sig + ") {\n            super(t);\n" + norm + body + "        }\n";
    }

    String fieldDecl(Layouts.Cls k, String f) {
        Ty t = typedField(k, f);
        if (t != null && t.k == Ty.K.INT) {
            return "long t_" + Emitter.mangle(f) + ";\n        Object b_" + Emitter.mangle(f) + ";";
        }
        return t == null ? null : jt(t) + " t_" + Emitter.mangle(f) + ";";
    }

    String fieldGetCase(Layouts.Cls k, String f) {
        Ty t = typedField(k, f);
        if (t.k == Ty.K.INT) {
            return dual("t_" + Emitter.mangle(f), "b_" + Emitter.mangle(f)).c;
        }
        return box(new E("t_" + Emitter.mangle(f), t));
    }

    String fieldSetCase(Layouts.Cls k, String f, String v) {
        Ty t = typedField(k, f);
        if (t.k == Ty.K.INT) {
            String m = Emitter.mangle(f);
            return "if (" + v + " instanceof Long) {\n                        t_" + m + " = ((Long) " + v
                    + ").longValue();\n                        b_" + m + " = null;\n                    } else {\n"
                    + "                        b_" + m + " = " + v + ";\n                    }";
        }
        return "t_" + Emitter.mangle(f) + " = " + unbox(v, t) + ";";
    }

    /// Members of layout class `k` (`tv_` overrides) and of the base `LB`
    /// (`k` null: the `tv_` declarations).
    String layoutMembers(Layouts.Cls k) {
        StringBuilder o = new StringBuilder();
        for (String key : usedGroups) {
            List<Infer.Fn> g = groups.get(key);
            Infer.Fn any = g.get(0);
            String name = any.def.name;
            int ar = any.params.length - 1;
            StringBuilder sig = new StringBuilder();
            StringBuilder pass = new StringBuilder();
            for (int i = 1; i <= ar; i++) {
                sig.append(i == 1 ? "" : ", ").append(param(any.params[i], "a" + i, "b" + i));
                pass.append(", a").append(i).append(any.params[i].k == Ty.K.INT ? ", b" + i : "");
            }
            String head = "        " + retJ(any) + " tv_" + Emitter.mangle(name) + "_" + ar + "(" + sig + ") {\n";
            if (k == null) {
                o.append(head).append("            throw Py.attributeError(\"'\" + cls.name + \"' object has no attribute ")
                        .append(Emitter.javaString(name).replace("\"", "'")).append("\");\n        }\n");
                continue;
            }
            FunctionDef m = k.typable.get(name);
            if (m == null || !k.bodyNames.contains(name)) {
                continue;
            }
            Infer.Fn f = in.fns.get(m);
            if (!g.contains(f)) {
                continue;
            }
            o.append("        @Override\n").append(head).append("            return ").append(typed.get(f))
                    .append("(this").append(pass).append(");\n        }\n");
        }
        return o.toString();
    }

    /// The typed methods and their helpers, as static members of the
    /// module class.
    String members() {
        StringBuilder o = new StringBuilder();
        for (String b : bodies.values()) {
            o.append(b);
        }
        for (String b : longBodies.values()) {
            o.append(b);
        }
        for (Layouts.Cls k : constructors) {
            Layouts.Cls ow = Layouts.resolver(k, "__init__");
            Infer.Fn f = in.fns.get(ow.typable.get("__init__"));
            StringBuilder sig = new StringBuilder();
            StringBuilder pass = new StringBuilder();
            for (int i = 1; i < f.params.length; i++) {
                sig.append(i == 1 ? "" : ", ").append(param(f.params[i], "a" + i, "b" + i));
                pass.append(", a").append(i).append(f.params[i].k == Ty.K.INT ? ", b" + i : "");
            }
            o.append("    static ").append(k.javaName).append(" n_").append(k.javaName).append('(').append(sig)
                    .append(") {\n        ").append(k.javaName).append(" o = new ").append(k.javaName).append('(')
                    .append(k.typeVar).append(");\n        ").append(typed.get(f)).append("(o").append(pass)
                    .append(");\n        return o;\n    }\n");
        }
        if (!bodies.isEmpty()) {
            o.append(LOCAL_HELPERS);
        }
        o.append(slow);
        for (String h : intHelpers) {
            o.append(intHelper(h));
        }
        for (String fb : fieldBoxers) {
            String[] p = fb.split(" ");
            o.append("    static Object fb_").append(p[0]).append('_').append(p[1]).append('(').append(p[0])
                    .append(" o) {\n        return o.b_").append(p[1]).append(" != null ? o.b_").append(p[1])
                    .append(" : Long.valueOf(o.t_").append(p[1]).append(");\n    }\n");
        }
        for (Layouts.Cls k : unboxers) {
            o.append("    static ").append(k.javaName).append(" u_").append(k.javaName)
                    .append("(Object o) {\n        if (o instanceof ").append(k.javaName).append(") {\n            return (")
                    .append(k.javaName).append(") o;\n        }\n        if (o == Py.NONE) {\n            return null;\n")
                    .append("        }\n        throw Typed.mismatch(o);\n    }\n");
        }
        return o.toString();
    }
}
