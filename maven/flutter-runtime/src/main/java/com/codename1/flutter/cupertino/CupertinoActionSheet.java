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
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

/**
 * An iOS bottom action sheet — Flutter's {@code CupertinoActionSheet}: an
 * optional title / message, a list of {@link CupertinoActionSheetAction}s and
 * an optional cancel button. Laid out as a vertical {@link Column} of those
 * parts this pass (the sheet chrome / slide-up is approximate).
 */
public class CupertinoActionSheet extends StatelessWidget {

    private Widget title;
    private Widget message;
    private DartList<Widget> actions;
    private Widget cancelButton;

    public void title(Widget v) {
        this.title = v;
    }

    public void message(Widget v) {
        this.message = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void messageScrollController(Object v) {
    }

    public void actionScrollController(Object v) {
    }

    public void cancelButton(Widget v) {
        this.cancelButton = v;
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (title != null) {
            kids.add(title);
        }
        if (message != null) {
            kids.add(message);
        }
        if (actions != null) {
            kids.addAll(actions);
        }
        if (cancelButton != null) {
            kids.add(cancelButton);
        }
        Column col = new Column();
        col.children(kids);
        return col;
    }
}
