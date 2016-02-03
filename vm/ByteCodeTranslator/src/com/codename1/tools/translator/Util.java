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

import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.TryCatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class Util {

    private static final Map<Class, String> ctypeMap = new HashMap<Class, String>();
    private static final Map<Class, String> sigTypeMap = new HashMap<Class, String>();

    static {
        ctypeMap.put(Integer.TYPE, "JAVA_INT");
        ctypeMap.put(Long.TYPE, "JAVA_LONG");
        ctypeMap.put(Short.TYPE, "JAVA_SHORT");
        ctypeMap.put(Byte.TYPE, "JAVA_BYTE");
        ctypeMap.put(Double.TYPE, "JAVA_DOUBLE");
        ctypeMap.put(Float.TYPE, "JAVA_FLOAT");
        ctypeMap.put(Boolean.TYPE, "JAVA_BOOLEAN");
        ctypeMap.put(Character.TYPE, "JAVA_CHAR");
        ctypeMap.put(Void.TYPE, "JAVA_VOID");
        sigTypeMap.put(Integer.TYPE, "int");
        sigTypeMap.put(Long.TYPE, "long");
        sigTypeMap.put(Short.TYPE, "short");
        sigTypeMap.put(Byte.TYPE, "byte");
        sigTypeMap.put(Double.TYPE, "double");
        sigTypeMap.put(Float.TYPE, "float");
        sigTypeMap.put(Boolean.TYPE, "boolean");
        sigTypeMap.put(Character.TYPE, "char");
        sigTypeMap.put(Void.TYPE, "void");
    }

    public static String getCType(Class cls) {
        return ctypeMap.get(cls);
    }

    public static String getSigType(Class cls) {
        return sigTypeMap.get(cls);
    }

    public static List<ByteCodeMethodArg> getMethodArgs(String methodDesc) {
        List<ByteCodeMethodArg> arguments = new ArrayList<ByteCodeMethodArg>();
        int currentArrayDim = 0;

        String desc = methodDesc;
        int pos = desc.lastIndexOf(')');
        desc = desc.substring(1, pos);
        for (int i = 0; i < desc.length(); i++) {
            char currentType = desc.charAt(i);
            switch (currentType) {
                case '[':
                    // array of...
                    currentArrayDim++;
                    continue;
                case 'L':
                    // Object skip until ;
                    int idx = desc.indexOf(';', i);
                    String objectType = desc.substring(i + 1, idx);
                    objectType = objectType.replace('/', '_').replace('$', '_');
                    //if(!dependentClasses.contains(objectType)) {
                    //    dependentClasses.add(objectType);
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
        return arguments;
    }

    public static char[] getStackInputTypes(Instruction instr) {
        char[] out = instr.getStackInputTypes();
        if (out != null) {
            return out;
        }

        switch (instr.getOpcode()) {
            
                case Opcodes.NOP:
                case Opcodes.ACONST_NULL:
                case Opcodes.ICONST_M1:
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5:
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                case Opcodes.SIPUSH:
                case Opcodes.BIPUSH:    
                    return new char[0];
                case Opcodes.BALOAD:
                case Opcodes.CALOAD:
                case Opcodes.IALOAD:
                case Opcodes.SALOAD:
                case Opcodes.LALOAD:
                case Opcodes.FALOAD:
                case Opcodes.DALOAD:
                case Opcodes.AALOAD:
                    return new char[]{'i', 'o'};
                
                case Opcodes.BASTORE:
                case Opcodes.CASTORE:
                case Opcodes.SASTORE:
                case Opcodes.IASTORE:
                    return new char[]{'i','i','o'};
                case Opcodes.LASTORE:
                    return new char[]{'l', 'i', 'o'};
                case Opcodes.FASTORE:
                    return new char[]{'f','i','o'};
                case Opcodes.DASTORE:
                    return new char[]{'d', 'i', 'o'};
                case Opcodes.AASTORE:
                    return new char[]{'o','i','o'};

                case Opcodes.POP:
                    return new char[]{'*'};

                case Opcodes.POP2:
                    return new char[]{'*','*'};

                case Opcodes.DUP:
                    return new char[]{'0'};

                case Opcodes.DUP2:
                case Opcodes.DUP_X2:
                case Opcodes.DUP2_X2:
                    return null; // DUP2 depends on the types on the stack so we don't statically know the input types

                case Opcodes.DUP_X1:
                case Opcodes.DUP2_X1:
                    return new char[]{'0','1'};
                case Opcodes.SWAP:
                    return new char[]{'0','1'};
                case Opcodes.IADD:
                case Opcodes.ISUB:
                case Opcodes.IMUL:
                case Opcodes.IDIV:
                case Opcodes.IREM:
                case Opcodes.ISHL:
                case Opcodes.ISHR:
                case Opcodes.IUSHR:
                case Opcodes.IAND:
                case Opcodes.IOR:
                case Opcodes.IXOR:
                    return new char[]{'i','i'};
               
                case Opcodes.LADD:
                case Opcodes.LSUB:
                case Opcodes.LMUL:
                case Opcodes.LDIV:
                case Opcodes.LREM:
                case Opcodes.LSHL:
                case Opcodes.LSHR:
                case Opcodes.LAND:
                case Opcodes.LOR:
                case Opcodes.LXOR:
                case Opcodes.LCMP:
                    return new char[]{'l','l'};
                case Opcodes.FADD:
                case Opcodes.FSUB:
                case Opcodes.FMUL:
                case Opcodes.FDIV:
                case Opcodes.FREM:
                case Opcodes.FCMPG:
                case Opcodes.FCMPL:
                    return new char[]{'f','f'};
                case Opcodes.DADD:
                case Opcodes.DSUB:
                case Opcodes.DMUL:
                case Opcodes.DDIV:
                case Opcodes.DREM:
                case Opcodes.DCMPL:
                case Opcodes.DCMPG:
                    return new char[]{'d','d'};
                    
                case Opcodes.INEG:
                case Opcodes.I2L:
                case Opcodes.I2F:
                case Opcodes.I2D:
                case Opcodes.I2B:
                case Opcodes.I2C:
                case Opcodes.I2S:
                case Opcodes.NEWARRAY:
                    return new char[]{'i'};
                case Opcodes.LNEG:
                case Opcodes.L2I:
                case Opcodes.L2F:
                case Opcodes.L2D:
                    return new char[]{'l'};
                    
                case Opcodes.FNEG:
                case Opcodes.F2I:
                case Opcodes.F2L:
                case Opcodes.F2D:
                    return new char[]{'f'};
                case Opcodes.DNEG:
                case Opcodes.D2I:
                case Opcodes.D2L:
                case Opcodes.D2F:
                    return new char[]{'d'};
                case Opcodes.LUSHR: 
                     return new char[]{'i','l'};
                    
                case Opcodes.ARRAYLENGTH:
                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                case Opcodes.ATHROW:
                    return new char[]{'o'};
                default: 
                    return null;
                
                    
        }
             
        

    }

    public static char[] getStackOutputTypes(Instruction instr) {
        char[] out = instr.getStackOutputTypes();
        if (out != null) {
            return out;
        }
        
        switch(instr.getOpcode()) {
            case Opcodes.NOP:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.AASTORE:
            case Opcodes.POP:
            case Opcodes.POP2:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
            case Opcodes.ATHROW:
                return new char[0];
                
            case Opcodes.ACONST_NULL:
            case Opcodes.AALOAD:
            case Opcodes.NEWARRAY:
                return new char[]{'o'};
                
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.IALOAD:
            case Opcodes.SALOAD:
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.INEG:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.F2I:
            case Opcodes.D2I:
            case Opcodes.L2I:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.LCMP:
            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
            case Opcodes.ARRAYLENGTH:
            case Opcodes.SIPUSH:
            case Opcodes.BIPUSH:
                return new char[]{'i'};
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.LALOAD:
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LNEG:
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                return new char[]{'l'};
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.FALOAD:
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
            case Opcodes.FNEG:
            case Opcodes.I2F:
            case Opcodes.D2F:
            case Opcodes.L2F:
                return new char[]{'f'};
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
            case Opcodes.DALOAD:
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:
            case Opcodes.DNEG:
            case Opcodes.I2D:
            case Opcodes.F2D:
            case Opcodes.L2D:
                return new char[]{'d'};
            case Opcodes.DUP:
                return new char[]{'0','0'};
                
            case Opcodes.DUP2:
            case Opcodes.DUP_X2:
            case Opcodes.DUP2_X2:
                return null;
                
            case Opcodes.DUP_X1:
            case Opcodes.DUP2_X1:
                return new char[]{'0','1','0'};
            case Opcodes.SWAP:
                    return new char[]{'1','0'};
                
            default:
                return null;
        }
         
    }
}
