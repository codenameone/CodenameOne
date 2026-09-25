/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Field;
import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.bytecodes.CustomJump;
import com.codename1.tools.translator.bytecodes.Invoke;
import com.codename1.tools.translator.bytecodes.Jump;
import com.codename1.tools.translator.bytecodes.SwitchInstruction;
import com.codename1.tools.translator.bytecodes.TryCatch;
import org.objectweb.asm.Label;
import com.codename1.tools.translator.bytecodes.LabelInstruction;
import com.codename1.tools.translator.bytecodes.LineNumber;
import com.codename1.tools.translator.bytecodes.LocalVariable;
import com.codename1.tools.translator.bytecodes.TypeInstruction;
import com.codename1.tools.translator.bytecodes.VarOp;
import java.util.List;
import org.objectweb.asm.Opcodes;

/// Does a reference escape the frame that produced it?
///
/// This exists for one purpose: deciding whether an iterator may live in the caller's C
/// stack frame instead of the heap. A stack-resident object is only sound if nothing can
/// still reach it after the frame unwinds, and "nothing" has to be proved rather than
/// assumed -- a stale reference to a dead frame is a native crash no Java catch can see.
///
/// The analysis is an abstract stack simulation that tracks exactly one bit per stack
/// slot: is this value the reference we are following? Everything else is "other". It is
/// deliberately conservative in one direction only -- when it cannot follow the value it
/// answers ESCAPES, never SAFE.
///
/// Branches are the part worth explaining. Simulating them properly needs a fixpoint over
/// the control-flow graph, which this does not do. Instead it enforces a stronger and much
/// simpler property: **the tracked reference is never live on the operand stack across a
/// branch or a label**. If it is, the method is rejected. Real iterator code never does
/// this (a receiver is pushed and consumed by the next instruction or two), so the
/// restriction costs nothing here, and it makes the linear walk sound: no value can flow
/// into a jump target that the walk did not already see.
class IteratorEscape {
    /// Result of an analysis. SAFE is the only value that permits stack allocation.
    static final int SAFE = 0;
    /// The reference provably reaches a field, an array, another frame or a return.
    static final int ESCAPES = 1;
    /// The walk lost track of the value -- an unmodelled instruction, or the reference
    /// live across a branch. Treated exactly like ESCAPES; kept separate so the census can
    /// report how much of the refusal is ignorance rather than a real leak.
    static final int UNKNOWN = 2;

    /// Why the last analysis answered UNKNOWN. Diagnostic only -- an analysis that
    /// refuses everything and an analysis that refuses nothing look identical from the
    /// counts alone, and the first cut refused all 37 classes on the same instruction.
    static String lastReason = "";

    /// Callees invoked ON the tracked reference during the last walk, as
    /// owner.name+desc. Valid only immediately after a walk, exactly like lastReason.
    /// A site is only retirable if none of these leaks `this`.
    static final java.util.List<String> receiverCalls = new java.util.ArrayList<String>();

    /// The local the last walk followed, or -1 if the value never reached one. Valid
    /// only immediately after a walk. Frame-exit retirement needs it: the object to
    /// retire is whatever that local holds when the frame goes.
    static int lastTrackedLocal = -1;

    private IteratorEscape() {
    }

    /// Does `this` escape any method of the class? The constructor is included, and it is
    /// the one that matters most: `X.<init>` storing `this` into its parent is the classic
    /// way an iterator outlives the loop that made it.
    static int thisEscapes(BytecodeMethod m) {
        if (m.isStatic()) {
            return SAFE;   // no `this` to leak
        }
        return walk(m, true, null, true);
    }

    /// Does the object allocated by `new owner` escape the method that allocates it,
    /// other than by being returned? Returning is allowed because the caller's own
    /// non-escape proof covers what happens next -- that is the whole contract between
    /// this method and the for-each site.
    static int newEscapes(BytecodeMethod m, String owner) {
        return walk(m, false, owner, false);
    }

    /// Same question, but ARETURN of the tracked value counts as an escape.
    ///
    /// The iterator scheme allocates in the CALLER's frame, so a factory that returns the
    /// object is handing it to the frame it already lives in -- not a leak. Any scheme that
    /// reclaims at the END of the allocating method (freeing a page wholesale, say) does not
    /// have that property: a returned reference outlives the frame that owns the storage.
    /// Census-only today; it exists so the two populations can be counted separately before
    /// anything is built on either.
    static int newEscapesStrict(BytecodeMethod m, String owner) {
        clusterStores = false;
        return walk(m, false, owner, true);
    }

    /// Does this NEW's object escape the frame, allowing CLUSTER-INTERNAL stores?
    ///
    /// Same question as newEscapesStrict, except that storing a tracked reference into
    /// a field of another tracked object is permitted. That admits self-referential
    /// structures built and dropped inside one frame -- linked lists, chains, small
    /// trees -- which per-object analysis must reject because every member is stored
    /// into another member.
    ///
    /// SOUNDNESS RESTS ON NON-ESCAPE: if no reference to the cluster leaves the frame,
    /// no code outside the frame can store into a cluster member's fields, so this walk
    /// sees every store that can ever happen to one. The single exception is the
    /// constructor, which runs in its own frame -- closed separately by
    /// ctorOnlyStoresParamsIntoThis.
    static int clusterEscapes(BytecodeMethod m, String owner) {
        clusterStores = true;
        int r = walk(m, false, owner, true);
        clusterStores = false;
        return r;
    }

    /// Set only for the duration of a clusterEscapes walk.
    private static boolean clusterStores = false;

    /// Does this constructor put its parameters NOWHERE except into fields of `this`?
    ///
    /// Deliberately a shape check rather than a dataflow: the body must consist only of
    /// loads, primitive arithmetic, PUTFIELDs, a super constructor call on `this`, and
    /// the return. Anything else -- a PUTSTATIC, a call taking a parameter, an array
    /// store, a throw, a second NEW -- and the answer is no.
    ///
    /// This is what closes the one hole in the cluster argument. Non-escape means no
    /// code outside the frame can store into a cluster member's fields, so the frame's
    /// own walk sees every store -- EXCEPT the ones the constructor performs in its own
    /// frame. Restricting the constructor to "parameters into my own fields" makes those
    /// stores knowable from the call site, where every reference parameter has already
    /// been shown to be a cluster member.
    static boolean ctorOnlyStoresParamsIntoThis(String owner, String desc) {
        BytecodeMethod ctor = null;
        String mangled = mangle(owner);
        for (ByteCodeClass c : Parser.getClasses()) {
            if (!mangle(c.getClsName()).equals(mangled)) {
                continue;
            }
            for (BytecodeMethod m : c.getMethods()) {
                if ("__INIT__".equals(m.getMethodName()) && desc.equals(m.getDesc())) {
                    ctor = m;
                    break;
                }
            }
            break;
        }
        if (ctor == null || ctor.isNative() || ctor.isAbstract()) {
            return false;   // an unknown body is an unchecked body
        }
        for (Instruction i : ctor.getInstructions()) {
            int op = i.getOpcode();
            if (i instanceof LabelInstruction || i instanceof LineNumber
                    || i instanceof LocalVariable) {
                continue;
            }
            if (i instanceof Field) {
                if (op == Opcodes.PUTFIELD || op == Opcodes.GETFIELD) {
                    continue;   // own-field traffic
                }
                return false;   // PUTSTATIC/GETSTATIC
            }
            if (i instanceof Invoke) {
                Invoke in = (Invoke) i;
                // Only the super constructor, and only with no arguments of its own.
                if (op == Opcodes.INVOKESPECIAL && "<init>".equals(in.getName())
                        && in.getArgs().isEmpty()) {
                    continue;
                }
                return false;
            }
            if (i instanceof TypeInstruction || i instanceof Jump || i instanceof CustomJump
                    || i instanceof SwitchInstruction || i instanceof TryCatch) {
                return false;   // allocation or control flow: out of shape
            }
            if (i instanceof VarOp) {
                if (op == Opcodes.ALOAD || op == Opcodes.ILOAD || op == Opcodes.LLOAD
                        || op == Opcodes.FLOAD || op == Opcodes.DLOAD) {
                    continue;
                }
                return false;   // a STORE means the body is doing more than wiring
            }
            switch (op) {
                case Opcodes.RETURN:
                case Opcodes.ACONST_NULL:
                case Opcodes.DUP:
                case Opcodes.ICONST_0: case Opcodes.ICONST_1: case Opcodes.ICONST_2:
                case Opcodes.ICONST_3: case Opcodes.ICONST_4: case Opcodes.ICONST_5:
                case Opcodes.ICONST_M1:
                    continue;
                default:
                    return false;
            }
        }
        return true;
    }

    /// @param trackThis    follow local 0 (ALOAD 0) rather than a NEW result
    /// @param newOwner     internal name of the class whose NEW result is followed
    /// @param returnIsLeak whether ARETURN of the tracked value counts as an escape
    private static int walk(BytecodeMethod m, boolean trackThis, String newOwner,
            boolean returnIsLeak) {
        receiverCalls.clear();
        lastTrackedLocal = -1;
        List<Instruction> ins = m.getInstructions();
        // WHICH LABELS ARE ACTUALLY JOIN POINTS.
        //
        // The walk used to refuse whenever the tracked value was on the stack at ANY
        // LabelInstruction. Most labels are not join points at all -- nothing branches
        // to them -- and a label nothing targets can only be reached by falling through
        // from the instruction above it, which is the very path this linear walk models.
        // Measured on the self-hosting corpus: 98 of 198 undecidable sites (49%) were
        // refused at a label with no incoming control flow.
        //
        // The set is every label some instruction can transfer control TO: Jump,
        // CustomJump (a fused conditional branch), every arm and the default of a
        // switch, and all three labels of a TryCatch -- an exception can enter the
        // handler from anywhere inside [start, end), so the region bounds count too.
        // LocalVariable's scopeStart/scopeEnd are debug metadata, not control flow,
        // and are deliberately NOT included; treating them as join points would put
        // nearly every label back in the set and recover nothing.
        java.util.Set<Object> joinLabels = new java.util.HashSet<Object>();
        for (Instruction ji : ins) {
            if (ji instanceof Jump) {
                joinLabels.add(((Jump) ji).getLabel());
            } else if (ji instanceof CustomJump) {
                joinLabels.add(((CustomJump) ji).getLabel());
            } else if (ji instanceof SwitchInstruction) {
                SwitchInstruction sw = (SwitchInstruction) ji;
                joinLabels.add(sw.getDefaultLabel());
                Label[] arms = sw.getLabels();
                if (arms != null) {
                    for (int a = 0; a < arms.length; a++) {
                        joinLabels.add(arms[a]);
                    }
                }
            } else if (ji instanceof TryCatch) {
                TryCatch tc = (TryCatch) ji;
                joinLabels.add(tc.getStart());
                joinLabels.add(tc.getEnd());
                joinLabels.add(tc.getHandler());
            }
        }
        // The abstract operand stack: true means "this slot holds the tracked reference".
        boolean[] stack = new boolean[Math.max(8, m.getMaxStack() + 8)];
        int sp = 0;
        boolean sawSource = false;
        // One tracked local. The common factory shape is `X it = new X(this); ... return
        // it;`, which an ASTORE would otherwise abandon -- 9 of the refused sites were
        // exactly that. One is enough: a second store of the tracked value to a different
        // local gives up rather than growing into a general dataflow.
        // ALIAS SET, not a single local. The walk used to refuse outright when the
        // tracked value was stored to a SECOND local -- 59 of 137 remaining undecidable
        // sites on the self-hosting corpus, the largest single cause left. A set of
        // locals is the natural answer and needs no control-flow fixpoint.
        //
        // It is deliberately never CLEARED. Marking a local as possibly-holding the
        // value when it does not is an over-approximation: it can only make the walk
        // report ESCAPES where the truth is SAFE. Clearing is the opposite and is
        // unsound on a linear walk -- if a branch skips the overwriting store, the local
        // still holds the tracked value while the walk believes it does not, and a later
        // ALOAD would hand back "untracked" and miss a real escape.
        boolean[] trackedLocals = new boolean[Math.max(8, m.getMaxLocals() + 8)];
        for (Instruction i : ins) {
            int op = i.getOpcode();
            // A label or a jump is a join point. The linear walk cannot model where the
            // tracked value would flow, so require that it is not on the stack at all
            // here; then there is nothing to flow and the walk stays sound.
            boolean joinPoint = isBranch(op)
                    || (i instanceof LabelInstruction
                        && joinLabels.contains(((LabelInstruction) i).getLabel()));
            // A label nothing branches to is a no-op for this walk: it moves no values
            // and has no stack effect. Skipping it is what makes the join-point set
            // useful -- otherwise the walk falls through to the generic stack model,
            // which has no entry for a label and refuses with "undescribed stack
            // effect" instead. Measured: that moved 72 of the 98 recovered sites
            // straight back into the undecidable bucket under a different name.
            // Pseudo-instructions carry no stack effect at all: a label nothing
            // branches to, a line-number marker, a local-variable debug range. The
            // generic model has no entry for them and refuses with "undescribed stack
            // effect", which is not a statement about the program. Measured: 98 sites
            // were refused at a LabelInstruction, and once that was fixed the same 72
            // reappeared at LineNumber -- the same non-answer wearing a different name.
            if (i instanceof LineNumber || i instanceof LocalVariable
                    || (i instanceof LabelInstruction && !joinPoint)) {
                continue;
            }
            if (joinPoint) {
                // POP THE BRANCH'S OWN OPERANDS BEFORE ASKING WHAT SURVIVES.
                //
                // A conditional branch CONSUMES the values it tests; they reach neither
                // successor. Checking the whole stack first therefore refuses on the very
                // value the branch destroys. `while (p != null)` compiles to IFNULL on the
                // tracked reference, and that alone rejected the linked-list shape this
                // analysis exists to accept -- reason "tracked value live across
                // branch/label op=198", with every other cluster rule already satisfied.
                //
                // What must not be live is whatever REMAINS on the stack afterwards: that
                // is what flows into the join, and the linear walk cannot model where it
                // goes.
                if (isBranch(op)) {
                    sp = Math.max(0, sp - branchPops(op));
                }
                for (int s = 0; s < sp; s++) {
                    if (stack[s]) {
                        lastReason = "tracked value live across branch/label op=" + op
                                + " (" + i.getClass().getSimpleName() + ")";
                        return UNKNOWN;
                    }
                }
                continue;
            }
            // --- sources of the tracked reference -------------------------------------
            if (trackThis && i instanceof VarOp && op == Opcodes.ALOAD
                    && ((VarOp) i).getIndex() == 0) {
                sawSource = true;
                stack = push(stack, sp++, true);
                continue;
            }
            if (!trackThis && i instanceof TypeInstruction && op == Opcodes.NEW
                    && newOwner.equals(mangle(((TypeInstruction) i).getTypeName()))) {
                sawSource = true;
                stack = push(stack, sp++, true);
                continue;
            }
            if (i instanceof VarOp && op == Opcodes.ALOAD) {
                int li = ((VarOp) i).getIndex();
                if (li >= 0 && li < trackedLocals.length && trackedLocals[li]) {
                    stack = push(stack, sp++, true);
                    continue;
                }
            }
            // --- sinks ----------------------------------------------------------------
            if (i instanceof Field) {
                Field f = (Field) i;
                if (op == Opcodes.PUTFIELD) {
                    // ..., objectref, value ->
                    boolean value = sp > 0 && stack[sp - 1];
                    boolean target = sp > 1 && stack[sp - 2];
                    sp = Math.max(0, sp - 2);
                    if (value) {
                        // A CLUSTER-INTERNAL STORE IS NOT AN ESCAPE.
                        //
                        // Storing a tracked reference into a field of ANOTHER tracked
                        // object keeps it inside the same frame-local cluster -- the
                        // reference has not become reachable from anywhere new. The
                        // linked-list shape this exists for is `head = new Node(v, head)`:
                        // no single Node is frame-local, because each is stored into the
                        // next one's field, yet the chain as a whole never leaves the
                        // frame. Per-object escape analysis rejects every member of it,
                        // which is why HotSpot does not scalar-replace these either.
                        if (target && clusterStores) {
                            continue;
                        }
                        return ESCAPES;   // stored into the heap
                    }
                    continue;
                }
                if (op == Opcodes.PUTSTATIC) {
                    boolean value = sp > 0 && stack[sp - 1];
                    sp = Math.max(0, sp - 1);
                    if (value) {
                        return ESCAPES;
                    }
                    continue;
                }
                if (op == Opcodes.GETFIELD) {
                    sp = Math.max(0, sp - 1);   // receiver consumed, result is "other"
                    stack = push(stack, sp++, false);
                    continue;
                }
                if (op == Opcodes.GETSTATIC) {
                    stack = push(stack, sp++, false);
                    continue;
                }
            }
            if (i instanceof Invoke) {
                Invoke inv = (Invoke) i;
                int argCount = inv.getArgs().size();
                boolean hasReceiver = op != Opcodes.INVOKESTATIC && op != Opcodes.INVOKEDYNAMIC;
                // Arguments sit above the receiver. Passing the tracked reference as an
                // ARGUMENT hands it to a frame this analysis has not examined, so it is
                // an escape; being the RECEIVER is not, because a receiver is only a
                // `this` inside the callee and that callee is checked on its own.
                boolean ctorAbsorbs = false;
                if (clusterStores && hasReceiver && op == Opcodes.INVOKESPECIAL
                        && "<init>".equals(inv.getName())) {
                    int ridx0 = sp - 1 - argCount;
                    // The receiver is a cluster member and the constructor puts its
                    // parameters nowhere but into that receiver's own fields, so a
                    // tracked argument stays inside the cluster. This is the other half
                    // of the linked-list shape: `new Node(v, head)` hands the previous
                    // member to the constructor, which stores it in this.next.
                    if (ridx0 >= 0 && stack[ridx0]
                            && ctorOnlyStoresParamsIntoThis(inv.getOwner(), inv.getDesc())) {
                        ctorAbsorbs = true;
                    }
                }
                for (int a = 0; a < argCount; a++) {
                    int idx = sp - 1 - a;
                    if (idx >= 0 && stack[idx] && !ctorAbsorbs) {
                        return ESCAPES;
                    }
                }
                // The tracked value as a RECEIVER is not an escape here, but only
                // because the callee is checked separately. Record WHICH callee, so
                // that check can be "do the methods this site actually calls leak
                // `this`" instead of "does any method of the class leak `this`".
                // The difference is decisive: ArrayList.iterator() hands `this` to the
                // iterator, which disqualifies EVERY ArrayList under the per-class
                // question -- including the overwhelming majority of methods that only
                // ever call add() and size().
                if (hasReceiver) {
                    int ridx = sp - 1 - argCount;
                    if (ridx >= 0 && stack[ridx]) {
                        receiverCalls.add(inv.getOwner() + "." + inv.getName() + inv.getDesc());
                    }
                }
                sp = Math.max(0, sp - argCount - (hasReceiver ? 1 : 0));
                char[] out = i.getStackOutputTypes();
                for (int o = 0; o < out.length; o++) {
                    stack = push(stack, sp++, false);
                }
                continue;
            }
            if (op == Opcodes.ARETURN) {
                boolean value = sp > 0 && stack[sp - 1];
                sp = Math.max(0, sp - 1);
                if (value && returnIsLeak) {
                    return ESCAPES;
                }
                continue;
            }
            if (op == Opcodes.AASTORE) {
                // ..., arrayref, index, value ->
                boolean value = sp > 0 && stack[sp - 1];
                sp = Math.max(0, sp - 3);
                if (value) {
                    return ESCAPES;
                }
                continue;
            }
            if (op == Opcodes.ASTORE) {
                boolean value = sp > 0 && stack[sp - 1];
                sp = Math.max(0, sp - 1);
                int idx = i instanceof VarOp ? ((VarOp) i).getIndex() : -1;
                if (value) {
                    if (idx < 0 || idx >= trackedLocals.length) {
                        lastReason = "ASTORE of the tracked value to an unaddressable local";
                        return UNKNOWN;
                    }
                    trackedLocals[idx] = true;
                    lastTrackedLocal = idx;
                }
                // No clear on an untracked store -- see the alias-set note above.
                continue;
            }
            if (op == Opcodes.DUP) {
                boolean top = sp > 0 && stack[sp - 1];
                stack = push(stack, sp++, top);
                continue;
            }
            if (i instanceof TypeInstruction && op == Opcodes.NEW) {
                // A NEW of some other class pushes a reference that is not the one being
                // followed. Without this the walk hit the fallback with the tracked value
                // still live and gave up, which is what made 7 iterator factories
                // unanalysable: they allocate something else on the way.
                stack = push(stack, sp++, false);
                continue;
            }
            if (op == Opcodes.ANEWARRAY || op == Opcodes.NEWARRAY) {
                sp = Math.max(0, sp - 1);
                stack = push(stack, sp++, false);
                continue;
            }
            if (op == Opcodes.CHECKCAST) {
                // A cast does not change the reference, so the tracked bit must survive
                // it. Treating the result as "other" loses the value and the walk then
                // reports the method unanalysable -- which is what it did for
                // (Iterator) casts before this case existed. Note ParparVM's CHECKCAST is
                // unchecked at runtime, but that is irrelevant here: this is about which
                // VALUE is on the stack, not whether the type is verified.
                continue;
            }
            if (op == Opcodes.DUP_X1) {
                // ..., v2, v1 -> ..., v1, v2, v1
                boolean v1 = sp > 0 && stack[sp - 1];
                boolean v2 = sp > 1 && stack[sp - 2];
                sp = Math.max(0, sp - 2);
                stack = push(stack, sp++, v1);
                stack = push(stack, sp++, v2);
                stack = push(stack, sp++, v1);
                continue;
            }
            if (op == Opcodes.DUP2) {
                // Long/double are one slot in this walk, so DUP2 of a wide value and of
                // two narrow values are the same operation here: duplicate the top two.
                boolean v1 = sp > 0 && stack[sp - 1];
                boolean v2 = sp > 1 && stack[sp - 2];
                stack = push(stack, sp++, v2);
                stack = push(stack, sp++, v1);
                continue;
            }
            if (op == Opcodes.SWAP) {
                boolean v1 = sp > 0 && stack[sp - 1];
                boolean v2 = sp > 1 && stack[sp - 2];
                if (sp > 1) {
                    stack[sp - 1] = v2;
                    stack[sp - 2] = v1;
                }
                continue;
            }
            if (op == Opcodes.POP) {
                sp = Math.max(0, sp - 1);
                continue;
            }
            if (op == Opcodes.ATHROW || op == Opcodes.MONITORENTER || op == Opcodes.MONITOREXIT) {
                // Throwing the tracked reference publishes it; the other two consume it
                // harmlessly but only for `this`, which is already covered elsewhere.
                boolean value = sp > 0 && stack[sp - 1];
                sp = Math.max(0, sp - 1);
                if (value && op == Opcodes.ATHROW) {
                    return ESCAPES;
                }
                continue;
            }
            // --- everything else --------------------------------------------------
            // Most instructions do not describe their stack effect (Instruction's default
            // returns null for both), so a table is needed before falling back. Without
            // it the walk bailed on ICONST_0 and on a plain ALOAD of a non-zero local --
            // it refused all 37 iterator classes on instructions that cannot leak
            // anything, which reads exactly like "nothing is eligible".
            int packed = simpleEffect(op);
            if (packed >= 0) {
                int pops = packed >> 8;
                int pushes = packed & 0xff;
                for (int a = 0; a < pops; a++) {
                    int idx = sp - 1 - a;
                    if (idx >= 0 && stack[idx]) {
                        lastReason = "consumed by op=" + op;
                        return UNKNOWN;
                    }
                }
                sp = Math.max(0, sp - pops);
                for (int o = 0; o < pushes; o++) {
                    stack = push(stack, sp++, false);
                }
                continue;
            }
            char[] in = i.getStackInputTypes();
            char[] out = i.getStackOutputTypes();
            if (in == null || out == null) {
                // An instruction whose stack effect is not described cannot be simulated.
                // If the tracked value is nowhere on the stack it also cannot be touched,
                // so the walk can continue; otherwise give up.
                for (int s = 0; s < sp; s++) {
                    if (stack[s]) {
                        lastReason = "undescribed stack effect, op=" + op + " ("
                                + i.getClass().getSimpleName() + ")";
                        return UNKNOWN;
                    }
                }
                sp = 0;
                continue;
            }
            for (int a = 0; a < in.length; a++) {
                int idx = sp - 1 - a;
                if (idx >= 0 && stack[idx]) {
                    lastReason = "consumed by unmodelled op=" + op + " ("
                            + i.getClass().getSimpleName() + ")";
                    return UNKNOWN;   // consumed by something unmodelled
                }
            }
            sp = Math.max(0, sp - in.length);
            for (int o = 0; o < out.length; o++) {
                stack = push(stack, sp++, false);
            }
        }
        // A method that never touches the reference cannot leak it. For the NEW form that
        // also means there was nothing to analyse, which the caller distinguishes.
        return SAFE;
    }

    /// TypeInstruction.appendInstruction mangles its type IN PLACE, so the same
    /// instruction reports "java/util/ArrayList" before codegen and "java_util_ArrayList"
    /// after. Normalising both sides makes the analysis independent of when it runs.
    static String mangle(String t) {
        if (t == null) {
            return null;
        }
        return t.replace('/', '_').replace('.', '_').replace('$', '_');
    }


    /// Stack effect of the opcodes that carry no operand reference, packed as
    /// (pops << 8) | pushes, or -1 when this table does not model the opcode.
    ///
    /// Every entry here is an instruction that cannot publish a reference: it either
    /// pushes a fresh primitive, moves a primitive, or consumes primitives. The tracked
    /// reference is still checked against the pop count, so an opcode that would swallow
    /// it (a mis-entry) still answers UNKNOWN rather than SAFE.
    ///
    /// Long and double are counted as ONE slot each, consistently with how this walk
    /// pushes values and with Invoke.getArgs().size(), which counts arguments rather than
    /// JVM stack words. The analysis only ever asks "is this slot the tracked reference",
    /// so slot width never enters the answer.
    private static int simpleEffect(int op) {
        if (op >= Opcodes.ACONST_NULL && op <= Opcodes.LDC) {
            return 0x0001;                      // constants: push one
        }
        if (op >= Opcodes.ILOAD && op <= Opcodes.ALOAD) {
            return 0x0001;                      // ALOAD 0 is handled before this point
        }
        if (op >= Opcodes.ISTORE && op <= Opcodes.DSTORE) {
            return 0x0100;                      // ASTORE is handled separately
        }
        if (op >= Opcodes.IALOAD && op <= Opcodes.SALOAD) {
            return 0x0201;                      // arrayref, index -> value
        }
        if (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE) {
            return 0x0300;                      // AASTORE is handled separately
        }
        switch (op) {
            case Opcodes.IINC: case Opcodes.NOP: case Opcodes.RETURN:
                return 0x0000;
            case Opcodes.POP2:
                return 0x0200;
            case Opcodes.ARRAYLENGTH: case Opcodes.INEG: case Opcodes.LNEG:
            case Opcodes.FNEG: case Opcodes.DNEG: case Opcodes.INSTANCEOF:
                return 0x0101;
            case Opcodes.IRETURN: case Opcodes.LRETURN: case Opcodes.FRETURN:
            case Opcodes.DRETURN:
                return 0x0100;
            default:
                break;
        }
        // Binary arithmetic, shifts, bitwise ops and the primitive comparisons all take
        // two and leave one; the numeric conversions take one and leave one.
        if ((op >= Opcodes.IADD && op <= Opcodes.LXOR)
                || (op >= Opcodes.LCMP && op <= Opcodes.DCMPG)) {
            return 0x0201;
        }
        if (op >= Opcodes.I2L && op <= Opcodes.I2S) {
            return 0x0101;
        }
        return -1;
    }

    private static boolean[] push(boolean[] stack, int sp, boolean v) {
        if (sp >= stack.length) {
            boolean[] n = new boolean[stack.length * 2 + 8];
            System.arraycopy(stack, 0, n, 0, stack.length);
            stack = n;
        }
        stack[sp] = v;
        return stack;
    }

    private static boolean isBranch(int op) {
        switch (op) {
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE:
            case Opcodes.IFGT: case Opcodes.IFLE: case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE: case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE: case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE: case Opcodes.GOTO: case Opcodes.JSR:
            case Opcodes.IFNULL: case Opcodes.IFNONNULL:
            case Opcodes.TABLESWITCH: case Opcodes.LOOKUPSWITCH:
                return true;
            default:
                return false;
        }
    }

    private static int branchPops(int op) {
        switch (op) {
            case Opcodes.GOTO: case Opcodes.JSR:
                return 0;
            case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE: case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE:
                return 2;
            default:
                return 1;
        }
    }
}
