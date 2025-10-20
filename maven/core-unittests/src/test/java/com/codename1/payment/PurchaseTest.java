package com.codename1.payment;

import com.codename1.io.Storage;
import com.codename1.io.TestImplementationProvider;
import com.codename1.io.Util;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class PurchaseTest {
    private static class TestPurchase extends Purchase {
        private final List<String> recordedPurchases = new ArrayList<String>();
        private final List<String> recordedPromotionalPurchases = new ArrayList<String>();

        public List<String> getRecordedPurchases() {
            return recordedPurchases;
        }

        public List<String> getRecordedPromotionalPurchases() {
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

    private TestPurchase purchase;
    private CodenameOneImplementation impl;
    private CodenameOneImplementation originalDisplayImpl;
    private CodenameOneImplementation originalUtilImpl;
    private Object originalStorageInstance;
    private Display display;
    private String receiptsKey;
    private String pendingKey;

    @BeforeEach
    public void setup() throws Exception {
        Field receiptsKeyField = Purchase.class.getDeclaredField("RECEIPTS_KEY");
        receiptsKeyField.setAccessible(true);
        receiptsKey = (String) receiptsKeyField.get(null);

        Field pendingKeyField = Purchase.class.getDeclaredField("PENDING_PURCHASE_KEY");
        pendingKeyField.setAccessible(true);
        pendingKey = (String) pendingKeyField.get(null);

        display = Display.getInstance();
        Field codenameOneRunningField = Display.class.getDeclaredField("codenameOneRunning");
        codenameOneRunningField.setAccessible(true);
        codenameOneRunningField.set(display, Boolean.FALSE);

        Field implField = Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        originalDisplayImpl = (CodenameOneImplementation) implField.get(null);

        Field utilImplField = Util.class.getDeclaredField("implInstance");
        utilImplField.setAccessible(true);
        originalUtilImpl = (CodenameOneImplementation) utilImplField.get(null);

        Field storageInstanceField = Storage.class.getDeclaredField("INSTANCE");
        storageInstanceField.setAccessible(true);
        originalStorageInstance = storageInstanceField.get(null);

        impl = TestImplementationProvider.installImplementation(true);
        implField.set(null, impl);

        purchase = new TestPurchase();
        when(impl.getInAppPurchase()).thenReturn(purchase);

        Storage.getInstance().clearStorage();
        Storage.getInstance().clearCache();
        resetPurchaseStatics();
        Receipt.registerExternalizable();
    }

    @AfterEach
    public void tearDown() throws Exception {
        Storage.getInstance().clearStorage();
        Storage.setStorageInstance(null);
        resetPurchaseStatics();

        Field implField = Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        implField.set(null, originalDisplayImpl);

        Util.setImplementation(originalUtilImpl);

        Field storageInstanceField = Storage.class.getDeclaredField("INSTANCE");
        storageInstanceField.setAccessible(true);
        storageInstanceField.set(null, originalStorageInstance);
    }

    private void resetPurchaseStatics() throws Exception {
        setStaticField(Purchase.class, "receiptStore", null);
        setStaticField(Purchase.class, "receipts", null);
        setStaticField(Purchase.class, "receiptsRefreshTime", null);
        setStaticField(Purchase.class, "syncInProgress", Boolean.FALSE);
        setStaticField(Purchase.class, "loadInProgress", Boolean.FALSE);
        setStaticField(Purchase.class, "synchronizeReceiptsCallbacks", null);
    }

    private void setStaticField(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
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

    @Test
    public void testPostReceiptStoresPendingReceipt() {
        long purchaseTime = System.currentTimeMillis();
        Purchase.postReceipt(Receipt.STORE_CODE_SIMULATOR, "pro", "txn-1", purchaseTime, "order-data");

        List<Receipt> pending = purchase.getPendingPurchases();
        assertEquals(1, pending.size());
        Receipt receipt = pending.get(0);
        assertEquals("pro", receipt.getSku());
        assertEquals("txn-1", receipt.getTransactionId());
        assertEquals("order-data", receipt.getOrderData());
        assertNotNull(receipt.getPurchaseDate());
        assertEquals(Receipt.STORE_CODE_SIMULATOR, receipt.getStoreCode());
    }

    @Test
    public void testGetReceiptsLoadsFromStorageAndCaches() {
        List<Receipt> stored = new ArrayList<Receipt>();
        stored.add(createReceipt("basic", new Date(1000L), new Date(2000L)));
        Storage.getInstance().writeObject(receiptsKey, new ArrayList<Receipt>(stored));
        Storage.getInstance().clearCache();

        List<Receipt> firstLoad = purchase.getReceipts();
        assertEquals(1, firstLoad.size());
        assertEquals("basic", firstLoad.get(0).getSku());

        List<Receipt> updated = new ArrayList<Receipt>(stored);
        updated.add(createReceipt("plus", new Date(3000L), new Date(4000L)));
        Storage.getInstance().writeObject(receiptsKey, updated);

        List<Receipt> secondLoad = purchase.getReceipts();
        assertSame(firstLoad, secondLoad, "Receipts should be cached");
        assertEquals(1, secondLoad.size(), "Cached list should not reload automatically");
    }

    @Test
    public void testGetReceiptsReturnsEmptyListWhenStoredDataHasUnexpectedType() {
        Storage.getInstance().writeObject(receiptsKey, "bad-data");
        Storage.getInstance().clearCache();

        List<Receipt> receipts = purchase.getReceipts();
        assertNotNull(receipts, "Receipts list should be initialized");
        assertTrue(receipts.isEmpty(), "Unexpected types should produce an empty cache");

        List<Receipt> secondCall = purchase.getReceipts();
        assertSame(receipts, secondCall, "Receipts cache should be reused after invalid data");
        assertEquals("bad-data", Storage.getInstance().readObject(receiptsKey),
                "Storage contents should remain untouched when data cannot be cast");
    }

    @Test
    public void testGetReceiptsFiltersBySku() {
        List<Receipt> receipts = purchase.getReceipts();
        receipts.clear();
        receipts.add(createReceipt("gold", new Date(1000L), new Date(5000L)));
        receipts.add(createReceipt("silver", new Date(2000L), new Date(6000L)));

        Receipt[] filtered = purchase.getReceipts("gold");
        assertEquals(1, filtered.length);
        assertEquals("gold", filtered[0].getSku());
    }

    @Test
    public void testSubscribeDelegatesToPurchaseWhenReceiptStoreInstalled() {
        ReceiptStore store = new ReceiptStore() {
            public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
                callback.onSucess(new Receipt[0]);
            }

            public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
                callback.onSucess(Boolean.TRUE);
            }
        };
        purchase.setReceiptStore(store);

        purchase.subscribe("vip");
        assertTrue(purchase.getRecordedPurchases().contains("vip"));
    }

    @Test
    public void testSubscribeWithPromotionalOfferDelegatesToPurchase() {
        ReceiptStore store = new ReceiptStore() {
            public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
                callback.onSucess(new Receipt[0]);
            }

            public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
                callback.onSucess(Boolean.TRUE);
            }
        };
        purchase.setReceiptStore(store);

        PromotionalOffer offer = new PromotionalOffer() { };
        purchase.subscribe("vip", offer);
        assertTrue(purchase.getRecordedPromotionalPurchases().contains("vip"));
    }

    @Test
    public void testSubscribeWithoutReceiptStoreThrows() {
        purchase.setReceiptStore(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> purchase.subscribe("missing"));
        assertEquals("Unsupported", ex.getMessage());
    }

    @Test
    public void testGetFirstReceiptExpiringAfter() {
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

    @Test
    public void testIsSubscribedUsesExpiryDate() {
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

    @Test
    public void testSynchronizeReceiptsSyncSubmitsPendingReceipts() throws Exception {
        Receipt pendingReceipt = createReceipt("gold", new Date(1000L), new Date(5000L));
        List<Receipt> pending = new ArrayList<Receipt>();
        pending.add(pendingReceipt);
        Storage.getInstance().writeObject(pendingKey, pending);

        final List<Receipt> fetched = Arrays.asList(createReceipt("gold", new Date(1000L), new Date(8000L)));
        final List<Receipt> submitted = new ArrayList<Receipt>();

        ReceiptStore store = new ReceiptStore() {
            public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
                callback.onSucess(fetched.toArray(new Receipt[fetched.size()]));
            }

            public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
                submitted.add(receipt);
                callback.onSucess(Boolean.TRUE);
            }
        };
        purchase.setReceiptStore(store);

        boolean success = purchase.synchronizeReceiptsSync(0);
        assertTrue(success);
        assertEquals(1, submitted.size());
        assertTrue(purchase.getPendingPurchases().isEmpty());
        Receipt[] refreshed = purchase.getReceipts("gold");
        assertEquals(1, refreshed.length);
        assertEquals(fetched.get(0).getExpiryDate(), refreshed[0].getExpiryDate());
    }

    @Test
    public void testSynchronizeReceiptsSyncReturnsFalseWhenSubmitFails() throws Exception {
        Receipt pendingReceipt = createReceipt("silver", new Date(2000L), new Date(6000L));
        Storage.getInstance().writeObject(pendingKey, Collections.singletonList(pendingReceipt));

        ReceiptStore store = new ReceiptStore() {
            public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
                callback.onSucess(new Receipt[0]);
            }

            public void submitReceipt(Receipt receipt, SuccessCallback<Boolean> callback) {
                callback.onSucess(Boolean.FALSE);
            }
        };
        purchase.setReceiptStore(store);

        boolean success = purchase.synchronizeReceiptsSync(0);
        assertFalse(success);
        List<Receipt> pending = purchase.getPendingPurchases();
        assertEquals(1, pending.size());
        assertEquals("silver", pending.get(0).getSku());
    }
}
