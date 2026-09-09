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

import com.codename1.flutter.TextDirection;

import dart.runtime.Funcs;

/**
 * A bag of semantic annotations passed to {@link Semantics#fromProperties} and
 * to {@code CustomPainterSemantics}, mirroring Flutter's
 * {@code SemanticsProperties}. The annotations are retained but not yet mapped
 * onto Codename One accessibility at this milestone; the named-argument setters
 * accept the values the emitter feeds after construction.
 */
public class SemanticsProperties {

    private String label;
    private String value;
    private TextDirection textDirection;
    private Boolean button;
    private Boolean enabled;

    public void enabled(boolean v) { this.enabled = v; }
    public void checked(boolean v) { }
    public void selected(boolean v) { }
    public void toggled(boolean v) { }
    public void button(boolean v) { this.button = v; }
    public void link(boolean v) { }
    public void header(boolean v) { }
    public void textField(boolean v) { }
    public void readOnly(boolean v) { }
    public void focusable(boolean v) { }
    public void focused(boolean v) { }
    public void inMutuallyExclusiveGroup(boolean v) { }
    public void hidden(boolean v) { }
    public void obscured(boolean v) { }
    public void multiline(boolean v) { }
    public void scopesRoute(boolean v) { }
    public void namesRoute(boolean v) { }
    public void image(boolean v) { }
    public void liveRegion(boolean v) { }
    public void label(String v) { this.label = v; }
    public void value(String v) { this.value = v; }
    public void increasedValue(String v) { }
    public void decreasedValue(String v) { }
    public void hint(String v) { }
    public void onTapHint(String v) { }
    public void onLongPressHint(String v) { }
    public void textDirection(TextDirection v) { this.textDirection = v; }
    public void sortKey(Object v) { }
    public void onTap(Funcs.VoidFunc0 v) { }
    public void onLongPress(Funcs.VoidFunc0 v) { }

    public String getLabel() { return label; }
    public String getValue() { return value; }
    public TextDirection getTextDirection() { return textDirection; }
    public Boolean getButton() { return button; }
    public Boolean getEnabled() { return enabled; }
}
