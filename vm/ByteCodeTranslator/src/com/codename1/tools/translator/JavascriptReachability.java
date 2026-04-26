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

import com.codename1.tools.translator.bytecodes.Field;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.bytecodes.Ldc;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import org.objectweb.asm.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/**
 * JavaScript-target-only Rapid Type Analysis culler. The default
 * {@link MethodDependencyGraph}-based culler over-approximates heavily:
 * it keys callers by ``desc.name`` only (no class), so any call to
 * {@code Foo.size()I} marks {@code Bar.size()I}, {@code Baz.size()I}
 * and every other ``size()I`` in the program as reachable. Running the
 * JS port through it produced ~72k surviving methods for an app that
 * only needs a small fraction.
 *
 * RTA starts from {@code main} plus a handful of runtime roots, keeps a
 * set of classes that have actually been instantiated (via {@code new}
 * or array creation), and resolves each {@code invokevirtual /
 * invokeinterface} to exactly the set of overrides reachable from the
 * currently known instantiated subtypes. When a new subtype enters
 * the instantiated set, pending virtual calls whose receiver type is
 * a supertype are re-resolved against it. {@code invokestatic /
 * invokespecial} are handled precisely against the declared owner.
 *
 * We leave the conservative culler alone for iOS / C# — their
 * runtimes rely on different dispatch mechanics and changing their
 * reachability could break end-user apps. JS is opt-in via the output
 * type check in {@link Parser#compile(File)}.
 *
 * Safety net: methods marked "used by native" and main methods are
 * kept unconditionally. Common runtime roots (Boolean/String/Integer/
 * Thread/etc. that {@link ByteCodeClass#markDependencies} pins
 * regardless) are also seeded as instantiated — app code that reaches
 * them via {@code Class.forName} or reflection still finds them alive.
 */
final class JavascriptReachability {
    private static final String[] RUNTIME_ROOT_CLASSES = {
        "java_lang_Boolean",
        "java_lang_String",
        "java_lang_Integer",
        "java_lang_Byte",
        "java_lang_Short",
        "java_lang_Character",
        "java_lang_Thread",
        "java_lang_Long",
        "java_lang_Double",
        "java_lang_Float",
        "java_lang_StackOverflowError",
        "java_text_DateFormat",
        "java_lang_NullPointerException",
        "java_lang_ArrayIndexOutOfBoundsException",
        "java_lang_ArithmeticException",
        "java_lang_ClassCastException",
        "java_lang_NegativeArraySizeException",
        "java_lang_Object"
    };

    private final Map<String, ByteCodeClass> byName = new HashMap<String, ByteCodeClass>();
    private final Map<String, Set<String>> subclassesOf = new HashMap<String, Set<String>>();
    private final Set<String> instantiated = new HashSet<String>();
    // BytecodeMethod.equals() is content-based (name+args+return) and
    // intentionally ignores the declaring class, so a plain HashSet
    // would collapse every class's ``<init>()V`` into a single entry
    // and wreck RTA. Use identity equality to keep per-class methods
    // distinct.
    private final Set<BytecodeMethod> live = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
    private final Deque<BytecodeMethod> worklist = new ArrayDeque<BytecodeMethod>();
    private final Map<String, List<VirtualCall>> pendingByReceiver = new HashMap<String, List<VirtualCall>>();

    private static final class VirtualCall {
        final String receiver;
        final String methodName;
        final String desc;
        final boolean isInterface;
        VirtualCall(String receiver, String methodName, String desc, boolean isInterface) {
            this.receiver = receiver;
            this.methodName = methodName;
            this.desc = desc;
            this.isInterface = isInterface;
        }
    }

    static int run(List<ByteCodeClass> classes, String[] nativeSources) {
        JavascriptReachability rta = new JavascriptReachability();
        rta.index(classes);
        rta.seedRoots(classes, nativeSources);
        rta.propagate();
        return rta.eliminate(classes);
    }

    private void index(List<ByteCodeClass> classes) {
        for (ByteCodeClass cls : classes) {
            byName.put(cls.getClsName(), cls);
        }
        // Build a subtype index covering both the extends chain and
        // implements/interface-extends relationships. This is the
        // transitive subtype set we scan when a virtual call's
        // receiver is a supertype of some instantiated class.
        for (ByteCodeClass cls : classes) {
            String clsName = cls.getClsName();
            String base = cls.getBaseClass();
            if (base != null) {
                base = JavascriptNameUtil.sanitizeClassName(base);
                addSubtype(base, clsName);
            }
            List<String> ifaces = cls.getBaseInterfaces();
            if (ifaces != null) {
                for (String iface : ifaces) {
                    iface = JavascriptNameUtil.sanitizeClassName(iface);
                    addSubtype(iface, clsName);
                }
            }
        }
    }

    private void addSubtype(String supertype, String subtype) {
        Set<String> set = subclassesOf.get(supertype);
        if (set == null) {
            set = new HashSet<String>();
            subclassesOf.put(supertype, set);
        }
        set.add(subtype);
    }

    private void seedRoots(List<ByteCodeClass> classes, String[] nativeSources) {
        // main + all main methods
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated()) {
                    continue;
                }
                if (m.isMain()) {
                    enqueue(m);
                }
                // Native-consumer methods are always kept — the iOS/JS
                // host may reach them via a channel the RTA can't see.
                if (m.isMethodUsedByNative(nativeSources, cls)) {
                    enqueue(m);
                }
                // finalize() is legal to be called by the VM without
                // appearing in bytecode.
                if ("finalize".equals(m.getMethodName())) {
                    enqueue(m);
                }
                // __CLINIT__ fires when the owning class is first
                // touched; seed it when we see the class.
            }
            if (cls.getUsedByNative() == ByteCodeClass.UsedByNativeResult.Used) {
                markClassInstantiated(cls.getClsName());
            }
        }
        // Runtime roots that the translator always keeps alive.
        for (String root : RUNTIME_ROOT_CLASSES) {
            markClassInstantiated(root);
        }
        // Thread.start() is a native stub that goes through ``jvm.spawn``
        // on the JS runtime side. The runtime drives ``Thread.run()`` as
        // a generator but that edge is invisible to bytecode-only RTA —
        // main never literally invokes ``thread.run()``. Seed it so the
        // Thread.run()V dispatch stays live, which in turn keeps every
        // user-supplied ``Runnable.run()V`` (anonymous or otherwise)
        // reachable via the INVOKEINTERFACE inside Thread.run()'s body.
        seedRuntimeDispatched("java_lang_Thread", "run", "()V");
        seedRuntimeDispatched("java_lang_Runnable", "run", "()V");
        // JSO bridge methods are reachable via hand-written port.js
        // dispatch sites that the bytecode-only RTA can't see (e.g.
        // ``__nativeEventListener`` in port.js calls
        // ``EventListener.handleEvent`` from JS when the DOM fires
        // an event). For every interface in the JSObject family,
        // seed all of its declared methods as runtime-dispatched so
        // the impl methods on instantiated implementing Java classes
        // stay live. Without this, ``handleEvent`` /
        // ``onAnimationFrame`` on user EventListener / callback
        // anonymous classes get culled, the m: lookup misses, and
        // ``resolveVirtual`` falls back to the JSO bridge — which
        // throws "Missing JS member handleEvent" because the
        // receiver is a Java object, not a JS handler.
        seedJsoBridgeInterfaceMethods(classes);
    }

    /**
     * For every JSObject-derived interface, treat every declared
     * (non-static) method as a runtime-dispatched virtual call. The
     * receiver is the interface itself; ``markClassInstantiated`` on
     * any implementing class re-resolves these pending calls and
     * enqueues the concrete override.
     */
    private void seedJsoBridgeInterfaceMethods(List<ByteCodeClass> classes) {
        for (ByteCodeClass cls : classes) {
            if (!isJsoBridgeType(cls)) {
                continue;
            }
            String owner = cls.getClsName();
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isStatic()) {
                    continue;
                }
                String name = m.getMethodName();
                String desc = m.getSignature();
                if (name == null || desc == null) {
                    continue;
                }
                // ``<init>`` / ``<clinit>`` would be normalised to
                // ``__INIT__`` / ``__CLINIT__`` here; static-init isn't a
                // virtual dispatch target and ctors aren't called via
                // the JSO bridge, so skip them.
                if ("__INIT__".equals(name) || "__CLINIT__".equals(name)) {
                    continue;
                }
                VirtualCall call = new VirtualCall(owner, name, desc, true);
                recordPending(call);
                dispatchVirtualFromInstantiated(call);
            }
        }
    }

    /**
     * True if {@code cls} extends or implements
     * ``com_codename1_html5_js_JSObject`` transitively (or is JSObject
     * itself). Mirrors ``JavascriptBundleWriter.isJsoBridgeClass`` /
     * ``JavascriptSuspensionAnalysis.isJsoBridgeClass``.
     */
    private boolean isJsoBridgeType(ByteCodeClass cls) {
        Set<String> seen = new HashSet<String>();
        Deque<ByteCodeClass> stack = new ArrayDeque<ByteCodeClass>();
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
            List<String> ifaces = current.getBaseInterfaces();
            if (ifaces != null) {
                for (String iface : ifaces) {
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
     * Treat {@code owner.methodName(desc)} as if the runtime invoked it
     * on an instance of {@code owner}. Equivalent to the RTA seeing an
     * ``INVOKEVIRTUAL owner.methodName(desc)`` on a freshly-instantiated
     * {@code owner}: the static receiver plus every instantiated subtype
     * becomes a dispatch target. Used for runtime-edge methods
     * (Thread.run, etc.) that bytecode analysis cannot see.
     */
    private void seedRuntimeDispatched(String owner, String methodName, String desc) {
        markClassInstantiated(owner);
        VirtualCall call = new VirtualCall(owner, methodName, desc, false);
        recordPending(call);
        dispatchVirtualFromInstantiated(call);
    }

    private void enqueue(BytecodeMethod method) {
        if (method == null || method.isEliminated() || !live.add(method)) {
            return;
        }
        worklist.add(method);
    }

    private void markClassInstantiated(String clsName) {
        if (clsName == null || !instantiated.add(clsName)) {
            return;
        }
        ByteCodeClass cls = byName.get(clsName);
        if (cls == null) {
            return;
        }
        // Static-initialiser fires implicitly on first touch.
        for (BytecodeMethod m : cls.getMethods()) {
            if ("__CLINIT__".equals(m.getMethodName())) {
                enqueue(m);
            }
        }
        // Walk the supertype chain so instance method dispatch can
        // land on any of them. Instantiating Foo implicitly touches
        // Foo's base classes too (they don't get their own "new", but
        // Foo's ctor calls super() etc.).
        String base = cls.getBaseClass();
        if (base != null) {
            markClassInstantiated(JavascriptNameUtil.sanitizeClassName(base));
        }
        // Resolve any pending virtual calls whose receiver type is this
        // class OR any of its (transitive) supertypes — including bases
        // that were already instantiated before us.
        //
        // Bug we used to have: virtual call sites are recorded by their
        // STATIC receiver (e.g. ``cmp.paint(g)`` inside Component records
        // pending under ``Component``); when the first Component subtype
        // (say ``Form``) becomes instantiated we run
        // ``dispatchVirtualSubtree`` once, which targets the snapshot of
        // instantiated subtypes seen at THAT moment. A later
        // instantiation deeper in the hierarchy (Scene, via the
        // anonymous Spinner3D$1) ran ``markClassInstantiated`` whose
        // recursion early-exited at the first already-instantiated base
        // (Container), so ``resolvePendingFor("Component")`` never
        // re-fired and Scene.paint stayed culled. The visible symptom
        // was the Spinner3D area painting only the Container default
        // (no row text, no scene-graph) — the LightweightPicker baseline
        // shows the date wheel, the regressed bundle shows a blank
        // panel.
        //
        // Walking the full ancestor chain (not just the direct base /
        // interfaces) on every instantiation re-resolves every pending
        // receiver type that the new class transitively satisfies, so
        // late-arriving subtypes pick up the existing pending calls.
        Set<String> ancestorChain = new HashSet<String>();
        collectTransitiveAncestors(clsName, ancestorChain);
        for (String ancestor : ancestorChain) {
            resolvePendingFor(ancestor);
        }
    }

    private void collectTransitiveAncestors(String clsName, Set<String> out) {
        if (clsName == null || !out.add(clsName)) {
            return;
        }
        ByteCodeClass cls = byName.get(clsName);
        if (cls == null) {
            return;
        }
        String base = cls.getBaseClass();
        if (base != null) {
            collectTransitiveAncestors(JavascriptNameUtil.sanitizeClassName(base), out);
        }
        List<String> ifaces = cls.getBaseInterfaces();
        if (ifaces != null) {
            for (String iface : ifaces) {
                collectTransitiveAncestors(JavascriptNameUtil.sanitizeClassName(iface), out);
            }
        }
    }

    private void resolvePendingFor(String receiverType) {
        List<VirtualCall> pending = pendingByReceiver.get(receiverType);
        if (pending == null) {
            return;
        }
        // Snapshot so re-entrant adds don't break iteration.
        VirtualCall[] snapshot = pending.toArray(new VirtualCall[0]);
        for (VirtualCall call : snapshot) {
            dispatchVirtualFromInstantiated(call);
        }
    }

    private void propagate() {
        while (!worklist.isEmpty()) {
            BytecodeMethod method = worklist.poll();
            visitMethod(method);
        }
    }

    private void visitMethod(BytecodeMethod method) {
        String clsName = method.getClsName();
        markClassInstantiated(clsName);
        List<Instruction> instructions = method.getInstructions();
        if (instructions == null) {
            return;
        }
        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            if (instr instanceof TypeInstruction) {
                int op = instr.getOpcode();
                if (op == Opcodes.NEW) {
                    String type = ((TypeInstruction) instr).getTypeName();
                    if (type != null) {
                        markClassInstantiated(JavascriptNameUtil.sanitizeClassName(type));
                    }
                }
                // ANEWARRAY doesn't create instances of the component type
                // (it creates an array object) — the component type only
                // becomes relevant when its methods are invoked, which
                // the invoke-walk below covers.
            } else if (instr instanceof Field) {
                Field f = (Field) instr;
                int op = f.getOpcode();
                if (op == Opcodes.GETSTATIC || op == Opcodes.PUTSTATIC) {
                    // Touching a static triggers the owner's clinit.
                    markClassInstantiated(JavascriptNameUtil.sanitizeClassName(f.getOwner()));
                }
            } else if (instr instanceof Invoke) {
                Invoke inv = (Invoke) instr;
                handleInvoke(inv);
                // ``NativeLookup.register(stub.class, impl.class)``
                // instantiates the impl class via reflection inside
                // ``NativeLookup.create()`` — which RTA can't see. The
                // impl class only appears in the bytecode as an LDC
                // operand to register, so its methods get culled and
                // the runtime throws "Missing virtual method" the first
                // time framework code dispatches into the native
                // interface. Treat the LDC class operand to register as
                // an instantiation marker, mirroring what would happen
                // if the launcher had a literal ``new ImplClass()``.
                if (inv.getOpcode() == Opcodes.INVOKESTATIC
                        && "com/codename1/system/NativeLookup".equals(inv.getOwner())
                        && "register".equals(inv.getName())) {
                    markRecentLdcClasses(instructions, i);
                }
            }
        }
    }

    /**
     * Walk backwards from {@code invokeIndex} collecting the two most
     * recent ``LDC class`` operands (the two arguments of
     * ``NativeLookup.register(Class, Class)V``) and mark each as
     * instantiated. Stops walking past control-flow boundaries — a
     * label or jump means the LDC is not in the same straight-line
     * region as the invoke.
     */
    private void markRecentLdcClasses(List<Instruction> instructions, int invokeIndex) {
        int needed = 2;
        for (int j = invokeIndex - 1; j >= 0 && needed > 0; j--) {
            Instruction prev = instructions.get(j);
            if (prev instanceof Ldc) {
                Object cst = ((Ldc) prev).getValue();
                if (cst instanceof Type) {
                    Type t = (Type) cst;
                    if (t.getSort() == Type.OBJECT) {
                        markClassInstantiated(JavascriptNameUtil.sanitizeClassName(t.getInternalName()));
                        needed--;
                    }
                }
            }
        }
    }

    private void handleInvoke(Invoke inv) {
        String owner = JavascriptNameUtil.sanitizeClassName(inv.getOwner());
        switch (inv.getOpcode()) {
            case Opcodes.INVOKESTATIC:
                markClassInstantiated(owner);
                enqueueResolved(owner, inv.getName(), inv.getDesc(), true);
                break;
            case Opcodes.INVOKESPECIAL:
                markClassInstantiated(owner);
                enqueueResolved(owner, inv.getName(), inv.getDesc(), true);
                break;
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE: {
                VirtualCall call = new VirtualCall(owner, inv.getName(), inv.getDesc(),
                        inv.getOpcode() == Opcodes.INVOKEINTERFACE);
                recordPending(call);
                dispatchVirtualFromInstantiated(call);
                break;
            }
            default:
                break;
        }
    }

    private void recordPending(VirtualCall call) {
        List<VirtualCall> list = pendingByReceiver.get(call.receiver);
        if (list == null) {
            list = new ArrayList<VirtualCall>();
            pendingByReceiver.put(call.receiver, list);
        }
        list.add(call);
    }

    private void dispatchVirtualFromInstantiated(VirtualCall call) {
        // If the static receiver type is itself instantiated, dispatch
        // to it first (covers the trivial case where no subtype has
        // been seen yet but the receiver's own method is reachable).
        if (instantiated.contains(call.receiver)) {
            enqueueResolved(call.receiver, call.methodName, call.desc, false);
        }
        Set<String> subtypes = subclassesOf.get(call.receiver);
        if (subtypes != null) {
            for (String sub : subtypes) {
                if (instantiated.contains(sub)) {
                    enqueueResolved(sub, call.methodName, call.desc, false);
                }
                // Transitively walk further subtypes too.
                dispatchVirtualSubtree(sub, call);
            }
        }
    }

    private void dispatchVirtualSubtree(String subtype, VirtualCall call) {
        Set<String> further = subclassesOf.get(subtype);
        if (further == null) {
            return;
        }
        for (String sub : further) {
            if (instantiated.contains(sub)) {
                enqueueResolved(sub, call.methodName, call.desc, false);
            }
            dispatchVirtualSubtree(sub, call);
        }
    }

    /**
     * Walk startClass's inheritance chain to find a concrete
     * (non-abstract, non-eliminated, matching name+desc) method, then
     * enqueue it for liveness. When {@code precise} is true the
     * starting class itself is the resolution target (static/special
     * dispatch); otherwise we still walk up in case the class inherits
     * the concrete impl from an ancestor.
     */
    private void enqueueResolved(String startClass, String methodName, String desc, boolean precise) {
        // BytecodeMethod normalises ``<init>`` / ``<clinit>`` to
        // ``__INIT__`` / ``__CLINIT__`` when it stores the method name,
        // but the Invoke instruction (built from raw ASM callbacks)
        // still holds the angle-bracket form. Normalise to the
        // translator's canonical form before comparing, or ctor /
        // clinit resolutions silently miss and the RTA culls bodies
        // the runtime still references.
        String normalizedName;
        if ("<init>".equals(methodName)) {
            normalizedName = "__INIT__";
        } else if ("<clinit>".equals(methodName)) {
            normalizedName = "__CLINIT__";
        } else {
            normalizedName = methodName;
        }
        String current = startClass;
        Set<String> visited = new HashSet<String>();
        while (current != null && visited.add(current)) {
            ByteCodeClass cls = byName.get(current);
            if (cls == null) {
                return;
            }
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated()) {
                    continue;
                }
                if (!normalizedName.equals(m.getMethodName()) || !desc.equals(m.getSignature())) {
                    continue;
                }
                if (m.isAbstract()) {
                    // Abstract declaration — keep walking up for a
                    // concrete impl (for static/special dispatch this
                    // would be a link error, but the conservative
                    // behaviour is safe).
                    if (precise) {
                        return;
                    }
                    break;
                }
                enqueue(m);
                return;
            }
            String base = cls.getBaseClass();
            current = base == null ? null : JavascriptNameUtil.sanitizeClassName(base);
        }
        // Java 8+ interface default methods: when no concrete impl is
        // found by walking the extends-chain, the dispatch resolves to
        // the (single, by spec) non-abstract method on an implemented
        // interface. RTA must keep that method alive too — otherwise
        // a lambda whose target functional interface only declares an
        // abstract sig (like ``BaseFormatter.process``) but inherits a
        // concrete default method (like ``BaseFormatter.format``) sees
        // the default method culled, and the runtime
        // ``resolveVirtual`` walk turns into ``Missing virtual method``
        // at the call site.
        enqueueInterfaceDefault(startClass, normalizedName, desc, new HashSet<String>());
    }

    /**
     * Walk every interface implemented by {@code clsName} and its
     * supertypes, looking for a concrete (non-abstract,
     * non-eliminated) method matching {@code name + desc}. Enqueues
     * the first match — Java's interface-resolution spec requires at
     * most one maximally-specific concrete default method on the
     * inheritance lattice for any given signature.
     */
    private void enqueueInterfaceDefault(String clsName, String methodName, String desc, Set<String> visited) {
        if (clsName == null || !visited.add(clsName)) {
            return;
        }
        ByteCodeClass cls = byName.get(clsName);
        if (cls == null) {
            return;
        }
        List<String> ifaces = cls.getBaseInterfaces();
        if (ifaces != null) {
            for (String iface : ifaces) {
                String sanitized = JavascriptNameUtil.sanitizeClassName(iface);
                if (enqueueInterfaceMethod(sanitized, methodName, desc, visited)) {
                    return;
                }
            }
        }
        String base = cls.getBaseClass();
        if (base != null) {
            enqueueInterfaceDefault(JavascriptNameUtil.sanitizeClassName(base), methodName, desc, visited);
        }
    }

    private boolean enqueueInterfaceMethod(String ifaceName, String methodName, String desc, Set<String> visited) {
        if (ifaceName == null || !visited.add(ifaceName)) {
            return false;
        }
        ByteCodeClass iface = byName.get(ifaceName);
        if (iface == null) {
            return false;
        }
        for (BytecodeMethod m : iface.getMethods()) {
            if (m.isEliminated() || m.isAbstract()) {
                continue;
            }
            if (methodName.equals(m.getMethodName()) && desc.equals(m.getSignature())) {
                enqueue(m);
                return true;
            }
        }
        List<String> superIfaces = iface.getBaseInterfaces();
        if (superIfaces != null) {
            for (String superIface : superIfaces) {
                if (enqueueInterfaceMethod(JavascriptNameUtil.sanitizeClassName(superIface), methodName, desc, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int eliminate(List<ByteCodeClass> classes) {
        int eliminated = 0;
        for (ByteCodeClass cls : classes) {
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated()) {
                    continue;
                }
                if (live.contains(m)) {
                    continue;
                }
                if (m.isMain()) {
                    continue;
                }
                // Abstract declarations emit no function body and have
                // no runtime cost beyond the classDef entry (they never
                // reach appendMethod). Leave them alone so interface
                // types keep their method list intact for RTTI /
                // dispatch-table consumers.
                if (m.isAbstract()) {
                    continue;
                }
                m.setEliminated(true);
                eliminated++;
            }
        }
        return eliminated;
    }
}
