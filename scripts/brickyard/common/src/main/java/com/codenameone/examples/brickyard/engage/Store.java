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
package com.codenameone.examples.brickyard.engage;

import com.codename1.payment.CommerceManager;
import com.codename1.payment.Purchase;
import com.codename1.payment.Receipt;
import com.codename1.ui.CN;
import com.codenameone.examples.brickyard.AppConfig;
import com.codenameone.examples.brickyard.api.ReceiptDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;

/// In-app purchases. Bricks are consumables bought with
/// `com.codename1.payment.Purchase`; Brickyard Plus is a subscription sold
/// through [CommerceManager] and its paywall. The server credits Bricks from
/// the store's own notifications, so after a purchase the app only re-reads
/// the account.
public final class Store {
    private static Runnable changed;

    private Store() {
    }

    /// Called on the EDT whenever the balance may have changed.
    public static void listen(Runnable onChange) {
        changed = onChange;
    }

    public static boolean available() {
        Purchase p = Purchase.getInAppPurchase();
        return !CN.isSimulator() && p != null && p.isManagedPaymentSupported();
    }

    public static void buy(String sku) {
        if (available()) {
            Purchase.getInAppPurchase().purchase(sku);
        }
    }

    public static boolean plus() {
        return CommerceManager.getInstance().isEntitled(AppConfig.ENTITLEMENT_PLUS);
    }

    public static void purchased(String sku) {
        Telemetry.purchase(sku);
        redeem(sku);
        refresh();
    }

    /// Hands the store's receipt for `sku` to the server, which validates it
    /// and credits the wallet. The store's own notification credits it too;
    /// the server keys both on the transaction id, so it is credited once.
    static void redeem(String sku) {
        Purchase p = Purchase.getInAppPurchase();
        if (p == null || Session.user() == null) {
            return;
        }
        Receipt latest = null;
        for (Receipt r : p.getReceipts(sku)) {
            if (latest == null || (r.getPurchaseDate() != null && latest.getPurchaseDate() != null
                    && r.getPurchaseDate().getTime() > latest.getPurchaseDate().getTime())) {
                latest = r;
            }
        }
        if (latest == null) {
            return;
        }
        Api.economy().redeemReceipt(new ReceiptDto(latest.getStoreCode(), sku, latest.getTransactionId(),
                latest.getOrderData()), Net.to(wallet -> {
                    if (changed != null) {
                        CN.callSerially(changed);
                    }
                }, (status, message) -> { }));
    }

    public static void refresh() {
        if (Session.user() == null) {
            return;
        }
        Session.refreshUser(() -> {
            if (changed != null) {
                CN.callSerially(changed);
            }
        }, (status, message) -> { });
    }
}
