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

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.MarketDto;
import com.codenameone.examples.bastion.api.OpenAccountDto;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.money.Assets;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.net.Session;
import java.util.ArrayList;
import java.util.List;

/// The dashboard: every account with its balance in its own asset, and the
/// total of all of them in dollars.
///
/// The total is a valuation, not a balance. A crypto holding is priced at its
/// market's index price, rounded down ([Amounts#value]); an asset with no
/// dollar market (euros, here) is left out of the total and the screen says
/// so, rather than converting at a rate the bank never quoted.
public final class HomeForm {
    private HomeForm() {
    }

    public static void show() {
        UserDto user = Session.user();
        Form form = Nav.form("Bastion", null);
        String name = user == null ? null : user.displayName();
        form.add(Ui.title(name == null ? Lang.tr("Welcome") : Lang.tr("Hello, {0}", name)));
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Ui.show(form, true);
        load(content);
    }

    private static void load(final Container content) {
        Ui.loading(content);
        Api.banking().accounts(Net.to((List<AccountDto> accounts) ->
                Api.exchange().markets(Net.to((List<MarketDto> markets) ->
                        fill(content, accounts, markets),
                        // Without prices the balances are still worth showing.
                        (status, message) -> fill(content, accounts, new ArrayList<MarketDto>()))),
                (status, message) -> Ui.failed(content, message, () -> load(content))));
    }

    private static void fill(final Container content, List<AccountDto> accounts,
            List<MarketDto> markets) {
        content.removeAll();
        if (!DeviceSetup.registered()) {
            content.add(deviceBanner(content));
        }
        long total = 0;
        List<String> unpriced = new ArrayList<String>();
        for (AccountDto account : accounts) {
            if (Assets.REPORTING.equals(account.asset())) {
                total += account.balance();
                continue;
            }
            long price = price(markets, account.asset(), Assets.REPORTING);
            if (price > 0) {
                total += Amounts.value(account.balance(), Assets.decimals(account.asset()), price);
            } else if (account.balance() != 0 && !unpriced.contains(account.asset())) {
                unpriced.add(account.asset());
            }
        }
        Container hero = new Container(BoxLayout.y(), "BnHero");
        hero.add(new Label(Lang.tr("Total balance"), "BnHeroLabel"));
        hero.add(new Label(Amounts.format(total, Assets.REPORTING), "BnHeroAmount"));
        if (!unpriced.isEmpty()) {
            SpanLabel note = new SpanLabel(Lang.tr("Not counted, no dollar price: {0}",
                    join(unpriced)));
            note.setTextUIID("BnHeroNote");
            hero.add(note);
        }
        content.add(hero);

        Button move = Ui.primary("Move money");
        move.addActionListener(evt -> TransferForm.show(null, false, HomeForm::show));
        Button pay = Ui.secondary("Pay someone");
        pay.addActionListener(evt -> TransferForm.show(null, true, HomeForm::show));
        Button trade = Ui.secondary("Trade");
        trade.addActionListener(evt -> ExchangeForm.show(HomeForm::show));
        content.add(Ui.across(move, pay, trade));

        content.add(Ui.section("Accounts"));
        if (accounts.isEmpty()) {
            content.add(Ui.empty("You have no accounts yet."));
        }
        for (final AccountDto account : accounts) {
            String detail = kindName(account.kind()) + " - " + account.asset()
                    + (account.frozen() ? " - " + Lang.tr("Frozen") : "");
            content.add(Ui.row(account.name(), detail,
                    Amounts.format(account.balance(), account.asset()),
                    account.frozen() ? "BnRowValueMuted" : "BnRowValue",
                    () -> AccountForm.show(account.id(), HomeForm::show)));
        }
        Button open = Ui.secondary("Open an account");
        open.addActionListener(evt -> openAccount(content));
        content.add(open);
        content.revalidate();
    }

    /// The price of one whole `base` in `quote` units, or 0 when no market
    /// quotes it.
    static long price(List<MarketDto> markets, String base, String quote) {
        for (MarketDto market : markets) {
            if (market.base().equals(base) && market.quote().equals(quote)) {
                return market.indexPrice();
            }
        }
        return 0;
    }

    static String kindName(String kind) {
        if ("CHECKING".equals(kind)) {
            return Lang.tr("Checking");
        }
        if ("SAVINGS".equals(kind)) {
            return Lang.tr("Savings");
        }
        if ("WALLET".equals(kind)) {
            return Lang.tr("Wallet");
        }
        return kind;
    }

    private static Container deviceBanner(final Container content) {
        Container banner = new Container(BoxLayout.y(), "BnBanner");
        banner.add(new Label(Lang.tr("This device cannot sign payments yet"), "BnBannerTitle"));
        SpanLabel why = new SpanLabel(Lang.tr("Payments to other people and crypto withdrawals "
                + "need a key that lives only on this phone."));
        why.setTextUIID("BnBannerText");
        banner.add(why);
        final Label error = Ui.error();
        Button register = Ui.primary("Register this device");
        register.addActionListener(evt -> DeviceSetup.register(() -> load(content),
                (status, message) -> Ui.showError(error, Lang.tr(message))));
        banner.addAll(error, register);
        return banner;
    }

    /// Opens another account: fiat in a checking or savings account, a coin
    /// in a wallet -- the server's rule, offered here as the only choices.
    private static void openAccount(final Container content) {
        Assets.Asset[] all = Assets.all();
        List<String> labels = new ArrayList<String>();
        final List<String[]> choices = new ArrayList<String[]>();
        for (Assets.Asset asset : all) {
            if (asset.isCrypto()) {
                labels.add(Lang.tr("{0} wallet", asset.name));
                choices.add(new String[] {asset.code, "WALLET"});
            } else {
                labels.add(Lang.tr("{0} checking", asset.name));
                choices.add(new String[] {asset.code, "CHECKING"});
                labels.add(Lang.tr("{0} savings", asset.name));
                choices.add(new String[] {asset.code, "SAVINGS"});
            }
        }
        final String[] names = labels.toArray(new String[labels.size()]);
        Picker which = Ui.picker(names, 0);
        TextField name = Ui.field("Name (optional)", TextField.ANY);
        Container body = new Container(BoxLayout.y());
        body.addAll(which, name);
        Command open = new Command(Lang.tr("Open"));
        if (Dialog.show(Lang.tr("Open an account"), body, open, new Command(Lang.tr("Cancel")))
                != open) {
            return;
        }
        int index = Ui.picked(which, names);
        if (index < 0) {
            return;
        }
        String[] choice = choices.get(index);
        String title = name.getText().trim().length() > 0 ? name.getText().trim() : names[index];
        Api.banking().open(new OpenAccountDto(choice[0], choice[1], title),
                Net.to((AccountDto account) -> load(content),
                        (status, message) -> Dialog.show(Lang.tr("Not opened"), Lang.tr(message),
                                Lang.tr("OK"), null)));
    }

    private static String join(List<String> parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(part);
        }
        return out.toString();
    }
}
