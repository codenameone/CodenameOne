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
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.table.DataGrid;
import com.codenameone.examples.bazaar.api.ClubDto;
import com.codenameone.examples.bazaar.api.ClubMembershipDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Consumer clubs: a grid of the store's clubs (fee, period, member price,
/// early access, members), an editor for one, and its members in a grid of
/// their own with status and renewal date.
final class ClubsPane extends MerchantPane {
    ClubsPane() {
        super("Clubs", "merchant-clubs");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "New club", "club-new", () -> edit(null));
        Api.clubs().list(MerchantState.storeId(), Net.to(this::fill, Ui::fail));
    }

    private void fill(List<ClubDto> clubs) {
        Container page = new Container(new BorderLayout());
        page.add(BorderLayout.NORTH, Ui.text(Lang.tr("Members pay a fee each period for member "
                + "prices and early access to sales. Open a club to see its members."), "SvMuted"));
        final Rows<ClubDto> rows = new Rows<ClubDto>()
                .col(Lang.tr("Club"), c -> c.name())
                .col(Lang.tr("Fee"), c -> MerchantState.money(c.feeCents()))
                .col(Lang.tr("Every (days)"), c -> String.valueOf(c.periodDays()))
                .col(Lang.tr("Member price (bp)"), c -> String.valueOf(c.memberBps()))
                .col(Lang.tr("Early access (h)"), c -> String.valueOf(c.earlyAccessHours()))
                .col(Lang.tr("Members"), c -> String.valueOf(c.members()))
                .col(Lang.tr("Status"), c -> Lang.tr(c.active() ? "Active" : "Closed"));
        rows.set(clubs);
        final DataGrid grid = MUi.grid(rows, "clubs-grid");
        grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
        grid.addActionListener(e -> {
            ClubDto c = rows.at(grid.getModelRow(grid.getSelectedRow()));
            if (c != null) {
                members(c);
            }
        });
        RowMenus.attach(grid, () -> {
            final ClubDto c = rows.at(grid.getModelRow(grid.getSelectedRow()));
            if (c == null) {
                return null;
            }
            return new Command[] {
                Command.create(Lang.tr("Members"), null, e -> members(c)),
                Command.create(Lang.tr("Edit"), null, e -> edit(c))
            };
        });
        page.add(BorderLayout.CENTER, grid);
        root(page);
    }

    private void edit(final ClubDto c) {
        Container page = MUi.page("club-editor");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final TextField name = MUi.text(card, "Name", "club-name", c == null ? "" : c.name());
        final TextField about = MUi.text(card, "Description", "club-description",
                c == null ? "" : c.description());
        final TextField fee = MUi.money(card, "Fee", "club-fee", c == null ? 0 : c.feeCents());
        final TextField period = MUi.number(card, "Every (days)", "club-period",
                c == null ? 30 : c.periodDays());
        final TextField bps = MUi.number(card, "Member price, basis points off", "club-bps",
                c == null ? 1000 : c.memberBps());
        final TextField early = MUi.number(card, "Early access to sales (hours)", "club-early",
                c == null ? 24 : c.earlyAccessHours());
        final boolean[] active = {c == null || c.active()};
        Switch on = Ui.toggle("club-active", active[0], wanted -> active[0] = wanted);
        card.add(Ui.row(FontImage.MATERIAL_GROUPS, Lang.tr("Open to new members"), null, on));
        page.add(card);
        page.add(MUi.buttons(Ui.primary(Lang.tr("Save"), "club-save", e -> {
            ClubDto wanted = new ClubDto(c == null ? null : c.id(), name.getText().trim(),
                    about.getText().trim(), MUi.cents(fee), MUi.integer(period),
                    MUi.integer(bps), MUi.integer(early), c == null ? "#6366f1" : c.colour(),
                    active[0], c == null ? 0 : c.members());
            Net.Ok<ClubDto> done = saved -> {
                Ui.say("Saved");
                pop();
                load();
            };
            if (c == null) {
                Api.clubs().create(MerchantState.storeId(), wanted, Net.to(done, Ui::fail));
            } else {
                Api.clubs().save(MerchantState.storeId(), c.id(), wanted, Net.to(done, Ui::fail));
            }
        })));
        push(c == null ? "New club" : c.name(), page);
    }

    private void members(final ClubDto c) {
        Api.clubs().members(MerchantState.storeId(), c.id(), Net.to(list -> {
            Container page = new Container(new BorderLayout());
            Rows<ClubMembershipDto> rows = new Rows<ClubMembershipDto>()
                    .col(Lang.tr("Member"), m -> m.username())
                    .col(Lang.tr("Status"), m -> Lang.tr(Words.of(m.status())))
                    .col(Lang.tr("Joined"), m -> MUi.when(m.joinedAt()))
                    .col(Lang.tr("Renews"), m -> m.renewsAt() > 0 ? MUi.when(m.renewsAt()) : "");
            rows.set(list);
            page.add(BorderLayout.NORTH, Ui.text(Lang.tr("{0} members", list.size()), "SvMuted"));
            page.add(BorderLayout.CENTER, MUi.grid(rows, "club-members"));
            push(c.name(), page);
        }, Ui::fail));
    }
}
