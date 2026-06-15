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
 * <li>It contains an {@code invokevirtual} / {@code invokeinterface}
 *     whose dispatched signature has AT LEAST ONE suspending impl in
 *     the class hierarchy (override-set CHA). Such sites are emitted as
 *     {@code yield* cn1_iv*}; sites whose every impl is synchronous use
 *     the {@code cn1_ivs*} sync dispatcher and do NOT make their caller
 *     suspending. The suspending-sig set is computed by fixed-point in
 *     {@link #propagate} and exported via {@link #exportedSuspendingSigs}.</li>
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
    // Sigs whose runtime impl can be a bindNative-installed generator the
    // static concrete-impl scan cannot see: declared (possibly abstractly)
    // on JSO-bridge classes, or string-referenced by the bridge JS (see
    // seedBridgeReferenced). Unconditionally suspending.
    private final Set<String> jsoDeclaredSigs = new java.util.HashSet<String>();

    // Native bridge bindings whose wrapper is a plain ``function`` (not
    // ``function*``): SYNCHRONOUS natives that never yield. They must NOT be
    // seeded suspending (neither by the native seed nor the bridge-referenced
    // seed), so callers can invoke them directly instead of ``yield*``-ing.
    // These are all declared ``native`` (no Java body), so propagate() never
    // re-introduces them.
    private final Set<String> syncNativeTokens = JavascriptBundleWriter.collectSyncNativeTokens();

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
        a.seedBridgeReferenced(classes);
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
                // JSO-declared SIGNATURES must be suspending even when the
                // declaration is abstract (interface methods like
                // ``Window.getDocument()`` have NO translated impl at all --
                // the only "impl" is the ``function*`` override bindNative
                // installs at runtime, which the concrete-impl scan in
                // ``propagate`` can never see). Record the sig here so
                // ``propagate`` folds it into ``suspendingSigs`` and every
                // dispatching call site keeps its ``yield*``.
                if (clsIsJso && !m.isEliminated() && !m.isStatic() && !m.isConstructor()
                        && !isSyncNativeBinding(cls, m)) {
                    jsoDeclaredSigs.add(m.getMethodName() + m.getSignature());
                }
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
                //
                // Virtual dispatch is NO LONGER an unconditional seed.
                // The emitter now has a synchronous virtual-dispatch
                // family (``cn1_ivs0..N`` in parparvm_runtime.js) that
                // it selects (via ``isInvokeSuspending`` consulting
                // ``exportedSuspendingSigs``) for any INVOKEVIRTUAL /
                // INVOKEINTERFACE whose CHA impl set is entirely
                // synchronous. So a method whose only virtual calls
                // target non-suspending sigs can itself be a plain
                // ``function``. Suspension still propagates through
                // virtual dispatch in ``propagate``: if ANY impl of a
                // called sig is suspending, that sig is suspending and
                // every caller of it is marked suspending there. The
                // earlier sync-dispatcher attempts failed by letting a
                // generator leak as a value; ``cn1_ivs*`` drives a
                // one-shot and throws a named error on a true gap
                // instead (see the runtime helper).
                if ((m.isNative() && !isSyncNativeBinding(cls, m))
                        || m.isSynchronizedMethod()
                        || hasMonitorOps(m)
                        || (clsIsJso && !isSyncNativeBinding(cls, m))) {
                    suspending.add(m);
                }
            }
        }
    }

    /**
     * Any method whose emitted identifier (or its {@code __impl} body, or
     * its class-free dispatch id) appears as a string literal in the
     * hand-written bridge JS must be suspending. Those strings are how
     * {@code bindNative} / {@code bindCiFallback} (port.js,
     * parparvm_runtime.js, browser_bridge.js) locate translated methods
     * to REPLACE with {@code function*} overrides at runtime. The static
     * body may look trivially synchronous, but the override that actually
     * runs is a generator -- a caller that skipped {@code yield*} would
     * receive the raw generator object as its "result" and the override
     * would never execute (observed as the screenshot runner's
     * done-callback silently never firing). Over-protecting names the
     * bridge merely CALLS (it wraps those in {@code cn1_ivAdapt}, which
     * tolerates sync) costs a handful of generators; under-protecting
     * breaks the bridge contract silently, so blanket-protect every
     * string-referenced name.
     */
    private void seedBridgeReferenced(List<ByteCodeClass> classes) {
        Set<String> tokens = JavascriptBundleWriter.collectBridgeReferencedCn1Tokens();
        if (tokens.isEmpty()) {
            return;
        }
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated() || m.isAbstract()) {
                    continue;
                }
                String full = JavascriptNameUtil.methodIdentifier(cls.getClsName(), m.getMethodName(), m.getSignature());
                boolean referenced = tokens.contains(full) || tokens.contains(full + "__impl");
                boolean dispatchable = !m.isStatic() && !m.isConstructor();
                if (!referenced && dispatchable
                        && tokens.contains(JavascriptNameUtil.dispatchMethodIdentifier(m.getMethodName(), m.getSignature()))) {
                    referenced = true;
                }
                if (referenced && !isSyncNativeBinding(cls, m)) {
                    suspending.add(m);
                    if (dispatchable) {
                        // Virtual dispatch can land on the runtime-installed
                        // override too -- protect the whole signature, same
                        // as the JSO-declared sigs.
                        jsoDeclaredSigs.add(m.getMethodName() + m.getSignature());
                    }
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

    /**
     * True when this method is implemented by a synchronous native bridge
     * binding (a plain {@code function} wrapper, see
     * {@link JavascriptBundleWriter#collectSyncNativeTokens}). Such a method
     * never yields, so it must stay synchronous regardless of the {@code
     * native} / bridge-referenced seeds.
     */
    private boolean isSyncNativeBinding(ByteCodeClass cls, BytecodeMethod m) {
        if (syncNativeTokens.isEmpty() || m.isAbstract()) {
            return false;
        }
        String full = JavascriptNameUtil.methodIdentifier(cls.getClsName(), m.getMethodName(), m.getSignature());
        if (syncNativeTokens.contains(full) || syncNativeTokens.contains(full + "__impl")) {
            return true;
        }
        return !m.isStatic() && !m.isConstructor()
                && syncNativeTokens.contains(JavascriptNameUtil.dispatchMethodIdentifier(m.getMethodName(), m.getSignature()));
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
        // JSO-bridge declared sigs are suspending regardless of their (often
        // absent / abstract) translated impls -- see seedDirectlySuspending.
        // Must be folded in BEFORE the caller scan below so dispatching
        // callers get escalated.
        suspendingSigs.addAll(jsoDeclaredSigs);
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
