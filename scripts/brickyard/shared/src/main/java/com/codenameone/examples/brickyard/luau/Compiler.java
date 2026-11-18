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
package com.codenameone.examples.brickyard.luau;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

/// Compiles the syntax tree to [Proto]s. A first pass resolves every name
/// so a local captured by a closure is known before its declaration is
/// compiled; such a local lives in a [Cell], every other one in a slot.
final class Compiler {
    static final class Var {
        final String name;
        final Object fn;
        boolean captured;
        int slot;

        Var(String name, Object fn) {
            this.name = name;
            this.fn = fn;
        }
    }

    private final IdentityHashMap<Object, Var> uses = new IdentityHashMap<Object, Var>();
    private final IdentityHashMap<Object, Var[]> decls = new IdentityHashMap<Object, Var[]>();
    private final String chunk;

    Compiler(String chunk) {
        this.chunk = chunk;
    }

    Proto compile(Ast.Function main) {
        new Resolver().function(main);
        return new FuncState(null, main).build();
    }

    // ------------------------------------------------------------ resolver

    private final class Resolver {
        private final ArrayList<Var> scope = new ArrayList<Var>();
        private Object fn;

        private Var declare(String name) {
            Var v = new Var(name, fn);
            scope.add(v);
            return v;
        }

        private void function(Ast.Function f) {
            Object saved = fn;
            int mark = scope.size();
            fn = f;
            Var[] ps = new Var[f.params.size()];
            for (int i = 0; i < ps.length; i++) {
                ps[i] = declare(f.params.get(i));
            }
            decls.put(f, ps);
            block(f.body);
            truncate(mark);
            fn = saved;
        }

        private void truncate(int mark) {
            while (scope.size() > mark) {
                scope.remove(scope.size() - 1);
            }
        }

        private void block(Ast.Block b) {
            int mark = scope.size();
            for (Ast.Stat s : b.stats) {
                stat(s);
            }
            truncate(mark);
        }

        private void stat(Ast.Stat s) {
            if (s instanceof Ast.Local) {
                Ast.Local l = (Ast.Local) s;
                exprs(l.values);
                Var[] vs = new Var[l.names.size()];
                for (int i = 0; i < vs.length; i++) {
                    vs[i] = declare(l.names.get(i));
                }
                decls.put(l, vs);
            } else if (s instanceof Ast.LocalFunction) {
                Ast.LocalFunction lf = (Ast.LocalFunction) s;
                decls.put(lf, new Var[] {declare(lf.name)});
                function(lf.fn);
            } else if (s instanceof Ast.Assign) {
                Ast.Assign a = (Ast.Assign) s;
                exprs(a.values);
                exprs(a.targets);
            } else if (s instanceof Ast.CompoundAssign) {
                Ast.CompoundAssign c = (Ast.CompoundAssign) s;
                expr(c.target);
                expr(c.value);
            } else if (s instanceof Ast.ExprStat) {
                expr(((Ast.ExprStat) s).call);
            } else if (s instanceof Ast.Do) {
                block(((Ast.Do) s).body);
            } else if (s instanceof Ast.While) {
                expr(((Ast.While) s).cond);
                block(((Ast.While) s).body);
            } else if (s instanceof Ast.Repeat) {
                Ast.Repeat r = (Ast.Repeat) s;
                int mark = scope.size();
                for (Ast.Stat x : r.body.stats) {
                    stat(x);
                }
                expr(r.cond);
                truncate(mark);
            } else if (s instanceof Ast.If) {
                Ast.If i = (Ast.If) s;
                for (int k = 0; k < i.conds.size(); k++) {
                    expr(i.conds.get(k));
                    block(i.blocks.get(k));
                }
                if (i.orElse != null) {
                    block(i.orElse);
                }
            } else if (s instanceof Ast.NumFor) {
                Ast.NumFor f = (Ast.NumFor) s;
                expr(f.start);
                expr(f.limit);
                if (f.step != null) {
                    expr(f.step);
                }
                int mark = scope.size();
                decls.put(f, new Var[] {declare(f.var)});
                block(f.body);
                truncate(mark);
            } else if (s instanceof Ast.GenFor) {
                Ast.GenFor f = (Ast.GenFor) s;
                exprs(f.exprs);
                int mark = scope.size();
                Var[] vs = new Var[f.vars.size()];
                for (int i = 0; i < vs.length; i++) {
                    vs[i] = declare(f.vars.get(i));
                }
                decls.put(f, vs);
                block(f.body);
                truncate(mark);
            } else if (s instanceof Ast.Return) {
                exprs(((Ast.Return) s).values);
            }
        }

        private void exprs(ArrayList<Ast.Expr> es) {
            for (Ast.Expr e : es) {
                expr(e);
            }
        }

        private void expr(Ast.Expr e) {
            if (e == null) {
                return;
            }
            if (e instanceof Ast.Name) {
                String n = ((Ast.Name) e).name;
                for (int i = scope.size() - 1; i >= 0; i--) {
                    Var v = scope.get(i);
                    if (v.name.equals(n)) {
                        if (v.fn != fn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                            v.captured = true;
                        }
                        uses.put(e, v);
                        return;
                    }
                }
            } else if (e instanceof Ast.Index) {
                expr(((Ast.Index) e).obj);
                expr(((Ast.Index) e).key);
            } else if (e instanceof Ast.Call) {
                expr(((Ast.Call) e).fn);
                exprs(((Ast.Call) e).args);
            } else if (e instanceof Ast.Function) {
                function((Ast.Function) e);
            } else if (e instanceof Ast.Binop) {
                expr(((Ast.Binop) e).a);
                expr(((Ast.Binop) e).b);
            } else if (e instanceof Ast.Unop) {
                expr(((Ast.Unop) e).a);
            } else if (e instanceof Ast.Paren) {
                expr(((Ast.Paren) e).e);
            } else if (e instanceof Ast.IfExpr) {
                Ast.IfExpr i = (Ast.IfExpr) e;
                exprs(i.conds);
                exprs(i.values);
                expr(i.orElse);
            } else if (e instanceof Ast.Interp) {
                exprs(((Ast.Interp) e).exprs);
            } else if (e instanceof Ast.TableCons) {
                Ast.TableCons t = (Ast.TableCons) e;
                for (int i = 0; i < t.values.size(); i++) {
                    expr(t.keys.get(i));
                    expr(t.values.get(i));
                }
            }
        }
    }

    // ------------------------------------------------------------ codegen

    private static final class Loop {
        final ArrayList<Integer> breaks = new ArrayList<Integer>();
        final ArrayList<Integer> continues = new ArrayList<Integer>();
    }

    private final class FuncState {
        final FuncState parent;
        final Ast.Function fn;
        int[] code = new int[64];
        int[] lines = new int[64];
        int pc;
        int line;
        final ArrayList<Object> consts = new ArrayList<Object>();
        final HashMap<Object, Integer> constIdx = new HashMap<Object, Integer>();
        final ArrayList<Proto> protos = new ArrayList<Proto>();
        final ArrayList<Var> upvars = new ArrayList<Var>();
        final ArrayList<Boolean> upFromLocal = new ArrayList<Boolean>();
        final ArrayList<Integer> upIndex = new ArrayList<Integer>();
        final ArrayList<Loop> loops = new ArrayList<Loop>();
        int nextSlot;
        int maxSlots;

        FuncState(FuncState parent, Ast.Function fn) {
            this.parent = parent;
            this.fn = fn;
            this.line = fn.line;
        }

        Proto build() {
            Var[] ps = decls.get(fn);
            for (Var v : ps) {
                v.slot = allocSlot();
            }
            for (Var v : ps) {
                if (v.captured) {
                    emit(Op.LGET, v.slot);
                    emit(Op.CNEW, v.slot);
                }
            }
            block(fn.body);
            emit(Op.RET, 0, 0);
            Proto p = new Proto();
            p.code = new int[pc];
            System.arraycopy(code, 0, p.code, 0, pc);
            p.lines = new int[pc];
            System.arraycopy(lines, 0, p.lines, 0, pc);
            p.consts = consts.toArray();
            p.protos = protos.toArray(new Proto[protos.size()]);
            p.numParams = ps.length;
            p.vararg = fn.vararg;
            p.numSlots = maxSlots;
            p.upFromLocal = new boolean[upvars.size()];
            p.upIndex = new int[upvars.size()];
            for (int i = 0; i < p.upIndex.length; i++) {
                p.upFromLocal[i] = upFromLocal.get(i).booleanValue();
                p.upIndex[i] = upIndex.get(i).intValue();
            }
            p.name = fn.name;
            p.chunk = chunk;
            p.lineDefined = fn.line;
            return p;
        }

        int allocSlot() {
            int s = nextSlot++;
            if (nextSlot > maxSlots) {
                maxSlots = nextSlot;
            }
            return s;
        }

        private void put(int v) {
            if (pc == code.length) {
                int[] nc = new int[pc * 2];
                System.arraycopy(code, 0, nc, 0, pc);
                code = nc;
                int[] nl = new int[pc * 2];
                System.arraycopy(lines, 0, nl, 0, pc);
                lines = nl;
            }
            lines[pc] = line;
            code[pc++] = v;
        }

        void emit(int op) {
            put(op);
        }

        void emit(int op, int a) {
            put(op);
            put(a);
        }

        void emit(int op, int a, int b) {
            put(op);
            put(a);
            put(b);
        }

        void emit(int op, int a, int b, int c) {
            put(op);
            put(a);
            put(b);
            put(c);
        }

        /// Emits a jump with a placeholder target; returns the operand's index.
        int jump(int op) {
            put(op);
            put(-1);
            return pc - 1;
        }

        void patch(int at, int target) {
            code[at] = target;
        }

        void patchHere(int at) {
            code[at] = pc;
        }

        int k(Object v) {
            Object key = v instanceof Double ? (Object) ("#n" + Double.doubleToLongBits(((Double) v).doubleValue())) : v;
            Integer i = constIdx.get(key);
            if (i == null) {
                i = Integer.valueOf(consts.size());
                consts.add(v);
                constIdx.put(key, i);
            }
            return i.intValue();
        }

        int upvalue(Var v) {
            int i = upvars.indexOf(v);
            if (i >= 0) {
                return i;
            }
            if (parent.fn == v.fn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                upFromLocal.add(Boolean.TRUE);
                upIndex.add(Integer.valueOf(v.slot));
            } else {
                upFromLocal.add(Boolean.FALSE);
                upIndex.add(Integer.valueOf(parent.upvalue(v)));
            }
            upvars.add(v);
            return upvars.size() - 1;
        }

        void block(Ast.Block b) {
            int mark = nextSlot;
            for (Ast.Stat s : b.stats) {
                stat(s);
            }
            nextSlot = mark;
        }

        void storeVar(Var v) {
            if (v.captured) {
                emit(Op.CNEW, v.slot);
            } else {
                emit(Op.LSET, v.slot);
            }
        }

        void stat(Ast.Stat s) {
            line = s.line;
            if (s instanceof Ast.Local) {
                Ast.Local l = (Ast.Local) s;
                Var[] vs = decls.get(l);
                exprList(l.values, vs.length);
                for (Var v : vs) {
                    v.slot = allocSlot();
                }
                for (int i = vs.length - 1; i >= 0; i--) {
                    storeVar(vs[i]);
                }
            } else if (s instanceof Ast.LocalFunction) {
                Ast.LocalFunction lf = (Ast.LocalFunction) s;
                Var v = decls.get(lf)[0];
                v.slot = allocSlot();
                if (v.captured) {
                    emit(Op.NIL);
                    emit(Op.CNEW, v.slot);
                    closure(lf.fn);
                    emit(Op.CSET, v.slot);
                } else {
                    closure(lf.fn);
                    emit(Op.LSET, v.slot);
                }
            } else if (s instanceof Ast.Assign) {
                assign((Ast.Assign) s);
            } else if (s instanceof Ast.CompoundAssign) {
                compound((Ast.CompoundAssign) s);
            } else if (s instanceof Ast.ExprStat) {
                call(((Ast.ExprStat) s).call, 0);
            } else if (s instanceof Ast.Do) {
                block(((Ast.Do) s).body);
            } else if (s instanceof Ast.While) {
                Ast.While w = (Ast.While) s;
                int top = pc;
                expr(w.cond);
                int exit = jump(Op.JF);
                Loop lp = new Loop();
                loops.add(lp);
                block(w.body);
                loops.remove(loops.size() - 1);
                emit(Op.JMP, top);
                patchHere(exit);
                patchAll(lp.continues, top);
                patchAll(lp.breaks, pc);
            } else if (s instanceof Ast.Repeat) {
                Ast.Repeat r = (Ast.Repeat) s;
                int top = pc;
                int mark = nextSlot;
                Loop lp = new Loop();
                loops.add(lp);
                for (Ast.Stat x : r.body.stats) {
                    stat(x);
                }
                loops.remove(loops.size() - 1);
                patchAll(lp.continues, pc);
                line = s.line;
                expr(r.cond);
                emit(Op.JF, top);
                nextSlot = mark;
                patchAll(lp.breaks, pc);
            } else if (s instanceof Ast.If) {
                ifStat((Ast.If) s);
            } else if (s instanceof Ast.NumFor) {
                numFor((Ast.NumFor) s);
            } else if (s instanceof Ast.GenFor) {
                genFor((Ast.GenFor) s);
            } else if (s instanceof Ast.Return) {
                Ast.Return r = (Ast.Return) s;
                if (r.values.size() == 1 && r.values.get(0) instanceof Ast.Call) {
                    call((Ast.Call) r.values.get(0), -1);
                    emit(Op.RET, 0, 1);
                } else {
                    int[] fm = exprList(r.values, -1);
                    emit(Op.RET, fm[0], fm[1]);
                }
            } else if (s instanceof Ast.Break) {
                if (loops.isEmpty()) {
                    throw new LuaError(chunk + ":" + s.line + ": break outside a loop statement");
                }
                loops.get(loops.size() - 1).breaks.add(Integer.valueOf(jump(Op.JMP)));
            } else if (s instanceof Ast.Continue) {
                if (loops.isEmpty()) {
                    throw new LuaError(chunk + ":" + s.line + ": continue outside a loop statement");
                }
                loops.get(loops.size() - 1).continues.add(Integer.valueOf(jump(Op.JMP)));
            }
        }

        void patchAll(ArrayList<Integer> at, int target) {
            for (Integer i : at) {
                patch(i.intValue(), target);
            }
        }

        void ifStat(Ast.If s) {
            ArrayList<Integer> ends = new ArrayList<Integer>();
            for (int i = 0; i < s.conds.size(); i++) {
                expr(s.conds.get(i));
                int next = jump(Op.JF);
                block(s.blocks.get(i));
                if (i < s.conds.size() - 1 || s.orElse != null) {
                    ends.add(Integer.valueOf(jump(Op.JMP)));
                }
                patchHere(next);
            }
            if (s.orElse != null) {
                block(s.orElse);
            }
            patchAll(ends, pc);
        }

        void numFor(Ast.NumFor f) {
            int mark = nextSlot;
            int base = allocSlot();
            allocSlot();
            allocSlot();
            expr(f.start);
            emit(Op.LSET, base);
            expr(f.limit);
            emit(Op.LSET, base + 1);
            if (f.step != null) {
                expr(f.step);
            } else {
                emit(Op.K, k(Interp.num(1)));
            }
            emit(Op.LSET, base + 2);
            emit(Op.FORPREP, base);
            int top = pc;
            put(Op.FORTEST);
            put(base);
            put(-1);
            int exit = pc - 1;
            Var v = decls.get(f)[0];
            int inner = nextSlot;
            v.slot = allocSlot();
            emit(Op.LGET, base);
            storeVar(v);
            Loop lp = new Loop();
            loops.add(lp);
            block(f.body);
            loops.remove(loops.size() - 1);
            nextSlot = inner;
            patchAll(lp.continues, pc);
            emit(Op.FORINC, base);
            emit(Op.JMP, top);
            patchHere(exit);
            patchAll(lp.breaks, pc);
            nextSlot = mark;
        }

        void genFor(Ast.GenFor f) {
            int mark = nextSlot;
            int base = allocSlot();
            allocSlot();
            allocSlot();
            exprList(f.exprs, 3);
            emit(Op.LSET, base + 2);
            emit(Op.LSET, base + 1);
            emit(Op.LSET, base);
            emit(Op.GFORPREP, base);
            Var[] vs = decls.get(f);
            int top = pc;
            emit(Op.LGET, base);
            emit(Op.LGET, base + 1);
            emit(Op.LGET, base + 2);
            emit(Op.CALL, 2, 0, vs.length);
            int inner = nextSlot;
            for (Var v : vs) {
                v.slot = allocSlot();
            }
            for (int i = vs.length - 1; i >= 1; i--) {
                storeVar(vs[i]);
            }
            emit(Op.DUP);
            emit(Op.LSET, base + 2);
            emit(Op.DUP);
            int exit = jump(Op.JNIL);
            storeVar(vs[0]);
            Loop lp = new Loop();
            loops.add(lp);
            block(f.body);
            loops.remove(loops.size() - 1);
            nextSlot = inner;
            patchAll(lp.continues, top);
            emit(Op.JMP, top);
            patchHere(exit);
            emit(Op.POP, 1);
            patchAll(lp.breaks, pc);
            nextSlot = mark;
        }

        void assign(Ast.Assign a) {
            if (a.targets.size() == 1) {
                Ast.Expr t = a.targets.get(0);
                if (t instanceof Ast.Index) {
                    Ast.Index ix = (Ast.Index) t;
                    expr(ix.obj);
                    if (ix.key instanceof Ast.Str) {
                        exprList(a.values, 1);
                        emit(Op.FSET, k(((Ast.Str) ix.key).v));
                    } else {
                        expr(ix.key);
                        exprList(a.values, 1);
                        emit(Op.ISET);
                    }
                    return;
                }
                exprList(a.values, 1);
                storeName((Ast.Name) t);
                return;
            }
            int n = a.targets.size();
            int mark = nextSlot;
            int tmp = nextSlot;
            for (int i = 0; i < n; i++) {
                allocSlot();
            }
            exprList(a.values, n);
            for (int i = n - 1; i >= 0; i--) {
                emit(Op.LSET, tmp + i);
            }
            for (int i = 0; i < n; i++) {
                Ast.Expr t = a.targets.get(i);
                if (t instanceof Ast.Index) {
                    Ast.Index ix = (Ast.Index) t;
                    expr(ix.obj);
                    expr(ix.key);
                    emit(Op.LGET, tmp + i);
                    emit(Op.ISET);
                } else {
                    emit(Op.LGET, tmp + i);
                    storeName((Ast.Name) t);
                }
            }
            nextSlot = mark;
        }

        void compound(Ast.CompoundAssign c) {
            int op = Op.binop(c.op);
            if (c.target instanceof Ast.Name) {
                expr(c.target);
                expr(c.value);
                emit(Op.BIN, op);
                storeName((Ast.Name) c.target);
                return;
            }
            Ast.Index ix = (Ast.Index) c.target;
            int mark = nextSlot;
            int to = allocSlot();
            int tk = allocSlot();
            expr(ix.obj);
            emit(Op.LSET, to);
            expr(ix.key);
            emit(Op.LSET, tk);
            emit(Op.LGET, to);
            emit(Op.LGET, tk);
            emit(Op.LGET, to);
            emit(Op.LGET, tk);
            emit(Op.IGET);
            expr(c.value);
            emit(Op.BIN, op);
            emit(Op.ISET);
            nextSlot = mark;
        }

        void storeName(Ast.Name n) {
            Var v = uses.get(n);
            if (v == null) {
                emit(Op.GSET, k(n.name));
            } else if (v.fn == fn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                emit(v.captured ? Op.CSET : Op.LSET, v.slot);
            } else {
                emit(Op.USET, upvalue(v));
            }
        }

        /// Compiles `es` to exactly `want` values, or with want = -1 to all of
        /// them; returns {fixed count, 1 when the last one is open}.
        int[] exprList(ArrayList<Ast.Expr> es, int want) {
            int n = es.size();
            if (want < 0) {
                for (int i = 0; i < n; i++) {
                    Ast.Expr e = es.get(i);
                    if (i == n - 1 && isMulti(e)) {
                        multi(e, -1);
                        return new int[] {n - 1, 1};
                    }
                    expr(e);
                }
                return new int[] {n, 0};
            }
            for (int i = 0; i < n; i++) {
                Ast.Expr e = es.get(i);
                if (i == n - 1 && isMulti(e) && want - i >= 0) {
                    multi(e, want - i);
                    return new int[] {want, 0};
                }
                expr(e);
                if (i >= want) {
                    emit(Op.POP, 1);
                }
            }
            for (int i = n; i < want; i++) {
                emit(Op.NIL);
            }
            return new int[] {want, 0};
        }

        boolean isMulti(Ast.Expr e) {
            return e instanceof Ast.Call || e instanceof Ast.Vararg;
        }

        void multi(Ast.Expr e, int nret) {
            if (e instanceof Ast.Call) {
                call((Ast.Call) e, nret);
            } else {
                emit(Op.VARARG, nret);
            }
        }

        void call(Ast.Call c, int nret) {
            int saved = line;
            if (c.method != null) {
                expr(c.fn);
                line = c.line;
                emit(Op.SELF, k(c.method));
                int[] fm = exprList(c.args, -1);
                line = c.line;
                emit(Op.CALL, fm[0] + 1, fm[1], nret);
            } else {
                expr(c.fn);
                int[] fm = exprList(c.args, -1);
                line = c.line;
                emit(Op.CALL, fm[0], fm[1], nret);
            }
            line = saved;
        }

        void closure(Ast.Function f) {
            FuncState child = new FuncState(this, f);
            Proto p = child.build();
            protos.add(p);
            emit(Op.CLOSURE, protos.size() - 1);
        }

        void expr(Ast.Expr e) {
            int saved = line;
            if (e.line > 0) {
                line = e.line;
            }
            exprInner(e);
            line = saved;
        }

        void exprInner(Ast.Expr e) {
            if (e instanceof Ast.Nil) {
                emit(Op.NIL);
            } else if (e instanceof Ast.Bool) {
                emit(((Ast.Bool) e).v ? Op.TRUE : Op.FALSE);
            } else if (e instanceof Ast.Num) {
                emit(Op.K, k(Interp.num(((Ast.Num) e).v)));
            } else if (e instanceof Ast.Str) {
                emit(Op.K, k(((Ast.Str) e).v));
            } else if (e instanceof Ast.Vararg) {
                if (!fn.vararg) {
                    throw new LuaError(chunk + ":" + e.line + ": cannot use '...' outside a vararg function");
                }
                emit(Op.VARARG, 1);
            } else if (e instanceof Ast.Name) {
                Var v = uses.get(e);
                if (v == null) {
                    emit(Op.GGET, k(((Ast.Name) e).name));
                } else if (v.fn == fn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                    emit(v.captured ? Op.CGET : Op.LGET, v.slot);
                } else {
                    emit(Op.UGET, upvalue(v));
                }
            } else if (e instanceof Ast.Index) {
                Ast.Index ix = (Ast.Index) e;
                expr(ix.obj);
                if (ix.key instanceof Ast.Str) {
                    emit(Op.FGET, k(((Ast.Str) ix.key).v));
                } else {
                    expr(ix.key);
                    emit(Op.IGET);
                }
            } else if (e instanceof Ast.Call) {
                call((Ast.Call) e, 1);
            } else if (e instanceof Ast.Function) {
                closure((Ast.Function) e);
            } else if (e instanceof Ast.Paren) {
                expr(((Ast.Paren) e).e);
            } else if (e instanceof Ast.Binop) {
                Ast.Binop b = (Ast.Binop) e;
                if ("and".equals(b.op) || "or".equals(b.op)) {
                    expr(b.a);
                    int j = jump("and".equals(b.op) ? Op.AND : Op.OR);
                    expr(b.b);
                    patchHere(j);
                } else {
                    expr(b.a);
                    expr(b.b);
                    line = b.line;
                    emit(Op.BIN, Op.binop(b.op));
                }
            } else if (e instanceof Ast.Unop) {
                Ast.Unop u = (Ast.Unop) e;
                expr(u.a);
                emit(Op.UN, "-".equals(u.op) ? Op.NEG : "not".equals(u.op) ? Op.NOT : Op.LEN);
            } else if (e instanceof Ast.IfExpr) {
                Ast.IfExpr i = (Ast.IfExpr) e;
                ArrayList<Integer> ends = new ArrayList<Integer>();
                for (int c = 0; c < i.conds.size(); c++) {
                    expr(i.conds.get(c));
                    int next = jump(Op.JF);
                    expr(i.values.get(c));
                    ends.add(Integer.valueOf(jump(Op.JMP)));
                    patchHere(next);
                }
                expr(i.orElse);
                patchAll(ends, pc);
            } else if (e instanceof Ast.Interp) {
                Ast.Interp in = (Ast.Interp) e;
                int parts = 0;
                for (int i = 0; i < in.parts.size(); i++) {
                    if (in.parts.get(i).length() > 0) {
                        emit(Op.K, k(in.parts.get(i)));
                        parts++;
                    }
                    if (i < in.exprs.size()) {
                        expr(in.exprs.get(i));
                        parts++;
                    }
                }
                emit(Op.CONCATN, parts);
            } else if (e instanceof Ast.TableCons) {
                table((Ast.TableCons) e);
            } else {
                throw new LuaError(chunk + ": cannot compile " + e.getClass().getName());
            }
        }

        void table(Ast.TableCons t) {
            int npos = 0;
            for (Ast.Expr kx : t.keys) {
                if (kx == null) {
                    npos++;
                }
            }
            emit(Op.NEWT, npos);
            int index = 1;
            int pending = 0;
            int n = t.values.size();
            for (int i = 0; i < n; i++) {
                Ast.Expr kx = t.keys.get(i);
                Ast.Expr v = t.values.get(i);
                if (kx == null) {
                    if (i == n - 1 && isMulti(v)) {
                        multi(v, -1);
                        emit(Op.TSETL, index, pending, 1);
                        return;
                    }
                    expr(v);
                    pending++;
                    if (pending == 32) {
                        emit(Op.TSETL, index, pending, 0);
                        index += pending;
                        pending = 0;
                    }
                } else {
                    if (pending > 0) {
                        emit(Op.TSETL, index, pending, 0);
                        index += pending;
                        pending = 0;
                    }
                    if (kx instanceof Ast.Str) {
                        expr(v);
                        emit(Op.TSETF, k(((Ast.Str) kx).v));
                    } else {
                        expr(kx);
                        expr(v);
                        emit(Op.TSETK);
                    }
                }
            }
            if (pending > 0) {
                emit(Op.TSETL, index, pending, 0);
            }
        }
    }
}
