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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * An iOS-style scrollbar wrapper — Flutter's {@code CupertinoScrollbar}. CN1
 * scrollables draw their own scrollbar, so this is a pass-through: it composes
 * its child directly.
 */
public class CupertinoScrollbar extends StatelessWidget {

    private Widget child;

    public void controller(Object v) {
    }

    public void thumbVisibility(boolean v) {
    }

    public void thickness(double v) {
    }

    public void thicknessWhileDragging(double v) {
    }

    public void radius(Object v) {
    }

    public void radiusWhileDragging(Object v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        // CN1's scrollables draw their own scrollbar, so wrapping one adds nothing. This
        // is a genuine pass-through rather than a missing feature.
        return child;
    }
}
