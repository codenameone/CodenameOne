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
package com.codename1.flutter.scopedmodel;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * scoped_model's {@code ScopedModel<T extends Model>}: publishes {@code model}
 * to its subtree by runtime type and renders its {@code child}.
 * {@link #of(BuildContext, boolean, Class)} walks the tree for the nearest
 * matching model — the Dart {@code ScopedModel.of<T>(context)} witness is
 * threaded in as {@code type} by the transpiler.
 */
public class ScopedModel extends StatelessWidget implements InheritedValueProvider {

    private Object model;
    private Widget child;

    public void model(Object v) {
        this.model = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getModel() {
        return model;
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        return model != null && type.isInstance(model) ? model : null;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }

    /** {@code ScopedModel.of<T>(context, rebuildOnChange: ...)}. */
    @SuppressWarnings("unchecked")
    public static <T> T of(BuildContext context, boolean rebuildOnChange, Class<T> type) {
        return (T) context.providerValueOfType(type);
    }
}
