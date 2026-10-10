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
import com.codename1.flutter.material.AlertDialog;

import dart.core.DartList;

/**
 * An iOS-style alert dialog — Flutter's {@code CupertinoAlertDialog}: a title,
 * optional content and a set of {@link CupertinoDialogAction} buttons.
 * Composed onto the material {@link AlertDialog} (shown through the same
 * {@code CupertinoDialogRoute} / dialog surface; visually approximate).
 */
public class CupertinoAlertDialog extends StatelessWidget {

    private Widget title;
    private Widget content;
    private DartList<Widget> actions;

    public void title(Widget v) {
        this.title = v;
    }

    public void content(Widget v) {
        this.content = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void scrollController(Object v) {
    }

    public void actionScrollController(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        AlertDialog d = new AlertDialog();
        if (title != null) {
            d.title(title);
        }
        if (content != null) {
            d.content(content);
        }
        if (actions != null) {
            d.actions(actions);
        }
        return d;
    }
}
