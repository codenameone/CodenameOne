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
import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.widgets.Column;
import com.codename1.flutter.widgets.GestureDetector;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A long-press context menu — Flutter's {@code CupertinoContextMenu}.
 *
 * <p>Press and hold reveals the actions. Flutter blurs the page and floats a scaled preview
 * of the child above the list; here the actions are presented as a modal sheet, so the menu
 * is reachable and its actions run even though the reveal is plainer than iOS's.</p>
 */
public class CupertinoContextMenu extends StatelessWidget {

    private DartList<Widget> actions;
    private Widget child;

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public void previewBuilder(Object v) {
    }

    public DartList<Widget> getActions() {
        return actions;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(final BuildContext context) {
        if (actions == null || actions.isEmpty()) {
            return child;
        }
        GestureDetector press = new GestureDetector();
        press.child(child);
        press.onLongPress(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                Dialogs.showDialog(context, new Funcs.Func1<BuildContext, Widget>() {
                    @Override
                    public Widget call(BuildContext dialogContext) {
                        Column list = new Column();
                        list.mainAxisSize(com.codename1.flutter.MainAxisSize.min);
                        list.children(actions);
                        return list;
                    }
                });
            }
        });
        return press;
    }
}
