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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// REACHABILITY FROM THE PROGRAM'S ROOTS, OVER THE CULL'S OWN CALL EDGES.
///
/// The method cull in Parser keeps a method when some surviving method calls it. That
/// is a statement about callers, not about reachability, and it leaves two kinds of
/// dead code behind:
///
/// - Cycles. Two methods that call only each other, or a method that reaches itself
///   through a chain, each have a surviving caller forever.
/// - Long chains. Elimination is greedy and the cull stops after a fixed number of
///   rounds (cullClasses recurses to depth 4), so the tail of a dead chain longer than
///   that survives.
///
/// Both are common in a framework the size of core: a feature package the app never
/// touches (health, home, intents, call, nearby...) is a web of classes calling one
/// another, every one of them "called". Measured on the transpiled Flutter gallery for
/// macOS arm64: 1,065 methods here plus 697 in the class cull that follows, and __text
/// from 23.3MB to 20.3MB.
///
/// This pass answers the question the cull meant to ask. It walks forward from the
/// roots the cull already exempts, along exactly the edges the cull uses -- a call is a
/// lookup signature (`desc.name`) and reaches EVERY method with that signature, in any
/// class, so virtual dispatch, interface dispatch and reflective no-arg construction
/// stay exactly as conservative as they were. Everything it does not reach is
/// eliminated. It can therefore only remove methods the cull would keep, never keep one
/// the cull removes, and it runs after the cull so it starts from what survived.
///
/// What counts as a root is the cull's own list of methods it never eliminates, plus
/// every method the native sources name. With the type-aware cull (Allocation) four
/// things narrow that, each measured on the gallery's iOS build and each with its own
/// integration test: a name inside a port native's C body, or a static C helper, is a
/// call that body makes rather than a root (NativeBodies); a branch a switched-off
/// CN1_INCLUDE_ feature compiles out names nothing (NativeFeatureFilter); a static
/// initializer runs only once its class is touched (Allocation.touch); and a static call
/// reaches only the class it names (StaticCalls). A MISSED ROOT IS SILENT: a culled method of a
/// surviving class is still emitted, as a body that returns 0 (see
/// BytecodeMethod.appendMethodC), so a live call to one compiles, links and answers
/// null. That is why the roots are the cull's own, read from the same predicates, and
/// why nothing here widens what an edge means. Checked on the gallery by listing every
/// stub a live function body calls: the only ones this pass adds are called from
/// interface dispatch thunks, which run only when that interface method is called --
/// and such a call would have kept the implementation.
final class ReachabilityCull {

    private ReachabilityCull() {
    }

    /// Eliminates every method not reachable from a root. Returns the number eliminated.
    static int run(List<ByteCodeClass> classes, MethodDependencyGraph graph, String[] nativeSources) {
        return run(classes, graph, nativeSources, null);
    }

    /// As above; with {@code nativeHeaders} non-null (the hand-written headers beside the
    /// native sources), instance methods are also gated on their class being allocated --
    /// see Allocation. -Dcn1.cullRta=false keeps the signature-only answer.
    static int run(List<ByteCodeClass> classes, MethodDependencyGraph graph, String[] nativeSources,
            String[] nativeHeaders) {
        // Phase times for the verbose log. Both perf-gate arms print them, so a regression
        // in the translator's own cost can be placed without a profiler: on the self-hosted
        // -O3 binary a sampling profile cannot name the functions (see vm/CLAUDE.md).
        long tStart = System.currentTimeMillis();
        // Every candidate method, indexed by the lookup signature a call names.
        Map<String, List<BytecodeMethod>> bySignature = new HashMap<String, List<BytecodeMethod>>();
        Map<BytecodeMethod, ByteCodeClass> owner = new IdentityHashMap<BytecodeMethod, ByteCodeClass>();
        // Classes whose parents (superclass, the superclass's interfaces and the class's
        // own interfaces) include a given class: the reverse of the cull's
        // isMethodUsedByBaseClassOrInterface, which keeps an override by NAME when the
        // declaration it overrides is called.
        Map<ByteCodeClass, List<ByteCodeClass>> children = new IdentityHashMap<ByteCodeClass, List<ByteCodeClass>>();
        for (ByteCodeClass c : classes) {
            if (c.isEliminated()) {
                continue;
            }
            for (BytecodeMethod m : c.getMethods()) {
                if (m.isEliminated()) {
                    continue;
                }
                owner.put(m, c);
                String sig = m.getLookupSignature();
                List<BytecodeMethod> l = bySignature.get(sig);
                if (l == null) {
                    l = new ArrayList<BytecodeMethod>(2);
                    bySignature.put(sig, l);
                }
                l.add(m);
            }
            ByteCodeClass base = c.getBaseClassObject();
            if (base != null) {
                addChild(children, base, c);
                if (base.getBaseInterfacesObject() != null) {
                    for (ByteCodeClass i : base.getBaseInterfacesObject()) {
                        addChild(children, i, c);
                    }
                }
            }
            if (c.getBaseInterfacesObject() != null) {
                for (ByteCodeClass i : c.getBaseInterfacesObject()) {
                    addChild(children, i, c);
                }
            }
        }

        Set<BytecodeMethod> called = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
        Set<BytecodeMethod> live = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
        ArrayDeque<BytecodeMethod> work = new ArrayDeque<BytecodeMethod>();
        // What a Java native's C body names is a call that native makes, followed once it
        // is live, rather than a root: see NativeBodies. Only with the type-aware cull,
        // whose held-method stubs keep such a callee declared for the body that names it.
        NativeBodies scoped = null;
        Map<String, BytecodeMethod> bySymbol = null;
        NativeTokenIndex rootIndex = null;
        long tSetup = System.currentTimeMillis();
        long tBodies = tSetup;
        NativeSymbolIndex fullIndex = null;
        boolean rta = nativeHeaders != null
                && !"false".equalsIgnoreCase(Util.getProperty("cn1.cullRta", "true"));
        if (rta && nativeSources != null && NativeBodies.enabled()) {
            bySymbol = new HashMap<String, BytecodeMethod>();
            Set<String> natives = new java.util.HashSet<String>();
            // The index answers two questions: a method's symbol (isScopedRoot) and a
            // class's name (Allocation). The shorter of the two bounds every query.
            int minQuery = Integer.MAX_VALUE;
            for (ByteCodeClass c : classes) {
                if (!c.isEliminated()) {
                    minQuery = Math.min(minQuery, c.getClsName().length());
                }
            }
            for (BytecodeMethod m : owner.keySet()) {
                String s = symbolOf(m);
                minQuery = Math.min(minQuery, s.length());
                bySymbol.put(s, m);
                if (m.isNative() && !isRuntimeClass(m.getClsName())) {
                    natives.add(s);
                }
            }
            scoped = NativeBodies.parse(nativeSources, natives);
            tBodies = System.currentTimeMillis();
            String[] rootAndHeaders = new String[scoped.rootText.length + nativeHeaders.length];
            System.arraycopy(scoped.rootText, 0, rootAndHeaders, 0, scoped.rootText.length);
            System.arraycopy(nativeHeaders, 0, rootAndHeaders, scoped.rootText.length, nativeHeaders.length);
            rootIndex = new NativeTokenIndex(rootAndHeaders, minQuery);
            fullIndex = Parser.getNativeSymbolIndex(nativeSources);
        }
        long tTokens = System.currentTimeMillis();
        Allocation alloc = rta
                ? new Allocation(classes, scoped != null ? scoped.rootText : nativeSources, nativeHeaders, rootIndex,
                        children, called, live, work)
                : null;
        long tAlloc = System.currentTimeMillis();
        StaticCalls staticCalls = new StaticCalls(classes);
        long tIndex = System.currentTimeMillis();
        CFlow cFlow = scoped != null ? new CFlow(scoped, bySymbol, alloc) : null;

        for (Map.Entry<BytecodeMethod, ByteCodeClass> e : owner.entrySet()) {
            BytecodeMethod m = e.getKey();
            ByteCodeClass c = e.getValue();
            if (alloc != null && alloc.clinitHandled(m, c)) {
                // Live once its class is touched: see Allocation.touch.
                continue;
            }
            if (scoped != null ? isScopedRoot(m, c, rootIndex) : isRoot(m, c, nativeSources)) {
                markCalled(m, c, called, live, work, children, alloc);
            } else if (isExempt(c)) {
                // The cull never eliminates an interface's or Object's methods, so their
                // bodies (default methods, Object's own) are emitted and what they call
                // has to be too. They are live without being "called": the override
                // rule below only follows a declaration something actually calls.
                if (live.add(m)) {
                    work.add(m);
                }
            }
        }

        if (cFlow != null) {
            for (BytecodeMethod t : cFlow.rootHelpers()) {
                markCalled(t, owner.get(t), called, live, work, children, alloc);
            }
        }

        // -Dcn1.reachWhy=com_codename1_home_,... (or CN1_REACHWHY) prints, for a few live
        // methods of each named class prefix, the chain of callers that kept them: the
        // question every "why is this feature in my binary" investigation starts with.
        String why = Util.getProperty("cn1.reachWhy", null);
        Map<BytecodeMethod, BytecodeMethod> via = why == null ? null
                : new IdentityHashMap<BytecodeMethod, BytecodeMethod>();
        while (!work.isEmpty()) {
            BytecodeMethod caller = work.poll();
            if (alloc != null) {
                alloc.scan(caller);
                alloc.scanForName(caller, graph.getCalls(caller));
            }
            if (scoped != null && caller.isNative()) {
                List<NativeBodies.Body> bodies = scoped.natives.get(symbolOf(caller));
                if (bodies != null) {
                    for (NativeBodies.Body b : bodies) {
                        for (BytecodeMethod t : cFlow.follow(b.file, b.text)) {
                            if (via != null && !via.containsKey(t)) {
                                via.put(t, caller);
                            }
                            markCalled(t, owner.get(t), called, live, work, children, alloc);
                        }
                    }
                }
            }
            for (String sig : graph.getCalls(caller)) {
                List<BytecodeMethod> targets = bySignature.get(sig);
                if (targets == null) {
                    continue;
                }
                for (BytecodeMethod t : targets) {
                    if (t == caller || called.contains(t)) {
                        continue;
                    }
                    // The cull's own test for "this caller uses that method".
                    if (caller.isMethodUsed(t)) {
                        if (alloc != null && StaticCalls.ENABLED && t.isStatic() && !staticCalls.reaches(caller, t)) {
                            continue;
                        }
                        if (via != null && !via.containsKey(t)) {
                            via.put(t, caller);
                        }
                        markCalled(t, owner.get(t), called, live, work, children, alloc, true);
                    }
                }
            }
        }
        if (alloc != null && ByteCodeTranslator.verbose) {
            System.out.println("reachability cull: " + alloc.allocated.size() + " classes allocated, "
                    + alloc.parked + " instance methods held for a class nothing allocates"
                    + ", " + alloc.reflectiveOnly + " of them only by what Class.forName may name");
        }
        // Always printed: it is what decides whether the reflection seed is narrow, and
        // TypeAwareCullIntegrationTest reads it.
        if (alloc != null && !alloc.unnarrowedForName.isEmpty()) {
            System.out.println("reachability cull: Class.forName in " + alloc.unnarrowedForName
                    + " is not narrowed, so every concrete class with a no-argument constructor"
                    + " counts as allocated");
        }
        if (via != null) {
            // Tokenised by hand: the translator also runs translated on its own JavaAPI
            // (the self-hosting check), whose String has no split().
            List<String> prefixes = new ArrayList<String>();
            for (int from = 0; from <= why.length(); ) {
                int comma = why.indexOf(',', from);
                int end = comma < 0 ? why.length() : comma;
                if (end > from) {
                    prefixes.add(why.substring(from, end));
                }
                from = end + 1;
            }
            for (String w : prefixes) {
                int shown = 0;
                for (BytecodeMethod m : live) {
                    if (!m.getClsName().startsWith(w) || shown++ > 3) {
                        continue;
                    }
                    StringBuilder chain = new StringBuilder("REACH-WHY " + m.getClsName() + "." + m.getMethodName());
                    BytecodeMethod c = via.get(m);
                    for (int d = 0; c != null && d < 30; d++) {
                        chain.append("\n    <- ").append(c.getClsName()).append('.').append(c.getMethodName());
                        c = via.get(c);
                    }
                    System.out.println(chain);
                }
            }
        }

        // A held method is called by live code, so its call site names its dispatcher and
        // it has to be emitted; nothing allocates its class, so it can never run. Its body
        // becomes the culled stub -- the one CN1_CULL_TRAP makes abort -- and what that
        // body called stops counting as called.
        long tWalk = System.currentTimeMillis();
        Set<BytecodeMethod> stubbed = alloc == null ? Collections.<BytecodeMethod>emptySet()
                : alloc.stillHeld();
        boolean dumpHeld = alloc != null && Util.getProperty("cn1.cullRtaHeld", null) != null;
        int eliminated = 0;
        for (Map.Entry<BytecodeMethod, ByteCodeClass> e : owner.entrySet()) {
            BytecodeMethod m = e.getKey();
            if (live.contains(m) || isExempt(e.getValue())) {
                continue;
            }
            if (stubbed.contains(m)) {
                if (dumpHeld) {
                    System.out.println("RTA-HELD " + m.getClsName() + "." + m.getMethodName() + m.getSignature());
                }
                m.cullBody();
                graph.removeCalls(m);
                eliminated++;
                continue;
            }
            if (scoped != null && fullIndex.contains(symbolOf(m))) {
                // Named by native C that is compiled whether or not it can run -- the body
                // of a native nothing calls. That C still has to find the function, so it
                // stays declared: a native or abstract method as it is, anything else as
                // the culled stub.
                if (!m.isNative() && !m.isAbstract()) {
                    m.cullBody();
                    graph.removeCalls(m);
                    eliminated++;
                }
                continue;
            }
            if (scoped != null && m.isNative()) {
                // Unnamed by the natives it is still declared in the class header, which
                // is harmless, and eliminating a native changes nothing that is emitted.
                continue;
            }
            m.setEliminated(true);
            graph.removeMethod(m);
            eliminated++;
        }
        if (ByteCodeTranslator.verbose) {
            System.out.println("reachability cull phases (ms): setup " + (tSetup - tStart)
                    + ", symbols + native bodies " + (tBodies - tSetup)
                    + ", token index " + (tTokens - tBodies)
                    + ", allocation " + (tAlloc - tTokens)
                    + ", static calls " + (tIndex - tAlloc)
                    + ", walk " + (tWalk - tIndex)
                    + ", eliminate " + (System.currentTimeMillis() - tWalk));
        }
        return eliminated;
    }

    /// Follows the C side of NativeBodies: what a live body names -- Java methods, returned
    /// to be marked called; classes, allocated; and the static helpers of its own file,
    /// whose bodies are followed in turn, once each.
    static final class CFlow {
        private final NativeBodies scoped;
        private final Map<String, BytecodeMethod> bySymbol;
        private final Allocation alloc;
        private final Set<String> liveHelpers = new java.util.HashSet<String>();

        CFlow(NativeBodies scoped, Map<String, BytecodeMethod> bySymbol, Allocation alloc) {
            this.scoped = scoped;
            this.bySymbol = bySymbol;
            this.alloc = alloc;
        }

        /// The helpers each file's own root text names are live from the start.
        List<BytecodeMethod> rootHelpers() {
            List<BytecodeMethod> out = new ArrayList<BytecodeMethod>();
            for (int f = 0; f < scoped.rootText.length; f++) {
                Map<String, String> helpers = scoped.helpers.get(f);
                if (helpers.isEmpty() || scoped.rootText[f] == null) {
                    continue;
                }
                Set<String> tokens = tokens(scoped.rootText[f]);
                for (String h : helpers.keySet()) {
                    if (tokens.contains(h)) {
                        out.addAll(follow(f, "", h));
                    }
                }
            }
            return out;
        }

        List<BytecodeMethod> follow(int file, String body) {
            return follow(file, body, null);
        }

        private List<BytecodeMethod> follow(int file, String body, String helper) {
            List<BytecodeMethod> out = new ArrayList<BytecodeMethod>();
            Map<String, String> helpers = scoped.helpers.get(file);
            ArrayDeque<String> pending = new ArrayDeque<String>();
            pending.add(body);
            if (helper != null) {
                enqueueHelper(file, helper, helpers, pending);
            }
            while (!pending.isEmpty()) {
                String text = pending.poll();
                for (String token : tokens(text)) {
                    if (helpers.containsKey(token)) {
                        enqueueHelper(file, token, helpers, pending);
                    }
                    for (int s = 0; s < token.length(); s++) {
                        if (s > 0 && token.charAt(s - 1) != '_') {
                            continue;
                        }
                        // A name at any `_` boundary: `virtual_X_m__`, `__NEW_X` and
                        // `class__X` all count, as they did when every name was a root.
                        BytecodeMethod m = bySymbol.get(token.substring(s));
                        if (m != null) {
                            out.add(m);
                        }
                        for (int e = s + 1; e <= token.length(); e++) {
                            if (e == token.length() || token.charAt(e) == '_') {
                                alloc.allocateIfClass(token.substring(s, e));
                            }
                        }
                    }
                }
            }
            return out;
        }

        private void enqueueHelper(int file, String name, Map<String, String> helpers, ArrayDeque<String> pending) {
            if (liveHelpers.add(file + ":" + name)) {
                pending.add(helpers.get(name));
            }
        }

        private static Set<String> tokens(String text) {
            Set<String> out = new java.util.HashSet<String>();
            int n = text.length();
            int i = 0;
            while (i < n) {
                if (!NativeBodies.isIdentifierChar(text.charAt(i))) {
                    i++;
                    continue;
                }
                int j = i + 1;
                while (j < n && NativeBodies.isIdentifierChar(text.charAt(j))) {
                    j++;
                }
                out.add(text.substring(i, j));
                i = j;
            }
            return out;
        }
    }

    /// Which static methods a caller can reach. A call is matched by lookup signature, so
    /// a live `Util.split(String)` kept every static `split(String)` in the program --
    /// the natives' callback classes each have one. A static call names its class, and
    /// resolves to that class or one of its superclasses, so that is all it reaches.
    ///
    /// A name the caller reaches through anything but a plain or custom invoke (a fused
    /// or rewritten instruction) keeps the signature rule, as does a call to a class this
    /// program does not have.
    static final class StaticCalls {
        /// -Dcn1.cullStaticOwner=false matches static calls by signature alone again.
        static final boolean ENABLED =
                !"false".equalsIgnoreCase(Util.getProperty("cn1.cullStaticOwner", "true"));
        private final Map<String, ByteCodeClass> byName = new HashMap<String, ByteCodeClass>();
        private final Map<BytecodeMethod, Object> perCaller = new IdentityHashMap<BytecodeMethod, Object>();
        private final Map<BytecodeMethod, String> keyOf = new IdentityHashMap<BytecodeMethod, String>();
        private final Map<String, String> mangled = new HashMap<String, String>();

        StaticCalls(List<ByteCodeClass> classes) {
            for (ByteCodeClass c : classes) {
                if (!c.isEliminated()) {
                    byName.put(c.getClsName(), c);
                }
            }
        }

        @SuppressWarnings("unchecked")
        boolean reaches(BytecodeMethod caller, BytecodeMethod target) {
            Object o = perCaller.get(caller);
            if (o == null) {
                o = index(caller);
                perCaller.put(caller, o);
            }
            String key = keyOf.get(target);
            if (key == null) {
                key = target.getMethodName() + target.getSignature();
                keyOf.put(target, key);
            }
            List<String> owners = ((Map<String, List<String>>) o).get(key);
            if (owners == null) {
                // Only virtual, special or interface calls name it: none reaches a static.
                return false;
            }
            if (owners.contains(null)) {
                // An instruction naming the method some other way (a fused or rewritten
                // one) keeps the signature rule.
                return true;
            }
            for (String owner : owners) {
                String name = mangled.get(owner);
                if (name == null) {
                    name = Allocation.mangleName(owner);
                    mangled.put(owner, name);
                }
                ByteCodeClass c = byName.get(name);
                if (c == null) {
                    return true;
                }
                while (c != null) {
                    if (c.getClsName().equals(target.getClsName())) {
                        return true;
                    }
                    c = c.getBaseClassObject();
                }
            }
            return false;
        }

        /// Per name and descriptor the caller's instructions name, the owners of its static
        /// calls. It mirrors what BytecodeMethod.isMethodUsed matches -- an instruction's
        /// getMethodName and getSignature -- so an instruction that names a method any other
        /// way (a fused or rewritten one) records a null owner, which keeps the signature
        /// rule for that name. Exact classes: a subclass of Invoke may call out on its own.
        private static Object index(BytecodeMethod caller) {
            Map<String, List<String>> out = new HashMap<String, List<String>>();
            List<com.codename1.tools.translator.bytecodes.Instruction> ins = caller.getInstructions();
            if (ins == null) {
                return out;
            }
            for (com.codename1.tools.translator.bytecodes.Instruction i : ins) {
                String name = i.getMethodName();
                if (name == null) {
                    continue;
                }
                String key = name + i.getSignature();
                String owner;
                boolean plain;
                if (i.getClass() == com.codename1.tools.translator.bytecodes.Invoke.class) {
                    owner = ((com.codename1.tools.translator.bytecodes.Invoke) i).getOwner();
                    plain = true;
                } else if (i.getClass() == com.codename1.tools.translator.bytecodes.CustomInvoke.class) {
                    owner = ((com.codename1.tools.translator.bytecodes.CustomInvoke) i).getOwner();
                    plain = true;
                } else {
                    owner = null;
                    plain = false;
                }
                int op = i.getOpcode();
                if (plain && (op == com.codename1.tools.translator.classfile.Opcodes.INVOKEVIRTUAL || op == com.codename1.tools.translator.classfile.Opcodes.INVOKESPECIAL
                        || op == com.codename1.tools.translator.classfile.Opcodes.INVOKEINTERFACE)) {
                    // A virtual, special or interface call never reaches a static method.
                    continue;
                }
                if (plain && op != com.codename1.tools.translator.classfile.Opcodes.INVOKESTATIC) {
                    // invokedynamic and anything else: no owner to go by.
                    plain = false;
                }
                List<String> l = out.get(key);
                if (l == null) {
                    l = new ArrayList<String>(1);
                    out.put(key, l);
                }
                l.add(plain ? owner : null);
            }
            return out;
        }
    }

    /// The C symbol a native source names a method by.
    static String symbolOf(BytecodeMethod m) {
        StringBuilder b = new StringBuilder();
        m.appendFunctionPointer(b);
        return b.toString();
    }

    /// The VM's own class library. Its natives stay roots, as every native used to be: the
    /// generated code and the runtime's C reach some of them without a bytecode call the
    /// cull could follow, so their bodies are not scoped.
    private static boolean isRuntimeClass(String clsName) {
        return clsName.startsWith("java_") || clsName.startsWith("javax_");
    }

    /// isRoot, with only the natives' text outside a port's Java native bodies rooting
    /// anything, and such a native itself live only once something calls it.
    private static boolean isScopedRoot(BytecodeMethod m, ByteCodeClass c, NativeTokenIndex rootIndex) {
        String name = m.getMethodName();
        if (m.isMain() || "__CLINIT__".equals(name) || "finalize".equals(name)) {
            return true;
        }
        if (m.isNative() && isRuntimeClass(c.getClsName())) {
            return true;
        }
        if (JavascriptNativeRegistry.isRuntimeDelegateTarget(c.getClsName(), name)) {
            return true;
        }
        if (BytecodeMethod.isOnDeviceDebug() && "java_lang_Object".equals(c.getClsName()) && !m.isStatic()) {
            return true;
        }
        return rootIndex.contains(symbolOf(m));
    }

    private static void addChild(Map<ByteCodeClass, List<ByteCodeClass>> children, ByteCodeClass parent,
            ByteCodeClass child) {
        List<ByteCodeClass> l = children.get(parent);
        if (l == null) {
            l = new ArrayList<ByteCodeClass>(2);
            children.put(parent, l);
        }
        l.add(child);
    }

    /// A root: live whether or not its class is allocated (the native side may call it).
    private static void markCalled(BytecodeMethod m, ByteCodeClass c, Set<BytecodeMethod> called,
            Set<BytecodeMethod> live, ArrayDeque<BytecodeMethod> work,
            Map<ByteCodeClass, List<ByteCodeClass>> children, Allocation alloc) {
        markCalled(m, c, called, live, work, children, alloc, false);
    }

    /// {@code gated}: an instance method reached through a call is live only once its class,
    /// or a subclass that inherits it, is allocated; until then Allocation holds it and
    /// releases it the moment one is. Overrides reached from a called declaration are
    /// always gated.
    static void markCalled(BytecodeMethod m, ByteCodeClass c, Set<BytecodeMethod> called,
            Set<BytecodeMethod> live, ArrayDeque<BytecodeMethod> work,
            Map<ByteCodeClass, List<ByteCodeClass>> children, Allocation alloc, boolean gated) {
        // Most calls change nothing -- the target is already called, or held -- so the
        // queue of overrides to follow is only created once there is one. Overrides are
        // always gated; the order they are visited in is first in, first out either way.
        ArrayDeque<Object[]> pending = null;
        BytecodeMethod pm = m;
        ByteCodeClass pc = c;
        boolean pg = gated;
        while (true) {
            if (!called.contains(pm) && (alloc == null || !pg || alloc.admits(pm, pc))) {
                called.add(pm);
                if (live.add(pm)) {
                    work.add(pm);
                }
                // isMethodUsedByBaseClassOrInterface: an override is kept when the method it
                // overrides -- matched by name, as the cull matches it -- is called.
                List<ByteCodeClass> kids = pc == null ? null : children.get(pc);
                if (kids != null) {
                    String name = pm.getMethodName();
                    for (ByteCodeClass k : kids) {
                        for (BytecodeMethod km : k.getMethods()) {
                            if (!km.isEliminated() && name.equals(km.getMethodName()) && !called.contains(km)) {
                                if (pending == null) {
                                    pending = new ArrayDeque<Object[]>();
                                }
                                pending.add(new Object[] {km, k});
                            }
                        }
                    }
                }
            }
            if (pending == null || pending.isEmpty()) {
                return;
            }
            Object[] p = pending.poll();
            pm = (BytecodeMethod) p[0];
            pc = (ByteCodeClass) p[1];
            pg = true;
        }
    }

    /// WHICH CLASSES THE PROGRAM CAN HOLD AN INSTANCE OF, AND WHAT THAT UNLOCKS.
    ///
    /// A call is matched by lookup signature, so `x.toString()` reaches every toString in
    /// the program and `new Foo()` every no-argument constructor. An instance method runs
    /// only on an instance, though, so here it is live only once its class -- or a subclass
    /// that inherits it -- is ALLOCATED; until then it is held, and released the moment one
    /// is. Static methods and initializers keep the signature rule.
    ///
    /// Allocated is every way an object comes into being, and the list is deliberately wide,
    /// because a missed one culls live code into a stub that returns 0 (run with
    /// CN1_CULL_TRAP to make such a stub abort instead):
    ///
    /// - a NEW in a live method;
    /// - a class literal in a live method -- NativeLookup.register, Util.register,
    ///   LookAndFeel.getMenuBarClass(), anything handed to newInstance();
    /// - a string constant spelling a class's binary name -- Class.forName("a.B");
    /// - every class named in the hand-written native sources or headers -- the C runtime
    ///   allocates exceptions, strings and port objects directly (__NEW_X, class__X);
    /// - the runtime roots the C side creates without naming them in any file scanned here;
    /// - every class a live method with a fused or custom instruction depends on, since a
    ///   rewritten instruction can hide its allocation from the scan;
    /// - what Class.forName can hand to Class.newInstance, once both are live -- see
    ///   FOR_NAME_SITES.
    ///
    /// REFLECTION. newInstance can only instantiate a Class object the program holds, and
    /// there are three ways to hold one: a class literal (allocated above), getClass() on
    /// an instance (its class is allocated already -- it made the instance) and
    /// Class.forName, which takes a string built at run time. getSuperclass() yields an
    /// ancestor of an allocated class, whose methods are already admitted through the
    /// subtree. So only forName needs a seed, and each live method that calls it gets one:
    /// a known site gets every concrete no-arg class assignable to the one type its result
    /// is used as, and any other caller -- an app's, a cn1lib's -- gets every concrete
    /// no-arg class, since nothing bounds its names.
    static final class Allocation {
        /// {class, method, type}: a live forName call in that method only ever feeds a
        /// newInstance whose result is used as that type. Each is also named at the call
        /// site, so an edit there can see this table. A method missing from here is not
        /// unsafe, only unnarrowed: it falls back to every concrete no-arg class.
        private static final String[][] FOR_NAME_SITES = {
            // NativeLookup.create: forName(c.getName() + "Impl"), c a NativeInterface.
            {"com_codename1_system_NativeLookup", "create", "com_codename1_system_NativeInterface"},
            // GeofenceManager.getListenerClass: the persisted listener class name.
            {"com_codename1_location_GeofenceManager", "getListenerClass", "com_codename1_location_GeofenceListener"},
            // DeviceRunner.runTest: the test class the device-side runner is handed.
            {"com_codename1_testing_DeviceRunner", "runTest", "com_codename1_testing_UnitTest"},
            // IOSImplementation.Loc: the persisted background location listener, and the
            // per-region geofence listener names.
            {"com_codename1_impl_ios_IOSImplementation_Loc", "getBackgroundLocationListener",
                    "com_codename1_location_LocationListener"},
            {"com_codename1_impl_ios_IOSImplementation_Loc", "getGeofenceListener",
                    "com_codename1_location_GeofenceListener"},
            // IOSImplementation.runBackgroundProcessing: the persisted BackgroundWorker.
            {"com_codename1_impl_ios_IOSImplementation", "runBackgroundProcessing",
                    "com_codename1_background_BackgroundWorker"},
        };
        private static final String FOR_NAME = "(Ljava/lang/String;)Ljava/lang/Class;.forName";
        private static final String FOR_NAME_3 = "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;.forName";

        private static final String[] RUNTIME_ROOTS = {
            "java_lang_Object", "java_lang_Class", "java_lang_String", "java_lang_Boolean",
            "java_lang_Integer", "java_lang_Byte", "java_lang_Short", "java_lang_Character",
            "java_lang_Long", "java_lang_Double", "java_lang_Float", "java_lang_Thread",
            "java_lang_StackOverflowError", "java_lang_OutOfMemoryError",
            "java_lang_NullPointerException", "java_lang_ArrayIndexOutOfBoundsException",
            "java_lang_ArithmeticException", "java_lang_ClassCastException",
            "java_lang_NegativeArraySizeException", "java_lang_ArrayStoreException",
            "java_lang_InterruptedException", "java_lang_IllegalArgumentException",
            "java_lang_IllegalStateException", "java_lang_RuntimeException",
            "java_lang_Throwable", "java_lang_Error", "java_lang_Exception"
        };

        private final Map<String, ByteCodeClass> byName = new HashMap<String, ByteCodeClass>();
        final Set<String> allocated = new java.util.HashSet<String>();
        /// Classes with an allocated class at or below them.
        private final Set<ByteCodeClass> subtree = Collections.newSetFromMap(
                new IdentityHashMap<ByteCodeClass, Boolean>());
        private final Map<ByteCodeClass, Set<BytecodeMethod>> held =
                new IdentityHashMap<ByteCodeClass, Set<BytecodeMethod>>();
        int parked;
        /// Classes the forName seeds allocated that nothing had allocated before them.
        int reflectiveOnly;
        /// Live methods calling Class.forName that FOR_NAME_SITES does not narrow.
        final List<String> unnarrowedForName = new ArrayList<String>();
        private boolean reflective;
        private boolean everything;
        /// -Dcn1.cullClinit=false makes every static initializer a root again.
        static final boolean CONDITIONAL_CLINIT =
                !"false".equalsIgnoreCase(Util.getProperty("cn1.cullClinit", "true"));
        private final Map<ByteCodeClass, BytecodeMethod> clinitOf =
                new IdentityHashMap<ByteCodeClass, BytecodeMethod>();
        private final Set<ByteCodeClass> touched = Collections.newSetFromMap(
                new IdentityHashMap<ByteCodeClass, Boolean>());
        private final List<BytecodeMethod> forNameSites = new ArrayList<BytecodeMethod>();
        /// mangle() memoized: the scan of every live method mangles the same few hundred
        /// class names over and over, three String copies each.
        private final Map<String, String> mangled = new HashMap<String, String>();
        private final Map<ByteCodeClass, List<ByteCodeClass>> children;
        private final Set<BytecodeMethod> called;
        private final Set<BytecodeMethod> live;
        private final ArrayDeque<BytecodeMethod> work;

        /// @param index the index over exactly `nativeSources` plus `nativeHeaders` when the
        ///     caller already built one -- the scoped root index is that same text -- or null
        Allocation(List<ByteCodeClass> classes, String[] nativeSources, String[] nativeHeaders,
                NativeTokenIndex index, Map<ByteCodeClass, List<ByteCodeClass>> children,
                Set<BytecodeMethod> called, Set<BytecodeMethod> live, ArrayDeque<BytecodeMethod> work) {
            this.children = children;
            this.called = called;
            this.live = live;
            this.work = work;
            for (ByteCodeClass c : classes) {
                if (!c.isEliminated()) {
                    byName.put(c.getClsName(), c);
                    if (CONDITIONAL_CLINIT && !isExempt(c)) {
                        for (BytecodeMethod m : c.getMethods()) {
                            if (!m.isEliminated() && "__CLINIT__".equals(m.getMethodName())) {
                                clinitOf.put(c, m);
                            }
                        }
                    }
                }
            }
            for (String r : RUNTIME_ROOTS) {
                allocate(r);
            }
            NativeTokenIndex natives = index;
            if (natives == null) {
                String[] all = new String[(nativeSources == null ? 0 : nativeSources.length) + nativeHeaders.length];
                int n = 0;
                if (nativeSources != null) {
                    for (String s : nativeSources) {
                        all[n++] = s;
                    }
                }
                for (String s : nativeHeaders) {
                    all[n++] = s;
                }
                int minQuery = Integer.MAX_VALUE;
                for (ByteCodeClass c : classes) {
                    if (!c.isEliminated()) {
                        minQuery = Math.min(minQuery, c.getClsName().length());
                    }
                }
                natives = new NativeTokenIndex(all, minQuery);
            }
            for (ByteCodeClass c : classes) {
                if (!c.isEliminated() && natives.contains(c.getClsName())) {
                    allocate(c.getClsName());
                }
            }
        }

        /// Whether a gated method may be marked called now; holds it otherwise.
        boolean admits(BytecodeMethod m, ByteCodeClass c) {
            if (m.isStatic() || c == null || isExempt(c) || subtree.contains(c)) {
                return true;
            }
            Set<BytecodeMethod> h = held.get(c);
            if (h == null) {
                h = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
                held.put(c, h);
            }
            if (h.add(m)) {
                parked++;
            }
            return false;
        }

        void allocateIfClass(String name) {
            if (byName.containsKey(name)) {
                allocate(name);
            }
        }

        /// Every method a call reached whose class was never allocated.
        Set<BytecodeMethod> stillHeld() {
            Set<BytecodeMethod> all = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
            for (Set<BytecodeMethod> h : held.values()) {
                all.addAll(h);
            }
            return all;
        }

        /// A static initializer runs on the first use of its class's own statics, static
        /// methods or allocation (ParparVM never runs a superclass's from a subclass's, but
        /// Java does, so the superclasses are touched too). With CONDITIONAL_CLINIT a
        /// <clinit> is live only once its class is touched this way, instead of being a
        /// root: a class the program keeps only for a native's sake no longer runs, and
        /// keeps, everything its initializer names -- the dead-code guards of the
        /// IOS*Callbacks classes, for one, which call every callback from <clinit>.
        void touch(ByteCodeClass c) {
            while (c != null && touched.add(c)) {
                BytecodeMethod m = clinitOf.get(c);
                if (m != null) {
                    markCalled(m, c, called, live, work, children, this);
                }
                c = c.getBaseClassObject();
            }
        }

        boolean clinitHandled(BytecodeMethod m, ByteCodeClass c) {
            return c != null && clinitOf.get(c) == m;
        }

        void allocate(String name) {
            if (name == null || !allocated.add(name)) {
                return;
            }
            ByteCodeClass c = byName.get(name);
            touch(c);
            ArrayDeque<ByteCodeClass> up = new ArrayDeque<ByteCodeClass>();
            if (c != null) {
                up.add(c);
            }
            while (!up.isEmpty()) {
                ByteCodeClass a = up.poll();
                if (!subtree.add(a)) {
                    continue;
                }
                Set<BytecodeMethod> h = held.remove(a);
                if (h != null) {
                    parked -= h.size();
                    for (BytecodeMethod m : h) {
                        markCalled(m, a, called, live, work, children, this, true);
                    }
                }
                if (a.getBaseClassObject() != null) {
                    up.add(a.getBaseClassObject());
                }
                if (a.getBaseInterfacesObject() != null) {
                    for (ByteCodeClass i : a.getBaseInterfacesObject()) {
                        up.add(i);
                    }
                }
            }
        }

        /// The allocations a live method performs.
        void scan(BytecodeMethod m) {
            if (!reflective && "java_lang_Class".equals(m.getClsName())
                    && "newInstance".equals(m.getMethodName())) {
                reflective = true;
                for (BytecodeMethod site : forNameSites) {
                    seedForNameSite(site);
                }
            }
            // Running any of a class's code means the class may be initialized.
            touch(byName.get(m.getClsName()));
            List<com.codename1.tools.translator.bytecodes.Instruction> ins = m.getInstructions();
            if (ins == null) {
                return;
            }
            boolean opaque = false;
            for (com.codename1.tools.translator.bytecodes.Instruction i : ins) {
                Class<?> k = i.getClass();
                if (k == com.codename1.tools.translator.bytecodes.TypeInstruction.class) {
                    if (i.getOpcode() == com.codename1.tools.translator.classfile.Opcodes.NEW) {
                        allocate(mangleCached(((com.codename1.tools.translator.bytecodes.TypeInstruction) i).getTypeName()));
                    }
                } else if (k == com.codename1.tools.translator.bytecodes.Ldc.class) {
                    Object v = ((com.codename1.tools.translator.bytecodes.Ldc) i).getValue();
                    if (v instanceof com.codename1.tools.translator.classfile.Type) {
                        com.codename1.tools.translator.classfile.Type t = (com.codename1.tools.translator.classfile.Type) v;
                        if (t.getSort() == com.codename1.tools.translator.classfile.Type.OBJECT) {
                            allocate(mangleCached(t.getInternalName()));
                        }
                    } else if (v instanceof String) {
                        String s = (String) v;
                        if (s.length() > 0 && s.length() < 256 && s.indexOf(' ') < 0) {
                            String cn = mangle(s);
                            if (byName.containsKey(cn)) {
                                allocate(cn);
                            }
                        }
                    }
                } else if (k == com.codename1.tools.translator.bytecodes.Field.class) {
                    int op = i.getOpcode();
                    if (op == com.codename1.tools.translator.classfile.Opcodes.GETSTATIC || op == com.codename1.tools.translator.classfile.Opcodes.PUTSTATIC) {
                        touch(byName.get(mangleCached(((com.codename1.tools.translator.bytecodes.Field) i).getOwner())));
                    }
                } else if (k != com.codename1.tools.translator.bytecodes.BasicInstruction.class
                        && k != com.codename1.tools.translator.bytecodes.Invoke.class
                        && k != com.codename1.tools.translator.bytecodes.LabelInstruction.class
                        && k != com.codename1.tools.translator.bytecodes.Jump.class
                        && k != com.codename1.tools.translator.bytecodes.VarOp.class
                        && k != com.codename1.tools.translator.bytecodes.IInc.class
                        && k != com.codename1.tools.translator.bytecodes.SwitchInstruction.class
                        && k != com.codename1.tools.translator.bytecodes.LineNumber.class
                        && k != com.codename1.tools.translator.bytecodes.LocalVariable.class
                        && k != com.codename1.tools.translator.bytecodes.TryCatch.class
                        && k != com.codename1.tools.translator.bytecodes.MultiArray.class) {
                    opaque = true;
                }
            }
            if (opaque) {
                for (String d : m.getDependentClasses()) {
                    allocate(d);
                }
            }
        }

        /// Records a live method that calls Class.forName, and seeds it once newInstance
        /// is live too. Class's own overloads delegate to each other and are skipped: their
        /// callers are the sites.
        void scanForName(BytecodeMethod m, Set<String> calls) {
            if ("java_lang_Class".equals(m.getClsName())
                    || !(calls.contains(FOR_NAME) || calls.contains(FOR_NAME_3))) {
                return;
            }
            forNameSites.add(m);
            if (reflective) {
                seedForNameSite(m);
            }
        }

        private void seedForNameSite(BytecodeMethod site) {
            String type = null;
            for (String[] s : FOR_NAME_SITES) {
                if (s[0].equals(site.getClsName()) && s[1].equals(site.getMethodName())) {
                    type = s[2];
                    break;
                }
            }
            if (type == null) {
                List<String> literals = literalForNameTargets(site);
                if (literals != null) {
                    // Every forName call in this method names a compile-time constant --
                    // see literalForNameTargets -- so nothing here is actually unbounded;
                    // allocate exactly the named classes (narrowing absent ones to nothing)
                    // and skip the FOR_NAME_SITES fallback below entirely.
                    int before = allocated.size();
                    for (String literal : literals) {
                        String cn = mangle(literal);
                        if (byName.containsKey(cn)) {
                            allocate(cn);
                        }
                        // Else: forNameImpl (nativeMethods.m) has no entry for a class
                        // outside the translated universe, forName() throws
                        // ClassNotFoundException, and newInstance() never runs -- this
                        // literal contributes no allocation, not "every no-arg class".
                    }
                    reflectiveOnly += allocated.size() - before;
                    return;
                }
                unnarrowedForName.add(site.getClsName() + "." + site.getMethodName());
            }
            if (everything) {
                return;
            }
            everything = type == null;
            int before = allocated.size();
            for (ByteCodeClass c : byName.values().toArray(new ByteCodeClass[0])) {
                if (c.isIsInterface() || c.isIsAbstract() || !hasNoArgConstructor(c)) {
                    continue;
                }
                if (type == null || isAssignable(c, type)) {
                    allocate(c.getClsName());
                }
            }
            reflectiveOnly += allocated.size() - before;
        }

        /// Every Class.forName call this method's own bytecode makes, or null if any of
        /// them cannot be proven constant. A call counts only when the instruction right
        /// before it (skipping label/line-number/debug pseudo-instructions, which have no
        /// stack effect) is an Ldc of a String -- the shape
        /// kotlin.jvm.internal.Reflection's own `static {}` uses to load
        /// "kotlin.reflect.jvm.internal.ReflectionFactoryImpl": `ldc "..."; invokestatic
        /// Class.forName`. kotlin-reflect is never bundled, so that name is never part of
        /// the translated universe, and without this, seedForNameSite had no way to tell
        /// "this name can never resolve" from "this name is unknown", and widened to every
        /// concrete no-arg class for every app that merely links the Kotlin stdlib. This
        /// stays deliberately narrow -- a 3-arg forName(String,boolean,ClassLoader) call
        /// never matches, because its ClassLoader argument sits between the Ldc and the
        /// invoke -- rather than growing into general constant propagation.
        private static List<String> literalForNameTargets(BytecodeMethod site) {
            List<com.codename1.tools.translator.bytecodes.Instruction> ins = site.getInstructions();
            if (ins == null) {
                return null;
            }
            List<String> literals = new ArrayList<String>();
            com.codename1.tools.translator.bytecodes.Instruction prev = null;
            boolean sawForName = false;
            for (com.codename1.tools.translator.bytecodes.Instruction i : ins) {
                Class<?> k = i.getClass();
                if (k == com.codename1.tools.translator.bytecodes.LabelInstruction.class
                        || k == com.codename1.tools.translator.bytecodes.LineNumber.class
                        || k == com.codename1.tools.translator.bytecodes.LocalVariable.class
                        || k == com.codename1.tools.translator.bytecodes.TryCatch.class) {
                    continue;
                }
                if (k == com.codename1.tools.translator.bytecodes.Invoke.class) {
                    com.codename1.tools.translator.bytecodes.Invoke call =
                            (com.codename1.tools.translator.bytecodes.Invoke) i;
                    String sig = call.getDesc() + "." + call.getName();
                    if (FOR_NAME.equals(sig) || FOR_NAME_3.equals(sig)) {
                        sawForName = true;
                        if (prev == null || prev.getClass() != com.codename1.tools.translator.bytecodes.Ldc.class) {
                            return null;
                        }
                        Object v = ((com.codename1.tools.translator.bytecodes.Ldc) prev).getValue();
                        if (!(v instanceof String)) {
                            return null;
                        }
                        literals.add((String) v);
                    }
                }
                prev = i;
            }
            return sawForName ? literals : null;
        }

        private static boolean hasNoArgConstructor(ByteCodeClass c) {
            for (BytecodeMethod m : c.getMethods()) {
                if (!m.isEliminated() && "__INIT__".equals(m.getMethodName())
                        && "()V".equals(m.getSignature())) {
                    return true;
                }
            }
            return false;
        }

        /// Whether {@code c} is, extends or implements the class named {@code type}.
        private static boolean isAssignable(ByteCodeClass c, String type) {
            ArrayDeque<ByteCodeClass> up = new ArrayDeque<ByteCodeClass>();
            Set<ByteCodeClass> seen = Collections.newSetFromMap(new IdentityHashMap<ByteCodeClass, Boolean>());
            up.add(c);
            while (!up.isEmpty()) {
                ByteCodeClass a = up.poll();
                if (!seen.add(a)) {
                    continue;
                }
                if (type.equals(a.getClsName())) {
                    return true;
                }
                if (a.getBaseClassObject() != null) {
                    up.add(a.getBaseClassObject());
                }
                if (a.getBaseInterfacesObject() != null) {
                    for (ByteCodeClass i : a.getBaseInterfacesObject()) {
                        up.add(i);
                    }
                }
            }
            return false;
        }

        static String mangleName(String internal) {
            return mangle(internal);
        }

        private String mangleCached(String internal) {
            String m = mangled.get(internal);
            if (m == null) {
                m = mangle(internal);
                mangled.put(internal, m);
            }
            return m;
        }

        /// "a/b/C$D" or "a.b.C$D" -> "a_b_C_D", the translator's class naming. It must fold every
        /// character the other manglers fold, '-' included (Kotlin lambdas, renamed classes): a
        /// raw name that misses here is never marked allocated, and every instance method reached
        /// through it is emitted as the CN1_CULL_TRAP stub (HyphenatedNamesIntegrationTest).
        private static String mangle(String internal) {
            return internal.replace('/', '_').replace('.', '_').replace('$', '_').replace('-', '_');
        }
    }

    /// The cull never looks at these classes' methods: see Parser.cullMethods.
    private static boolean isExempt(ByteCodeClass c) {
        return c.isIsInterface() || c.getBaseClass() == null;
    }

    /// The methods Parser.cullMethods never eliminates, plus what the natives name.
    private static boolean isRoot(BytecodeMethod m, ByteCodeClass c, String[] nativeSources) {
        String name = m.getMethodName();
        if (m.isMain() || "__CLINIT__".equals(name) || "finalize".equals(name) || m.isNative()) {
            return true;
        }
        if (JavascriptNativeRegistry.isRuntimeDelegateTarget(c.getClsName(), name)) {
            return true;
        }
        if (BytecodeMethod.isOnDeviceDebug() && "java_lang_Object".equals(c.getClsName()) && !m.isStatic()) {
            return true;
        }
        return m.isMethodUsedByNative(nativeSources, c);
    }
}
