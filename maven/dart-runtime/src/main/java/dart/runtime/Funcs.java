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

/**
 * Canonical functional interfaces for transpiled Dart closures.
 *
 * <p>Dart function types are normalized by the transpiler to a canonical
 * positional shape and bound to one of these interfaces. All are single
 * abstract method interfaces so transpiled closures emit as Java lambdas
 * or method references.</p>
 */
public final class Funcs {
    private Funcs() {
    }

    @FunctionalInterface
    public interface Func0<R> {
        R call();
    }

    @FunctionalInterface
    public interface Func1<A, R> {
        R call(A a);
    }

    @FunctionalInterface
    public interface Func2<A, B, R> {
        R call(A a, B b);
    }

    @FunctionalInterface
    public interface Func3<A, B, C, R> {
        R call(A a, B b, C c);
    }

    @FunctionalInterface
    public interface Func4<A, B, C, D, R> {
        R call(A a, B b, C c, D d);
    }

    @FunctionalInterface
    public interface Func5<A, B, C, D, E, R> {
        R call(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    public interface VoidFunc0 {
        void call();
    }

    @FunctionalInterface
    public interface VoidFunc1<A> {
        void call(A a);
    }

    @FunctionalInterface
    public interface VoidFunc2<A, B> {
        void call(A a, B b);
    }

    @FunctionalInterface
    public interface VoidFunc3<A, B, C> {
        void call(A a, B b, C c);
    }

    @FunctionalInterface
    public interface VoidFunc4<A, B, C, D> {
        void call(A a, B b, C c, D d);
    }

    @FunctionalInterface
    public interface VoidFunc5<A, B, C, D, E> {
        void call(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    public interface Func6<T0, T1, T2, T3, T4, T5, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5);
    }

    @FunctionalInterface
    public interface Func7<T0, T1, T2, T3, T4, T5, T6, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6);
    }

    @FunctionalInterface
    public interface Func8<T0, T1, T2, T3, T4, T5, T6, T7, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7);
    }

    @FunctionalInterface
    public interface Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8);
    }

    @FunctionalInterface
    public interface Func10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9);
    }

    @FunctionalInterface
    public interface Func11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10);
    }

    @FunctionalInterface
    public interface Func12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11);
    }

    @FunctionalInterface
    public interface Func13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12);
    }

    @FunctionalInterface
    public interface Func14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13);
    }

    @FunctionalInterface
    public interface Func15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14);
    }

    @FunctionalInterface
    public interface Func16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> {
        R call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14, T15 a15);
    }

    @FunctionalInterface
    public interface VoidFunc6<T0, T1, T2, T3, T4, T5> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5);
    }

    @FunctionalInterface
    public interface VoidFunc7<T0, T1, T2, T3, T4, T5, T6> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6);
    }

    @FunctionalInterface
    public interface VoidFunc8<T0, T1, T2, T3, T4, T5, T6, T7> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7);
    }

    @FunctionalInterface
    public interface VoidFunc9<T0, T1, T2, T3, T4, T5, T6, T7, T8> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8);
    }

    @FunctionalInterface
    public interface VoidFunc10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9);
    }

    @FunctionalInterface
    public interface VoidFunc11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10);
    }

    @FunctionalInterface
    public interface VoidFunc12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11);
    }

    @FunctionalInterface
    public interface VoidFunc13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12);
    }

    @FunctionalInterface
    public interface VoidFunc14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13);
    }

    @FunctionalInterface
    public interface VoidFunc15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14);
    }

    @FunctionalInterface
    public interface VoidFunc16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> {
        void call(T0 a0, T1 a1, T2 a2, T3 a3, T4 a4, T5 a5, T6 a6, T7 a7, T8 a8, T9 a9, T10 a10, T11 a11, T12 a12, T13 a13, T14 a14, T15 a15);
    }
}
