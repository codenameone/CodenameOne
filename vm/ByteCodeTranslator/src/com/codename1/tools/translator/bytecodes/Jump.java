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
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class Jump extends Instruction {
    private static int jsrCounter = 1;
    private Label label;
    
    public Jump(int opcode, Label label) {
        super(opcode);
        this.label = label;
        LabelInstruction.labelIsUsed(label);
    }
    
    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> instructions) {
        b.append("    ");
        switch(opcode) {
            case Opcodes.IFEQ:
                b.append("if(POP_INT() == 0) /* IFEQ */ ");
                break;
            case Opcodes.IFNE:
                b.append("if(POP_INT() != 0) /* IFNE */ ");
                break;
            case Opcodes.IFLT:
                b.append("if(POP_INT() < 0) /* IFLT */ ");
                break;
            case Opcodes.IFGE:
                b.append("if(POP_INT() >= 0) /* IFGE */ ");
                break;
            case Opcodes.IFGT:
                b.append("if(POP_INT() > 0) /* IFGT */ ");
                break;
            case Opcodes.IFLE:
                b.append("if(POP_INT() <= 0) /* IFLE */ ");
                break;
            case Opcodes.IF_ICMPEQ:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.i == stack[stackPointer + 1].data.i) /* IF_ICMPEQ */ ");
                break;
            case Opcodes.IF_ICMPNE:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.i != stack[stackPointer + 1].data.i) /* IF_ICMPNE */ ");
                break;
            case Opcodes.IF_ICMPLT:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.i < stack[stackPointer + 1].data.i) /* IF_ICMPLT */ ");
                break;
            case Opcodes.IF_ICMPGE:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.i >= stack[stackPointer + 1].data.i) /* IF_ICMPGE */ ");
                break;
            case Opcodes.IF_ICMPGT:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.i > stack[stackPointer + 1].data.i) /* IF_ICMPGT */ ");
                break;
            case Opcodes.IF_ICMPLE:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.i <= stack[stackPointer + 1].data.i) /* IF_ICMPLE */ ");
                break;
            case Opcodes.IF_ACMPEQ:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.o == stack[stackPointer + 1].data.o) /* IF_ACMPEQ */ ");
                break;
            case Opcodes.IF_ACMPNE:
                b.append("stackPointer -= 2; if(stack[stackPointer].data.o != stack[stackPointer + 1].data.o) /* IF_ACMPNE */ ");
                break;
            case Opcodes.GOTO:
                // this space intentionally left blank
                break;
            case Opcodes.JSR:
                b.append("/* JSR TODO */");
                /*b.append("PUSH_")
                b.append("goto label_");
                b.append(label.toString());
                b.append(";\n");
                b.append("JSR_RETURN_LABEL_");
                b.append(jsrCounter);
                b.append(":");
                jsrCounter++;*/
                return;
            case Opcodes.IFNULL:
                b.append("if(POP_OBJ() == JAVA_NULL) /* IFNULL */ ");
                break;
            case Opcodes.IFNONNULL:
                b.append("if(POP_OBJ() != JAVA_NULL) /* IFNONNULL */ ");
                break;
        }
        /*if(TryCatch.isTryCatchInMethod()) {
            b.append("JUMP_TO(label_");
            b.append(label.toString());
            b.append(", ");
            b.append(LabelInstruction.getLabelCatchDepth(label, instructions));
            b.append(");\n");
        } else {*/
            b.append("goto label_");
            b.append(label.toString());
            b.append(";\n");
        //}
    }

}
