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
import com.codename1.flutter.widgets.Kit;
import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * Flutter's {@code ListBody}: children one after another along {@code mainAxis},
 * each stretched across the other axis, taking only the length they need.
 */
public class ListBody extends StatelessWidget {
    private DartList<Widget> children = DartList.of();
    private com.codename1.flutter.Axis mainAxis = com.codename1.flutter.Axis.vertical;
    private boolean reverse;
    public void children(DartList<Widget> v) { this.children = v == null ? DartList.<Widget>of() : v; }
    public void mainAxis(com.codename1.flutter.Axis v) { if (v != null) { this.mainAxis = v; } }
    public void reverse(boolean v) { this.reverse = v; }
    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = DartList.of();
        for (int i = 0; i < children.size(); i++) {
            kids.add(children.get(reverse ? children.size() - 1 - i : i));
        }
        Flex f = mainAxis == com.codename1.flutter.Axis.horizontal ? new Row() : new Column();
        f.mainAxisSize(com.codename1.flutter.MainAxisSize.min);
        f.crossAxisAlignment(com.codename1.flutter.CrossAxisAlignment.stretch);
        f.children(kids);
        return f;
    }
}
