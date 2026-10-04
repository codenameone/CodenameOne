// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
//
// Codename One modifications Copyright (c) 2026, Codename One and/or its
// affiliates, distributed under the license above.
//
// This file plays the role of ASM's org.objectweb.asm.commons.JSRInlinerAdapter for ParparVM: it inlines
// JSR/RET subroutines over this package's tree API, which follows ASM's API and
// design (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile.tree;

import com.codename1.tools.translator.classfile.AnnotationVisitor;
import com.codename1.tools.translator.classfile.Label;
import com.codename1.tools.translator.classfile.MethodVisitor;
import com.codename1.tools.translator.classfile.Opcodes;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Removes {@code JSR}/{@code RET} subroutines -- the way compilers targeting class
 * files before version 50 implemented {@code finally} -- by giving every call site
 * its own copy of the subroutine.
 *
 * <p>A method's code is buffered and replayed to the delegate at {@code visitEnd};
 * everything that precedes the code (parameters, annotations) is forwarded as it
 * arrives, which is the order it would have been replayed in anyway. Code without
 * a {@code JSR} is replayed unchanged.
 *
 * <p>With one, the code is re-emitted once per <em>instantiation</em>: the main body,
 * then one copy of a subroutine per {@code JSR} that reaches it, breadth first. Each
 * {@code JSR} becomes {@code ACONST_NULL; GOTO copy} followed by the copy's return
 * label -- the null stands in for the return address, so nothing that stores it
 * needs rewriting -- and each {@code RET} becomes a {@code GOTO} to the return
 * label of the instantiation that owns it. Every instantiation re-emits every label
 * (runs of labels with no emitted instruction between them collapse into one), and
 * the exception table and local variables are re-emitted per instantiation for
 * every range that is not empty in it. This exact layout is what the translator's
 * output for such methods is built from, so it must not drift.
 */
public class JsrInliner extends MethodNode {
    private final MethodVisitor delegate;
    private boolean sawCode;
    private boolean hasJsr;
    /** Instructions of each subroutine, keyed by its entry label, in first-JSR order. */
    private final Map<LabelNode, BitSet> subroutines = new LinkedHashMap<LabelNode, BitSet>();
    private final BitSet mainInsns = new BitSet();
    /** Instructions reachable from more than one subroutine (or the main body). */
    private final BitSet sharedInsns = new BitSet();

    public JsrInliner(MethodVisitor delegate, int access, String name, String desc) {
        super(access, name, desc);
        this.delegate = delegate;
    }

    @Override
    public void visitParameter(String name, int access) {
        delegate.visitParameter(name, access);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return delegate.visitAnnotationDefault();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return delegate.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return delegate.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitCode() {
        sawCode = true;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        LabelNode target = ((JumpInsnNode) instructions.getLast()).label;
        if (opcode == Opcodes.JSR) {
            hasJsr = true;
            if (!subroutines.containsKey(target)) {
                subroutines.put(target, new BitSet());
            }
        }
    }

    @Override
    public void visitEnd() {
        if (hasJsr) {
            findSubroutines();
            emitCode();
        }
        if (sawCode) {
            replay();
        }
        delegate.visitEnd();
    }

    // ------------------------------------------------------------------ subroutine discovery

    private void findSubroutines() {
        BitSet visited = new BitSet();
        findSubroutine(0, mainInsns, visited);
        for (Map.Entry<LabelNode, BitSet> entry : subroutines.entrySet()) {
            findSubroutine(instructions.indexOf(entry.getKey()), entry.getValue(), visited);
        }
    }

    /** The instructions reachable from a start, plus every handler whose range touches them. */
    private void findSubroutine(int start, BitSet insns, BitSet visited) {
        findReachable(start, insns, visited);
        while (true) {
            boolean found = false;
            for (TryCatchBlockNode block : tryCatchBlocks) {
                int handler = instructions.indexOf(block.handler);
                if (insns.get(handler)) {
                    continue;
                }
                int from = instructions.indexOf(block.start);
                int to = instructions.indexOf(block.end);
                int first = insns.nextSetBit(from);
                if (first >= from && first < to) {
                    findReachable(handler, insns, visited);
                    found = true;
                }
            }
            if (!found) {
                return;
            }
        }
    }

    /** Follows control flow without entering subroutines: a JSR falls through. */
    private void findReachable(int index, BitSet insns, BitSet visited) {
        int i = index;
        while (i < instructions.size()) {
            if (insns.get(i)) {
                return;
            }
            insns.set(i);
            if (visited.get(i)) {
                sharedInsns.set(i);
            }
            visited.set(i);
            AbstractInsnNode node = instructions.get(i);
            if (node.getType() == AbstractInsnNode.JUMP_INSN && node.getOpcode() != Opcodes.JSR) {
                findReachable(instructions.indexOf(((JumpInsnNode) node).label), insns, visited);
            } else if (node.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
                TableSwitchInsnNode sw = (TableSwitchInsnNode) node;
                findReachable(instructions.indexOf(sw.dflt), insns, visited);
                for (int k = sw.labels.size() - 1; k >= 0; k--) {
                    findReachable(instructions.indexOf(sw.labels.get(k)), insns, visited);
                }
            } else if (node.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
                LookupSwitchInsnNode sw = (LookupSwitchInsnNode) node;
                findReachable(instructions.indexOf(sw.dflt), insns, visited);
                for (int k = sw.labels.size() - 1; k >= 0; k--) {
                    findReachable(instructions.indexOf(sw.labels.get(k)), insns, visited);
                }
            }
            switch (node.getOpcode()) {
                case Opcodes.GOTO: case Opcodes.RET: case Opcodes.TABLESWITCH: case Opcodes.LOOKUPSWITCH:
                case Opcodes.IRETURN: case Opcodes.LRETURN: case Opcodes.FRETURN: case Opcodes.DRETURN:
                case Opcodes.ARETURN: case Opcodes.RETURN: case Opcodes.ATHROW:
                    return;
                default:
                    i++;
            }
        }
    }

    // ------------------------------------------------------------------ emission

    private final class Instantiation {
        final Instantiation parent;
        final BitSet insns;
        final Map<LabelNode, LabelNode> cloned = new HashMap<LabelNode, LabelNode>();
        final LabelNode returnLabel;

        Instantiation(Instantiation parent, BitSet insns) {
            for (Instantiation p = parent; p != null; p = p.parent) {
                if (p.insns == insns) {
                    throw new IllegalArgumentException("Recursive invocation of a subroutine");
                }
            }
            this.parent = parent;
            this.insns = insns;
            this.returnLabel = parent == null ? null : new LabelNode(new Label());
            // Clone every label; consecutive labels with no instruction of ours between
            // them share one clone.
            LabelNode clone = null;
            for (int i = 0; i < instructions.size(); i++) {
                AbstractInsnNode node = instructions.get(i);
                if (node.getType() == AbstractInsnNode.LABEL) {
                    if (clone == null) {
                        clone = new LabelNode(new Label());
                    }
                    cloned.put((LabelNode) node, clone);
                } else if (owner(i) == this) {
                    clone = null;
                }
            }
        }

        /** The outermost instantiation that emits instruction i, or null if none here does. */
        Instantiation owner(int i) {
            if (!insns.get(i)) {
                return null;
            }
            if (!sharedInsns.get(i)) {
                return this;
            }
            Instantiation owner = this;
            for (Instantiation p = parent; p != null; p = p.parent) {
                if (p.insns.get(i)) {
                    owner = p;
                }
            }
            return owner;
        }

        LabelNode jumpTarget(LabelNode label) {
            return owner(instructions.indexOf(label)).cloned.get(label);
        }

        LabelNode label(LabelNode label) {
            return cloned.get(label);
        }
    }

    private void emitCode() {
        LinkedList<Instantiation> worklist = new LinkedList<Instantiation>();
        worklist.add(new Instantiation(null, mainInsns));
        InsnList newInsns = new InsnList();
        List<TryCatchBlockNode> newBlocks = new ArrayList<TryCatchBlockNode>();
        List<LocalVariableNode> newLocals = new ArrayList<LocalVariableNode>();
        while (!worklist.isEmpty()) {
            emit(worklist.removeFirst(), worklist, newInsns, newBlocks, newLocals);
        }
        instructions = newInsns;
        tryCatchBlocks = newBlocks;
        localVariables = newLocals;
    }

    private void emit(Instantiation inst, LinkedList<Instantiation> worklist, InsnList out,
            List<TryCatchBlockNode> blocks, List<LocalVariableNode> locals) {
        LabelNode previous = null;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode node = instructions.get(i);
            if (node.getType() == AbstractInsnNode.LABEL) {
                LabelNode clone = inst.label((LabelNode) node);
                if (clone != previous) {
                    out.add(clone);
                    previous = clone;
                }
            } else if (inst.owner(i) == inst) {
                if (node.getOpcode() == Opcodes.RET) {
                    LabelNode ret = null;
                    for (Instantiation p = inst; p != null; p = p.parent) {
                        if (p.insns.get(i)) {
                            ret = p.returnLabel;
                        }
                    }
                    if (ret == null) {
                        throw new IllegalArgumentException("Instruction #" + i + " is a RET not owned by any subroutine");
                    }
                    out.add(new JumpInsnNode(Opcodes.GOTO, ret));
                } else if (node.getOpcode() == Opcodes.JSR) {
                    LabelNode entry = ((JumpInsnNode) node).label;
                    Instantiation sub = new Instantiation(inst, subroutines.get(entry));
                    LabelNode target = sub.jumpTarget(entry);
                    out.add(new InsnNode(Opcodes.ACONST_NULL));
                    out.add(new JumpInsnNode(Opcodes.GOTO, target));
                    out.add(sub.returnLabel);
                    worklist.add(sub);
                } else {
                    out.add(copy(node, inst));
                }
            }
        }
        for (TryCatchBlockNode block : tryCatchBlocks) {
            LabelNode start = inst.label(block.start);
            LabelNode end = inst.label(block.end);
            if (start != end) {
                LabelNode handler = inst.jumpTarget(block.handler);
                if (start == null || end == null || handler == null) {
                    throw new IllegalStateException("Unmapped exception range in " + name + desc);
                }
                blocks.add(new TryCatchBlockNode(start, end, handler, block.type));
            }
        }
        for (LocalVariableNode local : localVariables) {
            LabelNode start = inst.label(local.start);
            LabelNode end = inst.label(local.end);
            if (start != end) {
                locals.add(new LocalVariableNode(local.name, local.desc, local.signature, start, end, local.index));
            }
        }
    }

    private static List<LabelNode> jumpTargets(List<LabelNode> labels, Instantiation inst) {
        List<LabelNode> out = new ArrayList<LabelNode>(labels.size());
        for (LabelNode l : labels) {
            out.add(inst.jumpTarget(l));
        }
        return out;
    }

    private static AbstractInsnNode copy(AbstractInsnNode node, Instantiation inst) {
        switch (node.getType()) {
            case AbstractInsnNode.INSN:
                return new InsnNode(node.getOpcode());
            case AbstractInsnNode.INT_INSN:
                return new IntInsnNode(node.getOpcode(), ((IntInsnNode) node).operand);
            case AbstractInsnNode.VAR_INSN:
                return new VarInsnNode(node.getOpcode(), ((VarInsnNode) node).var);
            case AbstractInsnNode.TYPE_INSN:
                return new TypeInsnNode(node.getOpcode(), ((TypeInsnNode) node).desc);
            case AbstractInsnNode.FIELD_INSN: {
                FieldInsnNode f = (FieldInsnNode) node;
                return new FieldInsnNode(f.getOpcode(), f.owner, f.name, f.desc);
            }
            case AbstractInsnNode.METHOD_INSN: {
                MethodInsnNode m = (MethodInsnNode) node;
                return new MethodInsnNode(m.getOpcode(), m.owner, m.name, m.desc, m.itf);
            }
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {
                InvokeDynamicInsnNode d = (InvokeDynamicInsnNode) node;
                return new InvokeDynamicInsnNode(d.name, d.desc, d.bsm, d.bsmArgs);
            }
            case AbstractInsnNode.JUMP_INSN:
                return new JumpInsnNode(node.getOpcode(), inst.jumpTarget(((JumpInsnNode) node).label));
            case AbstractInsnNode.LDC_INSN:
                return new LdcInsnNode(((LdcInsnNode) node).cst);
            case AbstractInsnNode.IINC_INSN:
                return new IincInsnNode(((IincInsnNode) node).var, ((IincInsnNode) node).incr);
            case AbstractInsnNode.TABLESWITCH_INSN: {
                TableSwitchInsnNode t = (TableSwitchInsnNode) node;
                return new TableSwitchInsnNode(t.min, t.max, inst.jumpTarget(t.dflt), jumpTargets(t.labels, inst));
            }
            case AbstractInsnNode.LOOKUPSWITCH_INSN: {
                LookupSwitchInsnNode l = (LookupSwitchInsnNode) node;
                return new LookupSwitchInsnNode(inst.jumpTarget(l.dflt), l.keys, jumpTargets(l.labels, inst));
            }
            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                return new MultiANewArrayInsnNode(((MultiANewArrayInsnNode) node).desc, ((MultiANewArrayInsnNode) node).dims);
            case AbstractInsnNode.LINE:
                return new LineNumberNode(((LineNumberNode) node).line, inst.jumpTarget(((LineNumberNode) node).start));
            default:
                throw new IllegalArgumentException("Cannot copy node type " + node.getType());
        }
    }

    // ------------------------------------------------------------------ replay

    private static Label[] labels(List<LabelNode> nodes) {
        Label[] out = new Label[nodes.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = nodes.get(i).getLabel();
        }
        return out;
    }

    private void replay() {
        MethodVisitor d = delegate;
        d.visitCode();
        for (TryCatchBlockNode block : tryCatchBlocks) {
            d.visitTryCatchBlock(block.start.getLabel(), block.end.getLabel(), block.handler.getLabel(), block.type);
        }
        for (AbstractInsnNode node = instructions.getFirst(); node != null; node = node.getNext()) {
            int op = node.getOpcode();
            switch (node.getType()) {
                case AbstractInsnNode.INSN:
                    d.visitInsn(op);
                    break;
                case AbstractInsnNode.INT_INSN:
                    d.visitIntInsn(op, ((IntInsnNode) node).operand);
                    break;
                case AbstractInsnNode.VAR_INSN:
                    d.visitVarInsn(op, ((VarInsnNode) node).var);
                    break;
                case AbstractInsnNode.TYPE_INSN:
                    d.visitTypeInsn(op, ((TypeInsnNode) node).desc);
                    break;
                case AbstractInsnNode.FIELD_INSN: {
                    FieldInsnNode f = (FieldInsnNode) node;
                    d.visitFieldInsn(op, f.owner, f.name, f.desc);
                    break;
                }
                case AbstractInsnNode.METHOD_INSN: {
                    MethodInsnNode m = (MethodInsnNode) node;
                    d.visitMethodInsn(op, m.owner, m.name, m.desc, m.itf);
                    break;
                }
                case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {
                    InvokeDynamicInsnNode i = (InvokeDynamicInsnNode) node;
                    d.visitInvokeDynamicInsn(i.name, i.desc, i.bsm, i.bsmArgs);
                    break;
                }
                case AbstractInsnNode.JUMP_INSN:
                    d.visitJumpInsn(op, ((JumpInsnNode) node).label.getLabel());
                    break;
                case AbstractInsnNode.LABEL:
                    d.visitLabel(((LabelNode) node).getLabel());
                    break;
                case AbstractInsnNode.LDC_INSN:
                    d.visitLdcInsn(((LdcInsnNode) node).cst);
                    break;
                case AbstractInsnNode.IINC_INSN:
                    d.visitIincInsn(((IincInsnNode) node).var, ((IincInsnNode) node).incr);
                    break;
                case AbstractInsnNode.TABLESWITCH_INSN: {
                    TableSwitchInsnNode t = (TableSwitchInsnNode) node;
                    d.visitTableSwitchInsn(t.min, t.max, t.dflt.getLabel(), labels(t.labels));
                    break;
                }
                case AbstractInsnNode.LOOKUPSWITCH_INSN: {
                    LookupSwitchInsnNode l = (LookupSwitchInsnNode) node;
                    d.visitLookupSwitchInsn(l.dflt.getLabel(), l.keys, labels(l.labels));
                    break;
                }
                case AbstractInsnNode.MULTIANEWARRAY_INSN:
                    d.visitMultiANewArrayInsn(((MultiANewArrayInsnNode) node).desc, ((MultiANewArrayInsnNode) node).dims);
                    break;
                case AbstractInsnNode.LINE:
                    d.visitLineNumber(((LineNumberNode) node).line, ((LineNumberNode) node).start.getLabel());
                    break;
                default:
                    throw new IllegalStateException("Unknown node type " + node.getType());
            }
        }
        for (LocalVariableNode local : localVariables) {
            d.visitLocalVariable(local.name, local.desc, local.signature, local.start.getLabel(), local.end.getLabel(),
                    local.index);
        }
        d.visitMaxs(maxStack, maxLocals);
    }
}
