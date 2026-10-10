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
package com.codename1.cil.translate;

import com.codename1.cil.metadata.CilType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// Opcode arithmetic: which JVM instruction handles a value of a given kind
/// or an array of a given element type.
final class Jvm implements Opcodes {
    private Jvm() {
    }

    static int loadOp(int kind) {
        return kind == Val.REF ? ALOAD : ILOAD + kind;
    }

    static int storeOp(int kind) {
        return kind == Val.REF ? ASTORE : ISTORE + kind;
    }

    static void pushInt(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            mv.visitLdcInsn(Integer.valueOf(value));
        }
    }

    static void pushDefault(MethodVisitor mv, int kind) {
        switch (kind) {
            case Val.I4:
                mv.visitInsn(ICONST_0);
                break;
            case Val.I8:
                mv.visitInsn(LCONST_0);
                break;
            case Val.R4:
                mv.visitInsn(FCONST_0);
                break;
            case Val.R8:
                mv.visitInsn(DCONST_0);
                break;
            default:
                mv.visitInsn(ACONST_NULL);
                break;
        }
    }

    /// Offset of an element type within the `xALOAD`/`xASTORE` families,
    /// which are laid out int, long, float, double, reference, byte, char,
    /// short.
    private static int arrayOffset(CilType element) {
        switch (element.kind) {
            case BOOLEAN:
            case I1:
            case U1:
                return 5;
            case CHAR:
            case U2:
                return 6;
            case I2:
                return 7;
            case I4:
            case U4:
            case I:
            case U:
                return 0;
            case I8:
            case U8:
                return 1;
            case R4:
                return 2;
            case R8:
                return 3;
            default:
                return 4;
        }
    }

    static int arrayLoad(CilType element) {
        return IALOAD + arrayOffset(element);
    }

    static int arrayStore(CilType element) {
        return IASTORE + arrayOffset(element);
    }

    static int arrayTypeCode(CilType element) {
        switch (element.kind) {
            case BOOLEAN:
                return T_BOOLEAN;
            case CHAR:
            case U2:
                return T_CHAR;
            case I1:
            case U1:
                return T_BYTE;
            case I2:
                return T_SHORT;
            case I8:
            case U8:
                return T_LONG;
            case R4:
                return T_FLOAT;
            case R8:
                return T_DOUBLE;
            default:
                return T_INT;
        }
    }

    /// Boxes the primitive on top of the stack as its `java.lang` class.
    static void box(MethodVisitor mv, Names names, CilType type) {
        CilType p = names.norm(type);
        if (p.kind == CilType.Kind.U1) {
            // Byte.valueOf indexes a cache with its argument: 200 has to
            // arrive as -56.
            mv.visitInsn(I2B);
        }
        String c = Names.boxClass(p);
        mv.visitMethodInsn(INVOKESTATIC, c, "valueOf", "(" + names.descriptor(p) + ")L" + c + ";", false);
    }

    /// Unboxes a value known to be the right box: the result of a generic
    /// member, not a cast the C# code asked for.
    static void unbox(MethodVisitor mv, Names names, CilType type) {
        CilType p = names.norm(type);
        String c = Names.boxClass(p);
        mv.visitTypeInsn(CHECKCAST, c);
        mv.visitMethodInsn(INVOKEVIRTUAL, c, unboxMethod(p), "()" + names.descriptor(p), false);
        if (p.kind == CilType.Kind.U1) {
            pushInt(mv, 255);
            mv.visitInsn(IAND);
        }
    }

    static String unboxMethod(CilType primitive) {
        switch (primitive.kind) {
            case BOOLEAN:
                return "booleanValue";
            case CHAR:
            case U2:
                return "charValue";
            case I1:
            case U1:
                return "byteValue";
            case I2:
                return "shortValue";
            case I8:
            case U8:
                return "longValue";
            case R4:
                return "floatValue";
            case R8:
                return "doubleValue";
            default:
                return "intValue";
        }
    }

    /// Suffix of the checked `Interop.unbox...` helper for a primitive.
    static String unboxSuffix(CilType primitive) {
        String m = unboxMethod(primitive);
        String stem = m.substring(0, m.length() - "Value".length());
        return Character.toUpperCase(stem.charAt(0)) + stem.substring(1);
    }

    /// The type an instruction suffix such as the `i4` of `ldelem.i4` names.
    static CilType suffixType(String suffix) {
        switch (suffix) {
            case "i1":
                return CilType.I1;
            case "u1":
                return CilType.U1;
            case "i2":
                return CilType.I2;
            case "u2":
                return CilType.U2;
            case "i4":
                return CilType.I4;
            case "u4":
                return CilType.U4;
            case "i8":
                return CilType.I8;
            case "i":
                return CilType.I;
            case "r4":
                return CilType.R4;
            case "r8":
                return CilType.R8;
            case "ref":
                return CilType.OBJECT;
            default:
                throw new TranslationException("unknown instruction suffix ." + suffix);
        }
    }

    /// Size of a primitive in an assembly's static data, or 0 when the type
    /// is not one an array literal can hold.
    static int byteSize(CilType primitive) {
        switch (primitive.kind) {
            case BOOLEAN:
            case I1:
            case U1:
                return 1;
            case CHAR:
            case I2:
            case U2:
                return 2;
            case I4:
            case U4:
            case R4:
                return 4;
            case I8:
            case U8:
            case R8:
                return 8;
            default:
                return 0;
        }
    }
}
