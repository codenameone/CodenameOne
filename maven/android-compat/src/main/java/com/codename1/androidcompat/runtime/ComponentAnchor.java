/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.androidcompat.runtime;

import android.content.Context;
import android.view.View;

import com.codename1.ui.Component;

/// A detached view standing in for a Codename One component as a popup
/// anchor: it reports the component's size and screen position, so a popup
/// can drop down from a toolbar button the Android tree does not contain.
public final class ComponentAnchor extends View {

    private final Component target;

    public ComponentAnchor(Context context, Component target) {
        super(context);
        this.target = target;
        layout(0, 0, target.getWidth(), target.getHeight());
    }

    @Override
    public void getLocationOnScreen(int[] outLocation) {
        outLocation[0] = target.getAbsoluteX();
        outLocation[1] = target.getAbsoluteY();
    }
}
