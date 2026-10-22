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
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.CryptoAddressDto;
import com.codenameone.examples.bastion.api.CryptoWithdrawalDto;
import com.codenameone.examples.bastion.api.NewCryptoAddressDto;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.money.Assets;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.security.RequestSigner;
import java.util.ArrayList;
import java.util.List;

/// Sending coins out of the bank, to an address on the user's own list.
///
/// A crypto withdrawal cannot be called back, so it is fenced three ways:
///
/// - **An allow-list.** Coins go only to an address the user added before;
///   there is no field to paste an address into on this screen. Adding one
///   is signed by the device.
/// - **A delay.** A newly added address becomes usable only after a waiting
///   period (`usableAt`), so an attacker who adds their own has to sit out
///   a window in which the owner is told and can remove it.
/// - **A signature and a key.** The withdrawal itself is signed and carries
///   an idempotency key, like every other money action, and counts toward
///   the account's crypto limit on the server.
public final class WithdrawForm {
    private static final String ADDRESSES = "/api/crypto/addresses";

    private WithdrawForm() {
    }

    /// `walletId` preselects the wallet to send from, or null.
    public static void show(final String walletId, final Runnable back) {
        Form form = Nav.form("Send crypto", back);
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Ui.show(form, true);
        load(content, walletId, back);
    }

    private static void load(final Container content, final String walletId, final Runnable back) {
        Ui.loading(content);
        Api.banking().accounts(Net.to((List<AccountDto> accounts) ->
                Api.exchange().addresses(Net.to((List<CryptoAddressDto> addresses) ->
                        fill(content, accounts, addresses, walletId, back),
                        (status, message) -> Ui.failed(content, message,
                                () -> load(content, walletId, back)))),
                (status, message) -> Ui.failed(content, message,
                        () -> load(content, walletId, back))));
    }

    private static void fill(final Container content, List<AccountDto> accounts,
            final List<CryptoAddressDto> addresses, String walletId, final Runnable back) {
        content.removeAll();
        final Runnable again = () -> load(content, walletId, back);
        final List<AccountDto> wallets = new ArrayList<AccountDto>();
        int initial = 0;
        for (AccountDto account : accounts) {
            if (Assets.isCrypto(account.asset()) && !account.frozen()) {
                if (account.id().equals(walletId)) {
                    initial = wallets.size();
                }
                wallets.add(account);
            }
        }
        long now = System.currentTimeMillis();
        if (wallets.isEmpty()) {
            content.add(Ui.empty("Open a crypto wallet to send coins from."));
        } else {
            content.add(sendPanel(wallets, initial, addresses, now, back));
        }

        content.add(Ui.section("Your addresses"));
        Button add = Ui.secondary("Add an address");
        add.addActionListener(evt -> addAddress(again));
        if (addresses.isEmpty()) {
            content.add(Ui.empty("No addresses yet. Coins can be sent only to an address on "
                    + "this list."));
        }
        for (final CryptoAddressDto address : addresses) {
            boolean waiting = address.usableAt() > now;
            String detail = address.asset() + " - " + address.address()
                    + (waiting ? "\n" + Lang.tr("Usable from {0}", Ui.when(address.usableAt())) : "");
            content.add(Ui.row(address.label(), detail, Lang.tr(waiting ? "Waiting" : "Ready"),
                    waiting ? "BnBadgeWarn" : "BnBadgeGood", () -> remove(address, again)));
        }
        content.add(add);
        content.revalidate();
    }

    private static Container sendPanel(final List<AccountDto> wallets, int initial,
            final List<CryptoAddressDto> addresses, final long now, final Runnable back) {
        final Container panel = new Container(BoxLayout.y(), "BnPanel");
        final String[] walletLabels = TransferForm.labels(wallets);
        final String[] walletIds = TransferForm.ids(wallets);
        final Picker wallet = Ui.picker(walletIds, walletLabels,
                walletIds[Math.min(initial, walletIds.length - 1)]);
        final Container target = new Container(BoxLayout.y());
        final List<CryptoAddressDto> usable = new ArrayList<CryptoAddressDto>();
        final Picker[] to = new Picker[1];
        final String[][] toLabels = new String[1][];
        final String[][] toIds = new String[1][];
        final Runnable retarget = () -> {
            AccountDto from = wallets.get(Math.max(0, Ui.indexOf(walletIds, Ui.pickedId(wallet))));
            usable.clear();
            for (CryptoAddressDto address : addresses) {
                if (address.asset().equals(from.asset()) && address.usableAt() <= now) {
                    usable.add(address);
                }
            }
            target.removeAll();
            toLabels[0] = new String[usable.size()];
            toIds[0] = new String[usable.size()];
            for (int iter = 0; iter < usable.size(); iter++) {
                toIds[0][iter] = usable.get(iter).id();
                toLabels[0][iter] = usable.get(iter).label() + " - " + shorten(usable.get(iter).address());
            }
            if (usable.isEmpty()) {
                to[0] = null;
                target.add(Ui.empty(Lang.tr("No {0} address is ready yet.", from.asset())));
            } else {
                to[0] = Ui.picker(toIds[0], toLabels[0], null);
                target.add(to[0]);
            }
            target.revalidate();
        };
        wallet.addActionListener(evt -> retarget.run());
        retarget.run();
        final TextField amount = Ui.field("Amount", TextField.DECIMAL);
        final Label error = Ui.error();
        final Button send = Ui.primary("Review");
        panel.addAll(Ui.section("From"), wallet, Ui.section("To"), target, amount, error, send);
        final CryptoWithdrawalDto[] tried = new CryptoWithdrawalDto[1];
        final String[] key = new String[1];
        send.addActionListener(evt -> {
            Ui.showError(error, null);
            final AccountDto from = wallets.get(Math.max(0, Ui.indexOf(walletIds, Ui.pickedId(wallet))));
            if (to[0] == null) {
                Ui.showError(error, Lang.tr("Choose an address."));
                return;
            }
            CryptoAddressDto address = usable.get(Math.max(0, Ui.indexOf(toIds[0], Ui.pickedId(to[0]))));
            long units = Amounts.parse(amount.getText(), from.asset());
            if (units <= 0 || units > from.balance()) {
                Ui.showError(error, Lang.tr(units <= 0 ? "Enter an amount."
                        : "That is more than this wallet holds."));
                return;
            }
            final CryptoWithdrawalDto withdrawal = new CryptoWithdrawalDto(address.id(), units);
            if (!Ui.confirm("Confirm", Lang.tr("Send {0} to {1}? This cannot be undone.",
                    Amounts.format(units, from.asset()), address.address()), "Send")) {
                return;
            }
            if (tried[0] == null || !tried[0].equals(withdrawal)) {
                tried[0] = withdrawal;
                key[0] = RequestSigner.idempotencyKey();
            }
            send.setEnabled(false);
            final String target2 = "/api/accounts/" + from.id() + "/crypto-withdraw";
            DeviceSetup.withDevice(failed -> Api.exchange().withdrawCrypto(from.id(), key[0],
                    RequestSigner.sign("POST", target2, key[0], withdrawal), withdrawal,
                    Net.to((ReceiptDto receipt) -> ReceiptForm.show(receipt, back), failed)),
                    (status, message) -> {
                        send.setEnabled(true);
                        Ui.showError(error, Lang.tr(message));
                    });
        });
        return panel;
    }

    private static void addAddress(final Runnable done) {
        List<String> coins = new ArrayList<String>();
        for (Assets.Asset asset : Assets.all()) {
            if (asset.isCrypto()) {
                coins.add(asset.code);
            }
        }
        final String[] codes = coins.toArray(new String[coins.size()]);
        Picker coin = Ui.picker(codes, codes, null);
        TextField address = Ui.field("Address", TextField.ANY);
        TextField label = Ui.field("Label, like My hardware wallet", TextField.ANY);
        Container body = new Container(BoxLayout.y());
        body.addAll(coin, address, label);
        Command ok = new Command(Lang.tr("Add"));
        if (Dialog.show(Lang.tr("Add an address"), body, ok, new Command(Lang.tr("Cancel"))) != ok) {
            return;
        }
        final NewCryptoAddressDto added = new NewCryptoAddressDto(Ui.pickedId(coin),
                address.getText().trim(), label.getText().trim());
        DeviceSetup.withDevice(failed -> Api.exchange().addAddress(
                RequestSigner.sign("POST", ADDRESSES, null, added), added,
                Net.to((CryptoAddressDto saved) -> {
                    Dialog.show(Lang.tr("Address added"), Lang.tr("For your safety it can be used "
                            + "from {0}.", Ui.when(saved.usableAt())), Lang.tr("OK"), null);
                    done.run();
                }, failed)),
                (status, message) -> Dialog.show(Lang.tr("Not added"), Lang.tr(message),
                        Lang.tr("OK"), null));
    }

    private static void remove(final CryptoAddressDto address, final Runnable done) {
        if (!Ui.confirm("Remove address", Lang.tr("Remove {0}? Adding it again starts a new wait.",
                address.label()), "Remove")) {
            return;
        }
        // Signed, as adding one is: the list is what a withdrawal is checked
        // against.
        DeviceSetup.withDevice(retry -> Api.exchange().removeAddress(address.id(),
                RequestSigner.sign("DELETE", "/api/crypto/addresses/" + address.id(), null, null),
                Net.to((CryptoAddressDto gone) -> done.run(), retry)),
                (status, message) -> Dialog.show(Lang.tr("Not removed"), Lang.tr(message),
                        Lang.tr("OK"), null));
    }

    /// The start and end of a long address: enough to compare with the
    /// wallet's, short enough for a row.
    static String shorten(String address) {
        if (address == null || address.length() <= 16) {
            return address;
        }
        return address.substring(0, 8) + "..." + address.substring(address.length() - 6);
    }
}
