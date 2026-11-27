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
import java.util.TreeMap;
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
        /// Whether `lc` is known to fit a Java `int` (a length, an index
        /// `find` answered), so adding a constant of 31 bits to it cannot
        /// overflow a `long`.
        boolean small;
        /// A guessed value (Infer#guessed) held as a pair: `gr`, a plain
        /// Java name of type `gt`, holds it while `go`, the plain name of
        /// the object that replaces it, is null. `c` reads the pair as an
        /// object and `t` is OBJECT, so code that does not know pairs reads it
        /// right; code that does takes the typed form when `go` is null.
        String gr;
        String go;
        Ty gt;
        /// A one-char read `s[i]` of a str with its pair count at hand: the
        /// str, the long index and the count, all plain or free of effects
        /// (see [#compareOp]); or null.
        String[] charAt;
        /// A value with a typed form valid only while a test holds: `gg`,
        /// a Java test free of side effects; `gview`, the typed form, which
        /// holds the value while `gg` is true; `ga`, the value as an object,
        /// right whatever `gg` says. `gstable` is false when `gg` reads a
        /// field, which a call evaluated between the test and the read could
        /// change. A pair is one (`gg` tests its object for null); so is the
        /// result of an expression over such values (see [#split]).
        String gg;
        E gview;
        String ga;
        boolean gstable;
        /// The value's truth as a Java boolean expression evaluating the
        /// same operands once, when one is cheaper than testing `c` (a
        /// comparison answers its truth without boxing a bool); or null.
        String tst;

        E(String c, Ty t) {
            this(c, t, null);
        }

        E(String c, Ty t, String lc) {
            this(c, t, lc, null, null);
        }

        E(String c, Ty t, String lc, String ll, String bg) {
            this.c = c;
            // A value the pass had no evidence for (Ty#UNSEEN) is, once
            // written, an object of any type: joined with a typed one it is
            // untyped, never converted to the other's type.
            this.t = t != null && t.unseen() ? Ty.OBJECT : t;
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
    /// The exceptions of the handlers being lowered, innermost last: what a
    /// bare `raise` raises again.
    private final List<String> handlers = new ArrayList<String>();
    /// For each loop being lowered, innermost first, the label a break
    /// leaves by: the block around a loop with an `else`, or "" for a plain
    /// break.
    private final java.util.ArrayDeque<String> loopLabels = new java.util.ArrayDeque<String>();
    /// The int ranges of the function being emitted, and the locals they
    /// put in a Java `long` (see `IntRanges`).
    private IntRanges ranges;
    private Set<String> longLocals = new HashSet<String>();
    /// Untyped locals that accumulate with `+=` / `-=`: held as a long and
    /// an object (null while the long is the value), so adding an int to
    /// one that holds an int allocates nothing. The object takes anything
    /// else, and every read boxes the pair back into one value.
    private Set<String> accLocals = new HashSet<String>();
    /// Str locals bound by nothing but `name = value` statements: each has
    /// an `int` beside it (`sp` + its Java name) holding its surrogate pair
    /// count once a subscript asked, reset by every store, so `s[i]` in a
    /// loop does not count them again on every read (Typed#sat).
    private Set<String> flatStr = new HashSet<String>();

    /// The names every binding of which, in `body` outside nested functions
    /// and classes, is an assignment statement with that one name as its
    /// only target. None when the body has a `match`, which binds by
    /// pattern.
    static Set<String> simplyAssigned(List<Stmt> body) {
        Set<String> simple = new HashSet<String>();
        Set<String> other = new HashSet<String>();
        boolean[] match = new boolean[1];
        for (Stmt s : body) {
            bindingsIn(s, simple, other, match);
        }
        if (match[0]) {
            return new HashSet<String>();
        }
        simple.removeAll(other);
        return simple;
    }

    private static void bindingsIn(Object o, Set<String> simple, Set<String> other, boolean[] match) {
        if (o instanceof FunctionDef || o instanceof ClassDef) {
            // Its name is bound here; its body is a scope of its own.
            other.add(o instanceof FunctionDef ? ((FunctionDef) o).name : ((ClassDef) o).name);
            return;
        }
        if (o instanceof Lambda) {
            return;
        }
        if (o instanceof Match) {
            match[0] = true;
        }
        List<String> out = new ArrayList<String>();
        if (o instanceof Assign) {
            List<Expr> ts = ((Assign) o).targets;
            if (ts.size() == 1 && ts.get(0) instanceof Name) {
                simple.add(((Name) ts.get(0)).id);
            } else {
                for (Expr t : ts) {
                    names(t, out);
                }
            }
        } else if (o instanceof AugAssign) {
            names(((AugAssign) o).target, out);
        } else if (o instanceof AnnAssign) {
            names(((AnnAssign) o).target, out);
        } else if (o instanceof For) {
            names(((For) o).target, out);
        } else if (o instanceof Comprehension) {
            names(((Comprehension) o).target, out);
        } else if (o instanceof NamedExpr) {
            out.add(((NamedExpr) o).target.id);
        } else if (o instanceof WithItem && ((WithItem) o).vars != null) {
            names(((WithItem) o).vars, out);
        } else if (o instanceof ExceptHandler && ((ExceptHandler) o).name != null) {
            out.add(((ExceptHandler) o).name);
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                names(t, out);
            }
        } else if (o instanceof Import || o instanceof ImportFrom) {
            for (Alias a : o instanceof Import ? ((Import) o).names : ((ImportFrom) o).names) {
                String n = a.asname != null ? a.asname : a.name;
                int dot = n.indexOf('.');
                out.add(dot < 0 ? n : n.substring(0, dot));
            }
        }
        other.addAll(out);
        Infer.children(o, x -> bindingsIn(x, simple, other, match));
    }
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
        for (Infer.Fn f : in.emitFns().values()) {
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
        for (Infer.Fn f : in.emitFns().values()) {
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
        if (!(o instanceof Node)) {
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
                        && ((Attribute) t).value instanceof Name && initSelf.equals(((Name) ((Attribute) t).value).id))) {
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
        Layouts.Cls k = in.emitClass(n);
        if (k != null && (b == null || b == Scope.Binding.GLOBAL)) {
            Layouts.Cls o = Layouts.resolver(k, "__init__");
            if (o == null) {
                return true;
            }
            FunctionDef init = o.typable.get("__init__");
            return init != null && pureInits.contains(in.emitFns().get(init));
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
        for (Layouts.Cls k : layouts.classes.values()) {
            Map<String, Ty> m = new LinkedHashMap<String, Ty>();
            for (String f : k.fields) {
                Ty t = in.fieldType(k, f);
                if (t.k == Ty.K.DICT && in.guarded(k, f)) {
                    // A guarded dict (Infer#guessedDict) stays an object
                    // slot, which any store fits: only what is read from
                    // it is taken to be a dict, and checked as one.
                    continue;
                }
                if (representable(t) && in.definitelySet(k, f)) {
                    m.put(f, t);
                } else if (representable(t) && in.openable(k, f, t)) {
                    m.put(f, t);
                    openFields.computeIfAbsent(k, x -> new HashSet<String>()).add(f);
                }
            }
            typedFields.put(k, m);
        }
        // A typed field no generic code may reach directly: the generic
        // code goes through getField/setField, which box. Done before any
        // body is written: a typed function holding a nested def writes
        // that def's generic code as part of its own.
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

    /// Writes the typed bodies. Every module's typed fields are decided
    /// ([#plan]) before any module's bodies, which read the fields of
    /// other modules' classes as those modules hold them (see
    /// [#typedField]).
    void planBodies() {
        for (Infer.Fn f : in.emitFns().values()) {
            byScope.put(f.def.scope, f);
            if (f.cls != null && !f.def.name.equals("__init__") && !in.removed(f.def)) {
                String key = f.def.name + "/" + (f.params.length - 1);
                List<Infer.Fn> g = groups.get(key);
                if (g == null) {
                    g = new ArrayList<Infer.Fn>();
                    groups.put(key, g);
                }
                g.add(f);
            }
        }
        Set<Infer.Fn> ok = new LinkedHashSet<Infer.Fn>(in.emitFns().values());
        for (Infer.Fn f : in.emitFns().values()) {
            // Nothing reaches it: there is no body to type, and no types
            // its calls and stores were held to.
            if (in.removed(f.def)) {
                ok.remove(f);
            }
        }
        Map<Infer.Fn, Set<Infer.Fn>> deps = new HashMap<Infer.Fn, Set<Infer.Fn>>();
        Map<Infer.Fn, Set<String>> gdeps = new HashMap<Infer.Fn, Set<String>>();
        for (Infer.Fn f : ok) {
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
                    IntRanges.Iv[] a = g == null || !ok.contains(g) || g.cls != null || g.def.scope.hasYield ? null
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

    /// Typed names given so far: a property's getter and setter, or a
    /// function defined twice, share a Python name and not a Java one.
    private final Set<String> typedNames = new HashSet<String>();

    private String typedName(Infer.Fn f) {
        String base = f.cls != null ? "TM_" + f.cls.javaName + "_" + Emitter.mangle(f.def.name)
                : "TF_" + Emitter.mangle(f.def.name);
        String n = base;
        for (int i = 2; !typedNames.add(n); i++) {
            n = base + "$" + i;
        }
        return n;
    }

    /// Field `f` of `k` read on `recv`, an expression of `k`'s Java class,
    /// as an object, or null when the field is not held typed.
    String foreignGet(Layouts.Cls k, String f, String recv) {
        Ty t = typedField(k, f);
        if (t == null) {
            return null;
        }
        String m = Emitter.mangle(f);
        if (t.k == Ty.K.INT) {
            return dual(recv + ".t_" + m, recv + ".b_" + m).c;
        }
        if (guardedField(k, f)) {
            return pairE(recv + ".t_" + m, recv + ".b_" + m, t, present(k, f, recv + ".b_" + m, recv)).c;
        }
        return box(new E(recv + ".t_" + m, t));
    }

    /// Typed fields no constructor is known to set (see Infer#openable):
    /// held as a guarded pair whose object half starts as Typed.UNSET.
    private final Map<Layouts.Cls, Set<String>> openFields = new HashMap<Layouts.Cls, Set<String>>();

    /// Whether typed field `f` of `owner` may be absent (see [#openFields]).
    boolean openField(Layouts.Cls owner, String f) {
        // A subclass shares the slot an ancestor declares, absent or not;
        // each class is asked of the module that decided its fields.
        for (Layouts.Cls x = owner; x != null; x = x.parent) {
            TypedEmit home = x.typed != null ? x.typed : this;
            Set<String> s = home.openFields.get(x);
            if (s != null && s.contains(f)) {
                return true;
            }
        }
        return false;
    }

    /// The object half `o` of field `f` of `k` on `recv` as a value: an
    /// absent field (Typed.UNSET) is read generically.
    private String present(Layouts.Cls k, String f, String o, String recv) {
        return openField(k, f) ? "Typed.present(" + o + ", " + recv + ", " + Emitter.javaString(f) + ")" : null;
    }

    Ty typedField(Layouts.Cls owner, String f) {
        if (owner != null && owner.typed != null && owner.typed != this) { //NOPMD CompareObjectsWithEquals - identity is meant
            // A field a compiled class of another module declares (one this
            // module's class inherits) is held as that module decided.
            return owner.typed.typedField(owner, f);
        }
        Map<String, Ty> m = typedFields.get(owner);
        return m == null ? null : m.get(f);
    }

    // ---- Java types ------------------------------------------------------

    /// How typed code names compiled class `k`: qualified by its module's
    /// Java class, which names it from any module of the program (an
    /// instance of another module's class is typed as any other).
    static String clsJ(Layouts.Cls k) {
        return k.ref != null ? k.ref : k.javaName;
    }

    static String jt(Ty t) {
        switch (t.k) {
            case BOOL:
                return "boolean";
            case FLOAT:
                return "double";
            case STR:
                return "String";
            case INST:
                return clsJ(t.cls);
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
        if (e.gview != null) {
            return e.go != null ? e.c : "(" + e.gg + " ? " + box(e.gview) + " : " + e.ga + ")";
        }
        switch (e.t.k) {
            case BOOL:
                // A constant needs no call: `return True` in bs4's match
                // rules boxed through Py.bool on every return.
                return "true".equals(e.c) ? "Py.TRUE" : "false".equals(e.c) ? "Py.FALSE" : "Py.bool(" + e.c + ")";
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

    /// Guarded `e` as an object where its test is false: a pair's object
    /// itself.
    private static String orElse(E e) {
        return e.go != null ? e.go : e.ga;
    }

    /// Loop source `e`, a list, as a PyList: a guarded pair is typed as an
    /// object and holds its list type beside it (see [#pairE]).
    private String listOf(E e) {
        return e.gview != null ? conv(e, e.gt) : e.t.k == Ty.K.LIST ? e.c : unbox(box(e), e.t);
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
                return unboxerName(to.cls) + "(" + c + ")";
            default:
                return c;
        }
    }

    String conv(E e, Ty to) {
        String tj = jt(to);
        if ("Object".equals(tj)) {
            return box(e);
        }
        if (e.gview != null) {
            try {
                return "(" + e.gg + " ? " + conv(e.gview, to) + " : " + unbox(orElse(e), to) + ")";
            } catch (Unsupported u) {
                return unbox(e.c, to);
            }
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
        if (e.gview != null) {
            return "(" + e.gg + " ? " + toD(e.gview) + " : Typed.asFloat(" + orElse(e) + "))";
        }
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

    /// Whether an instance typed `k` may answer its truth itself: `k`, a
    /// class above it or one below it anywhere in the program binds
    /// `__bool__` or `__len__`, or has a base besides its compiled parent.
    /// The whole program, not this module's classes: contextlib's
    /// `while self._exit_callbacks` holds a `deque` of `collections`, whose
    /// `__len__` a test of this module alone took for "never empty".
    private boolean customTruth(Layouts.Cls k) {
        return hierarchyBinds(k, "__bool__", "__len__");
    }

    /// Whether `k`, a class above it or one below it in the whole program
    /// binds `a` or `b`, or has a base besides its compiled parent.
    private boolean hierarchyBinds(Layouts.Cls k, String a, String b) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (c.extraBases || c.bodyNames.contains(a) || c.bodyNames.contains(b)) {
                return true;
            }
        }
        for (Layouts.Cls c : in.subclasses(k)) {
            if (c.extraBases || c.bodyNames.contains(a) || c.bodyNames.contains(b)) {
                return true;
            }
        }
        return false;
    }

    String test(E e) {
        if (e.tst != null) {
            return e.tst;
        }
        if (e.gview != null) {
            return "(" + e.gg + " ? " + test(e.gview) + " : Py.isTrue(" + orElse(e) + "))";
        }
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

    /// The cells of the function being lowered that a closure shares and
    /// something stores into.
    private Set<String> trueCells = new TreeSet<String>();

    /// Whether [#var] answers the cell of a [#trueCells] name: not while
    /// the signature and the parameters copied into cells are written.
    private boolean cellsLive;

    /// Int locals of the function being lowered that a read may find
    /// unbound: their object half starts as Typed.UNBOUND.
    private Set<String> unboundInts = new HashSet<String>();

    /// Whether `n`, read from the scope being lowered, is one of [#trueCells].
    private boolean trueCell(String n) {
        if (!trueCells.contains(n) || compVar(n) != null) {
            return false;
        }
        Scope.Binding b = sc.resolve(n);
        return b == Scope.Binding.CELL && sc == cur.def.scope //NOPMD CompareObjectsWithEquals - identity is meant
                || b == Scope.Binding.FREE && sc.owner(n) == cur.def.scope; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    private String cellRef(String n) {
        return em.cellName(cur.def.scope, n) + "[0]";
    }

    private String var(String n) {
        String c = compVar(n);
        if (c == null && cellsLive && trueCell(n)) {
            // A store a lowering writes directly (a loop target) goes to
            // the cell; a Java long, double or boolean boxes to the value
            // Python holds.
            return cellRef(n);
        }
        return c != null ? c : "v_" + Emitter.mangle(n);
    }

    /// Where `n` resolves from the scope being lowered. A cell only
    /// comprehensions capture is a plain local: they are lowered in line,
    /// reading it where they stand. Inside one, its own targets and the
    /// locals of the function (or of a comprehension around it) are locals.
    private Scope.Binding binding(String n) {
        Scope.Binding b = sc.resolve(n);
        if (b == Scope.Binding.CELL && (in.compOnlyCell(sc, n) || compVar(n) != null)) {
            return Scope.Binding.LOCAL;
        }
        if (b == Scope.Binding.FREE && sc.kind == Scope.Kind.COMPREHENSION) {
            Scope o = sc.owner(n);
            // A name Emitter.uncell took off the function's cells (only
            // helper comprehensions read it) is a plain local of it too.
            if (o == cur.def.scope && (in.compOnlyCell(o, n) || !o.cells.contains(n)) || o != null && o.kind == Scope.Kind.COMPREHENSION //NOPMD CompareObjectsWithEquals - identity is meant
                    && compVars.containsKey(o)) {
                return Scope.Binding.LOCAL;
            }
        }
        return b;
    }

    /// The Java local of each target of the comprehensions being lowered.
    private final Map<Scope, Map<String, String>> compVars = new IdentityHashMap<Scope, Map<String, String>>();

    /// The Java local holding comprehension target `n` as seen from the
    /// scope being lowered, or null when `n` is not one.
    private String compVar(String n) {
        for (Scope s = sc; s != null && s.kind == Scope.Kind.COMPREHENSION; s = s.parent) {
            Map<String, String> m = compVars.get(s);
            if (m != null && s.isLocal(n)) {
                return m.get(n);
            }
        }
        return null;
    }

    /// The type comprehension target `n` is held as, seen from the scope
    /// being lowered (see [#compTy]); OBJECT when it is not one.
    private Ty compVarType(String n) {
        for (Scope s = sc; s != null && s.kind == Scope.Kind.COMPREHENSION; s = s.parent) {
            Map<String, String> m = compVars.get(s);
            if (m != null && s.isLocal(n)) {
                return compTy(s, n);
            }
        }
        return Ty.OBJECT;
    }

    /// The Java type a temporary for target `n` of comprehension scope `s`
    /// holds: what the analysis says its iterable yields (Infer#compLocal),
    /// where that has a Java type of its own; an int keeps its big half
    /// beside the long, which a temporary has no room for, so it is boxed.
    private Ty compTy(Scope s, String n) {
        Ty t = in.compLocal(s, n);
        if (t == null || t.k == Ty.K.INT || t.k == Ty.K.BOT || !representable(t) || "Object".equals(jt(t))) {
            return Ty.OBJECT;
        }
        return t;
    }

    /// Binds the target of generator `g` to `raw`, the element its
    /// iterable gave. Where the analysis narrowed the target by the
    /// generator's `isinstance` filter (Infer#narrowedBy), an element of
    /// another class is skipped before it reaches the typed variable: the
    /// filter would skip it anyway, and a cast would fail on it first.
    /// The object half of each comprehension target held as a guess (see
    /// Infer#guessedCompVar), by the Java variable of its typed half.
    private final Map<String, String> compOver = new HashMap<String, String>();

    private void compStore(Comprehension g, String raw, StringBuilder p, int d) {
        if (g.target instanceof Name) {
            String v = compVar(((Name) g.target).id);
            String o = v == null ? null : compOver.get(v);
            if (o != null) {
                String x = exprTemp("Object");
                indent(p, d);
                p.append(x).append(" = ").append(raw).append(";\n");
                indent(p, d);
                fitInto(x, compVarType(((Name) g.target).id), v, o, p, d);
                // The typed half holds nothing of an element that missed:
                // never the element before it.
                indent(p, d);
                p.append("if (").append(o).append(" != null) ").append(v).append(" = null;\n");
                return;
            }
        }
        Layouts.Cls k = in.narrowedBy(g);
        if (k != null && g.target instanceof Name
                && compVarType(((Name) g.target).id).k == Ty.K.INST) {
            String o = exprTemp("Object");
            indent(p, d);
            p.append(o).append(" = ").append(raw).append(";\n");
            indent(p, d);
            p.append("if (!(").append(o).append(" instanceof ").append(clsJ(k)).append(")) {\n");
            indent(p, d + 1);
            p.append("continue;\n");
            indent(p, d);
            p.append("}\n");
            store(g.target, new E("((" + clsJ(k) + ") " + o + ")", Ty.inst(k, false)), p, d);
            return;
        }
        store(g.target, new E(raw, Ty.OBJECT), p, d);
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

    /// The lowering's own locals (`GenLowering.hidden`) a generator body
    /// used: values kept across a suspension, fields of the generator.
    private final Set<String> hiddenLocals = new TreeSet<String>();

    private Ty localType(String n) {
        if (compVar(n) != null) {
            return compVarType(n);
        }
        if (inGen && n.startsWith("$y")) {
            hiddenLocals.add(n);
            return Ty.OBJECT;
        }
        if (trueCells.contains(n)) {
            return Ty.OBJECT;
        }
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
        fallback = null;
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
        boolean generator = f.def.scope.hasYield;
        cellsLive = false;
        trueCells = new TreeSet<String>(f.def.scope.cells);
        trueCells.removeAll(Infer.compOnlyCells(f.def));
        fallbackStores = null;
        loopStored = null;
        for (String n : f.def.scope.cells) {
            // A cell a lazy generator captures (Emitter.uncell kept it) and
            // the function rebinds: the generator must read the value at the
            // time it runs, so it shares the cell too.
            if (!trueCells.contains(n) && !Infer.paramCells(f.def).contains(n) && !settled(n)) {
                trueCells.add(n);
            }
        }
        for (String n : trueCells) {
            // Shared with a closure: an Object in a cell, never a Java local.
            f.locals.remove(n);
            longLocals.remove(n);
        }
        if (generator && asLong || f.def.isAsync) {
            throw new Unsupported("signature: " + (f.def.isAsync ? "async" : "long generator"));
        }
        for (String n : f.locals.keySet()) {
            if (binding(n) != Scope.Binding.LOCAL) {
                throw new Unsupported("non-local " + n);
            }
        }
        widenRebound(f);
        da = new DefAssign();
        da.function(f.def.body, sc.params);
        unboundInts = new HashSet<String>();
        for (Name x : da.unsafe) {
            Ty lt = f.locals.get(x.id);
            if (lt == null || sc.params.contains(x.id) || generator || asLong) {
                continue;
            }
            if (lt.k == Ty.K.INT) {
                // A read that may come first: held as a pair, whose object
                // half can say "unbound", even where a long would do.
                unboundInts.add(x.id);
            } else if (!"null".equals(zero(lt))) {
                // A bool or a float has no value meaning "unbound": held as
                // an object, whose null does.
                f.locals.put(x.id, Ty.OBJECT);
            }
        }
        longLocals.removeAll(unboundInts);
        soleStores = soleStores(f.def.body);
        fallbackStores = null;
        loopStored = null;
        // A parameter is bound on entry as well: its one assignment in the
        // body is a second store, and folding it gave every read the value
        // only one path assigns.
        soleStores.keySet().removeAll(sc.params);
        accLocals = accumulators(f);
        flatStr = generator || asLong ? new HashSet<String>() : simplyAssigned(f.def.body);
        flatStr.removeAll(sc.params);
        for (java.util.Iterator<String> it = flatStr.iterator(); it.hasNext();) {
            String n = it.next();
            Ty lt = f.locals.get(n);
            if (lt == null || lt.k != Ty.K.STR || trueCell(n) || longLocals.contains(n) || accLocals.contains(n)) {
                it.remove();
            }
        }
        for (Name x : da.unsafe) {
            if (accLocals.contains(x.id) && !sc.params.contains(x.id) && !generator && !asLong) {
                // An accumulator is a pair as well.
                unboundInts.add(x.id);
            }
        }
        folded = new IdentityHashMap<Expr, Object>();
        recycled = ListRecycling.find(f.def.body, f.names, sc);
        scalars = findScalars(f);
        StringBuilder b = new StringBuilder();
        b.append("    static ").append(retJ(f)).append(' ').append(lp != null ? longName(f) : typed.get(f)).append('(');
        Set<String> declared = new HashSet<String>(f.names);
        StringBuilder norm = new StringBuilder();
        // A parameter the body rebinds to other types (an int made a tuple)
        // arrives under its own name and is copied into the local.
        StringBuilder rebound = new StringBuilder();
        paramAlias.clear();
        for (int i = 0; i < f.names.size(); i++) {
            String n = f.names.get(i);
            if (lp != null && lp[i] != null) {
                b.append(i == 0 ? "" : ", ").append("long ").append(var(n));
                continue;
            }
            Ty lt = f.locals.get(n);
            String pv = var(n);
            String pb = big(n);
            boolean pi = f.params[i].k == Ty.K.INT;
            if (lt != null && !longLocals.contains(n)
                    && (pi != (lt.k == Ty.K.INT) || !pi && !jt(lt).equals(jt(f.params[i])))) {
                if (lt.k == Ty.K.INT || longParams.get(f) != null) {
                    throw new Unsupported("parameter " + n + " of " + f.def.name + " rebound: " + f.params[i] + " -> " + lt);
                }
                pv = "p_" + var(n);
                pb = "p_" + big(n);
                E arg = f.params[i].k == Ty.K.INT ? dual(pv, pb) : new E(pv, f.params[i]);
                if ("Object".equals(jt(lt))) {
                    rebound.append("        Object ").append(var(n)).append(" = ").append(box(arg)).append(";\n");
                } else {
                    // The argument need not convert (`xy = list(xy)` takes a
                    // tuple): read as it came until the first statement, which
                    // must rebind it, stores the local.
                    Stmt first = f.def.body.isEmpty() ? null : f.def.body.get(0);
                    if (!(first instanceof Assign) || ((Assign) first).targets.size() != 1
                            || !(((Assign) first).targets.get(0) instanceof Name)
                            || !n.equals(((Name) ((Assign) first).targets.get(0)).id)) {
                        throw new Unsupported("parameter " + n + " of " + f.def.name + " rebound: " + f.params[i] + " -> " + lt);
                    }
                    paramAlias.put(n, arg);
                    rebound.append("        ").append(jt(lt)).append(' ').append(var(n)).append(" = ")
                            .append("double".equals(jt(lt)) ? "0.0" : "boolean".equals(jt(lt)) ? "false" : "null")
                            .append(";\n");
                }
            }
            b.append(i == 0 ? "" : ", ").append(param(f.params[i], pv, pb));
            if (f.params[i].k == Ty.K.INT) {
                // A caller holding only the object passes it whole.
                norm.append("        if (").append(pb).append(" instanceof Long) {\n            ").append(pv)
                        .append(" = ((Long) ").append(pb).append(").longValue();\n            ").append(pb)
                        .append(" = null;\n        }\n");
            }
        }
        b.append(") {\n").append(norm).append(rebound);
        if (generator && (rebound.length() > 0 || !paramAlias.isEmpty())) {
            throw new Unsupported("generator parameter rebound");
        }
        IntRanges.Iv[] dl = asLong || generator ? null : longParams.get(f);
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
        // A generator's locals live across suspensions: from here on the
        // declarations become fields of the generator object.
        int declAt = b.length();
        for (Map.Entry<String, Ty> e : f.locals.entrySet()) {
            if (longLocals.contains(e.getKey())) {
                if (declared.contains(e.getKey())) {
                    continue;
                }
                declared.add(e.getKey());
                b.append("        long ").append(var(e.getKey())).append(" = 0L;\n");
            } else if (declared.add(e.getKey())) {
                if (e.getValue().k == Ty.K.INT || accLocals.contains(e.getKey())) {
                    b.append("        long ").append(var(e.getKey())).append(" = 0L;\n        Object ")
                            .append(big(e.getKey())).append(unboundInts.contains(e.getKey()) ? " = Typed.UNBOUND;\n"
                                    : " = null;\n");
                } else {
                    b.append("        ").append(jt(e.getValue())).append(' ').append(var(e.getKey())).append(" = ")
                            .append(zero(e.getValue())).append(";\n");
                }
            }
        }
        for (String n : new TreeSet<String>(flatStr)) {
            b.append("        int sp").append(var(n)).append(" = -1;\n");
        }
        for (String n : new TreeSet<String>(f.locals.keySet())) {
            if (guessLocal(n)) {
                b.append("        Object ").append(over(n)).append(" = null;\n");
            }
        }
        scalarDecls(b);
        for (String n : Infer.paramCells(f.def)) {
            // The cell a closure reads, beside the typed parameter.
            int pi = f.names.indexOf(n);
            Ty lt = f.locals.get(n);
            String v;
            if (lp != null && lp[pi] != null || longLocals.contains(n)) {
                v = "Long.valueOf(" + var(n) + ")";
            } else if (lt != null && lt.k == Ty.K.INT) {
                v = box(dual(var(n), big(n)));
            } else {
                v = box(new E(var(n), lt == null ? Ty.OBJECT : lt));
            }
            b.append("        final Object[] ").append(em.cellName(f.def.scope, n)).append(" = new Object[] {").append(v)
                    .append("};\n");
        }
        cellsLive = false;
        for (String n : trueCells) {
            // A cell a closure shares and something stores into: every read
            // and store goes through it, so each sees the other's.
            int pi = f.names.indexOf(n);
            String v = "null";
            if (pi >= 0) {
                IntRanges.Iv[] lpp = lp;
                if (lpp != null && lpp[pi] != null) {
                    v = "Long.valueOf(" + var(n) + ")";
                } else if (f.params[pi].k == Ty.K.INT) {
                    v = box(dual(var(n), big(n)));
                } else {
                    v = box(new E(var(n), f.params[pi]));
                }
            }
            b.append("        final Object[] ").append(em.cellName(f.def.scope, n)).append(" = new Object[] {").append(v)
                    .append("};\n");
        }
        cellsLive = true;
        temps = new StringBuilder();
        if (generator) {
            return generator(f, b, declAt);
        }
        int at = b.length();
        boolean abrupt = block(f.def.body, b, 2);
        temps.append(fallbackDecls());
        b.insert(at, temps);
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

    /// Whether the body being emitted is a generator's step (see `generator`).
    private boolean inGen;

    /// Ends `b`, the typed version of generator `f` up to its declarations
    /// (from `declAt`), with the generator object it returns: the locals as
    /// its fields, each parameter copied into its own, and the body lowered
    /// into its states by [GenLowering], every statement that cannot suspend
    /// emitted by this class.
    private String generator(Infer.Fn f, StringBuilder b, int declAt) {
        String decls = b.substring(declAt);
        b.setLength(declAt);
        StringBuilder copies = new StringBuilder();
        StringBuilder own = new StringBuilder();
        for (int i = 0; i < f.names.size(); i++) {
            String n = f.names.get(i);
            if (f.params[i].k == Ty.K.INT) {
                copy(copies, own, "long", var(n));
                copy(copies, own, "Object", big(n));
            } else {
                copy(copies, own, jt(f.params[i]), var(n));
            }
        }
        boolean saved = inGen;
        inGen = true;
        hiddenLocals.clear();
        String step;
        GenLowering g = new GenLowering(genTarget(), em.file(), sc);
        g.plain = true;
        try {
            step = g.step(f.def.body);
        } finally {
            inGen = saved;
        }
        b.append(copies);
        b.append("        return new PyGenerator(").append(Emitter.javaString(f.def.name)).append(", false) {\n");
        b.append(own).append(decls).append(g.fields());
        for (String n : hiddenLocals) {
            b.append("                Object ").append(var(n)).append(" = null;\n");
        }
        if (g.usesReturnField()) {
            b.append("                Object ret$;\n");
        }
        b.append("\n            @Override\n            protected Object step() {\n");
        b.append(temps).append(fallbackDecls());
        b.append(step);
        b.append("            }\n        };\n    }\n");
        return b.toString();
    }

    /// A parameter the body rebinds to another type (`parser=None`, then
    /// `if parser is None: parser = XMLParser()`), other than by its first
    /// statement: its local holds either, so it is an object here.
    private void widenRebound(Infer.Fn f) {
        if (longParams.get(f) != null) {
            return;
        }
        Stmt first = f.def.body.isEmpty() ? null : f.def.body.get(0);
        for (int i = 0; i < f.names.size(); i++) {
            String n = f.names.get(i);
            Ty lt = f.locals.get(n);
            if (lt == null || lt.k == Ty.K.INT || f.params[i].k == Ty.K.INT
                    || "Object".equals(jt(lt)) || jt(lt).equals(jt(f.params[i]))) {
                continue;
            }
            boolean byFirst = first instanceof Assign && ((Assign) first).targets.size() == 1
                    && ((Assign) first).targets.get(0) instanceof Name
                    && n.equals(((Name) ((Assign) first).targets.get(0)).id);
            if (!byFirst) {
                f.locals.put(n, Ty.OBJECT);
            }
        }
    }

    /// The final copy of parameter `v` the generator object takes, and the
    /// field of the object that holds it.
    private static void copy(StringBuilder copies, StringBuilder own, String jt, String v) {
        copies.append("        final ").append(jt).append(" g$").append(v).append(" = ").append(v).append(";\n");
        own.append("        ").append(jt).append(' ').append(v).append(" = g$").append(v).append(";\n");
    }

    /// This emitter as the target of a typed generator's lowering. Every
    /// statement starts with no flow facts: the states it sits between are
    /// joined by jumps this class does not see, so what held before one
    /// need not hold at the next.
    private GenTarget genTarget() {
        return new GenTarget() {
            @Override
            public void stmt(Stmt s, StringBuilder out, int ind) {
                forgetFlow();
                StringBuilder r = new StringBuilder();
                if (TypedEmit.this.stmt(s, r, ind + 1)) {
                    // The lowering goes on writing after it, as javac must allow.
                    indent(out, ind);
                    out.append("if (true) {\n").append(r);
                    indent(out, ind);
                    out.append("}\n");
                } else {
                    out.append(r);
                }
                forgetFlow();
            }

            @Override
            public void store(Expr t, String value, StringBuilder out, int ind) {
                forgetFlow();
                TypedEmit.this.store(t, new E(value, Ty.OBJECT), out, ind);
                forgetFlow();
            }

            @Override
            public void storeName(String n, String value, StringBuilder out, int ind) {
                store(new Name(n), value, out, ind);
            }

            @Override
            public String nameLvalue(String n) {
                Ty t = cur.locals.get(n);
                if (t == null || !"Object".equals(jt(t)) || longLocals.contains(n) || accLocals.contains(n)) {
                    throw new Unsupported("handler name " + n);
                }
                return var(n);
            }

            @Override
            public String expr(Expr e) {
                return outOfStmt(() -> box(TypedEmit.this.expr(e)));
            }

            @Override
            public String cond(Expr e) {
                return outOfStmt(() -> testExpr(e));
            }

            @Override
            public String fresh(String prefix) {
                return prefix + "$" + (++tmp);
            }

            @Override
            public RuntimeException unsupported(Node n, String what) {
                return new Unsupported(what);
            }

            @Override
            public List<String> handlers() {
                return handlers;
            }
        };
    }

    /// `f` evaluated where no statement is being lowered: nothing may be
    /// hoisted ahead of it, and no flow fact holds.
    private String outOfStmt(java.util.function.Supplier<String> f) {
        StringBuilder savedPre = pre;
        Stmt savedStmt = curStmt;
        pre = null;
        curStmt = null;
        forgetFlow();
        try {
            return f.get();
        } finally {
            pre = savedPre;
            curStmt = savedStmt;
            forgetFlow();
        }
    }

    /// The declarations of the temporaries the function's generic
    /// expressions took.
    private String fallbackDecls() {
        String d = fallback == null ? "" : em.fallbackDecls(fallback);
        return d.isEmpty() ? "" : "        " + d + "\n";
    }

    private void forgetFlow() {
        nonNull = new HashSet<String>();
        shadow = new HashMap<String, String>();
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

    /// Declarations of the temporaries expressions assign in line
    /// (`exprTemp`), put at the top of the function being lowered.
    private StringBuilder temps; //NOPMD AvoidStringBufferField - the generator's output

    /// A local of Java type `jt` declared at the top of the function, for an
    /// expression to assign a value it reads again (`(t = x) ... t`).
    private String exprTemp(String jt) {
        String t = fresh();
        temps.append("        ").append(jt).append(' ').append(t).append(" = ")
                .append("long".equals(jt) ? "0L" : "double".equals(jt) ? "0.0" : "boolean".equals(jt) ? "false" : "null")
                .append(";\n");
        return t;
    }

    /// Statements that hoisted int arithmetic (`hoistInt`) runs before, or
    /// null where nothing may run first (a loop header).
    private StringBuilder pre; //NOPMD AvoidStringBufferField - the generator's output
    private int preInd;

    private boolean stmt(Stmt s, StringBuilder b, int ind) {
        if (s instanceof FunctionDef) {
            FunctionDef d = (FunctionDef) s;
            List<Expr> made = new ArrayList<Expr>(d.decorators);
            made.addAll(d.args.defaults);
            for (Expr x : d.args.kwDefaults) {
                if (x != null) {
                    made.add(x);
                }
            }
            // The generic function object, decorated, its free names read
            // from the cells made on entry; its decorators and defaults read
            // the typed locals' values.
            Map<String, String> names = defNames(made);
            if (fallback == null) {
                fallback = em.fallbackFrame(sc);
            }
            String fn = names == null ? null : em.genericDef(fallback, sc, d, names);
            if (fn == null) {
                throw new Unsupported("nested def");
            }
            store(new Name(d.name), new E(fn, Ty.OBJECT), b, ind);
            return false;
        }
        if (s instanceof ClassDef) {
            ClassDef cd = (ClassDef) s;
            List<Expr> made = new ArrayList<Expr>(cd.decorators);
            for (Expr x : cd.bases) {
                made.add(x);
            }
            for (Keyword k : cd.keywords) {
                made.add(k.value);
            }
            Map<String, String> names = defNames(made);
            if (fallback == null) {
                fallback = em.fallbackFrame(sc);
            }
            String[] made2 = names == null ? null : em.genericClass(fallback, sc, cd, names);
            if (made2 == null) {
                throw new Unsupported("ClassDef");
            }
            // The class statement as the generic version runs it; its body
            // reads the function's cells.
            for (String line : made2[0].split("\n")) {
                if (!line.isEmpty()) {
                    indent(b, ind);
                    b.append(line).append('\n');
                }
            }
            forgetPaths();
            store(new Name(cd.name), new E(made2[1], Ty.OBJECT), b, ind);
            return false;
        }
        StringBuilder savedPre = pre;
        int savedInd = preInd;
        boolean hoist = (s instanceof ExprStmt || s instanceof Assign || s instanceof AugAssign
                || s instanceof Return || s instanceof If) && !hasWalrus(s);
        pre = hoist ? new StringBuilder() : null;
        preInd = ind;
        Stmt savedStmt = curStmt;
        curStmt = s;
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
            curStmt = savedStmt;
            stmtPure = savedPure;
        }
    }

    /// `try` as the generic code lowers it (`Emitter.tryStmt`): the same
    /// handler matching, `Typed.untake`, `sys.exception()` bookkeeping and
    /// order, so a function with a try keeps its typed version. Only the
    /// flow facts change: what the body assigns may or may not have
    /// happened when a handler runs, so none of it is known there or after.
    /// The abrupt answer follows javac's reachability exactly, as `block`
    /// requires: a try with handlers completes normally when its body or any
    /// handler does, and a finally is abrupt when its own block is.
    /// The exception variable and the label of the try whose body is being
    /// lowered as jumps (see `raiseOnly`); null elsewhere.
    private String[] localRaise;

    /// Builtin exception classes whose construction from plain values runs
    /// no user code and cannot raise.
    private static final Set<String> PLAIN_EXCEPTIONS = new HashSet<String>(Arrays.asList("ValueError", "KeyError",
            "IndexError", "TypeError", "RuntimeError", "LookupError", "ArithmeticError", "ZeroDivisionError",
            "AttributeError", "StopIteration", "Exception", "NotImplementedError", "AssertionError", "OverflowError"));

    /// Whether `body` can raise only by a `raise C(args)` statement of its
    /// own, with C a builtin exception: ifs, passes and raises over tests
    /// and arguments of int and bool locals and constants, and at least one
    /// raise. Such a try needs no Java try (see `tryStmt`).
    private boolean raiseOnly(List<Stmt> body) {
        boolean[] raises = {false};
        return raiseOnly(body, raises) && raises[0];
    }

    private boolean raiseOnly(List<Stmt> body, boolean[] raises) {
        for (Stmt s : body) {
            if (s instanceof Pass) {
                continue;
            }
            if (s instanceof If) {
                If f = (If) s;
                if (!noRaise(f.test) || !raiseOnly(f.body, raises) || !raiseOnly(f.orelse, raises)) {
                    return false;
                }
                continue;
            }
            if (s instanceof Raise && ((Raise) s).cause == null && ((Raise) s).exc instanceof Call) {
                Call c = (Call) ((Raise) s).exc;
                if (!(c.func instanceof Name) || !PLAIN_EXCEPTIONS.contains(((Name) c.func).id)
                        || binding(((Name) c.func).id) != Scope.Binding.BUILTIN || !c.keywords.isEmpty()) {
                    return false;
                }
                for (Expr a : c.args) {
                    if (!noRaise(a)) {
                        return false;
                    }
                }
                raises[0] = true;
            } else {
                return false;
            }
        }
        return true;
    }

    /// Whether `e` cannot raise: constants, int and bool locals, and
    /// comparisons, `not`, `and`/`or` and the overflow-safe int operators
    /// of those (`%` and `//` only by a non-zero constant).
    private boolean noRaise(Expr e) {
        if (e instanceof Constant) {
            Constant.Kind k = ((Constant) e).kind;
            return k == Constant.Kind.INT || k == Constant.Kind.TRUE || k == Constant.Kind.FALSE || k == Constant.Kind.NONE;
        }
        if (e instanceof Name) {
            String n = ((Name) e).id;
            if (binding(n) != Scope.Binding.LOCAL || inlineArgs != null) {
                return false;
            }
            Ty t = localType(n);
            return (t.k == Ty.K.INT || t.k == Ty.K.BOOL) && !t.opt;
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            return ("not".equals(u.op) || "-".equals(u.op)) && noRaise(u.operand);
        }
        if (e instanceof BoolOp) {
            for (Expr v : ((BoolOp) e).values) {
                if (!noRaise(v)) {
                    return false;
                }
            }
            return true;
        }
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            for (String op : c.ops) {
                if (!("==".equals(op) || "!=".equals(op) || "<".equals(op) || "<=".equals(op) || ">".equals(op)
                        || ">=".equals(op))) {
                    return false;
                }
            }
            if (!noRaise(c.left)) {
                return false;
            }
            for (Expr x : c.comparators) {
                if (!noRaise(x)) {
                    return false;
                }
            }
            return true;
        }
        if (e instanceof BinOp) {
            BinOp o = (BinOp) e;
            if ("%".equals(o.op) || "//".equals(o.op)) {
                Long k = intConstant(o.right);
                return k != null && k.longValue() != 0 && noRaise(o.left);
            }
            return ("+".equals(o.op) || "-".equals(o.op) || "*".equals(o.op) || "&".equals(o.op) || "|".equals(o.op)
                    || "^".equals(o.op)) && noRaise(o.left) && noRaise(o.right);
        }
        return false;
    }

    private boolean tryStmt(Try t, StringBuilder b, int ind) {
        if (t.star) {
            throw new Unsupported("except*");
        }
        Set<String> touched = new HashSet<String>(assigned(t.body));
        for (ExceptHandler h : t.handlers) {
            touched.addAll(assigned(h.body));
            if (h.name != null) {
                touched.add(h.name);
            }
        }
        touched.addAll(assigned(t.orelse));
        touched.addAll(assigned(t.finalbody));
        nonNull.removeAll(touched);
        shadow.keySet().removeAll(touched);
        forgetPaths();
        Set<String> entry = new HashSet<String>(nonNull);
        boolean fin = !t.finalbody.isEmpty();
        if (fin) {
            indent(b, ind);
            b.append("try {\n");
            ind++;
        }
        boolean abrupt;
        if (t.handlers.isEmpty()) {
            abrupt = block(t.body, b, ind);
        } else {
            String ok = t.orelse.isEmpty() ? null : fresh();
            if (ok != null) {
                indent(b, ind);
                b.append("boolean ").append(ok).append(" = false;\n");
            }
            boolean jump = t.orelse.isEmpty() && raiseOnly(t.body);
            String pe = fresh();
            boolean bodyAbrupt;
            if (jump) {
                // The body raises only by its own `raise` statements: each
                // is a jump to the handlers, and no Java try (a setjmp on
                // ParparVM) is entered at all.
                String lbl = fresh();
                indent(b, ind);
                b.append("PyException ").append(pe).append(" = null;\n");
                indent(b, ind);
                b.append(lbl).append(": {\n");
                String[] outer = localRaise;
                localRaise = new String[] {pe, lbl};
                try {
                    block(t.body, b, ind + 1);
                } finally {
                    localRaise = outer;
                }
                indent(b, ind);
                b.append("}\n");
                indent(b, ind);
                b.append("if (").append(pe).append(" != null) {\n");
                bodyAbrupt = false;
            } else {
                String tk = fresh();
                indent(b, ind);
                b.append("final int ").append(tk).append(" = Typed.taken();\n");
                indent(b, ind);
                b.append("try {\n");
                bodyAbrupt = block(t.body, b, ind + 1);
                if (ok != null && !bodyAbrupt) {
                    indent(b, ind + 1);
                    b.append(ok).append(" = true;\n");
                }
                String th = fresh();
                indent(b, ind);
                b.append("} catch (Throwable ").append(th).append(") {\n");
                indent(b, ind + 1);
                b.append("Typed.untake(").append(tk).append(");\n");
                indent(b, ind + 1);
                b.append("final PyException ").append(pe).append(" = Py.convert(").append(th).append(");\n");
            }
            handlers.add(pe);
            boolean catchAll = false;
            boolean allAbrupt = true;
            try {
                for (int i = 0; i < t.handlers.size(); i++) {
                    ExceptHandler h = t.handlers.get(i);
                    nonNull = new HashSet<String>(entry);
                    indent(b, ind + 1);
                    if (h.type == null) {
                        if (i != t.handlers.size() - 1) {
                            throw new Unsupported("default except not last");
                        }
                        b.append(i == 0 ? "{\n" : "} else {\n");
                        catchAll = true;
                    } else {
                        b.append(i == 0 ? "if (" : "} else if (").append(pe).append(".matches(").append(excRef(h.type))
                                .append(")) {\n");
                    }
                    if (h.name != null) {
                        Ty lt = localType(h.name);
                        // An instance local: the handler matched, so the
                        // value is one of the classes the local joins.
                        boolean inst = lt.k == Ty.K.INST;
                        if (binding(h.name) != Scope.Binding.LOCAL) {
                            throw new Unsupported("except as " + h.name);
                        }
                        if (inst && !guessLocal(h.name) && !longLocals.contains(h.name)) {
                            indent(b, ind + 2);
                            b.append(var(h.name)).append(" = (").append(jt(lt)).append(") ").append(pe)
                                    .append(".value;\n");
                        } else {
                            // Any other local takes the exception as any store
                            // of an object: converted, or into a guess's
                            // object half.
                            Name hn = new Name(h.name);
                            hn.line = h.line;
                            hn.col = h.col;
                            store(hn, new E(pe + ".value", Ty.OBJECT), b, ind + 2);
                        }
                    }
                    String hv = fresh();
                    indent(b, ind + 2);
                    b.append("final Object ").append(hv).append(" = Py.enterHandler(").append(pe).append(");\n");
                    indent(b, ind + 2);
                    b.append("try {\n");
                    boolean ha = block(h.body, b, ind + 3);
                    indent(b, ind + 2);
                    b.append("} finally {\n");
                    indent(b, ind + 3);
                    b.append("Py.exitHandler(").append(hv).append(");\n");
                    indent(b, ind + 2);
                    b.append("}\n");
                    if (h.name != null && !ha) {
                        // The name is unbound when the handler ends.
                        indent(b, ind + 2);
                        if (localType(h.name).k == Ty.K.INT) {
                            if (!unboundInts.contains(h.name)) {
                                throw new Unsupported("except as int " + h.name);
                            }
                            b.append(big(h.name)).append(" = Typed.UNBOUND;\n");
                        } else {
                            b.append(var(h.name)).append(" = null;\n");
                            if (guessLocal(h.name)) {
                                indent(b, ind + 2);
                                b.append(over(h.name)).append(" = null;\n");
                            }
                        }
                    }
                    allAbrupt &= ha;
                }
            } finally {
                handlers.remove(handlers.size() - 1);
            }
            if (!catchAll) {
                indent(b, ind + 1);
                b.append("} else {\n");
                indent(b, ind + 2);
                b.append("throw ").append(pe).append(";\n");
            }
            indent(b, ind + 1);
            b.append("}\n");
            indent(b, ind);
            b.append("}\n");
            nonNull = new HashSet<String>(entry);
            abrupt = bodyAbrupt && allAbrupt;
            if (ok != null && !bodyAbrupt) {
                indent(b, ind);
                b.append("if (").append(ok).append(") {\n");
                block(t.orelse, b, ind + 1);
                indent(b, ind);
                b.append("}\n");
                nonNull = new HashSet<String>(entry);
            }
        }
        if (fin) {
            ind--;
            indent(b, ind);
            b.append("} finally {\n");
            boolean fa = block(t.finalbody, b, ind + 1);
            indent(b, ind);
            b.append("}\n");
            nonNull = new HashSet<String>(entry);
            abrupt |= fa;
        }
        shadow.keySet().removeAll(touched);
        forgetPaths();
        return abrupt;
    }

    /// An exception class or instance for `raise` and `except`: a builtin
    /// or a module-level class by reference, a tuple of them, or any value
    /// the typed code can lower, boxed.
    private String excRef(Expr e) {
        if (e instanceof Name) {
            String n = ((Name) e).id;
            Scope.Binding k = binding(n);
            if (k == Scope.Binding.BUILTIN && !em.moduleScopeHasStarImport()) {
                return em.builtinRef(n);
            }
            if (k == Scope.Binding.GLOBAL) {
                return "g(" + em.slot(n) + ")";
            }
        }
        if (e instanceof TupleExpr) {
            StringBuilder sb = new StringBuilder("Py.tuple(");
            List<Expr> el = ((TupleExpr) e).elts;
            for (int i = 0; i < el.size(); i++) {
                if (el.get(i) instanceof Starred) {
                    throw new Unsupported("starred");
                }
                sb.append(i == 0 ? "" : ", ").append(excRef(el.get(i)));
            }
            sb.append(')');
            return Emitter.foldable(el) ? em.hoist("PyTuple", sb.toString()) : sb.toString();
        }
        return box(expr(e));
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
            // Py.setsliceFrom takes any sequences, as the generic code does.
            indent(b, ind);
            b.append("Py.setsliceFrom(").append(box(v)).append(", ").append(vp).append(", ").append(box(o))
                    .append(", ").append(op).append(");\n");
            return false;
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
        if (s instanceof Assign && dictCount((Assign) s, b, ind)) {
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
        if (s instanceof Return && inGen) {
            // A generator's step ends with pc at -1, answering the value.
            indent(b, ind);
            b.append("pc = -1;\n");
            inGen = false;
            try {
                return stmt0(s, b, ind);
            } finally {
                inGen = true;
            }
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
            boolean forever = w.test instanceof Constant && ((Constant) w.test).kind == Constant.Kind.TRUE;
            // A loop that never ends but by a break never runs its else.
            String label = w.orelse.isEmpty() || forever ? "" : fresh();
            if (!label.isEmpty()) {
                indent(b, ind);
                b.append(label).append(": {\n");
            }
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
            loopLabels.push(label);
            try {
                block(w.body, b, ind + 1);
            } finally {
                loopLabels.pop();
            }
            nonNull = inv;
            if (!Infer.hasBreak(w.body)) {
                nonNull.addAll(whenFalse(w.test));
            }
            indent(b, ind);
            b.append("}\n");
            if (!label.isEmpty()) {
                return loopElse(w.orelse, w.body, inv, b, ind);
            }
            return forever && !Infer.hasBreak(w.body);
        }
        if (s instanceof For) {
            For f = (For) s;
            String label = f.orelse.isEmpty() ? "" : fresh();
            if (!label.isEmpty()) {
                indent(b, ind);
                b.append(label).append(": {\n");
            }
            Set<String> inv = new HashSet<String>(nonNull);
            inv.removeAll(assigned(f.body));
            if (f.target instanceof Name) {
                inv.remove(((Name) f.target).id);
            }
            loopLabels.push(label);
            try {
                forLoop(f, b, ind);
            } finally {
                loopLabels.pop();
            }
            if (!label.isEmpty()) {
                return loopElse(f.orelse, f.body, inv, b, ind);
            }
            return false;
        }
        if (s instanceof Break) {
            indent(b, ind);
            String l = loopLabels.isEmpty() ? "" : loopLabels.peek();
            b.append(l.isEmpty() ? "break;\n" : "break " + l + ";\n");
            return true;
        }
        if (s instanceof Continue) {
            indent(b, ind);
            b.append("continue;\n");
            return true;
        }
        if (s instanceof Raise) {
            Raise r = (Raise) s;
            indent(b, ind);
            if (r.exc == null) {
                if (handlers.isEmpty()) {
                    throw new Unsupported("bare raise outside a handler");
                }
                b.append("throw ").append(handlers.get(handlers.size() - 1)).append(";\n");
            } else if (r.cause != null) {
                b.append("throw Py.raise(").append(excRef(r.exc)).append(", ").append(excRef(r.cause)).append(");\n");
            } else if (localRaise != null) {
                // A jump to the handlers of the try around it (see `raiseOnly`).
                // The class is looked up at run time and may have been
                // rebound to one whose construction raises: that error is the
                // one the handlers see. The Java try is entered only on this
                // path, never on the one that does not raise.
                String th = fresh();
                b.append("try {\n");
                indent(b, ind + 1);
                b.append(localRaise[0]).append(" = Py.raise(").append(excRef(r.exc)).append(");\n");
                indent(b, ind);
                b.append("} catch (Throwable ").append(th).append(") {\n");
                indent(b, ind + 1);
                b.append(localRaise[0]).append(" = Py.convert(").append(th).append(");\n");
                indent(b, ind);
                b.append("}\n");
                indent(b, ind);
                b.append("break ").append(localRaise[1]).append(";\n");
            } else {
                b.append("throw Py.raise(").append(excRef(r.exc)).append(");\n");
            }
            return true;
        }
        if (s instanceof Try) {
            return tryStmt((Try) s, b, ind);
        }
        if (s instanceof Assert) {
            Assert a = (Assert) s;
            indent(b, ind);
            b.append("if (!(").append(testExpr(a.test)).append(")) {\n");
            indent(b, ind + 1);
            if (a.msg == null) {
                b.append("throw Py.raise(Py.ASSERTION_ERROR);\n");
            } else {
                b.append("throw Py.raise(Py.call(Py.ASSERTION_ERROR, ").append(box(expr(a.msg))).append("));\n");
            }
            indent(b, ind);
            b.append("}\n");
            return false;
        }
        if (s instanceof AnnAssign) {
            // In a function the annotation is never evaluated.
            AnnAssign a = (AnnAssign) s;
            if (a.value != null) {
                store(a.target, expr(a.value), b, ind);
            }
            return false;
        }
        if (s instanceof Global) {
            return false;
        }
        if (s instanceof Import) {
            for (Alias a : ((Import) s).names) {
                String load = "Py.importModule(" + Emitter.javaString(a.name) + ")";
                int dot = a.name.indexOf('.');
                if (a.asname != null || dot < 0) {
                    store(new Name(a.asname != null ? a.asname : a.name), new E(load, Ty.OBJECT), b, ind);
                } else {
                    indent(b, ind);
                    b.append(load).append(";\n");
                    String top = a.name.substring(0, dot);
                    store(new Name(top), new E("Py.importModule(" + Emitter.javaString(top) + ")", Ty.OBJECT), b, ind);
                }
            }
            return false;
        }
        if (s instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) s;
            String mod = em.resolveRelative(f);
            if ("__future__".equals(mod)) {
                return false;
            }
            String m = fresh();
            indent(b, ind);
            b.append("final PyModule ").append(m).append(" = Py.importModule(").append(Emitter.javaString(mod)).append(");\n");
            for (Alias a : f.names) {
                if ("*".equals(a.name)) {
                    throw new Unsupported("import *");
                }
                store(new Name(a.asname != null ? a.asname : a.name),
                        new E("Py.importFrom(" + m + ", " + Emitter.javaString(a.name) + ")", Ty.OBJECT), b, ind);
            }
            return false;
        }
        if (s instanceof Delete) {
            for (Expr t : ((Delete) s).targets) {
                delete(t, b, ind);
            }
            return false;
        }
        if (s instanceof With) {
            return withStmt((With) s, b, ind);
        }
        throw new Unsupported(s.getClass().getSimpleName());
    }

    /// `del t`: an attribute or an item, through the generic object protocol;
    /// a local that holds an object, cleared.
    private void delete(Expr t, StringBuilder b, int ind) {
        if (t instanceof Name && trueCell(((Name) t).id)) {
            String n = ((Name) t).id;
            indent(b, ind);
            b.append("Typed.drop(Typed.local(").append(cellRef(n)).append(", ").append(Emitter.javaString(n))
                    .append("));\n");
            indent(b, ind);
            b.append(cellRef(n)).append(" = null;\n");
            return;
        }
        if (t instanceof TupleExpr || t instanceof ListExpr) {
            for (Expr e : t instanceof TupleExpr ? ((TupleExpr) t).elts : ((ListExpr) t).elts) {
                delete(e, b, ind);
            }
            return;
        }
        if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            indent(b, ind);
            b.append("Py.obj(").append(box(expr(a.value))).append(").delAttr(").append(Emitter.javaString(a.attr))
                    .append(");\n");
            forgetPaths();
            return;
        }
        if (t instanceof Subscript) {
            Subscript x = (Subscript) t;
            String o = box(expr(x.value));
            indent(b, ind);
            b.append("Py.obj(").append(o).append(").delItem(").append(box(expr(x.index))).append(");\n");
            forgetPaths();
            return;
        }
        if (t instanceof Name && binding(((Name) t).id) == Scope.Binding.LOCAL) {
            String n = ((Name) t).id;
            Ty lt = localType(n);
            if (lt == null || !"Object".equals(jt(lt)) || longLocals.contains(n) || accLocals.contains(n)
                    || scalars.containsKey(n)) {
                throw new Unsupported("del of " + n);
            }
            if (lt.k == Ty.K.INT) {
                if (!unboundInts.contains(n) || shadow.containsKey(n)) {
                    throw new Unsupported("del of " + n);
                }
                // Read first (deleting an unbound local raises), then the
                // object half says unbound again.
                String v = box(expr((Name) t));
                indent(b, ind);
                b.append("Typed.drop(").append(v).append(");\n");
                indent(b, ind);
                b.append(big(n)).append(" = Typed.UNBOUND;\n");
                forgetPathsOf(n);
                return;
            }
            // Read first: deleting an unbound local raises.
            String v = box(expr((Name) t));
            indent(b, ind);
            b.append("Typed.drop(").append(v).append(");\n");
            indent(b, ind);
            b.append(var(n)).append(" = null;\n");
            if (guessLocal(n)) {
                indent(b, ind);
                b.append(over(n)).append(" = null;\n");
            }
            nonNull.remove(n);
            forgetPathsOf(n);
            return;
        }
        throw new Unsupported("del of " + t.getClass().getSimpleName());
    }

    /// `with`: as the generic version runs it (Emitter.withItems), the body
    /// typed. Exit may swallow what the body raised, so nothing the body
    /// learned holds after it.
    private boolean withStmt(With w, StringBuilder b, int ind) {
        if (w.isAsync) {
            throw new Unsupported("async with");
        }
        Set<String> touched = new HashSet<String>(assigned(w.body));
        for (WithItem it : w.items) {
            if (it.vars != null) {
                Infer.children(it.vars, x -> {
                    if (x instanceof Name) {
                        touched.add(((Name) x).id);
                    }
                });
                if (it.vars instanceof Name) {
                    touched.add(((Name) it.vars).id);
                }
            }
        }
        nonNull.removeAll(touched);
        shadow.keySet().removeAll(touched);
        forgetPaths();
        Set<String> entry = new HashSet<String>(nonNull);
        String[] outer = localRaise;
        // A raise in the body is thrown, for the exit to see.
        localRaise = null;
        try {
            withItems(w, 0, b, ind);
        } finally {
            localRaise = outer;
        }
        nonNull = entry;
        shadow.keySet().removeAll(touched);
        forgetPaths();
        return false;
    }

    private void withItems(With w, int i, StringBuilder b, int ind) {
        if (i == w.items.size()) {
            block(w.body, b, ind);
            return;
        }
        WithItem item = w.items.get(i);
        String mgr = fresh();
        String exit = fresh();
        String ok = fresh();
        String th = fresh();
        String pe = fresh();
        String tk = fresh();
        String ctx = box(expr(item.context));
        indent(b, ind);
        b.append("final Object ").append(mgr).append(" = ").append(ctx).append(";\n");
        indent(b, ind);
        b.append("final Object ").append(exit).append(" = ").append(em.attrSite("__exit__")).append(".get(").append(mgr)
                .append(");\n");
        String entered = em.attrSite("__enter__") + ".call0(" + mgr + ")";
        if (item.vars != null) {
            String ev = fresh();
            indent(b, ind);
            b.append("final Object ").append(ev).append(" = ").append(entered).append(";\n");
            store(item.vars, new E(ev, Ty.OBJECT), b, ind);
        } else {
            indent(b, ind);
            b.append(entered).append(";\n");
        }
        indent(b, ind);
        b.append("boolean ").append(ok).append(" = true;\n");
        indent(b, ind);
        b.append("final int ").append(tk).append(" = Typed.taken();\n");
        indent(b, ind);
        b.append("try {\n");
        withItems(w, i + 1, b, ind + 1);
        indent(b, ind);
        b.append("} catch (Throwable ").append(th).append(") {\n");
        indent(b, ind + 1);
        b.append("Typed.untake(").append(tk).append(");\n");
        indent(b, ind + 1);
        b.append(ok).append(" = false;\n");
        indent(b, ind + 1);
        b.append("final PyException ").append(pe).append(" = Py.convert(").append(th).append(");\n");
        indent(b, ind + 1);
        b.append("if (!Py.isTrue(Py.call(").append(exit).append(", Py.typeOf(").append(pe).append(".value), ")
                .append(pe).append(".value, Py.NONE))) {\n");
        indent(b, ind + 2);
        b.append("throw ").append(pe).append(";\n");
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("} finally {\n");
        indent(b, ind + 1);
        b.append("if (").append(ok).append(") {\n");
        indent(b, ind + 2);
        b.append("Py.call(").append(exit).append(", Py.NONE, Py.NONE, Py.NONE);\n");
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
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
            clearOver(tn, b, ind + 2);
            if (vt.k == Ty.K.STR || vt.wide()) {
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

    /// `for w in s.split()` / `for w in s.split(sep)` with a str target: the
    /// pieces found one at a time in `s`, held in a Java local, so the list
    /// and its array are never made. The string is immutable, so finding
    /// each piece as the loop reaches it sees what the list would hold.
    private boolean splitLoop(For f, StringBuilder b, int ind) {
        if (!(f.iter instanceof Call) || !(((Call) f.iter).func instanceof Attribute)) {
            return false;
        }
        Call c = (Call) f.iter;
        Attribute m = (Attribute) c.func;
        String tn = ((Name) f.target).id;
        Ty vt = localType(tn);
        if (!"split".equals(m.attr) || !c.keywords.isEmpty() || c.args.size() > 1
                // A separator that is a name or a constant is read with no
                // statement of its own, so the receiver is still read first.
                || c.args.size() == 1 && !(c.args.get(0) instanceof Name || c.args.get(0) instanceof Constant)
                || longLocals.contains(tn)
                || vt.k != Ty.K.STR && !vt.wide()) {
            return false;
        }
        E r = expr(m.value);
        if (r.t.k != Ty.K.STR || r.t.opt) {
            return false;
        }
        E sepE = c.args.isEmpty() ? null : expr(c.args.get(0));
        if (sepE != null && (sepE.t.k != Ty.K.STR || sepE.t.opt)) {
            return false;
        }
        String s = fresh();
        String p = fresh();
        String q = fresh();
        indent(b, ind);
        b.append("{\n");
        indent(b, ind + 1);
        b.append("String ").append(s).append(" = ").append(r.c).append(";\n");
        if (sepE == null) {
            indent(b, ind + 1);
            b.append("int ").append(p).append(" = 0;\n");
            indent(b, ind + 1);
            b.append("while ((").append(p).append(" = Typed.wordStart(").append(s).append(", ").append(p).append(")) < ")
                    .append(s).append(".length()) {\n");
            indent(b, ind + 2);
            b.append("int ").append(q).append(" = Typed.wordEnd(").append(s).append(", ").append(p).append(");\n");
            indent(b, ind + 2);
            b.append(var(tn)).append(" = ").append(s).append(".substring(").append(p).append(", ").append(q)
                    .append(");\n");
            clearOver(tn, b, ind + 2);
            indent(b, ind + 2);
            b.append(p).append(" = ").append(q).append(";\n");
        } else {
            String sep = fresh();
            indent(b, ind + 1);
            b.append("String ").append(sep).append(" = Typed.splitSep(").append(sepE.c).append(");\n");
            indent(b, ind + 1);
            b.append("int ").append(p).append(" = 0;\n");
            indent(b, ind + 1);
            b.append("while (").append(p).append(" >= 0) {\n");
            indent(b, ind + 2);
            b.append("int ").append(q).append(" = ").append(s).append(".indexOf(").append(sep).append(", ").append(p)
                    .append(");\n");
            indent(b, ind + 2);
            b.append(var(tn)).append(" = ").append(q).append(" < 0 ? ").append(s).append(".substring(").append(p)
                    .append(") : ").append(s).append(".substring(").append(p).append(", ").append(q).append(");\n");
            clearOver(tn, b, ind + 2);
            indent(b, ind + 2);
            b.append(p).append(" = ").append(q).append(" < 0 ? -1 : ").append(q).append(" + ").append(sep)
                    .append(".length();\n");
        }
        Set<String> inBody = assigned(f.body);
        if (!inBody.contains(tn)) {
            nonNull.add(tn);
        }
        block(f.body, b, ind + 2);
        // The target keeps whatever it held when the loop runs no times.
        nonNull.remove(tn);
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
        return true;
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

    /// The `else` of a loop just emitted inside the block `label` opened:
    /// it runs when the loop ends without a break, which leaves the block.
    /// Whether the block cannot complete normally, as javac judges it.
    private boolean loopElse(List<Stmt> orelse, List<Stmt> body, Set<String> inv, StringBuilder b, int ind) {
        boolean abrupt = block(orelse, b, ind + 1);
        indent(b, ind);
        b.append("}\n");
        // After a break the else did not run: only what held at the top of
        // every iteration is known.
        nonNull.retainAll(inv);
        shadow.keySet().removeAll(assigned(orelse));
        forgetPaths();
        return abrupt && !Infer.hasBreak(body);
    }

    private void forLoop(For f, StringBuilder b, int ind) {
        if (indexedLoop(f, b, ind)) {
            return;
        }
        if (!(f.target instanceof Name) || binding(((Name) f.target).id) != Scope.Binding.LOCAL) {
            anyTargetLoop(f, b, ind);
            return;
        }
        Set<String> inBody = assigned(f.body);
        nonNull.removeAll(inBody);
        shadow.keySet().removeAll(inBody);
        forgetPaths();
        if (f.target instanceof Name) {
            nonNull.remove(((Name) f.target).id);
        }
        if (!(f.target instanceof Name) || binding(((Name) f.target).id) != Scope.Binding.LOCAL) {
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
                        if (longLocals.contains(((Name) f.target).id)) {
                            throw new Unsupported("range step");
                        }
                        // A step known only at run time: the range object.
                        anyTargetLoop(f, b, ind);
                        return;
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
        if (splitLoop(f, b, ind)) {
            return;
        }
        if (displayItems(f.iter) != null) {
            if (longLocals.contains(((Name) f.target).id)) {
                throw new Unsupported("for over a display into a long");
            }
            if (vt.k == Ty.K.INT) {
                anyTargetLoop(f, b, ind);
                return;
            }
            indent(b, ind);
            b.append("{\n");
            String x = displayLoopHead(f.iter, b, ind);
            indent(b, ind + 2);
            if (guessLocal(((Name) f.target).id)) {
                storeGuess(v, over(((Name) f.target).id), vt, new E(x, Ty.OBJECT), b, ind + 2);
            } else {
                b.append(v).append(" = ").append(conv(new E(x, Ty.OBJECT), vt)).append(";\n");
            }
            block(f.body, b, ind + 2);
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
            // Any other iterable: the generic iterator, with the body still
            // typed. An int target is held as a long and an object, which a
            // value of unknown type cannot fill without a test, so it stays
            // with the generic version.
            if (longLocals.contains(((Name) f.target).id)) {
                throw new Unsupported("for over " + it.t + " into a long");
            }
            if (vt.k == Ty.K.INT) {
                // The general loop stores through storePair, which tests.
                anyTargetLoop(f, b, ind);
                return;
            }
            String itr = fresh();
            String x = fresh();
            boolean guess = guessLocal(((Name) f.target).id);
            String store = guess ? null : conv(new E(x, Ty.OBJECT), vt);
            indent(b, ind);
            b.append("{\n");
            indent(b, ind + 1);
            b.append("final PyObject ").append(itr).append(" = Py.iter(").append(box(it)).append(");\n");
            indent(b, ind + 1);
            b.append("try {\n");
            indent(b, ind + 1);
            b.append(loopHead(x, loopNext(itr), null));
            indent(b, ind + 2);
            if (guess) {
                // A guessed target: the item into the slot when it fits.
                storeGuess(v, over(((Name) f.target).id), vt, new E(x, Ty.OBJECT), b, ind + 2);
            } else {
                b.append(v).append(" = ").append(store).append(";\n");
            }
            block(f.body, b, ind + 2);
            indent(b, ind + 1);
            b.append("}\n");
            indent(b, ind + 1);
            b.append(loopHandler(itr));
            indent(b, ind);
            b.append("}\n");
            return;
        }
        Ty el = in.elem(it.t);
        String l = fresh();
        String k = fresh();
        indent(b, ind);
        b.append("{\n");
        indent(b, ind + 1);
        b.append("PyList ").append(l).append(" = Typed.iterable(").append(listOf(it)).append(");\n");
        indent(b, ind + 1);
        b.append("for (int ").append(k).append(" = 0; ").append(k).append(" < ").append(l).append(".size(); ")
                .append(k).append("++) {\n");
        indent(b, ind + 2);
        E item = el.k != Ty.K.INT && in.guessedSite(it.t) ? guessedElem(l + ".get(" + k + ")", el, b, ind + 2)
                : el.k == Ty.K.FLOAT ? new E("Typed.atF(" + l + ", " + k + ")", Ty.FLOAT)
                : new E(unbox(l + ".get(" + k + ")", el), el);
        if (vt.k == Ty.K.INT && !longLocals.contains(((Name) f.target).id)) {
            storePair(v, big(((Name) f.target).id), item, b, ind + 2);
        } else if (vt.k == Ty.K.INT) {
            b.append(v).append(" = Typed.toLong(").append(box(item)).append(");\n");
        } else if (guessLocal(((Name) f.target).id)) {
            storeGuess(v, over(((Name) f.target).id), vt, item, b, ind + 2);
        } else {
            b.append(v).append(" = ").append(conv(item, vt)).append(";\n");
        }
        block(f.body, b, ind + 2);
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
    }

    /// `for i, x in enumerate(l)` and `for a, b in zip(l, m)` over typed
    /// lists, into plain locals: walked by index as the lists' own
    /// iterators walk them (each length read again at every step), with no
    /// iterator and no tuple per item. False, writing nothing, for any
    /// other loop.
    private boolean indexedLoop(For f, StringBuilder b, int ind) {
        if (!(f.iter instanceof Call) || !(f.target instanceof TupleExpr)) {
            return false;
        }
        Call c = (Call) f.iter;
        if (!(c.func instanceof Name) || binding(((Name) c.func).id) != Scope.Binding.BUILTIN
                || em.moduleScopeHasStarImport() || !c.keywords.isEmpty() || Infer.hasStarred(c.args)) {
            return false;
        }
        String n = ((Name) c.func).id;
        boolean en = "enumerate".equals(n);
        if (!en && !"zip".equals(n) || c.args.isEmpty() || en && c.args.size() > 2) {
            return false;
        }
        List<Expr> targets = ((TupleExpr) f.target).elts;
        int lists = en ? 1 : c.args.size();
        if (targets.size() != (en ? 2 : lists)) {
            return false;
        }
        for (Expr t : targets) {
            if (!(t instanceof Name) || binding(((Name) t).id) != Scope.Binding.LOCAL) {
                return false;
            }
        }
        for (int i = 0; i < lists; i++) {
            Ty lt = in.type(c.args.get(i), cur, sc);
            if (lt.k != Ty.K.LIST || lt.opt || in.guessed(c.args.get(i))) {
                return false;
            }
        }
        if (en && c.args.size() == 2) {
            Ty st = in.type(c.args.get(1), cur, sc);
            if (st.k != Ty.K.INT || st.opt) {
                return false;
            }
        }
        Set<String> inBody = assigned(f.body);
        nonNull.removeAll(inBody);
        shadow.keySet().removeAll(inBody);
        forgetPaths();
        for (Expr t : targets) {
            nonNull.remove(((Name) t).id);
        }
        indent(b, ind);
        b.append("{\n");
        // The arguments in order, once, as the call evaluates them.
        String[] ls = new String[lists];
        Ty[] els = new Ty[lists];
        for (int i = 0; i < lists; i++) {
            E le = expr(c.args.get(i));
            ls[i] = fresh();
            els[i] = in.elem(le.t);
            indent(b, ind + 1);
            // A field read may answer through its object fallback: as a
            // list, checked.
            b.append("PyList ").append(ls[i]).append(" = Typed.iterable(").append(listOf(le))
                    .append(");\n");
        }
        String start = null;
        if (en) {
            start = fresh();
            indent(b, ind + 1);
            b.append("long ").append(start).append(" = ")
                    .append(c.args.size() == 2 ? bound(expr(c.args.get(1))) : "0L").append(";\n");
        }
        String k = fresh();
        StringBuilder cond = new StringBuilder();
        for (String l : ls) {
            cond.append(cond.length() == 0 ? "" : " && ").append(k).append(" < ").append(l).append(".size()");
        }
        indent(b, ind + 1);
        b.append("for (int ").append(k).append(" = 0; ").append(cond).append("; ").append(k).append("++) {\n");
        int ti = 0;
        if (en) {
            String tn = ((Name) targets.get(0)).id;
            String v = var(tn);
            Ty vt = localType(tn);
            String ix = "(" + start + " + " + k + ")";
            if (vt.k == Ty.K.INT) {
                indent(b, ind + 2);
                b.append(v).append(" = ").append(ix).append(";\n");
                if (!longLocals.contains(tn)) {
                    indent(b, ind + 2);
                    b.append(big(tn)).append(" = null;\n");
                }
            } else {
                store(targets.get(0), new E("Long.valueOf(" + ix + ")", Ty.INT), b, ind + 2);
            }
            ti = 1;
        }
        for (int i = 0; i < lists; i++) {
            Ty el = els[i];
            String l = ls[i];
            Ty at = in.type(c.args.get(i), cur, sc);
            E item = el.k != Ty.K.INT && in.guessedSite(at) ? guessedElem(l + ".get(" + k + ")", el, b, ind + 2)
                    : el.k == Ty.K.FLOAT ? new E("Typed.atF(" + l + ", " + k + ")", Ty.FLOAT)
                    : new E(unbox(l + ".get(" + k + ")", el), el);
            store(targets.get(ti + i), item, b, ind + 2);
        }
        block(f.body, b, ind + 2);
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
        return true;
    }

    /// The items of a tuple or list display with no starred item, or null.
    private static List<Expr> displayItems(Expr iter) {
        List<Expr> elts = iter instanceof TupleExpr ? ((TupleExpr) iter).elts
                : iter instanceof ListExpr ? ((ListExpr) iter).elts : null;
        if (elts == null || elts.isEmpty()) {
            return null;
        }
        for (Expr x : elts) {
            if (x instanceof Starred) {
                return null;
            }
        }
        return elts;
    }

    /// Opens a loop over a display ([#displayItems]) -- bs4's `for attrdict
    /// in attrs, kwargs:` -- as an array of the items, evaluated once and in
    /// order as the tuple would be, walked by index: no tuple, iterator or
    /// loop handler. Answers the name holding the item.
    private String displayLoopHead(Expr iter, StringBuilder b, int ind) {
        StringBuilder items = new StringBuilder();
        for (Expr x : displayItems(iter)) {
            if (items.length() > 0) {
                items.append(", ");
            }
            items.append(box(expr(x)));
        }
        String arr = fresh();
        String k = fresh();
        String x = fresh();
        indent(b, ind + 1);
        b.append("final Object[] ").append(arr).append(" = {").append(items).append("};\n");
        indent(b, ind + 1);
        b.append("for (int ").append(k).append(" = 0; ").append(k).append(" < ").append(arr).append(".length; ")
                .append(k).append("++) {\n");
        indent(b, ind + 2);
        b.append("final Object ").append(x).append(" = ").append(arr).append('[').append(k).append("];\n");
        return x;
    }

    /// A loop whose target is not one local: a tuple to unpack, a global, an
    /// attribute or an element. Each item is stored as an assignment would.
    private void anyTargetLoop(For f, StringBuilder b, int ind) {
        Set<String> inBody = new HashSet<String>(assigned(f.body));
        inBody.addAll(assigned(java.util.Collections.<Stmt>singletonList(new Assign(
                java.util.Collections.singletonList(f.target), f.target))));
        nonNull.removeAll(inBody);
        shadow.keySet().removeAll(inBody);
        forgetPaths();
        if (displayItems(f.iter) != null) {
            indent(b, ind);
            b.append("{\n");
            String x = displayLoopHead(f.iter, b, ind);
            store(f.target, new E(x, Ty.OBJECT), b, ind + 2);
            block(f.body, b, ind + 2);
            indent(b, ind + 1);
            b.append("}\n");
            indent(b, ind);
            b.append("}\n");
            return;
        }
        E it = loopSource(f.iter, f.target);
        String handled = null;
        indent(b, ind);
        b.append("{\n");
        if (it.t.k == Ty.K.LIST && !it.t.opt) {
            Ty el = in.elem(it.t);
            String l = fresh();
            String k = fresh();
            indent(b, ind + 1);
            b.append("PyList ").append(l).append(" = Typed.iterable(").append(listOf(it)).append(");\n");
            indent(b, ind + 1);
            b.append("for (int ").append(k).append(" = 0; ").append(k).append(" < ").append(l).append(".size(); ")
                    .append(k).append("++) {\n");
            E item = el.k != Ty.K.INT && in.guessedSite(it.t) ? guessedElem(l + ".get(" + k + ")", el, b, ind + 2)
                    : el.k == Ty.K.FLOAT ? new E("Typed.atF(" + l + ", " + k + ")", Ty.FLOAT)
                    : new E(unbox(l + ".get(" + k + ")", el), el);
            store(f.target, spillTo(item, b, ind + 2), b, ind + 2);
        } else if (Emitter.itemPairs(f.iter, f.target) != null) {
            // An ItemWalk is never a generator, and the iterator it may wrap
            // runs its own handler: no loop handler, its next() called
            // directly.
            String itr = fresh();
            String x = fresh();
            indent(b, ind + 1);
            b.append("final PyObject ").append(itr).append(" = ").append(it.c).append(";\n");
            indent(b, ind + 1);
            b.append(loopHead(x, itr + ".next()", null));
            store(f.target, new E(x, Ty.OBJECT), b, ind + 2);
        } else {
            String itr = fresh();
            String x = fresh();
            indent(b, ind + 1);
            b.append("final PyObject ").append(itr).append(" = Py.iter(").append(box(it)).append(");\n");
            indent(b, ind + 1);
            b.append("try {\n");
            indent(b, ind + 1);
            b.append(loopHead(x, loopNext(itr), null));
            store(f.target, new E(x, Ty.OBJECT), b, ind + 2);
            handled = itr;
        }
        block(f.body, b, ind + 2);
        indent(b, ind + 1);
        b.append("}\n");
        if (handled != null) {
            indent(b, ind + 1);
            b.append(loopHandler(handled));
        }
        indent(b, ind);
        b.append("}\n");
    }

    /// The next value of loop iterator `itr`, without a handler per value when
    /// it is a plain generator: the loop holds the one handler instead
    /// ([#loopHandler]; see PyGenerator.loopNext).
    static String loopNext(String itr) {
        return "PyGenerator.loopNext(" + itr + ")";
    }

    /// `for (Object x = next; x != null; [step, ]x = next) {` -- the first
    /// fetch is the init, not the test, so the back edge lands on the test
    /// INSIDE the try. A loop whose test is the try's first instruction jumps
    /// back to the try's own start, and the translator re-enters the try on
    /// every iteration.
    static String loopHead(String x, String next, String step) {
        return "for (Object " + x + " = " + next + "; " + x + " != null; "
                + (step != null ? step + ", " : "") + x + " = " + next + ") {\n";
    }

    /// Closes the try a [#loopNext] loop is wrapped in.
    static String loopHandler(String itr) {
        return "} catch (Throwable " + itr + "$e) {\n throw PyGenerator.loopEscape(" + itr + ", " + itr + "$e);\n}\n";
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

    /// The statement being lowered, which a comprehension is hoisted ahead of.
    private Stmt curStmt;

    /// A list, set or dict comprehension, as a loop run ahead of the
    /// statement (`pre`) into a fresh container. Only where nothing the
    /// statement evaluates before it could observe the difference
    /// (`inertBefore`), and where its targets name no local of the function.
    private E comp(Comp c) {
        if (pre == null || condDepth != 0 || curStmt == null || c.scope == null || !inertBefore(curStmt, c)) {
            throw new Unsupported("comprehension");
        }
        Map<String, String> vars = new HashMap<String, String>();
        for (Comprehension g : c.generators) {
            if (g.isAsync || !compTargets(g.target, vars)) {
                throw new Unsupported("comprehension target");
            }
        }
        boolean[] ok = {true};
        Infer.children(c, x -> ok[0] &= compBody(x));
        if (!ok[0]) {
            throw new Unsupported("comprehension body");
        }
        StringBuilder p = pre;
        int ind = preInd;
        Scope outer = sc;
        String kind = c.kind == Comp.Kind.SET ? "PySet" : c.kind == Comp.Kind.DICT ? "PyDict" : "PyList";
        String r = exprTemp(kind);
        for (Map.Entry<String, String> v : vars.entrySet()) {
            Ty vt = compTy(c.scope, v.getKey());
            v.setValue(exprTemp(vt.wide() ? "Object" : jt(vt)));
            if (vt.k == Ty.K.INST && in.guessedCompVar(c.scope, v.getKey())) {
                compOver.put(v.getValue(), exprTemp("Object"));
            } else {
                // Temporaries are numbered per function: an entry of an
                // earlier one's comprehension names another variable here.
                compOver.remove(v.getValue());
            }
        }
        forgetPaths();
        compVars.put(c.scope, vars);
        try {
            String[] first = compHead(c.generators.get(0));
            pre = null;
            sc = c.scope;
            indent(p, ind);
            p.append(r).append(" = new ").append(kind).append("();\n");
            int d = ind;
            for (int i = 0; i < c.generators.size(); i++) {
                Comprehension g = c.generators.get(i);
                String[] h = i == 0 ? first : compHead(g);
                indent(p, d);
                p.append("{\n");
                indent(p, d + 1);
                p.append(h[0]);
                indent(p, d + 1);
                p.append(h[1]);
                d += 2;
                compStore(g, h[2], p, d);
                for (Expr f : g.ifs) {
                    indent(p, d);
                    p.append("if (!(").append(testExpr(f)).append(")) {\n");
                    indent(p, d + 1);
                    p.append("continue;\n");
                    indent(p, d);
                    p.append("}\n");
                }
            }
            indent(p, d);
            if (c.kind == Comp.Kind.DICT) {
                String k = box(expr(c.elt));
                p.append(r).append(".setItem(").append(k).append(", ").append(box(expr(c.value))).append(");\n");
            } else {
                p.append(r).append(c.kind == Comp.Kind.SET ? ".addItem(" : ".append(").append(box(expr(c.elt)))
                        .append(");\n");
            }
            for (int i = c.generators.size() - 1; i >= 0; i--) {
                d -= 2;
                indent(p, d + 1);
                p.append("}\n");
                indent(p, d);
                p.append("}\n");
            }
        } finally {
            sc = outer;
            pre = p;
            compVars.remove(c.scope);
            nonNull.removeAll(vars.keySet());
            shadow.keySet().removeAll(vars.keySet());
        }
        forgetPaths();
        return new E(r, c.kind == Comp.Kind.LIST ? in.type(c, cur, sc) : Ty.OBJECT);
    }

    /// `any(e for x in xs if c)` and `all(...)`: the loop run ahead of the
    /// statement, as [#comp] runs one, leaving at the first item that
    /// decides -- bs4's SoupStrainer made and stepped a generator object for
    /// `any(x.exclude_everything for x in self.string_rules)` on every
    /// call. When the name no longer holds the builtin, the generic call
    /// runs instead. Null where the loop cannot be lowered.
    private E anyAll(Call call, String fn) {
        Comp c = (Comp) call.args.get(0);
        if (pre == null || condDepth != 0 || curStmt == null || c.scope == null || !inertBefore(curStmt, c)) {
            return null;
        }
        Map<String, String> vars = new HashMap<String, String>();
        for (Comprehension g : c.generators) {
            if (g.isAsync || !compTargets(g.target, vars)) {
                return null;
            }
        }
        boolean[] ok = {true};
        Infer.children(c, x -> ok[0] &= compBody(x));
        if (!ok[0]) {
            return null;
        }
        String gen = genericFallback(call);
        if (gen == null) {
            return null;
        }
        boolean any = "any".equals(fn);
        StringBuilder p = pre;
        int ind = preInd;
        Scope outer = sc;
        String r = exprTemp("boolean");
        String label = fresh();
        Map<String, String> saved = new HashMap<String, String>(vars);
        for (Map.Entry<String, String> v : vars.entrySet()) {
            Ty vt = compTy(c.scope, v.getKey());
            v.setValue(exprTemp(vt.wide() ? "Object" : jt(vt)));
            if (vt.k == Ty.K.INST && in.guessedCompVar(c.scope, v.getKey())) {
                compOver.put(v.getValue(), exprTemp("Object"));
            } else {
                // Temporaries are numbered per function: an entry of an
                // earlier one's comprehension names another variable here.
                compOver.remove(v.getValue());
            }
        }
        forgetPaths();
        compVars.put(c.scope, vars);
        try {
            indent(p, ind);
            p.append("if (").append(em.builtinRef(fn)).append(" == Builtins.").append(any ? "ANY" : "ALL").append(") {\n");
            indent(p, ind + 1);
            p.append(r).append(" = ").append(!any).append(";\n");
            indent(p, ind + 1);
            p.append(label).append(": {\n");
            String[] first = compHead(c.generators.get(0));
            pre = null;
            sc = c.scope;
            int d = ind + 2;
            for (int i = 0; i < c.generators.size(); i++) {
                Comprehension g = c.generators.get(i);
                String[] h = i == 0 ? first : compHead(g);
                indent(p, d);
                p.append("{\n");
                indent(p, d + 1);
                p.append(h[0]);
                indent(p, d + 1);
                p.append(h[1]);
                d += 2;
                compStore(g, h[2], p, d);
                for (Expr f : g.ifs) {
                    indent(p, d);
                    p.append("if (!(").append(testExpr(f)).append(")) {\n");
                    indent(p, d + 1);
                    p.append("continue;\n");
                    indent(p, d);
                    p.append("}\n");
                }
            }
            indent(p, d);
            p.append("if (").append(any ? "" : "!").append('(').append(testExpr(c.elt)).append(")) {\n");
            indent(p, d + 1);
            p.append(r).append(" = ").append(any).append(";\n");
            indent(p, d + 1);
            p.append("break ").append(label).append(";\n");
            indent(p, d);
            p.append("}\n");
            for (int i = c.generators.size() - 1; i >= 0; i--) {
                d -= 2;
                indent(p, d + 1);
                p.append("}\n");
                indent(p, d);
                p.append("}\n");
            }
            indent(p, ind + 1);
            p.append("}\n");
            indent(p, ind);
            p.append("} else {\n");
            indent(p, ind + 1);
            p.append(r).append(" = Py.isTrue(").append(gen).append(");\n");
            indent(p, ind);
            p.append("}\n");
        } finally {
            sc = outer;
            pre = p;
            compVars.remove(c.scope);
            nonNull.removeAll(saved.keySet());
            shadow.keySet().removeAll(saved.keySet());
        }
        forgetPaths();
        return new E(r, Ty.BOOL);
    }

    /// What a loop over `iter` into `target` iterates: `x.items()` into two
    /// names walks the pairs (see Emitter.itemPairs).
    private E loopSource(Expr iter, Expr target) {
        Expr recv = Emitter.itemPairs(iter, target);
        return recv != null ? new E("ItemWalk.of(" + box(expr(recv)) + ")", Ty.OBJECT) : expr(iter);
    }

    /// The loop over one `for` clause of a comprehension: the statement
    /// evaluating what it iterates, the loop header, and the item (boxed).
    /// A `range` counts in a long and a list is indexed, as `for` does.
    private String[] compHead(Comprehension g) {
        if (in.rangeCall(g.iter, sc) && ((Call) g.iter).args.size() <= 2) {
            List<Expr> a = ((Call) g.iter).args;
            String start = a.size() == 1 ? "0L" : bound(expr(a.get(0)));
            String stop = bound(expr(a.get(a.size() - 1)));
            String s0 = fresh();
            String e = fresh();
            String i = fresh();
            return new String[] {"long " + s0 + " = " + start + ", " + e + " = " + stop + ";\n",
                "for (long " + i + " = " + s0 + "; " + i + " < " + e + "; " + i + "++) {\n",
                "Long.valueOf(" + i + ")"};
        }
        E it = loopSource(g.iter, g.target);
        if (it.t.k == Ty.K.LIST && !it.t.opt) {
            String l = fresh();
            String k = fresh();
            return new String[] {"PyList " + l + " = Typed.iterable(" + listOf(it) + ");\n",
                "for (int " + k + " = 0; " + k + " < " + l + ".size(); " + k + "++) {\n",
                l + ".get(" + k + ")"};
        }
        String itr = fresh();
        String x = fresh();
        return new String[] {"final PyObject " + itr + " = Py.iter(" + box(it) + ");\n",
            "for (Object " + x + "; (" + x + " = " + itr + ".next()) != null;) {\n", x};
    }

    /// Records the names a comprehension target binds; false when it binds
    /// anything but names, or a name the function has as a local.
    private boolean compTargets(Expr t, Map<String, String> vars) {
        if (t instanceof Name) {
            String n = ((Name) t).id;
            if (compVar(n) != null) {
                return false;
            }
            vars.put(n, null);
            return true;
        }
        if (t instanceof TupleExpr || t instanceof ListExpr) {
            for (Expr e : t instanceof TupleExpr ? ((TupleExpr) t).elts : ((ListExpr) t).elts) {
                if (!compTargets(e, vars)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /// Whether a comprehension's body can be lowered in line: nothing in it
    /// opens a scope that would capture its targets, or binds outside it.
    private static boolean compBody(Object o) {
        if (o instanceof NamedExpr || o instanceof Lambda || o instanceof Yield || o instanceof Await
                || o instanceof Comp && ((Comp) o).kind == Comp.Kind.GENERATOR) {
            return false;
        }
        boolean[] ok = {true};
        Infer.children(o, x -> ok[0] &= compBody(x));
        return ok[0];
    }

    /// Whether everything `s` evaluates before `target` is inert: running
    /// the comprehension first cannot change what it reads. Only straight
    /// line evaluation is walked; a comprehension under a branch of `and`,
    /// `or` or `if` is never reached, so never hoisted.
    private boolean inertBefore(Object s, Object target) {
        return inertWalk(s, target) == 1;
    }

    /// 1 found the target with only inert nodes before it, 0 walked `o`
    /// without finding it and it was inert, -1 gave up.
    private int inertWalk(Object o, Object target) {
        if (o == target) { //NOPMD CompareObjectsWithEquals - identity is meant
            return 1;
        }
        List<Object> kids = new ArrayList<Object>();
        if (o instanceof ExprStmt) {
            kids.add(((ExprStmt) o).value);
        } else if (o instanceof Return) {
            if (((Return) o).value != null) {
                kids.add(((Return) o).value);
            }
        } else if (o instanceof Assign) {
            kids.add(((Assign) o).value);
        } else if (o instanceof AugAssign) {
            if (!(((AugAssign) o).target instanceof Name)) {
                return -1;
            }
            kids.add(((AugAssign) o).value);
        } else if (o instanceof If) {
            kids.add(((If) o).test);
        } else if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Attribute) {
                kids.add(((Attribute) c.func).value);
            } else {
                kids.add(c.func);
            }
            kids.addAll(c.args);
            for (Keyword k : c.keywords) {
                kids.add(k.value);
            }
        } else if (o instanceof TupleExpr) {
            kids.addAll(((TupleExpr) o).elts);
        } else if (o instanceof ListExpr) {
            kids.addAll(((ListExpr) o).elts);
        } else if (o instanceof BinOp) {
            kids.add(((BinOp) o).left);
            kids.add(((BinOp) o).right);
        } else if (o instanceof Name) {
            Scope.Binding b = binding(((Name) o).id);
            return b == Scope.Binding.LOCAL || b == Scope.Binding.CELL || b == Scope.Binding.BUILTIN ? 0 : -1;
        } else if (o instanceof Constant) {
            return 0;
        } else {
            return -1;
        }
        for (Object k : kids) {
            int r = inertWalk(k, target);
            if (r != 0) {
                return r;
            }
        }
        // A call or an operator finished before the target ran user code.
        return o instanceof Call || o instanceof BinOp ? -1 : 0;
    }

    private void store(Expr t, E v, StringBuilder b, int ind) {
        if (t instanceof Name && trueCell(((Name) t).id)) {
            indent(b, ind);
            b.append(cellRef(((Name) t).id)).append(" = ").append(box(v)).append(";\n");
            return;
        }
        if (t instanceof Name) {
            String n = ((Name) t).id;
            switch (binding(n)) {
                case LOCAL:
                    if (compVar(n) == null) {
                        paramAlias.remove(n);
                    }
                    indent(b, ind);
                    // A comprehension target is a variable of its own: what
                    // the function's local of that name is proved to hold
                    // says nothing of it.
                    boolean own = compVar(n) == null;
                    if (own && longLocals.contains(n)) {
                        // The analysis proved every value it holds fits.
                        b.append(var(n)).append(" = ").append(fitting(v))
                                .append(";\n");
                        return;
                    }
                    if (localType(n).k == Ty.K.INT || own && accLocals.contains(n)) {
                        storePair(var(n), big(n), v, b, ind);
                    } else if (guessLocal(n)) {
                        storeGuess(var(n), over(n), localType(n), v, b, ind);
                    } else {
                        b.append(var(n)).append(" = ").append(conv(v, localType(n))).append(";\n");
                    }
                    if (own && flatStr.contains(n)) {
                        indent(b, ind);
                        b.append("sp").append(var(n)).append(" = -1;\n");
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
            if (!fieldStore(r, a.attr)) {
                // Not a field this build keeps in a Java field: the generic
                // store, rather than giving up the whole typed function.
                if (!plain(v.c) && !plain(r.c)) {
                    // Python evaluates the value before the receiver.
                    String tv = fresh();
                    indent(b, ind);
                    b.append(jt(v.t)).append(' ').append(tv).append(" = ").append(v.c).append(";\n");
                    v = new E(tv, v.t);
                }
                indent(b, ind);
                b.append(em.guardedSet(a.attr, "(Object) " + box(r), box(v))).append('\n');
                forgetAttr(a.attr);
                return;
            }
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
            if (guardedField(owner(r, a.attr), a.attr)) {
                indent(b, ind);
                storeGuardedField(r, a.attr, v, b, ind);
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
                if (!plain(v.c)) {
                    // Python evaluates the value before the target.
                    v = spillTo(v, b, ind);
                }
                E l = expr(s.value);
                String p = sliceParts((Slice) s.index);
                indent(b, ind);
                b.append("Py.setslice(").append(box(l)).append(", ").append(p).append(", ").append(box(v)).append(");\n");
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
        if (t instanceof TupleExpr || t instanceof ListExpr) {
            // Unpacking anything but a literal of the same length: the
            // generic helpers, each element then stored as the target
            // wants it (Infer types them as dynamic, or as the list's
            // elements).
            List<Expr> elts = t instanceof TupleExpr ? ((TupleExpr) t).elts : ((ListExpr) t).elts;
            int star = -1;
            for (int i = 0; i < elts.size(); i++) {
                if (elts.get(i) instanceof Starred) {
                    if (star >= 0) {
                        throw new Unsupported("two starred targets");
                    }
                    star = i;
                }
            }
            String arr = fresh();
            if (star < 0) {
                // A fixed-size tuple is read in place: the runtime answers
                // an empty array for one and no array is made.
                String held = fresh();
                indent(b, ind);
                b.append("final Object ").append(held).append(" = ").append(box(v)).append(";\n");
                indent(b, ind);
                b.append("final Object[] ").append(arr).append(" = ")
                        .append(Emitter.onlyNames(t) ? "Py.unpackNamesT(" : "Py.unpackT(").append(held).append(", ")
                        .append(elts.size()).append(");\n");
                for (int i = 0; i < elts.size(); i++) {
                    store(elts.get(i), new E("(" + arr + ".length != 0 ? " + arr + "[" + i + "] : ((PyTuple) " + held
                            + ").get(" + i + "))", Ty.OBJECT), b, ind);
                }
                return;
            }
            indent(b, ind);
            b.append("final Object[] ").append(arr).append(" = ");
            {
                b.append("Py.unpackStar(").append(box(v)).append(", ").append(star).append(", ")
                        .append(elts.size() - star - 1).append(");\n");
            }
            for (int i = 0; i < elts.size(); i++) {
                Expr e = elts.get(i);
                store(e instanceof Starred ? ((Starred) e).value : e, new E(arr + "[" + i + "]", Ty.OBJECT), b, ind);
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

    /// The untyped locals of `f` worth holding as a long and an object: a
    /// `+=` or `-=` targets them, and nothing deletes them.
    private Set<String> accumulators(Infer.Fn f) {
        Set<String> targets = new HashSet<String>();
        Infer.Visit[] v = new Infer.Visit[1];
        v[0] = x -> {
            if (x instanceof AugAssign && ((AugAssign) x).target instanceof Name
                    && ("+".equals(((AugAssign) x).op) || "-".equals(((AugAssign) x).op))) {
                targets.add(((Name) ((AugAssign) x).target).id);
            }
            if (!(x instanceof FunctionDef || x instanceof Lambda || x instanceof ClassDef || x instanceof Comp)) {
                Infer.children(x, v[0]);
            }
        };
        for (Stmt st : f.def.body) {
            v[0].on(st);
        }
        Set<String> out = new HashSet<String>();
        for (String n : targets) {
            Ty t = f.locals.get(n);
            if (t != null && t.wide() && !sc.params.contains(n) && !longLocals.contains(n)
                    && !sc.deleted.contains(n) && sc.resolve(n) == Scope.Binding.LOCAL) {
                out.add(n);
            }
        }
        return out;
    }

    /// `n += v` (or `-=`) on an accumulator: the long sum while both are
    /// ints and it fits, Python's in-place operator on the boxed value
    /// otherwise.
    private void accumulate(String n, String op, E v, StringBuilder b, int ind) {
        String lv = var(n);
        String ob = big(n);
        String slow = "Py." + ("+".equals(op) ? "iadd" : "isub") + "(" + ob + " == null ? (Object) Long.valueOf(" + lv
                + ") : " + ob + ", ";
        String r = fresh();
        String s = fresh();
        indent(b, ind);
        String over = "+".equals(op) ? "((" + lv + " ^ " + s + ") & (" + r + " ^ " + s + ")) < 0"
                : "((" + lv + " ^ " + r + ") & (" + lv + " ^ " + s + ")) < 0";
        if (v.t.k == Ty.K.INT && !v.t.opt && v.lc != null && v.pk == null) {
            b.append("long ").append(r).append(" = ").append(v.lc).append(";\n");
            indent(b, ind);
            b.append("long ").append(s).append(" = ").append(lv).append(' ').append(op).append(' ').append(r).append(";\n");
            indent(b, ind);
            b.append("if (").append(ob).append(" == null && !(").append(over).append(")) {\n");
            indent(b, ind + 1);
            b.append(lv).append(" = ").append(s).append(";\n");
            indent(b, ind);
            b.append("} else {\n");
            indent(b, ind + 1);
            storePair(lv, ob, new E(slow + "Long.valueOf(" + r + "))", Ty.OBJECT), b, ind + 1);
            indent(b, ind);
            b.append("}\n");
            return;
        }
        String x = fresh();
        b.append("Object ").append(x).append(" = ").append(box(v)).append(";\n");
        indent(b, ind);
        b.append("if (").append(ob).append(" == null && ").append(x).append(" instanceof Long) {\n");
        indent(b, ind + 1);
        b.append("long ").append(r).append(" = ((Long) ").append(x).append(").longValue();\n");
        indent(b, ind + 1);
        b.append("long ").append(s).append(" = ").append(lv).append(' ').append(op).append(' ').append(r).append(";\n");
        indent(b, ind + 1);
        b.append("if (!(").append(over).append(")) {\n");
        indent(b, ind + 2);
        b.append(lv).append(" = ").append(s).append(";\n");
        indent(b, ind + 2);
        b.append(x).append(" = null;\n");
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
        indent(b, ind);
        b.append("if (").append(x).append(" != null) {\n");
        indent(b, ind + 1);
        storePair(lv, ob, new E(slow + x + ")", Ty.OBJECT), b, ind + 1);
        indent(b, ind);
        b.append("}\n");
    }

    private void augAssign(AugAssign a, StringBuilder b, int ind) {
        Expr t = a.target;
        if (t instanceof Name && accLocals.contains(((Name) t).id) && ("+".equals(a.op) || "-".equals(a.op))) {
            if (unboundInts.contains(((Name) t).id)) {
                // `x += v` reads x before it evaluates v.
                indent(b, ind);
                b.append("Typed.boundInt(").append(big(((Name) t).id)).append(", ")
                        .append(Emitter.javaString(((Name) t).id)).append(");\n");
            }
            accumulate(((Name) t).id, a.op, expr(a.value), b, ind);
            return;
        }
        if (t instanceof Name && longLocals.contains(((Name) t).id)) {
            store(t, top(new BinOp(t, a.op, a.value)), b, ind);
            return;
        }
        if (t instanceof Name && (localType(((Name) t).id).k == Ty.K.INT || localType(((Name) t).id).k == Ty.K.FLOAT)
                && binding(((Name) t).id) == Scope.Binding.LOCAL) {
            store(t, top(new BinOp(t, a.op, a.value)), b, ind);
            return;
        }
        if (t instanceof Attribute && ((Attribute) t).value instanceof Name
                && binding(((Name) ((Attribute) t).value).id) == Scope.Binding.LOCAL) {
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
            if (cur0.t.k == Ty.K.LIST && !cur0.t.opt && "+".equals(a.op)) {
                // list += extends in place, so an alias sees the new items;
                // Py.iadd answers the same PyList it was given.
                store(t, new E("((PyList) Py.iadd(" + cur0.c + ", " + box(expr(a.value)) + "))", cur0.t), b, ind);
                return;
            }
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
            if (!fieldStore(recv, at.attr)) {
                // The generic read, operation and store, the receiver once.
                E old = new E(attrGet(recv, at.attr), Ty.OBJECT);
                E v = binop(a.op, old, expr(a.value), a.value);
                indent(b, ind);
                b.append(em.guardedSet(at.attr, "(Object) " + box(recv), box(v))).append('\n');
                forgetAttr(at.attr);
                return;
            }
            if (fieldTy(recv, at.attr).k == Ty.K.INT && typedField(owner(recv, at.attr), at.attr) != null) {
                E v = binop(a.op, intField(recv, at.attr), expr(a.value), a.value);
                indent(b, ind);
                storeIntField(recv, at.attr, v, b, ind);
                return;
            }
            if (guardedField(owner(recv, at.attr), at.attr)) {
                augGuarded(recv, at.attr, a, b, ind);
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
            if (l.t.k == Ty.K.DICT) {
                // Python's order: the dict, the key, the read, the operand,
                // the store; the dict and the key are each evaluated once.
                String dv = fresh();
                String kv = fresh();
                indent(b, ind);
                b.append("Object ").append(dv).append(" = ").append(l.c).append(";\n");
                indent(b, ind);
                b.append("Object ").append(kv).append(" = ").append(box(i)).append(";\n");
                E old = dictRead("Typed.dgetv(" + dv + ", " + kv + ")", l.t);
                String ov = fresh();
                indent(b, ind);
                b.append(jt(old.t)).append(' ').append(ov).append(" = ").append(old.c).append(";\n");
                E v;
                if (old.t.opt || old.t.k != Ty.K.INT && old.t.k != Ty.K.FLOAT && old.t.k != Ty.K.STR
                        && old.t.k != Ty.K.BOOL) {
                    if (Emitter.BIN.get(a.op) == null) {
                        throw new Unsupported("subscript aug " + a.op);
                    }
                    // A mutable item may update in place (a list extends).
                    v = new E("Py.i" + Emitter.BIN.get(a.op) + "(" + box(new E(ov, old.t)) + ", "
                            + box(expr(a.value)) + ")", Ty.OBJECT);
                } else {
                    v = binop(a.op, new E(ov, old.t), expr(a.value), a.value);
                }
                indent(b, ind);
                b.append("Py.setitem(").append(dv).append(", ").append(kv).append(", ").append(box(v)).append(");\n");
                return;
            }
            if (l.t.k != Ty.K.LIST || !(i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                // Any other container: the generic protocol, in Python's order.
                if (Emitter.BIN.get(a.op) == null) {
                    throw new Unsupported("subscript aug " + a.op);
                }
                String dv = fresh();
                String kv = fresh();
                String ov = fresh();
                indent(b, ind);
                b.append("Object ").append(dv).append(" = ").append(box(l)).append(";\n");
                indent(b, ind);
                b.append("Object ").append(kv).append(" = ").append(box(i)).append(";\n");
                indent(b, ind);
                b.append("Object ").append(ov).append(" = Py.getitem(").append(dv).append(", ").append(kv).append(");\n");
                // The in-place protocol: a list item extends where it is.
                E v = new E("Py.i" + Emitter.BIN.get(a.op) + "(" + ov + ", " + box(expr(a.value)) + ")", Ty.OBJECT);
                indent(b, ind);
                b.append("Py.setitem(").append(dv).append(", ").append(kv).append(", ").append(v.c).append(");\n");
                return;
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
            boolean ge = el.k != Ty.K.INT && in.guessedSite(l.t);
            if (ge) {
                el = Ty.OBJECT;
            }
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
        if (a.value instanceof Name && binding(((Name) a.value).id) == Scope.Binding.LOCAL
                && compVar(((Name) a.value).id) == null) {
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
    /// `s[a:b]` with no step and int bounds, the bounds passed as Java
    /// longs (each beside the object that replaces it when it is a big int
    /// or a bool, see `dual`); or null when a bound is anything else.
    private String strSliceLong(E l, Slice sl) {
        if (sl.step != null) {
            return null;
        }
        String[] lo = sl.lower == null ? new String[] {"0L", "null"} : longPair(expr(sl.lower));
        String[] hi = sl.upper == null ? new String[] {"Long.MAX_VALUE", "null"} : longPair(expr(sl.upper));
        if (lo.length == 0 || hi.length == 0) {
            return null;
        }
        return "Typed.sliceL(" + l.c + ", " + lo[0] + ", " + lo[1] + ", " + hi[0] + ", " + hi[1] + ")";
    }

    private static final String[] NO_PAIR = new String[0];

    /// A list read through a call (a module global's `Typed.asList(g(5))`)
    /// held in a local before the statement, where the statement cannot
    /// rebind it, so the fast index and length paths can name it twice.
    private E heldList(E l) {
        if (plain(l.c) || pre == null || !stmtPure || condDepth != 0) {
            return l;
        }
        String t = fresh();
        indent(pre, preInd);
        pre.append("final PyList ").append(t).append(" = ").append(l.c).append(";\n");
        return new E(t, l.t);
    }

    /// `x % k` for an int `x` held as a long and a constant `k` of 31 bits
    /// above zero: Python's remainder, never an overflow, and an int.
    private E modConstant(BinOp b) {
        Long k = intConstant(b.right);
        Ty lt = in.type(b.left, cur, sc);
        if (lt == null || lt.opt || lt.k != Ty.K.INT) {
            return null;
        }
        if (k == null || k <= 0 || k > Integer.MAX_VALUE) {
            // `x % y` of two ints held as longs: the remainder is smaller
            // than `y`, so it is a long too; a zero `y` raises.
            Ty rt = in.type(b.right, cur, sc);
            if (k != null || rt == null || rt.opt || rt.k != Ty.K.INT) {
                return null;
            }
            E l = expr(b.left);
            E r = expr(b.right);
            if (l.lc == null || l.pk != null || r.lc == null || r.pk != null || l.t.k != Ty.K.INT || r.t.k != Ty.K.INT
                    || l.t.opt || r.t.opt) {
                return binop("%", l, r, b.right);
            }
            String lc = "Typed.lmod(" + l.lc + ", " + r.lc + ")";
            E m = new E("Long.valueOf(" + lc + ")", Ty.INT, lc);
            m.small = r.small;
            return m;
        }
        E l = expr(b.left);
        if (l.lc == null || l.pk != null || l.t.k != Ty.K.INT || l.t.opt) {
            return binop("%", l, constE(k), b.right);
        }
        String lc = "Typed.imod(" + l.lc + ", " + k + "L)";
        E r = new E("Long.valueOf(" + lc + ")", Ty.INT, lc);
        r.small = true;
        return r;
    }

    /// `"| %s | %d |" % (s, n)` with every value typed: a str for `%s`, an
    /// int held as a long for `%d`, `%i` or `%s`. One Java concatenation,
    /// with no tuple, no boxed int and no conversion dispatch; the values
    /// are evaluated left to right, as CPython builds the tuple. Null when
    /// any value is of another type, so `Py.pct` or `Py.mod` takes it.
    private E percentConcat(BinOp b) {
        if (!"%".equals(b.op) || !(b.left instanceof Constant)) {
            return null;
        }
        List<Expr> vals;
        if (b.right instanceof TupleExpr) {
            vals = ((TupleExpr) b.right).elts;
        } else {
            Ty rt = in.type(b.right, cur, sc);
            if (rt == null || rt.opt || rt.k != Ty.K.STR && rt.k != Ty.K.INT) {
                return null;
            }
            vals = java.util.Collections.singletonList(b.right);
        }
        for (Expr v : vals) {
            Ty vt = v instanceof Starred ? null : in.type(v, cur, sc);
            if (vt == null || vt.opt || vt.k != Ty.K.STR && vt.k != Ty.K.INT) {
                return null;
            }
        }
        List<String> lits = em.percentParts((Constant) b.left, vals.size());
        if (lits == null) {
            return null;
        }
        String convs = lits.get(lits.size() - 1);
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < vals.size(); i++) {
            Object kv = inlineArgs == null ? fold(vals.get(i)) : null;
            if (kv instanceof String && convs.charAt(i) == 's' || kv instanceof Long) {
                // A folded value joins the literal text around it.
                lits.set(i, lits.get(i) + kv + lits.get(i + 1));
                lits.remove(i + 1);
                convs = convs.substring(0, i) + convs.substring(i + 1);
                vals = new ArrayList<Expr>(vals);
                vals.remove(i);
                i--;
                continue;
            }
            E v = expr(vals.get(i));
            char k = convs.charAt(i);
            if (v.t.k == Ty.K.STR && !v.t.opt && k == 's') {
                parts.add(v.c);
            } else if (v.t.k == Ty.K.INT && !v.t.opt && v.lc != null && v.pk == null) {
                parts.add("Long.toString(" + v.lc + ")");
            } else {
                parts.add("Py.pconv('" + k + "', " + box(v) + ")");
            }
        }
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (int i = 0; i <= vals.size(); i++) {
            String lit = lits.get(i);
            if (!lit.isEmpty() || first && i == vals.size()) {
                sb.append(first ? "" : " + ").append(Emitter.javaString(lit));
                first = false;
            }
            if (i < vals.size()) {
                sb.append(first ? "\"\" + " : " + ").append(parts.get(i));
                first = false;
            }
        }
        return new E(sb.append(')').toString(), Ty.STR);
    }

    /// An int as a Java long and the object standing in for it (null when
    /// the long is the value), both free of effects; or an empty array.
    private static String[] longPair(E e) {
        if (e.t.k != Ty.K.INT || e.t.opt || e.pk != null) {
            return NO_PAIR;
        }
        if (e.ll != null && e.bg != null) {
            return new String[] {e.ll, e.bg};
        }
        if (e.lc != null && (plain(e.lc) || literal(e.lc) || e.small)) {
            return new String[] {e.lc, "null"};
        }
        return NO_PAIR;
    }

    private String sliceParts(Slice s) {
        Expr[] parts = {s.lower, s.upper, s.step};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(i == 0 ? "" : ", ").append(parts[i] == null ? "Py.NONE" : box(expr(parts[i])));
        }
        return sb.toString();
    }

    private boolean narrowable(String n) {
        return binding(n) == Scope.Binding.LOCAL && localType(n).nullable();
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
    /// Per function: each local stored exactly once, by a plain `n = value`,
    /// mapped to that value. A local whose one value folds to a constant
    /// holds that constant wherever it is bound, in a loop or not.
    private Map<String, Expr> soleStores;
    /// [#fold]'s answers, NO_FOLD for an expression that does not fold.
    private Map<Expr, Object> folded;
    private static final Object NO_FOLD = new Object();
    private final Set<String> folding = new HashSet<String>();

    /// The typed locals `es` read, as the generic code reads them, or null
    /// when one cannot be handed over (not bound on every path, or no
    /// object value; a closure or `:=` among them).
    private Map<String, String> defNames(List<Expr> es) {
        Map<String, String> names = new HashMap<String, String>();
        boolean[] ok = {true};
        Infer.Visit[] v = new Infer.Visit[1];
        v[0] = x -> {
            if (x instanceof NamedExpr || x instanceof Lambda || x instanceof Comp || x instanceof Yield
                    || x instanceof Await) {
                ok[0] = false;
                return;
            }
            if (x instanceof Name) {
                Name n = (Name) x;
                if (!trueCell(n.id) && binding(n.id) == Scope.Binding.LOCAL && !names.containsKey(n.id)) {
                    if (scalars.containsKey(n.id) || !sc.params.contains(n.id) && !da.safe.contains(n)
                            && !paramAlias.containsKey(n.id)) {
                        ok[0] = false;
                        return;
                    }
                    try {
                        names.put(n.id, "((Object) " + box(expr1(n)) + ")");
                    } catch (Unsupported u) {
                        ok[0] = false;
                    }
                }
            }
            Infer.children(x, v[0]);
        };
        for (Expr e : es) {
            v[0].on(e);
        }
        return ok[0] ? names : null;
    }

    /// How many times each name is bound anywhere in `body`, nested scopes
    /// included.
    static Map<String, Integer> storeCounts(List<Stmt> body) {
        Map<String, Integer> count = new HashMap<String, Integer>();
        Map<String, Expr> value = new HashMap<String, Expr>();
        for (Stmt st : body) {
            countStores(st, count, value);
        }
        return count;
    }

    private static Map<String, Expr> soleStores(List<Stmt> body) {
        Map<String, Integer> count = new HashMap<String, Integer>();
        Map<String, Expr> value = new HashMap<String, Expr>();
        for (Stmt st : body) {
            countStores(st, count, value);
        }
        Map<String, Expr> r = new HashMap<String, Expr>();
        for (Map.Entry<String, Expr> en : value.entrySet()) {
            if (count.get(en.getKey()) == 1) {
                r.put(en.getKey(), en.getValue());
            }
        }
        return r;
    }

    /// Every way a name is bound counts, in nested scopes too: a comprehension
    /// variable or an inner function's local of the same name only makes the
    /// local look stored twice, which costs a fold and never correctness.
    private static void countStores(Object o, Map<String, Integer> count, Map<String, Expr> value) {
        List<String> ns = new ArrayList<String>();
        if (o instanceof Assign) {
            Assign a = (Assign) o;
            for (Expr t : a.targets) {
                names(t, ns);
            }
            if (a.targets.size() == 1 && a.targets.get(0) instanceof Name) {
                value.put(((Name) a.targets.get(0)).id, a.value);
            }
        } else if (o instanceof AugAssign) {
            names(((AugAssign) o).target, ns);
        } else if (o instanceof AnnAssign) {
            names(((AnnAssign) o).target, ns);
            ns.add(ns.isEmpty() ? "" : ns.get(0));
        } else if (o instanceof For) {
            names(((For) o).target, ns);
        } else if (o instanceof NamedExpr) {
            ns.add(((NamedExpr) o).target.id);
        } else if (o instanceof Comprehension) {
            names(((Comprehension) o).target, ns);
        } else if (o instanceof WithItem && ((WithItem) o).vars != null) {
            names(((WithItem) o).vars, ns);
        } else if (o instanceof ExceptHandler && ((ExceptHandler) o).name != null) {
            ns.add(((ExceptHandler) o).name);
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                names(t, ns);
            }
        } else if (o instanceof Import || o instanceof ImportFrom) {
            for (Alias al : o instanceof Import ? ((Import) o).names : ((ImportFrom) o).names) {
                ns.add(al.asname != null ? al.asname : al.name);
            }
        } else if (o instanceof Global) {
            for (String n : ((Global) o).names) {
                ns.add(n);
                ns.add(n);
            }
        } else if (o instanceof FunctionDef) {
            ns.add(((FunctionDef) o).name);
        } else if (o instanceof ClassDef) {
            ns.add(((ClassDef) o).name);
        }
        for (String n : ns) {
            Integer c = count.get(n);
            count.put(n, c == null ? 1 : c + 1);
        }
        Infer.children(o, x -> countStores(x, count, value));
    }

    /// `e` as a constant computed here, when it is a pure function of
    /// literals and of locals whose one store folds: a str, a Long or a
    /// Boolean; or null. Strings fold only while ASCII, where Java's
    /// `trim`-free scans, `indexOf` and case maps answer what CPython does.
    private Object fold(Expr e) {
        Object m = folded.get(e);
        if (m != null) {
            return m == NO_FOLD ? null : m; //NOPMD CompareObjectsWithEquals - sentinel
        }
        Object r = fold0(e);
        folded.put(e, r == null ? NO_FOLD : r);
        return r;
    }

    private Object fold0(Expr e) {
        if (e instanceof Constant) {
            Constant c = (Constant) e;
            if (c.kind == Constant.Kind.STR) {
                return ascii(c.text) ? c.text : null;
            }
            if (c.kind == Constant.Kind.INT && c.text.length() <= 9 && c.text.matches("[0-9]+")) {
                return Long.valueOf(c.text);
            }
            if (c.kind == Constant.Kind.TRUE || c.kind == Constant.Kind.FALSE) {
                return c.kind == Constant.Kind.TRUE;
            }
            return null;
        }
        if (e instanceof Name) {
            Name n = (Name) e;
            Expr v = soleStores.get(n.id);
            if (v == null || !da.safe.contains(n) || binding(n.id) != Scope.Binding.LOCAL || !folding.add(n.id)) {
                return null;
            }
            try {
                return fold(v);
            } finally {
                folding.remove(n.id);
            }
        }
        if (e instanceof UnaryOp && "-".equals(((UnaryOp) e).op)) {
            Object v = fold(((UnaryOp) e).operand);
            return v instanceof Long ? Long.valueOf(-(Long) v) : null;
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            Object l = fold(b.left);
            Object r = l == null ? null : fold(b.right);
            if (l instanceof Long && r instanceof Long && small((Long) l) && small((Long) r)) {
                long x = (Long) l;
                long y = (Long) r;
                switch (b.op) {
                    case "+":
                        return x + y;
                    case "-":
                        return x - y;
                    case "*":
                        return x * y;
                    default:
                        return null;
                }
            }
            if (l instanceof String && r instanceof String && "+".equals(b.op)
                    && ((String) l).length() + ((String) r).length() <= 4096) {
                return (String) l + r;
            }
            return null;
        }
        if (e instanceof Subscript && ((Subscript) e).index instanceof Slice) {
            Slice sl = (Slice) ((Subscript) e).index;
            Object v = fold(((Subscript) e).value);
            if (!(v instanceof String) || sl.step != null) {
                return null;
            }
            String str = (String) v;
            Object lo = sl.lower == null ? Long.valueOf(0) : fold(sl.lower);
            Object hi = sl.upper == null ? Long.valueOf(str.length()) : fold(sl.upper);
            if (!(lo instanceof Long) || !(hi instanceof Long)) {
                return null;
            }
            int n = str.length();
            long a = (Long) lo < 0 ? Math.max(0, (Long) lo + n) : Math.min((Long) lo, n);
            long z = (Long) hi < 0 ? Math.max(0, (Long) hi + n) : Math.min((Long) hi, n);
            return a >= z ? "" : str.substring((int) a, (int) z);
        }
        if (!(e instanceof Call) || !((Call) e).keywords.isEmpty()) {
            return null;
        }
        Call c = (Call) e;
        for (Expr a : c.args) {
            if (a instanceof Starred) {
                return null;
            }
        }
        if (c.func instanceof Name && "len".equals(((Name) c.func).id) && c.args.size() == 1
                && binding("len") == Scope.Binding.BUILTIN) {
            Object v = fold(c.args.get(0));
            return v instanceof String ? Long.valueOf(((String) v).length()) : null;
        }
        if (!(c.func instanceof Attribute)) {
            return null;
        }
        Object recv = fold(((Attribute) c.func).value);
        if (!(recv instanceof String)) {
            return null;
        }
        String str = (String) recv;
        List<Object> args = new ArrayList<Object>();
        for (Expr a : c.args) {
            Object v = fold(a);
            if (!(v instanceof String)) {
                return null;
            }
            args.add(v);
        }
        String m = ((Attribute) c.func).attr;
        int na = args.size();
        if (na == 0) {
            switch (m) {
                case "strip":
                    return stripAscii(str, true, true);
                case "lstrip":
                    return stripAscii(str, true, false);
                case "rstrip":
                    return stripAscii(str, false, true);
                case "lower":
                    return asciiCase(str, false);
                case "upper":
                    return asciiCase(str, true);
                default:
                    return null;
            }
        }
        String a0 = (String) args.get(0);
        if (na == 1) {
            switch (m) {
                case "find":
                    return Long.valueOf(str.indexOf(a0));
                case "rfind":
                    return Long.valueOf(str.lastIndexOf(a0));
                case "startswith":
                    return str.startsWith(a0);
                case "endswith":
                    return str.endsWith(a0);
                case "count":
                    return a0.isEmpty() ? null : Long.valueOf(countOf(str, a0));
                default:
                    return null;
            }
        }
        if (na == 2 && "replace".equals(m) && !a0.isEmpty()) {
            String r = str.replace(a0, (String) args.get(1));
            return r.length() <= 4096 ? r : null;
        }
        return null;
    }

    private static boolean small(long v) {
        return v > -(1L << 30) && v < (1L << 30);
    }

    private static boolean ascii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= 128) {
                return false;
            }
        }
        return true;
    }

    /// CPython's whitespace among ASCII: space, tab to carriage return, and
    /// the four separators 0x1c to 0x1f.
    private static boolean pySpace(char ch) {
        return ch == ' ' || ch >= '\t' && ch <= '\r' || ch >= 0x1c && ch <= 0x1f;
    }

    private static String stripAscii(String s, boolean left, boolean right) {
        int a = 0;
        int z = s.length();
        while (left && a < z && pySpace(s.charAt(a))) {
            a++;
        }
        while (right && z > a && pySpace(s.charAt(z - 1))) {
            z--;
        }
        return s.substring(a, z);
    }

    private static String asciiCase(String s, boolean up) {
        char[] cs = s.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            char ch = cs[i];
            if (up && ch >= 'a' && ch <= 'z') {
                cs[i] = (char) (ch - 32);
            } else if (!up && ch >= 'A' && ch <= 'Z') {
                cs[i] = (char) (ch + 32);
            }
        }
        return new String(cs);
    }

    private static int countOf(String s, String sub) {
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + sub.length())) {
            n++;
        }
        return n;
    }

    /// A folded constant as an expression of its type.
    private static E constE(Object k) {
        if (k instanceof String) {
            return new E(Emitter.javaString((String) k), Ty.STR);
        }
        if (k instanceof Boolean) {
            return new E(((Boolean) k) ? "true" : "false", Ty.BOOL);
        }
        long v = (Long) k;
        E r = new E("Long.valueOf(" + v + "L)", Ty.INT, v + "L");
        r.small = v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE;
        return r;
    }

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

    /// Whether a store of `attr` on `r` can go straight to its Java field.
    private boolean fieldStore(E r, String attr) {
        if (r.t.k != Ty.K.INST || r.t.cls.frozenStores()) {
            return false;
        }
        Layouts.Cls o = Infer.fieldOwner(r.t.cls, attr);
        return o != null && (typedField(o, attr) != null || in.definitelySet(o, attr));
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

    /// A Java identifier for a class `ref` (`py_mod.L1_K`), to name a helper.
    private static String boxerId(String ref) {
        return ref.replace('.', '_');
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
            pre.append(o.ref).append(' ').append(t).append(" = ").append(recv).append(";\n");
            recv = t;
        }
        if (plain(recv)) {
            return dual(recv + ".t_" + m, recv + ".b_" + m);
        }
        fieldBoxers.add(o.ref + " " + m);
        return new E("fb_" + boxerId(o.ref) + "_" + m + "(" + recv + ")", Ty.INT);
    }

    /// `recv.attr op= value` on guarded field `attr` of `recv` (a plain
    /// name): the typed operation while the field holds its typed value,
    /// the generic one when it holds the object that replaced it.
    private void augGuarded(E recv, String attr, AugAssign a, StringBuilder b, int ind) {
        Layouts.Cls o = owner(recv, attr);
        Ty st = typedField(o, attr);
        String m = Emitter.mangle(attr);
        String rc = nz(recv, attr);
        // Python reads the target before it evaluates the value.
        String ot = fresh();
        String oo = fresh();
        indent(b, ind);
        b.append(jt(st)).append(' ').append(ot).append(" = ").append(rc).append(".t_").append(m).append(";\n");
        indent(b, ind);
        b.append("Object ").append(oo).append(" = ").append(rc).append(".b_").append(m).append(";\n");
        E val = spillTo(expr(a.value), b, ind);
        StringBuilder tb = new StringBuilder();
        String typedCode = null;
        try {
            E tv = binop(a.op, new E(ot, st), val, a.value);
            indent(tb, ind + 1);
            storeGuess(rc + ".t_" + m, rc + ".b_" + m, st, tv, tb, ind + 1);
            typedCode = tb.toString();
        } catch (Unsupported u) {
            typedCode = null;
        }
        StringBuilder gb = new StringBuilder();
        E gv = binop(a.op, pairE(ot, oo, st), val, a.value);
        indent(gb, ind + 1);
        storeGuess(rc + ".t_" + m, rc + ".b_" + m, st, gv, gb, ind + 1);
        if (typedCode == null) {
            b.append(gb);
            return;
        }
        indent(b, ind);
        b.append("if (").append(oo).append(" == null) {\n").append(typedCode);
        indent(b, ind);
        b.append("} else {\n").append(gb);
        indent(b, ind);
        b.append("}\n");
        forgetAttr(attr);
    }

    /// Stores `v` into typed int field `attr` of `r`; Python evaluates the
    /// value first.
    private void storeIntField(E r, String attr, E v, StringBuilder b, int ind) {
        String m = Emitter.mangle(attr);
        Layouts.Cls o = owner(r, attr);
        String recv = nz(r, attr);
        if (!plain(recv)) {
            String t = fresh();
            b.append(o.ref).append(' ').append(t).append(" = ").append(recv).append(";\n");
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

    /// The frame the generic expressions of the function being emitted
    /// take their temporaries from (see `genericFallback`), or null before the first.
    private Object fallback;

    /// `e`, typed where this class can lower it; where it cannot, the
    /// generic code for `e` alone (`genericFallback`), its value an Object.
    E expr(Expr e) {
        List<Kid> outer = kids;
        List<Kid> mine = new ArrayList<Kid>();
        StringBuilder p = pre;
        int at = p == null ? 0 : p.length();
        int cd = condDepth;
        E r;
        kids = mine;
        try {
            r = expr0(e, mine);
        } finally {
            kids = outer;
        }
        if (!mine.isEmpty() && pre == p) { //NOPMD CompareObjectsWithEquals - identity is meant
            r = split(e, r, mine, p, at, cd);
        }
        if (outer != null) {
            int end = p == null ? 0 : p.length();
            outer.add(new Kid(e, r, p, at, end, p == null ? "" : p.substring(at, end), cd));
        }
        return r;
    }

    private E expr0(Expr e, List<Kid> mine) {
        StringBuilder p = pre;
        int at = p == null ? 0 : p.length();
        Set<String> nn = new HashSet<String>(nonNull);
        Map<String, String> sh = new HashMap<String, String>(shadow);
        try {
            return expr1(e);
        } catch (Unsupported u) {
            // Nothing the failed attempt wrote or learned holds.
            if (p != null) {
                p.setLength(at);
            }
            nonNull = nn;
            shadow = sh;
            mine.clear();
            String g = genericFallback(e);
            if (g == null) {
                throw u;
            }
            return new E(g, Ty.OBJECT);
        }
    }

    /// An operand `expr` lowered for the expression being lowered: the
    /// value, and the statements it put before the statement (`pre` from
    /// `s` to `end`, that text), at conditional depth `cd`.
    private static final class Kid {
        final Expr x;
        final E v;
        final StringBuilder p; //NOPMD AvoidStringBufferField - names the builder, which the generator owns
        final int s;
        final int end;
        final String text;
        final int cd;

        Kid(Expr x, E v, StringBuilder p, int s, int end, String text, int cd) {
            this.x = x;
            this.v = v;
            this.p = p;
            this.s = s;
            this.end = end;
            this.text = text;
            this.cd = cd;
        }
    }

    /// The operands `expr` lowered for the expression being lowered, or
    /// null outside one (and while [#split] lowers it again).
    private List<Kid> kids;

    /// Whether an expression may run as two versions chosen by a test made
    /// before it: not one that evaluates operands conditionally, binds, or
    /// suspends, nor a scope of its own.
    private static boolean splittable(Expr e) {
        return !(e instanceof Name || e instanceof Constant || e instanceof BoolOp || e instanceof IfExp
                || e instanceof Lambda || e instanceof Comp || e instanceof NamedExpr || e instanceof Yield
                || e instanceof Await || e instanceof Compare && ((Compare) e).ops.size() > 1);
    }

    /// Whether evaluating `x` reads and never runs code that could store:
    /// a name, a constant, or an attribute or item of one.
    private static boolean readOnly(Expr x) {
        if (x instanceof Name || x instanceof Constant) {
            return true;
        }
        if (x instanceof Attribute) {
            return readOnly(((Attribute) x).value);
        }
        return false;
    }

    /// `e`, whose operands `ks` include guessed values (see `E.gg`), as two
    /// versions: one over the typed forms, taken while every operand's test
    /// holds, and one over the objects. The typed version is what makes a
    /// guess pay: code over a guessed field or local otherwise reads it as
    /// an object and runs generic. The operands are evaluated once, by
    /// whichever version runs, and the tests read only plain names (or,
    /// where every call of the statement is pure, fields), so nothing runs
    /// between a test and the read it vouches for that could change it.
    /// `trial` is `e` lowered as usual; answered when no split applies.
    private E split(Expr e, E trial, List<Kid> ks, StringBuilder p, int at, int cd) {
        if (!splittable(e) || leafSubst.containsKey(e) || inlineArgs != null && !inlined.isEmpty()) {
            return trial;
        }
        boolean any = false;
        boolean stable = true;
        boolean ok = true;
        boolean ran = false;
        Set<Expr> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<Expr, Boolean>());
        for (Kid k : ks) {
            if (k.cd != cd || k.v.pk != null || !seen.add(k.x) || k.x == e) { //NOPMD CompareObjectsWithEquals - identity is meant
                return trial;
            }
            if (k.v.gview != null) {
                any = true;
                stable &= k.v.gstable;
                // A field the test read may have changed when an operand
                // evaluated before this one could run code.
                ok &= k.v.gstable || !ran;
            }
            ran |= !readOnly(k.x);
        }
        if (!any || !ok && !stmtPure) {
            return trial;
        }
        // Whatever `e` itself put before the statement (an operand copied
        // into temporaries, say) stays, and nothing lowered again may skip
        // it: such an `e` is left as it is. Where it copied operands, it
        // lowered itself again over the copies, and that split already.
        int own = p == null ? 0 : p.length() - at;
        if (p != null) {
            int last = at;
            for (Kid k : ks) {
                if (k.p != p) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (k.end != k.s) {
                        return trial;
                    }
                    continue;
                }
                if (k.s < last || k.end > p.length() || !p.substring(k.s, k.end).equals(k.text)) {
                    return trial;
                }
                own -= k.end - k.s;
                last = k.end;
            }
        }
        if (own != 0) {
            return trial;
        }
        Map<Expr, E> savedLs = leafSubst;
        StringBuilder savedPre = pre;
        List<Kid> savedKids = kids;
        Set<String> nn = new HashSet<String>(nonNull);
        Map<String, String> sh = new HashMap<String, String>(shadow);
        List<String> tests = new ArrayList<String>();
        E typed;
        E gen;
        try {
            pre = null;
            kids = null;
            leafSubst = new IdentityHashMap<Expr, E>(savedLs);
            for (Kid k : ks) {
                leafSubst.put(k.x, k.v.gview != null ? k.v.gview : k.v);
                if (k.v.gview != null && !tests.contains(k.v.gg)) {
                    tests.add(k.v.gg);
                }
            }
            try {
                typed = expr1(e);
            } catch (Unsupported u) {
                return trial;
            } finally {
                nonNull = new HashSet<String>(nn);
                shadow = new HashMap<String, String>(sh);
            }
            for (Kid k : ks) {
                if (k.v.gview != null) {
                    leafSubst.put(k.x, new E(k.v.ga, Ty.OBJECT));
                }
            }
            try {
                gen = expr1(e);
            } catch (Unsupported u) {
                gen = null;
            } finally {
                nonNull = nn;
                shadow = sh;
            }
        } finally {
            pre = savedPre;
            kids = savedKids;
            leafSubst = savedLs;
        }
        if (typed.pk != null || gen != null && gen.pk != null) {
            return trial;
        }
        if (gen == null) {
            gen = trial;
        }
        E view = typed;
        boolean st = stable;
        StringBuilder test = new StringBuilder();
        for (String t : tests) {
            test.append(test.length() == 0 ? "" : " && ").append(t);
        }
        if (typed.gview != null) {
            view = typed.gview;
            st &= typed.gstable;
            test.append(" && ").append(typed.gg);
        } else if (typed.t.k != Ty.K.INT && "Object".equals(jt(typed.t))
                && (gen == null || typed.c.equals(gen.c))) {
            // An object either way, and no cheaper to reach typed. Where the
            // two differ the typed one still pays: `o.name` over a guessed
            // `o` reads the field, where the object version asks the
            // attribute site.
            return trial;
        }
        String g = test.toString();
        if (gen.gview == null && view.t.k == gen.t.k && jt(view.t).equals(jt(gen.t)) && !"Object".equals(jt(gen.t))
                && (view.t.k != Ty.K.INST || view.t.cls == gen.t.cls) && view.t.opt == gen.t.opt) { //NOPMD CompareObjectsWithEquals - identity is meant
            // Both versions answer the same Java type: one typed value.
            return new E("(" + g + " ? " + view.c + " : " + gen.c + ")", view.t);
        }
        if (gen.gview == null && view.t.k == Ty.K.INT && gen.t.k == Ty.K.INT) {
            // Both halves with a long form keep one: bs4's `len(self.name_rules)
            // == 1` over a split field boxed both lengths to compare them.
            return new E("(" + g + " ? " + view.c + " : " + gen.c + ")", Ty.INT,
                    view.lc != null && gen.lc != null ? "(" + g + " ? " + view.lc + " : " + gen.lc + ")" : null);
        }
        String ga = box(gen);
        E r = new E("(" + g + " ? " + box(view) + " : " + ga + ")", Ty.OBJECT);
        r.gg = g;
        r.gview = view;
        r.ga = ga;
        r.gstable = st;
        return r;
    }

    /// `e` as generic code (Emitter.genericExpr) reading this function's
    /// locals boxed, or null where that cannot be: inside an inlined body or
    /// a comprehension, for an expression with a scope or binding of its
    /// own, or one reading a local that may be unbound here or that has no
    /// object (a scalar).
    /// Per function: how often each local is bound, and those bound in a loop.
    private Map<String, Integer> fallbackStores;
    private Set<String> loopStored;

    /// Whether local `n` holds one value for good once bound: a parameter
    /// the body never rebinds, or a local bound once, outside every loop.
    private boolean settled(String n) {
        if (fallbackStores == null) {
            fallbackStores = storeCounts(cur.def.body);
            loopStored = new HashSet<String>();
            Infer.Visit[] v = new Infer.Visit[1];
            v[0] = x -> {
                if (x instanceof For) {
                    For f = (For) x;
                    List<String> ts = new ArrayList<String>();
                    names(f.target, ts);
                    loopStored.addAll(ts);
                    loopStored.addAll(assigned(f.body));
                    loopStored.addAll(assigned(f.orelse));
                } else if (x instanceof While) {
                    loopStored.addAll(assigned(((While) x).body));
                    loopStored.addAll(assigned(((While) x).orelse));
                }
                Infer.children(x, v[0]);
            };
            for (Stmt st : cur.def.body) {
                v[0].on(st);
            }
        }
        Integer c = fallbackStores.get(n);
        if (sc.params.contains(n)) {
            return c == null;
        }
        return c != null && c == 1 && !loopStored.contains(n);
    }

    /// Builtins that consume a generator argument before they return and
    /// keep nothing of it.
    private static final Set<String> CONSUMERS = new HashSet<String>(
            Arrays.asList("any", "all", "sum", "min", "max"));

    private String genericFallback(Expr e) {
        if (cur == null || sc != cur.def.scope || inlineArgs != null || !inlined.isEmpty() || plainExpr == e //NOPMD CompareObjectsWithEquals - identity is meant
                || e instanceof Name) {
            // A bare name gains nothing, and a value made here may be moved
            // ahead of the branch it is read in: the expression around it
            // falls back instead.
            return null;
        }
        boolean[] ok = {true};
        List<Name> reads = new ArrayList<Name>();
        Infer.Visit[] v = new Infer.Visit[1];
        // Generator expressions their call drains on the spot, found from
        // the call before its arguments are visited.
        Set<Comp> drained = new HashSet<Comp>();
        // Generator expressions a builtin runs to its end, or to where it
        // stops, before the call returns: nothing rebinds the function's
        // locals meanwhile, so they may read the values captured now.
        boolean[] lazy = {false};
        // A generator the call may keep, read after this statement: exact only
        // over locals nothing rebinds once it is made (checked below).
        boolean[] kept = {false};
        // The reads a kept closure makes when it runs, not when it is made:
        // only these need a local nothing rebinds.
        Set<Name> late = java.util.Collections.newSetFromMap(new IdentityHashMap<Name, Boolean>());
        // Inside a comprehension the generic code makes in a frame of its
        // own: anything nested is generic too.
        int[] inNow = {0};
        v[0] = x -> {
            if (x instanceof Call) {
                Call c = (Call) x;
                Comp d = em.eagerArgument(c);
                if (d == null && !c.args.isEmpty() && c.args.get(0) instanceof Comp && c.func instanceof Name
                        && CONSUMERS.contains(((Name) c.func).id)
                        && binding(((Name) c.func).id) == Scope.Binding.BUILTIN
                        && ((Comp) c.args.get(0)).kind == Comp.Kind.GENERATOR) {
                    d = (Comp) c.args.get(0);
                    lazy[0] = true;
                }
                if (d != null) {
                    drained.add(d);
                }
            }
            boolean keep = x instanceof Comp && !drained.contains(x) && Emitter.capturable((Comp) x);
            if (keep) {
                kept[0] = true;
                lazy[0] = true;
            }
            // A list, set or dict comprehension, or a generator its call
            // drains, runs to its end where it stands: generic code in a
            // frame of its own, over the values the locals hold now.
            boolean now = x instanceof Comp && !keep
                    && (((Comp) x).kind != Comp.Kind.GENERATOR || drained.contains(x))
                    && !em.helperNow((Comp) x, drained);
            if (now) {
                lazy[0] = true;
            }
            if (x instanceof Lambda && ((Lambda) x).scope.nonlocals.isEmpty()) {
                // A closure: kept, so over settled locals only.
                kept[0] = true;
                lazy[0] = true;
                Lambda l = (Lambda) x;
                int from = reads.size();
                v[0].on(l.body);
                for (int i = reads.size() - 1; i >= from; i--) {
                    if (l.scope.isLocal(reads.get(i).id)) {
                        reads.remove(i);
                    } else {
                        late.add(reads.get(i));
                    }
                }
                // Defaults are evaluated here, in the function's scope.
                for (Expr d : l.args.defaults) {
                    v[0].on(d);
                }
                for (Expr d : l.args.kwDefaults) {
                    if (d != null) {
                        v[0].on(d);
                    }
                }
            } else if (x instanceof Comp && (keep || now || inNow[0] > 0 || em.helperNow((Comp) x, drained))) {
                // Written as a method taking what it reads by value; the
                // names it binds itself are its own, not the function's.
                Scope cs = ((Comp) x).scope;
                int from = reads.size();
                boolean nested = inNow[0] > 0 && !keep && !now && !drained.contains(x);
                if (nested) {
                    // May outlive the call: read late, over settled locals.
                    kept[0] = true;
                }
                if (now) {
                    inNow[0]++;
                }
                try {
                    Infer.children(x, v[0]);
                } finally {
                    if (now) {
                        inNow[0]--;
                    }
                }
                Set<Name> first = java.util.Collections.newSetFromMap(new IdentityHashMap<Name, Boolean>());
                if (keep || nested) {
                    // The first iterable is evaluated where the generator is made.
                    Infer.Visit[] fv = new Infer.Visit[1];
                    fv[0] = y -> {
                        if (y instanceof Name) {
                            first.add((Name) y);
                        }
                        Infer.children(y, fv[0]);
                    };
                    fv[0].on(((Comp) x).generators.get(0).iter);
                }
                for (int i = reads.size() - 1; i >= from; i--) {
                    if (cs.isLocal(reads.get(i).id)) {
                        reads.remove(i);
                    } else if ((keep || nested) && !first.contains(reads.get(i))) {
                        late.add(reads.get(i));
                    }
                }
            } else if (x instanceof Lambda || x instanceof Comp || x instanceof NamedExpr || x instanceof Yield
                    || x instanceof Await) {
                ok[0] = false;
            } else if (ok[0]) {
                if (x instanceof Name) {
                    reads.add((Name) x);
                }
                if (x instanceof Call && ((Call) x).func instanceof Name && "super".equals(((Name) ((Call) x).func).id)
                        && ((Call) x).args.isEmpty() && !sc.params.isEmpty()) {
                    // Zero-argument super() reads the first parameter, and the
                    // class, which only a layout's field makes reachable here.
                    reads.add(new Name(sc.params.iterator().next()));
                    if (sc.enclosingClass == null || em.superClass(sc.enclosingClass) == null) {
                        ok[0] = false;
                    }
                }
                Infer.children(x, v[0]);
            }
        };
        v[0].on(e);
        if (!ok[0]) {
            return null;
        }
        Map<String, String> names = new HashMap<String, String>();
        Set<String> unbound = new HashSet<String>();
        for (Name n : reads) {
            if (trueCells.contains(n.id) && compVar(n.id) == null) {
                // A shared cell: the generic code reads the cell itself.
                continue;
            }
            if (binding(n.id) != Scope.Binding.LOCAL) {
                continue;
            }
            if (scalars.containsKey(n.id) || sc.cells.contains(n.id) && !Infer.compOnlyCells(cur.def).contains(n.id)) {
                return null;
            }
            if (kept[0] && late.contains(n) && !settled(n.id)) {
                return null;
            }
            if (!sc.params.contains(n.id) && !da.safe.contains(n) && compVar(n.id) == null
                    && !paramAlias.containsKey(n.id)) {
                // Not bound on every path: checked where the generic code
                // reads it, which may be a branch that never runs -- never
                // ahead of the expression. A lambda's capture is read early.
                Ty ut = localType(n.id);
                if (longLocals.contains(n.id) || accLocals.contains(n.id) || ut.k == Ty.K.INT
                        || !"null".equals(zero(ut)) || kept[0] && late.contains(n)) {
                    return null;
                }
                if (lazy[0]) {
                    // Handed over as it is, null while unbound, and checked
                    // inside, where the generic code reads it.
                    unbound.add(n.id);
                    names.put(n.id, guessLocal(n.id) ? "((Object) (" + over(n.id) + " != null ? " + over(n.id) + " : (Object) "
                            + var(n.id) + "))" : "((Object) " + var(n.id) + ")");
                    continue;
                }
                names.put(n.id, guessLocal(n.id) ? "((Object) " + expr1(n).c + ")"
                        : "((Object) Typed.local(" + var(n.id) + ", " + Emitter.javaString(n.id) + "))");
                continue;
            }
            try {
                // Of static type Object: the generic code tests it with
                // instanceof against classes a typed local cannot be.
                String r = box(expr1(n));
                if (kept[0] && r.contains("Typed.local(")) {
                    return null;
                }
                names.put(n.id, "((Object) " + r + ")");
            } catch (Unsupported u) {
                return null;
            }
        }
        if (fallback == null) {
            fallback = em.fallbackFrame(sc);
        }
        if (!lazy[0] || names.isEmpty()) {
            return em.genericExpr(fallback, sc, e, names);
        }
        if (names.size() > 8) {
            return null;
        }
        // The generator is an object the Java expression makes: what it reads
        // goes in as parameters, which a local reassigned later cannot be.
        Map<String, String> params = new HashMap<String, String>();
        StringBuilder sig = new StringBuilder();
        StringBuilder args = new StringBuilder();
        for (Map.Entry<String, String> en : names.entrySet()) {
            String p = fresh();
            params.put(en.getKey(), unbound.contains(en.getKey())
                    ? "((Object) Typed.local(" + p + ", " + Emitter.javaString(en.getKey()) + "))" : p);
            sig.append(sig.length() == 0 ? "" : ", ").append("Object ").append(p);
            args.append(args.length() == 0 ? "" : ", ").append(en.getValue());
        }
        // A frame of its own, its temporaries declared inside the lambda,
        // which cannot assign the function's.
        Object own = em.fallbackFrame(sc);
        String g = em.genericExpr(own, sc, e, params);
        if (g == null) {
            return null;
        }
        return "((PyFunction.Body" + names.size() + ") (" + sig + ") -> {\n" + em.fallbackDecls(own) + "\nreturn " + g
                + ";\n}).run(" + args + ")";
    }

    /// `(n := v)`: a Java assignment, itself an expression, so nothing is
    /// hoisted and the value is stored exactly where Python stores it.
    private E namedExpr(NamedExpr w) {
        String n = w.target.id;
        if (trueCell(n)) {
            E v = expr(w.value);
            return new E("(" + cellRef(n) + " = " + box(v) + ")", Ty.OBJECT);
        }
        if (binding(n) != Scope.Binding.LOCAL || compVar(n) != null || inlineArgs != null
                || longLocals.contains(n) || accLocals.contains(n) || scalars.containsKey(n) || guessLocal(n)) {
            // A guessed local's object half would have to change with it.
            throw new Unsupported("NamedExpr");
        }
        Ty lt = localType(n);
        if (lt.k == Ty.K.INT || lt.k == Ty.K.BOT) {
            throw new Unsupported("NamedExpr");
        }
        E v = expr(w.value);
        paramAlias.remove(n);
        forgetPathsOf(n);
        shadow.remove(n);
        nonNull.remove(n);
        return new E("(" + var(n) + " = " + conv(v, lt) + ")", lt);
    }

    private E expr1(Expr e) {
        E sub = leafSubst.get(e);
        if (sub != null) {
            return sub;
        }
        if (!(e instanceof Constant) && inlineArgs == null && soleStores != null) {
            Object k = fold(e);
            if (k != null) {
                return constE(k);
            }
        }
        if (e instanceof BinOp && "%".equals(((BinOp) e).op) && inlineArgs == null) {
            E m = modConstant((BinOp) e);
            if (m != null) {
                return m;
            }
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
                case BYTES:
                    return new E(em.constant(c), Ty.BYTES);
                default:
                    throw new Unsupported("constant");
            }
        }
        if (e instanceof Name) {
            return name((Name) e);
        }
        if (e instanceof NamedExpr) {
            return namedExpr((NamedExpr) e);
        }
        if (e instanceof Attribute) {
            Attribute a = (Attribute) e;
            if (Infer.MATH_CONSTS.contains(a.attr) && in.isMath(a.value, sc)) {
                return new E(mathConstant(a.attr), Ty.FLOAT);
            }
            if (a.value instanceof Name && scalars.containsKey(((Name) a.value).id)) {
                return scalarRead(((Name) a.value).id, a.attr);
            }
            Layouts.Cls sk = in.classStaticOf(a.value, a.attr, sc);
            if (sk != null) {
                // A once-bound class attribute: its typed static field.
                return staticE(em.classStaticRef(sk, a.attr), in.classStatic(sk, a.attr));
            }
            E r = expr(a.value);
            if (r.t.k != Ty.K.INST) {
                return new E(attrGet(r, a.attr), Ty.OBJECT);
            }
            E viaStatic = r.t.opt || !plain(r.c) ? null : instanceStatic(r, a.attr);
            if (viaStatic != null) {
                return viaStatic;
            }
            Layouts.Cls fo = Infer.fieldOwner(r.t.cls, a.attr);
            if (fo != null && typedField(fo, a.attr) == null && !in.definitelySet(fo, a.attr) && !r.t.opt && plain(r.c)) {
                // A field that may be unset: its value when it holds one, the
                // generic read -- class attribute, AttributeError -- when not.
                String f = r.c + ".f_" + Emitter.mangle(a.attr);
                return new E("(" + f + " != null ? " + f + " : " + em.attrSite(a.attr) + ".get(" + r.c + "))", Ty.OBJECT);
            }
            Infer.Fn pg = fo == null && plain(r.c) ? in.propertyGetter(r.t, a.attr) : null;
            if (pg != null && typed.get(pg) != null) {
                // A property's getter called as a method is, unless the
                // class was patched at run time; generic reads otherwise
                // went through the site and the getter's PyFunction (bs4's
                // `SoupStrainer.includes_everything` in every match).
                calls.add(pg);
                E got = result(pg, typed.get(pg) + "(" + r.c + ")");
                return new E("(" + r.c + ".cls.patched ? " + em.attrSite(a.attr) + ".get(" + r.c + ") : " + box(got) + ")",
                        Ty.OBJECT);
            }
            if (fo == null || typedField(fo, a.attr) == null && !in.definitelySet(fo, a.attr)) {
                // Not a field the class declares, or one that may be unset: the
                // generic read, with its AttributeError, __getattr__ and class
                // attributes, rather than giving up the whole typed function.
                return new E(attrGet(r, a.attr), Ty.OBJECT);
            }
            Ty ft = fieldTy(r, a.attr);
            if (ft.k == Ty.K.INT && typedField(owner(r, a.attr), a.attr) != null) {
                return intField(r, a.attr);
            }
            String p = path(a);
            if (guardedField(owner(r, a.attr), a.attr)) {
                Ty st = typedField(owner(r, a.attr), a.attr);
                return guessField(r, a.attr, p != null && nonNull.contains(p) ? st.nonNone() : st);
            }
            return new E(fieldRef(r, a.attr), p != null && nonNull.contains(p) ? ft.nonNone() : ft);
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            E pc = percentConcat(b);
            if (pc != null) {
                return pc;
            }
            String pf = em.percentHead(b);
            if (pf != null) {
                StringBuilder sb = new StringBuilder(pf);
                for (Expr v : ((TupleExpr) b.right).elts) {
                    sb.append(", ").append(box(expr(v)));
                }
                return new E(sb.append(')').toString(), Ty.STR);
            }
            if ("%".equals(b.op) && b.left instanceof Constant && !(b.right instanceof TupleExpr)) {
                Ty rt = in.type(b.right, cur, sc);
                if (rt != null && !rt.opt && (rt.k == Ty.K.STR || rt.k == Ty.K.INT || rt.k == Ty.K.FLOAT)) {
                    String one = em.percentHead((Constant) b.left, 1);
                    if (one != null) {
                        return new E(one + ", " + box(expr(b.right)) + ")", Ty.STR);
                    }
                }
            }
            if (pre != null && condDepth == 0 && arith(e)) {
                // Not under a branch: the block runs before the statement.
                E h = hoistInt(e);
                if (h != null) {
                    return h;
                }
            }
            if ("+".equals(b.op) && b.left instanceof BinOp && "+".equals(((BinOp) b.left).op)) {
                E c = strChain(b);
                if (c != null) {
                    return c;
                }
            }
            E fz = fusedDyn(e);
            if (fz != null) {
                return fz;
            }
            E bl = expr(b.left);
            E br = expr(b.right);
            if (Infer.realPow(b, bl.t, br.t)) {
                return new E("Typed.pow(" + toD(bl) + ", " + toD(br) + ")", Ty.FLOAT);
            }
            return binop(b.op, bl, br, b.right);
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
            String f = "-".equals(u.op) ? "neg" : "~".equals(u.op) ? "invert" : "pos";
            return new E("Py." + f + "(" + box(o) + ")", Ty.OBJECT);
        }
        if (e instanceof BoolOp) {
            BoolOp o = (BoolOp) e;
            List<E> vs = new ArrayList<E>();
            boolean bools = true;
            for (int i = 0; i < o.values.size(); i++) {
                condDepth += i == 0 ? 0 : 1;
                try {
                    vs.add(expr(o.values.get(i)));
                } finally {
                    condDepth -= i == 0 ? 0 : 1;
                }
                bools &= vs.get(i).t.k == Ty.K.BOOL;
            }
            StringBuilder sb = new StringBuilder("(");
            if (bools) {
                String j = o.isAnd ? " && " : " || ";
                for (int i = 0; i < vs.size(); i++) {
                    sb.append(i == 0 ? "" : j).append(vs.get(i).c);
                }
                return new E(sb.append(')').toString(), Ty.BOOL);
            }
            // The operand that decides, as Python answers it: each but the
            // last is kept in a temporary while its truth is tested.
            String t = exprTemp("Object");
            for (int i = 0; i < vs.size() - 1; i++) {
                sb.append(o.isAnd ? "!" : "").append("Py.isTrue(").append(t).append(" = ").append(box(vs.get(i)))
                        .append(") ? ").append(t).append(" : ");
            }
            return new E(sb.append(box(vs.get(vs.size() - 1))).append(')').toString(), Ty.OBJECT);
        }
        if (e instanceof Compare) {
            Compare cm = (Compare) e;
            if (pre != null && condDepth == 0 && cm.ops.size() == 1 && ORDERS.contains(cm.ops.get(0))
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
                if (l.t.k == Ty.K.STR && !l.t.opt) {
                    String fast = strSliceLong(l, (Slice) s.index);
                    if (fast != null) {
                        return new E(fast, Ty.STR);
                    }
                    return new E("Typed.slice(" + l.c + ", " + sliceParts((Slice) s.index) + ")", Ty.STR);
                }
                if (l.t.k != Ty.K.LIST) {
                    // A bytes or any other value: the generic slice, which has a
                    // direct path for bytes with int bounds.
                    return new E("Py.getslice(" + box(l) + ", " + sliceParts((Slice) s.index) + ")",
                            l.t.k == Ty.K.BYTES ? Ty.BYTES : Ty.OBJECT);
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
            if (l.t.k == Ty.K.DICT) {
                return dictRead("Typed.dgetv(" + l.c + ", " + box(i) + ")", l.t);
            }
            if (l.t.k == Ty.K.BYTES && (i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                if (i.lc == null) {
                    // As for a str below: the generic read raises what
                    // CPython raises for an index no long holds.
                    return new E("Py.getitem(" + box(l) + ", " + box(i) + ")", Ty.INT);
                }
                String c = "Typed.bat(" + l.c + ", " + i.lc + ")";
                return small(new E("Long.valueOf(" + c + ")", Ty.INT, c));
            }
            if (l.t.k == Ty.K.STR && !l.t.opt && (i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                // An index that is not a long in hand goes the generic way: one
                // too big for a long is an IndexError there, as in CPython.
                String slow = "Typed.asStr(Py.getitem(" + l.c + ", " + box(i) + "))";
                String sp = "";
                if (s.value instanceof Name && flatStr.contains(((Name) s.value).id) && sc == cur.def.scope //NOPMD CompareObjectsWithEquals - identity is meant
                        && compVar(((Name) s.value).id) == null && l.c.equals(var(((Name) s.value).id))) {
                    String c = "sp" + l.c;
                    sp = ", (" + c + " < 0 ? (" + c + " = Typed.surr(" + l.c + ")) : " + c + ")";
                }
                E se = new E(i.lc != null ? "Typed.sat(" + l.c + ", " + i.lc + sp + ")"
                        : i.ll != null ? "(" + i.bg + " == null ? Typed.sat(" + l.c + ", " + i.ll + sp + ") : " + slow + ")"
                        : slow, Ty.STR);
                if (!sp.isEmpty() && i.lc != null && (plain(i.lc) || literal(i.lc))) {
                    se.charAt = new String[] {l.c, i.lc, sp.substring(2)};
                }
                return se;
            }
            if (l.t.k != Ty.K.LIST || !(i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                if (Emitter.isStrLiteral(i.c)) {
                    return new E(em.keyRead(box(l), i.c), Ty.OBJECT);
                }
                return new E("Py.getitem(" + box(l) + ", " + box(i) + ")", Ty.OBJECT);
            }
            l = heldList(l);
            Ty el = in.elem(l.t);
            if (el.k == Ty.K.BOT) {
                throw new Unsupported("element of nothing");
            }
            if (el.k != Ty.K.INT && in.guessedSite(l.t)) {
                return guessedElem("h_get(" + l.c + ", " + box(i) + ")", el, null, 0);
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
            } else if (i.lc != null && i.pk == null && el.k != Ty.K.FLOAT) {
                // A list named by a call: one helper, the index unboxed.
                fast = "Typed.at(" + l.c + ", " + i.lc + ")";
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
            sb.append(')');
            return new E(Emitter.foldable(el) ? em.hoist("PyTuple", sb.toString()) : sb.toString(), Ty.OBJECT);
        }
        if (e instanceof ListExpr) {
            ListExpr l = (ListExpr) e;
            Integer site = in.siteOf.get(l);
            if (site == null) {
                throw new Unsupported("list site");
            }
            if (l.elts.isEmpty()) {
                return new E("Py.list()", Ty.list(new int[] {site.intValue()}, false));
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
            return builtinResult((Call) e, call((Call) e));
        }
        if (e instanceof Comp && (((Comp) e).kind != Comp.Kind.GENERATOR || eagerGens.contains(e))) {
            return comp((Comp) e);
        }
        if (e instanceof DictExpr) {
            DictExpr d = (DictExpr) e;
            String shape = d.keys.contains(null) ? null : em.dictShape(d.keys);
            if (shape != null) {
                StringBuilder sb = new StringBuilder("PyDict.literal(").append(shape).append(", new Object[] {");
                for (int i = 0; i < d.values.size(); i++) {
                    sb.append(i == 0 ? "" : ", ").append(box(expr(d.values.get(i))));
                }
                return new E(sb.append("})").toString(), Ty.OBJECT);
            }
            // Made at its final size, one put per pair: no array of the pairs is built.
            StringBuilder sb = new StringBuilder("new PyDict(").append(d.keys.size()).append(')');
            for (int i = 0; i < d.keys.size(); i++) {
                if (d.keys.get(i) == null) {
                    throw new Unsupported("dict spread");
                }
                sb.append(".put(").append(box(expr(d.keys.get(i)))).append(", ")
                        .append(box(expr(d.values.get(i)))).append(')');
            }
            return new E(sb.toString(), Ty.OBJECT);
        }
        if (e instanceof Lambda && closedLambda((Lambda) e) && ((Lambda) e).args.defaults.isEmpty()
                && ((Lambda) e).args.kwDefaults.stream().allMatch(x -> x == null)) {
            // Nothing of this function in it: the function object the generic
            // code makes, which reads no local of the frame it is made in.
            return new E(em.lambdaValue((Lambda) e), Ty.OBJECT);
        }
        if (e instanceof JoinedStr) {
            return new E(joined((JoinedStr) e), Ty.OBJECT);
        }
        throw new Unsupported(e.getClass().getSimpleName());
    }

    /// An f-string, as the generic code builds it: every value is formatted
    /// by the runtime, so a typed operand is boxed and nothing else differs.
    private String joined(JoinedStr j) {
        List<String> parts = new ArrayList<String>();
        for (Expr p : j.parts) {
            if (p instanceof Constant) {
                parts.add(em.constant((Constant) p));
                continue;
            }
            FormattedValue f = (FormattedValue) p;
            String v = box(expr(f.value));
            if (f.conversion == 'r') {
                v = "Py.str(Py.toRepr(" + v + "))";
            } else if (f.conversion == 's') {
                v = "Py.str(Py.strOf(" + v + "))";
            } else if (f.conversion == 'a') {
                v = "Py.call(Builtins.ASCII, " + v + ")";
            }
            String spec;
            if (f.spec == null || f.spec.parts.isEmpty()) {
                spec = "\"\"";
            } else if (f.spec.parts.size() == 1 && f.spec.parts.get(0) instanceof Constant) {
                spec = Emitter.javaString(((Constant) f.spec.parts.get(0)).text);
            } else {
                spec = "Py.strOf(" + joined(f.spec) + ")";
            }
            parts.add("Py.format(" + v + ", " + spec + ")");
        }
        if (parts.isEmpty()) {
            return "Py.str(\"\")";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        String built = Emitter.stringBuilderChain(parts);
        if (built != null) {
            return built;
        }
        StringBuilder sb = new StringBuilder("Py.concat(");
        for (int i = 0; i < parts.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(parts.get(i));
        }
        return sb.append(')').toString();
    }

    /// Whether lambda `l` reads nothing but its parameters, globals and
    /// builtins, and opens no scope of its own.
    private static boolean closedLambda(Lambda l) {
        if (l.scope.hasYield || !l.scope.cells.isEmpty()) {
            return false;
        }
        boolean[] ok = {true};
        Infer.Visit[] v = new Infer.Visit[1];
        v[0] = x -> {
            if (x instanceof Lambda || x instanceof Comp || x instanceof NamedExpr || x instanceof Yield
                    || x instanceof Await) {
                ok[0] = false;
            } else if (x instanceof Name) {
                Scope.Binding b = l.scope.resolve(((Name) x).id);
                ok[0] &= b == Scope.Binding.LOCAL || b == Scope.Binding.GLOBAL || b == Scope.Binding.BUILTIN;
            } else if (ok[0]) {
                Infer.children(x, v[0]);
            }
        };
        v[0].on(l.body);
        return ok[0];
    }

    /// The parameters of a lambda being expanded in place (`inlineLambda`),
    /// each to its argument already in a local; read by `name` while `sc` is
    /// that lambda's scope.
    private Map<String, E> inlineArgs;
    private Scope inlineScope;

    /// `(lambda x, y: body)(a, b)` as `body`, in this function: the arguments
    /// into locals before the statement, in order, then the body over them.
    /// Null when that is not the same: the statement may not move its
    /// evaluation, or the lambda reads this function's locals.
    private E inlineLambda(Lambda l, Call c) {
        Arguments la = l.args;
        if (pre == null || !stmtPure || condDepth != 0 || inlineArgs != null || !c.keywords.isEmpty()
                || la.vararg != null || la.kwarg != null || !la.kwonly.isEmpty() || !la.defaults.isEmpty()
                || la.positional.size() != c.args.size() || !closedLambda(l)) {
            return null;
        }
        Map<String, E> map = new HashMap<String, E>();
        for (int i = 0; i < c.args.size(); i++) {
            if (c.args.get(i) instanceof Starred) {
                return null;
            }
        }
        for (int i = 0; i < c.args.size(); i++) {
            map.put(la.positional.get(i).name, spillTo(expr(c.args.get(i)), pre, preInd));
        }
        Scope saved = sc;
        sc = l.scope;
        inlineArgs = map;
        inlineScope = l.scope;
        try {
            return expr(l.body);
        } finally {
            sc = saved;
            inlineArgs = null;
            inlineScope = null;
        }
    }

    /// Parameters read as they arrived until the body's first statement
    /// rebinds them (`function`).
    private final Map<String, E> paramAlias = new HashMap<String, E>();

    private E name(Name nm) {
        String n = nm.id;
        if (inlineArgs != null && sc == inlineScope && inlineArgs.containsKey(n)) { //NOPMD CompareObjectsWithEquals - identity is meant
            return inlineArgs.get(n);
        }
        if (trueCell(n)) {
            return new E("Typed.local(" + cellRef(n) + ", " + Emitter.javaString(n) + ")", Ty.OBJECT);
        }
        switch (binding(n)) {
            case LOCAL: {
                String cv = compVar(n);
                if (cv != null) {
                    // A comprehension's own target, in a temporary of its own
                    // even where the function has a local of the same name;
                    // a guessed one is the pair, whose typed half holds
                    // nothing of an element that missed.
                    String co = compOver.get(cv);
                    return co != null ? pairE(cv, co, compVarType(n)) : new E(cv, compVarType(n));
                }
                E alias = paramAlias.get(n);
                if (alias != null) {
                    return alias;
                }
                if (!sc.params.contains(n) && !da.safe.contains(nm)) {
                    Ty ut = localType(n);
                    if (ut != null && unboundInts.contains(n) && !longLocals.contains(n)
                            && !scalars.containsKey(n) && !shadow.containsKey(n)
                            && inlineArgs == null) {
                        // Checked here, as CPython checks it. The read is an
                        // expression, so it stays inside any branch it is in.
                        String chk = "Typed.boundInt(" + big(n) + ", " + Emitter.javaString(n) + ")";
                        if (accLocals.contains(n)) {
                            return new E("(" + chk + " == null ? (Object) Long.valueOf(" + var(n) + ") : " + big(n)
                                    + ")", Ty.OBJECT);
                        }
                        return dual(var(n), chk);
                    }
                    if (ut == null || longLocals.contains(n) || accLocals.contains(n) || scalars.containsKey(n)
                            || ut.k == Ty.K.INT || !"null".equals(zero(ut)) || inlineArgs != null) {
                        throw new Unsupported("maybe unbound " + n);
                    }
                    // Null until bound: checked here, as CPython checks it.
                    if (guessLocal(n)) {
                        return new E("(" + over(n) + " != null ? " + over(n) + " : Typed.local(" + box(new E(var(n), ut))
                                + ", " + Emitter.javaString(n) + "))", Ty.OBJECT);
                    }
                    return new E("Typed.local(" + var(n) + ", " + Emitter.javaString(n) + ")", ut);
                }
                Ty t = localType(n);
                if (sc.deleted.contains(n) && (longLocals.contains(n) || !"Object".equals(jt(t)))) {
                    // An object local is null once deleted, and DefAssign has
                    // already proven this read follows a binding (an except
                    // target inside its handler); a long cannot be unbound.
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
                if (accLocals.contains(n)) {
                    // An int while the object is null: the view [#split]
                    // lowers comparisons and subscripts over, so html.parser's
                    // `while k < n and rawdata[k] not in _TAG_END` compares
                    // longs and reads a char instead of boxing `k` for
                    // Py.lt and Py.getitem.
                    E r = new E("(" + big(n) + " == null ? (Object) Long.valueOf(" + var(n) + ") : " + big(n) + ")", Ty.OBJECT);
                    r.gg = big(n) + " == null";
                    r.gview = new E("Long.valueOf(" + var(n) + ")", Ty.INT, var(n));
                    r.ga = r.c;
                    r.gstable = true;
                    return r;
                }
                if (guessLocal(n)) {
                    return pairE(var(n), over(n), nonNull.contains(n) ? t.nonNone() : t);
                }
                Ty nt = in.narrowedAt.get(nm);
                if (nt != null && !"boolean".equals(jt(t)) && !"double".equals(jt(t))
                        && (nt.k == Ty.K.INST && nt.cls.compiled || nt.k == Ty.K.STR || nt.k == Ty.K.LIST)) {
                    // Inside `if isinstance(x, K):` x is a K.
                    return new E("((" + jt(nt) + ") " + var(n) + ")", nt);
                }
                return new E(var(n), nonNull.contains(n) ? t.nonNone() : t);
            }
            case GLOBAL: {
                if (in.globalFns.containsKey(n) || in.globalClasses.containsKey(n)) {
                    // The function or class object itself: Infer has seen
                    // it escape, so nothing assumes its every caller.
                    return new E("g(" + em.slot(n) + ")", Ty.OBJECT);
                }
                Ty t = in.globals.get(n);
                if (t == null || t.k == Ty.K.BOT) {
                    // No type known (bound only through globals(), a star
                    // import or a later definition): the slot as it is,
                    // NameError while unbound.
                    return new E("g(" + em.slot(n) + ")", Ty.OBJECT);
                }
                if (t.k != Ty.K.INT && in.guessedGlobal(n)) {
                    return guessedElem("g(" + em.slot(n) + ")", t, null, 0);
                }
                return new E(unbox("g(" + em.slot(n) + ")", t), t);
            }
            case BUILTIN:
                if (em.moduleScopeHasStarImport()) {
                    throw new Unsupported("name " + n);
                }
                return new E(em.builtinRef(n), Ty.OBJECT);
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
        boolean quiet = e instanceof Constant || e instanceof Name && binding(((Name) e).id) == Scope.Binding.LOCAL;
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
        if (!(e instanceof BinOp)) {
            return 0;
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
            // Through expr, so a leaf with a typed view splits (see #split);
            // with no pre it cannot come back here.
            return expr(e);
        } finally {
            pre = saved;
        }
    }

    /// `v` in plain Java locals, so it may be read twice: declared in `pre`.
    private E spill(E v) {
        if (v.gr != null && v.gr.indexOf('.') < 0 || v.ll != null || plain(v.c) && v.lc == null || v.lc != null && (plain(v.lc) || literal(v.lc))) {
            return v;
        }
        if (v.gview != null && v.gstable && v.gview.lc != null && plain(v.gview.lc) && v.ga.equals(v.c)) {
            // An accumulator's read (see the name read): both forms read
            // plain locals, so it may be read twice as it is, and a copy
            // would lose the int view the comparison splits on.
            return v;
        }
        return spillTo(v, pre, preInd);
    }

    /// `v` copied into fresh Java locals declared in `into`.
    private E spillTo(E v, StringBuilder into, int ind) {
        if (v.gview != null && v.go == null && v.gview.t.k != Ty.K.INT && !"Object".equals(jt(v.gview.t))) {
            // Two versions' value: a pair again, the test made once.
            Ty t = v.gview.t;
            String r = fresh();
            String o = fresh();
            indent(into, ind);
            into.append(jt(t)).append(' ').append(r).append(" = ").append(zero(t)).append(";\n");
            indent(into, ind);
            into.append("Object ").append(o).append(" = null;\n");
            indent(into, ind);
            into.append("if (").append(v.gg).append(") {\n");
            indent(into, ind + 1);
            into.append(r).append(" = ").append(v.gview.c).append(";\n");
            indent(into, ind);
            into.append("} else {\n");
            indent(into, ind + 1);
            into.append(o).append(" = ").append(v.ga).append(";\n");
            indent(into, ind);
            into.append("}\n");
            return pairE(r, o, t);
        }
        if (v.gr != null) {
            if (v.gr.indexOf('.') < 0) {
                return v;
            }
            // A field's pair: a copy, which nothing evaluated later changes.
            String r = fresh();
            String o = fresh();
            indent(into, ind);
            into.append(jt(v.gt)).append(' ').append(r).append(" = ").append(v.gr).append(";\n");
            indent(into, ind);
            into.append("Object ").append(o).append(" = ").append(v.go).append(";\n");
            return pairE(r, o, v.gt);
        }
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
            // An int is boxed inside the slow path, never at the call: a
            // call that boxes puts objects on ParparVM's operand stack, which
            // gives the typed function a frame the C compiler will not
            // inline, and that call is what a small arithmetic function
            // costs (spectral_norm's a(i, j): 33ms out of line, 12ms inlined).
            if (v.t.k == Ty.K.INT && v.ll != null) {
                params.append(sep).append("long ").append(pn).append(", Object q").append(i);
                args.append(sep).append(v.ll).append(", ").append(v.bg);
                takes.append("        Object o").append(i).append(" = q").append(i).append(" != null ? q").append(i)
                        .append(" : Long.valueOf(").append(pn).append(");\n");
                sub.put(ls.get(i), new E("o" + i, Ty.INT));
                continue;
            }
            if (v.t.k == Ty.K.INT && v.lc != null) {
                params.append(sep).append("long ").append(pn);
                args.append(sep).append(v.lc);
                takes.append("        Object o").append(i).append(" = Long.valueOf(").append(pn).append(");\n");
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

    /// Arithmetic of two or more operators whose every leaf is of a type
    /// not known here (`a * x + c * y + e` over a tuple's items): the
    /// generic emitter's fused helper, which computes the whole tree in
    /// doubles when the leaves are floats and boxes only the result,
    /// instead of one boxed value per operator. Null when a leaf is typed;
    /// the typed operators are better then.
    private E fusedDyn(Expr e) {
        if (Emitter.numOps(e) < 2) {
            return null;
        }
        List<Expr> leaves = new ArrayList<Expr>();
        Emitter.numLeaves(e, leaves);
        if (leaves.isEmpty()) {
            return null;
        }
        Map<Expr, String> code = new IdentityHashMap<Expr, String>();
        for (Expr l : leaves) {
            if (l instanceof Constant) {
                continue;
            }
            E v = expr(l);
            if (!v.t.wide()) {
                return null;
            }
            code.put(l, box(v));
        }
        String f = em.fused(e, null, x -> code.containsKey(x) ? code.get(x) : box(expr(x)));
        return f == null ? null : new E(f, Ty.OBJECT);
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
            case INT: {
                Long k = intConstant(right);
                if (("+".equals(op) || "-".equals(op)) && l.small && l.lc != null && l.pk == null && !l.t.opt && k != null
                        && Math.abs(k.longValue()) < 0x80000000L) {
                    // A length or an index plus a constant of 31 bits: no
                    // overflow is possible, so plain Java arithmetic.
                    String lc = "(" + l.lc + " " + op + " " + k + "L)";
                    return new E("Long.valueOf(" + lc + ")", Ty.INT, lc);
                }
                return new E(intOp(pyOp(op), box(l), box(r), right), Ty.INT);
            }
            case STR:
                if ("%".equals(op)) {
                    return new E("Typed.format(" + l.c + ", " + box(r) + ")", Ty.STR);
                }
                if ("*".equals(op)) {
                    return new E("Typed.asStr(Py.mul(" + box(l) + ", " + box(r) + "))", Ty.STR);
                }
                return new E("Typed.concat(" + l.c + ", " + r.c + ")", Ty.STR);
            case BOT:
                throw new Unsupported("operation on nothing");
            default:
                if ("+".equals(op) || "-".equals(op)) {
                    // An operand of unknown type is still an int most of the time
                    // (`s += x` over a generator's values). The int helper tests for
                    // two Longs inline and hands anything else to the same Py call.
                    return new E(intOp(pyOp(op), box(l), box(r), right), Ty.OBJECT);
                }
                return new E("Py." + pyOp(op) + "(" + box(l) + ", " + box(r) + ")", Ty.OBJECT);
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
            // `a < b < c` is `a < b and b < c` with b evaluated once (into a
            // temporary unless it is a name or a constant) and c only when
            // the first link held; every link must be a bool.
            StringBuilder sb = new StringBuilder("(");
            E left = expr(c.left);
            for (int i = 0; i < c.ops.size(); i++) {
                Expr rn = c.comparators.get(i);
                // A later link runs only when the ones before it held:
                // nothing of it may be hoisted ahead of the test.
                condDepth += i == 0 ? 0 : 1;
                E right;
                try {
                    right = expr(rn);
                } finally {
                    condDepth -= i == 0 ? 0 : 1;
                }
                E use = right;
                E next = right;
                if (i < c.ops.size() - 1 && !(rn instanceof Name || rn instanceof Constant)) {
                    if (right.lc != null || right.ll != null) {
                        throw new Unsupported("chained comparison");
                    }
                    String t = exprTemp(jt(right.t));
                    use = new E("(" + t + " = " + right.c + ")", right.t);
                    next = new E(t, right.t);
                }
                E e = compareOp(c.ops.get(i), left, use, rn);
                if (e.t.k != Ty.K.BOOL) {
                    throw new Unsupported("chained comparison");
                }
                sb.append(i == 0 ? "" : " && ").append(e.c);
                left = next;
            }
            return new E(sb.append(")").toString(), Ty.BOOL);
        }
        List<Expr> lit = Emitter.inTuple(c);
        if (lit != null) {
            // `x in ("ltr", "rtl")`: compared item by item, no tuple built.
            StringBuilder r = new StringBuilder("not in".equals(c.ops.get(0)) ? "(!" : "(").append("Py.inOf(")
                    .append(box(expr(c.left)));
            for (Expr t : lit) {
                r.append(", ").append(box(expr(t)));
            }
            return new E(r.append("))").toString(), Ty.BOOL);
        }
        return compareOp(c.ops.get(0), expr(c.left), expr(c.comparators.get(0)), c.comparators.get(0));
    }

    /// `l op r`, both evaluated; `rn` is r's tree, for its int range.
    private E compareOp(String op, E l, E r, Expr rn) {
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
                // As objects: Java refuses `==` between two classes neither
                // of which extends the other, and two such values are
                // simply not the same object.
                s = "((Object) " + l.c + " == (Object) " + r.c + ")";
            } else if (op.startsWith("is")) {
                s = "((Object) " + box(l) + " == (Object) " + box(r) + ")";
            } else {
                throw new Unsupported("== on " + l.t + ", " + r.t);
            }
            return new E(neg ? "!" + s : s, Ty.BOOL);
        }
        boolean li = l.t.k == Ty.K.INT || l.t.k == Ty.K.BOOL;
        boolean ri = r.t.k == Ty.K.INT || r.t.k == Ty.K.BOOL;
        if ("in".equals(op) || "not in".equals(op)) {
            String s;
            if (r.t.k == Ty.K.LIST) {
                s = "Typed.nzl(" + r.c + ").contains(" + box(l) + ")";
            } else if (r.t.k == Ty.K.STR && !r.t.opt && l.charAt != null) {
                // `rawdata[k] in _TAG_END` (html.parser): the char itself.
                s = "Typed.inAt(" + r.c + ", " + l.charAt[0] + ", " + l.charAt[1] + ", " + l.charAt[2] + ")";
            } else if (r.t.k == Ty.K.STR && !r.t.opt && l.t.k == Ty.K.STR && !l.t.opt) {
                s = "(" + r.c + ".indexOf(" + l.c + ") >= 0)";
            } else {
                s = "Typed.in(" + box(l) + ", " + box(r) + ")";
            }
            return new E("in".equals(op) ? s : "!" + s, Ty.BOOL);
        }
        if (l.t.k == Ty.K.BOOL && r.t.k == Ty.K.BOOL && ("==".equals(op) || "!=".equals(op))) {
            return new E("(" + l.c + " " + op + " " + r.c + ")", Ty.BOOL);
        }
        if (l.t.k == Ty.K.INT && r.t.k == Ty.K.INT && l.lc != null && r.lc != null && l.pk == null && r.pk == null
                && !l.t.opt && !r.t.opt) {
            // Both at hand as Java longs (a length, a counter, a constant).
            return new E("(" + l.lc + " " + op + " " + r.lc + ")", Ty.BOOL);
        }
        if (l.t.k == Ty.K.INT && r.t.k == Ty.K.INT && !l.t.opt && !r.t.opt && l.pk == null && r.pk == null
                && (l.lc != null || l.ll != null && l.bg != null) && (r.lc != null || r.ll != null && r.bg != null)
                && (l.ll != null || r.ll != null) && ORDERS_AND_EQ.contains(op)) {
            // At least one side a long with a big-int overflow beside it: the
            // longs compare in place unless an overflow is held.
            String lv = l.lc != null ? l.lc : l.ll;
            String rv = r.lc != null ? r.lc : r.ll;
            String held = l.ll != null && r.ll != null ? l.bg + " == null && " + r.bg + " == null"
                    : (l.ll != null ? l.bg : r.bg) + " == null";
            String slow = "!=".equals(op) ? "!" + intOp("eq", box(l), box(r), rn) : intOp(pyCmp(op), box(l), box(r), rn);
            return new E("(" + held + " ? " + lv + " " + op + " " + rv + " : " + slow + ")", Ty.BOOL);
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
                    return new E("!" + intOp("eq", box(l), box(r), rn), Ty.BOOL);
                default:
                    throw new Unsupported("compare " + op);
            }
            return new E(intOp(f, box(l), box(r), rn), Ty.BOOL);
        }
        if ((l.t.k == Ty.K.FLOAT || li) && (r.t.k == Ty.K.FLOAT || ri)) {
            return new E("(" + toD(l) + " " + op + " " + toD(r) + ")", Ty.BOOL);
        }
        if (l.t.k == Ty.K.STR && r.t.k == Ty.K.STR && !l.t.opt && !r.t.opt
                && ("==".equals(op) || "!=".equals(op))) {
            String ch = l.charAt != null ? oneChar(r.c) : null;
            if (ch != null) {
                // `rawdata[k] == "/"`: the char compared in place.
                String s = "Typed.charIs(" + l.charAt[0] + ", " + l.charAt[1] + ", " + l.charAt[2] + ", " + ch + ")";
                return new E("==".equals(op) ? s : "!" + s, Ty.BOOL);
            }
            String s = l.c + ".equals(" + r.c + ")";
            return new E("==".equals(op) ? s : "!" + s, Ty.BOOL);
        }
        if (l.t.k == Ty.K.INST && r.t.k == Ty.K.INST && ("==".equals(op) || "!=".equals(op))
                && !customCompare(l.t.cls) && !customCompare(r.t.cls)) {
            return new E("(" + l.c + " " + op + " " + r.c + ")", Ty.BOOL);
        }
        if (("==".equals(op) || "!=".equals(op)) && plain(l.c) && plain(r.c)
                && (l.t.k == Ty.K.STR && !l.t.opt && r.t.wide()
                        || r.t.k == Ty.K.STR && !r.t.opt && l.t.wide())) {
            // A str against a value of no known type: compared in place when
            // the value is a str, the generic comparison (a user __eq__ may
            // answer anything) when not. Both sides are locals or constants,
            // so neither branch reorders an effect.
            String x = l.t.wide() ? l.c : r.c;
            String k = l.t.wide() ? r.c : l.c;
            String gen = "Py." + ("==".equals(op) ? "eq" : "ne") + "(" + l.c + ", " + r.c + ")";
            return new E("(" + x + " instanceof String ? Py.bool(" + ("==".equals(op) ? "" : "!") + k + ".equals("
                    + x + ")) : " + gen + ")", Ty.OBJECT);
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
        // Infer types this OBJECT too: a user comparison may answer anything.
        // Its truth is asked of the runtime directly: Py.eqBool and its
        // siblings answer isTrue of the same call without boxing a bool.
        String bl = box(l);
        String br = box(r);
        E res = new E("Py." + f + "(" + bl + ", " + br + ")", Ty.OBJECT);
        res.tst = "Py." + f + "Bool(" + bl + ", " + br + ")";
        return res;
    }

    /// The Java char literal for a str literal `c` of one plain ASCII
    /// printable char (no quote or backslash to escape), or null.
    private static String oneChar(String c) {
        if (c.length() == 3 && c.charAt(0) == '"' && c.charAt(2) == '"') {
            char x = c.charAt(1);
            if (x >= 32 && x < 127 && x != '\'' && x != '\\' && x != '"') {
                return "'" + x + "'";
            }
        }
        return null;
    }

    private static final Set<String> ORDERS_AND_EQ = new HashSet<String>(Arrays.asList("<", "<=", ">", ">=", "==", "!="));

    private static String pyCmp(String op) {
        switch (op) {
            case "<":
                return "lt";
            case "<=":
                return "le";
            case ">":
                return "gt";
            case ">=":
                return "ge";
            default:
                return "eq";
        }
    }

    /// Whether an instance typed `k` may compare itself: a class above or
    /// below it anywhere in the program binds `__eq__` or `__ne__`, or has a
    /// base besides its compiled parent (see [#customTruth]).
    private boolean customCompare(Layouts.Cls k) {
        return hierarchyBinds(k, "__eq__", "__ne__");
    }

    // ---- calls -----------------------------------------------------------

    private List<E> args(Call c) {
        if (!c.keywords.isEmpty()) {
            throw new Unsupported("keyword arguments");
        }
        return exprs(c.args);
    }

    /// The arguments of `c`, a call of `f` from its parameter `from` on,
    /// keywords bound to parameters as Infer bound them.
    private List<E> args(Call c, FunctionDef f, int from) {
        List<Expr> b = f == null ? null : Infer.staticArgs(c, f, from);
        if (b == null) {
            throw new Unsupported("keyword arguments");
        }
        List<E> r = exprs(b);
        boundLists.put(r, Boolean.TRUE);
        return r;
    }

    /// The argument lists [#args(Call, FunctionDef, int)] bound to a
    /// callee's parameters by name: only such a list reaches its keyword-only
    /// ones (#argList).
    private final Map<List<E>, Boolean> boundLists = new IdentityHashMap<List<E>, Boolean>();

    private List<E> exprs(List<Expr> l) {
        List<E> r = new ArrayList<E>();
        for (Expr a : l) {
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
        Arguments fa = f.def.args;
        List<Expr> defs = fa.defaults;
        int np = fa.positional.size();
        if (!fa.kwonly.isEmpty() && args.size() > np - from && !boundLists.containsKey(args)) {
            // A positional argument past the positional parameters: Python
            // raises, it never fills a keyword-only one.
            throw new Unsupported("positional argument for a keyword-only parameter");
        }
        for (int i = 0; i < n; i++) {
            E a;
            if (i < args.size()) {
                a = args.get(i);
            } else {
                // Keyword-only parameters follow the positional ones, each
                // with its own default (Arguments#names).
                int at = from + i;
                Expr dv;
                if (at >= np) {
                    dv = fa.vararg == null && at - np < fa.kwDefaults.size() ? fa.kwDefaults.get(at - np) : null;
                } else {
                    int d = at - (np - defs.size());
                    dv = d < 0 ? null : defs.get(d);
                }
                if (!(dv instanceof Constant)) {
                    throw new Unsupported("missing argument");
                }
                a = expr(dv);
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
        if (f.guessRet != null) {
            // An object back (Infer.Fn#guessRet): the typed value when it
            // fits, as a pair.
            return guessedElem(call, f.guessRet, null, 0);
        }
        return new E(call, f.ret.k == Ty.K.BOT ? Ty.OBJECT : f.ret);
    }

    /// Builtins that consume a generator argument whole, at once: handing
    /// them a list of the same items is not observable.
    private static final Set<String> EAGER = new HashSet<String>(Arrays.asList(
            "sum", "list", "tuple", "set", "frozenset", "sorted", "min", "max"));

    /// Generator expressions lowered as list comprehensions (`EAGER`).
    private final Set<Comp> eagerGens = java.util.Collections.newSetFromMap(new IdentityHashMap<Comp, Boolean>());

    /// Builtins Infer types by their arguments (Infer#builtinValue).
    private static final Set<String> LOWERED_BUILTINS = new HashSet<String>(
            Arrays.asList("sorted", "min", "max", "sum", "round"));

    private static final Set<String> FRAME_BUILTINS = new HashSet<String>(
            Arrays.asList("super", "globals", "locals", "vars", "dir"));

    /// Set while lowering a call [#call] wraps for its caller's module.
    private boolean inModuleCall;

    private E call(Call c) {
        if (in.isCast(c, sc)) {
            // typing.cast(T, v) is v (Infer#isCast).
            return expr(c.args.get(1));
        }
        if (!inModuleCall && (c.func instanceof Name && "type".equals(((Name) c.func).id) && c.args.size() == 3
                && c.keywords.isEmpty() && binding("type") == Scope.Binding.BUILTIN
                || Emitter.isClassFactory(c))) {
            // __module__ comes from the caller's globals, as in the generic
            // code (Emitter's call): Py.typeModule / Py.callerModule.
            boolean type = c.func instanceof Name && "type".equals(((Name) c.func).id) && c.args.size() == 3;
            inModuleCall = true;
            E made;
            try {
                made = call(c);
            } finally {
                inModuleCall = false;
            }
            forgetPaths();
            return new E((type ? "Py.typeModule(" : "Py.callerModule(") + box(made) + ", M)", Ty.OBJECT);
        }
        if (c.func instanceof Name && FRAME_BUILTINS.contains(((Name) c.func).id) && c.args.isEmpty()
                && binding(((Name) c.func).id) == Scope.Binding.BUILTIN) {
            // Zero-argument super() needs the class and the first parameter,
            // globals() the module: the generic code (genericFallback) has
            // both. locals() and vars() name its frame, and stay generic.
            throw new Unsupported(((Name) c.func).id);
        }
        if (Emitter.castCall(c)) {
            // `cast(T, v)` (see Emitter.castCall). Both branches are
            // conditional, so nothing of either is hoisted ahead of the test.
            condDepth++;
            try {
                String f = box(expr(c.func));
                String v = box(expr(c.args.get(1)));
                String r = "(" + f + " == StdModules.CAST ? " + v + " : Py.call(" + box(expr(c.func)) + ", "
                        + box(expr(c.args.get(0))) + ", " + box(expr(c.args.get(1))) + "))";
                forgetPaths();
                return new E(r, Ty.OBJECT);
            } finally {
                condDepth--;
            }
        }
        if (c.args.size() == 1 && c.keywords.isEmpty() && c.args.get(0) instanceof Comp
                && ((Comp) c.args.get(0)).kind == Comp.Kind.GENERATOR
                && (c.func instanceof Name && EAGER.contains(((Name) c.func).id)
                && binding(((Name) c.func).id) == Scope.Binding.BUILTIN && !em.moduleScopeHasStarImport()
                || c.func instanceof Attribute && "join".equals(((Attribute) c.func).attr)
                && ((Attribute) c.func).value instanceof Constant
                && ((Constant) ((Attribute) c.func).value).kind == Constant.Kind.STR)) {
            eagerGens.add((Comp) c.args.get(0));
        }
        Comp extended = Infer.extendedGenerator(c);
        if (extended != null && in.extendsEagerly(extended)) {
            eagerGens.add(extended);
        }
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

    /// The hidden calls of a miss probe, as `Emitter.missIntrinsic` lowers
    /// them.
    private E missIntrinsic(Call c) {
        String fn = ((Name) c.func).id;
        if ("$probe".equals(fn)) {
            return new E(probeRead(c.args.get(0), true), Ty.OBJECT);
        }
        if ("$unmiss".equals(fn) && plainProbe(c.args.get(0))) {
            // A miss reaches here only when the handler's name was rebound:
            // the re-read raises the real KeyError for it.
            String m = box(expr(c.args.get(0)));
            return dictRead("(" + m + " == Py.MISS ? " + probeRead(c.args.get(1), false) + " : " + m + ")",
                    expr(((Subscript) c.args.get(1)).value).t);
        }
        if ("$ismiss".equals(fn) && plainProbe(c.args.get(0))) {
            return new E("(" + box(expr(c.args.get(0))) + " == Py.MISS && " + excRef(c.args.get(1)) + " == Py.KEY_ERROR)",
                    Ty.BOOL);
        }
        if ("$unmiss".equals(fn)) {
            String m = box(expr(c.args.get(0)));
            String v = "(" + m + " == Py.MISS ? " + probeRead(c.args.get(1), false) + " : Py.unmissed(" + m + "))";
            Expr x = c.args.get(1);
            if (x instanceof Subscript && ((Subscript) x).value instanceof Name) {
                E d = expr(((Subscript) x).value);
                if (d.t.k == Ty.K.DICT) {
                    return dictRead(v, d.t);
                }
            }
            return new E(v, Ty.OBJECT);
        }
        if ("$ismiss".equals(fn)) {
            return new E("Py.isMiss(" + box(expr(c.args.get(0))) + ", " + excRef(c.args.get(1)) + ", "
                    + ("AttributeError".equals(((Name) c.args.get(1)).id) ? "Py.ATTRIBUTE_ERROR" : "Py.KEY_ERROR") + ")",
                    Ty.BOOL);
        }
        return null;
    }

    /// `d[k] = d.get(k, c) + e` on a dict local, with a name for a key and
    /// int constants for c and e: one probe of `d`, as the generic code
    /// does it. False, emitting nothing, when the statement is not that.
    private boolean dictCount(Assign a, StringBuilder b, int ind) {
        if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Subscript) || !(a.value instanceof BinOp)) {
            return false;
        }
        Subscript t = (Subscript) a.targets.get(0);
        BinOp op = (BinOp) a.value;
        if (!"+".equals(op.op) || !(t.value instanceof Name) || !(t.index instanceof Name) || !(op.left instanceof Call)
                || intConstant(op.right) == null) {
            return false;
        }
        Call c = (Call) op.left;
        if (!(c.func instanceof Attribute) || !"get".equals(((Attribute) c.func).attr)
                || !(((Attribute) c.func).value instanceof Name) || c.args.size() != 2 || !c.keywords.isEmpty()
                || !(c.args.get(0) instanceof Name) || intConstant(c.args.get(1)) == null) {
            return false;
        }
        String d = ((Name) t.value).id;
        String k = ((Name) t.index).id;
        if (!d.equals(((Name) ((Attribute) c.func).value).id) || !k.equals(((Name) c.args.get(0)).id)) {
            return false;
        }
        E de = expr(t.value);
        if (de.t.k != Ty.K.DICT || de.t.opt) {
            return false;
        }
        indent(b, ind);
        b.append("Py.dictCount(").append(de.c).append(", ").append(box(expr(t.index))).append(", ")
                .append(box(expr(c.args.get(1)))).append(", ").append(box(expr(op.right))).append(");\n");
        return true;
    }

    /// `c`, a read of a dict of type `d`, as the type its values have.
    private E dictRead(String c, Ty d) {
        Ty vt = in.dictVal(d);
        if (vt.k == Ty.K.BOT) {
            throw new Unsupported("value of an empty dict");
        }
        if (vt.k != Ty.K.INT && in.guessedSite(d)) {
            return guessedElem(c, vt, null, 0);
        }
        return vt.wide() || vt.isObject() ? new E(c, vt.wide() ? Ty.OBJECT : vt) : new E(unbox(c, vt), vt);
    }

    private String probeRead(Expr e, boolean probe) {
        if (e instanceof Attribute) {
            Attribute a = (Attribute) e;
            if (a.value instanceof Name && scalars.containsKey(((Name) a.value).id)) {
                return scalarProbeRead(((Name) a.value).id, a.attr, probe);
            }
            return em.attrSite(a.attr) + (probe ? ".getOrMiss(" : ".get(") + box(expr(a.value)) + ")";
        }
        Subscript s = (Subscript) e;
        if (probe && dictProbe(s)) {
            E k = expr(s.index);
            return "Typed.dprobe(" + expr(s.value).c + ", " + (k.lc != null ? k.lc : box(k)) + ")";
        }
        return (probe ? "Py.getitemOrMiss(" : "Py.getitem(") + box(expr(s.value)) + ", " + box(expr(s.index)) + ")";
    }

    /// `d[k]` on a dict local that is never None: a plain `PyDict`, whose
    /// probe answers the value or the bare miss sentinel.
    private boolean dictProbe(Subscript s) {
        if (!(s.value instanceof Name) || binding(((Name) s.value).id) != Scope.Binding.LOCAL) {
            return false;
        }
        Ty t = localType(((Name) s.value).id);
        return t.k == Ty.K.DICT && !t.opt;
    }

    /// Whether hidden local `m` (a miss probe's) only ever holds a
    /// [#dictProbe]: the value itself, or `Py.MISS`.
    private boolean plainProbe(Expr m) {
        if (!(m instanceof Name) || soleStores == null) {
            return false;
        }
        Expr v = soleStores.get(((Name) m).id);
        return v instanceof Call && ((Call) v).func instanceof Name && "$probe".equals(((Name) ((Call) v).func).id)
                && ((Call) v).args.get(0) instanceof Subscript && dictProbe((Subscript) ((Call) v).args.get(0));
    }

    private E call0(Call c) {
        if (Emitter.nextOrStop(c)) {
            return new E("Builtins.nextOrStop(" + box(expr(c.args.get(0))) + ", " + box(expr(c.args.get(1))) + ")",
                    Ty.OBJECT);
        }
        if (c.func instanceof Lambda) {
            E r = inlineLambda((Lambda) c.func, c);
            if (r != null) {
                return r;
            }
        }
        if (c.func instanceof Name && ((Name) c.func).id.startsWith("$")) {
            E miss = missIntrinsic(c);
            if (miss != null) {
                return miss;
            }
        }
        if (Emitter.getattrDefault(c) && binding("getattr") == Scope.Binding.BUILTIN && !em.moduleScopeHasStarImport()) {
            Attribute probe = new Attribute(c.args.get(0), ((Constant) c.args.get(1)).text);
            return new E("Py.orDefault(" + probeRead(probe, true) + ", " + box(expr(c.args.get(2))) + ")", Ty.OBJECT);
        }
        if (c.args.size() == 1 && c.args.get(0) instanceof Name && codePoints.containsKey(((Name) c.args.get(0)).id)
                && ordOf(c, ((Name) c.args.get(0)).id)) {
            String l = "((long) " + codePoints.get(((Name) c.args.get(0)).id) + ")";
            return new E("Long.valueOf(" + l + ")", Ty.INT, l);
        }
        if (c.func instanceof Attribute && ((Attribute) c.func).value instanceof Name
                && scalars.containsKey(((Name) ((Attribute) c.func).value).id)) {
            String n = ((Name) ((Attribute) c.func).value).id;
            Layouts.Cls o = Layouts.resolver(scalars.get(n).k, ((Attribute) c.func).attr);
            return scalarRead(n, getterField(in.emitFns().get(o.typable.get(((Attribute) c.func).attr))));
        }
        if (c.func instanceof Name) {
            String n = ((Name) c.func).id;
            Scope.Binding b = binding(n);
            if (b == Scope.Binding.GLOBAL) {
                Infer.Fn f = in.globalFns.get(n);
                if (f != null && (in.doors.contains(c) || substObject(c)) && !doorFits(f, c, 0)) {
                    // An argument is not known to be what a parameter's
                    // annotation names (Infer.callFn): the function object
                    // is called, and its guard picks the version to run.
                    Ty rt = in.type(c, cur, sc);
                    String g = genericCall("g(" + em.slot(n) + ")", c).c;
                    if (rt.k != Ty.K.INT && in.guessed(c)) {
                        return guessedElem(g, rt, null, 0);
                    }
                    return rt.wide() || rt.k == Ty.K.BOT || rt.isObject() ? new E(g, rt.k == Ty.K.BOT ? Ty.OBJECT : rt)
                            : new E(unbox(g, rt), rt);
                }
                if (f != null) {
                    List<E> a = args(c, f.def, 0);
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
                Layouts.Cls k = in.emitClass(n);
                if (k != null && (in.doors.contains(c) || substObject(c)) && in.newMethod(k) == null && !k.callConstructed()
                        && !initFits(k, c)) {
                    // An argument the typed __init__ cannot be sure of
                    // (Infer.callFn): the class is called as generic code
                    // calls it, and __init__'s guard picks the version.
                    return new E(unbox(genericCall("g(" + em.slot(n) + ")", c).c, Ty.inst(k, false)), Ty.inst(k, false));
                }
                if (k != null) {
                    Layouts.Cls io = Layouts.resolver(k, "__init__");
                    FunctionDef ii = io == null ? null : io.typable.get("__init__");
                    return construct(k, c.keywords.isEmpty() && (ii == null || ii.args.kwonly.isEmpty()) ? args(c)
                            : args(c, ii, 1));
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
                return new E("Typed.listOf(" + box(x) + ")", lt);
            }
            if (b == Scope.Binding.BUILTIN && !em.moduleScopeHasStarImport()) {
                if (("any".equals(n) || "all".equals(n)) && c.keywords.isEmpty() && c.args.size() == 1
                        && c.args.get(0) instanceof Comp && ((Comp) c.args.get(0)).kind == Comp.Kind.GENERATOR) {
                    E r = anyAll(c, n);
                    if (r != null) {
                        return r;
                    }
                }
                if (TYPED_BUILTINS.contains(n) && c.keywords.isEmpty()) {
                    E r = builtin(n, args(c));
                    if (r != null) {
                        return r;
                    }
                }
                if ("isinstance".equals(n) && c.args.size() == 2 && c.keywords.isEmpty()
                        && !(c.args.get(0) instanceof Starred) && !(c.args.get(1) instanceof Starred)) {
                    // A Java boolean: no argument array, no bool to box and test.
                    if (c.args.get(1) instanceof TupleExpr) {
                        List<Expr> ts = ((TupleExpr) c.args.get(1)).elts;
                        if (ts.size() >= 2 && ts.size() <= 8 && !Infer.hasStarred(ts)) {
                            // `isinstance(x, (A, B))`: no tuple built (Py.isinstanceAny).
                            StringBuilder r = new StringBuilder("Py.isTrue(Py.isinstanceAny(").append(em.builtinRef(n))
                                    .append(", ").append(box(expr(c.args.get(0))));
                            for (Expr t : ts) {
                                r.append(", ").append(box(expr(t)));
                            }
                            return new E(r.append("))").toString(), Ty.BOOL);
                        }
                    }
                    return new E("Py.isinstanceB(" + em.builtinRef(n) + ", " + box(expr(c.args.get(0))) + ", "
                            + box(expr(c.args.get(1))) + ")", Ty.BOOL);
                }
                if ("next".equals(n) && (c.args.size() == 1 || c.args.size() == 2) && c.keywords.isEmpty()
                        && !(c.args.get(0) instanceof Starred) && !(c.args.get(c.args.size() - 1) instanceof Starred)) {
                    return new E("Py.nextB(" + em.builtinRef(n) + ", " + box(expr(c.args.get(0)))
                            + (c.args.size() == 2 ? ", " + box(expr(c.args.get(1))) : "") + ")", Ty.OBJECT);
                }
                E g = genericCall(em.builtinRef(n), c);
                Ty fixed = Infer.fixedReturn(n);
                if (fixed == null && LOWERED_BUILTINS.contains(n)) {
                    // What Infer#builtinValue answers for it, over values the
                    // runtime orders and adds as primitives.
                    Ty rt = in.type(c, cur, sc);
                    fixed = rt.wide() || rt.k == Ty.K.BOT || rt.opt ? null : rt;
                }
                return fixed == null ? g : new E(unbox(g.c, fixed), fixed);
            }
            // Any other callable a name holds: a class without a layout,
            // an imported function, a parameter.
            return genericCall(box(name((Name) c.func)), c);
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
            Ty nr = in.nativeCalls.get(c);
            if (nr != null) {
                // A library function of declared result (Infer#NATIVE_RET),
                // called as any function and its answer unboxed.
                return new E(unbox(genericCall(box(expr(c.func)), c).c, nr), nr);
            }
        }
        if (c.func instanceof Attribute && in.superCall(c) != null) {
            // `super().m(args)` as Infer resolved it: the parent's method,
            // called directly with self first.
            Infer.Fn f = in.superCall(c);
            Name self = new Name(cur.names.get(0));
            self.line = c.line;
            self.col = c.col;
            List<E> all = new ArrayList<E>();
            all.add(expr(self));
            all.addAll(args(c));
            calls.add(f);
            return result(f, typed.get(f) + "(" + argList(f, 0, all) + ")");
        }
        if (c.func instanceof Attribute) {
            Attribute a = (Attribute) c.func;
            Layouts.Cls ck = in.emitCalledClass(a.value, sc);
            boolean local = a.value instanceof Name && in.globalClasses.containsKey(((Name) a.value).id)
                    && binding(((Name) a.value).id) == Scope.Binding.GLOBAL;
            Layouts.Cls cr = ck == null ? null : Layouts.resolver(ck, a.attr);
            FunctionDef cm = cr == null ? null : cr.typable.get(a.attr);
            if (ck != null && !in.doors.contains(c) && (local || !in.open.contains(ck) && cm != null && in.emitFns().containsKey(cm))) {
                // As Infer resolved it: a class of this module, or an
                // imported one whose method is this module's. Another
                // module's method is called generically, through its door.
                Layouts.Cls k = ck;
                Layouts.Cls o = Layouts.resolver(k, a.attr);
                FunctionDef m = o == null ? null : o.typable.get(a.attr);
                if (m == null) {
                    throw new Unsupported("class attribute call");
                }
                Infer.Fn f = in.emitFns().get(m);
                if (f == null) {
                    // A method of a class merged into another's layout
                    // with no typed body of its own here: called through
                    // the MRO, generically.
                    throw new Unsupported("class attribute call " + a.attr);
                }
                calls.add(f);
                return result(f, typed.get(f) + "(" + argList(f, 0, args(c, m, 0)) + ")");
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
            if (r.t.k == Ty.K.LIST && !r.t.opt && "extend".equals(a.attr) && c.keywords.isEmpty()
                    && c.args.size() == 1 && c.args.get(0) instanceof ListExpr
                    && (((ListExpr) c.args.get(0)).elts.size() == 2 || ((ListExpr) c.args.get(0)).elts.size() == 3)
                    && !Infer.hasStarred(((ListExpr) c.args.get(0)).elts)) {
                // `l.extend([a, b, c])` (markdownify's process_tag): appended
                // in place, no list of them made.
                List<Expr> es = ((ListExpr) c.args.get(0)).elts;
                StringBuilder s = new StringBuilder("Typed.extend").append(es.size()).append('(').append(r.c);
                for (Expr x : es) {
                    s.append(", ").append(box(expr(x)));
                }
                return new E(s.append(')').toString(), Ty.NONE);
            }
            List<E> args;
            if (!c.keywords.isEmpty() && r.t.k == Ty.K.INST) {
                // One method it can be: keywords bound to its parameters,
                // as Infer bound them.
                List<Expr> b = Infer.sharedArgs(c, in.candidates(r.t.cls, a.attr));
                if (b == null) {
                    throw new Unsupported("keyword arguments");
                }
                args = exprs(b);
            } else {
                args = args(c);
            }
            if (r.t.k == Ty.K.LIST && !r.t.opt) {
                return listMethod(r, a.attr, args);
            }
            if (r.t.k == Ty.K.STR && !r.t.opt && Infer.splitCall(a.attr, types(args), c)) {
                Integer site = in.siteOf.get(c);
                if (site != null) {
                    return new E("Typed.split(" + r.c + ", " + (args.isEmpty() ? "Py.NONE" : args.get(0).c) + ")",
                            Ty.list(new int[] {site.intValue()}, false));
                }
            }
            if (Infer.codecCall(r.t, a.attr, types(args), c)) {
                String enc = args.isEmpty() ? "null" : args.get(0).c;
                String err = args.size() < 2 ? "null" : args.get(1).c;
                return "encode".equals(a.attr) ? new E("Typed.encode(" + r.c + ", " + enc + ", " + err + ")", Ty.BYTES)
                        : new E("Typed.decode(" + r.c + ", " + enc + ", " + err + ")", Ty.STR);
            }
            if (r.t.k == Ty.K.STR && !r.t.opt && c.keywords.isEmpty() && !Infer.hasStarred(c.args)) {
                E s = strMethod(r, a.attr, args);
                if (s != null) {
                    return s;
                }
                s = strBoundedDyn(r, a.attr, args);
                if (s != null) {
                    return s;
                }
            }
            if (r.t.k == Ty.K.DICT && !r.t.opt && "get".equals(a.attr) && c.keywords.isEmpty()
                    && !Infer.hasStarred(c.args) && (args.size() == 1 || args.size() == 2)) {
                E def = args.size() == 2 ? args.get(1) : null;
                Ty vt = Ty.join(in.dictVal(r.t), def == null ? Ty.NONE : def.t);
                String g = "Typed.dgetd(" + r.c + ", " + box(args.get(0)) + ", " + (def == null ? "Py.NONE" : box(def)) + ")";
                if (vt.k == Ty.K.BOT || vt.wide() || vt.opt) {
                    return new E(g, Ty.OBJECT);
                }
                if (in.guessedSite(r.t)) {
                    // Before the object case: an element of a guessed
                    // dict is a guess whatever it is held as, and a value
                    // typed an instance is unboxed unchecked downstream.
                    return guessedElem(g, vt, null, 0);
                }
                if (vt.isObject()) {
                    return new E(g, vt);
                }
                return new E(unbox(g, vt), vt);
            }
            if (r.t.k != Ty.K.INST) {
                String k = "get".equals(a.attr) && args.size() == 1 && c.keywords.isEmpty() ? intKey(args.get(0)) : null;
                if (k != null) {
                    // A plain dict looks an int key up without boxing it.
                    return new E("Typed.dget(" + em.attrSite(a.attr) + ", " + box(r) + ", " + k + ")", Ty.OBJECT);
                }
                return slotResult(c, siteCall(r, a.attr, args));
            }
            if ((in.doors.contains(c) || substObject(c)) && !methodDoorFits(r.t.cls, a.attr, c)) {
                // An argument nothing knows (Infer.callFn): the method is
                // called as generic code calls it, and its guard picks the
                // version to run.
                Ty rt = in.type(c, cur, sc);
                E id = inlineDoor(r, a, c, args, rt);
                if (id != null) {
                    return id;
                }
                if (!c.keywords.isEmpty() && !boundPositionally(r.t.cls, a.attr)) {
                    throw new Unsupported("keyword door call " + a.attr);
                }
                String g = siteCall(r, a.attr, args).c;
                if (rt.k != Ty.K.INT && in.guessed(c)) {
                    return guessedElem(g, rt, null, 0);
                }
                return rt.wide() || rt.k == Ty.K.BOT || rt.isObject() ? new E(g, rt.k == Ty.K.BOT ? Ty.OBJECT : rt)
                        : new E(unbox(g, rt), rt);
            }
            List<FunctionDef> cand = in.candidates(r.t.cls, a.attr);
            E xc = cand != null && cand.size() == 1 ? foreignCall(r, a.attr, cand.get(0), args) : null;
            if (xc != null) {
                return xc;
            }
            if (cand == null || !localRoot(r.t.cls)) {
                // A class whose compiled base is another module's has no
                // typed virtual methods of this module's: called as the
                // generic code calls it, the rest of the expression typed.
                if (!c.keywords.isEmpty() || Infer.hasStarred(c.args)) {
                    throw new Unsupported("method " + a.attr);
                }
                return slotResult(c, siteCall(r, a.attr, args));
            }
            String recv = nz(r, a.attr);
            Layouts.Cls res = Layouts.resolver(r.t.cls, a.attr);
            if (cand.size() == 1 && res != null && res.typable.get(a.attr) == cand.get(0)) { //NOPMD CompareObjectsWithEquals - identity is meant
                // Not the getter's field in place of the call: a class built at
                // run time may override the getter (PyType.borrowLayout), which
                // the bridge asks about first.
                Infer.Fn f = in.emitFns().get(cand.get(0));
                calls.add(f);
                bridged.add(f);
                patchedSite(f.def.name);
                String al = argList(f, 1, args);
                return result(f, "pt_" + typed.get(f) + "(" + recv + (al.isEmpty() ? "" : ", " + al) + ")");
            }
            String key = a.attr + "/" + args.size();
            List<Infer.Fn> g = groups.get(key);
            if (g == null) {
                throw new Unsupported("no group " + key);
            }
            for (FunctionDef m : cand) {
                if (!g.contains(in.emitFns().get(m))) {
                    throw new Unsupported("arity mismatch " + key);
                }
            }
            callsGroups.add(key);
            patchedSite(a.attr);
            Infer.Fn f = g.get(0);
            String al = argList(f, 1, args);
            return result(f, recv + ".tv_" + Emitter.mangle(a.attr) + "_" + args.size() + "(" + al + ")");
        }
        return genericCall(box(expr(c.func)), c);
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
    /// A call dispatched by name (`g`) as the typed value Infer proved it
    /// returns: a method name only program methods answer returns what
    /// they return (Infer#closedName), whatever the receiver.
    /// A call of a Java builtin the pass types ([BuiltinSigs]): its generic
    /// result, converted (checked) to that type.
    private E builtinResult(Call c, E g) {
        if (!g.t.wide() || !in.builtinCalls.contains(c)) {
            return g;
        }
        Ty rt = in.type(c, cur, sc);
        if (rt.wide() || rt.k == Ty.K.BOT) {
            return g;
        }
        return rt.isObject() ? new E(box(g), rt) : new E(unbox(box(g), rt), rt);
    }

    private E slotResult(Call c, E g) {
        if (!in.slotCall(c)) {
            return g;
        }
        Ty rt = in.type(c, cur, sc);
        if (rt.wide() || rt.k == Ty.K.BOT) {
            return g;
        }
        return rt.isObject() ? new E(g.c, rt) : new E(unbox(g.c, rt), rt);
    }

    private E siteCall(E r, String attr, List<E> args) {
        // An instance of a known class too: a door call (an argument of no
        // known type) on a typed `self` reached the method through the
        // inline cache, where the generic body's guarded dispatch calls it
        // directly (bs4's `SoupStrainer.match` calling `self.matches_tag`).
        if (r.t.wide() || r.t.k == Ty.K.INST) {
            StringBuilder da = new StringBuilder();
            for (E x : args) {
                da.append(", ").append(box(x));
            }
            String d = em.dispatchCall(attr, args.size(), box(r), da.toString());
            if (d != null) {
                return new E(d, Ty.OBJECT);
            }
        }
        String site = em.attrSite(attr);
        StringBuilder a = new StringBuilder(box(r));
        for (E x : args) {
            a.append(", ").append(box(x));
        }
        if (args.size() <= 13) {
            return new E(site + ".call" + args.size() + "(" + a + ")", Ty.OBJECT);
        }
        StringBuilder arr = new StringBuilder();
        for (E x : args) {
            arr.append(arr.length() == 0 ? "" : ", ").append(box(x));
        }
        return new E(site + ".callN(" + box(r) + ", new Object[] {null, " + arr + "})", Ty.OBJECT);
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

    /// `r.attr` read generically: behind the program's field guard when
    /// `r` is held as a plain Object (`Emitter.guardedGet`), else the site.
    private String attrGet(E r, String attr) {
        if (r.t.wide()) {
            String s = shadowGet(r.c, attr);
            return s != null ? s : em.guardedGet(attr, box(r));
        }
        return em.attrSite(attr) + ".get(" + box(r) + ")";
    }

    /// Whether call `c` of `f` passes, positionally, exactly what the typed
    /// version takes: each argument where a parameter of type OBJECT (an
    /// Object the version accepts as it is) stands, or of the parameter's
    /// own type. Such a call needs no door (Infer#callFn sends a call with
    /// a OBJECT argument through the function object so its guard can pick a
    /// version), and markdownify's `should_remove_whitespace_inside(node)`
    /// paid a generic call and a guard for a version taking Object.
    private boolean doorFits(Infer.Fn f, Call c, int from) {
        if (typed.get(f) == null && in.emitFns().containsKey(f.def) || !c.keywords.isEmpty() || c.args.size() + from != f.params.length
                || Infer.hasStarred(c.args)) {
            return false;
        }
        for (int i = 0; i < c.args.size(); i++) {
            Ty p = f.params[from + i];
            // A guess (a guarded field, a guessed element) is of its type
            // only when its check passes: the typed version is entered
            // through the guard, never a checked conversion that raises
            // where the guess was wrong. yaml's `_analyze_scalar(node.value)`
            // converted a str to a list.
            if (!p.wide() && (!p.equals(in.type(c.args.get(i), cur, sc)) || objectSubst(c.args.get(i))
                    || in.guessed(c.args.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /// Whether the version of an expression being written holds `x` as an
    /// object: the generic half of a split over a guess (see [#split]),
    /// taken when the guess failed, so the value is of no type it vouches for.
    private boolean objectSubst(Expr x) {
        E s = leafSubst.get(x);
        return s != null && s.t.wide();
    }

    /// Whether any argument of `c` is such an object ([#objectSubst]): a
    /// typed callee is then entered through its guard, never a checked
    /// conversion that raises where the guess was wrong.
    private boolean substObject(Call c) {
        for (Expr x : c.args) {
            if (objectSubst(x instanceof Starred ? ((Starred) x).value : x)) {
                return true;
            }
        }
        for (Keyword kw : c.keywords) {
            if (objectSubst(kw.value)) {
                return true;
            }
        }
        return false;
    }

    /// Whether the typed `__init__` of `k` takes `c`'s arguments as written.
    private boolean initFits(Layouts.Cls k, Call c) {
        Layouts.Cls io = Layouts.resolver(k, "__init__");
        FunctionDef d = io == null ? null : io.typable.get("__init__");
        Infer.Fn f = d == null ? null : in.anyFn(d);
        return f != null && !in.doors.contains(c) && doorFits(f, c, 1);
    }

    /// [#doorFits] for every method a call of `attr` on a `cls` may run.
    /// A door call (see Infer.callFn) of the one method `r.attr` can run,
    /// with the guard in line: each argument of no known type is checked
    /// against its parameter, the typed method called when every one fits
    /// and the generic call made when not. Through the method's own guard
    /// every call of a recursion over values the build cannot type was a
    /// dictionary dispatch (markdownify's process_tag over its children).
    /// Null when it cannot be written so.
    private E inlineDoor(E r, Attribute a, Call c, List<E> args, Ty rt) {
        if (pre == null || !stmtPure || condDepth != 0 || r.t.k != Ty.K.INST || !plain(r.c)
                || Infer.hasStarred(c.args)) {
            return null;
        }
        List<FunctionDef> cand = in.candidates(r.t.cls, a.attr);
        Layouts.Cls res = Layouts.resolver(r.t.cls, a.attr);
        if (cand == null || cand.size() != 1 || res == null || res.typable.get(a.attr) != cand.get(0) //NOPMD CompareObjectsWithEquals - identity is meant
                || !localRoot(r.t.cls) || !boundPositionally(r.t.cls, a.attr)) {
            return null;
        }
        Infer.Fn f = in.emitFns().get(cand.get(0));
        if (f == null || typed.get(f) == null || f.ret.k == Ty.K.INT || args.size() + 1 != f.params.length) {
            return null;
        }
        StringBuilder cond = new StringBuilder();
        List<E> typedArgs = new ArrayList<E>(args.size());
        for (int i = 0; i < args.size(); i++) {
            E x = args.get(i);
            Ty pt = f.params[1 + i];
            boolean need = !pt.wide() && pt.k != Ty.K.BOT && (x.t.wide() || x.gview != null || objectSubst(c.args.size() > i ? c.args.get(i) : null));
            if (!need) {
                if (!plain(x.c) && !literal(x.c) && !x.c.startsWith("Py.") || x.t.k == Ty.K.INT && x.ll != null) {
                    return null;
                }
                typedArgs.add(x);
                continue;
            }
            if (pt.k == Ty.K.INT || pt.k == Ty.K.FLOAT) {
                return null;
            }
            String o = fresh();
            indent(pre, preInd);
            pre.append("Object ").append(o).append(" = ").append(box(x)).append(";\n");
            cond.append(cond.length() == 0 ? "" : " && ").append(fits(o, pt));
            typedArgs.add(new E(o, Ty.OBJECT));
            args = new ArrayList<E>(args);
            args.set(i, new E(o, Ty.OBJECT));
            typedArgs.set(i, new E(unbox(o, pt), pt));
        }
        if (cond.length() == 0) {
            return null;
        }
        calls.add(f);
        bridged.add(f);
        patchedSite(f.def.name);
        E t = result(f, "pt_" + typed.get(f) + "(" + r.c + (typedArgs.isEmpty() ? "" : ", " + argList(f, 1, typedArgs)) + ")");
        String g = siteCall(r, a.attr, args).c;
        String both = "(" + cond + " ? " + box(t) + " : " + g + ")";
        return rt.wide() || rt.k == Ty.K.BOT || rt.isObject() ? new E(both, rt.k == Ty.K.BOT ? Ty.OBJECT : rt)
                : new E(unbox(both, rt), rt);
    }

    /// Whether every method `cls.attr` may be takes only positional
    /// parameters: a call with keywords, bound to them at build time, can
    /// then be made generically with the bound list.
    private boolean boundPositionally(Layouts.Cls cls, String attr) {
        List<FunctionDef> cand = in.candidates(cls, attr);
        if (cand == null) {
            return false;
        }
        for (FunctionDef m : cand) {
            if (!m.args.kwonly.isEmpty() || m.args.vararg != null || m.args.kwarg != null) {
                return false;
            }
        }
        return true;
    }

    private boolean methodDoorFits(Layouts.Cls cls, String attr, Call c) {
        List<FunctionDef> cand = in.candidates(cls, attr);
        if (cand == null || cand.isEmpty() || !localRoot(cls) && cand.size() != 1) {
            return false;
        }
        for (FunctionDef m : cand) {
            Infer.Fn f = in.anyFn(m);
            if (f == null || !doorFits(f, c, 1)) {
                return false;
            }
        }
        return true;
    }

    /// Calls of other modules' typed methods, by marker (see [#foreignCall]).
    private final Map<String, String[]> foreign = new HashMap<String, String[]>();
    private final Map<String, Infer.Fn> foreignFn = new HashMap<String, Infer.Fn>();
    private final Map<String, TypedEmit> foreignHome = new HashMap<String, TypedEmit>();
    /// Markers are written and resolved by the same emitter (#members), so
    /// they need to be unique only within it.
    private int foreignIds;

    /// `r.attr(args)` where the method is `m`, defined by a base class of
    /// another module and the only one an instance can run
    /// (Infer#candidates): that module's typed copy, called directly when
    /// the class was not patched at run time. Whether that module kept a
    /// typed copy is known only once every module is planned, so the call
    /// is a marker until [#members] writes it, else the generic call it
    /// stands for -- its result held as the typed copy returns it, which
    /// the generic body returns too for the same arguments.
    private E foreignCall(E r, String attr, FunctionDef m, List<E> args) {
        Layouts.Cls res = Layouts.resolver(r.t.cls, attr);
        Infer.Fn f = in.anyFn(m);
        if (res == null || res.home == in.layouts || res.typed == null || f == null || f.guessRet != null //NOPMD CompareObjectsWithEquals - identity is meant
                || res.typable.get(attr) != m || r.t.opt) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        String recv = r.c;
        if (!plain(recv)) {
            if (pre == null || !stmtPure || condDepth != 0) {
                return null;
            }
            String tv = fresh();
            indent(pre, preInd);
            pre.append(jt(r.t)).append(' ').append(tv).append(" = ").append(recv).append(";\n");
            recv = tv;
        }
        E rv = new E(recv, r.t);
        String al = argList(f, 1, args);
        String g = siteCall(rv, attr, args).c;
        Ty rt = f.ret.k == Ty.K.BOT ? Ty.OBJECT : f.ret;
        String fb = rt.k == Ty.K.INT ? "Typed.retObj(" + g + ")" : "Object".equals(jt(rt)) ? g : unbox(g, rt);
        String id = "XC" + (foreignIds++) + "_";
        String cls = res.ref.substring(0, res.ref.lastIndexOf('.'));
        foreign.put(id, new String[] {recv, cls, al, fb});
        foreignFn.put(id, f);
        foreignHome.put(id, res.typed);
        return result(f, "/*" + id + "*/" + fb + "/*" + id + "*/");
    }

    /// `b` with every marker of [#foreignCall] written: the direct call
    /// where the defining module kept its typed copy, else the generic one.
    private String resolveForeign(String b) {
        if (foreign.isEmpty() || b.indexOf("/*XC") < 0) {
            return b;
        }
        StringBuilder o = new StringBuilder(b.length() + 64);
        int i = 0;
        while (true) {
            int s = b.indexOf("/*XC", i);
            if (s < 0) {
                break;
            }
            int e = b.indexOf("*/", s);
            String id = b.substring(s + 2, e);
            String close = "/*" + id + "*/";
            int end = b.indexOf(close, e + 2);
            String[] x = foreign.get(id);
            String name = foreignHome.get(id).typed.get(foreignFn.get(id));
            o.append(b, i, s);
            // An argument may hold another such call, written the same way.
            String fb = resolveForeign(x[3]);
            if (name == null) {
                o.append(fb);
            } else {
                String al = resolveForeign(x[2]);
                o.append('(').append(x[0]).append(".cls.patched ? ").append(fb).append(" : ").append(x[1]).append('.')
                        .append(name).append('(').append(x[0]).append(al.isEmpty() ? "" : ", " + al).append("))");
            }
            i = end + close.length();
        }
        return o.append(b, i, b.length()).toString();
    }

    /// Per typed function: its locals and the class their field reads name
    /// most (`ShadowTypes`, as the generic body keeps a typed copy of).
    private final Map<Infer.Fn, Map<String, Layouts.Cls>> shadows = new HashMap<Infer.Fn, Map<String, Layouts.Cls>>();

    /// `v.attr` for the plain local `v` of no known type whose field reads
    /// point at one class: that class's field behind an `instanceof`, the
    /// generic read otherwise. A field read past every guard the program
    /// can afford (`bs4`'s `Tag.name`, a name a dozen classes declare)
    /// otherwise always went through the site -- markdownify's `node`.
    private String shadowGet(String v, String attr) {
        if (cur == null || cur.def.body == null || !v.startsWith("v_")) {
            return null;
        }
        Map<String, Layouts.Cls> sh = shadows.get(cur);
        if (sh == null) {
            sh = new HashMap<String, Layouts.Cls>();
            for (Map.Entry<String, Layouts.Cls> e : ShadowTypes.of(cur.def.body, em.fieldOwners()).entrySet()) {
                sh.put(var(e.getKey()), e.getValue());
            }
            shadows.put(cur, sh);
        }
        Layouts.Cls k = sh.get(v);
        List<Layouts.Cls> os = k != null ? em.fieldOwners().get(attr) : null;
        Layouts.Cls own = null;
        for (Layouts.Cls a = k; a != null && os != null && own == null; a = a.parent) {
            if (os.contains(a)) {
                own = a;
            }
        }
        if (own == null) {
            // The local's class has no such field (bs4's `Tag.children` is
            // a property): the program's guards would test classes it is
            // not (pdfplumber's `PDFStructTree`), each a miss.
            return k != null ? em.attrSite(attr) + ".get(" + v + ")" : null;
        }
        String cast = "((" + k.ref + ") " + v + ")";
        String site = em.attrSite(attr) + ".get(" + v + ")";
        String typedF = own.typed != null ? own.typed.foreignGet(own, attr, cast) : null;
        if (typedF != null) {
            return "(" + v + " instanceof " + k.ref + " ? " + typedF + " : " + site + ")";
        }
        String f = cast + ".f_" + Emitter.mangle(attr);
        return "(" + v + " instanceof " + k.ref + " && " + f + " != null ? " + f + " : " + site + ")";
    }

    /// A call's result known, at this call site, to be `t` (`Infer.siteRet`)
    /// though the function returns something wider.
    private E relabel(E r, Ty t) {
        switch (t.k) {
            case INT:
                return new E(r.t.wide() ? r.c : box(r), Ty.INT);
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
            String t = testExpr(x.test);
            // Each branch runs only when chosen: nothing of either may be
            // hoisted ahead of the test.
            condDepth++;
            String a;
            String b;
            try {
                a = mod64(x.body);
                b = mod64(x.orelse);
            } finally {
                condDepth--;
            }
            return a == null || b == null ? null : "(" + t + " ? " + a + " : " + b + ")";
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
        switch (binding(n)) {
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
        /// Attributes a miss probe or a `getattr` with a default asks for.
        final Set<String> probes = new HashSet<String>();
        /// The probed ones no field, class or base defines: each answers a
        /// miss while the class still lacks it, and rebuilds the object
        /// from every field when it does not.
        final Set<String> missing = new HashSet<String>();
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
        for (String p : x.probes) {
            if (fields.contains(p)) {
                x.reads.add(p);
            } else if (p.startsWith("__") || Layouts.resolver(x.k, p) != null) {
                return false;
            } else {
                x.missing.add(p);
            }
        }
        if (!x.missing.isEmpty()) {
            x.reads.addAll(fields);
        }
        for (String g : x.getters) {
            Layouts.Cls o = Layouts.resolver(x.k, g);
            FunctionDef d = o == null ? null : o.typable.get(g);
            Infer.Fn gf = d == null ? null : in.emitFns().get(d);
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
        } else if (o instanceof Call && scalarProbe((Call) o, cand, bad)) {
            return;
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

    /// A miss probe (`$probe(n.a)`, `$unmiss(m, n.a)`) or `getattr(n, "a", d)`
    /// on candidate `n`: records `a` as probed and scans the rest. False,
    /// scanning nothing, when the call is none of those.
    private boolean scalarProbe(Call c, Map<String, Scalar> cand, Set<String> bad) {
        Expr recv;
        String attr;
        List<Expr> rest = new ArrayList<Expr>();
        String fn = c.func instanceof Name ? ((Name) c.func).id : "";
        if ("$probe".equals(fn) || "$unmiss".equals(fn)) {
            Expr x = c.args.get(c.args.size() - 1);
            if (!(x instanceof Attribute)) {
                return false;
            }
            recv = ((Attribute) x).value;
            attr = ((Attribute) x).attr;
            rest.addAll(c.args.subList(0, c.args.size() - 1));
        } else if (Emitter.getattrDefault(c) && binding("getattr") == Scope.Binding.BUILTIN
                && !em.moduleScopeHasStarImport()) {
            recv = c.args.get(0);
            attr = ((Constant) c.args.get(1)).text;
            rest.add(c.args.get(2));
        } else {
            return false;
        }
        if (!(recv instanceof Name) || !cand.containsKey(((Name) recv).id)) {
            return false;
        }
        if (da.safe.contains((Name) recv)) {
            cand.get(((Name) recv).id).probes.add(attr);
        } else {
            bad.add(((Name) recv).id);
        }
        for (Expr x : rest) {
            scanScalars(x, cand, bad);
        }
        return true;
    }

    /// `n.attr` probed (`probe`) or read on scalar local `n`.
    private String scalarProbeRead(String n, String attr, boolean probe) {
        Scalar x = scalars.get(n);
        if (!x.missing.contains(attr)) {
            return box(scalarRead(n, attr));
        }
        List<E> fs = new ArrayList<E>();
        for (String a : x.attrs) {
            fs.add(scalarRead(n, a));
        }
        String site = em.attrSite(attr);
        String obj = construct(x.k, fs).c;
        if (!probe) {
            return site + ".get(" + obj + ")";
        }
        return "(" + site + ".classLacks(" + x.k.typeVar + ") ? Py.MISS : " + site + ".getOrMiss(" + obj + "))";
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
                || binding(((Name) c.func).id) != Scope.Binding.GLOBAL) {
            return false;
        }
        Layouts.Cls k = in.emitClass(((Name) c.func).id);
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
        Infer.Fn f = init == null ? null : in.emitFns().get(init);
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

    /// The unboxer of class `k` this module writes (see [#unbox]): named
    /// after the class alone for one of its own, after the qualified name
    /// for another module's.
    private String unboxerName(Layouts.Cls k) {
        return k.home == in.layouts ? "u_" + k.javaName : "u_" + boxerId(clsJ(k)); //NOPMD CompareObjectsWithEquals - identity is meant
    }

    private boolean localRoot(Layouts.Cls k) {
        for (Layouts.Cls x = k; x != null; x = x.parent) {
            if (x.home != in.layouts) { //NOPMD CompareObjectsWithEquals - identity is meant
                return false;
            }
        }
        return true;
    }

    private E construct(Layouts.Cls k, List<E> args) {
        if (k.callConstructed()) {
            // BaseException's `__new__` fills `args` from the arguments
            // before any `__init__` runs: the class is called, as generic
            // code calls it (Infer types the result as any object).
            StringBuilder boxed = new StringBuilder();
            for (E a : args) {
                boxed.append(", ").append(box(a));
            }
            return new E("Py.call(" + k.typeVar + boxed + ")", Ty.OBJECT);
        }
        if (in.newMethod(k) != null) {
            // Its own `__new__` makes the object: the class is called, as
            // the generic code calls it. Inference types the result from
            // what `__new__` returns.
            StringBuilder boxed = new StringBuilder();
            for (E a : args) {
                boxed.append(", ").append(box(a));
            }
            return new E("Py.call(" + k.typeVar + boxed + ")", Ty.OBJECT);
        }
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        if (o == null && k.builtin != null) {
            // The builtin's __new__ makes the value of the arguments.
            StringBuilder boxed = new StringBuilder();
            for (E a : args) {
                boxed.append(", ").append(box(a));
            }
            return new E("Typed.withBase(new " + k.javaName + "(" + k.typeVar + "), " + builtinType(k.builtin)
                    + ", false" + boxed + ")", Ty.inst(k, false));
        }
        if (o == null) {
            if (!args.isEmpty()) {
                throw new Unsupported("arguments to object()");
            }
            return new E("new " + k.javaName + "(" + k.typeVar + ")", Ty.inst(k, false));
        }
        FunctionDef init = o.typable.get("__init__");
        if (init == null || o.home != k.home) { //NOPMD CompareObjectsWithEquals - identity is meant
            // An __init__ compiled in another module has no typed body here.
            throw new Unsupported("__init__");
        }
        Infer.Fn f = in.emitFns().get(init);
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
            case "min":
            case "max":
                if (a.size() == 2) {
                    return new E("Typed." + n + "2(" + em.builtinRef(n) + ", " + box(a.get(0)) + ", " + box(a.get(1)) + ")",
                            Ty.OBJECT);
                }
                break;
            case "set":
                if (a.isEmpty()) {
                    return new E("Typed.set(" + em.builtinRef("set") + ")", Ty.OBJECT);
                }
                if (a.size() == 1 && (a.get(0).t.wide() || a.get(0).t.k == Ty.K.LIST
                        || a.get(0).t.k == Ty.K.DICT || a.get(0).t.k == Ty.K.TUPLE
                        || a.get(0).t.k == Ty.K.STR && !a.get(0).t.opt)) {
                    // Every one of those kinds is already an Object.
                    return new E("Typed.set(" + em.builtinRef("set") + ", " + a.get(0).c + ")", Ty.OBJECT);
                }
                break;
            case "len":
                if (a.size() == 1 && a.get(0).t.k == Ty.K.LIST) {
                    a = new ArrayList<E>(a);
                    a.set(0, heldList(a.get(0)));
                    // The list read through a call stays in the long: every use
                    // of a length reads it once.
                    String l = "Typed.llen(" + a.get(0).c + ")";
                    return small(new E("Long.valueOf(" + l + ")", Ty.INT, l));
                }
                if (a.size() == 1 && a.get(0).t.k == Ty.K.STR && !a.get(0).t.opt) {
                    String l = "Typed.slen(" + a.get(0).c + ")";
                    return small(new E("Long.valueOf(" + l + ")", Ty.INT, l));
                }
                if (a.size() == 1 && a.get(0).t.wide()) {
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

    /// The class object of compiled class `k`, from this module.
    String classRef(Layouts.Cls k) {
        if (k.home == in.layouts) { //NOPMD CompareObjectsWithEquals - identity is meant
            return k.typeVar;
        }
        return k.ref.substring(0, k.ref.length() - k.javaName.length() - 1) + "." + k.typeVar;
    }

    /// The runtime's type object of the builtin `n` a compiled class extends.
    static String builtinType(String n) {
        return "Py." + n.toUpperCase(java.util.Locale.ROOT) + "_TYPE";
    }

    /// The builtins `builtin` lowers; any other is called as Python calls it.
    private static final Set<String> TYPED_BUILTINS = new HashSet<String>(Arrays.asList("len", "float", "str", "ord",
            "abs", "set", "min", "max"));

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
            return new E("Py.call(" + f + (a.length() == 0 ? "" : ", " + a) + ")", Ty.OBJECT);
        }
        StringBuilder names = new StringBuilder();
        for (Keyword kw : c.keywords) {
            if (kw.arg == null) {
                throw new Unsupported("** argument");
            }
            a.append(a.length() == 0 ? "" : ", ").append(box(expr(kw.value)));
            names.append(names.length() == 0 ? "" : ", ").append(Emitter.javaString(kw.arg));
        }
        // Hoisted: the callee caches its keyword binding by the array's identity.
        String w = em.hoist("String[]", "new String[] {" + names + "}");
        if (c.args.size() + c.keywords.size() <= 14) {
            return new E("Py.callKwF(" + f + ", " + w + ", " + a + ")", Ty.OBJECT);
        }
        return new E("Py.callKw(" + f + ", new Object[] {" + a + "}, " + w + ")", Ty.OBJECT);
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

    /// A str method called on a str: the runtime helper directly, for the
    /// shapes [Infer#strMethod] types, so the result type agrees.
    /// `s.find(sub, i)` (and `rfind`, `startswith`, `endswith`) on a str
    /// with a start nothing knows the type of -- html.parser's
    /// `rawdata.find('<', i)`, where `i` is what `updatepos` returned: the
    /// typed search when the start is an int, else the generic call, which
    /// raises or runs `__index__` as the builtin does.
    private E strBoundedDyn(E r, String m, List<E> a) {
        boolean find = "find".equals(m) || "rfind".equals(m);
        if (!find && !"startswith".equals(m) && !"endswith".equals(m) || a.size() != 2
                || a.get(0).t.k != Ty.K.STR || a.get(0).t.opt || !a.get(1).t.wide()
                || !plain(r.c) || !plain(a.get(1).c) || !plain(a.get(0).c) && !a.get(0).c.startsWith("\"")) {
            return null;
        }
        String i = a.get(1).c;
        String fast = (find ? "Typed.find(" : "Typed.affix(") + r.c + ", " + a.get(0).c + ", ((Long) " + i
                + ").longValue(), " + (find ? "rfind".equals(m) : "startswith".equals(m)) + ")";
        String slow = siteCall(r, m, a).c;
        return find ? new E("(" + i + " instanceof Long ? Long.valueOf(" + fast + ") : " + slow + ")", Ty.OBJECT)
                : new E("(" + i + " instanceof Long ? " + fast + " : Py.isTrue(" + slow + "))", Ty.BOOL);
    }

    private E strMethod(E r, String m, List<E> a) {
        List<Ty> ts = new ArrayList<>();
        for (E x : a) {
            ts.add(x.t);
        }
        Ty t = Infer.strMethod(m, ts);
        if (t == null) {
            return null;
        }
        String v = r.c;
        String c;
        if (a.isEmpty() && Infer.isAllKind(m) >= 0) {
            return new E("Typed.isAll(" + v + ", " + Infer.isAllKind(m) + ")", Ty.BOOL);
        }
        if (a.size() >= 2 && !"replace".equals(m)) {
            String bounds = ", " + box(a.get(1)) + ", " + (a.size() == 3 ? box(a.get(2)) : "Py.NONE") + ", ";
            String fn = t.k == Ty.K.BOOL ? "Typed.affix(" : "Typed.find(";
            String tail = (t.k == Ty.K.BOOL ? "startswith".equals(m) : "rfind".equals(m)) + ")";
            c = fn + v + ", " + a.get(0).c + bounds + tail;
            String start = a.size() == 2 ? a.get(1).lc : null;
            if (start != null) {
                // The start as the long it is: nothing boxed.
                c = fn + v + ", " + a.get(0).c + ", " + start + ", " + tail;
            } else if (a.size() == 2 && a.get(1).ll != null) {
                // Either way the receiver and the text are evaluated once,
                // after a test that reads a local of this frame only.
                c = "(" + a.get(1).bg + " == null ? " + fn + v + ", " + a.get(0).c + ", " + a.get(1).ll + ", " + tail
                        + " : " + c + ")";
            }
            return t.k == Ty.K.BOOL ? new E(c, Ty.BOOL) : small(new E("Long.valueOf(" + c + ")", t, c));
        }
        switch (m) {
            case "strip":
            case "lstrip":
            case "rstrip":
                c = "Typed.strip(" + v + (a.isEmpty() ? "" : ", " + a.get(0).c) + ", " + !"rstrip".equals(m)
                        + ", " + !"lstrip".equals(m) + ")";
                break;
            case "lower":
            case "upper":
                c = "Typed.mapCase(" + v + ", " + ("upper".equals(m) ? 1 : 0) + ")";
                break;
            case "replace":
                c = "Typed.replace(" + v + ", " + a.get(0).c + ", " + a.get(1).c + ")";
                break;
            case "find":
            case "rfind":
                c = "Typed.find(" + v + ", " + a.get(0).c + ", " + "rfind".equals(m) + ")";
                break;
            case "count":
                c = "Typed.count(" + v + ", " + a.get(0).c + ")";
                break;
            case "startswith":
                c = v + ".startsWith(" + a.get(0).c + ")";
                break;
            case "endswith":
                c = v + ".endsWith(" + a.get(0).c + ")";
                break;
            default:
                c = "Typed.join(" + v + ", " + box(a.get(0)) + ")";
                break;
        }
        return t.k == Ty.K.INT ? small(new E("Long.valueOf(" + c + ")", t, c)) : new E(c, t);
    }

    /// `e`, marked as an int that fits a Java `int` (see `E.small`).
    private static E small(E e) {
        e.small = true;
        return e;
    }

    private static List<Ty> types(List<E> a) {
        List<Ty> ts = new ArrayList<>();
        for (E x : a) {
            ts.add(x.t);
        }
        return ts;
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
                    if (el.k != Ty.K.INT && in.guessedSite(l.t)) {
                        return new E(c, Ty.OBJECT);
                    }
                    return new E(unbox(c, el), el.k == Ty.K.BOT ? Ty.OBJECT : el);
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
                    check = t.opt ? "(" + v + " instanceof " + clsJ(t.cls) + " || " + v + " == Py.NONE)"
                            : v + " instanceof " + clsJ(t.cls);
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
                case DICT:
                    check = "Typed.isDict(" + v + ")";
                    arg = v;
                    break;
                case CLS: {
                    // One of these classes, each with its layout: what
                    // `__new__` builds of it is then an instance typed code
                    // reads as fields.
                    StringBuilder ks = new StringBuilder();
                    for (Layouts.Cls k : in.classesOf(t)) {
                        ks.append(", ").append(classRef(k));
                    }
                    check = t.sites.length == 0 ? "(" + v + " instanceof PyType)" : "Typed.laidOut(" + v + ks + ")";
                    arg = v;
                    break;
                }
                case BYTES:
                    check = v + " instanceof PyBytes";
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
        Layouts.Cls root = k;
        while (root.parent != null) {
            root = root.parent;
        }
        if (root.home != k.home) { //NOPMD CompareObjectsWithEquals - identity is meant
            // A chain rooted in another module gets no plain constructor
            // (the class declaration in Emitter skips it), so no call names one.
            return null;
        }
        if (k.builtin != null || k.callConstructed()) {
            // Its value comes from the builtin's __new__ (see construct).
            return null;
        }
        for (String h : new String[] {"__new__", "__setattr__", "__getattribute__", "__getattr__", "__del__"}) {
            if (Layouts.resolver(k, h) != null) {
                return null;
            }
        }
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        FunctionDef init = o == null ? null : o.typable.get("__init__");
        Infer.Fn f = init == null ? null : in.emitFns().get(init);
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

    /// `self.attr` where `attr` resolves to a once-bound class attribute
    /// with a typed static (Infer#classStatic) and no compiled class at or
    /// below the receiver's binds it again: the static, while the instance
    /// is of one of those compiled classes exactly and nothing wrote to that
    /// class since it was built; the generic read, unboxed, otherwise (a
    /// class built at run time may bind the name in its own body). Null when
    /// it does not apply.
    private E instanceStatic(E r, String attr) {
        Layouts.Cls owner = Layouts.resolver(r.t.cls, attr);
        Ty t = owner == null ? null : in.classStatic(owner, attr);
        if (t == null) {
            return null;
        }
        StringBuilder g = new StringBuilder();
        for (Layouts.Cls c : in.selfAndBelow(r.t.cls)) {
            if (!c.compiled) {
                // Refused a layout: its instances are never typed as these.
                continue;
            }
            if (Layouts.resolver(c, attr) != owner) { //NOPMD CompareObjectsWithEquals - identity is meant
                return null;
            }
            String tv = em.typeVarRef(c);
            g.append(g.length() == 0 ? "" : " || ").append(r.c).append(".cls == ").append(tv).append(" && !")
                    .append(tv).append(".classWritten");
        }
        if (g.length() == 0) {
            return null;
        }
        if (t.k == Ty.K.INT) {
            return new E("((" + g + ") ? " + staticE(em.classStaticRef(owner, attr), t).c + " : " + attrGet(r, attr) + ")", t);
        }
        return new E("((" + g + ") ? " + em.classStaticRef(owner, attr) + " : " + unbox(attrGet(r, attr), t) + ")", t);
    }

    /// The read of a typed class static named `ref` (Emitter#classStatics):
    /// an int is its two-part pair.
    private static E staticE(String ref, Ty t) {
        return t.k == Ty.K.INT ? dual(ref + "_l", ref + "_b") : new E(ref, t);
    }

    String fieldDecl(Layouts.Cls k, String f) {
        Ty t = typedField(k, f);
        if (t != null && (t.k == Ty.K.INT || guardedField(k, f))) {
            return (t.k == Ty.K.INT ? "long" : jt(t)) + " t_" + Emitter.mangle(f)
                    + ";\n        Object b_" + Emitter.mangle(f) + (openField(k, f) ? " = Typed.UNSET;" : ";");
        }
        return t == null ? null : jt(t) + " t_" + Emitter.mangle(f) + ";";
    }

    String fieldGetCase(Layouts.Cls k, String f) {
        Ty t = typedField(k, f);
        if (t.k == Ty.K.INT) {
            return dual("t_" + Emitter.mangle(f), "b_" + Emitter.mangle(f)).c;
        }
        if (guardedField(k, f)) {
            String m = Emitter.mangle(f);
            // An absent field answers null, as a plain field never set does.
            return pairE("t_" + m, "b_" + m, t, openField(k, f) ? "(b_" + m + " == Typed.UNSET ? null : b_" + m + ")"
                    : null).c;
        }
        return box(new E("t_" + Emitter.mangle(f), t));
    }

    String fieldSetCase(Layouts.Cls k, String f, String v) {
        Ty t = typedField(k, f);
        if (guardedField(k, f) && t.k != Ty.K.INT) {
            StringBuilder b = new StringBuilder();
            fitInto(v, t, "t_" + Emitter.mangle(f), "b_" + Emitter.mangle(f), b, 5);
            return b.toString().trim();
        }
        if (t.k == Ty.K.INT) {
            String m = Emitter.mangle(f);
            return "if (" + v + " instanceof Long) {\n                        t_" + m + " = ((Long) " + v
                    + ").longValue();\n                        b_" + m + " = null;\n                    } else {\n"
                    + "                        b_" + m + " = " + v + ";\n                    }";
        }
        return "t_" + Emitter.mangle(f) + " = " + unbox(v, t) + ";";
    }

    // ---- guessed values (Infer#guessed) ---------------------------------

    /// Whether field `f` of `k` is held typed and guarded: a typed slot and
    /// beside it the object that replaces it (see Infer.Program#unknownStores).
    boolean guardedField(Layouts.Cls k, String f) {
        Ty t = typedField(k, f);
        if (t == null) {
            return false;
        }
        return in.guarded(k, f) || openField(k, f);
    }

    /// A guessed value of type `t` held in plain Java names `r` and `o`.
    private E pairE(String r, String o, Ty t) {
        return pairE(r, o, t, null);
    }

    /// [#pairE] whose object half, when not null, is read as `present`
    /// (see [#present]); the test for the typed slot reads `o` itself.
    private E pairE(String r, String o, Ty t, String present) {
        if (t.k == Ty.K.INT) {
            return dual(r, o);
        }
        String ov = present != null ? present : o;
        // The object half typed as an object: a conditional of two
        // reference types in an argument is typed by its target, and a
        // typed half of PyList picked the PyList overload of what reads it.
        E e = new E("(" + o + " != null ? (Object) " + ov + " : " + box(new E(r, t)) + ")", Ty.OBJECT);
        e.gr = r;
        e.go = ov;
        e.gt = t;
        e.gg = o + " == null";
        e.gview = new E(r, t);
        e.ga = e.c;
        e.gstable = r.indexOf('.') < 0;
        return e;
    }

    /// The typed form of pair `e`, valid where its object is null.
    private static E typedOf(E e) {
        return new E(e.gr, e.gt);
    }

    /// A Java test that object `v` (a plain name) is a value of type `t`,
    /// as a slot of that type holds it.
    private String fits(String v, Ty t) {
        String none = t.nullable() ? " || " + v + " == Py.NONE" : "";
        switch (t.k) {
            case BOOL:
                return "(" + v + " == Py.TRUE || " + v + " == Py.FALSE)";
            case FLOAT:
                return v + " instanceof Double";
            case STR:
                return "(" + v + " instanceof String" + none + ")";
            case LIST:
                return "(" + v + " instanceof PyList" + none + ")";
            case INST:
                return "(" + v + " instanceof " + clsJ(t.cls) + none + ")";
            default:
                throw new Unsupported("no slot for " + t);
        }
    }

    /// Stores object `v` (a plain name) into the pair `slot`/`over` of
    /// type `t`: into the slot when it fits, the object when not.
    private void fitInto(String v, Ty t, String slot, String over, StringBuilder b, int ind) {
        if (t.k == Ty.K.INT) {
            storePair(slot, over, new E(v, Ty.OBJECT), b, ind);
            return;
        }
        b.append("if (").append(fits(v, t)).append(") {\n");
        indent(b, ind + 1);
        b.append(slot).append(" = ").append(unbox(v, t)).append(";\n");
        indent(b, ind + 1);
        b.append(over).append(" = null;\n");
        indent(b, ind);
        b.append("} else {\n");
        indent(b, ind + 1);
        b.append(over).append(" = ").append(v).append(";\n");
        indent(b, ind);
        b.append("}\n");
    }

    /// `e` converted to `to` without a check, or null where that is not
    /// exact: `e` may not be of that type, or may be None where it is not.
    private String exactConv(E e, Ty to) {
        if ("Object".equals(jt(e.t)) && e.t.k != Ty.K.NONE || e.t.opt && !to.nullable()) {
            return null;
        }
        try {
            return conv(e, to);
        } catch (Unsupported u) {
            return null;
        }
    }

    /// Stores `v` into the pair `slot`/`over` of type `t`.
    private void storeGuess(String slot, String over, Ty t, E v, StringBuilder b, int ind) {
        if (t.k == Ty.K.INT) {
            storePair(slot, over, v, b, ind);
            return;
        }
        if (v.gr != null) {
            String c = exactConv(typedOf(v), t);
            if (c != null) {
                b.append(slot).append(" = ").append(c).append(";\n");
                indent(b, ind);
                b.append(over).append(" = ").append(v.go).append(";\n");
                return;
            }
        }
        String c = exactConv(v, t);
        if (c != null) {
            b.append(slot).append(" = ").append(c).append(";\n");
            indent(b, ind);
            b.append(over).append(" = null;\n");
            return;
        }
        String vc = v.gview != null && v.go == null ? exactConv(v.gview, t) : null;
        if (vc != null) {
            // The typed version's value fits as it is; the other is checked.
            String o = fresh();
            b.append("if (").append(v.gg).append(") {\n");
            indent(b, ind + 1);
            b.append(slot).append(" = ").append(vc).append(";\n");
            indent(b, ind + 1);
            b.append(over).append(" = null;\n");
            indent(b, ind);
            b.append("} else {\n");
            indent(b, ind + 1);
            b.append("Object ").append(o).append(" = ").append(v.ga).append(";\n");
            indent(b, ind + 1);
            fitInto(o, t, slot, over, b, ind + 1);
            indent(b, ind);
            b.append("}\n");
            return;
        }
        String o = fresh();
        b.append("Object ").append(o).append(" = ").append(box(v)).append(";\n");
        indent(b, ind);
        fitInto(o, t, slot, over, b, ind);
    }

    /// An element `obj` of a container whose elements are guesses of type
    /// `el` (Infer#guessedSite): a pair declared in `into`, or before the
    /// statement, or the object itself where neither can be.
    private E guessedElem(String obj, Ty el, StringBuilder into, int ind) {
        if (el.k == Ty.K.INT || !Infer.guessable(el) || "Object".equals(jt(el))) {
            return new E(obj, el.k == Ty.K.INT ? el : Ty.OBJECT);
        }
        StringBuilder b = into;
        int bi = ind;
        if (b == null) {
            if (pre == null || !stmtPure || condDepth != 0) {
                return new E(obj, Ty.OBJECT);
            }
            b = pre;
            bi = preInd;
        }
        String o = fresh();
        String r = fresh();
        String ro = fresh();
        indent(b, bi);
        b.append("Object ").append(o).append(" = ").append(obj).append(";\n");
        indent(b, bi);
        b.append(jt(el)).append(' ').append(r).append(" = ").append(zero(el)).append(";\n");
        indent(b, bi);
        b.append("Object ").append(ro).append(";\n");
        indent(b, bi);
        fitInto(o, el, r, ro, b, bi);
        return pairE(r, ro, el);
    }

    /// Whether local `n` is a guessed one held as a pair: its typed Java
    /// local and the object beside it ([#over]).
    private boolean guessLocal(String n) {
        if (cur == null || !in.guessedLocal(cur, n) || longLocals.contains(n) || accLocals.contains(n)
                || trueCells.contains(n) || compVar(n) != null || scalars.containsKey(n)) {
            return false;
        }
        Ty t = cur.locals.get(n);
        return t != null && t.k != Ty.K.INT && Infer.guessable(t) && !"Object".equals(jt(t));
    }

    /// The object half of guessed local `n`.
    private static String over(String n) {
        return "x_" + Emitter.mangle(n);
    }

    /// After a write of the typed half of local `n` alone: its object half
    /// no longer replaces it.
    private void clearOver(String n, StringBuilder b, int ind) {
        if (guessLocal(n)) {
            indent(b, ind);
            b.append(over(n)).append(" = null;\n");
        }
    }

    /// Typed fields read as objects through a helper, where the receiver
    /// is an expression that may not be evaluated twice.
    /// One helper per guarded field, keyed "class TAB field": sites that
    /// type the field with and without None share it, boxing for None.
    private final Map<String, String> guardBoxers = new TreeMap<String, String>();

    /// Guarded field `attr` of `r` (see [#guardedField]).
    private E guessField(E r, String attr, Ty t) {
        Layouts.Cls o = owner(r, attr);
        String m = Emitter.mangle(attr);
        String recv = nz(r, attr);
        if (!plain(recv) && pre != null && stmtPure && condDepth == 0) {
            String tv = fresh();
            indent(pre, preInd);
            pre.append(o.ref).append(' ').append(tv).append(" = ").append(recv).append(";\n");
            recv = tv;
        }
        if (plain(recv)) {
            return pairE(recv + ".t_" + m, recv + ".b_" + m, t, present(o, attr, recv + ".b_" + m, recv));
        }
        String ob = present(o, attr, "o.b_" + m, "o");
        // The typed half is never None (None lives in the object half), but
        // sites with and without None share the helper. orNone() keeps a
        // reference kind and is OBJECT for the rest -- and box() of OBJECT writes
        // a primitive bool as it is, which Java boxed to java.lang.Boolean:
        // a value every truth test reads as true (dateutil's `dayfirst`).
        Ty bt = t.orNone().k == t.k ? t.orNone() : t.nonNone();
        guardBoxers.merge(o.ref + "\t" + m, (ob != null ? ob : "o.b_" + m) + "\t" + box(new E("o.t_" + m, bt)),
                (x, y) -> x);
        return new E("fg_" + boxerId(o.ref) + "_" + m + "(" + recv + ")", Ty.OBJECT);
    }

    /// Stores `v` into guarded field `attr` of `r`; Python evaluates the
    /// value first.
    private void storeGuardedField(E r, String attr, E v, StringBuilder b, int ind) {
        Layouts.Cls o = owner(r, attr);
        String m = Emitter.mangle(attr);
        E val = v;
        if (!plain(r.c) && val.gr == null && !(plain(val.c) || val.lc != null && plain(val.lc))) {
            String tv = fresh();
            b.append(jt(val.t)).append(' ').append(tv).append(" = ").append(val.c).append(";\n");
            indent(b, ind);
            val = new E(tv, val.t);
        }
        String recv = nz(r, attr);
        if (!plain(recv)) {
            String tv = fresh();
            b.append(o.ref).append(' ').append(tv).append(" = ").append(recv).append(";\n");
            indent(b, ind);
            recv = tv;
        }
        storeGuess(recv + ".t_" + m, recv + ".b_" + m, typedField(o, attr), val, b, ind);
    }

    /// Method name -> the site its `pt_` bridge and `tv_` slots fall back
    /// to, taken while the bodies are lowered: the site tables are written
    /// before the members that use them.
    private final Map<String, String> patchedSites = new HashMap<String, String>();

    private void patchedSite(String name) {
        if (!patchedSites.containsKey(name)) {
            patchedSites.put(name, em.attrSite(name));
        }
    }

    /// Typed methods called directly, each through its `pt_` bridge.
    private final Set<Infer.Fn> bridged = new LinkedHashSet<Infer.Fn>();

    /// The statements a typed method entered on `self` runs first: when the
    /// instance's class overrides compiled methods (a class built at run
    /// time that borrowed this layout, PyType.borrowLayout), the call goes
    /// the generic way, which finds the override, and its result is taken
    /// back as the typed result. `a1..`/`b1..` are the parameters.
    private String patchedFallback(Infer.Fn f, String self) {
        int ar = f.params.length - 1;
        StringBuilder a = new StringBuilder();
        for (int i = 1; i <= ar; i++) {
            Ty t = f.params[i];
            a.append(", ").append(t.k == Ty.K.INT ? "(b" + i + " != null ? b" + i + " : (Object) Long.valueOf(a" + i + "))"
                    : box(new E("a" + i, t)));
        }
        String site = patchedSites.get(f.def.name);
        if (site == null) {
            // Only a method some typed call reaches has a bridge or a slot
            // that is called; its site was taken when that call was lowered.
            return "if (" + self + ".cls.patched) {\n    throw Py.runtimeError(\"unreachable\");\n}";
        }
        String call = ar <= 13 ? site + ".call" + ar + "(" + self + a + ")"
                : site + ".callN(" + self + ", new Object[] {null" + a + "})";
        String r;
        if (f.ret.k == Ty.K.INT) {
            r = "Typed.retObj(" + call + ")";
        } else if (objectRet(f.ret)) {
            r = call;
        } else {
            r = unbox(call, f.ret);
        }
        return "if (" + self + ".cls.patched) {\n    return " + r + ";\n}";
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
            Infer.Fn f = in.emitFns().get(m);
            if (!g.contains(f)) {
                continue;
            }
            o.append("        @Override\n").append(head);
            for (String line : patchedFallback(f, "this").split("\n")) {
                o.append("            ").append(line).append('\n');
            }
            o.append("            return ").append(typed.get(f)).append("(this").append(pass).append(");\n        }\n");
        }
        return o.toString();
    }

    /// The typed methods and their helpers, as static members of the
    /// module class.
    String members() {
        StringBuilder o = new StringBuilder();
        for (String b : bodies.values()) {
            o.append(resolveForeign(b));
        }
        for (String b : longBodies.values()) {
            o.append(resolveForeign(b));
        }
        for (Layouts.Cls k : constructors) {
            Layouts.Cls ow = Layouts.resolver(k, "__init__");
            Infer.Fn f = in.emitFns().get(ow.typable.get("__init__"));
            if (typed.get(f) == null) {
                // Its __init__ fell back to the generic version after the
                // call that asked for this was lowered, taking every
                // function that calls it along.
                continue;
            }
            StringBuilder sig = new StringBuilder();
            StringBuilder pass = new StringBuilder();
            for (int i = 1; i < f.params.length; i++) {
                sig.append(i == 1 ? "" : ", ").append(param(f.params[i], "a" + i, "b" + i));
                pass.append(", a").append(i).append(f.params[i].k == Ty.K.INT ? ", b" + i : "");
            }
            String made = "new " + k.javaName + "(" + k.typeVar + ")";
            if (k.builtin != null) {
                StringBuilder boxed = new StringBuilder();
                for (int i = 1; i < f.params.length; i++) {
                    boxed.append(", ").append(f.params[i].k == Ty.K.INT ? "(b" + i + " != null ? b" + i
                            + " : Long.valueOf(a" + i + "))" : box(new E("a" + i, f.params[i])));
                }
                made = "Typed.withBase(" + made + ", " + builtinType(k.builtin) + ", true" + boxed + ")";
            }
            o.append("    static ").append(k.javaName).append(" n_").append(k.javaName).append('(').append(sig)
                    .append(") {\n        ").append(k.javaName).append(" o = ").append(made)
                    .append(";\n        ").append(typed.get(f)).append("(o").append(pass)
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
            o.append("    static Object fb_").append(boxerId(p[0])).append('_').append(p[1]).append('(').append(p[0])
                    .append(" o) {\n        return o.b_").append(p[1]).append(" != null ? o.b_").append(p[1])
                    .append(" : Long.valueOf(o.t_").append(p[1]).append(");\n    }\n");
        }
        for (Map.Entry<String, String> ge : guardBoxers.entrySet()) {
            String[] p = (ge.getKey() + "\t" + ge.getValue()).split("\t", 4);
            o.append("    static Object fg_").append(boxerId(p[0])).append('_').append(p[1]).append('(').append(p[0])
                    .append(" o) {\n        return o.b_").append(p[1]).append(" != null ? ").append(p[2])
                    .append(" : ").append(p[3]).append(";\n    }\n");
        }
        for (Infer.Fn f : bridged) {
            if (typed.get(f) == null) {
                continue;
            }
            StringBuilder sig = new StringBuilder(f.cls.javaName + " a0");
            StringBuilder pass = new StringBuilder("a0");
            for (int i = 1; i < f.params.length; i++) {
                sig.append(", ").append(param(f.params[i], "a" + i, "b" + i));
                pass.append(", a").append(i).append(f.params[i].k == Ty.K.INT ? ", b" + i : "");
            }
            o.append("    static ").append(retJ(f)).append(" pt_").append(typed.get(f)).append('(').append(sig)
                    .append(") {\n");
            for (String line : patchedFallback(f, "a0").split("\n")) {
                o.append("        ").append(line).append('\n');
            }
            o.append("        return ").append(typed.get(f)).append('(').append(pass).append(");\n    }\n");
        }
        for (Layouts.Cls k : unboxers) {
            o.append("    static ").append(clsJ(k)).append(' ').append(unboxerName(k))
                    .append("(Object o) {\n        if (o instanceof ").append(clsJ(k)).append(") {\n            return (")
                    .append(clsJ(k)).append(") o;\n        }\n        if (o == Py.NONE) {\n            return null;\n")
                    .append("        }\n        throw Typed.mismatch(o);\n    }\n");
        }
        return o.toString();
    }
}
