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

/// The runtime value of a type parameter that a generic class's body uses as a value.
///
/// Java erases type arguments, while Dart lets a body write `x is T`, `T == int` or
/// `Type get type => T`. The transpiler gives only the classes that do so one hidden
/// witness per such parameter, passed by the code that constructs them from the type
/// argument it wrote. A witness is the erased class -- the same `Class` a type literal
/// becomes -- and whether the type admits null, which `null is T` needs.
public final class TypeWitness {
    /// Interned plain witnesses, one pair (non-null, nullable) per class: construction
    /// sites and checks call {@code of} on every evaluation, so it must not allocate.
    private static final java.util.HashMap<Class<?>, TypeWitness[]> INTERNED =
            new java.util.HashMap<Class<?>, TypeWitness[]>();

    private final Class<?> type;
    private final boolean nullable;
    /// The witnesses of a program generic class's own type arguments
    /// ({@code Box<int>} as a type argument), or null when not tracked.
    private final TypeWitness[] args;
    /// Parameterized witnesses derived from this plain one, interned by argument identity.
    private java.util.ArrayList<TypeWitness> derived;

    private TypeWitness(Class<?> type, boolean nullable, TypeWitness[] args) {
        this.type = type;
        this.nullable = nullable;
        this.args = args;
    }

    /// The interned witness of {@code type}.
    public static TypeWitness of(Class<?> type, boolean nullable) {
        TypeWitness[] pair = INTERNED.get(type);
        if (pair == null) {
            pair = new TypeWitness[] {new TypeWitness(type, false, null), new TypeWitness(type, true, null)};
            INTERNED.put(type, pair);
        }
        return pair[nullable ? 1 : 0];
    }

    /// The interned witness of a one-argument program generic type.
    public static TypeWitness of(Class<?> type, boolean nullable, TypeWitness a0) {
        TypeWitness plain = of(type, nullable);
        TypeWitness d = plain.find1(a0);
        return d != null ? d : plain.derive(new TypeWitness[] {a0});
    }

    /// The interned witness of a two-argument program generic type.
    public static TypeWitness of(Class<?> type, boolean nullable, TypeWitness a0, TypeWitness a1) {
        TypeWitness plain = of(type, nullable);
        TypeWitness d = plain.find2(a0, a1);
        return d != null ? d : plain.derive(new TypeWitness[] {a0, a1});
    }

    /// The interned witness of a program generic type with three or more arguments; the
    /// array the call passes is kept only when no equal witness exists yet.
    public static TypeWitness of(Class<?> type, boolean nullable, TypeWitness... args) {
        TypeWitness plain = of(type, nullable);
        if (plain.derived != null) {
            for (int i = 0; i < plain.derived.size(); i++) {
                TypeWitness d = plain.derived.get(i);
                if (sameArgs(d.args, args)) {
                    return d;
                }
            }
        }
        return plain.derive(args);
    }

    private TypeWitness find1(TypeWitness a0) {
        if (derived != null) {
            for (int i = 0; i < derived.size(); i++) {
                TypeWitness d = derived.get(i);
                if (d.args.length == 1 && d.args[0] == a0) {
                    return d;
                }
            }
        }
        return null;
    }

    private TypeWitness find2(TypeWitness a0, TypeWitness a1) {
        if (derived != null) {
            for (int i = 0; i < derived.size(); i++) {
                TypeWitness d = derived.get(i);
                if (d.args.length == 2 && d.args[0] == a0 && d.args[1] == a1) {
                    return d;
                }
            }
        }
        return null;
    }

    private static boolean sameArgs(TypeWitness[] a, TypeWitness[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private TypeWitness derive(TypeWitness[] args) {
        if (derived == null) {
            derived = new java.util.ArrayList<TypeWitness>();
        }
        TypeWitness d = new TypeWitness(type, nullable, args);
        derived.add(d);
        return d;
    }

    /// The witness admitting null too.
    public static TypeWitness nullable(TypeWitness w) {
        if (w.nullable) {
            return w;
        }
        if (w.args == null) {
            return of(w.type, true);
        }
        return of(w.type, true, w.args);
    }

    /// The type arguments' witnesses of a parameterized witness, or null.
    TypeWitness[] args() {
        return args;
    }

    /// The erased class this witness tests.
    public Class<?> type() {
        return type;
    }

    /// Whether {@code value} is an instance of the witnessed type (erased below the top level).
    public boolean test(Object value) {
        return value == null ? nullable : type.isInstance(value);
    }

    /// Dart's covariant subtype test of a reified type argument: whether a value whose
    /// type argument is {@code actual} is an instance of the type whose argument is
    /// {@code target}. A null witness on either side is an argument nothing recorded
    /// (built where it could not be named), which keeps the erased answer: true.
    /// {@code dynamic} and {@code Object?} are top and accept everything.
    public static boolean isSubtype(TypeWitness actual, TypeWitness target) {
        if (target == null || actual == null || actual == target) {
            return true;
        }
        if (target.type == Object.class && target.nullable) {
            return true;
        }
        if (actual.nullable && !target.nullable) {
            return false;
        }
        if (!target.type.isAssignableFrom(actual.type)) {
            return false;
        }
        if (target.args != null && actual.args != null && target.args.length == actual.args.length) {
            for (int i = 0; i < target.args.length; i++) {
                if (!isSubtype(actual.args[i], target.args[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---- Dart Type values -------------------------------------------------------------
    //
    // A Type value of a non-generic type is its Java class, exactly as before. Only a type
    // with known arguments (`Box<int>`) or a nullable one (`int?` through a type variable)
    // is a TypeWitness; witnesses are interned, so == and a map key compare by identity.

    /// The Type value of {@code c} with the given argument witnesses: the class itself when
    /// no argument is known, the interned witness otherwise.
    public static Object typeValue(Class<?> c, TypeWitness a0) {
        return a0 == null ? c : of(c, false, a0);
    }

    public static Object typeValue(Class<?> c, TypeWitness a0, TypeWitness a1) {
        return a0 == null && a1 == null ? c : of(c, false, a0, a1);
    }

    public static Object typeValue(Class<?> c, TypeWitness... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                return of(c, false, args);
            }
        }
        return c;
    }

    /// The Type value of a type variable whose witness is {@code w} (`T` as an expression).
    public static Object value(TypeWitness w) {
        if (w == null) {
            return Object.class;
        }
        if (w.args == null && (!w.nullable || w.type == Object.class)) {
            // `dynamic` (and Object?) is Object's class, as a `dynamic` literal is:
            // erasure cannot tell them apart, and `T == dynamic` compares the two.
            return w.type;
        }
        return w;
    }

    /// Dart's runtimeType of {@code o}: a reified generic instance answers its arguments.
    public static Object runtimeType(Object o) {
        if (o instanceof Reified) {
            return ((Reified) o).$runtimeType();
        }
        return o == null ? Void.class : o.getClass();
    }

    /// The Dart spelling of this type: `Box<int>`, `int?`, `Map<String, Box<int>?>`.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(DartRuntime.str(type));
        if (args != null) {
            sb.append('<');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args[i] == null ? "dynamic" : args[i].toString());
            }
            sb.append('>');
        }
        if (nullable && type != Object.class) {
            sb.append('?');
        }
        return sb.toString();
    }
}
