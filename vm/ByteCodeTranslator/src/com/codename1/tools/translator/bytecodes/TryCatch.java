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
    private static boolean firstException;
    private static boolean hasTryCatch;
    private static int counter;
    
    public static void reset() {
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
    
    @Override
    public void appendInstruction(StringBuilder b) {
        hasTryCatch = true;
        if(firstException) {
            // we need to append basic exception handling logic
            b.append("    DEFINE_EXCEPTION_HANDLING_CONSTANTS();\n");
            firstException = false;
        }
        
        String cid = "0";
        if(type != null) {
            cid = "cn1_class_id_" + type.replace('/', '_').replace('$', '_');
        } 
        LabelInstruction.addTryBeginLabel(start, cid, counter);
        b.append("    int restoreTo");
        b.append(start);
        b.append(cid);
        b.append(counter);
        b.append(";\n    DEFINE_CATCH_BLOCK(catch_");
        b.append(start);
        b.append(cid);
        b.append(counter);
        b.append(", label_");
        b.append(handler);
        b.append(", restoreTo");
        b.append(start);
        b.append(cid);
        b.append(counter);
        b.append(");\n");
        LabelInstruction.addTryEndLabel(end);
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
