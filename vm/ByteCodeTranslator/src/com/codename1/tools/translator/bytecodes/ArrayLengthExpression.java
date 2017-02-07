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
public class ArrayLengthExpression extends Instruction implements AssignableExpression {

    private Instruction target;
    private Instruction arrayLenInstruction;
    
    private ArrayLengthExpression() {
        super(-3);
    }
    
    public static int tryReduce(List<Instruction> instructions, int index) {
        if (index < 1) {
            return -1;
        }
        Instruction instr = instructions.get(index);
        Instruction prev = instructions.get(index-1);
        if (instr.getOpcode() == Opcodes.ARRAYLENGTH && prev instanceof AssignableExpression) {
            ArrayLengthExpression out = new ArrayLengthExpression();
            out.target = prev;
            out.arrayLenInstruction = instr;
            
            instructions.remove(index-1);
            instructions.remove(index-1);
            instructions.add(index-1, out);
            
            return index-1;
            
        }
        return -1;
    }
    
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        if (target != null) {
            target.addDependencies(dependencyList);
        }
        if (arrayLenInstruction != null) {
            arrayLenInstruction.addDependencies(dependencyList);
        }
        
    }

    
    
    @Override
    public void appendInstruction(StringBuilder b) {
        if (target != null) {
            target.appendInstruction(b);
        }
        if (arrayLenInstruction != null) {
            arrayLenInstruction.appendInstruction(b);
        }
    }

    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        if (target != null) {
            target.appendInstruction(b, l);
        }
        if (arrayLenInstruction != null) {
            arrayLenInstruction.appendInstruction(b, l);
        }
    }

    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        
        StringBuilder b = new StringBuilder();
        if (varName != null) {
            b.append(varName).append(" = ");
        }
        
        //switch (target.getOpcode()) {
        //    case Opcodes.ALOAD: {
                if (target instanceof AssignableExpression) {
                    StringBuilder b2 = new StringBuilder();
                    boolean res = ((AssignableExpression)target).assignTo(null, b2);
                    if (!res) {
                        return false;
                    }
                    //SP[-1].data.o == JAVA_NULL ? throwException_R_int(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)) : (*((JAVA_ARRAY)SP[-1].data.o)).length;
                    //b.append("((").append(b2).append(" == JAVA_NULL) ? throwException_R_int(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData)) : (*((JAVA_ARRAY)").append(b2).append(")).length)");
                    b.append("CN1_ARRAY_LENGTH(").append(b2.toString().trim()).append(")");
                } else {
                    return false;
        //        break;
        //    }
        //    default: {
        //        return false;
        //    }
        }
        if (varName != null) {
            b.append(";\n");
        }
                
        sb.append(b);
        return true;
    }
    
    
    
}
