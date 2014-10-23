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

/**
 *
 * @author Shai Almog
 */
public abstract class Instruction {
    static boolean hasInstructions;
    
    public static void setHasInstructions(boolean h) {
        hasInstructions = h;
    }   
    
    protected final int opcode;
    protected Instruction(int opcode) {
        this.opcode = opcode;
    }
    
    public void setMaxes(int maxStack, int maxLocals) {
    }
    
    public int getOpcode() {
        return opcode;
    }
    
    public void appendInstruction(StringBuilder b) {}

    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        appendInstruction(b);
    }

    public void addDependencies(List<String> dependencyList) {}
    
    public String unarray(String t) {
        if(t.startsWith("[")) {
            int pos = t.indexOf(';');
            if(pos < 0) {
                return null;
            }
            t = t.substring(t.indexOf('L') + 1, pos);
            return unarray(t);
        }
        return t;
    }
    
    public boolean isMethodUsed(String desc, String name) {
        return false;
    }

    public String getMethodUsed() {
        return null;
    }
    
    /**
     * Indicates whether this is a complex instruction that blocks simplification of this method
     */
    public boolean isComplexInstruction() {
        return false;
    }
}
