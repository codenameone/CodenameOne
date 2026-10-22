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
package com.codenameone.examples.skirmish.engage;

import com.codename1.io.Log;
import com.codename1.payment.Purchase;
import com.codename1.payment.Receipt;
import com.codename1.ui.CN;
import com.codenameone.examples.skirmish.api.ReceiptDto;
import com.codenameone.examples.skirmish.api.WalletDto;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import java.util.List;

/// In-app purchases, through `com.codename1.payment`.
///
/// The store takes the money; the game's server grants what was bought. A
/// purchase the device reports is sent to the server as a receipt, the server
/// asks Apple or Google whether it is real, and only then adds the gems --
/// once, however many times the receipt arrives. A client that claims a
/// purchase it did not make gets nothing.
///
/// The main class implements `PurchaseCallback`, which is how Codename One
/// reports a finished purchase; it calls [#purchased].
// tag::skirmish-store[]
public final class Store {
    /// Told about the wallet after the server granted a purchase.
    public interface Granted {
        void granted(WalletDto wallet);
    }

    private static Granted listener;

    private Store() {
    }

    public static void listen(Granted granted) {
        listener = granted;
    }

    /// Whether a real store takes the money. The simulator's own store stands
    /// in for one only when the app's main class is running to hear back, so
    /// here the simulator takes the simulated-receipt path instead, which
    /// exercises the same server validation.
    public static boolean available() {
        Purchase p = Purchase.getInAppPurchase();
        return !CN.isSimulator() && p != null && p.isManagedPaymentSupported();
    }

    /// Starts buying `sku` from the platform's store. In the simulator the
    /// purchase completes at once with a simulated receipt, which the server
    /// accepts on its development profiles only.
    public static void buy(String sku) {
        if (available()) {
            Purchase.getInAppPurchase().purchase(sku);
        } else {
            redeem(new ReceiptDto(Receipt.STORE_CODE_SIMULATOR, sku,
                    "sim-" + System.currentTimeMillis(), ""));
        }
    }

    /// From `PurchaseCallback.itemPurchased`: finds the store's receipt and
    /// sends it to the server.
    public static void purchased(String sku) {
        Telemetry.purchase(sku);
        Purchase p = Purchase.getInAppPurchase();
        if (p == null) {
            return;
        }
        Receipt[] receipts = p.getReceipts(sku);
        if (receipts == null || receipts.length == 0) {
            return;
        }
        Receipt r = receipts[receipts.length - 1];
        redeem(new ReceiptDto(r.getStoreCode(), sku, r.getTransactionId(), r.getOrderData()));
    }

    /// Sends every receipt the store still holds. The server grants each only
    /// once, so this is safe to repeat; it picks up a purchase whose first
    /// report was lost when the app was killed mid-way.
    public static void resend() {
        Purchase p = Purchase.getInAppPurchase();
        if (p == null || !available()) {
            return;
        }
        List<Receipt> all = p.getReceipts();
        for (int i = 0; i < all.size(); i++) {
            Receipt r = all.get(i);
            redeem(new ReceiptDto(r.getStoreCode(), r.getSku(), r.getTransactionId(), r.getOrderData()));
        }
    }

    private static void redeem(ReceiptDto receipt) {
        Api.shop().receipt(receipt, Net.to(wallet -> {
            if (listener != null) {
                CN.callSerially(() -> listener.granted(wallet));
            }
        }, (status, message) -> Log.p("Receipt refused: " + status + " " + message)));
    }
}
// end::skirmish-store[]
