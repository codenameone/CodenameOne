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
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Shared configuration of the material buttons ({@link ElevatedButton},
 * {@link TextButton}, {@link OutlinedButton}): a press callback (null means
 * disabled) and a content child consumed as the button's label or icon.
 */
public abstract class ButtonBase extends Widget {

    private Funcs.VoidFunc0 onPressed;
    private Widget child;
    private ButtonStyle style;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void style(ButtonStyle v) {
        this.style = v;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }

    public Widget getChild() {
        return child;
    }

    public ButtonStyle getStyle() {
        return style;
    }

    @Override
    public Element createElement() {
        return new ButtonRenderElement(this);
    }

    /**
     * The icon a {@code .icon} factory was given, beside the label.
     *
     * <p>Both were accepted and only one kept, so every icon button in the app
     * was a plain label -- the button demo's "+ BUTTON" pair came up as two more
     * "BUTTON"s. It is held HERE rather than composed into a Row with the label
     * because a button whose child is a row is mounted as a subtree, and the
     * button's own foreground then never reaches the text inside it: the label
     * came out black where the reference paints it in the button's colour.
     * Codename One's Button draws a glyph beside its text natively.</p>
     */
    private com.codename1.flutter.Widget leadingIcon;

    public void leadingIcon(com.codename1.flutter.Widget v) {
        this.leadingIcon = v;
    }

    public com.codename1.flutter.Widget getLeadingIcon() {
        return leadingIcon;
    }
}
