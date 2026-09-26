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

import com.codename1.tools.translator.bytecodes.Field;
import com.codename1.tools.translator.bytecodes.IInc;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.bytecodes.Jump;
import com.codename1.tools.translator.bytecodes.LabelInstruction;
import com.codename1.tools.translator.bytecodes.Ldc;
import com.codename1.tools.translator.bytecodes.LineNumber;
import com.codename1.tools.translator.bytecodes.LocalVariable;
import com.codename1.tools.translator.bytecodes.MultiArray;
import com.codename1.tools.translator.bytecodes.SwitchInstruction;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import com.codename1.tools.translator.bytecodes.VarOp;
import com.codename1.tools.translator.bytecodes.BasicInstruction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.objectweb.asm.Opcodes;

/**
 * Parsed class file
 *
 * @author Shai Almog
 */
public class ByteCodeClass {

    /**
     * The one class whose reference field the collector owns rather than traces.
     *
     * <p>{@code java.lang.ref.Reference.objReference} is the referent of every
     * WeakReference and SoftReference in the program. Emitting the ordinary
     * {@code gcMarkObject} for it would make it a STRONG edge -- which is exactly
     * what ParparVM did until this opt-out existed, so that a "weak" reference
     * pinned its referent for the life of the process and every cache built on
     * {@code Display.createSoftWeakRef} was unbounded. The two emission sites
     * below replace that with a weak edge and the load barrier that makes reading
     * one safe while a concurrent mark is running.</p>
     *
     * <p>Matched by name rather than by any annotation because the class is part
     * of the VM's own {@code java.lang} surface: there is nowhere to hang an
     * annotation that {@code vm/JavaAPI} and {@code Ports/CLDC11} would both
     * accept, and a marker interface would be one more thing to keep in step.
     * {@code Reference} is final in practice -- its constructor is package
     * private -- so the set of classes this can apply to is closed.</p>
     */
    static final String REFERENCE_CLASS = "java_lang_ref_Reference";

    /** The referent field within {@link #REFERENCE_CLASS}. */
    static final String REFERENCE_REFERENT_FIELD = "objReference";

    /**
     * True for the one field whose GC treatment and read accessor are special
     * cased below. Both call sites must agree, hence the shared predicate:
     * suppressing the mark without adding the barrier produces a collector that
     * frees a referent a mutator is holding, and adding the barrier without
     * suppressing the mark produces a weak reference that is still strong.
     */
    /**
     * NATIVE BACKING BLOCKS: library containers whose storage is a C block rather than a
     * Java array, listed as {mangled class, block field, count field}.
     *
     * The live-set census says 88.4% of every Object[] in a real program is one of these
     * containers' backing store and 94.1% of every int[] is HashMap's slot metadata --
     * ~130MB of a 488MB live set in arrays no Java code ever sees. We are committed to
     * the Map and List APIs, not to Object[] behind them, and a C block costs no 32-byte
     * array header, no BiBOP size-class rounding and no slot for the sweep to visit.
     *
     * A block is OUTSIDE THE HEAP BUT NOT OUTSIDE THE COLLECTOR. Its elements are live
     * references, and the conservative scanner walks native STACKS rather than arbitrary
     * malloc blocks, so nothing would see them unless the owner's mark function does --
     * which is what this table drives. It is deliberately a hard-coded list of known
     * library classes, exactly like isReferenceReferent above: this is a VM-internal
     * representation choice for containers whose internals are ours, not a facility
     * application code can opt into.
     */
    /** One ownership description drives tracing, reclamation and native field handling. */
    private static final class NativeBlock {
        final String owner, field;
        final boolean references, owns;
        // For a reference PART of a table: the field holding the table's root, and the
        // part index. A part has no header of its own (see cn1TablePart in
        // cn1_globals.m), so it is walked through the root.
        final String rootField;
        final int part;
        NativeBlock(String owner, String field, boolean references) {
            this(owner, field, references, true);
        }
        NativeBlock(String owner, String field, boolean references, boolean owns) {
            this(owner, field, references, owns, null, 0);
        }
        NativeBlock(String owner, String field, boolean references, boolean owns, String rootField, int part) {
            this.owner = owner;
            this.field = field;
            this.references = references;
            this.owns = owns;
            this.rootField = rootField;
            this.part = part;
        }
    }

    private static final NativeBlock[] NATIVE_BLOCKS = {
        new NativeBlock("java_lang_StringBuilder", "cn1Storage", false),
        new NativeBlock("java_util_IdentityHashMap", "elementData", true),
        new NativeBlock("java_util_ArrayDeque", "elements", true),
        new NativeBlock("java_util_Hashtable", "cn1Keys", true),
        // The values PART of the table (see cn1TablePart); no field of its own.
        new NativeBlock("java_util_Hashtable", "cn1Keys", true, false, "cn1Keys", 1),
        new NativeBlock("java_util_ArrayList", "cn1Storage", true),
        new NativeBlock("java_util_Vector", "cn1Storage", true),
        // A plain HashSet owns its table: element references and then int slot markers
        // in ONE allocation (cn1SetTableAlloc), and no values array.
        new NativeBlock("java_util_HashSet", "cn1KeysBlock", true),
        new NativeBlock("java_util_HashMap", "cn1KeysBlock", true),
        new NativeBlock("java_util_HashMap", "cn1KeysBlock", true, false, "cn1KeysBlock", 1),
    };

    private static boolean hasNativeBlocks(String cls) {
        for (NativeBlock block : NATIVE_BLOCKS) {
            if (block.owner.equals(cls)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReferenceReferent(String owningClass, ByteCodeField fld) {
        return REFERENCE_CLASS.equals(owningClass)
                && REFERENCE_REFERENT_FIELD.equals(fld.getFieldName())
                && REFERENCE_CLASS.equals(fld.getClsName());
    }

    /**
     * @param isAnonymous the isAnonymous to set
     */
    public void setIsAnonymous(boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    /**
     * @param isSynthetic the isSynthetic to set
     */
    public void setIsSynthetic(boolean isSynthetic) {
        this.isSynthetic = isSynthetic;
    }

    /**
     * @param isAnnotation the isAnnotation to set
     */
    public void setIsAnnotation(boolean isAnnotation) {
        this.isAnnotation = isAnnotation;
    }
    private List<ByteCodeField> fullFieldList;
    private List<ByteCodeField> staticFieldList;
    private Set<String> dependsClassesInterfaces = new TreeSet<String>();
    private Set<String> exportsClassesInterfaces = new TreeSet<String>();
    private List<BytecodeMethod> methods = new ArrayList<BytecodeMethod>();
    private List<ByteCodeField> fields = new ArrayList<ByteCodeField>();
    private String clsName;
    private String originalClassName;
    private String baseClass;
    private String concreteClass;
    private List<String> baseInterfaces;
    private boolean isInterface;
    private boolean isAbstract;
    private boolean isSynthetic;
    private boolean isAnnotation;
    private boolean isAnonymous;
    private boolean eliminated;

    private static boolean saveUnitTests;
    private boolean isUnitTest;


    private static Set<String> arrayTypes = new TreeSet<String>();

    /** Whether class_array{dim}__{clsName} is emitted (a used array type of that
     *  dimension or up to two deeper needs it as a component). */
    static boolean emitsArrayClass(String clsName, int dim) {
        return arrayTypes.contains(dim + "_" + clsName) || arrayTypes.contains((dim + 1) + "_" + clsName)
                || arrayTypes.contains((dim + 2) + "_" + clsName);
    }
    
    private ByteCodeClass baseClassObject;
    private List<ByteCodeClass> baseInterfacesObject;
    
    List<BytecodeMethod> virtualMethodList;
    private String sourceFile;

    private int classOffset;
    
    private boolean marked;
    private static ByteCodeClass mainClass;
    private static String preferredMainClass;
    private boolean finalClass;
    private boolean isEnum;
    private static Set<String> writableFields = new HashSet<String>();

    static void cleanup() {
        arrayTypes.clear();
        writableFields.clear();
        mainClass = null;
        preferredMainClass = null;
        saveUnitTests = false;
        concreteTarget = null;
    }

    /// Selects which {@code @Concrete} attribute the parser honours: {@code "win"}
    /// for the native Windows build (use {@code Concrete.win()}), {@code "linux"}
    /// for the native Linux build (use {@code Concrete.linux()}), {@code "mac"}
    /// for the native macOS build (use {@code Concrete.mac()}), {@code null}/
    /// anything else for the default iOS pipeline (use {@code Concrete.name()}).
    /// Set once per translation run from the app type (see ByteCodeTranslator).
    private static String concreteTarget;

    static void setConcreteTarget(String target) {
        concreteTarget = target;
    }

    static String getConcreteTarget() {
        return concreteTarget;
    }
    
    /**
     * 
     * @param clsName Class name with mangling.  e.g. java_lang_String
     * @param originalClassName Classname without mangling.  e.g. java/lang/String
     */
    public ByteCodeClass(String clsName, String originalClassName) {
        this.clsName = clsName;
        this.originalClassName = originalClassName;
    }

    /**
     * Checks if this class has been eliminated.
     * @return
     */
    public boolean isEliminated() {
        return eliminated;
    }

    /**
     * Marks class as eliminated.  Will recursively set all class methods
     * as eliminated too.
     * @param eliminated True to set eliminated.
     * @return Number of methods that were newly marked as eliminated by this call.
     */
    public int setEliminated(boolean eliminated) {
        int nfound = 0;
        if (this.eliminated) return nfound;
        this.eliminated = eliminated;
        if (eliminated) {
            for (BytecodeMethod m : methods) {
                if (!m.isEliminated()) {
                    m.setEliminated(true);
                    nfound++;
                }
            }
        }
        return nfound;

    }

    /**
     * Restores a class definition for the JavaScript RTA pass without
     * resurrecting all of its methods.  The conservative pass can remove a
     * class after eliminating the only method that instantiates it; RTA may
     * subsequently prove that method reachable through a runtime dispatch
     * edge.  RTA then restores only the methods it actually reaches.
     */
    void restoreEliminatedClass() {
        eliminated = false;
    }
    
    /**
     * Class name in original JVM format:  e.g. java/lang/String
     * @return 
     */
    public String getOriginalClassName() {
        return originalClassName;
    }
    static ByteCodeClass getMainClass() {
		return mainClass;
    }

    static void setPreferredMainClass(String preferredMainClassName) {
        preferredMainClass = preferredMainClassName;
    }
    
    static void setSaveUnitTests(boolean save) {
        saveUnitTests = save;
    }
    
    public void addMethod(BytecodeMethod m) {
        if(m.isMain()) {
            if (preferredMainClass != null) {
                if (clsName.equals(preferredMainClass)) {
                    mainClass = this;
                }
            } else if (mainClass == null) {
                mainClass = this;
            } else {
                throw new RuntimeException("Multiple main classes: "+mainClass.clsName+" and "+this.clsName);
            }
        }
        m.setSourceFile(sourceFile);
        m.setForceVirtual(isInterface);
        methods.add(m);
    }


    public void addField(ByteCodeField m) {
        fields.add(m);
    }
    
    public String generateJavascriptCode(List<ByteCodeClass> allClasses) {
        return JavascriptMethodGenerator.generateClassJavascript(this, allClasses);
    }
    
    public void addWritableField(String field) {
        writableFields.add(field);
    }

    /**
     * Marks dependencies in this class based on the provided classes in this round of optimization.
     * @param lst The list of classes that are available in this optimization step.
     * @param nativeSources Array of native sources in this round. Used to check if native files reference
     *                      this class or methods.
     */
    public static void markDependencies(List<ByteCodeClass> lst, String[] nativeSources) {
        mainClass.markDependent(lst);
        for(ByteCodeClass bc : lst) {
            if (bc.marked) {
                continue;
            }
            if (bc.isEliminated()) {
                continue;
            }
            if(bc.clsName.equals("java_lang_Boolean")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_String")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Integer")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Byte")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Short")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Character")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Thread")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Long")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Double")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_Float")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_lang_StackOverflowError")) {
                bc.markDependent(lst);
                continue;
            }
            if(bc.clsName.equals("java_text_DateFormat")) {
                bc.markDependent(lst);
                continue;
            }
            if (bc.getUsedByNative() == UsedByNativeResult.Unknown) {
                // We don't yet know if this class is used by native
                // calculate it now.
                bc.calcUsedByNative(nativeSources);
            }
            if(bc.getUsedByNative() == UsedByNativeResult.Used){
                bc.markDependent(lst);
                continue;
            }
            if(saveUnitTests && bc.isUnitTest) {
                bc.markDependent(lst);
                continue;
            }
        }
        
        // mark all non-final classes that aren't inherited as final for use in 
        // additional optimizations
        for(ByteCodeClass bc : lst) {
            if(bc.isFinalClass() || bc.isInterface || bc.isIsAbstract()) {
                continue;
            }
            boolean found = false;
            for(ByteCodeClass bk : lst) {
                if(bk.baseClassObject == bc) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                bc.setFinalClass(true);
            }
        }
        
        // we try to disable the "virtual" aspect of methods where possible
        for(ByteCodeClass bc : lst) {
            if(bc.isFinalClass()) {
                for(BytecodeMethod meth : bc.methods) {
                    if(meth.canBeVirtual() && !bc.isMethodFromBaseOrInterface(meth)) {
                        meth.setVirtualOverriden(true);
                    }
                } 
            } 
        }        
    }
    
    
    public boolean isMethodPrivate(String name, String desc) {
        for (BytecodeMethod meth : methods) {
            if (meth.getMethodName().equals(name) && desc.equals(meth.getSignature())) {
                return meth.isPrivate();
            }
        }
        return false;
    }

    public ByteCodeClass findMethodOwner(String name, String desc) {
        return findMethodOwner(name, desc, new HashSet<ByteCodeClass>());
    }

    private ByteCodeClass findMethodOwner(String name, String desc, Set<ByteCodeClass> visited) {
        if (!visited.add(this)) {
            return null;
        }
        BytecodeMethod declaredMethod = findDeclaredMethod(name, desc);
        if (declaredMethod != null && !declaredMethod.isAbstract()) {
            return this;
        }
        if (baseClassObject != null) {
            ByteCodeClass owner = baseClassObject.findMethodOwner(name, desc, visited);
            if (owner != null) {
                return owner;
            }
        }
        if (baseInterfacesObject != null) {
            for (ByteCodeClass iface : baseInterfacesObject) {
                ByteCodeClass owner = iface.findMethodOwner(name, desc, visited);
                if (owner != null) {
                    return owner;
                }
            }
        }
        return null;
    }

    private BytecodeMethod findDeclaredMethod(String name, String desc) {
        for (BytecodeMethod meth : methods) {
            if (meth.getMethodName().equals(name) && desc.equals(meth.getSignature())) {
                return meth;
            }
        }
        return null;
    }

    /**
     * Whether this class declares the method, it is not abstract, AND the dead code
     * pass has not eliminated it -- so a direct call naming it will actually find a
     * generated function.
     *
     * hasDeclaredNonAbstractMethod is not enough on its own: elimination is per
     * METHOD, so a surviving class can have a culled method, and a call emitted to it
     * fails at the C compiler as an implicit declaration rather than degrading.
     *
     * @param name method name
     * @param desc method descriptor
     * @return true when a direct call to it can be emitted
     */
    public boolean hasLiveNonAbstractMethod(String name, String desc) {
        BytecodeMethod m = findDeclaredMethod(name, desc);
        return m != null && !m.isAbstract() && !m.isEliminated();
    }

    public boolean hasDeclaredNonAbstractMethod(String name, String desc) {
        BytecodeMethod declaredMethod = findDeclaredMethod(name, desc);
        return declaredMethod != null && !declaredMethod.isAbstract();
    }

    /** any declaration (abstract or not) of the given method in THIS class. */
    public boolean hasDeclaredMethod(String name, String desc) {
        return findDeclaredMethod(name, desc) != null;
    }

    /// Walks {@code concrete} and then its superclass chain and returns the first
    /// class that declares a non-abstract {@code name}/{@code desc}, or null when
    /// none does.
    ///
    /// This is the method the runtime would dispatch to for an instance of
    /// {@code concrete}, which is precisely what a {@code @Concrete}
    /// devirtualization is allowed to bind directly. Looking only at
    /// {@code concrete}'s own declarations -- which is what this replaced -- gave
    /// up on every method the concrete class inherits rather than overrides. That
    /// was harmless while every {@code @Concrete} target derived straight from the
    /// annotated base, and stopped being harmless once a port's implementation
    /// subclassed another port's (MacImplementation extends IOSImplementation),
    /// because the ~1,200 inherited methods -- Graphics primitives among them --
    /// silently fell back to full virtual dispatch.
    public static ByteCodeClass findConcreteDeclaringClass(ByteCodeClass concrete, String name, String desc) {
        ByteCodeClass c = concrete;
        while (c != null) {
            if (c.hasDeclaredNonAbstractMethod(name, desc)) {
                return c;
            }
            c = c.getBaseClassObject();
        }
        return null;
    }

    public void unmark() {
        marked = false;
    }
    
    private void markDependent(List<ByteCodeClass> lst) {
        if(marked) {
            return;
        }

        marked = true;
        
        // make sure the method/classname are in the constant pool so we can later 
        // look them up in case of a stack trace exception
        Parser.addToConstantPool(clsName);
        for(BytecodeMethod bm : methods) {
            if(!bm.isEliminated()) {
                Parser.addToConstantPool(bm.getMethodName());
                bm.addToConstantPool();
            }
        }
        
        for(String s : dependsClassesInterfaces) {
            ByteCodeClass cls = findClass(s, lst);
            
            // annotation can be null
            if(cls != null) {
                cls.markDependent(lst);
            }
        }
    }
    
    public static List<ByteCodeClass> clearUnmarked(List<ByteCodeClass> lst) {
        List<ByteCodeClass> response = new ArrayList<ByteCodeClass>();
        for(ByteCodeClass bc : lst) {
            if(bc.marked) {
                response.add(bc);
            }
        }
        return response;
    }
    
    private ByteCodeClass findClass(String s, List<ByteCodeClass> lst) {
        // lst is always Parser.classes here (markDependencies -> markDependent), so
        // the shared name index gives the same first-match result in O(1) instead of
        // the old O(N) scan that ran per dependency per class during marking.
        return Parser.getClassObject(s);
    }
    
    public void updateAllDependencies() {
        dependsClassesInterfaces.clear();
        exportsClassesInterfaces.clear();
        dependsClassesInterfaces.add("java_lang_NullPointerException");
        if(ByteCodeTranslator.isCheckedCastsEnabled()) {
            // Kept alive for BC_CHECKCAST_CHECKED, which is emitted under the same
            // flag. Retaining it only when the check is emitted keeps the class out
            // of every build that does not enforce casts.
            dependsClassesInterfaces.add("java_lang_ClassCastException");
            dependsClassesInterfaces.add("java_lang_ArrayStoreException");
        }
        setBaseClass(baseClass);
        if (isAnnotation) {
            dependsClassesInterfaces.add("java_lang_annotation_Annotation");
        }
        for(String s : baseInterfaces) {
            s = s.replace('/', '_').replace('$', '_');
            if(!dependsClassesInterfaces.contains(s)) {
                dependsClassesInterfaces.add(s);
            }
            exportsClassesInterfaces.add(s);
        }
        if(virtualMethodList != null) {
            virtualMethodList.clear();
        } else {
            virtualMethodList = new ArrayList<BytecodeMethod>();
        }
        fillVirtualMethodTable(virtualMethodList);
        for(BytecodeMethod m : methods) {
            if(m.isEliminated()) {
                continue;
            }
            // late fold-dependency re-scan (see the method's javadoc): must run
            // now, when all classes are parsed, before the dep list is copied
            m.updateInlinableFieldDependencies();

            for(String s : m.getDependentClasses()) {
                if(!dependsClassesInterfaces.contains(s)) {
                    dependsClassesInterfaces.add(s);
                }
            }
            //for (String s : m.getExportedClasses()) {
            //    exportsClassesInterfaces.add(s);
            //}
        }
        for(ByteCodeField m : fields) {
            for(String s : m.getDependentClasses()) {
                if(!dependsClassesInterfaces.contains(s)) {
                    dependsClassesInterfaces.add(s);
                }
            }
        }
        
        // Resolve concrete invoke dependencies.  Invoke.addDependencies runs at
        // parse time when classes with @Concrete annotations may not yet be
        // loaded, so the concrete target is missed.  Re-scan here — all classes
        // have been parsed by the time updateAllDependencies is called.
        List<String> concreteExtras = new ArrayList<String>();
        for (String dep : dependsClassesInterfaces) {
            ByteCodeClass depClass = Parser.getClassObject(dep);
            if (depClass != null && depClass.getConcreteClass() != null) {
                String concrete = depClass.getConcreteClass().replace('/', '_').replace('$', '_');
                if (!dependsClassesInterfaces.contains(concrete) && !concreteExtras.contains(concrete)) {
                    concreteExtras.add(concrete);
                }
            }
        }
        for (String c : concreteExtras) {
            dependsClassesInterfaces.add(c);
        }
    }
    
    private boolean isMethodFromBaseOrInterface(BytecodeMethod bm) {
        if(baseInterfacesObject != null) {
            for(ByteCodeClass bi : baseInterfacesObject) {
                if(bi.getMethods().contains(bm)) {
                    return true;
                }
                if(bi.getBaseClassObject() != null) {
                    boolean b = bi.isMethodFromBaseOrInterface(bm);
                    if(b) {
                        return true;
                    }
                }
            }
        }
        if(baseClassObject != null) {
            if(baseClassObject.getMethods().contains(bm)) {
                return true;
            }
            return baseClassObject.isMethodFromBaseOrInterface(bm);
        }
        return false;
    }
    
    private boolean hasDefaultConstructor() {
        for(BytecodeMethod bm : methods) {
            if(bm.isDefaultConstructor()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFinalizer() {
        for(BytecodeMethod bm : methods) {
            if(bm.isFinalizer()) {
                return true;
            }
        }
        return false;
    }

    // True iff this class OR any ancestor actually declares a finalize() override, i.e.
    // the emitted __FINALIZER_<class> chain runs real user code (not just the empty
    // Object finalizer). Used to decide whether the clazz.finalizerFunction pointer is
    // worth emitting: a class with no real finalizer anywhere in its hierarchy gets a
    // null pointer, so freeAndFinalize / cn1BibopReclaimSlot (both guard `ptr != 0`)
    // skip a no-op indirect call -- and the BiBOP sweep can treat such a page's dead
    // slots as needing no per-slot reclaim work. Conservative on an unresolved base.
    private boolean hasRealFinalizerInHierarchy() {
        ByteCodeClass c = this;
        while(c != null) {
            if(c.hasFinalizer() || hasNativeBlocks(c.clsName)) {
                return true;
            }
            if(c.baseClassObject == null) {
                // root (Object: baseClass==null) -> no real finalizer; otherwise the base
                // is unresolved -> assume it might declare one.
                return c.baseClass != null;
            }
            c = c.baseClassObject;
        }
        return false;
    }

    // Leaf objects need a mark bit, but no queued tracing callback. Native reference
    // blocks and Reference.referent count even though neither is a normal strong field.
    private boolean hasGcReferencesInHierarchy() {
        ByteCodeClass c = this;
        while (c != null) {
            for (ByteCodeField field : c.fields) {
                if (!field.isStaticField() && field.isObjectType()) return true;
            }
            for (NativeBlock block : NATIVE_BLOCKS) {
                if (block.references && block.owner.equals(c.clsName)) return true;
            }
            if (c.baseClassObject == null) return c.baseClass != null;
            c = c.baseClassObject;
        }
        return false;
    }

    private boolean isInterfaceInHierarchy(String className) {
        if (clsName.equals(className)) {
            return true;
        }
        if (baseInterfacesObject != null) {
            for (ByteCodeClass bc : baseInterfacesObject) {
                if (bc.isInterfaceInHierarchy(className)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static void addArrayType(String type, int dimenstions) {
        String arr = dimenstions + "_" + type;
        if(!arrayTypes.contains(arr)) {
            arrayTypes.add(arr);
        }
    }



    // One reusable emit buffer for the whole output pass, reset per class rather
    // than reallocated. Parser.writeOutput -> writeFile -> generateCCode is a
    // single sequential loop with no executor and one call site, so there is no
    // concurrent or re-entrant use to guard against.
    //
    // This is not micro-tuning. A fresh StringBuilder starts at capacity 16 and
    // JavaAPI grows by 1.5x ((len>>1)+len+2), so building N chars allocates about
    // 3N chars = 6N bytes in abandoned intermediate arrays. Across 5897 emitted
    // files totalling 245MB that is roughly 1.4GB of pure churn, and MEASURED on
    // ParparVM the emit phase allocated 2518MB in a single GC cycle against a
    // 24MB trigger. Keeping the capacity across classes means the growth series
    // runs only until the buffer reaches the largest class, then never again.
    private static final StringBuilder EMIT_BUFFER = new StringBuilder(1 << 20);

    /* Cap on the interface-thunk switch. A jump table is O(1) however wide it is, but
     * every arm is a call the compiler may inline, so an interface with hundreds of
     * implementors would trade the dispatch for code size. Interfaces that wide are
     * also the ones whose receivers are genuinely unpredictable, where the indirect
     * form loses nothing. */
    private static final int CN1_MAX_THUNK_CASES = 16;
    // Bound on the cone we are willing to walk at all -- compile-time insurance,
    // not a codegen judgement; the real limit is CN1_MAX_THUNK_CASES above.
    private static final int CN1_MAX_THUNK_CONE = 1024;

    /**
     * Class-id switch cases for one interface method: every concrete implementor of
     * this interface, paired with the function that implementor runs.
     *
     * @param m the interface method
     * @return cases, or null when there are none or too many to be worth a switch
     */
    // generateCCode asks for every table TWICE -- once to collect the include-only
    // dependencies its arms name, once to emit it -- and building one walks the cone,
    // climbs a superclass chain per member and formats a symbol per arm. On Collection
    // that is 756 classes across fifteen methods, and the translator is itself the
    // benchmark, so the second pass showed up as retired instructions.
    private java.util.Map<BytecodeMethod, List<String[]>> thunkCaseCache =
            new java.util.HashMap<BytecodeMethod, List<String[]>>();

    private List<String[]> thunkCasesFor(BytecodeMethod m) {
        if (!isInterface) {
            return null;
        }
        if (thunkCaseCache.containsKey(m)) {
            return thunkCaseCache.get(m);
        }
        List<String[]> computed = computeThunkCasesFor(m);
        thunkCaseCache.put(m, computed);
        return computed;
    }

    private List<String[]> computeThunkCasesFor(BytecodeMethod m) {
        List<ByteCodeClass> cone = Parser.concreteReceiverCone(this);
        if (cone == null || cone.isEmpty() || cone.size() > CN1_MAX_THUNK_CONE) {
            return null;
        }
        String name = m.getMethodName();
        String desc = m.getDesc();
        List<String[]> cases = new ArrayList<String[]>(cone.size());
        StringBuilder sig = new StringBuilder("__");
        BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, sig, new ArrayList<String>());
        for (ByteCodeClass c : cone) {
            ByteCodeClass d = c;
            while (d != null && !d.hasDeclaredNonAbstractMethod(name, desc)) {
                String base = d.getBaseClass();
                d = base == null ? null : Parser.getClassObject(base.replace('/', '_').replace('$', '_'));
            }
            if (d == null || d.isEliminated()) {
                // One implementor whose body cannot be named here -- a default method on
                // the interface itself, or something the dead code pass removed. SKIP
                // it rather than abandoning the table: the default arm below is the
                // original indirect dispatch, so a receiver with no case still reaches
                // the right implementation. Refusing the whole table for one such
                // implementor produced ZERO switches on this corpus.
                continue;
            }
            cases.add(new String[] {
                "cn1_class_id_" + c.getClsName(),
                d.getClsName() + "_" + m.getCMethodName() + sig.toString(),
                d.getClsName() });
        }
        // The cost that matters is the number of DISTINCT bodies, not the number of
        // classes: a wide cone whose members all inherit one implementation collapses to
        // a couple of arms, and refusing it for its width is what left Iterator (116
        // implementors), Iterable (129), Comparable (52) and Collection (756) -- the
        // hottest interfaces in the VM -- dispatching indirectly. Group the labels, then
        // judge the table by how many arms it really has.
        java.util.Set<String> targets = new java.util.HashSet<String>();
        for (String[] c : cases) {
            targets.add(c[1]);
        }
        if (targets.size() > CN1_MAX_THUNK_CASES) {
            return null;
        }
        java.util.Collections.sort(cases, THUNK_CASE_ORDER);
        return cases;
    }

    private static final java.util.Comparator<String[]> THUNK_CASE_ORDER = new java.util.Comparator<String[]>() {
        public int compare(String[] a, String[] b) {
            int r = a[1].compareTo(b[1]);
            return r != 0 ? r : a[0].compareTo(b[0]);
        }
    };

    public String generateCCode(List<ByteCodeClass> allClasses) {

        StringBuilder b = EMIT_BUFFER;
        b.setLength(0);
        b.append("#include \"");
        b.append(clsName);
        
        b.append(".h\"\n");
        

        /* INCLUDE-ONLY dependencies, kept apart from dependsClassesInterfaces on
         * purpose. That set drives two unrelated things at once -- which headers this
         * file includes, and which classes the dead code pass must keep -- and a
         * guarded dispatch needs the first without the second.
         *
         * A guarded site names two to four concrete implementations directly and falls
         * back to the ordinary virtual thunk, so it keeps NOTHING alive that the
         * virtual call did not already keep: the thunk still marks the whole family
         * used. Adding the targets to dependsClassesInterfaces to get their headers was
         * measured at 853 to 951 emitted classes and 136B to 245B instructions, because
         * forcing one class alive enlarges other sites' cones and keeps their targets
         * alive in turn.
         *
         * Collected before the bodies are emitted because the includes are written
         * first. The queries are memoised, so the extra pass is a map lookup per site. */
        java.util.Set<String> guardIncludes = new java.util.TreeSet<String>();
        if(isInterface && virtualMethodList != null) {
            for(BytecodeMethod m : virtualMethodList) {
                if(m.getClsName().equals("java_lang_Object") || m.isVirtualOverriden()) {
                    continue;
                }
                List<String[]> cases = thunkCasesFor(m);
                if(cases != null) {
                    for(String[] c : cases) {
                        guardIncludes.add(c[2]);
                    }
                }
            }
        }
        for(BytecodeMethod m : methods) {
            if(m.isEliminated()) {
                continue;
            }
            for(com.codename1.tools.translator.bytecodes.Instruction i : m.getInstructions()) {
                if(i instanceof com.codename1.tools.translator.bytecodes.Invoke) {
                    ((com.codename1.tools.translator.bytecodes.Invoke)i).collectGuardIncludes(guardIncludes);
                }
            }
        }
        for(String s : dependsClassesInterfaces) {
            if (exportsClassesInterfaces.contains(s)) {
                continue;
            }
            guardIncludes.remove(s);
            b.append("#include \"");
            b.append(s);
            b.append(".h\"\n");
        }
        guardIncludes.remove(clsName);
        for(String s : guardIncludes) {
            if (exportsClassesInterfaces.contains(s)) {
                continue;
            }
            b.append("#include \"");
            b.append(s);
            b.append(".h\"\n");
        }
        // call-site-inlined fast paths for the hottest String/StringBuilder
        // natives (self-disables via __has_include when those classes are
        // eliminated from the build)
        b.append("#include \"cn1_intrinsics.h\"\n");
        
        b.append("const struct clazz *base_interfaces_for_");
        b.append(clsName);
        b.append("[] = {");
        boolean first = true;
        for(String ints : baseInterfaces) {
            if(!first) {
                b.append(", ");            
            }
            first = false;
            b.append("&class__");
            b.append(ints.replace('/', '_').replace('$', '_'));
        }
        b.append("};\n");
        
        
        // class struct, contains vtable, static fields and meta data (class name), type info etc.
        b.append("struct clazz class__");
        b.append(clsName);
        b.append(" = {\n");
        // object fields so class will be compatible to object
        
        // The header's first value is a class INDEX (classId + 1; see CN1_OBJ_HEADER_FIELDS).
        if(clsName.equals("java_lang_Class")) {
            b.append("  DEBUG_GC_INIT 0, 0, 0, ");
        } else {
            b.append("  DEBUG_GC_INIT cn1_class_id_java_lang_Class + 1, 0, 0, ");
        }
        // finalizerFunction: null unless a real finalize() exists in the hierarchy (the
        // __FINALIZER_<class> chain is still emitted for classes that DO, so subclass
        // chaining is unaffected).
        if(hasRealFinalizerInHierarchy()) {
            b.append("&__FINALIZER_");
            b.append(clsName);
        } else {
            b.append("0");
        }
        b.append(" ,0 , ");
        if (hasGcReferencesInHierarchy()) {
            b.append("&__GC_MARK_").append(clsName);
        } else {
            b.append("0");
        }
        
        // initialized defaults to false
        b.append(",  0, ");
        
        // the numberic id of the class 
        b.append("cn1_class_id_");
        b.append(clsName);
        b.append(", ");
        
        // name of the class
        b.append("\"");
        b.append(clsName.replace('_', '.'));
        b.append("\", ");
        
        // is array class type
        b.append("0, ");
        
        // array type dimensions
        b.append("0, ");
        
        // array internal type
        b.append("0, ");
        
        // primitive type
        b.append("JAVA_FALSE, ");
        
        // reference to the base class
        if(baseClass != null) {
            b.append("&class__");
            b.append(baseClass.replace('/', '_').replace('$', '_'));
        } else {
            b.append("(const struct clazz*)0");
        }
        b.append(", ");
        
        // references to the base interfaces
        b.append("base_interfaces_for_");
        b.append(clsName);
        b.append(", ");

        // number of base interfaces
        b.append(baseInterfaces.size());
        
        // new instance function pointer
        if(!isInterface && !isAbstract && hasDefaultConstructor()) {
            b.append(", &__NEW_INSTANCE_");
            b.append(clsName);
        } else {
            b.append(", 0");
        }
        
        // vtable 
        b.append(", 0\n");
        
        if (isEnum) {
            b.append(", &__VALUE_OF_");
            b.append(clsName);
        } else {
            b.append(", 0");
        }
        
        /*
        JAVA_BOOLEAN isSynthetic;
    JAVA_BOOLEAN isInterface;
    JAVA_BOOLEAN isAnonymous;
    JAVA_BOOLEAN isAnnotation;
        */
        b
                .append(", ")
                .append(isSynthetic?"JAVA_TRUE":"0")
                .append(", ")
                .append(isInterface?"JAVA_TRUE":"0")
                .append(", ")
                .append(isAnonymous?"JAVA_TRUE":"0")
                .append(", ")
                .append(isAnnotation?"JAVA_TRUE":"0")
                .append(", ")
                .append(getArrayClazz(1));
        
        
        b.append("};\n\n");

        // create class objects for 1 - 3 dimension arrays
        for(int iter = 1 ; iter < 4 ; iter++) {
            if(!emitsArrayClass(clsName, iter)) {
                continue;
            }
            b.append("struct clazz class_array");
            b.append(iter);
            b.append("__");
            b.append(clsName);
            if(clsName.equals("java_lang_Class")) {
                b.append(" = {\n DEBUG_GC_INIT 0, 0, 0, 0, &arrayFinalizerFunction, &gcMarkArrayObject, 0, cn1_array_");
            } else {
                b.append(" = {\n DEBUG_GC_INIT cn1_class_id_java_lang_Class + 1, 0, 0, 0, &arrayFinalizerFunction, &gcMarkArrayObject, 0, cn1_array_");
            }
            b.append(iter);
            b.append("_id_");
            b.append(clsName);
            b.append(", \"");
            b.append(clsName.replace('_', '.'));
            for (int arrayDim = 0; arrayDim < iter; arrayDim++) {
                b.append("[]");
            }
            b.append("\", ");

            // array class type, dimension & internal type
            b.append("JAVA_TRUE, ");
            b.append(iter);
            b.append(", &class__");
            b.append(clsName);

            /*
            JAVA_BOOLEAN primitiveType;

            const struct clazz* baseClass;
            const struct clazz** baseInterfaces;
            const int baseInterfaceCount;

            void* newInstanceFp;

            // virtual method table lookup
            void** vtable;

            void* enumValueOfFp;
            JAVA_BOOLEAN isSynthetic;
            JAVA_BOOLEAN isInterface;
            JAVA_BOOLEAN isAnonymous;
            JAVA_BOOLEAN isAnnotation;
            */
            // primitive type is always false here object is always the base class of the array it has no base interfaces
            b.append(", JAVA_FALSE, &class__java_lang_Object, EMPTY_INTERFACES, 0, ");

            // new instance function pointer and vtable 
            b.append("0, 0, 0, 0, 0, 0, 0, "+getArrayClazz(iter+1)+"\n};\n\n");
        }

        // Accessors below test completion before fetching thread context. The
        // recursion flag in class__X is set before <clinit> and cannot publish it.
        b.append("static int __").append(clsName).append("_LOADED__=0;\n");
        staticFieldList = new ArrayList<ByteCodeField>();
        buildStaticFieldList(staticFieldList);
        String enumValuesField = null;
        // static fields for the class
        for(ByteCodeField bf : staticFieldList) {
            if(bf.isStaticField() && bf.getClsName().equals(clsName)) {
                if (isEnum && ("_VALUES".equals(bf.getFieldName().replace('$','_')) || "ENUM_VALUES".equals(bf.getFieldName().replace('$','_')))) {
                    enumValuesField = bf.getFieldName();
                }
                if(bf.isFinal() && bf.getValue() != null && !writableFields.contains(bf.getFieldName())) {
                    // static getter 
                    b.append(bf.getCDefinition());
                    b.append(" get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName().replace('$', '_'));
                    b.append("() {\n    return ");
                    if(bf.getValue() instanceof String) {
                        b.append("STRING_FROM_CONSTANT_POOL_OFFSET(");
                        b.append(Parser.addToConstantPool((String)bf.getValue()));
                        b.append(") /* ");
                        b.append(String.valueOf(bf.getValue()).replace("*/", "* /"));
                        b.append(" */");
                    } else {
                        if(bf.getValue() instanceof Number) {
                            if(bf.getValue() instanceof Double) {
                                Double d = ((Double)bf.getValue());
                                if(d.isNaN()) {
                                    b.append("0.0/0.0");                                    
                                } else {
                                    if(d.isInfinite()) {
                                        if(d.doubleValue() > 0) {
                                            b.append("1.0f / 0.0f");
                                        } else {
                                            b.append("-1.0f / 0.0f");
                                        }
                                    } else {
                                        b.append(CNumber.literal(d.doubleValue()));
                                    }
                                }
                            } else {
                                if(bf.getValue() instanceof Float) {
                                    Float d = ((Float)bf.getValue());
                                    if(d.isNaN()) {
                                        b.append("0.0/0.0");                                    
                                    } else {
                                        if(d.isInfinite()) {
                                            if(d.floatValue() > 0) {
                                                b.append("1.0f / 0.0f");
                                            } else {
                                                b.append("-1.0f / 0.0f");
                                            }
                                        } else {
                                            b.append(CNumber.literal(d.floatValue()));
                                        }
                                    }
                                } else {
                                    b.append(bf.getValue());
                                }
                            }
                        } else {
                            if(bf.getValue() instanceof Boolean) {
                                if(((Boolean)bf.getValue()).booleanValue()) {
                                    b.append("JAVA_TRUE");
                                } else {
                                    b.append("JAVA_FALSE");
                                }
                            } else {
                                b.append("JAVA_NULL");
                            }
                        }
                    }
                    b.append(";\n}\n\n");                    
                } else {
                    b.append(bf.getCStorageDefinition());
                    b.append(" STATIC_FIELD_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    if (bf.isVolatile()) {
                        // No initializer. A static object is zero-initialized by
                        // the language, and ATOMIC_VAR_INIT expands to a plain
                        // parenthesized value -- which clang 14 (Debian bookworm,
                        // and therefore the glibc backend builder image) rejects
                        // on an atomic POINTER as "initializer element is not a
                        // compile-time constant". The macro is also deprecated in
                        // C17 and gone in C23, so this is where it was heading
                        // regardless. Reached by any `volatile` static reference
                        // field in user code.
                        b.append(";\n");
                    } else {
                        b.append(" = 0;\n");
                    }

                    // static getter
                    b.append(bf.getCDefinition());
                    b.append(" get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName().replace('$', '_'));
                    // Match the initializer's acquire/release completion check.
                    // TLS lookup and initialization are cold after the first access.
                    // No guard at all for an eagerly initialized class: it was
                    // initialized before any Java ran (isEagerInitEligible).
                    boolean accessGuard = !eagerInit(bf.getClsName());
                    b.append("() {\n");
                    if (accessGuard) {
                        b.append("    if (!__atomic_load_n(&__").append(bf.getClsName());
                        b.append("_LOADED__, __ATOMIC_ACQUIRE)) __STATIC_INITIALIZER_");
                        b.append(bf.getClsName()).append("(getThreadLocalData());\n");
                    }
                    if (bf.isVolatile()) {
                        b.append("     return atomic_load_explicit(&STATIC_FIELD_");
                        b.append(bf.getClsName());
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append(", memory_order_acquire);\n}\n\n");
                    } else {
                        b.append("     return STATIC_FIELD_");
                        b.append(bf.getClsName());
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append(";\n}\n\n");
                    }

                    // Static object fields may interact with heap bookkeeping, so they keep thread context.
                    b.append("void set_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName().replace('$', '_'));
                    b.append("(");
                    if (bf.isObjectType()) {
                        b.append("CODENAME_ONE_THREAD_STATE, ");
                    }
                    b.append(bf.getCDefinition());
                    b.append(" __cn1StaticVal) {\n    ");
                    if (accessGuard) {
                        b.append("if (!__atomic_load_n(&__").append(bf.getClsName());
                        b.append("_LOADED__, __ATOMIC_ACQUIRE)) __STATIC_INITIALIZER_");
                        b.append(bf.getClsName());
                        b.append(bf.isObjectType() ? "(threadStateData);\n    " : "(getThreadLocalData());\n    ");
                    }
                    if (bf.isObjectType()) {
                        // SATB insertion barrier: record the reference being stored, so
                        // a static that takes a child mid-mark keeps it alive.
                        b.append("CN1_WRITE_BARRIER(JAVA_NULL, __cn1StaticVal);\n    ");
                        // SATB deletion barrier: preserve the overwritten static reference.
                        b.append("CN1_SATB_DELETE(&STATIC_FIELD_").append(bf.getClsName())
                         .append("_").append(bf.getFieldName()).append(");\n    ");
                    }
                    if (bf.isVolatile()) {
                        b.append("atomic_store_explicit(&STATIC_FIELD_");
                        b.append(bf.getClsName());
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append(", __cn1StaticVal, memory_order_release);");
                    } else {
                        b.append("STATIC_FIELD_");
                        b.append(bf.getClsName());
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append(" = __cn1StaticVal;");
                    }
                    if(bf.shouldRemoveFromHeapCollection()) {
                        if(bf.getType() != null && bf.getType().endsWith("String")) {
                            b.append("\n    removeObjectFromHeapCollection(threadStateData, __cn1StaticVal);\n    if(__cn1StaticVal != 0) {\n        removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)__cn1StaticVal)->java_lang_String_value);\n    }\n}\n\n");
                        } else {
                            b.append("\n    removeObjectFromHeapCollection(threadStateData, __cn1StaticVal);\n}\n\n");
                        }
                    } else {
                        b.append("\n}\n\n");
                    }
                }
            }
        }
        
        if(isInterface) {
            b.append("int **classToInterfaceMap_");
            b.append(clsName);
            b.append(";\n");
        }
        
        fullFieldList = new ArrayList<ByteCodeField>();
        buildInstanceFieldList(fullFieldList);
        
        String nullCheck = "";
        if (Util.getProperty("fieldNullChecks", "false").equals("true")) {
            nullCheck = "if(__cn1T == JAVA_NULL){throwException(getThreadLocalData(), __NEW_INSTANCE_java_lang_NullPointerException(getThreadLocalData()));}\n";
        }
        for(ByteCodeField fld : fullFieldList) {
            // A conditionally-declared field has no accessor on a target that does
            // not declare it; see targetGuardFor. The guard has to wrap the WHOLE
            // getter/setter pair, so it opens here and closes after the setter.
            String fldGuard = targetGuardFor(fld.getClsName().replace('/', '_').replace('$', '_'),
                    fld.getFieldName());
            if(fldGuard == null) {
                fldGuard = targetGuardFor(clsName, fld.getFieldName());
            }
            if(fldGuard != null) {
                b.append("#if ").append(fldGuard).append("\n");
            }
            b.append(fld.getCDefinition());
            b.append(" get_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(JAVA_OBJECT __cn1T) {\n ").append(nullCheck).append("    ");
            if(isReferenceReferent(clsName, fld)) {
                // Reference.get() compiles into this accessor, and reading a weak referent
                // while a concurrent mark is running needs a barrier an ordinary field read
                // does not.
                //
                // ONE LOAD, and everything below acts on that value. The collector clears a
                // reference after the strong mark reaches its fixpoint but with mutators
                // still running, so a thread whose stack was scanned and released early can
                // take the referent out of here and hold it in a local the collector has
                // walked past -- neither marked nor fresh, the one case the sweep's "already
                // marked or FRESH" invariant does not cover. Enqueuing puts it back in the
                // snapshot, and the trial clear of gcSatbActive then finds a non-empty log,
                // re-arms, marks it, and leaves the reference alone.
                //
                // REGISTERING BEFORE THE LOAD, and holding it across, is what makes that
                // sound. Any flag sampled before registering can go stale in the gap: a
                // thread that read the flag as 0 -- or read it as 1 and was then descheduled
                // before registering -- can come away with an unmarked referent nothing
                // enqueued, while the collector finishes termination and sweeps it.
                // CN1_REF_LOAD_BEGIN registers first and answers afterwards, so the
                // collector's quiesce cannot complete anywhere inside this accessor.
                b.append("JAVA_BOOLEAN __cn1RefActive = CN1_REF_LOAD_BEGIN();\n    ");
                b.append(fld.getCDefinition()).append(" __cn1Ref = __atomic_load_n(&((struct obj__")
                 .append(clsName).append("*)__cn1T)->")
                 .append(fld.getClsName()).append("_").append(fld.getFieldName())
                 .append(", __ATOMIC_RELAXED);\n    ");
                b.append("CN1_SATB_REF_KEEP(__cn1RefActive, __cn1Ref);\n    ");
                // The touch stamp, and the entire per-read cost of ranking soft references
                // by use: a store of an immediate. Unconditional rather than guarded by a
                // "did it change" test, because the branch would cost more than the store.
                b.append("__atomic_store_n(&((struct obj__").append(clsName).append("*)__cn1T)->")
                 .append(REFERENCE_CLASS).append("_cn1TouchAge, CN1_REF_TOUCHED, __ATOMIC_RELAXED);\n    ");
                b.append("CN1_REF_LOAD_END();\n    ");
                b.append("return __cn1Ref;\n}\n\n");
            } else if (fld.isVolatile()) {
                b.append("return atomic_load_explicit(&((struct obj__");
                b.append(clsName);
                b.append("*)__cn1T)->");
                b.append(fld.getClsName());
                b.append("_");
                b.append(fld.getFieldName());
                b.append(", memory_order_acquire);\n}\n\n");
            } else {
                b.append("return ((struct obj__");
                b.append(clsName);
                b.append("*)__cn1T)->");
                b.append(fld.getClsName());
                b.append("_");
                b.append(fld.getFieldName());
                b.append(";\n}\n\n");
            }

            // Instance field setters don't use thread context directly.
            b.append("void set_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(");
            b.append(fld.getCDefinition());
            if(fld.isObjectType()) {
                b.append(" __cn1Val, JAVA_OBJECT __cn1T) {\n ").append(nullCheck).append("   ");
                // SATB insertion barrier: record the reference being stored, so an object
                // linked into the graph mid-mark is kept alive even when the container it
                // is stored into is a fresh grace object the mark has not reached. Off-mark
                // this is one predicted-not-taken flag load.
                b.append("CN1_WRITE_BARRIER(__cn1T, __cn1Val); ");
                // SATB deletion barrier: preserve the reference being overwritten for the
                // current mark cycle. No-op (single flag load) outside GC.
                // The referent takes the ATOMIC deletion barrier: the collector stores
                // JAVA_NULL into that field concurrently, so the generic macro's plain
                // volatile read would leave the pair a mixed atomic/non-atomic access.
                b.append(isReferenceReferent(clsName, fld) ? "CN1_SATB_DELETE_REF" : "CN1_SATB_DELETE")
                 .append("(&((struct obj__").append(clsName).append("*)__cn1T)->")
                 .append(fld.getClsName()).append("_").append(fld.getFieldName()).append("); ");
            } else {
                b.append(" __cn1Val, JAVA_OBJECT __cn1T) {\n  ").append(nullCheck).append("  ");
            }
            if(isReferenceReferent(clsName, fld)) {
                // Reference.clear() and the constructor both land here, and the collector
                // stores JAVA_NULL into the same word concurrently. Atomic for the reason
                // spelled out on the getter above.
                b.append("__atomic_store_n(&((struct obj__").append(clsName).append("*)__cn1T)->")
                 .append(fld.getClsName()).append("_").append(fld.getFieldName())
                 .append(", __cn1Val, __ATOMIC_RELAXED);\n}\n\n");
            } else if (fld.isVolatile()) {
                b.append("atomic_store_explicit(&((struct obj__");
                b.append(clsName);
                b.append("*)__cn1T)->");
                b.append(fld.getClsName());
                b.append("_");
                b.append(fld.getFieldName());
                b.append(", __cn1Val, memory_order_release);\n}\n\n");
            } else {
                b.append("((struct obj__");
                b.append(clsName);
                b.append("*)__cn1T)->");
                b.append(fld.getClsName());
                b.append("_");
                b.append(fld.getFieldName());
                b.append(" = __cn1Val;\n}\n\n");
            }
            if(fldGuard != null) {
                b.append("#endif\n");
            }
        }
                
        
        // finalizer and GC_RELEASE to cleanup variables
        b.append("JAVA_VOID __FINALIZER_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToDelete) {\n");
        // Invoke the most-derived Java finalizer once. Java code decides whether
        // to call super.finalize(); native storage cleanup is independent of it.
        ByteCodeClass finalizerOwner = this;
        while (finalizerOwner != null && !finalizerOwner.hasFinalizer()) {
            finalizerOwner = finalizerOwner.baseClassObject;
        }
        if (finalizerOwner != null) {
            b.append("    cn1InvokeFinalizer(threadStateData, objToDelete, ");
            b.append(finalizerOwner.clsName);
            b.append("_finalize__);\n");
        }
        // Walk ownership, not the Java finalize chain: throwing or omitting
        // super.finalize() must not leak an inherited native backing block.
        for (ByteCodeClass owner = this; owner != null; owner = owner.baseClassObject) {
            for (NativeBlock block : NATIVE_BLOCKS) {
                if (!block.owner.equals(owner.clsName)) continue;
                if (block.owns) {
                    if ("java_lang_StringBuilder".equals(block.owner)) {
                        b.append("    if (((struct obj__java_lang_StringBuilder*)objToDelete)->java_lang_StringBuilder_cn1Storage != ")
                         .append("(JAVA_LONG)(uintptr_t)((struct obj__java_lang_StringBuilder*)objToDelete)->__cn1InlineStorage)\n");
                    }
                    b.append("    cn1RefBlockFree(((struct obj__").append(block.owner);
                    b.append("*)objToDelete)->").append(block.owner).append("_").append(block.field).append(");\n");
                }
                b.append("    ((struct obj__").append(block.owner);
                b.append("*)objToDelete)->").append(block.owner).append("_").append(block.field).append(" = 0;\n");
            }
        }

        b.append("}\n\n");
                
        // mark function for the GC mark cycle to tag the objects that are reachable
        b.append("void __GC_MARK_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToMark, JAVA_BOOLEAN force) {\n");
        // The complete layout is known here: trace inherited fields in the same
        // callback, sharing one marking context rather than chaining base callbacks.
        List<ByteCodeField> markFields = new ArrayList<ByteCodeField>();
        for (ByteCodeClass owner = this; owner != null; owner = owner.baseClassObject) {
            for (ByteCodeField field : owner.fields) {
                if (!field.isStaticField()) markFields.add(field);
            }
        }
        boolean marksFields = false;
        for(ByteCodeField fld : markFields) {
            if(!fld.isStaticField() && fld.isObjectType()) {
                marksFields = true;
                break;
            }
        }
        if(marksFields) {
            b.append("    struct obj__");
            b.append(clsName);
            b.append("* objInstance = (struct obj__");
            b.append(clsName);
            b.append("*)objToMark;\n");
            b.append("    const int markEpoch = cn1GcFieldMarkEpoch(force);\n");
        }
        for(ByteCodeField fld : markFields) {
            if(!fld.isStaticField() && fld.isObjectType()) {
                if(isReferenceReferent(fld.getClsName(), fld)) {
                    // THE REFERENT IS NOT TRACED. Handing it to gcMarkObject here is
                    // what made every WeakReference strong; instead the collector is
                    // told the reference exists and is given the addresses it needs to
                    // decide, once the strong mark has closed, whether to keep the
                    // referent or clear the field.
                    //
                    // Addresses rather than the object, deliberately: cn1_globals.m is a
                    // fixed template compiled beside whatever the translator emitted, and
                    // it cannot name `struct obj__java_lang_ref_Reference` -- the class is
                    // absent from any program that never uses a reference, and including
                    // its generated header would make the runtime fail to build for those.
                    // Passing field pointers keeps the layout knowledge on this side,
                    // where it is generated from the layout itself.
                    b.append("    cn1GcDiscoverReference(threadStateData, objToMark, force, &objInstance->");
                    b.append(fld.getClsName()).append("_").append(fld.getFieldName());
                    b.append(", &objInstance->").append(REFERENCE_CLASS).append("_cn1TouchAge");
                    b.append(", &objInstance->").append(REFERENCE_CLASS).append("_cn1AgedCycle");
                    b.append(", objInstance->").append(REFERENCE_CLASS).append("_cn1Strength);\n");
                    continue;
                }
                // TYPE-IDENTITY CHECK, verifier builds only.
                //
                // CN1_GC_VERIFY already proves every traced reference RESOLVES, which
                // is why a reclaimed-and-recycled slot slips past it: the slot holds a
                // perfectly valid object, just not the one the field was pointing at.
                // A Linux suite core caught the consequence -- ArrayList.add running on
                // an object whose class word said charts.compat.Canvas, reading the
                // list's backing-array slot out of two of Canvas's int fields.
                //
                // The field's DECLARED type is known here and thrown away, so the
                // collector has no way to notice. Passing it lets the verifier ask
                // whether what the field holds is assignable to what it was declared
                // as, which is exactly the question a recycled slot answers wrongly --
                // and it names the field, instead of leaving a SIGSEGV in an unrelated
                // method a whole cycle later.
                //
                // Arrays are skipped for now: their id mapping is dimensional and the
                // failure this was written for was a plain object field.
                // getRuntimeDescriptor() is the mangled type for a plain object field
                // and carries "[]" for an array, which is how arrays are excluded.
                String fldType = fld.getRuntimeDescriptor();
                if (fldType != null && fldType.indexOf('[') < 0
                        && Parser.getClassObject(fldType) != null) {
                    b.append("#ifdef CN1_GC_VERIFY\n");
                    b.append("    cn1GcVerifyFieldType(threadStateData, objToMark, objInstance->");
                    b.append(fld.getClsName()).append("_").append(fld.getFieldName());
                    b.append(", cn1_class_id_").append(fldType);
                    b.append(", \"").append(clsName).append(".").append(fld.getFieldName()).append("\");\n");
                    b.append("#endif\n");
                }
                b.append("    cn1GcMarkField(threadStateData, ");
                if (fld.isVolatile()) {
                    b.append("atomic_load_explicit(&objInstance->");
                    b.append(fld.getClsName());
                    b.append("_");
                    b.append(fld.getFieldName());
                    b.append(", memory_order_acquire)");
                } else {
                    b.append("objInstance->");
                    b.append(fld.getClsName());
                    b.append("_");
                    b.append(fld.getFieldName());
                }
                b.append(", force, markEpoch);\n");
            }
        }
        // Trace native backing blocks from the complete inherited layout too.
        for (NativeBlock block : NATIVE_BLOCKS) {
            boolean ownsBlock = false;
            for (ByteCodeField field : markFields) {
                if (!field.isStaticField() && block.owner.equals(field.getClsName())
                        && block.field.equals(field.getFieldName())) { ownsBlock = true; break; }
            }
            if (block.references && ownsBlock) {
                // No count argument: the block carries its own length one slot below the
                // pointer. Passing a separate capacity field would let a marker pair a new
                // block with a stale capacity -- see cn1RefBlockAlloc.
                if (block.rootField != null) {
                    b.append("    cn1GcMarkTablePart(threadStateData, ((struct obj__");
                    b.append(clsName).append("*)objToMark)->").append(block.owner).append("_").append(block.rootField);
                    b.append(", ").append(block.part).append(", force);\n");
                } else {
                    b.append("    cn1GcMarkRefBlock(threadStateData, ((struct obj__");
                    b.append(clsName).append("*)objToMark)->").append(block.owner).append("_").append(block.field);
                    b.append(", force);\n");
                }
            }
        }
        // NO SELF-STAMP. This function used to end by storing currentGcMarkValue into the
        // object it traced -- a leftover from when mark functions chained up to Object's
        // and that store was how an object got marked at all. gcMarkObject now stamps
        // before it pushes, so for every drain the store was redundant; the one caller it
        // was not redundant for is the one it broke. The grace pass traces FRESH objects
        // by calling this directly, deliberately without marking them (the sweep's grace
        // rule keeps them), and the store stamped every one anyway: they then read at the
        // sweep as live-this-cycle rather than grace, so measured survival counted the
        // entire fresh generation. On objectAllocation -- a live set of 512 nodes --
        // "live" read 75-80% of occupied, the trigger doubled to its ceiling, and the heap
        // sat at ~600MB.
        b.append("}\n\n");

        // initialize object instances

        if(!isInterface && !isAbstract) {
            b.append("JAVA_OBJECT __NEW_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE) {\n    __STATIC_INITIALIZER_");
            b.append(clsName);
            b.append("(threadStateData);\n");
            if(Parser.isStackIterator(clsName)) {
                // This class is an iterator the translator proved cannot outlive the loop
                // that creates it -- `this` escapes none of its own methods, and no site
                // in the closed world lets the new object escape except by returning it.
                // So if the caller left a stack buffer pending, build there instead of on
                // the heap. sizeof is a compile-time constant in this translation unit, so
                // a class too big for the buffer compiles the branch away entirely.
                b.append("    {\n        JAVA_OBJECT __si = cn1IterScopeTake(threadStateData, &class__");
                b.append(clsName);
                b.append(", sizeof(struct obj__");
                b.append(clsName);
                b.append("));\n        if(__si != JAVA_NULL) {\n            return __si;\n        }\n    }\n");
            }
            b.append("    JAVA_OBJECT o = codenameOneGcMalloc(threadStateData, sizeof(struct obj__");
            b.append(clsName);
            b.append("), &class__");
            b.append(clsName);
            b.append(");\n    return o;\n}\n\n");

            if(hasDefaultConstructor()) {
                b.append("JAVA_OBJECT __NEW_INSTANCE_");
                b.append(clsName);
                b.append("(CODENAME_ONE_THREAD_STATE) {\n    __STATIC_INITIALIZER_");
                b.append(clsName);
                b.append("(threadStateData);\n    JAVA_OBJECT o = codenameOneGcMalloc(threadStateData, sizeof(struct obj__");
                b.append(clsName);
                b.append("), &class__");
                b.append(clsName);
                b.append(");\n");
                b.append(clsName);
                b.append("___INIT____(threadStateData, o);\n    return o;\n}\n\n");
            }
        }
                
        if(arrayTypes.contains("1_" + clsName) || arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("JAVA_OBJECT __NEW_ARRAY_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {\n");
            b.append("    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__");
            b.append(clsName);
            b.append(", sizeof(JAVA_OBJECT), 1);\n    CN1_OBJ_SET_CLASS(o, &class_array1__");
            b.append(clsName);
            b.append(");\n    return o;\n}\n\n");
        }

        /*b.append("JAVA_OBJECT __NEW_MULTI_ARRAY_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_INT dimensions, JAVA_INT* sizes) {\n    JAVA_OBJECT o = JAVA_NULL;\n");
        b.append("    switch(dimensions) {\n        case 2: o = allocMultiArray(sizes, &class_array2__");
        b.append(clsName);
        b.append(", sizeof(JAVA_OBJECT), 2); break;\n");
        b.append("        case 3: o = allocMultiArray(sizes, &class_array3__");
        b.append(clsName);
        b.append(", sizeof(JAVA_OBJECT), 3); break;\n");
        b.append("        case 4: o = allocMultiArray(sizes, &class_array4__");
        b.append(clsName);
        b.append(", sizeof(JAVA_OBJECT), 4); break;\n");
        b.append("        default: return JAVA_NULL;\n    }\n    (*o).__codenameOneParentClsReference = &class__");
        b.append(clsName);
        b.append(";\n    return o;\n}\n\n");*/

        String clInitMethod = null;
        if(isInterface) {
            for(BytecodeMethod m : methods) {
                if(m.getMethodName().equals("__CLINIT__")) {
                    m.appendMethodC(b);
                    clInitMethod = clsName + "_" + m.getMethodName() + "__";
                } else if (m.isAbstract()) {
                    m.appendInterfaceMethodC(b);
                } else {
                    m.appendMethodC(b);
                }
            }
            List<BytecodeMethod> bm = new ArrayList<BytecodeMethod>(methods);
            appendInheritedInterfaceMethods(b, bm);
        } else {
            for(BytecodeMethod m : methods) {
                m.appendMethodC(b);
                if(m.getMethodName().indexOf("_CLINIT_") > -1) {
                    clInitMethod = clsName + "_" + m.getMethodName() + "__";
                }
                if(m.isMain()) {
                    b.append("\nint main(int argc, char *argv[]) {\n");
                    // Line-buffer stdout/stderr. C streams block-buffer when they are
                    // not a tty, so everything an app logs into a pipe -- which is
                    // how CI captures it -- arrives in 4KB chunks. A run that is
                    // killed mid-flight then shows a log ending thousands of lines
                    // behind where the process actually was, and every diagnosis
                    // made from that tail names the wrong place. Three separate
                    // "the suite hangs in X" readings of the Linux job came from
                    // exactly this. Costs a flush per line; buys logs that mean
                    // what they say.
                    // _IONBF, not _IOLBF: the MSVC CRT rejects a line-buffered
                    // request with a NULL buffer and size 0 -- it demands a size of
                    // at least 2 -- and answers the invalid parameter by fail-fasting
                    // the process (0xC0000409), so every Windows clean-target binary
                    // died on its first instruction. _IONBF ignores the size argument
                    // and is valid on every CRT, and unbuffered is what the
                    // diagnostics actually want.
                    b.append("    setvbuf(stdout, NULL, _IONBF, 0);\n");
                    b.append("    setvbuf(stderr, NULL, _IONBF, 0);\n");
                    b.append("    initConstantPool();\n");
                    // An exception no handler catches used to be discarded and
                    // execution continued with the statement after the throw. An
                    // app target nearly always has something upstream that
                    // catches (the EDT's own try), so it stayed invisible there;
                    // a server binary has no such catch, and the symptom is a
                    // process that keeps serving with a half-built object where a
                    // connection should be.
                    //
                    // GATED, and the gate is the point. This main() is emitted for
                    // every target that has one -- iOS and macOS included -- so an
                    // unconditional assignment here would make an uncaught exception
                    // on any thread terminate a SHIPPED app, which is exactly the
                    // behaviour change this runtime path is meant not to cause. Only
                    // the clean target, which has no upstream catch to rely on, opts
                    // in. (Reported on PR #5658: the comment that used to sit here
                    // claimed this was already restricted; it was not.)
                    if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_CLEAN) {
                        b.append("    cn1AbortOnUncaughtException = 1;\n");
                        // This entry executes Java until process exit. Register
                        // both halves of the cooperative safepoint protocol,
                        // just as threadRunner does for a Java Thread.
                        b.append("    struct ThreadLocalData* mainThread = getThreadLocalData();\n");
                        b.append("    mainThread->lightweightThread = JAVA_TRUE;\n");
                        b.append("    mainThread->threadActive = JAVA_TRUE;\n");
                    }
                    // UI host threads remain native: AppKit's event loop enters
                    // Java through callbacks rather than running a Java main to
                    // completion. The clean target above has no such host loop.
                    // On the native macOS target the application's main method
                    // runs on a background thread and AppKit owns the main one.
                    // That is not a preference: the main thread has to be free to
                    // run the event loop, and Codename One's own code marshals to
                    // it synchronously to touch the UI -- so calling the app's
                    // main directly here deadlocks the first time it does, in
                    // dispatch_sync waiting for a queue that is waiting for it.
                    // The generated main becoming the AppKit main is what puts
                    // each on the right thread.
                    if (ByteCodeTranslator.output == ByteCodeTranslator.OutputType.OUTPUT_TYPE_MACOS) {
                        b.append("    [NSApplication sharedApplication];\n");
                        b.append("    [NSApp setActivationPolicy:NSApplicationActivationPolicyRegular];\n");
                        b.append("    CN1MacInstallMainMenu();\n");
                        b.append("    CN1MacInstallAppDelegate();\n");
                        b.append("    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INTERACTIVE, 0), ^{\n");
                        // OPEN QUESTION, INHERITED AND DELIBERATELY NOT ANSWERED HERE.
                        // This dispatched block runs the application's main on a thread
                        // the runtime knows nothing about: its thread-local flags all
                        // default to false, so the collector treats it as native and
                        // takes threadHeapMutex for it, which is the conservative side.
                        // A nursery-only block used to register it as lightweight+active
                        // and retire it with markDeadThread, the way threadRunner does
                        // for every thread it starts -- but it was inside
                        // #ifdef CN1_NURSERY and no build ever compiled it, so that
                        // registration has never actually happened on any shipped macOS
                        // binary. It went with the nursery instead of being switched on,
                        // because enabling it is a behaviour change to a live target and
                        // wants its own change and its own gate. If it is ever revisited:
                        // BOTH flags or neither. The collector reads a lightweight thread
                        // that is not active as PARKED, and a parked thread is precisely
                        // the one whose pendingHeapAllocations it may migrate without the
                        // heap mutex -- so setting lightweight alone is worse than
                        // setting nothing.
                        // Hand main() the real command line. This used to pass
                        // JAVA_NULL, so a translated program could not read its own
                        // arguments at all and every knob had to come in through the
                        // environment (see vm/benchmarks). cn1MainArgs skips argv[0] --
                        // Java's args array excludes the program name.
                        b.append("        ");
                        b.append(clsName);
                        b.append("_main___java_lang_String_1ARRAY(getThreadLocalData(), cn1MainArgs(getThreadLocalData(), argc, argv));\n");
                        // main returning does not end the process here -- AppKit
                        // owns the main thread and keeps running -- so leaving
                        // the worker registered would leave the collector
                        // waiting at every safepoint for a thread that no longer
                        // exists. markDeadThread is declared inline because it
                        // is defined in nativeMethods.m and appears in no
                        // header; without that it is an implicit declaration.
                        b.append("    });\n");
                        b.append("    [NSApp run];\n}\n\n");
                    } else {
                        b.append("    ");
                        b.append(clsName);
                        b.append("_main___java_lang_String_1ARRAY(getThreadLocalData(), cn1MainArgs(getThreadLocalData(), argc, argv));\n}\n\n");
                    }
                }
            }
        }
        if(!isInterface) {
            List<BytecodeMethod> bm = new ArrayList<BytecodeMethod>(methods);
            if(baseClassObject != null) {
                appendSuperStub(b, bm, baseClassObject);
            }
            appendDefaultInterfaceStubs(b, bm);
        }
        int offset = 0;
        if(clsName.equals("java_lang_Class")) {
            // special case for Class which can't have a vtable since it has no class of its own...
            appendClassVFunctions(b);
        } else {
            if(isInterface) {
                // special case, object virtual calls on interfaces should act
                // as if they are regular virtual calls
                for(BytecodeMethod m : virtualMethodList) {
                    if(m.getClsName().equals("java_lang_Object")) {
                        m.appendVirtualMethodC(clsName, b, "" + offset, true);
                    } else {
                        // we pretend to have a virtual method here but the optimizer says its not really needed
                        if(!m.isVirtualOverriden()) {
                            m.setThunkCases(thunkCasesFor(m));
                            m.appendVirtualMethodC(clsName, b, "classToInterfaceMap_" + clsName +
                                    "[cn1__cls->classId][" + offset + "]", true);
                            m.setThunkCases(null);
                        }
                        offset++;
                    }
                }
            } else {
                for(BytecodeMethod m : virtualMethodList) {
                    m.appendVirtualMethodC(clsName, b, offset);
                    offset++;
                }
            }
        }
        if(!isInterface) {
            b.append("void __INIT_VTABLE_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, void** vtable) {\n    ");
            if(baseClass != null) {
                b.append("    __INIT_VTABLE_");
                b.append(baseClass.replace('/', '_').replace('$', '_'));
                b.append("(threadStateData, vtable);\n");
            }
            for(int iter = 0 ; iter < virtualMethodList.size() ; iter++) {

                BytecodeMethod bm = virtualMethodList.get(iter);

                if(bm.getClsName().equals(clsName) && !bm.isVirtualOverriden()) {
                    b.append("    vtable[");
                    b.append(iter);
                    b.append("] = &");
                    bm.appendFunctionPointer(b);
                    b.append(";\n");
                } else if (isDefaultInterfaceMethod(bm, allClasses)) {
                    b.append("    vtable[");
                    b.append(iter);
                    b.append("] = &");
                    bm.appendFunctionPointer(b);
                    b.append(";\n");
                }
            }
            b.append("}\n\n");
        }
        
        if (isEnum) {
            
            b.append("JAVA_OBJECT __VALUE_OF_").append(clsName).append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT value) {\n    ");
            if (enumValuesField != null) {
                b.append("    JAVA_ARRAY values = (JAVA_ARRAY)get_static_").append(clsName).append("_").append(enumValuesField.replace('$', '_')).append("();\n");
                b.append("    JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)CN1_ARRAY_DATA(values);\n");
                b.append("    int len = values->length;\n");
                b.append("    for (int i=0; i<len; i++) {\n");
                b.append("        JAVA_OBJECT name = (*(struct obj__").append(clsName).append("*)data[i]).java_lang_Enum_name;\n");
                b.append("        if (name != JAVA_NULL && java_lang_String_equals___java_lang_Object_R_boolean(threadStateData, name, value)) { return data[i];}\n");
                b.append("    }\n");
                b.append("    return JAVA_NULL;\n");
            } else {
                System.err.println("Unable to find enum VALUES static ield for "+clsName+", this may cause unexpected results when using the "+clsName+" enum.");
                b.append("    return JAVA_NULL;\n");
            }
            b.append("}\n\n");
        }
        
        // The file-local completion flag was declared before the accessors.
        // CN1_CLINIT_ATTR (cold, noinline): see cn1_globals.h. Inlined, this body
        // costs every caller its registers for a path taken once.
        b.append("CN1_CLINIT_ATTR void __STATIC_INITIALIZER_");
        b.append(clsName);
        // ACQUIRE, not a plain load. This is the fast path of a double-checked
        // initialisation: the completing store below is a RELEASE, and the two
        // together are what make the writes this function performed -- the
        // vtable, and every classToInterfaceMap_<iface>[classId] row -- visible
        // to a thread that observes the flag set.
        //
        // With plain accesses on arm64 a second thread could see LOADED==1 while
        // those table stores were still invisible, then index a row that read as
        // NULL. OBSERVED: three identical SIGSEGVs at
        // classToInterfaceMap_java_util_NavigableMap[classId] + 0x8, reached from
        // TreeSet.clear -> the interface dispatch for NavigableMap.clear, in a
        // translator that is single-threaded in its own code but shares the
        // process with the GC thread, which also runs Java and so also runs
        // class initialisers.
        b.append("(CODENAME_ONE_THREAD_STATE) {\n    if(__atomic_load_n(&__")
         .append(clsName).append("_LOADED__, __ATOMIC_ACQUIRE)) return;\n\n    ");

        
        // Block-registered enter/exit (the synchronized-method pattern): if the
        // <clinit> body throws, throwException()'s unwind releases the class
        // monitor. With a plain monitorEnter the lock leaked on a throwing
        // clinit and every later thread touching the class deadlocked in
        // monitorEnter (observed on CI as the EDT wedged initializing
        // BufferedOutputStream while logging the very exception that leaked it).
        b.append("monitorEnterBlock(threadStateData, (JAVA_OBJECT)&class__");

        b.append(clsName);
        b.append(");\n    if(class__");
        b.append(clsName);
        b.append(".initialized) {\n        monitorExitBlock(threadStateData, (JAVA_OBJECT)&class__");
        b.append(clsName);
        b.append(");\n        return;\n    }\n\n");
        
        if(arrayTypes.contains("1_" + clsName) || arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("class_array1__");
            b.append(clsName);
            b.append(".vtable = initVtableForInterface();\n    ");
        }

        if( arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("class_array2__");
            b.append(clsName);
            b.append(".vtable = initVtableForInterface();\n    ");
        }
        if(arrayTypes.contains("3_" + clsName)) {
            b.append("class_array3__");
            b.append(clsName);
            b.append(".vtable = initVtableForInterface();\n    ");
        }
        
        // create the vtable
        b.append("    class__");
        b.append(clsName);
        b.append(".vtable = malloc(sizeof(void*) *");
        b.append(virtualMethodList.size());
        b.append(");\n");
        if(isInterface) {
            // special case, java_lang_object calls on interfaces should
            // act like standard virtual method calls
            b.append("    class__");
            b.append(clsName);
            b.append(".vtable = initVtableForInterface();\n");
            b.append("    classToInterfaceMap_");
            b.append(clsName);
            // calloc, not malloc: rows are filled only for classes that implement
            // this interface, so an id that does not read as a registered row must
            // read as NULL rather than as whatever the allocator last left there.
            b.append(" = calloc(cn1_array_start_offset, sizeof(int*));\n");
            for(ByteCodeClass cls : allClasses) {
                if(!cls.isInterface) {
                    if(cls.doesImplement(this)) {
                        b.append("    classToInterfaceMap_");
                        b.append(clsName);
                        b.append("[cn1_class_id_");
                        b.append(cls.clsName);
                        // sizeof(int), not sizeof(int*): a row is an int[] of vtable
                        // slot indices -- the map is int**, so the ROW is what is
                        // int-sized. Asking for pointer size allocated twice what the
                        // row can ever hold, on every interface/implementor pair.
                        b.append("] = malloc(sizeof(int) * ");
                        b.append(getMethodCountIncludingBase());
                        b.append(");\n");
                        offset = 0;
                        for(BytecodeMethod m : virtualMethodList) {
                            if(!m.getClsName().equals("java_lang_Object")) {
                                b.append("    classToInterfaceMap_");
                                b.append(clsName);
                                b.append("[cn1_class_id_");
                                b.append(cls.clsName);
                                b.append("][");
                                b.append(offset);
                                b.append("] = ");
                                b.append(cls.virtualMethodList.indexOf(m));
                                b.append(";\n");
                                offset++;
                            }
                        }
                    }
                }
            }
        } else {
            b.append("    __INIT_VTABLE_");
            b.append(clsName);
            b.append("(threadStateData, class__");
            b.append(clsName);
            b.append(".vtable);\n");

        }
        b.append("    __atomic_store_n(&class__");
        b.append(clsName);
        // This flag means STARTED, not completed: the JLS requires a class whose
        // initialiser re-enters itself to proceed rather than deadlock, so it has
        // to be set before __CLINIT__ runs, and the check above the monitor is
        // that recursion guard. Nothing outside this function may treat it as
        // "safe to use the class" in the JLS sense -- a class under initialization
        // is not finished. The release is what the INLINE GUARDS acquire against:
        // they test this flag, and it is what publishes the vtable and the
        // classToInterfaceMap rows written just above. Guarding them on
        // __X_LOADED__ instead would also be correct about the vtable and would
        // additionally hold other threads until __CLINIT__ returned -- a strictly
        // later gate than master opens, which moved layout on four native ports
        // and is not what the visibility defect required.
        b.append(".initialized, JAVA_TRUE, __ATOMIC_RELEASE);\n");
        // init static fields and invoke the static initializer code block
        if(clInitMethod != null) {
            b.append("    ");
            b.append(clInitMethod);
            b.append("(threadStateData);\n");
        }
        b.append("monitorExitBlock(threadStateData, (JAVA_OBJECT)&class__");
        b.append(clsName);
        b.append(");\n");

        // RELEASE: pairs with the acquire on the fast path above, so everything
        // this initialiser wrote happens-before another thread's early return.
        b.append("__atomic_store_n(&__").append(clsName)
         .append("_LOADED__, 1, __ATOMIC_RELEASE);\n");

        b.append("}\n\n");

        // On-device-debug: emit the instance-field offset table for this
        // class. Wrapped in CN1_ON_DEVICE_DEBUG so release builds don't pay
        // the data or registration cost.
        if (BytecodeMethod.isOnDeviceDebug()) {
            appendOnDeviceDebugFieldTable(b);
            appendOnDeviceDebugInvokeThunks(b);
        }

        return b.toString();
    }

    /**
     * Emits a per-method shim that lets the debugger runtime call any
     * translated method generically. Each thunk unpacks an argument array
     * into the typed C parameters that the underlying function expects,
     * wraps the call in a catch-all try block so an uncaught Throwable
     * round-trips back as a value instead of unwinding past
     * suspendCurrent, and packs the return value into a uniform
     * {@code cn1_invoke_result}. A __attribute__((constructor)) at the
     * bottom registers each thunk with the global registry keyed by
     * methodOffset.
     */
    private void appendOnDeviceDebugInvokeThunks(StringBuilder b) {
        // Skip thunk generation for classes whose impl is hand-written
        // native code that's been allowed to fall out of sync with the
        // translator's calling convention. Without thunks the linker
        // dead-strips the C wrapper (since user code typically doesn't
        // call those wrappers); with thunks the wrapper is forced live
        // and the link fails on missing native impls.
        //
        // The skip list is intentionally narrow — only the packages
        // where this is known to bite. java.lang / java.util etc. are
        // fine and worth keeping (jdb leans on Object.toString for
        // "print" output, and we want lists/strings to round-trip too).
        if (clsName.startsWith("java_io_") || clsName.startsWith("java_net_")
                || clsName.startsWith("java_nio_")
                || clsName.startsWith("com_codename1_impl_")) {
            return;
        }
        // We emit a constructor PER class that registers all of that
        // class's invoke thunks in one go. The thunks themselves are
        // file-static so they don't leak symbols.
        List<BytecodeMethod> eligible = new ArrayList<>();
        for (BytecodeMethod m : methods) {
            if (m.isEliminated()) continue;
            if (m.isConstructor()) continue;
            String name = m.getMethodName();
            if ("__CLINIT__".equals(name) || "<clinit>".equals(name)) continue;
            // Abstract methods have no body to call. Native methods are
            // fine — their impls live in nativeMethods.m / iOS port
            // hand-written code, with the same C name our thunk calls.
            // We rely on the class-prefix filter above to skip whole
            // packages whose native sidecar isn't linked in this build
            // (java.io / java.net / java.nio / com.codename1.impl).
            if (m.isAbstract()) continue;
            eligible.add(m);
        }
        if (eligible.isEmpty()) return;

        b.append("\n#ifdef CN1_ON_DEVICE_DEBUG\n");
        b.append("#include <setjmp.h>\n");
        b.append("#include <string.h>\n");
        for (BytecodeMethod m : eligible) {
            m.appendOnDeviceDebugInvokeThunk(clsName, b);
        }
        b.append("__attribute__((constructor)) static void __cn1_dbg_register_invoke_thunks_")
                .append(clsName).append("(void) {\n");
        for (BytecodeMethod m : eligible) {
            b.append("    cn1_debugger_register_invoke_thunk(")
              .append(m.getMethodOffset())
              .append(", &__cn1_dbg_invoke_").append(m.getMethodOffset()).append(");\n");
        }
        b.append("}\n");
        b.append("#endif // CN1_ON_DEVICE_DEBUG\n");
    }

    private void appendOnDeviceDebugFieldTable(StringBuilder b) {
        // Inherit-through layout-order list: parents first, this class last.
        // Matches addFields() so offsetof() lines up with the actual struct.
        List<ByteCodeField> instance = getAllInstanceFieldsInLayoutOrder();
        // Drop any field whose declaring class isn't itself in the
        // translation unit. We can't take offsetof of a field whose struct
        // we don't have, but this should never happen — translator pulls in
        // parents transitively.
        b.append("\n#ifdef CN1_ON_DEVICE_DEBUG\n");
        b.append("#import \"cn1_debugger.h\"\n");
        b.append("static const cn1_field_entry __cn1_dbg_fields_").append(clsName).append("[] = {\n");
        for (ByteCodeField bf : instance) {
            String declCls = bf.getClsName().replace('/', '_').replace('$', '_');
            int fid = Parser.getOrAssignFieldId(declCls, bf.getFieldName());
            char tc = onDeviceDebugTypeCharFor(bf);
            // A conditionally-declared field has no offsetof on a target that does
            // not declare it; see targetGuardFor.
            String guard = targetGuardFor(declCls, bf.getFieldName());
            if(guard != null) {
                b.append("#if ").append(guard).append("\n");
            }
            b.append("    { ").append(fid)
              .append(", (int)offsetof(struct obj__").append(clsName)
              .append(", ").append(declCls).append("_").append(bf.getFieldName())
              .append("), '").append(tc).append("', \"")
              .append(bf.getFieldName()).append("\" },\n");
            if(guard != null) {
                b.append("#endif\n");
            }
        }
        b.append("};\n");
        b.append("__attribute__((constructor)) static void __cn1_dbg_register_").append(clsName).append("(void) {\n");
        b.append("    cn1_debugger_register_fields(cn1_class_id_").append(clsName).append(",\n");
        b.append("            __cn1_dbg_fields_").append(clsName).append(",\n");
        b.append("            (int)(sizeof(__cn1_dbg_fields_").append(clsName).append(") / sizeof(cn1_field_entry)));\n");
        // Publish the clazz address too, so the runtime can tell a genuine
        // object header from a stale or fabricated pointer by an exact
        // identity check rather than a heuristic. Every generated class runs
        // this constructor, so the registry is complete before main().
        b.append("    cn1_debugger_register_class(cn1_class_id_").append(clsName)
          .append(", &class__").append(clsName).append(");\n");
        b.append("}\n");
        b.append("#endif // CN1_ON_DEVICE_DEBUG\n");
    }

    private static char onDeviceDebugTypeCharFor(ByteCodeField bf) {
        // Object and arrays — both stored as JAVA_OBJECT in the C struct.
        if (bf.isObjectType()) return 'L';
        String d = bf.getRuntimeDescriptor();
        if (d != null && d.length() == 1) return d.charAt(0);
        return 'L';
    }

    /**
     * Whether this class implements the given interface, directly or through a
     * superclass or a super-interface. Package visible so the devirtualizer can build
     * an interface-to-implementors index; it used to be private because only the
     * interface-map emitter asked.
     *
     * @param interfaceObj the interface to test
     * @return true when this class is assignable to it
     */
    boolean doesImplement(ByteCodeClass interfaceObj) {
        if(baseInterfacesObject != null) {
            if(baseInterfacesObject.contains(interfaceObj)) {
                return true;
            }
            // check if one of the interfaces we implement derives from this interface
            for(ByteCodeClass i : baseInterfacesObject) {
                if(i.getBaseClassObject() == interfaceObj || i.doesImplement(interfaceObj)) {
                    return true;
                }
            }
        }
        if(baseClassObject != null) {
            return baseClassObject.doesImplement(interfaceObj);
        }
        return false;
    }

    private boolean isDefaultInterfaceMethod(BytecodeMethod method, List<ByteCodeClass> allClasses) {
        ByteCodeClass owner = findClass(method.getClsName(), allClasses);
        return owner != null && owner.isInterface && !method.isAbstract();
    }
    
    private void appendSuperStub(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass base) {
        // append super stub
        BytecodeMethod.setAcceptStaticOnEquals(true);
        for(BytecodeMethod m : base.methods) {
            if(!m.isPrivate() && !bm.contains(m)) {
                m.appendSuperCall(b, clsName);
                bm.add(m);
            }
        }
        if(base.baseClassObject != null) {
            appendSuperStub(b, bm, base.baseClassObject);
        }
        BytecodeMethod.setAcceptStaticOnEquals(false);
    }

    private boolean hasMethodInBaseClass(BytecodeMethod method) {
        if(baseClassObject == null) {
            return false;
        }
        if(baseClassObject.methods.contains(method)) {
            return true;
        }
        return baseClassObject.hasMethodInBaseClass(method);
    }

    private void appendDefaultInterfaceStubs(StringBuilder b, List<BytecodeMethod> bm) {
        if(baseInterfacesObject == null) {
            return;
        }
        BytecodeMethod.setAcceptStaticOnEquals(true);
        for(ByteCodeClass baseInterface : baseInterfacesObject) {
            appendDefaultInterfaceStubs(b, bm, baseInterface);
        }
        BytecodeMethod.setAcceptStaticOnEquals(false);
    }

    private void appendDefaultInterfaceStubs(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass baseInterface) {
        if(baseInterface == null) {
            return;
        }
        if(baseClassObject != null && baseClassObject.doesImplement(baseInterface)) {
            return;
        }
        for(BytecodeMethod m : baseInterface.methods) {
            if(m.isAbstract() || m.isStatic() || m.isPrivate()) {
                continue;
            }
            if(!bm.contains(m) && !hasMethodInBaseClass(m)) {
                m.appendSuperCall(b, clsName);
                bm.add(m);
            }
        }
        if(baseInterface.baseInterfacesObject != null) {
            for(ByteCodeClass parentInterface : baseInterface.baseInterfacesObject) {
                appendDefaultInterfaceStubs(b, bm, parentInterface);
            }
        }
    }
    
    private void appendSuperStubHeader(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass base) {
        BytecodeMethod.setAcceptStaticOnEquals(true);
        // append super stub
        for(BytecodeMethod m : base.methods) {
            if(!m.isPrivate() && !bm.contains(m)) {
                m.appendMethodHeader(b, clsName);
                bm.add(m);
            }
        }
        if(base.baseClassObject != null) {
            appendSuperStubHeader(b, bm, base.baseClassObject);
        }
        BytecodeMethod.setAcceptStaticOnEquals(false);
    }

    private void appendDefaultInterfaceStubHeaders(StringBuilder b, List<BytecodeMethod> bm) {
        if(baseInterfacesObject == null) {
            return;
        }
        BytecodeMethod.setAcceptStaticOnEquals(true);
        for(ByteCodeClass baseInterface : baseInterfacesObject) {
            appendDefaultInterfaceStubHeaders(b, bm, baseInterface);
        }
        BytecodeMethod.setAcceptStaticOnEquals(false);
    }

    private void appendDefaultInterfaceStubHeaders(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass baseInterface) {
        if(baseInterface == null) {
            return;
        }
        if(baseClassObject != null && baseClassObject.doesImplement(baseInterface)) {
            return;
        }
        for(BytecodeMethod m : baseInterface.methods) {
            if(m.isAbstract() || m.isStatic() || m.isPrivate()) {
                continue;
            }
            if(!bm.contains(m) && !hasMethodInBaseClass(m)) {
                m.appendMethodHeader(b, clsName);
                bm.add(m);
            }
        }
        if(baseInterface.baseInterfacesObject != null) {
            for(ByteCodeClass parentInterface : baseInterface.baseInterfacesObject) {
                appendDefaultInterfaceStubHeaders(b, bm, parentInterface);
            }
        }
    }
    
    private void buildInstanceFieldList(List<ByteCodeField> fieldList) {
        buildInstanceFieldList(fieldList, true);
    }
    
    private void buildInstanceFieldList(List<ByteCodeField> fieldList, boolean includePrivateFields) {
        for(ByteCodeField bf : fields) {
            // We don't include private fields from parent classes.
            if (!includePrivateFields && bf.isPrivate()) continue;
            if(!bf.isStaticField() && !fieldList.contains(bf)) {
                fieldList.add(bf);
            } 
        }
        if(baseClassObject != null) {
            baseClassObject.buildInstanceFieldList(fieldList, false);
        }        
    } 

    private List<ByteCodeField> buildStaticFieldList(List<ByteCodeField> fieldList) {
        return buildStaticFieldList(fieldList, true);
    }
    
    private List<ByteCodeField> buildStaticFieldList(List<ByteCodeField> fieldList, boolean includePrivateFields) {
        if (fields != null) {
            for(ByteCodeField bf : fields) {
                // We don't include private fields from parent classes.
                if (!includePrivateFields && bf.isPrivate()) continue;
                if(bf.isStaticField() && !fieldList.contains(bf)) {
                    fieldList.add(bf);
                } 
            }
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass baseInterface : baseInterfacesObject) {
                baseInterface.buildStaticFieldList(fieldList, false);
            }
        }
        if(baseClassObject != null) {
            baseClassObject.buildStaticFieldList(fieldList, false);
        }        
        return fieldList;
    } 
    
    /**
     * A per-target preprocessor condition for a field the generated struct should
     * only DECLARE on some targets, or null when the field is unconditional.
     *
     * <p>There is exactly one today. {@code java.lang.String.nsString} caches a
     * retained NSString peer so a string that crosses into Objective-C does not
     * have to be converted twice. Every read and write of it is already inside
     * {@code #if defined(__APPLE__) && defined(__OBJC__)} -- so on the clean C
     * target, native Windows, native Linux and the JavaScript port it is eight
     * bytes per String that the binary it is compiled into cannot even read.
     * Measured, that is 8 x 412,859 live Strings = 3.15MB of the 168.97MB peak
     * heap on the self-hosting corpus.
     *
     * <p>Declaring it conditionally rather than deleting it keeps iOS behaviour
     * bit-for-bit identical: the peer cache is still a plain field load there.
     * The precedent is DEBUG_GC_VARIABLES, which already varies the object header
     * between builds.
     *
     * <p>EVERY place that reproduces the struct layout must ask this, or it will
     * disagree with the struct. There are two: {@link #addFields} emits the
     * declaration, and {@link #appendOnDeviceDebugFieldTable} takes offsetof of
     * it -- an unguarded entry there fails to compile the moment someone builds
     * CN1_ON_DEVICE_DEBUG for a non-Apple target.
     */
    public static String targetGuardFor(String mangledOwner, String fieldName) {
        if ("java_lang_String".equals(mangledOwner) && "nsString".equals(fieldName)) {
            return "defined(__APPLE__) && defined(__OBJC__)";
        }
        return null;
    }

    // Largest first, so a class's own fields leave no alignment holes between them.
    // Declaration order interleaved 2-byte shorts with 8-byte references and padded each
    // gap: measured on the self-hosting corpus, ASM's Label was 104 bytes (a 112-byte
    // BiBOP slot) where the same fields pack into 96. Only a class's OWN fields move --
    // the inherited prefix stays in the parent's order, which is what lets a pointer to
    // this struct be read as a pointer to its parent's. Every access is by field name
    // (and the offset table is taken with offsetof), so nothing depends on the order.
    // Stable, so fields of one size keep their declaration order.
    private static final Comparator<ByteCodeField> WIDEST_FIRST = new Comparator<ByteCodeField>() {
        public int compare(ByteCodeField a, ByteCodeField c) {
            return fieldStorageBytes(c) - fieldStorageBytes(a);
        }
    };

    private static int fieldStorageBytes(ByteCodeField bf) {
        String d = bf.getCDefinition();
        if(d.endsWith("JAVA_BOOLEAN") || d.endsWith("JAVA_BYTE")) return 1;
        if(d.endsWith("JAVA_SHORT") || d.endsWith("JAVA_CHAR")) return 2;
        if(d.endsWith("JAVA_INT") || d.endsWith("JAVA_FLOAT")) return 4;
        return 8;
    }

    private boolean ancestorsDeclareInstanceFields() {
        for (ByteCodeClass c = baseClassObject; c != null; c = c.baseClassObject) {
            for (ByteCodeField bf : c.fields) {
                if (!bf.isStaticField()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addFields(StringBuilder b) {
        if(baseClassObject != null) {
            baseClassObject.addFields(b);
        }
        List<ByteCodeField> ordered = new ArrayList<ByteCodeField>(fields);
        Collections.sort(ordered, WIDEST_FIRST);
        // The header is 4 bytes (CN1_OBJ_HEADER_FIELDS) in an 8-aligned struct, so offset 4
        // is free for fields of up to 4 bytes -- but only the FIRST class in the hierarchy
        // to declare instance fields can use it: a subclass must keep its parent's layout
        // as a prefix. That class puts up to 4 bytes of its smallest-fitting fields first.
        if (!ancestorsDeclareInstanceFields()) {
            List<ByteCodeField> head = new ArrayList<ByteCodeField>();
            int gap = 4;
            for (ByteCodeField bf : ordered) {
                int n = fieldStorageBytes(bf);
                if (!bf.isStaticField() && n <= gap && targetGuardFor(clsName, bf.getFieldName()) == null) {
                    head.add(bf);
                    gap -= n;
                    if (gap == 0) {
                        break;
                    }
                }
            }
            ordered.removeAll(head);
            ordered.addAll(0, head);
        }
        for(ByteCodeField bf : ordered) {
            if(!bf.isStaticField()) {
                String guard = targetGuardFor(clsName, bf.getFieldName());
                if(guard != null) {
                    b.append("#if ").append(guard).append("\n");
                }
                b.append("    ");
                b.append(bf.getCInstanceStorageDefinition());
                b.append(" ");
                b.append(clsName);
                b.append("_");
                b.append(bf.getFieldName());
                b.append(";\n");
                if(guard != null) {
                    b.append("#endif\n");
                }
            } 
        }
    }
    
    public String generateCHeader() {
        StringBuilder b = new StringBuilder();
        b.append("#ifndef __");
        b.append(clsName.toUpperCase());
        b.append("__\n");
        b.append("#define __");
        b.append(clsName.toUpperCase());
        b.append("__\n\n");

        b.append("#include \"cn1_globals.h\"\n");
        
        for(String s : exportsClassesInterfaces) {
            /*
            if(s.startsWith("java_lang_annotation") || s.startsWith("java_lang_Deprecated") || 
                    s.startsWith("java_lang_Override") || s.startsWith("java_lang_SuppressWarnings")) {
                continue;
            }
            */
            //if (isAnnotation) {
            //    continue;
            //}
            b.append("#include \"");
            b.append(s);
            b.append(".h\"\n");
        }

        b.append("extern struct clazz class__");
        b.append(clsName);
        b.append(";\n");

        if(arrayTypes.contains("1_" + clsName) || arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("extern struct clazz class_array1__");
            b.append(clsName);
            b.append(";\n");
        }

        if(arrayTypes.contains("2_" + clsName) || arrayTypes.contains("3_" + clsName)) {
            b.append("extern struct clazz class_array2__");
            b.append(clsName);
            b.append(";\n");
        }

        if(arrayTypes.contains("3_" + clsName)) {
            b.append("extern struct clazz class_array3__");
            b.append(clsName);
            b.append(";\n");
        }

        if(!isInterface) {
            b.append("extern void __INIT_VTABLE_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, void** vtable);\n");
        }

        b.append("extern CN1_CLINIT_ATTR void __STATIC_INITIALIZER_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE);\n");
        b.append("extern void __FINALIZER_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToDelete);\n");

        b.append("extern void __GC_MARK_");
        b.append(clsName);
        b.append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT objToMark, JAVA_BOOLEAN force);\n");
        
        if(!isInterface && !isAbstract) {
            b.append("extern JAVA_OBJECT __NEW_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE);\n");

            if(hasDefaultConstructor()) {
                b.append("extern JAVA_OBJECT __NEW_INSTANCE_");
                b.append(clsName);
                b.append("(CODENAME_ONE_THREAD_STATE);\n");
            }
        }
        
        if (isEnum) {
            b.append("extern JAVA_OBJECT __VALUE_OF_").append(clsName).append("(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT value);\n");
        }
                
        if(arrayTypes.contains("1_" + clsName)) {
            b.append("extern JAVA_OBJECT __NEW_ARRAY_");
            b.append(clsName);
            b.append("(CODENAME_ONE_THREAD_STATE, JAVA_INT size);\n");
        }
        
        appendMethodsToHeader(b);
        if(isInterface) {
            List<BytecodeMethod> bm = new ArrayList<BytecodeMethod>(methods);
            appendInheritedInterfaceMethodHeaders(b, bm);
        }
        
        if(!isInterface) {
            // append super stub
            List<BytecodeMethod> bm = new ArrayList<BytecodeMethod>(methods);
            if(baseClassObject != null) {
                appendSuperStubHeader(b, bm, baseClassObject);
            }
            appendDefaultInterfaceStubHeaders(b, bm);
        }
        
        for(BytecodeMethod m : virtualMethodList) {
            if(m.isVirtualOverriden()) {
                b.append("#define virtual_");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getCMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append(" ");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getCMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append("\n");
            } else {
                m.appendVirtualMethodHeader(b, clsName);
            }
        }

        // static fields for the class
        for(ByteCodeField bf : staticFieldList) {
            if(bf.isStaticField()) {
                if(bf.getClsName().equals(clsName)) {
                    b.append("extern ");
                    b.append(bf.getCDefinition());
                    b.append(" get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("();\n");
                    if(!(bf.isFinal() && bf.getValue() != null && !writableFields.contains(bf.getFieldName()))) {
                        b.append("extern ");
                        b.append(bf.getCStorageDefinition());
                        b.append(" STATIC_FIELD_");
                        b.append(clsName);
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append(";\n");

                    b.append("extern void");
                    b.append(" set_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("(");
                    if (bf.isObjectType()) {
                        b.append("CODENAME_ONE_THREAD_STATE, ");
                    }
                    b.append(bf.getCDefinition());
                    b.append(" v);\n");
                    }
                } else {
                    b.append("#define get_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("() get_static_");
                    b.append(bf.getClsName());
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append("()\n");

                    b.append("#define set_static_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    if (bf.isObjectType()) {
                        b.append("(threadStateArgument, valueArgument) set_static_");
                        b.append(bf.getClsName());
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append("(threadStateArgument, valueArgument)\n");
                    } else {
                        b.append("(valueArgument) set_static_");
                        b.append(bf.getClsName());
                        b.append("_");
                        b.append(bf.getFieldName());
                        b.append("(valueArgument)\n");
                    }
                }
            }
        }

        for(ByteCodeField fld : fullFieldList) {
            String declGuard = targetGuardFor(fld.getClsName().replace('/', '_').replace('$', '_'),
                    fld.getFieldName());
            if(declGuard == null) {
                declGuard = targetGuardFor(clsName, fld.getFieldName());
            }
            if(declGuard != null) {
                b.append("#if ").append(declGuard).append("\n");
            }
            b.append(fld.getCDefinition());
            b.append(" get_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(JAVA_OBJECT t);\n");

            b.append("void set_field_");
            b.append(clsName);
            b.append("_");
            b.append(fld.getFieldName());
            b.append("(");
            b.append(fld.getCDefinition());
            b.append(" __cn1Val, JAVA_OBJECT __cn1T);\n");
            if(declGuard != null) {
                b.append("#endif\n");
            }
        }
        
        b.append("\n\n");

        // object struct contains instace field variables
        b.append("struct obj__");
        b.append(clsName);
        b.append(" {\n");
        // reference to the class, reference counter for the arc portion of the GC
        // and a mutex for synchronization code
        b.append("    CN1_OBJ_HEADER_FIELDS\n");

        
        addFields(b);
        if ("java_lang_StringBuilder".equals(clsName)) {
            // Small builders need neither a child array nor a malloc allocation.
            b.append("    unsigned char __cn1InlineStorage[16] __attribute__((aligned(16)));\n");
        }
        b.append("};\n\n");
                     
        
        b.append("\n\n#endif //__");
        b.append(clsName.toUpperCase());
        b.append("__\n");
        return b.toString();
    }

    private void appendMethodsToHeader(StringBuilder b) {        
        for(BytecodeMethod m : methods) {
            m.appendMethodHeader(b);
            
            /*if(!m.isForceVirtual() && m.isVirtualBlockedDueToFinal() && !m.isVirtualOverriden()) {
                b.append("#define virtual_");
                b.append(clsName);
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append(" ");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append("\n");
            }*/
        }
        
        /*if(baseClassObject != null) {
            baseClassObject.appendVirtualBlockedMethodsToHeader(b, clsName);
        }*/
    }

    /*private void appendVirtualBlockedMethodsToHeader(StringBuilder b, String clsName) {        
        for(BytecodeMethod m : methods) {
            if(m.isVirtualBlockedDueToFinal() && !m.isVirtualOverriden()) {
                b.append("#define virtual_");
                b.append(clsName);
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append(" ");
                b.append(m.getClsName());
                b.append("_");
                b.append(m.getMethodName());
                b.append("__");
                m.appendArgumentTypes(b);
                b.append("\n");
            }
        }
        if(baseClassObject != null) {
            baseClassObject.appendVirtualBlockedMethodsToHeader(b, clsName);
        }
    }*/

    private void appendInheritedInterfaceMethods(StringBuilder b, List<BytecodeMethod> bm) {
        if(baseInterfacesObject == null) {
            return;
        }
        for(ByteCodeClass baseInterface : baseInterfacesObject) {
            appendInheritedInterfaceMethods(b, bm, baseInterface);
        }
    }

    private void appendInheritedInterfaceMethods(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass baseInterface) {
        if(baseInterface == null) {
            return;
        }
        for(BytecodeMethod m : baseInterface.methods) {
            if(m.isStatic() || m.isPrivate()) {
                continue;
            }
            if(!bm.contains(m)) {
                m.appendInterfaceMethodC(b, clsName);
                bm.add(m);
            }
        }
        if(baseInterface.baseInterfacesObject != null) {
            for(ByteCodeClass parentInterface : baseInterface.baseInterfacesObject) {
                appendInheritedInterfaceMethods(b, bm, parentInterface);
            }
        }
    }

    private void appendInheritedInterfaceMethodHeaders(StringBuilder b, List<BytecodeMethod> bm) {
        if(baseInterfacesObject == null) {
            return;
        }
        for(ByteCodeClass baseInterface : baseInterfacesObject) {
            appendInheritedInterfaceMethodHeaders(b, bm, baseInterface);
        }
    }

    private void appendInheritedInterfaceMethodHeaders(StringBuilder b, List<BytecodeMethod> bm, ByteCodeClass baseInterface) {
        if(baseInterface == null) {
            return;
        }
        for(BytecodeMethod m : baseInterface.methods) {
            if(m.isStatic() || m.isPrivate()) {
                continue;
            }
            if(!bm.contains(m)) {
                m.appendMethodHeader(b, clsName);
                bm.add(m);
            }
        }
        if(baseInterface.baseInterfacesObject != null) {
            for(ByteCodeClass parentInterface : baseInterface.baseInterfacesObject) {
                appendInheritedInterfaceMethodHeaders(b, bm, parentInterface);
            }
        }
    }

    
    /**
     * @param baseClass the baseClass to set
     */
    public void setBaseClass(String baseClass) {
        this.baseClass = baseClass;
        if(baseClass != null) {
            String b = baseClass.replace('/', '_').replace('$', '_');
            if(!dependsClassesInterfaces.contains(b)) {
                dependsClassesInterfaces.add(b);
            }
            exportsClassesInterfaces.add(b);
        }
    }
    
    public void setBaseInterfaces(String[] interfaces) {
        baseInterfaces = Arrays.asList(interfaces);
        if(baseInterfaces != null) {
            for(String s : interfaces) {
                s = s.replace('/', '_').replace('$', '_');
                if(!dependsClassesInterfaces.contains(s)) {
                    dependsClassesInterfaces.add(s);
                }
                exportsClassesInterfaces.add(s);
            }
        }
    }

    /**
     * @return the clsName
     */
    public String getClsName() {
        return clsName;
    }

    /**
     * @return the baseClassObject
     */
    public ByteCodeClass getBaseClassObject() {
        return baseClassObject;
    }

    /**
     * @param baseClassObject the baseClassObject to set
     */
    public void setBaseClassObject(ByteCodeClass baseClassObject) {
        this.baseClassObject = baseClassObject;
    }

    /**
     * @return the baseInterfacesObject
     */
    public List<ByteCodeClass> getBaseInterfacesObject() {
        return baseInterfacesObject;
    }

    /**
     * @param baseInterfacesObject the baseInterfacesObject to set
     */
    public void setBaseInterfacesObject(List<ByteCodeClass> baseInterfacesObject) {
        this.baseInterfacesObject = baseInterfacesObject;
    }

    /**
     * @return the baseInterfaces
     */
    public List<String> getBaseInterfaces() {
        return baseInterfaces;
    }
    
    public void fillVirtualMethodTable(List<BytecodeMethod> virtualMethods) {
        fillVirtualMethodTable(virtualMethods, true);
    }
    
    private void fillVirtualMethodTable(List<BytecodeMethod> virtualMethods, boolean replace) {
        if(baseClassObject != null) {
            baseClassObject.fillVirtualMethodTable(virtualMethods, true);
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass bc : baseInterfacesObject) {
                bc.fillVirtualMethodTable(virtualMethods, false);
            }
        }
        for(BytecodeMethod bm : methods) {
            if (bm.isEliminated()) continue;
            if(bm.canBeVirtual()) {
                int offset = virtualMethods.indexOf(bm);
                if(offset < 0) {
                    virtualMethods.add(bm);
                    if(isInterface) {
                        bm.setForceVirtual(true);
                    }
                } else {
                    if(replace || (isInterface && (isInterfaceInHierarchy(virtualMethods.get(offset).getClsName()) ||
                            "java_lang_Object".equals(virtualMethods.get(offset).getClsName())))) {
                        virtualMethods.set(offset, bm);
                        if(isInterface) {
                            bm.setForceVirtual(true);
                        }
                    }
                }
            } else {
                
            } 
        }
    }
    
    /*private boolean isMethodIn(ByteCodeClass bc, BytecodeMethod bm) {
        if(methods.contains(bm)) {
            return true;
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass cc : baseInterfacesObject) {
                if(isMethodIn(cc, bm)) {
                    return true;
                }
            }
        }
        if(baseClassObject != null) {
            return isMethodIn(baseClassObject, bm);
        }
        return false;
    }*/

    /**
     * @return the baseClass
     */
    public String getBaseClass() {
        return baseClass;
    }

    public String getConcreteClass() {
        return concreteClass;
    }

    public void setConcreteClass(String concreteClass) {
        this.concreteClass = concreteClass;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * @return the classOffset
     */
    public int getClassOffset() {
        return classOffset;
    }

    /**
     * @param classOffset the classOffset to set
     */
    public void setClassOffset(int classOffset) {
        this.classOffset = classOffset;
    }
    
    public int updateMethodOffsets(int initial) {
        for(BytecodeMethod m : methods) {
            m.setMethodOffset(initial);
            initial++;
        }
        return initial;
    }
    
    public int getMethodCountIncludingBase() {
        int size = methods.size();
        if(baseClassObject != null) {
            size += baseClassObject.getMethodCountIncludingBase();
        }
        if(baseInterfacesObject != null) {
            for(ByteCodeClass bo : baseInterfacesObject) {
                size += bo.getMethodCountIncludingBase();
            }
        }
        return size;
    }
    
    /// EAGER INITIALIZATION. A class or interface whose initialization runs no code a
    /// program can observe the timing of is initialized once at startup, before any
    /// Java executes, and every guard on it -- `if(!class__X.initialized)
    /// __STATIC_INITIALIZER_X(...)` at each static method entry and interface thunk,
    /// and the acquire in each get_static_/set_static_ accessor -- is omitted, because
    /// from then on it is always false. Two kinds qualify:
    ///
    /// - A class with NO <clinit>. Its __STATIC_INITIALIZER only mallocs and fills its
    ///   vtable, or for an interface its classToInterfaceMap rows. These run first, in
    ///   cn1EagerInitClasses, at the very start of initConstantPool.
    /// - A class whose <clinit> is PURE (see computePureClinits): it reads and writes
    ///   only its own static fields, constants, string literals and arrays it creates,
    ///   and calls nothing. Its effects are confined to its own statics, which nothing
    ///   can read before the first use of the class, so when it runs is unobservable.
    ///   These run in cn1EagerInitPureClasses, once the constant pool is published
    ///   (string literals are built from it on first use) and still before any Java.
    ///
    /// Note what "initialization" means here: ParparVM initializes each class on the
    /// first use of ITS OWN statics, static methods or allocation -- the generated
    /// initializer never runs a superclass's -- so a class's eligibility does not
    /// depend on its supertypes.
    ///
    /// ALLOCATION SITES KEEP THE GUARD: dropping it there measured ~11% slower on
    /// objectAllocation across four code layouts -- see the note on CN1_FAST_NEW in
    /// cn1_globals.h.
    ///
    /// The one behaviour a pure initializer can change by running early: it has no
    /// inputs, so it either always completes or always throws (an index or size fault
    /// in its own table code). One that always throws makes the class unusable in every
    /// run; running it eagerly moves that failure from the first use to startup.
    public boolean isEagerInitEligible() {
        if (isEliminated() || !initReferenced) {
            return false;
        }
        return !hasClinit() || pureClinit;
    }

    /// Whether surviving Java code can trigger this class's initialization at all.
    private boolean initReferenced;

    /// ONLY A CLASS THE PROGRAM CAN INITIALIZE IS INITIALIZED EAGERLY.
    ///
    /// The eager lists call every listed class's __STATIC_INITIALIZER unconditionally, and
    /// for a class with no <clinit> that initializer fills its vtable -- a reference to
    /// every virtual method it has. Lazily, an initializer is called only from the code
    /// that uses the class, so for a class nothing uses there is no caller at all and the
    /// linker's dead-stripping removes it together with everything only it reached. Listing
    /// every class that merely survived the translator's cull took that away: the Bench
    /// binary kept 481 more functions (87 static initializers, their mark functions,
    /// constructors and setters, java.io and java.net it never touches) and its code grew
    /// 64%, a Linux gallery app 38%.
    ///
    /// So a class is listed only if some surviving method names it where the JVM would
    /// initialize it: NEW, a static field access, a static call, or an interface call (an
    /// interface's initializer builds the tables its thunks dispatch through). A class
    /// left off keeps its guards and initializes on first use, exactly as before eager
    /// initialization existed -- so missing one here costs a guard, never correctness. That
    /// is also why the scan need not be exact: a static reached only through a subclass
    /// name, or a class only C code allocates, simply stays lazy.
    ///
    /// Computed before computePureClinits, which reads eligibility: a pure <clinit> may
    /// allocate only eager classes, and the NEW it does that with marks the class it
    /// allocates, so the two agree.
    static void computeInitReferences(List<ByteCodeClass> classes) {
        for (ByteCodeClass c : classes) {
            c.initReferenced = false;
        }
        for (ByteCodeClass c : classes) {
            for (BytecodeMethod m : c.methods) {
                for (Instruction i : m.getInstructions()) {
                    String owner = null;
                    int op = i.getOpcode();
                    if (i instanceof Field) {
                        if (op == Opcodes.GETSTATIC || op == Opcodes.PUTSTATIC) {
                            owner = ((Field) i).getOwner();
                        }
                    } else if (i instanceof TypeInstruction) {
                        if (op == Opcodes.NEW) {
                            owner = ((TypeInstruction) i).getTypeName();
                        }
                    } else if (i instanceof Invoke) {
                        if (op == Opcodes.INVOKESTATIC || op == Opcodes.INVOKEINTERFACE) {
                            owner = ((Invoke) i).getOwner();
                        }
                    }
                    if (owner != null) {
                        ByteCodeClass oc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
                        if (oc != null) {
                            oc.initReferenced = true;
                        }
                    }
                }
            }
        }
    }

    public boolean hasClinit() {
        for (BytecodeMethod m : methods) {
            if (m.getMethodName().indexOf("_CLINIT_") > -1) {
                return true;
            }
        }
        return false;
    }

    private boolean pureClinit;

    /// Why each rejected <clinit> was rejected, keyed by its first blocking
    /// instruction, for the census line Parser prints.
    static final Map<String, Integer> PURE_CLINIT_BLOCKERS = new TreeMap<String, Integer>();
    static int pureClinitCount;
    static int clinitCount;

    /// Decides pureClinit for every class, on the RAW bytecode: before optimize() or
    /// any fusion pass has replaced instructions with composite ones, which are
    /// rejected here by construction because only raw instruction classes are
    /// accepted. Runs once, after the dead-class cull and before code generation.
    ///
    /// A FIXPOINT, started pessimistic. A pure <clinit> may allocate an instance of a
    /// class that is itself eager (`static final Boolean TRUE = new Boolean(true)`,
    /// every enum constant), so one class's answer can depend on another's; iterating
    /// up from "nothing is pure" means two classes can never justify each other.
    static void computePureClinits(List<ByteCodeClass> classes) {
        PURE_CLINIT_BLOCKERS.clear();
        pureClinitCount = 0;
        clinitCount = 0;
        for (ByteCodeClass c : classes) {
            c.pureClinit = false;
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            PURE_CLINIT_BLOCKERS.clear();
            pureClinitCount = 0;
            clinitCount = 0;
            for (ByteCodeClass c : classes) {
                BytecodeMethod clinit = c.clinitMethod();
                if (clinit == null) {
                    continue;
                }
                clinitCount++;
                if (c.pureClinit) {
                    pureClinitCount++;
                    continue;
                }
                // IDENTITY, not equals: BytecodeMethod.equals compares name and signature only,
                // so an enum's E.<init>(String,int) and the Enum.<init>(String,int) it
                // calls are "equal", and a HashMap reads the in-progress subclass
                // constructor as recursion.
                String blocker = c.pureBodyBlocker(clinit, false, new java.util.IdentityHashMap<BytecodeMethod, String>());
                if (blocker == null) {
                    c.pureClinit = true;
                    pureClinitCount++;
                    changed = true;
                } else {
                    Integer n = PURE_CLINIT_BLOCKERS.get(blocker);
                    PURE_CLINIT_BLOCKERS.put(blocker, n == null ? 1 : n + 1);
                }
            }
        }
    }

    private BytecodeMethod clinitMethod() {
        for (BytecodeMethod m : methods) {
            if (m.getMethodName().indexOf("_CLINIT_") > -1) {
                return m;
            }
        }
        return null;
    }

    private BytecodeMethod findMethod(String name, String desc) {
        for (BytecodeMethod m : methods) {
            if (m.getMethodName().equals(name) && desc.equals(m.getSignature())) {
                return m;
            }
        }
        return null;
    }

    /// null when every instruction of `m` is allowed, else why not. `ctor` selects
    /// the constructor rules: a constructor may touch fields but no statics at all,
    /// since ANY static it reached would be another class's initialization or a
    /// write the <clinit> did not make. `memo` holds each method's answer for this
    /// query -- an enum's <clinit> calls the same constructor once per constant --
    /// and IN_PROGRESS for a method still being examined, so recursion is rejected
    /// rather than assumed pure.
    private String pureBodyBlocker(BytecodeMethod m, boolean ctor, java.util.Map<BytecodeMethod, String> memo) {
        if (memo.containsKey(m)) {
            String known = memo.get(m);
            return IN_PROGRESS.equals(known) ? "recursion" : known;
        }
        // No bytecode is not "does nothing": a native or abstract body is code this
        // analysis cannot see.
        if (m.isNative() || m.isAbstract() || m.isSynchronizedMethod()) {
            memo.put(m, "native-abstract-or-synchronized");
            return "native-abstract-or-synchronized";
        }
        memo.put(m, IN_PROGRESS);
        String result = null;
        for (Instruction i : m.getInstructions()) {
            result = pureClinitBlocker(i, ctor, memo);
            if (result != null) {
                break;
            }
        }
        memo.put(m, result);
        return result;
    }

    // Compared by value: no blocker name contains parentheses, so this cannot collide.
    private static final String IN_PROGRESS = "(in-progress)";

    /// null when the instruction is allowed in a pure <clinit> (or, with `ctor`, in a
    /// constructor it calls), else a short name for why not. A WHITELIST: an
    /// instruction class not named here is rejected, so a new instruction type cannot
    /// silently become "pure".
    ///
    /// Everything such code can reach is an object it created itself or an immutable
    /// literal, which is why field access is allowed: nothing else can observe it.
    private String pureClinitBlocker(Instruction i, boolean ctor, java.util.Map<BytecodeMethod, String> visiting) {
        if (i instanceof LabelInstruction || i instanceof LineNumber || i instanceof LocalVariable
                || i instanceof VarOp || i instanceof IInc || i instanceof Jump
                || i instanceof SwitchInstruction || i instanceof MultiArray) {
            return null;
        }
        if (i instanceof Ldc) {
            Object v = ((Ldc) i).getValue();
            // A class literal materializes a Class object; anything else (a method
            // handle, a condy) is not a plain constant.
            return (v instanceof Number || v instanceof String) ? null : "ldc-nonconstant";
        }
        if (i instanceof Field) {
            Field f = (Field) i;
            int op = f.getOpcode();
            if (op == Opcodes.GETFIELD || op == Opcodes.PUTFIELD) {
                return null;
            }
            if (ctor) {
                return "static-in-constructor";
            }
            // Another class's static would initialize THAT class -- an observable,
            // order-dependent effect.
            return getOriginalClassName().equals(f.getOwner()) ? null : "foreign-static";
        }
        if (i instanceof TypeInstruction) {
            int op = i.getOpcode();
            // ANEWARRAY creates an array; it does not initialize the element class.
            if (op == Opcodes.ANEWARRAY) {
                return null;
            }
            if (op == Opcodes.NEW) {
                // Allocation initializes the class, so it must be one whose
                // initialization is itself unobservable. Its constructor is checked
                // at the INVOKESPECIAL that follows.
                String t = ((TypeInstruction) i).getTypeName();
                if (t.equals(getOriginalClassName())) {
                    return null;
                }
                ByteCodeClass tc = Parser.getClassObject(t.replace('/', '_').replace('$', '_'));
                return (tc != null && tc.isEagerInitEligible()) ? null : "new-noneager";
            }
            return "type-" + op;
        }
        if (i instanceof Invoke) {
            Invoke inv = (Invoke) i;
            String owner = inv.getOwner();
            if (inv.getOpcode() == Opcodes.INVOKESPECIAL && inv.getName().equals("<init>")) {
                if (owner.equals("java/lang/Object")) {
                    return null;
                }
                ByteCodeClass oc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
                BytecodeMethod target = oc == null ? null : oc.findMethod("__INIT__", inv.getDesc());
                if (target == null) {
                    return "ctor-unresolved";
                }
                String inner = oc.pureBodyBlocker(target, true, visiting);
                return inner == null ? null : (inner.startsWith("ctor:") ? inner : "ctor:" + inner);
            }
            // The class's own static helpers -- javac compiles an enum's $VALUES
            // initializer into a synthetic private static $values().
            if (!ctor && inv.getOpcode() == Opcodes.INVOKESTATIC && owner.equals(getOriginalClassName())) {
                BytecodeMethod target = findMethod(inv.getName(), inv.getDesc());
                if (target == null) {
                    return "static-unresolved";
                }
                String inner = pureBodyBlocker(target, false, visiting);
                return inner == null ? null : (inner.startsWith("static:") ? inner : "static:" + inner);
            }
            return "invoke";
        }
        if (i instanceof BasicInstruction) {
            int op = i.getOpcode();
            if (op == Opcodes.NOP || (op >= Opcodes.ACONST_NULL && op <= Opcodes.SIPUSH)
                    || (op >= Opcodes.IALOAD && op <= Opcodes.SALOAD)
                    || (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE)
                    || (op >= Opcodes.POP && op <= Opcodes.LXOR)
                    || (op >= Opcodes.I2L && op <= Opcodes.DCMPG)
                    || (op >= Opcodes.IRETURN && op <= Opcodes.RETURN)
                    || op == Opcodes.NEWARRAY || op == Opcodes.ARRAYLENGTH) {
                return null;
            }
            return "op-" + op;
        }
        return i.getClass().getSimpleName();
    }

    /// Whether the class-init guard for the (mangled) class name can be omitted.
    public static boolean eagerInit(String mangledName) {
        ByteCodeClass c = Parser.getClassObject(mangledName);
        return c != null && c.isEagerInitEligible();
    }

    public List<BytecodeMethod> getMethods() {
        return methods;
    }

    public List<ByteCodeField> getFields() {
        return fields;
    }

    /**
     * Walks the inheritance chain and collects every instance field this class
     * physically stores in its C struct (in declaration order, parents first
     * to match {@link #addFields}). Used by the on-device-debug sidecar so the
     * proxy can ask the device for inherited fields by their declaring-class
     * fieldId without a JDWP-level type walk.
     */
    public List<ByteCodeField> getAllInstanceFieldsInLayoutOrder() {
        List<ByteCodeField> out = new ArrayList<>();
        collectInstanceFieldsInLayoutOrder(out);
        return out;
    }

    private void collectInstanceFieldsInLayoutOrder(List<ByteCodeField> out) {
        if (baseClassObject != null) {
            baseClassObject.collectInstanceFieldsInLayoutOrder(out);
        }
        for (ByteCodeField bf : fields) {
            if (!bf.isStaticField()) {
                out.add(bf);
            }
        }
    }

    /**
     * @return the isInterface
     */
    public boolean isIsInterface() {
        return isInterface;
    }

    /**
     * @param isInterface the isInterface to set
     */
    public void setIsInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }
    
    public void setIsUnitTest(boolean isUnitTest) {
        this.isUnitTest = isUnitTest;
    }

    /**
     * @return the isAbstract
     */
    public boolean isIsAbstract() {
        return isAbstract;
    }

    /**
     * @param isAbstract the isAbstract to set
     */
    public void setIsAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }
    
    private void appendClassVFunctions(StringBuilder b) {
        // special case, class has no Class object within it so no real virtual functions
        b.append("JAVA_BOOLEAN virtual_java_lang_Class_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {\n" +
            "    return java_lang_Object_equals___java_lang_Object_R_boolean(threadStateData, __cn1ThisObject, __cn1Arg1);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_OBJECT virtual_java_lang_Class_getClass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    return java_lang_Object_getClass___R_java_lang_Class(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_INT virtual_java_lang_Class_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    return java_lang_Object_hashCode___R_int(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_notify__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    java_lang_Object_notify__(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_notifyAll__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    java_lang_Object_notifyAll__(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_OBJECT virtual_java_lang_Class_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    return java_lang_Object_toString___R_java_lang_String(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_wait__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {\n" +
            "    java_lang_Object_wait__(threadStateData, __cn1ThisObject);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_wait___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {\n" +
            "    java_lang_Object_wait___long(threadStateData, __cn1ThisObject, __cn1Arg1);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "JAVA_VOID virtual_java_lang_Class_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {\n" +
            "    java_lang_Object_wait___long_int(threadStateData, __cn1ThisObject, __cn1Arg1, __cn1Arg2);\n" +
            "}\n");
    }

    /**
     * @return the finalClass
     */
    private boolean stackAllocatable;

    /**
     * True when the class carries {@code @com.codename1.annotations.StackAllocate}:
     * its instances are allocated as method-scoped C structs instead of on the GC
     * heap. The developer guarantees instances never escape their creating frame.
     */
    public boolean isStackAllocatable() {
        return stackAllocatable;
    }

    public void setStackAllocatable(boolean stackAllocatable) {
        this.stackAllocatable = stackAllocatable;
    }

    private boolean fused;

    /**
     * True when the class carries {@code @com.codename1.annotations.Fused}: its
     * constructor-created primitive-array fields are encapsulated, so instances
     * are allocated together with those arrays as ONE heap block (single
     * allocation, single GC object; the arrays have no independent GC identity
     * and die with the owner). See {@link
     * com.codename1.tools.translator.bytecodes.FusedConstructor}.
     */
    public boolean isFused() {
        return fused;
    }

    public void setFused(boolean fused) {
        this.fused = fused;
    }

    public boolean isFinalClass() {
        return finalClass;
    }

    /**
     * @param finalClass the finalClass to set
     */
    public void setFinalClass(boolean finalClass) {
        this.finalClass = finalClass;
    }
    
    public void appendStaticFieldsExtern(StringBuilder b) {
        for(ByteCodeField bf : fields) {
            if(bf.isStaticField() && bf.isObjectType() && !bf.shouldRemoveFromHeapCollection()) {
                b.append("extern ");
                b.append(bf.getCStorageDefinition());
                b.append(" STATIC_FIELD_");
                b.append(clsName);
                b.append("_");
                b.append(bf.getFieldName());
                b.append(";\n");
            }
        }
    }

    private boolean isTrulyFinal(ByteCodeField bf) {
        if(bf.isFinal()) {
            if(bf.isObjectType()) {
                if(bf.getType() != null) {
                    return bf.getType().endsWith("String");
                }
            } else {
                return true;
            }
        }
        return false;
    }
    
    public void appendStaticFieldsMark(StringBuilder b) {
        for(ByteCodeField bf : fields) {
            if(bf.isStaticField() && bf.isObjectType() && !bf.shouldRemoveFromHeapCollection()) {
                b.append("    gcMarkObject(threadStateData, ");
                if (bf.isVolatile()) {
                    b.append("atomic_load_explicit(&STATIC_FIELD_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                    b.append(", memory_order_acquire)");
                } else {
                    b.append("STATIC_FIELD_");
                    b.append(clsName);
                    b.append("_");
                    b.append(bf.getFieldName());
                }
                b.append(", JAVA_TRUE);\n");
            }
        }
    }

    /**
     * 3-state variable to keep track of whether the class is used by native sources.
     * 3 states, because we need to know if it is unknown.
     */
    private UsedByNativeResult usedByNative = UsedByNativeResult.Unknown;


    /**
     * Sets usedByNative flag in this class.
     * @param usedByNative True if this class is used by native sources.
     */
    public void setUsedByNative(boolean usedByNative) {
        this.usedByNative = usedByNative ? UsedByNativeResult.Used : UsedByNativeResult.Unused;
    }

    /**
     * Enum to track possible values of {@link #usedByNative}.
     */
    public static enum UsedByNativeResult {
        /**
         * The class is used by native sources.
         */
        Used,
        /**
         * The class is not used by native sources.
         */
        Unused,
        /**
         * We don't yet know if this class is used by native sources.
         */
        Unknown;
    }


    /**
     * Check whether this class is used by native sources.
     * @return
     */
    public UsedByNativeResult getUsedByNative() {
       return usedByNative;
    }

    /**
     * Calculates whether this class is used in any of the native sources.
     * @param nativeSources The native sources to check.
     * @see #getUsedByNative() 
     * @see #setUsedByNative(boolean)
     */
    public void calcUsedByNative(String[] nativeSources) {
        for (BytecodeMethod m : methods) {
            if (usedByNative != UsedByNativeResult.Unknown) {
                return;
            }
            m.isMethodUsedByNative(nativeSources, this);
        }
    }

    boolean isUnitTest() {
        return isUnitTest;
    }

    void setIsEnum(boolean b) {
        this.isEnum = b;
    }

    private String getArrayClazz(int dim) {
        if((arrayTypes.contains(dim + "_" + clsName) )) {
            return "&class_array"+dim+"__"+clsName;
        } else {
            return "0";
        }
    }

    
}
