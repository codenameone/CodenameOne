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

/// Closures whose Dart signature is known only at run time: a closure inside a generic
/// method, `(T x) => x`, is a `int Function(int)` for `g<int>()` and a
/// `String Function(String)` for `g<String>()`. The static signature markers
/// (DartFnSigs.Mn, one constant per signature) cannot hold a type argument's witness,
/// so such a closure is wrapped here with the witnesses its enclosing call received.
/// Calls go straight to the wrapped closure.
public final class SignedFns {
    private SignedFns() {
    }

    /// The Signed half every wrapper shares.
    private abstract static class Base implements Signed {
        private final TypeWitness[] sig;
        private final int required;
        private final String[] named;

        Base(TypeWitness[] sig, int required, String[] named) {
            this.sig = sig;
            this.required = required;
            this.named = named;
        }

        @Override
        public final TypeWitness[] $sig() {
            return sig;
        }

        @Override
        public final int $required() {
            return required;
        }

        @Override
        public final String[] $named() {
            return named;
        }

        /// For a generic closure (see generic): its type variables as Signed.$typeVars
        /// gives them, and what builds it for a list of type argument witnesses.
        private String[] typeVars;
        private Funcs.Func1<TypeWitness[], ?> instantiator;

        @Override
        public final boolean $generic() {
            return instantiator != null;
        }

        @Override
        public final String[] $typeVars() {
            return typeVars;
        }
    }

    /// A generic closure (`<T>(T x) => [x]`) whose body reads its type parameters: built by
    /// {@code instantiator} once per instantiation, from one witness per type parameter
    /// (null for one that is unknown). The value returned is the closure built with every
    /// witness unknown -- what a dynamic call, or one with nothing to infer, runs -- and
    /// carries the signature the built closure was signed with, still generic.
    @SuppressWarnings("unchecked")
    public static <F> F generic(Funcs.Func1<TypeWitness[], F> instantiator, int typeParams) {
        Object f = instantiator.call(new TypeWitness[typeParams]);
        Signed s = f instanceof Signed ? (Signed) f : null;
        Base b = wrap(f, s != null ? s.$sig() : unknownSig(f), s != null ? s.$required() : -1,
                s != null ? s.$named() : null);
        b.typeVars = s != null ? s.$typeVars() : null;
        b.instantiator = instantiator;
        return (F) b;
    }

    /// {@code f} instantiated with {@code typeArgs} when it is a generic closure built by
    /// generic(); any other value as it is. The result is no longer generic: each signature
    /// slot that is a type parameter is that parameter's witness.
    public static Object instantiate(Object f, TypeWitness[] typeArgs) {
        if (!(f instanceof Base) || ((Base) f).instantiator == null) {
            return f;
        }
        Base g = (Base) f;
        TypeWitness[] gsig = g.$sig();
        TypeWitness[] sig = new TypeWitness[gsig.length];
        System.arraycopy(gsig, 0, sig, 0, gsig.length);
        String[] tv = g.typeVars;
        if (tv != null && tv.length > 0 && tv[0] != null) {
            for (int i = 0; i < sig.length && i + 1 < tv.length; i++) {
                int at = typeVarIndex(tv[0], tv[i + 1]);
                if (at >= 0 && at < typeArgs.length && typeArgs[at] != null) {
                    sig[i] = typeArgs[at];
                }
            }
        }
        return wrap(g.instantiator.call(typeArgs), sig, g.$required(), g.$named());
    }

    /// The position of {@code name} in a `T, U` type parameter list, or -1.
    private static int typeVarIndex(String names, String name) {
        if (name == null) {
            return -1;
        }
        int index = 0;
        int start = 0;
        while (start <= names.length()) {
            int comma = names.indexOf(',', start);
            int end = comma < 0 ? names.length() : comma;
            if (names.substring(start, end).trim().equals(name)) {
                return index;
            }
            if (comma < 0) {
                return -1;
            }
            index++;
            start = comma + 1;
        }
        return -1;
    }

    /// A signature that tests nothing, for a closure built unsigned.
    private static TypeWitness[] unknownSig(Object f) {
        return new TypeWitness[arity(f) + 1];
    }

    private static int arity(Object f) {
        if (f instanceof Funcs.Func0 || f instanceof Funcs.VoidFunc0) {
            return 0;
        }
        if (f instanceof Funcs.Func1 || f instanceof Funcs.VoidFunc1) {
            return 1;
        }
        if (f instanceof Funcs.Func2 || f instanceof Funcs.VoidFunc2) {
            return 2;
        }
        if (f instanceof Funcs.Func3 || f instanceof Funcs.VoidFunc3) {
            return 3;
        }
        if (f instanceof Funcs.Func4 || f instanceof Funcs.VoidFunc4) {
            return 4;
        }
        return 5;
    }

    /// A closure with named parameters, given the names a dynamic call places its
    /// arguments by ($named) when it carries no signature of its own; one already signed
    /// keeps its own, which also records its parameter types. Parameter types it lacks
    /// stay untested here (null witnesses): the closure's own casts check them.
    public static Object named(Object f, int params, int required, String[] named) {
        if (f instanceof Signed || f == null) {
            return f;
        }
        return wrap(f, new TypeWitness[params + 1], required, named);
    }

    /// {@code f}, a closure of any Funcs SAM, signed with {@code sig}.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Base wrap(Object f, TypeWitness[] sig, int required, String[] named) {
        if (f instanceof Funcs.Func0) {
            return new F0((Funcs.Func0) f, sig, required, named);
        }
        if (f instanceof Funcs.Func1) {
            return new F1((Funcs.Func1) f, sig, required, named);
        }
        if (f instanceof Funcs.Func2) {
            return new F2((Funcs.Func2) f, sig, required, named);
        }
        if (f instanceof Funcs.Func3) {
            return new F3((Funcs.Func3) f, sig, required, named);
        }
        if (f instanceof Funcs.Func4) {
            return new F4((Funcs.Func4) f, sig, required, named);
        }
        if (f instanceof Funcs.Func5) {
            return new F5((Funcs.Func5) f, sig, required, named);
        }
        if (f instanceof Funcs.VoidFunc0) {
            return new V0((Funcs.VoidFunc0) f, sig, required, named);
        }
        if (f instanceof Funcs.VoidFunc1) {
            return new V1((Funcs.VoidFunc1) f, sig, required, named);
        }
        if (f instanceof Funcs.VoidFunc2) {
            return new V2((Funcs.VoidFunc2) f, sig, required, named);
        }
        if (f instanceof Funcs.VoidFunc3) {
            return new V3((Funcs.VoidFunc3) f, sig, required, named);
        }
        if (f instanceof Funcs.VoidFunc4) {
            return new V4((Funcs.VoidFunc4) f, sig, required, named);
        }
        if (f instanceof Funcs.VoidFunc5) {
            return new V5((Funcs.VoidFunc5) f, sig, required, named);
        }
        throw new IllegalArgumentException("not a closure: " + f);
    }

    private static final class F0<R> extends Base implements Funcs.Func0<R> {
        private final Funcs.Func0<R> f;

        F0(Funcs.Func0<R> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public R call() {
            return f.call();
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <R> Funcs.Func0<R> func0(Funcs.Func0<R> f, TypeWitness[] sig, int required,
            String[] named) {
        return new F0<>(f, sig, required, named);
    }

    private static final class F1<A, R> extends Base implements Funcs.Func1<A, R> {
        private final Funcs.Func1<A, R> f;

        F1(Funcs.Func1<A, R> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public R call(A a) {
            return f.call(a);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, R> Funcs.Func1<A, R> func1(Funcs.Func1<A, R> f, TypeWitness[] sig, int required,
            String[] named) {
        return new F1<>(f, sig, required, named);
    }

    private static final class F2<A, B, R> extends Base implements Funcs.Func2<A, B, R> {
        private final Funcs.Func2<A, B, R> f;

        F2(Funcs.Func2<A, B, R> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public R call(A a, B b) {
            return f.call(a, b);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B, R> Funcs.Func2<A, B, R> func2(Funcs.Func2<A, B, R> f, TypeWitness[] sig, int required,
            String[] named) {
        return new F2<>(f, sig, required, named);
    }

    private static final class F3<A, B, C, R> extends Base implements Funcs.Func3<A, B, C, R> {
        private final Funcs.Func3<A, B, C, R> f;

        F3(Funcs.Func3<A, B, C, R> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public R call(A a, B b, C c) {
            return f.call(a, b, c);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B, C, R> Funcs.Func3<A, B, C, R> func3(Funcs.Func3<A, B, C, R> f, TypeWitness[] sig, int required,
            String[] named) {
        return new F3<>(f, sig, required, named);
    }

    private static final class F4<A, B, C, D, R> extends Base implements Funcs.Func4<A, B, C, D, R> {
        private final Funcs.Func4<A, B, C, D, R> f;

        F4(Funcs.Func4<A, B, C, D, R> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public R call(A a, B b, C c, D d) {
            return f.call(a, b, c, d);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B, C, D, R> Funcs.Func4<A, B, C, D, R> func4(Funcs.Func4<A, B, C, D, R> f, TypeWitness[] sig, int required,
            String[] named) {
        return new F4<>(f, sig, required, named);
    }

    private static final class F5<A, B, C, D, E, R> extends Base implements Funcs.Func5<A, B, C, D, E, R> {
        private final Funcs.Func5<A, B, C, D, E, R> f;

        F5(Funcs.Func5<A, B, C, D, E, R> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public R call(A a, B b, C c, D d, E e) {
            return f.call(a, b, c, d, e);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B, C, D, E, R> Funcs.Func5<A, B, C, D, E, R> func5(Funcs.Func5<A, B, C, D, E, R> f, TypeWitness[] sig, int required,
            String[] named) {
        return new F5<>(f, sig, required, named);
    }

    private static final class V0 extends Base implements Funcs.VoidFunc0 {
        private final Funcs.VoidFunc0 f;

        V0(Funcs.VoidFunc0 f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public void call() {
            f.call();
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static Funcs.VoidFunc0 voidFunc0(Funcs.VoidFunc0 f, TypeWitness[] sig, int required,
            String[] named) {
        return new V0(f, sig, required, named);
    }

    private static final class V1<A> extends Base implements Funcs.VoidFunc1<A> {
        private final Funcs.VoidFunc1<A> f;

        V1(Funcs.VoidFunc1<A> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public void call(A a) {
            f.call(a);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A> Funcs.VoidFunc1<A> voidFunc1(Funcs.VoidFunc1<A> f, TypeWitness[] sig, int required,
            String[] named) {
        return new V1<>(f, sig, required, named);
    }

    private static final class V2<A, B> extends Base implements Funcs.VoidFunc2<A, B> {
        private final Funcs.VoidFunc2<A, B> f;

        V2(Funcs.VoidFunc2<A, B> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public void call(A a, B b) {
            f.call(a, b);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B> Funcs.VoidFunc2<A, B> voidFunc2(Funcs.VoidFunc2<A, B> f, TypeWitness[] sig, int required,
            String[] named) {
        return new V2<>(f, sig, required, named);
    }

    private static final class V3<A, B, C> extends Base implements Funcs.VoidFunc3<A, B, C> {
        private final Funcs.VoidFunc3<A, B, C> f;

        V3(Funcs.VoidFunc3<A, B, C> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public void call(A a, B b, C c) {
            f.call(a, b, c);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B, C> Funcs.VoidFunc3<A, B, C> voidFunc3(Funcs.VoidFunc3<A, B, C> f, TypeWitness[] sig, int required,
            String[] named) {
        return new V3<>(f, sig, required, named);
    }

    private static final class V4<A, B, C, D> extends Base implements Funcs.VoidFunc4<A, B, C, D> {
        private final Funcs.VoidFunc4<A, B, C, D> f;

        V4(Funcs.VoidFunc4<A, B, C, D> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public void call(A a, B b, C c, D d) {
            f.call(a, b, c, d);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B, C, D> Funcs.VoidFunc4<A, B, C, D> voidFunc4(Funcs.VoidFunc4<A, B, C, D> f, TypeWitness[] sig, int required,
            String[] named) {
        return new V4<>(f, sig, required, named);
    }

    private static final class V5<A, B, C, D, E> extends Base implements Funcs.VoidFunc5<A, B, C, D, E> {
        private final Funcs.VoidFunc5<A, B, C, D, E> f;

        V5(Funcs.VoidFunc5<A, B, C, D, E> f, TypeWitness[] sig, int required, String[] named) {
            super(sig, required, named);
            this.f = f;
        }

        @Override
        public void call(A a, B b, C c, D d, E e) {
            f.call(a, b, c, d, e);
        }
    }

    /// `f` signed with `sig` (return witness, then each parameter's; see Signed).
    public static <A, B, C, D, E> Funcs.VoidFunc5<A, B, C, D, E> voidFunc5(Funcs.VoidFunc5<A, B, C, D, E> f, TypeWitness[] sig, int required,
            String[] named) {
        return new V5<>(f, sig, required, named);
    }

    /// A closure with more named parameters than its static type names, seen as that type:
    /// `[({int a}) => a, ({int a, int b}) => a + b]` is a `List<({int a}) => int>`, the
    /// named parameters both share. Each named parameter is its own positional slot of the
    /// Java SAM, so the wider closure is a different SAM (a Func2 against a Func1) that no
    /// cast can make the narrower one; it is converted here. A static call goes through the
    /// narrow SAM, the {@code keep} names placed by name, any it omits (the absent marker)
    /// left out so the wide closure applies its own defaults. As Signed it answers the WIDE
    /// closure's signature -- its runtimeType and every `is` test are the closure's own, as
    /// in Dart -- and a dynamic call reaches the wide closure itself (unwrap).
    public static Object narrowNamed(Object wide, int positional, String[] keep, boolean isVoid) {
        if (wide == null || !(wide instanceof Signed)) {
            return wide;
        }
        int n = positional + keep.length;
        Narrowed r;
        switch (n) {
            case 0: r = isVoid ? new NV0() : new N0(); break;
            case 1: r = isVoid ? new NV1() : new N1(); break;
            case 2: r = isVoid ? new NV2() : new N2(); break;
            case 3: r = isVoid ? new NV3() : new N3(); break;
            case 4: r = isVoid ? new NV4() : new N4(); break;
            case 5: r = isVoid ? new NV5() : new N5(); break;
            default: throw new IllegalArgumentException("too many parameters: " + n);
        }
        r.wide = (Signed) wide;
        r.positional = positional;
        r.keep = keep;
        return r;
    }

    /// The closure a narrowNamed view stands for, or {@code f} itself.
    public static Object unwrap(Object f) {
        return f instanceof Narrowed ? ((Narrowed) f).wide : f;
    }

    private abstract static class Narrowed implements Signed {
        Signed wide;
        int positional;
        String[] keep;

        final Object fwd(Object... a) {
            Object[] pos = new Object[positional];
            System.arraycopy(a, 0, pos, 0, positional);
            int k = 0;
            for (int i = positional; i < a.length; i++) {
                if (!DartRuntime.isAbsent(a[i])) {
                    k++;
                }
            }
            String[] names = new String[k];
            Object[] values = new Object[k];
            k = 0;
            for (int i = positional; i < a.length; i++) {
                if (!DartRuntime.isAbsent(a[i])) {
                    String nm = keep[i - positional];
                    names[k] = nm.startsWith("!") ? nm.substring(1) : nm;
                    values[k++] = a[i];
                }
            }
            return Dyn.invokeNamed(wide, pos, names, values);
        }

        @Override
        public final TypeWitness[] $sig() {
            return wide.$sig();
        }

        @Override
        public final int $required() {
            return wide.$required();
        }

        @Override
        public final String[] $named() {
            return wide.$named();
        }

        @Override
        public final boolean $generic() {
            return wide.$generic();
        }

        @Override
        public final String[] $typeVars() {
            return wide.$typeVars();
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class N0 extends Narrowed implements Funcs.Func0 {
        @Override
        public Object call() {
            return fwd();
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class NV0 extends Narrowed implements Funcs.VoidFunc0 {
        @Override
        public void call() {
            fwd();
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class N1 extends Narrowed implements Funcs.Func1 {
        @Override
        public Object call(Object a) {
            return fwd(a);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class NV1 extends Narrowed implements Funcs.VoidFunc1 {
        @Override
        public void call(Object a) {
            fwd(a);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class N2 extends Narrowed implements Funcs.Func2 {
        @Override
        public Object call(Object a, Object b) {
            return fwd(a, b);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class NV2 extends Narrowed implements Funcs.VoidFunc2 {
        @Override
        public void call(Object a, Object b) {
            fwd(a, b);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class N3 extends Narrowed implements Funcs.Func3 {
        @Override
        public Object call(Object a, Object b, Object c) {
            return fwd(a, b, c);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class NV3 extends Narrowed implements Funcs.VoidFunc3 {
        @Override
        public void call(Object a, Object b, Object c) {
            fwd(a, b, c);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class N4 extends Narrowed implements Funcs.Func4 {
        @Override
        public Object call(Object a, Object b, Object c, Object d) {
            return fwd(a, b, c, d);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class NV4 extends Narrowed implements Funcs.VoidFunc4 {
        @Override
        public void call(Object a, Object b, Object c, Object d) {
            fwd(a, b, c, d);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class N5 extends Narrowed implements Funcs.Func5 {
        @Override
        public Object call(Object a, Object b, Object c, Object d, Object e) {
            return fwd(a, b, c, d, e);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class NV5 extends Narrowed implements Funcs.VoidFunc5 {
        @Override
        public void call(Object a, Object b, Object c, Object d, Object e) {
            fwd(a, b, c, d, e);
        }
    }
}
