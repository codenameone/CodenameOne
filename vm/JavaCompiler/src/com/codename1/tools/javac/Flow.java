/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.tools.javac;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Definite assignment (JLS 16) for local variables and blank final fields, plus unreachable-statement
 * detection (JLS 14.22). A read of a local that is not definitely assigned would
 * otherwise reach the JVM verifier (or, translated, read garbage), so it is a
 * compile error here as in javac.
 *
 * <p>State is the set of definitely assigned locals; {@code null} stands for the
 * universal set, which is what an unreachable point has.
 */
final class Flow {
    private final Compiler compiler;
    private Tree.CompilationUnit unit;
    private Map<VarSymbol, Integer> index;
    private int nextIndex;
    private BitSet inits;
    private boolean alive;
    private BitSet whenTrue;
    private BitSet whenFalse;
    /** Pending jumps: {target tree, BitSet inits at the jump (null = universal), Boolean isContinue}. */
    private List<Object[]> exits;
    /** Enclosing jump targets: {tree, label}. */
    private List<Object[]> targets;
    /**
     * The blank final fields a constructor body must assign before it returns
     * (JLS 16.9); null outside such a body. A {@code return} checks them too.
     */
    private List<VarSymbol> ctorBlankFinals;
    /** Where a constructor that falls off its end reports an unassigned field: its closing brace. */
    private int ctorEndPos;

    Flow(Compiler compiler) {
        this.compiler = compiler;
    }

    void checkClass(ClassSymbol c) {
        if (c.decl == null) {
            return;
        }
        unit = c.unit;
        // Blank finals (JLS 16.8, 16.9): a final field without an initializer must be
        // assigned exactly once -- a static one by the static initializers, an instance
        // one by the instance initializers or else by every constructor that does not
        // delegate to this(...). Record fields are assigned by the canonical constructor
        // the compiler completes, so records are left to it.
        List<VarSymbol> staticBlanks = new ArrayList<VarSymbol>();
        List<VarSymbol> instanceBlanks = new ArrayList<VarSymbol>();
        List<Tree.VarDef> blankDecls = new ArrayList<Tree.VarDef>();
        if (!c.isRecord()) {
            for (Tree member : c.decl.members) {
                if (member instanceof Tree.VarDef) {
                    Tree.VarDef v = (Tree.VarDef) member;
                    if (v.init == null && v.sym != null && v.sym.isFinal()) {
                        (v.sym.isStatic() ? staticBlanks : instanceBlanks).add(v.sym);
                        blankDecls.add(v);
                    }
                }
            }
        }

        // Static initialization, in source order.
        reset();
        declareAll(staticBlanks);
        for (Tree member : c.decl.members) {
            if (member instanceof Tree.VarDef && ((Tree.VarDef) member).sym != null
                    && ((Tree.VarDef) member).sym.isStatic()) {
                scanExpr(((Tree.VarDef) member).init);
            } else if (member instanceof Tree.Block && ((Tree.Block) member).isStatic) {
                scanStat(member);
            }
        }
        if (alive) {
            for (Tree.VarDef v : blankDecls) {
                if (v.sym.isStatic() && !isAssigned(v.sym)) {
                    error(v.pos, "variable " + v.name + " might not have been initialized");
                }
            }
        }

        // Instance initialization, in source order: its end state is where every
        // constructor that calls super(...) starts.
        reset();
        declareAll(instanceBlanks);
        for (Tree member : c.decl.members) {
            if (member instanceof Tree.VarDef && ((Tree.VarDef) member).sym != null
                    && !((Tree.VarDef) member).sym.isStatic()) {
                scanExpr(((Tree.VarDef) member).init);
            } else if (member instanceof Tree.Block && !((Tree.Block) member).isStatic) {
                scanStat(member);
            }
        }
        BitSet afterInit = copy(inits);
        boolean initAlive = alive;
        boolean hasCtor = false;
        for (Tree member : c.decl.members) {
            if (member instanceof Tree.MethodDecl && ((Tree.MethodDecl) member).sym != null
                    && ((Tree.MethodDecl) member).sym.isConstructor()) {
                hasCtor = true;
            }
        }
        if (!hasCtor && initAlive) {
            // The default constructor runs the instance initializers and nothing else.
            for (Tree.VarDef v : blankDecls) {
                if (!v.sym.isStatic() && !isAssigned(v.sym)) {
                    error(v.pos, "variable " + v.name + " not initialized in the default constructor");
                }
            }
        }

        for (Tree member : c.decl.members) {
            if (!(member instanceof Tree.MethodDecl)) {
                continue;
            }
            Tree.MethodDecl m = (Tree.MethodDecl) member;
            if (m.body == null || m.sym == null) {
                continue;
            }
            reset();
            boolean ctor = m.sym.isConstructor() && !m.compactConstructor;
            if (ctor) {
                declareAll(instanceBlanks);
                Tree.MethodCall explicit = Attr.explicitConstructorCall(m.body);
                if (explicit != null && !explicit.superCall) {
                    // this(...) has assigned every one of them already.
                    for (VarSymbol f : instanceBlanks) {
                        assign(f);
                    }
                } else {
                    inits = copy(afterInit);
                    alive = initAlive;
                    ctorBlankFinals = instanceBlanks;
                    ctorEndPos = m.body.endPos;
                }
            }
            for (VarSymbol p : m.sym.paramSyms) {
                declare(p);
                assign(p);
            }
            scanStat(m.body);
            if (ctorBlankFinals != null && alive) {
                checkBlankFinalsAssigned(ctorEndPos);
            }
            ctorBlankFinals = null;
        }
    }

    private void declareAll(List<VarSymbol> vars) {
        for (VarSymbol v : vars) {
            declare(v);
        }
    }

    private boolean isAssigned(VarSymbol v) {
        Integer i = index.get(v);
        return i == null || inits == null || inits.get(i.intValue());
    }

    /** At a constructor's normal exit: each blank final must be definitely assigned. */
    private void checkBlankFinalsAssigned(int pos) {
        for (VarSymbol f : ctorBlankFinals) {
            if (!isAssigned(f)) {
                error(pos, "variable " + f.name + " might not have been initialized");
            }
        }
    }

    private void reset() {
        index = new IdentityHashMap<VarSymbol, Integer>();
        nextIndex = 0;
        inits = new BitSet();
        alive = true;
        exits = new ArrayList<Object[]>();
        targets = new ArrayList<Object[]>();
    }

    private void error(int pos, String message) {
        compiler.error(unit, pos, message);
    }

    // ------------------------------------------------------------------ sets

    /*
     * Each variable has two bits in the state: index i is "definitely assigned" and i + 1 is
     * "definitely unassigned" (JLS 16). Both meet by intersection where paths join and are
     * universal on a dead path, so every merge below handles them alike; only loops differ,
     * where an assignment in the body reaches the loop head again (see loopAssignments).
     */

    private void declare(VarSymbol v) {
        if (v != null && !index.containsKey(v)) {
            int i = nextIndex;
            nextIndex += 2;
            index.put(v, Integer.valueOf(i));
            if (inits != null) {
                inits.set(i + 1);
            }
            if (v.kind == VarSymbol.Kind.LOCAL) {
                v.flowChecked = true;
            }
        }
    }

    private void assign(VarSymbol v) {
        Integer i = v == null ? null : index.get(v);
        if (i != null && inits != null) {
            inits.set(i.intValue());
            inits.clear(i.intValue() + 1);
        }
    }

    /**
     * JLS 14.22: the body of a while or for loop whose condition is the constant false can
     * never run, and javac reports it (unlike if (false), which is allowed on purpose).
     */
    private void deadWhenFalse(Tree cond, Tree body) {
        if (alive && cond.constant instanceof Boolean && !((Boolean) cond.constant).booleanValue()) {
            if (body != null && body.pos >= 0) {
                error(body.pos, "unreachable statement");
                // One diagnostic for the body, not another for its first statement.
                deadBodyReported = true;
            }
            markDead();
        }
    }

    /** Set while scanning a loop body already reported unreachable. */
    private boolean deadBodyReported;

    /** An assignment to v here: unless v is definitely unassigned, v is not effectively final. */
    private void assignedHere(VarSymbol v) {
        Integer i = v == null ? null : index.get(v);
        if (i != null && alive && inits != null && !inits.get(i.intValue() + 1)) {
            v.notEffectivelyFinal = true;
        }
    }

    /**
     * After a loop: a variable definitely unassigned on entry that the body (back edge state
     * {@code back}, null when the body never loops back) may assign is assigned while it may
     * already hold a value on the next iteration, and is no longer definitely unassigned after
     * the loop.
     */
    private void loopAssignments(BitSet entry, BitSet back) {
        if (entry == null || back == null) {
            return;
        }
        for (Map.Entry<VarSymbol, Integer> e : index.entrySet()) {
            int du = e.getValue().intValue() + 1;
            if (entry.get(du) && !back.get(du)) {
                e.getKey().notEffectivelyFinal = true;
                if (inits != null) {
                    inits.clear(du);
                }
            }
        }
    }

    /** The state flowing back to a loop's head: the end of the body met with its continues. */
    private BitSet backEdge(Tree loop) {
        boolean[] any = new boolean[1];
        BitSet conts = takeExits(loop, true, any);
        if (alive && any[0]) {
            return meet(inits, conts);
        }
        if (alive) {
            return copy(inits);
        }
        return any[0] ? copy(conts) : null;
    }

    private static BitSet copy(BitSet b) {
        if (b == null) {
            return null;
        }
        // JavaAPI's BitSet has no public clone().
        BitSet r = new BitSet();
        r.or(b);
        return r;
    }

    /** Intersection, where null is the universal set. */
    private static BitSet meet(BitSet a, BitSet b) {
        if (a == null) {
            return copy(b);
        }
        if (b == null) {
            return copy(a);
        }
        BitSet r = copy(a);
        r.and(b);
        return r;
    }

    private void markDead() {
        alive = false;
        inits = null;
    }

    /** Joins the current state with another reachable-or-not state. */
    private void join(BitSet other, boolean otherAlive) {
        if (!otherAlive) {
            return;
        }
        if (!alive) {
            inits = copy(other);
            alive = true;
            return;
        }
        inits = meet(inits, other);
    }

    private void checkRead(VarSymbol v, int pos) {
        Integer i = index.get(v);
        if (i == null || inits == null || !alive) {
            return;
        }
        if (!inits.get(i.intValue())) {
            error(pos, "variable " + v.name + " might not have been initialized");
            inits.set(i.intValue());
        }
    }

    // ------------------------------------------------------------------ jumps

    private Object resolveTarget(String label, boolean isContinue) {
        for (int i = targets.size() - 1; i >= 0; i--) {
            Object[] t = targets.get(i);
            Tree node = (Tree) t[0];
            if (label != null) {
                if (label.equals(t[1])) {
                    if (isContinue && node instanceof Tree.Labeled) {
                        return ((Tree.Labeled) node).body;
                    }
                    return node;
                }
                continue;
            }
            if (node instanceof Tree.Labeled) {
                continue;
            }
            if (isContinue && node instanceof Tree.Switch) {
                continue;
            }
            return node;
        }
        return null;
    }

    private void jump(Object target, boolean isContinue) {
        if (target != null && alive) {
            exits.add(new Object[]{target, copy(inits), Boolean.valueOf(isContinue)});
        }
        markDead();
    }

    /** Removes the exits to target of the given kind; returns their meet, and whether there were any. */
    private BitSet takeExits(Tree target, boolean isContinue, boolean[] any) {
        BitSet result = null;
        boolean found = false;
        for (int i = exits.size() - 1; i >= 0; i--) {
            Object[] e = exits.get(i);
            if (e[0] == target && ((Boolean) e[2]).booleanValue() == isContinue) {
                result = found ? meet(result, (BitSet) e[1]) : copy((BitSet) e[1]);
                if (!found && e[1] == null) {
                    result = null;
                }
                found = true;
                exits.remove(i);
            }
        }
        any[0] = found;
        return result;
    }

    private void pushTarget(Tree node, String label) {
        targets.add(new Object[]{node, label});
    }

    private void popTarget() {
        targets.remove(targets.size() - 1);
    }

    // ------------------------------------------------------------------ statements

    private void scanStats(List<Tree> stats) {
        boolean reported = deadBodyReported;
        for (Tree s : stats) {
            if (!alive && !reported && !(s instanceof Tree.Empty) && !(s instanceof Tree.ClassDecl) && s.pos >= 0) {
                error(s.pos, "unreachable statement");
                reported = true;
            }
            scanStat(s);
        }
    }

    private void scanStat(Tree t) {
        if (t == null) {
            return;
        }
        if (t instanceof Tree.Block) {
            scanStats(((Tree.Block) t).stats);
        } else if (t instanceof Tree.VarDef) {
            Tree.VarDef v = (Tree.VarDef) t;
            declare(v.sym);
            if (v.init != null) {
                scanExpr(v.init);
                assign(v.sym);
            }
        } else if (t instanceof Tree.ExpressionStatement) {
            scanExpr(((Tree.ExpressionStatement) t).expr);
        } else if (t instanceof Tree.If) {
            Tree.If s = (Tree.If) t;
            scanCond(s.cond);
            BitSet f = whenFalse;
            boolean condAlive = alive;
            inits = whenTrue;
            scanStat(s.thenPart);
            BitSet thenInits = inits;
            boolean thenAlive = alive;
            inits = f;
            alive = condAlive;
            if (s.elsePart != null) {
                scanStat(s.elsePart);
            }
            if (!thenAlive && !(s.elsePart != null)) {
                // if-then: completes normally whenever reachable (JLS 14.22).
                alive = condAlive;
            }
            if (s.elsePart == null) {
                if (inits == null && condAlive) {
                    inits = copy(thenInits);
                }
                join(thenInits, thenAlive);
                alive = condAlive;
            } else {
                join(thenInits, thenAlive);
            }
        } else if (t instanceof Tree.WhileLoop) {
            Tree.WhileLoop w = (Tree.WhileLoop) t;
            BitSet entry = alive ? copy(inits) : null;
            scanCond(w.cond);
            BitSet f = whenFalse;
            inits = whenTrue;
            deadWhenFalse(w.cond, w.body);
            pushTarget(w, null);
            scanStat(w.body);
            deadBodyReported = false;
            popTarget();
            BitSet back = backEdge(w);
            boolean[] any = new boolean[1];
            BitSet breaks = takeExits(w, false, any);
            boolean hasBreak = any[0];
            inits = f;
            alive = !Attr.isTrue(w.cond);
            join(breaks, hasBreak);
            loopAssignments(entry, back);
        } else if (t instanceof Tree.DoLoop) {
            Tree.DoLoop d = (Tree.DoLoop) t;
            BitSet entry = alive ? copy(inits) : null;
            pushTarget(d, null);
            scanStat(d.body);
            popTarget();
            boolean[] any = new boolean[1];
            BitSet conts = takeExits(d, true, any);
            join(conts, any[0]);
            BitSet breaks = takeExits(d, false, any);
            boolean hasBreak = any[0];
            BitSet back = null;
            if (alive) {
                scanCond(d.cond);
                back = copy(whenTrue);
                inits = whenFalse;
                alive = !Attr.isTrue(d.cond);
            }
            join(breaks, hasBreak);
            loopAssignments(entry, back);
        } else if (t instanceof Tree.ForLoop) {
            Tree.ForLoop f = (Tree.ForLoop) t;
            for (Tree i : f.init) {
                scanStat(i);
            }
            BitSet entry = alive ? copy(inits) : null;
            BitSet falseInits;
            if (f.cond != null) {
                scanCond(f.cond);
                falseInits = whenFalse;
                inits = whenTrue;
                deadWhenFalse(f.cond, f.body);
            } else {
                falseInits = null;
            }
            pushTarget(f, null);
            scanStat(f.body);
            deadBodyReported = false;
            popTarget();
            boolean[] any = new boolean[1];
            BitSet conts = takeExits(f, true, any);
            join(conts, any[0]);
            if (alive) {
                for (Tree s : f.step) {
                    scanStat(s);
                }
            }
            BitSet back = alive ? copy(inits) : null;
            BitSet breaks = takeExits(f, false, any);
            boolean hasBreak = any[0];
            inits = falseInits;
            alive = f.cond != null && !Attr.isTrue(f.cond);
            join(breaks, hasBreak);
            loopAssignments(entry, back);
        } else if (t instanceof Tree.ForEach) {
            Tree.ForEach f = (Tree.ForEach) t;
            scanExpr(f.expr);
            BitSet before = copy(inits);
            BitSet entry = alive ? copy(inits) : null;
            declare(f.var.sym);
            assign(f.var.sym);
            pushTarget(f, null);
            scanStat(f.body);
            deadBodyReported = false;
            popTarget();
            BitSet back = backEdge(f);
            boolean[] any = new boolean[1];
            BitSet breaks = takeExits(f, false, any);
            boolean hasBreak = any[0];
            inits = before;
            alive = true;
            join(breaks, hasBreak);
            loopAssignments(entry, back);
        } else if (t instanceof Tree.Labeled) {
            Tree.Labeled l = (Tree.Labeled) t;
            pushTarget(l, l.label);
            scanStat(l.body);
            popTarget();
            boolean[] any = new boolean[1];
            BitSet breaks = takeExits(l, false, any);
            join(breaks, any[0]);
        } else if (t instanceof Tree.Switch) {
            scanSwitch((Tree.Switch) t);
        } else if (t instanceof Tree.Return) {
            Tree.Return r = (Tree.Return) t;
            if (r.expr != null) {
                scanExpr(r.expr);
            }
            if (ctorBlankFinals != null && alive) {
                checkBlankFinalsAssigned(r.pos);
            }
            markDead();
        } else if (t instanceof Tree.Throw) {
            scanExpr(((Tree.Throw) t).expr);
            markDead();
        } else if (t instanceof Tree.Break) {
            jump(resolveTarget(((Tree.Break) t).label, false), false);
        } else if (t instanceof Tree.Continue) {
            jump(resolveTarget(((Tree.Continue) t).label, true), true);
        } else if (t instanceof Tree.Yield) {
            scanExpr(((Tree.Yield) t).value);
            Object target = null;
            for (int i = targets.size() - 1; i >= 0; i--) {
                Tree node = (Tree) targets.get(i)[0];
                if (node instanceof Tree.Switch && ((Tree.Switch) node).isExpression) {
                    target = node;
                    break;
                }
            }
            jump(target, false);
        } else if (t instanceof Tree.Try) {
            scanTry((Tree.Try) t);
        } else if (t instanceof Tree.Synchronized) {
            scanExpr(((Tree.Synchronized) t).lock);
            scanStat(((Tree.Synchronized) t).body);
        } else if (t instanceof Tree.Assert) {
            BitSet saved = copy(inits);
            boolean savedAlive = alive;
            scanCond(((Tree.Assert) t).cond);
            if (((Tree.Assert) t).detail != null) {
                inits = whenFalse;
                scanExpr(((Tree.Assert) t).detail);
            }
            inits = saved;
            alive = savedAlive;
        } else if (t instanceof Tree.ClassDecl || t instanceof Tree.Empty) {
            // Local classes are checked on their own.
        } else {
            scanExpr(t);
        }
    }

    private void scanTry(Tree.Try t) {
        for (Tree r : t.resources) {
            if (r instanceof Tree.VarDef) {
                scanStat(r);
            } else {
                scanExpr(r);
            }
        }
        BitSet before = copy(inits);
        boolean beforeAlive = alive;
        scanStat(t.body);
        BitSet after = inits;
        boolean afterAlive = alive;
        // A catch or finally can start anywhere in the try block, so a variable the block
        // may assign is not definitely unassigned there (JLS 16.2.15).
        BitSet handlerEntry = withoutUnassigned(before, assignedIn(t.body));
        for (Tree.Catch c : t.catches) {
            inits = copy(handlerEntry);
            alive = beforeAlive;
            declare(c.param.sym);
            assign(c.param.sym);
            scanStat(c.body);
            BitSet ci = inits;
            boolean ca = alive;
            inits = after;
            alive = afterAlive;
            join(ci, ca);
            after = inits;
            afterAlive = alive;
        }
        if (t.finalizer != null) {
            List<VarSymbol> assigned = assignedIn(t.body);
            for (Tree.Catch c : t.catches) {
                assigned.addAll(assignedIn(c.body));
            }
            inits = withoutUnassigned(before, assigned);
            alive = beforeAlive;
            scanStat(t.finalizer);
            BitSet fin = inits;
            boolean finAlive = alive;
            if (!finAlive) {
                markDead();
                return;
            }
            inits = after;
            alive = afterAlive;
            if (inits != null && fin != null) {
                // Assigned after the statement if assigned by either part; unassigned only if
                // unassigned by both.
                BitSet du = copy(inits);
                du.and(fin);
                inits.or(fin);
                for (Integer i : index.values()) {
                    int u = i.intValue() + 1;
                    inits.set(u, du.get(u));
                }
            }
        } else {
            inits = after;
            alive = afterAlive;
        }
    }

    private void scanSwitch(Tree.Switch sw) {
        scanExpr(sw.selector);
        BitSet start = copy(inits);
        boolean startAlive = alive;
        pushTarget(sw, null);
        BitSet result = null;
        boolean resultAlive = false;
        boolean fallAlive = false;
        BitSet fall = null;
        boolean hasDefault = false;
        for (Tree.Case c : sw.cases) {
            hasDefault |= c.isDefault;
            for (Tree l : c.labels) {
                if (Attr.isUnconditionalPattern(l, sw)) {
                    hasDefault = true;
                }
            }
            inits = copy(start);
            alive = startAlive;
            if (!sw.arrows && fallAlive) {
                inits = meet(inits, fall);
            }
            for (Tree l : c.labels) {
                bindPattern(l);
            }
            if (c.guard != null) {
                scanCond(c.guard);
                inits = whenTrue;
            }
            if (c.arrowExpr != null) {
                scanExpr(c.arrowExpr);
            } else {
                scanStats(c.stats);
            }
            if (sw.arrows) {
                if (alive) {
                    if (!resultAlive) {
                        result = copy(inits);
                        resultAlive = true;
                    } else {
                        result = meet(result, inits);
                    }
                }
            } else {
                fall = copy(inits);
                fallAlive = alive;
            }
        }
        popTarget();
        if (!sw.arrows && fallAlive) {
            if (!resultAlive) {
                result = copy(fall);
                resultAlive = true;
            } else {
                result = meet(result, fall);
            }
        }
        boolean exhaustive = hasDefault || sw.needsDefaultThrow || sw.isExpression;
        if (!exhaustive && startAlive) {
            if (!resultAlive) {
                result = copy(start);
                resultAlive = true;
            } else {
                result = meet(result, start);
            }
        }
        boolean[] any = new boolean[1];
        BitSet breaks = takeExits(sw, false, any);
        inits = result;
        alive = resultAlive;
        join(breaks, any[0]);
    }

    private void bindPattern(Tree p) {
        if (p instanceof Tree.BindingPattern) {
            VarSymbol v = ((Tree.BindingPattern) p).var.sym;
            declare(v);
            assign(v);
        } else if (p instanceof Tree.RecordPattern) {
            for (Tree n : ((Tree.RecordPattern) p).nested) {
                bindPattern(n);
            }
        }
    }

    // ------------------------------------------------------------------ expressions

    private void scanCond(Tree c) {
        if (c.constant instanceof Boolean) {
            // Nothing to scan: a constant expression assigns nothing and reads only
            // constant variables, which are initialized where declared. (Scanning it
            // re-entered scanCond for a constant !X or X && Y, without end.)
            if (((Boolean) c.constant).booleanValue()) {
                whenTrue = copy(inits);
                whenFalse = null;
            } else {
                whenTrue = null;
                whenFalse = copy(inits);
            }
            return;
        }
        if (c instanceof Tree.Parens) {
            scanCond(((Tree.Parens) c).expr);
            return;
        }
        if (c instanceof Tree.Unary && ((Tree.Unary) c).op == Token.Kind.BANG) {
            scanCond(((Tree.Unary) c).arg);
            BitSet t = whenTrue;
            whenTrue = whenFalse;
            whenFalse = t;
            return;
        }
        if (c instanceof Tree.Binary && (((Tree.Binary) c).op == Token.Kind.AMPAMP || ((Tree.Binary) c).op == Token.Kind.BARBAR)) {
            Tree.Binary b = (Tree.Binary) c;
            boolean and = b.op == Token.Kind.AMPAMP;
            scanCond(b.lhs);
            BitSet t1 = whenTrue;
            BitSet f1 = whenFalse;
            inits = copy(and ? t1 : f1);
            scanCond(b.rhs);
            if (and) {
                whenFalse = meet(f1, whenFalse);
            } else {
                whenTrue = meet(t1, whenTrue);
            }
            return;
        }
        if (c instanceof Tree.InstanceOf) {
            scanExpr(((Tree.InstanceOf) c).expr);
            whenFalse = copy(inits);
            bindPattern(((Tree.InstanceOf) c).pattern);
            whenTrue = copy(inits);
            return;
        }
        scanExpr(c);
        whenTrue = copy(inits);
        whenFalse = copy(inits);
    }

    /**
     * The variable an assignment target names, if this analysis tracks it: a local,
     * or a blank final field being initialized, named as {@code x} or {@code this.x}
     * (JLS 16: only those two forms count).
     */
    /** The variables of this method that a statement assigns anywhere inside it. */
    private List<VarSymbol> assignedIn(Tree stat) {
        final List<VarSymbol> out = new ArrayList<VarSymbol>();
        new TreeScanner() {
            @Override
            void scan(Tree t) {
                Tree lhs = t instanceof Tree.Assign ? ((Tree.Assign) t).lhs
                        : t instanceof Tree.CompoundAssign ? ((Tree.CompoundAssign) t).lhs
                        : t instanceof Tree.Unary && (((Tree.Unary) t).op == Token.Kind.PLUSPLUS
                                || ((Tree.Unary) t).op == Token.Kind.SUBSUB) ? ((Tree.Unary) t).arg : null;
                VarSymbol v = lhs == null ? null : localOf(lhs);
                if (v != null) {
                    out.add(v);
                }
                super.scan(t);
            }
        }.scan(stat);
        return out;
    }

    /** A copy of state in which none of vars is definitely unassigned. */
    private BitSet withoutUnassigned(BitSet state, List<VarSymbol> vars) {
        BitSet r = copy(state);
        if (r != null) {
            for (VarSymbol v : vars) {
                Integer i = index.get(v);
                if (i != null) {
                    r.clear(i.intValue() + 1);
                }
            }
        }
        return r;
    }

    private void markNotEffectivelyFinal(Tree lhs) {
        VarSymbol v = localOf(lhs);
        if (v != null) {
            v.notEffectivelyFinal = true;
        }
    }

    private VarSymbol localOf(Tree t) {
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        if (t instanceof Tree.Ident && ((Tree.Ident) t).sym instanceof VarSymbol) {
            VarSymbol v = (VarSymbol) ((Tree.Ident) t).sym;
            return v.kind == VarSymbol.Kind.FIELD && !index.containsKey(v) ? null : v;
        }
        if (t instanceof Tree.Select && ((Tree.Select) t).sym instanceof VarSymbol && isThis(((Tree.Select) t).selected)) {
            VarSymbol v = (VarSymbol) ((Tree.Select) t).sym;
            return index.containsKey(v) ? v : null;
        }
        return null;
    }

    /** Where javac reports an assignment target: the name, or for this.x the dot before it. */
    private static int targetPos(Tree t) {
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        if (t instanceof Tree.Select && ((Tree.Select) t).dotPos >= 0) {
            return ((Tree.Select) t).dotPos;
        }
        return t.pos;
    }

    private static boolean isThis(Tree t) {
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        return t instanceof Tree.Ident && "this".equals(((Tree.Ident) t).name);
    }

    private void scanExpr(Tree t) {
        if (t == null) {
            return;
        }
        if (t instanceof Tree.Ident) {
            Symbol s = ((Tree.Ident) t).sym;
            if (s instanceof VarSymbol) {
                // A field is only in the index while it is a blank final being initialized.
                checkRead((VarSymbol) s, t.pos);
            }
        } else if (t instanceof Tree.Assign) {
            Tree.Assign a = (Tree.Assign) t;
            VarSymbol v = localOf(a.lhs);
            if (v == null) {
                scanLhs(a.lhs);
            }
            scanExpr(a.rhs);
            if (v != null) {
                assignedHere(v);
                if (v.isFinal() && !v.hasInitializer && alive && inits != null) {
                    Integer i = index.get(v);
                    if (i != null && inits.get(i.intValue())) {
                        error(targetPos(a.lhs), "variable " + v.name + " might already have been assigned");
                    }
                }
                assign(v);
            }
        } else if (t instanceof Tree.CompoundAssign) {
            markNotEffectivelyFinal(((Tree.CompoundAssign) t).lhs);
            scanExpr(((Tree.CompoundAssign) t).lhs);
            scanExpr(((Tree.CompoundAssign) t).rhs);
        } else if (t instanceof Tree.Binary && (((Tree.Binary) t).op == Token.Kind.AMPAMP || ((Tree.Binary) t).op == Token.Kind.BARBAR)
                || t instanceof Tree.Unary && ((Tree.Unary) t).op == Token.Kind.BANG || t instanceof Tree.InstanceOf) {
            scanCond(t);
            inits = meet(whenTrue, whenFalse);
        } else if (t instanceof Tree.Conditional) {
            Tree.Conditional c = (Tree.Conditional) t;
            scanCond(c.cond);
            BitSet f = whenFalse;
            inits = whenTrue;
            scanExpr(c.truePart);
            BitSet a = inits;
            inits = f;
            scanExpr(c.falsePart);
            inits = meet(a, inits);
        } else if (t instanceof Tree.Lambda) {
            Tree.Lambda l = (Tree.Lambda) t;
            if (l.boundExpr != null) {
                scanExpr(l.boundExpr);
            }
            BitSet saved = copy(inits);
            boolean savedAlive = alive;
            List<Object[]> savedExits = exits;
            List<Object[]> savedTargets = targets;
            // A return in the lambda leaves the lambda, not the enclosing constructor.
            List<VarSymbol> savedCtorBlanks = ctorBlankFinals;
            ctorBlankFinals = null;
            exits = new ArrayList<Object[]>();
            targets = new ArrayList<Object[]>();
            for (Tree.VarDef p : l.params) {
                declare(p.sym);
                assign(p.sym);
            }
            if (l.boundVar != null) {
                declare(l.boundVar);
                assign(l.boundVar);
            }
            if (l.body instanceof Tree.Block) {
                scanStat(l.body);
            } else {
                scanExpr(l.body);
            }
            inits = saved;
            alive = savedAlive;
            exits = savedExits;
            targets = savedTargets;
            ctorBlankFinals = savedCtorBlanks;
        } else if (t instanceof Tree.MethodRef) {
            Tree.MethodRef r = (Tree.MethodRef) t;
            if (r.lambda != null) {
                scanExpr(r.lambda);
            }
        } else if (t instanceof Tree.Switch) {
            scanSwitch((Tree.Switch) t);
        } else if (t instanceof Tree.NewClass) {
            Tree.NewClass nc = (Tree.NewClass) t;
            scanExpr(nc.outer);
            for (Tree a : nc.args) {
                scanExpr(a);
            }
        } else if (t instanceof Tree.MethodCall) {
            Tree.MethodCall c = (Tree.MethodCall) t;
            scanExpr(c.receiver);
            for (Tree a : c.args) {
                scanExpr(a);
            }
        } else if (t instanceof Tree.Select) {
            Tree.Select sel = (Tree.Select) t;
            if (sel.sym instanceof VarSymbol && isThis(sel.selected)) {
                checkRead((VarSymbol) sel.sym, t.pos);
            } else {
                scanExpr(sel.selected);
            }
        } else if (t instanceof Tree.Parens) {
            scanExpr(((Tree.Parens) t).expr);
        } else if (t instanceof Tree.Unary) {
            Token.Kind op = ((Tree.Unary) t).op;
            if (op == Token.Kind.PLUSPLUS || op == Token.Kind.SUBSUB) {
                markNotEffectivelyFinal(((Tree.Unary) t).arg);
            }
            scanExpr(((Tree.Unary) t).arg);
        } else if (t instanceof Tree.Binary) {
            scanExpr(((Tree.Binary) t).lhs);
            scanExpr(((Tree.Binary) t).rhs);
        } else if (t instanceof Tree.Cast) {
            scanExpr(((Tree.Cast) t).expr);
        } else if (t instanceof Tree.NewArray) {
            Tree.NewArray na = (Tree.NewArray) t;
            for (Tree d : na.dims) {
                scanExpr(d);
            }
            if (na.elems != null) {
                for (Tree e : na.elems) {
                    scanExpr(e);
                }
            }
        } else if (t instanceof Tree.ArrayAccess) {
            scanExpr(((Tree.ArrayAccess) t).array);
            scanExpr(((Tree.ArrayAccess) t).index);
        }
    }

    /** The parts of an assignment target that are evaluated (receiver, array, index). */
    private void scanLhs(Tree lhs) {
        while (lhs instanceof Tree.Parens) {
            lhs = ((Tree.Parens) lhs).expr;
        }
        if (lhs instanceof Tree.Select) {
            scanExpr(((Tree.Select) lhs).selected);
        } else if (lhs instanceof Tree.ArrayAccess) {
            scanExpr(((Tree.ArrayAccess) lhs).array);
            scanExpr(((Tree.ArrayAccess) lhs).index);
        }
    }
}
