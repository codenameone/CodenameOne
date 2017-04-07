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
public class VarOp extends Instruction implements AssignableExpression {
    private int var;
    public VarOp(int opcode, int var) {
        super(opcode);
        this.var = var;
    }
    
    public int getIndex() {
        return var;
    }
    
    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        StringBuilder b = new StringBuilder();
        
        /*
        if (typeVarName != null) {
            switch (opcode) {
                case Opcodes.ALOAD:
                    b.append("locals[");
                    b.append(var);
                    b.append("].type = CN1_TYPE_OBJECT; ");
                    break;
            }
        }*/
        if (varName != null) {
            b.append("    ");
            b.append(varName).append(" = ");
        }
        switch(opcode) {
            case Opcodes.ILOAD:
                b.append("ilocals_");
                b.append(var);
                b.append("_");
                break;
            case Opcodes.LLOAD:
                b.append("llocals_");
                b.append(var);
                b.append("_");
                break;
            case Opcodes.FLOAD:
                b.append("flocals_");
                b.append(var);
                b.append("_");
                break;
            case Opcodes.DLOAD:
                b.append("dlocals_");
                b.append(var);
                b.append("_");
                break;
            case Opcodes.ALOAD:
                if (getMethod() != null && !getMethod().isStatic() && var == 0) {
                    b.append("__cn1ThisObject");
                } else {
                    b.append("locals[");
                    b.append(var);
                    b.append("].data.o");
                }
                break;
            default:
                return false;
                
        }
        if (varName != null) {
            b.append(";\n");
        }
        sb.append(b);
        return true;
    }
    
    public boolean assignFrom(AssignableExpression ex, StringBuilder b) {
        b.append("    /* VarOp.assignFrom */ ");
        switch (opcode) {
            case Opcodes.ISTORE:
                return ex.assignTo("ilocals_"+var+"_", b);
                
            case Opcodes.LSTORE:
                return ex.assignTo("llocals_"+var+"_", b);
            case Opcodes.FSTORE:
                return ex.assignTo("flocals_"+var+"_", b);
            case Opcodes.DSTORE:
                return ex.assignTo("dlocals_"+var+"_", b);
            case Opcodes.ASTORE: {
                StringBuilder sb = new StringBuilder();
                sb.append("locals[").append(var).append("].type=CN1_TYPE_INVALID;");
                boolean res = ex.assignTo("locals["+var+"].data.o", sb);
                if (!res) {
                    return false;
                }
                sb.append("locals[").append(var).append("].type=CN1_TYPE_OBJECT;");
                b.append(sb);
                return true;
            }
                
        }
        b.append("\n");
        return false;
    }
    
    public boolean assignFrom(CustomInvoke ex, StringBuilder b) {
        b.append("    /* VarOp.assignFrom */ ");
        StringBuilder sb = new StringBuilder();
        switch (opcode) {
            case Opcodes.ISTORE:
                if (ex.appendExpression(sb)) {
                    b.append("ilocals_").append(var).append("_ = ").append(sb.toString().trim()).append(";\n");
                    return true;
                }
            break;
                
            case Opcodes.LSTORE:
                if (ex.appendExpression(sb)) {
                    b.append("llocals_").append(var).append("_ = ").append(sb.toString().trim()).append(";\n");
                    return true;
                }
            break;
            case Opcodes.FSTORE:
                if (ex.appendExpression(sb)) {
                    b.append("flocals_").append(var).append("_ = ").append(sb.toString().trim()).append(";\n");
                    return true;
                }
            break;
                
            case Opcodes.DSTORE:
                if (ex.appendExpression(sb)) {
                    b.append("dlocals_").append(var).append("_ = ").append(sb.toString().trim()).append(";\n");
                    return true;
                }
            break;

            case Opcodes.ASTORE: {
                StringBuilder sb2 = new StringBuilder();
                //sb2.append("locals[").append(var).append("].type=CN1_TYPE_INVALID; ");
                if (ex.appendExpression(sb)) {
                    sb2.append("locals[").append(var).append("].data.o = ").append(sb.toString().trim()).append(";");
                    sb2.append("locals[").append(var).append("].type=CN1_TYPE_OBJECT;");
                    b.append(sb2);
                    return true;
                }
                
            break;
               
            }
                
        }
        //b.append("\n");
        return false;
    }
    
    @Override
    public void appendInstruction(StringBuilder b) {
        b.append("    ");
        switch(opcode) {
            case Opcodes.ILOAD:
                b.append("(*SP).type = CN1_TYPE_INT; /* ILOAD */ \n" +
                        "    (*SP).data.i = ilocals_");
                b.append(var);
                b.append("_; \n    SP++;\n");
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
