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
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class Field extends Instruction implements AssignableExpression {
    private String owner;
    private String name;
    private String desc;
    private boolean useThis;
    
    public Field(int opcode, String owner, String name, String desc) {
        super(opcode);
        this.owner = owner;
        this.name = name.replace('$', '_');
        this.desc = desc;
    }

    public boolean isObject() {
        char c = desc.charAt(0);
        return c == '[' || c == 'L';
    }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        String t = owner.replace('.', '_').replace('/', '_').replace('$', '_');
        t = unarray(t);
        if(t != null && !dependencyList.contains(t)) {
            dependencyList.add(t);
        }
    }
    
    public String getFieldFromThis() {
        return "get_field_" + owner.replace('/', '_').replace('$', '_') + 
                "_" + name + "(__cn1ThisObject)";
        
    }

    public String setFieldFromThis(int arg) {
        // special case for this
        if(arg == 0) {
            return "    set_field_" + owner.replace('/', '_').replace('$', '_') + 
                    "_" + name + "(threadStateData, __cn1ThisObject, __cn1ThisObject);\n";
        }
        if(isObject()) {
            return "    set_field_" + owner.replace('/', '_').replace('$', '_') + 
                    "_" + name + "(threadStateData, __cn1Arg" + arg + ", __cn1ThisObject);\n";
        }
        return "    set_field_" + owner.replace('/', '_').replace('$', '_') + 
                "_" + name + "(threadStateData, __cn1Arg" + arg + ", __cn1ThisObject);\n";        
    }

    
    
    public String pushFieldFromThis() {
        StringBuilder b = new StringBuilder("    ");
        switch(desc.charAt(0)) {
            case 'L':
            case '[':
                b.append("PUSH_POINTER");
                break;
            case 'D':
                b.append("PUSH_DOUBLE");
                break;
            case 'F':
                b.append("PUSH_FLOAT");
                break;
            case 'J':
                b.append("PUSH_LONG");
                break;
            default:
                b.append("PUSH_INT");
                break;
        }
        b.append("(get_field_");
        b.append(owner.replace('/', '_').replace('$', '_'));
        b.append("_");
        b.append(name);
        b.append("(__cn1ThisObject));\n");
        return b.toString();
        
    }
    
    @Override
    public boolean assignTo(String varName, StringBuilder sb) {
        if (opcode == Opcodes.GETSTATIC || (opcode == Opcodes.GETFIELD && useThis)) {
            StringBuilder b = new StringBuilder();
            /*if (typeVarName != null) {
                b.append(typeVarName).append(" = ");
                switch(desc.charAt(0)) {
                    case 'L':
                    case '[':
                        b.append("CN1_TYPE_OBJECT");
                        break;
                    case 'D':
                        b.append("CN1_TYPE_DOUBLE");
                        break;
                    case 'F':
                        b.append("CN1_TYPE_FLOAT");
                        break;
                    case 'J':
                        b.append("CN1_TYPE_LONG");
                        break;
                    default:
                        b.append("CN1_TYPE_INT");
                        break;
                }
                b.append("; ");
            }*/
            if (varName != null) {
                b.append(varName).append(" = ");
            }
            if (opcode == Opcodes.GETSTATIC) {
                b.append("get_static_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name.replace('/', '_').replace('$', '_'));
                b.append("(threadStateData)");
            } else {
                // useThis
                b.append("get_field_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name);
                b.append("(__cn1ThisObject)");
            }
            if (varName != null) {
                b.append(";\n");
            }
            sb.append(b);
            return true;
        }
        
            
            
        
        return false;
    }
    
    @Override
    public void appendInstruction(StringBuilder b) {
        b.append("    ");
        switch(opcode) {
            case Opcodes.GETSTATIC:
                switch(desc.charAt(0)) {
                    case 'L':
                    case '[':
                        b.append("PUSH_POINTER");
                        break;
                    case 'D':
                        b.append("PUSH_DOUBLE");
                        break;
                    case 'F':
                        b.append("PUSH_FLOAT");
                        break;
                    case 'J':
                        b.append("PUSH_LONG");
                        break;
                    default:
                        b.append("PUSH_INT");
                        break;
                }
                b.append("(get_static_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name.replace('/', '_').replace('$', '_'));
                b.append("(threadStateData));\n");
                break;
            case Opcodes.PUTSTATIC:
                //b.append("SAFE_RETAIN(1);\n    ");
                b.append("set_static_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name.replace('/', '_').replace('$', '_'));
                b.append("(threadStateData, ");
                switch(desc.charAt(0)) {
                    case 'L':
                    case '[':
                        b.append("PEEK_OBJ(1));\n    SP--;\n");
                        return;
                    case 'D':
                        b.append("POP_DOUBLE");
                        break;
                    case 'F':
                        b.append("POP_FLOAT");
                        break;
                    case 'J':
                        b.append("POP_LONG");
                        break;
                    default:
                        b.append("POP_INT");
                        break;
                }
                b.append("());\n");
                break;
            case Opcodes.GETFIELD:
                switch(desc.charAt(0)) {
                    case 'L':
                    case '[':
                        b.append("PUSH_POINTER");
                        break;
                    case 'D':
                        b.append("PUSH_DOUBLE");
                        break;
                    case 'F':
                        b.append("PUSH_FLOAT");
                        break;
                    case 'J':
                        b.append("PUSH_LONG");
                        break;
                    default:
                        b.append("PUSH_INT");
                        break;
                }
                b.append("(get_field_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name);
                if(useThis) {
                    b.append("(__cn1ThisObject));\n");
                } else {
                    b.append("(POP_OBJ()));\n");
                }
                break;
            case Opcodes.PUTFIELD:
                //b.append("SAFE_RETAIN(1);\n    ");
                b.append("set_field_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name);
                b.append("(threadStateData, ");
                switch(desc.charAt(0)) {
                    case 'L':
                    case '[':
                        b.append("PEEK_OBJ");
                        if(useThis) {
                            b.append("(1), __cn1ThisObject);\n    SP--;\n");
                        } else {
                            b.append("(1), PEEK_OBJ(2));\n    POP_MANY(2);\n");
                        }
                        return;
                    case 'D':
                        b.append("POP_DOUBLE");
                        break;
                    case 'F':
                        b.append("POP_FLOAT");
                        break;
                    case 'J':
                        b.append("POP_LONG");
                        break;
                    default:
                        b.append("POP_INT");
                        break;
                }
                if(useThis) {
                    b.append("(), __cn1ThisObject);\n");
                } else {
                    b.append("(), POP_OBJ());\n");
                }
                break;
        }
    }

    /**
     * @return the useThis
     */
    public boolean isUseThis() {
        return useThis;
    }

    /**
     * @param useThis the useThis to set
     */
    public void setUseThis(boolean useThis) {
        this.useThis = useThis;
    }

    

}
