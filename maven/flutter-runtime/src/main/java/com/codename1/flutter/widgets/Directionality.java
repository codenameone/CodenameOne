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
import com.codename1.flutter.TextDirection;

/**
 * Establishes the reading direction for its subtree, mirroring Flutter's
 * {@code Directionality}: an inherited widget, so {@link #of} answers the
 * NEAREST enclosing direction and a reader rebuilds when it changes.
 * Layout-transparent -- it builds its child unchanged.
 *
 * <p>It used to be a plain wrapper whose {@code of} always answered LTR, so a
 * {@code Directionality(textDirection: TextDirection.rtl)} subtree laid out
 * {@code PositionedDirectional} and every other start/end consumer as if it
 * were left-to-right. MaterialApp installs the root one from the app's
 * resolved locale, as Flutter's WidgetsApp does through its Localizations.</p>
 */
public class Directionality extends InheritedWidget {

    private TextDirection textDirection;

    public void textDirection(TextDirection v) {
        this.textDirection = v;
    }

    public TextDirection getTextDirection() {
        return textDirection;
    }

    @Override
    public boolean updateShouldNotify(InheritedWidget oldWidget) {
        return !(oldWidget instanceof Directionality)
                || ((Directionality) oldWidget).textDirection != textDirection;
    }

    /**
     * Dart's {@code Directionality.of(context)}: the text direction of the
     * nearest enclosing Directionality, registering the caller to rebuild when
     * it changes. Flutter asserts one exists; with none above (a bare widget
     * mounted outside any app) this answers LTR, the direction of the default
     * WidgetsLocalizations, rather than failing.
     */
    public static TextDirection of(BuildContext context) {
        TextDirection d = maybeOf(context);
        return d != null ? d : TextDirection.ltr;
    }

    /** Dart's {@code Directionality.maybeOf(context)}: null when none is above. */
    public static TextDirection maybeOf(BuildContext context) {
        Directionality d = context == null
                ? null
                : context.maybeDependOnInheritedWidgetOfExactType(Directionality.class);
        return d == null ? null : d.textDirection;
    }
}
