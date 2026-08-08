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
    /**
     * The bounds of the declaring scope, as ASM labels, or null for a local
     * the translator synthesised from a store opcode rather than reading out
     * of the class file's LocalVariableTable.
     *
     * Retained so the on-device debugger can resolve them to source lines and
     * hide a local outside its scope. Two locals sharing a slot in disjoint
     * scopes are otherwise indistinguishable at a breakpoint, and the debugger
     * shows whichever one it happens to list — reporting the contents of a
     * variable the code has not reached yet.
     */
    private final Label scopeStart;
    private final Label scopeEnd;

    /**
     * Declaration order, so that two locals sharing a slot and a storage
     * qualifier still sort deterministically. Identity hash codes vary per
     * run, so without this the emitted side-table's row order depended on
     * where the JVM happened to place the scope labels.
     */
    private final int sequence;
    private static final java.util.concurrent.atomic.AtomicInteger SEQUENCE =
            new java.util.concurrent.atomic.AtomicInteger();

    public LocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super(Opcodes.ALOAD);
        this.name = name;
        this.desc = desc;
        this.index = index;
        this.scopeStart = start;
        this.scopeEnd = end;
        this.sequence = SEQUENCE.incrementAndGet();
    }

    /** Relative declaration order within a method; see {@link #sequence}. */
    public int getSequence() {
        return sequence;
    }

    public int getIndex() {
        return index;
    }

    /** The label where this local's scope opens, or null if unknown. */
    public Label getScopeStart() {
        return scopeStart;
    }

    /** The label where this local's scope closes, or null if unknown. */
    public Label getScopeEnd() {
        return scopeEnd;
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
    
    /**
     * Returns the JVM type-descriptor first character (I/J/F/D/Z/B/S/C for
     * primitives, 'L' for objects, '[' for arrays). Distinct from
     * {@link #getQualifier()}, which collapses byte/short/char/boolean/int
     * onto 'i'. Used by the on-device-debug side-table so the proxy can
     * present primitives at their declared narrow width.
     */
    public char getTypeCode() {
        return desc.charAt(0);
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

    public String getDesc() {
        return desc;
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
    
    /**
     * Identity is the storage a local occupies <em>and</em> the scope it
     * occupies it for.
     *
     * Slot and qualifier alone would make two variables declared in disjoint
     * blocks of the same method — {@code int} in one, {@code int} in the next —
     * the same local, and the set that holds these would keep only the first.
     * That was survivable while every local was reported as live for the whole
     * method, since the two shared one storage location anyway; once a local
     * is filtered to its own scope it stops being survivable, because in the
     * second block the debugger would have no row to show at all.
     *
     * Locals the translator synthesised from a store opcode carry no scope, so
     * they still collapse onto one another, which is what keeps a repeated
     * store from producing a row per occurrence.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() == LocalVariable.class) {
            LocalVariable lv = (LocalVariable)o;
            return lv.getIndex() == this.getIndex()
                    && lv.getQualifier() == this.getQualifier()
                    && lv.scopeStart == this.scopeStart
                    && lv.scopeEnd == this.scopeEnd;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + this.getQualifier();
        hash = 19 * hash + this.index;
        hash = 19 * hash + System.identityHashCode(this.scopeStart);
        hash = 19 * hash + System.identityHashCode(this.scopeEnd);
        return hash;
    }
}
