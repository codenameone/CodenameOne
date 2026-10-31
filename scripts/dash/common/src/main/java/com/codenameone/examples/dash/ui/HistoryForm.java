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
import com.codename1.ui.Form;
import com.codenameone.examples.dash.api.RaceSummaryDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;

/// The player's recent races, each one a replay away.
public final class HistoryForm {
    private HistoryForm() {
    }

    public static void show() {
        final Form f = Ui.form("Replays", "history");
        Ui.back(f, Nav::home);
        Api.game().history(Net.to(list -> Ui.later(() -> {
            if (list.isEmpty()) {
                f.add(Ui.text("No races yet. Your live races appear here, ready to replay.", "Empty"));
            }
            for (final RaceSummaryDto r : list) {
                MultiButton b = new MultiButton(place(r.place()) + " of " + r.runners() + "   " + r.score() + " pts");
                b.setUIID("HistoryRow");
                b.setUIIDLine1(r.place() == 0 ? "HistoryWin" : r.place() * 2 < r.runners() ? "HistoryDraw"
                        : "HistoryLoss");
                b.setTextLine2(r.level() + "   " + (r.trophyChange() > 0 ? "+" : "") + r.trophyChange()
                        + " trophies");
                b.setName("race-" + r.id());
                b.addActionListener(e -> ReplayForm.load(r.id()));
                f.add(b);
            }
            f.revalidate();
        }), Ui::failed));
        f.show();
    }

    /// `place` counted from 0, as a word: 1st, 2nd, 3rd...
    public static String place(int place) {
        int n = place + 1;
        int tens = n % 100;
        if (tens >= 11 && tens <= 13) {
            return n + "th";
        }
        switch (n % 10) {
            case 1:
                return n + "st";
            case 2:
                return n + "nd";
            case 3:
                return n + "rd";
            default:
                return n + "th";
        }
    }
}
