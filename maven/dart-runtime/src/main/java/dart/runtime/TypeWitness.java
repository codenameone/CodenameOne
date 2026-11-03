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

    /// Dart's `Object?` where it must print as written: the parameter of a tear-off whose
    /// parameter is covariant (`D<num>().take` is `(Object?) => void`). It tests as any
    /// other nullable Object witness; the plain `of(Object.class, true)` prints `Object`.
    public static final TypeWitness OBJECT_NULLABLE = new TypeWitness(Object.class, true, null);

    /// Dart's `Null` as a recorded type argument (`[null]` is a List<Null>): only null is
    /// an instance, and it prints as `Null`, never `Null?`.
    public static final TypeWitness NULL = new TypeWitness(Void.class, true, null);

    private final Class<?> type;
    private final boolean nullable;
    /// The witnesses of a program generic class's own type arguments
    /// ({@code Box<int>} as a type argument), or null when not tracked.
    private final TypeWitness[] args;
    /// Parameterized witnesses derived from this plain one, interned by argument identity.
    private java.util.ArrayList<TypeWitness> derived;

    /// Dart's function subtyping for a function-type witness ({@link #function}), or null.
    private Tester tester;
    /// A record type's shape: the named fields' names (sorted), null for any other type.
    private String[] recordNamed;
    private int recordPositional;
    private TypeWitness[] fnSig;
    /// A function type's required positional parameter count, -1 when all positional
    /// ones are required; and its named parameters' names in declaration order (a
    /// required one prefixed with '!'), null when it has none. Laid out as FunctionType
    /// reads a closure's signature: fnSig holds the return, the positional parameters,
    /// then the named ones.
    private int fnRequired = -1;
    private String[] fnNamed;

    private TypeWitness(Class<?> type, boolean nullable, TypeWitness[] args) {
        this.type = type;
        this.nullable = nullable;
        this.args = args;
    }

    /// The full test of a non-null value against a function type, as `is` lowers it.
    public interface Tester {
        boolean test(Object value);
    }

    /// The witness of a function type such as `int Function(int)`: its erased SAM class
    /// {@code sam} alone would admit every function of that arity, so a value is tested
    /// by {@code tester}, which applies Dart's function subtyping. Not interned: each
    /// carries its own signature.
    public static TypeWitness function(Class<?> sam, boolean nullable, Tester tester) {
        TypeWitness w = new TypeWitness(sam, nullable, null);
        w.tester = tester;
        return w;
    }

    /// {@link #function(Class, boolean, Tester)} that also carries the function type's
    /// signature, {@code sig}: its return witness first, then one per positional parameter
    /// (null where it is untested). Two such witnesses compare by Dart's function
    /// subtyping in {@link #isSubtype}, so a `List<int Function(int)>` is not a
    /// `List<int Function(num)>`.
    public static TypeWitness function(Class<?> sam, boolean nullable, Tester tester, TypeWitness[] sig) {
        TypeWitness w = function(sam, nullable, tester);
        w.fnSig = sig;
        return w;
    }

    /// {@link #function(Class, boolean, Tester, TypeWitness[])} for a function type with
    /// optional positional or named parameters: {@code required} positional ones are
    /// required (-1 when all are), and {@code named} names the trailing slots of
    /// {@code sig} ('!' marks a required one).
    public static TypeWitness function(Class<?> sam, boolean nullable, Tester tester, TypeWitness[] sig,
            int required, String[] named) {
        TypeWitness w = function(sam, nullable, tester, sig);
        w.fnRequired = required;
        w.fnNamed = named;
        return w;
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
        if (w.tester != null) {
            return function(w.type, true, w.tester, w.fnSig, w.fnRequired, w.fnNamed);
        }
        if (w.args == null) {
            return of(w.type, true);
        }
        return of(w.type, true, w.args);
    }

    /// A record type, `(int, {String name})`: {@code type} is the generated record class of
    /// that shape, {@code fields} the field types' witnesses, positional ones first. The
    /// test is structural -- each field of the value against its type -- as Dart's is.
    public static TypeWitness record(Class<?> type, boolean nullable, int positional, String[] named,
            TypeWitness... fields) {
        TypeWitness w = of(type, nullable, fields);
        w.recordPositional = positional;
        w.recordNamed = named;
        return w;
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
        if (value == null) {
            return nullable;
        }
        if (tester != null) {
            return tester.test(value);
        }
        if (recordNamed != null) {
            if (!type.isInstance(value) || !(value instanceof dart.core.Record)) {
                return false;
            }
            dart.core.Record r = (dart.core.Record) value;
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !args[i].test(r.$field(i))) {
                    return false;
                }
            }
            return true;
        }
        if (args != null && args.length > 0) {
            // A core collection's witness with element arguments (`List<String>`): the
            // arguments the collection recorded are compared, as `is List<String>` does.
            // The emitter records a nested collection argument by its interface
            // (`Map<String, List<int>>` witnesses java.util.List with int), which is the
            // same kind: without it the element type went unchecked on `m[k] = <String>[]`.
            int kind = type == dart.core.DartList.class || type == java.util.List.class ? LIST
                    : type == dart.core.DartSet.class || type == java.util.Set.class ? SET
                    : type == dart.core.DartMap.class || type == java.util.Map.class ? MAP
                    : type == dart.core.DartIterable.class || type == Iterable.class ? ITERABLE : -1;
            if (kind >= 0) {
                return isCollectionOf(value, kind, type, args[0], args.length > 1 ? args[1] : null);
            }
        }
        return type.isInstance(value);
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
        if (actual == NULL) {
            // Null is a subtype of every nullable type, and of no other.
            return target.nullable;
        }
        if (actual.nullable && !target.nullable) {
            return false;
        }
        if (!target.type.isAssignableFrom(actual.type)) {
            return false;
        }
        if (target.fnSig != null && actual.fnSig != null && target.fnSig.length == actual.fnSig.length
                && target.fnNamed == null && actual.fnNamed == null) {
            // Return covariant, parameters contravariant.
            if (!isSubtype(actual.fnSig[0], target.fnSig[0])) {
                return false;
            }
            for (int i = 1; i < target.fnSig.length; i++) {
                if (!isSubtype(target.fnSig[i], actual.fnSig[i])) {
                    return false;
                }
            }
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

    /// Dart's checked cast of a value to a non-generic function type (`dyn as void
    /// Function(int)`, and the implicit downcast a write of a dynamic value into a
    /// function-typed field, local or argument is). The test is {@link #isFunction}, so a
    /// signed closure is judged by its reified signature -- parameters contravariant, the
    /// return covariant -- and an unsigned Java lambda by its functional interface. A value
    /// that passes is handed over in the slot's Java shape {@code target}: a `(num x) {}`
    /// closure is held as a Func1 but a `void Function(int)` slot is a VoidFunc1, so it is
    /// wrapped to call through {@link Dyn#invoke}. A failure is a TypeError.
    public static Object asFunction(Object f, Class<?> target, boolean nullable, String dartType,
            Class<?> valueSam, Class<?> voidSam, boolean topReturn, TypeWitness ret, int required,
            String[] named, TypeWitness... params) {
        if (f == null) {
            if (nullable) {
                return null;
            }
        } else if (isFunction(f, valueSam, voidSam, topReturn, ret, required, named, params)) {
            return target.isInstance(f) ? f : adaptFunction(f, target);
        }
        throw new dart.core.TypeError("type '" + DartRuntime.typeName(f) + "' is not a subtype of type '"
                + dartType + "' in type cast");
    }

    /// An argument of a generic function whose type variable has a self-referential bound
    /// (`T extends Comparable<T>`), entered instantiated at {@code w}: the instantiation
    /// itself must satisfy the bound, which the emitter decided from the type model as the
    /// classes in {@code satisfying} (num is a Comparable<num>; int, a Comparable<num>, is
    /// no Comparable<int>). Then {@code v} is tested against {@code w}. Either failure is a
    /// TypeError.
    public static <T> T checkedSelfBound(T v, TypeWitness w, String bound, Class<?>... satisfying) {
        boolean ok = false;
        for (Class<?> c : satisfying) {
            if (c == w.type()) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new dart.core.TypeError("type '" + w + "' is not a subtype of type '" + bound
                    + "' of the type variable's bound");
        }
        return DartRuntime.checkedArg(v, w);
    }

    private static Object adaptFunction(final Object f, Class<?> target) {
        if (target == Funcs.Func0.class) {
            return (Funcs.Func0<Object>) () -> Dyn.invoke(f);
        }
        if (target == Funcs.Func1.class) {
            return (Funcs.Func1<Object, Object>) (a) -> Dyn.invoke(f, a);
        }
        if (target == Funcs.Func2.class) {
            return (Funcs.Func2<Object, Object, Object>) (a, b) -> Dyn.invoke(f, a, b);
        }
        if (target == Funcs.Func3.class) {
            return (Funcs.Func3<Object, Object, Object, Object>) (a, b, c) -> Dyn.invoke(f, a, b, c);
        }
        if (target == Funcs.Func4.class) {
            return (Funcs.Func4<Object, Object, Object, Object, Object>) (a, b, c, d) -> Dyn.invoke(f, a, b, c, d);
        }
        if (target == Funcs.Func5.class) {
            return (Funcs.Func5<Object, Object, Object, Object, Object, Object>) (a, b, c, d, e) ->
                    Dyn.invoke(f, a, b, c, d, e);
        }
        if (target == Funcs.VoidFunc0.class) {
            return (Funcs.VoidFunc0) () -> Dyn.invoke(f);
        }
        if (target == Funcs.VoidFunc1.class) {
            return (Funcs.VoidFunc1<Object>) (a) -> Dyn.invoke(f, a);
        }
        if (target == Funcs.VoidFunc2.class) {
            return (Funcs.VoidFunc2<Object, Object>) (a, b) -> Dyn.invoke(f, a, b);
        }
        if (target == Funcs.VoidFunc3.class) {
            return (Funcs.VoidFunc3<Object, Object, Object>) (a, b, c) -> Dyn.invoke(f, a, b, c);
        }
        if (target == Funcs.VoidFunc4.class) {
            return (Funcs.VoidFunc4<Object, Object, Object, Object>) (a, b, c, d) -> Dyn.invoke(f, a, b, c, d);
        }
        if (target == Funcs.VoidFunc5.class) {
            return (Funcs.VoidFunc5<Object, Object, Object, Object, Object>) (a, b, c, d, e) ->
                    Dyn.invoke(f, a, b, c, d, e);
        }
        // A slot typed by an interface outside the canonical shapes: the value passed the
        // Dart test, so it is handed over as it is.
        return f;
    }

    /// `f is R Function<T0..Tn>(P..)`, a generic function type: only a generic closure with
    /// as many type parameters and the same positional arity is one. A slot of the target
    /// that is its i-th type parameter ({@code slotVars}: return first, then each
    /// parameter; -1 for a slot that is not one) must be the closure's i-th type parameter;
    /// every other slot is tested as {@link #isFunction} tests it. Type parameter bounds
    /// are not recorded on either side, so both are compared as unbounded.
    public static boolean isGenericFunction(Object f, int typeParamCount, int[] slotVars, TypeWitness ret,
            TypeWitness... params) {
        if (!(f instanceof Signed) || !((Signed) f).$generic()) {
            return false;
        }
        Signed s = (Signed) f;
        String[] vars = s.$typeVars();
        TypeWitness[] sig = s.$sig();
        if (vars == null || vars.length != sig.length + 1 || s.$named() != null || s.$required() >= 0
                || sig.length != params.length + 1) {
            return false;
        }
        String[] own = vars[0].split(", ");
        if (own.length != typeParamCount) {
            return false;
        }
        for (int slot = 0; slot < sig.length; slot++) {
            String v = vars[slot + 1];
            int ownIndex = -1;
            for (int i = 0; v != null && i < own.length; i++) {
                if (own[i].equals(v)) {
                    ownIndex = i;
                }
            }
            if (ownIndex != slotVars[slot]) {
                return false;
            }
            if (ownIndex < 0) {
                boolean ok = slot == 0 ? ret == null || sig[0] == null || isSubtype(sig[0], ret)
                        : paramOk(params[slot - 1], sig[slot]);
                if (!ok) {
                    return false;
                }
            }
        }
        return true;
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
    /// `Stream<X>`, `Future<X>` and `StreamController<X>`: read through the type argument
    /// the construction recorded (Stream.reify, Future.reify, StreamController.reify).
    public static final int STREAM = 4;
    public static final int FUTURE = 5;
    public static final int STREAM_CONTROLLER = 6;

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
            case STREAM:
            case FUTURE:
            case STREAM_CONTROLLER: {
                if (!erased.isInstance(v)) {
                    return false;
                }
                // Nothing recorded the argument (a stream or future some runtime path made):
                // unknown, which keeps the erased answer, as for a collection.
                TypeWitness w = v instanceof dart.async.Stream ? ((dart.async.Stream<?>) v).typeArgWitness()
                        : v instanceof dart.async.Future ? ((dart.async.Future<?>) v).typeArgWitness()
                        : v instanceof dart.async.StreamController
                                ? ((dart.async.StreamController<?>) v).typeArgWitness() : null;
                return a0 == null || w == null || isSubtype(w, a0);
            }
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
        return a0 == null ? DartRuntime.genericArity(c, 1) : of(c, false, a0);
    }

    public static Object typeValue(Class<?> c, TypeWitness a0, TypeWitness a1) {
        return a0 == null && a1 == null ? DartRuntime.genericArity(c, 2) : of(c, false, a0, a1);
    }

    public static Object typeValue(Class<?> c, TypeWitness... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                return of(c, false, args);
            }
        }
        // Every argument dynamic: the bare class, which prints them (`Holder<dynamic>`).
        return DartRuntime.genericArity(c, args.length);
    }

    /// The Type value of a type variable whose witness is {@code w} (`T` as an expression).
    public static Object value(TypeWitness w) {
        if (w == null) {
            return Object.class;
        }
        if (w.args == null && w.fnSig == null && (!w.nullable || w.type == Object.class)) {
            // `dynamic` (and Object?) is Object's class, as a `dynamic` literal is:
            // erasure cannot tell them apart, and `T == dynamic` compares the two.
            return w.type;
        }
        return rawIfDynamic(w);
    }

    /// A generic type whose arguments are all dynamic is the raw class's Type, as the raw
    /// literal is: Dart's `Holder == Holder<dynamic>` and `<dynamic>[].runtimeType == List`
    /// are true, and the class prints its arguments (`Holder<dynamic>`).
    private static Object rawIfDynamic(TypeWitness w) {
        if (w.args == null || w.nullable || w.fnSig != null || w.recordNamed != null || w.args.length == 0
                || w.type == dart.core.DartLongList.class || w.type == dart.core.DartDoubleList.class) {
            return w;
        }
        for (int i = 0; i < w.args.length; i++) {
            if (w.args[i] != null && w.args[i] != DYNAMIC) {
                return w;
            }
        }
        return DartRuntime.genericArity(w.type, w.args.length);
    }

    /// Function-type witnesses are built per evaluation, so two spellings of one function
    /// type (a typedef and its inline form) compare by signature, as Dart's Types do.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (fnSig == null) {
            return false;
        }
        if (o instanceof FunctionType) {
            // A closure's runtimeType against the function type's literal.
            return !nullable && o.equals(signature());
        }
        if (!(o instanceof TypeWitness)) {
            return false;
        }
        TypeWitness t = (TypeWitness) o;
        return t.fnSig != null && t.nullable == nullable && voidSam(t.type) == voidSam(type)
                && signature().equals(t.signature());
    }

    @Override
    public int hashCode() {
        if (fnSig == null) {
            return System.identityHashCode(this);
        }
        // As FunctionType hashes, since the two compare equal.
        int h = signature().hashCode();
        return nullable ? h + 7 : h;
    }

    /// Dart prints a void function's return as void; its return slot is untested (null).
    private static final TypeWitness VOID_RETURN = new TypeWitness(Void.class, true, null);

    /// The signature of a non-nullable function-type witness, null for any other.
    FunctionType functionType() {
        return fnSig == null || nullable ? null : signature();
    }

    private FunctionType signature() {
        TypeWitness[] sig = fnSig;
        if (voidSam(type)) {
            sig = sig.clone();
            sig[0] = VOID_RETURN;
        }
        return new FunctionType(sig, fnRequired, fnNamed);
    }

    /// Dart's runtimeType of {@code o}: a reified generic instance answers its arguments.
    public static Object runtimeType(Object o) {
        if (o instanceof dart.core.MixinView) {
            // A view (a boxed List<int> seen as DartLongList, a program Set seen as a Set)
            // is the object behind it, whose type is the one Dart reports.
            return runtimeType(((dart.core.MixinView) o).source());
        }
        if (o instanceof Reified) {
            Object t = ((Reified) o).$runtimeType();
            return t instanceof TypeWitness ? rawIfDynamic((TypeWitness) t) : t;
        }
        if (o instanceof Signed) {
            // A generic closure too: FunctionType prints its type parameters, renamed in
            // order as Dart does (`<Y0>(Y0) => Y0`).
            return new FunctionType((Signed) o);
        }
        return o == null ? Void.class : o.getClass();
    }

    private static boolean voidSam(Class<?> c) {
        return c == Funcs.VoidFunc0.class || c == Funcs.VoidFunc1.class || c == Funcs.VoidFunc2.class
                || c == Funcs.VoidFunc3.class || c == Funcs.VoidFunc4.class || c == Funcs.VoidFunc5.class;
    }

    /// The Dart spelling of this type: `Box<int>`, `int?`, `Map<String, Box<int>?>`.
    @Override
    public String toString() {
        if (this == DYNAMIC) {
            return "dynamic";
        }
        if (recordNamed != null) {
            StringBuilder rb = new StringBuilder("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    rb.append(", ");
                }
                if (i == recordPositional) {
                    rb.append('{');
                }
                rb.append(args[i] == null ? "dynamic" : args[i].toString());
                if (i >= recordPositional) {
                    rb.append(' ').append(recordNamed[i - recordPositional]);
                }
            }
            if (args.length > recordPositional) {
                rb.append('}');
            }
            rb.append(')');
            return nullable ? rb.append('?').toString() : rb.toString();
        }
        if (fnSig != null) {
            // A function type (a typedef used as a value): `(String, {int? b}) => int`,
            // spelled as a closure's runtimeType is.
            String fb = signature().toString();
            return nullable ? "(" + fb + ")?" : fb;
        }
        StringBuilder sb = new StringBuilder(args == null ? DartRuntime.str(type) : DartRuntime.className(type));
        // A primitive-backed list class already names its element type (`List<int>`).
        if (args != null && type != dart.core.DartLongList.class && type != dart.core.DartDoubleList.class) {
            sb.append('<');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args[i] == null ? "dynamic" : args[i].toString());
            }
            sb.append('>');
        }
        if (this == OBJECT_NULLABLE) {
            return "Object?";
        }
        if (nullable && type != Object.class && type != Void.class) {
            sb.append('?');
        }
        return sb.toString();
    }
}
