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

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Annotates its child subtree with accessibility semantics. The Codename One
 * runtime renders the child unchanged for this milestone (the annotations are
 * retained but not yet mapped onto CN1 accessibility). See
 * {@link PassThroughRenderElement}.
 */
public class Semantics extends Widget implements HasChild {

    private Widget child;
    private String label;
    private String value;
    private String increasedValue;
    private String decreasedValue;
    private String hint;
    private String tooltip;
    private Object sortKey;
    private Funcs.VoidFunc0 onTap;
    private Funcs.VoidFunc0 onLongPress;

    public void child(Widget v) {
        this.child = v;
    }

    public void container(boolean v) {
    }

    public void explicitChildNodes(boolean v) {
    }

    public void excludeSemantics(boolean v) {
    }

    public void enabled(boolean v) {
    }

    public void checked(boolean v) {
    }

    public void selected(boolean v) {
    }

    public void toggled(boolean v) {
    }

    public void button(boolean v) {
    }

    public void link(boolean v) {
    }

    public void header(boolean v) {
    }

    public void textField(boolean v) {
    }

    public void readOnly(boolean v) {
    }

    public void focusable(boolean v) {
    }

    public void focused(boolean v) {
    }

    public void image(boolean v) {
    }

    public void liveRegion(boolean v) {
    }

    public void hidden(boolean v) {
    }

    public void obscured(boolean v) {
    }

    public void multiline(boolean v) {
    }

    public void label(String v) {
        this.label = v;
    }

    public void value(String v) {
        this.value = v;
    }

    public void increasedValue(String v) {
        this.increasedValue = v;
    }

    public void decreasedValue(String v) {
        this.decreasedValue = v;
    }

    public void hint(String v) {
        this.hint = v;
    }

    public void tooltip(String v) {
        this.tooltip = v;
    }

    public void sortKey(Object v) {
        this.sortKey = v;
    }

    public void onLongPressHint(String v) {
    }

    public void onTapHint(String v) {
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public void onLongPress(Funcs.VoidFunc0 v) {
        this.onLongPress = v;
    }

    public void properties(SemanticsProperties v) {
        if (v != null) {
            this.label = v.getLabel();
            this.value = v.getValue();
        }
    }

    /**
     * {@code Semantics.fromProperties}: builds a Semantics node from a
     * pre-assembled {@link SemanticsProperties} bag. Positional parameters mirror
     * the stub's named-argument declaration order.
     */
    public static Semantics fromProperties(com.codename1.flutter.Key key, SemanticsProperties properties,
            Boolean container, Boolean explicitChildNodes, Boolean excludeSemantics, Widget child) {
        Semantics s = new Semantics();
        s.properties(properties);
        if (child != null) {
            s.child(child);
        }
        return s;
    }

    public String getLabel() {
        return label;
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
