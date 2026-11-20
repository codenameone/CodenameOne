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
package com.codenameone.examples.bazaar.admin;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// [Panes] inside a section of the shell.
///
/// The list fills the section. On a wide screen what is picked from it is
/// shown beside it, with a bar to go back through what was opened on top; on
/// a phone each of those is a screen of its own over the home screen.
final class EmbeddedPanes extends Panes {
    private static final float MASTER_MM = 100f;

    private final Container host = new Container(new BorderLayout());
    private final Container detail = new Container(new BorderLayout());
    private final boolean wide = Layouts.wide();
    private final List<Object[]> open = new ArrayList<Object[]>();
    private final List<Form> screens = new ArrayList<Form>();
    private Runnable shown;
    private Component picked;

    /// The section's content, for the shell to place.
    Container host() {
        return host;
    }

    @Override
    void list(String title, String name, Component content, Runnable shown) {
        this.shown = shown;
        open.clear();
        picked = null;
        host.removeAll();
        if (wide) {
            Container master = new Container(new BorderLayout()) {
                @Override
                protected Dimension calcPreferredSize() {
                    Dimension size = super.calcPreferredSize();
                    size.setWidth(Math.min(CN.convertToPixels(MASTER_MM),
                            host.getWidth() > 0 ? host.getWidth() / 2 : Integer.MAX_VALUE));
                    return size;
                }
            };
            master.setUIID("SvMaster");
            master.setName(name);
            master.add(BorderLayout.CENTER, content);
            host.add(BorderLayout.WEST, master);
            host.add(BorderLayout.CENTER, detail);
            showDetail();
        } else {
            content.setName(name);
            host.add(BorderLayout.CENTER, content);
        }
        shown.run();
    }

    @Override
    void page(String title, String name, Component content) {
        list(title, name, content, () -> { });
    }

    @Override
    void detail(String title, String name, Component content) {
        if (wide) {
            open.clear();
        } else {
            screens.clear();
        }
        over(title, name, content);
    }

    @Override
    void over(String title, String name, Component content) {
        if (wide) {
            open.add(new Object[] {title, name, content});
            showDetail();
            return;
        }
        Form form = Ui.form(title, name);
        Ui.back(form, this::back);
        form.add(BorderLayout.CENTER, content);
        Layouts.dress(form);
        screens.add(form);
        form.show();
    }

    @Override
    void back() {
        if (wide) {
            if (!open.isEmpty()) {
                open.remove(open.size() - 1);
            }
            showDetail();
            return;
        }
        if (!screens.isEmpty()) {
            screens.remove(screens.size() - 1);
        }
        form().showBack();
    }

    @Override
    void home() {
        if (wide) {
            open.clear();
            showDetail();
        } else {
            screens.clear();
            form().showBack();
        }
        if (shown != null) {
            shown.run();
        }
    }

    @Override
    void refresh() {
        Form form = form();
        if (form != null) {
            Layouts.dress(form);
            Ui.refresh(form);
        }
    }

    @Override
    Form form() {
        if (!screens.isEmpty()) {
            return screens.get(screens.size() - 1);
        }
        return host.getComponentForm() != null ? host.getComponentForm() : CN.getCurrentForm();
    }

    @Override
    void picked(Component row) {
        if (!wide) {
            return;
        }
        if (picked != null) {
            picked.setUIID("SvRow");
        }
        picked = row;
        row.setUIID("SvRowPicked");
    }

    private void showDetail() {
        detail.removeAll();
        if (open.isEmpty()) {
            Container none = Ui.empty(FontImage.MATERIAL_TOUCH_APP, "Nothing selected",
                    "Pick a row from the list to see it here.");
            none.setName("nothing");
            detail.add(BorderLayout.CENTER, none);
        } else {
            Object[] top = open.get(open.size() - 1);
            Ui.viewed((String) top[1]);
            Container bar = new Container(new BorderLayout());
            bar.setUIID("SvPaneBar");
            if (open.size() > 1) {
                Button back = Ui.flat(FontImage.MATERIAL_ARROW_BACK, "pane-back", e -> back());
                bar.add(BorderLayout.WEST, back);
            }
            bar.add(BorderLayout.CENTER, Ui.label((String) top[0], "SvPaneTitle"));
            Container holder = new Container(new BorderLayout());
            holder.setName((String) top[1]);
            Component content = (Component) top[2];
            content.remove();
            holder.add(BorderLayout.CENTER, content);
            detail.add(BorderLayout.NORTH, bar);
            detail.add(BorderLayout.CENTER, holder);
        }
        refresh();
    }
}
