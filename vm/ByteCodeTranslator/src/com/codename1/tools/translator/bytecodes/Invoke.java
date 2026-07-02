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
        String dependencyOwner = owner;
        if (opcode == Opcodes.INVOKEVIRTUAL) {
            ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
            String resolvedConcreteOwner = resolveConcreteInvokeOwner(bc, true);
            if (resolvedConcreteOwner != null) {
                dependencyOwner = resolvedConcreteOwner;
            }
        }
        String t = owner.replace('.', '_').replace('/', '_').replace('$', '_');
        t = unarray(t);
        if(t != null && !dependencyList.contains(t)) {
            dependencyList.add(t);
        }
        if (!owner.equals(dependencyOwner)) {
            String concreteDependency = dependencyOwner.replace('.', '_').replace('/', '_').replace('$', '_');
            concreteDependency = unarray(concreteDependency);
            if (concreteDependency != null && !dependencyList.contains(concreteDependency)) {
                dependencyList.add(concreteDependency);
            }
        }

        StringBuilder bld = new StringBuilder();
        if(opcode != Opcodes.INVOKEINTERFACE && opcode != Opcodes.INVOKEVIRTUAL) {
            return;
        }         
        bld.append(owner.replace('/', '_').replace('$', '_'));
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
        ByteCodeClass concreteClass = Parser.getClassObject(ownerClass.getConcreteClass().replace('/', '_').replace('$', '_'));
        if (concreteClass != null && concreteClass.hasDeclaredNonAbstractMethod(name, desc)) {
            return concreteClass.getClsName();
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
        List<FusedConstructor.Child> kids = fusedPlan.getChildren();
        String[] lenExprs = new String[kids.size()];
        for (int i = 0; i < kids.size(); i++) {
            FusedConstructor.Child c = kids.get(i);
            String expr = c.ctorLengthExpr();
            if (expr.startsWith("__cn1Arg")) {
                int p = Integer.parseInt(expr.substring("__cn1Arg".length()));
                expr = "SP[-" + (n - (p - 1)) + "].data.i";
            }
            lenExprs[i] = expr;
        }
        String cType = owner.replace('/', '_').replace('$', '_');
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
            String cType = owner.replace('/', '_').replace('$', '_');
            inlineCtorPlan.appendInitBeforePublish(b, cType, argExprs, null, n + 2, n + 1);
            return true;
        }
        b.append("#ifndef CN1_DISABLE_INLINE_CTOR\n");
        inlineCtorPlan.appendStores(b, objExpr, argExprs);
        b.append("    SP -= ").append(n + 1).append(";\n");
        b.append("#else\n");
        emittingInlineCtorElse = true;
        appendInstruction(b);
        emittingInlineCtorElse = false;
        b.append("#endif\n");
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
            boolean isVirtual = true;
            if (opcode == Opcodes.INVOKEVIRTUAL) {
                ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
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
                        }
                    }
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
            ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
            invokeOwner = findActualOwner(bc);
        }
        if (invokeOwner.startsWith("[")) {
            // Kotlin seems to generate calls to toString() on arrays using the array class
            // as an owner.  We'll just change this to java_lang_Object instead.
            bld.append("java_lang_Object");
        } else{
            bld.append(invokeOwner.replace('/', '_').replace('$', '_'));
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
            BytecodeMethod.addVirtualMethodsInvoked(bld.substring("virtual_".length()));
        }
        boolean noPop = false;
        if(returnVal == null) {
            b.append(bld);
        } else {
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
            b.append(bld);
        }
        b.append("(threadStateData");
        
        
        
        if(opcode != Opcodes.INVOKESTATIC) {
            b.append(", SP[-");
            b.append(args.size() + 1);
            b.append("].data.o");
        }
        int offset = args.size();
        //int numArgs = offset;
        for(String a : args) {
            b.append(", ");
            b.append("SP[-");
            b.append(offset);
            b.append("].data.");
            b.append(a);
            offset--;
        }
        if(noPop) {
            b.append("));\n");
            return;
        }
        if(returnVal != null) {
            b.append(");\n");
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
        b.append("); ");
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
            "true".equalsIgnoreCase(System.getProperty("CN1_DISABLE_INLINE", "false"));

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
            if (desc.length() < 3 || desc.charAt(0) != '(' || desc.charAt(1) != ')' || desc.charAt(2) == 'V') {
                return null;
            }
            BytecodeMethod target = findMethodUp(Parser.getClassObject(owner.replace('/', '_').replace('$', '_')));
            if (target == null || !target.isStatic()) {
                return null;
            }
            Field f = trivialStaticFieldGetter(target);
            if (f != null) {
                Field getstatic = new Field(Opcodes.GETSTATIC, f.getOwner(), f.getFieldName(), f.getDesc());
                getstatic.setMethod(getMethod());
                return getstatic;
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
        // setter: one arg, void return
        if (desc.endsWith(")V")) {
            Field f = trivialSetterField(target);
            if (f != null) {
                Field putfield = new Field(Opcodes.PUTFIELD, f.getOwner(), f.getFieldName(), f.getDesc());
                putfield.setMethod(getMethod());
                return putfield;
            }
        }
        return null;
    }

    /**
     * The concretely-called method if this is a direct (provably monomorphic)
     * instance call, else null. Monomorphic when: INVOKESPECIAL, or the static-type
     * owner class is final (no subtypes), or the target method is final or private,
     * or the existing @Concrete devirtualization resolves a single concrete owner.
     */
    private BytecodeMethod resolveDirectTarget() {
        if (opcode == Opcodes.INVOKESPECIAL) {
            return findMethodUp(Parser.getClassObject(owner.replace('/', '_').replace('$', '_')));
        }
        // INVOKEVIRTUAL
        ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
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
        return findMethodUp(Parser.getClassObject(rc.replace('/', '_').replace('$', '_')));
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
        int rc = c.getOpcode();
        if (rc != Opcodes.IRETURN && rc != Opcodes.LRETURN && rc != Opcodes.FRETURN
                && rc != Opcodes.DRETURN && rc != Opcodes.ARETURN) return null;
        return (Field) b;
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
