/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.tools.translator;

import com.codename1.tools.translator.bytecodes.Instruction;
import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;
import com.codename1.tools.translator.classfile.analysis.Analyzer;
import com.codename1.tools.translator.classfile.analysis.AnalyzerException;
import com.codename1.tools.translator.classfile.analysis.Frame;
import com.codename1.tools.translator.classfile.analysis.SourceInterpreter;
import com.codename1.tools.translator.classfile.analysis.SourceValue;
import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import com.codename1.tools.translator.classfile.tree.InvokeDynamicInsnNode;
import com.codename1.tools.translator.classfile.tree.MethodInsnNode;
import com.codename1.tools.translator.classfile.tree.MethodNode;
import com.codename1.tools.translator.classfile.tree.MultiANewArrayInsnNode;
import com.codename1.tools.translator.classfile.tree.TypeInsnNode;
import java.util.IdentityHashMap;
import java.util.Map;

/// String concatenation for the JavaScript backend: a `StringBuilder` that provably
/// never leaves its method becomes a JavaScript string, held in a one-field box.
///
/// javac lowers every `a + b` to `new StringBuilder().append(a).append(b).toString()`.
/// Run as written, each step is a call into the runtime that keeps the builder's
/// count, capacity and width up to date, and the builder is a heap object of its own.
/// Fused, `new` is `{__sbv: ""}`, an append is `box.__sbv += text` and `toString` is a
/// Java String made from the box's text: V8 then scalar-replaces the box and the
/// whole chain is a native string concatenation (stringBuilding 1.55x, measured by
/// hand before this pass existed).
///
/// A box rather than a bare string, so that aliases need no tracking: `append`
/// returns its receiver, javac leaves that alias on the operand stack across the
/// branches of `"x" + (c ? a : b)`, and every copy of the reference -- on the stack,
/// in a local, through a `DUP` or a `SWAP` -- still names the same box, exactly as it
/// named the same builder.
///
/// A site is fused only when frame analysis proves every use of the builder, and of
/// every alias an `append` returns, is one of:
///
/// - the receiver of `<init>()`, `<init>(String)`, `append` of a `String`, `int`,
///   `char`, `boolean` or `long`, `toString()` or `length()`;
/// - a move that keeps it a reference to the same object (`DUP*`, `SWAP`, `POP*`,
///   `ASTORE`/`ALOAD`).
///
/// Anything else rejects the site: an argument to any call, a field or array store, a
/// return, a throw, a comparison, a monitor, a cast, an append whose text needs a
/// virtual `toString()` (which can suspend), and a value that MIGHT be the builder but
/// might also be something else (`b = c ? new StringBuilder() : other`). The proof is
/// all or nothing per site, and so is the emission: the generator fuses a method only
/// when every instruction recorded here is still in its body.
final class JavascriptBuilderFusion {
    static final int NEW = 1;
    static final int INIT_EMPTY = 2;
    static final int INIT_STRING = 3;
    static final int APPEND_STRING = 4;
    static final int APPEND_INT = 5;
    static final int APPEND_CHAR = 6;
    static final int APPEND_BOOLEAN = 7;
    static final int APPEND_LONG = 8;
    static final int TO_STRING = 9;
    static final int LENGTH = 10;

    private static final String SB = "java/lang/StringBuilder";
    private static final String SB_TYPE = "Ljava/lang/StringBuilder;";

    private JavascriptBuilderFusion() {
    }

    /// Copies keep the value they copy, so a builder reloaded from a local or
    /// duplicated on the stack is still recognised as that builder.
    private static final class Provenance extends SourceInterpreter {
        @Override
        public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value) {
            return value;
        }
    }

    /// The fused instructions of {@code code}, each with its kind, keyed by the
    /// {@link Instruction} {@code nodes} maps its node to; null when nothing fuses.
    static Map<Instruction, Integer> analyze(String owner, MethodNode code, Map<AbstractInsnNode, Instruction> nodes) {
        if (code == null || nodes.isEmpty()
                || (code.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) != 0) {
            return null;
        }
        int n = code.instructions.size();
        // site[i]: the builder site instruction i belongs to (its NEW, or an append
        // returning that builder), or -1.
        int[] site = new int[n];
        java.util.Arrays.fill(site, -1);
        int sites = 0;
        for (int i = 0; i < n; i++) {
            AbstractInsnNode insn = code.instructions.get(i);
            if (insn.getOpcode() == Opcodes.NEW && SB.equals(((TypeInsnNode) insn).desc) && nodes.containsKey(insn)) {
                site[i] = sites++;
            }
        }
        if (sites == 0) {
            return null;
        }
        Frame<SourceValue>[] frames;
        try {
            frames = new Analyzer<SourceValue>(new Provenance()).analyze(owner, code);
        } catch (AnalyzerException error) {
            return null;
        } catch (RuntimeException error) {
            return null;
        }
        Map<AbstractInsnNode, Integer> index = new IdentityHashMap<AbstractInsnNode, Integer>();
        for (int i = 0; i < n; i++) {
            index.put(code.instructions.get(i), i);
        }

        // An append returns its receiver: its result belongs to the receiver's site.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < n; i++) {
                AbstractInsnNode insn = code.instructions.get(i);
                if (site[i] >= 0 || frames[i] == null || appendKind(insn) == 0) {
                    continue;
                }
                MethodInsnNode m = (MethodInsnNode) insn;
                int receiver = siteOf(frames[i].getStack(frames[i].getStackSize() - 1 - argumentCount(m.desc)),
                        site, index);
                if (receiver >= 0) {
                    site[i] = receiver;
                    changed = true;
                }
            }
        }

        boolean[] rejected = new boolean[sites];
        int[] kind = new int[n];
        int[] siteOfCall = new int[n];
        java.util.Arrays.fill(siteOfCall, -1);
        for (int i = 0; i < n; i++) {
            Frame<SourceValue> frame = frames[i];
            if (frame == null) {
                continue;
            }
            AbstractInsnNode insn = code.instructions.get(i);
            int op = insn.getOpcode();
            if (op == Opcodes.NEW && site[i] >= 0) {
                kind[i] = NEW;
                siteOfCall[i] = site[i];
            }
            if (isMove(op)) {
                continue;
            }
            int consumed = consumedValues(insn, frame);
            if (consumed < 0) {
                // An instruction this table does not know: anything it could touch is rejected.
                consumed = frame.getStackSize();
            }
            int fusedKind = fusedKind(insn);
            for (int k = 0; k < consumed; k++) {
                int depth = frame.getStackSize() - 1 - k;
                if (depth < 0) {
                    break;
                }
                SourceValue value = frame.getStack(depth);
                // The receiver of a call is its deepest consumed operand.
                boolean receiverPosition = fusedKind != 0 && k == consumed - 1;
                markUses(value, site, index, rejected, receiverPosition);
                if (receiverPosition) {
                    int s = siteOf(value, site, index);
                    if (s >= 0) {
                        // site[] is provenance: only NEW and append (which returns its
                        // receiver) produce the builder. toString, length and <init>
                        // belong to the site without their results being it.
                        kind[i] = fusedKind;
                        siteOfCall[i] = s;
                    }
                }
            }
        }

        return collect(code, nodes, rejected, siteOfCall, kind);
    }


    /// The accepted instructions. Every fused instruction must be one the generator
    /// will see; a site with one that is not would leave a box in front of a real
    /// StringBuilder call, so it is rejected and the rest collected again.
    private static Map<Instruction, Integer> collect(MethodNode code, Map<AbstractInsnNode, Instruction> nodes,
            boolean[] rejected, int[] site, int[] kind) {
        while (true) {
            Map<Instruction, Integer> result = null;
            boolean retry = false;
            for (int i = 0; i < kind.length && !retry; i++) {
                if (kind[i] == 0 || site[i] < 0 || rejected[site[i]]) {
                    continue;
                }
                Instruction instruction = nodes.get(code.instructions.get(i));
                if (instruction == null) {
                    rejected[site[i]] = true;
                    retry = true;
                } else {
                    if (result == null) {
                        result = new IdentityHashMap<Instruction, Integer>();
                    }
                    result.put(instruction, kind[i]);
                }
            }
            if (!retry) {
                return result;
            }
        }
    }

    /// The single site every source of {@code value} belongs to, or -1 when any source
    /// is not a builder or the sources name two different builders.
    private static int siteOf(SourceValue value, int[] site, Map<AbstractInsnNode, Integer> index) {
        if (value == null || value.insns == null || value.insns.isEmpty()) {
            return -1;
        }
        int found = -1;
        for (AbstractInsnNode source : value.insns) {
            Integer i = index.get(source);
            if (i == null || site[i] < 0) {
                return -1;
            }
            if (found >= 0 && found != site[i]) {
                return -1;
            }
            found = site[i];
        }
        return found;
    }

    /// Rejects every builder among {@code value}'s sources unless this use is a fused
    /// receiver of a value that is purely one builder.
    private static void markUses(SourceValue value, int[] site, Map<AbstractInsnNode, Integer> index,
            boolean[] rejected, boolean receiverPosition) {
        if (value == null || value.insns == null) {
            return;
        }
        boolean pure = receiverPosition && siteOf(value, site, index) >= 0;
        if (pure) {
            return;
        }
        for (AbstractInsnNode source : value.insns) {
            Integer i = index.get(source);
            if (i != null && site[i] >= 0) {
                rejected[site[i]] = true;
            }
        }
    }

    private static boolean isMove(int op) {
        switch (op) {
            case Opcodes.DUP: case Opcodes.DUP_X1: case Opcodes.DUP_X2:
            case Opcodes.DUP2: case Opcodes.DUP2_X1: case Opcodes.DUP2_X2:
            case Opcodes.SWAP: case Opcodes.POP: case Opcodes.POP2: case Opcodes.ASTORE:
                return true;
            default:
                return false;
        }
    }

    private static int appendKind(AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL || !(insn instanceof MethodInsnNode)) {
            return 0;
        }
        MethodInsnNode m = (MethodInsnNode) insn;
        if (!SB.equals(m.owner) || !"append".equals(m.name)) {
            return 0;
        }
        if (("(Ljava/lang/String;)" + SB_TYPE).equals(m.desc)) {
            return APPEND_STRING;
        }
        if (("(I)" + SB_TYPE).equals(m.desc)) {
            return APPEND_INT;
        }
        if (("(C)" + SB_TYPE).equals(m.desc)) {
            return APPEND_CHAR;
        }
        if (("(Z)" + SB_TYPE).equals(m.desc)) {
            return APPEND_BOOLEAN;
        }
        if (("(J)" + SB_TYPE).equals(m.desc)) {
            return APPEND_LONG;
        }
        return 0;
    }

    /// The fused form of {@code insn} when its receiver is a fused builder, or 0.
    private static int fusedKind(AbstractInsnNode insn) {
        int append = appendKind(insn);
        if (append != 0) {
            return append;
        }
        if (!(insn instanceof MethodInsnNode)) {
            return 0;
        }
        MethodInsnNode m = (MethodInsnNode) insn;
        if (!SB.equals(m.owner)) {
            return 0;
        }
        if (insn.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(m.name)) {
            if ("()V".equals(m.desc)) {
                return INIT_EMPTY;
            }
            if ("(Ljava/lang/String;)V".equals(m.desc)) {
                return INIT_STRING;
            }
            return 0;
        }
        if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
            if ("toString".equals(m.name) && "()Ljava/lang/String;".equals(m.desc)) {
                return TO_STRING;
            }
            if ("length".equals(m.name) && "()I".equals(m.desc)) {
                return LENGTH;
            }
        }
        return 0;
    }

    private static int argumentCount(String desc) {
        return Type.getArgumentTypes(desc).length;
    }

    /// How many operand stack VALUES {@code insn} consumes (a long is one value), or
    /// -1 for an opcode this table does not cover.
    private static int consumedValues(AbstractInsnNode insn, Frame<SourceValue> frame) {
        int op = insn.getOpcode();
        if (op < 0) {
            return 0; // labels, line numbers, frames
        }
        if (insn instanceof MethodInsnNode) {
            return argumentCount(((MethodInsnNode) insn).desc) + (op == Opcodes.INVOKESTATIC ? 0 : 1);
        }
        if (insn instanceof InvokeDynamicInsnNode) {
            return argumentCount(((InvokeDynamicInsnNode) insn).desc);
        }
        if (insn instanceof MultiANewArrayInsnNode) {
            return ((MultiANewArrayInsnNode) insn).dims;
        }
        if (op <= Opcodes.ALOAD || op == Opcodes.GETSTATIC || op == Opcodes.NEW || op == Opcodes.GOTO
                || op == Opcodes.JSR || op == Opcodes.RET || op == Opcodes.RETURN || op == Opcodes.IINC) {
            // NOP, constants, BIPUSH/SIPUSH, LDC and the loads all sit at or below ALOAD.
            return 0;
        }
        if (op >= Opcodes.IALOAD && op <= Opcodes.SALOAD) {
            return 2;
        }
        if (op >= Opcodes.ISTORE && op <= Opcodes.ASTORE) {
            return 1;
        }
        if (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE) {
            return 3;
        }
        if (op >= Opcodes.IADD && op <= Opcodes.DREM) {
            return 2;
        }
        if (op >= Opcodes.INEG && op <= Opcodes.DNEG) {
            return 1;
        }
        if (op >= Opcodes.ISHL && op <= Opcodes.LXOR) {
            return 2;
        }
        if (op >= Opcodes.I2L && op <= Opcodes.I2S) {
            return 1;
        }
        if (op >= Opcodes.LCMP && op <= Opcodes.DCMPG) {
            return 2;
        }
        if (op >= Opcodes.IFEQ && op <= Opcodes.IFLE) {
            return 1;
        }
        if (op >= Opcodes.IF_ICMPEQ && op <= Opcodes.IF_ACMPNE) {
            return 2;
        }
        switch (op) {
            case Opcodes.TABLESWITCH: case Opcodes.LOOKUPSWITCH:
            case Opcodes.IRETURN: case Opcodes.LRETURN: case Opcodes.FRETURN:
            case Opcodes.DRETURN: case Opcodes.ARETURN:
            case Opcodes.PUTSTATIC: case Opcodes.GETFIELD:
            case Opcodes.NEWARRAY: case Opcodes.ANEWARRAY: case Opcodes.ARRAYLENGTH:
            case Opcodes.ATHROW: case Opcodes.CHECKCAST: case Opcodes.INSTANCEOF:
            case Opcodes.MONITORENTER: case Opcodes.MONITOREXIT:
            case Opcodes.IFNULL: case Opcodes.IFNONNULL:
                return 1;
            case Opcodes.PUTFIELD:
                return 2;
            default:
                return -1;
        }
    }
}
