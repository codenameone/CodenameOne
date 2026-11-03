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
    /// A generic closure's type parameter names (Signed.$typeVars), or null.
    private final String[] typeVars;

    /// A function type's Type from a function-type witness's signature (no type
    /// parameters): see TypeWitness's function factory for the layout.
    FunctionType(TypeWitness[] sig, int required, String[] named) {
        this.sig = sig;
        this.required = required;
        this.named = named;
        this.typeVars = null;
    }

    FunctionType(Signed s) {
        this.sig = s.$sig();
        this.required = s.$required();
        this.named = s.$named();
        this.typeVars = s.$generic() ? s.$typeVars() : null;
    }

    /// Dart prints a generic function type with its type parameters renamed in order,
    /// `Y0`, `Y1`...: `T id<T>(T v)` is `<Y0>(Y0) => Y0`, whatever the source called T.
    /// The slot is a Dart type spelling (`T`, `List<K>`, `(V) => K`); every identifier in
    /// it that is one of the type parameters is renamed.
    private String canonical(String spelled) {
        String[] params = typeVars[0].split(", ");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < spelled.length()) {
            char c = spelled.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$') {
                int start = i;
                while (i < spelled.length() && (Character.isLetterOrDigit(spelled.charAt(i))
                        || spelled.charAt(i) == '_' || spelled.charAt(i) == '$')) {
                    i++;
                }
                String word = spelled.substring(start, i);
                String renamed = word;
                for (int k = 0; k < params.length; k++) {
                    if (params[k].equals(word)) {
                        renamed = "Y" + k;
                        break;
                    }
                }
                sb.append(renamed);
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private String slot(int i) {
        if (typeVars != null && i + 1 < typeVars.length && typeVars[i + 1] != null) {
            return canonical(typeVars[i + 1]);
        }
        return str(sig[i]);
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
        // TypeWitness.NULL shares void's class; it is Null, the return Dart infers for a
        // block-bodied closure with no context that returns no value.
        return w == null ? "dynamic" : w == TypeWitness.NULL ? "Null" : w.type() == Void.class ? "void" : w.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (typeVars != null && typeVars.length > 0) {
            String[] params = typeVars[0].split(", ");
            sb.append('<');
            for (int k = 0; k < params.length; k++) {
                sb.append(k > 0 ? ", " : "").append('Y').append(k);
            }
            sb.append('>');
        }
        sb.append('(');
        int pos = positional();
        int req = requiredPositional();
        for (int i = 0; i < pos; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (i == req) {
                sb.append('[');
            }
            sb.append(slot(i + 1));
        }
        if (req < pos) {
            sb.append(']');
        }
        if (namedCount() > 0) {
            if (pos > 0) {
                sb.append(", ");
            }
            sb.append('{');
            // Dart prints the named parameters by name, whatever order declared them.
            Integer[] order = sortedNamed();
            for (int k = 0; k < order.length; k++) {
                int i = order[k];
                if (k > 0) {
                    sb.append(", ");
                }
                String n = named[i];
                boolean r = n.charAt(0) == '!';
                if (r) {
                    sb.append("required ");
                }
                sb.append(slot(1 + pos + i)).append(' ').append(r ? n.substring(1) : n);
            }
            sb.append('}');
        }
        return sb.append(") => ").append(slot(0)).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof TypeWitness) {
            // A function type's literal (a typedef) against a closure's runtimeType.
            FunctionType w = ((TypeWitness) o).functionType();
            return w != null && equals(w);
        }
        if (!(o instanceof FunctionType)) {
            return false;
        }
        FunctionType f = (FunctionType) o;
        if ((typeVars == null) != (f.typeVars == null)) {
            return false;
        }
        if (f.sig.length != sig.length || f.requiredPositional() != requiredPositional()
                || f.namedCount() != namedCount()) {
            return false;
        }
        int pos = positional();
        for (int i = 0; i <= pos; i++) {
            if (!sameType(sig[i], f.sig[i])) {
                return false;
            }
        }
        // Named parameters are a set: matched by name (and requiredness), never by the
        // order they were declared in.
        for (int i = 0; i < namedCount(); i++) {
            int j = -1;
            for (int k = 0; k < f.named.length; k++) {
                if (named[i].equals(f.named[k])) {
                    j = k;
                    break;
                }
            }
            if (j < 0 || !sameType(sig[1 + pos + i], f.sig[1 + pos + j])) {
                return false;
            }
        }
        return true;
    }

    private Integer[] sortedNamed() {
        Integer[] order = new Integer[namedCount()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> bare(named[a]).compareTo(bare(named[b])));
        return order;
    }

    private static String bare(String n) {
        return n.charAt(0) == '!' ? n.substring(1) : n;
    }

    /// Type equality: a null witness is `dynamic`, which equals only `dynamic`.
    private static boolean sameType(TypeWitness a, TypeWitness b) {
        boolean ad = a == null || a == TypeWitness.DYNAMIC;
        boolean bd = b == null || b == TypeWitness.DYNAMIC;
        if (ad || bd) {
            return ad == bd;
        }
        if ((a == TypeWitness.NULL) != (b == TypeWitness.NULL)) {
            return false; // Null and void share a class but are two types
        }
        return a.toString().equals(b.toString()) && TypeWitness.isSubtype(a, b) && TypeWitness.isSubtype(b, a);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
