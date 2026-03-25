package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_payment {
    private GeneratedAccess_com_codename1_payment() {
    }

    public static Class<?> findClass(String name) {
        int lastDot = name == null ? -1 : name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(lastDot + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("ApplePromotionalOffer".equals(simpleName)) {
            return com.codename1.payment.ApplePromotionalOffer.class;
        }
        if ("PendingPurchaseCallback".equals(simpleName)) {
            return com.codename1.payment.PendingPurchaseCallback.class;
        }
        if ("Product".equals(simpleName)) {
            return com.codename1.payment.Product.class;
        }
        if ("PromotionalOffer".equals(simpleName)) {
            return com.codename1.payment.PromotionalOffer.class;
        }
        if ("Purchase".equals(simpleName)) {
            return com.codename1.payment.Purchase.class;
        }
        if ("PurchaseCallback".equals(simpleName)) {
            return com.codename1.payment.PurchaseCallback.class;
        }
        if ("Receipt".equals(simpleName)) {
            return com.codename1.payment.Receipt.class;
        }
        if ("ReceiptStore".equals(simpleName)) {
            return com.codename1.payment.ReceiptStore.class;
        }
        if ("RestoreCallback".equals(simpleName)) {
            return com.codename1.payment.RestoreCallback.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.payment.Receipt.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.payment.Receipt();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Date.class, java.util.Date.class, java.util.Date.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.util.Date.class, java.util.Date.class, java.util.Date.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.payment.Receipt((java.lang.String) adaptedArgs[0], (java.util.Date) adaptedArgs[1], (java.util.Date) adaptedArgs[2], (java.util.Date) adaptedArgs[3], ((Number) adaptedArgs[4]).intValue(), (java.lang.String) adaptedArgs[5], (java.lang.String) adaptedArgs[6], (java.lang.String) adaptedArgs[7], (java.lang.String) adaptedArgs[8]);
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
            if (safeArgs.length == 0) {
                return com.codename1.payment.Purchase.getInAppPurchase();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return com.codename1.payment.Purchase.getInAppPurchase(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("postReceipt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.String.class}, false);
                com.codename1.payment.Purchase.postReceipt((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Number) adaptedArgs[3]).longValue(), (java.lang.String) adaptedArgs[4]); return null;
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
            if (safeArgs.length == 0) {
                return typedTarget.getKeyIdentifier();
            }
        }
        if ("getNonce".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNonce();
            }
        }
        if ("getOfferIdentifier".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOfferIdentifier();
            }
        }
        if ("getSignature".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSignature();
            }
        }
        if ("getTimestamp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestamp();
            }
        }
        if ("setKeyIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setKeyIdentifier((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setNonce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setNonce((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOfferIdentifier".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setOfferIdentifier((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSignature".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSignature((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTimestamp".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.setTimestamp(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.payment.Product typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayName();
            }
        }
        if ("getLocalizedPrice".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalizedPrice();
            }
        }
        if ("getSku".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSku();
            }
        }
        if ("setDescription".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDescription((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setDisplayName".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setDisplayName((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setLocalizedPrice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setLocalizedPrice((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setSku".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSku((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.payment.Purchase typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.getExpiryDate(varArgs);
            }
        }
        if ("getFirstReceiptExpiringAfter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class, java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class, java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 1];
                for (int i = 1; i < adaptedArgs.length; i++) {
                    varArgs[i - 1] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.getFirstReceiptExpiringAfter((java.util.Date) adaptedArgs[0], varArgs);
            }
        }
        if ("getPendingPurchases".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPendingPurchases();
            }
        }
        if ("getProducts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, false);
                return typedTarget.getProducts((java.lang.String[]) adaptedArgs[0]);
            }
        }
        if ("getReceipts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getReceipts();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.getReceipts(varArgs);
            }
        }
        if ("getStoreCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStoreCode();
            }
        }
        if ("isItemListingSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isItemListingSupported();
            }
        }
        if ("isManageSubscriptionsSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isManageSubscriptionsSupported();
            }
        }
        if ("isManagedPaymentSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isManagedPaymentSupported();
            }
        }
        if ("isManualPaymentSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isManualPaymentSupported();
            }
        }
        if ("isRefundable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isRefundable((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isRestoreSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRestoreSupported();
            }
        }
        if ("isSubscribed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String[].class}, true)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String[].class}, true);
                java.lang.String[] varArgs = new java.lang.String[adaptedArgs.length - 0];
                for (int i = 0; i < adaptedArgs.length; i++) {
                    varArgs[i - 0] = (java.lang.String) adaptedArgs[i];
                }
                return typedTarget.isSubscribed(varArgs);
            }
        }
        if ("isSubscriptionSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSubscriptionSupported();
            }
        }
        if ("isUnsubscribeSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUnsubscribeSupported();
            }
        }
        if ("manageSubscriptions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.manageSubscriptions((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("pay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class}, false);
                return typedTarget.pay(((Number) adaptedArgs[0]).doubleValue(), (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Double.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.pay(((Number) adaptedArgs[0]).doubleValue(), (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("purchase".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.purchase((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.payment.PromotionalOffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.payment.PromotionalOffer.class}, false);
                typedTarget.purchase((java.lang.String) adaptedArgs[0], (com.codename1.payment.PromotionalOffer) adaptedArgs[1]); return null;
            }
        }
        if ("refund".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.refund((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("restore".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.restore(); return null;
            }
        }
        if ("setReceiptStore".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.payment.ReceiptStore.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.payment.ReceiptStore.class}, false);
                typedTarget.setReceiptStore((com.codename1.payment.ReceiptStore) adaptedArgs[0]); return null;
            }
        }
        if ("subscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.subscribe((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.payment.PromotionalOffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.payment.PromotionalOffer.class}, false);
                typedTarget.subscribe((java.lang.String) adaptedArgs[0], (com.codename1.payment.PromotionalOffer) adaptedArgs[1]); return null;
            }
        }
        if ("synchronizeReceipts".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.synchronizeReceipts(); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.synchronizeReceipts(((Number) adaptedArgs[0]).longValue(), (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
        }
        if ("synchronizeReceiptsSync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.synchronizeReceiptsSync(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("unsubscribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.unsubscribe((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("wasPurchased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.wasPurchased((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.payment.Receipt typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCancellationDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCancellationDate();
            }
        }
        if ("getExpiryDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getExpiryDate();
            }
        }
        if ("getInternalId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInternalId();
            }
        }
        if ("getObjectId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getObjectId();
            }
        }
        if ("getOrderData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOrderData();
            }
        }
        if ("getPurchaseDate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPurchaseDate();
            }
        }
        if ("getQuantity".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQuantity();
            }
        }
        if ("getSku".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSku();
            }
        }
        if ("getStoreCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStoreCode();
            }
        }
        if ("getTransactionId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTransactionId();
            }
        }
        if ("getVersion".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVersion();
            }
        }
        if ("setCancellationDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setCancellationDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setExpiryDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setExpiryDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setInternalId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setInternalId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setOrderData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setOrderData((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setPurchaseDate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Date.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Date.class}, false);
                typedTarget.setPurchaseDate((java.util.Date) adaptedArgs[0]); return null;
            }
        }
        if ("setQuantity".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setQuantity(((Number) adaptedArgs[0]).intValue()); return null;
            }
        }
        if ("setSku".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSku((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setStoreCode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setStoreCode((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTransactionId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTransactionId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.payment.PendingPurchaseCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("itemPurchaseError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.itemPurchaseError((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("itemPurchasePending".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.itemPurchasePending((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("itemPurchased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.itemPurchased((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("itemRefunded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.itemRefunded((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("paymentFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.paymentFailed((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("paymentSucceeded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.String.class}, false);
                typedTarget.paymentSucceeded((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("subscriptionCanceled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.subscriptionCanceled((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("subscriptionStarted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.subscriptionStarted((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.payment.PurchaseCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("itemPurchaseError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.itemPurchaseError((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("itemPurchased".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.itemPurchased((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("itemRefunded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.itemRefunded((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("paymentFailed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                typedTarget.paymentFailed((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("paymentSucceeded".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Double.class, java.lang.String.class}, false);
                typedTarget.paymentSucceeded((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).doubleValue(), (java.lang.String) adaptedArgs[2]); return null;
            }
        }
        if ("subscriptionCanceled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.subscriptionCanceled((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("subscriptionStarted".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.subscriptionStarted((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.payment.ReceiptStore typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fetchReceipts".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.util.SuccessCallback.class}, false);
                typedTarget.fetchReceipts((com.codename1.util.SuccessCallback) adaptedArgs[0]); return null;
            }
        }
        if ("submitReceipt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.payment.Receipt.class, com.codename1.util.SuccessCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.payment.Receipt.class, com.codename1.util.SuccessCallback.class}, false);
                typedTarget.submitReceipt((com.codename1.payment.Receipt) adaptedArgs[0], (com.codename1.util.SuccessCallback) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.payment.RestoreCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("itemRestored".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.itemRestored((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("restoreRequestComplete".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.restoreRequestComplete(); return null;
            }
        }
        if ("restoreRequestError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.restoreRequestError((java.lang.String) adaptedArgs[0]); return null;
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

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
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
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            return isSamInterface(type);
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
