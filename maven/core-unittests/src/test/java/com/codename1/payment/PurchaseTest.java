package com.codename1.payment;

import com.codename1.io.Storage;
import com.codename1.junit.EdtTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseTest extends UITestBase {
    private static final String RECEIPTS_STORAGE = "CN1SubscriptionsData.dat";
    private static final String RECEIPTS_REFRESH_STORAGE = "CN1SubscriptionsDataRefreshTime.dat";
    private static final String PENDING_STORAGE = "PendingPurchases.dat";

    private TestPurchase purchase;

    @BeforeEach
    void setUpPurchase() throws Exception {
        CN.callSeriallyAndWait(new Runnable() {
            public void run() {
                TestLogger.install();
                Storage.setStorageInstance(null);
                Storage.getInstance().clearStorage();
                Storage.getInstance().clearCache();
                purchase = new TestPurchase();
                implementation.setInAppPurchase(purchase);
                Receipt.registerExternalizable();
                resetPurchaseState();
            }
        });
    }

    @AfterEach
    void tearDownPurchase() throws Exception {
        final TestPurchase current = purchase;
        CN.callSeriallyAndWait(new Runnable() {
            public void run() {
                if (current != null) {
                    current.setReceiptStore(null);
                }
                implementation.setInAppPurchase(null);
                Storage.getInstance().clearCache();
                Storage.getInstance().clearStorage();
                TestLogger.remove();
            }
        });
    }

    private void resetPurchaseState() {
        Storage storage = Storage.getInstance();
        storage.deleteStorageFile(RECEIPTS_STORAGE);
        storage.deleteStorageFile(RECEIPTS_REFRESH_STORAGE);
        storage.deleteStorageFile(PENDING_STORAGE);
        ReceiptStore clearingStore = new ReceiptStore() {
            public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
                callback.onSucess(new Receipt[0]);
            }

            public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
                callback.onSucess(Boolean.TRUE);
            }
        };
        purchase.setReceiptStore(clearingStore);
        purchase.synchronizeReceiptsSync(0);
        purchase.setReceiptStore(null);
    }

    private Receipt createReceipt(String sku, Date purchaseDate, Date expiryDate) {
        Receipt receipt = new Receipt();
        receipt.setSku(sku);
        receipt.setPurchaseDate(purchaseDate);
        receipt.setExpiryDate(expiryDate);
        receipt.setTransactionId("tx-" + sku + "-" + purchaseDate.getTime());
        receipt.setStoreCode(Receipt.STORE_CODE_PLAY);
        receipt.setOrderData("order-" + sku);
        return receipt;
    }

    @EdtTest
    void testPostReceiptStoresPendingReceipt() {
        long purchaseTime = System.currentTimeMillis();
        Purchase.postReceipt(Receipt.STORE_CODE_SIMULATOR, "pro", "txn-1", purchaseTime, "order-data");
        flushSerialCalls();

        List<Receipt> pending = purchase.getPendingPurchases();
        assertEquals(1, pending.size());
        Receipt receipt = pending.get(0);
        assertEquals("pro", receipt.getSku());
        assertEquals("txn-1", receipt.getTransactionId());
        assertEquals("order-data", receipt.getOrderData());
        assertNotNull(receipt.getPurchaseDate());
        assertEquals(Receipt.STORE_CODE_SIMULATOR, receipt.getStoreCode());
    }

    @EdtTest
    void testGetReceiptsLoadsFromReceiptStoreAndCaches() {
        TestReceiptStore store = new TestReceiptStore();
        store.setReceipts(Collections.singletonList(createReceipt("basic", new Date(1000L), new Date(2000L))));
        purchase.setReceiptStore(store);

        assertTrue(purchase.synchronizeReceiptsSync(0));
        List<Receipt> firstLoad = purchase.getReceipts();
        assertEquals(1, firstLoad.size());
        assertEquals("basic", firstLoad.get(0).getSku());

        store.setReceipts(Arrays.asList(
                createReceipt("basic", new Date(1000L), new Date(2000L)),
                createReceipt("plus", new Date(3000L), new Date(4000L))
        ));

        List<Receipt> secondLoad = purchase.getReceipts();
        assertSame(firstLoad, secondLoad, "Receipts should be cached");
        assertEquals(1, secondLoad.size(), "Cached receipts should not change until synchronized");
    }

    @EdtTest
    void testGetReceiptsFiltersBySku() {
        List<Receipt> receipts = purchase.getReceipts();
        receipts.clear();
        receipts.add(createReceipt("gold", new Date(1000L), new Date(5000L)));
        receipts.add(createReceipt("silver", new Date(2000L), new Date(6000L)));

        Receipt[] filtered = purchase.getReceipts("gold");
        assertEquals(1, filtered.length);
        assertEquals("gold", filtered[0].getSku());
    }

    @EdtTest
    void testSubscribeDelegatesToPurchaseWhenReceiptStoreInstalled() {
        TestReceiptStore store = new TestReceiptStore();
        store.setReceipts(Collections.<Receipt>emptyList());
        purchase.setReceiptStore(store);

        purchase.subscribe("vip");
        assertTrue(purchase.getRecordedPurchases().contains("vip"));
    }

    @EdtTest
    void testSubscribeWithPromotionalOfferDelegatesToPurchase() {
        TestReceiptStore store = new TestReceiptStore();
        purchase.setReceiptStore(store);

        PromotionalOffer offer = new PromotionalOffer() { };
        purchase.subscribe("vip", offer);
        assertTrue(purchase.getRecordedPromotionalPurchases().contains("vip"));
    }

    @EdtTest
    void testSubscribeWithoutReceiptStoreThrows() {
        purchase.setReceiptStore(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> purchase.subscribe("missing"));
        assertEquals("Unsupported", ex.getMessage());
    }

    @EdtTest
    void testGetFirstReceiptExpiringAfter() {
        List<Receipt> receipts = purchase.getReceipts();
        receipts.clear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.JANUARY, 1, 0, 0, 0);
        Date purchaseA = calendar.getTime();
        calendar.set(2022, Calendar.JANUARY, 31, 0, 0, 0);
        Date expiryA = calendar.getTime();
        Receipt first = createReceipt("plan", purchaseA, expiryA);

        calendar.set(2022, Calendar.FEBRUARY, 1, 0, 0, 0);
        Date purchaseB = calendar.getTime();
        calendar.set(2022, Calendar.FEBRUARY, 28, 0, 0, 0);
        Date expiryB = calendar.getTime();
        Receipt second = createReceipt("plan", purchaseB, expiryB);

        receipts.add(first);
        receipts.add(second);

        calendar.set(2022, Calendar.JANUARY, 15, 0, 0, 0);
        Receipt effective = purchase.getFirstReceiptExpiringAfter(calendar.getTime(), "plan");
        assertEquals(first, effective);

        calendar.set(2021, Calendar.DECEMBER, 15, 0, 0, 0);
        Receipt pending = purchase.getFirstReceiptExpiringAfter(calendar.getTime(), "plan");
        assertEquals(first, pending);
    }

    @EdtTest
    void testIsSubscribedUsesExpiryDate() {
        List<Receipt> receipts = purchase.getReceipts();
        receipts.clear();

        Date futureExpiry = new Date(System.currentTimeMillis() + 3600_000L);
        Receipt active = createReceipt("pro", new Date(System.currentTimeMillis() - 1000L), futureExpiry);
        receipts.add(active);

        assertTrue(purchase.isSubscribed("pro"));

        Date pastExpiry = new Date(System.currentTimeMillis() - 3600_000L);
        Receipt expired = createReceipt("legacy", new Date(System.currentTimeMillis() - 7200_000L), pastExpiry);
        receipts.add(expired);

        assertFalse(purchase.isSubscribed("legacy"));
    }

    @EdtTest
    void testSynchronizeReceiptsSyncSubmitsPendingReceipts() {
        Receipt pendingReceipt = createReceipt("gold", new Date(1000L), new Date(5000L));
        List<Receipt> pending = new ArrayList<Receipt>();
        pending.add(pendingReceipt);
        Storage.getInstance().writeObject(PENDING_STORAGE, pending);

        final List<Receipt> fetched = Arrays.asList(createReceipt("gold", new Date(1000L), new Date(8000L)));
        final TestReceiptStore store = new TestReceiptStore();
        store.setReceipts(fetched);
        purchase.setReceiptStore(store);

        boolean success = purchase.synchronizeReceiptsSync(0);
        assertTrue(success);
        assertEquals(1, store.getSubmittedReceipts().size());
        assertTrue(purchase.getPendingPurchases().isEmpty());
        Receipt[] refreshed = purchase.getReceipts("gold");
        assertEquals(1, refreshed.length);
        assertEquals(fetched.get(0).getExpiryDate(), refreshed[0].getExpiryDate());
    }

    @EdtTest
    void testSynchronizeReceiptsSyncReturnsFalseWhenSubmitFails() {
        Receipt pendingReceipt = createReceipt("silver", new Date(2000L), new Date(6000L));
        Storage.getInstance().writeObject(PENDING_STORAGE, Collections.singletonList(pendingReceipt));

        TestReceiptStore store = new TestReceiptStore();
        store.setSubmitResult(false);
        purchase.setReceiptStore(store);

        boolean success = purchase.synchronizeReceiptsSync(0);
        assertFalse(success);
        List<Receipt> pending = purchase.getPendingPurchases();
        assertEquals(1, pending.size());
        assertEquals("silver", pending.get(0).getSku());
    }

    private static class TestReceiptStore implements ReceiptStore {
        private List<Receipt> receipts = new ArrayList<Receipt>();
        private final List<Receipt> submitted = new ArrayList<Receipt>();
        private boolean submitResult = true;

        void setReceipts(List<Receipt> receipts) {
            this.receipts = new ArrayList<Receipt>(receipts);
        }

        void setSubmitResult(boolean submitResult) {
            this.submitResult = submitResult;
        }

        List<Receipt> getSubmittedReceipts() {
            return new ArrayList<Receipt>(submitted);
        }

        public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
            Receipt[] data = receipts.toArray(new Receipt[receipts.size()]);
            callback.onSucess(data);
        }

        public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
            submitted.add(receipt);
            callback.onSucess(Boolean.valueOf(submitResult));
        }
    }

    private static class TestPurchase extends Purchase {
        private final List<String> recordedPurchases = new ArrayList<String>();
        private final List<String> recordedPromotionalPurchases = new ArrayList<String>();

        List<String> getRecordedPurchases() {
            return recordedPurchases;
        }

        List<String> getRecordedPromotionalPurchases() {
            return recordedPromotionalPurchases;
        }

        @Override
        public String pay(double amount, String currency) {
            return "token";
        }

        @Override
        public Product[] getProducts(String[] skus) {
            return new Product[0];
        }

        @Override
        public boolean wasPurchased(String sku) {
            return recordedPurchases.contains(sku);
        }

        @Override
        public void purchase(String sku) {
            recordedPurchases.add(sku);
        }

        @Override
        public void purchase(String sku, PromotionalOffer promotionalOffer) {
            recordedPromotionalPurchases.add(sku);
        }

        @Override
        public void unsubscribe(String sku) {
        }

        @Override
        public void refund(String sku) {
        }
    }
}
