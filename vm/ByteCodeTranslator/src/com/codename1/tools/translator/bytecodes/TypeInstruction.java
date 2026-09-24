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
import com.codename1.tools.translator.ByteCodeTranslator;
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

    /// Index of the frame-exit retire guard this NEW writes into, or -1.
    ///
    /// The guard exists so the retire scope points at a variable that can ONLY ever
    /// hold the object from this site. Pointing it at the LOCAL instead is unsound in a
    /// way no gate reliably shows: the escape analysis proves the OBJECT ALLOCATED HERE
    /// never leaves the frame, while the local it was stored into may later be
    /// overwritten by a parameter or a field read -- and the scope would then retire an
    /// object nothing proved dead.
    private int deadGuardId = -1;

    public void setDeadGuardId(int id) {
        this.deadGuardId = id;
    }

    public int getDeadGuardId() {
        return deadGuardId;
    }
    private int scalarStructId = -1;
    private boolean initBeforePublish = false;
    private String originalType;

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

    private boolean fusedNew = false;
    private boolean implicitStackAlloc = false;

    /**
     * Marks this {@code NEW} of a {@code @Fused} class as DEFERRED: like
     * init-before-publish, the NEW pushes only a null placeholder; the matching
     * {@code <init>} site allocates owner + fused children as one block (or
     * falls back to an ordinary allocation), writes it into both placeholder
     * slots, and calls the constructor. See FusedConstructor.
     */
    public void markFusedNew() {
        this.fusedNew = true;
    }

    public boolean isFusedNew() {
        return fusedNew;
    }
    public TypeInstruction(int opcode, String type) {
        super(opcode);
        this.type = type;
        // appendInstruction mangles `type` in place (dots/slashes/dollars become
        // underscores), so the readable name has to be kept aside here if anything
        // downstream wants to print it -- BC_CHECKCAST_CHECKED's message does.
        this.originalType = type;
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
        if(implicitStackAlloc) {
            // per-SITE stack allocation proven by escape analysis
            // (BytecodeMethod.stackAllocStringBuilders) -- no class annotation
            return mangled;
        }
        ByteCodeClass bc = Parser.getClassObject(mangled);
        if(bc != null && bc.isStackAllocatable()) {
            return mangled;
        }
        return null;
    }

    /**
     * Marks this NEW site for stack allocation WITHOUT the class-level
     * {@code @StackAllocate} annotation: the caller (an escape-analysis pass)
     * has proven this particular allocation never outlives the frame.
     */
    public void markImplicitStackAlloc() {
        this.implicitStackAlloc = true;
    }

    private int stackBuilderBytes;
    public void setStackBuilderBytes(int bytes) { stackBuilderBytes = bytes; }
    public int getStackBuilderBytes() { return stackBuilderBytes; }

    private int stackFusedLen = -1;
    private String stackFusedElemCType;
    private String stackFusedClassRef;
    private String stackFusedFieldCName;

    /**
     * For an implicit stack-alloc site whose ctor's @Fused plan has ONE
     * constant-length primitive-array child: the child buffer is placed in a
     * method-scoped stack blob (__cn1stkbuf_<id>) and installed before the ctor
     * runs, so the keep-if-null ctor field-init KEEPS it -- the builder then
     * allocates nothing on the heap at all.
     */
    public void setStackFusedChild(int len, String elemCType, String classRef, String fieldCName) {
        this.stackFusedLen = len;
        this.stackFusedElemCType = elemCType;
        this.stackFusedClassRef = classRef;
        this.stackFusedFieldCName = fieldCName;
    }

    public int getStackFusedLen() {
        return stackFusedLen;
    }

    public String getStackFusedElemCType() {
        return stackFusedElemCType;
    }

    public void setStackAllocId(int id) {
        this.stackAllocId = id;
    }

    public String getTypeName() {
        return type;
    }

    /**
     * The class this INSTANCEOF tests against, or null when this is not an INSTANCEOF
     * or when it tests an array type. Array ids sit outside the bitmap's row range and
     * keep the instanceofFunction path.
     *
     * @return the mangled class name, or null
     */
    public String instanceofTargetClass() {
        if(getOpcode() != Opcodes.INSTANCEOF) {
            return null;
        }
        if(type.indexOf('[') > -1) {
            return null;
        }
        return actualType;
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
                    // NOTE: this guard necessarily tests class__X.initialized rather
                    // than __X_LOADED__, because X is a DIFFERENT class from the one
                    // being emitted and __X_LOADED__ is file-local to X's own
                    // translation unit. initialized is set before __CLINIT__ runs, so
                    // this can still enter the allocation while X's <clinit> is in
                    // flight -- pre-existing, and the reason the guards emitted from
                    // ByteCodeClass (same translation unit) use the completion flag
                    // instead. Closing it here needs a globally visible completion
                    // flag on struct clazz, which is a larger change than this.
                    //
                    // ACQUIRE: this guard SKIPS the initialiser when the flag is
                    // set, so it never takes the class monitor and cannot rely on
                    // the monitor's release. Pairs with the __ATOMIC_RELEASE store
                    // in ByteCodeClass. A plain load here let a thread see the flag
                    // set while the vtable / classToInterfaceMap rows it describes
                    // were still invisible.
                    if(stackBuilderBytes > 0) {
                        b.append("cn1StackBufferReset(&__cn1sbscope_").append(stackAllocId).append("); ");
                    }
                    b.append("if(__builtin_expect(!__atomic_load_n(&class__");
                    b.append(type);
                    b.append(".initialized, __ATOMIC_ACQUIRE), 0)) __STATIC_INITIALIZER_");
                    b.append(type);
                    b.append("(threadStateData); memset(&__cn1stk_");
                    b.append(stackAllocId);
                    b.append(", 0, sizeof(struct obj__");
                    b.append(type);
                    b.append(")); CN1_OBJ_SET_CLASS(&__cn1stk_");
                    b.append(stackAllocId);
                    b.append(", &class__");
                    b.append(type);
                    b.append("); CN1_OBJ_SET_MARK(&__cn1stk_");
                    b.append(stackAllocId);
                    b.append(", CN1_GC_MARK_FRESH); CN1_OBJ_SET_HEAPPOS(&__cn1stk_");
                    b.append(stackAllocId);
                    b.append(", -1); ");
                    if(stackBuilderBytes > 0) {
                        b.append("CN1_OBJ_SET_HEAPPOS(&__cn1stk_").append(stackAllocId).append(", CN1_GC_STACK_BUILDER); ");
                        b.append("*(struct CN1StackBuffer**)__cn1stk_").append(stackAllocId)
                                .append(".__cn1InlineStorage = &__cn1sbscope_").append(stackAllocId).append("; ");
                        b.append("__cn1stk_").append(stackAllocId)
                                .append(".java_lang_StringBuilder_cn1Storage = (JAVA_LONG)(uintptr_t)__cn1sbdata_")
                                .append(stackAllocId).append("; ");
                    }
                    if(stackFusedLen >= 0) {
                        // stack-resident fused child: install a normal array header,
                        // point the owner's field at it BEFORE the ctor (keep-if-null
                        // keeps it). The DATA region is deliberately NOT zeroed:
                        // every reader of a builder buffer is bounded by count
                        // (charAt/getChars/toString; setLength zero-fills expansion
                        // explicitly), so the uninitialized tail is unreachable.
                        b.append("__cn1stk_");
                        b.append(stackAllocId);
                        b.append(".");
                        b.append(stackFusedFieldCName);
                        b.append(" = cn1FusedInstallPrimArray((JAVA_OBJECT)__cn1stkbuf_");
                        b.append(stackAllocId);
                        b.append(", 0, ");
                        b.append(stackFusedClassRef);
                        b.append(", sizeof(");
                        b.append(stackFusedElemCType);
                        b.append("), ");
                        b.append(stackFusedLen);
                        b.append("); ");
                    }
                    b.append("PUSH_POINTER((JAVA_OBJECT)&__cn1stk_");
                    b.append(stackAllocId);
                    b.append("); /* NEW stack-allocated */\n");
                    break;
                }
                if(fusedNew) {
                    // FUSED construction: allocation deferred to the matching <init>
                    // site (owner+children single block); push a placeholder only.
                    b.append("PUSH_POINTER(JAVA_NULL); /* NEW deferred (fused) */\n");
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
                // An iterator the escape analysis cleared takes the caller's pending
                // stack buffer when one is on offer; CN1_ITER_NEW falls through to
                // CN1_FAST_NEW when there is none, so this is the same allocation
                // everywhere else.
                b.append("PUSH_POINTER(");
                if(deadGuardId >= 0) {
                    b.append("__cn1dead_").append(deadGuardId).append(" = ");
                }
                b.append(com.codename1.tools.translator.Parser.isStackIterator(type)
                         ? "CN1_ITER_NEW(" : "CN1_FAST_NEW(");
                b.append(type);
                b.append(")");
                b.append(deadGuardId >= 0 ? "); /* NEW, frame-exit retired */\n"
                                          : "); /* NEW */\n");
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
                    b.append("));\n    CN1_OBJ_SET_CLASS(SP[-1].data.o, &class_array");
                    b.append(dim);
                    b.append("__");
                    b.append(actualType);
                    b.append("); /* ANEWARRAY multi */\n");
                    break;
                }
                b.append("SP--;\n    PUSH_POINTER(__NEW_ARRAY_");
                b.append(actualType);
                b.append("(threadStateData, SP[0].data.i));\n");
                break;
            case Opcodes.CHECKCAST:
                if(!ByteCodeTranslator.isCheckedCastsEnabled()) {
                    // Legacy shape: the macro expands to nothing, so the argument is
                    // discarded and the raw type name is fine.
                    b.append("BC_CHECKCAST(");
                    b.append(type);
                    b.append(");\n");
                    break;
                }
                // Enforcing shape. The class id has to be resolved the same way
                // INSTANCEOF resolves it -- array dimensions collapse into one
                // cn1_array_<n>_id_ token -- because instanceofFunction compares ids.
                // The readable name is baked in as a literal here rather than looked
                // up at runtime: the translator already knows it, and that keeps the
                // failure path free of any class-name table.
                int castPos = type.indexOf('[');
                if(castPos > -1) {
                    int castCount = 1;
                    while(type.charAt(castPos + 1) == '[') {
                        castCount++;
                        castPos++;
                    }
                    b.append("BC_CHECKCAST_CHECKED(cn1_array_");
                    b.append(castCount);
                    b.append("_id_");
                    b.append(actualType);
                } else {
                    b.append("BC_CHECKCAST_CHECKED(cn1_class_id_");
                    b.append(actualType);
                }
                b.append(", \"");
                b.append(originalType.replace('/', '.'));
                b.append("\");\n");
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
                    // Constant-time form when this type has a bitmap bit, which is
                    // every non-array type an instanceof anywhere in the application
                    // tests against. The id is still passed so the array fallback
                    // inside the macro has something to call with.
                    // Leaf first: nothing live extends it, so the class word IS the
                    // answer and the classId load disappears with the bitmap lookup.
                    // String is a leaf but its class word is not unique -- fused
                    // Strings carry a twin clazz. See BC_INSTANCEOF_STRING.
                    if("java_lang_String".equals(actualType)) {
                        b.append("BC_INSTANCEOF_STRING();\n");
                        break;
                    }
                    if(Parser.isLeafClass(actualType)) {
                        b.append("BC_INSTANCEOF_LEAF(class__");
                        b.append(actualType);
                        b.append(");\n");
                        break;
                    }
                    int typeTestIdx = Parser.typeTestIndex(actualType);
                    if(typeTestIdx >= 0) {
                        b.append("BC_INSTANCEOF_FAST(");
                        b.append(typeTestIdx);
                        b.append(", cn1_class_id_");
                        b.append(actualType);
                        b.append(");\n");
                        break;
                    }
                    b.append("BC_INSTANCEOF(cn1_class_id_");
                    b.append(actualType);
                }
                b.append(");\n");
                break;
        }
    }
}
