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
import com.codename1.tools.translator.ByteCodeMethodArg;
import com.codename1.tools.translator.BytecodeMethod;
import com.codename1.tools.translator.Parser;
import com.codename1.tools.translator.Util;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class Invoke extends Instruction {
    private String owner;
    private String name;
    private String desc;
    private boolean itf;
    private char[] stackInputTypes;
    private char[] stackOutputTypes;
    
    
    public Invoke(int opcode, String owner, String name, String desc, boolean itf) {
        super(opcode);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
    }
    
    String getOwner() {
        return owner;
    }
    
    String getName() {
        return name;
    }
    
    String getDesc() {
        return desc;
    }
    
    boolean isItf() {
        return itf;
    }
    

    public boolean isMethodUsed(String desc, String name) {
        return this.desc.equals(desc) && name.equals(name);
    }

    public String getMethodUsed() {
        return desc + "." + name;
    }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        String t = owner.replace('.', '_').replace('/', '_').replace('$', '_');
        t = unarray(t);
        if(t != null && !dependencyList.contains(t)) {
            dependencyList.add(t);
        }

        StringBuilder bld = new StringBuilder();
        if(opcode != Opcodes.INVOKEINTERFACE && opcode != Opcodes.INVOKEVIRTUAL) {
            return;
        }         
        bld.append(owner.replace('/', '_').replace('$', '_'));
        bld.append("_");
        if(name.equals("<init>")) {
            bld.append("__INIT__");
        } else {
            if(name.equals("<clinit>")) {
                bld.append("__CLINIT__");
            } else {
                bld.append(name);
            }
        }
        bld.append("__");
        ArrayList<String> args = new ArrayList<String>();
        String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, bld, args);        
        String str = bld.toString();
        BytecodeMethod.addVirtualMethodsInvoked(str);
    }
    
    private String findActualOwner(ByteCodeClass bc) {
        if(bc == null) {
            return owner;
        }
        List<BytecodeMethod> mtds = bc.getMethods();
        if(mtds == null) {
            return owner;
        }
        for(BytecodeMethod mtd : mtds) {
            if(mtd.getMethodName().equals(name) && mtd.isStatic()) {
                return bc.getClsName();
            }
        }
        return findActualOwner(bc.getBaseClassObject());
    }
    
    @Override
    public void appendInstruction(StringBuilder b) {
        // special case for clone on an array which isn't a real method invocation
        if(name.equals("clone") && owner.indexOf('[') > -1) {
            b.append("    POP_MANY_AND_PUSH_OBJ(cloneArray(PEEK_OBJ(1)), 1);\n");
            return;
        }
        
        StringBuilder bld = new StringBuilder();
        if(opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL) {
            b.append("    ");
            bld.append("virtual_");
        } else {
            b.append("    ");
        }
        
        if(opcode == Opcodes.INVOKESTATIC) {
            // find the actual class of the static method to workaround javac not defining it correctly
            ByteCodeClass bc = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
            owner = findActualOwner(bc);
        }
        //if(owner.replace('/', '_').replace('$', '_').equals("java_lang_System_1") && name.equals("sleep")) {
        //    System.out.println("Break");
        //}
        bld.append(owner.replace('/', '_').replace('$', '_'));
        bld.append("_");
        if(name.equals("<init>")) {
            bld.append("__INIT__");
        } else {
            if(name.equals("<clinit>")) {
                bld.append("__CLINIT__");
            } else {
                bld.append(name);
            }
        }
        bld.append("__");
        ArrayList<String> args = new ArrayList<String>();
        String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, bld, args);
        boolean noPop = false;
        if(returnVal == null) {
            b.append(bld);
        } else {
            if(args.size() == 0 && opcode == Opcodes.INVOKESTATIC) {
                // special case for static method
                if(returnVal.equals("JAVA_OBJECT")) {
                    b.append("PUSH_OBJ");
                } else {
                    if(returnVal.equals("JAVA_INT")) {
                        b.append("PUSH_INT");
                    } else {
                        if(returnVal.equals("JAVA_LONG")) {
                            b.append("PUSH_LONG");
                        } else {
                            if(returnVal.equals("JAVA_DOUBLE")) {
                                b.append("PUSH_DOUBLE");
                            } else {
                                if(returnVal.equals("JAVA_FLOAT")) {
                                    b.append("PUSH_FLOAT");
                                } else {
                                    throw new UnsupportedOperationException("Unknown type: " + returnVal);
                                }
                            }
                        }
                    }
                }
                //b.append(returnVal);
                noPop = true;
                b.append("(");
            } else {
                //b.append("POP_MANY_AND_");
                //b.append(returnVal);
                b.append("{ ");
                b.append(returnVal);
                b.append(" tmpResult = ");
            }
            b.append(bld);
        }
        b.append("(threadStateData");
        
        
        
        if(opcode != Opcodes.INVOKESTATIC) {
            b.append(", SP[-");
            b.append(args.size() + 1);
            b.append("].data.o");
        }
        int offset = args.size();
        //int numArgs = offset;
        int argIndex=0;
        for(String a : args) {
            
            b.append(", ");
            
            b.append("SP[-");
            b.append(offset);
            b.append("].data.");
            b.append(a);
            offset--;
            
            argIndex++;
        }
        if(noPop) {
            b.append("));\n");
            return;
        }
        if(returnVal != null) {
            b.append(");\n");
            if(opcode != Opcodes.INVOKESTATIC) {
                if(args.size() > 0) {
                    b.append("    SP-=");
                    b.append(args.size());
                    b.append(";\n");
                }
            } else {
                if(args.size() > 1) {
                    b.append("    SP-=");
                    b.append(args.size() - 1);
                    b.append(";\n");
                }
            }
            if(returnVal.equals("JAVA_OBJECT")) {
                b.append("    SP[-1].data.o = tmpResult; SP[-1].type = CN1_TYPE_OBJECT; }\n");
            } else {
                if(returnVal.equals("JAVA_INT")) {
                    b.append("    SP[-1].data.i = tmpResult; SP[-1].type = CN1_TYPE_INT; }\n");
                } else {
                    if(returnVal.equals("JAVA_LONG")) {
                        b.append("    SP[-1].data.l = tmpResult; SP[-1].type = CN1_TYPE_LONG; }\n");
                    } else {
                        if(returnVal.equals("JAVA_DOUBLE")) {
                            b.append("    SP[-1].data.d = tmpResult; SP[-1].type = CN1_TYPE_DOUBLE; }\n");
                        } else {
                            if(returnVal.equals("JAVA_FLOAT")) {
                                b.append("    SP[-1].data.f = tmpResult; SP[-1].type = CN1_TYPE_FLOAT; }\n");
                            } else {
                                throw new UnsupportedOperationException("Unknown type: " + returnVal);
                            }
                        }
                    }
                }
            }
            
            /*if(opcode != Opcodes.INVOKESTATIC) {
                b.append(args.size() + 1);
            } else {
                b.append(args.size());
            }
            b.append(");\n");      */
            
            return;
        }
        b.append("); ");
        int val; 
        if(opcode != Opcodes.INVOKESTATIC) {
            val = args.size() + 1;
        } else {
            val = args.size();
        }
        if(val > 0) {
            /*b.append("popMany(threadStateData, ");            
            b.append(val);
            b.append(", stack, &stackPointer); \n"); */
            b.append("    SP-= ");
            b.append(val);
            b.append(";\n");
        } else {
            b.append("\n");            
        }
    }
    
    
    public List<ByteCodeMethodArg> getArgs() {
        return Util.getMethodArgs(desc);
    }

    @Override
    public char[] getStackInputTypes() {
        if (stackInputTypes == null) {
            List<ByteCodeMethodArg> args = getArgs();
            int thisArg = 0;
            if (opcode != Opcodes.INVOKESTATIC) {
                thisArg++;
                
            }
            stackInputTypes = new char[args.size() + thisArg];
            if (opcode != Opcodes.INVOKESTATIC) {
                stackInputTypes[0] = 'o';
            }
            int len = args.size();
            for (int i=0; i<len; i++) {
                stackInputTypes[i+thisArg] = args.get(i).getQualifier();
            }
        }
        return stackInputTypes;
    }

    @Override
    public char[] getStackOutputTypes() {
        if (stackOutputTypes == null) {
            String returnVal = BytecodeMethod.appendMethodSignatureSuffixFromDesc(desc, new StringBuilder(), new ArrayList<String>());
            if (returnVal == null) {
                stackOutputTypes = new char[0];
            } else {
                stackOutputTypes = new char[1];
                if(returnVal.equals("JAVA_OBJECT")) {
                    stackOutputTypes[0] = 'o';
                } else {
                    if(returnVal.equals("JAVA_INT")) {
                        stackOutputTypes[0] = 'i';
                    } else {
                        if(returnVal.equals("JAVA_LONG")) {
                            stackOutputTypes[0] = 'l';
                        } else {
                            if(returnVal.equals("JAVA_DOUBLE")) {
                                stackOutputTypes[0] = 'd';
                            } else {
                                if(returnVal.equals("JAVA_FLOAT")) {
                                    stackOutputTypes[0] = 'f';
                                } else {
                                    throw new UnsupportedOperationException("Unknown type: " + returnVal);
                                }
                            }
                        }
                    }
                }
            }
        }
        return stackOutputTypes;
    }
    
    
    
    
    
}
