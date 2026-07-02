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
 * FUSED OBJECTS (general, annotation-driven -- see the runtime side in
 * cn1_globals.m/cn1AllocFused). A class annotated {@code @Fused} promises its
 * constructor-assigned primitive-array fields are ENCAPSULATED: the arrays are
 * created by the constructor ({@code this.f = new T[n]}) and never stored where
 * they can outlive the instance. For every {@code new X(...)} of such a class
 * the translator then:
 * <ol>
 *   <li>defers the allocation to the {@code <init>} site (the NEW pushes a null
 *       placeholder, exactly like init-before-publish);</li>
 *   <li>computes the child array sizes from the constructor arguments, allocates
 *       owner + all children as ONE BiBOP block (cn1AllocFused), installs the
 *       child array headers inside the block and pre-assigns the fields;</li>
 *   <li>calls the ORIGINAL constructor unchanged -- whose matched
 *       {@code this.f = new T[n]} stores were rewritten to KEEP-IF-NULL
 *       ({@link FusedFieldInit}): a pre-installed fused child is kept, while
 *       every other instantiation path (reflection, __NEW_INSTANCE, the
 *       oversize/BiBOP-unavailable fallback) arrives with null fields and
 *       allocates ordinary arrays. Constructors therefore chain unmodified and
 *       all paths stay semantically identical.</li>
 * </ol>
 * The fused child has a full ordinary object header (every reader sees a normal
 * array) but no independent GC identity: the page sweep walks slot boundaries
 * only, and the conservative resolver maps any pointer into the block -- child
 * header or interior element -- to the OWNER, so stack/argument references to
 * the child keep the whole block alive. The one thing the annotation must
 * guarantee is that the child reference is never stored into a heap object or
 * static that can outlive the owner.
 *
 * CONSTRUCTOR SHAPE (analyzeRaw): after the super() call, each fused field is
 * assigned exactly once by an unconditional {@code ALOAD 0; <len>; NEWARRAY T;
 * PUTFIELD f} where {@code <len>} is a constructor parameter or an int constant.
 * Bounds-guard idioms before the stores are tolerated (a conditional branch
 * whose one side throws); any other control flow, a this()/delegating ctor
 * shape that hides the stores, or a reassigned length parameter conservatively
 * disables fusion for that constructor (ordinary allocation everywhere -- a
 * false negative only forfeits the optimization). Object children and
 * finalizer chaining are future extensions; primitive arrays have no
 * finalizers, so owner-death needs no extra work.
 */
public final class FusedConstructor {
    /** Hard cap on fused children per constructor. */
    private static final int MAX_CHILDREN = 4;

    /** One fused child: {@code this.<fieldName> = new <type>[<len>]}. */
    public static final class Child {
        /** declaring (owner) class in C form. */
        final String cOwner;
        final String fieldName;
        /** NEWARRAY type operand: 4=boolean 5=char 6=float 7=double 8=byte 9=short 10=int 11=long */
        final int arrayType;
        /** 1-based constructor parameter holding the length, or -1. */
        final int paramIndex;
        /** length when paramIndex == -1. */
        final int constLength;

        Child(String cOwner, String fieldName, int arrayType, int paramIndex, int constLength) {
            this.cOwner = cOwner;
            this.fieldName = fieldName;
            this.arrayType = arrayType;
            this.paramIndex = paramIndex;
            this.constLength = constLength;
        }

        String elemCType() {
            switch (arrayType) {
                case 4: return "JAVA_ARRAY_BOOLEAN";
                case 5: return "JAVA_ARRAY_CHAR";
                case 6: return "JAVA_ARRAY_FLOAT";
                case 7: return "JAVA_ARRAY_DOUBLE";
                case 8: return "JAVA_ARRAY_BYTE";
                case 9: return "JAVA_ARRAY_SHORT";
                case 10: return "JAVA_ARRAY_INT";
                default: return "JAVA_ARRAY_LONG";
            }
        }

        String arrayClassRef() {
            switch (arrayType) {
                case 4: return "&class_array1__JAVA_BOOLEAN";
                case 5: return "&class_array1__JAVA_CHAR";
                case 6: return "&class_array1__JAVA_FLOAT";
                case 7: return "&class_array1__JAVA_DOUBLE";
                case 8: return "&class_array1__JAVA_BYTE";
                case 9: return "&class_array1__JAVA_SHORT";
                case 10: return "&class_array1__JAVA_INT";
                default: return "&class_array1__JAVA_LONG";
            }
        }

        /** C expression for the length INSIDE the constructor body (stable arg param). */
        String ctorLengthExpr() {
            return paramIndex > 0 ? ("__cn1Arg" + paramIndex) : Integer.toString(constLength);
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getCOwner() {
            return cOwner;
        }
    }

    private final List<Child> children;

    private FusedConstructor(List<Child> children) {
        this.children = children;
    }

    public List<Child> getChildren() {
        return children;
    }

    /**
     * Look up the fused plan for {@code owner.<init>desc}, or null. Only
     * non-null when the owner class carries @Fused AND the ctor matched the
     * fusable shape. {@code owner} is the JVM internal name.
     */
    public static FusedConstructor analyze(String owner, String desc) {
        ByteCodeClass cls = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
        if (cls == null || !cls.isFused()) {
            return null;
        }
        for (BytecodeMethod m : cls.getMethods()) {
            if (m.isConstructor() && desc.equals(m.getSignature())) {
                return m.getFusedConstructorPlan();
            }
        }
        return null;
    }

    /**
     * Analyze the RAW (pre-optimize) instruction list of a constructor. Called
     * once at parse time and cached on the {@link BytecodeMethod} (like the
     * inlinable-ctor plan); the class-level @Fused gate is applied by callers.
     */
    public static FusedConstructor analyzeRaw(BytecodeMethod ctor, String desc) {
        List<Instruction> raw = ctor.getInstructions();
        // Filtered view (no line numbers / local-var markers; LABELS ARE KEPT so
        // guard-branch targets can be resolved and verified).
        List<Instruction> body = new ArrayList<Instruction>();
        for (int i = 0; i < raw.size(); i++) {
            Instruction in = raw.get(i);
            if (in instanceof LineNumber || in instanceof LocalVariable) {
                continue;
            }
            body.add(in);
        }
        // ---- locate the super()/this() call: first INVOKESPECIAL <init> ----
        int start = -1;
        for (int i = 0; i < body.size() && i < 40; i++) {
            Instruction in = body.get(i);
            if (in instanceof Invoke && in.getOpcode() == Opcodes.INVOKESPECIAL
                    && "<init>".equals(((Invoke) in).getName())) {
                // a this(...) delegation hides the stores in the delegate -- bail
                if (((Invoke) in).getOwner().replace('/', '_').replace('$', '_')
                        .equals(ctor.getClsName())) {
                    return null;
                }
                start = i + 1;
                break;
            }
        }
        if (start < 0) {
            return null;
        }

        // slot -> 1-based parameter index (mirrors InlinableConstructor).
        List<ByteCodeMethodArg> args = Util.getMethodArgs(desc);
        int maxSlot = 1;
        for (ByteCodeMethodArg a : args) {
            maxSlot += a.isDoubleOrLong() ? 2 : 1;
        }
        int[] slotToParam = new int[maxSlot];
        int slot = 1;
        for (int p = 0; p < args.size(); p++) {
            slotToParam[slot] = p + 1;
            slot += args.get(p).isDoubleOrLong() ? 2 : 1;
        }

        List<Child> found = new ArrayList<Child>();
        boolean[] storedSlots = new boolean[maxSlot];
        int i = start;
        int scanned = 0;
        while (i < body.size() && scanned++ < 200) {
            Instruction in = body.get(i);
            int op = in.getOpcode();

            if (in instanceof LabelInstruction) {
                i++;
                continue;
            }

            // ---- fused triple: ALOAD 0; <len>; NEWARRAY T; PUTFIELD f ----
            if (in instanceof VarOp && op == Opcodes.ALOAD && ((VarOp) in).getIndex() == 0) {
                int i1 = nextReal(body, i + 1);
                int i2 = i1 < 0 ? -1 : nextReal(body, i1 + 1);
                int i3 = i2 < 0 ? -1 : nextReal(body, i2 + 1);
                if (i3 > 0) {
                    Instruction len = body.get(i1);
                    Instruction na = body.get(i2);
                    Instruction pf = body.get(i3);
                    if (na instanceof VarOp && na.getOpcode() == Opcodes.NEWARRAY
                            && pf instanceof Field && pf.getOpcode() == Opcodes.PUTFIELD) {
                        int paramIndex = -1;
                        int constLen = -1;
                        if (len instanceof VarOp && len.getOpcode() == Opcodes.ILOAD) {
                            int s = ((VarOp) len).getIndex();
                            if (s > 0 && s < slotToParam.length && slotToParam[s] != 0 && !storedSlots[s]) {
                                paramIndex = slotToParam[s];
                            }
                        } else {
                            constLen = constIntValue(len);
                        }
                        if (paramIndex > 0 || constLen >= 0) {
                            Field f = (Field) pf;
                            // field must be declared by this class and its descriptor
                            // must match the NEWARRAY element type
                            if (f.getOwner().replace('/', '_').replace('$', '_').equals(ctor.getClsName())
                                    && descMatchesArrayType(f.getDesc(), ((VarOp) na).getIndex())) {
                                if (found.size() >= MAX_CHILDREN) {
                                    return null;
                                }
                                found.add(new Child(ctor.getClsName(), f.getFieldName(),
                                        ((VarOp) na).getIndex(), paramIndex, constLen));
                                i = i3 + 1;
                                continue;
                            }
                        }
                    }
                }
            }

            // ---- region rules ----
            if (in instanceof VarOp && (op == Opcodes.ISTORE || op == Opcodes.IINC)) {
                int s = ((VarOp) in).getIndex();
                if (s >= 0 && s < storedSlots.length) {
                    storedSlots[s] = true; // a length param reassigned before its triple kills that triple
                }
            }
            if (in instanceof SwitchInstruction || in instanceof TryCatch) {
                break; // unmodeled flow: keep what we found so far, stop collecting
            }
            if (in instanceof Jump) {
                if (op == Opcodes.GOTO) {
                    break;
                }
                // Conditional branch. Only the two bounds-guard idioms keep the
                // remaining stores UNCONDITIONAL (required for keep-if-null
                // pre-installation); anything else stops collecting:
                //   (a) branch INTO a throw block: the resolved TARGET reaches
                //       ATHROW by straight fall-through -> the linear fall-through
                //       here is the unique non-throwing continuation;
                //   (b) branch OVER a throw block: the fall-through reaches ATHROW
                //       and the first label after it is the branch target -> resume
                //       scanning at that target.
                org.objectweb.asm.Label target = ((Jump) in).getLabel();
                int targetIdx = indexOfLabel(body, target);
                if (targetIdx >= 0 && findAthrowByFallthrough(body, targetIdx + 1, 12) >= 0) {
                    i++;               // idiom (a)
                    continue;
                }
                int athrowAhead = findAthrowByFallthrough(body, i + 1, 12);
                if (athrowAhead >= 0) { // idiom (b): resume at the target IF it follows the throw
                    int lbl = nextLabelIndex(body, athrowAhead + 1);
                    if (lbl >= 0 && body.get(lbl) instanceof LabelInstruction
                            && ((LabelInstruction) body.get(lbl)).getLabel() == target) {
                        i = lbl + 1;
                        continue;
                    }
                }
                break; // real conditional flow: stop collecting (conservative)
            }
            if (in instanceof CustomJump) {
                break;
            }
            if (op == Opcodes.ATHROW || isReturnOpcode(op)) {
                break;
            }
            i++;
        }
        if (found.isEmpty()) {
            return null;
        }
        return new FusedConstructor(found);
    }

    private static int nextReal(List<Instruction> body, int from) {
        for (int i = from; i < body.size(); i++) {
            if (!(body.get(i) instanceof LabelInstruction)) {
                return i;
            }
        }
        return -1;
    }

    private static int nextLabelIndex(List<Instruction> body, int from) {
        for (int i = from; i < body.size(); i++) {
            if (body.get(i) instanceof LabelInstruction) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfLabel(List<Instruction> body, org.objectweb.asm.Label target) {
        for (int i = 0; i < body.size(); i++) {
            Instruction in = body.get(i);
            if (in instanceof LabelInstruction && ((LabelInstruction) in).getLabel() == target) {
                return i;
            }
        }
        return -1;
    }

    /** body index of an ATHROW reachable by straight fall-through within cap, else -1. */
    private static int findAthrowByFallthrough(List<Instruction> body, int from, int cap) {
        int seen = 0;
        for (int i = from; i < body.size() && seen < cap; i++) {
            Instruction in = body.get(i);
            if (in instanceof LabelInstruction) {
                continue;
            }
            seen++;
            int op = in.getOpcode();
            if (op == Opcodes.ATHROW) {
                return i;
            }
            if (in instanceof Jump || in instanceof CustomJump || in instanceof SwitchInstruction
                    || isReturnOpcode(op)) {
                return -1;
            }
        }
        return -1;
    }

    private static boolean isReturnOpcode(int op) {
        return op == Opcodes.RETURN || op == Opcodes.ARETURN || op == Opcodes.IRETURN
                || op == Opcodes.LRETURN || op == Opcodes.FRETURN || op == Opcodes.DRETURN;
    }

    private static int constIntValue(Instruction in) {
        switch (in.getOpcode()) {
            case Opcodes.ICONST_0: return 0;
            case Opcodes.ICONST_1: return 1;
            case Opcodes.ICONST_2: return 2;
            case Opcodes.ICONST_3: return 3;
            case Opcodes.ICONST_4: return 4;
            case Opcodes.ICONST_5: return 5;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                if (in instanceof BasicInstruction) {
                    int v = ((BasicInstruction) in).getValue();
                    return v >= 0 ? v : -1;
                }
                if (in instanceof VarOp) {
                    int v = ((VarOp) in).getIndex();
                    return v >= 0 ? v : -1;
                }
                return -1;
            default:
                return -1;
        }
    }

    private static boolean descMatchesArrayType(String fieldDesc, int arrayType) {
        if (fieldDesc == null || fieldDesc.length() != 2 || fieldDesc.charAt(0) != '[') {
            return false;
        }
        switch (fieldDesc.charAt(1)) {
            case 'Z': return arrayType == 4;
            case 'C': return arrayType == 5;
            case 'F': return arrayType == 6;
            case 'D': return arrayType == 7;
            case 'B': return arrayType == 8;
            case 'S': return arrayType == 9;
            case 'I': return arrayType == 10;
            case 'J': return arrayType == 11;
            default: return false;
        }
    }

    /**
     * Emit the FUSED ALLOCATION block at a new-site. On entry the operand stack
     * holds [survivor(placeholder), receiver(placeholder), args...]; this emits
     * straight-line C that leaves BOTH placeholder slots pointing at the new
     * object (fused single block when possible, ordinary allocation otherwise)
     * and then falls through to the caller's ordinary {@code <init>} emission.
     *
     * @param cType     mangled owner class name
     * @param lenExprs  C int expression per child length, in child order
     * @param recvSlot  positive SP offset of the receiver placeholder
     * @param survSlot  positive SP offset of the survivor placeholder
     */
    public void appendFusedAlloc(StringBuilder b, String cType, String[] lenExprs,
                                 int recvSlot, int survSlot) {
        b.append("    { /* FUSED construction of ").append(cType).append(" */\n");
        b.append("    if(__builtin_expect(!class__").append(cType)
         .append(".initialized, 0)) __STATIC_INITIALIZER_").append(cType).append("(threadStateData);\n");
        for (int i = 0; i < children.size(); i++) {
            b.append("    int __fLen").append(i).append(" = ").append(lenExprs[i]).append(";\n");
        }
        b.append("    JAVA_OBJECT __fo = JAVA_NULL;\n");
        b.append("    if(1");
        for (int i = 0; i < children.size(); i++) {
            b.append(" && __fLen").append(i).append(" >= 0");
        }
        b.append(") {\n");
        b.append("        int __fOff = (int)((sizeof(struct obj__").append(cType)
         .append(") + 7) & ~(size_t)7);\n");
        for (int i = 0; i < children.size(); i++) {
            b.append("        int __fOff").append(i).append(" = __fOff;\n");
            b.append("        __fOff += CN1_FUSED_ARR_BYTES(__fLen").append(i)
             .append(", sizeof(").append(children.get(i).elemCType()).append("));\n");
        }
        b.append("        __fo = cn1AllocFused(threadStateData, __fOff, &class__").append(cType).append(");\n");
        b.append("        if(__fo != JAVA_NULL) {\n");
        for (int i = 0; i < children.size(); i++) {
            Child c = children.get(i);
            b.append("            ((struct obj__").append(c.cOwner).append("*)__fo)->")
             .append(c.cOwner).append("_").append(c.fieldName)
             .append(" = cn1FusedInstallPrimArray(__fo, __fOff").append(i).append(", ")
             .append(c.arrayClassRef()).append(", sizeof(").append(c.elemCType())
             .append("), __fLen").append(i).append(");\n");
        }
        b.append("        }\n");
        b.append("    }\n");
        // fallback: ordinary allocation; the KEEP-IF-NULL ctor then allocates the
        // children as plain arrays -- byte-for-byte the un-fused semantics.
        b.append("    if(__fo == JAVA_NULL) { __fo = CN1_FAST_NEW(").append(cType).append("); }\n");
        b.append("    SP[-").append(recvSlot).append("].data.o = __fo;\n");
        b.append("    SP[-").append(survSlot).append("].data.o = __fo;\n");
        b.append("    }\n");
    }
}
