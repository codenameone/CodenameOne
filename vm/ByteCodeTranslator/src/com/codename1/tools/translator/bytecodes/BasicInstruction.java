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

import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class BasicInstruction extends Instruction implements AssignableExpression  {
    private final int value;
    private int maxStack;
    private int maxLocals;
    private static boolean synchronizedMethod;
    private static boolean staticMethod;
    private static String className;
    
    public BasicInstruction(int opcode, int value) {
        super(opcode);
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static void setSynchronizedMethod(boolean b, boolean stat, String cls) {
        synchronizedMethod = b;
        staticMethod = stat;
        className = cls;
    }
    
    public void setMaxes(int maxStack, int maxLocals) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
    }

    private void appendSynchronized(StringBuilder b) {
        if(synchronizedMethod) {
            if(staticMethod) {
                b.append("    monitorExit(threadStateData, (JAVA_OBJECT)&class__");
                b.append(className);
                b.append(");\n");
            } else {
                b.append("    monitorExit(threadStateData, __cn1ThisObject);\n");
            }
        }
    }
    
    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> instructions) {
        switch(opcode) {
            case Opcodes.NOP:
                break;
                
            case Opcodes.ACONST_NULL:
                b.append("    PUSH_POINTER(JAVA_NULL); /* ACONST_NULL */\n");
                break;
                
            case Opcodes.ICONST_M1:
                b.append("    PUSH_INT(-1); /* ICONST_M1 */\n");
                break;
                
            case Opcodes.ICONST_0:
                b.append("    PUSH_INT(0); /* ICONST_0 */\n");
                break;

            case Opcodes.ICONST_1:
                b.append("    PUSH_INT(1); /* ICONST_1 */\n");
                break;

            case Opcodes.ICONST_2:
                b.append("    PUSH_INT(2); /* ICONST_2 */\n");
                break;

            case Opcodes.ICONST_3:
                b.append("    PUSH_INT(3); /* ICONST_3 */\n");
                break;

            case Opcodes.ICONST_4:
                b.append("    PUSH_INT(4); /* ICONST_4 */\n");
                break;

            case Opcodes.ICONST_5:
                b.append("    PUSH_INT(5); /* ICONST_5 */\n");
                break;

            case Opcodes.LCONST_0:
                b.append("    PUSH_LONG(0); /* LCONST_0 */\n");
                break;

            case Opcodes.LCONST_1:
                b.append("    PUSH_LONG(1); /* LCONST_1 */\n");
                break;

            case Opcodes.FCONST_0:
                b.append("    PUSH_FLOAT(0); /* FCONST_0 */\n");
                break;

            case Opcodes.FCONST_1:
                b.append("    PUSH_FLOAT(1); /* FCONST_1 */\n");
                break;

            case Opcodes.FCONST_2:
                b.append("    PUSH_FLOAT(2); /* FCONST_2 */\n");
                break;

            case Opcodes.DCONST_0:
                b.append("    PUSH_DOUBLE(0); /* DCONST_0 */\n");
                break;

            case Opcodes.DCONST_1:
                b.append("    PUSH_DOUBLE(1); /* DCONST_1 */\n");
                break;

            case Opcodes.BALOAD:
                b.append("    { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); /* BALOAD */ \n" +
                    "    SP--; SP[-1].type = CN1_TYPE_INT; \n" +
                    "    SP[-1].data.i = ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; \n" +
                    "    }\n");
                break;

            case Opcodes.CALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, SP[-1].data.i); /* CALOAD */\n" +
                    "    SP--; SP[-1].type = CN1_TYPE_INT; \n" +
                    "    SP[-1].data.i = ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i];\n");
                break;
                
            case Opcodes.IALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, SP[-1].data.i); /* IALOAD */\n" +
                        "    SP--; SP[-1].type = CN1_TYPE_INT; \n" +
                        "    SP[-1].data.i = ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i];\n");
                break;

            case Opcodes.SALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \n" +
                        "    SP--; SP[-1].type = CN1_TYPE_INT; \n" +
                        "    SP[-1].data.i = ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; /* SALOAD */\n");
                break;

            case Opcodes.LALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, SP[-1].data.i); /* LALOAD */\n" +
                        "    SP--; SP[-1].type = CN1_TYPE_LONG; \n" +
                        "    SP[-1].data.l = LONG_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i);\n");
                break;

            case Opcodes.FALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, SP[-1].data.i); /* FALOAD */\n" +
                        "    SP--; SP[-1].type = CN1_TYPE_FLOAT; \n" +
                        "    SP[-1].data.f = FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i);\n");
                break;

            case Opcodes.DALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, SP[-1].data.i); /* DALOAD */\n" +
                        "    SP--; SP[-1].type = CN1_TYPE_DOUBLE; \n" +
                        "    SP[-1].data.d = DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i);\n");
                break;

            case Opcodes.AALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, SP[-1].data.i); /* AALOAD */\n" +
                        "    SP--; SP[-1].type = CN1_TYPE_INVALID; \n" +
                        "    SP[-1].data.o = ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)SP[-1].data.o).data)[(*SP).data.i]; \n" +
                        "    SP[-1].type = CN1_TYPE_OBJECT; \n");
                break;

            case Opcodes.BASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); /* BASTORE */\n" +
                        "    ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP -= 3;\n");
                break;

            case Opcodes.CASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); /* CASTORE */\n" +
                        "    ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP -= 3;\n\n");
                break;

            case Opcodes.SASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); /* SASTORE */\n" +
                        "    ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP -= 3;\n");
                break;

            case Opcodes.IASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); /* IASTORE */\n" +
                        "    ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)SP[-3].data.o).data)[SP[-2].data.i] = SP[-1].data.i; SP -= 3;\n");
                break;

            case Opcodes.LASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); /* LASTORE */\n" +
                        "    LONG_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.l; SP -= 3;\n");
                break;

            case Opcodes.FASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); /* FASTORE */\n" +
                        "    FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.f; SP -= 3;\n");
                break;

            case Opcodes.DASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); /* DASTORE */\n" +
                        "    DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.d; SP -= 3;\n");
                break;

            case Opcodes.AASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, SP[-2].data.i); { /* BC_AASTORE */\n" +
                        "    JAVA_OBJECT aastoreTmp = SP[-3].data.o; \n" +
                        "    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)aastoreTmp).data)[SP[-2].data.i] = SP[-1].data.o; \n" +
                        "    SP -= 3; }\n");
                break;

            case Opcodes.POP:
                b.append("    SP--; /* POP */\n");
                break;

            case Opcodes.POP2:
                b.append("    popMany(threadStateData, 2, &SP); /* POP2 */\n");
                break;

            /*case Opcodes.DUP:
                b.append("    PUSH_INT(PEEK_INT(1));\n");
                break;

            case Opcodes.DUP_X1:
                b.append("    DUP_X1();\n");
                break;
                
            case Opcodes.DUP_X2:
                b.append("    DUP_X2();\n");
                break;*/
                
            case Opcodes.DUP:
                b.append("    BC_DUP(); /* DUP */\n");
                break;

            case Opcodes.DUP2:
                b.append("    BC_DUP2(); /* DUP2 */\n");
                break;
                
            case Opcodes.DUP_X1:
                b.append("    BC_DUP2_X1(); /* DUP_X1 */\n");
                break;
                
            case Opcodes.DUP2_X1:
                b.append("    BC_DUP2_X1(); /* DUP2_X1 */\n");
                break;
                
            case Opcodes.DUP_X2:
                b.append("    BC_DUP2_X2(); /* DUP_X2 */\n");
                break;
                
            case Opcodes.DUP2_X2:
                b.append("    BC_DUP2_X2(); /* DUP2_X2 */\n");
                break;
                
            case Opcodes.SWAP:
                b.append("    swapStack(SP); /* SWAP */\n");
                break;
                
            case Opcodes.IADD:
                b.append("    SP--; SP[-1].data.i = SP[-1].data.i + (*SP).data.i; /* IADD */\n");
                break;
                
            case Opcodes.LADD:
                b.append("    SP--; SP[-1].data.l = SP[-1].data.l + (*SP).data.l; /* LADD */\n");
                break;
                
            case Opcodes.FADD:
                b.append("    SP--; SP[-1].data.f = SP[-1].data.f + (*SP).data.f; /* FADD */\n");
                break;
                
            case Opcodes.DADD:
                b.append("    SP--; SP[-1].data.d = SP[-1].data.d + (*SP).data.d; /* DADD */\n");
                break;
                
            case Opcodes.ISUB:
                b.append("    SP--; SP[-1].data.i = (SP[-1].data.i - (*SP).data.i); /* ISUB */\n");
                break;
                
            case Opcodes.LSUB:
                b.append("    SP--; SP[-1].data.l = (SP[-1].data.l - (*SP).data.l); /* LSUB */\n");
                break;
                
            case Opcodes.FSUB:
                b.append("    SP--; SP[-1].data.f = (SP[-1].data.f - (*SP).data.f); /* FSUB */\n");
                break;
                
            case Opcodes.DSUB:
                b.append("    SP--; SP[-1].data.d = (SP[-1].data.d - (*SP).data.d); /* DSUB */\n");
                break;
                
            case Opcodes.IMUL:
                b.append("    SP--; SP[-1].data.i = SP[-1].data.i * (*SP).data.i; /* IMUL */\n");
                break;
                
            case Opcodes.LMUL:
                b.append("    SP--; SP[-1].data.l = SP[-1].data.l * (*SP).data.l; /* LMUL */\n");
                break;
                
            case Opcodes.FMUL:
                b.append("    SP--; SP[-1].data.f = SP[-1].data.f * (*SP).data.f; /* FMUL */\n");
                break;
                
            case Opcodes.DMUL:
                b.append("    SP--; SP[-1].data.d = SP[-1].data.d * (*SP).data.d; /* DMUL */\n");
                break;
                
            case Opcodes.IDIV:
                b.append("    SP--; SP[-1].data.i = SP[-1].data.i / (*SP).data.i; /* IDIV */\n");
                break;
                
            case Opcodes.LDIV:
                b.append("    SP--; SP[-1].data.l = SP[-1].data.l / (*SP).data.l; /* LDIV */\n");
                break;
                
            case Opcodes.FDIV:
                b.append("    SP--; SP[-1].data.f = SP[-1].data.f / (*SP).data.f; /* FDIV */\n");
                break;
                
            case Opcodes.DDIV:
                b.append("    SP--; SP[-1].data.d = SP[-1].data.d / (*SP).data.d; /* DDIV */\n");
                break;
                
            case Opcodes.IREM:
                b.append("    SP--; SP[-1].data.i = SP[-1].data.i % (*SP).data.i; /* IREM */\n");
                break;
                
            case Opcodes.LREM:
                b.append("    SP--; SP[-1].data.l = SP[-1].data.l % (*SP).data.l; /* LREM */\n");
                break;
                
            case Opcodes.FREM:
                b.append("    SP--; SP[-1].data.f = fmod(SP[-1].data.f, (*SP).data.f); /* FREM */\n");
                break;
                
            case Opcodes.DREM:
                b.append("    SP--; SP[-1].data.d = fmod(SP[-1].data.d, (*SP).data.d); /* DREM */\n");
                break;
                
            case Opcodes.INEG:
                b.append("    SP[-1].data.i *= -1; /* INEG */\n");
                break;
                
            case Opcodes.LNEG:
                b.append("    SP[-1].data.l *= -1; /* LNEG */\n");
                break;
                
            case Opcodes.FNEG:
                b.append("    SP[-1].data.f *= -1; /* FNEG */\n");
                break;
                
            case Opcodes.DNEG:
                b.append("    SP[-1].data.d *= -1; /* DNEG */\n");
                break;
                
            case Opcodes.ISHL:
                b.append("    SP--; SP[-1].data.i = (SP[-1].data.i << (0x1f & (*SP).data.i)); /* ISHL */\n");
                break;
                
            case Opcodes.LSHL:
                b.append("    SP--; SP[-1].data.l = (SP[-1].data.l << (0x3f & (*SP).data.i)); /* LSHL */\n");
                break;
                
            case Opcodes.ISHR:
                b.append("    SP--; SP[-1].data.i = (SP[-1].data.i >> (0x1f & (*SP).data.i)); /* ISHR */\n");
                break;
                
            case Opcodes.LSHR:
                b.append("    SP--; SP[-1].data.l = (SP[-1].data.l >> (0x3f & (*SP).data.l)); /* LSHR */\n");
                break;
                
            case Opcodes.IUSHR:
                b.append("    SP--; SP[-1].data.i = (((unsigned int)SP[-1].data.i) >> (0x1f & ((unsigned int)(*SP).data.i))); /* IUSHR */\n");
                break;
                
            case Opcodes.LUSHR:
                b.append("    SP--; SP[-1].data.l = (((unsigned long long)SP[-1].data.l) >> (0x3f & ((unsigned long long)(*SP).data.i))); /* LUSHR */\n");
                break;
                
            case Opcodes.IAND:
                b.append("    SP--; SP[-1].data.i = SP[-1].data.i & (*SP).data.i; /* IAND */\n") ;
                break;
                
            case Opcodes.LAND:
                b.append("    SP--; SP[-1].data.l = SP[-1].data.l & (*SP).data.l; /* LAND */\n") ;
                break;
                
            case Opcodes.IOR:
                b.append("    SP--; SP[-1].data.i = SP[-1].data.i | (*SP).data.i; /* IOR */\n") ;
                break;
                
            case Opcodes.LOR:
                b.append("    SP--; SP[-1].data.l = SP[-1].data.l | (*SP).data.l; /* LOR */\n") ;
                break;
                
            case Opcodes.IXOR:
                b.append("    SP--; SP[-1].data.i = SP[-1].data.i ^ (*SP).data.i; /* IXOR */\n") ;
                break;
                
            case Opcodes.LXOR:
                b.append("    SP--; SP[-1].data.l = SP[-1].data.l ^ (*SP).data.l; /* LXOR */\n") ;
                break;
                
            case Opcodes.I2L:
                b.append("    SP[-1].data.l = SP[-1].data.i; /* I2L */\n");
                break;
                
            case Opcodes.I2F:
                b.append("    SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.i; /* I2F */\n");
                break;
                
            case Opcodes.I2D:
                b.append("    SP[-1].data.d = SP[-1].data.i; /* I2D */;\n");
                break;
                
            case Opcodes.L2I:
                b.append("    SP[-1].data.i = (JAVA_INT)SP[-1].data.l; /* L2I */\n");
                break;
                
            case Opcodes.L2F:
                b.append("    SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.l; /* L2F */\n");
                break;
                
            case Opcodes.L2D:
                b.append("    SP[-1].data.d = (JAVA_DOUBLE)SP[-1].data.l; /* L2D */\n");
                break;
                
            case Opcodes.F2I:
                b.append("    SP[-1].data.i = (JAVA_INT)SP[-1].data.f; /* F2I */\n");
                break;
                
            case Opcodes.F2L:
                b.append("    SP[-1].data.l = (JAVA_LONG)SP[-1].data.f; /* F2L */\n");
                break;
                
            case Opcodes.F2D:
                b.append("    SP[-1].data.d = SP[-1].data.f; /* F2D */\n");
                break;
                
            case Opcodes.D2I:
                b.append("    SP[-1].data.i = (JAVA_INT)SP[-1].data.d; /* D2I */\n");
                break;
                
            case Opcodes.D2L:
                b.append("    SP[-1].data.l = (JAVA_LONG)SP[-1].data.d; /* D2L */\n");
                break;
                
            case Opcodes.D2F:
                b.append("    SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.d; /* D2F */\n");
                break;
                
            case Opcodes.I2B:
                b.append("    SP[-1].data.i = ((SP[-1].data.i << 24) >> 24); /* I2B */\n");
                break;
                
            case Opcodes.I2C:
                b.append("    SP[-1].data.i = (SP[-1].data.i & 0xffff); /* I2C */\n");
                break;
                
            case Opcodes.I2S:
                b.append("    SP[-1].data.i = ((SP[-1].data.i << 16) >> 16); /* I2S */\n");
                break;
                
            case Opcodes.LCMP:
                b.append("    BC_LCMP();\n");
                break;
                
            case Opcodes.FCMPG:
            case Opcodes.FCMPL:
                b.append("    BC_FCMPL();\n");
                break;
                
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                b.append("    BC_DCMPL();\n");
                break;                  
                
            case Opcodes.IRETURN:
                appendSynchronized(b);
                
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); return SP[-1].data.i;\n");
//                    b.append(maxLocals);
//                    b.append(", stack, locals, methodBlockOffset); \n    return SP[-1].data.i;\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); return SP[-1].data.i;\n");
//                    b.append(maxLocals);
//                    b.append(", stack, locals); \n    return SP[-1].data.i;\n");
                }
                break;                
                
            case Opcodes.LRETURN:
                appendSynchronized(b);
                                
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \n    return POP_LONG();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); \n    return POP_LONG();\n");
                }
                break;                
                
            case Opcodes.FRETURN:
                appendSynchronized(b);
                
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \n    return POP_FLOAT();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); \n    return POP_FLOAT();\n");
                }
                break;                
                
            case Opcodes.DRETURN:
                appendSynchronized(b);
                
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \n    return POP_DOUBLE();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); \n    return POP_DOUBLE();\n");
                }
                break;
                
            case Opcodes.ARETURN:
                appendSynchronized(b);
                
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \n    return POP_OBJ();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); \n    return POP_OBJ();\n");
                }
                break;
                
            case Opcodes.RETURN:
                appendSynchronized(b);
                
                if(!hasInstructions) {
                    b.append("    return;\n");
                    break;
                }
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \n    return;\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread); \n    return;\n");
                }
                break;
                
            case Opcodes.ARRAYLENGTH:
                b.append("    { /* ARRAYLENGTH */\n" +
                    "        if(SP[-1].data.o == JAVA_NULL) { \n" +
                    "            throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); \n" +
                    "        }; \n" +
                    "        SP[-1].type = CN1_TYPE_INT; \n" +
                    "        SP[-1].data.i = (*((JAVA_ARRAY)SP[-1].data.o)).length; \n" +
                    "    }\n");
                break;                
                
            case Opcodes.ATHROW:
                //b.append("    NSLog(@\"Exception thrown %s %d %s %s\\n\", __FILE__, __LINE__, __PRETTY_FUNCTION__, __FUNCTION__);\n");
                b.append("    throwException(threadStateData, POP_OBJ());\n");
                break;                
                
            case Opcodes.MONITORENTER:
                b.append("    monitorEnter(threadStateData, POP_OBJ());\n");
                break;                
                
            case Opcodes.MONITOREXIT:
                b.append("    monitorExit(threadStateData, POP_OBJ());\n");
                break;                
                
            // int instructions
            case Opcodes.SIPUSH:
            case Opcodes.BIPUSH:
                b.append("    PUSH_INT(");
                b.append(value);
                b.append(");\n");
                break;                
                
            case Opcodes.NEWARRAY:
                switch(value) {
                    case 4: // boolean
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_BOOLEAN, sizeof(JAVA_ARRAY_BOOLEAN), 1));\n");
                        break;
                    case 5: // char
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1));\n");
                        break;
                    case 6: // float
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT), 1));\n");
                        break;
                    case 7: // double
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_DOUBLE, sizeof(JAVA_ARRAY_DOUBLE), 1));\n");
                        break;
                    case 8: // byte
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1));\n");
                        break;
                    case 9: // short
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_SHORT, sizeof(JAVA_ARRAY_SHORT), 1));\n");
                        break;
                    case 10: // int
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1));\n");
                        break;
                    case 11: // long 
                        b.append("    SP--; PUSH_OBJ(allocArray(threadStateData, (*SP).data.i, &class_array1__JAVA_LONG, sizeof(JAVA_ARRAY_LONG), 1));\n");
                        break;
                }
                break;
        }
    }

    public boolean isComplexInstruction() {
        return opcode == Opcodes.ATHROW;
    }

    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        StringBuilder b = new StringBuilder();
        //StringBuilder b2 = new StringBuilder();
        //if (typeVarName != null) {
        //    b2.append(typeVarName).append(" = ");
        //    
        //}
        if (varName != null) {
            b.append(varName).append(" = ");
        }
        switch(opcode) {
            case Opcodes.ACONST_NULL:
                b.append("JAVA_NULL /* ACONST_NULL */");
                //b2.append("CN1_TYPE_OBJECT");
                break;
                
            case Opcodes.ICONST_M1:
                b.append("-1 /* ICONST_M1 */");
                //b2.append("CN1_TYPE_INT");
                break;
                
            case Opcodes.ICONST_0:
                b.append("0 /* ICONST_0 */");
                //b2.append("CN1_TYPE_INT");
                break;

            case Opcodes.ICONST_1:
                b.append("1 /* ICONST_1 */");
                //b2.append("CN1_TYPE_INT");
                break;

            case Opcodes.ICONST_2:
                b.append("2 /* ICONST_2 */");
                //b2.append("CN1_TYPE_INT");
                break;

            case Opcodes.ICONST_3:
                b.append("3 /* ICONST_3 */");
                //b2.append("CN1_TYPE_INT");
                break;

            case Opcodes.ICONST_4:
                b.append("4/* ICONST_4 */");
                //b2.append("CN1_TYPE_INT");
                break;

            case Opcodes.ICONST_5:
                b.append("5 /* ICONST_5 */");
                ///b2.append("CN1_TYPE_INT");
                break;

            case Opcodes.LCONST_0:
                b.append("0 /* LCONST_0 */");
                //b2.append("CN1_TYPE_LONG");
                break;

            case Opcodes.LCONST_1:
                b.append("1 /* LCONST_1 */");
                //b2.append("CN1_TYPE_LONG");
                break;

            case Opcodes.FCONST_0:
                b.append("0 /* FCONST_0 */");
                //b2.append("CN1_TYPE_FLOAT");
                break;

            case Opcodes.FCONST_1:
                b.append("1 /* FCONST_1 */");
                //b2.append("CN1_TYPE_FLOAT");
                break;

            case Opcodes.FCONST_2:
                b.append("2 /* FCONST_2 */");
                //b2.append("CN1_TYPE_FLOAT");
                break;

            case Opcodes.DCONST_0:
                b.append("0 /* DCONST_0 */");
                //b2.append("CN1_TYPE_DOUBLE");
                break;

            case Opcodes.DCONST_1:
                b.append("1 /* DCONST_1 */");
                //b2.append("CN1_TYPE_DOUBLE");
                break;
       
                
            // int instructions
            case Opcodes.SIPUSH:
            case Opcodes.BIPUSH:
                b.append(value);
                b.append("/* SIPUSH */");
                //b2.append("CN1_TYPE_INT");
                break;   
                
            
            
            default:
                return false;
        }
        
        if (varName != null) {
            sb.append("    ");
            b.append("; \n");
        }
        
        
        //if (typeVarName != null) {
        //    sb.append(b2).append("; ");
        //}
        sb.append(b);
        
        return true;
    }
}
