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

        E(String c, Ty t) {
            this(c, t, null);
        }

        E(String c, Ty t, String lc) {
            this.c = c;
            this.t = t;
            this.lc = lc;
        }
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
    /// Shadowed locals with no Object variable at all: read boxed only on a
    /// slow path.
    private Set<String> longOnly = new HashSet<String>();
    /// Locals whose previous list a new one may be written into (see
    /// ListRecycling).
    private Set<String> recycled = new HashSet<String>();
    /// What `name` answers for a local instead, inside an outlined slow path.
    private Map<String, E> subst;
    /// The outlined slow paths of hoisted arithmetic.
    private final StringBuilder slow = new StringBuilder(); //NOPMD AvoidStringBufferField - the generator's output
    private int slowCount;
    /// The function being emitted is its `TL_` variant: its unassigned int
    /// parameters arrive as longs.
    private boolean longParams;
    /// Typed functions some call site wants a `TL_` variant of.
    private final Set<Infer.Fn> wantLong = new LinkedHashSet<Infer.Fn>();
    private final Map<Infer.Fn, String> longBodies = new LinkedHashMap<Infer.Fn, String>();

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

    private void findPure() {
        for (Map.Entry<String, Infer.Fn> e : in.globalFns.entrySet()) {
            pure.add(e.getValue());
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Infer.Fn f : new ArrayList<Infer.Fn>(pure)) {
                for (Stmt st : f.def.body) {
                    if (!pureNode(st)) {
                        pure.remove(f);
                        changed = true;
                        break;
                    }
                }
            }
        }
    }

    private boolean pureNode(Object o) {
        if (o instanceof Call) {
            if (!pureCall((Call) o, null)) {
                return false;
            }
        } else if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (!localTarget(t)) {
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
        if (!(c.func instanceof Name) || !c.keywords.isEmpty()) {
            return false;
        }
        String n = ((Name) c.func).id;
        Scope.Binding b = scope == null ? null : scope.resolve(n);
        Infer.Fn f = in.globalFns.get(n);
        if (f != null && (b == null || b == Scope.Binding.GLOBAL)) {
            return pure.contains(f);
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

    private static boolean representable(Ty t) {
        switch (t.k) {
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

    static String retJ(Infer.Fn f) {
        return jt(f.ret);
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

    /// Whether parameter `i` of `f` can arrive as a long: an int nothing in
    /// the body stores into.
    private static boolean longParam(Infer.Fn f, int i, Set<String> stored) {
        return f.params[i].k == Ty.K.INT && !stored.contains(f.names.get(i));
    }

    private static boolean hasLongParam(Infer.Fn f) {
        Set<String> stored = assigned(f.def.body);
        for (int i = 0; i < f.names.size(); i++) {
            if (longParam(f, i, stored)) {
                return true;
            }
        }
        return false;
    }

    private String function(Infer.Fn f) {
        cur = f;
        sc = f.def.scope;
        calls = new LinkedHashSet<Infer.Fn>();
        callsGroups = new LinkedHashSet<String>();
        tmp = 0;
        nonNull = new HashSet<String>();
        shadow = new HashMap<String, String>();
        longOnly = new HashSet<String>();
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
        StringBuilder b = new StringBuilder();
        String fname = longParams ? "TL" + typed.get(f).substring(2) : typed.get(f);
        b.append("    static ").append(retJ(f)).append(' ').append(fname).append('(');
        Set<String> declared = new HashSet<String>(f.names);
        Set<String> stored = assigned(f.def.body);
        for (int i = 0; i < f.names.size(); i++) {
            String n = f.names.get(i);
            if (longParams && longParam(f, i, stored)) {
                String q = "q_" + Emitter.mangle(n);
                b.append(i == 0 ? "" : ", ").append("long ").append(q);
                shadow.put(n, q);
                longOnly.add(n);
            } else {
                b.append(i == 0 ? "" : ", ").append(jt(f.params[i])).append(' ').append(var(n));
            }
        }
        b.append(") {\n");
        for (Map.Entry<String, Ty> e : f.locals.entrySet()) {
            if (declared.add(e.getKey())) {
                b.append("        ").append(jt(e.getValue())).append(' ').append(var(e.getKey())).append(" = ")
                        .append(zero(e.getValue())).append(";\n");
            }
        }
        boolean abrupt = block(f.def.body, b, 2);
        if (!abrupt) {
            if (objectRet(f.ret)) {
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
                    E e = expr(v);
                    String t = fresh();
                    indent(b, ind);
                    b.append(jt(e.t)).append(' ').append(t).append(" = ").append(e.c).append(";\n");
                    tv.add(new E(t, e.t));
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
            indent(b, ind);
            b.append("while (").append(forever ? "true" : testExpr(w.test)).append(") {\n");
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
                stop = "Typed.bound(" + box(expr(a.get(0))) + ")";
            } else {
                start = "Typed.bound(" + box(expr(a.get(0))) + ")";
                stop = "Typed.bound(" + box(expr(a.get(1))) + ")";
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
            boolean unboxed = counter && !longOnly.contains(tn) && !readOutside(tn, f);
            if (unboxed) {
                // Read nowhere but in this body: no boxed copy at all.
                longOnly.add(tn);
            } else if (longOnly.contains(tn)) {
                throw new Unsupported("long-only " + tn + " reused");
            } else {
                indent(b, ind + 2);
                b.append(v).append(" = ").append(conv(new E("Long.valueOf(" + i + ")", Ty.INT), vt)).append(";\n");
            }
            if (counter) {
                shadow.put(tn, i);
            }
            block(f.body, b, ind + 2);
            shadow.remove(tn);
            if (unboxed) {
                longOnly.remove(tn);
            }
            indent(b, ind + 1);
            b.append("}\n");
            indent(b, ind);
            b.append("}\n");
            return;
        }
        E it = expr(f.iter);
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
        b.append(v).append(" = ").append(conv(item, vt)).append(";\n");
        block(f.body, b, ind + 2);
        indent(b, ind + 1);
        b.append("}\n");
        indent(b, ind);
        b.append("}\n");
    }

    private static Long intConstant(Expr e) {
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.INT) {
            try {
                return Long.valueOf(Long.parseLong(((Constant) e).text.replace("_", "")));
            } catch (NumberFormatException x) {
                return null;
            }
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

    private void store(Expr t, E v, StringBuilder b, int ind) {
        if (t instanceof Name) {
            String n = ((Name) t).id;
            switch (sc.resolve(n)) {
                case LOCAL:
                    indent(b, ind);
                    b.append(var(n)).append(" = ").append(conv(v, localType(n))).append(";\n");
                    forgetPathsOf(n);
                    if (longOnly.contains(n)) {
                        throw new Unsupported("store to a long-only " + n);
                    }
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
                throw new Unsupported("subscript store on " + l.t);
            }
            if (!plain(v.c)) {
                String tv = fresh();
                indent(b, ind);
                b.append(jt(v.t)).append(' ').append(tv).append(" = ").append(v.c).append(";\n");
                v = new E(tv, v.t);
            }
            Ty el = in.elem(l.t);
            indent(b, ind);
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

    private void augAssign(AugAssign a, StringBuilder b, int ind) {
        Expr t = a.target;
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
    private static Set<String> assigned(List<Stmt> body) {
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
            E r = expr(a.value);
            Ty ft = fieldTy(r, a.attr);
            String p = path(a);
            return new E(fieldRef(r, a.attr), p != null && nonNull.contains(p) ? ft.nonNone() : ft);
        }
        if (e instanceof BinOp) {
            if (pre != null && (pureInt(e) || pureFloat(e))) {
                return hoistInt(e);
            }
            BinOp b = (BinOp) e;
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
            if (pre != null && intCompare((Compare) e)) {
                return hoistInt(e);
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
            E l = expr(s.value);
            E i = expr(s.index);
            if (l.t.k != Ty.K.LIST || !(i.t.k == Ty.K.INT || i.t.k == Ty.K.BOOL)) {
                throw new Unsupported("subscript of " + l.t);
            }
            Ty el = in.elem(l.t);
            if (el.k == Ty.K.BOT) {
                throw new Unsupported("element of nothing");
            }
            String slow = el.k == Ty.K.FLOAT ? "h_getF(" + l.c + ", " + box(i) + ")" : "h_get(" + l.c + ", " + box(i) + ")";
            String fast = slow;
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
        throw new Unsupported(e.getClass().getSimpleName());
    }

    private E name(Name nm) {
        String n = nm.id;
        if (subst != null && subst.containsKey(n)) {
            return subst.get(n);
        }
        switch (sc.resolve(n)) {
            case LOCAL: {
                if (!sc.params.contains(n) && !da.safe.contains(nm)) {
                    throw new Unsupported("maybe unbound " + n);
                }
                Ty t = localType(n);
                if (sc.deleted.contains(n)) {
                    throw new Unsupported("deleted " + n);
                }
                String l = shadow.get(n);
                if (l != null) {
                    return new E(longOnly.contains(n) ? "Long.valueOf(" + l + ")" : var(n), t, l);
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
            "                long[] n = l.ints;",
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

    /// Whether `e` is int arithmetic over int locals and constants that
    /// cannot raise: evaluating it early, or twice, changes nothing.
    private boolean pureInt(Expr e) {
        if (e instanceof Constant) {
            Long c = intConstant(e);
            return c != null && Math.abs(c.longValue()) < (1L << 60);
        }
        if (e instanceof Name) {
            String n = ((Name) e).id;
            return sc.resolve(n) == Scope.Binding.LOCAL && localType(n).k == Ty.K.INT;
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            return "-".equals(u.op) && pureInt(u.operand);
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            switch (b.op) {
                case "+":
                case "-":
                case "*":
                case "&":
                case "|":
                case "^":
                    return pureInt(b.left) && pureInt(b.right);
                case ">>": {
                    Long c = intConstant(b.right);
                    return c != null && c.longValue() >= 0 && c.longValue() < 64 && pureInt(b.left);
                }
                default:
                    return false;
            }
        }
        return false;
    }

    /// Float arithmetic over float and int locals and constants, as
    /// `pureInt` is for ints; every step here is IEEE arithmetic, as in
    /// Python, except a division by zero, which takes the generic path.
    private boolean pureFloat(Expr e) {
        if (e instanceof Constant) {
            return ((Constant) e).kind == Constant.Kind.FLOAT && !((Constant) e).text.matches(".*[jJ]");
        }
        if (e instanceof Name) {
            String n = ((Name) e).id;
            return sc.resolve(n) == Scope.Binding.LOCAL && localType(n).k == Ty.K.FLOAT;
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            return "-".equals(u.op) && pureFloat(u.operand);
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            switch (b.op) {
                case "+":
                case "-":
                case "*":
                    return pureFloat(b.left) && (pureFloat(b.right) || pureInt(b.right))
                            || pureInt(b.left) && pureFloat(b.right);
                case "/":
                    return (pureFloat(b.left) || pureInt(b.left)) && (pureFloat(b.right) || pureInt(b.right));
                default:
                    return false;
            }
        }
        return false;
    }

    private boolean intCompare(Compare c) {
        if (c.ops.size() != 1) {
            return false;
        }
        switch (c.ops.get(0)) {
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
                return pureInt(c.left) && pureInt(c.comparators.get(0));
            default:
                return false;
        }
    }

    /// `e` computed on Java longs in a block before the statement, falling
    /// back to the generic operations when an operand is a big int or a
    /// step overflows. ParparVM inlines no call, not even to a helper of
    /// the same class, so each operation is spelled out here.
    private E hoistInt(Expr e) {
        StringBuilder saved = pre;
        String generic;
        Ty t;
        // The generic path goes into a method of its own: with it inline, a
        // function made only of such arithmetic would call out, and a
        // ParparVM function that calls out gets a stack guard and is no
        // longer inlined by the C compiler.
        Map<String, Name> nodes = new LinkedHashMap<String, Name>();
        for (Name nm : namesIn(e)) {
            nodes.putIfAbsent(nm.id, nm);
        }
        List<String> names = new ArrayList<String>(nodes.keySet());
        Map<String, E> sub = new HashMap<String, E>();
        StringBuilder params = new StringBuilder();
        StringBuilder args = new StringBuilder();
        for (int k = 0; k < names.size(); k++) {
            E ne = name(nodes.get(names.get(k)));
            String pn = "p" + k;
            String sep = k == 0 ? "" : ", ";
            if (ne.lc != null && longOnly.contains(names.get(k))) {
                params.append(sep).append("long ").append(pn);
                args.append(sep).append(ne.lc);
                sub.put(names.get(k), new E("Long.valueOf(" + pn + ")", ne.t, pn));
            } else {
                params.append(sep).append(jt(ne.t)).append(' ').append(pn);
                args.append(sep).append(ne.c);
                sub.put(names.get(k), new E(pn, ne.t));
            }
        }
        Map<String, E> savedSub = subst;
        pre = null;
        subst = sub;
        try {
            E g = e instanceof Compare ? compare((Compare) e) : expr(e);
            t = g.t;
            String m = "S" + (slowCount++);
            slow.append("    static ").append(jt(t)).append(' ').append(m).append('(').append(params)
                    .append(") {\n        return ").append(g.c).append(";\n    }\n");
            generic = m + "(" + args + ")";
        } finally {
            pre = saved;
            subst = savedSub;
        }
        String r = fresh();
        String done = fresh();
        String fast = fresh();
        StringBuilder f = new StringBuilder();
        Map<String, String> leaves = new HashMap<String, String>();
        String v;
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            String a = longOf(c.left, f, leaves, fast);
            String b2 = longOf(c.comparators.get(0), f, leaves, fast);
            v = a + " " + c.ops.get(0) + " " + b2;
        } else if (t.k == Ty.K.FLOAT) {
            v = doubleOf(e, f, leaves, fast);
        } else {
            v = "Long.valueOf(" + longOf(e, f, leaves, fast) + ")";
        }
        int ind = preInd;
        indent(pre, ind);
        pre.append(jt(t)).append(' ').append(r).append(";\n");
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
        return new E(r, t);
    }

    private static List<Name> namesIn(Object o) {
        List<Name> out = new ArrayList<Name>();
        if (o instanceof Name) {
            out.add((Name) o);
        }
        Infer.children(o, x -> out.addAll(namesIn(x)));
        return out;
    }

    /// Statements into `f` computing `e` (`pureFloat`, or `pureInt` to be
    /// converted) as a double; the expression holding it.
    private String doubleOf(Expr e, StringBuilder f, Map<String, String> leaves, String fast) {
        if (pureInt(e)) {
            String x = longOf(e, f, leaves, fast);
            String d = fresh();
            // Exact below 2^53; Python rounds a wider int the same way, but
            // the generic path is the one that knows.
            f.append("if (").append(x).append(" > 9007199254740992L || ").append(x)
                    .append(" < -9007199254740992L) {\n    break ").append(fast).append(";\n}\ndouble ").append(d)
                    .append(" = (double) ").append(x).append(";\n");
            return d;
        }
        if (e instanceof Constant || e instanceof Name) {
            return expr(e).c;
        }
        String d = fresh();
        if (e instanceof UnaryOp) {
            f.append("double ").append(d).append(" = -").append(doubleOf(((UnaryOp) e).operand, f, leaves, fast))
                    .append(";\n");
            return d;
        }
        BinOp b = (BinOp) e;
        String x = doubleOf(b.left, f, leaves, fast);
        String y = doubleOf(b.right, f, leaves, fast);
        if ("/".equals(b.op)) {
            f.append("if (").append(y).append(" == 0) {\n    break ").append(fast).append(";\n}\n");
        }
        f.append("double ").append(d).append(" = ").append(x).append(' ').append(b.op).append(' ').append(y)
                .append(";\n");
        return d;
    }

    /// Statements into `f` computing `e` as a long; the name holding it.
    private String longOf(Expr e, StringBuilder f, Map<String, String> leaves, String fast) {
        if (e instanceof Constant) {
            return intConstant(e) + "L";
        }
        if (e instanceof Name) {
            E ne = name((Name) e);
            if (ne.lc != null) {
                return ne.lc;
            }
            String c = ne.c;
            String d = leaves.get(c);
            if (d == null) {
                d = fresh();
                leaves.put(c, d);
                f.append("if (!(").append(c).append(" instanceof Long)) {\n    break ").append(fast)
                        .append(";\n}\nlong ").append(d).append(" = ((Long) ").append(c).append(").longValue();\n");
            }
            return d;
        }
        String d = fresh();
        if (e instanceof UnaryOp) {
            String x = longOf(((UnaryOp) e).operand, f, leaves, fast);
            f.append("if (").append(x).append(" == Long.MIN_VALUE) {\n    break ").append(fast).append(";\n}\n")
                    .append("long ").append(d).append(" = -").append(x).append(";\n");
            return d;
        }
        BinOp b = (BinOp) e;
        String x = longOf(b.left, f, leaves, fast);
        String y = longOf(b.right, f, leaves, fast);
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
                // Exact when both fit 31 bits; anything wider takes the runtime.
                f.append("if (((").append(x).append(" + 0x80000000L) | (").append(y)
                        .append(" + 0x80000000L)) >>> 32 != 0) {\n    break ").append(fast).append(";\n}\n")
                        .append("long ").append(d).append(" = ").append(x).append(" * ").append(y).append(";\n");
                break;
            default:
                f.append("long ").append(d).append(" = ").append(x).append(' ').append(b.op).append(' ').append(y)
                        .append(";\n");
                break;
        }
        return d;
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
            sb.append(i == 0 ? "" : ", ").append(conv(a, f.params[from + i]));
        }
        return sb.toString();
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

    /// A call of `f`'s `TL_` variant when every argument for one of its
    /// long parameters has a long form; else null.
    private String longCall(Infer.Fn f, List<E> args) {
        if (args.size() != f.params.length || !hasLongParam(f)) {
            return null;
        }
        Set<String> stored = assigned(f.def.body);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            E a = args.get(i);
            String v;
            if (longParam(f, i, stored)) {
                if (a.lc == null) {
                    return null;
                }
                v = a.lc;
            } else {
                v = conv(a, f.params[i]);
            }
            sb.append(i == 0 ? "" : ", ").append(v);
        }
        wantLong.add(f);
        return "TL" + typed.get(f).substring(2) + "(" + sb + ")";
    }

    private E result(Infer.Fn f, String call) {
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
        if (c.func instanceof Name) {
            String n = ((Name) c.func).id;
            Scope.Binding b = sc.resolve(n);
            if (b == Scope.Binding.GLOBAL) {
                Infer.Fn f = in.globalFns.get(n);
                if (f != null) {
                    List<E> a = args(c);
                    calls.add(f);
                    String lcall = longCall(f, a);
                    if (lcall != null) {
                        return result(f, lcall);
                    }
                    return result(f, typed.get(f) + "(" + argList(f, 0, a) + ")");
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
                return builtin(n, args(c));
            }
            throw new Unsupported("call of " + n);
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
            List<E> args = args(c);
            if (r.t.k == Ty.K.LIST && !r.t.opt) {
                return listMethod(r, a.attr, args);
            }
            if (r.t.k != Ty.K.INST) {
                throw new Unsupported("method of " + r.t);
            }
            List<FunctionDef> cand = in.candidates(r.t.cls, a.attr);
            if (cand == null) {
                throw new Unsupported("method " + a.attr);
            }
            String recv = nz(r, a.attr);
            Layouts.Cls res = Layouts.resolver(r.t.cls, a.attr);
            if (cand.size() == 1 && res != null && res.typable.get(a.attr) == cand.get(0)) { //NOPMD CompareObjectsWithEquals - identity is meant
                Infer.Fn f = in.fns.get(cand.get(0));
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
        constructors.add(k);
        String al = argList(f, 1, args);
        return new E("n_" + k.javaName + "(" + al + ")", Ty.inst(k, false));
    }

    private E builtin(String n, List<E> a) {
        switch (n) {
            case "len":
                if (a.size() == 1 && a.get(0).t.k == Ty.K.LIST) {
                    return new E("h_len(" + a.get(0).c + ")", Ty.INT);
                }
                break;
            case "float":
                if (a.size() == 1 && !a.get(0).t.opt
                        && (a.get(0).t.k == Ty.K.INT || a.get(0).t.k == Ty.K.FLOAT)) {
                    return new E(toD(a.get(0)), Ty.FLOAT);
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
        throw new Unsupported("builtin " + n);
    }

    private E listMethod(E l, String m, List<E> a) {
        switch (m) {
            case "append":
                if (a.size() == 1) {
                    return new E("Typed.append(" + l.c + ", " + box(a.get(0)) + ")", Ty.NONE);
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
                    arg = v;
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
        String call = box(new E(typed.get(f) + "(" + args + ")", f.ret));
        if (test.length() == 0) {
            // Always taken; javac must still see the generic body as reachable.
            test.append("Typed.yes");
        }
        return "if (" + test + ") {\n    return " + call + ";\n}\n";
    }

    /// The Java declaration of a typed field, or null for an object one.
    String fieldDecl(Layouts.Cls k, String f) {
        Ty t = typedField(k, f);
        return t == null ? null : jt(t) + " t_" + Emitter.mangle(f) + ";";
    }

    String fieldGetCase(Layouts.Cls k, String f) {
        Ty t = typedField(k, f);
        return box(new E("t_" + Emitter.mangle(f), t));
    }

    String fieldSetCase(Layouts.Cls k, String f, String v) {
        Ty t = typedField(k, f);
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
                sig.append(i == 1 ? "" : ", ").append(jt(any.params[i])).append(" a").append(i);
                pass.append(", a").append(i);
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
        // A TL_ body may want further variants; emit until none is new.
        while (!longBodies.keySet().containsAll(wantLong)) {
            for (Infer.Fn f : new ArrayList<Infer.Fn>(wantLong)) {
                if (!bodies.containsKey(f)) {
                    // Its caller turned out untyped too; nothing calls it.
                    longBodies.put(f, "");
                } else if (!longBodies.containsKey(f)) {
                    longParams = true;
                    try {
                        longBodies.put(f, function(f));
                    } finally {
                        longParams = false;
                    }
                    o.append(longBodies.get(f));
                }
            }
        }
        for (Layouts.Cls k : constructors) {
            Layouts.Cls ow = Layouts.resolver(k, "__init__");
            Infer.Fn f = in.fns.get(ow.typable.get("__init__"));
            StringBuilder sig = new StringBuilder();
            StringBuilder pass = new StringBuilder();
            for (int i = 1; i < f.params.length; i++) {
                sig.append(i == 1 ? "" : ", ").append(jt(f.params[i])).append(" a").append(i);
                pass.append(", a").append(i);
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
        for (Layouts.Cls k : unboxers) {
            o.append("    static ").append(k.javaName).append(" u_").append(k.javaName)
                    .append("(Object o) {\n        if (o instanceof ").append(k.javaName).append(") {\n            return (")
                    .append(k.javaName).append(") o;\n        }\n        if (o == Py.NONE) {\n            return null;\n")
                    .append("        }\n        throw Typed.mismatch(o);\n    }\n");
        }
        return o.toString();
    }
}
