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

import java.util.Vector;

/// Descriptor parsing and the boxing convention at the interpreter's boundary.
///
/// Inside a frame primitives live unboxed in a `long[]`; they are only boxed
/// when they cross into host code, which is where a real `Integer` or `Double`
/// is what reflection expects. Keeping that conversion in one place is what
/// stops a sub-int type (`boolean`, `byte`, `char`, `short`) from being handed
/// over as the `int` it is stored as -- a mismatch reflection rejects at the
/// call rather than silently coercing.
///
/// @author Shai Almog
final class InterpValues {
    private InterpValues() {
    }

    /// The RET_* kind for a field or return descriptor.
    static int kindOf(String desc) {
        if (desc.length() == 0) {
            return InterpOpcodes.RET_VOID;
        }
        switch (desc.charAt(0)) {
            case 'V': return InterpOpcodes.RET_VOID;
            case 'Z': return InterpOpcodes.RET_BOOLEAN;
            case 'B': return InterpOpcodes.RET_BYTE;
            case 'C': return InterpOpcodes.RET_CHAR;
            case 'S': return InterpOpcodes.RET_SHORT;
            case 'I': return InterpOpcodes.RET_INT;
            case 'J': return InterpOpcodes.RET_LONG;
            case 'F': return InterpOpcodes.RET_FLOAT;
            case 'D': return InterpOpcodes.RET_DOUBLE;
            default:  return InterpOpcodes.RET_OBJECT;
        }
    }

    /// The kind of a method descriptor's return type.
    static int returnKind(String methodDesc) {
        int close = methodDesc.indexOf(')');
        return kindOf(methodDesc.substring(close + 1));
    }

    /// The kinds of a method descriptor's parameters, in order.
    static int[] argumentKinds(String methodDesc) {
        Vector kinds = new Vector();
        int i = 1;
        while (i < methodDesc.length() && methodDesc.charAt(i) != ')') {
            int start = i;
            char c = methodDesc.charAt(i);
            while (c == '[') {
                i++;
                c = methodDesc.charAt(i);
            }
            if (c == 'L') {
                i = methodDesc.indexOf(';', i) + 1;
            } else {
                i++;
            }
            kinds.addElement(Integer.valueOf(kindOf(methodDesc.substring(start, i))));
        }
        int[] result = new int[kinds.size()];
        for (int j = 0; j < result.length; j++) {
            result[j] = ((Integer)kinds.elementAt(j)).intValue();
        }
        return result;
    }

    /// The parameter type descriptors of a method descriptor, in order.
    static String[] argumentTypes(String methodDesc) {
        Vector types = new Vector();
        int i = 1;
        while (i < methodDesc.length() && methodDesc.charAt(i) != ')') {
            int start = i;
            char c = methodDesc.charAt(i);
            while (c == '[') {
                i++;
                c = methodDesc.charAt(i);
            }
            if (c == 'L') {
                i = methodDesc.indexOf(';', i) + 1;
            } else {
                i++;
            }
            types.addElement(methodDesc.substring(start, i));
        }
        String[] result = new String[types.size()];
        types.copyInto(result);
        return result;
    }

    /// The zero value a field of this descriptor starts at.
    static Object defaultValue(String desc) {
        switch (kindOf(desc)) {
            case InterpOpcodes.RET_BOOLEAN: return Boolean.FALSE;
            case InterpOpcodes.RET_BYTE:    return Byte.valueOf((byte)0);
            case InterpOpcodes.RET_CHAR:    return Character.valueOf((char)0);
            case InterpOpcodes.RET_SHORT:   return Short.valueOf((short)0);
            case InterpOpcodes.RET_INT:     return Integer.valueOf(0);
            case InterpOpcodes.RET_LONG:    return Long.valueOf(0L);
            case InterpOpcodes.RET_FLOAT:   return Float.valueOf(0f);
            case InterpOpcodes.RET_DOUBLE:  return Double.valueOf(0d);
            default: return null;
        }
    }

    /// The zero value for an already-computed kind, for a call that has to
    /// return something without having run anything.
    static Object defaultForKind(int kind) {
        switch (kind) {
            case InterpOpcodes.RET_BOOLEAN: return Boolean.FALSE;
            case InterpOpcodes.RET_BYTE:    return Byte.valueOf((byte)0);
            case InterpOpcodes.RET_CHAR:    return Character.valueOf((char)0);
            case InterpOpcodes.RET_SHORT:   return Short.valueOf((short)0);
            case InterpOpcodes.RET_INT:     return Integer.valueOf(0);
            case InterpOpcodes.RET_LONG:    return Long.valueOf(0L);
            case InterpOpcodes.RET_FLOAT:   return Float.valueOf(0f);
            case InterpOpcodes.RET_DOUBLE:  return Double.valueOf(0d);
            default: return null;
        }
    }

    /// Boxes a raw slot value for the crossing into host code.
    ///
    /// The kind matters: a `boolean` parameter is stored as 0/1 in a long slot,
    /// and reflection will not accept an `Integer` where it wants a `Boolean`.
    static Object box(int kind, long raw, Object ref) {
        switch (kind) {
            case InterpOpcodes.RET_BOOLEAN: return raw != 0 ? Boolean.TRUE : Boolean.FALSE;
            case InterpOpcodes.RET_BYTE:    return Byte.valueOf((byte)raw);
            case InterpOpcodes.RET_CHAR:    return Character.valueOf((char)raw);
            case InterpOpcodes.RET_SHORT:   return Short.valueOf((short)raw);
            case InterpOpcodes.RET_INT:     return Integer.valueOf((int)raw);
            case InterpOpcodes.RET_LONG:    return Long.valueOf(raw);
            case InterpOpcodes.RET_FLOAT:   return Float.valueOf(Float.intBitsToFloat((int)raw));
            case InterpOpcodes.RET_DOUBLE:  return Double.valueOf(Double.longBitsToDouble(raw));
            default: return ref;
        }
    }

    /// Unboxes a value returned by host code into a raw slot value. Widening is
    /// deliberate: the JVM keeps every sub-int type in an int-sized slot.
    static long unbox(int kind, Object value) {
        if (value == null) {
            return 0;
        }
        switch (kind) {
            case InterpOpcodes.RET_BOOLEAN:
                return ((Boolean)value).booleanValue() ? 1 : 0;
            case InterpOpcodes.RET_BYTE:
                return ((Byte)value).byteValue();
            case InterpOpcodes.RET_CHAR:
                return ((Character)value).charValue();
            case InterpOpcodes.RET_SHORT:
                return ((Short)value).shortValue();
            case InterpOpcodes.RET_INT:
                return ((Integer)value).intValue();
            case InterpOpcodes.RET_LONG:
                return ((Long)value).longValue();
            case InterpOpcodes.RET_FLOAT:
                return Float.floatToIntBits(((Float)value).floatValue()) & 0xffffffffL;
            case InterpOpcodes.RET_DOUBLE:
                return Double.doubleToLongBits(((Double)value).doubleValue());
            default:
                return 0;
        }
    }

    /// Turns a JVM internal name or descriptor into the form
    /// `Class.forName` expects.
    static String binaryName(String internalOrDescriptor) {
        if (internalOrDescriptor.startsWith("[")) {
            return internalOrDescriptor.replace('/', '.');
        }
        if (internalOrDescriptor.startsWith("L") && internalOrDescriptor.endsWith(";")) {
            return internalOrDescriptor.substring(1, internalOrDescriptor.length() - 1)
                    .replace('/', '.');
        }
        return internalOrDescriptor.replace('/', '.');
    }
}
