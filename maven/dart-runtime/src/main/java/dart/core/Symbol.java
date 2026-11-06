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

    /// The Dart spelling when it differs from {@link #name} (a renamed private name), or null.
    private final String spelling;

    public Symbol(String name) {
        this(name, false);
    }

    /// A `#_x` literal of a library whose `_x` was renamed: equal by the renamed name,
    /// printed as written.
    public Symbol(String name, String spelling) {
        this(name, false, spelling);
    }

    Symbol(String name, boolean setter) {
        this(name, setter, null);
    }

    private Symbol(String name, boolean setter, String spelling) {
        if (name == null) {
            throw new ArgumentError("Symbol name must not be null");
        }
        this.name = name;
        this.privateSetter = setter && name.startsWith("_") && name.endsWith("=");
        this.spelling = spelling;
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

    /// Renamed private names (see the transpiler's
    /// {@code Program.separateLibraryPrivateMembers}) to the names as written, registered
    /// by the generated classes of each renaming library ({@link #spell}).
    private static final java.util.HashMap<String, String> SPELLINGS = new java.util.HashMap<String, String>();

    /// Registers pairs of renamed name, Dart spelling. Returns null, so an interface can
    /// call it from a field initializer.
    public static Object spell(String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            SPELLINGS.put(pairs[i], pairs[i + 1]);
        }
        return null;
    }

    /// Registers the pairs of {@link #spell} packed into one string, every name followed
    /// by a newline (no Dart identifier contains one). One literal instead of an array
    /// of them: built natively, each string constant costs code of its own.
    public static void spellPacked(String pairs) {
        int at = 0;
        while (at < pairs.length()) {
            int a = pairs.indexOf('\n', at);
            int b = pairs.indexOf('\n', a + 1);
            SPELLINGS.put(pairs.substring(at, a), pairs.substring(a + 1, b));
            at = b + 1;
        }
    }

    /// The name as Dart spells it: the one given with it, or the one registered for a
    /// renamed private name. The renamed name stays in {@link #name} as the library
    /// identity, so {@code #_x} of one library is not equal to {@code #_x} of another.
    /// Nothing is inferred from the text, so a user name really spelled {@code _x$p2} prints
    /// as written.
    private String dartSpelling(String n) {
        if (spelling != null) {
            return spelling;
        }
        boolean eq = n.endsWith("=");
        String base = eq ? n.substring(0, n.length() - 1) : n;
        String sp = SPELLINGS.get(base);
        return sp == null ? n : eq ? sp + "=" : sp;
    }

    @Override
    public String toString() {
        String n = privateSetter ? name.substring(0, name.length() - 1) : name;
        return "Symbol(\"" + dartSpelling(n) + "\")";
    }
}
