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
package com.codename1.backend.security.pki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// An X.500 distinguished name, as a certificate's subject and issuer carry
/// it.
///
/// [#toString] writes the RFC 4514 form, most specific attribute first:
/// `CN=api.example.com,O=Example,C=US`. [#parse] reads that form back, and
/// [#getEncoded] is the DER a certificate carries.
///
/// Two names are [#equals] when their DER is identical, or when their RFC 4514
/// strings match ignoring ASCII case. That is the comparison chain building
/// needs in practice: CAs copy the issuer name byte for byte. The full RFC 5280
/// comparison (Unicode case folding, whitespace collapsing) is not done.
public final class DistinguishedName {
    private static final String[][] NAMES = {
        {"CN", "2.5.4.3"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"SERIALNUMBER", "2.5.4.5"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"C", "2.5.4.6"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"L", "2.5.4.7"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"ST", "2.5.4.8"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"STREET", "2.5.4.9"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"O", "2.5.4.10"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"OU", "2.5.4.11"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"T", "2.5.4.12"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"GIVENNAME", "2.5.4.42"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"SURNAME", "2.5.4.4"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"DC", "0.9.2342.19200300.100.1.25"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"UID", "0.9.2342.19200300.100.1.1"}, //NOPMD AvoidUsingHardCodedIP - an OID
        {"EMAILADDRESS", "1.2.840.113549.1.9.1"}, //NOPMD AvoidUsingHardCodedIP - an OID
    };

    private final byte[] der;
    private final String text;
    private final List<String[]> attributes;

    private DistinguishedName(byte[] der, String text, List<String[]> attributes) {
        this.der = der;
        this.text = text;
        this.attributes = attributes;
    }

    /// Reads a Name from its DER.
    public static DistinguishedName decode(byte[] der) throws IOException {
        Asn1.Reader name = new Asn1.Reader(der).open(Asn1.SEQUENCE);
        List<String[]> attributes = new ArrayList<String[]>();
        List<String> rdns = new ArrayList<String>();
        while (name.hasMore()) {
            Asn1.Reader rdn = name.open(Asn1.SET);
            StringBuilder one = new StringBuilder();
            while (rdn.hasMore()) {
                Asn1.Reader pair = rdn.open(Asn1.SEQUENCE);
                String oid = pair.oid();
                String key = shortName(oid);
                String value;
                String rendered;
                int tag = pair.peek();
                if (isStringTag(tag)) {
                    value = pair.string();
                    rendered = escape(value);
                } else {
                    byte[] raw = pair.rawAny();
                    value = "#" + Asn1.hex(raw);
                    rendered = value;
                }
                pair.finish();
                attributes.add(new String[] {key, value});
                if (one.length() > 0) {
                    one.append('+');
                }
                one.append(key).append('=').append(rendered);
            }
            rdns.add(one.toString());
        }
        StringBuilder text = new StringBuilder();
        for (int iter = rdns.size() - 1 ; iter >= 0 ; iter--) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(rdns.get(iter));
        }
        return new DistinguishedName(der.clone(), text.toString(), attributes);
    }

    /// Reads the RFC 4514 form: `CN=api.example.com,O=Example,C=US`. Spaces
    /// around `=` and `,` are dropped, and `\,` escapes a comma. Every
    /// attribute is its own RDN; multi-valued RDNs (`+`) are not written.
    ///
    /// `C` is written as a PrintableString, `DC` and `EMAILADDRESS` as an
    /// IA5String, everything else as a UTF8String, which is what RFC 5280
    /// asks of a new certificate.
    public static DistinguishedName parse(String rfc4514) throws IOException {
        if (rfc4514 == null) {
            throw new IOException("No name was given");
        }
        List<String> parts = splitUnescaped(rfc4514, ',');
        List<byte[]> rdns = new ArrayList<byte[]>();
        for (int iter = parts.size() - 1 ; iter >= 0 ; iter--) {
            String part = parts.get(iter).trim();
            if (part.length() == 0) {
                if (parts.size() == 1) {
                    break;
                }
                throw new IOException("An empty attribute in the name " + rfc4514);
            }
            int eq = part.indexOf('=');
            if (eq <= 0) {
                throw new IOException("Not KEY=value: " + part);
            }
            String key = part.substring(0, eq).trim();
            String value = unescape(part.substring(eq + 1).trim());
            String oid = oidOf(key);
            byte[] encoded;
            if ("2.5.4.6".equals(oid)) { //NOPMD AvoidUsingHardCodedIP - an OID
                if (value.length() != 2) {
                    throw new IOException("A country (C) is a two letter code, not " + value);
                }
                encoded = Asn1.printableString(value);
            } else if ("0.9.2342.19200300.100.1.25".equals(oid) || "1.2.840.113549.1.9.1".equals(oid)) { //NOPMD AvoidUsingHardCodedIP - an OID
                encoded = Asn1.ia5String(value);
            } else {
                encoded = Asn1.utf8String(value);
            }
            rdns.add(Asn1.set(Asn1.sequence(Asn1.oid(oid), encoded)));
        }
        return decode(Asn1.sequence(rdns.toArray(new byte[rdns.size()][])));
    }

    /// The DER of this name.
    public byte[] getEncoded() {
        return der.clone();
    }

    byte[] der() {
        return der;
    }

    /// The first value of an attribute, by its short name (`CN`, `O`) or dotted
    /// OID, or null when the name has none. For the subject of a certificate,
    /// `get("CN")` is the common name.
    public String get(String attribute) {
        String want = attribute;
        for (String[] known : NAMES) {
            if (known[1].equals(attribute)) {
                want = known[0];
            }
        }
        for (String[] pair : attributes) {
            if (pair[0].equalsIgnoreCase(want)) {
                return pair[1];
            }
        }
        return null;
    }

    /// True for a name with no attributes, which a certificate whose identity is
    /// only in its subject alternative names may carry.
    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DistinguishedName)) {
            return false;
        }
        DistinguishedName that = (DistinguishedName) other;
        return Asn1.same(der, that.der) || text.equalsIgnoreCase(that.text);
    }

    @Override
    public int hashCode() {
        int out = 0;
        for (int iter = 0 ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + 32);
            }
            out = out * 31 + c;
        }
        return out;
    }

    /// The RFC 4514 form.
    @Override
    public String toString() {
        return text;
    }

    private static boolean isStringTag(int tag) {
        return tag == Asn1.UTF8_STRING || tag == Asn1.PRINTABLE_STRING || tag == Asn1.IA5_STRING
                || tag == Asn1.T61_STRING || tag == Asn1.BMP_STRING || tag == Asn1.UNIVERSAL_STRING;
    }

    private static String shortName(String oid) {
        for (String[] known : NAMES) {
            if (known[1].equals(oid)) {
                return known[0];
            }
        }
        return oid;
    }

    private static String oidOf(String key) throws IOException {
        for (String[] known : NAMES) {
            if (known[0].equalsIgnoreCase(key)) {
                return known[1];
            }
        }
        if (key.length() > 0 && key.charAt(0) >= '0' && key.charAt(0) <= '2' && key.indexOf('.') > 0) {
            return key;
        }
        if ("E".equalsIgnoreCase(key)) {
            return "1.2.840.113549.1.9.1"; //NOPMD AvoidUsingHardCodedIP - an OID
        }
        throw new IOException("Not a name attribute this runtime knows: " + key
                + "; give its dotted OID instead");
    }

    /// RFC 4514 escaping of a value.
    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            boolean edge = (iter == 0 && (c == ' ' || c == '#'))
                    || (iter == value.length() - 1 && c == ' ');
            if (edge || c == ',' || c == '+' || c == '"' || c == '\\' || c == '<' || c == '>'
                    || c == ';' || c == '=') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '\\' && iter + 1 < value.length()) {
                c = value.charAt(++iter);
            }
            out.append(c);
        }
        return out.toString();
    }

    private static List<String> splitUnescaped(String value, char sep) {
        List<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '\\' && iter + 1 < value.length()) {
                current.append(c).append(value.charAt(++iter));
            } else if (c == sep) {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(current.toString());
        return out;
    }
}
