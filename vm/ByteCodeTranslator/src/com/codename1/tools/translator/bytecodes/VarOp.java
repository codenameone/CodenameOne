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

import com.codename1.tools.translator.BytecodeMethod;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class VarOp extends Instruction {
    private int var;
    public VarOp(int opcode, int var) {
        super(opcode);
        this.var = var;
    }
    
    public int getIndex() {
        return var;
    }
    
    @Override
    public void appendInstruction(StringBuilder b) {
        b.append("    ");
        switch(opcode) {
            case Opcodes.ILOAD:
                b.append("stack[stackPointer].type = CN1_TYPE_INT; /* ILOAD */ \n" +
                        "    stack[stackPointer].data.i = ilocals_");
                b.append(var);
                b.append("_; \n    stackPointer++;\n");
                return;
            case Opcodes.LLOAD:
                b.append("BC_LLOAD(");
                break;
            case Opcodes.FLOAD:
                b.append("BC_FLOAD(");
                break;
            case Opcodes.DLOAD:
                b.append("BC_DLOAD(");
                break;
            case Opcodes.ALOAD:
                b.append("BC_ALOAD(");
                break;
            case Opcodes.ISTORE:
                b.append("BC_ISTORE(");
                break;
            case Opcodes.LSTORE:
                b.append("BC_LSTORE(");
                break;
            case Opcodes.FSTORE:
                b.append("BC_FSTORE(");
                break;
            case Opcodes.DSTORE:
                b.append("BC_DSTORE(");
                break;
            case Opcodes.ASTORE:
                b.append("BC_ASTORE(");
                break;
            case Opcodes.RET:
                b.append("/* RET TODO */");
                //b.append("goto label_");
                //b.append(var);
                //b.append("; /* RET */\n");
                return;
            case Opcodes.SIPUSH:
            case Opcodes.BIPUSH:
                b.append("PUSH_INT(");
                break;
            case Opcodes.NEWARRAY:
                switch(var) {
                    case 4: // boolean
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_BOOLEAN, sizeof(JAVA_ARRAY_BOOLEAN), 1));\n");
                        break;
                    case 5: // char
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1));\n");
                        break;
                    case 6: // float
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT), 1));\n");
                        break;
                    case 7: // double
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_DOUBLE, sizeof(JAVA_ARRAY_DOUBLE), 1));\n");
                        break;
                    case 8: // byte
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1));\n");
                        break;
                    case 9: // short
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_SHORT, sizeof(JAVA_ARRAY_SHORT), 1));\n");
                        break;
                    case 10: // int
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1));\n");
                        break;
                    case 11: // long 
                        b.append("PUSH_OBJ(allocArray(threadStateData, POP_INT(), &class_array1__JAVA_LONG, sizeof(JAVA_ARRAY_LONG), 1));\n");
                        break;
                }
                return;
            default:
                throw new RuntimeException("Missing opcode: " + opcode);
        }
        b.append(var);
        b.append(");\n");
    }

}
