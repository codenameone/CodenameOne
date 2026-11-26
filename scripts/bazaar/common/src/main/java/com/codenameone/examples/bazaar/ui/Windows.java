/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Desktop;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

/// A second native window, where the platform has a windowing system: an
/// order opened beside the list, the customer-facing display of a register.
///
/// Elsewhere -- a phone, a tablet, a browser -- there is no second window, and
/// the caller shows the same content another way (a form, a sheet).
public final class Windows {
    private Windows() {
    }

    /// Whether [#open] can open a window here.
    public static boolean supported() {
        return Desktop.isSupported();
    }

    /// Opens `content` in a window of its own, `widthMm` by `heightMm`.
    ///
    /// @return the window, or null where windows are not supported
    public static Window open(String title, Component content, float widthMm, float heightMm) {
        if (!supported()) {
            return null;
        }
        Window window = new Window(Lang.tr(title), new BorderLayout());
        window.getContentPane().setUIID("SvForm");
        window.add(BorderLayout.CENTER, content);
        window.setWindowSize(CN.convertToPixels(widthMm), CN.convertToPixels(heightMm));
        Layouts.dress(window.getContentPane());
        window.show();
        return window;
    }
}
