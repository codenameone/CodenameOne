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

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class TryCatch extends Instruction {
    private Label start;
    private Label end;
    private Label handler;
    private String type;
    /*
     * Restores the pre-fix eager catch-depth computation, which cached a wrong END_TRY
     * depth for every nested try (see LabelInstruction.setCatchDepthInstructions). It
     * exists so NestedTryIntegrationTest can require the old behaviour to reproduce the
     * escape -- a gate that cannot be shown to fail is not a gate. Nothing else reads it.
     */
    private static boolean legacyCatchDepth;
    private static boolean firstException;
    private static boolean hasTryCatch;
    private static int counter;
    
    public static void reset() {
        // Re-read per method rather than once per class load: the integration test drives
        // the translator IN PROCESS, so a static final captured at class-init would answer
        // for whichever run happened to load the class first.
        legacyCatchDepth = "true".equalsIgnoreCase(
                com.codename1.tools.translator.Util.getProperty("cn1.legacyCatchDepth", "false"));
        firstException = true;
        hasTryCatch = false;
        counter = 1;
    }
    
    public TryCatch(Label start, Label end, Label handler, String type) {
        super(-1);
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
        LabelInstruction.labelIsUsed(handler);
    }

    @Override
    public void addDependencies(List<String> dependencyList) {
        if(type != null) {
            String t = type.replace('.', '_').replace('/', '_').replace('$', '_');
            if(!dependencyList.contains(t)) {
                dependencyList.add(t);
            }
        }
    }
    
    public static boolean isTryCatchInMethod() {
        return hasTryCatch;
    }

    public Label getStart() {
        return start;
    }

    public Label getEnd() {
        return end;
    }

    public Label getHandler() {
        return handler;
    }

    public String getType() {
        return type;
    }
    
    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> instructions) {
        hasTryCatch = true;
        if(firstException) {
            // we need to append basic exception handling logic
            //b.append("    DEFINE_EXCEPTION_HANDLING_CONSTANTS();\n");
            firstException = false;
        }
        
        String cid = "0";
        if(type != null) {
            cid = "cn1_class_id_" + type.replace('/', '_').replace('$', '_');
        } 
        LabelInstruction.addTryBeginLabel(start, cid, counter);
        // VOLATILE is load-bearing: both variables are assigned at TRY-ENTRY,
        // i.e. AFTER the setjmp in DEFINE_CATCH_BLOCK, and read back in the
        // catch handler -- which runs after a longjmp. C11 7.13.2.1: non-volatile
        // automatics modified between setjmp and longjmp are INDETERMINATE after
        // the jump. gcc register-allocates them (longjmp rolls the register back
        // to its at-setjmp garbage), so the handler restored
        // threadObjectStackOffset from trash and later callee frames were
        // allocated on top of this frame's locals. clang happened to spill.
        b.append("    volatile int restoreTo");
        b.append(LabelInstruction.labelName(start));
        b.append(cid);
        b.append(counter);
        b.append(";\n    volatile int tryBlockOffset");
        b.append(LabelInstruction.labelName(start));
        b.append(cid);
        b.append(counter);
        b.append(";\n    DEFINE_CATCH_BLOCK(catch_");
        b.append(LabelInstruction.labelName(start));
        b.append(cid);
        b.append(counter);
        b.append(", label_");
        b.append(LabelInstruction.labelName(handler));
        b.append(", restoreTo");
        b.append(LabelInstruction.labelName(start));
        b.append(cid);
        b.append(counter);
        b.append(");\n");
        LabelInstruction.addTryEndLabel(end);
        // The label's END_TRY needs an explicit depth: blindly decrementing tryBlockLevel
        // is insufficient where a catch handler points to a position *inside* the
        // try/catch block, which happens with a synchronized() block around an exception
        // point. This used to COMPUTE that depth here, which cached a wrong answer for
        // every nested try -- see setCatchDepthInstructions. Hand over the list instead
        // and let the label ask when every try/catch in the method has registered.
        LabelInstruction.setCatchDepthInstructions(instructions);
        if(legacyCatchDepth) {
            LabelInstruction.getLabelCatchDepth(end, instructions);
        }
        counter++;
//        b.append("/* try/catch start: ");
//        b.append(start);
//        b.append(", end: ");
//        b.append(end);
//        b.append(", handler: ");
//        b.append(handler);
//        b.append(", type: ");
//        b.append(type);
//        b.append(" */\n\n");
    }

}
