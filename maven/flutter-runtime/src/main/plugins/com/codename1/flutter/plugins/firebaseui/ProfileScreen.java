/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.firebaseui;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Scaffold;
import com.codename1.flutter.plugins.firebase.FirebaseAuth;
import com.codename1.flutter.plugins.firebase.User;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.Text;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * firebase_ui_auth's {@code ProfileScreen}: the signed-in user's name and email, the
 * {@code children} below them, and a sign-out button that fires the
 * {@link SignedOutAction}s. Account linking, deletion and editing the name are not offered;
 * {@code providers}, {@code avatar*} and {@code showDeleteConfirmationDialog} are accepted and
 * not applied.
 */
public class ProfileScreen extends StatelessWidget {
    DartList<Object> actions;
    DartList<Widget> children;
    Widget appBar;
    FirebaseAuth auth;

    public void actions(DartList<Object> v) {
        actions = v;
    }

    public void children(DartList<Widget> v) {
        children = v;
    }

    public void appBar(Widget v) {
        appBar = v;
    }

    public void auth(FirebaseAuth v) {
        auth = v;
    }

    public void providers(Object v) {
    }

    public void avatar(Object v) {
    }

    public void avatarSize(Object v) {
    }

    public void showDeleteConfirmationDialog(Object v) {
    }

    @Override
    public Widget build(final BuildContext context) {
        final FirebaseAuth a = auth == null ? FirebaseAuth.instance : auth;
        User u = a.currentUser();
        DartList<Widget> col = new DartList<Widget>();
        if (u != null) {
            if (u.displayName() != null) {
                col.add(Ui.padded(new Text(u.displayName())));
            }
            if (u.email() != null) {
                col.add(Ui.padded(new Text(u.email())));
            }
        }
        if (children != null) {
            col.addAll(children);
        }
        col.add(Ui.button("Sign out", false, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                a.signOut();
                SignedOutAction.fireAll(actions, context);
            }
        }));
        Column c = new Column();
        c.children(col);
        Padding p = new Padding();
        p.padding(com.codename1.flutter.EdgeInsets.all(16));
        p.child(c);
        Scaffold s = new Scaffold();
        if (appBar != null) {
            s.appBar(appBar);
        }
        s.body(p);
        return s;
    }
}
