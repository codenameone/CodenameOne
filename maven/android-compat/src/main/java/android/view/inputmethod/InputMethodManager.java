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
package android.view.inputmethod;

import android.view.View;
import com.codename1.ui.Display;

/// Soft keyboard control. The keyboard follows Codename One's text editing:
/// showing it starts editing the focused field, hiding it stops.
public final class InputMethodManager {

    public static final int SHOW_IMPLICIT = 0x0001;
    public static final int SHOW_FORCED = 0x0002;
    public static final int HIDE_IMPLICIT_ONLY = 0x0001;
    public static final int HIDE_NOT_ALWAYS = 0x0002;
    public static final int RESULT_UNCHANGED_SHOWN = 0;
    public static final int RESULT_SHOWN = 2;
    public static final int RESULT_HIDDEN = 3;

    public InputMethodManager() {
    }

    public boolean showSoftInput(View view, int flags) {
        if (view instanceof android.widget.EditText) {
            ((android.widget.EditText) view).startEditing();
            return true;
        }
        return false;
    }

    public boolean hideSoftInputFromWindow(Object windowToken, int flags) {
        Display d = Display.getInstance();
        com.codename1.ui.Form f = d.getCurrent();
        com.codename1.ui.Component focused = f == null ? null : f.getFocused();
        if (focused instanceof com.codename1.ui.TextArea && ((com.codename1.ui.TextArea) focused).isEditing()) {
            d.stopEditing(focused);
        }
        d.setShowVirtualKeyboard(false);
        return true;
    }

    public void toggleSoftInput(int showFlags, int hideFlags) {
        Display.getInstance().setShowVirtualKeyboard(!Display.getInstance().isVirtualKeyboardShowing());
    }

    public boolean isActive() {
        return Display.getInstance().isVirtualKeyboardShowing();
    }

    public boolean isActive(View view) {
        return isActive();
    }

    public boolean isAcceptingText() {
        return isActive();
    }

    public void restartInput(View view) {
    }
}
