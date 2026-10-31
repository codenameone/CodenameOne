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

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Await;
import com.codename1.python.transpiler.ast.Node.Break;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Continue;
import com.codename1.python.transpiler.ast.Node.ExceptHandler;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.ExprStmt;
import com.codename1.python.transpiler.ast.Node.For;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.If;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Return;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Try;
import com.codename1.python.transpiler.ast.Node.While;
import com.codename1.python.transpiler.ast.Node.With;
import com.codename1.python.transpiler.ast.Node.WithItem;
import com.codename1.python.transpiler.ast.Node.Yield;
import com.codename1.python.transpiler.ast.Scope;
import java.util.ArrayList;
import java.util.List;

/// Lowers the body of a generator or coroutine into the `step` method of a
/// `PyGenerator` subclass: a `switch (pc)` whose cases are the stretches of
/// the body between suspension points.
///
/// A statement that cannot suspend (and does not jump out of a lowered loop
/// or `finally`) is emitted by the [Emitter] as it would be anywhere else; the
/// rest become states. Exceptions are routed by a table: every state records
/// the state that handles an exception raised in it (an `except` dispatch, a
/// `finally`, or none), and the `catch` around the `switch` jumps there with
/// the exception in `exc`. A `finally` is entered with a pending action in a
/// field -- fall through, re-raise, return, or jump to a state -- and performs
/// it when its body completes, which is how `break`, `continue` and `return`
/// run the `finally` blocks they leave.
final class GenLowering {
    /// Pending actions of a `finally`; a jump is the target state, 0 or more.
    private static final int NORMAL = -1;
    private static final int RERAISE = -2;
    private static final int RETURN = -3;
    private static final int SUPPRESSED = -4;

    private static final int IND = 5;

    private final Emitter em;
    private final String file;
    private final Linearize lin;
    private final StringBuilder fields = new StringBuilder(); //NOPMD AvoidStringBufferField - the generator's output
    private final StringBuilder out = new StringBuilder(); //NOPMD AvoidStringBufferField - the generator's output
    private final List<Integer> handlerOf = new ArrayList<Integer>();
    private final List<Loop> loops = new ArrayList<Loop>();
    private final List<Fin> fins = new ArrayList<Fin>();
    private int handler = -1;
    private boolean retField;

    /// A lowered loop: where `break` and `continue` go, and how many
    /// lowered `finally` blocks enclose it.
    private static final class Loop {
        final int breakTo;
        final int continueTo;
        final int depth;

        Loop(int breakTo, int continueTo, int depth) {
            this.breakTo = breakTo;
            this.continueTo = continueTo;
            this.depth = depth;
        }
    }

    /// A lowered `finally`, and the jumps routed through it.
    private static final class Fin {
        final int entry;
        final String pending;
        final String saved;
        final List<int[]> jumps = new ArrayList<int[]>();
        boolean returns;

        Fin(int entry, String pending, String saved) {
            this.entry = entry;
            this.pending = pending;
            this.saved = saved;
        }
    }

    /// Whether this is an async generator, whose yields are wrapped.
    boolean asyncGen;

    GenLowering(Emitter em, String file, Scope scope) {
        this.em = em;
        this.file = file;
        this.lin = new Linearize(file, scope);
    }

    /// The fields the lowered body needs besides the function's locals.
    String fields() {
        return fields.toString();
    }

    /// The body of `step()`: the dispatch loop over `body`'s states.
    String step(List<Stmt> body) {
        begin(newState());
        block(body);
        line("if (true) {");
        line("    pc = -1;");
        line("    return Py.NONE;");
        line("}");
        StringBuilder r = new StringBuilder();
        r.append("                d$: while (true) {\n");
        r.append("                    try {\n");
        r.append("                        switch (pc) {\n");
        r.append(out);
        r.append("                            }\n");
        r.append("                            default:\n");
        r.append("                                throw new IllegalStateException();\n");
        r.append("                        }\n");
        r.append("                    } catch (Throwable t$) {\n");
        r.append("                        final PyException x$ = Py.convert(t$);\n");
        r.append("                        switch (pc) {\n");
        boolean any = false;
        for (int h = 0; h < handlerOf.size(); h++) {
            StringBuilder labels = new StringBuilder();
            for (int s = 0; s < handlerOf.size(); s++) {
                if (handlerOf.get(s).intValue() == h) {
                    labels.append("                            case ").append(s).append(":\n");
                }
            }
            if (labels.length() > 0) {
                any = true;
                r.append(labels).append("                                pc = ").append(h).append(";\n")
                        .append("                                break;\n");
            }
        }
        r.append("                            default:\n");
        r.append("                                throw x$;\n");
        r.append("                        }\n");
        if (any) {
            r.append("                        exc = x$;\n");
        }
        r.append("                    }\n");
        r.append("                }\n");
        return r.toString();
    }

    // ---- states ------------------------------------------------------------

    private int newState() {
        handlerOf.add(Integer.valueOf(handler));
        return handlerOf.size() - 1;
    }

    private int newState(int h) {
        handlerOf.add(Integer.valueOf(h));
        return handlerOf.size() - 1;
    }

    private void begin(int s) {
        if (out.length() > 0) {
            out.append("                            }\n");
        }
        out.append("                            case ").append(s).append(": {\n");
    }

    private void line(String code) {
        for (int i = 0; i < IND + 2; i++) {
            out.append("    ");
        }
        out.append(code).append('\n');
    }

    private static String goTo(int s) {
        return "if (true) { pc = " + s + "; continue d$; }";
    }

    private void jump(int s) {
        line(goTo(s));
    }

    private String field(String type, String prefix) {
        String f = em.fresh(prefix) + "$";
        fields.append("                ").append(type).append(' ').append(f).append(";\n");
        return f;
    }

    // ---- statements --------------------------------------------------------

    private void block(List<Stmt> body) {
        for (Stmt s : body) {
            stmt(s);
        }
    }

    /// Whether `s` has to become states: it suspends, or leaves a lowered
    /// loop or `finally` in a way only states can follow.
    private boolean lowered(Stmt s) {
        if (Linearize.suspends(s)) {
            return true;
        }
        if (!fins.isEmpty() && returns(s)) {
            return true;
        }
        return !loops.isEmpty() && jumpsOut(s);
    }

    private static boolean returns(Stmt s) {
        if (s instanceof Return) {
            return true;
        }
        if (s instanceof FunctionDef || s instanceof ClassDef) {
            return false;
        }
        for (List<Stmt> b : Linearize.bodies(s)) {
            for (Stmt x : b) {
                if (returns(x)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// A `break` or `continue` that belongs to an enclosing loop.
    private static boolean jumpsOut(Stmt s) {
        if (s instanceof Break || s instanceof Continue) {
            return true;
        }
        if (s instanceof For || s instanceof While || s instanceof FunctionDef || s instanceof ClassDef) {
            // Their own loop's, or their own: a lowered loop's never reaches
            // inside one emitted as Java.
            return s instanceof For && jumpsOutOfElse(((For) s).orelse)
                    || s instanceof While && jumpsOutOfElse(((While) s).orelse);
        }
        for (List<Stmt> b : Linearize.bodies(s)) {
            for (Stmt x : b) {
                if (jumpsOut(x)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean jumpsOutOfElse(List<Stmt> orelse) {
        for (Stmt x : orelse) {
            if (jumpsOut(x)) {
                return true;
            }
        }
        return false;
    }

    private void stmt(Stmt s) {
        if (!lowered(s)) {
            em.stmt(s, out, IND + 2);
            return;
        }
        List<Stmt> rep = lin.stmt(s);
        if (rep != null) {
            block(rep);
            return;
        }
        if (s instanceof ExprStmt) {
            suspend(((ExprStmt) s).value);
            line("noop(" + result(((ExprStmt) s).value) + ");");
        } else if (s instanceof Assign) {
            Assign a = (Assign) s;
            suspend(a.value);
            String r = result(a.value);
            if (a.targets.size() == 1) {
                em.store(a.targets.get(0), r, out, IND + 2);
            } else {
                String t = em.fresh("s");
                line("final Object " + t + " = " + r + ";");
                for (Expr x : a.targets) {
                    em.store(x, t, out, IND + 2);
                }
            }
        } else if (s instanceof Return) {
            Expr v = ((Return) s).value;
            returnWith(v == null ? "Py.NONE" : em.expr(v));
        } else if (s instanceof Break) {
            if (loops.isEmpty()) {
                throw new PythonSyntaxError(file, s.line, s.col, "'break' outside loop");
            }
            Loop l = loops.get(loops.size() - 1);
            jumpTo(l.breakTo, l.depth);
        } else if (s instanceof Continue) {
            if (loops.isEmpty()) {
                throw new PythonSyntaxError(file, s.line, s.col, "'continue' not properly in loop");
            }
            Loop l = loops.get(loops.size() - 1);
            jumpTo(l.continueTo, l.depth);
        } else if (s instanceof If) {
            ifStmt((If) s);
        } else if (s instanceof While) {
            whileStmt((While) s);
        } else if (s instanceof For) {
            forStmt((For) s);
        } else if (s instanceof Try) {
            tryStmt((Try) s);
        } else if (s instanceof With) {
            withStmt((With) s);
        } else {
            throw em.unsupported(s, "await or yield inside " + s.getClass().getSimpleName());
        }
    }

    /// Emits the suspension at the top of `v`, if it has one, ending the
    /// current state; the code after it runs on resumption.
    private void suspend(Expr v) {
        if (v instanceof Yield && !((Yield) v).from) {
            Expr x = ((Yield) v).value;
            int k = newState();
            String y = x == null ? "Py.NONE" : em.expr(x);
            if (asyncGen) {
                y = "new PyGenerator.Yielded(" + y + ")";
            }
            line("if (true) { pc = " + k + "; return " + y + "; }");
            begin(k);
        } else if (v instanceof Yield) {
            delegate("Py.iter(" + em.expr(((Yield) v).value) + ")");
        } else if (v instanceof Await) {
            delegate("PyGenerator.awaitable(" + em.expr(((Await) v).value) + ")");
        }
    }

    /// The Java value of `v` once [#suspend] has run.
    private String result(Expr v) {
        if (v instanceof Yield && !((Yield) v).from) {
            return "resumed()";
        }
        if (v instanceof Yield || v instanceof Await) {
            return "delegateResult";
        }
        return em.expr(v);
    }

    /// `yield from` an iterator: one state that re-enters until it is done.
    private void delegate(String iterator) {
        line("delegate = " + iterator + ";");
        int k = newState();
        jump(k);
        begin(k);
        line("final Object r$ = delegateStep();");
        line("if (r$ != null) {");
        line("    return r$;");
        line("}");
    }

    /// Awaits `java`, an awaitable, inside the current state sequence; the
    /// result is then `delegateResult`.
    private void await(String java) {
        delegate("PyGenerator.awaitable(" + java + ")");
    }

    private void returnWith(String value) {
        if (fins.isEmpty()) {
            line("if (true) { pc = -1; return " + value + "; }");
            return;
        }
        retField = true;
        line("ret$ = " + value + ";");
        returnThrough();
    }

    /// Returns `ret$`, running the lowered `finally` blocks on the way.
    private void returnThrough() {
        if (fins.isEmpty()) {
            line("if (true) { pc = -1; return ret$; }");
            return;
        }
        Fin f = fins.get(fins.size() - 1);
        f.returns = true;
        line(f.pending + " = " + RETURN + ";");
        jump(f.entry);
    }

    /// Jumps to state `target`, which `depth` lowered `finally` blocks
    /// enclose, through the ones in between.
    private void jumpTo(int target, int depth) {
        if (fins.size() <= depth) {
            jump(target);
            return;
        }
        Fin f = fins.get(fins.size() - 1);
        f.jumps.add(new int[] {target, depth});
        line(f.pending + " = " + target + ";");
        jump(f.entry);
    }

    private void ifStmt(If s) {
        int end = newState();
        int els = s.orelse.isEmpty() ? end : newState();
        line("if (!(" + em.cond(s.test) + ")) " + goTo(els));
        block(s.body);
        jump(end);
        if (!s.orelse.isEmpty()) {
            begin(els);
            block(s.orelse);
            jump(end);
        }
        begin(end);
    }

    private void whileStmt(While w) {
        int top = newState();
        int end = newState();
        int els = w.orelse.isEmpty() ? end : newState();
        jump(top);
        begin(top);
        line("if (!(" + em.cond(w.test) + ")) " + goTo(els));
        loops.add(new Loop(end, top, fins.size()));
        block(w.body);
        loops.remove(loops.size() - 1);
        jump(top);
        if (!w.orelse.isEmpty()) {
            begin(els);
            block(w.orelse);
            jump(end);
        }
        begin(end);
    }

    private void forStmt(For f) {
        String it = field("PyObject", "i");
        int top = newState();
        int end = newState();
        int els = f.orelse.isEmpty() ? end : newState();
        String x = em.fresh("x");
        if (f.isAsync) {
            line(it + " = Py.obj(Py.callMethod(" + em.expr(f.iter) + ", \"__aiter__\"));");
            jump(top);
            begin(top);
            line("delegate = PyGenerator.awaitable(Py.callMethod(" + it + ", \"__anext__\"));");
            int k = newState();
            jump(k);
            begin(k);
            line("final Object r$;");
            line("try {");
            line("    r$ = delegateStep();");
            line("} catch (PyException e$) {");
            line("    if (e$.matches(Py.STOP_ASYNC_ITERATION)) " + goTo(els));
            line("    throw e$;");
            line("}");
            line("if (r$ != null) {");
            line("    return r$;");
            line("}");
            line("final Object " + x + " = delegateResult;");
        } else {
            line(it + " = Py.iter(" + em.expr(f.iter) + ");");
            jump(top);
            begin(top);
            line("final Object " + x + " = " + it + ".next();");
            line("if (" + x + " == null) " + goTo(els));
        }
        em.store(f.target, x, out, IND + 2);
        loops.add(new Loop(end, top, fins.size()));
        block(f.body);
        loops.remove(loops.size() - 1);
        jump(top);
        if (!f.orelse.isEmpty()) {
            begin(els);
            block(f.orelse);
            jump(end);
        }
        begin(end);
        line(it + " = null;");
    }

    /// Opens a `finally`: its entry state and the field of its pending action.
    private Fin openFin() {
        return new Fin(newState(), field("int", "p"), field("PyException", "e"));
    }

    /// The state that records a raise and enters `f`.
    private int raiseInto() {
        return newState();
    }

    /// The end of a `finally` body: performs the pending action.
    private void closeFin(Fin f, int after) {
        line("if (" + f.pending + " == " + RERAISE + ") {");
        line("    throw " + f.saved + ";");
        line("}");
        for (int[] j : f.jumps) {
            line("if (" + f.pending + " == " + j[0] + ") {");
            jumpTo(j[0], j[1]);
            line("}");
        }
        if (f.returns) {
            line("if (" + f.pending + " == " + RETURN + ") {");
            returnThrough();
            line("}");
        }
        jump(after);
    }

    private void tryStmt(Try t) {
        if (t.star) {
            throw em.unsupported(t, "except*");
        }
        int outer = handler;
        Fin fin = null;
        int finRaise = -1;
        if (!t.finalbody.isEmpty()) {
            fin = openFin();
            finRaise = raiseInto();
        }
        int inner = fin != null ? finRaise : outer;
        int after = newState();
        int dispatch = t.handlers.isEmpty() ? -1 : newState(inner);
        if (fin != null) {
            fins.add(fin);
        }
        handler = dispatch >= 0 ? dispatch : inner;
        int body = newState();
        jump(body);
        begin(body);
        block(t.body);
        handler = inner;
        if (!t.orelse.isEmpty()) {
            int els = newState();
            jump(els);
            begin(els);
            block(t.orelse);
        }
        leave(fin, after);
        if (dispatch >= 0) {
            begin(dispatch);
            String e = field("PyException", "e");
            line(e + " = exc;");
            List<Integer> bodies = new ArrayList<Integer>();
            boolean catchAll = false;
            for (int i = 0; i < t.handlers.size(); i++) {
                ExceptHandler h = t.handlers.get(i);
                int hs = newState();
                bodies.add(Integer.valueOf(hs));
                if (h.type == null) {
                    if (i != t.handlers.size() - 1) {
                        throw new PythonSyntaxError(file, h.line, h.col, "default 'except:' must be last");
                    }
                    jump(hs);
                    catchAll = true;
                } else {
                    line("if (" + e + ".matches(" + em.expr(h.type) + ")) " + goTo(hs));
                }
            }
            if (!catchAll) {
                line("if (true) throw " + e + ";");
            }
            for (int i = 0; i < t.handlers.size(); i++) {
                ExceptHandler h = t.handlers.get(i);
                begin(bodies.get(i).intValue());
                if (h.name != null) {
                    em.storeName(h.name, e + ".value", out, IND + 2);
                }
                em.handlers.add(e);
                block(h.body);
                em.handlers.remove(em.handlers.size() - 1);
                if (h.name != null) {
                    line(em.nameLvalue(h.name) + " = null;");
                }
                line(e + " = null;");
                leave(fin, after);
            }
        }
        handler = outer;
        if (fin != null) {
            fins.remove(fins.size() - 1);
            begin(finRaise);
            line(fin.pending + " = " + RERAISE + ";");
            line(fin.saved + " = exc;");
            jump(fin.entry);
            begin(fin.entry);
            block(t.finalbody);
            closeFin(fin, after);
        }
        begin(after);
    }

    /// Normal completion of a protected block: through `fin`, if there is one.
    private void leave(Fin fin, int after) {
        if (fin == null) {
            jump(after);
        } else {
            line(fin.pending + " = " + NORMAL + ";");
            jump(fin.entry);
        }
    }

    private void withStmt(With w) {
        WithItem item = w.items.get(0);
        boolean async = w.isAsync;
        String mgr = field("Object", "m");
        String exit = field("Object", "x");
        line(mgr + " = " + em.expr(item.context) + ";");
        line(exit + " = Py.getattr(" + mgr + ", \"" + (async ? "__aexit__" : "__exit__") + "\");");
        String entered;
        if (async) {
            await("Py.callMethod(" + mgr + ", \"__aenter__\")");
            entered = "delegateResult";
        } else {
            entered = "Py.callMethod(" + mgr + ", \"__enter__\")";
        }
        if (item.vars != null) {
            String ev = em.fresh("en");
            line("final Object " + ev + " = " + entered + ";");
            em.store(item.vars, ev, out, IND + 2);
        } else {
            line("noop(" + entered + ");");
        }
        int outer = handler;
        Fin fin = openFin();
        int finRaise = raiseInto();
        int after = newState();
        int dispatch = newState(finRaise);
        fins.add(fin);
        handler = dispatch;
        int body = newState();
        jump(body);
        begin(body);
        block(w.body);
        handler = finRaise;
        leave(fin, after);
        begin(dispatch);
        String e = field("PyException", "e");
        line(e + " = exc;");
        String call = "Py.call(" + exit + ", Py.typeOf(" + e + ".value), " + e + ".value, Py.NONE)";
        String suppressed;
        if (async) {
            await(call);
            suppressed = "delegateResult";
        } else {
            suppressed = call;
        }
        line("if (!Py.isTrue(" + suppressed + ")) {");
        line("    throw " + e + ";");
        line("}");
        line(fin.pending + " = " + SUPPRESSED + ";");
        jump(fin.entry);
        handler = outer;
        fins.remove(fins.size() - 1);
        begin(finRaise);
        line(fin.pending + " = " + RERAISE + ";");
        line(fin.saved + " = exc;");
        jump(fin.entry);
        begin(fin.entry);
        // The exit already ran for an exception the body raised; a raise
        // out of the exit itself (RERAISE) must not run it again.
        int skip = newState();
        line("if (" + fin.pending + " == " + SUPPRESSED + " || " + fin.pending + " == " + RERAISE + ") "
                + goTo(skip));
        String clean = "Py.call(" + exit + ", Py.NONE, Py.NONE, Py.NONE)";
        if (async) {
            await(clean);
        } else {
            line(clean + ";");
        }
        jump(skip);
        begin(skip);
        line(mgr + " = null;");
        line(exit + " = null;");
        closeFin(fin, after);
        begin(after);
    }

    /// Whether `body` needs a lowered body at all.
    static boolean needed(FunctionDef f) {
        return f.isAsync || f.scope.hasYield;
    }

    /// Whether a name the body binds is one of the hidden ones.
    static boolean hidden(Name n) {
        return n.id.startsWith("$y");
    }

    boolean usesReturnField() {
        return retField;
    }
}
