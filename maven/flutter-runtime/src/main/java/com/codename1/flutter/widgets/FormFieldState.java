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
import com.codename1.flutter.StatefulWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.AutovalidateMode;

/**
 * The state of a single form field -- Flutter's {@code FormFieldState<T>}, reached
 * through a {@code GlobalKey<FormFieldState<T>>().currentState}. It registers with
 * the enclosing {@link Form} when it builds, so the form's validate/save/reset reach
 * this field's validator, {@code onSaved} and initial value.
 *
 * @param <T> the field's value type
 */
public class FormFieldState<T> extends State<StatefulWidget> {

    private T value;
    private T initialValue;
    private String errorText;
    private boolean interacted;
    private FormFieldValidator<T> validator;
    private FormFieldSetter<T> onSaved;
    private Object autovalidateMode;
    private FormState form;

    /** The configuration a field widget hands its state on every build. */
    protected final void configure(FormFieldValidator<T> validator, FormFieldSetter<T> onSaved,
            Object autovalidateMode) {
        this.validator = validator;
        this.onSaved = onSaved;
        this.autovalidateMode = autovalidateMode;
    }

    /** Seeds the value and what {@link #reset} returns to; called once, from initState. */
    protected final void seed(T initial) {
        this.initialValue = initial;
        this.value = initial;
    }

    public T value() {
        return value;
    }

    /// What {@link #reset} returns to: the value seeded at initState.
    protected final T initialValue() {
        return initialValue;
    }

    public boolean hasError() {
        return errorText != null;
    }

    /** Whether the current value passes the validator, without showing an error. */
    public boolean isValid() {
        return validator == null || validator.call(value()) == null;
    }

    public String errorText() {
        return errorText;
    }

    public void didChange(final T v) {
        setState(new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                value = v;
                interacted = true;
            }
        });
        if (form != null) {
            form.fieldDidChange();
        }
    }

    public boolean validate() {
        setState(new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                runValidator();
            }
        });
        return !hasError();
    }

    private void runValidator() {
        errorText = validator == null ? null : validator.call(value());
    }

    public void save() {
        if (onSaved != null) {
            onSaved.call(value());
        }
    }

    public void reset() {
        setState(new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                value = initialValue;
                interacted = false;
                errorText = null;
            }
        });
        if (form != null) {
            form.fieldDidChange();
        }
    }

    /**
     * Registers with the nearest {@link Form} (Flutter does it in build too, so a
     * field moved under another form follows it) and applies autovalidation: the
     * field's own mode, else the form's.
     */
    protected final void buildField(BuildContext context) {
        FormState f = Form.maybeOf(context);
        if (f != form) {
            if (form != null) {
                form.unregister(this);
            }
            form = f;
            if (f != null) {
                f.register(this);
            }
        }
        Object mode = autovalidateMode != null ? autovalidateMode : form == null ? null : form.autovalidateMode();
        if (mode == AutovalidateMode.always || (mode == AutovalidateMode.onUserInteraction && interacted)) {
            runValidator();
        }
    }

    @Override
    public void dispose() {
        if (form != null) {
            form.unregister(this);
            form = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget build(BuildContext context) {
        StatefulWidget w = widget();
        if (!(w instanceof FormField)) {
            return null;
        }
        FormField<T> field = (FormField<T>) w;
        configure(field.validatorValue(), field.onSavedValue(), field.autovalidateModeValue());
        buildField(context);
        return field.builderValue() == null ? null : field.builderValue().call(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initState() {
        StatefulWidget w = widget();
        if (w instanceof FormField) {
            seed(((FormField<T>) w).initialValueValue());
        }
    }
}
