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

import java.util.Arrays;

/// A type as a signature spells it. Immutable, and compared structurally, so
/// two signatures read from different assemblies match when they mean the
/// same thing.
///
/// A named type carries its full CIL name: namespace, a dot, the name, and
/// `/` between a nested type and the type enclosing it. Generic arity stays in
/// the name as the compiler wrote it (``List`1``).
public final class CilType {
    public enum Kind {
        VOID, BOOLEAN, CHAR, I1, U1, I2, U2, I4, U4, I8, U8, R4, R8, I, U, STRING, OBJECT,
        CLASS, VALUETYPE, VAR, MVAR, SZARRAY, ARRAY, GENERICINST, BYREF, PTR, TYPEDBYREF, FNPTR
    }

    // Above the constants on purpose: their constructor reads it, and a
    // static declared below them is still null while they are built.
    private static final CilType[] NO_ARGS = new CilType[0];

    public static final CilType VOID = new CilType(Kind.VOID, null, null, null, 0);
    public static final CilType BOOLEAN = new CilType(Kind.BOOLEAN, null, null, null, 0);
    public static final CilType CHAR = new CilType(Kind.CHAR, null, null, null, 0);
    public static final CilType I1 = new CilType(Kind.I1, null, null, null, 0);
    public static final CilType U1 = new CilType(Kind.U1, null, null, null, 0);
    public static final CilType I2 = new CilType(Kind.I2, null, null, null, 0);
    public static final CilType U2 = new CilType(Kind.U2, null, null, null, 0);
    public static final CilType I4 = new CilType(Kind.I4, null, null, null, 0);
    public static final CilType U4 = new CilType(Kind.U4, null, null, null, 0);
    public static final CilType I8 = new CilType(Kind.I8, null, null, null, 0);
    public static final CilType U8 = new CilType(Kind.U8, null, null, null, 0);
    public static final CilType R4 = new CilType(Kind.R4, null, null, null, 0);
    public static final CilType R8 = new CilType(Kind.R8, null, null, null, 0);
    public static final CilType I = new CilType(Kind.I, null, null, null, 0);
    public static final CilType U = new CilType(Kind.U, null, null, null, 0);
    public static final CilType STRING = new CilType(Kind.STRING, null, null, null, 0);
    public static final CilType OBJECT = new CilType(Kind.OBJECT, null, null, null, 0);
    public static final CilType TYPEDBYREF = new CilType(Kind.TYPEDBYREF, null, null, null, 0);
    public static final CilType FNPTR = new CilType(Kind.FNPTR, null, null, null, 0);

    public final Kind kind;
    /// Full name, for CLASS and VALUETYPE.
    public final String name;
    /// Element of an array, byref or pointer; the generic definition of a GENERICINST.
    public final CilType element;
    /// Type arguments of a GENERICINST.
    public final CilType[] args;
    /// Position of a VAR or MVAR; rank of an ARRAY.
    public final int index;

    private CilType(Kind kind, String name, CilType element, CilType[] args, int index) {
        this.kind = kind;
        this.name = name;
        this.element = element;
        this.args = args == null ? NO_ARGS : args;
        this.index = index;
    }

    public static CilType named(String name, boolean valueType) {
        return new CilType(valueType ? Kind.VALUETYPE : Kind.CLASS, name, null, null, 0);
    }

    public static CilType var(int index) {
        return new CilType(Kind.VAR, null, null, null, index);
    }

    public static CilType mvar(int index) {
        return new CilType(Kind.MVAR, null, null, null, index);
    }

    public static CilType szArray(CilType element) {
        return new CilType(Kind.SZARRAY, null, element, null, 1);
    }

    public static CilType array(CilType element, int rank) {
        return new CilType(Kind.ARRAY, null, element, null, rank);
    }

    public static CilType byRef(CilType element) {
        return new CilType(Kind.BYREF, null, element, null, 0);
    }

    public static CilType pointer(CilType element) {
        return new CilType(Kind.PTR, null, element, null, 0);
    }

    public static CilType genericInst(CilType definition, CilType[] args) {
        return new CilType(Kind.GENERICINST, null, definition, args, 0);
    }

    /// The named type behind this one: itself for a class or value type, the
    /// definition for an instantiation, null for everything else.
    public CilType definition() {
        if (kind == Kind.GENERICINST) {
            return element;
        }
        return kind == Kind.CLASS || kind == Kind.VALUETYPE ? this : null;
    }

    /// Full name of the named type behind this one, or the `System` name a
    /// built-in stands for. Null for arrays, variables and pointers.
    public String typeName() {
        switch (kind) {
            case CLASS:
            case VALUETYPE:
                return name;
            case GENERICINST:
                return element.name;
            case BOOLEAN:
                return "System.Boolean";
            case CHAR:
                return "System.Char";
            case I1:
                return "System.SByte";
            case U1:
                return "System.Byte";
            case I2:
                return "System.Int16";
            case U2:
                return "System.UInt16";
            case I4:
                return "System.Int32";
            case U4:
                return "System.UInt32";
            case I8:
                return "System.Int64";
            case U8:
                return "System.UInt64";
            case R4:
                return "System.Single";
            case R8:
                return "System.Double";
            case I:
                return "System.IntPtr";
            case U:
                return "System.UIntPtr";
            case STRING:
                return "System.String";
            case OBJECT:
                return "System.Object";
            case VOID:
                return "System.Void";
            default:
                return null;
        }
    }

    /// The built-in a `System` name stands for, so that a reference to
    /// `System.Int32` and the `int32` element type are one type. Null when
    /// the name is not a built-in.
    public static CilType builtin(String fullName) {
        if (!fullName.startsWith("System.")) {
            return null;
        }
        switch (fullName) {
            case "System.Boolean":
                return BOOLEAN;
            case "System.Char":
                return CHAR;
            case "System.SByte":
                return I1;
            case "System.Byte":
                return U1;
            case "System.Int16":
                return I2;
            case "System.UInt16":
                return U2;
            case "System.Int32":
                return I4;
            case "System.UInt32":
                return U4;
            case "System.Int64":
                return I8;
            case "System.UInt64":
                return U8;
            case "System.Single":
                return R4;
            case "System.Double":
                return R8;
            case "System.IntPtr":
                return I;
            case "System.UIntPtr":
                return U;
            case "System.String":
                return STRING;
            case "System.Object":
                return OBJECT;
            case "System.Void":
                return VOID;
            default:
                return null;
        }
    }

    public boolean isPrimitive() {
        switch (kind) {
            case BOOLEAN:
            case CHAR:
            case I1:
            case U1:
            case I2:
            case U2:
            case I4:
            case U4:
            case I8:
            case U8:
            case R4:
            case R8:
            case I:
            case U:
                return true;
            default:
                return false;
        }
    }

    /// Replaces type variables with the arguments of an instantiation. Either
    /// array may be null when that level is not instantiated.
    public CilType substitute(CilType[] typeArgs, CilType[] methodArgs) {
        switch (kind) {
            case VAR:
                return typeArgs != null && index < typeArgs.length ? typeArgs[index] : this;
            case MVAR:
                return methodArgs != null && index < methodArgs.length ? methodArgs[index] : this;
            case SZARRAY:
                return szArray(element.substitute(typeArgs, methodArgs));
            case ARRAY:
                return array(element.substitute(typeArgs, methodArgs), index);
            case BYREF:
                return byRef(element.substitute(typeArgs, methodArgs));
            case PTR:
                return pointer(element.substitute(typeArgs, methodArgs));
            case GENERICINST: {
                CilType[] out = new CilType[args.length];
                for (int i = 0; i < out.length; i++) {
                    out[i] = args[i].substitute(typeArgs, methodArgs);
                }
                return genericInst(element, out);
            }
            default:
                return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CilType)) {
            return false;
        }
        CilType t = (CilType) o;
        if (kind != t.kind || index != t.index) {
            return false;
        }
        if (name == null ? t.name != null : !name.equals(t.name)) {
            return false;
        }
        if (element == null ? t.element != null : !element.equals(t.element)) {
            return false;
        }
        return Arrays.equals(args, t.args);
    }

    @Override
    public int hashCode() {
        int h = kind.ordinal() * 31 + index;
        if (name != null) {
            h = h * 31 + name.hashCode();
        }
        if (element != null) {
            h = h * 31 + element.hashCode();
        }
        return h * 31 + Arrays.hashCode(args);
    }

    @Override
    public String toString() {
        switch (kind) {
            case CLASS:
                return name;
            case VALUETYPE:
                return "valuetype " + name;
            case VAR:
                return "!" + index;
            case MVAR:
                return "!!" + index;
            case SZARRAY:
                return element + "[]";
            case ARRAY:
                return element + "[rank " + index + "]";
            case BYREF:
                return element + "&";
            case PTR:
                return element + "*";
            case GENERICINST: {
                StringBuilder sb = new StringBuilder(element.toString()).append('<');
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(args[i]);
                }
                return sb.append('>').toString();
            }
            default:
                return kind.name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
