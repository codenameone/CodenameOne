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
public class BytecodeMethod {    

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
    private List<ByteCodeMethodArg> arguments = new ArrayList<ByteCodeMethodArg>();
    private Set<LocalVariable> localVariables = new HashSet<LocalVariable>();
    private ByteCodeMethodArg returnType;
    private String methodName;
    private String clsName;
    private boolean constructor;
    private boolean staticMethod;
    private boolean privateMethod;
    private boolean nativeMethod;
    private List<String> dependentClasses = new ArrayList<String>();
    //private List<String> exportedClasses = new ArrayList<String>();
    private List<Instruction> instructions = new ArrayList<Instruction>();
    private String declaration = ""; 
    private String sourceFile;
    private int maxStack;
    private int maxLocals;
    private static boolean acceptStaticOnEquals;
    private int methodOffset;
    private boolean forceVirtual;
    private boolean virtualOverriden;
    private boolean finalMethod;
    private boolean synchronizedMethod;
    private final static Set<String> virtualMethodsInvoked = new TreeSet<String>();    
    private String desc;
    private boolean eliminated;
    private boolean usedByNative;
    
    static boolean optimizerOn;
    
    static {
        String op = System.getProperty("optimizer");
        optimizerOn = op == null || op.equalsIgnoreCase("on");
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
    }

    private Set<String> usedMethods;
    public boolean isMethodUsed(BytecodeMethod bm) {
        if(usedMethods == null) {
            usedMethods = new TreeSet<String>();
            for(Instruction ins : instructions) {
                String s = ins.getMethodUsed();
                if(s != null && !usedMethods.contains(s)) {
                    usedMethods.add(s);
                }
            }
        }
        if(bm.methodName.equals("__INIT__")) {
            return usedMethods.contains(bm.desc + ".<init>");
        }
        return usedMethods.contains(bm.desc + "." + bm.methodName);
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
        b.append(methodName);
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
    
    private boolean hasLocalVariableWithIndex(char qualifier, int index) {
        for (LocalVariable lv : localVariables) {
            if (lv.getIndex() == index && lv.getQualifier() == qualifier) {
                return true;
            }
        }
        return false;
    }
    
    public void appendMethodC(StringBuilder b) {
        if(nativeMethod) {
            return;
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
        
        boolean hasInstructions = true;
        if(optimizerOn) {
            hasInstructions = optimize();
        }
        
        if(hasInstructions) {
            Set<String> added = new HashSet<String>();
            for (LocalVariable lv : localVariables) {
                String variableName = lv.getQualifier() + "locals_"+lv.getIndex()+"_";
                if (!added.contains(variableName) && lv.getQualifier() != 'o') {
                    added.add(variableName);
                    b.append("    volatile ");
                    switch (lv.getQualifier()) {
                        case 'i' :
                            b.append("JAVA_INT"); break;
                        case 'l' :
                            b.append("JAVA_LONG"); break;
                        case 'f' :
                            b.append("JAVA_FLOAT"); break;
                        case 'd' :
                            b.append("JAVA_DOUBLE"); break;
                    }
                    b.append(" ").append(lv.getQualifier()).append("locals_").append(lv.getIndex()).append("_ = 0; /* ").append(lv.getOrigName()).append(" */\n");
                }
            }
            
            if(staticMethod) {
                if(methodName.equals("__CLINIT__")) {
                    b.append("    DEFINE_METHOD_STACK(");
                } else {
                    b.append("    __STATIC_INITIALIZER_");
                    b.append(clsName.replace('/', '_').replace('$', '_'));
                    b.append("(threadStateData);\n    DEFINE_METHOD_STACK(");
                }
            } else {
                b.append("    DEFINE_INSTANCE_METHOD_STACK(");
            }
            b.append(maxStack);
            b.append(", ");
            b.append(maxLocals);
            b.append(", 0, ");
            b.append(Parser.addToConstantPool(clsName));
            b.append(", ");
            b.append(Parser.addToConstantPool(methodName));
            b.append(");\n");
            int startOffset = 0;
            if(synchronizedMethod) {
                if(staticMethod) {
                    b.append("    monitorEnter(threadStateData, (JAVA_OBJECT)&class__");
                    b.append(clsName);
                    b.append(");\n");
                } else {
                    b.append("    monitorEnter(threadStateData, __cn1ThisObject);\n");
                }
            }
            if(!staticMethod) {
                b.append("    locals[0].data.o = __cn1ThisObject; locals[0].type = CN1_TYPE_OBJECT; ");
                startOffset++;
            }
            int localsOffset = startOffset;
            for(int iter = 0 ; iter < arguments.size() ; iter++) {
                ByteCodeMethodArg arg = arguments.get(iter);
                if (arg.getQualifier() == 'o') {
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
        }
        
        BasicInstruction.setSynchronizedMethod(synchronizedMethod, staticMethod, clsName);
        TryCatch.reset();
        BasicInstruction.setHasInstructions(hasInstructions);
        for(Instruction i : instructions) {
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
        appendCMethodPrefix(b, "", clsName);
        b.append(" {\n");
        if(!returnType.isVoid()) {
            b.append("return virtual_");
        } else {
            b.append("virtual_");            
        }
        b.append(clsName);
        b.append("_");
        b.append(methodName);
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
        b.append(methodName);
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
    
    public void appendVirtualMethodC(String cls, StringBuilder b, String offset, boolean includeStaticInitializer) {
        if(virtualOverriden) {
            return;
        }
        StringBuilder bld = new StringBuilder();
        bld.append(cls);
        bld.append("_");
        bld.append(methodName);
        bld.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(bld);
        }
        if(!returnType.isVoid()) {
            bld.append("_R");
            returnType.appendCMethodExt(bld);
        }
        
        if(!forceVirtual && !virtualMethodsInvoked.contains(bld.toString())) {
            return;
        }
                
        // generate the function pointer declaration
        appendCMethodPrefix("\ntypedef ", ")", b, "(*functionPtr_", cls);
        b.append(";\n");
        
        appendCMethodPrefix(b, "virtual_", cls);
        b.append(" {\n    ");
        
        if(includeStaticInitializer) {
            b.append("__STATIC_INITIALIZER_");
            b.append(cls);
            b.append("(threadStateData);\n    ");
        }
        b.append("if(__cn1ThisObject == JAVA_NULL) THROW_NULL_POINTER_EXCEPTION();\n    ");
        if(!returnType.isVoid()) {
            b.append("return (*(functionPtr_");
        } else {
            b.append("(*(functionPtr_");            
        }
        b.append(bld);
        b.append(")__cn1ThisObject->__codenameOneParentClsReference->vtable[");
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
        bld.append(methodName);
        bld.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(bld);
        }
        if(!returnType.isVoid()) {
            bld.append("_R");
            returnType.appendCMethodExt(bld);
        }
        if(!forceVirtual && !virtualMethodsInvoked.contains(bld.toString())) {
            return;
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
        b.append(methodName);
        b.append("__");
        for(ByteCodeMethodArg args : arguments) {
            args.appendCMethodExt(b);
        }        
        if(!returnType.isVoid()) {
            b.append("_R");
            returnType.appendCMethodExt(b);
        }
    }

    public void appendMethodCSharp(StringBuilder b) {
        // todo
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }
    
    public boolean equals(Object o) {
        BytecodeMethod bm = (BytecodeMethod)o;
        int val = bm.methodName.compareTo(methodName);
        if(val != 0) {
            return false;
        }
        if(acceptStaticOnEquals) {
            if(bm.arguments.size() != arguments.size()) {
                return false;
            }            
        } else {
            if(staticMethod || bm.staticMethod || bm.arguments.size() != arguments.size()) {
                return false;
            }
        }
        for(int iter = 0 ; iter < arguments.size() ; iter++) {
            ByteCodeMethodArg arg1 = arguments.get(iter);
            ByteCodeMethodArg arg2 = bm.arguments.get(iter);
            if(!arg1.equals(arg2)) {
                return false;
            }
        }
        return returnType.equals(bm.returnType);
    }
    
    public int hashCode() {
        return methodName.hashCode();
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
        //addInstruction(0, new LocalVariable(name, desc, signature, start, end, index));
        localVariables.add(new LocalVariable(name, desc, signature, start, end, index));
    }
    
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    } 
    
    public void addDebugInfo(int line) {
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
        i.addDependencies(dependentClasses);
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

    public void addField(int opcode, String owner, String name, String desc) {
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

    /**
     * @return the usedByNative
     */
    public boolean isUsedByNative() {
        return usedByNative;
    }

    /**
     * @param usedByNative the usedByNative to set
     */
    public void setUsedByNative(boolean usedByNative) {
        this.usedByNative = usedByNative;
    }

    private int varCounter = 0;
    
    
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
                            if (inv.methodHasReturnValue()) {
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
                                        sb.append("    monitorExit(threadStateData, (JAVA_OBJECT)&class__");
                                        sb.append(getClsName());
                                        sb.append(");\n");
                                    } else {
                                        sb.append("    monitorExit(threadStateData, __cn1ThisObject);\n");
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
                                            sb.append("    monitorExit(threadStateData, (JAVA_OBJECT)&class__");
                                            sb.append(getClsName());
                                            sb.append(");\n");
                                        } else {
                                            sb.append("    monitorExit(threadStateData, __cn1ThisObject);\n");
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
                            switch (current.getOpcode()) {
                                case Opcodes.AASTORE:
                                    elementType = "OBJECT";break;
                                case Opcodes.IASTORE:
                                    elementType = "INT"; break;
                                case Opcodes.DASTORE:
                                    elementType = "DOUBLE"; break;
                                    
                                case Opcodes.LASTORE:
                                    elementType = "LONG"; break;
                                case Opcodes.FASTORE:
                                    elementType = "FLOAT"; break;
                                case Opcodes.CASTORE:
                                    elementType = "CHAR";break;
                                case Opcodes.BASTORE:
                                    elementType = "BYTE"; break;
                                case Opcodes.SASTORE:
                                    elementType = "SHORT"; break;
                                    
                            }
                            if (elementType == null) {
                                break;
                            }
                            
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            instructions.remove(iter-3);
                            String code = "    CN1_SET_ARRAY_ELEMENT_"+elementType+"(" + arrayLiteral + ", "+indexLiteral+", "+valueLiteral+");\n";
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
}
