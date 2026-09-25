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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import com.codename1.tools.translator.bytecodes.LabelInstruction;

/**
 *
 * @author Shai Almog
 */
public class Parser extends ClassVisitor {
    private static final String DISABLE_DEBUG_INFO_ANNOTATION = "Lcom/codename1/annotations/DisableDebugInfo;";
    private static final String DISABLE_NULL_AND_ARRAY_BOUNDS_CHECKS_ANNOTATION =
            "Lcom/codename1/annotations/DisableNullChecksAndArrayBoundsChecks;";
    private static final String CONCRETE_ANNOTATION = "Lcom/codename1/annotations/Concrete;";
    private static final String STACK_ALLOCATE_ANNOTATION = "Lcom/codename1/annotations/StackAllocate;";
    private static final String FUSED_ANNOTATION = "Lcom/codename1/annotations/Fused;";
    private ByteCodeClass cls;
    private String clsName;
    private static String[] nativeSources;
    private static List<ByteCodeClass> classes = new ArrayList<>();

    /// The closed world, for analyses that must resolve a callee body. An analysis that
    /// cannot find a body must answer conservatively rather than assume anything.
    static List<ByteCodeClass> getClasses() {
        return classes;
    }

    // ---- CLOSED-WORLD DEVIRTUALIZATION -----------------------------------
    // ParparVM compiles a closed world: after dead-code elimination the class
    // list is final, so an INVOKEVIRTUAL whose method has NO reachable override
    // below the static receiver type can be emitted as a DIRECT call to the
    // implementing class's C function -- removing the vtable load + indirect
    // branch AND letting ThinLTO inline it (a vtable-dispatched getter like
    // String.length()/HashMap.size() otherwise stays an opaque call in every
    // hot loop). The subclass index is (re)built lazily whenever the class list
    // changed; emission runs after the list is final.
    private static java.util.Map<String, java.util.List<ByteCodeClass>> cn1SubclassIndex;
    private static int cn1SubclassIndexSize = -1;
    private static int readingClassDepth;

    public static boolean isReadingClass() { return readingClassDepth != 0; }

    private static void cn1EnsureSubclassIndex() {
        if (cn1SubclassIndexSize == classes.size()) {
            return;
        }
        cn1SubclassIndex = new java.util.HashMap<String, java.util.List<ByteCodeClass>>();
        for (ByteCodeClass c : classes) {
            if (c.getBaseClass() != null) {
                String b = c.getBaseClass().replace('/', '_').replace('$', '_');
                java.util.List<ByteCodeClass> l = cn1SubclassIndex.get(b);
                if (l == null) {
                    l = new java.util.ArrayList<ByteCodeClass>();
                    cn1SubclassIndex.put(b, l);
                }
                l.add(c);
            }
        }
        cn1SubclassIndexSize = classes.size();
    }

    /**
     * TRUE when nothing live extends {@code clsName}, so an object is an instance of it
     * exactly when its class word IS that class.
     *
     * That distinction is worth a load. The general test reads the class word, then
     * reads classId out of it -- a SECOND load, dependent on the first -- and indexes
     * the type-test bitmap with it. Against a leaf, comparing the class word to the
     * class itself answers the same question with one load and no bitmap. Read beside
     * C2's output for the same test, that dependent load is most of the difference:
     * HotSpot compares the klass word directly because for it the klass IS the
     * identity, while we had been comparing ids.
     *
     * Only meaningful after the cull, because "nothing live extends it" is a statement
     * about the classes that survived. Interfaces are excluded: an interface is never a
     * class word.
     */
    /**
     * TRUE when a value whose static type is {@code clsName} could be a TAGGED immediate.
     *
     * CN1_CLASS_OF is not a field load when tagged values are compiled in: it masks the
     * pointer, tests the tag and selects between a proxy table entry and the object
     * before loading anything -- five instructions, on every virtual dispatch and every
     * class query. That work only ever finds something for a boxed Integer, Long,
     * Double, Float, Character or Short.
     *
     * A virtual thunk for class X only ever receives objects whose class is X or below,
     * so if no taggable class is assignable to X the test cannot fire and the class word
     * can be read straight out of the object. Derived from the hierarchy rather than
     * hardcoded, so a future tagged type is covered by adding it to CN1_TAGGABLE_CLASSES
     * alone.
     */
    public static synchronized boolean canReceiveTagged(String clsName) {
        if (cn1TaggedReachable == null) {
            cn1TaggedReachable = new java.util.HashSet<String>();
            for (String t : CN1_TAGGABLE_CLASSES) {
                cn1TaggedReachable.add(t);
                ByteCodeClass c = getClassObject(t);
                // Every supertype and interface of a taggable class can be the static
                // type of a tagged value: Object, Number, Comparable, Serializable.
                while (c != null) {
                    cn1TaggedReachable.add(c.getClsName());
                    if (c.getBaseInterfaces() != null) {
                        for (String i : c.getBaseInterfaces()) {
                            cn1TaggedReachable.add(i.replace('/', '_').replace('$', '_'));
                        }
                    }
                    String base = c.getBaseClass();
                    c = base == null ? null
                            : getClassObject(base.replace('/', '_').replace('$', '_'));
                }
            }
            // Belt and braces: a class we could not resolve must not become "safe".
            cn1TaggedReachable.add("java_lang_Object");
        }
        return cn1TaggedReachable.contains(clsName);
    }

    private static java.util.Set<String> cn1TaggedReachable;

    /** Classes a TAGGED immediate reports as its own -- see CN1_TAG_* in cn1_globals.h. */
    private static final java.util.Set<String> CN1_TAGGABLE_CLASSES =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "java_lang_Integer", "java_lang_Long", "java_lang_Double",
                    "java_lang_Float", "java_lang_Character", "java_lang_Short"));

    public static synchronized boolean isLeafClass(String clsName) {
        ByteCodeClass c = getClassObject(clsName);
        if (c == null || c.isIsInterface() || c.isEliminated()) {
            return false;
        }
        // A BOXED type is final, so it is a leaf by the subclass test -- and the leaf
        // form is still wrong for it, because a tagged immediate IS an instance and has
        // no class word to compare. `Integer.valueOf(5) instanceof Integer` has to be
        // true. BoxEdge, HtTorture and InstanceOfT all caught this at once; the earlier
        // reasoning that "a tagged value's class is never a leaf anyone tests for" was
        // simply false, and final is exactly what these classes are.
        if (CN1_TAGGABLE_CLASSES.contains(clsName)) {
            return false;
        }
        cn1EnsureSubclassIndex();
        java.util.List<ByteCodeClass> subs = cn1SubclassIndex.get(clsName);
        if (subs == null) {
            return true;
        }
        for (ByteCodeClass sub : subs) {
            if (!sub.isEliminated()) {
                return false;
            }
        }
        return true;
    }

    /**
     * If (name, desc) invoked virtually on {@code owner} has no reachable
     * override in any non-eliminated subclass, returns the mangled name of the
     * class whose non-abstract declaration implements it (owner or an
     * ancestor); otherwise null (the call must stay a vtable dispatch).
     */
    /* Memo for the devirtualization queries.
     *
     * These are asked once per call site PER ROUND of the dead code pass, which is
     * four passes over every method, and each answer walks a class cone. Uncached
     * that is not a small cost on the thing being measured: the translator is the
     * benchmark, so a more thorough analysis shows up directly as a slower run. It
     * measured at 162B instructions against 136B before the memo -- the analysis was
     * costing more than the better code it produced was saving.
     *
     * Cleared at the start of every round, because the answers depend on what has
     * been eliminated so far and that is exactly what each round changes. */
    private static final Map<String, java.util.List<ByteCodeClass>> cn1DevirtMemo =
            new HashMap<String, java.util.List<ByteCodeClass>>();
    private static final Map<String, java.util.List<ByteCodeClass>> cn1ConeMemo =
            new HashMap<String, java.util.List<ByteCodeClass>>();

    static void cn1InvalidateDevirtMemo() {
        cn1DevirtMemo.clear();
        cn1ConeMemo.clear();
    }

    /* Interface -> the reachable classes implementing it. Built once, the same way
     * and for the same reason as the subclass index: the devirtualizer asks this per
     * call site, and a scan of every class per site is quadratic on a real corpus. */
    private static java.util.Map<String, java.util.List<ByteCodeClass>> cn1ImplementorIndex;
    private static int cn1ImplementorIndexSize = -1;

    private static void cn1EnsureImplementorIndex() {
        if (cn1ImplementorIndex != null && cn1ImplementorIndexSize == classes.size()) {
            return;
        }
        cn1ImplementorIndex = new HashMap<String, java.util.List<ByteCodeClass>>();
        java.util.List<ByteCodeClass> interfaces = new java.util.ArrayList<ByteCodeClass>();
        for (ByteCodeClass c : classes) {
            if (c.isIsInterface()) {
                interfaces.add(c);
            }
        }
        for (ByteCodeClass c : classes) {
            if (c.isIsInterface() || c.isEliminated()) {
                continue;
            }
            for (ByteCodeClass i : interfaces) {
                if (c.doesImplement(i)) {
                    java.util.List<ByteCodeClass> l = cn1ImplementorIndex.get(i.getClsName());
                    if (l == null) {
                        l = new java.util.ArrayList<ByteCodeClass>();
                        cn1ImplementorIndex.put(i.getClsName(), l);
                    }
                    l.add(c);
                }
            }
        }
        cn1ImplementorIndexSize = classes.size();
    }

    /**
     * Every reachable implementation of {@code (name, desc)} reachable through an
     * INTERFACE call on {@code iface}: the declaring class of the method in each
     * class that implements the interface, plus the interface's own default
     * implementation when a class inherits it rather than declaring one.
     *
     * Interface dispatch is the most expensive call this VM makes -- a class id, a
     * map row, an offset, a vtable slot and then an indirect branch -- so a site with
     * a single reachable implementation is worth far more to resolve than a virtual
     * one, and INVOKEINTERFACE was not being resolved at all.
     *
     * @param iface the interface named by the call site
     * @param name method name
     * @param desc method descriptor
     * @return distinct implementing classes, or null when the interface is unknown
     */
    /**
     * The concrete, non-eliminated classes a receiver of static type {@code owner}
     * can actually be at run time.
     *
     * This, not the number of implementations, is what decides whether a site can be
     * dispatched by comparing class ids: the guard tests the RECEIVER's id, and
     * several receiver classes can share one inherited implementation.
     *
     * @param owner static receiver type, class or interface
     * @return concrete assignable classes, or null when the type is unknown
     */
    public static synchronized java.util.List<ByteCodeClass> concreteReceiverCone(ByteCodeClass owner) {
        if (owner == null) {
            return null;
        }
        java.util.List<ByteCodeClass> coneMemo = cn1ConeMemo.get(owner.getClsName());
        if (coneMemo != null) {
            return coneMemo;
        }
        java.util.List<ByteCodeClass> cone = new java.util.ArrayList<ByteCodeClass>(4);
        if (owner.isIsInterface()) {
            cn1EnsureImplementorIndex();
            java.util.List<ByteCodeClass> impls = cn1ImplementorIndex.get(owner.getClsName());
            if (impls != null) {
                for (ByteCodeClass c : impls) {
                    if (!c.isEliminated() && !c.isIsAbstract()) {
                        cone.add(c);
                    }
                }
            }
            cn1ConeMemo.put(owner.getClsName(), cone);
            return cone;
        }
        cn1EnsureSubclassIndex();
        java.util.ArrayDeque<ByteCodeClass> stack = new java.util.ArrayDeque<ByteCodeClass>();
        stack.add(owner);
        while (!stack.isEmpty()) {
            ByteCodeClass c = stack.pop();
            if (!c.isEliminated() && !c.isIsAbstract()) {
                cone.add(c);
            }
            java.util.List<ByteCodeClass> kids = cn1SubclassIndex.get(c.getClsName());
            if (kids != null) {
                stack.addAll(kids);
            }
        }
        cn1ConeMemo.put(owner.getClsName(), cone);
        return cone;
    }

    public static synchronized java.util.List<ByteCodeClass> resolveInterfaceTargets(
            ByteCodeClass iface, String name, String desc) {
        if (iface == null || !iface.isIsInterface()) {
            return null;
        }
        String memoKey = "I" + iface.getClsName() + '#' + name + desc;
        java.util.List<ByteCodeClass> memo = cn1DevirtMemo.get(memoKey);
        if (memo != null) {
            return memo;
        }
        cn1EnsureImplementorIndex();
        java.util.List<ByteCodeClass> impls = cn1ImplementorIndex.get(iface.getClsName());
        java.util.List<ByteCodeClass> targets = new java.util.ArrayList<ByteCodeClass>(4);
        if (impls != null) {
            for (ByteCodeClass c : impls) {
                ByteCodeClass d = c;
                while (d != null) {
                    if (d.hasDeclaredNonAbstractMethod(name, desc)) {
                        if (!targets.contains(d)) {
                            targets.add(d);
                        }
                        break;
                    }
                    String b = d.getBaseClass();
                    d = b == null ? null : getClassObject(b.replace('/', '_').replace('$', '_'));
                }
                if (d == null) {
                    // Inherited from the interface as a default method, or not found at
                    // all. Either way this receiver does not resolve to a class, so the
                    // site cannot be reduced to one target.
                    return null;
                }
            }
        }
        cn1DevirtMemo.put(memoKey, targets);
        return targets;
    }

    /**
     * Every reachable implementation of {@code (name, desc)} that a virtual call on
     * {@code owner} could land in -- the class hierarchy cone below the static type,
     * restricted to classes the dead code pass kept.
     *
     * resolveDevirtualizedOwner answers the same question but collapses it to
     * "exactly one, or give up". That throws away the case a closed world makes
     * cheap: a site with two or three possible targets does not need a vtable at
     * all, it needs a compare and a direct call, which the C compiler can then
     * inline through. Only the count of targets decides which, so the set is what
     * this returns.
     *
     * @param owner static receiver type
     * @param name method name
     * @param desc method descriptor
     * @return the implementing classes, or null when the receiver type is unknown
     */
    public static synchronized java.util.List<ByteCodeClass> resolveVirtualTargets(
            ByteCodeClass owner, String name, String desc) {
        if (owner == null) {
            return null;
        }
        String memoKey = owner.getClsName() + '#' + name + desc;
        java.util.List<ByteCodeClass> memo = cn1DevirtMemo.get(memoKey);
        if (memo != null) {
            return memo;
        }
        cn1EnsureSubclassIndex();
        java.util.List<ByteCodeClass> targets = new java.util.ArrayList<ByteCodeClass>(4);
        // The implementation inherited at or above the static type, which is what a
        // receiver of exactly that type runs.
        ByteCodeClass c = owner;
        while (c != null) {
            if (c.hasDeclaredNonAbstractMethod(name, desc)) {
                targets.add(c);
                break;
            }
            String b = c.getBaseClass();
            c = b == null ? null : getClassObject(b.replace('/', '_').replace('$', '_'));
        }
        // Plus every override below it.
        java.util.ArrayDeque<ByteCodeClass> stack = new java.util.ArrayDeque<ByteCodeClass>();
        java.util.List<ByteCodeClass> kids = cn1SubclassIndex.get(owner.getClsName());
        if (kids != null) {
            stack.addAll(kids);
        }
        while (!stack.isEmpty()) {
            ByteCodeClass k = stack.pop();
            if (!k.isEliminated() && k.hasDeclaredNonAbstractMethod(name, desc) && !targets.contains(k)) {
                targets.add(k);
            }
            kids = cn1SubclassIndex.get(k.getClsName());
            if (kids != null) {
                stack.addAll(kids);
            }
        }
        cn1DevirtMemo.put(memoKey, targets);
        return targets;
    }

    public static synchronized String resolveDevirtualizedOwner(ByteCodeClass owner, String name, String desc) {
        if (owner == null) {
            return null;
        }
        cn1EnsureSubclassIndex();
        // any subclass DECLARING the method (abstract or not) keeps it virtual
        java.util.ArrayDeque<ByteCodeClass> stack = new java.util.ArrayDeque<ByteCodeClass>();
        java.util.List<ByteCodeClass> kids = cn1SubclassIndex.get(owner.getClsName());
        if (kids != null) {
            stack.addAll(kids);
        }
        while (!stack.isEmpty()) {
            ByteCodeClass c = stack.pop();
            if (!c.isEliminated() && c.hasDeclaredMethod(name, desc)) {
                return null;
            }
            kids = cn1SubclassIndex.get(c.getClsName());
            if (kids != null) {
                stack.addAll(kids);
            }
        }
        // resolve the implementing declaration at or above the static type
        ByteCodeClass c = owner;
        while (c != null) {
            if (c.hasDeclaredNonAbstractMethod(name, desc)) {
                return c.getClsName();
            }
            String b = c.getBaseClass();
            if (b == null) {
                return null;
            }
            c = getClassObject(b.replace('/', '_').replace('$', '_'));
        }
        return null;
    }
    /**
     * The concrete Iterator class a for-each over this collection type will
     * really get, or null when that cannot be established with certainty.
     *
     * Two things have to hold. The collection's iterator() must have exactly one
     * reachable implementation -- resolveDevirtualizedOwner answers that -- and
     * that implementation must do nothing but allocate and return, so the class
     * it allocates is the class the caller receives. Anything else answers null
     * and the call site is left as the interface call it was.
     */
    public static synchronized String resolveConcreteIteratorType(ByteCodeClass owner) {
        String decl = resolveDevirtualizedOwner(owner, "iterator", "()Ljava/util/Iterator;");
        if (decl == null) {
            return null;
        }
        ByteCodeClass dc = getClassObject(Util.mangle(decl));
        if (dc == null) {
            return null;
        }
        for (BytecodeMethod m : dc.getMethods()) {
            if ("iterator".equals(m.getMethodName()) && "()Ljava/util/Iterator;".equals(m.getDesc())) {
                return m.allocatedReturnType();
            }
        }
        return null;
    }

    private static final MethodDependencyGraph dependencyGraph = new MethodDependencyGraph();
    private int lambdaCounter;
    private int stringConcatCounter;
    public static void cleanup() {
        if ("true".equals(System.getProperty("cn1.iteratorCensus"))) {
            // Reported HERE rather than from the census itself: the census runs BEFORE
            // the pass that scopes the loops, so printing it there reports zero for both.
            System.out.println("[ITER] for-each intrinsified=" + BytecodeMethod.forEachIntrinsified
                    + " scoped=" + BytecodeMethod.stackIterScoped
                    + " refused=" + BytecodeMethod.stackIterRefused);
        }
        if ("true".equals(System.getProperty("cn1.sbCensus"))) {
            int sites = BytecodeMethod.sbCensusSites;
            System.out.println("[SB] StringBuilder NEW sites=" + sites
                    + " stackAllocated=" + BytecodeMethod.sbCensusStackAllocated
                    + " inTryCatchMethods=" + BytecodeMethod.sbCensusBailTryCatch
                    + " refusedBySynchronized=" + BytecodeMethod.sbCensusBailSync
                    + (sites > 0 ? "  (try/catch no longer refuses any of the "
                        + (100 * BytecodeMethod.sbCensusBailTryCatch / sites) + "% of sites it reaches)" : ""));
        }
        if (BytecodeMethod.BCE_CENSUS) {
            int ops = BytecodeMethod.bceArrayOpsTotal;
            System.out.println("[BCE] methods=" + BytecodeMethod.bceMethods
                + " withArrayOps=" + BytecodeMethod.bceMethodsWithArrays
                + " refusedByTryCatch=" + BytecodeMethod.bceRefusedTryCatch
                + " arrayOps=" + ops
                + " arrayOpsInRefusedMethods=" + BytecodeMethod.bceArrayOpsRefusedTryCatch
                + (ops > 0 ? "  (" + (100 * BytecodeMethod.bceArrayOpsRefusedTryCatch / ops)
                    + "% of array accesses sit in a method that used to be refused whole)" : ""));
            System.out.println("[BCE] cleared=" + BytecodeMethod.bceAccessesMarked
                + " ofThoseInTryCatchMethods=" + BytecodeMethod.bceAccessesMarkedInTryCatchMethod
                + " loopsRefusedByHandlerInside=" + BytecodeMethod.bceLoopsRefusedByHandler);
            StringBuilder why = new StringBuilder("[BCE] candidateLoops:");
            for (int i = 0; i < BytecodeMethod.BCE_WHY.length; i++) {
                why.append(' ').append(BytecodeMethod.BCE_WHY[i]).append('=').append(BytecodeMethod.bceWhy[i]);
            }
            System.out.println(why);
            StringBuilder miss = new StringBuilder("[BCE] inAcceptedLoops:");
            for (int i = 0; i < BytecodeMethod.BCE_MISS.length; i++) {
                miss.append(' ').append(BytecodeMethod.BCE_MISS[i]).append('=').append(BytecodeMethod.bceMiss[i]);
            }
            System.out.println(miss);
            StringBuilder ho = new StringBuilder("[BCE] hoistedLength:");
            for (int i = 0; i < BytecodeMethod.BCE_HOIST.length; i++) {
                ho.append(' ').append(BytecodeMethod.BCE_HOIST[i]).append('=').append(BytecodeMethod.bceHoist[i]);
            }
            System.out.println(ho);
        }
        if (BytecodeMethod.FRAMELESS_CENSUS) {
            System.out.println("[EAGER-INIT] clinits=" + ByteCodeClass.clinitCount
                + " pure=" + ByteCodeClass.pureClinitCount
                + " first blocker (reason -> classes): " + ByteCodeClass.PURE_CLINIT_BLOCKERS);
            int t = BytecodeMethod.censusTotal;
            System.out.println("[FRAMELESS] methods=" + t
                + " frameless=" + BytecodeMethod.censusEligible
                + " excludedByTryCatchALONE=" + BytecodeMethod.censusExcludedTryCatch
                + " excludedOther=" + BytecodeMethod.censusExcludedOther
                + (t > 0 ? "  (tryCatchAlone=" + (100 * BytecodeMethod.censusExcludedTryCatch / t)
                    + "% of all methods)" : ""));
            System.out.println("[FRAMELESS] excludedOther breakdown:"
                + " ctorOrClinit=" + BytecodeMethod.censusNoConstructor
                + " synchronized=" + BytecodeMethod.censusNoSync
                + " onDeviceDebug=" + BytecodeMethod.censusNoDebug
                + " unhandledOpcode=" + BytecodeMethod.censusNoOpcode
                + " empty=" + BytecodeMethod.censusEmpty);
            System.out.println("[FRAMELESS] first blocking instruction (class:opcode -> methods): "
                + BytecodeMethod.censusBlockers);
        }
        LocalReceiverTypes.clear();
        cn1SubclassIndex = null;
        cn1SubclassIndexSize = -1;
        nativeSources = null;
        classes.clear();
        // classes is cleared in place (same List reference), so the name index's
        // (reference, size) guard cannot detect a subsequent same-size refill across
        // translation runs in the same JVM (e.g. the unit tests). Invalidate it here.
        classIndexMap = null;
        classIndexSource = null;
        classIndexSize = -1;
        dependencyGraph.clear();
        BytecodeMethod.setDependencyGraph(null);
        ByteCodeClass.cleanup();
        LabelInstruction.cleanup();
    }
    public static void parse(File sourceFile) throws Exception {
        if(ByteCodeTranslator.verbose) {
            System.out.println("Parsing: " + sourceFile.getAbsolutePath());
        }
        BytecodeMethod.setDependencyGraph(dependencyGraph);
        ClassReader r;
        try (InputStream in = new FileInputStream(sourceFile)) {
            r = new ClassReader(in);
        }
        Parser p = new Parser();
        
        p.clsName = r.getClassName().replace('/', '_').replace('$', '_');
        p.cls = new ByteCodeClass(p.clsName, r.getClassName());
        readingClassDepth++;
        try {
            r.accept(p, ClassReader.EXPAND_FRAMES);
        } finally {
            readingClassDepth--;
        }
        
        classes.add(p.cls);
    }
    
    private static ByteCodeClass getClassByName(String name) {
        return classIndex().get(name.replace('/', '_').replace('$', '_'));
    }

    /**
     * Resolves a class by name and returns its post-{@link #writeOutput}
     * classOffset, or -1 if no class with that name exists. Used by the
     * on-device-debug side-table emitter to wire stable IDs into the
     * generated frame_info structs.
     */
    public static int getClassOffset(String name) {
        ByteCodeClass bc = getClassByName(name);
        return bc == null ? -1 : bc.getClassOffset();
    }

    /**
     * On-device-debug field-id allocator. Maps a (declaring-class, field-name)
     * pair to a stable int the device and proxy both use to address an
     * instance field. Field ids start at 1 — 0 is reserved as a "no-field"
     * sentinel so JDWP code can use 0 to mean "skip".
     *
     * IDs are persistent only within a single translator invocation; the
     * sidecar carries them so the proxy doesn't need to recompute the same
     * mapping.
     */
    private static final java.util.LinkedHashMap<String, Integer> fieldIdByKey = new java.util.LinkedHashMap<>();
    private static int nextFieldId = 1;

    public static int getOrAssignFieldId(String declClassMangled, String fieldName) {
        String key = declClassMangled + "." + fieldName;
        Integer id = fieldIdByKey.get(key);
        if (id != null) return id;
        id = nextFieldId++;
        fieldIdByKey.put(key, id);
        return id;
    }

    /**
     * Returns the JVM-style descriptor for a ByteCodeField — "I" for int,
     * "Lcom/example/Foo;" for object, "[I" for int[], etc. The translator
     * normally exposes underscore-mangled type names; this helper converts
     * to JVM form for JDWP wire compatibility.
     */
    public static String jvmDescriptorOf(ByteCodeField bf) {
        StringBuilder sb = new StringBuilder();
        String rd = bf.getRuntimeDescriptor();
        // getRuntimeDescriptor returns either a JVM single-char (I/J/...)
        // or an underscore-mangled object type, possibly followed by "[]"
        // repeats for array dimensions.
        int arrayDims = 0;
        while (rd != null && rd.endsWith("[]")) {
            arrayDims++;
            rd = rd.substring(0, rd.length() - 2);
        }
        for (int i = 0; i < arrayDims; i++) sb.append('[');
        if (rd != null && rd.length() == 1 && "ZBSCIJFD".indexOf(rd.charAt(0)) >= 0) {
            sb.append(rd);
        } else if (rd != null && !rd.isEmpty()) {
            sb.append('L').append(rd.replace('_', '/')).append(';');
        } else {
            sb.append("Ljava/lang/Object;");
        }
        return sb.toString();
    }

    /**
     * Returns a JDWP-style modifier bitmask (PUBLIC=1, PRIVATE=2, STATIC=8,
     * FINAL=16, VOLATILE=64, TRANSIENT=128). We don't track protected/transient
     * — fields are reported as public unless flagged private.
     */
    public static int jdwpAccessFlagsOf(ByteCodeField bf) {
        int f = 0;
        if (bf.isPrivate()) f |= 0x0002; else f |= 0x0001;
        if (bf.isStaticField()) f |= 0x0008;
        if (bf.isFinal()) f |= 0x0010;
        if (bf.isVolatile()) f |= 0x0040;
        return f;
    }

    /**
     * Emits the on-device-debug symbol table, gzip-compressed, as a generated
     * C source ({@code cn1_debug_symbols.c}) that links straight into the
     * iOS binary. The device serves it to the desktop debug proxy on demand
     * (CMD_GET_SYMBOLS) so the proxy never needs a local file — which is what
     * makes on-device debugging work for cloud builds (Windows/Linux), where
     * the translator ran on the build server and no sidecar ever reaches the
     * developer's machine.
     *
     * The uncompressed payload is the same line-based ASCII the proxy's
     * SymbolTable parses:
     *   version &lt;n&gt;
     *   class   &lt;classId&gt; &lt;clsName&gt; &lt;sourceFile&gt; &lt;superId&gt; &lt;jvmName&gt;
     *   method  &lt;methodId&gt; &lt;classId&gt; &lt;methodName&gt; &lt;desc&gt; &lt;isStatic&gt;
     *   line    &lt;methodId&gt; &lt;sourceLine&gt;
     *   var     &lt;methodId&gt; &lt;slot&gt; &lt;name&gt; &lt;desc&gt;
     *   field   &lt;classId&gt; &lt;fieldId&gt; &lt;name&gt; &lt;desc&gt; &lt;access&gt;
     */
    private static void writeSymbolSidecar(File outputDirectory) throws IOException {
        java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream(1 << 20);
        try (Writer w = new OutputStreamWriter(raw, "UTF-8")) {
            w.write("version\t1\n");
            for (ByteCodeClass bc : classes) {
                String src = bc.getSourceFile();
                if (src == null) {
                    src = "";
                }
                w.write(classSymbolRow(bc, src));
            }
            // Emit instance-field metadata so the proxy can answer JDWP
            // ClassType.Fields / FieldsWithGeneric without a device round-trip,
            // and so ObjectReference.GetValues knows what (type, declaring class)
            // each fieldId resolves to. We list inherited fields under each
            // class that physically stores them — JDWP expects a class's
            // Fields response to include only its own declarations, but
            // listing inherited fields too keeps single-table lookup cheap on
            // the proxy side; the proxy filters to "declared here" itself.
            for (ByteCodeClass bc : classes) {
                int classId = bc.getClassOffset();
                for (ByteCodeField bf : bc.getFields()) {
                    if (bf.isStaticField()) continue;
                    int fid = getOrAssignFieldId(bc.getClsName(), bf.getFieldName());
                    String desc = jvmDescriptorOf(bf);
                    int access = jdwpAccessFlagsOf(bf);
                    w.write("field\t" + classId + "\t" + fid
                            + "\t" + bf.getFieldName()
                            + "\t" + desc
                            + "\t" + access + "\n");
                }
            }
            for (ByteCodeClass bc : classes) {
                int classId = bc.getClassOffset();
                for (BytecodeMethod m : bc.getMethods()) {
                    if (m.isEliminated()) {
                        continue;
                    }
                    String desc = m.getDesc();
                    if (desc == null) {
                        desc = "";
                    }
                    // Extended method row: classId, name, desc, isStatic.
                    // Older proxies that only know 4 columns ignore the 5th
                    // because the parser slices with `split("\t", -1)` and
                    // size-checks before reading.
                    w.write("method\t" + m.getMethodOffset() + "\t" + classId
                            + "\t" + m.getMethodName()
                            + "\t" + desc
                            + "\t" + (m.isStatic() ? "1" : "0") + "\n");
                    Set<Integer> lines = new TreeSet<>();
                    for (com.codename1.tools.translator.bytecodes.Instruction ins : m.getInstructions()) {
                        if (ins instanceof com.codename1.tools.translator.bytecodes.LineNumber) {
                            lines.add(((com.codename1.tools.translator.bytecodes.LineNumber)ins).getLine());
                        }
                    }
                    for (Integer line : lines) {
                        w.write("line\t" + m.getMethodOffset() + "\t" + line + "\n");
                    }
                    // Local variables, each with the source-line range it is in
                    // scope for so the IDE only asks for slots that are live.
                    // 0/0 means "always live" — a local with no scope in the
                    // class file, or one the translator synthesised from a
                    // store opcode. Taken in the same deterministic order the
                    // device's own side-table uses.
                    java.util.List<com.codename1.tools.translator.bytecodes.LocalVariable> vars =
                            m.debugVarEntries();
                    java.util.List<int[]> scopes = m.debugVarScopes(vars);
                    for (int vi = 0; vi < vars.size(); vi++) {
                        com.codename1.tools.translator.bytecodes.LocalVariable lv = vars.get(vi);
                        int[] scope = scopes.get(vi);
                        w.write("var\t" + m.getMethodOffset()
                                + "\t" + lv.getIndex()
                                + "\t" + lv.getOrigName()
                                + "\t" + lv.getDesc()
                                + "\t" + scope[0]
                                + "\t" + scope[1] + "\n");
                    }
                }
            }
        }

        // gzip the payload — symbol tables are large and highly repetitive,
        // so this keeps the debug binary's footprint modest.
        byte[] gz = DebugSymbolCompressor.gzip(raw);

        // Compiled into the project like any other generated unit when
        // cn1.onDeviceDebug is on, so it needs provenance for the same reason they do.
        ByteCodeTranslator.sourceManifest.recordGenerated("cn1_debug_symbols.c");
        File f = new File(outputDirectory, "cn1_debug_symbols.c");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
            w.write("/* Auto-generated by the Codename One iOS translator. Do not edit.\n");
            w.write(" * On-device-debug symbol table (gzip-compressed), streamed to the\n");
            w.write(" * desktop debug proxy over CMD_GET_SYMBOLS. */\n");
            // cn1_globals.h carries the CN1_ON_DEVICE_DEBUG #define (flipped on
            // by the builder for debug builds); include it so the guard below
            // actually sees the macro rather than silently compiling to nothing.
            w.write("#include \"cn1_globals.h\"\n");
            w.write("#ifdef CN1_ON_DEVICE_DEBUG\n");
            w.write("static const unsigned char cn1_debug_symbols_gz[] = {\n");
            for (int i = 0; i < gz.length; i++) {
                if ((i & 15) == 0) {
                    w.write("    ");
                }
                w.write("0x");
                int b = gz[i] & 0xff;
                w.write(Util.hexDigit(b >> 4));
                w.write(Util.hexDigit(b & 0xf));
                w.write(',');
                w.write((i & 15) == 15 ? '\n' : ' ');
            }
            w.write("\n};\n");
            w.write("static const int cn1_debug_symbols_gz_len = " + gz.length + ";\n");
            w.write("const unsigned char* cn1_debug_symbols_data(void) { return cn1_debug_symbols_gz; }\n");
            w.write("int cn1_debug_symbols_length(void) { return cn1_debug_symbols_gz_len; }\n");
            w.write("#endif\n");
        }
        if (ByteCodeTranslator.verbose) {
            System.out.println("Wrote on-device-debug symbol blob: " + f.getAbsolutePath()
                    + " (" + gz.length + " gz bytes, " + raw.size() + " raw)");
        }
    }

    /**
     * Builds a symbol-table class row while retaining both ParparVM's mangled
     * identifier and the original JVM internal name. The latter is required
     * for JDWP signatures because mangling maps both {@code '/'} and
     * {@code '$'} to {@code '_'}, making anonymous and inner classes
     * impossible to reconstruct reliably in the debugger proxy.
     */
    static String classSymbolRow(ByteCodeClass bc, String sourceFile) {
        int superId = -1;
        if (bc.getBaseClassObject() != null) {
            superId = bc.getBaseClassObject().getClassOffset();
        }
        String jvmName = bc.getOriginalClassName();
        if (jvmName == null || jvmName.isEmpty()) {
            // Defensive compatibility for synthetic ByteCodeClass instances
            // that predate original-name tracking.
            jvmName = bc.getClsName().replace('_', '/');
        }
        return "class\t" + bc.getClassOffset() + "\t" + bc.getClsName()
                + "\t" + sourceFile + "\t" + superId + "\t" + jvmName + "\n";
    }
    
    private static void appendClassOffset(ByteCodeClass bc, List<Integer> clsIds) {
        if(bc.getBaseClassObject() != null) {
            if(!clsIds.contains(bc.getBaseClassObject().getClassOffset())) {
                clsIds.add(bc.getBaseClassObject().getClassOffset());
                appendClassOffset(bc.getBaseClassObject(), clsIds);
            }
        }
        if(bc.getBaseInterfacesObject() != null) {
            for(ByteCodeClass c : bc.getBaseInterfacesObject()) {
                if(c != null && !clsIds.contains(c.getClassOffset())) {
                    clsIds.add(c.getClassOffset());
                    if(c.getBaseClassObject() != null) {
                        appendClassOffset(c, clsIds);
                    }
                }
            }
        }
    }

    // Inverted index over the native sources for O(1) "is this symbol referenced
    // by native code" queries (see NativeSymbolIndex). Built lazily and cached
    // against the nativeSources array identity, mirroring the per-method memo in
    // BytecodeMethod.isMethodUsedByNative.
    private static NativeSymbolIndex nativeSymbolIndex;
    private static String[] nativeSymbolIndexSources;
    public static NativeSymbolIndex getNativeSymbolIndex(String[] nativeSources) {
        if (nativeSymbolIndex == null || nativeSymbolIndexSources != nativeSources) {
            nativeSymbolIndex = new NativeSymbolIndex(nativeSources);
            nativeSymbolIndexSources = nativeSources;
        }
        return nativeSymbolIndex;
    }

    private static final ArrayList<String> constantPool = new ArrayList<>();
    // Index of constantPool, so addToConstantPool does not have to scan it.
    //
    // The list stays the source of truth -- writeOutput emits it in order and the
    // emitted indices are positions in it -- and this only answers "where is s", the
    // question ArrayList.indexOf was answering with a String.equals against every
    // entry already interned. On a self-hosting translation the pool holds ~200k
    // strings and that scan was the single largest cost on the mutator thread:
    // String.equals 11.2%, the iterator 10.3%, indexOf 6.2% and ArrayList.get 5.1%
    // of samples, all of it here.
    private static final Map<String, Integer> constantPoolIndex = new HashMap<String, Integer>();

    /* Constant-time instanceof.
     *
     * instanceofFunction resolves a test by walking classInstanceOf[runtimeClassId],
     * the class's supertype list, until it finds the tested type or the -1 sentinel.
     * That is O(number of supertypes) on a path a real translation takes hundreds of
     * millions of times, and it is a pointer chase, so every step is a dependent load
     * the CPU cannot overlap.
     *
     * A closed world does not have to do that. The type on the right of an instanceof
     * is a compile-time constant, and across a whole application only a couple of
     * hundred distinct types are ever tested against -- 252 over a 5,326 class corpus.
     * So each tested type gets a dense bit index here, and each class gets a bitmap of
     * the tested types it is an instance of. The test is then a load, a shift and an
     * and, with no call and no loop, which is what a JIT emits for the same check.
     *
     * Array types keep the old path: classInstanceOf is indexed by ordinary class ids
     * only, array ids live above cn1_array_start_offset, and instanceofFunction already
     * handles them separately before it reaches the scan.
     *
     * Iteration order over classes and their instructions is deterministic, so the
     * indices are stable -- the self-hosting gate compares emitted C byte for byte. */
    private static final LinkedHashMap<String, Integer> typeTestIds = new LinkedHashMap<String, Integer>();

    /**
     * Dense bitmap index of a type tested by instanceof, or -1 when this type is not
     * tested anywhere and therefore has no bit.
     *
     * @param actualType mangled class name as TypeInstruction spells it
     * @return the bit index, or -1
     */
    /* Long.toHexString does not exist in vm/JavaAPI, and the translator has to
     * compile against it to self-host, so the 64-bit rows are formatted here. */
    private static String unsignedHex(long v) {
        if(v == 0) {
            return "0";
        }
        char[] digits = new char[16];
        int at = 16;
        while(v != 0) {
            int nib = (int)(v & 0xf);
            digits[--at] = (char)(nib < 10 ? ('0' + nib) : ('a' + nib - 10));
            v >>>= 4;
        }
        return new String(digits, at, 16 - at);
    }

    public static int typeTestIndex(String actualType) {
        Integer i = typeTestIds.get(actualType);
        return i == null ? -1 : i.intValue();
    }

    /**
     * Assigns a bit index to every class an instanceof tests against. Runs before any
     * code is emitted, because the emitter bakes the index into the call site.
     */
    private static void collectTypeTests() {
        typeTestIds.clear();
        for(ByteCodeClass bc : classes) {
            for(BytecodeMethod bm : bc.getMethods()) {
                if(bm.isEliminated()) {
                    continue;
                }
                for(Instruction i : bm.getInstructions()) {
                    if(!(i instanceof TypeInstruction)) {
                        continue;
                    }
                    String t = ((TypeInstruction)i).instanceofTargetClass();
                    if(t != null && !typeTestIds.containsKey(t)) {
                        typeTestIds.put(t, Integer.valueOf(typeTestIds.size()));
                    }
                }
            }
        }
    }
    
    // Name -> class index, replacing the O(N) linear scans that getClassObject /
    // getClassByName / ByteCodeClass.findClass used to do. Those run per dependency
    // per class during the dead-code cull, so the scans were O(N^2) per pass.
    // Rebuilt lazily when `classes` changes. `classes` is only ever reassigned (new
    // reference) or grown in place via add()/cleared -- it is never mutated to a
    // same-reference, same-size, different-content state -- so the (reference, size)
    // pair uniquely identifies its state and makes this self-correcting.
    private static HashMap<String, ByteCodeClass> classIndexMap;
    private static List<ByteCodeClass> classIndexSource;
    private static int classIndexSize;
    private static HashMap<String, ByteCodeClass> classIndex() {
        if (classIndexMap == null || classIndexSource != classes || classIndexSize != classes.size()) {
            HashMap<String, ByteCodeClass> m = new HashMap<String, ByteCodeClass>(classes.size() * 2);
            for (ByteCodeClass cls : classes) {
                // first-wins, matching the old "return the first match" linear scan
                if (!m.containsKey(cls.getClsName())) {
                    m.put(cls.getClsName(), cls);
                }
            }
            classIndexMap = m;
            classIndexSource = classes;
            classIndexSize = classes.size();
        }
        return classIndexMap;
    }

    public static ByteCodeClass getClassObject(String name) {
        return classIndex().get(name);
    }
    
    /**
     * Adds the given string to the hardcoded constant pool strings returns the offset in the pool
     */
    public static int addToConstantPool(String s) {
        Integer existing = constantPoolIndex.get(s);
        if(existing != null) {
            return existing.intValue();
        }
        int index = constantPool.size();
        constantPool.add(s);
        constantPoolIndex.put(s, Integer.valueOf(index));
        return index;
    }
    
    
    
    private static void generateClassAndMethodIndexHeader(File outputDirectory) throws Exception {
        int classOffset = 0;
        int methodOffset = 0;
        ArrayList<BytecodeMethod> methods = new ArrayList<>();
        for(ByteCodeClass bc : classes) {
            bc.setClassOffset(classOffset);
            classOffset++;
            
            methodOffset = bc.updateMethodOffsets(methodOffset);
            methods.addAll(bc.getMethods());
        }
        
        StringBuilder bld = new StringBuilder();
        StringBuilder bldM = new StringBuilder();
        bldM.append("#include \"cn1_class_method_index.h\"\n");
        bldM.append("#include \"cn1_globals.h\"\n\n");
        bld.append("#ifndef __CN1_CLASS_METHOD_INDEX_H__\n#define __CN1_CLASS_METHOD_INDEX_H__\n\n");
        
        
        bld.append("// maps to offsets in the constant pool below\nextern int classNameLookup[];\n");
        bldM.append("// maps to offsets in the constant pool below\nint classNameLookup[] = {");
        boolean first = true;
        for(ByteCodeClass bc : classes) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append(addToConstantPool(bc.getClsName().replace('_', '.')));
        }
        bldM.append("};\n\n");
        
        for(ByteCodeClass bc : classes) {
            bld.append("#define cn1_class_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(bc.getClassOffset());
            bld.append("\n");
        }
        
        int arrayId = classes.size() + 1;
        
        bld.append("#define cn1_array_start_offset ");
        bld.append(arrayId);
        bld.append("\n");
        
        // leave space for primitive arrays
        arrayId += 100;
        
        for(ByteCodeClass bc : classes) {
            bld.append("#define cn1_array_1_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(arrayId);
            bld.append("\n");
            arrayId++;

            bld.append("#define cn1_array_2_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(arrayId);
            bld.append("\n");
            arrayId++;

            bld.append("#define cn1_array_3_id_");
            bld.append(bc.getClsName());
            bld.append(" ");
            bld.append(arrayId);
            bld.append("\n");
            arrayId++;
        }

        // THE HIGHEST CLASS ID THIS TRANSLATION CAN PRODUCE, emitted rather than guessed.
        // Anything sizing a per-class-id table needs this bound, and the only previous way
        // to spell it was to name a class and hope it was last: the heap histogram sized
        // its tables with cn1_array_3_id_java_util_Vector, which stops existing the moment
        // an application does not reach java.util.Vector, so that diagnostic failed to
        // compile on any app that culls it. arrayId is one past the last id handed out,
        // so the last valid id is arrayId - 1. Nothing consumes this yet -- the histogram
        // that needed it was deleted as superseded -- and it is emitted anyway because the
        // NEXT per-class-id table wants a bound that is a fact rather than a guess.
        bld.append("#define cn1_max_class_id ");
        bld.append(arrayId - 1);
        bld.append("\n");
        // Object-header class indices of the two java.lang.String twins, after every
        // array id's index (see cn1ClazzById and cn1InitStringTwin).
        bld.append("#define cn1_header_index_java_lang_String_i8 ").append(arrayId + 1).append("\n");
        bld.append("#define cn1_header_index_java_lang_String_i16 ").append(arrayId + 2).append("\n");

        bld.append("\n\n");

        bld.append("// maps to offsets in the constant pool below\nextern int methodNameLookup[];\n");
        bldM.append("// maps to offsets in the constant pool below\nint methodNameLookup[] = {");
        first = true;
        for(BytecodeMethod m : methods) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append(addToConstantPool(m.getMethodName()));
        }
        bldM.append("};\n\n");
        
        // Dense bit index of every type an instanceof tests against, keyed by the id
        // rather than the name so the per-class rows below can be filled straight from
        // the supertype list appendClassOffset already produces.
        HashMap<Integer, Integer> denseByOffset = new HashMap<Integer, Integer>();
        for(Map.Entry<String, Integer> e : typeTestIds.entrySet()) {
            ByteCodeClass tc = getClassByName(e.getKey());
            if(tc != null) {
                denseByOffset.put(Integer.valueOf(tc.getClassOffset()), e.getValue());
            }
        }
        int typeTestWords = (typeTestIds.size() + 63) / 64;
        if(typeTestWords == 0) {
            // An application with no instanceof at all still has to emit a valid array.
            typeTestWords = 1;
        }
        long[][] typeTestRows = new long[classes.size()][typeTestWords];

        ArrayList<Integer> instances = new ArrayList<>();
        int counter = 0;
        for(ByteCodeClass bc : classes) {
            bldM.append("int classInstanceOfArr");
            bldM.append(counter);
            bldM.append("[] = {");
            appendClassOffset(bc, instances);
            
            for(Integer i : instances) {
                bldM.append(i);
                bldM.append(", ");
            }

            // Same row the scan would have walked, precomputed. The class is an
            // instance of itself, which the scan never sees because instanceofFunction
            // answers the identity case before it reaches the list.
            long[] row = typeTestRows[counter];
            Integer selfDense = denseByOffset.get(Integer.valueOf(bc.getClassOffset()));
            if(selfDense != null) {
                row[selfDense.intValue() >> 6] |= 1L << (selfDense.intValue() & 63);
            }
            for(Integer i : instances) {
                Integer d = denseByOffset.get(i);
                if(d != null) {
                    row[d.intValue() >> 6] |= 1L << (d.intValue() & 63);
                }
            }

            counter++;
            instances.clear();
            bldM.append("-1};\n");
        }

        bld.append("#define CN1_TYPETEST_WORDS ");
        bld.append(typeTestWords);
        bld.append("\n#define CN1_TYPETEST_ROWS ");
        bld.append(classes.size());
        bld.append("\nextern const unsigned long long cn1TypeTestBits[];\n");
        bldM.append("\n// Bit d of row c is set when class c is an instance of the type\n");
        bldM.append("// holding dense index d. See Parser.typeTestIds.\n");
        bldM.append("const unsigned long long cn1TypeTestBits[] = {");
        for(int r = 0 ; r < typeTestRows.length ; r++) {
            bldM.append("\n    ");
            for(int w = 0 ; w < typeTestWords ; w++) {
                bldM.append("0x");
                bldM.append(unsignedHex(typeTestRows[r][w]));
                bldM.append("ULL, ");
            }
        }
        bldM.append("\n};\n\n");
        bld.append("extern int *classInstanceOf[];\n");
        bldM.append("int *classInstanceOf[");
        bldM.append(classes.size());
        bldM.append("] = {");
        first = true;
        int classCount = classes.size();
        for(counter = 0 ; counter < classCount ; counter++) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append("classInstanceOfArr");
            bldM.append(counter);
        }
        bldM.append("};\n\n");
        
        bld.append("#define CN1_CONSTANT_POOL_SIZE ");
        bld.append(constantPool.size());
        bld.append("\n\nextern const char * const constantPool[];\n");

        bldM.append("\n\nconst char * const constantPool[] = {\n");
        first = true;
        int offset = 0;
        for(String con : constantPool) {
            if(first) {
                bldM.append("\n    \"");
            } else {
                bldM.append(",\n    \"");
            }
            first = false;            
            try {
                bldM.append(encodeString(con));
            } catch(Throwable t) {
                t.printStackTrace();
                System.out.println("Error writing the constant pool string: '" + con + "'");
                System.exit(1);
            }
            bldM.append("\" /* ");
            bldM.append(offset);
            offset++;
            bldM.append(" */");
        }
        bldM.append("};\n\nint classListSize = ");
        bldM.append(classes.size());
        bldM.append(";\n");

        for(ByteCodeClass bc : classes) {
            bldM.append("extern struct clazz class__");
            bldM.append(bc.getClsName().replace('/', '_').replace('$', '_'));
            bldM.append(";\n");
        }
        // cn1ClazzById: the object header's class INDEX (classId + 1) -> descriptor, for
        // every class and every array class this program has. Index 0 is "no class".
        // The index is 16 bits wide (CN1_OBJ_HEADER_FIELDS); refuse a program whose
        // numbering does not fit rather than wrap an id onto another class.
        // The two java.lang.String twins (cn1InitStringTwin) share String's classId and take
        // the two indices after every array id.
        int maxId = arrayId + 2;
        if (maxId > 0xFFFF) {
            throw new IllegalStateException("Too many classes for the 16-bit object header class "
                    + "index: " + classes.size() + " classes need ids up to " + maxId
                    + " (limit 65535)");
        }
        String[] primitives = {"JAVA_BOOLEAN", "JAVA_CHAR", "JAVA_BYTE", "JAVA_SHORT", "JAVA_INT",
                "JAVA_LONG", "JAVA_FLOAT", "JAVA_DOUBLE"};
        for (ByteCodeClass bc : classes) {
            String n = bc.getClsName().replace('/', '_').replace('$', '_');
            for (int dim = 1; dim <= 3; dim++) {
                if (ByteCodeClass.emitsArrayClass(n, dim)) {
                    bldM.append("extern struct clazz class_array").append(dim).append("__").append(n).append(";\n");
                }
            }
        }
        bldM.append("extern struct clazz ClazzClazz;\n");
        bldM.append("\n\nstruct clazz* const cn1ClazzById[] = {\n    [0] = 0");
        for (ByteCodeClass bc : classes) {
            String n = bc.getClsName().replace('/', '_').replace('$', '_');
            bldM.append(",\n    [cn1_class_id_").append(n).append(" + 1] = &class__").append(n);
            for (int dim = 1; dim <= 3; dim++) {
                if (ByteCodeClass.emitsArrayClass(n, dim)) {
                    bldM.append(",\n    [cn1_array_").append(dim).append("_id_").append(n)
                            .append(" + 1] = &class_array").append(dim).append("__").append(n);
                }
            }
        }
        for (String p : primitives) {
            for (int dim = 1; dim <= 3; dim++) {
                bldM.append(",\n    [cn1_array_").append(dim).append("_id_").append(p)
                        .append(" + 1] = &class_array").append(dim).append("__").append(p);
            }
        }
        // The runtime's descriptor for class objects (nativeMethods.m), classId
        // cn1_array_start_offset -- an id no array takes.
        bldM.append(",\n    [cn1_array_start_offset + 1] = &ClazzClazz");
        bldM.append(",\n    [cn1_header_index_java_lang_String_i8] = &class__java_lang_String_i8");
        bldM.append(",\n    [cn1_header_index_java_lang_String_i16] = &class__java_lang_String_i16");
        bldM.append("};\nconst int cn1ClazzByIdCount = (int)(sizeof(cn1ClazzById) / sizeof(cn1ClazzById[0]));\n");

        bldM.append("\n\nstruct clazz* classesList[] = {");
        first = true;
        for(ByteCodeClass bc : classes) {
            if(first) {
                bldM.append("\n    ");
            } else {
                bldM.append(",\n    ");
            }
            first = false;
            bldM.append("    &class__");
            bldM.append(bc.getClsName().replace('/', '_').replace('$', '_'));
        }
        bldM.append("};\n\n\n");
        
        // generate the markStatics method
        for(ByteCodeClass bc : classes) {
            bc.appendStaticFieldsExtern(bldM);
        }        
        // Eager class initialization: see ByteCodeClass.isEagerInitEligible. Emitted
        // here because this is the one generated file that lists every class.
        bldM.append("\n\n");
        for(ByteCodeClass bc : classes) {
            if(bc.isEagerInitEligible()) {
                bldM.append("extern CN1_CLINIT_ATTR void __STATIC_INITIALIZER_").append(bc.getClsName())
                    .append("(CODENAME_ONE_THREAD_STATE);\n");
            }
        }
        // Two phases. Classes with no <clinit> only build tables and run first, before
        // anything else in initConstantPool. Classes with a PURE <clinit> run Java --
        // they allocate arrays and read string literals -- so they run once the
        // constant pool is published, still before any other Java.
        bldM.append("void cn1EagerInitClasses(CODENAME_ONE_THREAD_STATE) {\n");
        for(ByteCodeClass bc : classes) {
            if(bc.isEagerInitEligible() && !bc.hasClinit()) {
                bldM.append("    __STATIC_INITIALIZER_").append(bc.getClsName()).append("(threadStateData);\n");
            }
        }
        bldM.append("}\n");
        bldM.append("void cn1EagerInitPureClasses(CODENAME_ONE_THREAD_STATE) {\n");
        for(ByteCodeClass bc : classes) {
            if(bc.isEagerInitEligible() && bc.hasClinit()) {
                bldM.append("    cn1RunEagerInitializer(threadStateData, __STATIC_INITIALIZER_").append(bc.getClsName())
                    .append(", \"").append(bc.getClsName()).append("\");\n");
            }
        }
        bldM.append("}\n");
        bldM.append("\n\nextern int recursionKey;\nvoid markStatics(CODENAME_ONE_THREAD_STATE) {\n    recursionKey++;\n");
        for(ByteCodeClass bc : classes) {
            bc.appendStaticFieldsMark(bldM);
        }        
        bldM.append("}\n\n");
        
        
        bld.append("\n\n#endif // __CN1_CLASS_METHOD_INDEX_H__\n");        
        
        FileOutputStream fos = new FileOutputStream(new File(outputDirectory, "cn1_class_method_index.h"));
        fos.write(bld.toString().getBytes(StandardCharsets.UTF_8));
        fos.close();
        ByteCodeTranslator.sourceManifest.recordGenerated("cn1_class_method_index.h");
        fos = new FileOutputStream(new File(outputDirectory, "cn1_class_method_index.m"));
        fos.write(bldM.toString().getBytes(StandardCharsets.UTF_8));
        fos.close();
        ByteCodeTranslator.sourceManifest.recordGenerated("cn1_class_method_index.m");
    }
    
    private static String encodeString(String con) {
        String str = con.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return encodeStringSlashU(str);
    }
    
    private static String encodeStringSlashU(String str) {
        int len = str.length();
        char[] chr = str.toCharArray();
        for(int iter = 0 ; iter < len ; iter++) {
            char c = chr[iter];
            if(c > 127 || c < 32) {
                // needs encoding... Verify there are no more characters to encode
                StringBuilder d = new StringBuilder();
                for(int internal = 0 ; internal < len ; internal++) {
                    c = chr[internal];
                    if(c > 127 || c < 32) {
                        d.append("~~u");
                        d.append(fourChars(Integer.toHexString(c)));
                    } else {
                        d.append(c);
                    }
                }
                return d.toString();
            }
        }
        return str;
    }
    
    private static String fourChars(String s) {
        switch(s.length()) {
            case 1: 
                return "000" + s;
            case 2: 
                return "00" + s;
            case 3: 
                return "0" + s;
        }
        return s;
    }
    
    public static void writeOutput(File outputDirectory) throws Exception {
        // Must run before anything is emitted: the instanceof call sites bake the
        // dense bit index in, so the assignment has to exist before the first one.
        collectTypeTests();
    
        if(ByteCodeTranslator.verbose) {
            System.out.println("outputDirectory is: " + outputDirectory.getAbsolutePath() );
        }
        if(ByteCodeClass.getMainClass()==null){
			System.out.println("Error main class is not defined. The main class name is expected to have a public static void main(String[]) method and it is assumed to reside in the com.package.name directory");
			System.exit(1);
		}
        String file = "Unknown File";
        List<ByteCodeClass> javascriptClassPool = ByteCodeTranslator.output
                == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT
                ? new ArrayList<ByteCodeClass>(classes) : null;
        try {
            for(ByteCodeClass bc : classes) {
                // special case for an object
                if(bc.getClsName().equals("java_lang_Object")) {
                    continue;
                }
                file = bc.getClsName();
                bc.setBaseClassObject(getClassByName(bc.getBaseClass()));
                List<ByteCodeClass> lst = new ArrayList<>();
                for(String s : bc.getBaseInterfaces()) {
					ByteCodeClass byteCode = getClassByName(s);
					if(byteCode == null){
					  System.out.println("Error while working with the class: " + s+" file:"+file+" no class definition");
					} else {
						lst.add(getClassByName(s));
					}
                }
                bc.setBaseInterfacesObject(lst);
            }
            boolean foundNewUnitTests = true;
            while (foundNewUnitTests) {
                foundNewUnitTests = false;
                for (ByteCodeClass bc : classes) {
                    if (!bc.isUnitTest() && bc.getBaseClassObject() != null && bc.getBaseClassObject().isUnitTest()) {
                        bc.setIsUnitTest(true);
                        foundNewUnitTests = true;
                    }
                }
            }

            // load the native sources (including user native code)
            // We need to load native sources before we clear any unmarked classes
            // because a native source may be the only thing referencing a class,
            // and the class may be purged before it even has a shot.
            readNativeFiles(outputDirectory);
            // Snapshot the project's native sources BEFORE the per-class .c/.h are
            // written into the same directory: what is here now is exactly the
            // hand-written and builder-injected code the verifier has to judge, with
            // none of the translator's own output to confuse it.
            List<File> handWrittenNativeSources =
                    NativeSignatureVerifier.listNativeSources(outputDirectory);

            for(ByteCodeClass bc : classes) {
                file = bc.getClsName();
                bc.updateAllDependencies();
            }
            ByteCodeClass.markDependencies(classes, nativeSources);
            Set<ByteCodeClass> unmarked = new HashSet<>(classes);
            classes = ByteCodeClass.clearUnmarked(classes);
            classes.forEach(unmarked::remove);
            int neliminated = 0;
            for (ByteCodeClass removedClass : unmarked) {
                removedClass.setEliminated(true);
                neliminated++;
            }
            // On the raw bytecode, before any fusion pass below rewrites instructions:
            // see ByteCodeClass.isEagerInitEligible.
            ByteCodeClass.computePureClinits(classes);
            // Also on the raw bytecode, where every field access is still a plain
            // GETFIELD/PUTFIELD: see DeadFieldElimination. Not for the JavaScript target
            // (its own field model), and not under on-device debugging, whose sidecar
            // shows every field the source declares.
            if (BytecodeMethod.optimizerOn
                    && ByteCodeTranslator.output != ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT
                    && !"true".equalsIgnoreCase(Util.getProperty("cn1.onDeviceDebug", "false"))) {
                DeadFieldElimination.run(classes, nativeSources);
            }

            // Fuse all-String StringBuilder concat chains into String.cn1ConcatN
            // BEFORE the cull, not during code generation.
            //
            // The cull decides what to keep from the dependency graph, and the graph
            // is only told about a call when the instruction is added. A rewrite that
            // runs later -- inside BytecodeMethod.optimize(), which happens during
            // generateCCode -- inserts calls to methods the cull has already deleted,
            // and a deleted method is emitted as `return 0;`. That is not a build
            // error: the rewritten call silently answered null, java.io.File got a
            // null path, and the translator died in File.getParentFile with a SIGSEGV
            // nowhere near the rewrite. Running here, the references exist before
            // anything is eliminated. See BytecodeMethod.lowerIteratorCalls.
            if (BytecodeMethod.optimizerOn) {
                LocalReceiverTypes.resolveFactories(getNativeSymbolIndex(nativeSources));
                iteratorStackCensus();
                allocationEscapeCensus();
                // NOTE: the retire counters are deliberately NOT reported from here.
                // The pass runs per method inside optimize(), so totals are not final at
                // this point, and a shutdown hook does not compile in this build (the
                // Runtime/Thread visible to the translator has no addShutdownHook). The
                // population is measured from the generated C instead, by counting
                // __cn1retire scopes -- which is the number that actually matters,
                // because it counts what codegen EMITTED rather than what analysis liked.
                for (ByteCodeClass ownershipClass : classes) {
                    for (BytecodeMethod method : ownershipClass.getMethods()) method.analyzeBuilderOwnership();
                }
                for (ByteCodeClass ownershipClass : classes) {
                    for (BytecodeMethod method : ownershipClass.getMethods()) method.freezeBuilderOwnership();
                }
                for (ByteCodeClass fuseCls : classes) {
                    for (BytecodeMethod fuseMtd : fuseCls.getMethods()) {
                        // These rewrites preserve local-stack semantics but introduce
                        // instructions outside the raw-bytecode eligibility whitelist.
                        // Prove the frame requirement before replacing that bytecode.
                        fuseMtd.freezeFramelessEligibility();
                        // BEFORE lowerIteratorCalls, which retypes the very calls this
                        // recognises: after it they are INVOKEVIRTUAL on the concrete
                        // iterator and the for-each shape no longer matches.
                        // The C intrinsic first: it removes the iterator entirely where
                        // it applies, so a buffer offer would be dead weight there.
                        if (ByteCodeTranslator.output != ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
                            fuseMtd.fuseStreams();
                            fuseMtd.intrinsifyForEach();
                            fuseMtd.markStackIterators();
                        }
                        fuseMtd.lowerIteratorCalls();
                        fuseMtd.elideToCharArrayScans();
                    }
                }
            }

            // loop over methods and start eliminating the body of unused methods
            if (BytecodeMethod.optimizerOn) {
                if(ByteCodeTranslator.verbose) {
                    System.out.println("Optimizer On: Removing unused methods and classes...");
                }
                Date now = new Date();
                neliminated += eliminateUnusedMethods();
                Date later = new Date();
                long dif = later.getTime()-now.getTime();
                if(ByteCodeTranslator.verbose) {
                    System.out.println("unused Method cull removed "+neliminated+" methods in "+(dif/1000)+" seconds");
                }
            }

            // JavaScript-target-only Rapid Type Analysis pass. Runs AFTER
            // the existing desc.name-keyed culler so we start from an
            // already-pruned class list. RTA only eliminates additional
            // methods the conservative graph considered "used" because
            // some OTHER class with the same name+desc was invoked; it
            // never resurrects methods the earlier pass removed. Gated
            // on OUTPUT_TYPE_JAVASCRIPT because the iOS runtime relies on
            // different dispatch mechanics and may break under stricter
            // reachability.
            // Unconditionally, and BEFORE the RTA decision: the suspension
            // analysis below reads what RTA published, and skipping RTA must
            // mean "no information", not "the previous application's answer".
            if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
                JavascriptReachability.resetExportedFacts();
            }
            if (BytecodeMethod.optimizerOn
                    && ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT
                    && System.getProperty("parparvm.js.rta.off") == null) {
                Date rtaStart = new Date();
                int rtaEliminated = JavascriptReachability.run(
                        classes, javascriptClassPool, nativeSources);
                Date rtaEnd = new Date();
                long rtaDif = rtaEnd.getTime() - rtaStart.getTime();
                if (ByteCodeTranslator.verbose) {
                    System.out.println("JS RTA pass removed " + rtaEliminated + " additional methods in "
                            + (rtaDif / 1000) + " seconds");
                }
                neliminated += rtaEliminated;
            }

            // JavaScript-target-only suspension analysis: decide which
            // surviving methods can be emitted as plain ``function``
            // (no generator allocation / no yield*) vs which must stay
            // as ``function*``. Must run after RTA so eliminated
            // methods don't pollute the analysis.
            if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
                Date suspStart = new Date();
                int syncCount = JavascriptSuspensionAnalysis.run(classes, outputDirectory);
                Date suspEnd = new Date();
                if (ByteCodeTranslator.verbose) {
                    System.out.println("JS suspension analysis: " + syncCount
                            + " methods classified synchronous in "
                            + ((suspEnd.getTime() - suspStart.getTime()) / 1000) + " seconds");
                }
            }

            if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
                JavascriptBundleWriter.write(outputDirectory, classes);
            } else {
                // Opt-in (CN1_NATIVE_VERIFY / -Dparparvm.nativeVerify), and a no-op
                // otherwise: before a line of C is emitted, every native method that
                // survived into this program must have an implementation the
                // generated code can actually call. The JavaScript target has its
                // own registry (JavascriptNativeRegistry) and reports its own
                // missing natives, so it is not run through this.
                verifyNativeMethods(handWrittenNativeSources);

                generateClassAndMethodIndexHeader(outputDirectory);

                boolean concatenate = "true".equals(Util.getProperty("concatenateFiles", "false"));
                ConcatenatingFileOutputStream cos = concatenate ? new ConcatenatingFileOutputStream(outputDirectory) : null;

                for(ByteCodeClass bc : classes) {
                    file = bc.getClsName();
                    writeFile(bc, outputDirectory, cos);
                }
                if (cos != null) cos.realClose();

                if (BytecodeMethod.isOnDeviceDebug()) {
                    writeSymbolSidecar(outputDirectory);
                }
            }
        } catch(NativeSignatureVerifier.VerificationFailedException t) {
            // Not a translation failure: the report above already says exactly which
            // native methods are wrong and what to write instead. Naming whichever
            // class the loop happened to be on, and dumping a stack into the build
            // log, would bury it.
            throw t;
        } catch(Throwable t) {
            System.out.println("Error while working with the class: " + file);
            t.printStackTrace();
            if(t instanceof Exception) {
                throw (Exception)t;
            }
            // Errors (notably OutOfMemoryError while emitting a very large
            // bundle) previously fell through here, so the translator exited
            // 0 with a half-written dist (e.g. parparvm_runtime.js but no
            // worker.js / translated_app.js). Rethrow so the caller fails
            // loudly instead of shipping a truncated app bundle.
            if(t instanceof Error) {
                throw (Error)t;
            }
            throw new RuntimeException(t);
        }
        finally { cleanup(); }
    }
    
    private static void readNativeFiles(File outputDirectory) throws IOException {
        File[] mFiles = Util.listFiles(outputDirectory, file ->
                file.getName().endsWith(".m") || file.getName().endsWith("." + ByteCodeTranslator.output.extension()));
        if(mFiles == null) {
            return;
        }
        nativeSources = new String[mFiles.length];
        int size = 0;
        if(ByteCodeTranslator.verbose) {
            System.out.println(mFiles.length + " native files");
        }
        for(int iter = 0 ; iter < mFiles.length ; iter++) { 
        	FileInputStream fi = new FileInputStream(mFiles[iter]);
            DataInputStream di = new DataInputStream(fi);
            int len = (int)mFiles[iter].length();
            size += len;
            byte[] dat = new byte[len];
            di.readFully(dat);
            fi.close();
            nativeSources[iter] = new String(dat, StandardCharsets.UTF_8);
        }
        if(ByteCodeTranslator.verbose) {
            System.out.println("Native files total "+(size/1024)+"K");
        }
    }

    /**
     * Fails the translation when a native method that survived into this program has
     * no C implementation, or has one whose prototype the generated call site would
     * not match.
     *
     * <p><b>Does nothing unless asked.</b> {@code -Dparparvm.nativeVerify} or
     * {@code CN1_NATIVE_VERIFY} has to select {@code strict} or {@code warn}; the
     * default is off, so an app that builds today keeps building even if it carries
     * a native declaration nobody implements. Codename One's CI sets the environment
     * variable for the whole job, which is where the check actually runs.</p>
     *
     * <p>When it does run it covers EVERY native method of every surviving class,
     * not just the ones the dead-code pass kept: a native method is kept alive
     * precisely by its symbol appearing in the native sources (see
     * {@link BytecodeMethod#isMethodUsedByNative}), so a misspelled implementation
     * makes the method look unused and it is dropped. Keying the check on "survived
     * elimination" would therefore skip exactly the methods it exists to catch.</p>
     *
     * @param handWrittenNativeSources the project's native sources as they were
     *        before the translator wrote its own C into the same directory
     */
    private static void verifyNativeMethods(List<File> handWrittenNativeSources) throws IOException {
        NativeSignatureVerifier.Mode mode = NativeSignatureVerifier.mode();
        if (mode == NativeSignatureVerifier.Mode.OFF) {
            return;
        }
        List<NativeSignatureVerifier.Signature> required =
                new ArrayList<NativeSignatureVerifier.Signature>();
        for (ByteCodeClass bc : classes) {
            for (BytecodeMethod m : bc.getMethods()) {
                if (m.isNative()) {
                    required.add(m.getNativeSignature());
                }
            }
        }
        if (required.isEmpty()) {
            return;
        }
        NativeSignatureVerifier.SourceIndex index = new NativeSignatureVerifier.SourceIndex(
                handWrittenNativeSources, NativeSignatureVerifier.bundledRuntimeSources());
        List<NativeSignatureVerifier.Problem> problems =
                NativeSignatureVerifier.verify(required, index);
        if (problems.isEmpty()) {
            if (ByteCodeTranslator.verbose) {
                System.out.println("Native signature check: " + required.size()
                        + " native method(s) verified against " + index.size()
                        + " C definition(s).");
            }
            return;
        }
        // Warnings are collapsed to one line here on purpose: they are near-miss C
        // functions nothing calls, which in an app build almost always live in a port
        // or a cn1lib the app author did not write. Listing them in full would bury
        // the errors that ARE theirs. scripts/check-native-signatures.sh prints them.
        int fatal = NativeSignatureVerifier.report(problems, mode,
                required.size() + " native methods", false);
        if (fatal > 0) {
            throw new NativeSignatureVerifier.VerificationFailedException(
                    "Native signature check failed: " + fatal
                    + " native method(s) have no callable C implementation."
                    + " Fix the names listed above, or set parparvm.nativeVerify /"
                    + " CN1_NATIVE_VERIFY to 'warn' to downgrade this to a warning.");
        }
    }

    private static int eliminateUnusedMethods() {
        return(eliminateUnusedMethods(false, 0));
    }

    private static int eliminateUnusedMethods(boolean forceFound, int depth) {
        int nfound = cullMethods();
        nfound += cullClasses(nfound>0 || forceFound, depth);
        return(nfound);
    }

    private static int cullMethods() {
        cn1InvalidateDevirtMemo();
        int nfound = 0;
        for(ByteCodeClass bc : classes) {
            bc.unmark();
            if(bc.isIsInterface() || bc.getBaseClass() == null) {
                continue;
            }
            for(BytecodeMethod mtd : bc.getMethods()) {
                // Pure-Java twins that the JS runtime's bindNative delegates call
                // (getImpl/putImpl/toStringImpl/valueOfHeap...): no bytecode call
                // site exists, so without this keep they would be culled and the
                // JS delegation would throw ReferenceError. Kept on all targets --
                // the C natives use some of them as fallbacks too.
                if(JavascriptNativeRegistry.isRuntimeDelegateTarget(bc.getClsName(), mtd.getMethodName())) {
                    continue;
                }
                if(mtd.isEliminated() || mtd.isMain() || mtd.getMethodName().equals("__CLINIT__") || mtd.getMethodName().equals("finalize") || mtd.isNative()) {
                    if (!mtd.isEliminated() && mtd.getMethodName().contains("yield")) {
                        if(ByteCodeTranslator.verbose) {
                            System.out.println("Not eliminating method ");
                            System.out.println("main="+mtd.isMain()+", isNative="+mtd.isNative());
                        }
                    }
                    continue;
                }
                // Preserve virtual-root methods of java.lang.Object when
                // on-device-debug is on. jdb's `print` formats objects by
                // calling Object.toString, and a debugger user can ask to
                // invoke equals/hashCode/etc. without a static call site
                // to keep their body alive on its own.
                if (BytecodeMethod.isOnDeviceDebug()
                        && "java_lang_Object".equals(bc.getClsName())
                        && !mtd.isStatic()) {
                    continue;
                }

                if(!isMethodUsed(mtd, bc)) {
                    if(isMethodUsedByBaseClassOrInterface(mtd, bc)) {
                        continue;
                    }
                    mtd.setEliminated(true);
                    dependencyGraph.removeMethod(mtd);
                    nfound++;
                }
            }

        }
        return nfound;
    }

    private static boolean isMethodUsedByBaseClassOrInterface(BytecodeMethod mtd, ByteCodeClass cls) {
        boolean b = checkMethodUsedByBaseClassOrInterface(mtd, cls.getBaseClassObject());
        if(b) {
            return true;
        }
        for(ByteCodeClass bc : cls.getBaseInterfacesObject()) {
            b = checkMethodUsedByBaseClassOrInterface(mtd, bc);
            if(b) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkMethodUsedByBaseClassOrInterface(BytecodeMethod mtd, ByteCodeClass cls) {
        if(cls == null) {
            return false;
        }
        if(cls.getBaseInterfacesObject() != null) {
            for(ByteCodeClass bc : cls.getBaseInterfacesObject()) {
                for(BytecodeMethod m :  bc.getMethods()) {
                    if (m.isEliminated()) continue;
                    if(m.getMethodName().equals(mtd.getMethodName())) {
                        if (isMethodUsed(m, bc)) {
                            return true;
                        }
                        break;
                    }
                }
            }
        }
        for(BytecodeMethod m :  cls.getMethods()) {
            if (m.isEliminated()) continue;
            if(m.getMethodName().equals(mtd.getMethodName())) {
                if(isMethodUsed(m, cls)) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private static int cullClasses(boolean found, int depth) {
        if(ByteCodeTranslator.verbose) {
            System.out.println("cullClasses()");
        }
        if(found && depth < 4) {
            for(ByteCodeClass bc : classes) {
                bc.updateAllDependencies();
            }   

            ByteCodeClass.markDependencies(classes, nativeSources);
            List<ByteCodeClass> tmp = ByteCodeClass.clearUnmarked(classes);

            // 2nd pass to mark classes as eliminated so that we can propagate down to each
            // method of the class to mark it eliminated so that virtual methods
            // aren't included later on when writing virtual methods
            // LinkedHashSet, not HashSet: ByteCodeClass overrides neither equals nor
            // hashCode, so a HashSet here iterates in identity-hash order. Elimination
            // is greedy and monotone -- isMethodUsed treats an already-eliminated
            // caller as no caller -- so with a cycle in the call graph the ORDER
            // decides which member of the cycle survives. Two runtimes hash
            // identities differently and culled different methods from the same
            // input; translating the translator with itself is what exposed it.
            Set<ByteCodeClass> removedClasses = new LinkedHashSet<>(classes);
            tmp.forEach(removedClasses::remove);
            int nfound = 0;
            for (ByteCodeClass cls : removedClasses) {
                nfound += cls.setEliminated(true);
                dependencyGraph.removeClass(cls.getClsName());
            }
            classes = tmp;
            return nfound + eliminateUnusedMethods(nfound > 0, depth + 1);
        }

        // Note: We may still have a lot of classes that are kept around solely because
        // they implement an interface that is used.
        // We should try to remove such classes
        return 0;
    }
    

    
    private static boolean isMethodUsed(BytecodeMethod m, ByteCodeClass cls) {
        if (!m.isEliminated() && m.isMethodUsedByNative(nativeSources, cls)) {
            return true;
        }
        List<BytecodeMethod> callers = dependencyGraph.getCallers(m.getLookupSignature());
        for (BytecodeMethod caller : callers) {
            if(caller.isEliminated() || caller == m) {
                continue;
            }
            if(caller.isMethodUsed(m)) {
                return true;
            }
        }
        return false;
    }

    private static void writeFile(ByteCodeClass cls, File outputDir, ConcatenatingFileOutputStream writeBufferInstead) throws Exception {
        OutputStream outMain =
                // isApple(), not == OUTPUT_TYPE_IOS: the native macOS target emits
                // the same Objective-C into the same concatenated buffer, and
                // comparing against the iOS constant alone would silently drop
                // it back to one file per class.
                writeBufferInstead != null && ByteCodeTranslator.output.isApple() ?
                        writeBufferInstead :
                        new FileOutputStream(new File(outputDir, cls.getClsName() + "." + ByteCodeTranslator.output.extension()));

        if (outMain instanceof ConcatenatingFileOutputStream) {
            ((ConcatenatingFileOutputStream)outMain).beginNextFile(cls.getClsName());
        } else {
            // Only the one-file-per-class case has a file name worth recording. Under
            // concatenation the classes are bucketed into concatenated_<n> and the
            // provenance of an individual class is genuinely gone by the time the
            // compiler sees it; the manifest says so by simply not naming these.
            ByteCodeTranslator.sourceManifest.recordGenerated(
                    cls.getClsName() + "." + ByteCodeTranslator.output.extension());
        }
        if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_JAVASCRIPT) {
            outMain.write(cls.generateJavascriptCode(classes).getBytes(StandardCharsets.UTF_8));
            outMain.close();
        } else {
            outMain.write(cls.generateCCode(classes).getBytes(StandardCharsets.UTF_8));
            outMain.close();

            // we also need to write the header file for C outputs
            String headerName = cls.getClsName() + ".h";
            try(FileOutputStream outHeader = new FileOutputStream(new File(outputDir, headerName))) {
                outHeader.write(cls.generateCHeader().getBytes(StandardCharsets.UTF_8));
            }
            // The header is per-class even when the bodies are concatenated, so it is
            // always recordable -- and it is where a good share of the generated-code
            // diagnostics land, since every class that depends on this one includes it.
            ByteCodeTranslator.sourceManifest.recordGenerated(headerName);
        }
    }
    
    public Parser() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visitEnd() {
        super.visitEnd(); 
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        BytecodeMethod mtd = new BytecodeMethod(clsName, access, name, desc, signature, exceptions);
        cls.addMethod(mtd);
        // Tee the (post-JSR-inlined) bytecode into a MethodNode so visitEnd can run
        // ASM frame analysis to resolve category-2-aware DUP/POP2 forms. The wrapper
        // sits INSIDE the JSRInlinerAdapter, so the MethodNode it feeds matches the
        // instruction stream BytecodeMethod is built from. Parser's own ClassVisitor
        // has no delegate writer (super.visitMethod returns null), so routing the
        // MethodNode as the wrapper's delegate loses nothing.
        MethodNode analysisNode = new MethodNode(Opcodes.ASM9, access, name, desc, signature, exceptions);
        MethodVisitorWrapper wrapper = new MethodVisitorWrapper(analysisNode, mtd);
        wrapper.dupAnalysisOwner = clsName;
        wrapper.dupAnalysisNode = analysisNode;
        return new JSRInlinerAdapter(wrapper, access, name, desc, signature, exceptions);
    }

    // Category-sensitive stack opcodes: their correct operand-stack-ENTRY shuffle
    // depends on whether the operands are category-2 (long/double), because the JS
    // backend models a long/double as ONE entry while the JVM defines these in slots.
    private static boolean isCategorySensitiveStackOp(int op) {
        return op == Opcodes.DUP2 || op == Opcodes.DUP2_X1 || op == Opcodes.DUP2_X2
                || op == Opcodes.DUP_X2 || op == Opcodes.POP2;
    }

    /**
     * Runs ASM frame analysis over the parsed MethodNode and stamps each
     * category-sensitive DUP/POP2 ``BasicInstruction`` with its resolved
     * entry-form (see {@link BasicInstruction#setDupForm}). On any analysis
     * failure the forms are left unset and the emitter uses its legacy
     * (category-1-assuming) path -- i.e. no worse than before.
     */
    private static void resolveDupForms(String owner, MethodNode mn, BytecodeMethod mtd,
            Frame<? extends org.objectweb.asm.tree.analysis.Value>[] existingFrames) {
        if (owner == null || mn == null || mn.instructions == null || mn.instructions.size() == 0) {
            return;
        }
        boolean needsForms = false;
        for (AbstractInsnNode instruction = mn.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (isCategorySensitiveStackOp(instruction.getOpcode())) { needsForms = true; break; }
        }
        if (!needsForms) return;
        Frame<? extends org.objectweb.asm.tree.analysis.Value>[] frames = existingFrames;
        if (frames == null) {
            try {
                frames = new Analyzer<BasicValue>(new BasicInterpreter()).analyze(owner, mn);
            } catch (org.objectweb.asm.tree.analysis.AnalyzerException error) {
                return;
            }
        }
        // Decisions for category-sensitive ops, in linear (parse) order.
        List<int[]> decisions = new ArrayList<int[]>();
        AbstractInsnNode[] insns = mn.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            int op = insns[i].getOpcode();
            if (!isCategorySensitiveStackOp(op)) {
                continue;
            }
            decisions.add(dupForm(op, frames[i]));
        }
        // Stamp the matching ParparVM BasicInstructions, in the same linear order.
        // The dup/pop2 sequence is identical between the ASM node and the
        // BytecodeMethod (both built from the same post-JSR stream), so they zip
        // 1:1. Stamping the instruction object (not a positional queue) keeps the
        // form correct even though the structured emitter later reorders blocks.
        int di = 0;
        for (Instruction instr : mtd.getInstructions()) {
            if (!(instr instanceof BasicInstruction) || !isCategorySensitiveStackOp(instr.getOpcode())) {
                continue;
            }
            if (di >= decisions.size()) {
                break;
            }
            int[] form = decisions.get(di++);
            if (form != null) {
                ((BasicInstruction) instr).setDupForm(form[0], form[1]);
            }
        }
    }

    /**
     * Resolves a category-sensitive stack opcode into entry terms given the frame
     * BEFORE it. Returns {nDup, nSkip} for the DUP family (duplicate the top nDup
     * entries, reinsert beneath the next nSkip), or {entriesToPop, -1} for POP2.
     * Returns null when the frame is unavailable (unreachable code).
     * ASM analysis frames model a long/double as ONE stack entry with size 2.
     */
    private static int[] dupForm(int op, Frame<? extends org.objectweb.asm.tree.analysis.Value> f) {
        if (f == null) {
            return null;
        }
        int sp = f.getStackSize();
        if (sp < 1) {
            return null;
        }
        boolean topWide = f.getStack(sp - 1).getSize() == 2;
        switch (op) {
            case Opcodes.POP2:
                return new int[]{ topWide ? 1 : 2, -1 };
            case Opcodes.DUP2:
                return topWide ? new int[]{1, 0} : new int[]{2, 0};
            case Opcodes.DUP2_X1:
                return topWide ? new int[]{1, 1} : new int[]{2, 1};
            case Opcodes.DUP_X2: {
                // Top is category-1; skip one entry if the value below is category-2.
                boolean belowWide = sp >= 2 && f.getStack(sp - 2).getSize() == 2;
                return belowWide ? new int[]{1, 1} : new int[]{1, 2};
            }
            case Opcodes.DUP2_X2:
                if (topWide) {
                    boolean belowWide = sp >= 2 && f.getStack(sp - 2).getSize() == 2;
                    return belowWide ? new int[]{1, 1} : new int[]{1, 2};
                } else {
                    // Top two entries are category-1; skip one entry if the value
                    // beneath them is category-2.
                    boolean belowWide = sp >= 3 && f.getStack(sp - 3).getSize() == 2;
                    return belowWide ? new int[]{2, 1} : new int[]{2, 2};
                }
            default:
                return null;
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        ByteCodeField fld = new ByteCodeField(clsName, access, name, desc, signature, value);
        cls.addField(fld);
        return new FieldVisitorWrapper(super.visitField(access, name, desc, signature, value));
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if(name.equals(cls.getOriginalClassName())) {
            cls.setIsAnonymous(innerName==null);
        }
        super.visitInnerClass(name, outerName, innerName, access); 
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr); 
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return new AnnotationVisitorWrapper(super.visitTypeAnnotation(typeRef, typePath, desc, visible)); 
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (STACK_ALLOCATE_ANNOTATION.equals(desc)) {
            cls.setStackAllocatable(true);
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
        }
        if (FUSED_ANNOTATION.equals(desc)) {
            cls.setFused(true);
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
        }
        if (CONCRETE_ANNOTATION.equals(desc)) {
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible)) {
                private String defaultConcrete;
                private String winConcrete;
                private String linuxConcrete;
                private String macConcrete;

                @Override
                public void visit(String name, Object value) {
                    if ("name".equals(name) && value instanceof String) {
                        defaultConcrete = (String) value;
                    } else if ("win".equals(name) && value instanceof String) {
                        winConcrete = (String) value;
                    } else if ("linux".equals(name) && value instanceof String) {
                        linuxConcrete = (String) value;
                    } else if ("mac".equals(name) && value instanceof String) {
                        macConcrete = (String) value;
                    }
                    super.visit(name, value);
                }

                @Override
                public void visitEnd() {
                    // Pick the concrete implementation for the active translation
                    // target: the native Windows build uses @Concrete.win(), the
                    // native Linux build uses @Concrete.linux(), the native macOS
                    // build uses @Concrete.mac(), every other target uses
                    // @Concrete.name() (the iOS pipeline). When building
                    // Windows/Linux/macOS and no win()/linux()/mac() is given,
                    // leave the concrete unset so the portable base class is
                    // translated instead of pulling in the absent iOS class.
                    //
                    // macOS is the one target that may legitimately name an
                    // iOS class here: it shares the Apple native binding classes
                    // with the iOS port, so e.g. IOSSimd is the correct mac()
                    // value even though it lives in com.codename1.impl.ios.
                    String target = ByteCodeClass.getConcreteTarget();
                    String concrete;
                    if ("win".equals(target)) {
                        concrete = winConcrete;
                    } else if ("linux".equals(target)) {
                        concrete = linuxConcrete;
                    } else if ("mac".equals(target)) {
                        concrete = macConcrete;
                    } else {
                        concrete = defaultConcrete;
                    }
                    if (concrete != null && concrete.length() > 0) {
                        cls.setConcreteClass(concrete.replace('.', '/'));
                    }
                    super.visitEnd();
                }
            };
        }
        return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        super.visitOuterClass(owner, name, desc); 
    }

    @Override
    public void visitSource(String source, String debug) {
        cls.setSourceFile(source);
        super.visitSource(source, debug); 
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cls.setBaseClass(superName);
        cls.setBaseInterfaces(interfaces);
        if((access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT) {
            cls.setIsAbstract(true);
        }
        if((access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE) {
            cls.setIsInterface(true);
        }
        if((access & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION) {
            cls.setIsAnnotation(true);
        }
        if((access & Opcodes.ACC_SYNTHETIC) == Opcodes.ACC_SYNTHETIC) {
            cls.setIsSynthetic(true);
        }
        
        if((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
            cls.setFinalClass(true);
        }
        if ("com/codename1/testing/UnitTest".equals(superName) || "com/codename1/testing/AbstractTest".equals(superName)) {
            cls.setIsUnitTest(true);
        }
        if ((access & Opcodes.ACC_ENUM) == Opcodes.ACC_ENUM) {
            cls.setIsEnum(true);
        }
        super.visit(version, access, name, signature, superName, interfaces); 
    }    
    
    class MethodVisitorWrapper extends MethodVisitor {
        private final BytecodeMethod mtd;
        String dupAnalysisOwner;
        MethodNode dupAnalysisNode;
        // One entry per invokedynamic in this method, in visit order: the synthetic
        // lambda class it becomes, or null for an indy that is not a lambda (string
        // concat). The analysis below walks a SEPARATE MethodNode whose nodes are not
        // the ones visited here, so position is what ties the two together.
        final java.util.List<String> indyLambdas = new java.util.ArrayList<String>();
        // A lambda cannot be invoked through a local before it is created, so a SAM
        // call only becomes worth analysing once one has been seen in this method.
        // This keeps the dataflow off the overwhelming majority of methods.
        boolean sawLambdaIndy;
        final java.util.Map<org.objectweb.asm.tree.AbstractInsnNode, com.codename1.tools.translator.bytecodes.Invoke> flowInvokes =
                new java.util.IdentityHashMap<org.objectweb.asm.tree.AbstractInsnNode, com.codename1.tools.translator.bytecodes.Invoke>();
        public MethodVisitorWrapper(MethodVisitor mv, BytecodeMethod mtd) {
            super(Opcodes.ASM9, mv);
            this.mtd = mtd;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            // LEVER B: snapshot the plans that must be read off the RAW instruction
            // list now, before optimize() (run later, per-class) folds the PUTFIELDs.
            mtd.computeRawMethodPlans();
            Frame<? extends org.objectweb.asm.tree.analysis.Value>[] flowFrames = BytecodeMethod.optimizerOn
                    ? LocalReceiverTypes.capture(dupAnalysisOwner, dupAnalysisNode, flowInvokes, mtd,
                            indyLambdas) : null;
            resolveDupForms(dupAnalysisOwner, dupAnalysisNode, mtd, flowFrames);
            // MethodNode uses Label.info as its label-to-tree-node map. These
            // Labels survive in our IR, so leaving that map installed retains
            // the complete doubly linked ASM instruction tree after analysis.
            // Both analyses have consumed it; code generation needs identity only.
            for (Instruction instruction : mtd.getInstructions()) {
                if (instruction instanceof LabelInstruction) {
                    Label label = ((LabelInstruction) instruction).getLabel();
                    if (label.info instanceof org.objectweb.asm.tree.LabelNode) label.info = null;
                }
            }
            flowInvokes.clear();
            indyLambdas.clear();
            sawLambdaIndy = false;
            dupAnalysisNode = null;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mtd.setMaxes(maxStack, maxLocals);
            super.visitMaxs(maxStack, maxLocals); 
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            mtd.addDebugInfo(line);
            super.visitLineNumber(line, start); 
        }

        @Override
        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
            return new AnnotationVisitorWrapper(super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible));
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            mtd.addLocalVariable(name, desc, signature, start, end, index);
            super.visitLocalVariable(name, desc, signature, start, end, index); 
        }

        @Override
        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return new AnnotationVisitorWrapper(super.visitTryCatchAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            mtd.addTryCatchBlock(start, end, handler, type);
            super.visitTryCatchBlock(start, end, handler, type); 
        }

        @Override
        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return new AnnotationVisitorWrapper(super.visitInsnAnnotation(typeRef, typePath, desc, visible)); 
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            mtd.addMultiArray(desc, dims);
            super.visitMultiANewArrayInsn(desc, dims); 
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            mtd.addSwitch(dflt, keys, labels);
            super.visitLookupSwitchInsn(dflt, keys, labels); 
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            int[] keys = new int[labels.length];
            int counter = min;
            for(int iter = 0 ; iter < keys.length ; iter++) {
                keys[iter] = counter;
                counter++;
            }
            mtd.addSwitch(dflt, keys, labels);
            super.visitTableSwitchInsn(min, max, dflt, labels); 
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            mtd.addIInc(var, increment);
            super.visitIincInsn(var, increment); 
        }

        @Override
        public void visitLdcInsn(Object cst) {
            mtd.addLdc(cst);
            super.visitLdcInsn(cst); 
        }

        @Override
        public void visitLabel(Label label) {
            mtd.addLabel(label);
            super.visitLabel(label); 
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            mtd.addJump(opcode, label);
            super.visitJumpInsn(opcode, label); 
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
            indyLambdas.add(null);
            if ("java/lang/invoke/StringConcatFactory".equals(bsm.getOwner()) &&
                ("makeConcatWithConstants".equals(bsm.getName()) || "makeConcat".equals(bsm.getName()))) {

                Type invokedType = Type.getMethodType(desc);
                if (!Type.getType(String.class).equals(invokedType.getReturnType())) {
                    return;
                }

                String helperName = "cn1$concat$" + (stringConcatCounter++);
                BytecodeMethod helper = new BytecodeMethod(clsName, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, helperName, desc, null, null);
                cls.addMethod(helper);

                // FAST PATH: when every argument is already String-typed (the common interpolation /
                // a + b + c shape), build the result compactly in one pass via String.cn1ConcatN
                // instead of the char[]-backed StringBuilder -- which decodes each compact byte[] arg
                // to char on append and re-encodes to byte in toString, over a 2-byte/char scratch
                // buffer (4 allocations + 2 conversions per concat). cn1ConcatN is 2 allocations and
                // no conversion. Only for 2..5 total parts (constants + args); anything else, or a
                // non-String arg, falls through to the general StringBuilder helper below.
                boolean cn1AllStringArgs = invokedType.getArgumentTypes().length > 0;
                for (Type at : invokedType.getArgumentTypes()) {
                    if (at.getSort() != Type.OBJECT || !"java/lang/String".equals(at.getInternalName())) {
                        cn1AllStringArgs = false;
                        break;
                    }
                }
                if (cn1AllStringArgs) {
                    // Ordered parts: a String means "literal/constant -> LDC", an Integer means
                    // "arg at that local -> ALOAD". Empty literals are dropped.
                    List<Object> cn1Parts = new ArrayList<>();
                    Type[] cn1Ats = invokedType.getArgumentTypes();
                    if ("makeConcat".equals(bsm.getName())) {
                        int li = 0;
                        for (Type at : cn1Ats) { cn1Parts.add(Integer.valueOf(li)); li += at.getSize(); }
                    } else {
                        String rcp = bsmArgs != null && bsmArgs.length > 0 ? String.valueOf(bsmArgs[0]) : "";
                        List<String> csts = new ArrayList<>();
                        if (bsmArgs != null) {
                            for (int i = 1; i < bsmArgs.length; i++) csts.add(String.valueOf(bsmArgs[i]));
                        }
                        int ci = 0, li = 0, ai = 0;
                        StringBuilder lit = new StringBuilder();
                        for (int i = 0; i < rcp.length(); i++) {
                            char ch = rcp.charAt(i);
                            if (ch == '\u0001') {
                                if (lit.length() > 0) { cn1Parts.add(lit.toString()); lit.setLength(0); }
                                if (ai < cn1Ats.length) { cn1Parts.add(Integer.valueOf(li)); li += cn1Ats[ai++].getSize(); }
                            } else if (ch == '\u0002') {
                                if (lit.length() > 0) { cn1Parts.add(lit.toString()); lit.setLength(0); }
                                if (ci < csts.size()) cn1Parts.add(csts.get(ci++));
                            } else {
                                lit.append(ch);
                            }
                        }
                        if (lit.length() > 0) cn1Parts.add(lit.toString());
                        while (ai < cn1Ats.length) { cn1Parts.add(Integer.valueOf(li)); li += cn1Ats[ai++].getSize(); }
                    }
                    int cn1N = cn1Parts.size();
                    if (cn1N >= 2 && cn1N <= 5) {
                        for (Object p : cn1Parts) {
                            if (p instanceof String) {
                                helper.addLdc((String) p);
                            } else {
                                helper.addVariableOperation(Opcodes.ALOAD, ((Integer) p).intValue());
                            }
                        }
                        StringBuilder cn1Sig = new StringBuilder("(");
                        for (int k = 0; k < cn1N; k++) cn1Sig.append("Ljava/lang/String;");
                        cn1Sig.append(")Ljava/lang/String;");
                        helper.addInvoke(Opcodes.INVOKESTATIC, "java/lang/String", "cn1Concat" + cn1N, cn1Sig.toString(), false);
                        helper.addInstruction(Opcodes.ARETURN);
                        int cn1MaxLocal = 0;
                        for (Type t : cn1Ats) cn1MaxLocal += t.getSize();
                        helper.setMaxes(cn1N + 1, cn1MaxLocal + 2);
                        mtd.addInvoke(Opcodes.INVOKESTATIC, clsName, helperName, desc, false);
                        return;
                    }
                }

                // Pre-size the StringBuilder from the recipe literals + per-argument length
                // estimates so the common-case concat never grows its char[] (each growth is
                // a fresh array + arraycopy). Over-estimates are harmless; under-estimates
                // (e.g. a long String arg) still grow correctly.
                int cn1Cap = 0;
                if ("makeConcatWithConstants".equals(bsm.getName())) {
                    String rcp = bsmArgs != null && bsmArgs.length > 0 ? String.valueOf(bsmArgs[0]) : "";
                    for (int ci = 0; ci < rcp.length(); ci++) {
                        char rc = rcp.charAt(ci);
                        if (rc != 0x0001 && rc != 0x0002) cn1Cap++; // skip arg/const markers
                    }
                    if (bsmArgs != null) {
                        for (int ci = 1; ci < bsmArgs.length; ci++) cn1Cap += String.valueOf(bsmArgs[ci]).length();
                    }
                }
                for (Type at : invokedType.getArgumentTypes()) {
                    switch (at.getSort()) {
                        case Type.LONG: cn1Cap += 20; break;
                        case Type.DOUBLE: cn1Cap += 24; break;
                        case Type.FLOAT: cn1Cap += 15; break;
                        case Type.BOOLEAN: cn1Cap += 5; break;
                        case Type.CHAR: cn1Cap += 1; break;
                        case Type.OBJECT: case Type.ARRAY: cn1Cap += 16; break;
                        default: cn1Cap += 11; break; // int/short/byte
                    }
                }
                if (cn1Cap < 16) cn1Cap = 16; else if (cn1Cap > 8192) cn1Cap = 8192;
                helper.addTypeInstruction(Opcodes.NEW, "java/lang/StringBuilder");
                helper.addInstruction(Opcodes.DUP);
                helper.addInstruction(Opcodes.SIPUSH, cn1Cap);
                helper.addInvoke(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(I)V", false);

                Type[] argTypes = invokedType.getArgumentTypes();
                int maxLocal = 0;
                for (Type t : argTypes) {
                    maxLocal += t.getSize();
                }

                int localIndex = 0;
                int argIndex = 0;
                if ("makeConcat".equals(bsm.getName())) {
                    for (Type argType : argTypes) {
                        appendConcatArgument(helper, argType, localIndex);
                        localIndex += argType.getSize();
                    }
                } else {
                    String recipe = bsmArgs != null && bsmArgs.length > 0 ? String.valueOf(bsmArgs[0]) : "";
                    List<String> constants = new ArrayList<>();
                    if (bsmArgs != null) {
                        for (int i = 1; i < bsmArgs.length; i++) {
                            constants.add(String.valueOf(bsmArgs[i]));
                        }
                    }
                    int constantIndex = 0;
                    StringBuilder literal = new StringBuilder();
                    for (int i = 0; i < recipe.length(); i++) {
                        char ch = recipe.charAt(i);
                        if (ch == '\u0001') {
                            appendConcatLiteral(helper, literal);
                            literal.setLength(0);
                            if (argIndex < argTypes.length) {
                                Type argType = argTypes[argIndex++];
                                appendConcatArgument(helper, argType, localIndex);
                                localIndex += argType.getSize();
                            }
                        } else if (ch == '\u0002') {
                            appendConcatLiteral(helper, literal);
                            literal.setLength(0);
                            if (constantIndex < constants.size()) {
                                appendConcatLiteral(helper, constants.get(constantIndex++));
                            }
                        } else {
                            literal.append(ch);
                        }
                    }
                    appendConcatLiteral(helper, literal);
                    while (argIndex < argTypes.length) {
                        Type argType = argTypes[argIndex++];
                        appendConcatArgument(helper, argType, localIndex);
                        localIndex += argType.getSize();
                    }
                }

                helper.addInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                helper.addInstruction(Opcodes.ARETURN);
                helper.setMaxes(16, maxLocal + 2);

                mtd.addInvoke(Opcodes.INVOKESTATIC, clsName, helperName, desc, false);
                return;
            }

            if ("java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner()) &&
                ("metafactory".equals(bsm.getName()) || "altMetafactory".equals(bsm.getName()))) {

                // 1. Generate a unique class name for the lambda
                String lambdaClassName = clsName + "_lambda_" + (lambdaCounter++);
                indyLambdas.set(indyLambdas.size() - 1, lambdaClassName);
                sawLambdaIndy = true;

                // 2. Create the ByteCodeClass for the lambda
                ByteCodeClass lambdaClass = new ByteCodeClass(lambdaClassName, lambdaClassName.replace('_', '/'));
                lambdaClass.setBaseClass("java/lang/Object");

                // The interface implemented is the return type of the invokedynamic descriptor
                Type invokedType = Type.getMethodType(desc);
                Type interfaceType = invokedType.getReturnType();
                lambdaClass.setBaseInterfaces(new String[]{interfaceType.getInternalName()});

                // 3. Add fields for captured arguments
                Type[] capturedArgs = invokedType.getArgumentTypes();
                for (int i = 0; i < capturedArgs.length; i++) {
                    String fieldName = "arg$" + (i + 1);
                    String fieldDesc = capturedArgs[i].getDescriptor();
                    ByteCodeField field = new ByteCodeField(lambdaClassName, Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, fieldDesc, null, null);
                    lambdaClass.addField(field);
                }

                // 4. Add Constructor
                StringBuilder ctorDesc = new StringBuilder("(");
                for (Type t : capturedArgs) {
                    ctorDesc.append(t.getDescriptor());
                }
                ctorDesc.append(")V");

                BytecodeMethod ctor = new BytecodeMethod(lambdaClassName, Opcodes.ACC_PUBLIC, "<init>", ctorDesc.toString(), null, null);
                lambdaClass.addMethod(ctor);

                // Constructor body (we need to generate instructions manually)
                // ALOAD 0
                // INVOKESPECIAL java/lang/Object.<init>
                // ... assign fields ...
                // RETURN

                // NOTE: do NOT also emit `addInstruction(Opcodes.ALOAD)` here.
                // `addVariableOperation(Opcodes.ALOAD, 0)` is the canonical aload_0.
                // Emitting both produces a BasicInstruction with opcode=ALOAD and value=0
                // that the C backend (iOS) silently ignores but the JavaScript backend
                // translates as a real `push locals[0]`. The extra push corrupts the
                // operand stack simulation: invokespecial/putfield/invokevirtual then
                // pop from the wrong positions, producing method calls with the wrong
                // target and shifted arguments (e.g. lambda `run()` ending up invoking
                // its captured method on the captured Form rather than on the
                // enclosing `this`, surfacing as VIRTUAL_FAIL missing_interface_default_method).
                ctor.addVariableOperation(Opcodes.ALOAD, 0);
                ctor.addInvoke(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

                int varIndex = 1;
                for (int i = 0; i < capturedArgs.length; i++) {
                    ctor.addVariableOperation(Opcodes.ALOAD, 0); // this

                    Type t = capturedArgs[i];
                    int opcode = t.getOpcode(Opcodes.ILOAD); // correct load opcode for type
                    ctor.addVariableOperation(opcode, varIndex);
                    varIndex += t.getSize();

                    String fieldName = "arg$" + (i + 1);
                    ctor.addField(lambdaClass, Opcodes.PUTFIELD, lambdaClassName, fieldName, t.getDescriptor());
                }
                ctor.addInstruction(Opcodes.RETURN);
                ctor.setMaxes(varIndex + 1, varIndex); // Approximate maxes


                // 5. Implement the interface method
                Type samMethodType = (Type) bsmArgs[0];
                Handle implMethod = (Handle) bsmArgs[1];

                // Name from invokedynamic
                String samMethodDesc = samMethodType.getDescriptor(); // Signature from BSM arg 0

                BytecodeMethod interfaceMethod = new BytecodeMethod(lambdaClassName, Opcodes.ACC_PUBLIC, name, samMethodDesc, null, null);
                lambdaClass.addMethod(interfaceMethod);

                // Method Body:
                // Load captured arguments from fields
                // Load method arguments
                // Invoke implMethod
                // Return result

                // Determine how the implementation method is invoked up front:
                // we need to know whether it has an implicit receiver (an
                // instance call consumes the first captured arg as `this`)
                // before we can line captured/SAM args up against the target
                // parameter types for adaptation.
                boolean isCtorRef = (implMethod.getTag() == Opcodes.H_NEWINVOKESPECIAL);
                int invokeOpcode;
                switch (implMethod.getTag()) {
                    case Opcodes.H_INVOKESTATIC: invokeOpcode = Opcodes.INVOKESTATIC; break;
                    case Opcodes.H_INVOKEVIRTUAL: invokeOpcode = Opcodes.INVOKEVIRTUAL; break;
                    case Opcodes.H_INVOKEINTERFACE: invokeOpcode = Opcodes.INVOKEINTERFACE; break;
                    case Opcodes.H_INVOKESPECIAL:
                    case Opcodes.H_NEWINVOKESPECIAL:
                        invokeOpcode = Opcodes.INVOKESPECIAL; break;
                    default:
                        invokeOpcode = Opcodes.INVOKESTATIC;
                        break;// Fallback
                }
                boolean instanceCall = !isCtorRef &&
                        (invokeOpcode == Opcodes.INVOKEVIRTUAL ||
                         invokeOpcode == Opcodes.INVOKEINTERFACE ||
                         invokeOpcode == Opcodes.INVOKESPECIAL);

                // The values the implementation invocation consumes, in order:
                // the receiver (for instance calls) followed by the declared
                // parameter types. LambdaMetafactory adapts each captured/SAM
                // argument to the matching target type (box, unbox, widen or
                // cast) -- e.g. a SAM that hands us a Double bound to a method
                // taking a primitive double must unbox via doubleValue(). We
                // must replicate that here; loading the SAM args verbatim and
                // invoking the impl method directly emits a C call that passes a
                // JAVA_OBJECT where a primitive (or vice versa) is expected and
                // fails to compile in the generated Xcode project.
                Type[] implArgTypes = Type.getArgumentTypes(implMethod.getDesc());
                Type[] targetTypes = new Type[(instanceCall ? 1 : 0) + implArgTypes.length];
                int tIdx = 0;
                if (instanceCall) {
                    targetTypes[tIdx++] = Type.getObjectType(implMethod.getOwner());
                }
                for (Type t : implArgTypes) {
                    targetTypes[tIdx++] = t;
                }
                Type[] samArgs = samMethodType.getArgumentTypes();
                // Defensive: only adapt when our model of the consumed values
                // matches the values we push (captured + SAM args). If it does
                // not we fall back to the verbatim load/invoke below.
                boolean adapt = targetTypes.length == capturedArgs.length + samArgs.length;

                // Handle Constructor Reference (special case)
                if (isCtorRef) {
                    interfaceMethod.addTypeInstruction(Opcodes.NEW, implMethod.getOwner());
                    interfaceMethod.addInstruction(Opcodes.DUP);
                }

                // Load captured args
                // Same caveat as the constructor: do not also emit addInstruction(Opcodes.ALOAD).
                int targetIndex = 0;
                for (int i = 0; i < capturedArgs.length; i++) {
                    interfaceMethod.addVariableOperation(Opcodes.ALOAD, 0);
                    String fieldName = "arg$" + (i + 1);
                    interfaceMethod.addField(lambdaClass, Opcodes.GETFIELD, lambdaClassName, fieldName, capturedArgs[i].getDescriptor());
                    if (adapt) {
                        adaptLambdaType(interfaceMethod, capturedArgs[i], targetTypes[targetIndex]);
                    }
                    targetIndex++;
                }

                // Load method args
                int localIndex = 1;
                for (Type t : samArgs) {
                    interfaceMethod.addVariableOperation(t.getOpcode(Opcodes.ILOAD), localIndex);
                    localIndex += t.getSize();
                    if (adapt) {
                        adaptLambdaType(interfaceMethod, t, targetTypes[targetIndex]);
                    }
                    targetIndex++;
                }

                // Invoke implMethod
                if (isCtorRef) {
                    interfaceMethod.addInvoke(Opcodes.INVOKESPECIAL, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), false);
                } else {
                    interfaceMethod.addInvoke(invokeOpcode, implMethod.getOwner(), implMethod.getName(), implMethod.getDesc(), implMethod.isInterface());
                }

                // Adapt the value left on the stack by the implementation method
                // to the SAM's return type (unbox/box/widen/cast, or pop for a
                // void SAM), then return per the SAM descriptor.
                Type returnType = samMethodType.getReturnType();
                Type implReturnType = isCtorRef ? Type.getObjectType(implMethod.getOwner()) : Type.getReturnType(implMethod.getDesc());
                adaptLambdaType(interfaceMethod, implReturnType, returnType);
                interfaceMethod.addInstruction(returnType.getOpcode(Opcodes.IRETURN));
                interfaceMethod.setMaxes(20, 20); // Approximation


                // 6. Add static factory method
                String factoryMethodName = "lambda$factory";
                // The desc of invokedynamic is (CapturedArgs)Interface.

                // We want factory to be (CapturedArgs)LambdaClass (to match NEW output but wrapped)
                // Actually, replacing invokedynamic with INVOKESTATIC means the return type on stack should match
                // what invokedynamic promised, which is the Interface.
                // Our factory returns LambdaClass, which implements Interface. So it's assignment compatible.
                // However, the method signature in C needs to return an object pointer anyway.

                // Let's make the factory return the class type explicitly in signature
                String factoryRetType = "L" + lambdaClassName + ";";
                String actualFactoryDesc = desc.substring(0, desc.lastIndexOf(')') + 1) + factoryRetType;

                BytecodeMethod factory = new BytecodeMethod(lambdaClassName, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, factoryMethodName, actualFactoryDesc, null, null);
                lambdaClass.addMethod(factory);

                if (capturedArgs.length == 0) {
                    // A LAMBDA THAT CAPTURES NOTHING HAS NO DISTINGUISHABLE INSTANCES, so
                    // it is allocated ONCE instead of on every evaluation.
                    //
                    // The class has no instance fields at all in this case -- the fields
                    // above are the captures -- so two instances differ in nothing a
                    // program can observe: not state, not behaviour. Identity is the only
                    // thing, and the JLS explicitly declines to guarantee it here (a
                    // lambda expression need not produce a new object; the JDK's own
                    // LambdaMetafactory caches non-capturing instances for exactly this
                    // reason). So the factory hands back a shared instance.
                    //
                    // Measured on the self-hosting corpus: 4 of 6 lambda classes capture
                    // nothing. This costs no analysis -- "declares no instance fields" is
                    // known here, at the point the class is synthesised -- and it is the
                    // only allocation removal in the collections plan that needs neither
                    // an escape analysis nor a receiver type.
                    //
                    // Lazily, and deliberately WITHOUT a lock. A race can construct two
                    // instances and publish one; the loser is garbage and nothing can tell
                    // which won, because the instances are indistinguishable. The field is
                    // written with PUTSTATIC, whose generated setter carries the write
                    // barrier, so the publish is safe for the collector.
                    ByteCodeField cache = new ByteCodeField(lambdaClassName,
                            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "cn1Instance",
                            factoryRetType, null, null);
                    lambdaClass.addField(cache);

                    Label haveIt = new Label();
                    factory.addField(lambdaClass, Opcodes.GETSTATIC, lambdaClassName, "cn1Instance", factoryRetType);
                    factory.addInstruction(Opcodes.DUP);
                    factory.addJump(Opcodes.IFNONNULL, haveIt);
                    factory.addInstruction(Opcodes.POP);
                    factory.addTypeInstruction(Opcodes.NEW, lambdaClassName);
                    factory.addInstruction(Opcodes.DUP);
                    factory.addInvoke(Opcodes.INVOKESPECIAL, lambdaClassName, "<init>", ctorDesc.toString(), false);
                    factory.addInstruction(Opcodes.DUP);
                    factory.addField(lambdaClass, Opcodes.PUTSTATIC, lambdaClassName, "cn1Instance", factoryRetType);
                    factory.addLabel(haveIt);
                    factory.addInstruction(Opcodes.ARETURN);
                    factory.setMaxes(4, 0);
                } else {
                    factory.addTypeInstruction(Opcodes.NEW, lambdaClassName);
                    factory.addInstruction(Opcodes.DUP);

                    // Load factory arguments (captured args)
                    localIndex = 0; // Static method
                    for (Type t : capturedArgs) {
                        factory.addVariableOperation(t.getOpcode(Opcodes.ILOAD), localIndex);
                        localIndex += t.getSize();
                    }

                    factory.addInvoke(Opcodes.INVOKESPECIAL, lambdaClassName, "<init>", ctorDesc.toString(), false);
                    factory.addInstruction(Opcodes.ARETURN);
                    factory.setMaxes(localIndex + 2, localIndex);
                }

                // 7. Register the new class
                classes.add(lambdaClass);

                // 8. Replace invokedynamic with INVOKESTATIC to factory
                mtd.addInvoke(Opcodes.INVOKESTATIC, lambdaClassName, factoryMethodName, actualFactoryDesc, false);
                ((com.codename1.tools.translator.bytecodes.Invoke)mtd.getInstructions().get(mtd.getInstructions().size() - 1)).setLambdaSam(interfaceMethod);

                return;
            }

        }

        /**
         * Emits the box / unbox / widen / cast instructions LambdaMetafactory
         * inserts when adapting a value of type {@code from} to type {@code to}
         * (e.g. when a method reference's parameter or return type differs from
         * the functional interface's). A no-op when the types already match.
         */
        private void adaptLambdaType(BytecodeMethod m, Type from, Type to) {
            if (from.equals(to)) {
                return;
            }
            int toSort = to.getSort();
            int fromSort = from.getSort();
            if (toSort == Type.VOID) {
                if (fromSort != Type.VOID) {
                    m.addInstruction(from.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
                }
                return;
            }
            if (fromSort == Type.VOID) {
                return; // nothing on the stack to adapt
            }
            boolean toRef = toSort == Type.OBJECT || toSort == Type.ARRAY;
            boolean fromRef = fromSort == Type.OBJECT || fromSort == Type.ARRAY;

            if (!toRef && fromRef) {
                // unbox: cast to the wrapper (when the source is Object/Number
                // rather than the exact wrapper) then invoke its xxxValue()
                String wrapper = wrapperType(toSort);
                if (wrapper == null) {
                    return;
                }
                if (!wrapper.equals(from.getInternalName())) {
                    m.addTypeInstruction(Opcodes.CHECKCAST, wrapper);
                }
                m.addInvoke(Opcodes.INVOKEVIRTUAL, wrapper, unboxMethod(toSort), "()" + to.getDescriptor(), false);
                return;
            }
            if (toRef && !fromRef) {
                // box via Wrapper.valueOf(primitive); the boxed wrapper is
                // assignment compatible with the reference target by contract
                String wrapper = wrapperType(fromSort);
                if (wrapper == null) {
                    return;
                }
                m.addInvoke(Opcodes.INVOKESTATIC, wrapper, "valueOf", "(" + from.getDescriptor() + ")L" + wrapper + ";", false);
                return;
            }
            if (!toRef && !fromRef) {
                emitPrimitiveConversion(m, from, to);
                return;
            }
            // both references: narrow with a checkcast (generic erasure)
            if (!"java/lang/Object".equals(to.getInternalName())) {
                m.addTypeInstruction(Opcodes.CHECKCAST, to.getInternalName());
            }
        }

        private void emitPrimitiveConversion(BytecodeMethod m, Type from, Type to) {
            int t = to.getSort();
            int f = from.getSort();
            // byte/short/char/boolean live as int on the operand stack
            int fc = (f == Type.BOOLEAN || f == Type.BYTE || f == Type.SHORT || f == Type.CHAR) ? Type.INT : f;
            switch (t) {
                case Type.LONG:
                    if (fc == Type.INT) m.addInstruction(Opcodes.I2L);
                    else if (fc == Type.FLOAT) m.addInstruction(Opcodes.F2L);
                    else if (fc == Type.DOUBLE) m.addInstruction(Opcodes.D2L);
                    return;
                case Type.FLOAT:
                    if (fc == Type.INT) m.addInstruction(Opcodes.I2F);
                    else if (fc == Type.LONG) m.addInstruction(Opcodes.L2F);
                    else if (fc == Type.DOUBLE) m.addInstruction(Opcodes.D2F);
                    return;
                case Type.DOUBLE:
                    if (fc == Type.INT) m.addInstruction(Opcodes.I2D);
                    else if (fc == Type.LONG) m.addInstruction(Opcodes.L2D);
                    else if (fc == Type.FLOAT) m.addInstruction(Opcodes.F2D);
                    return;
                case Type.INT:
                case Type.BYTE:
                case Type.SHORT:
                case Type.CHAR:
                case Type.BOOLEAN:
                    // bring the source down to int first, then narrow if needed
                    if (fc == Type.LONG) m.addInstruction(Opcodes.L2I);
                    else if (fc == Type.FLOAT) m.addInstruction(Opcodes.F2I);
                    else if (fc == Type.DOUBLE) m.addInstruction(Opcodes.D2I);
                    if (t == Type.BYTE) m.addInstruction(Opcodes.I2B);
                    else if (t == Type.SHORT) m.addInstruction(Opcodes.I2S);
                    else if (t == Type.CHAR) m.addInstruction(Opcodes.I2C);
                    return;
                default:
                    return;
            }
        }

        private String wrapperType(int sort) {
            switch (sort) {
                case Type.BOOLEAN: return "java/lang/Boolean";
                case Type.BYTE: return "java/lang/Byte";
                case Type.CHAR: return "java/lang/Character";
                case Type.SHORT: return "java/lang/Short";
                case Type.INT: return "java/lang/Integer";
                case Type.LONG: return "java/lang/Long";
                case Type.FLOAT: return "java/lang/Float";
                case Type.DOUBLE: return "java/lang/Double";
                default: return null;
            }
        }

        private String unboxMethod(int sort) {
            switch (sort) {
                case Type.BOOLEAN: return "booleanValue";
                case Type.BYTE: return "byteValue";
                case Type.CHAR: return "charValue";
                case Type.SHORT: return "shortValue";
                case Type.INT: return "intValue";
                case Type.LONG: return "longValue";
                case Type.FLOAT: return "floatValue";
                case Type.DOUBLE: return "doubleValue";
                default: return null;
            }
        }

        private void appendConcatLiteral(BytecodeMethod targetMethod, CharSequence literal) {
            if (literal == null || literal.length() == 0) {
                return;
            }
            targetMethod.addLdc(literal.toString());
            targetMethod.addInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        }

        private void appendConcatArgument(BytecodeMethod targetMethod, Type argType, int localIndex) {
            targetMethod.addVariableOperation(argType.getOpcode(Opcodes.ILOAD), localIndex);
            String appendDesc;
            switch (argType.getSort()) {
                case Type.BOOLEAN:
                    appendDesc = "(Z)Ljava/lang/StringBuilder;";
                    break;
                case Type.CHAR:
                    appendDesc = "(C)Ljava/lang/StringBuilder;";
                    break;
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    appendDesc = "(I)Ljava/lang/StringBuilder;";
                    break;
                case Type.LONG:
                    appendDesc = "(J)Ljava/lang/StringBuilder;";
                    break;
                case Type.FLOAT:
                    appendDesc = "(F)Ljava/lang/StringBuilder;";
                    break;
                case Type.DOUBLE:
                    appendDesc = "(D)Ljava/lang/StringBuilder;";
                    break;
                case Type.OBJECT:
                    if ("java/lang/String".equals(argType.getInternalName())) {
                        appendDesc = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
                    } else {
                        appendDesc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
                    }
                    break;
                case Type.ARRAY:
                default:
                    appendDesc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
                    break;
            }
            targetMethod.addInvoke(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", appendDesc, false);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            mtd.addInvoke(opcode, owner, name, desc, itf);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (dupAnalysisNode != null && (LocalReceiverTypes.isCandidate(opcode, owner, name, desc)
                    || sawLambdaIndy && LocalReceiverTypes.isLambdaCandidate(opcode))) {
                java.util.List<com.codename1.tools.translator.bytecodes.Instruction> body = mtd.getInstructions();
                flowInvokes.put(dupAnalysisNode.instructions.getLast(),
                        (com.codename1.tools.translator.bytecodes.Invoke) body.get(body.size() - 1));
            }
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            mtd.addField(cls, opcode, owner, name, desc);
            super.visitFieldInsn(opcode, owner, name, desc); 
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            mtd.addTypeInstruction(opcode, type);
            super.visitTypeInsn(opcode, type); 
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            mtd.addVariableOperation(opcode, var);
            super.visitVarInsn(opcode, var); 
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            mtd.addVariableOperation(opcode, operand);
            super.visitIntInsn(opcode, operand); 
        }

        @Override
        public void visitInsn(int opcode) {
            mtd.addInstruction(opcode);
            super.visitInsn(opcode); 
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            super.visitFrame(type, nLocal, local, nStack, stack); 
        }

        @Override
        public void visitCode() {
            super.visitCode(); 
        }

        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr); 
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitParameterAnnotation(parameter, desc, visible));
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitTypeAnnotation(typeRef, typePath, desc, visible));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if ("Lcom/codename1/html5/js/JSBody;".equals(desc) || "Lorg/teavm/jso/JSBody;".equals(desc)) {
                return new JSBodyAnnotationVisitor(mtd);
            }
            if (DISABLE_DEBUG_INFO_ANNOTATION.equals(desc)) {
                mtd.setDisableDebugInfo(true);
            } else if (DISABLE_NULL_AND_ARRAY_BOUNDS_CHECKS_ANNOTATION.equals(desc)) {
                mtd.setDisableNullAndArrayBoundsChecks(true);
            }
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitAnnotation(desc, visible));
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            if (mv == null) return null;
            return new AnnotationVisitorWrapper(super.visitAnnotationDefault());
        }

        @Override
        public void visitParameter(String name, int access) {
            super.visitParameter(name, access); 
        }    
        
        
    }
    
    static class FieldVisitorWrapper extends FieldVisitor {

        public FieldVisitorWrapper(FieldVisitor fv) {
            super(Opcodes.ASM9, fv);
        }

        @Override
        public void visitEnd() {
            super.visitEnd(); 
        }

        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr); 
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return super.visitTypeAnnotation(typeRef, typePath, desc, visible); 
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return super.visitAnnotation(desc, visible); 
        }
        
    }
    
    static class AnnotationVisitorWrapper extends AnnotationVisitor {

        public AnnotationVisitorWrapper(AnnotationVisitor av) {
            super(Opcodes.ASM9, av);
        }

        @Override
        public void visitEnd() {
            super.visitEnd(); 
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if (av == null) return null;
            return super.visitArray(name); 
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            if (av == null) return null;
            return super.visitAnnotation(name, desc); 
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            super.visitEnum(name, desc, value); 
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value); 
        }
        
        
    
    }

    static class JSBodyAnnotationVisitor extends AnnotationVisitor {
        private final BytecodeMethod method;
        private String script;
        private java.util.List<String> params = new java.util.ArrayList<>();

        public JSBodyAnnotationVisitor(BytecodeMethod method) {
            super(Opcodes.ASM9);
            this.method = method;
        }

        @Override
        public void visit(String name, Object value) {
            if ("script".equals(name)) {
                script = (String) value;
            }
            super.visit(name, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if ("params".equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        params.add((String) value);
                    }
                };
            }
            return super.visitArray(name);
        }

        @Override
        public void visitEnd() {
            if (script != null) {
                method.setJsBodyScript(script);
                method.setJsBodyParams(params.toArray(new String[0]));
            }
            super.visitEnd();
        }
    }

    /// -Dcn1.iteratorCensus=true: report which Iterator implementations could live in the
    /// caller's stack frame, and why the rest could not.
    ///
    /// Round 23 established that the iterator universe is closed and ours -- 33 of 34
    /// implementations are java.util classes, every one of them a parent reference plus a
    /// handful of primitives. That makes a uniform stack buffer possible, but only for
    /// classes whose instances provably cannot outlive the loop. Three conditions have to
    /// hold together, and this census measures all three:
    ///
    ///   1. `this` does not escape any method of the iterator class, its constructor
    ///      included -- a ctor that registers itself with its parent is the obvious way an
    ///      iterator outlives the frame;
    ///   2. at every site that allocates one, the new object does not escape that method
    ///      except by being returned -- which covers an iterator() that caches what it
    ///      hands out;
    ///   3. the for-each site's own slot does not escape, which the indexed lowering
    ///      already proves separately.
    ///
    /// Printed rather than assumed because the mechanism is only as sound as this set, and
    /// a class silently dropping out of it is a performance regression that no gate would
    /// otherwise report.
    /// Classes whose instances may live in the caller's stack frame, by mangled name.
    /// COMPUTED, never listed: a hand-written set goes stale the moment someone edits an
    /// iterator, and the failure would be a dangling pointer rather than a compile error.
    private static final java.util.Set<String> stackIterClasses = new java.util.HashSet<String>();

    public static boolean isStackIterator(String mangledClsName) {
        return stackIterClasses.contains(mangledClsName);
    }

    /// How many allocation sites in this corpus are provably frame-local?
    ///
    /// The escape analysis behind the iterator scheme is not iterator-specific -- it answers
    /// "does this reference escape the frame that produced it" for any type. It is only ever
    /// ASKED about iterators. This census asks it about every NEW in the program, so the size
    /// of the unexploited population is a measured number rather than an assumption.
    ///
    /// Two strictnesses, because they enable different things:
    ///   LOOSE  (returnIsLeak=false) -- may be returned; suits caller-frame allocation, the
    ///          shape the iterator work already ships.
    ///   STRICT (returnIsLeak=true)  -- dies with the allocating method; the population a
    ///          wholesale page free at method exit could reclaim without the collector.
    ///
    /// -Dcn1.allocCensus=true. Measurement only: nothing reads the result yet.
    /// Do all of these callees keep `this` to themselves? The precise form of the
    /// receiver check: only the methods a site ACTUALLY invokes on the tracked object
    /// matter, not every method the class happens to declare.
    /// Which callees are answering "unsafe", and how often. A resolution bug here is
    /// indistinguishable from real leakage in the totals, and the first cut of this
    /// check took the safe count to ZERO -- which is what an unresolvable callee looks
    /// like, not what a leaky program looks like.
    static final Map<String, int[]> calleeFailures = new HashMap<String, int[]>();

    static boolean calleesKeepThis(List<String> calls, Map<String, Boolean> memo) {
        for (int i = 0; i < calls.size(); i++) {
            String key = calls.get(i);
            Boolean cached = memo.get(key);
            if (cached == null) {
                cached = Boolean.valueOf(calleeIsSafe(key));
                memo.put(key, cached);
            }
            if (!cached.booleanValue()) {
                int[] fc = calleeFailures.get(key);
                if (fc == null) { fc = new int[1]; calleeFailures.put(key, fc); }
                fc[0]++;
                return false;
            }
        }
        return true;
    }

    /// owner.name+desc -> does that one method keep `this`? A callee that cannot be
    /// resolved in this closed world answers NO: an unknown body is an unchecked body.
    static boolean calleeIsSafe(String key) {
        int dot = key.indexOf('.');
        if (dot < 0) {
            return false;
        }
        String owner = IteratorEscape.mangle(key.substring(0, dot));
        String rest = key.substring(dot + 1);
        int paren = rest.indexOf('(');
        if (paren < 0) {
            return false;
        }
        String name = rest.substring(0, paren);
        // BytecodeMethod renames <init> to __INIT__ (see BytecodeMethod:902). Without
        // this the constructor never resolves, and since EVERY new is followed by
        // INVOKESPECIAL <init> on the receiver, every site answers "unsafe" -- which is
        // exactly how the first cut of this check reported ZERO retirable sites.
        if ("<init>".equals(name)) {
            name = "__INIT__";
        }
        String desc = rest.substring(paren);
        for (ByteCodeClass c : classes) {
            if (!IteratorEscape.mangle(c.getClsName()).equals(owner)) {
                continue;
            }
            for (BytecodeMethod m : c.getMethods()) {
                if (m.getMethodName().equals(name) && desc.equals(m.getDesc())) {
                    return IteratorEscape.thisEscapes(m) == IteratorEscape.SAFE;
                }
            }
            return false;   // class found, method not -- inherited or synthetic
        }
        return false;       // not in this closed world
    }

    static void reportRetireCounters() {
        System.out.println("[RETIRE] seen=" + BytecodeMethod.retireSeen
                + " kept=" + BytecodeMethod.retireKept
                + " dropStackAlloc=" + BytecodeMethod.retireDropStack
                + " dropEscapes=" + BytecodeMethod.retireDropEscape
                + " dropNoLocal=" + BytecodeMethod.retireDropNoLocal
                + " dropCallee=" + BytecodeMethod.retireDropCallee
                + " dropFrameless=" + BytecodeMethod.retireDropFrameless);
    }

    static void allocationEscapeCensus() {
        if (!"true".equals(System.getProperty("cn1.allocCensus"))) {
            return;
        }
        Map<String, List<BytecodeMethod>> allocations = new HashMap<String, List<BytecodeMethod>>();
        for (ByteCodeClass c : classes) {
            allocations.put(IteratorEscape.mangle(c.getClsName()), new ArrayList<BytecodeMethod>());
        }
        int sites = 0, loose = 0, strict = 0, escapes = 0, unknown = 0;
        int unsoundReceiver = 0;
        Map<String, Boolean> thisSafe = new HashMap<String, Boolean>();
        Map<String, int[]> byType = new HashMap<String, int[]>();
        // WHY each undecidable site was refused. There are several UNKNOWN exits, not
        // just the documented branch-liveness one, and they need different fixes: a CFG
        // fixpoint for the branch case, more modelled opcodes for "consumed by something
        // unmodelled". Without this split the 197 is a number nobody can act on.
        Map<String, int[]> reasons = new HashMap<String, int[]>();
        for (ByteCodeClass c : classes) {
            for (BytecodeMethod m : c.getMethods()) {
                String lastSeen = null;
                for (Instruction instruction : m.getInstructions()) {
                    if (!(instruction instanceof TypeInstruction)
                            || instruction.getOpcode() != Opcodes.NEW) {
                        continue;
                    }
                    String owner = IteratorEscape.mangle(((TypeInstruction) instruction).getTypeName());
                    if (!allocations.containsKey(owner)) {
                        continue;   // type not in this closed world
                    }
                    // One method is one analysis site per allocated type, matching
                    // iteratorStackCensus: re-allocating the same type in a loop is
                    // still a single decision.
                    if (owner.equals(lastSeen)) {
                        continue;
                    }
                    lastSeen = owner;
                    sites++;
                    int rLoose = IteratorEscape.newEscapes(m, owner);
                    int rStrict = IteratorEscape.newEscapesStrict(m, owner);
                    // SOUNDNESS: newEscapesStrict permits the tracked value to be a
                    // RECEIVER, because the walk reasons that a receiver is only `this`
                    // inside the callee "and that callee is checked on its own". For
                    // iterators that check exists (iteratorStackCensus runs thisEscapes
                    // over every method of the class). For an arbitrary type it does not,
                    // so a callee that stashes `this` -- list.register(this) -- would be
                    // missed. Counting a site as retirable requires BOTH properties.
                    if (rStrict == IteratorEscape.SAFE) {
                        List<String> calls = new ArrayList<String>(IteratorEscape.receiverCalls);
                        if (!calleesKeepThis(calls, thisSafe)) {
                            rStrict = IteratorEscape.ESCAPES;
                            unsoundReceiver++;
                        }
                    }
                    if (rLoose == IteratorEscape.SAFE) { loose++; }
                    if (rStrict == IteratorEscape.SAFE) { strict++; }
                    else if (rStrict == IteratorEscape.ESCAPES) { escapes++; }
                    else { unknown++; }
                    if (rStrict == IteratorEscape.UNKNOWN) {
                        String why = IteratorEscape.lastReason;
                        if (why == null || why.length() == 0) { why = "(unset)"; }
                        int[] rc = reasons.get(why);
                        if (rc == null) { rc = new int[1]; reasons.put(why, rc); }
                        rc[0]++;
                    }
                    int[] t = byType.get(owner);
                    if (t == null) { t = new int[3]; byType.put(owner, t); }
                    t[0]++;
                    if (rStrict == IteratorEscape.SAFE) { t[1]++; }
                    else if (rStrict == IteratorEscape.UNKNOWN) { t[2]++; }
                }
            }
        }
        List<String> fk = new ArrayList<String>(calleeFailures.keySet());
        Collections.sort(fk, new Comparator<String>() {
            public int compare(String a, String b) {
                return calleeFailures.get(b)[0] - calleeFailures.get(a)[0];
            }
        });
        int fshown = 0;
        for (String k : fk) {
            if (fshown++ >= 20) { break; }
            System.out.println("[ALLOC-CALLEE-FAIL] " + calleeFailures.get(k)[0] + "  " + k);
        }
        System.out.println("[ALLOC-CENSUS] receiverUnsound=" + unsoundReceiver
                + "  (strict-safe by the walk, but the class can leak `this` from a callee)");
        System.out.println("[ALLOC-CENSUS] sites=" + sites
                + " frameLocalLoose=" + loose + " frameLocalStrict=" + strict
                + " escapes=" + escapes + " unknown=" + unknown);
        List<String> keys = new ArrayList<String>(byType.keySet());
        Collections.sort(keys);
        int shown = 0;
        for (String k : keys) {
            int[] t = byType.get(k);
            if (t[1] > 0 && shown < 60) {
                System.out.println("[ALLOC-CENSUS]   " + k + " sites=" + t[0] + " strictSafe=" + t[1]);
                shown++;
            }
        }
        // UNKNOWN is the interesting column: those sites do not escape, the analysis
        // simply refuses to decide because the tracked value is live across a branch
        // (it has no CFG fixpoint). Ranked by count so the question "are the
        // undecidable ones the HOT types, or cold leaf nodes?" has an answer.
        List<String> rk = new ArrayList<String>(reasons.keySet());
        Collections.sort(rk, new Comparator<String>() {
            public int compare(String a, String b) {
                return reasons.get(b)[0] - reasons.get(a)[0];
            }
        });
        for (String r : rk) {
            System.out.println("[ALLOC-REASON]  " + reasons.get(r)[0] + "  " + r);
        }
        List<String> unk = new ArrayList<String>();
        for (String k : keys) {
            if (byType.get(k)[2] > 0) { unk.add(k); }
        }
        Collections.sort(unk, new Comparator<String>() {
            public int compare(String a, String b) {
                return byType.get(b)[2] - byType.get(a)[2];
            }
        });
        int un = 0;
        for (String k : unk) {
            if (un++ >= 25) { break; }
            int[] t = byType.get(k);
            System.out.println("[ALLOC-UNKNOWN]  " + k + " sites=" + t[0] + " undecidable=" + t[2]);
        }
    }

    static void iteratorStackCensus() {
        boolean verbose = "true".equals(System.getProperty("cn1.iteratorCensus"));
        int total = 0, safeClasses = 0, leakThis = 0, unknownThis = 0;
        int sites = 0, safeSites = 0, leakSites = 0, unknownSites = 0;
        StringBuilder rejected = new StringBuilder();
        StringBuilder accepted = new StringBuilder();
        // Index allocating methods once. Scanning every instruction separately for
        // every iterator implementation makes this pass proportional to their
        // product, even though each NEW names exactly one class.
        Map<String, List<BytecodeMethod>> allocations = new HashMap<String, List<BytecodeMethod>>();
        for (ByteCodeClass c : classes) {
            if (implementsIterator(c)) {
                allocations.put(IteratorEscape.mangle(c.getClsName()), new ArrayList<BytecodeMethod>());
            }
        }
        for (ByteCodeClass c : classes) {
            for (BytecodeMethod m : c.getMethods()) {
                for (Instruction instruction : m.getInstructions()) {
                    if (instruction instanceof TypeInstruction && instruction.getOpcode() == Opcodes.NEW) {
                        List<BytecodeMethod> methods = allocations.get(IteratorEscape.mangle(
                                ((TypeInstruction) instruction).getTypeName()));
                        // A method is one analysis site even if it allocates the
                        // same iterator more than once. Methods are visited contiguously.
                        if (methods != null && (methods.isEmpty() || methods.get(methods.size() - 1) != m)) {
                            methods.add(m);
                        }
                    }
                }
            }
        }
        for (ByteCodeClass c : classes) {
            if (!implementsIterator(c)) {
                continue;
            }
            total++;
            int worst = IteratorEscape.SAFE;
            String why = null;
            String reason = "";
            for (BytecodeMethod m : c.getMethods()) {
                int r = IteratorEscape.thisEscapes(m);
                if (r != IteratorEscape.SAFE && worst == IteratorEscape.SAFE) {
                    worst = r;
                    why = m.getMethodName();
                    // Captured HERE: lastReason is overwritten by every later method, so
                    // reading it after the loop names the wrong instruction.
                    reason = IteratorEscape.lastReason;
                }
            }
            boolean eligible = worst == IteratorEscape.SAFE;
            if (worst == IteratorEscape.SAFE) {
                safeClasses++;
            } else if (worst == IteratorEscape.ESCAPES) {
                leakThis++;
                rejected.append("    ").append(c.getClsName()).append(" -- this escapes in ")
                        .append(why).append("\n");
            } else {
                unknownThis++;
                rejected.append("    ").append(c.getClsName()).append(" -- unanalysable ")
                        .append(why).append(": ").append(reason).append("\n");
            }
            // Every site that allocates this iterator, anywhere in the closed world.
            String mangled = IteratorEscape.mangle(c.getClsName());
            for (BytecodeMethod m : allocations.get(mangled)) {
                sites++;
                int r = IteratorEscape.newEscapes(m, mangled);
                if (r != IteratorEscape.SAFE) {
                    // ONE leaking site disqualifies the whole class. The buffer is
                    // taken by class, not by site -- __NEW_X cannot tell which caller
                    // it is serving -- so eligibility must hold everywhere the class
                    // is allocated, not merely at the sites we like.
                    eligible = false;
                }
                if (r == IteratorEscape.SAFE) {
                    safeSites++;
                    accepted.append("    OK ").append(m.getClsName()).append(".")
                            .append(m.getMethodName()).append(" -> ")
                            .append(c.getClsName()).append("\n");
                } else if (r == IteratorEscape.ESCAPES) {
                    leakSites++;
                    rejected.append("    site ").append(m.getClsName()).append(".")
                            .append(m.getMethodName()).append(" leaks a ")
                            .append(c.getClsName()).append("\n");
                } else {
                    unknownSites++;
                    rejected.append("    site ").append(m.getClsName()).append(".")
                            .append(m.getMethodName()).append(" unanalysable for ")
                            .append(c.getClsName()).append(": ")
                            .append(IteratorEscape.lastReason).append("\n");
                }
            }
            if (eligible) {
                stackIterClasses.add(mangled);
            }
        }
        if (!verbose) {
            return;
        }
        System.out.println("[ITER] Iterator implementations=" + total
                + " thisSafe=" + safeClasses + " thisEscapes=" + leakThis
                + " unanalysable=" + unknownThis);
        System.out.println("[ITER] allocation sites=" + sites
                + " safe=" + safeSites + " escaping=" + leakSites
                + " unanalysable=" + unknownSites);
        System.out.println("[ITER] stack-eligible classes=" + stackIterClasses.size());
        System.out.println("[ITER] eligible set=" + stackIterClasses);
        if (accepted.length() > 0) {
            System.out.println("[ITER] eligible allocation sites:");
            System.out.print(accepted);
        }
        if (rejected.length() > 0) {
            System.out.println("[ITER] refusals:");
            System.out.print(rejected);
        }
    }

    private static boolean implementsIterator(ByteCodeClass c) {
        for (ByteCodeClass w = c; w != null; w = w.getBaseClassObject()) {
            for (String i : w.getBaseInterfaces()) {
                String n = IteratorEscape.mangle(i);
                if ("java_util_Iterator".equals(n) || "java_util_ListIterator".equals(n)
                        || "java_util_Enumeration".equals(n)) {
                    return true;
                }
            }
        }
        return false;
    }

}
