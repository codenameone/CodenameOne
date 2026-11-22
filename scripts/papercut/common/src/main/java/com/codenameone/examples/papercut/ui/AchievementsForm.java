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
package com.codenameone.examples.papercut.ui;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.papercut.api.AchievementDto;
import com.codenameone.examples.papercut.net.Api;
import java.util.List;

/// Goals, worked out by the server from the runs it accepted, so they cannot
/// be unlocked by a run it refused.
public final class AchievementsForm {
    private AchievementsForm() {
    }

    public static void show() {
        Form f = Ui.form("Achievements", "achievements");
        Ui.desktopColumn(f);
        Ui.back(f, Nav::home);
        final Container list = new Container(BoxLayout.y());
        f.add(list);
        Api.progress().achievements(Net.to(rows -> Ui.later(() -> fill(list, rows)), Ui::failed));
        f.show();
    }

    private static void fill(Container list, List<AchievementDto> rows) {
        for (AchievementDto a : rows) {
            Container row = new Container(new BorderLayout(), a.unlocked() ? "RowMe" : "Row");
            Label icon = new Label("", "Accent");
            FontImage.setMaterialIcon(icon, a.unlocked() ? FontImage.MATERIAL_EMOJI_EVENTS
                    : FontImage.MATERIAL_LOCK_OUTLINE, 5);
            row.add(BorderLayout.WEST, icon);
            Container text = BoxLayout.encloseY(new Label(a.name()), new Label(a.description(), "Muted"));
            row.add(BorderLayout.CENTER, text);
            if (!a.unlocked() && a.goal() > 0) {
                row.add(BorderLayout.SOUTH, Ui.bar((int) Math.min(a.progress(), a.goal()),
                        (int) a.goal(), 0xd4483c));
            }
            list.add(row);
        }
        list.revalidate();
    }
}
