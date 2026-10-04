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
// This file is a rewrite of ASM's org.objectweb.asm.Type for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile;

import java.util.ArrayList;
import java.util.List;

/**
 * A field, method or array type, held as its JVM descriptor. This is the subset of
 * the usual descriptor model the translator reads: sorts, sizes, the class and
 * internal names, and the argument and return types of a method descriptor.
 */
public final class Type {
    public static final int VOID = 0;
    public static final int BOOLEAN = 1;
    public static final int CHAR = 2;
    public static final int BYTE = 3;
    public static final int SHORT = 4;
    public static final int INT = 5;
    public static final int FLOAT = 6;
    public static final int LONG = 7;
    public static final int DOUBLE = 8;
    public static final int ARRAY = 9;
    public static final int OBJECT = 10;
    public static final int METHOD = 11;

    public static final Type VOID_TYPE = new Type(VOID, "V");
    public static final Type BOOLEAN_TYPE = new Type(BOOLEAN, "Z");
    public static final Type CHAR_TYPE = new Type(CHAR, "C");
    public static final Type BYTE_TYPE = new Type(BYTE, "B");
    public static final Type SHORT_TYPE = new Type(SHORT, "S");
    public static final Type INT_TYPE = new Type(INT, "I");
    public static final Type FLOAT_TYPE = new Type(FLOAT, "F");
    public static final Type LONG_TYPE = new Type(LONG, "J");
    public static final Type DOUBLE_TYPE = new Type(DOUBLE, "D");

    private final int sort;
    private final String descriptor;

    private Type(int sort, String descriptor) {
        this.sort = sort;
        this.descriptor = descriptor;
    }

    /** The type a field descriptor, array descriptor or method descriptor names. */
    public static Type getType(String descriptor) {
        return forDescriptor(descriptor, 0, descriptor.length());
    }

    /** The class or array type an internal name (or an array descriptor) names. */
    public static Type getObjectType(String internalName) {
        if (internalName.length() > 0 && internalName.charAt(0) == '[') {
            return new Type(ARRAY, internalName);
        }
        return new Type(OBJECT, "L" + internalName + ";");
    }

    public static Type getMethodType(String methodDescriptor) {
        return new Type(METHOD, methodDescriptor);
    }

    public static Type[] getArgumentTypes(String methodDescriptor) {
        List<Type> args = new ArrayList<Type>();
        int i = 1;
        while (methodDescriptor.charAt(i) != ')') {
            int end = endOfField(methodDescriptor, i);
            args.add(forDescriptor(methodDescriptor, i, end));
            i = end;
        }
        return args.toArray(new Type[args.size()]);
    }

    public static Type getReturnType(String methodDescriptor) {
        int i = methodDescriptor.indexOf(')') + 1;
        return forDescriptor(methodDescriptor, i, methodDescriptor.length());
    }

    private static int endOfField(String d, int start) {
        int i = start;
        while (d.charAt(i) == '[') {
            i++;
        }
        if (d.charAt(i) == 'L') {
            return d.indexOf(';', i) + 1;
        }
        return i + 1;
    }

    private static Type forDescriptor(String d, int start, int end) {
        switch (d.charAt(start)) {
            case 'V': return VOID_TYPE;
            case 'Z': return BOOLEAN_TYPE;
            case 'C': return CHAR_TYPE;
            case 'B': return BYTE_TYPE;
            case 'S': return SHORT_TYPE;
            case 'I': return INT_TYPE;
            case 'F': return FLOAT_TYPE;
            case 'J': return LONG_TYPE;
            case 'D': return DOUBLE_TYPE;
            case '[': return new Type(ARRAY, d.substring(start, end));
            case 'L': return new Type(OBJECT, d.substring(start, end));
            case '(': return new Type(METHOD, d.substring(start, end));
            default:
                throw new IllegalArgumentException("Invalid descriptor: " + d);
        }
    }

    public int getSort() {
        return sort;
    }

    public String getDescriptor() {
        return descriptor;
    }

    /** The internal name of a class type, or the descriptor of an array type. */
    public String getInternalName() {
        if (sort == OBJECT) {
            return descriptor.substring(1, descriptor.length() - 1);
        }
        return descriptor;
    }

    /** The Java language name: {@code int}, {@code java.lang.String}, {@code int[][]}. */
    public String getClassName() {
        switch (sort) {
            case VOID: return "void";
            case BOOLEAN: return "boolean";
            case CHAR: return "char";
            case BYTE: return "byte";
            case SHORT: return "short";
            case INT: return "int";
            case FLOAT: return "float";
            case LONG: return "long";
            case DOUBLE: return "double";
            case ARRAY: {
                StringBuilder b = new StringBuilder(getElementType().getClassName());
                for (int i = getDimensions(); i > 0; i--) {
                    b.append("[]");
                }
                return b.toString();
            }
            case OBJECT: return getInternalName().replace('/', '.');
            default: return null;
        }
    }

    public int getDimensions() {
        int n = 0;
        while (descriptor.charAt(n) == '[') {
            n++;
        }
        return n;
    }

    public Type getElementType() {
        int n = getDimensions();
        return forDescriptor(descriptor, n, descriptor.length());
    }

    public Type[] getArgumentTypes() {
        return getArgumentTypes(descriptor);
    }

    public Type getReturnType() {
        return getReturnType(descriptor);
    }

    /** Stack and local-variable slots a value of this type occupies. */
    public int getSize() {
        switch (sort) {
            case VOID: return 0;
            case LONG:
            case DOUBLE: return 2;
            default: return 1;
        }
    }

    /**
     * Adapts an {@code ILOAD}, {@code ISTORE}, {@code IALOAD}, {@code IASTORE},
     * {@code IRETURN} or int arithmetic opcode to this type, e.g. {@code ILOAD} to
     * {@code ALOAD} for a reference and {@code IRETURN} to {@code RETURN} for void.
     */
    public int getOpcode(int opcode) {
        if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
            switch (sort) {
                case BOOLEAN:
                case BYTE: return opcode + (Opcodes.BALOAD - Opcodes.IALOAD);
                case CHAR: return opcode + (Opcodes.CALOAD - Opcodes.IALOAD);
                case SHORT: return opcode + (Opcodes.SALOAD - Opcodes.IALOAD);
                case INT: return opcode;
                case FLOAT: return opcode + (Opcodes.FALOAD - Opcodes.IALOAD);
                case LONG: return opcode + (Opcodes.LALOAD - Opcodes.IALOAD);
                case DOUBLE: return opcode + (Opcodes.DALOAD - Opcodes.IALOAD);
                case ARRAY:
                case OBJECT: return opcode + (Opcodes.AALOAD - Opcodes.IALOAD);
                default: throw new IllegalArgumentException("Invalid array element type " + descriptor);
            }
        }
        switch (sort) {
            case VOID:
                if (opcode != Opcodes.IRETURN) {
                    throw new IllegalArgumentException("Invalid opcode for void " + opcode);
                }
                return Opcodes.RETURN;
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case SHORT:
            case INT: return opcode;
            case FLOAT: return opcode + (Opcodes.FRETURN - Opcodes.IRETURN);
            case LONG: return opcode + (Opcodes.LRETURN - Opcodes.IRETURN);
            case DOUBLE: return opcode + (Opcodes.DRETURN - Opcodes.IRETURN);
            case ARRAY:
            case OBJECT:
                if (opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
                    throw new IllegalArgumentException("Invalid opcode for a reference " + opcode);
                }
                return opcode + (Opcodes.ARETURN - Opcodes.IRETURN);
            default:
                throw new IllegalArgumentException("Invalid type for an opcode " + descriptor);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Type && ((Type) o).descriptor.equals(descriptor);
    }

    @Override
    public int hashCode() {
        return 13 * sort + descriptor.hashCode();
    }

    @Override
    public String toString() {
        return descriptor;
    }
}
