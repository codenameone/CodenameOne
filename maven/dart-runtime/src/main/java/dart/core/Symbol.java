/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

/// Dart's {@code Symbol}: {@code Symbol('a')} and the literal {@code #a}. Two symbols
/// of one name are equal; toString is {@code Symbol("a")}, as the VM prints it.
public final class Symbol {
    private final String name;
    /// The member name of an invocation of a private setter: the VM prints it without its
    /// {@code =} (a {@code Symbol('_x=')} made by hand keeps it), though it still is the
    /// setter's name for equality.
    private final boolean privateSetter;

    public Symbol(String name) {
        this(name, false);
    }

    Symbol(String name, boolean setter) {
        if (name == null) {
            throw new ArgumentError("Symbol name must not be null");
        }
        this.name = name;
        this.privateSetter = setter && name.startsWith("_") && name.endsWith("=");
    }

    /// The name this symbol was made from (`#a` is "a"); Function.apply places a named
    /// argument by it.
    public String $name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Symbol && ((Symbol) o).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + 0x5f;
    }

    /// The name as Dart spells it. A private name that two libraries both use is renamed in
    /// all but one of them ({@code _x$p2}, see the transpiler's
    /// {@code Program.separateLibraryPrivateMembers}), and that suffix is the library
    /// identity: it stays in {@link #name}, so {@code #_x} of one library is not equal to
    /// {@code #_x} of another, but Dart prints neither with it. Only a private name ending in
    /// {@code $p} and digits is shortened; a user identifier spelled that way is
    /// indistinguishable from a renamed one, since Dart identifiers may contain {@code $}.
    static String dartSpelling(String name) {
        if (name.endsWith("=")) {
            return dartSpelling(name.substring(0, name.length() - 1)) + "=";
        }
        if (name.length() < 4 || name.charAt(0) != '_') {
            return name;
        }
        int i = name.length();
        while (i > 0 && name.charAt(i - 1) >= '0' && name.charAt(i - 1) <= '9') {
            i--;
        }
        if (i == name.length() || i < 3 || name.charAt(i - 1) != 'p' || name.charAt(i - 2) != '$') {
            return name;
        }
        return name.substring(0, i - 2);
    }

    @Override
    public String toString() {
        String n = privateSetter ? name.substring(0, name.length() - 1) : name;
        return "Symbol(\"" + dartSpelling(n) + "\")";
    }
}
