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
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Shai Almog
 */
public class TypeInstruction extends Instruction {
    private String type;
    private String actualType;
    private int stackAllocId = -1;
    private boolean scalarReplaced = false;
    private int scalarStructId = -1;
    private boolean initBeforePublish = false;

    /**
     * Marks this {@code NEW} as INIT-BEFORE-PUBLISH (memset elimination): the
     * allocation is DEFERRED to the matching inlined {@code <init>} site, so the
     * NEW itself only pushes a null placeholder (keeping the operand-stack depth
     * unchanged). The matching {@code <init>} allocates into a C temp, initializes
     * every field, and only then publishes the object into the surviving stack
     * slot. Set by {@code BytecodeMethod.markInitBeforePublish}.
     */
    public void markInitBeforePublish() {
        this.initBeforePublish = true;
    }

    public boolean isInitBeforePublish() {
        return initBeforePublish;
    }
    public TypeInstruction(int opcode, String type) {
        super(opcode);
        this.type = type;
    }

    /**
     * Marks this {@code NEW} of a primitive-only {@code @StackAllocate} class as
     * scalar-replaced: the object becomes a pure C local struct
     * {@code __cn1sr_<id>} whose address is never taken, so clang's SROA promotes
     * its fields to registers. The NEW itself then emits nothing (no header init,
     * no PUSH); the matching {@code <init>}, {@code DUP}, {@code ASTORE} and field
     * accesses are rewritten by {@code BytecodeMethod.scalarReplaceStackAllocations}.
     */
    public void markScalarReplaced(int id) {
        this.scalarReplaced = true;
        this.scalarStructId = id;
    }

    public boolean isScalarReplaced() {
        return scalarReplaced;
    }

    public int getScalarStructId() {
        return scalarStructId;
    }

    /**
     * If this is a {@code NEW} of a class annotated {@code @StackAllocate},
     * returns the mangled struct suffix (e.g. {@code com_bench_Point}); otherwise
     * null. Used by BytecodeMethod to declare one method-scoped struct per such
     * site and to route the NEW codegen to the stack path. Must be queried before
     * {@link #appendInstruction} mangles {@code type} in place.
     */
    public String getStackAllocType() {
        if(opcode != Opcodes.NEW) {
            return null;
        }
        String mangled = type.replace('.', '_').replace('/', '_').replace('$', '_');
        ByteCodeClass bc = Parser.getClassObject(mangled);
        if(bc != null && bc.isStackAllocatable()) {
            return mangled;
        }
        return null;
    }

    public void setStackAllocId(int id) {
        this.stackAllocId = id;
    }

    public String getTypeName() {
        return type;
    }

    public String getActualType() {
        return actualType;
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
            if(type.startsWith("[")) {
                int dim = 2;
                String tt = type.substring(1);
                while(tt.startsWith("[")) {
                    tt = tt.substring(1);
                    dim++;
                }
                ByteCodeClass.addArrayType(actualType, dim);
                return;
            }
            ByteCodeClass.addArrayType(actualType, 1);
        }
    }
    
    
    @Override
    public void appendInstruction(StringBuilder b, List<Instruction> l) {
        type = type.replace('.', '_').replace('/', '_').replace('$', '_');
        b.append("    ");
        switch(opcode) {
            case Opcodes.NEW:
                if(scalarReplaced) {
                    // Scalar-replaced @StackAllocate: the struct __cn1sr_<id> is a
                    // pure C local (declared at method top by BytecodeMethod). The
                    // object never escapes -- it is built directly into the struct
                    // by the inlined <init> and read via direct member access -- so
                    // the NEW emits nothing at all (no header, no PUSH).
                    b.append("/* NEW scalar-replaced (__cn1sr_").append(scalarStructId).append(") */\n");
                    break;
                }
                if(stackAllocId >= 0) {
                    // @StackAllocate: the object lives in the method-scoped struct
                    // __cn1stk_<id> (declared by BytecodeMethod). Replicate exactly
                    // what __NEW_<type> does -- run the static initializer, then set
                    // the same header fields codenameOneGcMalloc sets -- but skip the
                    // heap registration so the sweep never visits it. The GC still
                    // reaches it as a root (its pointer rides the operand stack) and
                    // scans its fields, so any heap objects it references stay live.
                    // It is never freed; it simply dies when the frame unwinds.
                    b.append("__STATIC_INITIALIZER_");
                    b.append(type);
                    b.append("(threadStateData); memset(&__cn1stk_");
                    b.append(stackAllocId);
                    b.append(", 0, sizeof(struct obj__");
                    b.append(type);
                    b.append(")); __cn1stk_");
                    b.append(stackAllocId);
                    b.append(".__codenameOneParentClsReference = &class__");
                    b.append(type);
                    b.append("; __cn1stk_");
                    b.append(stackAllocId);
                    b.append(".__codenameOneGcMark = -1; __cn1stk_");
                    b.append(stackAllocId);
                    b.append(".__heapPosition = -1; PUSH_POINTER((JAVA_OBJECT)&__cn1stk_");
                    b.append(stackAllocId);
                    b.append("); /* NEW stack-allocated */\n");
                    break;
                }
                if(initBeforePublish) {
                    // INIT-BEFORE-PUBLISH: allocation is deferred to the matching
                    // inlined <init> (which builds the object in a C temp and only
                    // then publishes it). Push a null placeholder so the operand
                    // stack depth / DUP shape is exactly as before; the <init> writes
                    // the real object into the surviving slot.
                    b.append("PUSH_POINTER(JAVA_NULL); /* NEW deferred (init-before-publish) */\n");
                    break;
                }
                // CN1_FAST_NEW inlines the BiBOP bump fast-path at the allocation
                // site (Lever 1, -DCN1_INLINE_ALLOC); with the flag off it expands
                // verbatim to __NEW_<type>(threadStateData).
                b.append("PUSH_POINTER(CN1_FAST_NEW(");
                b.append(type);
                b.append(")); /* NEW */\n");
                break;
            case Opcodes.ANEWARRAY:
                if(type.startsWith("[")) {
                    int dim = 2;
                    String t = type.substring(1);
                    while(t.startsWith("[")) {
                        t = t.substring(1);
                        dim++;
                    }
                    
                    b.append(" SP--;\n    PUSH_POINTER(allocArray(threadStateData, (*SP).data.i, &class_array");
                    b.append(dim);
                    b.append("__");
                    b.append(actualType);
                    b.append(", sizeof(JAVA_OBJECT), ");
                    b.append(dim);
                    b.append("));\n    SP[-1].data.o->__codenameOneParentClsReference = &class_array");
                    b.append(dim);
                    b.append("__");
                    b.append(actualType);
                    b.append("; /* ANEWARRAY multi */\n");
                    break;
                }
                b.append("SP--;\n    PUSH_POINTER(__NEW_ARRAY_");
                b.append(actualType);
                b.append("(threadStateData, SP[0].data.i));\n");
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
