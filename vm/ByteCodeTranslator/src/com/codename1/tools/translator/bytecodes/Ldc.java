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
import com.codename1.tools.translator.Parser;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 *
 * @author Shai Almog
 */
public class Ldc extends Instruction {
    private Object cst;
    public Ldc(Object o) {
        super(Opcodes.LDC);
        cst = o;
    }

    public void addToConstantPool() {
        if (cst instanceof String) {
            Parser.addToConstantPool((String)cst);
        }
    }
    
    public Object getValue() {
        return cst;
    }
    
    @Override
    public void addDependencies(List<String> dependencyList) {
        if (cst instanceof Type) {
            int sort = ((Type) cst).getSort();
            Type tp = (Type) cst;
            if (sort == Type.OBJECT) {
                String t = tp.getInternalName().replace('/', '_').replace('$', '_');
                if(!dependencyList.contains(t)) {
                    dependencyList.add(t);
                }
            } else if (sort == Type.ARRAY) {
                try {
                    Type ttt = tp.getElementType();
                    switch(ttt.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.DOUBLE:
                        case Type.FLOAT:
                        case Type.INT:
                        case Type.LONG:
                        case Type.SHORT:
                            return;
                    }
                    String t = ttt.getInternalName().replace('/', '_').replace('$', '_');
                    ByteCodeClass.addArrayType(t, tp.getDimensions());
                    if(!dependencyList.contains(t)) {
                        dependencyList.add(t);
                    }
                } catch(Throwable t) {
                    System.out.println("Non-fatal error when reading type: " + tp);
                    t.printStackTrace();
                }
            } 
        }
    }

    @Override
    public void appendInstruction(StringBuilder b) {
        b.append("    ");
        if (cst instanceof Integer) {
            b.append("PUSH_INT(");
            b.append(((Number)cst).intValue());
            b.append("); /* LDC */\n");
        } else if (cst instanceof Float) {
            Float f = (Float)cst;
            b.append("PUSH_FLOAT(");
            if(f.isInfinite()) {
                if(f.floatValue() > 0) {
                    b.append("1.0f / 0.0f");
                } else {
                    b.append("-1.0f / 0.0f");
                }
            } else {
                if(f.isNaN()) {
                    b.append("0.0/0.0");
                } else {
                    b.append(f.floatValue());
                }
            }
            b.append("); /* LDC */\n");
        } else if (cst instanceof Long) {
            b.append("PUSH_LONG(");
            if(((Number)cst).longValue() == Long.MIN_VALUE) {
                // min value in C is 1 less (more really)
                b.append(((Number)cst).longValue() + 1);
            } else {
                b.append(((Number)cst).longValue());
            }
            b.append("LL); /* LDC */\n");
        } else if (cst instanceof Double) {
            Double d = (Double)cst;
            b.append("PUSH_DOUBLE(");
            if(d.isInfinite()) {
                if(d.floatValue() > 0) {
                    b.append("1.0 / 0.0");
                } else {
                    b.append("-1.0 / 0.0");
                }
            } else {
                if(d.isNaN()) {
                    b.append("0.0/0.0");
                } else {
                    b.append(d.doubleValue());
                }
            }
            b.append("); /* LDC */\n");
        } else if (cst instanceof String) {
            b.append("/* LDC: '");
            b.append(((String)cst).replace('\n', ' '));
            b.append("'*/\n    PUSH_POINTER(STRING_FROM_CONSTANT_POOL_OFFSET(");
            b.append(Parser.addToConstantPool((String)cst));
            b.append("));\n");
        } else if (cst instanceof Type) {
            // TODO...
            int sort = ((Type) cst).getSort();
            Type tp = (Type) cst;
            if (sort == Type.OBJECT) {
                b.append("/* LDC: '");
                b.append(tp.getInternalName().replace('/', '_').replace('$', '_'));
                b.append("'*/\n    PUSH_POINTER((JAVA_OBJECT)&class__");
                b.append(tp.getInternalName().replace('/', '_').replace('$', '_'));
                b.append(");\n");
            } else if (sort == Type.ARRAY) {
                b.append("/* LDC Array: '");
                b.append(tp.getInternalName().replace('/', '_').replace('$', '_'));
                b.append("'*/\n    PUSH_POINTER((JAVA_OBJECT)&class_array");
                b.append(tp.getDimensions());
                b.append("__");
                Type ttt = tp.getElementType();
                switch(ttt.getSort()) {
                    case Type.BOOLEAN:
                        b.append("JAVA_BOOLEAN");
                        break;
                    case Type.BYTE:
                        b.append("JAVA_BYTE");
                        break;
                    case Type.CHAR:
                        b.append("JAVA_CHAR");
                        break;
                    case Type.DOUBLE:
                        b.append("JAVA_DOUBLE");
                        break;
                    case Type.FLOAT:
                        b.append("JAVA_FLOAT");
                        break;
                    case Type.INT:
                        b.append("JAVA_INT");
                        break;
                    case Type.LONG:
                        b.append("JAVA_LONG");
                        break;
                    case Type.SHORT:
                        b.append("JAVA_SHORT");
                        break;
                    default:
                        b.append(ttt.getInternalName().replace('/', '_').replace('$', '_'));
                        break;
                }
                b.append(");\n");
            } else if (sort == Type.METHOD) {
                b.append("/* UNKNOWN CST type internal: ");
                b.append(tp.getInternalName());
                b.append(" */");
            } else {
                b.append("/* UNKNOWN CST type internal: ");
                b.append(tp.getInternalName());
                b.append(" */");
            }
        } else if (cst instanceof Handle) {
            //((Handle)cst).
            b.append("/* UNKNOWN CST type: ");
            b.append(cst.getClass().getName());
            b.append(" */");
        }
 
    }
}
