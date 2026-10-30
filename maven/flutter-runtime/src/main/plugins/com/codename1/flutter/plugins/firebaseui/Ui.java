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

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.ElevatedButton;
import com.codename1.flutter.material.OutlinedButton;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.Text;

import dart.runtime.Funcs;

/** The widgets the FirebaseUI screens are built from, made the way generated code makes them. */
final class Ui {
    private Ui() {
    }

    static Widget padded(Widget child) {
        Padding p = new Padding();
        p.padding(EdgeInsets.symmetric(0, 6));
        p.child(child);
        return p;
    }

    static Widget button(String label, boolean primary, Funcs.VoidFunc0 onPressed) {
        if (primary) {
            ElevatedButton b = new ElevatedButton();
            b.child(new Text(label));
            b.onPressed(onPressed);
            return padded(b);
        }
        OutlinedButton b = new OutlinedButton();
        b.child(new Text(label));
        b.onPressed(onPressed);
        return padded(b);
    }

    static String message(Object err) {
        if (err instanceof com.codename1.flutter.plugins.firebase.FirebaseException) {
            String m = ((com.codename1.flutter.plugins.firebase.FirebaseException) err).message();
            if (m != null) {
                return m;
            }
        }
        return String.valueOf(err);
    }
}
