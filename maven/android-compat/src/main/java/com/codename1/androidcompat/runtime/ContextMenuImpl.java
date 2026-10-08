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
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.View;

/// A long-press context menu.
public class ContextMenuImpl extends MenuImpl implements ContextMenu {

    public ContextMenuImpl(Context context) {
        super(context);
    }

    @Override
    public ContextMenu setHeaderTitle(int titleRes) {
        setHeaderTitleInternal(context.getText(titleRes));
        return this;
    }

    @Override
    public ContextMenu setHeaderTitle(CharSequence title) {
        setHeaderTitleInternal(title);
        return this;
    }

    @Override
    public ContextMenu setHeaderIcon(int iconRes) {
        return this;
    }

    @Override
    public ContextMenu setHeaderIcon(Drawable icon) {
        return this;
    }

    @Override
    public ContextMenu setHeaderView(View view) {
        return this;
    }

    @Override
    public void clearHeader() {
        setHeaderTitleInternal(null);
    }
}
