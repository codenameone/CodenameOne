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
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Dialogs;

import dart.runtime.Funcs;

/**
 * Host class for Dart's top-level {@code showCupertinoDialog} and
 * {@code showCupertinoModalPopup} functions. Both present the built widget
 * tree through the shared modeless dialog surface (see
 * {@link Dialogs#showDialog}) — the iOS-specific barrier / slide-up chrome is
 * approximate this pass.
 */
public final class CupertinoDialogs {

    private CupertinoDialogs() {
    }

    public static dart.async.Future<Object> showCupertinoDialog(BuildContext context,
                                           Funcs.Func1<BuildContext, Widget> builder,
                                           Boolean barrierDismissible, Color barrierColor,
                                           String barrierLabel, Boolean useRootNavigator,
                                           Object routeSettings) {
        return Dialogs.showDialog(context, builder);
    }

    public static dart.async.Future<Object> showCupertinoModalPopup(BuildContext context,
                                               Funcs.Func1<BuildContext, Widget> builder,
                                               Color barrierColor, Boolean barrierDismissible,
                                               Boolean useRootNavigator, Object semanticsDismissible,
                                               Object routeSettings) {
        return Dialogs.showModalPopup(context, builder);
    }
}
