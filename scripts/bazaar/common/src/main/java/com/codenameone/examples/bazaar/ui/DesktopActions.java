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

import java.util.HashMap;
import java.util.Map;

/// The actions of the desktop menu bar that only a feature screen knows how
/// to do: a new product, export, print.
///
/// The menu bar is installed once per home screen by [DesktopMenus], with
/// every item pointing here. A feature screen claims an action while it is on
/// screen, and gives it up when it leaves:
///
/// ```java
/// DesktopActions.set(DesktopActions.EXPORT_CSV, () -> exportProducts());
/// ...
/// DesktopActions.clear(DesktopActions.EXPORT_CSV);
/// ```
///
/// An action nobody claimed says so in a toast, rather than doing nothing.
public final class DesktopActions {
    public static final String NEW_PRODUCT = "newProduct";
    public static final String NEW_ORDER = "newOrder";
    public static final String OPEN_STORE = "openStore";
    public static final String EXPORT_CSV = "exportCsv";
    /// File > Import CSV: read records from a spreadsheet into the screen shown.
    public static final String IMPORT_CSV = "importCsv";
    public static final String PRINT = "print";
    public static final String FIND = "find";
    public static final String REFRESH = "refresh";
    public static final String HELP = "help";

    private static final Map<String, Runnable> HANDLERS = new HashMap<String, Runnable>();

    private DesktopActions() {
    }

    /// Makes `handler` what `action` does, replacing whoever had it.
    public static void set(String action, Runnable handler) {
        HANDLERS.put(action, handler);
    }

    public static void clear(String action) {
        HANDLERS.remove(action);
    }

    /// Whether some screen has claimed `action`.
    public static boolean has(String action) {
        return HANDLERS.get(action) != null;
    }

    /// Does `action`: what a menu item, a shortcut or a toolbar button calls.
    public static void run(String action) {
        Runnable handler = HANDLERS.get(action);
        if (handler != null) {
            handler.run();
        } else {
            Ui.say("Not available on this screen");
        }
    }
}
