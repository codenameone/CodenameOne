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
 
import com.codename1.tools.translator.bytecodes.ArithmeticExpression;
import com.codename1.tools.translator.bytecodes.ArrayLengthExpression;
import com.codename1.tools.translator.bytecodes.ArrayLoadExpression;
import com.codename1.tools.translator.bytecodes.AssignableExpression;
import com.codename1.tools.translator.bytecodes.BasicInstruction;
import com.codename1.tools.translator.bytecodes.CustomIntruction;
import com.codename1.tools.translator.bytecodes.CustomInvoke;
import com.codename1.tools.translator.bytecodes.CustomJump;
import com.codename1.tools.translator.bytecodes.DupExpression;
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
import com.codename1.tools.translator.bytecodes.TryCatch;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import com.codename1.tools.translator.bytecodes.VarOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class BytecodeMethod implements SignatureSet {
    private static MethodDependencyGraph dependencyGraph;

    /**
     * @return the acceptStaticOnEquals
     */
    public static boolean isAcceptStaticOnEquals() {
        return acceptStaticOnEquals;
    }

    /**
     * @param aAcceptStaticOnEquals the acceptStaticOnEquals to set
     */
    public static void setAcceptStaticOnEquals(boolean aAcceptStaticOnEquals) {
        acceptStaticOnEquals = aAcceptStaticOnEquals;
    }

    public static void setDependencyGraph(MethodDependencyGraph dependencyGraph) {
        BytecodeMethod.dependencyGraph = dependencyGraph;
    }
    private List<ByteCodeMethodArg> arguments = new ArrayList<ByteCodeMethodArg>();
    private Set<LocalVariable> localVariables = new HashSet<LocalVariable>();
    private ByteCodeMethodArg returnType;
    private String methodName;
    private String clsName;
    private boolean constructor;
    private boolean staticMethod;
    private boolean privateMethod;
    private boolean nativeMethod;
    private boolean abstractMethod;
    private List<String> dependentClasses = new ArrayList<String>();
    //private List<String> exportedClasses = new ArrayList<String>();
    private List<Instruction> instructions = new ArrayList<Instruction>();
    private String declaration = ""; 
    private String sourceFile;
    private int maxStack;
    private int maxLocals;
    private static boolean acceptStaticOnEquals;
    private static final boolean FORCE_VOLATILE_LOCALS =
            "true".equalsIgnoreCase(System.getProperty("CN1_FORCE_VOLATILE_LOCALS", "false"));
    private int methodOffset;
    private boolean forceVirtual;
    private boolean virtualOverriden;
    private boolean finalMethod;
    private boolean synchronizedMethod;
    private final static Set<String> virtualMethodsInvoked = new TreeSet<String>();    
    private String desc;
    private boolean eliminated;
    private boolean barebone;
    private boolean disableDebugInfo;
    private boolean disableNullAndArrayBoundsChecks;
    private boolean fastMethodStackInUse;
    private boolean fastMethodStackPrimitiveOnly;
    private String jsBodyScript;
    private String[] jsBodyParams;

    
    static boolean optimizerOn;

    /**
     * When true, the translator emits extra metadata (per-frame locals-address
     * tables, variable side-tables) and uses a debugger-aware form of
     * __CN1_DEBUG_INFO. Toggled via the cn1.onDeviceDebug system property.
     * Release builds leave this off and pay no overhead.
     */
    static boolean onDeviceDebug;

    static {
        String op = System.getProperty("optimizer");
        optimizerOn = op == null || op.equalsIgnoreCase("on");
        //optimizerOn = false;

        onDeviceDebug = "true".equalsIgnoreCase(System.getProperty("cn1.onDeviceDebug", "false"));
    }

    public static boolean isOnDeviceDebug() {
        return onDeviceDebug;
    }

    public boolean isBarebone() {
        return barebone;
    }

    public boolean isDisableDebugInfo() {
        return disableDebugInfo;
    }

    public void setDisableDebugInfo(boolean disableDebugInfo) {
        this.disableDebugInfo = disableDebugInfo;
    }

    public boolean isDisableNullAndArrayBoundsChecks() {
        return disableNullAndArrayBoundsChecks;
    }

    public void setDisableNullAndArrayBoundsChecks(boolean disableNullAndArrayBoundsChecks) {
        this.disableNullAndArrayBoundsChecks = disableNullAndArrayBoundsChecks;
    }

    private boolean checkBarebone() {
        if(synchronizedMethod || nativeMethod || hasExceptionHandlingOrMethodCalls() || localVariables.size() > 0) {
            return false;
        }
        int argSlots = 0;
        if(!staticMethod) {
            argSlots++;
        }
        for(ByteCodeMethodArg arg : arguments) {
            argSlots++;
            if(arg.isDoubleOrLong()) {
                argSlots++;
            }
        }
        if(maxLocals > argSlots) {
            return false;
        }
        int maxLocalIndexUsed = -1;
        for (Instruction i : instructions) {
            if (i instanceof VarOp) {
                maxLocalIndexUsed = Math.max(maxLocalIndexUsed, ((VarOp)i).getIndex());
            } else if (i instanceof IInc) {
                maxLocalIndexUsed = Math.max(maxLocalIndexUsed, ((IInc)i).getVar());
            }
        }
        if (maxLocalIndexUsed >= argSlots) {
            return false;
        }
        for(Instruction i : instructions) {
            if(i instanceof LabelInstruction || i instanceof LineNumber || i instanceof IInc ||
                    i instanceof Jump || i instanceof CustomJump || i instanceof LocalVariable) {
                continue;
            }
            if(i instanceof BasicInstruction) {
                int op = i.getOpcode();
                switch(op) {
                    case Opcodes.SIPUSH:
                    case Opcodes.BIPUSH:
                    case Opcodes.ICONST_0:
                    case Opcodes.ICONST_1:
                    case Opcodes.ICONST_2:
                    case Opcodes.ICONST_3:
                    case Opcodes.ICONST_4:
                    case Opcodes.ICONST_5:
                    case Opcodes.ICONST_M1:
                    case Opcodes.LCONST_0:
                    case Opcodes.LCONST_1:
                    case Opcodes.FCONST_0:
                    case Opcodes.FCONST_1:
                    case Opcodes.FCONST_2:
                    case Opcodes.DCONST_0:
                    case Opcodes.DCONST_1:
                    case Opcodes.RETURN:
                    case Opcodes.IRETURN:
                    case Opcodes.LRETURN:
                    case Opcodes.FRETURN:
                    case Opcodes.DRETURN:
                    case Opcodes.ARETURN:
                    case Opcodes.NOP:
                    case Opcodes.POP:
                    case Opcodes.POP2:
                    case Opcodes.DUP:
                    case Opcodes.DUP2:
                    case Opcodes.DUP_X1:
                    case Opcodes.DUP2_X1:
                    case Opcodes.DUP_X2:
                    case Opcodes.DUP2_X2:
                    case Opcodes.SWAP:
                    case Opcodes.IADD:
                    case Opcodes.LADD:
                    case Opcodes.FADD:
                    case Opcodes.DADD:
                    case Opcodes.ISUB:
                    case Opcodes.LSUB:
                    case Opcodes.FSUB:
                    case Opcodes.DSUB:
                    case Opcodes.IMUL:
                    case Opcodes.LMUL:
                    case Opcodes.FMUL:
                    case Opcodes.DMUL:
                    case Opcodes.FDIV:
                    case Opcodes.DDIV:
                    case Opcodes.FREM:
                    case Opcodes.DREM:
                    case Opcodes.INEG:
                    case Opcodes.LNEG:
                    case Opcodes.FNEG:
                    case Opcodes.DNEG:
                    case Opcodes.ISHL:
                    case Opcodes.LSHL:
                    case Opcodes.ISHR:
                    case Opcodes.LSHR:
                    case Opcodes.IUSHR:
                    case Opcodes.LUSHR:
                    case Opcodes.IAND:
                    case Opcodes.LAND:
                    case Opcodes.IOR:
                    case Opcodes.LOR:
                    case Opcodes.IXOR:
                    case Opcodes.LXOR:
                    case Opcodes.I2L:
                    case Opcodes.I2F:
                    case Opcodes.I2D:
                    case Opcodes.L2I:
                    case Opcodes.L2F:
                    case Opcodes.L2D:
                    case Opcodes.F2I:
                    case Opcodes.F2L:
                    case Opcodes.F2D:
                    case Opcodes.D2I:
                    case Opcodes.D2L:
                    case Opcodes.D2F:
                    case Opcodes.I2B:
                    case Opcodes.I2C:
                    case Opcodes.I2S:
                    case Opcodes.LCMP:
                    case Opcodes.FCMPG:
                    case Opcodes.FCMPL:
                    case Opcodes.DCMPL:
                    case Opcodes.DCMPG:
                        continue;
                }
                return false;
            }
            if(i instanceof VarOp) {
                continue;
            }
            if(i instanceof ArithmeticExpression) {
                continue;
            }
            if(i instanceof Field) {
                int op = i.getOpcode();
                if(op == Opcodes.GETFIELD) {
                    continue;
                }
                if(op == Opcodes.PUTFIELD) {
                    if(((Field)i).isObject()) {
                        return false;
                    }
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    private boolean canUseFastMethodStack() {
        if (synchronizedMethod || nativeMethod || abstractMethod) {
            return false;
        }
        for (Instruction instruction : instructions) {
            if (instruction instanceof TryCatch
                    || instruction instanceof Invoke
                    || instruction instanceof CustomInvoke
                    || instruction instanceof Field
                    || instruction instanceof TypeInstruction
                    || instruction instanceof MultiArray
                    || instruction instanceof CustomIntruction) {
                return false;
            }
            if (instruction instanceof ArrayLoadExpression && !disableNullAndArrayBoundsChecks) {
                return false;
            }
            if (instruction instanceof BasicInstruction) {
                int op = instruction.getOpcode();
                if (op == Opcodes.MONITORENTER || op == Opcodes.MONITOREXIT
                        || op == Opcodes.ATHROW
                        || op == Opcodes.IDIV || op == Opcodes.LDIV || op == Opcodes.IREM || op == Opcodes.LREM
                        || op == Opcodes.ARRAYLENGTH
                        || (!disableNullAndArrayBoundsChecks && (op >= Opcodes.IALOAD && op <= Opcodes.SALOAD))
                        || (!disableNullAndArrayBoundsChecks && (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE))
                        || (!disableNullAndArrayBoundsChecks && (op == Opcodes.AALOAD || op == Opcodes.AASTORE
                        || op == Opcodes.BALOAD || op == Opcodes.BASTORE || op == Opcodes.CALOAD || op == Opcodes.CASTORE))
                        || op == Opcodes.NEWARRAY) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPrimitiveOnlyFastFrameCandidate() {
        for (ByteCodeMethodArg arg : arguments) {
            if (arg.getQualifier() == 'o') {
                return false;
            }
        }
        if (!returnType.isVoid() && returnType.getQualifier() == 'o') {
            return false;
        }
        if (!staticMethod) {
            return false;
        }
        for (Instruction instruction : instructions) {
            if (instruction instanceof VarOp) {
                int op = instruction.getOpcode();
                if (op == Opcodes.ALOAD || op == Opcodes.ASTORE) {
                    return false;
                }
            }
            if (instruction instanceof BasicInstruction) {
                int op = instruction.getOpcode();
                if (op == Opcodes.ARETURN || op == Opcodes.ACONST_NULL || op == Opcodes.AALOAD || op == Opcodes.AASTORE) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean useFastReturnRelease() {
        return fastMethodStackInUse && !TryCatch.isTryCatchInMethod();
    }

    public BytecodeMethod(String clsName, int access, String name, String desc, String signature, String[] exceptions) {
        methodName = name;
        this.clsName = clsName;
        this.desc = desc;
        privateMethod = (access & Opcodes.ACC_PRIVATE) == Opcodes.ACC_PRIVATE;
        nativeMethod = (access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE;
        staticMethod = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
        finalMethod = (access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;
        synchronizedMethod = (access & Opcodes.ACC_SYNCHRONIZED) == Opcodes.ACC_SYNCHRONIZED;
        abstractMethod = (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
        int pos = desc.lastIndexOf(')');
        if (!staticMethod) {
            if (!dependentClasses.contains("java_lang_NullPointerException")) {
                dependentClasses.add("java_lang_NullPointerException");
            }
        } // 
        if(methodName.equals("<init>")) {
            methodName = "__INIT__";
            constructor = true;
            returnType = new ByteCodeMethodArg(Void.TYPE, 0);
        } else {
            if(methodName.equals("<clinit>")) {
                methodName = "__CLINIT__";
                returnType = new ByteCodeMethodArg(Void.TYPE, 0);
                staticMethod = true;
            } else {            
                String retType = desc.substring(pos + 1);
                if(retType.equals("V")) {
                    returnType = new ByteCodeMethodArg(Void.TYPE, 0);
                } else {
                    int dim = 0;
                    while(retType.startsWith("[")) {
                        retType = retType.substring(1);
                        dim++;
                    }
                    char currentType = retType.charAt(0);
                    switch(currentType) {
                        case 'L':
                            // Object skip until ;
                            int idx = retType.indexOf(';');
                            String objectType = retType.substring(1, idx);
                            objectType = objectType.replace('/', '_').replace('$', '_');
                            if(!dependentClasses.contains(objectType)) {
                                dependentClasses.add(objectType);
                            }
                            //if (!this.isPrivate() && !exportedClasses.contains(objectType)) {
                            //    exportedClasses.add(objectType);
                            //}
                            returnType = new ByteCodeMethodArg(objectType, dim);
                            break;
                        case 'I':
                            returnType = new ByteCodeMethodArg(Integer.TYPE, dim);
                            break;
                        case 'J':
                            returnType = new ByteCodeMethodArg(Long.TYPE, dim);
                            break;
                        case 'B':
                            returnType = new ByteCodeMethodArg(Byte.TYPE, dim);
                            break;
                        case 'S':
                            returnType = new ByteCodeMethodArg(Short.TYPE, dim);
                            break;
                        case 'F':
                            returnType = new ByteCodeMethodArg(Float.TYPE, dim);
                            break;
                        case 'D':
                            returnType = new ByteCodeMethodArg(Double.TYPE, dim);
                            break;
                        case 'Z':
                            returnType = new ByteCodeMethodArg(Boolean.TYPE, dim);
                            break;
                        case 'C':
                            returnType = new ByteCodeMethodArg(Character.TYPE, dim);
                            break;
                    }
                }
            }
        }
        int currentArrayDim = 0;
        desc = desc.substring(1, pos);
        for(int i = 0 ; i < desc.length() ; i++) {
            char currentType = desc.charAt(i);
            switch(currentType) {
                case '[':
                    // array of...
                    currentArrayDim++;
                    continue;
                case 'L':
                    // Object skip until ;
                    int idx = desc.indexOf(';', i);
                    String objectType = desc.substring(i + 1, idx);
                    objectType = objectType.replace('/', '_').replace('$', '_');
                    if(!dependentClasses.contains(objectType)) {
                        dependentClasses.add(objectType);
                    }
                    //if (!this.isPrivate() && !exportedClasses.contains(objectType)) {
                    //    exportedClasses.contains(objectType);
                    //}
                    i = idx;
                    arguments.add(new ByteCodeMethodArg(objectType, currentArrayDim));
                    break;
                case 'I':
                    arguments.add(new ByteCodeMethodArg(Integer.TYPE, currentArrayDim));
                    break;
                case 'J':
                    arguments.add(new ByteCodeMethodArg(Long.TYPE, currentArrayDim));
                    break;
                case 'B':
                    arguments.add(new ByteCodeMethodArg(Byte.TYPE, currentArrayDim));
                    break;
                case 'S':
                    arguments.add(new ByteCodeMethodArg(Short.TYPE, currentArrayDim));
                    break;
                case 'F':
                    arguments.add(new ByteCodeMethodArg(Float.TYPE, currentArrayDim));
                    break;
                case 'D':
                    arguments.add(new ByteCodeMethodArg(Double.TYPE, currentArrayDim));
                    break;
                case 'Z':
                    arguments.add(new ByteCodeMethodArg(Boolean.TYPE, currentArrayDim));
                    break;
                case 'C':
                    arguments.add(new ByteCodeMethodArg(Character.TYPE, currentArrayDim));
                    break;
            }
            currentArrayDim = 0;
        }

        if (dependencyGraph != null) {
            dependencyGraph.registerMethod(this);
        }
    }

    // use this instead of isMethodUsed to compare traditional with new results
    public boolean isMethodUsedTester(BytecodeMethod bm)
    {
        boolean oldway = isMethodUsedOldWay(bm);
        boolean newway = isMethodUsed(bm);
        if(oldway!=newway)
        	{ throw new Error("different result"); 
        	}
        return newway;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendFunctionPointer(sb);
        return sb.toString();
    }
    
    
    
    
    private Hashtable<String,SignatureSet> usedSigs;
    
    // [ddyer 4/2017] avoid creating a lot of temporary objects. 
    // more than 3x faster than the old way.
    public boolean isMethodUsed(BytecodeMethod bm0) {
    	SignatureSet bm = (SignatureSet)bm0;
        if(usedSigs == null) {
        	usedSigs = new Hashtable<String,SignatureSet>();
            for(Instruction ins : instructions) {
            	String sname = ins.getMethodName();
            	if(sname!=null)
            	{
            		SignatureSet ss = usedSigs.get(sname);
            		// either use the instruction itself, or create a set of them
            		ss = ss==null ? ins : new MultipleSignatureSet((SignatureSet)ins,ss); 
            		usedSigs.put(sname,ss);
            	}
            }
        }
        String name = bm.getMethodName();
        SignatureSet ss = usedSigs.get("__INIT__".equals(name)?"<init>":name);
        return ((ss==null) ? false : ss.containsSignature(bm));
    }

    /**
     * Flag to indicate whether this method is used by native sources.
     */
    private boolean usedByNative;

    /**
     * Internal use: to track the list of native sources that were used to calculate the
     * usedByNative flag.
     */
    private String[] usedByNativeSources;

    /**
     * Checks to see if this method is used by any of the provided native sources.
     * @param nativeSources The native sources to check.
     * @param cls The class that the method belongs to.  This is used to improve performance by first checking to
     *            see if the class is Not referenced in native sources.  If the class is not referenced, then we know that
     *            neither is the method.  This method will also set the {@link ByteCodeClass#setUsedByNative(boolean)} flag
     *            to improve the performance for the next method that is checked in the same class.
     * @return True if the method is used by native.
     */
    public boolean isMethodUsedByNative(String[] nativeSources, ByteCodeClass cls) {
        if (nativeSources == null) return false;
        if (nativeSources == usedByNativeSources) {
            return usedByNative;
        }
        usedByNativeSources = nativeSources;

        if (cls != null && cls.getUsedByNative() == ByteCodeClass.UsedByNativeResult.Unused) {
            // If the class isn't used, then neither is the method.
            usedByNative = false;
            return false;
        }



        // check native code: O(|symbol|) lookups against the inverted index instead
        // of an O(native_bytes) substring scan per method. Semantics are identical --
        // the index answers "is X a substring of the native source text".
        StringBuilder b = new StringBuilder();
        this.appendFunctionPointer(b);
        String str = b.toString();
        NativeSymbolIndex idx = Parser.getNativeSymbolIndex(nativeSources);
        if (idx.contains(str)) {
            usedByNative = true;
            if (cls != null) {
                cls.setUsedByNative(true);
            }
            return true;
        }
        if (cls != null && !idx.contains(clsName)) {
            // We didn't find the class at all.
            // Let's record that as it will save us time
            // when looking up other methods in this class.
            cls.setUsedByNative(false);
        }
        usedByNative = false;
        return false;
    }
    
    private Set<String> usedMethods;
    public boolean isMethodUsedOldWay(BytecodeMethod bm) {
        ensureUsedMethodsInitialized();
        if(bm.methodName.equals("__INIT__")) {
            return usedMethods.contains(bm.desc + ".<init>");
        }
        return usedMethods.contains(bm.desc + "." + bm.methodName);
    }

    public Set<String> getCalledMethodSignatures() {
        ensureUsedMethodsInitialized();
        return usedMethods;
    }

    public String getLookupSignature() {
        if(methodName.equals("__INIT__")) {
            return desc + ".<init>";
        }
        if(methodName.equals("__CLINIT__")) {
            return desc + ".<clinit>";
        }
        return desc + "." + methodName;
    }

    private void ensureUsedMethodsInitialized() {
        if(usedMethods != null) {
            return;
        }
        usedMethods = new TreeSet<String>();
        for(Instruction ins : instructions) {
            String s = ins.getMethodUsed();
            if(s != null && !usedMethods.contains(s)) {
                usedMethods.add(s);
            }
        }
    }
    
    public void findWritableFields(Set<String> outSet) {
        int len = instructions.size();
        for (int i=0; i<len; i++) {
            Instruction instr = instructions.get(i);
            if (instr instanceof Field) {
                
            }
        }
    }
    
    public static String appendMethodSignatureSuffixFromDesc(String desc, StringBuilder b, List<String> arguments) {
        int currentArrayDim = 0;
        desc = desc.substring(1);
        boolean returnVal = false;
        String returnType = null;
        for(int i = 0 ; i < desc.length() ; i++) {
            char currentType = desc.charAt(i);
            switch(currentType) {
                // return type parsing, and void return type
                case ')':
                case 'V':
                    returnVal = true;
                    continue;
                case '[':
                    // array of...
                    currentArrayDim++;
                    continue;
                case 'L':
                    if(!returnVal) {
                        arguments.add("o");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_OBJECT"; 
                    }
                    // Object skip until ;
                    int idx = desc.indexOf(';', i);
                    String objectType = desc.substring(i + 1, idx);
                    objectType = objectType.replace('/', '_').replace('$', '_');
                    i = idx;
                    b.append("_");
                    b.append(objectType);
                    break;
                case 'I':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_int");
                    break;
                case 'J':
                    if(!returnVal) {
                        arguments.add("l");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_LONG"; 
                    }
                    b.append("_long");
                    break;
                case 'B':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_byte");
                    break;
                case 'S':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_short");
                    break;
                case 'F':
                    if(!returnVal) {
                        arguments.add("f");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_FLOAT"; 
                    }
                    b.append("_float");
                    break;
                case 'D':
                    if(!returnVal) {
                        arguments.add("d");
                    } else {
                        returnType = "JAVA_DOUBLE"; 
                        b.append("_R");
                    }
                    b.append("_double");
                    break;
                case 'Z':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        returnType = "JAVA_INT"; 
                        b.append("_R");
                    }
                    b.append("_boolean");
                    break;
                case 'C':
                    if(!returnVal) {
                        arguments.add("i");
                    } else {
                        b.append("_R");
                        returnType = "JAVA_INT"; 
                    }
                    b.append("_char");
                    break;
            }
            if(currentArrayDim > 0) {
                if(!returnVal) {
                    arguments.remove(arguments.size() - 1);
                    arguments.add("o");
                } else {
                    returnType = "JAVA_OBJECT";                    
                }
                b.append("_");
                b.append(currentArrayDim);
                b.append("ARRAY");
            }
            currentArrayDim = 0;
        }
        return returnType;
    }
    
    public List<String> getDependentClasses() {
        return dependentClasses;
    }
    
    //public List<String> getExportedClasses() {
    //    return exportedClasses;
    //}
    
    private void appendCMethodPrefix(StringBuilder b, String prefix) {
        appendCMethodPrefix(b, prefix, clsName);
    }
    
    private void appendCMethodPrefix(StringBuilder b, String prefix, String clsName) {
        appendCMethodPrefix("\n", "", b, prefix, clsName);
    }
    
    public void appendArgumentTypes(StringBuilder b) {
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
    }
    
    private void appendCMethodPrefix(String before, String after, StringBuilder b, String prefix, String clsName) {
        b.append(before);
        returnType.appendCSig(b);
        b.append(prefix);
        b.append(clsName);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
        b.append(after);
        b.append("(CODENAME_ONE_THREAD_STATE");
        int arg = 1;
        if(!staticMethod) {
            b.append(", ");
            new ByteCodeMethodArg(clsName, 0).appendCSig(b);
            b.append(" __cn1ThisObject");
        }
        for(ByteCodeMethodArg args : arguments) {
            b.append(", ");
            args.appendCSig(b);
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        b.append(")");
    }
    
    public void addToConstantPool() {
        for(Instruction i : instructions) {
            i.addToConstantPool();
        }
    }
    
    public boolean isSynchronizedMethod() {
        return synchronizedMethod;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public List<ByteCodeMethodArg> getArguments() {
        return arguments;
    }

    public ByteCodeMethodArg getReturnType() {
        return returnType;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public String getMethodIdentifier() {
        StringBuilder b = new StringBuilder();
        b.append(clsName).append("_");
        if(methodName.equals("<init>")) {
            b.append("__INIT__");
        } else if(methodName.equals("<clinit>")) {
            b.append("__CLINIT__");
        } else {
            b.append(getCMethodName());
        }
        appendMethodSignatureSuffixFromDesc(desc, b, new ArrayList<String>());
        return b.toString();
    }
    
    private boolean hasLocalVariableWithIndex(char qualifier, int index) {
        for (LocalVariable lv : localVariables) {
            if (lv.getIndex() == index && lv.getQualifier() == qualifier) {
                return true;
            }
        }
        return false;
    }

    /**
     * Emits a stack-allocated {@code void*} array containing the address of
     * each JVM-local slot in the current frame, then stores the array pointer
     * into the per-frame slot {@code callStackLocalsAddresses[offset-1]} that
     * the debugger thread consults to read locals.
     *
     * Slot type is resolved from {@link #localVariables} first (the declared
     * source-level locals) and falls back to method arguments for slots that
     * have no debug info. Slots with no known type are emitted as NULL — the
     * debugger will report them as unavailable.
     *
     * Only called from the non-barebone path; barebone methods carry no
     * locals and bypass this entirely.
     */
    private void appendLocalsAddressTable(StringBuilder b) {
        if (maxLocals <= 0) {
            return;
        }
        char[] slotQual = new char[maxLocals];
        java.util.Arrays.fill(slotQual, ' ');
        for (LocalVariable lv : localVariables) {
            int idx = lv.getIndex();
            if (idx >= 0 && idx < maxLocals) {
                slotQual[idx] = lv.getQualifier();
            }
        }
        int slot = 0;
        if (!staticMethod) {
            if (slotQual[0] == ' ') {
                slotQual[0] = 'o';
            }
            slot = 1;
        }
        for (int i = 0; i < arguments.size() && slot < maxLocals; i++) {
            ByteCodeMethodArg arg = arguments.get(i);
            if (slotQual[slot] == ' ') {
                slotQual[slot] = arg.getQualifier();
            }
            slot++;
            if (arg.isDoubleOrLong() && slot < maxLocals) {
                slot++;
            }
        }

        // Leading newline: callers may have left the cursor mid-line after
        // the "this" assignment when there are no other arguments, and the
        // preprocessor requires #ifdef to be the first non-whitespace token
        // on its line.
        b.append("\n#ifdef CN1_ON_DEVICE_DEBUG\n");
        b.append("    void* __cn1_local_addrs[").append(maxLocals).append("] = { ");
        for (int s = 0; s < maxLocals; s++) {
            if (s > 0) {
                b.append(", ");
            }
            char q = slotQual[s];
            switch (q) {
                case 'o':
                    b.append("&locals[").append(s).append("].data.o");
                    break;
                case 'i':
                case 'l':
                case 'f':
                case 'd':
                    b.append("&").append(q).append("locals_").append(s).append("_");
                    break;
                default:
                    b.append("0");
                    break;
            }
        }
        b.append(" };\n");
        b.append("    threadStateData->callStackLocalsAddresses[threadStateData->callStackOffset - 1] = __cn1_local_addrs;\n");
        b.append("    threadStateData->callStackFrameInfo[threadStateData->callStackOffset - 1] = &__cn1_finfo_").append(getMethodIdentifier()).append(";\n");
        b.append("#endif\n");
    }

    /**
     * Emits the static per-method {@code cn1_frame_info} (and its inline
     * {@code cn1_var_entry[]} side-table). Held at file scope so the
     * per-frame pointer set up in {@link #appendLocalsAddressTable} stays
     * valid for the program's lifetime.
     *
     * The side-table currently lists every declared local with line=0
     * meaning "always live"; refining live-range per source line is left
     * for a follow-up so the proxy can hide locals before their declaration.
     */
    private void appendFrameInfoStruct(StringBuilder b) {
        String id = getMethodIdentifier();
        int classId = Parser.getClassOffset(clsName);
        int methodId = methodOffset;
        b.append("#ifdef CN1_ON_DEVICE_DEBUG\n");
        if (localVariables.isEmpty()) {
            b.append("static const struct cn1_frame_info __cn1_finfo_").append(id).append(" = {\n");
            b.append("    ").append(classId).append(", ").append(methodId).append(", ").append(maxLocals).append(", 0, 0\n");
            b.append("};\n");
        } else {
            b.append("static const struct cn1_var_entry __cn1_vars_").append(id).append("[] = {\n");
            for (LocalVariable lv : localVariables) {
                b.append("    { 0, ").append(lv.getIndex()).append(", '").append(lv.getTypeCode()).append("' },\n");
            }
            b.append("};\n");
            b.append("static const struct cn1_frame_info __cn1_finfo_").append(id).append(" = {\n");
            b.append("    ").append(classId).append(", ").append(methodId).append(", ").append(maxLocals).append(", ").append(localVariables.size()).append(", __cn1_vars_").append(id).append("\n");
            b.append("};\n");
        }
        b.append("#endif\n");
    }
    
    private void fixUpBarebone() {
        for (Instruction i : instructions) {
            if (i instanceof CustomJump) {
                CustomJump cj = (CustomJump)i;
                String cmp = cj.getCustomCompareCode();
                if (cmp != null) {
                    cj.setCustomCompareCode(cmp.replaceAll("locals\\[(\\d+)\\]\\.data\\.o", "olocals_$1_"));
                }
            } else if (i instanceof CustomIntruction) {
                CustomIntruction ci = (CustomIntruction)i;
                String code = ci.getCode();
                if (code != null) {
                    ci.setCode(code.replaceAll("locals\\[(\\d+)\\]\\.data\\.o", "olocals_$1_"));
                }
                String complexCode = ci.getComplexCode();
                if (complexCode != null) {
                    ci.setComplexCode(complexCode.replaceAll("locals\\[(\\d+)\\]\\.data\\.o", "olocals_$1_"));
                }
            } else if (i instanceof CustomInvoke) {
                CustomInvoke ci = (CustomInvoke)i;
                String target = ci.getTargetObjectLiteral();
                if (target != null) {
                    ci.setTargetObjectLiteral(target.replaceAll("locals\\[(\\d+)\\]\\.data\\.o", "olocals_$1_"));
                }
                String[] args = ci.getLiteralArgs();
                if (args != null) {
                    for (int j=0; j<args.length; j++) {
                        if (args[j] != null) {
                            ci.setLiteralArg(j, args[j].replaceAll("locals\\[(\\d+)\\]\\.data\\.o", "olocals_$1_"));
                        }
                    }
                }
            }
        }
    }

    public void appendMethodC(StringBuilder b) {
        if(nativeMethod) {
            return;
        }
        if (onDeviceDebug && !eliminated) {
            appendFrameInfoStruct(b);
        }
        appendCMethodPrefix(b, "");
        b.append(" {\n");
        if(eliminated) {
            if(returnType.isVoid()) {
                b.append("    return;\n}\n\n");
            } else {
                b.append("    return 0;\n}\n\n");
            }
            return;
        }
            
        b.append(declaration);
        boolean fastMethodStackCandidate = canUseFastMethodStack();

        boolean hasInstructions = true;
        if(optimizerOn) {
            hasInstructions = optimize();
        }
        
        if(hasInstructions) {
            barebone = checkBarebone();
            if (barebone) {
                fixUpBarebone();
            }
            // Local autos only need to be `volatile` when a setjmp/longjmp can land
            // back in THIS frame -- i.e. the method has a try/catch (so it emits
            // setjmp) -- or when the on-device debugger must inspect them. A method
            // with no try/catch is unwound *past* when a callee throws, so its
            // primitive autos are never read after a longjmp and need not be
            // volatile. Dropping volatile there lets the C compiler register-allocate
            // and vectorize hot loops (e.g. array reductions ran ~3x faster). It is
            // GC-safe: primitive autos are never GC roots (object locals live in the
            // scanned threadObjectStack, not here). setjmp-frame methods keep
            // volatile for longjmp correctness.
            // synchronizedMethod is included conservatively: a synchronized method
            // releases its monitor during exception unwinding and carries no explicit
            // TryCatch instruction, so we cannot prove its frame is never re-entered
            // by the unwind machinery -- keep its locals volatile.
            //
            // A call in the body also forces volatile, as a PERFORMANCE heuristic
            // (non-volatile is still correct without it): non-volatile locals in a
            // loop that makes calls (e.g. libm sqrt/sin/cos) make clang juggle
            // callee-saved registers across each call and run slower, whereas
            // call-free compute loops (array reductions, integer kernels) vectorize
            // and register-allocate dramatically better without volatile. Inlined
            // accessors no longer count as calls, so this composes with inlining.
            // -DCN1_FORCE_VOLATILE_LOCALS=true restores the always-volatile behavior
            // (off-switch for the non-volatile-locals optimization).
            boolean volatileLocals = FORCE_VOLATILE_LOCALS || onDeviceDebug || synchronizedMethod;
            if (!volatileLocals) {
                for (Instruction tcScan : instructions) {
                    if (tcScan instanceof TryCatch || tcScan instanceof Invoke || tcScan instanceof CustomInvoke) {
                        volatileLocals = true;
                        break;
                    }
                }
            }
            Set<String> added = new HashSet<String>();
            for (LocalVariable lv : localVariables) {
                String variableName = lv.getQualifier() + "locals_"+lv.getIndex()+"_";
                if (!added.contains(variableName) && (barebone || lv.getQualifier() != 'o')) {
                    added.add(variableName);
                    b.append("    ");
                    if (volatileLocals) {
                        b.append("volatile ");
                    }
                    switch (lv.getQualifier()) {
                        case 'i' :
                            b.append("JAVA_INT"); break;
                        case 'l' :
                            b.append("JAVA_LONG"); break;
                        case 'f' :
                            b.append("JAVA_FLOAT"); break;
                        case 'd' :
                            b.append("JAVA_DOUBLE"); break;
                        case 'o' :
                            b.append("JAVA_OBJECT"); break;
                    }
                    b.append(" ").append(lv.getQualifier()).append("locals_").append(lv.getIndex()).append("_ = 0; /* ").append(lv.getOrigName()).append(" */\n");
                }
            }
            
            boolean useFastMethodStack = !barebone && fastMethodStackCandidate;
            boolean usePrimitiveFastFrame = useFastMethodStack && isPrimitiveOnlyFastFrameCandidate();
            fastMethodStackInUse = useFastMethodStack;
            fastMethodStackPrimitiveOnly = usePrimitiveFastFrame;
            if(!barebone) {
                if(staticMethod) {
                    if(methodName.equals("__CLINIT__")) {
                        if (useFastMethodStack) {
                            if (usePrimitiveFastFrame) {
                                b.append("    DEFINE_METHOD_STACK_FAST_PRIMITIVE(");
                            } else {
                                b.append("    DEFINE_METHOD_STACK_FAST_REF(");
                            }
                        } else {
                            b.append("    DEFINE_METHOD_STACK(");
                        }
                    } else {
                        b.append("    if (!class__");
                        b.append(clsName.replace('/', '_').replace('$', '_'));
                        b.append(".initialized) __STATIC_INITIALIZER_");
                        b.append(clsName.replace('/', '_').replace('$', '_'));
                        if (useFastMethodStack) {
                            if (usePrimitiveFastFrame) {
                                b.append("(threadStateData);\n    DEFINE_METHOD_STACK_FAST_PRIMITIVE(");
                            } else {
                                b.append("(threadStateData);\n    DEFINE_METHOD_STACK_FAST_REF(");
                            }
                        } else {
                            b.append("(threadStateData);\n    DEFINE_METHOD_STACK(");
                        }
                    }
                } else {
                    if (useFastMethodStack) {
                        if (usePrimitiveFastFrame) {
                            b.append("    DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE(");
                        } else {
                            b.append("    DEFINE_INSTANCE_METHOD_STACK_FAST_REF(");
                        }
                    } else {
                        b.append("    DEFINE_INSTANCE_METHOD_STACK(");
                    }
                }
                b.append(maxStack);
                b.append(", ");
                b.append(maxLocals);
                b.append(", 0");
                if (!useFastMethodStack) {
                    b.append(", ");
                    b.append(Parser.addToConstantPool(clsName));
                    b.append(", ");
                    b.append(Parser.addToConstantPool(methodName));
                }
                b.append(");\n");
            } else {
                b.append("    struct elementStruct* SP = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset];\n");
            }
            int startOffset = 0;
            if(synchronizedMethod) {
                if(staticMethod) {
                    b.append("    monitorEnterBlock(threadStateData, (JAVA_OBJECT)&class__");
                    b.append(clsName);
                    b.append(");\n");
                } else {
                    b.append("    monitorEnterBlock(threadStateData, __cn1ThisObject);\n");
                }
            }
            if(!staticMethod) {
                if(!barebone) {
                    b.append("    locals[0].data.o = __cn1ThisObject; locals[0].type = CN1_TYPE_OBJECT; ");
                }
                startOffset++;
            }
            int localsOffset = startOffset;
            for(int iter = 0 ; iter < arguments.size() ; iter++) {
                ByteCodeMethodArg arg = arguments.get(iter);
                if (arg.getQualifier() == 'o') {
                    if(barebone) {
                        b.append("    JAVA_OBJECT olocals_");
                        b.append(localsOffset);
                        b.append("_ = __cn1Arg");
                        b.append(iter + 1);
                        b.append(";\n");
                    } else {
                        b.append("    locals[");
                        b.append(localsOffset);
                        b.append("].data.");

                        b.append(arg.getQualifier());
                        b.append(" = __cn1Arg");
                        b.append(iter + 1);
                        b.append(";\n");
                        b.append("    locals[");
                        b.append(localsOffset);
                        b.append("].type = CN1_TYPE_OBJECT;\n");
                    }
                } else {
                    b.append("    ");
                    if (!hasLocalVariableWithIndex(arg.getQualifier(), localsOffset)) {
                        switch (arg.getQualifier()) {
                            case 'i' : b.append("JAVA_INT"); break;
                            case 'f' : b.append("JAVA_FLOAT"); break;
                            case 'd' : b.append("JAVA_DOUBLE"); break;
                            case 'l' : b.append("JAVA_LONG"); break;
                            default: b.append("JAVA_INT"); break;
                        }
                        b.append(" ");
                        
                    }
                    b.append(arg.getQualifier());
                    b.append("locals_");
                    b.append(localsOffset);
                    b.append("_");
                    b.append(" = __cn1Arg");
                    b.append(iter + 1);
                    b.append(";\n");
                }
                // For now we'll still allocate space for locals that we're not using
                // so we keep the indexes the same for objects.
                localsOffset++;
                if(arg.isDoubleOrLong()) {
                    localsOffset++;
                }
            }
            if (onDeviceDebug && !barebone) {
                appendLocalsAddressTable(b);
            }
        } else {
            if(synchronizedMethod) {
                if(staticMethod) {
                    b.append("    monitorEnterBlock(threadStateData, (JAVA_OBJECT)&class__");
                    b.append(clsName);
                    b.append(");\n");
                } else {
                    b.append("    monitorEnterBlock(threadStateData, __cn1ThisObject);\n");
                }
            }
        }
        
        BasicInstruction.setSynchronizedMethod(synchronizedMethod, staticMethod, clsName);
        TryCatch.reset();
        BasicInstruction.setHasInstructions(hasInstructions);
        // Annotation-driven stack allocation: every NEW of a @StackAllocate class
        // gets one method-scoped struct, reused across loop iterations (only one
        // instance per site is live at a time -- the annotation's contract). The
        // struct lives for the whole frame so the object's references stay valid
        // until it dies with the frame; the matching TypeInstruction NEW codegen
        // initializes the header and pushes its address instead of heap-allocating.
        for(int saIter = 0 ; saIter < instructions.size() ; saIter++) {
            Instruction saInst = instructions.get(saIter);
            if(saInst instanceof TypeInstruction) {
                String saType = ((TypeInstruction)saInst).getStackAllocType();
                if(saType != null) {
                    b.append("    struct obj__").append(saType).append(" __cn1stk_").append(saIter).append(";\n");
                    ((TypeInstruction)saInst).setStackAllocId(saIter);
                }
            }
        }
        for(Instruction i : instructions) {
            i.setMethod(this);
            i.setMaxes(maxStack, maxLocals);
            i.appendInstruction(b, instructions);
        }
        if(instructions.size() == 0) {
            if(returnType.isVoid()) {
                b.append("    return;\n}\n\n");
            } else {
                b.append("    return 0;\n}\n\n");
            }
            return;
        }
        Instruction inst = instructions.get(instructions.size() - 1);
        int lastInstruction = inst.getOpcode();
        if(lastInstruction == -1 || inst instanceof LabelInstruction) {
            if(instructions.size() > 2) {
                inst = instructions.get(instructions.size() - 2);
                lastInstruction = inst.getOpcode();
            }
        }
        if(lastInstruction == Opcodes.RETURN || lastInstruction == Opcodes.ARETURN || lastInstruction == Opcodes.IRETURN || lastInstruction == Opcodes.LRETURN ||
                lastInstruction == Opcodes.FRETURN || lastInstruction == Opcodes.DRETURN || lastInstruction == -1) {
            b.append("}\n\n");
        } else {
            if(returnType.isVoid()) {
                b.append("    return;\n}\n\n");
            } else {
                b.append("    return 0;\n}\n\n");
            }
        }
    }
    
    public void appendInterfaceMethodC(StringBuilder b) {
        appendInterfaceMethodC(b, clsName);
    }

    public void appendInterfaceMethodC(StringBuilder b, String clsName) {
        appendCMethodPrefix(b, "", clsName);
        b.append(" {\n");
        if(!returnType.isVoid()) {
            b.append("return virtual_");
        } else {
            b.append("virtual_");            
        }
        b.append(clsName);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
        b.append("(threadStateData");
        
        int arg = 1;
        b.append(", __cn1ThisObject");
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            b.append(", ");
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        b.append(");\n}\n\n");
    }

    public void appendSuperCall(StringBuilder b, String cls) {
        if(nativeMethod) {
            return;
        }
        appendCMethodPrefix(b, "", cls);
        b.append(" {\n");
        if(!returnType.isVoid()) {
            b.append("    return ");
        } 
        b.append(clsName);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
        b.append("(threadStateData");
        int arg = 1;
        if(!staticMethod) {
            b.append(", __cn1ThisObject");
        }
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            b.append(", ");
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        
        b.append(");\n}\n\n");
    }

    public void appendMethodHeader(StringBuilder b) {
        appendMethodHeader(b, clsName);
    }
    
    public void appendMethodHeader(StringBuilder b, String clsName) {
        appendCMethodPrefix(b, "", clsName);
        b.append(";\n");
    }
    
    public void appendVirtualMethodC(String cls, StringBuilder b, int offset) {
        appendVirtualMethodC(cls, b, Integer.toString(offset));
    }
    
    public void appendVirtualMethodC(String cls, StringBuilder b, String offset) {
        appendVirtualMethodC(cls, b, offset, false);
    }
    
    public static void addVirtualMethodsInvoked(String m) {
        if(!virtualMethodsInvoked.contains(m)) {
            virtualMethodsInvoked.add(m);
        }
    }
    
    public void setForceVirtual(boolean forceVirtual) {
        this.forceVirtual = forceVirtual;
    }
    
    public boolean isForceVirtual() {
        return forceVirtual;
    }
    
    public String getFullCName() {
        return this.clsName + "_"+this.getCMethodName();
    }
    
    public void appendVirtualMethodC(String cls, StringBuilder b, String offset, boolean includeStaticInitializer) {
        if(virtualOverriden) {
            return;
        }
        StringBuilder bld = new StringBuilder();
        bld.append(cls);
        bld.append("_");
        bld.append(getCMethodName());
        bld.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(bld);
        }
        if(!returnType.isVoid()) {
            bld.append("_R");
            returnType.appendCMethodExt(bld);
        }
        
        // generate the function pointer declaration
        appendCMethodPrefix("\ntypedef ", ")", b, "(*functionPtr_", cls);
        b.append(";\n");
        
        appendCMethodPrefix(b, "virtual_", cls);
        b.append(" {\n    ");

        // Devirtualize the tagged-Integer fast path for the hottest collection methods.
        // A tagged receiver is ALWAYS an Integer, so its hashCode IS the untagged value --
        // turning HashMap's per-lookup key.hashCode() into a bare inline untag with no
        // indirect dispatch and no call. equals between two tagged ints is a pointer
        // compare (tags are canonical); the mixed case falls through to normal dispatch.
        String cn1mn = getCMethodName();
        if(cn1mn.equals("hashCode") && arguments.isEmpty() && !returnType.isVoid()) {
            b.append("\n#if CN1_TAGGED_ACTIVE\n    if(CN1_IS_TAGGED(__cn1ThisObject)) { return CN1_UNTAG_INT(__cn1ThisObject); }\n#endif\n    ");
        } else if(cn1mn.equals("equals") && arguments.size() == 1 && !returnType.isVoid()) {
            b.append("\n#if CN1_TAGGED_ACTIVE\n    if(CN1_IS_TAGGED(__cn1ThisObject) && CN1_IS_TAGGED(__cn1Arg1)) { return (__cn1ThisObject == __cn1Arg1) ? JAVA_TRUE : JAVA_FALSE; }\n#endif\n    ");
        }

        if(includeStaticInitializer) {
            b.append("__STATIC_INITIALIZER_");
            b.append(cls);
            b.append("(threadStateData);\n    ");
        }
        if (System.getProperty("INCLUDE_NPE_CHECKS", "false").equals("true")) {
            b.append("\n    if(__cn1ThisObject == JAVA_NULL) THROW_NULL_POINTER_EXCEPTION();\n    ");
        } 
        if(!returnType.isVoid()) {
            b.append("return (*(functionPtr_");
        } else {
            b.append("(*(functionPtr_");            
        }
        b.append(bld);
        b.append(")CN1_CLASS_OF(__cn1ThisObject)->vtable[");
        b.append(offset);
        b.append("])(threadStateData, ");
        
        int arg = 1;
        b.append("__cn1ThisObject");
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            b.append(", ");
            b.append("__cn1Arg");
            b.append(arg);
            arg++;
        }        
        b.append(");\n}\n\n");
    }

    public void appendVirtualMethodHeader(StringBuilder b, String cls) {
        StringBuilder bld = new StringBuilder();
        bld.append(cls);
        bld.append("_");
        bld.append(getCMethodName());
        bld.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(bld);
        }
        if(!returnType.isVoid()) {
            bld.append("_R");
            returnType.appendCMethodExt(bld);
        }
        appendCMethodPrefix(b, "virtual_", cls);
        b.append(";\n");
    }

    public void appendFunctionPointer(StringBuilder b) {
        appendFunctionPointer(b, clsName);
    }

    public void appendFunctionPointer(StringBuilder b, String className) {
        b.append(className);
        b.append("_");
        b.append(getCMethodName());
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }        
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BytecodeMethod)) {
            return false;
        }

        BytecodeMethod bm = (BytecodeMethod)o;

        if (!methodName.equals(bm.methodName)) {
            return false;
        }
        if (acceptStaticOnEquals) {
            if (bm.arguments.size() != arguments.size()) {
                return false;
            }
        } else {
            if (staticMethod || bm.staticMethod || bm.arguments.size() != arguments.size()) {
                return false;
            }
        }

        for (int iter = 0; iter < arguments.size(); iter++) {
            ByteCodeMethodArg arg1 = arguments.get(iter);
            ByteCodeMethodArg arg2 = bm.arguments.get(iter);
            if (!arg1.equals(arg2)) {
                return false;
            }
        }

        if (returnType == null) {
            return bm.returnType == null;
        }
        return returnType.equals(bm.returnType);
    }

    public int hashCode() {
        int result = methodName == null ? 0 : methodName.hashCode();
        result = 31 * result + arguments.size();
        result = 31 * result + (acceptStaticOnEquals || !staticMethod ? 0 : 1);
        result = 31 * result + (returnType == null ? 0 : returnType.hashCode());
        return result;
    }
    
    /**
     * JS-target-only flag. When true the method body may transitively
     * block the cooperative scheduler (sleep / wait / monitor entry /
     * native host bridge) and must be emitted as ``function*`` with
     * ``yield*`` at every call site. When false the method can run
     * straight through and is emitted as a regular ``function`` —
     * callers invoke it directly, with no generator allocation per
     * call. Computed by {@link JavascriptSuspensionAnalysis}.
     */
    private boolean javascriptSuspending = true;

    public boolean isJavascriptSuspending() {
        return javascriptSuspending;
    }

    public void setJavascriptSuspending(boolean value) {
        this.javascriptSuspending = value;
    }

    public boolean isStatic() {
        return staticMethod;
    }

    public boolean isPrivate() {
        return privateMethod;
    }
    
    /*public boolean isVirtualBlockedDueToFinal() {
        return (!privateMethod && !staticMethod && !constructor) && finalMethod;
    }*/
    
    public boolean canBeVirtual() {
        return !privateMethod && !staticMethod && !constructor;
    }
    
    public boolean isNative() {
        return nativeMethod;
    }

    public boolean isAbstract() {
        return abstractMethod;
    }
    
    public String getVariableNameForTypeIndex(int index, char type) {
        for(Instruction i : instructions) {
            if(i instanceof LocalVariable) {
                if(((LocalVariable)i).isRightVariable(index, type)) {
                    return ((LocalVariable)i).getVarName();
                }
            } else {
                return null;
            }
            
        }
        return null;
    }
    
    public void addMultiArray(String desc, int dims) {
        addInstruction(new MultiArray(desc, dims));
    }
    
    public void addTryCatchBlock(Label start, Label end, Label handler, String type) {
        addInstruction(new TryCatch(start, end, handler, type));
    }
    
    public void addLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        if (disableDebugInfo) {
            return;
        }
        //addInstruction(0, new LocalVariable(name, desc, signature, start, end, index));
        localVariables.add(new LocalVariable(name, desc, signature, start, end, index));
    }
    
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getDesc() {
        return desc;
    }

    public Set<LocalVariable> getLocalVariables() {
        return localVariables;
    }
    
    public void addDebugInfo(int line) {
        if (disableDebugInfo) {
            return;
        }
        addInstruction(new LineNumber(sourceFile, line));
    }
    
    public void addLabel(Label l) {
        addInstruction(new com.codename1.tools.translator.bytecodes.LabelInstruction(l));
    }
    
    public void addInvoke(int opcode, String owner, String name, String desc, boolean itf) {
        addInstruction(new Invoke(opcode, owner, name, desc, itf));
    }
    
    public void setMaxes(int maxStack, int maxLocals) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
    }
    
    private void addInstruction(Instruction i) {
        instructions.add(i);
        i.setMethod(this);
        i.addDependencies(dependentClasses);
        if (dependencyGraph != null) {
            String methodUsed = i.getMethodUsed();
            if (methodUsed != null) {
                dependencyGraph.recordMethodCall(this, methodUsed);
            }
        }
    }
    
    public void addVariableOperation(int opcode, int var) {
        VarOp op = new VarOp(opcode, var);
        LocalVariable lv = null;
        switch (opcode) {
            case Opcodes.ISTORE:
                lv = new LocalVariable("v"+var, "I", "I", null, null, var); break;
            case Opcodes.LSTORE:
                lv = new LocalVariable("v"+var, "J", "J", null, null, var); break;    
            case Opcodes.FSTORE:
                lv = new LocalVariable("v"+var, "F", "F", null, null, var); break;
            case Opcodes.DSTORE:
                lv = new LocalVariable("v"+var, "D", "D", null, null, var); break;
        }
        if (lv != null && !localVariables.contains(lv)) {
            localVariables.add(lv);
        }
        addInstruction(op);
    }
    
    public void addTypeInstruction(int opcode, String type) {
        addInstruction(new TypeInstruction(opcode, type));
    }
    
    /**
     * Allows us to detect if this is a very simple getter/setter in which case we 
     * can significantly optimize some operations
     */
    public boolean hasExceptionHandlingOrMethodCalls() {
        for(Instruction i : instructions) {
            if(i.isComplexInstruction()) {
                return true;
            }
        }
        return false;
    }
    
    public void addIInc(int var, int num) {
        addInstruction(new IInc(var, num));
    }

    public void addLdc(Object o) {
        addInstruction(new Ldc(o));
    }
    
    public void addJump(int opcode, Label label) {
        addInstruction(new Jump(opcode, label));
    }

    public void addField(ByteCodeClass cls, int opcode, String owner, String name, String desc) {
        if (cls.getOriginalClassName().equals(owner) && (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC)) {
            cls.addWritableField(name);
        }
        addInstruction(new Field(opcode, owner, name, desc));
    }
    
    public void addInstruction(int opcode) {
        addInstruction(new BasicInstruction(opcode, 0));
    }
    
    public void addInstruction(int opcode, int value) {
        addInstruction(new BasicInstruction(opcode, value));
    }
    
    public void addSwitch(Label dflt, int[] keys, Label[] labels) {
        addInstruction(new SwitchInstruction(dflt, keys, labels));
    }

    /**
     * @return the methodOffset
     */
    public int getMethodOffset() {
        return methodOffset;
    }

    /**
     * Emits the debugger-driven invoke thunk for this method. The thunk
     * has a uniform C signature so the registry can store all thunks in
     * one function-pointer table; it unpacks args from a {@code cn1_invoke_arg}
     * union array into the typed parameters the underlying translated
     * function expects, wraps the call in a catch-all try block so an
     * uncaught throw turns into {@code result.type='X'} rather than a
     * longjmp past the debugger's cond-wait, and packs the return value
     * back into the result union.
     */
    public void appendOnDeviceDebugInvokeThunk(String declaringClsName, StringBuilder b) {
        String symbol = declaringClsName + "_";
        if ("<init>".equals(methodName)) {
            // skipped at caller, but defensive
            return;
        } else if ("<clinit>".equals(methodName)) {
            return;
        }
        symbol += getCMethodName();
        // Append the descriptor suffix the translator uses
        // (args + _R<return> for non-void).
        StringBuilder argSuffix = new StringBuilder();
        for (ByteCodeMethodArg arg : arguments) {
            arg.appendCMethodExt(argSuffix);
        }
        if (!returnType.isVoid()) {
            argSuffix.append("_R");
            returnType.appendCMethodExt(argSuffix);
        }
        String fullSymbol = symbol + "__" + argSuffix.toString();
        // Choose virtual_<sym> only when the translator actually emits
        // one. Static, private, or methods marked virtualOverriden (the
        // class is final, so the dispatch was constant-folded) have no
        // virtual_ alias in their header, and the thunk has to call the
        // plain symbol or the C file won't compile.
        boolean useVirtualPrefix = !staticMethod && !privateMethod && !virtualOverriden;
        String callSymbol = useVirtualPrefix ? ("virtual_" + fullSymbol) : fullSymbol;
        int mid = methodOffset;

        b.append("static void __cn1_dbg_invoke_").append(mid)
          .append("(struct ThreadLocalData* threadStateData, JAVA_OBJECT thisObj, const cn1_invoke_arg* args, cn1_invoke_result* result) {\n");
        b.append("    (void)args; (void)thisObj;\n");
        b.append("    int __savedCallStack = threadStateData->callStackOffset;\n");
        b.append("    int __savedLocalsBegin = threadStateData->threadObjectStackOffset;\n");
        b.append("    int __savedTryBlock = threadStateData->tryBlockOffset;\n");
        b.append("    jmp_buf __tryJmp;\n");
        b.append("    if (setjmp(__tryJmp) == 0) {\n");
        b.append("        threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0;\n");
        b.append("        threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = 0;\n");
        b.append("        memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, __tryJmp, sizeof(jmp_buf));\n");
        b.append("        threadStateData->tryBlockOffset++;\n");
        // Emit the actual call
        b.append("        ");
        if (returnType.isVoid()) {
            b.append(callSymbol).append("(threadStateData");
        } else {
            // Capture return into a typed temp, then pack.
            char rq = returnType.getQualifier();
            if (rq == 'o') b.append("JAVA_OBJECT __r = ");
            else if (rq == 'l') b.append("JAVA_LONG __r = ");
            else if (rq == 'd') b.append("JAVA_DOUBLE __r = ");
            else if (rq == 'f') b.append("JAVA_FLOAT __r = ");
            else b.append("JAVA_INT __r = ");
            b.append(callSymbol).append("(threadStateData");
        }
        if (!staticMethod) {
            b.append(", thisObj");
        }
        for (int i = 0; i < arguments.size(); i++) {
            ByteCodeMethodArg arg = arguments.get(i);
            char q = arg.getQualifier();
            b.append(", args[").append(i).append("].");
            switch (q) {
                case 'o': b.append("o"); break;
                case 'l': b.append("j"); break;
                case 'd': b.append("d"); break;
                case 'f': b.append("f"); break;
                default:  b.append("i"); break;
            }
        }
        b.append(");\n");
        // Pop our try block (no exception path) and store the result.
        b.append("        threadStateData->tryBlockOffset--;\n");
        if (returnType.isVoid()) {
            b.append("        result->type = 'V';\n");
        } else {
            char rq = returnType.getQualifier();
            String rtc;
            String slot;
            if (rq == 'o') { rtc = "L"; slot = "o"; }
            else if (rq == 'l') { rtc = "J"; slot = "j"; }
            else if (rq == 'd') { rtc = "D"; slot = "d"; }
            else if (rq == 'f') { rtc = "F"; slot = "f"; }
            else {
                // Sub-int types still pack through the int slot;
                // we communicate the real type via the type-char.
                String d = returnType.getQualifier() == 'i' ? returnTypeChar() : "I";
                rtc = d;
                slot = "i";
            }
            b.append("        result->type = '").append(rtc).append("';\n");
            b.append("        result->value.").append(slot).append(" = __r;\n");
        }
        b.append("    } else {\n");
        b.append("        result->type = 'X';\n");
        b.append("        result->value.o = threadStateData->exception;\n");
        b.append("        threadStateData->exception = JAVA_NULL;\n");
        b.append("        threadStateData->callStackOffset = __savedCallStack;\n");
        b.append("        threadStateData->threadObjectStackOffset = __savedLocalsBegin;\n");
        b.append("        threadStateData->tryBlockOffset = __savedTryBlock;\n");
        b.append("    }\n");
        b.append("}\n");
    }

    /**
     * Returns the JDWP type-char for the method's return type. Only used
     * by {@link #appendOnDeviceDebugInvokeThunk} for sub-int primitive
     * returns where the C variable is JAVA_INT but the wire-level type
     * is more specific (e.g. boolean / byte / short / char).
     */
    private String returnTypeChar() {
        if (returnType.getPrimitiveType() == Boolean.TYPE) return "Z";
        if (returnType.getPrimitiveType() == Byte.TYPE)    return "B";
        if (returnType.getPrimitiveType() == Short.TYPE)   return "S";
        if (returnType.getPrimitiveType() == Character.TYPE) return "C";
        return "I";
    }

    /**
     * @param methodOffset the methodOffset to set
     */
    public void setMethodOffset(int methodOffset) {
        this.methodOffset = methodOffset;
    }

    /**
     * @return the staticMethod
     */
    public boolean isMain() {
        return staticMethod && methodName.equals("main") && arguments.size() == 1 && arguments.get(0).getArrayDimensions() == 1;
    }
    
    public boolean isDefaultConstructor() {
        return constructor && arguments.size() == 0;
    }

    private String cMethodName;
    
    /**
     * Gets the method name, mangled to be usable as the C method name.  This will replace illegal characters
     * with underscores.
     * @return 
     */
    public String getCMethodName() {
        if (cMethodName == null) {
            cMethodName = methodName.replace('-','_');
        }
        return cMethodName;
    }
    
    /**
     * @return the clsName
     */
    public String getClsName() {
        return clsName;
    }
    
    public boolean isFinalizer() {
        return methodName.equals("finalize") && arguments.size() == 0;
    }

    /**
     * @return the virtualOverriden
     */
    public boolean isVirtualOverriden() {
        return virtualOverriden;
    }

    /** True if the method is declared {@code final} (cannot be overridden). */
    public boolean isFinal() {
        return finalMethod;
    }

    /**
     * @param virtualOverriden the virtualOverriden to set
     */
    public void setVirtualOverriden(boolean virtualOverriden) {
        this.virtualOverriden = virtualOverriden;
    }

    /**
     * @return the eliminated
     */
    public boolean isEliminated() {
        return eliminated;
    }

    /**
     * @param eliminated the eliminated to set
     */
    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }



    private int varCounter = 0;
    // Master off-switch: -DCN1_DISABLE_BCE=true reverts to fully-checked array access.
    private static final boolean DISABLE_BCE =
            "true".equalsIgnoreCase(System.getProperty("CN1_DISABLE_BCE", "false"));

    /**
     * Prove-safe array-bounds-check elimination. Conservative and fail-closed:
     * marks an array LOAD bounds-safe only for the canonical counted loop
     * {@code for (int i = <const >= 0>; i < arr.length; i++) { ... arr[i] ... }}
     * recognised in the RAW (pre-reduction) bytecode IR. Because the loop
     * condition re-evaluates {@code arr.length} every iteration, entering the body
     * proves {@code arr != null} and {@code i < arr.length}; we additionally prove
     * {@code i} is monotonic non-negative and that neither {@code i} nor
     * {@code arr} is mutated between the loop top and the access, and that nothing
     * branches into the body bypassing the test. Any construct we don't model
     * precisely (try/catch, switch, computed jumps, non-constant init) bails.
     */
    void analyzeBoundsChecks() {
        if (DISABLE_BCE) {
            return;
        }
        // View without LineNumber noise but keeping labels for back-edge detection.
        java.util.ArrayList<Instruction> r = new java.util.ArrayList<Instruction>(instructions.size());
        for (Instruction in : instructions) {
            if (in instanceof LineNumber) {
                continue;
            }
            // Control flow we don't model precisely -> disable BCE for the whole method.
            if (in instanceof SwitchInstruction || in instanceof CustomJump || in instanceof TryCatch) {
                return;
            }
            r.add(in);
        }
        if (TryCatch.isTryCatchInMethod()) {
            return;
        }
        int n = r.size();
        java.util.HashMap<Label, Integer> pos = new java.util.HashMap<Label, Integer>();
        for (int i = 0; i < n; i++) {
            Instruction x = r.get(i);
            if (x instanceof LabelInstruction) {
                pos.put(((LabelInstruction) x).getLabel(), i);
            }
        }
        // Canonical javac top-test counted loop:
        //   ISTORE i(=const>=0)
        //   header: ILOAD i ; ALOAD a ; ARRAYLENGTH ; IF_ICMPGE exit   (forward)
        //   body:   ... a[i] ...
        //   IINC i,+k ; GOTO header
        //   exit:
        // Falling through the IF_ICMPGE proves i < a.length (and a != null, since
        // ARRAYLENGTH ran); the body is dominated by that test.
        for (int jx = 4; jx < n; jx++) {
            Instruction ji = r.get(jx);
            if (!(ji instanceof Jump) || ji.getOpcode() != Opcodes.IF_ICMPGE) {
                continue;
            }
            Jump j = (Jump) ji;
            Integer exitP = pos.get(j.getLabel());
            if (exitP == null || exitP <= jx) {
                continue; // exit must be a forward target
            }
            int exit = exitP;
            // condition shape:  ILOAD i ; ALOAD a ; ARRAYLENGTH ; IF_ICMPGE exit
            Instruction len = r.get(jx - 1);
            Instruction arr = r.get(jx - 2);
            Instruction idx = r.get(jx - 3);
            Instruction hdr = r.get(jx - 4);
            if (len.getOpcode() != Opcodes.ARRAYLENGTH) continue;
            if (!(arr instanceof VarOp) || arr.getOpcode() != Opcodes.ALOAD) continue;
            if (!(idx instanceof VarOp) || idx.getOpcode() != Opcodes.ILOAD) continue;
            if (!(hdr instanceof LabelInstruction)) continue;     // header label the back-edge returns to
            int header = jx - 4;
            int arrVar = ((VarOp) arr).getIndex();
            int indVar = ((VarOp) idx).getIndex();
            if (arrVar == indVar) continue;
            if (!bceInductionMonotonicNonNegative(r, indVar)) continue;
            if (bceCountJumpsTargeting(r, header) != 1) continue;          // only the back-edge enters the header
            if (bceLocalWrittenInRange(r, arrVar, jx, exit)) continue;     // array invariant in loop body

            for (int k = jx + 1; k < exit; k++) {
                Instruction ld = r.get(k);
                if (!bceIsArrayLoadOpcode(ld.getOpcode())) continue;
                Instruction li = r.get(k - 1);
                Instruction la = r.get(k - 2);
                if (!(li instanceof VarOp) || li.getOpcode() != Opcodes.ILOAD || ((VarOp) li).getIndex() != indVar) continue;
                if (!(la instanceof VarOp) || la.getOpcode() != Opcodes.ALOAD || ((VarOp) la).getIndex() != arrVar) continue;
                if (bceLocalWrittenInRange(r, indVar, jx, k)) continue;     // i unchanged test->access
                if (bceForeignEntry(r, pos, header, k, j)) continue;        // no bypass entry into cond/body
                ld.markBoundsSafe();
            }
        }
    }

    private static boolean bceIsArrayLoadOpcode(int op) {
        switch (op) {
            case Opcodes.IALOAD: case Opcodes.LALOAD: case Opcodes.FALOAD:
            case Opcodes.DALOAD: case Opcodes.AALOAD: case Opcodes.BALOAD:
            case Opcodes.CALOAD: case Opcodes.SALOAD: return true;
            default: return false;
        }
    }

    // i is written only by IInc(+positive) and ISTORE of a non-negative int constant,
    // with at least one such initializing store. Anything else -> not provably >= 0.
    private static boolean bceInductionMonotonicNonNegative(java.util.List<Instruction> r, int v) {
        boolean hasInit = false;
        for (int i = 0; i < r.size(); i++) {
            Instruction in = r.get(i);
            if (in instanceof IInc && ((IInc) in).getVar() == v) {
                if (((IInc) in).getAmount() <= 0) return false;
            } else if (in instanceof VarOp && in.getOpcode() == Opcodes.ISTORE && ((VarOp) in).getIndex() == v) {
                if (i == 0) return false;
                int srcOp = r.get(i - 1).getOpcode();
                if (srcOp != Opcodes.ICONST_0 && srcOp != Opcodes.ICONST_1 && srcOp != Opcodes.ICONST_2
                        && srcOp != Opcodes.ICONST_3 && srcOp != Opcodes.ICONST_4 && srcOp != Opcodes.ICONST_5) {
                    return false;
                }
                hasInit = true;
            }
        }
        return hasInit;
    }

    private static boolean bceLocalWrittenInRange(java.util.List<Instruction> r, int v, int from, int to) {
        for (int i = from + 1; i < to; i++) {
            Instruction in = r.get(i);
            if (in instanceof IInc && ((IInc) in).getVar() == v) return true;
            if (in instanceof VarOp) {
                int op = in.getOpcode();
                if ((op == Opcodes.ISTORE || op == Opcodes.ASTORE) && ((VarOp) in).getIndex() == v) return true;
            }
        }
        return false;
    }

    private static int bceCountJumpsTargeting(java.util.List<Instruction> r, int headerIndex) {
        Instruction li = r.get(headerIndex);
        if (!(li instanceof LabelInstruction)) return -1;
        Label target = ((LabelInstruction) li).getLabel();
        int c = 0;
        for (Instruction in : r) {
            if (in instanceof Jump && ((Jump) in).getLabel() == target) c++;
        }
        return c;
    }

    private static boolean bceForeignEntry(java.util.List<Instruction> r, java.util.HashMap<Label, Integer> pos,
                                           int from, int to, Jump backEdge) {
        for (Instruction in : r) {
            if (!(in instanceof Jump)) continue;
            Jump jj = (Jump) in;
            Integer tgt = pos.get(jj.getLabel());
            if (tgt == null) continue;
            if (tgt > from && tgt <= to && jj != backEdge) return true;
        }
        return false;
    }

    boolean optimize() {
        int instructionCount = instructions.size();
        
        // optimize away a method that only contains the void return instruction e.g. blank constructors etc.
        if(instructionCount < 6) {
            int realCount = instructionCount;
            Instruction actual = null;
            for(int iter = 0 ; iter < instructionCount ; iter++) {
                Instruction current = instructions.get(iter);
                if(current instanceof LabelInstruction) {
                    realCount--;
                    continue;
                }
                if(current instanceof LineNumber) {
                    realCount--;
                    continue;
                }
                actual = current;
            }
            
            if(realCount == 1 && actual != null && actual.getOpcode() == Opcodes.RETURN) {
                return false;
            }
        }

        // Prove-safe bounds-check elimination runs HERE, on the raw (pre-reduction)
        // instruction list, where stores/inits/lengths are still explicit opcodes
        // (the reduction passes below fuse them into opaque CustomIntructions that
        // could hide a write and make the analysis unsound).
        analyzeBoundsChecks();

        boolean astoreCalls = false;
        boolean hasInstructions = false; 
        
        boolean hasTryCatch = false;
        for (int iter=0; iter < instructionCount - 1; iter++) {
            Instruction current = instructions.get(iter);
            if (current instanceof TryCatch) {
                hasTryCatch = true;
            }
            current.setMethod(this);
            if (current.isOptimized()) {
                continue;
            }
            int currentOpcode = current.getOpcode();
            switch(currentOpcode) {
                case Opcodes.CHECKCAST: {
                    // Remove the check cast for now as it gets in the way of other optimizations
                    instructions.remove(iter);
                    iter--;
                    instructionCount--;
                    break;
                }
            }
        }
        
        for(int iter = 0 ; iter < instructionCount - 1 ; iter++) {
            Instruction current = instructions.get(iter);
            if (current.isOptimized()) {
                // This instruction has already been optimized
                // we should skip it and proceed to the next one
                continue;
            }
            Instruction next = instructions.get(iter + 1);

            int currentOpcode = current.getOpcode();
            int nextOpcode = next.getOpcode();
            
            
            if (ArithmeticExpression.isArithmeticOp(current)) {
                int addedIndex = ArithmeticExpression.tryReduce(instructions, iter);
                if (addedIndex >= 0) {
                    iter = addedIndex;
                    instructionCount = instructions.size();
                    continue;
                }
            }
            
            if (current instanceof Field) {
                int newIter = Field.tryReduce(instructions, iter);
                if (newIter >= 0) {
                    iter = newIter;
                    instructionCount = instructions.size();
                    continue;
                }
            }
            
            
            
            switch(currentOpcode) {
                
                case Opcodes.ARRAYLENGTH: {
                    if (!dependentClasses.contains("java_lang_NullPointerException")) {
                        dependentClasses.add("java_lang_NullPointerException");
                    }
                    int newIter = ArrayLengthExpression.tryReduce(instructions, iter);
                    if (newIter >= 0) {
                        instructionCount = instructions.size();
                        iter = newIter;
                        continue;
                    }
                    break;
                }
                   
                case Opcodes.DUP: {
                    int newIter = DupExpression.tryReduce(instructions, iter);
                    if (newIter >= 0) {
                        iter = newIter;
                        instructionCount = instructions.size();
                        continue;
                    }
                    break;
                }
                
                case Opcodes.POP: {
                    if (iter > 0) {
                        Instruction prev = instructions.get(iter-1);
                        if (prev instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)prev;
                            if (inv.methodHasReturnValue() && !inv.isNoReturn()) {
                                inv.setNoReturn(true);
                                instructions.remove(iter);
                                iter--;
                                instructionCount--;
                                continue;
                            }
                        }
                    }
                    break;
                }
                
                case Opcodes.ASTORE:
                case Opcodes.ISTORE:
                case Opcodes.DSTORE:
                case Opcodes.LSTORE:
                case Opcodes.FSTORE: {
                    if (iter > 0 && current instanceof VarOp) {
                        VarOp currentVarOp = (VarOp) current;
                        Instruction prev = instructions.get(iter-1);
                        if (prev instanceof AssignableExpression) {
                            AssignableExpression expr = (AssignableExpression)prev;
                            StringBuilder sb = new StringBuilder();
                            if (currentVarOp.assignFrom(expr, sb)) {
                                instructions.remove(iter-1);
                                instructions.remove(iter-1);
                                instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                iter = iter-1;
                                instructionCount = instructions.size();
                                continue;
                            }
                            
                        } else if (prev instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)prev;
                            StringBuilder sb = new StringBuilder();
                            if (currentVarOp.assignFrom(inv, sb)) {
                                instructions.remove(iter-1);
                                instructions.remove(iter-1);
                                instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                iter = iter-1;
                                instructionCount = instructions.size();
                                continue;
                            }
                        }
                    }
                    
                    break;
                }
                
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN:
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    if (iter > 0 && current instanceof BasicInstruction) {
                        Instruction prev = instructions.get(iter-1);
                        if (prev instanceof AssignableExpression) {
                            AssignableExpression expr = (AssignableExpression)prev;
                            StringBuilder sb= new StringBuilder();
                            if (expr.assignTo(null, sb)) {
                                instructions.remove(iter-1);
                                instructions.remove(iter-1);
                                String exprString = sb.toString().trim();
                                String retVal = exprString;
                                sb.setLength(0);
                                if (!prev.isConstant()) {
                                    sb.append("\n{\n    ");
                                    switch (currentOpcode) {
                                        case Opcodes.IRETURN:
                                            sb.append("JAVA_INT");
                                            break;
                                        case Opcodes.FRETURN:
                                            sb.append("JAVA_FLOAT");
                                            break;
                                        case Opcodes.ARETURN:
                                            sb.append("JAVA_OBJECT");
                                            break;
                                        case Opcodes.LRETURN:
                                            sb.append("JAVA_LONG");
                                            break;
                                        case Opcodes.DRETURN:
                                            sb.append("JAVA_DOUBLE");
                                            break;
                                    }
                                    sb.append(" ___returnValue=").append(exprString).append(";\n");
                                    retVal = "___returnValue";
                                }
                                if(synchronizedMethod) {
                                    if(staticMethod) {
                                        sb.append("    monitorExitBlock(threadStateData, (JAVA_OBJECT)&class__");
                                        sb.append(getClsName());
                                        sb.append(");\n");
                                    } else {
                                        sb.append("    monitorExitBlock(threadStateData, __cn1ThisObject);\n");
                                    }
                                }
                                if(hasTryCatch) {
                                    sb.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); return ").append(retVal).append(";\n");
                                } else {
                                    sb.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); return ").append(retVal).append(";\n");
                                }
                                if (!prev.isConstant()) {
                                    sb.append("}\n");
                                }
                                
                                instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                iter--;
                                instructionCount = instructions.size();
                                continue;
                                
                            }
                        } else if (prev instanceof CustomInvoke) {
                            
                            CustomInvoke expr = (CustomInvoke)prev;
                            String returnType = expr.getReturnValue();
                            if (returnType != null && !"JAVA_OBJECT".equals(returnType)) {
                                // We can't safely return a JAVA_OBJECT directly because it needs to be added 
                                // to the stack for the GC
                                StringBuilder sb= new StringBuilder();
                                if (expr.appendExpression(sb)) {
                                    instructions.remove(iter-1);
                                    instructions.remove(iter-1);
                                    String exprString = sb.toString().trim();
                                    String retVal = exprString;
                                    sb.setLength(0);
                                    if (!expr.isConstant()) {
                                        
                                        sb.append("\n{\n    ");
                                        switch (currentOpcode) {
                                            case Opcodes.IRETURN:
                                                sb.append("JAVA_INT");
                                                break;
                                            case Opcodes.FRETURN:
                                                sb.append("JAVA_FLOAT");
                                                break;
                                            case Opcodes.ARETURN:
                                                sb.append("JAVA_OBJECT");
                                                break;
                                            case Opcodes.LRETURN:
                                                sb.append("JAVA_LONG");
                                                break;
                                            case Opcodes.DRETURN:
                                                sb.append("JAVA_DOUBLE");
                                                break;
                                        }
                                    
                                        sb.append(" ___returnValue=").append(exprString).append(";\n");
                                        retVal = "___returnValue";
                                    }
                                    if(synchronizedMethod) {
                                        if(staticMethod) {
                                            sb.append("    monitorExitBlock(threadStateData, (JAVA_OBJECT)&class__");
                                            sb.append(getClsName());
                                            sb.append(");\n");
                                        } else {
                                            sb.append("    monitorExitBlock(threadStateData, __cn1ThisObject);\n");
                                        }
                                    }
                                    if(hasTryCatch) {
                                        sb.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); return ").append(retVal).append(";\n");
                                    } else {
                                        sb.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); return ").append(retVal).append(";\n");
                                    }
                                    if (!expr.isConstant()) {
                                        sb.append("}\n");
                                    }
                                    

                                    instructions.add(iter-1, new CustomIntruction(sb.toString(), sb.toString(), dependentClasses));
                                    iter--;
                                    instructionCount = instructions.size();
                                    continue;

                                }
                            }
                        }
                    }
                    break;
                }
                
                case Opcodes.BASTORE:
                case Opcodes.SASTORE:
                case Opcodes.CASTORE:
                case Opcodes.AASTORE:
                case Opcodes.IASTORE:
                case Opcodes.DASTORE:
                case Opcodes.LASTORE:
                case Opcodes.FASTORE: {
                    if (iter > 2 && current instanceof BasicInstruction) {
                        StringBuilder devNull = new StringBuilder();
                        String arrayLiteral = null;
                        String indexLiteral = null;
                        String valueLiteral = null;
                        Instruction prev3 = instructions.get(iter-3);
                        if (prev3 instanceof AssignableExpression) {
                            if (((AssignableExpression)prev3).assignTo(null, devNull)) {
                                arrayLiteral = devNull.toString().trim();
                                
                            }
                        }
                        devNull.setLength(0);
                        Instruction prev2 = instructions.get(iter-2);
                        if (prev2 instanceof AssignableExpression) {
                            if (((AssignableExpression)prev2).assignTo(null, devNull)) {
                                indexLiteral = devNull.toString().trim();
                            }
                        }
                        devNull.setLength(0);
                        Instruction prev1 = instructions.get(iter-1);
                        
                        if (prev1 instanceof AssignableExpression) {
                            if (((AssignableExpression)prev1).assignTo(null, devNull)) {
                                valueLiteral = devNull.toString().trim();
                            }
                        } else if (prev1 instanceof CustomInvoke) {
                            devNull.setLength(0);
                            if (((CustomInvoke)prev1).appendExpression(devNull)) {
                                valueLiteral = devNull.toString().trim();
                            }
                        }
                        
                        if (arrayLiteral != null  && indexLiteral != null && valueLiteral != null) {
                            String elementType = null;
                            String valueType = null;
                            switch (current.getOpcode()) {
                                case Opcodes.AASTORE:
                                    elementType = "OBJECT";
                                    valueType = "JAVA_OBJECT";
                                    break;
                                case Opcodes.IASTORE:
                                    elementType = "INT";
                                    valueType = "JAVA_INT";
                                    break;
                                case Opcodes.DASTORE:
                                    elementType = "DOUBLE";
                                    valueType = "JAVA_DOUBLE";
                                    break;
                                    
                                case Opcodes.LASTORE:
                                    elementType = "LONG";
                                    valueType = "JAVA_LONG";
                                    break;
                                case Opcodes.FASTORE:
                                    elementType = "FLOAT";
                                    valueType = "JAVA_FLOAT";
                                    break;
                                case Opcodes.CASTORE:
                                    elementType = "CHAR";
                                    valueType = "JAVA_CHAR";
                                    break;
                                case Opcodes.BASTORE:
                                    elementType = "BYTE";
                                    valueType = "JAVA_BYTE";
                                    break;
                                case Opcodes.SASTORE:
                                    elementType = "SHORT";
                                    valueType = "JAVA_SHORT";
                                    break;
                                    
                            }
                            if (elementType == null || valueType == null) {
                                break;
                            }
                            
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            String code = "    {\n" +
                                    "        JAVA_OBJECT __cn1ArrayTmp = " + arrayLiteral + ";\n" +
                                    "        JAVA_INT __cn1IndexTmp = " + indexLiteral + ";\n" +
                                    "        " + valueType + " __cn1ValueTmp = " + valueLiteral + ";\n" +
                                    "        CN1_SET_ARRAY_ELEMENT_"+elementType+"(__cn1ArrayTmp, __cn1IndexTmp, __cn1ValueTmp);\n" +
                                    "    }\n";
                            instructions.add(iter-3, new CustomIntruction(code, code, dependentClasses));
                            iter = iter-3;
                            instructionCount = instructions.size();
                            continue;
                        }
                    }
                    
                    break;
                }
                    
                
                case Opcodes.FALOAD:
                case Opcodes.BALOAD:
                case Opcodes.IALOAD:
                case Opcodes.LALOAD:
                case Opcodes.DALOAD:
                case Opcodes.AALOAD:
                case Opcodes.SALOAD:
                case Opcodes.CALOAD: {
                    int newIter = ArrayLoadExpression.tryReduce(instructions, iter);
                    if (newIter >= 0) {
                        iter = newIter;
                        instructionCount = instructions.size();
                        continue;
                    }
                    break;
                }
                
                
                /* Try to optimize if statements that just use constants
                   and local variables so that they don't need the intermediate
                   push and pop from the stack.
                */
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ACMPNE:
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPGT:
                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPGE: {
                    
                    if (iter > 1) {
                        Instruction leftArg = instructions.get(iter-2);
                        Instruction rightArg = instructions.get(iter-1);
                        
                        String leftLiteral = null;
                        String rightLiteral = null;
                        
                        if (leftArg instanceof AssignableExpression) {
                            StringBuilder sb = new StringBuilder();
                            if (((AssignableExpression)leftArg).assignTo(null, sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        } else if (leftArg instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)leftArg;
                            StringBuilder sb = new StringBuilder();
                            if (!"JAVA_OBJECT".equals(inv.getReturnValue()) && inv.appendExpression(sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        }
                        if (rightArg instanceof AssignableExpression) {
                            StringBuilder sb = new StringBuilder();
                            if (((AssignableExpression)rightArg).assignTo(null, sb)) {
                                rightLiteral = sb.toString().trim();
                            }
                        } else if (rightArg instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)rightArg;
                            StringBuilder sb = new StringBuilder();
                            if (!"JAVA_OBJECT".equals(inv.getReturnValue()) && inv.appendExpression(sb)) {
                                rightLiteral = sb.toString().trim();
                            }
                        }
                        
                        if (rightLiteral != null && leftLiteral != null) {
                            Jump jmp = (Jump)current;
                            instructions.remove(iter-2);
                            instructions.remove(iter-2);
                            instructions.remove(iter-2);
                            //instructions.remove(iter-2);
                            iter-=2;
                            //instructionCount -= 2;
                            StringBuilder sb = new StringBuilder();
                            String operator = null;
                            String opName = null;
                            switch (currentOpcode) {
                                case Opcodes.IF_ICMPLE:
                                    operator = "<="; opName = "IF_ICMPLE"; break;
                                case Opcodes.IF_ICMPLT:
                                    operator = "<"; opName = "IF_IMPLT"; break;
                                case Opcodes.IF_ICMPNE:
                                    operator = "!="; opName = "IF_ICMPNE"; break;
                                case Opcodes.IF_ICMPGT:
                                    operator = ">"; opName = "IF_ICMPGT"; break;
                                case Opcodes.IF_ICMPGE:
                                    operator = ">="; opName = "IF_ICMPGE"; break;
                                case Opcodes.IF_ICMPEQ:
                                    operator = "=="; opName = "IF_ICMPEQ"; break;
                                case Opcodes.IF_ACMPEQ:
                                    operator = "=="; opName = "IF_ACMPEQ"; break;
                                case Opcodes.IF_ACMPNE:
                                    operator = "!="; opName = "IF_ACMPNE"; break;
                                default :
                                    throw new RuntimeException("Invalid operator during optimization of integer comparison");
                            }
                                    
                            
                            sb.append("if (").append(leftLiteral).append(operator).append(rightLiteral).append(") /* ").append(opName).append(" CustomJump */ ");
                            CustomJump newJump = CustomJump.create(jmp, sb.toString());
                            //jmp.setCustomCompareCode(sb.toString());
                            newJump.setOptimized(true);
                            instructions.add(iter, newJump);
                            instructionCount = instructions.size();
                            
                        }
                        
                    }
                break;
                }   
                case Opcodes.IFNONNULL:
                case Opcodes.IFNULL:
                
                case Opcodes.IFLE:
                case Opcodes.IFLT:
                case Opcodes.IFNE:
                case Opcodes.IFGT:
                case Opcodes.IFEQ:
                case Opcodes.IFGE: {
                    String rightArg = "0";
                    if (currentOpcode == Opcodes.IFNONNULL || currentOpcode == Opcodes.IFNULL) {
                        rightArg = "JAVA_NULL";
                    }
                    if (iter > 0) {
                        Instruction leftArg = instructions.get(iter-1);
                        
                        String leftLiteral = null;
                        
                        
                        if (leftArg instanceof AssignableExpression) {
                            StringBuilder sb = new StringBuilder();
                            if (((AssignableExpression)leftArg).assignTo(null, sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        } else if (leftArg instanceof CustomInvoke) {
                            CustomInvoke inv = (CustomInvoke)leftArg;
                            StringBuilder sb = new StringBuilder();
                            if (inv.appendExpression(sb)) {
                                leftLiteral = sb.toString().trim();
                            }
                        }
                        
                        
                        if (leftLiteral != null) {
                            Jump jmp = (Jump)current;
                            instructions.remove(iter-1);
                            instructions.remove(iter-1);
                            //instructions.remove(iter-2);
                            iter-=1;
                            //instructionCount -= 2;
                            StringBuilder sb = new StringBuilder();
                            String operator = null;
                            String opName = null;
                            switch (currentOpcode) {
                                case Opcodes.IFLE:
                                    operator = "<="; opName = "IFLE"; break;
                                case Opcodes.IFLT:
                                    operator = "<"; opName = "IFLT"; break;
                                case Opcodes.IFNE:
                                    operator = "!="; opName = "IFNE"; break;
                                case Opcodes.IFGT:
                                    operator = ">"; opName = "IFGT"; break;
                                case Opcodes.IFGE:
                                    operator = ">="; opName = "IFGE"; break;
                                case Opcodes.IFEQ:
                                    operator = "=="; opName = "IFEQ"; break;
                                case Opcodes.IFNULL:
                                    operator = "=="; opName = "IFNULL"; break;
                                case Opcodes.IFNONNULL:
                                    operator = "!="; opName = "IFNONNULL"; break;
                                default :
                                    throw new RuntimeException("Invalid operator during optimization of integer comparison");
                            }
                                    
                            
                            sb.append("if (").append(leftLiteral).append(operator).append(rightArg).append(") /* ").append(opName).append(" CustomJump */ ");
                            CustomJump newJump = CustomJump.create(jmp, sb.toString());
                            //jmp.setCustomCompareCode(sb.toString());
                            newJump.setOptimized(true);
                            instructions.add(iter, newJump);
                            instructionCount = instructions.size();
                            
                        }
                        
                    }
                break;
                }   
                   
                
                
                
                
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKEINTERFACE: {
                    if (current instanceof Invoke) {
                        Invoke inv = (Invoke)current;
                        // AOT trivial accessor inlining: a monomorphic call to a
                        // `return this.field` getter / `this.field = arg` setter has the
                        // exact stack effect of a GETFIELD / PUTFIELD, so swap the call
                        // for the field op. Reprocess the slot so Field.tryReduce above
                        // folds the operands into a direct field expression.
                        Field inlinedField = inv.asInlinableFieldAccess();
                        if (inlinedField != null) {
                            instructions.set(iter, inlinedField);
                            iter--;
                            continue;
                        }
                        List<ByteCodeMethodArg> invocationArgs = inv.getArgs();
                        int numArgs = invocationArgs.size();
                        
                        //if (current.getOpcode() != Opcodes.INVOKESTATIC) {
                        //    numArgs++;
                        //}
                        if (iter >= numArgs) {
                            String[] argLiterals = new String[numArgs];
                            StringBuilder devNull = new StringBuilder();
                            for (int i=0; i<numArgs; i++) {
                                devNull.setLength(0);
                                Instruction instr = instructions.get(iter-numArgs+i);
                                if (instr instanceof AssignableExpression && ((AssignableExpression)instr).assignTo(null, devNull)) {
                                    argLiterals[i] = devNull.toString().trim();
                                } else if (instr instanceof CustomInvoke) {
                                    CustomInvoke cinv = (CustomInvoke)instr;
                                    devNull.setLength(0);
                                    if (!"JAVA_OBJECT".equals(cinv.getReturnValue()) && cinv.appendExpression(devNull)) {
                                        // We can't add invocations that return objects directly
                                        // because they need to be added to the stack for GC
                                        argLiterals[i] = devNull.toString().trim();
                                    }
                                } else if (instr instanceof ArithmeticExpression) {
                                    argLiterals[i] = ((ArithmeticExpression)instr).getExpressionAsString().trim();
                                } else if (instr instanceof VarOp) {
                                    VarOp var = (VarOp)instr;
                                    switch (instr.getOpcode()) {
                                        case Opcodes.ALOAD: {
                                            if (!isStatic() && var.getIndex() == 0) {
                                                argLiterals[i] = "__cn1ThisObject";
                                            } else {
                                                argLiterals[i] = "locals["+var.getIndex()+"].data.o";
                                            }
                                            break;
                                        }
                                        case Opcodes.ILOAD: {
                                            argLiterals[i] = "ilocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.ACONST_NULL: {
                                            argLiterals[i] = "JAVA_NULL";
                                            break;
                                        }
                                        case Opcodes.DLOAD: {
                                            argLiterals[i] = "dlocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.FLOAD: {
                                            argLiterals[i] = "flocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.LLOAD: {
                                            argLiterals[i] = "llocals_"+var.getIndex()+"_";
                                            break;
                                        }
                                        case Opcodes.ICONST_0: {
                                            argLiterals[i] = "0";
                                            break;
                                        }
                                        case Opcodes.ICONST_1: {
                                            argLiterals[i] = "1";
                                            break;
                                        }
                                        case Opcodes.ICONST_2: {
                                            argLiterals[i] = "2";
                                            break;
                                        }
                                        case Opcodes.ICONST_3: {
                                            argLiterals[i] = "3";
                                            break;
                                        }
                                        case Opcodes.ICONST_4: {
                                            argLiterals[i] = "4";
                                            break;
                                        }
                                        case Opcodes.ICONST_5: {
                                            argLiterals[i] = "5";
                                            break;
                                        }
                                        case Opcodes.ICONST_M1: {
                                            argLiterals[i] = "-1";
                                            break;
                                        }
                                        case Opcodes.LCONST_0: {
                                            argLiterals[i] = "(JAVA_LONG)0";
                                            break;
                                        }
                                        case Opcodes.LCONST_1: {
                                            argLiterals[i] = "(JAVA_LONG)1";
                                            break;
                                        }
                                        case Opcodes.BIPUSH: 
                                        case Opcodes.SIPUSH: {
                                            argLiterals[i] = String.valueOf(var.getIndex());
                                            
                                            break;
                                        }
                                    }
                                } else {
                                    switch (instr.getOpcode()) {
                                        
                                        case Opcodes.ACONST_NULL: {
                                            argLiterals[i] = "JAVA_NULL";
                                            break;
                                        }
                                        
                                        case Opcodes.ICONST_0: {
                                            argLiterals[i] = "0";
                                            break;
                                        }
                                        case Opcodes.ICONST_1: {
                                            argLiterals[i] = "1";
                                            break;
                                        }
                                        case Opcodes.ICONST_2: {
                                            argLiterals[i] = "2";
                                            break;
                                        }
                                        case Opcodes.ICONST_3: {
                                            argLiterals[i] = "3";
                                            break;
                                        }
                                        case Opcodes.ICONST_4: {
                                            argLiterals[i] = "4";
                                            break;
                                        }
                                        case Opcodes.ICONST_5: {
                                            argLiterals[i] = "5";
                                            break;
                                        }
                                        case Opcodes.ICONST_M1: {
                                            argLiterals[i] = "-1";
                                            break;
                                        }
                                        case Opcodes.LCONST_0: {
                                            argLiterals[i] = "(JAVA_LONG)0";
                                            break;
                                        }
                                        case Opcodes.LCONST_1: {
                                            argLiterals[i] = "(JAVA_LONG)1";
                                            break;
                                        }
                                        case Opcodes.BIPUSH: {
                                            if (instr instanceof BasicInstruction) {
                                                argLiterals[i] = String.valueOf(((BasicInstruction)instr).getValue());
                                            }
                                            break;
                                        }
                                        case Opcodes.LDC : {
                                            if (instr instanceof Ldc) {
                                                Ldc ldc = (Ldc)instr;
                                                argLiterals[i] = ldc.getValueAsString();
                                                
                                            }
                                            break;
                                        }


                                    }
                                    
                                }
                            }
                            
                            
                            // Check to make sure that we have all the args as literals.
                            boolean missingLiteral = false;
                            for (String lit : argLiterals) {
                                if (lit == null) {
                                    missingLiteral = true;
                                    break;
                                }
                            }
                            
                            // We have all of the arguments as literals.  Let's
                            // add them to our invoke instruction.
                            if (!missingLiteral) {
                                CustomInvoke newInvoke = CustomInvoke.create(inv);
                                instructions.remove(iter);
                                instructions.add(iter, newInvoke);
                                int newIter = iter;
                                for (int i=0; i< numArgs; i++) {
                                    instructions.remove(iter-numArgs);
                                    newIter--;
                                    newInvoke.setLiteralArg(i, argLiterals[i]);
                                }
                                if (inv.getOpcode() != Opcodes.INVOKESTATIC) {
                                    Instruction ldTarget = instructions.get(iter-numArgs-1);
                                    if (ldTarget instanceof AssignableExpression) {
                                        StringBuilder targetExprStr = new StringBuilder();
                                        if (((AssignableExpression)ldTarget).assignTo(null, targetExprStr)) {
                                            newInvoke.setTargetObjectLiteral(targetExprStr.toString().trim());
                                            instructions.remove(iter-numArgs-1);
                                            newIter--;
                                            
                                        }
                                        
                                    } else if (ldTarget instanceof CustomInvoke) {
                                        // WE Can't pass a custom invoke as the target directly
                                        // because it the return value needs to be added to the 
                                        // stack for the GC
                                    } else {
                                        switch (ldTarget.getOpcode()) {
                                            case Opcodes.ALOAD: {
                                                VarOp v = (VarOp)ldTarget;
                                                if (isStatic() && v.getIndex() == 0) {
                                                    newInvoke.setTargetObjectLiteral("__cn1ThisObject");
                                                } else {
                                                    newInvoke.setTargetObjectLiteral("locals["+v.getIndex()+"].data.o");
                                                }
                                                instructions.remove(iter-numArgs-1);
                                                newIter--;
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                newInvoke.setOptimized(true);
                                //iter = 0;
                                instructionCount = instructions.size();
                                iter = newIter;
                                
                                
                            }
                        }
                    }
                    break;
                }
                    
                
            }
            astoreCalls = astoreCalls || currentOpcode == Opcodes.ASTORE || currentOpcode == Opcodes.ISTORE || 
                    currentOpcode == Opcodes.LSTORE || currentOpcode == Opcodes.DSTORE || currentOpcode == Opcodes.FSTORE;
            
            hasInstructions = hasInstructions | current.getOpcode() != -1;
        }
        return hasInstructions;
    }

    private boolean constReturn(int type, int value, int nextOpcode, int iter) {
        if(nextOpcode == type) {
            instructions.remove(iter);
            instructions.remove(iter);
            if(synchronizedMethod && instructions.size() > 0) {
                if(staticMethod) {
                    instructions.add(iter, new CustomIntruction("    monitorExit(threadStateData, (JAVA_OBJECT)&class__" + clsName + ");\n" +
                            "    return " + value + ";\n",
                            "    monitorExit(threadStateData, (JAVA_OBJECT)&class__" + clsName + ");\n" +
                            "    RETURN_AND_RELEASE_FROM_METHOD(" + value + ", " + maxLocals + ");\n", dependentClasses));
                } else {
                    instructions.add(iter, new CustomIntruction("    monitorExit(threadStateData, __cn1ThisObject);\n" +
                            "    return " + value + ";\n",
                            "    monitorExit(threadStateData, __cn1ThisObject);\n" +
                            "    RETURN_AND_RELEASE_FROM_METHOD(" + value + ", " + maxLocals + ");\n", dependentClasses));
                }
            } else {
                instructions.add(iter, new CustomIntruction("    return " + value + ";\n",
                        "    RETURN_AND_RELEASE_FROM_METHOD(" + value + ", " + maxLocals + ");\n", dependentClasses));
            }
            return true;
        }
        return false;
    }
    
    private int localsOffsetToArgOffset(int offset) {
        int localsOffset = 0;
        if(!staticMethod) {
            localsOffset++;
        }
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            ByteCodeMethodArg arg = arguments.get(iter);
            if(localsOffset == offset) {
                return iter + 1;
            }
            localsOffset++;
            if(arg.isDoubleOrLong()) {
                localsOffset++;
            }
        }
        return -1;
    }

    // support for the SignatureSet interface
	public boolean containsSignature(SignatureSet sig) {
		return desc.equals(sig.getSignature());
	}
	public String getSignature() {
		return desc;
	}

    @Override
    public SignatureSet nextSignature() {
        return null;
    }

    public String getJsBodyScript() {
        return jsBodyScript;
    }

    public void setJsBodyScript(String jsBodyScript) {
        this.jsBodyScript = jsBodyScript;
    }

    public String[] getJsBodyParams() {
        return jsBodyParams;
    }

    public void setJsBodyParams(String[] jsBodyParams) {
        this.jsBodyParams = jsBodyParams;
    }

    public boolean isJsBodyMethod() {
        return jsBodyScript != null;
    }


}
