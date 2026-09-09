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

import dart.async.Future;
import dart.runtime.Funcs;

/**
 * Host class for Dart's top-level {@code showModalBottomSheet} function. This
 * milestone is a bookkeeping stub: it returns an already-completed
 * {@link Future} (the modal sheet is dismissed immediately) so a non-awaited
 * {@code showModalBottomSheet(...)} call transpiles and runs to completion. The
 * modal presentation of {@code builder}'s widget tree is deferred; wiring it to
 * a CN1 {@code Dialog} the way {@link Dialogs} does is a follow-up.
 */
public final class BottomSheets {

    private BottomSheets() {
    }

    public static Future<Object> showModalBottomSheet(BuildContext context,
                                                      Funcs.Func1<BuildContext, Widget> builder,
                                                      Color backgroundColor,
                                                      Double elevation,
                                                      Object shape,
                                                      Clip clipBehavior,
                                                      Object constraints,
                                                      Color barrierColor,
                                                      Boolean isScrollControlled,
                                                      Boolean useRootNavigator,
                                                      Boolean isDismissible,
                                                      Boolean enableDrag,
                                                      Boolean showDragHandle,
                                                      Object routeSettings,
                                                      Object transitionAnimationController) {
        return Future.value(null);
    }
}
