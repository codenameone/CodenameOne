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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.GlobalKey;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Form;
import com.codename1.flutter.widgets.FormFieldState;
import com.codename1.flutter.widgets.FormFieldSetter;
import com.codename1.flutter.widgets.FormFieldValidator;
import com.codename1.flutter.widgets.FormState;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Form was a pass-through: {@code Form.of} and {@code GlobalKey<FormState>.currentState}
 * both answered null, so {@code formKey.currentState!.validate()} failed and no
 * TextFormField validator or onSaved ever ran.
 */
class FormValidationTest {

    private static final FormFieldValidator<String> REQUIRED = new FormFieldValidator<String>() {
        @Override
        public String call(String v) {
            return v == null || v.isEmpty() ? "Required" : null;
        }
    };

    private final List<String> saved = new ArrayList<String>();
    private final BuildOwner owner = new BuildOwner();

    private TextFormField field(String initial, GlobalKey<FormFieldState<String>> key) {
        TextFormField f = new TextFormField();
        f.initialValue(initial);
        f.validator(REQUIRED);
        f.onSaved(new FormFieldSetter<String>() {
            @Override
            public void call(String v) {
                saved.add(v);
            }
        });
        if (key != null) {
            f.key(key);
        }
        return f;
    }

    private Element mountForm(GlobalKey<FormState> formKey, Widget... fields) {
        Column col = new Column();
        col.children(DartList.of(fields));
        Form form = new Form();
        form.key(formKey);
        form.child(col);
        return FlutterUI.mount(form, new RenderHost(), owner);
    }

    @Test
    void theFormKeyReachesALiveFormStateThatRunsEveryValidator() {
        GlobalKey<FormState> formKey = new GlobalKey<FormState>();
        GlobalKey<FormFieldState<String>> emptyKey = new GlobalKey<FormFieldState<String>>();
        mountForm(formKey, field("ok", null), field("", emptyKey));

        FormState form = formKey.currentState();
        assertNotNull(form, "GlobalKey<FormState>.currentState must be the Form's state");
        assertFalse(form.validate(), "the empty field fails its validator");
        assertEquals("Required", emptyKey.currentState().errorText());

        owner.flushSync();
        TextField rendered = (TextField) RenderElement.findRenderElement(
                (Element) emptyKey.currentContext()).widget();
        assertEquals("Required", rendered.getDecoration().getErrorText(),
                "the error reaches the decoration Material draws it in");
    }

    @Test
    void saveReachesEveryOnSavedAndResetRestoresTheInitialText() {
        GlobalKey<FormState> formKey = new GlobalKey<FormState>();
        GlobalKey<FormFieldState<String>> k = new GlobalKey<FormFieldState<String>>();
        mountForm(formKey, field("a", k), field("b", null));

        assertTrue(formKey.currentState().validate());
        formKey.currentState().save();
        assertEquals(java.util.Arrays.asList("a", "b"), saved);

        TextField rendered = (TextField) RenderElement.findRenderElement(
                (Element) k.currentContext()).widget();
        rendered.getController().setText("changed");
        assertEquals("changed", k.currentState().value(), "the value is the controller's text");
        formKey.currentState().reset();
        assertEquals("a", k.currentState().value());
        assertNull(k.currentState().errorText());
    }

    /// Flutter seeds a controller-backed field's initial value from controller.text, so
    /// reset restores it; it used to blank the field.
    @Test
    void resetRestoresAnExternalControllersTextAtCreation() {
        GlobalKey<FormState> formKey = new GlobalKey<FormState>();
        TextEditingController ctl = new TextEditingController();
        ctl.setText("start");
        TextFormField f = new TextFormField();
        f.controller(ctl);
        mountForm(formKey, f);

        ctl.setText("edited");
        formKey.currentState().reset();
        assertEquals("start", ctl.text());
    }

    /// inputFormatters were stored and never applied: the gallery's phone-number
    /// formatter never ran and the raw edit reached the controller and onChanged.
    @Test
    void inputFormattersReshapeAUserEdit() {
        GlobalKey<FormFieldState<String>> k = new GlobalKey<FormFieldState<String>>();
        final List<String> changed = new ArrayList<String>();
        TextFormField f = field("", k);
        f.onChanged(changed::add);
        f.inputFormatters(DartList.of((Object) new com.codename1.flutter.services.TextInputFormatter() {
            @Override
            public com.codename1.flutter.TextEditingValue formatEditUpdate(
                    com.codename1.flutter.TextEditingValue oldValue, com.codename1.flutter.TextEditingValue newValue) {
                return newValue.copyWith(newValue.text().toUpperCase(java.util.Locale.ROOT), null, null);
            }
        }));
        mountForm(new GlobalKey<FormState>(), f);

        TextFieldRenderElement field = (TextFieldRenderElement) RenderElement.findRenderElement(
                (Element) k.currentContext());
        field.userEdited("abc");

        assertEquals("ABC", ((TextField) field.widget()).getController().text());
        assertEquals(java.util.Arrays.asList("ABC"), changed);
    }

    @Test
    void formOfFindsTheEnclosingState() {
        GlobalKey<FormState> formKey = new GlobalKey<FormState>();
        GlobalKey<FormFieldState<String>> k = new GlobalKey<FormFieldState<String>>();
        mountForm(formKey, field("x", k));
        assertSame(formKey.currentState(), Form.of(k.currentContext()));
    }
}
