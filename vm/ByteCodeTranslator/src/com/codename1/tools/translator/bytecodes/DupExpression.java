/*
 * Copyright (c) 2017, Codename One and/or its affiliates. All rights reserved.
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

public class DupExpression extends Instruction implements AssignableExpression {
    private Instruction sourceInstr;
    private Instruction dupInstr;
    
    private DupExpression() {
        super(-88);
    }

    @Override
    public void appendInstruction(StringBuilder b) {
        if (dupInstr != null) {
            dupInstr.appendInstruction(b);
        }
    }

    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        if (dupInstr != null) {
            dupInstr.appendInstruction(b, l);
        }
    }

    @Override
    public void addDependencies(List<String> dependencyList) {
        if (dupInstr != null) {
            dupInstr.addDependencies(dependencyList);
        }
    }
    
    
    
    
    
    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        StringBuilder b = new StringBuilder();
        if (varName != null) {
            b.append("    ").append(varName).append(" = ");
        }
        boolean ret = false;
        if (sourceInstr != null) {
            switch (sourceInstr.getOpcode()) {
                case Opcodes.ALOAD: {
                    if (sourceInstr instanceof AssignableExpression) {
                        StringBuilder devNull = new StringBuilder();
                        if (((AssignableExpression)sourceInstr).assignTo(null, devNull)) {
                            b.append(devNull.toString().trim());
                            ret = true;
                        }
                    }
                    break;
                }
            }
        }
        if (varName != null) {
            b.append(";\n");
        }
        if (!ret) {
            return false;
        }
        sb.append(b);
        return true;
    }
    
    public static int tryReduce(List<Instruction> instructions, int index) {
        Instruction instr = instructions.get(index);
        if (index < 1 || instr.getOpcode() != Opcodes.DUP) {
            return -1;
        }
        
        Instruction prev = instructions.get(index-1);
        StringBuilder devNull = new StringBuilder();
        if (prev.getOpcode() == Opcodes.ALOAD && prev instanceof AssignableExpression && ((AssignableExpression)prev).assignTo(null, devNull)) {
            DupExpression dup = new DupExpression();
            dup.sourceInstr = prev;
            dup.dupInstr = instr;
            instructions.remove(index);
            instructions.add(index, dup);
            return index;
        }
        return -1;
    }
    
}
