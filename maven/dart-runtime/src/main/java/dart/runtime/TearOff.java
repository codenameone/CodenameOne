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

/// The value of an instance-method tear-off: it implements the function type it is held as
/// (a subclass, or a generated class, implements the SAM) and BoundTearOff, and is equal to
/// another tear-off of the same method from the same receiver (by identity). It keeps its
/// receiver as the method reference inside it already does, and nothing else: no side
/// table, so a receiver reachable only through its own tear-off is collected with it.
public abstract class TearOff implements BoundTearOff {
    private final Object recv;
    private final String key;

    protected TearOff(Object recv, String key) {
        this.recv = recv;
        this.key = key;
    }

    @Override
    public final Object receiver() {
        return recv;
    }

    @Override
    public final String key() {
        return key;
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BoundTearOff)) {
            return false;
        }
        BoundTearOff b = (BoundTearOff) o;
        return b.receiver() == recv && key.equals(b.key());
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(recv) * 31 + key.hashCode();
    }


    private static final class V0 extends TearOff implements Funcs.VoidFunc0 {
        private final Funcs.VoidFunc0 f;

        V0(Object recv, String key, Funcs.VoidFunc0 f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call() {
            f.call();
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static Funcs.VoidFunc0 v0(Object recv, String key, Funcs.VoidFunc0 f) {
        return new V0(recv, key, f);
    }

    private static final class V1<T0> extends TearOff implements Funcs.VoidFunc1<T0> {
        private final Funcs.VoidFunc1<T0> f;

        V1(Object recv, String key, Funcs.VoidFunc1<T0> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0) {
            f.call(a0);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0> Funcs.VoidFunc1<T0> v1(Object recv, String key, Funcs.VoidFunc1<T0> f) {
        return new V1<T0>(recv, key, f);
    }

    private static final class V2<T0, T1> extends TearOff implements Funcs.VoidFunc2<T0, T1> {
        private final Funcs.VoidFunc2<T0, T1> f;

        V2(Object recv, String key, Funcs.VoidFunc2<T0, T1> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1) {
            f.call(a0, a1);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1> Funcs.VoidFunc2<T0, T1> v2(Object recv, String key, Funcs.VoidFunc2<T0, T1> f) {
        return new V2<T0, T1>(recv, key, f);
    }

    private static final class V3<T0, T1, T2> extends TearOff implements Funcs.VoidFunc3<T0, T1, T2> {
        private final Funcs.VoidFunc3<T0, T1, T2> f;

        V3(Object recv, String key, Funcs.VoidFunc3<T0, T1, T2> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2) {
            f.call(a0, a1, a2);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2> Funcs.VoidFunc3<T0, T1, T2> v3(Object recv, String key, Funcs.VoidFunc3<T0, T1, T2> f) {
        return new V3<T0, T1, T2>(recv, key, f);
    }

    private static final class V4<T0, T1, T2, T3> extends TearOff implements Funcs.VoidFunc4<T0, T1, T2, T3> {
        private final Funcs.VoidFunc4<T0, T1, T2, T3> f;

        V4(Object recv, String key, Funcs.VoidFunc4<T0, T1, T2, T3> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3) {
            f.call(a0, a1, a2, a3);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3> Funcs.VoidFunc4<T0, T1, T2, T3> v4(Object recv, String key, Funcs.VoidFunc4<T0, T1, T2, T3> f) {
        return new V4<T0, T1, T2, T3>(recv, key, f);
    }

    private static final class V5<T0, T1, T2, T3, T4> extends TearOff implements Funcs.VoidFunc5<T0, T1, T2, T3, T4> {
        private final Funcs.VoidFunc5<T0, T1, T2, T3, T4> f;

        V5(Object recv, String key, Funcs.VoidFunc5<T0, T1, T2, T3, T4> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4) {
            f.call(a0, a1, a2, a3, a4);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4> Funcs.VoidFunc5<T0, T1, T2, T3, T4> v5(Object recv, String key, Funcs.VoidFunc5<T0, T1, T2, T3, T4> f) {
        return new V5<T0, T1, T2, T3, T4>(recv, key, f);
    }

    private static final class F0<T0> extends TearOff implements Funcs.Func0<T0> {
        private final Funcs.Func0<T0> f;

        F0(Object recv, String key, Funcs.Func0<T0> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T0 call() {
            return f.call();
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0> Funcs.Func0<T0> f0(Object recv, String key, Funcs.Func0<T0> f) {
        return new F0<T0>(recv, key, f);
    }

    private static final class F1<T0, T1> extends TearOff implements Funcs.Func1<T0, T1> {
        private final Funcs.Func1<T0, T1> f;

        F1(Object recv, String key, Funcs.Func1<T0, T1> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T1 call(T0 a0) {
            return f.call(a0);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1> Funcs.Func1<T0, T1> f1(Object recv, String key, Funcs.Func1<T0, T1> f) {
        return new F1<T0, T1>(recv, key, f);
    }

    private static final class F2<T0, T1, T2> extends TearOff implements Funcs.Func2<T0, T1, T2> {
        private final Funcs.Func2<T0, T1, T2> f;

        F2(Object recv, String key, Funcs.Func2<T0, T1, T2> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T2 call(T0 a0, T1 a1) {
            return f.call(a0, a1);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2> Funcs.Func2<T0, T1, T2> f2(Object recv, String key, Funcs.Func2<T0, T1, T2> f) {
        return new F2<T0, T1, T2>(recv, key, f);
    }

    private static final class F3<T0, T1, T2, T3> extends TearOff implements Funcs.Func3<T0, T1, T2, T3> {
        private final Funcs.Func3<T0, T1, T2, T3> f;

        F3(Object recv, String key, Funcs.Func3<T0, T1, T2, T3> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T3 call(T0 a0, T1 a1, T2 a2) {
            return f.call(a0, a1, a2);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3> Funcs.Func3<T0, T1, T2, T3> f3(Object recv, String key, Funcs.Func3<T0, T1, T2, T3> f) {
        return new F3<T0, T1, T2, T3>(recv, key, f);
    }

    private static final class F4<T0, T1, T2, T3, T4> extends TearOff implements Funcs.Func4<T0, T1, T2, T3, T4> {
        private final Funcs.Func4<T0, T1, T2, T3, T4> f;

        F4(Object recv, String key, Funcs.Func4<T0, T1, T2, T3, T4> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T4 call(T0 a0, T1 a1, T2 a2, T3 a3) {
            return f.call(a0, a1, a2, a3);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4> Funcs.Func4<T0, T1, T2, T3, T4> f4(Object recv, String key, Funcs.Func4<T0, T1, T2, T3, T4> f) {
        return new F4<T0, T1, T2, T3, T4>(recv, key, f);
    }

    private static final class F5<T0, T1, T2, T3, T4, T5> extends TearOff implements Funcs.Func5<T0, T1, T2, T3, T4, T5> {
        private final Funcs.Func5<T0, T1, T2, T3, T4, T5> f;

        F5(Object recv, String key, Funcs.Func5<T0, T1, T2, T3, T4, T5> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T5 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4) {
            return f.call(a0, a1, a2, a3, a4);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5> Funcs.Func5<T0, T1, T2, T3, T4, T5> f5(Object recv, String key, Funcs.Func5<T0, T1, T2, T3, T4, T5> f) {
        return new F5<T0, T1, T2, T3, T4, T5>(recv, key, f);
    }

    /// {@code body} applied to {@code recv}: a tear-off whose receiver is an expression
    /// (`foo().m`) evaluates it once and builds its value from the result.
    public static <R, F> F let(R recv, Funcs.Func1<? super R, F> body) {
        return body.call(recv);
    }

    private static final class F6<T0, T1, T2, T3, T4, T5, T6> extends TearOff implements Funcs.Func6<T0, T1, T2, T3, T4, T5, T6> {
        private final Funcs.Func6<T0, T1, T2, T3, T4, T5, T6> f;

        F6(Object recv, String key, Funcs.Func6<T0, T1, T2, T3, T4, T5, T6> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T6 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5) {
            return f.call(a0, a1, a2, a3, a4, a5);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6> Funcs.Func6<T0, T1, T2, T3, T4, T5, T6> f6(Object recv, String key, Funcs.Func6<T0, T1, T2, T3, T4, T5, T6> f) {
        return new F6<T0, T1, T2, T3, T4, T5, T6>(recv, key, f);
    }

    private static final class V6<T0, T1, T2, T3, T4, T5> extends TearOff implements Funcs.VoidFunc6<T0, T1, T2, T3, T4, T5> {
        private final Funcs.VoidFunc6<T0, T1, T2, T3, T4, T5> f;

        V6(Object recv, String key, Funcs.VoidFunc6<T0, T1, T2, T3, T4, T5> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5) {
            f.call(a0, a1, a2, a3, a4, a5);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5> Funcs.VoidFunc6<T0, T1, T2, T3, T4, T5> v6(Object recv, String key, Funcs.VoidFunc6<T0, T1, T2, T3, T4, T5> f) {
        return new V6<T0, T1, T2, T3, T4, T5>(recv, key, f);
    }

    private static final class F7<T0, T1, T2, T3, T4, T5, T6, T7> extends TearOff implements Funcs.Func7<T0, T1, T2, T3, T4, T5, T6, T7> {
        private final Funcs.Func7<T0, T1, T2, T3, T4, T5, T6, T7> f;

        F7(Object recv, String key, Funcs.Func7<T0, T1, T2, T3, T4, T5, T6, T7> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T7 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6) {
            return f.call(a0, a1, a2, a3, a4, a5, a6);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7> Funcs.Func7<T0, T1, T2, T3, T4, T5, T6, T7> f7(Object recv, String key, Funcs.Func7<T0, T1, T2, T3, T4, T5, T6, T7> f) {
        return new F7<T0, T1, T2, T3, T4, T5, T6, T7>(recv, key, f);
    }

    private static final class V7<T0, T1, T2, T3, T4, T5, T6> extends TearOff implements Funcs.VoidFunc7<T0, T1, T2, T3, T4, T5, T6> {
        private final Funcs.VoidFunc7<T0, T1, T2, T3, T4, T5, T6> f;

        V7(Object recv, String key, Funcs.VoidFunc7<T0, T1, T2, T3, T4, T5, T6> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6) {
            f.call(a0, a1, a2, a3, a4, a5, a6);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6> Funcs.VoidFunc7<T0, T1, T2, T3, T4, T5, T6> v7(Object recv, String key, Funcs.VoidFunc7<T0, T1, T2, T3, T4, T5, T6> f) {
        return new V7<T0, T1, T2, T3, T4, T5, T6>(recv, key, f);
    }

    private static final class F8<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends TearOff implements Funcs.Func8<T0, T1, T2, T3, T4, T5, T6, T7, T8> {
        private final Funcs.Func8<T0, T1, T2, T3, T4, T5, T6, T7, T8> f;

        F8(Object recv, String key, Funcs.Func8<T0, T1, T2, T3, T4, T5, T6, T7, T8> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T8 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> Funcs.Func8<T0, T1, T2, T3, T4, T5, T6, T7, T8> f8(Object recv, String key, Funcs.Func8<T0, T1, T2, T3, T4, T5, T6, T7, T8> f) {
        return new F8<T0, T1, T2, T3, T4, T5, T6, T7, T8>(recv, key, f);
    }

    private static final class V8<T0, T1, T2, T3, T4, T5, T6, T7> extends TearOff implements Funcs.VoidFunc8<T0, T1, T2, T3, T4, T5, T6, T7> {
        private final Funcs.VoidFunc8<T0, T1, T2, T3, T4, T5, T6, T7> f;

        V8(Object recv, String key, Funcs.VoidFunc8<T0, T1, T2, T3, T4, T5, T6, T7> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7> Funcs.VoidFunc8<T0, T1, T2, T3, T4, T5, T6, T7> v8(Object recv, String key, Funcs.VoidFunc8<T0, T1, T2, T3, T4, T5, T6, T7> f) {
        return new V8<T0, T1, T2, T3, T4, T5, T6, T7>(recv, key, f);
    }

    private static final class F9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> extends TearOff implements Funcs.Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        private final Funcs.Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> f;

        F9(Object recv, String key, Funcs.Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T9 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> Funcs.Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> f9(Object recv, String key, Funcs.Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> f) {
        return new F9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>(recv, key, f);
    }

    private static final class V9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends TearOff implements Funcs.VoidFunc9<T0, T1, T2, T3, T4, T5, T6, T7, T8> {
        private final Funcs.VoidFunc9<T0, T1, T2, T3, T4, T5, T6, T7, T8> f;

        V9(Object recv, String key, Funcs.VoidFunc9<T0, T1, T2, T3, T4, T5, T6, T7, T8> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> Funcs.VoidFunc9<T0, T1, T2, T3, T4, T5, T6, T7, T8> v9(Object recv, String key, Funcs.VoidFunc9<T0, T1, T2, T3, T4, T5, T6, T7, T8> f) {
        return new V9<T0, T1, T2, T3, T4, T5, T6, T7, T8>(recv, key, f);
    }

    private static final class F10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> extends TearOff implements Funcs.Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> {
        private final Funcs.Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> f;

        F10(Object recv, String key, Funcs.Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T10 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Funcs.Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> f10(Object recv, String key, Funcs.Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> f) {
        return new F10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(recv, key, f);
    }

    private static final class V10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> extends TearOff implements Funcs.VoidFunc10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        private final Funcs.VoidFunc10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> f;

        V10(Object recv, String key, Funcs.VoidFunc10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> Funcs.VoidFunc10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> v10(Object recv, String key, Funcs.VoidFunc10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> f) {
        return new V10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>(recv, key, f);
    }

    private static final class F11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> extends TearOff implements Funcs.Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> {
        private final Funcs.Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> f;

        F11(Object recv, String key, Funcs.Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T11 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Funcs.Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> f11(Object recv, String key, Funcs.Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> f) {
        return new F11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(recv, key, f);
    }

    private static final class V11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> extends TearOff implements Funcs.VoidFunc11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> {
        private final Funcs.VoidFunc11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> f;

        V11(Object recv, String key, Funcs.VoidFunc11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Funcs.VoidFunc11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> v11(Object recv, String key, Funcs.VoidFunc11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> f) {
        return new V11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(recv, key, f);
    }

    private static final class F12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> extends TearOff implements Funcs.Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> {
        private final Funcs.Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> f;

        F12(Object recv, String key, Funcs.Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T12 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Funcs.Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> f12(Object recv, String key, Funcs.Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> f) {
        return new F12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(recv, key, f);
    }

    private static final class V12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> extends TearOff implements Funcs.VoidFunc12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> {
        private final Funcs.VoidFunc12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> f;

        V12(Object recv, String key, Funcs.VoidFunc12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> Funcs.VoidFunc12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> v12(Object recv, String key, Funcs.VoidFunc12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> f) {
        return new V12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(recv, key, f);
    }

    private static final class F13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> extends TearOff implements Funcs.Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> {
        private final Funcs.Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> f;

        F13(Object recv, String key, Funcs.Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T13 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Funcs.Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> f13(Object recv, String key, Funcs.Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> f) {
        return new F13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(recv, key, f);
    }

    private static final class V13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> extends TearOff implements Funcs.VoidFunc13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> {
        private final Funcs.VoidFunc13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> f;

        V13(Object recv, String key, Funcs.VoidFunc13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> Funcs.VoidFunc13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> v13(Object recv, String key, Funcs.VoidFunc13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> f) {
        return new V13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(recv, key, f);
    }

    private static final class F14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> extends TearOff implements Funcs.Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> {
        private final Funcs.Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> f;

        F14(Object recv, String key, Funcs.Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T14 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Funcs.Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> f14(Object recv, String key, Funcs.Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> f) {
        return new F14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(recv, key, f);
    }

    private static final class V14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> extends TearOff implements Funcs.VoidFunc14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> {
        private final Funcs.VoidFunc14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> f;

        V14(Object recv, String key, Funcs.VoidFunc14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> Funcs.VoidFunc14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> v14(Object recv, String key, Funcs.VoidFunc14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> f) {
        return new V14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(recv, key, f);
    }

    private static final class F15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> extends TearOff implements Funcs.Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> {
        private final Funcs.Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> f;

        F15(Object recv, String key, Funcs.Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T15 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Funcs.Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> f15(Object recv, String key, Funcs.Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> f) {
        return new F15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(recv, key, f);
    }

    private static final class V15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> extends TearOff implements Funcs.VoidFunc15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> {
        private final Funcs.VoidFunc15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> f;

        V15(Object recv, String key, Funcs.VoidFunc15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> Funcs.VoidFunc15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> v15(Object recv, String key, Funcs.VoidFunc15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> f) {
        return new V15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(recv, key, f);
    }

    private static final class F16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> extends TearOff implements Funcs.Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> {
        private final Funcs.Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> f;

        F16(Object recv, String key, Funcs.Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public T16 call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14, T15 a15) {
            return f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> Funcs.Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> f16(Object recv, String key, Funcs.Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> f) {
        return new F16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(recv, key, f);
    }

    private static final class V16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> extends TearOff implements Funcs.VoidFunc16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> {
        private final Funcs.VoidFunc16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> f;

        V16(Object recv, String key, Funcs.VoidFunc16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> f) {
            super(recv, key);
            this.f = f;
        }

        @Override
        public void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14, T15 a15) {
            f.call(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
        }
    }

    /// {@code f}, a tear-off of method {@code key} of {@code recv}, as a value equal to every
    /// other tear-off of that method from that receiver.
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> Funcs.VoidFunc16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> v16(Object recv, String key, Funcs.VoidFunc16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> f) {
        return new V16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(recv, key, f);
    }
}
