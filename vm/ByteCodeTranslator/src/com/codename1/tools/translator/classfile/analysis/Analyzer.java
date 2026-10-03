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
package com.codename1.tools.translator.classfile.analysis;

import com.codename1.tools.translator.classfile.Opcodes;
import com.codename1.tools.translator.classfile.Type;
import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import com.codename1.tools.translator.classfile.tree.InsnList;
import com.codename1.tools.translator.classfile.tree.JumpInsnNode;
import com.codename1.tools.translator.classfile.tree.LabelNode;
import com.codename1.tools.translator.classfile.tree.LookupSwitchInsnNode;
import com.codename1.tools.translator.classfile.tree.MethodNode;
import com.codename1.tools.translator.classfile.tree.TableSwitchInsnNode;
import com.codename1.tools.translator.classfile.tree.TryCatchBlockNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the {@link Frame} before every node of a method by running its
 * instructions to a fixed point under an {@link Interpreter}. The frame of a node
 * no path reaches is null.
 *
 * <p>Instructions are processed from a LIFO worklist and a node is re-queued only
 * when a merge changes its frame. An exception handler receives the frame from
 * BEFORE each instruction its range covers, with the stack replaced by the caught
 * exception. Subroutines ({@code JSR}/{@code RET}) are rejected.
 */
public class Analyzer<V extends Value> {
    private final Interpreter<V> interpreter;
    private InsnList insns;
    private int size;
    private List<TryCatchBlockNode>[] handlers;
    private Frame<V>[] frames;
    private boolean[] queued;
    private int[] queue;
    private int top;

    @SuppressWarnings("unchecked")
    public Analyzer(Interpreter<V> interpreter) {
        this.interpreter = interpreter;
        // Sized per method by analyze(); empty until then.
        this.insns = new InsnList();
        this.handlers = (List<TryCatchBlockNode>[]) new List<?>[0];
        this.frames = (Frame<V>[]) new Frame<?>[0];
        this.queued = new boolean[0];
        this.queue = new int[0];
    }

    @SuppressWarnings("unchecked")
    public Frame<V>[] analyze(String owner, MethodNode method) throws AnalyzerException {
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            frames = (Frame<V>[]) new Frame<?>[0];
            return frames;
        }
        insns = method.instructions;
        size = insns.size();
        handlers = (List<TryCatchBlockNode>[]) new List<?>[size];
        frames = (Frame<V>[]) new Frame<?>[size];
        queued = new boolean[size];
        queue = new int[size];
        top = 0;

        for (AbstractInsnNode n = insns.getFirst(); n != null; n = n.getNext()) {
            if (n.getOpcode() == Opcodes.JSR || n.getOpcode() == Opcodes.RET) {
                throw new AnalyzerException(n, "JSR/RET subroutines are not supported");
            }
        }
        for (TryCatchBlockNode block : method.tryCatchBlocks) {
            int start = insns.indexOf(block.start);
            int end = insns.indexOf(block.end);
            for (int j = start; j < end; j++) {
                List<TryCatchBlockNode> list = handlers[j];
                if (list == null) {
                    list = new ArrayList<TryCatchBlockNode>();
                    handlers[j] = list;
                }
                list.add(block);
            }
        }

        merge(0, initialFrame(owner, method));

        Frame<V> current = new Frame<V>(method.maxLocals, method.maxStack);
        while (top > 0) {
            int index = queue[--top];
            Frame<V> before = frames[index];
            queued[index] = false;
            AbstractInsnNode node = null;
            try {
                node = insns.get(index);
                int opcode = node.getOpcode();
                int type = node.getType();
                if (type == AbstractInsnNode.LABEL || type == AbstractInsnNode.LINE) {
                    merge(index + 1, before);
                } else {
                    current.init(before).execute(node, interpreter);
                    if (node instanceof JumpInsnNode) {
                        if (opcode != Opcodes.GOTO) {
                            merge(index + 1, current);
                        }
                        merge(insns.indexOf(((JumpInsnNode) node).label), current);
                    } else if (node instanceof LookupSwitchInsnNode) {
                        LookupSwitchInsnNode sw = (LookupSwitchInsnNode) node;
                        merge(insns.indexOf(sw.dflt), current);
                        for (LabelNode target : sw.labels) {
                            merge(insns.indexOf(target), current);
                        }
                    } else if (node instanceof TableSwitchInsnNode) {
                        TableSwitchInsnNode sw = (TableSwitchInsnNode) node;
                        merge(insns.indexOf(sw.dflt), current);
                        for (LabelNode target : sw.labels) {
                            merge(insns.indexOf(target), current);
                        }
                    } else if (opcode != Opcodes.ATHROW && (opcode < Opcodes.IRETURN || opcode > Opcodes.RETURN)) {
                        merge(index + 1, current);
                    }
                }
                List<TryCatchBlockNode> covering = handlers[index];
                if (covering != null) {
                    for (TryCatchBlockNode block : covering) {
                        Type catchType = Type.getObjectType(block.type == null ? "java/lang/Throwable" : block.type);
                        Frame<V> handler = new Frame<V>(before);
                        handler.clearStack();
                        handler.push(interpreter.newExceptionValue(block, handler, catchType));
                        merge(insns.indexOf(block.handler), handler);
                    }
                }
            } catch (AnalyzerException e) {
                throw new AnalyzerException(e.node, "Error at instruction " + index + ": " + e.getMessage(), e);
            } catch (RuntimeException e) {
                throw new AnalyzerException(node, "Error at instruction " + index + ": " + e.getMessage(), e);
            }
        }
        return frames;
    }

    private Frame<V> initialFrame(String owner, MethodNode method) {
        Frame<V> frame = new Frame<V>(method.maxLocals, method.maxStack);
        int local = 0;
        boolean instance = (method.access & Opcodes.ACC_STATIC) == 0;
        if (instance) {
            frame.setLocal(local, interpreter.newParameterValue(true, local, Type.getObjectType(owner)));
            local++;
        }
        for (Type arg : Type.getArgumentTypes(method.desc)) {
            frame.setLocal(local, interpreter.newParameterValue(instance, local, arg));
            local++;
            if (arg.getSize() == 2) {
                frame.setLocal(local, interpreter.newEmptyValue(local));
                local++;
            }
        }
        while (local < method.maxLocals) {
            frame.setLocal(local, interpreter.newEmptyValue(local));
            local++;
        }
        frame.setReturn(interpreter.newReturnTypeValue(Type.getReturnType(method.desc)));
        return frame;
    }

    private void merge(int index, Frame<V> frame) throws AnalyzerException {
        if (index >= size) {
            throw new AnalyzerException(null, "Execution can fall off the end of the code");
        }
        boolean changed;
        Frame<V> old = frames[index];
        if (old == null) {
            frames[index] = new Frame<V>(frame);
            changed = true;
        } else {
            changed = old.merge(frame, interpreter);
        }
        if (changed && !queued[index]) {
            queued[index] = true;
            queue[top++] = index;
        }
    }
}
