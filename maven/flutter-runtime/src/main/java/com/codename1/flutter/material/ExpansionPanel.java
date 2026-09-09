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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * One panel in an {@link ExpansionPanelList} — Flutter's {@code ExpansionPanel}.
 * A configuration object with a {@code headerBuilder(context, isExpanded)} and a
 * {@code body}.
 */
public class ExpansionPanel {

    private Funcs.Func2<BuildContext, Boolean, Widget> headerBuilder;
    private Widget body;
    private boolean isExpanded;

    public void headerBuilder(Funcs.Func2<BuildContext, Boolean, Widget> v) {
        this.headerBuilder = v;
    }

    public void body(Widget v) {
        this.body = v;
    }

    public void isExpanded(boolean v) {
        this.isExpanded = v;
    }

    public void canTapOnHeader(boolean v) {
    }

    public void backgroundColor(Color v) {
    }

    public Funcs.Func2<BuildContext, Boolean, Widget> getHeaderBuilder() {
        return headerBuilder;
    }

    public Widget getBody() {
        return body;
    }

    public boolean isExpanded() {
        return isExpanded;
    }
}
