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

import dart.runtime.Funcs;

/**
 * A single form field wired to {@link Form} validation/save — Flutter's
 * {@code FormField<T>}. Application fields supply a {@code builder} that renders
 * the input from the current {@link FormFieldState}. This pass builds the field
 * from a fresh state; registration with the enclosing form lands with the form
 * renderer.
 *
 * @param <T> the field's value type
 */
public class FormField<T> extends StatelessWidget {

    private Funcs.Func1<FormFieldState<T>, Widget> builder;
    private FormFieldValidator<T> validator;
    private FormFieldSetter<T> onSaved;
    private T initialValue;
    private Boolean enabled;
    private Object autovalidateMode;
    private String restorationId;

    public void builder(Funcs.Func1<FormFieldState<T>, Widget> v) { this.builder = v; }
    public void validator(FormFieldValidator<T> v) { this.validator = v; }
    public void onSaved(FormFieldSetter<T> v) { this.onSaved = v; }
    public void initialValue(T v) { this.initialValue = v; }
    public void enabled(Boolean v) { this.enabled = v; }
    public void autovalidateMode(Object v) { this.autovalidateMode = v; }
    public void restorationId(String v) { this.restorationId = v; }

    @Override
    public Widget build(BuildContext context) {
        if (builder == null) {
            return null;
        }
        FormFieldState<T> state = new FormFieldState<T>();
        if (initialValue != null) {
            state.didChange(initialValue);
        }
        return builder.call(state);
    }
}
