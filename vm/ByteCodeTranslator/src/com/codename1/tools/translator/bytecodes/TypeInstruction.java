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
public class TypeInstruction extends Instruction {
    private String type;
    private String actualType;
    public TypeInstruction(int opcode, String type) {
        super(opcode);
        this.type = type;
    }

    @Override
    public void addDependencies(List<String> dependencyList) {
        String t = type.replace('.', '_').replace('/', '_').replace('$', '_');
        t = unarray(t);
        actualType = t;
        if(t != null && !dependencyList.contains(t)) {
            dependencyList.add(t);
        }
        if(actualType == null) {
            // primitive array
            switch(type.charAt(type.length() - 1)) {
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
        if(opcode == Opcodes.ANEWARRAY) {
            ByteCodeClass.addArrayType(actualType, 1);
        }
    }
    
    
    @Override
    public void appendInstruction(StringBuilder b) {
        type = type.replace('.', '_').replace('/', '_').replace('$', '_');
        b.append("    ");
        switch(opcode) {
            case Opcodes.NEW:
                b.append("PUSH_POINTER(__NEW_");
                b.append(type);
                b.append("(threadStateData)); /* NEW */\n");
                break;
            case Opcodes.ANEWARRAY:
                b.append("stackPointer--;\n    PUSH_POINTER(__NEW_ARRAY_");
                b.append(actualType);
                b.append("(threadStateData, stack[stackPointer].data.i)); /* ANEWARRAY */\n");
                break;
            case Opcodes.CHECKCAST:
                b.append("BC_CHECKCAST(");
                b.append(type);
                b.append(");\n");
                break;
            case Opcodes.INSTANCEOF:
                int pos = type.indexOf('[');
                if(pos > -1) {
                    int count = 1;
                    while(type.charAt(pos + 1) == '[') {
                        count++;
                        pos++;
                    }
                    b.append("BC_INSTANCEOF(cn1_array_");
                    b.append(count);
                    b.append("_id_");
                    b.append(actualType);
                } else {
                    b.append("BC_INSTANCEOF(cn1_class_id_");
                    b.append(actualType);
                }
                b.append(");\n");
                break;
        }
    }
}
