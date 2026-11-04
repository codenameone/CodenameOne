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

    public Symbol(String name) {
        if (name == null) {
            throw new ArgumentError("Symbol name must not be null");
        }
        this.name = name;
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

    @Override
    public String toString() {
        return "Symbol(\"" + name + "\")";
    }
}
