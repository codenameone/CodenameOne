/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codenameone.examples.purchasetest;

import com.codename1.io.Storage;
import com.codename1.payment.Product;
import com.codename1.payment.Purchase;
import com.codename1.payment.PendingPurchaseCallback;
import com.codename1.payment.PurchaseCallback;
import com.codename1.payment.RestoreCallback;
import com.codename1.system.Lifecycle;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.annotations.buildhints.*;

/**
 * Minimal Codename One app dedicated to the In-App-Purchase e2e tests.
 *
 * It references com.codename1.payment.* so the platform builders compile the
 * IAP native bridge (iOS: defines CN1_USE_STOREKIT + links StoreKit;
 * Android: pulls in Play Billing), and installs a {@link RecordingReceiptStore}
 * so the iOS StoreKitTest and Android billing-bridge tests can assert that a
 * purchase reached the store. Kept separate from the hellocodenameone sample so
 * IAP wiring never ripples into the screenshot/notification CI workflows.
 */
@Android(licenseKey = "CN1TESTPLACEHOLDERKEYNOTFORPRODUCTIONxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxIDAQAB", useAndroidX = Toggle.ON)
@Ios(applicationQueriesSchemes = {"cydia"}, newStorageLocation = Toggle.ON)
@IosPrivacy(cameraUsageDescription = "Used by the CI smoke test to verify the com.codename1.camera native bridge compiles. The app never opens a camera session.")
public class PurchaseTestApp extends Lifecycle implements PendingPurchaseCallback, RestoreCallback {
    private PurchaseTestSink events;

    @Override
    public void init(Object context) {
        super.init(context);
        try {
            Purchase.getInAppPurchase().setReceiptStore(new RecordingReceiptStore());
            // Drain anything enqueued before the store was installed (the
            // Android fake fires from the activity's onCreate, which can race
            // ahead of this init).
            Purchase.getInAppPurchase().synchronizeReceipts();
            System.out.println("CN1SS:IAP_DIAG installed=true");
        } catch (Throwable t) {
            System.out.println("CN1SS:IAP_DIAG:EXCEPTION " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    @Override
    public void runApp() {
        Form hi = new Form("Purchase Test", BoxLayout.y());
        hi.add(new Label("IAP e2e test app"));
        hi.show();
        PurchaseTestSink s = NativeLookup.create(PurchaseTestSink.class);
        events = (s != null && s.isSupported()) ? s : null;
        if (events != null) {
            // The hosted XCTest drives the purchase API through this channel
            // (PurchaseStoreKit2Tests); nothing is queued on other platforms.
            // Not a UITimer: that is bound to this form, and CommerceCheck
            // shows a paywall after the first purchase, which stopped every
            // later command from being taken.
            new java.util.Timer().schedule(new java.util.TimerTask() {
                public void run() {
                    final String c = events.takeCommand();
                    if (c != null) {
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                runCommand(c);
                            }
                        });
                    }
                }
            }, 200, 200);
        }
    }

    private void event(String e) {
        System.out.println("CN1SS:IAP:EVENT " + e);
        if (events != null) {
            events.recordEvent(e);
        }
    }

    private void runCommand(String c) {
        event("command " + c);
        Purchase p = Purchase.getInAppPurchase();
        if (c.startsWith("purchase:")) {
            p.purchase(c.substring("purchase:".length()));
        } else if (c.startsWith("subscribe:")) {
            p.subscribe(c.substring("subscribe:".length()));
        } else if (c.equals("restore")) {
            p.restore();
        } else if (c.equals("canMakePayments")) {
            event("canMakePayments " + p.isManagedPaymentSupported());
        } else if (c.startsWith("products:")) {
            String[] skus = com.codename1.util.StringUtil.tokenize(c.substring("products:".length()), ',')
                    .toArray(new String[0]);
            Product[] products = p.getProducts(skus);
            if (products == null) {
                event("products null");
                return;
            }
            StringBuilder sb = new StringBuilder("products");
            for (Product pr : products) {
                sb.append(';').append(pr.getSku()).append('|').append(pr.getDisplayName())
                        .append('|').append(pr.getLocalizedPrice());
            }
            event(sb.toString());
        } else if (c.equals("resetState")) {
            // The receipt queue dedupes by transaction id, and StoreKit's local
            // testing restarts its ids when the test clears its transactions.
            String[] files = {"PendingPurchases.dat", "ProcessedPurchases.dat",
                "CN1SubscriptionsData.dat", "CN1SubscriptionsDataRefreshTime.dat", "CN1PurchasedItemList.dat"};
            for (String f : files) {
                Storage.getInstance().deleteStorageFile(f);
            }
            CommerceCheck.reset();
            event("stateReset");
        }
    }

    public void itemPurchased(String sku) { event("itemPurchased " + sku); }
    public void itemPurchaseError(String sku, String errorMessage) { event("itemPurchaseError " + sku + " " + errorMessage); }
    public void itemRefunded(String sku) { event("itemRefunded " + sku); }
    public void itemPurchasePending(String sku) { event("itemPurchasePending " + sku); }
    public void subscriptionStarted(String sku) { event("subscriptionStarted " + sku); }
    public void subscriptionCanceled(String sku) { event("subscriptionCanceled " + sku); }
    public void paymentFailed(String paymentCode, String failureReason) { event("paymentFailed " + paymentCode); }
    public void paymentSucceeded(String paymentCode, double amount, String currency) { event("paymentSucceeded " + paymentCode); }
    public void itemRestored(String sku) { event("itemRestored " + sku); }
    public void restoreRequestComplete() { event("restoreRequestComplete"); }
    public void restoreRequestError(String message) { event("restoreRequestError " + message); }
}
