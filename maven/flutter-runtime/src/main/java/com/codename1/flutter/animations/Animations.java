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
package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Dialogs;

import dart.runtime.Funcs;

/**
 * Top-level entry points of the {@code animations} package. Currently hosts
 * {@code showModal}, which shows a modal route with an {@code animations}-package
 * transition (fade-scale by default). This pass presents the modal via the
 * Material {@link Dialogs#showDialog} plumbing; the package's custom
 * fade/scale reveal is deferred.
 */
public final class Animations {

    private Animations() {
    }

    /**
     * Shows a modal built by {@code builder} over the current route — the
     * {@code animations} package's top-level {@code showModal}. The
     * {@code configuration}, {@code useRootNavigator} and {@code filter}
     * parameters are captured for API shape.
     *
     * @return the route's completion result (a future value); ignored by
     *         callers that do not await the dismissal
     */
    public static Object showModal(BuildContext context, Object configuration,
                                   Object useRootNavigator,
                                   Funcs.Func1<BuildContext, Widget> builder,
                                   Object filter) {
        if (builder != null) {
            return Dialogs.showDialog(context, builder);
        }
        return dart.async.Future.value(null);
    }
}
