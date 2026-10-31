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
package com.codenameone.examples.dash.forge;

import com.codename1.components.MultiButton;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.dash.api.PackSummaryDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.ui.Nav;
import com.codenameone.examples.dash.ui.Ui;

/// The track editor's front door: the game's own levels, what other players
/// made, and the player's own packs, with a button to start a new one.
public final class ForgeHome {
    private static final String[] SCOPES = {"official", "popular", "new", "mine"};
    private static final String[] TITLES = {"Official", "Popular", "New", "Yours"};

    private ForgeHome() {
    }

    public static void show() {
        show("official");
    }

    static void show(final String scope) {
        final Form f = Ui.form("Forge", "forge");
        Ui.back(f, Nav::home);
        Container tabs = new Container(new GridLayout(1, SCOPES.length));
        for (int i = 0; i < SCOPES.length; i++) {
            final String s = SCOPES[i];
            Button b = Ui.button(TITLES[i], s.equals(scope) ? "ForgeTabOn" : "ForgeTab", "forge-" + s,
                    e -> show(s));
            tabs.add(b);
        }
        f.add(tabs);
        f.add(Ui.primary("Create a pack", "forgeCreate", e -> PackEditor.create()));
        Api.forge().packs(scope, Net.to(list -> Ui.later(() -> {
            if (list.isEmpty()) {
                f.add(Ui.text("mine".equals(scope)
                        ? "Nothing yet. A pack holds your own levels and obstacles; create one and "
                        + "share it with friends."
                        : "No packs here yet.", "Empty"));
            }
            for (final PackSummaryDto p : list) {
                f.add(row(p, "mine".equals(scope)));
            }
            f.revalidate();
        }), Ui::failed));
        f.show();
    }

    private static MultiButton row(final PackSummaryDto p, boolean mine) {
        MultiButton b = new MultiButton(p.name().length() == 0 ? p.id() : p.name());
        b.setUIID("HistoryRow");
        b.setUIIDLine1(p.official() ? "ForgeOfficial" : "HistoryDraw");
        StringBuilder line = new StringBuilder("by ").append(p.author()).append("  ")
                .append(p.levels()).append(p.levels() == 1 ? " level" : " levels");
        if (p.ratings() > 0) {
            line.append("  ").append(p.rating() / 10).append('.').append(p.rating() % 10).append(" stars");
        }
        b.setTextLine2(line.toString());
        if (mine) {
            b.setTextLine3(p.version() == 0 ? "Draft, not published" : "Version " + p.version() + ", "
                    + p.status() + (p.pendingAssets() ? ", files waiting for review" : ""));
        }
        b.setName("pack-" + p.id());
        b.addActionListener(e -> {
            if (mine && p.version() == 0) {
                PackEditor.open(p.id());
            } else {
                PackScreen.show(p.id());
            }
        });
        return b;
    }
}
