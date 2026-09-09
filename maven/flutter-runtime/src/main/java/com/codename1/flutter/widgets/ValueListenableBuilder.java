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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.foundation.ValueListenable;

/**
 * Rebuilds part of the tree whenever a {@code ValueListenable} changes —
 * Flutter's {@code ValueListenableBuilder<T>}. The {@code builder} is a
 * three-argument closure {@code (context, value, child)} that produces the
 * subtree, invoked with the listenable's current value and the optional
 * pass-through {@code child}.
 *
 * <p>{@link ValueListenableBuilderElement} does the listening; this widget is only the
 * configuration.</p>
 *
 * @param <T> the value type the listenable exposes
 */
public class ValueListenableBuilder<T> extends StatelessWidget {

    private Object valueListenable;
    private Object builder;
    private Widget child;

    public void valueListenable(Object v) {
        this.valueListenable = v;
    }

    public void builder(dart.runtime.Funcs.Func3<BuildContext, T, Widget, Widget> v) {
        this.builder = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getBuilder() {
        return builder;
    }

    public Object getValueListenable() {
        return valueListenable;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new ValueListenableBuilderElement(this);
    }

    @Override
    public Widget build(BuildContext context) {
        return buildWith(context);
    }

    /** Invokes the builder with the listenable's current value. */
    @SuppressWarnings("unchecked")
    Widget buildWith(BuildContext context) {
        if (builder instanceof dart.runtime.Funcs.Func3) {
            T value = valueListenable instanceof ValueListenable
                    ? ((ValueListenable<T>) valueListenable).value() : null;
            return ((dart.runtime.Funcs.Func3<BuildContext, T, Widget, Widget>) builder)
                    .call(context, value, child);
        }
        return child;
    }
}
