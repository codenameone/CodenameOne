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
public class ArrayLoadExpression extends Instruction implements AssignableExpression {
    private Instruction targetArrayInstruction;
    private Instruction indexInstruction;
    private Instruction loadInstruction;
    

    private ArrayLoadExpression() {
        super(-4);
    }
    
    
    public static int tryReduce(List<Instruction> instructions, int index) {
        Instruction instr = instructions.get(index);
        switch (instr.getOpcode()) {
            case Opcodes.AALOAD:
            case Opcodes.FALOAD:
            case Opcodes.CALOAD:
            case Opcodes.DALOAD:
            case Opcodes.BALOAD:
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.SALOAD:
                break;
            default:
                return -1;
        }
        
        if (index < 2) {
            return -1;
        }
        
        Instruction indexInstr = instructions.get(index-1);
        if (!(indexInstr instanceof AssignableExpression)) {
            return -1;
        }
        
        Instruction arrInstr = instructions.get(index-2);
        if (!(arrInstr instanceof AssignableExpression)) {
            return -1;
        }
        
        ArrayLoadExpression out = new ArrayLoadExpression();
        out.loadInstruction = instr;
        out.indexInstruction = indexInstr;
        out.targetArrayInstruction = arrInstr;
        
        instructions.remove(index-2);
        instructions.remove(index-2);
        instructions.remove(index-2);
        instructions.add(index-2, out);
        return index-2;
    }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        if (indexInstruction != null) {
            indexInstruction.addDependencies(dependencyList);
        }
        if (targetArrayInstruction != null) {
            targetArrayInstruction.addDependencies(dependencyList);
        }
        if (loadInstruction != null) {
            loadInstruction.addDependencies(dependencyList);
        }
                
        
    }

    
    
    @Override
    public void appendInstruction(StringBuilder b) {
        if (targetArrayInstruction != null) {
            targetArrayInstruction.appendInstruction(b);
        }
        
        if (indexInstruction != null) {
            indexInstruction.appendInstruction(b);
        }
        
        if (loadInstruction != null) {
            loadInstruction.appendInstruction(b);
        }
    }

    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        if (targetArrayInstruction != null) {
            targetArrayInstruction.appendInstruction(b, l);
        }
        
        if (indexInstruction != null) {
            indexInstruction.appendInstruction(b, l);
        }
        
        if (loadInstruction != null) {
            loadInstruction.appendInstruction(b, l);
        }
            
    }

    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        StringBuilder b = new StringBuilder();
        if (varName != null) {
            b.append(varName).append("=");
        }
        
        b.append("CN1_ARRAY_ELEMENT_");
        String arrayType = null;
        switch (loadInstruction.getOpcode()) {
            case Opcodes.FALOAD:
                arrayType = "FLOAT";
                break;
            case Opcodes.DALOAD:
                arrayType = "DOUBLE";
                break;
            case Opcodes.LALOAD:
                arrayType = "LONG";
                break;
            case Opcodes.IALOAD:
                arrayType = "INT";
                break;
            case Opcodes.BALOAD:
                arrayType = "BYTE";
                break;
            case Opcodes.CALOAD:
                arrayType = "CHAR";
                break;
            case Opcodes.AALOAD:
                arrayType = "OBJECT";
                break;
            case Opcodes.SALOAD:
                arrayType = "SHORT";
                break;
                
        }
        b.append(arrayType).append("(");
        if (targetArrayInstruction instanceof AssignableExpression) {
            StringBuilder sb2 = new StringBuilder();
            boolean res = ((AssignableExpression)targetArrayInstruction).assignTo(null, sb2);
            if (!res) {
                return false;
            }
            b.append(sb2.toString().trim());
        } else {
            return false;
        }
        b.append(", ");
        if (indexInstruction instanceof AssignableExpression) {
            StringBuilder sb2 = new StringBuilder();
            
            boolean res = ((AssignableExpression)indexInstruction).assignTo(null, sb2);
            if (!res) {
                return false;
            }
            b.append(sb2.toString().trim());
        } else {
            return false;
        }
        b.append(")");
        if (varName != null) {
            b.append(";\n");
        }
        
        sb.append(b);
        return true;
    }
}
