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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Flutter's InheritedWidget: a widget that exposes itself to descendants via
 * {@link BuildContext#dependOnInheritedWidgetOfExactType(Class)} and otherwise
 * renders its single {@code child}. Application subclasses (PageStatus,
 * LayoutCache, CodeStyle, ...) extend this and add their own fields; the lookup
 * is by runtime type, so no per-type wiring is required.
 *
 * <p>Rendered as a {@link StatelessWidget} whose {@code build} returns the child; the
 * {@link InheritedElement} it produces sits in the tree as the discoverable ancestor, and
 * is what remembers the descendants that read it so {@link #updateShouldNotify} can rebuild
 * them.</p>
 */
/*
 * Implements HasChild so that the helpers which walk down a wrapper chain --
 * "what glyph is inside this button?", "what text is inside this label?" --
 * can see THROUGH an inherited widget. IconTheme and DefaultTextStyle are
 * inherited widgets, and they are exactly what a theme puts between a button
 * and its icon: the gallery wraps every study's back arrow in an IconTheme, and
 * the FAB's glyph search stopped there and fell back to a plus sign.
 */
public class InheritedWidget extends StatelessWidget implements HasChild {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    /**
     * Whether descendants that read this widget should rebuild — Flutter's
     * {@code updateShouldNotify}. Defaults to true: a rebuilt inherited widget usually
     * carries a new value, and a false negative is invisible (stale UI) where a false
     * positive only costs a rebuild.
     */
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return true;
    }

    @Override
    public Element createElement() {
        return new InheritedElement(this);
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
