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
package com.codename1.flutter.provider;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * provider's {@code Selector<A, S>}: rebuilds only when a selected slice
 * {@code S} of a provided value {@code A} changes. {@code selector(context, a)}
 * extracts the slice and {@code builder(context, s, child)} renders it.
 *
 * <p>The Dart type argument {@code A} is threaded in by the transpiler as a type token
 * ({@link #providedType}), so the right model is found even when several are in scope.
 * Without it the lookup took the NEAREST provided value of any type: Reply has both its
 * localizations and its EmailStore above the Selector, got the localizations, and failed —
 * with a ClassCastException on the desktop and, because the cast is unchecked there, a
 * wrong object that flowed on until an unrelated switch matched nothing on iOS.</p>
 *
 * @param <A> the provided value type
 * @param <S> the selected slice type
 */
public class Selector<A, S> extends StatelessWidget {

    private Funcs.Func2<BuildContext, A, S> selector;
    private Funcs.Func3<BuildContext, S, Widget, Widget> builder;
    private Object shouldRebuild;
    private Widget child;
    private Class<?> providedType = Object.class;

    /**
     * The model type this selector reads — the Dart {@code A}, emitted by the transpiler.
     * Defaults to {@code Object}, which resolves to the nearest provider of any type and is
     * only correct when a single value is in scope.
     */
    public void providedType(Class<?> v) {
        this.providedType = v == null ? Object.class : v;
    }

    public void selector(Funcs.Func2<BuildContext, A, S> v) {
        this.selector = v;
    }

    public void builder(Funcs.Func3<BuildContext, S, Widget, Widget> v) {
        this.builder = v;
    }

    public void shouldRebuild(Object v) {
        this.shouldRebuild = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    /** The model this selector reads, from the provider above {@code context}. */
    @SuppressWarnings("unchecked")
    S select(BuildContext context) {
        A value = (A) context.providerValueOfType(providedType);
        return selector == null ? (S) value : selector.call(context, value);
    }

    /** Whether {@code next} differs from {@code previous} enough to rebuild. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    boolean changed(S previous, S next) {
        if (shouldRebuild instanceof Funcs.Func2) {
            return Boolean.TRUE.equals(((Funcs.Func2) shouldRebuild).call(previous, next));
        }
        return !deepEquals(previous, next);
    }

    Widget buildFor(BuildContext context, S selected) {
        return builder == null ? child : builder.call(context, selected, child);
    }

    /** Uncached: the element ({@link SelectorElement}) decides when to call this. */
    @Override
    public Widget build(BuildContext context) {
        return buildFor(context, select(context));
    }

    /**
     * An element that remembers ITS last selection. The cache used to live on the
     * widget, which one instance mounted in two places shares: the second place got the
     * first one's subtree, and each overwrote the other's selection.
     */
    @Override
    public com.codename1.flutter.Element createElement() {
        return new SelectorElement<A, S>(this);
    }

    /** provider's default test: DeepCollectionEquality, over lists, sets and maps. */
    @SuppressWarnings("rawtypes")
    static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a instanceof java.util.List && b instanceof java.util.List) {
            java.util.List la = (java.util.List) a;
            java.util.List lb = (java.util.List) b;
            if (la.size() != lb.size()) {
                return false;
            }
            for (int i = 0; i < la.size(); i++) {
                if (!deepEquals(la.get(i), lb.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof java.util.Map && b instanceof java.util.Map) {
            java.util.Map ma = (java.util.Map) a;
            java.util.Map mb = (java.util.Map) b;
            if (ma.size() != mb.size()) {
                return false;
            }
            for (Object k : ma.keySet()) {
                if (!mb.containsKey(k) || !deepEquals(ma.get(k), mb.get(k))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof java.util.Set && b instanceof java.util.Set) {
            java.util.Set sa = (java.util.Set) a;
            java.util.Set sb = (java.util.Set) b;
            return sa.size() == sb.size() && sa.containsAll(sb);
        }
        return dart.runtime.DartRuntime.eq(a, b);
    }
}
