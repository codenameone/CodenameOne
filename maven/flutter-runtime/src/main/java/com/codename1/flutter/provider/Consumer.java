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
 * provider's {@code Consumer<T>}: rebuilds via {@code builder(context, value,
 * child)} with the nearest ancestor-provided value.
 *
 * <p>The Dart type argument {@code T} is threaded in by the transpiler as a type token
 * ({@link #providedType}), so the right model is found with several in scope. Without it
 * the lookup took the NEAREST provided value of any type, which is only ever correct by
 * luck — see {@link Selector} for what that cost.</p>
 */
public class Consumer<T> extends StatelessWidget {

    private Funcs.Func3<BuildContext, T, Widget, Widget> builder;
    private Widget child;
    private Class<?> providedType = Object.class;

    /**
     * The model type this consumer reads — the Dart {@code T}, emitted by the transpiler.
     * Defaults to {@code Object}: the nearest provider of any type.
     */
    public void providedType(Class<?> v) {
        this.providedType = v == null ? Object.class : v;
    }

    public void builder(Funcs.Func3<BuildContext, T, Widget, Widget> v) {
        this.builder = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget build(BuildContext context) {
        T value = (T) context.providerValueOfType(providedType);
        return builder == null ? child : builder.call(context, value, child);
    }
}
