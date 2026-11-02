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
package dart.runtime;

/// The runtime type of a signed function value: what `f.runtimeType` answers. It prints
/// Dart's spelling, `(int, [String]) => bool` or `(int, {required int k}) => void`, and
/// compares structurally, as Dart's function Types do: equal when the parameter kinds,
/// the named parameters' names and every type are the same.
public final class FunctionType {
    private final TypeWitness[] sig;
    private final int required;
    private final String[] named;

    FunctionType(Signed s) {
        this.sig = s.$sig();
        this.required = s.$required();
        this.named = s.$named();
    }

    private int namedCount() {
        return named == null ? 0 : named.length;
    }

    private int positional() {
        return sig.length - 1 - namedCount();
    }

    private int requiredPositional() {
        return required < 0 ? positional() : required;
    }

    private static String str(TypeWitness w) {
        return w == null ? "dynamic" : w.type() == Void.class ? "void" : w.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        int pos = positional();
        int req = requiredPositional();
        for (int i = 0; i < pos; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (i == req) {
                sb.append('[');
            }
            sb.append(str(sig[i + 1]));
        }
        if (req < pos) {
            sb.append(']');
        }
        if (namedCount() > 0) {
            if (pos > 0) {
                sb.append(", ");
            }
            sb.append('{');
            for (int i = 0; i < named.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                String n = named[i];
                boolean r = n.charAt(0) == '!';
                if (r) {
                    sb.append("required ");
                }
                sb.append(str(sig[1 + pos + i])).append(' ').append(r ? n.substring(1) : n);
            }
            sb.append('}');
        }
        return sb.append(") => ").append(str(sig[0])).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionType)) {
            return false;
        }
        FunctionType f = (FunctionType) o;
        if (f.sig.length != sig.length || f.requiredPositional() != requiredPositional()
                || f.namedCount() != namedCount()) {
            return false;
        }
        for (int i = 0; i < sig.length; i++) {
            if (!sameType(sig[i], f.sig[i])) {
                return false;
            }
        }
        for (int i = 0; i < namedCount(); i++) {
            if (!named[i].equals(f.named[i])) {
                return false;
            }
        }
        return true;
    }

    /// Type equality: a null witness is `dynamic`, which equals only `dynamic`.
    private static boolean sameType(TypeWitness a, TypeWitness b) {
        boolean ad = a == null || a == TypeWitness.DYNAMIC;
        boolean bd = b == null || b == TypeWitness.DYNAMIC;
        if (ad || bd) {
            return ad == bd;
        }
        return a.toString().equals(b.toString()) && TypeWitness.isSubtype(a, b) && TypeWitness.isSubtype(b, a);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
