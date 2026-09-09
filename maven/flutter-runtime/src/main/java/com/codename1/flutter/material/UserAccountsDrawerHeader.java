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
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

/**
 * A Material drawer header showing the signed-in account — Flutter's
 * {@code UserAccountsDrawerHeader}. This pass renders the account picture, name
 * and email stacked in a {@link Column}; the themed background, details arrow and
 * other-account switching are captured for a later render pass.
 */
public class UserAccountsDrawerHeader extends StatelessWidget {

    private Object decoration;
    private Object margin;
    private Widget currentAccountPicture;
    private DartList<Widget> otherAccountsPictures;
    private Widget accountName;
    private Widget accountEmail;
    private Object onDetailsPressed;
    private Object arrowColor;

    public void decoration(Object v) { this.decoration = v; }
    public void margin(Object v) { this.margin = v; }
    public void currentAccountPicture(Widget v) { this.currentAccountPicture = v; }
    public void otherAccountsPictures(DartList<Widget> v) { this.otherAccountsPictures = v; }
    public void accountName(Widget v) { this.accountName = v; }
    public void accountEmail(Widget v) { this.accountEmail = v; }
    public void onDetailsPressed(Object v) { this.onDetailsPressed = v; }
    public void arrowColor(Object v) { this.arrowColor = v; }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> children = new DartList<Widget>();
        if (currentAccountPicture != null) {
            children.add(currentAccountPicture);
        }
        if (accountName != null) {
            children.add(accountName);
        }
        if (accountEmail != null) {
            children.add(accountEmail);
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.start);
        col.children(children);
        return col;
    }
}
