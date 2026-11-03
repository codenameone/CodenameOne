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
}
