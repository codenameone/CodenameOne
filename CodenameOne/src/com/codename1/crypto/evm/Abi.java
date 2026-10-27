/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.evm;

import com.codename1.crypto.UInt256;
import com.codename1.crypto.encoding.Hex;
import com.codename1.security.Hash;
import java.util.ArrayList;
import java.util.List;

/// The Solidity contract ABI: function selectors, call data and return values.
///
/// ```java
/// EvmAddress recipient = EvmAddress.parse("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed");
/// UInt256 amount = UInt256.parseUnits("25", 6);
/// byte[] data = Abi.encodeCall("transfer(address,uint256)", recipient, amount);
/// byte[] returned = new byte[32];                   // what eth_call answered
/// List<Object> out = Abi.decode("(uint256)", returned);   // [UInt256]
/// ```
///
/// Types are written as in a Solidity signature: `uint8`..`uint256`, `int8`..`int256`,
/// `address`, `bool`, `bytes1`..`bytes32`, `bytes`, `string`, arrays `T[]` and `T[k]`, and
/// tuples `(T1,T2)`. Values map to Java as:
///
/// | ABI | encode accepts | decode answers |
/// |---|---|---|
/// | `uintN` | `UInt256`, `Long`, `Integer`, decimal or `0x` `String` | `UInt256` |
/// | `intN` | `Long`, `Integer`, decimal `String` | `Long` up to 64 bits, decimal `String` above |
/// | `address` | `EvmAddress` or its `String` | `EvmAddress` |
/// | `bool` | `Boolean` | `Boolean` |
/// | `bytesN`, `bytes` | `byte[]` | `byte[]` |
/// | `string` | `String` | `String` |
/// | arrays, tuples | `List` or `Object[]` | `List<Object>` |
@com.codename1.impl.SharedWithBackend
public final class Abi {
    private Abi() {
    }

    /// The 4-byte selector of a function signature such as `transfer(address,uint256)`.
    public static byte[] selector(String signature) {
        byte[] hash = Hash.keccak256(utf8(canonical(signature)));
        byte[] out = new byte[4];
        System.arraycopy(hash, 0, out, 0, 4);
        return out;
    }

    /// The topic 0 of an event signature such as `Transfer(address,address,uint256)`.
    public static byte[] eventTopic(String signature) {
        return Hash.keccak256(utf8(canonical(signature)));
    }

    /// The call data for a function: its selector followed by the encoded arguments.
    ///
    /// #### Parameters
    ///
    /// - `signature`: `name(type1,type2,...)`
    ///
    /// - `args`: one value per parameter
    public static byte[] encodeCall(String signature, Object... args) {
        String sig = canonical(signature);
        int open = sig.indexOf('(');
        if (open <= 0 || !sig.endsWith(")")) {
            throw new IllegalArgumentException("not a function signature: " + signature);
        }
        byte[] params = encode(sig.substring(open), args);
        byte[] sel = selector(sig);
        byte[] out = new byte[4 + params.length];
        System.arraycopy(sel, 0, out, 0, 4);
        System.arraycopy(params, 0, out, 4, params.length);
        return out;
    }

    /// Encodes values against a tuple type such as `(address,uint256[],string)`.
    public static byte[] encode(String tupleType, Object... values) {
        Type t = Type.parse(canonical(tupleType));
        if (t.kind != Type.TUPLE) {
            throw new IllegalArgumentException("encode takes a tuple type such as (uint256)");
        }
        return t.encode(values);
    }

    /// Decodes ABI bytes against a tuple type, answering one value per member.
    public static List<Object> decode(String tupleType, byte[] data) {
        Type t = Type.parse(canonical(tupleType));
        if (t.kind != Type.TUPLE) {
            throw new IllegalArgumentException("decode takes a tuple type such as (uint256)");
        }
        return t.decodeMembers(data, 0);
    }

    /// [#decode] from `0x` hex, the form `eth_call` returns.
    public static List<Object> decodeHex(String tupleType, String hex) {
        return decode(tupleType, Hex.decode(hex));
    }

    /// Normalizes a signature: drops spaces and expands `uint`/`int` to their 256-bit names.
    static String canonical(String signature) {
        StringBuilder compact = new StringBuilder();
        for (int iter = 0; iter < signature.length(); iter++) {
            char c = signature.charAt(iter);
            if (c != ' ' && c != '\t' && c != '\n') {
                compact.append(c);
            }
        }
        String s = compact.toString();
        StringBuilder out = new StringBuilder();
        int iter = 0;
        while (iter < s.length()) {
            int end = iter;
            while (end < s.length() && isIdent(s.charAt(end))) {
                end++;
            }
            if (end == iter) {
                out.append(s.charAt(iter));
                iter++;
                continue;
            }
            String word = s.substring(iter, end);
            boolean typePosition = iter > 0;
            if (typePosition && ("uint".equals(word) || "int".equals(word))) {
                out.append(word).append("256");
            } else {
                out.append(word);
            }
            iter = end;
        }
        return out.toString();
    }

    private static boolean isIdent(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '$';
    }

    static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException("UTF-8 is always supported", impossible);
        }
    }

    static String fromUtf8(byte[] b) {
        try {
            return new String(b, "UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException("UTF-8 is always supported", impossible);
        }
    }

    /// A parsed ABI type.
    static final class Type {
        static final int UINT = 0;
        static final int INT = 1;
        static final int ADDRESS = 2;
        static final int BOOL = 3;
        static final int FIXED_BYTES = 4;
        static final int BYTES = 5;
        static final int STRING = 6;
        static final int ARRAY = 7;
        static final int FIXED_ARRAY = 8;
        static final int TUPLE = 9;

        final int kind;
        final int size;
        final Type element;
        final Type[] members;
        final String name;

        Type(int kind, int size, Type element, Type[] members, String name) {
            this.kind = kind;
            this.size = size;
            this.element = element;
            this.members = members;
            this.name = name;
        }

        static Type parse(String text) {
            int[] pos = {0};
            Type t = parseAt(text, pos);
            if (pos[0] != text.length()) {
                throw new IllegalArgumentException("bad ABI type: " + text);
            }
            return t;
        }

        private static Type parseAt(String s, int[] pos) {
            Type base;
            if (pos[0] < s.length() && s.charAt(pos[0]) == '(') {
                pos[0]++;
                List<Type> members = new ArrayList<Type>();
                if (pos[0] < s.length() && s.charAt(pos[0]) == ')') {
                    pos[0]++;
                } else {
                    while (true) {
                        members.add(parseAt(s, pos));
                        if (pos[0] >= s.length()) {
                            throw new IllegalArgumentException("unterminated tuple: " + s);
                        }
                        char c = s.charAt(pos[0]++);
                        if (c == ')') {
                            break;
                        }
                        if (c != ',') {
                            throw new IllegalArgumentException("bad ABI type: " + s);
                        }
                    }
                }
                Type[] m = members.toArray(new Type[members.size()]);
                StringBuilder name = new StringBuilder("(");
                for (int iter = 0; iter < m.length; iter++) {
                    if (iter > 0) {
                        name.append(',');
                    }
                    name.append(m[iter].name);
                }
                base = new Type(TUPLE, m.length, null, m, name.append(')').toString());
            } else {
                int start = pos[0];
                while (pos[0] < s.length() && isIdent(s.charAt(pos[0]))) {
                    pos[0]++;
                }
                base = elementary(s.substring(start, pos[0]));
            }
            while (pos[0] < s.length() && s.charAt(pos[0]) == '[') {
                int close = s.indexOf(']', pos[0]);
                if (close < 0) {
                    throw new IllegalArgumentException("bad array type: " + s);
                }
                String len = s.substring(pos[0] + 1, close);
                pos[0] = close + 1;
                if (len.length() == 0) {
                    base = new Type(ARRAY, 0, base, null, base.name + "[]");
                } else {
                    int k = Integer.parseInt(len);
                    if (k <= 0) {
                        throw new IllegalArgumentException("bad array length: " + s);
                    }
                    base = new Type(FIXED_ARRAY, k, base, null, base.name + "[" + k + "]");
                }
            }
            return base;
        }

        private static Type elementary(String n) {
            if ("address".equals(n)) {
                return new Type(ADDRESS, 160, null, null, n);
            }
            if ("bool".equals(n)) {
                return new Type(BOOL, 8, null, null, n);
            }
            if ("string".equals(n)) {
                return new Type(STRING, 0, null, null, n);
            }
            if ("bytes".equals(n)) {
                return new Type(BYTES, 0, null, null, n);
            }
            if (n.startsWith("bytes")) {
                int k = number(n.substring(5), n);
                if (k < 1 || k > 32) {
                    throw new IllegalArgumentException("bad ABI type: " + n);
                }
                return new Type(FIXED_BYTES, k, null, null, n);
            }
            if (n.startsWith("uint") || n.startsWith("int")) {
                boolean signed = n.startsWith("int");
                String bits = n.substring(signed ? 3 : 4);
                int m = bits.length() == 0 ? 256 : number(bits, n);
                if (m < 8 || m > 256 || m % 8 != 0) {
                    throw new IllegalArgumentException("bad ABI type: " + n);
                }
                String full = (signed ? "int" : "uint") + m;
                return new Type(signed ? INT : UINT, m, null, null, full);
            }
            throw new IllegalArgumentException("unsupported ABI type: " + n);
        }

        private static int number(String digits, String type) {
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException bad) {
                throw new IllegalArgumentException("bad ABI type: " + type, bad);
            }
        }

        boolean isDynamic() {
            switch (kind) {
                case BYTES:
                case STRING:
                case ARRAY:
                    return true;
                case FIXED_ARRAY:
                    return element.isDynamic();
                case TUPLE:
                    for (Type membersElem : members) {
                        if (membersElem.isDynamic()) {
                            return true;
                        }
                    }
                    return false;
                default:
                    return false;
            }
        }

        /// The size of the head of a static type, in bytes.
        int headSize() {
            if (isDynamic()) {
                return 32;
            }
            if (kind == FIXED_ARRAY) {
                return size * element.headSize();
            }
            if (kind == TUPLE) {
                int total = 0;
                for (Type membersElem : members) {
                    total += membersElem.headSize();
                }
                return total;
            }
            return 32;
        }

        byte[] encode(Object value) {
            switch (kind) {
                case UINT:
                    return uintWord(value);
                case INT:
                    return intWord(value);
                case ADDRESS: {
                    EvmAddress a = value instanceof EvmAddress ? (EvmAddress) value : EvmAddress.parse(str(value));
                    byte[] out = new byte[32];
                    System.arraycopy(a.toBytes(), 0, out, 12, 20);
                    return out;
                }
                case BOOL: {
                    if (!(value instanceof Boolean)) {
                        throw new IllegalArgumentException("bool takes a Boolean");
                    }
                    byte[] out = new byte[32];
                    out[31] = (byte) (((Boolean) value).booleanValue() ? 1 : 0);
                    return out;
                }
                case FIXED_BYTES: {
                    byte[] b = raw(value);
                    if (b.length != size) {
                        throw new IllegalArgumentException(name + " takes exactly " + size + " bytes");
                    }
                    byte[] out = new byte[32];
                    System.arraycopy(b, 0, out, 0, size);
                    return out;
                }
                case BYTES:
                    return dynamicBytes(raw(value));
                case STRING:
                    return dynamicBytes(utf8(str(value)));
                case ARRAY: {
                    Object[] items = items(value);
                    byte[] body = sequence(element, items);
                    return concat(UInt256.valueOf(items.length).toBytes32(), body);
                }
                case FIXED_ARRAY: {
                    Object[] items = items(value);
                    if (items.length != size) {
                        throw new IllegalArgumentException(name + " takes " + size + " items");
                    }
                    return sequence(element, items);
                }
                default: {
                    Object[] items = items(value);
                    if (items.length != members.length) {
                        throw new IllegalArgumentException(name + " takes " + members.length + " values, got "
                                + items.length);
                    }
                    Type[] types = members;
                    return heads(types, items);
                }
            }
        }

        private static byte[] sequence(Type element, Object[] items) {
            Type[] types = new Type[items.length];
            for (int iter = 0; iter < types.length; iter++) {
                types[iter] = element;
            }
            return heads(types, items);
        }

        private static byte[] heads(Type[] types, Object[] items) {
            int headLen = 0;
            for (Type typesItem : types) {
                headLen += typesItem.headSize();
            }
            java.io.ByteArrayOutputStream head = new java.io.ByteArrayOutputStream();
            java.io.ByteArrayOutputStream tail = new java.io.ByteArrayOutputStream();
            for (int iter = 0; iter < types.length; iter++) {
                byte[] enc = types[iter].encode(items[iter]);
                if (types[iter].isDynamic()) {
                    write(head, UInt256.valueOf(headLen + tail.size()).toBytes32());
                    write(tail, enc);
                } else {
                    write(head, enc);
                }
            }
            write(head, tail.toByteArray());
            return head.toByteArray();
        }

        Object decode(byte[] data, int base, int at) {
            switch (kind) {
                case UINT: {
                    UInt256 v = UInt256.fromBytes(word(data, at));
                    if (v.bitLength() > size) {
                        throw new IllegalArgumentException(name + " out of range");
                    }
                    return v;
                }
                case INT:
                    return decodeInt(word(data, at));
                case ADDRESS: {
                    byte[] w = word(data, at);
                    for (int iter = 0; iter < 12; iter++) {
                        if (w[iter] != 0) {
                            throw new IllegalArgumentException("dirty address word");
                        }
                    }
                    byte[] a = new byte[20];
                    System.arraycopy(w, 12, a, 0, 20);
                    return EvmAddress.fromBytes(a);
                }
                case BOOL: {
                    UInt256 v = UInt256.fromBytes(word(data, at));
                    if (v.bitLength() > 1) {
                        throw new IllegalArgumentException("bool out of range");
                    }
                    return Boolean.valueOf(!v.isZero());
                }
                case FIXED_BYTES: {
                    byte[] w = word(data, at);
                    byte[] out = new byte[size];
                    System.arraycopy(w, 0, out, 0, size);
                    return out;
                }
                case BYTES:
                case STRING: {
                    int off = base + offset(data, at);
                    int len = offset(data, off);
                    if (off + 32 + len > data.length) {
                        throw new IllegalArgumentException("ABI data truncated");
                    }
                    byte[] out = new byte[len];
                    System.arraycopy(data, off + 32, out, 0, len);
                    return kind == STRING ? fromUtf8(out) : out;
                }
                case ARRAY: {
                    int off = base + offset(data, at);
                    int len = offset(data, off);
                    if ((long) len * 32 > data.length) {
                        throw new IllegalArgumentException("ABI array length too large");
                    }
                    return decodeSequence(element, len, data, off + 32);
                }
                case FIXED_ARRAY:
                    if (isDynamic()) {
                        return decodeSequence(element, size, data, base + offset(data, at));
                    }
                    return decodeSequence(element, size, data, at);
                default:
                    return decodeMembers(data, isDynamic() ? base + offset(data, at) : at);
            }
        }

        /// The members of a tuple whose encoding starts at `start`.
        List<Object> decodeMembers(byte[] data, int start) {
            List<Object> out = new ArrayList<Object>();
            int pos = start;
            for (Type membersItem : members) {
                out.add(membersItem.decode(data, start, pos));
                pos += membersItem.headSize();
            }
            return out;
        }

        private static List<Object> decodeSequence(Type element, int count, byte[] data, int start) {
            List<Object> out = new ArrayList<Object>();
            int pos = start;
            for (int iter = 0; iter < count; iter++) {
                out.add(element.decode(data, start, pos));
                pos += element.headSize();
            }
            return out;
        }

        private Object decodeInt(byte[] w) {
            boolean negative = w[0] < 0;
            int fill = negative ? 0xff : 0;
            for (int iter = 0; iter < 32 - size / 8; iter++) {
                if ((w[iter] & 0xff) != fill) {
                    throw new IllegalArgumentException(name + " out of range");
                }
            }
            if (32 - size / 8 > 0 && size < 256) {
                int top = w[32 - size / 8] & 0x80;
                if ((top != 0) != negative) {
                    throw new IllegalArgumentException(name + " out of range");
                }
            }
            if (size <= 64) {
                long v = 0;
                for (int iter = 24; iter < 32; iter++) {
                    v = (v << 8) | (w[iter] & 0xff);
                }
                return Long.valueOf(v);
            }
            if (!negative) {
                return UInt256.fromBytes(w).toString();
            }
            // magnitude = ~w + 1
            byte[] inv = new byte[32];
            for (int iter = 0; iter < 32; iter++) {
                inv[iter] = (byte) ~w[iter];
            }
            return "-" + UInt256.fromBytes(inv).add(UInt256.ONE).toString();
        }

        private byte[] uintWord(Object value) {
            UInt256 v;
            if (value instanceof UInt256) {
                v = (UInt256) value;
            } else if (value instanceof Long || value instanceof Integer || value instanceof Short
                    || value instanceof Byte) {
                v = UInt256.valueOf(((Number) value).longValue());
            } else if (value instanceof String) {
                v = UInt256.parse((String) value);
            } else {
                throw new IllegalArgumentException(name + " takes a UInt256, an integer or a numeric String");
            }
            if (v.bitLength() > size) {
                throw new IllegalArgumentException("value does not fit in " + name);
            }
            return v.toBytes32();
        }

        private byte[] intWord(Object value) {
            boolean negative;
            UInt256 magnitude;
            if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
                long l = ((Number) value).longValue();
                if (l == Long.MIN_VALUE) {
                    negative = true;
                    magnitude = UInt256.ONE.shiftLeft(63);
                } else {
                    negative = l < 0;
                    magnitude = UInt256.valueOf(negative ? -l : l);
                }
            } else if (value instanceof String) {
                String s = (String) value;
                negative = s.startsWith("-");
                magnitude = UInt256.parse(negative ? s.substring(1) : s, 10);
            } else if (value instanceof UInt256) {
                negative = false;
                magnitude = (UInt256) value;
            } else {
                throw new IllegalArgumentException(name + " takes an integer or a decimal String");
            }
            int bits = magnitude.bitLength();
            boolean fits = negative ? (bits < size || (bits == size && magnitude.equals(UInt256.ONE.shiftLeft(size - 1))))
                    : bits < size;
            if (!fits) {
                throw new IllegalArgumentException("value does not fit in " + name);
            }
            byte[] w = magnitude.toBytes32();
            if (!negative || magnitude.isZero()) {
                return w;
            }
            int carry = 1;
            for (int iter = 31; iter >= 0; iter--) {
                int b = (~w[iter] & 0xff) + carry;
                w[iter] = (byte) b;
                carry = b >> 8;
            }
            return w;
        }

        private static byte[] dynamicBytes(byte[] b) {
            int padded = (b.length + 31) / 32 * 32;
            byte[] out = new byte[32 + padded];
            System.arraycopy(UInt256.valueOf(b.length).toBytes32(), 0, out, 0, 32);
            System.arraycopy(b, 0, out, 32, b.length);
            return out;
        }

        private static byte[] word(byte[] data, int at) {
            if (at < 0 || at + 32 > data.length) {
                throw new IllegalArgumentException("ABI data truncated");
            }
            byte[] out = new byte[32];
            System.arraycopy(data, at, out, 0, 32);
            return out;
        }

        private static int offset(byte[] data, int at) {
            UInt256 v = UInt256.fromBytes(word(data, at));
            if (v.bitLength() > 31 || v.longValueExact() > data.length) {
                throw new IllegalArgumentException("ABI offset out of range");
            }
            return (int) v.longValueExact();
        }

        private static Object[] items(Object value) {
            if (value instanceof Object[]) {
                return (Object[]) value;
            }
            if (value instanceof List) {
                return ((List<?>) value).toArray();
            }
            throw new IllegalArgumentException("arrays and tuples take a List or an Object[]");
        }

        private static byte[] raw(Object value) {
            if (value instanceof byte[]) {
                return (byte[]) value;
            }
            if (value instanceof String) {
                return Hex.decode((String) value);
            }
            throw new IllegalArgumentException("bytes take a byte[] or a 0x String");
        }

        private static String str(Object value) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("expected a String");
            }
            return (String) value;
        }

        private static void write(java.io.ByteArrayOutputStream out, byte[] b) {
            out.write(b, 0, b.length);
        }
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
