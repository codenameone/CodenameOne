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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.skirmish.api.LeaderDto;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;

/// The top players by trophies.
public final class LeaderboardForm {
    private LeaderboardForm() {
    }

    public static void show() {
        final Form f = Ui.form("Leaders", "leaders");
        Ui.back(f, Nav::home);
        Api.game().leaderboard(50, Net.to(list -> Ui.later(() -> {
            for (LeaderDto l : list) {
                Container row = new Container(new BorderLayout());
                row.setUIID(l.rank() <= 3 ? "LeaderTop" : "LeaderRow");
                row.add(BorderLayout.WEST, new Label("#" + l.rank(), "LeaderRank"));
                row.add(BorderLayout.CENTER, new Label(l.displayName(), "LeaderName"));
                row.add(BorderLayout.EAST, new Label(String.valueOf(l.trophies()), "Trophies"));
                f.add(row);
            }
            f.revalidate();
        }), Ui::failed));
        f.show();
    }
}
