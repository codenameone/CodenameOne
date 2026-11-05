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
package com.codenameone.examples.splat.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.splat.api.PlayerDto;
import com.codenameone.examples.splat.api.RunnerDto;
import com.codenameone.examples.splat.engage.Telemetry;
import com.codenameone.examples.splat.net.Api;
import com.codenameone.examples.splat.net.Net;
import com.codenameone.examples.splat.net.Session;
import com.codenameone.examples.splat.play.OperatorStage;
import com.codenameone.examples.splat.rules.Catalog;
import com.codenameone.examples.splat.rules.MarkerDef;

/// The operator screen: your operator on the right holding what you carry,
/// the primary slot down the left with every marker and, for the one in
/// view, its numbers as bars. Three side-grades, none better: the Purist is
/// one accurate shot at a time, the Tactical Office Supply the all-rounder,
/// the Ramping Dad a wall of paint that goes everywhere except where you aim.
// tag::splat-loadout[]
public final class LoadoutForm {
    /// Every marker, in the order the screen lists them.
    static final String[] RUNNERS = {"semi", "pump", "ramp"};

    private LoadoutForm() {
    }

    public static void show() {
        final Form f = Ui.form("", "loadout");
        f.setLayout(new LayeredLayout());
        f.getToolbar().hideToolbar();
        f.setScrollable(false);
        f.setUIID("MenuForm");
        PlayerDto p = Session.user();
        String current = p == null || p.runner() == null ? RUNNERS[0] : p.runner();
        boolean wide = CN.getDisplayWidth() > CN.getDisplayHeight();
        final OperatorStage stage = new OperatorStage(OperatorStage.operatorFor(p == null ? null : p.username()),
                current, wide ? 0.9f : 0.55f);
        f.add(stage);
        Label shade = new Label();
        shade.setUIID("MenuShade");
        f.add(shade);
        final Container front = new Container(new BorderLayout());
        front.setSafeArea(true);
        f.add(front);
        fill(f, front, stage, current);
        f.show();
    }

    private static void fill(final Form f, final Container front, final OperatorStage stage, final String shown) {
        front.removeAll();
        PlayerDto p = Session.user();
        String current = p == null || p.runner() == null ? RUNNERS[0] : p.runner();
        Container top = new Container(new BorderLayout());
        top.setUIID("MenuTop");
        Button back = Ui.button("< BACK", "BackLink", "back", e -> Nav.home());
        top.add(BorderLayout.WEST, BoxLayout.encloseY(back, new Label("OPERATOR", "MenuLogo"),
                new Label("PRIMARY  //  MARKER", "MenuLogoSub")));
        front.add(BorderLayout.NORTH, top);

        Container col = new Container(BoxLayout.y());
        col.setUIID("MenuColumn");
        int maxRate = 0;
        int maxSpeed = 0;
        int maxSpread = 1;
        int maxHopper = 0;
        int maxMob = 0;
        for (int i = 0; i < Catalog.MARKERS.length; i++) {
            MarkerDef m = Catalog.MARKERS[i];
            maxRate = Math.max(maxRate, 30 * 60 / Math.max(1, m.cooldown));
            maxSpeed = Math.max(maxSpeed, m.speed);
            maxSpread = Math.max(maxSpread, m.spread);
            maxHopper = Math.max(maxHopper, m.hopper);
            maxMob = Math.max(maxMob, m.mobility);
        }
        for (int i = 0; i < RUNNERS.length; i++) {
            final String id = RUNNERS[i];
            final MarkerDef m = Catalog.marker(id);
            boolean owned = i == 0 || p != null && p.owned() != null && p.owned().contains(id);
            boolean open = id.equals(shown);
            Button pick = Ui.button(Ui.caps(m.name), open ? "MenuItemTextPrimed" : "MenuItemText", id, null);
            // a long name shrinks to fit rather than ending in an ellipsis
            pick.setAutoSizeMode(true);
            pick.addActionListener(e -> {
                stage.setMarker(id);
                fill(f, front, stage, id);
            });
            Container row = new Container(BoxLayout.y());
            row.setUIID(open ? "MenuItemPrimed" : "MenuItem");
            row.add(pick);
            String state = id.equals(current) ? "EQUIPPED" : owned ? "OWNED" : "LOCKED -- STORE";
            row.add(new Label(state, id.equals(current) ? "LoadoutEquipped" : "MenuItemSub"));
            if (open) {
                row.add(Ui.text(m.blurb, "LoadoutBlurb"));
                row.add(stat("FIRE RATE", 30 * 60 / Math.max(1, m.cooldown), maxRate));
                row.add(stat("RANGE", m.speed, maxSpeed));
                row.add(stat("ACCURACY", maxSpread - m.spread + maxSpread / 4, maxSpread + maxSpread / 4));
                row.add(stat("HOPPER", m.hopper, maxHopper));
                row.add(stat("MOBILITY", m.mobility, maxMob));
                if (id.equals(current)) {
                    Label on = new Label("CARRYING", "RunnerChip");
                    row.add(on);
                } else if (owned) {
                    row.add(Ui.button("EQUIP", "DeployButton", "choose-" + id, e -> choose(f, front, stage, id)));
                } else {
                    row.add(Ui.button("UNLOCK IN STORE", "OpsAction", "shop-" + id, e -> ShopForm.show()));
                }
            } else {
                row.setLeadComponent(pick);
            }
            col.add(row);
        }
        col.add(Ui.button("ARMORY", "OpsAction", "runnerShop", e -> ShopForm.show()));
        Container left = new Container(new BorderLayout());
        left.add(BorderLayout.NORTH, col);
        left.setScrollableY(true);
        left.setPreferredW(Math.min(CN.getDisplayWidth() * 4 / 5, CN.convertToPixels(90)));
        front.add(BorderLayout.WEST, left);
        f.revalidate();
    }

    /// One labelled bar: the stat's name, then the bar in amber.
    private static Container stat(String name, int value, int max) {
        Container c = new Container(new BorderLayout());
        c.setUIID("LoadoutStat");
        Label l = new Label(name, "LoadoutStatName");
        l.setPreferredW(CN.convertToPixels(18));
        c.add(BorderLayout.WEST, l);
        c.add(BorderLayout.CENTER, Ui.bar(value, Math.max(1, max), 0xf2b632));
        return c;
    }

    private static void choose(final Form f, final Container front, final OperatorStage stage, final String id) {
        Api.game().saveRunner(new RunnerDto(id), Net.to(r -> Session.refreshUser(() -> Ui.later(() -> {
            Telemetry.runnerChosen(id);
            fill(f, front, stage, id);
        }), Ui::failed), Ui::failed));
    }
}
// end::splat-loadout[]
