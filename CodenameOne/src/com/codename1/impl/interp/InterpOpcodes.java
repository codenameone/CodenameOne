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
package com.codename1.impl.interp;

/// The instruction encoding shared by the desktop bundle writer and the device
/// interpreter.
///
/// Opcode numbers are the JVM's own. Only the operands differ: a constant-pool
/// index becomes an index into the bundle's extern or string table, and a
/// branch offset becomes an instruction index. Keeping the opcodes means the
/// interpreter's semantics can be checked against the JVM directly, which is
/// what the differential conformance tests do.
///
/// Two encodings are synthesised because the JVM's are not fixed-width:
/// [#OP_TABLESWITCH] and [#OP_LOOKUPSWITCH] store their own operand count
/// first. Everything else has a constant operand count given by
/// [#operandCount].
///
/// @author Shai Almog
public final class InterpOpcodes {
    private InterpOpcodes() {
    }

    // Return / value kinds. Used for descriptors, call argument widths and
    // array element widths.
    public static final int RET_VOID = 0;
    public static final int RET_INT = 1;
    public static final int RET_LONG = 2;
    public static final int RET_FLOAT = 3;
    public static final int RET_DOUBLE = 4;
    public static final int RET_OBJECT = 5;
    public static final int RET_BOOLEAN = 6;
    public static final int RET_BYTE = 7;
    public static final int RET_CHAR = 8;
    public static final int RET_SHORT = 9;

    /// True when a value of this kind occupies two stack slots.
    public static boolean isCategory2(int kind) {
        return kind == RET_LONG || kind == RET_DOUBLE;
    }

    // Opcodes are the JVM's; listed here only where the interpreter refers to
    // them by name.
    public static final int NOP = 0;
    public static final int ACONST_NULL = 1;
    public static final int ICONST_M1 = 2;
    public static final int ICONST_0 = 3;
    public static final int ICONST_5 = 8;
    public static final int LCONST_0 = 9;
    public static final int LCONST_1 = 10;
    public static final int FCONST_0 = 11;
    public static final int FCONST_2 = 13;
    public static final int DCONST_0 = 14;
    public static final int DCONST_1 = 15;
    public static final int BIPUSH = 16;
    public static final int SIPUSH = 17;
    public static final int LDC = 18;
    public static final int ILOAD = 21;
    public static final int LLOAD = 22;
    public static final int FLOAD = 23;
    public static final int DLOAD = 24;
    public static final int ALOAD = 25;
    public static final int IALOAD = 46;
    public static final int LALOAD = 47;
    public static final int FALOAD = 48;
    public static final int DALOAD = 49;
    public static final int AALOAD = 50;
    public static final int BALOAD = 51;
    public static final int CALOAD = 52;
    public static final int SALOAD = 53;
    public static final int ISTORE = 54;
    public static final int LSTORE = 55;
    public static final int FSTORE = 56;
    public static final int DSTORE = 57;
    public static final int ASTORE = 58;
    public static final int IASTORE = 79;
    public static final int LASTORE = 80;
    public static final int FASTORE = 81;
    public static final int DASTORE = 82;
    public static final int AASTORE = 83;
    public static final int BASTORE = 84;
    public static final int CASTORE = 85;
    public static final int SASTORE = 86;
    public static final int POP = 87;
    public static final int POP2 = 88;
    public static final int DUP = 89;
    public static final int DUP_X1 = 90;
    public static final int DUP_X2 = 91;
    public static final int DUP2 = 92;
    public static final int DUP2_X1 = 93;
    public static final int DUP2_X2 = 94;
    public static final int SWAP = 95;
    public static final int IADD = 96;
    public static final int LADD = 97;
    public static final int FADD = 98;
    public static final int DADD = 99;
    public static final int ISUB = 100;
    public static final int LSUB = 101;
    public static final int FSUB = 102;
    public static final int DSUB = 103;
    public static final int IMUL = 104;
    public static final int LMUL = 105;
    public static final int FMUL = 106;
    public static final int DMUL = 107;
    public static final int IDIV = 108;
    public static final int LDIV = 109;
    public static final int FDIV = 110;
    public static final int DDIV = 111;
    public static final int IREM = 112;
    public static final int LREM = 113;
    public static final int FREM = 114;
    public static final int DREM = 115;
    public static final int INEG = 116;
    public static final int LNEG = 117;
    public static final int FNEG = 118;
    public static final int DNEG = 119;
    public static final int ISHL = 120;
    public static final int LSHL = 121;
    public static final int ISHR = 122;
    public static final int LSHR = 123;
    public static final int IUSHR = 124;
    public static final int LUSHR = 125;
    public static final int IAND = 126;
    public static final int LAND = 127;
    public static final int IOR = 128;
    public static final int LOR = 129;
    public static final int IXOR = 130;
    public static final int LXOR = 131;
    public static final int IINC = 132;
    public static final int I2L = 133;
    public static final int I2F = 134;
    public static final int I2D = 135;
    public static final int L2I = 136;
    public static final int L2F = 137;
    public static final int L2D = 138;
    public static final int F2I = 139;
    public static final int F2L = 140;
    public static final int F2D = 141;
    public static final int D2I = 142;
    public static final int D2L = 143;
    public static final int D2F = 144;
    public static final int I2B = 145;
    public static final int I2C = 146;
    public static final int I2S = 147;
    public static final int LCMP = 148;
    public static final int FCMPL = 149;
    public static final int FCMPG = 150;
    public static final int DCMPL = 151;
    public static final int DCMPG = 152;
    public static final int IFEQ = 153;
    public static final int IFNE = 154;
    public static final int IFLT = 155;
    public static final int IFGE = 156;
    public static final int IFGT = 157;
    public static final int IFLE = 158;
    public static final int IF_ICMPEQ = 159;
    public static final int IF_ICMPNE = 160;
    public static final int IF_ICMPLT = 161;
    public static final int IF_ICMPGE = 162;
    public static final int IF_ICMPGT = 163;
    public static final int IF_ICMPLE = 164;
    public static final int IF_ACMPEQ = 165;
    public static final int IF_ACMPNE = 166;
    public static final int GOTO = 167;
    public static final int OP_TABLESWITCH = 170;
    public static final int OP_LOOKUPSWITCH = 171;
    public static final int IRETURN = 172;
    public static final int LRETURN = 173;
    public static final int FRETURN = 174;
    public static final int DRETURN = 175;
    public static final int ARETURN = 176;
    public static final int RETURN = 177;
    public static final int GETSTATIC = 178;
    public static final int PUTSTATIC = 179;
    public static final int GETFIELD = 180;
    public static final int PUTFIELD = 181;
    public static final int INVOKEVIRTUAL = 182;
    public static final int INVOKESPECIAL = 183;
    public static final int INVOKESTATIC = 184;
    public static final int INVOKEINTERFACE = 185;
    public static final int NEW = 187;
    public static final int NEWARRAY = 188;
    public static final int ANEWARRAY = 189;
    public static final int ARRAYLENGTH = 190;
    public static final int ATHROW = 191;
    public static final int CHECKCAST = 192;
    public static final int INSTANCEOF = 193;
    public static final int MONITORENTER = 194;
    public static final int MONITOREXIT = 195;
    public static final int MULTIANEWARRAY = 197;
    public static final int IFNULL = 198;
    public static final int IFNONNULL = 199;

    /// A constant loaded by LDC, tagged so the interpreter knows the width and
    /// whether the operand indexes the string pool or is an immediate.
    public static final int LDC_INT = 0;
    public static final int LDC_LONG = 1;
    public static final int LDC_FLOAT = 2;
    public static final int LDC_DOUBLE = 3;
    public static final int LDC_STRING = 4;
    public static final int LDC_CLASS = 5;

    /// Number of operand ints that follow the given opcode.
    ///
    /// The two switch opcodes are variable length and are not covered here;
    /// they store their operand count as their first operand. Everything else
    /// is fixed so the writer and the interpreter agree without a table lookup
    /// per instruction.
    public static int operandCount(int opcode) {
        switch (opcode) {
            case BIPUSH:
            case SIPUSH:
            case ILOAD: case LLOAD: case FLOAD: case DLOAD: case ALOAD:
            case ISTORE: case LSTORE: case FSTORE: case DSTORE: case ASTORE:
            case IFEQ: case IFNE: case IFLT: case IFGE: case IFGT: case IFLE:
            case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT:
            case IF_ICMPGE: case IF_ICMPGT: case IF_ICMPLE:
            case IF_ACMPEQ: case IF_ACMPNE:
            case GOTO:
            case NEW: case ANEWARRAY: case CHECKCAST: case INSTANCEOF:
            case NEWARRAY:
            case GETSTATIC: case PUTSTATIC: case GETFIELD: case PUTFIELD:
            case INVOKEVIRTUAL: case INVOKESPECIAL:
            case INVOKESTATIC: case INVOKEINTERFACE:
            case IFNULL: case IFNONNULL:
                return 1;
            case LDC:       // tag, value/index
            case IINC:      // local, increment
            case MULTIANEWARRAY: // extern, dimensions
                return 2;
            default:
                return 0;
        }
    }
}
