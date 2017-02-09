/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.tools.translator.bytecodes;

import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author shannah
 */
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
