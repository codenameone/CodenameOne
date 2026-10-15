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
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;
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
 *
 * <p>A {@link StatefulWidget}, not a stateless one, for the same reason Flutter's own
 * {@code TextFormField} is: when no {@link #controller} is given, {@code initialValue}
 * has to seed an internal {@link TextEditingController} kept alive by the {@code State}
 * across rebuilds -- a FRESH {@code TextFormField} instance arrives on every rebuild
 * (the app's own {@code build()} constructs one), so anything stashed on the widget
 * itself would be rebuilt away with it. See {@link TextFormFieldState}.</p>
 */
public class TextFormField extends StatefulWidget {

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
    public State<TextFormField> createState() {
        return new TextFormFieldState();
    }

    /**
     * Mirrors Flutter's {@code _TextFormFieldState}: owns the internal controller a
     * field with no {@link #controller} gets, seeded ONCE (in {@link #initState}) with
     * {@code initialValue} and then left alone -- neither a later change of
     * {@code initialValue} nor anything the user types is touched again on a rebuild,
     * because {@link #build} keeps handing the SAME controller instance to the
     * {@link TextField} it builds, every time.
     */
    private static final class TextFormFieldState extends State<TextFormField> {

        /** The controller this state owns, or null while an external one is in use. */
        private TextEditingController localController;

        @Override
        public void initState() {
            TextFormField w = widget();
            if (w.controller == null) {
                localController = new TextEditingController();
                if (w.initialValue != null) {
                    localController.setText(w.initialValue);
                }
            }
        }

        @Override
        public void didUpdateWidget(TextFormField oldWidget) {
            TextFormField w = widget();
            if (w.controller == oldWidget.controller) {
                // Same controller identity (both null, or the same external instance):
                // initialValue is consulted only once, at initState -- a later change
                // must not overwrite what is already showing, user edits included.
                return;
            }
            if (w.controller == null) {
                // Switched FROM an external controller back to none: Flutter's
                // _createLocalController(oldWidget.controller!.value) -- a fresh local
                // controller, seeded with whatever the external one currently holds, so
                // the switch does not blank the field.
                localController = new TextEditingController();
                localController.setText(oldWidget.controller.text());
            } else {
                // Switched TO an external controller: it is now authoritative and the
                // local one (if there was one) is dropped, exactly as Flutter disposes
                // its RestorableTextEditingController here.
                if (localController != null) {
                    localController.dispose();
                    localController = null;
                }
            }
        }

        @Override
        public void dispose() {
            if (localController != null) {
                localController.dispose();
                localController = null;
            }
        }

        private TextEditingController effectiveController() {
            TextFormField w = widget();
            return w.controller != null ? w.controller : localController;
        }

        @Override
        public Widget build(BuildContext context) {
            TextFormField w = widget();
            TextField field = new TextField();
            TextEditingController effective = effectiveController();
            if (effective != null) {
                field.controller(effective);
            }
            if (w.decoration != null) {
                field.decoration(w.decoration);
            }
            field.obscureText(w.obscureText);
            if (w.enabled != null) {
                field.enabled(w.enabled);
            }
            if (w.onChanged != null) {
                field.onChanged(w.onChanged);
            }
            // maxLength, maxLines and minLines were accepted here and dropped on the
            // way to the field, so a form field could not count its characters or
            // grow past one line however it was configured. The text-field demo is
            // all TextFormField: its phone number asks for a 14-character counter
            // and its life story for three lines.
            if (w.maxLength instanceof Number) {
                field.maxLength(((Number) w.maxLength).longValue());
            }
            if (w.maxLines instanceof Number) {
                field.maxLines(((Number) w.maxLines).longValue());
            }
            if (w.minLines instanceof Number) {
                field.minLines(((Number) w.minLines).longValue());
            }
            if (w.keyboardType != null) {
                field.keyboardType(w.keyboardType);
            }
            return field;
        }
    }
}
