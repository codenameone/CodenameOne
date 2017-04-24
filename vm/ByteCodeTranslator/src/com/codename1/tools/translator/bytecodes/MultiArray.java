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

import com.codename1.tools.translator.ByteCodeClass;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class MultiArray extends Instruction {
    private String desc;
    private int dims;
    private String actualType;
    
    public MultiArray(String desc, int dims) {
        super(Opcodes.MULTIANEWARRAY);
        this.desc = desc;
        this.dims = dims;
    }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        String t = desc.replace('.', '_').replace('/', '_').replace('$', '_');
        t = unarray(t);
        actualType = t;
        if(t != null && !dependencyList.contains(t)) {
            dependencyList.add(t);
        }
        if(actualType == null) {
            // primitive array
            switch(desc.charAt(desc.length() - 1)) {
                case 'I':
                    actualType = "JAVA_INT";
                    break;
                case 'J':
                    actualType = "JAVA_LONG";
                    break;
                case 'B':
                    actualType = "JAVA_BYTE";
                    break;
                case 'S':
                    actualType = "JAVA_SHORT";
                    break;
                case 'F':
                    actualType = "JAVA_FLOAT";
                    break;
                case 'D':
                    actualType = "JAVA_DOUBLE";
                    break;
                case 'Z':
                    actualType = "JAVA_BOOLEAN";
                    break;
                case 'C':
                    actualType = "JAVA_CHAR";
                    break;
            }
        }
        ByteCodeClass.addArrayType(actualType, dims);
    }

    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        int actualDim = dims;
        /*int offset = 0;
        offset = desc.indexOf('[', offset);
        while(offset > -1) {
            actualDim++;
            offset = desc.indexOf('[', offset);
        }*/
        switch(actualDim) {
            case 2:
                b.append("    SP -= ").append(dims).append("; PUSH_OBJ(alloc2DArray(threadStateData, ");
                switch(dims) {
                    case 1:
                        b.append("(*SP).data.i, -1");
                        break;
                    case 2:
                        b.append("(*(SP+1)).data.i, (*SP).data.i");
                        break;
                }
                b.append(", &class_array2__");
                b.append(actualType);
                b.append(", &class_array1__");
                b.append(actualType);
                b.append(", sizeof(");
                if(actualType.startsWith("JAVA_")) {
                    b.append(actualType);
                } else {
                    b.append("JAVA_OBJECT");
                }
                b.append("))); /* MULTIANEWARRAY */\n");
                break;
                
            case 3:
                b.append("    PUSH_OBJ(alloc3DArray(threadStateData, POP_INT(), ");
                switch(dims) {
                    case 1:
                        b.append("-1, -1");
                        break;
                        
                    case 2:
                        b.append("POP_INT(), -1");
                        break;
                        
                    case 3:
                        b.append("POP_INT(), POP_INT()");
                        break;
                }
                b.append(", &class_array3__");
                b.append(actualType);
                b.append(", &class_array2__");
                b.append(actualType);
                b.append(", &class_array1__");
                b.append(actualType);
                b.append(", sizeof(");
                if(actualType.startsWith("JAVA_")) {
                    b.append(actualType);
                } else {
                    b.append("JAVA_OBJECT");
                }
                b.append("))); /* MULTIANEWARRAY */\n");
                break;
                
            case 4:
                b.append("    PUSH_OBJ(alloc4DArray(threadStateData, POP_INT(), ");
                switch(dims) {
                    case 1:
                        b.append("-1, -1, -1");
                        break;
                        
                    case 2:
                        b.append("POP_INT(), -1, -1");
                        break;
                        
                    case 3:
                        b.append("POP_INT(), POP_INT(), -1");
                        break;
                        
                    case 4:
                        b.append("POP_INT(), POP_INT(), POP_INT()");
                        break;
                }
                b.append(", &class_array4__");
                b.append(actualType);
                b.append(", &class_array3__");
                b.append(actualType);
                b.append(", &class_array2__");
                b.append(actualType);
                b.append(", &class_array1__");
                b.append(actualType);
                b.append(", sizeof(");
                if(actualType.startsWith("JAVA_")) {
                    b.append(actualType);
                } else {
                    b.append("JAVA_OBJECT");
                }
                b.append("))); /* MULTIANEWARRAY */\n");
                break;
        }
        
    }

}
