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
package com.codename1.impl.interp;

import java.util.Vector;

/// Executes a pushed bundle.
///
/// ### Frames
///
/// One interpreted frame is one real Java frame: [#execute] recurses. The
/// alternative -- heap-allocated frames driven by a trampoline -- would be
/// tidier and would break `Display.invokeAndBlock`, which on ParparVM runs a
/// nested event loop on the caller's native stack. Every blocking Codename One
/// idiom (`Dialog.show()`, a synchronous `NetworkManager` call) is built on
/// that, so interpreted code has to be able to sit in the middle of it. The
/// cost is that interpreted depth is bounded by the real stack, which
/// [#maxDepth] caps well short of it.
///
/// ### Fuel
///
/// Every back edge and method entry decrements a counter. At zero the
/// interpreter checks whether it has been asked to stop and whether it has
/// outstayed its budget on the event thread. This is what makes a runaway
/// pushed program recoverable instead of a hung app -- the existing BeanShell
/// playground has no such check, and `while(true){}` there wedges the EDT
/// permanently. Accounting deliberately pauses while inside a host call, or a
/// legitimate `invokeAndBlock` waiting on the network would look like a runaway
/// loop.
///
/// @author Shai Almog
public final class InterpRuntime {
    private final InterpBundle bundle;
    private final InterpLinker linker;
    private final InterpObjectFactory factory;

    private int maxDepth = 512;
    private int fuelPerCheck = 20000;
    private long edtBudgetMs = 2000;

    private volatile boolean cancelRequested;

    /// The last exception interpreted code threw, and the interpreted frames it
    /// was thrown from.
    ///
    /// On the runtime rather than on the thread state, because the thread that
    /// reports a failure is not the thread that ran the program -- a pushed
    /// main runs on the event thread and the socket thread is what answers the
    /// push. The pair is only ever read through an identity check, so the worst
    /// a race can do is decline to produce a stack.
    private volatile Object lastThrown;
    private volatile String[] lastThrownStack;

    /// The host method being called when [#lastThrown] was thrown, if it was
    /// thrown by one rather than by interpreted code.
    private volatile String lastHostCall;

    /// Execution state that belongs to one thread, not to the runtime.
    ///
    /// The runtime is genuinely entered from several threads at once: the
    /// thread running a pushed `main`, and the event thread every time the
    /// framework calls an interpreted `paint` or listener through a generated
    /// shim. Holding depth, fuel and the call stack on the runtime meant those
    /// threads corrupted each other's -- the depth cap tripping on the wrong
    /// thread, a stack trace naming another thread's frames.
    private static final class ThreadState {
        int depth;
        int fuel;
        int hostCallDepth;
        long runStartMs;
        final Vector callStack = new Vector();
    }

    private final ThreadLocal threadState = new ThreadLocal();

    private ThreadState state() {
        ThreadState s = (ThreadState)threadState.get();
        if (s == null) {
            s = new ThreadState();
            s.fuel = fuelPerCheck;
            // Each thread's budget starts when it first enters the interpreter.
            // A shared start time would make a callback arriving an hour into
            // the session look like a program that had run for an hour.
            s.runStartMs = System.currentTimeMillis();
            threadState.set(s);
        }
        return s;
    }

    public InterpRuntime(InterpBundle bundle, InterpLinker linker, InterpObjectFactory factory) {
        this.bundle = bundle;
        this.linker = linker;
        this.factory = factory;
    }

    /// Maximum interpreted call depth. Exceeding it raises an interpreted
    /// `StackOverflowError` rather than letting the real stack overflow, which
    /// on a device is a process death with no diagnosis.
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /// Wall-clock budget for a single run on the event thread, in
    /// milliseconds. Zero disables the check.
    public void setEdtBudgetMs(long edtBudgetMs) {
        this.edtBudgetMs = edtBudgetMs;
    }

    /// Asks the running program to stop at the next checkpoint. Safe to call
    /// from another thread -- this is what the Stop button uses.
    public void requestCancel() {
        cancelRequested = true;
    }

    public InterpBundle getBundle() {
        return bundle;
    }

    /// Runs the bundle's main class.
    public Object runMain(String[] args) throws Throwable {
        String main = bundle.getMainClass();
        if (main == null) {
            throw new IllegalStateException("bundle declares no main class");
        }
        InterpClass c = bundle.findClass(main);
        if (c == null) {
            throw new IllegalStateException("main class " + main + " is not in the bundle");
        }
        InterpMethod m = c.declaredMethod("main", "([Ljava/lang/String;)V");
        cancelRequested = false;
        ensureInitialized(c);
        if (m != null) {
            return invokeInterpreted(m, null, new Object[]{args});
        }
        // A real Codename One application has no main: its entry point is a
        // Lifecycle subclass, and the platform calls init then start. Running
        // one is the whole point of this runtime, so that shape is entered the
        // way the platform would enter it.
        if (extendsHost(c, "com/codename1/system/Lifecycle")) {
            return runLifecycle(c);
        }
        throw new IllegalStateException(main + " has neither main(String[]) nor"
                + " a Lifecycle to start");
    }

    /// Whether this interpreted class has the named host class as a supertype.
    ///
    /// [InterpClass#isSubclassOfInterp] cannot answer it: as its name says, it
    /// walks the interpreted chain, and a real application's superclass --
    /// Lifecycle -- lives in the app, not in the bundle.
    private boolean extendsHost(InterpClass c, String hostName) {
        Vector hostSupertypes = new Vector();
        c.collectHostSupertypes(hostSupertypes);
        for (int i = 0; i < hostSupertypes.size(); i++) {
            int ext = ((Integer)hostSupertypes.elementAt(i)).intValue();
            if (hostName.equals(externOwnerName(ext))) {
                return true;
            }
        }
        return false;
    }

    /// Starts an interpreted Lifecycle the way the platform starts one.
    ///
    /// The object is constructed through the ordinary interpreted path, so it
    /// gets its generated peer and the framework sees a real Lifecycle. `init`
    /// receives null: the platform passes a native context that means nothing
    /// to interpreted code, and Lifecycle's own init ignores it.
    private Object runLifecycle(InterpClass c) throws Throwable {
        InterpMethod ctor = c.declaredMethod("<init>", "()V");
        if (ctor == null) {
            throw new IllegalStateException(c.getName().replace('/', '.')
                    + " is a Lifecycle with no no-argument constructor");
        }
        InterpObject app = new InterpObject(c);
        app.runtime = this;
        invokeInterpreted(ctor, app, new Object[0]);

        Object target = app.hostPeer != null ? app.hostPeer : app;
        callLifecycle(c, app, "init", "(Ljava/lang/Object;)V", new Object[]{null});
        callLifecycle(c, app, "start", "()V", new Object[0]);
        return target;
    }

    private void callLifecycle(InterpClass c, InterpObject app, String name, String desc,
                               Object[] args) throws Throwable {
        InterpMethod m = c.resolve(name, desc);
        if (m != null && !m.isAbstract()) {
            invokeInterpreted(m, app, args);
            return;
        }
        // Not overridden: Lifecycle's own implementation, reached through the
        // peer, which is what the framework would have called.
        if (app.hostPeer != null) {
            hostCall(app.hostPeerOwner, "super_" + name, desc, app.hostPeer, args, false);
        }
    }

    /// Returned by [#dispatch] when the interpreted class does not override the
    /// method, telling the generated shim to call `super` instead.
    ///
    /// A sentinel rather than null, because null is a perfectly good return
    /// value for a method the interpreted class *does* override.
    public static final Object NOT_OVERRIDDEN = new Object();

    /// Entry point for a generated shim: run the interpreted override of this
    /// method if there is one, otherwise report that there is not.
    ///
    /// The shim is a framework subclass compiled into the app, so every one of
    /// its overridable methods routes here. Methods the pushed class does not
    /// override have to cost as little as possible -- they are on the framework's
    /// own hot paths -- which is why the miss returns immediately rather than
    /// raising anything.
    public Object dispatch(InterpObject object, String name, String descriptor, Object[] args) {
        if (object == null) {
            return NOT_OVERRIDDEN;
        }
        InterpMethod m = object.getType().resolve(name, descriptor);
        if (m == null || m.isAbstract()) {
            return NOT_OVERRIDDEN;
        }
        try {
            return invoke(m, object, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            // The framework called us; it cannot be given a checked exception
            // its own signature does not declare.
            throw new InterpThrowable(t, snapshotStack());
        }
    }

    /// Invokes an interpreted method from host code -- an overridden `paint`,
    /// an `actionPerformed`, or a proxied interface method.
    ///
    /// A returned interpreted object is handed back as its host peer: the
    /// caller is host code, which can do nothing with an [InterpObject]. Calls
    /// that stay inside the interpreter use the internal path instead, where
    /// the interpreted identity is the thing that matters.
    public Object invoke(InterpMethod m, Object receiver, Object[] args) throws Throwable {
        Object result = invokeInterpreted(m, receiver, args);
        if (result instanceof InterpObject) {
            InterpObject io = (InterpObject)result;
            return io.hostPeer != null ? io.hostPeer : io;
        }
        return result;
    }

    /// Invokes an interpreted method without translating the result at the host
    /// boundary.
    private Object invokeInterpreted(InterpMethod m, Object receiver, Object[] args)
            throws Throwable {
        InterpFrame f = new InterpFrame(m);
        int slot = 0;
        if (!m.isStatic()) {
            f.setLocalRef(slot++, receiver);
        }
        for (int i = 0; i < m.argKinds.length; i++) {
            int kind = m.argKinds[i];
            Object a = args == null || i >= args.length ? null : args[i];
            if (kind == InterpOpcodes.RET_OBJECT) {
                f.setLocalRef(slot++, fromHost(a));
            } else if (InterpOpcodes.isCategory2(kind)) {
                f.setLocalLong(slot, InterpValues.unbox(kind, a));
                slot += 2;
            } else {
                f.setLocalInt(slot++, (int)InterpValues.unbox(kind, a));
            }
        }
        if (!m.isSynchronized()) {
            return execute(f);
        }
        // A synchronized method locks for its whole duration, and the body
        // contains no monitor instruction to hook -- ACC_SYNCHRONIZED is the
        // only sign of it. The peer is preferred as the lock where there is
        // one, because the peer is the object host code has and would lock.
        Object lock;
        if (m.isStatic()) {
            lock = m.getOwner();
        } else if (receiver instanceof InterpObject
                && ((InterpObject)receiver).hostPeer != null) {
            lock = ((InterpObject)receiver).hostPeer;
        } else {
            lock = receiver;
        }
        if (lock == null) {
            return execute(f);
        }
        synchronized (lock) {
            return execute(f);
        }
    }

    // ---------------------------------------------------------------- engine

    private Object execute(InterpFrame f) throws Throwable {
        ThreadState st = state();
        if (st.depth == 0) {
            // The budget covers one entry into the interpreter, not the age of
            // the session. Measuring from the start of the run made every
            // callback that arrived more than edtBudgetMs after the program
            // began -- which is every button press in a real application --
            // fail instantly with "ran without yielding", having done nothing.
            // The framework enters here for each callback, so this is the point
            // at which the clock starts.
            st.runStartMs = System.currentTimeMillis();
            st.fuel = fuelPerCheck;
        }
        if (++st.depth > maxDepth) {
            st.depth--;
            throw new InterpThrowable(new StackOverflowError(
                    "interpreted call depth exceeded " + maxDepth), snapshotStack());
        }
        st.callStack.addElement(f);
        try {
            return run(f);
        } finally {
            st.callStack.removeElementAt(st.callStack.size() - 1);
            st.depth--;
        }
    }

    /// Returned by [#run] when a `monitorexit` released the monitor the
    /// enclosing level is holding, so execution continues there -- outside the
    /// Java `synchronized` block, which is what releases the real lock.
    private static final Object MONITOR_RELEASED = new Object();

    private Object run(InterpFrame f) throws Throwable {
        return run(f, 0, false);
    }

    private Object run(InterpFrame f, int startInsn, boolean insideMonitor) throws Throwable {
        final InterpMethod m = f.method;
        final int[] code = m.code;
        int insn = startInsn;

        while (true) {
            f.insn = insn;
            int pc = m.instructionOffsets[insn];
            int op = code[pc];
            int next = insn + 1;

            try {
                switch (op) {
                    case InterpOpcodes.NOP:
                        break;
                    case InterpOpcodes.ACONST_NULL:
                        f.pushRef(null);
                        break;
                    case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                        f.pushInt(op - InterpOpcodes.ICONST_0);
                        break;
                    case InterpOpcodes.LCONST_0:
                    case InterpOpcodes.LCONST_1:
                        f.pushLong(op - InterpOpcodes.LCONST_0);
                        break;
                    case 11: case 12: case 13:
                        f.pushFloat(op - InterpOpcodes.FCONST_0);
                        break;
                    case InterpOpcodes.DCONST_0:
                    case InterpOpcodes.DCONST_1:
                        f.pushDouble(op - InterpOpcodes.DCONST_0);
                        break;
                    case InterpOpcodes.BIPUSH:
                    case InterpOpcodes.SIPUSH:
                        f.pushInt(code[pc + 1]);
                        break;
                    case InterpOpcodes.LDC:
                        ldc(f, code[pc + 1], code[pc + 2]);
                        break;

                    case InterpOpcodes.ILOAD:
                    case InterpOpcodes.FLOAD:
                        f.pushInt((int)f.prim[code[pc + 1]]);
                        break;
                    case InterpOpcodes.LLOAD:
                    case InterpOpcodes.DLOAD:
                        f.pushLong(f.prim[code[pc + 1]]);
                        break;
                    case InterpOpcodes.ALOAD:
                        f.pushRef(f.refs[code[pc + 1]]);
                        break;
                    case InterpOpcodes.ISTORE:
                    case InterpOpcodes.FSTORE:
                        f.setLocalInt(code[pc + 1], f.popInt());
                        break;
                    case InterpOpcodes.LSTORE:
                    case InterpOpcodes.DSTORE:
                        f.setLocalLong(code[pc + 1], f.popLong());
                        break;
                    case InterpOpcodes.ASTORE:
                        f.setLocalRef(code[pc + 1], f.popRef());
                        break;

                    case InterpOpcodes.IALOAD: case InterpOpcodes.LALOAD:
                    case InterpOpcodes.FALOAD: case InterpOpcodes.DALOAD:
                    case InterpOpcodes.AALOAD: case InterpOpcodes.BALOAD:
                    case InterpOpcodes.CALOAD: case InterpOpcodes.SALOAD:
                        arrayLoad(f, op);
                        break;
                    case InterpOpcodes.IASTORE: case InterpOpcodes.LASTORE:
                    case InterpOpcodes.FASTORE: case InterpOpcodes.DASTORE:
                    case InterpOpcodes.AASTORE: case InterpOpcodes.BASTORE:
                    case InterpOpcodes.CASTORE: case InterpOpcodes.SASTORE:
                        arrayStore(f, op);
                        break;

                    case InterpOpcodes.POP:
                        f.sp--;
                        break;
                    case InterpOpcodes.POP2:
                        f.sp -= 2;
                        break;
                    case InterpOpcodes.DUP:
                        dupSlots(f, 1, 0);
                        break;
                    case InterpOpcodes.DUP_X1:
                        dupSlots(f, 1, 1);
                        break;
                    case InterpOpcodes.DUP_X2:
                        dupSlots(f, 1, 2);
                        break;
                    case InterpOpcodes.DUP2:
                        dupSlots(f, 2, 0);
                        break;
                    case InterpOpcodes.DUP2_X1:
                        dupSlots(f, 2, 1);
                        break;
                    case InterpOpcodes.DUP2_X2:
                        dupSlots(f, 2, 2);
                        break;
                    case InterpOpcodes.SWAP: {
                        long p = f.stackPrim[f.sp - 1];
                        Object r = f.stackRefs[f.sp - 1];
                        f.stackPrim[f.sp - 1] = f.stackPrim[f.sp - 2];
                        f.stackRefs[f.sp - 1] = f.stackRefs[f.sp - 2];
                        f.stackPrim[f.sp - 2] = p;
                        f.stackRefs[f.sp - 2] = r;
                        break;
                    }

                    case InterpOpcodes.IADD: f.pushInt(f.popInt() + f.popInt()); break;
                    case InterpOpcodes.LADD: f.pushLong(f.popLong() + f.popLong()); break;
                    case InterpOpcodes.FADD: f.pushFloat(f.popFloat() + f.popFloat()); break;
                    case InterpOpcodes.DADD: f.pushDouble(f.popDouble() + f.popDouble()); break;
                    case InterpOpcodes.ISUB: { int b = f.popInt(); f.pushInt(f.popInt() - b); break; }
                    case InterpOpcodes.LSUB: { long b = f.popLong(); f.pushLong(f.popLong() - b); break; }
                    case InterpOpcodes.FSUB: { float b = f.popFloat(); f.pushFloat(f.popFloat() - b); break; }
                    case InterpOpcodes.DSUB: { double b = f.popDouble(); f.pushDouble(f.popDouble() - b); break; }
                    case InterpOpcodes.IMUL: f.pushInt(f.popInt() * f.popInt()); break;
                    case InterpOpcodes.LMUL: f.pushLong(f.popLong() * f.popLong()); break;
                    case InterpOpcodes.FMUL: f.pushFloat(f.popFloat() * f.popFloat()); break;
                    case InterpOpcodes.DMUL: f.pushDouble(f.popDouble() * f.popDouble()); break;
                    case InterpOpcodes.IDIV: {
                        int b = f.popInt();
                        if (b == 0) {
                            throw new InterpThrowable(new ArithmeticException("/ by zero"), snapshotStack());
                        }
                        f.pushInt(f.popInt() / b);
                        break;
                    }
                    case InterpOpcodes.LDIV: {
                        long b = f.popLong();
                        if (b == 0) {
                            throw new InterpThrowable(new ArithmeticException("/ by zero"), snapshotStack());
                        }
                        f.pushLong(f.popLong() / b);
                        break;
                    }
                    case InterpOpcodes.FDIV: { float b = f.popFloat(); f.pushFloat(f.popFloat() / b); break; }
                    case InterpOpcodes.DDIV: { double b = f.popDouble(); f.pushDouble(f.popDouble() / b); break; }
                    case InterpOpcodes.IREM: {
                        int b = f.popInt();
                        if (b == 0) {
                            throw new InterpThrowable(new ArithmeticException("/ by zero"), snapshotStack());
                        }
                        f.pushInt(f.popInt() % b);
                        break;
                    }
                    case InterpOpcodes.LREM: {
                        long b = f.popLong();
                        if (b == 0) {
                            throw new InterpThrowable(new ArithmeticException("/ by zero"), snapshotStack());
                        }
                        f.pushLong(f.popLong() % b);
                        break;
                    }
                    case InterpOpcodes.FREM: { float b = f.popFloat(); f.pushFloat(f.popFloat() % b); break; }
                    case InterpOpcodes.DREM: { double b = f.popDouble(); f.pushDouble(f.popDouble() % b); break; }
                    case InterpOpcodes.INEG: f.pushInt(-f.popInt()); break;
                    case InterpOpcodes.LNEG: f.pushLong(-f.popLong()); break;
                    case InterpOpcodes.FNEG: f.pushFloat(-f.popFloat()); break;
                    case InterpOpcodes.DNEG: f.pushDouble(-f.popDouble()); break;
                    case InterpOpcodes.ISHL: { int b = f.popInt(); f.pushInt(f.popInt() << b); break; }
                    case InterpOpcodes.LSHL: { int b = f.popInt(); f.pushLong(f.popLong() << b); break; }
                    case InterpOpcodes.ISHR: { int b = f.popInt(); f.pushInt(f.popInt() >> b); break; }
                    case InterpOpcodes.LSHR: { int b = f.popInt(); f.pushLong(f.popLong() >> b); break; }
                    case InterpOpcodes.IUSHR: { int b = f.popInt(); f.pushInt(f.popInt() >>> b); break; }
                    case InterpOpcodes.LUSHR: { int b = f.popInt(); f.pushLong(f.popLong() >>> b); break; }
                    case InterpOpcodes.IAND: f.pushInt(f.popInt() & f.popInt()); break;
                    case InterpOpcodes.LAND: f.pushLong(f.popLong() & f.popLong()); break;
                    case InterpOpcodes.IOR: f.pushInt(f.popInt() | f.popInt()); break;
                    case InterpOpcodes.LOR: f.pushLong(f.popLong() | f.popLong()); break;
                    case InterpOpcodes.IXOR: f.pushInt(f.popInt() ^ f.popInt()); break;
                    case InterpOpcodes.LXOR: f.pushLong(f.popLong() ^ f.popLong()); break;
                    case InterpOpcodes.IINC:
                        f.prim[code[pc + 1]] = (int)f.prim[code[pc + 1]] + code[pc + 2];
                        break;

                    case InterpOpcodes.I2L: f.pushLong(f.popInt()); break;
                    case InterpOpcodes.I2F: f.pushFloat(f.popInt()); break;
                    case InterpOpcodes.I2D: f.pushDouble(f.popInt()); break;
                    case InterpOpcodes.L2I: f.pushInt((int)f.popLong()); break;
                    case InterpOpcodes.L2F: f.pushFloat(f.popLong()); break;
                    case InterpOpcodes.L2D: f.pushDouble(f.popLong()); break;
                    case InterpOpcodes.F2I: f.pushInt((int)f.popFloat()); break;
                    case InterpOpcodes.F2L: f.pushLong((long)f.popFloat()); break;
                    case InterpOpcodes.F2D: f.pushDouble(f.popFloat()); break;
                    case InterpOpcodes.D2I: f.pushInt((int)f.popDouble()); break;
                    case InterpOpcodes.D2L: f.pushLong((long)f.popDouble()); break;
                    case InterpOpcodes.D2F: f.pushFloat((float)f.popDouble()); break;
                    case InterpOpcodes.I2B: f.pushInt((byte)f.popInt()); break;
                    case InterpOpcodes.I2C: f.pushInt((char)f.popInt()); break;
                    case InterpOpcodes.I2S: f.pushInt((short)f.popInt()); break;

                    case InterpOpcodes.LCMP: {
                        long b = f.popLong();
                        long a = f.popLong();
                        f.pushInt(a < b ? -1 : (a == b ? 0 : 1));
                        break;
                    }
                    case InterpOpcodes.FCMPL:
                    case InterpOpcodes.FCMPG: {
                        float b = f.popFloat();
                        float a = f.popFloat();
                        // NaN makes both operands unordered; the L and G forms
                        // differ only in which way they resolve it.
                        if (Float.isNaN(a) || Float.isNaN(b)) {
                            f.pushInt(op == InterpOpcodes.FCMPG ? 1 : -1);
                        } else {
                            f.pushInt(a < b ? -1 : (a == b ? 0 : 1));
                        }
                        break;
                    }
                    case InterpOpcodes.DCMPL:
                    case InterpOpcodes.DCMPG: {
                        double b = f.popDouble();
                        double a = f.popDouble();
                        if (Double.isNaN(a) || Double.isNaN(b)) {
                            f.pushInt(op == InterpOpcodes.DCMPG ? 1 : -1);
                        } else {
                            f.pushInt(a < b ? -1 : (a == b ? 0 : 1));
                        }
                        break;
                    }

                    case InterpOpcodes.IFEQ: if (f.popInt() == 0) next = code[pc + 1]; break;
                    case InterpOpcodes.IFNE: if (f.popInt() != 0) next = code[pc + 1]; break;
                    case InterpOpcodes.IFLT: if (f.popInt() < 0) next = code[pc + 1]; break;
                    case InterpOpcodes.IFGE: if (f.popInt() >= 0) next = code[pc + 1]; break;
                    case InterpOpcodes.IFGT: if (f.popInt() > 0) next = code[pc + 1]; break;
                    case InterpOpcodes.IFLE: if (f.popInt() <= 0) next = code[pc + 1]; break;
                    case InterpOpcodes.IF_ICMPEQ: { int b = f.popInt(); if (f.popInt() == b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IF_ICMPNE: { int b = f.popInt(); if (f.popInt() != b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IF_ICMPLT: { int b = f.popInt(); if (f.popInt() < b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IF_ICMPGE: { int b = f.popInt(); if (f.popInt() >= b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IF_ICMPGT: { int b = f.popInt(); if (f.popInt() > b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IF_ICMPLE: { int b = f.popInt(); if (f.popInt() <= b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IF_ACMPEQ: { Object b = f.popRef(); if (f.popRef() == b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IF_ACMPNE: { Object b = f.popRef(); if (f.popRef() != b) next = code[pc + 1]; break; }
                    case InterpOpcodes.IFNULL: if (f.popRef() == null) next = code[pc + 1]; break;
                    case InterpOpcodes.IFNONNULL: if (f.popRef() != null) next = code[pc + 1]; break;
                    case InterpOpcodes.GOTO: next = code[pc + 1]; break;

                    case InterpOpcodes.OP_TABLESWITCH: {
                        int min = code[pc + 2];
                        int max = code[pc + 3];
                        int dflt = code[pc + 4];
                        int key = f.popInt();
                        next = (key < min || key > max) ? dflt : code[pc + 5 + (key - min)];
                        break;
                    }
                    case InterpOpcodes.OP_LOOKUPSWITCH: {
                        int dflt = code[pc + 2];
                        int count = code[pc + 3];
                        int key = f.popInt();
                        next = dflt;
                        for (int i = 0; i < count; i++) {
                            if (code[pc + 4 + i * 2] == key) {
                                next = code[pc + 5 + i * 2];
                                break;
                            }
                        }
                        break;
                    }

                    case InterpOpcodes.IRETURN:
                        // Boxed by the declared return type, not as an int.
                        // `ireturn` is what boolean, byte, char, short and int
                        // all compile to, so the stack cannot say which it is;
                        // the caller unboxes by the descriptor, and an Integer
                        // where it expects a Boolean is a ClassCastException in
                        // the middle of an ordinary predicate.
                        return InterpValues.box(f.method.returnKind, f.popInt(), null);
                    case InterpOpcodes.LRETURN: return Long.valueOf(f.popLong());
                    case InterpOpcodes.FRETURN: return Float.valueOf(f.popFloat());
                    case InterpOpcodes.DRETURN: return Double.valueOf(f.popDouble());
                    case InterpOpcodes.ARETURN: return f.popRef();
                    case InterpOpcodes.RETURN: return null;

                    case InterpOpcodes.GETSTATIC: getStatic(f, code[pc + 1]); break;
                    case InterpOpcodes.PUTSTATIC: putStatic(f, code[pc + 1]); break;
                    case InterpOpcodes.GETFIELD: getField(f, code[pc + 1]); break;
                    case InterpOpcodes.PUTFIELD: putField(f, code[pc + 1]); break;

                    case InterpOpcodes.INVOKEVIRTUAL:
                    case InterpOpcodes.INVOKEINTERFACE:
                    case InterpOpcodes.INVOKESPECIAL:
                    case InterpOpcodes.INVOKESTATIC:
                        invokeSite(f, op, code[pc + 1]);
                        break;

                    case InterpOpcodes.NEW: {
                        int ext = code[pc + 1];
                        String name = externOwnerName(ext);
                        InterpClass ic = bundle.findClass(name);
                        if (ic != null) {
                            ensureInitialized(ic);
                            InterpObject created = new InterpObject(ic);
                            created.runtime = this;
                            f.pushRef(created);
                        } else {
                            // Uninitialised host object: the following
                            // invokespecial <init> is what actually constructs
                            // it, so record the intent and let that site do it.
                            f.pushRef(new PendingHostNew(ext));
                        }
                        break;
                    }
                    case InterpOpcodes.NEWARRAY: {
                        int count = f.popInt();
                        checkNegativeSize(count);
                        f.pushRef(newPrimitiveArray(code[pc + 1], count));
                        break;
                    }
                    case InterpOpcodes.ANEWARRAY: {
                        int count = f.popInt();
                        checkNegativeSize(count);
                        String comp = externOwnerName(code[pc + 1]);
                        // An array of an interpreted type is an Object[]: the
                        // element type only exists in the interpreter. This has
                        // to look through the brackets -- `new Entry[1][]` names
                        // the component `[LEntry;`, and asking the host to load
                        // Entry is asking for a class only the bundle has.
                        f.pushRef(isInterpretedLeaf(comp)
                                ? new Object[count]
                                : linker.newArray(comp.startsWith("[") ? comp : "L" + comp + ";", count));
                        break;
                    }
                    case InterpOpcodes.MULTIANEWARRAY: {
                        int dims = code[pc + 2];
                        int[] sizes = new int[dims];
                        for (int i = dims - 1; i >= 0; i--) {
                            sizes[i] = f.popInt();
                            checkNegativeSize(sizes[i]);
                        }
                        String arrayType = externOwnerName(code[pc + 1]);
                        f.pushRef(isInterpretedLeaf(arrayType)
                                ? nestedObjectArray(sizes, 0)
                                : linker.newMultiArray(arrayType, sizes));
                        break;
                    }
                    case InterpOpcodes.ARRAYLENGTH: {
                        Object a = f.popRef();
                        if (a == null) {
                            throw new InterpThrowable(new NullPointerException("array is null"),
                                    snapshotStack());
                        }
                        f.pushInt(arrayLength(a));
                        break;
                    }

                    case InterpOpcodes.ATHROW: {
                        Object t = f.popRef();
                        throw toThrowable(t);
                    }
                    case InterpOpcodes.CHECKCAST: {
                        Object v = f.stackRefs[f.sp - 1];
                        if (v != null && !isInstanceOf(v, code[pc + 1])) {
                            throw new InterpThrowable(new ClassCastException(
                                    "cannot cast to " + externOwnerName(code[pc + 1])),
                                    snapshotStack());
                        }
                        break;
                    }
                    case InterpOpcodes.INSTANCEOF: {
                        Object v = f.popRef();
                        f.pushInt(v != null && isInstanceOf(v, code[pc + 1]) ? 1 : 0);
                        break;
                    }
                    case InterpOpcodes.MONITORENTER: {
                        // The real monitor of the real object. Interpreted
                        // frames run on real threads and everything they can
                        // lock is a real object, so `synchronized` means what
                        // it says -- including against host code locking the
                        // same object.
                        //
                        // Java has no explicit monitor-enter, only a block, so
                        // the guarded region is run nested inside one. The
                        // matching `monitorexit` returns MONITOR_RELEASED and
                        // execution carries on here, outside the block, which
                        // is what drops the lock.
                        Object lock = f.popRef();
                        if (lock == null) {
                            throw new InterpThrowable(
                                    new NullPointerException("monitorenter on null"),
                                    snapshotStack());
                        }
                        Object nested;
                        synchronized (lock) {
                            nested = run(f, insn + 1, true);
                        }
                        if (nested != MONITOR_RELEASED) {
                            return nested;   // the region returned from the method
                        }
                        insn = f.resumeInsn;
                        continue;
                    }
                    case InterpOpcodes.MONITOREXIT: {
                        f.popRef();
                        if (insideMonitor) {
                            f.resumeInsn = insn + 1;
                            return MONITOR_RELEASED;
                        }
                        // Unbalanced. javac's synthetic handler releases the
                        // monitor again on the exception path, by which time
                        // the enclosing block already has, so the second one is
                        // a no-op rather than an error.
                        break;
                    }

                    default:
                        throw new InterpThrowable(new UnsupportedOperationException(
                                "opcode " + op + " in " + m), snapshotStack());
                }
            } catch (InterpThrowable it) {
                int handler = findHandler(m, insn, it.getThrown());
                if (handler < 0) {
                    throw it;
                }
                f.sp = 0;
                f.pushRef(it.getThrown());
                insn = handler;
                continue;
            } catch (Throwable hostThrown) {
                // Something the host raised while we were inside it. It is a
                // real Java throwable, and interpreted `catch` clauses have to
                // be able to see it.
                int handler = findHandler(m, insn, hostThrown);
                if (handler < 0) {
                    throw hostThrown;
                }
                f.sp = 0;
                f.pushRef(hostThrown);
                insn = handler;
                continue;
            }

            if (next <= insn) {
                // Back edge: the only place a loop can spin, so the only place
                // that needs a fuel check.
                ThreadState st = state();
                if (--st.fuel <= 0) {
                    checkpoint(st);
                }
            }
            insn = next;
        }
    }

    // ------------------------------------------------------------ checkpoint

    private void checkpoint(ThreadState st) throws InterpThrowable {
        st.fuel = fuelPerCheck;
        if (cancelRequested) {
            throw new InterpThrowable(new InterpCancelled("stopped by request"),
                    snapshotStack());
        }
        if (edtBudgetMs > 0 && st.hostCallDepth == 0 && st.runStartMs > 0) {
            long elapsed = System.currentTimeMillis() - st.runStartMs;
            if (elapsed > edtBudgetMs) {
                throw new InterpThrowable(new InterpCancelled(
                        "pushed program ran for " + elapsed + "ms without yielding"),
                        snapshotStack());
            }
        }
    }

    // ------------------------------------------------------------- constants

    private void ldc(InterpFrame f, int tag, int operand) throws Throwable {
        switch (tag) {
            case InterpOpcodes.LDC_INT:
                f.pushInt(operand);
                break;
            case InterpOpcodes.LDC_LONG:
                f.pushLong(Long.parseLong(bundle.string(operand)));
                break;
            case InterpOpcodes.LDC_FLOAT:
                f.pushFloat(Float.intBitsToFloat(operand));
                break;
            case InterpOpcodes.LDC_DOUBLE:
                f.pushDouble(Double.parseDouble(bundle.string(operand)));
                break;
            case InterpOpcodes.LDC_STRING:
                f.pushRef(bundle.string(operand));
                break;
            case InterpOpcodes.LDC_CLASS: {
                // `Color.class` where Color is in this bundle names something
                // the host has never heard of, so the class object is the
                // InterpClass itself. Every consumer that can receive one --
                // Enum.valueOf is the one javac generates -- checks for it.
                InterpClass local = bundle.findClass(externOwnerName(operand));
                f.pushRef(local != null
                        ? (Object)local
                        : linker.classObject(resolveExternClass(operand)));
                break;
            }
            default:
                throw new IllegalStateException("bad ldc tag " + tag);
        }
    }

    // ----------------------------------------------------------------- state

    /// Runs a class's initializer once, following JLS 12.4.2.
    ///
    /// The subtlety is that "has it been initialized" is four states, not two.
    /// Marking the class done before running `<clinit>` is what stops a cycle
    /// -- a static initializer that reaches back into its own class -- from
    /// recursing forever, but the same mark tells *another* thread that the
    /// static fields are ready when they are not. The JVM separates the two
    /// with a per-class lock and an owning thread: the initializing thread
    /// passes straight through, everyone else waits.
    ///
    /// A failed initializer is the other half. Once `<clinit>` throws, the
    /// class is erroneous forever; leaving the "done" mark set would hand every
    /// later reader a class whose statics were half-assigned, and the failure
    /// would surface as a wrong value rather than as an error.
    private void ensureInitialized(InterpClass c) throws Throwable {
        synchronized (c) {
            if (c.initState == InterpClass.INIT_DONE) {
                return;
            }
            if (c.initState == InterpClass.INIT_FAILED) {
                throw new NoClassDefFoundError("could not initialize "
                        + c.getName().replace('/', '.'));
            }
            if (c.initState == InterpClass.INIT_RUNNING) {
                if (c.initThread == Thread.currentThread()) {
                    // Recursive entry from the initializer itself: legal, and
                    // the one case where a partly-built class must be visible.
                    return;
                }
                while (c.initState == InterpClass.INIT_RUNNING) {
                    c.wait();
                }
                // Re-read the state the other thread left behind.
                if (c.initState == InterpClass.INIT_FAILED) {
                    throw new NoClassDefFoundError("could not initialize "
                            + c.getName().replace('/', '.'));
                }
                return;
            }
            c.initState = InterpClass.INIT_RUNNING;
            c.initThread = Thread.currentThread();
        }
        boolean ok = false;
        try {
            if (c.superInterp != null) {
                ensureInitialized(c.superInterp);
            }
            // JLS 12.4.1: initializing a class initializes the superinterfaces
            // that declare a default method, and only those. An interface
            // without one is initialized when a field of it is read, not when
            // an implementor is -- so initializing them all would run
            // initializers Java never runs, which is as wrong as running them
            // late.
            for (int i = 0; i < c.interpInterfaces.length; i++) {
                InterpClass iface = c.interpInterfaces[i];
                if (iface != null && declaresDefaultMethod(iface)) {
                    ensureInitialized(iface);
                }
            }
            InterpMethod clinit = c.declaredMethod("<clinit>", "()V");
            if (clinit != null) {
                invokeInterpreted(clinit, null, null);
            }
            ok = true;
        } finally {
            synchronized (c) {
                c.initState = ok ? InterpClass.INIT_DONE : InterpClass.INIT_FAILED;
                c.initThread = null;
                c.notifyAll();
            }
        }
    }

    /// Whether a type descriptor bottoms out in a class this bundle carries.
    ///
    /// The brackets have to be looked through, and so does the `L...;` wrapper,
    /// because the same leaf reaches here spelled three ways: `Entry` from a
    /// one-dimensional `anewarray`, `[LEntry;` from `new Entry[1][]`, and
    /// `[[LEntry;` from a `multianewarray`. Only the leaf says whether the host
    /// has ever heard of the type.
    private boolean isInterpretedLeaf(String descriptor) {
        String at = descriptor;
        while (at.length() > 0 && at.charAt(0) == '[') {
            at = at.substring(1);
        }
        if (at.length() > 2 && at.charAt(0) == 'L' && at.endsWith(";")) {
            at = at.substring(1, at.length() - 1);
        }
        return bundle.findClass(at) != null;
    }

    /// The interpreter's representation of a multi-dimensional array of an
    /// interpreted type: nested `Object[]`, allocated for every dimension the
    /// bytecode gave a size for.
    private static Object[] nestedObjectArray(int[] sizes, int depth) {
        Object[] out = new Object[sizes[depth]];
        if (depth + 1 < sizes.length) {
            for (int i = 0; i < out.length; i++) {
                out[i] = nestedObjectArray(sizes, depth + 1);
            }
        }
        return out;
    }

    /// Whether an interpreted interface declares a default method, directly or
    /// through a superinterface -- the condition JLS 12.4.1 attaches to
    /// initializing an interface on behalf of an implementor.
    private boolean declaresDefaultMethod(InterpClass iface) {
        for (int i = 0; i < iface.methods.length; i++) {
            InterpMethod m = iface.methods[i];
            if (!m.isStatic() && !m.isAbstract() && !"<clinit>".equals(m.name)) {
                return true;
            }
        }
        for (int i = 0; i < iface.interpInterfaces.length; i++) {
            InterpClass parent = iface.interpInterfaces[i];
            if (parent != null && declaresDefaultMethod(parent)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- fields

    private void getStatic(InterpFrame f, int ext) throws Throwable {
        String owner = externOwnerName(ext);
        String name = bundle.string(bundle.externName[ext]);
        String desc = bundle.string(bundle.externDesc[ext]);
        InterpClass ic = bundle.findClass(owner);
        if (ic != null) {
            ensureInitialized(ic);
            pushBoxed(f, InterpValues.kindOf(desc), findStaticHolder(ic, name).staticValue(name));
            return;
        }
        pushBoxed(f, InterpValues.kindOf(desc), linker.getStatic(owner, name, desc));
    }

    private void putStatic(InterpFrame f, int ext) throws Throwable {
        String owner = externOwnerName(ext);
        String name = bundle.string(bundle.externName[ext]);
        String desc = bundle.string(bundle.externDesc[ext]);
        int kind = InterpValues.kindOf(desc);
        Object value = popBoxed(f, kind);
        InterpClass ic = bundle.findClass(owner);
        if (ic != null) {
            ensureInitialized(ic);
            findStaticHolder(ic, name).setStaticValue(name, value);
            return;
        }
        linker.setStatic(owner, name, desc, value);
    }

    /// The slot of a field, resolved from the owner named in the field
    /// reference rather than from the object's runtime type.
    ///
    /// Field access is not virtual. When `Base` declares `v` and `Mid extends
    /// Base` shadows it with its own `v`, code compiled inside `Base` reads
    /// `Base.v` even for a `Mid` instance -- javac records which one it meant.
    /// Resolving from the runtime type instead would silently read the
    /// subclass's field, which is the sort of difference that produces a wrong
    /// number rather than an error.
    private int fieldIndex(InterpObject io, String owner, String name) {
        InterpClass declaring = bundle.findClass(owner);
        if (declaring == null) {
            return -1;
        }
        return io.indexOf(declaring, name);
    }

    private static InterpClass findStaticHolder(InterpClass c, String name) {
        InterpClass k = c;
        while (k != null) {
            if (k.declaresStatic(name)) {
                return k;
            }
            k = k.superInterp;
        }
        return c;
    }

    private void getField(InterpFrame f, int ext) throws Throwable {
        String owner = externOwnerName(ext);
        String name = bundle.string(bundle.externName[ext]);
        String desc = bundle.string(bundle.externDesc[ext]);
        Object target = f.popRef();
        if (target == null) {
            throw new InterpThrowable(new NullPointerException(owner + "." + name), snapshotStack());
        }
        if (target instanceof InterpObject) {
            InterpObject io = (InterpObject)target;
            int idx = fieldIndex(io, owner, name);
            if (idx >= 0) {
                pushBoxed(f, InterpValues.kindOf(desc), io.fields[idx]);
                return;
            }
            // Declared by a host superclass, so it lives on the peer.
            pushBoxed(f, InterpValues.kindOf(desc),
                    linker.getField(io.hostPeer, owner, name, desc));
            return;
        }
        pushBoxed(f, InterpValues.kindOf(desc), linker.getField(target, owner, name, desc));
    }

    private void putField(InterpFrame f, int ext) throws Throwable {
        String owner = externOwnerName(ext);
        String name = bundle.string(bundle.externName[ext]);
        String desc = bundle.string(bundle.externDesc[ext]);
        int kind = InterpValues.kindOf(desc);
        Object value = popBoxed(f, kind);
        Object target = f.popRef();
        if (target == null) {
            throw new InterpThrowable(new NullPointerException(owner + "." + name), snapshotStack());
        }
        if (target instanceof InterpObject) {
            InterpObject io = (InterpObject)target;
            int idx = fieldIndex(io, owner, name);
            if (idx >= 0) {
                io.fields[idx] = value;
                return;
            }
            linker.setField(io.hostPeer, owner, name, desc, value);
            return;
        }
        linker.setField(target, owner, name, desc, value);
    }

    // ----------------------------------------------------------------- calls

    private void invokeSite(InterpFrame f, int op, int ext) throws Throwable {
        String owner = externOwnerName(ext);
        String name = bundle.string(bundle.externName[ext]);
        String desc = bundle.string(bundle.externDesc[ext]);

        int[] argKinds = InterpValues.argumentKinds(desc);
        Object[] args = new Object[argKinds.length];
        for (int i = argKinds.length - 1; i >= 0; i--) {
            args[i] = popBoxed(f, argKinds[i]);
        }

        int returnKind = InterpValues.returnKind(desc);

        if (op == InterpOpcodes.INVOKESTATIC) {
            InterpClass ic = bundle.findClass(owner);
            if (ic != null) {
                ensureInitialized(ic);
                InterpMethod m = ic.declaredMethod(name, desc);
                if (m == null) {
                    m = ic.resolve(name, desc);
                }
                if (m != null) {
                    pushBoxed(f, returnKind, invokeInterpreted(m, null, args));
                    return;
                }
            }
            // javac compiles an enum's own valueOf(String) into a call to
            // Enum.valueOf(Class,String), which reflects over the class's
            // constants. There is no reflection here and the class is
            // interpreted, so the lookup runs against the bundle instead.
            if ("java/lang/Enum".equals(owner) && "valueOf".equals(name)
                    && args.length == 2 && args[0] instanceof InterpClass) {
                pushBoxed(f, returnKind, enumValueOf((InterpClass)args[0], (String)args[1]));
                return;
            }
            // A native interface declared by the pushed code itself. Its native
            // half was never compiled into this app and never could be -- that
            // is the one thing a device runtime cannot accept from the wire --
            // so it gets the answer the API is designed around: a stub that
            // reports isSupported() false. The library's Java half runs
            // normally, which is what makes an app that uses a cn1lib still
            // compile and still run here.
            if ("com/codename1/system/NativeLookup".equals(owner) && "create".equals(name)
                    && args.length == 1 && args[0] instanceof InterpClass) {
                pushBoxed(f, returnKind, new NativeStub((InterpClass)args[0]));
                return;
            }
            pushBoxed(f, returnKind, hostCall(owner, name, desc, null, args, false));
            return;
        }

        Object target = f.popRef();
        if (target == null) {
            throw new InterpThrowable(new NullPointerException(owner + "." + name), snapshotStack());
        }

        // `new X(...)` on a host class: NEW pushed a placeholder and this is
        // the invokespecial that turns it into a real object. The placeholder
        // may have been duplicated by DUP, so every copy has to be replaced.
        if (target instanceof PendingHostNew) {
            PendingHostNew pending = (PendingHostNew)target;
            Object created = linker.construct(resolveExternClass(pending.externIndex), desc, args);
            replaceOnStack(f, pending, created);
            return;
        }

        // `$VALUES.clone()`, which is how javac writes an enum's values(). An
        // array's clone is not a method any linker can look up, so it is done
        // here -- for arrays of every kind, not only for enums.
        if ("clone".equals(name) && args.length == 0 && isArray(target)) {
            pushBoxed(f, returnKind, copyArray(target));
            return;
        }

        // The unimplemented half of a cn1lib. isSupported() is the question the
        // API tells callers to ask, and it answers false; everything else
        // answers the way an uninitialised field would, so a caller that
        // ignores isSupported() gets zero or null rather than a crash.
        if (target instanceof NativeStub) {
            if ("isSupported".equals(name)) {
                pushBoxed(f, returnKind, Boolean.FALSE);
            } else {
                pushBoxed(f, returnKind, InterpValues.defaultForKind(returnKind));
            }
            return;
        }

        // A class literal or getClass() for a type only the bundle has: the
        // token on the stack is the InterpClass itself, because there is no host
        // class object to hand back. The bytecode still calls java.lang.Class
        // methods on it, and the linkers cannot -- a reflective one rejects the
        // receiver and a native one has no clazz pointer for it -- so the small
        // part of Class that means anything here is answered here.
        if (target instanceof InterpClass) {
            Object r = classCall((InterpClass)target, name, args);
            if (r != NOT_CLASS_METHOD) {
                pushBoxed(f, returnKind, r);
                return;
            }
            throw new InterpThrowable(new UnsupportedOperationException(
                    "Class." + name + desc + " is not available for "
                    + ((InterpClass)target).getName().replace('/', '.')
                    + ", which exists only in this bundle"), snapshotStack());
        }

        if (target instanceof InterpObject) {
            InterpObject io = (InterpObject)target;

            // `super(...)` reaching a host class is the moment the peer can be
            // built: it is the first point at which the superclass constructor
            // arguments are known, and it happens before any host code can
            // observe the object.
            if (op == InterpOpcodes.INVOKESPECIAL && "<init>".equals(name)
                    && bundle.findClass(owner) == null) {
                createPeer(io, owner, desc, args);
                return;
            }

            InterpMethod m;
            if (op == InterpOpcodes.INVOKESPECIAL) {
                // A super call, a private method or a constructor: resolve
                // against the named owner, not the receiver's class, or an
                // override would make `super.foo()` recurse forever.
                InterpClass declaring = bundle.findClass(owner);
                m = declaring == null ? null : declaring.declaredMethod(name, desc);
                if (m == null && declaring != null) {
                    m = declaring.resolve(name, desc);
                }
            } else {
                m = io.type.resolve(name, desc);
            }
            if (m != null && !m.isAbstract()) {
                pushBoxed(f, returnKind, invokeInterpreted(m, io, args));
                return;
            }
            // Nothing interpreted implements it: it must be inherited from the
            // host superclass, and the peer is what can run it.
            //
            // Resolution has to start at the peer's own class, not at `owner`.
            // The call site names the interpreted class -- `new MyForm().show()`
            // records MyForm -- and the host has never heard of that name. The
            // peer is a generated subclass of the real framework class, so
            // walking up from it finds the method exactly where it lives.
            if (io.hostPeer != null) {
                // The peer's own class name, recorded when the factory built it
                // rather than read back from getClass(). ParparVM derives
                // Class.getName() from the mangled C symbol, so a class whose
                // simple name contains an underscore -- which every generated
                // shim's does, Interp_Form -- comes back as "Interp/Form" and
                // resolves against nothing.
                String peerOwner = io.hostPeerOwner;
                if (op == InterpOpcodes.INVOKESPECIAL) {
                    // `super.paint(g)` in interpreted code. Calling `paint` on
                    // the peer would land on the shim's override, which asks the
                    // interpreter for paint again -- unbounded recursion, once
                    // per frame, on the event thread. The shim exists precisely
                    // to provide `super_paint` as the way out; that bridge is
                    // the only thing that can reach the framework implementation
                    // from here.
                    pushBoxed(f, returnKind,
                            hostCall(peerOwner, "super_" + name, desc, io.hostPeer, args, false));
                    return;
                }
                pushBoxed(f, returnKind,
                        hostCall(peerOwner, name, desc, io.hostPeer, args, false));
                return;
            }
            // An enum constant that did not override the method: what is left
            // is java.lang.Enum's own behaviour, which is small enough to
            // answer here and has no peer to answer it.
            if (io.enumOrdinal >= 0) {
                Object r = enumCall(io, name, desc, args);
                if (r != NOT_ENUM_METHOD) {
                    pushBoxed(f, returnKind, r);
                    return;
                }
            }
            // java.lang.Object's own methods, for an object with no peer to
            // inherit them from. getClass() is not an exotic case: javac emits
            // a `receiver.getClass()` null check in front of every bound
            // method reference, so `self::method` needs it.
            Object r = objectCall(io, name, args);
            if (r != NOT_OBJECT_METHOD) {
                pushBoxed(f, returnKind, r);
                return;
            }
            // IncompatibleClassChangeError rather than the AbstractMethodError
            // this really is: CLDC11's AbstractMethodError keeps its
            // constructors package-private, so the framework cannot throw one
            // with a message, and a message naming the method is worth more
            // here than the exactly right type.
            throw new InterpThrowable(new IncompatibleClassChangeError(
                    owner + "." + name + desc + " is not implemented"), snapshotStack());
        }

        pushBoxed(f, returnKind,
                hostCall(owner, name, desc, target, args, op == InterpOpcodes.INVOKESPECIAL));
    }

    /// Builds the host-visible peer for an interpreted object, at the point its
    /// constructor chains into the host superclass.
    ///
    /// `java.lang.Object` is not treated as a host superclass: every class has
    /// it as an ancestor and no dispatch depends on it, so a class whose only
    /// host supertype is Object needs no peer at all unless it also implements
    /// host interfaces.
    private void createPeer(InterpObject io, String superOwner, String superDesc, Object[] superArgs)
            throws Throwable {
        Vector hostSupertypes = new Vector();
        io.type.collectHostSupertypes(hostSupertypes);

        // An enum constant is not given a peer. `java.lang.Enum` has no shim
        // and can have none -- Java forbids naming it as a superclass, so there
        // is no source a generator could emit -- and it needs none: `Enum` is
        // a name and an ordinal plus the methods that read them, all of which
        // the interpreter answers itself. The constant is still free to
        // implement host interfaces, and those are collected below as usual.
        if ("java/lang/Enum".equals(superOwner)) {
            io.enumName = superArgs.length > 0 ? (String)superArgs[0] : null;
            io.enumOrdinal = superArgs.length > 1 && superArgs[1] instanceof Number
                    ? ((Number)superArgs[1]).intValue() : -1;
            superOwner = "java/lang/Object";
        }

        String hostSuperclassName = null;
        Vector interfaces = new Vector();
        if (!"java/lang/Object".equals(superOwner)) {
            if (linker.findClass(superOwner) == null) {
                throw new InterpThrowable(new NoClassDefFoundError(
                        superOwner + " is not present in the installed app"), snapshotStack());
            }
            hostSuperclassName = superOwner;
        }
        for (int i = 0; i < hostSupertypes.size(); i++) {
            int ext = ((Integer)hostSupertypes.elementAt(i)).intValue();
            String n = externOwnerName(ext);
            // Enum is skipped for the same reason it is skipped as a
            // superclass: it has no shim and needs none. It reaches here as
            // well as there because the walk collects the whole supertype set,
            // not just the immediate one.
            if (n.equals(superOwner) || "java/lang/Object".equals(n)
                    || "java/lang/Enum".equals(n)) {
                continue;
            }
            if (linker.findClass(n) != null) {
                interfaces.addElement(n);
            }
        }

        if (hostSuperclassName == null && interfaces.isEmpty()) {
            return;   // nothing in the host needs to see this object
        }
        if (!factory.canExtend(hostSuperclassName)) {
            throw new InterpThrowable(new UnsupportedOperationException(
                    io.type.getName().replace('/', '.') + " extends " + superOwner.replace('/', '.')
                    + ", which this platform's object factory cannot subclass"), snapshotStack());
        }
        String[] ifaceArray = new String[interfaces.size()];
        interfaces.copyInto(ifaceArray);
        io.hostPeer = factory.createPeer(io, hostSuperclassName, ifaceArray, superDesc, superArgs);
        io.hostPeerOwner = factory.peerClassName(io.hostPeer);
    }

    private Object hostCall(String owner, String name, String desc, Object target,
                            Object[] args, boolean special) throws Throwable {
        // Fuel accounting stops for the duration: a host call may legitimately
        // block for a long time (invokeAndBlock waiting on the network) and
        // that must not read as a runaway loop.
        ThreadState st = state();
        st.hostCallDepth++;
        try {
            if (target == null) {
                return linker.invokeStatic(owner, name, desc, args);
            }
            if (special) {
                return linker.invokeSpecial(target, owner, name, desc, args);
            }
            return linker.invokeVirtual(target, owner, name, desc, args);
        } catch (Throwable t) {
            // Record where interpreted code was when the framework threw.
            // Without this a failure inside a library method arrives with the
            // interpreter's own stack and no message -- "java.lang.
            // UnsupportedOperationException" and nothing else -- which says
            // neither what was called nor from where.
            if (lastThrown != t) {
                lastThrownStack = snapshotStack();
                lastHostCall = owner.replace('/', '.') + "." + name + desc;
                lastThrown = t;
            }
            throw t;
        } finally {
            st.hostCallDepth--;
        }
    }

    private void replaceOnStack(InterpFrame f, Object placeholder, Object created) {
        for (int i = 0; i < f.sp; i++) {
            if (f.stackRefs[i] == placeholder) {
                f.stackRefs[i] = created;
            }
        }
        for (int i = 0; i < f.refs.length; i++) {
            if (f.refs[i] == placeholder) {
                f.refs[i] = created;
            }
        }
    }

    // ----------------------------------------------------------------- types

    /// Resolves an extern class reference, caching the answer in the bundle.
    ///
    /// Synchronized, because the two arrays are one logical entry and the
    /// interpreter runs on every thread a pushed program touches. Publishing
    /// them in the wrong order produced a race that is worth describing, since
    /// the symptom pointed nowhere near the cause: a thread that set
    /// `externResolveAttempted` before storing `externResolved` left a window
    /// where another thread saw "attempted, and null" and reported
    /// `NoClassDefFoundError: java/lang/StringBuilder` -- for a class that had
    /// resolved perfectly well microseconds earlier, on a program that had done
    /// nothing wrong.
    ///
    /// The lock is on the whole lookup rather than only the store: without it,
    /// a non-null read of `externResolved[ext]` on one thread carries no
    /// guarantee that the write is visible, so a fast path outside the monitor
    /// would only make the window smaller rather than closing it. An
    /// uncontended monitor is cheap next to interpreted dispatch.
    /// Marker for "this is not one of Enum's methods", so that a null return
    /// from one that is stays distinguishable.
    private static final Object NOT_ENUM_METHOD = new Object();

    /// The same, for java.lang.Object's methods.
    private static final Object NOT_OBJECT_METHOD = new Object();

    /// java.lang.Object's behaviour for an interpreted object with no peer.
    ///
    /// Only reached when nothing interpreted implements the method and there is
    /// no host object to inherit it from.
    /// Sentinel for "java.lang.Class does not answer this here".
    private static final Object NOT_CLASS_METHOD = new Object();

    /// java.lang.Class, for a type that exists only in the bundle.
    ///
    /// The set is what an application can reasonably ask of a class literal
    /// without reflection, which the runtime does not have and the device could
    /// not provide: naming, identity, and the two type tests. Anything beyond
    /// that is refused by name rather than answered wrongly.
    private Object classCall(InterpClass c, String name, Object[] args) throws Throwable {
        if ("getName".equals(name) && args.length == 0) {
            return c.getName().replace('/', '.');
        }
        if ("getSimpleName".equals(name) && args.length == 0) {
            String n = c.getName();
            int slash = n.lastIndexOf('/');
            String simple = slash < 0 ? n : n.substring(slash + 1);
            int dollar = simple.lastIndexOf('$');
            return dollar < 0 ? simple : simple.substring(dollar + 1);
        }
        if ("toString".equals(name) && args.length == 0) {
            return (c.isInterface() ? "interface " : "class ")
                    + c.getName().replace('/', '.');
        }
        if ("hashCode".equals(name) && args.length == 0) {
            return Integer.valueOf(System.identityHashCode(c));
        }
        if ("equals".equals(name) && args.length == 1) {
            return args[0] == c ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("isInterface".equals(name) && args.length == 0) {
            return c.isInterface() ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("isInstance".equals(name) && args.length == 1) {
            return isInstanceOf(args[0], c.getName()) ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("getSuperclass".equals(name) && args.length == 0) {
            return c.superInterp;
        }
        return NOT_CLASS_METHOD;
    }

    private Object objectCall(InterpObject io, String name, Object[] args) {
        if ("getClass".equals(name) && args.length == 0) {
            // The interpreted class is its own class object: there is no host
            // class to hand back, and the bundle is what knows the type.
            return io.type;
        }
        if ("hashCode".equals(name) && args.length == 0) {
            return Integer.valueOf(System.identityHashCode(io));
        }
        if ("equals".equals(name) && args.length == 1) {
            return args[0] == io ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("toString".equals(name) && args.length == 0) {
            return io.toString();
        }
        return NOT_OBJECT_METHOD;
    }

    /// Stands in for a native interface whose native half is not in this app.
    ///
    /// Native code is the one thing that can never be pushed, so a cn1lib used
    /// by pushed code always arrives half-present: its Java half is interpreted
    /// like any other pushed class, and this is what its native half becomes.
    static final class NativeStub {
        final InterpClass iface;

        NativeStub(InterpClass iface) {
            this.iface = iface;
        }

        public String toString() {
            return iface.getName().replace('/', '.') + "(unsupported on this runtime)";
        }
    }

    /// java.lang.Enum's behaviour for an interpreted enum constant.
    ///
    /// Only the methods a constant inherits without overriding reach here;
    /// anything the enum declares itself was already resolved and run.
    private Object enumCall(InterpObject io, String name, String desc, Object[] args) {
        if ("name".equals(name) || "toString".equals(name)) {
            return io.enumName;
        }
        if ("ordinal".equals(name)) {
            return Integer.valueOf(io.enumOrdinal);
        }
        if ("equals".equals(name) && args.length == 1) {
            // Enum identity is object identity: constants are singletons.
            return args[0] == io ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("hashCode".equals(name)) {
            return Integer.valueOf(System.identityHashCode(io));
        }
        if ("compareTo".equals(name) && args.length == 1 && args[0] instanceof InterpObject) {
            return Integer.valueOf(io.enumOrdinal - ((InterpObject)args[0]).enumOrdinal);
        }
        return NOT_ENUM_METHOD;
    }

    /// Enum.valueOf over a bundle class, by scanning the constants the
    /// class's static initializer already built.
    private Object enumValueOf(InterpClass c, String name) throws Throwable {
        ensureInitialized(c);
        Object values = c.staticValue("$VALUES");
        if (values instanceof Object[]) {
            Object[] a = (Object[])values;
            for (int i = 0; i < a.length; i++) {
                if (a[i] instanceof InterpObject
                        && name.equals(((InterpObject)a[i]).enumName)) {
                    return a[i];
                }
            }
        }
        throw new InterpThrowable(new IllegalArgumentException(
                "No enum constant " + c.getName().replace('/', '.') + "." + name),
                snapshotStack());
    }

    private static boolean isArray(Object o) {
        return o instanceof Object[] || o instanceof int[] || o instanceof byte[]
                || o instanceof char[] || o instanceof short[] || o instanceof long[]
                || o instanceof float[] || o instanceof double[] || o instanceof boolean[];
    }

    /// A shallow copy of an array, which is what Object.clone does for one.
    ///
    /// Written out per kind rather than through java.lang.reflect.Array:
    /// ParparVM has no reflection, and a reference to it would eliminate this
    /// whole method on iOS.
    private static Object copyArray(Object a) {
        if (a instanceof Object[]) {
            Object[] s = (Object[])a;
            Object[] d = new Object[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof int[]) {
            int[] s = (int[])a;
            int[] d = new int[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof byte[]) {
            byte[] s = (byte[])a;
            byte[] d = new byte[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof char[]) {
            char[] s = (char[])a;
            char[] d = new char[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof short[]) {
            short[] s = (short[])a;
            short[] d = new short[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof long[]) {
            long[] s = (long[])a;
            long[] d = new long[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof float[]) {
            float[] s = (float[])a;
            float[] d = new float[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof double[]) {
            double[] s = (double[])a;
            double[] d = new double[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        boolean[] s = (boolean[])a;
        boolean[] d = new boolean[s.length];
        System.arraycopy(s, 0, d, 0, s.length);
        return d;
    }

    private Object resolveExternClass(int ext) throws Throwable {
        Object c;
        synchronized (bundle) {
            if (bundle.externResolveAttempted[ext]) {
                c = bundle.externResolved[ext];
                if (c == null) {
                    throw new InterpThrowable(new NoClassDefFoundError(externOwnerName(ext)),
                            snapshotStack());
                }
                return c;
            }
            c = linker.findClass(externOwnerName(ext));
            // Result first, flag second: the flag is what makes the result
            // readable, so it must never become true before there is one.
            bundle.externResolved[ext] = c;
            bundle.externResolveAttempted[ext] = true;
        }
        if (c == null) {
            throw new InterpThrowable(new NoClassDefFoundError(externOwnerName(ext)
                    + " is not present in the installed app"), snapshotStack());
        }
        return c;
    }

    private boolean isInstanceOf(Object v, int ext) throws Throwable {
        return isInstanceOf(v, externOwnerName(ext));
    }

    /// The same test against a type named directly, which is what
    /// `Class.isInstance` has and an extern index is not.
    private boolean isInstanceOf(Object v, String name) throws Throwable {
        if (name.length() > 0 && name.charAt(0) == '[') {
            return isArrayInstanceOf(v, name);
        }
        if (v instanceof InterpBacked) {
            v = ((InterpBacked)v).getInterpObject();
        }
        if (v instanceof NativeStub) {
            // The cast the NativeLookup idiom always performs. The stub stands
            // in for the interface it was asked for, and for NativeInterface
            // above it.
            InterpClass iface = ((NativeStub)v).iface;
            return iface.isSubclassOfInterp(name)
                    || "com/codename1/system/NativeInterface".equals(name);
        }
        if (v instanceof InterpObject) {
            InterpObject io = (InterpObject)v;
            if (io.type.isSubclassOfInterp(name)) {
                return true;
            }
            Object hostClass = linker.findClass(name);
            return hostClass != null && io.hostPeer != null
                    && linker.isInstance(hostClass, io.hostPeer);
        }
        Object hostClass = linker.findClass(name);
        return hostClass != null && linker.isInstance(hostClass, v);
    }

    /// `instanceof` and `checkcast` against an array type.
    ///
    /// An array of an interpreted type has no host class to ask -- there is no
    /// `EnumProbe$Color` on the device -- and the interpreter represents every
    /// reference array as `Object[]` regardless of its component type. So the
    /// component is checked element by element instead, which is a real check
    /// rather than a wave-through, and an empty array satisfies any component
    /// type exactly as an empty `Color[]` would.
    ///
    /// The approximation is that an `Object[]` holding only Colors answers true
    /// for `Color[]`. That follows from the representation, and it errs in the
    /// direction the representation already commits to.
    private boolean isArrayInstanceOf(Object v, String name) throws Throwable {
        if (v == null) {
            return false;
        }
        String component = name.substring(1);
        if (component.length() == 1) {
            // A primitive array: the concrete Java type answers exactly.
            switch (component.charAt(0)) {
                case 'Z': return v instanceof boolean[];
                case 'B': return v instanceof byte[];
                case 'C': return v instanceof char[];
                case 'S': return v instanceof short[];
                case 'I': return v instanceof int[];
                case 'J': return v instanceof long[];
                case 'F': return v instanceof float[];
                case 'D': return v instanceof double[];
                default: return false;
            }
        }
        if (!(v instanceof Object[])) {
            return false;
        }
        if (component.charAt(0) == '[') {
            Object[] a = (Object[])v;
            for (int i = 0; i < a.length; i++) {
                if (a[i] != null && !isArrayInstanceOf(a[i], component)) {
                    return false;
                }
            }
            return true;
        }
        String element = component.charAt(0) == 'L' && component.endsWith(";")
                ? component.substring(1, component.length() - 1)
                : component;
        if ("java/lang/Object".equals(element)) {
            return true;
        }
        Object[] a = (Object[])v;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                continue;
            }
            if (!isElementOf(a[i], element)) {
                return false;
            }
        }
        return true;
    }

    private boolean isElementOf(Object v, String element) throws Throwable {
        if (v instanceof InterpObject) {
            InterpObject io = (InterpObject)v;
            if (io.type.isSubclassOfInterp(element)) {
                return true;
            }
            Object hostClass = linker.findClass(element);
            return hostClass != null && io.hostPeer != null
                    && linker.isInstance(hostClass, io.hostPeer);
        }
        Object hostClass = linker.findClass(element);
        return hostClass != null && linker.isInstance(hostClass, v);
    }

    private String externOwnerName(int ext) {
        return bundle.string(bundle.externOwner[ext]);
    }

    // ---------------------------------------------------------------- arrays

    private static void checkNegativeSize(int n) throws InterpThrowable {
        if (n < 0) {
            throw new InterpThrowable(new NegativeArraySizeException(String.valueOf(n)), null);
        }
    }

    private static Object newPrimitiveArray(int atype, int count) {
        switch (atype) {
            case 4:  return new boolean[count];
            case 5:  return new char[count];
            case 6:  return new float[count];
            case 7:  return new double[count];
            case 8:  return new byte[count];
            case 9:  return new short[count];
            case 10: return new int[count];
            case 11: return new long[count];
            default: throw new IllegalStateException("bad newarray type " + atype);
        }
    }

    private void arrayLoad(InterpFrame f, int op) throws Throwable {
        int index = f.popInt();
        Object array = f.popRef();
        checkArray(array, index);
        switch (op) {
            case InterpOpcodes.IALOAD: f.pushInt(((int[])array)[index]); break;
            case InterpOpcodes.LALOAD: f.pushLong(((long[])array)[index]); break;
            case InterpOpcodes.FALOAD: f.pushFloat(((float[])array)[index]); break;
            case InterpOpcodes.DALOAD: f.pushDouble(((double[])array)[index]); break;
            case InterpOpcodes.BALOAD:
                // byte[] and boolean[] share the opcode; they are distinct
                // array types and only the runtime type says which.
                if (array instanceof boolean[]) {
                    f.pushInt(((boolean[])array)[index] ? 1 : 0);
                } else {
                    f.pushInt(((byte[])array)[index]);
                }
                break;
            case InterpOpcodes.CALOAD: f.pushInt(((char[])array)[index]); break;
            case InterpOpcodes.SALOAD: f.pushInt(((short[])array)[index]); break;
            default: f.pushRef(((Object[])array)[index]); break;
        }
    }

    private void arrayStore(InterpFrame f, int op) throws Throwable {
        switch (op) {
            case InterpOpcodes.LASTORE: {
                long v = f.popLong();
                int i = f.popInt();
                Object a = f.popRef();
                checkArray(a, i);
                ((long[])a)[i] = v;
                return;
            }
            case InterpOpcodes.DASTORE: {
                double v = f.popDouble();
                int i = f.popInt();
                Object a = f.popRef();
                checkArray(a, i);
                ((double[])a)[i] = v;
                return;
            }
            case InterpOpcodes.AASTORE: {
                Object v = f.popRef();
                int i = f.popInt();
                Object a = f.popRef();
                checkArray(a, i);
                ((Object[])a)[i] = v;
                return;
            }
            default: break;
        }
        int v = f.popInt();
        int i = f.popInt();
        Object a = f.popRef();
        checkArray(a, i);
        switch (op) {
            case InterpOpcodes.IASTORE: ((int[])a)[i] = v; break;
            case InterpOpcodes.FASTORE: ((float[])a)[i] = Float.intBitsToFloat(v); break;
            case InterpOpcodes.BASTORE:
                if (a instanceof boolean[]) {
                    ((boolean[])a)[i] = v != 0;
                } else {
                    ((byte[])a)[i] = (byte)v;
                }
                break;
            case InterpOpcodes.CASTORE: ((char[])a)[i] = (char)v; break;
            case InterpOpcodes.SASTORE: ((short[])a)[i] = (short)v; break;
            default: throw new IllegalStateException("bad array store " + op);
        }
    }

    private void checkArray(Object array, int index) throws InterpThrowable {
        if (array == null) {
            throw new InterpThrowable(new NullPointerException("array is null"), snapshotStack());
        }
        int len = arrayLength(array);
        if (index < 0 || index >= len) {
            throw new InterpThrowable(new ArrayIndexOutOfBoundsException(
                    "index " + index + " length " + len), snapshotStack());
        }
    }

    /// The length of any array, without `java.lang.reflect`.
    ///
    /// `Array.getLength` is the obvious call and it is not available on
    /// ParparVM, whose `java.lang.reflect.Array` has only `newInstance`. That
    /// mattered more than a missing method usually does: the translator's
    /// interp-host pass prunes methods referencing absent members, so a single
    /// `Array.getLength` call was enough to eliminate the interpreter's main
    /// loop. The iOS build then reported every pushed program as having run
    /// successfully while executing none of it.
    ///
    /// The instanceof chain needs no reflection and no native support, so it
    /// behaves identically on every platform. It is also the form this codebase
    /// requires anyway -- ParparVM's CHECKCAST is unchecked, so a cast whose
    /// failure you intend to handle does not throw there.
    private static int arrayLength(Object array) {
        if (array instanceof Object[]) return ((Object[])array).length;
        if (array instanceof int[]) return ((int[])array).length;
        if (array instanceof byte[]) return ((byte[])array).length;
        if (array instanceof char[]) return ((char[])array).length;
        if (array instanceof long[]) return ((long[])array).length;
        if (array instanceof double[]) return ((double[])array).length;
        if (array instanceof float[]) return ((float[])array).length;
        if (array instanceof short[]) return ((short[])array).length;
        if (array instanceof boolean[]) return ((boolean[])array).length;
        throw new IllegalArgumentException("not an array: " + array.getClass().getName());
    }

    // -------------------------------------------------------------- stack ops

    private static void dupSlots(InterpFrame f, int count, int under) {
        int total = count + under;
        long[] p = new long[total];
        Object[] r = new Object[total];
        for (int i = 0; i < total; i++) {
            p[i] = f.stackPrim[f.sp - total + i];
            r[i] = f.stackRefs[f.sp - total + i];
        }
        // The duplicated slots move down past `under` slots, and the originals
        // shift up to sit above them.
        int base = f.sp - total;
        for (int i = 0; i < count; i++) {
            f.stackPrim[base + i] = p[under + i];
            f.stackRefs[base + i] = r[under + i];
        }
        for (int i = 0; i < under; i++) {
            f.stackPrim[base + count + i] = p[i];
            f.stackRefs[base + count + i] = r[i];
        }
        for (int i = 0; i < count; i++) {
            f.stackPrim[base + count + under + i] = p[under + i];
            f.stackRefs[base + count + under + i] = r[under + i];
        }
        f.sp += count;
    }

    // --------------------------------------------------------- boxing bridge

    private void pushBoxed(InterpFrame f, int kind, Object value) {
        if (kind == InterpOpcodes.RET_VOID) {
            return;
        }
        if (kind == InterpOpcodes.RET_OBJECT) {
            f.pushRef(fromHost(value));
            return;
        }
        long raw = InterpValues.unbox(kind, value);
        if (InterpOpcodes.isCategory2(kind)) {
            f.pushLong(raw);
        } else {
            f.pushInt((int)raw);
        }
    }

    /// Turns a host-visible peer back into the interpreted object it stands for.
    ///
    /// The round trip is the ordinary case, not an exotic one: interpreted code
    /// hands its peer to the framework, the framework hands it back -- as a
    /// listener argument, as an element of a list it sorted -- and from there
    /// interpreted code has to see its own object again. Without this, a cast
    /// to the interpreted class fails, because a shim instance is not an
    /// instance of anything the bundle declares.
    private static Object fromHost(Object value) {
        return value instanceof InterpBacked
                ? ((InterpBacked)value).getInterpObject()
                : value;
    }

    private Object popBoxed(InterpFrame f, int kind) {
        if (kind == InterpOpcodes.RET_OBJECT) {
            Object v = f.popRef();
            // An interpreted object crossing into host code has to go as its
            // peer; the host cannot do anything with an InterpObject.
            if (v instanceof InterpObject) {
                InterpObject io = (InterpObject)v;
                return io.hostPeer != null ? io.hostPeer : io;
            }
            return v;
        }
        if (InterpOpcodes.isCategory2(kind)) {
            return InterpValues.box(kind, f.popLong(), null);
        }
        return InterpValues.box(kind, f.popInt(), null);
    }

    // ------------------------------------------------------------ exceptions

    private Throwable toThrowable(Object t) {
        if (t == null) {
            return new InterpThrowable(new NullPointerException("throw null"), snapshotStack());
        }
        if (t instanceof Throwable) {
            // Deliberately not wrapped. A framework method that calls
            // interpreted code and catches IllegalStateException has to keep
            // catching it, so the exception has to stay the exception. The
            // interpreted frames are recorded beside it instead, and
            // [#interpretedStackFor] hands them back if it escapes.
            lastThrownStack = snapshotStack();
            lastHostCall = null;
            lastThrown = t;
            return (Throwable)t;
        }
        return new InterpThrowable(t, snapshotStack());
    }

    /// Where interpreted code threw this exception, or null if it did not.
    ///
    /// A host exception thrown by interpreted code carries the host's own stack
    /// trace, which names the interpreter's frames rather than the program's.
    /// This is the program's, recorded when it was thrown.
    public String[] interpretedStackFor(Throwable t) {
        return lastThrown == t ? lastThrownStack : null;
    }

    /// The framework method that threw this, or null if interpreted code did.
    public String hostCallFor(Throwable t) {
        return lastThrown == t ? lastHostCall : null;
    }

    private int findHandler(InterpMethod m, int insn, Object thrown) throws Throwable {
        int[] t = m.exceptionTable;
        for (int i = 0; i < t.length; i += 4) {
            if (insn < t[i] || insn >= t[i + 1]) {
                continue;
            }
            int typeExtern = t[i + 3];
            if (typeExtern < 0) {
                return t[i + 2];   // finally / catch-all
            }
            if (thrown != null && isInstanceOf(thrown, typeExtern)) {
                return t[i + 2];
            }
        }
        return -1;
    }

    /// The interpreted call stack as `Class.method(File:line)` frames,
    /// innermost first. Synthesised from the bundle's line table, because a
    /// real `Throwable`'s stack trace would show the interpreter's own frames
    /// instead of the user's.
    String[] snapshotStack() {
        Vector callStack = state().callStack;
        String[] out = new String[callStack.size()];
        int j = 0;
        for (int i = callStack.size() - 1; i >= 0; i--) {
            InterpFrame f = (InterpFrame)callStack.elementAt(i);
            int line = f.method.lineFor(f.insn);
            String file = f.method.owner.sourceFile == null ? "Unknown" : f.method.owner.sourceFile;
            out[j++] = f.method.owner.name.replace('/', '.') + "." + f.method.name
                    + "(" + file + (line >= 0 ? ":" + line : "") + ")";
        }
        return out;
    }

    /// Placeholder for a host object between `new` and its constructor.
    private static final class PendingHostNew {
        final int externIndex;

        PendingHostNew(int externIndex) {
            this.externIndex = externIndex;
        }
    }
}
