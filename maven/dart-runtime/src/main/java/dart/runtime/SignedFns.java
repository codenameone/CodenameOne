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
}
