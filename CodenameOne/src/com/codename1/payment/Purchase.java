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
package com.codename1.payment;

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/// Represents the status of in-app-purchase goods, this class provides information
/// about products purchased by a user as well as the ability to purchase additional
/// products. There are generally two types of payment systems: Manual and managed.
/// In manual payments we pay a specific amount in a specific currency  while with managed
/// payment systems we work against a product catalog defined in the server.
///
/// In-app-purchase API's rely on managed server based products, other payment systems
/// use the manual approach. An application dealing with virtual goods must support both
/// since not all devices feature in-app-purchase API's. An application dealing with physical
/// goods & services must use the latter according to the TOS of current in-app-purchase
/// solutions.
///
/// @author Shai Almog
public abstract class Purchase {
    private static final String RECEIPTS_KEY = "CN1SubscriptionsData.dat";
    private static final String RECEIPTS_REFRESH_TIME_KEY = "CN1SubscriptionsDataRefreshTime.dat";
    private static final String PENDING_PURCHASE_KEY = "PendingPurchases.dat";
    private static final Object PENDING_PURCHASE_LOCK = new Object();
    private static final Object synchronizationLock = new Object();
    private static final Object receiptsLock = new Object();
    /// Boolean flag to prevent `com.codename1.util.SuccessCallback)`
    /// re-entry.
    private static boolean syncInProgress;
    /// Flag to prevent `com.codename1.util.SuccessCallback)` re-entry.
    private static boolean loadInProgress;
    private static List<SuccessCallback<Boolean>> synchronizeReceiptsCallbacks;
    private ReceiptStore receiptStore;
    private List<Receipt> receipts;
    private Date receiptsRefreshTime;

    /// Posts a receipt to be added to the receipt store.
    ///
    /// #### Parameters
    ///
    /// - `sku`: The sku of the product
    ///
    /// - `transactionId`: The transaction ID
    ///
    /// - `datePurchased`: The date of the purchase.
    ///
    /// #### Deprecated
    ///
    /// For internal implementation use only.
    public static void postReceipt(String storeCode, String sku, String transactionId, long datePurchased, String orderData) {

        Receipt r = new Receipt();
        r.setSku(sku);
        r.setTransactionId(transactionId);
        r.setOrderData(orderData);
        r.setStoreCode(storeCode);
        if (datePurchased > 0) {
            r.setPurchaseDate(new Date(datePurchased));
        } else {
            r.setPurchaseDate(new Date());
        }
        Purchase.getInAppPurchase().postReceipt(r);

    }

    /// Returns the native OS purchase implementation if applicable, if unavailable this
    /// method will try to fallback to a custom purchase implementation and failing that
    /// will return null
    ///
    /// #### Returns
    ///
    /// instance of the purchase class or null
    public static Purchase getInAppPurchase() {
        return Display.getInstance().getInAppPurchase();
    }

    /// #### Deprecated
    ///
    /// use the version that takes no arguments
    public static Purchase getInAppPurchase(boolean d) {
        return Display.getInstance().getInAppPurchase();
    }

    protected final ReceiptStore getReceiptStore() {
        return receiptStore;
    }

    /// Installs a given receipt store to handle receipt management
    ///
    /// #### Parameters
    ///
    /// - `store`
    public final void setReceiptStore(ReceiptStore store) {
        receiptStore = store;
    }

    /// Gets all of the receipts for this app.  Note:  You should periodically
    /// reload the receipts from the server to make sure that the user
    /// hasn't canceled a receipt or renewed one.
    ///
    /// #### Returns
    ///
    /// List of receipts for purchases this app.
    public final List<Receipt> getReceipts() {
        synchronized (receiptsLock) {
            if (receipts == null) {
                if (Storage.getInstance().exists(RECEIPTS_KEY)) {
                    Receipt.registerExternalizable();
                    try {
                        receipts = (List<Receipt>) Storage.getInstance().readObject(RECEIPTS_KEY);
                    } catch (Exception ex) {
                        Log.p("Failed to load receipts from " + RECEIPTS_KEY);
                        Log.e(ex);
                        receipts = new ArrayList<Receipt>();

                    }
                } else {
                    receipts = new ArrayList<Receipt>();
                }
            }
            return receipts;
        }
    }

    /// Sets the list of receipts.
    ///
    /// #### Parameters
    ///
    /// - `data`
    private void setReceipts(List<Receipt> data) {
        synchronized (receiptsLock) {
            receipts = new ArrayList<Receipt>();
            receipts.addAll(data);
            Storage.getInstance().writeObject(RECEIPTS_KEY, receipts);
        }
    }

    /// Gets all of the receipts for the specified skus.
    ///
    /// #### Parameters
    ///
    /// - `skus`: The skus for which to get receipts.
    ///
    /// #### Returns
    ///
    /// All receipts for the given skus.
    public final Receipt[] getReceipts(String... skus) {
        List<Receipt> out = new ArrayList<Receipt>();
        List<String> lSkus = Arrays.asList(skus);
        for (Receipt r : getReceipts()) {
            if (lSkus.contains(r.getSku())) {
                out.add(r);
            }
        }
        return out.toArray(new Receipt[out.size()]);
    }

    /// Gets the time that receipts were last refreshed.
    private Date getReceiptsRefreshTime() {
        synchronized (receiptsLock) {
            if (receiptsRefreshTime == null) {
                if (Storage.getInstance().exists(RECEIPTS_REFRESH_TIME_KEY)) {
                    receiptsRefreshTime = (Date) Storage.getInstance().readObject(RECEIPTS_REFRESH_TIME_KEY);
                } else {
                    return new Date(-1L);
                }
            }
            return receiptsRefreshTime;
        }
    }

    /// Updates the last refresh time for receipts.
    ///
    /// #### Parameters
    ///
    /// - `time`
    private void setReceiptsRefreshTime(Date time) {
        synchronized (receiptsLock) {
            receiptsRefreshTime = time;
            Storage.getInstance().writeObject(RECEIPTS_REFRESH_TIME_KEY, receiptsRefreshTime);
        }
    }

    /// Indicates whether the purchasing platform supports manual payments which
    /// are just payments of a specific amount of money.
    ///
    /// #### Returns
    ///
    /// true if manual payments are supported
    public boolean isManualPaymentSupported() {
        return false;
    }

    /// Indicates whether the purchasing platform supports managed payments which
    /// work by picking products that are handled by the servers/OS of the platform vendor.
    ///
    /// #### Returns
    ///
    /// true if managed payments are supported
    public boolean isManagedPaymentSupported() {
        return false;
    }

    /// Performs payment of a specific amount based on the manual payment API, notice that
    /// this doesn't use the in-app-purchase functionality of the device!
    ///
    /// #### Parameters
    ///
    /// - `amount`: the amount to pay
    ///
    /// - `currency`: the three letter currency type
    ///
    /// #### Returns
    ///
    /// @return a token representing the pending transaction which will be matched
    /// when receiving a callback from the platform or a null if the payment has
    /// failed or was canceled
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the manual payments API and will fail if
    ///                          isManualPaymentSupported() returns false
    public String pay(double amount, String currency) {
        throw new RuntimeException("Unsupported");
    }

    /// Performs payment of a specific amount based on the manual payment API, notice that
    /// this doesn't use the in-app-purchase functionality of the device!
    ///
    /// #### Parameters
    ///
    /// - `amount`: the amount to pay
    ///
    /// - `currency`: the three letter currency type
    ///
    /// - `invoiceNumber`: application specific invoice number
    ///
    /// #### Returns
    ///
    /// @return a token representing the pending transaction which will be matched
    /// when receiving a callback from the platform or a null if the payment has
    /// failed or was canceled
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the manual payments API and will fail if
    ///                          isManualPaymentSupported() returns false
    public String pay(double amount, String currency, String invoiceNumber) {
        return pay(amount, currency);
    }

    /// Indicates whether the payment platform supports things such as "item listing" or
    /// requires that items be coded into the system. iOS provides listing and pricing
    /// where Android expects developers to redirect into the Play application for
    /// application details.
    ///
    /// #### Returns
    ///
    /// true if the OS supports this behavior
    public boolean isItemListingSupported() {
        return false;
    }

    /// Returns the product list for the given SKU array
    ///
    /// #### Parameters
    ///
    /// - `skus`: the ids for the specific products
    ///
    /// #### Returns
    ///
    /// the product instances
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the managed payments API and will fail if
    ///                          isManagedPaymentSupported() returns false
    ///
    /// - `RuntimeException`: This method works only if isItemListingSupported() returns true
    public Product[] getProducts(String[] skus) {
        throw new RuntimeException("Unsupported");
    }

    /// Returns true if the given SKU was purchased in the past, notice this method might not
    /// work as expected for Unmanaged/consumable products which can be purchased multiple
    /// times.  In addition, this will only return true if the product was purchased (or
    /// has been restored) on the current device.
    ///
    /// #### Parameters
    ///
    /// - `sku`: the id of the product
    ///
    /// #### Returns
    ///
    /// true if the product was purchased
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the managed payments API and will fail if
    ///                          isManagedPaymentSupported() returns false
    public boolean wasPurchased(String sku) {
        throw new RuntimeException("Unsupported");
    }

    /// Begins the purchase process for the given SKU
    ///
    /// On Android you *must* use `#subscribe(java.lang.String)` for play store subscription products instead of this method.  You cannot use `#purchase(java.lang.String)`.  On iOS
    /// there is no difference between `#subscribe(java.lang.String)` and `#purchase(java.lang.String)`, so if you are simulating subscriptions
    /// on iOS using auto-renewables, you are better to use `#subscribe(java.lang.String)` as this will work correctly on both Android
    /// and iOS.
    ///
    /// #### Parameters
    ///
    /// - `sku`: the SKU with which to perform the purchase process
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the managed payments API and will fail if
    ///                          isManagedPaymentSupported() returns false
    public void purchase(String sku) {
        throw new RuntimeException("Unsupported");
    }

    /// Begins the purchase process for the given SKU using a provided promotional offer.
    ///
    /// Promotional offers are currently only supported on iOS.  See [Apple's documentation](https://developer.apple.com/documentation/storekit/in-app_purchase/original_api_for_in-app_purchase/subscriptions_and_offers/implementing_promotional_offers_in_your_app?language=objc)
    ///
    /// #### Parameters
    ///
    /// - `sku`: the SKU with which to perform the purchase process
    ///
    /// - `promotionalOffer`: The promotional offer.
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the managed payments API and will fail if
    ///                          isManagedPaymentSupported() returns false
    ///
    /// #### See also
    ///
    /// - ApplePromotionalOffer
    public void purchase(String sku, PromotionalOffer promotionalOffer) {
        throw new RuntimeException("Unsupported");
    }

    /// Begins subscribe process for the given subscription SKU
    ///
    /// On Android you *must* use this method for play store subscription products.  You cannot use `#purchase(java.lang.String)`.  On iOS
    /// there is no difference between `#subscribe(java.lang.String)` and `#purchase(java.lang.String)`, so if you are simulating subscriptions
    /// on iOS using auto-renewables, you are better to use `#subscribe(java.lang.String)` as this will work correctly on both Android
    /// and iOS.
    ///
    /// #### Parameters
    ///
    /// - `sku`: the SKU with which to perform the purchase process
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the managed payments API and will fail if
    ///                          isManagedPaymentSupported() returns false
    public void subscribe(String sku) {
        if (receiptStore != null) {
            purchase(sku);
            return;
        }
        throw new RuntimeException("Unsupported");
    }

    /// Begins subscribe process for the given subscription SKU using a provided promotional offer.
    ///
    /// Promotional offers are currently only supported on iOS.  See [Apple's documentation](https://developer.apple.com/documentation/storekit/in-app_purchase/original_api_for_in-app_purchase/subscriptions_and_offers/implementing_promotional_offers_in_your_app?language=objc)
    ///
    /// #### Parameters
    ///
    /// - `sku`: the SKU with which to perform the purchase process
    ///
    /// - `promotionalOffer`: The promotional offer.
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the managed payments API and will fail if
    ///                          isManagedPaymentSupported() returns false
    ///
    /// #### See also
    ///
    /// - ApplePromotionalOffer
    public void subscribe(String sku, PromotionalOffer promotionalOffer) {
        if (receiptStore != null) {
            purchase(sku, promotionalOffer);
            return;
        }
        throw new RuntimeException("Unsupported");
    }

    /// Cancels the subscription to a given SKU
    ///
    /// #### Parameters
    ///
    /// - `sku`: the SKU with which to perform the purchase process
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: @throws RuntimeException This method is a part of the managed payments API and will fail if
    ///                          isManagedPaymentSupported() returns false
    public void unsubscribe(String sku) {
        throw new RuntimeException("Unsupported");
    }

    /// Gets a list of purchases that haven't yet been sent to the server.  You can
    /// use this for diagnostic and debugging purposes periodically in the app to
    /// make sure there aren't a queue of purchases that aren't getting submitted
    /// to the server.
    ///
    /// #### Returns
    ///
    /// List of receipts that haven't been sent to the server.
    @SuppressWarnings("unchecked")
    public List<Receipt> getPendingPurchases() {
        synchronized (PENDING_PURCHASE_LOCK) {
            Storage s = Storage.getInstance();
            Util.register(new Receipt());
            if (s.exists(PENDING_PURCHASE_KEY)) {
                return (List<Receipt>) s.readObject(PENDING_PURCHASE_KEY);
            } else {
                return new ArrayList<Receipt>();
            }
        }
    }

    /// Adds a receipt to be pushed to the server.
    ///
    /// #### Parameters
    ///
    /// - `receipt`: the receipt
    private void addPendingPurchase(Receipt receipt) {
        synchronized (PENDING_PURCHASE_LOCK) {
            Storage s = Storage.getInstance();
            List<Receipt> pendingPurchases = getPendingPurchases();
            pendingPurchases.add(receipt);
            s.writeObject(PENDING_PURCHASE_KEY, pendingPurchases);
        }
    }

    /// Removes a receipt from pending purchases.
    ///
    /// #### Parameters
    ///
    /// - `transactionId`: the transaction id
    ///
    /// #### Returns
    ///
    /// the removed receipt
    private Receipt removePendingPurchase(String transactionId) {
        synchronized (PENDING_PURCHASE_LOCK) {
            Storage s = Storage.getInstance();
            List<Receipt> pendingPurchases = getPendingPurchases();
            Receipt found = null;
            for (Receipt r : pendingPurchases) {
                if (r.getTransactionId() != null && r.getTransactionId().equals(transactionId)) {
                    found = r;
                    break;

                }
            }
            if (found != null) {
                pendingPurchases.remove(found);
                s.writeObject(PENDING_PURCHASE_KEY, pendingPurchases);
                return found;
            } else {
                return null;
            }
        }
    }

    public final void synchronizeReceipts() {
        if (syncInProgress) {
            return;
        }
        synchronizeReceipts(0, null);
    }

    private void fireSynchronizeReceiptsCallbacks(boolean result) {

        synchronized (synchronizationLock) {
            if (synchronizeReceiptsCallbacks == null) {
                return;
            }
            for (SuccessCallback<Boolean> cb : synchronizeReceiptsCallbacks) {
                cb.onSucess(result);
            }
            synchronizeReceiptsCallbacks.clear();
        }
    }

    /// Synchronize with receipt store.  This will try to submit any pending purchases
    /// to the receipt store, and then reload receipts from the receipt store
    ///
    /// #### Parameters
    ///
    /// - `ifOlderThanMs`: Only fetch receipts if they haven't been fetched in `ifOlderThanMs` milliseconds.
    ///
    /// - `callback`: @param callback      Callback called when sync is done.  Will be passed true if all pending purchases were successfully
    ///                      submitted to the receipt store AND receipts were successfully loaded.
    public final void synchronizeReceipts(final long ifOlderThanMs, final SuccessCallback<Boolean> callback) {
        synchronized (synchronizationLock) {
            if (callback != null) {
                if (synchronizeReceiptsCallbacks == null) {
                    synchronizeReceiptsCallbacks = new ArrayList<SuccessCallback<Boolean>>();
                }
                synchronizeReceiptsCallbacks.add(callback);
            }
            if (syncInProgress) {
                return;
            }
            syncInProgress = true;
        }

        synchronized (PENDING_PURCHASE_LOCK) {

            List<Receipt> pending = getPendingPurchases();
            if (!pending.isEmpty() && receiptStore != null) {

                final Receipt receipt = pending.get(0);
                receiptStore.submitReceipt(pending.get(0), new SuccessCallback<Boolean>() {

                    @Override
                    public void onSucess(Boolean submitSucceeded) {
                        if (submitSucceeded) {
                            removePendingPurchase(receipt.getTransactionId());
                            syncInProgress = false;

                            // If the submit succeeded we need to refetch
                            // so we set this to zero here.
                            synchronizeReceipts(0, callback);
                        } else {
                            syncInProgress = false;
                            fireSynchronizeReceiptsCallbacks(false);
                        }
                    }

                });
            } else {
                loadReceipts(ifOlderThanMs, new SuccessCallback<Boolean>() {

                    @Override
                    public void onSucess(Boolean fetchSucceeded) {
                        syncInProgress = false;
                        fireSynchronizeReceiptsCallbacks(fetchSucceeded);
                    }

                });

            }
        }
    }

    /// Posts a receipt to be added to the receipt store.
    ///
    /// #### Parameters
    ///
    /// - `r`: The receipt to post.
    private void postReceipt(Receipt r) {
        addPendingPurchase(r);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                synchronizeReceipts();
            }
        });
    }

    /// Synchronize receipts and wait for the sync to complete before proceeding.
    ///
    /// #### Parameters
    ///
    /// - `ifOlderThanMs`: Only re-fetch if it hasn't been reloaded in this number of milliseconds.
    ///
    /// #### Returns
    ///
    /// True if the synchronization succeeds.  False otherwise.
    public final boolean synchronizeReceiptsSync(long ifOlderThanMs) {
        final boolean[] complete = new boolean[1];
        final boolean[] success = new boolean[1];
        synchronizeReceipts(ifOlderThanMs, new SynchronizeReceiptsSyncSuccessCallback(complete, success));

        if (!complete[0]) {
            Display.getInstance().invokeAndBlock(new SynchronizeReceiptsSyncRunnable(complete));
        }
        return success[0];
    }

    /// Fetches receipts from the IAP service so that we know we are dealing
    /// with current data.  This method should be called before checking a
    /// subscription expiry date so that any changes the user has made in the
    /// store is reflected here (e.g. cancelling or renewing subscription).
    ///
    /// #### Parameters
    ///
    /// - `ifOlderThanMs`: @param ifOlderThanMs Update is only performed if more than `ifOlderThanMs` milliseconds has elapsed
    ///                      since the last successful fetch.
    ///
    /// - `callback`: @param callback      Callback called when request is complete.  Passed `true` if
    ///                      the data was successfully fetched.  `false` otherwise.
    private void loadReceipts(long ifOlderThanMs, final SuccessCallback<Boolean> callback) {
        if (loadInProgress) {
            Log.p("Did not load receipts because another load is in progress");
            callback.onSucess(false);
            return;
        }
        loadInProgress = true;
        Date lastRefreshTime = getReceiptsRefreshTime();
        Date now = new Date();
        if (lastRefreshTime.getTime() + ifOlderThanMs > now.getTime()) {
            Log.p("Receipts were last refreshed at " + lastRefreshTime + " so we won't refetch.");
            loadInProgress = false;
            callback.onSucess(true);
            return;
        }

        SuccessCallback<Receipt[]> onSuccess = new SuccessCallback<Receipt[]>() {

            @Override
            public void onSucess(Receipt[] value) {
                if (value != null) {
                    setReceipts(Arrays.asList(value));
                    setReceiptsRefreshTime(new Date());
                    loadInProgress = false;
                    callback.onSucess(Boolean.TRUE);
                } else {
                    loadInProgress = false;
                    callback.onSucess(Boolean.FALSE);
                }
            }

        };
        if (receiptStore != null) {
            receiptStore.fetchReceipts(onSuccess);

        } else {
            Log.p("No receipt store is currently registered so no receipts were fetched");
            loadInProgress = false;
            callback.onSucess(Boolean.FALSE);
        }
    }

    /// Gets the latest expiry date for a set of SKUs as reflected by a set of receipts.
    ///
    /// #### Parameters
    ///
    /// - `receipts`: Receipts to check against.
    ///
    /// - `skus`: The set of skus we are checking for.
    ///
    /// #### Returns
    ///
    /// The expiry date for a set of skus
    private Date getExpiryDate(Receipt[] receipts, String... skus) {
        Date expiryDate = new Date(0L);
        List<String> lSkus = Arrays.asList(skus);

        for (Receipt r : receipts) {
            if (!lSkus.contains(r.getSku())) {
                continue;
            }
            if (r.getExpiryDate() == null) {
                continue;
            }
            if (r.getExpiryDate().getTime() > expiryDate.getTime() && r.getCancellationDate() == null) {
                expiryDate = r.getExpiryDate();
            }
        }
        return expiryDate;
    }

    /// Gets the expiry date for a set of skus.
    ///
    /// #### Parameters
    ///
    /// - `skus`: The skus to check.  The latest expiry date of the set will be used.
    ///
    /// #### Returns
    ///
    /// The expiry date for a set of skus.
    public final Date getExpiryDate(String... skus) {
        return getExpiryDate(getReceipts(skus), skus);
    }

    /// Checks to see if the user is currently subscribed to any of the given skus.  A user
    /// is deemed to be subscribed if `#getExpiryDate(java.lang.String...)` returns a date
    /// later than now.
    ///
    /// #### Parameters
    ///
    /// - `skus`: Set of skus to check.
    public final boolean isSubscribed(String... skus) {
        Date exp = getExpiryDate(skus);
        return exp != null && exp.getTime() >= System.currentTimeMillis();
    }

    /// Given the `publishDate` for an item, this returns the effective receipt that
    /// relates to that item.  This will either be a receipt with `purchaseDate <= publishDate <= expiryDate` or
    /// the earliest receipt with `publishDate < purchaseDate`,  or null if no receipts
    ///
    /// #### Parameters
    ///
    /// - `receipts`
    ///
    /// - `publishDate`
    ///
    /// - `skus`
    private Receipt getFirstReceiptExpiringAfter(Receipt[] receipts, Date publishDate, String... skus) {
        List<String> lSkus = Arrays.asList(skus);
        Receipt effectiveReceipt = null;
        for (Receipt r : receipts) {
            if (!lSkus.contains(r.getSku())) {
                continue;
            }
            if (r.getExpiryDate() == null) {
                continue;
            }
            if (r.getPurchaseDate() != null
                    && r.getPurchaseDate().getTime() <= publishDate.getTime()
                    && r.getExpiryDate().getTime() >= publishDate.getTime()
                    && (r.getCancellationDate() == null
                    || r.getCancellationDate().getTime() >= publishDate.getTime())) {
                // Exact match in range.
                return r;
            }

            if (r.getPurchaseDate() != null && r.getPurchaseDate().getTime() <= publishDate.getTime()) {
                // The previous check would see if we had an exact match.
                // If we are here and the purchase date is before the issue date,
                // then the receipt had expired by the time this issue came out
                continue;
            }

            // At this point we know that the issue date is before the purchase date
            if (effectiveReceipt == null || effectiveReceipt.getPurchaseDate().getTime() > r.getPurchaseDate().getTime()) {
                effectiveReceipt = r;
            }
        }
        return effectiveReceipt;
    }

    /// Gets the first receipt that expires after the specified date for the provided
    /// skus.
    ///
    /// #### Parameters
    ///
    /// - `dt`
    ///
    /// - `skus`
    public Receipt getFirstReceiptExpiringAfter(Date dt, String... skus) {
        return getFirstReceiptExpiringAfter(getReceipts(skus), dt, skus);
    }

    /// Indicates whether refunding is possible when the SKU is purchased
    ///
    /// #### Parameters
    ///
    /// - `sku`: the sku
    ///
    /// #### Returns
    ///
    /// true if the SKU can be refunded
    public boolean isRefundable(String sku) {
        return false;
    }

    /// Tries to refund the given SKU if applicable in the current market/product
    ///
    /// #### Parameters
    ///
    /// - `sku`: the id for the product
    public void refund(String sku) {
    }

    /// Returns true if the subscription API is supported in this platform
    ///
    /// #### Returns
    ///
    /// true if the subscription API is supported in this platform
    public boolean isSubscriptionSupported() {
        return false;
    }

    /// Some platforms support subscribing but don't support unsubscribe
    ///
    /// #### Returns
    ///
    /// true if the subscription API allows for unsubscribe
    ///
    /// #### Deprecated
    ///
    /// use `#isManageSubscriptionsSupported()` instead
    public boolean isUnsubscribeSupported() {
        return isSubscriptionSupported();
    }

    /// Indicates whether a purchase restore button is supported by the OS
    ///
    /// #### Returns
    ///
    /// true if you can invoke the restore method
    public boolean isRestoreSupported() {
        return false;
    }

    /// Restores purchases if applicable, this will only work if isRestoreSupported() returns true
    public void restore() {
    }

    /// Checks to see if this platform supports the `#manageSubscriptions(java.lang.String)` method.
    ///
    /// #### Returns
    ///
    /// True if the platform supports the `#manageSubscriptions(java.lang.String)` method.
    ///
    /// #### Since
    ///
    /// 6.0
    public boolean isManageSubscriptionsSupported() {
        return false;
    }

    /// Open the platform's UI for managing subscriptions.  Currently iOS and Android
    /// are the only platforms that support this.  Other platforms will simply display a dialog stating that
    /// it doesn't support this feature.  Use the `#isManageSubscriptionsSupported()` method to check
    /// if the platform supports this feature.
    ///
    /// #### Parameters
    ///
    /// - `sku`: @param sku Optional sku of product whose subscription you wish to manage.  If left null, then
    ///            the general subscription management UI will be opened.  iOS doesn't support "deep-linking" directly to the
    ///            management for a particular sku, so this parameter is ignored there.  If included on Android, howerver,
    ///            it will open the UI for managing the specified sku.
    ///
    /// #### Since
    ///
    /// 6.0
    public void manageSubscriptions(String sku) {
        Dialog.show("Not Supported", "This platform doesn't support in-app subscription management. ", "OK", null);
    }

    /// Returns the store code associated with this in-app purchase object.
    ///
    /// #### Returns
    ///
    /// @return The store code.  One of `Receipt#STORE_CODE_ITUNES`, `Receipt#STORE_CODE_PLAY`, `Receipt#STORE_CODE_SIMULATOR`, or
    /// `Receipt#STORE_CODE_WINDOWS`.
    ///
    /// #### Since
    ///
    /// 8.0
    public String getStoreCode() {
        return null;
    }

    private static class SynchronizeReceiptsSyncRunnable implements Runnable {

        private final boolean[] complete;

        public SynchronizeReceiptsSyncRunnable(boolean[] complete) {
            this.complete = complete;
        }

        @Override
        public void run() {

            while (!complete[0]) {
                synchronized (complete) {
                    try {
                        // need to recheck condition within the synchronized block
                        if (!complete[0]) {
                            complete.wait();
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

    }

    private static class SynchronizeReceiptsSyncSuccessCallback implements SuccessCallback<Boolean> {

        private final boolean[] complete;
        private final boolean[] success;

        public SynchronizeReceiptsSyncSuccessCallback(boolean[] complete, boolean[] success) {
            this.complete = complete;
            this.success = success;
        }

        @Override
        public void onSucess(Boolean value) {
            complete[0] = true;
            success[0] = value;

            synchronized (complete) {
                complete.notifyAll();
            }

        }

    }
}
