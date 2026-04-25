/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/**
 * JavaScript-target-only suspension analysis. Classifies each surviving
 * method as either {@code suspending} (can yield the cooperative
 * scheduler, so must be emitted as {@code function*} with {@code yield*}
 * at each call site) or {@code synchronous} (can run straight through
 * without yielding, so is emitted as plain {@code function} and invoked
 * directly).
 *
 * A method is suspending if any of these are true:
 * <ul>
 * <li>It is native — JS stubs emit {@code yield jvm.invokeHostNative(...)}.</li>
 * <li>It is declared {@code synchronized} — monitor acquisition can block.</li>
 * <li>Its bytecode contains {@code monitorenter} or {@code monitorexit}
 *     (synchronized block) — same reason.</li>
 * <li>It contains any {@code invokevirtual} / {@code invokeinterface}
 *     instruction — the dispatch goes through {@code cn1_iv*} which is
 *     a generator, so the caller must be ready to {@code yield*}. We
 *     treat ALL virtuals as suspending rather than doing
 *     override-set CHA, which keeps the analysis portable and safe
 *     against future-inherited suspending overrides.</li>
 * <li>It contains any {@code invokestatic} / {@code invokespecial} whose
 *     resolved target is itself suspending (recursive closure via
 *     fixed-point iteration).</li>
 * </ul>
 * Methods that satisfy NONE of the above are synchronous. In Initializr
 * this is mostly leaf getters/setters, simple arithmetic helpers, and
 * tiny utility bodies with no invokes or monitors.
 *
 * Runs after {@link JavascriptReachability}, so it only classifies live
 * methods (eliminated ones are ignored).
 */
final class JavascriptSuspensionAnalysis {
    private final Map<String, ByteCodeClass> byName = new HashMap<String, ByteCodeClass>();
    private final Set<BytecodeMethod> suspending = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());

    // Sigs (name + descriptor) whose concrete impl set contains AT
    // LEAST ONE suspending method. Populated during ``propagate``
    // and exposed for the emitter's INVOKEVIRTUAL / INVOKEINTERFACE
    // callsite decision: a dispatch whose sig isn't in this set can
    // drop the ``yield*`` ceremony and use a sync dispatcher.
    static volatile java.util.Set<String> exportedSuspendingSigs = java.util.Collections.<String>emptySet();

    static int run(List<ByteCodeClass> classes) {
        if (System.getProperty("parparvm.js.suspension.off") != null) {
            return 0;
        }
        JavascriptSuspensionAnalysis a = new JavascriptSuspensionAnalysis();
        a.index(classes);
        a.seedDirectlySuspending(classes);
        a.propagate(classes);
        return a.applyResults(classes);
    }

    private void index(List<ByteCodeClass> classes) {
        for (ByteCodeClass cls : classes) {
            byName.put(cls.getClsName(), cls);
        }
    }

    private void seedDirectlySuspending(List<ByteCodeClass> classes) {
        // Every method on a JSO-bridge class is conservatively
        // suspending. These classes (anything assignable to
        // com_codename1_html5_js_JSObject) are typically interfaces
        // whose Java-declared bodies are trivial ``return null``
        // stubs, but the runtime replaces them with ``function*``
        // overrides via ``bindNative`` in port.js / parparvm_runtime.js.
        // If we trusted the static body, the emitted caller would
        // skip ``yield*`` and the installed generator would leak
        // through to the caller as a raw generator object (we've
        // already seen this manifest as ``Window.current()`` returning
        // a non-wrapped value in the init path). Mark them suspending
        // up front so the caller stays ``yield*``-wrapped regardless.
        java.util.Set<String> jsoBridgeClasses = new java.util.HashSet<String>();
        for (ByteCodeClass cls : classes) {
            if (isJsoBridgeClass(cls)) {
                jsoBridgeClasses.add(cls.getClsName());
            }
        }
        for (ByteCodeClass cls : classes) {
            boolean clsIsJso = jsoBridgeClasses.contains(cls.getClsName());
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated() || m.isAbstract()) {
                    continue;
                }
                // Seed methods that are INTRINSICALLY suspending —
                // native, synchronized, contain monitor ops, live on
                // a JSO-bridge class, OR contain INVOKEVIRTUAL /
                // INVOKEINTERFACE. Forcing every method to be
                // suspending costs ~17× per-call overhead in the
                // cooperative scheduler (measured on the lifecycle
                // harness: from 1.6 host callbacks/s down to 0.09/s)
                // so we keep the CHA-sync optimization but pair it
                // with ``cn1_ivAdapt`` wrappers at every hand-written
                // ``yield* translatedFn(args)`` call site.
                if (m.isNative()
                        || m.isSynchronizedMethod()
                        || hasMonitorOps(m)
                        || clsIsJso
                        || hasVirtualDispatch(m)) {
                    suspending.add(m);
                }
            }
        }
    }

    private boolean isJsoBridgeClass(ByteCodeClass cls) {
        java.util.Set<String> seen = new java.util.HashSet<String>();
        java.util.Deque<ByteCodeClass> stack = new java.util.ArrayDeque<ByteCodeClass>();
        stack.push(cls);
        while (!stack.isEmpty()) {
            ByteCodeClass current = stack.pop();
            if (current == null || !seen.add(current.getClsName())) {
                continue;
            }
            if ("com_codename1_html5_js_JSObject".equals(current.getClsName())) {
                return true;
            }
            String base = current.getBaseClass();
            if (base != null) {
                ByteCodeClass baseObj = byName.get(JavascriptNameUtil.sanitizeClassName(base));
                if (baseObj != null) {
                    stack.push(baseObj);
                }
            }
            if (current.getBaseInterfaces() != null) {
                for (String iface : current.getBaseInterfaces()) {
                    ByteCodeClass ifaceObj = byName.get(JavascriptNameUtil.sanitizeClassName(iface));
                    if (ifaceObj != null) {
                        stack.push(ifaceObj);
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasMonitorOps(BytecodeMethod m) {
        List<Instruction> instructions = m.getInstructions();
        if (instructions == null) {
            return false;
        }
        for (Instruction instr : instructions) {
            if (instr instanceof BasicInstruction) {
                int op = instr.getOpcode();
                if (op == Opcodes.MONITORENTER || op == Opcodes.MONITOREXIT) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasVirtualDispatch(BytecodeMethod m) {
        List<Instruction> instructions = m.getInstructions();
        if (instructions == null) {
            return false;
        }
        for (Instruction instr : instructions) {
            if (!(instr instanceof Invoke)) {
                continue;
            }
            int op = instr.getOpcode();
            if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
                return true;
            }
        }
        return false;
    }

    private void propagate(List<ByteCodeClass> classes) {
        // Build two reverse indexes so a method becoming suspending
        // can propagate to all its callers without rescanning every
        // class on each iteration:
        //
        //   * ``callersOf``      : callee method → methods that
        //     INVOKESTATIC / INVOKESPECIAL call that exact callee.
        //   * ``sigCallersOf``   : ``name+desc`` signature → methods
        //     that INVOKEVIRTUAL / INVOKEINTERFACE dispatch on the
        //     signature, AND ``sigImpls`` maps the same signature to
        //     every concrete method that implements it. When any
        //     impl of a sig becomes suspending, every caller of the
        //     sig has to be re-examined.
        Map<BytecodeMethod, List<BytecodeMethod>> callersOf = new IdentityHashMap<BytecodeMethod, List<BytecodeMethod>>();
        Map<String, List<BytecodeMethod>> sigCallersOf = new HashMap<String, List<BytecodeMethod>>();
        Map<String, List<BytecodeMethod>> sigImpls = new HashMap<String, List<BytecodeMethod>>();
        Map<BytecodeMethod, Boolean> methodSigIsSuspending = new IdentityHashMap<BytecodeMethod, Boolean>();
        java.util.Set<String> suspendingSigs = new java.util.HashSet<String>();

        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated() || m.isAbstract() || m.isStatic() || m.isConstructor()) {
                    continue;
                }
                String sig = m.getMethodName() + m.getSignature();
                List<BytecodeMethod> impls = sigImpls.get(sig);
                if (impls == null) {
                    impls = new ArrayList<BytecodeMethod>();
                    sigImpls.put(sig, impls);
                }
                impls.add(m);
                if (suspending.contains(m)) {
                    suspendingSigs.add(sig);
                }
            }
        }
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod caller : cls.getMethods()) {
                if (caller.isEliminated() || caller.isAbstract()) {
                    continue;
                }
                List<Instruction> instructions = caller.getInstructions();
                if (instructions == null) {
                    continue;
                }
                for (Instruction instr : instructions) {
                    if (!(instr instanceof Invoke)) {
                        continue;
                    }
                    int op = instr.getOpcode();
                    Invoke inv = (Invoke) instr;
                    if (op == Opcodes.INVOKESTATIC || op == Opcodes.INVOKESPECIAL) {
                        BytecodeMethod target = resolveTarget(inv.getOwner(), inv.getName(), inv.getDesc());
                        if (target == null) {
                            continue;
                        }
                        List<BytecodeMethod> callers = callersOf.get(target);
                        if (callers == null) {
                            callers = new ArrayList<BytecodeMethod>();
                            callersOf.put(target, callers);
                        }
                        callers.add(caller);
                    } else if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
                        String sig = inv.getName() + inv.getDesc();
                        List<BytecodeMethod> callers = sigCallersOf.get(sig);
                        if (callers == null) {
                            callers = new ArrayList<BytecodeMethod>();
                            sigCallersOf.put(sig, callers);
                        }
                        callers.add(caller);
                        // Early escalation: if ANY impl of the sig is
                        // already known suspending, this caller also
                        // needs to be suspending. Add to the initial
                        // worklist via the standard ``suspending.add``
                        // + propagate path below.
                        if (suspendingSigs.contains(sig)) {
                            suspending.add(caller);
                        }
                    }
                }
            }
        }

        Deque<BytecodeMethod> worklist = new ArrayDeque<BytecodeMethod>(suspending);
        while (!worklist.isEmpty()) {
            BytecodeMethod suspended = worklist.poll();
            // Propagate to direct callers (static / special).
            List<BytecodeMethod> directCallers = callersOf.get(suspended);
            if (directCallers != null) {
                for (BytecodeMethod caller : directCallers) {
                    if (suspending.add(caller)) {
                        worklist.add(caller);
                    }
                }
            }
            // Propagate to virtual / interface callers of any
            // signature this method implements. If the sig wasn't
            // previously known-suspending, all its callers now must
            // be re-examined.
            if (!suspended.isStatic() && !suspended.isConstructor() && !suspended.isAbstract()) {
                String sig = suspended.getMethodName() + suspended.getSignature();
                if (suspendingSigs.add(sig)) {
                    List<BytecodeMethod> sigCallers = sigCallersOf.get(sig);
                    if (sigCallers != null) {
                        for (BytecodeMethod caller : sigCallers) {
                            if (suspending.add(caller)) {
                                worklist.add(caller);
                            }
                        }
                    }
                }
            }
        }
        // Publish the final suspending-sig set so the emitter can
        // consult it when deciding whether an INVOKEVIRTUAL /
        // INVOKEINTERFACE call site needs ``yield*`` wrapping.
        exportedSuspendingSigs = suspendingSigs;
    }

    /**
     * Walk the inheritance chain rooted at ``owner`` and return the
     * first non-eliminated, non-abstract method matching
     * ``name + desc``. Mirrors JVM-spec static/special dispatch
     * resolution. For ``<init>`` / ``<clinit>`` we normalise the name
     * to the translator's canonical ``__INIT__`` / ``__CLINIT__``
     * form before comparison.
     */
    private BytecodeMethod resolveTarget(String owner, String name, String desc) {
        String clsName = JavascriptNameUtil.sanitizeClassName(owner);
        String normalizedName;
        if ("<init>".equals(name)) {
            normalizedName = "__INIT__";
        } else if ("<clinit>".equals(name)) {
            normalizedName = "__CLINIT__";
        } else {
            normalizedName = name;
        }
        while (clsName != null) {
            ByteCodeClass cls = byName.get(clsName);
            if (cls == null) {
                return null;
            }
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated() || m.isAbstract()) {
                    continue;
                }
                if (normalizedName.equals(m.getMethodName()) && desc.equals(m.getSignature())) {
                    return m;
                }
            }
            String base = cls.getBaseClass();
            clsName = base == null ? null : JavascriptNameUtil.sanitizeClassName(base);
        }
        return null;
    }

    private int applyResults(List<ByteCodeClass> classes) {
        int sync = 0;
        int total = 0;
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated()) {
                    continue;
                }
                total++;
                boolean isSuspending = suspending.contains(m) || m.isAbstract();
                m.setJavascriptSuspending(isSuspending);
                if (!isSuspending) {
                    sync++;
                }
            }
        }
        return sync;
    }
}
