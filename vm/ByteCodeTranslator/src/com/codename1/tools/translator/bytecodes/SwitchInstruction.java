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
public class SwitchInstruction extends Instruction {
    private Label dflt;
    private int[] keys;
    private Label[] labels;
    
    public SwitchInstruction(Label dflt, int[] keys, Label[] labels) {
        super(Opcodes.TABLESWITCH);
        this.dflt = dflt;
        this.keys = keys;
        this.labels = labels;
        for(int iter = 0 ; iter < keys.length ; iter++) {
            LabelInstruction.labelIsUsed(labels[iter]);
        }
        LabelInstruction.labelIsUsed(dflt);
    }
    
    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> instructions) {
        b.append("    stackPointer--;\n    switch(stack[stackPointer].data.i) {\n");
        for(int iter = 0 ; iter < keys.length ; iter++) {
            b.append("        case ");
            b.append(keys[iter]);
            if(TryCatch.isTryCatchInMethod()) {
                b.append(": JUMP_TO(label_");
                b.append(labels[iter].toString());
                b.append(", ");
                b.append(LabelInstruction.getLabelCatchDepth(labels[iter], instructions));
                b.append(");\n");
            } else {
                b.append(": goto label_");
                b.append(labels[iter].toString());
                b.append(";\n");
            }
        }
        
        if(dflt != null) {
            if(TryCatch.isTryCatchInMethod()) {
                b.append("        default: JUMP_TO(label_");
                b.append(dflt.toString());
                b.append(", ");
                b.append(LabelInstruction.getLabelCatchDepth(dflt, instructions));
                b.append(");\n");
            } else {
                b.append("        default: goto label_");
                b.append(dflt.toString());
                b.append(";\n");
            }
        }
        b.append("    }\n");
    }

}
