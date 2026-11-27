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

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.ContextMenu;
import java.util.function.Supplier;

/// The right-click menu of a row: a secondary click, a stylus barrel button
/// or a long press opens the platform's context menu with the row's commands.
///
/// The commands are asked for when the menu opens, so a row whose state
/// changed offers what is true now:
///
/// ```java
/// RowMenus.attach(row, () -> new Command[] {
///     Command.create("Open in new window", null, e -> Windows.open(...)),
///     Command.create("Archive", null, e -> archive(product))});
/// ```
public final class RowMenus {
    private RowMenus() {
    }

    public static void attach(final Component row, final Supplier<Command[]> commands) {
        row.addContextMenuListener(e -> {
            Command[] items = commands.get();
            if (items != null && items.length > 0) {
                ContextMenu.show(row, e.getX(), e.getY(), items);
                e.consume();
            }
        });
    }

    /// A menu that is always the same.
    public static void attach(Component row, final Command... commands) {
        attach(row, () -> commands);
    }
}
