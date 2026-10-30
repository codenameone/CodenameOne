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

import com.codename1.components.MultiButton;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.RadioButton;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.dash.api.CrewDto;
import com.codenameone.examples.dash.api.ScoreDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;

/// A level's leaderboards. Every score on them was re-simulated by the server
/// from the inputs alone, so a board shows runs that really happened.
///
/// `all` takes any seed. `week` counts only runs on the week's seed for the
/// level, so everyone met the same track; `day` is the daily dash. `friends`
/// is the player, the friends they invited and their crew mates; a crew's
/// own board is `crew:<code>`. A row opens the run's replay.
public final class ScoresForm {
    private static final String[] BOARDS = {"all", "week", "day", "friends"};
    private static final String[] LABELS = {"All time", "Week", "Today", "Friends"};

    private ScoresForm() {
    }

    /// `level` is an official id or `pack/level`.
    public static void show(final String level, final Runnable back) {
        final Form f = Ui.form("Scores", "scores");
        f.setLayout(new BorderLayout());
        Ui.back(f, back == null ? Nav::home : back);
        final Container rows = new Container(BoxLayout.y());
        rows.setScrollableY(true);
        Container tabs = new Container(new GridLayout(1, BOARDS.length));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < BOARDS.length; i++) {
            final String board = BOARDS[i];
            RadioButton b = RadioButton.createToggle(LABELS[i], group);
            b.setUIID("BoardTab");
            b.setName("board-" + board);
            b.addActionListener(e -> load(f, rows, level, board));
            tabs.add(b);
            if (i == 0) {
                b.setSelected(true);
            }
        }
        Container north = BoxLayout.encloseY(tabs);
        Api.game().crews(Net.to(crews -> Ui.later(() -> {
            for (final CrewDto c : crews) {
                Button b = Ui.button(c.name(), "BoardTab", "board-crew-" + c.code(),
                        e -> load(f, rows, level, "crew:" + c.code()));
                north.add(b);
            }
            f.revalidate();
        }), (s, m) -> { }));
        f.add(BorderLayout.NORTH, north);
        f.add(BorderLayout.CENTER, rows);
        load(f, rows, level, "all");
        f.show();
    }

    private static void load(final Form f, final Container rows, String level, String board) {
        Api.game().scores(level, board, 50, Net.to(list -> Ui.later(() -> {
            rows.removeAll();
            if (list.isEmpty()) {
                rows.add(Ui.text("No runs on this board yet. Be the first.", "Empty"));
            }
            for (final ScoreDto s : list) {
                MultiButton b = new MultiButton("#" + s.rank() + "  " + s.displayName());
                b.setUIID(s.rank() <= 3 ? "LeaderTop" : "LeaderRow");
                b.setTextLine2(s.score() + " pts   " + s.distance() + " m   " + s.coins() + " coins   "
                        + LevelsForm.starText(s.stars()));
                b.setName("score-" + s.rank());
                if (s.replayId() != null && s.replayId().length() > 0) {
                    b.addActionListener(e -> Api.game().replay(s.replayId(), Net.to(
                            r -> Ui.later(() -> ReplayForm.open(r, () -> f.showBack())), Ui::failed)));
                }
                rows.add(b);
            }
            f.revalidate();
        }), Ui::failed));
    }
}
