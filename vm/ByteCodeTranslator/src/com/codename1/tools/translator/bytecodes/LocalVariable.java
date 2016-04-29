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

import java.util.Objects;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class LocalVariable extends Instruction {
    private String name;
    private String desc;
    private int index;
    public LocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super(Opcodes.ALOAD);
        this.name = name;
        this.desc = desc;
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    public boolean isRightVariable(int index, char type) {
        if(index == this.index) {
            if(type == 'L') {
                return desc.startsWith("L") || desc.startsWith("[");
            }
            return type == desc.charAt(0);
        }
        return false;
    }

    public void appendInstruction(StringBuilder b) {
        b.append("    ");
        if(desc.startsWith("[") || desc.startsWith("L")) {
            b.append("JAVA_OBJECT o");
        } else {
            switch(desc.charAt(0)) {
                case 'I':
                    b.append("JAVA_INT i");
                    break;
                case 'J':
                    b.append("JAVA_LONG j");
                    break;
                case 'B':
                    b.append("JAVA_BYTE b");
                    break;
                case 'S':
                    b.append("JAVA_SHORT s");
                    break;
                case 'F':
                    b.append("JAVA_FLOAT f");
                    break;
                case 'D':
                    b.append("JAVA_DOUBLE d");
                    break;
                case 'Z':
                    b.append("JAVA_BOOLEAN z");
                    break;
                case 'C':
                    b.append("JAVA_CHAR c");
                    break;
            }
        }
        if(name.equals("this")) {
            b.append("this = __cn1ThisObject;\n");
        } else {
            b.append(name);
            b.append("_");
            b.append(index);
            b.append(";\n");
        }
    }
    
    public char getQualifier() {
        switch (desc.charAt(0)) {
            case 'B' :
            case 'C' :
            case 'Z' :
            case 'I' : 
            case 'S' :
                return 'i';
            case 'J' :
                return 'l';
            case 'F' :
                return 'f';
            case 'D' : 
                return 'd';
            case 'L' :
            case '[' :
                return 'o';
            default :
                throw new RuntimeException("Unknown local variable type "+desc);
               
        }
        
    }
    
    public String getOrigName() {
        return name;
    }
    
    public String getVarName() {
        if(name.equals("this")) {
            return "__cn1ThisObject";
        }
        StringBuilder b = new StringBuilder();
        if(desc.startsWith("[") || desc.startsWith("L")) {
            b.append("0");
        } else {
            switch(desc.charAt(0)) {
                case 'I':
                    b.append("i");
                    break;
                case 'J':
                    b.append("j");
                    break;
                case 'B':
                    b.append("b");
                    break;
                case 'S':
                    b.append("s");
                    break;
                case 'F':
                    b.append("f");
                    break;
                case 'D':
                    b.append("d");
                    break;
                case 'Z':
                    b.append("z");
                    break;
                case 'C':
                    b.append("c");
                    break;
            }
        }
        b.append(name);
        b.append("_");
        b.append(index);
        return b.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() == LocalVariable.class) {
            LocalVariable lv = (LocalVariable)o;
            return lv.getIndex() == this.getIndex() && lv.getQualifier() == this.getQualifier();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.desc);
        hash = 19 * hash + this.index;
        return hash;
    }
}
