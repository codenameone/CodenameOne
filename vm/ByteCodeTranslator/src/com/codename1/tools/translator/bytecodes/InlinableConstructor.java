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
import com.codename1.tools.translator.ByteCodeField;
import com.codename1.tools.translator.ByteCodeMethodArg;
import com.codename1.tools.translator.BytecodeMethod;
import com.codename1.tools.translator.Parser;
import com.codename1.tools.translator.Util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/**
 * LEVER B (perf-tier1): inline a SMALL, leaf-ish constructor body as direct field
 * stores at the {@code new}-site, eliminating the out-of-line cross-translation-unit
 * {@code <X>___INIT___} call that runs right after the inlined BiBOP bump alloc
 * (Lever 1). Composes with Lever 1 so alloc + construct become one inlined sequence,
 * like HotSpot.
 *
 * INLINABILITY PREDICATE (deliberately conservative -- any miss falls back to the
 * out-of-line call, so a false-negative only forfeits the optimization, never
 * correctness):
 * <ul>
 *   <li>super() is exactly {@code java/lang/Object.<init>()V} (a genuine no-op in
 *       the generated C). A non-Object / arg-bearing super-ctor bails. (Chained
 *       inlinable supers are a possible future extension; none of the in-scope hot
 *       allocation sites need them.)</li>
 *   <li>the remaining body is zero or more {@code ALOAD 0; <value>; PUTFIELD f}
 *       triples, where {@code <value>} is a single PARAMETER load (matching the
 *       field's type category) or a simple integer/null constant -- no computed
 *       expressions, no field reads, no allocation.</li>
 *   <li>terminated by {@code RETURN}; NOTHING else may appear (no branches, no other
 *       INVOKE, no NEW/array/throw/dup/arithmetic/monitor/GETFIELD).</li>
 *   <li>bounded instruction count.</li>
 * </ul>
 *
 * GC: at the new-site the object is already a GC root (it sits on the operand stack),
 * and the bump/calloc zeroed its reference fields, so storing into them via the
 * existing {@code set_field_*} accessors (which carry the VM's write barrier, a no-op
 * unless the nursery is enabled) is exactly what the out-of-line ctor would do. The
 * straight-line field stores contain no safepoint, so no GC can interleave the
 * partially-constructed object.
 */
public final class InlinableConstructor {
    /** Hard cap on inlined field stores (keeps code growth + analysis bounded). */
    private static final int MAX_STORES = 8;

    /** A single {@code this.field = <source>} store. */
    public static final class Store {
        /** declaring class in C form, e.g. {@code com_bench_Bench_Node}. */
        final String cOwner;
        /** field name, e.g. {@code next}. */
        final String fieldName;
        /** type category of the field value: 'o','i','l','f','d'. */
        final char fieldCat;
        /** 1-based constructor parameter index (param 1 == first real arg), or -1. */
        final int paramIndex;
        /** non-null when the stored value is a constant literal (paramIndex == -1). */
        final String constLiteral;

        Store(String cOwner, String fieldName, char fieldCat, int paramIndex, String constLiteral) {
            this.cOwner = cOwner;
            this.fieldName = fieldName;
            this.fieldCat = fieldCat;
            this.paramIndex = paramIndex;
            this.constLiteral = constLiteral;
        }
    }

    private final List<Store> stores;

    private InlinableConstructor(List<Store> stores) {
        this.stores = stores;
    }

    /** number of field stores (0 for a pure {@code super()} ctor). */
    public int storeCount() {
        return stores.size();
    }

    /**
     * Emit the inlined field stores. {@code objExpr} is the C expression for the new
     * object; {@code argExprs[p-1]} is the C expression for constructor parameter p.
     */
    public void appendStores(StringBuilder b, String objExpr, String[] argExprs) {
        for (Store s : stores) {
            String value = s.paramIndex > 0 ? argExprs[s.paramIndex - 1] : s.constLiteral;
            // Direct struct-member store -- byte-for-byte the body of the generated
            // set_field_<owner>_<name>() accessor, but emitted INLINE so clang folds
            // it to a single store (the accessor itself is a cross-TU call the no-LTO
            // build cannot inline -- the very call Lever B exists to remove). The
            // struct type obj__<owner> is already complete in this TU (the new-site
            // references it). Object stores carry CN1_WRITE_BARRIER, exactly as the
            // accessor does (a no-op unless the nursery is enabled).
            String lhs = "((struct obj__" + s.cOwner + "*)(" + objExpr + "))->" + s.cOwner + "_" + s.fieldName;
            b.append("    ");
            if (s.fieldCat == 'o') {
                b.append("CN1_WRITE_BARRIER(").append(objExpr).append(", ").append(value).append("); ");
            }
            b.append(lhs).append(" = ").append(value).append(";\n");
        }
    }

    /** The set of instance-field names this ctor writes (for the unwritten-set diff). */
    private Set<String> writtenFieldNames() {
        Set<String> w = new HashSet<String>();
        for (Store s : stores) {
            w.add(s.fieldName);
        }
        return w;
    }

    /**
     * Emit per-argument C temps ({@code __ibpaN_}) in ARGUMENT ORDER and return the
     * temp names to substitute for {@code argExprs}. Folded literal args are not
     * always pure: one can be a call expression (a folded {@code CustomInvoke} with
     * a non-object return) or a throwing load (GETFIELD null check, array bounds).
     * Evaluating those at each USE site -- i.e. in ctor-body store order -- would
     * break Java's left-to-right argument evaluation, double-evaluate an arg the
     * ctor stores into two fields, and (on the no-memset path) run code that can
     * reach a GC safepoint while the half-built object is visible to the
     * conservative native-stack scan. Hoisting every arg into a temp BEFORE the
     * allocation restores Java call semantics and keeps the alloc->publish window
     * free of calls and throws. clang -O3 copy-propagates the temps, so pure args
     * cost nothing.
     */
    public static String[] appendArgTemps(StringBuilder b, String[] argExprs, char[] argCats) {
        String[] temps = new String[argExprs.length];
        for (int i = 0; i < argExprs.length; i++) {
            String t;
            switch (argCats[i]) {
                case 'o': t = "JAVA_OBJECT"; break;
                case 'l': t = "JAVA_LONG"; break;
                case 'f': t = "JAVA_FLOAT"; break;
                case 'd': t = "JAVA_DOUBLE"; break;
                default:  t = "JAVA_INT"; break;
            }
            temps[i] = "__ibpa" + i + "_";
            b.append("    ").append(t).append(" ").append(temps[i]).append(" = ")
             .append(argExprs[i]).append(";\n");
        }
        return temps;
    }

    /**
     * INIT-BEFORE-PUBLISH codegen (memset elimination). Evaluates the ctor args
     * into C temps (arg order -- see {@link #appendArgTemps}) when {@code argCats}
     * is non-null, allocates {@code cType} WITHOUT the body memset, runs the
     * inlined ctor field stores, explicitly zeroes every instance field the ctor
     * does NOT write (reference fields -> JAVA_NULL for GC safety; primitive
     * fields -> 0 for Java default-value semantics), THEN publishes the fully-
     * built object into the surviving operand-stack slot and pops the
     * receiver+args. Between the alloc and the publish the emitted C is
     * straight-line loads/stores only -- no calls, no throws, no safepoint -- so
     * the concurrent/conservative collector can never observe (or trace) the
     * half-built object.
     *
     * @param cType        mangled class name (e.g. {@code com_bench_Bench_Node})
     * @param argExprs     C expressions for the ctor parameters
     * @param argCats      per-arg type category ('o','i','l','f','d') for temp
     *                     hoisting; null when every argExpr is a pure, non-throwing
     *                     operand-stack read ({@code SP[-k].data.x})
     * @param survivorSlot the surviving object slot, as a POSITIVE SP offset k
     *                     (published via {@code SP[-k].data.o})
     * @param pop          number of stack slots to pop (receiver + on-stack args)
     */
    public void appendInitBeforePublish(StringBuilder b, String cType, String[] argExprs,
                                        char[] argCats, int survivorSlot, int pop) {
        b.append("    {\n");
        if (argCats != null) {
            argExprs = appendArgTemps(b, argExprs, argCats);
        }
        b.append("    JAVA_OBJECT __ibp = CN1_FAST_NEW_NOZERO(").append(cType).append(");\n");
        // ctor-written fields (params / constants)
        appendStores(b, "__ibp", argExprs);
        // explicit zeros for the fields the ctor does NOT write (empty when the
        // ctor writes every field). clang -O3 coalesces adjacent zero
        // stores; padding is neither written nor GC-scanned so it is left alone.
        Set<String> written = writtenFieldNames();
        ByteCodeClass cls = Parser.getClassObject(cType);
        if (cls != null) {
            for (ByteCodeField f : cls.getAllInstanceFieldsInLayoutOrder()) {
                if (written.contains(f.getFieldName())) {
                    continue;
                }
                b.append("    ((struct obj__").append(f.getClsName()).append("*)(__ibp))->")
                 .append(f.getClsName()).append("_").append(f.getFieldName())
                 .append(" = ").append(f.isObjectType() ? "JAVA_NULL" : "0").append(";\n");
            }
        }
        // parentCls was left 0 by cn1BibopFastAllocNoZero so that a signal-stopped
        // thread's conservative scan skips the mid-construction object (gcMarkObject
        // guards on parentCls==0). Set it only now, with every field written: from
        // this store on the object is safely traceable. (The __NEW_X slow-path
        // fallback already set it -- rewriting the same value is harmless.)
        b.append("    __ibp->__codenameOneParentClsReference = &class__").append(cType).append(";\n");
        // publish: the object becomes a GC root only now, fully constructed.
        b.append("    SP[-").append(survivorSlot).append("].data.o = __ibp;\n");
        b.append("    SP -= ").append(pop).append("; }\n");
    }

    /**
     * Look up the constructor declared by {@code owner} with signature {@code desc}
     * and return its inlinability plan (computed once from RAW bytecode at parse time),
     * or null. {@code owner} is the JVM internal name ({@code com/bench/Bench$Node}).
     * Called at emit time from the new-site.
     */
    public static InlinableConstructor analyze(String owner, String desc) {
        ByteCodeClass cls = Parser.getClassObject(owner.replace('/', '_').replace('$', '_'));
        if (cls == null) {
            return null;
        }
        for (BytecodeMethod m : cls.getMethods()) {
            // NB: a ctor's methodName is rewritten to "__INIT__" in the BytecodeMethod
            // ctor, so match on isConstructor() + the (untouched) descriptor.
            if (m.isConstructor() && desc.equals(m.getSignature())) {
                m.computeInlinableConstructorPlan(); // idempotent; normally already done
                return m.getInlinableConstructorPlan();
            }
        }
        return null;
    }

    /**
     * Analyze the RAW (pre-optimize) instruction list of {@code ctor}. Called once at
     * parse time; the result is cached on the {@link BytecodeMethod}.
     */
    public static InlinableConstructor analyzeRaw(BytecodeMethod ctor, String desc) {
        // Filter out the no-op bookkeeping instructions exactly like the existing
        // trivial getter/setter inliner (Invoke.trivialSetterField).
        List<Instruction> body = new ArrayList<Instruction>();
        for (Instruction in : ctor.getInstructions()) {
            if (in instanceof LineNumber || in instanceof LabelInstruction || in instanceof LocalVariable) {
                continue;
            }
            body.add(in);
        }
        // Minimum: ALOAD0; INVOKESPECIAL Object.<init>; RETURN.
        if (body.size() < 3) {
            return null;
        }
        // ---- super(): ALOAD 0; INVOKESPECIAL java/lang/Object.<init>()V ----
        Instruction a0 = body.get(0);
        Instruction sup = body.get(1);
        if (!(a0 instanceof VarOp) || a0.getOpcode() != Opcodes.ALOAD || ((VarOp) a0).getIndex() != 0) {
            return null;
        }
        if (!(sup instanceof Invoke)) {
            return null;
        }
        Invoke supInv = (Invoke) sup;
        if (supInv.getOpcode() != Opcodes.INVOKESPECIAL
                || !"<init>".equals(supInv.getName())
                || !"java/lang/Object".equals(supInv.getOwner())
                || !"()V".equals(supInv.getDesc())) {
            return null;
        }

        // slot -> 1-based parameter index (long/double occupy two local slots).
        List<ByteCodeMethodArg> args = Util.getMethodArgs(desc);
        int maxSlot = 1;
        for (ByteCodeMethodArg arg : args) {
            maxSlot += arg.isDoubleOrLong() ? 2 : 1;
        }
        int[] slotToParam = new int[maxSlot];
        char[] paramCat = new char[args.size() + 1]; // 1-based
        int slot = 1;
        for (int p = 0; p < args.size(); p++) {
            slotToParam[slot] = p + 1;
            paramCat[p + 1] = args.get(p).getQualifier();
            slot += args.get(p).isDoubleOrLong() ? 2 : 1;
        }

        List<Store> stores = new ArrayList<Store>();
        int i = 2;
        while (i + 2 < body.size()) {
            Instruction la = body.get(i);
            if (!(la instanceof VarOp) || la.getOpcode() != Opcodes.ALOAD || ((VarOp) la).getIndex() != 0) {
                break; // not a "this.field = ..." store; expect RETURN next
            }
            Instruction val = body.get(i + 1);
            Instruction pf = body.get(i + 2);
            if (!(pf instanceof Field) || pf.getOpcode() != Opcodes.PUTFIELD) {
                return null;
            }
            Field f = (Field) pf;
            char cat = fieldCategory(f.getDesc());
            if (cat == 0) {
                return null;
            }
            int paramIndex = -1;
            String constLiteral = null;
            if (val instanceof VarOp && isLoadOpcode(val.getOpcode())) {
                int s = ((VarOp) val).getIndex();
                if (s <= 0 || s >= slotToParam.length || slotToParam[s] == 0) {
                    return null; // not a parameter slot (e.g. a temp local)
                }
                int p = slotToParam[s];
                if (paramCat[p] != cat || loadCategory(val.getOpcode()) != cat) {
                    return null; // type-category mismatch
                }
                paramIndex = p;
            } else {
                constLiteral = constLiteral(val, cat);
                if (constLiteral == null) {
                    return null; // unsupported value source
                }
            }
            if (stores.size() >= MAX_STORES) {
                return null;
            }
            stores.add(new Store(f.getOwner().replace('/', '_').replace('$', '_'),
                    f.getFieldName(), cat, paramIndex, constLiteral));
            i += 3;
        }
        // The only thing allowed after the stores is the void RETURN, and it must be
        // the final instruction.
        if (i != body.size() - 1) {
            return null;
        }
        Instruction ret = body.get(i);
        if (ret.getOpcode() != Opcodes.RETURN) {
            return null;
        }
        return new InlinableConstructor(stores);
    }

    /** Maps a field descriptor to a value category, or 0 if unsupported. */
    private static char fieldCategory(String desc) {
        switch (desc.charAt(0)) {
            case 'L':
            case '[':
                return 'o';
            case 'J':
                return 'l';
            case 'F':
                return 'f';
            case 'D':
                return 'd';
            case 'I':
            case 'S':
            case 'B':
            case 'C':
            case 'Z':
                return 'i';
            default:
                return 0;
        }
    }

    private static boolean isLoadOpcode(int op) {
        return op == Opcodes.ILOAD || op == Opcodes.LLOAD || op == Opcodes.FLOAD
                || op == Opcodes.DLOAD || op == Opcodes.ALOAD;
    }

    private static char loadCategory(int op) {
        switch (op) {
            case Opcodes.ILOAD: return 'i';
            case Opcodes.LLOAD: return 'l';
            case Opcodes.FLOAD: return 'f';
            case Opcodes.DLOAD: return 'd';
            case Opcodes.ALOAD: return 'o';
            default: return 0;
        }
    }

    /**
     * Conservative constant-source support: integer-category constants and null.
     * Anything else (float/long/double/LDC/BIPUSH/SIPUSH) bails to keep the analyzer
     * small; those values are uncommon in trivial leaf ctors and merely forfeit the
     * inlining.
     */
    private static String constLiteral(Instruction val, char cat) {
        int op = val.getOpcode();
        if (cat == 'o' && op == Opcodes.ACONST_NULL) {
            return "JAVA_NULL";
        }
        if (cat == 'i') {
            switch (op) {
                case Opcodes.ICONST_M1: return "-1";
                case Opcodes.ICONST_0: return "0";
                case Opcodes.ICONST_1: return "1";
                case Opcodes.ICONST_2: return "2";
                case Opcodes.ICONST_3: return "3";
                case Opcodes.ICONST_4: return "4";
                case Opcodes.ICONST_5: return "5";
                default: return null;
            }
        }
        return null;
    }
}
