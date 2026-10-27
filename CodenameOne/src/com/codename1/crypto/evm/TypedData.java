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

import com.codename1.crypto.encoding.Json;
import com.codename1.security.Hash;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/// EIP-712 typed structured data: the `eth_signTypedData_v4` document permits, orders and
/// meta-transactions are signed as.
///
/// ```java
/// String jsonFromTheDapp = "{...}";           // the eth_signTypedData_v4 argument
/// byte[] privateKey = SecureRandom.bytes(32);
/// TypedData doc = TypedData.fromJson(jsonFromTheDapp);
/// byte[] signature = doc.sign(privateKey);      // r || s || v
/// ```
///
/// The document is the JSON a dapp sends: `types`, `primaryType`, `domain` and `message`.
/// When `types` omits `EIP712Domain`, it is derived from the domain fields present, as
/// wallets do.
@com.codename1.impl.SharedWithBackend
public final class TypedData {
    private static final String DOMAIN = "EIP712Domain";
    private static final String[][] DOMAIN_FIELDS = {
        {"name", "string"}, {"version", "string"}, {"chainId", "uint256"},
        {"verifyingContract", "address"}, {"salt", "bytes32"},
    };

    private final Map<String, List<String[]>> types;
    private final String primaryType;
    private final Map<String, Object> domain;
    private final Map<String, Object> message;

    /// A document from its parts.
    ///
    /// #### Parameters
    ///
    /// - `types`: struct name to its members, each `{name, type}`, in declaration order
    ///
    /// - `primaryType`: the struct `message` is an instance of
    ///
    /// - `domain`: the `EIP712Domain` values
    ///
    /// - `message`: the values of the primary struct
    public TypedData(Map<String, List<String[]>> types, String primaryType, Map<String, Object> domain,
            Map<String, Object> message) {
        this.types = new LinkedHashMap<String, List<String[]>>(types);
        this.primaryType = primaryType;
        this.domain = domain;
        this.message = message;
        if (!this.types.containsKey(DOMAIN)) {
            List<String[]> fields = new ArrayList<String[]>();
            for (String[] domainField : DOMAIN_FIELDS) {
                if (domain.containsKey(domainField[0])) {
                    fields.add(domainField);
                }
            }
            this.types.put(DOMAIN, fields);
        }
        if (!this.types.containsKey(primaryType)) {
            throw new IllegalArgumentException("primaryType " + primaryType + " is not among the types");
        }
    }

    /// Reads the `eth_signTypedData_v4` JSON document.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for malformed JSON or a document missing a part
    public static TypedData fromJson(String json) {
        Map<?, ?> root = map(Json.parse(json), "document");
        Map<?, ?> rawTypes = map(root.get("types"), "types");
        Map<String, List<String[]>> types = new LinkedHashMap<String, List<String[]>>();
        Iterator<?> it = rawTypes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
            Object members = e.getValue();
            if (!(members instanceof List)) {
                throw new IllegalArgumentException("type " + e.getKey() + " is not a list");
            }
            List<String[]> fields = new ArrayList<String[]>();
            List<?> l = (List<?>) members;
            for (Object lItem : l) {
                Map<?, ?> f = map(lItem, "member");
                fields.add(new String[] {text(f.get("name")), text(f.get("type"))});
            }
            types.put(String.valueOf(e.getKey()), fields);
        }
        Object primary = root.get("primaryType");
        return new TypedData(types, text(primary), strMap(map(root.get("domain"), "domain")),
                strMap(map(root.get("message"), "message")));
    }

    /// The 32-byte digest a wallet signs: `keccak256(0x1901 || domainSeparator ||
    /// hashStruct(message))`.
    public byte[] hash() {
        byte[] all = new byte[66];
        all[0] = 0x19;
        all[1] = 0x01;
        System.arraycopy(domainSeparator(), 0, all, 2, 32);
        System.arraycopy(hashStruct(primaryType, message), 0, all, 34, 32);
        return Hash.keccak256(all);
    }

    /// `hashStruct(EIP712Domain, domain)`.
    public byte[] domainSeparator() {
        return hashStruct(DOMAIN, domain);
    }

    /// The `hashStruct` of the message.
    public byte[] messageHash() {
        return hashStruct(primaryType, message);
    }

    /// Signs the document, answering `r || s || v` with `v` 27 or 28.
    public byte[] sign(byte[] privateKey) {
        return PersonalSign.signHash(hash(), privateKey);
    }

    /// The address that signed the document.
    public EvmAddress recover(byte[] signature65) {
        return PersonalSign.recoverHash(hash(), signature65);
    }

    /// The `encodeType` string of a struct, such as
    /// `Mail(Person from,Person to,string contents)Person(string name,address wallet)`.
    public String encodeType(String type) {
        TreeSet<String> deps = new TreeSet<String>();
        collect(type, deps);
        deps.remove(type);
        StringBuilder out = new StringBuilder();
        appendType(out, type);
        Iterator<String> it = deps.iterator();
        while (it.hasNext()) {
            appendType(out, it.next());
        }
        return out.toString();
    }

    private void appendType(StringBuilder out, String type) {
        out.append(type).append('(');
        List<String[]> fields = types.get(type);
        for (int iter = 0; iter < fields.size(); iter++) {
            if (iter > 0) {
                out.append(',');
            }
            out.append(fields.get(iter)[1]).append(' ').append(fields.get(iter)[0]);
        }
        out.append(')');
    }

    private void collect(String type, TreeSet<String> found) {
        String base = baseType(type);
        if (!types.containsKey(base) || found.contains(base)) {
            return;
        }
        found.add(base);
        List<String[]> fields = types.get(base);
        for (String[] fieldsItem : fields) {
            collect(fieldsItem[1], found);
        }
    }

    private static String baseType(String type) {
        int bracket = type.indexOf('[');
        return bracket < 0 ? type : type.substring(0, bracket);
    }

    /// `keccak256(typeHash || encodeData(value))` of a struct.
    public byte[] hashStruct(String type, Map<String, Object> value) {
        List<String[]> fields = types.get(type);
        if (fields == null) {
            throw new IllegalArgumentException("unknown type " + type);
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] typeHash = Hash.keccak256(Abi.utf8(encodeType(type)));
        out.write(typeHash, 0, 32);
        for (String[] f : fields) {
            byte[] enc = encodeValue(f[1], value.get(f[0]));
            out.write(enc, 0, 32);
        }
        return Hash.keccak256(out.toByteArray());
    }

    private byte[] encodeValue(String type, Object value) {
        if (type.endsWith("]")) {
            int open = type.lastIndexOf('[');
            String element = type.substring(0, open);
            List<?> items = list(value, type);
            String size = type.substring(open + 1, type.length() - 1);
            if (size.length() > 0 && Integer.parseInt(size) != items.size()) {
                throw new IllegalArgumentException(type + " takes " + size + " items");
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            for (Object itemsItem : items) {
                out.write(encodeValue(element, itemsItem), 0, 32);
            }
            return Hash.keccak256(out.toByteArray());
        }
        if (types.containsKey(type)) {
            if (value == null) {
                // a null struct value hashes as all zeros, as the reference implementations do
                return new byte[32];
            }
            return hashStruct(type, strMap(map(value, type)));
        }
        if ("string".equals(type)) {
            return Hash.keccak256(Abi.utf8(text(value)));
        }
        if ("bytes".equals(type)) {
            return Hash.keccak256(value instanceof byte[] ? (byte[]) value
                    : com.codename1.crypto.encoding.Hex.decode(text(value)));
        }
        Object v = value;
        if (v instanceof Double) {
            throw new IllegalArgumentException(type + " cannot take a fractional number");
        }
        if (v instanceof String && type.startsWith("int") && ((String) v).startsWith("0x")) {
            v = com.codename1.crypto.UInt256.parse((String) v);
        }
        if (v instanceof String && type.startsWith("bytes") && ((String) v).length() < 66) {
            // a short bytesN hex value is right-padded, as wallets accept it
            byte[] raw = com.codename1.crypto.encoding.Hex.decode((String) v);
            int n = Integer.parseInt(type.substring(5));
            byte[] padded = new byte[n];
            System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, n));
            v = padded;
        }
        return Abi.Type.parse(Abi.canonical(type)).encode(v);
    }

    private static Map<?, ?> map(Object o, String what) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("EIP-712 " + what + " must be an object");
        }
        return (Map<?, ?>) o;
    }

    private static List<?> list(Object o, String what) {
        if (!(o instanceof List)) {
            throw new IllegalArgumentException("EIP-712 " + what + " must be an array");
        }
        return (List<?>) o;
    }

    private static String text(Object o) {
        if (!(o instanceof String)) {
            throw new IllegalArgumentException("EIP-712 expected a string");
        }
        return (String) o;
    }

    private static Map<String, Object> strMap(Map<?, ?> in) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        Iterator<?> it = in.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }
}
