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
package com.codename1.tools.javac;

import java.util.ArrayList;
import java.util.List;

/**
 * The bytecode of one method under construction: instructions, labels with
 * forward-reference patching, local slots, the exception table and line numbers.
 * Tracks whether the current position is reachable so the generator never emits
 * dead code (which would need special StackMapTable treatment).
 */
final class Code {
    // Opcodes used by the generator.
    static final int NOP = 0x00, ACONST_NULL = 0x01, ICONST_M1 = 0x02, ICONST_0 = 0x03, LCONST_0 = 0x09,
            FCONST_0 = 0x0b, DCONST_0 = 0x0e, BIPUSH = 0x10, SIPUSH = 0x11, LDC = 0x12, LDC_W = 0x13, LDC2_W = 0x14,
            ILOAD = 0x15, LLOAD = 0x16, FLOAD = 0x17, DLOAD = 0x18, ALOAD = 0x19,
            IALOAD = 0x2e, LALOAD = 0x2f, FALOAD = 0x30, DALOAD = 0x31, AALOAD = 0x32, BALOAD = 0x33, CALOAD = 0x34,
            SALOAD = 0x35, ISTORE = 0x36, LSTORE = 0x37, FSTORE = 0x38, DSTORE = 0x39, ASTORE = 0x3a,
            IASTORE = 0x4f, LASTORE = 0x50, FASTORE = 0x51, DASTORE = 0x52, AASTORE = 0x53, BASTORE = 0x54,
            CASTORE = 0x55, SASTORE = 0x56, POP = 0x57, POP2 = 0x58, DUP = 0x59, DUP_X1 = 0x5a, DUP_X2 = 0x5b,
            DUP2 = 0x5c, DUP2_X1 = 0x5d, DUP2_X2 = 0x5e, SWAP = 0x5f,
            IADD = 0x60, LADD = 0x61, FADD = 0x62, DADD = 0x63, ISUB = 0x64, IMUL = 0x68, IDIV = 0x6c, IREM = 0x70,
            INEG = 0x74, ISHL = 0x78, ISHR = 0x7a, IUSHR = 0x7c, IAND = 0x7e, IOR = 0x80, IXOR = 0x82, LXOR = 0x83,
            IINC = 0x84, I2L = 0x85, I2F = 0x86, I2D = 0x87, L2I = 0x88, L2F = 0x89, L2D = 0x8a, F2I = 0x8b,
            F2L = 0x8c, F2D = 0x8d, D2I = 0x8e, D2L = 0x8f, D2F = 0x90, I2B = 0x91, I2C = 0x92, I2S = 0x93,
            LCMP = 0x94, FCMPL = 0x95, FCMPG = 0x96, DCMPL = 0x97, DCMPG = 0x98,
            IFEQ = 0x99, IFNE = 0x9a, IFLT = 0x9b, IFGE = 0x9c, IFGT = 0x9d, IFLE = 0x9e,
            IF_ICMPEQ = 0x9f, IF_ICMPNE = 0xa0, IF_ICMPLT = 0xa1, IF_ICMPGE = 0xa2, IF_ICMPGT = 0xa3, IF_ICMPLE = 0xa4,
            IF_ACMPEQ = 0xa5, IF_ACMPNE = 0xa6, GOTO = 0xa7, TABLESWITCH = 0xaa, LOOKUPSWITCH = 0xab,
            IRETURN = 0xac, LRETURN = 0xad, FRETURN = 0xae, DRETURN = 0xaf, ARETURN = 0xb0, RETURN = 0xb1,
            GETSTATIC = 0xb2, PUTSTATIC = 0xb3, GETFIELD = 0xb4, PUTFIELD = 0xb5, INVOKEVIRTUAL = 0xb6,
            INVOKESPECIAL = 0xb7, INVOKESTATIC = 0xb8, INVOKEINTERFACE = 0xb9, INVOKEDYNAMIC = 0xba, NEW = 0xbb,
            NEWARRAY = 0xbc, ANEWARRAY = 0xbd, ARRAYLENGTH = 0xbe, ATHROW = 0xbf, CHECKCAST = 0xc0,
            INSTANCEOF = 0xc1, MONITORENTER = 0xc2, MONITOREXIT = 0xc3, WIDE = 0xc4, MULTIANEWARRAY = 0xc5,
            IFNULL = 0xc6, IFNONNULL = 0xc7, GOTO_W = 0xc8;

    static final class Label {
        int pos = -1;
        /** {instruction start, patch position, width (2 or 4)} for each reference. */
        final List<int[]> refs = new ArrayList<int[]>();
        boolean jumpedTo;
    }

    final ByteBuf bc = new ByteBuf();
    final ConstPool pool;
    int nextLocal;
    int maxLocals;
    boolean alive = true;
    final List<int[]> exceptionTable = new ArrayList<int[]>();
    final List<int[]> lineNumbers = new ArrayList<int[]>();
    private int lastLine = -1;

    Code(ConstPool pool) {
        this.pool = pool;
    }

    int pc() {
        return bc.length;
    }

    int newLocal(Type t) {
        int slot = nextLocal;
        nextLocal += t.slots() == 0 ? 1 : t.slots();
        if (nextLocal > maxLocals) {
            maxLocals = nextLocal;
        }
        return slot;
    }

    void line(int line) {
        if (line > 0 && line != lastLine && alive) {
            lineNumbers.add(new int[]{pc(), line});
            lastLine = line;
        }
    }

    void op(int opcode) {
        bc.u1(opcode);
        switch (opcode) {
            case GOTO:
            case GOTO_W:
            case ATHROW:
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
            case RETURN:
                alive = false;
                break;
            default:
                break;
        }
    }

    void op1(int opcode, int b) {
        bc.u1(opcode).u1(b);
    }

    void op2(int opcode, int s) {
        bc.u1(opcode).u2(s);
    }

    void varOp(int opcode, int slot) {
        if (slot < 4 && opcode >= ILOAD && opcode <= ALOAD) {
            bc.u1(0x1a + (opcode - ILOAD) * 4 + slot);
        } else if (slot < 4 && opcode >= ISTORE && opcode <= ASTORE) {
            bc.u1(0x3b + (opcode - ISTORE) * 4 + slot);
        } else if (slot > 255) {
            bc.u1(WIDE).u1(opcode).u2(slot);
        } else {
            bc.u1(opcode).u1(slot);
        }
    }

    void iinc(int slot, int delta) {
        if (slot > 255 || delta < -128 || delta > 127) {
            bc.u1(WIDE).u1(IINC).u2(slot).u2(delta);
        } else {
            bc.u1(IINC).u1(slot).u1(delta);
        }
    }

    void iconst(int v) {
        if (v >= -1 && v <= 5) {
            bc.u1(ICONST_0 + v);
        } else if (v >= -128 && v <= 127) {
            bc.u1(BIPUSH).u1(v);
        } else if (v >= -32768 && v <= 32767) {
            bc.u1(SIPUSH).u2(v);
        } else {
            ldc(pool.integer(v));
        }
    }

    void ldc(int index) {
        if (index < 256) {
            bc.u1(LDC).u1(index);
        } else {
            bc.u1(LDC_W).u2(index);
        }
    }

    void ldc2(int index) {
        bc.u1(LDC2_W).u2(index);
    }

    void jump(int opcode, Label target) {
        int start = pc();
        bc.u1(opcode);
        target.jumpedTo = true;
        if (target.pos >= 0) {
            bc.u2(target.pos - start);
            checkRange(target.pos - start);
        } else {
            target.refs.add(new int[]{start, pc(), 2});
            bc.u2(0);
        }
        if (opcode == GOTO) {
            alive = false;
        }
    }

    private static void checkRange(int offset) {
        if (offset < -32768 || offset > 32767) {
            throw new CompileError("code too large");
        }
    }

    void place(Label l) {
        l.pos = pc();
        if (l.jumpedTo) {
            alive = true;
        }
        for (int[] r : l.refs) {
            int offset = l.pos - r[0];
            if (r[2] == 2) {
                checkRange(offset);
                bc.put2(r[1], offset);
            } else {
                bc.put4(r[1], offset);
            }
        }
        l.refs.clear();
    }

    /** Places a label that is reached only by an exception or by another path the caller knows about. */
    void placeReachable(Label l) {
        place(l);
        alive = true;
    }

    private void switchRef(int start, Label target) {
        target.jumpedTo = true;
        if (target.pos >= 0) {
            bc.u4(target.pos - start);
        } else {
            target.refs.add(new int[]{start, pc(), 4});
            bc.u4(0);
        }
    }

    void tableSwitch(int lo, int hi, Label dflt, Label[] targets) {
        int start = pc();
        bc.u1(TABLESWITCH);
        while (pc() % 4 != 0) {
            bc.u1(0);
        }
        switchRef(start, dflt);
        bc.u4(lo).u4(hi);
        for (Label t : targets) {
            switchRef(start, t);
        }
        alive = false;
    }

    void lookupSwitch(Label dflt, int[] keys, Label[] targets) {
        int start = pc();
        bc.u1(LOOKUPSWITCH);
        while (pc() % 4 != 0) {
            bc.u1(0);
        }
        switchRef(start, dflt);
        bc.u4(keys.length);
        for (int i = 0; i < keys.length; i++) {
            bc.u4(keys[i]);
            switchRef(start, targets[i]);
        }
        alive = false;
    }

    void addHandler(int start, int end, int handler, int catchType) {
        if (start < end) {
            exceptionTable.add(new int[]{start, end, handler, catchType});
        }
    }
}
