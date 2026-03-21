package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_payment {
    private GeneratedAccess_com_codename1_payment() {
    }

    public static Class<?> findClass(String name) {
        if ("com.codename1.payment.ApplePromotionalOffer".equals(name)) return com.codename1.payment.ApplePromotionalOffer.class;
        if ("com.codename1.payment.PendingPurchaseCallback".equals(name)) return com.codename1.payment.PendingPurchaseCallback.class;
        if ("com.codename1.payment.Product".equals(name)) return com.codename1.payment.Product.class;
        if ("com.codename1.payment.PromotionalOffer".equals(name)) return com.codename1.payment.PromotionalOffer.class;
        if ("com.codename1.payment.Purchase".equals(name)) return com.codename1.payment.Purchase.class;
        if ("com.codename1.payment.PurchaseCallback".equals(name)) return com.codename1.payment.PurchaseCallback.class;
        if ("com.codename1.payment.Receipt".equals(name)) return com.codename1.payment.Receipt.class;
        if ("com.codename1.payment.ReceiptStore".equals(name)) return com.codename1.payment.ReceiptStore.class;
        if ("com.codename1.payment.RestoreCallback".equals(name)) return com.codename1.payment.RestoreCallback.class;
        return null;
    }

    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.payment.Receipt.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return new com.codename1.payment.Receipt();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Date.class, java.util.Date.class, java.util.Date.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.payment.Receipt((java.lang.String) safeArgs[0], (java.util.Date) safeArgs[1], (java.util.Date) safeArgs[2], (java.util.Date) safeArgs[3], ((Number) safeArgs[4]).intValue(), (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6], (java.lang.String) safeArgs[7], (java.lang.String) safeArgs[8]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.payment.Purchase.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getInAppPurchase".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.payment.Purchase.getInAppPurchase();
            }
        }
        if ("getInAppPurchase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                return com.codename1.payment.Purchase.getInAppPurchase(((Boolean) safeArgs[0]).booleanValue());
            }
        }
        if ("postReceipt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.String.class}, false)) {
                com.codename1.payment.Purchase.postReceipt((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], ((Number) safeArgs[3]).longValue(), (java.lang.String) safeArgs[4]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.payment.Purchase.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.payment.ApplePromotionalOffer) {
            try {
                return invoke0((com.codename1.payment.ApplePromotionalOffer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.payment.Product) {
            try {
                return invoke1((com.codename1.payment.Product) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.payment.Purchase) {
            try {
                return invoke2((com.codename1.payment.Purchase) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.payment.Receipt) {
            try {
                return invoke3((com.codename1.payment.Receipt) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.payment.PendingPurchaseCallback) {
            try {
                return invoke4((com.codename1.payment.PendingPurchaseCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.payment.PurchaseCallback) {
            try {
                return invoke5((com.codename1.payment.PurchaseCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.payment.ReceiptStore) {
            try {
                return invoke6((com.codename1.payment.ReceiptStore) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.payment.RestoreCallback) {
            try {
                return invoke7((com.codename1.payment.RestoreCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.payment.ApplePromotionalOffer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getKeyIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getKeyIdentifier();
            }
        }
        if ("getNonce".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getNonce();
            }
        }
        if ("getOfferIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOfferIdentifier();
            }
        }
        if ("getSignature".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSignature();
            }
        }
        if ("getTimestamp".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTimestamp();
            }
        }
        if ("setKeyIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setKeyIdentifier((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setNonce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setNonce((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOfferIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setOfferIdentifier((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setSignature".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setSignature((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setTimestamp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                typedTarget.setTimestamp(((Number) safeArgs[0]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.payment.Product typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDescription();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getLocalizedPrice".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getLocalizedPrice();
            }
        }
        if ("getSku".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSku();
            }
        }
        if ("setDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDescription((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setDisplayName((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setLocalizedPrice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setLocalizedPrice((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setSku".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setSku((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.payment.Purchase typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.getExpiryDate(varArgs);
            }
        }
        if ("getFirstReceiptExpiringAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.getFirstReceiptExpiringAfter((java.util.Date) safeArgs[0], varArgs);
            }
        }
        if ("getPendingPurchases".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPendingPurchases();
            }
        }
        if ("getProducts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                return typedTarget.getProducts((java.lang.String[]) safeArgs[0]);
            }
        }
        if ("getReceipts".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getReceipts();
            }
        }
        if ("getReceipts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.getReceipts(varArgs);
            }
        }
        if ("getStoreCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStoreCode();
            }
        }
        if ("isItemListingSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isItemListingSupported();
            }
        }
        if ("isManageSubscriptionsSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isManageSubscriptionsSupported();
            }
        }
        if ("isManagedPaymentSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isManagedPaymentSupported();
            }
        }
        if ("isManualPaymentSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isManualPaymentSupported();
            }
        }
        if ("isRefundable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.isRefundable((java.lang.String) safeArgs[0]);
            }
        }
        if ("isRestoreSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRestoreSupported();
            }
        }
        if ("isSubscribed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) safeArgs[i];
                }
                return typedTarget.isSubscribed(varArgs);
            }
        }
        if ("isSubscriptionSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isSubscriptionSupported();
            }
        }
        if ("isUnsubscribeSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isUnsubscribeSupported();
            }
        }
        if ("manageSubscriptions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.manageSubscriptions((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("pay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                return typedTarget.pay(((Number) safeArgs[0]).doubleValue(), (java.lang.String) safeArgs[1]);
            }
        }
        if ("pay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.pay(((Number) safeArgs[0]).doubleValue(), (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]);
            }
        }
        if ("purchase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.purchase((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("purchase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.payment.PromotionalOffer.class}, false)) {
                typedTarget.purchase((java.lang.String) safeArgs[0], (com.codename1.payment.PromotionalOffer) safeArgs[1]); return null;
            }
        }
        if ("refund".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.refund((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("restore".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.restore(); return null;
            }
        }
        if ("setReceiptStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.payment.ReceiptStore.class}, false)) {
                typedTarget.setReceiptStore((com.codename1.payment.ReceiptStore) safeArgs[0]); return null;
            }
        }
        if ("subscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.subscribe((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("subscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.payment.PromotionalOffer.class}, false)) {
                typedTarget.subscribe((java.lang.String) safeArgs[0], (com.codename1.payment.PromotionalOffer) safeArgs[1]); return null;
            }
        }
        if ("synchronizeReceipts".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.synchronizeReceipts(); return null;
            }
        }
        if ("synchronizeReceipts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.synchronizeReceipts(((Number) safeArgs[0]).longValue(), (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        if ("synchronizeReceiptsSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                return typedTarget.synchronizeReceiptsSync(((Number) safeArgs[0]).longValue());
            }
        }
        if ("unsubscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.unsubscribe((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("wasPurchased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.wasPurchased((java.lang.String) safeArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.payment.Receipt typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCancellationDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCancellationDate();
            }
        }
        if ("getExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getExpiryDate();
            }
        }
        if ("getInternalId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getInternalId();
            }
        }
        if ("getObjectId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getObjectId();
            }
        }
        if ("getOrderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getOrderData();
            }
        }
        if ("getPurchaseDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPurchaseDate();
            }
        }
        if ("getQuantity".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getQuantity();
            }
        }
        if ("getSku".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getSku();
            }
        }
        if ("getStoreCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getStoreCode();
            }
        }
        if ("getTransactionId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTransactionId();
            }
        }
        if ("getVersion".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getVersion();
            }
        }
        if ("setCancellationDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                typedTarget.setCancellationDate((java.util.Date) safeArgs[0]); return null;
            }
        }
        if ("setExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                typedTarget.setExpiryDate((java.util.Date) safeArgs[0]); return null;
            }
        }
        if ("setInternalId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setInternalId((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setOrderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setOrderData((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setPurchaseDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                typedTarget.setPurchaseDate((java.util.Date) safeArgs[0]); return null;
            }
        }
        if ("setQuantity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                typedTarget.setQuantity(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        if ("setSku".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setSku((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setStoreCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setStoreCode((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setTransactionId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.setTransactionId((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.payment.PendingPurchaseCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("itemPurchaseError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.itemPurchaseError((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("itemPurchasePending".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.itemPurchasePending((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("itemPurchased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.itemPurchased((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("itemRefunded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.itemRefunded((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("paymentFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.paymentFailed((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("paymentSucceeded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.String.class}, false)) {
                typedTarget.paymentSucceeded((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("subscriptionCanceled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.subscriptionCanceled((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("subscriptionStarted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.subscriptionStarted((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.payment.PurchaseCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("itemPurchaseError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.itemPurchaseError((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("itemPurchased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.itemPurchased((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("itemRefunded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.itemRefunded((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("paymentFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                typedTarget.paymentFailed((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]); return null;
            }
        }
        if ("paymentSucceeded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.String.class}, false)) {
                typedTarget.paymentSucceeded((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).doubleValue(), (java.lang.String) safeArgs[2]); return null;
            }
        }
        if ("subscriptionCanceled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.subscriptionCanceled((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("subscriptionStarted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.subscriptionStarted((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.payment.ReceiptStore typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchReceipts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.fetchReceipts((com.codename1.util.SuccessCallback) safeArgs[0]); return null;
            }
        }
        if ("submitReceipt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.payment.Receipt.class, com.codename1.util.SuccessCallback.class}, false)) {
                typedTarget.submitReceipt((com.codename1.payment.Receipt) safeArgs[0], (com.codename1.util.SuccessCallback) safeArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.payment.RestoreCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("itemRestored".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.itemRestored((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("restoreRequestComplete".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.restoreRequestComplete(); return null;
            }
        }
        if ("restoreRequestError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.restoreRequestError((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.payment.Receipt.class) {
            if ("STORE_CODE_ITUNES".equals(name)) return com.codename1.payment.Receipt.STORE_CODE_ITUNES;
            if ("STORE_CODE_PLAY".equals(name)) return com.codename1.payment.Receipt.STORE_CODE_PLAY;
            if ("STORE_CODE_SIMULATOR".equals(name)) return com.codename1.payment.Receipt.STORE_CODE_SIMULATOR;
            if ("STORE_CODE_WINDOWS".equals(name)) return com.codename1.payment.Receipt.STORE_CODE_WINDOWS;
        }
        throw unsupportedStaticField(type, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            return value instanceof Number;
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
