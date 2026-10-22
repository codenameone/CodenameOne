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
package com.codenameone.examples.skirmish.ui;

import com.codename1.components.MultiButton;
import com.codename1.ui.Form;
import com.codenameone.examples.skirmish.api.MatchSummaryDto;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;

/// The player's recent battles, each one a replay away.
public final class HistoryForm {
    private HistoryForm() {
    }

    public static void show() {
        final Form f = Ui.form("Replays", "history");
        Ui.back(f, Nav::home);
        Api.game().history(Net.to(list -> Ui.later(() -> {
            if (list.isEmpty()) {
                f.add(Ui.text("No battles yet. Your online battles appear here, ready to replay.", "Empty"));
            }
            for (final MatchSummaryDto m : list) {
                MultiButton b = new MultiButton(outcome(m.result()) + "  " + m.myCrowns() + " - " + m.theirCrowns());
                b.setUIID("HistoryRow");
                b.setUIIDLine1(m.result() > 0 ? "HistoryWin" : m.result() < 0 ? "HistoryLoss" : "HistoryDraw");
                b.setTextLine2("vs " + m.opponentName() + "   " + (m.trophyChange() > 0 ? "+" : "")
                        + m.trophyChange() + " trophies");
                b.setName("match-" + m.id());
                b.addActionListener(e -> ReplayForm.load(m.id()));
                f.add(b);
            }
            f.revalidate();
        }), Ui::failed));
        f.show();
    }

    static String outcome(int result) {
        return result > 0 ? "Victory" : result < 0 ? "Defeat" : "Draw";
    }
}
