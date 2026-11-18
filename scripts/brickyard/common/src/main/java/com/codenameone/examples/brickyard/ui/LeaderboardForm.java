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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.LeaderDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;

import com.codename1.ui.ComboBox;
import com.codename1.ui.list.DefaultListModel;
import java.util.List;

/// An experience's leaderboard for a stat picked from those it has entries
/// for: daily, weekly and all-time tabs, each either everyone or only friends.
public final class LeaderboardForm {
    /// The stat the starter templates write; the default when an experience
    /// has no entries yet.
    static final String STAT = "score";
    static final String[] PERIODS = {"daily", "weekly", "all"};
    static final String[] TITLES = {"Today", "This week", "All time"};

    private LeaderboardForm() {
    }

    public static void show(final String experienceId, String title) {
        Form f = Ui.form(title, "leaderboard");
        Ui.back(f, () -> ExperienceForm.show(experienceId));
        f.setLayout(new BorderLayout());
        f.setScrollableY(false);
        final CheckBox friends = new CheckBox("Friends only");
        friends.setName("friendsOnly");
        friends.setUIID("Body");
        final ComboBox<String> stat = new ComboBox<String>(STAT);
        stat.setName("statPicker");
        final Tabs tabs = new Tabs();
        final Container[] lists = new Container[PERIODS.length];
        for (int i = 0; i < PERIODS.length; i++) {
            lists[i] = new Container(BoxLayout.y());
            lists[i].setScrollableY(true);
            lists[i].setName("board" + PERIODS[i]);
            tabs.addTab(TITLES[i], lists[i]);
        }
        Runnable load = () -> {
            int i = tabs.getSelectedIndex();
            Object picked = stat.getSelectedItem();
            load(experienceId, picked == null ? STAT : picked.toString(), PERIODS[i], friends.isSelected(),
                    lists[i]);
        };
        tabs.addSelectionListener((old, now) -> load.run());
        friends.addActionListener(e -> load.run());
        stat.addActionListener(e -> load.run());
        f.add(BorderLayout.NORTH, BorderLayout.centerEastWest(stat, friends, null));
        f.add(BorderLayout.CENTER, tabs);
        f.show();
        // The stats this experience really has entries for.
        Api.leaderboards().stats(experienceId, Net.to(found -> {
            List<String> names = new java.util.ArrayList<String>();
            if (found != null) {
                for (com.codenameone.examples.brickyard.api.StatDto d : found) {
                    names.add(d.name());
                }
            }
            if (!names.isEmpty()) {
                stat.setModel(new DefaultListModel<String>(names));
                stat.setSelectedIndex(0);
                f.revalidate();
            }
            load.run();
        }, (status, message) -> load.run()));
    }

    static void load(String experienceId, String stat, String period, boolean friends, final Container list) {
        list.removeAll();
        list.add(Ui.label("Loading...", "Muted"));
        list.revalidate();
        Net.Ok<List<LeaderDto>> ok = rows -> {
            list.removeAll();
            if (rows == null || rows.isEmpty()) {
                list.add(Ui.label("No scores yet. Be the first.", "Muted"));
            } else {
                for (LeaderDto r : rows) {
                    Container row = com.codename1.ui.layouts.BorderLayout.centerEastWest(
                            Ui.person(r.name(), "CardTitle"), Ui.label(String.valueOf(r.value()), "RankValue"),
                            Ui.label("#" + r.rank(), "RankNumber"));
                    row.setUIID("Panel");
                    list.add(row);
                }
            }
            list.revalidate();
        };
        Net.Failed failed = (status, message) -> {
            list.removeAll();
            list.add(Ui.text(message, "Muted"));
            list.revalidate();
        };
        if (friends) {
            Api.leaderboards().friends(experienceId, stat, period, Net.to(ok, failed));
        } else {
            Api.leaderboards().top(experienceId, stat, period, 50, Net.to(ok, failed));
        }
    }
}
