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

/// Decodes the signature blobs of ECMA-335 II.23.2: field, method, local
/// variable and generic instantiation signatures, and the types inside them.
final class SignatureParser {
    private final CilAssembly assembly;
    private final MetadataReader md;
    private int at;

    SignatureParser(CilAssembly assembly, int at) {
        this.assembly = assembly;
        this.md = assembly.metadata();
        this.at = at;
    }

    private int compressed() {
        int first = md.u1(at);
        if ((first & 0x80) == 0) {
            at += 1;
            return first;
        }
        if ((first & 0xC0) == 0x80) {
            int value = ((first & 0x3F) << 8) | md.u1(at + 1);
            at += 2;
            return value;
        }
        int value = ((first & 0x1F) << 24) | (md.u1(at + 1) << 16) | (md.u1(at + 2) << 8) | md.u1(at + 3);
        at += 4;
        return value;
    }

    private int typeDefOrRefToken() {
        return md.codedToken(MetadataReader.TYPE_DEF_OR_REF, compressed());
    }

    private CilFormatException bad(String what) {
        return new CilFormatException(md.source() + ": " + what);
    }

    // A signature states outright whether a named type is a class or a value
    // type, so nothing has to be resolved to read one.
    private CilType named(boolean valueType) {
        int token = typeDefOrRefToken();
        if (MetadataReader.tokenTable(token) == MetadataReader.TYPE_SPEC) {
            return assembly.typeFromToken(token);
        }
        String fullName = assembly.typeNameFromToken(token);
        CilType builtin = CilType.builtin(fullName);
        return builtin != null ? builtin : CilType.named(fullName, valueType);
    }

    CilType type() {
        while (true) {
            int element = md.u1(at++);
            switch (element) {
                case 0x01:
                    return CilType.VOID;
                case 0x02:
                    return CilType.BOOLEAN;
                case 0x03:
                    return CilType.CHAR;
                case 0x04:
                    return CilType.I1;
                case 0x05:
                    return CilType.U1;
                case 0x06:
                    return CilType.I2;
                case 0x07:
                    return CilType.U2;
                case 0x08:
                    return CilType.I4;
                case 0x09:
                    return CilType.U4;
                case 0x0A:
                    return CilType.I8;
                case 0x0B:
                    return CilType.U8;
                case 0x0C:
                    return CilType.R4;
                case 0x0D:
                    return CilType.R8;
                case 0x0E:
                    return CilType.STRING;
                case 0x0F:
                    return CilType.pointer(type());
                case 0x10:
                    return CilType.byRef(type());
                case 0x11:
                    return named(true);
                case 0x12:
                    return named(false);
                case 0x13:
                    return CilType.var(compressed());
                case 0x14: {
                    CilType elementType = type();
                    int rank = compressed();
                    int sizes = compressed();
                    for (int i = 0; i < sizes; i++) {
                        compressed();
                    }
                    int lowerBounds = compressed();
                    for (int i = 0; i < lowerBounds; i++) {
                        compressed();
                    }
                    return CilType.array(elementType, rank);
                }
                case 0x15: {
                    int definitionKind = md.u1(at++);
                    CilType definition = named(definitionKind == 0x11);
                    CilType[] args = new CilType[compressed()];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = type();
                    }
                    return CilType.genericInst(definition, args);
                }
                case 0x16:
                    return CilType.TYPEDBYREF;
                case 0x18:
                    return CilType.I;
                case 0x19:
                    return CilType.U;
                case 0x1B:
                    methodSig();
                    return CilType.FNPTR;
                case 0x1C:
                    return CilType.OBJECT;
                case 0x1D:
                    return CilType.szArray(type());
                case 0x1E:
                    return CilType.mvar(compressed());
                case 0x1F:
                case 0x20:
                    // A custom modifier (modreq/modopt) qualifies the type
                    // that follows it and does not change what that type is.
                    compressed();
                    break;
                case 0x41:
                case 0x45:
                    // Sentinel and pinned: markers in front of a type.
                    break;
                default:
                    throw bad("unknown element type 0x" + Integer.toHexString(element) + " in a signature");
            }
        }
    }

    CilType fieldSig() {
        int lead = md.u1(at++);
        if (lead != 0x06) {
            throw bad("expected a field signature, found 0x" + Integer.toHexString(lead));
        }
        return type();
    }

    CilAssembly.MethodSig methodSig() {
        int lead = md.u1(at++);
        boolean hasThis = (lead & 0x20) != 0;
        int genericParams = (lead & 0x10) != 0 ? compressed() : 0;
        CilType[] params = new CilType[compressed()];
        CilType returnType = type();
        for (int i = 0; i < params.length; i++) {
            params[i] = type();
        }
        return new CilAssembly.MethodSig(hasThis, genericParams, returnType, params);
    }

    CilType[] locals() {
        int lead = md.u1(at++);
        if (lead != 0x07) {
            throw bad("expected a local variable signature, found 0x" + Integer.toHexString(lead));
        }
        CilType[] out = new CilType[compressed()];
        for (int i = 0; i < out.length; i++) {
            out[i] = type();
        }
        return out;
    }

    CilType[] methodSpec() {
        int lead = md.u1(at++);
        if (lead != 0x0A) {
            throw bad("expected a generic instantiation, found 0x" + Integer.toHexString(lead));
        }
        CilType[] out = new CilType[compressed()];
        for (int i = 0; i < out.length; i++) {
            out[i] = type();
        }
        return out;
    }
}
