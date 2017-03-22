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
    //private boolean useThis;
    
    private Instruction targetOp;
    private Instruction valueOp;
    
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
        if (targetOp != null) {
            targetOp.addDependencies(dependencyList);
        }
        if (valueOp != null) {
            valueOp.addDependencies(dependencyList);
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
        if (opcode == Opcodes.GETSTATIC || (opcode == Opcodes.GETFIELD)) {
            StringBuilder b = new StringBuilder();
            
                
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
                
                b.append("get_field_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name);
                StringBuilder sb3 = new StringBuilder();
                boolean targetProvided = (targetOp != null &&
                    targetOp instanceof AssignableExpression && 
                    ((AssignableExpression)targetOp).assignTo(null, sb3));
                if (targetProvided) {
                    b.append("(").append(sb3.toString().trim()).append(")");
                //} else if (useThis) {
                //    b.append("(__cn1ThisObject)");
                } else {
                    return false;
                }
                
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
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        StringBuilder sb = new StringBuilder();
        appendInstruction(sb); //To change body of generated methods, choose Tools | Templates.
        if (valueOp != null && !valueOpAppended) {
            valueOp.appendInstruction(b, l);
            valueOpAppended = true;
        }
        if (targetOp != null && !targetOpAppended) {
            targetOp.appendInstruction(b, l);
            targetOpAppended = true;
        }
        b.append(sb);
    }
    
    private boolean valueOpAppended;
    private boolean targetOpAppended;
    
    @Override
    public void appendInstruction(StringBuilder sbOut) {
        valueOpAppended = false;
        targetOpAppended = false;
        StringBuilder b = new StringBuilder();
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
            case Opcodes.PUTSTATIC: {
                //b.append("SAFE_RETAIN(1);\n    ");
                b.append("set_static_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name.replace('/', '_').replace('$', '_'));
                b.append("(threadStateData, ");
                StringBuilder sb2 = new StringBuilder();
                StringBuilder sb3 = new StringBuilder();
                if (valueOp != null && valueOp instanceof AssignableExpression && ((AssignableExpression)valueOp).assignTo(null, sb2)) {
                    b.append(sb2.toString().trim()).append(");\n");
                    valueOpAppended = true;
                } else if (valueOp != null && valueOp instanceof CustomInvoke && ((CustomInvoke)valueOp).appendExpression(sb3)) {
                    b.append(sb3.toString().trim()).append(");\n");
                    valueOpAppended = true;
                } else {
                    
                    switch(desc.charAt(0)) {
                        case 'L':
                        case '[':
                            b.append("PEEK_OBJ(1));\n    SP--;\n");
                            sbOut.append(b);
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
                }
                
                break;
            }
            case Opcodes.GETFIELD: {
                StringBuilder sb3 = new StringBuilder();
                boolean targetProvided = (targetOp != null &&
                        targetOp instanceof AssignableExpression && 
                        ((AssignableExpression)targetOp).assignTo(null, sb3));
                
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
                
                if (targetProvided) {
                    b.append("(").append(sb3.toString().trim()).append("));\n");
                    targetOpAppended = true;
                //} else if(useThis) {
                //    b.append("(__cn1ThisObject));\n");
                } else {
                    b.append("(POP_OBJ()));\n");
                }
                break;
            }
            case Opcodes.PUTFIELD: {
                //b.append("SAFE_RETAIN(1);\n    ");
                b.append("set_field_");
                b.append(owner.replace('/', '_').replace('$', '_'));
                b.append("_");
                b.append(name);
                b.append("(threadStateData, ");
                
                
                
                StringBuilder sb2 = new StringBuilder();
                StringBuilder sb3 = new StringBuilder();
                
                String targetLiteral = null;
                String valueLiteral = null;
                if (valueOp != null && valueOp instanceof AssignableExpression && ((AssignableExpression)valueOp).assignTo(null, sb2)) {
                    valueLiteral = sb2.toString().trim();
                } else if (valueOp != null && valueOp instanceof CustomInvoke && ((CustomInvoke)valueOp).appendExpression(sb3)) {
                    valueLiteral = sb3.toString().trim();
                }
                sb3.setLength(0);
                if (targetOp != null && targetOp instanceof AssignableExpression && ((AssignableExpression)targetOp).assignTo(null, sb3)) {
                    targetLiteral = sb3.toString().trim();
                }
                
                if (valueLiteral != null && targetLiteral != null) {
                    b.append(valueLiteral).append(", ").append(targetLiteral).append(");\n");
                    valueOpAppended = true;
                    targetOpAppended = true;
                } else {
                    switch(desc.charAt(0)) {
                        case 'L':
                        case '[':
                            b.append("PEEK_OBJ");
                            //if(useThis) {
                            //    b.append("(1), __cn1ThisObject);\n    SP--;\n");
                            //} else {
                                b.append("(1), PEEK_OBJ(2));\n    POP_MANY(2);\n");
                            //}
                                sbOut.append(b);
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
                    //if(useThis) {
                    //    b.append("(), __cn1ThisObject);\n");
                    //} else {
                        b.append("(), POP_OBJ());\n");
                    //}
                }
                break;
            }
        }
        if (valueOp != null && !valueOpAppended) {
            valueOp.appendInstruction(sbOut);
            valueOpAppended = true;
        }
        if (targetOp != null && !targetOpAppended) {
            targetOp.appendInstruction(sbOut);
            targetOpAppended = true;
        }
        sbOut.append(b);
    }

    
    
    /**
     * @return the useThis
     */
    //public boolean isUseThis() {
    //    return useThis;
    //}

    /**
     * @param useThis the useThis to set
     */
    //public void setUseThis(boolean useThis) {
    //    this.useThis = useThis;
    //}
    
    public static int tryReduce(List<Instruction> instructions, int index) {
        Instruction instr = instructions.get(index);
        if (!(instr instanceof Field)) {
            return -1;
        }
        if (instr.getOpcode() == Opcodes.PUTFIELD) {
            if (index < 2) {
                return -1;
            }
            
            Field f = (Field)instr;
            if (f.targetOp != null || f.valueOp != null) {
                return -1;
            }
            Instruction targetInstr = instructions.get(index-2);
            if (!(targetInstr instanceof AssignableExpression)) {
                return -1;
            }
            Instruction valueInstr = instructions.get(index-1);
            if (!(valueInstr instanceof AssignableExpression)) {
                return -1;
            }
            
            StringBuilder sb = new StringBuilder();
            
            AssignableExpression targetExpr = (AssignableExpression) targetInstr;
            if (!targetExpr.assignTo(null, sb)) {
                return -1;
            }
            
            AssignableExpression valueExpr = (AssignableExpression) valueInstr;
            if (!valueExpr.assignTo(null, sb)) {
                return -1;
            }
            
            f.targetOp = targetInstr;
            f.valueOp = valueInstr;
            
            instructions.remove(index-2);
            instructions.remove(index-2);
            return index-2;
        } else if (instr.getOpcode() == Opcodes.PUTSTATIC) {
            if (index < 1) {
                return -1;
            }
            
            Field f = (Field)instr;
            if (f.valueOp != null) {
                return -1;
            }
        
            Instruction valueInstr = instructions.get(index-1);
            if (!(valueInstr instanceof AssignableExpression)) {
                return -1;
            }
            
            StringBuilder sb = new StringBuilder();
            
           
            
            AssignableExpression valueExpr = (AssignableExpression) valueInstr;
            if (!valueExpr.assignTo(null, sb)) {
                return -1;
            }
            
            
            f.valueOp = valueInstr;
            
            instructions.remove(index-1);
            
            return index-1;
        } else if (instr.getOpcode() == Opcodes.GETFIELD) {
            if (index < 1) {
                return -1;
            }
            
            Field f = (Field)instr;
            //if (f.useThis) {
            //    return -1;
            //}
            
            Instruction targetInstr = instructions.get(index-1);
            if (!(targetInstr instanceof AssignableExpression)) {
                return -1;
            }
            
            StringBuilder sb = new StringBuilder();
            
           
            
            AssignableExpression targetExpr = (AssignableExpression) targetInstr;
            if (!targetExpr.assignTo(null, sb)) {
                return -1;
            }
            
            
            f.targetOp = targetInstr;
            
            instructions.remove(index-1);
            
            return index-1;
        }
        return -1;
    }

    

}
