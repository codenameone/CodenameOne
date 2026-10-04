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
// This file is a rewrite of ASM's org.objectweb.asm.tree.MethodNode for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile.tree;

import com.codename1.tools.translator.classfile.Handle;
import com.codename1.tools.translator.classfile.Label;
import com.codename1.tools.translator.classfile.MethodVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A method's code recorded as a list of nodes, for the analyses that need random
 * access and a fixed point over it. Labels map to one {@link LabelNode} each, by
 * identity, so the same {@link Label} the reader created is always reachable from
 * its node.
 */
public class MethodNode extends MethodVisitor {
    public int access;
    public String name;
    public String desc;
    public InsnList instructions = new InsnList();
    public List<TryCatchBlockNode> tryCatchBlocks = new ArrayList<TryCatchBlockNode>();
    public List<LocalVariableNode> localVariables = new ArrayList<LocalVariableNode>();
    public int maxStack;
    public int maxLocals;
    private final Map<Label, LabelNode> labelNodes = new IdentityHashMap<Label, LabelNode>();

    public MethodNode(int access, String name, String desc) {
        this.access = access;
        this.name = name;
        this.desc = desc;
    }

    protected LabelNode getLabelNode(Label label) {
        LabelNode node = labelNodes.get(label);
        if (node == null) {
            node = new LabelNode(label);
            labelNodes.put(label, node);
        }
        return node;
    }

    private List<LabelNode> labelNodes(Label[] labels) {
        LabelNode[] nodes = new LabelNode[labels.length];
        for (int i = 0; i < labels.length; i++) {
            nodes[i] = getLabelNode(labels[i]);
        }
        return Arrays.asList(nodes);
    }

    @Override
    public void visitInsn(int opcode) {
        instructions.add(new InsnNode(opcode));
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        instructions.add(new IntInsnNode(opcode, operand));
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        instructions.add(new VarInsnNode(opcode, var));
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        instructions.add(new TypeInsnNode(opcode, type));
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        instructions.add(new FieldInsnNode(opcode, owner, name, desc));
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        instructions.add(new MethodInsnNode(opcode, owner, name, desc, itf));
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        instructions.add(new InvokeDynamicInsnNode(name, desc, bsm, bsmArgs));
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        instructions.add(new JumpInsnNode(opcode, getLabelNode(label)));
    }

    @Override
    public void visitLabel(Label label) {
        instructions.add(getLabelNode(label));
    }

    @Override
    public void visitLdcInsn(Object value) {
        instructions.add(new LdcInsnNode(value));
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        instructions.add(new IincInsnNode(var, increment));
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        instructions.add(new TableSwitchInsnNode(min, max, getLabelNode(dflt), labelNodes(labels)));
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        instructions.add(new LookupSwitchInsnNode(getLabelNode(dflt), keys, labelNodes(labels)));
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        instructions.add(new MultiANewArrayInsnNode(desc, dims));
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        tryCatchBlocks.add(new TryCatchBlockNode(getLabelNode(start), getLabelNode(end), getLabelNode(handler), type));
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        localVariables.add(new LocalVariableNode(name, desc, signature, getLabelNode(start), getLabelNode(end), index));
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        instructions.add(new LineNumberNode(line, getLabelNode(start)));
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
    }
}
