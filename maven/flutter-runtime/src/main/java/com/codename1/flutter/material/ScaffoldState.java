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
import com.codename1.flutter.Clip;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * The mutable state of a {@link Scaffold} ({@code ScaffoldState} in Flutter),
 * reached via {@code Scaffold.of(context)}. Only the surface exercised by the
 * gallery is modelled: showing a bottom sheet (returning a controller whose
 * {@code closed} future the caller awaits), showing snack bars, and opening the
 * drawers. Rendering of these is a later milestone; the methods keep the right
 * shape so callers transpile and compile.
 */
public class ScaffoldState {

    /**
     * Shows a persistent bottom sheet built by {@code builder}, returning a
     * controller. The sheet is not mounted at this milestone; the controller's
     * {@code closed} future completes immediately.
     */
    public PersistentBottomSheetController showBottomSheet(Funcs.Func1<BuildContext, Widget> builder,
            Double elevation, Color backgroundColor, Object shape, Clip clipBehavior,
            Object constraints, Boolean enableDrag) {
        return new PersistentBottomSheetController();
    }

    public void showSnackBar(SnackBar snackBar) {
    }

    public void openDrawer() {
    }

    public void openEndDrawer() {
    }
}
