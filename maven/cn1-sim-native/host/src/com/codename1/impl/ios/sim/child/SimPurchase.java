/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.ios.sim.child;

import com.codename1.impl.ios.sim.bridge.BridgeRegistry;
import com.codename1.io.Storage;
import com.codename1.payment.Product;
import com.codename1.payment.PromotionalOffer;
import com.codename1.payment.Purchase;
import com.codename1.payment.Receipt;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;

import java.util.Vector;

/**
 * Simulator backing for {@link Purchase} - parity with JavaSEPort's anonymous
 * in-app-purchase stub. Capabilities come from the shell's Simulate &gt; In App
 * Purchase menu (via {@link BridgeRegistry}); pay/purchase/subscribe pop CN1
 * confirm dialogs and drive the app's PurchaseCallback, persisting owned items
 * in Storage just like JavaSEPort.
 */
public final class SimPurchase extends Purchase {

    private Vector purchases;

    @Override
    public Product[] getProducts(String[] skus) {
        return null;
    }

    @Override
    public boolean isItemListingSupported() {
        return false;
    }

    @Override
    public boolean isManagedPaymentSupported() {
        return BridgeRegistry.isIapManagedSupported();
    }

    @Override
    public boolean isManualPaymentSupported() {
        return BridgeRegistry.isIapManualSupported();
    }

    @Override
    public boolean isSubscriptionSupported() {
        return BridgeRegistry.isIapSubscriptionSupported();
    }

    @Override
    public boolean isUnsubscribeSupported() {
        return BridgeRegistry.isIapSubscriptionSupported();
    }

    @Override
    public boolean isRefundable(final String sku) {
        if (!BridgeRegistry.isIapRefundSupported()) {
            return false;
        }
        return confirm("Purchase", "Is " + sku + " refundable?");
    }

    private Vector getPurchases() {
        if (purchases == null) {
            purchases = (Vector) Storage.getInstance().readObject("CN1InAppPurchases");
            if (purchases == null) {
                purchases = new Vector();
            }
        }
        return purchases;
    }

    private void savePurchases() {
        if (purchases != null) {
            Storage.getInstance().writeObject("CN1InAppPurchases", purchases);
        }
    }

    private static String uuid() {
        return java.util.UUID.randomUUID().toString();
    }

    private static com.codename1.payment.PurchaseCallback callback() {
        return com.codename1.impl.CodenameOneImplementation.getPurchaseCallback();
    }

    /** Modal CN1 confirm on the EDT, blocking the caller wherever it runs. */
    private static boolean confirm(final String title, final String text) {
        final boolean[] ok = new boolean[1];
        if (Display.getInstance().isEdt()) {
            ok[0] = Dialog.show(title, text, "Accept", "Decline");
        } else {
            Display.getInstance().callSeriallyAndWait(new Runnable() {
                public void run() {
                    ok[0] = Dialog.show(title, text, "Accept", "Decline");
                }
            });
        }
        return ok[0];
    }

    @Override
    public String pay(final double amount, final String currency) {
        if (!BridgeRegistry.isIapManualSupported()) {
            throw new RuntimeException("Manual payment isn't supported check the isManualPaymentSupported() method!");
        }
        boolean accepted = confirm("Payment",
                "A payment of " + amount + " " + currency + " was made\nDo you wish to accept it?");
        final String receipt = accepted ? uuid() : null;
        if (callback() != null) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if (receipt != null) {
                        callback().paymentSucceeded(receipt, amount, currency);
                    } else {
                        callback().paymentFailed(uuid(), null);
                    }
                }
            });
        }
        return receipt;
    }

    @Override
    public void purchase(final String sku) {
        if (!BridgeRegistry.isIapManagedSupported()) {
            throw new RuntimeException("In app purchase isn't supported on this platform!");
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                boolean ok = Dialog.show("Payment",
                        "An in-app purchase of " + sku + " was made\nDo you wish to accept it?",
                        "Accept", "Decline");
                if (ok) {
                    Purchase.postReceipt(Receipt.STORE_CODE_SIMULATOR, sku,
                            "cn1-iap-sim-" + uuid(), System.currentTimeMillis(), "");
                    if (callback() != null) {
                        callback().itemPurchased(sku);
                    }
                    getPurchases().addElement(sku);
                    savePurchases();
                } else if (callback() != null) {
                    callback().itemPurchaseError(sku, "Purchase failed");
                }
            }
        });
    }

    @Override
    public void purchase(String sku, PromotionalOffer promotionalOffer) {
        purchase(sku);
    }

    @Override
    public void refund(final String sku) {
        if (!BridgeRegistry.isIapRefundSupported()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (callback() != null) {
                    callback().itemRefunded(sku);
                }
                getPurchases().removeElement(sku);
                savePurchases();
            }
        });
    }

    @Override
    public void subscribe(final String sku) {
        if (getReceiptStore() != null) {
            purchase(sku);
            return;
        }
        if (!BridgeRegistry.isIapSubscriptionSupported()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                boolean ok = Dialog.show("Payment",
                        "An in-app subscription to " + sku + " was made\nDo you wish to accept it?",
                        "Accept", "Decline");
                if (ok) {
                    if (callback() != null) {
                        callback().subscriptionStarted(sku);
                    }
                    getPurchases().addElement(sku);
                    savePurchases();
                } else if (callback() != null) {
                    callback().itemPurchaseError(sku, "Subscription failed");
                }
            }
        });
    }

    @Override
    public void subscribe(String sku, PromotionalOffer promotionalOffer) {
        subscribe(sku);
    }

    @Override
    public void unsubscribe(final String sku) {
        if (!BridgeRegistry.isIapSubscriptionSupported()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                boolean ok = Dialog.show("Payment",
                        "In-app unsubscription request for " + sku + " was made\nDo you wish to accept it?",
                        "Accept", "Decline");
                if (ok) {
                    if (callback() != null) {
                        callback().subscriptionCanceled(sku);
                    }
                    getPurchases().removeElement(sku);
                    savePurchases();
                } else if (callback() != null) {
                    callback().itemPurchaseError(sku, "Error in unsubscribe");
                }
            }
        });
    }
}
