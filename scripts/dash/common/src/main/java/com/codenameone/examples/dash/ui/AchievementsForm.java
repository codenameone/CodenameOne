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
package com.codenameone.examples.dash.ui;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.dash.api.AchievementDto;
import com.codenameone.examples.dash.api.ProgressDto;

/// Every achievement, unlocked or still to go, with how far along each is.
public final class AchievementsForm {
    private AchievementsForm() {
    }

    public static void show() {
        final Form f = Ui.form("Trophy room", "achievements");
        Ui.back(f, Nav::home);
        final Container col = Ui.column();
        f.add(Ui.centeredColumn(col));
        Rewards.refresh(() -> {
            ProgressDto p = Rewards.progress();
            if (p == null) {
                col.add(Ui.label("Could not load your progress.", "Empty"));
            } else {
                int done = 0;
                for (AchievementDto a : p.achievements()) {
                    done += a.unlocked() ? 1 : 0;
                }
                col.add(Ui.label(done + " of " + p.achievements().size() + " unlocked", "ArenaName"));
                for (AchievementDto a : p.achievements()) {
                    col.add(row(a));
                }
            }
            f.revalidate();
        });
        f.show();
    }

    private static Container row(AchievementDto a) {
        Container c = new Container(new BorderLayout());
        c.setUIID(a.unlocked() ? "AchievementRow" : "AchievementLocked");
        c.setName("achievement-" + a.id());
        Label icon = new Label("", "Container");
        icon.setIcon(FontImage.createMaterial(a.unlocked() ? FontImage.MATERIAL_MILITARY_TECH : FontImage.MATERIAL_LOCK,
                a.unlocked() ? "RewardAchievement" : "XpText", 7));
        c.add(BorderLayout.WEST, icon);
        Container text = new Container(BoxLayout.y());
        text.add(new Label(a.title(), "QuestTitle"));
        text.add(new Label(a.detail(), "XpText"));
        Container bar = new Container(new BorderLayout());
        bar.add(BorderLayout.CENTER, Ui.bar(a.progress(), a.goal(), a.unlocked() ? 0xffd34d : 0x7a849e));
        bar.add(BorderLayout.EAST, new Label(" " + a.progress() + "/" + a.goal(), "XpText"));
        text.add(bar);
        c.add(BorderLayout.CENTER, text);
        return c;
    }
}
