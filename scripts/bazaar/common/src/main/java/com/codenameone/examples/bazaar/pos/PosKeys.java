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
package com.codenameone.examples.bazaar.pos;

import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codenameone.examples.bazaar.ui.DesktopMenus;
import com.codenameone.examples.bazaar.ui.Lang;
import java.util.HashMap;
import java.util.Map;

/// The register's keyboard: a Sale menu on a desktop with Pay, Clear sale and
/// one tender per key. Cmd/Ctrl+1..9 already open the sections, so tenders
/// take Cmd/Ctrl+Shift+1..4. Enter in the scan field pays (see [SellScreen])
/// and Escape clears.
///
/// The commands go on the home form once and call whatever handler the
/// screen on top set last, so a screen that is rebuilt never adds them twice.
final class PosKeys {
    static final String PAY = "pay";
    static final String CLEAR = "clear";
    static final String CASH = "cash";
    static final String CARD = "card";
    static final String GIFT = "gift";
    static final String CREDIT = "credit";

    private static final Map<String, Runnable> HANDLERS = new HashMap<String, Runnable>();
    private static final int ESCAPE = 27;

    private PosKeys() {
    }

    static void set(String action, Runnable handler) {
        HANDLERS.put(action, handler);
    }

    static void clear(String action) {
        HANDLERS.remove(action);
    }

    static void run(String action) {
        Runnable r = HANDLERS.get(action);
        if (r != null) {
            r.run();
        }
    }

    /// Adds the Sale menu to `form` unless it has it.
    static void install(Form form) {
        if (form == null || form.getClientProperty("posKeys") != null) {
            return;
        }
        form.putClientProperty("posKeys", Boolean.TRUE);
        form.addKeyListener(ESCAPE, e -> run(CLEAR));
        if (!CN.isDesktop() || !DesktopMenus.nativeBar(form)) {
            return;
        }
        add(form, "Pay", PAY, 'p', 0);
        add(form, "Clear sale", CLEAR, 'k', 0);
        add(form, "Cash", CASH, '1', Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
        add(form, "Card", CARD, '2', Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
        add(form, "Gift card", GIFT, '3', Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
        add(form, "Store credit", CREDIT, '4', Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
    }

    private static void add(Form form, String name, final String action, char key, int extra) {
        Command c = Command.create(Lang.tr(name), null, e -> run(action));
        c.setDesktopMenu(Lang.tr("Sale"));
        c.setDesktopShortcut(key, Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY | extra);
        form.getToolbar().addCommandToSideMenu(c);
    }
}
