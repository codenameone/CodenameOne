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
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Row;

import dart.core.DartList;

/**
 * A horizontal set of toggle buttons that share a selection state — Flutter's
 * {@code ToggleButtons}. {@code isSelected} runs parallel to {@code children};
 * {@code onPressed} fires with the tapped index. This pass lays the children
 * out in a {@link Row} and records styling for API shape; ripple, borders and
 * selection painting are deferred.
 */
public class ToggleButtons extends StatelessWidget {

    private DartList<Widget> children;
    private DartList<Boolean> isSelected;
    private Object onPressed;
    private TextStyle textStyle;
    private Object constraints;
    private Color color;
    private Color selectedColor;
    private Color disabledColor;
    private Color fillColor;
    private Color focusColor;
    private Color highlightColor;
    private Color hoverColor;
    private Color splashColor;
    private boolean renderBorder = true;
    private Color borderColor;
    private Color selectedBorderColor;
    private Color disabledBorderColor;
    private Object borderRadius;
    private Double borderWidth;
    private Object direction;

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void isSelected(DartList<Boolean> v) {
        this.isSelected = v;
    }

    public void onPressed(dart.runtime.Funcs.VoidFunc1<Long> v) {
        this.onPressed = v;
    }

    public void textStyle(TextStyle v) {
        this.textStyle = v;
    }

    public void constraints(Object v) {
        this.constraints = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void selectedColor(Color v) {
        this.selectedColor = v;
    }

    public void disabledColor(Color v) {
        this.disabledColor = v;
    }

    public void fillColor(Color v) {
        this.fillColor = v;
    }

    public void focusColor(Color v) {
        this.focusColor = v;
    }

    public void highlightColor(Color v) {
        this.highlightColor = v;
    }

    public void hoverColor(Color v) {
        this.hoverColor = v;
    }

    public void splashColor(Color v) {
        this.splashColor = v;
    }

    public void renderBorder(boolean v) {
        this.renderBorder = v;
    }

    public void borderColor(Color v) {
        this.borderColor = v;
    }

    public void selectedBorderColor(Color v) {
        this.selectedBorderColor = v;
    }

    public void disabledBorderColor(Color v) {
        this.disabledBorderColor = v;
    }

    public void borderRadius(Object v) {
        this.borderRadius = v;
    }

    public void borderWidth(double v) {
        this.borderWidth = v;
    }

    public void direction(Object v) {
        this.direction = v;
    }

    public DartList<Widget> getChildren() {
        return children;
    }

    public DartList<Boolean> getIsSelected() {
        return isSelected;
    }

    public Object getOnPressed() {
        return onPressed;
    }

    @Override
    public Widget build(BuildContext context) {
        Row row = new Row();
        row.children(children);
        return row;
    }
}
