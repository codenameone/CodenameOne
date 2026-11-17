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
package com.codename1.luau;

import com.codename1.util.MathUtil;

/// A Luau virtual machine: globals, the running coroutine and the
/// interpreter loop.
///
/// ```java
/// Interp vm = new Interp();
/// vm.load("print('hello ' .. 1 + 2)", "demo").call0();
/// ```
public final class Interp {
    static final Double ZERO = Double.valueOf(0);
    private static final Double[] SMALL = new Double[1280];
    private static final Object[] NONE = new Object[0];
    private static final Object[] YIELD = new Object[0];
    private static final int MAX_FRAMES = 15000;
    private static final int MAX_NEST = 180;

    static {
        for (int i = 0; i < SMALL.length; i++) {
            SMALL[i] = Double.valueOf(i - 256);
        }
    }

    private final LuaTable globals = new LuaTable();
    private LuaTable stringMeta;
    private Coroutine current;
    private final Coroutine mainThread;
    private int nest;
    private long budget = Long.MAX_VALUE;
    private long steps;
    private Printer printer;
    final JavaFunction pcallFn;
    final JavaFunction xpcallFn;

    /// `pcall` and `xpcall`: the loop runs them itself, so a protected
    /// call costs a frame flag rather than a nested Java loop.
    private static final class Marker extends JavaFunction {
        Marker(String name) {
            super(name);
        }

        @Override
        public Object[] call(Interp interp, Object[] args) {
            throw new IllegalStateException(name());
        }
    }

    /// Receives `print` output.
    public interface Printer {
        /// One line of output, already joined with tabs.
        void print(String line);
    }

    /// A VM with the standard library installed.
    public Interp() {
        mainThread = new Coroutine(null);
        mainThread.status = Coroutine.RUNNING;
        mainThread.started = true;
        current = mainThread;
        pcallFn = new Marker("pcall");
        xpcallFn = new Marker("xpcall");
        Stdlib.install(this);
    }

    /// The global table (`_G`).
    public LuaTable getGlobals() {
        return globals;
    }

    LuaTable stringMeta() {
        return stringMeta;
    }

    void setStringMeta(LuaTable mt) {
        stringMeta = mt;
    }

    /// Where `print` writes; null writes to `System.out`.
    public void setPrinter(Printer p) {
        printer = p;
    }

    void print(String line) {
        if (printer != null) {
            printer.print(line);
        } else {
            System.out.println(Utf8.toJava(line));
        }
    }

    /// Caps how many loop iterations and calls one resume may run before it
    /// fails with a timeout that `pcall` cannot catch; 0 lifts the cap.
    public void setBudget(long maxSteps) {
        budget = maxSteps <= 0 ? Long.MAX_VALUE : maxSteps;
    }

    /// Charges `n` steps of library work (a long `string.rep`, a pattern
    /// match that backtracks) to the same budget loops and calls draw on, so
    /// one builtin call cannot run unbounded inside a single bytecode step.
    void charge(long n) {
        steps += n;
        if (steps > budget || steps < 0) {
            throw LuaError.fatal("Script timeout: exhausted allowed execution time");
        }
    }

    /// Steps run since the last [#resetSteps()].
    public long getSteps() {
        return steps;
    }

    /// Restarts the budget count, usually once per frame or resume.
    public void resetSteps() {
        steps = 0;
    }

    /// The running coroutine.
    public Coroutine currentThread() {
        return current;
    }

    /// True on the main thread, where `coroutine.isyieldable` is false.
    boolean isMain(Coroutine c) {
        return c == mainThread; // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
    }

    // ------------------------------------------------------------ values

    /// Boxes a number, sharing the boxes of small integers.
    public static Double num(double d) {
        int i = (int) d;
        if (i == d && i >= -256 && i < SMALL.length - 256 && !(d == 0 && 1 / d < 0)) {
            return SMALL[i + 256];
        }
        return Double.valueOf(d);
    }

    /// Luau truthiness: everything but nil and false.
    public static boolean truthy(Object v) {
        return v != null && (!(v instanceof Boolean) || ((Boolean) v).booleanValue());
    }

    /// The name `type` reports.
    public static String typeName(Object v) {
        if (v == null) {
            return "nil";
        }
        if (v instanceof Boolean) {
            return "boolean";
        }
        if (v instanceof Double) {
            return "number";
        }
        if (v instanceof String) {
            return "string";
        }
        if (v instanceof LuaTable) {
            return "table";
        }
        if (v instanceof LuaFunction) {
            return "function";
        }
        if (v instanceof Coroutine) {
            return "thread";
        }
        if (v instanceof LuaBuffer) {
            return "buffer";
        }
        return "userdata";
    }

    /// The metatable of any value, or null.
    public LuaTable getMetatable(Object v) {
        if (v instanceof LuaTable) {
            return ((LuaTable) v).getMetatable();
        }
        if (v instanceof LuaUserdata) {
            return ((LuaUserdata) v).getMetatable();
        }
        if (v instanceof String) {
            return stringMeta;
        }
        return null;
    }

    Object metamethod(Object v, String event) {
        LuaTable mt = getMetatable(v);
        return mt == null ? null : mt.rawget(event);
    }

    /// Converts a number or a numeric string, or returns null.
    public static Double toNumber(Object v) {
        if (v instanceof Double) {
            return (Double) v;
        }
        if (v instanceof String) {
            return parseNumber((String) v);
        }
        return null;
    }

    static Double parseNumber(String s) {
        String t = s.trim();
        if (t.length() == 0) {
            return null;
        }
        boolean neg = false;
        String body = t;
        if (body.startsWith("-")) {
            neg = true;
            body = body.substring(1);
        } else if (body.startsWith("+")) {
            body = body.substring(1);
        }
        if (body.startsWith("0x") || body.startsWith("0X")) {
            if (body.length() == 2) {
                return null;
            }
            double v = 0;
            for (int i = 2; i < body.length(); i++) {
                int d = Character.digit(body.charAt(i), 16);
                if (d < 0) {
                    return null;
                }
                v = v * 16 + d;
            }
            return num(neg ? -v : v);
        }
        if (body.startsWith("0b") || body.startsWith("0B")) {
            if (body.length() == 2) {
                return null;
            }
            double v = 0;
            for (int i = 2; i < body.length(); i++) {
                char c = body.charAt(i);
                if (c != '0' && c != '1') {
                    return null;
                }
                v = v * 2 + (c - '0');
            }
            return num(neg ? -v : v);
        }
        if ("inf".equals(body) || "nan".equals(body) || "Infinity".equals(body) || "NaN".equals(body)) {
            return null;
        }
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (!(c >= '0' && c <= '9' || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-')) {
                return null;
            }
        }
        try {
            double d = Double.parseDouble(body);
            return num(neg ? -d : d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /// Formats a number as Luau does: integers without a fraction, other
    /// values in their shortest round-trip form.
    public static String numberToString(double d) {
        if (d != d) {
            return "nan";
        }
        if (d == Double.POSITIVE_INFINITY) {
            return "inf";
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return "-inf";
        }
        if (d == (long) d && Math.abs(d) < 1e16) {
            if (d == 0 && 1 / d < 0) {
                return "-0";
            }
            return Long.toString((long) d);
        }
        String s = Double.toString(d);
        int e = s.indexOf('E');
        if (e < 0) {
            if (s.endsWith(".0")) {
                return s.substring(0, s.length() - 2);
            }
            return s;
        }
        String mant = s.substring(0, e);
        String exp = s.substring(e + 1);
        if (mant.endsWith(".0")) {
            mant = mant.substring(0, mant.length() - 2);
        }
        boolean negExp = exp.startsWith("-");
        if (negExp) {
            exp = exp.substring(1);
        }
        if (exp.length() < 2) {
            exp = "0" + exp;
        }
        return mant + "e" + (negExp ? "-" : "+") + exp;
    }

    /// `tostring` without metamethods.
    public static String rawToString(Object v) {
        if (v == null) {
            return "nil";
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue() ? "true" : "false";
        }
        if (v instanceof Double) {
            return numberToString(((Double) v).doubleValue());
        }
        if (v instanceof String) {
            return (String) v;
        }
        return v.toString();
    }

    /// `tostring`, honouring `__tostring` and `__name`.
    public String tostring(Object v) {
        Object mm = metamethod(v, "__tostring");
        if (mm != null) {
            Object r = call1(mm, v);
            if (!(r instanceof String)) {
                if (r instanceof Double) {
                    return rawToString(r);
                }
                throw LuaError.rt("'__tostring' must return a string");
            }
            return (String) r;
        }
        if (v instanceof LuaTable || v instanceof LuaUserdata) {
            Object name = metamethod(v, "__name");
            if (name instanceof String) {
                return (String) name + ": 0x" + Integer.toHexString(System.identityHashCode(v));
            }
        }
        return rawToString(v);
    }

    /// Primitive equality, no `__eq`.
    public static boolean rawEquals(Object a, Object b) {
        if (a == b) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Double && b instanceof Double) {
            return ((Double) a).doubleValue() == ((Double) b).doubleValue();
        }
        if (a instanceof String && b instanceof String || a instanceof Boolean && b instanceof Boolean) {
            return a.equals(b);
        }
        return false;
    }

    // ------------------------------------------------------------ loading

    /// Compiles source to a function; `chunk` names it in errors.
    public LuaClosure load(String source, String chunk) {
        return load(source, chunk, globals);
    }

    /// Compiles source with `env` as its global table.
    public LuaClosure load(String source, String chunk, LuaTable env) {
        String bytes = Utf8.fromJava(source);
        Ast.Function f = new Parser(bytes, chunk).parseChunk();
        Proto p = new Compiler(chunk).compile(f);
        return new LuaClosure(p, new Cell[0], env);
    }

    // ------------------------------------------------------------ calls from Java

    /// Calls any callable value and returns all results.
    public Object[] call(Object fn, Object... args) {
        if (fn instanceof JavaFunction && fn != pcallFn && fn != xpcallFn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
            Object[] r = ((JavaFunction) fn).call(this, args);
            if (r == YIELD) {
                throw LuaError.rt("attempt to yield across a C-call boundary");
            }
            return r == null ? NONE : r;
        }
        if (fn instanceof LuaClosure) {
            Coroutine co = current;
            if (++nest > MAX_NEST) { // NOPMD AssignmentInOperand - interpreter stack push/pop, kept as one expression on the hot path
                nest--;
                throw LuaError.rt("C stack overflow");
            }
            co.javaNest++;
            int depth = co.frames.size();
            try {
                pushFrame(co, (LuaClosure) fn, args, 0, args.length, -1);
                return execute(co, depth);
            } finally {
                co.javaNest--;
                nest--;
                while (co.frames.size() > depth) {
                    co.frames.remove(co.frames.size() - 1);
                }
            }
        }
        if (fn == pcallFn || fn == xpcallFn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
            return protectedJavaCall(fn == xpcallFn, args); // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
        }
        Object mm = metamethod(fn, "__call");
        if (mm == null) {
            throw LuaError.rt("attempt to call a " + typeName(fn) + " value");
        }
        Object[] a2 = new Object[args.length + 1];
        a2[0] = fn;
        System.arraycopy(args, 0, a2, 1, args.length);
        return call(mm, a2);
    }

    private Object[] protectedJavaCall(boolean x, Object[] args) {
        if (args.length == 0) {
            throw LuaError.rt("bad argument #1 to '" + (x ? "xpcall" : "pcall") + "' (value expected)");
        }
        Object f = args[0];
        Object handler = x ? JavaFunction.arg(args, 1) : null;
        int skip = x ? 2 : 1;
        Object[] rest = new Object[Math.max(0, args.length - skip)];
        System.arraycopy(args, skip, rest, 0, rest.length);
        try {
            Object[] r = call(f, rest);
            Object[] out = new Object[r.length + 1];
            out[0] = Boolean.TRUE;
            System.arraycopy(r, 0, out, 1, r.length);
            return out;
        } catch (LuaError e) {
            if (e.isFatal()) {
                throw e;
            }
            Object ev = e.getValue();
            if (x) {
                ev = call1(handler, ev);
            }
            return new Object[] {Boolean.FALSE, ev};
        } catch (StackOverflowError e) {
            return new Object[] {Boolean.FALSE, "stack overflow"};
        }
    }

    /// Calls and returns the first result.
    public Object call1(Object fn, Object... args) {
        Object[] r = call(fn, args);
        return r.length == 0 ? null : r[0];
    }

    // ------------------------------------------------------------ coroutines

    /// A new suspended coroutine running `fn`.
    public Coroutine newThread(LuaFunction fn) {
        return new Coroutine(fn);
    }

    /// Marks a [JavaFunction]'s return as a yield of `values`.
    public Object[] yield(Object[] values) {
        Coroutine co = current;
        if (co == mainThread) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
            throw LuaError.rt("attempt to yield from outside a coroutine");
        }
        if (co.javaNest > 0) {
            throw LuaError.rt("attempt to yield across metamethod/C-call boundary");
        }
        co.yieldValues = values;
        return YIELD;
    }

    /// Resumes `co` with `args`. Returns `{true, values...}` when it yields
    /// or finishes and `{false, error}` when it fails, as
    /// `coroutine.resume` does. A fatal timeout is rethrown.
    public Object[] resume(Coroutine co, Object[] args) {
        if (co.status == Coroutine.DEAD) {
            return new Object[] {Boolean.FALSE, "cannot resume dead coroutine"};
        }
        if (co.status != Coroutine.SUSPENDED) {
            return new Object[] {Boolean.FALSE, "cannot resume non-suspended coroutine"};
        }
        if (++nest > MAX_NEST) { // NOPMD AssignmentInOperand - interpreter stack push/pop, kept as one expression on the hot path
            nest--;
            return new Object[] {Boolean.FALSE, "C stack overflow"};
        }
        Coroutine prev = current;
        prev.status = Coroutine.NORMAL;
        co.status = Coroutine.RUNNING;
        current = co;
        try {
            Object[] r;
            if (!co.started) {
                co.started = true;
                if (co.fn instanceof LuaClosure) {
                    pushFrame(co, (LuaClosure) co.fn, args, 0, args.length, -1);
                    r = execute(co, 0);
                } else {
                    r = callJavaInThread(co, args);
                }
            } else if (co.frames.isEmpty()) {
                // a Java function that yielded: its resume values are its results
                r = args;
            } else {
                Frame top = co.frames.get(co.frames.size() - 1);
                pushResults(top, args, co.pendingNret);
                r = execute(co, 0);
            }
            if (r == YIELD) {
                co.status = Coroutine.SUSPENDED;
                Object[] y = co.yieldValues;
                co.yieldValues = null;
                return prepend(Boolean.TRUE, y);
            }
            co.status = Coroutine.DEAD;
            return prepend(Boolean.TRUE, r);
        } catch (LuaError e) {
            co.status = Coroutine.DEAD;
            co.frames.clear();
            if (e.isFatal()) {
                throw e;
            }
            return new Object[] {Boolean.FALSE, e.getValue()};
        } catch (StackOverflowError e) {
            co.status = Coroutine.DEAD;
            co.frames.clear();
            return new Object[] {Boolean.FALSE, "stack overflow"};
        } finally {
            current = prev;
            prev.status = Coroutine.RUNNING;
            nest--;
        }
    }

    private Object[] callJavaInThread(Coroutine co, Object[] args) {
        Object fn = co.fn;
        if (fn instanceof JavaFunction && fn != pcallFn && fn != xpcallFn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
            Object[] r = ((JavaFunction) fn).call(this, args);
            return r == null ? NONE : r;
        }
        return call(fn, args);
    }

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] out = new Object[(rest == null ? 0 : rest.length) + 1];
        out[0] = first;
        if (rest != null) {
            System.arraycopy(rest, 0, out, 1, rest.length);
        }
        return out;
    }

    // ------------------------------------------------------------ the loop

    private void pushFrame(Coroutine co, LuaClosure cl, Object[] src, int off, int nargs, int nret) {
        if (co.frames.size() >= MAX_FRAMES) {
            throw LuaError.rt("stack overflow");
        }
        Frame f = new Frame(cl, nret);
        Proto p = cl.proto;
        int np = p.numParams;
        int copy = Math.min(np, nargs);
        System.arraycopy(src, off, f.slots, 0, copy);
        if (p.vararg) {
            int extra = nargs - np;
            if (extra > 0) {
                f.varargs = new Object[extra];
                System.arraycopy(src, off + np, f.varargs, 0, extra);
            } else {
                f.varargs = NONE;
            }
        }
        co.frames.add(f);
    }

    private static void pushResults(Frame f, Object[] r, int nret) {
        if (nret < 0) {
            f.ensure(r.length);
            System.arraycopy(r, 0, f.stk, f.sp, r.length);
            f.sp += r.length;
            f.multi = r.length;
            return;
        }
        f.ensure(nret);
        for (int i = 0; i < nret; i++) {
            f.stk[f.sp++] = i < r.length ? r[i] : null;
        }
    }

    private String where(Frame f) {
        return f.cl.proto.chunk + ":" + f.line() + ": ";
    }

    /// Runs `co` until the frame at `stopDepth` returns (its results) or the
    /// coroutine yields ([#YIELD]).
    private Object[] execute(Coroutine co, int stopDepth) {
        while (true) {
            try {
                return run(co, stopDepth);
            } catch (LuaError e) {
                Frame f = co.frames.isEmpty() ? null : co.frames.get(co.frames.size() - 1);
                if (f != null) {
                    e.position(where(f));
                    e.setLuaTraceback(traceback(co));
                }
                if (!unwind(co, stopDepth, e)) {
                    throw e;
                }
            } catch (StackOverflowError so) {
                LuaError e = LuaError.rt("stack overflow");
                if (!unwind(co, stopDepth, e)) {
                    throw e;
                }
            }
        }
    }

    /// Pops frames to the nearest protected one at or above `stopDepth` and
    /// delivers `false, err` to its caller; false when there is none.
    private boolean unwind(Coroutine co, int stopDepth, LuaError e) {
        if (e.isFatal()) {
            return false;
        }
        for (int i = co.frames.size() - 1; i > stopDepth; i--) {
            Frame f = co.frames.get(i);
            if (f.protect != 0) {
                Object ev = e.getValue();
                Object handler = f.handler;
                while (co.frames.size() > i) {
                    co.frames.remove(co.frames.size() - 1);
                }
                Frame caller = co.frames.get(i - 1);
                if (f.protect == 2) {
                    try {
                        ev = call1(handler, ev);
                    } catch (LuaError he) {
                        ev = he.getValue();
                    }
                }
                pushResults(caller, new Object[] {Boolean.FALSE, ev}, f.nret);
                return true;
            }
        }
        return false;
    }

    /// The Luau call stack of `co`, innermost first.
    String traceback(Coroutine co) {
        StringBuilder b = new StringBuilder();
        for (int i = co.frames.size() - 1; i >= 0; i--) {
            Frame f = co.frames.get(i);
            b.append(f.cl.proto.chunk).append(':').append(f.line());
            String n = f.cl.proto.name;
            if (n != null && !n.equals("main chunk")) {
                b.append(" function ").append(n);
            }
            b.append('\n');
        }
        return b.toString();
    }

    /// The line the Luau code `level` frames up is running (1 = the caller
    /// of the current Java function), or -1.
    int currentLine(int level) {
        Coroutine co = current;
        int i = co.frames.size() - level;
        if (i < 0 || i >= co.frames.size()) {
            return -1;
        }
        return co.frames.get(i).line();
    }

    String currentChunk(int level) {
        Coroutine co = current;
        int i = co.frames.size() - level;
        if (i < 0 || i >= co.frames.size()) {
            return null;
        }
        return co.frames.get(i).cl.proto.chunk;
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    private Object[] run(Coroutine co, int stopDepth) {
        Frame f = co.frames.get(co.frames.size() - 1);
        int[] code = f.code;
        Object[] k = f.consts;
        while (true) {
            int op = code[f.pc++];
            switch (op) {
                case Op.NIL:
                    f.push(null);
                    break;
                case Op.TRUE:
                    f.push(Boolean.TRUE);
                    break;
                case Op.FALSE:
                    f.push(Boolean.FALSE);
                    break;
                case Op.K:
                    f.push(k[code[f.pc++]]);
                    break;
                case Op.LGET:
                    f.push(f.slots[code[f.pc++]]);
                    break;
                case Op.LSET:
                    f.slots[code[f.pc++]] = f.stk[--f.sp];
                    f.stk[f.sp] = null;
                    break;
                case Op.CGET:
                    f.push(((Cell) f.slots[code[f.pc++]]).v);
                    break;
                case Op.CSET:
                    ((Cell) f.slots[code[f.pc++]]).v = f.stk[--f.sp];
                    break;
                case Op.CNEW:
                    f.slots[code[f.pc++]] = new Cell(f.stk[--f.sp]);
                    break;
                case Op.UGET:
                    f.push(f.cl.upvals[code[f.pc++]].v);
                    break;
                case Op.USET:
                    f.cl.upvals[code[f.pc++]].v = f.stk[--f.sp];
                    break;
                case Op.GGET: {
                    String name = (String) k[code[f.pc++]];
                    LuaTable env = f.cl.env;
                    Object v = env.rawget(name);
                    if (v == null && env.getMetatable() != null) {
                        v = index(env, name);
                    }
                    f.push(v);
                    break;
                }
                case Op.GSET: {
                    String name = (String) k[code[f.pc++]];
                    Object v = f.stk[--f.sp];
                    LuaTable env = f.cl.env;
                    if (env.getMetatable() != null) {
                        setIndex(env, name, v);
                    } else {
                        env.rawset(name, v);
                    }
                    break;
                }
                case Op.IGET: {
                    Object key = f.stk[--f.sp];
                    Object t = f.stk[f.sp - 1];
                    f.stk[f.sp - 1] = index(t, key);
                    break;
                }
                case Op.ISET: {
                    Object v = f.stk[--f.sp];
                    Object key = f.stk[--f.sp];
                    Object t = f.stk[--f.sp];
                    setIndex(t, key, v);
                    break;
                }
                case Op.FGET: {
                    Object key = k[code[f.pc++]];
                    Object t = f.stk[f.sp - 1];
                    f.stk[f.sp - 1] = index(t, key);
                    break;
                }
                case Op.FSET: {
                    Object key = k[code[f.pc++]];
                    Object v = f.stk[--f.sp];
                    Object t = f.stk[--f.sp];
                    setIndex(t, key, v);
                    break;
                }
                case Op.SELF: {
                    Object key = k[code[f.pc++]];
                    Object o = f.stk[f.sp - 1];
                    Object m = selfIndex(o, (String) key);
                    f.stk[f.sp - 1] = m;
                    f.push(o);
                    break;
                }
                case Op.BIN: {
                    int bop = code[f.pc++];
                    Object b = f.stk[--f.sp];
                    Object a = f.stk[f.sp - 1];
                    f.stk[f.sp] = null;
                    f.stk[f.sp - 1] = binop(bop, a, b);
                    break;
                }
                case Op.UN: {
                    int uop = code[f.pc++];
                    Object a = f.stk[f.sp - 1];
                    f.stk[f.sp - 1] = unop(uop, a);
                    break;
                }
                case Op.JMP: {
                    int target = code[f.pc];
                    if (target < f.pc && ++steps > budget) { // NOPMD AssignmentInOperand - interpreter stack push/pop, kept as one expression on the hot path
                        throw LuaError.fatal("Script timeout: exhausted allowed execution time");
                    }
                    f.pc = target;
                    break;
                }
                case Op.JF: {
                    Object v = f.stk[--f.sp];
                    f.stk[f.sp] = null;
                    int target = code[f.pc++];
                    if (!truthy(v)) {
                        if (target < f.pc && ++steps > budget) { // NOPMD AssignmentInOperand - interpreter stack push/pop, kept as one expression on the hot path
                            throw LuaError.fatal("Script timeout: exhausted allowed execution time");
                        }
                        f.pc = target;
                    }
                    break;
                }
                case Op.JT: {
                    Object v = f.stk[--f.sp];
                    f.stk[f.sp] = null;
                    int target = code[f.pc++];
                    if (truthy(v)) {
                        f.pc = target;
                    }
                    break;
                }
                case Op.JNIL: {
                    Object v = f.stk[--f.sp];
                    f.stk[f.sp] = null;
                    int target = code[f.pc++];
                    if (v == null) {
                        f.pc = target;
                    }
                    break;
                }
                case Op.AND: {
                    int target = code[f.pc++];
                    if (!truthy(f.stk[f.sp - 1])) {
                        f.pc = target;
                    } else {
                        f.stk[--f.sp] = null;
                    }
                    break;
                }
                case Op.OR: {
                    int target = code[f.pc++];
                    if (truthy(f.stk[f.sp - 1])) {
                        f.pc = target;
                    } else {
                        f.stk[--f.sp] = null;
                    }
                    break;
                }
                case Op.CALL: {
                    int nfixed = code[f.pc++];
                    int nargs = nfixed + (code[f.pc++] != 0 ? f.multi : 0);
                    int nret = code[f.pc++];
                    if (++steps > budget) { // NOPMD AssignmentInOperand - interpreter stack push/pop, kept as one expression on the hot path
                        throw LuaError.fatal("Script timeout: exhausted allowed execution time");
                    }
                    int fnIdx = f.sp - nargs - 1;
                    Object fn = f.stk[fnIdx];
                    while (!(fn instanceof LuaFunction)) {
                        Object mm = metamethod(fn, "__call");
                        if (mm == null) {
                            throw LuaError.rt("attempt to call a " + typeName(fn) + " value");
                        }
                        f.ensure(1);
                        System.arraycopy(f.stk, fnIdx, f.stk, fnIdx + 1, nargs + 1);
                        f.stk[fnIdx] = mm;
                        f.sp++;
                        nargs++;
                        fn = mm;
                    }
                    if (fn instanceof LuaClosure) {
                        pushFrame(co, (LuaClosure) fn, f.stk, fnIdx + 1, nargs, nret);
                        clear(f, fnIdx);
                        f = co.frames.get(co.frames.size() - 1);
                        code = f.code;
                        k = f.consts;
                        break;
                    }
                    if (fn == pcallFn || fn == xpcallFn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                        boolean x = fn == xpcallFn; // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                        Object target = nargs > 0 ? f.stk[fnIdx + 1] : null;
                        if (nargs == 0) {
                            throw LuaError.rt("bad argument #1 to '" + (x ? "xpcall" : "pcall") + "' (value expected)");
                        }
                        if (target instanceof LuaClosure) {
                            int skip = x ? 2 : 1;
                            Object handler = x && nargs > 1 ? f.stk[fnIdx + 2] : null;
                            pushFrame(co, (LuaClosure) target, f.stk, fnIdx + 1 + skip, Math.max(0, nargs - skip), nret);
                            clear(f, fnIdx);
                            f = co.frames.get(co.frames.size() - 1);
                            f.protect = x ? 2 : 1;
                            f.handler = handler;
                            code = f.code;
                            k = f.consts;
                            break;
                        }
                    }
                    Object[] args = new Object[nargs];
                    System.arraycopy(f.stk, fnIdx + 1, args, 0, nargs);
                    clear(f, fnIdx);
                    Object[] r;
                    if (fn == pcallFn || fn == xpcallFn) { // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                        r = protectedJavaCall(fn == xpcallFn, args); // NOPMD CompareObjectsWithEquals - identity is the intent: a sentinel, a shared box or a tree node
                    } else {
                        r = ((JavaFunction) fn).call(this, args);
                    }
                    if (r == YIELD) {
                        co.pendingNret = nret;
                        if (stopDepth != 0) {
                            throw LuaError.rt("attempt to yield across metamethod/C-call boundary");
                        }
                        return YIELD;
                    }
                    pushResults(f, r == null ? NONE : r, nret);
                    break;
                }
                case Op.RET: {
                    int nfixed = code[f.pc++];
                    int n = nfixed + (code[f.pc++] != 0 ? f.multi : 0);
                    Object[] r;
                    if (f.protect != 0) {
                        r = new Object[n + 1];
                        r[0] = Boolean.TRUE;
                        System.arraycopy(f.stk, f.sp - n, r, 1, n);
                    } else if (n == 0) {
                        r = NONE;
                    } else {
                        r = new Object[n];
                        System.arraycopy(f.stk, f.sp - n, r, 0, n);
                    }
                    co.frames.remove(co.frames.size() - 1);
                    if (co.frames.size() <= stopDepth) {
                        return r;
                    }
                    int nret = f.nret;
                    f = co.frames.get(co.frames.size() - 1);
                    code = f.code;
                    k = f.consts;
                    pushResults(f, r, nret);
                    break;
                }
                case Op.VARARG: {
                    int nret = code[f.pc++];
                    pushResults(f, f.varargs, nret);
                    break;
                }
                case Op.CLOSURE: {
                    Proto p = f.cl.proto.protos[code[f.pc++]];
                    Cell[] ups = new Cell[p.upIndex.length];
                    for (int i = 0; i < ups.length; i++) {
                        ups[i] = p.upFromLocal[i] ? (Cell) f.slots[p.upIndex[i]] : f.cl.upvals[p.upIndex[i]];
                    }
                    f.push(new LuaClosure(p, ups, f.cl.env));
                    break;
                }
                case Op.NEWT:
                    f.push(new LuaTable(code[f.pc++]));
                    break;
                case Op.TSETL: {
                    int start = code[f.pc++];
                    int n = code[f.pc++];
                    if (code[f.pc++] != 0) { // NOPMD AssignmentInOperand - interpreter stack push/pop, kept as one expression on the hot path
                        n += f.multi;
                    }
                    LuaTable t = (LuaTable) f.stk[f.sp - n - 1];
                    for (int i = 0; i < n; i++) {
                        t.rawseti(start + i, f.stk[f.sp - n + i]);
                    }
                    clear(f, f.sp - n);
                    break;
                }
                case Op.TSETK: {
                    Object v = f.stk[--f.sp];
                    Object key = f.stk[--f.sp];
                    ((LuaTable) f.stk[f.sp - 1]).rawset(key, v);
                    break;
                }
                case Op.TSETF: {
                    Object key = k[code[f.pc++]];
                    Object v = f.stk[--f.sp];
                    ((LuaTable) f.stk[f.sp - 1]).rawset(key, v);
                    break;
                }
                case Op.POP:
                    clear(f, f.sp - code[f.pc++]);
                    break;
                case Op.DUP:
                    f.push(f.stk[f.sp - 1]);
                    break;
                case Op.CONCATN: {
                    int n = code[f.pc++];
                    StringBuilder b = new StringBuilder();
                    for (int i = f.sp - n; i < f.sp; i++) {
                        b.append(tostring(f.stk[i]));
                    }
                    clear(f, f.sp - n);
                    f.push(b.toString());
                    break;
                }
                case Op.FORPREP: {
                    int base = code[f.pc++];
                    Object[] s = f.slots;
                    for (int i = 0; i < 3; i++) {
                        Double d = toNumber(s[base + i]);
                        if (d == null) {
                            throw LuaError.rt("invalid 'for' " + (i == 0 ? "initial value" : i == 1 ? "limit" : "step")
                                    + " (number expected, got " + typeName(s[base + i]) + ")");
                        }
                        s[base + i] = d;
                    }
                    break;
                }
                case Op.FORTEST: {
                    int base = code[f.pc++];
                    int target = code[f.pc++];
                    double i = ((Double) f.slots[base]).doubleValue();
                    double lim = ((Double) f.slots[base + 1]).doubleValue();
                    double step = ((Double) f.slots[base + 2]).doubleValue();
                    if (step > 0 && i > lim || step < 0 && i < lim || step == 0 || i != i) {
                        f.pc = target;
                    }
                    break;
                }
                case Op.FORINC: {
                    int base = code[f.pc++];
                    if (++steps > budget) { // NOPMD AssignmentInOperand - interpreter stack push/pop, kept as one expression on the hot path
                        throw LuaError.fatal("Script timeout: exhausted allowed execution time");
                    }
                    f.slots[base] = num(((Double) f.slots[base]).doubleValue() + ((Double) f.slots[base + 2]).doubleValue());
                    break;
                }
                case Op.GFORPREP: {
                    int base = code[f.pc++];
                    Object it = f.slots[base];
                    if (!(it instanceof LuaFunction)) {
                        Object iter = metamethod(it, "__iter");
                        if (iter != null) {
                            Object[] r = call(iter, it);
                            f.slots[base] = r.length > 0 ? r[0] : null;
                            f.slots[base + 1] = r.length > 1 ? r[1] : null;
                            f.slots[base + 2] = r.length > 2 ? r[2] : null;
                        } else if (it instanceof LuaTable && metamethod(it, "__call") == null) {
                            f.slots[base] = Stdlib.NEXT;
                            f.slots[base + 1] = it;
                            f.slots[base + 2] = null;
                        }
                    }
                    break;
                }
                default:
                    throw new LuaError("bad opcode " + op);
            }
        }
    }

    private static void clear(Frame f, int newSp) {
        for (int i = newSp; i < f.sp; i++) {
            f.stk[i] = null;
        }
        f.sp = newSp;
    }

    // ------------------------------------------------------------ operations

    /// `t[k]` with `__index`.
    public Object index(Object t, Object key) {
        for (int loop = 0; loop < 100; loop++) {
            Object h;
            if (t instanceof LuaTable) {
                LuaTable tt = (LuaTable) t;
                Object v = tt.rawget(key);
                if (v != null) {
                    return v;
                }
                LuaTable mt = tt.getMetatable();
                if (mt == null) {
                    return null;
                }
                h = mt.rawget("__index");
                if (h == null) {
                    return null;
                }
            } else {
                h = metamethod(t, "__index");
                if (h == null) {
                    throw LuaError.rt("attempt to index " + typeName(t) + " with " + describeKey(key));
                }
            }
            if (h instanceof LuaFunction) {
                return call1(h, t, key);
            }
            t = h;
        }
        throw LuaError.rt("'__index' chain too long; possible loop");
    }

    private Object selfIndex(Object o, String key) {
        if (o instanceof String) {
            Object m = stringMeta == null ? null : index(stringMeta.rawget("__index"), key);
            if (m == null) {
                throw LuaError.rt("attempt to call missing method '" + key + "' of string");
            }
            return m;
        }
        Object m = index(o, key);
        if (m == null) {
            throw LuaError.rt("attempt to call missing method '" + key + "' of " + typeName(o));
        }
        return m;
    }

    private static String describeKey(Object key) {
        if (key instanceof String) {
            return "'" + key + "'";
        }
        return typeName(key);
    }

    /// `t[k] = v` with `__newindex`.
    public void setIndex(Object t, Object key, Object v) {
        for (int loop = 0; loop < 100; loop++) {
            Object h;
            if (t instanceof LuaTable) {
                LuaTable tt = (LuaTable) t;
                LuaTable mt = tt.getMetatable();
                if (mt == null || tt.rawget(key) != null) {
                    tt.rawset(key, v);
                    return;
                }
                h = mt.rawget("__newindex");
                if (h == null) {
                    tt.rawset(key, v);
                    return;
                }
            } else {
                h = metamethod(t, "__newindex");
                if (h == null) {
                    throw LuaError.rt("attempt to index " + typeName(t) + " with " + describeKey(key));
                }
            }
            if (h instanceof LuaFunction) {
                call(h, t, key, v);
                return;
            }
            t = h;
        }
        throw LuaError.rt("'__newindex' chain too long; possible loop");
    }

    private static final String[] EVENTS = {
        "__add", "__sub", "__mul", "__div", "__mod", "__pow", "__idiv", "__concat"
    };

    /// A binary operator by [Op] id.
    Object binop(int op, Object a, Object b) {
        if (a instanceof Double && b instanceof Double) {
            double x = ((Double) a).doubleValue();
            double y = ((Double) b).doubleValue();
            switch (op) {
                case Op.ADD:
                    return num(x + y);
                case Op.SUB:
                    return num(x - y);
                case Op.MUL:
                    return num(x * y);
                case Op.DIV:
                    return num(x / y);
                case Op.MOD:
                    return num(mod(x, y));
                case Op.POW:
                    return num(pow(x, y));
                case Op.IDIV:
                    return num(Math.floor(x / y));
                case Op.EQ:
                    return x == y ? Boolean.TRUE : Boolean.FALSE;
                case Op.NE:
                    return x != y ? Boolean.TRUE : Boolean.FALSE;
                case Op.LT:
                    return x < y ? Boolean.TRUE : Boolean.FALSE;
                case Op.LE:
                    return x <= y ? Boolean.TRUE : Boolean.FALSE;
                case Op.GT:
                    return x > y ? Boolean.TRUE : Boolean.FALSE;
                case Op.GE:
                    return x >= y ? Boolean.TRUE : Boolean.FALSE;
                default:
                    break;
            }
        }
        switch (op) {
            case Op.EQ:
                return equalsOp(a, b) ? Boolean.TRUE : Boolean.FALSE;
            case Op.NE:
                return equalsOp(a, b) ? Boolean.FALSE : Boolean.TRUE;
            case Op.LT:
                return lessThan(a, b) ? Boolean.TRUE : Boolean.FALSE;
            case Op.LE:
                return lessEqual(a, b) ? Boolean.TRUE : Boolean.FALSE;
            case Op.GT:
                return lessThan(b, a) ? Boolean.TRUE : Boolean.FALSE;
            case Op.GE:
                return lessEqual(b, a) ? Boolean.TRUE : Boolean.FALSE;
            case Op.CONCAT:
                if ((a instanceof String || a instanceof Double) && (b instanceof String || b instanceof Double)) {
                    return rawToString(a).concat(rawToString(b));
                }
                return arithMeta(op, a, b);
            default:
                Double x = a instanceof String ? parseNumber((String) a) : null;
                Double y = b instanceof String ? parseNumber((String) b) : null;
                if ((x != null || a instanceof Double) && (y != null || b instanceof Double)) {
                    return binop(op, x != null ? x : a, y != null ? y : b);
                }
                return arithMeta(op, a, b);
        }
    }

    private Object arithMeta(int op, Object a, Object b) {
        String ev = EVENTS[op];
        Object mm = metamethod(a, ev);
        if (mm == null) {
            mm = metamethod(b, ev);
        }
        if (mm == null) {
            if (op == Op.CONCAT) {
                throw LuaError.rt("attempt to concatenate " + typeName(a) + " with " + typeName(b));
            }
            throw LuaError.rt("attempt to perform arithmetic (" + ev.substring(2) + ") on " + typeName(a)
                    + (typeName(a).equals(typeName(b)) ? "" : " and " + typeName(b)));
        }
        return call1(mm, a, b);
    }

    static double mod(double x, double y) {
        return x - Math.floor(x / y) * y;
    }

    static double pow(double x, double y) {
        if (y == 2) {
            return x * x;
        }
        if (y == 0.5) {
            return Math.sqrt(x);
        }
        return MathUtil.pow(x, y);
    }

    /// `==` with `__eq`.
    public boolean equalsOp(Object a, Object b) {
        if (rawEquals(a, b)) {
            return true;
        }
        if (a instanceof LuaTable && b instanceof LuaTable || a instanceof LuaUserdata && b instanceof LuaUserdata) {
            Object mm = metamethod(a, "__eq");
            if (mm == null) {
                mm = metamethod(b, "__eq");
            }
            if (mm != null) {
                return truthy(call1(mm, a, b));
            }
        }
        return false;
    }

    /// `<` with `__lt`.
    public boolean lessThan(Object a, Object b) {
        if (a instanceof Double && b instanceof Double) {
            return ((Double) a).doubleValue() < ((Double) b).doubleValue();
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b) < 0;
        }
        Object mm = metamethod(a, "__lt");
        if (mm == null) {
            mm = metamethod(b, "__lt");
        }
        if (mm == null) {
            throw compareError(a, b, "<");
        }
        return truthy(call1(mm, a, b));
    }

    /// `<=` with `__le`.
    public boolean lessEqual(Object a, Object b) {
        if (a instanceof Double && b instanceof Double) {
            return ((Double) a).doubleValue() <= ((Double) b).doubleValue();
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b) <= 0;
        }
        Object mm = metamethod(a, "__le");
        if (mm == null) {
            mm = metamethod(b, "__le");
        }
        if (mm == null) {
            throw compareError(a, b, "<=");
        }
        return truthy(call1(mm, a, b));
    }

    private static LuaError compareError(Object a, Object b, String op) {
        String ta = typeName(a);
        String tb = typeName(b);
        if (ta.equals(tb)) {
            return LuaError.rt("attempt to compare two " + ta + " values");
        }
        return LuaError.rt("attempt to compare " + ta + " " + op + " " + tb);
    }

    Object unop(int op, Object a) {
        switch (op) {
            case Op.NOT:
                return truthy(a) ? Boolean.FALSE : Boolean.TRUE;
            case Op.NEG: {
                if (a instanceof Double) {
                    return num(-((Double) a).doubleValue());
                }
                Double d = a instanceof String ? parseNumber((String) a) : null;
                if (d != null) {
                    return num(-d.doubleValue());
                }
                Object mm = metamethod(a, "__unm");
                if (mm == null) {
                    throw LuaError.rt("attempt to perform arithmetic (unm) on " + typeName(a));
                }
                return call1(mm, a, a);
            }
            default:
                return len(a);
        }
    }

    /// `#v` with `__len`.
    public Object len(Object a) {
        if (a instanceof String) {
            return num(((String) a).length());
        }
        Object mm = metamethod(a, "__len");
        if (mm != null) {
            return call1(mm, a);
        }
        if (a instanceof LuaTable) {
            return num(((LuaTable) a).length());
        }
        throw LuaError.rt("attempt to get length of a " + typeName(a) + " value");
    }
}
