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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The values each int local of a typed function can hold, as intervals.
///
/// A Python int never overflows, so typed code has kept every int boxed
/// and checked each operation for overflow. Most ints in real code are
/// bounded, by construction: a loop counter by its `range`, an accumulator
/// by its trip count, and a value masked with `& 0xFFFFFFFF` by the mask.
/// The masking idioms that emulate fixed-width arithmetic (`((v + 2**31) &
/// M32) - 2**31`) bound their result whatever goes in. A local whose every
/// value fits in 64 bits is held in a Java `long`, and an expression whose
/// result fits is computed with wrapping `long` arithmetic: when the true
/// result is known to fit, the result modulo 2^64 is the result, however
/// large the intermediate values were.
///
/// The analysis is abstract interpretation over the statements, with loops
/// run to a fixpoint. A local that still grows after a few passes over a
/// `range` loop of known trip count, by the same amount each pass, is an
/// accumulator: its bound is the trip count times that amount. Anything
/// else that grows has no bound.
final class IntRanges {
    static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger TWO64 = BigInteger.ONE.shiftLeft(64);
    private static final int PASSES = 4;

    /// An interval; a null bound is unbounded.
    static final class Iv {
        static final Iv TOP = new Iv(null, null);
        final BigInteger lo;
        final BigInteger hi;

        Iv(BigInteger lo, BigInteger hi) {
            this.lo = lo;
            this.hi = hi;
        }

        static Iv of(long v) {
            BigInteger b = BigInteger.valueOf(v);
            return new Iv(b, b);
        }

        boolean fitsLong() {
            return lo != null && hi != null && lo.compareTo(LONG_MIN) >= 0 && hi.compareTo(LONG_MAX) <= 0;
        }

        /// Within [0, 2^64): a value whose low 64 bits, read unsigned, are it.
        boolean fitsUnsigned64() {
            return lo != null && hi != null && lo.signum() >= 0 && hi.compareTo(TWO64) < 0;
        }

        Iv join(Iv o) {
            if (o == null) {
                return this;
            }
            return new Iv(lo == null || o.lo == null ? null : lo.min(o.lo),
                    hi == null || o.hi == null ? null : hi.max(o.hi));
        }

        boolean same(Iv o) {
            return o != null && eq(lo, o.lo) && eq(hi, o.hi);
        }

        private static boolean eq(BigInteger a, BigInteger b) {
            return a == null ? b == null : a.equals(b);
        }
    }

    private final Infer in;
    private final Infer.Fn fn;
    private final Scope sc;
    /// The int locals considered: typed INT, not parameters.
    private final Set<String> ints = new HashSet<String>();
    /// Every value each local ever holds.
    private final Map<String, Iv> seen = new HashMap<String, Iv>();
    /// Parameters of an expression function being inlined, bound to the
    /// argument's interval.
    private final List<Map<String, Iv>> bound = new ArrayList<Map<String, Iv>>();

    IntRanges(Infer in, Infer.Fn fn) {
        this.in = in;
        this.fn = fn;
        this.sc = fn.def.scope;
        for (Map.Entry<String, Ty> e : fn.locals.entrySet()) {
            if (e.getValue().k == Ty.K.INT && !e.getValue().opt && !sc.params.contains(e.getKey())) {
                ints.add(e.getKey());
            }
        }
        if (!ints.isEmpty()) {
            Map<String, Iv> env = new HashMap<String, Iv>();
            run(fn.def.body, env, new Loop());
        }
    }

    /// The locals held in a Java `long`.
    Set<String> longLocals() {
        Set<String> r = new HashSet<String>();
        for (String n : ints) {
            Iv v = seen.get(n);
            if (v != null && v.fitsLong()) {
                r.add(n);
            }
        }
        return r;
    }

    /// The int literal `e`, or null: a constant, or a negated one.
    static BigInteger literal(Expr e) {
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.INT) {
            return parse(((Constant) e).text);
        }
        if (e instanceof UnaryOp && "-".equals(((UnaryOp) e).op)) {
            BigInteger v = literal(((UnaryOp) e).operand);
            return v == null ? null : v.negate();
        }
        return null;
    }

    static BigInteger parse(String text) {
        String t = text.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        try {
            if (t.startsWith("0x")) {
                return new BigInteger(t.substring(2), 16);
            }
            if (t.startsWith("0o")) {
                return new BigInteger(t.substring(2), 8);
            }
            if (t.startsWith("0b")) {
                return new BigInteger(t.substring(2), 2);
            }
            return new BigInteger(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- statements ------------------------------------------------------

    /// The states that leave a loop body early.
    private static final class Loop {
        Map<String, Iv> brk;
        Map<String, Iv> cont;
    }

    private void store(String n, Iv v, Map<String, Iv> env) {
        if (!ints.contains(n)) {
            return;
        }
        env.put(n, v);
        Iv s = seen.get(n);
        seen.put(n, s == null ? v : s.join(v));
    }

    private void top(Set<String> names, Map<String, Iv> env) {
        for (String n : names) {
            store(n, Iv.TOP, env);
        }
    }

    private static Map<String, Iv> join(Map<String, Iv> a, Map<String, Iv> b) {
        if (a == null) {
            return b == null ? null : new HashMap<String, Iv>(b);
        }
        if (b == null) {
            return new HashMap<String, Iv>(a);
        }
        Map<String, Iv> r = new HashMap<String, Iv>(a);
        for (Map.Entry<String, Iv> e : b.entrySet()) {
            Iv x = r.get(e.getKey());
            r.put(e.getKey(), x == null ? e.getValue() : x.join(e.getValue()));
        }
        return r;
    }

    /// Runs `body` from `env`; answers the state at its normal end, or null
    /// when it cannot end normally.
    private Map<String, Iv> run(List<Stmt> body, Map<String, Iv> env, Loop loop) {
        Map<String, Iv> cur = env;
        for (Stmt s : body) {
            if (cur == null) {
                return null;
            }
            cur = stmt(s, cur, loop);
        }
        return cur;
    }

    private Map<String, Iv> stmt(Stmt s, Map<String, Iv> env, Loop loop) {
        walrus(s, env);
        if (s instanceof Assign) {
            Assign a = (Assign) s;
            Iv v = eval(a.value, env);
            for (Expr t : a.targets) {
                if (t instanceof Name) {
                    store(((Name) t).id, v, env);
                } else {
                    top(TypedEmit.assigned(Collections.<Stmt>singletonList(new Assign(
                            Collections.singletonList(t), a.value))), env);
                }
            }
            return env;
        }
        if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            if (a.target instanceof Name) {
                store(((Name) a.target).id, eval(new BinOp(a.target, a.op, a.value), env), env);
            }
            return env;
        }
        if (s instanceof If) {
            If i = (If) s;
            return join(run(i.body, new HashMap<String, Iv>(env), loop),
                    run(i.orelse, new HashMap<String, Iv>(env), loop));
        }
        if (s instanceof For) {
            return forLoop((For) s, env);
        }
        if (s instanceof While) {
            While w = (While) s;
            Map<String, Iv> out = loop(w.body, env, null, null, null);
            return run(w.orelse, out, loop);
        }
        if (s instanceof Return || s instanceof Raise) {
            return null;
        }
        if (s instanceof Break) {
            loop.brk = join(loop.brk, env);
            return null;
        }
        if (s instanceof Continue) {
            loop.cont = join(loop.cont, env);
            return null;
        }
        if (s instanceof ExprStmt || s instanceof Pass) {
            return env;
        }
        // Anything else (try, with, match, a nested def): what it stores is
        // unbounded, and it may leave the loop in ways not followed here.
        top(TypedEmit.assigned(Collections.singletonList(s)), env);
        return env;
    }

    private void walrus(Object o, Map<String, Iv> env) {
        if (o instanceof NamedExpr) {
            store(((NamedExpr) o).target.id, Iv.TOP, env);
        }
        if (!(o instanceof Stmt) || o instanceof ExprStmt || o instanceof Assign || o instanceof AugAssign
                || o instanceof Return) {
            Infer.children(o, x -> walrus(x, env));
        }
    }

    private Map<String, Iv> forLoop(For f, Map<String, Iv> env) {
        String target = f.target instanceof Name ? ((Name) f.target).id : null;
        Iv tv = Iv.TOP;
        BigInteger trips = null;
        if (in.rangeCall(f.iter, sc)) {
            List<Expr> a = ((Call) f.iter).args;
            Iv start = a.size() == 1 ? Iv.of(0) : eval(a.get(0), env);
            Iv stop = eval(a.get(a.size() == 1 ? 0 : 1), env);
            BigInteger step = a.size() == 3 ? literal(a.get(2)) : BigInteger.ONE;
            if (step != null && step.signum() != 0 && start.lo != null && start.hi != null && stop.lo != null
                    && stop.hi != null) {
                if (step.signum() > 0) {
                    tv = new Iv(start.lo, stop.hi.subtract(BigInteger.ONE).max(start.lo));
                    trips = stop.hi.subtract(start.lo).max(BigInteger.ZERO);
                } else {
                    tv = new Iv(stop.lo.add(BigInteger.ONE).min(start.hi), start.hi);
                    trips = start.hi.subtract(stop.lo).max(BigInteger.ZERO);
                }
                trips = trips.add(step.abs()).subtract(BigInteger.ONE).divide(step.abs());
            }
        } else if (target != null) {
            top(Collections.singleton(target), env);
        } else {
            top(TypedEmit.assigned(Collections.<Stmt>singletonList(f)), env);
        }
        Map<String, Iv> out = loop(f.body, env, target, tv, trips);
        return run(f.orelse, out, new Loop());
    }

    /// A loop over `body`, entered from `entry`, that runs at most `trips`
    /// times (null: unknown). Answers the state after it.
    ///
    /// Passes run until the state at the head of the loop stops changing.
    /// From the third pass on, a local that grew by the same interval in the
    /// last two passes, in a loop of known trip count, is widened to its
    /// value after `trips - 1` passes, and one more pass must confirm that
    /// it grows by that same interval and nothing else moves. Whatever fails
    /// that is unbounded.
    private Map<String, Iv> loop(List<Stmt> body, Map<String, Iv> entry, String target, Iv tv, BigInteger trips) {
        Map<String, Iv> head = new HashMap<String, Iv>(entry);
        Map<String, Iv> exit = new HashMap<String, Iv>(entry);
        Map<String, Iv> prevDelta = new HashMap<String, Iv>();
        Map<String, Iv> widened = new HashMap<String, Iv>();
        for (int pass = 0;; pass++) {
            Map<String, Iv> in0 = new HashMap<String, Iv>(head);
            if (target != null) {
                store(target, tv, in0);
            }
            Loop l = new Loop();
            Map<String, Iv> out = join(run(body, in0, l), l.cont);
            exit = join(join(exit, out), l.brk);
            Map<String, Iv> delta = new HashMap<String, Iv>();
            Set<String> growing = new HashSet<String>();
            if (out != null) {
                for (Map.Entry<String, Iv> e : out.entrySet()) {
                    String n = e.getKey();
                    Iv h = head.get(n);
                    Iv j = h == null ? e.getValue() : h.join(e.getValue());
                    Iv w = widened.get(n);
                    if (w != null) {
                        // Widened: it may grow by its delta, past the head.
                        Iv d = diff(e.getValue(), h);
                        if (d == null || !d.same(w)) {
                            growing.add(n);
                            delta.put(n, null);
                        }
                    } else if (!j.same(h)) {
                        growing.add(n);
                        delta.put(n, diff(e.getValue(), h));
                    }
                }
            }
            if (growing.isEmpty()) {
                return exit;
            }
            Map<String, Iv> next = join(head, out);
            boolean widen = pass >= 2 && pass < PASSES;
            for (String n : growing) {
                Iv d = delta.get(n);
                Iv e0 = entry.get(n);
                if (widen && !widened.containsKey(n) && trips != null && trips.signum() > 0 && d != null
                        && d.same(prevDelta.get(n)) && e0 != null && e0.lo != null && e0.hi != null) {
                    BigInteger k = trips.subtract(BigInteger.ONE);
                    next.put(n, new Iv(e0.lo.add(d.lo.min(BigInteger.ZERO).multiply(k)),
                            e0.hi.add(d.hi.max(BigInteger.ZERO).multiply(k))));
                    widened.put(n, d);
                } else if (pass >= 2) {
                    next.put(n, Iv.TOP);
                    widened.remove(n);
                }
            }
            prevDelta = delta;
            head = next;
        }
    }

    /// How much `b` moved to become `a`, per bound; null when either is
    /// unbounded there.
    private static Iv diff(Iv a, Iv b) {
        if (a == null || b == null || a.lo == null || a.hi == null || b.lo == null || b.hi == null) {
            return null;
        }
        return new Iv(a.lo.subtract(b.lo), a.hi.subtract(b.hi));
    }

    // ---- expressions -----------------------------------------------------

    /// The interval of int expression `e` in state `env`, using what each
    /// local has been anywhere when `env` is null. TOP for anything that is
    /// not provably an int built from ints the analysis knows.
    Iv eval(Expr e, Map<String, Iv> env) {
        if (e instanceof Constant) {
            Constant c = (Constant) e;
            if (c.kind == Constant.Kind.INT) {
                BigInteger v = parse(c.text);
                return v == null ? Iv.TOP : new Iv(v, v);
            }
            return Iv.TOP;
        }
        if (e instanceof Name) {
            return name(((Name) e).id, env);
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            Iv o = eval(u.operand, env);
            switch (u.op) {
                case "+":
                    return o;
                case "-":
                    return new Iv(o.hi == null ? null : o.hi.negate(), o.lo == null ? null : o.lo.negate());
                case "~":
                    return new Iv(o.hi == null ? null : o.hi.not(), o.lo == null ? null : o.lo.not());
                default:
                    return Iv.TOP;
            }
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            return binop(b.op, eval(b.left, env), eval(b.right, env), b.right);
        }
        if (e instanceof IfExp) {
            IfExp x = (IfExp) e;
            return eval(x.body, env).join(eval(x.orelse, env));
        }
        if (e instanceof Call) {
            return call((Call) e, env);
        }
        return Iv.TOP;
    }

    private Iv name(String n, Map<String, Iv> env) {
        for (int i = bound.size() - 1; i >= 0; i--) {
            Iv v = bound.get(i).get(n);
            if (v != null) {
                return v;
            }
            if (i == bound.size() - 1) {
                // An inlined body sees its parameters and the globals only.
                BigInteger c = in.intConsts.get(n);
                return c == null ? Iv.TOP : new Iv(c, c);
            }
        }
        switch (sc.resolve(n)) {
            case LOCAL: {
                if (!ints.contains(n)) {
                    return Iv.TOP;
                }
                Iv v = env == null ? seen.get(n) : env.get(n);
                return v == null ? Iv.TOP : v;
            }
            case GLOBAL: {
                BigInteger c = in.intConsts.get(n);
                return c == null ? Iv.TOP : new Iv(c, c);
            }
            default:
                return Iv.TOP;
        }
    }

    /// The expression function `c` calls (see `Infer.exprBody`), or null.
    Infer.Fn inlinable(Call c) {
        if (!(c.func instanceof Name) || !c.keywords.isEmpty()) {
            return null;
        }
        String n = ((Name) c.func).id;
        Scope s = bound.isEmpty() ? sc : null;
        if (s != null && s.resolve(n) != Scope.Binding.GLOBAL) {
            return null;
        }
        Infer.Fn f = in.globalFns.get(n);
        if (f == null || Infer.exprBody(f.def) == null || f.names.size() != c.args.size()
                || !f.def.args.defaults.isEmpty() || !f.def.decorators.isEmpty() || bound.size() > 4) {
            return null;
        }
        for (Expr a : c.args) {
            if (a instanceof Starred) {
                return null;
            }
        }
        return f;
    }

    private Iv call(Call c, Map<String, Iv> env) {
        Infer.Fn f = inlinable(c);
        if (f != null) {
            Map<String, Iv> b = new HashMap<String, Iv>();
            for (int i = 0; i < c.args.size(); i++) {
                b.put(f.names.get(i), eval(c.args.get(i), env));
            }
            bound.add(b);
            try {
                return eval(Infer.exprBody(f.def), env);
            } finally {
                bound.remove(bound.size() - 1);
            }
        }
        return Iv.TOP;
    }

    static Iv binop(String op, Iv l, Iv r, Expr right) {
        switch (op) {
            case "+":
                return new Iv(add(l.lo, r.lo), add(l.hi, r.hi));
            case "-":
                return new Iv(l.lo == null || r.hi == null ? null : l.lo.subtract(r.hi),
                        l.hi == null || r.lo == null ? null : l.hi.subtract(r.lo));
            case "*":
                return mul(l, r);
            case "&":
                return and(l, r);
            case "|":
            case "^":
                return bits(l, r);
            case "<<": {
                BigInteger c = literal(right);
                if (c == null || c.signum() < 0 || c.bitLength() > 7 || l.lo == null || l.hi == null) {
                    return Iv.TOP;
                }
                return new Iv(l.lo.shiftLeft(c.intValue()), l.hi.shiftLeft(c.intValue()));
            }
            case ">>": {
                BigInteger c = literal(right);
                if (c == null || c.signum() < 0 || c.bitLength() > 7) {
                    return Iv.TOP;
                }
                return new Iv(l.lo == null ? null : l.lo.shiftRight(c.intValue()),
                        l.hi == null ? null : l.hi.shiftRight(c.intValue()));
            }
            case "%":
                if (r.lo != null && r.lo.signum() > 0) {
                    return new Iv(BigInteger.ZERO, r.hi == null ? null : r.hi.subtract(BigInteger.ONE));
                }
                return Iv.TOP;
            case "//":
                if (r.lo != null && r.lo.signum() > 0 && l.lo != null && l.hi != null) {
                    BigInteger lo = l.lo.signum() >= 0 ? l.lo.divide(r.hi == null ? l.lo.add(BigInteger.ONE) : r.hi)
                            : floorDiv(l.lo, r.lo);
                    return new Iv(lo, l.hi.signum() >= 0 ? l.hi.divide(r.lo) : floorDiv(l.hi, r.hi == null ? r.lo : r.hi));
                }
                return Iv.TOP;
            default:
                return Iv.TOP;
        }
    }

    private static BigInteger floorDiv(BigInteger a, BigInteger b) {
        BigInteger[] qr = a.divideAndRemainder(b);
        return qr[1].signum() != 0 && (qr[1].signum() != b.signum()) ? qr[0].subtract(BigInteger.ONE) : qr[0];
    }

    private static BigInteger add(BigInteger a, BigInteger b) {
        return a == null || b == null ? null : a.add(b);
    }

    private static Iv mul(Iv l, Iv r) {
        if (l.lo == null || l.hi == null || r.lo == null || r.hi == null) {
            return Iv.TOP;
        }
        BigInteger a = l.lo.multiply(r.lo);
        BigInteger b = l.lo.multiply(r.hi);
        BigInteger c = l.hi.multiply(r.lo);
        BigInteger d = l.hi.multiply(r.hi);
        return new Iv(a.min(b).min(c).min(d), a.max(b).max(c).max(d));
    }

    /// `&`: with a non-negative side the result lies in [0, its maximum],
    /// whatever the other side is.
    private static Iv and(Iv l, Iv r) {
        boolean ln = l.lo != null && l.lo.signum() >= 0;
        boolean rn = r.lo != null && r.lo.signum() >= 0;
        if (ln && rn) {
            return new Iv(BigInteger.ZERO, l.hi == null ? r.hi : r.hi == null ? l.hi : l.hi.min(r.hi));
        }
        if (ln) {
            return new Iv(BigInteger.ZERO, l.hi);
        }
        if (rn) {
            return new Iv(BigInteger.ZERO, r.hi);
        }
        return bits(l, r);
    }

    /// `|`, `^` (and `&` of two possibly negative values): within the
    /// two's complement width of the wider operand.
    private static Iv bits(Iv l, Iv r) {
        if (l.lo == null || l.hi == null || r.lo == null || r.hi == null) {
            return Iv.TOP;
        }
        int w = Math.max(Math.max(l.lo.bitLength(), l.hi.bitLength()), Math.max(r.lo.bitLength(), r.hi.bitLength()));
        BigInteger lim = BigInteger.ONE.shiftLeft(w);
        if (l.lo.signum() >= 0 && r.lo.signum() >= 0) {
            return new Iv(BigInteger.ZERO, lim.subtract(BigInteger.ONE));
        }
        return new Iv(lim.negate(), lim.subtract(BigInteger.ONE));
    }

    // ---- inlined calls, for the emitter ---------------------------------

    /// Binds the parameters of an expression function the emitter inlines,
    /// until the matching `pop`.
    void push(Map<String, Iv> params) {
        bound.add(params);
    }

    void pop() {
        bound.remove(bound.size() - 1);
    }

    boolean inlining() {
        return !bound.isEmpty();
    }
}
