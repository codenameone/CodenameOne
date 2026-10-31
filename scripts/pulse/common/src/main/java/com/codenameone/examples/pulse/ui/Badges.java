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
package com.codenameone.examples.pulse.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import java.util.List;

/// The badges a member can earn, as the app shows them. The server decides
/// which are earned (`Points` in the engagement package); this only says what
/// each looks like and how it is earned, so a badge the server adds later
/// shows with a generic medal until this learns about it.
public final class Badges {
    /// Every badge, in the server's order, with its icon, name and how it is
    /// earned.
    static final String[] IDS = {"FIRST_CLASS", "TEN_CLASSES", "FIFTY_CLASSES", "STREAK_4",
        "STREAK_12", "HEART_TRACKED", "STRAPPED_IN", "ZONE_5", "TEN_HOURS"};
    static final char[] ICONS = {FontImage.MATERIAL_FLAG, FontImage.MATERIAL_LOOKS_ONE,
        FontImage.MATERIAL_MILITARY_TECH, FontImage.MATERIAL_LOCAL_FIRE_DEPARTMENT,
        FontImage.MATERIAL_WHATSHOT, FontImage.MATERIAL_FAVORITE,
        FontImage.MATERIAL_MONITOR_HEART, FontImage.MATERIAL_BOLT,
        FontImage.MATERIAL_TIMER};
    static final String[] NAMES = {"First class", "Ten classes", "Fifty classes",
        "Four-week streak", "Twelve-week streak", "Heart tracked", "Strapped in", "Zone 5",
        "Ten hours"};
    static final String[] HOW = {"Come to your first class.", "Come to ten classes.",
        "Come to fifty classes.", "Train every week for four weeks running.",
        "Train every week for twelve weeks running.",
        "Record a workout with your heart rate.",
        "Wear a heart-rate strap or a watch in a class.",
        "Spend a minute flat out, in zone 5.", "Record ten hours of workouts."};

    private Badges() {
    }

    /// A row of every badge, earned ones in colour and the rest greyed out,
    /// each opening a sheet that says how it is earned.
    public static Container shelf(List<String> earned) {
        Container row = new Container(BoxLayout.x());
        row.setUIID("SvShelf");
        row.setScrollableX(true);
        row.setScrollVisible(false);
        row.setName("badges");
        for (int iter = 0; iter < IDS.length; iter++) {
            row.add(medal(iter, earned != null && earned.contains(IDS[iter])));
        }
        return row;
    }

    /// How many badges there are to earn.
    public static int total() {
        return IDS.length;
    }

    /// How many of the badges are earned.
    public static int count(List<String> earned) {
        int count = 0;
        for (int iter = 0; earned != null && iter < IDS.length; iter++) {
            if (earned.contains(IDS[iter])) {
                count++;
            }
        }
        return count;
    }

    /// Whether a badge is earned.
    public static boolean has(List<String> earned, String id) {
        return earned != null && earned.contains(id);
    }

    private static Container medal(final int index, final boolean earned) {
        Button disc = new Button("", earned ? "SvMedal" : "SvMedalLocked");
        Ui.icon(disc, earned ? ICONS[index] : FontImage.MATERIAL_LOCK, 4.4f);
        disc.setName("badge-" + IDS[index]);
        disc.getAllStyles().setAlignment(Label.CENTER);
        round(disc);
        String name = Lang.tr(NAMES[index]);
        disc.setAccessibilityText(earned ? name : Lang.tr("{0}, not earned yet", name));
        disc.addActionListener(e -> explain(index, earned));
        Container cell = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        cell.setUIID("SvMedalCell");
        cell.add(FlowLayout.encloseCenter(disc));
        Label caption = new Label(name, earned ? "SvMedalName" : "SvMedalNameLocked");
        caption.getAllStyles().setAlignment(Label.CENTER);
        cell.add(caption);
        return cell;
    }

    // A circle in the style's own background colour, whatever the icon's size.
    private static void round(Component disc) {
        disc.getAllStyles().setBorder(RoundBorder.create()
                .color(disc.getUnselectedStyle().getBgColor()).rectangle(false));
    }

    private static void explain(int index, boolean earned) {
        Container body = new Container(BoxLayout.y());
        Label disc = new Label("", earned ? "SvMedal" : "SvMedalLocked");
        Ui.icon(disc, ICONS[index], 6f);
        round(disc);
        disc.getAllStyles().setAlignment(Label.CENTER);
        body.add(FlowLayout.encloseCenter(disc));
        Label title = new Label(Lang.tr(NAMES[index]), "SvHeading");
        title.getAllStyles().setAlignment(Label.CENTER);
        body.add(title);
        SpanLabel how = new SpanLabel(Lang.tr(HOW[index]), "SvMuted");
        how.setTextBlockAlign(Label.CENTER);
        body.add(how);
        body.add(Ui.badge(earned ? Lang.tr("Earned") : Lang.tr("Not earned yet"),
                earned ? "SvBadgeGood" : "SvBadge"));
        Sheet sheet = new Sheet(null, "");
        sheet.getContentPane().setLayout(new BorderLayout());
        sheet.getContentPane().add(BorderLayout.CENTER, body);
        sheet.show();
    }
}
