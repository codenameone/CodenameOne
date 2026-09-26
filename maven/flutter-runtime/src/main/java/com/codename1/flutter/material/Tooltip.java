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

import com.codename1.flutter.Element;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.HasChild;
import com.codename1.flutter.widgets.PassThroughRenderElement;

/**
 * A material tooltip that shows a label on long-press/hover. The label is
 * retained but not yet shown; the child renders unchanged. See
 * {@link PassThroughRenderElement}.
 */
public class Tooltip extends Widget implements HasChild {

    private String message;
    private Object richMessage;
    private double height;
    private Object padding;
    private Object margin;
    private double verticalOffset;
    private boolean preferBelow = true;
    private boolean excludeFromSemantics;
    private Object decoration;
    private TextStyle textStyle;
    private Object waitDuration;
    private Object showDuration;
    private Object triggerMode;
    private Widget child;

    public void message(String v) {
        this.message = v;
    }

    public void richMessage(Object v) {
        this.richMessage = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void padding(Object v) {
        this.padding = v;
    }

    public void margin(Object v) {
        this.margin = v;
    }

    public void verticalOffset(double v) {
        this.verticalOffset = v;
    }

    public void preferBelow(boolean v) {
        this.preferBelow = v;
    }

    public void excludeFromSemantics(boolean v) {
        this.excludeFromSemantics = v;
    }

    public void decoration(Object v) {
        this.decoration = v;
    }

    public void textStyle(TextStyle v) {
        this.textStyle = v;
    }

    public void waitDuration(Object v) {
        this.waitDuration = v;
    }

    public void showDuration(Object v) {
        this.showDuration = v;
    }

    public void triggerMode(Object v) {
        this.triggerMode = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}
