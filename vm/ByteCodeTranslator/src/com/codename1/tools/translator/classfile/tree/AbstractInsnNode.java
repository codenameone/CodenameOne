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

/**
 * A node of an {@link InsnList}: an instruction, or a label or line-number marker,
 * which report an opcode of -1.
 */
public abstract class AbstractInsnNode {
    public static final int INSN = 0;
    public static final int INT_INSN = 1;
    public static final int VAR_INSN = 2;
    public static final int TYPE_INSN = 3;
    public static final int FIELD_INSN = 4;
    public static final int METHOD_INSN = 5;
    public static final int INVOKE_DYNAMIC_INSN = 6;
    public static final int JUMP_INSN = 7;
    public static final int LABEL = 8;
    public static final int LDC_INSN = 9;
    public static final int IINC_INSN = 10;
    public static final int TABLESWITCH_INSN = 11;
    public static final int LOOKUPSWITCH_INSN = 12;
    public static final int MULTIANEWARRAY_INSN = 13;
    public static final int LINE = 15;

    protected int opcode;
    AbstractInsnNode previous;
    AbstractInsnNode next;
    /** Position in the owning list, valid while the list's cache is. */
    int index = -1;

    protected AbstractInsnNode(int opcode) {
        this.opcode = opcode;
    }

    public int getOpcode() {
        return opcode;
    }

    public abstract int getType();

    public AbstractInsnNode getPrevious() {
        return previous;
    }

    public AbstractInsnNode getNext() {
        return next;
    }

    /**
     * The descriptor a field, method, dynamic or MULTIANEWARRAY instruction carries,
     * or the internal name a type instruction does; null for anything else.
     */
    public String getDescriptor() {
        return null;
    }

    /** The local variable a load, store, RET or IINC addresses; -1 for anything else. */
    public int getLocal() {
        return -1;
    }

    /** The constant an LDC loads; null for anything else. */
    public Object getConstant() {
        return null;
    }

    /** The dimension count of a MULTIANEWARRAY; 0 for anything else. */
    public int getDimensions() {
        return 0;
    }
}
