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
// This file is a rewrite of ASM's org.objectweb.asm.Opcodes for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile;

/**
 * JVM opcode, access-flag, array-type and method-handle-kind constants, with the
 * values the JVM specification assigns them. The {@code ACC_RECORD} and
 * {@code ACC_DEPRECATED} pseudo flags sit above the 16 bits a class file stores,
 * as the reader reports them.
 */
public interface Opcodes {
    int ACC_PUBLIC = 0x0001;
    int ACC_PRIVATE = 0x0002;
    int ACC_PROTECTED = 0x0004;
    int ACC_STATIC = 0x0008;
    int ACC_FINAL = 0x0010;
    int ACC_SUPER = 0x0020;
    int ACC_SYNCHRONIZED = 0x0020;
    int ACC_OPEN = 0x0020;
    int ACC_TRANSITIVE = 0x0020;
    int ACC_VOLATILE = 0x0040;
    int ACC_BRIDGE = 0x0040;
    int ACC_STATIC_PHASE = 0x0040;
    int ACC_VARARGS = 0x0080;
    int ACC_TRANSIENT = 0x0080;
    int ACC_NATIVE = 0x0100;
    int ACC_INTERFACE = 0x0200;
    int ACC_ABSTRACT = 0x0400;
    int ACC_STRICT = 0x0800;
    int ACC_SYNTHETIC = 0x1000;
    int ACC_ANNOTATION = 0x2000;
    int ACC_ENUM = 0x4000;
    int ACC_MANDATED = 0x8000;
    int ACC_MODULE = 0x8000;
    int ACC_RECORD = 0x10000;
    int ACC_DEPRECATED = 0x20000;
    int T_BOOLEAN = 4;
    int T_CHAR = 5;
    int T_FLOAT = 6;
    int T_DOUBLE = 7;
    int T_BYTE = 8;
    int T_SHORT = 9;
    int T_INT = 10;
    int T_LONG = 11;
    int H_GETFIELD = 1;
    int H_GETSTATIC = 2;
    int H_PUTFIELD = 3;
    int H_PUTSTATIC = 4;
    int H_INVOKEVIRTUAL = 5;
    int H_INVOKESTATIC = 6;
    int H_INVOKESPECIAL = 7;
    int H_NEWINVOKESPECIAL = 8;
    int H_INVOKEINTERFACE = 9;

    int NOP = 0;
    int ACONST_NULL = 1;
    int ICONST_M1 = 2;
    int ICONST_0 = 3;
    int ICONST_1 = 4;
    int ICONST_2 = 5;
    int ICONST_3 = 6;
    int ICONST_4 = 7;
    int ICONST_5 = 8;
    int LCONST_0 = 9;
    int LCONST_1 = 10;
    int FCONST_0 = 11;
    int FCONST_1 = 12;
    int FCONST_2 = 13;
    int DCONST_0 = 14;
    int DCONST_1 = 15;
    int BIPUSH = 16;
    int SIPUSH = 17;
    int LDC = 18;
    int ILOAD = 21;
    int LLOAD = 22;
    int FLOAD = 23;
    int DLOAD = 24;
    int ALOAD = 25;
    int IALOAD = 46;
    int LALOAD = 47;
    int FALOAD = 48;
    int DALOAD = 49;
    int AALOAD = 50;
    int BALOAD = 51;
    int CALOAD = 52;
    int SALOAD = 53;
    int ISTORE = 54;
    int LSTORE = 55;
    int FSTORE = 56;
    int DSTORE = 57;
    int ASTORE = 58;
    int IASTORE = 79;
    int LASTORE = 80;
    int FASTORE = 81;
    int DASTORE = 82;
    int AASTORE = 83;
    int BASTORE = 84;
    int CASTORE = 85;
    int SASTORE = 86;
    int POP = 87;
    int POP2 = 88;
    int DUP = 89;
    int DUP_X1 = 90;
    int DUP_X2 = 91;
    int DUP2 = 92;
    int DUP2_X1 = 93;
    int DUP2_X2 = 94;
    int SWAP = 95;
    int IADD = 96;
    int LADD = 97;
    int FADD = 98;
    int DADD = 99;
    int ISUB = 100;
    int LSUB = 101;
    int FSUB = 102;
    int DSUB = 103;
    int IMUL = 104;
    int LMUL = 105;
    int FMUL = 106;
    int DMUL = 107;
    int IDIV = 108;
    int LDIV = 109;
    int FDIV = 110;
    int DDIV = 111;
    int IREM = 112;
    int LREM = 113;
    int FREM = 114;
    int DREM = 115;
    int INEG = 116;
    int LNEG = 117;
    int FNEG = 118;
    int DNEG = 119;
    int ISHL = 120;
    int LSHL = 121;
    int ISHR = 122;
    int LSHR = 123;
    int IUSHR = 124;
    int LUSHR = 125;
    int IAND = 126;
    int LAND = 127;
    int IOR = 128;
    int LOR = 129;
    int IXOR = 130;
    int LXOR = 131;
    int IINC = 132;
    int I2L = 133;
    int I2F = 134;
    int I2D = 135;
    int L2I = 136;
    int L2F = 137;
    int L2D = 138;
    int F2I = 139;
    int F2L = 140;
    int F2D = 141;
    int D2I = 142;
    int D2L = 143;
    int D2F = 144;
    int I2B = 145;
    int I2C = 146;
    int I2S = 147;
    int LCMP = 148;
    int FCMPL = 149;
    int FCMPG = 150;
    int DCMPL = 151;
    int DCMPG = 152;
    int IFEQ = 153;
    int IFNE = 154;
    int IFLT = 155;
    int IFGE = 156;
    int IFGT = 157;
    int IFLE = 158;
    int IF_ICMPEQ = 159;
    int IF_ICMPNE = 160;
    int IF_ICMPLT = 161;
    int IF_ICMPGE = 162;
    int IF_ICMPGT = 163;
    int IF_ICMPLE = 164;
    int IF_ACMPEQ = 165;
    int IF_ACMPNE = 166;
    int GOTO = 167;
    int JSR = 168;
    int RET = 169;
    int TABLESWITCH = 170;
    int LOOKUPSWITCH = 171;
    int IRETURN = 172;
    int LRETURN = 173;
    int FRETURN = 174;
    int DRETURN = 175;
    int ARETURN = 176;
    int RETURN = 177;
    int GETSTATIC = 178;
    int PUTSTATIC = 179;
    int GETFIELD = 180;
    int PUTFIELD = 181;
    int INVOKEVIRTUAL = 182;
    int INVOKESPECIAL = 183;
    int INVOKESTATIC = 184;
    int INVOKEINTERFACE = 185;
    int INVOKEDYNAMIC = 186;
    int NEW = 187;
    int NEWARRAY = 188;
    int ANEWARRAY = 189;
    int ARRAYLENGTH = 190;
    int ATHROW = 191;
    int CHECKCAST = 192;
    int INSTANCEOF = 193;
    int MONITORENTER = 194;
    int MONITOREXIT = 195;
    int MULTIANEWARRAY = 197;
    int IFNULL = 198;
    int IFNONNULL = 199;
}
