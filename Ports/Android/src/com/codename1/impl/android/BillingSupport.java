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
package com.codename1.impl.android;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.codename1.payment.PendingPurchaseCallback;
import com.codename1.payment.Product;
import com.codename1.payment.PurchaseCallback;
import com.codename1.payment.Receipt;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A utility class including all of the billing related functionality for the Play billing library
 * version 4.  {@link CodenameOneActivity} can be overridden to return an instance of this in
 * {@link CodenameOneActivity#createBillingSupport()}.   The default implementation returns null
 * which disables billing support.
 *
 * The build server will strip this class if billing is not enabled.
 * @since 8.0
 */
public class BillingSupport implements IBillingSupport {
    final CodenameOneActivity activity;

    public BillingSupport(CodenameOneActivity activity) {
        this.activity = activity;
    }

    //private final Object lock = new Object();
    private final Inventory inventory = new Inventory();

    //IabHelper mHelper;
    private final PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            // To be implemented in a later section.
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
            } else {
                // Handle any other error codes.
            }

        }
    };

    private BillingClient billingClient;

    private boolean billingConnected;

    @Override
    public void initBilling() {
        if (!activity.isBillingEnabled()) return;
        billingClient= BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new com.android.billingclient.api.BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(com.android.billingclient.api.BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    billingConnected = true;
                    consumeAndAcknowlegePurchases();

                } else {
                    System.err.println("Failed to connect to billing service: "+billingResult.getDebugMessage());
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                billingConnected = false;


            }
        });
    }


    private static boolean isFailure(BillingResult billingResult) {
        return (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK);
    }

    private void consumeAndAcknowlegePurchases(List<Purchase> purchases) {
        for (Purchase pur : purchases) {
            handlePurchase(pur);
        }
    }



    @Override
    public void consumeAndAcknowlegePurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    return;
                }
                if (purchases != null && !purchases.isEmpty()) {
                    consumeAndAcknowlegePurchases(purchases);
                }
            }
        });
    }

    private Set<String> handlingPurchase = new HashSet<String>();

    private void handlePurchase(final Purchase purchase) {
        if (handlingPurchase.contains(purchase.getPurchaseToken())) {
            return;

        }
        handlingPurchase.add(purchase.getPurchaseToken());

        final PurchaseCallback pc = getPurchaseCallback();
        if (!verifyDeveloperPayload(purchase)) {
            if (pc != null && pc instanceof PendingPurchaseCallback) {
                final PendingPurchaseCallback ppc = (PendingPurchaseCallback)pc;
                CN.callSerially(new Runnable() {
                    public void run() {
                        for (String sku : purchase.getSkus()) {
                            ppc.itemPurchaseError(sku, "Invalid developer payload");
                        }

                    }
                });
            }
            handlingPurchase.remove(purchase.getPurchaseToken());
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            // This must be a pending purchase.  We don't do anything here.
            // This will be called again when the purchase completes.
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING && pc != null && pc instanceof PendingPurchaseCallback) {
                final PendingPurchaseCallback ppc = (PendingPurchaseCallback)pc;
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        ppc.itemPurchasePending(purchase.getSkus().iterator().next());
                    }
                });
            }
            handlingPurchase.remove(purchase.getPurchaseToken());
            return;

        }


        final String sku = purchase.getSkus().iterator().next();

        final Runnable onPurchaseAcknowledged = new Runnable() {
            public void run() {
                if (pc != null) {
                    Display.getInstance().callSerially(new Runnable() {

                        @Override
                        public void run() {
                            // Sandbox transactions have no order ID, so we'll make a dummy transaction ID
                            // in this case.
                            String transactionId = (purchase.getOrderId() == null || purchase.getOrderId().isEmpty()) ?
                                    "play-sandbox-"+ UUID.randomUUID().toString() : purchase.getOrderId();
                            String purchaseJsonStr = purchase.getOriginalJson();
                            try {
                                // In order to verify receipts, we'll need both the order data and the signature
                                // so we'll pack it all into a single JSON string.
                                JSONObject purchaseJson = new JSONObject(purchaseJsonStr);
                                JSONObject rootJson = new JSONObject();
                                rootJson.put("data", purchaseJson);
                                rootJson.put("signature", purchase.getSignature());
                                purchaseJsonStr = rootJson.toString();

                            } catch (JSONException ex) {
                                Logger.getLogger(CodenameOneActivity.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            com.codename1.payment.Purchase.postReceipt(Receipt.STORE_CODE_PLAY, sku, transactionId, purchase.getPurchaseTime(), purchaseJsonStr);
                            pc.itemPurchased(sku);
                        }
                    });
                    inventory.add(sku, purchase);
                    //This is a temp hack to get the last purchase raw data
                    //The IAP API needs to be modified to support this on all platforms
                    Display.getInstance().setProperty("lastPurchaseData", purchase.getOriginalJson());
                }
            }
        };
        //check if this product is a non consumable product

        if (!isConsumable(sku)) {
            if (!purchase.isAcknowledged()) {
                billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(), new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(final BillingResult billingResult) {
                        handlingPurchase.remove(purchase.getPurchaseToken());
                        if (isFailure(billingResult)) {
                            final PurchaseCallback pc = getPurchaseCallback();
                            if (pc != null) {
                                Display.getInstance().callSerially(new Runnable() {

                                    @Override
                                    public void run() {

                                        pc.itemPurchaseError(sku, billingResult.getDebugMessage());
                                    }
                                });
                            }
                        } else {
                            onPurchaseAcknowledged.run();
                        }
                    }
                });
            } else {
                handlingPurchase.remove(purchase.getPurchaseToken());
            }
            return;
        }
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(final BillingResult billingResult, String purchaseToken) {
                handlingPurchase.remove(purchase.getPurchaseToken());
                if (isFailure(billingResult)) {
                    final PurchaseCallback pc = getPurchaseCallback();
                    if (pc != null) {
                        Display.getInstance().callSerially(new Runnable() {

                            @Override
                            public void run() {

                                pc.itemPurchaseError(sku, billingResult.getDebugMessage());
                            }
                        });
                    }
                } else {
                    onPurchaseAcknowledged.run();
                }
                if(purchase != null){
                    inventory.erasePurchase(sku);
                }
            }
        };

        if (!purchase.isAcknowledged()) {
            billingClient.consumeAsync(consumeParams, listener);
        } else {
            handlingPurchase.remove(purchase.getPurchaseToken());
        }

    }


    @Override
    public void purchase(final String item) {
        _purchase(item, BillingClient.SkuType.INAPP);
    }

    @Override
    public void subscribe(final String item) {
        _purchase(item, BillingClient.SkuType.SUBS);
    }

    public void _purchase(final String item, final String type) {
        if (!areSubscriptionsSupported() && type.equals(BillingClient.SkuType.SUBS)) {
            final PurchaseCallback pc = getPurchaseCallback();
            if (pc == null) {
                return;
            }
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    pc.itemPurchaseError(item, "Subscriptions are not supported on this device");
                }

            });
            return;
        }

        billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setType(type).setSkusList((List<String>) Arrays.asList(item)).build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(final BillingResult billingResult, final List<SkuDetails> list) {
                if (isFailure(billingResult)) {
                    final PurchaseCallback pc = getPurchaseCallback();
                    if (pc == null) {
                        return;
                    }
                    CN.callSerially(new Runnable() {
                        @Override
                        public void run() {
                            pc.itemPurchaseError(item, billingResult.getDebugMessage());
                        }

                    });
                    return;

                }
                if (list == null || list.isEmpty()) {
                    final PurchaseCallback pc = getPurchaseCallback();
                    if (pc == null) {
                        return;
                    }
                    CN.callSerially(new Runnable() {
                        @Override
                        public void run() {
                            pc.itemPurchaseError(item, "No item could be found in the Playstore with sku "+item);
                        }

                    });
                    return;
                }
                for (SkuDetails details : list) {
                    inventory.add(details, type.equals(BillingClient.SkuType.SUBS) );
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        billingClient.launchBillingFlow(activity,
                                BillingFlowParams.newBuilder()
                                        .setSkuDetails(list.iterator().next()).build()
                        );

                    }
                });

            }
        });

    }

    private class Inventory {
        private final Set<String> subscriptions = new HashSet<String>();
        private final LinkedHashMap<String, Product> products = new LinkedHashMap<String,Product>();
        private final LinkedHashMap<String,Purchase> purchases = new LinkedHashMap<String,Purchase>();


        public synchronized boolean hasDetails(String sku) {
            return products.containsKey(sku);
        }

        public synchronized void add(String sku, Product product) {
            products.put(sku, product);
        }

        public synchronized void add(SkuDetails details) {
            add(details, false);
        }

        public synchronized void add(SkuDetails details, boolean subscription) {
            Product p = new Product();
            p.setSku(details.getSku());
            p.setDescription(details.getDescription());
            p.setDisplayName(details.getTitle());
            p.setLocalizedPrice(details.getPrice());
            add(details.getSku(), p);
            if (subscription) {

                subscriptions.add(details.getSku());
            }
        }

        public synchronized Product getProduct(String sku) {
            return products.get(sku);
        }

        public synchronized boolean hasPurchase(String sku) {
            return purchases.containsKey(sku);
        }

        public synchronized void add(String sku, Purchase purchase) {
            purchases.put(sku, purchase);
        }

        private synchronized Purchase getPurchase(String sku) {
            return purchases.get(sku);
        }

        public synchronized void erasePurchase(String sku) {
            purchases.remove(sku);
        }





        public synchronized void loadSkuDetailsAsync() {
            Set<String> skus = new HashSet<String>();
            skus.addAll(purchases.keySet());
            skus.removeAll(products.keySet());

            if (!skus.isEmpty()) {

                billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setType(BillingClient.SkuType.INAPP).setSkusList(new ArrayList<String>(skus)).build(), new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse( BillingResult billingResult,  List<SkuDetails> list) {
                        if (list != null && !list.isEmpty()) {
                            for (SkuDetails details : list) {
                                add(details);

                            }
                        }
                    }
                });
                if (areSubscriptionsSupported()) {
                    billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setType(BillingClient.SkuType.SUBS).setSkusList(new ArrayList<String>(skus)).build(), new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse( BillingResult billingResult,  List<SkuDetails> list) {
                            if (list != null && !list.isEmpty()) {
                                for (SkuDetails details : list) {

                                    add(details, true);

                                }

                            }

                        }
                    });
                }
            }
        }

        public boolean isSubscription(String sku) {
            return subscriptions.contains(sku);
        }
    }

    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        return true;
    }


    public boolean areSubscriptionsSupported() {
        BillingResult billingResult = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
        return billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    @Override
    public Product[] getProducts(String[] skus, boolean fromCacheOnly){

        if(inventory != null){
            final ArrayList pList = new ArrayList<Product>();
            ArrayList moreskusList = new ArrayList<Product>();
            for (int i = 0; i < skus.length; i++) {
                String sku = skus[i];
                if(inventory.hasDetails(sku)){

                    pList.add(inventory.getProduct(sku));
                }else{
                    moreskusList.add(sku);
                }
            }
            //if the inventory does not all the requestes sku make an update.
            if(moreskusList.size() > 0 && !fromCacheOnly){

                final int[] complete = new int[1];
                final Object lock = new Object();
                billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setType(BillingClient.SkuType.INAPP).setSkusList((List<String>) moreskusList).build(), new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse( BillingResult billingResult, List<SkuDetails> list) {
                        synchronized (lock) {
                            if (isFailure(billingResult)) {
                                complete[0]++;
                                lock.notifyAll();
                                return;
                            }

                            for (SkuDetails details : list) {

                                inventory.add(details);


                            }
                            complete[0]++;
                            lock.notifyAll();
                        }
                    }
                });
                if (areSubscriptionsSupported()) {
                    billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setType(BillingClient.SkuType.SUBS).setSkusList((List<String>) moreskusList).build(), new SkuDetailsResponseListener() {
                        @Override
                        public void onSkuDetailsResponse( BillingResult billingResult, List<SkuDetails> list) {
                            synchronized (lock) {
                                if (isFailure(billingResult)) {
                                    complete[0]++;
                                    lock.notifyAll();
                                    return;
                                }

                                for (SkuDetails details : list) {

                                    inventory.add(details, true);

                                }
                                complete[0]++;
                                lock.notifyAll();
                            }
                        }
                    });
                } else {
                    synchronized (lock) {
                        complete[0]++;
                        lock.notifyAll();
                    }
                }


                while (complete[0] < 2) {
                    CN.invokeAndBlock(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (lock) {
                                while (complete[0] < 2) {
                                    try {
                                        lock.wait(1000);
                                    } catch (InterruptedException ex) {

                                    }

                                }
                            }
                        }
                    });
                }

                //inventory = mHelper.queryInventory(true, moreskusList);
                return getProducts(skus, true);


            }
            Product [] productsOut = new Product[pList.size()];
            productsOut = (Product[]) pList.toArray(productsOut);
            return productsOut;
        }
        return null;
    }

    @Override
    public boolean isConsumable(String sku){

        if (isSubscription(sku) || sku.endsWith("nonconsume")) {
            return false;
        }
        return true;
    }

    public boolean isSubscription(String sku) {
        return inventory.isSubscription(sku);


    }

    @Override
    public boolean wasPurchased(String item) {
        return inventory.hasPurchase(item);

    }

    public PurchaseCallback getPurchaseCallback() {
        Object app = activity.getApp();
        PurchaseCallback pc = app instanceof PurchaseCallback ? (PurchaseCallback) app : null;
        return pc;
    }

    @Override
    public void onDestroy() {
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

}
