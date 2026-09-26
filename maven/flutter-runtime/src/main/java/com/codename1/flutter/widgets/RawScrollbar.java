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
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A scrollbar with no theme defaults wrapping a scrollable — Flutter's
 * {@code RawScrollbar}. This milestone renders the {@code child}; the scrollbar
 * track/thumb overlay is deferred (CN1 scrollables draw their own indicator).
 */
public class RawScrollbar extends StatelessWidget {

    private Widget child;

    public void child(Widget v) {
        this.child = v;
    }

    public void controller(Object v) {
    }

    private boolean thumbVisibility;

    public void thumbVisibility(boolean v) {
        this.thumbVisibility = v;
    }

    /** Whether the thumb stays on screen when nothing is scrolling. */
    public boolean isThumbVisible() {
        return thumbVisibility;
    }

    public void thumbColor(Object v) {
    }

    public void radius(Object v) {
    }

    public void thickness(double v) {
    }

    public void interactive(boolean v) {
    }

    public void notificationPredicate(Object v) {
    }

    public void scrollbarOrientation(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
