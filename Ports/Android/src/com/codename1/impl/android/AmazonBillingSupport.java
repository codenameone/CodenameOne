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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.ProductType;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserData;
import com.amazon.device.iap.model.UserDataResponse;
import com.codename1.payment.PendingPurchaseCallback;
import com.codename1.payment.Product;
import com.codename1.payment.PurchaseCallback;
import com.codename1.payment.Receipt;
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * In-app purchase over the Amazon Appstore SDK, for an application built with the
 * {@code android.billing.store=amazon} build hint.
 *
 * <p>This is the Amazon counterpart of {@link BillingSupport} and is selected the
 * same way: the builder generates the application stub's
 * {@link CodenameOneActivity#createBillingSupport()} to construct one or the
 * other, by name, so no class is looked up at run time and obfuscation cannot
 * break the choice. Like {@code BillingSupport} it ships as source and is
 * compiled only inside an application build that asked for it -- the builder
 * deletes it everywhere else, because the Amazon SDK is not on any other
 * build's class path.</p>
 *
 * <p>Differences from Play that shape the code:</p>
 * <ul>
 * <li>There is no connection to open. The SDK answers every request through the
 * one registered {@link PurchasingListener}, always on the main thread.</li>
 * <li>One {@code purchase} call covers consumables, entitlements and
 * subscriptions; the product's type is Amazon's to know.</li>
 * <li>A receipt is settled with {@code notifyFulfillment}, which is what stops
 * Amazon from delivering it again. It is sent only after the receipt reached
 * the application, so a process that dies in between gets the receipt again on
 * its next launch instead of losing the purchase.</li>
 * <li>{@code getPurchaseUpdates(true)} returns every entitlement and
 * subscription the customer has, cancelled ones included, on every launch.
 * The receipt ids already handed to the application are remembered so that a
 * launch does not replay old purchases as new ones.</li>
 * </ul>
 */
public class AmazonBillingSupport implements IBillingSupport {
    private static final String TAG = "Codename One";

    /** Preferences holding the receipt ids already reported to the application. */
    private static final String PREFS = "cn1_amazon_iap";
    private static final String KEY_DELIVERED = "delivered";
    private static final String KEY_CANCELED = "canceled";

    /**
     * Amazon documents a ceiling of 100 SKUs per product data request and fails
     * the whole request above it.
     */
    private static final int MAX_SKUS_PER_REQUEST = 100;

    /**
     * How long a product listing waits for Amazon. A device without the Appstore
     * client may never answer, and the caller is blocked until this returns.
     */
    private static final long PRODUCT_TIMEOUT_MILLIS = 20000;

    private final CodenameOneActivity activity;
    private final Object lock = new Object();

    // All guarded by lock: the listener runs on the main thread, the
    // application asks from the EDT.
    private final Map<String, Product> products = new HashMap<String, Product>();
    private final Map<String, ProductType> productTypes = new HashMap<String, ProductType>();
    private final Set<String> owned = new HashSet<String>();
    private final Set<String> pendingProductRequests = new HashSet<String>();
    private final Map<String, String> purchaseRequests = new HashMap<String, String>();
    private String userId;

    private boolean registered;
    /** The first query of a process asks for everything, later ones for what changed. */
    private boolean fullQueryDone;

    private final PurchasingListener listener = new PurchasingListener() {
        @Override
        public void onUserDataResponse(UserDataResponse response) {
            if (response != null
                    && response.getRequestStatus() == UserDataResponse.RequestStatus.SUCCESSFUL) {
                rememberUser(response.getUserData());
            }
        }

        @Override
        public void onProductDataResponse(ProductDataResponse response) {
            handleProductData(response);
        }

        @Override
        public void onPurchaseResponse(PurchaseResponse response) {
            handlePurchaseResponse(response);
        }

        @Override
        public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse response) {
            handlePurchaseUpdates(response);
        }
    };

    public AmazonBillingSupport(CodenameOneActivity activity) {
        this.activity = activity;
    }

    @Override
    public void initBilling() {
        if (!activity.isBillingEnabled() || registered) {
            return;
        }
        // The listener has to be registered before any other call, and from the
        // main thread; initBilling runs in the activity's onCreate.
        PurchasingService.registerListener(activity.getApplicationContext(), listener);
        registered = true;
    }

    /**
     * Called on every resume. On Amazon there is nothing to acknowledge up
     * front; this is where the user and the purchases that happened elsewhere
     * (another device, a purchase interrupted by a crash, a refund) are picked
     * up, as Amazon's own guidance asks of {@code onResume}.
     */
    @Override
    public void consumeAndAcknowlegePurchases() {
        if (!registered) {
            return;
        }
        boolean reset;
        synchronized (lock) {
            reset = !fullQueryDone;
        }
        PurchasingService.getUserData();
        PurchasingService.getPurchaseUpdates(reset);
    }

    @Override
    public void purchase(final String item) {
        if (!registered) {
            reportError(item, "In-app purchase is not available");
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RequestId id = PurchasingService.purchase(item);
                if (id != null) {
                    synchronized (lock) {
                        // A failed response carries no receipt, so this is the
                        // only way to tell the application which SKU failed.
                        purchaseRequests.put(id.toString(), item);
                    }
                }
            }
        });
    }

    /** Amazon has one purchase call; a subscription is a product like any other. */
    @Override
    public void subscribe(String item) {
        purchase(item);
    }

    @Override
    public boolean wasPurchased(String item) {
        synchronized (lock) {
            return owned.contains(item);
        }
    }

    @Override
    public boolean isConsumable(String item) {
        ProductType type;
        synchronized (lock) {
            type = productTypes.get(item);
        }
        if (type != null) {
            return type == ProductType.CONSUMABLE;
        }
        // Not listed yet: the naming rule the Play implementation applies.
        return !item.endsWith("nonconsume");
    }

    @Override
    public Product[] getProducts(String[] skus, boolean fromCacheOnly) {
        if (!fromCacheOnly && registered) {
            final Set<String> missing = new HashSet<String>();
            synchronized (lock) {
                for (int i = 0; i < skus.length; i++) {
                    if (!products.containsKey(skus[i])) {
                        missing.add(skus[i]);
                    }
                }
            }
            if (!missing.isEmpty()) {
                loadProducts(missing);
            }
        }
        List<Product> out = new ArrayList<Product>();
        synchronized (lock) {
            for (int i = 0; i < skus.length; i++) {
                Product p = products.get(skus[i]);
                if (p != null) {
                    out.add(p);
                }
            }
        }
        return out.toArray(new Product[out.size()]);
    }

    @Override
    public void onDestroy() {
        // The SDK holds the listener by application context and offers no
        // unregister; there is no connection to close.
    }

    private void loadProducts(Set<String> missing) {
        final List<Set<String>> batches = new ArrayList<Set<String>>();
        Set<String> batch = new HashSet<String>();
        for (String sku : missing) {
            if (batch.size() == MAX_SKUS_PER_REQUEST) {
                batches.add(batch);
                batch = new HashSet<String>();
            }
            batch.add(sku);
        }
        batches.add(batch);

        final Set<String> mine = new HashSet<String>();
        final boolean[] sent = new boolean[1];
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    for (Set<String> b : batches) {
                        RequestId id = PurchasingService.getProductData(b);
                        if (id != null) {
                            mine.add(id.toString());
                            pendingProductRequests.add(id.toString());
                        }
                    }
                    sent[0] = true;
                    lock.notifyAll();
                }
            }
        });

        final long deadline = System.currentTimeMillis() + PRODUCT_TIMEOUT_MILLIS;
        Runnable wait = new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    while (!sent[0] || outstanding(mine)) {
                        long left = deadline - System.currentTimeMillis();
                        if (left <= 0) {
                            return;
                        }
                        try {
                            lock.wait(left);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        };
        if (CN.isEdt()) {
            // Keeps the EDT alive while Amazon answers, as the Play path does.
            CN.invokeAndBlock(wait);
        } else {
            wait.run();
        }
        synchronized (lock) {
            pendingProductRequests.removeAll(mine);
        }
    }

    /** Callers hold lock. */
    private boolean outstanding(Set<String> requestIds) {
        for (String id : requestIds) {
            if (pendingProductRequests.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private void handleProductData(ProductDataResponse response) {
        if (response == null) {
            return;
        }
        synchronized (lock) {
            if (response.getRequestStatus() == ProductDataResponse.RequestStatus.SUCCESSFUL
                    && response.getProductData() != null) {
                for (Map.Entry<String, com.amazon.device.iap.model.Product> e
                        : response.getProductData().entrySet()) {
                    com.amazon.device.iap.model.Product src = e.getValue();
                    if (src == null) {
                        continue;
                    }
                    Product p = new Product();
                    p.setSku(src.getSku());
                    p.setDisplayName(src.getTitle());
                    p.setDescription(src.getDescription());
                    // Already formatted for the customer's marketplace.
                    p.setLocalizedPrice(src.getPrice());
                    products.put(e.getKey(), p);
                    if (src.getProductType() != null) {
                        productTypes.put(e.getKey(), src.getProductType());
                    }
                }
            }
            if (response.getRequestId() != null) {
                pendingProductRequests.remove(response.getRequestId().toString());
            }
            lock.notifyAll();
        }
    }

    private void handlePurchaseResponse(PurchaseResponse response) {
        if (response == null) {
            return;
        }
        String requested = null;
        if (response.getRequestId() != null) {
            synchronized (lock) {
                requested = purchaseRequests.remove(response.getRequestId().toString());
            }
        }
        rememberUser(response.getUserData());
        PurchaseResponse.RequestStatus status = response.getRequestStatus();
        com.amazon.device.iap.model.Receipt receipt = response.getReceipt();
        if (status == PurchaseResponse.RequestStatus.SUCCESSFUL && receipt != null) {
            // The application asked for this SKU by the name it knows; for a
            // subscription that is the term SKU, which is what it hears back.
            deliver(receipt, requested != null ? requested : skuOf(receipt), true);
            return;
        }
        final String sku = requested != null ? requested
                : (receipt != null ? skuOf(receipt) : "");
        if (status == PurchaseResponse.RequestStatus.PENDING) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    PurchaseCallback pc = getPurchaseCallback();
                    if (pc instanceof PendingPurchaseCallback) {
                        ((PendingPurchaseCallback) pc).itemPurchasePending(sku);
                    }
                }
            });
            return;
        }
        if (status == PurchaseResponse.RequestStatus.ALREADY_PURCHASED) {
            // Amazon sends no receipt with this answer; the full query brings
            // it, and delivers it if this installation never saw it.
            synchronized (lock) {
                owned.add(sku);
            }
            PurchasingService.getPurchaseUpdates(true);
            reportError(sku, "Item already purchased");
            return;
        }
        reportError(sku, "Purchase failed: " + (status == null ? "unknown" : status.name()));
    }

    private void handlePurchaseUpdates(PurchaseUpdatesResponse response) {
        if (response == null
                || response.getRequestStatus() != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            return;
        }
        rememberUser(response.getUserData());
        List<com.amazon.device.iap.model.Receipt> receipts = response.getReceipts();
        if (receipts != null) {
            for (com.amazon.device.iap.model.Receipt receipt : receipts) {
                if (receipt == null) {
                    continue;
                }
                if (receipt.isCanceled()) {
                    revoke(receipt);
                } else {
                    deliver(receipt, skuOf(receipt), false);
                }
            }
        }
        if (response.hasMore()) {
            // The next page continues from the SDK's own cursor.
            PurchasingService.getPurchaseUpdates(false);
        } else {
            synchronized (lock) {
                fullQueryDone = true;
            }
        }
    }

    /**
     * Hands a receipt to the application and then tells Amazon it arrived.
     *
     * @param fresh true for the answer to a purchase the application just made,
     * which is always reported; a receipt found by a query is reported only if
     * this installation has not reported it before
     */
    private void deliver(final com.amazon.device.iap.model.Receipt receipt, final String sku,
            final boolean fresh) {
        final String receiptId = receipt.getReceiptId();
        final boolean consumable = receipt.getProductType() == ProductType.CONSUMABLE;
        synchronized (lock) {
            if (receipt.getProductType() != null) {
                productTypes.put(sku, receipt.getProductType());
            }
            if (!consumable) {
                // A fulfilled consumable is spent; the others are owned for
                // as long as Amazon keeps listing them.
                owned.add(sku);
                owned.add(receipt.getSku());
            }
        }
        if (receiptId == null) {
            return;
        }
        final String user;
        synchronized (lock) {
            user = userId;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (activity.getApp() == null) {
                    // The application is not up yet (a query answered during
                    // launch). Nothing is recorded and nothing is fulfilled,
                    // so the next resume or launch brings the receipt again.
                    return;
                }
                if (!fresh && wasReported(KEY_DELIVERED, receiptId)) {
                    fulfill(receiptId);
                    return;
                }
                Date purchased = receipt.getPurchaseDate();
                com.codename1.payment.Purchase.postReceipt(Receipt.STORE_CODE_AMAZON, sku,
                        receiptId, purchased == null ? System.currentTimeMillis() : purchased.getTime(),
                        orderData(receiptId, user, sku));
                markReported(KEY_DELIVERED, receiptId);
                PurchaseCallback pc = getPurchaseCallback();
                if (pc != null) {
                    pc.itemPurchased(sku);
                }
                // Last, so a receipt Amazon stops sending is one the
                // application already has.
                fulfill(receiptId);
            }
        });
    }

    /**
     * A cancelled receipt: Amazon revoked the entitlement (a refund) or the
     * subscription ended. Reported once per receipt, since a full query lists
     * it again on every launch.
     */
    private void revoke(com.amazon.device.iap.model.Receipt receipt) {
        final String sku = skuOf(receipt);
        final String receiptId = receipt.getReceiptId();
        final boolean subscription = receipt.getProductType() == ProductType.SUBSCRIPTION;
        synchronized (lock) {
            owned.remove(sku);
            owned.remove(receipt.getSku());
        }
        if (receiptId == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (activity.getApp() == null || wasReported(KEY_CANCELED, receiptId)) {
                    return;
                }
                markReported(KEY_CANCELED, receiptId);
                PurchaseCallback pc = getPurchaseCallback();
                if (pc == null) {
                    return;
                }
                if (subscription) {
                    pc.subscriptionCanceled(sku);
                } else {
                    pc.itemRefunded(sku);
                }
            }
        });
    }

    private void fulfill(final String receiptId) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    PurchasingService.notifyFulfillment(receiptId, FulfillmentResult.FULFILLED);
                } catch (RuntimeException ex) {
                    // Not fatal: an unfulfilled receipt is delivered again and
                    // recognised by its id.
                    Log.e(TAG, "Amazon notifyFulfillment failed for " + receiptId, ex);
                }
            }
        });
    }

    private void reportError(final String sku, final String message) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                PurchaseCallback pc = getPurchaseCallback();
                if (pc != null) {
                    pc.itemPurchaseError(sku, message);
                }
            }
        });
    }

    private void rememberUser(UserData data) {
        if (data != null && data.getUserId() != null) {
            synchronized (lock) {
                userId = data.getUserId();
            }
        }
    }

    /**
     * The SKU the application purchased. For a subscription Amazon's receipt
     * names the parent in {@code getSku} and the purchased term separately.
     */
    private static String skuOf(com.amazon.device.iap.model.Receipt receipt) {
        String term = receipt.getTermSku();
        return term != null && term.length() > 0 ? term : receipt.getSku();
    }

    /**
     * The three values Amazon's receipt verification needs, as the JSON object
     * the receipt carries to a server.
     */
    static String orderData(String receiptId, String user, String sku) {
        try {
            JSONObject json = new JSONObject();
            json.put("receiptId", receiptId);
            json.put("userId", user == null ? "" : user);
            json.put("sku", sku == null ? "" : sku);
            return json.toString();
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to encode the Amazon receipt " + receiptId, ex);
            return "";
        }
    }

    private SharedPreferences prefs() {
        return activity.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private boolean wasReported(String key, String receiptId) {
        Set<String> ids = prefs().getStringSet(key, null);
        return ids != null && ids.contains(receiptId);
    }

    private void markReported(String key, String receiptId) {
        SharedPreferences p = prefs();
        // The set getStringSet returns must not be modified; store a copy.
        Set<String> ids = new HashSet<String>();
        Set<String> stored = p.getStringSet(key, null);
        if (stored != null) {
            ids.addAll(stored);
        }
        ids.add(receiptId);
        p.edit().putStringSet(key, ids).apply();
    }

    public PurchaseCallback getPurchaseCallback() {
        Object app = activity.getApp();
        return app instanceof PurchaseCallback ? (PurchaseCallback) app : null;
    }
}
