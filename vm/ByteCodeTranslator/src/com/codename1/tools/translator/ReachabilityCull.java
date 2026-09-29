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
/// every method the native sources name. A MISSED ROOT IS SILENT: a culled method of a
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
        Allocation alloc = nativeHeaders != null
                && !"false".equalsIgnoreCase(Util.getProperty("cn1.cullRta", "true"))
                ? new Allocation(classes, nativeSources, nativeHeaders, children, called, live, work)
                : null;

        for (Map.Entry<BytecodeMethod, ByteCodeClass> e : owner.entrySet()) {
            BytecodeMethod m = e.getKey();
            ByteCodeClass c = e.getValue();
            if (isRoot(m, c, nativeSources)) {
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
                    + (alloc.reflectiveOnly < 0 ? ", Class.newInstance not reachable"
                            : ", " + alloc.reflectiveOnly + " allocated when Class.newInstance became reachable"));
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
            m.setEliminated(true);
            graph.removeMethod(m);
            eliminated++;
        }
        return eliminated;
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
        ArrayDeque<Object[]> pending = new ArrayDeque<Object[]>();
        pending.add(new Object[] {m, c, gated ? Boolean.TRUE : Boolean.FALSE});
        while (!pending.isEmpty()) {
            Object[] p = pending.poll();
            BytecodeMethod pm = (BytecodeMethod) p[0];
            ByteCodeClass pc = (ByteCodeClass) p[1];
            if (called.contains(pm)) {
                continue;
            }
            if (alloc != null && Boolean.TRUE.equals(p[2]) && !alloc.admits(pm, pc)) {
                continue;
            }
            called.add(pm);
            if (live.add(pm)) {
                work.add(pm);
            }
            // isMethodUsedByBaseClassOrInterface: an override is kept when the method it
            // overrides -- matched by name, as the cull matches it -- is called.
            List<ByteCodeClass> kids = pc == null ? null : children.get(pc);
            if (kids == null) {
                continue;
            }
            String name = pm.getMethodName();
            for (ByteCodeClass k : kids) {
                for (BytecodeMethod km : k.getMethods()) {
                    if (!km.isEliminated() && name.equals(km.getMethodName()) && !called.contains(km)) {
                        pending.add(new Object[] {km, k, Boolean.TRUE});
                    }
                }
            }
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
    /// - once Class.newInstance is live, every concrete class with a no-argument
    ///   constructor: Class.forName takes names built at run time (NativeLookup appends
    ///   "Impl", UIBuilder reads them from a resource file, geofence and background
    ///   listeners are stored as strings), and newInstance can only run a no-arg
    ///   constructor, so this bounds everything reflection can create.
    static final class Allocation {
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
        /// Classes nothing but the Class.newInstance seed allocated (at the time it fired).
        int reflectiveOnly = -1;
        private boolean reflective;
        private final Map<ByteCodeClass, List<ByteCodeClass>> children;
        private final Set<BytecodeMethod> called;
        private final Set<BytecodeMethod> live;
        private final ArrayDeque<BytecodeMethod> work;

        Allocation(List<ByteCodeClass> classes, String[] nativeSources, String[] nativeHeaders,
                Map<ByteCodeClass, List<ByteCodeClass>> children,
                Set<BytecodeMethod> called, Set<BytecodeMethod> live, ArrayDeque<BytecodeMethod> work) {
            this.children = children;
            this.called = called;
            this.live = live;
            this.work = work;
            for (ByteCodeClass c : classes) {
                if (!c.isEliminated()) {
                    byName.put(c.getClsName(), c);
                }
            }
            for (String r : RUNTIME_ROOTS) {
                allocate(r);
            }
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
            NativeSymbolIndex natives = new NativeSymbolIndex(all);
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

        /// Every method a call reached whose class was never allocated.
        Set<BytecodeMethod> stillHeld() {
            Set<BytecodeMethod> all = Collections.newSetFromMap(new IdentityHashMap<BytecodeMethod, Boolean>());
            for (Set<BytecodeMethod> h : held.values()) {
                all.addAll(h);
            }
            return all;
        }

        void allocate(String name) {
            if (name == null || !allocated.add(name)) {
                return;
            }
            ByteCodeClass c = byName.get(name);
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
                int before = allocated.size();
                allocateReflectivelyCreatable();
                reflectiveOnly = allocated.size() - before;
            }
            List<com.codename1.tools.translator.bytecodes.Instruction> ins = m.getInstructions();
            if (ins == null) {
                return;
            }
            boolean opaque = false;
            for (com.codename1.tools.translator.bytecodes.Instruction i : ins) {
                Class<?> k = i.getClass();
                if (k == com.codename1.tools.translator.bytecodes.TypeInstruction.class) {
                    if (i.getOpcode() == org.objectweb.asm.Opcodes.NEW) {
                        allocate(mangle(((com.codename1.tools.translator.bytecodes.TypeInstruction) i).getTypeName()));
                    }
                } else if (k == com.codename1.tools.translator.bytecodes.Ldc.class) {
                    Object v = ((com.codename1.tools.translator.bytecodes.Ldc) i).getValue();
                    if (v instanceof org.objectweb.asm.Type) {
                        org.objectweb.asm.Type t = (org.objectweb.asm.Type) v;
                        if (t.getSort() == org.objectweb.asm.Type.OBJECT) {
                            allocate(mangle(t.getInternalName()));
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
                } else if (k != com.codename1.tools.translator.bytecodes.BasicInstruction.class
                        && k != com.codename1.tools.translator.bytecodes.Field.class
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

        private void allocateReflectivelyCreatable() {
            for (ByteCodeClass c : byName.values().toArray(new ByteCodeClass[0])) {
                if (c.isIsInterface() || c.isIsAbstract()) {
                    continue;
                }
                for (BytecodeMethod m : c.getMethods()) {
                    if (!m.isEliminated() && "__INIT__".equals(m.getMethodName())
                            && "()V".equals(m.getSignature())) {
                        allocate(c.getClsName());
                        break;
                    }
                }
            }
        }

        /// "a/b/C$D" or "a.b.C$D" -> "a_b_C_D", the translator's class naming.
        private static String mangle(String internal) {
            return internal.replace('/', '_').replace('.', '_').replace('$', '_');
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
