package com.codenameone.examples.hellocodenameone;

import com.codename1.impl.android.IBillingSupport;
import com.codename1.payment.Product;
import com.codename1.payment.Purchase;
import com.codename1.payment.Receipt;

/**
 * Fake {@link IBillingSupport} injected by {@link PurchaseBillingInstrumentationTest}.
 *
 * There is no local Google Play Billing sandbox, so instead of talking to a
 * real {@code BillingClient} this fake synthesizes a completed purchase exactly
 * where the real {@code BillingSupport.onPurchasesUpdated(...)} would: by
 * calling the static {@link Purchase#postReceipt} entry point. That drives the
 * cross-platform receipt-sync engine and the app's installed
 * {@code RecordingReceiptStore}, which logs {@code CN1SS:IAP:SUBMITTED}. The
 * instrumentation test scrapes logcat for that marker.
 *
 * It is the Android-side guard for issue #5186: postReceipt submits through a
 * freshly constructed Purchase instance, so a recorded submission proves the
 * store installed on a different instance is visible to it.
 */
public class CN1TestBillingSupport implements IBillingSupport {
    static final String TEST_SKU = "com.codenameone.hello.pro";
    static final String TEST_TX_ID = "android-test-tx-1";

    @Override
    public void initBilling() {
        // The generated stub enables billing + calls initBilling() on first
        // resume; synthesize the purchase here so no UI interaction is needed.
        fireSyntheticPurchase();
    }

    private void fireSyntheticPurchase() {
        Purchase.postReceipt(Receipt.STORE_CODE_PLAY, TEST_SKU, TEST_TX_ID,
                System.currentTimeMillis(), "{\"orderId\":\"GPA.TEST-0001\"}");
    }

    @Override
    public void purchase(String item) {
        fireSyntheticPurchase();
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
