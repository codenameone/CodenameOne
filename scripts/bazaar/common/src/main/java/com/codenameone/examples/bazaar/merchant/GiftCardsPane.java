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
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.GiftCardDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Gift cards issued by the store, their balances, and disabling one. The
/// server shows a card's full code once, when it is issued; afterwards only
/// the last four characters.
final class GiftCardsPane extends MerchantPane {
    GiftCardsPane() {
        super("Gift cards", "merchant-giftcards");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Issue gift card", "giftcard-issue", this::issue);
        Api.giftCards().list(MerchantState.storeId(), Net.to(this::fill, Ui::fail));
    }

    private void fill(List<GiftCardDto> list) {
        Container page = MUi.page("giftcards-list");
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        for (final GiftCardDto g : list) {
            Container row = Ui.row(FontImage.MATERIAL_CARD_GIFTCARD, "**** " + g.lastFour(),
                    Lang.tr("{0} of {1}", MerchantState.money(g.balanceCents()),
                            MerchantState.money(g.initialCents()))
                    + (g.note() == null || g.note().length() == 0 ? "" : Ui.DOT + g.note()),
                    MUi.badge(g.active() ? "ACTIVE" : "DISABLED"));
            row.setName("giftcard-" + g.lastFour());
            if (g.active()) {
                RowMenus.attach(row, Command.create(Lang.tr("Disable"), null, e -> disable(g)));
            }
            card.add(row);
        }
        if (list.isEmpty()) {
            card.add(Ui.empty(FontImage.MATERIAL_CARD_GIFTCARD, "No gift cards",
                    "Issue a card to give store credit."));
        }
        page.add(card);
        root(page);
    }

    private void disable(GiftCardDto g) {
        if (MUi.confirm("Disable gift card", "The remaining balance can no longer be spent.",
                "Disable")) {
            Api.giftCards().disable(MerchantState.storeId(), g.id(), Net.to(x -> load(), Ui::fail));
        }
    }

    private void issue() {
        Container body = new Container(BoxLayout.y());
        TextField amount = MUi.money(body, "Value", "giftcard-value", 5000);
        TextField note = MUi.text(body, "Note", "giftcard-note", "");
        TextField to = Ui.field(body, "Send to (e-mail, optional)", "giftcard-to", "",
                com.codename1.ui.TextArea.EMAILADDR);
        TextField name = MUi.text(body, "Recipient name", "giftcard-to-name", "");
        TextField message = MUi.text(body, "Message", "giftcard-message", "");
        TextField days = Ui.field(body, "Send in days (blank: now)", "giftcard-days", "",
                com.codename1.ui.TextArea.NUMERIC);
        Command ok = new com.codename1.ui.Command(Lang.tr("Issue"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Issue gift card"), body, cancel, ok) != ok) {
            return;
        }
        long cents = MUi.cents(amount);
        if (cents <= 0) {
            return;
        }
        Api.giftCards().issue(MerchantState.storeId(), new GiftCardDto(null, null, null, cents, cents,
                true, note.getText(), 0, null, 0, to.getText().trim(), name.getText().trim(),
                message.getText().trim(),
                CustomerMatches.daysFrom(days.getText(), System.currentTimeMillis()), 0),
                Net.to(made -> {
                    Dialog.show(Lang.tr("Gift card issued"), Lang.tr("Code: {0}", made.code()),
                            Lang.tr("OK"), null);
                    load();
                }, Ui::fail));
    }
}
