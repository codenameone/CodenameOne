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
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.Net;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.ApprovalRequestDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;

/// The manager PIN prompt: a manager types their e-mail and PIN at the
/// register and the server grants a one-use approval whose id the sale,
/// refund or edit then carries. The PIN never leaves this dialog except in
/// the approval request itself, and it is never stored.
final class ManagerApproval {
    /// Receives the id of a granted approval.
    interface Approved {
        void approved(String approvalId);
    }

    private ManagerApproval() {
    }

    /// Asks for a manager's approval of `kind` over `amountCents`.
    static void ask(final String kind, final long amountCents, final String reason,
            final Approved done) {
        final Dialog d = new Dialog(Lang.tr("Manager approval"), BoxLayout.y());
        d.setName("pos-approval");
        d.add(PosUi.label(why(kind), "PosMuted"));
        if (amountCents > 0) {
            d.add(PosUi.data(PosState.money(amountCents), "PosTitle"));
        }
        final TextField who = new TextField("", Lang.tr("Manager e-mail"), 20, TextField.EMAILADDR);
        who.setUIID("PosField");
        who.setName("pos-approval-user");
        final TextField pin = new TextField("", Lang.tr("PIN"), 6,
                TextField.NUMERIC | TextField.PASSWORD);
        pin.setUIID("PosField");
        pin.setName("pos-approval-pin");
        final Label message = new Label("", "PosError");
        message.setName("pos-approval-message");
        d.add(who).add(pin).add(message);
        d.add(PosUi.key(Lang.tr("Approve"), "pos-approval-ok", "PosPay", e -> {
            String p = pin.getText().trim();
            if (p.length() < 4) {
                message.setText(Lang.tr("A PIN has 4 to 6 digits"));
                d.revalidateLater();
                return;
            }
            message.setText(Lang.tr("Checking..."));
            pin.setText("");
            Api.posTools().approve(PosState.storeId(), PosState.registerId(),
                    new ApprovalRequestDto(who.getText().trim(), p, kind, amountCents,
                            reason == null ? "" : reason), Net.to(a -> {
                                d.dispose();
                                if (a != null && a.id() != null) {
                                    done.approved(a.id());
                                }
                            }, (status, text) -> {
                                message.setText(status <= 0
                                        ? Lang.tr("An approval needs the network")
                                        : status == 401 || status == 403
                                        ? Lang.tr("That manager or PIN is not right")
                                        : status == 423
                                        ? Lang.tr("Too many wrong PINs. Try again in 15 minutes.")
                                        : Lang.tr(text));
                                d.revalidateLater();
                            }));
        }));
        d.add(PosUi.key(Lang.tr("Cancel"), "pos-approval-cancel", "PosChoice", e -> d.dispose()));
        d.show();
    }

    static String why(String kind) {
        if (PosRules.REFUND.equals(kind)) {
            return "Refunds need a manager";
        }
        if (PosRules.ORDER_EDIT.equals(kind)) {
            return "Editing an order needs a manager";
        }
        if (PosRules.PRICE_OVERRIDE.equals(kind)) {
            return "A changed price needs a manager";
        }
        return "This discount needs a manager";
    }

    /// Whether `status` is the server asking for an approval the register
    /// did not know it needed (its cached policy was older than the store's).
    static boolean wanted(int status, String message) {
        return status == 403 && message != null && message.indexOf("approval") >= 0;
    }
}
