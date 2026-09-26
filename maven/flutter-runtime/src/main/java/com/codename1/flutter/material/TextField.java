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

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.FocusNode;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.services.TextInputAction;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A material single-line text input, backed by a CN1
 * {@code com.codename1.ui.TextField} (UIID "FlutterTextField"). Supports a
 * {@link TextEditingController} (two-way sync), {@link InputDecoration}
 * label/hint (rendered as the CN1 hint in M3), obscured (password) input,
 * enabled/disabled state and onChanged/onSubmitted callbacks.
 */
public class TextField extends Widget {

    private TextEditingController controller;
    private InputDecoration decoration;
    private Boolean obscureText;
    private Boolean enabled;
    private Funcs.VoidFunc1<String> onChanged;
    private Funcs.VoidFunc1<String> onSubmitted;
    private TextStyle style;
    private Color cursorColor;
    private TextInputAction textInputAction;
    private String restorationId;
    private Funcs.VoidFunc0 onTap;
    private Long maxLines = 1L;
    private Long minLines;
    private Long maxLength;
    private DartList<String> autofillHints;
    private Object keyboardType;
    private Object textCapitalization;
    private FocusNode focusNode;

    public void textAlign(TextAlign v) {
    }

    public void style(TextStyle v) {
        this.style = v;
    }

    public void cursorColor(Color v) {
        this.cursorColor = v;
    }

    public void textInputAction(TextInputAction v) {
        this.textInputAction = v;
    }

    public void restorationId(String v) {
        this.restorationId = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public Funcs.VoidFunc0 getOnTap() {
        return onTap;
    }

    public void maxLines(long v) {
        this.maxLines = v;
    }

    public void minLines(long v) {
        this.minLines = v;
    }

    /** How many lines the editor may grow to; 1 for a single-line field. */
    public Long getMaxLines() {
        return maxLines;
    }

    /** How many lines the editor shows before it has any content. */
    public Long getMinLines() {
        return minLines;
    }

    /**
     * The character limit, which is also what makes Material draw a counter.
     *
     * <p>Accepted and discarded before, so the demo's phone-number field showed
     * neither the limit nor the {@code 0/14} the reference counts out under
     * it.</p>
     */
    public void maxLength(long v) {
        this.maxLength = v;
    }

    public Long getMaxLength() {
        return maxLength;
    }

    public void autofillHints(DartList<String> v) {
        this.autofillHints = v;
    }

    public void keyboardType(Object v) {
        this.keyboardType = v;
    }

    public void textCapitalization(Object v) {
        this.textCapitalization = v;
    }

    public void focusNode(FocusNode v) {
        this.focusNode = v;
    }

    public FocusNode getFocusNode() {
        return focusNode;
    }

    public TextStyle getStyle() {
        return style;
    }

    public void controller(TextEditingController v) {
        this.controller = v;
    }

    public void decoration(InputDecoration v) {
        this.decoration = v;
    }

    public void obscureText(boolean v) {
        this.obscureText = v;
    }

    public void enabled(boolean v) {
        this.enabled = v;
    }

    public void onChanged(Funcs.VoidFunc1<String> v) {
        this.onChanged = v;
    }

    public void onSubmitted(Funcs.VoidFunc1<String> v) {
        this.onSubmitted = v;
    }

    public TextEditingController getController() {
        return controller;
    }

    public InputDecoration getDecoration() {
        return decoration;
    }

    public boolean isObscureText() {
        return obscureText != null && obscureText;
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public Funcs.VoidFunc1<String> getOnChanged() {
        return onChanged;
    }

    public Funcs.VoidFunc1<String> getOnSubmitted() {
        return onSubmitted;
    }

    @Override
    public Element createElement() {
        return new TextFieldRenderElement(this);
    }
}
