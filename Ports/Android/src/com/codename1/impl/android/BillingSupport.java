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
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.codename1.payment.PendingPurchaseCallback;
import com.codename1.payment.Product;
import com.codename1.payment.PurchaseCallback;
import com.codename1.payment.Receipt;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;
import com.codename1.util.SuccessCallback;
import java.io.IOException;

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
 * A utility class including all of the billing related functionality for the Play billing
 * library.  {@link CodenameOneActivity} can be overridden to return an instance of this in
 * {@link CodenameOneActivity#createBillingSupport()}.   The default implementation returns null
 * which disables billing support.
 *
 * The build server will strip this class if billing is not enabled.
 * @since 7.0
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
    private AsyncResource pendingConnection;

    @Override
    public void initBilling() {
        requireConnection();
    }
    
    private AsyncResource runWithConnection(final Runnable r) {
        final AsyncResource out = new AsyncResource();
        requireConnection().ready(new SuccessCallback() {
            @Override
            public void onSucess(Object arg0) {
                r.run();
                out.complete(true);
            }
        }).except(new SuccessCallback() {
            public void onSucess(Object arg0) {
                out.error((Throwable)arg0);
            }
        });
        return out;
    }
    
    private AsyncResource requireConnection() {
        final AsyncResource out;
        synchronized(this) {
            if (pendingConnection != null) {
                return pendingConnection;
            }
            out = new AsyncResource();
            pendingConnection = out;
        }
        
        if (!activity.isBillingEnabled()){
            out.error(new UnsupportedOperationException("Billing is not enabled."));
            return out;
        }
        if (billingClient == null) {
            billingClient= BillingClient.newBuilder(activity)
                    .setListener(purchasesUpdatedListener)
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build())
                    .build();
        }
        
        if (billingConnected) {
            out.complete(true);
        } else {
            billingClient.startConnection(new com.android.billingclient.api.BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(com.android.billingclient.api.BillingResult billingResult) {
                    if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                        billingConnected = true;
                        synchronized(BillingSupport.this) {
                            pendingConnection = null;
                        }
                        consumeAndAcknowlegePurchases();
                        
                        out.complete(true);

                    } else {
                        synchronized(BillingSupport.this) {
                            pendingConnection = null;
                        }
                        System.err.println("Failed to connect to billing service: "+billingResult.getDebugMessage());
                        out.error(new IOException(billingResult.getDebugMessage()));
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
        return out;
    }


    /// The query the ProductDetails API takes, built from the plain sku strings the
    /// Codename One payment API deals in. One product type per query: unlike the SKU
    /// API, Play rejects a query mixing INAPP and SUBS products.
    private static QueryProductDetailsParams productQuery(String type, List<String> skus) {
        List<QueryProductDetailsParams.Product> products =
                new ArrayList<QueryProductDetailsParams.Product>();
        for (String sku : skus) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(type)
                    .build());
        }
        return QueryProductDetailsParams.newBuilder().setProductList(products).build();
    }

    /// The price to show for a product, as the old {@code SkuDetails.getPrice()} did.
    ///
    /// The ProductDetails API has no single price, because a product carries offers and
    /// a subscription's offer carries pricing phases. A one-time product has one offer;
    /// a subscription is quoted at the first phase of its first offer, which is the
    /// introductory price when there is one and the recurring price otherwise. Returns
    /// null rather than a fabricated string when Play sends neither, so a caller shows
    /// no price instead of a wrong one.
    private static String formattedPrice(ProductDetails details) {
        ProductDetails.OneTimePurchaseOfferDetails oneTime =
                details.getOneTimePurchaseOfferDetails();
        if (oneTime != null) {
            return oneTime.getFormattedPrice();
        }
        List<ProductDetails.SubscriptionOfferDetails> offers =
                details.getSubscriptionOfferDetails();
        if (offers != null && !offers.isEmpty()) {
            ProductDetails.PricingPhases phases = offers.get(0).getPricingPhases();
            if (phases != null && phases.getPricingPhaseList() != null
                    && !phases.getPricingPhaseList().isEmpty()) {
                return phases.getPricingPhaseList().get(0).getFormattedPrice();
            }
        }
        return null;
    }

    /// The offer token launchBillingFlow needs, or null when Play offered none.
    ///
    /// Both product types can carry one. A subscription is always bought through an
    /// offer and the flow rejects it without a token. A one-time product can now carry
    /// offers too -- `getOneTimePurchaseOfferDetailsList` -- and one configured with
    /// more than a base offer has to name which is being bought, or the flow is
    /// rejected the same way. That is not a subscription-only concern, which is what
    /// the first version of this assumed.
    ///
    /// The default offer is preferred over the list for a one-time product, because
    /// that is the one the removed `setSkuDetails` call would have bought; the list is
    /// only consulted when Play sends no default. A token is returned only when Play
    /// actually supplied one, so a product with no offers still launches the flow with
    /// no token, exactly as before.
    private static String offerToken(ProductDetails details, String type) {
        if (BillingClient.ProductType.SUBS.equals(type)) {
            List<ProductDetails.SubscriptionOfferDetails> offers =
                    details.getSubscriptionOfferDetails();
            if (offers == null || offers.isEmpty()) {
                return null;
            }
            return emptyToNull(offers.get(0).getOfferToken());
        }
        ProductDetails.OneTimePurchaseOfferDetails preferred =
                details.getOneTimePurchaseOfferDetails();
        if (preferred != null && emptyToNull(preferred.getOfferToken()) != null) {
            return preferred.getOfferToken();
        }
        List<ProductDetails.OneTimePurchaseOfferDetails> offers =
                details.getOneTimePurchaseOfferDetailsList();
        if (offers != null) {
            for (ProductDetails.OneTimePurchaseOfferDetails offer : offers) {
                String token = offer == null ? null : emptyToNull(offer.getOfferToken());
                if (token != null) {
                    return token;
                }
            }
        }
        return null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.length() == 0 ? null : value;
    }

    private static boolean isFailure(BillingResult billingResult) {
        return (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK);
    }

    private void consumeAndAcknowlegePurchases(final List<Purchase> purchases) {
        runWithConnection(new Runnable() {
            public void run() {
                for (Purchase pur : purchases) {
                    handlePurchase(pur);
                }
            }
        });
    }



    @Override
    public void consumeAndAcknowlegePurchases() {
        runWithConnection(new Runnable() {
            public void run() {
                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build(),
                        new PurchasesResponseListener() {
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
                        for (String sku : purchase.getProducts()) {
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
                        ppc.itemPurchasePending(purchase.getProducts().iterator().next());
                    }
                });
            }
            handlingPurchase.remove(purchase.getPurchaseToken());
            return;

        }


        final String sku = purchase.getProducts().iterator().next();

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
                runWithConnection(new Runnable() {
                    public void run() {
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
                    }
                });
                
            } else {
                handlingPurchase.remove(purchase.getPurchaseToken());
            }
            return;
        }
        final ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        final ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(final BillingResult billingResult, String purchaseToken) {
                if (purchase != null) handlingPurchase.remove(purchase.getPurchaseToken());
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
            
            runWithConnection(new Runnable() {
                public void run() {
                    billingClient.consumeAsync(consumeParams, listener);
                }
            }).except(new SuccessCallback<Throwable>() {
                public void onSucess(final Throwable t) {
                    if (purchase != null) handlingPurchase.remove(purchase.getPurchaseToken());

                    final PurchaseCallback pc = getPurchaseCallback();
                    if (pc != null) {
                        Display.getInstance().callSerially(new Runnable() {

                            @Override
                            public void run() {

                                pc.itemPurchaseError(sku, t.getMessage());
                            }
                        });
                    }
                    if(purchase != null){
                        inventory.erasePurchase(sku);
                    }
                }

                
            });
            
        } else {
            handlingPurchase.remove(purchase.getPurchaseToken());
        }

    }


    @Override
    public void purchase(final String item) {
        _purchase(item, BillingClient.ProductType.INAPP);
    }

    @Override
    public void subscribe(final String item) {
        _purchase(item, BillingClient.ProductType.SUBS);
    }

    public void _purchase(final String item, final String type) {
        if (!areSubscriptionsSupported() && type.equals(BillingClient.ProductType.SUBS)) {
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

        runWithConnection(new Runnable() {
            public void run() {
                billingClient.queryProductDetailsAsync(productQuery(type, Arrays.asList(item)), new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(final BillingResult billingResult, final QueryProductDetailsResult productDetailsResult) {
                        final List<ProductDetails> list = productDetailsResult.getProductDetailsList();
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
                        for (ProductDetails details : list) {
                            inventory.add(details, type.equals(BillingClient.ProductType.SUBS) );
                        }
                        final ProductDetails details = list.iterator().next();
                        // Both product types can need an offer token; see offerToken. A
                        // subscription with none is not purchasable at all, so that stays a
                        // reported error rather than a flow that opens and is rejected.
                        final String offerToken = offerToken(details, type);
                        if (type.equals(BillingClient.ProductType.SUBS) && offerToken == null) {
                            final PurchaseCallback pc = getPurchaseCallback();
                            if (pc == null) {
                                return;
                            }
                            CN.callSerially(new Runnable() {
                                @Override
                                public void run() {
                                    pc.itemPurchaseError(item, "No purchasable offer is available for subscription " + item);
                                }
                            });
                            return;
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BillingFlowParams.ProductDetailsParams.Builder productParams =
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                .setProductDetails(details);
                                if (offerToken != null) {
                                    productParams.setOfferToken(offerToken);
                                }
                                billingClient.launchBillingFlow(activity,
                                        BillingFlowParams.newBuilder()
                                                .setProductDetailsParamsList(
                                                        Arrays.asList(productParams.build()))
                                                .build()
                                );

                            }
                        });

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

        public synchronized void add(ProductDetails details) {
            add(details, false);
        }

        public synchronized void add(ProductDetails details, boolean subscription) {
            Product p = new Product();
            p.setSku(details.getProductId());
            p.setDescription(details.getDescription());
            p.setDisplayName(details.getTitle());
            p.setLocalizedPrice(formattedPrice(details));
            add(details.getProductId(), p);
            if (subscription) {

                subscriptions.add(details.getProductId());
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





        public synchronized void loadProductDetailsAsync() {
            final Set<String> skus = new HashSet<String>();
            skus.addAll(purchases.keySet());
            skus.removeAll(products.keySet());

            if (!skus.isEmpty()) {
                runWithConnection(new Runnable() {
                    public void run() {
                        billingClient.queryProductDetailsAsync(productQuery(BillingClient.ProductType.INAPP, new ArrayList<String>(skus)), new ProductDetailsResponseListener() {
                            @Override
                            public void onProductDetailsResponse( BillingResult billingResult,  QueryProductDetailsResult productDetailsResult) {
                                List<ProductDetails> list = productDetailsResult.getProductDetailsList();
                                if (list != null && !list.isEmpty()) {
                                    for (ProductDetails details : list) {
                                        add(details);

                                    }
                                }
                            }
                        });
                        if (areSubscriptionsSupported()) {
                            billingClient.queryProductDetailsAsync(productQuery(BillingClient.ProductType.SUBS, new ArrayList<String>(skus)), new ProductDetailsResponseListener() {
                                @Override
                                public void onProductDetailsResponse( BillingResult billingResult,  QueryProductDetailsResult productDetailsResult) {
                                    List<ProductDetails> list = productDetailsResult.getProductDetailsList();
                                    if (list != null && !list.isEmpty()) {
                                        for (ProductDetails details : list) {

                                            add(details, true);

                                        }

                                    }

                                }
                            });
                        }
                    }
                });
                
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
            final ArrayList moreskusList = new ArrayList<Product>();
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
                runWithConnection(new Runnable() {
                    public void run() {
                        billingClient.queryProductDetailsAsync(productQuery(BillingClient.ProductType.INAPP, (List<String>) moreskusList), new ProductDetailsResponseListener() {
                            @Override
                            public void onProductDetailsResponse( BillingResult billingResult, QueryProductDetailsResult productDetailsResult) {
                                List<ProductDetails> list = productDetailsResult.getProductDetailsList();
                                synchronized (lock) {
                                    if (isFailure(billingResult)) {
                                        complete[0]++;
                                        lock.notifyAll();
                                        return;
                                    }

                                    for (ProductDetails details : list) {

                                        inventory.add(details);


                                    }
                                    complete[0]++;
                                    lock.notifyAll();
                                }
                            }
                        });
                    }
                    
                }).except(new SuccessCallback<Throwable>() {
                    public void onSucess(Throwable t) {
                        synchronized(lock) {
                            complete[0]++;
                            lock.notifyAll();
                        }
                        
                    }
                });
                
                if (areSubscriptionsSupported()) {
                    runWithConnection(new Runnable() {
                        public void run() {
                            billingClient.queryProductDetailsAsync(productQuery(BillingClient.ProductType.SUBS, (List<String>) moreskusList), new ProductDetailsResponseListener() {
                                @Override
                                public void onProductDetailsResponse( BillingResult billingResult, QueryProductDetailsResult productDetailsResult) {
                                    List<ProductDetails> list = productDetailsResult.getProductDetailsList();
                                    synchronized (lock) {
                                        if (isFailure(billingResult)) {
                                            complete[0]++;
                                            lock.notifyAll();
                                            return;
                                        }

                                        for (ProductDetails details : list) {

                                            inventory.add(details, true);

                                        }
                                        complete[0]++;
                                        lock.notifyAll();
                                    }
                                }
                            });
                        }
                    }).except(new SuccessCallback<Throwable>() {
                        public void onSucess(Throwable t) {
                            synchronized(lock) {
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
