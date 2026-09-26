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

package com.codename1.tools.translator.bytecodes;

import com.codename1.tools.translator.ByteCodeClass;
import com.codename1.tools.translator.ByteCodeMethodArg;
import com.codename1.tools.translator.BytecodeMethod;
import com.codename1.tools.translator.Parser;
import com.codename1.tools.translator.SignatureSet;
import com.codename1.tools.translator.Util;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class Invoke extends Instruction {
    private String owner;
    // Set only on factories synthesized from LambdaMetafactory, never inferred
    // from a user-controlled class or method name.
    private BytecodeMethod lambdaSam;
    public BytecodeMethod getLambdaSam() { return lambdaSam; }
    public void setLambdaSam(BytecodeMethod method) { lambdaSam = method; }
    private String exactReceiverType;

    public void setClosedWorldReceiverTypes(java.util.Set<String> types) {
        // Consumers require one concrete type; mixed proofs need no retained set.
        exactReceiverType = types.size() == 1 ? types.iterator().next() : null;
    }

    /* Whether the proof that fixed the receiver's type ALSO shows it is not null.
     *
     * The null test on a devirtualized call exists because devirtualization removed
     * the dispatch that would have faulted. But the provenance that made the type
     * exact is frequently a NEW or a lambda's invokedynamic, and neither can produce
     * null -- so for those sites the test can never fire. A factory call or a field
     * read proves a type without proving non-nullness, and those keep it. */
    private boolean exactReceiverNonNull;

    public void setExactReceiverNonNull(boolean value) {
        exactReceiverNonNull = value;
    }

    public boolean isExactReceiverNonNull() {
        return exactReceiverNonNull;
    }

    public boolean hasExactReceiver(String type) {
        return exactReceiverType != null && exactReceiverType.equals(type);
    }

    /** Exact receiver provenance survives interface typing; resolve inherited declarations. */
    public String getProvenDirectOwner() {
        if ((opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE)
                || exactReceiverType == null) return null;
        ByteCodeClass concrete = Parser.getClassObject(exactReceiverType);
        ByteCodeClass declaring = ByteCodeClass.findConcreteDeclaringClass(concrete, name, desc);
        return declaring == null ? null : declaring.getClsName();
    }

    private final String name;
    private final String desc;
    private final boolean itf;
    private char[] stackInputTypes;
    private char[] stackOutputTypes;
    
    
    public Invoke(int opcode, String owner, String name, String desc, boolean itf) {
        super(opcode);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public boolean isItf() {
        return itf;
    }
    

    public boolean isMethodUsed(String desc, String name) {
        return this.desc.equals(desc) && this.name.equals(name);
    }

    public String getMethodUsed() {
        return desc + "." + name;
    }
    
    private String cMethodName;
    private String getCMethodName() {
        if (cMethodName == null) {
            cMethodName = name.replace('-', '_');
        }
        return cMethodName;
    }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        String t = owner.replace('.', '_').replace('/', '_').replace('$', '_');
        t = unarray(t);
        if (t != null && !dependencyList.contains(t)) dependencyList.add(t);
        // The world is incomplete while ClassReader visits instructions. Resolve
        // inherited/direct-call dependencies in the existing late rescan instead
        // of rebuilding the subclass index for each newly parsed class.
        if (!Parser.isReadingClass()) addResolvedDependencies(dependencyList);

        StringBuilder bld = new StringBuilder();
        if(opcode != Opcodes.INVOKEINTERFACE && opcode != Opcodes.INVOKEVIRTUAL) {
            return;
        }         
        bld.append(Util.mangle(owner));
        bld.append("_");
        if(name.equals("<init>")) {
            bld.append("__INIT__");
        } else {
            if(name.equals("<clinit>")) {
                bld.append("__CLINIT__");
            } else {
                bld.append(getCMethodName());
            }
        }
        bld.append("__");
        ArrayList<String> args = new ArrayList<>();
        BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, bld, args);
        String str = bld.toString();
        BytecodeMethod.addVirtualMethodsInvoked(str);
    }
    
    /**
     * The single class a virtual or interface call at this site must land in, or null
     * when more than one implementation is reachable and the dispatch has to stay
     * indirect.
     *
     * ONE resolver, asked by both the dependency pass and the emitter. They used to
     * ask different questions -- the dependency pass via resolveDevirtualizedOwner,
     * the emitter via its own chain -- and the moment the two disagreed the emitter
     * devirtualized to a method the dead code pass had already culled, which fails at
     * the C compiler with an implicitly declared function. Keeping one answer is what
     * makes the two passes consistent by construction.
     *
     * @return mangled class name of the only reachable implementation, or null
     */
    /* Upper bound on the compare chain. Past a handful the chain costs more than the
     * indirect branch it replaces, and inlining that many callees at one site stops
     * paying for itself in i-cache pressure. */
    private static final int CN1_MAX_GUARDS = 4;

    /**
     * Class-id guards for a virtual site whose receiver can only be a few concrete
     * classes: the id to compare against, and the function that receiver would run.
     *
     * @param suffix mangled signature suffix shared by every target
     * @return guards, or null when the site is not a candidate
     */
    java.util.List<String[]> buildGuards(String suffix) {
        if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
            return null;
        }
        ByteCodeClass bc = Parser.getClassObject(Util.mangle(owner));
        java.util.List<ByteCodeClass> cone = Parser.concreteReceiverCone(bc);
        if (cone == null || cone.size() < 2 || cone.size() > CN1_MAX_GUARDS) {
            return null;
        }
        java.util.List<String[]> guards = new java.util.ArrayList<String[]>(cone.size());
        for (ByteCodeClass c : cone) {
            ByteCodeClass d = c;
            while (d != null && !d.hasDeclaredNonAbstractMethod(name, desc)) {
                String b = d.getBaseClass();
                d = b == null ? null : Parser.getClassObject(b.replace('/', '_').replace('$', '_'));
            }
            if (d == null || d.isEliminated()) {
                // A receiver whose implementation cannot be named here: a default
                // interface method, or a target the dead code pass removed. The site is
                // then left alone entirely rather than partially guarded.
                return null;
            }
            guards.add(new String[] {
                "cn1_class_id_" + Util.mangle(c.getClsName()),
                Util.mangle(d.getClsName()) + "_" + getCMethodName() + suffix,
                Util.mangle(d.getClsName()) });
        }
        return guards;
    }

    /**
     * The classes a guarded form of this call would name directly, so the emitting
     * class can INCLUDE their headers.
     *
     * Include-only, never a liveness dependency. Guarding keeps nothing alive that was
     * not already alive: the chain still ends in the ordinary virtual thunk, which
     * marks the whole virtual family used exactly as before. Putting these on the
     * dependency list instead was measured at 853 to 951 emitted classes and 136B to
     * 245B instructions, because forcing a class alive enlarges other sites' cones and
     * keeps their targets alive in turn.
     *
     * @param out receives the mangled class names
     */
    public void collectGuardIncludes(java.util.Set<String> out) {
        StringBuilder sfx = new StringBuilder("__");
        BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, sfx, new ArrayList<String>());
        java.util.List<String[]> guards = buildGuards(sfx.toString());
        if (guards != null) {
            for (String[] g : guards) {
                out.add(g[2]);
            }
        }
        String single = resolveSingleTarget();
        if (single != null) {
            out.add(Util.mangle(single));
        }
    }

    String resolveSingleTarget() {
        ByteCodeClass bc = Parser.getClassObject(Util.mangle(owner));
        if (bc == null) {
            return null;
        }
        java.util.List<ByteCodeClass> impls = opcode == Opcodes.INVOKEINTERFACE
                ? Parser.resolveInterfaceTargets(bc, name, desc)
                : Parser.resolveVirtualTargets(bc, name, desc);
        if (impls == null) {
            return null;
        }
        if (impls.size() != 1) {
            return null;
        }
        ByteCodeClass target = impls.get(0);
        // The set is recomputed as elimination proceeds, so a target that was single
        // when dependencies were collected can be culled before emission. Emitting a
        // call to it then does not compile. Re-checking here costs a lookup and turns
        // that into an ordinary virtual dispatch instead.
        if (target.isEliminated() || !target.hasDeclaredNonAbstractMethod(name, desc)) {
            return null;
        }
        return target.getClsName();
    }

    public void addResolvedDependencies(List<String> dependencyList) {
        String proven = getProvenDirectOwner();
        if (proven != null && !dependencyList.contains(proven)) dependencyList.add(proven);
        if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) return;
        ByteCodeClass bc = Parser.getClassObject(Util.mangle(owner));
        String resolved = opcode == Opcodes.INVOKEVIRTUAL
                ? resolveConcreteInvokeOwner(bc, true) : null;
        // NOT resolveSingleTarget: the one implementation a closed-world cone holds is
        // include-only (collectGuardIncludes), exactly like the guard targets. As a
        // liveness dependency it kept a class nothing instantiates -- a Tracer call
        // devirtualized to the only Tracer, OtlpTracer, linked the whole OpenTelemetry
        // exporter into every server that never asked for tracing. The emitter
        // re-checks the target, so a culled one becomes an ordinary virtual call.
        if (resolved != null) {
            String dependency = unarray(Util.mangle(resolved));
            if (dependency != null && !dependencyList.contains(dependency)) dependencyList.add(dependency);
        }
    }

    private String findActualOwner(ByteCodeClass bc) {
        if(bc == null) {
            return owner;
        }
        List<BytecodeMethod> mtds = bc.getMethods();
        if(mtds == null) {
            return owner;
        }
        for(BytecodeMethod mtd : mtds) {
            if(mtd.getMethodName().equals(name) && mtd.isStatic()) {
                return bc.getClsName();
            }
        }
        return findActualOwner(bc.getBaseClassObject());
    }

    private String resolveConcreteInvokeOwner(ByteCodeClass ownerClass, boolean allowMissingMethodContext) {
        if (ownerClass == null || ownerClass.getConcreteClass() == null) {
            return null;
        }
        String currentClass = getMethod() != null ? getMethod().getClsName() : null;
        if (currentClass == null && !allowMissingMethodContext) {
            return null;
        }
        String ownerName = ownerClass.getClsName();
        if (currentClass != null && (ownerName.equals(currentClass) || currentClass.startsWith(ownerName + "_"))) {
            return null;
        }
        ByteCodeClass concreteClass = Parser.getClassObject(Util.mangle(ownerClass.getConcreteClass()));
        // The nearest class in the concrete type's own hierarchy that actually
        // declares the method -- which is what the runtime would dispatch to for
        // an instance of it. Resolving against concreteClass's declarations alone
        // gave up on everything it inherits rather than overrides.
        ByteCodeClass declaring = ByteCodeClass.findConcreteDeclaringClass(concreteClass, name, desc);
        if (declaring != null) {
            return declaring.getClsName();
        }
        return null;
    }

    // LEVER B (perf-tier1): re-entrancy guard for inlined-constructor #else emission.
    private boolean emittingInlineCtorElse = false;
    private InlinableConstructor inlineCtorPlan;
    private boolean inlineCtorAnalyzed = false;
    // Set by BytecodeMethod.markInitBeforePublish -- this <init> allocates + builds
    // + publishes its object (the matching NEW only pushed a placeholder).
    private boolean initBeforePublish = false;

    public void markInitBeforePublish() {
        this.initBeforePublish = true;
    }

    public boolean isInitBeforePublish() {
        return initBeforePublish;
    }

    // FUSED OBJECTS: non-null when this <init> belongs to a deferred NEW of a
    // @Fused class -- the emission allocates owner+children as one block,
    // fills BOTH placeholder slots, then proceeds with the ordinary call.
    private FusedConstructor fusedPlan;

    public void setFusedPlan(FusedConstructor plan) {
        this.fusedPlan = plan;
    }

    public FusedConstructor getFusedPlan() {
        return fusedPlan;
    }

    /**
     * Emit the fused allocation block for this {@code <init>}. Stack layout on
     * entry: [survivor(placeholder), receiver(placeholder), args...]; child
     * length expressions are pure reads of the on-stack int args. Falls through
     * to the caller's ordinary emission afterwards.
     */
    private void appendFusedAllocBlock(StringBuilder b) {
        List<ByteCodeMethodArg> args = getArgs();
        int n = args.size();
        // substitution table: __cn1ArgP -> this site's on-stack read of arg P
        String[] argExprByParam = new String[n];
        for (int p = 1; p <= n; p++) {
            argExprByParam[p - 1] = "SP[-" + (n - (p - 1)) + "].data.i";
        }
        List<FusedConstructor.Child> kids = fusedPlan.getChildren();
        String[] lenExprs = new String[kids.size()];
        for (int i = 0; i < kids.size(); i++) {
            lenExprs[i] = kids.get(i).siteLengthExpr(argExprByParam);
        }
        String cType = Util.mangle(owner);
        fusedPlan.appendFusedAlloc(b, cType, lenExprs, n + 1, n + 2);
    }

    /**
     * Lever B for the non-folded path: a void INVOKESPECIAL {@code <init>} whose args
     * are all still on the operand stack. Emits the {@code #ifdef CN1_INLINE_CTOR}
     * block (inlined field stores ON / ordinary call via re-entry OFF). Returns true
     * if handled. See {@link InlinableConstructor}.
     */
    private boolean tryAppendInlinedConstructor(StringBuilder b) {
        if (opcode != Opcodes.INVOKESPECIAL || !"<init>".equals(name)) {
            return false;
        }
        List<ByteCodeMethodArg> args = getArgs();
        if (!inlineCtorAnalyzed) {
            inlineCtorAnalyzed = true;
            inlineCtorPlan = InlinableConstructor.analyze(owner, desc);
        }
        if (inlineCtorPlan == null) {
            return false;
        }
        int n = args.size();
        String objExpr = "SP[-" + (n + 1) + "].data.o";
        String[] argExprs = new String[n];
        for (int j = 0; j < n; j++) {
            argExprs[j] = "SP[-" + (n - j) + "].data." + args.get(j).getQualifier();
        }
        if (initBeforePublish) {
            // Memset elimination: allocate into a temp, build fully, THEN publish
            // into the surviving object slot (SP[-(n+2)]) and pop receiver+args.
            // argCats == null: every argExpr here is a pure SP[-k].data.x read
            // (the args were evaluated onto the operand stack BEFORE this <init>),
            // so no temp hoisting is needed.
            String cType = Util.mangle(owner);
            // No retire guard on this path: the retire analysis wires guards to the
            // CustomInvoke that follows a NEW, never to a plain Invoke.
            inlineCtorPlan.appendInitBeforePublish(b, cType, argExprs, null, n + 2, n + 1, -1);
            return true;
        }
        b.append("\n#ifndef CN1_DISABLE_INLINE_CTOR\n"); // leading \n: the previous emission may not end a line, and a directive must start one
        inlineCtorPlan.appendStores(b, objExpr, argExprs);
        b.append("    SP -= ").append(n + 1).append(";\n");
        b.append("\n#else\n");
        emittingInlineCtorElse = true;
        appendInstruction(b);
        emittingInlineCtorElse = false;
        b.append("\n#endif\n");
        return true;
    }

    @Override
    public void appendInstruction(StringBuilder b) {
        if (fusedPlan != null) {
            // FUSED construction: single-block owner+children allocation into the
            // placeholder slots, then fall through to the ordinary ctor call below.
            appendFusedAllocBlock(b);
        }
        if (!emittingInlineCtorElse && tryAppendInlinedConstructor(b)) {
            return;
        }
        // special case for clone on an array which isn't a real method invocation
        if(name.equals("clone") && owner.indexOf('[') > -1) {
            b.append("    POP_MANY_AND_PUSH_OBJ(cloneArray(PEEK_OBJ(1)), 1);\n");
            return;
        }
        if (opcode == Opcodes.INVOKESPECIAL && !name.equals("<init>") && !name.equals("<clinit>")) {
            owner = Util.resolveInvokeSpecialOwner(owner, name, desc);
        }

        String invokeOwner = owner;
        StringBuilder bld = new StringBuilder();
        boolean isVirtualCall = false;
        if(opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL) {
            b.append("    ");

            // Well, it is actually legal to call private methods with invoke virtual, and kotlin
            // generates such calls.  But ParparVM strips out these virtual method definitions,
            // so we need to check if the method is private, and remove the virtual invocation 
            // if it is.
            String proven = getProvenDirectOwner();
            boolean isVirtual = proven == null;
            if (proven != null) invokeOwner = proven;
            if (isVirtual && opcode == Opcodes.INVOKEVIRTUAL) {
                ByteCodeClass bc = Parser.getClassObject(Util.mangle(owner));
                if (bc == null) {
                    System.err.println("WARNING: Failed to find class object for owner "+owner+" when rendering virtual method "+name);
                } else {
                    if (bc.isMethodPrivate(name, desc)) {
                        isVirtual = false;
                    } else {
                        String resolvedConcreteOwner = resolveConcreteInvokeOwner(bc, false);
                        if (resolvedConcreteOwner != null) {
                            invokeOwner = resolvedConcreteOwner;
                            isVirtual = false;
                        } else {
                            // CLOSED-WORLD DEVIRT: one reachable implementation -> direct
                            // call, which ThinLTO can then inline through.
                            //
                            // This asks for the SET of implementations rather than
                            // resolveDevirtualizedOwner's "exactly one or give up", because
                            // that one bails as soon as any subclass DECLARES the method --
                            // including an ABSTRACT re-declaration, which can never be the
                            // target of a call. An abstract class cannot be instantiated, and
                            // a concrete subclass below it must declare the method itself, in
                            // which case it shows up here as a second implementation and the
                            // site correctly stays virtual.
                            String single = resolveSingleTarget();
                            if (single != null) {
                                invokeOwner = single;
                                isVirtual = false;
                            }
                        }
                    }
                }
            }
            if (isVirtual && opcode == Opcodes.INVOKEINTERFACE) {
                // CLOSED-WORLD DEVIRT FOR INTERFACES. This was not attempted at all --
                // the block above is gated on INVOKEVIRTUAL -- and interface dispatch is
                // the most expensive call this VM makes: a class id load, a
                // classToInterfaceMap row, an offset, a vtable slot, then an indirect
                // branch, none of which the C compiler can see through.
                //
                // A closed world does not have to guess. If exactly one reachable class
                // implements the interface's method, that is where the call lands, and
                // it becomes a direct call that ThinLTO can inline.
                //
                // The null receiver is still checked: the direct form emits its own
                // test (see NativeInvocation), so a null does not silently skip to the
                // callee the way it would if the check lived only in the thunk.
                String single = resolveSingleTarget();
                if (single != null) {
                    invokeOwner = single;
                    isVirtual = false;
                }
            }
            if (isVirtual) {
                bld.append("virtual_");
                isVirtualCall = true;
            }
        } else {
            b.append("    ");
        }
        
        if(opcode == Opcodes.INVOKESTATIC) {
            // find the actual class of the static method to work around javac not defining it correctly
            ByteCodeClass bc = Parser.getClassObject(Util.mangle(owner));
            invokeOwner = findActualOwner(bc);
        }
        if (invokeOwner.startsWith("[")) {
            // Kotlin seems to generate calls to toString() on arrays using the array class
            // as an owner.  We'll just change this to java_lang_Object instead.
            bld.append("java_lang_Object");
        } else{
            bld.append(Util.mangle(invokeOwner));
        }
        bld.append("_");
        if(name.equals("<init>")) {
            bld.append("__INIT__");
        } else {
            if(name.equals("<clinit>")) {
                bld.append("__CLINIT__");
            } else {
                bld.append(getCMethodName());
            }
        }
        bld.append("__");
        ArrayList<String> args = new ArrayList<>();
        String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, bld, args);
        if (isVirtualCall) {
            BytecodeMethod.addVirtualMethodsInvoked(bld.toString().substring("virtual_".length()));
        } else {
            // direct/devirtualized calls of the hottest String/StringBuilder
            // natives get the call-site-inlined fast path (cn1_intrinsics.h)
            String renamed = com.codename1.tools.translator.InlineIntrinsics.rename(bld.toString());
            if (!renamed.contentEquals(bld)) {
                bld.setLength(0);
                bld.append(renamed);
            }
        }
        String receiver = opcode == Opcodes.INVOKESTATIC ? null : "SP[-" + (args.size() + 1) + "].data.o";
        boolean noPop = false;
        if (returnVal != null) {
            if(args.isEmpty() && opcode == Opcodes.INVOKESTATIC) {
                // special case for static method
                if(returnVal.equals("JAVA_OBJECT")) {
                    b.append("PUSH_OBJ");
                } else {
                    if(returnVal.equals("JAVA_INT")) {
                        b.append("PUSH_INT");
                    } else {
                        if(returnVal.equals("JAVA_LONG")) {
                            b.append("PUSH_LONG");
                        } else {
                            if(returnVal.equals("JAVA_DOUBLE")) {
                                b.append("PUSH_DOUBLE");
                            } else {
                                if(returnVal.equals("JAVA_FLOAT")) {
                                    b.append("PUSH_FLOAT");
                                } else {
                                    throw new UnsupportedOperationException("Unknown type: " + returnVal);
                                }
                            }
                        }
                    }
                }
                //b.append(returnVal);
                noPop = true;
                b.append("(");
            } else {
                b.append("{ ");
                b.append(returnVal);
                b.append(" tmpResult = ");
            }

        }
        java.util.List<String[]> guards = null;
        if (isVirtualCall && receiver != null) {
            StringBuilder sfx = new StringBuilder("__");
            BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, sfx, new ArrayList<String>());
            guards = buildGuards(sfx.toString());
        }
        if (guards != null) {
            NativeInvocation.appendGuarded(b, guards, bld.toString(), receiver, args, null,
                    args.size(), returnVal == null);
        } else {
            NativeInvocation.append(b, bld.toString(), receiver, args, null, args.size(),
                    getProvenDirectOwner() != null && !isExactReceiverNonNull());
        }
        if(noPop) {
            b.append(");\n");
            return;
        }
        if(returnVal != null) {
            b.append(";\n");
            if(opcode != Opcodes.INVOKESTATIC) {
                if(!args.isEmpty()) {
                    b.append("    SP-=");
                    b.append(args.size());
                    b.append(";\n");
                }
            } else {
                if(args.size() > 1) {
                    b.append("    SP-=");
                    b.append(args.size() - 1);
                    b.append(";\n");
                }
            }
            // TYPE-BEFORE-DATA discipline (same as the PUSH_* macros): the slot being
            // overwritten here is typically the stale receiver slot (type OBJECT). A
            // thread can be signal-stopped BETWEEN these two stores (conservative-GC
            // thread freeze lands at arbitrary instructions); if data were written
            // first the precise stack scan would see (type=OBJECT, data=<int>) and
            // gcMarkObject would dereference a non-pointer. Primitives set the final
            // type first (a (INT, stale-data) window is never dereferenced); the
            // object case goes through INVALID exactly like PUSH_POINTER (tmpResult
            // stays alive in the C temp, covered by the conservative scan).
            if(returnVal.equals("JAVA_OBJECT")) {
                b.append("    SP[-1].type = CN1_TYPE_INVALID; SP[-1].data.o = tmpResult; SP[-1].type = CN1_TYPE_OBJECT; }\n");
            } else {
                if(returnVal.equals("JAVA_INT")) {
                    b.append("    SP[-1].type = CN1_TYPE_INT; SP[-1].data.i = tmpResult; }\n");
                } else {
                    if(returnVal.equals("JAVA_LONG")) {
                        b.append("    SP[-1].type = CN1_TYPE_LONG; SP[-1].data.l = tmpResult; }\n");
                    } else {
                        if(returnVal.equals("JAVA_DOUBLE")) {
                            b.append("    SP[-1].type = CN1_TYPE_DOUBLE; SP[-1].data.d = tmpResult; }\n");
                        } else {
                            if(returnVal.equals("JAVA_FLOAT")) {
                                b.append("    SP[-1].type = CN1_TYPE_FLOAT; SP[-1].data.f = tmpResult; }\n");
                            } else {
                                throw new UnsupportedOperationException("Unknown type: " + returnVal);
                            }
                        }
                    }
                }
            }

            return;
        }
        b.append("; ");
        int val; 
        if(opcode != Opcodes.INVOKESTATIC) {
            val = args.size() + 1;
        } else {
            val = args.size();
        }
        if(val > 0) {
            b.append("    SP-= ");
            b.append(val);
            b.append(";\n");
        } else {
            b.append("\n");            
        }
    }
    
    
    // Master off-switch: -DCN1_DISABLE_INLINE=true disables trivial-method inlining.
    private static final boolean DISABLE_INLINE =
            "true".equalsIgnoreCase(Util.getProperty("CN1_DISABLE_INLINE", "false"));

    /**
     * If this invoke is a direct (provably monomorphic) instance call to a trivial
     * getter ({@code ALOAD 0; GETFIELD f; xRETURN}) or setter
     * ({@code ALOAD 0; xLOAD 1; PUTFIELD f; RETURN}), returns the equivalent
     * GETFIELD/PUTFIELD {@link Field} the optimizer can swap in for the call. The
     * field op has the identical operand-stack effect (getter: pop receiver, push
     * field; setter: pop receiver+value, push nothing) and derefs the same receiver,
     * so null-pointer behavior is preserved. {@code Field.tryReduce} later folds the
     * operands into a direct field expression. Returns null otherwise.
     */
    public Field asInlinableFieldAccess() {
        if (DISABLE_INLINE) {
            return null;
        }
        // Static no-arg singleton/static-field accessor: GETSTATIC f; xRETURN.
        // Monomorphic by definition (static dispatch), no receiver -> no null concern.
        if (opcode == Opcodes.INVOKESTATIC) {
            if (desc.length() < 3 || desc.charAt(0) != '(' || desc.endsWith(")V")) {
                return null;
            }
            BytecodeMethod target = findMethodUp(Parser.getClassObject(Util.mangle(owner)));
            if (target == null || !target.isStatic()) {
                return null;
            }
            if (desc.charAt(1) != ')') {
                // ONE-OBJECT-ARGUMENT STATIC ACCESSOR -- in practice javac's synthetic
                // access$NNN. An inner class reading a PRIVATE field of its outer class
                // cannot emit a GETFIELD, so javac synthesizes
                // `static int access$000(Outer o) { return o.field; }` and routes every
                // read through it. The translator emits the accessor into the OUTER
                // class's .c and the inner class into its own, so each read becomes a
                // CROSS-TRANSLATION-UNIT CALL that only LTO can remove -- and the clean,
                // desktop and CMake targets do not link with LTO.
                //
                // Measured on the self-hosting corpus before this fold: ArrayList 3
                // (one of them on ArrayListIterator.next(), i.e. per element of every
                // for-each loop in the program), ArrayDeque 8, TreeMap's sub-map family
                // 26, IdentityHashMap 3, Hashtable 1, plus ASM and translator classes.
                // Fixing it in the library instead -- widening those fields to
                // package-private, which is why OpenJDK's ArrayList.elementData is not
                // private -- fixes one class per edit and nothing in application code.
                //
                // The fold is exact rather than approximate. A static call with one
                // argument and a non-void return pops one slot and pushes one; GETFIELD
                // pops the objectref and pushes the field, which is the SAME stack
                // effect, and it dereferences the same reference, so a null argument
                // still throws at the same point. The body test below is the identical
                // `ALOAD 0; GETFIELD f; xRETURN` shape used for instance getters --
                // local 0 of a static method IS its first argument, so the existing
                // matcher describes this case without modification.
                //
                // Restricted to a single argument that is a REFERENCE: GETFIELD needs an
                // objectref, and a multi-argument method whose body reads only the first
                // would strand the rest on the operand stack -- the same trap documented
                // on the setter fold below.
                if (countDescArguments(desc) != 1) {
                    return null;
                }
                char argKind = desc.charAt(1);
                if (argKind != 'L' && argKind != '[') {
                    return null;
                }
                // MEMOIZED, AND THAT IS A CORRECTNESS REQUIREMENT RATHER THAN A SPEED
                // ONE. The verdict is read from the TARGET's body, and optimize()
                // rewrites bodies IN PLACE during code generation, so the same call
                // site answers differently depending on whether the class holding its
                // target happened to be emitted first -- and emission order is not
                // stable across hosts, because the JVM and ParparVM do not iterate a
                // HashMap in the same order. Measured: without this, Gate A reported 9
                // of 798 files differing between the JVM translator and the
                // self-hosted one, each side having folded a different subset.
                //
                // updateInlinableFieldDependencies() queries every invoke from
                // ByteCodeClass.updateAllDependencies, which runs BEFORE any
                // optimize(), so the first query reads raw bytecode and every later
                // one reuses that verdict. It also stops the dependency scan and
                // emission disagreeing, which would leave the caller's include list
                // missing the field owner's header.
                //
                // Scoped to THIS branch on purpose. The instance-getter and
                // static-forwarder folds above have the same order-dependence and
                // predate this change; memoizing them too moves ~280 files of emitted
                // C and can only fold MORE getters, which is the case vm/CLAUDE.md
                // warns about for the boxed types (a folded `return value;` on a
                // tagged immediate reads off a pointer with no fields). That is its
                // own change, with its own measurement.
                if (!staticAccessorComputed) {
                    Field f = trivialGetterField(target);
                    if (f != null) {
                        staticAccessorCache = new Field(Opcodes.GETFIELD, f.getOwner(),
                                f.getFieldName(), f.getDesc());
                        staticAccessorCache.setMethod(getMethod());
                    }
                    staticAccessorComputed = true;
                }
                return staticAccessorCache;
            }
            // Follow trivial static FORWARDER chains: with pre-nestmates javac a
            // lazy-holder getter compiles to `INVOKESTATIC Holder.access$000()`
            // whose own body is the GETSTATIC. optimize() collapses that inner
            // call IN PLACE, so whether this call site sees a foldable body
            // depends on CLASS EMISSION ORDER -- and the dependency re-scan
            // (updateInlinableFieldDependencies) would disagree with emission.
            // Resolving through the chain here makes both phases deterministic:
            // the fold always lands on the final field, order-independent.
            for (int depth = 0; target != null && depth < 4; depth++) {
                Field f = trivialStaticFieldGetter(target);
                if (f != null) {
                    Field getstatic = new Field(Opcodes.GETSTATIC, f.getOwner(), f.getFieldName(), f.getDesc());
                    getstatic.setMethod(getMethod());
                    return getstatic;
                }
                target = trivialStaticForwarderTarget(target);
            }
            return null;
        }
        if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKESPECIAL) {
            return null;
        }
        BytecodeMethod target = resolveDirectTarget();
        if (target == null) {
            return null;
        }
        // getter: zero args, non-void return
        if (desc.length() >= 3 && desc.charAt(0) == '(' && desc.charAt(1) == ')' && desc.charAt(2) != 'V') {
            Field f = trivialGetterField(target);
            if (f != null) {
                Field getfield = new Field(Opcodes.GETFIELD, f.getOwner(), f.getFieldName(), f.getDesc());
                getfield.setMethod(getMethod());
                return getfield;
            }
        }
        // setter: EXACTLY one arg, void return. A multi-arg method whose body
        // happens to store only arg1 (e.g. DateTimeRenderer.setMarkToday
        // (boolean, long) -- the long is unused) still POPS every argument as a
        // CALL; folding it to a PUTFIELD strands the extra args on the operand
        // stack and the emitted C reads misaligned slots.
        if (desc.endsWith(")V") && countDescArguments(desc) == 1) {
            Field f = trivialSetterField(target);
            if (f != null) {
                Field putfield = new Field(Opcodes.PUTFIELD, f.getOwner(), f.getFieldName(), f.getDesc());
                putfield.setMethod(getMethod());
                return putfield;
            }
        }
        return null;
    }

    private Field staticAccessorCache;
    private boolean staticAccessorComputed;

    /**
     * The concretely-called method if this is a direct (provably monomorphic)
     * instance call, else null. Monomorphic when: INVOKESPECIAL, or the static-type
     * owner class is final (no subtypes), or the target method is final or private,
     * or the existing @Concrete devirtualization resolves a single concrete owner.
     */
    private BytecodeMethod resolveDirectTarget() {
        String proven = getProvenDirectOwner();
        if (proven != null) return findMethodUp(Parser.getClassObject(proven));
        if (opcode == Opcodes.INVOKESPECIAL) {
            return findMethodUp(Parser.getClassObject(Util.mangle(owner)));
        }
        // INVOKEVIRTUAL
        ByteCodeClass bc = Parser.getClassObject(Util.mangle(owner));
        if (bc == null) {
            return null;
        }
        BytecodeMethod m = findMethodUp(bc);
        if (m != null && (bc.isFinalClass() || m.isFinal() || m.isPrivate())) {
            return m;
        }
        String rc = resolveConcreteInvokeOwner(bc, false);
        if (rc == null) {
            return null; // genuinely virtual -> target not fixed -> unsafe to inline
        }
        return findMethodUp(Parser.getClassObject(Util.mangle(rc)));
    }

    /** Exact callee for interprocedural ownership analysis; unresolved dispatch is rejected. */
    public BytecodeMethod getOwnershipTarget() {
        return opcode == Opcodes.INVOKESTATIC
                ? findMethodUp(Parser.getClassObject(Util.mangle(owner))) : resolveDirectTarget();
    }

    /**
     * If {@code m}'s body is exactly {@code INVOKESTATIC n()X ; xRETURN} (a
     * synthetic accessor / forwarder), resolves and returns n's method; else null.
     */
    private static BytecodeMethod trivialStaticForwarderTarget(BytecodeMethod m) {
        Instruction a = null, b = null;
        int count = 0;
        for (Instruction in : m.getInstructions()) {
            if (in instanceof LineNumber || in instanceof LabelInstruction || in instanceof LocalVariable) {
                continue;
            }
            count++;
            if (count == 1) a = in;
            else if (count == 2) b = in;
            else return null;
        }
        if (count != 2 || !(a instanceof Invoke)) return null;
        Invoke inner = (Invoke) a;
        if (inner.opcode != Opcodes.INVOKESTATIC) return null;
        if (inner.desc.length() < 3 || inner.desc.charAt(0) != '(' || inner.desc.charAt(1) != ')'
                || inner.desc.charAt(2) == 'V') return null;
        int rc = b.getOpcode();
        if (rc != Opcodes.IRETURN && rc != Opcodes.LRETURN && rc != Opcodes.FRETURN
                && rc != Opcodes.DRETURN && rc != Opcodes.ARETURN) return null;
        BytecodeMethod t = inner.findMethodUp(Parser.getClassObject(
                Util.mangle(inner.owner)));
        return (t != null && t.isStatic()) ? t : null;
    }

    /** Number of declared arguments (not slots) in a method descriptor. */
    private static int countDescArguments(String desc) {
        int n = 0;
        int i = 1; // skip '('
        while (i < desc.length() && desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            if (c == '[') {
                i++;
                continue; // array dims prefix the element type
            }
            n++;
            if (c == 'L') {
                i = desc.indexOf(';', i) + 1;
                if (i == 0) {
                    return -1; // malformed
                }
            } else {
                i++;
            }
        }
        return n;
    }

    /** Finds the method (name+desc) declared in cls or, if inherited, a superclass. */
    private BytecodeMethod findMethodUp(ByteCodeClass cls) {
        while (cls != null) {
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.getMethodName().equals(name) && desc.equals(m.getSignature())) {
                    return m;
                }
            }
            cls = cls.getBaseClassObject();
        }
        return null;
    }

    /** Returns the GETFIELD Field if the method body is exactly ALOAD 0; GETFIELD f; xRETURN. */
    private static Field trivialGetterField(BytecodeMethod m) {
        Instruction a = null, b = null, c = null;
        int count = 0;
        for (Instruction in : m.getInstructions()) {
            if (in instanceof LineNumber || in instanceof LabelInstruction || in instanceof LocalVariable) {
                continue;
            }
            count++;
            if (count == 1) a = in;
            else if (count == 2) b = in;
            else if (count == 3) c = in;
            else return null;
        }
        if (count != 3) return null;
        if (!(a instanceof VarOp) || a.getOpcode() != Opcodes.ALOAD || ((VarOp) a).getIndex() != 0) return null;
        if (!(b instanceof Field) || b.getOpcode() != Opcodes.GETFIELD) return null;
        if (!isValueReturnOpcode(c.getOpcode())) return null;
        return (Field) b;
    }

    /** True for the xRETURN opcodes that return a value (i.e. not RETURN). */
    private static boolean isValueReturnOpcode(int rc) {
        return rc == Opcodes.IRETURN || rc == Opcodes.LRETURN || rc == Opcodes.FRETURN
                || rc == Opcodes.DRETURN || rc == Opcodes.ARETURN;
    }

    /** Returns the GETSTATIC Field if the body is exactly GETSTATIC f; xRETURN. */
    private static Field trivialStaticFieldGetter(BytecodeMethod m) {
        Instruction a = null, b = null;
        int count = 0;
        for (Instruction in : m.getInstructions()) {
            if (in instanceof LineNumber || in instanceof LabelInstruction || in instanceof LocalVariable) {
                continue;
            }
            count++;
            if (count == 1) a = in;
            else if (count == 2) b = in;
            else return null;
        }
        if (count != 2) return null;
        if (!(a instanceof Field) || a.getOpcode() != Opcodes.GETSTATIC) return null;
        int rc = b.getOpcode();
        if (rc != Opcodes.IRETURN && rc != Opcodes.LRETURN && rc != Opcodes.FRETURN
                && rc != Opcodes.DRETURN && rc != Opcodes.ARETURN) return null;
        return (Field) a;
    }

    /** Returns the PUTFIELD Field if the body is exactly ALOAD 0; xLOAD 1; PUTFIELD f; RETURN. */
    private static Field trivialSetterField(BytecodeMethod m) {
        Instruction a = null, b = null, c = null, d = null;
        int count = 0;
        for (Instruction in : m.getInstructions()) {
            if (in instanceof LineNumber || in instanceof LabelInstruction || in instanceof LocalVariable) {
                continue;
            }
            count++;
            if (count == 1) a = in;
            else if (count == 2) b = in;
            else if (count == 3) c = in;
            else if (count == 4) d = in;
            else return null;
        }
        if (count != 4) return null;
        if (!(a instanceof VarOp) || a.getOpcode() != Opcodes.ALOAD || ((VarOp) a).getIndex() != 0) return null;
        if (!(b instanceof VarOp) || ((VarOp) b).getIndex() != 1) return null; // the single value arg
        int lop = b.getOpcode();
        if (lop != Opcodes.ILOAD && lop != Opcodes.LLOAD && lop != Opcodes.FLOAD
                && lop != Opcodes.DLOAD && lop != Opcodes.ALOAD) return null;
        if (!(c instanceof Field) || c.getOpcode() != Opcodes.PUTFIELD) return null;
        if (d.getOpcode() != Opcodes.RETURN) return null; // void
        return (Field) c;
    }

    public List<ByteCodeMethodArg> getArgs() {
        return Util.getMethodArgs(desc);
    }

    @Override
    public char[] getStackInputTypes() {
        if (stackInputTypes == null) {
            List<ByteCodeMethodArg> args = getArgs();
            int thisArg = 0;
            if (opcode != Opcodes.INVOKESTATIC) {
                thisArg++;
                
            }
            stackInputTypes = new char[args.size() + thisArg];
            if (opcode != Opcodes.INVOKESTATIC) {
                stackInputTypes[0] = 'o';
            }
            int len = args.size();
            for (int i=0; i<len; i++) {
                stackInputTypes[i+thisArg] = args.get(i).getQualifier();
            }
        }
        return stackInputTypes;
    }

    @Override
    public char[] getStackOutputTypes() {
        if (stackOutputTypes == null) {
            String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, new StringBuilder(), new ArrayList<>());
            if (returnVal == null) {
                stackOutputTypes = new char[0];
            } else {
                stackOutputTypes = new char[1];
                if(returnVal.equals("JAVA_OBJECT")) {
                    stackOutputTypes[0] = 'o';
                } else {
                    if(returnVal.equals("JAVA_INT")) {
                        stackOutputTypes[0] = 'i';
                    } else {
                        if(returnVal.equals("JAVA_LONG")) {
                            stackOutputTypes[0] = 'l';
                        } else {
                            if(returnVal.equals("JAVA_DOUBLE")) {
                                stackOutputTypes[0] = 'd';
                            } else {
                                if(returnVal.equals("JAVA_FLOAT")) {
                                    stackOutputTypes[0] = 'f';
                                } else {
                                    throw new UnsupportedOperationException("Unknown type: " + returnVal);
                                }
                            }
                        }
                    }
                }
            }
        }
        return stackOutputTypes;
    }
    // for the SignatureSet interface
	public boolean containsSignature(SignatureSet sig) {
		return desc.equals(sig.getSignature());
	}
	public String getMethodName() {
		return(name);
	}
	public String getSignature() { return(desc); }
    
    
}
