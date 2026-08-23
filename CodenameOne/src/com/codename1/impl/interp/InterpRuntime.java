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

    private volatile boolean cancelRequested;  //NOPMD AvoidUsingVolatile - written from another thread on purpose

    /// The last exception interpreted code threw, and the interpreted frames it
    /// was thrown from.
    ///
    /// On the runtime rather than on the thread state, because the thread that
    /// reports a failure is not the thread that ran the program -- a pushed
    /// main runs on the event thread and the socket thread is what answers the
    /// push. The pair is only ever read through an identity check, so the worst
    /// a race can do is decline to produce a stack.
    /// One record, published in one write: the throwable, the frames it came
    /// from and the host call it happened in. Three separate fields could be
    /// read half-updated -- another thread's frames beside this thread's
    /// throwable -- which is a confidently wrong stack rather than a missing
    /// one.
    private static final class Failure {
        private final Object thrown;
        /// The object interpreted code will see on the operand stack when a
        /// catch runs -- `getThrown()` for a wrapped InterpObject, and the
        /// throwable itself when there is no wrapper. Kept alongside
        /// [#thrown] so a rethrow of either identity is recognised: the
        /// wrapper is what escapes to host code, but the original is what
        /// interpreted code pops off the stack and passes to ATHROW.
        private final Object original;
        private final String[] stack;
        private final String hostCall;

        Failure(Object thrown, Object original, String[] stack, String hostCall) {
            this.thrown = thrown;
            this.original = original;
            this.stack = stack;
            this.hostCall = hostCall;
        }
    }

    private volatile Failure lastFailure;  //NOPMD AvoidUsingVolatile - written from another thread on purpose

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
        ThreadState s = (ThreadState) threadState.get();
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
    /// True once the program has been stopped and must not run again.
    private volatile boolean detached;  //NOPMD AvoidUsingVolatile - set from the UI thread, read on every callback

    /// Ends this runtime for good: cancels what is running and refuses every
    /// later callback.
    ///
    /// Stop cannot be only a cancellation. A normal Lifecycle program is not
    /// running when the user presses it -- its start() returned after showing a
    /// Form -- and what remains is listeners the framework still holds.
    public void detach() {
        detached = true;
        requestCancel();
    }

    /// Whether this runtime has been stopped.
    public boolean isDetached() {
        return detached;
    }

    /// Stands in for host subsystems this runtime only mocks. See
    /// [InterpHostInterceptor].
    private InterpHostInterceptor hostInterceptor;

    /// Installs the interceptor consulted before a host static call.
    public void setHostInterceptor(InterpHostInterceptor interceptor) {
        this.hostInterceptor = interceptor;
    }

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
        // Only a `public static void main(String[])` is an entry point, per
        // the same rule the JVM applies. A Lifecycle subclass that happens to
        // declare a private or instance helper of the same signature is not
        // meant to be entered through it -- the packer's finder rejects one
        // for exactly this reason, and the runtime has to match or a bundle
        // whose main class was chosen via the Lifecycle fallback would still
        // invoke the helper here with a null receiver.
        if (m != null && m.isStatic() && m.isPublic()) {
            return invokeInterpreted(m, null, new Object[]{args});
        }
        // A real Codename One application has no main: its entry point is a
        // Lifecycle subclass, and the platform calls init then start. Running
        // one is the whole point of this runtime, so that shape is entered the
        // way the platform would enter it.
        if (extendsHost(c, "com/codename1/system/Lifecycle")) {
            return runLifecycle(c);
        }
        throw new IllegalStateException(main + " has neither public static"
                + " main(String[]) nor a Lifecycle to start");
    }

    /// The pushed Lifecycle, when the program has one.
    private InterpObject lifecycle;

    /// Delivers `stop()` to the pushed Lifecycle, if it has one to receive.
    ///
    /// The platform calls stop before an application goes away, and a program
    /// that acquired anything releases it there. Call it before [#detach],
    /// which is what makes every later callback a no-op -- including this one.
    ///
    /// @return whether a stop() actually ran
    public boolean stopLifecycle() throws Throwable {
        InterpObject app = lifecycle;
        if (app == null || detached) {
            return false;
        }
        lifecycle = null;
        // The interpreted override when there is one, the framework's own
        // through the peer when there is not -- the same route init and start
        // took, so a program that overrides nothing still behaves like the
        // Lifecycle it is.
        callLifecycle(app.getType(), app, "stop", "()V", new Object[0]);
        return true;
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
            int ext = ((Integer) hostSupertypes.elementAt(i)).intValue();
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
        // Held before init, not after start: a Lifecycle that opened a media
        // player, a socket or a sensor releases it in stop(), and init is
        // already far enough in to have opened one. Recording it only once the
        // program was running left an init that threw with nothing to release
        // it, and detaching the runtime without calling stop leaves those
        // running against the runtime's own screen and the next pushed program.
        lifecycle = app;
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

    /// Returned by [#dispatch] when the program that owned the object has been
    /// stopped.
    ///
    /// Distinct from [#NOT_OVERRIDDEN] because the two mean different things to
    /// a shim that has nothing to defer to. A class shim answers both by
    /// calling the framework's own implementation, but an interface shim over
    /// an abstract method has none -- and it used to throw AbstractMethodError,
    /// which turned an expected late callback (a timer, a network response, a
    /// listener the framework still holds) into an event-thread failure long
    /// after the user stopped the program. On this sentinel a generated method
    /// quietly answers nothing instead.
    public static final Object DETACHED = new Object();

    /// Entry point for a generated shim: run the interpreted override of this
    /// method if there is one, otherwise report that there is not.
    ///
    /// The shim is a framework subclass compiled into the app, so every one of
    /// its overridable methods routes here. Methods the pushed class does not
    /// override have to cost as little as possible -- they are on the framework's
    /// own hot paths -- which is why the miss returns immediately rather than
    /// raising anything.
    public Object dispatch(InterpObject object, String name, String descriptor, Object[] args) {
        if (detached) {
            // The program was stopped. Its peers are still held by framework
            // listeners and timers, and cancellation only stops code that is
            // currently running -- so a short callback arriving now would
            // execute happily against a program the user has ended.
            return DETACHED;
        }
        if (object == null) {
            return NOT_OVERRIDDEN;
        }
        InterpMethod m = object.getType().resolve(name, descriptor);
        if (m == null || m.isAbstract()) {
            // The pushed class did not override the method, but Enum defines
            // some of them itself. `Collections.sort` on a list of interpreted
            // enum constants casts to Comparable and calls compareTo -- and
            // Enum.compareTo is not on the enum class's own method table.
            // Route the Enum-inherited methods through `enumCall` here so
            // host sorting works on peers that advertise Comparable.
            if (object.enumOrdinal >= 0) {
                Object early = enumCall(object, name, args);
                if (early != NOT_ENUM_METHOD) {
                    return early;
                }
            }
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
            InterpObject io = (InterpObject) result;
            return io.hostPeer != null ? io.hostPeer : io;
        }
        if (result instanceof Object[]) {
            // A returned array crosses like an argument does: its elements are
            // exchanged for their peers, or host code casting one to the
            // interface it implements gets a wrapper instead. Not converted
            // back -- the array is the caller's now, and interpreted code
            // reading an element converts on the way in.
            toHostElements(result, new Vector());
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
                f.setLocalInt(slot++, (int) InterpValues.unbox(kind, a));
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
                && ((InterpObject) receiver).hostPeer != null) {
            lock = ((InterpObject) receiver).hostPeer;
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
        // A fresh entry into the interpreter, which is what the budget covers.
        //
        // "Fresh" is not "depth == 0". A host call can run a nested event loop
        // -- Dialog.show, invokeAndBlock -- and dispatch a callback into the
        // interpreter from inside it, on the same thread, while the outer
        // frames are still on the stack. That callback is a new entry and needs
        // its own clock; without one it inherits an already-spent budget, or
        // (worse) is exempted from the check entirely because the outer host
        // call is still counted.
        //
        // Measuring from the start of the run instead of per entry made every
        // callback arriving more than edtBudgetMs after the program began --
        // which is every button press in a real application -- fail instantly
        // with "ran without yielding", having done nothing.
        boolean freshEntry = st.depth == 0 || st.hostCallDepth > 0;
        int enclosingHostCalls = 0;
        long enclosingRunStart = st.runStartMs;
        int enclosingFuel = st.fuel;
        if (freshEntry) {
            st.runStartMs = System.currentTimeMillis();
            st.fuel = fuelPerCheck;
            // The host calls below this entry are not this entry's business:
            // leaving them counted would exempt every reentrant callback from
            // the budget, which is exactly the wedge the budget exists to stop.
            enclosingHostCalls = st.hostCallDepth;
            st.hostCallDepth = 0;
        }
        // Entering a method is progress too. A back edge is the usual place to
        // check, but code that recurses, catches the StackOverflowError this
        // raises and recurses again never takes one -- and would hold the event
        // thread with Stop having no effect, since nothing would look at the
        // cancel flag. Charged against the same fuel counter, so the cost is a
        // decrement per call and a real check once every fuelPerCheck of them.
        st.fuel--;
        if (st.fuel <= 0) {
            checkpoint(st);
        }
        st.depth++;
        if (st.depth > maxDepth) {
            st.depth--;
            // The fresh-entry bookkeeping above already reset fuel, clock and
            // hostCallDepth; the finally that undoes it is only reached
            // through the run() call below, so this early throw has to undo it
            // itself. Otherwise the enclosing host call returns to a state
            // where hostCallDepth was zeroed and never restored, and a later
            // reentrant callback fails to recognise itself as fresh -- it
            // inherits the outer entry's budget and its cancellation
            // checkpoints stop firing.
            if (freshEntry) {
                st.hostCallDepth = enclosingHostCalls;
                st.runStartMs = enclosingRunStart;
                st.fuel = enclosingFuel;
            }
            throw new InterpThrowable(new StackOverflowError(
                    "interpreted call depth exceeded " + maxDepth), snapshotStack());
        }
        st.callStack.addElement(f);
        try {
            return run(f);
        } finally {
            st.callStack.removeElementAt(st.callStack.size() - 1);
            st.depth--;
            if (freshEntry) {
                st.hostCallDepth = enclosingHostCalls;
                // And the clock it was measured against. The outer entry is
                // still running -- its host call has not returned yet -- so
                // leaving the callback's clock in place would hand the outer
                // one a fresh budget every time a dialog dispatched an event,
                // and the host-call exclusion would then be added to a
                // timestamp that no longer belongs to anybody.
                st.runStartMs = enclosingRunStart;
                // The fuel counter with it. A loop whose body dispatches a
                // listener would otherwise see a nearly full counter on every
                // iteration and never reach a checkpoint -- so Stop would have
                // nothing to act on, which is the thing the counter is for.
                st.fuel = enclosingFuel;
            }
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
                        f.pushInt((int) f.prim[code[pc + 1]]);
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
                    case InterpOpcodes.ISUB: {
                        int b = f.popInt();
                        f.pushInt(f.popInt() - b);
                        break;
                    }
                    case InterpOpcodes.LSUB: {
                        long b = f.popLong();
                        f.pushLong(f.popLong() - b);
                        break;
                    }
                    case InterpOpcodes.FSUB: {
                        float b = f.popFloat();
                        f.pushFloat(f.popFloat() - b);
                        break;
                    }
                    case InterpOpcodes.DSUB: {
                        double b = f.popDouble();
                        f.pushDouble(f.popDouble() - b);
                        break;
                    }
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
                    case InterpOpcodes.FDIV: {
                        float b = f.popFloat();
                        f.pushFloat(f.popFloat() / b);
                        break;
                    }
                    case InterpOpcodes.DDIV: {
                        double b = f.popDouble();
                        f.pushDouble(f.popDouble() / b);
                        break;
                    }
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
                    case InterpOpcodes.FREM: {
                        float b = f.popFloat();
                        f.pushFloat(f.popFloat() % b);
                        break;
                    }
                    case InterpOpcodes.DREM: {
                        double b = f.popDouble();
                        f.pushDouble(f.popDouble() % b);
                        break;
                    }
                    case InterpOpcodes.INEG: f.pushInt(-f.popInt()); break;
                    case InterpOpcodes.LNEG: f.pushLong(-f.popLong()); break;
                    case InterpOpcodes.FNEG: f.pushFloat(-f.popFloat()); break;
                    case InterpOpcodes.DNEG: f.pushDouble(-f.popDouble()); break;
                    case InterpOpcodes.ISHL: {
                        int b = f.popInt();
                        f.pushInt(f.popInt() << b);
                        break;
                    }
                    case InterpOpcodes.LSHL: {
                        int b = f.popInt();
                        f.pushLong(f.popLong() << b);
                        break;
                    }
                    case InterpOpcodes.ISHR: {
                        int b = f.popInt();
                        f.pushInt(f.popInt() >> b);
                        break;
                    }
                    case InterpOpcodes.LSHR: {
                        int b = f.popInt();
                        f.pushLong(f.popLong() >> b);
                        break;
                    }
                    case InterpOpcodes.IUSHR: {
                        int b = f.popInt();
                        f.pushInt(f.popInt() >>> b);
                        break;
                    }
                    case InterpOpcodes.LUSHR: {
                        int b = f.popInt();
                        f.pushLong(f.popLong() >>> b);
                        break;
                    }
                    case InterpOpcodes.IAND: f.pushInt(f.popInt() & f.popInt()); break;
                    case InterpOpcodes.LAND: f.pushLong(f.popLong() & f.popLong()); break;
                    case InterpOpcodes.IOR: f.pushInt(f.popInt() | f.popInt()); break;
                    case InterpOpcodes.LOR: f.pushLong(f.popLong() | f.popLong()); break;
                    case InterpOpcodes.IXOR: f.pushInt(f.popInt() ^ f.popInt()); break;
                    case InterpOpcodes.LXOR: f.pushLong(f.popLong() ^ f.popLong()); break;
                    case InterpOpcodes.IINC:
                        f.prim[code[pc + 1]] = (int) f.prim[code[pc + 1]] + code[pc + 2];
                        break;

                    case InterpOpcodes.I2L: f.pushLong(f.popInt()); break;
                    case InterpOpcodes.I2F: f.pushFloat(f.popInt()); break;
                    case InterpOpcodes.I2D: f.pushDouble(f.popInt()); break;
                    case InterpOpcodes.L2I: f.pushInt((int) f.popLong()); break;
                    case InterpOpcodes.L2F: f.pushFloat(f.popLong()); break;
                    case InterpOpcodes.L2D: f.pushDouble(f.popLong()); break;
                    case InterpOpcodes.F2I: f.pushInt((int) f.popFloat()); break;
                    case InterpOpcodes.F2L: f.pushLong((long) f.popFloat()); break;
                    case InterpOpcodes.F2D: f.pushDouble(f.popFloat()); break;
                    case InterpOpcodes.D2I: f.pushInt((int) f.popDouble()); break;
                    case InterpOpcodes.D2L: f.pushLong((long) f.popDouble()); break;
                    case InterpOpcodes.D2F: f.pushFloat((float) f.popDouble()); break;
                    case InterpOpcodes.I2B: f.pushInt((byte) f.popInt()); break;
                    case InterpOpcodes.I2C: f.pushInt((char) f.popInt()); break;
                    case InterpOpcodes.I2S: f.pushInt((short) f.popInt()); break;

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

                    case InterpOpcodes.IFEQ:
                        if (f.popInt() == 0) {
                            next = code[pc + 1];
                        }
                        break;
                    case InterpOpcodes.IFNE:
                        if (f.popInt() != 0) {
                            next = code[pc + 1];
                        }
                        break;
                    case InterpOpcodes.IFLT:
                        if (f.popInt() < 0) {
                            next = code[pc + 1];
                        }
                        break;
                    case InterpOpcodes.IFGE:
                        if (f.popInt() >= 0) {
                            next = code[pc + 1];
                        }
                        break;
                    case InterpOpcodes.IFGT:
                        if (f.popInt() > 0) {
                            next = code[pc + 1];
                        }
                        break;
                    case InterpOpcodes.IFLE:
                        if (f.popInt() <= 0) {
                            next = code[pc + 1];
                        }
                        break;
                    case InterpOpcodes.IF_ICMPEQ: {
                        int b = f.popInt();
                        if (f.popInt() == b) {
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IF_ICMPNE: {
                        int b = f.popInt();
                        if (f.popInt() != b) {
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IF_ICMPLT: {
                        int b = f.popInt();
                        if (f.popInt() < b) {
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IF_ICMPGE: {
                        int b = f.popInt();
                        if (f.popInt() >= b) {
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IF_ICMPGT: {
                        int b = f.popInt();
                        if (f.popInt() > b) {
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IF_ICMPLE: {
                        int b = f.popInt();
                        if (f.popInt() <= b) {
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IF_ACMPEQ: {
                        Object b = f.popRef();
                        if (f.popRef() == b) {  //NOPMD CompareObjectsWithEquals - IF_ACMP compares references, by definition
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IF_ACMPNE: {
                        Object b = f.popRef();
                        if (f.popRef() != b) {  //NOPMD CompareObjectsWithEquals - IF_ACMP compares references, by definition
                            next = code[pc + 1];
                        }
                        break;
                    }
                    case InterpOpcodes.IFNULL:
                        if (f.popRef() == null) {
                            next = code[pc + 1];
                        }
                        break;
                    case InterpOpcodes.IFNONNULL:
                        if (f.popRef() != null) {
                            next = code[pc + 1];
                        }
                        break;
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
                        // `new Class[n]` is Object[] for the same reason a
                        // pushed type is: a class literal for a pushed type is
                        // an InterpClass token, and storing that in a real
                        // host Class[] would raise ArrayStoreException. The
                        // arg-conversion path materialises a real Class[]
                        // when handing one to a host method that wants it.
                        f.pushRef(isInterpretedLeaf(comp) || isClassLeaf(comp)
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
                        f.pushRef(isInterpretedLeaf(arrayType) || isClassLeaf(arrayType)
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
                        // The peer, when there is one: a synchronized method on
                        // an interpreted Form locks the peer (that is where the
                        // call runs), so a synchronized block locking the
                        // wrapper would be a second, unrelated monitor over
                        // state Java says the same one protects.
                        if (lock instanceof InterpObject
                                && ((InterpObject) lock).hostPeer != null) {
                            lock = ((InterpObject) lock).hostPeer;
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
                // Cancellation (InterpCancelled) is now caught by any handler,
                // including javac's compiler-generated cleanup for
                // try-with-resources (which is a `catch (Throwable)` entry
                // rather than a catch-all). The Stop button and EDT budget
                // are still honoured: `cancelRequested` stays set, so the
                // next checkpoint after the handler returns raises
                // `InterpCancelled` again. A `catch (Throwable)` around a
                // loop can therefore run its cleanup but cannot resume --
                // the back-edge checkpoint will re-fire cancellation on the
                // next iteration.
                Object thrown = it.getThrown();
                int handler = findHandler(m, insn, thrown, false);
                if (handler < 0) {
                    throw it;
                }
                f.sp = 0;
                f.pushRef(thrown);
                insn = handler;
                continue;
            } catch (Throwable hostThrown) {
                // Something the host raised while we were inside it. It is a
                // real Java throwable, and interpreted `catch` clauses have to
                // be able to see it.
                int handler = findHandler(m, insn, hostThrown, false);
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
                // that needs a fuel check. The checkpoint is wrapped in its
                // own try/catch that routes an InterpThrowable through
                // `findHandler` -- otherwise a Stop or budget cancellation
                // fired from a back edge would escape run() without ever
                // reaching the source-level `finally`, skipping the very
                // cleanup that had to run for the resource close.
                ThreadState st = state();
                st.fuel--;
                if (st.fuel <= 0) {
                    try {
                        checkpoint(st);
                    } catch (InterpThrowable it) {
                        Object thrown = it.getThrown();
                        int handler = findHandler(m, insn, thrown, false);
                        if (handler < 0) {
                            throw it;
                        }
                        f.sp = 0;
                        f.pushRef(thrown);
                        insn = handler;
                        continue;
                    }
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
        // Only on the event thread. A worker thread computing for ten seconds
        // blocks nothing and is a perfectly ordinary thing for a program to do;
        // killing it would be the runtime inventing a rule Java does not have.
        // Cancellation above applies to every thread, which is what the Stop
        // button needs.
        if (edtBudgetMs > 0 && st.hostCallDepth == 0 && st.runStartMs > 0
                && isEventThread()) {
            long elapsed = System.currentTimeMillis() - st.runStartMs;
            if (elapsed > edtBudgetMs) {
                throw new InterpThrowable(new InterpCancelled(
                        "pushed program ran for " + elapsed + "ms without yielding"),
                        snapshotStack());
            }
        }
    }

    /// Whether a thrown value is an Error, interpreted or not.
    ///
    /// A pushed `class MyError extends Error` is an InterpObject, so a host
    /// `instanceof Error` says no and the initializer's own error would be
    /// replaced by ExceptionInInitializerError -- which JLS 12.4.2 says happens
    /// only for a non-Error, and which would make `catch (MyError)` miss.
    private boolean isError(Object thrown) {
        if (thrown instanceof Error) {
            return true;
        }
        if (thrown instanceof InterpObject) {
            InterpObject io = (InterpObject) thrown;
            if (io.hostPeer instanceof Error) {
                return true;
            }
            try {
                return isInstanceOf(io, "java/lang/Error");
            } catch (Throwable cannotTell) {
                // The hierarchy could not be walked; treat it as not an Error,
                // which wraps rather than loses it.
                return false;
            }
        }
        return false;
    }

    /// What an interpreted failure actually carries: the thrown object when it
    /// arrived in the interpreter's carrier, the throwable itself otherwise.
    private static Object unwrapInterpreted(Throwable failure) {
        if (failure instanceof InterpThrowable) {
            return ((InterpThrowable) failure).getThrown();
        }
        return failure;
    }

    /// Whether the wall-clock budget applies on this thread.
    ///
    /// It is an *event thread* budget: a worker thread computing for ten
    /// seconds blocks nothing, and killing it would be the runtime inventing a
    /// rule Java does not have. Cancellation is separate and applies
    /// everywhere, which is what the Stop button needs.
    ///
    /// With no Display -- the conformance harness, and anything embedding the
    /// interpreter headless -- there is no event thread to protect and no way
    /// to identify one, so the budget applies to whatever thread is running.
    private boolean isEventThread() {
        return !com.codename1.ui.Display.isInitialized()
                || com.codename1.ui.Display.getInstance().isEdt();
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
                // Raw long bits, matching the writer's format. Reading via
                // `Double.parseDouble` collapsed every noncanonical NaN back
                // to the canonical `0x7ff8000000000000L` -- because "NaN" is
                // the only spelling `Double.toString` produces for any NaN --
                // and a program that read the LDC constant back with
                // `doubleToRawLongBits` would see the wrong bits.
                f.pushDouble(Double.longBitsToDouble(Long.parseLong(bundle.string(operand))));
                break;
            case InterpOpcodes.LDC_STRING:
                f.pushRef(bundle.string(operand));
                break;
            case InterpOpcodes.LDC_CLASS: {
                // `Color.class` where Color is in this bundle names something
                // the host has never heard of, so the class object is the
                // InterpClass itself. Every consumer that can receive one --
                // Enum.valueOf is the one javac generates -- checks for it.
                String literal = externOwnerName(operand);
                InterpClass local = bundle.findClass(literal);
                if (local == null && isInterpretedLeaf(literal)) {
                    // `Entry[].class` for a bundle-only Entry. There is no host
                    // class for it and asking the linker to load `[LEntry;`
                    // fails before the program can so much as call getName(),
                    // so the interpreter makes a token of its own -- one per
                    // rank, so `Entry[].class == Entry[].class` holds and
                    // `Entry[].class != Entry.class` does too.
                    local = arrayTokenFor(literal);
                }
                f.pushRef(local != null
                        ? (Object) local
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
                if (c.initThread == Thread.currentThread()) {  //NOPMD CompareObjectsWithEquals - the initializing thread, not an equal one
                    // Recursive entry from the initializer itself: legal, and
                    // the one case where a partly-built class must be visible.
                    return;
                }
                // Uninterruptibly, as JVMS 5.5 requires: waiting for another
                // thread's <clinit> is not something the instruction that
                // triggered it can report. An interrupt is remembered and
                // reasserted, so the program still sees it at its next
                // interruptible point rather than as a failure from a getstatic.
                boolean interrupted = false;
                while (c.initState == InterpClass.INIT_RUNNING) {
                    try {
                        c.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
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
            } else if (c.superExtern >= 0) {
                // A host superclass has to be initialized first too. Resolution
                // deliberately does not initialize, so without this the host
                // parent's <clinit> runs whenever its first peer is constructed
                // -- after the interpreted subclass's, which reverses the order
                // Java guarantees and with it any registration the parent does.
                linker.initializeClass(externOwnerName(c.superExtern));
            }
            // JLS 12.4.1: initializing a *class* initializes the
            // superinterfaces that declare a default method, and only those.
            // Reaching one through an interface that declares none does not
            // initialize the intermediate interface -- so this walks the
            // hierarchy but initializes only the interfaces that themselves
            // declare a default method. Initializing an interface, on the other
            // hand, initializes none of its superinterfaces at all, which is why
            // this is skipped for one.
            if (!c.isInterface()) {
                initializeDefaultBearingInterfaces(c);
            }
            InterpMethod clinit = c.declaredMethod("<clinit>", "()V");
            if (clinit != null) {
                try {
                    invokeInterpreted(clinit, null, null);
                } catch (Throwable failure) {
                    // JLS 12.4.2: a non-Error failure is wrapped, so
                    // `catch (ExceptionInInitializerError)` -- which is how Java
                    // code catches this -- actually catches it. An Error passes
                    // through unwrapped, as the spec says, and so does
                    // cancellation, which is not the program's failure at all.
                    Object thrown = unwrapInterpreted(failure);  //NOPMD AvoidInstanceofChecksInCatchClause - the carrier has to be looked through
                    if (isError(thrown)) {
                        throw failure;
                    }
                    // The peer when the failure is a pushed exception class:
                    // an ExceptionInInitializerError whose getCause() is null
                    // says nothing about what actually went wrong, and the peer
                    // is the throwable host code was given.
                    ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                            InterpThrowable.hostThrowableOf(thrown));
                    throw new InterpThrowable(wrapped, interpretedStackFor(failure));
                }
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
        return bundle.findClass(leafOf(descriptor)) != null;
    }

    /// Whether the leaf of a component or array-type descriptor is
    /// java.lang.Class. Accepts the bare name `java/lang/Class`, the
    /// L-wrapped form `Ljava/lang/Class;`, and any level of bracketing:
    /// ANEWARRAY names an outer `Class[][]` as `[Ljava/lang/Class;`, and the
    /// inner `Class[]` allocation must round-trip to the same Object[]
    /// representation or storing it into the outer array throws
    /// ArrayStoreException.
    private static boolean isClassLeaf(String descriptor) {
        return "java/lang/Class".equals(leafOf(descriptor));
    }

    /// The token for an interpreted array type named by a descriptor.
    private InterpClass arrayTokenFor(String descriptor) {
        InterpClass t = bundle.findClass(leafOf(descriptor));
        if (t == null) {
            return null;
        }
        for (int i = 0; i < descriptor.length() && descriptor.charAt(i) == '['; i++) {
            t = t.arrayType();
        }
        return t;
    }

    /// The class name at the bottom of a descriptor: `[[LEntry;` is `Entry`.
    private static String leafOf(String descriptor) {
        String at = descriptor;
        while (at.length() > 0 && at.charAt(0) == '[') {
            at = at.substring(1);
        }
        if (at.length() > 2 && at.charAt(0) == 'L' && at.endsWith(";")) {
            at = at.substring(1, at.length() - 1);
        }
        return at;
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
        for (InterpMethod m : iface.methods) {
            // Private too: an interface may declare a private helper with a
            // body (JDK 9 onwards) and that is not a default method, so it must
            // not pull the interface's initializer forward.
            if (!m.isStatic() && !m.isAbstract() && !m.isPrivate()
                    && !"<clinit>".equals(m.name)) {
                return true;
            }
        }
        return false;
    }

    /// Initializes every superinterface that declares a default method itself.
    ///
    /// Walks through the ones that do not, rather than stopping at them: an
    /// interface with no default method is not initialized on an implementor's
    /// behalf, but an interface *above* it that has one still is.
    private void initializeDefaultBearingInterfaces(InterpClass c) throws Throwable {
        for (InterpClass iface : c.interpInterfaces) {
            if (iface == null) {
                continue;
            }
            // Above it first, always. `ensureInitialized` on an interface
            // deliberately initializes none of its superinterfaces -- that is
            // the rule for initializing an interface -- so the walk on the
            // class's behalf has to reach the ancestors itself, in the order
            // the JVM would.
            initializeDefaultBearingInterfaces(iface);
            if (declaresDefaultMethod(iface)) {
                ensureInitialized(iface);
            }
        }
        // Host interfaces too, and under the same rule -- but the whole walk
        // belongs to the platform. The bundle records only the interfaces a
        // class declares directly, so an interpreted class implementing a host
        // Child that extends a default-bearing host Parent is a hierarchy only
        // the app can see.
        for (int i = 0; i < c.hostInterfaces.length; i++) {
            linker.initializeDefaultBearingInterfaces(externOwnerName(c.hostInterfaces[i]));
        }
    }

    // ---------------------------------------------------------------- fields

    private void getStatic(InterpFrame f, int ext) throws Throwable {
        String owner = externOwnerName(ext);
        String name = bundle.string(bundle.externName[ext]);
        String desc = bundle.string(bundle.externDesc[ext]);
        InterpClass ic = bundle.findClass(owner);
        if (ic != null) {
            if (declaredByInterpreted(ic, name)) {
                // The class that declares it, not the one the call site named.
                // `Child.x` where Parent declares x initializes Parent and
                // leaves Child alone.
                InterpClass holder = findStaticHolder(ic, name);
                ensureInitialized(holder);
                pushBoxed(f, InterpValues.kindOf(desc), holder.staticValue(name));
                return;
            }
            // Inherited from a host supertype -- a superclass, or an
            // interface whose constant is read through the implementing class.
            // Reading it from the interpreted class would answer null for a
            // field that class never declared.
            String[] hostOwners = hostStaticOwners(ic);
            for (int i = 0; i < hostOwners.length; i++) {
                try {
                    pushBoxed(f, InterpValues.kindOf(desc),
                            linker.getStatic(hostOwners[i], name, desc));
                    return;
                } catch (Throwable notThere) {
                    // Only "this candidate does not have it" moves on. A field
                    // that exists and whose class initializer threw must be
                    // reported as itself, not masked by the next candidate's
                    // NoSuchFieldError.
                    if (!isAbsent(notThere) || i == hostOwners.length - 1) {
                        throw notThere;
                    }
                }
            }
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
            if (declaredByInterpreted(ic, name)) {
                InterpClass holder = findStaticHolder(ic, name);
                ensureInitialized(holder);
                holder.setStaticValue(name, value);
                return;
            }
            // As above: writing it here would create a private copy the host
            // never sees, and leave the real field unchanged. An interface's
            // fields are final, so only the superclass chain can be written --
            // but the same lookup is used so a wrong owner fails loudly rather
            // than silently writing somewhere else.
            String[] hostOwners = hostStaticOwners(ic);
            for (int i = 0; i < hostOwners.length; i++) {
                try {
                    linker.setStatic(hostOwners[i], name, desc, value);
                    return;
                } catch (Throwable notThere) {
                    if (!isAbsent(notThere) || i == hostOwners.length - 1) {
                        throw notThere;
                    }
                }
            }
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
    /// Whether the named Object method is one that reports naming or identity
    /// (`toString`, `hashCode`, `equals`) rather than one tied to the object's
    /// monitor (`wait`, `notify`, `notifyAll`). The interface-only-peer route
    /// above uses this to hand naming/identity to the interpreter -- so the
    /// pushed class's own name shows through -- while leaving monitor
    /// operations on the peer, which is what MONITORENTER already locked.
    private static boolean isObjectNamingOrIdentity(String methodName) {
        return "toString".equals(methodName)
                || "hashCode".equals(methodName)
                || "equals".equals(methodName);
    }

    private int fieldIndex(InterpObject io, String owner, String name) {
        InterpClass declaring = bundle.findClass(owner);
        if (declaring == null) {
            return -1;
        }
        return io.indexOf(declaring, name);
    }

    /// Whether the named instance field is declared `volatile`. Walks up the
    /// declared owner's chain to reach the class that actually declares the
    /// field, matching `indexOf`.
    private boolean isInstanceFieldVolatile(String owner, String name) {
        InterpClass declaring = bundle.findClass(owner);
        while (declaring != null) {
            for (int i = 0; i < declaring.fieldNames.length; i++) {
                if (declaring.fieldNames[i].equals(name)) {
                    return declaring.isInstanceFieldVolatile(i);
                }
            }
            declaring = declaring.superInterp;
        }
        return false;
    }

    // Static-field volatile access needs no explicit synchronisation from
    // the interpreter: static storage goes through `Hashtable.get`/`put`,
    // whose synchronised bodies establish happens-before between the writer
    // and any reader that also enters the map. That gives static volatile
    // the same memory-visibility guarantee the JVM does, without an extra
    // wrapping monitor.

    /// The nearest host ancestor's internal name, or null when there is none.
    ///
    /// A static that no interpreted class in the chain declares belongs to the
    /// host superclass: `MyForm.SOME_CONSTANT` compiles to a field reference
    /// owned by MyForm, which the installed app has never heard of, so the
    /// access has to be re-addressed to the class that does declare it.
    private String hostOwnerOf(InterpClass c) {
        InterpClass k = c;
        while (k != null) {
            if (k.superExtern >= 0) {
                return externOwnerName(k.superExtern);
            }
            k = k.superInterp;
        }
        return null;
    }

    /// Whether a failure means "this class does not have that member" rather
    /// than "reading it went wrong".
    ///
    /// The difference decides whether another candidate owner may be tried. A
    /// class initializer that threw is a real failure and belongs to the
    /// caller; only absence is a reason to keep looking.
    private static boolean isAbsent(Throwable t) {
        // NoClassDefFoundError is deliberately not here. It is what a class
        // whose initializer already failed throws on every later touch, and
        // treating that as "this candidate does not have the field" would move
        // on and report the next one's NoSuchFieldError -- hiding the failure
        // this whole distinction exists to preserve. A class the app genuinely
        // lacks arrives as ClassNotFoundException from the reflective linker
        // and as NoSuchFieldError from the symbol-table one.
        if (t instanceof NoSuchFieldError || t instanceof ClassNotFoundException) {
            return true;
        }
        // NoSuchFieldException is a reflection type, and the device's java.lang
        // does not have one -- naming it here would not compile for the device
        // at all. The reflection-backed linker still throws it, so it is
        // recognised by name.
        String thrown = t.getClass().getName();
        return "java.lang.NoSuchFieldException".equals(thrown)
                || "java.lang.NoSuchMethodException".equals(thrown);
    }

    /// Where a static the interpreted hierarchy does not declare might live.
    ///
    /// The host superclass chain first, then the host interfaces -- a constant
    /// on an implemented interface is read as `PushedClass.FIELD`, and the
    /// superclass walk answers `java/lang/Object`, which does not have it.
    /// Every candidate is tried in turn because only the linker can say which
    /// one actually declares it.
    private String[] hostStaticOwners(InterpClass c) {
        Vector out = new Vector();
        String superOwner = hostOwnerOf(c);
        if (superOwner != null) {
            out.addElement(superOwner);
        }
        collectHostInterfaceOwners(c, out, new Vector());
        String[] owners = new String[out.size()];
        out.copyInto(owners);
        return owners;
    }

    /// Every host interface reachable from this class, however it is reached.
    ///
    /// Through the interpreted superclass chain and through interpreted
    /// interfaces alike: `class C implements I` where `interface I extends
    /// HostIface` reads `HostIface.VALUE` as `C.VALUE`, and a walk that only
    /// followed superclasses never saw HostIface -- so a constant that plainly
    /// exists was reported as NoSuchFieldError. The visited set is what makes
    /// the diamond an interface hierarchy is allowed to be terminate; a depth
    /// cap would answer wrongly on a legal hierarchy instead.
    private void collectHostInterfaceOwners(InterpClass c, Vector out, Vector visited) {
        InterpClass k = c;
        while (k != null) {
            if (visited.contains(k)) {
                return;
            }
            visited.addElement(k);
            for (int i = 0; i < k.hostInterfaces.length; i++) {
                String iface = externOwnerName(k.hostInterfaces[i]);
                if (!out.contains(iface)) {
                    out.addElement(iface);
                }
            }
            for (int i = 0; i < k.interpInterfaces.length; i++) {
                collectHostInterfaceOwners(k.interpInterfaces[i], out, visited);
            }
            k = k.superInterp;
        }
    }

    /// Whether any interpreted class in the chain declares this static.
    private boolean declaredByInterpreted(InterpClass c, String name) throws Throwable {
        InterpClass k = c;
        while (k != null) {
            if (k.declaresStatic(name) || findStaticInInterfaces(k, name) != null) {
                return true;
            }
            k = k.superInterp;
        }
        return false;
    }

    /// The class a host instance field really belongs to.
    ///
    /// The call site names the interpreted class -- javac records the type it
    /// saw -- and the installed app has never heard of it, so the nearest host
    /// ancestor is the name the linker can resolve.
    private String hostFieldOwner(InterpObject io, String owner) {
        if (bundle.findClass(owner) == null) {
            return owner;
        }
        String hostOwner = hostOwnerOf(io.type);
        return hostOwner == null ? owner : hostOwner;
    }

    private InterpClass findStaticHolder(InterpClass c, String name) throws Throwable {
        InterpClass k = c;
        while (k != null) {
            if (k.declaresStatic(name)) {
                return k;
            }
            // Interfaces too, and before moving up: `B.Z` where B implements an
            // interface declaring Z compiles to a field reference owned by B,
            // and searching only the superclass chain answers with B, which
            // declares no such field. That reads as the field's default value
            // rather than as an error, which is the worst way to be wrong.
            InterpClass fromInterface = findStaticInInterfaces(k, name);
            if (fromInterface != null) {
                return fromInterface;
            }
            k = k.superInterp;
        }
        return c;
    }

    /// The interface that declares a static field, searching an interpreted
    /// class's interfaces depth first. Initializes it on the way, since reading
    /// an interface's field is exactly what initializes that interface.
    private InterpClass findStaticInInterfaces(InterpClass c, String name) throws Throwable {
        for (InterpClass iface : c.interpInterfaces) {
            if (iface == null) {
                continue;
            }
            if (iface.declaresStatic(name)) {
                ensureInitialized(iface);
                return iface;
            }
            InterpClass deeper = findStaticInInterfaces(iface, name);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    /// A static method declared by this class or inherited from an interpreted
    /// superclass.
    ///
    /// The vtable cannot answer: it holds instance methods only, by design, and
    /// javac records the *call site's* owner for a static call, so `B.m()` where
    /// B inherits m from A arrives naming B. Falling through to the host linker
    /// from there asks it for a class only the bundle has.
    private static InterpMethod resolveStatic(InterpClass c, String name, String desc) {
        InterpClass k = c;
        while (k != null) {
            InterpMethod m = k.declaredMethod(name, desc);
            if (m != null && m.isStatic()) {
                return m;
            }
            k = k.superInterp;
        }
        return null;
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
            InterpObject io = (InterpObject) target;
            int idx = fieldIndex(io, owner, name);
            if (idx >= 0) {
                Object v;
                if (isInstanceFieldVolatile(owner, name)) {
                    // `volatile` needs the read to happen-after every write
                    // this field's writer performed before publishing. A
                    // plain `io.fields[idx]` is a lock-free access with no
                    // barrier, so a `volatile boolean ready` coordinating a
                    // worker-thread handoff would let the reader observe
                    // `ready` while the writes it announced remained stale.
                    // Synchronising on the object gives the pair the same
                    // happens-before ordering the JVM does.
                    synchronized (io) {
                        v = io.fields[idx];
                    }
                } else {
                    v = io.fields[idx];
                }
                pushBoxed(f, InterpValues.kindOf(desc), v);
                return;
            }
            // Declared by a host superclass, so it lives on the peer -- and
            // under that class's name, not the interpreted subclass javac
            // recorded. A pushed Form subclass reading `focusScrolling` names
            // itself as the owner, and no linker has ever heard of it.
            pushBoxed(f, InterpValues.kindOf(desc),
                    linker.getField(io.hostPeer, hostFieldOwner(io, owner), name, desc));
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
            InterpObject io = (InterpObject) target;
            int idx = fieldIndex(io, owner, name);
            if (idx >= 0) {
                if (isInstanceFieldVolatile(owner, name)) {
                    // Same rationale as getField above: the reader waits on
                    // the same monitor, and both entries share happens-before.
                    synchronized (io) {
                        io.fields[idx] = value;
                    }
                } else {
                    io.fields[idx] = value;
                }
                return;
            }
            linker.setField(io.hostPeer, hostFieldOwner(io, owner), name, desc, value);
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
                InterpMethod m = ic.declaredMethod(name, desc);
                if (m == null) {
                    m = resolveStatic(ic, name, desc);
                }
                if (m == null) {
                    m = ic.resolve(name, desc);
                }
                // The declaring class, not the one the call site named. `B.m()`
                // where only A declares m initializes A and leaves B alone, and
                // an initializer with an observable effect makes the difference
                // visible.
                ensureInitialized(m != null && m.owner != null ? m.owner : ic);
                if (m != null) {
                    pushBoxed(f, returnKind, invokeInterpreted(m, null, args));
                    return;
                }
                // Nothing interpreted declares it, so it is inherited from the
                // host superclass -- and the call site names the interpreted
                // subclass, which the installed app has never heard of.
                // Re-address it to the class that does declare it.
                String hostOwner = hostOwnerOf(ic);
                if (hostOwner != null) {
                    pushBoxed(f, returnKind,
                            hostCall(hostOwner, name, desc, null, args, false));
                    return;
                }
            }
            // javac compiles an enum's own valueOf(String) into a call to
            // Enum.valueOf(Class,String), which reflects over the class's
            // constants. There is no reflection here and the class is
            // interpreted, so the lookup runs against the bundle instead.
            if ("java/lang/Enum".equals(owner) && "valueOf".equals(name)
                    && args.length == 2 && args[0] instanceof InterpClass) {
                pushBoxed(f, returnKind, enumValueOf((InterpClass) args[0], (String) args[1]));
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
                pushBoxed(f, returnKind, new NativeStub((InterpClass) args[0]));
                return;
            }
            // `System.identityHashCode(value)` must return the same hash as
            // `value.hashCode()` would when hashCode is not overridden.
            // Which object that hashes depends on whether the pushed class
            // has a host peer to inherit `Object.hashCode` from:
            //
            //  * class-backed peer (`class Sub extends Form`): hashCode
            //    reaches the peer's inherited Object.hashCode and returns
            //    the peer's identity, so identityHashCode has to match it.
            //  * interface-only peer or peerless: hashCode is answered by
            //    `objectCall(io)` which hashes the InterpObject wrapper,
            //    so identityHashCode has to hash the wrapper too.
            //
            // `popBoxed` handed us the peer for a peered InterpObject, so
            // `fromHost` picks the wrapper back up; the choice below then
            // selects the identity that matches the hashCode path.
            if ("java/lang/System".equals(owner) && "identityHashCode".equals(name)
                    && "(Ljava/lang/Object;)I".equals(desc) && args.length == 1) {
                Object v = fromHost(args[0]);
                if (v instanceof InterpObject) {
                    InterpObject io = (InterpObject) v;
                    Object target = io.hostPeer != null && !io.hostPeerFromInterfacesOnly
                            ? io.hostPeer : io;
                    pushBoxed(f, returnKind, Integer.valueOf(System.identityHashCode(target)));
                    return;
                }
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
            PendingHostNew pending = (PendingHostNew) target;
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
            Object r = classCall((InterpClass) target, name, args);
            if (r != NOT_CLASS_METHOD) {
                pushBoxed(f, returnKind, r);
                return;
            }
            throw new InterpThrowable(new UnsupportedOperationException(
                    "Class." + name + desc + " is not available for "
                    + ((InterpClass) target).getName().replace('/', '.')
                    + ", which exists only in this bundle"), snapshotStack());
        }

        if (target instanceof InterpObject) {
            InterpObject io = (InterpObject) target;

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
                // A private method is not virtual, and the opcode no longer
                // says so: from JDK 11 javac emits invokevirtual for one,
                // nestmates having replaced the synthetic bridges. Resolving
                // from the receiver would make `Base.value()` calling its own
                // private `label()` land on a `label()` that Child happens to
                // declare -- a different answer, silently.
                m = resolveVirtual(io.type, owner, name, desc);
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
            // getClass() first. The peer is a generated shim -- Interp_ui_Form
            // -- and answering with its class would hand the program a name it
            // has never heard of, break class identity, and pass that on to
            // every API taking a Class.
            if ("getClass".equals(name) && args.length == 0) {
                pushBoxed(f, returnKind, io.type);
                return;
            }
            // Enum's own methods first when the receiver is a constant: an
            // interpreted enum that implements a host interface gets an
            // Object-based peer, and that peer does not inherit java.lang.Enum,
            // so name() and ordinal() would be looked up on something that has
            // never heard of them.
            if (io.enumOrdinal >= 0) {
                Object early = enumCall(io, name, args);
                if (early != NOT_ENUM_METHOD) {
                    pushBoxed(f, returnKind, early);
                    return;
                }
            }
            // Object's naming/identity defaults on an interface-only peer:
            // the shim's Object.toString/hashCode/equals prints the shim
            // class's own name (`Interp_Runnable@...`), which is neither the
            // pushed class's name nor consistent with `getClass()` above.
            // Answered through `objectCall` here so the class's own name
            // shows through and an interpreted override still wins via the
            // `resolveVirtual` result higher up.
            //
            // Only those three -- `wait`/`notify`/`notifyAll` stay on the
            // peer, because `MONITORENTER` already locked the peer (see
            // `synchronized`) and running them on `io` instead acquires a
            // different monitor and raises IllegalMonitorStateException. Any
            // other Object method that reaches here is also left to the peer.
            if (io.hostPeer != null && io.hostPeerFromInterfacesOnly
                    && "java/lang/Object".equals(owner)
                    && isObjectNamingOrIdentity(name)) {
                Object early = objectCall(io, name, args, op == InterpOpcodes.INVOKESPECIAL);
                if (early != NOT_OBJECT_METHOD) {
                    pushBoxed(f, returnKind, early);
                    return;
                }
            }
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
                    //
                    // Unless there is no bridge, which is not an error: a shim
                    // overrides only what it can, so a *final* host method has
                    // none -- and `super.play()` on a final method is ordinary
                    // Java. With nothing overriding it, calling the method
                    // itself is what the super call means, and cannot recurse.
                    if (linker.hasMethod(peerOwner, "super_" + name, desc)) {
                        pushBoxed(f, returnKind, hostCall(peerOwner, "super_" + name, desc,
                                io.hostPeer, args, false));
                    } else {
                        pushBoxed(f, returnKind,
                                hostCall(owner, name, desc, io.hostPeer, args, true));
                    }
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
                Object r = enumCall(io, name, args);
                if (r != NOT_ENUM_METHOD) {
                    pushBoxed(f, returnKind, r);
                    return;
                }
            }
            // java.lang.Object's own methods, for an object with no peer to
            // inherit them from. getClass() is not an exotic case: javac emits
            // a `receiver.getClass()` null check in front of every bound
            // method reference, so `self::method` needs it.
            Object r = objectCall(io, name, args, op == InterpOpcodes.INVOKESPECIAL);
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
            io.enumName = superArgs.length > 0 ? (String) superArgs[0] : null;
            io.enumOrdinal = superArgs.length > 1 && superArgs[1] instanceof Number
                    ? ((Number) superArgs[1]).intValue() : -1;
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
            int ext = ((Integer) hostSupertypes.elementAt(i)).intValue();
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
        // Interfaces Enum implements are not in the pushed class's own
        // interface list, but they still belong on the peer -- an enum
        // sorted through Collections.sort casts to Comparable, and Enum
        // supplies that. `enumCall` answers `compareTo`, so the shim only
        // needs to advertise the interface; the interpreter routes the
        // call back. Comparable and Serializable are the two Enum brings
        // in on every JDK.
        if (io.enumOrdinal >= 0) {
            addIfPresent(interfaces, "java/lang/Comparable");
            addIfPresent(interfaces, "java/io/Serializable");
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
        // Interface-only peer: no host superclass in the chain, so Object's
        // own default methods still belong to the interpreter. The dispatch
        // path below routes toString/hashCode/equals to `objectCall` in that
        // case rather than calling the shim, which inherits Object.toString
        // from the shim class and prints `Interp_Runnable@...` instead of
        // the pushed class's own name.
        io.hostPeerFromInterfacesOnly = hostSuperclassName == null;
    }

    private Object hostCall(String owner, String name, String desc, Object target,
                            Object[] args, boolean special) throws Throwable {
        // Fuel accounting stops for the duration: a host call may legitimately
        // block for a long time (invokeAndBlock waiting on the network) and
        // that must not read as a runaway loop.
        ThreadState st = state();
        // A class token for a bundle-only type is an InterpClass, and a host
        // method declaring java.lang.Class cannot be handed one. The documented
        // resource idiom -- `getResourceAsStream(getClass(), "/theme.res")` --
        // hits this on every pushed program, so the token is exchanged for the
        // class of its nearest host ancestor: the same class loader, and a real
        // Class.
        //
        // Only where the parameter actually says Class. Substituting into an
        // Object parameter loses the token itself: `list.add(Pushed.class)`
        // would store Object.class, and reading it back would not equal the
        // literal the program still holds. A host that took it as Object never
        // needed a Class in the first place -- it is storing a reference.
        if (target != null && "java/lang/Class".equals(owner)
                && "isAssignableFrom".equals(name) && args.length == 1
                && args[0] instanceof InterpClass) {
            // `Runnable.class.isAssignableFrom(Task.class)` where Task is a
            // pushed class implementing Runnable. Substituting the token below
            // would hand the host Object.class and get false for a relationship
            // the bundle records; the answer is whether any host supertype the
            // pushed class actually has is assignable to the receiver.
            return assignableFromInterp(target, (InterpClass) args[0])
                    ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("getResourceAsStream".equals(name) && args.length == 2
                && args[0] instanceof InterpClass && args[1] instanceof String) {
            // Java resolves a relative resource name against the *caller's*
            // package -- `getResourceAsStream(MyApp.class, "data.json")` reads
            // /com/example/data.json -- and the bundle stores it under exactly
            // that path. The token is about to become a host class, taking the
            // package with it, so the name is qualified here while it is still
            // known.
            String path = (String) args[1];
            if (path.length() > 0 && path.charAt(0) != '/') {
                String caller = ((InterpClass) args[0]).getName();
                int slash = caller.lastIndexOf('/');
                args[1] = slash < 0 ? "/" + path : "/" + caller.substring(0, slash + 1) + path;
            }
        }
        String[] params = paramDescriptors(desc);
        // Pairs of (src, dst) for Class[] materialisations: kept so the finally
        // block can copy any host mutations back to the interpreter-owned
        // Object[], preserving Java's array-by-reference semantics for
        // `Class<?>...` arguments the host method reorders, clears or replaces.
        Vector classArrayPairs = null;
        for (int i = 0; i < args.length && i < params.length; i++) {
            if (args[i] instanceof InterpClass && "Ljava/lang/Class;".equals(params[i])) {
                args[i] = hostClassFor((InterpClass) args[i]);
            } else if (args[i] instanceof Object[]
                    && "[Ljava/lang/Class;".equals(params[i])) {
                // A `Class<?>[]` argument -- ordinary `Class.getMethod(...,
                // Type.class)` and any varargs `Class<?>...` call. We hand the
                // interpreter an Object[] for `new Class[n]` (a Class[] would
                // reject an InterpClass token at AASTORE), and a host method
                // expecting a real Class[] needs one materialised at the
                // boundary. Elements go through hostClassFor so pushed-only
                // classes at least resolve to their nearest host ancestor,
                // matching the scalar conversion above.
                Object[] src = (Object[]) args[i];
                Object[] converted = classArrayFor(src);
                if (converted != null) {
                    if (classArrayPairs == null) {
                        classArrayPairs = new Vector();
                    }
                    classArrayPairs.addElement(src);
                    classArrayPairs.addElement(converted);
                    args[i] = converted;
                }
            }
        }
        // Elements of a reference array cross the same way a scalar argument
        // does: as their peers. `Arrays.sort(items)` where the items implement
        // a host Comparable would otherwise hand the host a wrapper it can
        // only fail to cast. The array is not converted back afterwards --
        // `Arrays.asList(items)`, `Collections.addAll` and other collectors
        // retain the passed array, and reverting elements in place would
        // leave the host's alias holding InterpObject wrappers that do not
        // implement the interfaces their peers do. Interpreted reads via
        // AALOAD run each element through `fromHost`, so an element that
        // stayed as its peer round-trips back to the wrapper on the way in
        // and everything the interpreter compares by identity still matches.
        for (Object arg : args) {
            if (arg instanceof Object[]) {
                toHostElements(arg, new Vector());
            }
        }
        // A typed-array parameter (`Component[]`, `MyButton[]`,
        // `Component[][]`, `Object[][]`, ...) needs an actual array of that
        // host component type. The interpreter's representation is a plain
        // `Object[]` -- `ANEWARRAY` cannot allocate a Sub[] whose leaf
        // exists in the bundle, and even Sub[] whose leaf is a host class
        // extending Component came out as Object[]. Method dispatch on the
        // JVM rejects a plain Object[] passed to a `Component[]` (or
        // `Object[][]`) slot with an argument-type mismatch, so materialise
        // the array here and copy the (already peer-converted) elements
        // across. Class[] is handled above; a 1D Object[] parameter needs
        // no conversion. Anything else with `[L...;` or `[[...` gets a
        // typed array, recursively for multi-dimensional cases so covariance
        // through the outer type reaches the innermost element type too.
        Vector hostArrayPairs = null;
        // Two-pass: collect every slot that needs a typed array, then per
        // source pick the most specific type covering all its aliases, and
        // materialise once. A single call site can pass the same `Button[]`
        // as both `Component[]` and `Button[]`; Java hands the host one
        // array whose covariance covers both slots, and two independently
        // typed dsts (one per parameter type) would break that alias --
        // `first != second`, writes through one invisible through the
        // other. Picking `Button[]` at the narrowest end satisfies both
        // parameters at once because a Button[] passes reflection's check
        // for Component[] via array covariance.
        Vector arrayIntents = null;   // triples (Integer argIndex, Object[] src, String elementDesc)
        for (int i = 0; i < args.length && i < params.length; i++) {
            if (!(args[i] instanceof Object[])) {
                continue;
            }
            String p = params[i];
            if (!p.startsWith("[")) {
                continue;
            }
            if ("[Ljava/lang/Object;".equals(p)
                    || "[Ljava/lang/Class;".equals(p)) {
                continue;
            }
            String elementDesc = p.substring(1);
            // Primitive-leaf arrays (`[I`, `[[D`) can never present as
            // Object[] here -- they are int[], double[], etc. The rank-2
            // form `[[I` is a `[I[]` whose outer *is* Object[] though, so
            // only reject when the element descriptor is a primitive.
            if (!elementDesc.startsWith("L") && !elementDesc.startsWith("[")) {
                continue;
            }
            Object[] src = (Object[]) args[i];
            // Skip when the host already handed us a typed array (a
            // `String[]` returned by an earlier call). Only the interpreter's
            // own `ANEWARRAY` produces exactly plain `Object[]`; anything
            // typed enough for the host to accept is already assignable.
            if (!"[Ljava.lang.Object;".equals(src.getClass().getName())) {
                continue;
            }
            if (arrayIntents == null) {
                arrayIntents = new Vector();
            }
            arrayIntents.addElement(Integer.valueOf(i));
            arrayIntents.addElement(src);
            arrayIntents.addElement(elementDesc);
        }
        if (arrayIntents != null) {
            hostArrayPairs = new Vector();
            Vector handledSrcs = new Vector();
            for (int n = 0; n < arrayIntents.size(); n += 3) {
                Object[] src = (Object[]) arrayIntents.elementAt(n + 1);
                if (containsIdentity(handledSrcs, src)) {
                    continue;
                }
                handledSrcs.addElement(src);
                String bestDesc = (String) arrayIntents.elementAt(n + 2);
                for (int m = n + 3; m < arrayIntents.size(); m += 3) {
                    if (arrayIntents.elementAt(m + 1) != src) {   //NOPMD CompareObjectsWithEquals - identity is the point
                        continue;
                    }
                    String cand = (String) arrayIntents.elementAt(m + 2);
                    bestDesc = moreSpecificElement(bestDesc, cand);
                }
                Object[] dst = materializeTypedArray(src, bestDesc, hostArrayPairs);
                if (dst == null) {
                    continue;
                }
                if (!containsMaterialisation(hostArrayPairs, src, bestDesc)) {
                    hostArrayPairs.addElement(src);
                    hostArrayPairs.addElement(dst);
                    hostArrayPairs.addElement(bestDesc);
                }
                for (int m = n; m < arrayIntents.size(); m += 3) {
                    if (arrayIntents.elementAt(m + 1) != src) {   //NOPMD CompareObjectsWithEquals - identity is the point
                        continue;
                    }
                    int argIdx = ((Integer) arrayIntents.elementAt(m)).intValue();
                    args[argIdx] = dst;
                }
            }
        }
        st.hostCallDepth++;
        // When this is the outermost host call, the wall clock it spends is not
        // the program's to answer for: invokeAndBlock, a network read or a
        // dialog can sit for seconds, and the budget is about interpreted code
        // that never yields. Suppressing the check for the duration is not
        // enough -- the entry clock keeps running, so the first checkpoint
        // after a long call trips on time the host spent. The clock is moved
        // forward by that interval instead.
        long hostCallStart = st.hostCallDepth == 1 ? System.currentTimeMillis() : 0;
        try {
            if (target == null) {
                if (hostInterceptor != null) {
                    Object answer = hostInterceptor.interceptStatic(owner, name, desc, args);
                    if (answer != InterpHostInterceptor.NOT_INTERCEPTED) {  //NOPMD CompareObjectsWithEquals - a sentinel
                        return answer;
                    }
                }
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
            Failure previous = lastFailure;
            if (previous == null || previous.thrown != t) {  //NOPMD CompareObjectsWithEquals - the same throwable instance, not an equal one
                lastFailure = new Failure(t, t, snapshotStack(),
                        owner.replace('/', '.') + "." + name + desc);
            }
            throw t;
        } finally {
            // No fromHostElements sweep: the peers stay in place so a host
            // method that retained the array (Arrays.asList, Collections.
            // addAll, an executor's task queue) keeps its host-compatible
            // view. Interpreted reads round-trip via `fromHost` at AALOAD.
            // Class[] parameters: mirror the host's writes on the materialised
            // Class[] back to the interpreter-owned Object[] so the caller
            // sees mutations, matching Java's array-by-reference semantics.
            //
            // Every slot is copied unconditionally: a "did the host write this
            // slot?" test based on post-call identity can't distinguish "host
            // was read-only" from "host explicitly assigned the same value we
            // passed in" (which for a pushed-only token materialised through
            // `hostClassFor` to Object.class is `Object.class` on both sides).
            // Copying always is the honest answer -- a pushed-only class was
            // never a real host Class to begin with, so an InterpClass token
            // that survives a host call and equals its ancestor stand-in is
            // no more informative than the ancestor itself. Callers that need
            // to compare against the original token afterwards should keep
            // their own reference rather than re-read the array slot.
            if (classArrayPairs != null) {
                for (int i = 0; i < classArrayPairs.size(); i += 2) {
                    Object[] src = (Object[]) classArrayPairs.elementAt(i);
                    Object[] dst = (Object[]) classArrayPairs.elementAt(i + 1);
                    int len = src.length < dst.length ? src.length : dst.length;
                    for (int k = 0; k < len; k++) {
                        src[k] = dst[k];
                    }
                }
            }
            // Same reasoning as classArrayPairs above: a host method that
            // reorders or fills its `Component[]` needs those writes visible
            // through the interpreter's original array. Copy peers back into
            // the src; AALOAD's fromHost hop turns them back into wrappers
            // for interpreted reads.
            if (hostArrayPairs != null) {
                for (int i = 0; i < hostArrayPairs.size(); i += 3) {
                    Object[] src = (Object[]) hostArrayPairs.elementAt(i);
                    Object[] dst = (Object[]) hostArrayPairs.elementAt(i + 1);
                    int len = src.length < dst.length ? src.length : dst.length;
                    for (int k = 0; k < len; k++) {
                        src[k] = dst[k];
                    }
                }
            }
            st.hostCallDepth--;
            if (st.hostCallDepth == 0 && st.runStartMs > 0) {
                st.runStartMs += System.currentTimeMillis() - hostCallStart;
            }
        }
    }

    /// Replaces peer-backed elements of a reference array with their peers.
    ///
    /// In place, and recursively for nested arrays, because the array itself is
    /// the value being passed: a copy would lose whatever the host method did
    /// to it. The seen list is what makes a self-referencing array terminate.
    private static void toHostElements(Object array, Vector seen) {
        if (!(array instanceof Object[]) || seen.contains(array)) {
            return;
        }
        seen.addElement(array);
        Object[] a = (Object[]) array;
        for (int i = 0; i < a.length; i++) {
            if (a[i] instanceof InterpObject) {
                InterpObject io = (InterpObject) a[i];
                if (io.hostPeer != null) {
                    a[i] = io.hostPeer;
                }
            } else if (a[i] instanceof Object[]) {
                toHostElements(a[i], seen);
            }
        }
    }

    // `fromHostElements` used to sweep peer-populated arrays back to wrappers
    // after the host call returned. Removed because host methods like
    // `Arrays.asList` and `Collections.addAll` retain the array they were
    // handed; reverting in place would leave the host's alias holding
    // InterpObject wrappers that do not implement the interfaces their peers
    // do. AALOAD converts elements through `fromHost` on read, so both sides
    // see the representation they expect.

    /// The simple name of a class, as `Class.getSimpleName` reports it.
    ///
    /// Read from the bundle rather than worked out from the binary name, which
    /// cannot be done: `Outer$1` is anonymous and has no simple name at all,
    /// `Outer$1Local` is a local class called Local, and a `$` may equally be
    /// part of a class's own identifier -- nested or top-level. javac records
    /// which in the InnerClasses attribute, and the bundle now carries it.
    private static String simpleNameOf(InterpClass c) {
        if (c.simpleName != null) {
            return c.simpleName;
        }
        // No entry in the attribute at all: a top-level class, whose simple
        // name is the last segment of its binary name -- including any `$` it
        // carries, which belongs to the class's own identifier.
        String name = c.getName();
        int slash = name.lastIndexOf('/');
        return slash < 0 ? name : name.substring(slash + 1);
    }

    /// Selects the method an `invokevirtual` actually runs.
    ///
    /// Not simply the receiver's, for two reasons the opcode does not express.
    /// A *private* method is not virtual at all, and from JDK 11 javac emits
    /// invokevirtual for one (nestmates replaced the synthetic bridges). A
    /// *package-private* method is overridden only from within its own package
    /// -- JVMS 5.4.5 -- so a public method of the same signature in another
    /// package does not replace it, and the call still runs the one that was
    /// written.
    private InterpMethod resolveVirtual(InterpClass receiver, String owner,
                                        String name, String desc) {
        InterpClass named = bundle.findClass(owner);
        InterpMethod declared = named == null ? null : named.declaredMethod(name, desc);
        if (declared == null) {
            // JVMS 5.4.3.3: superclass class methods win over interface
            // defaults, and if the superclass method is inaccessible from the
            // receiver (a package-private method in a different package) the
            // resolution is an IllegalAccessError -- not a silent fallback to
            // the interface default. The vtable filter earlier drops the
            // inaccessible entry so subsequent virtual dispatch cannot land
            // on it, but that filter would also let this path pick the
            // interface's default and execute a method the JVM refuses.
            // Walk the superclass chain here and raise the linkage error
            // when appropriate.
            InterpMethod superMethod = findClassMethodInSuperchain(receiver, name, desc);
            if (superMethod != null && isPackagePrivate(superMethod)
                    && !samePackage(receiver.getName(), superMethod.owner.getName())) {
                // IncompatibleClassChangeError rather than the strict
                // IllegalAccessError, for the same reason the abstract-method
                // path uses it: the CLDC11 subset does not carry
                // IllegalAccessError, and a message naming the inaccessible
                // method and the wrong package is worth more here than the
                // exactly right type. IllegalAccessError is a subtype of
                // IncompatibleClassChangeError on the JVM, so a `catch
                // (IncompatibleClassChangeError)` in pushed code sees the
                // same shape either way.
                throw new InterpThrowable(new IncompatibleClassChangeError(
                        superMethod.owner.getName().replace('/', '.') + "." + name + desc
                        + " is package-private and " + receiver.getName().replace('/', '.')
                        + " is in a different package"), snapshotStack());
            }
            return receiver.resolve(name, desc);
        }
        if (declared.isPrivate()) {
            return declared;
        }
        if (!isPackagePrivate(declared)) {
            return receiver.resolve(name, desc);
        }
        // The most derived class that may override it: one in the same package
        // as the declaring class. Anything nearer the receiver but outside that
        // package declares a different method that happens to share a name.
        for (InterpClass k = receiver; k != null && k != named; k = k.superInterp) {  //NOPMD CompareObjectsWithEquals - one class object, not an equal one
            InterpMethod m = k.declaredMethod(name, desc);
            if (m != null && !m.isPrivate() && samePackage(k.getName(), named.getName())) {
                return m;
            }
        }
        return declared;
    }

    /// Adds an interface name to `interfaces` if the linker can resolve it and
    /// the list does not already have it. Used to attach `Enum`-inherited
    /// interfaces (Comparable, Serializable) to an enum's peer without a
    /// second copy when the pushed class also happens to declare one.
    private void addIfPresent(Vector interfaces, String name) {
        if (!interfaces.contains(name) && linker.findClass(name) != null) {
            interfaces.addElement(name);
        }
    }

    /// The nearest class-declared method up {@code receiver}'s interpreted
    /// superclass chain (skipping the receiver itself), regardless of
    /// visibility. Used to detect an inaccessible superclass method during
    /// virtual resolution -- JVMS 5.4.3.3 makes that an IllegalAccessError,
    /// not a fallback to an interface default.
    private static InterpMethod findClassMethodInSuperchain(InterpClass receiver,
                                                             String name, String desc) {
        InterpClass k = receiver.superInterp;
        while (k != null) {
            InterpMethod m = k.declaredMethod(name, desc);
            if (m != null && !m.isStatic()) {
                return m;
            }
            k = k.superInterp;
        }
        return null;
    }

    /// Whether a method is package-private: none of public, protected, private.
    private static boolean isPackagePrivate(InterpMethod m) {
        return !m.isPrivate() && !m.isPublic() && !m.isProtected();
    }

    /// Whether two JVM internal names sit in the same package.
    private static boolean samePackage(String a, String b) {
        int i = a.lastIndexOf('/');
        int j = b.lastIndexOf('/');
        if (i != j) {
            return false;
        }
        return i < 0 || a.regionMatches(0, b, 0, i);
    }

    /// Whether a host class is a supertype of an interpreted one.
    ///
    /// The interpreted class itself is nothing to the host, but its supertypes
    /// are: the host class it extends and every host interface it declares,
    /// transitively. If the receiver is assignable from any of them it is
    /// assignable from the pushed class, which is exactly what a program asking
    /// `Runnable.class.isAssignableFrom(Task.class)` wants to know.
    private boolean assignableFromInterp(Object hostClass, InterpClass c) throws Throwable {
        if (c.isArray()) {
            return assignableFromInterpArray(hostClass, c);
        }
        Vector externs = new Vector();
        c.collectHostSupertypes(externs);
        for (int i = 0; i < externs.size(); i++) {
            // The cast is outside the try on purpose: a catch of Throwable
            // around it would be a handler for a failed cast, which ParparVM
            // never raises -- see check-cast-semantics.
            int ext = ((Integer) externs.elementAt(i)).intValue();
            Object supertype;
            try {
                supertype = resolveExternClass(ext);
            } catch (Throwable absent) {
                // A supertype the installed app does not have cannot make
                // anything assignable to the receiver, and asking a type
                // question is not a reason to raise NoClassDefFoundError.
                continue;
            }
            Object answer = linker.invokeVirtual(hostClass, "java/lang/Class",
                    "isAssignableFrom", "(Ljava/lang/Class;)Z",
                    new Object[] {linker.classObject(supertype)});
            if (answer instanceof Boolean && ((Boolean) answer).booleanValue()) {
                return true;
            }
        }
        return false;
    }

    /// Whether a host class is a supertype of an interpreted array type.
    ///
    /// An array token records only its component, so the walk above finds no
    /// supertypes at all for one -- and yet every array in Java is an Object, a
    /// Cloneable and a Serializable, and `Base[]` is an `S[]` for every host
    /// supertype S of Base. Both are ordinary things to ask.
    private boolean assignableFromInterpArray(Object hostClass, InterpClass c) throws Throwable {
        if (assignableToHostNamed(hostClass, "java/lang/Object")
                || assignableToHostNamed(hostClass, "java/lang/Cloneable")
                || assignableToHostNamed(hostClass, "java/io/Serializable")) {
            return true;
        }
        String name = c.getName();
        int rank = 0;
        while (rank < name.length() && name.charAt(rank) == '[') {
            rank++;
        }
        InterpClass leaf = c;
        while (leaf.isArray()) {
            leaf = leaf.arrayComponent;
        }
        // A multi-dimensional array's intermediate components are themselves
        // arrays, and every array is Object/Cloneable/Serializable: `Pushed[][]`
        // is a `Cloneable[]` because its `Pushed[]` component is a Cloneable.
        // Check each intermediate rank against the marker interfaces so a
        // hostClass of `Cloneable[]` or `Object[][]` on a rank-3 receiver still
        // resolves to true. Rank 0 is the first block above; the leaf-supertype
        // loop below covers the full rank.
        for (int k = 1; k < rank; k++) {
            StringBuilder mid = new StringBuilder();
            for (int i = 0; i < k; i++) {
                mid.append('[');
            }
            if (assignableToHostNamed(hostClass, mid + "Ljava/lang/Object;")
                    || assignableToHostNamed(hostClass, mid + "Ljava/lang/Cloneable;")
                    || assignableToHostNamed(hostClass, mid + "Ljava/io/Serializable;")) {
                return true;
            }
        }
        StringBuilder brackets = new StringBuilder();
        for (int i = 0; i < rank; i++) {
            brackets.append('[');
        }
        Vector externs = new Vector();
        leaf.collectHostSupertypes(externs);
        for (int i = 0; i < externs.size(); i++) {
            String supertype = externOwnerName(((Integer) externs.elementAt(i)).intValue());
            if (assignableToHostNamed(hostClass, brackets + "L" + supertype + ";")) {
                return true;
            }
        }
        return false;
    }

    /// Whether the host class is assignable from the host type of this name,
    /// answering false when the installed app does not have that type.
    private boolean assignableToHostNamed(Object hostClass, String internalName)
            throws Throwable {
        Object other = linker.findClass(internalName);
        if (other == null) {
            return false;
        }
        Object answer = linker.invokeVirtual(hostClass, "java/lang/Class", "isAssignableFrom",
                "(Ljava/lang/Class;)Z", new Object[] {linker.classObject(other)});
        return answer instanceof Boolean && ((Boolean) answer).booleanValue();
    }

    /// The parameter descriptors of a method descriptor, in order.
    ///
    /// `(ILjava/lang/String;[J)V` is `I`, `Ljava/lang/String;`, `[J` -- one
    /// entry per argument, so the result lines up with the argument array.
    private static String[] paramDescriptors(String desc) {
        Vector out = new Vector();
        int i = desc.indexOf('(') + 1;
        int end = desc.indexOf(')');
        if (i <= 0 || end < i) {
            return new String[0];
        }
        while (i < end) {
            int start = i;
            while (i < end && desc.charAt(i) == '[') {
                i++;
            }
            if (i < end && desc.charAt(i) == 'L') {
                int semi = desc.indexOf(';', i);
                if (semi < 0 || semi > end) {
                    // A descriptor this malformed cannot be walked; the caller
                    // simply does not substitute, which is the safe direction.
                    return new String[0];
                }
                i = semi + 1;
            } else {
                i++;
            }
            out.addElement(desc.substring(start, i));
        }
        String[] answer = new String[out.size()];
        out.copyInto(answer);
        return answer;
    }

    /// A real host array of the requested element type, built from an
    /// interpreter-owned Object[]. Elements are copied as-is (the caller has
    /// already replaced peer-backed values with their peers), and a nested
    /// plain `Object[]` recurses so multi-dimensional parameters
    /// (`Component[][]`, `Object[][]`) also arrive as the exact host array
    /// class the JVM will accept through reflection. A `Ljava/lang/Class;`
    /// leaf routes each `InterpClass` token through {@link #hostClassFor},
    /// mirroring the 1D `Class[]` conversion so a nested `Class[][]`
    /// containing a pushed class literal doesn't hit `ArrayStoreException`
    /// on the leaf assignment. Returns null when the linker cannot produce
    /// an array of this type at all -- the caller falls back to the untyped
    /// array in that case.
    ///
    /// `innerPairs` -- when non-null -- collects (src, dst) for every
    /// nested substitution so the caller's finally block can mirror host
    /// writes on the inner arrays back through the interpreter's original
    /// aliases (`Component[] row = matrix[0]` still sees the reorder).
    private Object[] materializeTypedArray(Object[] src, String elementDescriptor,
                                           Vector innerPairs) throws Throwable {
        // A nested source array may appear more than once -- the matrix
        // `[[row, row]]` where both outer slots are the same inner array,
        // or the same Component[] passed both as its own argument and
        // reachable through another. Materialising each occurrence
        // independently gives the host two dst arrays for one src, and
        // any host write through the second is overwritten again when
        // the finally-block mirror copies both back onto src in turn.
        // Reuse an existing dst -- but only when the earlier
        // materialisation targeted the *same* element type. Reusing a
        // `Component[]` dst for a `Button[]` slot would fail the
        // reflective linker's argument-type check because Component[] is
        // not assignable to Button[]; different requested types get their
        // own dst arrays, each mirrored back independently.
        Object[] existing = existingMaterialisation(innerPairs, src, elementDescriptor);
        if (existing != null) {
            return existing;
        }
        Object array = linker.newArray(elementDescriptor, src.length);
        if (!(array instanceof Object[])) {
            return null;
        }
        Object[] dst = (Object[]) array;
        boolean nested = elementDescriptor.startsWith("[");
        String innerDesc = nested ? elementDescriptor.substring(1) : null;
        boolean classLeaf = !nested && "Ljava/lang/Class;".equals(elementDescriptor);
        for (int j = 0; j < src.length; j++) {
            Object el = src[j];
            if (nested && el instanceof Object[]
                    && "[Ljava.lang.Object;".equals(el.getClass().getName())
                    && (innerDesc.startsWith("L") || innerDesc.startsWith("["))) {
                Object[] inner = materializeTypedArray((Object[]) el, innerDesc, innerPairs);
                if (inner != null) {
                    dst[j] = inner;
                    if (innerPairs != null
                            && !containsMaterialisation(innerPairs, el, innerDesc)) {
                        innerPairs.addElement(el);
                        innerPairs.addElement(inner);
                        innerPairs.addElement(innerDesc);
                    }
                } else {
                    dst[j] = el;
                }
            } else if (classLeaf && el instanceof InterpClass) {
                dst[j] = hostClassFor((InterpClass) el);
            } else {
                dst[j] = el;
            }
        }
        return dst;
    }

    /// `pairs` is a flat list of (src, dst, elementDescriptor) triples --
    /// the descriptor is what makes a `Component[]` materialisation
    /// distinguishable from a `Button[]` materialisation of the same src,
    /// so the reflective linker doesn't reject the narrower slot with a
    /// stale wider dst.
    private static Object[] existingMaterialisation(Vector pairs, Object src,
                                                    String elementDescriptor) {
        if (pairs == null) {
            return null;
        }
        for (int k = 0; k < pairs.size(); k += 3) {
            if (pairs.elementAt(k) == src   //NOPMD CompareObjectsWithEquals - identity is the point
                    && elementDescriptor.equals(pairs.elementAt(k + 2))) {
                return (Object[]) pairs.elementAt(k + 1);
            }
        }
        return null;
    }

    private static boolean containsMaterialisation(Vector pairs, Object src,
                                                   String elementDescriptor) {
        return existingMaterialisation(pairs, src, elementDescriptor) != null;
    }

    private static boolean containsIdentity(Vector items, Object o) {
        for (int i = 0; i < items.size(); i++) {
            if (items.elementAt(i) == o) {   //NOPMD CompareObjectsWithEquals - identity is the point
                return true;
            }
        }
        return false;
    }

    /// The more specific of two array element descriptors -- `a` if it is
    /// (transitively) a subtype of `b`, `b` if the reverse holds, and `a`
    /// arbitrarily when the two are unrelated siblings. "More specific"
    /// means the type whose array class satisfies both parameter slots via
    /// Java's array covariance, so aliased Component[] and Button[] slots
    /// of the same pushed Button[] can share a single Button[] dst and
    /// keep alias identity intact.
    private String moreSpecificElement(String a, String b) throws Throwable {
        if (a.equals(b)) {
            return a;
        }
        Object aArrayClass = linker.findClass("[" + a);
        Object bArrayClass = linker.findClass("[" + b);
        if (aArrayClass == null || bArrayClass == null) {
            return a;
        }
        // A zero-length dummy of each type is cheap; the reflective
        // isInstance check answers whether one array class is assignable
        // to the other -- which is exactly the covariance rule Java uses
        // to accept a wider array parameter.
        Object bDummy = linker.newArray(b, 0);
        if (linker.isInstance(aArrayClass, bDummy)) {
            return b;   // b's array is-a a's array, so b is more specific
        }
        Object aDummy = linker.newArray(a, 0);
        if (linker.isInstance(bArrayClass, aDummy)) {
            return a;   // a's array is-a b's array, so a is more specific
        }
        // Unrelated (siblings that share only Object as an ancestor);
        // pick one arbitrarily. The host will reject the mismatched slot
        // -- but that mismatch is present in the pushed program, not
        // introduced here.
        return a;
    }

    /// A real `Class[]` built from an interpreter-owned Object[] used to
    /// stand in for one. Elements go through {@link #hostClassFor}, so a
    /// pushed-only leaf resolves to its nearest host ancestor -- matching how
    /// a scalar `Class` argument is converted; a real host Class element is
    /// left as-is. Returns null when the linker cannot produce a Class[] at
    /// all, letting the caller keep the Object[] rather than fail loudly for
    /// a host method it turns out never to have needed one.
    private Object[] classArrayFor(Object[] src) throws Throwable {
        Object array = linker.newArray("Ljava/lang/Class;", src.length);
        if (!(array instanceof Object[])) {
            return null;
        }
        Object[] dst = (Object[]) array;
        for (int i = 0; i < src.length; i++) {
            Object element = src[i];
            if (element instanceof InterpClass) {
                dst[i] = hostClassFor((InterpClass) element);
            } else {
                dst[i] = element;
            }
        }
        return dst;
    }

    /// The host class standing in for an interpreted one: the nearest ancestor
    /// the installed app actually has, or `java.lang.Object`.
    ///
    /// There is no host class for a type that exists only in the bundle, and
    /// nothing can conjure one. What the callers of this actually want is a
    /// class loader and an identity in the app -- which the nearest host
    /// ancestor provides.
    private Object hostClassFor(InterpClass c) throws Throwable {
        InterpClass k = c;
        while (k != null) {
            if (k.superExtern >= 0) {
                // classObject, not the resolved handle: on iOS a resolved class
                // is its numeric id, and a host method taking a Class wants the
                // Class. Handing over the id makes the resource idiom fail on
                // the platform it was most needed on.
                return linker.classObject(resolveExternClass(k.superExtern));
            }
            k = k.superInterp;
        }
        Object object = linker.findClass("java/lang/Object");
        return object == null ? null : linker.classObject(object);
    }

    private void replaceOnStack(InterpFrame f, Object placeholder, Object created) {
        for (int i = 0; i < f.sp; i++) {
            if (f.stackRefs[i] == placeholder) {  //NOPMD CompareObjectsWithEquals - a placeholder sentinel
                f.stackRefs[i] = created;
            }
        }
        for (int i = 0; i < f.refs.length; i++) {
            if (f.refs[i] == placeholder) {  //NOPMD CompareObjectsWithEquals - a placeholder sentinel
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
        if ("isArray".equals(name) && args.length == 0) {
            return c.isArray() ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("getComponentType".equals(name) && args.length == 0) {
            return c.arrayComponent;
        }
        if ("getSimpleName".equals(name) && args.length == 0 && c.isArray()) {
            return classCall(c.arrayComponent, "getSimpleName", args) + "[]";
        }
        if ("getSimpleName".equals(name) && args.length == 0) {
            return simpleNameOf(c);
        }
        if ("toString".equals(name) && args.length == 0) {
            return (c.isInterface() ? "interface " : "class ")
                    + c.getName().replace('/', '.');
        }
        if ("hashCode".equals(name) && args.length == 0) {
            return Integer.valueOf(System.identityHashCode(c));
        }
        if ("equals".equals(name) && args.length == 1) {
            return args[0] == c ? Boolean.TRUE : Boolean.FALSE;  //NOPMD CompareObjectsWithEquals - Class identity is reference identity
        }
        if ("isInterface".equals(name) && args.length == 0) {
            return c.isInterface() ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("desiredAssertionStatus".equals(name) && args.length == 0) {
            // Not a curiosity: javac compiles an `assert` into a <clinit> that
            // reads ThisClass.class.desiredAssertionStatus() into a synthetic
            // $assertionsDisabled field, so without an answer here a class
            // containing one assert fails to initialize and the push dies
            // before the program runs. False is also what the device says --
            // java.lang.Class on ParparVM returns false unconditionally -- so
            // an assert is inert here exactly as it is in a built app.
            return Boolean.FALSE;
        }
        if ("isInstance".equals(name) && args.length == 1) {
            return isInstanceOf(args[0], c.getName()) ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("isAssignableFrom".equals(name) && args.length == 1) {
            // The other type test Java offers without reflection, and the
            // hierarchy to answer it with is already here. A host class is
            // never a subtype of one only the bundle has, so anything that is
            // not an interpreted token answers false.
            if (args[0] == null) {
                throw new InterpThrowable(new NullPointerException(
                        "isAssignableFrom(null)"), snapshotStack());
            }
            if (!(args[0] instanceof InterpClass)) {
                return Boolean.FALSE;
            }
            InterpClass other = (InterpClass) args[0];
            if (c.isArray() || other.isArray()) {
                // Array covariance follows the components; an array is
                // assignable to nothing else here but itself.
                return c.isArray() && other.isArray()
                        && Boolean.TRUE.equals(classCall(c.arrayComponent, "isAssignableFrom",
                                new Object[] {other.arrayComponent}))
                        ? Boolean.TRUE : Boolean.FALSE;
            }
            return other.isSubclassOfInterp(c.getName()) ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("getResourceAsStream".equals(name) && args.length == 1
                && args[0] instanceof String) {
            // Java resolves a relative resource name against the *caller's*
            // package -- `MyApp.class.getResourceAsStream("data.json")` reads
            // `/com/example/data.json` -- and the bundle carries resources
            // under exactly that path. Look them up here so the pushed
            // program's own `theme.res`, JSON blobs and images reach the
            // ordinary Class.getResourceAsStream idiom; a class-token receiver
            // never reached the host path that would fall back to
            // `localResource`, so the resource looked absent.
            String path = (String) args[0];
            if (path.length() > 0 && path.charAt(0) != '/') {
                String owner = c.getName();
                int slash = owner.lastIndexOf('/');
                path = slash < 0 ? "/" + path : "/" + owner.substring(0, slash + 1) + path;
            }
            byte[] data = (byte[]) bundle.getResources().get(path);
            if (data == null && path.startsWith("/")) {
                // Some resources are stored without the leading slash --
                // published verbatim by the caller. Try that spelling too
                // rather than answering null for a resource that is present.
                data = (byte[]) bundle.getResources().get(path.substring(1));
            }
            return data == null ? null : new java.io.ByteArrayInputStream(data);
        }
        if ("getSuperclass".equals(name) && args.length == 0) {
            if (c.isArray()) {
                // Every array class reports Object, whatever its component is.
                Object object = linker.findClass("java/lang/Object");
                return object == null ? null : linker.classObject(object);
            }
            if (c.superInterp != null) {
                return c.superInterp;
            }
            // Null only for an interface and for Object itself. Every other
            // interpreted class has a host parent -- Form, or Object -- and
            // answering null there says the class has no superclass at all.
            if (c.isInterface() || c.superExtern < 0) {
                return null;
            }
            // classObject, because on iOS a resolved class is a numeric handle
            // and the caller is about to treat this as a java.lang.Class.
            return linker.classObject(resolveExternClass(c.superExtern));
        }
        return NOT_CLASS_METHOD;
    }

    private Object objectCall(InterpObject io, String name, Object[] args, boolean special)
            throws Throwable {
        if ("getClass".equals(name) && args.length == 0) {
            // The interpreted class is its own class object: there is no host
            // class to hand back, and the bundle is what knows the type.
            return io.type;
        }
        if ("hashCode".equals(name) && args.length == 0) {
            return Integer.valueOf(System.identityHashCode(io));
        }
        if ("equals".equals(name) && args.length == 1) {
            // The argument came off the interpreter's stack through
            // `popBoxed`, which converts a peer-backed object back to the
            // InterpObject it stands for -- but a peer that was set on an
            // InterpObject with an interface-only shim would arrive here as
            // the peer, not the wrapper, when the caller passed `this`. A
            // plain `args[0] == io` would answer false for `value.equals(value)`.
            // Compare identity through `fromHost` so both representations
            // resolve to the same InterpObject.
            Object other = fromHost(args[0]);
            return other == io ? Boolean.TRUE : Boolean.FALSE;  //NOPMD CompareObjectsWithEquals - Object.equals default is identity
        }
        if ("toString".equals(name) && args.length == 0) {
            if (special) {
                // `super.toString()` from an interpreted override. Calling
                // io.toString() would dispatch straight back into that
                // override: an infinite recursion reported as a stack
                // overflow, in code that reads as ordinary Java.
                return io.type.getName().replace('/', '.') + "@"
                        + Integer.toHexString(System.identityHashCode(io));
            }
            return io.toString();
        }
        // wait/notify on an object of a pushed-only class. The wrapper is a
        // real Java object with a real monitor, and the interpreter locks that
        // same wrapper for a synchronized block on a peerless object -- so
        // producer/consumer code written against `new CustomLock()` works
        // rather than dying on "not implemented".
        if ("wait".equals(name)) {
            if (args.length == 0) {
                io.wait();
            } else if (args.length == 1) {
                io.wait(((Long) args[0]).longValue());
            } else {
                io.wait(((Long) args[0]).longValue(), ((Integer) args[1]).intValue());
            }
            return null;
        }
        if ("notify".equals(name) && args.length == 0) {
            // notify(), because that is the method the program called.
            // Substituting notifyAll() would be the runtime quietly changing
            // what the pushed code asked for.
            io.notify();  //NOPMD UseNotifyAllInsteadOfNotify - implementing Object.notify
            return null;
        }
        if ("notifyAll".equals(name) && args.length == 0) {
            io.notifyAll();
            return null;
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

        @Override
        public String toString() {
            return iface.getName().replace('/', '.') + "(unsupported on this runtime)";
        }
    }

    /// Names the thing an enum was asked to compare itself to.
    private static String describeForCompare(Object other) {
        if (other == null) {
            return "null";
        }
        if (other instanceof InterpObject) {
            return ((InterpObject) other).type.getName().replace('/', '.');
        }
        return other.getClass().getName();
    }

    /// Whether two constants belong to the same enum, looking through the
    /// anonymous subclass a constant with a class body gets.
    private static boolean sameEnum(InterpObject a, InterpObject b) {
        return declaringEnum(a) == declaringEnum(b);  //NOPMD CompareObjectsWithEquals - one class object
    }

    private static InterpClass declaringEnum(InterpObject o) {
        InterpClass k = o.type;
        while (k != null && k.superInterp != null && k.getName().indexOf('$') > 0) {
            k = k.superInterp;
        }
        return k;
    }

    /// java.lang.Enum's behaviour for an interpreted enum constant.
    ///
    /// Only the methods a constant inherits without overriding reach here;
    /// anything the enum declares itself was already resolved and run.
    private Object enumCall(InterpObject io, String name, Object[] args) {
        if ("name".equals(name) || "toString".equals(name)) {
            return io.enumName;
        }
        if ("ordinal".equals(name)) {
            return Integer.valueOf(io.enumOrdinal);
        }
        if ("getDeclaringClass".equals(name)) {
            // The enum type, not the constant's own class: a constant with a
            // class body is an anonymous subclass, and Java's contract is that
            // every constant of an enum answers with the enum.
            InterpClass k = io.type;
            while (k != null && k.superInterp != null && k.getName().indexOf('$') > 0) {
                k = k.superInterp;
            }
            return k;
        }
        if ("equals".equals(name) && args.length == 1) {
            // Enum identity is object identity: constants are singletons.
            // popBoxed converted a peer-backed argument to the peer, so a
            // self-equals for an interface-only-peer enum arrived here as
            // the peer against the InterpObject wrapper; normalise through
            // fromHost so both representations resolve to the same object.
            Object other = fromHost(args[0]);
            return other == io ? Boolean.TRUE : Boolean.FALSE;  //NOPMD CompareObjectsWithEquals - Object.equals default is identity
        }
        if ("hashCode".equals(name)) {
            return Integer.valueOf(System.identityHashCode(io));
        }
        if ("compareTo".equals(name) && args.length == 1) {
            // Java compares constants of one enum and throws otherwise --
            // including for null and for something that is not an enum at all,
            // which a raw Comparable call can supply. Returning an ordering
            // instead quietly corrupts any sorted collection the two ended up
            // in together. `fromHost` normalises for the same reason as
            // `equals` above: a peer-backed enum arrives as its peer.
            Object other = fromHost(args[0]);
            if (other == null) {
                // Enum.compareTo(null) is a NullPointerException, not a
                // ClassCastException: code that tells the two apart is code
                // that would see the difference.
                throw new NullPointerException("compareTo(null)");
            }
            boolean comparable = other instanceof InterpObject
                    && ((InterpObject) other).enumOrdinal >= 0
                    && sameEnum(io, (InterpObject) other);
            if (!comparable) {
                throw new ClassCastException(describeForCompare(other)
                        + " is not comparable to " + io.type.getName().replace('/', '.'));
            }
            return Integer.valueOf(io.enumOrdinal - ((InterpObject) other).enumOrdinal);
        }
        return NOT_ENUM_METHOD;
    }

    /// Enum.valueOf over a bundle class, by scanning the constants the
    /// class's static initializer already built.
    private Object enumValueOf(InterpClass c, String name) throws Throwable {
        ensureInitialized(c);
        Object values = c.staticValue("$VALUES");
        if (values instanceof Object[]) {
            Object[] a = (Object[]) values;
            for (Object raw : a) {
                // `$VALUES` is the interpreter's own Object[]. Its elements
                // are the peers each enum constant was stored as; walk back to
                // the InterpObject the peer stands for before matching.
                Object constant = fromHost(raw);
                if (constant instanceof InterpObject
                        && name.equals(((InterpObject) constant).enumName)) {
                    return constant;
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
    private Object copyArray(Object a) throws Throwable {
        if (a instanceof Object[]) {
            Object[] s = (Object[]) a;
            // A String[] must clone to a String[]. The interpreter's own
            // reference arrays are Object[] and stay that way, but a host array
            // arriving here carries a real component type, and a copy that lost
            // it fails the moment it is handed back to a host method.
            Object[] d = (Object[]) linker.cloneArray(s);
            if (d == null) {
                d = new Object[s.length];
            }
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof int[]) {
            int[] s = (int[]) a;
            int[] d = new int[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof byte[]) {
            byte[] s = (byte[]) a;
            byte[] d = new byte[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof char[]) {
            char[] s = (char[]) a;
            char[] d = new char[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof short[]) {
            short[] s = (short[]) a;
            short[] d = new short[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof long[]) {
            long[] s = (long[]) a;
            long[] d = new long[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof float[]) {
            float[] s = (float[]) a;
            float[] d = new float[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        if (a instanceof double[]) {
            double[] s = (double[]) a;
            double[] d = new double[s.length];
            System.arraycopy(s, 0, d, 0, s.length);
            return d;
        }
        boolean[] s = (boolean[]) a;
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
            v = ((InterpBacked) v).getInterpObject();
        }
        if (v instanceof NativeStub) {
            // The cast the NativeLookup idiom always performs. The stub stands
            // in for the interface it was asked for, and for NativeInterface
            // above it.
            InterpClass iface = ((NativeStub) v).iface;
            return iface.isSubclassOfInterp(name)
                    || "com/codename1/system/NativeInterface".equals(name);
        }
        if ("java/lang/Object".equals(name)) {
            // Object is recorded as an extern and no interpreted class lists it
            // as an interpreted supertype, so the hierarchy walk below answers
            // false -- for `x instanceof Object`, which is true of every
            // non-null reference there has ever been.
            return true;
        }
        if (v instanceof InterpObject) {
            InterpObject io = (InterpObject) v;
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
        // When the leaf type is one the host has, the host can answer exactly,
        // and exactly is better than the element scan below: casting an empty
        // Object[] to String[] must throw, and scanning no elements says yes.
        // The scan is for arrays whose leaf exists only in the bundle, where
        // there is nothing to ask.
        if (!isInterpretedLeaf(name)) {
            Object hostArrayClass = linker.findClass(name);
            if (hostArrayClass != null) {
                return linker.isInstance(hostArrayClass, v);
            }
        }
        if (component.charAt(0) == '[') {
            Object[] a = (Object[]) v;
            for (Object element : a) {
                if (element != null && !isArrayInstanceOf(element, component)) {
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
        Object[] a = (Object[]) v;
        for (Object item : a) {
            if (item == null) {
                continue;
            }
            if (!isElementOf(item, element)) {
                return false;
            }
        }
        return true;
    }

    private boolean isElementOf(Object v, String element) throws Throwable {
        // Arrays hold peers when a peer exists, so an element read raw from the
        // backing `Object[]` looks host-typed. Reach back to the interpreted
        // object it stands for before answering, or a Color[] whose elements
        // are stored as their shim peers rejects every element.
        Object unwrapped = fromHost(v);
        if (unwrapped instanceof InterpObject) {
            InterpObject io = (InterpObject) unwrapped;
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
            case InterpOpcodes.IALOAD: f.pushInt(((int[]) array)[index]); break;
            case InterpOpcodes.LALOAD: f.pushLong(((long[]) array)[index]); break;
            case InterpOpcodes.FALOAD: f.pushFloat(((float[]) array)[index]); break;
            case InterpOpcodes.DALOAD: f.pushDouble(((double[]) array)[index]); break;
            case InterpOpcodes.BALOAD:
                // byte[] and boolean[] share the opcode; they are distinct
                // array types and only the runtime type says which.
                if (array instanceof boolean[]) {
                    f.pushInt(((boolean[]) array)[index] ? 1 : 0);
                } else {
                    f.pushInt(((byte[]) array)[index]);
                }
                break;
            case InterpOpcodes.CALOAD: f.pushInt(((char[]) array)[index]); break;
            case InterpOpcodes.SALOAD: f.pushInt(((short[]) array)[index]); break;
            default:
                // AALOAD. Back to the interpreted object when the element is a
                // peer: a host-typed array holds peers, and handing one to
                // interpreted code means the next call -- owned by a class only
                // the bundle has -- goes to a linker that cannot resolve it.
                f.pushRef(fromHost(((Object[]) array)[index]));
                break;
        }
    }

    private void arrayStore(InterpFrame f, int op) throws Throwable {
        switch (op) {
            case InterpOpcodes.LASTORE: {
                long v = f.popLong();
                int i = f.popInt();
                Object a = f.popRef();
                checkArray(a, i);
                ((long[]) a)[i] = v;
                return;
            }
            case InterpOpcodes.DASTORE: {
                double v = f.popDouble();
                int i = f.popInt();
                Object a = f.popRef();
                checkArray(a, i);
                ((double[]) a)[i] = v;
                return;
            }
            case InterpOpcodes.AASTORE: {
                Object v = f.popRef();
                int i = f.popInt();
                Object a = f.popRef();
                checkArray(a, i);
                // Store the peer whenever the value has one, even into a plain
                // `Object[]` that holds pushed-only-type elements. An earlier
                // version kept wrappers in exact `Object[]` on the theory that
                // interpreter reads would see them again, but a host method
                // like `Arrays.asList(items)` retains the array by reference;
                // a later interpreted `items[0] = new Item()` would then leave
                // a wrapper alongside the peers the host handed out, and a
                // subsequent `Collections.sort` casts to `Comparable` on the
                // wrong side. `AALOAD` routes reads through `fromHost` so the
                // interpreter still sees its own object. The wrapper is stored
                // only when no peer exists (a pushed-only type has none).
                if (v instanceof InterpObject && !(a instanceof InterpObject[])
                        && ((InterpObject) v).hostPeer != null) {
                    v = ((InterpObject) v).hostPeer;
                }
                ((Object[]) a)[i] = v;
                return;
            }
            default: break;
        }
        int v = f.popInt();
        int i = f.popInt();
        Object a = f.popRef();
        checkArray(a, i);
        switch (op) {
            case InterpOpcodes.IASTORE: ((int[]) a)[i] = v; break;
            case InterpOpcodes.FASTORE: ((float[]) a)[i] = Float.intBitsToFloat(v); break;
            case InterpOpcodes.BASTORE:
                if (a instanceof boolean[]) {
                    ((boolean[]) a)[i] = v != 0;
                } else {
                    ((byte[]) a)[i] = (byte) v;
                }
                break;
            case InterpOpcodes.CASTORE: ((char[]) a)[i] = (char) v; break;
            case InterpOpcodes.SASTORE: ((short[]) a)[i] = (short) v; break;
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
        if (array instanceof Object[]) {
            return ((Object[]) array).length;
        }
        if (array instanceof int[]) {
            return ((int[]) array).length;
        }
        if (array instanceof byte[]) {
            return ((byte[]) array).length;
        }
        if (array instanceof char[]) {
            return ((char[]) array).length;
        }
        if (array instanceof long[]) {
            return ((long[]) array).length;
        }
        if (array instanceof double[]) {
            return ((double[]) array).length;
        }
        if (array instanceof float[]) {
            return ((float[]) array).length;
        }
        if (array instanceof short[]) {
            return ((short[]) array).length;
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array).length;
        }
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
            f.pushInt((int) raw);
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
                ? ((InterpBacked) value).getInterpObject()
                : value;
    }

    private Object popBoxed(InterpFrame f, int kind) {
        if (kind == InterpOpcodes.RET_OBJECT) {
            Object v = f.popRef();
            // An interpreted object crossing into host code has to go as its
            // peer; the host cannot do anything with an InterpObject.
            if (v instanceof InterpObject) {
                InterpObject io = (InterpObject) v;
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
        // Rethrow: `catch (E e) { throw e; }` reaches ATHROW with the same
        // instance the original throw recorded, and Java preserves the stack
        // of the original throw rather than the rethrow site. Detect that
        // by instance identity: `lastFailure.thrown` is what escapes host
        // code (an InterpThrowable wrapper for interpreted throwables) and
        // `.original` is what interpreted code sees on the stack (the
        // InterpObject or bare host throwable) -- either match makes this a
        // rethrow, and the recorded stack is kept rather than replaced with
        // the rethrow site.
        Failure prev = lastFailure;
        boolean rethrow = prev != null                             //NOPMD CompareObjectsWithEquals - identity is the point
                && (prev.thrown == t || prev.original == t);
        if (t instanceof Throwable) {
            // Deliberately not wrapped. A framework method that calls
            // interpreted code and catches IllegalStateException has to keep
            // catching it, so the exception has to stay the exception. The
            // interpreted frames are recorded beside it instead, and
            // [#interpretedStackFor] hands them back if it escapes.
            if (!rethrow) {
                lastFailure = new Failure(t, t, snapshotStack(), null);
            }
            return (Throwable) t;
        }
        InterpThrowable wrapped = new InterpThrowable(t, rethrow ? prev.stack : snapshotStack());
        // Record BOTH identities: `thrown` = wrapper (what host code sees
        // and passes to [#interpretedStackFor]) and `original` = the
        // InterpObject (what a subsequent interpreted `throw e` pops off
        // the operand stack, since the catch handler pushed `getThrown()`
        // rather than the wrapper). Without the second, the rethrow would
        // fail the identity check and land a new snapshot at the rethrow
        // site rather than preserving the original one.
        lastFailure = new Failure(wrapped, t, wrapped.getInterpretedStack(), null);
        return wrapped;
    }

    /// Where interpreted code threw this exception, or null if it did not.
    ///
    /// A host exception thrown by interpreted code carries the host's own stack
    /// trace, which names the interpreter's frames rather than the program's.
    /// This is the program's, recorded when it was thrown.
    public String[] interpretedStackFor(Throwable t) {
        Failure f = lastFailure;
        return f != null && f.thrown == t ? f.stack : null;  //NOPMD CompareObjectsWithEquals - the same throwable instance
    }

    /// The framework method that threw this, or null if interpreted code did.
    public String hostCallFor(Throwable t) {
        Failure f = lastFailure;
        return f != null && f.thrown == t ? f.hostCall : null;  //NOPMD CompareObjectsWithEquals - the same throwable instance
    }

    private int findHandler(InterpMethod m, int insn, Object thrown, boolean unusedFilter)
            throws Throwable {
        // `unusedFilter` was a per-throwable filter used to gate cancellation
        // to catch-all handlers only. That skipped javac's compiler-generated
        // cleanup for try-with-resources (which is a typed `catch (Throwable)`
        // rather than a catch-all), so the resource never closed on cancel.
        // The runtime now allows any handler to match; `cancelRequested`
        // stays set for the ThreadState, so the next checkpoint after the
        // handler returns re-raises `InterpCancelled` -- a user
        // `catch (Throwable)` around a loop cannot silence Stop for long
        // because the back-edge checkpoint keeps firing it. The parameter
        // is kept in the signature so callers do not all need touching for
        // the removed distinction.
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
            InterpFrame f = (InterpFrame) callStack.elementAt(i);
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
