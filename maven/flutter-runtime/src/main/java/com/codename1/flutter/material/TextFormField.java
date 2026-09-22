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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.FormFieldSetter;
import com.codename1.flutter.widgets.FormFieldValidator;

import dart.runtime.Funcs;

/**
 * A Material {@link TextField} wired to {@link com.codename1.flutter.widgets.Form}
 * validation — Flutter's {@code TextFormField}. It renders as a {@code TextField}
 * (this pass forwards the controller/decoration/obscure/enabled it can map);
 * {@code validator}/{@code onSaved} are captured so a later pass can register the
 * field with its enclosing form. All values are String-valued in new_gallery.
 */
public class TextFormField extends StatelessWidget {

    private TextEditingController controller;
    private String initialValue;
    private InputDecoration decoration;
    private Object keyboardType;
    private Object style;
    private boolean obscureText;
    private Boolean enabled;
    private Object maxLines;
    private Object minLines;
    private Object maxLength;
    private FormFieldValidator<String> validator;
    private FormFieldSetter<String> onSaved;
    private Funcs.VoidFunc1<String> onChanged;
    private Funcs.VoidFunc1<String> onFieldSubmitted;
    private Funcs.VoidFunc0 onEditingComplete;
    private Object focusNode;
    private Object textInputAction;
    private Object textCapitalization;
    private Object autovalidateMode;
    private Object inputFormatters;
    private Object autofillHints;
    private Object autofocus;
    private Object cursorColor;
    private String restorationId;

    public void controller(TextEditingController v) { this.controller = v; }
    public void initialValue(String v) { this.initialValue = v; }
    public void decoration(InputDecoration v) { this.decoration = v; }
    public void keyboardType(Object v) { this.keyboardType = v; }
    public void style(Object v) { this.style = v; }
    public void obscureText(boolean v) { this.obscureText = v; }
    public void enabled(Boolean v) { this.enabled = v; }
    public void maxLines(Object v) { this.maxLines = v; }
    public void minLines(Object v) { this.minLines = v; }
    public void maxLength(Object v) { this.maxLength = v; }
    public void validator(FormFieldValidator<String> v) { this.validator = v; }
    public void onSaved(FormFieldSetter<String> v) { this.onSaved = v; }
    public void onChanged(Funcs.VoidFunc1<String> v) { this.onChanged = v; }
    public void onFieldSubmitted(Funcs.VoidFunc1<String> v) { this.onFieldSubmitted = v; }
    public void onEditingComplete(Funcs.VoidFunc0 v) { this.onEditingComplete = v; }
    public void focusNode(Object v) { this.focusNode = v; }
    public void textInputAction(Object v) { this.textInputAction = v; }
    public void textCapitalization(Object v) { this.textCapitalization = v; }
    public void autovalidateMode(Object v) { this.autovalidateMode = v; }
    public void inputFormatters(Object v) { this.inputFormatters = v; }
    public void autofillHints(Object v) { this.autofillHints = v; }
    public void autofocus(Object v) { this.autofocus = v; }
    public void cursorColor(Object v) { this.cursorColor = v; }
    public void restorationId(String v) { this.restorationId = v; }
    public void maxLengthEnforcement(Object v) { }
    public void onTap(Object v) { }
    public void buildCounter(Object v) { }

    @Override
    public Widget build(BuildContext context) {
        TextField field = new TextField();
        if (controller != null) {
            field.controller(controller);
        }
        if (decoration != null) {
            field.decoration(decoration);
        }
        field.obscureText(obscureText);
        if (enabled != null) {
            field.enabled(enabled);
        }
        if (onChanged != null) {
            field.onChanged(onChanged);
        }
        // maxLength, maxLines and minLines were accepted here and dropped on the
        // way to the field, so a form field could not count its characters or
        // grow past one line however it was configured. The text-field demo is
        // all TextFormField: its phone number asks for a 14-character counter
        // and its life story for three lines.
        if (maxLength instanceof Number) {
            field.maxLength(((Number) maxLength).longValue());
        }
        if (maxLines instanceof Number) {
            field.maxLines(((Number) maxLines).longValue());
        }
        if (minLines instanceof Number) {
            field.minLines(((Number) minLines).longValue());
        }
        if (keyboardType != null) {
            field.keyboardType(keyboardType);
        }
        return field;
    }
}
