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

/// One interpreted method: its signature, its frame sizes, and its code.
///
/// `code` is a flat int array of (opcode, operand...) with a fixed operand
/// count per opcode, and `pcOfInstruction` maps instruction index to code
/// offset. Jump operands are already instruction indices, so branching is an
/// array index rather than a search for a label.
///
/// @author Shai Almog
public final class InterpMethod {
    final InterpClass owner;

    String name = "";
    String desc = "";
    int accessFlags;

    int maxStack;
    int maxLocals;

    /// Flat instruction stream. Layout is (opcode, operands...) with the count
    /// fixed per opcode -- except the two switch opcodes, which carry their own
    /// length as the first operand.
    int[] code = new int[0];

    /// Instruction index -> offset into `code`. Jump operands are instruction
    /// indices, which this turns into a code offset.
    int[] instructionOffsets = new int[0];

    /// Exception table, four ints per entry: startInsn, endInsn (exclusive),
    /// handlerInsn, typeExtern (-1 for `finally` / catch-all).
    int[] exceptionTable = new int[0];

    /// Line numbers, two ints per entry: instruction index, source line.
    int[] lineTable = new int[0];

    /// Argument kinds, one RET_* per declared parameter, used to pop a call's
    /// arguments in the right widths.
    int[] argKinds = new int[0];

    /// The kind of the declared return type.
    ///
    /// Needed because `ireturn` carries boolean, byte, char, short and int
    /// alike -- the JVM keeps them all in an int-sized slot -- so the value on
    /// the stack cannot say which one it is. Only the descriptor knows, and the
    /// caller unboxes by the descriptor.
    int returnKind = InterpOpcodes.RET_INT;

    InterpMethod(InterpClass owner) {
        this.owner = owner;
    }

    /// True for a static method.
    public boolean isStatic() {
        return (accessFlags & 0x0008) != 0;
    }

    /// A synchronized method holds its receiver's monitor -- or its class's,
    /// when static -- for the whole call. There is no `monitorenter` in the
    /// body; the flag is the only record of it.
    public boolean isSynchronized() {
        return (accessFlags & 0x0020) != 0;
    }

    /// Whether this method is private, which is what makes it not virtual.
    ///
    /// javac emits `invokevirtual` for a private method from JDK 11 onwards
    /// (nestmates replaced the synthetic access bridges), so the opcode alone
    /// no longer says whether dispatch follows the receiver.
    public boolean isPrivate() {
        return (accessFlags & 0x0002) != 0;
    }

    /// Whether this method is public.
    public boolean isPublic() {
        return (accessFlags & 0x0001) != 0;
    }

    /// Whether this method is protected.
    public boolean isProtected() {
        return (accessFlags & 0x0004) != 0;
    }

    /// True for an abstract method -- one with no code.
    public boolean isAbstract() {
        return (accessFlags & 0x0400) != 0;
    }

    /// The declaring class.
    public InterpClass getOwner() {
        return owner;
    }

    /// The method name; `<init>` for a constructor.
    public String getName() {
        return name;
    }

    /// The JVM descriptor.
    public String getDescriptor() {
        return desc;
    }

    /// The source line for an instruction index, or -1. Used to build a stack
    /// trace that names real lines in the user's file.
    public int lineFor(int instructionIndex) {
        int line = -1;
        for (int i = 0; i < lineTable.length; i += 2) {
            if (lineTable[i] <= instructionIndex) {
                line = lineTable[i + 1];
            } else {
                break;
            }
        }
        return line;
    }

    @Override
    public String toString() {
        return owner.name + "." + name + desc;
    }
}
