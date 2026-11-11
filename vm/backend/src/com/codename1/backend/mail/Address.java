/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.mail;

/// One mailbox: an optional display name and the address itself.
///
/// Accepts `user@example.com` and `Display Name <user@example.com>`. A quoted
/// local part, a comment and a group are valid RFC 5322 and are refused here:
/// no application sends to one on purpose, and each is a way to smuggle a
/// second recipient past a check that reads the string by eye.
final class Address {
    /// The display name, or null.
    final String name;
    /// The address: `local@domain`.
    final String spec;
    /// Whether [#spec] is ASCII; one that is not needs the server's SMTPUTF8.
    final boolean ascii;

    private Address(String name, String spec, boolean ascii) {
        this.name = name;
        this.spec = spec;
        this.ascii = ascii;
    }

    /// Parses one mailbox, or throws IllegalArgumentException naming what is
    /// wrong with it.
    static Address parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("An email address is null");
        }
        String v = value.trim();
        String name = null;
        String spec = v;
        int open = v.lastIndexOf('<');
        if (open >= 0 && v.endsWith(">")) {
            spec = v.substring(open + 1, v.length() - 1).trim();
            name = v.substring(0, open).trim();
            if (name.length() >= 2 && name.charAt(0) == '"'
                    && name.charAt(name.length() - 1) == '"') {
                name = name.substring(1, name.length() - 1);
            }
            if (name.length() == 0) {
                name = null;
            }
        }
        if (name != null) {
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c < ' ' || c == 127) {
                    throw new IllegalArgumentException("The display name of \"" + printable(value)
                            + "\" has a control character in it");
                }
            }
        }
        return new Address(name, spec, checkSpec(spec, value));
    }

    /// Whether `spec` is a usable `local@domain`; answers without throwing.
    static boolean isValid(String spec) {
        if (spec == null) {
            return false;
        }
        try {
            checkSpec(spec, spec);
            return true;
        } catch (IllegalArgumentException err) {
            return false;
        }
    }

    private static boolean checkSpec(String spec, String original) {
        int at = spec.indexOf('@');
        if (at <= 0 || at != spec.lastIndexOf('@') || at == spec.length() - 1) {
            throw new IllegalArgumentException("\"" + printable(original)
                    + "\" is not an email address: it needs one @ with text on both sides");
        }
        if (spec.length() > 254 || at > 64) {
            throw new IllegalArgumentException("\"" + printable(original)
                    + "\" is longer than an email address may be");
        }
        boolean ascii = true;
        for (int i = 0; i < spec.length(); i++) {
            char c = spec.charAt(i);
            if (c <= ' ' || c == 127 || c == '<' || c == '>' || c == '(' || c == ')'
                    || c == ',' || c == ';' || c == ':' || c == '"' || c == '\\'
                    || c == '[' || c == ']') {
                throw new IllegalArgumentException("\"" + printable(original)
                        + "\" is not an email address: character " + (int) c
                        + " cannot be part of one");
            }
            if (c > 126) {
                ascii = false;
            }
        }
        if (spec.charAt(0) == '.' || spec.charAt(at - 1) == '.' || spec.charAt(at + 1) == '.'
                || spec.charAt(spec.length() - 1) == '.' || spec.indexOf("..") >= 0) {
            throw new IllegalArgumentException("\"" + printable(original)
                    + "\" is not an email address: a dot is misplaced");
        }
        return ascii;
    }

    /// `value` with anything that could break a log line replaced.
    private static String printable(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length() && i < 80; i++) {
            char c = value.charAt(i);
            sb.append(c < ' ' || c == 127 ? '?' : c);
        }
        return sb.toString();
    }

    /// The domain of the address.
    String domain() {
        return spec.substring(spec.indexOf('@') + 1);
    }

    @Override
    public String toString() {
        return name == null ? spec : name + " <" + spec + ">";
    }
}
