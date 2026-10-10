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
import com.codename1.flutter.material.TextButton;

import dart.runtime.Funcs;

/**
 * An entry of a {@link CupertinoContextMenu} — Flutter's
 * {@code CupertinoContextMenuAction}. Composed onto a material
 * {@link TextButton} this pass.
 */
public class CupertinoContextMenuAction extends StatelessWidget {

    private Funcs.VoidFunc0 onPressed;
    private Widget child;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void isDefaultAction(boolean v) {
    }

    public void isDestructiveAction(boolean v) {
    }

    public void trailingIcon(Widget v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        TextButton b = new TextButton();
        b.onPressed(onPressed);
        b.child(child);
        return b;
    }
}
