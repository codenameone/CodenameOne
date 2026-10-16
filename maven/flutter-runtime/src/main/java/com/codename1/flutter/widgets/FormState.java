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
import com.codename1.flutter.State;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * The state of a {@link Form} -- Flutter's {@code FormState}, reached through
 * {@link Form#of} or a {@code GlobalKey<FormState>().currentState}. Every
 * {@link FormFieldState} built below the form registers here, so validate, save
 * and reset reach each field's validator, {@code onSaved} and initial value.
 */
public class FormState extends State<Form> {

    private final List<FormFieldState<?>> fields = new ArrayList<FormFieldState<?>>();

    void register(FormFieldState<?> field) {
        if (!fields.contains(field)) {
            fields.add(field);
        }
    }

    void unregister(FormFieldState<?> field) {
        fields.remove(field);
    }

    /** A field's value changed: the form's {@code onChanged}, as Flutter's _fieldDidChange. */
    void fieldDidChange() {
        Form w = widget();
        if (w != null && w.onChangedCallback() instanceof Funcs.VoidFunc0) {
            ((Funcs.VoidFunc0) w.onChangedCallback()).call();
        }
    }

    /** The form's own autovalidate mode, which a field without one of its own follows. */
    Object autovalidateMode() {
        Form w = widget();
        return w == null ? null : w.autovalidateModeValue();
    }

    /**
     * Validates every field and answers whether all are valid. Every field runs, not
     * just up to the first failure, so each one shows its own error, as in Flutter.
     */
    public boolean validate() {
        boolean valid = true;
        for (FormFieldState<?> f : new ArrayList<FormFieldState<?>>(fields)) {
            valid &= f.validate();
        }
        return valid;
    }

    /** Saves every field (invokes their {@code onSaved}). */
    public void save() {
        for (FormFieldState<?> f : new ArrayList<FormFieldState<?>>(fields)) {
            f.save();
        }
    }

    /** Resets every field to its initial value. */
    public void reset() {
        for (FormFieldState<?> f : new ArrayList<FormFieldState<?>>(fields)) {
            f.reset();
        }
        fieldDidChange();
    }

    @Override
    public Widget build(BuildContext context) {
        Form w = widget();
        return w == null ? null : w.getChild();
    }
}
