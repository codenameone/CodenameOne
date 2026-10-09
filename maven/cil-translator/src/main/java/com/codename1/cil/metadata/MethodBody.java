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
package com.codename1.cil.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The decoded body of one method: its locals, its instructions and its
/// exception clauses.
///
/// Instructions are normalised while they are read so that a consumer sees one
/// spelling of each operation. The compact forms a compiler picks to save
/// bytes -- `ldarg.0`, `ldc.i4.s`, `br.s` -- come out as the general form with
/// the operand filled in, and a branch operand is the absolute offset of its
/// target, never the relative distance the file stores.
public final class MethodBody {

    /// One instruction. Which operand field is meaningful follows from the
    /// operation: a token, an index, a 32-bit constant or a branch target is
    /// in [#operand].
    public static final class Instr {
        public final int offset;
        /// Offset of the instruction that follows this one in the stream.
        public final int next;
        public final String op;
        public final int operand;
        public final long longOperand;
        public final double doubleOperand;
        public final int[] targets;

        Instr(int offset, int next, String op, int operand, long longOperand, double doubleOperand, int[] targets) {
            this.offset = offset;
            this.next = next;
            this.op = op;
            this.operand = operand;
            this.longOperand = longOperand;
            this.doubleOperand = doubleOperand;
            this.targets = targets;
        }

        @Override
        public String toString() {
            return "IL_" + Integer.toHexString(offset) + ": " + op;
        }
    }

    /// One row of the exception table. The ranges are half open:
    /// `[tryStart, tryEnd)` and `[handlerStart, handlerEnd)`.
    public static final class Clause {
        public static final int CATCH = 0;
        public static final int FILTER = 1;
        public static final int FINALLY = 2;
        public static final int FAULT = 4;

        public final int flags;
        public final int tryStart;
        public final int tryEnd;
        public final int handlerStart;
        public final int handlerEnd;
        /// Token of the caught type for a catch clause; filter offset for a filter.
        public final int classToken;

        Clause(int flags, int tryStart, int tryLength, int handlerStart, int handlerLength, int classToken) {
            this.flags = flags;
            this.tryStart = tryStart;
            this.tryEnd = tryStart + tryLength;
            this.handlerStart = handlerStart;
            this.handlerEnd = handlerStart + handlerLength;
            this.classToken = classToken;
        }
    }

    private static final String ONE_BYTE =
            "00 nop n;01 break n;02 ldarg.0 n;03 ldarg.1 n;04 ldarg.2 n;05 ldarg.3 n;06 ldloc.0 n;07 ldloc.1 n;"
            + "08 ldloc.2 n;09 ldloc.3 n;0A stloc.0 n;0B stloc.1 n;0C stloc.2 n;0D stloc.3 n;0E ldarg.s v1;"
            + "0F ldarga.s v1;10 starg.s v1;11 ldloc.s v1;12 ldloca.s v1;13 stloc.s v1;14 ldnull n;15 ldc.i4.m1 n;"
            + "16 ldc.i4.0 n;17 ldc.i4.1 n;18 ldc.i4.2 n;19 ldc.i4.3 n;1A ldc.i4.4 n;1B ldc.i4.5 n;1C ldc.i4.6 n;"
            + "1D ldc.i4.7 n;1E ldc.i4.8 n;1F ldc.i4.s c1;20 ldc.i4 i4;21 ldc.i8 i8;22 ldc.r4 r4;23 ldc.r8 r8;"
            + "25 dup n;26 pop n;27 jmp t;28 call t;29 calli t;2A ret n;2B br.s b1;2C brfalse.s b1;2D brtrue.s b1;"
            + "2E beq.s b1;2F bge.s b1;30 bgt.s b1;31 ble.s b1;32 blt.s b1;33 bne.un.s b1;34 bge.un.s b1;"
            + "35 bgt.un.s b1;36 ble.un.s b1;37 blt.un.s b1;38 br b4;39 brfalse b4;3A brtrue b4;3B beq b4;3C bge b4;"
            + "3D bgt b4;3E ble b4;3F blt b4;40 bne.un b4;41 bge.un b4;42 bgt.un b4;43 ble.un b4;44 blt.un b4;"
            + "45 switch s;46 ldind.i1 n;47 ldind.u1 n;48 ldind.i2 n;49 ldind.u2 n;4A ldind.i4 n;4B ldind.u4 n;"
            + "4C ldind.i8 n;4D ldind.i n;4E ldind.r4 n;4F ldind.r8 n;50 ldind.ref n;51 stind.ref n;52 stind.i1 n;"
            + "53 stind.i2 n;54 stind.i4 n;55 stind.i8 n;56 stind.r4 n;57 stind.r8 n;58 add n;59 sub n;5A mul n;"
            + "5B div n;5C div.un n;5D rem n;5E rem.un n;5F and n;60 or n;61 xor n;62 shl n;63 shr n;64 shr.un n;"
            + "65 neg n;66 not n;67 conv.i1 n;68 conv.i2 n;69 conv.i4 n;6A conv.i8 n;6B conv.r4 n;6C conv.r8 n;"
            + "6D conv.u4 n;6E conv.u8 n;6F callvirt t;70 cpobj t;71 ldobj t;72 ldstr t;73 newobj t;74 castclass t;"
            + "75 isinst t;76 conv.r.un n;79 unbox t;7A throw n;7B ldfld t;7C ldflda t;7D stfld t;7E ldsfld t;"
            + "7F ldsflda t;80 stsfld t;81 stobj t;82 conv.ovf.i1.un n;83 conv.ovf.i2.un n;84 conv.ovf.i4.un n;"
            + "85 conv.ovf.i8.un n;86 conv.ovf.u1.un n;87 conv.ovf.u2.un n;88 conv.ovf.u4.un n;89 conv.ovf.u8.un n;"
            + "8A conv.ovf.i.un n;8B conv.ovf.u.un n;8C box t;8D newarr t;8E ldlen n;8F ldelema t;90 ldelem.i1 n;"
            + "91 ldelem.u1 n;92 ldelem.i2 n;93 ldelem.u2 n;94 ldelem.i4 n;95 ldelem.u4 n;96 ldelem.i8 n;"
            + "97 ldelem.i n;98 ldelem.r4 n;99 ldelem.r8 n;9A ldelem.ref n;9B stelem.i n;9C stelem.i1 n;"
            + "9D stelem.i2 n;9E stelem.i4 n;9F stelem.i8 n;A0 stelem.r4 n;A1 stelem.r8 n;A2 stelem.ref n;"
            + "A3 ldelem t;A4 stelem t;A5 unbox.any t;B3 conv.ovf.i1 n;B4 conv.ovf.u1 n;B5 conv.ovf.i2 n;"
            + "B6 conv.ovf.u2 n;B7 conv.ovf.i4 n;B8 conv.ovf.u4 n;B9 conv.ovf.i8 n;BA conv.ovf.u8 n;C2 refanyval t;"
            + "C3 ckfinite n;C6 mkrefany t;D0 ldtoken t;D1 conv.u2 n;D2 conv.u1 n;D3 conv.i n;D4 conv.ovf.i n;"
            + "D5 conv.ovf.u n;D6 add.ovf n;D7 add.ovf.un n;D8 mul.ovf n;D9 mul.ovf.un n;DA sub.ovf n;"
            + "DB sub.ovf.un n;DC endfinally n;DD leave b4;DE leave.s b1;DF stind.i n;E0 conv.u n";

    private static final String TWO_BYTE =
            "00 arglist n;01 ceq n;02 cgt n;03 cgt.un n;04 clt n;05 clt.un n;06 ldftn t;07 ldvirtftn t;09 ldarg v2;"
            + "0A ldarga v2;0B starg v2;0C ldloc v2;0D ldloca v2;0E stloc v2;0F localloc n;11 endfilter n;"
            + "12 unaligned. c1;13 volatile. n;14 tail. n;15 initobj t;16 constrained. t;17 cpblk n;18 initblk n;"
            + "19 no. c1;1A rethrow n;1C sizeof t;1D refanytype n;1E readonly. n";

    private static final String[][] ONE = parseTable(ONE_BYTE);
    private static final String[][] TWO = parseTable(TWO_BYTE);
    private static final Map<String, Object[]> CANONICAL = new HashMap<String, Object[]>();

    private static String[][] parseTable(String spec) {
        String[][] table = new String[256][];
        for (String entry : spec.split(";")) {
            String[] parts = entry.split(" ");
            table[Integer.parseInt(parts[0], 16)] = new String[] {parts[1], parts[2]};
        }
        return table;
    }

    /// Splits a compact opcode name into the general operation and the
    /// operand the name implies: `ldloc.2` is `ldloc` 2, `br.s` is `br`.
    private static Object[] canonical(String raw) {
        Object[] cached = CANONICAL.get(raw);
        if (cached != null) {
            return cached;
        }
        String op = raw;
        Integer implied = null;
        if ("ldc.i4.m1".equals(raw)) {
            op = "ldc.i4";
            implied = Integer.valueOf(-1);
        } else if ("ldc.i4.s".equals(raw)) {
            op = "ldc.i4";
        } else {
            int dot = raw.lastIndexOf('.');
            String head = dot < 0 ? raw : raw.substring(0, dot);
            String tail = dot < 0 ? "" : raw.substring(dot + 1);
            boolean digit = tail.length() == 1 && tail.charAt(0) >= '0' && tail.charAt(0) <= '9';
            if (digit && ("ldarg".equals(head) || "ldloc".equals(head) || "stloc".equals(head)
                    || "ldc.i4".equals(head))) {
                op = head;
                implied = Integer.valueOf(tail.charAt(0) - '0');
            } else if ("s".equals(tail)) {
                op = head;
            }
        }
        Object[] out = new Object[] {op.intern(), implied};
        CANONICAL.put(raw, out);
        return out;
    }

    public final CilType[] locals;
    public final List<Instr> instructions = new ArrayList<Instr>();
    public final List<Clause> clauses = new ArrayList<Clause>();
    private final Map<Integer, Integer> indexByOffset = new HashMap<Integer, Integer>();

    MethodBody(CilAssembly assembly, int rva) {
        MetadataReader md = assembly.metadata();
        int at = md.rvaToOffset(rva);
        int first = md.u1(at);
        int code;
        int codeSize;
        boolean moreSections = false;
        if ((first & 3) == 2) {
            // Tiny header: no locals, no clauses, at most 8 stack slots.
            locals = new CilType[0];
            codeSize = first >> 2;
            code = at + 1;
        } else {
            int flags = md.u2(at);
            int headerSize = (flags >> 12) * 4;
            codeSize = md.i4(at + 4);
            int localsToken = md.i4(at + 8);
            moreSections = (flags & 0x08) != 0;
            locals = localsToken == 0 ? new CilType[0] : assembly.localsFromToken(localsToken);
            code = at + headerSize;
        }
        decode(md, code, codeSize);
        if (moreSections) {
            readClauses(md, (code + codeSize + 3) & ~3);
        }
    }

    private void decode(MetadataReader md, int code, int codeSize) {
        int p = 0;
        while (p < codeSize) {
            int start = p;
            int b = md.u1(code + p++);
            String[] def;
            if (b == 0xFE) {
                def = TWO[md.u1(code + p++)];
            } else {
                def = ONE[b];
            }
            if (def == null) {
                throw new CilFormatException(md.source() + ": unknown opcode 0x" + Integer.toHexString(b)
                        + " at IL_" + Integer.toHexString(start));
            }
            Object[] canon = canonical(def[0]);
            String op = (String) canon[0];
            int operand = canon[1] == null ? 0 : ((Integer) canon[1]).intValue();
            long longOperand = 0;
            double doubleOperand = 0;
            int[] targets = null;
            String kind = def[1];
            if ("v1".equals(kind)) {
                operand = md.u1(code + p);
                p += 1;
            } else if ("v2".equals(kind)) {
                operand = md.u2(code + p);
                p += 2;
            } else if ("c1".equals(kind)) {
                operand = (byte) md.u1(code + p);
                p += 1;
            } else if ("i4".equals(kind) || "t".equals(kind)) {
                operand = md.i4(code + p);
                p += 4;
            } else if ("i8".equals(kind)) {
                longOperand = md.i8(code + p);
                p += 8;
            } else if ("r4".equals(kind)) {
                doubleOperand = Float.intBitsToFloat(md.i4(code + p));
                p += 4;
            } else if ("r8".equals(kind)) {
                doubleOperand = Double.longBitsToDouble(md.i8(code + p));
                p += 8;
            } else if ("b1".equals(kind)) {
                int delta = (byte) md.u1(code + p);
                p += 1;
                operand = p + delta;
            } else if ("b4".equals(kind)) {
                int delta = md.i4(code + p);
                p += 4;
                operand = p + delta;
            } else if ("s".equals(kind)) {
                int count = md.i4(code + p);
                p += 4;
                targets = new int[count];
                int base = p + count * 4;
                for (int i = 0; i < count; i++) {
                    targets[i] = base + md.i4(code + p);
                    p += 4;
                }
            }
            indexByOffset.put(Integer.valueOf(start), Integer.valueOf(instructions.size()));
            instructions.add(new Instr(start, p, op, operand, longOperand, doubleOperand, targets));
        }
    }

    private void readClauses(MetadataReader md, int at) {
        while (true) {
            int kind = md.u1(at);
            if ((kind & 0x01) == 0) {
                throw new CilFormatException(md.source() + ": unknown method data section 0x"
                        + Integer.toHexString(kind));
            }
            boolean fat = (kind & 0x40) != 0;
            int size;
            if (fat) {
                size = md.u1(at + 1) | (md.u1(at + 2) << 8) | (md.u1(at + 3) << 16);
                for (int c = at + 4; c + 24 <= at + size; c += 24) {
                    clauses.add(new Clause(md.i4(c), md.i4(c + 4), md.i4(c + 8), md.i4(c + 12), md.i4(c + 16),
                            md.i4(c + 20)));
                }
            } else {
                size = md.u1(at + 1);
                for (int c = at + 4; c + 12 <= at + size; c += 12) {
                    clauses.add(new Clause(md.u2(c), md.u2(c + 2), md.u1(c + 4), md.u2(c + 5), md.u1(c + 7),
                            md.i4(c + 8)));
                }
            }
            if ((kind & 0x80) == 0) {
                return;
            }
            at = (at + size + 3) & ~3;
        }
    }

    /// Position in [#instructions] of the instruction at an IL offset.
    public int indexOf(int offset) {
        Integer index = indexByOffset.get(Integer.valueOf(offset));
        if (index == null) {
            throw new CilFormatException("no instruction starts at IL_" + Integer.toHexString(offset));
        }
        return index.intValue();
    }
}
