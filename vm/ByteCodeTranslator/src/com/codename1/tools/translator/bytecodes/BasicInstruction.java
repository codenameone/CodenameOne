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
public class BasicInstruction extends Instruction {
    private final int value;
    private int maxStack;
    private int maxLocals;
    
    public BasicInstruction(int opcode, int value) {
        super(opcode);
        this.value = value;
    }
    
    public void setMaxes(int maxStack, int maxLocals) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
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
                b.append("    { CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); /* BALOAD */ \n" +
                    "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \n" +
                    "    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; \n" +
                    "    }\n");
                break;

            case Opcodes.CALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); /* CALOAD */\n" +
                    "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \n" +
                    "    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i];\n");
                break;
                
            case Opcodes.IALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); /* IALOAD */\n" +
                        "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \n" +
                        "    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i];\n");
                break;

            case Opcodes.SALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); \n" +
                        "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INT; \n" +
                        "    stack[stackPointer - 1].data.i = ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; /* SALOAD */\n");
                break;

            case Opcodes.LALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); /* LALOAD */\n" +
                        "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_LONG; \n" +
                        "    stack[stackPointer - 1].data.l = LONG_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 1].data.o, stack[stackPointer].data.i);\n");
                break;

            case Opcodes.FALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); /* FALOAD */\n" +
                        "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_FLOAT; \n" +
                        "    stack[stackPointer - 1].data.f = FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 1].data.o, stack[stackPointer].data.i);\n");
                break;

            case Opcodes.DALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); /* DALOAD */\n" +
                        "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_DOUBLE; \n" +
                        "    stack[stackPointer - 1].data.d = DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 1].data.o, stack[stackPointer].data.i);\n");
                break;

            case Opcodes.AALOAD:
                b.append("    CHECK_ARRAY_ACCESS(2, stack[stackPointer - 1].data.i); /* AALOAD */\n" +
                        "    stackPointer--; stack[stackPointer - 1].type = CN1_TYPE_INVALID; \n" +
                        "    stack[stackPointer - 1].data.o = ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)stack[stackPointer - 1].data.o).data)[stack[stackPointer].data.i]; \n" +
                        "    stack[stackPointer - 1].type = CN1_TYPE_OBJECT; retainObj(stack[stackPointer - 1].data.o);\n");
                break;

            case Opcodes.BASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); /* BASTORE */\n" +
                        "    ((JAVA_ARRAY_BYTE*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3;\n");
                break;

            case Opcodes.CASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); /* CASTORE */\n" +
                        "    ((JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3;\n\n");
                break;

            case Opcodes.SASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); /* SASTORE */\n" +
                        "    ((JAVA_ARRAY_SHORT*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3;\n");
                break;

            case Opcodes.IASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); /* IASTORE */\n" +
                        "    ((JAVA_ARRAY_INT*) (*(JAVA_ARRAY)stack[stackPointer - 3].data.o).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.i; stackPointer -= 3;\n");
                break;

            case Opcodes.LASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); /* LASTORE */\n" +
                        "    LONG_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 3].data.o, stack[stackPointer - 2].data.i) = stack[stackPointer - 1].data.l; stackPointer -= 3;\n");
                break;

            case Opcodes.FASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); /* FASTORE */\n" +
                        "    FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 3].data.o, stack[stackPointer - 2].data.i) = stack[stackPointer - 1].data.f; stackPointer -= 3;\n");
                break;

            case Opcodes.DASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); /* DASTORE */\n" +
                        "    DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)stack[stackPointer - 3].data.o, stack[stackPointer - 2].data.i) = stack[stackPointer - 1].data.d; stackPointer -= 3;\n");
                break;

            case Opcodes.AASTORE:
                b.append("    CHECK_ARRAY_ACCESS(3, stack[stackPointer - 2].data.i); { /* BC_AASTORE */\n" +
                        "    JAVA_OBJECT aastoreTmp = stack[stackPointer - 3].data.o; \n" +
                        "    ((JAVA_ARRAY_OBJECT*) (*(JAVA_ARRAY)aastoreTmp).data)[stack[stackPointer - 2].data.i] = stack[stackPointer - 1].data.o; \n" +
                        "    releaseObj(threadStateData, aastoreTmp); \n" +
                        "    retainObj(stack[stackPointer - 1].data.o); \n" +
                        "    stackPointer -= 3; }\n");
                break;

            case Opcodes.POP:
                b.append("    stackPointer--; /* POP */\n");
                break;

            case Opcodes.POP2:
                b.append("    popMany(threadStateData, 2, stack, &stackPointer); /* POP2 */\n");
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
                b.append("    BC_DUP(); /* DUP2 */\n");
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
                b.append("    swapStack(stack, stackPointer); /* SWAP */\n");
                break;
                
            case Opcodes.IADD:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i + stack[stackPointer].data.i; /* IADD */\n");
                break;
                
            case Opcodes.LADD:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l + stack[stackPointer].data.l; /* LADD */\n");
                break;
                
            case Opcodes.FADD:
                b.append("    stackPointer--; stack[stackPointer - 1].data.f = stack[stackPointer - 1].data.f + stack[stackPointer].data.f; /* FADD */\n");
                break;
                
            case Opcodes.DADD:
                b.append("    stackPointer--; stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.d + stack[stackPointer].data.d; /* DADD */\n");
                break;
                
            case Opcodes.ISUB:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i - stack[stackPointer].data.i); /* ISUB */\n");
                break;
                
            case Opcodes.LSUB:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = (stack[stackPointer - 1].data.l - stack[stackPointer].data.l); /* LSUB */\n");
                break;
                
            case Opcodes.FSUB:
                b.append("    stackPointer--; stack[stackPointer - 1].data.f = (stack[stackPointer - 1].data.f - stack[stackPointer].data.f); /* FSUB */\n");
                break;
                
            case Opcodes.DSUB:
                b.append("    stackPointer--; stack[stackPointer - 1].data.d = (stack[stackPointer - 1].data.d - stack[stackPointer].data.d); /* DSUB */\n");
                break;
                
            case Opcodes.IMUL:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i * stack[stackPointer].data.i; /* IMUL */\n");
                break;
                
            case Opcodes.LMUL:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l * stack[stackPointer].data.l; /* LMUL */\n");
                break;
                
            case Opcodes.FMUL:
                b.append("    stackPointer--; stack[stackPointer - 1].data.f = stack[stackPointer - 1].data.f * stack[stackPointer].data.f; /* FMUL */\n");
                break;
                
            case Opcodes.DMUL:
                b.append("    stackPointer--; stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.d * stack[stackPointer].data.d; /* DMUL */\n");
                break;
                
            case Opcodes.IDIV:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i / stack[stackPointer].data.i; /* IDIV */\n");
                break;
                
            case Opcodes.LDIV:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l / stack[stackPointer].data.l; /* LDIV */\n");
                break;
                
            case Opcodes.FDIV:
                b.append("    stackPointer--; stack[stackPointer - 1].data.f = stack[stackPointer - 1].data.f / stack[stackPointer].data.f; /* FDIV */\n");
                break;
                
            case Opcodes.DDIV:
                b.append("    stackPointer--; stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.d / stack[stackPointer].data.d; /* DDIV */\n");
                break;
                
            case Opcodes.IREM:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i % stack[stackPointer].data.i; /* IREM */\n");
                break;
                
            case Opcodes.LREM:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l % stack[stackPointer].data.l; /* LREM */\n");
                break;
                
            case Opcodes.FREM:
                b.append("    stackPointer--; stack[stackPointer - 1].data.f = fmod(stack[stackPointer - 1].data.f, stack[stackPointer].data.f); /* FREM */\n");
                break;
                
            case Opcodes.DREM:
                b.append("    stackPointer--; stack[stackPointer - 1].data.d = fmod(stack[stackPointer - 1].data.d, stack[stackPointer].data.d); /* DREM */\n");
                break;
                
            case Opcodes.INEG:
                b.append("    stack[stackPointer - 1].data.i *= -1; /* INEG */\n");
                break;
                
            case Opcodes.LNEG:
                b.append("    stack[stackPointer - 1].data.l *= -1; /* LNEG */\n");
                break;
                
            case Opcodes.FNEG:
                b.append("    stack[stackPointer - 1].data.f *= -1; /* FNEG */\n");
                break;
                
            case Opcodes.DNEG:
                b.append("    stack[stackPointer - 1].data.d *= -1; /* DNEG */\n");
                break;
                
            case Opcodes.ISHL:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i << (0x1f & stack[stackPointer].data.i)); /* ISHL */\n");
                break;
                
            case Opcodes.LSHL:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = (stack[stackPointer - 1].data.l << (0x3f & stack[stackPointer].data.l)); /* LSHL */\n");
                break;
                
            case Opcodes.ISHR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i >> (0x1f & stack[stackPointer].data.i)); /* ISHR */\n");
                break;
                
            case Opcodes.LSHR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = (stack[stackPointer - 1].data.l >> (0x3f & stack[stackPointer].data.l)); /* LSHR */\n");
                break;
                
            case Opcodes.IUSHR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = (((unsigned int)stack[stackPointer - 1].data.i) >> (0x1f & ((unsigned int)stack[stackPointer].data.i))); /* IUSHR */\n");
                break;
                
            case Opcodes.LUSHR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = (((unsigned long long)stack[stackPointer - 1].data.l) << (0x3f & ((unsigned long long)stack[stackPointer].data.l))); /* LUSHR */\n");
                break;
                
            case Opcodes.IAND:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i & stack[stackPointer].data.i; /* IAND */\n") ;
                break;
                
            case Opcodes.LAND:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l & stack[stackPointer].data.l; /* LAND */\n") ;
                break;
                
            case Opcodes.IOR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i | stack[stackPointer].data.i; /* IOR */\n") ;
                break;
                
            case Opcodes.LOR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l | stack[stackPointer].data.l; /* LOR */\n") ;
                break;
                
            case Opcodes.IXOR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.i = stack[stackPointer - 1].data.i ^ stack[stackPointer].data.i; /* IXOR */\n") ;
                break;
                
            case Opcodes.LXOR:
                b.append("    stackPointer--; stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.l ^ stack[stackPointer].data.l; /* LXOR */\n") ;
                break;
                
            case Opcodes.I2L:
                b.append("    stack[stackPointer - 1].data.l = stack[stackPointer - 1].data.i; /* I2L */\n");
                break;
                
            case Opcodes.I2F:
                b.append("    stack[stackPointer - 1].data.f = (JAVA_FLOAT)stack[stackPointer - 1].data.i; /* I2F */\n");
                break;
                
            case Opcodes.I2D:
                b.append("    stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.i; /* I2D */;\n");
                break;
                
            case Opcodes.L2I:
                b.append("    stack[stackPointer - 1].data.i = (JAVA_INT)stack[stackPointer - 1].data.l; /* L2I */\n");
                break;
                
            case Opcodes.L2F:
                b.append("    stack[stackPointer - 1].data.f = (JAVA_FLOAT)stack[stackPointer - 1].data.l; /* L2F */\n");
                break;
                
            case Opcodes.L2D:
                b.append("    stack[stackPointer - 1].data.d = (JAVA_DOUBLE)stack[stackPointer - 1].data.l; /* L2D */\n");
                break;
                
            case Opcodes.F2I:
                b.append("    stack[stackPointer - 1].data.i = (JAVA_INT)stack[stackPointer - 1].data.f; /* F2I */\n");
                break;
                
            case Opcodes.F2L:
                b.append("    stack[stackPointer - 1].data.l = (JAVA_LONG)stack[stackPointer - 1].data.f; /* F2L */\n");
                break;
                
            case Opcodes.F2D:
                b.append("    stack[stackPointer - 1].data.d = stack[stackPointer - 1].data.f; /* F2D */\n");
                break;
                
            case Opcodes.D2I:
                b.append("    stack[stackPointer - 1].data.i = (JAVA_INT)stack[stackPointer - 1].data.d; /* D2I */\n");
                break;
                
            case Opcodes.D2L:
                b.append("    stack[stackPointer - 1].data.l = (JAVA_LONG)stack[stackPointer - 1].data.d; /* D2L */\n");
                break;
                
            case Opcodes.D2F:
                b.append("    stack[stackPointer - 1].data.f = (JAVA_FLOAT)stack[stackPointer - 1].data.d; /* D2F */\n");
                break;
                
            case Opcodes.I2B:
                b.append("    stack[stackPointer - 1].data.i = ((stack[stackPointer - 1].data.i << 24) >> 24); /* I2B */\n");
                break;
                
            case Opcodes.I2C:
                b.append("    stack[stackPointer - 1].data.i = (stack[stackPointer - 1].data.i & 0xffff); /* I2C */\n");
                break;
                
            case Opcodes.I2S:
                b.append("    stack[stackPointer - 1].data.i = ((stack[stackPointer - 1].data.i << 16) >> 16); /* I2S */\n");
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
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, ");
                    b.append(maxLocals);
                    b.append(", stack, locals, methodBlockOffset); \n    return stack[stackPointer - 1].data.i;\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer - 1, ");
                    b.append(maxLocals);
                    b.append(", stack, locals); \n    return stack[stackPointer - 1].data.i;\n");
                }
                break;                
                
            case Opcodes.LRETURN:
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, ");
                    b.append(maxLocals);
                    b.append(", stack, locals, methodBlockOffset); \n    return POP_LONG();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer - 1, ");
                    b.append(maxLocals);
                    b.append(", stack, locals); \n    return POP_LONG();\n");
                }
                break;                
                
            case Opcodes.FRETURN:
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, ");
                    b.append(maxLocals);
                    b.append(", stack, locals, methodBlockOffset); \n    return POP_FLOAT();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer - 1, ");
                    b.append(maxLocals);
                    b.append(", stack, locals); \n    return POP_FLOAT();\n");
                }
                break;                
                
            case Opcodes.DRETURN:
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, ");
                    b.append(maxLocals);
                    b.append(", stack, locals, methodBlockOffset); \n    return POP_DOUBLE();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer - 1, ");
                    b.append(maxLocals);
                    b.append(", stack, locals); \n    return POP_DOUBLE();\n");
                }
                break;
                
            case Opcodes.ARETURN:
                b.append("    retainObj(PEEK_OBJ(1));\n");
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, ");
                    b.append(maxLocals);
                    b.append(", stack, locals, methodBlockOffset); \n    return POP_OBJ_NO_RELEASE();\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer - 1, ");
                    b.append(maxLocals);
                    b.append(", stack, locals); \n    return POP_OBJ_NO_RELEASE();\n");
                }
                break;
                
            case Opcodes.RETURN:
                if(!hasInstructions) {
                    b.append("    return;\n");
                    break;
                }
                if(TryCatch.isTryCatchInMethod()) {
                    b.append("    releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, stackPointer, ");
                    b.append(maxLocals);
                    b.append(", stack, locals, methodBlockOffset); \n    return;\n");
                } else {
                    b.append("    releaseForReturn(threadStateData, cn1LocalsBeginInThread, stackPointer - 1, ");
                    b.append(maxLocals);
                    b.append(", stack, locals); \n    return;\n");
                }
                break;
                
            case Opcodes.ARRAYLENGTH:
                b.append("    { /* ARRAYLENGTH */\n" +
                    "    if(stack[stackPointer - 1].data.o == JAVA_NULL) { \n" +
                    "        throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)); \n" +
                    "    }; \n" +
                    "    stack[stackPointer - 1].type = CN1_TYPE_INT; \n" +
                    "    stack[stackPointer - 1].data.i = (*((JAVA_ARRAY)stack[stackPointer - 1].data.o)).length; \n" +
                    "}\n");
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
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_BOOLEAN, sizeof(JAVA_ARRAY_BOOLEAN), 1));\n");
                        break;
                    case 5: // char
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1));\n");
                        break;
                    case 6: // float
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT), 1));\n");
                        break;
                    case 7: // double
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_DOUBLE, sizeof(JAVA_ARRAY_DOUBLE), 1));\n");
                        break;
                    case 8: // byte
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1));\n");
                        break;
                    case 9: // short
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_SHORT, sizeof(JAVA_ARRAY_SHORT), 1));\n");
                        break;
                    case 10: // int
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1));\n");
                        break;
                    case 11: // long 
                        b.append("    stackPointer--; PUSH_OBJ(allocArray(threadStateData, stack[stackPointer].data.i, &class_array1__JAVA_LONG, sizeof(JAVA_ARRAY_LONG), 1));\n");
                        break;
                }
                break;
        }
    }

    public boolean isComplexInstruction() {
        return opcode == Opcodes.ATHROW;
    }
}
