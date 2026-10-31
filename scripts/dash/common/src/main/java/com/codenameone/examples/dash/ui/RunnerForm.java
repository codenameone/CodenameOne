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

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.api.RunnerDto;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;

/// The runner the player races as. One is free; the shop sells the others.
/// Cosmetic only: every runner runs by the same rules.
public final class RunnerForm {
    /// Every runner, in the order the screen shows them.
    static final String[] RUNNERS = {"runner-dash", "runner-nova", "runner-kite"};
    static final String[] NAMES = {"Dash", "Nova", "Kite"};

    private RunnerForm() {
    }

    public static void show() {
        final Form f = Ui.form("Runner", "runner");
        Ui.back(f, Nav::home);
        final Container grid = new Container(new GridLayout(1, RUNNERS.length));
        f.add(Ui.centeredColumn(Ui.column(grid)));
        fill(f, grid);
        f.show();
    }

    private static void fill(final Form f, final Container grid) {
        grid.removeAll();
        PlayerDto p = Session.user();
        String current = p == null || p.runner() == null ? RUNNERS[0] : p.runner();
        for (int i = 0; i < RUNNERS.length; i++) {
            final String id = RUNNERS[i];
            boolean owned = i == 0 || p != null && p.owned() != null && p.owned().contains(id);
            Container card = new Container(new BorderLayout());
            card.setUIID(id.equals(current) ? "CardSlotSelected" : "CardSlot");
            card.setName(id);
            card.add(BorderLayout.CENTER, new Label(Portraits.figure(Portraits.of(id), CN.convertToPixels(22)),
                    "Container"));
            Container under = new Container(BoxLayout.y());
            under.add(new Label(NAMES[i], "QuestTitle"));
            if (id.equals(current)) {
                under.add(new Label("Running", "Claimed"));
            } else if (owned) {
                under.add(Ui.button("Choose", "ClaimButton", "choose-" + id, e -> choose(f, grid, id)));
            } else {
                under.add(Ui.button("Shop", "CardAction", "shop-" + id, e -> ShopForm.show()));
            }
            card.add(BorderLayout.SOUTH, under);
            grid.add(card);
        }
        f.revalidate();
    }

    private static void choose(final Form f, final Container grid, final String id) {
        Api.game().saveRunner(new RunnerDto(id), Net.to(r -> Session.refreshUser(() -> Ui.later(() -> {
            Telemetry.runnerChosen(id);
            fill(f, grid);
        }), Ui::failed), Ui::failed));
    }
}
