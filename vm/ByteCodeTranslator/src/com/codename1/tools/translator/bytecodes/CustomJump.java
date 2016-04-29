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
