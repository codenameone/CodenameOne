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

import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.LayeredLayout;

/// Views added straight to the window manager are overlays: they go on the
/// current form's layered pane, positioned by the window layout params.
final class WindowManagerImpl implements WindowManager {

    @Override
    public Display getDefaultDisplay() {
        return Display.DEFAULT;
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams params) {
        Form f = android.app.ActivityThread.visibleForm();
        if (f == null) {
            return;
        }
        if (!(view instanceof ViewGroup)) {
            CompatReport.unsupported("window", "overlay of a non-ViewGroup view");
            return;
        }
        Component peer = view.getPeer();
        view.setLayoutParams(params);
        f.getLayeredPane(WindowManagerImpl.class, true).setLayout(new LayeredLayout());
        f.getLayeredPane(WindowManagerImpl.class, true).add(peer);
        view.dispatchAttachedToWindow(true);
        f.revalidate();
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        view.setLayoutParams(params);
    }

    @Override
    public void removeView(View view) {
        if (view.hasPeer() && view.getPeer().getParent() != null) {
            Form f = view.getPeer().getComponentForm();
            view.getPeer().remove();
            view.dispatchAttachedToWindow(false);
            if (f != null) {
                f.revalidate();
            }
        }
    }
}
