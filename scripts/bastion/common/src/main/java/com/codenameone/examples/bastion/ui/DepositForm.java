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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codenameone.examples.bastion.api.ChainTransferDto;
import com.codenameone.examples.bastion.api.DepositAddressDto;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;

import java.util.List;

/// Receiving coins: the customer's own deposit address on the real network,
/// as text to copy and as a QR code a wallet scans, and every transfer that
/// crossed the chain with its status and transaction hash.
///
/// The network's name is in large print above the address. Coins sent on the
/// wrong network -- a test network's address used on mainnet, or the reverse
/// -- arrive at an address the bank holds on neither, and are the most common
/// way customers lose them.
public final class DepositForm {
    private static final String ASSET = "ETH";

    private DepositForm() {
    }

    public static void show(final Runnable back) {
        Form form = Nav.form("Receive crypto", back);
        final Container content = new Container(BoxLayout.y());
        form.add(content);
        Ui.show(form, true);
        load(content);
    }

    private static void load(final Container content) {
        Ui.loading(content);
        Api.exchange().depositAddress(ASSET, Net.to((DepositAddressDto d) ->
                Api.exchange().chainTransfers(Net.to((List<ChainTransferDto> moves) ->
                        fill(content, d, moves),
                        (status, message) -> fill(content, d, null))),
                (status, message) -> Ui.failed(content, message, () -> load(content))));
    }

    private static void fill(Container content, DepositAddressDto d, List<ChainTransferDto> moves) {
        content.removeAll();
        if (!d.live()) {
            content.add(Ui.wrapped(Lang.tr("Coins move on a simulated chain in this server. "
                    + "Connect it to an Ethereum node to receive real Ether here."), "BnRowDetail"));
            content.revalidate();
            return;
        }
        content.add(Ui.section("Network"));
        content.add(Ui.row(Lang.tr(d.network()), Lang.tr("Send only {0} on this network to this "
                + "address. Coins sent on another network are lost.", d.asset()),
                Lang.tr("Chain {0}", String.valueOf(d.chainId())), "BnBadgeWarn", null));
        // EIP-681: a wallet that scans this fills in the address and refuses
        // to send on a different chain.
        String uri = "ethereum:" + d.address() + "@" + d.chainId();
        byte[] bytes = ascii(uri);
        if (bytes.length <= QrCode.MAX_BYTES) {
            Container qr = new Container(new FlowLayout(Container.CENTER));
            qr.add(new QrView(QrCode.encode(bytes)));
            content.add(qr);
        }
        content.add(Ui.copyable(Lang.tr("Your {0} address", d.asset()), d.address(), false));
        content.add(Ui.wrapped(Lang.tr("Credited after {0} confirmations, usually a few minutes.",
                String.valueOf(d.confirmations())), "BnRowDetail"));
        content.add(Ui.section("On the chain"));
        if (moves == null || moves.isEmpty()) {
            content.add(Ui.empty("Nothing has crossed the chain yet."));
        } else {
            for (ChainTransferDto t : moves) {
                content.add(row(t));
            }
        }
        content.revalidate();
    }

    private static Container row(ChainTransferDto t) {
        boolean in = "IN".equals(t.direction());
        String title = (in ? "+" : "-") + Amounts.format(t.amount(), t.asset());
        String detail = (in ? Lang.tr("Deposit") : Lang.tr("To {0}", t.address()))
                + (t.txHash().length() > 0 ? "\n" + t.txHash() : "")
                + (t.detail().length() > 0 ? "\n" + t.detail() : "");
        String style = "CONFIRMED".equals(t.status()) ? "BnBadgeGood"
                : "FAILED".equals(t.status()) ? "BnBadgeBad" : "BnBadgeWarn";
        Container row = Ui.row(title, detail, Lang.tr(statusName(t.status())), style,
                t.txHash().length() > 0 ? () -> Ui.copy(t.txHash(), false) : null);
        return row;
    }

    static String statusName(String status) {
        if ("CONFIRMED".equals(status)) {
            return "Final";
        }
        if ("FAILED".equals(status)) {
            return "Failed";
        }
        if ("BROADCAST".equals(status)) {
            return "Confirming";
        }
        return "Sending";
    }

    private static byte[] ascii(String text) {
        byte[] out = new byte[text.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) text.charAt(iter);
        }
        return out;
    }
}
