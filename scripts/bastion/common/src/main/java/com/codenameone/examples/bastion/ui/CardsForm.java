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
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.CardDto;
import com.codenameone.examples.bastion.api.CardLimitDto;
import com.codenameone.examples.bastion.security.RequestSigner;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import java.util.List;

/// The cards: freeze one the moment it is missing, and set what it may spend
/// in a day.
///
/// Freezing is instant and needs no signature -- the point is that it can be
/// done in a hurry -- while the daily limit bounds what a stolen card number
/// costs before anyone notices. Only the last four digits ever reach the
/// app.
public final class CardsForm {
    private CardsForm() {
    }

    public static void show(final Runnable back) {
        Form form = Nav.form("Cards", back);
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Ui.show(form, true);
        load(content);
    }

    private static void load(final Container content) {
        Ui.loading(content);
        Api.banking().cards(Net.to((List<CardDto> cards) ->
                Api.banking().accounts(Net.to((List<AccountDto> accounts) ->
                        fill(content, cards, accounts),
                        (status, message) -> Ui.failed(content, message, () -> load(content)))),
                (status, message) -> Ui.failed(content, message, () -> load(content))));
    }

    private static void fill(final Container content, List<CardDto> cards,
            List<AccountDto> accounts) {
        content.removeAll();
        if (cards.isEmpty()) {
            content.add(Ui.empty("No cards yet. Order one from a checking account."));
        }
        for (final CardDto card : cards) {
            final String asset = assetOf(accounts, card.accountId());
            Container face = new Container(BoxLayout.y(), card.frozen() ? "BnCardFrozen" : "BnCard");
            face.add(new Label("Bastion", "BnCardBrand"));
            face.add(new Label("**** **** **** " + card.last4(), "BnCardNumber"));
            face.add(new Label(card.frozen() ? Lang.tr("Frozen")
                    : Lang.tr("Spends up to {0} a day", Amounts.format(card.dailyLimit(), asset)),
                    "BnCardNote"));
            content.add(face);
            final Button freeze = Ui.secondary(card.frozen() ? "Unfreeze" : "Freeze");
            freeze.addActionListener(evt -> {
                freeze.setEnabled(false);
                Net.Failed failed = (status, message) -> {
                    freeze.setEnabled(true);
                    Dialog.show(Lang.tr("Not changed"), Lang.tr(message), Lang.tr("OK"), null);
                };
                if (card.frozen()) {
                    DeviceSetup.withDevice(retry -> Api.banking().unfreezeCard(card.id(),
                            RequestSigner.sign("POST", "/api/cards/" + card.id() + "/unfreeze", null,
                                    null), Net.to((CardDto c) -> load(content), retry)), failed);
                } else {
                    Api.banking().freezeCard(card.id(), Net.to((CardDto c) -> load(content), failed));
                }
            });
            Button limit = Ui.secondary("Daily limit");
            limit.addActionListener(evt -> limit(content, card, asset));
            content.add(Ui.across(freeze, limit));
        }
        content.revalidate();
    }

    private static void limit(final Container content, CardDto card, String asset) {
        TextField amount = Ui.field("Daily limit", TextField.DECIMAL);
        amount.setText(Amounts.plain(card.dailyLimit(), asset));
        Command ok = new Command(Lang.tr("Save"));
        if (Dialog.show(Lang.tr("Daily limit"), amount, ok, new Command(Lang.tr("Cancel"))) != ok) {
            return;
        }
        long units = Amounts.parse(amount.getText(), asset);
        if (units < 0) {
            Dialog.show(Lang.tr("Daily limit"), Ui.amountHint(),
                    Lang.tr("OK"), null);
            return;
        }
        // Signed always; the server asks for it only when the limit goes up.
        final CardLimitDto limit = new CardLimitDto(units);
        DeviceSetup.withDevice(retry -> Api.banking().cardLimit(card.id(),
                RequestSigner.sign("PUT", "/api/cards/" + card.id() + "/limit", null, limit), limit,
                Net.to((CardDto c) -> load(content), retry)),
                (status, message) -> Dialog.show(Lang.tr("Not changed"), Lang.tr(message),
                        Lang.tr("OK"), null));
    }

    private static String assetOf(List<AccountDto> accounts, String accountId) {
        for (AccountDto account : accounts) {
            if (account.id().equals(accountId)) {
                return account.asset();
            }
        }
        return "USD";
    }
}
