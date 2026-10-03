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

import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Render element for {@link PopupMenuButton}: draws the trigger as a real button and OPENS
 * THE MENU when it is pressed.
 *
 * <p>It is a {@link ButtonRenderElement} rather than a tappable wrapper around the glyph,
 * and that is not incidental. An InkWell's gesture pane is an ordinary sibling in the flat
 * component list, so a scroll view's own pane — added later, sitting above — swallowed the
 * press before it arrived: the menu button drew correctly and did nothing, which is
 * indistinguishable from a missing handler. A CN1 Button receives its own events, exactly as
 * the neighbouring IconButtons in the same app bar already did.</p>
 *
 * <p>With neither {@code child} nor {@code icon} the trigger is Flutter's overflow glyph, so
 * a menu button written the ordinary way is visible at all.</p>
 */
public class PopupMenuButtonRenderElement extends ButtonRenderElement {

    private final Funcs.VoidFunc0 open = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            // Read through widget() so a rebuilt configuration is honoured rather than the
            // one that happened to be current when this element was created.
            PopupMenus.show(PopupMenuButtonRenderElement.this, button(), component());
        }
    };

    public PopupMenuButtonRenderElement(PopupMenuButton<?> widget) {
        super(widget);
    }

    private PopupMenuButton<?> button() {
        return (PopupMenuButton<?>) widget();
    }

    @Override
    protected Widget rawContentWidget() {
        return button().effectiveTrigger(this);
    }

    @Override
    protected Funcs.VoidFunc0 onPressed() {
        // A null handler would also DISABLE the button, which is what we want when the Dart
        // says the menu is disabled.
        return button().isEnabled() ? open : null;
    }
}
