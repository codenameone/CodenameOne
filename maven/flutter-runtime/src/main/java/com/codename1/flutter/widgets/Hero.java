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
 * Marks a subtree as a shared-element that flies between routes during a
 * navigation transition — Flutter's {@code Hero}. This milestone renders the
 * {@code child} in place; the cross-route flight animation is deferred.
 */
public class Hero extends StatelessWidget {

    private Object tag;
    private Widget child;
    private boolean transitionOnUserGestures;

    public void tag(Object v) {
        this.tag = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void createRectTween(Object v) {
    }

    public void flightShuttleBuilder(Object v) {
    }

    public void placeholderBuilder(Object v) {
    }

    public void transitionOnUserGestures(boolean v) {
        this.transitionOnUserGestures = v;
    }

    @Override
    public Widget build(BuildContext context) {
        return child != null ? child : new SizedBox();
    }
}
