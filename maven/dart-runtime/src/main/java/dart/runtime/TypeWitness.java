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

    /// Dart's `dynamic` as a recorded type argument: top, like Object?, but its own type --
    /// a value of it is no int, and it prints as `dynamic`. A null witness instead is an
    /// argument nothing could record.
    public static final TypeWitness DYNAMIC = new TypeWitness(Object.class, true, null);

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

    /// The witness admitting null too. An unknown witness (an unreified type variable's,
    /// null) stays unknown: `T?` of an unknown T is unknown as well.
    public static TypeWitness nullable(TypeWitness w) {
        if (w == null) {
            return null;
        }
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
            // dynamic, Object? (and void) are top.
            return true;
        }
        if (actual == DYNAMIC) {
            return false;
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

    /// `f is R Function(P0, .., [Pk, ..])` or `R Function(P0, .., {Pk name, ..})`, Dart's
    /// function subtyping in full: a signed closure (Signed) answers from its signature
    /// alone -- it needs no more required positional parameters than the target has, at
    /// least as many positional ones, every named one the target has (and requires only
    /// named ones the target requires); parameters are contravariant and the return
    /// covariant. A null witness on either side is a type nothing could name and is not
    /// tested. An unsigned function is tested by its SAM: {@code valueSam} (and
    /// {@code voidSam} too when the target's return is top).
    ///
    /// @param topReturn the target returns void, dynamic or Object?
    /// @param required  how many of the target's positional parameters are required; -1
    ///                  when all are
    /// @param named     the target's named parameters (`!name` when required), whose
    ///                  witnesses follow the positional ones in {@code params}; or null
    public static boolean isFunction(Object f, Class<?> valueSam, Class<?> voidSam, boolean topReturn,
            TypeWitness ret, int required, String[] named, TypeWitness... params) {
        if (!(f instanceof Signed)) {
            return valueSam.isInstance(f) || topReturn && voidSam.isInstance(f);
        }
        Signed s = (Signed) f;
        if (s.$generic()) {
            return false;
        }
        TypeWitness[] sig = s.$sig();
        String[] fNamed = s.$named();
        int gNamedCount = named == null ? 0 : named.length;
        int fNamedCount = fNamed == null ? 0 : fNamed.length;
        int gPos = params.length - gNamedCount;
        int gReq = required < 0 ? gPos : required;
        int fPos = sig.length - 1 - fNamedCount;
        int fReq = s.$required() < 0 ? fPos : s.$required();
        if (fReq > gReq || fPos < gPos) {
            return false;
        }
        if (gNamedCount > 0 && (fReq != gPos || fPos != gPos)) {
            // A target with named parameters is met only by exactly its positional ones.
            return false;
        }
        for (int i = 0; i < gPos; i++) {
            if (!paramOk(params[i], sig[i + 1])) {
                return false;
            }
        }
        for (int i = 0; i < fNamedCount; i++) {
            String fn = fNamed[i];
            boolean fRequired = fn.charAt(0) == '!';
            String bare = fRequired ? fn.substring(1) : fn;
            int g = indexOfNamed(named, bare);
            if (fRequired && (g < 0 || named[g].charAt(0) != '!')) {
                return false;
            }
            if (g >= 0 && !paramOk(params[gPos + g], sig[1 + fPos + i])) {
                return false;
            }
        }
        for (int g = 0; g < gNamedCount; g++) {
            String gn = named[g].charAt(0) == '!' ? named[g].substring(1) : named[g];
            if (indexOfNamed(fNamed, gn) < 0) {
                return false;
            }
        }
        return topReturn || ret == null || sig[0] == null || isSubtype(sig[0], ret);
    }

    private static boolean paramOk(TypeWitness target, TypeWitness own) {
        return target == null || own == null || isSubtype(target, own);
    }

    private static int indexOfNamed(String[] names, String bare) {
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                String n = names[i];
                int off = n.charAt(0) == '!' ? 1 : 0;
                if (n.length() - off == bare.length() && n.startsWith(bare, off)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /// Collection kinds for {@link #isCollectionOf}.
    public static final int LIST = 0;
    public static final int SET = 1;
    public static final int MAP = 2;
    public static final int ITERABLE = 3;

    /// `v is List<X>`/`Set<X>`/`Map<K, V>`/`Iterable<X>` with reified arguments: the
    /// erased class first, then the arguments the collection recorded (its elements where
    /// it recorded none). A null argument witness is not tested.
    public static boolean isCollectionOf(Object v, int kind, Class<?> erased, TypeWitness a0, TypeWitness a1) {
        switch (kind) {
            case LIST:
                return a0 == null ? erased.isInstance(v) || v instanceof dart.collection.ListMixin
                        : dart.core.DartList.isListOfW(v, a0);
            case SET:
                if (v instanceof dart.core.DartSet) {
                    return a0 == null || dart.core.DartSet.isSetOf(v, a0);
                }
                return dart.core.SetMixinView.isSet(v);
            case MAP:
                return erased.isInstance(v) && (!(v instanceof java.util.Map)
                        || dart.core.DartMap.isMapOf(v, a0, a1));
            default:
                if (a0 != null && (v instanceof dart.core.DartList || v instanceof dart.collection.ListMixin)) {
                    return dart.core.DartList.isListOfW(v, a0);
                }
                if (a0 != null && v instanceof dart.core.DartSet) {
                    return dart.core.DartSet.isSetOf(v, a0);
                }
                if (a0 != null && v instanceof dart.core.DartIterable) {
                    // A lazy iterable (a sync* generator, a map view) that records its
                    // element type is tested by it; an unrecorded one is unknown and passes.
                    TypeWitness w = ((dart.core.DartIterable<?>) v).elementWitness();
                    return erased.isInstance(v) && (w == null || isSubtype(w, a0));
                }
                return erased.isInstance(v) || v instanceof dart.core.DartList || v instanceof dart.core.DartSet;
        }
    }

    /// `v as List<X>` (and the other kinds): {@code v} itself when the test holds, else
    /// Dart's TypeError naming {@code target}.
    public static Object checkCast(Object v, int kind, Class<?> erased, TypeWitness a0, TypeWitness a1,
            boolean nullable, String target) {
        if (v == null ? nullable : isCollectionOf(v, kind, erased, a0, a1)) {
            return v;
        }
        throw new dart.core.TypeError("type '" + DartRuntime.str(runtimeType(v)) + "' is not a subtype of type '"
                + target + "' in type cast");
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
        if (o instanceof Signed) {
            // A generic closure too: FunctionType prints its type parameters, renamed in
            // order as Dart does (`<Y0>(Y0) => Y0`).
            return new FunctionType((Signed) o);
        }
        return o == null ? Void.class : o.getClass();
    }

    /// The Dart spelling of this type: `Box<int>`, `int?`, `Map<String, Box<int>?>`.
    @Override
    public String toString() {
        if (this == DYNAMIC) {
            return "dynamic";
        }
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
