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
package com.codename1.backend.security.ldap;

import com.codename1.backend.security.pki.Asn1;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/// An RFC 4515 filter string, such as `(&(objectClass=person)(uid=ada))`,
/// compiled to the BER a SearchRequest carries. Values in the string must
/// already be escaped with [LdapEncoder#filterEncode].
final class LdapFilter {
    private static final int MAX_DEPTH = 32;

    private final String text;
    private int pos;

    private LdapFilter(String text) {
        this.text = text;
    }

    static byte[] compile(String filter) throws IOException {
        String f = filter.trim();
        if (!f.startsWith("(")) {
            f = "(" + f + ")";
        }
        LdapFilter p = new LdapFilter(f);
        byte[] out = p.filter(0);
        if (p.pos != f.length()) {
            throw p.bad("text after the filter");
        }
        return out;
    }

    private IOException bad(String why) {
        return new IOException("Bad LDAP filter " + text + " at " + pos + ": " + why);
    }

    private byte[] filter(int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw bad("nested too deep");
        }
        expect('(');
        if (pos >= text.length()) {
            throw bad("ends early");
        }
        char c = text.charAt(pos);
        byte[] out;
        if (c == '&' || c == '|') {
            pos++;
            List<byte[]> parts = new ArrayList<byte[]>();
            while (pos < text.length() && text.charAt(pos) == '(') {
                parts.add(filter(depth + 1));
            }
            out = Asn1.tlv(c == '&' ? 0xa0 : 0xa1, parts.toArray(new byte[parts.size()][]));
        } else if (c == '!') {
            pos++;
            out = Asn1.tlv(0xa2, filter(depth + 1));
        } else {
            out = item();
        }
        expect(')');
        return out;
    }

    private void expect(char c) throws IOException {
        if (pos >= text.length() || text.charAt(pos) != c) {
            throw bad("expected " + c);
        }
        pos++;
    }

    private byte[] item() throws IOException {
        int start = pos;
        while (pos < text.length() && "=<>~()".indexOf(text.charAt(pos)) < 0) {
            pos++;
        }
        String attribute = text.substring(start, pos).trim();
        if (attribute.length() == 0 || pos >= text.length()) {
            throw bad("no attribute");
        }
        int tag;
        char op = text.charAt(pos);
        if (op == '=' && attribute.endsWith(":")) {
            pos++;
            return extensible(attribute.substring(0, attribute.length() - 1));
        }
        if (op == '=') {
            tag = 0xa3;
            pos++;
        } else if ((op == '>' || op == '<' || op == '~') && pos + 1 < text.length()
                && text.charAt(pos + 1) == '=') {
            tag = op == '>' ? 0xa5 : op == '<' ? 0xa6 : 0xa8;
            pos += 2;
        } else {
            throw bad("expected an operator");
        }
        int valueStart = pos;
        while (pos < text.length() && text.charAt(pos) != ')') {
            if (text.charAt(pos) == '(') {
                throw bad("unescaped ( in a value");
            }
            pos++;
        }
        String raw = text.substring(valueStart, pos);
        byte[] type = Asn1.octets(utf8(attribute));
        if (tag != 0xa3 || raw.indexOf('*') < 0) {
            return Asn1.tlv(tag, type, Asn1.octets(unescape(raw)));
        }
        if ("*".equals(raw)) {
            return Asn1.tlv(0x87, utf8(attribute));
        }
        // Substrings: initial, any..., final, split at the unescaped stars.
        List<byte[]> parts = new ArrayList<byte[]>();
        String[] pieces = splitStars(raw);
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].length() == 0) {
                continue;
            }
            int t = i == 0 ? 0x80 : i == pieces.length - 1 ? 0x82 : 0x81;
            parts.add(Asn1.tlv(t, unescape(pieces[i])));
        }
        return Asn1.tlv(0xa4, type, Asn1.sequence(parts.toArray(new byte[parts.size()][])));
    }

    /// RFC 4515 extensible match, `attr[:dn][:rule]:=value` or
    /// `[:dn]:rule:=value` -- Active Directory's
    /// `memberOf:1.2.840.113556.1.4.1941:=` among them.
    private byte[] extensible(String head) throws IOException {
        String[] parts = LdapEncoder.split(head, ':');
        String type = parts[0].trim();
        boolean dn = false;
        String rule = null;
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if ("dn".equalsIgnoreCase(p) && !dn && rule == null) {
                dn = true;
            } else if (p.length() > 0 && rule == null && isRule(p)) {
                rule = p;
            } else {
                throw bad("not an extensible match");
            }
        }
        if (type.length() == 0 && rule == null) {
            throw bad("an extensible match needs an attribute or a matching rule");
        }
        int valueStart = pos;
        while (pos < text.length() && text.charAt(pos) != ')') {
            if (text.charAt(pos) == '(') {
                throw bad("unescaped ( in a value");
            }
            pos++;
        }
        List<byte[]> out = new ArrayList<byte[]>();
        if (rule != null) {
            out.add(Asn1.tlv(0x81, utf8(rule)));
        }
        if (type.length() > 0) {
            out.add(Asn1.tlv(0x82, utf8(type)));
        }
        out.add(Asn1.tlv(0x83, unescape(text.substring(valueStart, pos))));
        if (dn) {
            out.add(new byte[] {(byte) 0x84, 1, (byte) 0xff});
        }
        return Asn1.tlv(0xa9, out.toArray(new byte[out.size()][]));
    }

    private static boolean isRule(String p) {
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (!((c >= '0' && c <= '9') || c == '.' || c == '-' || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    private static String[] splitStars(String raw) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '*') {
                out.add(raw.substring(from, i));
                from = i + 1;
            }
        }
        out.add(raw.substring(from));
        return out.toArray(new String[out.size()]);
    }

    private byte[] unescape(String raw) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length());
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\\') {
                if (i + 3 > raw.length()) {
                    throw bad("a \\ needs two hex digits");
                }
                int hi = Character.digit(raw.charAt(i + 1), 16);
                int lo = Character.digit(raw.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    throw bad("a \\ needs two hex digits");
                }
                out.write((hi << 4) | lo);
                i += 3;
            } else {
                byte[] b = utf8(String.valueOf(c));
                if (Character.isHighSurrogate(c) && i + 1 < raw.length()) {
                    b = utf8(raw.substring(i, i + 2));
                    i++;
                }
                out.write(b, 0, b.length);
                i++;
            }
        }
        return out.toByteArray();
    }

    static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException("No UTF-8", err);
        }
    }
}
