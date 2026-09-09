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
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * The stack of {@link OverlayEntry} objects floating above the navigator —
 * Flutter's {@code Overlay}. new_gallery reaches the ambient overlay through the
 * static {@link #of(BuildContext, boolean, Object)} to insert feature-discovery
 * entries; the {@code Overlay} widget itself is provided by the navigator and is
 * not constructed by the app, so its element holds no children at this pass.
 */
public class Overlay extends Widget {

    private static final OverlayState SHARED_STATE = new OverlayState();

    private DartList<OverlayEntry> initialEntries;
    private Object clipBehavior;

    public void initialEntries(DartList<OverlayEntry> v) {
        this.initialEntries = v;
    }

    public void clipBehavior(Object v) {
        this.clipBehavior = v;
    }

    /** Flutter's {@code Overlay.of} — the nearest ancestor overlay's state. */
    public static OverlayState of(BuildContext context, boolean rootOverlay, Object debugRequiredFor) {
        return SHARED_STATE;
    }

    /** Flutter's {@code Overlay.maybeOf}. */
    public static OverlayState maybeOf(BuildContext context, boolean rootOverlay) {
        return SHARED_STATE;
    }

    @Override
    public Element createElement() {
        return new SimpleChildrenRenderElement(this, new SimpleChildrenRenderElement.Children() {
            @Override
            public DartList<Widget> get() {
                return null;
            }
        });
    }
}
