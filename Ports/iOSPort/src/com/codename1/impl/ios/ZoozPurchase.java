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
package com.codename1.impl.ios;

import com.codename1.io.Log;
import com.codename1.payment.*;
import com.codename1.processing.Result;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.io.JSONParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the purchase API
 */
class ZoozPurchase extends Purchase implements Runnable {

    private String purchaseId = null;
    private static final Object LOCK = new Object();
    private static boolean completed = false;
    private static boolean fetchProductsFailed;
    private static boolean fetchProductsComplete;
    private static String fetchProductsFailedMessage;

    private IOSNative nativeInstance;
    private IOSImplementation ioImpl;
    private static String transactionId;
    private static double amount;
    private static String errorMessage;
    private PurchaseCallback callback;
    public ZoozPurchase(IOSImplementation ioImpl, IOSNative nativeInstance, PurchaseCallback callback) {
        this.nativeInstance = nativeInstance;
        this.ioImpl = ioImpl;
        this.callback = callback;
    }

    @Override
    public String getStoreCode() {
        return Receipt.STORE_CODE_ITUNES;
    }

    public boolean isManagedPaymentSupported() {
        return nativeInstance.canMakePayments();
    }

    public boolean isItemListingSupported() {
        return true;
    }

    static void paymentSuccessWithResponse(String t, float total) {
        transactionId = t;
        amount = total;
        completed = true;
        synchronized(LOCK) {
            LOCK.notify();
        }
    }

    static void paymentCanceledOrFailed(String error) {
        transactionId = null;
        errorMessage = error;
        completed = true;
        synchronized(LOCK) {
            LOCK.notify();
        }
    }

    static void fetchProductsCanceledOrFailed(String error) {
        fetchProductsFailedMessage = error;
        fetchProductsFailed = true;
    }

    static void fetchProductsComplete(){
        fetchProductsComplete=true;
    }

    public synchronized Product[] getProducts(String[] skus) {
        if (nativeInstance.storeKit2Active()) {
            return getProductsStoreKit2(skus);
        }
        int numSkus = skus.length;
        final Product[] p = new Product[numSkus];
        for(int iter = 0; iter < numSkus; iter++) {
            p[iter] = new Product();
        }
        fetchProductsFailed = false;
        fetchProductsComplete = false;
        nativeInstance.fetchProducts(skus, p);

        // wait for request to complete
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while(!fetchProductsFailed && !fetchProductsComplete) {
                    try {
                        Thread.currentThread().sleep(10);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        if(fetchProductsFailed) {
            Log.e(new IOException("Failed to fetch products: " + fetchProductsFailedMessage));
            return null;
        }
        return p;
    }

    /// StoreKit 2 product lookup. The answer is matched to the request by sku,
    /// never by position: the store answers in any order and leaves out skus
    /// it does not know (the StoreKit 1 path above fills by position, which
    /// is why a missing product there shifts every later one). A sku the
    /// store did not return keeps a `Product` with only its sku set.
    private Product[] getProductsStoreKit2(final String[] skus) {
        final String[] answer = new String[1];
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                answer[0] = nativeInstance.storeKit2FetchProducts(skus);
            }
        });
        Map<String, Object> m = parseJson(answer[0]);
        if (m == null || m.get("error") != null) {
            Log.e(new IOException("Failed to fetch products: "
                    + (m == null ? "no answer" : m.get("error"))));
            return null;
        }
        Map<String, Map<String, Object>> bySku = new HashMap<String, Map<String, Object>>();
        Object list = m.get("products");
        if (list instanceof List) {
            for (Object o : (List) list) {
                if (o instanceof Map) {
                    Map<String, Object> pm = (Map<String, Object>) o;
                    Object sku = pm.get("sku");
                    if (sku instanceof String) {
                        bySku.put((String) sku, pm);
                    }
                }
            }
        }
        Product[] out = new Product[skus.length];
        for (int i = 0; i < skus.length; i++) {
            Product p = new Product();
            p.setSku(skus[i]);
            Map<String, Object> pm = bySku.get(skus[i]);
            if (pm != null) {
                p.setDisplayName(str(pm.get("displayName")));
                p.setDescription(str(pm.get("description")));
                p.setLocalizedPrice(str(pm.get("localizedPrice")));
            }
            out[i] = p;
        }
        return out;
    }

    private static String str(Object o) {
        return o instanceof String ? (String) o : null;
    }

    private static Map<String, Object> parseJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            return parser.parseJSON(new InputStreamReader(
                    new ByteArrayInputStream(json.getBytes("UTF-8")), "UTF-8"));
        } catch (IOException ex) {
            Log.e(ex);
            return null;
        }
    }

    private static boolean storeKit2EventsStarted;

    /// Starts draining StoreKit 2's events, once, when this run uses StoreKit 2.
    /// Called from `IOSImplementation.setMainClass`, after the purchase and
    /// restore callbacks are known, so a transaction StoreKit delivers at
    /// launch (an unfinished one, an approved Ask to Buy, a renewal) reaches the
    /// app's callback rather than nothing.
    static void startStoreKit2Events() {
        if (storeKit2EventsStarted || !IOSImplementation.nativeInstance.storeKit2Active()) {
            return;
        }
        storeKit2EventsStarted = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Display.isInitialized()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        // keep waiting
                    }
                }
                while (true) {
                    String e = IOSImplementation.nativeInstance.storeKit2NextEvent(1000);
                    if (e == null) {
                        continue;
                    }
                    final Map<String, Object> event = parseJson(e);
                    if (event == null) {
                        continue;
                    }
                    CN.callSerially(new Runnable() {
                        @Override
                        public void run() {
                            dispatchStoreKit2Event(event);
                        }
                    });
                }
            }
        }, "StoreKit2");
        t.start();
    }

    /// Runs on the EDT. A transaction is acknowledged (and StoreKit finishes
    /// it) only after its receipt is in `Purchase`'s persistent pending queue,
    /// which `postReceipt` writes synchronously when called on the EDT. A crash
    /// before that leaves the transaction unfinished, and StoreKit delivers it
    /// again on the next launch; the pending queue drops a transaction id it
    /// already holds, so a redelivery is never stored twice.
    static void dispatchStoreKit2Event(Map<String, Object> event) {
        String type = str(event.get("type"));
        String sku = str(event.get("sku"));
        String transactionId = str(event.get("transactionId"));
        if (type == null) {
            return;
        }
        if ("purchased".equals(type)) {
            Purchase.postReceipt(Receipt.STORE_CODE_ITUNES, sku, transactionId,
                    date(event.get("date")), str(event.get("jws")));
            IOSImplementation.itemPurchased(sku);
            IOSImplementation.nativeInstance.storeKit2Finish(transactionId);
        } else if ("superseded".equals(type)) {
            // Upgraded within its group, or already expired when delivered --
            // an app not opened for months drains its old renewals this way.
            // The receipt store still learns of it, but it is not a purchase:
            // no analytics event, and no CommerceManager purchase context is
            // spent on it.
            Purchase.postRestoredReceipt(Receipt.STORE_CODE_ITUNES, sku, transactionId,
                    date(event.get("date")), str(event.get("jws")));
            IOSImplementation.nativeInstance.storeKit2Finish(transactionId);
        } else if ("refunded".equals(type)) {
            IOSImplementation.itemRefunded(sku);
            IOSImplementation.nativeInstance.storeKit2Finish(transactionId);
        } else if ("restored".equals(type)) {
            // Posted so a receipt store (and CommerceManager) learns of
            // purchases made on another device or before a reinstall, which is
            // what restore is for. Not a purchase for analytics.
            Purchase.postRestoredReceipt(Receipt.STORE_CODE_ITUNES, sku, transactionId,
                    date(event.get("date")), str(event.get("jws")));
            IOSImplementation.itemRestored(sku);
        } else if ("restoreComplete".equals(type)) {
            IOSImplementation.restoreRequestComplete();
        } else if ("restoreError".equals(type)) {
            IOSImplementation.restoreRequestError(str(event.get("error")));
        } else if ("purchaseError".equals(type)) {
            IOSImplementation.itemPurchaseError(sku, str(event.get("error")));
        } else if ("cancelled".equals(type)) {
            // As on StoreKit 1, a purchase the user cancels reports nothing.
            Log.p("Purchase of " + sku + " cancelled by the user");
        } else if ("pending".equals(type)) {
            // Ask to Buy / Strong Customer Authentication. The approved
            // transaction arrives later as a "purchased" event.
            Log.p("Purchase of " + sku + " is pending approval");
            IOSImplementation.itemPurchasePending(sku);
        }
    }

    private static long date(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }

    public boolean wasPurchased(String sku) {
        return ioImpl.getPurchased().contains(sku);
    }

    @Override
    public void purchaseImpl(String sku) {
        nativeInstance.purchase(sku);
    }

    @Override
    public void purchaseImpl(String sku, PromotionalOffer promotionalOffer) {
        if (promotionalOffer == null) {
            nativeInstance.purchase(sku);
            return;
        }

        if (!(promotionalOffer instanceof ApplePromotionalOffer)) {
            throw new RuntimeException("Unsupported promotional offer.  Expected ApplePromotionalOffer but received " + promotionalOffer.getClass());
        }

        nativeInstance.purchase(skuAndPromotionalOfferToJSON(sku, (ApplePromotionalOffer) promotionalOffer));
    }

    public void subscribe(String sku) {
        if (getReceiptStore() != null) {
            purchase(sku);
            return;
        }
        super.subscribe(sku);
    }

    @Override
    public void subscribe(String sku, PromotionalOffer promotionalOffer) {
        if (promotionalOffer == null) {
            subscribe(sku);
            return;
        }

        if (!(promotionalOffer instanceof ApplePromotionalOffer)) {
            throw new RuntimeException("Unsupported promotional offer.  Expected ApplePromotionalOffer but received " + promotionalOffer.getClass());
        }

        nativeInstance.purchase(skuAndPromotionalOfferToJSON(sku, (ApplePromotionalOffer) promotionalOffer));
    }

    public boolean isSubscriptionSupported() {
        return getReceiptStore() != null;
    }

    public boolean isUnsubscribeSupported() {
        return false;
    }

    @Override
    public boolean isManualPaymentSupported() {
        return true;
    }

    @Override
    public String pay(double amount, final String currency, String invoiceNumber) {
        String zoozAppKey = Display.getInstance().getProperty("ZoozAppKey", "");
        boolean isSandBox = Display.getInstance().getProperty("ZoozSandBox", "true").equals("true");
        nativeInstance.zoozPurchase(amount, currency, zoozAppKey, isSandBox, invoiceNumber);
        Display.getInstance().invokeAndBlock(this);
        // use call serially so the purchase callback happens on the 
        // next EDT loop AFTER the value was returned 
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    if (errorMessage != null) {
                        callback.paymentFailed(purchaseId, errorMessage);
                    } else {
                        // iOS port doesn't return the currency in its callback
                        callback.paymentSucceeded(purchaseId, ZoozPurchase.this.amount, currency);
                    }
                }
            }
        });
        return transactionId;
    }

    @Override
    public String pay(double amount, String currency) {
        return pay(amount, currency, "N/A");
    }

    @Override
    public void run() {
        synchronized (LOCK) {
            while (!completed) {
                try {
                    LOCK.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    @Override
    public boolean isRestoreSupported() {
        return true;
    }

    @Override
    public void restore() {
        nativeInstance.restorePurchases();
    }

    @Override
    public boolean isManageSubscriptionsSupported() {
        return true;
    }

    @Override
    public void manageSubscriptions(String sku) {
        if ("mac".equals(Display.getInstance().getPlatformName())) {
            // The native macOS port. StoreKit has no manage-subscriptions sheet
            // there and itms-apps is an iOS scheme; the web address is the one
            // macOS routes to the App Store's subscriptions page.
            CN.execute("https://apps.apple.com/account/subscriptions");
            return;
        }
        CN.execute("itms-apps://apps.apple.com/account/subscriptions");
    }

    private String skuAndPromotionalOfferToJSON(String sku, ApplePromotionalOffer promotionalOffer) {
        Map<String,Object> payload = new HashMap<String, Object>();
        payload.put("sku", sku);
        Map<String,Object> promo = new HashMap<String,Object>();
        promo.put("offerIdentifier", promotionalOffer.getOfferIdentifier());
        promo.put("keyIdentifier", promotionalOffer.getKeyIdentifier());
        promo.put("nonce", promotionalOffer.getNonce());
        promo.put("signature", promotionalOffer.getSignature());
        promo.put("timestamp", promotionalOffer.getTimestamp());
        payload.put("promotionalOffer", promo);
        return Result.fromContent(payload).toString().trim();
    }
}
