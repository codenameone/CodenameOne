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
package com.codenameone.examples.fieldline.dispatch;

import com.codename1.ui.Component;
import com.codename1.ui.Form;

/// Where a page of the back office shows what was picked from its list.
///
/// The pages are written once. In the console the list stays in view and the
/// detail is shown beside it; on a phone each is a screen of its own, reached
/// from the list and left with the back arrow.
interface Host {
    /// Shows `content` as what was picked, in place of whatever was.
    void detail(String title, String name, Component content);

    /// Marks `row` as the one that is open, where the list stays in view.
    void picked(Component row);

    /// Back to the list alone, which is fetched again.
    void reload();

    /// The screen all of this is on.
    Form form();
}
