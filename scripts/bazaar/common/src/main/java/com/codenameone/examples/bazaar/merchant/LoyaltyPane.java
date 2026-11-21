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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.components.Switch;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.DataGrid;
import com.codenameone.examples.bazaar.api.LoyaltyProgramDto;
import com.codenameone.examples.bazaar.api.LoyaltyTierDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The loyalty programme: how points are earned and spent, and the tiers
/// as an editable `DataGrid` -- name, the spend or points a customer needs,
/// the member price (basis points off), free shipping and early access.
///
/// A cell edit changes the row here; Save sends the whole programme, which
/// the server checks as one (thresholds must climb with the tier order).
final class LoyaltyPane extends MerchantPane {
    private LoyaltyProgramDto program;
    private final List<LoyaltyTierDto> tiers = new ArrayList<LoyaltyTierDto>();
    private Rows<LoyaltyTierDto> tierRows;

    LoyaltyPane() {
        super("Loyalty", "merchant-loyalty");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Add tier", "loyalty-add-tier", this::addTier);
        Api.loyalty().program(MerchantState.storeId(), Net.to(p -> {
            program = p;
            tiers.clear();
            if (p.tiers() != null) {
                tiers.addAll(p.tiers());
            }
            paint();
        }, Ui::fail));
    }

    private void paint() {
        final LoyaltyProgramDto p = program;
        Container page = MUi.page("loyalty-page");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        card.add(Ui.plain(Lang.tr("Earning and rewards"), "SvRowTitle"));
        final boolean[] on = {p.active()};
        final boolean[] pos = {p.earnOnPos()};
        Switch active = Ui.toggle("loyalty-active", p.active(), wanted -> on[0] = wanted);
        card.add(Ui.row(FontImage.MATERIAL_LOYALTY, Lang.tr("Programme on"), null, active));
        final TextField perUnit = MUi.number(card, "Points per unit spent", "loyalty-points",
                p.pointsPerUnit());
        final TextField unit = MUi.money(card, "Unit of spend", "loyalty-unit", p.unitCents());
        final TextField perReward = MUi.number(card, "Points for a reward", "loyalty-reward-points",
                p.pointsPerReward());
        final TextField reward = MUi.money(card, "Reward value", "loyalty-reward-value",
                p.rewardCents());
        final TextField min = MUi.number(card, "Fewest points to redeem", "loyalty-min",
                p.minRedeemPoints());
        final TextField expiry = MUi.number(card, "Points expire after (days, 0 never)",
                "loyalty-expiry", p.expiryDays());
        final TextField referrer = MUi.number(card, "Points for referring", "loyalty-referrer",
                p.referrerPoints());
        final TextField referee = MUi.number(card, "Points for being referred", "loyalty-referee",
                p.refereePoints());
        Switch earnPos = Ui.toggle("loyalty-pos", p.earnOnPos(), wanted -> pos[0] = wanted);
        card.add(Ui.row(FontImage.MATERIAL_POINT_OF_SALE, Lang.tr("Earn at the register"), null,
                earnPos));
        page.add(card);

        Container tierCard = new Container(BoxLayout.y());
        tierCard.setUIID("MeCard");
        tierCard.add(Ui.plain(Lang.tr("Tiers"), "SvRowTitle"));
        tierCard.add(Ui.text(Lang.tr("Double-click a cell to change it. Member price is basis "
                + "points off (500 = 5%)."), "SvMuted"));
        final Rows<LoyaltyTierDto> rows = new Rows<LoyaltyTierDto>();
        rows.col(Lang.tr("Tier"), t -> t.name(), (t, v) -> edit(rows, t, with(t, "name", v)))
                .col(Lang.tr("Threshold"), t -> String.valueOf(t.threshold()),
                        (t, v) -> edit(rows, t, with(t, "threshold", v)))
                .col(Lang.tr("Member price (bp)"), t -> String.valueOf(t.memberBps()),
                        (t, v) -> edit(rows, t, with(t, "memberBps", v)))
                .col(Lang.tr("Free shipping"), t -> t.freeShipping() ? Lang.tr("Yes") : Lang.tr("No"),
                        (t, v) -> edit(rows, t, with(t, "freeShipping", v)))
                .col(Lang.tr("Early access (h)"), t -> String.valueOf(t.earlyAccessHours()),
                        (t, v) -> edit(rows, t, with(t, "earlyAccessHours", v)));
        rows.set(tiers);
        tierRows = rows;
        final DataGrid grid = MUi.grid(rows, "loyalty-tiers");
        grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
        RowMenus.attach(grid, () -> {
            final LoyaltyTierDto t = rows.at(grid.getModelRow(grid.getSelectedRow()));
            if (t == null) {
                return null;
            }
            return new Command[] {Command.create(Lang.tr("Remove tier"), null, e -> {
                tiers.remove(t);
                rows.set(tiers);
            })};
        });
        tierCard.add(grid);
        page.add(tierCard);

        page.add(MUi.buttons(Ui.primary(Lang.tr("Save"), "loyalty-save", e -> {
            List<LoyaltyTierDto> ordered = new ArrayList<LoyaltyTierDto>();
            for (int iter = 0; iter < tiers.size(); iter++) {
                LoyaltyTierDto t = tiers.get(iter);
                ordered.add(new LoyaltyTierDto(t.id(), t.name(), t.threshold(), t.memberBps(),
                        t.freeShipping(), t.earlyAccessHours(), t.colour(), iter));
            }
            LoyaltyProgramDto wanted = new LoyaltyProgramDto(on[0], MUi.integer(perUnit),
                    MUi.cents(unit), p.rounding(), MUi.integer(perReward), MUi.cents(reward),
                    MUi.integer(min), MUi.integer(expiry), pos[0], p.tierBasis(),
                    MUi.integer(referrer), MUi.integer(referee), ordered);
            Api.loyalty().saveProgram(MerchantState.storeId(), wanted, Net.to(saved -> {
                Ui.say("Saved");
                load();
            }, Ui::fail));
        })));
        root(page);
    }

    private void edit(Rows<LoyaltyTierDto> rows, LoyaltyTierDto was, LoyaltyTierDto now) {
        int at = tiers.indexOf(was);
        if (at >= 0) {
            tiers.set(at, now);
        }
        int row = rows.indexOf(was);
        rows.replace(row, now);
    }

    /// `t` with one field changed from the text typed into its cell.
    static LoyaltyTierDto with(LoyaltyTierDto t, String field, Object value) {
        String v = value == null ? "" : value.toString().trim();
        String name = t.name();
        long threshold = t.threshold();
        int bps = t.memberBps();
        boolean ship = t.freeShipping();
        int early = t.earlyAccessHours();
        if ("name".equals(field)) {
            name = v.length() == 0 ? name : v;
        } else if ("threshold".equals(field)) {
            threshold = SegmentRules.whole(v) ? Long.parseLong(v) : threshold;
        } else if ("memberBps".equals(field)) {
            bps = SegmentRules.whole(v) ? (int) Math.min(10000L, Long.parseLong(v)) : bps;
        } else if ("freeShipping".equals(field)) {
            ship = "yes".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v) || "1".equals(v)
                    || v.equalsIgnoreCase(Lang.tr("Yes"));
        } else if ("earlyAccessHours".equals(field)) {
            early = SegmentRules.whole(v) ? (int) Math.min(720L, Long.parseLong(v)) : early;
        }
        return new LoyaltyTierDto(t.id(), name, threshold, bps, ship, early, t.colour(),
                t.position());
    }

    private void addTier() {
        if (program == null) {
            return;
        }
        long top = 0;
        for (int iter = 0; iter < tiers.size(); iter++) {
            top = Math.max(top, tiers.get(iter).threshold());
        }
        tiers.add(new LoyaltyTierDto(null, Lang.tr("New tier"), top + 50000, 0, false, 0,
                "#6366f1", tiers.size()));
        if (tierRows != null) {
            tierRows.set(tiers);
        }
    }
}
