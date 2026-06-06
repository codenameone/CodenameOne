package com.codenameone.examples.purchasetest;

import com.codename1.impl.android.IBillingSupport;
import com.codename1.payment.Product;
import com.codename1.payment.Purchase;
import com.codename1.payment.Receipt;

/**
 * Fake {@link IBillingSupport} used by {@link PurchaseBillingInstrumentationTest}.
 *
 * There is no local Google Play Billing sandbox, so instead of talking to a real
 * {@code BillingClient} this fake synthesizes a completed purchase exactly where
 * the real {@code BillingSupport.onPurchasesUpdated(...)} would: by calling the
 * static {@link Purchase#postReceipt}. That drives the cross-platform
 * receipt-sync engine and the app's installed {@code RecordingReceiptStore},
 * which logs {@code CN1SS:IAP:SUBMITTED}.
 *
 * {@link #purchase(String)} is invoked directly by the instrumentation test
 * once the CN1 VM has booted and installed the store (deterministic), rather
 * than auto-firing from {@link #initBilling()} where it would race app startup.
 * Android-side guard for #5186: postReceipt submits through a freshly
 * constructed Purchase instance, so a recorded submission proves the store
 * installed on a different instance is visible to it.
 */
public class CN1TestBillingSupport implements IBillingSupport {
    static final String TEST_SKU = "com.codenameone.hello.pro";
    static final String TEST_TX_ID = "android-test-tx-1";

    @Override
    public void purchase(String item) {
        System.out.println("CN1SS:IAP_FAKE fired postReceipt for " + TEST_TX_ID);
        Purchase.postReceipt(Receipt.STORE_CODE_PLAY, TEST_SKU, TEST_TX_ID,
                System.currentTimeMillis(), "{\"orderId\":\"GPA.TEST-0001\"}");
    }

    @Override
    public void initBilling() {
        // No-op: the test drives the purchase explicitly after the store is
        // installed, so we don't race app startup here.
    }

    @Override
    public boolean wasPurchased(String item) {
        return false;
    }

    @Override
    public void subscribe(String item) {
    }

    @Override
    public void consumeAndAcknowlegePurchases() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public Product[] getProducts(String[] skus, boolean fromCacheOnly) {
        return new Product[0];
    }

    @Override
    public boolean isConsumable(String item) {
        return true;
    }
}
