/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.tools.translator.bytecodes;

import java.util.List;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author shannah
 */
public class CustomJump extends Instruction {
    private static int jsrCounter = 1;
    private Label label;
    
    /**
     * If customCompareCode is set, it will be used for the comparison 
     * leading to the jump instead of the default stack-based comparison.
     */
    private String customCompareCode;
    
    public CustomJump(Label label, String customCompareCode) {
        super(-1);
        this.label = label;
        LabelInstruction.labelIsUsed(label);
        this.customCompareCode = customCompareCode;
    }
    
    public static CustomJump create(Jump orig, String customCompareCode) {
        return new CustomJump(orig.getLabel(), customCompareCode);
    }
    
    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> instructions) {
        b.append("    ");
        b.append(customCompareCode);
        
        if(TryCatch.isTryCatchInMethod()) {
            b.append("JUMP_TO(label_");
            b.append(label.toString());
            b.append(", ");
            b.append(LabelInstruction.getLabelCatchDepth(label, instructions));
            b.append(");\n");
        } else {
            b.append("goto label_");
            b.append(label.toString());
            b.append(";\n");
        }
    }
    
    public void setCustomCompareCode(String code) {
        this.customCompareCode = code;
    }
}
