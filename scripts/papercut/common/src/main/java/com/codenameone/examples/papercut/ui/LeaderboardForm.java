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
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.papercut.api.DailyDto;
import com.codenameone.examples.papercut.api.LeaderDto;
import com.codenameone.examples.papercut.net.Api;
import com.codenameone.examples.papercut.play.Arena;
import com.codenameone.examples.papercut.play.Hud;
import java.util.List;

/// The boards: a mode (Classic, Fold, Today's sheet) by a period (all time,
/// this week, today, friends). The daily board is always today's.
public final class LeaderboardForm {
    private static final String[] MODE_LABELS = {"Classic", "Fold", "Today's sheet"};
    private static final String[] PERIODS = {"ALL", "WEEK", "TODAY", "FRIENDS"};
    private static final String[] PERIOD_LABELS = {"All time", "Week", "Today", "Friends"};

    private LeaderboardForm() {
    }

    public static void show() {
        Form f = Ui.form("Boards", "boards");
        Ui.desktopColumn(f);
        Ui.back(f, Nav::home);
        final Container list = new Container(BoxLayout.y());
        list.setName("boardRows");
        final int[] pick = {Arena.CLASSIC, 0};
        Container modes = segmented(MODE_LABELS, i -> {
            pick[0] = i;
            load(list, pick[0], pick[1]);
        });
        Container periods = segmented(PERIOD_LABELS, i -> {
            pick[1] = i;
            load(list, pick[0], pick[1]);
        });
        f.add(modes).add(periods).add(list);
        load(list, pick[0], pick[1]);
        f.show();
    }

    interface Pick {
        void picked(int index);
    }

    static Container segmented(String[] labels, final Pick pick) {
        Container c = new Container(new GridLayout(1, labels.length));
        ButtonGroup g = new ButtonGroup();
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            RadioButton b = RadioButton.createToggle(labels[i], g);
            b.setUIID(i == 0 ? "TabSelected" : "Tab");
            b.addActionListener(e -> {
                for (int k = 0; k < c.getComponentCount(); k++) {
                    c.getComponentAt(k).setUIID(k == index ? "TabSelected" : "Tab");
                }
                c.repaint();
                pick.picked(index);
            });
            c.add(b);
        }
        g.setSelected(0);
        return c;
    }

    static void load(final Container list, int mode, int period) {
        list.removeAll();
        list.add(new Label("Reading the board...", "Muted"));
        list.revalidate();
        if (mode == Arena.DAILY) {
            Api.runs().daily(Net.to(d -> Ui.later(() -> dailyHeader(list, d)), Ui::failed));
        }
        Api.runs().leaderboard(Runs.modeName(mode), mode == Arena.DAILY ? "TODAY" : PERIODS[period],
                Net.to(rows -> Ui.later(() -> fill(list, rows)), Ui::failed));
    }

    private static void dailyHeader(Container list, DailyDto d) {
        Container card = new Container(BoxLayout.y(), "Card");
        card.add(new Label("Sheet of " + d.date(), "CardTitle"));
        card.add(new Label(d.players() + " players  -  top " + Hud.format(d.topScore())
                + (d.topName() == null ? "" : " by " + d.topName()), "CardText"));
        card.add(new Label("Your best today: " + Hud.format(d.myBest()), "Accent"));
        Button go = Ui.primary("Play today's sheet", "playDailyFromBoard", e -> Nav.daily());
        card.add(go);
        list.addComponent(0, card);
        list.revalidate();
    }

    private static void fill(Container list, List<LeaderDto> rows) {
        for (int i = list.getComponentCount() - 1; i >= 0; i--) {
            if (list.getComponentAt(i) instanceof Label) {
                list.removeComponent(list.getComponentAt(i));
            }
        }
        if (rows.isEmpty()) {
            list.add(new Label("Nobody yet. Be the first.", "Muted"));
        }
        for (LeaderDto r : rows) {
            Container row = new Container(new BorderLayout(), r.me() ? "RowMe" : "Row");
            row.add(BorderLayout.WEST, new Label("#" + r.rank(), "Rank"));
            row.add(BorderLayout.CENTER, new Label(r.displayName()));
            row.add(BorderLayout.EAST, new Label(Hud.format(r.score()), "Accent"));
            list.add(row);
        }
        list.revalidate();
    }
}
