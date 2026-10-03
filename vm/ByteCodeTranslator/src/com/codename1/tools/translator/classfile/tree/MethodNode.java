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
