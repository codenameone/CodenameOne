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

        for (Map.Entry<BytecodeMethod, ByteCodeClass> e : owner.entrySet()) {
            BytecodeMethod m = e.getKey();
            ByteCodeClass c = e.getValue();
            if (isRoot(m, c, nativeSources)) {
                markCalled(m, c, called, live, work, children);
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
                        markCalled(t, owner.get(t), called, live, work, children);
                    }
                }
            }
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

        int eliminated = 0;
        for (Map.Entry<BytecodeMethod, ByteCodeClass> e : owner.entrySet()) {
            BytecodeMethod m = e.getKey();
            if (live.contains(m) || isExempt(e.getValue())) {
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

    private static void markCalled(BytecodeMethod m, ByteCodeClass c, Set<BytecodeMethod> called,
            Set<BytecodeMethod> live, ArrayDeque<BytecodeMethod> work,
            Map<ByteCodeClass, List<ByteCodeClass>> children) {
        ArrayDeque<Object[]> pending = new ArrayDeque<Object[]>();
        pending.add(new Object[] {m, c});
        while (!pending.isEmpty()) {
            Object[] p = pending.poll();
            BytecodeMethod pm = (BytecodeMethod) p[0];
            ByteCodeClass pc = (ByteCodeClass) p[1];
            if (!called.add(pm)) {
                continue;
            }
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
                        pending.add(new Object[] {km, k});
                    }
                }
            }
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
