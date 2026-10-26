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
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.CardDto;
import com.codenameone.examples.bastion.api.CardLimitDto;
import com.codenameone.examples.bastion.api.PostingDto;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.net.Session;
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
        UserDto user = Session.user();
        String holder = user == null ? "" : user.displayName() == null ? user.username()
                : user.displayName();
        for (final CardDto card : cards) {
            final AccountDto account = accountOf(accounts, card.accountId());
            final String asset = account == null ? "USD" : account.asset();
            Container face = new Container(new FlowLayout(Component.CENTER));
            face.add(new CardFace(card.last4(), holder, CardFace.expiryOf(card.createdAt()),
                    card.frozen()));
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

            // The card's details, then what it spent: both from the account's
            // history, which the server keeps per entry kind.
            content.add(Ui.section("Card details"));
            content.add(Ui.row(Lang.tr("Status"), null, Lang.tr(card.frozen() ? "Frozen" : "Active"),
                    card.frozen() ? "BnRowValueMuted" : "BnAmountIn", null));
            content.add(Ui.row(Lang.tr("Type"), null, Lang.tr("Virtual debit"), null, null));
            content.add(Ui.row(Lang.tr("Account"), null, account == null ? "" : account.name(),
                    null, null));
            content.add(Ui.row(Lang.tr("Expires"), null, CardFace.expiryOf(card.createdAt()), null,
                    null));
            content.add(Ui.copyable(Lang.tr("Card ID"), card.id(), true));
            final Container used = new Container(BoxLayout.y(), "BnRow");
            content.add(used);
            final Container recent = new Container(BoxLayout.y());
            content.add(recent);
            showUse(used, card, asset, 0);
            Api.banking().history(card.accountId(), Net.to((List<PostingDto> postings) ->
                    payments(used, recent, card, asset, postings),
                    (status, message) -> { }));
        }
        content.revalidate();
    }

    /// The daily limit and how much of it today's payments have used.
    private static void showUse(Container used, CardDto card, String asset, long spent) {
        used.removeAll();
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, new Label(Lang.tr("Daily limit"), "BnRowTitle"));
        top.add(BorderLayout.EAST, new Label(Amounts.format(card.dailyLimit(), asset),
                "BnRowValue"));
        used.add(top);
        long limit = Math.max(1L, card.dailyLimit());
        used.add(new Meter((float) Math.min(spent, limit) / limit));
        Ui.addDetail(used, Lang.tr("{0} used today, {1} left",
                Amounts.format(spent, asset),
                Amounts.format(Math.max(0L, card.dailyLimit() - spent), asset)));
    }

    /// The card's recent payments: the account's entries of kind CARD.
    private static void payments(Container used, Container recent, CardDto card, String asset,
            List<PostingDto> postings) {
        long today = startOfToday();
        long spent = 0;
        int shown = 0;
        recent.removeAll();
        recent.add(Ui.section("Recent card payments"));
        for (PostingDto posting : postings) {
            if (!"CARD".equals(posting.kind())) {
                continue;
            }
            if (posting.createdAt() >= today && posting.amount() < 0) {
                spent -= posting.amount();
            }
            if (shown < RECENT) {
                boolean in = posting.amount() > 0;
                recent.add(Ui.row(AccountForm.describe(posting), Ui.when(posting.createdAt()),
                        (in ? "+" : "") + Amounts.format(posting.amount(), asset),
                        in ? "BnAmountIn" : "BnAmountOut", null));
                shown++;
            }
        }
        if (shown == 0) {
            recent.add(Ui.empty("This card has not been used yet."));
        }
        showUse(used, card, asset, spent);
        if (recent.getComponentForm() != null) {
            recent.getComponentForm().revalidate();
        }
    }

    /// How many recent payments the screen lists.
    private static final int RECENT = 8;

    private static long startOfToday() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        now.set(java.util.Calendar.HOUR_OF_DAY, 0);
        now.set(java.util.Calendar.MINUTE, 0);
        now.set(java.util.Calendar.SECOND, 0);
        now.set(java.util.Calendar.MILLISECOND, 0);
        return now.getTime().getTime();
    }

    /// A bar: how much of something is used. Turns to the warning colour
    /// past 80%.
    static final class Meter extends Component {
        private final float used;

        Meter(float used) {
            this.used = used;
            setUIID(used > 0.8f ? "BnMeterHigh" : "BnMeter");
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            return new com.codename1.ui.geom.Dimension(CN.convertToPixels(20f),
                    CN.convertToPixels(1.4f));
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            // The track is the style's background; the bar is its colour,
            // from the start edge (the right one in Hebrew).
            int w = Math.round(getWidth() * used);
            if (w <= 0) {
                return;
            }
            g.setColor(getStyle().getFgColor());
            boolean anti = g.isAntiAliased();
            g.setAntiAliased(true);
            int x = isRTL() ? getX() + getWidth() - w : getX();
            g.fillRoundRect(x, getY(), w, getHeight(), getHeight(), getHeight());
            g.setAntiAliased(anti);
        }
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

    private static AccountDto accountOf(List<AccountDto> accounts, String accountId) {
        for (AccountDto account : accounts) {
            if (account.id().equals(accountId)) {
                return account;
            }
        }
        return null;
    }
}
