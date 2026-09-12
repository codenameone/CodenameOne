/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import java.io.File;
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
    // Sigs protected because the BRIDGE JS names their class-free dispatch id
    // as a string literal. Unlike the JSO set these stay signature-wide: the
    // bridge also installs overrides through ``classDef.methods[id] = fn``
    // with a computed key, so there is no receiver type to reason about.
    private final Set<String> bridgeDispatchSigs = new java.util.HashSet<String>();
    // Every class assignable to JSObject. A ``bindNative`` override can only
    // land on one of these, so a call site whose receiver cone contains none
    // of them cannot reach one -- which is what makes the JSO protection
    // cone-aware rather than signature-wide.
    private final Set<String> jsoBridgeClasses = new java.util.HashSet<String>();

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

    // Receiver-type information from the RTA pass that ran immediately before
    // us. Null when RTA did not run, or when owner-aware classification is
    // switched off -- both cases fall back to the historical signature-wide
    // behaviour, which is strictly more conservative.
    private JavascriptReachability.Model rta;

    /**
     * The call-site decision, shared by this analysis and the emitter.
     *
     * They MUST agree: the analysis decides whether a method is emitted
     * ``function*``, the emitter decides whether each call inside it is
     * ``yield*``, and a ``yield*`` inside a plain ``function`` is a JS
     * SyntaxError rather than a subtle bug. So there is exactly one
     * implementation of the rule and both sides call it.
     */
    static final class DispatchModel {
        // The analysis instance, so a direct (static / special) call site is
        // answered by the SAME resolver that built the propagation edges.
        //
        // The emitter used to resolve those itself. Two resolvers meant two
        // answers, and the one that mattered was a ``super`` call landing on
        // an interface default: the emitter resolved it and called it
        // suspending, the analysis did not and left the caller synchronous, so
        // a ``yield*`` was emitted inside a plain ``function`` -- which is not
        // a subtle bug but ``ReferenceError: yield is not defined``.
        private final JavascriptSuspensionAnalysis analysis;
        private final JavascriptReachability.Model rta;
        // Declared on a JSO bridge class. Suspending only when the call
        // site's receiver cone can actually reach one of those classes.
        private final java.util.Set<String> jsoSigs;
        // Named by the bridge JS as a class-free dispatch id. Suspending
        // whatever the receiver is -- see bridgeDispatchSigs.
        private final java.util.Set<String> bridgeSigs;
        private final java.util.Set<String> jsoClasses;
        // Signature-wide fallback, i.e. the historical answer.
        private final java.util.Set<String> suspendingSigs;

        DispatchModel(JavascriptSuspensionAnalysis analysis, JavascriptReachability.Model rta,
                java.util.Set<String> jsoSigs, java.util.Set<String> bridgeSigs,
                java.util.Set<String> jsoClasses, java.util.Set<String> suspendingSigs) {
            this.analysis = analysis;
            this.rta = rta;
            this.jsoSigs = jsoSigs;
            this.bridgeSigs = bridgeSigs;
            this.jsoClasses = jsoClasses;
            this.suspendingSigs = suspendingSigs;
        }

        /**
         * The answer for an {@code INVOKESTATIC} / {@code INVOKESPECIAL}.
         * An unresolvable target stays suspending, as it always did.
         */
        boolean isDirectSuspending(String owner, String name, String desc) {
            BytecodeMethod target = analysis.resolveTarget(owner, name, desc);
            return target == null || target.isJavascriptSuspending();
        }

        boolean isDispatchSuspending(String owner, String name, String desc) {
            String sig = name + desc;
            if (rta == null || isUnconditionallySuspendingDispatch(rta, jsoSigs, bridgeSigs,
                    jsoClasses, owner, sig)) {
                return rta == null ? suspendingSigs.contains(sig) : true;
            }
            List<BytecodeMethod> impls = rta.resolveImpls(owner, name, desc);
            if (impls == null) {
                return suspendingSigs.contains(sig);
            }
            for (int i = 0; i < impls.size(); i++) {
                if (impls.get(i).isJavascriptSuspending()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Published for {@link JavascriptMethodGenerator}. Null until this
     * analysis has run, which the emitter reads as "assume suspending".
     */
    static volatile DispatchModel exportedDispatchModel = null;

    // Where the suspension report is written -- always, beside the bundle, as
    // ``suspension-report.txt``. It exists because the sync/suspending split
    // is the number this pass exists to move, and the only thing emitted
    // before was a total (Parser, behind -verbose) that could not say WHICH
    // rule was responsible for the suspending half. Null only when no output
    // directory was supplied, which is the in-memory unit-test path.
    private String reportPath;
    // Method -> the rule that FIRST classified it suspending. A method can
    // have several independent causes; this records the one that won the race
    // in the worklist, which is why the ranking below is documented as an
    // upper bound on beneficiaries rather than a prediction.
    private final Map<BytecodeMethod, String> suspendReason = new IdentityHashMap<BytecodeMethod, String>();
    // Signature -> number of INVOKEVIRTUAL / INVOKEINTERFACE call sites that
    // dispatch on it. Captured in propagate(); for a suspending signature this
    // is literally the number of ``yield*`` sites it is responsible for.
    private Map<String, Integer> dispatchSiteCount = java.util.Collections.<String, Integer>emptyMap();
    // Every INVOKEVIRTUAL / INVOKEINTERFACE instruction, by signature,
    // regardless of whether the site resolved against its receiver or fell
    // back to the signature-wide answer.
    private final Map<String, Integer> dispatchSites = new HashMap<String, Integer>();

    // Kill-switch for the class-initialization edges (issue #5774). With them
    // off, a <clinit> that suspends is only driven correctly when the guard
    // happens to sit in a method that is suspending for some other reason --
    // the pre-fix behaviour, minus the silence. Exists so the generator-count
    // cost of the edges can be A/B measured on a real bundle from its
    // suspension-report.txt, and so a regression has something to bisect
    // against. JavascriptMethodGenerator reads the same property, because the
    // two sides MUST make the same decision.
    private static final boolean CLINIT_EDGES_OFF =
            System.getProperty("parparvm.js.clinitedge.off") != null;

    static int run(List<ByteCodeClass> classes, File outputDirectory) {
        // Same reason as JavascriptReachability.run: never let a previous
        // translation's model answer this one's questions. Cleared before the
        // kill-switch return too, so the disabled path cannot inherit a model
        // either.
        exportedDispatchModel = null;
        if (System.getProperty("parparvm.js.suspension.off") != null) {
            return 0;
        }
        JavascriptSuspensionAnalysis a = new JavascriptSuspensionAnalysis();
        // Always written, always beside the bundle. A diagnostic behind a
        // system property is a diagnostic nobody sets, and the sync/suspending
        // split is the number this whole pass exists to move -- it belongs in
        // the build output where CI and a bisect can both read it.
        if (outputDirectory != null) {
            a.reportPath = new File(outputDirectory, "suspension-report.txt").getAbsolutePath();
        }
        a.rta = JavascriptReachability.modelFor(classes);
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

    /**
     * Adds {@code m} to the suspending set, recording WHY when the opt-in
     * report is on. Returns true when this call is the one that added it, so
     * it is a drop-in for {@code suspending.add(m)} at the propagation
     * worklist sites that depend on that return value.
     */
    private boolean markSuspending(BytecodeMethod m, String reason) {
        if (!suspending.add(m)) {
            return false;
        }
        if (reportPath != null) {
            suspendReason.put(m, reason);
        }
        return true;
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
                // Split into a labelled chain rather than one boolean so the
                // report can name the rule. The disjunction is unchanged --
                // order decides only which label wins, never the outcome.
                String seed = null;
                if (m.isNative() && !isSyncNativeBinding(cls, m)) {
                    seed = "native";
                } else if (m.isSynchronizedMethod()) {
                    seed = "synchronized";
                } else if (hasMonitorOps(m)) {
                    seed = "monitor-op";
                } else if (clsIsJso && !isSyncNativeBinding(cls, m)) {
                    seed = "jso-bridge-class";
                }
                if (seed != null) {
                    markSuspending(m, seed);
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
        // REPLACED, not merely referenced -- see collectBridgeReplacedCn1Tokens.
        // Narrowing the shared referenced-set instead renamed the names the
        // bridge looks up and broke nine theme screenshots.
        Set<String> tokens = JavascriptBundleWriter.collectBridgeReplacedCn1Tokens();
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
                    markSuspending(m, "bridge-referenced");
                    if (dispatchable) {
                        // Virtual dispatch can land on the runtime-installed
                        // override too -- protect the whole signature.
                        bridgeDispatchSigs.add(m.getMethodName() + m.getSignature());
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
        suspendingSigs.addAll(bridgeDispatchSigs);
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod caller : cls.getMethods()) {
                if (caller.isEliminated() || caller.isAbstract()) {
                    continue;
                }
                List<Instruction> instructions = caller.getInstructions();
                if (instructions == null) {
                    continue;
                }
                // NOTE: no edge for the own-class guard the emitter puts in a
                // static method's WRAPPER. It would be nearly redundant --
                // every INVOKESTATIC call site emits its own guard, which the
                // instruction scan below already sees -- and it reached
                // methods that have no translated body at all, including the
                // SYNC NATIVES (String.charsToBytes and friends). Promoting
                // one of those is not a slowdown, it is a miscompile: the
                // binding stays a plain ``function`` returning a byte[], while
                // every call site starts saying ``yield*`` on it -- and a
                // ``yield*`` over an array iterates it and evaluates to
                // ``undefined``, so PrintStream.print(String) read
                // ``undefined.length``. The wrapper stays correct without the
                // edge: when the method is synchronous the wrapper is a plain
                // function emitting the plain ``_I``.
                //
                // Dropping the edge is the fix, NOT a blanket sync-native veto
                // inside markSuspending. ``isSyncNativeBinding`` matches
                // instance methods signature-wide, so a veto there also
                // cancelled the STRUCTURAL seeds (``synchronized`` /
                // monitorenter), which the seed sites deliberately leave
                // unguarded -- and a synchronized method left classified
                // synchronous still gets its ``yield* _me(monitor)`` from the
                // emitter, i.e. ``ReferenceError: yield is not defined``. The
                // instruction scan below cannot reach a native anyway: a
                // native has no instructions, so it is never a caller here.
                //
                // The residual gap is narrow and no longer silent: a static
                // method entered through its wrapper, on a class whose clinit
                // suspends, hits the run-to-completion loop -- which now names
                // the class and the op instead of answering ``undefined``.
                for (Instruction instr : instructions) {
                    // Class-initialization edges. Every guard the emitter
                    // places (GETSTATIC / PUTSTATIC / INVOKESTATIC / NEW) is a
                    // call into that class's <clinit>, and a <clinit> that
                    // suspends can only be driven from a generator -- issue
                    // #5774, where a host call raised inside one was silently
                    // answered ``undefined`` because the guard ran on the
                    // synchronous run-to-completion path. Recording the edge
                    // here is what lets the emitter use ``yield* _Ig(...)``
                    // there, and the two sides MUST agree: a ``yield*`` in a
                    // plain ``function`` is a SyntaxError, so this walk mirrors
                    // JavascriptMethodGenerator.classClinitCanSuspend exactly
                    // (self + superclass chain + interfaces) and elides the
                    // same own-class/ancestor cases the emitter does.
                    String initOwner = classInitGuardOwner(instr);
                    if (initOwner != null) {
                        addClinitEdges(callersOf, caller, initOwner, true);
                    }
                    if (!(instr instanceof Invoke)) {
                        continue;
                    }
                    int op = instr.getOpcode();
                    Invoke inv = (Invoke) instr;
                    if (op == Opcodes.INVOKESTATIC || op == Opcodes.INVOKESPECIAL) {
                        BytecodeMethod target = resolveTarget(inv.getOwner(), inv.getName(), inv.getDesc());
                        if (target == null) {
                            // Unresolvable, and the EMITTER treats that as
                            // suspending (isDirectSuspending returns true for a
                            // null target). Skipping the caller here made the
                            // two sides mean opposite things by the same
                            // "unknown": a ``yield*`` at the call site inside a
                            // method emitted as a plain ``function``, which is
                            // ``ReferenceError: yield is not defined`` rather
                            // than anything subtle. If the site is suspending,
                            // so is the method containing it.
                            markSuspending(caller, "unresolved-direct:"
                                    + JavascriptNameUtil.sanitizeClassName(inv.getOwner())
                                    + "." + inv.getName() + inv.getDesc());
                            continue;
                        }
                        addCaller(callersOf, target, caller);
                    } else if (op == Opcodes.INVOKEVIRTUAL || op == Opcodes.INVOKEINTERFACE) {
                        String sig = inv.getName() + inv.getDesc();
                        // Count the site BEFORE the receiver-resolved branch
                        // returns. Counting from sigCallersOf alone measured
                        // only the fallback path, so under the default RTA
                        // path the report showed dispatch sites collapsing to
                        // near zero -- an artefact of where the edge was
                        // recorded, not a reduction in emitted call sites.
                        if (reportPath != null) {
                            Integer prev = dispatchSites.get(sig);
                            dispatchSites.put(sig, Integer.valueOf(prev == null ? 1 : prev.intValue() + 1));
                        }
                        // Resolve the call site against its RECEIVER TYPE
                        // rather than its bare signature. A cone that
                        // resolves gives us exact per-impl edges, so a
                        // blocking ``run()V`` somewhere else in the program
                        // no longer reaches this caller at all.
                        List<BytecodeMethod> impls = rta == null
                                || isUnconditionallySuspendingDispatch(rta, jsoDeclaredSigs,
                                        bridgeDispatchSigs, jsoBridgeClasses, inv.getOwner(), sig)
                                ? null
                                : rta.resolveImpls(inv.getOwner(), inv.getName(), inv.getDesc());
                        if (impls != null) {
                            for (int i = 0; i < impls.size(); i++) {
                                BytecodeMethod impl = impls.get(i);
                                addCaller(callersOf, impl, caller);
                                if (suspending.contains(impl)) {
                                    markSuspending(caller, "dispatch:"
                                            + JavascriptNameUtil.sanitizeClassName(inv.getOwner())
                                            + "." + sig);
                                }
                            }
                            continue;
                        }
                        // No receiver information (array owner, unindexed
                        // class, nothing in the cone instantiated, or a
                        // signature the bridge can override at runtime):
                        // keep the historical signature-wide edge.
                        addSigCaller(sigCallersOf, sig, caller);
                        // Early escalation: if ANY impl of the sig is
                        // already known suspending, this caller also
                        // needs to be suspending. Add to the initial
                        // worklist via the standard ``suspending.add``
                        // + propagate path below.
                        if (suspendingSigs.contains(sig)) {
                            markSuspending(caller, "dispatch:" + sig);
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
                    if (markSuspending(caller, "calls:" + qualify(suspended))) {
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
                            if (markSuspending(caller, "dispatch:" + sig)) {
                                worklist.add(caller);
                            }
                        }
                    }
                }
            }
        }
        dispatchSiteCount = dispatchSites;
        // Publish the final suspending-sig set so the emitter can
        // consult it when deciding whether an INVOKEVIRTUAL /
        // INVOKEINTERFACE call site needs ``yield*`` wrapping.
        exportedSuspendingSigs = suspendingSigs;
        exportedDispatchModel = new DispatchModel(this, rta,
                new java.util.HashSet<String>(jsoDeclaredSigs),
                new java.util.HashSet<String>(bridgeDispatchSigs),
                new java.util.HashSet<String>(jsoBridgeClasses),
                suspendingSigs);
    }

    /**
     * True when this dispatch must be suspending regardless of which body it
     * resolves to, so neither the analysis nor the emitter may consult the
     * receiver cone.
     *
     * Two different protections live here and they are NOT the same rule:
     *
     * - {@code bridgeSigs} is signature-wide. The bridge JS installs an
     *   override through {@code classDef.methods[id] = fn} with a computed
     *   key, so there is no receiver type a static walk could check.
     * - {@code jsoSigs} is cone-aware. Those overrides land on JSO bridge
     *   classes specifically, so a call whose receiver cone contains no such
     *   class cannot reach one. That distinction is the whole point: {@code
     *   getWidth()I} is declared on three JSO bridge interfaces, which used
     *   to make every {@code Component.getWidth()} in the program a
     *   suspension point.
     *
     * An unresolvable cone (array owner, class we did not index) answers true,
     * because "we do not know" has to mean "assume the bridge can reach it".
     */
    private static boolean isUnconditionallySuspendingDispatch(JavascriptReachability.Model rta,
            Set<String> jsoSigs, Set<String> bridgeSigs, Set<String> jsoClasses,
            String owner, String sig) {
        if (bridgeSigs.contains(sig)) {
            return true;
        }
        if (!jsoSigs.contains(sig)) {
            return false;
        }
        Set<String> cone = rta.coneTypes(owner);
        if (cone == null) {
            return true;
        }
        for (String type : cone) {
            if (jsoClasses.contains(type)) {
                return true;
            }
        }
        return false;
    }

    private static void addCaller(Map<BytecodeMethod, List<BytecodeMethod>> callersOf,
            BytecodeMethod callee, BytecodeMethod caller) {
        List<BytecodeMethod> callers = callersOf.get(callee);
        if (callers == null) {
            callers = new ArrayList<BytecodeMethod>();
            callersOf.put(callee, callers);
        }
        callers.add(caller);
    }

    private static void addSigCaller(Map<String, List<BytecodeMethod>> sigCallersOf,
            String sig, BytecodeMethod caller) {
        List<BytecodeMethod> callers = sigCallersOf.get(sig);
        if (callers == null) {
            callers = new ArrayList<BytecodeMethod>();
            sigCallersOf.put(sig, callers);
        }
        callers.add(caller);
    }

    /**
     * Walk the inheritance chain rooted at ``owner`` and return the
     * first non-eliminated, non-abstract method matching
     * ``name + desc``. Mirrors JVM-spec static/special dispatch
     * resolution. For ``<init>`` / ``<clinit>`` we normalise the name
     * to the translator's canonical ``__INIT__`` / ``__CLINIT__``
     * form before comparison.
     */
    // owner#name+desc -> resolved direct-invoke target, null included.
    //
    // This is called once per direct invoke while building the propagation
    // edges and again per direct invoke while emitting, and since it gained an
    // interface walk on the miss path an unmemoised version made
    // JavascriptTargetIntegrationTest go from 48s to 378s.
    private final Map<String, BytecodeMethod> resolvedTargets = new HashMap<String, BytecodeMethod>();

    /**
     * The class whose {@code <clinit>} guard the emitter places for this
     * instruction, or {@code null} when the instruction triggers no guard.
     * Mirrors the emitter's guard sites: GETSTATIC / PUTSTATIC
     * (appendStraightLineEnsureClassInitialized /
     * appendInterpreterEnsureClassInitialized), INVOKESTATIC, and NEW (whose
     * {@code _O} initializes the class on the way to allocating).
     */
    private static String classInitGuardOwner(Instruction instr) {
        int op = instr.getOpcode();
        if (instr instanceof com.codename1.tools.translator.bytecodes.Field) {
            if (op == Opcodes.GETSTATIC || op == Opcodes.PUTSTATIC) {
                return ((com.codename1.tools.translator.bytecodes.Field) instr).getOwner();
            }
            return null;
        }
        if (instr instanceof Invoke) {
            return op == Opcodes.INVOKESTATIC ? ((Invoke) instr).getOwner() : null;
        }
        if (instr instanceof com.codename1.tools.translator.bytecodes.TypeInstruction) {
            return op == Opcodes.NEW
                    ? ((com.codename1.tools.translator.bytecodes.TypeInstruction) instr).getTypeName()
                    : null;
        }
        return null;
    }

    /**
     * Records {@code caller} as a caller of every {@code <clinit>} in
     * {@code owner}'s initialization chain, and escalates it immediately if one
     * of them is already known suspending. When {@code elideOwnHierarchy} is
     * set, a guard for the caller's own class or one of its ancestors is
     * skipped -- those are already initialized by the time any of its code runs
     * and the emitter drops the guard for exactly that reason.
     */
    private void addClinitEdges(Map<BytecodeMethod, List<BytecodeMethod>> callersOf,
            BytecodeMethod caller, String owner, boolean elideOwnHierarchy) {
        if (CLINIT_EDGES_OFF) {
            return;
        }
        String start = JavascriptNameUtil.sanitizeClassName(owner);
        if (elideOwnHierarchy && isOwnOrAncestor(caller, start)) {
            return;
        }
        Set<String> seen = new java.util.HashSet<String>();
        Deque<String> stack = new ArrayDeque<String>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (cur == null || !seen.add(cur)) {
                continue;
            }
            ByteCodeClass cls = byName.get(cur);
            if (cls == null) {
                continue;
            }
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated() || !"__CLINIT__".equals(m.getMethodName())) {
                    continue;
                }
                if (m == caller) {
                    continue;        // a clinit does not guard against itself
                }
                addCaller(callersOf, m, caller);
                if (suspending.contains(m)) {
                    markSuspending(caller, "clinit:" + cur);
                }
            }
            String base = cls.getBaseClass();
            if (base != null) {
                stack.push(JavascriptNameUtil.sanitizeClassName(base));
            }
            if (cls.getBaseInterfaces() != null) {
                for (String iface : cls.getBaseInterfaces()) {
                    stack.push(JavascriptNameUtil.sanitizeClassName(iface));
                }
            }
        }
    }

    /**
     * True when {@code target} is the class declaring {@code caller} or one of
     * its superclasses. Mirrors
     * JavascriptMethodGenerator.isClassAlreadyInitializedForCurrentEmission,
     * including its hop bound, so the analysis never classifies a method
     * synchronous at a site the emitter would give a {@code yield*}.
     */
    private boolean isOwnOrAncestor(BytecodeMethod caller, String target) {
        String walk = caller.getClsName() == null
                ? null
                : JavascriptNameUtil.sanitizeClassName(caller.getClsName());
        int hops = 0;
        while (walk != null && hops++ < 64) {
            if (target.equals(walk)) {
                return true;
            }
            ByteCodeClass cls = byName.get(walk);
            String base = cls == null ? null : cls.getBaseClass();
            walk = base == null ? null : JavascriptNameUtil.sanitizeClassName(base);
        }
        return false;
    }

    private BytecodeMethod resolveTarget(String owner, String name, String desc) {
        String key = owner + "#" + name + desc;
        if (resolvedTargets.containsKey(key)) {
            return resolvedTargets.get(key);
        }
        BytecodeMethod resolved = resolveTargetUncached(owner, name, desc);
        resolvedTargets.put(key, resolved);
        return resolved;
    }

    private BytecodeMethod resolveTargetUncached(String owner, String name, String desc) {
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
        // Same interface-default case the emitter handles: a ``super`` call
        // whose superclass chain declares nothing resolves to an interface
        // DEFAULT method. Returning null here would make the caller treat the
        // site as suspending while the default itself is classified sync --
        // the two sides must agree, so search the interfaces exactly as the
        // emitter and the runtime's resolveVirtual do.
        return resolveThroughInterfaces(JavascriptNameUtil.sanitizeClassName(owner),
                normalizedName, desc);
    }

    /** Breadth-first interface search; superclasses have already been tried. */
    private BytecodeMethod resolveThroughInterfaces(String owner, String name, String desc) {
        java.util.ArrayDeque<String> pending = new java.util.ArrayDeque<String>();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        String current = owner;
        while (current != null && seen.add(current)) {
            ByteCodeClass cls = byName.get(current);
            if (cls == null) {
                break;
            }
            if (cls.getBaseInterfaces() != null) {
                for (String iface : cls.getBaseInterfaces()) {
                    pending.add(JavascriptNameUtil.sanitizeClassName(iface));
                }
            }
            String base = cls.getBaseClass();
            current = base == null ? null : JavascriptNameUtil.sanitizeClassName(base);
        }
        java.util.HashSet<String> visited = new java.util.HashSet<String>();
        while (!pending.isEmpty()) {
            String ifaceName = pending.poll();
            if (ifaceName == null || !visited.add(ifaceName)) {
                continue;
            }
            ByteCodeClass iface = byName.get(ifaceName);
            if (iface == null) {
                continue;
            }
            for (BytecodeMethod m : iface.getMethods()) {
                if (m.isEliminated() || m.isAbstract()) {
                    continue;
                }
                if (name.equals(m.getMethodName()) && desc.equals(m.getSignature())) {
                    return m;
                }
            }
            if (iface.getBaseInterfaces() != null) {
                for (String up : iface.getBaseInterfaces()) {
                    pending.add(JavascriptNameUtil.sanitizeClassName(up));
                }
            }
        }
        return null;
    }

    private int applyResults(List<ByteCodeClass> classes) {
        int sync = 0;
        int total = 0;
        List<String> methodLines = reportPath == null ? null : new ArrayList<String>();
        Map<String, Integer> causeCount = reportPath == null ? null : new HashMap<String, Integer>();
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
                if (methodLines == null) {
                    continue;
                }
                String qualified = cls.getClsName() + "." + m.getMethodName() + m.getSignature();
                if (!isSuspending) {
                    methodLines.add("M SYNC " + qualified);
                    continue;
                }
                // An abstract method has no body to classify -- it is forced
                // suspending so a caller that cannot see the override still
                // emits ``yield*``. It has no seed rule, so name it as itself.
                String cause = suspendReason.get(m);
                if (cause == null) {
                    cause = m.isAbstract() ? "abstract" : "unattributed";
                }
                methodLines.add("M SUSP " + qualified + " " + cause);
                Integer prev = causeCount.get(cause);
                causeCount.put(cause, Integer.valueOf(prev == null ? 1 : prev.intValue() + 1));
            }
        }
        if (methodLines != null) {
            writeReport(total, sync, methodLines, causeCount);
        }
        return sync;
    }

    /**
     * Orders report keys by their count, largest first, then by name so the
     * order is total and two runs of the report diff cleanly. A missing key
     * counts as zero. Static and named rather than an anonymous inner class
     * because SpotBugs runs as a zero-findings gate over this project.
     */
    private static final class ByCountDescending
            implements java.util.Comparator<String>, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, Integer> counts;

        ByCountDescending(Map<String, Integer> counts) {
            this.counts = counts;
        }

        public int compare(String a, String b) {
            Integer ca = counts.get(a);
            Integer cb = counts.get(b);
            int va = ca == null ? 0 : ca.intValue();
            int vb = cb == null ? 0 : cb.intValue();
            if (va != vb) {
                return vb < va ? -1 : 1;
            }
            return a.compareTo(b);
        }
    }

    /**
     * Folds every {@code dispatch:} cause onto the bare signature it concerns.
     *
     * A receiver-resolved edge records {@code dispatch:<owner>.<name+desc>} and
     * a fallback edge records {@code dispatch:<name+desc>}. Section 2 ranks by
     * signature, so it has to count both; reading only the bare key counted
     * the fallback path alone, which is the minority once call sites resolve
     * against their receiver -- the ranking then understated exactly the
     * signatures the owner-aware path handles.
     *
     * Splitting on the first {@code '.'} is exact: class names are sanitized to
     * identifier characters, and a JVM descriptor uses {@code '/'} rather than
     * {@code '.'}, so the only dot present is the owner separator.
     */
    private static Map<String, Integer> aggregateDispatchCauses(Map<String, Integer> causeCount) {
        Map<String, Integer> bySig = new HashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : causeCount.entrySet()) {
            String cause = entry.getKey();
            if (!cause.startsWith("dispatch:")) {
                continue;
            }
            String rest = cause.substring("dispatch:".length());
            int dot = rest.indexOf('.');
            String sig = dot < 0 ? rest : rest.substring(dot + 1);
            Integer prev = bySig.get(sig);
            bySig.put(sig, Integer.valueOf(
                    (prev == null ? 0 : prev.intValue()) + entry.getValue().intValue()));
        }
        return bySig;
    }

    /** ``owner.name+descriptor``, the identity used throughout the report. */
    private static String qualify(BytecodeMethod m) {
        return m.getClsName() + "." + m.getMethodName() + m.getSignature();
    }

    /**
     * Writes the opt-in report. Deliberately plain text and sorted, so two
     * runs diff cleanly and a shell can aggregate it without a parser.
     *
     * The ranking in section 2 is the point of the whole file: a suspending
     * SIGNATURE costs one ``yield*`` per dispatch site AND forces every
     * method containing one of those sites to be a generator, so the
     * signatures at the top are where the bundle's generator population
     * actually comes from.
     *
     * Read ``firstCause`` as an UPPER BOUND on beneficiaries, not a
     * prediction: a method is recorded against whichever cause reached it
     * first, and removing that cause can leave it suspending for another.
     */
    private void writeReport(int total, int sync, List<String> methodLines,
            Map<String, Integer> causeCount) {
        java.util.Set<String> suspendingSigs = exportedSuspendingSigs;
        List<String> sigs = new ArrayList<String>(suspendingSigs);
        Collections.sort(sigs);
        // Rank suspending signatures by dispatch sites, then by name so the
        // order is total and the file diffs cleanly.
        Map<String, Integer> sites = dispatchSiteCount;
        Collections.sort(sigs, new ByCountDescending(sites));
        List<String> causes = new ArrayList<String>(causeCount.keySet());
        Collections.sort(causes, new ByCountDescending(causeCount));
        Collections.sort(methodLines);
        java.io.PrintWriter out = null;
        try {
            out = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(reportPath), "UTF-8"));
            out.println("# ParparVM JavaScript suspension report");
            out.println("# A suspending method is emitted ``function*`` and every call to it is");
            out.println("# ``yield*``; a synchronous one is a plain function called directly.");
            out.println("TOTAL " + total);
            out.println("SYNC " + sync);
            out.println("SUSPENDING " + (total - sync));
            out.println("SUSPENDING_SIGS " + suspendingSigs.size());
            out.println("#");
            out.println("# Section 1: first-recorded cause, most common first.");
            out.println("# CAUSE <methods> <cause>");
            for (String cause : causes) {
                out.println("CAUSE " + causeCount.get(cause) + " " + cause);
            }
            out.println("#");
            out.println("# Section 2: suspending signatures ranked by dispatch call sites.");
            out.println("# dispatchSites counts every INVOKEVIRTUAL / INVOKEINTERFACE on the");
            out.println("# signature, receiver-resolved and fallback alike -- NOT the number");
            out.println("# that end up emitting yield*, which depends on each site's receiver.");
            out.println("# firstCauseMethods aggregates BOTH cause spellings for the");
            out.println("# signature: the receiver-resolved ``dispatch:<owner>.<sig>`` and the");
            out.println("# fallback ``dispatch:<sig>``. Reading only the bare key counted the");
            out.println("# fallback path alone, which is the minority under RTA.");
            out.println("# SIG <dispatchSites> <firstCauseMethods> <name+descriptor>");
            Map<String, Integer> dispatchCauseBySig = aggregateDispatchCauses(causeCount);
            for (String sig : sigs) {
                Integer siteCount = sites.get(sig);
                Integer firstCause = dispatchCauseBySig.get(sig);
                out.println("SIG " + (siteCount == null ? 0 : siteCount.intValue())
                        + " " + (firstCause == null ? 0 : firstCause.intValue())
                        + " " + sig);
            }
            out.println("#");
            out.println("# Section 3: every live method.");
            out.println("# M SYNC <owner.name+desc> | M SUSP <owner.name+desc> <cause>");
            for (String line : methodLines) {
                out.println(line);
            }
        } catch (java.io.IOException err) {
            // A diagnostic must never be the thing that breaks the build.
            System.out.println("JS suspension report could not be written to "
                    + reportPath + ": " + err.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
