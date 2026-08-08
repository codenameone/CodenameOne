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
package com.codename1.hardening;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

/**
 * Conservative UPPER-BOUND estimate of a method's encoded {@code Code} size in bytes, so a transform
 * can refuse to grow a method past the JVM's 65,535-byte limit (which makes ASM throw
 * {@code MethodTooLargeException} at write time and abort an otherwise-valid build). Every instruction
 * is charged its widest legal encoding, so the estimate never undercounts -- a transform that stops
 * short of {@link #SAFE_LIMIT} is safe even though ASM might have squeezed a little more in.
 */
final class MethodSize {

    /** The hard JVM limit on a method's bytecode array. */
    static final int LIMIT = 65535;
    /**
     * The size a transform must stay under. Below {@link #LIMIT} by a margin that absorbs both the
     * upper-bound estimate's slack and the fact that the real limit is on the emitted bytes, which
     * COMPUTE_MAXS/FRAMES can shift slightly.
     */
    static final int SAFE_LIMIT = 60000;

    private MethodSize() {
    }

    /** Upper-bound encoded byte size of {@code m}'s instructions, or 0 when it has none. */
    static int estimateBytes(MethodNode m) {
        return m == null || m.instructions == null ? 0 : estimateBytes(m.instructions);
    }

    /** Upper-bound encoded byte size of an instruction list. */
    static int estimateBytes(InsnList insns) {
        int total = 0;
        for (AbstractInsnNode n = insns.getFirst(); n != null; n = n.getNext()) {
            total += estimateBytes(n);
        }
        return total;
    }

    /** Upper-bound encoded byte size of a single instruction node (0 for labels/line/frame metadata). */
    static int estimateBytes(AbstractInsnNode n) {
        switch (n.getType()) {
            case AbstractInsnNode.LABEL:
            case AbstractInsnNode.LINE:
            case AbstractInsnNode.FRAME:
                return 0;
            case AbstractInsnNode.INSN:
                return 1;
            case AbstractInsnNode.INT_INSN:
                return 3;
            case AbstractInsnNode.VAR_INSN:
                return 4;
            case AbstractInsnNode.TYPE_INSN:
                return 3;
            case AbstractInsnNode.FIELD_INSN:
                return 3;
            case AbstractInsnNode.METHOD_INSN:
                return 5;
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                return 5;
            case AbstractInsnNode.JUMP_INSN:
                return 5;
            case AbstractInsnNode.LDC_INSN:
                return 3;
            case AbstractInsnNode.IINC_INSN:
                return 6;
            case AbstractInsnNode.TABLESWITCH_INSN:
                // opcode + up to 3 pad + default/low/high (12) + one 4-byte offset per case.
                return 16 + ((TableSwitchInsnNode) n).labels.size() * 4;
            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                // opcode + up to 3 pad + default/npairs (8) + one 8-byte (match,offset) per case.
                return 12 + ((LookupSwitchInsnNode) n).keys.size() * 8;
            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                return 4;
            default:
                return 4;
        }
    }
}
